package lila.accountintel

import chess.Color

import lila.accountintel.AccountIntel.*
import lila.accountintel.service.{ SelectiveEvalLookup, SelectiveEvalProbe, SelectiveEvalRefiner }

class SelectiveEvalRefinerTest extends munit.FunSuite:

  given Executor = scala.concurrent.ExecutionContext.global

  private def parsed(id: String, color: Color, result: SubjectResult) =
    ParsedGame(
      external = ExternalGame(
        "chesscom",
        id,
        "2026-03-17 00:00",
        if color.white then "ych24" else "opp",
        if color.black then "ych24" else "opp",
        result match
          case SubjectResult.Win => if color.white then "1-0" else "0-1"
          case SubjectResult.Draw => "1/2-1/2"
          case SubjectResult.Loss => if color.white then "0-1" else "1-0",
        none,
        "pgn"
      ),
      subjectName = "ych24",
      subjectColor = color,
      subjectResult = result,
      openingName = if color.white then "Queen's Gambit Declined" else "Sicilian Defense",
      openingFamily = if color.white then "Queen's Gambit Declined" else "Sicilian Defense",
      openingBucket = if color.white then "faced Queen's Gambit Declined" else "faced Sicilian Defense",
      openingRelation = "faced",
      canonicalEcoCode = None,
      providerOpeningName = None,
      providerEcoCode = None,
      providerEcoUrl = None,
      labels = List("structure"),
      plyCount = 30,
      rep = none
    )

  private def row(id: String, color: Color, structure: String, ply: Int, fen: String) =
    SnapshotFeatureRow(
      gameId = id,
      subjectColor = color,
      openingFamily = if color.white then "Queen's Gambit Declined" else "Sicilian Defense",
      structureFamily = structure,
      labels = List("structure"),
      ply = ply,
      fen = fen,
      sideToMove = color,
      quiet = true,
      triggerHints = List("tension_release"),
      playedUci = "d4d5",
      playedSan = "d5",
      explainabilityScore = 0.75,
      preventabilityScore = 0.74,
      branchingScore = 0.73,
      transitionType = Some("ForcedPivot"),
      strategicSalienceHigh = true,
      planAlignmentBand = Some("OffPlan"),
      planIntent = Some("keep central tension"),
      planRisk = Some("overclarifying"),
      hypothesisThemes = List("center"),
      integratedTension = 0.7,
      earliestPreventablePly = none,
      collapseMomentPly = none,
      collapseAnalysis = none,
      analysis = null.asInstanceOf[lila.llm.model.ExtendedAnalysisData],
      game = parsed(id, color, SubjectResult.Loss),
      lastSan = Some("move")
    )

  private def candidate(
      id: String,
      color: Color,
      structure: String,
      ply: Int,
      fen: String,
      playedUci: String,
      snapshotConfidence: Double = 0.62,
      collapseBacked: Boolean = false
  ) =
    DecisionSnapshotCandidate(
      gameId = id,
      triggerType = "tension_release",
      side = color,
      openingFamily = if color.white then "Queen's Gambit Declined" else "Sicilian Defense",
      structureFamily = structure,
      labels = List("structure"),
      ply = ply,
      fen = fen,
      quiet = true,
      playedUci = playedUci,
      explainabilityScore = 0.75,
      preventabilityScore = if collapseBacked then 0.85 else 0.6,
      branchingScore = 0.73,
      snapshotConfidence = snapshotConfidence,
      commitmentScore = 0.7,
      collapseBacked = collapseBacked,
      transitionType = Some("ForcedPivot"),
      planAlignmentBand = Some("OffPlan"),
      earliestPreventablePly = Option.when(collapseBacked)(ply - 2),
      windowStartPly = ply - 2,
      windowEndPly = ply + 2,
      repeatabilityKey = s"${colorKey(color)}|${slug(structure)}|tension_release|forcedpivot|offplan",
      game = parsed(id, color, SubjectResult.Loss),
      lastSan = Some("move")
    )

  private def cluster(
      id: String,
      color: Color,
      structure: String,
      exemplar: DecisionSnapshotCandidate,
      others: List[DecisionSnapshotCandidate],
      collapseBackedRate: Double = 0.0,
      snapshotConfidenceMean: Double = 0.62
  ) =
    SnapshotCluster(
      id = id,
      side = color,
      openingFamily = exemplar.openingFamily,
      structureFamily = structure,
      triggerType = exemplar.triggerType,
      labels = List("structure"),
      priorityScore = 4.8,
      priorityBreakdown = PriorityBreakdown(1, 0.4, 0.7, 0.6, 0.5, 0.45, 0.2, 0, 0),
      distinctGames = (exemplar :: others).map(_.gameId).distinct.size,
      distinctOpenings = (exemplar :: others).map(_.openingFamily).distinct.size,
      collapseBackedRate = collapseBackedRate,
      offPlanRate = 1.0,
      quietRate = 1.0,
      triggerDiversity = 1.0,
      snapshotConfidenceMean = snapshotConfidenceMean,
      exemplarCentrality = 0.8,
      nonContinuationRate = 1.0,
      averageExplainability = 0.75,
      averagePreventability = 0.74,
      averageBranching = 0.73,
      resultImpactScore = 0.8,
      earliestPreventableRate = if collapseBackedRate > 0 then 1.0 else 0.0,
      candidates = exemplar :: others,
      exemplar = ClusterExemplar(exemplar.game, exemplar.some)
    )

  test("selective eval uses cache first and boosts anchor when best move disagrees"):
    val cacheLookup = new SelectiveEvalLookup:
      def lookup(fen: String) =
        fuccess(
          Option.when(fen == "fen-a")(
            SelectiveEvalProbe(cp = Some(80), mate = none[Int], bestMove = Some("g1f3"), source = "cache")
          )
        )
    val requesterLookup = new SelectiveEvalLookup:
      def lookup(fen: String) = fuccess(none[SelectiveEvalProbe])

    val refiner = new SelectiveEvalRefiner(cacheLookup, requesterLookup)
    val anchor = candidate("g1", Color.White, "White IQP", 16, "fen-a", "d4d5")
    val selected = List(cluster("white-iqp", Color.White, "White IQP", anchor, Nil))
    val rows = List(row("g1", Color.White, "White IQP", 14, "fen-a0"), row("g1", Color.White, "White IQP", 16, "fen-a"))

    refiner.refine("chesscom", ProductKind.MyAccountIntelligenceLite, selected, rows).map: result =>
      val refined = result.clusters.head.exemplar.candidate.get
      assert(refined.preventabilityScore >= 0.82)
      assert(refined.earliestPreventablePly.contains(14))
      assert(refined.snapshotConfidence > anchor.snapshotConfidence)
      assert(refined.collapseBacked)
      assert(result.clusters.head.collapseBackedRate > 0d)
      assertEquals(result.warnings, Nil)

  test("refinement only targets low-confidence top two clusters and warns on total miss"):
    val seen = scala.collection.mutable.ListBuffer.empty[String]
    val cacheLookup = new SelectiveEvalLookup:
      def lookup(fen: String) =
        seen += s"cache:$fen"
        fuccess(none[SelectiveEvalProbe])
    val requesterLookup = new SelectiveEvalLookup:
      def lookup(fen: String) =
        seen += s"request:$fen"
        fuccess(none[SelectiveEvalProbe])

    val refiner = new SelectiveEvalRefiner(cacheLookup, requesterLookup)
    val c1 = candidate("g1", Color.White, "White IQP", 16, "fen-1", "d4d5")
    val c2 = candidate("g2", Color.Black, "Black IQP", 17, "fen-2", "d7d5")
    val c3 = candidate("g3", Color.White, "Kingside fianchetto shell", 18, "fen-3", "g2g3", snapshotConfidence = 0.79, collapseBacked = true)
    val selected = List(
      cluster("c1", Color.White, "White IQP", c1, Nil, collapseBackedRate = 0.0, snapshotConfidenceMean = 0.61),
      cluster("c2", Color.Black, "Black IQP", c2, Nil, collapseBackedRate = 0.0, snapshotConfidenceMean = 0.64),
      cluster("c3", Color.White, "Kingside fianchetto shell", c3, Nil, collapseBackedRate = 0.5, snapshotConfidenceMean = 0.79)
    )
    val rows = List(
      row("g1", Color.White, "White IQP", 14, "fen-1a"),
      row("g1", Color.White, "White IQP", 16, "fen-1"),
      row("g1", Color.White, "White IQP", 18, "fen-1b"),
      row("g2", Color.Black, "Black IQP", 15, "fen-2a"),
      row("g2", Color.Black, "Black IQP", 17, "fen-2"),
      row("g2", Color.Black, "Black IQP", 19, "fen-2b"),
      row("g3", Color.White, "Kingside fianchetto shell", 16, "fen-3a"),
      row("g3", Color.White, "Kingside fianchetto shell", 18, "fen-3"),
      row("g3", Color.White, "Kingside fianchetto shell", 20, "fen-3b")
    )

    refiner.refine("chesscom", ProductKind.MyAccountIntelligenceLite, selected, rows).map: result =>
      assertEquals(result.warnings, List("eval refinement unavailable"))
      assert(seen.exists(_.contains("fen-1")))
      assert(seen.exists(_.contains("fen-2")))
      assert(!seen.exists(_.contains("fen-3")))
      assertEquals(result.clusters.map(_.id), selected.map(_.id))

  test("refinement deduplicates repeated fen probes across windows"):
    val seen = scala.collection.mutable.ListBuffer.empty[String]
    val cacheLookup = new SelectiveEvalLookup:
      def lookup(fen: String) =
        seen += s"cache:$fen"
        fuccess(
          SelectiveEvalProbe(cp = Some(32), mate = none[Int], bestMove = Some("d4d5"), source = "cache").some
        )
    val requesterLookup = new SelectiveEvalLookup:
      def lookup(fen: String) =
        fail("requester should not be used when cache hits")

    val refiner = new SelectiveEvalRefiner(cacheLookup, requesterLookup)
    val anchor1 = candidate("g1", Color.White, "White IQP", 16, "fen-shared", "d4d5")
    val anchor2 = candidate("g2", Color.Black, "Black IQP", 18, "fen-shared", "d7d5")
    val selected = List(
      cluster("c1", Color.White, "White IQP", anchor1, Nil, collapseBackedRate = 0.0, snapshotConfidenceMean = 0.61),
      cluster("c2", Color.Black, "Black IQP", anchor2, Nil, collapseBackedRate = 0.0, snapshotConfidenceMean = 0.62)
    )
    val rows = List(
      row("g1", Color.White, "White IQP", 14, "fen-shared"),
      row("g1", Color.White, "White IQP", 16, "fen-shared"),
      row("g2", Color.Black, "Black IQP", 16, "fen-shared"),
      row("g2", Color.Black, "Black IQP", 18, "fen-shared")
    )

    refiner.refine("chesscom", ProductKind.MyAccountIntelligenceLite, selected, rows).map: _ =>
      assertEquals(seen.toList, List("cache:fen-shared"))

  test("opponent prep can refine the third steering cluster when budget remains"):
    val seen = scala.collection.mutable.ListBuffer.empty[String]
    val cacheLookup = new SelectiveEvalLookup:
      def lookup(fen: String) =
        seen += s"cache:$fen"
        fuccess(
          SelectiveEvalProbe(cp = Some(30), mate = none[Int], bestMove = Some("e4e5"), source = "cache").some
        )
    val requesterLookup = new SelectiveEvalLookup:
      def lookup(fen: String) = fuccess(none[SelectiveEvalProbe])

    val refiner = new SelectiveEvalRefiner(cacheLookup, requesterLookup)
    val c1 = candidate("g1", Color.White, "Black IQP", 16, "fen-1", "d4d5", collapseBacked = true, snapshotConfidence = 0.74)
    val c2 = candidate("g2", Color.Black, "White IQP", 17, "fen-2", "d7d5", collapseBacked = true, snapshotConfidence = 0.75)
    val c3 = candidate("g3", Color.White, "Locked Center", 18, "fen-3", "e4e5", snapshotConfidence = 0.63)
    val selected = List(
      cluster("c1", Color.White, "Black IQP", c1, Nil, collapseBackedRate = 0.5, snapshotConfidenceMean = 0.74),
      cluster("c2", Color.Black, "White IQP", c2, Nil, collapseBackedRate = 0.5, snapshotConfidenceMean = 0.75),
      cluster("c3", Color.White, "Locked Center", c3, Nil, collapseBackedRate = 0.0, snapshotConfidenceMean = 0.63)
    )
    val rows = List(
      row("g1", Color.White, "Black IQP", 16, "fen-1"),
      row("g2", Color.Black, "White IQP", 17, "fen-2"),
      row("g3", Color.White, "Locked Center", 16, "fen-3"),
      row("g3", Color.White, "Locked Center", 18, "fen-3")
    )

    refiner.refine("chesscom", ProductKind.OpponentPrep, selected, rows).map: result =>
      assert(seen.exists(_.contains("fen-3")))
      val refinedThird = result.clusters.find(_.id == "c3").flatMap(_.exemplar.candidate).get
      assert(refinedThird.snapshotConfidence > c3.snapshotConfidence)
