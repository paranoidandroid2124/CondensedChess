package lila.llm.analysis

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.*
import scala.sys.process.*
import scala.util.control.NonFatal
import java.util.regex.Pattern

import _root_.chess.format.Fen
import _root_.chess.variant.Standard
import lila.llm.PgnAnalysisHelper
import lila.llm.model.{ ExtendedAnalysisData }
import lila.llm.model.strategic.{ VariationLine, PlanContinuity }
import munit.FunSuite
import play.api.libs.json.*

/**
 * End-to-end quality harness:
 * - Loads 4 era benchmark games from docs
 * - Iterates every ply from first move to last move
 * - Runs real Stockfish WASM MultiPV per position
 * - Renders Bookmaker commentary for each ply
 * - Writes a consolidated report with quality + engine-alignment metrics
 */
class EraFullGameNarrativeQualityTest extends FunSuite {
  override val munitTimeout = 10.minutes

  private case class EraGame(
      id: String,
      event: String,
      white: String,
      black: String,
      date: String,
      pgn: String
  )

  private case class QualityMetrics(
      lexicalDiversity: Double,
      sentenceCount: Int,
      uniqueSentenceRatio: Double,
      duplicateSentenceCount: Int,
      boilerplateHits: Int,
      moveTokenCount: Int,
      scoreTokenCount: Int,
      variationAnchorCoverage: Double,
      playedMoveRank: Int,
      playedMoveDeltaCp: Int,
      engineNarrativeMismatch: Boolean,
      qualityScore: Int
  )

  private case class PlyResult(
      ply: Int,
      playedUci: String,
      firstSentenceNorm: String,
      quality: QualityMetrics,
      prose: String
  )

  private case class GameSummary(
      game: EraGame,
      totalPlies: Int,
      generatedPlies: Int,
      failedPlies: List[(Int, String)],
      results: List[PlyResult],
      averageQuality: Double,
      averageLexical: Double,
      averageAnchorCoverage: Double,
      averagePlayedDeltaCp: Double,
      averagePlayedRank: Double,
      playedMoveSeenRate: Double,
      engineMismatchCount: Int,
      lowQualityCount: Int,
      duplicateLeadTransitions: Int,
      longestLeadRepeatStreak: Int
  )

  private val moveTokenRegex =
    """(?i)\b(?:[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?|O-O(?:-O)?)\b""".r
  private val scoreTokenRegex = """(?i)(?:\([+\-]\d+\.\d\)|\bmate\b|\bcp\b|#[0-9]+)""".r
  private val tokenRegex = """[A-Za-z][A-Za-z0-9'\-]{1,}""".r
  private val sentenceSplitRegex = """(?<=[\.\!\?])\s+|\n+""".r
  private val moveHeaderPrefixRegex = """^\s*\d+\.(?:\.\.)?\s*[^:\n]{1,40}:\s*""".r
  private val tagRegex = """(?m)^\[(\w+)\s+"(.*)"\]\s*$""".r
  private val jsonStartMarker = "---JSON_START---"
  private val jsonEndMarker = "---JSON_END---"
  private val engineDepth = 8
  private val engineMultiPv = 4

  private val boilerplateLexicon = List(
    "is another good option",
    "is also playable here",
    "deserves attention",
    "both sides have their chances",
    "the game remains tense",
    "the position is roughly equal",
    "in practical terms, this is comfortable to play",
    "accuracy is required to hold the balance",
    "the plan is clear:",
    "both sides are still coordinating pieces"
  )

  private val negativeNarrativeLexicon = List(
    "blunder",
    "mistake",
    "inaccuracy",
    "misses",
    "slip",
    "inferior",
    "drops",
    "loses"
  )

  private val strongPositiveNarrativeLexicon = List(
    "best move",
    "excellent choice",
    "strong move",
    "very accurate",
    "precise move",
    "fully sound"
  )

