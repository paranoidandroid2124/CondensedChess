package lila.study

final private class StudyMaker(
    chapterMaker: ChapterMaker
)(using Executor):

  def apply(data: StudyMaker.ImportGame, user: User, @scala.annotation.unused withRatings: Boolean): Fu[Study.WithChapter] =
    createFromScratch(data, user).map { sc =>
      // apply specified From if any
      sc.copy(study = sc.study.copy(from = data.from | sc.study.from))
    }

  private def createFromScratch(data: StudyMaker.ImportGame, user: User): Fu[Study.WithChapter] =
    val study = Study.make(user, Study.From.Scratch, data.id, data.name, data.settings)
    chapterMaker
      .fromFenOrPgnOrBlank(
        study,
        ChapterMaker.Data(
          game = none,
          name = StudyChapterName("Chapter 1"),
          variant = data.form.variant,
          fen = data.form.fen,
          pgn = data.form.pgnStr,
          orientation = data.form.orientation | ChapterMaker.Orientation.Auto,
          mode = ChapterMaker.Mode.Normal,
          initial = true
        ),
        order = 1,
        userId = user.id
      )
      .map { chapter =>
        Study.WithChapter(study.withChapter(chapter), chapter)
      }

object StudyMaker:

  case class ImportGame(
      form: StudyForm.importGame.Data = StudyForm.importGame.Data(),
      id: Option[StudyId] = None,
      name: Option[StudyName] = None,
      settings: Option[Settings] = None,
      from: Option[Study.From] = None
  )
