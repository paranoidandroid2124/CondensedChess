package controllers

import play.api.mvc.*
import lila.app.Env
import lila.core.id.*
// Note: UserStr and Context removed - unused after @nowarn additions

/** Stub controllers for deleted modules.
  * All stubs redirect to /analysis to satisfy UI references without breaking builds.
  * This follows the "Aggressive Simplification" principle of CondensedChess.
  */
final class StubController(env: Env) extends LilaController(env):
  def stub = Open:
    Redirect(routes.UserAnalysis.index)
  def stub1(@annotation.nowarn p1: String) = Open:
    Redirect(routes.UserAnalysis.index)
  def stub2(@annotation.nowarn p1: String, @annotation.nowarn p2: String) = Open:
    Redirect(routes.UserAnalysis.index)
  def stub3(@annotation.nowarn p1: String, @annotation.nowarn p2: String, @annotation.nowarn p3: String) = Open:
    Redirect(routes.UserAnalysis.index)
  // For User.mod - mod zone deleted
  def userMod(@annotation.nowarn user: lila.core.userId.UserStr) = Open:
    Redirect(routes.UserAnalysis.index)
  // For User.setDoxNote - mod feature deleted
  def setDoxNote(@annotation.nowarn id: String, @annotation.nowarn v: Boolean) = Open:
    Redirect(routes.UserAnalysis.index)
  // For Appeal.home - appeal module deleted
  def appealHome = Open:
    Redirect(routes.UserAnalysis.index)
  // For Team.show - team module deleted
  def teamShow(@annotation.nowarn id: String) = Open:
    Redirect(routes.UserAnalysis.index)
  // Specific streamer stubs to avoid type erasure conflicts
  def streamerSubscribe(@annotation.nowarn streamer: lila.core.userId.UserStr) = Open:
    Redirect(routes.UserAnalysis.index)
  def streamerPlatform(@annotation.nowarn platform: String) = Open:
    Redirect(routes.UserAnalysis.index)

final class Cms(env: Env) extends LilaController(env):
  def tos = Open:
    Redirect(routes.UserAnalysis.index)
  def menuPage(@annotation.nowarn key: lila.core.id.CmsPageKey) = Open:
    Redirect(routes.UserAnalysis.index)
  def lonePage(@annotation.nowarn key: lila.core.id.CmsPageKey) = Open:
    Redirect(routes.UserAnalysis.index)
  def variantHome = Open:
    Redirect(routes.UserAnalysis.index)
  def variant(@annotation.nowarn key: chess.variant.Variant.LilaKey) = Open:
    Redirect(routes.UserAnalysis.index)
  def help = Open:
    Redirect(routes.UserAnalysis.index)
  def source = Open:
    Redirect(routes.UserAnalysis.index)

final class TitleVerify(env: Env) extends LilaController(env):
  def index = Open:
    Redirect(routes.UserAnalysis.index)
  def form = Open:
    Redirect(routes.UserAnalysis.index)
  @scala.annotation.targetName("showStub")
  def show(@annotation.nowarn id: TitleRequestId) = Open:
    Redirect(routes.UserAnalysis.index)
  @scala.annotation.targetName("processStub")
  def process(@annotation.nowarn id: TitleRequestId) = Open:
    Redirect(routes.UserAnalysis.index)
  def create = Open:
    Redirect(routes.UserAnalysis.index)
  @scala.annotation.targetName("updateStub")
  def update(@annotation.nowarn id: TitleRequestId) = Open:
    Redirect(routes.UserAnalysis.index)
  @scala.annotation.targetName("cancelStub")
  def cancel(@annotation.nowarn id: TitleRequestId) = Open:
    Redirect(routes.UserAnalysis.index)
  @scala.annotation.targetName("imageStub")
  def image(@annotation.nowarn id: TitleRequestId, @annotation.nowarn tag: String) = Open:
    Redirect(routes.UserAnalysis.index)

final class Tv(env: Env) extends LilaController(env):
  def index = Open:
    Redirect(routes.UserAnalysis.index)
  def onChannel(@annotation.nowarn chanKey: String) = Open:
    Redirect(routes.UserAnalysis.index)
  def games = Open:
    Redirect(routes.UserAnalysis.index)
  def gameChannelReplacement(
      @annotation.nowarn chanKey: String,
      @annotation.nowarn gameId: GameId,
      @annotation.nowarn exclude: List[GameId]
  ) = Open:
    Redirect(routes.UserAnalysis.index)

final class Mod(env: Env) extends LilaController(env):
  def apiUserLog(@annotation.nowarn user: lila.core.userId.UserStr) = Open:
    Redirect(routes.UserAnalysis.index)

final class Report(env: Env) extends LilaController(env):
  def form = Open:
    Redirect(routes.UserAnalysis.index)

final class PlayApi(env: Env) extends LilaController(env):
  def botOnline = Open:
    Redirect(routes.UserAnalysis.index)
