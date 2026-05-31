package lila.commentary.analysis.semantic

import chess.Color
import lila.commentary.StrategyPack
import lila.commentary.analysis.{ MoveReviewExchangeAnalyzer, StrategicConceptSemantics, StrategicIdeaSemanticContext }
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

  private lazy val replayUpToSix: Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]] =
    MoveReviewExchangeAnalyzer
      .boundedTopReplayPrefix(semantic.fen, semantic.engineVariations, minPlies = 1, maxPlies = 6)

  lazy val relationWitnesses: List[MoveReviewExchangeAnalyzer.RelationWitness] =
    replayAtLeast(1).toList.flatMap(replay =>
      playedMove.toList.flatMap(move =>
        MoveReviewExchangeAnalyzer.relationWitnesses(replay, move, exactTargets, continuationLines)
      )
    )

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
