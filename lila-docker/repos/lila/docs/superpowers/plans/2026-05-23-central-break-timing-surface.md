# Central Break Timing Surface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Project already-verified `central_break_timing` exact witnesses into a safe MoveReview `moveReviewPlayerSurface.summaryRows` support row.

**Architecture:** Reuse the existing `MoveReviewSupportedLocalSurfaceRows` path and public `MoveReviewPlayerSurfaceRow` schema. Do not add a new runtime module, frontend schema, or raw-carrier reconstruction path. The row is admitted only from an existing `central_break_timing` packet plus `CentralBreakTimingWitness.exact(ctx)`.

**Tech Stack:** Scala 3, munit, sbt, existing Chesstory commentary runtime and commentaryTools QC runners.

---

## File Map

- Modify `modules/commentaryCore/src/main/scala/lila/commentary/analysis/claim/ClaimAuthorityResolver.scala`
  - Add a central-break SupportedLocal packet admission helper.
  - Reuse existing tactical veto, `supportsLocalMoveDelta`, and packet token helpers.
- Modify `modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewPlayerPayloadBuilder.scala`
  - Extend `MoveReviewSupportedLocalSurfaceRows`.
  - Add a private in-file `CentralBreakTimingSurfaceGate`.
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/analysis/MoveReviewSupportedLocalSurfaceRowsTest.scala`
  - Add exact central positive/negative row tests.
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/tools/moveReview/MoveReviewCoverageDiagnostics.scala`
  - Align central admitted diagnostics with product-visible surface gate.
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/tools/moveReview/MoveReviewCoverageDiagnosticsTest.scala`
  - Add central diagnostic tests.
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewEvidenceCoverageAudit.scala`
  - Add `Central break` row quality counters.
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewEvidenceCoverageAuditTest.scala`
  - Add central audit tests.
- Modify `modules/commentary/docs/CommentaryPipelineSSOT.md`
  - Document central-break supported-local surface projection.
- Modify `modules/commentary/docs/CommentaryTrustBoundary.md`
  - Document exact-witness and tactical-veto constraints.
- Modify `modules/commentary/docs/CommentaryTruthBoundary.md`
  - Reaffirm that taxonomy alone does not certify central-break truth.

---

### Task 1: Add Central-Break Admission API

**Files:**
- Modify `modules/commentaryCore/src/main/scala/lila/commentary/analysis/claim/ClaimAuthorityResolver.scala`

- [ ] **Step 1: Add the admission result type**

Add near `SupportedLocalNeutralizeKeyBreakAdmission`:

```scala
final case class SupportedLocalCentralBreakTimingAdmission(
    packet: PlayerFacingClaimPacket,
    witness: CentralBreakTimingWitness.Witness,
    decision: ClaimAuthorityDecision
)
```

- [ ] **Step 2: Add public internal helpers**

Add near `supportedLocalNeutralizeKeyBreakPacketDecision`:

```scala
def supportedLocalCentralBreakTimingPacketDecision(
    ctx: Option[NarrativeContext],
    inputs: QuestionPlannerInputs,
    truthContract: Option[DecisiveTruthContract],
    packet: PlayerFacingClaimPacket
): ClaimAuthorityDecision =
  supportedLocalCentralBreakTimingAdmission(ctx, inputs, truthContract, packet)
    .map(_.decision)
    .getOrElse(ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, authorityFailureCodes(packet)))

def supportedLocalCentralBreakTimingAdmission(
    ctx: Option[NarrativeContext],
    inputs: QuestionPlannerInputs,
    truthContract: Option[DecisiveTruthContract],
    packet: PlayerFacingClaimPacket
): Option[SupportedLocalCentralBreakTimingAdmission] =
  if packet.proofSource != CentralBreakTimingWitness.ProofSource ||
      packet.proofFamily != CentralBreakTimingWitness.ProofFamily
  then None
  else
    for
      narrativeCtx <- ctx
      witness <- CentralBreakTimingWitness.exact(narrativeCtx)
      if supportsLocalMoveDelta(packet)
      if centralBreakTimingWitnessMatchesPacket(witness, packet)
    yield
      val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
      val decision =
        if tacticalReasons.nonEmpty then ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
        else ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
      SupportedLocalCentralBreakTimingAdmission(packet, witness, decision)
