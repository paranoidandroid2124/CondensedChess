package lila.llm.analysis

import munit.FunSuite

class CommentaryOpsBoardTest extends FunSuite:

  test("recent samples returns newest entries first within the requested limit") {
    val board = new CommentaryOpsBoard(maxSamples = 3)

    board.recordSample("first", Map("a" -> "1"))
    board.recordSample("second", Map("b" -> "2"))
    board.recordSample("third", Map("c" -> "3"))
    board.recordSample("fourth", Map("d" -> "4"))

    val samples = board.recentSamples(limit = 2)

    assertEquals(samples.map(_.kind), List("fourth", "third"))
    assertEquals(samples.head.fields.get("d"), Some("4"))
  }
end CommentaryOpsBoardTest
