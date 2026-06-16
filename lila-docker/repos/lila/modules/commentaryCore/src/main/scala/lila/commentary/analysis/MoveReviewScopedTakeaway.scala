package lila.commentary.analysis

private[commentary] object MoveReviewScopedTakeaway:
  import MoveReviewLocalFact.{
    Admission as LocalFactAdmission,
    Family as LocalFactFamily,
    LineBinding as LocalFactLineBinding,
    Producer as LocalFactProducer
  }

  enum EvidenceTier:
    case PvCoupledLocal

    def key: String =
      this match
        case PvCoupledLocal => "pv_coupled_local"

  enum Source:
    case MoveReviewPvMeaning

    def key: String =
      this match
        case MoveReviewPvMeaning => "move_review_pv_meaning"

  final case class ScopedTakeaway(
      text: String,
      fen: String,
      playedUci: String,
      lineId: Option[String],
      evidenceTier: EvidenceTier,
      source: Source,
      guardrails: List[String]
  )

  private val Guardrails = List(
    "scope:move_review_local",
    "owner:pv_coupled_line",
    "schema:compatibility_projection",
    "requires:admitted_typed_local_fact",
    "excludes:raw_strategy_pack_text",
    "excludes:fallback_provider"
  )

  private val ForbiddenGlobalizers =
    List(
      """(?i)\balways\b""".r,
      """(?i)\bgenerally\b""".r,
      """(?i)\bin every position\b""".r,
      """(?i)\bshared lesson\b""".r,
      """(?i)\bthe lesson is\b""".r,
      """(?i)\bas a rule\b""".r,
      """(?i)\bthe rule is\b""".r
    )

  def build(
      purpose: String,
      played: CommentaryIdeaSurface.PlayedMove,
      evidence: CommentaryIdeaSurface.MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      localFact: LocalFactAdmission
  ): Option[ScopedTakeaway] =
    lineFacts
      .filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci)
      .flatMap { line =>
        renderText(purpose, played, evidence, line, localFact).filter(isAllowedText).map { text =>
          ScopedTakeaway(
            text = text,
            fen = line.first.fenAfter,
            playedUci = played.uci,
            lineId = Some(line.line.lineId),
            evidenceTier = EvidenceTier.PvCoupledLocal,
            source = Source.MoveReviewPvMeaning,
            guardrails = Guardrails
          )
        }
      }

  def isAllowedText(text: String): Boolean =
    val clean = Option(text).getOrElse("").trim
    clean.nonEmpty && !ForbiddenGlobalizers.exists(_.findFirstIn(clean).nonEmpty)

  private def renderText(
      purpose: String,
      played: CommentaryIdeaSurface.PlayedMove,
      evidence: CommentaryIdeaSurface.MoveReviewEvidence,
      line: MoveReviewPvLine.LineFacts,
      localFact: LocalFactAdmission
  ): Option[String] =
    val replySan = line.reply.map(_.san).filter(_.nonEmpty).getOrElse("the reply")
    val continuationSan = line.continuation.map(_.san).filter(_.nonEmpty).getOrElse("the follow-up")
    val checkedReply = s"the checked continuation begins with $replySan"
    val checkedSequence = s"the checked continuation runs through $replySan, then $continuationSan"
    val openingGoalName = evidence.openingGoal.map(_.goalName.toLowerCase).getOrElse("")
    Option.when(admitsPurpose(purpose, localFact))(
      (localFact.producer, localFact.family) match
        case (LocalFactProducer.CertifiedStrategyDelta, LocalFactFamily.Defense) =>
          s"The PV keeps the counterplay-restraint detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader defensive claim."
        case (LocalFactProducer.CertifiedStrategyDelta, LocalFactFamily.PlanSupport) =>
          certifiedStrategyPlanSupportTakeaway(played, localFact, checkedReply)
            .getOrElse(
              s"The PV keeps the plan-support detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader plan claim."
            )
        case (LocalFactProducer.CertifiedStrategyDelta, LocalFactFamily.Pressure) =>
          certifiedStrategyPressureTakeaway(played, localFact, checkedReply)
            .getOrElse(
              s"The PV keeps the positional pressure detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader plan claim."
            )
        case (LocalFactProducer.CertifiedStrategyDelta, LocalFactFamily.LineConsequence) =>
          s"The PV keeps the strategic consequence bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader plan claim."
        case (_, LocalFactFamily.Attack) =>
          s"The PV keeps the local attacking detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader evaluation claim."
        case (_, LocalFactFamily.Pressure) if purpose == "restrict_piece_mobility" =>
          s"The PV keeps the mobility restriction local: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader evaluation claim."
        case (_, LocalFactFamily.Timing) if purpose == "move_order_timing" =>
          s"The PV keeps the move-order detail local: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader timing claim."
        case (LocalFactProducer.TargetPressure, LocalFactFamily.Pressure) =>
          targetPressureTakeaway(played, localFact, checkedReply)
            .getOrElse(
              s"The PV keeps the local tactical detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader evaluation claim."
            )
        case (LocalFactProducer.TacticalMotif, LocalFactFamily.Threat) =>
          tacticalMotifTakeaway(played, localFact, checkedReply)
            .getOrElse(
              s"The PV keeps the local tactical detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader evaluation claim."
            )
        case (LocalFactProducer.RelationWitness, LocalFactFamily.Threat) =>
          relationWitnessTakeaway(played, localFact, checkedReply)
            .getOrElse(
              s"The PV keeps the local tactical detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader evaluation claim."
            )
        case (_, LocalFactFamily.Threat | LocalFactFamily.Pressure) =>
          s"The PV keeps the local tactical detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader evaluation claim."
        case (_, LocalFactFamily.Defense) =>
          s"The PV keeps the defensive detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader threat claim."
        case (_, LocalFactFamily.Endgame) =>
          endgameTakeaway(played, localFact, checkedSequence)
            .getOrElse(s"The line keeps the concrete endgame detail local to ${played.san}: $checkedSequence.")
        case (_, LocalFactFamily.Capture) =>
          captureTakeaway(played, localFact, checkedReply)
            .getOrElse(
              s"The line clarifies the exchange: ${played.san} is met by $replySan, so the sequence remains bounded to ${played.toKey}."
            )
        case (_, LocalFactFamily.KingSafety) =>
          castlingTakeaway(played, localFact, checkedSequence)
            .getOrElse(s"The line keeps the king-safety detail bounded: ${played.san} is the move under review, and $checkedSequence.")
        case (_, LocalFactFamily.OpeningGoal) =>
          purpose match
            case "quiet_development" if line.continuation.exists(move => MoveReviewPvLine.normalizeUci(move.uci) == "d2d3") =>
              s"The checked line stays local to ${played.san}: after $replySan, $continuationSan remains the checked continuation."
            case "quiet_development" =>
              s"The checked line stays local to ${played.san}: $checkedSequence."
            case "center_break_setup" =>
              s"The checked line keeps the setup bounded: ${played.san} comes first, then $continuationSan continues the line."
            case "challenge_center" =>
              s"The checked line keeps the center sequence bounded: ${played.san} is the move under review, and $continuationSan continues the line."
            case _ =>
              s"The checked line stays local to ${played.san}: $checkedSequence."
        case (_, LocalFactFamily.LineConsequence) =>
          purpose match
            case "clarify_exchange" =>
              s"The checked line keeps the exchange consequence local to ${played.san}: $checkedSequence."
            case "clarify_delayed_capture" =>
              s"The checked line keeps the delayed pawn capture local to ${played.san}: $checkedSequence."
            case "show_immediate_pawn_capture" =>
              s"The checked line keeps the immediate pawn capture local to ${played.san}: $checkedSequence."
            case "show_immediate_reply_pressure" =>
              s"The checked line keeps the immediate reply pressure local to ${played.san}: $checkedSequence."
            case "show_played_target_pressure" =>
              s"The checked line keeps the target-pressure detail local to ${played.san}: $checkedSequence."
            case "center_break_setup" | "challenge_center" =>
              s"The checked line keeps the central consequence local to ${played.san}: $checkedSequence."
            case "force_sequence" =>
              s"The checked line keeps the forcing detail local to ${played.san}: $checkedSequence."
            case _ =>
              s"The checked line keeps the line consequence local to ${played.san}: $checkedSequence."
        case _ =>
          purpose match
            case "quiet_development" if line.continuation.exists(move => MoveReviewPvLine.normalizeUci(move.uci) == "d2d3") =>
              s"The checked line stays local to ${played.san}: after $replySan, $continuationSan remains the checked continuation."
            case "quiet_development" =>
              s"The checked line stays local to ${played.san}: $checkedSequence."
            case "center_break_setup" =>
              s"The checked line keeps the setup bounded: ${played.san} comes first, then $continuationSan continues the line."
            case "challenge_center" =>
              s"The checked line keeps the center sequence bounded: ${played.san} is the move under review, and $continuationSan continues the line."
            case "king_safety_first" =>
              s"The line keeps the king-safety sequence bounded: ${played.san} is the move under review, and $continuationSan continues the line."
            case "create_tactical_threat" if openingGoalName.nonEmpty =>
              s"The PV keeps the opening goal bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader evaluation claim."
            case "create_tactical_threat" =>
              s"The PV keeps the local tactical detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader evaluation claim."
            case "answer_direct_threat" =>
              s"The PV keeps the defensive detail bounded: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader threat claim."
            case "restrict_piece_mobility" =>
              s"The PV keeps the mobility restriction local: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader evaluation claim."
            case "move_order_timing" =>
              s"The PV keeps the move-order detail local: ${played.san} is the move under review, $checkedReply, and the line does not authorize a broader timing claim."
            case "resolve_capture_tension" =>
              s"The PV shows the capture tension clearly: ${played.san} can be answered by $replySan, so the point is what remains after the recapture rather than the capture alone."
            case "clarify_exchange" =>
              s"The line clarifies the exchange: ${played.san} is met by $replySan, so the sequence resolves which trade remains on ${played.toKey}."
            case "improve_endgame_activity" =>
              endgameTakeaway(played, localFact, checkedSequence)
                .getOrElse(s"The line keeps the concrete endgame detail local to ${played.san}: $checkedSequence.")
            case _ =>
              s"The checked line stays local to ${played.san}: $checkedSequence."
    )

  private def endgameTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedSequence: String
  ): Option[String] =
    anchorValue(localFact, "endgame_fact") match
      case Some("opposition") =>
        for
          kingSquare <- anchorValue(localFact, "king_square")
          opposingKing <- anchorValue(localFact, "opposing_king_square")
          oppositionType <- anchorValue(localFact, "opposition_type")
        yield
          s"The line keeps the $oppositionType opposition detail local to ${played.san}: the move puts the king on $kingSquare against the king on $opposingKing, and $checkedSequence."
      case Some("king_activity") =>
        anchorValue(localFact, "king_square").map { kingSquare =>
          s"The line keeps the king-activity detail local to ${played.san}: the move reaches $kingSquare, and $checkedSequence."
        }
      case _ => None

  private def targetPressureTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedReply: String
  ): Option[String] =
    for
      target <- anchorValue(localFact, "target_square")
      role <- anchorValue(localFact, "target_role")
    yield
      s"The PV keeps the pressure on the ${targetPressureLabel(target, role)} local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."

  private def targetPressureLabel(target: String, role: String): String =
    val cleanRole = role.trim
    val cleanTarget = target.trim
    if cleanRole.nonEmpty && cleanTarget.nonEmpty then s"$cleanTarget $cleanRole"
    else if cleanTarget.nonEmpty then cleanTarget
    else cleanRole

  private def tacticalMotifTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedReply: String
  ): Option[String] =
    anchorValue(localFact, "tactical_kind").flatMap {
      case "pin" =>
        for
          pinned <- pieceAnchorLabel(localFact, "pinned_square", "pinned_role")
          behind <- pieceAnchorLabel(localFact, "behind_square", "behind_role")
        yield
          s"The PV keeps the pin of the $pinned to the $behind local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."
      case "skewer" =>
        for
          front <- pieceAnchorLabel(localFact, "front_square", "front_role")
          back <- pieceAnchorLabel(localFact, "back_square", "back_role")
        yield
          s"The PV keeps the skewer through the $front toward the $back local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."
      case "fork" =>
        val targets = indexedTargetLabels(localFact)
        Option.when(targets.nonEmpty)(
          s"The PV keeps the fork targets ${joinLabels(targets)} local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."
        )
      case "discovered_attack" =>
        for
          revealed <- pieceAnchorLabel(localFact, "revealed_square", "revealed_role")
          target <- pieceAnchorLabel(localFact, "target_square", "target_role")
        yield
          s"The PV keeps the discovered attack from the $revealed toward the $target local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."
      case "trapped_piece" =>
        pieceAnchorLabel(localFact, "trapped_square", "trapped_role").map { trapped =>
          s"The PV keeps the trap on the $trapped local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."
        }
      case "check" =>
        pieceAnchorLabel(localFact, "king_square", "king_role").map { king =>
          s"The PV keeps the check on the $king local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."
        }
      case _ => None
    }

  private def captureTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedReply: String
  ): Option[String] =
    anchorValue(localFact, "captured_square").map { square =>
      val captured =
        anchorValue(localFact, "captured_role")
          .map(role => targetPressureLabel(square, role))
          .getOrElse(square)
      val queenTrade =
        for
          tradeSquare <- anchorValue(localFact, "followup_queen_trade_square")
          captureSan <- anchorValue(localFact, "followup_queen_trade_capture_san")
          recaptureSan <- anchorValue(localFact, "followup_queen_trade_recapture_san")
        yield s"The checked line keeps the capture of the $captured local to ${played.san}: after that capture, $captureSan and $recaptureSan trade queens on $tradeSquare."
      queenTrade.getOrElse(
        s"The checked line keeps the capture of the $captured local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."
      )
    }

  private def relationWitnessTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedReply: String
  ): Option[String] =
    anchorValue(localFact, "relation_kind").flatMap {
      case "discovered_attack" =>
        for
          attacker <- anchorValue(localFact, "attacker_square")
          target <- anchorValue(localFact, "target_square")
        yield
          s"The PV keeps the discovered attack from $attacker toward $target local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."
      case "overload" =>
        for defender <- anchorValue(localFact, "defender_square")
        yield
          val duties = anchorValues(localFact, "duty_square")
          val dutyText =
            if duties.nonEmpty then s" across ${joinLabels(duties)}"
            else ""
          s"The PV keeps the overload on the $defender defender$dutyText local to ${played.san}: $checkedReply, and the line does not authorize a broader evaluation claim."
      case _ => None
    }

  private def certifiedStrategyPressureTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedReply: String
  ): Option[String] =
    outpostOccupationTakeaway(played, localFact, checkedReply)
      .orElse(exactTargetFixationTakeaway(played, localFact, checkedReply))
      .orElse(anchorValue(localFact, "strategic_idea_kind").flatMap {
      case "line_occupation" =>
        for
          file <- anchorValue(localFact, "line_file")
          status <- anchorValue(localFact, "line_file_status")
        yield
          val targetText = anchorValue(localFact, "line_target").map(square => s" toward $square").getOrElse("")
          s"The PV keeps the ${lineFileStatusLabel(status)} $file-file occupation$targetText local to ${played.san}: $checkedReply, and the line does not authorize a broader plan claim."
      case "target_fixing" =>
        for target <- anchorValue(localFact, "target_fixing_square")
        yield
          val targetText =
            anchorValue(localFact, "target_fixing_target_kind") match
              case Some("weak_square") => s"the weak $target square"
              case _                   => s"the $target target square"
          s"The PV keeps the pressure on $targetText local to ${played.san}: $checkedReply, and the line does not authorize a broader plan claim."
      case "king_attack_build_up" =>
        for target <- anchorValue(localFact, "attack_lane_square")
        yield
          val axis = anchorValue(localFact, "attack_lane_axis").map(lineFileStatusLabel).getOrElse("line")
          s"The PV keeps the $axis attack lane toward $target local to ${played.san}: $checkedReply, and the line does not authorize a broader plan claim."
      case "space_gain_or_restriction" =>
        for file <- anchorValue(localFact, "space_gain_file")
        yield
          val side = anchorValue(localFact, "space_gain_side").map(side => s" on the $side").getOrElse("")
          s"The PV keeps the $file-pawn space gain$side local to ${played.san}: $checkedReply, and the line does not authorize a broader plan claim."
      case _ => None
    })

  private def outpostOccupationTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedReply: String
  ): Option[String] =
    for
      square <- anchorValue(localFact, "outpost_square")
      role <- anchorValue(localFact, "outpost_piece_role")
    yield
      s"The PV keeps the $role outpost on $square local to ${played.san}: $checkedReply, and the line does not authorize a broader plan claim."

  private def exactTargetFixationTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedReply: String
  ): Option[String] =
    for target <- anchorValue(localFact, "exact_target_square")
    yield
      s"The PV keeps pressure on the $target pawn target local to ${played.san}: $checkedReply, and the line does not authorize a broader plan claim."

  private def certifiedStrategyPlanSupportTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedReply: String
  ): Option[String] =
    anchorValue(localFact, "strategic_idea_kind").flatMap {
      case "pawn_break" =>
        anchorValue(localFact, "pawn_break_file").map { file =>
          s"The PV keeps the $file-pawn break local to ${played.san}: $checkedReply, and the line does not authorize a broader plan claim."
        }
      case _ => None
    }

  private def pieceAnchorLabel(
      localFact: LocalFactAdmission,
      squareKey: String,
      roleKey: String
  ): Option[String] =
    for
      square <- anchorValue(localFact, squareKey)
      role <- anchorValue(localFact, roleKey)
    yield targetPressureLabel(square, role)

  private def indexedTargetLabels(localFact: LocalFactAdmission): List[String] =
    (1 to 4).toList
      .flatMap(index => pieceAnchorLabel(localFact, s"target_${index}_square", s"target_${index}_role"))
      .distinct

  private def joinLabels(labels: List[String]): String =
    labels match
      case Nil          => ""
      case one :: Nil   => one
      case first :: second :: Nil => s"$first and $second"
      case many         => s"${many.dropRight(1).mkString(", ")}, and ${many.last}"

  private def lineFileStatusLabel(status: String): String =
    status match
      case "semi_open" => "semi-open"
      case "open"      => "open"
      case other       => other.replace('_', '-')

  private def castlingTakeaway(
      played: CommentaryIdeaSurface.PlayedMove,
      localFact: LocalFactAdmission,
      checkedSequence: String
  ): Option[String] =
    for
      kingSquare <- anchorValue(localFact, "king_square")
      rookSquare <- anchorValue(localFact, "rook_square")
    yield
      s"The line keeps the castling detail local to ${played.san}: the king reaches $kingSquare, the rook lands on $rookSquare, and $checkedSequence."

  private def anchorValue(localFact: LocalFactAdmission, key: String): Option[String] =
    localFact.anchors.collectFirst {
      case anchor if anchor.key == key && anchor.value.trim.nonEmpty => anchor.value.trim
    }

  private def anchorValues(localFact: LocalFactAdmission, key: String): List[String] =
    localFact.anchors.collect {
      case anchor if anchor.key == key && anchor.value.trim.nonEmpty => anchor.value.trim
    }.distinct

  private def admitsPurpose(
      purpose: String,
      localFact: LocalFactAdmission
  ): Boolean =
    localFact.lineBinding == LocalFactLineBinding.PvCoupled &&
      allowedFamilies(purpose).contains(localFact.family)

  private def allowedFamilies(purpose: String): Set[LocalFactFamily] =
    purpose match
      case "quiet_development" | "center_break_setup" | "challenge_center" | "local_piece_improvement" |
          "local_pawn_setup" =>
        Set(LocalFactFamily.LineConsequence, LocalFactFamily.OpeningGoal)
      case "king_safety_first" =>
        Set(LocalFactFamily.KingSafety, LocalFactFamily.OpeningGoal)
      case "create_tactical_threat" =>
        Set(LocalFactFamily.Attack, LocalFactFamily.Threat, LocalFactFamily.Pressure)
      case "answer_direct_threat" | "prevent_counterplay" =>
        Set(LocalFactFamily.Defense)
      case "restrict_piece_mobility" =>
        Set(LocalFactFamily.Pressure)
      case "move_order_timing" =>
        Set(LocalFactFamily.Timing)
      case "resolve_capture_tension" =>
        Set(LocalFactFamily.Capture)
      case "clarify_exchange" | "local_capture" =>
        Set(LocalFactFamily.Capture, LocalFactFamily.LineConsequence)
      case "clarify_delayed_capture" =>
        Set(LocalFactFamily.LineConsequence)
      case "show_immediate_pawn_capture" =>
        Set(LocalFactFamily.LineConsequence)
      case "show_immediate_reply_pressure" =>
        Set(LocalFactFamily.LineConsequence)
      case "show_played_target_pressure" =>
        Set(LocalFactFamily.LineConsequence)
      case "force_sequence" =>
        Set(LocalFactFamily.LineConsequence)
      case "improve_endgame_activity" =>
        Set(LocalFactFamily.Endgame)
      case "advance_plan" =>
        Set(LocalFactFamily.PlanSupport)
      case "quiet_improvement" =>
        Set(LocalFactFamily.LineConsequence, LocalFactFamily.PlanSupport, LocalFactFamily.Pressure)
      case _ =>
        Set.empty
