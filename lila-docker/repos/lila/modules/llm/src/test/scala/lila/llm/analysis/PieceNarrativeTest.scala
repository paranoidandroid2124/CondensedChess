package lila.llm.analysis

import munit.FunSuite
import lila.llm.model._
import lila.llm.model.strategic._
import chess.Color

/**
 * Consolidated piece and strategic narrative tests.
 * Tests that BookStyleRenderer produces correct keywords
 * for various motifs, positional features, and strategic themes.
 */
class PieceNarrativeTest extends FunSuite {

  // ── Bishop Narratives ──

  test("Bishop: Pin narrative") {
    val prose = renderMotif("Bb5", "Pin(Bishop on b5 to king on e8)", "pinning the king")
    assertFound(prose, List("pin", "paralyz", "troublesome"))
    assert(prose.toLowerCase.contains("king"), "Should mention King")
  }

  test("Bishop: Skewer narrative") {
    val prose = renderMotif("Ba3", "Skewer(Bishop through queen on d6 to rook on f8)", "skewering pieces")
    assertFound(prose, List("skewer", "firing through", "power"))
    assert(prose.toLowerCase.contains("queen"), "Should mention Queen")
  }

  test("Bishop: XRay narrative") {
    val prose = renderMotif("Bg2", "XRay(Bishop through to rook on h1)", "x-ray pressure")
    assertFound(prose, List("x-ray", "peering", "pressure"))
  }

  test("Bishop: Battery narrative") {
    val prose = renderMotif("Bc2", "Battery(Bishop and Queen)", "battery formation")
    assertFound(prose, List("battery", "aligning", "backing up"))
  }

  test("Bishop: positional themes via ConceptLinker") {
    val themes = List(
      ("GoodBishop", List("good bishop", "activating", "strong", "unobstructed")),
      ("BadBishop", List("bad bishop", "struggling", "restricted", "blocked")),
      ("BishopPairAdvantage", List("bishop pair", "leveraging", "long-range", "coordination")),
      ("OppositeColorBishops", List("opposite-colored", "opposite-coloured", "complexities", "draw")),
      ("ColorComplexWeakness", List("color complex", "colour complex", "dominating", "exploiting", "light or dark"))
    )
    themes.foreach { case (tagType, keywords) =>
      val prose = renderPositional(tagType)
      assertFound(prose, keywords)
    }
  }

  // ── Knight Narratives ──

  test("Knight: Maneuver narrative") {
    val prose = renderMotif("Nd2", "Maneuver(Knight, rerouting)", "maneuvering")
    assertFound(prose, List("rerout", "transfer", "switch"))
  }

  test("Knight: Domination narrative") {
    val prose = renderMotif("Nd5", "Domination(Knight dominates Bishop)", "central domination")
    assertFound(prose, List("dominat", "paralyz", "grip"))
  }

  test("Knight: TrappedPiece narrative") {
    val prose = renderMotif("Bh6", "TrappedPiece(Queen)", "trapping material")
    assertFound(prose, List("trap", "entomb", "exploit"))
    assert(prose.toLowerCase.contains("queen"), "Should mention Queen")
  }

  test("Knight: KnightVsBishop narrative") {
    val prose = renderMotif("Nd4", "KnightVsBishop(White, true)", "exploiting minor piece imbalance")
    assertFound(prose, List("knight", "bishop", "closed", "open"))
  }

  test("Knight: Blockade narrative") {
    val prose = renderMotif("Nd4", "Blockade(Knight, d5)", "stopping the passer")
    assertFound(prose, List("blocking", "stopping", "blockade"))
  }

  test("Knight: SmotheredMate narrative") {
    val prose = renderMotif("Nf7#", "SmotheredMate(Knight)", "checkmate")
    assertFound(prose, List("smothered", "suffocating", "spectacular"))
  }

  // ── Rook Narratives ──

  test("Rook: Semi-open File narrative") {
    val prose = renderPositional("SemiOpenFileControl", Some("d"))
    assertFound(prose, List("semi-open", "file"))
  }

  test("Rook: Seventh Rank Invasion narrative") {
    val prose = renderPositional("RookOnSeventh")
    assertFound(prose, List("seventh", "invasion", "poking"))
  }

  test("Rook: Behind Passed Pawn narrative") {
    val prose = renderPositional("RookBehindPassedPawn", Some("e"))
    assertFound(prose, List("behind", "passed", "tarrasch"))
  }

  test("Rook: King Cut-Off narrative") {
    val prose = renderPositional("KingCutOff", Some("rank"))
    assertFound(prose, List("cutting", "restricting", "trapped"))
  }

  test("Rook: Doubled/Connected Rooks narrative") {
    assertFound(renderPositional("DoubledRooks"), List("doubled", "firepower", "stacking"))
    assertFound(renderPositional("ConnectedRooks"), List("connecting", "coordination", "linking"))
  }

  test("Rook: Exchange Sacrifice ROI") {
    val roi = Motif.SacrificeROI("open_file", 250)
    val ctx = createCtx(
      move = "Rxd4", evidence = s"ExchangeSacrifice(open_file)", plan = "tactical attack",
      annotation = "!!", difficulty = "sharp",
      semantic = Some(SemanticSection(Nil, Nil, Nil, None, None,
        Some(PracticalInfo(10, 10, "Under Pressure")), Nil, Nil))
    )
    val prose = BookStyleRenderer.render(ctx)
    assertFound(prose, List("sacrificing", "exchange", "file", "dominant"))
  }

  // ── Strategic Narratives ──

