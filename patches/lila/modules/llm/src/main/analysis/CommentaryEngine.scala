package lila.llm.analysis

import _root_.chess.*
import _root_.chess.format.Uci
import lila.llm.model.*

/**
 * The central hub for chess position analysis. 
 * Orchestrates motifs, plans, and position nature characterization.
 */
case class PositionAssessment(
    pos: Position,
    motifs: List[Motif],
    plans: PlanScoringResult,
    nature: PositionNature,
    phase: GamePhase
)

enum GamePhase:
  case Opening, Middlegame, Endgame

object CommentaryEngine:

  def assess(
      fen: String,
      pv: List[String],
      opening: Option[String] = None,
      phase: Option[String] = None
  ): Option[PositionAssessment] =
    _root_.chess.format.Fen.read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(fen)).map { initialPos =>
      val (lastPos, motifs) = pv.foldLeft((initialPos, List.empty[Motif])) {
        case ((p, acc), uciStr) =>
          Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(p.move(_).toOption) match
            case Some(mv) =>
              val moveMotifs = MotifTokenizer.tokenize(List(Uci(uciStr).get), p)
              (mv.after, acc ++ moveMotifs)
            case None => (p, acc)
      }

      val nature = PositionCharacterizer.characterize(lastPos)
      val currentPhase = determinePhase(lastPos)
      
      val ctx = IntegratedContext(
        eval = 0.0,
        phase = phase.getOrElse(currentPhase.toString.toLowerCase),
        openingName = opening
      )
      
      val planScoring = PlanMatcher.matchPlans(motifs, ctx, lastPos.color)

      PositionAssessment(
        pos = lastPos,
        motifs = motifs,
        plans = planScoring,
        nature = nature,
        phase = currentPhase
      )
    }

  private def determinePhase(pos: Position): GamePhase =
    val pieces = pos.board.occupied.count
    if (pieces > 28) GamePhase.Opening
    else if (pieces < 12) GamePhase.Endgame
    else GamePhase.Middlegame

  /**
   * Formats the assessment into a sparse text representation for the LLM prompt.
   */
  def formatSparse(assessment: PositionAssessment): String =
    val p = assessment.plans
    val n = assessment.nature
    
    val planText = p.topPlans.map { m =>
      s"- Plan: ${m.plan.name} (score: ${"%.2f".format(m.score)})"
    }.mkString("\n")

    s"""NATURE: ${n.natureType} (tension=${"%.2f".format(n.tension)}, stability=${"%.2f".format(n.stability)})
       |${n.description}
       |
       |TOP STRATEGIES:
       |$planText
       |
       |OBSERVED MOTIFS:
       |${assessment.motifs.take(8).map(m => s"- ${m.getClass.getSimpleName}: ${m.move.getOrElse("?")}").mkString("\n")}
       |""".stripMargin