  test("per-ply era benchmark quality report (all moves, all 4 games)") {
    val pgnPath = Path.of("modules/llm/docs/EraBenchmarkGames.pgn")
    assert(Files.exists(pgnPath), s"Missing benchmark PGN file: $pgnPath")

    val games = loadGames(pgnPath)
    assertEquals(games.size, 4)

    val summaries = games.map(evaluateGame)
    val report = renderReport(summaries)
    val reportPath = Path.of("modules/llm/docs/EraBenchmarkPlyReport.md")
    Files.writeString(reportPath, report, StandardCharsets.UTF_8)

    val totalPlies = summaries.map(_.totalPlies).sum
    val generatedPlies = summaries.map(_.generatedPlies).sum
    val coverage =
      if (totalPlies == 0) 0.0 else generatedPlies.toDouble / totalPlies.toDouble

    val allQualities = summaries.flatMap(_.results.map(_.quality.qualityScore))
    val avgQuality =
      if (allQualities.isEmpty) 0.0 else allQualities.sum.toDouble / allQualities.size.toDouble
    val mismatchCount = summaries.map(_.engineMismatchCount).sum
    val avgSeenRate = average(summaries.map(_.playedMoveSeenRate))

    println(
      f"[era-quality] games=${summaries.size}%d totalPlies=$totalPlies%d generated=$generatedPlies%d coverage=$coverage%.3f avgQuality=$avgQuality%.2f seenRate=$avgSeenRate%.3f mismatches=$mismatchCount%d report=$reportPath"
    )

    assert(
      coverage >= 0.95,
      f"Coverage too low: generated $generatedPlies / $totalPlies (coverage=$coverage%.3f)"
    )
    assert(
      avgQuality >= 40.0,
      f"Average quality score too low: $avgQuality%.2f"
    )
    assert(
      avgSeenRate >= 0.95,
      f"Played-move MultiPV coverage too low: $avgSeenRate%.3f"
    )
  }

  private def evaluateGame(game: EraGame): GameSummary = {
    val plyDataList = PgnAnalysisHelper.extractPlyData(game.pgn) match {
      case Left(err)  => fail(s"[${game.id}] PGN parse failed: $err")
      case Right(ply) => ply
    }

    val distinctFens = plyDataList.map(_.fen).distinct
    val playedMovesByFen =
      plyDataList
        .groupBy(_.fen)
        .view
        .mapValues(_.map(_.playedUci).distinct)
        .toMap
    val engineVariations = loadEngineVariations(distinctFens, playedMovesByFen)
    val missingEngineFens =
      distinctFens.count(f => engineVariations.get(f).forall(_.isEmpty))

    assert(
      missingEngineFens == 0,
      s"[${game.id}] Missing engine PV for $missingEngineFens/${distinctFens.size} FENs"
    )

    var prevPlanContinuity: Option[PlanContinuity] = None
    var prevAnalysis: Option[ExtendedAnalysisData] = None

    val results = ListBuffer.empty[PlyResult]
    val failures = ListBuffer.empty[(Int, String)]

    plyDataList.foreach { pd =>
      val variations = engineVariations.getOrElse(pd.fen, Nil) match {
        case vs if vs.nonEmpty => vs
        case _                 => List(VariationLine(moves = List(pd.playedUci), scoreCp = 0, mate = None, depth = 0))
      }
      val phase = inferPhase(pd.fen)

      try {
        CommentaryEngine.assessExtended(
          fen = pd.fen,
          variations = variations,
          playedMove = Some(pd.playedUci),
          opening = None,
          phase = Some(phase),
          ply = pd.ply,
          prevMove = Some(pd.playedUci),
          prevPlanContinuity = prevPlanContinuity,
          prevAnalysis = prevAnalysis
        ) match {
          case None =>
            failures += ((pd.ply, "assessExtended returned None"))

          case Some(data) =>
            val ctx = NarrativeContextBuilder.build(
              data = data,
              ctx = data.toContext,
              prevAnalysis = prevAnalysis
            )
            val prose = BookStyleRenderer.render(ctx).trim
            val quality = computeQualityMetrics(prose, pd.fen, pd.playedUci, variations)
            val firstSentence = extractLeadSentence(prose)

            results += PlyResult(
              ply = pd.ply,
              playedUci = pd.playedUci,
              firstSentenceNorm = normalizeSentence(firstSentence),
              quality = quality,
              prose = prose
            )

            prevPlanContinuity = data.planContinuity
            prevAnalysis = Some(data)
        }
      } catch {
        case NonFatal(e) =>
          val reason = Option(e.getMessage).getOrElse(e.getClass.getSimpleName).take(200)
          failures += ((pd.ply, reason))
      }
    }

    val plyResults = results.toList
    val avgQuality = average(plyResults.map(_.quality.qualityScore.toDouble))
    val avgLexical = average(plyResults.map(_.quality.lexicalDiversity))
    val avgAnchor = average(plyResults.map(_.quality.variationAnchorCoverage))
    val seenRanks = plyResults.flatMap(r => Option.when(r.quality.playedMoveRank > 0)(r.quality.playedMoveRank.toDouble))
    val seenDeltas =
      plyResults.flatMap(r => Option.when(r.quality.playedMoveRank > 0)(r.quality.playedMoveDeltaCp.toDouble))
    val avgRank = average(seenRanks)
    val avgDelta = average(seenDeltas)
    val seenRate =
      if (plyResults.isEmpty) 0.0 else seenRanks.size.toDouble / plyResults.size.toDouble
    val mismatchCount = plyResults.count(_.quality.engineNarrativeMismatch)
    val lowQualityCount = plyResults.count(_.quality.qualityScore < 70)
    val duplicateLeadTransitions = countDuplicateAdjacent(plyResults.map(_.firstSentenceNorm))
    val longestRepeatStreak = longestRepeatStreakOf(plyResults.map(_.firstSentenceNorm))

    GameSummary(
      game = game,
      totalPlies = plyDataList.size,
      generatedPlies = plyResults.size,
      failedPlies = failures.toList,
      results = plyResults,
      averageQuality = avgQuality,
      averageLexical = avgLexical,
      averageAnchorCoverage = avgAnchor,
      averagePlayedDeltaCp = avgDelta,
      averagePlayedRank = avgRank,
      playedMoveSeenRate = seenRate,
      engineMismatchCount = mismatchCount,
      lowQualityCount = lowQualityCount,
      duplicateLeadTransitions = duplicateLeadTransitions,
      longestLeadRepeatStreak = longestRepeatStreak
    )
  }

