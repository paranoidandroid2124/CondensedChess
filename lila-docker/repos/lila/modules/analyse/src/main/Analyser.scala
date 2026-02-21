package lila.analyse

import lila.common.Bus
import lila.tree.Analysis

final class Analyser(
    analysisRepo: AnalysisRepo
)(using Executor)
    extends lila.tree.Analyser:

  def get(game: Game): Fu[Option[Analysis]] =
    analysisRepo.byGame(game)

  def byId(id: Analysis.Id): Fu[Option[Analysis]] = analysisRepo.byId(id)

  def save(analysis: Analysis): Funit =
    analysisRepo.save(analysis) >>
      sendAnalysisProgress(analysis, complete = true)

  def progress(analysis: Analysis): Funit = sendAnalysisProgress(analysis, complete = false)

  private def sendAnalysisProgress(analysis: Analysis, complete: Boolean): Funit =
    analysis.id match
      case Analysis.Id.Study(_, _) =>
        fuccess:
          Bus.pub(lila.tree.StudyAnalysisProgress(analysis, complete))
      case Analysis.Id.Game(_) =>
        funit
