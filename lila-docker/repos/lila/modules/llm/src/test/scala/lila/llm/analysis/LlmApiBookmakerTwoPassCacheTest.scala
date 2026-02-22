package lila.llm.analysis

import munit.FunSuite
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import lila.llm.{ CommentaryCache, CommentResponse }
import lila.llm.model.{ Plan, PlanMatch }
import lila.llm.model.ProbeResult

class LlmApiBookmakerTwoPassCacheTest extends FunSuite {

  given ExecutionContextExecutor = ExecutionContext.global

  private val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val move = Some("e2e4")

  private val probeA = ProbeResult(
    id = "b_probe",
    fen = Some(fen),
    evalCp = 80,
    bestReplyPv = List("e7e5", "g1f3"),
    deltaVsBaseline = 30,
    keyMotifs = Nil,
    purpose = Some("played_move_counterfactual"),
    probedMove = Some("e2e4"),
    depth = Some(18)
  )

  private val probeB = ProbeResult(
    id = "a_probe",
    fen = Some(fen),
    evalCp = 60,
    bestReplyPv = List("c7c5", "g1f3"),
    deltaVsBaseline = 10,
    keyMotifs = Nil,
    purpose = Some("reply_multipv"),
    probedMove = Some("e2e4"),
    depth = Some(18)
  )

  private val stateA = PlanStateTracker.empty.update(
    movingColor = chess.Color.White,
    ply = 5,
    primaryPlan = Some(PlanMatch(Plan.CentralControl(chess.Color.White), 0.7, Nil)),
    secondaryPlan = None
  )
  private val stateB = PlanStateTracker.empty.update(
    movingColor = chess.Color.White,
    ply = 5,
    primaryPlan = Some(PlanMatch(Plan.KingsideAttack(chess.Color.White), 0.7, Nil)),
    secondaryPlan = None
  )

  test("cache keeps first-pass and refined second-pass entries isolated") {
    val cache = new CommentaryCache()
    val first = CommentResponse(commentary = "first pass", concepts = List("c1"))
    val refined = CommentResponse(commentary = "refined pass", concepts = List("c2"))

    cache.put(fen, move, first, Nil, Some(stateA))
    assertEquals(cache.get(fen, move, Nil, Some(stateA)).map(_.commentary), Some("first pass"))
    assertEquals(cache.get(fen, move, List(probeA), Some(stateA)), None)

    cache.put(fen, move, refined, List(probeA), Some(stateA))
    assertEquals(cache.get(fen, move, List(probeA), Some(stateA)).map(_.commentary), Some("refined pass"))
    assertEquals(cache.get(fen, move, Nil, Some(stateA)).map(_.commentary), Some("first pass"))
  }

  test("probe fingerprint is stable regardless of probe list ordering") {
    val cache = new CommentaryCache()
    val refined = CommentResponse(commentary = "order independent", concepts = Nil)

    cache.put(fen, move, refined, List(probeA, probeB), Some(stateA))
    assertEquals(cache.get(fen, move, List(probeB, probeA), Some(stateA)).map(_.commentary), Some("order independent"))
  }

  test("state token fingerprint isolates cache entries for same fen/lastMove/probe") {
    val cache = new CommentaryCache()
    val responseA = CommentResponse(commentary = "state A", concepts = Nil)
    val responseB = CommentResponse(commentary = "state B", concepts = Nil)

    cache.put(fen, move, responseA, Nil, Some(stateA))
    cache.put(fen, move, responseB, Nil, Some(stateB))

    assertEquals(cache.get(fen, move, Nil, Some(stateA)).map(_.commentary), Some("state A"))
    assertEquals(cache.get(fen, move, Nil, Some(stateB)).map(_.commentary), Some("state B"))
    assertEquals(cache.get(fen, move, Nil, None), None)
  }
}
