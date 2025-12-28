# LLM Commentary Module - Architecture Documentation

## Overview

This module provides AI-powered chess commentary by analyzing positions and moves
using a combination of motif detection, plan matching, and LLM-based text generation.

---

## Architecture Decisions (2024-12-25)

### 1. No Fishnet Usage

**Decision**: We do NOT use Lichess fishnet for analysis.

**Rationale**:
- Fishnet uses volunteer CPU donations intended for Lichess.org only
- Using it for our service would be ethically inappropriate
- ToS considerations for third-party use

**Alternative**: Client-side Stockfish WASM + Lichess Cloud Eval API

---

### 2. Eval Data Sources

| Source | Use Case | Limit |
|--------|----------|-------|
| **Lichess Cloud Eval API** | First ~15 plies (cached positions) | 3000 req/day per user IP |
| **Client WASM Worker** | Uncached positions, sidelines | None (user's CPU) |

**Flow**:
```
User's Browser
├── Lichess Cloud Eval API (direct call, CC0 license)
│   └── https://lichess.org/api/cloud-eval?fen=...&multiPv=3
└── WASM Worker (fallback for cache miss)
    ↓
POST /api/llm/comment { fen, played, eval: { cp, pvs } }
    ↓
CounterfactualAnalyzer + LLM → Commentary Text
```

---

### 3. License Compliance

| Component | License | Our Obligation |
|-----------|---------|----------------|
| lila (Lichess backend) | AGPL-3.0 | Full source code public |
| chessground (UI) | GPL-3.0 | Frontend source public |
| Stockfish WASM | GPL-3.0 | Distribute with source |
| Lichess Cloud Eval | CC0 | None (public domain) |

**Conclusion**: Project will be fully open-source under AGPL.

---

## Module Structure

```
modules/llm/src/main/
├── model/
│   ├── Motif.scala       # 34 motif case classes
│   └── Plan.scala        # 24 plan types
├── analysis/
│   ├── MotifTokenizer.scala     # PV → Motifs
│   ├── PlanMatcher.scala        # Motifs → Plans
│   ├── PositionCharacterizer.scala
│   ├── CounterfactualAnalyzer.scala  # (planned)
│   └── CommentaryEngine.scala   # Orchestration
├── LlmClient.scala       # Gemini API client
├── LlmApi.scala          # Service layer
└── LlmConfig.scala       # Configuration
```

---

## Implementation Status

| Component | Status |
|-----------|--------|
| Motif detection (34 types) | ✅ Complete |
| Plan matching (24 types) | ✅ Complete |
| PositionCharacterizer | ✅ Complete |
| CommentaryEngine | ✅ Complete |
| API endpoint `/api/llm/comment` | ✅ Complete |
| CounterfactualAnalyzer | ⏳ Pending |
| Frontend integration | ⏳ Pending |

---

## API Schema

### Request
```json
{
  "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
  "lastMove": "e2e4",
  "played": "e7e5",
  "eval": {
    "cp": 35,
    "depth": 20,
    "pvs": [
      { "moves": ["e7e5", "g1f3"], "cp": 30 },
      { "moves": ["c7c5", "g1f3"], "cp": 35 }
    ]
  },
  "context": {
    "opening": "Italian Game",
    "phase": "opening",
    "ply": 2
  }
}
```

### Response
```json
{
  "commentary": "The most natural response, occupying the center..."
}
```