  private def loadGames(path: Path): List[EraGame] = {
    val raw = Files.readString(path, StandardCharsets.UTF_8).replace("\uFEFF", "").trim
    if (raw.isEmpty) Nil
    else {
      raw
        .split("(?m)(?=^\\[Event\\s+\")")
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .map { block =>
          val tags = parseTags(block)
          val event = tags.getOrElse("Event", "Unknown Event")
          val white = tags.getOrElse("White", "White")
          val black = tags.getOrElse("Black", "Black")
          val date = tags.getOrElse("Date", "Unknown")
          val id = tags.getOrElse("CorpusKey", s"${event}_${date}".replaceAll("\\s+", "_").toLowerCase)

          EraGame(
            id = id,
            event = event,
            white = white,
            black = black,
            date = date,
            pgn = block
          )
        }
    }
  }

  private def parseTags(pgn: String): Map[String, String] =
    tagRegex.findAllMatchIn(pgn).map(m => m.group(1) -> m.group(2)).toMap

  private def loadEngineVariations(
      fens: List[String],
      playedMovesByFen: Map[String, List[String]],
      depth: Int = engineDepth,
      multiPv: Int = engineMultiPv
  ): Map[String, List[VariationLine]] = {
    val uniqueFens = fens.distinct.filter(_.nonEmpty)
    if (uniqueFens.isEmpty) Map.empty
    else {
      val inputPath = Files.createTempFile("era-stockfish-input-", ".json")
      val payload = Json.obj(
        "fens" -> uniqueFens,
        "playedMovesByFen" -> playedMovesByFen,
        "depth" -> depth,
        "multiPv" -> multiPv
      )
      val stdout = new StringBuilder()
      val stderr = new StringBuilder()

      try {
        Files.writeString(inputPath, Json.stringify(payload), StandardCharsets.UTF_8)
        val command = Seq("node", "run_stockfish_wasm.js", "--input", inputPath.toString)
        val exit = Process(command, Path.of(".").toFile).!(
          ProcessLogger(
            out => stdout.append(out).append('\n'),
            err => stderr.append(err).append('\n')
          )
        )

        assert(
          exit == 0,
          s"Stockfish helper failed with exit code $exit\n${stderr.toString.take(2000)}"
        )

        val jsonText = extractJsonPayload(stdout.toString)
        Json.parse(jsonText).as[Map[String, List[VariationLine]]]
      } finally {
        Files.deleteIfExists(inputPath)
      }
    }
  }

