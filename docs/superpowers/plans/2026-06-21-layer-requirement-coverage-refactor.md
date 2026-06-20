# Layer Requirement Coverage Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make layer requirement, graph coverage, support cluster eligibility, and event cluster eligibility speak through shared existing model contracts while preserving current behavior.

**Architecture:** Keep `ClaimTruthPolicy` as the per-claim certification authority and keep `JudgmentQualityMetrics` as packet-level diagnostics. Move only exact duplicate classification sets into existing model/packet objects; do not create a new policy module in this pass.

**Tech Stack:** Scala 3, sbt, chessJudgmentCore.

---

## Findings To Preserve

- `ClaimTruthPolicy` works on claim-bound evidence records, not global packet layer presence.
- `JudgmentLayerGapProfile` currently works on packet-global slots and can disagree with per-claim certification.
- No required `EvidenceLayer` is producerless; bottlenecks are narrow gates and duplicated classification rules.
- `Line` and `Eval` are the main upstream bottleneck because legal replay failure suppresses both and blocks relative assessment.
- `FeatureAnchor` / `ApplicabilityAssessment` and `PlanPressure` / `PlanTransition` have layer-presence checks that are weaker than their family proof gates.
- `supportStatus` for deferred/rejected claims is evaluated before arbitration but final packet claims currently expose only certified claims.

## Files

- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/model/judgment/JudgmentPacket.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/assembly/ClaimCandidateGraphAssembler.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/assembly/JudgmentPacketValidator.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/policy/ClaimTruthPolicy.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/qc/JudgmentQualityMetrics.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/test/scala/lila/chessjudgment/analysis/qc/MoveReviewPhase3AuditRunner.scala`

Do not create new runtime modules or new public APIs in this cleanup pass.

---

### Task 1: Baseline Verification

**Files:** none.

- [ ] **Step 1: Confirm current dirty state**

Run:

```powershell
git status --short
```

Expected: existing dirty chessJudgmentCore files are present. Do not revert them.

- [ ] **Step 2: Run baseline compile/test**

Run:

```powershell
cd C:\Codes\CondensedChess-master-restored\lila-docker\repos\lila
sbt --no-server "chessJudgmentCore/test"
```

Expected: `[success]`; if it fails, stop and investigate before refactoring.

---

### Task 2: Remove Remaining Family-Set Duplicates

**Files:**
- Modify: `JudgmentPacket.scala`
- Modify: `JudgmentPacketValidator.scala`
- Modify: `MoveReviewPhase3AuditRunner.scala`

- [ ] **Step 1: Find exact family-set duplicates**

Run:

```powershell
rg -n -e "longTermFamilies" -e "eventFamilies" -e "concreteFamilies" -e "ClaimFamily\.Strategic \| ClaimFamily\.PawnStructure" -e "ClaimFamily\.Tactical \| ClaimFamily\.Defensive" lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment lila-docker/repos/lila/modules/chessJudgmentCore/src/test/scala/lila/chessjudgment
```

Expected: remaining local family sets in packet cluster code, validator, and audit runner.

- [ ] **Step 2: Replace long-term family checks**

Use existing `claim.family.isLongTerm` / `family.isLongTerm`.

Expected equivalent replacements:

```scala
claim.family.isLongTerm
```

for any set containing exactly `Strategic`, `PawnStructure`, `Opening`, `Plan`.

- [ ] **Step 3: Replace event/concrete family checks**

Use existing `claim.family.isEvent` / `family.isEvent`.

Expected equivalent replacements:

```scala
claim.family.isEvent
```

for any set containing exactly `Tactical`, `Defensive`, `Conversion`, `Material`, `Evaluation`.

- [ ] **Step 4: Verify no exact duplicate family sets remain**

Run the `rg` command from Step 1 again.

Expected: no remaining exact set helpers, except scoring/ranking matches that intentionally assign different weights.

---

### Task 3: Unify Cause-Bound Layer Classification

**Files:**
- Modify: `JudgmentPacket.scala`
- Modify: `ClaimCandidateGraphAssembler.scala`
- Modify: `JudgmentPacketValidator.scala`

- [ ] **Step 1: Locate duplicated cause-bound layer sets**

Run:

```powershell
rg -n -e "causeBoundLayers" -e "eventEvidenceLayers" -e "eventInteractionEvidenceLayers" -e "RelativeAssessment" -e "MoveVerdictCertification" lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/model/judgment/JudgmentPacket.scala lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/assembly/ClaimCandidateGraphAssembler.scala lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/assembly/JudgmentPacketValidator.scala
```

Expected: duplicate sets in claim dedup, support cluster eligibility, event cluster evidence filtering, and validator checks.

- [ ] **Step 2: Add a narrow existing-object helper**

In `ClaimSupportCluster` inside `JudgmentPacket.scala`, expose a package-visible predicate around the existing support-cluster cause-bound set:

```scala
private[chessjudgment] def causeBoundLayer(layer: EvidenceLayer): Boolean =
  causeBoundLayers.contains(layer)
