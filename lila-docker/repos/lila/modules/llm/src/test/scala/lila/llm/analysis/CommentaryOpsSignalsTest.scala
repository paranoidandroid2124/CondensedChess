package lila.llm.analysis

import munit.FunSuite
import lila.llm.*

class CommentaryOpsSignalsTest extends FunSuite:

  test("decision comparison flags missing whyAbsent support for why_absent deferred branch") {
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        deferredMove = Some("g4"),
        deferredSource = Some("why_absent"),
        practicalAlternative = false,
        chosenMatchesBest = false
      )

    val obs =
      CommentaryOpsSignals.decisionComparisonConsistency(
        digest = Some(digest),
        whyAbsent = Nil,
        authorEvidence = Nil,
        probeRequests = Nil,
        strategyPack = None
      ).getOrElse(fail("expected observation"))

    assert(!obs.consistent)
    assert(obs.reasons.contains("why_absent_source_missing"))
    assert(obs.reasons.contains("deferred_without_support"))
  }

  test("decision comparison accepts deferred line with explicit support and matching strategy pack") {
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("Nf3"),
        engineBestMove = Some("Nc3"),
        engineBestPv = List("Nc3", "...a6", "g3"),
        deferredMove = Some("Nc3"),
        deferredReason = Some("it keeps queenside pressure in reserve"),
        deferredSource = Some("close_candidate"),
        evidence = Some("The engine line begins Nc3 ..."),
        practicalAlternative = true,
        chosenMatchesBest = false
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        signalDigest = Some(NarrativeSignalDigest(decisionComparison = Some(digest)))
      )

    val obs =
      CommentaryOpsSignals.decisionComparisonConsistency(
        digest = Some(digest),
        whyAbsent = List("Nc3 delays kingside castling"),
        authorEvidence = Nil,
        probeRequests = Nil,
        strategyPack = Some(pack)
      ).getOrElse(fail("expected observation"))

    assert(obs.consistent, clues(obs.reasons))
    assertEquals(obs.reasons, Nil)
  }

  test("active thesis agreement recognizes structure-led deployment note") {
    val digest =
      NarrativeSignalDigest(
        structureProfile = Some("Carlsbad"),
        alignmentBand = Some("Playable"),
        deploymentPiece = Some("R"),
        deploymentRoute = List("b1", "b3"),
        deploymentPurpose = Some("minority attack pressure on the b-file"),
        deploymentContribution = Some("this move clears the b-file route")
      )
    val base =
      "12. Rb1: This Carlsbad structure calls for minority attack pressure, and the rook belongs on the b-file."
    val active =
      "In this Carlsbad structure the rook belongs on the b-file because the minority attack gains force there, and Rb1 clears that route."

    val obs = CommentaryOpsSignals.activeThesisAgreement(base, active, Some(digest))
    assert(obs.agreed, clues(obs.dominantLabels))
  }

  test("active thesis agreement flags drift when note ignores dominant structure and deployment") {
    val digest =
      NarrativeSignalDigest(
        structureProfile = Some("French chain"),
        deploymentPurpose = Some("knight reroute to e5"),
        deploymentContribution = Some("this move supports the reroute")
      )
    val base =
      "18. Nf1: This French chain calls for a knight reroute to e5, and this move supports that route."
    val active =
      "The position stays pleasant and White should simply improve pieces while keeping pressure."

    val obs = CommentaryOpsSignals.activeThesisAgreement(base, active, Some(digest))
    assert(!obs.agreed)
    assert(obs.reasons.contains("dominant_thesis_not_preserved"))
  }
