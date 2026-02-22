#!/bin/bash

# Build Docker images in Google Cloud Build (Linux/Mac)
# This avoids local Docker build time on your PC.

PROJECT_ID="hackathon-java"
REGION="us-central1"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Building Docker Images in Cloud Build${NC}"
echo -e "${BLUE}=========================================${NC}"

if [ "$PROJECT_ID" == "YOUR_GCP_PROJECT_ID" ]; then
  echo -e "${RED}Error: Please set your GCP PROJECT_ID in this script${NC}"
  exit 1
fi

gcloud config set project "$PROJECT_ID"

echo -e "${GREEN}Submitting remote build to Cloud Build...${NC}"
gcloud builds submit \
  --config="cloudbuild-all-services.yaml" \
  --project="$PROJECT_ID" \
  --region="$REGION"

if [ $? -ne 0 ]; then
  echo -e "${RED}Cloud Build failed${NC}"
  exit 1
fi

echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Cloud Build completed successfully${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}Next step: Run deploy-services.sh${NC}"
