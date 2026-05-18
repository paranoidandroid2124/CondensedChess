package lila.commentary.analysis

import chess.Color
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.analysis.L3.ThreatAnalyzer
import munit.FunSuite

class StrategicConceptSemanticsTest extends FunSuite:

  private def semantic(
      fen: String,
      side: String,
      threatsToUs: Option[L3.ThreatAnalysis] = None
  ): StrategicIdeaSemanticContext =
    val board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))
    StrategicIdeaSemanticContext(
      sideToMove = side,
      fen = fen,
      board = Some(board),
      threatsToUs = threatsToUs
    )

  private def minority(
      fen: String,
      side: String = "white",
      threatsToUs: Option[L3.ThreatAnalysis] = None
  ): List[StrategicConceptSemantics.StrategicConceptObservation] =
    StrategicConceptSemantics.minorityAttackObservations(semantic(fen, side, threatsToUs))

  test("Carlsbad structure is a semantic minority-attack instance, not the definition") {
    val obs =
      minority("4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1")
        .find(_.wing == "queenside")
        .getOrElse(fail("missing queenside minority observation"))

    assertEquals(obs.status, StrategicConceptSemantics.ConceptStatus.SemanticReady)
    assertEquals(obs.side, Color.White)
    assertEquals(obs.primaryBreak, Some("b4b5"))
    assert(obs.targets.contains("c6"), clues(obs))
    assert(obs.structuralDelta.exists(_.pawnTensionDelta > 0), clues(obs))
    assert(obs.essentialEvidence.exists(_.id == "reachable_break"), clues(obs))
    assert(obs.essentialEvidence.exists(_.id == "structural_consequence"), clues(obs))
  }

  test("same structure without an opening profile is still recognized by board predicate") {
    val obs =
      minority("4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1")
        .find(_.wing == "queenside")
        .getOrElse(fail("missing queenside minority observation"))

    assertEquals(obs.status, StrategicConceptSemantics.ConceptStatus.SemanticReady)
    assert(!obs.essentialEvidence.exists(_.id.contains("carlsbad")), clues(obs))
  }

  test("color-reversed minority attack uses the same schema") {
    val obs =
      minority("4k3/p4ppp/4p3/1p1p4/3P4/2P1P3/PP3PPP/4K3 b - - 0 1", side = "black")
        .find(_.wing == "queenside")
        .getOrElse(fail("missing black queenside minority observation"))

    assertEquals(obs.status, StrategicConceptSemantics.ConceptStatus.SemanticReady)
    assertEquals(obs.side, Color.Black)
    assertEquals(obs.primaryBreak, Some("b5b4"))
    assert(obs.targets.contains("c3"), clues(obs))
  }

  test("file-mirrored wing minority uses the same schema") {
    val obs =
      minority("4k3/6pp/5p2/8/6P1/4P3/7P/4K3 w - - 0 1")
        .find(_.wing == "kingside")
        .getOrElse(fail("missing kingside minority observation"))

    assertEquals(obs.status, StrategicConceptSemantics.ConceptStatus.SemanticReady)
    assertEquals(obs.primaryBreak, Some("g4g5"))
    assert(obs.targets.contains("f6"), clues(obs))
  }

  test("pawn-count minority without a targetable majority remains only a candidate") {
    val obs =
      minority("4k3/ppp2ppp/8/8/1P6/4P3/P4PPP/4K3 w - - 0 1")
        .find(_.wing == "queenside")
        .getOrElse(fail("missing queenside minority candidate"))

    assertEquals(obs.status, StrategicConceptSemantics.ConceptStatus.Candidate)
    assert(obs.blockedReasons.contains("targetable_enemy_majority_missing"), clues(obs))
    assert(obs.structuralDelta.isEmpty, clues(obs))
  }

  test("targetable majority without a reachable break does not become semantic-ready") {
    val obs =
      minority("4k3/pp3ppp/2p5/3p4/3P4/4P3/P4PPP/4K3 w - - 0 1")
        .find(_.wing == "queenside")
        .getOrElse(fail("missing blocked queenside minority observation"))

    assertEquals(obs.status, StrategicConceptSemantics.ConceptStatus.Deferred)
    assert(obs.blockedReasons.contains("reachable_break_missing"), clues(obs))
  }

  test("reachable break without structural consequence is not certified as a concept") {
    val obs =
      minority("4k3/pp3ppp/2p5/3P4/1P6/4P3/P4PPP/4K3 w - - 0 1")
        .find(_.wing == "queenside")
        .getOrElse(fail("missing queenside minority candidate"))

    assert(obs.status != StrategicConceptSemantics.ConceptStatus.SemanticReady, clues(obs))
    assert(obs.blockedReasons.exists(_.contains("structural_consequence")), clues(obs))
  }

  test("structural candidate is refuted when immediate tactical danger dominates") {
    val refutedThreat =
      ThreatAnalyzer.noThreat.copy(
        threatIgnorable = false,
        defenseRequired = true,
        maxLossIfIgnored = 350,
        primaryDriver = "material_threat"
      )
    val obs =
      minority(
        "4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1",
        threatsToUs = Some(refutedThreat)
      ).find(_.wing == "queenside").getOrElse(fail("missing queenside minority observation"))

    assertEquals(obs.status, StrategicConceptSemantics.ConceptStatus.Refuted)
    assert(obs.blockedReasons.contains("tactical_refutation"), clues(obs))
  }
