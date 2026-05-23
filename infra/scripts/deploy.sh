#!/usr/bin/env bash
set -euo pipefail

STACK_NAME="${1:-frict}"
REGION="${AWS_DEFAULT_REGION:-eu-west-1}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CFN_DIR="$SCRIPT_DIR/../cloudformation"
PACKAGED="$CFN_DIR/packaged.yml"

if [ ! -f "$PACKAGED" ]; then
  echo "Run package.sh first — $PACKAGED not found."
  exit 1
fi

# Required parameters (override via env vars)
: "${DOMAIN_NAME:?Set DOMAIN_NAME (e.g. app.example.com)}"
: "${HOSTED_ZONE_ID:?Set HOSTED_ZONE_ID}"
: "${CF_CERT_ARN:?Set CF_CERT_ARN (ACM cert in us-east-1 for CloudFront)}"

IMAGE_TAG="${IMAGE_TAG:-latest}"
ALB_CERT_ARN="${ALB_CERT_ARN:-}"
ENABLE_DOCDB="${ENABLE_DOCDB:-true}"
MIN_TASKS="${MIN_TASKS:-2}"
MAX_TASKS="${MAX_TASKS:-8}"

echo "Deploying stack: $STACK_NAME to $REGION"
aws cloudformation deploy \
  --template-file "$PACKAGED" \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
  --parameter-overrides \
    EnvironmentName="$STACK_NAME" \
    DomainName="$DOMAIN_NAME" \
    HostedZoneId="$HOSTED_ZONE_ID" \
    ImageTag="$IMAGE_TAG" \
    MinTaskCount="$MIN_TASKS" \
    MaxTaskCount="$MAX_TASKS" \
    EnableDocDB="$ENABLE_DOCDB" \
    ALBCertificateArn="$ALB_CERT_ARN" \
    CloudFrontCertificateArn="$CF_CERT_ARN"

echo ""
echo "Stack outputs:"
aws cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query "Stacks[0].Outputs" \
  --output table
