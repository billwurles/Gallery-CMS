#!/bin/bash

if [ "$EUID" -ne 0 ]; then
    echo "This script must be run as root. Restarting with sudo..."
    exec sudo "$0" "$@"
fi

# Get the directory where the script is stored
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Navigate to the script's directory
cd "$SCRIPT_DIR" || { echo "Directory not found!"; exit 1; }

echo "Stopping Docker containers..."
docker-compose down

echo "Pulling latest changes from Git..."
git pull origin master

echo "Rebuilding Docker images..."
docker-compose up --build -d

echo "Cleaning up unused Docker resources..."
docker system prune -f

echo "Deployment complete!"
docker ps  # Show running containers
