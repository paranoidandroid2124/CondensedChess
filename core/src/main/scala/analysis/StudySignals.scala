package chess
package analysis

import AnalyzeDomain.*
import PhaseClassifier.phaseTransition
import chess.opening.Opening

object StudySignals:
  private def bestVsSecondGapOf(p: PlyOutput): Double =
    p.bestVsSecondGap.getOrElse {
      val top = p.evalBeforeDeep.lines.headOption
      val second = p.evalBeforeDeep.lines.drop(1).headOption
      (for t <- top; s <- second yield (t.winPct - s.winPct).abs).getOrElse(100.0)
    }

  private def computeStudySignals(p: PlyOutput, opening: Option[Opening.AtPly]): (Double, List[String]) =
    val gap = bestVsSecondGapOf(p)
    val branchTension = math.max(0.0, 12.0 - gap)
    val deltaScore = math.abs(p.deltaWinPct)
    val (phaseShift, phaseLabel) = phaseTransition(p.conceptDelta, p.winPctBefore)
    val planShift =
      List(
        math.abs(p.conceptDelta.dynamic),
        math.abs(p.conceptDelta.kingSafety),
        math.abs(p.conceptDelta.rookActivity),
        math.abs(p.conceptDelta.pawnStorm),
        math.abs(p.conceptDelta.fortress),
        math.abs(p.conceptDelta.conversionDifficulty)
      ).max
    val theoryFork = opening.exists(op => p.ply.value <= op.ply.value + 5) && gap <= 3.0
    val score =
      deltaScore * 0.6 +
        branchTension * 0.8 +
        phaseShift * 1.4 +
        planShift * 12.0 +
        (if theoryFork then 6.0 else 0.0)
    val tags = scala.collection.mutable.ListBuffer.empty[String]
    if theoryFork then tags += TagName.OpeningTheoryBranch
    phaseLabel.orElse(p.phaseLabel).foreach(tags += _)
    if planShift >= 0.3 then tags += TagName.PlanChange
    tags ++= p.mistakeCategory.toList
    tags ++= p.semanticTags.take(4)
    val canon = tags.toList.distinct
    (score, canon)

  def withStudySignals(timeline: Vector[PlyOutput], opening: Option[Opening.AtPly]): Vector[PlyOutput] =
    timeline.map { p =>
      val (score, tags) = computeStudySignals(p, opening)
      p.copy(studyScore = score, studyTags = tags, phaseLabel = p.phaseLabel)
    }
