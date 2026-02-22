# ✅ Your Project is Ready for Cloud Deployment!

## Configuration Applied

### GCP Project

- **Project ID**: `hackathon-java`
- **Region**: `us-central1`

### Firebase Project

- **Project ID**: `hackathon-f1c4a`
- **Service Account**: `firebase-adminsdk-fbsvc@hackathon-f1c4a.iam.gserviceaccount.com`
- **Database URL**: `https://hackathon-f1c4a-default-rtdb.firebaseio.com`

### Files Updated

✅ [.env.cloud](.env.cloud) - Environment variables configured
✅ [build-images.ps1](build-images.ps1) - Windows build script configured
✅ [build-images.sh](build-images.sh) - Linux/Mac build script configured
✅ [deploy-services.ps1](deploy-services.ps1) - Windows deployment script configured
✅ [deploy-services.sh](deploy-services.sh) - Linux/Mac deployment script configured

---

## ⚠️ IMPORTANT: Before Deployment

### 1. Set a Strong JWT Secret

Edit [.env.cloud](.env.cloud) and change the JWT_SECRET to a strong random string:

```properties
JWT_SECRET=your-super-secure-jwt-secret-key-change-this-to-a-random-32plus-character-string
```

Generate a secure JWT secret:

```powershell
# PowerShell - Generate random 64-character secret
-join ((65..90) + (97..122) + (48..57) + (33,35,36,37,38,42,43,45,61) | Get-Random -Count 64 | ForEach-Object {[char]$_})
```

### 2. Enable Required GCP APIs

```powershell
gcloud config set project hackathon-java
gcloud services enable run.googleapis.com
gcloud services enable containerregistry.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable secretmanager.googleapis.com
```

### 3. Upload Firebase Private Key to Secret Manager

```bash
# Create secret from the existing firebase-service-account.json file
gcloud secrets create firebase-private-key --data-file=auth-service/src/main/resources/firebase-service-account.json --replication-policy=automatic

# Grant access to Cloud Run (get your project number first)
gcloud projects describe hackathon-java --format="value(projectNumber)"

# Use the project number in this command:
gcloud secrets add-iam-policy-binding firebase-private-key --member=serviceAccount:YOUR_PROJECT_NUMBER-compute@developer.gserviceaccount.com --role=roles/secretmanager.secretAccessor
```

---

## 🚀 Initial Deployment (3 Steps)

### Step 1: Build Docker Images

From Command Prompt (cmd):

```cmd
powershell -ExecutionPolicy Bypass -File .\build-images.ps1
```

⏱️ This will take **5-10 minutes** to build all 6 services

Or if you're already in PowerShell:

```powershell
.\build-images.ps1
```

### ✅ Faster Option (No local Docker build)

Build all images in Google Cloud Build instead of your local PC:

```powershell
.\build-images-cloud.ps1
```

Or Linux/Mac:

```bash
./build-images-cloud.sh
```

This uploads source once and builds in GCP, so your laptop/PC CPU is not used for Docker builds.

### Step 2: Deploy Services to Cloud Run

From Command Prompt (cmd):

```cmd
powershell -ExecutionPolicy Bypass -File .\deploy-services.ps1
```

⏱️ This will take **3-5 minutes** to deploy all services

Or if you're already in PowerShell:

```powershell
.\deploy-services.ps1
```

### Step 3: Get Your API Gateway URL

The script will display all service URLs. Copy the **API Gateway URL** like:

```
API Gateway: https://api-gateway-xxxxx-uc.a.run.app
```

---

## 🔄 Making Changes & Redeployment

### Scenario 1: Code Changes (Java/Spring Boot)

When you modify service code (controllers, services, models, etc.):

#### Step 1: Make Your Changes

Edit your Java files in any service (auth-service, user-service, etc.)

#### Step 2: Rebuild the Specific Service

```cmd
REM Build only the changed service (e.g., user-service)
docker build -t gcr.io/hackathon-java/user-service:latest -f user-service/Dockerfile .
```

#### Step 3: Push to Container Registry

```cmd
docker push gcr.io/hackathon-java/user-service:latest
```

#### Step 4: Redeploy the Service

```cmd
gcloud run deploy user-service --image=gcr.io/hackathon-java/user-service:latest --region=us-central1
```

⏱️ **Time**: 2-3 minutes per service

---

### Scenario 2: Configuration Changes

When you modify application.yml or cloud configs:

#### Step 1: Update Config Files

Edit files in `config-server/src/main/resources/configs/`

#### Step 2: Rebuild & Redeploy Config Server

```cmd
REM Rebuild config-server
docker build -t gcr.io/hackathon-java/config-server:latest -f config-server/Dockerfile .

REM Push to registry
docker push gcr.io/hackathon-java/config-server:latest

REM Redeploy
gcloud run deploy config-server --image=gcr.io/hackathon-java/config-server:latest --region=us-central1
```

