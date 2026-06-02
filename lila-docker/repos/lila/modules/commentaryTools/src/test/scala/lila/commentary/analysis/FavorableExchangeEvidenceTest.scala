package lila.commentary.analysis

import chess.{ Bishop, Knight, Queen, Rook, Square }
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

final class FavorableExchangeEvidenceTest extends FunSuite:

  private def position(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).getOrElse(fail(s"invalid FEN: $fen"))

  test("queen trade candidates require queens and a recapture-like defender") {
    val pos = position("3r4/4k3/8/3q4/8/8/8/3QK3 w - - 0 1")
    val moves =
      FavorableExchangeEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.QueenTradeShield,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Queen && mv.orig == Square.D1 && mv.dest == Square.D5), clue(moves.map(_.toUci.uci)))
  }

  test("queen trade candidates do not relabel a minor-piece queen win") {
    val pos = position("4k3/8/8/3q4/8/2N5/8/4K3 w - - 0 1")
    val moves =
      FavorableExchangeEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.QueenTradeShield,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("queen trade candidates do not relabel an undefended queen capture") {
    val pos = position("4k3/8/8/3q4/8/8/8/3QK3 w - - 0 1")
    val moves =
      FavorableExchangeEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.QueenTradeShield,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("defender trade candidates capture a defender of a real weakness target") {
    val pos = position("4k3/8/8/4p3/2n5/8/8/4KB2 w - - 0 1")
    val moves =
      FavorableExchangeEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.DefenderTrade,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Bishop && mv.orig == Square.F1 && mv.dest == Square.C4), clue(moves.map(_.toUci.uci)))
  }

  test("simplification candidates are concrete favorable captures") {
    val pos = position("r3k3/8/2b5/8/8/8/8/R3K3 w - - 0 1")
    val moves =
      FavorableExchangeEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.SimplificationWindow,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Rook && mv.orig == Square.A1 && mv.dest == Square.A8), clue(moves.map(_.toUci.uci)))
  }

  test("simplification candidates do not relabel a large tactical material win") {
    val pos = position("4k3/8/8/3q4/8/2N5/8/4K3 w - - 0 1")
    val moves =
      FavorableExchangeEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.SimplificationWindow,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("bad-piece liquidation candidates use a low-mobility piece capture") {
    val pos = position("4k3/8/8/8/8/1n6/8/N3K3 w - - 0 1")
    val moves =
      FavorableExchangeEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.BadPieceLiquidation,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Knight && mv.orig == Square.A1 && mv.dest == Square.B3), clue(moves.map(_.toUci.uci)))
  }

  test("favorable exchange subplans do not emit quiet rook moves without a capture") {
    val pos = position("4k3/8/8/8/8/8/8/R3K3 w - - 0 1")
    val moves =
      FavorableExchangeEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.SimplificationWindow,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("plan-first proposals can emit typed queen trade subplans") {
    val fen = "3r4/4k3/8/3q4/8/8/8/3QK3 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 32,
        ctx = ctx,
        maxItems = 5
      )

    assert(
      hypotheses.exists(h =>
        h.subplanId.contains(PlanTaxonomy.PlanKind.QueenTradeShield.id) &&
          h.evidenceSources.exists(_.startsWith("recapture_defenders:"))
      ),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }
