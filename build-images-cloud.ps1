# Build Docker images in Google Cloud Build (PowerShell)
# This avoids local Docker build time on your PC.

$PROJECT_ID = "hackathon-java"
$REGION = "us-central1"

Write-Host "=========================================" -ForegroundColor Blue
Write-Host "Building Docker Images in Cloud Build" -ForegroundColor Blue
Write-Host "=========================================" -ForegroundColor Blue

if ($PROJECT_ID -eq "YOUR_GCP_PROJECT_ID") {
    Write-Host "Error: Please set your GCP PROJECT_ID in this script" -ForegroundColor Red
    exit 1
}

gcloud config set project $PROJECT_ID

Write-Host "Submitting remote build to Cloud Build..." -ForegroundColor Green
gcloud builds submit `
    --config="cloudbuild-all-services.yaml" `
    --project=$PROJECT_ID `
    --region=$REGION

if ($LASTEXITCODE -ne 0) {
    Write-Host "Cloud Build failed" -ForegroundColor Red
    exit 1
}

Write-Host "" 
Write-Host "=========================================" -ForegroundColor Blue
Write-Host "Cloud Build completed successfully" -ForegroundColor Blue
Write-Host "=========================================" -ForegroundColor Blue
Write-Host "Next step: Run deploy-services.ps1" -ForegroundColor Green
