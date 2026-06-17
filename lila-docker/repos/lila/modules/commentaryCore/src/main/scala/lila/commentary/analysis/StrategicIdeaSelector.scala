package lila.commentary.analysis

import _root_.chess.{ Bishop, Board, Color, File, Knight, Pawn, Rank, Role, Square }

import lila.commentary.analysis.L3.{ PawnPlayAnalysis, ThreatAnalysis }
import lila.commentary.analysis.semantic.StrategicObservationIds.{
  EvidenceRef,
  EvidenceSourceId,
  FactId,
  SemanticObservationId
}
import lila.commentary.analysis.semantic.{
  RelationObservationCatalog,
  StrategicIdeaEvidence,
  StrategicIdeaEvidencePipeline,
  StrategicSemanticObservation
}
import lila.commentary.analysis.semantic.StrategicSemanticObservationPipeline
import lila.commentary.*
import lila.commentary.model.{ Motif, PlanId, PlanMatch, StrategicPlanExperiment }
import lila.commentary.model.strategic.{ PositionalTag, PreventedPlan, WeakComplex }
import lila.commentary.model.structure.{ CenterState, StructureId }

private[commentary] object StrategicIdeaSelector:

  private val ChessSquarePattern = "^[a-h][1-8]$".r
  private val NonAlphaNumPattern = "[^a-z0-9]+".r

  private final case class Candidate(
      ownerSide: String,
      kind: String,
      group: String,
      readiness: String,
      focusSquares: List[String] = Nil,
      focusFiles: List[String] = Nil,
      focusDiagonals: List[String] = Nil,
      focusZone: Option[String] = None,
      targetSquare: Option[String] = None,
      relationKind: Option[String] = None,
      relationFocusSquares: List[String] = Nil,
      beneficiaryPieces: List[String] = Nil,
      score: Double,
      evidenceRefs: List[EvidenceRef] = Nil,
      evidenceCount: Int = 0,
      sourceCount: Int = 0
  )

  private final case class FamilyCandidate(
      family: String,
      score: Double,
      members: List[Candidate]
  )

  private final case class DerivedCompensationCarrier(
      summary: Option[String],
      vectors: List[String],
      investedMaterial: Option[Int]
  ):
    def hasSignal: Boolean =
      summary.exists(_.nonEmpty) || vectors.nonEmpty || investedMaterial.exists(_ > 0)

  private object StrategicIdeaFamily:
    val ForcingOrTacticalNow = "forcing_or_tactical_now"
    val SlowStructural = "slow_structural"
    val PreventionOrSuppression = "prevention_or_suppression"
    val ConversionOrTransformation = "conversion_or_transformation"

  private enum ExperimentEvidenceTier:
    case EvidenceBacked, PvCoupled, Deferred, Refuted, Other

  private def experimentTier(experiment: StrategicPlanExperiment): ExperimentEvidenceTier =
    experiment.evidenceTier match
      case "evidence_backed" => ExperimentEvidenceTier.EvidenceBacked
      case "pv_coupled"      => ExperimentEvidenceTier.PvCoupled
      case "deferred"        => ExperimentEvidenceTier.Deferred
      case "refuted"         => ExperimentEvidenceTier.Refuted
      case _                 => ExperimentEvidenceTier.Other

  private def isRefutedExperiment(experiment: StrategicPlanExperiment): Boolean =
    experimentTier(experiment) == ExperimentEvidenceTier.Refuted

  private def isPlayableExperiment(experiment: StrategicPlanExperiment): Boolean =
    !isRefutedExperiment(experiment)

  def enrich(pack: StrategyPack): StrategyPack =
    enrich(pack, StrategicIdeaSemanticContext.empty(pack.sideToMove))

  def enrich(pack: StrategyPack, semantic: StrategicIdeaSemanticContext): StrategyPack =
    val ideas = select(pack, semantic)
    if ideas.isEmpty then pack
    else
      val enrichedDigest = enrichDigest(pack, pack.signalDigest, ideas, semantic)
      val enrichedFocus = enrichLongTermFocus(pack.longTermFocus, ideas, pack.directionalTargets, enrichedDigest)
      val enrichedEvidence = enrichEvidence(pack.evidence, ideas, pack.directionalTargets)
      pack.copy(
        strategicIdeas = ideas,
        longTermFocus = enrichedFocus,
        evidence = enrichedEvidence,
        signalDigest = enrichedDigest
      )

  def select(pack: StrategyPack): List[StrategyIdeaSignal] =
    select(pack, StrategicIdeaSemanticContext.empty(pack.sideToMove))

  def select(pack: StrategyPack, semantic: StrategicIdeaSemanticContext): List[StrategyIdeaSignal] =
    val evidence = collectTypedEvidence(pack, semantic)
    if evidence.isEmpty then Nil
    else
      val merged = mergeEvidence(evidence, semantic)
      val familyRanking = rankFamilies(merged, semantic)
      val selectedFamilies = selectFamilies(familyRanking)
      val resolved = stageCandidates(merged, selectedFamilies, semantic)
      selectedFamilies.headOption.toList.flatMap { dominantFamily =>
        val dominantCandidates = resolved.filter(candidate => familyForKind(candidate.kind) == dominantFamily)
        val dominantCandidate =
          if dominantFamily == StrategicIdeaFamily.SlowStructural then
            preferredSlowStructuralKind(dominantCandidates, semantic)
              .flatMap(kind => dominantCandidates.find(_.kind == kind))
              .orElse(dominantCandidates.headOption)
          else dominantCandidates.headOption
        dominantCandidate.toList.flatMap { dominant =>
          val secondary =
            selectedFamilies
              .drop(1)
              .headOption
              .flatMap(family =>
                resolved
                  .filter(candidate => familyForKind(candidate.kind) == family)
                  .find(candidate =>
                    candidate.group != dominant.group &&
                      math.abs(dominant.score - candidate.score) <= 0.12
                  )
              )
              .orElse(
                dominantCandidates.filterNot(_ == dominant).find(candidate =>
                  candidate.group != dominant.group &&
                    math.abs(dominant.score - candidate.score) <= 0.12
                )
              )
          List(Some(dominant), secondary).flatten.zipWithIndex.map { case (candidate, idx) =>
            StrategyIdeaSignal(
              ideaId = s"idea_${idx + 1}",
              ownerSide = candidate.ownerSide,
              kind = candidate.kind,
              group = candidate.group,
              readiness = candidate.readiness,
              focusSquares = candidate.focusSquares,
              focusFiles = candidate.focusFiles,
              focusDiagonals = candidate.focusDiagonals,
              focusZone = candidate.focusZone,
              beneficiaryPieces = candidate.beneficiaryPieces.distinct,
              confidence = candidate.score.min(0.98),
              evidenceRefs = surfaceEvidenceRefs(candidate),
              targetSquare = candidate.targetSquare,
              relationKind = candidate.relationKind,
              relationFocusSquares = candidate.relationFocusSquares
            )
          }
        }
      }

  def humanizedKind(kind: String): String =
    kind match
      case StrategicIdeaKind.PawnBreak                       => "pawn break"
      case StrategicIdeaKind.SpaceGainOrRestriction          => "space"
      case StrategicIdeaKind.TargetFixing                    => "fixed targets"
      case StrategicIdeaKind.LineOccupation                  => "open-line pressure"
      case StrategicIdeaKind.OutpostCreationOrOccupation     => "an outpost"
      case StrategicIdeaKind.MinorPieceImbalanceExploitation => "the minor-piece imbalance"
      case StrategicIdeaKind.Prophylaxis                     => "prophylaxis"
      case StrategicIdeaKind.KingAttackBuildUp               => "attacking chances"
      case StrategicIdeaKind.FavorableTradeOrTransformation  => "favorable exchanges"
      case StrategicIdeaKind.CounterplaySuppression          => "stopping counterplay"
      case other                                             => other.replace('_', ' ')

  def playerFacingIdeaText(signal: StrategyIdeaSignal): String =
    signal.kind match
      case StrategicIdeaKind.PawnBreak =>
        pawnBreakText(signal.ownerSide, signal.focusSquares, signal.focusFiles, signal.focusZone)
      case StrategicIdeaKind.KingAttackBuildUp =>
        pressureText(signal.focusSquares, signal.focusFiles, signal.focusDiagonals, signal.focusZone, fallback = "attacking chances")
      case StrategicIdeaKind.LineOccupation =>
        pressureText(signal.focusSquares, signal.focusFiles, signal.focusDiagonals, signal.focusZone, fallback = "open-line pressure")
      case StrategicIdeaKind.FavorableTradeOrTransformation =>
        exchangeText(signal)
      case StrategicIdeaKind.TargetFixing =>
        targetFixingText(signal.focusSquares, signal.focusZone)
      case StrategicIdeaKind.OutpostCreationOrOccupation =>
        outpostText(signal)
      case StrategicIdeaKind.SpaceGainOrRestriction =>
        spaceText(signal.focusSquares, signal.focusZone)
      case StrategicIdeaKind.CounterplaySuppression =>
        counterplayText(signal.focusSquares, signal.focusFiles, signal.focusDiagonals, signal.focusZone)
      case StrategicIdeaKind.Prophylaxis =>
        prophylaxisText(signal.focusSquares, signal.focusFiles, signal.focusDiagonals, signal.focusZone)
      case StrategicIdeaKind.MinorPieceImbalanceExploitation =>
        minorPieceText(signal.focusSquares, signal.focusZone)
      case other =>
        val label = humanizedKind(other)
        val focus = focusSummary(signal)
        if focus.nonEmpty && focus != "the key sector" then s"$label ${focusJoiner(focus)}" else label

  private val PrioritySupportEvidenceRefs =
    List(
      EvidenceRef.Source(EvidenceSourceId.MinorityAttackSemantic).wireKey,
      EvidenceRef.Fact(FactId.semantic(SemanticObservationId.TargetPressureSemantic)).wireKey
    )

  private val FrenchProfilePriorityEvidenceRefs =
    List(
      EvidenceRef.Source(EvidenceSourceId.FrenchMinorPieceProfile).wireKey,
      "structure_french_advance_chain"
    )

  private val LockedCenterPriorityEvidenceRefs =
    List(
      EvidenceRef.Source(EvidenceSourceId.LockedCenterBind).wireKey,
      "structure_locked_center"
    )

  private def prioritySupportEvidenceRefs(refs: List[String]): List[String] =
    val minorityPriority = PrioritySupportEvidenceRefs.filter(refs.contains)
    val minoritySupportPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.MinorityAttackSupport).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.MinorityAttackSupport).wireKey ::
            refs.filter(_.startsWith("minority_attack_support_"))
        }
        .toList
        .flatten
    val targetBridgePriority =
      List(
        EvidenceRef.Source(EvidenceSourceId.PlanMatchTargetFixing).wireKey,
        EvidenceRef.Source(EvidenceSourceId.DirectionalTargetFixation).wireKey,
        EvidenceRef.Source(EvidenceSourceId.CarlsbadFixationProfile).wireKey
      ).filter(refs.contains) ++
        refs.filter(ref => ref.startsWith("directional_target_") || ref == "structure_carlsbad")
    val weakSquarePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.EnemyWeakSquare).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.EnemyWeakSquare).wireKey ::
            refs.filter(_.startsWith("enemy_weak_square_"))
        }
        .toList
        .flatten
    val weakComplexPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.WeakComplexFixation).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.WeakComplexFixation).wireKey ::
            refs.filter(ref => ref.startsWith("weak_complex_") && ref != "weak_complex_fixation")
        }
        .toList
        .flatten
    val doubledPawnPressurePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.DoubledPawnPressureMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.DoubledPawnPressureMotif).wireKey ::
            refs.filter(ref => ref == "doubled_pawn_pressure_shape" || ref.startsWith("doubled_pawn_file_"))
        }
        .toList
        .flatten
    val pawnBreakReadyPriority =
      Option
        .when(
          refs.contains(EvidenceRef.Source(EvidenceSourceId.PawnAnalysisBreakReady).wireKey) ||
            refs.contains(EvidenceRef.Source(EvidenceSourceId.PawnPlayBreakReady).wireKey)
        ) {
          List(
            EvidenceRef.Source(EvidenceSourceId.PawnAnalysisBreakReady).wireKey,
            EvidenceRef.Source(EvidenceSourceId.PawnPlayBreakReady).wireKey,
            "pawn_analysis_break_ready_shape",
            "pawn_play_break_ready_shape"
          ).filter(refs.contains) ++ refs.filter(_.startsWith("break_file_"))
        }
        .toList
        .flatten
    val pawnBreakMotifPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.PawnBreakMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.PawnBreakMotif).wireKey ::
            refs.filter(ref =>
              ref == "pawn_break_motif_shape" ||
                ref.startsWith("break_file_") ||
                ref.startsWith("break_target_file_")
            )
        }
        .toList
        .flatten
    val centralBreakTensionPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.CentralBreakTension).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.CentralBreakTension).wireKey ::
            refs.filter(_ == "locked_center")
        }
        .toList
        .flatten
    val pawnChainSpacePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.PawnChainSpaceMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.PawnChainSpaceMotif).wireKey ::
            refs.filter(ref => ref == "pawn_chain_space_shape" || ref.startsWith("pawn_chain_"))
        }
        .toList
        .flatten
    val fileOpeningPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.FileOpeningConsequence).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.FileOpeningConsequence).wireKey ::
            refs.filter(ref => ref == "file_opening_consequence" || ref.startsWith("contested_file_"))
        }
        .toList
        .flatten
    val frenchF6BreakSeedPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.FrenchF6BreakSeed).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.FrenchF6BreakSeed).wireKey ::
            refs.filter(ref =>
              ref == "french_f6_break_seed_shape" ||
                ref == "white_e5_chain" ||
                ref == "black_f7_break_pawn"
            )
        }
        .toList
        .flatten
    val rookEndgamePatternPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.RookEndgamePattern).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.RookEndgamePattern).wireKey ::
            refs.filter(ref =>
              ref == "rook_endgame_pattern_shape" ||
                ref == "rook_behind_passed_pawn" ||
                ref == "king_cut_off" ||
                ref.startsWith("rook_behind_passer_square_")
            )
        }
        .toList
        .flatten
    val endgameTechniqueMotifPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.EndgameTechniqueMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.EndgameTechniqueMotif).wireKey ::
            refs.filter(ref =>
              ref == "endgame_technique_shape" ||
                ref.startsWith("opposition_") ||
                ref == "zugzwang_shape" ||
                ref == "king_activity_shape"
            )
        }
        .toList
        .flatten
    val passedPawnConversionPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.PassedPawnConversionMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.PassedPawnConversionMotif).wireKey ::
            refs.filter(ref =>
              ref == "passed_pawn_conversion_shape" ||
                ref.startsWith("passed_pawn_") ||
                ref == "pawn_promotion" ||
                ref.startsWith("promotion_piece_") ||
                ref == "underpromotion" ||
                ref == "protected_passed_pawn" ||
                ref == "advanced_passed_pawn"
            )
        }
        .toList
        .flatten
    val compensationDiagonalPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.CompensationDiagonalBattery).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.CompensationDiagonalBattery).wireKey ::
            refs.filter(ref =>
              ref == "compensation_diagonal_battery" ||
                ref == "material_deficit_compensation" ||
                ref == "bishop_pair_compensation" ||
                ref == "battery_axis_diagonal" ||
                ref.startsWith("compensation_battery_square_")
            )
        }
        .toList
        .flatten
    val compensationKingAttackPriority =
      Option
        .when(
          refs.contains(EvidenceRef.Source(EvidenceSourceId.CompensationDevelopmentLead).wireKey) ||
            refs.contains(EvidenceRef.Source(EvidenceSourceId.CompensationKingWindow).wireKey)
        ) {
          List(
            EvidenceRef.Source(EvidenceSourceId.CompensationDevelopmentLead).wireKey,
            EvidenceRef.Source(EvidenceSourceId.CompensationKingWindow).wireKey,
            EvidenceRef.Source(EvidenceSourceId.CompensationDiagonalBattery).wireKey,
            "material_deficit_compensation",
            "development_lead_compensation",
            "uncastled_or_unsettled_king_window",
            "compensation_diagonal_battery"
          ).filter(refs.contains)
        }
        .toList
        .flatten
    val compensationLinePriority =
      Option
        .when(
          refs.contains(EvidenceRef.Source(EvidenceSourceId.CompensationOpenLines).wireKey) ||
            refs.contains(EvidenceRef.Source(EvidenceSourceId.DelayedRecoveryWindow).wireKey)
        ) {
          List(
            EvidenceRef.Source(EvidenceSourceId.CompensationOpenLines).wireKey,
            EvidenceRef.Source(EvidenceSourceId.DelayedRecoveryWindow).wireKey,
            "compensation_open_lines_shape",
            "delayed_material_recovery",
            "material_deficit_compensation",
            "development_lead_compensation"
          ).filter(refs.contains) ++
            refs.filter(ref => ref.startsWith("open_file_") || ref.startsWith("semi_open_file_") || ref == "seventh_rank_entry")
        }
        .toList
        .flatten
    val routeAttackLanePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.RouteAttackLane).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.RouteAttackLane).wireKey ::
            refs.filter(ref => ref == "route_attack_lane_shape" || ref == "route_surface_exact")
        }
        .toList
        .flatten
    val directionalAttackLanePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.DirectionalAttackLane).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.DirectionalAttackLane).wireKey ::
            refs.filter(_ == "directional_attack_lane_shape")
        }
        .toList
        .flatten
    val motifRookLiftPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.MotifRookLift).wireKey)) {
          List(EvidenceRef.Source(EvidenceSourceId.MotifRookLift).wireKey)
        }
        .toList
        .flatten
    val motifPieceLiftPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.MotifPieceLift).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.MotifPieceLift).wireKey :: refs.filter(_ == "motif_piece_lift_shape")
        }
        .toList
        .flatten
    val motifCheckPressurePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.MotifCheckPressure).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.MotifCheckPressure).wireKey ::
            refs.filter(ref => ref == "check_type_normal" || ref == "check_type_discovered")
        }
        .toList
        .flatten
    val fianchettoMotifPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.FianchettoMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.FianchettoMotif).wireKey ::
            refs.filter(ref => ref == "fianchetto_motif_shape" || ref.startsWith("fianchetto_side_"))
        }
        .toList
        .flatten
    val motifBatteryPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.MotifBattery).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.MotifBattery).wireKey ::
            refs.filter(ref => ref == "battery_axis_diagonal" || ref == "battery_axis_file")
        }
        .toList
        .flatten
    val routeLinePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.RouteLineAccess).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.RouteLineAccess).wireKey ::
            refs.filter(ref => ref == "route_surface_exact" || ref.startsWith("open_file_") || ref.startsWith("semi_open_file_"))
        }
        .toList
        .flatten
    val fianchettoAssaultPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.FianchettoAssaultProfile).wireKey)) {
          List(
            EvidenceRef.Source(EvidenceSourceId.FianchettoAssaultProfile).wireKey,
            EvidenceRef.Source(EvidenceSourceId.OppositeSideStorm).wireKey,
            "structure_fianchetto_shell"
          ).filter(refs.contains)
        }
        .toList
        .flatten
    val kingRingPressurePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.KingRingPressure).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.KingRingPressure).wireKey ::
            refs.filter(ref => ref == "king_ring_pressure_shape" || ref == "king_exposed_files")
        }
        .toList
        .flatten
    val enemyKingStuckCenterPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.EnemyKingStuckCenter).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.EnemyKingStuckCenter).wireKey ::
            refs.filter(_ == "enemy_king_central_exposure")
        }
        .toList
        .flatten
    val enemyWeakBackRankPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.EnemyWeakBackRank).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.EnemyWeakBackRank).wireKey ::
            refs.filter(_ == "enemy_weak_back_rank_shape")
        }
        .toList
        .flatten
    val flankPawnPressurePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.FlankPawnPressure).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.FlankPawnPressure).wireKey ::
            refs.filter(ref => ref == "hook_creation_chance" || ref == "rook_pawn_march_ready")
        }
        .toList
        .flatten
    val flankPawnAdvanceMotifPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.FlankPawnAdvanceMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.FlankPawnAdvanceMotif).wireKey ::
            refs.filter(ref =>
              ref == "flank_pawn_advance_shape" ||
                ref.startsWith("flank_pawn_file_") ||
                ref.startsWith("flank_pawn_to_rank_")
            )
        }
        .toList
        .flatten
    val opponentCounterbreakDenialPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.OpponentCounterbreakDenial).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.OpponentCounterbreakDenial).wireKey ::
            refs.filter(_ == "opponent_counter_break")
        }
        .toList
        .flatten
    val passerBlockadePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.PasserBlockadeMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.PasserBlockadeMotif).wireKey ::
            refs.filter(ref =>
              ref == "passer_blockade_shape" ||
                ref.startsWith("blockade_square_") ||
                ref.startsWith("blockaded_pawn_")
            )
        }
        .toList
        .flatten
    val hedgehogBreakDenialPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.HedgehogBreakDenialGeometry).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.HedgehogBreakDenialGeometry).wireKey ::
            refs.filter(ref => ref == "structure_hedgehog" || ref == "hedgehog_break_denial_shape")
        }
        .toList
        .flatten
    val maroczyBreakDenialPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.MaroczyBreakDenialGeometry).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.MaroczyBreakDenialGeometry).wireKey ::
            refs.filter(ref => ref == "structure_maroczy_bind" || ref == "maroczy_break_denial_shape")
        }
        .toList
        .flatten
    val counterplaySuppressionPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.CounterplaySuppression).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.CounterplaySuppression).wireKey ::
            refs.filter(ref =>
              ref == "counterplay_suppression_shape" ||
                ref == "counterplay_break_denial" ||
                ref == "break_neutralized" ||
                ref == "denied_break_resource" ||
                ref == "counterplay_score_drop"
            )
        }
        .toList
        .flatten
    val compensationCounterplayDenialPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.CompensationCounterplayDenial).wireKey)) {
          List(
            EvidenceRef.Source(EvidenceSourceId.CompensationCounterplayDenial).wireKey,
            "material_deficit_compensation",
            "break_neutralized"
          ).filter(refs.contains)
        }
        .toList
        .flatten
    val doubledRooksPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.DoubledRooks).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.DoubledRooks).wireKey ::
            refs.filter(_.startsWith("doubled_rooks_"))
        }
        .toList
        .flatten
    val rookOnSeventhPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.RookOnSeventh).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.RookOnSeventh).wireKey ::
            refs.filter(_ == "rook_on_seventh_shape")
        }
        .toList
        .flatten
    val openFilePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.OpenFileControl).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.OpenFileControl).wireKey ::
            refs.filter(_.startsWith("open_file_"))
        }
        .toList
        .flatten
    val semiOpenFilePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.SemiOpenFileControl).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.SemiOpenFileControl).wireKey ::
            refs.filter(_.startsWith("semi_open_file_"))
        }
        .toList
        .flatten
    val occupiedLinePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.OccupiedLineControl).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.OccupiedLineControl).wireKey ::
            refs.filter(ref =>
              ref.startsWith("occupied_") ||
                ref.startsWith("open_file_") ||
                ref.startsWith("semi_open_file_")
            )
        }
        .toList
        .flatten
    val directionalLinePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.DirectionalLineAccess).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.DirectionalLineAccess).wireKey ::
            refs.filter(ref => ref == "directional_line_access_shape" || ref.startsWith("open_file_") || ref.startsWith("semi_open_file_"))
        }
        .toList
        .flatten
    val lineControlPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.LineControlFeatures).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.LineControlFeatures).wireKey ::
            refs.filter(_ == "line_control_shape")
        }
        .toList
        .flatten
    val connectedRooksPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.ConnectedRooks).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.ConnectedRooks).wireKey ::
            refs.filter(_ == "connected_rooks_shape")
        }
        .toList
        .flatten
    val strongKnightOutpostPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.StrongKnight).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.StrongKnight).wireKey ::
            refs.filter(_.startsWith("strong_knight_"))
        }
        .toList
        .flatten
    val routeOutpostPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.RouteOutpostAccess).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.RouteOutpostAccess).wireKey ::
            refs.filter(ref => ref == "route_outpost_access_shape" || ref == "route_surface_exact")
        }
        .toList
        .flatten
    val directionalOutpostPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.DirectionalOutpostAccess).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.DirectionalOutpostAccess).wireKey ::
            refs.filter(_ == "directional_outpost_access_shape")
        }
        .toList
        .flatten
    val strongKnightVsBishopPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.StrongKnightVsBadBishop).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.StrongKnightVsBadBishop).wireKey :: refs.filter(_.startsWith("strong_knight_"))
        }
        .toList
        .flatten
    val knightVsBishopMotifPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.KnightVsBishopMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.KnightVsBishopMotif).wireKey ::
            refs.filter(ref => ref == "knight_vs_bishop_motif_shape" || ref == "knight_preferred_over_bishop")
        }
        .toList
        .flatten
    val bishopPairPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.BishopPairAdvantage).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.BishopPairAdvantage).wireKey ::
            refs.filter(_ == "bishop_pair_advantage_shape")
        }
        .toList
        .flatten
    val oppositeColorBishopsPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.OppositeColorBishops).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.OppositeColorBishops).wireKey ::
            refs.filter(_ == "opposite_color_bishops_shape")
        }
        .toList
        .flatten
    val enemyBadBishopPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.EnemyBadBishop).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.EnemyBadBishop).wireKey ::
            refs.filter(_ == "enemy_bad_bishop_shape")
        }
        .toList
        .flatten
    val goodBishopCountPriority =
      Option.when(
        refs.contains(EvidenceRef.Source(EvidenceSourceId.GoodBishop).wireKey) &&
          refs.contains(EvidenceRef.Source(EvidenceSourceId.MinorPieceCountImbalance).wireKey)
      ) {
        List(
          EvidenceRef.Source(EvidenceSourceId.GoodBishop).wireKey,
          "good_bishop_shape",
          EvidenceRef.Source(EvidenceSourceId.MinorPieceCountImbalance).wireKey,
          "minor_piece_count_imbalance_shape",
          "good_bishop_count_edge"
        ).filter(refs.contains)
      }.toList.flatten
    val iqpSimplificationPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.IqpSimplificationProfile).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.IqpSimplificationProfile).wireKey ::
            refs.filter(ref =>
              ref == "structure_iqp_black" ||
                ref == "capture_or_exchange" ||
                ref == "iqp_trade_down_plan"
            )
        }
        .toList
        .flatten
    val exchangeAvailabilityPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.ExchangeAvailabilityBridge).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.ExchangeAvailabilityBridge).wireKey ::
            refs.filter(_ == "structure_iqp_black")
        }
        .toList
        .flatten
    val badBishopActivityPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.PieceActivityBadBishop).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.PieceActivityBadBishop).wireKey :: refs.filter(_.startsWith("enemy_bad_bishop_"))
        }
        .toList
        .flatten
    val pieceCentralizationPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.PieceCentralizationMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.PieceCentralizationMotif).wireKey ::
            refs.filter(ref => ref == "piece_centralization_shape" || ref.startsWith("centralized_piece_"))
        }
        .toList
        .flatten
    val pieceManeuverPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.PieceManeuverMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.PieceManeuverMotif).wireKey ::
            refs.filter(_.startsWith("piece_maneuver_"))
        }
        .toList
        .flatten
    val colorComplexClampPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.ColorComplexClamp).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.ColorComplexClamp).wireKey ::
            refs.filter(ref =>
              ref == "enemy_color_complex_weakness" ||
                ref == "color_complex_dark" ||
                ref == "color_complex_light"
            )
        }
        .toList
        .flatten
    val maroczyProfilePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.MaroczyBindProfile).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.MaroczyBindProfile).wireKey ::
            refs.filter(_ == "structure_maroczy_bind")
        }
        .toList
        .flatten
    val spaceAdvantageMotifPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.SpaceAdvantageMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.SpaceAdvantageMotif).wireKey ::
            refs.filter(ref => ref == "space_advantage_motif_shape" || ref.startsWith("space_pawn_delta_"))
        }
        .toList
        .flatten
    val centralPawnAdvancePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.CentralPawnAdvanceMotif).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.CentralPawnAdvanceMotif).wireKey ::
            refs.filter(ref =>
              ref == "central_pawn_advance_shape" ||
                ref.startsWith("central_pawn_file_") ||
                ref.startsWith("central_pawn_to_rank_")
            )
        }
        .toList
        .flatten
    val frenchMinorPriority =
      Option.when(FrenchProfilePriorityEvidenceRefs.forall(refs.contains)) {
        FrenchProfilePriorityEvidenceRefs ++ strongKnightVsBishopPriority ++ badBishopActivityPriority
      }.toList.flatten
    val lockedCenterPriority =
      Option.when(LockedCenterPriorityEvidenceRefs.forall(refs.contains))(LockedCenterPriorityEvidenceRefs).toList.flatten
    val iqpSpaceBridgePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.IqpSpaceBridge).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.IqpSpaceBridge).wireKey ::
            refs.filter(_ == "structure_iqp_white")
        }
        .toList
        .flatten
    val iqpCentralPresencePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.IqpCentralPresence).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.IqpCentralPresence).wireKey ::
            refs.filter(ref => ref == "structure_iqp_white" || ref == "iqp_central_presence_shape")
        }
        .toList
        .flatten
    val centralSpacePriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.CentralSpaceEdge).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.CentralSpaceEdge).wireKey ::
            refs.filter(ref => ref == "central_space_edge_shape" || ref.startsWith("central_space_diff_"))
        }
        .toList
        .flatten
    val mobilityRestrictionPriority =
      Option
        .when(refs.contains(EvidenceRef.Source(EvidenceSourceId.MobilityRestriction).wireKey)) {
          EvidenceRef.Source(EvidenceSourceId.MobilityRestriction).wireKey ::
            refs.filter(ref =>
              ref == "mobility_restriction_shape" ||
                ref.startsWith("mobility_restriction_gap_") ||
                ref.startsWith("enemy_low_mobility_pieces_") ||
                ref.startsWith("own_low_mobility_pieces_")
            )
        }
        .toList
        .flatten
    (
      minorityPriority ++
        minoritySupportPriority ++
        targetBridgePriority ++
        weakSquarePriority ++
        weakComplexPriority ++
        doubledPawnPressurePriority ++
        pawnChainSpacePriority ++
        fileOpeningPriority ++
        frenchF6BreakSeedPriority ++
        pawnBreakReadyPriority ++
        pawnBreakMotifPriority ++
        centralBreakTensionPriority ++
        rookEndgamePatternPriority ++
        endgameTechniqueMotifPriority ++
        passedPawnConversionPriority ++
        compensationDiagonalPriority ++
        compensationKingAttackPriority ++
        compensationLinePriority ++
        fianchettoAssaultPriority ++
        routeAttackLanePriority ++
        directionalAttackLanePriority ++
        motifRookLiftPriority ++
        motifPieceLiftPriority ++
        motifCheckPressurePriority ++
        fianchettoMotifPriority ++
        motifBatteryPriority ++
        routeLinePriority ++
        kingRingPressurePriority ++
        enemyKingStuckCenterPriority ++
        enemyWeakBackRankPriority ++
        flankPawnPressurePriority ++
        flankPawnAdvanceMotifPriority ++
        opponentCounterbreakDenialPriority ++
        passerBlockadePriority ++
        hedgehogBreakDenialPriority ++
        maroczyBreakDenialPriority ++
        compensationCounterplayDenialPriority ++
        counterplaySuppressionPriority ++
        doubledRooksPriority ++
        rookOnSeventhPriority ++
        occupiedLinePriority ++
        openFilePriority ++
        semiOpenFilePriority ++
        directionalLinePriority ++
        lineControlPriority ++
        connectedRooksPriority ++
        centralPawnAdvancePriority ++
        strongKnightOutpostPriority ++
        routeOutpostPriority ++
        directionalOutpostPriority ++
        strongKnightVsBishopPriority ++
        knightVsBishopMotifPriority ++
        bishopPairPriority ++
        oppositeColorBishopsPriority ++
        enemyBadBishopPriority ++
        badBishopActivityPriority ++
        pieceCentralizationPriority ++
        pieceManeuverPriority ++
        frenchMinorPriority ++
        goodBishopCountPriority ++
        exchangeAvailabilityPriority ++
        iqpSimplificationPriority ++
        colorComplexClampPriority ++
        maroczyProfilePriority ++
        spaceAdvantageMotifPriority ++
        lockedCenterPriority ++
        iqpSpaceBridgePriority ++
        iqpCentralPresencePriority ++
        centralSpacePriority ++
        mobilityRestrictionPriority
    ).distinct

  private def surfaceEvidenceRefs(candidate: Candidate): List[String] =
    val distinct = candidate.evidenceRefs.map(_.wireKey).distinct
    val relationPriority =
      candidate.relationKind
        .flatMap(RelationObservationCatalog.descriptorForKind)
        .toList
        .flatMap(_.wireEvidenceRefs)
        .filter(distinct.contains)
    val priority = (relationPriority ++ prioritySupportEvidenceRefs(distinct)).distinct
    (priority ++ distinct.filterNot(priority.contains)).take(8)

  private def sourceWire(source: EvidenceSourceId): String =
    EvidenceRef.Source(source).wireKey

  def focusSummary(signal: StrategyIdeaSignal): String =
    focusSummary(
      focusSquares = signal.focusSquares,
      focusFiles = signal.focusFiles,
      focusDiagonals = signal.focusDiagonals,
      focusZone = signal.focusZone
    )

  def packetAnchorTerms(signal: StrategyIdeaSignal): List[String] =
    (
      signal.focusSquares ++
        signal.focusFiles.map(file => s"${normalizeFileToken(file).getOrElse(file)}-file") ++
        signal.focusDiagonals ++
        signal.focusZone.toList ++
        signal.beneficiaryPieces
    ).flatMap(displayToken).distinct

  def packetRivalKind(pack: StrategyPack): Option[String] =
    pack.strategicIdeas.lift(1).flatMap(signal => displayToken(signal.kind))

  private def displayToken(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def collectTypedEvidence(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    // Runtime selection is limited to typed structural and semantic sources,
    // not prose-only text fields.
    val side = pack.sideToMove
    val semanticObservations = StrategicSemanticObservationPipeline.collect(pack, semantic)
    (
      collectPawnBreakEvidence(side, semantic) ++
        StrategicIdeaEvidencePipeline.collect(pack, semantic) ++
        collectSemanticTransformationEvidence(side, semanticObservations) ++
        collectTargetFixingEvidence(side, pack, semantic, semanticObservations)
    ).sortBy(ev => (-ev.confidence, ev.kind, ev.source.wireKey))

  private def collectPawnBreakEvidence(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val analysisBreakReady =
      semantic.pawnAnalysis.toList.flatMap { analysis =>
        val file = analysis.breakFile.flatMap(normalizeFileToken)
        val focusSquares = normalizeSquareKeys(analysis.tensionSquares)
        Option.when(analysis.pawnBreakReady && (file.nonEmpty || focusSquares.nonEmpty)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.PawnAnalysisBreakReady,
            confidence = 0.84 + breakImpactBonusFromInt(analysis.breakImpact),
            focusSquares = focusSquares.take(3),
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken).orElse(zoneFromSquareKeys(focusSquares)),
            factIds =
              List("pawn_analysis_break_ready_shape") ++
                Option.when(analysis.advanceOrCapture)("advance_or_capture").toList ++
                Option.when(analysis.counterBreak)("counter_break_race").toList ++
                Option.when(focusSquares.nonEmpty)("tension_squares").toList
          )
        }
      }

    val analysisTension =
      semantic.pawnAnalysis.toList.flatMap { analysis =>
        val focusSquares = normalizeSquareKeys(analysis.tensionSquares)
        Option.when(analysis.advanceOrCapture && focusSquares.nonEmpty) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = if analysis.pawnBreakReady then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PawnAnalysisTension,
            confidence = 0.78 + Option.when(semantic.structureProfile.exists(_.centerState == CenterState.Locked))(0.02)
              .getOrElse(0.0),
            focusSquares = focusSquares.take(3),
            focusZone = zoneFromSquareKeys(focusSquares),
            factIds =
              List("pawn_analysis_tension", "advance_or_capture") ++
                Option.when(analysis.counterBreak)("counter_break_race").toList
          )
        }
      }

    val analysisBreakRace =
      semantic.pawnAnalysis.toList.flatMap { analysis =>
        val file = analysis.breakFile.flatMap(normalizeFileToken)
        Option.when(analysis.counterBreak && file.nonEmpty) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = if analysis.pawnBreakReady then StrategicIdeaReadiness.Build else StrategicIdeaReadiness.Premature,
            source = EvidenceSourceId.PawnAnalysisBreakRace,
            confidence = 0.72,
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken),
            factIds = List("counter_break_race") ++ file.toList.map(v => s"break_file_$v")
          )
        }
      }

    val base =
      semantic.pawnPlay.toList.flatMap { pawnPlay =>
        Option.when(pawnPlay.breakReady && pawnPlay.breakFile.exists(_.trim.nonEmpty) && pawnPlay.breakImpact != "Low") {
          val file = normalizeFileToken(pawnPlay.breakFile.get)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.PawnPlayBreakReady,
            confidence = 0.84 + breakImpactBonus(pawnPlay.breakImpact),
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken),
            factIds = List("pawn_play_break_ready_shape") ++ file.toList.map(v => s"break_file_$v")
          )
        }
      }

    val pawnBreakMotif =
      semantic.motifs.collect {
        case Motif.PawnBreak(file, targetFile, color, _, _) if matchesSide(color, side) =>
          val fileKey = fileToken(file)
          val targetFileKey = fileToken(targetFile)
          val focusFiles = List(fileKey, targetFileKey).distinct
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PawnBreakMotif,
            confidence = 0.76,
            focusFiles = focusFiles,
            focusZone = focusFiles.flatMap(zoneFromFileToken).headOption,
            factIds = List("pawn_break_motif_shape", s"break_file_$fileKey", s"break_target_file_$targetFileKey")
          )
      }

    val tension =
      for
        pawnPlay <- semantic.pawnPlay.toList
        file <- pawnPlay.breakFile.flatMap(normalizeFileToken).toList
        features <- semantic.positionFeatures.toList
        if features.centralSpace.pawnTensionCount > 0 || features.centralSpace.lockedCenter
      yield
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.PawnBreak,
          readiness = if pawnPlay.breakReady then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.CentralBreakTension,
          confidence = 0.74 + (if features.centralSpace.lockedCenter then 0.04 else 0.0),
          focusFiles = List(file),
          focusZone = Some("center"),
          factIds =
            List("central_break_tension") ++
              Option.when(features.centralSpace.lockedCenter)("locked_center").toList
        )

    val fileOpening =
      for
        pawnPlay <- semantic.pawnPlay.toList
        file <- pawnPlay.breakFile.flatMap(normalizeFileToken).flatMap(fileFromToken).toList
        board <- semantic.board.toList
        if fileHasBothColorsPawns(board, file)
      yield
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.PawnBreak,
          readiness = if pawnPlay.breakReady then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.FileOpeningConsequence,
          confidence = 0.72,
          focusFiles = List(fileToken(file)),
          focusZone = zoneFromFileToken(fileToken(file)),
          factIds = List("file_opening_consequence", s"contested_file_${fileToken(file)}")
        )

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          plan.plan.id == PlanId.PawnBreakPreparation || plan.plan.id == PlanId.CentralBreakthrough
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PlanMatchBreakPreparation,
            confidence = 0.76 + math.min(0.08, plan.score * 0.10),
            focusFiles =
              semantic.pawnAnalysis.flatMap(_.breakFile.flatMap(normalizeFileToken)).toList ++
                semantic.pawnPlay.flatMap(_.breakFile.flatMap(normalizeFileToken)).toList,
            factIds = List("plan_match_break_preparation", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val frenchCounterBreak =
      Option.when(
        structureIs(semantic, StructureId.FrenchAdvanceChain) &&
          side == "black" &&
          semantic.phase != "endgame"
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.PawnBreak,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.FrenchCounterbreakProfile,
          confidence = 0.88,
          focusFiles = List("f"),
          focusZone = Some("center"),
          factIds = List("structure_french_advance_chain", "french_counterbreak_profile", "french_f6_break")
        )
      }.toList

    val frenchF6Break =
      Option.when(
        structureIs(semantic, StructureId.FrenchAdvanceChain) &&
          side == "black" &&
          semantic.phase != "endgame" &&
          semantic.board.exists(board =>
            pawnAt(board, Color.Black, Square.F7) &&
              pawnAt(board, Color.Black, Square.E6) &&
              pawnAt(board, Color.Black, Square.D5) &&
              pawnAt(board, Color.White, Square.E5)
          )
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.PawnBreak,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.FrenchF6BreakSeed,
          confidence = 0.92,
          focusFiles = List("f"),
          focusSquares = List("e5", "f6"),
          focusZone = Some("center"),
          factIds = List("french_f6_break_seed_shape", "white_e5_chain", "black_f7_break_pawn")
        )
      }.toList

    analysisBreakReady ++ analysisTension ++ analysisBreakRace ++ base ++ pawnBreakMotif ++ tension ++ fileOpening ++ planBridge ++
      frenchCounterBreak ++ frenchF6Break

  private def collectSemanticTransformationEvidence(
      side: String,
      observations: List[StrategicSemanticObservation]
  ): List[StrategicIdeaEvidence] =
    observations
      .filter(_.ownerSide == side)
      .flatMap { observation =>
        RelationObservationCatalog
          .descriptorForObservationId(observation.id)
          .filter(descriptor => observation.source.contains(descriptor.source))
          .toList
          .map { descriptor =>
            evidence(
              ownerSide = side,
              kind = descriptor.ideaKind,
              readiness = descriptor.readiness,
              source = descriptor.source,
              confidence = descriptor.confidence,
              focusSquares = observation.focusSquares,
              focusZone = observation.focusZone,
              targetSquare = observation.targetSquare,
              beneficiaryPieces = descriptor.beneficiaryPieces,
              factIds = Nil,
              typedFactIds = observation.facts,
              relationKind = Some(descriptor.relationKind),
              relationFocusSquares = observation.focusSquares
            )
          }
      }

  private def collectTargetFixingEvidence(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext,
      semanticObservations: List[StrategicSemanticObservation]
  ): List[StrategicIdeaEvidence] =
    val enemyWeakSquares =
      semantic.positionalFeatures.collect {
        case PositionalTag.WeakSquare(square, owner) if !matchesSide(owner, side) => square.key
      }.toSet
    val structuralTargetSquares =
      semantic.structuralWeaknesses
        .filter(weakness => !matchesSide(weakness.color, side))
        .flatMap(_.squares.map(_.key))
        .distinct
    val exactTargetSquares = (enemyWeakSquares.toList ++ structuralTargetSquares).distinct

    val weakSquareEvidence =
      enemyWeakSquares.toList.map { square =>
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.TargetFixing,
          readiness = StrategicIdeaReadiness.Ready,
          source = EvidenceSourceId.EnemyWeakSquare,
          confidence = 0.74,
          focusSquares = List(square),
          factIds = List(s"enemy_weak_square_$square")
        )
      }

    val colorComplexEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.ColorComplexWeakness(owner, squareColor, squares) if !matchesSide(owner, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.ColorComplexWeakness,
            confidence = 0.70,
            focusSquares = squares.map(_.key).take(3),
            focusZone = Some(s"$squareColor squares"),
            factIds = List("enemy_color_complex_weakness", s"color_complex_$squareColor")
          )
      }

    val minorityObservations =
      semanticObservations
        .filter(observation =>
          observation.id == SemanticObservationId.MinorityAttackSemantic &&
            observation.ownerSide == side
        )
    val compensationMaterialDeficit =
      semantic.positionFeatures.exists(features => hasCompensationMaterialDeficitFor(side, features))

    val minorityAttackEvidence =
      Option.unless(compensationMaterialDeficit)(minorityObservations).toList.flatten.map { observation =>
        val focusSquares = observation.focusSquares.take(3)
        val primaryBreakBonus = observation.facts.exists(_.wireKey.startsWith("minority_break_"))
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.TargetFixing,
          readiness = StrategicIdeaReadiness.Build,
          source = observation.source.getOrElse(EvidenceSourceId.MinorityAttackSemantic),
          confidence =
            0.54 +
              math.min(0.05, focusSquares.size * 0.01 + Option.when(primaryBreakBonus)(0.02).getOrElse(0.0)),
          focusSquares = focusSquares,
          focusZone = observation.focusZone,
          typedFactIds = observation.facts
        )
      }

    val minorityAttackSupportEvidence =
      Option.when(minorityObservations.isEmpty) {
        semantic.positionalFeatures.collect {
          case PositionalTag.MinorityAttack(color, flank)
              if matchesSide(color, side) && flankTargetSquares(flank, exactTargetSquares).nonEmpty =>
            val focusSquares = flankTargetSquares(flank, exactTargetSquares).take(3)
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.TargetFixing,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.MinorityAttackSupport,
              confidence = 0.56,
              focusSquares = focusSquares,
              focusZone = Some(flank),
              factIds = List(s"minority_attack_support_$flank")
            )
        }
      }.toList.flatten

    val weakComplexEvidence =
      semantic.structuralWeaknesses
        .filter { weakness =>
          !matchesSide(weakness.color, side) &&
          weakness.squares.nonEmpty &&
          (
            normalizeFactToken(weakness.cause) != "holes" ||
              weakness.isOutpost ||
              weakness.squares.map(_.key).exists(enemyWeakSquares.contains)
          )
        }
        .map { weakness =>
          val cause = normalizeFactToken(weakness.cause)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.WeakComplexFixation,
            confidence = weakComplexConfidence(weakness),
            focusSquares = weakness.squares.map(_.key).distinct.take(3),
            focusZone = zoneFromSquares(weakness.squares),
              factIds =
              List("weak_complex_fixation", s"weak_complex_$cause") ++
              Option.when(weakness.isOutpost)("weak_complex_outpost").toList
          )
        }

    val motifWeakComplexEvidence =
      semantic.motifs.collect {
        case Motif.IsolatedPawn(file, rank, color, _, _) if !matchesSide(color, side) && rank >= 1 && rank <= 8 =>
          val square = s"${fileToken(file)}$rank"
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.WeakComplexFixation,
            confidence = 0.72,
            focusSquares = List(square),
            focusZone = zoneFromFileToken(fileToken(file)),
            factIds = List("weak_complex_fixation", "weak_complex_isolated_pawn")
          )
        case Motif.BackwardPawn(file, rank, color, _, _) if !matchesSide(color, side) && rank >= 1 && rank <= 8 =>
          val square = s"${fileToken(file)}$rank"
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.WeakComplexFixation,
            confidence = 0.72,
            focusSquares = List(square),
            focusZone = zoneFromFileToken(fileToken(file)),
            factIds = List("weak_complex_fixation", "weak_complex_backward_pawn")
          )
      }

    val doubledPawnPressureEvidence =
      semantic.motifs.collect {
        case Motif.DoubledPawns(file, color, _, _) if !matchesSide(color, side) =>
          val fileKey = fileToken(file)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.DoubledPawnPressureMotif,
            confidence = 0.70,
            focusFiles = List(fileKey),
            focusZone = zoneFromFileToken(fileKey),
            factIds = List("doubled_pawn_pressure_shape", s"doubled_pawn_file_$fileKey")
          )
      }

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          plan.plan.id == PlanId.WeakPawnAttack ||
            plan.plan.id == PlanId.MinorityAttack ||
            plan.plan.id == PlanId.Blockade
        ) {
          val reviewedWeaknessBonus =
            Option.when(
              semantic.strategicPlanExperiments.exists(experiment =>
                experiment.themeL1 == PlanTaxonomy.PlanTheme.WeaknessFixation.id &&
                  isPlayableExperiment(experiment)
              )
            )(0.08).getOrElse(0.0)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PlanMatchTargetFixing,
            confidence = 0.78 + math.min(0.06, plan.score * 0.08) + reviewedWeaknessBonus,
            focusSquares = exactTargetSquares.take(2),
            factIds = List("plan_match_target_fixing", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val directionalFixation =
      pack.directionalTargets
        .filter(_.ownerSide == side)
        .flatMap { target =>
          Option.when(exactTargetSquares.contains(target.targetSquare)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.TargetFixing,
              readiness = ideaReadinessFromDirectionalTarget(target.readiness),
              source = EvidenceSourceId.DirectionalTargetFixation,
              confidence = 0.64 + readinessBonus(target.readiness),
              focusSquares = List(target.targetSquare),
              beneficiaryPieces = List(target.piece),
              factIds = List("directional_target_fixation", s"directional_target_${target.targetSquare}")
            )
          }
        }

    val compensationTargetFixation =
      semantic.positionFeatures
        .flatMap { features =>
          val allTargetSquares = exactTargetSquares
          val queensideTargetCount =
            allTargetSquares.count(square =>
              square.headOption.exists(file => file == 'a' || file == 'b' || file == 'c')
            )
          Option.when(
            hasCompensationMaterialDeficitFor(side, features) &&
              isCompensationEligiblePhase(semantic) &&
              allTargetSquares.nonEmpty &&
              hasCompensationTargetPlanSupport(side, semantic) &&
              (
                directionalFixation.nonEmpty ||
                  weakComplexEvidence.nonEmpty ||
                  hasCompensationLineAccess(side, pack, semantic)
              )
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.TargetFixing,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.CompensationTargetFixation,
              confidence =
                0.78 +
                  math.min(0.06, allTargetSquares.size * 0.02) +
                  Option.when(queensideTargetCount > 0)(0.04).getOrElse(0.0),
              focusSquares = allTargetSquares.take(3),
              focusZone = Option.when(queensideTargetCount > 0)("queenside"),
              factIds =
                List("compensation_target_fixation", "material_deficit_compensation") ++
                  Option.when(queensideTargetCount > 0)("queenside_target_fixation_from_gambit").toList
            )
          }
        }
        .toList

    val carlsbadFixation =
      Option.when(
        structureIs(semantic, StructureId.Carlsbad) &&
          minorityObservations.exists(observation =>
            observation.focusZone.exists(normalizeFactToken(_) == "queenside") &&
              observation.focusSquares.exists(target =>
                exactTargetSquares.map(normalizeFactToken).contains(normalizeFactToken(target))
              )
          ) &&
          exactTargetSquares.nonEmpty
      ) {
        val targetPressureFacts =
          minorityObservations
            .filter(observation =>
              observation.focusZone.exists(normalizeFactToken(_) == "queenside") &&
                observation.focusSquares.exists(target =>
                  exactTargetSquares.map(normalizeFactToken).contains(normalizeFactToken(target))
                )
            )
            .flatMap(_.facts)
            .filter(fact => fact.wireKey == SemanticObservationId.TargetPressureSemantic.wireKey || fact.wireKey.startsWith("target_pressure_"))
            .distinct
        val queensideTargets = flankTargetSquares("queenside", exactTargetSquares).take(3)
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.TargetFixing,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.CarlsbadFixationProfile,
          confidence = 0.90,
          focusSquares = if queensideTargets.nonEmpty then queensideTargets else exactTargetSquares.take(3),
          factIds = List("structure_carlsbad"),
          typedFactIds =
            (targetPressureFacts ++ List(FactId.semantic(SemanticObservationId.MinorityAttackSemantic))).distinct
        )
      }.toList

    weakSquareEvidence ++ colorComplexEvidence ++ minorityAttackEvidence ++ minorityAttackSupportEvidence ++ weakComplexEvidence ++ motifWeakComplexEvidence ++
      doubledPawnPressureEvidence ++
      planBridge ++ directionalFixation ++ compensationTargetFixation ++ carlsbadFixation

  private def mergeEvidence(
      evidence: List[StrategicIdeaEvidence],
      semantic: StrategicIdeaSemanticContext
  ): List[Candidate] =
    evidence
      .groupBy(_.signature)
      .values
      .flatMap { grouped =>
        val best = grouped.maxBy(_.confidence)
        val bestRelation =
          grouped.filter(_.relationKind.nonEmpty).toList.sortBy(ev => (-ev.confidence, ev.source.wireKey)).headOption
        val stackCap =
          best.kind match
            case StrategicIdeaKind.TargetFixing                   => 0.10
            case StrategicIdeaKind.FavorableTradeOrTransformation => 0.08
            case StrategicIdeaKind.LineOccupation                 => 0.08
            case _                                                => 0.18
        val stackIncrement =
          best.kind match
            case StrategicIdeaKind.TargetFixing                   => 0.03
            case StrategicIdeaKind.FavorableTradeOrTransformation => 0.03
            case StrategicIdeaKind.LineOccupation                 => 0.02
            case _                                                => 0.05
        val baseScore = (best.confidence + math.min(stackCap, (grouped.size - 1) * stackIncrement)).min(0.98)
        val matchingPlanEvidence =
          semantic.strategicPlanExperiments.filter(planEvidenceAppliesToKind(_, best.kind))
        val blocked =
          matchingPlanEvidence.exists(planEvidenceBlocksKind(best.kind, _))
        val planEvidenceDelta =
          matchingPlanEvidence.map(planEvidenceModifier(best.kind, _)).sum.max(-0.36).min(0.28)
        val tacticalCompetitionPenalty =
          slowIdeaTacticalCompetitionPenalty(best.kind, semantic.strategicPlanExperiments)
        val score = (baseScore + planEvidenceDelta + tacticalCompetitionPenalty).max(0.0).min(0.98)
        val selectedRelationKind = bestRelation.flatMap(_.relationKind)
        val relationTarget = bestRelation.flatMap(_.targetSquare)
        val genericTarget = best.targetSquare.orElse(grouped.flatMap(_.targetSquare).headOption)
        Option.when(!blocked) {
          Candidate(
            ownerSide = best.ownerSide,
            kind = best.kind,
            group = best.group,
            readiness = mergeReadiness(grouped.map(_.readiness)),
            focusSquares = grouped.flatMap(_.focusSquares).distinct.take(4),
            focusFiles = grouped.flatMap(_.focusFiles).distinct.take(2),
            focusDiagonals = grouped.flatMap(_.focusDiagonals).distinct.take(2),
            focusZone = mostCommon(grouped.flatMap(_.focusZone)),
            targetSquare =
              if selectedRelationKind.nonEmpty then relationTarget
              else genericTarget,
            relationKind = selectedRelationKind,
            relationFocusSquares = bestRelation.map(_.relationFocusSquares).getOrElse(Nil),
            beneficiaryPieces = grouped.flatMap(_.beneficiaryPieces).distinct.take(4),
            score = score,
            evidenceRefs = candidateEvidenceRefs(grouped, matchingPlanEvidence, bestRelation),
            evidenceCount = grouped.size,
            sourceCount = grouped.map(_.source).distinct.size
          )
        }
      }
      .toList
      .sortBy(candidate => (-candidate.score, candidate.kind))

  private def candidateEvidenceRefs(
      grouped: Iterable[StrategicIdeaEvidence],
      matchingPlanEvidence: Iterable[StrategicPlanExperiment],
      bestRelation: Option[StrategicIdeaEvidence]
  ): List[EvidenceRef] =
    val refs =
      (
        grouped.map(ev => EvidenceRef.Source(ev.source)) ++
          grouped.flatMap(_.factIds.map(EvidenceRef.Fact(_))) ++
          matchingPlanEvidence.flatMap(planEvidenceRefs)
      ).toList.distinct
    val relationPriority = relationEvidenceRefs(bestRelation).filter(refs.contains)
    val supportPriorityWireKeys = prioritySupportEvidenceRefs(refs.map(_.wireKey))
    val supportPriority = supportPriorityWireKeys.flatMap(wireKey => refs.find(_.wireKey == wireKey))
    val priority = (relationPriority ++ supportPriority).distinct
    (priority ++ refs.filterNot(priority.contains)).take(8)

  private def relationEvidenceRefs(relationEvidence: Option[StrategicIdeaEvidence]): List[EvidenceRef] =
    relationEvidence
      .flatMap(_.relationKind)
      .flatMap(RelationObservationCatalog.descriptorForKind)
      .toList
      .flatMap(_.evidenceRefs)

  private def selectFamilies(
      rankedFamilies: List[FamilyCandidate]
  ): List[String] =
    rankedFamilies.headOption.toList.flatMap { dominant =>
      val secondary =
        rankedFamilies.drop(1).find(candidate =>
          math.abs(dominant.score - candidate.score) <= 0.08
        )
      List(Some(dominant.family), secondary.map(_.family)).flatten.distinct
    }

  private def rankFamilies(
      candidates: List[Candidate],
      semantic: StrategicIdeaSemanticContext
  ): List[FamilyCandidate] =
    candidates
      .groupBy(candidate => familyForKind(candidate.kind))
      .flatMap { case (family, members) =>
        members.headOption.map { _ =>
          val previewScores = members.map(candidate => candidate -> previewKindAdjustment(candidate, semantic)).toMap
          val bestPreview = members.map(candidate => candidate.score + previewScores(candidate)).max
          val supportBonus = math.min(0.04, (members.map(_.kind).distinct.size - 1) * 0.02)
          FamilyCandidate(
            family = family,
            score = (bestPreview + supportBonus + familyStageAdjustment(family, members, semantic)).min(0.98),
            members = members.sortBy(candidate => (-(candidate.score + previewScores(candidate)), candidate.kind))
          )
        }
      }
      .toList
      .sortBy(candidate => (-candidate.score, familyTiePriority(candidate, semantic), candidate.family))

  private def familyTiePriority(
      candidate: FamilyCandidate,
      semantic: StrategicIdeaSemanticContext
  ): Int =
    candidate.family match
      case StrategicIdeaFamily.ForcingOrTacticalNow =>
        if candidate.members.exists(member =>
            member.kind == StrategicIdeaKind.PawnBreak &&
              candidateHasSource(member, EvidenceSourceId.FrenchF6BreakSeed)
          )
        then 0
        else if candidate.members.exists(member =>
            member.kind == StrategicIdeaKind.KingAttackBuildUp &&
              hasStrongKingAttackAnchor(member, semantic)
          )
        then 0
        else if candidate.members.exists(member =>
            member.kind == StrategicIdeaKind.PawnBreak &&
              hasConcretePawnBreakAnchor(member)
          )
        then 2
        else 3
      case StrategicIdeaFamily.SlowStructural =>
        if preferredSlowStructuralKind(candidate.members, semantic).isDefined then 1 else 2
      case StrategicIdeaFamily.ConversionOrTransformation =>
        if candidate.members.exists(member => hasStrongConversionAnchor(member, semantic)) ||
            hasStructuredIqpConversionWindow(candidate.members, semantic)
        then 0
        else 2
      case StrategicIdeaFamily.PreventionOrSuppression =>
        if hasPreventionOrSuppressionAnchor(semantic.sideToMove, semantic) ||
            candidate.members.exists(_.kind == StrategicIdeaKind.CounterplaySuppression)
        then 0
        else 3
      case _ =>
        2

  private def stageCandidates(
      candidates: List[Candidate],
      selectedFamilies: List[String],
      semantic: StrategicIdeaSemanticContext
  ): List[Candidate] =
    candidates
      .filter(candidate => selectedFamilies.contains(familyForKind(candidate.kind)))
      .groupBy(candidate => familyForKind(candidate.kind))
      .values
      .flatMap { familyMembers =>
        val preferredKind =
          familyMembers.headOption.flatMap(candidate =>
            if familyForKind(candidate.kind) == StrategicIdeaFamily.SlowStructural then
              preferredSlowStructuralKind(familyMembers, semantic)
            else None
          )
        familyMembers.map { candidate =>
          val structuralWinnerBoost =
            Option.when(preferredKind.contains(candidate.kind))(0.18).getOrElse(0.0)
          val stageDelta =
            previewKindAdjustment(candidate, semantic) +
              familyContextAdjustment(candidate, familyMembers, semantic) +
              familyStageAdjustment(familyForKind(candidate.kind), familyMembers, semantic) +
              structuralWinnerBoost
          candidate.copy(score = (candidate.score + stageDelta).max(0.0).min(0.98))
        }
      }
      .toList
      .sortBy(candidate => (-candidate.score, candidate.kind))

  private def familyStageAdjustment(
      family: String,
      members: List[Candidate],
      semantic: StrategicIdeaSemanticContext
  ): Double =
    family match
      case StrategicIdeaFamily.PreventionOrSuppression =>
        if hasPreventionOrSuppressionAnchor(semantic.sideToMove, semantic) ||
            members.exists(candidate => candidate.kind == StrategicIdeaKind.CounterplaySuppression)
        then 0.04
        else -0.24
      case StrategicIdeaFamily.ConversionOrTransformation =>
        if members.exists(candidate => hasStrongConversionAnchor(candidate, semantic)) ||
            hasStructuredIqpConversionWindow(members, semantic)
        then 0.18
        else if members.exists(candidate => hasDefenderTagExchangeSupport(candidate) || hasSoftTransformationPlanSupport(candidate)) then 0.18
        else if members.forall(isWeakConversionWindowOnly(_, semantic)) then -0.12
        else 0.0
      case StrategicIdeaFamily.ForcingOrTacticalNow =>
        if members.exists(candidate =>
            candidate.kind == StrategicIdeaKind.PawnBreak &&
              candidateHasSource(candidate, EvidenceSourceId.FrenchF6BreakSeed)
          )
        then 0.18
        else if members.exists(candidate =>
            candidate.kind == StrategicIdeaKind.PawnBreak &&
              hasConcretePawnBreakAnchor(candidate)
          )
        then 0.04
        else if members.exists(candidate =>
            candidate.kind == StrategicIdeaKind.KingAttackBuildUp &&
              hasCompensationAttackAnchor(candidate, semantic)
          )
        then 0.14
        else if semantic.strategicPlanExperiments.exists(experiment =>
            experiment.themeL1 == PlanTaxonomy.PlanTheme.ImmediateTacticalGain.id &&
              isPlayableExperiment(experiment)
          )
        then 0.04
        else 0.0
      case _ =>
        0.0

  private def previewKindAdjustment(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Double =
    candidate.kind match
      case StrategicIdeaKind.SpaceGainOrRestriction =>
        if hasBroadSpaceAnchor(candidate, semantic) then 0.08 else 0.0
      case StrategicIdeaKind.TargetFixing =>
        if hasCompensationTargetFixingAnchor(candidate, semantic) then 0.12
        else if hasStrongTargetFixingAnchor(candidate, semantic) then 0.08
        else if isGenericTargetFixing(candidate, semantic) then -0.10
        else 0.0
      case StrategicIdeaKind.LineOccupation =>
        if hasStrongLineAnchor(candidate) then 0.10
        else if hasCompensationLineAnchor(candidate, semantic) then 0.12
        else if hasRouteLineAnchor(candidate) then 0.06
        else 0.0
      case StrategicIdeaKind.OutpostCreationOrOccupation =>
        if hasStableOutpostAnchor(candidate, semantic) then 0.08 else -0.16
      case StrategicIdeaKind.MinorPieceImbalanceExploitation =>
        if hasStrongMinorPieceAnchor(candidate, semantic) then 0.06 else 0.0
      case StrategicIdeaKind.Prophylaxis =>
        if hasSupportedProphylaxisContext(candidate.ownerSide, semantic) then 0.03 else -0.22
      case StrategicIdeaKind.FavorableTradeOrTransformation =>
        if hasStrongConversionAnchor(candidate, semantic) then 0.08
        else if isWeakConversionWindowOnly(candidate, semantic) then -0.12
        else 0.0
      case StrategicIdeaKind.CounterplaySuppression =>
        if hasCompensationSuppressionAnchor(candidate, semantic) then 0.10
        else if hasPreventionOrSuppressionAnchor(candidate.ownerSide, semantic) then 0.03
        else 0.0
      case StrategicIdeaKind.KingAttackBuildUp =>
        if hasCompensationAttackAnchor(candidate, semantic) then
          if hasWeakKingWindowCompensationContext(candidate.ownerSide, semantic) then 0.04 else 0.12
        else 0.0
      case _ =>
        0.0

  private def familyContextAdjustment(
      candidate: Candidate,
      familyMembers: List[Candidate],
      semantic: StrategicIdeaSemanticContext
  ): Double =
    candidate.kind match
      case StrategicIdeaKind.SpaceGainOrRestriction =>
        val genericFixationPenalty =
          if familyMembers.exists(other =>
              other.kind == StrategicIdeaKind.TargetFixing &&
                isGenericTargetFixing(other, semantic)
            )
          then 0.04
          else 0.0
        val concreteStructuralPenalty =
          if !hasBroadSpaceAnchor(candidate, semantic) &&
              familyMembers.exists(other =>
                (other.kind == StrategicIdeaKind.OutpostCreationOrOccupation &&
                  hasStableOutpostAnchor(other, semantic)) ||
                  (other.kind == StrategicIdeaKind.LineOccupation &&
                    (hasStrongLineAnchor(other) || hasRouteLineAnchor(other)))
              )
          then -0.08
          else 0.0
        genericFixationPenalty + concreteStructuralPenalty
      case StrategicIdeaKind.TargetFixing =>
        val structuralCompetitionPenalty =
          if familyMembers.exists(other =>
              other.kind == StrategicIdeaKind.SpaceGainOrRestriction &&
                hasBroadSpaceAnchor(other, semantic)
            )
          then 0.06
          else 0.0
        val lineCompetitionPenalty =
          if familyMembers.exists(other =>
              other.kind == StrategicIdeaKind.LineOccupation &&
                (hasStrongLineAnchor(other) || hasCompensationLineAnchor(other, semantic) || hasRouteLineAnchor(other))
            )
          then 0.04
          else 0.0
        -(structuralCompetitionPenalty + lineCompetitionPenalty)
      case StrategicIdeaKind.LineOccupation =>
        val genericCompetitionBonus =
          if familyMembers.exists(other =>
              other.kind == StrategicIdeaKind.OutpostCreationOrOccupation &&
                !hasStableOutpostAnchor(other, semantic)
            ) ||
              familyMembers.exists(other =>
                other.kind == StrategicIdeaKind.TargetFixing &&
                  isGenericTargetFixing(other, semantic)
              )
          then 0.04
          else 0.0
        val broadSpacePenalty =
          if familyMembers.exists(other =>
              other.kind == StrategicIdeaKind.SpaceGainOrRestriction &&
                hasBroadSpaceAnchor(other, semantic)
            )
          then -0.03
          else 0.0
        val compensationBonus =
          if hasCompensationLineAnchor(candidate, semantic) &&
              familyMembers.exists(other =>
                other.kind == StrategicIdeaKind.SpaceGainOrRestriction &&
                  hasBroadSpaceAnchor(other, semantic)
              )
          then 0.06
          else 0.0
        genericCompetitionBonus + broadSpacePenalty + compensationBonus
      case StrategicIdeaKind.OutpostCreationOrOccupation =>
        val minorPiecePenalty =
          if familyMembers.exists(other =>
              other.kind == StrategicIdeaKind.MinorPieceImbalanceExploitation &&
                hasStrongMinorPieceAnchor(other, semantic)
            ) &&
              !hasStableOutpostAnchor(candidate, semantic)
          then -0.06
          else 0.0
        val concreteAnchorBonus =
          if hasStableOutpostAnchor(candidate, semantic) &&
              familyMembers.exists(other =>
                other.kind == StrategicIdeaKind.SpaceGainOrRestriction &&
                  !hasBroadSpaceAnchor(other, semantic)
              )
          then 0.04
          else 0.0
        minorPiecePenalty + concreteAnchorBonus
      case StrategicIdeaKind.MinorPieceImbalanceExploitation =>
        if familyMembers.exists(other =>
            other.kind == StrategicIdeaKind.OutpostCreationOrOccupation &&
              !hasStableOutpostAnchor(other, semantic)
          )
        then 0.05
        else 0.0
      case StrategicIdeaKind.KingAttackBuildUp =>
        val quietCompensationCompetition =
          if hasCompensationAttackAnchor(candidate, semantic) &&
              hasWeakKingWindowCompensationContext(candidate.ownerSide, semantic) &&
              familyMembers.exists(other =>
                (other.kind == StrategicIdeaKind.LineOccupation &&
                  hasCompensationLineAnchor(other, semantic)) ||
                  (other.kind == StrategicIdeaKind.TargetFixing &&
                    hasCompensationTargetFixingAnchor(other, semantic)) ||
                  (other.kind == StrategicIdeaKind.CounterplaySuppression &&
                    hasCompensationSuppressionAnchor(other, semantic))
              )
          then -0.10
          else 0.0
        quietCompensationCompetition
      case _ =>
        0.0

  private def planEvidenceAppliesToKind(
      experiment: StrategicPlanExperiment,
      kind: String
  ): Boolean =
    val matchedKinds =
      experiment.subplanId
        .flatMap(PlanTaxonomy.PlanKind.fromId)
        .map {
          case PlanTaxonomy.PlanKind.BreakPrevention | PlanTaxonomy.PlanKind.KeySquareDenial =>
            if experiment.counterBreakNeutralized then Set(StrategicIdeaKind.CounterplaySuppression, StrategicIdeaKind.Prophylaxis)
            else Set(StrategicIdeaKind.Prophylaxis)
          case PlanTaxonomy.PlanKind.ProphylaxisRestraint =>
            Set(StrategicIdeaKind.Prophylaxis)
          case PlanTaxonomy.PlanKind.OutpostEntrenchment =>
            Set(StrategicIdeaKind.OutpostCreationOrOccupation)
          case PlanTaxonomy.PlanKind.WorstPieceImprovement | PlanTaxonomy.PlanKind.BishopReanchor =>
            Set(StrategicIdeaKind.MinorPieceImbalanceExploitation, StrategicIdeaKind.LineOccupation)
          case PlanTaxonomy.PlanKind.RookFileTransfer | PlanTaxonomy.PlanKind.OpenFilePressure =>
            Set(StrategicIdeaKind.LineOccupation)
          case PlanTaxonomy.PlanKind.FlankClamp | PlanTaxonomy.PlanKind.CentralSpaceBind |
              PlanTaxonomy.PlanKind.MobilitySuppression =>
            Set(StrategicIdeaKind.SpaceGainOrRestriction)
          case PlanTaxonomy.PlanKind.StaticWeaknessFixation | PlanTaxonomy.PlanKind.MinorityAttackFixation |
              PlanTaxonomy.PlanKind.BackwardPawnTargeting | PlanTaxonomy.PlanKind.IQPInducement =>
            Set(StrategicIdeaKind.TargetFixing)
          case PlanTaxonomy.PlanKind.CentralBreakTiming | PlanTaxonomy.PlanKind.WingBreakTiming |
              PlanTaxonomy.PlanKind.TensionMaintenance =>
            Set(StrategicIdeaKind.PawnBreak)
          case PlanTaxonomy.PlanKind.SimplificationWindow | PlanTaxonomy.PlanKind.DefenderTrade |
              PlanTaxonomy.PlanKind.QueenTradeShield | PlanTaxonomy.PlanKind.SimplificationConversion |
              PlanTaxonomy.PlanKind.PasserConversion | PlanTaxonomy.PlanKind.PassedPawnManufacture |
              PlanTaxonomy.PlanKind.BadPieceLiquidation | PlanTaxonomy.PlanKind.InvasionTransition |
              PlanTaxonomy.PlanKind.OppositeBishopsConversion =>
            Set(StrategicIdeaKind.FavorableTradeOrTransformation)
          case PlanTaxonomy.PlanKind.RookPawnMarch | PlanTaxonomy.PlanKind.HookCreation |
              PlanTaxonomy.PlanKind.RookLiftScaffold =>
            Set(StrategicIdeaKind.KingAttackBuildUp)
          case PlanTaxonomy.PlanKind.OpeningDevelopment |
              PlanTaxonomy.PlanKind.ForcingTacticalShot | PlanTaxonomy.PlanKind.DefenderOverload |
              PlanTaxonomy.PlanKind.ClearanceBreak | PlanTaxonomy.PlanKind.BatteryPressure =>
            Set.empty[String]
        }
        .getOrElse(themeIdeaKinds(experiment.themeL1))
    matchedKinds.contains(kind)

  private def themeIdeaKinds(themeL1: String): Set[String] =
    PlanTaxonomy.PlanTheme
      .fromId(themeL1)
      .map {
        case PlanTaxonomy.PlanTheme.RestrictionProphylaxis =>
          Set(StrategicIdeaKind.Prophylaxis)
        case PlanTaxonomy.PlanTheme.PieceRedeployment =>
          Set(StrategicIdeaKind.LineOccupation)
        case PlanTaxonomy.PlanTheme.SpaceClamp =>
          Set(StrategicIdeaKind.SpaceGainOrRestriction)
        case PlanTaxonomy.PlanTheme.WeaknessFixation =>
          Set(StrategicIdeaKind.TargetFixing)
        case PlanTaxonomy.PlanTheme.PawnBreakPreparation =>
          Set(StrategicIdeaKind.PawnBreak)
        case PlanTaxonomy.PlanTheme.FavorableExchange | PlanTaxonomy.PlanTheme.AdvantageTransformation =>
          Set(StrategicIdeaKind.FavorableTradeOrTransformation)
        case PlanTaxonomy.PlanTheme.FlankInfrastructure =>
          Set(StrategicIdeaKind.KingAttackBuildUp)
        case PlanTaxonomy.PlanTheme.ImmediateTacticalGain =>
          Set.empty[String]
        case _ =>
          Set.empty[String]
      }
      .getOrElse(Set.empty)

  private def planEvidenceBlocksKind(
      kind: String,
      experiment: StrategicPlanExperiment
  ): Boolean =
    isRefutedExperiment(experiment) ||
      (
        isCounterBreakCriticalKind(kind) &&
          (experiment.supportProbeCount > 0 || experiment.refuteProbeCount > 0) &&
          !experiment.counterBreakNeutralized &&
          experiment.refuteProbeCount > 0
      )

  private def planEvidenceModifier(
      kind: String,
      experiment: StrategicPlanExperiment
  ): Double =
    val tierModifier =
      experimentTier(experiment) match
        case ExperimentEvidenceTier.EvidenceBacked => 0.22
        case ExperimentEvidenceTier.PvCoupled      => 0.10
        case ExperimentEvidenceTier.Deferred       => -0.10
        case ExperimentEvidenceTier.Refuted        => -0.30
        case ExperimentEvidenceTier.Other          => 0.0
    val bestReplyModifier =
      if experiment.bestReplyStable then 0.12
      else if experiment.supportProbeCount + experiment.refuteProbeCount > 0 then -0.12
      else 0.0
    val futureModifier = if experiment.futureSnapshotAligned then 0.08 else 0.0
    val counterBreakModifier =
      if isCounterBreakCriticalKind(kind) then
        if experiment.counterBreakNeutralized then 0.16
        else if experiment.supportProbeCount + experiment.refuteProbeCount > 0 then -0.14
        else 0.0
      else 0.0
    val moveOrderModifier =
      if experiment.moveOrderSensitive then
        if isSlowStrategicKind(kind) then -0.14 else -0.08
      else 0.0
    val supportBalance =
      math.min(0.06, experiment.supportProbeCount * 0.02) -
        math.min(0.10, experiment.refuteProbeCount * 0.05)
    val confidenceNudge = (experiment.experimentConfidence - 0.5) * 0.06
    tierModifier + bestReplyModifier + futureModifier + counterBreakModifier + moveOrderModifier + supportBalance + confidenceNudge

  private def slowIdeaTacticalCompetitionPenalty(
      kind: String,
      experiments: List[StrategicPlanExperiment]
  ): Double =
    if !isSlowStrategicKind(kind) then 0.0
    else
      experiments
        .filter(_.themeL1 == PlanTaxonomy.PlanTheme.ImmediateTacticalGain.id)
        .map { experiment =>
          experimentTier(experiment) match
            case ExperimentEvidenceTier.EvidenceBacked => -0.16
            case ExperimentEvidenceTier.PvCoupled      => -0.08
            case _                                     => 0.0
        }
        .sum
        .max(-0.16)

  private def planEvidenceRefs(experiment: StrategicPlanExperiment): List[EvidenceRef] =
    List(
      Some(s"experiment:${experiment.themeL1}"),
      experiment.subplanId.map(id => s"experiment_subplan:$id"),
      Some(s"experiment_tier:${experiment.evidenceTier}")
    ).flatten.flatMap(FactId.dynamic).map(EvidenceRef.Fact(_))

  private def isSlowStrategicKind(kind: String): Boolean =
    kind match
      case StrategicIdeaKind.SpaceGainOrRestriction | StrategicIdeaKind.TargetFixing |
          StrategicIdeaKind.LineOccupation | StrategicIdeaKind.OutpostCreationOrOccupation |
          StrategicIdeaKind.MinorPieceImbalanceExploitation =>
        true
      case _ => false

  private def isCounterBreakCriticalKind(kind: String): Boolean =
    kind match
      case StrategicIdeaKind.Prophylaxis | StrategicIdeaKind.CounterplaySuppression |
          StrategicIdeaKind.KingAttackBuildUp | StrategicIdeaKind.PawnBreak =>
        true
      case _ => false

  private def enrichDigest(
      pack: StrategyPack,
      digest: Option[NarrativeSignalDigest],
      ideas: List[StrategyIdeaSignal],
      semantic: StrategicIdeaSemanticContext
  ): Option[NarrativeSignalDigest] =
    val base = digest.getOrElse(NarrativeSignalDigest())
    val dominant = ideas.headOption
    val secondary = ideas.drop(1).headOption
    val compensationCarrier =
      if base.compensation.exists(_.trim.nonEmpty) then None else deriveCompensationCarrier(pack, ideas, semantic)
    Option.when(dominant.isDefined || digest.isDefined || compensationCarrier.exists(_.hasSignal))(
      base.copy(
        compensation = base.compensation.orElse(compensationCarrier.flatMap(_.summary)),
        compensationVectors =
          if base.compensationVectors.nonEmpty then base.compensationVectors
          else compensationCarrier.map(_.vectors).getOrElse(Nil),
        investedMaterial = base.investedMaterial.orElse(compensationCarrier.flatMap(_.investedMaterial)),
        dominantIdeaKind = dominant.map(_.kind),
        dominantIdeaGroup = dominant.map(_.group),
        dominantIdeaReadiness = dominant.map(_.readiness),
        dominantIdeaFocus = dominant.map(focusSummary),
        secondaryIdeaKind = secondary.map(_.kind),
        secondaryIdeaGroup = secondary.map(_.group),
        secondaryIdeaFocus = secondary.map(focusSummary)
      )
    )

  private def enrichLongTermFocus(
      current: List[String],
      ideas: List[StrategyIdeaSignal],
      targets: List[StrategyDirectionalTarget],
      digest: Option[NarrativeSignalDigest]
  ): List[String] =
    val compensationLine =
      digest.flatMap(_.compensation).map { summary =>
        val vectors = digest.toList.flatMap(_.compensationVectors).take(2)
        val vectorTail =
          Option.when(vectors.nonEmpty)(s", backed by ${vectors.mkString(" and ")}").getOrElse("")
        s"compensation carrier: $summary$vectorTail"
      }
    val ideaLines =
      ideas.zipWithIndex.map { case (idea, idx) =>
        val prefix = if idx == 0 then "dominant idea" else "secondary idea"
        s"$prefix: ${playerFacingIdeaText(idea)}"
      }
    val targetLines =
      targets.take(2).map(target => s"objective: work toward making ${target.targetSquare} available for the ${pieceName(target.piece)}")
    (compensationLine.toList ++ ideaLines ++ targetLines ++ current).map(_.trim).filter(_.nonEmpty).distinct.take(6)

  private def deriveCompensationCarrier(
      pack: StrategyPack,
      ideas: List[StrategyIdeaSignal],
      semantic: StrategicIdeaSemanticContext
  ): Option[DerivedCompensationCarrier] =
    val owner = ideas.headOption.map(_.ownerSide).getOrElse(pack.sideToMove)
    semantic.positionFeatures
      .filter(features => hasCompensationMaterialDeficitFor(owner, features))
      .flatMap { features =>
        val ideaRefs = ideas.flatMap(_.evidenceRefs).toSet
        val ownRoutes = pack.pieceRoutes.filter(_.ownerSide == owner)
        val ownTargets = pack.directionalTargets.filter(_.ownerSide == owner)
        val developmentLead = developmentLeadFor(owner, features)
        val openLineCount = semiOpenFilesFor(owner, features) + openFilesCount(features)
        val lineAccessCarrier =
          ownRoutes.exists(routePurposeContainsLinePressure) ||
            ownTargets.exists(targetCarriesLinePressure)
        val contestedLineTargets =
          ownTargets.count(target =>
            target.readiness == DirectionalTargetReadiness.Contested && targetCarriesLinePressure(target)
          )
        val attackWindow =
          attackersCountFor(owner, features) +
            enemyKingRingAttackedFor(owner, features) +
            enemyKingExposedFilesFor(owner, features)
        val transformationCarrier =
          ideas.exists(_.kind == StrategicIdeaKind.FavorableTradeOrTransformation) &&
            (
              ideaRefs.contains(sourceWire(EvidenceSourceId.ExchangeAvailabilityBridge)) ||
                ideaRefs.contains(sourceWire(EvidenceSourceId.IqpSimplificationProfile)) ||
                ideaRefs.contains(sourceWire(EvidenceSourceId.PlanMatchTransformation)) ||
                ideaRefs.contains("exchange_availability_bridge") ||
                ideaRefs.contains("iqp_simplification_profile") ||
                ideaRefs.contains("capture_or_exchange")
            )
        val establishedPressureCarrier =
          (
            ideas.exists(_.kind == StrategicIdeaKind.LineOccupation) ||
              ideas.exists(_.kind == StrategicIdeaKind.TargetFixing)
          ) &&
            (
              openLineCount > 0 ||
                lineAccessCarrier ||
              contestedLineTargets > 0
            )
        val compensationDigestPhaseEligible =
          isCompensationEligiblePhase(semantic) ||
            semantic.effectiveCompensation.isDefined ||
            transformationCarrier ||
            establishedPressureCarrier
        Option.when(compensationDigestPhaseEligible) {
        val initiativeCarrier =
          ideaRefs.contains(sourceWire(EvidenceSourceId.CompensationKingWindow)) ||
            ideaRefs.contains(sourceWire(EvidenceSourceId.CompensationDevelopmentLead)) ||
            ideaRefs.contains(sourceWire(EvidenceSourceId.CompensationDiagonalBattery)) ||
            (
              establishedPressureCarrier &&
                (
                  attackWindow >= 1 ||
                    ideaRefs.contains(sourceWire(EvidenceSourceId.OccupiedLineControl)) ||
                    ideaRefs.contains(sourceWire(EvidenceSourceId.DirectionalLineAccess)) ||
                    contestedLineTargets > 0
                )
            ) ||
            (
              ideas.exists(_.kind == StrategicIdeaKind.KingAttackBuildUp) &&
                (attackWindow >= 2 || developmentLead >= 2)
            )
        val linePressureCarrier =
          ideaRefs.contains(sourceWire(EvidenceSourceId.CompensationOpenLines)) ||
            ideaRefs.contains(sourceWire(EvidenceSourceId.CompensationTargetFixation)) ||
            establishedPressureCarrier ||
            (
              ideas.exists(_.kind == StrategicIdeaKind.LineOccupation) &&
                (
                  openLineCount > 0 ||
                    lineAccessCarrier
                )
            )
        val delayedRecoveryCarrier =
          ideaRefs.contains(sourceWire(EvidenceSourceId.DelayedRecoveryWindow)) ||
            (transformationCarrier && (developmentLead >= 1 || lineAccessCarrier)) ||
            (linePressureCarrier && (developmentLead >= 2 || establishedPressureCarrier))
        val returnVectorCarrier =
          transformationCarrier ||
            ideaRefs.contains(sourceWire(EvidenceSourceId.CompensationTargetFixation)) ||
            (linePressureCarrier && (openLineCount > 0 || lineAccessCarrier))

        val summaryTerms =
          if transformationCarrier then
            List(
              Option.when(delayedRecoveryCarrier)("delayed recovery"),
              Option.when(linePressureCarrier)("line pressure"),
              Option.when(initiativeCarrier)("initiative")
            ).flatten
          else
            List(
              Option.when(initiativeCarrier)("initiative"),
              Option.when(linePressureCarrier)("line pressure"),
              Option.when(delayedRecoveryCarrier)("delayed recovery")
            ).flatten
        val investedMaterial = Some(math.abs(materialEdgeFor(owner, features)) * 100).filter(_ > 0)

        val summary =
          summaryTerms.distinct.take(2) match
            case Nil =>
              Option.when(returnVectorCarrier)("return vector")
            case terms if returnVectorCarrier =>
              Some(s"return vector through ${joinLowerTerms(terms)}")
            case terms =>
              Some(joinLowerTerms(terms))

        val vectors =
          List(
            Option.when(initiativeCarrier)(
              formatCompensationVector(
                "Initiative",
                0.40 +
                  (math.min(2, attackWindow) * 0.10) +
                  Option.when(developmentLead >= 2)(0.10).getOrElse(0.0)
              )
            ),
            Option.when(linePressureCarrier)(
              formatCompensationVector(
                "Line Pressure",
                0.40 +
                  math.min(0.20, openLineCount * 0.10) +
                  Option.when(ownRoutes.exists(routePurposeContainsLinePressure))(0.10).getOrElse(0.0)
              )
            ),
            Option.when(delayedRecoveryCarrier)(
              formatCompensationVector(
                "Delayed Recovery",
                0.40 +
                  Option.when(developmentLead >= 2)(0.10).getOrElse(0.0) +
                  Option.when(transformationCarrier)(0.10).getOrElse(0.0)
              )
            ),
            Option.when(returnVectorCarrier)(
              formatCompensationVector(
                "Return Vector",
                0.40 +
                  Option.when(transformationCarrier)(0.10).getOrElse(0.0) +
                  Option.when(linePressureCarrier || initiativeCarrier)(0.10).getOrElse(0.0)
                )
            )
          ).flatten.distinct

        val carrier = DerivedCompensationCarrier(summary, vectors, investedMaterial)
        val interpretation =
          CompensationInterpretation.derivedDecision(
            summary = carrier.summary,
            vectors = carrier.vectors,
            investedMaterial = carrier.investedMaterial,
            phase = semantic.phase,
            fenBefore = semantic.fen,
            playedMove = semantic.playedMove
          )
        Option.when(carrier.hasSignal && interpretation.exists(_.accepted))(carrier)
        }.flatten
      }

  private def enrichEvidence(
      current: List[String],
      ideas: List[StrategyIdeaSignal],
      targets: List[StrategyDirectionalTarget]
  ): List[String] =
    val ideaEvidence = ideas.map(playerFacingIdeaText)
    val targetEvidence =
      targets.flatMap { target =>
        val square = target.targetSquare.trim.toLowerCase
        Option.when(ChessSquarePattern.matches(square))(s"objective: ${pieceName(target.piece)} toward $square")
      }
    (current ++ ideaEvidence ++ targetEvidence).map(_.trim).filter(_.nonEmpty).distinct.take(12)

  private def evidence(
      ownerSide: String,
      kind: String,
      readiness: String,
      source: EvidenceSourceId,
      confidence: Double,
      focusSquares: List[String] = Nil,
      focusFiles: List[String] = Nil,
      focusDiagonals: List[String] = Nil,
      focusZone: Option[String] = None,
      targetSquare: Option[String] = None,
      relationKind: Option[String] = None,
      relationFocusSquares: List[String] = Nil,
      beneficiaryPieces: List[String] = Nil,
      factIds: List[String] = Nil,
      typedFactIds: List[FactId] = Nil
  ): StrategicIdeaEvidence =
    StrategicIdeaEvidence.from(
      ownerSide = ownerSide,
      kind = kind,
      readiness = readiness,
      source = source,
      confidence = confidence,
      focusSquares = focusSquares,
      focusFiles = focusFiles,
      focusDiagonals = focusDiagonals,
      focusZone = focusZone,
      targetSquare = targetSquare,
      beneficiaryPieces = beneficiaryPieces,
      factIds = factIds,
      typedFactIds = typedFactIds,
      relationKind = relationKind,
      relationFocusSquares = relationFocusSquares
    )

  private def mergeReadiness(readinessValues: Iterable[String]): String =
    if readinessValues.exists(_ == StrategicIdeaReadiness.Ready) then StrategicIdeaReadiness.Ready
    else if readinessValues.exists(_ == StrategicIdeaReadiness.Build) then StrategicIdeaReadiness.Build
    else if readinessValues.exists(_ == StrategicIdeaReadiness.Premature) then StrategicIdeaReadiness.Premature
    else if readinessValues.exists(_ == StrategicIdeaReadiness.Blocked) then StrategicIdeaReadiness.Blocked
    else StrategicIdeaReadiness.Build

  private def ideaReadinessFromDirectionalTarget(readiness: String): String =
    readiness match
      case DirectionalTargetReadiness.Build     => StrategicIdeaReadiness.Build
      case DirectionalTargetReadiness.Contested => StrategicIdeaReadiness.Build
      case DirectionalTargetReadiness.Premature => StrategicIdeaReadiness.Premature
      case DirectionalTargetReadiness.Blocked   => StrategicIdeaReadiness.Blocked
      case _                                    => StrategicIdeaReadiness.Build

  private def readinessBonus(readiness: String): Double =
    readiness match
      case DirectionalTargetReadiness.Build     => 0.04
      case DirectionalTargetReadiness.Contested => 0.00
      case DirectionalTargetReadiness.Premature => -0.02
      case DirectionalTargetReadiness.Blocked   => -0.05
      case _                                    => 0.0

  private def breakImpactBonus(impact: String): Double =
    impact.trim.toLowerCase match
      case "high"   => 0.08
      case "medium" => 0.04
      case _        => 0.0

  private def breakImpactBonusFromInt(impact: Int): Double =
    if impact >= 180 then 0.08
    else if impact >= 100 then 0.05
    else if impact >= 40 then 0.02
    else 0.0

  private def isPreventiveWithoutCounterplaySuppression(prevented: PreventedPlan): Boolean =
    (
      prevented.deniedSquares.nonEmpty ||
        prevented.preventedThreatType.isDefined ||
        prevented.mobilityDelta < 0 ||
        prevented.counterplayScoreDrop > 0 ||
        prevented.deniedResourceClass.isDefined ||
        prevented.defensiveSufficiency.exists(_ > 0)
    ) &&
      !isCounterplaySuppression(prevented)

  private def isCounterplaySuppression(prevented: PreventedPlan): Boolean =
    prevented.breakNeutralized.isDefined ||
      prevented.deniedResourceClass.contains("break") ||
      prevented.deniedEntryScope.exists(scope => scope == "file" || scope == "sector") ||
      prevented.breakNeutralizationStrength.exists(_ >= 60) ||
      prevented.counterplayScoreDrop >= 100 ||
      prevented.deniedSquares.size >= 2 ||
      prevented.mobilityDelta <= -2

  private def isThreatDrivenProphylaxis(threats: ThreatAnalysis): Boolean =
    threats.prophylaxisNeeded &&
      !threats.immediateThreat &&
      threats.resourceAvailable &&
      !threats.counterThreatBetter &&
      (threats.strategicThreat || threats.defense.prophylaxisNeeded) &&
      threats.maxLossIfIgnored > 0 &&
      threats.maxLossIfIgnored < 250

  private def isThreatDrivenCounterplaySuppression(
      threats: ThreatAnalysis,
      opponentPawnAnalysis: Option[PawnPlayAnalysis],
      preventedPlans: List[PreventedPlan]
  ): Boolean =
    threats.maxLossIfIgnored >= 120 &&
      !threats.threatIgnorable &&
      (
        threats.strategicThreat ||
          opponentPawnAnalysis.exists(_.counterBreak) ||
          preventedPlans.exists(isCounterplaySuppression)
      )

  private def topPlansFor(side: String, semantic: StrategicIdeaSemanticContext): List[PlanMatch] =
    semantic.plans
      .filter(plan => matchesSide(plan.plan.color, side))
      .sortBy(plan => -plan.score)

  private def structureIs(semantic: StrategicIdeaSemanticContext, structureId: StructureId): Boolean =
    semantic.structureProfile.exists(_.primary == structureId)

  private def hasBishopPinWatch(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.board.exists { board =>
      if side == "white" then
        hasPiece(board, Color.White, Square.F3, Knight) &&
        hasPiece(board, Color.Black, Square.C8, Bishop) &&
        diagonalClear(board, Square.C8, Square.G4)
      else
        hasPiece(board, Color.Black, Square.F6, Knight) &&
        hasPiece(board, Color.White, Square.C1, Bishop) &&
        diagonalClear(board, Square.C1, Square.G5)
    }

  private def hasQueensideClampWatch(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.board.exists { board =>
      if side == "white" then
        pawnAt(board, Color.White, Square.C4) &&
        pawnAt(board, Color.White, Square.D5) &&
        pawnAt(board, Color.White, Square.E4) &&
        pawnAt(board, Color.Black, Square.D6) &&
        pawnAt(board, Color.Black, Square.E5) &&
        pawnAt(board, Color.Black, Square.G6) &&
        pawnAt(board, Color.Black, Square.B7)
      else
        pawnAt(board, Color.Black, Square.C5) &&
        pawnAt(board, Color.Black, Square.D4) &&
        pawnAt(board, Color.Black, Square.E5) &&
        pawnAt(board, Color.White, Square.D3) &&
        pawnAt(board, Color.White, Square.E4) &&
        pawnAt(board, Color.White, Square.G3) &&
        pawnAt(board, Color.White, Square.B2)
    }

  private def lineAccessFacts(
      side: String,
      endpoint: Square,
      semantic: StrategicIdeaSemanticContext
  ): Option[(List[String], Option[String], List[String])] =
    semantic.board.flatMap { board =>
      val color = sideColor(side)
      val open = isOpenFile(board, endpoint.file)
      val semiOpen = isSemiOpenFileFor(board, endpoint.file, color)
      val seventh = isSeventhRankFor(side, endpoint)
      Option.when(open || semiOpen || seventh) {
        val files = Option.when(open || semiOpen)(List(fileToken(endpoint.file))).getOrElse(Nil)
        val facts =
          List(
            Option.when(open)(s"open_file_${fileToken(endpoint.file)}"),
            Option.when(semiOpen)(s"semi_open_file_${fileToken(endpoint.file)}"),
            Option.when(seventh)("seventh_rank_entry")
          ).flatten
        val zone =
          if seventh then Some("back rank")
          else zoneFromFileToken(fileToken(endpoint.file))
        (files, zone, facts)
      }
    }

  private def occupiedStrongKnightSquaresFor(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Set[String] =
    semantic.positionalFeatures.collect {
      case PositionalTag.StrongKnight(square, color)
          if matchesSide(color, side) &&
            semantic.board.exists(board =>
              hasPiece(board, sideColor(side), square, Knight) || hasPiece(board, sideColor(side), square, Bishop)
            ) =>
        square.key
    }.toSet

  private def hasStablePlanEvidence(
      semantic: StrategicIdeaSemanticContext,
      kind: String
  ): Boolean =
    semantic.strategicPlanExperiments.exists { experiment =>
      planEvidenceAppliesToKind(experiment, kind) &&
        isPlayableExperiment(experiment) &&
        !experiment.moveOrderSensitive &&
        (
          experiment.bestReplyStable ||
            experiment.futureSnapshotAligned ||
            experiment.counterBreakNeutralized ||
            experiment.supportProbeCount > 0
        )
    }

  private def familyForKind(kind: String): String =
    kind match
      case StrategicIdeaKind.PawnBreak | StrategicIdeaKind.KingAttackBuildUp =>
        StrategicIdeaFamily.ForcingOrTacticalNow
      case StrategicIdeaKind.SpaceGainOrRestriction | StrategicIdeaKind.TargetFixing |
          StrategicIdeaKind.LineOccupation | StrategicIdeaKind.OutpostCreationOrOccupation |
          StrategicIdeaKind.MinorPieceImbalanceExploitation =>
        StrategicIdeaFamily.SlowStructural
      case StrategicIdeaKind.Prophylaxis | StrategicIdeaKind.CounterplaySuppression =>
        StrategicIdeaFamily.PreventionOrSuppression
      case StrategicIdeaKind.FavorableTradeOrTransformation =>
        StrategicIdeaFamily.ConversionOrTransformation
      case _ =>
        StrategicIdeaFamily.ForcingOrTacticalNow

  private def candidateHasSource(candidate: Candidate, source: EvidenceSourceId): Boolean =
    candidate.evidenceRefs.contains(EvidenceRef.Source(source))

  private def candidateHasAnySource(candidate: Candidate, sources: Set[EvidenceSourceId]): Boolean =
    sources.exists(candidateHasSource(candidate, _))

  private def candidateHasAnyFact(candidate: Candidate, predicate: String => Boolean): Boolean =
    candidate.evidenceRefs.exists(ref => predicate(ref.wireKey))

  private def hasBroadSpaceAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    candidateHasAnySource(
      candidate,
      Set(
        EvidenceSourceId.SpaceAdvantageTag,
        EvidenceSourceId.ColorComplexClamp,
        EvidenceSourceId.LockedCenterBind,
        EvidenceSourceId.MaroczyBindProfile,
        EvidenceSourceId.IqpSpaceBridge,
        EvidenceSourceId.IqpCentralPresence,
        EvidenceSourceId.PlanMatchSpaceAdvantage
      )
    ) ||
      structureIs(semantic, StructureId.MaroczyBind) ||
      structureIs(semantic, StructureId.IQPWhite)

  private def hasProfileSpaceAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    candidateHasAnySource(
      candidate,
      Set(
        EvidenceSourceId.MaroczyBindProfile,
        EvidenceSourceId.IqpSpaceBridge,
        EvidenceSourceId.IqpCentralPresence
      )
    ) ||
      structureIs(semantic, StructureId.MaroczyBind) ||
      structureIs(semantic, StructureId.IQPWhite)

  private def isGenericTargetFixing(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    !hasStrongTargetFixingAnchor(candidate, semantic)

  private def flankTargetSquares(flank: String, squares: List[String]): List[String] =
    val files =
      normalizeFactToken(flank) match
        case "queenside" => Set('a', 'b', 'c', 'd')
        case "kingside"  => Set('e', 'f', 'g', 'h')
        case _           => Set.empty[Char]
    if files.isEmpty then Nil
    else
      squares.filter(square => square.headOption.exists(files.contains))

  private def hasStrongTargetFixingAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    val compensationMaterialDeficit =
      semantic.positionFeatures.exists(features => hasCompensationMaterialDeficitFor(candidate.ownerSide, features))
    val reviewedWeaknessExperiment =
      semantic.strategicPlanExperiments.exists(experiment =>
        experiment.themeL1 == PlanTaxonomy.PlanTheme.WeaknessFixation.id &&
          isPlayableExperiment(experiment)
      )
    val corroboratedWeakness =
      candidate.focusSquares.nonEmpty &&
        candidateHasAnySource(candidate, Set(EvidenceSourceId.WeakComplexFixation, EvidenceSourceId.EnemyWeakSquare))
    val directionalTargetWithCorroboration =
      !compensationMaterialDeficit &&
        !candidateHasSource(candidate, EvidenceSourceId.MinorityAttackSemantic) &&
        reviewedWeaknessExperiment &&
        candidateHasSource(candidate, EvidenceSourceId.DirectionalTargetFixation) &&
        corroboratedWeakness
    val exactStructuralTarget =
      !compensationMaterialDeficit &&
        candidateHasSource(candidate, EvidenceSourceId.PlanMatchTargetFixing) &&
        reviewedWeaknessExperiment &&
        corroboratedWeakness
    candidateHasAnySource(
      candidate,
      Set(
        EvidenceSourceId.CarlsbadFixationProfile,
        EvidenceSourceId.CompensationTargetFixation
      )
    ) ||
      directionalTargetWithCorroboration ||
      exactStructuralTarget

  private def hasStrongLineAnchor(candidate: Candidate): Boolean =
    candidateHasAnySource(
      candidate,
      Set(
        EvidenceSourceId.OpenFileControl,
        EvidenceSourceId.OccupiedLineControl,
        EvidenceSourceId.DoubledRooks,
        EvidenceSourceId.RookOnSeventh,
        EvidenceSourceId.PlanMatchLineOccupation
      )
    ) ||
      (candidate.sourceCount >= 2 && candidate.score >= 0.80)

  private def hasConcreteLineOccupationAnchor(candidate: Candidate): Boolean =
    candidateHasAnySource(
      candidate,
      Set(
        EvidenceSourceId.OpenFileControl,
        EvidenceSourceId.OccupiedLineControl,
        EvidenceSourceId.DoubledRooks,
        EvidenceSourceId.RookOnSeventh
      )
    )

  private def hasCompensationLineAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.positionFeatures.exists(features => hasCompensationMaterialDeficitFor(candidate.ownerSide, features)) &&
      candidateHasAnySource(candidate, Set(EvidenceSourceId.CompensationOpenLines, EvidenceSourceId.DelayedRecoveryWindow))

  private def hasCompensationTargetFixingAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.positionFeatures.exists(features => hasCompensationMaterialDeficitFor(candidate.ownerSide, features)) &&
      candidateHasSource(candidate, EvidenceSourceId.CompensationTargetFixation)

  private def hasRouteLineAnchor(candidate: Candidate): Boolean =
    candidateHasAnySource(
      candidate,
      Set(EvidenceSourceId.RouteLineAccess, EvidenceSourceId.DirectionalLineAccess, EvidenceSourceId.LineControlFeatures)
    ) &&
      candidateHasAnyFact(candidate, fact =>
          fact.startsWith("open_file_") ||
          fact.startsWith("semi_open_file_") ||
          fact == "seventh_rank_entry" ||
          fact == "line_control_shape"
      )

  private def hasStrongMinorPieceAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    candidateHasAnySource(candidate, Set(EvidenceSourceId.StrongKnightVsBadBishop, EvidenceSourceId.FrenchMinorPieceProfile)) ||
      (
        structureIs(semantic, StructureId.FrenchAdvanceChain) &&
          candidateHasAnySource(
            candidate,
            Set(EvidenceSourceId.EnemyBadBishop, EvidenceSourceId.GoodBishop, EvidenceSourceId.BishopPairAdvantage)
          )
      )

  private def hasStableOutpostAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    candidateHasSource(candidate, EvidenceSourceId.OutpostTag) ||
      candidate.focusSquares.exists(square =>
        occupiedStrongKnightSquaresFor(candidate.ownerSide, semantic).contains(square)
      ) ||
      (
        hasStablePlanEvidence(semantic, StrategicIdeaKind.OutpostCreationOrOccupation) &&
          candidateHasAnySource(
            candidate,
            Set(EvidenceSourceId.StrongKnight, EvidenceSourceId.EntrenchedPieceState, EvidenceSourceId.RouteOutpostAccess)
          )
      )

  private def hasRealProphylaxisAnchor(
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.preventedPlans.exists(isPreventiveWithoutCounterplaySuppression) ||
      semantic.threatsToUs.exists(isThreatDrivenProphylaxis) ||
      hasStablePlanEvidence(semantic, StrategicIdeaKind.Prophylaxis)

  private def hasSupportedProphylaxisContext(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    hasRealProphylaxisAnchor(semantic) ||
      (
        !semantic.positionFeatures.exists(features => hasCompensationMaterialDeficitFor(side, features)) &&
        topPlansFor(side, semantic).exists(plan =>
          plan.plan.id == PlanId.Prophylaxis || plan.plan.id == PlanId.DefensiveConsolidation
        ) &&
          (hasBishopPinWatch(side, semantic) || hasQueensideClampWatch(side, semantic))
      )

  private def hasPreventionOrSuppressionAnchor(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.preventedPlans.exists(plan =>
      isPreventiveWithoutCounterplaySuppression(plan) || isCounterplaySuppression(plan)
    ) ||
      semantic.threatsToUs.exists(threats =>
        isThreatDrivenProphylaxis(threats) ||
          isThreatDrivenCounterplaySuppression(threats, semantic.opponentPawnAnalysis, semantic.preventedPlans)
      ) ||
      hasSupportedProphylaxisContext(side, semantic) ||
      hasStablePlanEvidence(semantic, StrategicIdeaKind.Prophylaxis) ||
      hasStablePlanEvidence(semantic, StrategicIdeaKind.CounterplaySuppression)

  private def hasCompensationAttackAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.positionFeatures.exists(features => hasCompensationMaterialDeficitFor(candidate.ownerSide, features)) &&
      candidateHasAnySource(
        candidate,
        Set(
          EvidenceSourceId.CompensationDevelopmentLead,
          EvidenceSourceId.CompensationKingWindow,
          EvidenceSourceId.CompensationDiagonalBattery
        )
      )

  private def hasStrongKingAttackAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    hasCompensationAttackAnchor(candidate, semantic) ||
      candidateHasAnySource(
        candidate,
        Set(
          EvidenceSourceId.MateNet,
          EvidenceSourceId.KingRingPressure,
          EvidenceSourceId.AttackingThreatAnalysis,
          EvidenceSourceId.OppositeSideStorm,
          EvidenceSourceId.FianchettoAssaultProfile,
          EvidenceSourceId.PlanMatchKingAttack
        )
      )

  private def hasCompensationSuppressionAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.positionFeatures.exists(features => hasCompensationMaterialDeficitFor(candidate.ownerSide, features)) &&
      candidateHasSource(candidate, EvidenceSourceId.CompensationCounterplayDenial)

  private def hasWeakKingWindowCompensationContext(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.positionFeatures.exists { features =>
      hasCompensationMaterialDeficitFor(side, features) &&
      isCompensationEligiblePhase(semantic) &&
      enemyKingCastledSideFor(side, features) != "none" &&
      enemyKingExposedFilesFor(side, features) == 0 &&
      enemyKingRingAttackedFor(side, features) < 2 &&
      attackersCountFor(side, features) < 2
    }

  private def hasStrongConversionAnchor(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    candidateHasAnySource(
      candidate,
      Set(
        EvidenceSourceId.RemovingTheDefender,
        EvidenceSourceId.ExchangeAvailabilityBridge,
        EvidenceSourceId.IqpSimplificationProfile
      )
    ) ||
      (
        candidateHasSource(candidate, EvidenceSourceId.CaptureExchangeTransformation) &&
          candidateHasAnySource(
            candidate,
            Set(
              EvidenceSourceId.RemovingTheDefender,
              EvidenceSourceId.ExchangeAvailabilityBridge,
              EvidenceSourceId.IqpSimplificationProfile
            )
          )
      ) ||
      (
        candidateHasSource(candidate, EvidenceSourceId.PlanMatchTransformation) &&
          candidateHasAnySource(
            candidate,
            Set(
              EvidenceSourceId.RemovingTheDefender,
              EvidenceSourceId.ExchangeAvailabilityBridge,
              EvidenceSourceId.IqpSimplificationProfile
            )
          )
      ) ||
      (
        candidateHasSource(candidate, EvidenceSourceId.ClassificationTransformationWindow) &&
          candidateHasAnySource(
            candidate,
            Set(
              EvidenceSourceId.RemovingTheDefender,
              EvidenceSourceId.ExchangeAvailabilityBridge,
              EvidenceSourceId.IqpSimplificationProfile
            )
          )
      ) ||
      (
        structureIs(semantic, StructureId.IQPBlack) &&
          candidateHasSource(candidate, EvidenceSourceId.ClassificationTransformationWindow) &&
          candidateHasAnySource(candidate, Set(EvidenceSourceId.ExchangeAvailabilityBridge, EvidenceSourceId.IqpSimplificationProfile))
      ) ||
      (
        hasStablePlanEvidence(semantic, StrategicIdeaKind.FavorableTradeOrTransformation) &&
          candidateHasAnySource(
            candidate,
            Set(
              EvidenceSourceId.RemovingTheDefender,
              EvidenceSourceId.ExchangeAvailabilityBridge,
              EvidenceSourceId.IqpSimplificationProfile,
              EvidenceSourceId.PlanMatchTransformation
            )
          )
      )

  private def isWeakConversionWindowOnly(
      candidate: Candidate,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    (
      candidateHasSource(candidate, EvidenceSourceId.ClassificationTransformationWindow) ||
        candidateHasSource(candidate, EvidenceSourceId.CaptureExchangeTransformation)
    ) &&
      !hasStrongConversionAnchor(candidate, semantic)

  private def hasDefenderTagExchangeSupport(candidate: Candidate): Boolean =
    candidateHasSource(candidate, EvidenceSourceId.CaptureExchangeTransformation) &&
      candidateHasAnyFact(candidate, _.contains("removing_defender_tag_support_"))

  private def hasSoftTransformationPlanSupport(candidate: Candidate): Boolean =
    candidateHasSource(candidate, EvidenceSourceId.PlanMatchTransformation) &&
      candidateHasAnyFact(candidate, _.contains("soft_transformation_plan_support"))

  private def hasStructuredIqpConversionWindow(
      members: List[Candidate],
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    structureIs(semantic, StructureId.IQPBlack) &&
      semantic.sideToMove == "white" &&
      members.exists(candidate =>
        candidate.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
          (
            candidateHasSource(candidate, EvidenceSourceId.IqpSimplificationProfile) ||
              candidateHasSource(candidate, EvidenceSourceId.ExchangeAvailabilityBridge) ||
              candidateHasSource(candidate, EvidenceSourceId.CaptureExchangeTransformation) ||
              candidateHasSource(candidate, EvidenceSourceId.ClassificationTransformationWindow)
          )
      )

  private def hasConcretePawnBreakAnchor(candidate: Candidate): Boolean =
    val moveOrFileLinked =
      candidateHasAnySource(
        candidate,
        Set(
          EvidenceSourceId.PawnPlayBreakReady,
          EvidenceSourceId.FileOpeningConsequence,
          EvidenceSourceId.FrenchF6BreakSeed
        )
      )
    val centralBreakWithConsequence =
      candidateHasSource(candidate, EvidenceSourceId.CentralBreakTension) &&
        candidateHasAnySource(
          candidate,
          Set(
            EvidenceSourceId.PawnPlayBreakReady,
            EvidenceSourceId.FileOpeningConsequence,
            EvidenceSourceId.PlanMatchBreakPreparation
          )
        )
    moveOrFileLinked || centralBreakWithConsequence

  private def preferredSlowStructuralKind(
      familyMembers: List[Candidate],
      semantic: StrategicIdeaSemanticContext
  ): Option[String] =
    val byKind = familyMembers.map(candidate => candidate.kind -> candidate).toMap
    val space = byKind.get(StrategicIdeaKind.SpaceGainOrRestriction)
    val outpost = byKind.get(StrategicIdeaKind.OutpostCreationOrOccupation)
    val minor = byKind.get(StrategicIdeaKind.MinorPieceImbalanceExploitation)
    val target = byKind.get(StrategicIdeaKind.TargetFixing)
    val line = byKind.get(StrategicIdeaKind.LineOccupation)
    val strongMinor = minor.filter(candidate => hasStrongMinorPieceAnchor(candidate, semantic))
    val strongTarget = target.filter(candidate => hasStrongTargetFixingAnchor(candidate, semantic))
    val concreteLine = line.filter(candidate => hasConcreteLineOccupationAnchor(candidate))

    if space.exists(hasProfileSpaceAnchor(_, semantic)) then Some(StrategicIdeaKind.SpaceGainOrRestriction)
    else if outpost.exists(hasStableOutpostAnchor(_, semantic)) &&
        !strongMinor.exists(candidate => candidate.score >= outpost.map(_.score).getOrElse(0.0) - 0.02)
    then Some(StrategicIdeaKind.OutpostCreationOrOccupation)
    else if strongMinor.nonEmpty then Some(StrategicIdeaKind.MinorPieceImbalanceExploitation)
    else if concreteLine.nonEmpty &&
        target.exists(candidate =>
          candidateHasSource(candidate, EvidenceSourceId.PlanMatchTargetFixing) &&
            !structureIs(semantic, StructureId.Carlsbad)
        )
    then Some(StrategicIdeaKind.LineOccupation)
    else if strongTarget.nonEmpty then Some(StrategicIdeaKind.TargetFixing)
    else if line.exists(candidate =>
        hasStrongLineAnchor(candidate) ||
          hasCompensationLineAnchor(candidate, semantic) ||
          hasRouteLineAnchor(candidate) ||
          target.exists(isGenericTargetFixing(_, semantic))
      )
    then Some(StrategicIdeaKind.LineOccupation)
    else if space.exists(hasBroadSpaceAnchor(_, semantic)) then Some(StrategicIdeaKind.SpaceGainOrRestriction)
    else familyMembers.sortBy(candidate => (-candidate.score, candidate.kind)).headOption.map(_.kind)

  private def normalizeSquareKeys(keys: List[String]): List[String] =
    keys.flatMap(squareFromKey).map(_.key).distinct

  private def hasPiece(board: Board, color: Color, square: Square, role: Role): Boolean =
    board.pieceAt(square).exists(piece => piece.color == color && piece.role == role)

  private def pawnAt(board: Board, color: Color, square: Square): Boolean =
    hasPiece(board, color, square, Pawn)

  private def diagonalClear(board: Board, from: Square, to: Square): Boolean =
    val fileStep = math.signum(to.file.value - from.file.value)
    val rankStep = math.signum(to.rank.value - from.rank.value)
    val fileDiff = (to.file.value - from.file.value).abs
    val rankDiff = (to.rank.value - from.rank.value).abs
    if fileDiff != rankDiff || fileDiff == 0 then false
    else
      (1 until fileDiff).forall { offset =>
        Square
          .at(from.file.value + offset * fileStep, from.rank.value + offset * rankStep)
          .forall(board.pieceAt(_).isEmpty)
      }

  private def zoneFromSquareKeys(keys: List[String]): Option[String] =
    zoneFromSquares(keys.flatMap(squareFromKey))

  private def zoneFromSquares(squares: List[Square]): Option[String] =
    mostCommon(squares.flatMap(zoneFromSquare))

  private def normalizeFactToken(value: String): String =
    Option(value)
      .map(v => NonAlphaNumPattern.replaceAllIn(v.trim.toLowerCase, "_").stripPrefix("_").stripSuffix("_"))
      .filter(_.nonEmpty)
      .getOrElse("unknown")

  private def weakComplexConfidence(weakness: WeakComplex): Double =
    weakness.cause.trim.toLowerCase match
      case "backward pawn" => 0.76
      case "hanging pawns" => 0.72
      case "doubled pawns" => 0.68
      case "holes"         => if weakness.isOutpost then 0.64 else 0.60
      case _               => 0.66

  private def mostCommon(values: List[String]): Option[String] =
    values.groupBy(identity).toList.sortBy { case (value, grouped) => (-grouped.size, value) }.headOption.map(_._1)

  private def focusSummary(
      focusSquares: List[String],
      focusFiles: List[String],
      focusDiagonals: List[String],
      focusZone: Option[String]
  ): String =
    if focusSquares.nonEmpty then focusSquares.take(4).mkString(", ")
    else if focusFiles.nonEmpty then joinLowerTerms(focusFiles.take(3).map(file => s"$file-file"))
    else if focusDiagonals.nonEmpty then joinLowerTerms(focusDiagonals.take(2).map(diagonal => s"the $diagonal diagonal"))
    else focusZone.flatMap(zoneFocusText).getOrElse("the key sector")

  private def focusJoiner(focus: String): String =
    val low = Option(focus).getOrElse("").trim.toLowerCase
    if low.startsWith("on ") || low.startsWith("along ") || low.startsWith("in ") then focus.trim
    else if low.startsWith("the ") then s"around ${focus.trim}"
    else s"around ${focus.trim}"

  private def pawnBreakText(
      ownerSide: String,
      focusSquares: List[String],
      focusFiles: List[String],
      focusZone: Option[String]
  ): String =
    val breaks =
      focusSquares.take(3).filter(_.nonEmpty).map(square => renderBreakToken(ownerSide, square))
    if breaks.nonEmpty then
      if breaks.size == 1 then s"the ${breaks.head} break"
      else s"breaks with ${joinLowerTerms(breaks)}"
    else
      val fileBreaks = focusFiles.take(2).filter(_.nonEmpty).map(file => s"the $file-pawn break")
      if fileBreaks.nonEmpty then joinLowerTerms(fileBreaks)
      else focusZone.flatMap(zoneFocusText).map(zone => s"pawn play in $zone").getOrElse("a pawn break")

  private def pressureText(
      focusSquares: List[String],
      focusFiles: List[String],
      focusDiagonals: List[String],
      focusZone: Option[String],
      fallback: String
  ): String =
    pressureAnchor(focusSquares, focusFiles, focusDiagonals, focusZone).map(anchor => s"pressure $anchor").getOrElse(fallback)

  private def exchangeText(signal: StrategyIdeaSignal): String =
    val refs = signal.evidenceRefs.map(_.trim.toLowerCase)
    val focusSquares = signal.focusSquares.map(_.trim.toLowerCase).filter(ChessSquarePattern.matches)
    val focusZone = signal.focusZone
    if refs.contains("source:passed_pawn_conversion_motif") && refs.contains("passed_pawn_conversion_shape") then
      val passedPawnSquare =
        focusSquares.find(square => refs.contains(s"passed_pawn_$square")).orElse(focusSquares.headOption)
      if refs.contains("pawn_promotion") then
        passedPawnSquare.map(square => s"promotion cue on $square").getOrElse("promotion cue")
      else passedPawnSquare.map(square => s"passed-pawn cue around $square").getOrElse("passed-pawn cue")
    else if refs.contains("source:rook_endgame_pattern") && refs.contains("rook_endgame_pattern_shape") then
      val rookBehindPasserSquare =
        focusSquares.find(square => refs.contains(s"rook_behind_passer_square_$square"))
      val facts =
        List(
          rookBehindPasserSquare.map(square => s"rook-behind-passer cue around $square"),
          Option.when(refs.contains("king_cut_off"))("king cut-off cue")
        ).flatten
      facts match
        case fact :: Nil => fact
        case _           => "rook-endgame cue"
    else if refs.contains("source:endgame_technique_motif") && refs.contains("endgame_technique_shape") then
      val facts =
        List(
          Option.when(refs.contains("opposition_direct"))("direct-opposition cue"),
          Option.when(refs.contains("opposition_distant"))("distant-opposition cue"),
          Option.when(refs.contains("opposition_diagonal"))("diagonal-opposition cue"),
          Option.when(refs.contains("zugzwang_shape"))("zugzwang cue"),
          Option.when(refs.contains("king_activity_shape"))("king-activity cue")
        ).flatten
      facts match
        case fact :: Nil => fact
        case _           => "endgame-pattern cue"
    else if focusSquares.nonEmpty then s"exchanges on ${joinLowerTerms(focusSquares.take(3))}"
    else focusZone.flatMap(zoneFocusText).map(zone => s"favorable exchanges in $zone").getOrElse("favorable exchanges")

  private def targetFixingText(focusSquares: List[String], focusZone: Option[String]): String =
    if focusSquares.nonEmpty then s"fixed targets on ${joinLowerTerms(focusSquares.take(3))}"
    else focusZone.flatMap(zoneFocusText).map(zone => s"fixed targets in $zone").getOrElse("fixed targets")

  private def outpostText(signal: StrategyIdeaSignal): String =
    val refs = signal.evidenceRefs.map(_.trim.toLowerCase).toSet
    val focusSquares = signal.focusSquares.map(_.trim.toLowerCase).filter(ChessSquarePattern.matches)
    if refs.contains(sourceWire(EvidenceSourceId.RouteOutpostAccess)) then
      focusSquares.headOption.map(square => s"minor-piece outpost access around $square")
        .getOrElse("minor-piece outpost access")
    else if refs.contains(sourceWire(EvidenceSourceId.DirectionalOutpostAccess)) then
      focusSquares.headOption.map(square => s"minor-piece outpost cue around $square")
        .getOrElse("minor-piece outpost cue")
    else
      focusSquares.headOption.map(square => s"an outpost on $square")
        .orElse(signal.focusZone.flatMap(zoneFocusText).map(zone => s"an outpost in $zone"))
        .getOrElse("an outpost")

  private def spaceText(focusSquares: List[String], focusZone: Option[String]): String =
    focusZone.flatMap(zoneFocusText).map(zone => s"space in $zone")
      .orElse(Option.when(focusSquares.nonEmpty)(s"space around ${joinLowerTerms(focusSquares.take(3))}"))
      .getOrElse("space")

  private def counterplayText(
      focusSquares: List[String],
      focusFiles: List[String],
      focusDiagonals: List[String],
      focusZone: Option[String]
  ): String =
    pressureAnchor(focusSquares, focusFiles, focusDiagonals, focusZone)
      .map(anchor => s"stopping counterplay $anchor")
      .getOrElse("stopping counterplay")

  private def prophylaxisText(
      focusSquares: List[String],
      focusFiles: List[String],
      focusDiagonals: List[String],
      focusZone: Option[String]
  ): String =
    focusSquares.headOption.map(square => s"keeping the opponent out of $square")
      .orElse(pressureAnchor(focusSquares, focusFiles, focusDiagonals, focusZone).map(anchor => s"slowing the opponent $anchor"))
      .getOrElse("slowing the opponent's next active idea")

  private def minorPieceText(focusSquares: List[String], focusZone: Option[String]): String =
    if focusSquares.nonEmpty then s"the minor-piece imbalance on ${joinLowerTerms(focusSquares.take(2))}"
    else focusZone.flatMap(zoneFocusText).map(zone => s"the minor-piece imbalance in $zone").getOrElse("the minor-piece imbalance")

  private def pressureAnchor(
      focusSquares: List[String],
      focusFiles: List[String],
      focusDiagonals: List[String],
      focusZone: Option[String]
  ): Option[String] =
    if focusSquares.nonEmpty then Some(s"on ${joinLowerTerms(focusSquares.take(3))}")
    else if focusFiles.nonEmpty then
      Some(s"along ${joinLowerTerms(focusFiles.take(2).map(file => s"the $file-file"))}")
    else if focusDiagonals.nonEmpty then
      Some(s"along ${joinLowerTerms(focusDiagonals.take(2).map(diagonal => s"the $diagonal diagonal"))}")
    else focusZone.flatMap(zoneFocusText).map(zone => s"in $zone")

  private def zoneFocusText(raw: String): Option[String] =
    Option(raw).map(_.trim.toLowerCase).filter(_.nonEmpty).map {
      case "center"    => "the center"
      case "kingside"  => "the kingside"
      case "queenside" => "the queenside"
      case other       => other
    }

  private def renderBreakToken(ownerSide: String, square: String): String =
    val normalized = Option(square).map(_.trim).getOrElse("")
    if ChessSquarePattern.matches(normalized) && Option(ownerSide).exists(_.trim.equalsIgnoreCase("black")) then s"...$normalized"
    else normalized

  private def routePurposeContainsLinePressure(route: StrategyPieceRoute): Boolean =
    val low = Option(route.purpose).getOrElse("").trim.toLowerCase
    low.contains("open-file occupation") ||
      low.contains("line access") ||
      low.contains("file") ||
      low.contains("clamp")

  private def targetCarriesLinePressure(target: StrategyDirectionalTarget): Boolean =
    target.strategicReasons.exists { reason =>
      val low = Option(reason).getOrElse("").trim.toLowerCase
      low.contains("line access") || low.contains("file")
    }

  private def joinLowerTerms(values: List[String]): String =
    values.map(_.trim).filter(_.nonEmpty).distinct match
      case Nil          => ""
      case head :: Nil  => head
      case a :: b :: Nil => s"$a and $b"
      case many         => s"${many.dropRight(1).mkString(", ")}, and ${many.last}"

  private def formatCompensationVector(label: String, score: Double): String =
    s"$label (${f"${score.max(0.3).min(0.9)}%.1f"})"

  private def pieceName(code: String): String =
    code match
      case "N" => "knight"
      case "B" => "bishop"
      case "R" => "rook"
      case "Q" => "queen"
      case "K" => "king"
      case "P" => "pawn"
      case _   => "piece"

  private def normalizeFileToken(value: String): Option[String] =
    BreakFileToken.extract(value)

  private def zoneFromFileToken(file: String): Option[String] =
    file.trim.toLowerCase.headOption.flatMap {
      case ch if ch <= 'c'                => Some("queenside")
      case ch if ch >= 'f'                => Some("kingside")
      case ch if ch == 'd' || ch == 'e'   => Some("center")
      case _                              => None
    }

  private def fileFromToken(token: String): Option[File] =
    token.headOption.flatMap(ch => File.all.find(_.char == ch))

  private def fileToken(file: File): String = file.char.toString

  private def squareFromKey(key: String): Option[Square] =
    Square.all.find(_.key == Option(key).map(_.trim.toLowerCase).getOrElse(""))

  private def sideColor(side: String): Color =
    if side == "white" then Color.White else Color.Black

  private def matchesSide(color: Color, side: String): Boolean =
    color.white == (side == "white")

  private def materialEdgeFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.materialPhase.materialDiff else -features.materialPhase.materialDiff

  private def hasCompensationMaterialDeficitFor(side: String, features: PositionFeatures): Boolean =
    val edge = materialEdgeFor(side, features)
    edge <= -1 && edge >= -3

  private def developmentLagFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.activity.whiteDevelopmentLag else features.activity.blackDevelopmentLag

  private def enemyDevelopmentLagFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.activity.blackDevelopmentLag else features.activity.whiteDevelopmentLag

  private def developmentLeadFor(side: String, features: PositionFeatures): Int =
    (enemyDevelopmentLagFor(side, features) - developmentLagFor(side, features)).max(0)

  private def openFilesCount(features: PositionFeatures): Int = features.lineControl.openFilesCount

  private def semiOpenFilesFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.lineControl.whiteSemiOpenFiles else features.lineControl.blackSemiOpenFiles

  private def attackersCountFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.kingSafety.whiteAttackersCount else features.kingSafety.blackAttackersCount

  private def enemyKingRingAttackedFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.kingSafety.blackKingRingAttacked else features.kingSafety.whiteKingRingAttacked

  private def enemyKingExposedFilesFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.kingSafety.blackKingExposedFiles else features.kingSafety.whiteKingExposedFiles

  private def enemyKingCastledSideFor(side: String, features: PositionFeatures): String =
    if side == "white" then features.kingSafety.blackCastledSide else features.kingSafety.whiteCastledSide

  private def isCompensationEligiblePhase(semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.phase == "opening" || semantic.phase == "middlegame"

  private def hasConversionPlanPressure(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    topPlansFor(side, semantic).take(2).exists(plan =>
      plan.plan.id == PlanId.Simplification ||
        plan.plan.id == PlanId.Exchange ||
        plan.plan.id == PlanId.QueenTrade
    )

  private def hasCompensationTargetPlanSupport(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    !hasConversionPlanPressure(side, semantic) &&
      topPlansFor(side, semantic).take(4).exists(plan =>
        plan.plan.id == PlanId.WeakPawnAttack ||
          plan.plan.id == PlanId.FileControl ||
          plan.plan.id == PlanId.Blockade ||
          plan.plan.id == PlanId.MinorityAttack
      )

  private def hasCompensationLineAccess(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    pack.pieceRoutes.exists(route =>
      route.ownerSide == side &&
        route.surfaceMode != RouteSurfaceMode.Hidden &&
        isMajorPiece(route.piece) &&
        route.route.lastOption.flatMap(squareFromKey).flatMap(endpoint => lineAccessFacts(side, endpoint, semantic)).nonEmpty
    ) ||
      pack.directionalTargets.exists(target =>
        target.ownerSide == side &&
          isMajorPiece(target.piece) &&
          squareFromKey(target.targetSquare).flatMap(endpoint => lineAccessFacts(side, endpoint, semantic)).nonEmpty
      )

  private def isMajorPiece(piece: String): Boolean =
    piece == "R" || piece == "Q"

  private def isSeventhRankFor(side: String, square: Square): Boolean =
    if side == "white" then square.rank == Rank.Seventh else square.rank == Rank.Second

  private def isOpenFile(board: Board, file: File): Boolean =
    (board.pawns & _root_.chess.Bitboard.file(file)).isEmpty

  private def isSemiOpenFileFor(board: Board, file: File, color: Color): Boolean =
    val mask = _root_.chess.Bitboard.file(file)
    val ours = board.pawns & board.byColor(color) & mask
    val theirs = board.pawns & board.byColor(!color) & mask
    ours.isEmpty && theirs.nonEmpty

  private def fileHasBothColorsPawns(board: Board, file: File): Boolean =
    val mask = _root_.chess.Bitboard.file(file)
    (board.pawns & board.white & mask).nonEmpty && (board.pawns & board.black & mask).nonEmpty

  private def zoneFromSquare(square: Square): Option[String] =
    if square.file.value <= File.C.value then Some("queenside")
    else if square.file.value >= File.F.value then Some("kingside")
    else Some("center")
