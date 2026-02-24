# Chesstory

AI-powered chess storytelling platform that transforms your games into engaging narratives.

## What is Chesstory?

Chesstory analyzes your chess games and generates **book-style narratives** that explain:
- Why certain moves were played
- Strategic plans and tactical motifs
- Key turning points and critical moments
- Alternative possibilities you might have missed

Unlike traditional computer analysis that shows cold numbers, Chesstory tells the **story** of your game.

## Features

| Feature | Status |
|---------|--------|
| Standard Chess Analysis | ✅ |
| Freestyle Chess (Chess960) | ✅ |
| AI Narrative Generation | ✅ |
| Motif Detection (34 types) | ✅ |
| Plan Matching (24 types) | ✅ |
| Stockfish 17.1 WASM | ✅ |

## Quick Start

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Git

### Installation

```bash
git clone https://github.com/paranoidandroid2124/CondensedChess.git
cd CondensedChess/lila-docker
./lila-docker start
```

### Environment Variables

Create or edit `settings.env`:

```env
# LLM provider (default: openai; without key it falls back to rule-based)
LLM_PROVIDER=openai # openai | gemini | none

# OpenAI (recommended quality-cost defaults)
OPENAI_API_KEY=
OPENAI_MODEL_SYNC=gpt-4.1-mini
OPENAI_MODEL_FALLBACK=gpt-4o-mini
OPENAI_MODEL_ASYNC=gpt-4.1-mini
OPENAI_PROMPT_CACHE_KEY_PREFIX=bookmaker:polish:v1
OPENAI_MAX_OUTPUT_TOKENS=256

# Optional Gemini fallback
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.0-flash

# Optional support links shown on /support
SUPPORT_PATREON_URL=
SUPPORT_GITHUB_SPONSORS_URL=
SUPPORT_BMC_URL=
```

### Access

- **Main Site**: http://localhost:8080/
- **Analysis Board**: http://localhost:8080/analysis
- **Support**: http://localhost:8080/support

## Development

### UI Development (Watch Mode)
```bash
./lila-docker ui --watch
```

### Restart Backend (After Scala Changes)
```bash
./lila-docker lila restart
```

### Full Build
```bash
docker compose run --rm -w /lila ui node ui/build.mjs
```

## Architecture

See [modules/llm/ARCHITECTURE.md](modules/llm/ARCHITECTURE.md) for detailed technical documentation.

### Key Components

```
modules/llm/           # AI Commentary Engine
├── analysis/          # Motif detection, plan matching
├── model/             # Motif & Plan definitions
└── LlmClient.scala    # Optional Gemini integration (currently disabled in API flow)

ui/analyse/            # Analysis Board UI
├── src/narrative/     # Narrative display components
└── src/bookmaker.ts   # Story generation orchestration
```

## License

Chesstory is licensed under the **GNU Affero General Public License v3.0** (AGPL-3.0).
See [COPYING.md](COPYING.md) for full license details and upstream attribution.
