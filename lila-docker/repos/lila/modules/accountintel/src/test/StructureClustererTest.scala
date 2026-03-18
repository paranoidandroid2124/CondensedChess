package lila.accountintel

import chess.Color

import lila.accountintel.AccountIntel.*
import lila.accountintel.cluster.StructureClusterer

class StructureClustererTest extends munit.FunSuite:

  private val parsedWhite = ParsedGame(
    external = ExternalGame("chesscom", "gw", "2026-03-17 00:00", "ych24", "opp", "0-1", None, "pgn"),
    subjectName = "ych24",
    subjectColor = Color.White,
    subjectResult = SubjectResult.Loss,
    openingName = "Queen's Gambit Declined",
    openingFamily = "Queen's Gambit Declined",
    openingBucket = "faced Queen's Gambit Declined",
    openingRelation = "faced",
    canonicalEcoCode = None,
    providerOpeningName = None,
    providerEcoCode = None,
    providerEcoUrl = None,
    labels = List("queen-pawn tension"),
    plyCount = 30,
    rep = None
  )

  private val parsedBlack = parsedWhite.copy(
    external = parsedWhite.external.copy(gameId = "gb", white = "opp", black = "ych24", result = "1-0"),
    subjectColor = Color.Black
  )

  private def candidate(
      gameId: String,
      side: Color,
      openingFamily: String,
      structure: String,
      trigger: String,
      result: SubjectResult,
      transitionType: String = "NaturalShift",
      band: String = "OffPlan",
      snapshotConfidence: Double = 0.78,
      commitmentScore: Double = 0.74,
      collapseBacked: Boolean = false
  ) =
    DecisionSnapshotCandidate(
      gameId = gameId,
      triggerType = trigger,
      side = side,
      openingFamily = openingFamily,
      structureFamily = structure,
      labels = List("queen-pawn tension"),
      ply = 16,
      fen = s"fen-$gameId",
      quiet = true,
      playedUci = "c4d5",
      explainabilityScore = 0.8,
      preventabilityScore = if collapseBacked then 0.9 else 0.72,
      branchingScore = 0.8,
      snapshotConfidence = snapshotConfidence,
      commitmentScore = commitmentScore,
      collapseBacked = collapseBacked,
      transitionType = Some(transitionType),
      planAlignmentBand = Some(band),
      earliestPreventablePly = Option.when(collapseBacked)(14),
      windowStartPly = if collapseBacked then 14 else 14,
      windowEndPly = if collapseBacked then 20 else 17,
      repeatabilityKey =
        s"${colorKey(side)}|${slug(structure)}|$trigger|${transitionType.toLowerCase}|${band.toLowerCase}",
      game =
        if side.white then parsedWhite.copy(
          external = parsedWhite.external.copy(gameId = gameId),
          subjectResult = result,
          openingFamily = openingFamily,
          openingName = openingFamily
        )
        else parsedBlack.copy(
          external = parsedBlack.external.copy(gameId = gameId),
          subjectResult = result,
          openingFamily = openingFamily,
          openingName = openingFamily
        ),
      lastSan = Some("cxd5")
    )

  test("clusterer keeps white and black structure buckets separate"):
    val clusters = StructureClusterer.cluster(
      List(
        candidate("gw", Color.White, "Queen's Gambit Declined", "Carlsbad", "pawn_structure_mutation", SubjectResult.Loss),
        candidate("gb", Color.Black, "Sicilian Defense", "Carlsbad", "pawn_structure_mutation", SubjectResult.Loss)
      )
    )

    assertEquals(clusters.size, 2)
    assertEquals(clusters.map(_.side).toSet, Set(Color.White, Color.Black))

  test("clusterer ranks high-value repair clusters above shallow frequent ones"):
    val shallow = List(
      candidate("g-shallow-1", Color.White, "Queen's Gambit Declined", "Carlsbad", "file_commitment", SubjectResult.Win, transitionType = "Continuation", band = "Playable", snapshotConfidence = 0.59, commitmentScore = 0.25),
      candidate("g-shallow-2", Color.White, "English Opening", "Carlsbad", "file_commitment", SubjectResult.Draw, transitionType = "Continuation", band = "Playable", snapshotConfidence = 0.6, commitmentScore = 0.22),
      candidate("g-shallow-3", Color.White, "Queen's Gambit Declined", "Carlsbad", "file_commitment", SubjectResult.Draw, transitionType = "Continuation", band = "Playable", snapshotConfidence = 0.58, commitmentScore = 0.2)
    )
    val deep = List(
      candidate("g-deep-1", Color.Black, "Sicilian Defense", "Najdorf Hedgehog", "pawn_structure_mutation", SubjectResult.Loss, transitionType = "ForcedPivot", band = "OffPlan", snapshotConfidence = 0.84, commitmentScore = 0.86, collapseBacked = true),
      candidate("g-deep-2", Color.Black, "French Defense", "Najdorf Hedgehog", "major_simplification", SubjectResult.Loss, transitionType = "ForcedPivot", band = "OffPlan", snapshotConfidence = 0.82, commitmentScore = 0.8, collapseBacked = true)
    )

    val clusters = StructureClusterer.cluster(shallow ++ deep)

    assertEquals(clusters.head.structureFamily, "Najdorf Hedgehog")
    assert(clusters.head.priorityScore > clusters(1).priorityScore)

  test("redundancy penalty applies to weaker sibling in the same side and structure family"):
    val bestSibling = List(
      candidate("sib-1", Color.White, "Queen's Gambit Declined", "White IQP", "tension_release", SubjectResult.Loss, snapshotConfidence = 0.82, commitmentScore = 0.82),
      candidate("sib-2", Color.White, "English Opening", "White IQP", "tension_release", SubjectResult.Draw, snapshotConfidence = 0.8, commitmentScore = 0.8),
      candidate("sib-5", Color.White, "Catalan", "White IQP", "tension_release", SubjectResult.Loss, snapshotConfidence = 0.81, commitmentScore = 0.79)
    )
    val weakerSibling = List(
      candidate("sib-3", Color.White, "Queen's Gambit Declined", "White IQP", "major_simplification", SubjectResult.Win, transitionType = "Continuation", band = "Playable", snapshotConfidence = 0.52, commitmentScore = 0.3),
      candidate("sib-4", Color.White, "English Opening", "White IQP", "major_simplification", SubjectResult.Loss, transitionType = "ForcedPivot", band = "OffPlan", snapshotConfidence = 0.91, commitmentScore = 0.78)
    )

    val clusters = StructureClusterer.cluster(bestSibling ++ weakerSibling)
    val stronger = clusters.find(_.triggerType == "tension_release").get
    val weaker = clusters.find(_.triggerType == "major_simplification").get

    assert(Set(stronger.priorityBreakdown.redundancyPenalty, weaker.priorityBreakdown.redundancyPenalty) == Set(0d, 0.22d))
