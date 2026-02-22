# Pawn Structure Quality Report

- Dataset: `modules\llm\src\test\resources\structure_goldset_v1.jsonl`
- Evaluated Rows: 300
- Macro-F1: 0.5263
- Unknown False Positive Rate: 1.0000
- Alignment Top1 Accuracy: 0.6111 (154/252)
- Gate: FAIL

## Thresholds
- macro-F1 >= 0.85
- alignment top1 >= 0.75
- unknown false positive <= 0.10

## Per-Class Metrics
| Label | Support | Precision | Recall | F1 |
|---|---:|---:|---:|---:|
| BenoniCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| Carlsbad | 14 | 0.0000 | 0.0000 | 0.0000 |
| FianchettoShell | 14 | 1.0000 | 1.0000 | 1.0000 |
| FluidCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| FrenchAdvanceChain | 14 | 0.0000 | 0.0000 | 0.0000 |
| HangingPawnsBlack | 14 | 0.2000 | 1.0000 | 0.3333 |
| HangingPawnsWhite | 14 | 0.5000 | 1.0000 | 0.6667 |
| Hedgehog | 14 | 1.0000 | 1.0000 | 1.0000 |
| IQPBlack | 14 | 1.0000 | 1.0000 | 1.0000 |
| IQPWhite | 14 | 1.0000 | 1.0000 | 1.0000 |
| KIDLockedCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| LockedCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| MaroczyBind | 14 | 0.0000 | 0.0000 | 0.0000 |
| NajdorfScheveningenCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| OpenCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| SlavCaroTriangle | 14 | 0.0000 | 0.0000 | 0.0000 |
| Stonewall | 14 | 0.0000 | 0.0000 | 0.0000 |
| SymmetricCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| Unknown | 48 | 0.0000 | 0.0000 | 0.0000 |

## Confusion (non-zero rows)
- BenoniCenter -> HangingPawnsBlack:14
- Carlsbad -> HangingPawnsBlack:14
- FianchettoShell -> FianchettoShell:14
- FluidCenter -> FluidCenter:14
- FrenchAdvanceChain -> Carlsbad:14
- HangingPawnsBlack -> HangingPawnsBlack:14
- HangingPawnsWhite -> HangingPawnsWhite:14
- Hedgehog -> Hedgehog:14
- IQPBlack -> IQPBlack:14
- IQPWhite -> IQPWhite:14
- KIDLockedCenter -> KIDLockedCenter:14
- LockedCenter -> LockedCenter:14
- MaroczyBind -> Unknown:14
- NajdorfScheveningenCenter -> HangingPawnsBlack:14
- OpenCenter -> OpenCenter:14
- SlavCaroTriangle -> HangingPawnsBlack:14
- Stonewall -> HangingPawnsWhite:14
- SymmetricCenter -> SymmetricCenter:14
- Unknown -> Carlsbad:48

## Sample Mismatches
| id | gt | pred |
|---|---|---|
| Carlsbad-1 | Carlsbad | HangingPawnsBlack |
| Carlsbad-2 | Carlsbad | HangingPawnsBlack |
| Carlsbad-3 | Carlsbad | HangingPawnsBlack |
| Carlsbad-4 | Carlsbad | HangingPawnsBlack |
| Carlsbad-5 | Carlsbad | HangingPawnsBlack |
| Carlsbad-6 | Carlsbad | HangingPawnsBlack |
| Carlsbad-7 | Carlsbad | HangingPawnsBlack |
| Carlsbad-8 | Carlsbad | HangingPawnsBlack |
| Carlsbad-9 | Carlsbad | HangingPawnsBlack |
| Carlsbad-10 | Carlsbad | HangingPawnsBlack |
| Carlsbad-11 | Carlsbad | HangingPawnsBlack |
| Carlsbad-12 | Carlsbad | HangingPawnsBlack |
| Carlsbad-13 | Carlsbad | HangingPawnsBlack |
| Carlsbad-14 | Carlsbad | HangingPawnsBlack |
| FrenchAdvanceChain-1 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-2 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-3 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-4 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-5 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-6 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-7 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-8 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-9 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-10 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-11 | FrenchAdvanceChain | Carlsbad |
