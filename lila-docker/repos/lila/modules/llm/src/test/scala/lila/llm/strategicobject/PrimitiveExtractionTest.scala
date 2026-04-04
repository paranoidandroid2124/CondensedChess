package lila.llm.strategicobject

import chess.{ Color, File, Square }
import lila.llm.analysis.*
import munit.FunSuite
import play.api.libs.json.*

import scala.io.Source

class PrimitiveExtractionTest extends FunSuite:

  PrimitiveExtractionTest.rows.foreach { row =>
    test(s"primitive expectation ${row.id}") {
      val evidence =
        RawPositionEvidence.fromFen(row.fen).fold(err => fail(err), identity)
      val primitives =
        CanonicalPrimitiveExtractor
          .extract(evidence, PrimitiveExtractionTest.neutralTruthFrame, PrimitiveExtractionTest.neutralContract)

      val matched = PrimitiveExtractionTest.matches(row, primitives)
      val primitiveDump = PrimitiveExtractionTest.render(primitives)

      row.expectation match
        case "present" =>
          assert(matched, clue(s"${row.id} expected present\n$primitiveDump"))
        case "absent" =>
          assert(!matched, clue(s"${row.id} expected absent\n$primitiveDump"))
        case other =>
          fail(s"${row.id}: unsupported expectation=$other")
    }
  }

  test("primitive expectation bank covers contrastive and eager-negative boundary rows") {
    val rows = PrimitiveExtractionTest.rows

    assert(rows.size >= 40, clue(s"expected at least 40 primitive expectations, got ${rows.size}"))
    assert(rows.count(_.expectation == "present") >= 20, clue("expected a meaningful positive primitive slice"))
    assert(rows.count(_.expectation == "absent") >= 12, clue("expected a meaningful negative primitive slice"))
    assert(rows.exists(_.source.startsWith("contrastive:")), clue("expected contrastive source labels"))
    assert(rows.exists(row => row.primitive == "RouteContestSeed" && row.expectation == "present"))
    assert(rows.exists(row => row.primitive == "RouteContestSeed" && row.expectation == "absent"))
    assert(rows.exists(_.source == "negative:blocked-route"), clue("expected blocked-route eager-access negatives"))
    assert(rows.exists(_.source == "negative:wing-exchange-noise"), clue("expected noisy exchange negatives"))
    assert(rows.exists(_.source == "negative:almost-passer"), clue("expected almost-passer negatives"))
  }

