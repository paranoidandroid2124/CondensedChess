package lila.commentary.analysis

import chess.{ Color, King, Piece, Role, Square }
import lila.commentary.{ MoveReviewMoveRef, MoveReviewPvInterpretation }
import lila.commentary.model.*

private[commentary] object CommentaryIdeaSurface:

  // Data descriptors only.
  final case class PlayedMove(
      uci: String,
      san: String,
      from: Square,
      to: Square,
      piece: Piece,
      afterFen: String,
      capturedRole: Option[Role]
  ):
    def role: Role = piece.role
    def color: Color = piece.color
    def isWhite: Boolean = color.white
    def isCapture: Boolean = capturedRole.nonEmpty || san.contains("x")
    def isCastle: Boolean =
      san.startsWith("O-O") || Set("e1g1", "e1c1", "e8g8", "e8c8").contains(uci)
    def toKey: String = to.key

  // MoveReview basic-lane projection input, not a cross-surface carrier.
  final case class MoveReviewEvidence(
      facts: List[Fact],
      motifs: List[Motif],
      openingGoal: Option[OpeningGoals.Evaluation],
      openingName: Option[String]
  ):
    def endgameFacts: List[Fact] =
      facts.collect {
        case fact: Fact.KingActivity             => fact
        case fact: Fact.Opposition               => fact
        case fact: Fact.RuleOfSquare             => fact
        case fact: Fact.TriangulationOpportunity => fact
        case fact: Fact.RookEndgamePattern       => fact
        case fact: Fact.PawnPromotion            => fact
        case fact: Fact.Zugzwang                 => fact
      }

    def hasCaptureMotif: Boolean =
      motifs.exists(_.isInstanceOf[Motif.Capture])

  final case class MoveReviewIdeaDescriptor(
      ideaKind: String,
      source: String,
      title: String,
      baseProse: String,
      reasonTags: List[String],
      confirms: List[String],
      linePurpose: Option[String],
      tension: Option[String],
      opponentReplyMeaning: Option[String],
      learningPoint: Option[String],
      requiresPvForAdmission: Boolean
  ):
    def prose: String =
      learningPoint.map(point => s"${baseProse.trim} ${point.trim}".trim).getOrElse(baseProse.trim)

    def pvInterpretation(lineFacts: Option[MoveReviewPvLine.LineFacts]): Option[MoveReviewPvInterpretation] =
      for
        purpose <- linePurpose
        line <- lineFacts
      yield
        MoveReviewPvInterpretation(
          linePurpose = purpose,
          confirms = confirms,
          tension = tension.getOrElse("tension_maintained"),
          opponentReplyMeaning = opponentReplyMeaning,
          learningPoint = learningPoint.getOrElse(""),
          supportedByLineId = Some(line.line.lineId),
          confidence = "bounded_local"
        )

  def describe(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    val descriptor =
      MoveReviewIdeaRules.iterator
        .map(_.describe(played, evidence, lineFacts))
        .collectFirst { case Some(desc) => desc }

    descriptor.filter(desc => !desc.requiresPvForAdmission || lineFacts.nonEmpty)

  private final case class MoveReviewIdeaRule(
      name: String,
      describe: (PlayedMove, MoveReviewEvidence, Option[MoveReviewPvLine.LineFacts]) => Option[MoveReviewIdeaDescriptor]
  )

  private val MoveReviewIdeaRules: List[MoveReviewIdeaRule] =
    List(
      MoveReviewIdeaRule("opening_goal", openingDescriptor),
      MoveReviewIdeaRule("castling", (played, _, lineFacts) => castlingDescriptor(played, lineFacts)),
      MoveReviewIdeaRule("tactical", tacticalDescriptor),
      MoveReviewIdeaRule("target", targetDescriptor),
      MoveReviewIdeaRule("capture", captureDescriptor),
      MoveReviewIdeaRule("endgame", endgameDescriptor)
    )

  // MoveReview admission rules. Keep descriptor selection before rendering fields.
  private def openingDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    evidence.openingGoal.map { goal =>
      val reasonTags = List("opening_goal", slug(goal.goalName))
      val purpose = lineFacts.flatMap(line => PvMeaning.classifyOpeningPurpose(played, evidence, line.continuation, reasonTags))
      val openingPart = evidence.openingName.map(name => s" in the $name").getOrElse("")
      val supported = goal.supportedEvidence.map(_.trim).filter(_.nonEmpty).mkString(", ")
      val supportText =
        if supported.nonEmpty then s" The existing opening-goal evidence is: $supported."
        else ""
      descriptor(
        ideaKind = "opening_goal",
        source = "opening_goal",
        title = s"${played.san} advances ${goal.goalName}",
        baseProse = s"${played.san}$openingPart is admitted through ${goal.goalName}, not by the opening name alone.$supportText",
        reasonTags = reasonTags,
        linePurpose = purpose,
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = false
      )
    }

  private def castlingDescriptor(
      played: PlayedMove,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    Option.when(played.isCastle) {
      val reasonTags = List("king_safety")
      descriptor(
        ideaKind = "king_safety",
        source = "basic_move_explanation",
        title = s"${played.san} improves king safety",
        baseProse = s"${played.san} improves king safety before the position asks for a more concrete central or tactical decision.",
        reasonTags = reasonTags,
        linePurpose = Some("king_safety_first"),
        played = played,
        evidence = MoveReviewEvidence(Nil, Nil, None, None),
        lineFacts = lineFacts,
        requiresPvForAdmission = false
      )
    }

  private def tacticalDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    val factKind =
      if evidence.facts.exists(_.isInstanceOf[Fact.Fork]) then Some("fork")
      else if evidence.facts.exists(_.isInstanceOf[Fact.Pin]) then Some("pin")
      else if evidence.facts.exists(_.isInstanceOf[Fact.Skewer]) then Some("skewer")
      else if evidence.facts.exists(_.isInstanceOf[Fact.DoubleCheck]) then Some("double_check")
      else None

    factKind.flatMap { kind =>
      Option.when(lineFacts.nonEmpty) {
        val label = kind.replace("_", " ")
        val title =
          if kind == "double_check" then s"${played.san} creates a tactical threat"
          else s"${played.san} creates a $label"
        val prose =
          kind match
            case "fork" =>
              s"${played.san} is admitted because the canonical facts mark a fork, so the explanation stays tied to the verified targets."
            case "pin" =>
              s"${played.san} is admitted because the canonical facts mark a pin, so the explanation stays tied to the pinned piece and the piece behind it."
            case "skewer" =>
              s"${played.san} is admitted because the canonical facts mark a skewer, so the explanation stays tied to the front and back targets."
            case _ =>
              s"${played.san} is admitted because the canonical facts mark a tactical pattern, so the explanation stays tied to the verified targets."
        descriptor(
          ideaKind = kind,
          source = "canonical_fact",
          title = title,
          baseProse = prose,
          reasonTags = evidenceTags(evidence.facts, evidence.motifs),
          linePurpose = Some("create_tactical_threat"),
          played = played,
          evidence = evidence,
          lineFacts = lineFacts,
          requiresPvForAdmission = true
        )
      }
    }

  private def targetDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    Option.when(
      evidence.facts.exists(f => f.isInstanceOf[Fact.HangingPiece] || f.isInstanceOf[Fact.TargetPiece]) &&
        lineFacts.flatMap(_.reply).nonEmpty
    ) {
      descriptor(
        ideaKind = "direct_threat",
        source = "canonical_fact",
        title = s"${played.san} asks a concrete question",
        baseProse = s"${played.san} is admitted because the canonical facts identify a concrete target for the opponent to answer.",
        reasonTags = evidenceTags(evidence.facts, evidence.motifs),
        linePurpose = Some("answer_direct_threat"),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true
      )
    }

  private def captureDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    Option.when(evidence.hasCaptureMotif && played.isCapture && isImmediateRecapture(played.toKey, lineFacts.flatMap(_.reply))) {
      val purpose =
        if played.capturedRole.exists(_ != chess.Pawn) || played.role != chess.Pawn then "clarify_exchange"
        else "resolve_capture_tension"
      descriptor(
        ideaKind = if purpose == "clarify_exchange" then "exchange_clarified" else "capture_tension",
        source = "basic_move_explanation",
        title =
          if purpose == "clarify_exchange" then s"${played.san} clarifies the exchange"
          else s"${played.san} resolves the capture tension",
        baseProse = s"${played.san} serves a concrete local purpose.",
        reasonTags = List("capture_sequence"),
        linePurpose = Some(purpose),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true
      )
    }

  private def endgameDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    Option.when(evidence.endgameFacts.nonEmpty && lineFacts.nonEmpty) {
      descriptor(
        ideaKind = "endgame_activity",
        source = "canonical_fact",
        title = s"${played.san} improves endgame activity",
        baseProse = s"${played.san} is admitted through exact endgame facts, so the explanation stays local to the verified endgame activity.",
        reasonTags = evidenceTags(evidence.facts, evidence.motifs),
        linePurpose = Some("improve_endgame_activity"),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true
      )
    }

  private def descriptor(
      ideaKind: String,
      source: String,
      title: String,
      baseProse: String,
      reasonTags: List[String],
      linePurpose: Option[String],
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      requiresPvForAdmission: Boolean
  ): MoveReviewIdeaDescriptor =
    val confirms = PvMeaning.confirmedFacts(linePurpose, evidence, reasonTags)
    MoveReviewIdeaDescriptor(
      ideaKind = ideaKind,
      source = source,
      title = title,
      baseProse = baseProse,
      reasonTags = reasonTags.distinct,
      confirms = confirms,
      linePurpose = linePurpose,
      tension = linePurpose.map(purpose => PvMeaning.classifyTension(purpose, played, lineFacts.flatMap(_.reply), lineFacts.flatMap(_.continuation))),
      opponentReplyMeaning = lineFacts.flatMap(_.reply).flatMap(reply => PvMeaning.classifyOpponentReply(played, evidence, reply)),
      learningPoint = linePurpose.flatMap(purpose => PvMeaning.learningPoint(purpose, played, evidence, lineFacts)),
      requiresPvForAdmission = requiresPvForAdmission
    )

  private object PvMeaning:
    def classifyOpeningPurpose(
        played: PlayedMove,
        evidence: MoveReviewEvidence,
        continuation: Option[MoveReviewMoveRef],
        reasonTags: List[String]
    ): Option[String] =
      evidence.openingGoal.map { goal =>
        val goalName = goal.goalName.toLowerCase
        if continuation.exists(move => isCentralBreak(move.uci, played.isWhite)) then "center_break_setup"
        else if continuation.exists(move => Set("d2d3", "d7d6", "e2e3", "e7e6").contains(MoveReviewPvLine.normalizeUci(move.uci))) then
          "quiet_development"
        else if goalName.contains("challenge") || goalName.contains("break") || goalName.contains("sicilian") then "challenge_center"
        else if reasonTags.contains("king_safety") then "king_safety_first"
        else "quiet_development"
      }

    def classifyTension(
        purpose: String,
        played: PlayedMove,
        reply: Option[MoveReviewMoveRef],
        continuation: Option[MoveReviewMoveRef]
    ): String =
      if purpose == "resolve_capture_tension" || purpose == "clarify_exchange" then "center_released"
      else if continuation.exists(move => isCentralBreak(move.uci, played.isWhite)) then "delayed_center_break"
      else if purpose == "challenge_center" && Set("c2c4", "c7c5").contains(played.uci) then "tension_maintained"
      else if isCentralBreak(played.uci, played.isWhite) then "immediate_center_break"
      else if (reply.toList ++ continuation.toList).exists(move => isCenterCapture(move.uci)) then "center_released"
      else "tension_maintained"

    def classifyOpponentReply(
        played: PlayedMove,
        evidence: MoveReviewEvidence,
        reply: MoveReviewMoveRef
    ): Option[String] =
      val uci = MoveReviewPvLine.normalizeUci(reply.uci)
      val opening = evidence.openingName.map(_.toLowerCase).getOrElse("")
      if opening.contains("ruy lopez") && uci == "a7a6" then Some("asks_piece_commitment")
      else if played.isWhite && (opening.contains("italian") || played.uci == "f1c4") && uci == "g8f6" then Some("attacks_center_pawn")
      else if played.isCapture && isImmediateRecapture(played.toKey, Some(reply)) then Some("recaptures")
      else if evidence.facts.exists(f => f.isInstanceOf[Fact.HangingPiece] || f.isInstanceOf[Fact.TargetPiece]) then Some("addresses_target")
      else if Set("e7e6", "d7d6", "c7c6", "e2e3", "d2d3", "c2c3").contains(uci) then Some("reinforces_center")
      else if Set("c7c5", "c2c4", "d7d5", "d2d4", "e7e5", "e2e4").contains(uci) then Some("challenges_center")
      else if uci.endsWith("c6") || uci.endsWith("f6") || uci.endsWith("c3") || uci.endsWith("f3") then Some("develops_and_contests")
      else None

    def confirmedFacts(
        linePurpose: Option[String],
        evidence: MoveReviewEvidence,
        reasonTags: List[String]
    ): List[String] =
      val values = scala.collection.mutable.LinkedHashSet.empty[String]
      reasonTags.foreach(values += _)
      evidence.openingGoal.foreach(_ => values += "opening_goal")
      evidenceTags(evidence.facts, evidence.motifs).foreach(values += _)
      linePurpose.foreach {
        case "center_break_setup"       => values += "center_break_setup"
        case "resolve_capture_tension"  => values += "capture_tension"
        case "answer_direct_threat"     => values += "direct_threat"
        case "clarify_exchange"         => values += "exchange_clarified"
        case "improve_endgame_activity" => values += "endgame_activity"
        case "create_tactical_threat"   => values += "tactical_threat"
        case _                          =>
      }
      values.toList

    def learningPoint(
        purpose: String,
        played: PlayedMove,
        evidence: MoveReviewEvidence,
        lineFacts: Option[MoveReviewPvLine.LineFacts]
    ): Option[String] =
      lineFacts.map { line =>
        val replySan = line.reply.map(_.san).filter(_.nonEmpty).getOrElse("the reply")
        val continuationSan = line.continuation.map(_.san).filter(_.nonEmpty).getOrElse("the follow-up")
        val openingName = evidence.openingGoal.map(_.goalName.toLowerCase).getOrElse("")
        purpose match
          case "quiet_development" if line.continuation.exists(move => MoveReviewPvLine.normalizeUci(move.uci) == "d2d3") =>
            s"$replySan asks about the e4 pawn, and $continuationSan shows the quiet setup: keep ${played.san} useful, support e4, and postpone the central break."
          case "quiet_development" =>
            s"The line keeps the purpose local: ${played.san} improves the setup, $replySan asks for a response, and $continuationSan shows how the development is maintained."
          case "center_break_setup" =>
            s"The line shows the setup idea: ${played.san} comes first, then $continuationSan tests whether the center can be opened on better terms."
          case "challenge_center" =>
            s"The line keeps the center question alive: ${played.san} starts the challenge while $continuationSan shows how the structure is supported."
          case "king_safety_first" =>
            s"The line shows king safety first: ${played.san} gets the king safe before deciding how or when to open the center."
          case "create_tactical_threat" if openingName.nonEmpty =>
            s"The PV keeps the opening goal bounded: ${played.san} is the move under review, $replySan is the first answer, and the line does not authorize a broader evaluation claim."
          case "create_tactical_threat" =>
            s"The PV keeps the tactical point concrete: ${played.san} creates the verified target pattern, and $replySan is the first line response to that pressure."
          case "answer_direct_threat" =>
            s"${played.san} creates a direct target, and $replySan is the reply that asks the moved piece to prove the threat instead of letting it continue for free."
          case "resolve_capture_tension" =>
            s"The PV shows the capture tension clearly: ${played.san} can be answered by $replySan, so the lesson is what remains after the recapture rather than the capture alone."
          case "clarify_exchange" =>
            s"The line clarifies the exchange: ${played.san} is met by $replySan, so the sequence resolves which trade remains on ${played.toKey}."
          case "improve_endgame_activity" if evidence.endgameFacts.exists(_.isInstanceOf[Fact.KingActivity]) =>
            s"The line shows king activity in the endgame: ${played.san} improves the king first, and $continuationSan shows how that activity starts to matter."
          case "improve_endgame_activity" =>
            s"The line shows endgame activity in concrete terms: ${played.san} changes the verified endgame feature, and $continuationSan shows the next local use."
          case _ =>
            s"The line keeps the purpose local: ${played.san} sets the idea, $replySan asks for a response, and $continuationSan shows how it is maintained."
      }

  // FactProjection: canonical Fact -> surface wording, tags, IDs, and selection policy.
  def statement(bead: Int, fact: Fact, ctx: NarrativeContext): Option[String] =
    fact match
      case Fact.HangingPiece(square, role, attackers, defenders, _) =>
        hangingStatement(
          bead = bead,
          square = square,
          role = role,
          attackers = attackers,
          defenders = defenders,
          tier = StandardCommentaryClaimPolicy.hangingTier(ctx, square, role, attackers, defenders)
        )

      case Fact.TargetPiece(square, role, attackers, defenders, _) =>
        if StandardCommentaryClaimPolicy.quietStandardPosition(ctx) &&
            !StandardCommentaryClaimPolicy.allowsAmberTier(ctx)
        then None
        else
          val balance = attackDefenseBalance(attackers, defenders)
          Some(
            NarrativeLexicon.pick(bead, List(
              s"Pressure can build against the ${roleLabel(role)} on ${square.key} ($balance).",
              s"The ${roleLabel(role)} on ${square.key} is a practical point to watch ($balance).",
              s"The ${roleLabel(role)} on ${square.key} can become a target if move order slips ($balance)."
            ))
          )

      case Fact.Pin(_, _, pinned, pinnedRole, behind, behindRole, isAbsolute, _) =>
        val abs = if isAbsolute then " (absolute)" else ""
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The ${roleLabel(pinnedRole)} on ${pinned.key} is pinned$abs to the ${roleLabel(behindRole)} on ${behind.key}.",
            s"There's a pin: the ${roleLabel(pinnedRole)} on ${pinned.key} cannot move without exposing the ${roleLabel(behindRole)} on ${behind.key}.",
            s"${pinned.key} is pinned, leaving the ${roleLabel(pinnedRole)} with limited mobility.",
            s"The pin on ${pinned.key} slows coordination of that ${roleLabel(pinnedRole)}, costing valuable tempi.",
            s"The pin restrains the ${roleLabel(pinnedRole)} on ${pinned.key}, reducing practical flexibility."
          ))
        )

      case Fact.Fork(attacker, attackerRole, targets, _) =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The ${roleLabel(attackerRole)} on ${attacker.key} has a fork idea against ${targetText(targets)}.",
            s"Watch for a fork by the ${roleLabel(attackerRole)} on ${attacker.key} hitting ${targetText(targets)}.",
            s"A fork motif is in the air: ${attacker.key} can attack ${targetText(targets)}."
          ))
        )

      case Fact.WeakSquare(square, color, reason, _) =>
        val owner = color.name.toLowerCase
        val why = reason.trim
        val detail = if why.nonEmpty then s" ($why)" else ""
        Some(
          NarrativeLexicon.pick(bead, List(
            s"${square.key} is a weak square for $owner$detail.",
            s"The square ${square.key} looks vulnerable$detail.",
            s"A potential outpost on ${square.key} appears$detail."
          ))
        )

      case Fact.Outpost(square, role, _) =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"${square.key} can serve as an outpost for a ${roleLabel(role)}.",
            s"An outpost on ${square.key} could be valuable for a ${roleLabel(role)}.",
            s"Keep ${square.key} in mind as an outpost square."
          ))
        )

      case Fact.Opposition(_, _, _, isDirect, oppositionType, _) =>
        val kind =
          if oppositionType.nonEmpty && !oppositionType.equalsIgnoreCase("None") then s"${oppositionType.toLowerCase} opposition"
          else if isDirect then "direct opposition"
          else "opposition"
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The kings are in $kind.",
            s"$kind is an important endgame detail.",
            s"King opposition becomes a key factor."
          ))
        )

      case Fact.KingActivity(square, mobility, _, _) =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The king on ${square.key} is active (mobility: $mobility).",
            s"King activity matters: ${square.key} has $mobility safe steps.",
            s"The king on ${square.key} is well-placed for the endgame."
          ))
        )

      case _ => None

  def branchReason(
      fact: Fact,
      hangingTier: Option[StandardCommentaryClaimPolicy.HangingTier] = None
  ): Option[String] =
    fact match
      case Fact.HangingPiece(square, role, _, _, _) =>
        hangingTier match
          case Some(StandardCommentaryClaimPolicy.HangingTier.Red) =>
            Some(s"It also keeps the ${roleLabel(role)} on ${square.key} from becoming a tactical liability.")
          case Some(StandardCommentaryClaimPolicy.HangingTier.Amber) =>
            Some(s"It also keeps pressure from building against the ${roleLabel(role)} on ${square.key}.")
          case _ => None
      case Fact.Pin(_, _, pinned, pinnedRole, _, _, _, _) =>
        Some(s"It reduces the pin pressure against the ${roleLabel(pinnedRole)} on ${pinned.key}.")
      case Fact.WeakSquare(square, _, _, _) =>
        Some(s"It prevents longer-term weakening around ${square.key}.")
      case _ => None

  def consequenceBody(fact: Fact): Option[String] =
    fact match
      case Fact.HangingPiece(square, role, _, defenders, _) if defenders.isEmpty =>
        Some(s"it leaves the ${roleLabel(role)} on ${square.key} hanging.")
      case Fact.Pin(_, _, pinned, pinnedRole, behind, behindRole, _, _) =>
        Some(s"it allows a pin on ${pinned.key}, tying the ${roleLabel(pinnedRole)} to the ${roleLabel(behindRole)} on ${behind.key}.")
      case Fact.Fork(attacker, attackerRole, targets, _) if targets.nonEmpty =>
        Some(s"it allows a fork by the ${roleLabel(attackerRole)} on ${attacker.key} against ${targetText(targets)}.")
      case Fact.Skewer(attacker, attackerRole, front, frontRole, back, backRole, _) =>
        Some(s"it allows a skewer: ${roleLabel(attackerRole)} on ${attacker.key} can hit ${roleLabel(frontRole)} on ${front.key} and then ${roleLabel(backRole)} on ${back.key}.")
      case Fact.WeakSquare(square, _, _, _) =>
        Some(s"it creates a durable weakness on ${square.key}.")
      case _ => None

  def issueConsequence(fact: Fact): Option[String] =
    fact match
      case Fact.WeakSquare(square, _, _, _) =>
        Some(s"Consequence: ${square.key} can become a long-term target.")
      case Fact.HangingPiece(square, role, _, _, _) =>
        Some(s"Consequence: the ${roleLabel(role)} on ${square.key} can become a direct tactical target.")
      case _ => None

  def tags(fact: Fact): List[String] =
    fact match
      case _: Fact.Fork                     => List("fork")
      case _: Fact.Pin                      => List("pin")
      case _: Fact.Skewer                   => List("skewer")
      case _: Fact.HangingPiece             => List("hanging_piece")
      case _: Fact.TargetPiece              => List("direct_threat")
      case _: Fact.DoubleCheck              => List("double_check")
      case _: Fact.KingActivity             => List("king_activity")
      case _: Fact.Opposition               => List("opposition")
      case _: Fact.RuleOfSquare             => List("rule_of_square")
      case _: Fact.RookEndgamePattern       => List("rook_endgame_pattern")
      case _: Fact.PawnPromotion            => List("pawn_promotion")
      case _: Fact.TriangulationOpportunity => List("triangulation")
      case _: Fact.Zugzwang                 => List("zugzwang")
      case _: Fact.WeakSquare               => List("weak_square")
      case _: Fact.Outpost                  => List("outpost")
      case _                                => Nil

  def factTags(facts: Iterable[Fact]): List[String] =
    facts.iterator.flatMap(tags).toList.distinct

  def motifTags(motifs: Iterable[Motif]): List[String] =
    val values = scala.collection.mutable.LinkedHashSet.empty[String]
    motifs.foreach {
      case _: Motif.Capture        => values += "capture_sequence"
      case _: Motif.PawnBreak      => values += "center_break_setup"
      case _: Motif.Centralization => values += "piece_activity"
      case _                       =>
    }
    values.toList

  def evidenceTags(facts: Iterable[Fact], motifs: Iterable[Motif]): List[String] =
    (factTags(facts) ++ motifTags(motifs)).distinct

  def factPriority(fact: Fact): Int =
    fact match
      case _: Fact.HangingPiece => 0
      case _: Fact.Pin          => 1
      case _: Fact.Fork         => 2
      case _: Fact.Skewer       => 3
      case _: Fact.WeakSquare   => 4
      case _                    => 99

  def isTacticalProofFact(fact: Fact): Boolean =
    fact match
      case _: Fact.Fork | _: Fact.Pin | _: Fact.Skewer | _: Fact.HangingPiece => true
      case _                                                                   => false

  def isKeyFactEligible(fact: Fact): Boolean =
    fact match
      case _: Fact.TargetPiece    => false
      case _: Fact.DoubleCheck    => false
      case _: Fact.ActivatesPiece => false
      case _                      => true

  def keyFactPriority(fact: Fact): Int =
    fact match
      case _: Fact.HangingPiece  => 0
      case _: Fact.Pin           => 1
      case _: Fact.Fork          => 2
      case _: Fact.Skewer        => 3
      case _: Fact.PawnPromotion => 4
      case _: Fact.WeakSquare    => 5
      case _: Fact.Outpost       => 6
      case _: Fact.Opposition    => 7
      case _: Fact.KingActivity  => 8
      case _                     => 99

  def canonicalFactId(fact: Fact): Option[String] =
    fact match
      case _: Fact.HangingPiece    => Some("hanging_piece")
      case _: Fact.TargetPiece     => Some("target_piece")
      case _: Fact.Pin             => Some("pin")
      case _: Fact.Skewer          => Some("skewer")
      case _: Fact.Fork            => Some("fork")
      case _: Fact.PawnPromotion   => Some("promotion_race")
      case _: Fact.StalemateThreat => Some("stalemate_resource")
      case _: Fact.DoubleCheck     => Some("double_check")
      case _: Fact.Zugzwang        => Some("zugzwang")
      case _: Fact.Opposition      => Some("opposition")
      case _                       => None

  def motifCorroboratedByFact(rawMotif: String, fact: Fact): Boolean =
    val motif = normalizeMotifKey(rawMotif)
    fact match
      case _: Fact.Pin =>
        motif.contains("pin") || motif.contains("xray")
      case _: Fact.Skewer =>
        motif.contains("skewer") || motif.contains("xray")
      case _: Fact.Fork =>
        motif.contains("fork") || motif.contains("deflection")
      case _: Fact.HangingPiece =>
        motif.contains("trapped_piece") || motif.contains("battery")
      case _: Fact.WeakSquare =>
        List("minority_attack", "color_complex", "bad_bishop", "good_bishop", "outpost").exists(motif.contains)
      case _: Fact.Outpost =>
        List("outpost", "knight_domination", "maneuver", "knight_vs_bishop").exists(motif.contains)
      case _: Fact.Opposition =>
        motif.contains("opposition")
      case _: Fact.KingActivity =>
        motif.contains("king_cut_off") || motif.contains("passed_pawn")
      case _ => false

  private val CenterSquares: Set[String] = Set("d4", "e4", "d5", "e5")

  private def toSquare(uci: String): Option[String] =
    val move = MoveReviewPvLine.normalizeUci(uci)
    Option.when(move.length >= 4)(move.slice(2, 4))

  private def isCaptureLike(move: MoveReviewMoveRef): Boolean =
    val uci = MoveReviewPvLine.normalizeUci(move.uci)
    move.san.contains("x") ||
      (uci.length >= 4 && uci.charAt(0) != uci.charAt(2) && move.san.headOption.forall(!"KQRBN".contains(_)))

  private def isImmediateRecapture(targetSquare: String, reply: Option[MoveReviewMoveRef]): Boolean =
    reply.exists(move => isCaptureLike(move) && toSquare(move.uci).contains(targetSquare))

  private def isCentralBreak(uci: String, whiteMove: Boolean): Boolean =
    val move = MoveReviewPvLine.normalizeUci(uci)
    val whiteBreaks = Set("d2d4", "e2e4", "c2c4")
    val blackBreaks = Set("d7d5", "e7e5", "c7c5")
    if whiteMove then whiteBreaks.contains(move) else blackBreaks.contains(move)

  private def isCenterCapture(uci: String): Boolean =
    val move = MoveReviewPvLine.normalizeUci(uci)
    move.length >= 4 &&
      CenterSquares.contains(move.slice(2, 4)) &&
      move.take(2).headOption.exists(file => file != move.slice(2, 4).headOption.getOrElse(file))

  private def normalizeMotifKey(value: String): String =
    Option(value).getOrElse("").trim.toLowerCase.replaceAll("""[^a-z0-9]+""", "_")

  private def slug(value: String): String =
    normalizeMotifKey(value).stripPrefix("_").stripSuffix("_")

  private def hangingStatement(
      bead: Int,
      square: Square,
      role: Role,
      attackers: List[Square],
      defenders: List[Square],
      tier: StandardCommentaryClaimPolicy.HangingTier
  ): Option[String] =
    val balance = attackDefenseBalance(attackers, defenders)
    tier match
      case StandardCommentaryClaimPolicy.HangingTier.Red =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The ${roleLabel(role)} on ${square.key} is hanging ($balance).",
            s"The ${roleLabel(role)} on ${square.key} is underdefended: $balance.",
            s"Keep an eye on the ${roleLabel(role)} on ${square.key}: $balance."
          ))
        )
      case StandardCommentaryClaimPolicy.HangingTier.Amber =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"Pressure is building against the ${roleLabel(role)} on ${square.key} ($balance).",
            s"The ${roleLabel(role)} on ${square.key} can become a target if the pressure grows ($balance).",
            s"The ${roleLabel(role)} on ${square.key} needs watching ($balance)."
          ))
        )
      case StandardCommentaryClaimPolicy.HangingTier.Suppress =>
        None

  private def attackDefenseBalance(attackers: List[Square], defenders: List[Square]): String =
    val a = attackers.size
    val d = defenders.size
    val aText = if a == 0 then "no attackers" else s"$a ${plural(a, "attacker", "attackers")}"
    val dText = if d == 0 then "no defenders" else s"$d ${plural(d, "defender", "defenders")}"
    if d == 0 then s"$aText, $dText" else s"$aText vs $dText"

  private def plural(n: Int, one: String, many: String): String =
    if n == 1 then one else many

  private def targetText(targets: List[(Square, Role)]): String =
    targets.take(2).map { case (sq, r) => s"${roleLabel(r)} on ${sq.key}" } match
      case a :: b :: Nil => s"$a and $b"
      case a :: Nil      => a
      case _             => "multiple targets"

  def roleLabel(role: Role): String =
    role match
      case chess.Pawn   => "pawn"
      case chess.Knight => "knight"
      case chess.Bishop => "bishop"
      case chess.Rook   => "rook"
      case chess.Queen  => "queen"
      case King         => "king"
