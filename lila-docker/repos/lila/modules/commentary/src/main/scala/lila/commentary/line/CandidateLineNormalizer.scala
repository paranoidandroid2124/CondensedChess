package lila.commentary.line

import chess.format.{ Fen, Uci }
import chess.variant

import scala.util.control.NonFatal

import lila.commentary.selection.{ VariationSurfaceAllowance, WordingStrength }

final case class CandidateLineRawLine(
    branchId: CandidateBranchId,
    parentBranchId: Option[CandidateBranchId],
    role: CandidateBranchRole,
    rank: Int,
    multiPvIndex: Int,
    uciLine: Vector[String],
    provenance: CandidateLineProvenance
):
  require(uciLine.forall(_.trim.nonEmpty), "Candidate line raw UCI entries must be non-empty")

final case class CandidateLineNormalizationInput(
    startFen: String,
    startNodeId: String,
    startPly: Int,
    rawLines: Vector[CandidateLineRawLine],
    requestedDepth: Int,
    realizedDepth: Int,
    multiPv: Int,
    generatedAtEpochMs: Long,
    maxAgeMs: Long,
    engineConfigFingerprint: String,
    requireDistinctRootFirstMoves: Boolean = true
)

object CandidateLineNormalizer:

  def normalize(input: CandidateLineNormalizationInput, nowEpochMs: Long): Vector[CandidateLineEvidence] =
    val start = readStart(input.startFen)
    val side = sideToMove(input.startFen).getOrElse(CandidateLineSide.White)
    val timing = CandidateLineTiming(input.generatedAtEpochMs, input.maxAgeMs)
    val commonFailures =
      Vector(
        Option.when(!timing.freshnessValid)(CandidateLineRejectionReason.InvalidFreshnessWindow),
        Option.when(!timing.freshAt(nowEpochMs))(CandidateLineRejectionReason.Stale),
        Option.when(input.realizedDepth < CandidateLineEvidence.DefaultDepthFloor)(
          CandidateLineRejectionReason.InsufficientDepth
        ),
        Option.when(input.multiPv <= 0)(CandidateLineRejectionReason.InvalidMultiPv)
      ).flatten
    val rootMultiPvFailure =
      if input.multiPv > 1 && input.rawLines.count(_.role == CandidateBranchRole.RootCandidate) < input.multiPv then
        Vector(CandidateLineRejectionReason.InvalidMultiPv)
      else Vector.empty
    val duplicateRootFirstMoveFailure =
      if hasDuplicateRootFirstMove(input) then Vector(CandidateLineRejectionReason.DuplicateRootFirstMove)
      else Vector.empty
    input.rawLines.map: raw =>
      val replay =
        start match
          case Left(reason) => ReplayResult(Vector.empty, Vector.empty, Vector(reason), CandidateLineReplayStatus.Illegal)
          case Right(position) => replayLine(position, raw.uciLine)
      val lineFailures =
        commonFailures ++
          replay.failureReasons ++
          Option.when(raw.role == CandidateBranchRole.RootCandidate)(rootMultiPvFailure ++ duplicateRootFirstMoveFailure).toVector.flatten
      CandidateLineEvidence(
        branchId = raw.branchId,
        parentBranchId = raw.parentBranchId,
        role = raw.role,
        rank = raw.rank,
        multiPvIndex = raw.multiPvIndex,
        startFen = input.startFen,
        startNodeId = input.startNodeId,
        startPly = input.startPly,
        sideToMove = side,
        lineUci = replay.uci,
        lineSan = replay.san,
        requestedDepth = input.requestedDepth,
        realizedDepth = input.realizedDepth,
        multiPv = input.multiPv,
        timing = timing,
        engineConfigFingerprint = input.engineConfigFingerprint,
        legalReplay = replay.replayStatus,
        provenance = raw.provenance,
        surfaceAllowance = VariationSurfaceAllowance.PublicLine,
        wordingCap = WordingStrength.QualifiedSupport,
        failureReasons = lineFailures.distinct
      )

  private def readStart(fen: String): Either[CandidateLineRejectionReason, chess.Position] =
    safe:
      Fen
        .read(variant.Standard, Fen.Full.clean(fen))
        .toRight(CandidateLineRejectionReason.InvalidFen)
    match
      case Right(result) => result
      case Left(_) => Left(CandidateLineRejectionReason.InvalidFen)

  private def replayLine(start: chess.Position, rawLine: Vector[String]): ReplayResult =
    rawLine.foldLeft[Either[ReplayResult, ReplayAccumulator]](Right(ReplayAccumulator(start, Vector.empty, Vector.empty))):
      case (Left(done), _) => Left(done)
      case (Right(acc), rawUci) =>
        parseMove(rawUci) match
          case Left(reason) =>
            Left(ReplayResult(acc.uci, acc.san, Vector(reason), CandidateLineReplayStatus.Illegal))
          case Right(move) =>
            acc.position
              .move(move)
              .map: moved =>
                val normalizedUci = rawUci.trim.toLowerCase
                ReplayAccumulator(
                  position = moved.after.position,
                  uci = acc.uci :+ normalizedUci,
                  san = acc.san :+ moved.toSanStr.toString
                )
              .left
              .map(_ => ReplayResult(acc.uci, acc.san, Vector(CandidateLineRejectionReason.IllegalReplay), CandidateLineReplayStatus.Illegal))
    match
      case Right(acc) => ReplayResult(acc.uci, acc.san, Vector.empty, CandidateLineReplayStatus.Legal)
      case Left(done) => done

  private def parseMove(rawUci: String): Either[CandidateLineRejectionReason, Uci.Move] =
    Uci(rawUci.trim) match
      case Some(move: Uci.Move) => Right(move)
      case _ => Left(CandidateLineRejectionReason.MalformedUci)

  private def hasDuplicateRootFirstMove(input: CandidateLineNormalizationInput): Boolean =
    if !input.requireDistinctRootFirstMoves || input.multiPv <= 1 then false
    else
      val firstMoves =
        input.rawLines
          .filter(_.role == CandidateBranchRole.RootCandidate)
          .sortBy(_.multiPvIndex)
          .take(input.multiPv)
          .flatMap(_.uciLine.headOption.map(_.trim.toLowerCase))
      firstMoves.distinct.size < input.multiPv

  private def sideToMove(fen: String): Option[CandidateLineSide] =
    fen.trim.split("\\s+").lift(1).flatMap:
      case "w" => Some(CandidateLineSide.White)
      case "b" => Some(CandidateLineSide.Black)
      case _ => None

  private def safe[A](body: => A): Either[Throwable, A] =
    try Right(body)
    catch case NonFatal(error) => Left(error)

  private final case class ReplayAccumulator(
      position: chess.Position,
      uci: Vector[String],
      san: Vector[String]
  )

  private final case class ReplayResult(
      uci: Vector[String],
      san: Vector[String],
      failureReasons: Vector[CandidateLineRejectionReason],
      replayStatus: CandidateLineReplayStatus
  )
