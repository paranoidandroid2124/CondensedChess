package lila.llm.tools.review

import java.nio.file.Paths

import chess.White
import munit.FunSuite
import play.api.libs.json.Json

import lila.llm.*
import lila.llm.analysis.UserFacingSignalSanitizer
import lila.llm.model.StrategicPlanExperiment
import lila.llm.model.authoring.*
import lila.llm.tools.review.CommentarySceneCoverageCollectionMaterializer.{ QuietRichRow, retargetManifestEntries, selectQuietRichRowsForCollection }

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

  private def whyNowEvidence(questionId: String): QuestionEvidence =
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

  private def coverageSnapshot(
      gameKey: String,
      ply: Int,
      phase: String,
      digest: NarrativeSignalDigest,
      strategyPack: Option[StrategyPack] = None
  ): SliceSnapshot =
    val base = whyNowSnapshot(gameKey, ply, concreteOwner = false)
    base.copy(
      phase = phase,
      data = minimalExtendedData(ply).copy(phase = phase),
      ctx = base.ctx.copy(header = base.ctx.header.copy(criticality = "Normal", choiceType = "NarrowChoice")),
      strategyPack = strategyPack,
      signalDigest = Some(digest)
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
          plannerSceneReasons = List("owner_family=PlanRace"),
          plannerOwnerCandidates = List("PlanRace:evidence_backed_plan:move_linked"),
          plannerAdmittedFamilies = List("PlanRace:evidence_backed_plan:move_linked"),
          plannerDroppedFamilies = List("DecisionTiming:decision_comparison:timing_loss"),
          plannerSupportMaterialSeparation = List("close_candidate:support_material"),
          plannerProposedFamilyMappings = List("close_candidate->DecisionTiming:DecisionTiming/support_only"),
        plannerDemotionReasons = List("generic_urgency_only"),
          plannerSelectedQuestion = Some("WhosePlanIsFaster"),
          plannerSelectedOwnerFamily = Some("PlanRace"),
          plannerSelectedOwnerSource = Some("evidence_backed_plan"),
          rawDecisionIngressReason = Some("decision_present"),
          rawPvDeltaIngressReason = Some("pv_delta_present_with_content"),
          sanitizedDecisionIngressReason = Some("decision_present"),
          sanitizedPvDeltaIngressReason = Some("pv_delta_present_with_content"),
          surfaceReplayOutcome = Some("bookmaker_planner_owned")
        ).withQuietSupportTrace(
          BookmakerQuietSupportTrace(
            liftApplied = true,
            runtimeGatePassed = Some(true),
            runtimeSceneType = Some("quiet_improvement"),
            candidateBucket = Some("slow_route_improvement"),
            candidateSourceKinds = List("MoveDelta.pv_delta", "Digest.route"),
            candidateVerbFamily = Some("keeps available"),
            candidateText = Some("This keeps the route toward c4 available."),
            factualSentence = Some("The route toward c4 stays available.")
          )
        )

    val js = Json.toJson(entry).as[play.api.libs.json.JsObject]
    val parsed = js.validate[BookmakerOutputEntry].asEither.toOption.get

    assertEquals(parsed.plannerPrimaryKind, Some("WhosePlanIsFaster"))
    assertEquals(parsed.bookmakerFallbackMode, "planner_owned")
    assertEquals(parsed.plannerSceneType, Some("plan_clash"))
    assertEquals(parsed.plannerSceneReasons, List("owner_family=PlanRace"))
    assertEquals(parsed.plannerSelectedOwnerFamily, Some("PlanRace"))
    assertEquals(parsed.rawDecisionIngressReason, Some("decision_present"))
    assertEquals(parsed.sanitizedPvDeltaIngressReason, Some("pv_delta_present_with_content"))
    assertEquals(parsed.plannerSupportMaterialSeparation, List("close_candidate:support_material"))
    assertEquals(parsed.quietSupportLiftApplied, Some(true))
    assertEquals(parsed.quietSupportRuntimeGatePassed, Some(true))
    assertEquals(parsed.quietSupportCandidateBucket, Some("slow_route_improvement"))
    assertEquals((js \ "quietSupportLiftApplied").asOpt[Boolean], Some(true))
    assertEquals((js \ "quietSupportRuntimeGatePassed").asOpt[Boolean], Some(true))
    assertEquals((js \ "track3QuietSupportLiftApplied").toOption, None)
    assertEquals((js \ "track3RuntimeGatePassed").toOption, None)
  }

  test("BookmakerOutputEntry reads legacy quiet-support artifact keys") {
    val legacyJson =
      Json.obj(
        "sampleId" -> "sample_1",
        "gameKey" -> "game_1",
        "sliceKind" -> SliceKind.StrategicChoice,
        "targetPly" -> 24,
        "fen" -> "8/8/8/8/8/8/8/8 w - - 0 1",
        "playedSan" -> "Rc3",
        "playedUci" -> "c1c3",
        "opening" -> "English Opening",
        "commentary" -> "12... Rc3: This puts the rook on c3.",
        "supportRows" -> Json.arr(),
        "advancedRows" -> Json.arr(),
        "sourceMode" -> "rule",
        "model" -> "gpt-5.2",
        "rawResponsePath" -> "C:\\tmp\\raw.json",
        "variationCount" -> 3,
        "cacheHit" -> false,
        "track3QuietSupportLiftApplied" -> true,
        "track3QuietSupportRejectReasons" -> Json.arr("planner_owned_row"),
        "track3RuntimeGatePassed" -> false,
        "track3RuntimeGateRejectReasons" -> Json.arr("move_linked_pv_delta_anchor_missing"),
        "track3RuntimeSceneType" -> "quiet_improvement",
        "track3RuntimeSelectedOwnerFamily" -> "MoveDelta",
        "track3RuntimeSelectedOwnerSource" -> "pv_delta",
        "track3RuntimePvDeltaAvailable" -> true,
        "track3RuntimeSignalDigestAvailable" -> true,
        "track3RuntimeMoveLinkedPvDeltaAnchorAvailable" -> false,
        "track3QuietSupportCandidateBucket" -> "slow_route_improvement",
        "track3QuietSupportCandidateSourceKinds" -> Json.arr("MoveDelta.pv_delta", "Digest.route"),
        "track3QuietSupportCandidateVerbFamily" -> "keeps available",
        "track3QuietSupportCandidateText" -> "This keeps the route toward c4 available.",
        "track3QuietSupportFactualSentence" -> "The route toward c4 stays available."
      )

    val parsed = legacyJson.validate[BookmakerOutputEntry].asEither.toOption.get

    assertEquals(parsed.quietSupportLiftApplied, Some(true))
    assertEquals(parsed.quietSupportRejectReasons, List("planner_owned_row"))
    assertEquals(parsed.quietSupportRuntimeGatePassed, Some(false))
    assertEquals(parsed.quietSupportRuntimeGateRejectReasons, List("move_linked_pv_delta_anchor_missing"))
    assertEquals(parsed.quietSupportRuntimeSceneType, Some("quiet_improvement"))
    assertEquals(parsed.quietSupportRuntimeSelectedOwnerFamily, Some("MoveDelta"))
    assertEquals(parsed.quietSupportRuntimeSelectedOwnerSource, Some("pv_delta"))
    assertEquals(parsed.quietSupportRuntimePvDeltaAvailable, Some(true))
    assertEquals(parsed.quietSupportRuntimeSignalDigestAvailable, Some(true))
    assertEquals(parsed.quietSupportRuntimeMoveLinkedPvDeltaAnchorAvailable, Some(false))
    assertEquals(parsed.quietSupportCandidateBucket, Some("slow_route_improvement"))
    assertEquals(parsed.quietSupportCandidateSourceKinds, List("MoveDelta.pv_delta", "Digest.route"))
    assertEquals(parsed.quietSupportCandidateVerbFamily, Some("keeps available"))
    assertEquals(parsed.quietSupportCandidateText, Some("This keeps the route toward c4 available."))
    assertEquals(parsed.quietSupportFactualSentence, Some("The route toward c4 stays available."))
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

  test("selectSlices keeps scene coverage lane buckets even when the same ply already has a base family slice") {
    val squeeze =
      coverageSnapshot(
        gameKey = "coverage-game",
        ply = 24,
        phase = "middlegame",
        digest =
          NarrativeSignalDigest(
            structureProfile = Some("queenside bind"),
            structuralCue = Some("Black stays tied to c6 and b7."),
            decision = Some("White keeps tightening the bind.")
          ),
        strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List("long queenside squeeze")))
      )
    val transition =
      coverageSnapshot(
        gameKey = "coverage-game",
        ply = 48,
        phase = "endgame",
        digest =
          NarrativeSignalDigest(
            endgameTransitionClaim = Some("The queenless transition leaves White with the only active rook."),
            practicalVerdict = Some("The rook race still favors White if the king stays active.")
          )
      )

    val selectedKinds = selectSlices(List(squeeze, transition)).map(_._1).toSet

    assert(selectedKinds.contains(SliceKind.StrategicChoice), clues(selectedKinds))
    assert(selectedKinds.contains(SliceKind.LongStructuralSqueeze), clues(selectedKinds))
    assert(selectedKinds.contains(SliceKind.EndgameConversion), clues(selectedKinds))
    assert(selectedKinds.contains(SliceKind.TransitionHeavyEndgames), clues(selectedKinds))
    assert(SliceKind.sceneCoverageLaneKinds.contains(SliceKind.ProphylaxisRestraint))
    assert(SliceKind.sceneCoverageLaneKinds.contains(SliceKind.OpeningDeviationAfterMiddlegamePlanClash))
  }

  test("collectOpeningPlanClashCandidates materializes middlegame rows from opening branch evidence") {
    val base =
      coverageSnapshot(
        gameKey = "opening-plan-clash",
        ply = 24,
        phase = "middlegame",
        digest =
          NarrativeSignalDigest(
            opening = Some("Queen's Gambit Declined"),
            structuralCue = Some("White keeps the queenside structure intact."),
            decision = Some("White redirects play to the c-file."),
            opponentPlan = Some("Black still wants ...c5 to free the position.")
          ),
        strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List("keep pressure on c6")))
      )
    val candidate =
      base.copy(
        ctx =
          base.ctx.copy(
            openingEvent =
              Some(lila.llm.model.OpeningEvent.OutOfBook("Qc2", List("Re1", "a3", "h3"), 24))
          )
      )

    val selected = collectOpeningPlanClashCandidates(List(candidate))

    assertEquals(selected.size, 1)
    assertEquals(selected.head.bucket, SliceKind.OpeningDeviationAfterMiddlegamePlanClash)
    assert(selected.head.reasons.contains("opening_branch_event"), clues(selected.head.reasons))
    assert(selected.head.reasons.contains("decision"), clues(selected.head.reasons))
  }

  test("collectOpeningPlanClashCandidates accepts corpus-backed opening precedent on the collection lane") {
    val base =
      coverageSnapshot(
        gameKey = "opening-precedent",
        ply = 26,
        phase = "middlegame",
        digest =
          NarrativeSignalDigest(
            opening = Some("Queen's Gambit Declined"),
            decision = Some("White keeps play on the c-file."),
            opponentPlan = Some("Black still wants ...c5.")
          ),
        strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List("hold the queenside bind")))
      )
    val candidate =
      base.copy(
        ctx =
          base.ctx.copy(
            openingData =
              Some(
                lila.llm.model.OpeningReference(
                  eco = Some("D37"),
                  name = Some("Queen's Gambit Declined"),
                  totalGames = 240,
                  topMoves =
                    List(
                      lila.llm.model.ExplorerMove(
                        uci = "c2c4",
                        san = "c4",
                        total = 240,
                        white = 92,
                        draws = 108,
                        black = 40,
                        performance = 2550
                      )
                    ),
                  sampleGames = Nil,
                  description = Some("exchange variation")
                )
              )
          )
      )

    val selected = collectOpeningPlanClashCandidates(List(candidate))

    assertEquals(selected.size, 1)
    assert(selected.head.reasons.contains("opening_precedent_available"), clues(selected.head.reasons))
  }

  test("collectOpeningPlanClashCandidates rejects generic middlegame rows that only carry an opening label") {
    val generic =
      coverageSnapshot(
        gameKey = "generic-opening-label",
        ply = 24,
        phase = "middlegame",
        digest =
          NarrativeSignalDigest(
            opening = Some("Queen's Gambit Declined"),
            decision = Some("White improves coordination.")
          ),
        strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List("coordinate the rooks")))
      )

    val selected = collectOpeningPlanClashCandidates(List(generic))

    assertEquals(selected, Nil)
  }

  test("collectProphylaxisRestraintCandidates keeps quiet restraint rows with prophylaxis evidence") {
    val candidate =
      coverageSnapshot(
        gameKey = "prophylaxis-quiet-row",
        ply = 38,
        phase = "middlegame",
        digest =
          NarrativeSignalDigest(
            prophylaxisPlan = Some("The move keeps ...c5 from solving Black's space problem."),
            structuralCue = Some("Black stays tied to the c-file and queenside pawns."),
            decision = Some("White keeps the bind instead of trading into relief."),
            opponentPlan = Some("Black still wants ...c5 to free the position."),
            counterplayScoreDrop = Some(55)
          ),
        strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List("keep Black short of queenside counterplay")))
      )

    val selected = collectProphylaxisRestraintCandidates(List(candidate))

    assertEquals(selected.size, 1)
    assertEquals(selected.head.bucket, SliceKind.ProphylaxisRestraint)
    assert(selected.head.reasons.contains("prophylaxis_plan"), clues(selected.head.reasons))
    assert(selected.head.reasons.contains("counterplay_score_drop"), clues(selected.head.reasons))
  }

  test("collectProphylaxisRestraintCandidates rejects generic quiet rows without restraint evidence") {
    val generic =
      coverageSnapshot(
        gameKey = "generic-quiet-row",
        ply = 38,
        phase = "middlegame",
        digest =
          NarrativeSignalDigest(
            structuralCue = Some("White improves the file pressure."),
            decision = Some("White improves coordination.")
          ),
        strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List("improve coordination")))
      )

    val selected = collectProphylaxisRestraintCandidates(List(generic))

    assertEquals(selected, Nil)
  }

  test("selectQuietRichRowsForCollection deduplicates repeated moves and keeps the better prophylaxis source row") {
    val weakerDuplicate =
      QuietRichRow(
        schemaVersion = "cqf_track3_quiet_rich.v1",
        sampleId = "game-1:long_structural_squeeze:55:bookmaker",
        gameKey = "game-1",
        bucket = SliceKind.ProphylaxisRestraint,
        sliceKind = SliceKind.LongStructuralSqueeze,
        targetPly = 55,
        playedSan = "Qa5",
        commentary = "Qa5",
        realShard = true,
        status = "upstream_blocked",
        lane = "upstream-blocked / non-eligible rows",
        statusReasons = Nil,
        plannerSceneType = Some("forcing_defense"),
        bookmakerFallbackMode = Some("planner_owned"),
        quietnessVerified = Some(true),
        quietnessSpreadCp = Some(8),
        quietnessNotes = Nil,
        directSources = Nil,
        supportSources = List("Digest.pressure")
      )
    val strongerDuplicate =
      weakerDuplicate.copy(
        sampleId = "game-1:transition_heavy_endgames:55:bookmaker",
        sliceKind = SliceKind.TransitionHeavyEndgames,
        plannerSceneType = Some("quiet_improvement"),
        bookmakerFallbackMode = Some("exact_factual"),
        quietnessSpreadCp = Some(4),
        directSources = List("MoveDelta.pv_delta")
      )
    val distinctRow =
      weakerDuplicate.copy(
        sampleId = "game-2:long_structural_squeeze:58:bookmaker",
        gameKey = "game-2",
        targetPly = 58,
        playedSan = "Rc8",
        bookmakerFallbackMode = Some("exact_factual"),
        quietnessSpreadCp = Some(5),
        directSources = List("MoveDelta.pv_delta")
      )

    val selected =
      selectQuietRichRowsForCollection(
        quietRichRows = List(weakerDuplicate, strongerDuplicate, distinctRow),
        bucket = SliceKind.ProphylaxisRestraint,
        limit = 10,
        preferredSliceKinds =
          List(
            SliceKind.LongStructuralSqueeze,
            SliceKind.TransitionHeavyEndgames,
            SliceKind.EndgameConversion
          )
      )

    assertEquals(selected.map(_.sampleId), List("game-1:transition_heavy_endgames:55:bookmaker", "game-2:long_structural_squeeze:58:bookmaker"))
  }

  test("retargetManifestEntries rewrites sample ids and slice kinds for collection lanes") {
    val entries =
      List(
        SliceManifestEntry(
          sampleId = "game-1:opening_transition:10:bookmaker",
          gameKey = "game-1",
          surface = "bookmaker",
          sliceKind = SliceKind.OpeningTransition,
          targetPly = 10,
          fen = questionFen,
          playedSan = "a6",
          opening = Some("Sicilian Defense"),
          tags = List("master_classical", SliceKind.OpeningTransition),
          pgnPath = "C:\\tmp\\game-1.pgn",
          playedUci = "a7a6",
          variant = "standard",
          mixBucket = Some(MixBucket.MasterClassical)
        ),
        SliceManifestEntry(
          sampleId = "game-1:opening_transition:10:chronicle",
          gameKey = "game-1",
          surface = "chronicle",
          sliceKind = SliceKind.OpeningTransition,
          targetPly = 10,
          fen = questionFen,
          playedSan = "a6",
          opening = Some("Sicilian Defense"),
          tags = List("master_classical", SliceKind.OpeningTransition),
          pgnPath = "C:\\tmp\\game-1.pgn",
          playedUci = "a7a6",
          variant = "standard",
          mixBucket = Some(MixBucket.MasterClassical)
        )
      )

    val retargeted = retargetManifestEntries(entries, "opening_deviation_after_stable_development")

    assertEquals(
      retargeted.map(_.sampleId),
      List(
        "game-1:opening_deviation_after_stable_development:10:bookmaker",
        "game-1:opening_deviation_after_stable_development:10:chronicle"
      )
    )
    assert(retargeted.forall(_.sliceKind == "opening_deviation_after_stable_development"))
    assert(retargeted.forall(_.tags.contains("opening_deviation_after_stable_development")))
    assert(!retargeted.exists(_.tags.contains(SliceKind.OpeningTransition)))
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