#### Step 3: Restart Dependent Services

```cmd
REM Force new revision for services to pick up new config
gcloud run services update user-service --region=us-central1
gcloud run services update auth-service --region=us-central1
gcloud run services update api-gateway --region=us-central1
```

⏱️ **Time**: 3-5 minutes

---

### Scenario 3: Rebuild & Redeploy ALL Services

When you have multiple changes across services:

```cmd
REM Rebuild and push all images
powershell -ExecutionPolicy Bypass -File .\build-images.ps1

REM Redeploy all services
powershell -ExecutionPolicy Bypass -File .\deploy-services.ps1
```

⏱️ **Time**: 10-15 minutes

---

### Scenario 4: Environment Variable Changes

When you change JWT secret, Firebase credentials, or service URLs:

#### Step 1: Update .env.cloud

Edit your environment variables in `.env.cloud`

#### Step 2: Redeploy with New Environment Variables

```cmd
REM Source the new environment variables (PowerShell)
powershell -ExecutionPolicy Bypass -File .\deploy-services.ps1
```

Or update a specific service:

```cmd
gcloud run services update auth-service ^
  --set-env-vars="JWT_SECRET=new-secret-here" ^
  --region=us-central1
```

⏱️ **Time**: 1-2 minutes per service

---

### Quick Redeployment Commands Reference

#### Single Service (Fast - 2-3 min)

```cmd
REM Example: Redeploy user-service after code changes
docker build -t gcr.io/hackathon-java/user-service:latest -f user-service/Dockerfile .
docker push gcr.io/hackathon-java/user-service:latest
gcloud run deploy user-service --image=gcr.io/hackathon-java/user-service:latest --region=us-central1
```

#### Multiple Services (Medium - 5-10 min)

```cmd
REM Build and push multiple services
docker build -t gcr.io/hackathon-java/auth-service:latest -f auth-service/Dockerfile .
docker build -t gcr.io/hackathon-java/user-service:latest -f user-service/Dockerfile .
docker push gcr.io/hackathon-java/auth-service:latest
docker push gcr.io/hackathon-java/user-service:latest

REM Deploy them
gcloud run deploy auth-service --image=gcr.io/hackathon-java/auth-service:latest --region=us-central1
gcloud run deploy user-service --image=gcr.io/hackathon-java/user-service:latest --region=us-central1
```

#### All Services (Full - 10-15 min)

```cmd
powershell -ExecutionPolicy Bypass -File .\build-images.ps1
powershell -ExecutionPolicy Bypass -File .\deploy-services.ps1
```

---

### Rollback to Previous Version

If something goes wrong after deployment:

```cmd
REM View revisions
gcloud run services describe user-service --region=us-central1

REM Rollback to previous revision
gcloud run services update-traffic user-service --to-revisions=user-service-00001-xyz=100 --region=us-central1
```

---

## 📱 Update Postman Collection

1. Open Postman
2. Import `Microservices-API.postman_collection.json`
3. Edit Collection Variables:
   - Set `baseUrl` to your API Gateway URL: `https://api-gateway-xxxxx-uc.a.run.app`
4. Test with Register endpoint!

---

## 🧪 Quick Test

### Register a User

```powershell
$body = @{
    email = "test@example.com"
    password = "Test@123"
    displayName = "Test User"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "https://YOUR-API-GATEWAY-URL/api/auth/register" -Method POST -Body $body -ContentType "application/json"
$response
```

### Login

```powershell
$body = @{
    email = "test@example.com"
    password = "Test@123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "https://YOUR-API-GATEWAY-URL/api/auth/login" -Method POST -Body $body -ContentType "application/json"
$token = $response.token
Write-Host "Token: $token"
```

### Get Profile

```powershell
$headers = @{
    Authorization = "Bearer $token"
}

Invoke-RestMethod -Uri "https://YOUR-API-GATEWAY-URL/api/users/profile" -Method GET -Headers $headers
```

---

## 🔧 Development Workflow

### Making Code Changes

1. **Edit your Java code** in any service
2. **Build the service:**
   ```cmd
   docker build -t gcr.io/hackathon-java/SERVICE_NAME:latest -f SERVICE_NAME/Dockerfile .
   ```
3. **Push to registry:**
   ```cmd
   docker push gcr.io/hackathon-java/SERVICE_NAME:latest
   ```
4. **Deploy updated service:**
   ```cmd
   gcloud run deploy SERVICE_NAME --image=gcr.io/hackathon-java/SERVICE_NAME:latest --region=us-central1
   ```
5. **Test your changes** using Postman or curl

### Making Config Changes

