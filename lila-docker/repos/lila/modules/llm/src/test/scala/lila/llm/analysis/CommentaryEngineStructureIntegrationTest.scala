package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.strategic.VariationLine

class CommentaryEngineStructureIntegrationTest extends FunSuite:

  test("assessExtended exposes structure profile and plan alignment") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val vars = List(
      VariationLine(
        moves = List("e2e4", "e7e5", "g1f3"),
        scoreCp = 20,
        mate = None,
        depth = 18
      )
    )

    val data = CommentaryEngine
      .assessExtended(
        fen = fen,
        variations = vars,
        playedMove = Some("e2e4"),
        phase = Some("opening"),
        ply = 1,
        prevMove = Some("e2e4")
      )
      .getOrElse(fail("assessExtended returned None"))

    assert(data.structureProfile.isDefined, clues(data.structureProfile))
    assert(data.planAlignment.isDefined, clues(data.planAlignment))
    assert(data.integratedContext.flatMap(_.structureProfile).isDefined, clues(data.integratedContext))
    assert(data.integratedContext.flatMap(_.planAlignment).isDefined, clues(data.integratedContext))
  }
