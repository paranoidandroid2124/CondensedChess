package lila.commentary

import lila.commentary.analysis.PlanStateTracker
import lila.commentary.model.{ ExplorerGame, ExplorerMove, ExplorerPlayer, FutureSnapshot, L1DeltaSnapshot, OpeningReference, ProbeResult, TargetsDelta }
import lila.commentary.model.strategic.{ EndgamePatternState, TheoreticalOutcomeHint }
import munit.FunSuite

import scala.concurrent.ExecutionContext

class CommentaryCacheTest extends FunSuite:

  given Executor = ExecutionContext.global

  private val response =
    CommentResponse(
      commentary = "Cached response",
      concepts = Nil
    )

  private val probeResults =
    List(
      ProbeResult(
        id = "probe-b",
        fen = Some("fen-b"),
        evalCp = 42,
        bestReplyPv = List("e7e5", "g1f3", "b8c6", "f1b5", "a7a6"),
        deltaVsBaseline = 12,
        keyMotifs = List("central"),
        purpose = Some("support"),
        probedMove = Some("e2e4"),
        depth = Some(18)
      ),
      ProbeResult(
        id = "probe-a",
        fen = Some("fen-a"),
        evalCp = -11,
        bestReplyPv = List("d7d5", "c2c4", "e7e6", "b1c3"),
        deltaVsBaseline = -3,
        keyMotifs = List("space"),
        purpose = Some("refute"),
        probedMove = Some("d2d4"),
        mate = Some(0),
        depth = Some(20)
      )
    )

  private val baseFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
  private val baseLastMove = Some("e2e4")

  private val baseContext =
    CommentaryCacheContext(
      model = "gpt-test",
      promptVersion = "move-review-v1",
      lang = "en",
      planTier = "basic",
      commentaryMode = "polish",
      authorityFingerprint = "authority-v1"
    )

  private val endgameState =
    EndgamePatternState(
      activePattern = Some("rook_activity"),
      patternAge = 1,
      outcomeHint = TheoreticalOutcomeHint.Draw,
      prevKingActivityDelta = 1,
      prevConfidence = 0.67,
      lastPly = 44
    )

  test("prebuilt cache keys can be reused across get and put") {
    val cache = CommentaryCache()
    val key =
      cache.key(
        fen = baseFen,
        lastMove = baseLastMove,
        probeResults = probeResults,
        planStateToken = None,
        endgameStateToken = None,
        commentaryContext = Some(baseContext)
      )

    assertEquals(cache.get(key), None, clue(key))
    cache.put(key, response)
    assertEquals(cache.get(key).map(_.commentary), Some("Cached response"), clue(key))
  }

  test("probe-aware cache keys use compact non-SHA fingerprints") {
    val cache = CommentaryCache()
    val key =
      cache.key(
        fen = "fen",
        lastMove = Some("e2e4"),
        probeResults = probeResults,
        planStateToken = None,
        endgameStateToken = None,
        commentaryContext = None
      )

    assert(!key.value.matches(""".*probe:[0-9a-f]{40}.*"""), clue(key.value))
  }

  test("probe-aware public cache APIs are stable across probe order") {
    val cache = CommentaryCache()
    cache.put(
      fen = baseFen,
      lastMove = baseLastMove,
      response = response,
      probeResults = probeResults,
      planStateToken = None,
      endgameStateToken = None,
      commentaryContext = Some(baseContext)
    )

    val hit =
      cache.get(
        fen = baseFen,
        lastMove = baseLastMove,
        probeResults = probeResults.reverse,
        planStateToken = None,
        endgameStateToken = None,
        commentaryContext = Some(baseContext)
      )

    assertEquals(hit.map(_.commentary), Some("Cached response"), clue(hit))
  }

  test("probe-aware public cache APIs separate changed probe content") {
    val cache = CommentaryCache()
    cache.put(baseFen, baseLastMove, response, probeResults, None, None, Some(baseContext))

    val changedEval = probeResults.updated(0, probeResults.head.copy(evalCp = probeResults.head.evalCp + 1))
    val changedDepth = probeResults.updated(0, probeResults.head.copy(depth = Some(99)))
    val changedPv = probeResults.updated(0, probeResults.head.copy(bestReplyPv = List("a7a6", "a2a4")))
    val changedPurpose = probeResults.updated(0, probeResults.head.copy(purpose = Some("changed-purpose")))

    assertEquals(cache.get(baseFen, baseLastMove, changedEval, None, None, Some(baseContext)), None, clue(changedEval))
    assertEquals(cache.get(baseFen, baseLastMove, changedDepth, None, None, Some(baseContext)), None, clue(changedDepth))
    assertEquals(cache.get(baseFen, baseLastMove, changedPv, None, None, Some(baseContext)), None, clue(changedPv))
    assertEquals(cache.get(baseFen, baseLastMove, changedPurpose, None, None, Some(baseContext)), None, clue(changedPurpose))
  }

  test("probe-aware public cache APIs separate downstream-consumed probe fields") {
    val cache = CommentaryCache()
    val richProbe =
      probeResults.head.copy(
        replyPvs = Some(List(List("e7e5", "g1f3"), List("c7c5", "g1f3"))),
        deltaVsBaseline = 12,
        keyMotifs = List("central", "king"),
        motifTags = List("forcing"),
        l1Delta = Some(L1DeltaSnapshot(1, 2, 3, 4, 5, Some("king exposed"))),
        futureSnapshot = Some(
          FutureSnapshot(
            resolvedThreatKinds = List("Mate"),
            newThreatKinds = List("Fork"),
            targetsDelta = TargetsDelta(
              tacticalAdded = List("h7"),
              tacticalRemoved = List("e4"),
              strategicAdded = List("d5"),
              strategicRemoved = List("c6")
            ),
            planBlockersRemoved = List("blocker-a"),
            planPrereqsMet = List("prereq-a")
          )
        ),
        objective = Some("validate_latent_plan"),
        seedId = Some("seed-a"),
        requiredSignals = List("replyPvs", "futureSnapshot"),
        generatedRequiredSignals = List("replyPvs"),
        motifInferenceMode = Some("strict"),
        candidateMove = Some("e2e4"),
        depthFloor = Some(18),
        variationHash = Some("vh-a"),
        engineConfigFingerprint = Some("engine-a")
      )
    val richProbes = List(richProbe)
    cache.put(baseFen, baseLastMove, response, richProbes, None, None, Some(baseContext))

    val changedRows =
      List(
        richProbe.copy(replyPvs = Some(List(List("a7a6", "a2a4")))),
        richProbe.copy(replyPvs = Some(List(List("e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4", "g8f6")))),
        richProbe.copy(bestReplyPv = List("e7e5", "g1f3", "b8c6", "f1b5", "h7h6")),
        richProbe.copy(deltaVsBaseline = richProbe.deltaVsBaseline + 1),
        richProbe.copy(keyMotifs = List("different")),
        richProbe.copy(motifTags = List("trade")),
        richProbe.copy(l1Delta = richProbe.l1Delta.map(_.copy(materialDelta = 99))),
        richProbe.copy(futureSnapshot = richProbe.futureSnapshot.map(_.copy(newThreatKinds = List("Skewer")))),
        richProbe.copy(objective = Some("refute_plan")),
        richProbe.copy(seedId = Some("seed-b")),
        richProbe.copy(requiredSignals = List("l1Delta")),
        richProbe.copy(generatedRequiredSignals = List("futureSnapshot")),
        richProbe.copy(motifInferenceMode = Some("relaxed")),
        richProbe.copy(candidateMove = Some("d2d4")),
        richProbe.copy(depthFloor = Some(22)),
        richProbe.copy(variationHash = Some("vh-b")),
        richProbe.copy(engineConfigFingerprint = Some("engine-b"))
      )

    changedRows.foreach { changed =>
      assertEquals(cache.get(baseFen, baseLastMove, List(changed), None, None, Some(baseContext)), None, clue(changed))
    }
  }

  test("cache keys separate opening reference fingerprints") {
    val cache = CommentaryCache()
    val keyA =
      cache.key(
        fen = baseFen,
        lastMove = baseLastMove,
        probeResults = probeResults,
        planStateToken = None,
        endgameStateToken = None,
        commentaryContext = Some(baseContext),
        openingFingerprint = "opening-a"
      )
    val keyB =
      cache.key(
        fen = baseFen,
        lastMove = baseLastMove,
        probeResults = probeResults,
        planStateToken = None,
        endgameStateToken = None,
        commentaryContext = Some(baseContext),
        openingFingerprint = "opening-b"
      )

    cache.put(keyA, response)
    assertEquals(cache.get(keyB), None, clue(s"${keyA.value} vs ${keyB.value}"))
  }

  test("opening reference fingerprints change when explorer evidence changes") {
    val baseOpening =
      OpeningReference(
        eco = Some("C20"),
        name = Some("King's Pawn Game"),
        totalGames = 1000,
        topMoves = List(ExplorerMove("e7e5", "e5", 700, 300, 100, 300, 52)),
        sampleGames = List(
          ExplorerGame(
            id = "game-a",
            winner = None,
            white = ExplorerPlayer("White", 2500),
            black = ExplorerPlayer("Black", 2480),
            year = 2024,
            month = 1,
            event = Some("Masters"),
            pgn = Some("1. e4 e5")
          )
        ),
        description = Some("Open game")
      )

    val changedOpening = baseOpening.copy(topMoves = List(ExplorerMove("c7c5", "c5", 700, 250, 150, 300, 54)))

    assert(
      CommentaryCache.openingFingerprint(Some(baseOpening)) != CommentaryCache.openingFingerprint(Some(changedOpening)),
      clue((CommentaryCache.openingFingerprint(Some(baseOpening)), CommentaryCache.openingFingerprint(Some(changedOpening))))
    )
  }

  test("cache keys separate plan and endgame state tokens") {
    val cache = CommentaryCache()
    cache.put(baseFen, baseLastMove, response, probeResults, None, None, Some(baseContext))

    assertEquals(
      cache.get(baseFen, baseLastMove, probeResults, Some(PlanStateTracker.empty), None, Some(baseContext)),
      None,
      clue(PlanStateTracker.empty.build_fingerprint)
    )
    assertEquals(
      cache.get(baseFen, baseLastMove, probeResults, None, Some(endgameState), Some(baseContext)),
      None,
      clue(endgameState.build_fingerprint)
    )
  }

  test("cache keys separate commentary context dimensions") {
    val cache = CommentaryCache()
    cache.put(baseFen, baseLastMove, response, probeResults, None, None, Some(baseContext))

    val changedContexts =
      List(
        baseContext.copy(model = "other-model"),
        baseContext.copy(promptVersion = "move-review-v2"),
        baseContext.copy(lang = "ko"),
        baseContext.copy(planTier = "pro"),
        baseContext.copy(commentaryMode = "active"),
        baseContext.copy(authorityFingerprint = "authority-v2")
      )

    changedContexts.foreach { ctx =>
      assertEquals(cache.get(baseFen, baseLastMove, probeResults, None, None, Some(ctx)), None, clue(ctx))
    }
  }
