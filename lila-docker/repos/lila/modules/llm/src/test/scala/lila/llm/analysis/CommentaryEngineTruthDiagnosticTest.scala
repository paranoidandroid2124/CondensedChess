package lila.llm.analysis

import munit.FunSuite

import lila.llm.model.strategic.VariationLine

class CommentaryEngineTruthDiagnosticTest extends FunSuite:

  private val samplePgn =
    """[Event "Diagnostic Sample"]
      |[Site "?"]
      |[Date "2026.03.25"]
      |[Round "1"]
      |[White "White"]
      |[Black "Black"]
      |[Result "*"]
      |
      |1. e4 e5 2. Nf3 Nc6 *
      |""".stripMargin

  private val evals =
    Map(
      1 -> List(VariationLine(moves = List("e7e5"), scoreCp = 20)),
      2 -> List(VariationLine(moves = List("g1f3"), scoreCp = 18)),
      3 -> List(VariationLine(moves = List("b8c6"), scoreCp = 12)),
      4 -> List(VariationLine(moves = List("f1b5"), scoreCp = -360))
    )

  test("generateGameArcDiagnostic keeps canonical visible traces and separate witness traces") {
    val diagnostic =
      CommentaryEngine.generateGameArcDiagnostic(
        pgn = samplePgn,
        evals = evals,
        diagnosticWitnesses = List(
          CommentaryEngine.DiagnosticWitness(
            ply = 2,
            momentType = Some("TensionPeak"),
            label = Some("Witness ply")
          )
        )
      )

    assert(diagnostic.arc.keyMomentNarratives.nonEmpty, clue(diagnostic))
    val visiblePlies = diagnostic.arc.keyMomentNarratives.map(_.ply).toSet
    assert(visiblePlies.subsetOf(diagnostic.canonicalTraceMoments.map(_.ply).toSet), clue(diagnostic))
    assert(
      diagnostic.canonicalTraceMoments.exists(trace =>
        visiblePlies.contains(trace.ply) &&
          trace.traceSource == "canonical_internal" &&
          trace.finalInternal
      ),
      clue(diagnostic.canonicalTraceMoments)
    )
    assert(
      diagnostic.witnessTraceMoments.exists(trace =>
        trace.ply == 2 &&
          trace.traceSource == "diagnostic_witness" &&
          trace.selectionKind == "diagnostic_witness" &&
          !trace.finalInternal
      ),
      clue(diagnostic.witnessTraceMoments)
    )
  }
