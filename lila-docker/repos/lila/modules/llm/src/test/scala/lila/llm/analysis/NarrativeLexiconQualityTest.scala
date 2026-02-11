package lila.llm.analysis

import munit.FunSuite

class NarrativeLexiconQualityTest extends FunSuite:

  test("opening templates do not emit legacy coordination boilerplate"):
    val legacy = "both sides are still coordinating pieces"
    (0 until 32).foreach { bead =>
      val nonTactical = NarrativeLexicon.getOpening(bead, "opening", "The position is near parity.")
      val tactical = NarrativeLexicon.getOpening(bead, "opening", "The position is near parity.", tactical = true)
      assert(!nonTactical.toLowerCase.contains(legacy))
      assert(!tactical.toLowerCase.contains(legacy))
    }

  test("plan statement does not use legacy 'plan is clear' phrasing"):
    val legacy = "the plan is clear"
    (0 until 16).foreach { bead =>
      val text = NarrativeLexicon.getPlanStatement(bead, "Pawn Chain Maintenance").toLowerCase
      assert(!text.contains(legacy))
    }

  test("compensation statement avoids tautological compensation phrase"):
    val text = NarrativeLexicon
      .getCompensationStatement(bead = 9, tpe = "Positional Compensation", severity = "Sufficient")
      .toLowerCase
    assert(!text.contains("compensation provides sufficient compensation"))
    assert(!text.contains("sufficient positional compensation provides sufficient compensation"))

  test("plan statement rotates wording across consecutive plies"):
    val plan = "Pawn Chain Maintenance"
    val p0 = NarrativeLexicon.getPlanStatement(bead = 101, planName = plan, ply = 20)
    val p1 = NarrativeLexicon.getPlanStatement(bead = 101, planName = plan, ply = 21)
    val p2 = NarrativeLexicon.getPlanStatement(bead = 101, planName = plan, ply = 22)
    val p3 = NarrativeLexicon.getPlanStatement(bead = 101, planName = plan, ply = 23)
    val set = Set(p0, p1, p2, p3)
    assert(set.size >= 3)

  test("annotation negative uses severe language for blunder scale cp loss"):
    val text = NarrativeLexicon.getAnnotationNegative(
      bead = 17,
      playedSan = "Qh5",
      bestSan = "Nf3",
      cpLoss = 320
    ).toLowerCase
    assert(text.contains("blunder") || text.contains("decisive"))
    assert(text.contains("??"))

  test("annotation negative avoids cp-centric phrasing"):
    val text = NarrativeLexicon.getAnnotationNegative(
      bead = 21,
      playedSan = "Qh5",
      bestSan = "Nf3",
      cpLoss = 180
    ).toLowerCase
    assert(!text.contains("cp"))
    assert(!text.contains("pawns"))

  test("threat statement avoids cp display and explains consequence"):
    val text = NarrativeLexicon.getThreatStatement(
      bead = 9,
      kind = "Material",
      loss = 320
    ).toLowerCase
    assert(!text.contains("cp"))
    assert(text.contains("material") || text.contains("piece"))

  test("teaching point avoids cp display"):
    val text = NarrativeLexicon.getTeachingPoint(
      bead = 13,
      theme = "fork",
      cpLoss = 180
    ).toLowerCase
    assert(!text.contains("cp"))

  test("annotation positive is not overhyped"):
    val text = NarrativeLexicon.getAnnotationPositive(bead = 5, playedSan = "Nf3").toLowerCase
    assert(!text.contains("excellent choice"))

  test("opening lead rotates across adjacent plies with same seed"):
    val p20 = NarrativeLexicon.getOpening(
      bead = 77,
      phase = "middlegame",
      evalText = "White has a small pull.",
      tactical = false,
      ply = 20
    )
    val p21 = NarrativeLexicon.getOpening(
      bead = 77,
      phase = "middlegame",
      evalText = "White has a small pull.",
      tactical = false,
      ply = 21
    )
    assertNotEquals(p20, p21)

  test("opening lead and angle start with sentence case"):
    val text = NarrativeLexicon.getOpening(
      bead = 91,
      phase = "opening",
      evalText = "The position is roughly balanced.",
      tactical = false,
      ply = 6
    )
    val parts = text.split("\\.\\s+").toList.filter(_.nonEmpty)
    assert(parts.nonEmpty)
    assert(parts.head.head.isUpper)
    if parts.size >= 2 then assert(parts(1).head.isUpper)

  test("motif prefix rotates phrasing across adjacent plies"):
    val m10 = NarrativeLexicon.getMotifPrefix(bead = 31, motifs = List("deflection"), ply = 10)
    val m11 = NarrativeLexicon.getMotifPrefix(bead = 31, motifs = List("deflection"), ply = 11)
    assert(m10.nonEmpty)
    assert(m11.nonEmpty)
    assertNotEquals(m10, m11)

  test("motif prefix signal detector matches prefix families"):
    val positives = List("OpenFile", "StalemateTrap", "RookOnSeventh", "pawnBreak", "GreekGift", "skewerQueen")
    positives.foreach(m => assert(NarrativeLexicon.isMotifPrefixSignal(m), m))

    val negatives = List("quiet_move", "simple_development", "waiting_move")
    negatives.foreach(m => assert(!NarrativeLexicon.isMotifPrefixSignal(m), m))

  test("precedent mechanism line rotates wording without collapsing to one stem"):
    val lines = (0 until 24).map { bead =>
      NarrativeLexicon.getPrecedentMechanismLine(
        bead = bead + 17,
        triggerMove = "Na4",
        replyMove = Some("Rab1"),
        pivotMove = Some("Rc5"),
        mechanism = "ExchangeCascade"
      )
    }
    val normalized =
      lines
        .map(_.toLowerCase.replaceAll("""[^a-z\s]""", " ").replaceAll("""\s+""", " ").trim)
        .map(_.split(" ").take(6).mkString(" "))
        .toSet
    assert(normalized.size >= 4)
