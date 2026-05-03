package lila.commentary.selection.semantic

import chess.format.Fen

import lila.commentary.CommentaryCore
import lila.commentary.api.CommentaryPipelineInput
import lila.commentary.certification.EngineNodeIdentity
import lila.commentary.selection.*

class SemanticClaimSelectorShadowTest extends munit.FunSuite:

  private def assert(condition: => Boolean)(using loc: munit.Location): Unit =
    super.assert(condition, clues(""))

  private def assertEquals[A, B](obtained: A, expected: B)(using
      loc: munit.Location,
      compare: munit.Compare[A, B],
      diffOptions: munit.diff.DiffOptions
  ): Unit =
    super.assertEquals(obtained, expected, clues(""))

  private val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"

  test("shadow result uses fixed input, score, and topK shapes"):
    val claims = Vector(boardClaim("shape-board-claim"))
    val result = SemanticClaimSelector.shadow(input(), claims, ClaimSelector.select(claims))

    assertEquals(SemanticModelShape.InputSize, 8969)
    assertEquals(SemanticModelShape.ScoreVectorSize, 64)
    assertEquals(SemanticModelShape.TopK, 8)
    assertEquals(result.inputSize, 8969)
    assertEquals(result.scoreVectorSize, 64)
    assertEquals(result.topK, 8)
    assertEquals(result.candidates.map(_.inputVector.values.size), Vector(8969))
    assertEquals(result.decisions.map(_.scoreVector.values.size), Vector(64))
    assert(result.topDecisions.size <= 8)

  test("hard gates keep raw engine, source context, renderer, support-only, and missing evidence non-public"):
    val claims = Vector(
      boardClaim("eligible-board-claim"),
      CommentaryClaim(
        id = "raw-engine-only",
        layer = ClaimLayer.Engine,
        status = ClaimStatus.Admitted,
        impact = ClaimImpact(evalSwing = 900, evidenceConfidence = 90),
        evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-cp-plus-900"))
      ),
      CommentaryClaim(
        id = "source-context-only",
        layer = ClaimLayer.SourceContext,
        status = ClaimStatus.Context,
        evidenceRefs =
          Vector(EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:catalan:canonical")),
        sourceContextKind = Some(SourceContextKind.Opening),
        wordingStrengthCap = WordingStrength.ContextOnly
      ),
      CommentaryClaim(
        id = "renderer-blocked",
        layer = ClaimLayer.Renderer,
        status = ClaimStatus.Rejected,
        suppressionHints = Vector(SuppressionReason.RendererNotAllowed)
      ),
      boardClaim("support-only-board").copy(status = ClaimStatus.SupportOnly),
      boardClaim("missing-evidence").copy(evidenceRefs = Vector.empty)
    )

    val result = SemanticClaimSelector.shadow(input(), claims, ClaimSelector.select(claims))

    assertEquals(decision(result, "eligible-board-claim").publicEligible, true)
    assertSuppressed(result, "raw-engine-only", SemanticGate.NoRawEngine, SuppressionReason.RawEngineOnly)
    assertSuppressed(
      result,
      "source-context-only",
      SemanticGate.SourceSafe,
      SuppressionReason.SourceContextOnly
    )
    assertSuppressed(
      result,
      "renderer-blocked",
      SemanticGate.RenderContractSafe,
      SuppressionReason.RendererNotAllowed
    )
    assertSuppressed(result, "support-only-board", SemanticGate.StatusAdmitted, SuppressionReason.SupportOnly)
    assertSuppressed(
      result,
      "missing-evidence",
      SemanticGate.EvidencePresent,
      SuppressionReason.NoBoardReason
    )

  private def assertSuppressed(
      result: SemanticModelResult,
      claimId: String,
      gate: SemanticGate,
      reason: SuppressionReason
  )(using munit.Location): Unit =
    val suppressed = decision(result, claimId)
    assertEquals(suppressed.publicEligible, false)
    assert(suppressed.gateFailures.contains(gate), clues(suppressed))
    assert(suppressed.suppressionReasons.contains(reason), clues(suppressed))

  private def decision(result: SemanticModelResult, claimId: String): SemanticDecision =
    result.decisions
      .find(_.candidate.claimId == claimId)
      .getOrElse(fail(s"missing semantic decision for $claimId"))

  private def input(): CommentaryPipelineInput =
    val fullFen = Fen.Full.clean(fen): Fen.Full
    CommentaryPipelineInput(
      node = EngineNodeIdentity("mainline:0", 0),
      currentFen = fullFen,
      beforeFen = None,
      currentExtraction =
        CommentaryCore.extractStrategicObjectsFromFenFailClosed(fen).fold(fail(_), identity),
      deltaExtraction = None,
      engineIntake = None,
      engineCertificationEvidence = None,
      completedProbe = None
    )

  private def boardClaim(id: String): CommentaryClaim =
    CommentaryClaim(
      id = id,
      layer = ClaimLayer.Delta,
      status = ClaimStatus.Admitted,
      owner = Some("white"),
      beneficiary = Some("white"),
      defender = Some("black"),
      sideToMove = Some("white"),
      anchor = Some("board"),
      route = Some("semantic_safe_route"),
      scope = Some("position_local"),
      impact = ClaimImpact(
        resultMaterialImpact = 60,
        evidenceConfidence = 80,
        boardExplainability = 70,
        pedagogicalClarity = 75
      ),
      evidenceRefs = Vector(
        EvidenceRef(
          EvidenceRefKind.Delta,
          "semantic-safe-delta",
          Some("white"),
          Some("board"),
          Some("semantic_safe_route"),
          Some("position_local")
        )
      ),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )
