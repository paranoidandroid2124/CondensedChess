package lila.commentary.analysis

import chess.{ Color, King, Piece, Role, Square }
import chess.format.Fen
import lila.commentary.analysis.claim.ProofContractRules
import lila.commentary.analysis.semantic.{ RelationObservationCatalog, RelationObservationDescriptor, RelationSurfaceRowKind }
import lila.commentary.{ MoveReviewMoveRef, MoveReviewPvInterpretation, StrategicIdeaKind, StrategicIdeaReadiness, StrategyIdeaSignal }
import lila.commentary.model.*
import scala.annotation.unused

private[commentary] object CommentaryIdeaSurface:
  import MoveReviewLocalFact.{
    Admission as LocalFactAdmission,
    Anchor as LocalFactAnchor,
    Candidate as LocalFactCandidate,
    Family as LocalFactFamily,
    LineBinding as LocalFactLineBinding,
    Producer as LocalFactProducer,
    Source as LocalFactSource,
    Subject as LocalFactSubject
  }

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
      lineConsequence: Option[LineConsequenceEvidence] = None,
      postMoveTargetFacts: List[Fact] = Nil,
      relationWitnesses: List[MoveReviewExchangeAnalyzer.RelationWitness] = Nil,
      strategicDeltas: List[PlayerFacingMoveDeltaEvidence] = Nil,
      phase: String = "",
      ply: Int = 0,
      strictLocalFacts: Boolean = false,
      forcedLineTheme: Option[ForcedLineTruth.VerifiedTheme] = None,
      practicalPositionFacts: List[PracticalPositionFact] = Nil
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

  final case class PracticalPositionFact(
      family: LocalFactFamily,
      subject: LocalFactSubject,
      ideaKind: String,
      label: String,
      text: String,
      anchors: List[LocalFactAnchor],
      evidenceRefs: List[String],
      guardrails: List[String]
  )

  final case class MoveReviewIdeaDescriptor(
      ideaKind: String,
      reviewIntent: String,
      moveCharacterBand: MoveCharacterBand,
      source: String,
      title: String,
      baseProse: String,
      movePurpose: String,
      reasonTags: List[String],
      confirms: List[String],
      linePurpose: Option[String],
      learningPoint: Option[String],
      scopedTakeaway: Option[MoveReviewScopedTakeaway.ScopedTakeaway] = None,
      requiresPvForAdmission: Boolean,
      localFact: LocalFactAdmission,
      factFragments: List[FactFragment] = Nil
  ):
    def prose: String =
      List(Some(movePurpose), scopedTakeaway.map(_.text))
        .flatten
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .mkString(" ")

    def pvInterpretation(lineFacts: Option[MoveReviewPvLine.LineFacts]): Option[MoveReviewPvInterpretation] =
      for
        purpose <- linePurpose
        line <- lineFacts
        takeaway <- scopedTakeaway
      yield
        MoveReviewPvInterpretation(
          linePurpose = purpose,
          confirms = confirms,
          tension = "scoped_local",
          learningPoint = takeaway.text,
          supportedByLineId = Some(line.line.lineId),
          confidence = "bounded_local"
        )

  def describe(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      truthContract: Option[DecisiveTruthContract] = None,
      strictLocalFacts: Boolean = false
  ): Option[MoveReviewIdeaDescriptor] =
    val characterBand = moveCharacterBand(truthContract)
    MoveReviewIdeaRules.iterator
      .flatMap(_.describe(played, evidence, lineFacts, characterBand, truthContract))
      .filter(desc =>
        (!desc.requiresPvForAdmission || lineFacts.nonEmpty) &&
          (!strictLocalFacts || desc.localFact.strictFallbackEligible)
      )
      .nextOption()

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
      MoveReviewIdeaRule("forced_line_truth", forcedLineTruthDescriptor),
      MoveReviewIdeaRule("tactical", tacticalDescriptor),
      MoveReviewIdeaRule("target", targetDescriptor),
      MoveReviewIdeaRule("capture", captureDescriptor),
      MoveReviewIdeaRule("endgame", endgameDescriptor),
      MoveReviewIdeaRule("line_consequence", lineConsequenceDescriptor),
      MoveReviewIdeaRule("relation_witness", relationWitnessDescriptor),
      MoveReviewIdeaRule("position_probe_support", positionProbeSupportDescriptor),
      MoveReviewIdeaRule("practical_position_support", practicalPositionSupportDescriptor)
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
        localFact = admittedLocalFact(LocalFactCandidate(
          family = LocalFactFamily.OpeningGoal,
          source = LocalFactSource.OpeningGoalEvidence,
          producer = LocalFactProducer.OpeningGoal,
          subject = LocalFactSubject.OpeningGoal,
          strictFallbackCandidate = true,
          anchors = List(LocalFactAnchor("opening_goal", goal.goalName)),
          lineBinding = LocalFactLineBinding.PvCoupled,
          guardrails = List("opening_goal_status_achieved_or_partial", "pv_coupled")
        )),
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
        localFact = admittedLocalFact(LocalFactCandidate(
          family = LocalFactFamily.KingSafety,
          source = LocalFactSource.PvCoupledLine,
          producer = LocalFactProducer.CastlingSafety,
          subject = LocalFactSubject.KingSafety,
          strictFallbackCandidate = true,
          lineBinding = LocalFactLineBinding.PvCoupled,
          guardrails = List("castle_move", "pv_coupled")
        )),
        factFragments = List(FactFragment.KingSafetyFragment(played.san))
      )
    }

  private def positionProbeSupportDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      delta <- evidence.strategicDeltas.find(supportedLocalPositionProbe)
      line <- lineFacts
      row <- MoveReviewSupportedLocalSurfaceRows.positionProbeExactSliceRow(delta.packet)
    yield
      descriptor(
        ideaKind = "position_probe_support",
        reviewIntent = "quiet_improvement",
        moveCharacterBand = characterBand,
        source = "supported_local_position_probe",
        title = s"${played.san} has checked ${row.label.toLowerCase} support",
        baseProse = row.text,
        reasonTags = List(
          "supported_local_position_probe",
          s"proof_family:${delta.packet.proofFamily}",
          s"proof_source:${delta.packet.proofSource}",
          "scope:position_local",
          "no_move_cause_projection"
        ),
        linePurpose = None,
        lineProof = lineProof(
          "position_probe_support",
          played,
          line,
          subjectOverride = positionProbeSubject(delta).orElse(Some(played.uci))
        ),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(LocalFactCandidate(
          family = LocalFactFamily.Pressure,
          source = LocalFactSource.CertifiedStrategy,
          producer = LocalFactProducer.CertifiedStrategyDelta,
          subject = LocalFactSubject.Target,
          strictFallbackCandidate = true,
          anchors = positionProbeAnchors(delta),
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs = positionProbeEvidenceRefs(delta),
          guardrails = List(
            "supported_local_position_probe",
            "scope:position_local",
            "no_move_cause_projection",
            s"proof_family:${delta.packet.proofFamily}",
            s"proof_source:${delta.packet.proofSource}"
          )
        )),
        factFragments = Nil
      )

  private def practicalPositionSupportDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      fact <- evidence.practicalPositionFacts.headOption
      line <- lineFacts
    yield
      descriptor(
        ideaKind = "practical_position_support",
        reviewIntent = practicalPositionIntent(fact),
        moveCharacterBand = characterBand,
        source = "practical_position_support",
        title = s"${played.san} has checked ${fact.label.toLowerCase} support",
        baseProse = fact.text,
        reasonTags =
          List(
            "practical_position_support",
            s"strategic_idea_kind:${fact.ideaKind}",
            "source:strategy_pack",
            "scope:move_local"
          ) ++ fact.evidenceRefs,
        linePurpose = Some(practicalPositionLinePurpose(fact)),
        lineProof =
          lineProof(
            "practical_position_support",
            played,
            line,
            subjectOverride = fact.anchors.find(_.key == "target").map(_.value).orElse(Some(played.uci))
          ),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(LocalFactCandidate(
          family = fact.family,
          source = LocalFactSource.CertifiedStrategy,
          producer = LocalFactProducer.CertifiedStrategyDelta,
          subject = fact.subject,
          strictFallbackCandidate = false,
          anchors = fact.anchors,
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs = fact.evidenceRefs,
          guardrails = fact.guardrails
        )),
        factFragments = Nil
      )

  private def practicalPositionIntent(fact: PracticalPositionFact): String =
    fact.family match
      case LocalFactFamily.Defense     => "prevents_counterplay"
      case LocalFactFamily.PlanSupport => "advances_plan"
      case _                           => "quiet_improvement"

  private def practicalPositionLinePurpose(fact: PracticalPositionFact): String =
    fact.family match
      case LocalFactFamily.Defense     => "prevent_counterplay"
      case LocalFactFamily.PlanSupport => "advance_plan"
      case _                           => "quiet_improvement"

  private def strategicSupportDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      delta <- evidence.strategicDeltas.find(certifiedStrategicSupport)
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
        localFact = admittedLocalFact(
          MoveReviewLocalFact.strategicMoveDeltaCandidate(
            delta,
            anchors = preferredStrategicSubject(delta).map(subject => LocalFactAnchor("preferred_subject", subject)).toList,
            guardrails = List(
              s"proof_family:${delta.packet.proofFamily}",
              s"proof_source:${delta.packet.proofSource}",
              "strict_requires_causal_or_exact_fallback"
            ),
            strictFallbackCandidate = strictStrategicFallbackCandidate(delta, evidence)
          )
        ),
        factFragments = List(FactFragment.StrategicSupportFragment(
          san = played.san,
          proofFamily = delta.packet.proofFamily,
          proofSource = delta.packet.proofSource,
          purpose = strategicPurpose(played, delta)
        ))
      )

  private def forcedLineTruthDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      line <- lineFacts.filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci)
      theme <- evidence.forcedLineTheme
    yield
      val family = forcedLineTruthFamily(theme)
      descriptor(
        ideaKind = "forced_line_truth",
        reviewIntent = forcedLineTruthReviewIntent(family),
        moveCharacterBand = characterBand,
        source = "forced_line_truth",
        title = s"${played.san} has a confirmed ${theme.name} sequence",
        baseProse = forcedLineTruthText(played, theme),
        reasonTags = forcedLineTruthEvidenceRefs(theme),
        linePurpose = Some(forcedLineTruthPurpose(family)),
        lineProof = lineProof("forced_line_truth", played, line),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(LocalFactCandidate(
          family = family,
          source = LocalFactSource.PvCoupledLine,
          producer = LocalFactProducer.ForcedLineTruth,
          subject = LocalFactSubject.Target,
          strictFallbackCandidate = true,
          anchors = forcedLineTruthAnchors(theme),
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs = forcedLineTruthEvidenceRefs(theme),
          guardrails = forcedLineTruthGuardrails(theme)
        )),
        factFragments = Nil
      )

  private def forcedLineTruthFamily(theme: ForcedLineTruth.VerifiedTheme): LocalFactFamily =
    val key = s"${theme.id} ${theme.name}".toLowerCase
    if key.contains("stalemate") || key.contains("perpetual") then LocalFactFamily.Defense
    else if key.contains("mate") then LocalFactFamily.Attack
    else LocalFactFamily.Threat

  private def forcedLineTruthReviewIntent(family: LocalFactFamily): String =
    family match
      case LocalFactFamily.Defense => "finds_tactical_resource"
      case LocalFactFamily.Attack  => "creates_attack"
      case _                       => "creates_tactical_threat"

  private def forcedLineTruthPurpose(family: LocalFactFamily): String =
    family match
      case LocalFactFamily.Defense => "answer_direct_threat"
      case LocalFactFamily.Attack  => "create_tactical_attack"
      case _                       => "create_tactical_threat"

  private def forcedLineTruthText(
      played: PlayedMove,
      theme: ForcedLineTruth.VerifiedTheme
  ): String =
    Option(theme.line)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(line => s"${played.san} is tied to the confirmed ${theme.name} sequence: $line.")
      .getOrElse(s"${played.san} is tied to the confirmed ${theme.name} sequence.")

  private def forcedLineTruthEvidenceRefs(theme: ForcedLineTruth.VerifiedTheme): List[String] =
    (
      List(
        s"forced_line_theme:${theme.id}",
        s"forced_line_name:${theme.name}"
      ) ++ Option(theme.line).map(_.trim).filter(_.nonEmpty).map(line => s"forced_line_san:${line.take(80)}")
    ).distinct

  private def forcedLineTruthAnchors(theme: ForcedLineTruth.VerifiedTheme): List[LocalFactAnchor] =
    (
      List(
        LocalFactAnchor("forced_line_theme", theme.id),
        LocalFactAnchor("forced_line_name", theme.name)
      ) ++ Option(theme.line).map(_.trim).filter(_.nonEmpty).map(line => LocalFactAnchor("forced_line_san", line.take(80)))
    ).distinct

  private def forcedLineTruthGuardrails(theme: ForcedLineTruth.VerifiedTheme): List[String] =
    (
      List(
        "forced_line_truth_verified",
        "played_move_first",
        "pv_coupled",
        s"forced_line_theme:${theme.id}"
      ) ++ Option(theme.line).map(_.trim).filter(_.nonEmpty).map(_ => "forced_line_has_san")
    ).distinct

  private def tacticalDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    ownedTacticalEvidence(played, evidence, lineFacts).map { owned =>
      val kind = owned.kind
        val label = kind.replace("_", " ")
        val title =
          kind match
            case "trapped_piece" => s"${played.san} traps a piece"
            case _               => s"${played.san} creates a $label"
        val prose =
          kind match
            case "fork" =>
              s"${played.san} creates a fork that stays tied to the verified tactical targets."
            case "pin" =>
              s"${played.san} creates a pin tied to the pinned piece and the piece behind it."
            case "skewer" =>
              s"${played.san} creates a skewer tied to the front and back targets."
            case "discovered_attack" =>
              s"${played.san} creates a discovered attack tied to the revealed piece and target."
            case "trapped_piece" =>
              s"${played.san} traps a piece that has no safe escape in the verified position."
            case _ =>
              s"${played.san} creates a tactical threat tied to the verified targets."
        descriptor(
          ideaKind = kind,
          reviewIntent = "creates_threat",
          moveCharacterBand = characterBand,
          source = "canonical_fact",
          title = title,
          baseProse = prose,
          reasonTags = evidenceTags(owned.fact.toList, List(owned.motif)),
          linePurpose = Some("create_tactical_threat"),
          lineProof = lineProof("tactical_threat", played, lineFacts.get),
          played = played,
          evidence = evidence,
          lineFacts = lineFacts,
          requiresPvForAdmission = true,
          localFact = admittedLocalFact(LocalFactCandidate(
            family = LocalFactFamily.Threat,
            source = LocalFactSource.CanonicalFact,
            producer = LocalFactProducer.TacticalMotif,
            subject = LocalFactSubject.Target,
            strictFallbackCandidate = true,
            anchors =
              List(LocalFactAnchor("tactical_kind", owned.kind)) ++
                owned.targetSquares.distinct.map(square => LocalFactAnchor("tactical_square", square.key)),
            lineBinding = LocalFactLineBinding.PvCoupled,
            evidenceRefs = tacticalEvidenceRefs(owned),
            guardrails = List(s"tactical_kind:${owned.kind}", "current_move_motif_owned", "pv_confirms_tactic")
          )),
          factFragments = List(FactFragment.TacticalThreatFragment(
            san = played.san,
            kind = owned.kind,
            targets = owned.targetSquares.map(_.key)
          ))
        )
    }

  private final case class OwnedTacticalEvidence(
      kind: String,
      fact: Option[Fact],
      motif: Motif,
      targetSquares: List[Square]
  )

  private def ownedTacticalEvidence(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[OwnedTacticalEvidence] =
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
              OwnedTacticalEvidence("fork", Some(fact), immediateMotifs.find(motifOwnsFork(played, fact, _)).get, fact.participants)
          }
        val pin =
          evidence.facts.collectFirst {
            case fact: Fact.Pin
                if fact.attacker == played.to &&
                  immediateMotifs.exists(motif => motifOwnsPin(played, fact, motif)) &&
                  lineConfirmsTactic("pin", fact, line) =>
              OwnedTacticalEvidence("pin", Some(fact), immediateMotifs.find(motifOwnsPin(played, fact, _)).get, fact.participants)
          }
        val skewer =
          evidence.facts.collectFirst {
            case fact: Fact.Skewer
                if fact.attacker == played.to &&
                  immediateMotifs.exists(motif => motifOwnsSkewer(played, fact, motif)) &&
                  lineConfirmsTactic("skewer", fact, line) =>
              OwnedTacticalEvidence("skewer", Some(fact), immediateMotifs.find(motifOwnsSkewer(played, fact, _)).get, fact.participants)
          }
        val motifOnly =
          immediateMotifs.collectFirst(Function.unlift(motifOnlyTacticalEvidence(played, line)))
        List(fork, pin, skewer, motifOnly).flatten.headOption
      }

  private[commentary] def tacticalOwnershipRejectReasons(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): List[String] =
    if !hasTacticalForkPinSkewer(evidence) then Nil
    else if lineFacts.isEmpty then List("tactical_coupled_pv_missing")
    else if !lineFacts.exists(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci) then
      List("tactical_coupled_pv_mismatch")
    else if ownedTacticalEvidence(played, evidence, lineFacts).nonEmpty then Nil
    else
      val line = lineFacts.get
      val currentMotifs = evidence.motifs.filter(immediateMotifOwnsMove(played, _))
      val currentFacts = tacticalFacts(evidence.facts).filter(tacticalFactAttackerMatches(played, _))
      val matchingFactMotifs = currentTacticalFactMotifPairs(played, currentFacts, currentMotifs)
      val reasons = List.newBuilder[String]
      if currentMotifs.isEmpty then
        reasons += "tactical_raw_not_current_move_owned"
      else
        if line.reply.isEmpty then reasons += "tactical_coupled_pv_reply_missing"
        if currentMotifs.exists(tacticalMotifTargets(_).isEmpty) then
          reasons += "tactical_current_move_motif_target_missing"
        if currentFacts.nonEmpty && matchingFactMotifs.isEmpty then
          reasons += "tactical_current_fact_motif_mismatch"
        if matchingFactMotifs.nonEmpty &&
            !matchingFactMotifs.exists { case (kind, fact, _) => lineConfirmsTactic(kind, fact, line) }
        then
          reasons += "tactical_current_fact_unconfirmed_by_pv"
        if !currentMotifs.exists(motif => motifOnlyTacticalEvidence(played, line)(motif).nonEmpty) &&
            currentFacts.isEmpty
        then
          reasons += "tactical_current_move_motif_unconfirmed_by_pv"
      if currentFacts.nonEmpty && currentMotifs.isEmpty then
        reasons += "tactical_fact_attacker_matches_without_current_motif"
      reasons.result().distinct match
        case Nil => List("tactical_owned_evidence_rejected_by_descriptor")
        case xs  => xs

  private def hasTacticalForkPinSkewer(evidence: MoveReviewEvidence): Boolean =
    tacticalFacts(evidence.facts).nonEmpty || tacticalMotifs(evidence.motifs).nonEmpty

  private def tacticalFacts(facts: List[Fact]): List[Fact] =
    facts.collect {
      case fact: Fact.Fork   => fact
      case fact: Fact.Pin    => fact
      case fact: Fact.Skewer => fact
    }

  private def tacticalMotifs(motifs: List[Motif]): List[Motif] =
    motifs.collect {
      case motif: Motif.Fork             => motif
      case motif: Motif.Pin              => motif
      case motif: Motif.Skewer           => motif
      case motif: Motif.DiscoveredAttack => motif
      case motif: Motif.TrappedPiece     => motif
    }

  private def tacticalFactAttackerMatches(played: PlayedMove, fact: Fact): Boolean =
    fact match
      case fact: Fact.Fork   => fact.attacker == played.to
      case fact: Fact.Pin    => fact.attacker == played.to
      case fact: Fact.Skewer => fact.attacker == played.to
      case _                 => false

  private def currentTacticalFactMotifPairs(
      played: PlayedMove,
      facts: List[Fact],
      motifs: List[Motif]
  ): List[(String, Fact, Motif)] =
    facts.flatMap {
      case fact: Fact.Fork =>
        motifs.filter(motifOwnsFork(played, fact, _)).map(motif => ("fork", fact, motif))
      case fact: Fact.Pin =>
        motifs.filter(motifOwnsPin(played, fact, _)).map(motif => ("pin", fact, motif))
      case fact: Fact.Skewer =>
        motifs.filter(motifOwnsSkewer(played, fact, _)).map(motif => ("skewer", fact, motif))
      case _ => Nil
    }

  private def tacticalMotifTargets(motif: Motif): List[Square] =
    motif match
      case motif: Motif.Fork             => motif.targetSquares
      case motif: Motif.Pin              => List(motif.pinnedSq, motif.behindSq).flatten
      case motif: Motif.Skewer           => List(motif.frontSq, motif.backSq).flatten
      case motif: Motif.DiscoveredAttack => List(motif.attackingSq, motif.targetSq).flatten
      case motif: Motif.TrappedPiece     => List(motif.trappedSquare)
      case _                             => Nil

  private def motifOnlyTacticalEvidence(
      played: PlayedMove,
      line: MoveReviewPvLine.LineFacts
  )(motif: Motif): Option[OwnedTacticalEvidence] =
    motif match
      case m: Motif.Fork if m.square == played.to && m.targetSquares.nonEmpty && line.reply.nonEmpty =>
        Some(OwnedTacticalEvidence("fork", None, m, (m.square :: m.targetSquares).distinct))
      case m: Motif.Pin
          if m.pinningSq.contains(played.to) &&
            m.pinnedSq.nonEmpty &&
            m.behindSq.nonEmpty &&
            continuationTouchesAny(line, List(m.pinnedSq, m.behindSq).flatten) =>
        Some(OwnedTacticalEvidence("pin", None, m, List(m.pinningSq, m.pinnedSq, m.behindSq).flatten.distinct))
      case m: Motif.Skewer
          if m.attackingSq.contains(played.to) &&
            m.frontSq.nonEmpty &&
            m.backSq.nonEmpty &&
            continuationTouchesAny(line, List(m.frontSq, m.backSq).flatten) =>
        Some(OwnedTacticalEvidence("skewer", None, m, List(m.attackingSq, m.frontSq, m.backSq).flatten.distinct))
      case m: Motif.DiscoveredAttack
          if m.movingSq.contains(played.to) &&
            m.attackingSq.nonEmpty &&
            m.targetSq.nonEmpty &&
            lineConfirmsDiscoveredAttack(m, line) =>
        Some(OwnedTacticalEvidence("discovered_attack", None, m, List(m.movingSq, m.attackingSq, m.targetSq).flatten.distinct))
      case m: Motif.TrappedPiece
          if m.isValuableTrap &&
            line.reply.nonEmpty &&
            playedAttacksTrappedPiece(played, line, m) =>
        Some(OwnedTacticalEvidence("trapped_piece", None, m, List(played.to, m.trappedSquare).distinct))
      case _ => None

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
      case m: Motif.DiscoveredAttack =>
        m.plyIndex == 0 &&
          m.color == played.color &&
          m.movingPiece == played.role &&
          m.movingSq.contains(played.to) &&
          m.move.exists(sanEquivalent(_, played.san))
      case m: Motif.TrappedPiece =>
        m.plyIndex == 0 &&
          m.color == !played.color &&
          m.isValuableTrap &&
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

  private def lineConfirmsDiscoveredAttack(
      motif: Motif.DiscoveredAttack,
      line: MoveReviewPvLine.LineFacts
  ): Boolean =
    motif.target == King ||
      motif.targetSq.exists(square => lineTouchesAny(line, List(square)))

  private def playedAttacksTrappedPiece(
      played: PlayedMove,
      line: MoveReviewPvLine.LineFacts,
      motif: Motif.TrappedPiece
  ): Boolean =
    Fen.read(chess.variant.Standard, Fen.Full(line.first.fenAfter)).exists { position =>
      position.board
        .attackers(motif.trappedSquare, played.color)
        .squares
        .contains(played.to)
    }

  private def continuationTouchesAny(line: MoveReviewPvLine.LineFacts, squares: List[Square]): Boolean =
    val beforeFen = line.reply.map(_.fenAfter).getOrElse(line.first.fenAfter)
    val continuations = line.checkedContinuations
    MoveReviewExchangeAnalyzer
      .boundedReplay(beforeFen, continuations.map(_.uci), maxPlies = continuations.size.min(4))
      .exists(_.exists(step => squares.exists(square => step.move.orig == square || step.move.dest == square)))

  private def lineTouchesAny(line: MoveReviewPvLine.LineFacts, squares: List[Square]): Boolean =
    line.reply.exists(moveTouchesAny(_, squares)) ||
      continuationTouchesAny(line, squares)

  private def moveTouchesAny(move: MoveReviewMoveRef, squares: List[Square]): Boolean =
    val uci = MoveReviewPvLine.normalizeUci(move.uci)
    squares.exists(square => uci.take(2) == square.key || uci.slice(2, 4) == square.key)

  private def legalOnePlyStep(
      beforeFen: String,
      move: MoveReviewMoveRef
  ): Option[MoveReviewExchangeAnalyzer.BoundedReplayStep] =
    MoveReviewExchangeAnalyzer
      .boundedReplay(beforeFen, List(move.uci), maxPlies = 1)
      .flatMap(_.headOption)

  private def sanEquivalent(left: String, right: String): Boolean =
    def clean(value: String): String =
      Option(value).getOrElse("").trim.replaceAll("""[+#?!]+$""", "")
    clean(left) == clean(right)

  private def tacticalEvidenceRefs(owned: OwnedTacticalEvidence): List[String] =
    (
      List(
        s"tactical_kind:${owned.kind}",
        "motif_owner:current_move"
      ) ++
        owned.fact.toList.flatMap(factEvidenceRefs) ++
        owned.targetSquares.distinct.map(square => s"motif_square:${square.key}")
    ).distinct

  private def factEvidenceRefs(fact: Fact): List[String] =
    (
      List(
        s"fact_kind:${factKind(fact)}",
        s"fact_scope:${tokenKey(fact.scope)}"
      ) ++ fact.participants.distinct.map(square => s"fact_square:${square.key}") ++
        factRoleRefs(fact)
    ).distinct

  private def factRoleRefs(fact: Fact): List[String] =
    fact match
      case hanging: Fact.HangingPiece => List(s"fact_role:${roleKey(hanging.role)}")
      case target: Fact.TargetPiece   => List(s"fact_role:${roleKey(target.role)}")
      case pin: Fact.Pin =>
        List(
          s"attacker_role:${roleKey(pin.attackerRole)}",
          s"pinned_role:${roleKey(pin.pinnedRole)}",
          s"behind_role:${roleKey(pin.behindRole)}"
        )
      case skewer: Fact.Skewer =>
        List(
          s"attacker_role:${roleKey(skewer.attackerRole)}",
          s"front_role:${roleKey(skewer.frontRole)}",
          s"back_role:${roleKey(skewer.backRole)}"
        )
      case fork: Fact.Fork =>
        s"attacker_role:${roleKey(fork.attackerRole)}" :: fork.targets.map { case (_, role) => s"target_role:${roleKey(role)}" }
      case _ => Nil

  private def factKind(fact: Fact): String =
    tokenKey(fact.getClass.getSimpleName.stripSuffix("$"))

  private def roleKey(role: Role): String =
    tokenKey(role)

  private def tokenKey(value: Any): String =
    Option(value).map(_.toString).getOrElse("").replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase

  private def targetDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    val threatLineFacts = evidence.facts.filter(_.scope == FactScope.ThreatLine)
    val pressureFact = ownedTargetPressureFact(played, evidence)
    val defensive = defensiveTruth(truthContract) && threatLineFacts.nonEmpty
    val offensive = pressureFact.exists(owned => !owned.postMoveStatic || postMovePressureAllowed(played, evidence, owned.fact))
    val relationPreemptsTarget = offensive && relationPreemptsTargetPressure(played, evidence, pressureFact)
    Option.when(
      lineFacts.flatMap(_.reply).nonEmpty &&
        (defensive || (offensive && !relationPreemptsTarget))
    ) {
      val purpose = if defensive then "answer_direct_threat" else "create_tactical_threat"
      val intent = if defensive then "answers_threat" else "creates_threat"
      val proofKind = if defensive then "defensive_answer" else "target_pressure"
      descriptor(
        ideaKind = "direct_threat",
        reviewIntent = intent,
        moveCharacterBand = characterBand,
        source = "canonical_fact",
        title = if defensive then s"${played.san} has a defensive fact" else s"${played.san} has a local target fact",
        baseProse =
          if defensive then s"${played.san} is tied to a concrete defensive detail in the checked line."
          else s"${played.san} is tied to a concrete local target.",
        reasonTags = evidenceTags(evidence.facts, evidence.motifs),
        linePurpose = Some(purpose),
        lineProof = lineProof(proofKind, played, lineFacts.get),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(LocalFactCandidate(
          family = if defensive then LocalFactFamily.Defense else LocalFactFamily.Pressure,
          source = if defensive then LocalFactSource.OnlyMoveDefense else LocalFactSource.CanonicalFact,
          producer = if defensive then LocalFactProducer.DefensiveTruth else LocalFactProducer.TargetPressure,
          subject = LocalFactSubject.Target,
          strictFallbackCandidate = true,
          anchors =
            if defensive then
              threatLineFacts.flatMap(_.participants).distinct.map(square => LocalFactAnchor("threat_line_square", square.key))
            else
              pressureFact.toList.flatMap(_.fact.participants).distinct.map(square => LocalFactAnchor("target_fact_square", square.key)),
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs =
            if defensive then
              truthContractEvidenceRefs(truthContract) ++ threatLineFacts.flatMap(factEvidenceRefs)
            else pressureFact.toList.flatMap(pressureFactEvidenceRefs),
          guardrails =
            if defensive then List("only_move_defense_truth", "pv_coupled")
            else
              (
                List("target_fact_attacked_by_played_move", "pv_coupled") ++
                  Option.when(pressureFact.exists(_.postMoveStatic))("post_move_static_target")
              ).distinct
        )),
        factFragments = List(FactFragment.DirectThreatFragment(
          san = played.san,
          isDefensive = defensive,
          reason = if defensive then "answer_direct_threat" else "create_tactical_threat"
        ))
      )
    }

  private def relationPreemptsTargetPressure(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      pressureFact: Option[OwnedPressureFact]
  ): Boolean =
    pressureFact.flatMap(ownedPressureTarget).exists { target =>
      evidence.relationWitnesses
        .flatMap(relationWitnessEvidence(played, evidence, _))
        .exists(relation =>
          relation.projection.targetSquare.contains(target) &&
            (
              relation.descriptor.surfaceRowKind == RelationSurfaceRowKind.MobilityRestriction ||
                relation.descriptor.surfaceRowKind == RelationSurfaceRowKind.MoveOrder
            )
        )
    }

  private def ownedPressureTarget(owned: OwnedPressureFact): Option[String] =
    owned.fact match
      case Fact.TargetPiece(square, _, _, _, _)  => Some(square.key)
      case Fact.HangingPiece(square, _, _, _, _) => Some(square.key)
      case _                                     => None

  private final case class OwnedPressureFact(fact: Fact, postMoveStatic: Boolean)

  private def ownedTargetPressureFact(
      played: PlayedMove,
      evidence: MoveReviewEvidence
  ): Option[OwnedPressureFact] =
    evidence.facts.collectFirst {
      case fact: Fact.TargetPiece if fact.attackers.contains(played.to) => fact
      case fact: Fact.HangingPiece if fact.attackers.contains(played.to) => fact
    }.map(fact => OwnedPressureFact(fact, postMoveStatic = false))
      .orElse(
        evidence.postMoveTargetFacts.collectFirst {
          case fact: Fact.TargetPiece if fact.attackers.contains(played.to) => fact
          case fact: Fact.HangingPiece if fact.attackers.contains(played.to) => fact
        }.map(fact => OwnedPressureFact(fact, postMoveStatic = true))
      )

  private def postMovePressureAllowed(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      fact: Fact
  ): Boolean =
    evidence.strictLocalFacts &&
      !played.isCapture &&
      !evidence.phase.trim.equalsIgnoreCase("endgame") &&
      meaningfulPressureTarget(fact)

  private def meaningfulPressureTarget(fact: Fact): Boolean =
    fact match
      case Fact.HangingPiece(_, role, _, _, _) => role != King
      case Fact.TargetPiece(_, role, _, _, _)  => role != King && role != chess.Pawn
      case _                                   => false

  private def pressureFactEvidenceRefs(owned: OwnedPressureFact): List[String] =
    (
      factEvidenceRefs(owned.fact) ++
        Option.when(owned.postMoveStatic)("fact_source:post_move_static").toList
    ).distinct

  private def truthContractEvidenceRefs(truthContract: Option[DecisiveTruthContract]): List[String] =
    truthContract.toList.flatMap(contract =>
      List(
        s"truth_class:${tokenKey(contract.truthClass)}",
        s"truth_reason:${tokenKey(contract.reasonFamily)}",
        s"truth_failure:${tokenKey(contract.failureMode)}",
        s"truth_visibility:${tokenKey(contract.visibilityRole)}",
        s"truth_surface:${tokenKey(contract.surfaceMode)}"
      )
    )

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
        localFact = admittedLocalFact(LocalFactCandidate(
          family = LocalFactFamily.Capture,
          source = LocalFactSource.PvCoupledLine,
          producer = LocalFactProducer.CaptureSequence,
          subject = LocalFactSubject.Capture,
          strictFallbackCandidate = true,
          lineBinding = LocalFactLineBinding.PvCoupled,
          guardrails = List("capture_motif", "immediate_recapture")
        )),
        factFragments = List(FactFragment.CaptureFragment(
          san = played.san,
          purpose = purpose
        ))
      )
    }

  private def lineConsequenceDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    Option.unless(hasDrawResourceRelationEvidence(played, evidence)) {
      for
        lineEvidence <- evidence.lineConsequence.filter(validLineConsequence(played, evidence, _))
        line <- lineFacts.filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci)
        purpose <- lineConsequencePurpose(lineEvidence)
      yield
        descriptor(
          ideaKind = "line_consequence",
          reviewIntent = lineConsequenceIntent(lineEvidence),
          moveCharacterBand = characterBand,
          source = "line_consequence",
          title = s"${played.san} has a checked line consequence",
          baseProse = s"${played.san} is tied to a checked ${lineConsequenceLabel(lineEvidence.kind)}.",
          reasonTags =
            List("line_consequence", s"line_consequence_kind:${tokenKey(lineEvidence.kind)}") ++
              MoveReviewLocalFact.lineConsequenceEvidenceRefs(lineEvidence),
          linePurpose = Some(purpose),
          lineProof = lineProof("line_consequence", played, line, blockedReasons = lineEvidence.rejectReasons),
          played = played,
          evidence = evidence,
          lineFacts = lineFacts,
          requiresPvForAdmission = true,
          localFact = admittedLocalFact(MoveReviewLocalFact.lineConsequenceCandidate(lineEvidence, strictFallbackCandidate = true)),
          factFragments = Nil
        )
    }.flatten

  private def validLineConsequence(
      played: PlayedMove,
      moveEvidence: MoveReviewEvidence,
      lineEvidence: LineConsequenceEvidence
  ): Boolean =
    playedOwnsLineConsequence(played, lineEvidence) &&
      lineEvidence.surfaceReady &&
      lineEvidence.kind != LineConsequenceKind.PreviewOnly &&
      !endgameCentralLineConsequence(moveEvidence, lineEvidence) &&
      lineEvidence.uciMoves.headOption.exists(uci => MoveReviewPvLine.normalizeUci(uci) == played.uci)

  private def playedOwnsLineConsequence(
      played: PlayedMove,
      lineEvidence: LineConsequenceEvidence
  ): Boolean =
    !played.isCapture ||
      (
        lineEvidence.triggerSan.exists(sanEquivalent(_, played.san)) &&
          Set(
            LineConsequenceKind.ExchangeSequence,
            LineConsequenceKind.ForcingCheckSequence,
            LineConsequenceKind.MaterialTransition,
            LineConsequenceKind.PassedPawnCreation,
            LineConsequenceKind.PromotionRace
          ).contains(lineEvidence.kind)
      )

  private def endgameCentralLineConsequence(
      moveEvidence: MoveReviewEvidence,
      lineEvidence: LineConsequenceEvidence
  ): Boolean =
    moveEvidence.phase.trim.equalsIgnoreCase("endgame") &&
      (lineEvidence.kind == LineConsequenceKind.CentralBreakTiming || lineEvidence.kind == LineConsequenceKind.CentralPawnAdvance)

  private def lineConsequencePurpose(evidence: LineConsequenceEvidence): Option[String] =
    evidence.kind match
      case LineConsequenceKind.ExchangeSequence | LineConsequenceKind.MaterialTransition =>
        Some("clarify_exchange")
      case LineConsequenceKind.CentralBreakTiming =>
        Some("center_break_setup")
      case LineConsequenceKind.CentralPawnAdvance =>
        Some("challenge_center")
      case LineConsequenceKind.PassedPawnCreation =>
        Some("create_passed_pawn")
      case LineConsequenceKind.PromotionRace =>
        Some("promotion_race")
      case LineConsequenceKind.ForcingCheckSequence =>
        Some("force_sequence")
      case LineConsequenceKind.PreviewOnly =>
        None

  private def lineConsequenceIntent(evidence: LineConsequenceEvidence): String =
    evidence.kind match
      case LineConsequenceKind.ExchangeSequence | LineConsequenceKind.MaterialTransition =>
        "clarifies_exchange"
      case LineConsequenceKind.CentralBreakTiming | LineConsequenceKind.CentralPawnAdvance =>
        "challenges_center"
      case LineConsequenceKind.PassedPawnCreation =>
        "creates_passed_pawn"
      case LineConsequenceKind.PromotionRace =>
        "pushes_promotion_race"
      case LineConsequenceKind.ForcingCheckSequence =>
        "starts_forcing_line"
      case LineConsequenceKind.PreviewOnly =>
        "line_preview"

  private def lineConsequenceLabel(kind: LineConsequenceKind): String =
    kind match
      case LineConsequenceKind.ExchangeSequence =>
        "exchange sequence"
      case LineConsequenceKind.ForcingCheckSequence =>
        "forcing sequence"
      case LineConsequenceKind.CentralBreakTiming =>
        "central-break timing detail"
      case LineConsequenceKind.CentralPawnAdvance =>
        "central pawn advance"
      case LineConsequenceKind.MaterialTransition =>
        "material transition"
      case LineConsequenceKind.PassedPawnCreation =>
        "passed pawn creation"
      case LineConsequenceKind.PromotionRace =>
        "promotion race"
      case LineConsequenceKind.PreviewOnly =>
        "line preview"

  private final case class RelationWitnessEvidence(
      witness: MoveReviewExchangeAnalyzer.RelationWitness,
      projection: MoveReviewExchangeAnalyzer.RelationProjection,
      descriptor: RelationObservationDescriptor,
      family: LocalFactFamily
  )

  private def relationWitnessDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      line <- lineFacts.filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci)
      relationEvidence <- evidence.relationWitnesses
        .flatMap(relationWitnessEvidence(played, evidence, _))
        .headOption
      purpose <- relationLinePurpose(relationEvidence)
    yield
      val label = relationEvidence.descriptor.publicLabel
      descriptor(
        ideaKind = relationIdeaKind(relationEvidence),
        reviewIntent = relationReviewIntent(relationEvidence),
        moveCharacterBand = characterBand,
        source = "relation_witness",
        title = s"${played.san} has a checked $label relation",
        baseProse = relationLocalFactText(played, relationEvidence),
        reasonTags = relationEvidenceRefs(relationEvidence),
        linePurpose = Some(purpose),
        lineProof = lineProof("relation_witness", played, line),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(relationLocalFactCandidate(relationEvidence)),
        factFragments = Nil
      )

  private def relationLocalFactCandidate(
      evidence: RelationWitnessEvidence,
      strictFallbackCandidate: Boolean = true
  ): LocalFactCandidate =
    LocalFactCandidate(
      family = evidence.family,
      source = LocalFactSource.PvCoupledLine,
      producer = LocalFactProducer.RelationWitness,
      subject = LocalFactSubject.Target,
      strictFallbackCandidate = strictFallbackCandidate,
      anchors = relationAnchors(evidence),
      lineBinding = LocalFactLineBinding.PvCoupled,
      evidenceRefs = relationEvidenceRefs(evidence),
      guardrails = relationGuardrails(evidence),
      relationSurface = Some(evidence.descriptor.surfaceRowKind)
    )

  private def relationLocalFactText(
      played: PlayedMove,
      evidence: RelationWitnessEvidence
  ): String =
    s"${played.san} is tied to a checked ${evidence.descriptor.publicLabel} relation in the PV."

  private def relationWitnessEvidence(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      witness: MoveReviewExchangeAnalyzer.RelationWitness
  ): Option[RelationWitnessEvidence] =
    if validRelationWitness(played, evidence, witness) then
      for
        projection <- MoveReviewExchangeAnalyzer.relationProjectionFromWitness(witness)
        descriptor <- RelationObservationCatalog.descriptorForKind(projection.kind)
        family <- relationFamily(witness, projection.kind, descriptor)
      yield RelationWitnessEvidence(witness, projection, descriptor, family)
    else None

  private def validRelationWitness(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      witness: MoveReviewExchangeAnalyzer.RelationWitness
  ): Boolean =
    val drawResource = relationWitnessDrawResource(witness)
    relationWitnessSurfaceEligible(evidence, witness) &&
      (!played.isCapture || drawResource) &&
      (!evidence.phase.trim.equalsIgnoreCase("endgame") || drawResource) &&
      MoveReviewExchangeAnalyzer.relationDetailsValidForKind(witness) &&
      witness.lineMoves.headOption.exists(uci => MoveReviewPvLine.normalizeUci(uci) == played.uci)

  private def relationWitnessSurfaceEligible(
      evidence: MoveReviewEvidence,
      witness: MoveReviewExchangeAnalyzer.RelationWitness
  ): Boolean =
    evidence.strictLocalFacts || relationWitnessTacticalSurface(witness) || relationWitnessDrawResource(witness)

  private def relationWitnessTacticalSurface(witness: MoveReviewExchangeAnalyzer.RelationWitness): Boolean =
    MoveReviewExchangeAnalyzer
      .relationProjectionFromWitness(witness)
      .flatMap(projection => RelationObservationCatalog.descriptorForKind(projection.kind))
      .exists(_.surfaceRowKind == RelationSurfaceRowKind.TacticalRelation)

  private def relationWitnessDrawResource(witness: MoveReviewExchangeAnalyzer.RelationWitness): Boolean =
    MoveReviewExchangeAnalyzer
      .relationProjectionFromWitness(witness)
      .flatMap(projection => RelationObservationCatalog.descriptorForKind(projection.kind))
      .exists(_.surfaceRowKind == RelationSurfaceRowKind.DrawResource)

  private def hasDrawResourceRelationEvidence(
      played: PlayedMove,
      evidence: MoveReviewEvidence
  ): Boolean =
    evidence.relationWitnesses.exists(witness =>
      relationWitnessEvidence(played, evidence, witness).exists(_.descriptor.surfaceRowKind == RelationSurfaceRowKind.DrawResource)
    )

  private def relationFamily(
      witness: MoveReviewExchangeAnalyzer.RelationWitness,
      kind: String,
      descriptor: RelationObservationDescriptor
  ): Option[LocalFactFamily] =
    kind match
      case _ if descriptor.surfaceRowKind == RelationSurfaceRowKind.MobilityRestriction =>
        Some(LocalFactFamily.Pressure)
      case _ if descriptor.surfaceRowKind == RelationSurfaceRowKind.MoveOrder =>
        Some(LocalFactFamily.Timing)
      case _ if descriptor.surfaceRowKind == RelationSurfaceRowKind.DrawResource =>
        Some(LocalFactFamily.Defense)
      case _ if relationTargetsKing(witness) =>
        Some(LocalFactFamily.Attack)
      case MoveReviewExchangeAnalyzer.RelationKind.XRay |
          MoveReviewExchangeAnalyzer.RelationKind.Battery |
          MoveReviewExchangeAnalyzer.RelationKind.Pin |
          MoveReviewExchangeAnalyzer.RelationKind.Skewer |
          MoveReviewExchangeAnalyzer.RelationKind.Clearance |
          MoveReviewExchangeAnalyzer.RelationKind.Interference =>
        Some(LocalFactFamily.Pressure)
      case _ if descriptor.surfaceRowKind == RelationSurfaceRowKind.TacticalRelation =>
        Some(LocalFactFamily.Threat)
      case _ =>
        None

  private def relationTargetsKing(witness: MoveReviewExchangeAnalyzer.RelationWitness): Boolean =
    MoveReviewExchangeAnalyzer
      .relationProjectionFromWitness(witness)
      .exists(projection =>
        projection.factTerms.exists(term =>
          term == "target_role:king" ||
            term == "behind_role:king" ||
            term == "front_role:king" ||
            term.startsWith("king:")
        )
      )

  private def relationLinePurpose(evidence: RelationWitnessEvidence): Option[String] =
    evidence.descriptor.surfaceRowKind match
      case RelationSurfaceRowKind.MobilityRestriction => Some("restrict_piece_mobility")
      case RelationSurfaceRowKind.MoveOrder           => Some("move_order_timing")
      case _ =>
        evidence.family match
          case LocalFactFamily.Attack | LocalFactFamily.Threat | LocalFactFamily.Pressure => Some("create_tactical_threat")
          case LocalFactFamily.Defense                                                    => Some("answer_direct_threat")
          case _                                                                          => None

  private def relationIdeaKind(evidence: RelationWitnessEvidence): String =
    evidence.descriptor.surfaceRowKind match
      case RelationSurfaceRowKind.DrawResource         => "draw_resource"
      case RelationSurfaceRowKind.MobilityRestriction  => "mobility_restriction"
      case RelationSurfaceRowKind.MoveOrder            => "move_order"
      case _ =>
        evidence.family match
          case LocalFactFamily.Defense  => "direct_threat"
          case LocalFactFamily.Attack   => "direct_attack"
          case LocalFactFamily.Pressure => "direct_threat"
          case _                        => "tactical"

  private def relationReviewIntent(evidence: RelationWitnessEvidence): String =
    evidence.descriptor.surfaceRowKind match
      case RelationSurfaceRowKind.DrawResource         => "draws_resource"
      case RelationSurfaceRowKind.MobilityRestriction  => "restricts_mobility"
      case RelationSurfaceRowKind.MoveOrder            => "times_move_order"
      case _ =>
        evidence.family match
          case LocalFactFamily.Defense  => "answers_threat"
          case LocalFactFamily.Attack   => "creates_attack"
          case LocalFactFamily.Pressure => "creates_threat"
          case _                        => "creates_tactical_threat"

  private def relationEvidenceRefs(evidence: RelationWitnessEvidence): List[String] =
    (
      List(
        s"relation_kind:${evidence.projection.kind}",
        s"relation_source:${evidence.descriptor.source.wireKey}",
        s"relation_observation:${evidence.descriptor.observationId.wireKey}"
      ) ++
        evidence.descriptor.wireEvidenceRefs ++
        evidence.projection.factTerms.map(term => s"relation_fact:$term") ++
        evidence.projection.lineMoves.take(6).map(uci => s"line_move:${MoveReviewPvLine.normalizeUci(uci)}")
    ).distinct

  private def relationAnchors(evidence: RelationWitnessEvidence): List[LocalFactAnchor] =
    (
      evidence.projection.targetSquare.toList.map(square => LocalFactAnchor("relation_target", square)) ++
        evidence.projection.focusSquares.map(square => LocalFactAnchor("relation_square", square))
    ).distinct

  private def relationGuardrails(evidence: RelationWitnessEvidence): List[String] =
    List(
      "relation_witness_typed_details",
      "fen_validated_line_replayed",
      "played_move_first",
      s"relation_kind:${evidence.projection.kind}",
      s"relation_surface:${tokenKey(evidence.descriptor.surfaceRowKind)}"
    ).distinct

  private def endgameDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    lineFacts.filter(_ => ownedEndgameFacts(played, evidence).nonEmpty).map { line =>
      val ownedFacts = ownedEndgameFacts(played, evidence)
      descriptor(
        ideaKind = "endgame_activity",
        reviewIntent = "improves_endgame_activity",
        moveCharacterBand = characterBand,
        source = "canonical_fact",
        title = s"${played.san} has an endgame fact",
        baseProse =
          if ownedFacts.exists(_.isInstanceOf[Fact.KingActivity]) then
            s"${played.san} is tied to a concrete king-activity detail."
          else s"${played.san} is tied to a concrete endgame detail.",
        reasonTags = evidenceTags(evidence.facts, evidence.motifs),
        linePurpose = Some("improve_endgame_activity"),
        lineProof = lineProof("endgame_activity", played, line),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(LocalFactCandidate(
          family = LocalFactFamily.Endgame,
          source = LocalFactSource.CanonicalFact,
          producer = LocalFactProducer.EndgameFact,
          subject = LocalFactSubject.Endgame,
          strictFallbackCandidate = true,
          lineBinding = LocalFactLineBinding.PvCoupled,
          guardrails = List("endgame_fact_owned_by_played_move", "pv_coupled")
        )),
        factFragments = List(FactFragment.EndgameFragment(
          san = played.san,
          facts = ownedFacts.map(_.toString)
        ))
      )
    }

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
      localFact: LocalFactAdmission,
      factFragments: List[FactFragment]
  ): MoveReviewIdeaDescriptor =
    val movePurpose = baseProse.trim
    val expandedReasonTags =
      (reasonTags ++
        lineProof.tags ++
        localFact.tags ++
        List(s"review_intent:$reviewIntent", s"character_band:${moveCharacterBand.key}")).distinct
    val confirms = PvMeaning.confirmedFacts(reviewIntent, linePurpose, evidence, expandedReasonTags)
    val scopedTakeaway =
      linePurpose.flatMap(purpose => MoveReviewScopedTakeaway.build(purpose, played, evidence, lineFacts, localFact))
    MoveReviewIdeaDescriptor(
      ideaKind = ideaKind,
      reviewIntent = reviewIntent,
      moveCharacterBand = moveCharacterBand,
      source = source,
      title = title,
      baseProse = baseProse,
      movePurpose = movePurpose,
      reasonTags = expandedReasonTags,
      confirms = confirms,
      linePurpose = linePurpose,
      learningPoint = scopedTakeaway.map(_.text),
      scopedTakeaway = scopedTakeaway,
      requiresPvForAdmission = requiresPvForAdmission,
      localFact = localFact,
      factFragments = factFragments
    )

  private def admittedLocalFact(candidate: LocalFactCandidate): LocalFactAdmission =
    MoveReviewLocalFact.admitted(candidate)

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

  private[commentary] def practicalPositionFacts(
      played: PlayedMove,
      surface: StrategyPackSurface.Snapshot
  ): List[PracticalPositionFact] =
    if surface.ownerMismatch then Nil
    else
      surface.allIdeas
        .flatMap(practicalPositionFact(played, surface, _))
        .distinctBy(fact => (fact.family, fact.ideaKind, fact.anchors.map(anchor => anchor.key -> anchor.value)))
        .sortBy(fact => practicalPositionPriority(fact))

  private def practicalPositionFact(
      played: PlayedMove,
      surface: StrategyPackSurface.Snapshot,
      idea: StrategyIdeaSignal
  ): Option[PracticalPositionFact] =
    val sideMatches =
      normalizeSide(idea.ownerSide).forall(_ == playedSide(played))
    val confidenceFloor =
      if idea.readiness == StrategicIdeaReadiness.Ready then 0.70 else 0.82
    val anchor = practicalPositionAnchor(played, surface, idea)
    for
      anchorValue <- anchor
      if sideMatches
      if idea.confidence >= confidenceFloor
      if idea.evidenceRefs.nonEmpty
      family <- practicalPositionFamily(idea)
    yield
      val label = practicalPositionLabel(idea)
      val evidenceRefs = practicalPositionEvidenceRefs(idea, anchorValue)
      PracticalPositionFact(
        family = family,
        subject =
          if family == LocalFactFamily.Pressure then LocalFactSubject.Target
          else LocalFactSubject.PlanResource,
        ideaKind = idea.kind,
        label = label,
        text = practicalPositionText(played, label, family, anchorValue),
        anchors =
          List(
            LocalFactAnchor("target", anchorValue),
            LocalFactAnchor("strategic_idea_kind", idea.kind)
          ) ++ idea.focusFiles.take(2).map(file => LocalFactAnchor("focus_file", file)),
        evidenceRefs = evidenceRefs,
        guardrails =
          List(
            "practical_position_support",
            "source:strategy_pack",
            "move_touches_strategy_anchor",
            "pv_coupled",
            s"strategic_idea_kind:${idea.kind}",
            s"strategic_readiness:${idea.readiness}",
            f"strategic_confidence:${idea.confidence}%.2f"
          ) ++ evidenceRefs
      )

  private def practicalPositionFamily(idea: StrategyIdeaSignal): Option[LocalFactFamily] =
    idea.kind match
      case StrategicIdeaKind.CounterplaySuppression | StrategicIdeaKind.Prophylaxis =>
        Some(LocalFactFamily.Defense)
      case StrategicIdeaKind.PawnBreak =>
        Some(LocalFactFamily.PlanSupport)
      case StrategicIdeaKind.TargetFixing | StrategicIdeaKind.LineOccupation |
          StrategicIdeaKind.OutpostCreationOrOccupation | StrategicIdeaKind.SpaceGainOrRestriction |
          StrategicIdeaKind.KingAttackBuildUp =>
        Some(LocalFactFamily.Pressure)
      case _ => None

  private def practicalPositionLabel(idea: StrategyIdeaSignal): String =
    idea.kind match
      case StrategicIdeaKind.CounterplaySuppression => "Counterplay restraint"
      case StrategicIdeaKind.Prophylaxis            => "Prophylaxis"
      case StrategicIdeaKind.PawnBreak              => "Pawn-break"
      case StrategicIdeaKind.TargetFixing           => "Target pressure"
      case StrategicIdeaKind.LineOccupation         => "Line pressure"
      case StrategicIdeaKind.OutpostCreationOrOccupation => "Outpost"
      case StrategicIdeaKind.SpaceGainOrRestriction => "Space"
      case StrategicIdeaKind.KingAttackBuildUp      => "King pressure"
      case _                                        => "Strategic"

  private def practicalPositionText(
      played: PlayedMove,
      label: String,
      family: LocalFactFamily,
      anchor: String
  ): String =
    family match
      case LocalFactFamily.Defense =>
        s"${played.san} is tied to checked counterplay restraint around $anchor."
      case LocalFactFamily.PlanSupport =>
        s"${played.san} is tied to checked plan support around $anchor."
      case _ =>
        s"${played.san} is tied to checked ${label.toLowerCase} around $anchor."

  private def practicalPositionAnchor(
      played: PlayedMove,
      surface: StrategyPackSurface.Snapshot,
      idea: StrategyIdeaSignal
  ): Option[String] =
    val playedTo = played.to.key
    val playedFrom = played.from.key
    val playedFile = playedTo.take(1)
    val squareAnchors =
      (idea.targetSquare.toList ++ idea.focusSquares ++ idea.relationFocusSquares)
        .map(_.trim.toLowerCase)
        .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
        .distinct
    val fileAnchors =
      idea.focusFiles
        .map(_.trim.toLowerCase)
        .filter(_.matches("^[a-h]$"))
        .distinct
    val routeAnchors =
      surface.allRoutes
        .filter(route => normalizeSide(route.ownerSide).forall(_ == playedSide(played)))
        .filter(route => route.from == playedFrom || route.route.contains(playedTo))
        .flatMap(route => route.route.lastOption.toList ++ route.route)
        .map(_.trim.toLowerCase)
        .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
    val moveRefAnchors =
      surface.allMoveRefs
        .filter(ref => normalizeSide(ref.ownerSide).forall(_ == playedSide(played)))
        .filter(ref => ref.from == playedFrom || ref.target == playedTo)
        .flatMap(ref => List(ref.from, ref.target))
        .map(_.trim.toLowerCase)
        .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
    squareAnchors.find(_ == playedTo)
      .orElse(Option.when(fileAnchors.contains(playedFile))(s"${playedFile}-file"))
      .orElse(routeAnchors.headOption)
      .orElse(moveRefAnchors.headOption)

  private def practicalPositionEvidenceRefs(
      idea: StrategyIdeaSignal,
      anchor: String
  ): List[String] =
    (
      List(
        s"strategic_idea_id:${idea.ideaId}",
        s"strategic_idea_kind:${idea.kind}",
        s"strategic_readiness:${idea.readiness}",
        s"anchor:$anchor"
      ) ++
        idea.evidenceRefs.map(ref => s"strategy_ref:$ref") ++
        idea.focusSquares.take(4).map(square => s"focus_square:$square") ++
        idea.focusFiles.take(4).map(file => s"focus_file:$file")
    ).map(_.trim).filter(_.nonEmpty).distinct

  private def practicalPositionPriority(fact: PracticalPositionFact): Int =
    fact.family match
      case LocalFactFamily.Defense     => 0
      case LocalFactFamily.Pressure    => 1
      case LocalFactFamily.PlanSupport => 2
      case _                           => 3

  private def playedSide(played: PlayedMove): String =
    if played.isWhite then "white" else "black"

  private def normalizeSide(raw: String): Option[String] =
    Option(raw).map(_.trim.toLowerCase).filter(side => side == "white" || side == "black")

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

  private def supportedLocalPositionProbe(delta: PlayerFacingMoveDeltaEvidence): Boolean =
    val packet = delta.packet
    packet.scope == PlayerFacingPacketScope.PositionLocal &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.releaseRisks.isEmpty &&
      packet.suppressionReasons.isEmpty &&
      ProofContractRules.supportedLocalAdmissible(packet) &&
      PlayerFacingClaimProof.allowsWeakMainClaim(packet) &&
      packet.proofPathWitness.exactSliceProof.exists(proof =>
        PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof)
      )

  private def positionProbeSubject(delta: PlayerFacingMoveDeltaEvidence): Option[String] =
    delta.packet.proofPathWitness.exactSliceProof
      .flatMap(PlayerFacingExactSliceProofFacts.targetSquare)
      .orElse(
        (delta.packet.anchorTerms ++ delta.anchorTerms ++ delta.packet.proofPathWitness.ownerSeedTerms)
          .map(_.trim.toLowerCase)
          .find(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
      )

  private def positionProbeAnchors(delta: PlayerFacingMoveDeltaEvidence): List[LocalFactAnchor] =
    val packet = delta.packet
    (
      positionProbeSubject(delta).map(subject => LocalFactAnchor("target", subject)).toList ++
        packet.proofPathWitness.ownerSeedTerms
          .map(_.trim)
          .filter(_.nonEmpty)
          .take(4)
          .map(term => LocalFactAnchor("owner_seed", term))
    ).distinct

  private def positionProbeEvidenceRefs(delta: PlayerFacingMoveDeltaEvidence): List[String] =
    val packet = delta.packet
    (
      List(
        s"proof_family:${packet.proofFamily}",
        s"proof_source:${packet.proofSource}",
        "packet_scope:position_local"
      ) ++
        positionProbeSubject(delta).map(target => s"target:$target")
    ).distinct

  private def strictStrategicFallbackCandidate(
      delta: PlayerFacingMoveDeltaEvidence,
      evidence: MoveReviewEvidence
  ): Boolean =
    evidence.strictLocalFacts &&
      !evidence.phase.trim.equalsIgnoreCase("endgame") &&
      delta.packet.releaseRisks.isEmpty &&
      delta.packet.suppressionReasons.isEmpty &&
      (
        (delta.packet.proofFamily == CentralBreakTimingWitness.ProofFamily &&
          delta.packet.proofSource == CentralBreakTimingWitness.ProofSource) ||
          MoveReviewSupportedLocalSurfaceRows.moveLocalExactSliceRow(delta.packet).nonEmpty
      )

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
    if centralBreakTimingDelta(delta) then s"${played.san} has checked central-break timing"
    else strategicIntent(delta) match
      case "prevents_counterplay" => s"${played.san} keeps counterplay restrained"
      case "clarifies_exchange"   => s"${played.san} clarifies the exchange"
      case "advances_plan"        => s"${played.san} has local plan support"
      case "answers_threat"       => s"${played.san} answers the local resource"
      case "creates_threat"       => s"${played.san} increases local pressure"
      case _                      => s"${played.san} improves the local setup"

  private def strategicPurpose(played: PlayedMove, delta: PlayerFacingMoveDeltaEvidence): String =
    if centralBreakTimingDelta(delta) then s"${played.san} is tied to a checked central-break timing detail."
    else
      val anchor = preferredStrategicSubject(delta).map(subject => s" around $subject").getOrElse("")
      strategicIntent(delta) match
        case "prevents_counterplay" => s"${played.san} keeps counterplay restrained$anchor."
        case "clarifies_exchange"   => s"${played.san} clarifies the exchange$anchor."
        case "advances_plan"        => s"${played.san} is tied to local plan support$anchor."
        case "answers_threat"       => s"${played.san} limits the defensive resource$anchor."
        case "creates_threat"       => s"${played.san} increases local pressure$anchor."
        case _                      => s"${played.san} improves the local setup$anchor."

  private def centralBreakTimingDelta(delta: PlayerFacingMoveDeltaEvidence): Boolean =
    delta.packet.proofFamily == CentralBreakTimingWitness.ProofFamily &&
      delta.packet.proofSource == CentralBreakTimingWitness.ProofSource

  private def preferredStrategicSubject(delta: PlayerFacingMoveDeltaEvidence): Option[String] =
    MoveReviewSupportedLocalSurfaceRows
      .moveLocalExactSliceRow(delta.packet)
      .flatMap(_.authority.flatMap(_.target))
      .orElse(
        (delta.packet.anchorTerms ++ delta.anchorTerms ++ delta.packet.proofPathWitness.ownerSeedTerms ++ delta.packet.proofPathWitness.continuationTerms)
          .map(_.trim)
          .find(subject => subject.nonEmpty && !genericStrategicSubject(subject))
      )

  private def genericStrategicSubject(subject: String): Boolean =
    val normalized = subject.trim.toLowerCase
    normalized == "plan" ||
      normalized == "main plan" ||
      normalized == "the plan" ||
      normalized == "the main plan" ||
      normalized == "plan activation lane" ||
      normalized == "the plan activation lane" ||
      normalized == "plan_activation_lane"

  private def ownedEndgameFacts(
      played: PlayedMove,
      evidence: MoveReviewEvidence
  ): List[Fact] =
    evidence.endgameFacts.filter {
      case fact: Fact.KingActivity =>
        played.role == King && fact.square == played.to
      case _: Fact.Opposition =>
        played.role == King
      case _: Fact.RuleOfSquare =>
        played.role == King || played.role == chess.Pawn
      case _: Fact.TriangulationOpportunity =>
        played.role == King
      case _: Fact.RookEndgamePattern =>
        played.role == chess.Rook
      case fact: Fact.PawnPromotion =>
        played.role == chess.Pawn && fact.square == played.to
      case _: Fact.Zugzwang =>
        played.role == King
      case _ => false
    }

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
        case "restrict_piece_mobility"  => values += "mobility_restriction"
        case "move_order_timing"        => values += "move_order"
        case "clarify_exchange"         => values += "exchange_clarified"
        case "force_sequence"           => values += "forcing_sequence"
        case "improve_endgame_activity" => values += "endgame_activity"
        case "create_tactical_threat"   => values += "tactical_threat"
        case _                          =>
      }
      values.toList

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

  private def legalCaptureToSquare(
      beforeFen: String,
      move: Option[MoveReviewMoveRef],
      targetSquare: String
  ): Boolean =
    move.exists(ref =>
      legalOnePlyStep(beforeFen, ref)
        .exists(step => step.move.captures && step.move.dest.key == targetSquare)
    )

  private def normalizeMotifKey(value: String): String =
    Option(value).getOrElse("").trim.toLowerCase.replaceAll("""[^a-z0-9]+""", "_")

  private def slug(value: String): String =
    normalizeMotifKey(value).stripPrefix("_").stripSuffix("_")

  private def openingGoalPhrase(goalName: String): String =
    val key = slug(goalName)
    if key.contains("development") || key.contains("fianchetto") then "a board-backed development goal"
    else if key.contains("challenge") || key.contains("sicilian") then "a board-backed center goal"
    else if key.contains("break") || key.contains("gambit") then "a board-backed pawn-break goal"
    else if key.contains("tension") || key.contains("release") then "a board-backed tension goal"
    else if key.contains("safety") || key.contains("castle") then "a board-backed king-safety goal"
    else "a board-backed opening goal"

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
