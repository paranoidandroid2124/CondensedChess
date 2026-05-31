package lila.commentary.analysis.structure

import munit.FunSuite

import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.VariationLine

class TranspositionPvAlignerTest extends FunSuite:

  private val TranspositionFen =
    "4k3/2pp4/8/8/1N3N2/8/3P4/4K3 w - - 0 1"
  private val Target = "d5"

  test("returns the same proof for move-order transpositions that reach the same weak target") {
    val c6First =
      VariationLine(
        moves = List("d2d4", "c7c6", "e1d2", "d7d5", "d2e1"),
        scoreCp = 28,
        depth = 18
      )
    val d5First =
      VariationLine(
        moves = List("d2d4", "d7d5", "e1d2", "c7c6", "d2e1"),
        scoreCp = 26,
        depth = 18
      )

    val left =
      TranspositionPvAligner.align(
        fen = TranspositionFen,
        line = c6First,
        planId = "StaticWeakness",
        subplanId = Some("static_weakness_fixation"),
        expectedTargets = List(Target),
        baselineScoreCp = Some(30)
      )
    val right =
      TranspositionPvAligner.align(
        fen = TranspositionFen,
        line = d5First,
        planId = "StaticWeakness",
        subplanId = Some("static_weakness_fixation"),
        expectedTargets = List(Target),
        baselineScoreCp = Some(30)
      )

    assertEquals(left, right)
    assertEquals(left.map(_.targetSquare), Some(Target))
    assert(left.exists(_.attackerDefenderDelta > 0), clue(left))
  }

  test("alignPlans only trusts exact square target hints") {
    val line =
      VariationLine(
        moves = List("d2d4", "c7c6", "e1d2", "d7d5", "d2e1"),
        scoreCp = 28,
        depth = 18
      )
    val exact =
      weaknessHypothesis("target:d5")
    val malformed =
      weaknessHypothesis("target:d5_extra")

    assert(TranspositionPvAligner.alignPlans(TranspositionFen, List(line), List(exact)).nonEmpty)
    assertEquals(
      TranspositionPvAligner.alignPlans(TranspositionFen, List(line), List(malformed)),
      Nil
    )
  }

  test("rejects a line where the defender liquidates the requested target") {
    val fenWithTarget =
      "4k3/8/8/3p4/1N3N2/8/8/4K3 w - - 0 1"
    val liquidatingLine =
      VariationLine(
        moves = List("e1d2", "d5d4", "d2e1", "e8d7", "e1d2"),
        scoreCp = 12,
        depth = 18
      )

    assertEquals(
      TranspositionPvAligner.align(
        fen = fenWithTarget,
        line = liquidatingLine,
        planId = "StaticWeakness",
        subplanId = Some("static_weakness_fixation"),
        expectedTargets = List(Target),
        baselineScoreCp = Some(12)
      ),
      None
    )
  }

  test("rejects a line where the target remains but final pressure does not beat defense") {
    val underPressuredFen =
      "4k3/2pp4/8/8/5N2/8/3P4/4K3 w - - 0 1"
    val line =
      VariationLine(
        moves = List("d2d4", "c7c6", "e1d2", "d7d5", "d2e1"),
        scoreCp = 18,
        depth = 18
      )

    assertEquals(
      TranspositionPvAligner.align(
        fen = underPressuredFen,
        line = line,
        planId = "StaticWeakness",
        subplanId = Some("static_weakness_fixation"),
        expectedTargets = List(Target),
        baselineScoreCp = Some(20)
      ),
      None
    )
  }

  test("rejects illegal PV replay without throwing") {
    val illegalLine =
      VariationLine(
        moves = List("d2d4", "e7e8", "e1d2", "d7d5", "d2e1"),
        scoreCp = 0,
        depth = 18
      )

    assertEquals(
      TranspositionPvAligner.align(
        fen = TranspositionFen,
        line = illegalLine,
        planId = "StaticWeakness",
        subplanId = Some("static_weakness_fixation"),
        expectedTargets = List(Target),
        baselineScoreCp = Some(0)
      ),
      None
    )
  }

  test("rejects too-short PV lines without throwing") {
    val shortLine =
      VariationLine(
        moves = List("d2d4", "c7c6", "e1d2", "d7d5"),
        scoreCp = 0,
        depth = 18
      )

    assertEquals(
      TranspositionPvAligner.align(
        fen = TranspositionFen,
        line = shortLine,
        planId = "StaticWeakness",
        subplanId = Some("static_weakness_fixation"),
        expectedTargets = List(Target),
        baselineScoreCp = Some(0)
      ),
      None
    )
  }

  test("does not treat malformed resulting FEN as sufficient horizon") {
    val shortLineWithBadFen =
      VariationLine(
        moves = List("d2d4", "c7c6", "e1d2", "d7d5"),
        scoreCp = 0,
        depth = 18,
        resultingFen = Some("not a fen")
      )

    assertEquals(
      TranspositionPvAligner.align(
        fen = TranspositionFen,
        line = shortLineWithBadFen,
        planId = "StaticWeakness",
        subplanId = Some("static_weakness_fixation"),
        expectedTargets = List(Target),
        baselineScoreCp = Some(0)
      ),
      None
    )
  }

  test("vetoes aligned lines with a large mover loss") {
    val blunderLine =
      VariationLine(
        moves = List("d2d4", "c7c6", "e1d2", "d7d5", "d2e1"),
        scoreCp = -180,
        depth = 18
      )

    assertEquals(
      TranspositionPvAligner.align(
        fen = TranspositionFen,
        line = blunderLine,
        planId = "StaticWeakness",
        subplanId = Some("static_weakness_fixation"),
        expectedTargets = List(Target),
        baselineScoreCp = Some(0),
        maxMoverLossCp = 120
      ),
      None
    )
  }

  private def weaknessHypothesis(targetSource: String): PlanHypothesis =
    PlanHypothesis(
      planId = "StaticWeakness",
      planName = "Static weakness",
      rank = 1,
      score = 0.8,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(score = 0.8, label = "high", risk = "low"),
      evidenceSources = List(targetSource),
      subplanId = Some("static_weakness_fixation")
    )
