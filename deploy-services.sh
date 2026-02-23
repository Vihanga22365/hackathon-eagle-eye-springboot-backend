#!/bin/bash

set -euo pipefail

# Google Cloud Run Deployment Script
# Deploys all microservices to Cloud Run in the correct order

# Configuration
PROJECT_ID="hackathon-java"
REGION="us-central1"
REGISTRY="gcr.io"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Deploying Services to Cloud Run${NC}"
echo -e "${BLUE}=========================================${NC}"

# Wait for a Cloud Run service to become Ready before deploying dependents
wait_for_service_ready() {
    local service_name="$1"
    local max_attempts=30
    local attempt=1

    echo -e "${BLUE}Waiting for ${service_name} to become ready...${NC}"
    while [ $attempt -le $max_attempts ]; do
        local ready_status
        ready_status=$(gcloud run services describe "$service_name" \
            --platform=managed \
            --region="$REGION" \
            --project="$PROJECT_ID" \
            --format="value(status.conditions[?type='Ready'].status)")

        if [ "$ready_status" = "True" ]; then
            echo -e "${GREEN}${service_name} is ready.${NC}"
            return 0
        fi

        sleep 5
        attempt=$((attempt + 1))
    done

    echo -e "${RED}Timeout: ${service_name} did not become ready in time.${NC}"
    return 1
}

# Check if PROJECT_ID is set
if [ "$PROJECT_ID" == "YOUR_GCP_PROJECT_ID" ]; then
    echo -e "${RED}Error: Please set your GCP PROJECT_ID in this script${NC}"
    exit 1
fi

# Check if required environment variables file exists
if [ ! -f ".env.cloud" ]; then
    echo -e "${RED}Error: .env.cloud file not found!${NC}"
    echo -e "${YELLOW}Please create .env.cloud with your Firebase and JWT credentials${NC}"
    exit 1
fi

# Source environment variables
source .env.cloud

# Deploy Config Server (must be deployed first)
echo -e "${GREEN}Deploying config-server...${NC}"
gcloud run deploy config-server \
    --image=$REGISTRY/$PROJECT_ID/config-server:latest \
    --platform=managed \
    --region=$REGION \
    --allow-unauthenticated \
    --port=8888 \
    --memory=512Mi \
    --cpu=1 \
    --min-instances=1 \
    --max-instances=3 \
    --concurrency=40 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloud" \
    --project=$PROJECT_ID

CONFIG_SERVER_URL=$(gcloud run services describe config-server --platform=managed --region=$REGION --project=$PROJECT_ID --format='value(status.url)')
echo -e "${GREEN}Config Server deployed at: $CONFIG_SERVER_URL${NC}"
wait_for_service_ready "config-server"

# Deploy Auth Service
echo -e "${GREEN}Deploying auth-service...${NC}"
gcloud run deploy auth-service \
    --image=$REGISTRY/$PROJECT_ID/auth-service:latest \
    --platform=managed \
    --region=$REGION \
    --allow-unauthenticated \
    --port=8081 \
    --memory=1Gi \
    --cpu=1 \
    --min-instances=1 \
    --max-instances=5 \
    --concurrency=40 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,CONFIG_SERVER_URL=$CONFIG_SERVER_URL,SPRING_CONFIG_IMPORT=configserver:$CONFIG_SERVER_URL,JWT_SECRET=$JWT_SECRET,FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID,FIREBASE_PRIVATE_KEY_ID=$FIREBASE_PRIVATE_KEY_ID,FIREBASE_CLIENT_EMAIL=$FIREBASE_CLIENT_EMAIL,FIREBASE_CLIENT_ID=$FIREBASE_CLIENT_ID,FIREBASE_CLIENT_CERT_URL=$FIREBASE_CLIENT_CERT_URL,FIREBASE_DATABASE_URL=$FIREBASE_DATABASE_URL" \
    --set-secrets="FIREBASE_PRIVATE_KEY=firebase-private-key:latest" \
    --project=$PROJECT_ID

AUTH_SERVICE_URL=$(gcloud run services describe auth-service --platform=managed --region=$REGION --project=$PROJECT_ID --format='value(status.url)')
echo -e "${GREEN}Auth Service deployed at: $AUTH_SERVICE_URL${NC}"

