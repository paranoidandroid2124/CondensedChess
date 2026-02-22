# Pawn Structure Quality Report

- Mode: DualGate
- Inputs: `modules\llm\src\test\resources\structure_goldset_v1.jsonl`, `modules\llm\src\test\resources\structure_goldset_v1_llm_curated.jsonl`
- Overall Gate: PASS
- Min Confidence: 0.72 | Min Margin: 0.10

---

## Dataset: `modules\llm\src\test\resources\structure_goldset_v1.jsonl`

- Evaluated Rows: 300
- Macro-F1: 1.0000
- Unknown False Positive Rate: 0.0000
- Alignment Top1 Accuracy: 0.8889 (224/252)
- Gate: PASS

### Thresholds
- macro-F1 >= 0.80
- alignment top1 >= 0.70
- unknown false positive <= 0.15

### Per-Class Metrics
| Label | Support | Precision | Recall | F1 |
|---|---:|---:|---:|---:|
| BenoniCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| Carlsbad | 14 | 1.0000 | 1.0000 | 1.0000 |
| FianchettoShell | 14 | 1.0000 | 1.0000 | 1.0000 |
| FluidCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| FrenchAdvanceChain | 14 | 1.0000 | 1.0000 | 1.0000 |
| HangingPawnsBlack | 14 | 1.0000 | 1.0000 | 1.0000 |
| HangingPawnsWhite | 14 | 1.0000 | 1.0000 | 1.0000 |
| Hedgehog | 14 | 1.0000 | 1.0000 | 1.0000 |
| IQPBlack | 14 | 1.0000 | 1.0000 | 1.0000 |
| IQPWhite | 14 | 1.0000 | 1.0000 | 1.0000 |
| KIDLockedCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| LockedCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| MaroczyBind | 14 | 1.0000 | 1.0000 | 1.0000 |
| NajdorfScheveningenCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| OpenCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| SlavCaroTriangle | 14 | 1.0000 | 1.0000 | 1.0000 |
| Stonewall | 14 | 1.0000 | 1.0000 | 1.0000 |
| SymmetricCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| Unknown | 48 | 1.0000 | 1.0000 | 1.0000 |

### Confusion (non-zero rows)
- BenoniCenter -> BenoniCenter:14
- Carlsbad -> Carlsbad:14
- FianchettoShell -> FianchettoShell:14
- FluidCenter -> FluidCenter:14
- FrenchAdvanceChain -> FrenchAdvanceChain:14
- HangingPawnsBlack -> HangingPawnsBlack:14
- HangingPawnsWhite -> HangingPawnsWhite:14
- Hedgehog -> Hedgehog:14
- IQPBlack -> IQPBlack:14
- IQPWhite -> IQPWhite:14
- KIDLockedCenter -> KIDLockedCenter:14
- LockedCenter -> LockedCenter:14
- MaroczyBind -> MaroczyBind:14
- NajdorfScheveningenCenter -> NajdorfScheveningenCenter:14
- OpenCenter -> OpenCenter:14
- SlavCaroTriangle -> SlavCaroTriangle:14
- Stonewall -> Stonewall:14
- SymmetricCenter -> SymmetricCenter:14
- Unknown -> Unknown:48

---

## Dataset: `modules\llm\src\test\resources\structure_goldset_v1_llm_curated.jsonl`

- Evaluated Rows: 300
- Macro-F1: 0.9106
- Unknown False Positive Rate: 0.0000
- Alignment Top1 Accuracy: 0.8333 (210/252)
- Gate: PASS

### Thresholds
- macro-F1 >= 0.85
- alignment top1 >= 0.75
- unknown false positive <= 0.10

### Per-Class Metrics
| Label | Support | Precision | Recall | F1 |
|---|---:|---:|---:|---:|
| BenoniCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| Carlsbad | 14 | 1.0000 | 1.0000 | 1.0000 |
| FianchettoShell | 14 | 1.0000 | 1.0000 | 1.0000 |
| FluidCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| FrenchAdvanceChain | 14 | 1.0000 | 1.0000 | 1.0000 |
| HangingPawnsBlack | 14 | 0.8571 | 0.8571 | 0.8571 |
| HangingPawnsWhite | 14 | 0.8571 | 0.8571 | 0.8571 |
| Hedgehog | 14 | 1.0000 | 1.0000 | 1.0000 |
| IQPBlack | 14 | 0.8571 | 0.8571 | 0.8571 |
| IQPWhite | 14 | 0.8571 | 0.8571 | 0.8571 |
| KIDLockedCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| LockedCenter | 14 | 0.0000 | 0.0000 | 0.0000 |
| MaroczyBind | 14 | 1.0000 | 1.0000 | 1.0000 |
| NajdorfScheveningenCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| OpenCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| SlavCaroTriangle | 14 | 1.0000 | 1.0000 | 1.0000 |
| Stonewall | 14 | 1.0000 | 1.0000 | 1.0000 |
| SymmetricCenter | 14 | 1.0000 | 1.0000 | 1.0000 |
| Unknown | 48 | 0.7742 | 1.0000 | 0.8727 |

### Confusion (non-zero rows)
- BenoniCenter -> BenoniCenter:14
- Carlsbad -> Carlsbad:14
- FianchettoShell -> FianchettoShell:14
- FluidCenter -> FluidCenter:14
- FrenchAdvanceChain -> FrenchAdvanceChain:14
- HangingPawnsBlack -> HangingPawnsBlack:12, HangingPawnsWhite:2
- HangingPawnsWhite -> HangingPawnsWhite:12, HangingPawnsBlack:2
- Hedgehog -> Hedgehog:14
- IQPBlack -> IQPBlack:12, IQPWhite:2
- IQPWhite -> IQPWhite:12, IQPBlack:2
- KIDLockedCenter -> KIDLockedCenter:14
- LockedCenter -> Unknown:14
- MaroczyBind -> MaroczyBind:14
- NajdorfScheveningenCenter -> NajdorfScheveningenCenter:14
- OpenCenter -> OpenCenter:14
- SlavCaroTriangle -> SlavCaroTriangle:14
- Stonewall -> Stonewall:14
- SymmetricCenter -> SymmetricCenter:14
- Unknown -> Unknown:48

### Sample Mismatches
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
| LockedCenter-1 | LockedCenter | Unknown |
| LockedCenter-2 | LockedCenter | Unknown |
| LockedCenter-3 | LockedCenter | Unknown |
| LockedCenter-4 | LockedCenter | Unknown |
| LockedCenter-5 | LockedCenter | Unknown |
| LockedCenter-6 | LockedCenter | Unknown |
| LockedCenter-7 | LockedCenter | Unknown |
| LockedCenter-8 | LockedCenter | Unknown |
| LockedCenter-9 | LockedCenter | Unknown |
| LockedCenter-10 | LockedCenter | Unknown |
| LockedCenter-11 | LockedCenter | Unknown |
| LockedCenter-12 | LockedCenter | Unknown |
| LockedCenter-13 | LockedCenter | Unknown |
| LockedCenter-14 | LockedCenter | Unknown |
