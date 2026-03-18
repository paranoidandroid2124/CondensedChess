# Chesstory Docker Environment

Docker-based development environment for Chesstory, an AI-powered chess storytelling platform.

## Quick Start

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)

### Setup

```bash
./lila-docker start
```

### Environment Variables

Edit `settings.env` to configure:

```env
# LLM provider (default: openai; without key it falls back to rule-based)
LLM_PROVIDER=openai # openai | gemini | none

# OpenAI (recommended quality-cost defaults)
OPENAI_API_KEY=
OPENAI_MODEL_SYNC=gpt-5-mini
OPENAI_MODEL_FALLBACK=gpt-4.1-mini
OPENAI_MODEL_ASYNC=gpt-5-mini
# Optional pro(active) tier routing
OPENAI_MODEL_PRO_SYNC=gpt-5.2
OPENAI_MODEL_PRO_FALLBACK=gpt-5-mini
OPENAI_MODEL_PRO_ASYNC=gpt-5.2
OPENAI_PROMPT_CACHE_KEY_PREFIX=bookmaker:polish:v1
OPENAI_MAX_OUTPUT_TOKENS=640

# Optional Gemini fallback
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.0-flash

# Optional support links shown on /support
SUPPORT_PATREON_URL=
SUPPORT_GITHUB_SPONSORS_URL=
SUPPORT_BMC_URL=
```

## URLs

| Service | URL |
|---------|-----|
| Main Site | http://localhost:8080/ |
| Analysis Board | http://localhost:8080/analysis |
| Support Page | http://localhost:8080/support |
| MongoDB Admin | http://localhost:8081/ |

## Development

### UI Development (Watch Mode)
```bash
./lila-docker ui --watch
```

### Restart Backend
```bash
./lila-docker lila restart
```

### Deployment Automation
Open-beta deployment is now repo-contained via GCP / Cloud Run.

- Workflow: [../.github/workflows/gcp-openbeta.yml](../.github/workflows/gcp-openbeta.yml)
- Bootstrap guide: [../OPENBETA_GCP.md](../OPENBETA_GCP.md)

Use this README for local Docker development. Use the GCP workflow and guide for open-beta deployment.

### Prod-Like Runtime (No `sbt run`)
To run `lila` with Play stage binaries (Prod mode) instead of dev hot-reload:

```bash
# PowerShell
$env:LILA_RUNTIME="prod"
$env:LILA_STAGE_ON_BOOT="auto" # auto | always | skip
docker compose --profile base up -d --force-recreate lila
```

Default stage binary path is `/tmp/lila-target/lila/universal/stage/bin/lila` (override with `LILA_STAGE_BIN`).

### Full UI Build
```bash
docker compose run --rm -w /lila ui node ui/build.mjs
```

### Updating Routes
```bash
docker compose exec lila ./lila.sh playRoutes
```

## Project Structure

```
lila-docker/
├── repos/
│   ├── lila/           # Main Chesstory server (Scala + TypeScript)
│   ├── lila-ws/        # WebSocket server
│   ├── lila-db-seed/   # Database seed data
│   └── scalachess/     # Chess logic library
├── compose.yml         # Docker Compose configuration
├── settings.env        # Environment variables
└── command/            # CLI tool source (Rust)
```

## License

- Chesstory: AGPL-3.0
- Third-party and upstream attributions: see `repos/lila/COPYING.md`

See [LICENSE](LICENSE) for details.
