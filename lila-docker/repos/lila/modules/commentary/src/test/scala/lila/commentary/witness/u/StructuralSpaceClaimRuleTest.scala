package lila.commentary.witness.u

import chess.{ Bishop, Color, Pawn, Square }
import chess.format.Fen

import lila.commentary.root.{ RootAtomRegistry, RootStateVector }
import lila.commentary.witness.{ WitnessAnchor, WitnessDescriptorId, WitnessSector, WitnessVariantId }
import lila.commentary.witness.u.UWitnessTestSupport.{ colorValue, squareList }

class StructuralSpaceClaimRuleTest extends munit.FunSuite:

  private val descriptorId = WitnessDescriptorId("structural_space_claim")

  test("structural space claim emits a closed-center host witness only for empty attached space beyond a true two-file center lock"):
    val fen = Fen.Full.clean("4k3/8/8/3ppN2/3PP3/7B/8/4K3 w - - 0 1")
    val extraction = UAttachedExtractor.fromFen(fen).fold(message => fail(message), identity)

    val whiteClaim =
      extraction.witnesses.forDescriptorId(descriptorId).find: witness =>
        witness.anchor == WitnessAnchor.SectorAnchor(WitnessSector.Center) &&
          witness.color.contains(Color.White) &&
          witness.variant.contains(WitnessVariantId("closed_center_host"))

    assert(whiteClaim.nonEmpty)
    assertEquals(
      squareList(whiteClaim.get.payload, "claimed_squares").map(_.key),
      Vector("d6", "e7")
    )
    assertEquals(
      squareList(whiteClaim.get.payload, "boundary_pawn_squares").map(_.key),
      Vector("d4", "e4", "d5", "e5")
    )
    assertEquals(
      extraction.witnesses.forDescriptorId(descriptorId).count(_.variant.contains(WitnessVariantId("closed_center_host"))),
      1
    )

  test("structural space claim rejects occupied frontier squares as claimed space"):
    val fen = Fen.Full.clean("4k3/8/8/3pp3/3PP3/8/8/4K3 w - - 0 1")
    val extraction = UAttachedExtractor.fromFen(fen).fold(message => fail(message), identity)

    assertEquals(extraction.witnesses.forDescriptorId(descriptorId), Vector.empty)

  test("structural space claim rejects a lone locked center file as a closed-center host"):
    val fen = Fen.Full.clean("4k3/8/8/3p1N2/3P4/8/8/4K3 w - - 0 1")
    val extraction = UAttachedExtractor.fromFen(fen).fold(message => fail(message), identity)

    assertEquals(
      extraction.witnesses.forDescriptorId(descriptorId).filter(
        _.variant.contains(WitnessVariantId("closed_center_host"))
      ),
      Vector.empty
    )

  test("structural space claim selects the live fixed-chain segment when multiple same-sector segments exist"):
    val fen = Fen.Full.clean("k7/6p1/5pP1/5P1N/2B3p1/5pP1/5P2/K7 w - - 0 1")
    val extraction = UAttachedExtractor.fromFen(fen).fold(message => fail(message), identity)

    val whiteClaim =
      extraction.witnesses.forDescriptorId(descriptorId).find: witness =>
        witness.anchor == WitnessAnchor.SectorAnchor(WitnessSector.Kingside) &&
          witness.color.contains(Color.White) &&
          witness.variant.contains(WitnessVariantId("fixed_chain_host_white_segment"))

    assert(whiteClaim.nonEmpty)
    assertEquals(colorValue(whiteClaim.get.payload, "host_owner"), Some(Color.White))
    assertEquals(
      squareList(whiteClaim.get.payload, "boundary_pawn_squares").map(_.key),
      Vector("f5", "g6")
    )
    assert(squareList(whiteClaim.get.payload, "claimed_squares").map(_.key).contains("f7"))
    assert(squareList(whiteClaim.get.payload, "claimed_squares").map(_.key).contains("g8"))

  test("structural space claim emits the black fixed-chain host variant with owner-specific payload"):
    val fen = Fen.Full.clean("4k3/8/8/3p1n2/3Ppn2/4P3/8/4K3 b - - 0 1")
    val extraction = UAttachedExtractor.fromFen(fen).fold(message => fail(message), identity)

    val blackClaim =
      extraction.witnesses.forDescriptorId(descriptorId).find: witness =>
        witness.anchor == WitnessAnchor.SectorAnchor(WitnessSector.Center) &&
          witness.color.contains(Color.Black) &&
          witness.variant.contains(WitnessVariantId("fixed_chain_host_black_segment"))

    assert(blackClaim.nonEmpty)
    assertEquals(colorValue(blackClaim.get.payload, "host_owner"), Some(Color.Black))
    assertEquals(
      squareList(blackClaim.get.payload, "boundary_pawn_squares").map(_.key),
      Vector("e4", "d5")
    )
    assertEquals(
      squareList(blackClaim.get.payload, "claimed_squares").map(_.key),
      Vector("e2", "d3")
    )

  test("structural space claim rejects a branched fixed-pawn cluster that is not a rear-supported segment"):
    val fen = Fen.Full.clean("4k3/8/6p1/5pPp/5P1P/3B4/8/4K3 w - - 0 1")
    val extraction = UAttachedExtractor.fromFen(fen).fold(message => fail(message), identity)

    val whiteClaim =
      extraction.witnesses.forDescriptorId(descriptorId).find: witness =>
        witness.anchor == WitnessAnchor.SectorAnchor(WitnessSector.Kingside) &&
          witness.color.contains(Color.White) &&
          witness.variant.contains(WitnessVariantId("fixed_chain_host_white_segment"))

    assertEquals(whiteClaim, None)

  test("structural space claim rejects a single frontier seed with no connected square set"):
    val fen = Fen.Full.clean("4k3/8/8/3p4/2Bp4/8/8/4K3 w - - 0 1")
    val extraction = UAttachedExtractor.fromFen(fen).fold(message => fail(message), identity)

    assertEquals(extraction.witnesses.forDescriptorId(descriptorId), Vector.empty)

  test("structural space claim keeps one strongest connected attached component instead of unioning disconnected islands"):
    val fen = Fen.Full.clean("k7/6p1/5pP1/5P2/2BBN3/5N2/8/K7 w - - 0 1")
    val extraction = UAttachedExtractor.fromFen(fen).fold(message => fail(message), identity)

    val whiteClaim =
      extraction.witnesses.forDescriptorId(descriptorId).find: witness =>
        witness.anchor == WitnessAnchor.SectorAnchor(WitnessSector.Kingside) &&
          witness.color.contains(Color.White) &&
          witness.variant.contains(WitnessVariantId("fixed_chain_host_white_segment"))

    assert(whiteClaim.nonEmpty)
    assertEquals(
      squareList(whiteClaim.get.payload, "claimed_squares").map(_.key),
      Vector("f1", "g1", "f2", "h2", "g3", "h4", "g5")
    )
    assert(!squareList(whiteClaim.get.payload, "claimed_squares").map(_.key).contains("f7"))

  test("u attached extractor fail-closes malformed root state during attached extraction"):
    val malformedRootState = RootStateVector.fromIndices(
      Vector(
        RootAtomRegistry.pieceOnIndex(Color.White, Pawn, Square.D4),
        RootAtomRegistry.pieceOnIndex(Color.White, Bishop, Square.D4)
      )
    )

    assert(UAttachedExtractor.fromRootFailClosed(malformedRootState).isLeft)

  test("u attached extractor fail-closed path preserves exact-board input discipline"):
    val illegalFen = Fen.Full.clean("8/8/8/8/8/8/4k3/4K3 w - - 0 1")

    assert(UAttachedExtractor.fromFenFailClosed(illegalFen).isLeft)
