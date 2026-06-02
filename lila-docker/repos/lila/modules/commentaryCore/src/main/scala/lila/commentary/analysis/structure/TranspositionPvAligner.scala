package lila.commentary.analysis.structure

import _root_.chess.{ Board, Color, Square }
import _root_.chess.format.{ Fen, Uci }
import _root_.chess.variant.Standard

import lila.commentary.analysis.PlanClaimBoundary
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme }
import lila.commentary.model.authoring.PlanHypothesis
import lila.commentary.model.strategic.VariationLine

private[commentary] object TranspositionPvAligner:

  final case class TranspositionProof(
      proofId: String,
      planId: String,
      subplanId: Option[String],
      targetSquare: String,
      targetKind: String,
      terminalFen: String,
      attackerDefenderDelta: Int,
      linePlies: Int
  )

  private val MinLinePlies = 5
  private val DefaultMaxMoverLossCp = 120
  private val MaxLines = 5
  private val WeaknessSubplans =
    Set(
      PlanKind.StaticWeaknessFixation.id,
      PlanKind.MinorityAttackFixation.id,
      PlanKind.BackwardPawnTargeting.id,
      PlanKind.IQPInducement.id
    )
  private val TargetHintPrefixes = List("weakness_target:", "target:")

  def alignPlans(
      fen: String,
      lines: List[VariationLine],
      hypotheses: List[PlanHypothesis],
      maxMoverLossCp: Int = DefaultMaxMoverLossCp
  ): List[TranspositionProof] =
    val baselineScore = lines.headOption.map(_.scoreCp)
    hypotheses
      .filter(isWeaknessPlan)
      .flatMap { hypothesis =>
        val targets = targetHints(hypothesis)
        if targets.isEmpty then Nil
        else
          lines.take(MaxLines).flatMap(line =>
            align(
              fen = fen,
              line = line,
              planId = hypothesis.planId,
              subplanId = hypothesisPlanKind(hypothesis),
              expectedTargets = targets,
              baselineScoreCp = baselineScore,
              maxMoverLossCp = maxMoverLossCp
            )
          )
      }
      .distinctBy(proof =>
        (
          normalizeKey(proof.planId),
          proof.subplanId.map(normalizeKey),
          proof.targetSquare,
          proof.targetKind,
          proof.terminalFen
        )
      )

  def align(
      fen: String,
      line: VariationLine,
      planId: String,
      subplanId: Option[String],
      expectedTargets: List[String],
      baselineScoreCp: Option[Int] = None,
      maxMoverLossCp: Int = DefaultMaxMoverLossCp
  ): Option[TranspositionProof] =
    val targetSet = expectedTargets.flatMap(normalizeSquare).toSet
    val endpoint = line.resultingFen.flatMap(value => Fen.read(Standard, Fen.Full(value)))
    if targetSet.isEmpty || lineTooShort(line, endpoint) || moverLossTooLarge(fen, line, baselineScoreCp, maxMoverLossCp) then None
    else
      Fen.read(Standard, Fen.Full(fen)).flatMap { start =>
        replayLine(start, line.moves).flatMap { replayed =>
          val terminal = endpoint.getOrElse(replayed)
          val terminalFen = Fen.write(terminal).value
          WeaknessTargetProfile
            .targetsForPressure(terminal.board, start.color)
            .find(target => targetSet.contains(target.targetSquare))
            .flatMap { target =>
              val delta = attackerDefenderDelta(terminal.board, start.color, target.targetSquare)
              Option.when(delta > 0) {
                TranspositionProof(
                  proofId = proofId(planId, target.targetSquare, target.kind),
                  planId = planId,
                  subplanId = subplanId,
                  targetSquare = target.targetSquare,
                  targetKind = target.kind,
                  terminalFen = terminalFen,
                  attackerDefenderDelta = delta,
                  linePlies = line.moves.size
                )
              }
            }
        }
      }

  private def replayLine(
      start: _root_.chess.Position,
      moves: List[String]
  ): Option[_root_.chess.Position] =
    moves.foldLeft(Option(start)) { case (positionOpt, raw) =>
      positionOpt.flatMap(position =>
        Uci(raw.trim.toLowerCase)
          .collect { case move: Uci.Move => move }
          .flatMap(position.move(_).toOption.map(_.after))
      )
    }

  private def lineTooShort(
      line: VariationLine,
      endpoint: Option[_root_.chess.Position]
  ): Boolean =
    endpoint.isEmpty && line.moves.size < MinLinePlies

  private def moverLossTooLarge(
      fen: String,
      line: VariationLine,
      baselineScoreCp: Option[Int],
      maxMoverLossCp: Int
  ): Boolean =
    Fen.read(Standard, Fen.Full(fen)).exists { start =>
      val cpLoss =
        baselineScoreCp.map { baseline =>
          if start.color.white then baseline - line.scoreCp
          else line.scoreCp - baseline
        }.getOrElse(0)
      val mateAgainstMover =
        line.mate.exists(mate => if start.color.white then mate < 0 else mate > 0)
      cpLoss > maxMoverLossCp || mateAgainstMover
    }

  private def attackerDefenderDelta(
      board: Board,
      pressureSide: Color,
      targetSquare: String
  ): Int =
    Square
      .fromKey(targetSquare)
      .map(square => board.attackers(square, pressureSide).count - board.attackers(square, !pressureSide).count)
      .getOrElse(0)

  private def isWeaknessPlan(hypothesis: PlanHypothesis): Boolean =
    val proposal = PlanClaimBoundary.PlanProposal.fromHypothesis(hypothesis)
    proposal.fallbackTheme.contains(PlanTheme.WeaknessFixation) ||
      proposal.supportKind.exists(kind => WeaknessSubplans.contains(kind.id))

  private def hypothesisPlanKind(hypothesis: PlanHypothesis): Option[String] =
    PlanClaimBoundary.PlanProposal.fromHypothesis(hypothesis).supportKind.map(_.id)

  private def targetHints(hypothesis: PlanHypothesis): List[String] =
    hypothesis.evidenceSources.flatMap(targetHintSquare).distinct

  private def targetHintSquare(source: String): Option[String] =
    val lower = Option(source).getOrElse("").trim.toLowerCase
    TargetHintPrefixes
      .collectFirst { case prefix if lower.startsWith(prefix) =>
        lower.stripPrefix(prefix).trim
      }
      .flatMap(normalizeSquare)

  private def normalizeSquare(square: String): Option[String] =
    Option(square).map(_.trim.toLowerCase).filter(_.matches("""[a-h][1-8]"""))

  private def proofId(planId: String, targetSquare: String, targetKind: String): String =
    s"transposition:${normalizeKey(planId)}:$targetSquare:$targetKind"

  private def normalizeKey(value: String): String =
    Option(value).getOrElse("").trim.toLowerCase.replaceAll("""[^a-z0-9_]+""", "_").stripSuffix("_")
