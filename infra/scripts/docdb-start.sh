#!/usr/bin/env bash
set -euo pipefail

CLUSTER_ID="${1:-frict-docdb}"
REGION="${AWS_DEFAULT_REGION:-eu-west-1}"

echo "Starting DocumentDB cluster: $CLUSTER_ID"
aws docdb start-db-cluster --db-cluster-identifier "$CLUSTER_ID" --region "$REGION"

echo "Waiting for cluster to become available..."
aws docdb wait db-cluster-available --db-cluster-identifier "$CLUSTER_ID" --region "$REGION"
echo "Cluster $CLUSTER_ID is now available."
