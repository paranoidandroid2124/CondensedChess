package lila.commentary.analysis

import lila.commentary.{ MoveReviewPlayerSurfaceRow, MoveReviewSurfaceAuthority }
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.NarrativeContext
import lila.commentary.model.authoring.AuthorQuestionKind
import lila.commentary.model.strategic.VariationLine
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
  private val MadernaExactFen =
    "nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17"
  private val MadernaExactLines =
    List(
      VariationLine(List("e4e5", "d6e5", "f4e5", "d7e5", "c4e5"), scoreCp = 82, depth = 18),
      VariationLine(List("c1e3", "b7b5", "a5b6"), scoreCp = 36, depth = 18)
    )
  private val DirectBreakFen =
    "rnbqk2r/pp2bppp/4pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1 w kq - 0 8"

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
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = scope,
      triggerKind = PlanTaxonomy.PlanKind.BreakPrevention.id,
      anchorTerms = anchorTerms,
      bestDefenseMove = bestDefenseMove,
      bestDefenseBranchKey = Some("e4d5|c6d5"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = ownerSeedTerms,
          continuationTerms = List("counterplay_axis_suppression", "e4d5", "c6d5"),
          structureTransitionTerms = structureTransitionTerms,
          exactSliceProof = exactSliceProof
        ),
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedDefenderTradePacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.DefenderTrade("d4", "e5", "c5")),
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
      proofFamily = PlanTaxonomy.PlanKind.DefenderTrade.id,
      triggerKind = PlanTaxonomy.PlanKind.DefenderTrade.id,
      anchorTerms = List("d4", "e5", "c5"),
      ownerSeedTerms = List("defender_trade", "d4", "c5"),
      structureTransitionTerms = List("defender_removed:d4-e5", "target:c5"),
      exactSliceProof = exactSliceProof,
      releaseRisks = releaseRisks,
      suppressionReasons = suppressionReasons,
      sameBranchState = sameBranchState,
      persistence = persistence
    )

  private def supportedBadPieceLiquidationPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.BadPieceLiquidation("c1", "e3"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
      proofFamily = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
      triggerKind = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
      anchorTerms = List("c1", "e3"),
      ownerSeedTerms = List("bad_piece_liquidation", "c1", "e3"),
      structureTransitionTerms = List("bad_piece_removed:c1-e3"),
      exactSliceProof = exactSliceProof
    )

  private def supportedOverloadPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Overload("f6", List("d5", "h7"), "d3"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Overload,
      anchorTerms = List("f6", "d5", "h7", "d3"),
      ownerSeedTerms = List("overload", "defender:f6", "target:d5", "target:h7", "attacker:d3"),
      structureTransitionTerms = List("overload_relation", "d1d3"),
      exactSliceProof = exactSliceProof
    )

  private def supportedDeflectionPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Deflection("f8", "g7", "a3"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Deflection,
      anchorTerms = List("f8", "g7", "a3"),
      ownerSeedTerms = List("deflection", "defender:f8", "target:g7", "attacker:a3"),
      structureTransitionTerms = List("deflection_relation", "c1a3", "f8a3"),
      exactSliceProof = exactSliceProof
    )

  private def supportedDiscoveredAttackPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.DiscoveredAttack("b1", "d3", "h7", "bishop"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
      anchorTerms = List("b1", "d3", "h7"),
      ownerSeedTerms = List("discovered_attack", "attacker:b1", "cleared_square:d3", "target:h7"),
      structureTransitionTerms = List("discovered_attack_relation", "d3f4"),
      exactSliceProof = exactSliceProof
    )

  private def supportedForkPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(
          PlayerFacingExactSliceProof.Fork(
            attackerSquare = "f5",
            attackerRole = "knight",
            targets =
              List(
                PlayerFacingExactSliceProof.TargetPiece("h4", "queen"),
                PlayerFacingExactSliceProof.TargetPiece("e7", "rook")
              )
          )
        )
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Fork,
      anchorTerms = List("f5", "h4", "e7"),
      ownerSeedTerms = List("fork", "attacker:f5", "target:h4:queen", "target:e7:rook"),
      structureTransitionTerms = List("fork_relation", "d4f5"),
      exactSliceProof = exactSliceProof
    )

  private def supportedHangingPiecePacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.HangingPiece("d3", "f5", "bishop", "bishop"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.HangingPiece,
      anchorTerms = List("d3", "f5"),
      ownerSeedTerms = List("hanging_piece", "attacker:d3", "target:f5", "target_role:bishop"),
      structureTransitionTerms = List("hanging_piece_relation", "c4d3"),
      exactSliceProof = exactSliceProof
    )

  private def supportedTrappedPiecePacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.TrappedPiece("a8", "knight", List("a1"), 0))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
      anchorTerms = List("a8", "a1"),
      ownerSeedTerms = List("trapped_piece", "target:a8", "target_role:knight", "attacker:a1"),
      structureTransitionTerms = List("trapped_piece_relation", "h1a1"),
      exactSliceProof = exactSliceProof
    )

  private def supportedDominationPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Domination("a1", "a8", "rook", "knight", 1))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Domination,
      anchorTerms = List("a1", "a8"),
      ownerSeedTerms = List("domination", "controller:a1", "target:a8", "target_role:knight"),
      structureTransitionTerms = List("domination_relation", "h1a1"),
      exactSliceProof = exactSliceProof
    )

  private def supportedZwischenzugPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Zwischenzug("e1e8", "check", "g8h7", "e8h5", "h5"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
      anchorTerms = List("e8", "h5"),
      ownerSeedTerms = List("zwischenzug", "intermediate_move:e1e8", "payoff_move:e8h5", "target:h5"),
      structureTransitionTerms = List("zwischenzug_relation", "e1e8", "g8h7", "e8h5"),
      exactSliceProof = exactSliceProof
    )

  private def supportedDecoyPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Decoy("f4", "d3", "d5", "e2", "d3", "knight", "queen"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Decoy,
      anchorTerms = List("f4", "d3", "d5"),
      ownerSeedTerms = List("decoy", "bait:d3", "lured_from:d5", "execution:e2-d3"),
      structureTransitionTerms = List("decoy_relation", "f4d3", "d5d3", "e2d3"),
      exactSliceProof = exactSliceProof
    )

  private def supportedXRayPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.XRay("e4", "f5", "g6", "bishop", "knight", "queen"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
      anchorTerms = List("e4", "f5", "g6"),
      ownerSeedTerms = List("xray", "attacker:e4", "blocker:f5", "target:g6"),
      structureTransitionTerms = List("xray_relation", "b1e4"),
      exactSliceProof = exactSliceProof
    )

  private def supportedClearancePacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Clearance("b1", "d3", "h7", "bishop", "f4"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Clearance,
      anchorTerms = List("b1", "d3", "h7"),
      ownerSeedTerms = List("clearance", "beneficiary:b1", "cleared_square:d3", "target:h7"),
      structureTransitionTerms = List("clearance_relation", "d3f4"),
      exactSliceProof = exactSliceProof
    )

  private def supportedBatteryPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Battery("d3", "b1", "h7", "queen", "bishop", "diagonal"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Battery,
      anchorTerms = List("d3", "b1", "h7"),
      ownerSeedTerms = List("battery", "front:d3", "back:b1", "target:h7", "axis:diagonal"),
      structureTransitionTerms = List("battery_relation", "d1d3"),
      exactSliceProof = exactSliceProof
    )

  private def supportedPinPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Pin("b4", "c3", "e1", "c3", "bishop", "knight", "king", true))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
      anchorTerms = List("b4", "c3", "e1"),
      ownerSeedTerms = List("pin", "attacker:b4", "pinned:c3", "behind:e1", "absolute_pin"),
      structureTransitionTerms = List("pin_relation", "f8b4"),
      exactSliceProof = exactSliceProof
    )

  private def supportedSkewerPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Skewer("a1", "e1", "h1", "e1", "rook", "queen", "rook"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Skewer,
      anchorTerms = List("a1", "e1", "h1"),
      ownerSeedTerms = List("skewer", "attacker:a1", "front:e1", "back:h1"),
      structureTransitionTerms = List("skewer_relation", "a8a1"),
      exactSliceProof = exactSliceProof
    )

  private def supportedInterferencePacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.Interference("d6", "d8", "d5", "knight", "rook", "queen"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.Interference,
      anchorTerms = List("d6", "d8", "d5"),
      ownerSeedTerms = List("interference", "blocker:d6", "defender:d8", "target:d5"),
      structureTransitionTerms = List("interference_relation", "f5d6"),
      exactSliceProof = exactSliceProof
    )

  private def supportedDoubleCheckPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.DoubleCheck("e8", List("e1", "f6"), "f6", "knight"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
      anchorTerms = List("e8", "e1", "f6"),
      ownerSeedTerms = List("double_check", "king:e8", "checkers:e1|f6", "mover:f6"),
      structureTransitionTerms = List("double_check_relation", "e4f6"),
      exactSliceProof = exactSliceProof
    )

  private def supportedBackRankMatePacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.BackRankMate("g8", List("e8"), "e1e8"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
      anchorTerms = List("g8", "e8"),
      ownerSeedTerms = List("back_rank_mate", "king:g8", "checker:e8", "mating_move:e1e8"),
      structureTransitionTerms = List("back_rank_mate_relation", "e1e8"),
      exactSliceProof = exactSliceProof
    )

  private def supportedMateNetPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.MateNet("h8", List("f7"), "h6f7", Some("smothered_mate")))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      anchorTerms = List("h8", "f7"),
      ownerSeedTerms = List("mate_net", "king:h8", "checker:f7", "pattern:smothered_mate"),
      structureTransitionTerms = List("mate_net_relation", "h6f7"),
      exactSliceProof = exactSliceProof
    )

  private def supportedGreekGiftPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.GreekGift("h7", "h7", "d3h7", "greek_gift"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
      anchorTerms = List("h7"),
      ownerSeedTerms = List("greek_gift", "bishop:h7", "target:h7", "entry_move:d3h7"),
      structureTransitionTerms = List("greek_gift_relation", "d3h7"),
      exactSliceProof = exactSliceProof
    )

  private def supportedStalemateTrapPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.StalemateTrap("h8", "g5g6"))
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
      anchorTerms = List("h8"),
      ownerSeedTerms = List("stalemate_trap", "king:h8", "trapping_move:g5g6"),
      structureTransitionTerms = List("stalemate_trap_relation", "g5g6"),
      exactSliceProof = exactSliceProof
    )

  private def supportedPerpetualCheckPacket(
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(
          PlayerFacingExactSliceProof.PerpetualCheck(
            kingSquare = "h7",
            checkingMoves = List("e1e8", "e8h5", "h5e8", "e8h5"),
            cycleMoves = List("e8h5", "h7g8", "h5e8", "g8h7", "e8h5"),
            repeatedPositionPly = 7
          )
        )
  ): PlayerFacingClaimPacket =
    relationTransformationPacket(
      proofSource = ProofSourceId.RelationTransformation.wireKey,
      proofFamily = ProofFamilyId.RelationTransformation.wireKey,
      triggerKind = MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
      anchorTerms = List("h7", "e8", "h5"),
      ownerSeedTerms = List("perpetual_check", "king:h7", "repeated_position_ply:7"),
      structureTransitionTerms = List("perpetual_check_relation", "e1e8", "e8h5", "h5e8"),
      exactSliceProof = exactSliceProof
    )

  private def relationTransformationPacket(
      proofSource: String,
      proofFamily: String,
      triggerKind: String,
      anchorTerms: List[String],
      ownerSeedTerms: List[String],
      structureTransitionTerms: List[String],
      exactSliceProof: Option[PlayerFacingExactSliceProof],
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable
  ): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = PlayerFacingPacketScope.MoveLocal,
      triggerKind = triggerKind,
      anchorTerms = anchorTerms,
      bestDefenseMove = Some("d4e5"),
      bestDefenseBranchKey = Some("d4e5|c6e5"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = ownerSeedTerms,
          continuationTerms = List("d4e5", "c6e5"),
          structureTransitionTerms = structureTransitionTerms,
          exactSliceProof = exactSliceProof
        ),
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
    )

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
      "On the checked line, this stops the ...c5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5")))
    )
    assertEquals(rows.head.source, None)
  }

  test("projects exact central_break_timing packet into a central break row") {
    val (centralCtx, centralInputs, centralRanked) = centralScene()

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

    assert(!rows.exists(_.label == "Central break"), clue(rows))
    assertEquals(rows.map(_.label), List("Central liquidation"))
    assertEquals(rows.head.text, "The move releases central tension through d5-e4.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralLiquidation, token = Some("...d5-e4")))
    )
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
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = inputs(),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this stops the ...c5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5")))
    )
    assertEquals(rows.head.source, None)
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
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = tokenless),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

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
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = collisionCtx,
        inputs = inputs(packet = collisionPacket),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

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
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = sanOnlyCollisionCtx,
        inputs = inputs(packet = collisionPacket),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

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
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = collisionCtx,
        inputs = inputs(packet = collisionPacket),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects route-shaped break tokens even when the played move occupies the destination square") {
    val routePacket =
      supportedNeutralizePacket(
        anchorTerms = List("e4-e5"),
        ownerSeedTerms = List("neutralize_key_break", "e4-e5"),
        structureTransitionTerms = List("e4-e5"),
        bestDefenseMove = Some("e4e5"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("e4-e5"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = playedDestinationRouteCtx,
        inputs = inputs(packet = routePacket),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this stops the e4-e5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("e4-e5")))
    )
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
    val sourceMismatch =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedNeutralizePacket(proofSource = "wrong_source")),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val releaseRisk =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedNeutralizePacket(releaseRisks = List(PlayerFacingClaimReleaseRisk.RivalRelease))),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val otherFamily =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedNeutralizePacket(proofFamily = ProofFamilyId.CounterplayRestraint.wireKey)),
        rankedPlans = ranked(neutralizePlan(proofFamily = ProofFamilyId.CounterplayRestraint.wireKey)),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(sourceMismatch, Nil)
    assertEquals(releaseRisk, Nil)
    assertEquals(otherFamily, Nil)
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
      Some("On the checked line, this stops the ...c5 break before it appears.")
    )
    assert(!rows.exists(_.text.contains("neutralize_key_break")), clue(rows))
    assert(!rows.exists(_.text.contains("counterplay_axis_suppression")), clue(rows))
    assert(!rows.exists(_.text.contains("e4d5|c6d5")), clue(rows))
  }

  test("projects proof-backed relation transformation packets as supported relation rows") {
    val defenderRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(
          packet = supportedDefenderTradePacket(),
          claimText = "raw defender_trade c5 prose must not be the release text"
        ),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val badPieceRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedBadPieceLiquidationPacket()),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(defenderRows.map(_.label), List("Tactical relation"))
    assertEquals(
      defenderRows.head.text,
      "On the checked line, this trades the defender on d4 and leaves c5 as the relation target."
    )
    assertEquals(
      defenderRows.head.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some(MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade),
          target = Some("c5")
        )
      )
    )
    assert(!defenderRows.exists(_.text.contains("defender_trade")), clue(defenderRows))

    assertEquals(badPieceRows.map(_.label), List("Tactical relation"))
    assertEquals(
      badPieceRows.head.text,
      "On the checked line, this trades the bad piece from c1 on e3."
    )
    assertEquals(
      badPieceRows.head.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some(MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation),
          target = Some("e3")
        )
      )
    )
  }

  test("projects proof-backed overload deflection and discovered-attack packets as supported relation rows") {
    val overloadRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedOverloadPacket()),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val deflectionRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedDeflectionPacket()),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val discoveredRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedDiscoveredAttackPacket()),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(overloadRows.map(_.label), List("Tactical relation"))
    assertEquals(
      overloadRows.head.text,
      "On the checked line, the defender on f6 is overloaded across d5 and h7 under pressure from d3."
    )
    assertEquals(overloadRows.head.authority.flatMap(_.token), Some(MoveReviewExchangeAnalyzer.RelationKind.Overload))
    assertEquals(overloadRows.head.authority.flatMap(_.target), Some("f6"))

    assertEquals(deflectionRows.map(_.label), List("Tactical relation"))
    assertEquals(
      deflectionRows.head.text,
      "On the checked line, the attack from a3 deflects the defender on f8 away from g7."
    )
    assertEquals(deflectionRows.head.authority.flatMap(_.token), Some(MoveReviewExchangeAnalyzer.RelationKind.Deflection))
    assertEquals(deflectionRows.head.authority.flatMap(_.target), Some("g7"))

    assertEquals(discoveredRows.map(_.label), List("Tactical relation"))
    assertEquals(
      discoveredRows.head.text,
      "On the checked line, the bishop on b1 is uncovered through d3 toward h7."
    )
    assertEquals(discoveredRows.head.authority.flatMap(_.token), Some(MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack))
    assertEquals(discoveredRows.head.authority.flatMap(_.target), Some("h7"))
  }

  test("projects proof-backed material-target relation packets as supported relation rows") {
    val forkRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedForkPacket()),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val hangingRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedHangingPiecePacket()),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val trappedRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedTrappedPiecePacket()),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val dominationRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedDominationPacket()),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(forkRows.map(_.label), List("Tactical relation"))
    assertEquals(
      forkRows.head.text,
      "On the checked line, the knight on f5 forks the queen on h4 and the rook on e7."
    )
    assertEquals(forkRows.head.authority.flatMap(_.token), Some(MoveReviewExchangeAnalyzer.RelationKind.Fork))
    assertEquals(forkRows.head.authority.flatMap(_.target), Some("h4"))

    assertEquals(hangingRows.map(_.label), List("Tactical relation"))
    assertEquals(
      hangingRows.head.text,
      "On the checked line, the bishop on d3 attacks the undefended bishop on f5."
    )
    assertEquals(hangingRows.head.authority.flatMap(_.token), Some(MoveReviewExchangeAnalyzer.RelationKind.HangingPiece))
    assertEquals(hangingRows.head.authority.flatMap(_.target), Some("f5"))

    assertEquals(trappedRows.map(_.label), List("Tactical relation"))
    assertEquals(
      trappedRows.head.text,
      "On the checked line, the knight on a8 has no safe escape against pressure from a1."
    )
    assertEquals(trappedRows.head.authority.flatMap(_.token), Some(MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece))
    assertEquals(trappedRows.head.authority.flatMap(_.target), Some("a8"))

    assertEquals(dominationRows.map(_.label), List("Tactical relation"))
    assertEquals(
      dominationRows.head.text,
      "On the checked line, the rook on a1 restricts the knight on a8 to 1 legal move."
    )
    assertEquals(dominationRows.head.authority.flatMap(_.token), Some(MoveReviewExchangeAnalyzer.RelationKind.Domination))
    assertEquals(dominationRows.head.authority.flatMap(_.target), Some("a8"))
  }

  test("projects proof-backed line and move-order relation packets as supported relation rows") {
    def rows(packet: PlayerFacingClaimPacket): List[MoveReviewPlayerSurfaceRow] =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = packet),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    val cases =
      List(
        (
          rows(supportedZwischenzugPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
          "h5",
          "On the checked line, e1-e8 is the forcing in-between move before e8-h5 wins h5."
        ),
        (
          rows(supportedDecoyPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.Decoy,
          "d3",
          "On the checked line, the knight from f4 lures the queen from d5 onto d3 before e2-d3 wins it."
        ),
        (
          rows(supportedXRayPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.XRay,
          "g6",
          "On the checked line, the bishop on e4 x-rays the queen on g6 through the knight on f5."
        ),
        (
          rows(supportedClearancePacket()),
          MoveReviewExchangeAnalyzer.RelationKind.Clearance,
          "h7",
          "On the checked line, f4 clears d3 so the bishop on b1 reaches h7."
        ),
        (
          rows(supportedBatteryPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.Battery,
          "h7",
          "On the checked line, the queen on d3 and the bishop on b1 form a diagonal battery toward h7."
        ),
        (
          rows(supportedPinPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.Pin,
          "c3",
          "On the checked line, the bishop on b4 pins the knight on c3 to the king on e1."
        ),
        (
          rows(supportedSkewerPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.Skewer,
          "e1",
          "On the checked line, the rook on a1 skewers the queen on e1 in front of the rook on h1."
        ),
        (
          rows(supportedInterferencePacket()),
          MoveReviewExchangeAnalyzer.RelationKind.Interference,
          "d5",
          "On the checked line, the knight on d6 interferes with the rook on d8 defending the queen on d5."
        )
      )

    cases.foreach { case (surfaceRows, token, target, text) =>
      assertEquals(surfaceRows.map(_.label), List("Tactical relation"))
      assertEquals(surfaceRows.head.text, text)
      assertEquals(surfaceRows.head.authority.flatMap(_.token), Some(token))
      assertEquals(surfaceRows.head.authority.flatMap(_.target), Some(target))
    }
  }

  test("projects proof-backed king and pattern relation packets as supported relation rows") {
    def rows(packet: PlayerFacingClaimPacket): List[MoveReviewPlayerSurfaceRow] =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = packet),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    val cases =
      List(
        (
          rows(supportedDoubleCheckPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
          "e8",
          "On the checked line, the knight on f6 gives double check on e8 with e1 and f6."
        ),
        (
          rows(supportedBackRankMatePacket()),
          MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
          "g8",
          "On the checked line, e1-e8 gives back-rank mate on g8."
        ),
        (
          rows(supportedMateNetPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.MateNet,
          "h8",
          "On the checked line, h6-f7 gives a smothered_mate mate net on h8."
        ),
        (
          rows(supportedGreekGiftPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
          "h7",
          "On the checked line, d3-h7 is a Greek gift entry on h7."
        ),
        (
          rows(supportedStalemateTrapPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
          "h8",
          "On the checked line, g5-g6 leaves the king on h8 stalemated."
        ),
        (
          rows(supportedPerpetualCheckPacket()),
          MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
          "h7",
          "On the checked line, the checks on h7 repeat after 7 plies."
        )
      )

    cases.foreach { case (surfaceRows, token, target, text) =>
      assertEquals(surfaceRows.map(_.label), List("Tactical relation"))
      assertEquals(surfaceRows.head.text, text)
      assertEquals(surfaceRows.head.authority.flatMap(_.token), Some(token))
      assertEquals(surfaceRows.head.authority.flatMap(_.target), Some(target))
    }
  }

  test("relation transformation rows require typed exact-slice proof and stable branch evidence") {
    val missingProofRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedDefenderTradePacket(exactSliceProof = None)),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val releaseRiskRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs =
          inputs(packet = supportedDefenderTradePacket(releaseRisks = List(PlayerFacingClaimReleaseRisk.RivalRelease))),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val unstableRows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs =
          inputs(packet = supportedDefenderTradePacket(persistence = PlayerFacingClaimPersistence.BestDefenseOnly)),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(missingProofRows, Nil)
    assertEquals(releaseRiskRows, Nil)
    assertEquals(unstableRows, Nil)
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
