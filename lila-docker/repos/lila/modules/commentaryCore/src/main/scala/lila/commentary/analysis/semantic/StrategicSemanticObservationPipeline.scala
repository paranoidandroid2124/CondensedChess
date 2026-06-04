package lila.commentary.analysis.semantic

import chess.Color
import lila.commentary.StrategyPack
import lila.commentary.analysis.{ MoveReviewExchangeAnalyzer, StrategicConceptSemantics, StrategicIdeaSemanticContext }
import lila.commentary.model.{ ProbeContractValidator, ProbeResult }
import lila.commentary.model.strategic.PositionalTag

private[commentary] trait StrategicSemanticObservationProducer:
  def collect(
      context: StrategicSemanticObservationContext
  ): List[StrategicSemanticObservation]

private[commentary] final case class StrategicSemanticObservationContext(
    pack: StrategyPack,
    semantic: StrategicIdeaSemanticContext
):
  import StrategicSemanticObservationProducerSupport.*

  lazy val playedMove: Option[String] =
    normalizedPlayedMove(semantic)

  lazy val exactTargets: List[String] =
    exactTargetSquares(semantic)

  private lazy val drawResourceReplay: Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]] =
    MoveReviewExchangeAnalyzer
      .boundedTopReplayPrefix(
        semantic.fen,
        semantic.engineVariations,
        minPlies = 1,
        maxPlies = DrawResourceRelationReplayMaxPlies
      )

  private lazy val replayUpToSix: Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]] =
    drawResourceReplay.map(_.take(StandardRelationReplayMaxPlies))

  lazy val relationWitnesses: List[MoveReviewExchangeAnalyzer.RelationWitness] =
    playedMove.toList.flatMap { move =>
      val engineScoreCp = semantic.engineVariations.headOption.map(_.scoreCp)
      val engineMate = semantic.engineVariations.headOption.flatMap(_.mate)
      val standardWitnesses =
        replayAtLeast(1).toList.flatMap(replay =>
          MoveReviewExchangeAnalyzer
            .relationWitnesses(
              replay = replay,
              playedMove = move,
              explicitTargets = exactTargets,
              continuationLines = continuationLines,
              engineScoreCp = engineScoreCp,
              engineMate = engineMate,
              includeDrawResources = false
            )
        )
      val drawResourceWitnessesFromTopPv =
        drawResourceReplay.toList.flatMap(replay =>
          drawResourceWitnessesFromReplay(
            replay = replay,
            move = move,
            engineScoreCp = engineScoreCp,
            engineMate = engineMate
          )
        )
      val drawResourceWitnessesFromProbes =
        semantic.probeResults
          .filter(result => validatedRootProbeForPlayedMove(result, move))
          .flatMap(result =>
            probeReplyLines(result).flatMap(replyLine =>
              MoveReviewExchangeAnalyzer
                .boundedReplayPrefix(
                  semantic.fen,
                  move :: replyLine,
                  minPlies = 1,
                  maxPlies = DrawResourceRelationReplayMaxPlies
                )
                .toList
                .flatMap(replay =>
                  drawResourceWitnessesFromReplay(
                    replay = replay,
                    move = move,
                    engineScoreCp = Some(result.evalCp),
                    engineMate = result.mate
                  )
                )
            )
          )
      (standardWitnesses ++ drawResourceWitnessesFromTopPv ++ drawResourceWitnessesFromProbes).distinct
    }

  private def drawResourceWitnessesFromReplay(
      replay: List[MoveReviewExchangeAnalyzer.BoundedReplayStep],
      move: String,
      engineScoreCp: Option[Int],
      engineMate: Option[Int]
  ): List[MoveReviewExchangeAnalyzer.RelationWitness] =
    List(
      MoveReviewExchangeAnalyzer.stalemateTrapWitness(
        replay = replay,
        playedMove = move,
        engineScoreCp = engineScoreCp,
        engineMate = engineMate
      ),
      MoveReviewExchangeAnalyzer.perpetualCheckWitness(
        replay = replay,
        playedMove = move,
        engineScoreCp = engineScoreCp,
        engineMate = engineMate
      )
    ).flatten

  private def validatedRootProbeForPlayedMove(
      result: ProbeResult,
      move: String
  ): Boolean =
    ProbeContractValidator.validate(result).isValid &&
      result.fen.map(_.trim).filter(_.nonEmpty).contains(semantic.fen.trim) &&
      probeMoveMatchesPlayedMove(result, move)

  private def probeMoveMatchesPlayedMove(result: ProbeResult, move: String): Boolean =
    val normalizedBoundMoves =
      (result.probedMove.toList ++ result.candidateMove.toList)
        .map(lila.commentary.analysis.NarrativeUtils.normalizeUciMove)
        .filter(_.nonEmpty)
    normalizedBoundMoves.nonEmpty &&
      normalizedBoundMoves.forall(_ == move) &&
      normalizedBoundMoves.forall(MoveReviewExchangeAnalyzer.isUciMove)

  private def probeReplyLines(result: ProbeResult): List[List[String]] =
    (result.bestReplyPv :: result.replyPvs.toList.flatten)
      .filter(_.nonEmpty)
      .flatMap(normalizedStrictUciLine)
      .distinct

  private lazy val continuationLines: List[List[String]] =
    semantic.engineVariations.map(MoveReviewExchangeAnalyzer.normalizedLineMoves).filter(_.nonEmpty)

  def relationWitnessesFor(kinds: Set[String]): List[MoveReviewExchangeAnalyzer.RelationWitness] =
    relationWitnesses.filter(witness => kinds.contains(witness.kind))

  def replayAtLeast(minPlies: Int): Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]] =
    replayUpToSix.filter(_.size >= minPlies)

