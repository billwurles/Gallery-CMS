#!/bin/bash

if [ "$EUID" -ne 0 ]; then
    echo "This script must be run as root. Restarting with sudo..."
    exec sudo "$0" "$@"
fi

# Get the directory where the script is stored
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Navigate to the script's directory
cd "$SCRIPT_DIR" || { echo "Failed to navigate to docker project directory!"; exit 1; }

# Check if Docker and docker-compose are installed
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "ERROR: docker-compose is not installed. Please install docker-compose first."
    exit 1
fi

# Check if a docker-compose file exists
if [ ! -f "docker-compose.yml" ] && [ ! -f "docker-compose.yaml" ] && [ ! -f "docker-compose.override.yml" ] && [ ! -f "docker-compose.override.yaml" ]; then
    echo "ERROR: No docker-compose file found in the current directory."
    echo "This script must be run from the root of a docker-compose project"
    exit 1
fi

echo "Pulling latest changes from Git..."
git pull origin master || { echo "ERROR: Failed to pull latest changes. Aborting deployment."; exit 1; }

echo "Building new Docker images..."
if docker-compose build; then
    echo "Build successful! Restarting Docker containers..."

    # Stop and remove old containers
    docker-compose down

    # Start new containers in detached mode
    docker-compose up -d

    echo "Cleaning up unused Docker resources..."
    docker system prune -f

    echo "Deployment complete!"
    docker ps  # Show running containers
else
    echo "ERROR: Build failed. Keeping the current containers running."
    exit 1
fi
