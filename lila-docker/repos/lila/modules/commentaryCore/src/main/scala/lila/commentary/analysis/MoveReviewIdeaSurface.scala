package lila.commentary.analysis

import lila.commentary.{ MoveReviewMoveRef, MoveReviewPvInterpretation }
import lila.commentary.model.*

private[commentary] object MoveReviewIdeaSurface:

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

    def pvInterpretation(lineFacts: Option[MoveReviewPvFacts.LineFacts]): Option[MoveReviewPvInterpretation] =
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
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    val descriptor =
      openingDescriptor(played, evidence, lineFacts)
        .orElse(castlingDescriptor(played, lineFacts))
        .orElse(tacticalDescriptor(played, evidence, lineFacts))
        .orElse(targetDescriptor(played, evidence, lineFacts))
        .orElse(captureDescriptor(played, evidence, lineFacts))
        .orElse(endgameDescriptor(played, evidence, lineFacts))

    descriptor.filter(desc => !desc.requiresPvForAdmission || lineFacts.nonEmpty)

  private def openingDescriptor(
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    evidence.openingGoal.map { goal =>
      val reasonTags = List("opening_goal", slug(goal.goalName))
      val purpose = lineFacts.flatMap(line => classifyOpeningPurpose(played, evidence, line.continuation, reasonTags))
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
      played: MoveReviewExplanationBuilder.PlayedMove,
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
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
        evidence = MoveReviewExplanationBuilder.CanonicalEvidence(Nil, Nil, None, None),
        lineFacts = lineFacts,
        requiresPvForAdmission = false
      )
    }

  private def tacticalDescriptor(
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
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
          reasonTags = factTags(evidence),
          linePurpose = Some("create_tactical_threat"),
          played = played,
          evidence = evidence,
          lineFacts = lineFacts,
          requiresPvForAdmission = true
        )
      }
    }

  private def targetDescriptor(
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
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
        reasonTags = factTags(evidence),
        linePurpose = Some("answer_direct_threat"),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true
      )
    }

  private def captureDescriptor(
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
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
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
  ): Option[MoveReviewIdeaDescriptor] =
    Option.when(evidence.endgameFacts.nonEmpty && lineFacts.nonEmpty) {
      descriptor(
        ideaKind = "endgame_activity",
        source = "canonical_fact",
        title = s"${played.san} improves endgame activity",
        baseProse = s"${played.san} is admitted through exact endgame facts, so the explanation stays local to the verified endgame activity.",
        reasonTags = factTags(evidence),
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
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      lineFacts: Option[MoveReviewPvFacts.LineFacts],
      requiresPvForAdmission: Boolean
  ): MoveReviewIdeaDescriptor =
    val confirms = confirmedFacts(linePurpose, evidence, reasonTags)
    MoveReviewIdeaDescriptor(
      ideaKind = ideaKind,
      source = source,
      title = title,
      baseProse = baseProse,
      reasonTags = reasonTags.distinct,
      confirms = confirms,
      linePurpose = linePurpose,
      tension = linePurpose.map(purpose => classifyTension(purpose, played, lineFacts.flatMap(_.reply), lineFacts.flatMap(_.continuation))),
      opponentReplyMeaning = lineFacts.flatMap(_.reply).flatMap(reply => classifyOpponentReply(played, evidence, reply)),
      learningPoint = linePurpose.flatMap(purpose => learningPoint(purpose, played, evidence, lineFacts)),
      requiresPvForAdmission = requiresPvForAdmission
    )

  private def factTags(evidence: MoveReviewExplanationBuilder.CanonicalEvidence): List[String] =
    CommentaryFactSurface.evidenceTags(evidence.facts, evidence.motifs)

  private def classifyOpeningPurpose(
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      continuation: Option[MoveReviewMoveRef],
      reasonTags: List[String]
  ): Option[String] =
    evidence.openingGoal.map { goal =>
      val goalName = goal.goalName.toLowerCase
      if continuation.exists(move => isCentralBreak(move.uci, played.isWhite)) then "center_break_setup"
      else if continuation.exists(move => Set("d2d3", "d7d6", "e2e3", "e7e6").contains(MoveReviewPvFacts.normalizeUci(move.uci))) then
        "quiet_development"
      else if goalName.contains("challenge") || goalName.contains("break") || goalName.contains("sicilian") then "challenge_center"
      else if reasonTags.contains("king_safety") then "king_safety_first"
      else "quiet_development"
    }

  private def classifyTension(
      purpose: String,
      played: MoveReviewExplanationBuilder.PlayedMove,
      reply: Option[MoveReviewMoveRef],
      continuation: Option[MoveReviewMoveRef]
  ): String =
    if purpose == "resolve_capture_tension" || purpose == "clarify_exchange" then "center_released"
    else if continuation.exists(move => isCentralBreak(move.uci, played.isWhite)) then "delayed_center_break"
    else if purpose == "challenge_center" && Set("c2c4", "c7c5").contains(played.uci) then "tension_maintained"
    else if isCentralBreak(played.uci, played.isWhite) then "immediate_center_break"
    else if (reply.toList ++ continuation.toList).exists(move => isCenterCapture(move.uci)) then "center_released"
    else "tension_maintained"

  private def classifyOpponentReply(
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      reply: MoveReviewMoveRef
  ): Option[String] =
    val uci = MoveReviewPvFacts.normalizeUci(reply.uci)
    val opening = evidence.openingName.map(_.toLowerCase).getOrElse("")
    if opening.contains("ruy lopez") && uci == "a7a6" then Some("asks_piece_commitment")
    else if played.isWhite && (opening.contains("italian") || played.uci == "f1c4") && uci == "g8f6" then Some("attacks_center_pawn")
    else if played.isCapture && isImmediateRecapture(played.toKey, Some(reply)) then Some("recaptures")
    else if evidence.facts.exists(f => f.isInstanceOf[Fact.HangingPiece] || f.isInstanceOf[Fact.TargetPiece]) then Some("addresses_target")
    else if Set("e7e6", "d7d6", "c7c6", "e2e3", "d2d3", "c2c3").contains(uci) then Some("reinforces_center")
    else if Set("c7c5", "c2c4", "d7d5", "d2d4", "e7e5", "e2e4").contains(uci) then Some("challenges_center")
    else if uci.endsWith("c6") || uci.endsWith("f6") || uci.endsWith("c3") || uci.endsWith("f3") then Some("develops_and_contests")
    else None

  private def confirmedFacts(
      linePurpose: Option[String],
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      reasonTags: List[String]
  ): List[String] =
    val values = scala.collection.mutable.LinkedHashSet.empty[String]
    reasonTags.foreach(values += _)
    evidence.openingGoal.foreach(_ => values += "opening_goal")
    factTags(evidence).foreach(values += _)
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

  private def learningPoint(
      purpose: String,
      played: MoveReviewExplanationBuilder.PlayedMove,
      evidence: MoveReviewExplanationBuilder.CanonicalEvidence,
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
  ): Option[String] =
    lineFacts.map { line =>
      val replySan = line.reply.map(_.san).filter(_.nonEmpty).getOrElse("the reply")
      val continuationSan = line.continuation.map(_.san).filter(_.nonEmpty).getOrElse("the follow-up")
      val openingName = evidence.openingGoal.map(_.goalName.toLowerCase).getOrElse("")
      purpose match
        case "quiet_development" if line.continuation.exists(move => MoveReviewPvFacts.normalizeUci(move.uci) == "d2d3") =>
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

  private val CenterSquares: Set[String] = Set("d4", "e4", "d5", "e5")

  private def toSquare(uci: String): Option[String] =
    val move = MoveReviewPvFacts.normalizeUci(uci)
    Option.when(move.length >= 4)(move.slice(2, 4))

  private def isCaptureLike(move: MoveReviewMoveRef): Boolean =
    val uci = MoveReviewPvFacts.normalizeUci(move.uci)
    move.san.contains("x") ||
      (uci.length >= 4 && uci.charAt(0) != uci.charAt(2) && move.san.headOption.forall(!"KQRBN".contains(_)))

  private def isImmediateRecapture(targetSquare: String, reply: Option[MoveReviewMoveRef]): Boolean =
    reply.exists(move => isCaptureLike(move) && toSquare(move.uci).contains(targetSquare))

  private def isCentralBreak(uci: String, whiteMove: Boolean): Boolean =
    val move = MoveReviewPvFacts.normalizeUci(uci)
    val whiteBreaks = Set("d2d4", "e2e4", "c2c4")
    val blackBreaks = Set("d7d5", "e7e5", "c7c5")
    if whiteMove then whiteBreaks.contains(move) else blackBreaks.contains(move)

  private def isCenterCapture(uci: String): Boolean =
    val move = MoveReviewPvFacts.normalizeUci(uci)
    move.length >= 4 &&
      CenterSquares.contains(move.slice(2, 4)) &&
      move.take(2).headOption.exists(file => file != move.slice(2, 4).headOption.getOrElse(file))

  private def slug(value: String): String =
    Option(value).getOrElse("").trim.toLowerCase.replaceAll("""[^a-z0-9]+""", "_").stripPrefix("_").stripSuffix("_")