object PrimitiveExtractionTest:

  final case class ExpectationRow(
      id: String,
      source: String,
      fen: String,
      expectation: String,
      primitive: String,
      owner: String,
      square: Option[String],
      file: Option[String],
      kind: Option[String]
  )

  private given Reads[ExpectationRow] = Reads { js =>
    for
      id <- (js \ "id").validate[String]
      source <- (js \ "source").validate[String]
      fen <- (js \ "fen").validate[String]
      expectation <- (js \ "expectation").validate[String]
      primitive <- (js \ "primitive").validate[String]
      owner <- (js \ "owner").validate[String]
      square <- (js \ "square").validateOpt[String]
      file <- (js \ "file").validateOpt[String]
      kind <- (js \ "kind").validateOpt[String]
    yield ExpectationRow(id, source, fen, expectation, primitive, owner, square, file, kind)
  }

  val rows: List[ExpectationRow] =
    Source
      .fromResource("strategic-object-corpus/primitive-expectations.jsonl")
      .getLines()
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[ExpectationRow].asEither match
          case Right(row) => row
          case Left(err)  => throw new IllegalArgumentException(s"invalid primitive expectation row ${idx + 1}: $err")
      }

  val neutralTruthFrame: MoveTruthFrame =
    MoveTruthFrame(
      playedMove = None,
      verifiedBestMove = None,
      moveQuality =
        MoveQualityFact(
          verdict = MoveQualityVerdict.Best,
          cpLoss = 0,
          swingSeverity = 0,
          winPercentBefore = 0.5,
          winPercentAfter = 0.5,
          winPercentLoss = 0.0,
          severityBand = "none"
        ),
      benchmark =
        BenchmarkFact(
          verifiedBestMove = None,
          chosenMatchesBest = false,
          onlyMove = false,
          uniqueGoodMove = false,
          benchmarkNamingAllowed = false,
          alternativeCount = 0,
          verificationTier = "fixture"
        ),
      tactical =
        TacticalFact(
          immediateRefutation = false,
          forcingLine = false,
          forcedMate = false,
          forcedDrawResource = false,
          motifs = Nil,
          proofLine = Nil
        ),
      materialEconomics =
        MaterialEconomicsFact(
          investedMaterialCp = None,
          beforeDeficit = 0,
          afterDeficit = 0,
          movingPieceValue = 0,
          capturedPieceValue = 0,
          sacrificeKind = None,
          valueDownCapture = false,
          recoversDeficit = false,
          overinvestment = false,
          uncompensatedLoss = false,
          forcedRecovery = false
        ),
      strategicOwnership =
        StrategicOwnershipFact(
          truthPhase = None,
          reasonFamily = DecisiveReasonFamily.QuietTechnicalMove,
          benchmarkCriticalMove = false,
          verifiedPayoffAnchor = None,
          chainKey = None,
          evidenceProvenance = Set.empty,
          createsFreshInvestment = false,
          maintainsInvestment = false,
          convertsInvestment = false,
          durablePressure = false,
          currentMoveEvidence = false,
          currentConcreteCarrier = false,
          currentSemanticAnchorMatch = false,
          currentCarrierAnchorMatch = false,
          freshCommitmentCandidate = false,
          freshCurrentInvestmentEvidence = false,
          ownerEligible = false,
          legacyVisibleOnly = false,
          maintenancePressureQualified = false,
          criticalMaintenance = false,
          maintenanceExemplarCandidate = false,
        ),
      punishConversion =
        PunishConversionFact(
          immediatePunishment = false,
          latentPunishment = false,
          conversionRoute = None,
          concessionSummary = None
        ),
      failureInterpretation =
        FailureInterpretationFact(
          failureMode = FailureInterpretationMode.NoClearPlan,
          intentConfidence = 0.0,
          intentAnchor = None,
          interpretationAllowed = false
        ),
      difficultyNovelty =
        DifficultyNoveltyFact(
          onlyMoveDefense = false,
          uniqueGoodMove = false,
          depthSensitive = false,
          shallowUnderestimated = false,
          verificationTier = "fixture"
        ),
      truthClass = DecisiveTruthClass.Best,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.Hidden,
      surfaceMode = TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false
    )

  val neutralContract: DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = None,
      verifiedBestMove = None,
      truthClass = DecisiveTruthClass.Best,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = DecisiveReasonFamily.QuietTechnicalMove,
      allowConcreteBenchmark = false,
      chosenMatchesBest = false,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.Hidden,
      surfaceMode = TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  def matches(row: ExpectationRow, primitives: PrimitiveBank): Boolean =
    val owner = parseColor(row.owner)
    row.primitive match
      case "TargetSquare" =>
        primitives.hasTarget(owner, requireSquare(row))
      case "BreakCandidate" =>
        primitives.hasBreakCandidate(owner, requireFile(row))
      case "RouteContestSeed" =>
        primitives.hasRouteContestSeed(owner, requireSquare(row))
      case "ExchangeSquare" =>
        primitives.hasExchangeSquare(owner, requireSquare(row))
      case "AccessRoute" =>
        primitives.hasAccessRoute(owner, requireFile(row))
      case "DefendedResource" =>
        primitives.hasDefendedResource(owner, requireSquare(row))
      case "PieceRoleIssue" =>
        val issue = requirePieceRoleIssueKind(row)
        row.square.flatMap(parseSquare) match
            case Some(square) => primitives.hasPieceRoleIssue(owner, square, issue)
            case None         => primitives.pieceRoleIssues.exists(p => p.owner == owner && p.issue == issue)
      case "CriticalSquare" =>
        val kind = requireCriticalSquareKind(row)
        row.square.flatMap(parseSquare) match
            case Some(square) => primitives.hasCriticalSquare(owner, square, kind)
            case None         => primitives.criticalSquares.exists(p => p.owner == owner && p.kind == kind)
      case "PasserSeed" =>
        primitives.hasPasserSeed(owner, requireSquare(row))
      case other =>
        throw new IllegalArgumentException(s"${row.id}: unsupported primitive=$other")

  def render(primitives: PrimitiveBank): String =
    primitives.all
      .map {
        case t: TargetSquare =>
          s"TargetSquare(owner=${showColor(t.owner)}, square=${t.square.key}, fixed=${t.fixed}, atk=${t.attackerCount}, def=${t.defenderCount})"
        case b: BreakCandidate =>
          s"BreakCandidate(owner=${showColor(b.owner)}, file=${b.file.char}, break=${b.breakSquare.key}, targets=${b.targetSquares.map(_.key).mkString("[", ",", "]")}, support=${b.supportCount}, resist=${b.resistanceCount})"
        case e: RouteContestSeed =>
          s"RouteContestSeed(owner=${showColor(e.owner)}, square=${e.square.key}, lane=${e.lane.char}, atk=${e.attackerCount}, def=${e.defenderCount})"
        case e: ExchangeSquare =>
          s"ExchangeSquare(owner=${showColor(e.owner)}, square=${e.square.key}, occupant=${e.occupant}, atk=${e.attackerCount}, def=${e.defenderCount})"
        case a: AccessRoute =>
          s"AccessRoute(owner=${showColor(a.owner)}, file=${a.file.char}, roles=${a.roles.mkString("[", ",", "]")})"
        case d: DefendedResource =>
          s"DefendedResource(owner=${showColor(d.owner)}, square=${d.square.key}, role=${d.role}, atk=${d.attackerCount}, def=${d.defenderCount})"
        case p: PieceRoleIssue =>
          s"PieceRoleIssue(owner=${showColor(p.owner)}, square=${p.square.key}, role=${p.role}, issue=${p.issue})"
        case c: CriticalSquare =>
          s"CriticalSquare(owner=${showColor(c.owner)}, square=${c.square.key}, kind=${c.kind}, pressure=${c.pressure})"
        case p: PasserSeed =>
          s"PasserSeed(owner=${showColor(p.owner)}, square=${p.square.key}, protected=${p.protectedByPawn}, rank=${p.relativeRank})"
      }
      .mkString("\n")

  private def parseColor(raw: String): Color =
    raw.toLowerCase match
      case "white" => Color.White
      case "black" => Color.Black
      case _       => throw new IllegalArgumentException(s"unsupported owner=$raw")

  private def parseSquare(raw: String): Option[Square] =
    Square.all.find(_.key.equalsIgnoreCase(raw))

  private def parseFile(raw: String): Option[File] =
    Option.when(raw.length == 1)(raw.head).flatMap(ch => File.all.find(_.char.toLower == ch.toLower))

  private def parsePieceRoleIssueKind(raw: String): Option[PieceRoleIssueKind] =
    PieceRoleIssueKind.values.find(_.toString.equalsIgnoreCase(raw))

  private def parseCriticalSquareKind(raw: String): Option[CriticalSquareKind] =
    CriticalSquareKind.values.find(_.toString.equalsIgnoreCase(raw))

  private def requireSquare(row: ExpectationRow): Square =
    row.square.flatMap(parseSquare).getOrElse(throw new IllegalArgumentException(s"${row.id}: missing/invalid square"))

  private def requireFile(row: ExpectationRow): File =
    row.file.flatMap(parseFile).getOrElse(throw new IllegalArgumentException(s"${row.id}: missing/invalid file"))

  private def requirePieceRoleIssueKind(row: ExpectationRow): PieceRoleIssueKind =
    row.kind.flatMap(parsePieceRoleIssueKind).getOrElse(
      throw new IllegalArgumentException(s"${row.id}: missing/invalid piece-role issue kind")
    )

  private def requireCriticalSquareKind(row: ExpectationRow): CriticalSquareKind =
    row.kind.flatMap(parseCriticalSquareKind).getOrElse(
      throw new IllegalArgumentException(s"${row.id}: missing/invalid critical-square kind")
    )

  private def showColor(color: Color): String =
    if color.white then "white" else "black"
