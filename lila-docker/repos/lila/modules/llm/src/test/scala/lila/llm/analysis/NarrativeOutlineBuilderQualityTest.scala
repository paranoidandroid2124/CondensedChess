package lila.llm.analysis

import chess.{ Color, Pawn, Square }
import lila.llm.model.*
import lila.llm.model.authoring.OutlineBeatKind
import lila.llm.model.strategic.{ CounterfactualMatch, EngineEvidence, VariationLine }
import munit.FunSuite

class NarrativeOutlineBuilderQualityTest extends FunSuite:

  test("context starts with a concrete board anchor when a key fact exists"):
    val ctx = baseContext(
      playedMove = Some("e2e4"),
      playedSan = Some("e4"),
      facts = List(
        Fact.HangingPiece(
          square = Square.E4,
          role = Pawn,
          attackers = List(Square.D5),
          defenders = Nil,
          scope = FactScope.Now
        )
      ),
      threats = ThreatTable(Nil, Nil)
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val context = outline.beats.find(_.kind == OutlineBeatKind.Context).map(_.text).getOrElse("")
    val firstSentence = context.split("(?<=[.!?])\\s+").headOption.getOrElse("").toLowerCase

    assert(firstSentence.contains("e4"), clue(firstSentence))
    assert(!firstSentence.startsWith("the opening"), clue(firstSentence))

  test("bad annotation explains cause, consequence, and better alternative"):
    val badMove = CandidateInfo(
      move = "h3",
      uci = Some("h2h3"),
      annotation = "?",
      planAlignment = "Quiet move",
      downstreamTactic = None,
      tacticalAlert = Some("...Qh4 creates direct king pressure"),
      practicalDifficulty = "clean",
      whyNot = Some("it weakens dark squares around the king"),
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = List("Qh4"),
      facts = List(Fact.WeakSquare(Square.G3, Color.White, "dark-square holes", FactScope.Now))
    )

    val bestMove = CandidateInfo(
      move = "Nf3",
      uci = Some("g1f3"),
      annotation = "!",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = None,
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil
    )

    val ctx = baseContext(
      playedMove = Some("h2h3"),
      playedSan = Some("h3"),
      candidates = List(bestMove, badMove),
      threats = ThreatTable(
        List(
          ThreatRow(
            kind = "Mate",
            side = "US",
            square = Some("h2"),
            lossIfIgnoredCp = 220,
            turnsToImpact = 2,
            bestDefense = Some("Nf3"),
            defenseCount = 1,
            insufficientData = false
          )
        ),
        Nil
      ),
      counterfactual = Some(
        CounterfactualMatch(
          userMove = "h3",
          bestMove = "Nf3",
          cpLoss = 180,
          missedMotifs = Nil,
          userMoveMotifs = Nil,
          severity = "mistake",
          userLine = VariationLine(moves = List("h2h3", "d7d5"), scoreCp = -130)
        )
      ),
      engineEvidence = Some(
        EngineEvidence(
          depth = 12,
          variations = List(
            VariationLine(moves = List("g1f3", "d7d5"), scoreCp = 40),
            VariationLine(moves = List("h2h3", "d7d5"), scoreCp = -130)
          )
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val main = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).getOrElse("")
    val lower = main.toLowerCase

    assert(lower.contains("issue:"), clue(main))
    assert(lower.contains("consequence:"), clue(main))
    val hasCausalBridge = List("therefore", "as a result", "for that reason", "so ").exists(lower.contains)
    assert(hasCausalBridge, clue(main))
    assert(
      main.contains("Better is **Nf3**") || main.contains("Better is **f3**"),
      clue(main)
    )

  test("rank-2 low-loss move is not labeled as the strongest continuation"):
    val bestMove = CandidateInfo(
      move = "Nf3",
      uci = Some("g1f3"),
      annotation = "!",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = None,
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil
    )
    val secondMove = CandidateInfo(
      move = "Nc3",
      uci = Some("b1c3"),
      annotation = "",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = Some("it is slightly less accurate"),
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil
    )

    val ctx = baseContext(
      playedMove = Some("b1c3"),
      playedSan = Some("Nc3"),
      candidates = List(bestMove, secondMove),
      threats = ThreatTable(Nil, Nil),
      counterfactual = Some(
        CounterfactualMatch(
          userMove = "Nc3",
          bestMove = "Nf3",
          cpLoss = 20,
          missedMotifs = Nil,
          userMoveMotifs = Nil,
          severity = "ok",
          userLine = VariationLine(moves = List("b1c3", "d7d5"), scoreCp = 20)
        )
      ),
      engineEvidence = Some(
        EngineEvidence(
          depth = 12,
          variations = List(
            VariationLine(moves = List("g1f3", "d7d5"), scoreCp = 40),
            VariationLine(moves = List("b1c3", "d7d5"), scoreCp = 20)
          )
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val main = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).getOrElse("")
    val lower = main.toLowerCase

    assert(!lower.contains("strongest continuation"), clue(main))
    assert(!lower.contains("fully sound"), clue(main))
    assert(main.contains("**Nc3**"), clue(main))

  test("high cp-loss annotation explicitly carries negative polarity"):
    val bestMove = CandidateInfo(
      move = "Nf3",
      uci = Some("g1f3"),
      annotation = "!",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = None,
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil
    )
    val badMove = CandidateInfo(
      move = "h3",
      uci = Some("h2h3"),
      annotation = "?",
      planAlignment = "Quiet move",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = Some("it wastes a critical tempo"),
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil
    )

    val ctx = baseContext(
      playedMove = Some("h2h3"),
      playedSan = Some("h3"),
      candidates = List(bestMove, badMove),
      threats = ThreatTable(Nil, Nil),
      counterfactual = Some(
        CounterfactualMatch(
          userMove = "h3",
          bestMove = "Nf3",
          cpLoss = 180,
          missedMotifs = Nil,
          userMoveMotifs = Nil,
          severity = "mistake",
          userLine = VariationLine(moves = List("h2h3", "d7d5"), scoreCp = -140)
        )
      ),
      engineEvidence = Some(
        EngineEvidence(
          depth = 12,
          variations = List(
            VariationLine(moves = List("g1f3", "d7d5"), scoreCp = 40),
            VariationLine(moves = List("h2h3", "d7d5"), scoreCp = -140)
          )
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val main = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).getOrElse("")
    val lower = main.toLowerCase

    val hasNegativePolarity = List("blunder", "mistake", "inaccuracy").exists(lower.contains)
    assert(hasNegativePolarity, clue(main))
    assert(!lower.contains("this is a mistake that gives the opponent easier play"), clue(main))

  test("concept-only motif does not force a prefix without corroboration"):
    val seed = baseContext(
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3"),
      threats = ThreatTable(Nil, Nil)
    )
    val ctx = seed.copy(
      semantic = seed.semantic.map(_.copy(conceptSummary = List("OpenFile")))
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val context = outline.beats.find(_.kind == OutlineBeatKind.Context).map(_.text).getOrElse("")
    val lower = context.toLowerCase

    assert(!lower.contains("open-file control is a central strategic objective"), clue(context))
    assert(!lower.contains("the open file is the key channel for major-piece activity"), clue(context))
    assert(!lower.contains("file control along the open line can dictate the middlegame"), clue(context))

  test("delta-confirmed motif can still drive prefix"):
    val seed = baseContext(
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3"),
      threats = ThreatTable(Nil, Nil)
    )
    val ctx = seed.copy(
      semantic = seed.semantic.map(_.copy(conceptSummary = List("OpenFile"))),
      delta = Some(
        MoveDelta(
          evalChange = 0,
          newMotifs = List("OpenFile"),
          lostMotifs = Nil,
          structureChange = None,
          openFileCreated = None,
          phaseChange = None
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val context = outline.beats.find(_.kind == OutlineBeatKind.Context).map(_.text).getOrElse("")
    val lower = context.toLowerCase

    val hasOpenFilePrefix = List(
      "open-file control is a central strategic objective",
      "the open file is the key channel for major-piece activity",
      "file control along the open line can dictate the middlegame"
    ).exists(lower.contains)
    assert(hasOpenFilePrefix, clue(context))

  test("endgame context suppresses opening-side motif prefixes"):
    val seed = baseContext(
      playedMove = Some("e2e4"),
      playedSan = Some("e4"),
      threats = ThreatTable(Nil, Nil)
    )
    val ctx = seed.copy(
      phase = PhaseContext("Endgame", "Technical conversion", None),
      semantic = seed.semantic.map(_.copy(conceptSummary = List("minority_attack")))
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val context = outline.beats.find(_.kind == OutlineBeatKind.Context).map(_.text).getOrElse("")
    val lower = context.toLowerCase

    assert(!lower.contains("minority attack"), clue(context))

  test("theme reinforcement rotates wording for minority attack motifs"):
    val seed = baseContext(
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3"),
      threats = ThreatTable(Nil, Nil)
    )
    val semanticWithMinority = seed.semantic.map(_.copy(conceptSummary = List("minority_attack")))
    val ctx10 = seed.copy(ply = 20, semantic = semanticWithMinority)
    val ctx11 = seed.copy(ply = 21, semantic = semanticWithMinority)

    val (outline10, _) = NarrativeOutlineBuilder.build(ctx10, new TraceRecorder())
    val (outline11, _) = NarrativeOutlineBuilder.build(ctx11, new TraceRecorder())
    val context10 = outline10.beats.find(_.kind == OutlineBeatKind.Context).map(_.text).getOrElse("")
    val context11 = outline11.beats.find(_.kind == OutlineBeatKind.Context).map(_.text).getOrElse("")

    val s10 = context10
      .split("(?<=[.!?])\\s+")
      .find(_.toLowerCase.contains("minority attack"))
      .map(_.trim)
      .getOrElse("")
    val s11 = context11
      .split("(?<=[.!?])\\s+")
      .find(_.toLowerCase.contains("minority attack"))
      .map(_.trim)
      .getOrElse("")

    assert(s10.nonEmpty, clue(context10))
    assert(s11.nonEmpty, clue(context11))
    assertNotEquals(s10, s11)

  test("engine-ranked best move overrides candidate order in annotation mode"):
    val playedFirst = CandidateInfo(
      move = "g5",
      uci = Some("g7g5"),
      annotation = "",
      planAlignment = "Pawn push",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "complex",
      whyNot = Some("it weakens the king's cover"),
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = List("Nf6"),
      facts = Nil
    )
    val stronger = CandidateInfo(
      move = "Nf6",
      uci = Some("g8f6"),
      annotation = "!",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = None,
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil
    )

    val ctx = baseContext(
      playedMove = Some("g7g5"),
      playedSan = Some("g5"),
      candidates = List(playedFirst, stronger),
      threats = ThreatTable(Nil, Nil),
      engineEvidence = Some(
        EngineEvidence(
          depth = 12,
          variations = List(
            VariationLine(moves = List("g8f6", "d2d4"), scoreCp = 40),
            VariationLine(moves = List("g7g5", "d2d4"), scoreCp = -210)
          )
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val main = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).getOrElse("")
    val lower = main.toLowerCase

    assert(List("blunder", "mistake", "inaccuracy").exists(lower.contains), clue(main))
    assert(main.contains("**Nf6**") || main.contains("**f6**"), clue(main))

  test("alternatives explicitly contrast lower-ranked engine choices"):
    val bestMove = CandidateInfo(
      move = "Nf3",
      uci = Some("g1f3"),
      annotation = "!",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = None,
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil
    )
    val nearAlternative = CandidateInfo(
      move = "Nc3",
      uci = Some("b1c3"),
      annotation = "",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = Some("it can drift from the main plan"),
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil
    )
    val lowerAlternative = CandidateInfo(
      move = "h3",
      uci = Some("h2h3"),
      annotation = "",
      planAlignment = "Quiet move",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = Some("it wastes a useful tempo"),
      tags = Nil,
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil
    )

    val ctx = baseContext(
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3"),
      candidates = List(bestMove, nearAlternative, lowerAlternative),
      threats = ThreatTable(Nil, Nil),
      engineEvidence = Some(
        EngineEvidence(
          depth = 12,
          variations = List(
            VariationLine(moves = List("g1f3", "d7d5"), scoreCp = 35),
            VariationLine(moves = List("b1c3", "d7d5"), scoreCp = 15),
            VariationLine(moves = List("h2h3", "d7d5"), scoreCp = -95)
          )
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val main = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).getOrElse("")
    val alternatives = outline.beats.find(_.kind == OutlineBeatKind.Alternatives).map(_.text).getOrElse("")
    val lower = alternatives.toLowerCase
    val lines = alternatives.split("\n").toList.map(_.trim).filter(_.nonEmpty)
    val mainStem =
      main.toLowerCase
        .replaceAll("""\*\*[^*]+\*\*""", " ")
        .replaceAll("""\([^)]*\)""", " ")
        .replaceAll("""[^a-z\s]""", " ")
        .replaceAll("""\s+""", " ")
        .trim
        .split(" ")
        .filter(_.nonEmpty)
        .take(5)
        .mkString(" ")
    val stems = lines.map { line =>
      line.toLowerCase
        .replaceAll("""\*\*[^*]+\*\*""", " ")
        .replaceAll("""\([^)]*\)""", " ")
        .replaceAll("""[^a-z\s]""", " ")
        .replaceAll("""\s+""", " ")
        .trim
        .split(" ")
        .filter(_.nonEmpty)
        .take(5)
        .mkString(" ")
    }

    assert(alternatives.contains("**Nf3**"), clue(alternatives))
    assertEquals(lines.size, 2, clue(alternatives))
    assert(stems.distinct.size == stems.size, clue(alternatives))
    assert(!stems.contains(mainStem), clue(s"main=$main\nalts=$alternatives"))
    assert(lines.count(_.toLowerCase.startsWith("engine")) <= 1, clue(alternatives))
    assert(!lower.contains("technical setup for the next"), clue(alternatives))
    assert(!lower.contains("strategic test after"), clue(alternatives))
    val strategicSignals = List("coordination", "initiative", "king safety", "tempo", "conversion", "practical burden")
    assert(lines.forall(line => strategicSignals.exists(line.toLowerCase.contains)), clue(alternatives))

  test("hypothesis-space contract appears in main, alternatives, and wrap-up"):
    val mainMove = CandidateInfo(
      move = "Nf3",
      uci = Some("g1f3"),
      annotation = "!",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = Some("keeps king safety tempo under control"),
      practicalDifficulty = "clean",
      whyNot = None,
      tags = List(CandidateTag.Solid),
      tacticEvidence = List("Centralization(Knight on f3)"),
      probeLines = List("...d5 c4"),
      facts = Nil,
      hypotheses = List(
        HypothesisCard(
          axis = HypothesisAxis.PieceCoordination,
          claim = "Nf3 keeps coordination lanes connected before central clarification.",
          supportSignals = List("engine rank 1", "reply multipv coverage collected"),
          conflictSignals = Nil,
          confidence = 0.78,
          horizon = HypothesisHorizon.Medium
        ),
        HypothesisCard(
          axis = HypothesisAxis.KingSafety,
          claim = "Nf3 preserves kingside safety tempo while development continues.",
          supportSignals = List("threat or tactical alert points to king safety"),
          conflictSignals = Nil,
          confidence = 0.69,
          horizon = HypothesisHorizon.Short
        )
      )
    )
    val altMove = CandidateInfo(
      move = "Ne2",
      uci = Some("g1e2"),
      annotation = "",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "complex",
      whyNot = Some("it delays pressure on central tension"),
      tags = List(CandidateTag.Competitive),
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil,
      hypotheses = List(
        HypothesisCard(
          axis = HypothesisAxis.PawnBreakTiming,
          claim = "Ne2 keeps c-pawn flexibility but delays immediate central tension tests.",
          supportSignals = List("d-file break is available"),
          conflictSignals = List("engine gap is significant for this route"),
          confidence = 0.56,
          horizon = HypothesisHorizon.Medium
        )
      )
    )
    val thirdMove = CandidateInfo(
      move = "h3",
      uci = Some("h2h3"),
      annotation = "",
      planAlignment = "Quiet move",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "complex",
      whyNot = Some("it loosens kingside squares"),
      tags = List(CandidateTag.TacticalGamble),
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil,
      hypotheses = List(
        HypothesisCard(
          axis = HypothesisAxis.KingSafety,
          claim = "h3 concedes king-safety tempo for speculative flexibility.",
          supportSignals = List("candidate rationale already flags king safety issues"),
          conflictSignals = List("probe shows a clear practical concession"),
          confidence = 0.38,
          horizon = HypothesisHorizon.Short
        )
      )
    )

    val ctx = baseContext(
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3"),
      candidates = List(mainMove, altMove, thirdMove),
      threats = ThreatTable(Nil, Nil),
      engineEvidence = Some(
        EngineEvidence(
          depth = 16,
          variations = List(
            VariationLine(moves = List("g1f3", "d7d5"), scoreCp = 35),
            VariationLine(moves = List("g1e2", "d7d5"), scoreCp = 9),
            VariationLine(moves = List("h2h3", "d7d5"), scoreCp = -70)
          )
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val main = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).getOrElse("")
    val alternatives = outline.beats.find(_.kind == OutlineBeatKind.Alternatives).map(_.text).getOrElse("")
    val wrap = outline.beats.find(_.kind == OutlineBeatKind.WrapUp).map(_.text).getOrElse("")

    val mainLower = main.toLowerCase
    assert(
      mainLower.contains("hypothesis") ||
        mainLower.contains("read is that") ||
        mainLower.contains("explanatory lens"),
      clue(main)
    )
    assert(mainLower.contains("validation") || mainLower.contains("validated"), clue(main))
    assert(mainLower.contains("practical"), clue(main))

    val altLines = alternatives.split("\n").toList.map(_.trim).filter(_.nonEmpty)
    assertEquals(altLines.size, 2, clue(alternatives))
    assert(
      altLines.forall { line =>
        val lower = line.toLowerCase
        lower.contains("compared with") || lower.contains("relative to")
      },
      clue(alternatives)
    )

    val strategicWords = List("initiative", "structure", "coordination", "king", "plan", "timing", "trajectory", "conversion", "practical")
    val numericStandalone = altLines.exists { line =>
      val lower = line.toLowerCase
      val hasNumeric = lower.exists(_.isDigit)
      hasNumeric && !strategicWords.exists(lower.contains)
    }
    assert(!numericStandalone, clue(alternatives))

    val mainStem = firstFiveStem(main)
    val altStems = altLines.map(firstFiveStem)
    assert(!altStems.contains(mainStem), clue(s"main=$main\nalts=$alternatives"))
    assert(altStems.distinct.size == altStems.size, clue(alternatives))

    val wrapLower = wrap.toLowerCase
    assert(
      List("decisive split", "decisively", "key difference").exists(wrapLower.contains),
      clue(wrap)
    )

  test("alternatives suppress move-only variants of the same hypothesis claim"):
    val mainMove = CandidateInfo(
      move = "Na4",
      uci = Some("b6a4"),
      annotation = "!",
      planAlignment = "Positional maneuvering",
      downstreamTactic = None,
      tacticalAlert = Some("maintains structural pressure on c3"),
      practicalDifficulty = "clean",
      whyNot = None,
      tags = List(CandidateTag.Solid),
      tacticEvidence = List("Outpost(Knight on a4)"),
      probeLines = Nil,
      facts = Nil,
      hypotheses = List(
        HypothesisCard(
          axis = HypothesisAxis.Structure,
          claim = "Na4 changes the structural balance, trading immediate activity for long-term square control.",
          supportSignals = List("fact-level structural weakness signal"),
          conflictSignals = Nil,
          confidence = 0.74,
          horizon = HypothesisHorizon.Long
        )
      )
    )
    val altMove1 = CandidateInfo(
      move = "Rd7",
      uci = Some("f7d7"),
      annotation = "",
      planAlignment = "Positional pressure",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "complex",
      whyNot = Some("it slows practical counterplay"),
      tags = List(CandidateTag.Competitive),
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil,
      hypotheses = List(
        HypothesisCard(
          axis = HypothesisAxis.PawnBreakTiming,
          claim = "Rd7 delays the pawn break to improve support, betting on better timing in the next phase.",
          supportSignals = List("d-file break is available"),
          conflictSignals = List("engine gap is significant for this route"),
          confidence = 0.57,
          horizon = HypothesisHorizon.Medium
        )
      )
    )
    val altMove2 = CandidateInfo(
      move = "Rac8",
      uci = Some("a8c8"),
      annotation = "",
      planAlignment = "Positional pressure",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "complex",
      whyNot = Some("it loosens central timing"),
      tags = List(CandidateTag.Competitive),
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil,
      hypotheses = List(
        HypothesisCard(
          axis = HypothesisAxis.PawnBreakTiming,
          claim = "Rac8 delays the pawn break to improve support, betting on better timing in the next phase.",
          supportSignals = List("d-file break is available"),
          conflictSignals = List("engine gap is significant for this route"),
          confidence = 0.54,
          horizon = HypothesisHorizon.Medium
        )
      )
    )

    val ctx = baseContext(
      playedMove = Some("b6a4"),
      playedSan = Some("Na4"),
      candidates = List(mainMove, altMove1, altMove2),
      threats = ThreatTable(Nil, Nil),
      engineEvidence = Some(
        EngineEvidence(
          depth = 16,
          variations = List(
            VariationLine(moves = List("b6a4", "h2g3"), scoreCp = 30),
            VariationLine(moves = List("f7d7", "h2g3"), scoreCp = 5),
            VariationLine(moves = List("a8c8", "h2g3"), scoreCp = 2)
          )
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val alternatives = outline.beats.find(_.kind == OutlineBeatKind.Alternatives).map(_.text).getOrElse("")
    val lines = alternatives.split("\n").toList.map(_.trim).filter(_.nonEmpty)
    val lower = alternatives.toLowerCase

    assertEquals(lines.size, 2, clue(alternatives))
    assert(occurrences(lower, "delays the pawn break to improve support") <= 1, clue(alternatives))
    assert(lines.forall(_.contains("**")), clue(alternatives))

  test("opening branchpoint with valid sample game emits precedent snippet"):
    val openingRef = OpeningReference(
      eco = Some("D85"),
      name = Some("Gruenfeld Defense"),
      totalGames = 2500,
      topMoves = List(ExplorerMove("c2c4", "c4", 1000, 400, 300, 300, 2700)),
      sampleGames = List(
        ExplorerGame(
          id = "g1",
          winner = Some(chess.Black),
          white = ExplorerPlayer("Carlsen, Magnus", 2860),
          black = ExplorerPlayer("Grischuk, Alexander", 2760),
          year = 2019,
          month = 6,
          event = Some("Norway Chess 2019"),
          pgn = Some("19... Na4 20. Rab1 Rc5 21. Qe1")
        )
      )
    )
    val ctx = baseContext(
      playedMove = Some("b6a4"),
      playedSan = Some("Na4"),
      threats = ThreatTable(Nil, Nil)
    ).copy(
      openingData = Some(openingRef),
      openingEvent = Some(OpeningEvent.BranchPoint(List("Na4", "Rc5"), "Main line shifts", None))
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val all = outline.beats.map(_.text).mkString(" ")
    val lower = all.toLowerCase

    assert(lower.contains("in magnus carlsen-alexander grischuk"), clue(all))
    assert(lower.contains("after 19... na4 20. rab1 rc5 21. qe1"), clue(all))
    assert(lower.contains("won (0-1)"), clue(all))
    assert(lower.contains("turning point"), clue(all))

  test("long-horizon main hypothesis adds exactly one delayed-payoff bridge sentence"):
    val mainMove = CandidateInfo(
      move = "Nf3",
      uci = Some("g1f3"),
      annotation = "!",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = Some("keeps tactical pressure contained"),
      practicalDifficulty = "clean",
      whyNot = None,
      tags = List(CandidateTag.Solid),
      tacticEvidence = List("Centralization(Knight on f3)"),
      probeLines = Nil,
      facts = Nil,
      hypotheses = List(
        HypothesisCard(
          axis = HypothesisAxis.EndgameTrajectory,
          claim = "Nf3 keeps the endgame trajectory favorable by preserving flexible simplification options.",
          supportSignals = List(
            "long-horizon probe confirms delayed plan prerequisites",
            "long-horizon probe samples conversion and trajectory branches"
          ),
          conflictSignals = Nil,
          confidence = 0.81,
          horizon = HypothesisHorizon.Long
        ),
        HypothesisCard(
          axis = HypothesisAxis.PieceCoordination,
          claim = "Nf3 keeps piece coordination lanes connected during development.",
          supportSignals = List("reply multipv coverage collected"),
          conflictSignals = Nil,
          confidence = 0.62,
          horizon = HypothesisHorizon.Medium
        )
      )
    )
    val altMove = CandidateInfo(
      move = "Ne2",
      uci = Some("g1e2"),
      annotation = "",
      planAlignment = "Development",
      downstreamTactic = None,
      tacticalAlert = None,
      practicalDifficulty = "complex",
      whyNot = Some("it delays central pressure"),
      tags = List(CandidateTag.Competitive),
      tacticEvidence = Nil,
      probeLines = Nil,
      facts = Nil,
      hypotheses = List(
        HypothesisCard(
          axis = HypothesisAxis.PawnBreakTiming,
          claim = "Ne2 delays central tension tests and shifts timing risk.",
          supportSignals = List("d-file break is available"),
          conflictSignals = List("engine gap is significant for this route"),
          confidence = 0.52,
          horizon = HypothesisHorizon.Medium
        )
      )
    )
    val ctx = baseContext(
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3"),
      candidates = List(mainMove, altMove),
      threats = ThreatTable(Nil, Nil),
      engineEvidence = Some(
        EngineEvidence(
          depth = 16,
          variations = List(
            VariationLine(moves = List("g1f3", "d7d5"), scoreCp = 35),
            VariationLine(moves = List("g1e2", "d7d5"), scoreCp = 9)
          )
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val main = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).getOrElse("")
    val sentences = main.split("(?<=[.!?])\\s+").toList.map(_.trim).filter(_.nonEmpty)
    val bridgeMarkers = List(
      "decisive split is expected",
      "usually decides the game later",
      "practical payoff tends to appear later",
      "real test arrives later",
      "shifts the balance later",
      "advantage normally appears later"
    )
    val bridgeSentences = sentences.filter { s =>
      val lower = s.toLowerCase
      bridgeMarkers.exists(lower.contains)
    }
    assertEquals(bridgeSentences.size, 1, clue(main))

    val hypothesisStem = sentences
      .find { s =>
        val lower = s.toLowerCase
        lower.contains("hypothesis") || lower.contains("read is that")
      }
      .map(firstFiveStem)
      .getOrElse("")
    val bridgeStem = firstFiveStem(bridgeSentences.head)
    assertNotEquals(bridgeStem, hypothesisStem)

  test("branchpoint with multiple sample games emits A/B/C precedent comparison"):
    val openingRef = OpeningReference(
      eco = Some("D85"),
      name = Some("Gruenfeld Defense"),
      totalGames = 2500,
      topMoves = List(
        ExplorerMove("b6a4", "Na4", 620, 260, 170, 190, 2740),
        ExplorerMove("c8e6", "Be6", 540, 220, 160, 160, 2730),
        ExplorerMove("f8c8", "Rc8", 430, 150, 150, 130, 2720)
      ),
      sampleGames = List(
        ExplorerGame(
          id = "gA",
          winner = Some(chess.Black),
          white = ExplorerPlayer("Carlsen, Magnus", 2860),
          black = ExplorerPlayer("Grischuk, Alexander", 2760),
          year = 2019,
          month = 6,
          event = Some("Norway Chess 2019"),
          pgn = Some("19... Na4 20. hxg6 hxg6 21. f5")
        ),
        ExplorerGame(
          id = "gB",
          winner = Some(chess.White),
          white = ExplorerPlayer("Kramnik, Vladimir", 2800),
          black = ExplorerPlayer("Svidler, Peter", 2740),
          year = 2018,
          month = 1,
          event = Some("Tata Steel"),
          pgn = Some("18... Rc7 19. Rxa7 Rb8 20. Rd5")
        ),
        ExplorerGame(
          id = "gC",
          winner = Some(chess.White),
          white = ExplorerPlayer("Ding, Liren", 2812),
          black = ExplorerPlayer("Mamedyarov, Shakhriyar", 2765),
          year = 2019,
          month = 6,
          event = Some("Norway Chess"),
          pgn = Some("20... Rc5 21. Be7 Rxc3 22. bxc3")
        )
      )
    )

    val ctx = baseContext(
      playedMove = Some("b6a4"),
      playedSan = Some("Na4"),
      threats = ThreatTable(Nil, Nil)
    ).copy(
      openingData = Some(openingRef),
      openingEvent = Some(OpeningEvent.BranchPoint(List("Na4", "Rc7", "Rc5"), "Main line shifts", None))
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val all = outline.beats.map(_.text).mkString(" ")
    val lower = all.toLowerCase

    assert(lower.contains("a)"), clue(all))
    assert(lower.contains("b)"), clue(all))
    assert(lower.contains("c)"), clue(all))
    val hasRouteRole =
      List("line route:", "the branch follows", "the move path here is", "the practical route is")
        .exists(lower.contains)
    val hasTransitionRole =
      List(
        "strategically, the game turned",
        "strategic transition",
        "practical turning factor",
        "shifts plans through"
      )
        .exists(lower.contains)
    val hasDriverRole =
      List("decisive practical driver", "results hinged on", "key match result factor", "conversion quality")
        .exists(lower.contains)
    assert(hasRouteRole, clue(all))
    assert(hasTransitionRole, clue(all))
    assert(hasDriverRole, clue(all))
    assert(occurrences(lower, "sequence focus") <= 1, clue(all))
    assert(occurrences(lower, "strategic shift") <= 1, clue(all))

  test("main move keeps fallback rhythm when no precedent game is available"):
    val openingRef = OpeningReference(
      eco = Some("D85"),
      name = Some("Gruenfeld Defense"),
      totalGames = 2500,
      topMoves = List(ExplorerMove("b6a4", "Na4", 620, 260, 170, 190, 2740)),
      sampleGames = Nil
    )
    val ctx = baseContext(
      playedMove = Some("b6a4"),
      playedSan = Some("Na4"),
      threats = ThreatTable(Nil, Nil)
    ).copy(
      openingData = Some(openingRef),
      openingEvent = Some(OpeningEvent.BranchPoint(List("Na4"), "Main line shifts", None))
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val main = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).getOrElse("")
    val lower = main.toLowerCase
    val hasFallbackRhythm = List(
      "practical terms, the key is to keep plans coherent",
      "position is decided more by accurate follow-up",
      "practical move-order discipline is the main guide"
    ).exists(lower.contains)
    assert(hasFallbackRhythm, clue(main))
    assert(!lower.contains("no data"), clue(main))
    assert(!lower.contains("unavailable"), clue(main))

  test("precedent snippet is omitted when sample game lacks verification fields"):
    val openingRef = OpeningReference(
      eco = Some("D85"),
      name = Some("Gruenfeld Defense"),
      totalGames = 2500,
      topMoves = List(ExplorerMove("c2c4", "c4", 1000, 400, 300, 300, 2700)),
      sampleGames = List(
        ExplorerGame(
          id = "g2",
          winner = None,
          white = ExplorerPlayer("Carlsen, Magnus", 2860),
          black = ExplorerPlayer("Grischuk, Alexander", 2760),
          year = 2019,
          month = 6,
          event = Some("Norway Chess 2019"),
          pgn = None
        )
      )
    )
    val ctx = baseContext(
      playedMove = Some("b6a4"),
      playedSan = Some("Na4"),
      threats = ThreatTable(Nil, Nil)
    ).copy(
      openingData = Some(openingRef),
      openingEvent = Some(OpeningEvent.BranchPoint(List("Na4"), "Main line shifts", None))
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val opening = outline.beats.find(_.kind == OutlineBeatKind.OpeningTheory).map(_.text).getOrElse("")
    assert(!opening.toLowerCase.contains("magnus carlsen"), clue(opening))

  test("intro opening event does not emit precedent snippet"):
    val openingRef = OpeningReference(
      eco = Some("D85"),
      name = Some("Gruenfeld Defense"),
      totalGames = 2500,
      topMoves = List(ExplorerMove("c2c4", "c4", 1000, 400, 300, 300, 2700)),
      sampleGames = List(
        ExplorerGame(
          id = "g3",
          winner = Some(chess.White),
          white = ExplorerPlayer("Carlsen, Magnus", 2860),
          black = ExplorerPlayer("Grischuk, Alexander", 2760),
          year = 2019,
          month = 6,
          event = Some("Norway Chess 2019"),
          pgn = Some("6. Bg5 Nfd7 7. Qd2")
        )
      )
    )
    val ctx = baseContext(
      playedMove = Some("c2c4"),
      playedSan = Some("c4"),
      threats = ThreatTable(Nil, Nil)
    ).copy(
      openingData = Some(openingRef),
      openingEvent = Some(OpeningEvent.Intro("D85", "Gruenfeld Defense", "Dynamic center", List("c4", "Nf6")))
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val opening = outline.beats.find(_.kind == OutlineBeatKind.OpeningTheory).map(_.text).getOrElse("")
    assert(!opening.toLowerCase.contains("magnus carlsen"), clue(opening))

  test("low-confidence precedent keeps factual single sentence only"):
    val openingRef = OpeningReference(
      eco = Some("D85"),
      name = Some("Gruenfeld Defense"),
      totalGames = 2500,
      topMoves = List(ExplorerMove("c2c4", "c4", 1000, 400, 300, 300, 2700)),
      sampleGames = List(
        ExplorerGame(
          id = "g5",
          winner = Some(chess.White),
          white = ExplorerPlayer("Carlsen, Magnus", 2860),
          black = ExplorerPlayer("Grischuk, Alexander", 2760),
          year = 2019,
          month = 6,
          event = None,
          pgn = Some("19... Na4 20. Rab1")
        )
      )
    )
    val ctx = baseContext(
      playedMove = Some("c8e6"),
      playedSan = Some("Be6"),
      threats = ThreatTable(Nil, Nil)
    ).copy(
      openingData = Some(openingRef),
      openingEvent = Some(OpeningEvent.BranchPoint(List("Be6"), "Shift", None))
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val all = outline.beats.map(_.text).mkString(" ")
    val lower = all.toLowerCase

    assert(lower.contains("won (1-0)"), clue(all))
    assert(!lower.contains("turning point"), clue(all))

  test("middlegame annotation can attach precedent snippet when SAN overlaps sample game"):
    val openingRef = OpeningReference(
      eco = Some("D77"),
      name = Some("Neo-Gruenfeld"),
      totalGames = 1800,
      topMoves = List(ExplorerMove("g7d4", "Bxd4", 300, 110, 90, 100, 2720)),
      sampleGames = List(
        ExplorerGame(
          id = "g4",
          winner = Some(chess.Black),
          white = ExplorerPlayer("Carlsen, Magnus", 2860),
          black = ExplorerPlayer("Grischuk, Alexander", 2760),
          year = 2019,
          month = 6,
          event = Some("Norway Chess 2019"),
          pgn = Some("19... Bxd4 20. Nxd4 Rc5 21. Qe1")
        )
      )
    )
    val ctx = baseContext(
      playedMove = Some("g7d4"),
      playedSan = Some("Bxd4"),
      threats = ThreatTable(Nil, Nil),
      engineEvidence = Some(
        EngineEvidence(
          depth = 14,
          variations = List(
            VariationLine(moves = List("g7d4", "f3d4", "d8d4"), scoreCp = -45),
            VariationLine(moves = List("c8e6", "d4g7"), scoreCp = -15)
          )
        )
      )
    ).copy(
      openingData = Some(openingRef),
      openingEvent = None
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val main = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).getOrElse("")
    val lower = main.toLowerCase

    assert(lower.contains("magnus carlsen-alexander grischuk"), clue(main))
    assert(lower.contains("after 19... bxd4 20. nxd4 rc5 21. qe1"), clue(main))
    assert(lower.contains("turning point"), clue(main))

  private def occurrences(haystack: String, needle: String): Int =
    if needle.isEmpty then 0
    else haystack.sliding(needle.length).count(_ == needle)

  private def firstFiveStem(text: String): String =
    text
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(5)
      .mkString(" ")

  private def baseContext(
    playedMove: Option[String],
    playedSan: Option[String],
    candidates: List[CandidateInfo] = List(
      CandidateInfo(
        move = "Nf3",
        uci = Some("g1f3"),
        annotation = "!",
        planAlignment = "Development",
        downstreamTactic = None,
        tacticalAlert = None,
        practicalDifficulty = "clean",
        whyNot = None,
        tags = Nil,
        tacticEvidence = Nil,
        facts = Nil
      )
    ),
    facts: List[Fact] = Nil,
    threats: ThreatTable,
    counterfactual: Option[CounterfactualMatch] = None,
    engineEvidence: Option[EngineEvidence] = None
  ): NarrativeContext =
    NarrativeContext(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Low", "ExplainPlan"),
      ply = 10,
      playedMove = playedMove,
      playedSan = playedSan,
      counterfactual = counterfactual,
      summary = NarrativeSummary("Central control", None, "NarrowChoice", "Maintain", "+0.2"),
      threats = threats,
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Test", "Background", None, false, "quiet"),
      plans = PlanTable(
        top5 = List(PlanRow(rank = 1, name = "Central control", score = 0.78, evidence = List("space"))),
        suppressed = Nil
      ),
      snapshots = List(L1Snapshot("=", None, None, None, None, Some("Balanced"), Nil)),
      delta = None,
      phase = PhaseContext("Opening", "Pieces undeveloped", None),
      candidates = candidates,
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = Some(PracticalInfo(5, 4, "Balanced")),
          preventedPlans = Nil,
          conceptSummary = Nil
        )
      ),
      facts = facts,
      engineEvidence = engineEvidence
    )
