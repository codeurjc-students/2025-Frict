#!/bin/bash
# Git Bash: ./docker/publish.sh dev (from root)
set -e  # Stop immediately if any error

# 1. Check tag
if [ -z "$1" ]; then
    echo "❌ Error: Tag missing"
    echo "Use: ./publish.sh <version>"
    echo "Ejemplo: ./publish.sh v1.0"
    exit 1
fi

TAG=$1

# 2. Automatic route calculation
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Search for .env file
ENV_FILE="$SCRIPT_DIR/../backend/.env"

# Search for project folder
PROJECT_ROOT="$SCRIPT_DIR/.."

# 3. Load secrets backend/.env
if [ -f "$ENV_FILE" ]; then
    echo "🔓 Loading secrets from: $ENV_FILE"
    # Export variables ignoring comments
    export $(grep -v '^#' "$ENV_FILE" | xargs)
else
    echo "❌ Critial error: .env file not found in:"
    echo "   $ENV_FILE"
    echo "   Check the .env file is in 'backend' folder"
    exit 1
fi

FULL_IMAGE="$DOCKER_USER/$IMAGE_NAME:$TAG"

# 4. Go to root folder
cd "$PROJECT_ROOT"

echo "🔨 Building: $FULL_IMAGE"
echo "📂 Context:     $(pwd)"
docker build -t "$FULL_IMAGE" -f "$SCRIPT_DIR/Dockerfile" .

echo "🔑 DockerHub Login..."
echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

echo "🚀 Uploading image..."
docker push "$FULL_IMAGE"

echo "✅ Image successfully published"