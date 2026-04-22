package lila.commentary.delta

import chess.Color
import chess.format.{ Fen, Uci }

import lila.commentary.root.{ RootAtomRegistry, RootStateVector }
import lila.commentary.strategic.{ StrategicObjectExtraction, StrategicObjectExtractor, StrategicObjectSet }
import lila.commentary.witness.WitnessSet

class StrategicDeltaBoundaryTest extends munit.FunSuite:

  test("StrategicDeltaScopeContract freezes both delta families together"):
    assertEquals(
      StrategicDeltaScopeContract.activeDeltaFamilyIds.map(_.value),
      Vector("TradeCompressionCorridor", "TradeInvariant")
    )

  test("StrategicDeltaExtractor emits TradeCompressionCorridor through both live entrypoints"):
    val move = moveUci("d4d5")
    val fromFens =
      StrategicDeltaExtractor
        .fromFens(
          Fen.Full.clean("4k3/3r4/8/3n4/3R4/8/8/4K3 w - - 0 1"),
          move,
          Fen.Full.clean("4k3/3r4/8/3R4/8/8/8/4K3 b - - 0 1")
        )
        .fold(message => fail(message), identity)
    val fromExtractions =
      StrategicDeltaExtractor
        .fromExtractions(fromFens.before, fromFens.after, move)
        .fold(message => fail(message), identity)

    assertEquals(fromExtractions, fromFens)
    assert(
      fromFens.deltas.contains(
        "TradeCompressionCorridor",
        lila.commentary.witness.WitnessAnchor.BoardAnchor,
        color = Some(Color.White),
        scope = Some(StrategicDeltaScope.MoveLocal),
        deltaTag = Some(StrategicDeltaTag("transition_compression"))
      )
    )

  test("StrategicDeltaExtractor emits TradeInvariant through both live entrypoints"):
    val move = moveUci("d6c7")
    val fromFens =
      StrategicDeltaExtractor
        .fromFens(
          Fen.Full.clean("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"),
          move,
          Fen.Full.clean("4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1")
        )
        .fold(message => fail(message), identity)
    val fromExtractions =
      StrategicDeltaExtractor
        .fromExtractions(fromFens.before, fromFens.after, move)
        .fold(message => fail(message), identity)

    assertEquals(fromExtractions, fromFens)
    assert(
      fromFens.deltas.contains(
        "TradeInvariant",
        lila.commentary.witness.WitnessAnchor.BoardAnchor,
        color = Some(Color.White),
        scope = Some(StrategicDeltaScope.MoveLocal),
        deltaTag = Some(StrategicDeltaTag("bounded_favorable_simplification"))
      )
    )

  test("StrategicDeltaExtractor rejects a board-coherent move when the side to move is wrong"):
    val result =
      StrategicDeltaExtractor.fromFens(
        Fen.Full.clean("4k3/8/8/8/8/8/4P3/4K3 b - - 0 1"),
        moveUci("e2e4"),
        Fen.Full.clean("4k3/8/8/8/4P3/8/8/4K3 w - - 0 1")
      )

    assert(
      result.left.exists(_.contains("wrong side to move")),
      clues(result)
    )

  test("StrategicDeltaExtractor accepts legal castling transitions with exact auxiliary state"):
    val result =
      StrategicDeltaExtractor.fromFensFailClosed(
        Fen.Full.clean("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"),
        moveUci("e1g1"),
        Fen.Full.clean("r3k2r/8/8/8/8/8/8/R4RK1 b kq - 1 1")
      )

    assert(result.isRight, clues(result))

  test("StrategicDeltaExtractor accepts legal black castling transitions with exact auxiliary state"):
    val result =
      StrategicDeltaExtractor.fromFensFailClosed(
        Fen.Full.clean("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1"),
        moveUci("e8g8"),
        Fen.Full.clean("r4rk1/8/8/8/8/8/8/R3K2R w KQ - 1 2")
      )

    assert(result.isRight, clues(result))

  test("StrategicDeltaExtractor accepts legal en passant transitions with exact auxiliary state"):
    val result =
      StrategicDeltaExtractor.fromFensFailClosed(
        Fen.Full.clean("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"),
        moveUci("e5d6"),
        Fen.Full.clean("4k3/8/3P4/8/8/8/8/4K3 b - - 0 1")
      )

    assert(result.isRight, clues(result))

  test("StrategicDeltaExtractor accepts legal black en passant transitions with exact auxiliary state"):
    val result =
      StrategicDeltaExtractor.fromFensFailClosed(
        Fen.Full.clean("4k3/8/8/8/3Pp3/8/8/4K3 b - d3 0 1"),
        moveUci("e4d3"),
        Fen.Full.clean("4k3/8/8/8/8/3p4/8/4K3 w - - 0 2")
      )

    assert(result.isRight, clues(result))

  test("StrategicDeltaExtractor rejects after-state auxiliary mismatches even when board squares match"):
    val result =
      StrategicDeltaExtractor.fromFens(
        Fen.Full.clean("4k3/8/8/8/3p4/8/4P3/4K3 w - - 0 1"),
        moveUci("e2e4"),
        Fen.Full.clean("4k3/8/8/8/3pP3/8/8/4K3 b - - 0 1")
      )

    assert(
      result.left.exists(_.contains("exact after-state mismatch")),
      clues(result)
    )

  test("StrategicDeltaExtractor fails closed on prebuilt extractions with impossible en passant history"):
    val invalidBeforeRoot = RootStateVector.fromIndices(
      Vector(
        RootAtomRegistry.pieceOnIndex(Color.White, chess.King, chess.Square.E1),
        RootAtomRegistry.pieceOnIndex(Color.Black, chess.King, chess.Square.E8),
        RootAtomRegistry.sideToMoveIndex(Color.White),
        RootAtomRegistry.castlingRightsIndex(0),
        RootAtomRegistry.enPassantIndex(Color.White, chess.File.D)
      )
    )
    val invalidBefore =
      StrategicObjectExtraction(
        rootState = invalidBeforeRoot,
        primaryWitnesses = WitnessSet.empty,
        attachedWitnesses = WitnessSet.empty,
        objects = StrategicObjectSet.empty
      )
    val validAfter =
      StrategicObjectExtractor
        .fromFen(Fen.Full.clean("4k3/8/8/8/8/8/4K3/8 b - - 0 1"))
        .fold(message => fail(message), identity)

    val result = StrategicDeltaExtractor.fromExtractions(invalidBefore, validAfter, moveUci("e1e2"))

    assert(
      result.left.exists(_.contains("Before exact position reconstruction failed: Illegal en-passant history state")),
      clues(result)
    )

  test("StrategicDeltaExtractor rejects forged canonical object carriers on raw extraction entrypoints"):
    val move = moveUci("d6c7")
    val canonical =
      StrategicDeltaExtractor
        .fromFens(
          Fen.Full.clean("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"),
          move,
          Fen.Full.clean("4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1")
        )
        .fold(message => fail(message), identity)
    val forgedBefore = canonical.before.copy(primaryWitnesses = WitnessSet.empty)

    val result = StrategicDeltaExtractor.fromExtractions(forgedBefore, canonical.after, move)

    assert(
      result.left.exists(_.contains("Before object extraction canonicalization failed")),
      clues(result)
    )

  private def moveUci(uci: String): Uci.Move =
    Uci(uci) match
      case Some(move: Uci.Move) => move
      case Some(_) => fail(s"Unsupported non-move UCI: $uci")
      case None => fail(s"Invalid UCI: $uci")
