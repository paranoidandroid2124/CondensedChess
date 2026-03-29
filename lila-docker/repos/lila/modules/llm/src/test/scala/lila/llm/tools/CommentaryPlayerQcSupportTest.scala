package lila.llm.tools

import java.nio.file.Paths

import chess.White
import munit.FunSuite
import play.api.libs.json.Json

import lila.llm.*
import lila.llm.analysis.UserFacingSignalSanitizer
import lila.llm.model.StrategicPlanExperiment
import lila.llm.model.authoring.*

class CommentaryPlayerQcSupportTest extends FunSuite:

  import CommentaryPlayerQcSupport.*

  private val questionFen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"

  private def whyNowQuestion(id: String = "q_why_now"): AuthorQuestion =
    AuthorQuestion(
      id = id,
      kind = AuthorQuestionKind.WhyNow,
      priority = 10,
      question = "Why now?"
    )

  private def whyNowEvidence(questionId: String = "q_why_now"): QuestionEvidence =
    QuestionEvidence(
      questionId = questionId,
      purpose = "reply_multipv",
      branches =
        List(
          EvidenceBranch(
            keyMove = "e4",
            line = "a) 24... e4 25. Qd2 Re8",
            evalCp = Some(40)
          )
        )
    )

  private def minimalExtendedData(ply: Int): lila.llm.model.ExtendedAnalysisData =
    lila.llm.model.ExtendedAnalysisData(
      fen = questionFen,
      nature = lila.llm.model.PositionNature(lila.llm.model.NatureType.Dynamic, 0.5, 0.5, "Dynamic"),
      motifs = Nil,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = Nil,
      structuralWeaknesses = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      prevMove = Some("e2e4"),
      ply = ply,
      evalCp = 35,
      isWhiteToMove = true,
      phase = "middlegame",
      integratedContext = Some(lila.llm.analysis.IntegratedContext(evalCp = 35, isWhiteToMove = true))
    )

  private def whyNowSnapshot(gameKey: String, ply: Int, concreteOwner: Boolean): SliceSnapshot =
    val question = whyNowQuestion()
    val ctx =
      lila.llm.model.NarrativeContext(
        fen = questionFen,
        header = lila.llm.model.ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
        ply = ply,
        playedMove = Some("e2e4"),
        playedSan = Some("e4"),
        summary = lila.llm.model.NarrativeSummary("Central pressure", None, "NarrowChoice", "Maintain", "0.00"),
        threats =
          lila.llm.model.ThreatTable(
            toUs =
              Option.when(concreteOwner)(
                List(
                  lila.llm.model.ThreatRow(
                    kind = "Mate",
                    side = "US",
                    square = Some("h7"),
                    lossIfIgnoredCp = 500,
                    turnsToImpact = 1,
                    bestDefense = Some("Qh4+"),
                    defenseCount = 1,
                    insufficientData = false
                  )
                )
              ).getOrElse(Nil),
            toThem = Nil
          ),
        pawnPlay = lila.llm.model.PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
        plans = lila.llm.model.PlanTable(Nil, Nil),
        delta = None,
        phase = lila.llm.model.PhaseContext("Middlegame", "Balanced middlegame"),
        candidates = Nil,
        authorQuestions = List(question),
        authorEvidence = List(whyNowEvidence(question.id)),
        renderMode = lila.llm.model.NarrativeRenderMode.FullGame
      )
    SliceSnapshot(
      entry =
        CatalogEntry(
          gameKey = gameKey,
          source = "test",
          sourceId = "test",
          pgnPath = "C:\\tmp\\test.pgn",
          mixBucket = MixBucket.Club,
          familyTags = Nil,
          ratingBucket = RatingBucket.Club,
          timeControlBucket = TimeControlBucket.Rapid
        ),
      plyData = PgnAnalysisHelper.PlyData(ply, questionFen, "e4", "e2e4", White),
      openingLabel = None,
      phase = "middlegame",
      evalBeforeCp = 20,
      evalAfterCp = 35,
      evalSwingCp = 15,
      data = minimalExtendedData(ply),
      ctx = ctx,
      strategyPack = None,
      signalDigest = None,
      truthContract = None,
      refs = None
    )

  test("splitPgnGames splits multi-game PGN files") {
    val raw =
      """[Event "Mini 1"]
        |[Site "?"]
        |[Date "2026.03.20"]
        |[Round "1"]
        |[White "A"]
        |[Black "B"]
        |[Result "1-0"]
        |
        |1.e4 e5 2.Nf3 Nc6 1-0
        |
        |[Event "Mini 2"]
        |[Site "?"]
        |[Date "2026.03.21"]
        |[Round "1"]
        |[White "C"]
        |[Black "D"]
        |[Result "0-1"]
        |
        |1.d4 d5 2.c4 e6 0-1
        |""".stripMargin

    val games = splitPgnGames(raw)
    assertEquals(games.size, 2)
    assert(games.forall(_.startsWith("[Event ")))
  }

  test("rating and time-control buckets infer practical mix buckets") {
    val headers = Map(
      "WhiteElo" -> "2680",
      "BlackElo" -> "2710",
      "TimeControl" -> "5400+30"
    )

    assertEquals(ratingBucketOf(headers), RatingBucket.Elite)
    assertEquals(timeControlBucketOf(headers), TimeControlBucket.Classical)
    assertEquals(inferMixBucket(Paths.get("C:\\tmp\\masters\\game.pgn"), headers), MixBucket.MasterClassical)
  }

  test("selectCorpus enforces event cap and per-player cap") {
    val sharedEvent =
      (1 to 20).toList.map { idx =>
        CatalogEntry(
          gameKey = s"shared-$idx",
          source = "x",
          sourceId = "x",
          pgnPath = s"x/$idx.pgn",
          mixBucket = MixBucket.Club,
          familyTags = Nil,
          ratingBucket = RatingBucket.Club,
          timeControlBucket = TimeControlBucket.Rapid,
          openingMacroFamily = Some("open_game"),
          white = Some("PlayerA"),
          black = Some(s"Opp$idx"),
          event = Some("Same Event")
        )
      }
    val extra =
      (1 to 10).toList.map { idx =>
        CatalogEntry(
          gameKey = s"extra-$idx",
          source = "x",
          sourceId = "x",
          pgnPath = s"x/extra-$idx.pgn",
          mixBucket = MixBucket.Club,
          familyTags = Nil,
          ratingBucket = RatingBucket.Club,
          timeControlBucket = TimeControlBucket.Rapid,
          openingMacroFamily = Some("open_game"),
          white = Some(s"Player$idx"),
          black = Some(s"Alt$idx"),
          event = Some(s"Event $idx")
        )
      }

    val selected = selectCorpus(sharedEvent ++ extra)
    assert(selected.count(_.event.contains("Same Event")) <= 12)
    assert(selected.count(_.white.contains("PlayerA")) <= 6)
  }

  test("reviewFlags mark meta language and missing concrete anchors") {
    val flags =
      reviewFlags(
        prose = "The move improves plan fit and keeps the initiative.",
        supportRows = Nil,
        advancedRows = Nil,
        sliceKind = SliceKind.StrategicChoice
      )

    assert(flags.exists(_.startsWith("meta_language:")), clues(flags))
    assert(flags.contains("missing_concrete_anchor"), clues(flags))
    assert(flags.contains("sidecar_empty_prose_risk"), clues(flags))
  }

  test("reviewFlags escalate generic filler when anchored support is missing from prose") {
    val flags =
      reviewFlags(
        prose = "The next step is to finish development without giving up the center.",
        supportRows = List(
          SupportRow("Structure", "The e-file break and pressure on d4 remain the real theme."),
          SupportRow("Evidence note", "The c-file still matters before the break works cleanly.")
        ),
        advancedRows = Nil,
        sliceKind = SliceKind.StrategicChoice
      )

    assert(flags.contains("generic_filler_main_prose"), clues(flags))
    assert(flags.contains("anchored_support_missing_from_prose"), clues(flags))
  }

  test("reviewFlags catch taxonomy residue outside the player-facing contract") {
    val flags =
      reviewFlags(
        prose = "The move keeps pressure on d4.",
        supportRows = List(
          SupportRow("Evidence note", "PlayableByPV under strict evidence mode with theme:piece_redeployment.")
        ),
        advancedRows = Nil,
        sliceKind = SliceKind.CompensationOrExchangeSac
      )

    assert(flags.exists(_.startsWith("taxonomy_residue:")), clues(flags))
  }

  test("buildBookmakerRows humanizes raw support labels") {
    val response =
      CommentResponse(
        commentary = "7... O-O: The move prepares the e pawn break.",
        concepts = Nil,
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "break",
            planName = "Preparing e-break Break",
            rank = 1,
            score = 0.71,
            preconditions = Nil,
            executionSteps = Nil,
            failureModes = Nil,
            viability = PlanViability(0.71, "high", "Exploiting Space Advantage"),
            evidenceSources = Nil
          ),
          PlanHypothesis(
            planId = "dev",
            planName = "Piece Activation",
            rank = 2,
            score = 0.63,
            preconditions = Nil,
            executionSteps = Nil,
            failureModes = Nil,
            viability = PlanViability(0.63, "medium", "Opening Development and Center Control"),
            evidenceSources = Nil
          )
        ),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "break",
            themeL1 = "pawn_break_preparation",
            subplanId = None,
            evidenceTier = "pv_coupled",
            supportProbeCount = 0,
            refuteProbeCount = 0,
            bestReplyStable = false,
            futureSnapshotAligned = false,
            counterBreakNeutralized = false,
            moveOrderSensitive = true,
            experimentConfidence = 0.51
          )
        ),
        signalDigest = Some(NarrativeSignalDigest())
      )

    val (support, advanced) = buildBookmakerRows(response)

    assertEquals(
      support.find(_.label == "Main plans").map(_.text),
      Some("Preparing the e-break [pv coupled], Improving piece placement")
    )
    assertEquals(support.find(_.label == "Why it stayed conditional"), None)
    assertEquals(advanced.find(_.label == "Latent plan"), None)
    assertEquals(advanced.find(_.label == "Latent reason"), None)
  }

  test("buildBookmakerRows drops abstract support labels in compensation context") {
    val response =
      CommentResponse(
        commentary = "12... Rxb2: The material can wait while the queenside files stay active.",
        concepts = Nil,
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "piece-placement",
            planName = "Piece Activation",
            rank = 1,
            score = 0.68,
            preconditions = Nil,
            executionSteps = Nil,
            failureModes = Nil,
            viability = PlanViability(0.68, "medium", "Improving piece placement"),
            evidenceSources = Nil
          ),
          PlanHypothesis(
            planId = "break",
            planName = "Preparing e-break Break",
            rank = 2,
            score = 0.66,
            preconditions = Nil,
            executionSteps = Nil,
            failureModes = Nil,
            viability = PlanViability(0.66, "medium", "Preparing the e-break"),
            evidenceSources = Nil
          )
        ),
        signalDigest = Some(
          NarrativeSignalDigest(
            compensation = Some("The material can wait while the queenside files stay active."),
            investedMaterial = Some(100)
          )
        )
      )

    val (support, advanced) = buildBookmakerRows(response)

    assertEquals(support.find(_.label == "Main plans").map(_.text), Some("Preparing the e-break"))
    assertEquals(advanced.find(_.label == "Latent plan"), None)
    assertEquals(advanced.find(_.label == "Latent reason"), None)
  }

  test("sentenceCount ignores chess move-number dots in cited bookmaker prose") {
    val prose =
      """12... Rc3: White keeps the initiative ahead of Queenside Counterplay because the reply window is short.
        |
        |The real fight is on the kingside.
        |
        |One concrete line that keeps the idea in play is a) 23... Rc8 24. Rg3 Rc7 25. Qxg7+ (0.40)""".stripMargin

    assertEquals(sentenceCount(prose), 3)
  }

  test("BookmakerOutputEntry round-trips planner signoff fields") {
    val entry =
      BookmakerOutputEntry(
        sampleId = "sample_1",
        gameKey = "game_1",
        sliceKind = SliceKind.StrategicChoice,
        targetPly = 24,
        fen = "8/8/8/8/8/8/8/8 w - - 0 1",
        playedSan = "Rc3",
        playedUci = "c1c3",
        opening = Some("English Opening"),
        commentary = "12... Rc3: This puts the rook on c3.",
        supportRows = Nil,
        advancedRows = Nil,
        sourceMode = "rule",
        model = Some("gpt-5.2"),
        rawResponsePath = "C:\\tmp\\raw.json",
        variationCount = 3,
        cacheHit = false,
        plannerPrimaryKind = Some("WhosePlanIsFaster"),
        plannerPrimaryFallbackMode = Some("PlannerOwned"),
        plannerSecondaryKind = Some("WhyNow"),
        plannerSecondarySurfaced = true,
        bookmakerFallbackMode = "planner_owned",
        plannerSceneType = Some("plan_clash"),
        plannerOwnerCandidates = List("PlanRace:evidence_backed_plan:move_linked"),
        plannerAdmittedFamilies = List("PlanRace:evidence_backed_plan:move_linked"),
        plannerDroppedFamilies = List("DecisionTiming:decision_comparison:timing_loss"),
        plannerSupportMaterialSeparation = List("close_candidate:support_material"),
        plannerProposedFamilyMappings = List("close_candidate->DecisionTiming:DecisionTiming/support_only"),
        plannerDemotionReasons = List("generic_urgency_only"),
        plannerSelectedQuestion = Some("WhosePlanIsFaster"),
        plannerSelectedOwnerFamily = Some("PlanRace"),
        plannerSelectedOwnerSource = Some("evidence_backed_plan"),
        surfaceReplayOutcome = Some("bookmaker_planner_owned")
      )

    val parsed = Json.toJson(entry).validate[BookmakerOutputEntry].asEither.toOption.get

    assertEquals(parsed.plannerPrimaryKind, Some("WhosePlanIsFaster"))
    assertEquals(parsed.bookmakerFallbackMode, "planner_owned")
    assertEquals(parsed.plannerSceneType, Some("plan_clash"))
    assertEquals(parsed.plannerSelectedOwnerFamily, Some("PlanRace"))
    assertEquals(parsed.plannerSupportMaterialSeparation, List("close_candidate:support_material"))
  }

  test("allowCompensationSupportText keeps only concrete compensation support phrasing") {
    assert(!UserFacingSignalSanitizer.allowCompensationSupportText("Winning the material back can wait because the open lines stay active."))
    assert(UserFacingSignalSanitizer.allowCompensationSupportText("Winning the material back can wait because pressure on d4 is still there."))
    assert(UserFacingSignalSanitizer.allowCompensationSupportText("The rooks can take over the queenside files next."))
  }

  test("ReviewQueueEntry reads legacy rows without audit metadata") {
    val js =
      Json.obj(
        "sampleId" -> "sample_1",
        "surface" -> "bookmaker",
        "sliceKind" -> SliceKind.StrategicChoice,
        "fen" -> "8/8/8/8/8/8/8/8 w - - 0 1",
        "playedSan" -> "Nd5",
        "mainProse" -> "The move keeps pressure on d5.",
        "supportRows" -> Json.arr(),
        "advancedRows" -> Json.arr(),
        "flags" -> Json.arr()
      )

    val parsed = js.validate[ReviewQueueEntry].asEither.toOption.get

    assertEquals(parsed.gameId, "sample_1")
    assertEquals(parsed.reviewKind, ReviewKind.FocusMoment)
    assertEquals(parsed.tier, None)
  }

  test("JudgmentEntry reads legacy rows without surface metadata") {
    val js =
      Json.obj(
        "sampleId" -> "sample_1",
        "reviewer" -> "qa",
        "severity" -> ReviewSeverity.Blocker,
        "rubric" -> Json.arr("player-facing"),
        "blockerType" -> BlockerType.OverloadedMainClaim,
        "notes" -> "Body stayed generic.",
        "fixFamily" -> Json.arr(FixFamily.GenericFillerMainProse)
      )

    val parsed = js.validate[JudgmentEntry].asEither.toOption.get

    assertEquals(parsed.surface, None)
    assertEquals(parsed.fixFamily, List(FixFamily.GenericFillerMainProse))
  }

  test("selectSlices adds question_why_now only for carried concrete timing-owner scenes") {
    val concrete = whyNowSnapshot("g1", 24, concreteOwner = true)
    val generic = whyNowSnapshot("g1", 26, concreteOwner = false)

    val selectedKinds = selectSlices(List(concrete, generic)).map(_._1)
    val genericOnlyKinds = selectSlices(List(generic)).map(_._1)

    assert(selectedKinds.contains(SliceKind.QuestionWhyNow), clues(selectedKinds))
    assertEquals(selectedKinds.count(_ == SliceKind.QuestionWhyNow), 1)
    assert(!genericOnlyKinds.contains(SliceKind.QuestionWhyNow), clues(genericOnlyKinds))
  }

  test("selectQuestionWhyNowSnapshot can restrict to visible-capable plies") {
    val concreteEarly = whyNowSnapshot("g1", 24, concreteOwner = true)
    val concreteVisible = whyNowSnapshot("g1", 30, concreteOwner = true)

    val unrestricted = selectQuestionWhyNowSnapshot(List(concreteEarly, concreteVisible))
    val visibleOnly = selectQuestionWhyNowSnapshot(List(concreteEarly, concreteVisible), allowedPlies = Some(Set(30)))
    val noVisible = selectQuestionWhyNowSnapshot(List(concreteEarly, concreteVisible), allowedPlies = Some(Set(18)))

    assertEquals(unrestricted.map(_.plyData.ply), Some(30))
    assertEquals(visibleOnly.map(_.plyData.ply), Some(30))
    assertEquals(noVisible, None)
  }