  private def extractJsonPayload(output: String): String = {
    val start = output.indexOf(jsonStartMarker)
    val end = output.indexOf(jsonEndMarker)
    if (start < 0 || end < 0 || end <= start)
      fail(s"Could not parse Stockfish JSON payload.\nOutput:\n${output.take(2000)}")
    output.substring(start + jsonStartMarker.length, end).trim
  }

  private def inferPhase(fen: String): String =
    Fen
      .read(Standard, Fen.Full(fen))
      .map { pos =>
        val pieces = pos.board.occupied.count
        if (pieces > 28) "opening"
        else if (pieces < 12) "endgame"
        else "middlegame"
      }
      .getOrElse("middlegame")

  private def sortVariationsForSideToMove(fen: String, vars: List[VariationLine]): List[VariationLine] =
    if fenSideToMoveIsWhite(fen) then vars.sortBy(v => -v.effectiveScore)
    else vars.sortBy(_.effectiveScore)

  private def fenSideToMoveIsWhite(fen: String): Boolean =
    Option(fen).getOrElse("").trim.split("\\s+").drop(1).headOption.contains("w")

  private def cpLossForSideToMove(fen: String, bestScore: Int, playedScore: Int): Int =
    if fenSideToMoveIsWhite(fen) then (bestScore - playedScore).max(0)
    else (playedScore - bestScore).max(0)

