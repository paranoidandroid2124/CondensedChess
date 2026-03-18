package lila.accountintel

import chess.Color

import java.time.Instant

import lila.accountintel.AccountIntel.*
import lila.accountintel.dossier.NotebookDossierAssembler
import play.api.libs.json.JsObject

class NotebookDossierAssemblerTest extends munit.FunSuite:

  private def parsed(
      id: String,
      color: Color,
      result: SubjectResult,
      opening: String,
      subjectName: String = "ych24"
  ) =
    ParsedGame(
      external = ExternalGame(
        "chesscom",
        id,
        "2026-03-17 00:00",
        if color.white then subjectName else "opp",
        if color.black then subjectName else "opp",
        result match
          case SubjectResult.Win => if color.white then "1-0" else "0-1"
          case SubjectResult.Draw => "1/2-1/2"
          case SubjectResult.Loss => if color.white then "0-1" else "1-0",
        None,
        "pgn"
      ),
      subjectName = subjectName,
      subjectColor = color,
      subjectResult = result,
      openingName = opening,
      openingFamily = opening,
      openingBucket = if color.white then s"chose $opening" else s"faced $opening",
      openingRelation = if color.white then "chose" else "faced",
      canonicalEcoCode = None,
      providerOpeningName = Some(opening),
      providerEcoCode = None,
      providerEcoUrl = None,
      labels = List("structure"),
      plyCount = 24,
      rep = None
    )

  private def candidate(
      gameId: String,
      side: Color,
      structure: String,
      trigger: String,
      scoreBias: Double,
      result: SubjectResult,
      collapseBacked: Boolean = false
  ) =
    DecisionSnapshotCandidate(
      gameId = gameId,
      triggerType = trigger,
      side = side,
      openingFamily = if side.white then "Queen's Gambit Declined" else "Sicilian Defense",
      structureFamily = structure,
      labels = List("structure"),
      ply = 15,
      fen = s"fen-$gameId",
      quiet = true,
      playedUci = "d4d5",
      explainabilityScore = 0.75 + scoreBias,
      preventabilityScore = 0.74 + scoreBias,
      branchingScore = 0.73 + scoreBias,
      snapshotConfidence = 0.72 + scoreBias,
      commitmentScore = 0.7 + scoreBias,
      collapseBacked = collapseBacked,
      transitionType = Some("ForcedPivot"),
      planAlignmentBand = Some("OffPlan"),
      earliestPreventablePly = Option.when(collapseBacked)(13),
      windowStartPly = 13,
      windowEndPly = 18,
      repeatabilityKey =
        s"${colorKey(side)}|${slug(structure)}|$trigger|forcedpivot|offplan",
      game = parsed(gameId, side, result, if side.white then "Queen's Gambit Declined" else "Sicilian Defense"),
      lastSan = Some("move")
    )

  private def clusterOf(
      id: String,
      side: Color,
      structure: String,
      priority: Double,
      supportCandidates: List[DecisionSnapshotCandidate]
  ) =
    SnapshotCluster(
      id = id,
      side = side,
      openingFamily = supportCandidates.head.openingFamily,
      structureFamily = structure,
      triggerType = supportCandidates.head.triggerType,
      labels = List("structure"),
      priorityScore = priority,
      priorityBreakdown = PriorityBreakdown(
        supportScore = 1,
        repeatabilityScore = 0.5,
        snapshotScore = 0.8,
        preventabilityScore = 0.7,
        branchingScore = 0.7,
        repairImpactScore = 0.6,
        readinessBonus = 0.2,
        triggerPenalty = 0,
        redundancyPenalty = 0
      ),
      distinctGames = supportCandidates.map(_.gameId).distinct.size,
      distinctOpenings = supportCandidates.map(_.openingFamily).distinct.size,
      collapseBackedRate = supportCandidates.count(_.collapseBacked).toDouble / supportCandidates.size.toDouble,
      offPlanRate = 1.0,
      quietRate = 1.0,
      triggerDiversity = supportCandidates.map(_.triggerType).distinct.size.toDouble,
      snapshotConfidenceMean = supportCandidates.map(_.snapshotConfidence).sum / supportCandidates.size.toDouble,
      exemplarCentrality = 0.8,
      nonContinuationRate = 1.0,
      averageExplainability = 0.8,
      averagePreventability = 0.8,
      averageBranching = 0.8,
      resultImpactScore = 0.8,
      earliestPreventableRate =
        supportCandidates.count(_.earliestPreventablePly.isDefined).toDouble / supportCandidates.size.toDouble,
      candidates = supportCandidates,
      exemplar = ClusterExemplar(supportCandidates.head.game, supportCandidates.head.some)
    )

  test("my account notebook selection preserves side coverage before duplicate side clusters"):
    val whiteA = candidate("w-a", Color.White, "Carlsbad", "pawn_structure_mutation", 0.1, SubjectResult.Loss)
    val whiteB = candidate("w-b", Color.White, "IQP", "tension_release", 0.05, SubjectResult.Draw)
    val blackA = candidate("b-a", Color.Black, "Najdorf Hedgehog", "pawn_structure_mutation", 0.02, SubjectResult.Loss)
    val blackB = candidate("b-b", Color.Black, "Scheveningen", "tension_release", 0.01, SubjectResult.Draw)

    val clusters = List(
      clusterOf("white-a", Color.White, "Carlsbad", 5.2, List(whiteA, whiteA.copy(gameId = "w-a-2", game = parsed("w-a-2", Color.White, SubjectResult.Loss, "Queen's Gambit Declined")))),
      clusterOf("white-b", Color.White, "IQP", 4.9, List(whiteB, whiteB.copy(gameId = "w-b-2", game = parsed("w-b-2", Color.White, SubjectResult.Draw, "Queen's Gambit Declined")))),
      clusterOf("black-a", Color.Black, "Najdorf Hedgehog", 4.3, List(blackA, blackA.copy(gameId = "b-a-2", game = parsed("b-a-2", Color.Black, SubjectResult.Loss, "Sicilian Defense")))),
      clusterOf("black-b", Color.Black, "Scheveningen", 4.0, List(blackB, blackB.copy(gameId = "b-b-2", game = parsed("b-b-2", Color.Black, SubjectResult.Draw, "Sicilian Defense"))))
    )

    val selected =
      NotebookDossierAssembler.selectClustersForNotebook(ProductKind.MyAccountIntelligenceLite, clusters)

    assertEquals(selected.size, 3)
    assertEquals(selected.take(2).map(_.side).toSet, Set(Color.White, Color.Black))
    assertEquals(selected.head.id, "white-a")
    assertEquals(selected(1).id, "black-a")

  test("opening wording reflects faced/chose perspective for defense structures"):
    val bundle = PrimitiveBundle(
      parsedGames = List(
        parsed("w1", Color.White, SubjectResult.Loss, "Kings Indian Defense").copy(
          openingBucket = "faced Kings Indian Defense",
          openingRelation = "faced"
        ),
        parsed("w2", Color.White, SubjectResult.Draw, "Kings Indian Defense").copy(
          openingBucket = "faced Kings Indian Defense",
          openingRelation = "faced"
        ),
        parsed("w3", Color.White, SubjectResult.Loss, "Kings Indian Defense").copy(
          openingBucket = "faced Kings Indian Defense",
          openingRelation = "faced"
        ),
        parsed("b1", Color.Black, SubjectResult.Loss, "Petrovs Defense").copy(
          openingBucket = "chose Petrovs Defense",
          openingRelation = "chose"
        ),
        parsed("b2", Color.Black, SubjectResult.Draw, "Petrovs Defense").copy(
          openingBucket = "chose Petrovs Defense",
          openingRelation = "chose"
        ),
        parsed("b3", Color.Black, SubjectResult.Loss, "Petrovs Defense").copy(
          openingBucket = "chose Petrovs Defense",
          openingRelation = "chose"
        )
      ),
      featureRows = Nil,
      sampledGameCount = 6,
      eligibleGameCount = 6,
      warnings = Nil
    )

    val dossier = NotebookDossierAssembler
      .assemble(
        provider = "chesscom",
        username = "ych24",
        kind = ProductKind.MyAccountIntelligenceLite,
        bundle = bundle,
        selectedClusters = Nil,
        allClusters = Nil,
        requestedGameLimit = 6,
        generatedAt = Instant.parse("2026-03-17T00:00:00Z")
      )
      .toOption
      .get
      .dossier

    val overviewCards = (dossier \ "overview" \ "cards").as[List[JsObject]]
    val openingHeadline = (overviewCards.head \ "headline").as[String]
    val openingCards =
      (dossier \ "sections")
        .as[List[JsObject]]
        .find(_("kind").as[String] == "opening_map")
        .map(_("cards").as[List[JsObject]])
        .getOrElse(Nil)
    val whiteCard = openingCards.find(_("side").as[String] == "white").get

    assert(openingHeadline.contains("faced Kings Indian Defense with White"))
    assert(!openingHeadline.contains("Kings Indian Defense as White"))
    assertEquals((whiteCard \ "title").as[String], "White: faced Kings Indian Defense")
    assert((whiteCard \ "story").as[String].contains("facing Kings Indian Defense"))

  test("collapse-backed anchors talk about the earlier quiet decision window"):
    val collapseCandidate = candidate("w-collapse", Color.White, "White IQP", "tension_release", 0.1, SubjectResult.Loss, collapseBacked = true)
    val companion =
      collapseCandidate.copy(gameId = "w-collapse-2", game = parsed("w-collapse-2", Color.White, SubjectResult.Draw, "Queen's Gambit Declined"))
    val cluster = clusterOf("white-iqp", Color.White, "White IQP", 5.1, List(collapseCandidate, companion))
    val bundle = PrimitiveBundle(
      parsedGames = List(collapseCandidate.game, companion.game),
      featureRows = Nil,
      sampledGameCount = 2,
      eligibleGameCount = 2,
      warnings = List("snapshot confirmed heuristically")
    )

    val dossier = NotebookDossierAssembler
      .assemble(
        provider = "chesscom",
        username = "ych24",
        kind = ProductKind.MyAccountIntelligenceLite,
        bundle = bundle,
        selectedClusters = List(cluster),
        allClusters = List(cluster),
        requestedGameLimit = 2,
        generatedAt = Instant.parse("2026-03-17T00:00:00Z")
      )
      .toOption
      .get
      .dossier

    val patternSection =
      (dossier \ "sections")
        .as[List[JsObject]]
        .find(_("kind").as[String] == "pattern_cluster")
        .get
    val anchorCard = (patternSection \ "cards").as[List[JsObject]].find(_("cardKind").as[String] == "anchor_position").get

    assert((anchorCard \ "explanation").as[String].contains("earlier quiet decision window"))
    assert((anchorCard \ "questionPrompt").as[String].contains("later mistake"))
    assert((dossier \ "appendix" \ "warnings").as[List[String]].contains("snapshot confirmed heuristically"))

  test("iqp anchors describe the concrete decision, not just a generic clean plan"):
    val iqpCandidate =
      candidate("w-iqp", Color.White, "White IQP", "tension_release", 0.05, SubjectResult.Loss)
    val companion =
      iqpCandidate.copy(gameId = "w-iqp-2", game = parsed("w-iqp-2", Color.White, SubjectResult.Draw, "Queen's Gambit Declined"))
    val third =
      iqpCandidate.copy(gameId = "w-iqp-3", game = parsed("w-iqp-3", Color.White, SubjectResult.Draw, "Queen's Gambit Declined"))
    val cluster = clusterOf("white-iqp", Color.White, "White IQP", 5.0, List(iqpCandidate, companion, third))
    val bundle = PrimitiveBundle(
      parsedGames = List(iqpCandidate.game, companion.game, third.game),
      featureRows = Nil,
      sampledGameCount = 3,
      eligibleGameCount = 3,
      warnings = Nil
    )

    val dossier = NotebookDossierAssembler
      .assemble(
        provider = "chesscom",
        username = "ych24",
        kind = ProductKind.MyAccountIntelligenceLite,
        bundle = bundle,
        selectedClusters = List(cluster),
        allClusters = List(cluster),
        requestedGameLimit = 3,
        generatedAt = Instant.parse("2026-03-17T00:00:00Z")
      )
      .toOption
      .get
      .dossier

    val patternSection =
      (dossier \ "sections")
        .as[List[JsObject]]
        .find(_("kind").as[String] == "pattern_cluster")
        .get
    val anchorCard = (patternSection \ "cards").as[List[JsObject]].find(_("cardKind").as[String] == "anchor_position").get
    val summary = (patternSection \ "summary").as[String]

    assert(summary.contains("clarify the center"))
    assert((anchorCard \ "claim").as[String].contains("clarify the center"))
    assert((anchorCard \ "recommendedPlan" \ "summary").as[String].contains("clarify the center"))
