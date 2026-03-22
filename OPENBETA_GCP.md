# Chesstory Open Beta on GCP

This repository ships a repo-contained path for open-beta deployment:

- container build: [lila-docker/repos/lila/Dockerfile.openbeta](lila-docker/repos/lila/Dockerfile.openbeta)
- production config: [lila-docker/repos/lila/conf/application.openbeta.conf](lila-docker/repos/lila/conf/application.openbeta.conf)
- binding manifest: [lila-docker/repos/lila/conf/openbeta-bindings.json](lila-docker/repos/lila/conf/openbeta-bindings.json)
- GitHub Actions workflow: [.github/workflows/gcp-openbeta.yml](.github/workflows/gcp-openbeta.yml)

## Deployment model

Use GitHub Actions to:

1. build the app image
2. push it to Artifact Registry
3. deploy the new image to an already-bootstrapped Cloud Run service

The workflow now validates Cloud Run runtime bindings before the build starts. A deployment is expected to fail if the target service is missing or if its env/secret bindings are incomplete.

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

Create the Cloud Run service once before using the standard workflow. A simple bootstrap path is to deploy a temporary placeholder image:

```bash
gcloud run deploy chesstory-openbeta \
  --region=asia-northeast3 \
  --image=us-docker.pkg.dev/cloudrun/container/hello \
  --allow-unauthenticated \
  --port=8080
```

Then configure the runtime env vars and secret bindings with `gcloud run services update`. After the service exists and the bindings are complete, the `gcp-openbeta` workflow becomes the standard rollout path for later revisions.

## Required always

These bindings are mandatory for open-beta runtime and are validated before deploy.

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
- `SMTP_PASSWORD`
- `PLAY_HTTP_SECRET_KEY`
- `USER_PASSWORD_BPASS_SECRET`
- `PASSWORD_RESET_SECRET`
- `EMAIL_CONFIRM_SECRET`
- `EMAIL_CHANGE_SECRET`
- `LOGIN_TOKEN_SECRET`
- `HCAPTCHA_SITEKEY`
- `HCAPTCHA_SECRET`
- `PROMETHEUS_KEY`
- `EXTERNAL_ENGINE_ENDPOINT`

Notes:

- `PROMETHEUS_KEY` is production-required, not optional.

## Hardcoded open-beta defaults

These runtime values are intentionally fixed in [application.openbeta.conf](lila-docker/repos/lila/conf/application.openbeta.conf) instead of being pushed out to Cloud Run env vars.

- `net.ratelimit = true`
- `explorer.endpoint = https://explorer.lichess.org`
- `explorer.internal_endpoint = https://explorer.lichess.org`
- `explorer.tablebase_endpoint = https://tablebase.lichess.ovh`
- `external.import.lichess.api_base = https://lichess.org`
- `external.import.lichess.web_base = https://lichess.org`
- `external.import.chesscom.api_base = https://api.chess.com`

These are still tracked by readiness and the open-beta binding manifest, but they are no longer Cloud Run env requirements.

## Required only in selected modes

These bindings are only required when the related mode is enabled.

- `ACCOUNT_INTEL_DISPATCH_BASE_URL`
- `ACCOUNT_INTEL_DISPATCH_BEARER_TOKEN`
- `ACCOUNT_INTEL_WORKER_TOKEN`
- `ACCOUNT_INTEL_SELECTIVE_EVAL_ENDPOINT`

Rules:

- if `ACCOUNT_INTEL_DISPATCH_BASE_URL` is set, the dispatch auth token set must be complete before deploy
- if `ACCOUNT_INTEL_SELECTIVE_EVAL_ENDPOINT` is set, it becomes part of readiness and must point to a real remote service
- if dispatch is not configured, local worker mode remains valid

## Soft optional integrations

These bindings are optional and do not block boot by themselves, but they are tracked in the binding manifest and shown in ops diagnostics.

- `GIF_EXPORT_URL`
- `LICHESS_EXPLORER_TOKEN`
- `SUPPORT_PATREON_URL`
- `SUPPORT_GITHUB_SPONSORS_URL`
- `SUPPORT_BMC_URL`
- `ENABLE_MONITORING`
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
- `CHESSTORY_BETA_PREMIUM_ALL`

Notes:

- `LICHESS_EXPLORER_TOKEN` is optional. If omitted, the explorer proxy can still work against public upstream access when allowed.
- `GIF_EXPORT_URL` is optional. If omitted, GIF export remains intentionally disabled instead of inheriting an upstream default.

## Removed / dormant bindings

These are intentionally dormant in open-beta and should not be configured on Cloud Run.

- `PUSH_WEB_URL`
- `PUSH_WEB_VAPID_PUBLIC_KEY`

The frontend does not currently register the service worker in open-beta boot, so push bindings are dead configuration and are expected to stay empty.

## Example service update

Required always, plain env vars:

```bash
gcloud run services update chesstory-openbeta \
  --region=asia-northeast3 \
  --update-env-vars=LILA_DOMAIN=beta.chesstory.com,LILA_BASE_URL=https://beta.chesstory.com,PUBLIC_CONTACT_EMAIL=support@chesstory.com,MONGODB_URI='mongodb+srv://...',REDIS_URI='redis://...',SMTP_HOST=smtp.postmarkapp.com,SMTP_PORT=587,SMTP_TLS=true,SMTP_USER=postmark-server-token,SMTP_SENDER='Chesstory <noreply@beta.chesstory.com>',HCAPTCHA_SITEKEY=YOUR_SITE_KEY,EXTERNAL_ENGINE_ENDPOINT=https://engine.chesstory.com
```

Required always, secret bindings:

```bash
gcloud run services update chesstory-openbeta \
  --region=asia-northeast3 \
  --set-secrets=PLAY_HTTP_SECRET_KEY=play-http-secret:latest,USER_PASSWORD_BPASS_SECRET=user-password-bpass-secret:latest,PASSWORD_RESET_SECRET=password-reset-secret:latest,EMAIL_CONFIRM_SECRET=email-confirm-secret:latest,EMAIL_CHANGE_SECRET=email-change-secret:latest,LOGIN_TOKEN_SECRET=login-token-secret:latest,SMTP_PASSWORD=smtp-password:latest,HCAPTCHA_SECRET=hcaptcha-secret:latest,PROMETHEUS_KEY=prometheus-key:latest
```

Conditional Account Intel dispatch bindings:

```bash
gcloud run services update chesstory-openbeta \
  --region=asia-northeast3 \
  --update-env-vars=ACCOUNT_INTEL_DISPATCH_BASE_URL=https://worker.chesstory.com \
  --set-secrets=ACCOUNT_INTEL_DISPATCH_BEARER_TOKEN=account-intel-dispatch-token:latest,ACCOUNT_INTEL_WORKER_TOKEN=account-intel-worker-token:latest
```

## Open-beta product feedback path

This repo ships structured beta feedback capture and paid-plan waitlist hooks:

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

Production boot fails if any of these are missing or weak:

- `play.http.secret.key`
- `user.password.bpass.secret`
- email confirmation enabled
- hCaptcha enabled with real keys
- password-reset / email-confirm / email-change / login-token secrets
- `auth.magicLink.autoCreate = false`
- real SMTP configuration
- empty push web bindings

See [lila-docker/repos/lila/app/ProductionConfigValidator.scala](lila-docker/repos/lila/app/ProductionConfigValidator.scala).
