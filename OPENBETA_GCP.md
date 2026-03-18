# Chesstory Open Beta on GCP

This repository now ships a repo-contained path for open-beta deployment:

- container build: [lila-docker/repos/lila/Dockerfile.openbeta](lila-docker/repos/lila/Dockerfile.openbeta)
- production config: [lila-docker/repos/lila/conf/application.openbeta.conf](lila-docker/repos/lila/conf/application.openbeta.conf)
- GitHub Actions workflow: [.github/workflows/gcp-openbeta.yml](.github/workflows/gcp-openbeta.yml)

## Deployment model

Use GitHub Actions to:

1. build the app image
2. push it to Artifact Registry
3. deploy the new image to an existing Cloud Run service

The workflow intentionally keeps runtime secrets and service settings on Cloud Run / Secret Manager instead of duplicating them in GitHub Actions.

## GitHub Actions prerequisites

Set these repository variables:

- `GCP_PROJECT_ID`
- `GCP_REGION`
- `GCP_ARTIFACT_REPOSITORY`
- `CLOUD_RUN_SERVICE`
- optional: `CLOUD_RUN_IMAGE_NAME`
- optional: `CLOUD_RUN_FLAGS`

Set these repository secrets:

- `GCP_WORKLOAD_IDENTITY_PROVIDER`
- `GCP_SERVICE_ACCOUNT`

## First-time GCP bootstrap

Create an Artifact Registry repository once:

```bash
gcloud artifacts repositories create chesstory-openbeta \
  --repository-format=docker \
  --location=asia-northeast3
```

Then configure the GitHub Actions variables/secrets and run the `gcp-openbeta` workflow once with `workflow_dispatch`. The same workflow can create the first Cloud Run service revision and then keep updating it on later pushes.

After the first image deploy, configure the service env vars and secret bindings with `gcloud run services update`.

## Required runtime environment

Required plain env vars:

- `LILA_DOMAIN`
- `LILA_BASE_URL`
- `PUBLIC_CONTACT_EMAIL`
- `MONGODB_URI`
- `REDIS_URI`
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_TLS`
- `SMTP_USER`
- `SMTP_SENDER`
- `HCAPTCHA_SITEKEY`
- `ENABLE_RATE_LIMITING=true`
- `CHESSTORY_BETA_PREMIUM_ALL=true` if you want beta users to receive premium-grade LLM behavior

Required secret-backed env vars:

- `PLAY_HTTP_SECRET_KEY`
- `USER_PASSWORD_BPASS_SECRET`
- `PASSWORD_RESET_SECRET`
- `EMAIL_CONFIRM_SECRET`
- `EMAIL_CHANGE_SECRET`
- `LOGIN_TOKEN_SECRET`
- `SMTP_PASSWORD`
- `HCAPTCHA_SECRET`

Recommended optional env vars:

- `OPENAI_API_KEY`
- `OPENAI_MODEL_SYNC`
- `OPENAI_MODEL_FALLBACK`
- `OPENAI_MODEL_ASYNC`
- `OPENAI_MODEL_PRO_SYNC`
- `OPENAI_MODEL_PRO_FALLBACK`
- `OPENAI_MODEL_PRO_ASYNC`
- `OPENAI_PROMPT_CACHE_KEY_PREFIX`
- `OPENAI_MAX_OUTPUT_TOKENS`
- `GEMINI_API_KEY`
- `GEMINI_MODEL`
- `SUPPORT_PATREON_URL`
- `SUPPORT_GITHUB_SPONSORS_URL`
- `SUPPORT_BMC_URL`
- `PROMETHEUS_KEY`
- `ENABLE_MONITORING`
- `EXPLORER_API_BASE`
- `TABLEBASE_API_BASE`
- `LICHESS_IMPORT_API_BASE`
- `LICHESS_WEB_BASE`
- `CHESSCOM_API_BASE`
- `EXTERNAL_ENGINE_ENDPOINT`
- `ACCOUNT_INTEL_DISPATCH_BASE_URL`
- `ACCOUNT_INTEL_DISPATCH_BEARER_TOKEN`
- `ACCOUNT_INTEL_WORKER_TOKEN`
- `ACCOUNT_INTEL_SELECTIVE_EVAL_ENDPOINT`

## Example service update

Non-secret env vars:

```bash
gcloud run services update chesstory-openbeta \
  --region=asia-northeast3 \
  --update-env-vars=LILA_DOMAIN=beta.chesstory.com,LILA_BASE_URL=https://beta.chesstory.com,PUBLIC_CONTACT_EMAIL=support@chesstory.com,MONGODB_URI='mongodb+srv://...',REDIS_URI='redis://...',ENABLE_RATE_LIMITING=true,SMTP_HOST=smtp.postmarkapp.com,SMTP_PORT=587,SMTP_TLS=true,SMTP_USER=postmark-server-token,SMTP_SENDER='Chesstory <noreply@beta.chesstory.com>',HCAPTCHA_SITEKEY=YOUR_SITE_KEY
```

Secret bindings:

```bash
gcloud run services update chesstory-openbeta \
  --region=asia-northeast3 \
  --set-secrets=PLAY_HTTP_SECRET_KEY=play-http-secret:latest,USER_PASSWORD_BPASS_SECRET=user-password-bpass-secret:latest,PASSWORD_RESET_SECRET=password-reset-secret:latest,EMAIL_CONFIRM_SECRET=email-confirm-secret:latest,EMAIL_CHANGE_SECRET=email-change-secret:latest,LOGIN_TOKEN_SECRET=login-token-secret:latest,SMTP_PASSWORD=smtp-password:latest,HCAPTCHA_SECRET=hcaptcha-secret:latest
```

## Open-beta product feedback path

This repo now ships structured beta feedback capture and paid-plan waitlist hooks:

- global form: [lila-docker/repos/lila/app/views/pages/betaFeedback.scala](lila-docker/repos/lila/app/views/pages/betaFeedback.scala)
- backend storage/API: [lila-docker/repos/lila/modules/beta/src/main/BetaFeedbackApi.scala](lila-docker/repos/lila/modules/beta/src/main/BetaFeedbackApi.scala)
- routes: [lila-docker/repos/lila/conf/routes](lila-docker/repos/lila/conf/routes)

Current entry points:

- landing and support pages
- post-completion Strategic Puzzle prompt
- post-analysis Game Chronicle prompt

The stored signals are:

- product surface
- triggering feature label
- willingness to pay
- optional price band
- optional notes
- optional waitlist enrollment email

## Signup protections enforced in production

Production boot now fails if any of these are missing or weak:

- `play.http.secret.key`
- `user.password.bpass.secret`
- email confirmation enabled
- hCaptcha enabled with real keys
- password-reset / email-confirm / email-change / login-token secrets
- `auth.magicLink.autoCreate = false`
- real SMTP configuration

See [lila-docker/repos/lila/app/ProductionConfigValidator.scala](lila-docker/repos/lila/app/ProductionConfigValidator.scala).
