package lila.commentary.api

import play.api.libs.json.*

import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.certification.CertificationEngineRuntimeIntake

class CommentaryPublicJsonTransportContractTest extends munit.FunSuite:

  private val validFen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"
  private val nodeId = "mainline:0"

  test("valid minimal public render request parses and returns public response without internal metadata"):
    val parsed = CommentaryPublicJsonTransport.parseRequest(minimalRequest)
    val json = publicResponse(minimalRequest)
    val response = json.as[CommentaryResponse]

    assert(parsed.isRight)
    assert(response.status != CommentaryResponseStatus.InvalidRequest)
    assertEquals(response.internal, None)

  test("caller debug flag does not expose internal metadata"):
    val json = publicResponse(minimalRequest ++ Json.obj("debug" -> true))
    val response = json.as[CommentaryResponse]

    assertEquals(response.internal, None)
    assertNoTokens(json, Vector("invalidReason", "engineIntake"))

  test("separator and case variants are rejected at top level and sanitized"):
    forbiddenFieldVariants.foreach: field =>
      val error = badRequest(minimalRequest ++ Json.obj(field -> Json.obj("secret" -> s"$field-secret")))

      assertSanitizedBadRequest(error)
      assertNoTokens(error, Vector(field, s"$field-secret", "completed_probe", "candidate-line", "cache-key", "proof_id"))

  test("separator and case variants are rejected inside enginePacket and sanitized"):
    forbiddenFieldVariants.foreach: field =>
      val error = badRequest(
        minimalRequest ++ Json.obj(
          "enginePacket" -> Json.obj(field -> Json.obj("secret" -> s"$field-secret"))
        )
      )

      assertSanitizedBadRequest(error)
      assertNoTokens(error, Vector(field, s"$field-secret", "enginePacket", "ROOT_PROBE", "parent-uci-prefix", "internal_payload"))

  test("internal candidate-line probe cache source and proof vocabulary is rejected without echo"):
    internalVocabularyFields.foreach: field =>
      val error = badRequest(
        minimalRequest ++ Json.obj(
          "enginePacket" -> Json.obj(field -> s"$field-secret")
        )
      )

      assertSanitizedBadRequest(error)
      assertNoTokens(error, Vector(field, s"$field-secret"))

  test("valid typed RuntimeEnginePacket JSON with public engine intake fields and no claims is not rejected by transport guard"):
    val packetJson = Json.toJson(enginePacket())
    val parsed = CommentaryPublicJsonTransport.parseRequest(minimalRequest ++ Json.obj("enginePacket" -> packetJson))

    assertEquals((packetJson \ "engineConfigFingerprint").as[String], "api-transport-test-engine")
    assertEquals((packetJson \ "pvLines").as[Vector[Vector[String]]], Vector(Vector("g8f6")))
    assert(parsed.isRight)

  test("public runtime engine claims are rejected even when purpose evidence map is typed"):
    val packetJson = Json.toJson(
      enginePacket(
        claims = Vector(
          runtimeEngineClaim(
            purposes = Map(
              "comparative_superiority" -> "satisfied",
              "best_defense_survival" -> "satisfied",
              "counterplay_denial" -> "insufficient"
            )
          )
        )
      )
    )
    val error = badRequest(minimalRequest ++ Json.obj("enginePacket" -> packetJson))

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("best_defense_survival", "counterplay_denial", "enginePacket", "claims"))

  test("public runtime engine baseline is rejected without paired transition request"):
    val packetJson = Json.toJson(enginePacket()).as[JsObject] ++
      Json.obj(
        "baseline" -> Json.obj(
          "fen" -> validFen,
          "nodeId" -> nodeId,
          "ply" -> 0,
          "search" -> Json.obj(
            "requestedDepth" -> 18,
            "realizedDepth" -> 18,
            "multiPv" -> 1,
            "completed" -> true,
            "generatedAtEpochMs" -> 1,
            "maxAgeMs" -> 1,
            "engineConfigFingerprint" -> "caller-baseline-engine"
          ),
          "score" -> Json.obj("type" -> "centipawns", "cp" -> 0),
          "scorePerspective" -> "white",
          "pvLines" -> Json.arr(Json.arr("g8f6"))
        )
      )
    val error = badRequest(minimalRequest ++ Json.obj("enginePacket" -> packetJson))

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("baseline", "caller-baseline-engine", "g8f6"))

  test("unknown runtime engine claim purpose key is rejected without echo"):
    val error = badRequest(requestWithClaimPurposes(Json.obj("future_payload" -> "secret-purpose-token")))

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("future_payload", "secret-purpose-token", "enginePacket", "claims"))

  test("runtime engine claim purpose key cannot reuse packet field names"):
    val error = badRequest(requestWithClaimPurposes(Json.obj("engineConfigFingerprint" -> "satisfied", "pvLines" -> "satisfied")))

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("engineConfigFingerprint", "pvLines", "enginePacket", "claims"))

  test("unknown runtime engine claim purpose strength is rejected without echo"):
    Vector("secret", "future_strength").foreach: strength =>
      val error = badRequest(requestWithClaimPurposes(Json.obj("comparative_superiority" -> strength)))

      assertSanitizedBadRequest(error)
      assertNoTokens(error, Vector(strength, "comparative_superiority", "enginePacket", "claims"))

  test("internal-looking runtime engine claim purpose keys are rejected without echo"):
    Vector("requestProofId", "cache-key").foreach: field =>
      val error = badRequest(requestWithClaimPurposes(Json.obj(field -> "satisfied")))

      assertSanitizedBadRequest(error)
      assertNoTokens(error, Vector(field, "enginePacket", "claims"))

  test("invalid typed score object shape is rejected without decoder details"):
    val packetJson = Json.toJson(enginePacket()).as[JsObject]
    val invalidScore = (packetJson \ "score").as[JsObject] ++ Json.obj(
      "futureScoreContext" -> Json.obj("note" -> "score-secret")
    )
    val error = badRequest(minimalRequest ++ Json.obj("enginePacket" -> (packetJson ++ Json.obj("score" -> invalidScore))))

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("futureScoreContext", "score-secret", "JsError"))

  test("invalid typed claim object shape is rejected without decoder details"):
    val packetJson = Json.toJson(enginePacket()).as[JsObject]
    val claimJson = Json.toJson(runtimeEngineClaim()).as[JsObject] ++ Json.obj(
      "futureClaimContext" -> Json.obj("note" -> "claim-secret")
    )
    val error = badRequest(minimalRequest ++ Json.obj("enginePacket" -> (packetJson ++ Json.obj("claims" -> Json.arr(claimJson)))))

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("futureClaimContext", "claim-secret", "JsError"))

  test("bad request JSON is sanitized and does not echo caller tokens"):
    val error =
      badRequest(Json.obj("nodeId" -> nodeId, "ply" -> 0, "cache-key" -> "secret-cache-token", "proof_id" -> "secret-proof-token"))

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("currentFen", "nodeId", "ply", "cache-key", "proof_id", "secret-cache-token", "secret-proof-token", "JsError"))

  test("suspicious request is rejected before response JSON can leak probe cache or internal tokens"):
    val error = badRequest(
      minimalRequest ++ Json.obj(
        "completed-probe" -> Json.obj(
          "probeRequests" -> Json.arr(Json.obj("cache-key" -> "secret-cache-token")),
          "internal_payload" -> Json.obj("proof_id" -> "secret-proof-token")
        )
      )
    )

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("completed-probe", "probeRequests", "probe", "cache", "internal", "secret-cache-token", "secret-proof-token"))

  test("public completedProbe is rejected even when it has sanitized probe shape"):
    val error = badRequest(
      minimalRequest ++ Json.obj(
        "completedProbe" -> Json.obj(
          "current" -> Json.obj("currentFen" -> validFen, "nodeId" -> nodeId, "ply" -> 0, "variant" -> "standard"),
          "engineFingerprint" -> "caller-stockfish-forged",
          "budget" -> Json.obj("rootMultiPv" -> 3, "childMultiPv" -> 2, "depthFloor" -> 16),
          "probeRequests" -> Json.arr(
            Json.obj(
              "role" -> "root_candidate",
              "currentFen" -> validFen,
              "nodeId" -> nodeId,
              "ply" -> 0,
              "variant" -> "standard",
              "multiPv" -> 3,
              "requestedDepth" -> 18,
              "depthFloor" -> 16
            )
          ),
          "rootProbe" -> Json.obj(
            "currentFen" -> validFen,
            "nodeId" -> nodeId,
            "ply" -> 0,
            "variant" -> "standard",
            "engineFingerprint" -> "caller-stockfish-forged",
            "requestedDepth" -> 18,
            "realizedDepth" -> 18,
            "multiPv" -> 3,
            "generatedAt" -> "2026-04-29T00:00:00.000Z",
            "maxAgeMillis" -> 60000,
            "completed" -> true,
            "lines" -> Json.arr(Json.obj("rank" -> 1, "multiPvIndex" -> 1, "multiPv" -> 3, "uci" -> Json.arr("g8f6")))
          ),
          "childProbes" -> Json.arr()
        )
      )
    )

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("completedProbe", "caller-stockfish-forged", "rootProbe", "probeRequests", "g8f6"))

  test("unknown top-level completed-payload-shaped fields are rejected and sanitized"):
    val error = badRequest(
      minimalRequest ++ Json.obj(
        "futurePayload" -> Json.obj(
          "current" -> Json.obj("fen" -> validFen, "nodeId" -> nodeId),
          "root" -> Json.obj(
            "completed" -> true,
            "multiPv" -> 3,
            "requestedDepth" -> 18,
            "realizedDepth" -> 18,
            "generatedAtEpochMs" -> 1,
            "maxAgeMs" -> 1,
            "lines" -> Json.arr(Json.arr("g8f6"))
          )
        )
      )
    )

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("futurePayload", "current", "root", "completed", "lines", "g8f6"))

  test("unknown nested object under enginePacket is rejected and sanitized"):
    val packetJson = Json.toJson(enginePacket()).as[JsObject]
    val error = badRequest(
      minimalRequest ++ Json.obj(
        "enginePacket" -> (packetJson ++ Json.obj("futureContext" -> Json.obj("note" -> "quiet-secret")))
      )
    )

    assertSanitizedBadRequest(error)
    assertNoTokens(error, Vector("enginePacket", "futureContext", "quiet-secret"))

  test("normalized forbidden suffix and embedded field variants are rejected without echo"):
    forbiddenSuffixAndEmbeddedVariants.foreach: field =>
      val error = badRequest(minimalRequest ++ Json.obj(field -> Json.obj("secret" -> s"$field-secret")))

      assertSanitizedBadRequest(error)
      assertNoTokens(error, Vector(field, s"$field-secret"))

  private def forbiddenFieldVariants: Vector[String] =
    Vector(
      "completed_probe",
      "candidate-line",
      "cache-key",
      "proof_id",
      "ROOT_PROBE",
      "parent-uci-prefix",
      "internal_payload"
    )

  private def internalVocabularyFields: Vector[String] =
    Vector(
      "engineFingerprint",
      "rawPv",
      "rawLines",
      "rawProbe",
      "branchId",
      "parentBranchId",
      "sourceRow",
      "probeRequests",
      "strategyProbeRequest",
      "tauCacheKey",
      "proofBurden",
      "proofId",
      "proves"
    )

  private def forbiddenSuffixAndEmbeddedVariants: Vector[String] =
    Vector(
      "requestProofId",
      "preparedProves",
      "branchIds",
      "rootBranchId",
      "rawPvLines",
      "rawLinePayload",
      "candidateEngineFingerprint"
    )

  private def minimalRequest: JsObject =
    Json.obj(
      "currentFen" -> validFen,
      "nodeId" -> nodeId,
      "ply" -> 0
    )

  private def publicResponse(body: JsValue): JsValue =
    CommentaryPublicJsonTransport.renderJson(body).fold(error => fail(s"expected public response, got $error"), identity)

  private def badRequest(body: JsValue): JsObject =
    CommentaryPublicJsonTransport.renderJson(body).fold(identity, response => fail(s"expected bad request, got $response"))

  private def requestWithClaimPurposes(purposes: JsObject): JsObject =
    val claimJson = Json.toJson(runtimeEngineClaim()).as[JsObject] ++ Json.obj("purposes" -> purposes)
    val packetJson = Json.toJson(enginePacket()).as[JsObject] ++ Json.obj("claims" -> Json.arr(claimJson))
    minimalRequest ++ Json.obj("enginePacket" -> packetJson)

  private def assertSanitizedBadRequest(error: JsObject)(using munit.Location): Unit =
    assertEquals((error \ "error").as[String], "invalid_commentary_request")
    assertEquals(error.keys, Set("error"))

  private def assertNoTokens(json: JsValue, tokens: Vector[String])(using munit.Location): Unit =
    val rendered = Json.stringify(json)
    tokens.foreach: token =>
      assert(!rendered.contains(token), clues(token, rendered))

  private def enginePacket(
      packetNodeId: String = nodeId,
      fen: String = validFen,
      ply: Int = 0,
      claims: Vector[CertificationEngineRuntimeIntake.RuntimeCertificationClaim] = Vector.empty
  ): CertificationEngineRuntimeIntake.RuntimeEnginePacket =
    CertificationEngineRuntimeIntake.RuntimeEnginePacket(
      fen = fen,
      nodeId = packetNodeId,
      ply = ply,
      requestedDepth = 18,
      realizedDepth = 18,
      multiPv = 1,
      completed = true,
      generatedAtEpochMs = System.currentTimeMillis(),
      maxAgeMs = 20_000L,
      engineConfigFingerprint = "api-transport-test-engine",
      score = CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(80),
      scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.White,
      pvLines = Vector(Vector("g8f6")),
      claims = claims
    )

  private def runtimeEngineClaim(
      purposes: Map[String, String] = Map("comparative_superiority" -> "satisfied")
  ): CertificationEngineRuntimeIntake.RuntimeCertificationClaim =
    CertificationEngineRuntimeIntake.RuntimeCertificationClaim(
      familyId = "MobilityComparison",
      owner = "white",
      purposes = purposes,
      anchor = CertificationEngineRuntimeIntake.RuntimeAnchor.Board,
      minDepth = 18,
      minMultiPv = 1,
      minPvPlies = 1,
      requiredScore = Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtLeast(1))
    )
