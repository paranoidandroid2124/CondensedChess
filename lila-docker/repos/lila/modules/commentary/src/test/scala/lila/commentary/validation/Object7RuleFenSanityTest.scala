package lila.commentary.validation

import scala.concurrent.duration.*

class Object7RuleFenSanityTest extends munit.FunSuite:

  private val corpusFens = Object7FenAuditReport.corpusRows.map(_.fen).toSet
  private val ruleOnlyRows =
    Object7FenAuditReport.ruleTestRows.filterNot(row => corpusFens.contains(row.fen))

  test("Object 7 rule-test-only FENs survive a local Stockfish smoke check"):
    assert(ruleOnlyRows.nonEmpty)

    ruleOnlyRows
      .groupBy(_.fen)
      .values
      .map(_.head)
      .toVector
      .sortBy(row => (row.family, row.id))
      .foreach: row =>
        val probe = StockfishProbe.probeFen(row.fen, 150.millis)
        assert(probe.bestMove.nonEmpty, s"${row.id} produced no best move")
        assert(probe.cp.nonEmpty || probe.mate.nonEmpty, s"${row.id} produced no score line")
