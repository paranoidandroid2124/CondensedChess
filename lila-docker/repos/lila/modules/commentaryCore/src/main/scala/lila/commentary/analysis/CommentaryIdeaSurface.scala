package lila.commentary.analysis

import chess.{ Color, King, Piece, Role, Square }
import lila.commentary.analysis.claim.ProofContractRules
import lila.commentary.analysis.semantic.RelationObservationCatalog
import lila.commentary.{ MoveReviewMoveRef, MoveReviewPvInterpretation }
import lila.commentary.model.*
import scala.annotation.unused

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
      openingName: Option[String],
      strategicDelta: Option[PlayerFacingMoveDeltaEvidence] = None,
      phase: String = "",
      ply: Int = 0
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

  enum MoveCharacterBand:
    case Bad, Necessary, Good, Quiet, Neutral

    def key: String =
      toString.toLowerCase

  final case class MoveReviewLineProof(
      proofKind: String,
      subject: String,
      reply: Option[MoveReviewMoveRef],
      continuation: Option[MoveReviewMoveRef],
      blockedReasons: List[String] = Nil
  ):
    def tags: List[String] =
      List(Some(s"line_proof:$proofKind"), Some(s"line_subject:$subject")).flatten

  final case class MoveReviewIdeaDescriptor(
      ideaKind: String,
      reviewIntent: String,
      moveCharacterBand: MoveCharacterBand,
      source: String,
      title: String,
      baseProse: String,
      movePurpose: String,
      opponentQuestion: Option[String],
      lineResolution: Option[String],
      reasonTags: List[String],
      confirms: List[String],
      linePurpose: Option[String],
      tension: Option[String],
      opponentReplyMeaning: Option[String],
      learningPoint: Option[String],
      scopedTakeaway: Option[MoveReviewScopedTakeaway.ScopedTakeaway] = None,
      requiresPvForAdmission: Boolean,
      factFragments: List[FactFragment] = Nil
  ):
    def prose: String =
      List(Some(movePurpose), opponentQuestion, lineResolution, scopedTakeaway.map(_.text).orElse(learningPoint))
        .flatten
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .mkString(" ")

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
          learningPoint = scopedTakeaway.map(_.text).orElse(learningPoint).getOrElse(""),
          supportedByLineId = Some(line.line.lineId),
          confidence = "bounded_local"
        )

  def describe(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[MoveReviewIdeaDescriptor] =
    val characterBand = moveCharacterBand(truthContract)
    val descriptor =
      MoveReviewIdeaRules.iterator
        .map(_.describe(played, evidence, lineFacts, characterBand, truthContract))
        .collectFirst { case Some(desc) => desc }

    descriptor.filter(desc => !desc.requiresPvForAdmission || lineFacts.nonEmpty)

  private final case class MoveReviewIdeaRule(
      name: String,
      describe: (
          PlayedMove,
          MoveReviewEvidence,
          Option[MoveReviewPvLine.LineFacts],
          MoveCharacterBand,
          Option[DecisiveTruthContract]
      ) => Option[MoveReviewIdeaDescriptor]
  )

  private val MoveReviewIdeaRules: List[MoveReviewIdeaRule] =
    List(
      MoveReviewIdeaRule("opening_goal", openingDescriptor),
      MoveReviewIdeaRule("castling", (played, _, lineFacts, characterBand, _) => castlingDescriptor(played, lineFacts, characterBand)),
      MoveReviewIdeaRule("strategic_support", strategicSupportDescriptor),
      MoveReviewIdeaRule("tactical", tacticalDescriptor),
      MoveReviewIdeaRule("target", targetDescriptor),
      MoveReviewIdeaRule("capture", captureDescriptor),
      MoveReviewIdeaRule("endgame", endgameDescriptor),
      MoveReviewIdeaRule("line_backed_local", lineBackedLocalDescriptor)
    )

  // MoveReview admission rules. Keep descriptor selection before rendering fields.
  private def openingDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      goal <- evidence.openingGoal
      line <- lineFacts
    yield
      val reasonTags = List("opening_goal", slug(goal.goalName))
      val purpose = lineFacts.flatMap(line => PvMeaning.classifyOpeningPurpose(played, evidence, line.continuation, reasonTags))
      val openingPart = evidence.openingName.map(name => s" in the $name").getOrElse("")
      val goalPhrase = openingGoalPhrase(goal.goalName)
      val supported = goal.supportedEvidence.map(_.trim).filter(_.nonEmpty).mkString(", ")
      val supportText =
        if supported.nonEmpty then s" The board evidence is: $supported."
        else ""
      descriptor(
        ideaKind = "opening_goal",
        reviewIntent = "normal_development",
        moveCharacterBand = characterBand,
        source = "opening_goal",
        title = s"${played.san} follows $goalPhrase",
        baseProse = s"${played.san}$openingPart fits $goalPhrase.$supportText",
        reasonTags = reasonTags,
        linePurpose = purpose,
        lineProof = lineProof("opening_goal", played, line),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        factFragments = List(FactFragment.OpeningGoalFragment(
          san = played.san,
          openingName = evidence.openingName,
          goalName = goal.goalName,
          supportedEvidence = goal.supportedEvidence.map(_.trim).filter(_.nonEmpty)
        ))
      )

  private def castlingDescriptor(
      played: PlayedMove,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand
  ): Option[MoveReviewIdeaDescriptor] =
    lineFacts.filter(_ => played.isCastle).map { line =>
      val reasonTags = List("king_safety")
      descriptor(
        ideaKind = "king_safety",
        reviewIntent = "king_safety",
        moveCharacterBand = characterBand,
        source = "basic_move_explanation",
        title = s"${played.san} improves king safety",
        baseProse = s"${played.san} improves king safety before the position asks for a more concrete central or tactical decision.",
        reasonTags = reasonTags,
        linePurpose = Some("king_safety_first"),
        lineProof = lineProof("king_safety", played, line),
        played = played,
        evidence = MoveReviewEvidence(Nil, Nil, None, None),
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        factFragments = List(FactFragment.KingSafetyFragment(played.san))
      )
    }

  private def strategicSupportDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      delta <- evidence.strategicDelta.filter(certifiedStrategicSupport)
      line <- lineFacts
    yield
      val intent = strategicIntent(delta)
      descriptor(
        ideaKind = intent,
        reviewIntent = intent,
        moveCharacterBand = characterBand,
        source = "certified_strategy_support",
        title = strategicTitle(played, delta),
        baseProse = strategicPurpose(played, delta),
        reasonTags = List(
          "certified_strategy_support",
          s"proof_family:${delta.packet.proofFamily}",
          s"proof_source:${delta.packet.proofSource}"
        ),
        linePurpose = Some(strategicLinePurpose(delta)),
        lineProof = lineProof("certified_strategy", played, line, subjectOverride = preferredStrategicSubject(delta).orElse(Some(played.uci))),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        factFragments = List(FactFragment.StrategicSupportFragment(
          san = played.san,
          proofFamily = delta.packet.proofFamily,
          proofSource = delta.packet.proofSource,
          purpose = strategicPurpose(played, delta)
        ))
      )

  private def tacticalDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    ownedTacticalFact(played, evidence, lineFacts).map { owned =>
      val kind = owned.kind
        val label = kind.replace("_", " ")
        val title =
          s"${played.san} creates a $label"
        val prose =
          kind match
            case "fork" =>
              s"${played.san} creates a fork that stays tied to the verified tactical targets."
            case "pin" =>
              s"${played.san} creates a pin tied to the pinned piece and the piece behind it."
            case "skewer" =>
              s"${played.san} creates a skewer tied to the front and back targets."
            case _ =>
              s"${played.san} creates a tactical threat tied to the verified targets."
        descriptor(
          ideaKind = kind,
          reviewIntent = "creates_threat",
          moveCharacterBand = characterBand,
          source = "canonical_fact",
          title = title,
          baseProse = prose,
          reasonTags = evidenceTags(List(owned.fact), List(owned.motif)),
          linePurpose = Some("create_tactical_threat"),
          lineProof = lineProof("tactical_threat", played, lineFacts.get),
          played = played,
          evidence = evidence,
          lineFacts = lineFacts,
          requiresPvForAdmission = true,
          factFragments = List(FactFragment.TacticalThreatFragment(
            san = played.san,
            kind = owned.kind,
            targets = owned.fact.participants.map(_.key)
          ))
        )
    }

  private final case class OwnedTacticalFact(kind: String, fact: Fact, motif: Motif)

  private def ownedTacticalFact(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[OwnedTacticalFact] =
    lineFacts
      .filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci && line.reply.nonEmpty)
      .flatMap { line =>
        val immediateMotifs = evidence.motifs.filter(immediateMotifOwnsMove(played, _))
        val fork =
          evidence.facts.collectFirst {
            case fact: Fact.Fork
                if fact.attacker == played.to &&
                  immediateMotifs.exists(motif => motifOwnsFork(played, fact, motif)) &&
                  lineConfirmsTactic("fork", fact, line) =>
              OwnedTacticalFact("fork", fact, immediateMotifs.find(motifOwnsFork(played, fact, _)).get)
          }
        val pin =
          evidence.facts.collectFirst {
            case fact: Fact.Pin
                if fact.attacker == played.to &&
                  immediateMotifs.exists(motif => motifOwnsPin(played, fact, motif)) &&
                  lineConfirmsTactic("pin", fact, line) =>
              OwnedTacticalFact("pin", fact, immediateMotifs.find(motifOwnsPin(played, fact, _)).get)
          }
        val skewer =
          evidence.facts.collectFirst {
            case fact: Fact.Skewer
                if fact.attacker == played.to &&
                  immediateMotifs.exists(motif => motifOwnsSkewer(played, fact, motif)) &&
                  lineConfirmsTactic("skewer", fact, line) =>
              OwnedTacticalFact("skewer", fact, immediateMotifs.find(motifOwnsSkewer(played, fact, _)).get)
          }
        List(fork, pin, skewer).flatten.headOption
      }

  private def immediateMotifOwnsMove(played: PlayedMove, motif: Motif): Boolean =
    motif match
      case m: Motif.Fork =>
        m.plyIndex == 0 &&
          m.color == played.color &&
          m.attackingPiece == played.role &&
          m.square == played.to &&
          m.move.exists(sanEquivalent(_, played.san))
      case m: Motif.Pin =>
        m.plyIndex == 0 &&
          m.color == played.color &&
          m.pinningPiece == played.role &&
          m.pinningSq.contains(played.to) &&
          m.move.exists(sanEquivalent(_, played.san))
      case m: Motif.Skewer =>
        m.plyIndex == 0 &&
          m.color == played.color &&
          m.attackingPiece == played.role &&
          m.attackingSq.contains(played.to) &&
          m.move.exists(sanEquivalent(_, played.san))
      case _ => false

  private def motifOwnsFork(played: PlayedMove, fact: Fact.Fork, motif: Motif): Boolean =
    motif match
      case m: Motif.Fork =>
        val motifTargets = m.targetSquares.toSet
        val factTargets = fact.targets.map(_._1).toSet
        fact.attacker == played.to &&
          m.square == played.to &&
          motifTargets.nonEmpty &&
          factTargets.nonEmpty &&
          factTargets.subsetOf(motifTargets)
      case _ => false

  private def motifOwnsPin(played: PlayedMove, fact: Fact.Pin, motif: Motif): Boolean =
    motif match
      case m: Motif.Pin =>
        fact.attacker == played.to &&
          m.pinningSq.contains(fact.attacker) &&
          m.pinnedSq.contains(fact.pinned) &&
          m.behindSq.contains(fact.behind)
      case _ => false

  private def motifOwnsSkewer(played: PlayedMove, fact: Fact.Skewer, motif: Motif): Boolean =
    motif match
      case m: Motif.Skewer =>
        fact.attacker == played.to &&
          m.attackingSq.contains(fact.attacker) &&
          m.frontSq.contains(fact.front) &&
          m.backSq.contains(fact.back)
      case _ => false

  private def lineConfirmsTactic(kind: String, fact: Fact, line: MoveReviewPvLine.LineFacts): Boolean =
    kind match
      case "fork" =>
        line.reply.nonEmpty
      case "pin" =>
        fact match
          case pin: Fact.Pin => continuationTouchesAny(line, List(pin.pinned, pin.behind))
          case _             => false
      case "skewer" =>
        fact match
          case skewer: Fact.Skewer => continuationTouchesAny(line, List(skewer.front, skewer.back))
          case _                   => false
      case _ => false

  private def continuationTouchesAny(line: MoveReviewPvLine.LineFacts, squares: List[Square]): Boolean =
    val beforeFen = line.reply.map(_.fenAfter).getOrElse(line.first.fenAfter)
    line.continuation.exists(moveTouchesAny(beforeFen, _, squares))

  private def moveTouchesAny(beforeFen: String, move: MoveReviewMoveRef, squares: List[Square]): Boolean =
    MoveReviewExchangeAnalyzer
      .boundedReplay(beforeFen, List(move.uci), maxPlies = 1)
      .flatMap(_.headOption)
      .exists(step => squares.exists(square => step.move.orig == square || step.move.dest == square))

  private def sanEquivalent(left: String, right: String): Boolean =
    def clean(value: String): String =
      Option(value).getOrElse("").trim.replaceAll("""[+#?!]+$""", "")
    clean(left) == clean(right)

  private def targetDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    Option.when(
      evidence.facts.exists(f => f.isInstanceOf[Fact.HangingPiece] || f.isInstanceOf[Fact.TargetPiece]) &&
        lineFacts.flatMap(_.reply).nonEmpty
    ) {
      val defensive = defensiveTruth(truthContract) || evidence.facts.exists(_.scope == FactScope.ThreatLine)
      val purpose = if defensive then "answer_direct_threat" else "create_tactical_threat"
      val intent = if defensive then "answers_threat" else "creates_threat"
      val proofKind = if defensive then "defensive_answer" else "target_pressure"
      descriptor(
        ideaKind = "direct_threat",
        reviewIntent = intent,
        moveCharacterBand = characterBand,
        source = "canonical_fact",
        title = if defensive then s"${played.san} answers the immediate question" else s"${played.san} asks a concrete question",
        baseProse =
          if defensive then s"${played.san} answers the immediate threat shown by the local evidence."
          else s"${played.san} creates a concrete target that the opponent has to answer.",
        reasonTags = evidenceTags(evidence.facts, evidence.motifs),
        linePurpose = Some(purpose),
        lineProof = lineProof(proofKind, played, lineFacts.get),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        factFragments = List(FactFragment.DirectThreatFragment(
          san = played.san,
          isDefensive = defensive,
          reason = if defensive then "answer_direct_threat" else "create_tactical_threat"
        ))
      )
    }

  private def captureDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    Option.when(evidence.hasCaptureMotif && played.isCapture && isImmediateRecapture(played.toKey, lineFacts)) {
      val purpose =
        if played.capturedRole.exists(_ != chess.Pawn) || played.role != chess.Pawn then "clarify_exchange"
        else "resolve_capture_tension"
      descriptor(
        ideaKind = if purpose == "clarify_exchange" then "exchange_clarified" else "capture_tension",
        reviewIntent = if purpose == "clarify_exchange" then "clarifies_exchange" else "keeps_tension",
        moveCharacterBand = characterBand,
        source = "basic_move_explanation",
        title =
          if purpose == "clarify_exchange" then s"${played.san} clarifies the exchange"
          else s"${played.san} resolves the capture tension",
        baseProse = s"${played.san} serves a concrete local purpose.",
        reasonTags = List("capture_sequence"),
        linePurpose = Some(purpose),
        lineProof = lineProof(if purpose == "clarify_exchange" then "exchange_clarification" else "capture_tension", played, lineFacts.get),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        factFragments = List(FactFragment.CaptureFragment(
          san = played.san,
          purpose = purpose
        ))
      )
    }

  private def endgameDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    lineFacts.filter(_ => evidence.endgameFacts.nonEmpty).map { line =>
      descriptor(
        ideaKind = "endgame_activity",
        reviewIntent = "improves_endgame_activity",
        moveCharacterBand = characterBand,
        source = "canonical_fact",
        title = s"${played.san} improves endgame activity",
        baseProse = s"${played.san} improves activity through exact endgame facts, keeping the explanation local to the verified feature.",
        reasonTags = evidenceTags(evidence.facts, evidence.motifs),
        linePurpose = Some("improve_endgame_activity"),
        lineProof = lineProof("endgame_activity", played, line),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        factFragments = List(FactFragment.EndgameFragment(
          san = played.san,
          facts = evidence.endgameFacts.map(_.toString)
        ))
      )
    }

  private def lineBackedLocalDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    lineFacts.filter(line => line.reply.nonEmpty && isOpeningPhase(evidence)).map { line =>
      val purpose = localLinePurpose(played)
      descriptor(
        ideaKind = "line_backed_local",
        reviewIntent = localReviewIntent(purpose),
        moveCharacterBand = characterBand,
        source = "basic_move_explanation",
        title = localTitle(played, purpose),
        baseProse = localPurposeText(played, purpose),
        reasonTags = List("line_backed_local", s"local_purpose:$purpose"),
        linePurpose = Some(purpose),
        lineProof = lineProof("line_backed_local", played, line),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true
      )
    }

  private def localLinePurpose(played: PlayedMove): String =
    if isCentralBreak(played.uci, played.isWhite) then "challenge_center"
    else if played.isCapture then "local_capture"
    else
      played.role match
        case chess.Knight | chess.Bishop => "quiet_development"
        case chess.Pawn                  => "local_pawn_setup"
        case _                           => "local_piece_improvement"

  private def localReviewIntent(purpose: String): String =
    purpose match
      case "challenge_center"        => "keeps_tension"
      case "local_capture"           => "clarifies_exchange"
      case "quiet_development"       => "normal_development"
      case "local_piece_improvement" => "quiet_improvement"
      case _                         => "keeps_tension"

  private def localTitle(played: PlayedMove, purpose: String): String =
    purpose match
      case "challenge_center"        => s"${played.san} challenges the center"
      case "local_capture"           => s"${played.san} changes the local material"
      case "quiet_development"       => s"${played.san} develops the ${roleLabel(played.role)}"
      case "local_piece_improvement" => s"${played.san} improves the ${roleLabel(played.role)}"
      case _                         => s"${played.san} changes the pawn setup"

  private def localPurposeText(played: PlayedMove, purpose: String): String =
    purpose match
      case "challenge_center" =>
        s"${played.san} challenges the center immediately."
      case "local_capture" =>
        s"${played.san} captures on ${played.toKey} and keeps the material change concrete."
      case "quiet_development" =>
        s"${played.san} develops the ${roleLabel(played.role)} to ${played.toKey}."
      case "local_piece_improvement" =>
        s"${played.san} improves the ${roleLabel(played.role)} on ${played.toKey}."
      case _ =>
        s"${played.san} changes the pawn setup on ${played.toKey}."

  private def isOpeningPhase(evidence: MoveReviewEvidence): Boolean =
    evidence.phase.trim.equalsIgnoreCase("opening") ||
      (evidence.openingName.nonEmpty && evidence.ply > 0 && evidence.ply <= 20)

  private def descriptor(
      ideaKind: String,
      reviewIntent: String,
      moveCharacterBand: MoveCharacterBand,
      source: String,
      title: String,
      baseProse: String,
      reasonTags: List[String],
      linePurpose: Option[String],
      lineProof: MoveReviewLineProof,
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      requiresPvForAdmission: Boolean,
      factFragments: List[FactFragment] = Nil
  ): MoveReviewIdeaDescriptor =
    val frags = factFragments
    val opponentReplyMeaning = lineFacts.flatMap(_.reply).flatMap(reply => PvMeaning.classifyOpponentReply(played, evidence, reply))
    val movePurpose = baseProse.trim
    val opponentQuestion =
      lineFacts.flatMap(_.reply).flatMap(reply => PvMeaning.opponentQuestion(played, reply, opponentReplyMeaning))
    val lineResolution =
      linePurpose.flatMap(purpose => lineFacts.flatMap(line => PvMeaning.lineResolution(purpose, played, evidence, line)))
    val expandedReasonTags =
      (reasonTags ++ lineProof.tags ++ List(s"review_intent:$reviewIntent", s"character_band:${moveCharacterBand.key}")).distinct
    val confirms = PvMeaning.confirmedFacts(reviewIntent, linePurpose, evidence, expandedReasonTags)
    val scopedTakeaway =
      linePurpose.flatMap(purpose => MoveReviewScopedTakeaway.build(purpose, played, evidence, lineFacts))
    MoveReviewIdeaDescriptor(
      ideaKind = ideaKind,
      reviewIntent = reviewIntent,
      moveCharacterBand = moveCharacterBand,
      source = source,
      title = title,
      baseProse = baseProse,
      movePurpose = movePurpose,
      opponentQuestion = opponentQuestion,
      lineResolution = lineResolution,
      reasonTags = expandedReasonTags,
      confirms = confirms,
      linePurpose = linePurpose,
      tension = linePurpose.map(purpose => PvMeaning.classifyTension(purpose, played, lineFacts.flatMap(_.reply), lineFacts.flatMap(_.continuation))),
      opponentReplyMeaning = opponentReplyMeaning,
      learningPoint = scopedTakeaway.map(_.text),
      scopedTakeaway = scopedTakeaway,
      requiresPvForAdmission = requiresPvForAdmission,
      factFragments = frags
    )

  private def moveCharacterBand(truthContract: Option[DecisiveTruthContract]): MoveCharacterBand =
    truthContract match
      case Some(contract) if contract.isBad =>
        MoveCharacterBand.Bad
      case Some(contract)
          if contract.truthClass == DecisiveTruthClass.Best &&
            contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense &&
            contract.benchmarkCriticalMove &&
            contract.visibilityRole == TruthVisibilityRole.PrimaryVisible =>
        MoveCharacterBand.Necessary
      case Some(contract)
          if contract.isVerifiedExemplar ||
            contract.isConversionFollowthrough ||
            contract.truthClass == DecisiveTruthClass.WinningInvestment ||
            contract.truthClass == DecisiveTruthClass.CompensatedInvestment =>
        MoveCharacterBand.Good
      case Some(contract)
          if contract.reasonFamily == DecisiveReasonKind.QuietTechnicalMove ||
            contract.surfaceMode == TruthSurfaceMode.Neutral =>
        MoveCharacterBand.Quiet
      case _ =>
        MoveCharacterBand.Neutral

  private def lineProof(
      proofKind: String,
      played: PlayedMove,
      line: MoveReviewPvLine.LineFacts,
      subjectOverride: Option[String] = None,
      blockedReasons: List[String] = Nil
  ): MoveReviewLineProof =
    MoveReviewLineProof(
      proofKind = proofKind,
      subject = subjectOverride.getOrElse(played.uci),
      reply = line.reply,
      continuation = line.continuation,
      blockedReasons = blockedReasons
    )

  private def defensiveTruth(truthContract: Option[DecisiveTruthContract]): Boolean =
    truthContract.exists(contract =>
      contract.truthClass == DecisiveTruthClass.Best &&
        contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense
    )

  private def certifiedStrategicSupport(delta: PlayerFacingMoveDeltaEvidence): Boolean =
    val packet = delta.packet
    packet.scope == PlayerFacingPacketScope.MoveLocal &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.sameBranchState == PlayerFacingSameBranchState.Proven &&
      packet.persistence == PlayerFacingClaimPersistence.Stable &&
      packet.releaseRisks.isEmpty &&
      packet.suppressionReasons.isEmpty &&
      ProofContractRules.supportedLocalEligible(packet.proofFamily) &&
      ProofContractRules.failureCodes(packet).isEmpty

  private def strategicIntent(delta: PlayerFacingMoveDeltaEvidence): String =
    delta.deltaClass match
      case PlayerFacingMoveDeltaClass.CounterplayReduction => "prevents_counterplay"
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => "clarifies_exchange"
      case PlayerFacingMoveDeltaClass.PlanAdvance          => "advances_plan"
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => "answers_threat"
      case PlayerFacingMoveDeltaClass.PressureIncrease     => "creates_threat"
      case PlayerFacingMoveDeltaClass.NewAccess            => "quiet_improvement"

  private def strategicLinePurpose(delta: PlayerFacingMoveDeltaEvidence): String =
    delta.deltaClass match
      case PlayerFacingMoveDeltaClass.CounterplayReduction => "prevent_counterplay"
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => "clarify_exchange"
      case PlayerFacingMoveDeltaClass.PlanAdvance          => "advance_plan"
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => "answer_direct_threat"
      case PlayerFacingMoveDeltaClass.PressureIncrease     => "create_tactical_threat"
      case PlayerFacingMoveDeltaClass.NewAccess            => "quiet_improvement"

  private def strategicTitle(played: PlayedMove, delta: PlayerFacingMoveDeltaEvidence): String =
    strategicIntent(delta) match
      case "prevents_counterplay" => s"${played.san} keeps counterplay restrained"
      case "clarifies_exchange"   => s"${played.san} clarifies the exchange"
      case "advances_plan"        => s"${played.san} advances the local plan"
      case "answers_threat"       => s"${played.san} answers the local resource"
      case "creates_threat"       => s"${played.san} increases local pressure"
      case _                      => s"${played.san} improves the local setup"

  private def strategicPurpose(played: PlayedMove, delta: PlayerFacingMoveDeltaEvidence): String =
    val anchor = preferredStrategicSubject(delta).map(subject => s" around $subject").getOrElse("")
    strategicIntent(delta) match
      case "prevents_counterplay" => s"${played.san} keeps counterplay restrained$anchor."
      case "clarifies_exchange"   => s"${played.san} clarifies the exchange$anchor."
      case "advances_plan"        => s"${played.san} supports the plan$anchor."
      case "answers_threat"       => s"${played.san} limits the defensive resource$anchor."
      case "creates_threat"       => s"${played.san} increases local pressure$anchor."
      case _                      => s"${played.san} improves the local setup$anchor."

  private def preferredStrategicSubject(delta: PlayerFacingMoveDeltaEvidence): Option[String] =
    (delta.packet.anchorTerms ++ delta.anchorTerms ++ delta.packet.proofPathWitness.ownerSeedTerms ++ delta.packet.proofPathWitness.continuationTerms)
      .map(_.trim)
      .find(_.nonEmpty)

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
      else if legalCenterCapture(played.afterFen, reply) ||
          legalCenterCapture(reply.map(_.fenAfter).getOrElse(played.afterFen), continuation)
      then "center_released"
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
      else if played.isCapture && legalCaptureToSquare(played.afterFen, Some(reply), played.toKey) then Some("recaptures")
      else if evidence.facts.exists(f => f.isInstanceOf[Fact.HangingPiece] || f.isInstanceOf[Fact.TargetPiece]) then Some("addresses_target")
      else if Set("e7e6", "d7d6", "c7c6", "e2e3", "d2d3", "c2c3").contains(uci) then Some("reinforces_center")
      else if Set("c7c5", "c2c4", "d7d5", "d2d4", "e7e5", "e2e4").contains(uci) then Some("challenges_center")
      else if reply.san.trim.startsWith("N") && Set("c6", "f6", "c3", "f3").contains(uci.takeRight(2)) then
        Some("knight_development")
      else None

    def confirmedFacts(
        reviewIntent: String,
        linePurpose: Option[String],
        evidence: MoveReviewEvidence,
        reasonTags: List[String]
    ): List[String] =
      val values = scala.collection.mutable.LinkedHashSet.empty[String]
      reasonTags.foreach(values += _)
      values += reviewIntent
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

    def opponentQuestion(
        played: PlayedMove,
        reply: MoveReviewMoveRef,
        opponentReplyMeaning: Option[String]
    ): Option[String] =
      val replySan = reply.san.trim
      Option.when(replySan.nonEmpty)(
        opponentReplyMeaning match
          case Some("attacks_center_pawn") =>
            s"$replySan asks whether ${played.san}'s setup can keep the e-pawn supported."
          case Some("asks_piece_commitment") =>
            s"$replySan asks the moved piece to clarify its retreat or follow-up."
          case Some("recaptures") =>
            s"$replySan immediately asks what remains after the capture."
          case Some("addresses_target") =>
            s"$replySan is the first answer to the target ${played.san} created."
          case Some("reinforces_center") =>
            s"$replySan reinforces the center and asks how ${played.san} will keep its point."
          case Some("challenges_center") =>
            s"$replySan challenges the center and asks whether ${played.san}'s idea holds up."
          case Some("knight_development") =>
            s"$replySan develops a knight toward the center."
          case _ =>
            s"$replySan is the first reply that tests the point of ${played.san}."
      )

    def lineResolution(
        purpose: String,
        played: PlayedMove,
        evidence: MoveReviewEvidence,
        line: MoveReviewPvLine.LineFacts
    ): Option[String] =
      val replySan = line.reply.map(_.san).filter(_.nonEmpty).getOrElse("the reply")
      val continuationSan = line.continuation.map(_.san).filter(_.nonEmpty)
      purpose match
        case "quiet_development" if line.continuation.exists(move => MoveReviewPvLine.normalizeUci(move.uci) == "d2d3") =>
          Some(s"${continuationSan.getOrElse("the continuation")} supports e4 and keeps the quiet setup intact.")
        case "quiet_development" =>
          continuationSan.map(move => s"$move keeps the development idea from ${played.san} intact.")
        case "center_break_setup" =>
          continuationSan.map(move => s"$move is the follow-up that tests the central break.")
        case "challenge_center" =>
          continuationSan.map(move => s"$move shows how the center challenge is supported.")
        case "king_safety_first" =>
          Some(s"The line keeps ${played.san}'s king-safety purpose ahead of any broader evaluation claim.")
        case "create_tactical_threat" =>
          Some(s"The line keeps the tactical point concrete after $replySan instead of turning it into a broad evaluation claim.")
        case "answer_direct_threat" =>
          Some(s"The line resolves the target question through $replySan and keeps the claim local.")
        case "resolve_capture_tension" =>
          Some(s"The line shows the tension after $replySan, so the review is about what remains after recapture.")
        case "clarify_exchange" =>
          Some(s"The line uses $replySan to clarify which exchange remains on ${played.toKey}.")
        case "improve_endgame_activity" if evidence.endgameFacts.exists(_.isInstanceOf[Fact.KingActivity]) =>
          continuationSan.map(move => s"$move shows the next use of the improved king activity.")
        case "improve_endgame_activity" =>
          continuationSan.map(move => s"$move shows the next local use of the verified endgame feature.")
        case _ =>
          continuationSan.map(move => s"$move shows how the idea from ${played.san} is maintained.")

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
    RelationObservationCatalog.deferredFallbackForMotifTag(motif).isEmpty &&
      !RelationObservationCatalog.relationWitnessOnlyMotifTag(motif) &&
      (
        fact match
          case _: Fact.Pin =>
            motif.contains("pin") || motif.contains("xray")
          case _: Fact.Skewer =>
            motif.contains("skewer") || motif.contains("xray")
          case _: Fact.Fork =>
            motif.contains("fork") || motif.contains("deflection")
          case _: Fact.HangingPiece =>
            motif.contains("battery")
          case _: Fact.WeakSquare =>
            List("minority_attack", "color_complex", "bad_bishop", "good_bishop", "outpost").exists(motif.contains)
          case _: Fact.Outpost =>
            List("outpost", "knight_domination", "maneuver", "knight_vs_bishop").exists(motif.contains)
          case _: Fact.Opposition =>
            motif.contains("opposition")
          case _: Fact.KingActivity =>
            motif.contains("king_cut_off") || motif.contains("passed_pawn")
          case _ => false
      )

  private val CenterSquares: Set[String] = Set("d4", "e4", "d5", "e5")

  private def isImmediateRecapture(
      targetSquare: String,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Boolean =
    lineFacts.exists(line =>
      legalCaptureToSquare(
        beforeFen = line.first.fenAfter,
        move = line.reply,
        targetSquare = targetSquare
      )
    )

  private def isCentralBreak(uci: String, whiteMove: Boolean): Boolean =
    val move = MoveReviewPvLine.normalizeUci(uci)
    val whiteBreaks = Set("d2d4", "e2e4", "c2c4")
    val blackBreaks = Set("d7d5", "e7e5", "c7c5")
    if whiteMove then whiteBreaks.contains(move) else blackBreaks.contains(move)

  private def legalCenterCapture(
      beforeFen: String,
      move: Option[MoveReviewMoveRef]
  ): Boolean =
    move.exists(ref =>
      MoveReviewExchangeAnalyzer
        .boundedReplay(beforeFen, List(ref.uci), maxPlies = 1)
        .flatMap(_.headOption)
        .exists(step =>
          step.move.captures &&
            CenterSquares.contains(step.move.dest.key) &&
            step.move.orig.file != step.move.dest.file
        )
    )

  private def legalCaptureToSquare(
      beforeFen: String,
      move: Option[MoveReviewMoveRef],
      targetSquare: String
  ): Boolean =
    move.exists(ref =>
      MoveReviewExchangeAnalyzer
        .boundedReplay(beforeFen, List(ref.uci), maxPlies = 1)
        .flatMap(_.headOption)
        .exists(step => step.move.captures && step.move.dest.key == targetSquare)
    )

  private def normalizeMotifKey(value: String): String =
    Option(value).getOrElse("").trim.toLowerCase.replaceAll("""[^a-z0-9]+""", "_")

  private def slug(value: String): String =
    normalizeMotifKey(value).stripPrefix("_").stripSuffix("_")

  private def openingGoalPhrase(goalName: String): String =
    val key = slug(goalName)
    if key.contains("development") || key.contains("fianchetto") then "a development idea"
    else if key.contains("challenge") || key.contains("sicilian") then "a center challenge"
    else if key.contains("break") || key.contains("gambit") then "a thematic pawn-break plan"
    else if key.contains("tension") || key.contains("release") then "a tension-handling idea"
    else if key.contains("safety") || key.contains("castle") then "a king-safety idea"
    else "an opening idea"

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
