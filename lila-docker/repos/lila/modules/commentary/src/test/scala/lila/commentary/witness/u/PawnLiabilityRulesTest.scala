package lila.commentary.witness.u

import chess.Color
import chess.Square

import lila.commentary.root.{ RootAtomRegistry, RootStateVector }
import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.witness.{ WitnessAnchor, WitnessSector }
import lila.commentary.witness.u.UWitnessTestSupport.*

class PawnLiabilityRulesTest extends munit.FunSuite:

  test("weak pawn target carries beneficiary polarity and weakness tags"):
    val fixedFen = "4k3/8/8/3p4/3P4/8/8/4K3 w - - 0 1"
    val passedFen = "4k3/8/8/8/2PP4/8/8/4K3 w - - 0 1"

    val witness =
      findPieceSquare(fixedFen, "weak_pawn_target_state", "d4", Some(Color.Black)).getOrElse(fail("missing weak-pawn target"))

    assert(tokenList(witness.payload, "weakness_tags").contains("fixed"))
    assert(findPieceSquare(passedFen, "weak_pawn_target_state", "d4", Some(Color.Black)).isEmpty)

  test("passed pawn entity stays owner-side and rejects candidate-only pawns"):
    val passedFen = "4k3/8/8/8/3P4/8/8/4K3 w - - 0 1"
    val candidateFen = "4k3/8/2p5/2P5/3P4/8/8/4K3 w - - 0 1"

    val witness =
      findPieceSquare(passedFen, "passed_pawn_entity_state", "d4", Some(Color.White)).getOrElse(fail("missing passed-pawn entity"))

    assertEquals(colorValue(witness.payload, "owner"), Some(Color.White))
    assert(findPieceSquare(candidateFen, "passed_pawn_entity_state", "d4", Some(Color.White)).isEmpty)

  test("sector asymmetry emits only on unequal sector pawn counts"):
    val positiveFen = "4k3/pp6/8/8/8/8/PPP5/4K3 w - - 0 1"
    val equalFen = "4k3/pp6/8/8/8/8/PP6/4K3 w - - 0 1"

    val witness =
      findSector(positiveFen, "sector_asymmetry_state", WitnessSector.Queenside).getOrElse(fail("missing queenside asymmetry"))

    assertEquals(witness.color, None)
    assertEquals(colorValue(witness.payload, "majority_side"), Some(Color.White))
    assertEquals(colorValue(witness.payload, "minority_side"), Some(Color.Black))
    assert(findSector(equalFen, "sector_asymmetry_state", WitnessSector.Queenside).isEmpty)

  test("available lever trigger emits single and double push variants without capture-only inflation"):
    val bothFen = "4k3/8/8/4p3/2p5/8/3P4/4K3 w - - 0 1"
    val captureOnlyFen = "4k3/8/8/8/8/1p6/2P5/4K3 w - - 0 1"

    val witnesses = descriptor(bothFen, "available_lever_trigger").filter(_.anchor == WitnessAnchor.PieceSquareAnchor(chess.Square.fromKey("d2").get))
    val variants = witnesses.flatMap(_.variant.map(_.value)).sorted

    assertEquals(variants, Vector("double_push_lever_state", "single_push_lever_state"))
    assert(witnesses.forall(_.color.contains(Color.White)))
    assert(findPieceSquare(captureOnlyFen, "available_lever_trigger", "c2", Some(Color.White)).isEmpty)

  test("pawn push break contact source requires a strategic target, not just any lever"):
    val positiveFen = "4k3/8/8/8/3p4/8/2P5/4K3 w - - 0 1"
    val negativeFen = "4k3/8/8/8/ppp5/8/2P5/4K3 w - - 0 1"

    val witness =
      findPieceSquare(positiveFen, "pawn_push_break_contact_source", "c2", Some(Color.White)).getOrElse(fail("missing pawn-push contact source"))

    val variantPayloads = witness.payload.get("contact_variants").get
    assert(variantPayloads.isInstanceOf[lila.commentary.witness.WitnessValue.ListValue])
    assert(findPieceSquare(negativeFen, "pawn_push_break_contact_source", "c2", Some(Color.White)).isEmpty)

  test("forged lever roots still reject illegal push-contact variants"):
    val pinnedFen = "7k/8/8/7b/3p4/8/4P3/3K4 w - - 0 1"
    val anchorSquare = Square.fromKey("e2").get
    val forgedRoots =
      RootStateVector.fromIndices(
        rootState(pinnedFen).activeIndices :+
          RootAtomRegistry.colorPawnSquareIndex(SchemaId.LeverAvailable, Color.White, anchorSquare).get
      )

    val leverWitnesses =
      descriptor(forgedRoots, "available_lever_trigger").filter(_.anchor == WitnessAnchor.PieceSquareAnchor(anchorSquare))
    val breakContactWitnesses =
      descriptor(forgedRoots, "pawn_push_break_contact_source").filter(_.anchor == WitnessAnchor.PieceSquareAnchor(anchorSquare))

    assert(leverWitnesses.isEmpty)
    assert(breakContactWitnesses.isEmpty)
