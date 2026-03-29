package lila.llm.tools

import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

import scala.collection.mutable

import lila.llm.analysis.{ LiveNarrativeCompressionCore, UserFacingSignalSanitizer }

object CommentaryPlayerReviewQueueBuilder:

  import CommentaryPlayerQcSupport.*
  import RealPgnNarrativeEvalRunner.*

  final case class Config(
      manifestPath: Path = DefaultManifestDir.resolve("slice_manifest.jsonl"),
      bookmakerOutputsPath: Path = DefaultBookmakerRunDir.resolve("bookmaker_outputs.jsonl"),
      chronicleReportPath: Path = DefaultChronicleRunDir.resolve("report.json"),
      outPath: Path = DefaultReviewDir.resolve("review_queue.jsonl"),
      summaryPath: Path = DefaultReportDir.resolve("review_queue_summary.json"),
      auditSetPath: Option[Path] = None,
      fullReview: Boolean = false
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val (queue, summary) =
      config.auditSetPath match
        case Some(path) => buildAuditQueue(config, path)
        case None       => buildLegacyQueue(config)

    writeJsonLines(config.outPath, queue)
    writeJson(config.summaryPath, Json.toJson(summary))
    val mandatory = queue.count(entry => config.fullReview || isMandatoryReview(entry.sliceKind, entry.flags))
    println(s"[player-qc-queue] wrote `${config.outPath}` (reviewed=${queue.size}, mandatory=$mandatory)")

  private[tools] def buildAuditQueue(config: Config, auditSetPath: Path): (List[ReviewQueueEntry], ChronicleQueueReport) =
    val auditSet =
      readAuditSet(auditSetPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[player-qc-queue] failed to read audit set `${auditSetPath}`: $err")
          sys.exit(1)

    val reportCache = mutable.Map.empty[Path, RunReport]
    val queue =
      auditSet.games.flatMap { entry =>
        val reportPath = Paths.get(entry.reportPath)
        val runReport = reportCache.getOrElseUpdate(reportPath, readRunReportOrExit(reportPath))
        val game =
          runReport.games.find(_.id == entry.gameId).getOrElse {
            System.err.println(
              s"[player-qc-queue] audit set game `${entry.gameId}` missing from report `${entry.reportPath}`"
            )
            sys.exit(1)
          }
        val rawDir = Paths.get(entry.rawDir)
        val wholeGame = buildWholeGameEntry(entry, game, rawDir)
        val focusRows = game.focusMoments.flatMap(moment => buildAuditMomentEntries(entry, moment, rawDir, config.fullReview))
        wholeGame.toList ++ focusRows
      }

    val chronicleMomentCount =
      queue.count(row => row.surface == ReviewSurface.Chronicle && row.reviewKind == ReviewKind.FocusMoment)
    val wholeGameCount =
      queue.count(row => row.surface == ReviewSurface.Chronicle && row.reviewKind == ReviewKind.WholeGame)
    val bookmakerCount = queue.count(_.surface == ReviewSurface.Bookmaker)
    val mandatory = queue.count(entry => config.fullReview || isMandatoryReview(entry.sliceKind, entry.flags))
    val summary =
      ChronicleQueueReport(
        version = 1,
        generatedAt = java.time.Instant.now().toString,
        bookmakerOutputCount = bookmakerCount,
        chronicleMomentCount = chronicleMomentCount,
        wholeGameReviewCount = wholeGameCount,
        mandatoryReviewCount = mandatory,
        sampledReviewCount = queue.size - mandatory,
        reviewedCount = queue.size,
        fullReview = config.fullReview,
        auditSetGameCount = auditSet.games.size
      )

    (queue, summary)

  private def buildLegacyQueue(config: Config): (List[ReviewQueueEntry], ChronicleQueueReport) =
    val manifest =
      readJsonLines[SliceManifestEntry](config.manifestPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[player-qc-queue] failed to read manifest `${config.manifestPath}`: $err")
          sys.exit(1)
    val bookmaker =
      readJsonLines[BookmakerOutputEntry](config.bookmakerOutputsPath) match
        case Right(value) => value.map(output => output.sampleId -> output).toMap
        case Left(err) =>
          System.err.println(s"[player-qc-queue] failed to read bookmaker outputs `${config.bookmakerOutputsPath}`: $err")
          sys.exit(1)

    val chronicleReport = readRunReportOrExit(config.chronicleReportPath)
    val chronicleByKey =
      chronicleReport.games.map(game => game.id -> game.focusMoments.map(moment => moment.ply -> moment).toMap).toMap

    val queue =
      manifest.flatMap { entry =>
        entry.surface match
          case ReviewSurface.Bookmaker =>
            bookmaker.get(entry.sampleId).flatMap { output =>
              val flags = reviewFlags(output.commentary, output.supportRows, output.advancedRows, output.sliceKind)
              val include = config.fullReview || isMandatoryReview(output.sliceKind, flags) || sampleByHash(output.sampleId)
              Option.when(include) {
                ReviewQueueEntry(
                  sampleId = output.sampleId,
                  gameId = output.gameKey,
                  surface = ReviewSurface.Bookmaker,
                  reviewKind = ReviewKind.BookmakerFocus,
                  sliceKind = output.sliceKind,
                  fen = output.fen,
                  playedSan = output.playedSan,
                  mainProse = output.commentary,
                  supportRows = flattenRows(output.supportRows),
                  advancedRows = flattenRows(output.advancedRows),
                  flags = flags
                )
              }
            }

          case ReviewSurface.Chronicle =>
            chronicleByKey
              .get(entry.gameKey)
              .flatMap(_.get(entry.targetPly))
              .flatMap { moment =>
                val support = chronicleSupportRows(moment)
                val advanced = chronicleAdvancedRows(moment)
                val flags = reviewFlags(moment.gameArcNarrative, support, advanced, entry.sliceKind)
                val include = config.fullReview || isMandatoryReview(entry.sliceKind, flags) || sampleByHash(entry.sampleId)
                Option.when(include) {
                  ReviewQueueEntry(
                    sampleId = entry.sampleId,
                    gameId = entry.gameKey,
                    surface = ReviewSurface.Chronicle,
                    reviewKind = ReviewKind.FocusMoment,
                    sliceKind = entry.sliceKind,
                    fen = entry.fen,
                    playedSan = entry.playedSan,
                    mainProse = moment.gameArcNarrative,
                    supportRows = flattenRows(support),
                    advancedRows = flattenRows(advanced),
                    flags = flags
                  )
                }
              }
          case _ => None
      }

    val mandatory = queue.count(entry => config.fullReview || isMandatoryReview(entry.sliceKind, entry.flags))
    val summary =
      ChronicleQueueReport(
        version = 1,
        generatedAt = java.time.Instant.now().toString,
        bookmakerOutputCount = bookmaker.size,
        chronicleMomentCount = chronicleByKey.values.map(_.size).sum,
        wholeGameReviewCount = 0,
        mandatoryReviewCount = mandatory,
        sampledReviewCount = queue.size - mandatory,
        reviewedCount = queue.size,
        fullReview = config.fullReview,
        auditSetGameCount = 0
      )

    (queue, summary)

  private def buildAuditMomentEntries(
      entry: AuditSetEntry,
      moment: FocusMomentReport,
      rawDir: Path,
      fullReview: Boolean
  ): List[ReviewQueueEntry] =
    val chronicleSampleId = s"${entry.auditId}:${moment.ply}:chronicle"
    val chronicleSupport = chronicleSupportRows(moment)
    val chronicleAdvanced = chronicleAdvancedRows(moment)
    val chronicleFlags =
      reviewFlags(moment.gameArcNarrative, chronicleSupport, chronicleAdvanced, WholeGameSliceKind.ChronicleFocus)
    val chronicleEntry =
      Option.when(fullReview || isMandatoryReview(WholeGameSliceKind.ChronicleFocus, chronicleFlags) || sampleByHash(chronicleSampleId)) {
        ReviewQueueEntry(
          sampleId = chronicleSampleId,
          auditId = Some(entry.auditId),
          gameId = entry.gameId,
          surface = ReviewSurface.Chronicle,
          reviewKind = ReviewKind.FocusMoment,
          sliceKind = WholeGameSliceKind.ChronicleFocus,
          tier = Some(entry.tier),
          openingFamily = Some(entry.openingFamily),
          label = Some(entry.label),
          fen = momentFen(rawDir, entry.gameId, moment.ply).getOrElse(""),
          playedSan = "",
          mainProse = moment.gameArcNarrative,
          supportRows = flattenRows(chronicleSupport),
          advancedRows = flattenRows(chronicleAdvanced),
          flags = chronicleFlags
        )
      }

    val bookmakerRowPayload = bookmakerPayload(entry.gameId, moment, rawDir)
    val bookmakerSampleId = s"${entry.auditId}:${moment.ply}:bookmaker"
    val bookmakerFlags =
      reviewFlags(
        bookmakerRowPayload.commentary,
        bookmakerRowPayload.supportRows,
        bookmakerRowPayload.advancedRows,
        WholeGameSliceKind.BookmakerFocus
      )
    val bookmakerEntry =
      Option.when(fullReview || isMandatoryReview(WholeGameSliceKind.BookmakerFocus, bookmakerFlags) || sampleByHash(bookmakerSampleId)) {
        ReviewQueueEntry(
          sampleId = bookmakerSampleId,
          auditId = Some(entry.auditId),
          gameId = entry.gameId,
          surface = ReviewSurface.Bookmaker,
          reviewKind = ReviewKind.BookmakerFocus,
          sliceKind = WholeGameSliceKind.BookmakerFocus,
          tier = Some(entry.tier),
          openingFamily = Some(entry.openingFamily),
          label = Some(entry.label),
          pairedSampleId = Some(chronicleSampleId),
          fen = momentFen(rawDir, entry.gameId, moment.ply).getOrElse(""),
          playedSan = "",
          mainProse = bookmakerRowPayload.commentary,
          supportRows = flattenRows(bookmakerRowPayload.supportRows),
          advancedRows = flattenRows(bookmakerRowPayload.advancedRows),
          flags = bookmakerFlags
        )
      }

    val activeSampleId = s"${entry.auditId}:${moment.ply}:active"
    val activeRows = activeReviewRows(moment, bookmakerRowPayload.commentary)
    val activeFlags = activeReviewFlags(moment, activeRows._1, activeRows._2)
    val activeEntry =
      Option.when(fullReview || activeFlags.nonEmpty || sampleByHash(activeSampleId)) {
        ReviewQueueEntry(
          sampleId = activeSampleId,
          auditId = Some(entry.auditId),
          gameId = entry.gameId,
          surface = ReviewSurface.ActiveNote,
          reviewKind = ReviewKind.ActiveParity,
          sliceKind = WholeGameSliceKind.ActiveParity,
          tier = Some(entry.tier),
          openingFamily = Some(entry.openingFamily),
          label = Some(entry.label),
          pairedSampleId = Some(chronicleSampleId),
          fen = momentFen(rawDir, entry.gameId, moment.ply).getOrElse(""),
          playedSan = "",
          mainProse = moment.activeNote.getOrElse(""),
          supportRows = flattenRows(activeRows._1),
          advancedRows = flattenRows(activeRows._2),
          flags = activeFlags
        )
      }

    List(chronicleEntry, bookmakerEntry, activeEntry).flatten

  private def buildWholeGameEntry(entry: AuditSetEntry, game: GameReport, rawDir: Path): Option[ReviewQueueEntry] =
    val rawPath = rawDir.resolve(s"${entry.gameId}.game_arc.json")
    val rawJs =
      if Files.exists(rawPath) then Json.parse(Files.readString(rawPath))
      else Json.obj()

    val intro = (rawJs \ "intro").asOpt[String].getOrElse(s"${entry.label} review.")
    val conclusion = (rawJs \ "conclusion").asOpt[String].getOrElse("")
    val moments = (rawJs \ "moments").asOpt[List[JsObject]].getOrElse(Nil)
    val review = (rawJs \ "review").asOpt[JsObject]
    val whitePlans = planNamesForSide(moments, "white")
    val blackPlans = planNamesForSide(moments, "black")
    val blunderCount = review.flatMap(obj => (obj \ "blundersCount").asOpt[Int]).getOrElse(moments.count(m => (m \ "moveClassification").asOpt[String].contains("Blunder")))
    val missedWinCount = review.flatMap(obj => (obj \ "missedWinsCount").asOpt[Int]).getOrElse(moments.count(m => (m \ "moveClassification").asOpt[String].contains("MissedWin")))
    val turningPoints = turningPointSummary(moments)
    val rootCauses = collapseRootCauseSummary(moments)
    val wholeGameStory =
      wholeGameStorySummary(
        whitePlans = whitePlans,
        blackPlans = blackPlans,
        turningPoints = turningPoints,
        punishment = renderPunishmentSummary(moments, blunderCount, missedWinCount, rootCauses),
        result = List(entry.result, game.result).flatten.headOption.getOrElse("unknown"),
        nonStrategicShortGame = isNonStrategicShortGame(moments)
      )
    val support =
      List(
        Some(SupportRow("Themes", renderList(game.overallThemes))),
        Option.when(whitePlans.nonEmpty)(SupportRow("White plan", whitePlans.mkString(", "))),
        Option.when(blackPlans.nonEmpty)(SupportRow("Black plan", blackPlans.mkString(", "))),
        Some(SupportRow("Turning points", turningPoints)),
        Some(SupportRow("Punishment", renderPunishmentSummary(moments, blunderCount, missedWinCount, rootCauses))),
        Some(SupportRow("Result", List(entry.result, game.result).flatten.headOption.getOrElse("unknown")))
      ).flatten.filter(row => row.text.trim.nonEmpty)
    val advanced =
      List(
        Option.when(conclusion.trim.nonEmpty)(SupportRow("Conclusion", conclusion.trim)),
        Option.when(game.visibleMomentPlies.nonEmpty)(SupportRow("Moment plies", game.visibleMomentPlies.mkString(", "))),
        Option.when(rootCauses.nonEmpty)(SupportRow("Root causes", rootCauses.mkString(", "))),
        Option.when(game.probeExecutedRequests > 0)(SupportRow("Probe refinement", s"executed ${game.probeExecutedRequests} probe requests"))
      ).flatten
    val mainProse = List(intro.trim, wholeGameStory.getOrElse("").trim, conclusion.trim).filter(_.nonEmpty).mkString("\n\n")
    val flags = wholeGameReviewFlags(mainProse, moments, whitePlans, blackPlans)

    Some(
      ReviewQueueEntry(
        sampleId = s"${entry.auditId}:whole_game",
        auditId = Some(entry.auditId),
        gameId = entry.gameId,
        surface = ReviewSurface.Chronicle,
        reviewKind = ReviewKind.WholeGame,
        sliceKind = WholeGameSliceKind.ChronicleWholeGame,
        tier = Some(entry.tier),
        openingFamily = Some(entry.openingFamily),
        label = Some(entry.label),
        fen = "",
        playedSan = "",
        mainProse = mainProse,
        supportRows = flattenRows(support),
        advancedRows = flattenRows(advanced),
        flags = flags
      )
    )

  private def chronicleSupportRows(moment: FocusMomentReport): List[SupportRow] =
    List(
      moment.objective.map(value => SupportRow("Objective", value)),
      moment.focus.map(value => SupportRow("Focus", value)),
      moment.execution.map(value => SupportRow("Execution", value)),
      moment.dominantIdea.map(value => SupportRow("Dominant idea", value)),
      moment.secondaryIdea.map(value => SupportRow("Secondary idea", value))
    ).flatten.filter(row => LiveNarrativeCompressionCore.keepPlayerFacingSentence(row.text))

  private def chronicleAdvancedRows(moment: FocusMomentReport): List[SupportRow] =
    moment.activeNote
      .filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence)
      .map(value => List(SupportRow("Active note", value)))
      .getOrElse(Nil)

  private final case class BookmakerPayload(
      commentary: String,
      supportRows: List[SupportRow],
      advancedRows: List[SupportRow]
  )

  private def bookmakerPayload(gameId: String, moment: FocusMomentReport, rawDir: Path): BookmakerPayload =
    val rawBookmakerPath = rawDir.resolve(s"${gameId}.ply_${moment.ply}.bookmaker.json")
    if !Files.exists(rawBookmakerPath) then
      BookmakerPayload(
        commentary = moment.bookmakerCommentary,
        supportRows = chronicleSupportRows(moment),
        advancedRows = Nil
      )
    else
      val js = Json.parse(Files.readString(rawBookmakerPath))
      val commentary = (js \ "commentary").asOpt[String].getOrElse(moment.bookmakerCommentary)
      val (support, advanced) = bookmakerRowsFromJson(js)
      BookmakerPayload(commentary = commentary, supportRows = support, advancedRows = advanced)

  private def bookmakerRowsFromJson(js: JsValue): (List[SupportRow], List[SupportRow]) =
    val support = mutable.ListBuffer.empty[SupportRow]
    val advanced = mutable.ListBuffer.empty[SupportRow]
    val signalDigest = js \ "signalDigest"
    val compensationContext =
      (signalDigest \ "compensation").asOpt[String].exists(_.trim.nonEmpty) ||
        (signalDigest \ "investedMaterial").asOpt[Int].exists(_ > 0) ||
        (signalDigest \ "compensationVectors").asOpt[List[String]].exists(_.exists(_.trim.nonEmpty))

    def sanitizeRowText(raw: String): Option[String] =
      Option(raw)
        .map(UserFacingSignalSanitizer.sanitize)
        .map(_.trim)
        .filter(text => !compensationContext || UserFacingSignalSanitizer.allowCompensationSupportText(text))
        .filter(_.nonEmpty)

    val experimentsByPlan =
      (js \ "strategicPlanExperiments")
        .asOpt[List[JsObject]]
        .getOrElse(Nil)
        .map { exp =>
          val key = ((exp \ "planId").asOpt[String].getOrElse(""), (exp \ "subplanId").asOpt[String].getOrElse(""))
          key -> (exp \ "evidenceTier").asOpt[String].map(_.replace('_', ' '))
        }
        .toMap

    val planRows =
      (js \ "mainStrategicPlans")
        .asOpt[List[JsObject]]
        .getOrElse(Nil)
        .flatMap { plan =>
          val key = ((plan \ "planId").asOpt[String].getOrElse(""), (plan \ "subplanId").asOpt[String].getOrElse(""))
          val label =
            experimentsByPlan.get(key).flatten match
              case Some(tier) => s"${(plan \ "planName").asOpt[String].getOrElse("")} [$tier]"
              case None       => (plan \ "planName").asOpt[String].getOrElse("")
          sanitizeRowText(label)
        }
    if planRows.nonEmpty then support += SupportRow("Main plans", planRows.mkString(", "))

    (signalDigest \ "opening").asOpt[String].flatMap(sanitizeRowText).filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence).foreach { opening =>
      support += SupportRow("Opening", opening)
    }
    (signalDigest \ "opponentPlan").asOpt[String].flatMap(sanitizeRowText).filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence).foreach { opponent =>
      support += SupportRow("Opponent", opponent)
    }

    val structure =
      List(
        (signalDigest \ "structureProfile").asOpt[String],
        (signalDigest \ "structuralCue").asOpt[String],
        (signalDigest \ "centerState").asOpt[String]
      ).flatten.flatMap(sanitizeRowText).distinct.mkString("; ")
    if structure.nonEmpty && LiveNarrativeCompressionCore.keepPlayerFacingSentence(structure) then
      support += SupportRow("Structure", structure)

    val deployment =
      List(
        (signalDigest \ "deploymentPiece").asOpt[String],
        (signalDigest \ "deploymentRoute").asOpt[List[String]].filter(_.nonEmpty).map(_.mkString(" -> ")),
        (signalDigest \ "deploymentPurpose").asOpt[String]
      ).flatten.flatMap(sanitizeRowText).mkString(" ")
    if deployment.nonEmpty && LiveNarrativeCompressionCore.hasConcreteAnchor(deployment) then
      support += SupportRow("Piece deployment", deployment)

    val practical =
      List(
        (signalDigest \ "practicalVerdict").asOpt[String].flatMap(sanitizeRowText),
        (signalDigest \ "practicalFactors").asOpt[List[String]].flatMap(_.headOption).flatMap(f => LiveNarrativeCompressionCore.renderPracticalBiasPlayer(f, f).toList.headOption).flatMap(sanitizeRowText)
      ).flatten.mkString("; ")
    if practical.nonEmpty && LiveNarrativeCompressionCore.keepPlayerFacingSentence(practical) then
      support += SupportRow("Practical", practical)

    List(
      (signalDigest \ "compensation").asOpt[String].flatMap(sanitizeRowText).map(text => SupportRow("Compensation", text)),
      (signalDigest \ "authoringEvidence").asOpt[String].flatMap(sanitizeRowText).map(text => SupportRow("Evidence note", text)),
      (signalDigest \ "preservedSignals").asOpt[List[String]].filter(_.nonEmpty).map(values =>
        SupportRow("Preserved signals", values.flatMap(sanitizeRowText).mkString("; "))
      )
    ).flatten.foreach { row =>
      if row.text.nonEmpty && LiveNarrativeCompressionCore.keepPlayerFacingSentence(row.text) then advanced += row
    }

    (support.toList.distinct, advanced.toList.distinct)

  private def activeReviewRows(moment: FocusMomentReport, bookmakerCommentary: String): (List[SupportRow], List[SupportRow]) =
    val support =
      List(
        Some(SupportRow("Status", moment.activeNoteStatus)),
        moment.objective.map(value => SupportRow("Objective", value)),
        moment.focus.map(value => SupportRow("Focus", value)),
        moment.execution.map(value => SupportRow("Execution", value)),
        moment.dominantIdea.map(value => SupportRow("Chronicle idea", value)),
        Some(SupportRow("Chronicle narrative", moment.gameArcNarrative)),
        Option.when(bookmakerCommentary.trim.nonEmpty)(SupportRow("Bookmaker", bookmakerCommentary))
      ).flatten
    val advanced =
      List(
        moment.secondaryIdea.map(value => SupportRow("Secondary idea", value))
      ).flatten
    (support, advanced)

  private def activeReviewFlags(
      moment: FocusMomentReport,
      supportRows: List[SupportRow],
      advancedRows: List[SupportRow]
  ): List[String] =
    val note = moment.activeNote.getOrElse("").trim
    val flags = mutable.ListBuffer.empty[String]
    if note.isEmpty || moment.activeNoteStatus == "omitted" then flags += FixFamily.ActiveNoteMissingContract
    if note.nonEmpty && !LiveNarrativeCompressionCore.hasConcreteAnchor(note) then flags += FixFamily.AnchorlessActiveContinuation
    if note.nonEmpty && sentenceCount(note) <= 1 && note.toLowerCase.contains("compensation") then flags += FixFamily.DryContractNote
    val expectedTokens =
      List(moment.dominantIdea, moment.objective, moment.focus, moment.execution).flatten.flatMap(tokenizeMeaningful).distinct
    if note.nonEmpty && expectedTokens.nonEmpty && expectedTokens.forall(token => !note.toLowerCase.contains(token)) then
      flags += FixFamily.ChronicleActiveStoryDrift
    val metaFlags = reviewFlags(note, supportRows, advancedRows, WholeGameSliceKind.ActiveParity)
    (flags.toList ++ metaFlags).distinct

  private def wholeGameReviewFlags(
      mainProse: String,
      moments: List[JsObject],
      whitePlans: List[String],
      blackPlans: List[String]
  ): List[String] =
    val flags = mutable.ListBuffer.empty[String]
    val tensionPeakCount = moments.count(m => (m \ "momentType").asOpt[String].contains("TensionPeak"))
    val turningMoments =
      moments.filter(m =>
        Set("AdvantageSwing", "StrategicBridge", "MatePivot", "Equalization").contains((m \ "momentType").asOpt[String].getOrElse(""))
      )
    val blunders = moments.filter(m => (m \ "moveClassification").asOpt[String].contains("Blunder"))
    val missedWins = moments.filter(m => (m \ "moveClassification").asOpt[String].contains("MissedWin"))
    if !isNonStrategicShortGame(moments) && (whitePlans.isEmpty || blackPlans.isEmpty) then
      flags += FixFamily.SideAsymmetryOrMissingSidePlan
    if tensionPeakCount >= 6 && turningMoments.size <= 1 then flags += FixFamily.GenericTensionPeakOverload
    if turningMoments.nonEmpty && turningMoments.forall(m => !(m \ "narrative").asOpt[String].exists(LiveNarrativeCompressionCore.hasConcreteAnchor)) then
      flags += FixFamily.TurningPointUnderexplained
    if blunders.nonEmpty && renderPunishmentSummary(moments, blunders.size, missedWins.size, collapseRootCauseSummary(moments)).toLowerCase.contains("blunders=") then
      flags += FixFamily.BlunderWithoutPunishFeedback
    if missedWins.nonEmpty && renderPunishmentSummary(moments, blunders.size, missedWins.size, collapseRootCauseSummary(moments)).toLowerCase.contains("missedwins=") then
      flags += FixFamily.MissedPunishUnderexplained
    val longStory = mainProse.trim
    if whitePlans.nonEmpty && blackPlans.nonEmpty &&
      (!mentionsAnyPlan(longStory, whitePlans) || !mentionsAnyPlan(longStory, blackPlans))
    then flags += FixFamily.WholeGamePlanDrift
    if longStory.nonEmpty &&
      List("plan", "pressure", "compensation", "initiative", "counterplay").exists(longStory.toLowerCase.contains) &&
      !LiveNarrativeCompressionCore.hasConcreteAnchor(longStory)
    then flags += FixFamily.ConcreteAnchorMissingInLongTermStory
    flags.toList.distinct

  private def planNamesForSide(moments: List[JsObject], side: String): List[String] =
    val planNames =
      moments
        .flatMap(moment => (moment \ "strategyPack" \ "plans").asOpt[List[JsObject]].getOrElse(Nil))
        .filter(plan => (plan \ "side").asOpt[String].exists(_.equalsIgnoreCase(side)))
        .flatMap(plan => (plan \ "planName").asOpt[String])
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .take(5)
    if planNames.nonEmpty then planNames
    else
      moments
        .filter(moment => (moment \ "side").asOpt[String].exists(_.equalsIgnoreCase(side)))
        .flatMap(moment =>
          List(
            (moment \ "objective").asOpt[String],
            (moment \ "focus").asOpt[String],
            (moment \ "execution").asOpt[String],
            (moment \ "dominantIdea").asOpt[String],
            (moment \ "strategyPack" \ "longTermFocus").asOpt[List[String]].flatMap(_.headOption)
          ).flatten
        )
        .map(_.trim)
        .filter(text => text.nonEmpty && LiveNarrativeCompressionCore.keepPlayerFacingSentence(text))
        .distinct
        .take(3)

  private def turningPointSummary(moments: List[JsObject]): String =
    val summary =
      moments
        .flatMap { moment =>
          val momentType = (moment \ "momentType").asOpt[String]
          val ply = (moment \ "ply").asOpt[Int]
          Option.when(momentType.exists(_ != "TensionPeak")) {
            val anchor = turningPointAnchor(moment)
            anchor match
              case Some(text) => s"${momentType.getOrElse("Moment")} @${ply.getOrElse(0)}: $text"
              case None       => s"${momentType.getOrElse("Moment")} @${ply.getOrElse(0)}"
          }
        }
        .distinct
        .take(6)
    if summary.nonEmpty then summary.mkString(", ") else "No non-trivial turning point summary"

  private def collapseRootCauseSummary(moments: List[JsObject]): List[String] =
    moments
      .flatMap(moment => (moment \ "collapse" \ "rootCause").asOpt[String])
      .map(_.trim)
      .filter(_.nonEmpty)
      .groupBy(identity)
      .toList
      .sortBy { case (_, entries) => -entries.size }
      .map { case (cause, entries) => s"$cause x${entries.size}" }

  private def renderPunishmentSummary(
      moments: List[JsObject],
      blunders: Int,
      missedWins: Int,
      rootCauses: List[String]
  ): String =
    val blunderSummary =
      moments
        .filter(m => (m \ "moveClassification").asOpt[String].contains("Blunder"))
        .take(2)
        .flatMap { moment =>
          val ply = (moment \ "ply").asOpt[Int].getOrElse(0)
          punishmentAnchor(moment).map(anchor => s"Blunder @$ply was punished through $anchor")
        }
    val missedWinSummary =
      moments
        .filter(m => (m \ "moveClassification").asOpt[String].contains("MissedWin"))
        .take(2)
        .flatMap { moment =>
          val ply = (moment \ "ply").asOpt[Int].getOrElse(0)
          punishmentAnchor(moment).map(anchor => s"Missed win @$ply left $anchor unfinished")
        }
    val summaries = (blunderSummary ++ missedWinSummary).distinct
    if summaries.nonEmpty then summaries.mkString("; ")
    else
      List(
        Some(s"blunders=$blunders"),
        Some(s"missedWins=$missedWins"),
        Option.when(rootCauses.nonEmpty)(rootCauses.mkString("; "))
      ).flatten.mkString(", ")

  private def wholeGameStorySummary(
      whitePlans: List[String],
      blackPlans: List[String],
      turningPoints: String,
      punishment: String,
      result: String,
      nonStrategicShortGame: Boolean
  ): Option[String] =
    if nonStrategicShortGame then
      Some("This was too short for a stable two-sided strategic battle.")
    else
      val sidePlanSentence =
        (whitePlans.headOption, blackPlans.headOption) match
          case (Some(whitePlan), Some(blackPlan)) =>
            Some(s"White was mainly playing for $whitePlan, while Black was mainly playing for $blackPlan.")
          case (Some(whitePlan), None) =>
            Some(s"White's clearest plan was $whitePlan.")
          case (None, Some(blackPlan)) =>
            Some(s"Black's clearest plan was $blackPlan.")
          case _ =>
            None
      val turningSentence =
        Option.when(turningPoints != "No non-trivial turning point summary") {
          s"The decisive shift came through $turningPoints."
        }
      val punishmentSentence =
        Option.when(!punishment.toLowerCase.startsWith("blunders=") && !punishment.toLowerCase.startsWith("missedwins=")) {
          s"The punishment story was $punishment."
        }
      val resultSentence =
        Option.when(result.trim.nonEmpty) {
          s"The final verdict was $result."
        }
      List(sidePlanSentence, turningSentence, punishmentSentence, resultSentence).flatten match
        case Nil       => None
        case sentences => Some(sentences.mkString(" "))

  private def turningPointAnchor(moment: JsObject): Option[String] =
    val direct =
      List(
        (moment \ "objective").asOpt[String],
        (moment \ "focus").asOpt[String],
        (moment \ "execution").asOpt[String],
        (moment \ "dominantIdea").asOpt[String],
        (moment \ "strategyPack" \ "longTermFocus").asOpt[List[String]].flatMap(_.headOption)
      ).flatten
        .map(_.trim)
        .filter(text => text.nonEmpty && LiveNarrativeCompressionCore.keepPlayerFacingSentence(text))
        .find(text => LiveNarrativeCompressionCore.hasConcreteAnchor(text) || looksStrategicAnchor(text))
    direct.orElse {
      (moment \ "narrative").asOpt[String]
        .flatMap(_.split("(?<=[.!?])\\s+").map(_.trim).find(_.nonEmpty))
        .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
        .filter(text => text.nonEmpty && !LiveNarrativeCompressionCore.isLowValueNarrativeSentence(text))
        .filter(text => LiveNarrativeCompressionCore.hasConcreteAnchor(text) || looksStrategicAnchor(text))
    }

  private def punishmentAnchor(moment: JsObject): Option[String] =
    turningPointAnchor(moment)
      .orElse((moment \ "collapse" \ "rootCause").asOpt[String].map(_.trim).filter(_.nonEmpty))

  private def mentionsAnyPlan(text: String, plans: List[String]): Boolean =
    val low = Option(text).getOrElse("").toLowerCase
    plans.exists(plan => plan.trim.nonEmpty && low.contains(plan.trim.toLowerCase))

  private def looksStrategicAnchor(text: String): Boolean =
    val low = Option(text).getOrElse("").toLowerCase
    List("pressure", "target", "break", "counterplay", "exchange", "file", "pawn", "king", "outpost").exists(low.contains)

  private def isNonStrategicShortGame(moments: List[JsObject]): Boolean =
    if moments.isEmpty then true
    else
      val maxPly = moments.flatMap(moment => (moment \ "ply").asOpt[Int]).maxOption.getOrElse(0)
      val allTensionPeaks = moments.forall(moment => (moment \ "momentType").asOpt[String].contains("TensionPeak"))
      val hasStrategicAnchor =
        moments.exists(moment =>
          List(
            (moment \ "objective").asOpt[String],
            (moment \ "focus").asOpt[String],
            (moment \ "execution").asOpt[String],
            (moment \ "dominantIdea").asOpt[String]
          ).flatten.exists(text => text.trim.nonEmpty && looksStrategicAnchor(text))
        )
      (moments.size <= 1 && maxPly <= 8 && !hasStrategicAnchor) ||
      (allTensionPeaks && maxPly <= 12 && !hasStrategicAnchor)

  private def momentFen(rawDir: Path, gameId: String, ply: Int): Option[String] =
    val rawGamePath = rawDir.resolve(s"${gameId}.game_arc.json")
    if !Files.exists(rawGamePath) then None
    else
      val js = Json.parse(Files.readString(rawGamePath))
      (js \ "moments")
        .asOpt[List[JsObject]]
        .getOrElse(Nil)
        .find(moment => (moment \ "ply").asOpt[Int].contains(ply))
        .flatMap(moment => (moment \ "fen").asOpt[String])

  private def tokenizeMeaningful(raw: String): List[String] =
    Option(raw)
      .getOrElse("")
      .toLowerCase
      .split("[^a-z0-9]+")
      .toList
      .map(_.trim)
      .filter(token => token.length >= 4)

  private def readAuditSet(path: Path): Either[String, AuditSetManifest] =
    try
      Json.parse(Files.readString(path)).validate[AuditSetManifest].asEither.left.map(_.toString)
    catch case err: Exception => Left(err.getMessage)

  private def readRunReportOrExit(path: Path): RunReport =
    Json
      .parse(Files.readString(path))
      .validate[RunReport]
      .asEither match
      case Right(value) => value
      case Left(err) =>
        System.err.println(s"[player-qc-queue] failed to parse chronicle report `${path}`: $err")
        sys.exit(1)

  private def renderList(values: List[String]): String =
    values.map(_.trim).filter(_.nonEmpty).distinct.mkString(", ")

  private def parseConfig(args: List[String]): Config =
    @annotation.tailrec
    def loop(
        rest: List[String],
        positional: List[String],
        auditSetPath: Option[Path],
        fullReview: Boolean
    ): (List[String], Option[Path], Boolean) =
      rest match
        case "--audit-set" :: value :: tail => loop(tail, positional, Some(Paths.get(value)), fullReview)
        case "--full-review" :: tail        => loop(tail, positional, auditSetPath, true)
        case "--no-sampling" :: tail        => loop(tail, positional, auditSetPath, true)
        case head :: tail if head.startsWith("--") =>
          loop(tail, positional, auditSetPath, fullReview)
        case head :: tail =>
          loop(tail, positional :+ head, auditSetPath, fullReview)
        case Nil => (positional, auditSetPath, fullReview)

    val (positional, auditSetPath, fullReview) = loop(args, Nil, None, false)
    auditSetPath match
      case Some(path) =>
        Config(
          outPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultReviewDir.resolve("review_queue.jsonl")),
          summaryPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("review_queue_summary.json")),
          auditSetPath = Some(path),
          fullReview = fullReview
        )
      case None =>
        Config(
          manifestPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("slice_manifest.jsonl")),
          bookmakerOutputsPath =
            positional.lift(1).map(Paths.get(_)).getOrElse(DefaultBookmakerRunDir.resolve("bookmaker_outputs.jsonl")),
          chronicleReportPath =
            positional.lift(2).map(Paths.get(_)).getOrElse(DefaultChronicleRunDir.resolve("report.json")),
          outPath = positional.lift(3).map(Paths.get(_)).getOrElse(DefaultReviewDir.resolve("review_queue.jsonl")),
          summaryPath = positional.lift(4).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("review_queue_summary.json")),
          auditSetPath = None,
          fullReview = fullReview
        )
