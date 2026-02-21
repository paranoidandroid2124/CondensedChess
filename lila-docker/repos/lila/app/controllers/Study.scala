package controllers

import chess.format.pgn.PgnStr
import chess.ErrorStr
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.opening.OpeningDb
import play.api.data.Form
import play.api.data.Forms.*
import play.api.libs.json.*
import play.api.mvc.*
import lila.app.{ *, given }
import lila.analyse.CondensedJsonView
import lila.core.game.Pov
import lila.study.StudyForm
import lila.llm.model.strategic.VariationLine
import lila.tree.Branch

// Chesstory: Restored minimal Study pages (list + chapter view) without sockets.
final class Study(
    env: Env
) extends LilaController(env):

  private val importPgnForm = Form(single("pgn" -> text))

  private case class BookmakerSyncRequest(
      commentPath: String,
      originPath: String,
      commentary: String,
      variations: List[VariationLine],
      maxLines: Option[Int] = None,
      maxPlies: Option[Int] = None
  )

  private object BookmakerSyncRequest:
    given Reads[BookmakerSyncRequest] = Json.reads[BookmakerSyncRequest]

  private val defaultPvLines = 5
  private val defaultPvPlies = 16
  private val maxPvLines = 8
  private val maxPvPlies = 40
  private val aiAuthor = "Chesstory AI"

  private def branchFromUci(
      variant: chess.variant.Variant,
      fen: Fen.Full,
      uciStr: String
  ): Either[ErrorStr, Branch] =
    Uci(uciStr) match
      case Some(m: Uci.Move) =>
        chess
          .Game(variant.some, fen.some)(m.orig, m.dest, m.promotion)
          .map: (game, move) =>
            val uci = Uci(move)
            val movable = game.position.playable(false)
            val newFen = chess.format.Fen.write(game)
            Branch(
              id = UciCharPair(uci),
              ply = game.ply,
              move = Uci.WithSan(uci, move.toSanStr),
              fen = newFen,
              check = game.position.check,
              dests = Some(movable.so(game.position.destinations)),
              opening = (game.ply <= 30 && chess.variant.Variant.list.openingSensibleVariants(variant)).so(
                OpeningDb.findByFullFen(newFen)
              ),
              drops = if movable then game.position.drops else Some(Nil),
              crazyData = game.position.crazyData
            )
      case Some(d: Uci.Drop) =>
        chess
          .Game(variant.some, fen.some)
          .drop(d.role, d.square)
          .map: (game, drop) =>
            val uci = Uci(drop)
            val movable = !game.position.end
            val newFen = chess.format.Fen.write(game)
            Branch(
              id = UciCharPair(uci),
              ply = game.ply,
              move = Uci.WithSan(uci, drop.toSanStr),
              fen = newFen,
              check = game.position.check,
              dests = Some(movable.so(game.position.destinations)),
              opening = OpeningDb.findByFullFen(newFen),
              drops = if movable then game.position.drops else Some(Nil),
              crazyData = game.position.crazyData
            )
      case _ => Left(ErrorStr(s"Bad UCI: $uciStr"))

  private def insertPvLine(
      studyId: StudyId,
      chapter: lila.study.Chapter,
      startPath: UciPath,
      startFen: Fen.Full,
      moves: List[String],
      opts: lila.study.MoveOpts
  )(using who: lila.study.Who): Funit =
    if moves.isEmpty then funit
    else
      val variant = chapter.setup.variant
      moves.foldLeft(fuccess((startPath, startFen))) { (acc, uciStr) =>
        acc.flatMap { (path, fen) =>
          branchFromUci(variant, fen, uciStr) match
            case Left(_) => fuccess((path, fen))
            case Right(branch) =>
              env.study.api
                .addNode(
                  lila.study.AddNode(
                    studyId = studyId,
                    positionRef = lila.study.Position(chapter, path).ref,
                    node = branch,
                    opts = opts
                  )
                )
                .inject((path + branch.id, branch.fen))
        }
      }.inject(())

  def show(id: StudyId) = Open:
    env.study.api.byIdWithChapter(id).flatMap:
      _.fold(notFound): sc =>
        if !sc.study.canView(ctx.me.map(_.userId)) then notFound
        else Redirect(routes.Study.chapter(sc.study.id, sc.chapter.id)).toFuccess

  def chapter(id: StudyId, chapterId: StudyChapterId) = Open:
    env.study.api.byIdWithChapterOrFallback(id, chapterId).flatMap:
      _.fold(notFound): sc =>
        if !sc.study.canView(ctx.me.map(_.userId)) then notFound
        else
          env.study.chapterRepo.idNames(sc.study.id).flatMap: chapters =>
            val canWrite = ctx.me.exists(sc.study.canContribute)
            val pov =
              Pov(lila.core.game.Game.make(sc.chapter.setup.variant, sc.chapter.root.fen.some), sc.chapter.setup.orientation)
            val data = CondensedJsonView(pov, sc.chapter.root, ctx.pref)
            Ok.page(views.study.ui.chapter(data, sc.study, sc.chapter, canWrite, chapters))

  def anaMove(id: StudyId, chapterId: StudyChapterId) = AuthBody(parse.json) { ctx ?=> me ?=>
    ctx.body.body.asOpt[JsObject].fold(BadRequest("Invalid JSON").toFuccess): obj =>
      env.study.api.byIdWithChapter(id, chapterId).flatMap:
        _.fold(notFound): sc =>
          if !sc.study.canContribute(me) then Forbidden("No permission").toFuccess
          else if sc.chapter.isOverweight then BadRequest("Chapter is too big").toFuccess
          else
            lila.study.AnaMove
              .parse(obj)
              .fold(BadRequest("Invalid move payload").toFuccess): req =>
                val posNode = sc.chapter.root.nodeAt(req.path)
                posNode.fold(BadRequest("Invalid path").toFuccess): parent =>
                  val fixed = req.copy(variant = sc.chapter.setup.variant, fen = parent.fen, chapterId = chapterId.some)
                  fixed.branch match
                    case Left(err) => BadRequest(err.value).toFuccess
                    case Right(branch) =>
                      val opts = lila.study.MoveOpts.parse(obj)
                      given lila.study.Who = lila.study.Who(me.userId)
                      env.study.api
                        .addNode(
                          lila.study.AddNode(
                            studyId = id,
                            positionRef = lila.study.Position(sc.chapter, fixed.path).ref,
                            node = branch,
                            opts = opts
                          )
                        )
                        .map: _ =>
                          Ok(
                            Json.obj(
                              "ch" -> chapterId.value,
                              "path" -> fixed.path.value,
                              "node" -> lila.tree.Node.defaultNodeJsonWriter.writes(branch)
                            )
                          )
  }

  def anaDrop(id: StudyId, chapterId: StudyChapterId) = AuthBody(parse.json) { ctx ?=> me ?=>
    ctx.body.body.asOpt[JsObject].fold(BadRequest("Invalid JSON").toFuccess): obj =>
      env.study.api.byIdWithChapter(id, chapterId).flatMap:
        _.fold(notFound): sc =>
          if !sc.study.canContribute(me) then Forbidden("No permission").toFuccess
          else if sc.chapter.isOverweight then BadRequest("Chapter is too big").toFuccess
          else
            lila.study.AnaDrop
              .parse(obj)
              .fold(BadRequest("Invalid drop payload").toFuccess): req =>
                val posNode = sc.chapter.root.nodeAt(req.path)
                posNode.fold(BadRequest("Invalid path").toFuccess): parent =>
                  val fixed = req.copy(variant = sc.chapter.setup.variant, fen = parent.fen, chapterId = chapterId.some)
                  fixed.branch match
                    case Left(err) => BadRequest(err.value).toFuccess
                    case Right(branch) =>
                      val opts = lila.study.MoveOpts.parse(obj)
                      given lila.study.Who = lila.study.Who(me.userId)
                      env.study.api
                        .addNode(
                          lila.study.AddNode(
                            studyId = id,
                            positionRef = lila.study.Position(sc.chapter, fixed.path).ref,
                            node = branch,
                            opts = opts
                          )
                        )
                        .map: _ =>
                          Ok(
                            Json.obj(
                              "ch" -> chapterId.value,
                              "path" -> fixed.path.value,
                              "node" -> lila.tree.Node.defaultNodeJsonWriter.writes(branch)
                            )
                          )
  }

  def deleteNode(id: StudyId, chapterId: StudyChapterId) = AuthBody(parse.json) { ctx ?=> me ?=>
    ctx.body.body.asOpt[JsObject].flatMap(o => (o \ "path").asOpt[String]).fold(BadRequest("Missing path").toFuccess) {
      pathStr =>
        val path = UciPath(pathStr)
        if path.isEmpty then BadRequest("Cannot delete root").toFuccess
        else
          env.study.api.byIdWithChapter(id, chapterId).flatMap:
            _.fold(notFound): sc =>
              if !sc.study.canContribute(me) then Forbidden("No permission").toFuccess
              else
                env.study.api
                  .deleteNodeAt(id, lila.study.Position(sc.chapter, path).ref)(lila.study.Who(me.userId))
                  .inject(NoContent)
    }
  }

  def promoteNode(id: StudyId, chapterId: StudyChapterId) = AuthBody(parse.json) { ctx ?=> me ?=>
    val body = ctx.body.body.asOpt[JsObject]
    val pathStr = body.flatMap(o => (o \ "path").asOpt[String])
    val toMainline = body.flatMap(o => (o \ "toMainline").asOpt[Boolean]).getOrElse(false)
    pathStr.fold(BadRequest("Missing path").toFuccess): p =>
      val path = UciPath(p)
      if path.isEmpty then BadRequest("Invalid path").toFuccess
      else
        env.study.api.byIdWithChapter(id, chapterId).flatMap:
          _.fold(notFound): sc =>
            if !sc.study.canContribute(me) then Forbidden("No permission").toFuccess
            else
              given lila.study.Who = lila.study.Who(me.userId)
              env.study.api
                .promote(id, lila.study.Position(sc.chapter, path).ref, toMainline)
                .inject(NoContent)
  }

  def forceVariationNode(id: StudyId, chapterId: StudyChapterId) = AuthBody(parse.json) { ctx ?=> me ?=>
    val body = ctx.body.body.asOpt[JsObject]
    val pathStr = body.flatMap(o => (o \ "path").asOpt[String])
    val force = body.flatMap(o => (o \ "force").asOpt[Boolean]).getOrElse(false)
    pathStr.fold(BadRequest("Missing path").toFuccess): p =>
      val path = UciPath(p)
      if path.isEmpty then BadRequest("Invalid path").toFuccess
      else
        env.study.api.byIdWithChapter(id, chapterId).flatMap:
          _.fold(notFound): sc =>
            if !sc.study.canContribute(me) then Forbidden("No permission").toFuccess
            else
              env.study.api
                .forceVariation(id, lila.study.Position(sc.chapter, path).ref, force)(lila.study.Who(me.userId))
                .inject(NoContent)
  }

  def setNodeComment(id: StudyId, chapterId: StudyChapterId) = AuthBody(parse.json) { ctx ?=> me ?=>
    val body = ctx.body.body.asOpt[JsObject]
    val pathStr = body.flatMap(o => (o \ "path").asOpt[String])
    val text = body.flatMap(o => (o \ "text").asOpt[String]).getOrElse("")
    pathStr.fold(BadRequest("Missing path").toFuccess): p =>
      val path = UciPath(p)
      env.study.api.byIdWithChapter(id, chapterId).flatMap:
        _.fold(notFound): sc =>
          if !sc.study.canContribute(me) then Forbidden("No permission").toFuccess
          else
            val comment = lila.tree.Node.Comment.sanitize(text)
            env.study.api
              .setComment(id, lila.study.Position(sc.chapter, path).ref, comment)(lila.study.Who(me.userId))
              .flatMap: _ =>
                env.study.chapterRepo.byIdAndStudy(chapterId, id).map:
                  _.flatMap(_.root.nodeAt(path))
                .map:
                  case Some(node) =>
                    Ok(
                      Json.obj(
                        "path" -> path.value,
                        "node" -> lila.tree.Node.defaultNodeJsonWriter.writes(node)
                      )
                    )
                  case None => NotFound("Node not found")
  }

  def bookmakerSync(id: StudyId, chapterId: StudyChapterId) = AuthBody(parse.json) { ctx ?=> me ?=>
    ctx.body.body.validate[BookmakerSyncRequest].fold(
      errors => BadRequest(JsError.toJson(errors)).toFuccess,
      req =>
        env.study.api.byIdWithChapter(id, chapterId).flatMap:
          _.fold(notFound): sc =>
            if !sc.study.canContribute(me) then Forbidden("No permission").toFuccess
            else if sc.chapter.isOverweight then BadRequest("Chapter is too big").toFuccess
            else
              val originPath = UciPath(req.originPath)
              val commentPath = UciPath(req.commentPath)
              val originNodeOpt = sc.chapter.root.nodeAt(originPath)
              val commentNodeOpt = sc.chapter.root.nodeAt(commentPath)
              (originNodeOpt, commentNodeOpt) match
                case (None, _) => BadRequest("Invalid origin path").toFuccess
                case (_, None) => BadRequest("Invalid comment path").toFuccess
                case (Some(originNode), Some(_)) =>
                  val lines = req.maxLines.getOrElse(defaultPvLines).max(0).min(maxPvLines)
                  val plies = req.maxPlies.getOrElse(defaultPvPlies).max(0).min(maxPvPlies)
                  val pvMoves =
                    req.variations
                      .filter(_.moves.nonEmpty)
                      .take(lines)
                      .map(v => v.moves.take(plies))

                  val pvOpts = lila.study.MoveOpts(write = true, sticky = false, promoteToMainline = false)
                  given lila.study.Who = lila.study.Who(me.userId)

                  val addAll = pvMoves.foldLeft(funit): (acc, moves) =>
                    acc.flatMap(_ => insertPvLine(id, sc.chapter, originPath, originNode.fen, moves, pvOpts))

                  val comment = lila.tree.Node.Comment.sanitize(req.commentary)

                  addAll
                    .flatMap(_ =>
                      env.study.api.setExternalComment(id, lila.study.Position(sc.chapter, commentPath).ref, comment, aiAuthor)(
                        summon[lila.study.Who]
                      )
                    )
                    .inject(NoContent)
     )
  }

  def importPgn(id: StudyId, chapterId: StudyChapterId) = AuthBody { ctx ?=> me ?=>
    bindForm(importPgnForm)(
      _ => BadRequest("Invalid PGN").toFuccess,
      pgnRaw =>
        env.study.api.byIdWithChapter(id, chapterId).flatMap:
          _.fold(notFound): sc =>
            if !sc.study.canContribute(me) then Forbidden("No permission").toFuccess
            else
              val pgn = pgnRaw.trim
              val parsed =
                if pgn.isEmpty then Right(lila.tree.Root.default(sc.chapter.setup.variant) -> sc.chapter.setup.variant)
                else lila.study.StudyPgnImport.result(PgnStr(pgn), Nil).map(res => res.root -> res.variant)
              parsed match
                case Left(err) => BadRequest(err.value).toFuccess
                case Right((root, variant)) =>
                  env.study.api
                    .resetRoot(id, chapterId, root, variant)(lila.study.Who(me.userId))
                    .flatMap:
                      case Some(_) => NoContent.toFuccess
                      case None => notFound
    )
  }

  def create = AuthBody { ctx ?=> me ?=>
    bindForm(StudyForm.importGame.form)(
      err =>
        negotiate(
          Redirect(routes.User.show(me.username)),
          badJsonFormError(err)
        ),
      data =>
        data.gameId match
          case Some(_) =>
            negotiate(
              Redirect(routes.User.show(me.username)).flashing("error" -> "Game import is disabled in this deployment."),
              BadRequest(jsonError("game import disabled"))
            )
          case None =>
            env.study.api.importGame(lila.study.StudyMaker.ImportGame(data), me, true).flatMap {
              case Some(sc) =>
                negotiate(
                  Redirect(routes.Study.chapter(sc.study.id, sc.chapter.id)),
                  JsonOk(
                    Json.obj(
                      "id" -> sc.study.id.value,
                      "chapterId" -> sc.chapter.id.value,
                      "name" -> sc.study.name.value,
                      "chapterName" -> sc.chapter.name.value,
                      "canWrite" -> true,
                      "chapters" -> Json.arr(
                        Json.obj(
                          "id" -> sc.chapter.id.value,
                          "name" -> sc.chapter.name.value
                        )
                      ),
                      "url" -> routes.Study.chapter(sc.study.id, sc.chapter.id).url
                    )
                  )
                )
              case _ =>
                negotiate(
                  Redirect(routes.User.show(me.username)),
                  BadRequest(jsonError("Study creation failed"))
                )
            }
    )
  }

  def createAs = create

  def delete(id: StudyId) = Auth { _ ?=> me ?=>
    env.study.api.byIdAndOwnerOrAdmin(id, me).flatMap {
      case Some(study) => env.study.api.delete(study).inject(Redirect(routes.User.show(me.username)))
      case _ => notFound
    }
  }

  def allDefault(page: Int = 1) = all(lila.study.Orders.default, page)
  def all(order: lila.core.study.StudyOrder, page: Int) = Open:
    env.study.pager.all(order, page)(using ctx.me).flatMap: pag =>
      Ok.page(views.study.ui.all(pag, order))
  def byOwner(username: UserStr, order: lila.core.study.StudyOrder, page: Int) = Open:
    Redirect(routes.UserAnalysis.index.url + s"?study=1&owner=${username.value}&order=$order&page=$page").toFuccess
  def mine(order: lila.core.study.StudyOrder, page: Int) = Auth { _ ?=> me ?=>
    env.study.pager.mine(order, page).flatMap: pag =>
      Ok.page(views.study.ui.mine(pag, order))
  }
