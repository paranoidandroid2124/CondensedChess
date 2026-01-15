# Chesstory

AI-powered chess storytelling platform that transforms your games into engaging narratives.

> 🎯 **A [Lichess](https://lichess.org) fork** focused on narrative-driven chess analysis with AI commentary.

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
# Required for AI narratives
GEMINI_API_KEY=your-api-key-here
GEMINI_MODEL=gemini-3.0-flash-preview
```

Get your API key from [Google AI Studio](https://aistudio.google.com/apikey).

### Access

- **Main Site**: http://localhost:8080/
- **Analysis Board**: http://localhost:8080/analysis

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
└── LlmClient.scala    # Gemini API integration

ui/analyse/            # Analysis Board UI
├── src/narrative/     # Narrative display components
└── src/bookmaker.ts   # Story generation orchestration
```

## License

Chesstory is licensed under the **GNU Affero General Public License v3.0** (AGPL-3.0).

This project is a fork of [Lichess](https://github.com/lichess-org/lila). See [COPYING.md](COPYING.md) for full license details and attribution.

## Credits

- [Lichess](https://lichess.org) - The original open-source chess platform
- [Stockfish](https://stockfishchess.org/) - Chess engine
- [Google Gemini](https://ai.google.dev/) - AI text generation
