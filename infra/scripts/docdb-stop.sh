#!/usr/bin/env bash
set -euo pipefail

CLUSTER_ID="${1:-frict-docdb}"
REGION="${AWS_DEFAULT_REGION:-eu-west-1}"

echo "Stopping DocumentDB cluster: $CLUSTER_ID"
aws docdb stop-db-cluster --db-cluster-identifier "$CLUSTER_ID" --region "$REGION"
echo "Stop command sent. Cluster will become 'stopped' in a few minutes."