  private def computeQualityMetrics(
      prose: String,
      fen: String,
      playedUci: String,
      vars: List[VariationLine]
  ): QualityMetrics = {
    val lower = prose.toLowerCase

    val tokens = tokenRegex.findAllIn(lower).toList
    val uniqueTokens = tokens.distinct
    val lexicalDiversity =
      if (tokens.isEmpty) 0.0 else uniqueTokens.size.toDouble / tokens.size.toDouble

    val sentences = extractSentences(prose)
    val sentenceNorm = sentences.map(normalizeSentence).filter(_.length >= 8)
    val uniqueSentenceCount = sentenceNorm.distinct.size
    val sentenceCount = sentenceNorm.size
    val uniqueSentenceRatio =
      if (sentenceCount == 0) 1.0 else uniqueSentenceCount.toDouble / sentenceCount.toDouble
    val duplicateSentenceCount = (sentenceCount - uniqueSentenceCount).max(0)

    val boilerplateHits = boilerplateLexicon.map(p => occurrencesOf(lower, p)).sum
    val moveTokenCount = moveTokenRegex.findAllIn(prose).length
    val scoreTokenCount = scoreTokenRegex.findAllIn(prose).length

    val anchors =
      vars.take(3).flatMap(v => NarrativeUtils.uciListToSan(fen, v.moves).headOption).map(_.toLowerCase)
    val covered = anchors.count(a => lower.contains(a))
    val variationAnchorCoverage =
      if (anchors.isEmpty) 1.0 else covered.toDouble / anchors.size.toDouble

    val ranked = sortVariationsForSideToMove(fen, vars)
    val bestScore = ranked.headOption.map(_.effectiveScore).getOrElse(0)
    val playedIdx = ranked.indexWhere(_.moves.headOption.contains(playedUci))
    val playedMoveRank = if (playedIdx >= 0) playedIdx + 1 else 0
    val playedMoveDeltaCp =
      if (playedIdx >= 0) cpLossForSideToMove(fen, bestScore, ranked(playedIdx).effectiveScore) else 0

    val playedSanOpt = NarrativeUtils.uciListToSan(fen, List(playedUci)).headOption.map(_.toLowerCase)
    def collectFocusText(matches: (String, String) => Boolean): Option[String] =
      playedSanOpt.flatMap { san =>
        val hitIndices = sentences.zipWithIndex.collect { case (s, idx) if matches(s, san) => idx }
        if (hitIndices.isEmpty) None
        else
          Some(
            hitIndices
              .flatMap(i => List(i - 1, i, i + 1))
              .filter(i => i >= 0 && i < sentences.size)
              .distinct
              .sorted
              .map(sentences)
              .mkString(" ")
              .toLowerCase
          )
      }

    val strictFocusLower = collectFocusText { (sentence, san) =>
      val escaped = Pattern.quote(san)
      val pattern = s"(?i)(\\*\\*$escaped\\*\\*|(?<![a-z0-9])$escaped(?![a-z0-9]))".r
      pattern.findFirstIn(sentence).nonEmpty
    }
    val broadFocusLower = collectFocusText((sentence, san) => sentence.toLowerCase.contains(san))
    val focusedLower = strictFocusLower.orElse(broadFocusLower).getOrElse(lower)

    val hasNegativeLanguageFocused = negativeNarrativeLexicon.exists(focusedLower.contains)
    val hasNegativeLanguageBroad = negativeNarrativeLexicon.exists(lower.contains)
    val hasStrongPositiveLanguage = strongPositiveNarrativeLexicon.exists(lower.contains)
    val engineNarrativeMismatch =
      (playedMoveRank > 0 && playedMoveDeltaCp >= 140 && !hasNegativeLanguageBroad) ||
        (playedMoveRank > 0 && playedMoveDeltaCp <= 25 && hasNegativeLanguageFocused) ||
        (playedMoveRank > 0 && playedMoveDeltaCp >= 200 && hasStrongPositiveLanguage)

    val qualityScore = {
      var s = 100
      if (lexicalDiversity < 0.34) s -= 20
      else if (lexicalDiversity < 0.42) s -= 10
      if (uniqueSentenceRatio < 0.70) s -= 20
      else if (uniqueSentenceRatio < 0.82) s -= 10
      s -= math.min(24, boilerplateHits * 8)
      if (variationAnchorCoverage < 0.34) s -= 20
      else if (variationAnchorCoverage < 0.67) s -= 10
      if (moveTokenCount < 6) s -= 10

      if (playedMoveRank == 0) s -= 8
      else if (playedMoveDeltaCp >= 220) s -= 20
      else if (playedMoveDeltaCp >= 140) s -= 14
      else if (playedMoveDeltaCp >= 80) s -= 8

      if (engineNarrativeMismatch) s -= 12
      s.max(0).min(100)
    }

    QualityMetrics(
      lexicalDiversity = lexicalDiversity,
      sentenceCount = sentenceCount,
      uniqueSentenceRatio = uniqueSentenceRatio,
      duplicateSentenceCount = duplicateSentenceCount,
      boilerplateHits = boilerplateHits,
      moveTokenCount = moveTokenCount,
      scoreTokenCount = scoreTokenCount,
      variationAnchorCoverage = variationAnchorCoverage,
      playedMoveRank = playedMoveRank,
      playedMoveDeltaCp = playedMoveDeltaCp,
      engineNarrativeMismatch = engineNarrativeMismatch,
      qualityScore = qualityScore
    )
  }

  private def extractSentences(text: String): List[String] =
    sentenceSplitRegex
      .split(text.replace("\r\n", "\n"))
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

  private def extractLeadSentence(text: String): String = {
    val stripped = moveHeaderPrefixRegex.replaceFirstIn(text.trim, "")
    extractSentences(stripped).headOption.getOrElse("")
  }

