package lila.llm.analysis

import lila.llm.model.*

private[analysis] object TacticalTensionPolicy:

  private val StrongThreatCp = 80
  private val SevereCounterfactualCp = 150

  final case class Evaluation(
      forcedOrCritical: Boolean,
      strongTacticalPressure: Boolean,
      severeCounterfactual: Boolean
  ):
    def tacticallyTense: Boolean =
      forcedOrCritical || strongTacticalPressure || severeCounterfactual

  def evaluate(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Evaluation =
    val benchmarkForced =
      normalized(ctx.header.criticality).contains("forced") ||
        normalized(ctx.header.criticality).contains("critical") ||
        normalized(ctx.header.choiceType) == "onlymove"
    val benchmarkPressure =
      ctx.threats.toUs.exists(t => t.lossIfIgnoredCp >= StrongThreatCp || normalized(t.kind).contains("mate")) ||
        ctx.candidates.exists(c =>
          c.tags.contains(CandidateTag.Sharp) ||
            c.tags.contains(CandidateTag.TacticalGamble) ||
            c.tacticEvidence.nonEmpty
        ) ||
        ctx.meta.flatMap(_.errorClass).exists(ec => ec.isTactical || ec.missedMotifs.nonEmpty)
    val benchmarkSevere =
      ctx.counterfactual.exists { cf =>
        cf.cpLoss >= SevereCounterfactualCp ||
        List("blunder", "missedwin", "mistake").contains(normalized(cf.severity).replace(" ", ""))
      }

    truthContract match
      case Some(contract) =>
        val truthForced =
          contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense ||
            contract.failureMode == FailureInterpretationMode.OnlyMoveFailure
        val truthPressure =
          contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation ||
            contract.failureMode == FailureInterpretationMode.TacticalRefutation ||
            (
              contract.failureInterpretationAllowed &&
                contract.cpLoss >= SevereCounterfactualCp &&
                (contract.truthClass == DecisiveTruthClass.Blunder || contract.truthClass == DecisiveTruthClass.MissedWin)
            )
        val truthSevere =
          contract.truthClass == DecisiveTruthClass.MissedWin ||
            (
              contract.failureInterpretationAllowed &&
                contract.cpLoss >= SevereCounterfactualCp &&
                (contract.truthClass == DecisiveTruthClass.Blunder || contract.truthClass == DecisiveTruthClass.MissedWin)
            )
        Evaluation(
          forcedOrCritical = truthForced || benchmarkForced,
          strongTacticalPressure = truthPressure || benchmarkPressure,
          severeCounterfactual = truthSevere || benchmarkSevere
        )
      case None =>
        Evaluation(
          forcedOrCritical = benchmarkForced,
          strongTacticalPressure = benchmarkPressure,
          severeCounterfactual = benchmarkSevere
        )

  def hasForcedOrCriticalState(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    evaluate(ctx, truthContract).forcedOrCritical

  def hasStrongTacticalPressure(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    evaluate(ctx, truthContract).strongTacticalPressure

  def hasSevereCounterfactual(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    evaluate(ctx, truthContract).severeCounterfactual

  def isTacticallyTense(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    evaluate(ctx, truthContract).tacticallyTense

  private def normalized(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
