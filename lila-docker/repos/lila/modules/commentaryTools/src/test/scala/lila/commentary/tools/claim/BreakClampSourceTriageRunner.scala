package lila.commentary.tools.claim

import java.nio.file.{ Path, Paths }

import lila.commentary.PgnAnalysisHelper
import lila.commentary.analysis.strategic.BreakClampCandidateScanner
import lila.commentary.analysis.strategic.BreakClampSourceTriage
import lila.commentary.model.strategic.VariationLine
import lila.commentary.tools.review.CommentaryPlayerQcSupport.LocalUciEngine

object BreakClampSourceTriageRunner:

  private final case class Dummy()

  private final class LocalEngine(engine: LocalUciEngine)
      extends BreakClampCandidateScanner.Engine
      with SourceReview.SourceReviewEngine:
    override def newGame(): Unit = engine.newGame()
    override def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] =
      engine.analyze(fen, depth, multiPv)

  @main def runBreakClampSourceTriage(args: String*): Unit =
    val argList = args.toList
    val depth = optionInt(argList, "--depth").getOrElse(12)
    val multiPv = optionInt(argList, "--multi-pv").orElse(optionInt(argList, "--multiPv")).getOrElse(3)
    val nearTopCp = optionInt(argList, "--near-top-cp").getOrElse(50)
    val offset = optionInt(argList, "--offset").getOrElse(0)
    val limit = optionInt(argList, "--limit")
    val maxCandidates = optionInt(argList, "--max-candidates").getOrElse(24)
    val admitLimit = optionInt(argList, "--admit-limit").getOrElse(1)
    val sourceUrl = optionString(argList, "--source-url").getOrElse(BreakClampSourceTriage.DefaultSourceUrl)
    val selectedIds = optionSet(argList, "--ids")
    val games =
      sourceCatalogGames(argList, selectedIds) ++
        corpusGames(argList) ++
        pgnFileGame(argList)
    if games.isEmpty then
      System.err.println(
        "[break-clamp-triage] no input games; provide --corpus, --pgn-file with --id/--label, or --source-catalog"
      )
      sys.exit(1)

    val config =
      BreakClampSourceTriage.TriageConfig(
        scanConfig = BreakClampCandidateScanner.ScanConfig(
          depth = depth,
          multiPv = multiPv,
          nearTopCp = nearTopCp,
          offset = offset,
          limit = limit
        ),
        maxCandidates = maxCandidates,
        admitLimit = admitLimit,
        sourceUrl = sourceUrl
      )
    val maybeEnginePath = enginePath(argList)
    if maybeEnginePath.isEmpty then
      System.err.println("[break-clamp-triage] missing engine path; scanner rows will be engine-blocked")
    val report =
      maybeEnginePath match
        case None =>
          BreakClampSourceTriage.triage(
            games,
            scannerEngine = None,
            reviewEngine = None,
            config = config
          )
        case Some(path) =>
          val engine = LocalUciEngine(path)
          val adapter = LocalEngine(engine)
          try
            BreakClampSourceTriage.triage(
              games,
              scannerEngine = Some(adapter),
              reviewEngine = Some(adapter),
              config = config
            )
          finally engine.close()
    val (matrix, review) = BreakClampSourceTriage.writeArtifacts(report)
    println(report.tsv)
    println(s"wrote=${matrix.toAbsolutePath}")
    println(s"review=${review.toAbsolutePath}")
    println(report.markdown.linesIterator.take(10).mkString("\n"))

  private def sourceCatalogGames(args: List[String], ids: Set[String]): List[BreakClampCandidateScanner.SourceGame] =
    if !args.contains("--source-catalog") then Nil
    else
      SourceWitnessCatalog.all
        .filter(source => ids.isEmpty || ids.contains(source.id))
        .map { source =>
          val focusColor =
            PgnAnalysisHelper
              .extractPlyDataStrict(source.pgn)
              .toOption
              .flatMap(_.find(ply => source.candidatePlyRange.contains(ply.ply)).map(_.color))
          BreakClampCandidateScanner.SourceGame(
            id = source.id,
            label = source.gameName,
            pgn = source.pgn,
            plyRange = Some(source.candidatePlyRange.start -> source.candidatePlyRange.end),
            focusColor = focusColor
          )
        }

  private def corpusGames(args: List[String]): List[BreakClampCandidateScanner.SourceGame] =
    optionString(args, "--corpus")
      .map(path => BreakClampCandidateScanner.readCorpus(Paths.get(path)))
      .getOrElse(Nil)

  private def pgnFileGame(args: List[String]): List[BreakClampCandidateScanner.SourceGame] =
    optionString(args, "--pgn-file").toList.map { path =>
      val id = optionString(args, "--id").getOrElse("pgn-file-break-clamp-triage")
      val label = optionString(args, "--label").getOrElse(id)
      BreakClampCandidateScanner.readPgnFile(Paths.get(path), id = id, label = label)
    }

  private def enginePath(args: List[String]): Option[Path] =
    optionString(args, "--engine")
      .orElse(sys.env.get("STOCKFISH_BIN").map(_.trim).filter(_.nonEmpty))
      .orElse(sys.env.get("AI_ACTIVE_CORPUS_ENGINE_PATH").map(_.trim).filter(_.nonEmpty))
      .map(Paths.get(_))

  private def optionString(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst { case List(flag, value) if flag == name => value }.map(_.trim).filter(_.nonEmpty)

  private def optionSet(args: List[String], name: String): Set[String] =
    optionString(args, name)
      .map(_.split(",").toList.map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)
