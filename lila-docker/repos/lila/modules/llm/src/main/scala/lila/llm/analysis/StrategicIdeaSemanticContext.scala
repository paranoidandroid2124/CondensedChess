package lila.llm.analysis

import _root_.chess.Board
import _root_.chess.format.Fen

import lila.llm.analysis.L3.{ PawnPlayAnalysis, PositionClassification, ThreatAnalysis }
import lila.llm.model.{ CompensationInfo, ExtendedAnalysisData, Motif, NarrativeContext, PawnPlayTable, PlanMatch, StrategicPlanExperiment }
import lila.llm.model.structure.StructureProfile
import lila.llm.model.strategic.{ EndgameFeature, PieceActivity, PositionalTag, PreventedPlan, WeakComplex }

private[llm] final case class StrategicIdeaSemanticContext(
    sideToMove: String,
    fen: String = "",
    playedMove: Option[String] = None,
    board: Option[Board] = None,
    pieceActivity: List[PieceActivity] = Nil,
    positionalFeatures: List[PositionalTag] = Nil,
    structuralWeaknesses: List[WeakComplex] = Nil,
    preventedPlans: List[PreventedPlan] = Nil,
    endgameFeatures: Option[EndgameFeature] = None,
    pawnPlay: Option[PawnPlayTable] = None,
    classification: Option[PositionClassification] = None,
    pawnAnalysis: Option[PawnPlayAnalysis] = None,
    opponentPawnAnalysis: Option[PawnPlayAnalysis] = None,
    threatsToUs: Option[ThreatAnalysis] = None,
    threatsToThem: Option[ThreatAnalysis] = None,
    structureProfile: Option[StructureProfile] = None,
    planAlignmentReasonCodes: List[String] = Nil,
    plans: List[PlanMatch] = Nil,
    strategicPlanExperiments: List[StrategicPlanExperiment] = Nil,
    motifs: List[Motif] = Nil,
    phase: String = "middlegame",
    positionFeatures: Option[PositionFeatures] = None,
    strategicState: Option[StrategicStateFeatures] = None,
    currentCompensation: Option[CompensationInfo] = None,
    effectiveCompensation: Option[CompensationInfo] = None,
    afterCompensation: Option[CompensationInfo] = None
)

private[llm] object StrategicIdeaSemanticContext:

  def empty(sideToMove: String): StrategicIdeaSemanticContext =
    StrategicIdeaSemanticContext(sideToMove = sideToMove)

  def from(
      data: ExtendedAnalysisData,
      ctx: NarrativeContext,
      boardOpt: Option[Board]
  ): StrategicIdeaSemanticContext =
    val integrated = data.toContext
    val board =
      boardOpt.orElse(Fen.read(_root_.chess.variant.Standard, Fen.Full(data.fen)).map(_.board))

    StrategicIdeaSemanticContext(
      sideToMove = if data.isWhiteToMove then "white" else "black",
      fen = data.fen,
      playedMove = data.prevMove,
      board = board,
      pieceActivity = data.pieceActivity,
      positionalFeatures = data.positionalFeatures,
      structuralWeaknesses = data.structuralWeaknesses,
      preventedPlans = data.preventedPlans,
      endgameFeatures = data.endgameFeatures,
      pawnPlay = Some(ctx.pawnPlay),
      classification = integrated.classification,
      pawnAnalysis = integrated.pawnAnalysis,
      opponentPawnAnalysis = integrated.opponentPawnAnalysis,
      threatsToUs = integrated.threatsToUs,
      threatsToThem = integrated.threatsToThem,
      structureProfile = integrated.structureProfile,
      planAlignmentReasonCodes = integrated.planAlignment.toList.flatMap(_.reasonCodes).distinct,
      plans = data.plans,
      strategicPlanExperiments = ctx.strategicPlanExperiments,
      motifs = data.motifs,
      phase = data.phase,
      positionFeatures = integrated.features.orElse(PositionAnalyzer.extractFeatures(data.fen, data.ply.max(1))),
      strategicState = PositionAnalyzer.extractStrategicState(data.fen),
      currentCompensation = ctx.semantic.flatMap(_.compensation),
      effectiveCompensation = CompensationInterpretation.effectiveSemanticDecision(ctx).map(_.compensation),
      afterCompensation = ctx.semantic.flatMap(_.afterCompensation)
    )
