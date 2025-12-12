# Chesstory GCP Deployment Guide

This guide details the steps to deploy Chesstory (Frontend + Backend + DB) to Google Cloud Platform.

## 1. Prerequisites

1.  **GCP Account & Project**
    *   Create a project (e.g., `chesstory-prod`).
    *   Enable APIs: `Cloud Run`, `Cloud Build`, `Secret Manager`, `Cloud SQL`, `Compute Engine` (if using VM).

2.  **Tools**
    *   Install [Google Cloud CLI](https://cloud.google.com/sdk/docs/install).
    *   Run `gcloud init` to authenticate.

---

## 2. Infrastructure Setup

### A. Database (Cloud SQL)
1.  Go to **Cloud SQL** > **Create Instance** > **PostgreSQL**.
2.  Set ID: `chesstory-db`, Password: `YOUR_DB_PASSWORD`.
3.  **Connections**: Enable "Public IP" (for easy dev access) or "Private IP" (for VPC).
4.  Create Database: `chess`.

### B. Redis (Cloud Memorystore)
1.  Go to **Memorystore** > **Redis** > **Create Instance**.
2.  Tier: Basic (for dev) or Standard.
3.  Note the **IP Address** and **Port**.

### C. Storage (GCS)
1.  Go to **Cloud Storage** > **Create Bucket**.
2.  Name: `chesstory-analysis-data`.
3.  Upload `opening/masters_stats.db` (3.7GB) to this bucket.

---

## 3. Secrets Management (Secure)

Don't put passwords in `docker-compose.yml`. Use **Secret Manager**.

1.  Go to **Security** > **Secret Manager**.
2.  Create Secrets:
    *   `DB_PASSWORD`
    *   `JWT_SECRET`
    *   `GOOGLE_CLIENT_ID`
3.  Grant **Secret Accessor** role to your Cloud Run service account.

---

## 4. Manual Deployment (Cloud Run)

### Backend (API)
```bash
# 1. Build & Push Image
gcloud builds submit --tag gcr.io/chesstory-prod/api:latest -f Dockerfile.backend .

# 2. Deploy
gcloud run deploy chesstory-api \
  --image gcr.io/chesstory-prod/api:latest \
  --allow-unauthenticated \
  --port 8080 \
  --set-env-vars DB_URL="jdbc:postgresql://<DB_IP>:5432/chess",REDIS_URL="redis://<REDIS_IP>:6379" \
  --set-secrets JWT_SECRET=JWT_SECRET:latest
```
*Note: For the 3.7GB Opening DB, it's best to use **Cloud Run Gen 2** with a localized volume or download it from GCS on startup.*

### Frontend (Next.js)
```bash
# 1. Build & Push
gcloud builds submit --tag gcr.io/chesstory-prod/web:latest -f Dockerfile.frontend .

# 2. Deploy
gcloud run deploy chesstory-web \
  --image gcr.io/chesstory-prod/web:latest \
  --allow-unauthenticated \
  --port 3000 \
  --set-env-vars NEXT_PUBLIC_API_URL="https://chesstory-api-xyz.a.run.app"
```

---

## 5. Automatic Deployment (GitHub Actions)

We will automate this using `.github/workflows/deploy.yml` (to be created).

### Step 1: Service Account
1.  Create Service Account: `github-deployer`.
2.  Roles: `Cloud Run Admin`, `Storage Admin`, `Service Account User`, `Artifact Registry Writer`.
3.  **Keys** > **Add Key** > **JSON**. Download it.

### Step 2: GitHub Secrets
Go to Repo > Settings > Secrets and Variables > Actions:
*   `GCP_PROJECT_ID`: `chesstory-prod`
*   `GCP_SA_KEY`: (Paste JSON content)

### Step 3: Workflow File
Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy to Cloud Run

on:
  push:
    branches: [ "main" ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Authenticate to GCP
        uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.GCP_SA_KEY }}

      - name: Setup Cloud SDK
        uses: google-github-actions/setup-gcloud@v1

      - name: Build & Push Backend
        run: |
          gcloud builds submit --tag gcr.io/${{ secrets.GCP_PROJECT_ID }}/api:latest -f Dockerfile.backend .

      - name: Deploy Backend
        run: |
          gcloud run deploy chesstory-api \
            --image gcr.io/${{ secrets.GCP_PROJECT_ID }}/api:latest \
            --region us-central1 \
            --allow-unauthenticated

      # Repeat for Frontend...
```

---

## 6. Accessing the Application

Once deployed, Cloud Run will provide URLs:
*   Frontend: `https://chesstory-web-xyz.a.run.app`
*   Backend: `https://chesstory-api-xyz.a.run.app`

Update your **DNS** (chesstory.com) to point to the Frontend URL.
