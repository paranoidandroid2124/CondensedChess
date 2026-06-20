package lila.chessjudgment.analysis.assembly

import chess.Color
import lila.chessjudgment.analysis.evaluation.PerspectiveMath
import lila.chessjudgment.analysis.opening.{ OpeningRecognitionIndex, OpeningThemePriorIndex }
import lila.chessjudgment.analysis.line.PrincipalVariationEvidence
import lila.chessjudgment.model.{
  ProbeAdmissionDiagnostic,
  ProbeAdmissionStatus,
  ProbeContractValidator,
  ProbePurpose,
  ProbeRequest,
  ProbeResult
}
import lila.chessjudgment.model.strategic.VariationLine
import lila.chessjudgment.model.judgment.{
  BranchReplyProbeBinding,
  LineNodeRole,
  OpeningContextSignal,
  OpeningFamily,
  OpeningIdentity,
  OpeningRecognition,
  OpeningThemePrior
}

final case class RawOpeningContext(
    eco: Option[String] = None,
    name: Option[String] = None,
    family: Option[String] = None
)

final case class RawMoveReviewInput(
    fen: String,
    playedMoveUci: String,
    variations: List[VariationLine],
    currentEvalCp: Option[Int] = None,
    ply: Option[Int] = None,
    openingContext: Option[RawOpeningContext] = None,
    movePrefixUci: List[String] = Nil,
    probeResults: List[ProbeResult] = Nil
)

final case class NormalizedCandidateLine(
    role: LineNodeRole,
    rank: Int,
    line: VariationLine
):
  def rootMove: Option[String] =
    line.moves.headOption.map(MoveReviewInputNormalizer.normalizeUci)

final case class NormalizedThreatBranch(
    sourceProbeId: String,
    probedMoveUci: String,
    branchFen: String,
    branchPly: Int,
    sideToMove: Option[Color],
    purpose: ProbePurpose,
    lines: List[NormalizedCandidateLine]
)

final case class NormalizedMoveReviewInput(
    beforeFen: String,
    playedMoveUci: String,
    beforePly: Int,
    sideToMove: Option[Color],
    afterPlayedFen: String,
    afterReferenceFen: Option[String],
    lines: List[NormalizedCandidateLine],
    currentEvalCp: Int,
    opening: Option[OpeningIdentity],
    movePrefixUci: List[String] = Nil,
    openingRecognition: Option[OpeningRecognition] = None,
    openingThemePrior: Option[OpeningThemePrior] = None,
    openingSignals: List[OpeningContextSignal] = Nil,
    threatBranches: List[NormalizedThreatBranch] = Nil,
    probeDiagnostics: List[ProbeAdmissionDiagnostic] = Nil
):
  def playedLine: Option[NormalizedCandidateLine] =
    lines.find(_.role == LineNodeRole.Played)

  def referenceLine: Option[NormalizedCandidateLine] =
    lines.find(_.role == LineNodeRole.BestReference)

