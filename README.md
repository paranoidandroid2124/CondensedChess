# Chesstory: Advanced Game Review Engine

**Chesstory** is a high-performance chess analysis pipeline designed to bridge the gap between engine evaluation and human understanding. It transforms raw PGN files into rich, narrative-driven "Studies" by identifying key moments, structural changes, and distinct game phases.

Powered by **Scala 3**, **Stockfish 16+**, and optional **LLM integration** (Gemini/OpenAI).

## 📊 Key Features

- **Sequential Analysis Pipeline**: Replay -> Engine Eval -> Hypothesis Testing -> Narrative Generation.
- **Hypothesis Testing**: Instead of just specific moves, the engine tests for concepts like "Fortress", "Attack", or "Squeeze".
- **Golden Test Suite**: Regression testing ensures analysis logic remains stable across refactors.
- **Narrative Structure**: Games are broken into Chapters (Opening, Middlegame, Endgame) with generated prose.

## 🚀 Getting Started

### Prerequisites

- **JDK 21+** (Eclipse Adoptium suggested)
- **SBT** (Scala Build Tool)
- **Stockfish 16+** (Must be on `PATH` or set via `STOCKFISH_BIN`)
- **Node.js 18+** (For Frontend)

### 1. Backend Setup

```bash
# Clone and enter directory
git clone https://github.com/your-repo/chesstory.git
cd chesstory

# Run Tests (Highly Recommended)
sbt test
# Note: integration tests require Stockfish to be executable.
```

### 2. Running the Server

```bash
# Start the API Server on localhost:8080
sbt "scalachess/runMain chess.analysis.ApiServer"
```

The server exposes:
- `POST /api/game-review/chapter`: Submit PGN for analysis.
- `GET /status`: Monitor job queue and engine pool status.
- `GET /api/game-review/chapter/:jobId`: Retrieve results.

See **[openapi.yaml](./openapi.yaml)** for full API documentation.

### 3. Frontend Setup

```bash
cd frontend
npm install
npm run dev
# Open http://localhost:3000
```
Ensure `.env.local` points to your backend: `NEXT_PUBLIC_API_URL=http://localhost:8080`.

## 🛠 Architecture

The system uses a **Actor-like concurrency model** backed by `java.util.concurrent`.

- **EnginePool**: Manages a fixed set of Stockfish instances to prevent CPU starvation.
- **ExperimentRunner**: Caches identical positions to avoid re-computing known evaluations.
- **AnalysisPipeline**:
    1.  **Assessment**: Deep search + Static Features.
    2.  **Investigation**: Finding "Critical Moments" and "Turning Points".
    3.  **Editorial**: Grouping moves into Chapters.
    4.  **Publication**: generating JSON + LLM Text.

Detailed design is available in **[COACH_ARCHITECTURE.md](./COACH_ARCHITECTURE.md)**.

## 🧪 Testing

We use a **Golden Master** testing strategy.
- `core/src/test/resources/golden/*.pgn`: Source games.
- `core/src/test/resources/golden/*.json`: Expected analysis output.

To update golden files (approve changes):
```powershell
$env:UPDATE_GOLDEN="true"; sbt test
```
See **[TESTING.md](./TESTING.md)** for details.

## 🧱 Extending

Want to add a new "Concept Tag" (e.g., *Greek Gift*)?
See **[docs/EXTENSION_GUIDE.md](./docs/EXTENSION_GUIDE.md)**.

## 📄 License
MIT