```

Keep the existing private set in the same object. Do not move it to a new module.

- [ ] **Step 3: Reuse it in `ClaimCandidateGraphAssembler`**

Replace local `causeBoundLayers.contains(ref.layer)` checks with:

```scala
ClaimSupportCluster.causeBoundLayer(ref.layer)
```

Remove the local duplicate set if it becomes unused.

- [ ] **Step 4: Reuse it in validator only where semantics match**

For long-term support cluster leakage checks, use `ClaimSupportCluster.causeBoundLayer`.

Do not replace event-layer allowlists unless the set is exactly the same. Event evidence layers and cause-bound layers are related but not identical.

---

### Task 4: Consolidate Cause-Kind Membership Where Equivalent

**Files:**
- Modify: `ClaimCandidateGraphAssembler.scala`
- Modify: `ClaimTruthPolicy.scala`
- Modify: `JudgmentQualityMetrics.scala`

- [ ] **Step 1: Find cause-kind predicates**

Run:

```powershell
rg -n -e "tactical.*Cause" -e "defensive.*Cause" -e "material.*Cause" -e "conversion.*Cause" -e "ClaimEventCluster\.kindForCause" lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment
```

Expected: membership predicates in truth policy, arbitration, QC, and `ClaimEventCluster.kindForCause`.

- [ ] **Step 2: Replace pure event-kind membership checks**

For checks that mean event cluster kind membership, use:

```scala
ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.TacticalEvent)
ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.DefensiveEvent)
ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.ConversionEvent)
ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.MaterialEvent)
```

- [ ] **Step 3: Keep scoring and proof-specific checks local**

Do not replace cause predicates when they encode ranking weight, proof strength, or a narrower domain condition than event kind.

- [ ] **Step 4: Verify compile**

Run:

```powershell
cd C:\Codes\CondensedChess-master-restored\lila-docker\repos\lila
sbt --no-server "chessJudgmentCore/test"
```

Expected: `[success]` and no unused private helper warnings.

---

### Task 5: Add Evidence Layer Invariant Check

**Files:**
- Modify: `JudgmentPacketValidator.scala`

- [ ] **Step 1: Locate validator evidence checks**

Run:

```powershell
rg -n -e "EvidenceRecord" -e "payload.layer" -e "ref.layer" lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/assembly/JudgmentPacketValidator.scala lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/model/judgment/EvidenceGraph.scala
```

- [ ] **Step 2: Add a validator issue for layer mismatch**

In existing validator traversal over evidence records, add a check equivalent to:

```scala
record.ref.layer == record.payload.layer
```

Use the existing validation issue style in `JudgmentPacketValidator.scala`. Do not create a new validator module.

- [ ] **Step 3: Run module test**

Run:

```powershell
cd C:\Codes\CondensedChess-master-restored\lila-docker\repos\lila
sbt --no-server "chessJudgmentCore/test"
```

Expected: `[success]`.

---

### Task 6: Separate Global Coverage From Claim-Bound Requirement In Names

**Files:**
- Modify: `JudgmentQualityMetrics.scala`
- Modify: `ClaimTruthPolicy.scala`

- [ ] **Step 1: Do not change behavior**

This task is naming and local structure only. Do not change which slots are present/applicable.

- [ ] **Step 2: Rename local QC helpers for global meaning**

Inside `JudgmentLayerGapProfile`, make helper names communicate packet-global semantics:

```scala
private def globalEvidenceLayerSlot(...)
private def globalPositionEvidenceSlot(...)
```

Keep call behavior identical.

- [ ] **Step 3: Rename local truth variables for bound meaning**

Inside `ClaimTruthPolicy.evaluate`, keep public case class fields unchanged, but use local names like:

```scala
val claimBoundRecords = ...
val claimBoundLayers = ...
```

Expected: no public API change.

---

### Task 7: Optional Packet Evidence Index For QC Scan Reduction

**Files:**
- Modify: `JudgmentQualityMetrics.scala`

- [ ] **Step 1: Add a same-file private index only if repeated scans remain noisy**

Add inside `JudgmentQualityMetrics.scala` near `JudgmentLayerGapProfile`:

```scala
private final case class PacketEvidenceIndex(records: List[EvidenceRecord]):
  lazy val byLayer: Map[EvidenceLayer, List[EvidenceRecord]] =
    records.groupBy(_.ref.layer)

  def hasLayer(layer: EvidenceLayer): Boolean =
    byLayer.contains(layer)

  def recordsFor(layer: EvidenceLayer): List[EvidenceRecord] =
    byLayer.getOrElse(layer, Nil)
