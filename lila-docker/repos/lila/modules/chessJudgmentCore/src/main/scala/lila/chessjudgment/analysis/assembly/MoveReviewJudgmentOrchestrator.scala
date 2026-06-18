package lila.chessjudgment.analysis.assembly

import lila.chessjudgment.model.judgment.*

final case class MoveReviewJudgmentResult(
    context: JudgmentAssemblyContext,
    packet: EvidenceBackedJudgmentPacket,
    validation: JudgmentPacketValidationResult
):
  def isValid: Boolean = validation.isValid

object MoveReviewJudgmentOrchestrator:

  def assemble(raw: RawMoveReviewInput): Option[JudgmentAssemblyContext] =
    ClaimSeedAssembler.assemble(raw).map(_.context)

  def packet(raw: RawMoveReviewInput): Option[EvidenceBackedJudgmentPacket] =
    assemble(raw).flatMap(JudgmentPacketBuilder.fromAssembly)

  def build(raw: RawMoveReviewInput): Option[MoveReviewJudgmentResult] =
    assemble(raw).flatMap { context =>
      JudgmentPacketBuilder.fromAssembly(context).map { packet =>
        MoveReviewJudgmentResult(
          context = context,
          packet = packet,
          validation = JudgmentPacketValidator.validate(packet)
        )
      }
    }
