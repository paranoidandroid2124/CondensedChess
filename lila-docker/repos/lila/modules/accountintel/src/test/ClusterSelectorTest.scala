package lila.accountintel

import chess.Color

import lila.accountintel.AccountIntel.*
import lila.accountintel.cluster.ClusterSelector

class ClusterSelectorTest extends munit.FunSuite:

  private def parsed(id: String, color: Color, result: SubjectResult) =
    ParsedGame(
      external = ExternalGame(
        "chesscom",
        id,
        "2026-03-17 00:00",
        if color.white then "ych24" else "opp",
        if color.black then "ych24" else "opp",
        result match
          case SubjectResult.Win => if color.white then "1-0" else "0-1"
          case SubjectResult.Draw => "1/2-1/2"
          case SubjectResult.Loss => if color.white then "0-1" else "1-0",
        none,
        "pgn"
      ),
      subjectName = "ych24",
      subjectColor = color,
      subjectResult = result,
      openingName = if color.white then "Queen's Gambit Declined" else "Sicilian Defense",
      openingFamily = if color.white then "Queen's Gambit Declined" else "Sicilian Defense",
      openingBucket = if color.white then "faced Queen's Gambit Declined" else "faced Sicilian Defense",
      openingRelation = "faced",
      canonicalEcoCode = None,
      providerOpeningName = None,
      providerEcoCode = None,
      providerEcoUrl = None,
      labels = List("structure"),
      plyCount = 28,
      rep = none
    )

  private def candidate(
      id: String,
      side: Color,
      structure: String,
      trigger: String,
      transition: String,
      band: String,
      result: SubjectResult,
      snapshotConfidence: Double = 0.8
  ) =
    DecisionSnapshotCandidate(
      gameId = id,
      triggerType = trigger,
      side = side,
      openingFamily = if side.white then "Queen's Gambit Declined" else "Sicilian Defense",
      structureFamily = structure,
      labels = List("structure"),
      ply = 16,
      fen = s"fen-$id",
      quiet = true,
      playedUci = "d4d5",
      explainabilityScore = 0.82,
      preventabilityScore = if band == "OffPlan" then 0.83 else 0.7,
      branchingScore = 0.81,
      snapshotConfidence = snapshotConfidence,
      commitmentScore = if trigger == "file_commitment" then 0.22 else 0.76,
      collapseBacked = band == "OffPlan",
      transitionType = Some(transition),
      planAlignmentBand = Some(band),
      earliestPreventablePly = Option.when(band == "OffPlan")(14),
      windowStartPly = 14,
      windowEndPly = 18,
      repeatabilityKey =
        s"${colorKey(side)}|${slug(structure)}|$trigger|${transition.toLowerCase}|${band.toLowerCase}",
      game = parsed(id, side, result),
      lastSan = Some("move")
    )

  private def cluster(
      id: String,
      side: Color,
      structure: String,
      trigger: String,
      priority: Double,
      candidates: List[DecisionSnapshotCandidate]
  ) =
    SnapshotCluster(
      id = id,
      side = side,
      openingFamily = candidates.head.openingFamily,
      structureFamily = structure,
      triggerType = trigger,
      labels = List("structure"),
      priorityScore = priority,
      priorityBreakdown = PriorityBreakdown(
        supportScore = 1.0,
        repeatabilityScore = 0.45,
        snapshotScore = 0.82,
        preventabilityScore = 0.75,
        branchingScore = 0.7,
        repairImpactScore = 0.68,
        readinessBonus = 0.2,
        triggerPenalty = if trigger == "file_commitment" then 0.18 else 0,
        redundancyPenalty = 0
      ),
      distinctGames = candidates.map(_.gameId).distinct.size,
      distinctOpenings = candidates.map(_.openingFamily).distinct.size,
      collapseBackedRate = candidates.count(_.collapseBacked).toDouble / candidates.size.toDouble,
      offPlanRate = candidates.count(_.planAlignmentBand.contains("OffPlan")).toDouble / candidates.size.toDouble,
      quietRate = 1.0,
      triggerDiversity = candidates.map(_.triggerType).distinct.size.toDouble,
      snapshotConfidenceMean = candidates.map(_.snapshotConfidence).sum / candidates.size.toDouble,
      exemplarCentrality = 0.8,
      nonContinuationRate =
        candidates.count(_.transitionType.exists(_ != "Continuation")).toDouble / candidates.size.toDouble,
      averageExplainability = candidates.map(_.explainabilityScore).sum / candidates.size.toDouble,
      averagePreventability = candidates.map(_.preventabilityScore).sum / candidates.size.toDouble,
      averageBranching = candidates.map(_.branchingScore).sum / candidates.size.toDouble,
      resultImpactScore = 0.8,
      earliestPreventableRate =
        candidates.count(_.earliestPreventablePly.isDefined).toDouble / candidates.size.toDouble,
      candidates = candidates,
      exemplar = ClusterExemplar(candidates.head.game, candidates.head.some)
    )

  test("my account selection preserves white and black coverage first"):
    val white = cluster(
      "white-iqp",
      Color.White,
      "White IQP",
      "tension_release",
      5.1,
      List(
        candidate("w1", Color.White, "White IQP", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Loss),
        candidate("w2", Color.White, "White IQP", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Draw)
      )
    )
    val black = cluster(
      "black-shell",
      Color.Black,
      "Kingside fianchetto shell",
      "pawn_structure_mutation",
      4.9,
      List(
        candidate("b1", Color.Black, "Kingside fianchetto shell", "pawn_structure_mutation", "NaturalShift", "OffPlan", SubjectResult.Loss),
        candidate("b2", Color.Black, "Kingside fianchetto shell", "pawn_structure_mutation", "NaturalShift", "OffPlan", SubjectResult.Draw)
      )
    )
    val whiteSecond = cluster(
      "white-second",
      Color.White,
      "Carlsbad",
      "major_simplification",
      4.8,
      List(
        candidate("w3", Color.White, "Carlsbad", "major_simplification", "NaturalShift", "OffPlan", SubjectResult.Draw),
        candidate("w4", Color.White, "Carlsbad", "major_simplification", "NaturalShift", "OffPlan", SubjectResult.Loss)
      )
    )

    val selected =
      ClusterSelector.select(ProductKind.MyAccountIntelligenceLite, List(whiteSecond, white, black))

    assertEquals(selected.take(2).map(_.side).toSet, Set(Color.White, Color.Black))
    assertEquals(selected.head.id, "white-iqp")
    assertEquals(selected(1).id, "black-shell")

  test("opponent prep favors steering-friendly black cluster over continuation-heavy file commitment"):
    val white = cluster(
      "white-iqp",
      Color.White,
      "White IQP",
      "tension_release",
      5.0,
      List(
        candidate("w1", Color.White, "White IQP", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Loss),
        candidate("w2", Color.White, "White IQP", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Draw)
      )
    )
    val blackSteering = cluster(
      "black-steer",
      Color.Black,
      "Kingside fianchetto shell",
      "pawn_structure_mutation",
      4.7,
      List(
        candidate("b1", Color.Black, "Kingside fianchetto shell", "pawn_structure_mutation", "ForcedPivot", "OffPlan", SubjectResult.Loss),
        candidate("b2", Color.Black, "Kingside fianchetto shell", "pawn_structure_mutation", "NaturalShift", "OffPlan", SubjectResult.Draw)
      )
    )
    val blackFile = cluster(
      "black-file",
      Color.Black,
      "Open c-file campaign",
      "file_commitment",
      5.3,
      List(
        candidate("b3", Color.Black, "Open c-file campaign", "file_commitment", "Continuation", "Playable", SubjectResult.Draw, snapshotConfidence = 0.61),
        candidate("b4", Color.Black, "Open c-file campaign", "file_commitment", "Continuation", "Playable", SubjectResult.Win, snapshotConfidence = 0.6)
      )
    )

    val selected =
      ClusterSelector.select(ProductKind.OpponentPrep, List(blackFile, white, blackSteering))

    assertEquals(selected.take(2).map(_.id).toSet, Set("white-iqp", "black-steer"))
    assert(!selected.take(2).exists(_.id == "black-file"))

  test("third pick prefers a different trigger when the score is close"):
    val white = cluster(
      "white-iqp",
      Color.White,
      "White IQP",
      "tension_release",
      5.0,
      List(
        candidate("w1", Color.White, "White IQP", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Loss),
        candidate("w2", Color.White, "White IQP", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Draw)
      )
    )
    val black = cluster(
      "black-shell",
      Color.Black,
      "Kingside fianchetto shell",
      "tension_release",
      4.9,
      List(
        candidate("b1", Color.Black, "Kingside fianchetto shell", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Loss),
        candidate("b2", Color.Black, "Kingside fianchetto shell", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Draw)
      )
    )
    val sameTriggerThird = cluster(
      "same-trigger-third",
      Color.White,
      "Carlsbad",
      "tension_release",
      4.82,
      List(
        candidate("w3", Color.White, "Carlsbad", "tension_release", "NaturalShift", "OffPlan", SubjectResult.Draw),
        candidate("w4", Color.White, "Carlsbad", "tension_release", "NaturalShift", "OffPlan", SubjectResult.Loss)
      )
    )
    val differentTriggerThird = cluster(
      "different-trigger-third",
      Color.White,
      "White isolated d-pawn",
      "major_simplification",
      4.74,
      List(
        candidate("w5", Color.White, "White isolated d-pawn", "major_simplification", "NaturalShift", "OffPlan", SubjectResult.Draw),
        candidate("w6", Color.White, "White isolated d-pawn", "major_simplification", "NaturalShift", "OffPlan", SubjectResult.Loss)
      )
    )

    val selected =
      ClusterSelector.select(
        ProductKind.MyAccountIntelligenceLite,
        List(white, black, sameTriggerThird, differentTriggerThird)
      )

    assertEquals(selected.last.id, "different-trigger-third")

  test("file commitment cannot enter the top two slots"):
    val whiteFile = cluster(
      "white-file",
      Color.White,
      "White rook lift lane",
      "file_commitment",
      5.6,
      List(
        candidate("wf1", Color.White, "White rook lift lane", "file_commitment", "Continuation", "Playable", SubjectResult.Draw, snapshotConfidence = 0.6),
        candidate("wf2", Color.White, "White rook lift lane", "file_commitment", "Continuation", "Playable", SubjectResult.Draw, snapshotConfidence = 0.61)
      )
    )
    val whiteRepair = cluster(
      "white-repair",
      Color.White,
      "White IQP",
      "tension_release",
      5.0,
      List(
        candidate("wr1", Color.White, "White IQP", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Loss),
        candidate("wr2", Color.White, "White IQP", "tension_release", "ForcedPivot", "OffPlan", SubjectResult.Draw)
      )
    )
    val blackRepair = cluster(
      "black-repair",
      Color.Black,
      "Black IQP",
      "pawn_structure_mutation",
      4.8,
      List(
        candidate("br1", Color.Black, "Black IQP", "pawn_structure_mutation", "NaturalShift", "OffPlan", SubjectResult.Loss),
        candidate("br2", Color.Black, "Black IQP", "pawn_structure_mutation", "NaturalShift", "OffPlan", SubjectResult.Draw)
      )
    )

    val selected =
      ClusterSelector.select(ProductKind.MyAccountIntelligenceLite, List(whiteFile, whiteRepair, blackRepair))

    assertEquals(selected.head.id, "white-repair")
    assertEquals(selected(1).id, "black-repair")
