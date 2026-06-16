package lila.commentary.analysis

import lila.commentary.{ DirectionalTargetReadiness, MoveReviewSurfaceAuthority, StrategicIdeaGroup, StrategicIdeaKind, StrategicIdeaReadiness, StrategyDirectionalTarget, StrategyIdeaSignal, StrategyPack, StrategyPieceMoveRef, StrategyPieceRoute }
import lila.commentary.analysis.semantic.RelationObservationCatalog
import lila.commentary.analysis.semantic.StrategicObservationIds.{ EvidenceRef, EvidenceSourceId, ProofFamilyId, ProofSourceId }
import lila.commentary.model.{ ConfidenceLevel, DecisionRationale, FactScope, NarrativeContext, NarrativeRenderMode, PhaseContext, PieceActivityInfo, PreventedPlanInfo, PVDelta, TargetSquare, WeakComplexInfo }
import lila.commentary.model.authoring.AuthorQuestionKind
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }
import munit.FunSuite

final class MoveReviewSupportedLocalSurfaceRowsTest extends FunSuite:

  private val ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx
  private val neutralizeCtx =
    ctx.copy(
      fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
      playedMove = Some("e2e3"),
      playedSan = Some("e3")
    )
  private val collisionCtx =
    ctx.copy(
      fen = "2b1k3/8/8/8/8/8/8/4K3 b - - 0 1",
      playedMove = Some("c8g4"),
      playedSan = Some("Bg4")
    )
  private val sanOnlyCollisionCtx =
    collisionCtx.copy(playedMove = None)
  private val playedDestinationRouteCtx =
    ctx.copy(
      playedMove = Some("e7e5"),
      playedSan = Some("e5")
    )
  private val quietKnightCtx =
    ctx.copy(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3")
    )
  private val MadernaExactFen =
    "nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17"
  private val MadernaExactLines =
    List(
      VariationLine(List("e4e5", "d6e5", "f4e5", "d7e5", "c4e5"), scoreCp = 82, depth = 18),
      VariationLine(List("c1e3", "b7b5", "a5b6"), scoreCp = 36, depth = 18)
    )
  private val DirectBreakFen =
    "rnbqk2r/pp2bppp/4pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1 w kq - 0 8"
  private val NoRankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil)

  private def withFocalTarget(base: NarrativeContext, square: String): NarrativeContext =
    val decision =
      base.decision
        .getOrElse(DecisionRationale(None, "", PVDelta(Nil, Nil, Nil, Nil), ConfidenceLevel.Heuristic))
        .copy(focalPoint = Some(TargetSquare(square)))
    base.copy(decision = Some(decision))

  private def supportedClaimPacket(
      proofFamily: String,
      proofSource: String,
      triggerKind: String,
      anchorTerms: List[String],
      ownerSeedTerms: List[String],
      continuationTerms: List[String],
      structureTransitionTerms: List[String],
      bestDefenseMove: Option[String],
      bestDefenseBranchKey: Option[String],
      scope: PlayerFacingPacketScope = PlayerFacingPacketScope.MoveLocal,
      sameBranchState: PlayerFacingSameBranchState,
      persistence: PlayerFacingClaimPersistence,
      releaseRisks: List[String],
      suppressionReasons: List[String],
      fallbackMode: PlayerFacingClaimFallbackMode,
      exactSliceProof: Option[PlayerFacingExactSliceProof] = None,
      attributionGrade: PlayerFacingClaimAttributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
      ontologyFamily: PlayerFacingClaimOntologyKind = PlayerFacingClaimOntologyKind.Unknown
  ): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = attributionGrade,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = ontologyFamily
        ),
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = scope,
      triggerKind = triggerKind,
      anchorTerms = anchorTerms,
      bestDefenseMove = bestDefenseMove,
      bestDefenseBranchKey = bestDefenseBranchKey,
      sameBranchState = sameBranchState,
      persistence = persistence,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = ownerSeedTerms,
          continuationTerms = continuationTerms,
          structureTransitionTerms = structureTransitionTerms,
          exactSliceProof = exactSliceProof
        ),
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedNeutralizePacket(
      proofFamily: String = ProofFamilyId.NeutralizeKeyBreak.wireKey,
      proofSource: String = ProofSourceId.CounterplayAxisSuppression.wireKey,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      scope: PlayerFacingPacketScope = PlayerFacingPacketScope.MoveLocal,
      anchorTerms: List[String] = List("...c5"),
      ownerSeedTerms: List[String] = List("neutralize_key_break", "...c5"),
      structureTransitionTerms: List[String] = List("...c5"),
      bestDefenseMove: Option[String] = Some("c5"),
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...c5"))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = scope,
      triggerKind = PlanTaxonomy.PlanKind.BreakPrevention.id,
      anchorTerms = anchorTerms,
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = List("counterplay_axis_suppression", "e4d5", "c6d5"),
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = bestDefenseMove,
      bestDefenseBranchKey = Some("e4d5|c6d5"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedColorComplexPacket(
      proofFamily: String = ProofFamilyId.ColorComplexSqueeze.wireKey,
      proofSource: String = ProofSourceId.ColorComplexSqueezeProbe.wireKey,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.ColorComplexSqueeze("e5", "dark", "knight", "c4"))
  ): PlayerFacingClaimPacket =
    supportedPositionProbePacket(
      proofFamily = proofFamily,
      proofSource = proofSource,
      anchorSquare = "e5",
      ownerSeedTerms =
        List(
          "e5",
          "weak_square:e5",
          "color_complex:dark",
          "minor_piece:knight_c4",
          "attacks:e5",
          "minor_piece_attack:c4-e5",
          proofSource,
          proofFamily
        ),
      continuationTerms = List("color_complex_squeeze_probe", "weak_square:e5", "best_branch:c4e5|e8f8"),
      structureTransitionTerms = List("color_complex_squeeze_probe", "weak_square:e5", "minor_piece_attack:c4-e5"),
      exactSliceProof = exactSliceProof,
      sameBranchState = sameBranchState,
      persistence = persistence,
      releaseRisks = releaseRisks,
      suppressionReasons = suppressionReasons,
      fallbackMode = fallbackMode
    )

  private def supportedPositionProbePacket(
      proofFamily: String,
      proofSource: String,
      anchorSquare: String,
      ownerSeedTerms: List[String],
      continuationTerms: List[String],
      structureTransitionTerms: List[String],
      exactSliceProof: Option[PlayerFacingExactSliceProof],
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = PlayerFacingPacketScope.PositionLocal,
      triggerKind = "position_probe",
      anchorTerms = List(anchorSquare),
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = continuationTerms,
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("e8f8"),
      bestDefenseBranchKey = Some("c4e5|e8f8"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode,
      attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
      ontologyFamily = PlayerFacingClaimOntologyKind.ColorComplexSqueeze
    )

  private def supportedLocalFilePacket(
      proofFamily: String = ProofFamilyId.HalfOpenFilePressure.wireKey,
      proofSource: String = ProofSourceId.LocalFileEntryBind.wireKey,
      anchorSquare: String = "c6",
      continuationTerms: List[String] = List("local_file_entry_bind", "c-file", "c6", "d4c6", "c7c6"),
      structureTransitionTerms: List[String] = List("file-entry:c-file:c6"),
      bestDefenseMove: Option[String] = Some("c7c6"),
      bestDefenseBranchKey: Option[String] = Some("d4c6|c7c6"),
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "c6"))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = "file_entry_denial",
      anchorTerms = List("c-file", anchorSquare),
      ownerSeedTerms = List("c-file", anchorSquare),
      continuationTerms = continuationTerms,
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = bestDefenseMove,
      bestDefenseBranchKey = bestDefenseBranchKey,
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedOutpostPacket(
      proofFamily: String = PlayerFacingTruthModePolicy.OutpostEntrenchmentProofFamily,
      proofSource: String = PlayerFacingTruthModePolicy.OutpostEntrenchmentProofSource,
      square: String = "e5",
      ownerSeedTerms: List[String] = List("e5", "outpost:e5", "piece:knight", "outpost_occupation:knight:e5"),
      structureTransitionTerms: List[String] =
        List("outpost_occupation", "outpost:e5", "piece:knight", "outpost_occupation:knight:e5"),
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.OutpostOccupation("knight", "e5"))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = PlanTaxonomy.PlanKind.OutpostEntrenchment.id,
      anchorTerms = List(square),
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = List("outpost:e5", "best_branch:g4e5|e8f8", "e8f8"),
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("e8f8"),
      bestDefenseBranchKey = Some("g4e5|e8f8"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode,
      attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
      ontologyFamily = PlayerFacingClaimOntologyKind.Access
    )

  private def supportedIqpPacket(
      proofFamily: String = PlanTaxonomy.PlanKind.IQPInducement.id,
      proofSource: String = ProofSourceId.IQPInducementProbe.wireKey,
      targetSquare: String = "d5",
      structureTransitionTerms: List[String] =
        List("before_not_isolated:d5", "after_isolated:d5", "target_pressure:d5", "central_isolated_pawn"),
      ownerSeedTerms: List[String] = List("d5", "isolated_pawn:d5", "target_pressure:d5", "iqp_inducement"),
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Missing,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.IqpInducement("d5", List("c4d5", "e6d5")))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = PlanTaxonomy.PlanKind.IQPInducement.id,
      anchorTerms = List(targetSquare),
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = List("iqp_inducement_branch", "c4d5", "e6d5"),
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("e6d5"),
      bestDefenseBranchKey = Some("c4d5|e6d5"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedSimplificationPacket(
      proofFamily: String = PlanTaxonomy.PlanKind.SimplificationWindow.id,
      proofSource: String = PlanTaxonomy.PlanKind.SimplificationWindow.id,
      exchangeSquare: String = "d5",
      continuationTerms: List[String] = List("exact_trade_continuation", "exchange_square:d5", "b5d5", "f7d5"),
      structureTransitionTerms: List[String] = List("target_complex:d5", "simplification_window"),
      ownerSeedTerms: List[String] = List("simplification_window", "d5", "local_edge"),
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.SimplificationWindow("d5"))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = PlanTaxonomy.PlanKind.SimplificationWindow.id,
      anchorTerms = List(exchangeSquare),
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = continuationTerms,
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("f7d5"),
      bestDefenseBranchKey = Some("b5d5|f7d5"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedCounterplayRestraintPacket(
      proofFamily: String = ProofFamilyId.CounterplayRestraint.wireKey,
      proofSource: String = ProofSourceId.ProphylacticMove.wireKey,
      resourceToken: String = "denied_resource:break",
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.ProphylacticRestraint("denied_resource:break"))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = PlanTaxonomy.PlanKind.ProphylaxisRestraint.id,
      anchorTerms = List(resourceToken),
      ownerSeedTerms = List(resourceToken, "prophylactic_move", "counterplay_restraint"),
      continuationTerms = List("counterplay_restraint", "f8e8", "c1c8"),
      structureTransitionTerms = List(resourceToken),
      bestDefenseMove = Some("f8e8"),
      bestDefenseBranchKey = Some("f8e8|c1c8"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode,
      attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
      ontologyFamily = PlayerFacingClaimOntologyKind.CounterplayRestraint
    )

  private def supportedExchangePacket(
      planKind: PlanTaxonomy.PlanKind,
      proofSource: String,
      structureTransitionTerms: List[String],
      exactSliceProof: Option[PlayerFacingExactSliceProof] = None,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain
  ): PlayerFacingClaimPacket =
    val proofFamily = ProofFamilyId.fromPlanKind(planKind).map(_.wireKey).getOrElse(planKind.id)
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = planKind.id,
      anchorTerms = List(planKind.id),
      ownerSeedTerms = List(planKind.id, "local_branch"),
      continuationTerms = List(planKind.id, "d4c6", "d7c6"),
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("d4c6"),
      bestDefenseBranchKey = Some("d4c6|d7c6"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def admittedExchangeOwnershipCases: List[(PlayerFacingClaimPacket, String, String)] =
    List(
      (
        supportedExchangePacket(
          planKind = PlanTaxonomy.PlanKind.DefenderTrade,
          proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
          structureTransitionTerms =
            List(
              "defender_trade_branch",
              "defender:c5",
              "exchange_square:d4",
              "defended_target:e5",
              "defender_removed:c5-d4",
              "target_unlocked:e5"
            ),
          exactSliceProof = Some(PlayerFacingExactSliceProof.DefenderTrade("c5", "d4", "e5"))
        ),
        "Defender trade",
        "The checked line trades on d4 to remove the defender from c5, loosening e5."
      ),
      (
        supportedExchangePacket(
          planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
          proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
          structureTransitionTerms =
            List(
              "bad_piece_liquidation_branch",
              "bad_piece:c8",
              "exchange_square:e6",
              "bad_piece_removed:c8-e6"
            ),
          exactSliceProof = Some(PlayerFacingExactSliceProof.BadPieceLiquidation("c8", "e6"))
        ),
        "Bad piece trade",
        "The checked line trades on e6 to clear the bad piece from c8."
      ),
      (
        supportedExchangePacket(
          planKind = PlanTaxonomy.PlanKind.QueenTradeShield,
          proofSource = PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
          structureTransitionTerms = List("d4c6", "d7c6", "d3d8", "e8d8", "queenless_branch", "queen_trade"),
          exactSliceProof =
            Some(
              PlayerFacingExactSliceProof.QueenTradeShield(List("d4c6", "d7c6", "d3d8", "e8d8"))
            )
        ),
        "Queen trade",
        "This exchange moves the game into the queenless branch."
      )
    )

  private def expectedExchangeOwnershipAuthority(label: String): MoveReviewSurfaceAuthority =
    label match
      case "Defender trade" =>
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some(MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade),
          target = Some("e5")
        )
      case "Bad piece trade" =>
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some(MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation),
          target = Some("e6")
        )
      case "Fixed target" =>
        MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d6"))
      case "Minority attack" =>
        MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6"))
      case "File entry" =>
        MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6"))
      case "IQP target" =>
        MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5"))
      case "Simplification" =>
        MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5"))
      case "Knight outpost" =>
        MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5"))
      case "Target coordination" =>
        MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5"))
      case "Color complex" =>
        MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5"))
      case _ =>
        MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)

  private def mainClaim(
      packet: PlayerFacingClaimPacket,
      claimText: String
  ): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.MoveLocal,
      mode = PlayerFacingTruthMode.Strategic,
      deltaClass = Some(PlayerFacingMoveDeltaClass.CounterplayReduction),
      claimText = claimText,
      anchorTerms = List("...c5"),
      evidenceLines = List("14.e4 c5 15.d5"),
      sourceKind = "prevented_plan",
      tacticalOwnership = None,
      packet = Some(packet)
    )

  private def inputs(
      packet: PlayerFacingClaimPacket = supportedNeutralizePacket(),
      truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic,
      claimText: String = "This move stops the ...c5 break before Black gets counterplay."
  ): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle = Some(MainPathClaimBundle(Some(mainClaim(packet, claimText)), None)),
      quietIntent = None,
      decisionFrame = CertifiedDecisionFrame(),
      decisionComparison = None,
      alternativeNarrative = None,
      truthMode = truthMode,
      preventedPlansNow = Nil,
      pvDelta = None,
      counterfactual = None,
      practicalAssessment = None,
      opponentThreats = Nil,
      forcingThreats = Nil,
      evidenceByQuestionId = Map.empty,
      candidateEvidenceLines = Nil,
      evidenceBackedPlans = Nil,
      opponentPlan = None,
      factualFallback = None
    )

  private def supportedQuietIntent(
      intentClass: QuietMoveIntentClass = QuietMoveIntentClass.PieceImprovement,
      anchorSquare: String = "f3",
      sourceKind: String = "piece_improvement",
      claimText: String = "This improves the knight by placing it on f3.",
      proofFamilyOverride: Option[String] = None,
      ontologyOverride: Option[PlayerFacingClaimOntologyKind] = None
  ): QuietMoveIntentClaim =
    val proofFamily = proofFamilyOverride.getOrElse(intentClass.proofFamily)
    val ontology = ontologyOverride.getOrElse(intentClass.ontologyFamily)
    val packet =
      PlayerFacingClaimPacket(
        claimGate =
          PlanEvidenceEvaluator.ClaimCertification(
            certificateStatus = PlayerFacingCertificateStatus.Valid,
            quantifier = PlayerFacingClaimQuantifier.Universal,
            modalityTier = PlayerFacingClaimModalityTier.Supports,
            attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
            stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
            provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
            ontologyFamily = ontology
          ),
        proofSource = sourceKind,
        proofFamily = proofFamily,
        scope = PlayerFacingPacketScope.MoveLocal,
        triggerKind = sourceKind,
        anchorTerms = List(anchorSquare),
        bestDefenseMove = Some("g8f6"),
        bestDefenseBranchKey = Some("g1f3|g8f6"),
        sameBranchState = PlayerFacingSameBranchState.Proven,
        persistence = PlayerFacingClaimPersistence.Stable,
        proofPathWitness =
          PlayerFacingProofPathWitness(
            ownerSeedTerms = List(anchorSquare),
            continuationTerms = List("probe_backed", "stable", "support_probe:quiet_intent"),
            structureTransitionTerms = Nil
          ),
        fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
      )
    QuietMoveIntentClaim(
      intentClass = intentClass,
      claimText = claimText,
      evidenceLine = Some(s"Line: $anchorSquare is anchored by probe-backed quiet intent."),
      sourceKind = sourceKind,
      packet = packet
    )

  private def routeNetworkInputs(
      surface: Option[RouteNetworkBindProof.SurfaceNetwork],
      truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic,
      heavyPieceLocalBindBlocked: Boolean = false
  ): QuestionPlannerInputs =
    inputs().copy(
      mainBundle = None,
      truthMode = truthMode,
      heavyPieceLocalBindBlocked = heavyPieceLocalBindBlocked,
      namedRouteNetworkSurface = surface
    )

  private def dualAxisInputs(
      contract: Option[TwoAxisBindProof.Contract],
      truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic,
      heavyPieceLocalBindBlocked: Boolean = false
  ): QuestionPlannerInputs =
    inputs().copy(
      mainBundle = None,
      truthMode = truthMode,
      heavyPieceLocalBindBlocked = heavyPieceLocalBindBlocked,
      dualAxisBindSurface = contract
    )

  private def restrictedDefenseInputs(
      contract: Option[RestrictedDefenseConversionProof.Contract],
      truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic
  ): QuestionPlannerInputs =
    inputs().copy(
      mainBundle = None,
      truthMode = truthMode,
      restrictedDefenseConversionSurface = contract
    )

  private def certifiedRestrictedDefenseConversion(
      bestDefenseFound: Option[String] = Some("e8e7"),
      bestDefenseBranchKey: Option[String] = Some("e8e7 e1e2"),
      evidence: RestrictedDefenseConversionProof.RestrictedDefenseEvidence =
        RestrictedDefenseConversionProof.RestrictedDefenseEvidence(
          defenderResourceCount = 2,
          moveQualityCompression = true,
          counterplayScoreDrop = 125,
          preventedResourcePressure = true,
          bestReplyStable = true,
          futureSnapshotPersistence = true
        ),
      routePersistence: RestrictedDefenseConversionProof.RoutePersistence =
        RestrictedDefenseConversionProof.RoutePersistence(
          bestDefenseStable = true,
          futureSnapshotPersistent = true,
          counterplayStillCompressed = true,
          directBestDefensePresent = true,
          sameDefendedBranch = true
        ),
      failsIf: List[String] = Nil,
      moveOrderFragility: RestrictedDefenseConversionProof.MoveOrderFragility =
        RestrictedDefenseConversionProof.MoveOrderFragility(fragile = false, reasons = Nil)
  ): RestrictedDefenseConversionProof.Contract =
    RestrictedDefenseConversionProof.Contract(
      strategyHypothesis = "Convert with restricted defense",
      restrictedDefenseEvidence = evidence,
      defenderResources = List("e8e7", "e8f8"),
      bestDefenseFound = bestDefenseFound,
      bestDefenseBranchKey = bestDefenseBranchKey,
      routePersistence = routePersistence,
      failsIf = failsIf,
      moveOrderFragility = moveOrderFragility,
      confidence = if failsIf.isEmpty then 0.9 else 0.36,
      evidenceSources = List("convert_reply_multipv", "counterplay_drop:125")
    )

  private def certifiedDualAxisBind(
      breakLabel: String = "...c5",
      entryLabel: String = "b4",
      failsIf: List[String] = Nil,
      counterplayReinflationRisk: String = "bounded_dual_axis_only"
  ): TwoAxisBindProof.Contract =
    val breakAxis =
      TwoAxisBindProof.AxisDescriptor(
        kind = "break_axis",
        label = breakLabel,
        deniedResourceClass = Some("break"),
        counterplayScoreDrop = 145,
        breakNeutralizationStrength = Some(84),
        defensiveSufficiency = Some(80)
      )
    val entryAxis =
      TwoAxisBindProof.AxisDescriptor(
        kind = "entry_axis",
        label = entryLabel,
        deniedResourceClass = Some("entry_square"),
        counterplayScoreDrop = 130,
        breakNeutralizationStrength = Some(76),
        defensiveSufficiency = Some(74)
      )
    TwoAxisBindProof.Contract(
      strategyHypothesis = "Stop the break and keep the entry square closed",
      claimScope = "dual_axis_local",
      primaryAxis = Some(breakAxis),
      corroboratingAxes = List(entryAxis),
      axisIndependence =
        TwoAxisBindProof.AxisIndependence(
          primaryAxis = Some(breakLabel),
          corroboratingAxis = Some(entryLabel),
          proven = failsIf.isEmpty,
          reasons = Nil
        ),
      bindArchetype = "break_plus_entry",
      restrictionEvidence =
        TwoAxisBindProof.RestrictionEvidence(
          primaryAxis = Some(breakAxis),
          corroboratingAxes = List(entryAxis),
          primaryAxisMeasured = true,
          corroboratingAxisMeasured = true,
          restrictionDeltaMeasured = true,
          bestDefenseStable = true,
          futureSnapshotPersistence = true
        ),
      defenderResources = List("counterplay"),
      freeingResourcesRemaining = Nil,
      tacticalReleasesRemaining = Nil,
      bestDefenseFound = Some("f8e8"),
      bestDefenseBranchKey = Some("f8e8|c1c8"),
      persistenceAfterBestDefense = true,
      routeContinuity =
        TwoAxisBindProof.RouteContinuity(
          directBestDefensePresent = true,
          bestDefenseStable = true,
          futureSnapshotPersistent = true,
          convertReplyAligned = true,
          boundedContinuationVisible = true,
          sameDefendedBranch = true
        ),
      fortressRisk = false,
      moveOrderFragility = TwoAxisBindProof.MoveOrderFragility(fragile = false, reasons = Nil),
      counterplayReinflationRisk = counterplayReinflationRisk,
      claimCertification =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          modalityTier = PlayerFacingClaimModalityTier.Advances,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyKind.RouteDenial
        ),
      failsIf = failsIf,
      confidence = if failsIf.isEmpty then 0.91 else 0.42,
      evidenceSources = List("route_denial_validation", "defense_reply_multipv")
    )

  private def safeTruthContract(playedMove: String = "c3g3"): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some(playedMove),
      verifiedBestMove = Some(playedMove),
      truthClass = DecisiveTruthClass.Best,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.SupportingVisible,
      surfaceMode = TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  private def topPvPracticalContext(
      fen: String,
      playedMove: String,
      playedSan: String,
      line: List[String],
      scoreCp: Int,
      mate: Option[Int] = None
  ) =
    ctx.copy(
      fen = fen,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      openingGoalEvaluation = None,
      engineEvidence =
        Some(
          EngineEvidence(
            depth = 18,
            variations = List(VariationLine(line, scoreCp = scoreCp, mate = mate, depth = 18))
          )
        )
    )

  private def noRankedSupportedLocalRows(
      localCtx: NarrativeContext = ctx,
      localInputs: QuestionPlannerInputs,
      playedMove: String = "c3g3"
  ) =
    MoveReviewSupportedLocalSurfaceRows.build(
      ctx = localCtx,
      inputs = localInputs,
      rankedPlans = NoRankedPlans,
      truthContract = Some(safeTruthContract(playedMove))
    )

  private def topPvPracticalRows(localCtx: NarrativeContext, playedMove: String) =
    noRankedSupportedLocalRows(
      localCtx = localCtx,
      localInputs = inputs().copy(mainBundle = None),
      playedMove = playedMove
    )

  private def neutralizePlan(
      claim: String = "This move stops the ...c5 break before Black gets counterplay.",
      proofFamily: String = ProofFamilyId.NeutralizeKeyBreak.wireKey,
      plannerSource: String = "prevented_plan",
      namedBreak: Option[String] = Some("...c5")
  ): QuestionPlan =
    QuestionPlan(
      questionId = "q_now_counterplay_break",
      questionKind = AuthorQuestionKind.WhyNow,
      priority = 100,
      claim = claim,
      evidence = None,
      contrast = None,
      consequence = None,
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = List(plannerSource),
      admissibilityReasons = Nil,
      plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
      plannerSource = plannerSource,
      timingWitness =
        Some(
          QuestionPlanTimingWitness(
            proofFamily = proofFamily,
            source = plannerSource,
            namedBreak = namedBreak,
            continuationMove = Some("e4d5"),
            branchKey = Some("e4d5|c6d5"),
            witnessTokens = List("counterplay_axis_suppression")
          )
        )
    )

  private def ranked(plan: QuestionPlan): RankedQuestionPlans =
    RankedQuestionPlans(primary = Some(plan), secondary = None, rejected = Nil)

  private def centralScene(
      fen: String = MadernaExactFen,
      ply: Int = 33,
      playedMove: String = "e4e5",
      lines: List[VariationLine] = MadernaExactLines,
      phase: String = "middlegame",
      truthContract: Option[DecisiveTruthContract] = None
  ): (NarrativeContext, QuestionPlannerInputs, RankedQuestionPlans) =
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fen,
          variations = lines,
          playedMove = Some(playedMove),
          phase = Some(phase),
          ply = ply,
          prevMove = Some(playedMove)
        )
        .getOrElse(fail(s"analysis missing for $fen"))
    val centralCtx = NarrativeContextBuilder.build(data, data.toContext, None)
    val pack = StrategyPackBuilder.build(data, centralCtx).getOrElse(fail(s"strategy pack missing for $fen"))
    val effectiveTruthContract = truthContract.orElse(Some(safeTruthContract(playedMove)))
    val centralInputs = QuestionPlannerInputsBuilder.build(centralCtx, Some(pack), effectiveTruthContract)
    val centralRanked = QuestionFirstCommentaryPlanner.plan(centralCtx, centralInputs, effectiveTruthContract)
    (centralCtx, centralInputs, centralRanked)

  test("projects a SupportedLocal neutralize_key_break timing plan into a named counterplay break row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = inputs(),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "This stops the ...c5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5")))
    )
    assertEquals(rows.head.source, None)
  }

  test("projects exact central_break_timing packet into a central break row") {
    val (centralCtx, centralInputs, centralRanked) = centralScene()
    val packet =
      centralInputs.mainBundle.flatMap(_.primaryClaim.flatMap(_.packet)).getOrElse(fail("central break packet missing"))
    val exactProof = PlayerFacingExactSliceProof.CentralBreakTiming("e4e5", "e5", "e4-e5")

    assertEquals(centralCtx.fen, MadernaExactFen)
    assertEquals(centralCtx.playedMove, Some("e4e5"))
    assertEquals(packet.proofSource, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assert(packet.proofPathWitness.ownerSeedTerms.contains("e4-e5"), clue(packet))

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the e4-e5 break at this moment."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralBreak, token = Some("e4-e5")))
    )
    assertEquals(rows.head.source, None)
    assert(!rows.exists(_.text.contains("central_break_timing")), clue(rows))
  }

  test("projects certified color-complex squeeze packet into a board-backed row") {
    val moveOwnedColorCtx =
      ctx.copy(
        fen = "4k3/8/8/4p3/8/8/1N6/4K3 w - - 0 1",
        playedMove = Some("b2c4"),
        playedSan = Some("Nc4"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 12,
              variations = List(VariationLine(List("b2c4", "e8e7", "e1e2", "e7e6"), scoreCp = 20, depth = 12))
            )
          )
      )
    val rows =
      noRankedSupportedLocalRows(
        localCtx = moveOwnedColorCtx,
        localInputs = inputs(
          packet = supportedColorComplexPacket(),
          claimText = "A minor piece keeps the color-complex pressure on e5."
        ),
        playedMove = "b2c4"
      )

    assertEquals(rows.map(_.label), List("Color complex"))
    assertEquals(
      rows.head.text,
      "The checked line keeps the knight on c4 attacking e5 in the dark-square complex."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
    )
    assertEquals(rows.head.source, None)
  }

  test("does not project color-complex squeeze when the checked PV removes the proof minor") {
    val baseCtx =
      topPvPracticalContext(
        fen = "2r1k2r/1p1bbpp1/pqn1p2p/2p1Pn2/3p1BP1/2PP1N1P/PPQNBP2/R3K2R b KQk - 0 14",
        playedMove = "f5h4",
        playedSan = "Nh4",
        line = List("f5h4", "g2h4", "e7h4", "d2c4", "b6c7"),
        scoreCp = 20
      )
    val semantic = baseCtx.semantic.getOrElse(fail("expected base semantic"))
    val colorComplexCtx =
      baseCtx.copy(
        semantic =
          Some(
            semantic.copy(
              structuralWeaknesses =
                List(
                  WeakComplexInfo(
                    owner = "White",
                    squareColor = "light",
                    squares = List("f3"),
                    isOutpost = false,
                    cause = "color complex hole"
                  )
                ),
              pieceActivity =
                List(
                  PieceActivityInfo(
                    piece = "knight",
                    square = "h4",
                    mobilityScore = 0.72,
                    isTrapped = false,
                    isBadBishop = false,
                    keyRoutes = List("f3"),
                    coordinationLinks = List("f3"),
                    directionalTargets = List("f3")
                  )
                )
            )
          )
      )
    val exactProof = PlayerFacingExactSliceProof.ColorComplexSqueeze("f3", "light", "knight", "h4")
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(colorComplexCtx, StrategyPackSurface.from(None), Some(safeTruthContract("f5h4")))
    assert(delta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource), clue(delta))
    val stalePacket =
      supportedPositionProbePacket(
        proofFamily = PlayerFacingTruthModePolicy.ColorComplexSqueezeProofFamily,
        proofSource = PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource,
        anchorSquare = "f3",
        ownerSeedTerms =
          List(
            "f3",
            "weak_square:f3",
            "color_complex:light",
            "minor_piece:knight_h4",
            "attacks:f3",
            "minor_piece_attack:h4-f3",
            PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource,
            PlayerFacingTruthModePolicy.ColorComplexSqueezeProofFamily
          ),
        continuationTerms = List("color_complex_squeeze_probe", "weak_square:f3", "best_branch:f5h4|g2h4"),
        structureTransitionTerms = List("color_complex_squeeze_probe", "weak_square:f3", "minor_piece_attack:h4-f3"),
        exactSliceProof = Some(exactProof)
      )
    val rows =
      noRankedSupportedLocalRows(
        localCtx = colorComplexCtx,
        localInputs = inputs(packet = stalePacket, claimText = "A minor piece keeps the color-complex pressure on f3."),
        playedMove = "f5h4"
      )

    assert(!rows.exists(_.label == "Knight outpost"), clue(rows))
  }

  test("projects PGN-backed color-complex squeeze exact-slice packets through the public row") {
    val cases =
      List(
        (
          "source-botvinnik-vidmar-1936-flank-clamp",
          "r2q1rk1/pp1bbppp/4pn2/3n2B1/3P4/1BNQ1N2/PP3PPP/R4RK1 w - - 5 13",
          "f3e5",
          "Ne5",
          List("f3e5", "a8c8", "f1e1", "h7h6", "g5d2", "d7c6", "d3h3", "c8c7", "a1d1"),
          "Black",
          "e5",
          "dark",
          "knight",
          "f3",
          Some("Rc8"),
          Option.empty[String]
        ),
        (
          "source-botvinnik-vidmar-1936-e4-color-complex-squeeze",
          "r2q1rk1/pp2bppp/2b1pn2/4N1B1/1n1P4/1BN4Q/PP3PPP/3R1RK1 b - - 10 15",
          "c6d5",
          "Bxd5",
          List(
            "c6d5",
            "c3d5",
            "f6d5",
            "g5e7",
            "d8e7",
            "f2f4",
            "f7f6",
            "e5d3",
            "f6f5",
            "f1e1",
            "b4d3",
            "h3d3",
            "a8d8",
            "e1e5",
            "g8h8",
            "d3f3",
            "d8d6",
            "b3d5",
            "d6d5",
            "e5d5",
            "e6d5",
            "f3d5"
          ),
          "White",
          "e4",
          "light",
          "bishop",
          "d5",
          Some("Nxd5"),
          None
        ),
        (
          "source-alexjota-granddistroyer7-2026-nf1-nonowned-color-complex-squeeze",
          "2r2rk1/pp3ppp/3qpn2/n2p1b2/3P4/2P2N1P/PP1NBPP1/R2QR1K1 w - - 6 13",
          "d2f1",
          "Nf1",
          List("d2f1", "d6b6", "d1c1", "a5c6", "c1d2"),
          "Black",
          "e5",
          "dark",
          "knight",
          "f3",
          Some("Qb6"),
          None
        ),
        (
          "source-camara-bazan-1960-d5-color-complex-squeeze",
          "1rbqr1k1/pp1n1pbp/3p2p1/2pP4/1n2PP2/2NB3P/PP2N1P1/R1BQ1R1K w - - 3 14",
          "d3b5",
          "Bb5",
          List(
            "d3b5",
            "b4a6",
            "e4e5",
            "d6e5",
            "f4f5",
            "a6c7",
            "a2a4",
            "c7b5",
            "a4b5",
            "e5e4",
            "c1f4",
            "d7e5",
            "c3e4",
            "c8f5",
            "e2g3",
            "d8d7",
            "g3f5",
            "g6f5",
            "e4g3",
            "b8d8",
            "d5d6",
            "d7d6",
            "d1d6",
            "d8d6"
          ),
          "Black",
          "d5",
          "light",
          "knight",
          "c3",
          Some("Na6"),
          None
        ),
        (
          "source-pfleger-maalouf-1961-d5-color-complex-squeeze",
          "r2qr1k1/1p3pb1/pn1p1npp/2pP4/P3P3/2NQ1N2/1P1B1PPP/R3R1K1 w - - 0 17",
          "a4a5",
          "a5",
          List(
            "a4a5",
            "b6d7",
            "c3a4",
            "f6g4",
            "d2c3",
            "g4e5",
            "f3e5",
            "g7e5",
            "c3e5",
            "d7e5",
            "d3g3",
            "d8g5",
            "a4b6",
            "g5g3",
            "h2g3",
            "a8d8",
            "f2f4",
            "e5d3",
            "e1e3",
            "d3b4",
            "a1d1",
            "b4c2"
          ),
          "Black",
          "d5",
          "light",
          "knight",
          "c3",
          Some("Nbd7"),
          None
        )
      )

    cases.foreach {
      case (
            sourceId,
            fen,
            playedMove,
            playedSan,
            line,
            weaknessOwner,
            target,
            squareColor,
            minorPieceRole,
            minorPieceSquare,
            expectedBestDefense,
            expectedRowText
          ) =>
        val baseCtx =
          topPvPracticalContext(
            fen = fen,
            playedMove = playedMove,
            playedSan = playedSan,
            line = line,
            scoreCp = 20
          )
        val semantic = baseCtx.semantic.getOrElse(fail(s"expected base semantic for $sourceId"))
        val colorComplexCtx =
          baseCtx.copy(
            semantic =
              Some(
                semantic.copy(
                  structuralWeaknesses =
                    List(
                      WeakComplexInfo(
                        owner = weaknessOwner,
                        squareColor = squareColor,
                        squares = List(target),
                        isOutpost = false,
                        cause = "color complex hole"
                      )
                    ),
                  pieceActivity =
                    List(
                      PieceActivityInfo(
                        piece = minorPieceRole,
                        square = minorPieceSquare,
                        mobilityScore = 0.72,
                        isTrapped = false,
                        isBadBishop = false,
                        keyRoutes = List(target),
                        coordinationLinks = List(target),
                        directionalTargets = List(target)
                      )
                    )
                )
              )
          )
        val deltaOpt =
          PlayerFacingTruthModePolicy
            .mainPathMoveDeltaEvidence(colorComplexCtx, StrategyPackSurface.from(None), Some(safeTruthContract(playedMove)))
        val exactProof =
          PlayerFacingExactSliceProof.ColorComplexSqueeze(
            targetSquare = target,
            squareColor = squareColor,
            minorPieceRole = minorPieceRole,
            minorPieceSquare = minorPieceSquare
          )

        assertEquals(colorComplexCtx.fen, fen, clue(sourceId))
        assertEquals(colorComplexCtx.playedMove, Some(playedMove), clue(sourceId))
        expectedRowText match
          case Some(text) =>
            val delta = deltaOpt.getOrElse(fail(s"expected color-complex exact-slice move-delta packet for $sourceId"))
            val packet = delta.packet
            val rows =
              noRankedSupportedLocalRows(
                localCtx = colorComplexCtx,
                localInputs =
                  inputs(
                    packet = packet,
                    claimText = s"A minor piece keeps the color-complex pressure on $target."
                  ),
                playedMove = playedMove
              )
            assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.CounterplayReduction, clue(sourceId))
            assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource, clue(sourceId))
            assertEquals(packet.proofFamily, PlayerFacingTruthModePolicy.ColorComplexSqueezeProofFamily, clue(sourceId))
            assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof), clue(sourceId))
            assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(sourceId -> packet))
            assertEquals(packet.bestDefenseMove, expectedBestDefense, clue(sourceId -> packet))
            assert(packet.bestDefenseBranchKey.nonEmpty, clue(sourceId -> packet))
            assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven, clue(sourceId))
            assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable, clue(sourceId))
            assertEquals(rows.map(_.label), List("Color complex"), clue(sourceId -> rows))
            assertEquals(rows.head.text, text, clue(sourceId -> rows))
            assertEquals(
              rows.head.authority,
              Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some(target))),
              clue(sourceId -> rows)
            )
          case None =>
            deltaOpt.foreach { delta =>
              val rows =
                noRankedSupportedLocalRows(
                  localCtx = colorComplexCtx,
                  localInputs =
                    inputs(
                      packet = delta.packet,
                      claimText = s"A minor piece keeps the color-complex pressure on $target."
                    ),
                  playedMove = playedMove
                )
              assert(!rows.exists(_.label == "Color complex"), clue(sourceId -> rows))
              assert(!rows.exists(_.text.contains(s"$minorPieceRole on $minorPieceSquare")), clue(sourceId -> rows))
            }
    }
  }

  test("keeps two independent exact supported-local rows") {
    val rows =
      noRankedSupportedLocalRows(
        localCtx =
          neutralizeCtx.copy(
            fen = "4k3/8/8/4p3/8/8/1N2P3/4K3 w - - 0 1",
            playedMove = Some("b2c4"),
            playedSan = Some("Nc4"),
            engineEvidence =
              Some(
                EngineEvidence(
                  depth = 12,
                  variations = List(VariationLine(List("b2c4", "e8e7", "e2e3", "e7e6"), scoreCp = 20, depth = 12))
                )
              )
          ),
        localInputs =
          inputs().copy(
            mainBundle =
              Some(
                MainPathClaimBundle(
                  Some(mainClaim(supportedNeutralizePacket(), "This move stops the ...c5 break before Black gets counterplay.")),
                  Some(mainClaim(supportedColorComplexPacket(), "A minor piece keeps the color-complex pressure on e5."))
                )
              )
          ),
        playedMove = "b2c4"
      )

    assertEquals(rows.map(_.label), List("Counterplay break", "Color complex"))
    assertEquals(rows.head.text, "This stops the ...c5 break before it appears.")
    assertEquals(
      rows(1).text,
      "The checked line keeps the knight on c4 attacking e5 in the dark-square complex."
    )
  }

  test("keeps certified typed surface row visible when packet rows fill the supported-local cap") {
    val surface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "b6",
        counterplayScoreDrop = 140
      )
    val rows =
      noRankedSupportedLocalRows(
        localCtx = neutralizeCtx,
        localInputs =
          inputs().copy(
            mainBundle =
              Some(
                MainPathClaimBundle(
                  Some(mainClaim(supportedNeutralizePacket(), "This move stops the ...c5 break before Black gets counterplay.")),
                  Some(mainClaim(supportedColorComplexPacket(), "A minor piece keeps the color-complex pressure on e5."))
                )
              ),
            namedRouteNetworkSurface = Some(surface)
          ),
        playedMove = "e2e3"
      )

    assertEquals(rows.map(_.label), List("Counterplay break", "Route denial"))
    assertEquals(rows(1).text, "The checked line keeps c6 closed, takes the c-file away, and cuts off the b6 reroute.")
  }

  test("projects certified exact position-probe packets into bounded rows") {
    val staticWeaknessFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id
      )
    val cases =
      List(
        (
          supportedPositionProbePacket(
            proofFamily = staticWeaknessFamily,
            proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
            anchorSquare = "d6",
            ownerSeedTerms = List("d6", "fixed_target:d6", "exact_target_fixation"),
            continuationTerms = List("exact_target_fixation", "fixed_target:d6", "best_branch:f3d2|b8a6"),
            structureTransitionTerms = List("exact_target_fixation", "fixed_target:d6"),
            exactSliceProof = Some(PlayerFacingExactSliceProof.ExactTargetFixation("d6"))
          ),
          "Fixed target",
          "The checked line keeps d6 fixed as the target.",
          Some("d6")
        ),
        (
          supportedPositionProbePacket(
            proofFamily = ProofFamilyId.BackwardPawnTargeting.wireKey,
            proofSource = PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
            anchorSquare = "c6",
            ownerSeedTerms = List("c6", "fixed_target:c6", "carlsbad_fixed_target_probe"),
            continuationTerms = List("carlsbad_fixed_target_probe", "fixed_target:c6", "best_branch:b4b5|a6b5"),
            structureTransitionTerms = List("carlsbad_fixed_target_probe", "fixed_target:c6"),
            exactSliceProof = Some(PlayerFacingExactSliceProof.CarlsbadFixedTarget("c6", minoritySupport = true))
          ),
          "Minority attack",
          "The checked line keeps c6 as the minority-attack fixed target.",
          Some("c6")
        ),
        (
          supportedPositionProbePacket(
            proofFamily = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily,
            proofSource = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource,
            anchorSquare = "e5",
            ownerSeedTerms = List("e5", "coordinated_target:e5", "target_focused_coordination_probe"),
            continuationTerms = List("target_focused_coordination_probe", "coordinated_target:e5"),
            structureTransitionTerms = List("target_focused_coordination_probe", "coordinated_target:e5"),
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.TargetFocusedCoordination(
                  targetSquare = "e5",
                  supportFromSquares = List("e3", "g2"),
                  targetPieces = List("target_knight")
                )
              )
          ),
          "Target coordination",
          "The checked line coordinates pressure on e5 from e3 and g2.",
          Some("e5")
        )
      )

    cases.foreach { case (packet, expectedLabel, expectedText, expectedTarget) =>
      val rows =
        noRankedSupportedLocalRows(localInputs = inputs(packet = packet, claimText = expectedText))

      assertEquals(rows.map(_.label), List(expectedLabel), clue(rows))
      assertEquals(rows.head.authority.flatMap(_.target), expectedTarget, clue(rows))
      assertEquals(rows.head.text, expectedText, clue(rows))
      assertEquals(
        rows.head.authority,
        Some(expectedExchangeOwnershipAuthority(expectedLabel)),
        clue(rows)
      )
    }
  }

  test("projects FEN-backed target-focused coordination exact-slice packets through the public row") {
    val cases =
      List(
        (
          "K09A-certified-coordination",
          "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PP2PPBP/2RQ1RK1 w - - 4 13",
          "d1b3",
          "Qb3",
          List("d1b3", "d8d7", "f1d1", "a8c8", "d4e6", "f7e6", "e3f4", "e7b4", "c3e4"),
          Some("Qd7")
        ),
        (
          "K09D-certified-coordination",
          "1r1q1rk1/pp3ppp/2n2n2/3p4/3P2b1/2N2N2/PP2BPPP/2RQ1RK1 w - - 3 13",
          "h2h3",
          "h3",
          List(
            "h2h3",
            "g4f3",
            "e2f3",
            "f8e8",
            "d1d2",
            "d8d7",
            "f1d1",
            "h7h6",
            "a2a3",
            "c6e7",
            "d1e1",
            "b7b5",
            "b2b4",
            "a7a6",
            "e1e5",
            "e7g6",
            "e5e8",
            "b8e8",
            "f3e2"
          ),
          Some("Bxf3")
        )
      )

    cases.foreach { case (fixtureId, fen, playedMove, playedSan, line, expectedBestDefense) =>
      val coordinationCtx =
        topPvPracticalContext(
          fen = fen,
          playedMove = playedMove,
          playedSan = playedSan,
          line = line,
          scoreCp = 20
        )
      val surface =
        StrategyPackSurface.from(
          Some(
            StrategyPack(
              sideToMove = "white",
              pieceMoveRefs =
                List(
                  StrategyPieceMoveRef(
                    ownerSide = "white",
                    piece = "rook",
                    from = "c1",
                    target = "c6",
                    idea = "contest the c6 target with coordinated pressure",
                    evidence = List("target_knight")
                  ),
                  StrategyPieceMoveRef(
                    ownerSide = "white",
                    piece = "bishop",
                    from = "e3",
                    target = "c6",
                    idea = "contest the c6 target with coordinated pressure",
                    evidence = List("support_piece")
                  )
                ),
              pieceRoutes =
                List(
                  StrategyPieceRoute(
                    side = "white",
                    piece = "rook",
                    from = "c1",
                    route = List("c1", "c6"),
                    purpose = "coordination and plan activation against c6",
                    confidence = 0.84,
                    evidence = List("target_focused_coordination_probe")
                  )
                )
            )
          )
        )
      val delta =
        PlayerFacingTruthModePolicy
          .mainPathMoveDeltaEvidence(coordinationCtx, surface, Some(safeTruthContract(playedMove)))
          .getOrElse(fail(s"expected target-focused coordination exact-slice packet for $fixtureId"))
      val packet = delta.packet
      val exactProof =
        PlayerFacingExactSliceProof.TargetFocusedCoordination(
          targetSquare = "c6",
          supportFromSquares = List("c1", "e3"),
          targetPieces = List("target_knight")
        )
      val rows =
        noRankedSupportedLocalRows(
          localCtx = coordinationCtx,
          localInputs =
            inputs(
              packet = packet,
              claimText = "the pressure is coordinated on c6."
            ),
          playedMove = playedMove
        )

      assertEquals(coordinationCtx.fen, fen, clue(fixtureId))
      assertEquals(coordinationCtx.playedMove, Some(playedMove), clue(fixtureId))
      assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.PressureIncrease, clue(fixtureId))
      assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource, clue(fixtureId))
      assertEquals(packet.proofFamily, PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily, clue(fixtureId))
      assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof), clue(fixtureId))
      assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(fixtureId -> packet))
      assertEquals(packet.bestDefenseMove, expectedBestDefense, clue(fixtureId -> packet))
      assert(packet.bestDefenseBranchKey.nonEmpty, clue(fixtureId -> packet))
      assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven, clue(fixtureId))
      assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable, clue(fixtureId))
      assertEquals(rows.map(_.label), List("Target coordination"), clue(fixtureId -> rows))
      assertEquals(rows.head.text, "The checked line coordinates pressure on c6 from c1 and e3.", clue(fixtureId -> rows))
      assertEquals(
        rows.head.authority,
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6"))),
        clue(fixtureId -> rows)
      )
    }
  }

  test("does not project exact position-probe proof when packet target terms mismatch") {
    val staticWeaknessFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id
      )
    val cases =
      List(
        supportedPositionProbePacket(
          proofFamily = staticWeaknessFamily,
          proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
          anchorSquare = "d6",
          ownerSeedTerms = List("d6", "fixed_target:e5", "exact_target_fixation"),
          continuationTerms = List("exact_target_fixation", "fixed_target:e5", "best_branch:f3d2|b8a6"),
          structureTransitionTerms = List("exact_target_fixation", "fixed_target:e5"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.ExactTargetFixation("d6"))
        ),
        supportedPositionProbePacket(
          proofFamily = ProofFamilyId.BackwardPawnTargeting.wireKey,
          proofSource = PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
          anchorSquare = "c6",
          ownerSeedTerms = List("c6", "fixed_target:c3", "carlsbad_fixed_target_probe"),
          continuationTerms = List("carlsbad_fixed_target_probe", "fixed_target:c3", "best_branch:b4b5|a6b5"),
          structureTransitionTerms = List("carlsbad_fixed_target_probe", "fixed_target:c3"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.CarlsbadFixedTarget("c6", minoritySupport = true))
        ),
        supportedPositionProbePacket(
          proofFamily = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily,
          proofSource = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource,
          anchorSquare = "e5",
          ownerSeedTerms = List("e5", "coordinated_target:d4", "target_focused_coordination_probe"),
          continuationTerms = List("target_focused_coordination_probe", "coordinated_target:d4"),
          structureTransitionTerms = List("target_focused_coordination_probe", "coordinated_target:d4"),
          exactSliceProof =
            Some(
              PlayerFacingExactSliceProof.TargetFocusedCoordination(
                targetSquare = "e5",
                supportFromSquares = List("e3", "g2"),
                targetPieces = List("target_knight")
              )
            )
        )
      )

    cases.foreach { packet =>
      val rows =
        noRankedSupportedLocalRows(localInputs = inputs(packet = packet))

      assertEquals(rows, Nil, clue(packet))
    }
  }

  test("does not project move-local exact target fixation as a position-probe row") {
    val staticWeaknessFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id
      )
    val moveLocal =
      supportedPositionProbePacket(
        proofFamily = staticWeaknessFamily,
        proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
        anchorSquare = "d6",
        ownerSeedTerms = List("d6", "fixed_target:d6", "exact_target_fixation"),
        continuationTerms = List("exact_target_fixation", "fixed_target:d6"),
        structureTransitionTerms = List("exact_target_fixation", "fixed_target:d6"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.ExactTargetFixation("d6"))
      ).copy(scope = PlayerFacingPacketScope.MoveLocal)

    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = moveLocal))

    assert(!rows.exists(_.label == "Knight outpost"), clue(rows))
  }

  test("certifies played Ne4 pressure on the Carlsbad c3 pawn target from board and checked PV") {
    val targetCtx =
      topPvPracticalContext(
        fen = "r2qk2r/pp3ppp/2n1pn2/1B1pPb2/3P4/2P5/PP4PP/RNBQK2R b KQkq - 0 10",
        playedMove = "f6e4",
        playedSan = "Ne4",
        line = List("f6e4", "e1g1", "d8b6", "d1e2", "e8g8", "c1e3"),
        scoreCp = -25
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = None,
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = Nil
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(targetCtx, surface, Some(safeTruthContract("f6e4")))
        .getOrElse(fail("expected played Ne4 to certify the c3 pawn target"))
    val packet = delta.packet
    val exactProof = PlayerFacingExactSliceProof.ExactTargetFixation("c3")
    val staticWeaknessFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id
      )
    val positionPacket =
      supportedPositionProbePacket(
        proofFamily = staticWeaknessFamily,
        proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
        anchorSquare = "c3",
        ownerSeedTerms = List("c3", "fixed_target:c3", "structural_pawn_target"),
        continuationTerms = List("exact_target_fixation", "fixed_target:c3", "best_branch:f6e4|e1g1"),
        structureTransitionTerms = List("weak_complex:c3", "structural_pawn_target"),
        exactSliceProof = Some(exactProof)
      )
    val rows =
      noRankedSupportedLocalRows(
        localCtx = targetCtx,
        localInputs = inputs(packet = positionPacket, claimText = "Ne4 fixes the c3 pawn target."),
        playedMove = "f6e4"
      )

    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.PressureIncrease)
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.ExactTargetFixationProofSource)
    assertEquals(packet.proofFamily, ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).get.wireKey)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assert(packet.proofPathWitness.ownerSeedTerms.contains("fixed_target:c3"), clue(packet.proofPathWitness.ownerSeedTerms))
    assert(packet.proofPathWitness.structureTransitionTerms.contains("structural_pawn_target"), clue(packet.proofPathWitness.structureTransitionTerms))
    assert(
      packet.proofPathWitness.structureTransitionTerms.contains("target_persistent_after_line:c3"),
      clue(packet.proofPathWitness.structureTransitionTerms)
    )
    assert(
      packet.proofPathWitness.structureTransitionTerms.contains("target_attacked_after_line:c3"),
      clue(packet.proofPathWitness.structureTransitionTerms)
    )
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven, clue(packet))
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable, clue(packet))
    assertEquals(rows.map(_.label), List("Fixed target"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line keeps c3 fixed as a pawn target, with the b2 pawn still defending it."
    )
  }

  test("does not certify incidental capture pressure as board-only target fixation") {
    val captureCtx =
      topPvPracticalContext(
        fen = "r2qkb1r/pp1n1p1p/2p1p1p1/2P5/3Pn3/5Q1P/PP3PP1/R1B1KB1R w KQkq - 0 11",
        playedMove = "f3e4",
        playedSan = "Qxe4",
        line = List("f3e4", "a7a6", "f1c4"),
        scoreCp = 24
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("white"),
        dominantIdea = None,
        secondaryIdea = None,
        campaignOwner = Some("white"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = Nil
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(captureCtx, surface, Some(safeTruthContract("f3e4")))

    assert(
      delta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.ExactTargetFixationProofSource),
      clue(delta)
    )
  }

  test("does not certify generic weak-pawn pressure as board-only target fixation without typed focus") {
    val quietCtx =
      topPvPracticalContext(
        fen = "r4rk1/pp3ppp/3q1n2/2p5/2P3b1/3PBPN1/PP2Q1PP/R4RK1 b - - 0 15",
        playedMove = "g4e6",
        playedSan = "Be6",
        line = List("g4e6", "b2b3", "f8e8", "e2f2", "b7b6"),
        scoreCp = -30
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = None,
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = Nil
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(quietCtx, surface, Some(safeTruthContract("g4e6")))

    assert(
      delta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.ExactTargetFixationProofSource),
      clue(delta)
    )
  }

  test("does not project color-complex squeeze terms without typed exact proof") {
    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = supportedColorComplexPacket(exactSliceProof = None)))

    assertEquals(rows, Nil)
  }

  test("does not project color-complex squeeze row when typed proof geometry is inconsistent") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs =
          inputs(
            packet =
              supportedColorComplexPacket(
                exactSliceProof = Some(PlayerFacingExactSliceProof.ColorComplexSqueeze("e5", "light", "knight", "c4"))
              )
          )
      )

    assertEquals(rows, Nil)
  }

  test("does not project color-complex squeeze proof without matching witness terms") {
    val packet = supportedColorComplexPacket()
    val strippedWitness =
      packet.proofPathWitness.copy(
        ownerSeedTerms = List("color_complex_squeeze_probe"),
        continuationTerms = List("color_complex_squeeze_probe"),
        structureTransitionTerms = List("color_complex_squeeze_probe")
      )
    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = packet.copy(proofPathWitness = strippedWitness)))

    assertEquals(rows, Nil)
  }

  test("does not project color-complex squeeze proof when the mirrored minor-piece role mismatches") {
    val packet = supportedColorComplexPacket()
    val mismatchedWitness =
      packet.proofPathWitness.copy(
        ownerSeedTerms =
          packet.proofPathWitness.ownerSeedTerms
            .filterNot(_.startsWith("minor_piece:")) :+ "minor_piece:bishop_c4"
      )

    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = packet.copy(proofPathWitness = mismatchedWitness)))

    assertEquals(rows, Nil)
  }

  test("suppresses color-complex squeeze row under tactical truth mode") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs = inputs(packet = supportedColorComplexPacket(), truthMode = PlayerFacingTruthMode.Tactical)
      )

    assertEquals(rows, Nil)
  }

  test("projects exact named route-network surface into a bounded row") {
    val surface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "b6",
        counterplayScoreDrop = 140
      )
    val rows =
      noRankedSupportedLocalRows(localInputs = routeNetworkInputs(Some(surface)))

    assertEquals(rows.map(_.label), List("Route denial"))
    assertEquals(
      rows.head.text,
      "The checked line keeps c6 closed, takes the c-file away, and cuts off the b6 reroute."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("keeps broader route-chain intermediates backend-only") {
    val surface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "a5",
        counterplayScoreDrop = 140,
        intermediateSquare = Some("b6")
      )
    val rows =
      noRankedSupportedLocalRows(localInputs = routeNetworkInputs(Some(surface)))

    assertEquals(rows, Nil)
  }

  test("suppresses named route-network row under heavy-piece block or tactical truth mode") {
    val surface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "b6",
        counterplayScoreDrop = 140
      )
    val heavyPieceBlocked =
      noRankedSupportedLocalRows(localInputs = routeNetworkInputs(Some(surface), heavyPieceLocalBindBlocked = true))
    val tactical =
      noRankedSupportedLocalRows(localInputs = routeNetworkInputs(Some(surface), truthMode = PlayerFacingTruthMode.Tactical))

    assertEquals(heavyPieceBlocked, Nil)
    assertEquals(tactical, Nil)
  }

  test("projects certified dual-axis bind into a bounded break-and-entry row") {
    val rows =
      noRankedSupportedLocalRows(localInputs = dualAxisInputs(Some(certifiedDualAxisBind())))

    assertEquals(rows.map(_.label), List("Break and entry"))
    assertEquals(rows.head.text, "The checked line keeps the ...c5 break shut while keeping b4 unavailable.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("does not project uncertified or reinflation-risk dual-axis bind") {
    val uncertified =
      noRankedSupportedLocalRows(
        localInputs = dualAxisInputs(Some(certifiedDualAxisBind(failsIf = List("dual_axis_burden_missing"))))
      )
    val reinflationRisk =
      noRankedSupportedLocalRows(
        localInputs = dualAxisInputs(Some(certifiedDualAxisBind(counterplayReinflationRisk = "high")))
      )

    assertEquals(uncertified, Nil)
    assertEquals(reinflationRisk, Nil)
  }

  test("suppresses dual-axis bind row under heavy-piece block or tactical truth mode") {
    val heavyPieceBlocked =
      noRankedSupportedLocalRows(localInputs = dualAxisInputs(Some(certifiedDualAxisBind()), heavyPieceLocalBindBlocked = true))
    val tactical =
      noRankedSupportedLocalRows(
        localInputs = dualAxisInputs(Some(certifiedDualAxisBind()), truthMode = PlayerFacingTruthMode.Tactical)
      )

    assertEquals(heavyPieceBlocked, Nil)
    assertEquals(tactical, Nil)
  }

  test("projects certified restricted-defense conversion into a bounded technical row") {
    val rows =
      noRankedSupportedLocalRows(
        localCtx = neutralizeCtx,
        localInputs = restrictedDefenseInputs(Some(certifiedRestrictedDefenseConversion())),
        playedMove = "e2e3"
      )

    assertEquals(rows.map(_.label), List("Technical conversion"))
    assertEquals(rows.head.text, "The checked line keeps the conversion route intact after Ke7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("keeps three independently admitted typed supported-local surface rows") {
    val routeSurface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "b6",
        counterplayScoreDrop = 140
      )
    val rows =
      noRankedSupportedLocalRows(
        localCtx = neutralizeCtx,
        localInputs =
          routeNetworkInputs(Some(routeSurface)).copy(
            dualAxisBindSurface = Some(certifiedDualAxisBind()),
            restrictedDefenseConversionSurface = Some(certifiedRestrictedDefenseConversion())
          ),
        playedMove = "e2e3"
      )

    assertEquals(rows.map(_.label), List("Route denial", "Break and entry", "Technical conversion"))
    assert(rows.forall(_.authority.contains(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))), clue(rows))
  }

  test("mixed packet and typed supported-local surfaces reserve two typed slots") {
    val routeSurface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "b6",
        counterplayScoreDrop = 140
      )
    val rows =
      noRankedSupportedLocalRows(
        localCtx = neutralizeCtx,
        localInputs =
          inputs().copy(
            namedRouteNetworkSurface = Some(routeSurface),
            dualAxisBindSurface = Some(certifiedDualAxisBind()),
            restrictedDefenseConversionSurface = Some(certifiedRestrictedDefenseConversion())
          ),
        playedMove = "e2e3"
      )

    assertEquals(rows.map(_.label), List("Counterplay break", "Route denial", "Break and entry"))
    assert(!rows.exists(_.label == "Technical conversion"), clue(rows))
  }

  test("does not project uncertified or stale restricted-defense conversion") {
    val uncertified =
      noRankedSupportedLocalRows(
        localCtx = neutralizeCtx,
        localInputs =
          restrictedDefenseInputs(
            Some(certifiedRestrictedDefenseConversion(failsIf = List("route_persistence_missing")))
          ),
        playedMove = "e2e3"
      )
    val stale =
      noRankedSupportedLocalRows(
        localCtx = neutralizeCtx,
        localInputs =
          restrictedDefenseInputs(
            Some(
              certifiedRestrictedDefenseConversion(
                routePersistence =
                  RestrictedDefenseConversionProof.RoutePersistence(
                    bestDefenseStable = true,
                    futureSnapshotPersistent = false,
                    counterplayStillCompressed = true,
                    directBestDefensePresent = true,
                    sameDefendedBranch = true
                  )
              )
            )
          ),
        playedMove = "e2e3"
      )

    assert(!uncertified.exists(_.label == "Technical conversion"), clue(uncertified))
    assert(!stale.exists(_.label == "Technical conversion"), clue(stale))
  }

  test("suppresses restricted-defense conversion row under tactical truth mode") {
    val rows =
      noRankedSupportedLocalRows(
        localCtx = neutralizeCtx,
        localInputs =
          restrictedDefenseInputs(
            Some(certifiedRestrictedDefenseConversion()),
            truthMode = PlayerFacingTruthMode.Tactical
          ),
        playedMove = "e2e3"
      )

    assertEquals(rows, Nil)
  }

  test("projects exact outpost-occupation packet into a bounded row") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs = inputs(
          packet = supportedOutpostPacket(),
          claimText = "The move occupies the e5 outpost."
        )
      )

    assertEquals(rows.map(_.label), List("Knight outpost"))
    assertEquals(rows.head.text, "The checked line puts the knight on the e5 outpost.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
    )
  }

  test("projects FEN-backed outpost exact-slice packet through the public row") {
    val outpostCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/3P4/5N2/8/6K1 w - - 0 1",
        playedMove = "f3e5",
        playedSan = "Ne5",
        line = List("f3e5", "g8f8", "d4d5", "f8e8"),
        scoreCp = 42
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(outpostCtx, StrategyPackSurface.from(None), Some(safeTruthContract("f3e5")))
        .getOrElse(fail("expected outpost exact-slice move-delta packet"))
    val packet = delta.packet
    val rows =
      noRankedSupportedLocalRows(
        localCtx = outpostCtx,
        localInputs = inputs(packet = packet, claimText = "The move occupies the e5 outpost."),
        playedMove = "f3e5"
      )

    assertEquals(outpostCtx.fen, "6k1/8/8/8/3P4/5N2/8/6K1 w - - 0 1")
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.OutpostEntrenchmentProofSource)
    assertEquals(packet.proofFamily, PlayerFacingTruthModePolicy.OutpostEntrenchmentProofFamily)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(PlayerFacingExactSliceProof.OutpostOccupation("knight", "e5")))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, PlayerFacingExactSliceProof.OutpostOccupation("knight", "e5")))
    assertEquals(packet.bestDefenseMove, Some("Kf8"))
    assert(packet.bestDefenseBranchKey.nonEmpty, clue(packet))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven, clue(packet))
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable, clue(packet))
    assertEquals(rows.map(_.label), List("Knight outpost"), clue(rows))
    assertEquals(rows.head.text, "The checked line puts the knight on the e5 outpost.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
    )
  }

  test("does not certify transient Ne4 as an outpost when an enemy minor attacks e4") {
    val transientCtx =
      topPvPracticalContext(
        fen = "rnbqk2r/ppp1ppbp/5np1/3p4/3P1B2/2N2N1P/PPP1PPP1/R2QKB1R b KQkq - 3 5",
        playedMove = "f6e4",
        playedSan = "Ne4",
        line = List("f6e4", "e2e3", "e4c3", "b2c3", "c7c5"),
        scoreCp = -18
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(transientCtx, StrategyPackSurface.from(None), Some(safeTruthContract("f6e4")))

    assert(
      !delta.exists(_.packet.proofPathWitness.exactSliceProof.exists {
        case _: PlayerFacingExactSliceProof.OutpostOccupation => true
        case _                                                => false
      }),
      clue(delta.map(_.packet))
    )
  }

  test("does not project outpost occupation terms without typed exact proof") {
    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = supportedOutpostPacket(exactSliceProof = None)))

    assertEquals(rows, Nil)
  }

  test("does not project outpost occupation proof without matching witness terms") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs =
          inputs(
            packet =
              supportedOutpostPacket(
                ownerSeedTerms = List("e5", "outpost:e5", "piece:knight"),
                structureTransitionTerms = List("outpost_occupation")
              )
          )
      )

    assertEquals(rows, Nil)
  }

  test("suppresses outpost occupation row under tactical truth mode") {
    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = supportedOutpostPacket(), truthMode = PlayerFacingTruthMode.Tactical))

    assertEquals(rows, Nil)
  }

  test("projects exact local-file entry packet into a bounded row") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs = inputs(
          packet = supportedLocalFilePacket(),
          claimText = "The file-entry bind keeps c6 under pressure."
        )
      )

    assertEquals(rows.map(_.label), List("File entry"))
    assertEquals(rows.head.text, "The checked line keeps pressure on c6 through the c-file.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
    )
  }

  test("does not project FEN-backed local-file entry pair when typed proof geometry is off-file") {
    val fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23"
    val playedMove = "a2a3"
    val exactProof = PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "b4")
    val line = List("a2a3", "c8c7", "c1c2", "f8c8")
    val packet =
      supportedLocalFilePacket(
        anchorSquare = "b4",
        continuationTerms = List("local_file_entry_bind", "c-file", "b4") ++ line,
        structureTransitionTerms = List("file-entry:c-file:b4"),
        bestDefenseMove = Some("c8c7"),
        bestDefenseBranchKey = Some("a2a3|c8c7"),
        exactSliceProof = Some(exactProof)
      )
    val localCtx =
      topPvPracticalContext(
        fen = fen,
        playedMove = playedMove,
        playedSan = "a3",
        line = line,
        scoreCp = 140
      )
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(line, scoreCp = 140, depth = 18)), maxPlies = 2)
        .getOrElse(fail("local-file entry public witness line should replay legally"))
    val rows =
      noRankedSupportedLocalRows(
        localCtx = localCtx,
        localInputs = inputs(
          packet = packet,
          claimText = "The file-entry bind keeps b4 under pressure."
        ),
        playedMove = playedMove
      )

    assertEquals(replay.map(_.uci), line.take(2))
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(!PlayerFacingExactSliceProofFacts.validShape(exactProof), clue(exactProof))
    assert(!PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assert(!rows.exists(_.label == "File entry"), clue(rows))
  }

  test("does not project local-file entry rows without exact matching proof") {
    val basePacket = supportedLocalFilePacket()
    val cases =
      List(
        "missing exact slice proof" ->
          supportedLocalFilePacket(exactSliceProof = None),
        "missing matching witness terms" ->
          basePacket.copy(
            anchorTerms = Nil,
            proofPathWitness =
              basePacket.proofPathWitness.copy(
                ownerSeedTerms = Nil,
                continuationTerms = List("local_file_entry_bind"),
                structureTransitionTerms = Nil
              )
          ),
        "split file and square terms only" ->
          basePacket.copy(
            proofPathWitness =
              basePacket.proofPathWitness.copy(
                structureTransitionTerms = Nil
              )
          ),
        "entry square off claimed file" ->
          basePacket.copy(
            anchorTerms = List("c-file", "d6"),
            proofPathWitness =
              PlayerFacingProofPathWitness(
                ownerSeedTerms = List("c-file", "d6"),
                continuationTerms = List("local_file_entry_bind", "c-file", "d6", "d4d6", "c7c6"),
                structureTransitionTerms = List("file-entry:c-file:d6"),
                exactSliceProof = Some(PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "d6"))
              )
          )
      )

    cases.foreach { case (name, packet) =>
      val rows =
        noRankedSupportedLocalRows(localInputs = inputs(packet = packet))

      assertEquals(rows, Nil, clue(name -> rows))
    }
  }

  test("suppresses local-file entry row under tactical truth mode") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs = inputs(packet = supportedLocalFilePacket(), truthMode = PlayerFacingTruthMode.Tactical)
      )

    assertEquals(rows, Nil)
  }

  test("projects exact IQP inducement packet into a bounded row") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs = inputs(
          packet = supportedIqpPacket(),
          claimText = "This sequence leaves an isolated pawn as the local target."
        )
      )

    assertEquals(rows.map(_.label), List("IQP target"))
    assertEquals(rows.head.text, "The checked line leaves d5 as an isolated pawn target.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5")))
    )
  }

  test("does not project transient IQP inducement when the checked line resolves the pawn") {
    val iqpCtx =
      topPvPracticalContext(
        fen = "bq1rrbk1/3n1pp1/pp2pn1p/3p4/2P1P3/P1N1BP2/1P1NBQPP/2RR3K w - - 0 25",
        playedMove = "c4d5",
        playedSan = "cxd5",
        line = List("c4d5", "e6d5", "e4d5", "f6d5"),
        scoreCp = 38
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(iqpCtx, StrategyPackSurface.from(None), Some(safeTruthContract("c4d5")))

    assertEquals(iqpCtx.fen, "bq1rrbk1/3n1pp1/pp2pn1p/3p4/2P1P3/P1N1BP2/1P1NBQPP/2RR3K w - - 0 25")
    assertEquals(
      delta.flatMap(_.packet.proofPathWitness.exactSliceProof),
      None,
      clue(delta.map(_.packet))
    )
  }

  test("does not project Evans-Opsahl IQP when horizon target pressure is missing") {
    val iqpCtx =
      topPvPracticalContext(
        fen = "r3rnk1/1p3ppp/p1p5/3p2q1/PP1P2b1/2QBP3/3N1PPP/1R3RK1 w - - 3 17",
        playedMove = "f1c1",
        playedSan = "Rfc1",
        line = List("f1c1", "h7h5", "b4b5", "c6b5", "a4b5"),
        scoreCp = 34
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(iqpCtx, StrategyPackSurface.from(None), Some(safeTruthContract("f1c1")))

    assertEquals(iqpCtx.fen, "r3rnk1/1p3ppp/p1p5/3p2q1/PP1P2b1/2QBP3/3N1PPP/1R3RK1 w - - 3 17")
    assertEquals(
      delta.flatMap(_.packet.proofPathWitness.exactSliceProof),
      None,
      clue(delta.map(_.packet))
    )
  }

  test("does not project Capablanca-Golombek IQP when horizon target pressure is missing") {
    val iqpCtx =
      topPvPracticalContext(
        fen = "r3r1k1/pp3pn1/2pq2pp/3p4/NP1P4/3QP2P/P4PP1/1RR3K1 w - - 0 23",
        playedMove = "b4b5",
        playedSan = "b5",
        line = List("b4b5", "e8c8", "b5c6", "b7b6"),
        scoreCp = 28
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(iqpCtx, StrategyPackSurface.from(None), Some(safeTruthContract("b4b5")))

    assertEquals(iqpCtx.fen, "r3r1k1/pp3pn1/2pq2pp/3p4/NP1P4/3QP2P/P4PP1/1RR3K1 w - - 0 23")
    assertEquals(
      delta.flatMap(_.packet.proofPathWitness.exactSliceProof),
      None,
      clue(delta.map(_.packet))
    )
  }

  test("does not project transient Alekhine-Bogoljubow IQP when the target pawn moves") {
    val iqpCtx =
      topPvPracticalContext(
        fen = "rnb1k2r/pp3ppp/4p3/2pqP3/PbpPn3/2N2N2/1PQ1BPPP/R1B2RK1 b kq - 1 10",
        playedMove = "e4c3",
        playedSan = "Nxc3",
        line = List("e4c3", "b2c3", "c5d4", "c3b4"),
        scoreCp = 31
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(iqpCtx, StrategyPackSurface.from(None), Some(safeTruthContract("e4c3")))

    assertEquals(iqpCtx.fen, "rnb1k2r/pp3ppp/4p3/2pqP3/PbpPn3/2N2N2/1PQ1BPPP/R1B2RK1 b kq - 1 10")
    assertEquals(
      delta.flatMap(_.packet.proofPathWitness.exactSliceProof),
      None,
      clue(delta.map(_.packet))
    )
  }

  test("does not project Najdorf-Sergeant IQP when horizon target pressure is blocked") {
    val iqpCtx =
      topPvPracticalContext(
        fen = "r1b2rk1/pp2qppp/4p3/2nn4/3N4/2N1P3/PPQ2PPP/3RKB1R w K - 0 12",
        playedMove = "c3d5",
        playedSan = "Nxd5",
        line = List("c3d5", "e6d5", "f1e2", "b7b6"),
        scoreCp = 33
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(iqpCtx, StrategyPackSurface.from(None), Some(safeTruthContract("c3d5")))

    assertEquals(iqpCtx.fen, "r1b2rk1/pp2qppp/4p3/2nn4/3N4/2N1P3/PPQ2PPP/3RKB1R w K - 0 12")
    assertEquals(
      delta.flatMap(_.packet.proofPathWitness.exactSliceProof),
      None,
      clue(delta.map(_.packet))
    )
  }

  test("does not project Botvinnik-Vidmar IQP when horizon target pressure is missing") {
    val fen = "r1bq1rk1/pp1nbppp/4pn2/2pp2B1/2PP4/2NBPN2/PP3PPP/R2Q1RK1 b - - 1 8"
    val playedMove = "c5d4"
    val line =
      List(
        "c5d4",
        "e3d4",
        "d5c4",
        "d3c4",
        "h7h6",
        "g5h4",
        "d7b6",
        "c4b3",
        "c8d7",
        "f3e5",
        "d7c6",
        "f1e1",
        "b6d5",
        "d1d3",
        "d5f4",
        "d3e3",
        "f4d5"
      )
    val iqpCtx =
      topPvPracticalContext(
        fen = fen,
        playedMove = playedMove,
        playedSan = "cxd4",
        line = line,
        scoreCp = -120
      )
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(line, scoreCp = -120, depth = 18)), maxPlies = 2)
        .getOrElse(fail("Botvinnik-Vidmar opening IQP public witness line should replay legally"))
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(iqpCtx, StrategyPackSurface.from(None), Some(safeTruthContract(playedMove)))

    assertEquals(iqpCtx.fen, fen)
    assertEquals(replay.map(_.uci), line.take(2))
    assertEquals(
      delta.flatMap(_.packet.proofPathWitness.exactSliceProof),
      None,
      clue(delta.map(_.packet))
    )
  }

  test("projects live cxd5 IQP when the horizon queen pressures d4") {
    val iqpCtx =
      topPvPracticalContext(
        fen = "rnbqkbnr/pp2pppp/2p5/3P4/3P4/8/PPP2PPP/RNBQKBNR b KQkq - 0 3",
        playedMove = "c6d5",
        playedSan = "cxd5",
        line = List("c6d5", "c2c4", "g8f6", "c4d5", "d8d5", "g1f3", "d5a5", "c1d2"),
        scoreCp = 40
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(iqpCtx, StrategyPackSurface.from(None), Some(safeTruthContract("c6d5")))
        .getOrElse(fail("expected live cxd5 IQP exact-slice move-delta packet"))
    val packet = delta.packet
    val exactProof =
      PlayerFacingExactSliceProof.IqpInducement("d4", List("c6d5", "c2c4", "g8f6", "c4d5", "d8d5", "g1f3"))
    val rows =
      noRankedSupportedLocalRows(
        localCtx = iqpCtx,
        localInputs =
          inputs(
            packet = packet,
            claimText = "This sequence leaves an isolated pawn as the local target."
          ),
        playedMove = "c6d5"
      )

    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing)
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.IQPInducementProbeProofSource)
    assertEquals(packet.proofFamily, ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.IQPInducement).get.wireKey)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assert(packet.proofPathWitness.ownerSeedTerms.contains("target_pressure:d4"), clue(packet.proofPathWitness.ownerSeedTerms))
    assert(packet.proofPathWitness.structureTransitionTerms.contains("target_pressure:d4"), clue(packet.proofPathWitness.structureTransitionTerms))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(rows.map(_.label), List("IQP target"), clue(rows))
    assertEquals(rows.head.text, "The checked line leaves d4 as an isolated pawn target.")
    assertEquals(rows.head.refSans, List("cxd5", "c4", "Nf6", "cxd5", "Qxd5", "Nf3"))
  }

  test("does not project IQP rows without typed induced target proof") {
    val cases =
      List(
        "missing exact slice proof" ->
          supportedIqpPacket(exactSliceProof = None),
        "missing induced target witness terms" ->
          supportedIqpPacket(
            structureTransitionTerms = List("central_isolated_pawn"),
            ownerSeedTerms = List("iqp_inducement", "iqp")
          ),
        "missing target pressure witness term" ->
          supportedIqpPacket(
            structureTransitionTerms = List("before_not_isolated:d5", "after_isolated:d5", "central_isolated_pawn"),
            ownerSeedTerms = List("d5", "isolated_pawn:d5", "iqp_inducement")
          )
      )

    cases.foreach { case (name, packet) =>
      val rows =
        noRankedSupportedLocalRows(localInputs = inputs(packet = packet))

      assertEquals(rows, Nil, clue(name -> rows))
    }
  }

  test("suppresses IQP inducement row under tactical truth mode") {
    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = supportedIqpPacket(), truthMode = PlayerFacingTruthMode.Tactical))

    assertEquals(rows, Nil)
  }

  test("projects exact simplification-window packet into a bounded row") {
    val exactProof = PlayerFacingExactSliceProof.SimplificationWindow("d5")
    val packet = supportedSimplificationPacket(exactSliceProof = Some(exactProof))
    val rows =
      noRankedSupportedLocalRows(
        localInputs = inputs(
          packet = packet,
          claimText = "This trade keeps the same local edge on d5."
        )
      )

    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assertEquals(rows.map(_.label), List("Simplification"))
    assertEquals(rows.head.text, "The checked line keeps the same local edge after the exchange on d5.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5")))
    )
  }

  test("projects Botvinnik-Vidmar PGN-backed simplification exact-slice packet through the public row") {
    val fen = "r2q1rk1/pp2bppp/4pn2/3bN1B1/1n1P4/1BN4Q/PP3PPP/3R1RK1 w - - 11 16"
    val playedMove = "c3d5"
    val line =
      List(
        "c3d5",
        "f6d5",
        "g5e7",
        "d8e7",
        "f2f4",
        "f7f6",
        "e5d3",
        "b4d3",
        "h3d3",
        "f6f5",
        "b3d5",
        "e6d5",
        "f1e1",
        "e7b4",
        "b2b3",
        "a8e8"
      )
    val simplificationCtx =
      topPvPracticalContext(
        fen = fen,
        playedMove = playedMove,
        playedSan = "Nxd5",
        line = line,
        scoreCp = 14
      )
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(line, scoreCp = 14, depth = 18)), maxPlies = 2)
        .getOrElse(fail("Botvinnik-Vidmar simplification public witness line should replay legally"))
    val surface =
      StrategyPackSurface.from(
        Some(
          StrategyPack(
            sideToMove = "white",
            pieceMoveRefs =
              List(
                StrategyPieceMoveRef(
                  ownerSide = "white",
                  piece = "N",
                  from = "c3",
                  target = "d5",
                  idea = "simplification trade keeps the local edge on d5",
                  tacticalTheme = Some("simplification")
                )
              ),
            directionalTargets =
              List(
                StrategyDirectionalTarget(
                  targetId = "target_d5_simplification",
                  ownerSide = "white",
                  piece = "N",
                  from = "c3",
                  targetSquare = "d5",
                  readiness = DirectionalTargetReadiness.Build,
                  strategicReasons = List("trade keeps the same local edge on d5"),
                  evidence = List("source-window")
                )
              ),
            strategicIdeas =
              List(
                StrategyIdeaSignal(
                  ideaId = "source-botvinnik-vidmar-1936-simplification-window-idea",
                  ownerSide = "white",
                  kind = StrategicIdeaKind.FavorableTradeOrTransformation,
                  group = StrategicIdeaGroup.InteractionAndTransformation,
                  readiness = StrategicIdeaReadiness.Ready,
                  focusSquares = List("d5"),
                  beneficiaryPieces = List("Nc3", "Bg5"),
                  confidence = 0.86,
                  evidenceRefs =
                    List(
                      EvidenceRef.Source(EvidenceSourceId.ClassificationTransformationWindow).wireKey
                    ),
                  targetSquare = Some("d5")
                )
              )
          )
        )
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(simplificationCtx, surface, Some(safeTruthContract(playedMove)))
        .getOrElse(fail("expected Botvinnik-Vidmar simplification exact-slice move-delta packet"))
    val packet = delta.packet
    val exactProof = PlayerFacingExactSliceProof.SimplificationWindow("d5")
    val rows =
      noRankedSupportedLocalRows(
        localCtx = simplificationCtx,
        localInputs =
          inputs(
            packet = packet,
            claimText = "This trade keeps the same local edge on d5."
          ),
        playedMove = playedMove
      )

    assertEquals(simplificationCtx.fen, fen)
    assertEquals(replay.map(_.uci), line.take(2))
    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing)
    assertEquals(packet.proofSource, PlanTaxonomy.PlanKind.SimplificationWindow.id)
    assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.SimplificationWindow.id)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assertEquals(packet.bestDefenseMove, Some("Nfxd5"))
    assert(packet.bestDefenseBranchKey.nonEmpty, clue(packet))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(rows.map(_.label), List("Simplification"), clue(rows))
    assertEquals(rows.head.text, "The checked line keeps the same local edge after the exchange on d5.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5"))),
      clue(rows)
    )
  }

  test("projects natural same-task simplification exact-slice packets through the public row") {
    val cases =
      List(
        (
          "natural-K09B",
          "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13",
          List("d4e6", "f7e6", "a1d1", "g8h8", "e3f4", "d8b6", "a2a3", "a8c8", "e2e4", "c6d4", "c2d2", "d4b3"),
          60
        ),
        (
          "natural-K09F",
          "2rqr1k1/pp2bpp1/2n1bn1p/3p4/3N4/P1N1B1P1/1P2PPBP/2RQ1RK1 w - - 1 14",
          List("d4e6", "f7e6", "g2h3", "d8d7", "c1c2", "e7f8", "c3b5", "a7a6", "b5d4"),
          40
        )
      )

    cases.foreach { case (fixtureId, fen, line, scoreCp) =>
      val playedMove = "d4e6"
      val exchangeSquare = "e6"
      val simplificationCtx =
        topPvPracticalContext(
          fen = fen,
          playedMove = playedMove,
          playedSan = "Nxe6",
          line = line,
          scoreCp = scoreCp
        )
      val replay =
        MoveReviewExchangeAnalyzer
          .boundedTopReplay(fen, List(VariationLine(line, scoreCp = scoreCp, depth = 18)), maxPlies = 2)
          .getOrElse(fail(s"$fixtureId simplification public witness line should replay legally"))
      val surface =
        StrategyPackSurface.from(
          Some(
            StrategyPack(
              sideToMove = "white",
              pieceMoveRefs =
                List(
                  StrategyPieceMoveRef(
                    ownerSide = "white",
                    piece = "N",
                    from = "d4",
                    target = exchangeSquare,
                    idea = s"simplification trade keeps the local edge on $exchangeSquare",
                    tacticalTheme = Some("simplification")
                  )
                ),
              directionalTargets =
                List(
                  StrategyDirectionalTarget(
                    targetId = s"target_${exchangeSquare}_simplification",
                    ownerSide = "white",
                    piece = "N",
                    from = "d4",
                    targetSquare = exchangeSquare,
                    readiness = DirectionalTargetReadiness.Build,
                    strategicReasons = List(s"trade keeps the same local edge on $exchangeSquare"),
                    evidence = List("natural-same-task-simplification")
                  )
                ),
              strategicIdeas =
                List(
                  StrategyIdeaSignal(
                    ideaId = s"$fixtureId-simplification-window-idea",
                    ownerSide = "white",
                    kind = StrategicIdeaKind.FavorableTradeOrTransformation,
                    group = StrategicIdeaGroup.InteractionAndTransformation,
                    readiness = StrategicIdeaReadiness.Ready,
                    focusSquares = List(exchangeSquare),
                    beneficiaryPieces = List("Nd4", "Be3"),
                    confidence = 0.82,
                    evidenceRefs =
                      List(
                        EvidenceRef.Source(EvidenceSourceId.ClassificationTransformationWindow).wireKey
                      ),
                    targetSquare = Some(exchangeSquare)
                  )
                )
            )
          )
        )
      val delta =
        PlayerFacingTruthModePolicy
          .mainPathMoveDeltaEvidence(simplificationCtx, surface, Some(safeTruthContract(playedMove)))
          .getOrElse(fail(s"expected $fixtureId simplification exact-slice move-delta packet"))
      val packet = delta.packet
      val exactProof = PlayerFacingExactSliceProof.SimplificationWindow(exchangeSquare)
      val rows =
        noRankedSupportedLocalRows(
          localCtx = simplificationCtx,
          localInputs =
            inputs(
              packet = packet,
              claimText = s"This trade keeps the same local edge on $exchangeSquare."
            ),
          playedMove = playedMove
        )

      assertEquals(simplificationCtx.fen, fen, clue(fixtureId))
      assertEquals(replay.map(_.uci), line.take(2), clue(fixtureId))
      assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing, clue(fixtureId))
      assertEquals(packet.proofSource, PlanTaxonomy.PlanKind.SimplificationWindow.id, clue(fixtureId))
      assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.SimplificationWindow.id, clue(fixtureId))
      assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof), clue(fixtureId))
      assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(fixtureId -> packet))
      assertEquals(packet.bestDefenseMove, Some("fxe6"), clue(fixtureId -> packet))
      assert(packet.bestDefenseBranchKey.nonEmpty, clue(fixtureId -> packet))
      assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven, clue(fixtureId))
      assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable, clue(fixtureId))
      assertEquals(rows.map(_.label), List("Simplification"), clue(fixtureId -> rows))
      assertEquals(rows.head.text, "The checked line keeps the same local edge after the exchange on e6.", clue(fixtureId -> rows))
      assertEquals(
        rows.head.authority,
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e6"))),
        clue(fixtureId -> rows)
      )
    }
  }

  test("does not project simplification-window rows without typed exact proof") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs = inputs(packet = supportedSimplificationPacket(exactSliceProof = None))
      )

    assertEquals(rows, Nil)
  }

  test("does not project simplification-window prose without an exchange-square witness") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs =
          inputs(
            packet =
              supportedSimplificationPacket(
                continuationTerms = List("exact_trade_continuation", "b5d5", "f7d5"),
                ownerSeedTerms = List("simplification_window", "local_edge"),
                structureTransitionTerms = List("simplification_window")
              )
          )
      )

    assertEquals(rows, Nil)
  }

  test("suppresses simplification-window row under tactical truth mode") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs = inputs(packet = supportedSimplificationPacket(), truthMode = PlayerFacingTruthMode.Tactical)
      )

    assertEquals(rows, Nil)
  }

  test("projects exact counterplay-restraint packet into a bounded row") {
    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = supportedCounterplayRestraintPacket()))

    assertEquals(rows.map(_.label), List("Counterplay restraint"))
    assertEquals(rows.head.text, "The checked line keeps the opponent's break restrained.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("projects FEN-backed counterplay-restraint exact-slice packet through the public row") {
    val restraintPlan =
      PlanHypothesis(
        planId = "fen_backed_counterplay_restraint",
        planName = "Counterplay restraint",
        rank = 1,
        score = 0.86,
        preconditions = List("The opponent's break resource can be held back."),
        executionSteps = List("Keep the restraint stable on the best-defense branch."),
        failureModes = List("The break resource is released too soon."),
        viability = PlanViability(0.82, "high", "best-defense branch keeps the resource denied"),
        themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
        subplanId = Some(PlanTaxonomy.PlanKind.ProphylaxisRestraint.id)
      )
    val baseCtx =
      topPvPracticalContext(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        playedMove = "a2a3",
        playedSan = "a3",
        line = List("a2a3", "f8e8"),
        scoreCp = 74
      )
    val counterplayCtx =
      baseCtx.copy(
        semantic =
          baseCtx.semantic.map(
            _.copy(
              preventedPlans =
                List(
                  PreventedPlanInfo(
                    planId = "deny_break_resource",
                    deniedSquares = Nil,
                    breakNeutralized = None,
                    mobilityDelta = -1,
                    counterplayScoreDrop = 140,
                    preventedThreatType = Some("counterplay"),
                    deniedResourceClass = Some("break"),
                    sourceScope = FactScope.Now,
                    citationLine = Some("The checked branch keeps the break resource unavailable.")
                  )
                )
            )
          ),
        mainStrategicPlans = List(restraintPlan),
        strategicPlanEvidence = StrategicPlanEvidenceTestSupport.probeBacked(List(restraintPlan))
      )
    val truthContract =
      safeTruthContract("a2a3").copy(verifiedPayoffAnchor = Some("denied_resource:break"))
    val surface =
      StrategyPackSurface.from(
        Some(
          StrategyPack(
            sideToMove = "white",
            pieceMoveRefs =
              List(
                StrategyPieceMoveRef(
                  ownerSide = "white",
                  piece = "pawn",
                  from = "a2",
                  target = "denied_resource:break",
                  idea = "keeps the break resource restrained",
                  evidence = List("top_pv_branch:a2a3 f8e8")
                )
              )
          )
        )
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(counterplayCtx, surface, Some(truthContract))
        .getOrElse(fail("expected counterplay-restraint exact-slice move-delta packet"))
    val packet = delta.packet
    val exactProof = PlayerFacingExactSliceProof.ProphylacticRestraint("denied_resource:break")
    val rows =
      noRankedSupportedLocalRows(
        localCtx = counterplayCtx,
        localInputs = inputs(packet = packet, claimText = "The move keeps the opponent's break restrained."),
        playedMove = "a2a3"
      )

    assertEquals(counterplayCtx.fen, "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23")
    assertEquals(packet.proofSource, ProofSourceId.ProphylacticMove.wireKey)
    assertEquals(packet.proofFamily, ProofFamilyId.CounterplayRestraint.wireKey)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assertEquals(packet.bestDefenseMove, Some("Rfe8"))
    assert(packet.bestDefenseBranchKey.nonEmpty, clue(packet))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(rows.map(_.label), List("Counterplay restraint"), clue(rows))
    assertEquals(rows.head.text, "The checked line keeps the opponent's break restrained.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("does not project counterplay-restraint prose tokens without typed exact resource proof") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs =
          inputs(
            packet =
              supportedCounterplayRestraintPacket(
                exactSliceProof = Some(PlayerFacingExactSliceProof.ProphylacticRestraint("counterplay window"))
              )
          )
      )

    assertEquals(rows, Nil)
  }

  test("does not project counterplay-restraint proof without matching resource terms") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs =
          inputs(
            packet =
              supportedCounterplayRestraintPacket(
                exactSliceProof = Some(PlayerFacingExactSliceProof.ProphylacticRestraint("denied_resource:route"))
              )
          )
      )

    assertEquals(rows, Nil)
  }

  test("suppresses counterplay-restraint row under tactical truth mode") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs = inputs(packet = supportedCounterplayRestraintPacket(), truthMode = PlayerFacingTruthMode.Tactical)
      )

    assertEquals(rows, Nil)
  }

  test("projects FEN-backed defender-trade exact-slice packet through the public row") {
    val defenderPlan =
      PlanHypothesis(
        planId = "fen_backed_defender_trade",
        planName = "Defender trade",
        rank = 1,
        score = 0.88,
        preconditions = List("The defender can be drawn onto the exchange square."),
        executionSteps = List("Trade on a3 and recapture to remove the defender."),
        failureModes = List("The defender remains attached to the target."),
        viability = PlanViability(0.84, "high", "top-PV branch removes the defender"),
        themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id,
        subplanId = Some(PlanTaxonomy.PlanKind.DefenderTrade.id)
      )
    val defenderCtx =
      withFocalTarget(
        topPvPracticalContext(
          fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
          playedMove = "c1a3",
          playedSan = "Ba3",
          line = List("c1a3", "f8a3", "b3a3"),
          scoreCp = 44
        ).copy(
          mainStrategicPlans = List(defenderPlan),
          strategicPlanEvidence = StrategicPlanEvidenceTestSupport.probeBacked(List(defenderPlan))
        ),
        "c5"
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(defenderCtx, StrategyPackSurface.from(None), Some(safeTruthContract("c1a3")))
        .getOrElse(fail("expected defender-trade exact-slice move-delta packet"))
    val packet = delta.packet
    val exactProof = PlayerFacingExactSliceProof.DefenderTrade("f8", "a3", "c5")
    val rows =
      noRankedSupportedLocalRows(
        localCtx = defenderCtx,
        localInputs =
          inputs(
            packet = packet,
            claimText = "The checked line trades on a3 to remove the defender from f8, loosening c5."
          ),
        playedMove = "c1a3"
      )

    assertEquals(defenderCtx.fen, "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17")
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.DefenderTradeProofSource)
    assertEquals(packet.proofFamily, ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.DefenderTrade).get.wireKey)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assertEquals(packet.bestDefenseMove, Some("Bxa3"))
    assert(packet.bestDefenseBranchKey.nonEmpty, clue(packet))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(rows.map(_.label), List("Defender trade"), clue(rows))
    assertEquals(rows.head.text, "The checked line trades on a3 to remove the defender from f8, loosening c5.")
    assertEquals(
      rows.head.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some(MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade),
          target = Some("c5")
        )
      ),
      clue(rows)
    )
  }

  test("projects PGN-backed bad-piece-liquidation exact-slice packet through the public row") {
    val badPieceCtx =
      topPvPracticalContext(
        fen = "r2qr1k1/pp3pn1/2pb2pp/3pB3/NP1P4/3QP2P/P4PP1/1RR3K1 w - - 1 22",
        playedMove = "e5d6",
        playedSan = "Bxd6",
        line = List("e5d6", "d8d6", "b4b5"),
        scoreCp = 20
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(badPieceCtx, StrategyPackSurface.from(None), Some(safeTruthContract("e5d6")))
        .getOrElse(fail("expected bad-piece-liquidation exact-slice move-delta packet"))
    val packet = delta.packet
    val exactProof = PlayerFacingExactSliceProof.BadPieceLiquidation("e5", "d6")
    val rows =
      noRankedSupportedLocalRows(
        localCtx = badPieceCtx,
        localInputs =
          inputs(
            packet = packet,
            claimText = "The checked line trades on d6 to clear the bad piece from e5."
          ),
        playedMove = "e5d6"
      )

    assertEquals(badPieceCtx.fen, "r2qr1k1/pp3pn1/2pb2pp/3pB3/NP1P4/3QP2P/P4PP1/1RR3K1 w - - 1 22")
    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing)
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource)
    assertEquals(packet.proofFamily, ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.BadPieceLiquidation).get.wireKey)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assertEquals(packet.bestDefenseMove, Some("Qxd6"))
    assert(packet.bestDefenseBranchKey.nonEmpty, clue(packet))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(rows.map(_.label), List("Bad piece trade"), clue(rows))
    assertEquals(rows.head.text, "The checked line trades on d6 to clear the bad piece from e5.")
    assertEquals(
      rows.head.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some(MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation),
          target = Some("d6")
        )
      ),
      clue(rows)
    )
  }

  test("projects FEN-backed bad-piece-liquidation pilot exact-slice packet through the public row") {
    val badPieceCtx =
      topPvPracticalContext(
        fen = "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1",
        playedMove = "c1a3",
        playedSan = "Ba3",
        line = List("c1a3", "e7f7", "a3f8", "f7f8"),
        scoreCp = 38
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(badPieceCtx, StrategyPackSurface.from(None), Some(safeTruthContract("c1a3")))
        .getOrElse(fail("expected bad-piece-liquidation pilot exact-slice move-delta packet"))
    val packet = delta.packet
    val exactProof = PlayerFacingExactSliceProof.BadPieceLiquidation("c1", "f8")
    val rows =
      noRankedSupportedLocalRows(
        localCtx = badPieceCtx,
        localInputs =
          inputs(
            packet = packet,
            claimText = "The checked line trades on f8 to clear the bad piece from c1."
          ),
        playedMove = "c1a3"
      )

    assertEquals(badPieceCtx.fen, "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1")
    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing)
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource)
    assertEquals(packet.proofFamily, ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.BadPieceLiquidation).get.wireKey)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assertEquals(packet.bestDefenseMove, Some("Kf7"))
    assert(packet.bestDefenseBranchKey.nonEmpty, clue(packet))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(rows.map(_.label), List("Bad piece trade"), clue(rows))
    assertEquals(rows.head.text, "The checked line trades on f8 to clear the bad piece from c1.")
    assertEquals(
      rows.head.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some(MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation),
          target = Some("f8")
        )
      ),
      clue(rows)
    )
  }

  test("projects PGN-backed queen-trade exact-slice packet through the public row") {
    val queenTradeCtx =
      topPvPracticalContext(
        fen = "r1bqk2r/1p1p1ppp/p1n1pn2/8/1bPNP3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 5 8",
        playedMove = "d4c6",
        playedSan = "Nxc6",
        line = List("d4c6", "d7c6", "d3d8", "e8d8"),
        scoreCp = 20
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(queenTradeCtx, StrategyPackSurface.from(None), Some(safeTruthContract("d4c6")))
        .getOrElse(fail("expected queen-trade exact-slice move-delta packet"))
    val packet = delta.packet
    val exactProof = PlayerFacingExactSliceProof.QueenTradeShield(List("d4c6", "d7c6", "d3d8", "e8d8"))
    val rows =
      noRankedSupportedLocalRows(
        localCtx = queenTradeCtx,
        localInputs =
          inputs(
            packet = packet,
            claimText = "This exchange moves the game into the queenless branch."
          ),
        playedMove = "d4c6"
      )

    assertEquals(
      queenTradeCtx.fen,
      "r1bqk2r/1p1p1ppp/p1n1pn2/8/1bPNP3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 5 8"
    )
    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing)
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.QueenTradeShieldProofSource)
    assertEquals(packet.proofFamily, ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.QueenTradeShield).get.wireKey)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assertEquals(packet.bestDefenseMove, Some("dxc6"))
    assert(packet.bestDefenseBranchKey.nonEmpty, clue(packet))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Ambiguous)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.BestDefenseOnly)
    assertEquals(rows.map(_.label), List("Queen trade"), clue(rows))
    assertEquals(rows.head.text, "This exchange moves the game into the queenless branch.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects Carlsen-Anand queen-trade completion exact-slice packet through the public row") {
    val queenTradeCtx =
      topPvPracticalContext(
        fen = "r1bqk2r/1p3ppp/p1p1pn2/8/1bP1P3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 0 9",
        playedMove = "d3d8",
        playedSan = "Qxd8+",
        line = List("d3d8", "e8d8", "e4e5", "f6d7"),
        scoreCp = 20
      )
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(queenTradeCtx, StrategyPackSurface.from(None), Some(safeTruthContract("d3d8")))
        .getOrElse(fail("expected queen-trade completion exact-slice move-delta packet"))
    val packet = delta.packet
    val exactProof = PlayerFacingExactSliceProof.QueenTradeShield(List("d3d8", "e8d8"))
    val rows =
      noRankedSupportedLocalRows(
        localCtx = queenTradeCtx,
        localInputs =
          inputs(
            packet = packet,
            claimText = "This exchange moves the game into the queenless branch."
          ),
        playedMove = "d3d8"
      )

    assertEquals(
      queenTradeCtx.fen,
      "r1bqk2r/1p3ppp/p1p1pn2/8/1bP1P3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 0 9"
    )
    assertEquals(queenTradeCtx.playedMove, Some("d3d8"))
    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing)
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.QueenTradeShieldProofSource)
    assertEquals(packet.proofFamily, ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.QueenTradeShield).get.wireKey)
    assertEquals(packet.proofPathWitness.exactSliceProof, Some(exactProof))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(packet, exactProof), clue(packet))
    assertEquals(packet.bestDefenseMove, Some("Kxd8"))
    assert(packet.bestDefenseBranchKey.nonEmpty, clue(packet))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Ambiguous)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.BestDefenseOnly)
    assertEquals(rows.map(_.label), List("Queen trade"), clue(rows))
    assertEquals(rows.head.text, "This exchange moves the game into the queenless branch.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects admitted exchange ownership packets into bounded rows") {
    admittedExchangeOwnershipCases.foreach { case (packet, expectedLabel, expectedText) =>
      val rows =
        noRankedSupportedLocalRows(localInputs = inputs(packet = packet, claimText = expectedText))

      assertEquals(rows.map(_.label), List(expectedLabel), clue(rows))
      assertEquals(rows.head.text, expectedText, clue(rows))
      assertEquals(
        rows.head.authority,
        Some(expectedExchangeOwnershipAuthority(expectedLabel)),
        clue(rows)
      )
    }
  }

  test("does not project exchange ownership labels without required witness shape") {
    val cases =
      List(
        "no defender structure" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = Nil
          ),
        "generic defender shape" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = List("defender_trade_branch", "defender_removed:c5-d4", "target_unlocked:e5")
          ),
        "generic bad-piece shape" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            structureTransitionTerms = List("bad_piece_liquidation_branch", "bad_piece_removed:c8-e6")
          ),
        "branch-only defender shape" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:e5")
          ),
        "branch-only bad-piece shape" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            structureTransitionTerms = List("bad_piece_liquidation_branch", "bad_piece:c8", "exchange_square:e6")
          ),
        "malformed defender square" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = List("defender_trade_branch", "defender:c-file", "exchange_square:d4", "defended_target:e5")
          ),
        "malformed bad-piece square" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            structureTransitionTerms = List("bad_piece_liquidation_branch", "bad_piece:c8", "exchange_square:e-file")
          ),
        "mismatched defender proof" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:e5"),
            exactSliceProof = Some(PlayerFacingExactSliceProof.DefenderTrade("c5", "d4", "f7"))
          ),
        "mismatched bad-piece proof" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            structureTransitionTerms = List("bad_piece_liquidation_branch", "bad_piece:c8", "exchange_square:e6"),
            exactSliceProof = Some(PlayerFacingExactSliceProof.BadPieceLiquidation("c8", "d5"))
          ),
        "missing defender branch" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            sameBranchState = PlayerFacingSameBranchState.Missing,
            structureTransitionTerms =
              List(
                "defender_trade_branch",
                "defender:c5",
                "exchange_square:d4",
                "defended_target:e5",
                "defender_removed:c5-d4",
                "target_unlocked:e5"
              )
          ),
        "unstable bad-piece branch" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            persistence = PlayerFacingClaimPersistence.BestDefenseOnly,
            structureTransitionTerms =
              List(
                "bad_piece_liquidation_branch",
                "bad_piece:c8",
                "exchange_square:e6",
                "bad_piece_removed:c8-e6"
              )
          ),
        "missing queen proof" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.QueenTradeShield,
            proofSource = PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
            structureTransitionTerms = Nil,
            exactSliceProof = None
          ),
        "mismatched queen terms" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.QueenTradeShield,
            proofSource = PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
            structureTransitionTerms = List("d4c6", "d7c6", "queenless_branch"),
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.QueenTradeShield(List("d4c6", "d7c6", "d3d8", "e8d8"))
              )
          )
      )

    cases.foreach { case (name, packet) =>
      val rows =
        noRankedSupportedLocalRows(localInputs = inputs(packet = packet))

      assertEquals(rows, Nil, clue(name -> rows))
    }
  }

  test("suppresses exchange ownership row under tactical truth mode") {
    val rows =
      noRankedSupportedLocalRows(
        localInputs =
          inputs(
            packet =
              supportedExchangePacket(
                planKind = PlanTaxonomy.PlanKind.DefenderTrade,
                proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
                structureTransitionTerms =
                  List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:e5")
              ),
            truthMode = PlayerFacingTruthMode.Tactical
          )
      )

    assertEquals(rows, Nil)
  }

  test("suppresses central_break_timing row when tactical truth mode vetoes SupportedLocal") {
    val truth = tacticalTruthContract(playedMove = "e4e5")
    val (centralCtx, centralInputs, centralRanked) = centralScene(truthContract = Some(truth))

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(truth)
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
  }

  test("does not project plan-only central_break_timing review evidence") {
    val lines =
      List(
        VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 72, depth = 18),
        VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 28, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "b1c3", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
  }

  test("projects central_break_timing when the board-backed break is not best-line-exclusive") {
    val lines =
      List(
        VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 68, depth = 18),
        VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 31, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the d4-d5 break at this moment."
    )
  }

  test("projects central_break_timing without requiring a two-move branch key") {
    val lines =
      List(
        VariationLine(List("d4d5"), scoreCp = 0, depth = 18),
        VariationLine(List("b1c3"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the d4-d5 break at this moment."
    )
  }

  test("projects played direct central_break_timing when top PV omits that break") {
    val lines =
      List(
        VariationLine(List("b1c3", "e8g8", "h2h3"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the d4-d5 break at this moment."
    )
  }

  test("projects diagonal central captures as central liquidation rows, not central break") {
    val diagonalCaptureFen =
      "4k3/8/8/3p4/4P3/8/8/4K3 b - - 0 1"
    val lines =
      List(
        VariationLine(List("d5e4"), scoreCp = 0, depth = 18),
        VariationLine(List("e8d7"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = diagonalCaptureFen, ply = 2, playedMove = "d5e4", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
    assertEquals(rows.map(_.label), List("Central liquidation"))
    assertEquals(rows.head.text, "The move releases central tension through d5-e4.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralLiquidation, token = Some("...d5-e4")))
    )
  }

  test("prioritizes relation practical rows over central liquidation fallback") {
    val diagonalCaptureFen =
      "r2q1rk1/pp2npb1/2p1b1pp/3pB3/3PN3/1BP2N2/PP3PPP/R2Q1RK1 b - - 0 15"
    val lines =
      List(
        VariationLine(List("d5e4"), scoreCp = 0, depth = 18),
        VariationLine(List("g7e5"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = diagonalCaptureFen, ply = 30, playedMove = "d5e4", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Overloaded defender"), clue(rows))
    assert(!rows.exists(_.label == "Central liquidation"), clue(rows))
    assertEquals(rows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
  }

  test("projects prep or challenge pawn moves as central challenge rows, not central break") {
    val prepFen =
      "rnbqkbnr/pp1ppppp/8/2p1P3/2B5/5Q2/PPPP1PPP/RNB1K1NR b KQkq - 2 4"
    val lines =
      List(
        VariationLine(List("d7d6"), scoreCp = 0, depth = 18),
        VariationLine(List("b8c6"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = prepFen, ply = 8, playedMove = "d7d6", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
    assertEquals(rows.map(_.label), List("Central challenge"))
    assertEquals(rows.head.text, "The move challenges the center through d7-d6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralChallenge, token = Some("...d7-d6")))
    )
  }

  test("projects non-central opening pawn breaks as played pawn-break rows") {
    val cases =
      List(
        (
          "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
          2,
          "c7c5",
          List(VariationLine(List("c7c5", "g1f3"), scoreCp = 0, depth = 18)),
          "The checked line plays the c7-c5 pawn break."
        ),
        (
          "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
          3,
          "f2f4",
          List(VariationLine(List("f2f4", "e5f4"), scoreCp = 0, depth = 18)),
          "The checked line plays the f2-f4 pawn break, but king safety still needs care."
        )
      )

    cases.foreach { case (fen, ply, playedMove, lines, expectedText) =>
      val (openingCtx, openingInputs, openingRanked) =
        centralScene(fen = fen, ply = ply, playedMove = playedMove, lines = lines, phase = "opening")

      val rows =
        MoveReviewSupportedLocalSurfaceRows.build(
          ctx = openingCtx,
          inputs = openingInputs,
          rankedPlans = openingRanked,
          truthContract = Some(safeTruthContract(playedMove))
        )

      assertEquals(rows.map(_.label), List("Pawn break"), clues(playedMove, rows))
      assertEquals(rows.head.text, expectedText, clue(rows))
      assertEquals(
        rows.head.authority,
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
        clue(rows)
      )
    }
  }

  test("does not project pawn-break prose without a top-PV played-move witness") {
    val injectedCtx =
      ctx.copy(
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        playedMove = Some("c7c5"),
        engineEvidence = Some(EngineEvidence(depth = 18, variations = List(VariationLine(List("d7d6", "g1f3"), scoreCp = 0, depth = 18)))),
        openingGoalEvaluation =
          Some(
            OpeningGoals.Evaluation(
              goalName = "Sicilian c-pawn Challenge",
              status = OpeningGoals.Status.Achieved,
              supportedEvidence = List("c-pawn challenge reached", "e4 contested"),
              missingEvidence = Nil,
              confidence = 0.92
            )
          )
      )

    val rows = topPvPracticalRows(injectedCtx, "c7c5")

    assert(!rows.exists(_.label == "Pawn break"), clue(rows))
  }

  test("does not project pawn-break prose when the goal does not match the played pawn witness") {
    val injectedCtx =
      ctx.copy(
        fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
        playedMove = Some("f2f4"),
        engineEvidence = Some(EngineEvidence(depth = 18, variations = List(VariationLine(List("f2f4", "e5f4"), scoreCp = 0, depth = 18)))),
        openingGoalEvaluation =
          Some(
            OpeningGoals.Evaluation(
              goalName = "Sicilian c-pawn Challenge",
              status = OpeningGoals.Status.Achieved,
              supportedEvidence = List("c-pawn challenge reached", "e4 contested"),
              missingEvidence = Nil,
              confidence = 0.92
            )
          )
      )

    val rows = topPvPracticalRows(injectedCtx, "f2f4")

    assert(!rows.exists(_.label == "Pawn break"), clue(rows))
  }

  test("projects board-backed opening outposts as knight outpost rows") {
    val dutchOutpostFen =
      "rnbqkb1r/ppp1p1pp/5n2/3p1p2/2PP4/8/PP2PPPP/RNBQKBNR b KQkq - 2 4"
    val lines =
      List(
        VariationLine(List("f6e4", "g1f3"), scoreCp = 0, depth = 18)
      )
    val (openingCtx, openingInputs, openingRanked) =
      centralScene(fen = dutchOutpostFen, ply = 8, playedMove = "f6e4", lines = lines, phase = "opening")

    assertEquals(openingCtx.openingGoalEvaluation.map(_.goalName), Some("Dutch E4 Outpost"))

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = openingCtx,
        inputs = openingInputs,
        rankedPlans = openingRanked,
        truthContract = Some(safeTruthContract("f6e4"))
    )

    assertEquals(rows.map(_.label), List("Knight outpost"), clue(rows))
    assertEquals(rows.head.text, "The checked line puts the knight on the pawn-supported e4 outpost square.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e4"))),
      clue(rows)
    )
  }

  test("does not project opening outpost prose without a played knight witness") {
    val injectedCtx =
      ctx.copy(
        fen = "rnbqkb1r/ppp1p1pp/5n2/3p1p2/2PP4/6P1/PP2PPBP/RNBQK1NR b KQkq - 2 4",
        playedMove = Some("d5d4"),
        openingGoalEvaluation =
          Some(
            OpeningGoals.Evaluation(
              goalName = "Dutch E4 Outpost",
              status = OpeningGoals.Status.Achieved,
              supportedEvidence = List("e4 outpost occupied"),
              missingEvidence = Nil,
              confidence = 0.9
            )
          )
      )

    val rows = topPvPracticalRows(injectedCtx, "d5d4")

    assert(!rows.exists(_.label == "Knight outpost"), clue(rows))
  }

  test("does not project opening outpost prose without a top-PV played-move witness") {
    val injectedCtx =
      ctx.copy(
        fen = "rnbqkb1r/ppp1p1pp/5n2/3p1p2/2PP4/6P1/PP2PPBP/RNBQK1NR b KQkq - 2 4",
        playedMove = Some("f6e4"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("d5d4", "g1f3"), scoreCp = 0, depth = 18))
            )
          ),
        openingGoalEvaluation =
          Some(
            OpeningGoals.Evaluation(
              goalName = "Dutch E4 Outpost",
              status = OpeningGoals.Status.Achieved,
              supportedEvidence = List("e4 outpost occupied"),
              missingEvidence = Nil,
              confidence = 0.9
            )
          )
      )

    val rows = topPvPracticalRows(injectedCtx, "f6e4")

    assertEquals(rows, Nil)
  }

  test("does not project opening outpost prose when an enemy minor can remove the knight") {
    val injectedCtx =
      ctx.copy(
        fen = "rnbqk2r/ppp1ppbp/5np1/3p4/3P1B2/2N2N1P/PPP1PPP1/R2QKB1R b KQkq - 3 5",
        playedMove = Some("f6e4"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("f6e4", "c3e4"), scoreCp = -18, depth = 18))
            )
          ),
        openingGoalEvaluation =
          Some(
            OpeningGoals.Evaluation(
              goalName = "Dutch E4 Outpost",
              status = OpeningGoals.Status.Achieved,
              supportedEvidence = List("e4 outpost occupied"),
              missingEvidence = Nil,
              confidence = 0.9
            )
          )
      )

    val rows = topPvPracticalRows(injectedCtx, "f6e4")

    assert(!rows.exists(_.label == "Knight outpost"), clue(rows))
  }

  test("projects top-PV rook-pawn hook creation as a bounded practical row") {
    val flankCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/7p/8/7P/8/6K1 w - - 0 1",
        playedMove = "h3h4",
        playedSan = "h4",
        line = List("h3h4", "g8f8"),
        scoreCp = 28
      )

    val rows = topPvPracticalRows(flankCtx, "h3h4")

    assertEquals(rows.map(_.label), List("Hook creation"), clue(rows))
    assertEquals(rows.head.text, "The checked rook-pawn move creates a flank hook on h4.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects top-PV rook-pawn march as a bounded practical row") {
    val flankCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/7P/8/6K1 w - - 0 1",
        playedMove = "h3h4",
        playedSan = "h4",
        line = List("h3h4", "g8f8"),
        scoreCp = 28
      )

    val rows = topPvPracticalRows(flankCtx, "h3h4")

    assertEquals(rows.map(_.label), List("Rook-pawn march"), clue(rows))
    assertEquals(rows.head.text, "The checked line advances the rook pawn to h4 for flank space.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project rook-pawn hook prose without a top-PV played-move witness") {
    val flankCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/7p/8/7P/8/6K1 w - - 0 1",
        playedMove = "h3h4",
        playedSan = "h4",
        line = List("g1f1", "g8f8"),
        scoreCp = 28
      )

    val rows = topPvPracticalRows(flankCtx, "h3h4")

    assertEquals(rows, Nil)
  }

  test("projects top-PV rook lift as a bounded practical row") {
    val liftCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/8/6KR w - - 0 1",
        playedMove = "h1h3",
        playedSan = "Rh3",
        line = List("h1h3", "g8f8"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(liftCtx, "h1h3")

    assertEquals(rows.map(_.label), List("Rook lift"), clue(rows))
    assertEquals(rows.head.text, "The checked line lifts the rook to h3 as attacking infrastructure.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project rook lift prose without a top-PV played-move witness") {
    val liftCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/8/6KR w - - 0 1",
        playedMove = "h1h3",
        playedSan = "Rh3",
        line = List("g1f1", "g8f8"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(liftCtx, "h1h3")

    assertEquals(rows, Nil)
  }

  test("does not project back-rank rook shuffles as rook lift prose") {
    val shuffleCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/8/R3K3 w - - 0 1",
        playedMove = "a1d1",
        playedSan = "Rd1",
        line = List("a1d1", "g8f8"),
        scoreCp = 22
      )

    val rows = topPvPracticalRows(shuffleCtx, "a1d1")

    assertEquals(rows, Nil)
  }

  test("projects top-PV knight outposts as a bounded practical row") {
    val outpostCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/3P4/5N2/8/6K1 w - - 0 1",
        playedMove = "f3e5",
        playedSan = "Ne5",
        line = List("f3e5", "g8f8"),
        scoreCp = 42
      )

    val rows = topPvPracticalRows(outpostCtx, "f3e5")

    assertEquals(rows.map(_.label), List("Knight outpost"), clue(rows))
    assertEquals(rows.head.text, "The checked line puts the knight on the pawn-supported e5 outpost square.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5"))),
      clue(rows)
    )
  }

  test("does not project knight-outpost prose without a top-PV played-move witness") {
    val outpostCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/3P4/5N2/8/6K1 w - - 0 1",
        playedMove = "f3e5",
        playedSan = "Ne5",
        line = List("g1f1", "g8f8"),
        scoreCp = 42
      )

    val rows = topPvPracticalRows(outpostCtx, "f3e5")

    assertEquals(rows, Nil)
  }

  test("does not project unsupported knight landings as outpost prose") {
    val unsupportedCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/5N2/8/6K1 w - - 0 1",
        playedMove = "f3e5",
        playedSan = "Ne5",
        line = List("f3e5", "g8f8"),
        scoreCp = 28
      )

    val rows = topPvPracticalRows(unsupportedCtx, "f3e5")

    assertEquals(rows, Nil)
  }

  test("does not project pawn-attacked knight landings as outpost prose") {
    val attackedCtx =
      topPvPracticalContext(
        fen = "6k1/8/5p2/8/3P4/5N2/8/6K1 w - - 0 1",
        playedMove = "f3e5",
        playedSan = "Ne5",
        line = List("f3e5", "g8f8"),
        scoreCp = 30
      )

    val rows = topPvPracticalRows(attackedCtx, "f3e5")

    assertEquals(rows, Nil)
  }

  test("does not project minor-attacked transient knight landings as outpost prose") {
    val transientCtx =
      topPvPracticalContext(
        fen = "rnbqk2r/ppp1ppbp/5np1/3p4/3P1B2/2N2N1P/PPP1PPP1/R2QKB1R b KQkq - 3 5",
        playedMove = "f6e4",
        playedSan = "Ne4",
        line = List("f6e4", "e2e3", "e4c3", "b2c3", "c7c5"),
        scoreCp = -18
      )

    val rows = topPvPracticalRows(transientCtx, "f6e4")

    assert(!rows.exists(_.label == "Knight outpost"), clue(rows))
  }

  test("projects top-PV bishop-pair retention after captures as a bounded practical row") {
    val bishopPairCtx =
      topPvPracticalContext(
        fen = "k7/5n2/8/8/2B5/8/6B1/6K1 w - - 0 1",
        playedMove = "c4f7",
        playedSan = "Bxf7+",
        line = List("c4f7", "a8b8"),
        scoreCp = 46
      )

    val rows = topPvPracticalRows(bishopPairCtx, "c4f7")

    assertEquals(rows.map(_.label), List("Bishop pair"), clue(rows))
    assertEquals(rows.head.text, "The checked capture keeps the bishop pair on the board.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project bishop-pair prose without a top-PV played-move witness") {
    val bishopPairCtx =
      topPvPracticalContext(
        fen = "k7/5n2/8/8/2B5/8/6B1/6K1 w - - 0 1",
        playedMove = "c4f7",
        playedSan = "Bxf7+",
        line = List("g1f1", "a8b8"),
        scoreCp = 46
      )

    val rows = topPvPracticalRows(bishopPairCtx, "c4f7")

    assertEquals(rows, Nil)
  }

  test("does not project bishop-pair prose when the mover has only one bishop after the capture") {
    val singleBishopCtx =
      topPvPracticalContext(
        fen = "k7/5n2/8/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = "c4f7",
        playedSan = "Bxf7+",
        line = List("c4f7", "a8b8"),
        scoreCp = 22
      )

    val rows = topPvPracticalRows(singleBishopCtx, "c4f7")

    assertEquals(rows, Nil)
  }

  test("does not project bishop-pair prose when both sides keep the bishop pair") {
    val mirroredPairCtx =
      topPvPracticalContext(
        fen = "k7/1b3n1b/8/8/2B5/8/6B1/6K1 w - - 0 1",
        playedMove = "c4f7",
        playedSan = "Bxf7+",
        line = List("c4f7", "a8b8"),
        scoreCp = 28
      )

    val rows = topPvPracticalRows(mirroredPairCtx, "c4f7")

    assertEquals(rows, Nil)
  }

  test("projects top-PV opposite-colored bishop end states as a bounded practical row") {
    val oppositeBishopCtx =
      topPvPracticalContext(
        fen = "k7/5nb1/8/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = "c4f7",
        playedSan = "Bxf7+",
        line = List("c4f7", "a8b8"),
        scoreCp = 16
      )

    val rows = topPvPracticalRows(oppositeBishopCtx, "c4f7")

    assertEquals(rows.map(_.label), List("Opposite-color bishops"), clue(rows))
    assertEquals(rows.head.text, "The checked capture leaves opposite-colored bishops on the board.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project opposite-colored bishop prose without a top-PV played-move witness") {
    val oppositeBishopCtx =
      topPvPracticalContext(
        fen = "k7/5nb1/8/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = "c4f7",
        playedSan = "Bxf7+",
        line = List("g1f1", "a8b8"),
        scoreCp = 16
      )

    val rows = topPvPracticalRows(oppositeBishopCtx, "c4f7")

    assertEquals(rows, Nil)
  }

  test("does not project same-color bishop end states as opposite-colored bishop prose") {
    val sameColorBishopCtx =
      topPvPracticalContext(
        fen = "k7/5n2/2b5/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = "c4f7",
        playedSan = "Bxf7+",
        line = List("c4f7", "a8b8"),
        scoreCp = 12
      )

    val rows = topPvPracticalRows(sameColorBishopCtx, "c4f7")

    assertEquals(rows, Nil)
  }

  test("does not project opposite-colored bishop prose while major pieces remain") {
    val majorPieceCtx =
      topPvPracticalContext(
        fen = "k6r/5nb1/8/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = "c4f7",
        playedSan = "Bxf7+",
        line = List("c4f7", "a8b8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(majorPieceCtx, "c4f7")

    assertEquals(rows, Nil)
  }

  test("projects top-PV seventh-rank rook entries as a bounded practical row") {
    val seventhCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/R7/6K1 w - - 0 1",
        playedMove = "a2a7",
        playedSan = "Ra7",
        line = List("a2a7", "g8f8"),
        scoreCp = 58
      )

    val rows = topPvPracticalRows(seventhCtx, "a2a7")

    assertEquals(rows.map(_.label), List("Seventh-rank entry"), clue(rows))
    assertEquals(rows.head.text, "The checked line puts the rook on the seventh rank at a7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project seventh-rank entry prose without a top-PV played-move witness") {
    val seventhCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/R7/6K1 w - - 0 1",
        playedMove = "a2a7",
        playedSan = "Ra7",
        line = List("g1f1", "g8f8"),
        scoreCp = 58
      )

    val rows = topPvPracticalRows(seventhCtx, "a2a7")

    assertEquals(rows, Nil)
  }

  test("does not project non-seventh-rank rook moves as seventh-rank entry prose") {
    val sixthCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/R7/6K1 w - - 0 1",
        playedMove = "a2a6",
        playedSan = "Ra6",
        line = List("a2a6", "g8f8"),
        scoreCp = 42
      )

    val rows = topPvPracticalRows(sixthCtx, "a2a6")

    assertEquals(rows, Nil)
  }

  test("projects top-PV rook-behind-passer entries as a bounded practical row") {
    val passerRookCtx =
      topPvPracticalContext(
        fen = "6k1/8/1P6/8/8/R7/8/6K1 w - - 0 1",
        playedMove = "a3b3",
        playedSan = "Rb3",
        line = List("a3b3", "g8f8"),
        scoreCp = 82
      )

    val rows = topPvPracticalRows(passerRookCtx, "a3b3")

    assertEquals(rows.map(_.label), List("Rook behind passer"), clue(rows))
    assertEquals(rows.head.text, "The checked line places the rook behind the passed pawn on b6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project rook-behind-passer prose without a top-PV played-move witness") {
    val passerRookCtx =
      topPvPracticalContext(
        fen = "6k1/8/1P6/8/8/R7/8/6K1 w - - 0 1",
        playedMove = "a3b3",
        playedSan = "Rb3",
        line = List("g1f1", "g8f8"),
        scoreCp = 82
      )

    val rows = topPvPracticalRows(passerRookCtx, "a3b3")

    assertEquals(rows, Nil)
  }

  test("does not project rook-behind-passer prose when the rook is in front of the passer") {
    val frontRookCtx =
      topPvPracticalContext(
        fen = "6k1/R7/1P6/8/8/8/8/6K1 w - - 0 1",
        playedMove = "a7b7",
        playedSan = "Rb7",
        line = List("a7b7", "g8f8"),
        scoreCp = 42
      )

    val rows = topPvPracticalRows(frontRookCtx, "a7b7")

    assertEquals(rows, Nil)
  }

  test("projects top-PV knight blockades of passed pawns as a bounded practical row") {
    val blockadeCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/3p1N2/8/8/6K1 w - - 0 1",
        playedMove = "f4d3",
        playedSan = "Nd3",
        line = List("f4d3", "g8f8"),
        scoreCp = 36
      )

    val rows = topPvPracticalRows(blockadeCtx, "f4d3")

    assertEquals(rows.map(_.label), List("Passer blockade"), clue(rows))
    assertEquals(rows.head.text, "The checked line blockades the passed pawn on d4 with the knight.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project passer-blockade prose without a top-PV played-move witness") {
    val blockadeCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/3p1N2/8/8/6K1 w - - 0 1",
        playedMove = "f4d3",
        playedSan = "Nd3",
        line = List("g1f1", "g8f8"),
        scoreCp = 36
      )

    val rows = topPvPracticalRows(blockadeCtx, "f4d3")

    assertEquals(rows, Nil)
  }

  test("does not project passer-blockade prose when the pawn is not passed") {
    val blockadeCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/3p1N2/8/2P5/6K1 w - - 0 1",
        playedMove = "f4d3",
        playedSan = "Nd3",
        line = List("f4d3", "g8f8"),
        scoreCp = 20
      )

    val rows = topPvPracticalRows(blockadeCtx, "f4d3")

    assertEquals(rows, Nil)
  }

  test("does not project knight moves away from the passer stop-square as blockade prose") {
    val blockadeCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/3p1N2/8/8/6K1 w - - 0 1",
        playedMove = "f4e2",
        playedSan = "Ne2",
        line = List("f4e2", "g8f8"),
        scoreCp = 24
      )

    val rows = topPvPracticalRows(blockadeCtx, "f4e2")

    assertEquals(rows, Nil)
  }

  test("projects top-PV open-file major-piece entries as a bounded practical row") {
    val fileCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/R7/8/6K1 w - - 0 1",
        playedMove = "a3e3",
        playedSan = "Re3",
        line = List("a3e3", "g8f8"),
        scoreCp = 44
      )

    val rows = topPvPracticalRows(fileCtx, "a3e3")

    assertEquals(rows.map(_.label), List("File entry"), clue(rows))
    assertEquals(rows.head.text, "The checked line places the rook on the open e-file.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects top-PV semi-open-file major-piece entries as a bounded practical row") {
    val fileCtx =
      topPvPracticalContext(
        fen = "6k1/4p3/8/8/8/R7/8/6K1 w - - 0 1",
        playedMove = "a3e3",
        playedSan = "Re3",
        line = List("a3e3", "g8f8"),
        scoreCp = 48
      )

    val rows = topPvPracticalRows(fileCtx, "a3e3")

    assertEquals(rows.map(_.label), List("File entry"), clue(rows))
    assertEquals(rows.head.text, "The checked line places the rook on the semi-open e-file.")
  }

  test("does not project file-entry prose without a top-PV played-move witness") {
    val fileCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/R7/8/6K1 w - - 0 1",
        playedMove = "a3e3",
        playedSan = "Re3",
        line = List("g1f1", "g8f8"),
        scoreCp = 44
      )

    val rows = topPvPracticalRows(fileCtx, "a3e3")

    assertEquals(rows, Nil)
  }

  test("does not project file-entry prose when the destination file still has an own pawn") {
    val blockedCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/R7/4P3/6K1 w - - 0 1",
        playedMove = "a3e3",
        playedSan = "Re3",
        line = List("a3e3", "g8f8"),
        scoreCp = 22
      )

    val rows = topPvPracticalRows(blockedCtx, "a3e3")

    assertEquals(rows, Nil)
  }

  test("projects top-PV x-ray pressure as a bounded practical row") {
    val xrayCtx =
      topPvPracticalContext(
        fen = "k7/8/6q1/5n2/8/8/8/1B5K w - - 0 1",
        playedMove = "b1e4",
        playedSan = "Be4",
        line = List("b1e4", "a8b8"),
        scoreCp = 42
      )

    val rows = topPvPracticalRows(xrayCtx, "b1e4")

    assertEquals(rows.map(_.label), List("X-ray pressure"), clue(rows))
    assertEquals(rows.head.text, "The checked line sets x-ray pressure from the bishop on e4 through f5 toward g6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project x-ray prose without a target beyond the blocker") {
    val blockedXrayCtx =
      topPvPracticalContext(
        fen = "k7/8/8/5n2/8/8/8/1B5K w - - 0 1",
        playedMove = "b1e4",
        playedSan = "Be4",
        line = List("b1e4", "a8b8"),
        scoreCp = 42
      )

    val rows = topPvPracticalRows(blockedXrayCtx, "b1e4")

    assertEquals(rows, Nil)
  }

  test("projects top-PV back-rank mates as bounded practical rows") {
    val mateCtx =
      topPvPracticalContext(
        fen = "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1",
        playedMove = "e1e8",
        playedSan = "Re8#",
        line = List("e1e8"),
        scoreCp = 9999
      )

    val rows = topPvPracticalRows(mateCtx, "e1e8")

    assertEquals(rows.map(_.label), List("Back-rank mate"), clue(rows))
    assertEquals(rows.head.text, "The checked line ends in back-rank mate on g8 after e1e8.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project back-rank mate prose without a top-PV played-move witness") {
    val mateCtx =
      topPvPracticalContext(
        fen = "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1",
        playedMove = "e1e8",
        playedSan = "Re8#",
        line = List("g1f1", "e1e8"),
        scoreCp = 9999
      )

    val rows = topPvPracticalRows(mateCtx, "e1e8")

    assertEquals(rows, Nil)
  }

  test("projects named top-PV mate patterns before broad practical rows") {
    val mateNetCtx =
      topPvPracticalContext(
        fen = "6rk/6pp/4P2N/8/8/8/8/6K1 w - - 0 1",
        playedMove = "h6f7",
        playedSan = "Nf7#",
        line = List("h6f7"),
        scoreCp = 9999
      )

    val rows = topPvPracticalRows(mateNetCtx, "h6f7")

    assertEquals(rows.map(_.label), List("Smothered mate"), clue(rows))
    assertEquals(rows.head.text, "The checked line ends in smothered mate on h8 after h6f7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project mate-net prose without a top-PV played-move witness") {
    val mateNetCtx =
      topPvPracticalContext(
        fen = "6rk/6pp/7N/8/8/8/8/6K1 w - - 0 1",
        playedMove = "h6f7",
        playedSan = "Nf7#",
        line = List("g1f1", "h6f7"),
        scoreCp = 9999
      )

    val rows = topPvPracticalRows(mateNetCtx, "h6f7")

    assertEquals(rows, Nil)
  }

  test("projects top-PV Greek gift entries as bounded practical rows") {
    val greekGiftCtx =
      topPvPracticalContext(
        fen = "6k1/7p/8/8/8/3B1N2/8/3QK3 w - - 0 1",
        playedMove = "d3h7",
        playedSan = "Bxh7+",
        line = List("d3h7", "g8h7", "f3g5", "h7g8", "d1h5"),
        scoreCp = 120
      )

    val rows = topPvPracticalRows(greekGiftCtx, "d3h7")

    assertEquals(rows.map(_.label), List("Greek gift"), clue(rows))
    assertEquals(rows.head.text, "The checked line starts a Greek gift sacrifice with the bishop on h7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project Greek gift prose without top-PV continuation support") {
    val greekGiftCtx =
      topPvPracticalContext(
        fen = "6k1/7p/8/8/8/3B1N2/8/3QK3 w - - 0 1",
        playedMove = "d3h7",
        playedSan = "Bxh7+",
        line = List("d3h7"),
        scoreCp = 120
      )

    val rows = topPvPracticalRows(greekGiftCtx, "d3h7")

    assertEquals(rows, Nil)
  }

  test("projects top-PV stalemate resources as bounded practical rows") {
    val stalemateCtx =
      topPvPracticalContext(
        fen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1",
        playedMove = "g5g6",
        playedSan = "Qg6",
        line = List("g5g6"),
        scoreCp = 0
      )

    val rows = topPvPracticalRows(stalemateCtx, "g5g6")

    assertEquals(rows.map(_.label), List("Stalemate resource"), clue(rows))
    assertEquals(rows.head.text, "The checked line keeps a stalemate resource around the king on h8 via g6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects top-PV perpetual checks as bounded practical rows") {
    val perpetualCtx =
      topPvPracticalContext(
        fen = "r4rk1/5ppp/8/8/7q/8/2Q3P1/R5K1 b - - 0 1",
        playedMove = "h4e1",
        playedSan = "Qe1+",
        line = List("h4e1", "g1h2", "e1h4", "h2g1", "h4e1"),
        scoreCp = 0
      )

    val rows = topPvPracticalRows(perpetualCtx, "h4e1")

    assertEquals(rows.map(_.label), List("Perpetual check"), clue(rows))
    assertEquals(rows.head.text, "The checked line keeps a perpetual-check resource against the king on g1 from e1.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project draw-resource rows without draw-stable played top-PV proof") {
    val cases =
      List(
        "score outside draw band" ->
          topPvPracticalContext(
            fen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1",
            playedMove = "g5g6",
            playedSan = "Qg6",
            line = List("g5g6"),
            scoreCp = 180
          ),
        "mate score" ->
          topPvPracticalContext(
            fen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1",
            playedMove = "g5g6",
            playedSan = "Qg6",
            line = List("g5g6"),
            scoreCp = 0,
            mate = Some(1)
          ),
        "played move not top PV entry" ->
          topPvPracticalContext(
            fen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1",
            playedMove = "g5g6",
            playedSan = "Qg6",
            line = List("g5g7"),
            scoreCp = 0
          )
      )

    cases.foreach { case (name, localCtx) =>
      val rows = topPvPracticalRows(localCtx, "g5g6")

      assertEquals(rows, Nil, clue(name -> rows))
    }
  }

  test("projects top-PV double checks as bounded practical rows") {
    val doubleCheckCtx =
      topPvPracticalContext(
        fen = "4k3/8/8/8/4N3/8/8/4R1K1 w - - 0 1",
        playedMove = "e4f6",
        playedSan = "Nf6++",
        line = List("e4f6"),
        scoreCp = 130
      )

    val rows = topPvPracticalRows(doubleCheckCtx, "e4f6")

    assertEquals(rows.map(_.label), List("Double check"), clue(rows))
    assertEquals(rows.head.text, "The checked line gives double check on e8 from e1 and f6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project double-check prose without a top-PV played-move witness") {
    val doubleCheckCtx =
      topPvPracticalContext(
        fen = "4k3/8/8/8/4N3/8/8/4R1K1 w - - 0 1",
        playedMove = "e4f6",
        playedSan = "Nf6++",
        line = List("g1f1", "e4f6"),
        scoreCp = 130
      )

    val rows = topPvPracticalRows(doubleCheckCtx, "e4f6")

    assertEquals(rows, Nil)
  }

  test("projects top-PV defender trades as bounded practical rows") {
    val defenderTradeCtx =
      withFocalTarget(
        topPvPracticalContext(
          fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
          playedMove = "c1a3",
          playedSan = "Ba3",
          line = List("c1a3", "f8a3", "b3a3"),
          scoreCp = 44
        ),
        "c5"
      )

    val rows = topPvPracticalRows(defenderTradeCtx, "c1a3")

    assertEquals(rows.map(_.label), List("Defender trade"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line trades on a3 to remove the defender from f8, loosening c5."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project defender-trade prose without a declared or structural target") {
    val defenderTradeCtx =
      topPvPracticalContext(
        fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
        playedMove = "c1a3",
        playedSan = "Ba3",
        line = List("c1a3", "f8a3", "b3a3"),
        scoreCp = 44
      )

    val rows = topPvPracticalRows(defenderTradeCtx, "c1a3")

    assert(!rows.exists(_.label == "Defender trade"), clue(rows))
  }

  test("projects top-PV bad-piece liquidation as a bounded practical row") {
    val badPieceCtx =
      topPvPracticalContext(
        fen = "3k1b2/8/8/8/3P4/n3P3/8/2B3K1 w - - 0 1",
        playedMove = "c1a3",
        playedSan = "Bxa3",
        line = List("c1a3", "f8a3"),
        scoreCp = 22
      )

    val rows = topPvPracticalRows(badPieceCtx, "c1a3")

    assertEquals(rows.map(_.label), List("Bad piece trade"), clue(rows))
    assertEquals(rows.head.text, "The checked line trades on a3 to clear the bad piece from c1.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project bad-piece liquidation prose for open bishop exchanges") {
    val openBishopCtx =
      topPvPracticalContext(
        fen = "5r2/4k3/8/8/3P4/2P5/8/2B3K1 w - - 0 1",
        playedMove = "c1a3",
        playedSan = "Ba3",
        line = List("c1a3", "e7f7", "a3f8", "f7f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(openBishopCtx, "c1a3")

    assert(!rows.exists(_.label == "Bad piece trade"), clue(rows))
  }

  test("projects top-PV queen trades as bounded practical rows") {
    val queenTradeCtx =
      topPvPracticalContext(
        fen = "3qk3/8/8/8/8/3Q4/8/6K1 w - - 0 1",
        playedMove = "d3d8",
        playedSan = "Qxd8+",
        line = List("d3d8", "e8d8"),
        scoreCp = 20
      )

    val rows = topPvPracticalRows(queenTradeCtx, "d3d8")

    assertEquals(rows.map(_.label), List("Queen trade"), clue(rows))
    assertEquals(rows.head.text, "This exchange moves the game into the queenless branch.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project queen-trade prose without king recapture") {
    val rookRecaptureCtx =
      topPvPracticalContext(
        fen = "k2qr3/8/8/8/8/3Q4/8/6K1 w - - 0 1",
        playedMove = "d3d8",
        playedSan = "Qxd8+",
        line = List("d3d8", "e8d8"),
        scoreCp = 20
      )

    val rows = topPvPracticalRows(rookRecaptureCtx, "d3d8")

    assert(!rows.exists(_.label == "Queen trade"), clue(rows))
  }

  test("does not project queen-trade prose without a top-PV played-move witness") {
    val queenTradeCtx =
      topPvPracticalContext(
        fen = "3qk3/8/8/8/8/3Q4/8/6K1 w - - 0 1",
        playedMove = "d3d8",
        playedSan = "Qxd8+",
        line = List("g1f1", "e8d8"),
        scoreCp = 20
      )

    val rows = topPvPracticalRows(queenTradeCtx, "d3d8")

    assertEquals(rows, Nil)
  }

  test("projects top-PV knight forks as bounded practical rows") {
    val forkCtx =
      topPvPracticalContext(
        fen = "k7/4r3/8/8/3N3q/8/8/6K1 w - - 0 1",
        playedMove = "d4f5",
        playedSan = "Nf5",
        line = List("d4f5"),
        scoreCp = 90
      )

    val rows = topPvPracticalRows(forkCtx, "d4f5")

    assertEquals(rows.map(_.label), List("Fork"), clue(rows))
    assertEquals(rows.head.text, "The checked line puts the knight on f5 attacking the queen on h4 and the rook on e7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project fork prose without a top-PV played-move witness") {
    val forkCtx =
      topPvPracticalContext(
        fen = "k7/4r3/8/8/3N3q/8/8/6K1 w - - 0 1",
        playedMove = "d4f5",
        playedSan = "Nf5",
        line = List("g1f1", "d4f5"),
        scoreCp = 90
      )

    val rows = topPvPracticalRows(forkCtx, "d4f5")

    assertEquals(rows, Nil)
  }

  test("projects top-PV overloaded defenders as bounded practical rows") {
    val overloadCtx =
      topPvPracticalContext(
        fen = "k7/7p/5n2/3p4/8/8/8/3Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("d1d3"),
        scoreCp = 26
      )

    val rows = topPvPracticalRows(overloadCtx, "d1d3")

    assertEquals(rows.map(_.label), List("Overloaded defender"), clue(rows))
    assertEquals(rows.head.text, "The checked line overloads the defender on f6 across d5 and h7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project overload prose without a top-PV played-move witness") {
    val overloadCtx =
      topPvPracticalContext(
        fen = "k7/7p/5n2/3p4/8/8/8/3Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("g1f1", "d1d3"),
        scoreCp = 26
      )

    val rows = topPvPracticalRows(overloadCtx, "d1d3")

    assertEquals(rows, Nil)
  }

  test("projects top-PV decoys as bounded practical rows") {
    val decoyCtx =
      topPvPracticalContext(
        fen = "k7/8/8/3q4/5N2/8/4B3/3Q2K1 w - - 0 1",
        playedMove = "f4d3",
        playedSan = "Nd3",
        line = List("f4d3", "d5d3", "e2d3"),
        scoreCp = 120
      )

    val rows = topPvPracticalRows(decoyCtx, "f4d3")

    assertEquals(rows.map(_.label), List("Decoy"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line offers the knight on d3 to lure the queen from d5, then recaptures on d3."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project decoy prose without the full top-PV lure branch") {
    val decoyCtx =
      topPvPracticalContext(
        fen = "k7/8/8/3q4/5N2/8/4B3/3Q2K1 w - - 0 1",
        playedMove = "f4d3",
        playedSan = "Nd3",
        line = List("f4d3", "a8b8"),
        scoreCp = 120
      )

    val rows = topPvPracticalRows(decoyCtx, "f4d3")

    assertEquals(rows, Nil)
  }

  test("projects top-PV deflections as bounded practical rows") {
    val deflectionCtx =
      topPvPracticalContext(
        fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
        playedMove = "c1a3",
        playedSan = "Ba3",
        line = List("c1a3", "f8a3"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(deflectionCtx, "c1a3")

    assertEquals(rows.map(_.label), List("Deflection"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line attacks the defender on f8 from a3; after it moves, g7 loses that defense."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project deflection prose without the top-PV defender move") {
    val deflectionCtx =
      topPvPracticalContext(
        fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
        playedMove = "c1a3",
        playedSan = "Ba3",
        line = List("c1a3", "d8c7"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(deflectionCtx, "c1a3")

    assertEquals(rows, Nil)
  }

  test("projects top-PV discovered attacks as bounded practical rows") {
    val discoveredCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1",
        playedMove = "d3f4",
        playedSan = "Nf4",
        line = List("d3f4"),
        scoreCp = 80
      )

    val rows = topPvPracticalRows(discoveredCtx, "d3f4")

    assertEquals(rows.map(_.label), List("Discovered attack"), clue(rows))
    assertEquals(rows.head.text, "The checked line clears d3, revealing a bishop attack from b1 on h7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project discovered-attack prose without a top-PV played-move witness") {
    val discoveredCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1",
        playedMove = "d3f4",
        playedSan = "Nf4",
        line = List("g1f1", "d3f4"),
        scoreCp = 80
      )

    val rows = topPvPracticalRows(discoveredCtx, "d3f4")

    assertEquals(rows, Nil)
  }

  test("projects top-PV hanging pieces as bounded practical rows") {
    val hangingCtx =
      topPvPracticalContext(
        fen = "k7/8/8/5r2/2B5/8/8/6K1 w - - 0 1",
        playedMove = "c4d3",
        playedSan = "Bd3",
        line = List("c4d3"),
        scoreCp = 48
      )

    val rows = topPvPracticalRows(hangingCtx, "c4d3")

    assertEquals(rows.map(_.label), List("Hanging piece"), clue(rows))
    assertEquals(rows.head.text, "The checked line attacks the undefended rook on f5 with the bishop on d3.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project hanging-piece prose without a top-PV played-move witness") {
    val hangingCtx =
      topPvPracticalContext(
        fen = "k7/8/8/5r2/2B5/8/8/6K1 w - - 0 1",
        playedMove = "c4d3",
        playedSan = "Bd3",
        line = List("g1f1", "c4d3"),
        scoreCp = 48
      )

    val rows = topPvPracticalRows(hangingCtx, "c4d3")

    assertEquals(rows, Nil)
  }

  test("projects target-bound trapped pieces as bounded practical rows") {
    val trappedCtx =
      withFocalTarget(
        topPvPracticalContext(
          fen = "k5pr/7p/8/8/8/2B5/8/6K1 w - - 0 1",
          playedMove = "c3g7",
          playedSan = "Bg7",
          line = List("c3g7"),
          scoreCp = 64
        ),
        "h8"
      )

    val rows = topPvPracticalRows(trappedCtx, "c3g7")

    assertEquals(rows.map(_.label), List("Trapped piece"), clue(rows))
    assertEquals(rows.head.text, "The checked line traps the rook on h8 with the bishop on g7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project trapped-piece prose without an exact focal target") {
    val trappedCtx =
      topPvPracticalContext(
        fen = "k5pr/7p/8/8/8/2B5/8/6K1 w - - 0 1",
        playedMove = "c3g7",
        playedSan = "Bg7",
        line = List("c3g7"),
        scoreCp = 64
      )

    val rows = topPvPracticalRows(trappedCtx, "c3g7")

    assertNotEquals(rows.map(_.label), List("Trapped piece"), clue(rows))
  }

  test("projects target-bound domination as bounded practical rows") {
    val dominationCtx =
      withFocalTarget(
        topPvPracticalContext(
          fen = "n6k/8/B7/1K6/8/8/8/2R5 w - - 0 1",
          playedMove = "a6b7",
          playedSan = "Bb7",
          line = List("a6b7"),
          scoreCp = 42
        ),
        "a8"
      )

    val rows = topPvPracticalRows(dominationCtx, "a6b7")

    assertEquals(rows.map(_.label), List("Domination"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line dominates the knight on a8; the bishop on b7 controls b6 and c7."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects target-bound zwischenzug as a bounded practical row") {
    val zwischenzugCtx =
      withFocalTarget(
        topPvPracticalContext(
          fen = "4k3/8/8/8/3b4/5N2/8/3QK3 w - - 0 1",
          playedMove = "d1a4",
          playedSan = "Qa4+",
          line = List("d1a4"),
          scoreCp = 42
        ),
        "d4"
      )

    val rows = topPvPracticalRows(zwischenzugCtx, "d1a4")

    assertEquals(rows.map(_.label), List("Zwischenzug"), clue(rows))
    assertEquals(rows.head.text, "The checked line inserts d1a4 as a check on e8 before the recapture on d4.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects top-PV skewers as bounded practical rows") {
    val skewerCtx =
      topPvPracticalContext(
        fen = "r6k/8/8/8/8/8/7K/4Q2R b - - 0 1",
        playedMove = "a8a1",
        playedSan = "Ra1",
        line = List("a8a1"),
        scoreCp = 76
      )

    val rows = topPvPracticalRows(skewerCtx, "a8a1")

    assertEquals(rows.map(_.label), List("Skewer"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line lines up the rook on a1 against the queen on e1, with the rook on h1 behind it."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project skewer prose without a top-PV played-move witness") {
    val skewerCtx =
      topPvPracticalContext(
        fen = "r6k/8/8/8/8/8/7K/4Q2R b - - 0 1",
        playedMove = "a8a1",
        playedSan = "Ra1",
        line = List("h8g8", "a8a1"),
        scoreCp = 76
      )

    val rows = topPvPracticalRows(skewerCtx, "a8a1")

    assertEquals(rows, Nil)
  }

  test("projects top-PV interference as a bounded practical row") {
    val interferenceCtx =
      topPvPracticalContext(
        fen = "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1",
        playedMove = "f5d6",
        playedSan = "Nd6",
        line = List("f5d6", "a8b8"),
        scoreCp = 48
      )

    val rows = topPvPracticalRows(interferenceCtx, "f5d6")

    assertEquals(rows.map(_.label), List("Interference"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line puts the knight on d6 between the rook on d8 and the queen on d5."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project interference prose without a top-PV played-move witness") {
    val interferenceCtx =
      topPvPracticalContext(
        fen = "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1",
        playedMove = "f5d6",
        playedSan = "Nd6",
        line = List("g1f1", "f5d6"),
        scoreCp = 48
      )

    val rows = topPvPracticalRows(interferenceCtx, "f5d6")

    assertEquals(rows, Nil)
  }

  test("projects top-PV battery pressure as a bounded practical row") {
    val batteryCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/8/8/8/1B1Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("d1d3", "a8b8"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(batteryCtx, "d1d3")

    assertEquals(rows.map(_.label), List("Battery pressure"), clue(rows))
    assertEquals(rows.head.text, "The checked line forms a queen-bishop battery on the diagonal toward h7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project battery prose without a top-PV played-move witness") {
    val batteryCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/8/8/8/1B1Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("g1f1", "d1d3"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(batteryCtx, "d1d3")

    assertEquals(rows, Nil)
  }

  test("does not project battery prose when the target ray is blocked") {
    val blockedBatteryCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/4P3/8/8/1B1Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("d1d3", "a8b8"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(blockedBatteryCtx, "d1d3")

    assertEquals(rows, Nil)
  }

  test("projects top-PV absolute pins as bounded practical rows") {
    val pinCtx =
      topPvPracticalContext(
        fen = "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1",
        playedMove = "f8b4",
        playedSan = "Bb4",
        line = List("f8b4", "e1d1"),
        scoreCp = 52
      )

    val rows = topPvPracticalRows(pinCtx, "f8b4")

    assertEquals(rows.map(_.label), List("Pin pressure"), clue(rows))
    assertEquals(rows.head.text, "The checked line pins the knight on c3 to the king on e1.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project pin prose without a top-PV played-move witness") {
    val pinCtx =
      topPvPracticalContext(
        fen = "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1",
        playedMove = "f8b4",
        playedSan = "Bb4",
        line = List("e8f7", "f8b4"),
        scoreCp = 52
      )

    val rows = topPvPracticalRows(pinCtx, "f8b4")

    assertEquals(rows, Nil)
  }

  test("keeps relative pins out of practical pin rows") {
    val relativePinCtx =
      topPvPracticalContext(
        fen = "4kb2/8/8/8/8/2N5/3Q4/6K1 b - - 0 1",
        playedMove = "f8b4",
        playedSan = "Bb4",
        line = List("f8b4", "g1f1"),
        scoreCp = 52
      )

    val rows = topPvPracticalRows(relativePinCtx, "f8b4")

    assertEquals(rows.map(_.label), List("X-ray pressure"), clue(rows))
    assert(!rows.exists(_.label == "Pin pressure"), clue(rows))
  }

  test("projects top-PV connected rooks as a bounded practical row") {
    val connectedRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/6K1/R6R w - - 0 1",
        playedMove = "h1d1",
        playedSan = "Rd1",
        line = List("h1d1", "g8f8"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(connectedRooksCtx, "h1d1")

    assertEquals(rows.map(_.label), List("Connected rooks"), clue(rows))
    assertEquals(rows.head.text, "The checked line connects the rooks on the first rank.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project connected-rook prose without a top-PV played-move witness") {
    val connectedRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/6K1/R6R w - - 0 1",
        playedMove = "h1d1",
        playedSan = "Rd1",
        line = List("g2f1", "g8f8"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(connectedRooksCtx, "h1d1")

    assertEquals(rows, Nil)
  }

  test("does not project connected-rook prose when a piece remains between the rooks") {
    val blockedRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/6K1/RN5R w - - 0 1",
        playedMove = "h1d1",
        playedSan = "Rd1",
        line = List("h1d1", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(blockedRooksCtx, "h1d1")

    assertEquals(rows, Nil)
  }

  test("projects top-PV doubled rooks as a bounded practical row") {
    val doubledRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/R7/8/6K1/R7 w - - 0 1",
        playedMove = "a1a2",
        playedSan = "Ra2",
        line = List("a1a2", "g8f8"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(doubledRooksCtx, "a1a2")

    assertEquals(rows.map(_.label), List("Doubled rooks"), clue(rows))
    assertEquals(rows.head.text, "The checked line doubles the rooks on the a-file.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project doubled-rook prose without a top-PV played-move witness") {
    val doubledRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/R7/8/6K1/R7 w - - 0 1",
        playedMove = "a1a2",
        playedSan = "Ra2",
        line = List("g2f1", "g8f8"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(doubledRooksCtx, "a1a2")

    assertEquals(rows, Nil)
  }

  test("does not project doubled-rook prose when a piece remains between the rooks") {
    val blockedRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/R7/N7/6K1/R7 w - - 0 1",
        playedMove = "a1a2",
        playedSan = "Ra2",
        line = List("a1a2", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(blockedRooksCtx, "a1a2")

    assertEquals(rows, Nil)
  }

  test("projects top-PV connected passers as a bounded practical row") {
    val connectedCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/3P4/4P3/8/8/6K1 w - - 0 1",
        playedMove = "e4e5",
        playedSan = "e5",
        line = List("e4e5", "g8f8"),
        scoreCp = 68
      )

    val rows = topPvPracticalRows(connectedCtx, "e4e5")

    assertEquals(rows.map(_.label), List("Connected passers"), clue(rows))
    assertEquals(rows.head.text, "The checked line leaves connected passers on d5 and e5.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project connected-passer prose without a top-PV played-move witness") {
    val connectedCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/3P4/4P3/8/8/6K1 w - - 0 1",
        playedMove = "e4e5",
        playedSan = "e5",
        line = List("g1f1", "g8f8"),
        scoreCp = 68
      )

    val rows = topPvPracticalRows(connectedCtx, "e4e5")

    assertEquals(rows, Nil)
  }

  test("does not project disconnected pawn advances as connected-passer prose") {
    val disconnectedCtx =
      topPvPracticalContext(
        fen = "6k1/8/3p4/8/4P3/8/8/6K1 w - - 0 1",
        playedMove = "e4e5",
        playedSan = "e5",
        line = List("e4e5", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(disconnectedCtx, "e4e5")

    assertEquals(rows, Nil)
  }

  test("does not project connected-passer prose when the opponent also has a passer") {
    val counterPasserCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/3P4/4P2p/8/8/6K1 w - - 0 1",
        playedMove = "e4e5",
        playedSan = "e5",
        line = List("e4e5", "g8f8"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(counterPasserCtx, "e4e5")

    assert(!rows.exists(_.label == "Connected passers"), clue(rows))
  }

  test("projects top-PV outside passers as a bounded practical row") {
    val outsideCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/5P2/P7/8/8/6K1 w - - 0 1",
        playedMove = "a4a5",
        playedSan = "a5",
        line = List("a4a5", "g8f8"),
        scoreCp = 54
      )

    val rows = topPvPracticalRows(outsideCtx, "a4a5")

    assertEquals(rows.map(_.label), List("Outside passer"), clue(rows))
    assertEquals(rows.head.text, "The checked line leaves an outside passer on a5.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project outside-passer prose without a top-PV played-move witness") {
    val outsideCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/5P2/P7/8/8/6K1 w - - 0 1",
        playedMove = "a4a5",
        playedSan = "a5",
        line = List("g1f1", "g8f8"),
        scoreCp = 54
      )

    val rows = topPvPracticalRows(outsideCtx, "a4a5")

    assertEquals(rows, Nil)
  }

  test("does not project lone flank passers as outside-passer prose") {
    val loneFlankCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/P7/8/8/6K1 w - - 0 1",
        playedMove = "a4a5",
        playedSan = "a5",
        line = List("a4a5", "g8f8"),
        scoreCp = 24
      )

    val rows = topPvPracticalRows(loneFlankCtx, "a4a5")

    assert(!rows.exists(_.label == "Outside passer"), clue(rows))
  }

  test("does not project front-blocked flank passers as outside-passer prose") {
    val blockedOutsideCtx =
      topPvPracticalContext(
        fen = "k7/8/8/5P2/P7/8/8/6K1 w - - 0 1",
        playedMove = "a4a5",
        playedSan = "a5",
        line = List("a4a5", "a8b8"),
        scoreCp = 20
      )

    val rows = topPvPracticalRows(blockedOutsideCtx, "a4a5")

    assert(!rows.exists(_.label == "Outside passer"), clue(rows))
  }

  test("projects top-PV passed-pawn advances as a bounded practical row") {
    val passerCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/1P6/8/8/8/6K1 w - - 0 1",
        playedMove = "b5b6",
        playedSan = "b6",
        line = List("b5b6", "g8f8"),
        scoreCp = 74
      )

    val rows = topPvPracticalRows(passerCtx, "b5b6")

    assertEquals(rows.map(_.label), List("Passed pawn advance"), clue(rows))
    assertEquals(rows.head.text, "The checked line advances the passed pawn to b6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project passed-pawn prose without a top-PV played-move witness") {
    val passerCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/1P6/8/8/8/6K1 w - - 0 1",
        playedMove = "b5b6",
        playedSan = "b6",
        line = List("g1f1", "g8f8"),
        scoreCp = 74
      )

    val rows = topPvPracticalRows(passerCtx, "b5b6")

    assertEquals(rows, Nil)
  }

  test("does not project passed-pawn prose when the advanced pawn is still stoppable by enemy pawns") {
    val blockedCtx =
      topPvPracticalContext(
        fen = "6k1/2p5/8/1P6/8/8/8/6K1 w - - 0 1",
        playedMove = "b5b6",
        playedSan = "b6",
        line = List("b5b6", "g8f8"),
        scoreCp = 24
      )

    val rows = topPvPracticalRows(blockedCtx, "b5b6")

    assertEquals(rows, Nil)
  }

  test("projects top-PV endgame king activation as a bounded practical row") {
    val kingCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/8/4K3 w - - 0 1",
        playedMove = "e1d2",
        playedSan = "Kd2",
        line = List("e1d2", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(kingCtx, "e1d2")

    assertEquals(rows.map(_.label), List("King activation"), clue(rows))
    assertEquals(rows.head.text, "The checked line activates the king on d2 for the endgame.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project king activation without a top-PV played-move witness") {
    val kingCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/8/4K3 w - - 0 1",
        playedMove = "e1d2",
        playedSan = "Kd2",
        line = List("g1f1", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(kingCtx, "e1d2")

    assertEquals(rows, Nil)
  }

  test("does not project king activation when the king does not move closer to the center") {
    val kingCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/8/4K3 w - - 0 1",
        playedMove = "e1f1",
        playedSan = "Kf1",
        line = List("e1f1", "g8f8"),
        scoreCp = 12
      )

    val rows = topPvPracticalRows(kingCtx, "e1f1")

    assertEquals(rows, Nil)
  }

  test("suppresses practical central rows when tactical truth mode vetoes support") {
    val diagonalCaptureFen =
      "r2q1rk1/pp2npb1/2p1b1pp/3pB3/3PN3/1BP2N2/PP3PPP/R2Q1RK1 b - - 0 15"
    val lines =
      List(
        VariationLine(List("d5e4"), scoreCp = 0, depth = 18),
        VariationLine(List("g7e5"), scoreCp = 0, depth = 18)
      )
    val truth = tacticalTruthContract(playedMove = "d5e4")
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = diagonalCaptureFen, ply = 30, playedMove = "d5e4", lines = lines, truthContract = Some(truth))

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(truth)
      )

    assertEquals(rows, Nil)
  }

  test("projects probe-backed quiet piece improvement as a bounded practical row") {
    val rows =
      noRankedSupportedLocalRows(
        localCtx = quietKnightCtx,
        localInputs =
          inputs().copy(
            mainBundle = None,
            quietIntent = Some(supportedQuietIntent())
          ),
        playedMove = "g1f3"
      )

    assertEquals(rows.map(_.label), List("Piece improvement"), clue(rows))
    assertEquals(rows.head.text, "The checked move improves the knight by placing it on f3.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("projects quiet endgame piece intent as plain piece improvement") {
    val rows =
      noRankedSupportedLocalRows(
        localCtx = quietKnightCtx.copy(phase = PhaseContext("Endgame", "Technical ending")),
        localInputs =
          inputs().copy(
            mainBundle = None,
            quietIntent = Some(supportedQuietIntent())
          ),
        playedMove = "g1f3"
      )

    assertEquals(rows.map(_.label), List("Piece improvement"), clue(rows))
    assertEquals(rows.head.text, "The checked move improves the knight by placing it on f3.")
  }

  test("projects legal castling quiet intent as king-safety practical support") {
    val castleCtx =
      ctx.copy(
        fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
        playedMove = Some("e1g1"),
        playedSan = Some("O-O")
      )
    val rows =
      noRankedSupportedLocalRows(
        localCtx = castleCtx,
        localInputs =
          inputs().copy(
            mainBundle = None,
            quietIntent =
              Some(
                supportedQuietIntent(
                  intentClass = QuietMoveIntentClass.KingSafety,
                  anchorSquare = "g1",
                  sourceKind = "castle_short",
                  claimText = "This castles to keep the king safer."
                )
              )
          ),
        playedMove = "e1g1"
      )

    assertEquals(rows.map(_.label), List("King safety"), clue(rows))
    assertEquals(rows.head.text, "The checked move castles to improve king safety.")
  }

  test("does not project quiet intent when the legal move misses the anchored square") {
    val rows =
      noRankedSupportedLocalRows(
        localCtx = quietKnightCtx,
        localInputs =
          inputs().copy(
            mainBundle = None,
            quietIntent = Some(supportedQuietIntent(anchorSquare = "c4"))
          ),
        playedMove = "g1f3"
      )

    assertEquals(rows, Nil)
  }

  test("does not project quiet intent without a square anchor") {
    val rows =
      noRankedSupportedLocalRows(
        localCtx = quietKnightCtx,
        localInputs =
          inputs().copy(
            mainBundle = None,
            quietIntent = Some(supportedQuietIntent(anchorSquare = ""))
          ),
        playedMove = "g1f3"
      )

    assertEquals(rows, Nil)
  }

  test("does not project quiet intent when packet family does not match intent class") {
    val rows =
      noRankedSupportedLocalRows(
        localCtx = quietKnightCtx,
        localInputs =
          inputs().copy(
            mainBundle = None,
            quietIntent =
              Some(
                supportedQuietIntent(
                  proofFamilyOverride = Some(ProofFamilyId.KingSafety.wireKey),
                  ontologyOverride = Some(PlayerFacingClaimOntologyKind.KingSafety)
                )
              )
          ),
        playedMove = "g1f3"
      )

    assertEquals(rows, Nil)
  }

  test("suppresses quiet practical rows when tactical truth mode vetoes support") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = quietKnightCtx,
        inputs =
          inputs(truthMode = PlayerFacingTruthMode.Tactical).copy(
            mainBundle = None,
            quietIntent = Some(supportedQuietIntent())
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(tacticalTruthContract("g1f3"))
      )

    assertEquals(rows, Nil)
  }

  test("suppresses neutralize_key_break row when tactical truth mode vetoes SupportedLocal") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects an admitted neutralize_key_break packet claim when no timing plan is selected") {
    val rows =
      noRankedSupportedLocalRows(localCtx = neutralizeCtx, localInputs = inputs(), playedMove = "e2e3")

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "This stops the ...c5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5")))
    )
    assertEquals(rows.head.source, None)
  }

  test("verbalizes typed pinned-pawn mechanism for neutralize_key_break packet claims") {
    val pinCtx =
      ctx.copy(
        fen = "1k1rr3/pp3ppp/3p1b2/1qp2Q2/4P3/2P1BP2/PP4PP/2KR3R w - - 3 17",
        playedMove = Some("e3f4"),
        playedSan = Some("Bf4")
      )
    val pinPacket =
      supportedNeutralizePacket(
        anchorTerms = List("...d5", "d5"),
        ownerSeedTerms =
          List(
            "neutralize_key_break",
            "...d5",
            "d5",
            "break_clamp_mechanism:pinned_pawn",
            "pinned_break_pawn:d6",
            "pin_attacker:f4",
            "pin_king:b8",
            "break_route:...d6-d5"
          ),
        structureTransitionTerms =
          List(
            "...d5",
            "d5",
            "break_clamp_mechanism:pinned_pawn",
            "pinned_break_pawn:d6",
            "pin_attacker:f4",
            "pin_king:b8",
            "break_route:...d6-d5"
          ),
        bestDefenseMove = Some("Re6"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...d5"))
      )
    val rows =
      noRankedSupportedLocalRows(localCtx = pinCtx, localInputs = inputs(packet = pinPacket), playedMove = "e3f4")

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "This pins the d6 pawn, so the ...d5 break is not available."
    )
    assertEquals(
      rows.head.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.CounterplayBreak,
          token = Some("...d5"),
          target = Some("d6")
        )
      )
    )
  }

  test("does not project tokenless neutralize_key_break packets") {
    val tokenless =
      supportedNeutralizePacket(
        anchorTerms = Nil,
        ownerSeedTerms = List("neutralize_key_break", "counterplay_axis_suppression"),
        structureTransitionTerms = Nil,
        bestDefenseMove = None,
        exactSliceProof = None
      )

    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = tokenless))

    assertEquals(rows, Nil)
  }

  test("does not recover fake break tokens from packet terms without typed proof") {
    val termOnly =
      supportedNeutralizePacket(
        anchorTerms = List("...c5"),
        ownerSeedTerms = List("neutralize_key_break", "...c5"),
        structureTransitionTerms = List("...c5"),
        exactSliceProof = None
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = termOnly),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project neutralize_key_break proof without matching break terms") {
    val mismatched =
      supportedNeutralizePacket(
        anchorTerms = List("...c5"),
        ownerSeedTerms = List("neutralize_key_break", "...c5"),
        structureTransitionTerms = List("...c5"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...e5"))
      )

    val rows =
      noRankedSupportedLocalRows(localInputs = inputs(packet = mismatched))

    assertEquals(rows, Nil)
  }

  test("does not project single-square break tokens that collide with the played move") {
    val collisionPacket =
      supportedNeutralizePacket(
        anchorTerms = List("g4"),
        ownerSeedTerms = List("neutralize_key_break", "g4"),
        structureTransitionTerms = List("g4"),
        bestDefenseMove = Some("g4"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
      )

    val rows =
      noRankedSupportedLocalRows(localCtx = collisionCtx, localInputs = inputs(packet = collisionPacket))

    assertEquals(rows, Nil)
  }

  test("does not use SAN-only played move text as collision authority") {
    val collisionPacket =
      supportedNeutralizePacket(
        anchorTerms = List("g4"),
        ownerSeedTerms = List("neutralize_key_break", "g4"),
        structureTransitionTerms = List("g4"),
        bestDefenseMove = Some("g4"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
      )

    val rows =
      noRankedSupportedLocalRows(localCtx = sanOnlyCollisionCtx, localInputs = inputs(packet = collisionPacket))

    assertEquals(rows, Nil)
  }

  test("does not replace a played-move collision with a later anchor square") {
    val collisionPacket =
      supportedNeutralizePacket(
        anchorTerms = List("c6"),
        ownerSeedTerms = List("neutralize_key_break", "g4", "c6"),
        structureTransitionTerms = List("g4", "c6"),
        bestDefenseMove = Some("g4"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
      )

    val rows =
      noRankedSupportedLocalRows(localCtx = collisionCtx, localInputs = inputs(packet = collisionPacket))

    assertEquals(rows, Nil)
  }

  test("does not project route-shaped break tokens when the played move occupies the destination square") {
    val routePacket =
      supportedNeutralizePacket(
        anchorTerms = List("e4-e5"),
        ownerSeedTerms = List("neutralize_key_break", "e4-e5"),
        structureTransitionTerms = List("e4-e5"),
        bestDefenseMove = Some("e4e5"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("e4-e5"))
      )

    val rows =
      noRankedSupportedLocalRows(localCtx = playedDestinationRouteCtx, localInputs = inputs(packet = routePacket))

    assertEquals(rows, Nil)
  }

  test("does not project plan plus packet neutralize rows when typed proof token differs from plan witness") {
    val mismatched =
      supportedNeutralizePacket(
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...e5"))
      )

    val decision =
      NeutralizeKeyBreakSurfaceGate.decideForPlanPacket(
        plan = neutralizePlan(namedBreak = Some("...c5")),
        packet = mismatched,
        ctx = ctx
      )

    assertEquals(decision.token, None)
    assertEquals(decision.rejectReason, Some(NeutralizeKeyBreakSurfaceGate.MissingNamedBreak))
  }

  test("ignores source mismatch release risk and other proof families") {
    val cases =
      List(
        (
          "source mismatch",
          supportedNeutralizePacket(proofSource = "wrong_source"),
          neutralizePlan()
        ),
        (
          "release risk",
          supportedNeutralizePacket(releaseRisks = List(PlayerFacingClaimReleaseRisk.RivalRelease)),
          neutralizePlan()
        ),
        (
          "other proof family",
          supportedNeutralizePacket(proofFamily = ProofFamilyId.CounterplayRestraint.wireKey),
          neutralizePlan(proofFamily = ProofFamilyId.CounterplayRestraint.wireKey)
        )
      )

    cases.foreach { case (name, packet, plan) =>
      val rows =
        MoveReviewSupportedLocalSurfaceRows.build(
          ctx = ctx,
          inputs = inputs(packet = packet),
          rankedPlans = ranked(plan),
          truthContract = Some(safeTruthContract())
        )

      assertEquals(rows, Nil, clue(name -> rows))
    }
  }

  test("released text comes from the named witness token rather than raw claim prose") {
    val raw =
      "The strategic point is that neutralize_key_break uses counterplay_axis_suppression on e4d5|c6d5."
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = inputs(claimText = raw),
        rankedPlans = ranked(neutralizePlan(claim = raw)),
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(
      rows.headOption.map(_.text),
      Some("This stops the ...c5 break before it appears.")
    )
    assert(!rows.exists(_.text.contains("neutralize_key_break")), clue(rows))
    assert(!rows.exists(_.text.contains("counterplay_axis_suppression")), clue(rows))
    assert(!rows.exists(_.text.contains("e4d5|c6d5")), clue(rows))
  }

  test("keeps bounded supported-local rows on the full player payload surface") {
    val moveDeltaCases =
      List(
        (
          supportedIqpPacket(),
          "This sequence leaves an isolated pawn as the local target.",
          "IQP target",
          "The checked line leaves d5 as an isolated pawn target."
        ),
        (
          supportedSimplificationPacket(),
          "This trade keeps the same local edge on d5.",
          "Simplification",
          "The checked line keeps the same local edge after the exchange on d5."
        ),
        (
          supportedOutpostPacket(),
          "The move occupies the e5 outpost.",
          "Knight outpost",
          "The checked line puts the knight on the e5 outpost."
        ),
        (
          supportedCounterplayRestraintPacket(),
          "This keeps the opponent's break restrained.",
          "Counterplay restraint",
          "The checked line keeps the opponent's break restrained."
        )
      ) ++ admittedExchangeOwnershipCases.map { case (packet, expectedLabel, expectedText) =>
        (packet, expectedText, expectedLabel, expectedText)
      }

    moveDeltaCases.foreach { case (packet, claimText, expectedLabel, expectedText) =>
      val supportedRows =
        noRankedSupportedLocalRows(localInputs = inputs(packet = packet, claimText = claimText))
      val surface =
        MoveReviewPlayerPayloadBuilder.build(
          ctx = ctx.copy(renderMode = NarrativeRenderMode.MoveReview),
          moveReviewExplanation = None,
          moveReviewLedger = None,
          refs = None,
          evaluatedPlans = Nil,
          authoringSurface = AuthoringEvidenceSurface(Nil, Nil, None),
          supportedLocalRows = supportedRows
        )

      assertEquals(surface.summaryRows.map(row => row.label -> row.text), List(expectedLabel -> expectedText))
      assertEquals(
        surface.summaryRows.flatMap(_.authority),
        List(expectedExchangeOwnershipAuthority(expectedLabel)),
        clue(surface.summaryRows)
      )
      assertEquals(surface.advancedRows, Nil, clue(surface.advancedRows))
    }
  }

  test("projects cataloged relation selector evidence as targeted advanced authority") {
    val focus = List("a1", "b2", "c3")

    RelationObservationCatalog.Implemented.zipWithIndex.foreach { case (descriptor, index) =>
      val relationIdea =
        StrategyIdeaSignal(
          ideaId = s"relation_${index + 1}",
          ownerSide = "white",
          kind = descriptor.ideaKind,
          group = StrategicIdeaGroup.PieceAndLineManagement,
          readiness = descriptor.readiness,
          focusSquares = focus,
          confidence = descriptor.confidence,
          evidenceRefs = descriptor.wireEvidenceRefs,
          relationKind = Some(descriptor.relationKind),
          relationFocusSquares = focus
        )
      val surface =
        MoveReviewPlayerPayloadBuilder.build(
          ctx = ctx.copy(renderMode = NarrativeRenderMode.MoveReview),
          moveReviewExplanation = None,
          moveReviewLedger = None,
          refs = None,
          evaluatedPlans = Nil,
          authoringSurface = AuthoringEvidenceSurface(Nil, Nil, None),
          strategyPack = Some(StrategyPack(sideToMove = "white", strategicIdeas = List(relationIdea)))
        )
      val relationRow =
        surface.advancedRows
          .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
          .getOrElse(fail(s"missing relation row for ${descriptor.relationKind}: ${surface.advancedRows}"))

      assertEquals(relationRow.label, descriptor.surfaceRowLabel, clue(descriptor))
      assertEquals(
        relationRow.authority,
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.StrategicRelation,
            token = Some(descriptor.relationKind),
            target = descriptor.fallbackTarget(focus)
          )
        ),
        clue(descriptor)
      )
    }
  }

  private def tacticalTruthContract(playedMove: String): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some(playedMove),
      verifiedBestMove = Some(playedMove),
      truthClass = DecisiveTruthClass.Blunder,
      cpLoss = 280,
      swingSeverity = 280,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.TacticalRefutation,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )
