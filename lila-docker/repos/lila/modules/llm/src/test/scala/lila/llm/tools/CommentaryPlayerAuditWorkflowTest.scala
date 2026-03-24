package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import munit.FunSuite
import play.api.libs.json.Json

class CommentaryPlayerAuditWorkflowTest extends FunSuite:

  import CommentaryPlayerQcSupport.*
  import RealPgnNarrativeEvalRunner.*

  private def tempDir(name: String): Path =
    Files.createTempDirectory(name)

  private def writeJson(path: Path, value: play.api.libs.json.JsValue): Unit =
    Files.createDirectories(path.getParent)
    Files.writeString(path, Json.prettyPrint(value), StandardCharsets.UTF_8)

  private def writeJsonLines[T](path: Path, values: List[T])(using play.api.libs.json.Writes[T]): Unit =
    Files.createDirectories(path.getParent)
    val payload = values.map(value => Json.stringify(Json.toJson(value))).mkString("", "\n", "\n")
    Files.writeString(path, payload, StandardCharsets.UTF_8)

  private def sampleFocusMoment(
      ply: Int = 24,
      gameArcNarrative: String = "White fixed the d5 square and should now build around it.",
      bookmakerCommentary: String = "12... Nd5: The move fixes the knight on d5 and prepares c4 next.",
      activeNote: Option[String] = Some("The key idea is to keep the knight on d5 and follow with c4.")
  ): FocusMomentReport =
    FocusMomentReport(
      ply = ply,
      moveNumber = 12,
      side = "white",
      momentType = "AdvantageSwing",
      selectionKind = "key",
      dominantIdea = Some("pressure on d5"),
      secondaryIdea = Some("the c-file"),
      campaignOwner = Some("White"),
      ownerMismatch = false,
      gameArcCompensationPosition = false,
      bookmakerCompensationPosition = false,
      compensationPosition = false,
      exemplarVisible = false,
      gameArcCompensationSubtype = None,
      bookmakerCompensationSubtype = None,
      compensationSubtype = None,
      gameArcPreparationCompensationSubtype = None,
      bookmakerPreparationCompensationSubtype = None,
      gameArcPayoffCompensationSubtype = None,
      bookmakerPayoffCompensationSubtype = None,
      gameArcDisplaySubtypeSource = "path",
      bookmakerDisplaySubtypeSource = "path",
      activeCompensationMention = false,
      bookmakerCompensationMention = false,
      execution = Some("rook toward c1"),
      objective = Some("c4 to support the knight"),
      focus = Some("The knight stays pointed at d5."),
      gameArcNarrative = gameArcNarrative,
      bookmakerCommentary = bookmakerCommentary,
      bookmakerSourceMode = "rule",
      activeNoteStatus = activeNote.fold("omitted")(_ => "rule"),
      activeNote = activeNote,
      probeRequestCount = 0,
      probeRefinementRequestCount = 0
    )

  private def sampleGameReport(id: String, tier: String, family: String, label: String): GameReport =
    GameReport(
      id = id,
      tier = tier,
      family = family,
      label = label,
      event = Some("Sample Event"),
      date = Some("2026.03.22"),
      opening = Some("Sample Opening"),
      result = Some("1-0"),
      totalPlies = 80,
      initialMomentCount = 3,
      refinedMomentCount = 3,
      strategicMomentCount = 3,
      threadCount = 2,
      activeNoteCount = 1,
      probeCandidateMoments = 0,
      probeCandidateRequests = 0,
      probeExecutedRequests = 0,
      probeUnsupportedRequests = 0,
      usedProbeRefinement = false,
      overallThemes = List("Improving piece placement", "Pressure on d5"),
      visibleMomentPlies = List(24),
      focusMoments = List(sampleFocusMoment())
    )

  private def sampleRunReport(game: GameReport): RunReport =
    RunReport(
      generatedAt = "2026-03-22T00:00:00Z",
      corpusTitle = "sample",
      corpusAsOfDate = "2026-03-22",
      depth = 10,
      multiPv = 3,
      enginePath = "stockfish",
      summary = Summary(
        totalGames = 1,
        totalFocusMoments = game.focusMoments.size,
        totalActiveNotes = game.activeNoteCount,
        gamesUsingProbeRefinement = 0,
        totalProbeCandidateRequests = 0,
        totalProbeExecutedRequests = 0,
        totalProbeUnsupportedRequests = 0,
        familyCounts = Map(game.family -> 1),
        compensationSubtypeCounts = Map.empty
      ),
      signoff = SignoffSummary(
        falsePositiveCount = 0,
        falseNegativeCount = 0,
        positiveExemplarExpectedCount = 0,
        positiveExemplarEvaluatedCount = 0,
        crossSurfaceAgreementRate = 1.0,
        subtypeAgreementRate = 1.0,
        payoffTheaterAgreementRate = 1.0,
        pathVsPayoffDivergenceCount = 0,
        displaySubtypeSourceDistribution = Map("path" -> 1),
        mustFixFailureCount = 0,
        mustFixFailureCounts = Map.empty,
        negativeGuardPassCount = 0,
        negativeGuardFailCount = 0,
        negativeGuards = Nil,
        releaseGatePassed = true,
        mustFixFailures = Nil
      ),
      games = List(game)
    )

  test("buildAuditSet uses report games and keeps singled-out residual entries as separate audit ids") {
    val root = tempDir("audit-set-builder")
    val shardDir = root.resolve("master_classical_000")
    val rawDir = shardDir.resolve("raw")
    Files.createDirectories(rawDir)
    Files.writeString(rawDir.resolve("stale.game_arc.json"), "{}")

    val report = sampleRunReport(sampleGameReport("game_main", "master_classical", "sicilian", "Main Game"))
    writeJson(shardDir.resolve("report.json"), Json.toJson(report))

    val singleCorpus =
      Corpus(
        version = 1,
        generatedAt = "2026-03-22T00:00:00Z",
        asOfDate = "2026-03-22",
        title = "single",
        description = "single",
        games = List(
          CorpusGame(
            id = "game_main",
            tier = "titled_practical",
            family = "open_game",
            label = "Residual Game",
            notes = Nil,
            expectedThemes = Nil,
            pgn =
              """[Event "Single"]
                |[Site "?"]
                |[Date "2026.03.22"]
                |[Round "1"]
                |[White "A"]
                |[Black "B"]
                |[Result "1-0"]
                |
                |1. e4 e5 2. Nf3 Nc6 1-0
                |""".stripMargin
          )
        )
      )
    val singleCorpusPath = root.resolve("single_actorxu_77.json")
    writeJson(singleCorpusPath, Json.toJson(singleCorpus))

    val auditSet =
      CommentaryPlayerAuditSetBuilder.buildAuditSet(
        CommentaryPlayerAuditSetBuilder.Config(
          runDirs = List(shardDir),
          singleCorpora = List(singleCorpusPath),
          singleRunsRoot = root.resolve("single_runs"),
          outPath = root.resolve("audit_202_set.json")
        ),
        java.time.Instant.parse("2026-03-22T00:00:00Z")
      )

    assertEquals(auditSet.games.size, 2)
    assertEquals(auditSet.games.map(_.auditId).distinct.size, 2)
    assert(auditSet.games.exists(_.reportPath.endsWith("master_classical_000\\report.json")))
    assert(auditSet.games.exists(_.reportPath.endsWith("single_runs\\single_actorxu_77\\report.json")))
  }

  test("buildAuditQueue emits whole-game, chronicle focus, bookmaker, and active-note rows for one audit game") {
    val root = tempDir("audit-queue-builder")
    val runDir = root.resolve("edge_case_000")
    val rawDir = runDir.resolve("raw")
    Files.createDirectories(rawDir)

    val game = sampleGameReport("game_focus", "edge_case", "other", "Focus Game")
    val report = sampleRunReport(game)
    writeJson(runDir.resolve("report.json"), Json.toJson(report))

    val rawGame =
      Json.obj(
        "intro" -> "White built around d5 and Black never solved the outpost.",
        "conclusion" -> "The d5 square decided the game.",
        "themes" -> Json.arr("Pressure on d5"),
        "review" -> Json.obj(
          "blundersCount" -> 1,
          "missedWinsCount" -> 0,
          "selectedMomentPlies" -> Json.arr(24),
          "momentTypeCounts" -> Json.obj("AdvantageSwing" -> 1)
        ),
        "moments" -> Json.arr(
          Json.obj(
            "ply" -> 24,
            "side" -> "white",
            "fen" -> "8/8/8/3N4/8/8/8/8 w - - 0 1",
            "momentType" -> "AdvantageSwing",
            "moveClassification" -> "Blunder",
            "narrative" -> "Nd5 fixes the outpost and Black never gets rid of it.",
            "collapse" -> Json.obj("rootCause" -> "Tactical Miss"),
            "strategyPack" -> Json.obj(
              "plans" -> Json.arr(
                Json.obj("side" -> "white", "planName" -> "Improving piece placement"),
                Json.obj("side" -> "black", "planName" -> "Attacking a fixed pawn")
              )
            )
          )
        )
      )
    writeJson(rawDir.resolve("game_focus.game_arc.json"), rawGame)

    val rawBookmaker =
      Json.obj(
        "commentary" -> "12... Nd5: The move fixes the knight on d5 and prepares c4 next.",
        "mainStrategicPlans" -> Json.arr(
          Json.obj("planId" -> "plan_1", "subplanId" -> play.api.libs.json.JsNull, "planName" -> "Preparing c4")
        ),
        "strategicPlanExperiments" -> Json.arr(
          Json.obj("planId" -> "plan_1", "subplanId" -> play.api.libs.json.JsNull, "evidenceTier" -> "pv_coupled")
        ),
        "whyAbsentFromTopMultiPV" -> Json.arr("This route stays secondary until c4 lands."),
        "signalDigest" -> Json.obj(
          "opening" -> "English Opening",
          "opponentPlan" -> "Challenge d5 with c6",
          "structuralCue" -> "d5 is fixed",
          "deploymentPiece" -> "rook",
          "deploymentRoute" -> Json.arr("c1", "c4"),
          "deploymentPurpose" -> "support the outpost",
          "practicalVerdict" -> "The position stays easier to handle.",
          "compensation" -> "The open c-file gives enough pressure.",
          "preservedSignals" -> Json.arr("Pressure on d5")
        )
      )
    writeJson(rawDir.resolve("game_focus.ply_24.bookmaker.json"), rawBookmaker)

    val auditSet =
      AuditSetManifest(
        generatedAt = "2026-03-22T00:00:00Z",
        title = "audit",
        description = "audit",
        games = List(
          AuditSetEntry(
            auditId = "edge_case_000:game_focus",
            gameId = "game_focus",
            tier = "edge_case",
            openingFamily = "other",
            label = "Focus Game",
            reportPath = runDir.resolve("report.json").toString,
            rawDir = rawDir.toString,
            sourceTag = "edge_case_000"
          )
        )
      )
    val auditSetPath = root.resolve("audit_set.json")
    writeJson(auditSetPath, Json.toJson(auditSet))

    val (queue, summary) =
      CommentaryPlayerReviewQueueBuilder.buildAuditQueue(
        CommentaryPlayerReviewQueueBuilder.Config(
          outPath = root.resolve("review_queue.jsonl"),
          summaryPath = root.resolve("review_queue_summary.json"),
          auditSetPath = Some(auditSetPath),
          fullReview = true
        ),
        auditSetPath
      )

    assertEquals(queue.size, 4)
    assertEquals(summary.wholeGameReviewCount, 1)
    assert(queue.forall(_.auditId.contains("edge_case_000:game_focus")))
    assert(queue.exists(entry => entry.surface == ReviewSurface.Chronicle && entry.reviewKind == ReviewKind.WholeGame))
    assert(queue.exists(entry => entry.surface == ReviewSurface.Bookmaker && entry.supportRows.exists(_.startsWith("Main plans:"))))
    assert(queue.exists(entry => entry.surface == ReviewSurface.ActiveNote && entry.pairedSampleId.nonEmpty))
    val wholeGame = queue.find(entry => entry.surface == ReviewSurface.Chronicle && entry.reviewKind == ReviewKind.WholeGame).getOrElse(fail("missing whole-game row"))
    assert(wholeGame.mainProse.contains("White was mainly playing for"), clue(wholeGame.mainProse))
    assert(wholeGame.mainProse.contains("The decisive shift came through"), clue(wholeGame.mainProse))
    assert(wholeGame.supportRows.exists(_.contains("Punishment: Blunder @24 was punished")), clue(wholeGame.supportRows))
  }

  test("review merge uses queue metadata to emit richer family summaries") {
    val root = tempDir("review-merge")
    val reviewDir = root.resolve("reviews")
    val reportDir = root.resolve("reports")
    Files.createDirectories(reviewDir)
    Files.createDirectories(reportDir)

    val queuePath = reviewDir.resolve("review_queue.jsonl")
    writeJsonLines(
      queuePath,
      List(
        ReviewQueueEntry(
          sampleId = "audit:1",
          gameId = "game_a",
          surface = ReviewSurface.Bookmaker,
          reviewKind = ReviewKind.BookmakerFocus,
          sliceKind = WholeGameSliceKind.BookmakerFocus,
          tier = Some("master_classical"),
          openingFamily = Some("sicilian"),
          label = Some("Game A"),
          fen = "",
          playedSan = "",
          mainProse = "The move keeps pressure on d5.",
          supportRows = Nil,
          advancedRows = Nil,
          flags = Nil
        )
      )
    )
    writeJsonLines(
      reviewDir.resolve("judgments-a.jsonl"),
      List(
        JudgmentEntry(
          sampleId = "audit:1",
          reviewer = "qa",
          severity = ReviewSeverity.Blocker,
          rubric = List("player-facing"),
          blockerType = Some(BlockerType.OverloadedMainClaim),
          notes = "Body stayed generic while support had the real idea.",
          fixFamily = List(FixFamily.AnchoredSupportMissingFromProse)
        )
      )
    )

    CommentaryPlayerReviewMerge.main(
      Array(
        reviewDir.toString,
        queuePath.toString,
        reportDir.resolve("blockers.md").toString,
        reportDir.resolve("phrase-family-clusters.md").toString,
        reportDir.resolve("fix-priority.json").toString,
        reportDir.resolve("audit-report.md").toString
      )
    )

    val fixPriority = Json.parse(Files.readString(reportDir.resolve("fix-priority.json")))
    val auditReport = Files.readString(reportDir.resolve("audit-report.md"))

    assertEquals((fixPriority \ "familyCounts" \ FixFamily.AnchoredSupportMissingFromProse).as[Int], 1)
    assertEquals((fixPriority \ "surfaceFamilyCounts" \ ReviewSurface.Bookmaker \ FixFamily.AnchoredSupportMissingFromProse).as[Int], 1)
    assert(auditReport.contains(FixFamily.AnchoredSupportMissingFromProse), clue(auditReport))
    assert(auditReport.contains("Bookmaker"), clue(auditReport))
  }