  private def normalizeSentence(s: String): String =
    s.toLowerCase
      .replaceAll("""[*`_>\[\]\(\)]""", " ")
      .replaceAll("""[^a-z0-9\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim

  private def occurrencesOf(haystack: String, needle: String): Int =
    if (needle.isEmpty) 0
    else haystack.sliding(needle.length).count(_ == needle)

  private def buildWorstPlyTaxonomy(summaries: List[GameSummary]): List[(String, Int, List[String])] = {
    val sampled =
      summaries.flatMap { gs =>
        gs.results
          .sortBy(_.quality.qualityScore)
          .take(8)
          .map(r => (gs.game.id, r))
      }

    val concreteReasonHints = List(
      "issue:",
      "after this",
      "forcing reply",
      "pin",
      "hanging",
      "fork",
      "skewer",
      "threat",
      "does not neutralize",
      "outside the sampled principal lines"
    )
    val templateHints = List(
      "strategic focus:",
      "key theme:",
      "strategic priority:",
      "the practical roadmap centers on",
      "current play is organized around",
      "most sensible plans here converge on"
    )

    val buckets = scala.collection.mutable.LinkedHashMap.empty[String, scala.collection.mutable.ListBuffer[String]]

    sampled.foreach { case (gameId, r) =>
      val lower = r.prose.toLowerCase
      val hasConcreteReason = concreteReasonHints.exists(lower.contains)
      val templateHits = templateHints.count(lower.contains)
      val categories = scala.collection.mutable.ListBuffer.empty[String]

      if (r.quality.engineNarrativeMismatch)
        categories += "Engine/Text polarity mismatch"
      if (templateHits >= 2 || (templateHits >= 1 && !hasConcreteReason))
        categories += "Template-heavy position framing"
      if ((r.quality.scoreTokenCount >= 4 || lower.contains(" cp ")) && !hasConcreteReason)
        categories += "Numeric-heavy critique with weak causal detail"
      if ((r.quality.playedMoveRank == 0 || r.quality.playedMoveRank >= 3) &&
          !lower.contains("better is **") &&
          !lower.contains("stronger is **") &&
          !lower.contains("engine reference"))
        categories += "Lower-rank move not contrasted clearly"
      if (categories.isEmpty && r.quality.playedMoveDeltaCp >= 80 && !hasConcreteReason)
        categories += "Why-bad explanation under-specified"

      categories.distinct.foreach { cat =>
        val sample = s"$gameId ply ${r.ply} (${r.playedUci}, score=${r.quality.qualityScore})"
        val list = buckets.getOrElseUpdate(cat, scala.collection.mutable.ListBuffer.empty[String])
        if (!list.contains(sample)) list += sample
      }
    }

    buckets.toList
      .map { case (label, samples) => (label, samples.size, samples.toList) }
      .sortBy { case (_, count, _) => -count }
  }

  private def average(xs: List[Double]): Double =
    if (xs.isEmpty) 0.0 else xs.sum / xs.size

  private def countDuplicateAdjacent(lines: List[String]): Int =
    lines
      .sliding(2)
      .count {
        case List(a, b) if a.nonEmpty && a == b => true
        case _                                   => false
      }

  private def longestRepeatStreakOf(lines: List[String]): Int = {
    var best = 1
    var run = 1
    var prev = ""
    lines.foreach { s =>
      if (s.nonEmpty && s == prev) run += 1
      else run = 1
      if (run > best) best = run
      prev = s
    }
    if (lines.isEmpty) 0 else best
  }

  private def renderReport(summaries: List[GameSummary]): String = {
    val totalPlies = summaries.map(_.totalPlies).sum
    val generated = summaries.map(_.generatedPlies).sum
    val coverage = if (totalPlies == 0) 0.0 else generated.toDouble / totalPlies.toDouble
    val allQualityScores = summaries.flatMap(_.results.map(_.quality.qualityScore.toDouble))
    val avgQuality = average(allQualityScores)
    val avgLexical = average(summaries.map(_.averageLexical))
    val avgAnchor = average(summaries.map(_.averageAnchorCoverage))
    val avgDelta = average(summaries.map(_.averagePlayedDeltaCp))
    val avgRank = average(summaries.map(_.averagePlayedRank))
    val avgSeenRate = average(summaries.map(_.playedMoveSeenRate))
    val mismatchCount = summaries.map(_.engineMismatchCount).sum
    val taxonomy = buildWorstPlyTaxonomy(summaries)

    val sb = new StringBuilder()
    sb.append("# Era Full-Game Per-Ply Commentary Report\n\n")
    sb.append(s"- Games: ${summaries.size}\n")
    sb.append(s"- Total plies: $totalPlies\n")
    sb.append(s"- Generated plies: $generated\n")
    sb.append(f"- Coverage: $coverage%.3f\n")
    sb.append(f"- Average quality score: $avgQuality%.2f / 100\n")
    sb.append(f"- Average lexical diversity: $avgLexical%.3f\n")
    sb.append(f"- Average variation-anchor coverage: $avgAnchor%.3f\n")
    sb.append(f"- Played move seen in MultiPV (avg): $avgSeenRate%.3f\n")
    sb.append(f"- Average played-move rank (seen plies): $avgRank%.2f\n")
    sb.append(f"- Average played-move cp loss vs best (seen plies): $avgDelta%.2f\n")
    sb.append(s"- Engine/text mismatch plies: $mismatchCount\n\n")

    if (taxonomy.nonEmpty) {
      sb.append("## Error Taxonomy (sampled worst plies)\n\n")
      taxonomy.foreach { case (label, count, samples) =>
        sb.append(s"- $label: $count\n")
        if (samples.nonEmpty) {
          sb.append(s"  samples: ${samples.take(3).mkString("; ")}\n")
        }
      }
      sb.append("\n")
    }

    summaries.foreach { gs =>
      val lowShare =
        if (gs.generatedPlies == 0) 0.0 else gs.lowQualityCount.toDouble / gs.generatedPlies.toDouble
      val duplicateLeadShare =
        if (gs.generatedPlies <= 1) 0.0
        else gs.duplicateLeadTransitions.toDouble / (gs.generatedPlies - 1).toDouble

      sb.append(s"## ${gs.game.id}\n\n")
      sb.append(s"- Event: ${gs.game.event}\n")
      sb.append(s"- Players: ${gs.game.white} vs ${gs.game.black}\n")
      sb.append(s"- Date: ${gs.game.date}\n")
      sb.append(s"- Plies: ${gs.generatedPlies}/${gs.totalPlies}\n")
      sb.append(f"- Avg quality: ${gs.averageQuality}%.2f\n")
      sb.append(f"- Avg lexical diversity: ${gs.averageLexical}%.3f\n")
      sb.append(f"- Avg anchor coverage: ${gs.averageAnchorCoverage}%.3f\n")
      sb.append(f"- Played move seen in MultiPV: ${gs.playedMoveSeenRate}%.3f\n")
      sb.append(f"- Avg played-move rank (seen plies): ${gs.averagePlayedRank}%.2f\n")
      sb.append(f"- Avg played-move cp loss vs best (seen plies): ${gs.averagePlayedDeltaCp}%.2f\n")
      sb.append(s"- Engine/text mismatch plies: ${gs.engineMismatchCount}\n")
      sb.append(s"- Low-quality plies (<70): ${gs.lowQualityCount}\n")
      sb.append(f"- Low-quality share: $lowShare%.3f\n")
      sb.append(s"- Duplicate adjacent lead-ins: ${gs.duplicateLeadTransitions}\n")
      sb.append(f"- Duplicate adjacent lead-in share: $duplicateLeadShare%.3f\n")
      sb.append(s"- Longest repeated lead-in streak: ${gs.longestLeadRepeatStreak}\n")

      if (gs.failedPlies.nonEmpty) {
        sb.append("\n### Failed plies\n\n")
        gs.failedPlies.take(20).foreach { case (ply, reason) =>
          sb.append(s"- ply $ply: $reason\n")
        }
      }

      val worst = gs.results.sortBy(_.quality.qualityScore).take(8)
      if (worst.nonEmpty) {
        sb.append("\n### Worst plies\n\n")
        worst.foreach { w =>
          val snippet = w.prose.linesIterator.take(2).mkString(" ").take(220)
          sb.append(
            f"- ply ${w.ply}%d (${w.playedUci}): score=${w.quality.qualityScore}%d lexical=${w.quality.lexicalDiversity}%.3f uniqueSent=${w.quality.uniqueSentenceRatio}%.3f anchor=${w.quality.variationAnchorCoverage}%.3f rank=${w.quality.playedMoveRank}%d deltaCp=${w.quality.playedMoveDeltaCp}%d mismatch=${w.quality.engineNarrativeMismatch} :: ${snippet}\n"
          )
        }
      }

      sb.append("\n")
    }

    sb.toString
  }
}
