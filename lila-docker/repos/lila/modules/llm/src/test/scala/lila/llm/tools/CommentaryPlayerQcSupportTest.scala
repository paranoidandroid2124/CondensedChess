package lila.llm.tools

import java.nio.file.Paths

import munit.FunSuite
import play.api.libs.json.Json

import lila.llm.*
import lila.llm.analysis.UserFacingSignalSanitizer
import lila.llm.model.StrategicPlanExperiment
import lila.llm.model.authoring.*

class CommentaryPlayerQcSupportTest extends FunSuite:

  import CommentaryPlayerQcSupport.*

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
          SupportRow("Why it stayed conditional", "This route stays secondary until the c-file opens.")
        ),
        advancedRows = Nil,
        sliceKind = SliceKind.StrategicChoice
      )

    assert(flags.contains("generic_filler_main_prose"), clues(flags))
    assert(flags.contains("anchored_support_missing_from_prose"), clues(flags))
    assert(flags.contains("conditionality_blur"), clues(flags))
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
        whyAbsentFromTopMultiPV = List("Preparing e-break Break.", "Opening Development and Center Control."),
        signalDigest = Some(
          NarrativeSignalDigest(
            latentPlan = Some("Preparing d-break Break"),
            latentReason = Some("Piece Activation")
          )
        )
      )

    val (support, advanced) = buildBookmakerRows(response)

    assertEquals(
      support.find(_.label == "Main plans").map(_.text),
      Some("Preparing the e-break [pv coupled], Improving piece placement")
    )
    assertEquals(
      support.find(_.label == "Why it stayed conditional").map(_.text),
      Some("Preparing the e-break.; Development and central control.")
    )
    assertEquals(advanced.find(_.label == "Latent plan").map(_.text), Some("Preparing the d-break"))
    assertEquals(advanced.find(_.label == "Latent reason").map(_.text), Some("Improving piece placement"))
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
            investedMaterial = Some(100),
            latentPlan = Some("Piece Activation"),
            latentReason = Some("Improving piece placement")
          )
        )
      )

    val (support, advanced) = buildBookmakerRows(response)

    assertEquals(support.find(_.label == "Main plans").map(_.text), Some("Preparing the e-break"))
    assertEquals(advanced.find(_.label == "Latent plan"), None)
    assertEquals(advanced.find(_.label == "Latent reason"), None)
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
