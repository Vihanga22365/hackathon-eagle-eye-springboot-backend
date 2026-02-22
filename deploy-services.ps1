# Google Cloud Run Deployment Script (PowerShell)
# Deploys all microservices to Cloud Run in the correct order

# Configuration
$PROJECT_ID = "hackathon-java"
$REGION = "us-central1"
$REGISTRY = "gcr.io"

Write-Host "=========================================" -ForegroundColor Blue
Write-Host "Deploying Services to Cloud Run" -ForegroundColor Blue
Write-Host "=========================================" -ForegroundColor Blue

# Check if PROJECT_ID is set
if ($PROJECT_ID -eq "YOUR_GCP_PROJECT_ID") {
    Write-Host "Error: Please set your GCP PROJECT_ID in this script" -ForegroundColor Red
    exit 1
}

# Check if required environment variables file exists
if (-not (Test-Path ".env.cloud")) {
    Write-Host "Error: .env.cloud file not found!" -ForegroundColor Red
    Write-Host "Please create .env.cloud with your Firebase and JWT credentials" -ForegroundColor Yellow
    exit 1
}

# Load environment variables from .env.cloud
Get-Content ".env.cloud" | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.+)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        Set-Item -Path "env:$name" -Value $value
    }
}

# Deploy Config Server (must be deployed first)
Write-Host "Deploying config-server..." -ForegroundColor Green
gcloud run deploy config-server `
    --image="$REGISTRY/$PROJECT_ID/config-server:latest" `
    --platform=managed `
    --region=$REGION `
    --allow-unauthenticated `
    --port=8888 `
    --memory=512Mi `
    --cpu=1 `
    --max-instances=3 `
    --env-vars-file="config-server-env.yaml" `
    --project=$PROJECT_ID

if ($LASTEXITCODE -eq 0) {
    $CONFIG_SERVER_URL = gcloud run services describe config-server --platform=managed --region=$REGION --format='value(status.url)' | Out-String
    $CONFIG_SERVER_URL = $CONFIG_SERVER_URL.Trim()
    Write-Host "Config Server deployed at: $CONFIG_SERVER_URL" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy config-server" -ForegroundColor Red
    exit 1
}

