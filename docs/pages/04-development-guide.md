## 🧭 Development guide

### 🔎 Index

1. [Introduction](#-introduction)
2. [Technologies Stack](#-technologies-stack)
3. [Tools](#-tools)
4. [Architecture](#-architecture)
5. [Quality Assurance](#-quality-assurance)
6. [Architecture Deployment](#-architecture-deployment)
7. [Development Process](#-development-process)
8. [Development Environment Setup](#-development-environment-setup)

&nbsp;



### 📍 Introduction

This website follows a Single-Page Application (SPA) architecture, where the user interface is dynamically updated by combining different independent components rather than loading entire new pages. This ensures a faster, smoother, and more fluid user experience during each interaction.

The system is built upon a strictly decoupled Client-Server model, ensuring high scalability and maintainability by reducing dependencies. The core architecture relies on the following components:

* **Client (Frontend):** An Angular-based SPA that communicates with the server via REST API requests and WebSockets to fetch, render, and update dynamic content in real time.


* **Server (Backend):** A robust Spring Boot application managing the REST API. It strictly adheres to the **Model-View-Controller (MVC)** architecture, keeping controllers completely isolated from any business logic. Furthermore, it implements the **Facade design pattern** through Orchestrators to efficiently manage complex, multi-service transactions.


* **Relational Database:** A MySQL database with a dynamic schema managed automatically via Spring Data JPA entities and annotations.


* **Non-Relational Database:** A MongoDB database with replica set enabled, which enables change streams features for multiple backend instances communications.


* **Object Storage:** MinIO, an AWS S3-compatible storage server, dedicated to handling and serving multimedia assets (such as user and product images) efficiently.


To ensure data protection and safe access, the system utilizes **Spring Security** integrated with **JWT (JSON Web Tokens)** for internal session management and **OAuth2** for third-party authentication. Additionally, the API is built with production readiness in mind: it is fully documented using **Swagger (OpenAPI)** to ensure the documentation is always synchronized with the codebase, and relies on **Spring Boot Actuator** to provide real-time health checks and system monitoring.

Beyond local execution, the platform is deployed on **Amazon Web Services (AWS)** following a cloud-native model with containerized instances, horizontal autoscaling, and fully automated delivery, all defined as Infrastructure as Code. The complete cloud design is covered in the [AWS Architecture](/docs/pages/05-aws-architecture.md) page.

| Feature | Technologies & Patterns                                                           |
| :--- |:----------------------------------------------------------------------------------|
| **Architecture & Patterns** | SPA, Strict MVC, Facade Pattern (Service Orchestrators), Event-Driven.            |
| **Backend Technologies** | Java 21, Spring Boot 3, Spring Security (JWT & OAuth2), Spring Data JPA, Hibernate, Lombok. |
| **Frontend Technologies** | Angular 19 (Standalone Components, Signals), TypeScript, Tailwind CSS, PrimeNG 19. |
| **API & Monitoring** | Swagger (OpenAPI), Spring Boot Actuator.                                          |
| **Data & Storage** | MySQL, MongoDB (replica set), MinIO (S3-compatible Object Storage).               |
| **Testing & QA** | JUnit, Mockito, REST Assured, Jasmine, JaCoCo, Istanbul, SonarQube Cloud, k6.     |
| **Tools & IDEs** | IntelliJ IDEA, MySQL Workbench, MongoDB Compass, Git, HAProxy.                    |
| **Deployment** | Docker, Docker Compose, AWS (CloudFormation), GitHub Actions (CI/CD).             |
| **Development Process** | Feature branches, Pull Requests, GitHub Actions (Strict CI validation).           |

&nbsp;



### 📋 Technologies Stack

#### 💾 Backend

- [**Java**](https://www.java.com/en/): Main programming language of the backend. Its object-oriented structure, strong typing, and high performance provide a solid and efficient base for the system's business logic.


- [**Spring Boot**](https://spring.io/projects/spring-boot): Framework that facilitates the creation and execution of REST services by reducing initial configuration and providing a ready-to-use productive environment.


- [**Hibernate**](https://hibernate.org/) & [**Spring Data JPA**](https://spring.io/projects/spring-data): Technological abstraction that simplifies relational database access. Hibernate acts as the ORM engine that maps Java objects to database tables, while Spring Data JPA streamlines persistence through dynamic repositories, removing the need to write manual SQL.


- [**Spring Security**](https://spring.io/projects/spring-security): Handles authentication and authorization using JWT for internal sessions and OAuth2 for external integrations, ensuring endpoints are strictly protected.


- [**JWT (JSON Web Token)**](https://www.jwt.io/introduction): Provides stateless authentication, a secure method for transferring access information between the Angular client and the Spring Boot server through digitally signed tokens.


- [**OAuth2**](https://oauth.net/2/): Standard authorization protocol integrated with Spring Security to enable third-party authentication, allowing users to log in reliably with their external accounts.


- [**Thymeleaf**](https://www.thymeleaf.org/): Server-side template engine that renders dynamic HTML, used to design and format the emails sent by the platform.


- [**Spring Boot Actuator**](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html): Provides built-in endpoints for real-time application monitoring and health checks.

&nbsp;

#### 📺 Frontend

- [**Angular**](https://angular.dev/): Open-source platform and core pillar of the client, managing the entire user interface and presentation logic. Leveraging modern features such as Standalone Components and Signals, it delivers a highly reactive, optimized, and seamless SPA experience.


- [**TypeScript**](https://www.typescriptlang.org/): Provides strict type safety and powerful tooling support, preventing runtime errors and drastically improving frontend maintainability.


- [**Tailwind CSS**](https://tailwindcss.com/): A utility-first CSS framework used for rapid UI development and highly customizable styling directly from the HTML.


- [**PrimeNG**](https://primeng.org/): High-level UI component library for Angular, fully compatible with Tailwind CSS, that provides complex ready-to-use interactive elements and accelerates UI development.


- [**JSON**](https://www.json.org/): Serves as the standard, lightweight format for exchanging data between the Angular client and the Spring Boot server.

&nbsp;

#### 🗄️ Persistence

- [**MySQL**](https://www.mysql.com/): Relational database engine that guarantees the persistence and integrity of the structured business data, providing reliable storage and robust entity modeling.


- [**MongoDB**](https://www.mongodb.com/): NoSQL document-oriented database that provides high availability and flexible, JSON-like storage. Configured as a replica set, it manages dynamic data flows and real-time communications between connected users via change streams.


- [**MinIO**](https://min.io/): High-performance, S3-compatible object storage server used for efficiently centralizing and serving the application's multimedia assets, such as user and product images.

&nbsp;

#### 📚 Auxiliary Libraries

- [**Lombok**](https://projectlombok.org/): Reduces boilerplate Java code (getters, setters, constructors) through annotations, keeping the backend codebase clean and maintainable.


- [**JJWT**](https://github.com/jwtk/jjwt): Java implementation of the JWT standard, used to create, sign, and validate the authentication tokens on the backend.


- [**Spring Boot Mail**](https://docs.spring.io/spring-boot/reference/io/email.html): Integrates JavaMail for sending emails from the server, used for order confirmations and the OTP codes during credential recovery.


- [**iText**](https://itextpdf.com/): PDF generation and manipulation library, used to automatically create invoices and exportable reports from the platform data.


- [**Leaflet**](https://leafletjs.com/): Interactive map library for the frontend, handling the geographic visualization of shops, trucks, and delivery addresses with OpenStreetMap cartographic data.


- [**Chart.js**](https://www.chartjs.org/): Data visualization library used to render the statistics and performance charts of the platform.


- [**angularx-qrcode**](https://github.com/Cordobo/angularx-qrcode) & [**html5-qrcode**](https://github.com/mebjas/html5-qrcode): Used to generate the unique confirmation QR code associated with each order, and to read it through the device camera at delivery time.

&nbsp;

#### 🌐 External Services

- [**Nominatim**](https://nominatim.org/): OpenStreetMap geocoding service that converts addresses into geographic coordinates and vice versa. It is consumed through an internal backend proxy to avoid exposing the service directly to the client.


- [**OSRM**](https://project-osrm.org/): Open-source routing engine based on OpenStreetMap data, used to calculate routes between two points, returning distance, estimated duration, and the trace coordinates.

&nbsp;

#### 🧪 Testing & QA

- [**JUnit**](https://junit.org/) & [**Mockito**](https://site.mockito.org/): Core frameworks used for implementing isolated unit and integration tests, mocking dependencies to isolate the logic under test.


- [**REST Assured**](https://rest-assured.io/): Specifically utilized to automate and validate the behavior of the REST API endpoints, their JSON payloads, and security constraints.


- [**Jasmine**](https://jasmine.github.io/): Behavior-driven development framework used to execute robust client-side tests for the Angular components and signals logic.


- [**JaCoCo**](https://www.jacoco.org/), [**Istanbul**](https://istanbul.js.org/) & [**SonarQube Cloud**](https://sonarcloud.io/): Provide detailed code coverage (backend and frontend respectively) and continuous static code analysis to maintain high-quality standards.


- [**k6**](https://k6.io/): Open-source load and performance testing tool used to define traffic-shaped scenarios in JavaScript and validate the system's behavior under high demand, especially against the cloud deployment.

&nbsp;

#### ☁️ Cloud (AWS)

- [**Amazon ECS Fargate**](https://aws.amazon.com/fargate/): Serverless container orchestration service that runs the application instances without managing the underlying infrastructure, scaling horizontally on demand.


- [**Application Load Balancer (ALB)**](https://aws.amazon.com/elasticloadbalancing/application-load-balancer/): Distributes incoming traffic across the Fargate instances with sticky sessions, working in coordination with ECS for autoscaling.


- [**Amazon RDS**](https://aws.amazon.com/rds/) & [**Amazon DocumentDB**](https://aws.amazon.com/documentdb/): Managed relational (MySQL) and document (MongoDB-compatible, with change streams) database services in the cloud.


- [**Amazon S3**](https://aws.amazon.com/s3/) & [**Amazon ECR**](https://aws.amazon.com/ecr/): Object storage for multimedia assets and private registry for the Docker images consumed by ECS, respectively.


- [**Amazon CloudFront**](https://aws.amazon.com/cloudfront/) & [**Amazon Route 53**](https://aws.amazon.com/route53/): CDN with TLS termination at the edge and DNS management for the application's own domain.


- [**AWS WAF**](https://aws.amazon.com/waf/), [**AWS IAM**](https://aws.amazon.com/iam/), [**AWS Secrets Manager**](https://aws.amazon.com/secrets-manager/) & [**AWS ACM**](https://aws.amazon.com/certificate-manager/): Security layer covering IP-based access control, least-privilege permissions, secret management, and TLS certificates.


- [**AWS Lambda**](https://aws.amazon.com/lambda/) & [**Amazon SES**](https://aws.amazon.com/ses/): Serverless functions acting as proxies for external services (geocoding, routing) and managed email delivery.


- [**Amazon CloudWatch**](https://aws.amazon.com/cloudwatch/) & [**AWS Budgets**](https://aws.amazon.com/aws-cost-management/aws-budgets/): Real-time monitoring of the system's behavior and cost control.

&nbsp;

#### 🚀 DevOps

- [**Docker**](https://www.docker.com/): Packages the application into isolated containers, ensuring consistent execution regardless of the host system.


- [**Docker Compose**](https://docs.docker.com/compose/): Orchestrates multiple containers, allowing easy setup of the full application stack with a single command.


- [**GitHub Actions**](https://github.com/features/actions): Automates CI/CD workflows, enforcing strict Pull Request validation and managing the build, infrastructure provisioning, and deployment cycle.


- [**AWS CloudFormation**](https://aws.amazon.com/cloudformation/): Defines the entire cloud infrastructure as code through nested stacks, enabling reproducible and version-controlled deployments.

&nbsp;



### 🔧 Tools

- [**IntelliJ IDEA**](https://www.jetbrains.com/idea/): Powerful IDE for backend development, with deep integration with the Spring ecosystem.


- [**MySQL Workbench**](https://www.mysql.com/products/workbench/): Facilitates database design and visual modeling for managing the MySQL schema.


- [**MongoDB Compass**](https://www.mongodb.com/products/tools/compass): Allows visualizing the collections, documents, and data flows within a MongoDB database.


- [**Git**](https://git-scm.com/): Enables strict version control and collaboration across the development lifecycle.


- [**GitHub Projects**](https://github.com/features/issues): Planning tool integrated into the repository, acting as a dynamic Kanban board to organize tasks and track progress by iterations.


- [**Swagger (OpenAPI)**](https://swagger.io/): Automatically generates interactive and up-to-date API documentation directly from the codebase.


- [**HAProxy**](https://www.haproxy.org/): High-performance load balancer used locally to replicate the cloud's multi-instance topology, distributing requests across the application replicas before deployment to AWS.


- [**AWS SDK v2**](https://aws.amazon.com/sdk-for-java/): Official AWS development kit for Java, used to interact with the cloud services (such as S3 and Lambda) directly from the application code.

&nbsp;



### 🏢 Architecture

The application's architecture is designed for high scalability and clear separation of concerns, following a modular approach across all its layers.

#### 🏛️ Domain Model

The system splits persistence into two complementary vertices according to the nature of the data: relational entities (managed via JPA/Hibernate) guarantee business consistency, while non-relational entities (MongoDB documents) provide flexibility for usage data and enable reactive event flows.

* **Relational Database Schema:** A relational structure used to handle the complex relationships between the core business entities.

![Relational Database Schema](../diagrams/v1.0/relational-model.png)

* **Non-Relational Database Schema:** A decoupled document structure, using common documents (connections, notifications) and time series documents (registries) for high request rates and easier data insight retrieval.

![Non-Relational Database Schema](../diagrams/v1.0/nonrelational-model.png)

&nbsp;

#### 📃 REST API & Documentation

Communication between the frontend client and the backend server is handled entirely via a standardized RESTful API, utilizing Angular proxies to enable relative routing on the client side.

The API strictly adheres to the **OpenAPI** (Swagger) specification, which serves as a comprehensive and interactive contract for the system's endpoints. This standardization ensures that frontend components and external consumers have a reliable, self-documenting interface to interact with. Once the application is running (Check the [**Development Environment Setup**](#-development-environment-setup) section), developers can dynamically explore, test, and validate API requests in real-time through the built-in **Swagger UI**.

> ℹ️ **NOTE:** For quick reference without needing to spin up the application environment, a static rendered version of the API documentation is readily available [here](../openapi.json).

&nbsp;

#### ⚙️ Server-Side Architecture (Backend)

The backend is built with **Spring Boot**, implementing a robust multi-layered MVC architecture. This design guarantees a strict separation of concerns, isolating core business logic from HTTP request handling and database interactions. As the system relies on two data sources of different nature, two independent request flows coexist across this structure:

* **Controller Layer:** REST controllers that act as the entry points, receiving HTTP requests and building the JSON responses.
* **Service Layer:** A combination of services and orchestrators (implementing the Facade Pattern to resolve circular references) that manage core business rules, service communication, and complex transactional integrity.
* **Repository Layer:** Utilizes the Repository Pattern (via Spring Data JPA) to abstract database operations and persist the domain entities.
* **Domain Layer:** The entities that model the business and define their structural properties and logical relationships.

As an exception to this layered distribution, the WebSocket service communicates directly with the non-relational repositories to speed up the real-time event transmission, without traversing the intermediate layers.

![Relational Backend Architecture](../diagrams/v1.0/relational-services.png)

![Non-Relational Backend Architecture](../diagrams/v1.0/nonrelational-services.png)

&nbsp;

#### 💻 Client-Side Architecture (Frontend)

The frontend is a modern **Angular** application built with Standalone Components to maximize code reusability, maintainability, and performance. It is structured into two operational layers (components and services), supported by transversal interceptors (which manage the WebSocket connection) and data interfaces (which mirror the backend model to keep strong typing). Given the size of the project, the diagrams are split into three blocks:

##### Management Components
![Management Components Architecture](../diagrams/v1.0/frontend-management.png)

##### Client Components
![Client Components Architecture](../diagrams/v1.0/frontend-client.png)

##### Common Components
![Common Components Architecture](../diagrams/v1.0/frontend-common.png)

&nbsp;



### ☑️ Quality Assurance

To ensure a robust and bug-free environment, the application undergoes a rigorous Quality Assurance (QA) process. A strict Continuous Integration (CI) pipeline enforces these standards, blocking any pull request that does not pass the automated test suite.

#### ⌛ Automated Testing

The platform's reliability is validated through multiple automated testing layers, following the Test Pyramid model:

- **Unit & Integration Testing:** Powered by **JUnit** and **Mockito**, isolating backend business logic and verifying proper database communication.
- **API Testing:** Using **REST Assured** to validate HTTP responses, JSON payloads, and security constraints.
- **Client-Side Testing:** **Jasmine** is utilized to test the Angular Standalone components and signals logic.

To guarantee a reproducible environment isolated from real business data, the backend uses a dedicated test profile that enables Testcontainers, spinning up ephemeral databases for each run.

&nbsp;

#### 📊 Static Code Analysis & Results

Overall project code quality, vulnerabilities, and test coverage are continuously monitored using **SonarQube Cloud**, which integrates with **JaCoCo** and **Istanbul** to measure precise coverage indexes. Analyzing the current project state provides a transparent view of the architecture's evolution and the management of technical debt.

**Overall Project Health:**
As reflected in the dashboard, the project maintains a **duplication rate of 2.4%**, within the recommended standards, and a **global coverage of 76.8%** over the codebase (~22,000 lines). All critical parameters hold an **'A' rating** in reliability and security, with 0 open vulnerabilities and 0 open issues.

![SonarQube Overall Analysis](../diagrams/v1.0/sonarqube-overall.png)

**Risk & Technical Debt Distribution:**
The bubble chart below correlates **Technical Debt** with **Code Coverage** per file. The vast majority of files cluster in the high-coverage, near-zero-debt quadrant, confirming a highly maintainable and well-tested core.

The most notable exception is the frontend's `orders-delivery` component, with 1h 15min of technical debt and 52.7% coverage over its 470 lines. This deviation is justified by its nature: it integrates complex asynchronous flows, manages reactive state with Signals, and interacts directly with the external DOM through the map library, which considerably complicates mocking in tests. Despite this, it keeps an 'A' rating in reliability and security.

![SonarQube Code Analysis](../diagrams/v1.0/sonarqube-code.png)

&nbsp;



### 📡 Architecture Deployment

#### Packaging & Distribution

The application's deployment strategy is designed for portability, automation, and a seamless transition to cloud environments.

* **Single Docker Image:** The entire application is packaged into a unified Docker image. This encapsulates both the compiled Angular client (frontend) and the Spring Boot server (backend), eliminating version mismatches and drastically simplifying the distribution process.
* **Compose Orchestration:** The execution of the application container, alongside its required external services (MySQL, MongoDB and MinIO), is coordinated locally using **Docker Compose** with HAProxy for load balancing across multiple replicas.
* **Artifact Distribution:** Docker images are published to **Amazon ECR** (Elastic Container Registry) for production cloud deployments, and to **DockerHub** for public distribution and local testing. You can access and pull the application artifacts directly from:
  **[Frict DockerHub](https://hub.docker.com/r/mjpulido/frict)**

&nbsp;

#### AWS Cloud Deployment

The production environment runs on **Amazon Web Services (AWS)** using a cloud-native architecture with ECS Fargate, horizontal autoscaling, and fully automated CI/CD pipelines via GitHub Actions with OIDC federation. The entire infrastructure is provisioned as **CloudFormation nested stacks** for reproducible, version-controlled deployments.

For a comprehensive description of the AWS architecture, services, security model, and cost estimates, see the [AWS Architecture](/docs/pages/05-aws-architecture.md) page.

&nbsp;



### 📅 Development Process

The project is being developed using an iterative and incremental process, which follows the Agile principles and applies some of the good coding practices described by XP (Extreme Programming) and Kanban methodologies, such as:
- Code refactoring to achieve improvement
- Simple, incremental and evolutionary changes
- Quality improvement via Continuous Integration


#### ✏️ Task management

In order to keep control of the pending tasks in each iteration, the project will include:

- **GitHub Issues**: Contains the main information about a task, such as its title, description, priority, deadline or even associated branch.

- **GitHub Project**: Contains each one of the issues to be completed during the iteration, divided by columns depending on its progress.
  - Columns: _To Do, In Progress, Under Review, Done_.
  - Priorities: _Low, Medium, High, Very High_.
  - Task size: _XS, S, M, L, XL_.

&nbsp;

#### 📀 Git

**Main branch**

Remains stable and always available for deployment.

**Branching strategy**

Each branch will enclose the code changes made to implement a single functionality, and will be merged to the main branch once the implementation has concluded and all required tests have been passed successfully.

**Branching process**

1. Create the branch `iteration-feature` from main (e.g. `2-add-minimal-services`)


2. Commit to the branch. After each commit, client and server unit tests will automatically be triggered.


3. Pull request to the main branch after feature implementation is completed. Before merging, client and server unit, integration and system tests will be automatically triggered.

&nbsp;

#### 🚧 Continuous Integration (CI) and Automated Publishing

Project testing, static code analysis and artifact publishing is automated by using CI/CD pipelines and GitHub Actions workflows.

#### Unit Testing On Commit (unit_test_on_commit.yml)

- Runs all **unit tests** with **every commit** made in a feature branch, or manually
- Tests server and client separately to optimize workflow duration
- Does not run any component during the tests

#### Full System Testing On Pull Request (full_test_on_pull_request.yml)

- Runs **before merging a pull request** from a feature branch to the main branch, or manually
- Runs all **unit, integration and API tests**
- Backend component is started before integration and API tests against a real database

#### SonarQube Static Code Analysis (sonar_analysis.yml)

- Runs only manually (due to its complexity and duration)
- Runs all unit, integration and API tests
- Backend and frontend components are started before the analysis
- Auxiliar MySQL Docker container is built before the analysis
- JaCoCo coverage tool is used during the analysis, so SonarQube can show coverage metrics

#### Docker Image and OCI Artifact Publishing (oci_artifact_publishing.yml)

This workflow automates the containerization, versioning, and distribution of the application to Docker Hub for public access.

- **Triggers:** Runs automatically when code is pushed or merged into the `main` branch, when a new GitHub Release is published, or manually via workflow dispatch.
- **Smart Tagging:** Dynamically resolves and assigns image tags based on the trigger context:
  - `dev` for standard pushes/merges into the main branch.
  - The specific release tag (e.g., `v1.0.0`) alongside the `latest` tag when a formal release is published.
  - A custom tag combining the branch name, timestamp, and commit SHA for manual executions.
- **Build & Registry Push:** Safely builds the Docker image and pushes it to the public Docker Hub registry (`mjpulido/frict`).
- **Compose OCI Artifacts:** Automatically parses the `docker-compose.yml` file to inject the exact generated image tag, and publishes the Compose file itself as an OCI artifact (e.g., `dev-compose` or `latest-compose`). This enables users to seamlessly deploy the entire stack remotely using the `docker compose -f oci://...` command without needing to download any files.

#### AWS Deployment Pipeline (deploy.yml)

A single unified workflow handles production deployment to AWS using **OIDC authentication** (no AWS credentials stored in GitHub). It is triggered on push to `main` when the backend, frontend, `Dockerfile`, CloudFormation templates, or Lambda code change, and can also be launched manually.

It runs three jobs:

1. **Deploy Infrastructure** and **Build & Push Image** run **in parallel**:
  - *Infrastructure*: authenticates via OIDC, packages the nested CloudFormation templates to S3, and deploys the stack. On subsequent runs it exits immediately when there are no infrastructure changes (`--no-fail-on-empty-changeset`).
  - *Image*: authenticates via OIDC, builds the Docker image, waits for the ECR repository, and pushes it tagged with the commit SHA and `latest`.
2. **Activate Deployment** runs once both complete: it forces a new ECS deployment (`force-new-deployment`) and waits for service stability.

This parallel design resolves the chicken-and-egg problem on first deployments: the image is pushed to ECR while CloudFormation is still creating the data layer (~15 min), so it is available before ECS starts.

#### Load Testing (load-test.yml)

Manual workflow (`workflow_dispatch`) that runs the **k6** load test against the deployed environment and uploads the results as a GitHub artifact. The test drives the public API the same way the Angular SPA does, with cookie-based authentication and a per-VU cookie jar.

It exposes several **profiles**, selectable via parameter, each shaping the traffic differently:

- **`smoke`** — 5 VUs for 30s. Quick sanity check of script and environment.
- **`load`** — steady ~30 VUs (default). Verifies the SLOs at baseline capacity.
- **`scale`** — ramps up to 100 VUs to deliberately push past the CPU target and provoke a **measured ECS scale-out**, confirming the new tasks absorb the load and latency recovers.
- **`soak`** — sustained ~30 VUs for an extended period to detect leaks or drift over time.

The test combines four traffic-shaped **scenarios** running together: anonymous storefront browsing (the bulk of the traffic), an authenticated client journey, a WebSocket presence pool, and an optional staff dashboard scenario (heavier read queries, opt-in via credentials). Reversible writes (such as adding and removing a favourite) can be enabled with a flag.

> ℹ️ **NOTE:** Because the test **mutates data** (product views, WebSocket presence, optional writes), it never runs against production. A hard guard in the script aborts the run if the target points at the production domain, with no override.

#### Parallel Load-Test Environment

To run these tests safely without touching production, the load test targets a **disposable load-test clone** of the platform (`loadtest.frict.es`), provisioned as a separate, ephemeral stack alongside production. It is a full sibling environment with its own ECS cluster, databases, and load balancer, and it is the `loadtest` target selectable in the lifecycle workflows described below. Thanks to the snapshot-based parameters in the data layer, this clone can be restored from production database snapshots, so it exercises a realistic dataset while remaining fully isolated and destroyable after each measurement.

#### Architecture Lifecycle Management

Beyond build and deploy, the project includes three manual workflows (`workflow_dispatch`) to manage the running cloud architecture and keep costs under control. All three take an `environment` input to target either the production stack (`frict`) or the load-test clone (`loadtest`), and authenticate via OIDC:

- **Start Environment (`start-environment.yml`):** Brings the whole stack back online. It starts the RDS MySQL instance and the DocumentDB cluster (only if they are stopped) and restores the ECS autoscaling target to 2-8 tasks. The full startup takes around 15 minutes until everything reports ready.
- **Stop Environment (`stop-environment.yml`):** Powers the environment down to minimize cost. It first scales the ECS service to 0/0, waits for the running tasks to drain, and then stops both the RDS instance and the DocumentDB cluster.
- **Environment Status (`status-environment.yml`):** Reports a consolidated view of the environment without changing anything: CloudFormation stack status, ECS running vs. desired tasks, RDS and DocumentDB states, ALB healthy targets, and the site URL, ending with an overall verdict (`READY`, `STOPPED`, or `TRANSITIONING`).

> ℹ️ **NOTE:** AWS automatically restarts stopped databases after 7 days. A scheduled **EventBridge** + **Lambda** routine also checks the DocumentDB uptime daily and stops the cluster once it exceeds a configured threshold, preventing forgotten resources from accumulating cost.

&nbsp;

#### 🏷️ Versioning

The application follows a strictly automated versioning and release procedure powered by GitHub Actions. This ensures consistency between the source code repository and the distributed artifacts.

**Release Procedure:**
* **Automated Releases:** Whenever a new Release is formally published via the GitHub repository, the `oci_artifact_publishing.yml` workflow is automatically triggered. It handles the entire build process and publishes both the Docker image and the Compose OCI artifact matching the specific release version tag (e.g., `v1.0`).
* **Latest Tagging:** During this formal release cycle, the workflow automatically updates the global `latest` and `latest-compose` tags to point to this newly published version. This guarantees that users downloading the default image always receive the most stable production build.
* **Continuous Development:** Routine merges to the `main` branch automatically build and update the `dev` tag, providing a continuously updated environment for testing without interfering with production releases.
* **Manual Artifact Publishing:** Developers can manually execute the workflow via `workflow_dispatch` if immediate artifact generation is required. This triggers a custom build that dynamically tags the image and Compose file using a combination of the branch name, timestamp, and commit SHA for precise tracking and isolated testing.

**Version History:**

| Version  | Release Date | High-Level Features & Changelog                                                 |
|:---------|:-------------|:--------------------------------------------------------------------------------|
| **v0.1** | 07/04/2026   | Core architecture, basic app features and Docker packaging.                     |
| **v0.2** | 12/05/2026   | Async event-driven systems and intermediate app features.                       |
| **v1.0** | 08/06/2026   | Driver tracking system, advanced features and error resolution.                 |

&nbsp;



### 🏁 Development Environment Setup

This section describes how to configure the local development environment and run the project in edit mode. Unlike the Docker Compose deployment described in the [Execution](/docs/pages/03-execution.md) page, this process starts each component independently to allow for code development and debugging.

#### Initial Requirements
To run this project locally, ensure you have the following tools installed on your system:
- **Git** — version control
- **Node.js** (includes npm) — frontend runtime environment
- **Angular CLI** — command-line tool for Angular projects
- **Java JDK** — version compatible with Spring Boot 3
- **Maven** — backend dependency management (optional, as the repository includes the Maven Wrapper)
- **Docker Desktop** — to bring up the auxiliary service containers
- **MySQL Workbench** — visual exploration of the relational database
- **MongoDB Compass** — visual exploration of the non-relational database

&nbsp;

#### Step-by-Step Execution

**1. Clone the repository**
```bash
git clone https://github.com/codeurjc-students/2025-2025-Frict.git Frict
cd Frict
```

**2. Bring up the auxiliary services**
The repository includes the `docker/docker-compose.dev-deps.yml` file, which starts all the required services for both development and testing at once: MySQL, MongoDB, and MinIO in their respective development and test instances, each on isolated ports so they do not interfere with each other.
```bash
docker compose -f docker/docker-compose.dev-deps.yml up -d
```

Once started, the development services are available at:
- **MySQL:** port `3306` — accessible from MySQL Workbench with user `user` and password `password`.
- **MongoDB:** port `27017` — accessible from MongoDB Compass with user `user` and password `password`.
- **MinIO:** administration console at `https://localhost:9001`.

> ℹ️ **NOTE:** The test containers stay on separate ports (MySQL on `3307`, MongoDB on `27018`, MinIO on `9002`) and are used automatically by Spring Boot's `test` profile, so development and test data never interfere with one another.

**3. Configure the backend environment variables (`.env`)**
Spring Boot reads the environment variables from a `.env` file located in the `backend/` folder. It must contain the following variables:
```env
SPRING_PROFILES_ACTIVE=local

MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=Frict
MYSQL_USERNAME=user
MYSQL_PASSWORD=password

MONGODB_URI=mongodb://user:password@localhost:27017/Frict?authSource=admin&directConnection=true

JWT_SECRET=A3F7B2E8C1D4F6A9B0E3C2D5F8A1B4E7C0D3F6A9
DB_ENCRYPTION_KEY=a1b2c3d4e5f6g7h8a1b2c3d4e5f6g7h8

CORS_ALLOWED_ORIGIN=https://localhost:4202

APP_STORAGE_ENDPOINT=https://localhost:9000
APP_STORAGE_PUBLIC_URL=https://localhost:9000
APP_STORAGE_BUCKET_NAME=images
APP_STORAGE_REGION=us-east-1
AWS_ACCESS_KEY_ID=user
AWS_SECRET_ACCESS_KEY=password

SENDER_MAIL_PORT=587
SENDER_MAIL_ADDRESS=address@domain.com
SENDER_MAIL_PASSWORD=aaaa aaaa aaaa aaaa
GOOGLE_AUTH_CLIENT_ID=123456789012-abcde.apps.googleusercontent.com
```

> ℹ️ **NOTE:** The mail and Google Auth variables require real credentials for those features to be operational. If you launch the application from the IDE's "Run" button instead of Maven, you might need to install an environment-variable loading plugin (such as EnvFile for IntelliJ IDEA).

**4. Start the backend**
With the auxiliary services running, start the backend from the `backend/` folder:
```bash
cd backend
./mvnw spring-boot:run
```
> ℹ️ **NOTE:** On Windows, the equivalent command is `mvnw.cmd spring-boot:run`.

**5. Start the frontend**
In a separate terminal, from the `frontend/` folder:
```bash
cd frontend
npm install
ng serve
```

**6. Access the application**
With both servers running, the application is accessible at:
- **Web Application:** `https://localhost:4202`
- **Swagger UI:** `https://localhost:8443/swagger-ui/index.html`
- **MinIO Console:** `https://localhost:9001`

&nbsp;

#### Running Tests

The test containers are already running since Step 2, so no additional configuration is required.

**Backend tests**
```bash
cd backend
./mvnw verify
```

**Frontend tests**
```bash
cd frontend
ng test --watch=false
```

> ℹ️ **NOTE:** Upon successful completion, the coverage reports are available at `backend/target/site/index.html` (backend, JaCoCo) and `frontend/coverage/index.html` (frontend, Istanbul).

&nbsp;

#### Using API Endpoints

Since the backend integrates the **OpenAPI** standard and Swagger UI, there is no need to manually craft HTTP requests or memorize JSON payload structures. The entire API is dynamically documented and fully interactive directly from your browser.

Once the Spring Boot application is running, follow these steps to test any endpoint:

1. Open your web browser and navigate to the Swagger UI panel: `https://localhost:8443/swagger-ui/index.html`


2. Scroll through the interface to explore the available controllers and expand the endpoint you wish to test (e.g., `GET /api/v1/products`).


3. Click the **"Try it out"** button located in the top right corner of the expanded endpoint block.


4. Fill in any required parameters or modify the request body (Swagger automatically generates the correct JSON structure for you to edit).


5. Click the **"Execute"** button to send the request to the local server. You will immediately see the server's response, status code, and headers directly below.

&nbsp;

[◀️](/docs/pages/03-execution.md) **Page 4. Development Guide** [▶️](/docs/pages/05-aws-architecture.md)

[⏪ Return to Index](/README.md)