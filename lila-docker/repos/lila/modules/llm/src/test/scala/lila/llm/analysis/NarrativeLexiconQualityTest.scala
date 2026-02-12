package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.{ HypothesisAxis, HypothesisHorizon }

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

  test("annotation positive avoids legacy technical setup stem and rotates first words"):
    val lines = (0 until 48).map { bead =>
      NarrativeLexicon.getAnnotationPositive(bead = bead + 300, playedSan = "Nf3").toLowerCase
    }
    assert(lines.forall(!_.contains("retains a sound technical setup for the next phase")))
    assert(lines.forall(!_.contains("technical setup for the next")))
    val stems =
      lines.map { line =>
        line
          .replaceAll("""\*\*[^*]+\*\*""", " ")
          .replaceAll("""[^a-z\s]""", " ")
          .replaceAll("""\s+""", " ")
          .trim
          .split(" ")
          .filter(_.nonEmpty)
          .take(4)
          .mkString(" ")
      }.toSet
    assert(stems.size >= 4)

  test("annotation/engine/alternative templates keep medium sentence rhythm"):
    val positive = (0 until 32).map { bead =>
      NarrativeLexicon.getAnnotationPositive(bead = bead + 500, playedSan = "Nf3")
    }
    val negative = (0 until 32).map { bead =>
      NarrativeLexicon.getAnnotationNegative(bead = bead + 540, playedSan = "Qh5", bestSan = "Nf3", cpLoss = 160)
    }
    val engine = (0 until 32).flatMap { bead =>
      NarrativeLexicon.getEngineRankContext(bead = bead + 580, rank = Some(2), bestSan = "Nf3", cpLoss = 45)
    }
    val alternatives = (0 until 32).map { bead =>
      NarrativeLexicon.getAlternative(bead = bead + 620, move = "Nc3", whyNot = Some("it loosens central control"))
    }
    val sampled = positive ++ negative ++ engine ++ alternatives
    val avgWords = sampled.map(wordCount).sum.toDouble / sampled.size
    assert(avgWords >= 9.0, clue(f"avgWords=$avgWords%.2f"))

  test("engine and alternative templates diversify first stems"):
    val engineStems = (0 until 24).flatMap { bead =>
      NarrativeLexicon.getEngineRankContext(bead = bead + 700, rank = Some(3), bestSan = "Nf3", cpLoss = 55)
    }.map(firstStem).toSet
    val altStems = (0 until 24).map { bead =>
      NarrativeLexicon.getAlternative(bead = bead + 760, move = "Nc3", whyNot = Some("it loosens central control"))
    }.map(firstStem).toSet
    assert(engineStems.size >= 3, clue(engineStems.mkString(" | ")))
    assert(altStems.size >= 4, clue(altStems.mkString(" | ")))

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

  test("precedent comparison role lines avoid fixed legacy prefixes"):
    val routeLines = (0 until 16).map { bead =>
      NarrativeLexicon.getPrecedentRouteLine(
        bead = bead + 41,
        triggerMove = "Na4",
        replyMove = Some("Rab1"),
        pivotMove = Some("Rc5")
      ).toLowerCase
    }
    val transitionLines = (0 until 16).map { bead =>
      NarrativeLexicon.getPrecedentStrategicTransitionLine(
        bead = bead + 59,
        mechanism = "exchange timing that simplified into a cleaner structure"
      ).toLowerCase
    }
    val driverLines = (0 until 16).map { bead =>
      NarrativeLexicon.getPrecedentDecisionDriverLine(
        bead = bead + 73,
        mechanism = "initiative swings created by faster piece activity"
      ).toLowerCase
    }

    assert(routeLines.forall(!_.contains("sequence focus")))
    assert(transitionLines.forall(!_.contains("strategic shift:")))
    assert(routeLines.exists(_.contains("route")))
    assert(transitionLines.exists(_.contains("strateg")))
    assert(driverLines.exists(l => l.contains("driver") || l.contains("hinged")))

  test("precedent route role rotates wording"):
    val stems = (0 until 20).map { bead =>
      NarrativeLexicon.getPrecedentRouteLine(
        bead = bead + 11,
        triggerMove = "Na4",
        replyMove = Some("Rab1"),
        pivotMove = Some("Rc5")
      )
        .toLowerCase
        .replaceAll("""[^a-z\s]""", " ")
        .replaceAll("""\s+""", " ")
        .trim
        .split(" ")
        .take(4)
        .mkString(" ")
    }.toSet
    assert(stems.size >= 3)

  test("engine rank context avoids legacy engine-wise phrasing"):
    val text = NarrativeLexicon
      .getEngineRankContext(bead = 29, rank = Some(2), bestSan = "Nf3", cpLoss = 45)
      .getOrElse("")
      .toLowerCase
    assert(!text.contains("engine-wise"))

  test("hypothesis templates diversify stems and avoid fixed boilerplate"):
    val lines = (0 until 24).map { bead =>
      NarrativeLexicon.getHypothesisClause(
        bead = bead + 900,
        claim = "Nf3 keeps coordination lanes connected before central clarification.",
        confidence = 0.64,
        horizon = HypothesisHorizon.Medium,
        axis = HypothesisAxis.PieceCoordination
      )
    }
    val stems = lines.map(firstFiveStem).toSet
    assert(stems.size >= 4, clue(stems.mkString(" | ")))
    assert(lines.forall(!_.toLowerCase.contains("strategic test after")))

  test("hypothesis practical clause rotates short and long horizon wording"):
    val short = NarrativeLexicon.getHypothesisPracticalClause(
      bead = 911,
      horizon = HypothesisHorizon.Short,
      axis = HypothesisAxis.KingSafety,
      move = "Nf3"
    ).toLowerCase
    val long = NarrativeLexicon.getHypothesisPracticalClause(
      bead = 911,
      horizon = HypothesisHorizon.Long,
      axis = HypothesisAxis.EndgameTrajectory,
      move = "Nf3"
    ).toLowerCase
    assertNotEquals(short, long)
    assert(short.contains("next") || short.contains("immediate"), clue(short))
    assert(long.contains("long") || long.contains("later") || long.contains("ending"), clue(long))

  test("long-horizon bridge clause diversifies stems and keeps now-to-later structure"):
    val lines = (0 until 28).map { bead =>
      NarrativeLexicon.getLongHorizonBridgeClause(
        bead = bead + 980,
        move = "Nf3",
        axis = HypothesisAxis.EndgameTrajectory
      )
    }
    val stems = lines.map(firstFiveStem).toSet
    assert(stems.size >= 4, clue(stems.mkString(" | ")))
    assert(
      lines.forall { line =>
        val lower = line.toLowerCase
        lower.contains("now") &&
        (lower.contains("later") || lower.contains("late") || lower.contains("simplif") || lower.contains("endgame"))
      },
      clue(lines.mkString(" || "))
    )
    assert(lines.forall(!_.toLowerCase.contains("technical setup for the next")))

  test("alternative hypothesis difference always contains comparison and strategic meaning"):
    val line = NarrativeLexicon.getAlternativeHypothesisDifference(
      bead = 947,
      alternativeMove = "Ne2",
      mainMove = "Nf3",
      mainAxis = Some(HypothesisAxis.PieceCoordination),
      alternativeAxis = Some(HypothesisAxis.PawnBreakTiming),
      alternativeClaim = Some("Ne2 keeps c-pawn flexibility but delays central tension tests."),
      confidence = 0.58,
      horizon = HypothesisHorizon.Medium
    ).toLowerCase
    assert(
      List(
        "compared with",
        "relative to",
        "against the main move",
        "versus the principal choice",
        "in contrast to",
        "set against",
        "measured against"
      ).exists(line.contains),
      clue(line)
    )
    assert(line.contains("timing") || line.contains("coordination") || line.contains("trajectory"), clue(line))

  test("alternative and wrap-up variants keep unique prefixes and block banned phrase families"):
    val altVariants = NarrativeLexicon.getAlternativeHypothesisDifferenceVariants(
      bead = 1311,
      alternativeMove = "Ne2",
      mainMove = "Nf3",
      mainAxis = Some(HypothesisAxis.PieceCoordination),
      alternativeAxis = Some(HypothesisAxis.PawnBreakTiming),
      alternativeClaim = Some("Ne2 keeps c-pawn flexibility but delays central tension tests."),
      confidence = 0.58,
      horizon = HypothesisHorizon.Medium
    )
    val altPrefixes = altVariants.map(firstFourStem).filter(_.nonEmpty)
    assertEquals(altPrefixes.distinct.size, altPrefixes.size, clue(altVariants.mkString(" || ")))
    val altLower = altVariants.mkString(" ").toLowerCase
    assert(!altLower.contains("puts more weight on"), clue(altLower))
    assert(!altLower.contains("as plans crystallize after"), clue(altLower))
    assert(!altLower.contains("as **ne2** enters concrete tactical play, **ne2**"), clue(altLower))

    val wrapVariants = NarrativeLexicon.getWrapUpDecisiveDifferenceVariants(
      bead = 1777,
      mainMove = "Nf3",
      altMove = "Ne2",
      mainAxis = HypothesisAxis.PieceCoordination,
      altAxis = HypothesisAxis.PawnBreakTiming,
      mainHorizon = HypothesisHorizon.Short,
      altHorizon = HypothesisHorizon.Medium
    )
    val wrapPrefixes = wrapVariants.map(firstFourStem).filter(_.nonEmpty)
    assertEquals(wrapPrefixes.distinct.size, wrapPrefixes.size, clue(wrapVariants.mkString(" || ")))

  private def wordCount(text: String): Int =
    text
      .split("""\s+""")
      .toList
      .map(_.replaceAll("""[^A-Za-z0-9']""", ""))
      .count(_.nonEmpty)

  private def firstStem(text: String): String =
    text
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(4)
      .mkString(" ")

  private def firstFiveStem(text: String): String =
    text
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(5)
      .mkString(" ")

  private def firstFourStem(text: String): String =
    text
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(4)
      .mkString(" ")
