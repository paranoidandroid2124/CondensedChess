# Endgame Pattern Quality Report

- V1 input: `modules\llm\src\test\resources\endgame_goldset_v1.jsonl`
- V2 input: `modules\llm\src\test\resources\endgame_goldset_v2_patterns.jsonl`
- Overall gate: PASS

## V1 Baseline

- Rows: 8
- Checks: 12
- Matched: 12
- Concept F1: 1.0000 (gate >= 0.85)
- Latency p95: 8.000 ms

## V2 Pattern Metrics

- Cases: 75
- Accuracy: 1.0000
- Precision: 1.0000 (gate >= 0.88)
- Recall: 1.0000
- F1: 1.0000 (gate >= 0.88)
- Macro F1: 1.0000
- Signal match: 0.7382 (checks=359, matched=265, gate >= 0.55)
- Latency p95: 3.000 ms

## Combined Latency Gate

- max(v1, v2) p95: 8.000 ms (gate <= 40.00)

## Per-Pattern

| Pattern | TP | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|
| BreakthroughSacrifice | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| ConnectedPassers | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| GoodBishopRookPawnConversion | 3 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| KeySquaresOppositionBreakthrough | 3 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| KnightBlockadeRookPawnDraw | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| Lucena | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| OppositeColoredBishopsDraw | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| OutsidePasserDecoy | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| PhilidorDefense | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| RetiManeuver | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| ShortSideDefense | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| Shouldering | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| TriangulationZugzwang | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| VancuraDefense | 2 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |
| WrongRookPawnWrongBishopFortress | 3 | 0 | 0 | 1.0000 | 1.0000 | 1.0000 |

## Gate Verdict

- v1_f1: PASS
- v2_precision: PASS
- v2_f1: PASS
- signal_match: PASS
- latency_p95: PASS
