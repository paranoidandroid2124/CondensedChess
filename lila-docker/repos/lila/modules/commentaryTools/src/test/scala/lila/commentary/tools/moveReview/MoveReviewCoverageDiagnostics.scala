package lila.commentary.tools.moveReview

import lila.commentary.{ MoveReviewMoveRef, MoveReviewRefs, StrategyPack }
import lila.commentary.analysis.*
import lila.commentary.analysis.claim.ProofContractRules
import lila.commentary.model.*

object MoveReviewCoverageDiagnostics:

  object BasicEvidenceStatus:
    val Emitted = "emitted"
    val PlannerPreempted = "planner_preempted"
    val Blocked = "blocked"

  final case class BasicEvidenceDiagnostic(status: String, rejectReasons: List[String])

  final case class SupportedLocalDiagnostic(
      candidateFamilies: List[String] = Nil,
      admittedFamilies: List[String] = Nil,
      rejectReasons: List[String] = Nil
  )

  final case class Result(
      moveReviewSourceKind: Option[String] = None,
      basicEvidenceStatus: Option[String] = None,
      basicEvidenceRejectReasons: List[String] = Nil,
      supportedLocalCandidateFamilies: List[String] = Nil,
      supportedLocalAdmittedFamilies: List[String] = Nil,
      supportedLocalRejectReasons: List[String] = Nil
  )

  object Result:
    val empty: Result = Result()

  def build(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      slots: MoveReviewPolishSlots,
      plannerInputs: QuestionPlannerInputs
  ): Result =
    val basic = basicEvidence(ctx, refs, strategyPack, truthContract, slots.sourceKind)
    val supportedLocal = supportedLocalFromInputs(plannerInputs)
    Result(
      moveReviewSourceKind = Some(slots.sourceKind),
      basicEvidenceStatus = Some(basic.status),
      basicEvidenceRejectReasons = basic.rejectReasons,
      supportedLocalCandidateFamilies = supportedLocal.candidateFamilies,
      supportedLocalAdmittedFamilies = supportedLocal.admittedFamilies,
      supportedLocalRejectReasons = supportedLocal.rejectReasons
    )

  private def basicEvidence(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      sourceKind: String
  ): BasicEvidenceDiagnostic =
    sourceKind match
      case MoveReviewPolishSlots.Source.BasicMoveExplanation =>
        BasicEvidenceDiagnostic(BasicEvidenceStatus.Emitted, Nil)
      case MoveReviewPolishSlots.Source.Planner =>
        BasicEvidenceDiagnostic(BasicEvidenceStatus.PlannerPreempted, Nil)
      case _ =>
        val played = MoveReviewExplanationBuilder.current(ctx)
        val lineFacts = played.flatMap(move => MoveReviewPvLine.firstCoupled(ctx.fen, move.uci, refs))
        val explanation = MoveReviewExplanationBuilder.build(ctx, refs, truthContract, strategyPack)
        val blockerReasons =
          List(
            Option.when(played.isEmpty)("missing_current_move"),
            Option.when(played.nonEmpty && lineFacts.isEmpty)(
              coupledPvMissingReason(played.get, refs)
            ),
            Option.when(
              played.nonEmpty &&
                lineFacts.nonEmpty &&
                explanation.isEmpty
            )("no_descriptor_rule_matched")
          ).flatten
        val detailReasons = played.toList.flatMap(move => basicDetailReasons(ctx, move, lineFacts, explanation))
        val reasons =
          (blockerReasons ++ detailReasons).distinct match
            case Nil => List("basic_builder_not_emitted")
            case xs  => xs
        BasicEvidenceDiagnostic(BasicEvidenceStatus.Blocked, reasons)

  private def coupledPvMissingReason(
      played: CommentaryIdeaSurface.PlayedMove,
      refs: Option[MoveReviewRefs]
  ): String =
    if firstMoveMatchesPlayed(refs, played.uci) then "coupled_pv_replay_failed"
    else "missing_coupled_pv_line"

  private def basicDetailReasons(
      ctx: NarrativeContext,
      played: CommentaryIdeaSurface.PlayedMove,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      explanation: Option[lila.commentary.MoveReviewExplanation]
  ): List[String] =
    val facts = moveFacts(ctx)
    val motifs = moveMotifs(ctx)
    List(
      Option.when(
        tacticalEvidencePresent(facts, motifs) &&
          lineFacts.nonEmpty &&
          explanation.isEmpty
      )("tactical_not_current_move_owned"),
      Option.when(
        played.isCapture &&
          motifs.exists(_.isInstanceOf[Motif.Capture]) &&
          !isImmediateRecapture(played.toKey, lineFacts.flatMap(_.reply))
      )("capture_not_immediate_recapture")
    ).flatten

  private def supportedLocalFromInputs(inputs: QuestionPlannerInputs): SupportedLocalDiagnostic =
    supportedLocalFromPackets(mainPathPackets(inputs))

  private[moveReview] def supportedLocalFromPackets(packets: List[PlayerFacingClaimPacket]): SupportedLocalDiagnostic =
    val candidates =
      packets.filter(packet => ProofContractRules.supportedLocalEligible(packet.proofFamily))
    val admitted =
      candidates.filter(supportedLocalPolicyAdmitted)
    val rejectReasons =
      candidates
        .filterNot(supportedLocalPolicyAdmitted)
        .flatMap { packet =>
          val trace = ProofContractRules.traceFor(packet)
          val failures =
            (trace.failureCodes ++ policyFailureCodes(packet)).distinct match
              case Nil => List("policy:not_supported_local_admitted")
              case xs  => xs
          failures.map(code => s"${packet.proofFamily}:$code")
        }
        .distinct
        .sorted

    SupportedLocalDiagnostic(
      candidateFamilies = candidates.map(_.proofFamily).distinct.sorted,
      admittedFamilies = admitted.map(_.proofFamily).distinct.sorted,
      rejectReasons = rejectReasons
    )

  private def supportedLocalPolicyAdmitted(packet: PlayerFacingClaimPacket): Boolean =
    ProofContractRules.supportedLocalAdmissible(packet) &&
      PlayerFacingClaimProof.allowsWeakMainClaim(packet) &&
      packet.releaseRisks.isEmpty

  private def policyFailureCodes(packet: PlayerFacingClaimPacket): List[String] =
    List(
      Option.when(packet.scope != PlayerFacingPacketScope.MoveLocal && packet.scope != PlayerFacingPacketScope.PositionLocal)(
        "policy:scope_not_local"
      ),
      Option.when(packet.fallbackMode != PlayerFacingClaimFallbackMode.WeakMain)("policy:fallback_not_weak_main"),
      Option.when(packet.suppressionReasons.nonEmpty)("policy:suppressed"),
      Option.when(packet.releaseRisks.nonEmpty)("policy:release_risk"),
      Option.when(!PlayerFacingClaimProof.allowsWeakMainClaim(packet))("policy:weak_main_gate_blocked")
    ).flatten

  private def mainPathPackets(inputs: QuestionPlannerInputs): List[PlayerFacingClaimPacket] =
    inputs.mainBundle.toList.flatMap { bundle =>
      List(bundle.mainClaim, bundle.lineScopedClaim).flatten.flatMap(_.packet)
    }

  private def moveFacts(ctx: NarrativeContext): List[Fact] =
    (
      ctx.candidates.flatMap(_.facts) ++
        ctx.facts ++
        ctx.mainPvFacts ++
        ctx.threatLineFacts ++
        ctx.counterfactualFacts
      ).distinct

  private def moveMotifs(ctx: NarrativeContext): List[Motif] =
    ctx.candidates.flatMap(_.lineMotifs).distinct

  private def tacticalEvidencePresent(facts: List[Fact], motifs: List[Motif]): Boolean =
    facts.exists {
      case _: Fact.Fork | _: Fact.Pin | _: Fact.Skewer => true
      case _                                           => false
    } || motifs.exists {
      case _: Motif.Fork | _: Motif.Pin | _: Motif.Skewer => true
      case _                                              => false
    }

  private def isImmediateRecapture(playedToKey: String, reply: Option[MoveReviewMoveRef]): Boolean =
    reply.exists { move =>
      MoveReviewPvLine.normalizeUci(move.uci).slice(2, 4) == playedToKey
    }

  private def firstMoveMatchesPlayed(refs: Option[MoveReviewRefs], playedUci: String): Boolean =
    val normalized = MoveReviewPvLine.normalizeUci(playedUci)
    refs.exists(_.variations.exists(_.moves.headOption.exists(move => MoveReviewPvLine.normalizeUci(move.uci) == normalized)))
