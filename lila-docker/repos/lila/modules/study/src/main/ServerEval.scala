package lila.study

import chess.format.pgn.Glyphs
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import play.api.libs.json.*
import scala.annotation.unused

import lila.db.dsl.bsonWriteOpt
import lila.tree.Node.Comment
import lila.tree.{ Advice, Analysis, Branch, Info, Node, Root }

// Chesstory: Removed socket and relay dependencies - simplified for analysis-only system
object ServerEval:

  final class Requester(
      chapterRepo: ChapterRepo,
      @unused userApi: lila.core.user.UserApi
  )(using Executor):

    private val onceEvery = scalalib.cache.OnceEvery[StudyChapterId](5.minutes)

    def apply(study: Study, chapter: Chapter, userId: UserId, official: Boolean = false): Funit =
      chapter.serverEval
        .forall: eval =>
          !eval.done && onceEvery(chapter.id)
        .so:
          for
            isOfficial <- fuccess(official) >>|
              fuccess(userId.is(UserId.lichess))
            _ <- chapterRepo.startServerEval(chapter)
          yield lila.common.Bus.pub(
            lila.core.fishnet.Bus.StudyChapterRequest(
              studyId = study.id,
              chapterId = chapter.id,
              initialFen = chapter.root.fen.some,
              variant = chapter.setup.variant,
              moves = chess.format
                .UciDump(
                  moves = chapter.root.mainline.map(_.move.san),
                  initialFen = chapter.root.fen.some,
                  variant = chapter.setup.variant
                )
                .toOption
                .map(_.flatMap(chess.format.Uci.apply)) | List.empty,
              userId = userId,
              official = isOfficial
            )
          )

  // Chesstory: Simplified Merger - removed socket dependency
  final class Merger(
      sequencer: StudySequencer,
      chapterRepo: ChapterRepo,
      @unused analysisJson: lila.tree.AnalysisJson
  )(using Executor):

    def apply(analysis: Analysis, complete: Boolean): Funit = analysis.id match
      case Analysis.Id.Study(studyId, chapterId) =>
        sequencer.sequenceStudyWithChapter(studyId, chapterId):
          case Study.WithChapter(_, chapter) =>
            for
              _ <- complete.so(chapterRepo.completeServerEval(chapter))
              _ <- chapter.root.mainline
                .zip(analysis.infoAdvices)
                .foldM(UciPath.root):
                  case (path, (node, (info, advOpt))) =>
                    saveAnalysis(chapter, node, path, info, advOpt)
                .andDo(sendProgress(studyId, chapterId, analysis, complete))
                .logFailure(logger)
            yield ()
      case _ => funit

    private def saveAnalysis(
        chapter: Chapter,
        node: Branch,
        path: UciPath,
        info: Info,
        advOpt: Option[Advice]
    ): Future[UciPath] =

      val nextPath = path + node.id

      def saveAnalysisLine() =
        chapter.root
          .nodeAt(path)
          .flatMap: parent =>
            analysisLine(parent, chapter.setup.variant, info).map: subTree =>
              parent.addChild(subTree) -> subTree
          .so: (_, subTree) =>
            chapterRepo.addSubTree(chapter, subTree, path, none)

      def saveInfoAdvice() =
        import BSONHandlers.given
        import lila.db.dsl.given
        import lila.study.Node.BsonFields as F
        ((info.eval.score.isDefined && node.eval.isEmpty) || (advOpt.isDefined && !node.comments.hasLichessComment))
          .so(
            chapterRepo
              .setNodeValues(
                chapter,
                nextPath,
                List(
                  F.score -> info.eval.score
                    .ifTrue:
                      node.eval.isEmpty ||
                      advOpt.isDefined && node.comments.findBy(Comment.Author.Lichess).isEmpty
                    .flatMap(bsonWriteOpt),
                  F.comments -> advOpt
                    .map: adv =>
                      node.comments + Comment(
                        Comment.Id.make,
                        adv.makeComment(false),
                        Comment.Author.Lichess
                      )
                    .flatMap(bsonWriteOpt),
                  F.glyphs -> advOpt
                    .map(adv => node.glyphs.merge(Glyphs.fromList(List(adv.judgment.glyph))))
                    .flatMap(bsonWriteOpt)
                )
              )
          )

      saveAnalysisLine()
        >> saveInfoAdvice().inject(nextPath)

    end saveAnalysis

    private def analysisLine(root: Node, variant: chess.variant.Variant, info: Info): Option[Branch] =
      val setup = chess.Position.AndFullMoveNumber(variant, root.fen)
      val (result, error) = setup.position
        .foldRight(info.variation.take(20), setup.ply)(
          none[Branch],
          (step, acc) =>
            inline def branch = makeBranch(step.move, step.ply)
            acc.fold(branch)(acc => branch.addChild(acc)).some
        )
      error.foreach(e => logger.info(e.value))
      result

    private def makeBranch(m: chess.MoveOrDrop, ply: chess.Ply): Branch =
      Branch(
        id = UciCharPair(m.toUci),
        ply = ply,
        move = Uci.WithSan(m.toUci, m.toSanStr),
        fen = Fen.write(m.after, ply.fullMoveNumber),
        check = m.after.position.check,
        crazyData = m.after.position.crazyData,
        clock = none,
        forceVariation = false
      )

    // Simplified: no socket push, just log progress
    private def sendProgress(
        studyId: StudyId,
        chapterId: StudyChapterId,
        analysis: Analysis,
        complete: Boolean
    ): Funit =
      chapterRepo
        .byId(chapterId)
        .flatMapz: chapter =>
          logger.info(s"Analysis progress: study=$studyId chapter=$chapterId complete=$complete")
          funit

  case class Progress(chapterId: StudyChapterId, tree: Root, analysis: JsObject)