```

- [ ] **Step 3: Add witness/packet overlap helper**

Add below `timingWitnessTokens(packet)`:

```scala
private def centralBreakTimingWitnessMatchesPacket(
    witness: CentralBreakTimingWitness.Witness,
    packet: PlayerFacingClaimPacket
): Boolean =
  val witnessTokens =
    (
      witness.ownerSeedTerms ++
        witness.structureTransitionTerms ++
        List(witness.breakMove, witness.breakSquare, witness.breakToken)
    ).flatMap(witnessTokenVariants).filter(validTimingWitnessToken).toSet
  timingWitnessTokens(packet).exists(witnessTokens.contains)
```

- [ ] **Step 4: Run compile/test slice**

Run:

```powershell
sbt "commentaryTools/testOnly lila.commentary.analysis.CentralBreakTimingPolicyTest"
```

Expected: PASS. Any compile failure should be fixed before Task 2.

---

### Task 2: Project `Central break` Support Row

**Files:**
- Modify `modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewPlayerPayloadBuilder.scala`
- Test `modules/commentaryTools/src/test/scala/lila/commentary/analysis/MoveReviewSupportedLocalSurfaceRowsTest.scala`

- [ ] **Step 1: Add failing positive test**

In `MoveReviewSupportedLocalSurfaceRowsTest`, add a fixture based on the existing Maderna exact central position:

```scala
private val MadernaExactFen =
  "nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17"

private val MadernaExactLines =
  List(
    VariationLine(List("e4e5", "d6e5", "f4e5", "d7e5", "c4e5"), scoreCp = 82, depth = 18),
    VariationLine(List("c1e3", "b7b5", "a5b6"), scoreCp = 36, depth = 18)
  )
```

Add helper:

```scala
private def centralScene(
    fen: String = MadernaExactFen,
    ply: Int = 33,
    playedMove: String = "e4e5",
    lines: List[VariationLine] = MadernaExactLines,
    truthContract: Option[DecisiveTruthContract] = None
): (NarrativeContext, QuestionPlannerInputs, RankedQuestionPlans) =
  val data =
    CommentaryEngine
      .assessExtended(
        fen = fen,
        variations = lines,
        playedMove = Some(playedMove),
        phase = Some("middlegame"),
        ply = ply,
        prevMove = Some(playedMove)
      )
      .getOrElse(fail(s"analysis missing for $fen"))
  val ctx = NarrativeContextBuilder.build(data, data.toContext, None)
  val pack = StrategyPackBuilder.build(data, ctx).getOrElse(fail(s"strategy pack missing for $fen"))
  val inputs = QuestionPlannerInputsBuilder.build(ctx, Some(pack), truthContract)
  val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract)
  (ctx, inputs, ranked)