# Deploy User Service
echo -e "${GREEN}Deploying user-service...${NC}"
gcloud run deploy user-service \
    --image=$REGISTRY/$PROJECT_ID/user-service:latest \
    --platform=managed \
    --region=$REGION \
    --allow-unauthenticated \
    --port=8082 \
    --memory=1Gi \
    --cpu=1 \
    --min-instances=1 \
    --max-instances=5 \
    --concurrency=40 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,CONFIG_SERVER_URL=$CONFIG_SERVER_URL,SPRING_CONFIG_IMPORT=configserver:$CONFIG_SERVER_URL,FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID,FIREBASE_PRIVATE_KEY_ID=$FIREBASE_PRIVATE_KEY_ID,FIREBASE_CLIENT_EMAIL=$FIREBASE_CLIENT_EMAIL,FIREBASE_CLIENT_ID=$FIREBASE_CLIENT_ID,FIREBASE_CLIENT_CERT_URL=$FIREBASE_CLIENT_CERT_URL,FIREBASE_DATABASE_URL=$FIREBASE_DATABASE_URL" \
    --set-secrets="FIREBASE_PRIVATE_KEY=firebase-private-key:latest" \
    --project=$PROJECT_ID

USER_SERVICE_URL=$(gcloud run services describe user-service --platform=managed --region=$REGION --project=$PROJECT_ID --format='value(status.url)')
echo -e "${GREEN}User Service deployed at: $USER_SERVICE_URL${NC}"

# Deploy Loan Service
echo -e "${GREEN}Deploying loan-service...${NC}"
gcloud run deploy loan-service \
    --image=$REGISTRY/$PROJECT_ID/loan-service:latest \
    --platform=managed \
    --region=$REGION \
    --allow-unauthenticated \
    --port=8083 \
    --memory=512Mi \
    --cpu=1 \
    --min-instances=1 \
    --max-instances=5 \
    --concurrency=40 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,CONFIG_SERVER_URL=$CONFIG_SERVER_URL,SPRING_CONFIG_IMPORT=configserver:$CONFIG_SERVER_URL,FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID,FIREBASE_PRIVATE_KEY_ID=$FIREBASE_PRIVATE_KEY_ID,FIREBASE_CLIENT_EMAIL=$FIREBASE_CLIENT_EMAIL,FIREBASE_CLIENT_ID=$FIREBASE_CLIENT_ID,FIREBASE_CLIENT_CERT_URL=$FIREBASE_CLIENT_CERT_URL,FIREBASE_DATABASE_URL=$FIREBASE_DATABASE_URL" \
    --set-secrets="FIREBASE_PRIVATE_KEY=firebase-private-key:latest" \
    --project=$PROJECT_ID

LOAN_SERVICE_URL=$(gcloud run services describe loan-service --platform=managed --region=$REGION --project=$PROJECT_ID --format='value(status.url)')
echo -e "${GREEN}Loan Service deployed at: $LOAN_SERVICE_URL${NC}"

# Deploy API Gateway
echo -e "${GREEN}Deploying api-gateway...${NC}"
gcloud run deploy api-gateway \
    --image=$REGISTRY/$PROJECT_ID/api-gateway:latest \
    --platform=managed \
    --region=$REGION \
    --allow-unauthenticated \
    --port=8080 \
    --memory=1Gi \
    --cpu=1 \
    --min-instances=2 \
    --max-instances=10 \
    --concurrency=80 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,CONFIG_SERVER_URL=$CONFIG_SERVER_URL,SPRING_CONFIG_IMPORT=configserver:$CONFIG_SERVER_URL,JWT_SECRET=$JWT_SECRET,AUTH_SERVICE_URL=$AUTH_SERVICE_URL,USER_SERVICE_URL=$USER_SERVICE_URL,LOAN_SERVICE_URL=$LOAN_SERVICE_URL" \
    --project=$PROJECT_ID

GATEWAY_URL=$(gcloud run services describe api-gateway --platform=managed --region=$REGION --project=$PROJECT_ID --format='value(status.url)')
echo -e "${GREEN}API Gateway deployed at: $GATEWAY_URL${NC}"

echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Deployment Summary${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}Config Server:${NC} $CONFIG_SERVER_URL"
echo -e "${GREEN}Auth Service:${NC} $AUTH_SERVICE_URL"
echo -e "${GREEN}User Service:${NC} $USER_SERVICE_URL"
echo -e "${GREEN}Loan Service:${NC} $LOAN_SERVICE_URL"
echo -e "${GREEN}API Gateway:${NC} $GATEWAY_URL"
echo ""
echo -e "${YELLOW}Update your Postman collection with the API Gateway URL:${NC}"
echo -e "${YELLOW}$GATEWAY_URL${NC}"
echo -e "${BLUE}=========================================${NC}"
