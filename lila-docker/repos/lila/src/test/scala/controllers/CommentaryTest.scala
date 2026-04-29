package controllers

import java.nio.file.{ Files, Paths }

class CommentaryTest extends munit.FunSuite:

  test("public commentary render route is registered"):
    val routes = Files.readString(Paths.get("conf/routes"))

    assert(routes.contains("POST  /api/commentary/render"))
    assert(routes.contains("controllers.Commentary.render"))

  test("local probe commentary route is internal only"):
    val routes = Files.readString(Paths.get("conf/routes"))

    assert(routes.contains("POST  /internal/commentary/render-local-probe"))
    assert(routes.contains("controllers.Commentary.renderLocalProbeCommentary"))

  test("local probe commentary route rejects production before JSON body parsing"):
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))

    assert(controller.contains("def renderLocalProbeCommentary =\n    if env.mode.isProd then Open:"))
    assert(!controller.contains("def renderLocalProbeCommentary = OpenBodyOf(parse.json):"))
