package lila.commentary.tools.claim

import java.nio.file.{ Path, Paths }

import lila.commentary.model.strategic.VariationLine
import lila.commentary.tools.realpgn.RealPgnNarrativeEvalRunner

object AdmissionUnitReviewRunner:

  private final class LocalEngine(engine: RealPgnNarrativeEvalRunner.LocalUciEngine)
      extends SourceReview.SourceReviewEngine:
    override def newGame(): Unit = engine.newGame()
    override def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] =
      engine.analyze(fen, depth, multiPv)

  @main def runAdmissionUnitReview(args: String*): Unit =
    val argList = args.toList
    val planKind = optionString(argList, "--plan-kind").getOrElse("prophylaxis_restraint")
    val depth = optionInt(argList, "--depth").getOrElse(16)
    val multiPv = optionInt(argList, "--multi-pv").orElse(optionInt(argList, "--multiPv")).getOrElse(3)
    val nearTopCp = optionInt(argList, "--near-top-cp").getOrElse(50)
    val offset = optionInt(argList, "--offset").getOrElse(0)
    val limit = optionInt(argList, "--limit")
    val maxCandidates = optionInt(argList, "--max-candidates").getOrElse(24)
    val admitLimit = optionInt(argList, "--admit-limit").getOrElse(1)
    val sourceUrl =
      optionString(argList, "--source-url")
        .getOrElse("https://www.pgnmentor.com/openings/ModernBenoni6e4.zip")
    val selectedIds = optionSet(argList, "--ids")
    val games =
      sourceCatalogGames(argList, selectedIds) ++
        corpusGames(argList, sourceUrl) ++
        pgnFileGames(argList, sourceUrl)
    if games.isEmpty then
      System.err.println(
        "[admission-unit-review] no input games; provide --corpus, --pgn-file with --id/--label, or --source-catalog"
      )
      sys.exit(1)

    val config =
      AdmissionUnitReview.AdmissionConfig(
        planKind = planKind,
        depth = depth,
        multiPv = multiPv,
        nearTopCp = nearTopCp,
        offset = offset,
        limit = limit,
        maxCandidates = maxCandidates,
        admitLimit = admitLimit,
        sourceUrl = sourceUrl
      )
    val maybeEnginePath = enginePath(argList)
    if maybeEnginePath.isEmpty then
      System.err.println("[admission-unit-review] missing engine path; rows will be engine-blocked")
    val report =
      maybeEnginePath match
        case None =>
          AdmissionUnitReview.admit(games, engine = None, config = config)
        case Some(path) =>
          val engine = RealPgnNarrativeEvalRunner.LocalUciEngine(path, timeoutMs = 30000L)
          try AdmissionUnitReview.admit(games, engine = Some(LocalEngine(engine)), config = config)
          finally engine.close()
    val (matrix, review) = AdmissionUnitReview.writeArtifacts(report)
    println(report.tsv)
    println(s"wrote=${matrix.toAbsolutePath}")
    println(s"review=${review.toAbsolutePath}")
    println(report.markdown.linesIterator.take(12).mkString("\n"))

  private def sourceCatalogGames(
      args: List[String],
      ids: Set[String]
  ): List[AdmissionUnitReview.SourceGame] =
    if !args.contains("--source-catalog") then Nil
    else
      SourceWitnessCatalog.all
        .filter(source => ids.isEmpty || ids.contains(source.id))
        .map(source =>
          AdmissionUnitReview.SourceGame(
            id = source.id,
            label = source.gameName,
            pgn = source.pgn,
            sourceUrl = source.sourceUrl,
            plyRange = Some(source.candidatePlyRange.start -> source.candidatePlyRange.end)
          )
        )

  private def corpusGames(
      args: List[String],
      sourceUrl: String
  ): List[AdmissionUnitReview.SourceGame] =
    optionString(args, "--corpus")
      .map(path => AdmissionUnitReview.readCorpus(Paths.get(path), sourceUrl))
      .getOrElse(Nil)

  private def pgnFileGames(
      args: List[String],
      sourceUrl: String
  ): List[AdmissionUnitReview.SourceGame] =
    optionString(args, "--pgn-file").toList.flatMap { path =>
      val id = optionString(args, "--id").getOrElse("pgn-file-admission-unit-review")
      val label = optionString(args, "--label").getOrElse(id)
      AdmissionUnitReview.readPgnFile(Paths.get(path), id = id, label = label, sourceUrl = sourceUrl)
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