private[commentary] object StrategicSemanticObservationPipeline:

  private val producers: List[StrategicSemanticObservationProducer] =
    List(
      MinorityAttackObservationProducer,
      RelationObservationProducer
    )

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicSemanticObservation] =
    val context = StrategicSemanticObservationContext(pack, semantic)
    producers.flatMap(_.collect(context)).distinct

private[commentary] object MinorityAttackObservationProducer extends StrategicSemanticObservationProducer:

  import StrategicSemanticObservationProducerSupport.*

  def collect(
      context: StrategicSemanticObservationContext
  ): List[StrategicSemanticObservation] =
    val side = sideFromString(context.pack.sideToMove)
    StrategicConceptSemantics
      .minorityAttackObservations(context.semantic)
      .filter(observation =>
        observation.side == side &&
          observation.status == StrategicConceptSemantics.ConceptStatus.SemanticReady
      )
      .map { observation =>
        val focusSquares =
          if observation.targets.nonEmpty then observation.targets.take(3)
          else flankTargetSquares(observation.wing, context.exactTargets).take(3)
        StrategicSemanticObservation.minorityAttackFromConcept(
          ownerSide = context.pack.sideToMove,
          focusSquares = focusSquares,
          observation = observation
        )
      }

private[commentary] object RelationObservationProducer extends StrategicSemanticObservationProducer:

  private[semantic] val RelationKinds: Set[String] =
    RelationObservationCatalog.ImplementedKinds

  def collect(
      context: StrategicSemanticObservationContext
  ): List[StrategicSemanticObservation] =
    val relationWitnesses =
      context.relationWitnessesFor(RelationKinds)
    relationWitnesses.flatMap(StrategicSemanticObservation.relationWitness(context.pack.sideToMove, _))

private object StrategicSemanticObservationProducerSupport:

  val StandardRelationReplayMaxPlies = 6
  val DrawResourceRelationReplayMaxPlies = 12

  def exactTargetSquares(semantic: StrategicIdeaSemanticContext): List[String] =
    val enemyWeakSquares =
      semantic.positionalFeatures.collect {
        case PositionalTag.WeakSquare(square, owner)
            if !matchesSide(owner, semantic.sideToMove) =>
          square.key
      }
    val structuralTargetSquares =
      semantic.structuralWeaknesses
        .filter(weakness => !matchesSide(weakness.color, semantic.sideToMove))
        .flatMap(_.squares.map(_.key))
    (enemyWeakSquares ++ structuralTargetSquares).distinct

  def normalizedPlayedMove(semantic: StrategicIdeaSemanticContext): Option[String] =
    semantic.playedMove
      .map(lila.commentary.analysis.NarrativeUtils.normalizeUciMove)
      .filter(MoveReviewExchangeAnalyzer.isUciMove)

  def normalizedStrictUciLine(moves: List[String]): Option[List[String]] =
    val normalized =
      moves.map(move => Option(move).fold("")(lila.commentary.analysis.NarrativeUtils.normalizeUciMove))
    Option.when(normalized.nonEmpty && normalized.forall(MoveReviewExchangeAnalyzer.isUciMove))(normalized)

  def flankTargetSquares(flank: String, squares: List[String]): List[String] =
    val files =
      normalize(flank) match
        case "queenside" => Set('a', 'b', 'c', 'd')
        case "kingside"  => Set('e', 'f', 'g', 'h')
        case _           => Set.empty[Char]
    if files.isEmpty then Nil
    else squares.filter(square => square.headOption.exists(files.contains))

  def matchesSide(color: Color, side: String): Boolean =
    if color.white then normalize(side) == "white" else normalize(side) == "black"

  def sideFromString(side: String): Color =
    if normalize(side) == "black" then Color.Black else Color.White

  def normalize(raw: String): String =
    raw.trim.toLowerCase
