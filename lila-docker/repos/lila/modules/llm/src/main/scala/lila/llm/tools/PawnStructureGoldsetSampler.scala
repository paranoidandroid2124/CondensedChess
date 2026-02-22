package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import scala.jdk.CollectionConverters.*

import chess.format.Fen
import chess.variant.Standard
import lila.llm.PgnAnalysisHelper
import lila.llm.analysis.PositionAnalyzer
import lila.llm.analysis.structure.PawnStructureClassifier
import lila.llm.model.structure.{ StructureGoldRow, StructureId }
import play.api.libs.json.Json

/**
 * Generate deduplicated candidate rows for manual pawn-structure labeling.
 *
 * Usage:
 *   sbt "llm/runMain lila.llm.tools.PawnStructureGoldsetSampler"
 *   sbt "llm/runMain lila.llm.tools.PawnStructureGoldsetSampler --out=modules/llm/docs/PawnStructureCandidates.jsonl --limit=600"
 */
object PawnStructureGoldsetSampler:

  private final case class Config(
      docsDir: Path,
      outPath: Path,
      limit: Int,
      minConfidence: Double
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[goldset-sampler] $err")
        sys.exit(2)
      case Right(cfg) =>
        val pgnFiles = listPgnFiles(cfg.docsDir)
        if pgnFiles.isEmpty then
          System.err.println(s"[goldset-sampler] no .pgn files found under ${cfg.docsDir}")
          sys.exit(2)

        val candidates = pgnFiles
          .flatMap(readGames)
          .flatMap { case (gameId, gamePgn) => sampleGame(gameId, gamePgn, cfg.minConfidence) }
          .groupBy(row => normalizeFen(row.fen))
          .values
          .map(_.head)
          .take(cfg.limit)
          .toList
          .sortBy(_.id)

        writeJsonl(cfg.outPath, candidates)
        println(s"[goldset-sampler] wrote ${candidates.size} candidates to ${cfg.outPath}")

  private def parseArgs(args: List[String]): Either[String, Config] =
    val defaults = Config(
      docsDir = Paths.get("modules/llm/docs"),
      outPath = Paths.get("modules/llm/docs/PawnStructureCandidates.jsonl"),
      limit = 1200,
      minConfidence = 0.72
    )

    args.foldLeft[Either[String, Config]](Right(defaults)) {
      case (Left(err), _) => Left(err)
      case (Right(cfg), arg) if arg.startsWith("--docs=") =>
        Right(cfg.copy(docsDir = Paths.get(arg.stripPrefix("--docs="))))
      case (Right(cfg), arg) if arg.startsWith("--out=") =>
        Right(cfg.copy(outPath = Paths.get(arg.stripPrefix("--out="))))
      case (Right(cfg), arg) if arg.startsWith("--limit=") =>
        arg.stripPrefix("--limit=").toIntOption
          .filter(_ > 0)
          .toRight(s"invalid --limit: $arg")
          .map(v => cfg.copy(limit = v))
      case (Right(cfg), arg) if arg.startsWith("--min-confidence=") =>
        arg.stripPrefix("--min-confidence=").toDoubleOption
          .filter(v => v > 0.0 && v <= 1.0)
          .toRight(s"invalid --min-confidence: $arg")
          .map(v => cfg.copy(minConfidence = v))
      case (Right(_), arg) =>
        Left(s"unknown argument: $arg")
    }

  private def listPgnFiles(docsDir: Path): List[Path] =
    if !Files.exists(docsDir) then Nil
    else
      val stream = Files.walk(docsDir)
      try stream.iterator().asScala.filter(p => Files.isRegularFile(p) && p.toString.toLowerCase.endsWith(".pgn")).toList
      finally stream.close()

  private def readGames(path: Path): List[(String, String)] =
    val raw = Files.readString(path, StandardCharsets.UTF_8).replace("\uFEFF", "").trim
    if raw.isEmpty then Nil
    else
      raw
        .split("(?m)(?=^\\[Event\\s+\")")
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .zipWithIndex
        .map { case (gamePgn, idx) =>
          val gameId = s"${path.getFileName.toString.replace(".pgn", "")}-${idx + 1}"
          gameId -> gamePgn
        }

  private def sampleGame(
      gameId: String,
      pgn: String,
      minConfidence: Double
  ): List[StructureGoldRow] =
    PgnAnalysisHelper.extractPlyData(pgn).toOption.toList.flatten.flatMap { pd =>
      val prediction = for
        features <- PositionAnalyzer.extractFeatures(pd.fen, pd.ply)
        pos <- Fen.read(Standard, Fen.Full(pd.fen))
      yield PawnStructureClassifier.classify(
        features = features,
        board = pos.board,
        sideToMove = pos.color,
        minConfidence = minConfidence
      )

      prediction.flatMap { profile =>
        Option.when(profile.primary != StructureId.Unknown) {
          StructureGoldRow(
            id = s"$gameId-ply-${pd.ply}",
            fen = pd.fen,
            primary = profile.primary,
            alternatives = profile.alternatives,
            expectedTopPlanIds = Nil,
            seedPv = List(pd.playedUci),
            sourceGameId = Some(gameId),
            sourcePly = Some(pd.ply),
            annotators = Nil,
            adjudicatedBy = None,
            notes = Some("auto-sampled")
          )
        }
      }
    }

  private def normalizeFen(fen: String): String =
    fen.trim.split("\\s+").take(4).mkString(" ")

  private def writeJsonl(path: Path, rows: List[StructureGoldRow]): Unit =
    val parent = path.getParent
    if parent != null then Files.createDirectories(parent)
    val body = rows.map(r => Json.stringify(Json.toJson(r))).mkString("\n")
    Files.writeString(path, body + "\n", StandardCharsets.UTF_8)
