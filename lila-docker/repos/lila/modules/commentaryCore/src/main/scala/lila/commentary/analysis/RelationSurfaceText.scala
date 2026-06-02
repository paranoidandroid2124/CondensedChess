package lila.commentary.analysis

import lila.commentary.{ StrategyIdeaSignal, StrategyRelationSupport }
import lila.commentary.analysis.semantic.RelationObservationDescriptor

private[analysis] object RelationSurfaceText:

  final case class SupportedLocalProofSurface(
      relationKind: String,
      targetSquare: String,
      text: String
  )

  def surfaceRowText(
      descriptor: RelationObservationDescriptor,
      idea: StrategyIdeaSignal,
      focusSquares: List[String],
      targetSquare: Option[String]
  ): Option[String] =
    supportFor(descriptor, idea).map { support =>
      val focusText = focusSquares.take(3).mkString(", ")
      val base =
        if focusText.nonEmpty then s"The checked line gives ${descriptor.publicLabel} evidence around $focusText"
        else s"The checked line gives ${descriptor.publicLabel} evidence"
      detailClause(descriptor.relationKind, Some(support), focusSquares, targetSquare)
        .map(clause => s"$base: $clause.")
        .getOrElse(s"$base.")
    }

  def ideaText(
      descriptor: RelationObservationDescriptor,
      idea: StrategyIdeaSignal,
      focusSquares: List[String],
      targetSquare: Option[String]
  ): Option[String] =
    supportFor(descriptor, idea).flatMap { support =>
      detailClause(descriptor.relationKind, Some(support), Nil, targetSquare)
        .orElse(focusSummary(descriptor, idea, focusSquares, targetSquare))
    }

  def reviewExplanationText(
      descriptor: RelationObservationDescriptor,
      idea: StrategyIdeaSignal,
      focusSquares: List[String],
      targetSquare: Option[String]
  ): Option[String] =
    supportFor(descriptor, idea).flatMap { support =>
      val why =
        detailClause(descriptor.relationKind, Some(support), Nil, targetSquare)
          .orElse(focusSummary(descriptor, idea, focusSquares, targetSquare))
      val cue =
        reviewCueClause(descriptor.relationKind, support, focusSquares, targetSquare)
      for
        whyText <- why
        cueText <- cue
      yield s"Why it works: $whyText. Next check: $cueText."
    }

  def supportedLocalProofSurface(packet: PlayerFacingClaimPacket): Option[SupportedLocalProofSurface] =
    packet.proofPathWitness.exactSliceProof.collect {
      case proof: PlayerFacingExactSliceProof.DefenderTrade
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, this trades the defender on ${proof.defenderSquare} and leaves ${proof.targetSquare} as the relation target."
        )
      case proof: PlayerFacingExactSliceProof.BadPieceLiquidation
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation,
          targetSquare = proof.exchangeSquare,
          text = s"On the checked line, this trades the bad piece from ${proof.badPieceSquare} on ${proof.exchangeSquare}."
        )
      case proof: PlayerFacingExactSliceProof.Overload
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Overload,
          targetSquare = proof.defenderSquare,
          text =
            s"On the checked line, the defender on ${proof.defenderSquare} is overloaded across ${joinTerms(proof.targetSquares.take(3))} under pressure from ${proof.attackerSquare}."
        )
      case proof: PlayerFacingExactSliceProof.Deflection
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Deflection,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, the attack from ${proof.attackerSquare} deflects the defender on ${proof.defenderSquare} away from ${proof.targetSquare}."
        )
      case proof: PlayerFacingExactSliceProof.DiscoveredAttack
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val role = pieceRole(proof.attackerRole).getOrElse(proof.attackerRole)
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, the $role on ${proof.attackerSquare} is uncovered through ${proof.clearedSquare} toward ${proof.targetSquare}."
        )
      case proof: PlayerFacingExactSliceProof.DoubleCheck
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val role = pieceRole(proof.moverRole).getOrElse(proof.moverRole)
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
          targetSquare = proof.kingSquare,
          text =
            s"On the checked line, the $role on ${proof.moverSquare} gives double check on ${proof.kingSquare} with ${joinTerms(proof.checkerSquares.take(3))}."
        )
      case proof: PlayerFacingExactSliceProof.BackRankMate
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
          targetSquare = proof.kingSquare,
          text =
            s"On the checked line, ${displayUci(proof.matingMove)} gives back-rank mate on ${proof.kingSquare}."
        )
      case proof: PlayerFacingExactSliceProof.MateNet
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val pattern = proof.patternId.getOrElse("forced")
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.MateNet,
          targetSquare = proof.kingSquare,
          text =
            s"On the checked line, ${displayUci(proof.matingMove)} gives a $pattern mate net on ${proof.kingSquare}."
        )
      case proof: PlayerFacingExactSliceProof.GreekGift
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, ${displayUci(proof.entryMove)} is a Greek gift entry on ${proof.targetSquare}."
        )
      case proof: PlayerFacingExactSliceProof.Fork
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val attackerRole = pieceRole(proof.attackerRole).getOrElse(proof.attackerRole)
        val targetText =
          joinTerms(
            proof.targets
              .take(3)
              .map(target => pieceWithArticle(target.square, Some(target.role), "target"))
          )
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Fork,
          targetSquare = PlayerFacingExactSliceProofFacts.targetSquare(proof).getOrElse(proof.targets.head.square),
          text =
            s"On the checked line, the $attackerRole on ${proof.attackerSquare} forks $targetText."
        )
      case proof: PlayerFacingExactSliceProof.HangingPiece
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val attacker = pieceWithArticle(proof.attackerSquare, Some(proof.attackerRole), "attacker")
        val targetRole = pieceRole(proof.targetRole).getOrElse(proof.targetRole)
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.HangingPiece,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, $attacker attacks the undefended $targetRole on ${proof.targetSquare}."
        )
      case proof: PlayerFacingExactSliceProof.TrappedPiece
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val targetRole = pieceRole(proof.targetRole).getOrElse(proof.targetRole)
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, the $targetRole on ${proof.targetSquare} has no safe escape against pressure from ${joinTerms(proof.attackerSquares.take(3))}."
        )
      case proof: PlayerFacingExactSliceProof.Domination
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val controllerRole = pieceRole(proof.controllerRole).getOrElse(proof.controllerRole)
        val targetRole = pieceRole(proof.targetRole).getOrElse(proof.targetRole)
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Domination,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, the $controllerRole on ${proof.controllerSquare} restricts the $targetRole on ${proof.targetSquare} to ${proof.legalMoveCount} legal move."
        )
      case proof: PlayerFacingExactSliceProof.StalemateTrap
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
          targetSquare = proof.kingSquare,
          text =
            s"On the checked line, ${displayUci(proof.trappingMove)} leaves the king on ${proof.kingSquare} stalemated."
        )
      case proof: PlayerFacingExactSliceProof.PerpetualCheck
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
          targetSquare = proof.kingSquare,
          text =
            s"On the checked line, the checks on ${proof.kingSquare} repeat after ${proof.repeatedPositionPly} plies."
        )
      case proof: PlayerFacingExactSliceProof.Zwischenzug
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, ${displayUci(proof.intermediateMove)} is the forcing in-between move before ${displayUci(proof.payoffMove)} wins ${proof.targetSquare}."
        )
      case proof: PlayerFacingExactSliceProof.Decoy
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val baitRole = pieceRole(proof.baitRole).getOrElse(proof.baitRole)
        val luredRole = pieceRole(proof.luredRole).getOrElse(proof.luredRole)
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Decoy,
          targetSquare = proof.baitSquare,
          text =
            s"On the checked line, the $baitRole from ${proof.baitFromSquare} lures the $luredRole from ${proof.luredFromSquare} onto ${proof.baitSquare} before ${displayUci(proof.executionFromSquare + proof.executionToSquare)} wins it."
        )
      case proof: PlayerFacingExactSliceProof.XRay
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val attackerRole = pieceRole(proof.attackerRole).getOrElse(proof.attackerRole)
        val blocker = pieceWithArticle(proof.blockerSquare, Some(proof.blockerRole), "blocker")
        val target = pieceWithArticle(proof.targetSquare, Some(proof.targetRole), "target")
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, the $attackerRole on ${proof.attackerSquare} x-rays $target through $blocker."
        )
      case proof: PlayerFacingExactSliceProof.Clearance
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val beneficiaryRole = pieceRole(proof.beneficiaryRole).getOrElse(proof.beneficiaryRole)
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Clearance,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, ${proof.clearingTo} clears ${proof.clearedSquare} so the $beneficiaryRole on ${proof.beneficiarySquare} reaches ${proof.targetSquare}."
        )
      case proof: PlayerFacingExactSliceProof.Battery
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val front = pieceWithArticle(proof.frontSquare, Some(proof.frontRole), "front piece")
        val back = pieceWithArticle(proof.backSquare, Some(proof.backRole), "back piece")
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Battery,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, $front and $back form a ${proof.axis} battery toward ${proof.targetSquare}."
        )
      case proof: PlayerFacingExactSliceProof.Pin
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val attacker = pieceWithArticle(proof.attackerSquare, Some(proof.attackerRole), "attacker")
        val pinned = pieceWithArticle(proof.pinnedSquare, Some(proof.pinnedRole), "piece")
        val behind = pieceWithArticle(proof.behindSquare, Some(proof.behindRole), "piece")
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, $attacker pins $pinned to $behind."
        )
      case proof: PlayerFacingExactSliceProof.Skewer
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val attacker = pieceWithArticle(proof.attackerSquare, Some(proof.attackerRole), "attacker")
        val front = pieceWithArticle(proof.frontSquare, Some(proof.frontRole), "front piece")
        val back = pieceWithArticle(proof.backSquare, Some(proof.backRole), "back piece")
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Skewer,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, $attacker skewers $front in front of $back."
        )
      case proof: PlayerFacingExactSliceProof.Interference
          if PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) =>
        val blocker = pieceWithArticle(proof.blockerSquare, Some(proof.blockerRole), "blocker")
        val defender = pieceWithArticle(proof.defenderSquare, Some(proof.defenderRole), "defender")
        val target = pieceWithArticle(proof.targetSquare, Some(proof.targetRole), "target")
        SupportedLocalProofSurface(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Interference,
          targetSquare = proof.targetSquare,
          text =
            s"On the checked line, $blocker interferes with $defender defending $target."
        )
    }

  def focusSummary(
      descriptor: RelationObservationDescriptor,
      idea: StrategyIdeaSignal,
      focusSquares: List[String],
      targetSquare: Option[String]
  ): Option[String] =
    supportFor(descriptor, idea).flatMap { support =>
      supportSummaryClause(descriptor.relationKind, support, targetSquare)
        .orElse(detailClause(descriptor.relationKind, Some(support), Nil, targetSquare))
        .orElse(Option.when(focusSquares.nonEmpty)(focusSquares.take(3).mkString(", ")))
    }

  private def supportFor(
      descriptor: RelationObservationDescriptor,
      idea: StrategyIdeaSignal
  ): Option[StrategyRelationSupport] =
    idea.relationSupport.filter(support =>
      support.relationKind == descriptor.relationKind &&
        matchingRelationFocus(idea.relationFocusSquares, support.focusSquares)
    )

  private def matchingRelationFocus(
      ideaFocusSquares: List[String],
      supportFocusSquares: List[String]
  ): Boolean =
    val ideaFocus = normalizedSquareKeys(ideaFocusSquares)
    val supportFocus = normalizedSquareKeys(supportFocusSquares)
    ideaFocus.nonEmpty && ideaFocus == supportFocus

  private def normalizedSquareKeys(keys: List[String]): List[String] =
    keys
      .flatMap(key => Option(key).map(_.trim.toLowerCase).filter(_.matches("""[a-h][1-8]""")))
      .distinct

  private def supportSummaryClause(
      relationKind: String,
      detail: StrategyRelationSupport,
      targetSquare: Option[String]
  ): Option[String] =
    relationKind match
      case MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val role = detail.targetRole.flatMap(pieceRole).getOrElse("piece")
          s"trapped $role on $target"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Domination =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val role = detail.targetRole.flatMap(pieceRole).getOrElse("piece")
          s"restricted $role on $target"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.HangingPiece =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val role = detail.targetRole.flatMap(pieceRole).getOrElse("piece")
          s"undefended $role on $target"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade =>
        targetSquare.orElse(detail.targetSquare).map(target => s"defender removal on $target")
      case MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation =>
        detail.badPieceSquare.orElse(targetSquare).map(square => s"bad-piece trade from $square")
      case MoveReviewExchangeAnalyzer.RelationKind.Overload =>
        detail.defenderSquare.map(square => s"overloaded defender on $square")
      case MoveReviewExchangeAnalyzer.RelationKind.Deflection =>
        detail.defenderSquare.map(square => s"deflected defender on $square")
      case MoveReviewExchangeAnalyzer.RelationKind.Decoy =>
        detail.baitSquare.map(square => s"decoy on $square")
      case MoveReviewExchangeAnalyzer.RelationKind.Interference =>
        detail.blockerSquare.map(square => s"interference block on $square")
      case MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug =>
        detail.intermediateMove.map(move => s"intermediate ${displayUci(move)}")
      case MoveReviewExchangeAnalyzer.RelationKind.XRay =>
        lineSummary("x-ray line", detail.attackerSquare, detail.blockerSquare, targetSquare.orElse(detail.targetSquare))
      case MoveReviewExchangeAnalyzer.RelationKind.Clearance =>
        lineSummary("cleared line", detail.beneficiarySquare, detail.clearedSquare, targetSquare.orElse(detail.targetSquare))
      case MoveReviewExchangeAnalyzer.RelationKind.Battery =>
        lineSummary("battery", detail.frontSquare, detail.backSquare, targetSquare.orElse(detail.targetSquare))
      case MoveReviewExchangeAnalyzer.RelationKind.Pin =>
        lineSummary("pin", detail.attackerSquare, detail.pinnedSquare, detail.behindSquare.orElse(targetSquare))
      case MoveReviewExchangeAnalyzer.RelationKind.Skewer =>
        lineSummary("skewer", detail.attackerSquare, detail.frontSquare, detail.backSquare.orElse(targetSquare))
      case MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack =>
        targetSquare.orElse(detail.targetSquare).map(target => s"discovered attack on $target")
      case MoveReviewExchangeAnalyzer.RelationKind.Fork =>
        detail.attackerSquare.map(square => s"fork from $square")
      case MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map(square => s"repeating checks on $square")
      case MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map(square => s"stalemate resource on $square")
      case MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map(square => s"double check on $square")
      case MoveReviewExchangeAnalyzer.RelationKind.BackRankMate | MoveReviewExchangeAnalyzer.RelationKind.MateNet =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map(square => s"mate net around $square")
      case MoveReviewExchangeAnalyzer.RelationKind.GreekGift =>
        targetSquare.orElse(detail.targetSquare).map(square => s"sacrifice entry on $square")
      case _ => None

  private def lineSummary(
      label: String,
      first: Option[String],
      second: Option[String],
      third: Option[String]
  ): Option[String] =
    val squares = List(first, second, third).flatten.distinct
    Option.when(squares.nonEmpty)(s"$label ${squares.mkString("-")}")

  private def detailClause(
      relationKind: String,
      support: Option[StrategyRelationSupport],
      focusSquares: List[String],
      targetSquare: Option[String]
  ): Option[String] =
    support.flatMap { detail =>
      restrictedPieceClause(relationKind, detail, targetSquare)
        .orElse(moveOrderClause(relationKind, detail, targetSquare))
        .orElse(lineGeometryClause(relationKind, detail, targetSquare))
        .orElse(kingClause(relationKind, detail, targetSquare))
        .orElse(multiTargetClause(relationKind, detail))
    }.orElse(focusClause(relationKind, focusSquares, targetSquare))

  private def reviewCueClause(
      relationKind: String,
      detail: StrategyRelationSupport,
      focusSquares: List[String],
      targetSquare: Option[String]
  ): Option[String] =
    relationKind match
      case MoveReviewExchangeAnalyzer.RelationKind.Fork =>
        val targets = describedPieces(detail.targetSquares.take(3), detail.targetRoles.take(3))
        if targets.nonEmpty then Some(s"whether ${joinTerms(targets)} stay loose after the reply")
        else targetSquare.orElse(detail.targetSquare).map(target => s"whether the target on $target stays loose after the reply")
      case MoveReviewExchangeAnalyzer.RelationKind.Overload =>
        detail.defenderSquare.map { defender =>
          val duties =
            if detail.targetSquares.nonEmpty then s" can still cover ${joinTerms(detail.targetSquares.take(3))}"
            else " can still cover every duty"
          s"whether the defender on $defender$duties after the reply"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Deflection =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val defender = detail.defenderSquare.map(square => s"the defender on $square").getOrElse("the defender")
          s"whether $target is still protected after $defender is pulled"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade =>
        targetSquare.orElse(detail.targetSquare).map(target => s"whether $target remains under-defended after the exchange")
      case MoveReviewExchangeAnalyzer.RelationKind.Decoy =>
        detail.baitSquare.orElse(targetSquare).map { bait =>
          val lured = detail.luredFromSquare.map(square => s" from $square").getOrElse("")
          s"whether the lured piece$lured remains exposed after the bait on $bait"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Interference =>
        targetSquare.orElse(detail.targetSquare).map(target => s"whether the blocked defender can reconnect to $target")
      case MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug =>
        detail.payoffMove.orElse(targetSquare).map(value => s"whether the intermediate move still wins ${displayMaybeMove(value)}")
      case MoveReviewExchangeAnalyzer.RelationKind.XRay |
          MoveReviewExchangeAnalyzer.RelationKind.Clearance |
          MoveReviewExchangeAnalyzer.RelationKind.Battery |
          MoveReviewExchangeAnalyzer.RelationKind.Pin |
          MoveReviewExchangeAnalyzer.RelationKind.Skewer |
          MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack =>
        val path = relationPath(detail, focusSquares, targetSquare)
        Option.when(path.nonEmpty)(s"the line geometry through ${joinTerms(path.take(4))}")
      case MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece =>
        targetSquare.orElse(detail.targetSquare).map(target => s"the legal escape count for the target on $target")
      case MoveReviewExchangeAnalyzer.RelationKind.Domination =>
        targetSquare.orElse(detail.targetSquare).map(target => s"the legal move count for the restricted target on $target")
      case MoveReviewExchangeAnalyzer.RelationKind.HangingPiece =>
        targetSquare.orElse(detail.targetSquare).map(target => s"whether the target on $target can be defended in time")
      case MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map(king => s"both checking lines against the king on $king")
      case MoveReviewExchangeAnalyzer.RelationKind.BackRankMate | MoveReviewExchangeAnalyzer.RelationKind.MateNet =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map(king => s"the forced replies around the king on $king before treating it as decisive")
      case MoveReviewExchangeAnalyzer.RelationKind.GreekGift =>
        targetSquare.orElse(detail.targetSquare).map(target => s"the follow-up against $target before treating the sacrifice as sound")
      case MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map(king => s"the repeated checking cycle against the king on $king")
      case MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map(king => s"the stalemate position around the king on $king")
      case MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation =>
        detail.badPieceSquare.orElse(targetSquare).map(square => s"whether the traded piece on $square was still the strategic problem")
      case _ =>
        Option.when(focusSquares.nonEmpty)(s"the relation support around ${joinTerms(focusSquares.take(3))}")

  private def restrictedPieceClause(
      relationKind: String,
      detail: StrategyRelationSupport,
      targetSquare: Option[String]
  ): Option[String] =
    relationKind match
      case MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val role = detail.targetRole.flatMap(pieceRole).map(role => s"the $role on ").getOrElse("the piece on ")
          val escape =
            detail.legalEscapeCount match
              case Some(0) => "has no legal escape square in that replay"
              case Some(1) => "has only one legal escape square in that replay"
              case Some(n) => s"has $n legal escape squares in that replay"
              case None    => "is restricted by the replayed attackers"
          val attackers =
            if detail.attackerSquares.nonEmpty then s"; attackers include ${joinTerms(detail.attackerSquares.take(3))}"
            else ""
          s"$role$target $escape$attackers"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Domination =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val controller =
            detail.controllerSquare
              .map(square =>
                detail.controllerRole
                  .flatMap(pieceRole)
                  .map(role => s" under $role control from $square")
                  .getOrElse(s" under control from $square")
              )
              .getOrElse("")
          val role = detail.targetRole.flatMap(pieceRole).map(role => s"the $role on ").getOrElse("the piece on ")
          val mobility =
            detail.legalMoveCount match
              case Some(0) => "has no legal move"
              case Some(1) => "has only one legal move"
              case Some(n) => s"has $n legal moves"
              case None    => "is kept restricted"
          s"$role$target $mobility$controller"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.HangingPiece =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val role = detail.targetRole.flatMap(pieceRole).map(role => s"the $role on ").getOrElse("the piece on ")
          val attacker = detail.attackerSquare.map(square => s" from $square").getOrElse("")
          s"$role$target is an undefended target$attacker"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation =>
        detail.badPieceSquare.orElse(targetSquare).map { badPiece =>
          val exchange = detail.exchangeSquare.map(square => s" through $square").getOrElse("")
          s"the replay trades off the poorly placed piece on $badPiece$exchange"
        }
      case _ => None

  private def moveOrderClause(
      relationKind: String,
      detail: StrategyRelationSupport,
      targetSquare: Option[String]
  ): Option[String] =
    relationKind match
      case MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val exchange = detail.exchangeSquare.map(square => s" on $square").getOrElse("")
          s"the exchange$exchange removes a defender of $target"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Overload =>
        detail.defenderSquare.map { defender =>
          val duties = detail.targetSquares.take(2)
          val dutyText =
            if duties.nonEmpty then s" across ${joinTerms(duties)}"
            else " across multiple duties"
          val pressure = detail.attackerSquare.map(square => s" under pressure from $square").getOrElse("")
          s"the defender on $defender is overloaded$dutyText$pressure"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Deflection =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val attack = detail.attackerSquare.map(square => s"attack from $square pulls").getOrElse("the line pulls")
          val defender = detail.defenderSquare.map(square => s"the defender on $square").getOrElse("the defender")
          s"$attack $defender away from $target"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Decoy =>
        detail.baitSquare.map { bait =>
          val baitText =
            detail.baitRole.flatMap(pieceRole).map(role => s"$role bait on $bait").getOrElse(s"bait on $bait")
          val lured =
            detail.luredFromSquare
              .map(square =>
                detail.luredRole
                  .flatMap(pieceRole)
                  .map(role => s" draws the $role from $square")
                  .getOrElse(s" draws a piece from $square")
              )
              .getOrElse(" draws a piece")
          val execution = executionRoute(detail).map(route => s" before $route").getOrElse("")
          s"the $baitText$lured$execution"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Interference =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val blocker = detail.blockerSquare.map(square => s" on $square").getOrElse("")
          val defender = detail.defenderSquare.map(square => s" from $square").getOrElse("")
          s"the blocker$blocker cuts the defender$defender off from $target"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug =>
        val intermediate = detail.intermediateMove.map(move => s"${displayUci(move)} comes before")
          .getOrElse("the intermediate move comes before")
        val payoff = detail.payoffMove.orElse(targetSquare).map(value => s" the payoff on ${displayMaybeMove(value)}").getOrElse(" the payoff")
        val threat = detail.threatType.map(value => s" after a ${humanize(value)}").getOrElse("")
        Some(s"$intermediate$payoff$threat")
      case _ => None

  private def lineGeometryClause(
      relationKind: String,
      detail: StrategyRelationSupport,
      targetSquare: Option[String]
  ): Option[String] =
    relationKind match
      case MoveReviewExchangeAnalyzer.RelationKind.XRay =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val attacker = detail.attackerSquare.map(square => s" from $square").getOrElse("")
          val blocker = detail.blockerSquare.map(square => s" through $square").getOrElse("")
          s"the line runs$attacker$blocker toward $target"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Clearance =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val cleared = detail.clearedSquare.map(square => s" $square").getOrElse(" a line")
          val beneficiary =
            detail.beneficiarySquare
              .map(square => s" for ${pieceWithArticle(square, detail.beneficiaryRole, "piece")}")
              .getOrElse("")
          s"the replay clears$cleared$beneficiary toward $target"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Battery =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val front = pieceWithoutArticle(detail.frontSquare, detail.frontRole, "front piece")
          val back = pieceWithoutArticle(detail.backSquare, detail.backRole, "supporting piece")
          val axis = detail.axis.map(value => s" on the ${humanize(value)}").getOrElse("")
          (front, back) match
            case (Some(frontText), Some(backText)) => s"$frontText and $backText align$axis toward $target"
            case (Some(frontText), None)           => s"$frontText aligns$axis toward $target"
            case (None, Some(backText))            => s"$backText supports the battery$axis toward $target"
            case (None, None)                      => s"the pieces align$axis toward $target"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.Pin =>
        pinOrSkewerClause(
          verb = "pins",
          frontSquare = detail.pinnedSquare,
          frontRole = detail.pinnedRole,
          backSquare = detail.behindSquare,
          backRole = detail.behindRole,
          detail = detail,
          targetSquare = targetSquare,
          prefix = Option.when(detail.absolutePin.contains(true))("absolute pin: ")
        )
      case MoveReviewExchangeAnalyzer.RelationKind.Skewer =>
        pinOrSkewerClause(
          verb = "skewers",
          frontSquare = detail.frontSquare,
          frontRole = detail.frontRole,
          backSquare = detail.backSquare,
          backRole = detail.backRole,
          detail = detail,
          targetSquare = targetSquare,
          prefix = None
        )
      case MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val attacker = detail.attackerSquare.map(square => s" for the line piece on $square").getOrElse("")
          val cleared = detail.clearedSquare.map(square => s" after $square clears").getOrElse("")
          s"the move opens an attack$attacker$cleared toward $target"
        }
      case _ => None

  private def kingClause(
      relationKind: String,
      detail: StrategyRelationSupport,
      targetSquare: Option[String]
  ): Option[String] =
    relationKind match
      case MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map { king =>
          val cycle = if detail.cycleMoves.nonEmpty then detail.cycleMoves else detail.checkingMoves
          val checks = cycle.map(displayUci).take(2)
          val checkText =
            if checks.nonEmpty then s"checking cycle ${joinTerms(checks)} repeats"
            else "the checking sequence repeats"
          val ply = detail.repeatedPositionPly.map(value => s" by ply $value").getOrElse("")
          s"$checkText against the king on $king$ply"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map { king =>
          val move = detail.trappingMove.map(value => s" after ${displayUci(value)}").getOrElse("")
          s"the replay shows a stalemate resource around the king on $king$move"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map { king =>
          val checkers = detail.checkerSquares.take(2)
          val checkerText =
            if checkers.nonEmpty then s" from ${joinTerms(checkers)}"
            else ""
          val mover = pieceWithoutArticle(detail.moverSquare, detail.moverRole, "piece")
          mover match
            case Some(text) => s"$text gives double check on the king on $king$checkerText"
            case None       => s"the king on $king is checked by two lines$checkerText"
        }
      case MoveReviewExchangeAnalyzer.RelationKind.BackRankMate | MoveReviewExchangeAnalyzer.RelationKind.MateNet =>
        targetSquare.orElse(detail.kingSquare).orElse(detail.targetSquare).map { king =>
          val pattern =
            relationKind match
              case MoveReviewExchangeAnalyzer.RelationKind.BackRankMate => "back-rank mate"
              case _ => detail.patternId.map(humanize).filter(_.nonEmpty).getOrElse("mate net")
          detail.matingMove
            .map(move => s"${displayUci(move)} delivers $pattern against the king on $king")
            .getOrElse(s"$pattern targets the king on $king")
        }
      case MoveReviewExchangeAnalyzer.RelationKind.GreekGift =>
        targetSquare.orElse(detail.targetSquare).map { target =>
          val bishop = detail.bishopSquare.map(square => s" from $square").getOrElse("")
          s"the sacrifice entry$bishop points at $target"
        }
      case _ => None

  private def multiTargetClause(
      relationKind: String,
      detail: StrategyRelationSupport
  ): Option[String] =
    relationKind match
      case MoveReviewExchangeAnalyzer.RelationKind.Fork =>
        detail.attackerSquare.map { attacker =>
          val attackerText = pieceWithoutArticle(Some(attacker), detail.attackerRole, "piece").getOrElse(s"piece on $attacker")
          val targets = describedPieces(detail.targetSquares.take(3), detail.targetRoles.take(3))
          val targetText =
            if targets.nonEmpty then s" forks ${joinTerms(targets)}"
            else " attacks multiple targets"
          s"$attackerText$targetText"
        }
      case _ => None

  private def focusClause(
      relationKind: String,
      focusSquares: List[String],
      targetSquare: Option[String]
  ): Option[String] =
    relationKind match
      case MoveReviewExchangeAnalyzer.RelationKind.XRay |
          MoveReviewExchangeAnalyzer.RelationKind.Clearance |
          MoveReviewExchangeAnalyzer.RelationKind.Battery |
          MoveReviewExchangeAnalyzer.RelationKind.Pin |
          MoveReviewExchangeAnalyzer.RelationKind.Skewer |
          MoveReviewExchangeAnalyzer.RelationKind.Interference |
          MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack =>
        targetSquare.map(target => s"the geometry stays bound to $target")
      case MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece |
          MoveReviewExchangeAnalyzer.RelationKind.Domination |
          MoveReviewExchangeAnalyzer.RelationKind.HangingPiece =>
        targetSquare.map(target => s"the target remains $target in the replay")
      case _ =>
        Option.when(focusSquares.size >= 2)(s"the support is bound to ${joinTerms(focusSquares.take(2))}")

  private def pinOrSkewerClause(
      verb: String,
      frontSquare: Option[String],
      frontRole: Option[String],
      backSquare: Option[String],
      backRole: Option[String],
      detail: StrategyRelationSupport,
      targetSquare: Option[String],
      prefix: Option[String]
  ): Option[String] =
    targetSquare.orElse(backSquare).map { target =>
      val lead = prefix.getOrElse("")
      val attacker = detail.attackerSquare.map(square => s"the attacker on $square ").getOrElse("the line ")
      val frontText =
        frontSquare.map(square => pieceWithArticle(square, frontRole, "front piece")).getOrElse("the front piece")
      val backText =
        backSquare
          .map(square =>
            backRole
              .flatMap(pieceRole)
              .map(role => s"to the $role behind on $square")
              .getOrElse(s"to the piece behind on $square")
          )
          .getOrElse(s"to $target")
      s"$lead$attacker$verb $frontText $backText"
    }

  private def relationPath(
      detail: StrategyRelationSupport,
      focusSquares: List[String],
      targetSquare: Option[String]
  ): List[String] =
    (
      List(
        detail.attackerSquare,
        detail.beneficiarySquare,
        detail.clearedSquare,
        detail.frontSquare,
        detail.backSquare,
        detail.pinnedSquare,
        detail.behindSquare,
        detail.blockerSquare,
        targetSquare,
        detail.targetSquare
      ).flatten ++
        detail.targetSquares ++
        focusSquares
    ).distinct

  private def executionRoute(detail: StrategyRelationSupport): Option[String] =
    for
      from <- detail.executionFromSquare
      to <- detail.executionToSquare
    yield s"$from-$to"

  private def pieceRole(raw: String): Option[String] =
    Option(raw).map(_.trim.toLowerCase).flatMap {
      case "p" | "pawn"   => Some("pawn")
      case "n" | "knight" => Some("knight")
      case "b" | "bishop" => Some("bishop")
      case "r" | "rook"   => Some("rook")
      case "q" | "queen"  => Some("queen")
      case "k" | "king"   => Some("king")
      case _               => None
    }

  private def pieceWithArticle(square: String, role: Option[String], fallback: String): String =
    val label = role.flatMap(pieceRole).getOrElse(fallback)
    s"the $label on $square"

  private def pieceWithoutArticle(
      square: Option[String],
      role: Option[String],
      fallback: String
  ): Option[String] =
    square.map { value =>
      val label = role.flatMap(pieceRole).getOrElse(fallback)
      s"$label on $value"
    }

  private def describedPieces(squares: List[String], roles: List[String]): List[String] =
    squares.zipWithIndex.map { case (square, index) =>
      pieceWithArticle(square, roles.lift(index), "target")
    }

  private def displayMaybeMove(value: String): String =
    if value.matches("""[a-h][1-8][a-h][1-8][qrbn]?""") then displayUci(value)
    else value

  private def displayUci(uci: String): String =
    val normalized = uci.trim.toLowerCase
    if normalized.matches("""[a-h][1-8][a-h][1-8][qrbn]?""") then s"${normalized.take(2)}-${normalized.slice(2, 4)}"
    else normalized

  private def joinTerms(values: List[String]): String =
    values.map(_.trim).filter(_.nonEmpty).distinct match
      case Nil           => ""
      case one :: Nil    => one
      case a :: b :: Nil => s"$a and $b"
      case many          => s"${many.dropRight(1).mkString(", ")}, and ${many.last}"

  private def humanize(raw: String): String =
    raw.trim.toLowerCase.replaceAll("[^a-z0-9]+", " ").trim