```

Use it only in `JudgmentLayerGapProfile.fromPacket`; do not rewrite unrelated QC logic in this task.

- [ ] **Step 2: Preserve all slot results**

Replace `packet.evidenceGraph.records.exists(_.ref.layer == layer)` with `index.hasLayer(layer)`.

Replace layer-specific payload scans only when the filtered layer is identical.

- [ ] **Step 3: Verify**

Run:

```powershell
cd C:\Codes\CondensedChess-master-restored\lila-docker\repos\lila
sbt --no-server "chessJudgmentCore/test"
```

Expected: `[success]`.

---

### Task 8: Defer Behavior Changes To A Separate Pass

**Files:** none in this pass.

- [ ] **Step 1: Do not change these in cleanup**

Do not change:

- relation slot applicability that currently only applies when relation evidence exists
- opening context slot `applicable = false`
- support/event cluster applicability breadth
- final packet dropping deferred claims
- legal replay gate behavior for `Line`/`Eval`

- [ ] **Step 2: Record follow-up behavior risks**

After cleanup, create a separate behavior-change proposal for:

- exposing deferred claim diagnostics in packet or audit output
- making support/event cluster applicability match actual cluster builder eligibility
- making relative-cause coverage follow `CandidateComparisonDiagnostic.requiresExplanatoryCause`
- resolving opening certification's non-board anchor requirement
- deciding whether `PlanTransition` should really count toward plan proof

---

## Final Verification

Run:

```powershell
cd C:\Codes\CondensedChess-master-restored\lila-docker\repos\lila
git diff --check
sbt --no-server "chessJudgmentCore/test"
```

Expected:

- `git diff --check` exits `0`
- `sbt` exits `0` with `[success]`

## Self-Review

- Spec coverage: subagent findings on truth policy, QC layer gaps, evidence producers, and claim/event clustering are represented.
- Placeholder scan: no placeholder tasks; behavior-change items are explicitly deferred.
- Type consistency: all proposed helpers are in existing files and use existing `ClaimFamily`, `EvidenceLayer`, `ClaimEventClusterKind`, `EvidenceRecord`, and `EvidenceBackedJudgmentPacket` types.
