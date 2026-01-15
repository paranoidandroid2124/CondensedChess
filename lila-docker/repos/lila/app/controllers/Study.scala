package controllers

import play.api.mvc.*
import lila.app.*
import lila.study.Study as LilaStudy
import lila.study.StudyForm

// Chesstory: Aggressively simplified Study controller
final class Study(
    env: Env
) extends LilaController(env):

  def show(id: StudyId) = Open:
    env.study.api.byIdWithChapter(id).flatMap { (opt: Option[LilaStudy.WithChapter]) =>
      opt.fold(notFound) { sc =>
        Ok(s"Study ${sc.study.name.value} - simplified view").toFuccess
      }
    }

  def chapter(id: StudyId, chapterId: StudyChapterId) = Open:
    env.study.api.byIdWithChapter(id, chapterId).flatMap { (opt: Option[LilaStudy.WithChapter]) =>
      opt.fold(notFound) { sc =>
        Ok(s"Study ${sc.study.name.value}, Chapter ${sc.chapter.name.value}").toFuccess
      }
    }

  def create = AuthBody { ctx ?=> me ?=>
    bindForm(StudyForm.importGame.form)(
      _ => Redirect(routes.User.show(me.username)),
      data =>
        env.study.api.importGame(lila.study.StudyMaker.ImportGame(data), me, true).flatMap { 
          case Some(sc) => Redirect(routes.Study.chapter(sc.study.id, sc.chapter.id)).toFuccess
          case _ => Redirect(routes.User.show(me.username)).toFuccess
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

  def allDefault(page: Int = 1) = Open:
    Redirect(routes.UserAnalysis.index.url + s"?study=1&page=$page").toFuccess
  def all(order: lila.core.study.StudyOrder, page: Int) = Open:
    Redirect(routes.UserAnalysis.index.url + s"?study=1&order=$order&page=$page").toFuccess
  def byOwner(username: UserStr, order: lila.core.study.StudyOrder, page: Int) = Open:
    Redirect(routes.UserAnalysis.index.url + s"?study=1&owner=${username.value}&order=$order&page=$page").toFuccess
  def mine(order: lila.core.study.StudyOrder, page: Int) = Open:
    Redirect(routes.UserAnalysis.index.url + s"?study=1&mine=1&order=$order&page=$page").toFuccess
