#!/usr/bin/env bash
set -euo pipefail

STACK_NAME="${1:-frict}"
REGION="${AWS_DEFAULT_REGION:-eu-west-1}"
ARTIFACT_BUCKET="${STACK_NAME}-cfn-artifacts-$(aws sts get-caller-identity --query Account --output text)-${REGION}"

# Ensure artifact bucket exists
if ! aws s3 head-bucket --bucket "$ARTIFACT_BUCKET" 2>/dev/null; then
  echo "Creating artifact bucket: $ARTIFACT_BUCKET"
  aws s3 mb "s3://$ARTIFACT_BUCKET" --region "$REGION"
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CFN_DIR="$SCRIPT_DIR/../cloudformation"

echo "Packaging CloudFormation templates..."
aws cloudformation package \
  --template-file "$CFN_DIR/main.yml" \
  --s3-bucket "$ARTIFACT_BUCKET" \
  --s3-prefix "cfn-templates" \
  --output-template-file "$CFN_DIR/packaged.yml" \
  --region "$REGION"

echo "Packaged template: $CFN_DIR/packaged.yml"
echo "Artifact bucket: $ARTIFACT_BUCKET"
