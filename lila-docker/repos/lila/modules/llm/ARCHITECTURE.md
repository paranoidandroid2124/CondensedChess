# Chesstory LLM Module - Architecture

## Overview

The LLM module provides AI-powered chess commentary by analyzing positions and moves using motif detection, plan matching, and rule-based text generation.

## Architecture Decisions

### 1. No Volunteer Distributed Analysis Network Usage

**Decision**: We do NOT use third-party volunteer compute networks for analysis.

**Rationale**:
- Volunteer CPU pools are not part of this product's compute model
- Operational boundaries should stay explicit and self-managed
- Third-party usage constraints are avoided by design

**Alternative**: Client-side Stockfish WASM + Public Cloud Eval API

### 2. Eval Data Sources

| Source | Use Case | Limit |
|--------|----------|-------|
| **Public Cloud Eval API** | Cached positions (first ~15 plies) | 3000 req/day per IP |
| **Stockfish 17.1 WASM** | Uncached positions, sidelines | None (user's CPU) |

### 3. Supported Variants

| Variant | Status |
|---------|--------|
| Standard Chess | ✅ Supported |
| Freestyle Chess (Chess960) | ✅ Supported |
| Other variants | ❌ Removed |

> **Note**: Non-standard variants (Crazyhouse, Atomic, etc.) have been removed to focus on core functionality.

### 4. License Compliance

| Component | License | Our Obligation |
|-----------|---------|----------------|
| core server backend | AGPL-3.0 | Full source code public |
| chessground (UI) | GPL-3.0 | Frontend source public |
| Stockfish WASM | GPL-3.0 | Distribute with source |
| Public Cloud Eval | CC0 | None (public domain) |

**Conclusion**: Project is fully open-source under AGPL-3.0.

---

## Module Structure

```
modules/llm/
├── src/main/
│   ├── model/
│   │   ├── Motif.scala              # 34 motif case classes
│   │   └── Plan.scala               # 24 plan types
│   ├── analysis/
│   │   ├── BookStyleRenderer.scala  # Narrative text generation
│   │   ├── ConceptLabeler.scala     # Semantic labeling
│   │   ├── MoveAnalyzer.scala       # State/trajectory motifs + variation analysis
│   │   ├── NarrativeLexicon.scala   # Template library
│   │   ├── PlanMatcher.scala        # Motifs → Plans
│   │   └── PositionCharacterizer.scala
│   ├── Env.scala                    # Module initialization
│   ├── LlmApi.scala                 # Service layer
│   ├── LlmClient.scala              # Optional Gemini API client (disabled in runtime flow)
│   ├── LlmConfig.scala              # Environment config
│   └── PgnAnalysisHelper.scala      # PGN parsing utilities
└── docs/
    └── CpContract.md                # CP score specification
```

---

## Implementation Status

| Component | Status |
|-----------|--------|
| Motif detection (34 types) | ✅ Complete |
| Plan matching (24 types) | ✅ Complete |
| PositionCharacterizer | ✅ Complete |
| BookStyleRenderer | ✅ Complete |
| NarrativeLexicon | ✅ Complete |
| ConceptLabeler | ✅ Complete |
| LlmClient (Gemini) | ✅ Available (runtime disabled) |
| Frontend narrative UI | ✅ Complete |
| Variant restriction | ✅ Complete |

---

## Configuration

### Environment Variables

```env
GEMINI_API_KEY=your-api-key     # Optional
GEMINI_MODEL=gemini-2.0-flash   # Optional, default: gemini-3-flash-preview
```

---

## Data Flow

```
Browser
├── Stockfish 17.1 WASM (local analysis)
│   └── Position eval + PV lines
└── POST /api/llm/comment
    ├── MoveAnalyzer (state/trajectory provider): PV/state → Motifs
    ├── PlanMatcher: Motifs → Plans
    ├── PositionCharacterizer: Context extraction
    ├── BookStyleRenderer: Template selection
    ↓
    Narrative JSON response
```

---

## Frontend Integration

### Narrative Controller
`ui/analyse/src/narrative/narrativeCtrl.ts`

### Narrative View
`ui/analyse/src/narrative/narrativeView.ts`

### Bookmaker Orchestration
`ui/analyse/src/bookmaker.ts`