1. **Edit config files** in `config-server/src/main/resources/configs/`
2. **Rebuild config-server:**
   ```cmd
   docker build -t gcr.io/hackathon-java/config-server:latest -f config-server/Dockerfile .
   docker push gcr.io/hackathon-java/config-server:latest
   gcloud run deploy config-server --image=gcr.io/hackathon-java/config-server:latest --region=us-central1
   ```
3. **Restart dependent services** to pick up new config:
   ```cmd
   gcloud run services update SERVICE_NAME --region=us-central1
   ```

---

## 📊 Estimated Costs

### Current Configuration

- **Free Tier**: 2 million requests/month
- **Development/Testing**: $0/month (within free tier)
- **Low Traffic Production**: $5-15/month
- **Medium Traffic**: $30-60/month

### Your Services

| Service        | Memory | Max Instances | Est. Cost |
| -------------- | ------ | ------------- | --------- |
| Config Server  | 512Mi  | 3             | $2-5/mo   |
| Eureka Service | 512Mi  | 2             | $2-5/mo   |
| Auth Service   | 1Gi    | 5             | $5-10/mo  |
| User Service   | 1Gi    | 5             | $5-10/mo  |
| Loan Service   | 512Mi  | 5             | $2-5/mo   |
| API Gateway    | 1Gi    | 10            | $5-10/mo  |

**Total**: ~$21-45/month for production use

---

## 🆘 Troubleshooting

### "Permission Denied" Error

```bash
gcloud auth login
gcloud auth configure-docker
```

### "Project Not Found" Error

```bash
gcloud config set project hackathon-java
gcloud projects list
```

### Check Service Logs

```bash
gcloud run services logs read api-gateway --region=us-central1 --limit=50
```

### View All Services

```bash
gcloud run services list --region=us-central1
```

---

## 📚 Documentation

- **Quick Start**: [QUICK-START.md](QUICK-START.md)
- **Complete Guide**: [DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md)
- **File Overview**: [DEPLOYMENT-FILES.md](DEPLOYMENT-FILES.md)

---

## ⚡ Ready to Deploy!

### Pre-Deployment Checklist

1. ✅ GCP Project: `hackathon-java` configured
2. ✅ Firebase credentials loaded
3. ✅ Build scripts ready
4. ✅ Deployment scripts ready
5. ⚠️ **TODO**: Set strong JWT secret in `.env.cloud`
6. ⚠️ **TODO**: Enable GCP APIs
7. ⚠️ **TODO**: Upload Firebase key to Secret Manager

### Run Initial Deployment

From Command Prompt (cmd):

```cmd
REM Step 1: Build all Docker images (5-10 minutes)
powershell -ExecutionPolicy Bypass -File .\build-images.ps1

REM Step 2: Deploy all services to Cloud Run (3-5 minutes)
powershell -ExecutionPolicy Bypass -File .\deploy-services.ps1
```

### After Making Changes

```cmd
REM Quick: Single service rebuild & redeploy (2-3 min)
docker build -t gcr.io/hackathon-java/SERVICE_NAME:latest -f SERVICE_NAME/Dockerfile .
docker push gcr.io/hackathon-java/SERVICE_NAME:latest
gcloud run deploy SERVICE_NAME --image=gcr.io/hackathon-java/SERVICE_NAME:latest --region=us-central1

REM Full: Rebuild & redeploy all services (10-15 min)
powershell -ExecutionPolicy Bypass -File .\build-images.ps1
powershell -ExecutionPolicy Bypass -File .\deploy-services.ps1
```

---

## 📋 Common Commands Cheatsheet

### Build Commands

```cmd
REM Build all services
powershell -ExecutionPolicy Bypass -File .\build-images.ps1

REM Build single service
docker build -t gcr.io/hackathon-java/SERVICE_NAME:latest -f SERVICE_NAME/Dockerfile .
docker push gcr.io/hackathon-java/SERVICE_NAME:latest
```

### Deployment Commands

```cmd
REM Deploy all services
powershell -ExecutionPolicy Bypass -File .\deploy-services.ps1

REM Deploy single service
gcloud run deploy SERVICE_NAME --image=gcr.io/hackathon-java/SERVICE_NAME:latest --region=us-central1
```

### Monitoring Commands

```cmd
REM View service logs
gcloud run services logs read SERVICE_NAME --region=us-central1 --limit=50

REM List all services
gcloud run services list --region=us-central1

REM Describe service details
gcloud run services describe SERVICE_NAME --region=us-central1
```

### Update Commands

```cmd
REM Update environment variables
gcloud run services update SERVICE_NAME --set-env-vars="KEY=VALUE" --region=us-central1

REM Update memory/CPU
gcloud run services update SERVICE_NAME --memory=1Gi --cpu=2 --region=us-central1

REM Force new revision (restart)
gcloud run services update SERVICE_NAME --region=us-central1
```

Good luck! 🚀
