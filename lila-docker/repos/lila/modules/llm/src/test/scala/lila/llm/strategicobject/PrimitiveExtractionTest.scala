package lila.llm.strategicobject

import chess.{ Color, File, Square }
import lila.llm.analysis.*
import munit.FunSuite
import play.api.libs.json.*

import scala.io.Source

class PrimitiveExtractionTest extends FunSuite:

  test("canonical primitive expectation bank stays primitive-first and exact-board backed") {
    val rows = PrimitiveExtractionTest.rows
    assert(rows.size >= 20, clue(s"expected at least 20 primitive expectations, got ${rows.size}"))

    rows.foreach { row =>
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
        row.square.exists(parseSquare).exists(square => primitives.hasTarget(owner, square))
      case "BreakAxis" =>
        row.file.exists(parseFile).exists(file => primitives.hasBreakAxis(owner, file))
      case "EntrySquare" =>
        row.square.exists(parseSquare).exists(square => primitives.hasEntrySquare(owner, square))
      case "ExchangeSquare" =>
        row.square.exists(parseSquare).exists(square => primitives.hasExchangeSquare(owner, square))
      case "AccessRoute" =>
        row.file.exists(parseFile).exists(file => primitives.hasAccessRoute(owner, file))
      case "DefendedResource" =>
        row.square.exists(parseSquare).exists(square => primitives.hasDefendedResource(owner, square))
      case "PieceRoleIssue" =>
        row.kind.flatMap(parsePieceRoleIssueKind).exists { issue =>
          row.square.flatMap(parseSquare) match
            case Some(square) => primitives.hasPieceRoleIssue(owner, square, issue)
            case None         => primitives.pieceRoleIssues.exists(p => p.owner == owner && p.issue == issue)
        }
      case "CriticalSquare" =>
        row.kind.flatMap(parseCriticalSquareKind).exists { kind =>
          row.square.flatMap(parseSquare) match
            case Some(square) => primitives.hasCriticalSquare(owner, square, kind)
            case None         => primitives.criticalSquares.exists(p => p.owner == owner && p.kind == kind)
        }
      case "PasserSeed" =>
        row.square.exists(parseSquare).exists(square => primitives.hasPasserSeed(owner, square))
      case other =>
        throw new IllegalArgumentException(s"${row.id}: unsupported primitive=$other")

  def render(primitives: PrimitiveBank): String =
    primitives.all
      .map {
        case t: TargetSquare =>
          s"TargetSquare(owner=${showColor(t.owner)}, square=${t.square.key}, fixed=${t.fixed}, atk=${t.attackerCount}, def=${t.defenderCount})"
        case b: BreakAxis =>
          s"BreakAxis(owner=${showColor(b.owner)}, file=${b.file.char}, break=${b.breakSquare.key}, targets=${b.targetSquares.map(_.key).mkString("[", ",", "]")})"
        case e: EntrySquare =>
          s"EntrySquare(owner=${showColor(e.owner)}, square=${e.square.key}, lane=${e.lane.char})"
        case e: ExchangeSquare =>
          s"ExchangeSquare(owner=${showColor(e.owner)}, square=${e.square.key}, occupant=${e.occupant}, atk=${e.attackerCount}, def=${e.defenderCount})"
        case a: AccessRoute =>
          s"AccessRoute(owner=${showColor(a.owner)}, file=${a.file.char}, entries=${a.entrySquares.map(_.key).mkString("[", ",", "]")})"
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
    if raw.equalsIgnoreCase("white") then Color.White else Color.Black

  private def parseSquare(raw: String): Option[Square] =
    Square.all.find(_.key.equalsIgnoreCase(raw))

  private def parseFile(raw: String): Option[File] =
    raw.headOption.flatMap(ch => File.all.find(_.char.toLower == ch.toLower))

  private def parsePieceRoleIssueKind(raw: String): Option[PieceRoleIssueKind] =
    PieceRoleIssueKind.values.find(_.toString.equalsIgnoreCase(raw))

  private def parseCriticalSquareKind(raw: String): Option[CriticalSquareKind] =
    CriticalSquareKind.values.find(_.toString.equalsIgnoreCase(raw))

  private def showColor(color: Color): String =
    if color.white then "white" else "black"
