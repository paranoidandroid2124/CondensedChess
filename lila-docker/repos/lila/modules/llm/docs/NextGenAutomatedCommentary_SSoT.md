# Next-Generation LLM Commentary Engine Architecture (SSoT)

This document serves as the Single Source of Truth (SSoT) for the future evolution of the `llm` commentary module. The goal is to elevate the commentary from a "Static Pattern Matcher" to a "Deep Causal Reasoning Engine" capable of providing Master-level, book-quality explanations that justify a premium user experience.

The current `NarrativeLexicon` and structural tagging systems are robust and expressive. The next phase focuses entirely on injecting **Deep Causality**, **Long-Term Context**, and **Domain-Specific Logic** into the engine's analytical core.

---

## 1. Counterfactual Cross-Validation (반사실 교차 검증) - [CLOSED]

### Completion definition
The feature is considered closed only when all of the following are true:
1. Two-pass generation is not neutralized by cache key collisions.
2. Evaluation perspective is consistent end-to-end (White POV source, mover POV comparison).
3. Counterfactual explanation can still be generated when played move is absent in top MultiPV.
4. Compile/tests/docs are aligned with the actual implementation.

## 2. Multi-Ply Plan Tracking (장기 계획 추적 상태 머신) - [BOOKMAKER CLOSED]

**Goal:** Understand chess as a continuous narrative rather than a sequence of isolated 1-ply snapshots. Connect setup moves to their eventual execution.

**Core Mechanisms:**
*   **Stateful Intent Memory:** Create a state machine (e.g., `PlanStateTracker`) that persists identified plans across multiple moves (e.g., recognizing that `a3` on move 15 is preparation for a `b4` minority attack much later).
*   **Phase Transitions:** Track when a plan moves from `Preparation` -> `Execution` -> `Fruition/Failure`.
*   **Contextual Callbacks:** When a critical move is played, the engine must recall the setup from earlier in the game.
    *   *Example:* "Black finally executes the d5 break. This validates the painstaking preparation started with c6 and Nbd7 five moves ago."

### Bookmaker implementation status (2026-02-22)
1. `planStateToken` now round-trips end-to-end (`/api/llm/bookmaker-position`) and remains optional for backward compatibility.
2. `PlanStateTracker` is upgraded to v2:
   - color별 `primary/secondary` continuity
   - `lastTransition(TransitionType, momentum)`
   - `lastPly` 기반 same-ply idempotent update
   - v1 JSON 읽기 호환, v2 JSON 쓰기 고정
3. continuity identity is `planId` first, `planName` fallback.
4. cache isolation now includes state fingerprint:
   - `fen|lastMove|state:<stateFp>|probe:<probeFp>`
5. narrative now consumes transition + continuity:
   - `Continuation/NaturalShift/ForcedPivot/Opportunistic` 전략 흐름 문구 반영
   - `PlanTable.isEstablished` uses `consecutivePlies >= 2`.

## 3. "Pawn Structure" Knowledge Base (폰 구조 기반 지식 베이스) - [BOOKMAKER VALIDATION]

**Goal:** Evaluate middle-game plans based on well-established, GM-level understanding of central pawn structures, rather than relying solely on engine evaluations or superficial ECO names.

**Core Mechanisms:**
*   **Structure Classifier:** A robust module capable of identifying classical pawn structures (e.g., Carlsbad, French Advance, Isolani, Hanging Pawns, Najdorf/Scheveningen center) regardless of move order or specific opening names.
*   **Structural Playbooks (The Oracle):** A database mapping identified structures to standard, high-level plans for both sides.
    *   *If Carlsbad:* White = Minority Attack on Queenside, Black = Kingside Attack or e4 break.
*   **Plan Alignment Scoring:** Evaluate player moves not just by centipawn loss, but by how well they align with the *correct* strategic playbook for the current pawn structure.

### Bookmaker implementation status (2026-02-22)
1. Structure KB models are integrated:
   - `StructureId` taxonomy (18 core + `Unknown`)
   - `StructureProfile`, `StructuralPlaybookEntry`, `PlanAlignment`, `AlignmentBand`
2. classifier/playbook/scorer pipeline is implemented:
   - `analysis/structure/PawnStructureClassifier.scala`
   - `analysis/structure/StructuralPlaybook.scala`
   - `analysis/structure/PlanAlignmentScorer.scala`