  test("Strategic: Pawn Majority / Storm") {
    val text = renderStrategic(
      positional = List(PositionalTag.PawnMajority(Color.White, "kingside", 3))
    )
    assert(text.toLowerCase.contains("majority") || text.toLowerCase.contains("storm"),
      s"Text should mention pawn majority/storm. Got: $text")
  }

  test("Strategic: Hanging Pawns") {
    val text = renderStrategic(
      weaknesses = List(WeakComplex(Color.White, List(chess.Square.C4, chess.Square.D4), false, "Hanging Pawns"))
    )
    assert(text.toLowerCase.contains("hanging pawn"),
      s"Text should mention hanging pawns. Got: $text")
  }

  test("Strategic: Minority Attack") {
    val text = renderStrategic(
      positional = List(PositionalTag.MinorityAttack(Color.White, "queenside"))
    )
    assert(text.toLowerCase.contains("minority attack"),
      s"Text should mention Minority Attack. Got: $text")
  }

  // ── Shared Helpers ──

  private val defaultFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  private def assertFound(prose: String, keywords: List[String]): Unit = {
    val proseLower = prose.toLowerCase.replace("-", " ")
    val found = keywords.find(k => proseLower.contains(k.toLowerCase.replace("-", " ")))
    assert(found.isDefined, s"Result should contain one of $keywords, but none found in: $prose")
  }

  /** Render a motif-driven context and return prose. */
  private def renderMotif(move: String, evidence: String, plan: String): String =
    BookStyleRenderer.render(createCtx(move, evidence, plan))

  /** Render a positional-feature context and return prose. */
  private def renderPositional(tagType: String, detail: Option[String] = None): String = {
    val feature = PositionalTagInfo(tagType, None, None, "White", detail)
    val semantic = SemanticSection(Nil, Nil, List(feature), None, None,
      Some(PracticalInfo(10, 10, "Comfortable")), Nil, Nil)
    BookStyleRenderer.render(createCtx(
      move = "Bc4", evidence = "", plan = "",
      semantic = Some(semantic),
      plans = PlanTable(Nil, Nil)
    ))
  }

  /** Render a strategic-feature context and return prose. */
  private def renderStrategic(
    positional: List[PositionalTag] = Nil,
    weaknesses: List[WeakComplex] = Nil,
    concepts: List[String] = Nil
  ): String = {
    val semantic = SemanticSection(
      positionalFeatures = positional.map(convertTag),
      structuralWeaknesses = weaknesses.map(convertWeak),
      pieceActivity = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = Nil,
      conceptSummary = concepts
    )
    val ctx = NarrativeContext(
      fen = defaultFen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 1,
      summary = NarrativeSummary("Plan A", None, "StyleChoice", "Maintain", "0.0"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Reason", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      snapshots = Nil,
      delta = None,
      phase = PhaseContext("Middlegame", "Reason", None),
      candidates = List(
        CandidateInfo(move = "e4", uci = None, annotation = "",
          planAlignment = "Attack", tacticalAlert = None,
          practicalDifficulty = "clean", whyNot = None)
      ),
      facts = Nil,
      probeRequests = Nil,
      meta = None,
      strategicFlow = None,
      semantic = Some(semantic),
      opponentPlan = None,
      decision = None,
      openingEvent = None,
      openingData = None,
      updatedBudget = OpeningEventBudget(),
      engineEvidence = None
    )
    BookStyleRenderer.render(ctx)
  }

  private def createCtx(
    move: String,
    evidence: String,
    plan: String,
    annotation: String = "!",
    difficulty: String = "clean",
    semantic: Option[SemanticSection] = None,
    plans: PlanTable = PlanTable(Nil, Nil)
  ): NarrativeContext = {
    val planTable = if (plan.nonEmpty && plans.top5.isEmpty)
      PlanTable(List(PlanRow(1, plan, 1.0, List("evidence"))), Nil)
    else plans

    NarrativeContext(
      fen = defaultFen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 1,
      summary = NarrativeSummary("Improving", None, "StyleChoice", "Maintain", "+0.0"),
      phase = PhaseContext("Middlegame", "Material equality", None),
      snapshots = List(L1Snapshot("=", None, None, None, None, None, Nil)),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Test", "Background", None, false, "quiet"),
      threats = ThreatTable(Nil, Nil),
      plans = planTable,
      candidates = List(
        CandidateInfo(
          move = move, uci = None, annotation = annotation,
          planAlignment = plan, downstreamTactic = None,
          tacticalAlert = None, practicalDifficulty = difficulty,
          whyNot = None, tags = Nil,
          tacticEvidence = if (evidence.nonEmpty) List(evidence) else Nil,
          facts = Nil
        )
      ),
      delta = None,
      decision = None,
      opponentPlan = None,
      facts = Nil,
      engineEvidence = None,
      semantic = semantic
    )
  }

  private def convertTag(pt: PositionalTag): PositionalTagInfo = {
    val (name, detail) = pt match {
      case PositionalTag.PawnMajority(_, flank, count) =>
        ("PawnMajority", s"$flank $count pawns")
      case PositionalTag.MinorityAttack(_, flank) =>
        ("MinorityAttack", s"$flank attack")
      case PositionalTag.Outpost(_, _) => ("Outpost", "")
      case _ => (pt.toString.takeWhile(_ != '('), "")
    }
    PositionalTagInfo(name, None, None, "White", Some(detail))
  }

  private def convertWeak(wc: WeakComplex): WeakComplexInfo =
    WeakComplexInfo(
      owner = wc.color.name.capitalize,
      squareColor = "light",
      squares = wc.squares.map(_.key),
      isOutpost = wc.isOutpost,
      cause = wc.cause
    )
}
