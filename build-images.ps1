# Google Cloud Run Deployment Script for Spring Boot Microservices (PowerShell)
# This script builds Docker images and pushes them to Google Container Registry

# Configuration
$PROJECT_ID = "hackathon-java"
$REGION = "us-central1"
$REGISTRY = "gcr.io"

# Service names
$SERVICES = @(
    "config-server",
    "auth-service",
    "user-service",
    "loan-service",
    "api-gateway"
)

Write-Host "=========================================" -ForegroundColor Blue
Write-Host "Building and Pushing Docker Images" -ForegroundColor Blue
Write-Host "=========================================" -ForegroundColor Blue

# Check if PROJECT_ID is set
if ($PROJECT_ID -eq "YOUR_GCP_PROJECT_ID") {
    Write-Host "Error: Please set your GCP PROJECT_ID in this script" -ForegroundColor Red
    exit 1
}

# Authenticate with Google Cloud
Write-Host "Authenticating with Google Cloud..." -ForegroundColor Green
gcloud auth configure-docker

# Build and push each service
foreach ($SERVICE in $SERVICES) {
    Write-Host "Building $SERVICE..." -ForegroundColor Green
    
    # Build Docker image with host network to resolve DNS issues
    docker build --network=host -t "$REGISTRY/$PROJECT_ID/${SERVICE}:latest" -f "$SERVICE/Dockerfile" .
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Successfully built $SERVICE" -ForegroundColor Green
        
        # Push to Container Registry
        Write-Host "Pushing $SERVICE to Container Registry..." -ForegroundColor Green
        docker push "$REGISTRY/$PROJECT_ID/${SERVICE}:latest"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Successfully pushed $SERVICE" -ForegroundColor Green
        } else {
            Write-Host "Failed to push $SERVICE" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "Failed to build $SERVICE" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
}

Write-Host "=========================================" -ForegroundColor Blue
Write-Host "All images built and pushed successfully!" -ForegroundColor Blue
Write-Host "=========================================" -ForegroundColor Blue
Write-Host ""
Write-Host "Next step: Run deploy-services.ps1 to deploy to Cloud Run" -ForegroundColor Green