3. `CommentaryEngine.assessExtended` now computes:
   - `structureProfile` (precision-first unknown fallback)
   - `planAlignment` (0-100 with reason codes: `PA_MATCH`, `PRECOND_MISS`, `ANTI_PLAN`, `LOW_CONF`)
4. `IntegratedContext` and `ExtendedAnalysisData` carry structure/alignment fields end-to-end.
5. Narrative exposure policy remains internal-only:
   - alignment may influence candidate tone
   - structure IDs/reason codes are redacted in renderer/post-critic output
6. Latent seed precondition checks (`PawnStructureIs`) now prefer `ctx.structureProfile` and fallback to legacy heuristics when KB is disabled.
7. rollout flags are available (default safe):
   - `LLM_STRUCT_KB_ENABLED=false`
   - `LLM_STRUCT_KB_SHADOW_MODE=true`
   - `LLM_STRUCT_KB_MIN_CONFIDENCE=0.72`
8. metrics added in `LlmApi`:
   - structure coverage / unknown rate / low-confidence rate
   - structure distribution / alignment-band distribution
9. test coverage added:
   - `PawnStructureClassifierTest`
   - `PlanAlignmentScorerTest`
   - `CommentaryEngineStructureIntegrationTest`
   - `NarrativeStructureLeakTest`
   - `StructureGoldsetContractTest`
10. goldset contract file is present:
    - `src/test/resources/structure_goldset_v1.jsonl` (300 rows)
    - `src/test/resources/structure_goldset_v1_llm_curated.jsonl` (300 rows, high board diversity)
11. goldset v2 contract hardening is applied:
    - required metadata fields (`expectedTopPlanIds`, `seedPv`, `sourceGameId`, `sourcePly`, `annotators`, `adjudicatedBy`, `notes`)
    - FEN uniqueness + no cross-label conflicts
    - fixed distribution gate (`18 x 14` + `Unknown 48`)
12. offline quality evaluator is implemented:
    - `analysis/structure/PawnStructureQualityEvaluator.scala`
    - metrics: confusion matrix, per-class P/R/F1, macro-F1, unknown false-positive rate, alignment top1 accuracy
13. strict quality runner is implemented:
    - `tools/PawnStructureQualityRunner.scala` (`--strict` gate exit)
14. shadow latency gate tooling is implemented:
    - `tools/ShadowMetricsGateRunner.scala` (`3d/2k` window, p95 delta check)
    - sample shadow inputs: `docs/shadow/ShadowMetricsOps.txt`, `docs/shadow/ShadowMetricsStaging.txt`
15. CI gates are wired:
    - `.github/workflows/llm-structure-quality.yml`
    - PR: contract + quality gate tests
    - Nightly: strict quality runner + strict shadow gate runner
16. LLM-assisted curation workflow is available:
    - generator script: `src/test/resources/generate_goldset.py`
    - output target (default): `structure_goldset_v1_llm_curated.jsonl`
    - dedicated contract test: `StructureGoldsetLlmCuratedContractTest`
17. v1.1 validation policy is now explicit:
    - Dual Gate 운영: `v1 regression` + `llm_curated quality` 동시 통과
    - Internal-only exposure 유지: 구조 ID/이유코드는 사용자 출력에서 비노출
    - 상태 정의: implementation complete, validation pending until Dual Gate + shadow gate pass

## 4. Endgame Oracle (엔드게임 전용 수학적/개념적 평가기)

**Goal:** Replace the current superficial endgame heuristics with rigorous, rule-based chess theory capable of explaining *how* to convert or draw an endgame.

**Core Mechanisms:**
*   **Rule-Based Evaluators:** Implement explicit, code-level logic for fundamental endgame concepts:
    *   *Opposition (Direct, Distant, Diagonal)*
    *   *Rule of the Square*
    *   *Key Squares (for pawn promotion)*
    *   *Triangulation (Losing a tempo to gain the opposition)*
    *   *Zugzwang Detection* (Moving beyond hardcoded values to actual calculation)
*   **Theoretical Positions Library:** Recognize forced win/draw patterns (e.g., Lucena Position, Philidor Position, Vancura Defense) and explain them textually rather than simply reporting a `+5.0` or `0.0` evaluation.
    *   *Example:* "By playing Rg2, White seamlessly enters the Lucena position, guaranteeing the pawn's promotion using the classic 'building a bridge' technique."

---

*This document should be referenced when designing and implementing new analytical capabilities for the LLM module.*
