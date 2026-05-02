package lila.commentary.certification

import chess.Color
import chess.format.{ Fen, Uci }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

import lila.commentary.delta.StrategicDeltaExtractor
import lila.commentary.strategic.StrategicObjectExtractor
import lila.commentary.witness.WitnessAnchor

class CertificationEngineEvidenceContractTest extends munit.FunSuite:

  private val exactFen =
    Fen.Full.clean("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3")
  private val nearMissFen =
    Fen.Full.clean("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 2 3")
  private val deltaBeforeFen =
    Fen.Full.clean("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1")
  private val deltaAfterFen =
    Fen.Full.clean("4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1")

  private val current =
    StrategicObjectExtractor.fromFenFailClosed(exactFen).fold(message => fail(message), identity)
  private val nearMiss =
    StrategicObjectExtractor.fromFenFailClosed(nearMissFen).fold(message => fail(message), identity)
  private val node = EngineNodeIdentity("mainline:3b", 5)
  private val engineFingerprint = "stockfish:depth=20:multipv=3"

  test("Engine packet admits bounded certification evidence only when exact FEN, node, freshness, score, and PV laws pass"):
    val packet =
      basePacket().copy(
        claims = Vector(bestDefenseClaim(CertificationEvidenceStrength.Satisfied))
      )

    val evidence =
      CertificationEngineEvidenceContract
        .forObjectExtraction(
          current = current,
          expectedNode = node,
          expectedFen = exactFen,
          packet = packet,
          nowEpochMs = 12_000L
        )
        .fold(message => fail(message), identity)

    val claim =
      evidence
        .evidenceFor(CertificationId("CertifiedKingSafetyEdge"), Color.White, WitnessAnchor.BoardAnchor)
        .getOrElse(fail("missing certified king safety edge evidence"))

    assert(
      claim.strengthFor(CertificationEvidencePurpose.BestDefenseSurvival) ==
        Some(CertificationEvidenceStrength.Satisfied),
      clues(claim)
    )

  test("Engine packet rejects stale, wrong-node, wrong-FEN, shallow, and insufficient MultiPV evidence"):
    val packet = basePacket().copy(claims = Vector(bestDefenseClaim(CertificationEvidenceStrength.Satisfied)))

    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(current, EngineNodeIdentity("side:5", 5), exactFen, packet, 12_000L),
      "node identity"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(nearMiss, node, exactFen, packet, 12_000L),
      "same exact FEN"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(current, node, exactFen, packet, 20_001L),
      "stale"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(search = packet.search.copy(realizedDepth = 17)),
        12_000L
      ),
      "depth"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(search = packet.search.copy(multiPv = 1), pvLines = packet.pvLines.take(1)),
        12_000L
      ),
      "MultiPV"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(
          claims = Vector(bestDefenseClaim(CertificationEvidenceStrength.Satisfied).copy(minMultiPv = 2)),
          search = packet.search.copy(multiPv = 2),
          pvLines = packet.pvLines.take(2)
        ),
        12_000L
      ),
      "MultiPV 3"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        Fen.Full.clean(exactFen.toString.replace(" 3 3", " 4 3")),
        packet,
        12_000L
      ),
      "exact FEN"
    )

  test("Engine packet rejects illegal or truncated PVs before creating certification evidence"):
    val packet = basePacket().copy(claims = Vector(bestDefenseClaim(CertificationEvidenceStrength.Satisfied)))
    val illegalPv = Vector(move("e2e4"), move("g8f6"))
    val truncatedPv = Vector(move("g8f6"))
    val duplicateFirstMovePv = Vector(Vector(move("g8f6"), move("f3g5")), Vector(move("g8f6"), move("f3g5")), packet.pvLines(2))

    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(pvLines = Vector(illegalPv, packet.pvLines(1), packet.pvLines(2))),
        12_000L
      ),
      "illegal PV"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(pvLines = Vector(truncatedPv, packet.pvLines(1), packet.pvLines(2))),
        12_000L
      ),
      "truncated PV"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(pvLines = duplicateFirstMovePv),
        12_000L
      ),
      "distinct first moves"
    )

  test("Engine score normalization keeps mate separate from centipawns and refuses mate-as-cp conversion"):
    val cpPacket = basePacket(score = EngineScore.Centipawns(180))
    assert(cpPacket.normalizedCentipawnsFor(Color.White) == Some(-180), clues(cpPacket))
    assert(cpPacket.normalizedCentipawnsFor(Color.Black) == Some(180), clues(cpPacket))

    val matePacket = basePacket(score = EngineScore.MateIn(3))
    assert(matePacket.normalizedCentipawnsFor(Color.White).isEmpty, clues(matePacket))
    assert(matePacket.normalizedMateFor(Color.Black) == Some(3), clues(matePacket))

    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        matePacket.copy(
          claims = Vector(
            bestDefenseClaim(CertificationEvidenceStrength.Satisfied).copy(
              familyId = CertificationId("MateNetCertification"),
              probeRequestId = Some(engineQRequestId("MateNetCertification", Color.White, WitnessAnchor.BoardAnchor, CertificationEngineRole.BestDefenseSurvival, node))
            )
          )
        ),
        12_000L
      ),
      "centipawn"
    )

  test("Engine transition binding must match beforeFen, playedMove, and afterFen for delta-backed evidence"):
    val delta =
      StrategicDeltaExtractor
        .fromFensFailClosed(deltaBeforeFen, move("d6c7"), deltaAfterFen)
        .fold(message => fail(message), identity)
    val after = delta.after
    val transitionNode = EngineNodeIdentity("mainline:transition", 2)
    val transitionPacket =
      basePacket(
        fen = deltaAfterFen.toString,
        node = transitionNode,
        score = EngineScore.Centipawns(-180),
        pv = Vector(
          Vector(move("e8d7"), move("e2d3")),
          Vector(move("e8f7"), move("e2d3")),
          Vector(move("e8f8"), move("e2d3"))
        )
      ).copy(
        identity = EnginePacketIdentity(
          fen = deltaAfterFen.toString,
          node = transitionNode,
          transition = Some(EngineTransitionBinding(deltaBeforeFen.toString, "d6c7", deltaAfterFen.toString))
        ),
        claims = Vector(
          EngineCertificationClaim(
            familyId = CertificationId("MaterialHarvest"),
            owner = Color.White,
            purposes = Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied),
            minDepth = 18,
            minMultiPv = 3,
            minPvPlies = 2,
            requiredScore = Some(EngineScoreRequirement.CentipawnAtLeast(100)),
            probeRequestId = Some(engineQRequestId("MaterialHarvest", Color.White, WitnessAnchor.BoardAnchor, CertificationEngineRole.BestDefenseSurvival, transitionNode)),
            probePolicyFingerprint = Some(engineQPolicyFingerprint(engineFingerprint, CertificationEngineRole.BestDefenseSurvival)),
            roleReports = Map(
              CertificationEngineRole.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
            )
          )
        )
      )

    val evidence =
      CertificationEngineEvidenceContract
        .forDeltaExtraction(delta, transitionNode, deltaBeforeFen, deltaAfterFen, transitionPacket, 12_000L)
        .fold(message => fail(message), identity)

    assert(evidence.evidenceFor(CertificationId("MaterialHarvest"), Color.White).nonEmpty, clues(evidence))

    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(after, transitionNode, deltaAfterFen, transitionPacket, 12_000L),
      "transition-bound"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forDeltaExtraction(
        delta,
        transitionNode,
        deltaBeforeFen,
        deltaAfterFen,
        transitionPacket.copy(
          identity = transitionPacket.identity.copy(
            transition = Some(EngineTransitionBinding(deltaBeforeFen.toString, "d6d7", deltaAfterFen.toString))
          )
        ),
        12_000L
      ),
      "playedMove"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forDeltaExtraction(
        delta,
        transitionNode,
        deltaBeforeFen,
        deltaAfterFen,
        transitionPacket.copy(
          identity = transitionPacket.identity.copy(
            transition = Some(EngineTransitionBinding(exactFen.toString, "d6c7", deltaAfterFen.toString))
          )
        ),
        12_000L
      ),
      "beforeFen"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forDeltaExtraction(
        delta,
        transitionNode,
        deltaBeforeFen,
        deltaAfterFen,
        transitionPacket.copy(
          identity = transitionPacket.identity.copy(
            transition = Some(EngineTransitionBinding(deltaBeforeFen.toString, "d6c7", exactFen.toString))
          )
        ),
        12_000L
      ),
      "afterFen"
    )

  test("Raw engine eval or PV without bounded certification claims stays fail-closed empty"):
    val evidence =
      CertificationEngineEvidenceContract
        .forObjectExtraction(current, node, exactFen, basePacket(), 12_000L)
        .fold(message => fail(message), identity)

    assert(evidence == CertificationEvidenceBundle.empty, clues(evidence))
    assert(CertificationEngineEvidence.fromProbe(basePacket()) == CertificationEvidenceBundle.empty, clues(evidence))

  test("Engine packet rejects scoreless claims, duplicate claims, and unbound eval swing"):
    val claim = bestDefenseClaim(CertificationEvidenceStrength.Satisfied)
    val packet = basePacket().copy(claims = Vector(claim))

    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(current, node, exactFen, packet.copy(claims = Vector(claim.copy(requiredScore = None))), 12_000L),
      "score requirement"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(current, node, exactFen, packet.copy(claims = Vector(claim, claim)), 12_000L),
      "duplicate"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(claims = Vector(claim.copy(requiredScore = Some(EngineScoreRequirement.CentipawnSwingAtLeast(100))))),
        12_000L
      ),
      "bound baseline"
    )

  test("Engine role claims require server-issued Q request identity and matching role policy"):
    val claim = bestDefenseClaim(CertificationEvidenceStrength.Satisfied)
    val packet = basePacket().copy(claims = Vector(claim))

    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(claims = Vector(claim.copy(probeRequestId = None))),
        12_000L
      ),
      "Q request id"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(claims = Vector(claim.copy(probeRequestId = Some("not-q-best-defense")))),
        12_000L
      ),
      "Q request id"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(claims = Vector(claim.copy(probeRequestId = Some("q-comparative-superiority-certified-king-safety-edge-white-board")))),
        12_000L
      ),
      "Q request role"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(claims = Vector(claim.copy(probeRequestId = Some("q-best-defense-survival-forged-family-white-board-mainline-3b-5")))),
        12_000L
      ),
      "Q request role identity"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(claims = Vector(claim.copy(probePolicyFingerprint = Some("other-engine-policy")))),
        12_000L
      ),
      "policy fingerprint"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(claims = Vector(claim.copy(probePolicyFingerprint = Some(engineFingerprint)))),
        12_000L
      ),
      "Q policy fingerprint"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forObjectExtraction(
        current,
        node,
        exactFen,
        packet.copy(
          search = packet.search.copy(requestedDepth = 1, realizedDepth = 1),
          claims = Vector(claim.copy(minDepth = 1, minPvPlies = 1))
        ),
        12_000L
      ),
      "Q policy target depth"
    )

  test("Engine eval swing requires a bound fresh before-position baseline for delta evidence"):
    val delta =
      StrategicDeltaExtractor
        .fromFensFailClosed(deltaBeforeFen, move("d6c7"), deltaAfterFen)
        .fold(message => fail(message), identity)
    val transitionNode = EngineNodeIdentity("mainline:transition", 2)
    val transitionPacket =
      basePacket(
        fen = deltaAfterFen.toString,
        node = transitionNode,
        score = EngineScore.Centipawns(-300),
        pv = Vector(
          Vector(move("e8d7"), move("e2d3")),
          Vector(move("e8f7"), move("e2d3")),
          Vector(move("e8f8"), move("e2d3"))
        )
      ).copy(
        identity = EnginePacketIdentity(
          fen = deltaAfterFen.toString,
          node = transitionNode,
          transition = Some(EngineTransitionBinding(deltaBeforeFen.toString, "d6c7", deltaAfterFen.toString))
        ),
        claims = Vector(
          EngineCertificationClaim(
            familyId = CertificationId("MaterialHarvest"),
            owner = Color.White,
            purposes = Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied),
            minDepth = 18,
            minMultiPv = 3,
            minPvPlies = 2,
            requiredScore = Some(EngineScoreRequirement.CentipawnSwingAtLeast(200)),
            probeRequestId = Some(engineQRequestId("MaterialHarvest", Color.White, WitnessAnchor.BoardAnchor, CertificationEngineRole.BestDefenseSurvival, transitionNode)),
            probePolicyFingerprint = Some(engineQPolicyFingerprint(engineFingerprint, CertificationEngineRole.BestDefenseSurvival)),
            roleReports = Map(
              CertificationEngineRole.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
            )
          )
        ),
        baseline = Some(
          EngineBaselinePacket(
            identity = EnginePacketIdentity(deltaBeforeFen.toString, EngineNodeIdentity("mainline:before", 1)),
            search = EngineSearchState(
              requestedDepth = 20,
              realizedDepth = 20,
              multiPv = 3,
              completed = true,
              generatedAtEpochMs = 10_000L,
              maxAgeMs = 5_000L,
              engineConfigFingerprint = engineFingerprint
            ),
            score = EngineScore.Centipawns(50),
            scorePerspective = EngineScorePerspective.SideToMove,
            pvLines = Vector(
              Vector(move("d6d7"), move("e8d7")),
              Vector(move("d6c7"), move("e8d7")),
              Vector(move("e2d3"), move("e8d7"))
            )
          )
        )
      )

    val evidence =
      CertificationEngineEvidenceContract
        .forDeltaExtraction(delta, transitionNode, deltaBeforeFen, deltaAfterFen, transitionPacket, 12_000L)
        .fold(message => fail(message), identity)
    assert(evidence.evidenceFor(CertificationId("MaterialHarvest"), Color.White).nonEmpty, clues(evidence))

    assertLeftContains(
      CertificationEngineEvidenceContract.forDeltaExtraction(
        delta,
        transitionNode,
        deltaBeforeFen,
        deltaAfterFen,
        transitionPacket.copy(baseline = transitionPacket.baseline.map(b => b.copy(search = b.search.copy(generatedAtEpochMs = 1L)))),
        12_000L
      ),
      "baseline is stale"
    )
    assertLeftContains(
      CertificationEngineEvidenceContract.forDeltaExtraction(
        delta,
        transitionNode,
        deltaBeforeFen,
        deltaAfterFen,
        transitionPacket.copy(
          baseline = transitionPacket.baseline.map(b => b.copy(identity = b.identity.copy(fen = exactFen.toString)))
        ),
        12_000L
      ),
      "baseline FEN"
    )

  test("Engine E freeze is documented as certification-only bounded evidence"):
    val docs =
      Vector(
        "modules/commentary/docs/CommentaryCoreSSOT.md",
        "modules/commentary/docs/ValidationMethodology.md",
        "modules/commentary/docs/Witnesses61.md",
        "modules/commentary/docs/LegacyFailureTaxonomy.md"
      ).map(readDoc).mkString("\n")

    Vector(
      "Engine E certification evidence freeze",
      "12 active certification families",
      "Engine E is certification evidence only",
      "Root/U/Object/Delta/Sxx truth owner",
      "exact FEN, node identity, freshness, depth, MultiPV, score, and legal-PV checks",
      "CertificationEngineEvidenceContract",
      "CertificationEngineRuntimeIntake",
      "UI/API raw probe sidecars are not certification evidence",
      "opaque Engine E evidence facade",
      "normalized full-FEN string",
      "server-shaped",
      "Q policy fingerprint"
    ).foreach: token =>
      assert(docs.contains(token), clues(s"missing doc token: $token"))

  private def basePacket(
      fen: String = exactFen.toString,
      node: EngineNodeIdentity = node,
      score: EngineScore = EngineScore.Centipawns(180),
      pv: Vector[Vector[Uci.Move]] = Vector(
        Vector(move("g8f6"), move("f3g5")),
        Vector(move("d7d6"), move("f3g5")),
        Vector(move("e5d4"), move("f3d4"))
      )
  ): EngineEvidencePacket =
    EngineEvidencePacket(
      identity = EnginePacketIdentity(
        fen = fen,
        node = node,
        transition = None
      ),
      search = EngineSearchState(
        requestedDepth = 20,
        realizedDepth = 20,
        multiPv = 3,
        completed = true,
        generatedAtEpochMs = 10_000L,
        maxAgeMs = 5_000L,
        engineConfigFingerprint = engineFingerprint
      ),
      score = score,
      scorePerspective = EngineScorePerspective.SideToMove,
      pvLines = pv,
      claims = Vector.empty
    )

  private def bestDefenseClaim(strength: CertificationEvidenceStrength): EngineCertificationClaim =
    EngineCertificationClaim(
      familyId = CertificationId("CertifiedKingSafetyEdge"),
      owner = Color.White,
      purposes = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> strength
      ),
      minDepth = 18,
      minMultiPv = 3,
      minPvPlies = 2,
      requiredScore = Some(EngineScoreRequirement.CentipawnAtMost(-100)),
      probeRequestId = Some(engineQRequestId("CertifiedKingSafetyEdge", Color.White, WitnessAnchor.BoardAnchor, CertificationEngineRole.BestDefenseSurvival, node)),
      probePolicyFingerprint = Some(engineQPolicyFingerprint(engineFingerprint, CertificationEngineRole.BestDefenseSurvival)),
      roleReports = Map(
        CertificationEngineRole.BestDefenseSurvival -> strength
      )
    )

  private def move(uci: String): Uci.Move =
    Uci(uci) match
      case Some(move: Uci.Move) => move
      case Some(_) => fail(s"unsupported non-move UCI $uci")
      case None => fail(s"invalid UCI $uci")

  private def assertLeftContains(result: Either[String, ?], expected: String): Unit =
    result match
      case Left(message) => assert(message.contains(expected), clues(message))
      case Right(value) => fail(s"Expected Left containing '$expected', got $value")

  private def engineQRequestId(
      familyId: String,
      owner: Color,
      anchor: WitnessAnchor,
      role: CertificationEngineRole,
      node: EngineNodeIdentity
  ): String =
    Vector(
      "q",
      stableToken(role.key),
      stableToken(familyId),
      if owner.white then "white" else "black",
      stableToken(anchor.key),
      stableToken(node.nodeId),
      node.ply.toString
    ).mkString("-")

  private def engineQPolicyFingerprint(
      engineFingerprint: String,
      role: CertificationEngineRole
  ): String =
    CertificationEnginePolicyFingerprint.defaultForRole(engineFingerprint, role)

  private def stableToken(value: String): String =
    value
      .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
      .replaceAll("[^A-Za-z0-9]+", "-")
      .stripPrefix("-")
      .stripSuffix("-")
      .toLowerCase

  private def readDoc(path: String): String =
    Files.readString(Paths.get(path), StandardCharsets.UTF_8)
