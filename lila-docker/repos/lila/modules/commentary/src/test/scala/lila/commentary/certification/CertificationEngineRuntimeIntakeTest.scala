package lila.commentary.certification

import chess.Color
import chess.format.{ Fen, Uci }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

import lila.commentary.delta.StrategicDeltaExtractor
import lila.commentary.projection.{ StrategyProjectionAdmission, StrategyProjectionEvidence, StrategyProjectionBandId }
import lila.commentary.strategic.StrategicObjectExtractor
import lila.commentary.witness.WitnessAnchor
import lila.commentary.witness.seed.StrategySupportSeedExtractor

class CertificationEngineRuntimeIntakeTest extends munit.FunSuite:

  private val exactFen =
    Fen.Full.clean("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3")
  private val deltaBeforeFen =
    Fen.Full.clean("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1")
  private val deltaAfterFen =
    Fen.Full.clean("4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1")
  private val current =
    StrategicObjectExtractor.fromFenFailClosed(exactFen).fold(message => fail(message), identity)
  private val node = EngineNodeIdentity("mainline:3b", 5)

  test("valid same-FEN analyse engine packet creates bounded certification evidence"):
    val result =
      CertificationEngineRuntimeIntake.forObjectExtraction(
        current = current,
        expectedNode = node,
        expectedFen = exactFen,
        packet = Some(runtimePacket()),
        nowEpochMs = 12_000L
      )

    assertEquals(result.status, CertificationEngineRuntimeIntake.Status.Accepted)
    val evidence =
      result.evidence
        .evidenceFor(CertificationId("CertifiedKingSafetyEdge"), Color.White, WitnessAnchor.BoardAnchor)
        .getOrElse(fail("missing engine-backed certification evidence"))
    assert(
      evidence.engineRoles.map(_.key) == Set("best_defense_survival"),
      clues(result)
    )

  test("missing, stale-FEN, wrong node/ply, shallow, illegal PV, and ambiguous MultiPV packets fail closed"):
    assertEquals(
      CertificationEngineRuntimeIntake
        .forObjectExtraction(current, node, exactFen, None, 12_000L)
        .status,
      CertificationEngineRuntimeIntake.Status.Missing
    )
    assertRejected(
      runtimePacket(fen = exactFen.toString.replace(" 3 3", " 4 3")),
      "exact FEN"
    )
    assertRejected(
      runtimePacket(nodeId = "other-node"),
      "node identity"
    )
    assertRejected(
      runtimePacket(ply = 6),
      "node identity"
    )
    assertRejected(
      runtimePacket(realizedDepth = 12),
      "depth"
    )
    assertRejected(
      runtimePacket(pvLines = Vector(Vector("e2e4", "g8f6"), Vector("d7d6", "f3g5"), Vector("e5d4", "f3d4"))),
      "illegal PV"
    )
    assertRejected(
      runtimePacket(multiPv = 2, pvLines = Vector(Vector("g8f6", "f3g5"), Vector("d7d6", "f3g5"))),
      "MultiPV 3"
    )

  test("role policy invariants reject weak PV semantics before certification evidence is minted"):
    assertRejected(
      runtimePacket().copy(
        claims = Vector(runtimeClaim(purposes = Map("best_defense_survival" -> "satisfied"), roleReports = Some(Map.empty)))
      ),
      "best-defense semantic coverage"
    )
    assertRejected(
      runtimePacket().copy(
        claims = Vector(
          runtimeClaim(
            purposes = Map("comparative_superiority" -> "satisfied"),
            scoreRequirement = Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtLeast(50))
          )
        )
      ),
      "bound baseline"
    )
    assertRejected(
      runtimePacket(requestedDepth = 1, realizedDepth = 1).copy(
        claims = Vector(runtimeClaim(minDepth = 1, minPvPlies = 1))
      ),
      "Q policy target depth"
    )

  test("outcome caps reject mate and material-collapse scores for non-result strategic engine claims"):
    assertRejected(
      runtimePacket(
        score = CertificationEngineRuntimeIntake.RuntimeScore.MateIn(3),
        scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.White,
        scoreRequirement = CertificationEngineRuntimeIntake.RuntimeScoreRequirement.MateInAtMost(4)
      ).copy(
        claims = Vector(
          runtimeClaim(
            purposes = Map("best_defense_survival" -> "satisfied"),
            scoreRequirement = Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.MateInAtMost(4))
          )
        )
      ),
      "mate outcome cap"
    )
    assertRejected(
      runtimePacket(
        score = CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(1_500),
        scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.White,
        scoreRequirement = CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtLeast(1_200)
      ).copy(
        claims = Vector(
          runtimeClaim(
            purposes = Map("best_defense_survival" -> "satisfied"),
            scoreRequirement = Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtLeast(1_200))
          )
        )
      ),
      "material-collapse cap"
    )
    Vector("horizon_limited", "multipv_ambiguous", "tablebase_required").foreach: cap =>
      assertRejected(
        runtimePacket().copy(claims = Vector(runtimeClaim(publicCaps = Some(Vector(cap))))),
        "public outcome cap"
      )

  test("runtime intake rejects malformed certification claim and transition normalization fields"):
    assertRejected(
      runtimePacket().copy(claims = Vector(runtimeClaim(familyId = "InactiveFamily"))),
      "inactive certification family"
    )
    assertRejected(
      runtimePacket().copy(claims = Vector(runtimeClaim(purposes = Map("unknown_purpose" -> "satisfied")))),
      "Unknown engine evidence purpose"
    )
    assertRejected(
      runtimePacket().copy(claims = Vector(runtimeClaim(purposes = Map("best_defense_survival" -> "unknown_strength")))),
      "Unknown engine evidence strength"
    )
    assertRejected(
      runtimePacket().copy(claims = Vector(runtimeClaim(owner = "green"))),
      "unknown owner color"
    )
    assertRejected(
      runtimePacket(pvLines = Vector(Vector("notuci"), Vector("d7d6", "f3g5"), Vector("e5d4", "f3d4"))),
      "invalid UCI"
    )
    val partialBeforeNode =
      runtimeTransitionPacket(EngineNodeIdentity("mainline:transition", 2)).copy(
        transition = Some(
          CertificationEngineRuntimeIntake.RuntimeTransitionBinding(
            deltaBeforeFen.toString,
            "d6c7",
            deltaAfterFen.toString,
            beforeNodeId = Some("mainline:before"),
            beforePly = None
          )
        )
      )
    val partialResult =
      CertificationEngineRuntimeIntake.forDeltaExtraction(
        deltaExtraction(),
        EngineNodeIdentity("mainline:transition", 2),
        deltaBeforeFen,
        deltaAfterFen,
        Some(partialBeforeNode),
        12_000L
      )
    assertEquals(partialResult.status, CertificationEngineRuntimeIntake.Status.Rejected)
    assert(partialResult.reason.exists(_.contains("beforeNodeId and beforePly")), clues(partialResult))

  test("mate and centipawn side-perspective normalization are admitted only under typed score requirements"):
    val mateResult =
      CertificationEngineRuntimeIntake.forObjectExtraction(
        current = current,
        expectedNode = node,
        expectedFen = exactFen,
        packet = Some(
          runtimePacket(
            score = CertificationEngineRuntimeIntake.RuntimeScore.MateIn(3),
            scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.White,
            scoreRequirement = CertificationEngineRuntimeIntake.RuntimeScoreRequirement.MateInAtMost(4)
          ).copy(
            claims = Vector(
              runtimeClaim(
                familyId = "MateNetCertification",
                purposes = Map("best_defense_survival" -> "satisfied"),
                scoreRequirement = Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.MateInAtMost(4))
              )
            )
          )
        ),
        nowEpochMs = 12_000L
      )
    assertEquals(mateResult.status, CertificationEngineRuntimeIntake.Status.Accepted)

    assertRejected(
      runtimePacket(
        score = CertificationEngineRuntimeIntake.RuntimeScore.MateIn(3),
        scoreRequirement = CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtMost(-100)
      ).copy(
        claims = Vector(
          runtimeClaim(
            familyId = "MateNetCertification",
            purposes = Map("best_defense_survival" -> "satisfied"),
            scoreRequirement = Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtMost(-100))
          )
        )
      ),
      "centipawn"
    )

    val cpResult =
      CertificationEngineRuntimeIntake.forObjectExtraction(
        current = current,
        expectedNode = node,
        expectedFen = exactFen,
        packet = Some(
          runtimePacket(
            score = CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(180),
            scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.SideToMove,
            scoreRequirement = CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtMost(-100)
          )
        ),
        nowEpochMs = 12_000L
      )
    assertEquals(cpResult.status, CertificationEngineRuntimeIntake.Status.Accepted)

  test("optional bounded Engine E evidence integrates with certification extraction without breaking empty path"):
    val emptyExtraction =
      CertificationEngineRuntimeIntake
        .extractCertificationsForObject(
          current = current,
          baseEvidence = CertificationEvidenceBundle.empty,
          expectedNode = node,
          expectedFen = exactFen,
          packet = None,
          nowEpochMs = 12_000L
        )
        .fold(message => fail(message), identity)

    assertEquals(emptyExtraction.evidenceBundle, CertificationEvidenceBundle.empty)

    val withEngine =
      CertificationEngineRuntimeIntake
        .extractCertificationsForObject(
          current = current,
          baseEvidence = CertificationEvidenceBundle.empty,
          expectedNode = node,
          expectedFen = exactFen,
          packet = Some(runtimePacket()),
          nowEpochMs = 12_000L
        )
        .fold(message => fail(message), identity)

    assert(
      withEngine.evidenceBundle
        .evidenceFor(CertificationId("CertifiedKingSafetyEdge"), Color.White, WitnessAnchor.BoardAnchor)
        .nonEmpty,
      clues(withEngine)
    )

  test("runtime eval swing is admitted only with a bound fresh before-position baseline"):
    val delta =
      StrategicDeltaExtractor
        .fromFensFailClosed(deltaBeforeFen, move("d6c7"), deltaAfterFen)
        .fold(message => fail(message), identity)
    val transitionNode = EngineNodeIdentity("mainline:transition", 2)

    val result =
      CertificationEngineRuntimeIntake.forDeltaExtraction(
        delta = delta,
        expectedNode = transitionNode,
        expectedBeforeFen = deltaBeforeFen,
        expectedAfterFen = deltaAfterFen,
        packet = Some(runtimeTransitionPacket(transitionNode)),
        nowEpochMs = 12_000L
      )
    assertEquals(result.status, CertificationEngineRuntimeIntake.Status.Accepted)

    val staleBaseline =
      runtimeTransitionPacket(transitionNode).copy(
        baseline = runtimeTransitionPacket(transitionNode).baseline.map(b =>
          b.copy(search = b.search.copy(generatedAtEpochMs = 1L))
        )
      )
    val staleResult =
      CertificationEngineRuntimeIntake.forDeltaExtraction(
        delta,
        transitionNode,
        deltaBeforeFen,
        deltaAfterFen,
        Some(staleBaseline),
        12_000L
      )
    assertEquals(staleResult.status, CertificationEngineRuntimeIntake.Status.Rejected)
    assert(staleResult.reason.exists(_.contains("baseline is stale")), clues(staleResult))

    val wrongBaselineNode =
      runtimeTransitionPacket(transitionNode).copy(
        baseline = runtimeTransitionPacket(transitionNode).baseline.map(b => b.copy(nodeId = "side:before"))
      )
    val wrongNodeResult =
      CertificationEngineRuntimeIntake.forDeltaExtraction(
        delta,
        transitionNode,
        deltaBeforeFen,
        deltaAfterFen,
        Some(wrongBaselineNode),
        12_000L
      )
    assertEquals(wrongNodeResult.status, CertificationEngineRuntimeIntake.Status.Rejected)
    assert(wrongNodeResult.reason.exists(_.contains("baseline node identity")), clues(wrongNodeResult))

    val wrongEngineConfig =
      runtimeTransitionPacket(transitionNode).copy(
        baseline = runtimeTransitionPacket(transitionNode).baseline.map(b =>
          b.copy(search = b.search.copy(engineConfigFingerprint = "wasm_stockfish:depth=20:multipv=1"))
        )
      )
    val wrongConfigResult =
      CertificationEngineRuntimeIntake.forDeltaExtraction(
        delta,
        transitionNode,
        deltaBeforeFen,
        deltaAfterFen,
        Some(wrongEngineConfig),
        12_000L
      )
    assertEquals(wrongConfigResult.status, CertificationEngineRuntimeIntake.Status.Rejected)
    assert(wrongConfigResult.reason.exists(_.contains("engine config fingerprint")), clues(wrongConfigResult))

  test("EngineEvidence has no direct Sxx projection admission path"):
    val seed =
      StrategySupportSeedExtractor
        .fromFenFailClosed(exactFen)
        .fold(message => fail(message), identity)
    val engineOnly =
      CertificationEngineRuntimeIntake
        .forObjectExtraction(current, node, exactFen, Some(runtimePacket()), 12_000L)
        .evidence
        .asBundle

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S01"),
        seed,
        StrategyProjectionEvidence.empty,
        Color.White,
        engineOnly
      ),
      Right(false)
    )

    val mainProjectionSources =
      sourceFilesUnder("modules/commentary/src/main/scala/lila/commentary/projection")
    val joinedMainProjectionSources =
      mainProjectionSources.map(path => Files.readString(path, StandardCharsets.UTF_8)).mkString("\n")
    assert(
      !rawAdmissionObjectPattern.findFirstIn(joinedMainProjectionSources).isDefined,
      clues("legacy raw admission must stay out of main")
    )
    assert(!joinedMainProjectionSources.contains("StrategyProjectionAdmission.admit"), clues("main projection must not call legacy raw admission"))
    assert(!joinedMainProjectionSources.contains("StrategyProjectionAdmission.admits"), clues("main projection must not call legacy raw admission"))
    assert(!joinedMainProjectionSources.contains("EngineEvidencePacket"), clues("projection must not consume raw Engine E packets"))
    assert(!joinedMainProjectionSources.contains("CertificationEngineEvidence"), clues("projection must not consume Engine E facade"))

  test("UI API and validation probe sidecars do not reference certification runtime intake"):
    val rawSidecarSources =
      sourceFilesUnder("app/controllers") ++
        sourceFilesUnder("ui/analyse/src") ++
        sourceFilesUnder("modules/commentary/src/test/scala/lila/commentary/validation")
    val joined = rawSidecarSources.map(path => Files.readString(path, StandardCharsets.UTF_8)).mkString("\n")

    Vector(
      "CertificationEngineRuntimeIntake",
      "RuntimeEnginePacket",
      "EngineEvidencePacket",
      "CertificationEngineEvidenceContract"
    ).foreach: forbidden =>
      assert(!joined.contains(forbidden), clues(s"raw sidecar path must not reference $forbidden"))

  private def runtimePacket(
      fen: String = exactFen.toString,
      nodeId: String = node.nodeId,
      ply: Int = node.ply,
      requestedDepth: Int = 20,
      realizedDepth: Int = 20,
      multiPv: Int = 3,
      pvLines: Vector[Vector[String]] = Vector(
        Vector("g8f6", "f3g5"),
        Vector("d7d6", "f3g5"),
        Vector("e5d4", "f3d4")
      ),
      score: CertificationEngineRuntimeIntake.RuntimeScore =
        CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(180),
      scorePerspective: CertificationEngineRuntimeIntake.RuntimeScorePerspective =
        CertificationEngineRuntimeIntake.RuntimeScorePerspective.SideToMove,
      scoreRequirement: CertificationEngineRuntimeIntake.RuntimeScoreRequirement =
        CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtMost(-100)
  ): CertificationEngineRuntimeIntake.RuntimeEnginePacket =
    CertificationEngineRuntimeIntake.RuntimeEnginePacket(
      fen = fen,
      nodeId = nodeId,
      ply = ply,
      requestedDepth = requestedDepth,
      realizedDepth = realizedDepth,
      multiPv = multiPv,
      completed = true,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 5_000L,
      engineConfigFingerprint = "wasm_stockfish:depth=20:multipv=3",
      score = score,
      scorePerspective = scorePerspective,
      pvLines = pvLines,
      claims = Vector(
        runtimeClaim(scoreRequirement = Some(scoreRequirement))
      )
    )

  private def runtimeClaim(
      familyId: String = "CertifiedKingSafetyEdge",
      owner: String = "white",
      purposes: Map[String, String] = Map(
        "best_defense_survival" -> "satisfied"
      ),
      scoreRequirement: Option[CertificationEngineRuntimeIntake.RuntimeScoreRequirement] =
        Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtMost(-100)),
      minDepth: Int = 18,
      minPvPlies: Int = 2,
      probeRequestId: Option[String] = None,
      probePolicyFingerprint: Option[String] = None,
      roleReports: Option[Map[String, String]] = None,
      publicCaps: Option[Vector[String]] = None
  ): CertificationEngineRuntimeIntake.RuntimeCertificationClaim =
    CertificationEngineRuntimeIntake.RuntimeCertificationClaim(
      familyId = familyId,
      owner = owner,
      purposes = purposes,
      minDepth = minDepth,
      minMultiPv = 3,
      minPvPlies = minPvPlies,
      requiredScore = scoreRequirement,
      probeRequestId = probeRequestId.orElse(Some(runtimeQRequestId(familyId, owner, purposes, node.nodeId, node.ply))),
      probePolicyFingerprint = probePolicyFingerprint.orElse(
        Some(runtimeQPolicyFingerprint("wasm_stockfish:depth=20:multipv=3", purposes))
      ),
      roleReports = roleReports.orElse(Some(purposes)),
      publicCaps = publicCaps
    )

  private def runtimeTransitionPacket(
      transitionNode: EngineNodeIdentity
  ): CertificationEngineRuntimeIntake.RuntimeEnginePacket =
    CertificationEngineRuntimeIntake.RuntimeEnginePacket(
      fen = deltaAfterFen.toString,
      nodeId = transitionNode.nodeId,
      ply = transitionNode.ply,
      requestedDepth = 20,
      realizedDepth = 20,
      multiPv = 3,
      completed = true,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 5_000L,
      engineConfigFingerprint = "wasm_stockfish:depth=20:multipv=3",
      score = CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(-300),
      scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.SideToMove,
      pvLines = Vector(
        Vector("e8d7", "e2d3"),
        Vector("e8f7", "e2d3"),
        Vector("e8f8", "e2d3")
      ),
      claims = Vector(
        CertificationEngineRuntimeIntake.RuntimeCertificationClaim(
          familyId = "MaterialHarvest",
          owner = "white",
          purposes = Map("best_defense_survival" -> "satisfied"),
          minDepth = 18,
          minMultiPv = 3,
          minPvPlies = 2,
          requiredScore = Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnSwingAtLeast(200)),
          probeRequestId = Some(runtimeQRequestId("MaterialHarvest", "white", Map("best_defense_survival" -> "satisfied"), transitionNode.nodeId, transitionNode.ply)),
          probePolicyFingerprint = Some(
            CertificationEnginePolicyFingerprint.defaultForRole(
              "wasm_stockfish:depth=20:multipv=3",
              CertificationEngineRole.BestDefenseSurvival
            )
          ),
          roleReports = Some(
            Map(
              "best_defense_survival" -> "satisfied"
            )
          )
        )
      ),
      transition = Some(
        CertificationEngineRuntimeIntake.RuntimeTransitionBinding(
          deltaBeforeFen.toString,
          "d6c7",
          deltaAfterFen.toString,
          beforeNodeId = Some("mainline:before"),
          beforePly = Some(1)
        )
      ),
      baseline = Some(
        CertificationEngineRuntimeIntake.RuntimeBaselinePacket(
          fen = deltaBeforeFen.toString,
          nodeId = "mainline:before",
          ply = 1,
          search = CertificationEngineRuntimeIntake.RuntimeSearchState(
            requestedDepth = 20,
            realizedDepth = 20,
            multiPv = 3,
            completed = true,
            generatedAtEpochMs = 10_000L,
            maxAgeMs = 5_000L,
            engineConfigFingerprint = "wasm_stockfish:depth=20:multipv=3"
          ),
          score = CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(50),
          scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.SideToMove,
          pvLines = Vector(
            Vector("d6d7", "e8d7"),
            Vector("d6c7", "e8d7"),
            Vector("e2d3", "e8d7")
          )
        )
      )
    )

  private def move(uci: String): Uci.Move =
    Uci(uci) match
      case Some(move: Uci.Move) => move
      case Some(_) => fail(s"unsupported non-move UCI $uci")
      case None => fail(s"invalid UCI $uci")

  private def deltaExtraction() =
    StrategicDeltaExtractor
      .fromFensFailClosed(deltaBeforeFen, move("d6c7"), deltaAfterFen)
      .fold(message => fail(message), identity)

  private def assertRejected(packet: CertificationEngineRuntimeIntake.RuntimeEnginePacket, expected: String): Unit =
    val result =
      CertificationEngineRuntimeIntake.forObjectExtraction(current, node, exactFen, Some(packet), 12_000L)
    assertEquals(result.status, CertificationEngineRuntimeIntake.Status.Rejected)
    assert(result.reason.exists(_.contains(expected)), clues(result))
    assertEquals(result.evidence, CertificationEngineEvidence.empty)

  private def sourceFilesUnder(path: String): Vector[java.nio.file.Path] =
    val root = Paths.get(path)
    if !Files.exists(root) then Vector.empty
    else
      val stream = Files.walk(root)
      try
        stream
          .filter(Files.isRegularFile(_))
          .filter(path =>
            path.toString.endsWith(".scala") ||
              path.toString.endsWith(".ts") ||
              path.toString.endsWith(".tsx")
          )
          .toArray
          .toVector
          .collect { case path: java.nio.file.Path => path }
      finally stream.close()

  private val rawAdmissionObjectPattern =
    "(?m)^\\s*object\\s+StrategyProjectionAdmission\\s*:".r

  private def runtimeQRequestId(
      familyId: String,
      owner: String,
      purposes: Map[String, String],
      nodeId: String,
      ply: Int
  ): String =
    val role = purposes.keys.toVector.sorted.headOption.getOrElse("best_defense_survival")
    Vector(
      "q",
      stableToken(role),
      stableToken(familyId),
      stableToken(owner),
      "board",
      stableToken(nodeId),
      ply.toString
    ).mkString("-")

  private def runtimeQPolicyFingerprint(
      engineConfigFingerprint: String,
      purposes: Map[String, String]
  ): String =
    purposes.keys.toVector.sorted.headOption
      .flatMap(CertificationEngineRole.fromKey)
      .map(role => CertificationEnginePolicyFingerprint.defaultForRole(engineConfigFingerprint, role))
      .getOrElse("qpolicy-unknown-runtime-role")

  private def stableToken(value: String): String =
    value
      .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
      .replaceAll("[^A-Za-z0-9]+", "-")
      .stripPrefix("-")
      .stripSuffix("-")
      .toLowerCase
