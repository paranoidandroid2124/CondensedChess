package controllers

import java.nio.file.{ Files, Paths }

class CommentaryTest extends munit.FunSuite:

  test("public commentary render route is registered"):
    val routes = Files.readString(Paths.get("conf/routes"))

    assert(routes.contains("POST  /api/commentary/render"))
    assert(routes.contains("controllers.Commentary.renderCommentary"))

  test("local probe commentary route is registered"):
    val routes = Files.readString(Paths.get("conf/routes"))

    assert(routes.contains("POST  /internal/commentary/render-local-probe"))
    assert(routes.contains("controllers.Commentary.renderLocalProbeCommentary"))

  test("commentary routes remain fail-closed disabled"):
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))

    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(controller.contains("def renderCommentary = OpenBodyOf(parse.json):"))
    assert(controller.contains("def renderLocalProbeCommentary =\n    OpenBodyOf(parse.json):"))
    assert(!controller.contains("env.mode.isProd"))

  test("KCNFC-6 commentary routes expose no public 200 or production API"):
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))

    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(!controller.contains("Ok("), "commentary route tombstones must not return 200")
    assert(!controller.contains("LlmNarrationSmoke"), "commentary routes must not expose public LLM narration")
    Vector("BoardFacts", "Story(", "CheckGivenProof", "CheckEscapedProof", "CheckmateProof", "EngineCheck").foreach:
      raw =>
        assert(!controller.contains(raw), s"commentary routes must not expose raw $raw")
