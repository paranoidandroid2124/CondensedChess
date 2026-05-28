package lila.commentary.analysis

import java.nio.file.{ Files, Paths }

import lila.commentary.model.strategic.{ VariationLine, VariationTag }
import munit.FunSuite

class ForcedLineTruthTest extends FunSuite:

  test("scholars mate is not detected from legal continuation alone after e4 e5") {
    val fenAfterE4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"

    assertEquals(
      ForcedLineTruth.detect(
        fen = fenAfterE4,
        playedUci = "e7e5",
        ply = 1,
        variations = Nil
      ),
      None
    )
  }

  test("scholars mate requires the played trap entry and a coupled PV line") {
    val fenAfterE4E5 = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"
    val scholarPv =
      VariationLine(
        moves = List("d1h5", "b8c6", "f1c4", "g8f6", "h5f7"),
        scoreCp = 0,
        mate = Some(1),
        depth = 18,
        tags = List(VariationTag.Forced)
      )

    assertEquals(
      ForcedLineTruth
        .detect(
          fen = fenAfterE4E5,
          playedUci = "d1h5",
          ply = 3,
          variations = List(scholarPv)
        )
        .map(_.id),
      Some("scholars_mate")
    )
  }

  test("greek gift recognizes a bishop sacrifice on h7 with kingside support") {
    val fen = "6k1/6pp/8/6NQ/8/3B4/8/4K3 w - - 0 1"

    assertEquals(
      ForcedLineTruth.detect(fen, "d3h7", ply = 1).map(_.id),
      Some("greek_gift")
    )
  }

  test("greek gift recognizes Bxh7+ when support appears only through PV replay") {
    val fen = "6k1/7p/8/8/8/3B1N2/8/3QK3 w - - 0 1"
    val greekGiftPv =
      VariationLine(
        moves = List("d3h7", "g8h7", "f3g5", "h7g8", "d1h5"),
        scoreCp = 120,
        depth = 18,
        tags = List(VariationTag.Forced)
      )

    assertEquals(
      ForcedLineTruth.detect(fen, "d3h7", ply = 1, variations = List(greekGiftPv)).map(_.id),
      Some("greek_gift")
    )
  }

  test("greek gift does not recognize a queen check on h7") {
    val fen = "6k1/6pp/8/8/8/3Q4/8/4K3 w - - 0 1"

    assertEquals(
      ForcedLineTruth.detect(fen, "d3h7", ply = 1).map(_.id),
      None
    )
  }

  test("win-material sequence requires material gain for the final mover") {
    val fen = "4k3/8/8/8/8/8/r3Q3/4K3 w - - 0 1"

    assert(
      ForcedLineTruth.verifySequence(
        fen,
        List("e1f1", "a2e2"),
        ForcedLineTruth.ExpectedResult.WinMaterial
      )
    )
  }

  test("legal non-material line is not enough for win-material truth") {
    val fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"

    assert(
      !ForcedLineTruth.verifySequence(
        fen,
        List("e2e3", "e8e7"),
        ForcedLineTruth.ExpectedResult.WinMaterial
      )
    )
  }

  test("pattern detection is delegated to tactical detector implementations") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/ForcedLineTruth.scala"))

    assert(source.contains("TacticalPatternDetectors"), clue(source))
    assert(!source.contains("private def isSmotheredMate"), clue(source))
    assert(!source.contains("private def isAnastasiaMate"), clue(source))
  }
