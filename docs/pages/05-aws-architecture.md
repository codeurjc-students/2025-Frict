## ☁️ AWS Architecture

### 🔎 Index

1. [Overview](#-overview)
2. [Architecture Diagram](#-architecture-diagram)
3. [AWS Services Used](#-aws-services-used)
4. [Infrastructure as Code](#-infrastructure-as-code)
5. [Spring Profiles](#-spring-profiles)
6. [CI/CD Pipelines](#-cicd-pipelines)
7. [Autoscaling](#-autoscaling)
8. [Multi-Replica Architecture](#-multi-replica-architecture)
9. [Security](#-security)
10. [Cost Estimate](#-cost-estimate)

&nbsp;



### 📍 Overview

The application is deployed on **Amazon Web Services (AWS)** following a cloud-native architecture designed for high availability, horizontal scalability, and fully automated delivery. The deployment strategy builds on the single Docker image approach described in the [Development Guide](/docs/pages/04-development-guide.md), extending it to a production-grade environment with the following pillars:

- **ECS Fargate** for serverless container orchestration with horizontal autoscaling
- **Infrastructure as Code** via CloudFormation nested stacks for reproducible, version-controlled infrastructure
- **Continuous Deployment** via GitHub Actions with OIDC authentication (no stored AWS credentials)
- **Cost target:** under $50 USD/month during normal usage

&nbsp;



### 🗺️ Architecture Diagram

```
                  Route53 (domain)
                        |
                  WAF (IP-based access control)
                        |
                  CloudFront (static cache + TLS)
                        |
                  ALB (sticky sessions, CloudFront-only SG)
                        |
            ┌───────────┼───────────┐         ← Application Auto Scaling
        Task 1      Task 2      Task N        (target tracking on CPU 60%)
       (Fargate)   (Fargate)   (Fargate)
            └─────┬─────┴─────┬─────┘
                  │           │
              RDS MySQL   DocumentDB
                              │
                       ChangeStream delivers to ALL tasks
                       → each task pushes WS to its local users

  Each task → invoke → Lambda Geocoding → Nominatim
  Each task → S3 (images via IAM task role)
  Each task → Secrets Manager (JWT, DB creds)
```

&nbsp;



### 📋 AWS Services Used

| Service | Purpose | Tier/Size |
|:---|:---|:---|
| **ECS Fargate** | Application containers (2-8 tasks) | 0.5 vCPU / 1 GB per task |
| **ALB** | Load balancing with sticky sessions | CloudFront-only ingress |
| **WAF** | IP-based access control on CloudFront | 1 Web ACL + 1 IP set rule |
| **RDS MySQL** | Transactional data | db.t4g.micro (free tier) |
| **DocumentDB** | Notifications + ChangeStreams | db.t3.medium (1 instance) |
| **S3** | Image storage | Standard |
| **CloudFront** | CDN + TLS termination | PriceClass_100 |
| **Route53** | DNS management | Public hosted zone |
| **ACM** | TLS certificates | Free (DNS validated) |
| **Lambda** | Geocoding proxy + DocumentDB auto-stop | Python 3.12 |
| **ECR** | Docker image registry | Private repo, keep 10 images |
| **Secrets Manager** | JWT secret, DB credentials | Auto-generated |
| **EventBridge** | Daily DocumentDB auto-stop check | rate(1 day) |
| **CloudWatch Logs** | Container log aggregation | 14-day retention |

&nbsp;



### 🏗️ Infrastructure as Code

The entire infrastructure is defined as **CloudFormation nested stacks**, enabling modular management, independent updates, and clear separation of concerns across layers.

```
infra/cloudformation/
  main.yml        — Root stack orchestrating all nested stacks
  network.yml     — VPC, subnets, security groups
  storage.yml     — S3 bucket, ECR repository
  data.yml        — RDS MySQL, DocumentDB, Secrets Manager
  lambda.yml      — Geocoding Lambda, auto-stop Lambda, EventBridge
  compute.yml     — ECS cluster, ALB, task definition, autoscaling
  edge.yml        — CloudFront, Route53

infra/scripts/
  package.sh      — Package nested templates to S3
  deploy.sh       — Deploy the stack
  docdb-start.sh  — Start DocumentDB manually
  docdb-stop.sh   — Stop DocumentDB manually
```

**Root stack parameters:** `DomainName`, `HostedZoneId`, `ImageTag`, `MinTaskCount`, `MaxTaskCount`, `EnableDocDB`, `ALBCertificateArn`, `CloudFrontCertificateArn`, `CloudFrontPrefixListId`.

&nbsp;



### ⚙️ Spring Profiles

| Profile | Activated by | Behavior |
|:---|:---|:---|
| `local` | `.env` in development | Nominatim direct geocoding, `ddl-auto=create-drop`, MinIO for S3 |
| `prod` | ECS TaskDefinition env var | Lambda geocoding, Flyway migrations, real S3, DocumentDB |
| `test` | CI workflows | Nominatim direct geocoding, test databases |

&nbsp;



### 🚀 CI/CD Pipelines

Three GitHub Actions workflows automate the build, infrastructure provisioning, and validation cycle.

#### deploy-app.yml

Triggered on push to `main` when `backend/**`, `frontend/**`, or `docker/Dockerfile` change:

1. Authenticate via OIDC (no stored AWS keys)
2. Build Docker image (Angular production build + Spring Boot JAR)
3. Push to ECR with commit SHA and `latest` tags
4. Rolling deploy on ECS with `force-new-deployment`
5. Wait for service stability

&nbsp;

#### deploy-infra.yml

Triggered on push to `main` when `infra/cloudformation/**` or `infra/lambda/**` change:

1. Authenticate via OIDC
2. Package CloudFormation templates to S3
3. Deploy stack with parameter overrides from GitHub Secrets

&nbsp;

#### load-test.yml

Manual trigger (`workflow_dispatch`):

1. Run k6 load test against the deployed environment
2. Ramp from 0 to 100 virtual users
3. Upload results as GitHub artifact

&nbsp;



### 📊 Autoscaling

The ECS service uses **Application Auto Scaling** with a target tracking policy on average CPU utilization:

- **Target value:** 60% average CPU
- **Min capacity:** 2 tasks
- **Max capacity:** 8 tasks
- **Scale-out cooldown:** 60 seconds
- **Scale-in cooldown:** 300 seconds

WebSocket reconnection with exponential backoff on the client side handles scale-in events gracefully, ensuring users experience minimal disruption when tasks are terminated.

&nbsp;



### 🔄 Multi-Replica Architecture

Running multiple ECS tasks behind an ALB requires careful coordination of stateful WebSocket connections and event delivery:

- **ChangeStreams fan-out:** Each ECS task opens its own MongoDB ChangeStream listener. When any task writes a notification, ALL tasks receive the event via the ChangeStream. Each task only delivers the notification via WebSocket to users connected to THAT specific task.

- **ALB sticky sessions:** Configured with `lb_cookie` (24-hour duration) to keep a user's WebSocket connection pinned to the same task for the duration of their session.

- **Deregistration delay:** Set to 30 seconds, allowing graceful WebSocket closure during scale-in events before the ALB removes the task from the target group.

- **Frontend reconnection:** The Angular client reconnects automatically with exponential backoff (1-second base, 30-second cap) when a WebSocket connection is lost.

&nbsp;



### 🔒 Security

The architecture implements defense in depth across multiple layers:

#### Network isolation

- **ALB restricted to CloudFront:** The ALB security group only allows inbound HTTP from the AWS-managed CloudFront prefix list (`com.amazonaws.global.cloudfront.origin-facing`). This prevents direct access to the ALB, forcing all traffic through CloudFront. The prefix list ID is resolved dynamically during deployment.
- **Internal traffic isolation:** All inter-service traffic within the VPC uses HTTP, isolated by security groups. TLS termination occurs only at the edge (CloudFront and ALB).

#### Access control

- **AWS WAF on CloudFront:** A Web ACL with an IP set rule controls which end-user IPs can access the application. The default action is **Block**, with an explicit **Allow** rule for whitelisted IPs. This is managed via the AWS console (not IaC, as it is intended as a temporary measure during pre-release). WAF costs ~$6/month (Web ACL + 1 rule + requests).
- **No AWS credentials stored in GitHub:** OIDC federation with an IAM role restricted to the specific repository and branch.

#### Data protection

- **Secrets Manager** for all sensitive values (database passwords, JWT secret) with auto-generated credentials.
- **S3 block public access** enabled on the images bucket; images are served exclusively via presigned URLs.
- **Least-privilege IAM task role:** Scoped to S3 access on the specific bucket and Lambda invoke on the specific geocoding function.

&nbsp;



### 💰 Cost Estimate

| Resource | Normal Usage | During Load Test | During Demo |
|:---|:---|:---|:---|
| **ECS Fargate** (2 tasks baseline) | ~$15/month | ~$20/month (burst to 4-8 tasks) | ~$15/month |
| **RDS MySQL** (db.t4g.micro) | $0 (free tier) | $0 | $0 |
| **DocumentDB** (db.t3.medium) | ~$12/month (with auto-stop) | ~$15/month (always on) | ~$12/month |
| **ALB** | ~$5/month | ~$6/month | ~$5/month |
| **CloudFront** | ~$1/month | ~$2/month | ~$1/month |
| **S3 + ECR** | ~$1/month | ~$1/month | ~$1/month |
| **Lambda + EventBridge** | < $1/month | < $1/month | < $1/month |
| **WAF** (1 Web ACL + 1 rule) | ~$6/month | ~$6/month | ~$6/month |
| **Route53 + ACM** | ~$1/month | ~$1/month | ~$1/month |
| **Secrets Manager** | ~$2/month | ~$2/month | ~$2/month |
| **CloudWatch Logs** | ~$1/month | ~$2/month | ~$1/month |
| **Total** | **~$45/month** | **~$56/month** | **~$45/month** |

&nbsp;

[◀️](/docs/pages/04-development-guide.md) **Page 5. AWS Architecture** [▶️](/docs/pages/06-progress-tracking.md)

[⏪ Return to Index](/README.md)
