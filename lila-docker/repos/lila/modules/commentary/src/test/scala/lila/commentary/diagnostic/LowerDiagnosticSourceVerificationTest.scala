package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class LowerDiagnosticSourceVerificationTest extends munit.FunSuite:

  test("PGN source verification proves a unique transition against the source game"):
    val pgn = Files.createTempFile("source-verification", ".pgn")
    val input = Files.createTempFile("source-verification", ".jsonl")
    try
      Files.writeString(
        pgn,
        """[Event "Source verification"]
          |[Site "?"]
          |[Date "2026.04.30"]
          |[Round "?"]
          |[White "White"]
          |[Black "Black"]
          |[Result "*"]
          |
          |1. e4 e5 *
          |""".stripMargin,
        StandardCharsets.UTF_8
      )
      Files.writeString(
        input,
        s"""{"sampleId":"sample:e4:a","gameKey":"game-e4","pgnPath":"${escape(pgn.toString)}","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","beforeFen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","currentFen":"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1","ply":1,"playedUci":"e2e4","playedMove":"e2e4","family":"FixedTargetComplex"}""" + System.lineSeparator() +
          s"""{"sampleId":"sample:e4:b","gameKey":"game-e4","pgnPath":"${escape(pgn.toString)}","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","beforeFen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","currentFen":"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1","ply":1,"playedUci":"e2e4","playedMove":"e2e4","family":"AccessNetwork"}""" + System.lineSeparator(),
        StandardCharsets.UTF_8
      )

      val report = LowerDiagnosticSourceVerificationReport.fromRows(LowerDiagnosticLargeCorpus.loadExternalRows(input))

      assertEquals(report.summary.totalRows, 2)
      assertEquals(report.summary.uniqueTransitions, 1)
      assertEquals(report.summary.verifiedTransitions, 1)
      assertEquals(report.rows.head.sourceState, "verified")
      assertEquals(report.rows.head.duplicateRows, 2)
    finally
      Files.deleteIfExists(input)
      Files.deleteIfExists(pgn)

  test("PGN source verification keeps missing source files separate from replay acceptance"):
    val input = Files.createTempFile("source-verification-missing", ".jsonl")
    try
      Files.writeString(
        input,
        """{"sampleId":"sample:missing","gameKey":"game-missing","pgnPath":"C:\\missing\\source.pgn","beforeFen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","currentFen":"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1","ply":1,"playedUci":"e2e4","playedMove":"e2e4","family":"FixedTargetComplex"}""" + System.lineSeparator(),
        StandardCharsets.UTF_8
      )

      val report = LowerDiagnosticSourceVerificationReport.fromRows(LowerDiagnosticLargeCorpus.loadExternalRows(input))

      assertEquals(report.summary.uniqueTransitions, 1)
      assertEquals(report.summary.verifiedTransitions, 0)
      assertEquals(report.summary.countsByState("missing_pgn_file"), 1)
      assertEquals(report.rows.head.sourceState, "missing_pgn_file")
    finally Files.deleteIfExists(input)

  test("PGN source verification can remap missing original paths through a recovered corpus root"):
    val root = Files.createTempDirectory("source-verification-root")
    val pgn = root.resolve("recovered.pgn")
    val input = Files.createTempFile("source-verification-remap", ".jsonl")
    try
      Files.writeString(
        pgn,
        """[Event "Source verification"]
          |
          |1. e4 e5 *
          |""".stripMargin,
        StandardCharsets.UTF_8
      )
      Files.writeString(
        input,
        """{"sampleId":"sample:remap","gameKey":"game-remap","pgnPath":"C:\\missing\\old\\recovered.pgn","beforeFen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","currentFen":"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1","ply":1,"playedUci":"e2e4","playedMove":"e2e4","family":"FixedTargetComplex"}""" + System.lineSeparator(),
        StandardCharsets.UTF_8
      )

      val report = LowerDiagnosticSourceVerificationReport.fromRows(
        LowerDiagnosticLargeCorpus.loadExternalRows(input),
        pgnRoots = Vector(root)
      )

      assertEquals(report.summary.verifiedTransitions, 1)
      assertEquals(report.rows.head.sourceState, "verified")
    finally
      Files.deleteIfExists(input)
      Files.deleteIfExists(pgn)
      Files.deleteIfExists(root)

  test("PGN source verification rejects conflicting duplicate transition groups"):
    val pgn = Files.createTempFile("source-verification-conflict", ".pgn")
    val input = Files.createTempFile("source-verification-conflict", ".jsonl")
    try
      Files.writeString(
        pgn,
        """[Event "Source verification"]
          |
          |1. e4 e5 *
          |""".stripMargin,
        StandardCharsets.UTF_8
      )
      Files.writeString(
        input,
        s"""{"sampleId":"sample:e4:a","gameKey":"game-e4","pgnPath":"${escape(pgn.toString)}","beforeFen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","currentFen":"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1","ply":1,"playedUci":"e2e4","playedMove":"e2e4","family":"FixedTargetComplex"}""" + System.lineSeparator() +
          s"""{"sampleId":"sample:e4:b","gameKey":"game-e4","pgnPath":"${escape(pgn.toString)}","beforeFen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","currentFen":"rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1","ply":1,"playedUci":"e2e4","playedMove":"e2e4","family":"AccessNetwork"}""" + System.lineSeparator(),
        StandardCharsets.UTF_8
      )

      val report = LowerDiagnosticSourceVerificationReport.fromRows(LowerDiagnosticLargeCorpus.loadExternalRows(input))

      assertEquals(report.summary.verifiedTransitions, 0)
      assertEquals(report.rows.head.sourceState, "current_fen_conflict")
    finally
      Files.deleteIfExists(input)
      Files.deleteIfExists(pgn)

  private def escape(value: String): String =
    value.replace("\\", "\\\\")
