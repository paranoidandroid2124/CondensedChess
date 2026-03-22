package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant

import play.api.libs.json.Json

object RealPgnPositiveCompensationExemplarBuilder:

  private val DefaultSourceCorpusPath = Paths.get("tmp/commentary-player-qc/manifests/chronicle_corpus.json")
  private val DefaultOutputPath = Paths.get("modules/llm/docs/RealPgnNarrativeEvalPositiveCompensationExemplars.json")
  private val DefaultExpectedThemes = List("compensation", "initiative", "durable pressure")

  private[tools] val SelectedMomentKeys: List[String] =
    RealPgnNarrativeEvalRunner.PositiveCompensationExemplars.toList.sorted

  private[tools] val SelectedGameIds: List[String] =
    SelectedMomentKeys.map(_.takeWhile(_ != ':')).distinct

  def main(args: Array[String]): Unit =
    val outputPath =
      args.headOption
        .map(path => Paths.get(path).toAbsolutePath.normalize)
        .getOrElse(DefaultOutputPath.toAbsolutePath.normalize)
    val sourcePath =
      args.lift(1)
        .map(path => Paths.get(path).toAbsolutePath.normalize)
        .getOrElse(DefaultSourceCorpusPath.toAbsolutePath.normalize)
    writeCorpus(outputPath = outputPath, sourcePath = sourcePath)
    println(s"[positive-exemplar-builder] wrote `${outputPath}` from `${sourcePath}`")

  private[tools] def writeCorpus(
      outputPath: Path,
      sourcePath: Path = DefaultSourceCorpusPath.toAbsolutePath.normalize
  ): Path =
    val corpus = buildCorpus(readCorpus(sourcePath))
    val parent = outputPath.getParent
    if parent != null then Files.createDirectories(parent)
    Files.writeString(outputPath, Json.prettyPrint(Json.toJson(corpus)), StandardCharsets.UTF_8)
    outputPath

  private[tools] def buildCorpus(
      sourceCorpus: RealPgnNarrativeEvalRunner.Corpus,
      generatedAt: Instant = Instant.now()
  ): RealPgnNarrativeEvalRunner.Corpus =
    val sourceById = sourceCorpus.games.map(game => game.id -> game).toMap
    val missingGameIds = SelectedGameIds.filterNot(sourceById.contains)
    require(
      missingGameIds.isEmpty,
      s"positive exemplar source corpus is missing selected game ids: ${missingGameIds.mkString(", ")}"
    )
    RealPgnNarrativeEvalRunner.Corpus(
      version = 1,
      generatedAt = generatedAt.toString,
      asOfDate = generatedAt.toString.take(10),
      title = "Positive Compensation Exemplars",
      description =
        "Real-PGN compensation-positive regression corpus carved out of the selected 360-game narrative corpus.",
      games =
        SelectedGameIds.flatMap(sourceById.get).map { game =>
          game.copy(
            tier = "positive_exemplar",
            notes =
              (game.notes ++ List("Real-PGN positive compensation exemplar from the 360 selected corpus.")).distinct,
            expectedThemes =
              if game.expectedThemes.nonEmpty then game.expectedThemes.distinct
              else DefaultExpectedThemes
          )
        }
    )

  private def readCorpus(path: Path): RealPgnNarrativeEvalRunner.Corpus =
    require(Files.isRegularFile(path), s"positive exemplar source corpus not found: $path")
    Json.parse(Files.readString(path, StandardCharsets.UTF_8)).as[RealPgnNarrativeEvalRunner.Corpus]