```

Add test:

```scala
test("projects exact central_break_timing packet into a central break row") {
  val (ctx, inputs, ranked) = centralScene()

  val rows =
    MoveReviewSupportedLocalSurfaceRows.build(
      ctx = ctx,
      inputs = inputs,
      rankedPlans = ranked,
      truthContract = None
    )

  assertEquals(rows.map(_.label), List("Central break"))
  assertEquals(rows.head.text, "On the checked line, this improves the e-break timing on this branch.")
  assertEquals(rows.head.source, None)
  assert(!rows.exists(_.text.contains("central_break_timing")), clue(rows))
}
```

- [ ] **Step 2: Run test and confirm RED**

Run:

```powershell
sbt "commentaryTools/testOnly lila.commentary.analysis.MoveReviewSupportedLocalSurfaceRowsTest"
```

Expected: FAIL because no `Central break` row is emitted.

- [ ] **Step 3: Add central surface gate**

In `MoveReviewPlayerPayloadBuilder.scala`, add after `NeutralizeKeyBreakSurfaceGate`:

```scala
private[commentary] object CentralBreakTimingSurfaceGate:

  val MissingExactWitness = "surface:central_break_exact_witness_missing"
  val MalformedBreakToken = "surface:central_break_token_malformed"

  final case class Decision(token: Option[String], rejectReason: Option[String]):
    def admitted: Boolean = token.nonEmpty && rejectReason.isEmpty

  def decide(witness: CentralBreakTimingWitness.Witness): Decision =
    val token = Option(witness.breakToken).map(_.trim).filter(_.nonEmpty)
    token match
      case Some(value) if value.matches("""(?:\.\.\.)?[de]-break""") =>
        Decision(Some(value), None)
      case Some(_) =>
        Decision(None, Some(MalformedBreakToken))
      case None =>
        Decision(None, Some(MissingExactWitness))

  def surfaceText(token: String): String =
    s"On the checked line, this improves the $token timing on this branch."
```

- [ ] **Step 4: Extend row projection**

In `MoveReviewSupportedLocalSurfaceRows`, add:

```scala
private val CentralBreakLabel = "Central break"
```

Replace `rowForClaim` body with packet-family dispatch:

```scala
private def rowForClaim(
    ctx: NarrativeContext,
    inputs: QuestionPlannerInputs,
    truthContract: Option[DecisiveTruthContract],
    claim: MainPathScopedClaim
): Option[MoveReviewPlayerSurfaceRow] =
  claim.packet.flatMap { packet =>
    if packet.proofSource == ProofSourceId.CounterplayAxisSuppression.wireKey &&
        packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey
    then
      ClaimAuthorityResolver
        .supportedLocalNeutralizeKeyBreakPacketDecision(
          ctx = Some(ctx),
          inputs = inputs,
          truthContract = truthContract,
          packet = packet
        )
        .filter(decision => decision.tier == ClaimAuthorityTier.SupportedLocal && decision.vetoReasons.isEmpty)
        .flatMap(_ => NeutralizeKeyBreakSurfaceGate.decideForPacket(packet, ctx).token)
        .flatMap(token => row(CounterplayBreakLabel, NeutralizeKeyBreakSurfaceGate.surfaceText(token)))
    else if packet.proofSource == CentralBreakTimingWitness.ProofSource &&
        packet.proofFamily == CentralBreakTimingWitness.ProofFamily
    then
      ClaimAuthorityResolver
        .supportedLocalCentralBreakTimingAdmission(
          ctx = Some(ctx),
          inputs = inputs,
          truthContract = truthContract,
          packet = packet
        )
        .filter(admission =>
          admission.decision.tier == ClaimAuthorityTier.SupportedLocal &&
            admission.decision.vetoReasons.isEmpty
        )
        .flatMap(admission => CentralBreakTimingSurfaceGate.decide(admission.witness).token)
        .flatMap(token => row(CentralBreakLabel, CentralBreakTimingSurfaceGate.surfaceText(token)))
    else None
  }
```

- [ ] **Step 5: Run test and confirm GREEN**

Run:

```powershell
sbt "commentaryTools/testOnly lila.commentary.analysis.MoveReviewSupportedLocalSurfaceRowsTest"
```

Expected: PASS.

---

### Task 3: Add Central Negative Row Tests

**Files:**
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/analysis/MoveReviewSupportedLocalSurfaceRowsTest.scala`

- [ ] **Step 1: Add tactical veto test**

Add:

