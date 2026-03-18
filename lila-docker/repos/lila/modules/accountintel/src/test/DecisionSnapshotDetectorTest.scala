package lila.accountintel

import chess.Color

import lila.accountintel.AccountIntel.*
import lila.accountintel.snapshot.DecisionSnapshotDetector
import lila.llm.analysis.CommentaryEngine
import lila.llm.model.strategic.VariationLine

class DecisionSnapshotDetectorTest extends munit.FunSuite:

  private val parsedGame = ParsedGame(
    external = ExternalGame("chesscom", "g1", "2026-03-17 00:00", "ych24", "opp", "0-1", None, "pgn"),
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
    plyCount = 24,
    rep = None
  )

  private val analysis =
    CommentaryEngine
      .assessExtended(
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        variations = List(VariationLine(List("d2d4"), 0)),
        playedMove = Some("d2d4"),
        ply = 1,
        prevMove = Some("d2d4")
      )
      .get

  private def row(
      ply: Int,
      triggerHints: List[String],
      quiet: Boolean = true,
      transitionType: Option[String] = Some("NaturalShift"),
      planAlignmentBand: Option[String] = Some("OffPlan"),
      explainabilityScore: Double = 0.8,
      preventabilityScore: Double = 0.8,
      branchingScore: Double = 0.8,
      earliestPreventablePly: Option[Int] = None,
      collapseMomentPly: Option[Int] = None
  ) =
    SnapshotFeatureRow(
      gameId = "g1",
      subjectColor = Color.White,
      openingFamily = "Queen's Gambit Declined",
      structureFamily = "Carlsbad",
      labels = List("queen-pawn tension"),
      ply = ply,
      fen = s"fen-$ply",
      sideToMove = Color.White,
      quiet = quiet,
      triggerHints = triggerHints,
      playedUci = "c4d5",
      playedSan = "cxd5",
      explainabilityScore = explainabilityScore,
      preventabilityScore = preventabilityScore,
      branchingScore = branchingScore,
      transitionType = transitionType,
      strategicSalienceHigh = true,
      planAlignmentBand = planAlignmentBand,
      planIntent = Some("play around central tension"),
      planRisk = Some("balanced"),
      hypothesisThemes = List("center"),
      integratedTension = 0.8,
      earliestPreventablePly = earliestPreventablePly,
      collapseMomentPly = collapseMomentPly,
      collapseAnalysis = None,
      analysis = analysis,
      game = parsedGame,
      lastSan = Some("cxd5")
    )

  test("accepts strong tension release snapshots"):
    val detected = DecisionSnapshotDetector.detect(
      List(
        row(
          ply = 16,
          triggerHints = List("tension_release"),
          explainabilityScore = 0.8,
          preventabilityScore = 0.8,
          branchingScore = 0.9,
          earliestPreventablePly = Some(14)
        )
      )
    )

    assertEquals(detected.map(_.triggerType), List("tension_release"))
    assert(detected.head.snapshotConfidence >= 0.58)

  test("collapse-backed windows choose the earliest quiet row before the later slip"):
    val detected = DecisionSnapshotDetector.detect(
      List(
        row(
          ply = 14,
          triggerHints = Nil,
          transitionType = Some("ForcedPivot"),
          planAlignmentBand = Some("OffPlan"),
          preventabilityScore = 0.82,
          earliestPreventablePly = Some(14),
          collapseMomentPly = Some(20)
        ),
        row(
          ply = 18,
          triggerHints = List("tension_release"),
          transitionType = Some("Continuation"),
          planAlignmentBand = Some("OffPlan"),
          preventabilityScore = 0.88,
          earliestPreventablePly = Some(14),
          collapseMomentPly = Some(20)
        )
      )
    )

    assertEquals(detected.head.ply, 14)
    assertEquals(detected.head.windowStartPly, 14)
    assertEquals(detected.head.windowEndPly, 20)
    assert(detected.head.collapseBacked)

  test("non-collapse windows can pull the anchor 1-2 ply earlier than the trigger row"):
    val detected = DecisionSnapshotDetector.detect(
      List(
        row(
          ply = 16,
          triggerHints = Nil,
          transitionType = Some("NaturalShift"),
          planAlignmentBand = Some("Playable"),
          explainabilityScore = 0.79,
          preventabilityScore = 0.72,
          branchingScore = 0.76
        ),
        row(
          ply = 18,
          triggerHints = List("pawn_structure_mutation"),
          transitionType = Some("NaturalShift"),
          planAlignmentBand = Some("OffPlan"),
          explainabilityScore = 0.83,
          preventabilityScore = 0.74,
          branchingScore = 0.8
        )
      )
    )

    assertEquals(detected.head.triggerType, "pawn_structure_mutation")
    assertEquals(detected.head.ply, 16)
    assertEquals(detected.head.windowStartPly, 16)
    assertEquals(detected.head.windowEndPly, 19)

  test("does not let file commitment stand alone at low confidence"):
    val rows = List(
      row(
        ply = 18,
        triggerHints = List("file_commitment"),
        transitionType = Some("Continuation"),
        planAlignmentBand = Some("Playable"),
        explainabilityScore = 0.8,
        preventabilityScore = 0.8,
        branchingScore = 0.6
      )
    )

    assertEquals(DecisionSnapshotDetector.detect(rows), Nil)
