# Chesstory GCP 배포 가이드

이 가이드는 Chesstory (Frontend + Backend + DB)를 Google Cloud Platform (GCP)에 배포하는 단계를 상세히 설명합니다.

## 1. 필수 준비 사항 (Prerequisites)

1.  **GCP 계정 및 프로젝트**
    *   새 프로젝트 생성 (예: `chesstory-prod`).
    *   다음 API 활성화: `Cloud Run`, `Cloud Build`, `Secret Manager`, `Cloud SQL`, `Compute Engine` (VM 사용 시).

2.  **도구 설치**
    *   [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) 설치.
    *   `gcloud init` 명령어로 인증 및 프로젝트 설정.

---

## 2. 인프라 설정 (Infrastructure Setup)

### A. 데이터베이스 (Cloud SQL)
1.  **Cloud SQL** > **인스턴스 만들기** > **PostgreSQL** 선택.
2.  ID: `chesstory-db`, 비밀번호: `YOUR_DB_PASSWORD` 설정 (안전한 비밀번호 사용).
3.  **연결 구성**: "공용 IP" (개발 편의용) 또는 "비공개 IP" (VPC 보안용) 활성화.
4.  데이터베이스 생성: 이름 `chess`.

### B. Redis (Cloud Memorystore)
1.  **Memorystore** > **Redis** > **인스턴스 만들기**.
2.  계층: 기본(Basic, 개발용) 또는 표준(Standard, 운영용).
3.  생성 후 **IP 주소**와 **포트** 번호를 기록해둡니다.

### C. 스토리지 (GCS)
1.  **Cloud Storage** > **버킷 만들기**.
2.  이름: `chesstory-analysis-data` (전역 고유 이름 필요).
3.  `opening/masters_stats.db` 파일 (약 3.7GB)을 이 버킷에 업로드합니다.

---

## 3. 비밀 관리 (Secrets Management - 보안 필수)

`docker-compose.yml`이나 코드에 비밀번호를 직접 적지 마세요. **Secret Manager**를 사용합니다.

1.  **보안** > **Secret Manager** 로 이동.
2.  새 보안 비밀 생성:
    *   `DB_PASSWORD`
    *   `JWT_SECRET`
    *   `GOOGLE_CLIENT_ID`
3.  Cloud Run 서비스 계정에 **Secret Accessor (보안 비밀 접근자)** 역할을 부여해야 합니다.

---

## 4. 수동 배포 (Cloud Run)

### 백엔드 (API)
```bash
# 1. 이미지 빌드 및 푸시
gcloud builds submit --tag gcr.io/chesstory-prod/api:latest -f Dockerfile.backend .

# 2. 배포 (Deploy)
gcloud run deploy chesstory-api \
  --image gcr.io/chesstory-prod/api:latest \
  --allow-unauthenticated \
  --port 8080 \
  --set-env-vars DB_URL="jdbc:postgresql://<DB_IP>:5432/chess",REDIS_URL="redis://<REDIS_IP>:6379" \
  --set-secrets JWT_SECRET=JWT_SECRET:latest
```
*참고: 3.7GB 오프닝 DB의 경우, **Cloud Run Gen 2**를 사용하여 시작 시 GCS에서 로컬 볼륨으로 다운로드하거나, 네트워크 마운트 볼륨 기능을 고려해야 합니다.*

### 프론트엔드 (Next.js)
```bash
# 1. 빌드 및 푸시
gcloud builds submit --tag gcr.io/chesstory-prod/web:latest -f Dockerfile.frontend .

# 2. 배포 (Deploy)
gcloud run deploy chesstory-web \
  --image gcr.io/chesstory-prod/web:latest \
  --allow-unauthenticated \
  --port 3000 \
  --set-env-vars NEXT_PUBLIC_API_URL="https://chesstory-api-xyz.a.run.app"
```

---

## 5. 자동 배포 (GitHub Actions)

`.github/workflows/deploy.yml` 파일을 통해 배포를 자동화합니다.

### 1단계: 서비스 계정 생성
1.  GCP IAM에서 서비스 계정 생성: `github-deployer`.
2.  역할 부여: `Cloud Run 관리자`, `스토리지 관리자`, `서비스 계정 사용자`, `Artifact Registry 작성자`.
3.  **키** > **키 추가** > **JSON** 생성 및 다운로드.

### 2단계: GitHub Secrets 설정
GitHub 저장소 > Settings > Secrets and Variables > Actions:
*   `GCP_PROJECT_ID`: `chesstory-prod`
*   `GCP_SA_KEY`: (다운로드한 JSON 파일 내용 전체)

### 3단계: 워크플로우 파일 생성
`.github/workflows/deploy.yml` 파일을 생성하여 `main` 브랜치 푸시 시 자동 배포되도록 설정합니다. (상세 내용은 영문 가이드의 5번 항목 코드 참조)

---

## 6. 접속 확인

배포가 완료되면 Cloud Run에서 URL을 제공합니다:
*   Frontend: `https://chesstory-web-xyz.a.run.app`
*   Backend: `https://chesstory-api-xyz.a.run.app`

도메인(예: chesstory.com)의 **DNS 설정**에서 프론트엔드 URL을 가리키도록 CNAME 등을 설정하면 완료됩니다.