```scala
test("suppresses central_break_timing row when tactical truth mode vetoes SupportedLocal") {
  val truth = tacticalTruthContract(playedMove = "e4e5")
  val (ctx, inputs, ranked) = centralScene(truthContract = Some(truth))

  val rows =
    MoveReviewSupportedLocalSurfaceRows.build(
      ctx = ctx,
      inputs = inputs,
      rankedPlans = ranked,
      truthContract = Some(truth)
    )

  assert(!rows.exists(_.label == "Central break"), clue(rows))
}
```

Use or add a local `tacticalTruthContract(playedMove: String)` helper matching the existing neutralize/central policy tests.

- [ ] **Step 2: Add plan-only/no-board-link test**

Add:

```scala
private val DirectBreakFen =
  "rnbqk2r/pp2bppp/4pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1 w kq - 0 8"

test("does not project plan-only central_break_timing review evidence") {
  val lines =
    List(
      VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 72, depth = 18),
      VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 28, depth = 18)
    )
  val (ctx, inputs, ranked) =
    centralScene(fen = DirectBreakFen, ply = 15, playedMove = "b1c3", lines = lines)

  val rows =
    MoveReviewSupportedLocalSurfaceRows.build(ctx, inputs, ranked, truthContract = None)

  assert(!rows.exists(_.label == "Central break"), clue(rows))
}
```

- [ ] **Step 3: Add low-PV-gap test**

Add:

```scala
test("does not project central_break_timing when PV gap is below forty centipawns") {
  val lines =
    List(
      VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 68, depth = 18),
      VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 31, depth = 18)
    )
  val (ctx, inputs, ranked) =
    centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

  val rows =
    MoveReviewSupportedLocalSurfaceRows.build(ctx, inputs, ranked, truthContract = None)

  assert(!rows.exists(_.label == "Central break"), clue(rows))
}
```

- [ ] **Step 4: Run targeted tests**

Run:

```powershell
sbt "commentaryTools/testOnly lila.commentary.analysis.MoveReviewSupportedLocalSurfaceRowsTest lila.commentary.analysis.CentralBreakTimingPolicyTest"
```

Expected: PASS.

---

### Task 4: Align Diagnostics And Audit

**Files:**
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/tools/moveReview/MoveReviewCoverageDiagnostics.scala`
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/tools/moveReview/MoveReviewCoverageDiagnosticsTest.scala`
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewEvidenceCoverageAudit.scala`
- Modify `modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewEvidenceCoverageAuditTest.scala`

- [ ] **Step 1: Pass context into SupportedLocal diagnostics**

Change `supportedLocalFromInputs` to accept `ctx: NarrativeContext`, and call:

```scala
supportedLocalFromPackets(mainPathPackets(inputs), tacticalVetoReasons, playedSan, playedUci, Some(ctx))
```

Update `supportedLocalFromPackets` signature:

```scala
private[moveReview] def supportedLocalFromPackets(
    packets: List[PlayerFacingClaimPacket],
    tacticalVetoReasons: List[String] = Nil,
    playedSan: Option[String] = None,
    playedUci: Option[String] = None,
    ctx: Option[NarrativeContext] = None
): SupportedLocalDiagnostic =
```

- [ ] **Step 2: Extend product-surface gate checks**

Change:

```scala
private def tacticalVetoApplies(packet: PlayerFacingClaimPacket, tacticalVetoReasons: List[String]): Boolean =
  tacticalVetoReasons.nonEmpty &&
    (packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey ||
      packet.proofFamily == CentralBreakTimingWitness.ProofFamily)
```

Change central surface admission:

```scala
private def supportedLocalSurfaceAdmitted(
    packet: PlayerFacingClaimPacket,
    playedSan: Option[String],
    playedUci: Option[String],
    ctx: Option[NarrativeContext]
): Boolean =
  if packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey then
    NeutralizeKeyBreakSurfaceGate.decideForPacket(packet, playedSan, playedUci).admitted
  else if packet.proofFamily == CentralBreakTimingWitness.ProofFamily then
    ctx.flatMap(CentralBreakTimingWitness.exact).exists(CentralBreakTimingSurfaceGate.decide(_).admitted)
  else true