# Deploy Auth Service
Write-Host "Deploying auth-service..." -ForegroundColor Green
gcloud run deploy auth-service `
    --image="$REGISTRY/$PROJECT_ID/auth-service:latest" `
    --platform=managed `
    --region=$REGION `
    --allow-unauthenticated `
    --port=8081 `
    --memory=1Gi `
    --cpu=1 `
    --max-instances=5 `
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,CONFIG_SERVER_URL=$CONFIG_SERVER_URL,SPRING_CONFIG_IMPORT=optional:configserver:$CONFIG_SERVER_URL,JWT_SECRET=$env:JWT_SECRET,FIREBASE_PROJECT_ID=$env:FIREBASE_PROJECT_ID,FIREBASE_PRIVATE_KEY_ID=$env:FIREBASE_PRIVATE_KEY_ID,FIREBASE_CLIENT_EMAIL=$env:FIREBASE_CLIENT_EMAIL,FIREBASE_CLIENT_ID=$env:FIREBASE_CLIENT_ID,FIREBASE_CLIENT_CERT_URL=$env:FIREBASE_CLIENT_CERT_URL,FIREBASE_DATABASE_URL=$env:FIREBASE_DATABASE_URL" `
    --set-secrets="FIREBASE_PRIVATE_KEY=firebase-private-key:latest" `
    --project=$PROJECT_ID

if ($LASTEXITCODE -eq 0) {
    $AUTH_SERVICE_URL = gcloud run services describe auth-service --platform=managed --region=$REGION --format='value(status.url)' | Out-String
    $AUTH_SERVICE_URL = $AUTH_SERVICE_URL.Trim()
    Write-Host "Auth Service deployed at: $AUTH_SERVICE_URL" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy auth-service" -ForegroundColor Red
    exit 1
}

# Deploy User Service
Write-Host "Deploying user-service..." -ForegroundColor Green
gcloud run deploy user-service `
    --image="$REGISTRY/$PROJECT_ID/user-service:latest" `
    --platform=managed `
    --region=$REGION `
    --allow-unauthenticated `
    --port=8082 `
    --memory=1Gi `
    --cpu=1 `
    --max-instances=5 `
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,CONFIG_SERVER_URL=$CONFIG_SERVER_URL,SPRING_CONFIG_IMPORT=optional:configserver:$CONFIG_SERVER_URL,FIREBASE_PROJECT_ID=$env:FIREBASE_PROJECT_ID,FIREBASE_PRIVATE_KEY_ID=$env:FIREBASE_PRIVATE_KEY_ID,FIREBASE_CLIENT_EMAIL=$env:FIREBASE_CLIENT_EMAIL,FIREBASE_CLIENT_ID=$env:FIREBASE_CLIENT_ID,FIREBASE_CLIENT_CERT_URL=$env:FIREBASE_CLIENT_CERT_URL,FIREBASE_DATABASE_URL=$env:FIREBASE_DATABASE_URL" `
    --set-secrets="FIREBASE_PRIVATE_KEY=firebase-private-key:latest" `
    --project=$PROJECT_ID

if ($LASTEXITCODE -eq 0) {
    $USER_SERVICE_URL = gcloud run services describe user-service --platform=managed --region=$REGION --format='value(status.url)' | Out-String
    $USER_SERVICE_URL = $USER_SERVICE_URL.Trim()
    Write-Host "User Service deployed at: $USER_SERVICE_URL" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy user-service" -ForegroundColor Red
    exit 1
}

# Deploy Loan Service
Write-Host "Deploying loan-service..." -ForegroundColor Green
gcloud run deploy loan-service `
    --image="$REGISTRY/$PROJECT_ID/loan-service:latest" `
    --platform=managed `
    --region=$REGION `
    --allow-unauthenticated `
    --port=8083 `
    --memory=512Mi `
    --cpu=1 `
    --max-instances=5 `
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,CONFIG_SERVER_URL=$CONFIG_SERVER_URL,SPRING_CONFIG_IMPORT=optional:configserver:$CONFIG_SERVER_URL,FIREBASE_PROJECT_ID=$env:FIREBASE_PROJECT_ID,FIREBASE_PRIVATE_KEY_ID=$env:FIREBASE_PRIVATE_KEY_ID,FIREBASE_CLIENT_EMAIL=$env:FIREBASE_CLIENT_EMAIL,FIREBASE_CLIENT_ID=$env:FIREBASE_CLIENT_ID,FIREBASE_CLIENT_CERT_URL=$env:FIREBASE_CLIENT_CERT_URL,FIREBASE_DATABASE_URL=$env:FIREBASE_DATABASE_URL" `
    --set-secrets="FIREBASE_PRIVATE_KEY=firebase-private-key:latest" `
    --project=$PROJECT_ID

if ($LASTEXITCODE -eq 0) {
    $LOAN_SERVICE_URL = gcloud run services describe loan-service --platform=managed --region=$REGION --format='value(status.url)' | Out-String
    $LOAN_SERVICE_URL = $LOAN_SERVICE_URL.Trim()
    Write-Host "Loan Service deployed at: $LOAN_SERVICE_URL" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy loan-service" -ForegroundColor Red
    exit 1
}

# Deploy API Gateway
Write-Host "Deploying api-gateway..." -ForegroundColor Green
gcloud run deploy api-gateway `
    --image="$REGISTRY/$PROJECT_ID/api-gateway:latest" `
    --platform=managed `
    --region=$REGION `
    --allow-unauthenticated `
    --port=8080 `
    --memory=1Gi `
    --cpu=1 `
    --max-instances=10 `
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,CONFIG_SERVER_URL=$CONFIG_SERVER_URL,SPRING_CONFIG_IMPORT=optional:configserver:$CONFIG_SERVER_URL,JWT_SECRET=$env:JWT_SECRET,AUTH_SERVICE_URL=$AUTH_SERVICE_URL,USER_SERVICE_URL=$USER_SERVICE_URL,LOAN_SERVICE_URL=$LOAN_SERVICE_URL" `
    --project=$PROJECT_ID

if ($LASTEXITCODE -eq 0) {
    $GATEWAY_URL = gcloud run services describe api-gateway --platform=managed --region=$REGION --format='value(status.url)' | Out-String
    $GATEWAY_URL = $GATEWAY_URL.Trim()
    Write-Host "API Gateway deployed at: $GATEWAY_URL" -ForegroundColor Green
} else {
    Write-Host "Failed to deploy api-gateway" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Blue
Write-Host "Deployment Summary" -ForegroundColor Blue
Write-Host "=========================================" -ForegroundColor Blue
Write-Host "Config Server: $CONFIG_SERVER_URL" -ForegroundColor Green
Write-Host "Auth Service: $AUTH_SERVICE_URL" -ForegroundColor Green
Write-Host "User Service: $USER_SERVICE_URL" -ForegroundColor Green
Write-Host "Loan Service: $LOAN_SERVICE_URL" -ForegroundColor Green
Write-Host "API Gateway: $GATEWAY_URL" -ForegroundColor Green
Write-Host ""
Write-Host "Update your Postman collection with the API Gateway URL:" -ForegroundColor Yellow
Write-Host "$GATEWAY_URL" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Blue
