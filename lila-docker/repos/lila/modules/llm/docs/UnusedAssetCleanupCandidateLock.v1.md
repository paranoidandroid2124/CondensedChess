# Unused Asset Cleanup Candidate Lock v1

## Summary
- Scope is locked to **confirmed unused only**.
- Criteria: **Scala repo-wide static references**; symbols with no runtime call-site references outside their own definition are candidates.
- Cold-path/fallback usage is explicitly excluded from this lock and deferred.

## Confirmed Candidates (Delete/Consolidate Review)
1. `MoveAnalyzer.compareMove`  
   `modules/llm/src/main/scala/lila/llm/analysis/MoveAnalyzer.scala:114`
2. `MoveAnalyzer.generateHypotheses`  
   `modules/llm/src/main/scala/lila/llm/analysis/MoveAnalyzer.scala:158`
3. `CounterfactualAnalyzer.analyzeDivergence`  
   `modules/llm/src/main/scala/lila/llm/analysis/CounterfactualAnalyzer.scala:29`
4. `NarrativeGenerator.describeMove`  
   `modules/llm/src/main/scala/lila/llm/analysis/NarrativeGenerator.scala:28`
5. `NarrativeGenerator.describeCounterfactual`  
   `modules/llm/src/main/scala/lila/llm/analysis/NarrativeGenerator.scala:57`
6. `NarrativeGenerator.describeExtended`  
   `modules/llm/src/main/scala/lila/llm/analysis/NarrativeGenerator.scala:144`
7. Unused scopes: `FactScope.PV`, `FactScope.Hypothesis`  
   `modules/llm/src/main/scala/lila/llm/model/Fact.scala:11`  
   `modules/llm/src/main/scala/lila/llm/model/Fact.scala:12`
8. Unreferenced fields: `OpeningReference.isNoveltyCandidate`, `OpeningReference.outOfBook`  
   `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala:518`  
   `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala:519`

## Deferred to Next Round
1. `NarrativeGenerator.describeHierarchical` meta/decision/probe rendering block  
   Reason: cold-path/fallback exists; not "unused".
2. `LatentPlanSeeder` / `ProbeDetector` family  
   Reason: active references exist in current pipeline.

## Reference Validation Snapshot
- Symbol self-definition hits were found, but no external Scala call-site references were found for the confirmed functions.
- Main rendering path remains:
  - `CommentaryEngine` uses book style first and fallback second:  
    `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:515`  
    `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:516`
  - Book path calls outline builder via renderer:  
    `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala:20`

## Public API / Interface Impact
- This lock performs **candidate confirmation only**.
- No external API or JSON schema changes.
- Actual deletion phase will re-verify `CommentResponse`, fixtures, and serialization compatibility.

## Validation Commands
```powershell
rg -n "<symbol>" c:\Codes\CondensedChess -g "*.scala"
rg -n "BookStyleRenderer\.render|NarrativeOutlineBuilder\.build|describeHierarchical" c:\Codes\CondensedChess\lila-docker\repos\lila\modules\llm\src\main\scala\lila\llm -g "*.scala"
rg -n "FactScope\.PV|FactScope\.Hypothesis|isNoveltyCandidate|outOfBook" c:\Codes\CondensedChess -g "*.scala"
```

## Assumptions / Defaults
1. Static Scala references are the lock criterion; runtime reflection-based usage is assumed absent.
2. Test-only references are treated as runtime-unused for cleanup prioritization.
3. This document is a lock artifact, not a deletion execution.
