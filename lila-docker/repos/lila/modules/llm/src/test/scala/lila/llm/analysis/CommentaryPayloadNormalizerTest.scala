package lila.llm.analysis

import munit.FunSuite

class CommentaryPayloadNormalizerTest extends FunSuite:

  test("normalizer unwraps commentary json object") {
    val raw = """{"commentary":"1. e4: The opening remains fluid.\n\nWhite develops quickly."}"""
    val normalized = CommentaryPayloadNormalizer.normalize(raw)
    assertEquals(normalized, "1. e4: The opening remains fluid.\n\nWhite develops quickly.")
  }

  test("normalizer unwraps fenced commentary payload") {
    val raw =
      """```json
        |{"commentary":"9. cxd5: The pawn on c4 is hanging."}
        |```""".stripMargin
    val normalized = CommentaryPayloadNormalizer.normalize(raw)
    assertEquals(normalized, "9. cxd5: The pawn on c4 is hanging.")
  }

  test("normalizer unwraps quoted nested commentary payload") {
    val raw =
      "\"{\\\"commentary\\\":\\\"7. c3: The move extends Ruy Lopez ideas toward preparing d4.\\\"}\""
    val normalized = CommentaryPayloadNormalizer.normalize(raw)
    assertEquals(normalized, "7. c3: The move extends Ruy Lopez ideas toward preparing d4.")
  }
