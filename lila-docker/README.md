# Chesstory Docker Environment

Docker-based development environment for Chesstory, an AI-powered chess storytelling platform.

> This is a modified version of [lila-docker](https://github.com/lichess-org/lila-docker) for Chesstory development.

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
# Required for AI narratives
GEMINI_API_KEY=your-api-key-here
GEMINI_MODEL=gemini-2.0-flash
```

## URLs

| Service | URL |
|---------|-----|
| Main Site | http://localhost:8080/ |
| Analysis Board | http://localhost:8080/analysis |
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

- Chesstory: AGPL-3.0 (fork of Lichess)
- lila-docker: AGPL-3.0

See [LICENSE](LICENSE) for details.
