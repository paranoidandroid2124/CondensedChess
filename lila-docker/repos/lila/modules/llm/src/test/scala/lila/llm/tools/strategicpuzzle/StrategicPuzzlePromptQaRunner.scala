package lila.llm.tools.strategicpuzzle

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import scala.concurrent.{ Await, ExecutionContext }

import akka.actor.ActorSystem
import play.api.libs.json.*
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.*
import lila.llm.analysis.CommentaryPayloadNormalizer
import lila.strategicPuzzle.StrategicPuzzle.{ RuntimeShell, StatusFull, StrategicPuzzleDoc, TerminalReveal }

object StrategicPuzzlePromptQaRunner:

  final case class Config(
      inputPath: Path,
      outPath: Path,
      limit: Int
  )

  private final case class StaticPromptMetrics(
      chars: Int,
      estimatedTokens: Int
  )

  private final case class OutputEval(
      paragraphs: Int,
      bannedHits: List[String],
      markdownLike: Boolean,
      wordCount: Int
  )

  private final case class LiveResult(
      model: String,
      promptTokens: Option[Int],
      cachedTokens: Option[Int],
      completionTokens: Option[Int],
      estimatedCostUsd: Option[Double],
      parseWarnings: List[String],
      commentary: String,
      eval: OutputEval
  )

  private final case class QaRow(
      puzzleId: String,
      opening: Option[String],
      dominantFamily: Option[String],
      terminalId: String,
      lineSan: List[String],
      terminalPrompt: StaticPromptMetrics,
      summaryPrompt: StaticPromptMetrics,
      terminal: Option[LiveResult],
      summary: Option[LiveResult]
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[strategic-puzzle-prompt-qa] $err")
        sys.exit(2)
      case Right(config) =>
        given Executor = ExecutionContext.global
        given ActorSystem = ActorSystem("strategic-puzzle-prompt-qa")
        val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
        try run(config, ws)
        finally
          ws.close()
          summon[ActorSystem].terminate()

  private def run(config: Config, ws: StandaloneAhcWSClient)(using Executor): Unit =
    if !Files.exists(config.inputPath) then
      throw new IllegalArgumentException(s"input file does not exist: ${config.inputPath}")
    Files.createDirectories(config.outPath.getParent)

    val openAi = OpenAiClient(ws, OpenAiConfig.fromEnv)
    if !openAi.isEnabled then
      throw new IllegalStateException("OpenAI is not enabled in the current environment")

    val docs = readDocs(config.inputPath)
    val rows = docs.flatMap(selectRow).take(config.limit).map(runQa(openAi))
    val report = renderReport(rows)
    Files.writeString(config.outPath, report, StandardCharsets.UTF_8)
    println(s"[strategic-puzzle-prompt-qa] wrote ${config.outPath} for ${rows.size} samples")

  private def runQa(openAi: OpenAiClient)(row: (StrategicPuzzleDoc, RuntimeShell, TerminalReveal)): QaRow =
    val (doc, shell, terminal) = row
    val terminalInput =
      StrategicPuzzlePrompt.TerminalInput(
        startFen = shell.startFen,
        sideToMove = shell.sideToMove,
        outcome = terminal.outcome,
        dominantFamily = terminal.dominantFamilyKey.orElse(terminal.familyKey).orElse(doc.dominantFamily.map(_.key)),
        lineSan = terminal.lineSan,
        siblingMoves = terminal.siblingMoves,
        opening = terminal.opening.orElse(doc.source.opening),
        eco = terminal.eco.orElse(doc.source.eco),
        draftCommentary = terminal.commentary
      )
    val summaryInput =
      StrategicPuzzlePrompt.SummaryInput(
        startFen = shell.startFen,
        sideToMove = shell.sideToMove,
        dominantFamily = terminal.dominantFamilyKey.orElse(doc.dominantFamily.map(_.key)),
        mainLine = terminal.lineSan,
        acceptedStarts = shell.rootChoices.filter(_.credit == StatusFull).map(_.san),
        opening = terminal.opening.orElse(doc.source.opening),
        eco = terminal.eco.orElse(doc.source.eco),
        draftSummary = List(terminal.title, terminal.summary).map(_.trim).filter(_.nonEmpty).mkString(" "),
        draftCommentary = firstParagraph(terminal.commentary)
      )

    val terminalPrompt = StrategicPuzzlePrompt.buildTerminalPrompt(terminalInput)
    val summaryPrompt = StrategicPuzzlePrompt.buildSummaryPrompt(summaryInput)
    val terminalResult =
      Await.result(
        openAi.strategicPuzzleTerminalSync(terminalInput, lang = "en", maxOutputTokens = Some(220)),
        scala.concurrent.duration.Duration(120, "seconds")
      ).map(toLiveResult)
    val summaryResult =
      Await.result(
        openAi.strategicPuzzleSummarySync(summaryInput, lang = "en", maxOutputTokens = Some(180)),
        scala.concurrent.duration.Duration(120, "seconds")
      ).map(toLiveResult)

    QaRow(
      puzzleId = doc.id,
      opening = doc.source.opening,
      dominantFamily = doc.dominantFamily.map(_.key),
      terminalId = terminal.id,
      lineSan = terminal.lineSan,
      terminalPrompt = StaticPromptMetrics(
        chars = terminalPrompt.length,
        estimatedTokens = StrategicPuzzlePrompt.estimateRequestTokens(terminalPrompt)
      ),
      summaryPrompt = StaticPromptMetrics(
        chars = summaryPrompt.length,
        estimatedTokens = StrategicPuzzlePrompt.estimateRequestTokens(summaryPrompt)
      ),
      terminal = terminalResult,
      summary = summaryResult
    )

  private def toLiveResult(result: OpenAiPolishResult): LiveResult =
    val commentary = CommentaryPayloadNormalizer.normalize(result.commentary).trim
    LiveResult(
      model = result.model,
      promptTokens = result.promptTokens,
      cachedTokens = result.cachedTokens,
      completionTokens = result.completionTokens,
      estimatedCostUsd = result.estimatedCostUsd,
      parseWarnings = result.parseWarnings,
      commentary = commentary,
      eval = evaluate(commentary)
    )

  private def evaluate(commentary: String): OutputEval =
    val normalized = Option(commentary).map(_.trim).getOrElse("")
    val lowered = normalized.toLowerCase
    val banned = List("stockfish", "best move", "computer", "engine", "evaluation")
    OutputEval(
      paragraphs = normalized.split("""\n\s*\n""").map(_.trim).count(_.nonEmpty),
      bannedHits = banned.filter(lowered.contains),
      markdownLike =
        normalized.linesIterator.exists { line =>
          val trimmed = line.trim
          trimmed.startsWith("#") || trimmed.startsWith("- ") || trimmed.startsWith("* ")
        },
      wordCount = normalized.split("\\s+").count(_.nonEmpty)
    )

  private def renderReport(rows: List[QaRow]): String =
    val terminalRows = rows.flatMap(_.terminal)
    val summaryRows = rows.flatMap(_.summary)
    val avgTerminalPrompt =
      if rows.isEmpty then 0.0 else rows.map(_.terminalPrompt.estimatedTokens.toDouble).sum / rows.size.toDouble
    val avgSummaryPrompt =
      if rows.isEmpty then 0.0 else rows.map(_.summaryPrompt.estimatedTokens.toDouble).sum / rows.size.toDouble
    val avgTerminalCost =
      if terminalRows.flatMap(_.estimatedCostUsd).isEmpty then None
      else Some(terminalRows.flatMap(_.estimatedCostUsd).sum / terminalRows.flatMap(_.estimatedCostUsd).size.toDouble)
    val avgSummaryCost =
      if summaryRows.flatMap(_.estimatedCostUsd).isEmpty then None
      else Some(summaryRows.flatMap(_.estimatedCostUsd).sum / summaryRows.flatMap(_.estimatedCostUsd).size.toDouble)

    val sb = new StringBuilder()
    sb.append("# Strategic Puzzle Prompt QA\n\n")
    sb.append(s"- Sample count: ${rows.size}\n")
    sb.append(s"- Terminal system tokens (est.): ${StrategicPuzzlePrompt.estimatedTerminalSystemTokens}\n")
    sb.append(s"- Summary system tokens (est.): ${StrategicPuzzlePrompt.estimatedSummarySystemTokens}\n")
    sb.append(f"- Average terminal request tokens (est.): $avgTerminalPrompt%.1f\n")
    sb.append(f"- Average summary request tokens (est.): $avgSummaryPrompt%.1f\n")
    sb.append(s"- Average terminal estimated cost usd: ${avgTerminalCost.map(v => f"$v%.6f").getOrElse("n/a")}\n")
    sb.append(s"- Average summary estimated cost usd: ${avgSummaryCost.map(v => f"$v%.6f").getOrElse("n/a")}\n\n")

    rows.foreach { row =>
      sb.append(s"## ${row.puzzleId}\n\n")
      sb.append(s"- Opening: ${row.opening.getOrElse("n/a")}\n")
      sb.append(s"- Dominant family: ${row.dominantFamily.getOrElse("n/a")}\n")
      sb.append(s"- Terminal id: ${row.terminalId}\n")
      sb.append(s"- Line: ${row.lineSan.mkString(" ")}\n")
      sb.append(s"- Terminal prompt chars: ${row.terminalPrompt.chars}\n")
      sb.append(s"- Terminal prompt est tokens: ${row.terminalPrompt.estimatedTokens}\n")
      sb.append(s"- Summary prompt chars: ${row.summaryPrompt.chars}\n")
      sb.append(s"- Summary prompt est tokens: ${row.summaryPrompt.estimatedTokens}\n\n")

      sb.append("### Terminal Output\n\n")
      row.terminal match
        case Some(result) =>
          sb.append(s"- Model: ${result.model}\n")
          sb.append(s"- Prompt tokens: ${result.promptTokens.map(_.toString).getOrElse("n/a")}\n")
          sb.append(s"- Cached tokens: ${result.cachedTokens.map(_.toString).getOrElse("n/a")}\n")
          sb.append(s"- Completion tokens: ${result.completionTokens.map(_.toString).getOrElse("n/a")}\n")
          sb.append(s"- Estimated cost usd: ${result.estimatedCostUsd.map(v => f"$v%.6f").getOrElse("n/a")}\n")
          sb.append(s"- Parse warnings: ${if result.parseWarnings.isEmpty then "none" else result.parseWarnings.mkString(", ")}\n")
          sb.append(s"- Paragraphs: ${result.eval.paragraphs}\n")
          sb.append(s"- Word count: ${result.eval.wordCount}\n")
          sb.append(s"- Markdown-like: ${result.eval.markdownLike}\n")
          sb.append(s"- Banned hits: ${if result.eval.bannedHits.isEmpty then "none" else result.eval.bannedHits.mkString(", ")}\n\n")
          sb.append("```text\n")
          sb.append(result.commentary)
          sb.append("\n```\n\n")
        case None =>
          sb.append("- No terminal output returned.\n\n")

      sb.append("### Summary Output\n\n")
      row.summary match
        case Some(result) =>
          sb.append(s"- Model: ${result.model}\n")
          sb.append(s"- Prompt tokens: ${result.promptTokens.map(_.toString).getOrElse("n/a")}\n")
          sb.append(s"- Cached tokens: ${result.cachedTokens.map(_.toString).getOrElse("n/a")}\n")
          sb.append(s"- Completion tokens: ${result.completionTokens.map(_.toString).getOrElse("n/a")}\n")
          sb.append(s"- Estimated cost usd: ${result.estimatedCostUsd.map(v => f"$v%.6f").getOrElse("n/a")}\n")
          sb.append(s"- Parse warnings: ${if result.parseWarnings.isEmpty then "none" else result.parseWarnings.mkString(", ")}\n")
          sb.append(s"- Paragraphs: ${result.eval.paragraphs}\n")
          sb.append(s"- Word count: ${result.eval.wordCount}\n")
          sb.append(s"- Markdown-like: ${result.eval.markdownLike}\n")
          sb.append(s"- Banned hits: ${if result.eval.bannedHits.isEmpty then "none" else result.eval.bannedHits.mkString(", ")}\n\n")
          sb.append("```text\n")
          sb.append(result.commentary)
          sb.append("\n```\n\n")
        case None =>
          sb.append("- No summary output returned.\n\n")
    }
    sb.toString

  private def firstParagraph(text: String): Option[String] =
    Option(text)
      .map(_.split("""\n\s*\n""").map(_.trim).find(_.nonEmpty).getOrElse("").trim)
      .filter(_.nonEmpty)

  private def readDocs(path: Path): List[StrategicPuzzleDoc] =
    Files.readAllLines(path, StandardCharsets.UTF_8).toArray(new Array[String](0)).toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { line =>
        Json.parse(line).as[StrategicPuzzleDoc]
      }

  private def selectRow(doc: StrategicPuzzleDoc): Option[(StrategicPuzzleDoc, RuntimeShell, TerminalReveal)] =
    doc.runtimeShell.flatMap { shell =>
      shell.terminals.find(_.outcome == StatusFull).orElse(shell.terminals.headOption).map(terminal => (doc, shell, terminal))
    }

  private def parseArgs(args: List[String]): Either[String, Config] =
    val workspaceRoot = detectWorkspaceRoot()
    val defaults =
      Config(
        inputPath = workspaceRoot.resolve(Path.of("tools", "strategic_puzzles", "runtime_prompt_sample10_20260319.jsonl")),
        outPath = workspaceRoot.resolve(Path.of("tools", "strategic_puzzles", "reports", "StrategicPuzzlePromptQaReport.md")),
        limit = 10
      )

    @annotation.tailrec
    def loop(rest: List[String], cfg: Config): Either[String, Config] =
      rest match
        case Nil => Right(cfg)
        case head :: tail if head.startsWith("--input=") =>
          loop(tail, cfg.copy(inputPath = Path.of(head.stripPrefix("--input=")).toAbsolutePath.normalize))
        case "--input" :: value :: tail =>
          loop(tail, cfg.copy(inputPath = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--out=") =>
          loop(tail, cfg.copy(outPath = Path.of(head.stripPrefix("--out=")).toAbsolutePath.normalize))
        case "--out" :: value :: tail =>
          loop(tail, cfg.copy(outPath = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--limit=") =>
          head.stripPrefix("--limit=").toIntOption match
            case Some(limit) if limit > 0 => loop(tail, cfg.copy(limit = limit))
            case _                        => Left(s"invalid --limit: $head")
        case "--limit" :: value :: tail =>
          value.toIntOption match
            case Some(limit) if limit > 0 => loop(tail, cfg.copy(limit = limit))
            case _                        => Left(s"invalid --limit: $value")
        case unknown :: _ => Left(s"unknown argument: $unknown")

    loop(args, defaults)

  private def detectWorkspaceRoot(): Path =
    val cwd = Path.of(".").toAbsolutePath.normalize
    if Files.isDirectory(cwd.resolve("tools")) && Files.isDirectory(cwd.resolve("lila-docker")) then cwd
    else
      Option(cwd.getParent)
        .flatMap(parent => Option(parent.getParent))
        .flatMap(grandParent => Option(grandParent.getParent))
        .filter(root => Files.isDirectory(root.resolve("tools")) && Files.isDirectory(root.resolve("lila-docker")))
        .getOrElse(cwd)
