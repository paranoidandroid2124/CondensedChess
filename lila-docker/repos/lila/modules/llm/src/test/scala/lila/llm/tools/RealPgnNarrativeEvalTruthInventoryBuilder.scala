package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant

import play.api.libs.json.*

object RealPgnNarrativeEvalTruthInventoryBuilder:

  private val DefaultManifestRoot =
    CommentaryPlayerQcSupport.DefaultManifestDir.resolve("master_only_truth_reboot_v1_20260324_2344")
  private val DefaultOutputPath = CommentaryPlayerQcSupport.DefaultTruthInventoryPath
  private val SupplementalSourceCorpusCandidates = CommentaryPlayerQcSupport.TruthInventoryLookupPaths
  private val PracticalErrorCpLossThreshold = 70
  private val SevereErrorCpLossThreshold = 180
  private val ExplicitProblemMomentTypes = Set("InvestmentPivot", "Blunder", "MatePivot")
  private val ExplicitProblemClassifications = Set("Blunder", "MissedWin", "CompensatedInvestment")

  object InventoryTag:
    val PositiveExemplarGate = "positive_exemplar_gate"
    val NegativeGuard = "negative_guard"
    val ExplicitProblemClassification = "explicit_problem_classification"
    val ExplicitProblemMoment = "explicit_problem_moment"
    val CompensatedInvestment = "compensated_investment"
    val MissedWin = "missed_win"
    val BlunderClassified = "blunder_classified"
    val InvestmentPivot = "investment_pivot"
    val BlunderMoment = "blunder_moment"
    val MatePivot = "mate_pivot"
    val PracticalError = "practical_error"
    val SevereError = "severe_error"
    val HighCpLossUnclassified = "high_cp_loss_unclassified"

  final case class InventorySelection(
      practicalErrorCpLossThreshold: Int,
      severeErrorCpLossThreshold: Int,
      explicitProblemMomentTypes: List[String],
      explicitProblemClassifications: List[String],
      includesCanonicalPositiveExemplarGate: Boolean,
      includesNegativeGuards: Boolean
  )
  object InventorySelection:
    given OFormat[InventorySelection] = Json.format[InventorySelection]

  final case class InventorySummary(
      totalEntries: Int,
      totalGames: Int,
      moveClassificationCounts: Map[String, Int],
      momentTypeCounts: Map[String, Int],
      diagnosticTagCounts: Map[String, Int],
      explicitProblemClassifiedCount: Int,
      explicitProblemMomentCount: Int,
      highCpLossCount: Int,
      highCpLossUnclassifiedCount: Int
  )
  object InventorySummary:
    given OFormat[InventorySummary] = Json.format[InventorySummary]

  final case class TruthInventoryEntry(
      key: String,
      gameId: String,
      ply: Int,
      moveNumber: Int,
      side: String,
      tier: String,
      family: String,
      label: String,
      momentType: String,
      moveClassification: Option[String],
      cpLossVsPlayed: Option[Int],
      tags: List[String],
      notes: List[String]
  )
  object TruthInventoryEntry:
    given OFormat[TruthInventoryEntry] = Json.format[TruthInventoryEntry]

  final case class TruthInventory(
      version: Int = 1,
      generatedAt: String,
      asOfDate: String,
      title: String,
      description: String,
      sourceManifestRoot: String,
      selection: InventorySelection,
      summary: InventorySummary,
      games: List[RealPgnNarrativeEvalRunner.CorpusGame],
      entries: List[TruthInventoryEntry]
  )
  object TruthInventory:
    given OFormat[TruthInventory] = Json.format[TruthInventory]

  private[tools] final case class RawMomentEntry(
      key: String,
      gameId: String,
      ply: Int,
      moveNumber: Int,
      side: String,
      momentType: String,
      moveClassification: Option[String],
      cpLossVsPlayed: Option[Int]
  )

  private[tools] final case class RawTopEngineMove(cpLossVsPlayed: Option[Int])
  private[tools] object RawTopEngineMove:
    given Reads[RawTopEngineMove] = Json.reads[RawTopEngineMove]

  private[tools] final case class RawMoment(
      ply: Int,
      moveNumber: Int,
      side: String,
      momentType: String,
      moveClassification: Option[String],
      topEngineMove: Option[RawTopEngineMove]
  )
  private[tools] object RawMoment:
    given Reads[RawMoment] = Json.reads[RawMoment]

  private[tools] final case class RawGameArc(moments: List[RawMoment])
  private[tools] object RawGameArc:
    given Reads[RawGameArc] = Json.reads[RawGameArc]

  def main(args: Array[String]): Unit =
    val outputPath =
      args.headOption
        .map(path => Paths.get(path).toAbsolutePath.normalize)
        .getOrElse(DefaultOutputPath.toAbsolutePath.normalize)
    val manifestRoot =
      args.lift(1)
        .map(path => Paths.get(path).toAbsolutePath.normalize)
        .getOrElse(DefaultManifestRoot.toAbsolutePath.normalize)
    val inventory = buildInventoryFromManifest(manifestRoot = manifestRoot)
    val parent = outputPath.getParent
    if parent != null then Files.createDirectories(parent)
    Files.writeString(outputPath, Json.prettyPrint(Json.toJson(inventory)), StandardCharsets.UTF_8)
    println(
      s"[truth-inventory-builder] wrote `${outputPath}` from `${manifestRoot}` (entries=${inventory.entries.size})"
    )

  private[tools] def buildInventoryFromManifest(
      manifestRoot: Path,
      generatedAt: Instant = Instant.now()
  ): TruthInventory =
    require(Files.isDirectory(manifestRoot), s"truth inventory manifest root not found: $manifestRoot")
    val sourceGames =
      (readSourceGames(resolveSourceCorpusPath(manifestRoot)) ++
        readSupplementalSourceGames())
        .sortBy(_.id)
        .distinctBy(_.id)
    val rawMoments = readRawMoments(manifestRoot)
    buildInventory(
      sourceGames = sourceGames,
      rawMoments = rawMoments,
      manifestRoot = manifestRoot,
      generatedAt = generatedAt
    )

  private[tools] def buildInventory(
      sourceGames: List[RealPgnNarrativeEvalRunner.CorpusGame],
      rawMoments: List[RawMomentEntry],
      manifestRoot: Path = DefaultManifestRoot,
      generatedAt: Instant = Instant.now()
  ): TruthInventory =
    val sourceGamesById = sourceGames.groupBy(_.id).view.mapValues(_.head).toMap
    val uniqueMoments = rawMoments.sortBy(_.key).distinctBy(_.key)
    val includedMoments = uniqueMoments.filter(shouldInclude)
    val entries =
      includedMoments.map { moment =>
        buildInventoryEntry(
          moment = moment,
          game = sourceGamesById.getOrElse(
            moment.gameId,
            throw new IllegalArgumentException(s"missing source corpus game for inventory entry `${moment.gameId}`")
          )
        )
      }
    val guardGames =
      RealPgnNarrativeEvalRunner.NegativeGuards.map(guard =>
        RealPgnNarrativeEvalRunner.CorpusGame(
          id = guard.id,
          tier = "negative_guard",
          family = guard.family,
          label = guard.label,
          notes = List("Canonical negative guard carried over from the current signoff runner."),
          expectedThemes = List(guard.family),
          pgn = guard.pgn
        )
      )
    val guardEntries = RealPgnNarrativeEvalRunner.NegativeGuards.map(buildNegativeGuardEntry)
    val allEntries = (entries ++ guardEntries).sortBy(entry => (entry.gameId, entry.ply, entry.key))
    val gameIds = allEntries.map(_.gameId).distinct
    val games =
      gameIds.flatMap(gameId => sourceGamesById.get(gameId).orElse(guardGames.find(_.id == gameId)))
    val selection =
      InventorySelection(
        practicalErrorCpLossThreshold = PracticalErrorCpLossThreshold,
        severeErrorCpLossThreshold = SevereErrorCpLossThreshold,
        explicitProblemMomentTypes = ExplicitProblemMomentTypes.toList.sorted,
        explicitProblemClassifications = ExplicitProblemClassifications.toList.sorted,
        includesCanonicalPositiveExemplarGate = true,
        includesNegativeGuards = true
      )
    val summary =
      InventorySummary(
        totalEntries = allEntries.size,
        totalGames = games.size,
        moveClassificationCounts =
          allEntries.flatMap(_.moveClassification.toList).groupBy(identity).view.mapValues(_.size).toMap,
        momentTypeCounts = allEntries.map(_.momentType).groupBy(identity).view.mapValues(_.size).toMap,
        diagnosticTagCounts = allEntries.flatMap(_.tags).groupBy(identity).view.mapValues(_.size).toMap,
        explicitProblemClassifiedCount =
          allEntries.count(_.tags.contains(InventoryTag.ExplicitProblemClassification)),
        explicitProblemMomentCount =
          allEntries.count(_.tags.contains(InventoryTag.ExplicitProblemMoment)),
        highCpLossCount =
          allEntries.count(entry =>
            entry.tags.contains(InventoryTag.PracticalError) || entry.tags.contains(InventoryTag.SevereError)
          ),
        highCpLossUnclassifiedCount =
          allEntries.count(_.tags.contains(InventoryTag.HighCpLossUnclassified))
      )

    TruthInventory(
      generatedAt = generatedAt.toString,
      asOfDate = generatedAt.toString.take(10),
      title = "Real PGN Truth Inventory",
      description =
        "Exhaustive move-level truth inventory cut from the master-140 engine-evaluated raw chronicle outputs. It includes every canonical positive exemplar, every explicit problem classification, every explicit problem moment type, every move with cp-loss at or above the practical-error threshold, and the canonical negative guard.",
      sourceManifestRoot = manifestRoot.toAbsolutePath.normalize.toString,
      selection = selection,
      summary = summary,
      games = games,
      entries = allEntries
    )

  private def shouldInclude(moment: RawMomentEntry): Boolean =
    RealPgnNarrativeEvalRunner.PositiveCompensationExemplars.contains(moment.key) ||
      moment.moveClassification.exists(ExplicitProblemClassifications.contains) ||
      ExplicitProblemMomentTypes.contains(moment.momentType) ||
      moment.cpLossVsPlayed.exists(_ >= PracticalErrorCpLossThreshold)

  private def buildInventoryEntry(
      moment: RawMomentEntry,
      game: RealPgnNarrativeEvalRunner.CorpusGame
  ): TruthInventoryEntry =
    val tags = deriveTags(moment)
    TruthInventoryEntry(
      key = moment.key,
      gameId = moment.gameId,
      ply = moment.ply,
      moveNumber = moment.moveNumber,
      side = moment.side,
      tier = game.tier,
      family = game.family,
      label = game.label,
      momentType = moment.momentType,
      moveClassification = moment.moveClassification,
      cpLossVsPlayed = moment.cpLossVsPlayed,
      tags = tags,
      notes = notesFor(moment, tags)
    )

  private def buildNegativeGuardEntry(
      guard: RealPgnNarrativeEvalRunner.NegativeGuardSpec
  ): TruthInventoryEntry =
    TruthInventoryEntry(
      key = s"${guard.id}:${guard.targetPly}",
      gameId = guard.id,
      ply = guard.targetPly,
      moveNumber = (guard.targetPly + 1) / 2,
      side = if guard.targetPly % 2 == 1 then "white" else "black",
      tier = "negative_guard",
      family = guard.family,
      label = guard.label,
      momentType = "NegativeGuard",
      moveClassification = None,
      cpLossVsPlayed = None,
      tags = List(InventoryTag.NegativeGuard),
      notes = List("Canonical negative guard carried over from the current signoff runner.")
    )

  private def deriveTags(moment: RawMomentEntry): List[String] =
    List(
      Option.when(RealPgnNarrativeEvalRunner.PositiveCompensationExemplars.contains(moment.key))(InventoryTag.PositiveExemplarGate),
      Option.when(moment.moveClassification.exists(ExplicitProblemClassifications.contains))(InventoryTag.ExplicitProblemClassification),
      Option.when(ExplicitProblemMomentTypes.contains(moment.momentType))(InventoryTag.ExplicitProblemMoment),
      Option.when(moment.moveClassification.contains("CompensatedInvestment"))(InventoryTag.CompensatedInvestment),
      Option.when(moment.moveClassification.contains("MissedWin"))(InventoryTag.MissedWin),
      Option.when(moment.moveClassification.contains("Blunder"))(InventoryTag.BlunderClassified),
      Option.when(moment.momentType == "InvestmentPivot")(InventoryTag.InvestmentPivot),
      Option.when(moment.momentType == "Blunder")(InventoryTag.BlunderMoment),
      Option.when(moment.momentType == "MatePivot")(InventoryTag.MatePivot),
      Option.when(moment.cpLossVsPlayed.exists(loss => loss >= PracticalErrorCpLossThreshold && loss < SevereErrorCpLossThreshold))(InventoryTag.PracticalError),
      Option.when(moment.cpLossVsPlayed.exists(_ >= SevereErrorCpLossThreshold))(InventoryTag.SevereError),
      Option.when(
        moment.cpLossVsPlayed.exists(_ >= PracticalErrorCpLossThreshold) && moment.moveClassification.isEmpty
      )(InventoryTag.HighCpLossUnclassified)
    ).flatten

  private def notesFor(moment: RawMomentEntry, tags: List[String]): List[String] =
    List(
      Option.when(tags.contains(InventoryTag.PositiveExemplarGate))(
        "Canonical exemplar-gate inventory entry from the audited positive-exemplar set."
      ),
      Option.when(tags.contains(InventoryTag.CompensatedInvestment))(
        "Raw chronicle classified this move as CompensatedInvestment."
      ),
      Option.when(tags.contains(InventoryTag.MissedWin))(
        "Raw chronicle classified this move as MissedWin."
      ),
      Option.when(tags.contains(InventoryTag.BlunderClassified))(
        "Raw chronicle classified this move as Blunder."
      ),
      Option.when(tags.contains(InventoryTag.HighCpLossUnclassified))(
        "Engine cp-loss reached the practical threshold even though the raw moveClassification is empty."
      ),
      moment.cpLossVsPlayed.map(loss => s"Engine cp-loss versus the played move: $loss."),
      Some(s"Raw chronicle momentType: ${moment.momentType}.")
    ).flatten

  private def readSourceGames(path: Path): List[RealPgnNarrativeEvalRunner.CorpusGame] =
    require(Files.isRegularFile(path), s"truth inventory source corpus not found: $path")
    val js = Json.parse(readUtf8NoBom(path))
    val games =
      js.validate[TruthInventory].asOpt
        .map(_.games)
        .orElse(
          js.validate[List[RealPgnNarrativeEvalRunner.Corpus]].asOpt
            .orElse(js.validate[RealPgnNarrativeEvalRunner.Corpus].asOpt.map(List(_)))
            .map(_.flatMap(_.games))
        )
        .getOrElse(throw new IllegalArgumentException(s"truth inventory source corpus has unexpected JSON shape: $path"))
    games.sortBy(_.id).distinctBy(_.id)

  private def readSupplementalSourceGames(): List[RealPgnNarrativeEvalRunner.CorpusGame] =
    SupplementalSourceCorpusCandidates
      .map(_.toAbsolutePath.normalize)
      .find(Files.isRegularFile(_))
      .map(readSourceGames)
      .getOrElse(Nil)

  private def readRawMoments(manifestRoot: Path): List[RawMomentEntry] =
    val rawRoot = manifestRoot.resolve("runs")
    require(Files.isDirectory(rawRoot), s"truth inventory raw run root not found: $rawRoot")
    import scala.jdk.CollectionConverters.*
    val stream = Files.walk(rawRoot)
    try
      stream
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && path.getFileName.toString.endsWith(".game_arc.json"))
        .toList
        .flatMap(readRawMomentFile)
    finally stream.close()

  private def readRawMomentFile(path: Path): List[RawMomentEntry] =
    val gameId = stripGameArcSuffix(path.getFileName.toString)
    Json
      .parse(readUtf8NoBom(path))
      .as[RawGameArc]
      .moments
      .map(moment =>
        RawMomentEntry(
          key = s"$gameId:${moment.ply}",
          gameId = gameId,
          ply = moment.ply,
          moveNumber = moment.moveNumber,
          side = moment.side,
          momentType = moment.momentType,
          moveClassification = moment.moveClassification.filter(_.trim.nonEmpty),
          cpLossVsPlayed = moment.topEngineMove.flatMap(_.cpLossVsPlayed)
        )
      )

  private def resolveSourceCorpusPath(manifestRoot: Path): Path =
    val direct = manifestRoot.resolve("chronicle_corpus_master_only_140.json")
    if Files.isRegularFile(direct) then direct
    else throw new IllegalArgumentException(s"truth inventory source corpus not found under manifest root: $manifestRoot")

  private def stripGameArcSuffix(fileName: String): String =
    fileName.stripSuffix(".game_arc.json")

  private def readUtf8NoBom(path: Path): String =
    Files.readString(path, StandardCharsets.UTF_8).stripPrefix("\uFEFF")
