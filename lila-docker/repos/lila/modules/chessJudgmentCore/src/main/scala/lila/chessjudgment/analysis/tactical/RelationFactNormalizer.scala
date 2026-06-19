package lila.chessjudgment.analysis.tactical

import lila.chessjudgment.model.judgment.*

object RelationFactNormalizer:

  def fromWitness(
      id: String,
      witness: RelationWitness,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope,
      confidence: EvidenceConfidence
  ): Option[EvidenceRecord] =
    RelationFactKind.fromId(witness.kind).map { kind =>
      val ref =
        EvidenceRef(
          id = id,
          producer = EvidenceProducer.TacticalRelationProducer,
          layer = EvidenceLayer.Relation,
          position = position,
          line = line,
          scope = scope,
          confidence = confidence
        )
      EvidenceRecord(
        ref = ref,
        payload = RelationFactEvidence(
          kind = kind,
          focusSquares = witness.focusSquares.map(EvidenceSquare(_)),
          targetSquare = witness.targetSquare.map(EvidenceSquare(_)),
          lineMoves = witness.lineMoves,
          participants = participants(witness.details)
        )
      )
    }

  private def participants(details: RelationDetails): List[RelationParticipant] =
    import RelationDetails.*
    details match
      case Empty => Nil
      case DefenderTrade(defenderSquare, exchangeSquare, targetSquare) =>
        List(
          part(defenderSquare, RelationParticipantRole.Defender),
          part(exchangeSquare, RelationParticipantRole.Other),
          part(targetSquare, RelationParticipantRole.Target)
        )
      case BadPieceLiquidation(badPieceSquare, exchangeSquare) =>
        List(
          part(badPieceSquare, RelationParticipantRole.Target),
          part(exchangeSquare, RelationParticipantRole.Other)
        )
      case Overload(defenderSquare, targetSquares, attackerSquare) =>
        part(defenderSquare, RelationParticipantRole.Defender) ::
          part(attackerSquare, RelationParticipantRole.Attacker) ::
          targetSquares.map(part(_, RelationParticipantRole.Target))
      case Deflection(defenderSquare, targetSquare, attackerSquare) =>
        List(
          part(defenderSquare, RelationParticipantRole.Defender),
          part(targetSquare, RelationParticipantRole.Target),
          part(attackerSquare, RelationParticipantRole.Attacker)
        )
      case DiscoveredAttack(attackerSquare, clearedSquare, targetSquare, attackerRole) =>
        List(
          part(attackerSquare, RelationParticipantRole.Attacker, Some(attackerRole)),
          part(clearedSquare, RelationParticipantRole.Mover),
          part(targetSquare, RelationParticipantRole.Target)
        )
      case DoubleCheck(kingSquare, checkerSquares, moverSquare, moverRole) =>
        part(kingSquare, RelationParticipantRole.King) ::
          part(moverSquare, RelationParticipantRole.Mover, Some(moverRole)) ::
          checkerSquares.map(part(_, RelationParticipantRole.Attacker))
      case MatePattern(_, kingSquare, checkerSquares, matingMove, _) =>
        part(kingSquare, RelationParticipantRole.King) ::
          uciDestination(matingMove).map(part(_, RelationParticipantRole.Mover)).toList :::
          checkerSquares.map(part(_, RelationParticipantRole.Attacker))
      case GreekGift(bishopSquare, targetSquare, _, _) =>
        List(
          part(bishopSquare, RelationParticipantRole.Attacker, Some("bishop")),
          part(targetSquare, RelationParticipantRole.Target)
        )
      case Fork(attackerSquare, attackerRole, targets) =>
        part(attackerSquare, RelationParticipantRole.Attacker, Some(attackerRole)) ::
          targets.map(t => part(t.square, RelationParticipantRole.Target, Some(t.role)))
      case HangingPiece(attackerSquare, targetSquare, attackerRole, targetRole) =>
        List(
          part(attackerSquare, RelationParticipantRole.Attacker, Some(attackerRole)),
          part(targetSquare, RelationParticipantRole.Target, Some(targetRole))
        )
      case TrappedPiece(attackerSquare, targetSquare, attackerRole, targetRole) =>
        List(
          part(attackerSquare, RelationParticipantRole.Attacker, Some(attackerRole)),
          part(targetSquare, RelationParticipantRole.Target, Some(targetRole))
        )
      case Domination(attackerSquare, targetSquare, attackerRole, targetRole, controlledEscapeSquares) =>
        List(
          part(attackerSquare, RelationParticipantRole.Attacker, Some(attackerRole)),
          part(targetSquare, RelationParticipantRole.Target, Some(targetRole))
        ) ++ controlledEscapeSquares.map(part(_, RelationParticipantRole.Other))
      case Zwischenzug(intermediateMove, expectedRecaptureSquare, checkingPieceSquare, checkingPieceRole, checkedKingSquare, _) =>
        uciDestination(intermediateMove).map(part(_, RelationParticipantRole.Mover)).toList ++ List(
          part(expectedRecaptureSquare, RelationParticipantRole.Target),
          part(checkingPieceSquare, RelationParticipantRole.Attacker, Some(checkingPieceRole)),
          part(checkedKingSquare, RelationParticipantRole.King)
        )
      case Decoy(baitFromSquare, baitSquare, luredFromSquare, executionFromSquare, executionToSquare, baitRole, luredRole) =>
        List(
          part(baitFromSquare, RelationParticipantRole.Bait, Some(baitRole)),
          part(baitSquare, RelationParticipantRole.Bait, Some(baitRole)),
          part(luredFromSquare, RelationParticipantRole.Lured, Some(luredRole)),
          part(executionFromSquare, RelationParticipantRole.Attacker),
          part(executionToSquare, RelationParticipantRole.Target)
        )
      case XRay(attackerSquare, blockerSquare, targetSquare, attackerRole, blockerRole, targetRole) =>
        List(
          part(attackerSquare, RelationParticipantRole.Attacker, Some(attackerRole)),
          part(blockerSquare, RelationParticipantRole.Blocker, Some(blockerRole)),
          part(targetSquare, RelationParticipantRole.Target, Some(targetRole))
        )
      case Clearance(beneficiarySquare, clearedSquare, targetSquare, beneficiaryRole, clearingTo) =>
        List(
          part(beneficiarySquare, RelationParticipantRole.Beneficiary, Some(beneficiaryRole)),
          part(clearedSquare, RelationParticipantRole.Mover),
          part(clearingTo, RelationParticipantRole.Mover),
          part(targetSquare, RelationParticipantRole.Target)
        )
      case Battery(frontSquare, backSquare, targetSquare, frontRole, backRole, _) =>
        List(
          part(frontSquare, RelationParticipantRole.Attacker, Some(frontRole)),
          part(backSquare, RelationParticipantRole.Attacker, Some(backRole)),
          part(targetSquare, RelationParticipantRole.Target)
        )
      case Interference(blockerSquare, defenderSquare, targetSquare, blockerRole, defenderRole, targetRole) =>
        List(
          part(blockerSquare, RelationParticipantRole.Blocker, Some(blockerRole)),
          part(defenderSquare, RelationParticipantRole.Defender, Some(defenderRole)),
          part(targetSquare, RelationParticipantRole.Target, Some(targetRole))
        )
      case Pin(attackerSquare, pinnedSquare, behindSquare, targetSquare, attackerRole, pinnedRole, behindRole, _) =>
        List(
          part(attackerSquare, RelationParticipantRole.Attacker, Some(attackerRole)),
          part(pinnedSquare, RelationParticipantRole.Defender, Some(pinnedRole)),
          part(behindSquare, RelationParticipantRole.Target, Some(behindRole)),
          part(targetSquare, RelationParticipantRole.Target)
        )
      case Skewer(attackerSquare, frontSquare, backSquare, targetSquare, attackerRole, frontRole, backRole) =>
        List(
          part(attackerSquare, RelationParticipantRole.Attacker, Some(attackerRole)),
          part(frontSquare, RelationParticipantRole.Target, Some(frontRole)),
          part(backSquare, RelationParticipantRole.Target, Some(backRole)),
          part(targetSquare, RelationParticipantRole.Target)
        )
      case StalemateTrap(stalematedKingSquare, resourceSquare, _, _, _) =>
        List(
          part(stalematedKingSquare, RelationParticipantRole.King),
          part(resourceSquare, RelationParticipantRole.Other)
        )
      case PerpetualCheck(checkedKingSquare, checkerSquares, _, _, _, _, _, _) =>
        part(checkedKingSquare, RelationParticipantRole.King) ::
          checkerSquares.map(part(_, RelationParticipantRole.Attacker))

  private def part(
      square: String,
      participantRole: RelationParticipantRole,
      role: Option[String] = None
  ): RelationParticipant =
    RelationParticipant(
      square = EvidenceSquare(square),
      role = role.map(EvidencePieceRole(_)),
      participantRole = participantRole
    )

  private def uciDestination(move: String): Option[String] =
    val normalized = Option(move).getOrElse("").trim.toLowerCase
    val dest = normalized.drop(2).take(2)
    Option.when(dest.matches("[a-h][1-8]"))(dest)
