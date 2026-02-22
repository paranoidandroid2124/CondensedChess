# Pawn Structure Quality Report

- Dataset: `modules\llm\src\test\resources\structure_goldset_v1_llm_curated.jsonl`
- Evaluated Rows: 300
- Macro-F1: 0.1771
- Unknown False Positive Rate: 1.0000
- Alignment Top1 Accuracy: 0.3175 (80/252)
- Gate: FAIL

## Thresholds
- macro-F1 >= 0.85
- alignment top1 >= 0.75
- unknown false positive <= 0.10

## Per-Class Metrics
| Label | Support | Precision | Recall | F1 |
|---|---:|---:|---:|---:|
| BenoniCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| Carlsbad | 14 | 0.1556 | 1.0000 | 0.2692 |
| FianchettoShell | 14 | 0.5000 | 1.0000 | 0.6667 |
| FluidCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| FrenchAdvanceChain | 14 | 0.0000 | 0.0000 | 0.0000 |
| HangingPawnsBlack | 14 | 0.4286 | 0.8571 | 0.5714 |
| HangingPawnsWhite | 14 | 0.2857 | 0.8571 | 0.4286 |
| Hedgehog | 14 | 0.0000 | 0.0000 | 0.0000 |
| IQPBlack | 14 | 0.4286 | 0.8571 | 0.5714 |
| IQPWhite | 14 | 0.8571 | 0.8571 | 0.8571 |
| KIDLockedCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| LockedCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| MaroczyBind | 14 | 0.0000 | 0.0000 | 0.0000 |
| NajdorfScheveningenCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| OpenCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| SlavCaroTriangle | 14 | 0.0000 | 0.0000 | 0.0000 |
| Stonewall | 14 | 0.0000 | 0.0000 | 0.0000 |
| SymmetricCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| Unknown | 48 | 0.0000 | 0.0000 | 0.0000 |

## Confusion (non-zero rows)
- BenoniCenter -> Unknown:14
- Carlsbad -> Carlsbad:14
- FianchettoShell -> FianchettoShell:14
- FluidCenter -> HangingPawnsBlack:14
- FrenchAdvanceChain -> Carlsbad:14
- HangingPawnsBlack -> HangingPawnsBlack:12, HangingPawnsWhite:2
- HangingPawnsWhite -> HangingPawnsWhite:12, HangingPawnsBlack:2
- Hedgehog -> HangingPawnsWhite:14
- IQPBlack -> IQPBlack:12, IQPWhite:2
- IQPWhite -> IQPWhite:12, IQPBlack:2
- KIDLockedCenter -> Unknown:14
- LockedCenter -> Carlsbad:14
- MaroczyBind -> FianchettoShell:14
- NajdorfScheveningenCenter -> Unknown:14
- OpenCenter -> IQPBlack:14
- SlavCaroTriangle -> Unknown:14
- Stonewall -> HangingPawnsWhite:14
- SymmetricCenter -> Unknown:14
- Unknown -> Carlsbad:48

## Sample Mismatches
| id | gt | pred |
|---|---|---|
| IQPWhite-13 | IQPWhite | IQPBlack |
| IQPWhite-14 | IQPWhite | IQPBlack |
| IQPBlack-13 | IQPBlack | IQPWhite |
| IQPBlack-14 | IQPBlack | IQPWhite |
| HangingPawnsWhite-13 | HangingPawnsWhite | HangingPawnsBlack |
| HangingPawnsWhite-14 | HangingPawnsWhite | HangingPawnsBlack |
| HangingPawnsBlack-13 | HangingPawnsBlack | HangingPawnsWhite |
| HangingPawnsBlack-14 | HangingPawnsBlack | HangingPawnsWhite |
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
| FrenchAdvanceChain-12 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-13 | FrenchAdvanceChain | Carlsbad |
| FrenchAdvanceChain-14 | FrenchAdvanceChain | Carlsbad |
| NajdorfScheveningenCenter-1 | NajdorfScheveningenCenter | Unknown |
| NajdorfScheveningenCenter-2 | NajdorfScheveningenCenter | Unknown |
| NajdorfScheveningenCenter-3 | NajdorfScheveningenCenter | Unknown |
