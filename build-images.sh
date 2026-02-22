#!/bin/bash

# Google Cloud Run Deployment Script for Spring Boot Microservices
# This script builds Docker images and pushes them to Google Container Registry

# Configuration
PROJECT_ID="hackathon-java"
REGION="us-central1"
REGISTRY="gcr.io"

# Service names and ports
declare -A SERVICES=(
    ["config-server"]="8888"
    ["auth-service"]="8081"
    ["user-service"]="8082"
    ["loan-service"]="8083"
    ["api-gateway"]="8080"
)

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Building and Pushing Docker Images${NC}"
echo -e "${BLUE}=========================================${NC}"

# Check if PROJECT_ID is set
if [ "$PROJECT_ID" == "YOUR_GCP_PROJECT_ID" ]; then
    echo -e "${RED}Error: Please set your GCP PROJECT_ID in this script${NC}"
    exit 1
fi

# Authenticate with Google Cloud
echo -e "${GREEN}Authenticating with Google Cloud...${NC}"
gcloud auth configure-docker

# Build and push each service
for SERVICE in "${!SERVICES[@]}"; do
    echo -e "${GREEN}Building $SERVICE...${NC}"
    
    # Build Docker image with host network to resolve DNS issues
    docker build --network=host -t $REGISTRY/$PROJECT_ID/$SERVICE:latest -f $SERVICE/Dockerfile .
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Successfully built $SERVICE${NC}"
        
        # Push to Container Registry
        echo -e "${GREEN}Pushing $SERVICE to Container Registry...${NC}"
        docker push $REGISTRY/$PROJECT_ID/$SERVICE:latest
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}Successfully pushed $SERVICE${NC}"
        else
            echo -e "${RED}Failed to push $SERVICE${NC}"
            exit 1
        fi
    else
        echo -e "${RED}Failed to build $SERVICE${NC}"
        exit 1
    fi
    
    echo ""
done

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}All images built and pushed successfully!${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""
echo -e "${GREEN}Next step: Run deploy-services.sh to deploy to Cloud Run${NC}"
