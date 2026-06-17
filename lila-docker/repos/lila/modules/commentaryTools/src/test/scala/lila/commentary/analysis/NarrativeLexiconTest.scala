package lila.commentary.analysis

import java.nio.file.{ Files, Paths }

import lila.commentary.analysis.claim.OpeningFamilyClaimResolver.OpeningFamilyId
import lila.commentary.model.{ CandidateTag, HypothesisAxis, HypothesisHorizon, PhaseContext }
import munit.FunSuite

class NarrativeLexiconTest extends FunSuite:

  test("gameIntro avoids unknown placeholders, markdown markers, and ply wording for short unfinished lines") {
    val intro = NarrativeLexicon.gameIntro(
      white = "Unknown",
      black = "Unknown",
      event = "Unknown",
      date = "Unknown",
      result = "*",
      totalPlies = 11,
      keyMomentsCount = 2
    )

    assertNoDiff(
      intro,
      "This review covers this game. The line is still in the opening after 6 moves. The review focuses on 2 highlighted moments."
    )
    assert(!intro.contains("**"))
    assert(!intro.toLowerCase.contains("plies"))
    assert(!intro.toLowerCase.contains("battle"))
  }

  test("gameIntro keeps known player names and uses move wording for finished games") {
    val intro = NarrativeLexicon.gameIntro(
      white = "WhitePlayer",
      black = "BlackPlayer",
      event = "Club Championship",
      date = "2026.03.19",
      result = "1-0",
      totalPlies = 47,
      keyMomentsCount = 1
    )

    assertEquals(
      intro,
      "This review covers WhitePlayer vs BlackPlayer (Club Championship, 2026.03.19). WhitePlayer won after 24 moves. The review focuses on one highlighted moment."
    )
  }

  test("gameIntro drops placeholder broadcast metadata") {
    val intro = NarrativeLexicon.gameIntro(
      white = "WhitePlayer",
      black = "BlackPlayer",
      event = "Weekend Swiss",
      date = "????.??.??",
      result = "1-0",
      totalPlies = 47,
      keyMomentsCount = 1
    )

    assertEquals(
      intro,
      "This review covers WhitePlayer vs BlackPlayer (Weekend Swiss). WhitePlayer won after 24 moves. The review focuses on one highlighted moment."
    )
    assert(!intro.contains("????.??.??"), clue(intro))
  }

  test("gameConclusion uses the lead theme instead of generic critical-moments boilerplate") {
    val conclusion =
      NarrativeLexicon.gameConclusion(
        winner = Some("WhitePlayer"),
        themes = List("Attacking a fixed pawn", "Improving piece placement", "Development and central control"),
        blunders = 0,
        missedWins = 0
      )

    assertEquals(
      conclusion,
      "**WhitePlayer** kept the game on the right strategic track through Attacking a fixed pawn. Other recurring themes were Improving piece placement, Development and central control."
    )
    assert(!conclusion.toLowerCase.contains("critical moments"), clue(conclusion))
  }

  test("gameConclusion surfaces practical swings when blunders and missed wins exist") {
    val conclusion =
      NarrativeLexicon.gameConclusion(
        winner = Some("BlackPlayer"),
        themes = List("Queenside pressure"),
        blunders = 2,
        missedWins = 1
      )

    assert(conclusion.contains("BlackPlayer"), clue(conclusion))
    assert(conclusion.contains("Queenside pressure"), clue(conclusion))
    assert(conclusion.contains("2 blunders"), clue(conclusion))
    assert(conclusion.contains("1 missed win"), clue(conclusion))
  }

  test("gameConclusion prefers concrete whole-game binder sentences over count-only practical tail") {
    val conclusion =
      NarrativeLexicon.gameConclusion(
        winner = Some("WhitePlayer"),
        themes = List("Queenside pressure", "Improving piece placement"),
        blunders = 2,
        missedWins = 1,
        mainContest = Some("White was mainly playing for pressure on b2, while Black was mainly playing for control of the d-file."),
        decisiveShift = Some("The decisive shift came through pressure on b2."),
        payoff = Some("The punishment story ran through pressure on b2.")
      )

    assert(conclusion.contains("White was mainly playing for pressure on b2"), clue(conclusion))
    assert(conclusion.contains("The decisive shift came through pressure on b2."), clue(conclusion))
    assert(conclusion.contains("The punishment story ran through pressure on b2."), clue(conclusion))
    assert(!conclusion.contains("around Queenside pressure"), clue(conclusion))
    assert(!conclusion.contains("2 blunders"), clue(conclusion))
    assert(!conclusion.contains("1 missed win"), clue(conclusion))
  }

  test("gameConclusion keeps balanced games compact when the binder only has a contest sentence") {
    val conclusion =
      NarrativeLexicon.gameConclusion(
        winner = None,
        themes = List("Queenside pressure"),
        blunders = 0,
        missedWins = 0,
        mainContest = Some("White was mainly playing for queenside targets, while Black was mainly playing for central files."),
        decisiveShift = None,
        payoff = None
      )

    assert(conclusion.startsWith("The game stayed balanced while the main plans stayed in tension."), clue(conclusion))
    assert(conclusion.contains("White was mainly playing for queenside targets"), clue(conclusion))
    assert(!conclusion.contains("around Queenside pressure"), clue(conclusion))
  }

  test("generic negative annotations avoid forced-only wording without explicit evidence") {
    val text = NarrativeLexicon.getAnnotationNegative(
      bead = 0,
      playedSan = "Qe2",
      bestSan = "Qd3",
      cpLoss = 320
    ).toLowerCase

    assert(!text.contains("was required"), clue(text))
    assert(!text.contains("only stable route"), clue(text))
    assert(!text.contains("was necessary"), clue(text))
    assert(!text.contains("forcing sequence"), clue(text))
    assert(text.contains("cleanest defense") || text.contains("resilient route") || text.contains("hold things together"), clue(text))
  }

  test("annotation clauses do not infer practical ease initiative or conversion from cp loss alone") {
    val samples =
      (0 until 24).flatMap { bead =>
        List(
          NarrativeLexicon.getAnnotationPositive(bead, "Qe2"),
          NarrativeLexicon.getAnnotationInvestment(bead, "Qe2"),
          NarrativeLexicon.getAnnotationNegative(bead, "Qe2", "Qd3", 320),
          NarrativeLexicon.getAnnotationNegative(bead, "Qe2", "Qd3", 140),
          NarrativeLexicon.getAnnotationNegative(bead, "Qe2", "Qd3", 45),
          NarrativeLexicon.getAnnotationNegativeWithoutBenchmark(bead, "Qe2", 320),
          NarrativeLexicon.getAnnotationNegativeWithoutBenchmark(bead, "Qe2", 140),
          NarrativeLexicon.getAnnotationNegativeWithoutBenchmark(bead, "Qe2", 45)
        ) ++
          List(2, 3).flatMap(rank =>
            List(20, 90).flatMap(cpLoss =>
              NarrativeLexicon.getEngineRankContext(bead, Some(rank), "Qd3", cpLoss).toList
            )
          ) ++
          List(20, 90).flatMap(cpLoss =>
            NarrativeLexicon.getEngineRankContext(bead, None, "Qd3", cpLoss).toList
          )
      }

    val forbidden =
      List(
        "decisive",
        "practical initiative",
        "practical ease",
        "practical control",
        "practical gap",
        "practical difference",
        "practical benchmark",
        "best practical",
        "simpler to convert",
        "easier to manage",
        "easier to handle",
        "easier play",
        "initiative handling",
        "control of initiative",
        "easy target",
        "straightforward over the board"
      )

    samples.foreach { text =>
      val lower = text.toLowerCase
      assert(!forbidden.exists(lower.contains), clue(text))
    }
  }

  test("eval outcome clauses stay objective and do not imply conversion or practical ease") {
    val samples =
      for
        cp <- List(620, 360, 140, 45, 0, -45, -140, -360, -620)
        bead <- 0 until 16
      yield NarrativeLexicon.evalOutcomeClauseFromCp(bead, cp, ply = bead % 5)

    val forbidden =
      List(
        "winning",
        "decisive",
        "completely on top",
        "convert",
        "comfortable",
        "two results",
        "easier side",
        "practical initiative",
        "better game",
        "pleasant position"
      )

    samples.foreach { text =>
      val lower = text.toLowerCase
      assert(!forbidden.exists(lower.contains), clue(text))
    }
    assert(samples.exists(_.toLowerCase.contains("evaluation")), clue(samples))
  }

  test("endgame phase and long-horizon lexicon stay context-only without technique or conversion authority") {
    val samples =
      (0 until 32).flatMap { bead =>
        List(
          NarrativeLexicon.getOpening(bead, "Endgame", "The evaluation is near equal", tactical = false, ply = bead),
          NarrativeLexicon.getHypothesisPracticalClause(
            bead,
            HypothesisHorizon.Long,
            HypothesisAxis.Conversion,
            "Qe2"
          ),
          NarrativeLexicon.getHypothesisPracticalClause(
            bead,
            HypothesisHorizon.Long,
            HypothesisAxis.EndgameTrajectory,
            "Qe2"
          ),
          NarrativeLexicon.getLongHorizonBridgeClause(bead, "Qe2", HypothesisAxis.Conversion),
          NarrativeLexicon.getLongHorizonBridgeClause(bead, "Qe2", HypothesisAxis.EndgameTrajectory),
          NarrativeLexicon.getAnnotationDifficultyHint(bead, "clean", "Qe2", "Endgame").getOrElse(""),
          NarrativeLexicon.getAnnotationDifficultyHint(bead, "clean", "Qe2", "Middlegame").getOrElse("")
        ) ++
          NarrativeLexicon.getAlternativeHypothesisDifferenceVariants(
            bead = bead,
            alternativeMove = "Qe2",
            mainMove = "Qd3",
            mainAxis = Some(HypothesisAxis.Conversion),
            alternativeAxis = Some(HypothesisAxis.EndgameTrajectory),
            alternativeClaim = None,
            confidence = 0.62,
            horizon = HypothesisHorizon.Long
          )
      }

    val forbidden =
      List(
        "technical endgame",
        "endgame technique",
        "technical handling",
        "technical outcome",
        "technical route",
        "technical phase",
        "conversion phase",
        "conversion window",
        "converted",
        "must be converted",
        "practical result",
        "decisive split",
        "decides the game",
        "resulting advantage"
      )

    samples.foreach { text =>
      val lower = text.toLowerCase
      assert(!forbidden.exists(lower.contains), clue(text))
    }
  }

  test("raw intent labels stay cue-level without line consequence or endgame technique authority") {
    val samples =
      (0 until 16).flatMap { bead =>
        List(
          NarrativeLexicon.getIntent(bead, "zugzwang", None, ply = 52),
          NarrativeLexicon.getIntent(bead, "zugzwang", Some("opposition cue"), ply = 52),
          NarrativeLexicon.getIntent(bead, "passed pawn", None, ply = 52),
          NarrativeLexicon.getIntent(bead, "pawn_race", None, ply = 52),
          NarrativeLexicon.getIntent(bead, "opposition", None, ply = 52),
          NarrativeLexicon.getIntent(bead, "simplification", None, ply = 52),
          NarrativeLexicon.getIntent(bead, "outpost", None, ply = 28),
          NarrativeLexicon.getIntent(bead, "exchange", None, ply = 28)
        )
      }

    val forbidden =
      List(
        "forces zugzwang",
        "in zugzwang",
        "endgame-technique",
        "fatal concession",
        "creates promotion threats",
        "promotion races",
        "races for promotion",
        "pushes for promotion",
        "takes the opposition",
        "gains the opposition",
        "favorable endgame",
        "easier position",
        "unassailable",
        "durable outpost",
        "forces a favorable exchange",
        "forces an exchange"
      )

    samples.foreach { text =>
      val lower = text.toLowerCase
      assert(!forbidden.exists(lower.contains), clue(text))
    }
  }

  test("main-flow wrappers keep sampled replies neutral") {
    val samples =
      (0 until 16).flatMap { bead =>
        List(
          NarrativeLexicon.getMainFlow(
            bead = bead,
            move = "Qe2",
            annotation = "",
            intent = "keeps a candidate cue in view",
            replySan = Some("Nf6"),
            sampleRest = Some("Nc3 Bb4"),
            evalTerm = "The engine comparison stays near equal."
          ),
          NarrativeLexicon.getMainFlow(
            bead = bead,
            move = "Qe2",
            annotation = "",
            intent = "keeps a candidate cue in view",
            replySan = Some("Nf6"),
            sampleRest = None,
            evalTerm = "The engine comparison stays near equal."
          ),
          NarrativeLexicon.getMainFlow(
            bead = bead,
            move = "Qe2",
            annotation = "",
            intent = "keeps a candidate cue in view",
            replySan = None,
            sampleRest = None,
            evalTerm = "The engine comparison stays near equal."
          )
        )
      }

    val forbidden =
      List(
        "forcing ",
        "clear outcome",
        "causing problems",
        "precise choice"
      )

    samples.foreach { text =>
      val lower = text.toLowerCase
      assert(!forbidden.exists(lower.contains), clue(text))
    }
  }

  test("post critic endgame filler repair does not mint technique authority") {
    val ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
      phase = PhaseContext("Endgame", "Material reduced")
    )
    val prose = PostCritic.revise(
      ctx,
      "The game begins. The struggle has shifted into technical endgame play. This phase is now about endgame technique."
    ).toLowerCase

    assert(prose.contains("endgame details"), clue(prose))
    assert(!prose.contains("technical endgame"), clue(prose))
    assert(!prose.contains("endgame technique"), clue(prose))
    assert(!prose.contains("technical handling"), clue(prose))
  }

  test("post critic boilerplate repair does not mint practical ease or edge authority") {
    val ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx
    val prose = PostCritic.revise(
      ctx,
      "The move remains the benchmark continuation, with only a modest practical edge. It changes which plan family is easier to execute."
    ).toLowerCase
    val forbidden =
      List(
        "practical edge",
        "practical benchmark",
        "practical margin",
        "small practical plus",
        "easier to execute",
        "easiest to handle",
        "most practical",
        "comfortable"
      )

    forbidden.foreach(term => assert(!prose.contains(term), clue(prose)))
  }

  test("tag-only conversion and motif prefixes stay context-only without result authority") {
    val runtimeSamples =
      (0 until 12).flatMap { bead =>
        List(
          NarrativeLexicon.getAnnotationTagOnlyHint(bead, CandidateTag.Converting, "Qe2").getOrElse(""),
          NarrativeLexicon.getAlternative(bead, "Qe2", None),
          NarrativeLexicon.getPrecedentDecisionDriverLine(bead, "the dark squares"),
          NarrativeLexicon.getEngineRankContext(bead, Some(3), "Qe2", cpLoss = 20).getOrElse(""),
          NarrativeLexicon.getEngineRankContext(bead, Some(3), "Qe2", cpLoss = 120).getOrElse(""),
          NarrativeLexicon.getMotifPrefix(bead, List("stalemate_trick"), ply = bead).getOrElse(""),
          NarrativeLexicon.getMotifPrefix(bead, List("rook_on_seventh"), ply = bead).getOrElse(""),
          NarrativeLexicon.getMotifPrefix(bead, List("blockade"), ply = bead).getOrElse("")
        )
      }
    val tableSource =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeMotifPrefixTable.scala"))
    val forbidden =
      List(
        "cleaner conversion",
        "simpler win",
        "technical converting",
        "systematic conversion",
        "technical means",
        "conversion quality",
        "decisive practical driver",
        "results hinged",
        "key match result",
        "straightforward conversion",
        "technical battle",
        "conversion plan",
        "clearer technical path",
        "stable technical reference",
        "conversion relies",
        "more accurately",
        "separated the sampled routes",
        "practical turning factor",
        "game turned on",
        "practical edge",
        "easier to execute"
      )

    runtimeSamples.foreach { text =>
      val lower = text.toLowerCase
      assert(!forbidden.exists(lower.contains), clue(text))
    }
    forbidden.foreach(term => assert(!tableSource.toLowerCase.contains(term), clue(term)))
  }

  test("generic negative annotations without a verified benchmark stay vague and truthful") {
    val text = NarrativeLexicon.getAnnotationNegativeWithoutBenchmark(
      bead = 0,
      playedSan = "Qc6",
      cpLoss = 180
    ).toLowerCase

    assert(!text.contains("better is"), clue(text))
    assert(!text.contains("engine reference"), clue(text))
    assert(!text.contains("benchmark"), clue(text))
    assert(!text.contains("qd3"), clue(text))
    assert(
      text.contains("loses control") ||
        text.contains("concedes") ||
        text.contains("lets the position drift") ||
        text.contains("coordination control") ||
        text.contains("move-order terms"),
      clue(text)
    )
  }

  test("opening mismatch prose comes from structured family identity") {
    val text =
      NarrativeLexicon.getGoalStatusDescription(
        bead = 0,
        evaluation =
          OpeningGoals.Evaluation(
            goalName = "Sicilian Liberator",
            status = OpeningGoals.Status.Mismatch,
            supportedEvidence = Nil,
            missingEvidence = Nil,
            confidence = 0.8,
            requiredFamily = Some(OpeningFamilyId.Sicilian)
          )
      )

    assert(text.contains("Sicilian structure"), clue(text))
    assert(!text.contains("Sicilian Liberator"), clue(text))
    assert(!text.contains("c5 pawn or traded c-pawn"), clue(text))
  }

  test("opening goal status prose does not expose raw goal or evidence strings") {
    val evals =
      List(
        OpeningGoals.Evaluation(
          goalName = "Dutch E4 Outpost",
          status = OpeningGoals.Status.Achieved,
          supportedEvidence = List("Sound", "King safe", "e4 outpost occupied"),
          missingEvidence = Nil,
          confidence = 0.9
        ),
        OpeningGoals.Evaluation(
          goalName = "Flank Fianchetto Support",
          status = OpeningGoals.Status.Partial,
          supportedEvidence = List("PV activates the long diagonal"),
          missingEvidence = List("lack of d5 restraint/space allows opponent to equalize easily"),
          confidence = 0.6
        ),
        OpeningGoals.Evaluation(
          goalName = "Kingside Storm",
          status = OpeningGoals.Status.Premature,
          supportedEvidence = Nil,
          missingEvidence = List("pawn storm not yet supported"),
          confidence = 0.3
        ),
        OpeningGoals.Evaluation(
          goalName = "French Base Chipper",
          status = OpeningGoals.Status.Failed,
          supportedEvidence = Nil,
          missingEvidence = List("d-pawn strike does not contest e4"),
          confidence = 0.2
        )
      )

    val leakedTerms =
      List(
        "Dutch E4 Outpost",
        "Flank Fianchetto Support",
        "Kingside Storm",
        "French Base Chipper",
        "Sound",
        "King safe",
        "outpost occupied",
        "long diagonal",
        "equalize easily",
        "pawn storm",
        "contest e4"
      )

    evals.foreach { evaluation =>
      val text = NarrativeLexicon.getGoalStatusDescription(bead = 0, evaluation = evaluation)
      assert(text.toLowerCase.contains("opening"), clue(text))
      leakedTerms.foreach(term => assert(!text.contains(term), clue(text)))
    }
  }

  test("motif prefix selection is table-driven rather than an else-if chain") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeLexicon.scala"))
    val methodBody = source.substring(source.indexOf("def getMotifPrefix"), source.indexOf("def getThreatStatement"))
    val tableSource =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeMotifPrefixTable.scala"))

    assert(methodBody.contains("NarrativeMotifPrefixTable.templatesFor"), clue(methodBody))
    assert(!methodBody.contains("MotifPrefixRule"), clue(methodBody))
    assert(!methodBody.contains("else if (hasAny"), clue(methodBody))
    assert(tableSource.contains("MotifPrefixRule"), clue(tableSource))
  }

  test("raw relation motifs stay silent unless backed by relation witnesses") {
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("zwischenzug"), ply = 24), None)
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("trapped_piece_queen"), ply = 24), None)
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("domination"), ply = 24), None)
  }

  test("relation witness-only motifs are not generic motif-prefix signals") {
    assert(!NarrativeLexicon.isMotifPrefixSignal("zwischenzug"))
    assert(!NarrativeLexicon.isMotifPrefixSignal("trapped_piece_queen"))
    assert(!NarrativeLexicon.isMotifPrefixSignal("domination"))
    assert(!NarrativeLexicon.isMotifPrefixSignal("stalemate_trap"))
    assert(!NarrativeLexicon.isMotifPrefixSignal("perpetual_check"))
    assert(NarrativeLexicon.isMotifPrefixSignal("stalemate"))
    assert(NarrativeLexicon.isMotifPrefixSignal("knight_domination"))
  }

  test("witness-only raw relation motifs do not emit motif prefixes") {
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("stalemate_trap"), ply = 52), None)
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("perpetual_check"), ply = 52), None)
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("TrappedPiece(Queen,h4)"), ply = 52), None)
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("Domination(Knight,e5)"), ply = 52), None)
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("Zwischenzug(Nf7)"), ply = 52), None)
    assert(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("stalemate"), ply = 52).nonEmpty)
  }

  test("outline canonical motif terms consume the deferred relation catalog") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala"))
    val body =
      source.substring(source.indexOf("private def canonicalTermForMotif"), source.indexOf("private def buildImbalanceContrast"))

    assert(body.contains("deferredRelationCanonicalTerm"), clue(body))
    assert(body.contains("deferredFallbackForMotifTag"), clue(body))
    assert(!body.contains("deferredFallbackLabelForMotifTag"), clue(body))
  }

  test("outline theme keywords consume the deferred relation catalog") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala"))
    val body =
      source.substring(source.indexOf("private def buildThemeKeywordSentence"), source.indexOf("private def buildCanonicalMotifTermSentence"))

    assert(body.contains(".flatMap(themeKeywordForMotif)"), clue(body))
    assert(body.contains("private def themeKeywordForMotif"), clue(body))
    assert(body.contains("deferredRelationCanonicalTerm(motif)"), clue(body))
    assert(body.contains("case Some(term) => term"), clue(body))
  }

  test("outline tactical tension does not promote deferred relation motif tags") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala"))
    val tensionBody =
      source.substring(source.indexOf("private def contextHighTension"), source.indexOf("private def contextMotifHash"))

    assert(tensionBody.contains("motifSignals.exists(tacticalTensionMotif)"), clue(tensionBody))
    assert(!tensionBody.contains("zwischenzug"), clue(tensionBody))
    assert(source.contains("private def tacticalTensionMotif"), clue(source))
    assert(source.contains("RelationObservationCatalog.deferredFallbackForMotifTag(motif).isEmpty"), clue(source))
  }

  test("outline context motifs use typed facts instead of tactic evidence prose parsing") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala"))
    val body =
      source.substring(source.indexOf("private def collectDerivedContextMotifs"), source.indexOf("private def conceptToMotif"))

    assert(body.contains("CommentaryIdeaSurface.factTags"), clue(body))
    assert(!body.contains("tacticEvidence"), clue(body))
    assert(!source.contains("private def tacticEvidenceMotifs"), clue(source))
  }

  test("outline context motifs keep raw draw and endgame concepts out of public theme authority") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala"))
    val sourcesBody =
      source.substring(source.indexOf("private def contextMotifSources"), source.indexOf("private def contextConceptLeadMotifs"))
    val contextOnlyBody =
      source.substring(source.indexOf("private def endgameContextOnlyConcept"), source.indexOf("private def positionalTagMotifs"))

    assert(sourcesBody.contains(".filterNot(endgameContextOnlyConcept)"), clue(sourcesBody))
    List("stalemate", "perpetual", "fortress", "zugzwang", "forced_draw", "draw_resource").foreach { term =>
      assert(contextOnlyBody.contains(term), clue(term))
    }
  }

  test("outline cp-loss severity fallbacks stay objective instead of practical or result claims") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala"))
    val severityBody =
      source.substring(source.indexOf("private def severityIssueConsequence"), source.indexOf("private def isForcingReplySan"))
    val tailBody =
      source.substring(source.indexOf("private def buildSeverityTail"), source.indexOf("private def containsBenchmarkNegativeLexicon"))
    val defaultIssueBody =
      source.substring(source.indexOf("private def defaultIssueBySeverity"), source.indexOf("private def motifName"))
    val alternativeContrastBody =
      source.substring(source.indexOf("private def rankTwoContrastTemplates"), source.indexOf("private def appendStrategicImplication"))
    val strategicImplicationBody =
      source.substring(source.indexOf("private def strategicImplicationTemplates"), source.indexOf("private def strategicImplicationSeed"))
    val alternativeBaseBody =
      source.substring(source.indexOf("private def alternativeBaseTemplates"), source.indexOf("private def alternativeDiversifiedMaterial"))
    val combined =
      List(severityBody, tailBody, defaultIssueBody, alternativeContrastBody, strategicImplicationBody, alternativeBaseBody)
        .mkString("\n")
        .toLowerCase
    val forbidden =
      List(
        "conversion becomes straightforward",
        "king safety and coordination collapse",
        "forcing control shifts",
        "direct technical route",
        "concedes initiative",
        "defensive workload",
        "practical control",
        "practical gap",
        "practical margin",
        "practical deficit",
        "practical tier",
        "practical handling",
        "technical ease",
        "easier to handle",
        "easier plan",
        "comfortable piece play",
        "practical initiative",
        "smoother sequence",
        "simpler choices",
        "defensive duties",
        "lightens defensive duties",
        "technically manageable",
        "technical endgame"
      )

    forbidden.foreach(term => assert(!combined.contains(term), clue(term)))
    assert(combined.contains("objective gap"), clue(combined))
    assert(combined.contains("engine comparison"), clue(combined))
  }

  test("motif delta prose keeps witness-only relation labels silent") {
    assertEquals(NarrativeLexicon.getMotifAppearsStatement(bead = 0, motif = "zwischenzug"), "")
    assertEquals(NarrativeLexicon.getMotifFadesStatement(bead = 0, motif = "trapped_piece_queen"), "")
    assertEquals(NarrativeLexicon.getMotifAppearsStatement(bead = 0, motif = "domination"), "")
    assertEquals(NarrativeLexicon.getMotifAppearsStatement(bead = 0, motif = "stalemate_trap"), "")
    assertEquals(NarrativeLexicon.getMotifFadesStatement(bead = 0, motif = "perpetual_check"), "")
    assert(NarrativeLexicon.getMotifAppearsStatement(bead = 0, motif = "fork").toLowerCase.contains("fork"))
  }