```

Add central surface failure:

```scala
else if packet.proofFamily == CentralBreakTimingWitness.ProofFamily then
  ctx.flatMap(CentralBreakTimingWitness.exact)
    .map(CentralBreakTimingSurfaceGate.decide(_).rejectReason.toList)
    .getOrElse(List(CentralBreakTimingSurfaceGate.MissingExactWitness))
```

- [ ] **Step 3: Add audit counters**

In `MoveReviewEvidenceCoverageAudit.Summary`, add:

```scala
centralBreakRowCount: Int,
centralBreakNamedTokenRowCount: Int,
centralBreakGenericFallbackCount: Int,
```

Count rows with label `Central break`. Token regex:

```scala
private def centralBreakSurfaceToken(text: String): Option[String] =
  val pattern = """(?i)\b(?:improves\s+the|is\s+the)\s+((?:\.\.\.)?[de]-break)\b""".r
  pattern.findFirstMatchIn(text).map(_.group(1).toLowerCase)
```

Generic fallback detector:

```scala
private def genericCentralBreakText(text: String): Boolean =
  val low = text.trim.toLowerCase
  low.contains("local reading") ||
    low.contains("strategic point") ||
    low.contains("central_break_timing")
```

- [ ] **Step 4: Add tests**

Add one positive audit fixture row:

```scala
MoveReviewPlayerSurfaceRow(
  label = "Central break",
  text = "On the checked line, this improves the e-break timing on this branch.",
  tone = None,
  source = None,
  refSans = Nil
)
```

Assert:

```scala
assertEquals(report.summary.centralBreakRowCount, 1)
assertEquals(report.summary.centralBreakNamedTokenRowCount, 1)
assertEquals(report.summary.centralBreakGenericFallbackCount, 0)
```

- [ ] **Step 5: Run diagnostics/audit tests**

Run:

```powershell
sbt "commentaryTools/testOnly lila.commentary.tools.moveReview.MoveReviewCoverageDiagnosticsTest lila.commentary.tools.quality.MoveReviewEvidenceCoverageAuditTest"
```

Expected: PASS.

---

### Task 5: Update Canonical Docs

**Files:**
- Modify `modules/commentary/docs/CommentaryPipelineSSOT.md`
- Modify `modules/commentary/docs/CommentaryTrustBoundary.md`
- Modify `modules/commentary/docs/CommentaryTruthBoundary.md`

- [ ] **Step 1: Update Pipeline SSoT**

In the MoveReview surface section around the current `neutralize_key_break` row text, add:

```markdown
`central_break_timing` may also add a `Central break` summary row when the
main-path packet is admitted as `SupportedLocal`, `CentralBreakTimingWitness.exact`
is present, the packet source/family is exactly `central_break_timing`, and
tactical veto reasons are empty. The row uses the witness `breakToken`; it does
not parse raw prose or expose proof ids.
```

- [ ] **Step 2: Update Trust Boundary**

Add:

```markdown
`central_break_timing`: product-visible support rows require the exact runtime
witness, including PV gap, branch key, board link, and source/family match.
Taxonomy, strategy-pack labels, raw claim prose, and signal-digest text do not
admit the row. Tactical truth veto remains higher priority.
```

- [ ] **Step 3: Update Truth Boundary**

Strengthen the existing central-break sentence:

```markdown
`central_break_timing` taxonomy remains non-authoritative by itself. A product
surface row is allowed only after the exact witness and SupportedLocal packet
both survive the runtime boundary; this does not make the family
`CertifiedOwner`.
```

- [ ] **Step 4: Run doc diff check**

Run:

```powershell
git diff --check -- modules/commentary/docs/CommentaryPipelineSSOT.md modules/commentary/docs/CommentaryTrustBoundary.md modules/commentary/docs/CommentaryTruthBoundary.md
```

Expected: no output.

---

### Task 6: Final Verification

**Files:** no edits.

- [ ] **Step 1: Run targeted suite**

Run:

```powershell
sbt "commentaryTools/testOnly lila.commentary.analysis.MoveReviewSupportedLocalSurfaceRowsTest lila.commentary.analysis.CentralBreakTimingPolicyTest lila.commentary.tools.moveReview.MoveReviewCoverageDiagnosticsTest lila.commentary.tools.quality.MoveReviewEvidenceCoverageAuditTest"
```

Expected: PASS.

- [ ] **Step 2: Run full commentaryTools suite**

Run:

```powershell
sbt "commentaryTools/test"
```

Expected: PASS.

- [ ] **Step 3: Run current MoveReview corpus**

Use the current manifest path from the active QC workspace. If continuing from the previous run layout:

```powershell
sbt "commentaryTools/test:runMain lila.commentary.tools.moveReview.MoveReviewCorpusRunner C:\Codes\CondensedChess\tmp\commentary-player-qc\manifests\slice_manifest.jsonl C:\Codes\CondensedChess\tmp\commentary-player-qc\runs\move_review_central_break_surface_20260523\move_review_outputs.jsonl C:\Codes\CondensedChess\tmp\commentary-player-qc\runs\move_review_central_break_surface_20260523\raw --depth 8 --multi-pv 3"
```

Expected: runner completes and writes outputs.

- [ ] **Step 4: Run evidence coverage audit**

```powershell
sbt "commentaryTools/test:runMain lila.commentary.tools.quality.MoveReviewEvidenceCoverageAudit C:\Codes\CondensedChess\tmp\commentary-player-qc\runs\move_review_central_break_surface_20260523\move_review_outputs.jsonl C:\Codes\CondensedChess\tmp\commentary-player-qc\runs\move_review_central_break_surface_20260523\move_review_evidence_coverage_audit.json C:\Codes\CondensedChess\tmp\commentary-player-qc\runs\move_review_central_break_surface_20260523\move_review_evidence_coverage_audit.md"
```

Expected acceptance:

- `centralBreakRowCount > 0` if current corpus still contains central admitted packets.
- `centralBreakNamedTokenRowCount == centralBreakRowCount`.
- `centralBreakGenericFallbackCount == 0`.
- tactical-failure central visible rows = `0`.
- `counterplayBreakGenericFallbackCount == 0` remains unchanged.

- [ ] **Step 5: Run whitespace check**

```powershell
git diff --check -- modules/commentaryCore/src/main/scala/lila/commentary/analysis/claim/ClaimAuthorityResolver.scala modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewPlayerPayloadBuilder.scala modules/commentaryTools/src/test/scala/lila/commentary/analysis/MoveReviewSupportedLocalSurfaceRowsTest.scala modules/commentaryTools/src/test/scala/lila/commentary/tools/moveReview/MoveReviewCoverageDiagnostics.scala modules/commentaryTools/src/test/scala/lila/commentary/tools/moveReview/MoveReviewCoverageDiagnosticsTest.scala modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewEvidenceCoverageAudit.scala modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewEvidenceCoverageAuditTest.scala modules/commentary/docs/CommentaryPipelineSSOT.md modules/commentary/docs/CommentaryTrustBoundary.md modules/commentary/docs/CommentaryTruthBoundary.md
```

Expected: no output.

---

## Acceptance Criteria

- `central_break_timing` exact packet produces exactly one `Central break` support row.
- Row text uses `CentralBreakTimingWitness.Witness.breakToken`.
- Row text does not contain `central_break_timing`, branch keys, raw proof metadata, `SupportedLocal`, or `local reading`.
- Plan-only, low PV-gap, missing witness, source/family mismatch, release risk, and tactical-veto scenes emit no `Central break` row.
- Existing `neutralize_key_break` `Counterplay break` behavior remains unchanged.
- Public JSON schema is unchanged.
- No new runtime module is added.
- Canonical docs are updated in the same change.