object MoveReviewInputNormalizer:

  private val BranchReplyPurposes: Set[ProbePurpose] = Set(
    ProbePurpose.ReplyMultipv,
    ProbePurpose.DefenseReplyMultipv,
    ProbePurpose.ConvertReplyMultipv,
    ProbePurpose.RecaptureBranches,
    ProbePurpose.KeepTensionBranches,
    ProbePurpose.FreeTempoBranches
  )

  def normalize(
      raw: RawMoveReviewInput,
      recognitionIndex: OpeningRecognitionIndex = OpeningRecognitionIndex.default,
      themePriorIndex: OpeningThemePriorIndex = OpeningThemePriorIndex.default
  ): Option[NormalizedMoveReviewInput] =
    val beforeFen = normalizeFen(raw.fen)
    val playedMove = normalizeUci(raw.playedMoveUci)
    val movePrefix = raw.movePrefixUci.map(normalizeUci).filter(_.nonEmpty)
    for
      afterPlayed <- PrincipalVariationEvidence.legalFenAfter(beforeFen, playedMove)
      currentEval <- raw.currentEvalCp
        .orElse(raw.variations.headOption.map(_.scoreCp))
    yield
      val beforePly = raw.ply.getOrElse(plyFromFen(beforeFen))
      val side = sideToMove(beforeFen)
      val inputOpening = normalizeOpening(raw.openingContext)
      val recognition = recognitionIndex.recognize(movePrefix, beforeFen, beforePly)
      val opening = inputOpening.orElse(recognition.flatMap(_.bestIdentity))
      val themePrior = themePriorIndex.priorFor(recognition.flatMap(_.lineage), opening.flatMap(_.family))
      val openingSignals =
        List(
          Option.when(inputOpening.nonEmpty)(OpeningContextSignal.InputIdentity),
          Option.when(recognition.nonEmpty)(OpeningContextSignal.RecognizedIdentity),
          Option.when(themePrior.nonEmpty)(OpeningContextSignal.ThemePrior)
        ).flatten
      val ranked = preferredRankedLines(beforeFen, raw.variations.zipWithIndex.filter(_._1.moves.nonEmpty), side)
      val reference = ranked.headOption.map { case (line, index) =>
        NormalizedCandidateLine(LineNodeRole.BestReference, index + 1, normalizedLine(line))
      }
      val played =
        ranked
          .find { case (line, _) => line.moves.headOption.exists(move => normalizeUci(move) == playedMove) }
          .map { case (line, index) => NormalizedCandidateLine(LineNodeRole.Played, index + 1, normalizedLine(line)) }
      val alternatives =
        ranked
          .filterNot { case (_, index) =>
            reference.exists(_.rank == index + 1) || played.exists(_.rank == index + 1)
          }
          .map { case (line, index) => NormalizedCandidateLine(LineNodeRole.Alternative, index + 1, normalizedLine(line)) }
      val lines = (reference.toList ++ played.toList ++ alternatives).distinctBy(line => line.role -> line.rank)
      val afterReference =
        reference.flatMap(_.rootMove).flatMap(PrincipalVariationEvidence.legalFenAfter(beforeFen, _))
      val threatBranchNormalization =
        normalizeThreatBranches(
          raw.probeResults,
          beforeFen = beforeFen,
          rootLines = lines,
          firstThreatRank = lines.map(_.rank).maxOption.getOrElse(0) + 1
        )
      NormalizedMoveReviewInput(
        beforeFen = beforeFen,
        playedMoveUci = playedMove,
        beforePly = beforePly,
        sideToMove = side,
        afterPlayedFen = afterPlayed,
        afterReferenceFen = afterReference,
        lines = lines,
        currentEvalCp = currentEval,
        opening = opening,
        movePrefixUci = movePrefix,
        openingRecognition = recognition,
        openingThemePrior = themePrior,
        openingSignals = openingSignals,
        threatBranches = threatBranchNormalization.branches,
        probeDiagnostics = threatBranchNormalization.diagnostics
      )

  def normalizeUci(uci: String): String =
    PrincipalVariationEvidence.normalizeUci(uci)

  private def normalizedLine(line: VariationLine): VariationLine =
    line.copy(moves = line.moves.map(normalizeUci))

  private def preferredRankedLines(
      startFen: String,
      lines: List[(VariationLine, Int)],
      sideToMove: Option[Color]
  ): List[(VariationLine, Int)] =
    val representatives = lines
      .groupBy { case (line, _) => line.moves.headOption.map(normalizeUci).getOrElse("") }
      .values
      .map { entries =>
        val earliestRank = entries.map(_._2).min
        val bestLine = entries.maxBy { case (line, index) =>
          (line.depth, line.moves.size, line.mate.fold(0)(mate => 1000 - mate.abs), -index)
        }._1
        val legalBestLine = entries.filter { case (line, _) => legalLine(startFen, line) }.maxByOption { case (line, index) =>
          (line.depth, line.moves.size, line.mate.fold(0)(mate => 1000 - mate.abs), -index)
        }.map(_._1).getOrElse(bestLine)
        legalBestLine -> earliestRank
      }
      .toList
    val ordered = sideToMove match
      case Some(side) =>
        representatives.sortBy { case (line, originalRank) =>
          (-scoreForMover(side, line), originalRank)
        }
      case None =>
        representatives.sortBy(_._2)
      ordered.map(_._1).zipWithIndex

  private final case class ThreatBranchSeed(
      sourceProbeId: String,
      probedMoveUci: String,
      branchFen: String,
      purpose: ProbePurpose,
      lines: List[VariationLine],
      variationHash: Option[String]
  )

  private final case class ThreatBranchNormalization(
      branches: List[NormalizedThreatBranch],
      diagnostics: List[ProbeAdmissionDiagnostic]
  )

  private def normalizeThreatBranches(
      probes: List[ProbeResult],
      beforeFen: String,
      rootLines: List[NormalizedCandidateLine],
      firstThreatRank: Int
  ): ThreatBranchNormalization =
    val branches = List.newBuilder[NormalizedThreatBranch]
    val diagnostics = List.newBuilder[ProbeAdmissionDiagnostic]
    var nextRank = firstThreatRank
    probes.foreach { probe =>
      threatBranchSeed(beforeFen, rootLines, probe) match
        case Left(diagnostic) =>
          diagnostics += diagnostic
        case Right(seed) =>
          val side = sideToMove(seed.branchFen)
          val depthFloor = BranchReplyProbeBinding.DepthFloor
          val ranked =
            preferredRankedLines(seed.branchFen, seed.lines.zipWithIndex.filter(_._1.moves.nonEmpty), side)
              .map(_._1)
          val legalLines = ranked.filter(line => legalLine(seed.branchFen, line))
          val depthReadyLines = legalLines.filter(_.depth >= depthFloor)
          val requiredLineCount = BranchReplyProbeBinding.ReplyMultiPv
          if depthReadyLines.size >= requiredLineCount then
            val branchLines = depthReadyLines.map { line =>
              val rank = nextRank
              nextRank += 1
              NormalizedCandidateLine(
                role = LineNodeRole.Threat,
                rank = rank,
                line = normalizedLine(line)
              )
            }
            branches += NormalizedThreatBranch(
              sourceProbeId = seed.sourceProbeId,
              probedMoveUci = seed.probedMoveUci,
              branchFen = seed.branchFen,
              branchPly = plyFromFen(seed.branchFen),
              sideToMove = side,
              purpose = seed.purpose,
              lines = branchLines
            )
            diagnostics += probeDiagnostic(
              probe,
              status = ProbeAdmissionStatus.Admitted,
              reasons = List("ADMITTED"),
              purpose = Some(seed.purpose),
              candidateMove = Some(seed.probedMoveUci),
              branchFen = Some(seed.branchFen),
              admittedLineCount = branchLines.size,
              legalLineCount = legalLines.size,
              scoredLineCount = seed.lines.count(line => line.moves.nonEmpty && line.depth > 0),
              depthFloor = Some(depthFloor),
              variationHash = seed.variationHash
            )
          else
            val reason =
              if legalLines.size < requiredLineCount then "INSUFFICIENT_LEGAL_REPLY_LINES"
              else "REPLY_LINE_DEPTH_FLOOR_UNMET"
            diagnostics += probeDiagnostic(
              probe,
              status = ProbeAdmissionStatus.Rejected,
              reasons = List(reason),
              purpose = Some(seed.purpose),
              candidateMove = Some(seed.probedMoveUci),
              branchFen = Some(seed.branchFen),
              admittedLineCount = 0,
              legalLineCount = legalLines.size,
              scoredLineCount = seed.lines.count(line => line.moves.nonEmpty && line.depth > 0),
              depthFloor = Some(depthFloor),
              variationHash = seed.variationHash
            )
    }
    ThreatBranchNormalization(branches.result(), diagnostics.result())

  private def threatBranchSeed(
      beforeFen: String,
      rootLines: List[NormalizedCandidateLine],
      probe: ProbeResult
  ): Either[ProbeAdmissionDiagnostic, ThreatBranchSeed] =
    probe.purpose.filter(BranchReplyPurposes) match
      case None =>
        Left(
          probeDiagnostic(
            probe,
            status = ProbeAdmissionStatus.Ignored,
            reasons = List("UNSUPPORTED_BRANCH_PURPOSE"),
            purpose = probe.purpose,
            candidateMove = probe.probedMove.orElse(probe.candidateMove).map(normalizeUci).filter(_.nonEmpty),
            branchFen = probe.fen.map(normalizeFen).filter(_.nonEmpty),
            admittedLineCount = 0,
            legalLineCount = 0,
            scoredLineCount = 0,
            depthFloor = None,
            variationHash = probe.variationHash
          )
        )
      case Some(purpose) =>
        val branchFen = probe.fen.map(normalizeFen).filter(_.nonEmpty)
        val scoredLines = probe.replyLines.filter(_.nonEmpty)
        val probedMove = probe.probedMove.orElse(probe.candidateMove).map(normalizeUci).filter(_.nonEmpty)
        val baseDiagnostic =
          (reasons: List[String]) =>
            probeDiagnostic(
              probe,
              status = ProbeAdmissionStatus.Rejected,
              reasons = reasons,
              purpose = Some(purpose),
              candidateMove = probedMove,
              branchFen = branchFen,
              admittedLineCount = 0,
              legalLineCount = 0,
              scoredLineCount = scoredLines.map(_.count(line => line.moves.nonEmpty && line.depth > 0)).getOrElse(0),
              depthFloor = Some(BranchReplyProbeBinding.DepthFloor),
              variationHash = probe.variationHash
            )
        (branchFen, scoredLines, probedMove) match
          case (None, _, _) =>
            Left(baseDiagnostic(List("BRANCH_FEN_MISSING")))
          case (_, None, _) =>
            Left(baseDiagnostic(List("SCORED_REPLY_LINES_MISSING")))
          case (_, _, None) =>
            Left(baseDiagnostic(List("PROBED_MOVE_MISSING")))
          case (Some(fen), Some(lines), Some(move)) =>
            PrincipalVariationEvidence.legalFenAfter(beforeFen, move) match
              case Some(expectedBranchFen) if expectedBranchFen == fen =>
                val matchingMoveLines =
                  rootLines
                  .filterNot(_.role == LineNodeRole.Threat)
                  .filter(line => line.rootMove.contains(move))
                if matchingMoveLines.isEmpty then
                  Left(baseDiagnostic(List("ROOT_LINE_UNBOUND")))
                else if probe.variationHash.forall(_.trim.isEmpty) then
                  Left(baseDiagnostic(List("VARIATION_HASH_MISSING")))
                else
                  matchingMoveLines.find(line => probe.variationHash.contains(branchReplyHash(beforeFen, line, move))) match
                    case None =>
                      Left(baseDiagnostic(List("VARIATION_HASH_MISMATCH")))
                    case Some(rootLine) =>
                      val expectedHash = branchReplyHash(beforeFen, rootLine, move)
                      val contract = branchProbeContract(probe, purpose, expectedBranchFen, move, rootLine, expectedHash)
                      if contract.isValid then
                        Right(
                          ThreatBranchSeed(
                            sourceProbeId = probe.id,
                            probedMoveUci = move,
                            branchFen = expectedBranchFen,
                            purpose = purpose,
                            lines = lines,
                            variationHash = probe.variationHash
                          )
                        )
                      else
                        Left(baseDiagnostic(("CONTRACT_INVALID" :: contract.reasonCodes).distinct))
              case Some(_) =>
                Left(baseDiagnostic(List("BRANCH_FEN_MISMATCH")))
              case None =>
                Left(baseDiagnostic(List("PROBED_MOVE_ILLEGAL_FROM_ROOT")))

  private def branchReplyHash(
      beforeFen: String,
      line: NormalizedCandidateLine,
      probedMove: String
  ): String =
    BranchReplyProbeBinding.variationHash(
      rootFen = beforeFen,
      role = line.role,
      rootMove = probedMove,
      evalCp = line.line.scoreCp,
      mate = line.line.mate,
      depth = line.line.depth,
      moves = line.line.moves
    )

  private def branchProbeContract(
      probe: ProbeResult,
      purpose: ProbePurpose,
      branchFen: String,
      probedMove: String,
      rootLine: NormalizedCandidateLine,
      expectedHash: String
  ): ProbeContractValidator.ValidationResult =
    val request =
      ProbeRequest(
        id = probe.id,
        fen = branchFen,
        moves = Nil,
        depth = BranchReplyProbeBinding.Depth,
        purpose = Some(purpose),
        multiPv = Some(BranchReplyProbeBinding.ReplyMultiPv),
        baselineMove = Some(probedMove),
        baselineEvalCp = Some(rootLine.line.scoreCp),
        baselineMate = rootLine.line.mate,
        baselineDepth = Some(rootLine.line.depth).filter(_ > 0),
        objective = Some(BranchReplyProbeBinding.Objective),
        requiredSignals = BranchReplyProbeBinding.RequiredSignals,
        horizon = Some("short"),
        candidateMove = Some(probedMove),
        depthFloor = Some(BranchReplyProbeBinding.DepthFloor),
        variationHash = Some(expectedHash),
        engineConfigFingerprint = probe.engineConfigFingerprint
      )
    ProbeContractValidator.validateAgainstRequest(request, probe)

  private def probeDiagnostic(
      probe: ProbeResult,
      status: ProbeAdmissionStatus,
      reasons: List[String],
      purpose: Option[ProbePurpose],
      candidateMove: Option[String],
      branchFen: Option[String],
      admittedLineCount: Int,
      legalLineCount: Int,
      scoredLineCount: Int,
      depthFloor: Option[Int],
      variationHash: Option[String]
  ): ProbeAdmissionDiagnostic =
    ProbeAdmissionDiagnostic(
      probeId = Option(probe.id).map(_.trim).filter(_.nonEmpty).getOrElse("probe-result"),
      status = status,
      reasonCodes = reasons.distinct,
      purpose = purpose,
      candidateMove = candidateMove,
      fen = branchFen,
      admittedLineCount = admittedLineCount,
      legalLineCount = legalLineCount,
      scoredLineCount = scoredLineCount,
      depthFloor = depthFloor,
      variationHash = variationHash
    )

  private def scoreForMover(side: Color, line: VariationLine): Double =
    val outcome = PerspectiveMath.winPercentForMover(side, line.scoreCp, line.mate)
    val mateTieBreak =
      line.mate
        .filter(mate => (side.white && mate > 0) || (side.black && mate < 0))
        .map(mate => 1.0 / mate.abs.max(1).toDouble)
        .getOrElse(0.0)
    outcome + mateTieBreak

  private def legalLine(startFen: String, line: VariationLine): Boolean =
    line.moves.nonEmpty &&
      line.moves.foldLeft(Option(startFen)) { (fen, move) =>
        fen.flatMap(PrincipalVariationEvidence.legalFenAfter(_, normalizeUci(move)))
      }.nonEmpty

  private def normalizeFen(fen: String): String =
    Option(fen).getOrElse("").trim.split("\\s+").filter(_.nonEmpty).mkString(" ")

  private def normalizeOpening(raw: Option[RawOpeningContext]): Option[OpeningIdentity] =
    raw.flatMap { context =>
      val eco = cleanText(context.eco).map(_.toUpperCase)
      val name = cleanText(context.name)
      val family =
        cleanText(context.family).flatMap(OpeningFamily.fromRaw)
          .orElse(eco.flatMap(OpeningFamily.fromEco))
          .orElse(name.flatMap(OpeningFamily.fromOpeningName))
      Option.when(eco.nonEmpty || name.nonEmpty || family.nonEmpty)(
        OpeningIdentity(
          eco = eco,
          name = name,
          family = family
        )
      )
    }

  private def cleanText(raw: Option[String]): Option[String] =
    raw.map(_.trim).filter(_.nonEmpty)

  private def sideToMove(fen: String): Option[Color] =
    fen.split("\\s+").lift(1).flatMap:
      case "w" => Some(Color.White)
      case "b" => Some(Color.Black)
      case _   => None

  private def plyFromFen(fen: String): Int =
    val parts = fen.split("\\s+")
    val fullMove = parts.lift(5).flatMap(_.toIntOption).getOrElse(1).max(1)
    val blackToMove = parts.lift(1).contains("b")
    (fullMove - 1) * 2 + Option.when(blackToMove)(1).getOrElse(0)
