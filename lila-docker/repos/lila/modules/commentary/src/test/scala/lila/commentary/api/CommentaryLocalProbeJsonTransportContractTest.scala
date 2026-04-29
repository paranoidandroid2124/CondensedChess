package lila.commentary.api

import play.api.libs.json.*

class CommentaryLocalProbeJsonTransportContractTest extends munit.FunSuite:

  test("local probe transport accepts internal completed probe while public transport rejects it"):
    val payload = localProbePayload

    assert(CommentaryPublicJsonTransport.renderJson(payload).isLeft)
    val response = CommentaryLocalProbeJsonTransport.renderJson(payload)

    assert(response.isRight)
    val json = response.toOption.get.toString
    assert(json.contains("variationEvidence"), json)
    Vector("root-candidate", "parentBranchId", "engineFingerprint", "rawPv", "cacheKey").foreach: token =>
      assert(!json.contains(token), clues(token, json))

  test("local probe transport stays silent when completed probe does not yield public line evidence"):
    val payload = localProbePayload ++ Json.obj(
      "completedProbe" -> (localProbePayload \ "completedProbe").as[JsObject].deepMerge(
        Json.obj("childProbes" -> Json.arr())
      )
    )

    val response = CommentaryLocalProbeJsonTransport.renderJson(payload)

    assert(response.isRight)
    val json = response.toOption.get
    assertEquals((json \ "status").as[String], "noCommentary")
    assertEquals((json \ "render" \ "variationEvidence").as[Vector[JsValue]], Vector.empty)
    assertEquals((json \ "render" \ "blocks").as[Vector[JsValue]], Vector.empty)

  test("local probe transport rejects nested public engine and debug request fields"):
    val request = (localProbePayload \ "request").as[JsObject]
    val debugOnlyPayload = localProbePayload ++ Json.obj(
      "request" -> request.deepMerge(Json.obj("debug" -> true))
    )
    val payload = localProbePayload ++ Json.obj(
      "request" -> request.deepMerge(
        Json.obj(
          "debug" -> true,
          "enginePacket" -> Json.obj(
            "fen" -> "8/8/8/8/8/8/8/8 w - - 0 1"
          )
        )
      )
    )

    assert(CommentaryLocalProbeJsonTransport.renderJson(debugOnlyPayload).isLeft)
    assert(CommentaryLocalProbeJsonTransport.renderJson(payload).isLeft)

  private def localProbePayload: JsObject =
    val currentFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
    Json.obj(
      "request" -> Json.obj(
        "currentFen" -> currentFen,
        "beforeFen" -> "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "playedMove" -> "e2e4",
        "nodeId" -> "root",
        "ply" -> 1
      ),
      "completedProbe" -> Json.obj(
        "current" -> Json.obj("currentFen" -> currentFen, "nodeId" -> "root", "ply" -> 1, "variant" -> "standard"),
        "engineFingerprint" -> "local-stockfish-contract",
        "budget" -> Json.obj("rootMultiPv" -> 3, "childMultiPv" -> 2, "depthFloor" -> 16, "rootTargetDepth" -> 18, "childTargetDepth" -> 18),
        "probeRequests" -> Json.arr(),
        "rootProbe" -> Json.obj(
          "currentFen" -> currentFen,
          "nodeId" -> "root",
          "ply" -> 1,
          "variant" -> "standard",
          "engineFingerprint" -> "local-stockfish-contract",
          "requestedDepth" -> 18,
          "realizedDepth" -> 18,
          "multiPv" -> 3,
          "generatedAt" -> System.currentTimeMillis().toString,
          "maxAgeMillis" -> 60000,
          "completed" -> true,
          "lines" -> Json.arr(
            Json.obj("rank" -> 1, "multiPvIndex" -> 1, "multiPv" -> 3, "uci" -> Json.arr("e7e5", "g1f3")),
            Json.obj("rank" -> 2, "multiPvIndex" -> 2, "multiPv" -> 3, "uci" -> Json.arr("c7c5", "g1f3")),
            Json.obj("rank" -> 3, "multiPvIndex" -> 3, "multiPv" -> 3, "uci" -> Json.arr("e7e6", "d2d4"))
          )
        ),
        "childProbes" -> Json.arr(
          Json.obj(
            "currentFen" -> "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
            "nodeId" -> "root",
            "ply" -> 2,
            "variant" -> "standard",
            "engineFingerprint" -> "local-stockfish-contract",
            "parentBranchId" -> "root-candidate-1",
            "parentUciPrefix" -> Json.arr("e7e5"),
            "parentRootRank" -> 1,
            "requestedDepth" -> 18,
            "realizedDepth" -> 18,
            "multiPv" -> 2,
            "generatedAt" -> System.currentTimeMillis().toString,
            "maxAgeMillis" -> 60000,
            "completed" -> true,
            "lines" -> Json.arr(
              Json.obj("rank" -> 1, "multiPvIndex" -> 1, "multiPv" -> 2, "uci" -> Json.arr("g1f3", "b8c6")),
              Json.obj("rank" -> 2, "multiPvIndex" -> 2, "multiPv" -> 2, "uci" -> Json.arr("d2d4", "e5d4"))
            )
          ),
          Json.obj(
            "currentFen" -> "rnbqkbnr/pppppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
            "nodeId" -> "root",
            "ply" -> 2,
            "variant" -> "standard",
            "engineFingerprint" -> "local-stockfish-contract",
            "parentBranchId" -> "root-candidate-2",
            "parentUciPrefix" -> Json.arr("c7c5"),
            "parentRootRank" -> 2,
            "requestedDepth" -> 18,
            "realizedDepth" -> 18,
            "multiPv" -> 2,
            "generatedAt" -> System.currentTimeMillis().toString,
            "maxAgeMillis" -> 60000,
            "completed" -> true,
            "lines" -> Json.arr(
              Json.obj("rank" -> 1, "multiPvIndex" -> 1, "multiPv" -> 2, "uci" -> Json.arr("g1f3", "b8c6")),
              Json.obj("rank" -> 2, "multiPvIndex" -> 2, "multiPv" -> 2, "uci" -> Json.arr("d2d4", "c5d4"))
            )
          )
        )
      )
    )
