# CP Score Contract

> **All centipawn (CP) values passed to the LLM analysis pipeline MUST follow this contract.**

## Score Perspective

All CP scores are in **White absolute perspective**:

| Score | Meaning |
|:---:|:---|
| `+100` | White is ahead by approximately 1 pawn |
| `-250` | Black is ahead by approximately 2.5 pawns |
| `+2000` | White is winning decisively |
| `-2000` | Black is winning decisively |

## Mate Scores

| Score | Meaning |
|:---:|:---|
| `+M5` (or `mate = 5`) | White mates in 5 moves |
| `-M3` (or `mate = -3`) | Black mates in 3 moves |

## Internal Normalization

The `StrategicFeatureExtractorImpl` handles color normalization **internally**:

```scala
// For Black to move: lower (more negative) CP = better for Black
val sortedVars = vars.sortBy(v => 
  if (color.white) -v.effectiveScore else v.effectiveScore
)
```

## Input Sources

| Source | Perspective | Status |
|:---|:---:|:---:|
| Server Stockfish backend | White absolute | ✅ Correct |
| Stockfish WASM (client) | White absolute | ✅ Correct |
| UCI `info score cp` | White absolute | ✅ Correct |

> [!WARNING]
> If a source provides scores already normalized to side-to-move, **do not use it directly**.
> Convert to White absolute perspective first.

## Validation

Debug builds include runtime assertions:
```scala
require(vars.forall(v => v.scoreCp.abs < 10000), 
  "CP score out of expected range - verify perspective")
```
