package lila.commentary.analysis

import chess.format.Fen
import chess.variant.Standard
import lila.commentary.PgnAnalysisHelper
import lila.commentary.analysis.strategic.{ BreakClampMaterializer, ProphylaxisAnalyzerImpl }
import lila.commentary.model.FactScope
import lila.commentary.model.strategic.VariationLine
import lila.commentary.tools.claim.SourceWitnessCatalog
import munit.FunSuite

class BreakClampPipelineHandoffTest extends FunSuite:

  private final case class CandidateLine(
      id: String,
      moves: List[String],
      scoreCp: Int
  )

  private final case class HandoffSnapshot(
      id: String,
      ply: Int,
      played: String,
      routeEvidence: List[String],
      directPlans: List[String],
      prophylaxisPlans: List[String],
      extendedPlans: List[String],
      semanticPlans: List[String],
      plannerPlans: List[String],
      truthMode: String,
      deltaClass: Option[String],
      deltaOwner: Option[String],
      deltaFamily: Option[String],
      deltaScope: Option[String],
      deltaFallback: Option[String],
      deltaSameBranch: Option[String],
      deltaPersistence: Option[String],
      deltaSuppression: List[String],
      deltaRisks: List[String],
      deltaGate: Option[String],
      mainClaimSource: Option[String]
  ):
    override def toString: String =
      List(
        s"id=$id",
        s"ply=$ply",
        s"played=$played",
        s"routeEvidence=${routeEvidence.mkString(",")}",
        s"directPlans=${directPlans.mkString(",")}",
        s"prophylaxisPlans=${prophylaxisPlans.mkString(",")}",
        s"extendedPlans=${extendedPlans.mkString(",")}",
        s"semanticPlans=${semanticPlans.mkString(",")}",
        s"plannerPlans=${plannerPlans.mkString(",")}",
        s"truthMode=$truthMode",
        s"deltaClass=${deltaClass.getOrElse("-")}",
        s"deltaOwner=${deltaOwner.getOrElse("-")}",
        s"deltaFamily=${deltaFamily.getOrElse("-")}",
        s"deltaScope=${deltaScope.getOrElse("-")}",
        s"deltaFallback=${deltaFallback.getOrElse("-")}",
        s"deltaSameBranch=${deltaSameBranch.getOrElse("-")}",
        s"deltaPersistence=${deltaPersistence.getOrElse("-")}",
        s"deltaSuppression=${deltaSuppression.mkString("|")}",
        s"deltaRisks=${deltaRisks.mkString("|")}",
        s"deltaGate=${deltaGate.getOrElse("-")}",
        s"mainClaimSource=${mainClaimSource.getOrElse("-")}"
      ).mkString(" ")

  private val candidateLines =
    List(
      CandidateLine(
        id = "source-lokvenc-czerniak-1952-b6-b5-break-prevention",
        moves = List("e2b5", "a6b4", "c2d1", "c8d7", "b5f1", "b6b5", "a2a3", "b4a6"),
        scoreCp = 37
      ),
      CandidateLine(
        id = "source-maderna-palermo-1955-a6-a5-break-prevention",
        moves = List("a4a5", "b6a8", "d2c4", "d7f8", "e4e5", "d6e5", "f4e5", "b7b5"),
        scoreCp = 82
      ),
      CandidateLine(
        id = "source-polugaevsky-giorgadze-1956-c5-c4-break-prevention",
        moves = List("d2c4", "b8d7", "f2f4", "a8b8", "c1e3", "b6b5", "a4b5", "a6b5"),
        scoreCp = 64
      )
    )

  test("preserves exact route-clamp plans across the live semantic handoff") {
    val snapshots = candidateLines.map(snapshot)

    assert(snapshots.forall(_.routeEvidence.nonEmpty), clues(snapshots.mkString("\n")))
    assert(snapshots.forall(_.directPlans.nonEmpty), clues(snapshots.mkString("\n")))
    assert(snapshots.forall(_.prophylaxisPlans.nonEmpty), clues(snapshots.mkString("\n")))
    assert(snapshots.forall(snapshot => snapshot.extendedPlans == snapshot.prophylaxisPlans), clues(snapshots.mkString("\n")))
    assert(snapshots.forall(snapshot => snapshot.semanticPlans == snapshot.extendedPlans), clues(snapshots.mkString("\n")))
    assert(snapshots.forall(snapshot => snapshot.plannerPlans == snapshot.semanticPlans), clues(snapshots.mkString("\n")))
    assert(snapshots.forall(_.truthMode == "Strategic"), clues(snapshots.mkString("\n")))
    assert(snapshots.forall(_.deltaOwner.contains("counterplay_axis_suppression")), clues(snapshots.mkString("\n")))
    assert(snapshots.forall(_.mainClaimSource.contains("counterplay_axis_suppression")), clues(snapshots.mkString("\n")))
  }

  private def snapshot(candidate: CandidateLine): HandoffSnapshot =
    val source =
      SourceWitnessCatalog.all.find(_.id == candidate.id).getOrElse(fail(s"missing source: ${candidate.id}"))
    val plyData =
      PgnAnalysisHelper
        .extractPlyDataStrict(source.pgn)
        .fold(error => fail(s"strict PGN failed for ${candidate.id}: $error"), identity)
        .find(_.ply == source.candidatePlyRange.start)
        .getOrElse(fail(s"missing ply ${source.candidatePlyRange.start} for ${candidate.id}"))
    val position =
      Fen.read(Standard, Fen.Full(plyData.fen)).getOrElse(fail(s"bad FEN: ${plyData.fen}"))
    val line = VariationLine(
      moves = candidate.moves,
      scoreCp = candidate.scoreCp,
      depth = 16
    )
    val routeEvidence =
      BreakClampMaterializer
        .routeEvidence(
          fen = plyData.fen,
          board = position.board,
          color = plyData.color,
          mainLine = line
        )
        .map(evidence => s"${evidence.routeId}:${evidence.transformRisk}:${evidence.transformRoutes.mkString("|")}")
    val directPlans =
      BreakClampMaterializer
        .materialize(
          fen = plyData.fen,
          board = position.board,
          color = plyData.color,
          mainLine = line
        )
        .flatMap(_.breakNeutralized)
    val prophylaxisPlans =
      ProphylaxisAnalyzerImpl()
        .analyze(
          fen = plyData.fen,
          board = position.board,
          color = plyData.color,
          mainLine = line,
          threatLine = None
        )
        .flatMap(_.breakNeutralized)

    val data =
      CommentaryEngine
        .assessExtended(
          fen = plyData.fen,
          variations = List(line),
          playedMove = Some(plyData.playedUci),
          phase = Some("middlegame"),
          ply = plyData.ply,
          prevMove = Some(plyData.playedUci)
        )
        .getOrElse(fail(s"assessExtended missing for ${candidate.id}"))
    val ctx = NarrativeContextBuilder.build(data, data.toContext, None)
    val pack = StrategyPackBuilder.build(data, ctx)
    val inputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
    val surface = StrategyPackSurface.from(pack)
    val delta = PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx, surface, truthContract = None)
    val mainClaim = inputs.mainBundle.flatMap(_.mainClaim)

    HandoffSnapshot(
      id = candidate.id,
      ply = plyData.ply,
      played = plyData.playedUci,
      routeEvidence = routeEvidence,
      directPlans = directPlans,
      prophylaxisPlans = prophylaxisPlans,
      extendedPlans = data.preventedPlans.flatMap(_.breakNeutralized),
      semanticPlans = ctx.semantic.toList
        .flatMap(_.preventedPlans)
        .filter(_.sourceScope == FactScope.Now)
        .flatMap(_.breakNeutralized),
      plannerPlans = inputs.preventedPlansNow.flatMap(_.breakNeutralized),
      truthMode = PlayerFacingTruthModePolicy.classify(ctx, pack, truthContract = None).toString,
      deltaClass = delta.map(_.deltaClass.toString),
      deltaOwner = delta.map(_.packet.ownerSource),
      deltaFamily = delta.map(_.packet.ownerFamily),
      deltaScope = delta.map(_.packet.scope.toString),
      deltaFallback = delta.map(_.packet.fallbackMode.toString),
      deltaSameBranch = delta.map(_.packet.sameBranchState.toString),
      deltaPersistence = delta.map(_.packet.persistence.toString),
      deltaSuppression = delta.toList.flatMap(_.packet.suppressionReasons),
      deltaRisks = delta.toList.flatMap(_.packet.releaseRisks),
      deltaGate = delta.map(d =>
        List(
          s"cert=${d.packet.claimGate.certificateStatus}",
          s"quant=${d.packet.claimGate.quantifier}",
          s"attr=${d.packet.claimGate.attributionGrade}",
          s"stab=${d.packet.claimGate.stabilityGrade}",
          s"prov=${d.packet.claimGate.provenanceClass}",
          s"taint=${d.packet.claimGate.taintFlags.mkString("|")}"
        ).mkString(",")
      ),
      mainClaimSource = mainClaim.flatMap(_.packet).map(_.ownerSource).orElse(mainClaim.map(_.sourceKind))
    )
