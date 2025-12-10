# Extension Guide: How to Add new features

This guide explains how to extend Condensed Chess with new analytical concepts or narrative sections.

## 1. Adding a New Concept Tag
"I want the engine to detect 'Greek Gift Sacrifice' or 'Space Advantage'."

### Steps
1.  **Define the Tag Enum**:
    -   Go to `core/src/main/scala/analysis/AnalysisModel.scala`.
    -   Add a case to `enum EvaluationTag`.
    
2.  **Implement Logic (`ConceptLabeler`)**:
    -   Go to `core/src/main/scala/analysis/ConceptLabeler.scala`.
    -   Add a new function `hasGreekGift(fen: String, moves: List[Move])`.
    -   Register it in the `label()` function pipeline.
    
3.  **Document it**:
    -   Update `CONCEPT_TAGS.md` with the new tag definition.

### Available Tags (Phase 20+)
- **Analysis**: `Greed`, `Refuted`, `Premature`, `Fear`.
- **Structure**: `Iqp`, `HangingPawns`, `MinorityAttack`.
- **Endgame**: `RookBehindPassedPawn`, `KingActivity`, `WrongBishop`.

---

## 2. Adding a New Book Section
"I want a special 'Tactical Summary' page in the review."

### Steps
1.  **Define SectionType**:
    -   Go to `core/src/main/scala/analysis/AnalysisModel.scala`.
    -   Add `case TacticalSummary` to `enum SectionType`.

2.  **Implement Builder Logic**:
    -   Go to `core/src/main/scala/analysis/StudyChapterBuilder.scala`.
    -   Create a function `buildTacticalSummary(timeline: Timeline): StudySection`.
    -   Add it to the `buildSections` list.

3.  **Update Frontend**:
    -   Go to `frontend/src/app/study/[id]/components/ReadingMode.tsx`.
    -   Add a case for `SectionType.TacticalSummary` and render your new component.

---

## 3. Adding New Engine Experiments
"I want to verify if a sacrifice is sound by running a deep search."

### Steps
1.  **Define ExperimentType**:
    -   `AnalysisTypes.scala` -> `enum ExperimentType`.

2.  **Implement Runner**:
    -   `ExperimentRunner.scala` -> Add `verifySacrifice(fen, move)`.
    -   Use `engineService.submit()` to run the custom analysis.

3.  **Integrate**:
    -   Call your new experiment in `TimelineBuilder` or `CriticalDetector`.
