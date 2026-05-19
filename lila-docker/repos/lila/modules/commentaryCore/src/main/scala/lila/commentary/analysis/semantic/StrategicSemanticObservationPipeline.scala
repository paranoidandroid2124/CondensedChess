package lila.commentary.analysis.semantic

import chess.Color
import lila.commentary.StrategyPack
import lila.commentary.analysis.{ StrategicConceptSemantics, StrategicIdeaSemanticContext }

private[commentary] trait StrategicSemanticObservationProducer:
  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicSemanticObservation]

private[commentary] object StrategicSemanticObservationPipeline:

  private val producers: List[StrategicSemanticObservationProducer] =
    List(MinorityAttackObservationProducer)

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicSemanticObservation] =
    producers.flatMap(_.collect(pack, semantic)).distinct

private[commentary] object MinorityAttackObservationProducer extends StrategicSemanticObservationProducer:

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicSemanticObservation] =
    val side = sideFromString(pack.sideToMove)
    val exactTargets = exactTargetSquares(semantic)
    StrategicConceptSemantics
      .minorityAttackObservations(semantic)
      .filter(observation =>
        observation.side == side &&
          observation.status == StrategicConceptSemantics.ConceptStatus.SemanticReady
      )
      .map { observation =>
        val focusSquares =
          if observation.targets.nonEmpty then observation.targets.take(3)
          else flankTargetSquares(observation.wing, exactTargets).take(3)
        StrategicSemanticObservation.minorityAttackFromConcept(
          ownerSide = pack.sideToMove,
          focusSquares = focusSquares,
          observation = observation
        )
      }

  private def exactTargetSquares(semantic: StrategicIdeaSemanticContext): List[String] =
    val enemyWeakSquares =
      semantic.positionalFeatures.collect {
        case lila.commentary.model.strategic.PositionalTag.WeakSquare(square, owner)
            if !matchesSide(owner, semantic.sideToMove) =>
          square.key
      }
    val structuralTargetSquares =
      semantic.structuralWeaknesses
        .filter(weakness => !matchesSide(weakness.color, semantic.sideToMove))
        .flatMap(_.squares.map(_.key))
    (enemyWeakSquares ++ structuralTargetSquares).distinct

  private def flankTargetSquares(flank: String, squares: List[String]): List[String] =
    val files =
      normalize(flank) match
        case "queenside" => Set('a', 'b', 'c', 'd')
        case "kingside"  => Set('e', 'f', 'g', 'h')
        case _           => Set.empty[Char]
    if files.isEmpty then Nil
    else squares.filter(square => square.headOption.exists(files.contains))

  private def matchesSide(color: Color, side: String): Boolean =
    if color.white then normalize(side) == "white" else normalize(side) == "black"

  private def sideFromString(side: String): Color =
    if normalize(side) == "black" then Color.Black else Color.White

  private def normalize(raw: String): String =
    raw.trim.toLowerCase
