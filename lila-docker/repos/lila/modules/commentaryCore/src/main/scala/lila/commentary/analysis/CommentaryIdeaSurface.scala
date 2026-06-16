package lila.commentary.analysis

import chess.{ Bishop, Bitboard, Color, King, Knight, Pawn, Piece, Role, Square }
import chess.format.Fen
import chess.variant.Standard
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

  final case class ForkEntryDefenseFact(
      entrySquare: Square,
      attackerSquare: Square,
      targets: List[(Square, Role)],
      defenderSquare: Square
  )

  // MoveReview basic-lane projection input, not a cross-surface carrier.
  final case class MoveReviewEvidence(
      facts: List[Fact],
      motifs: List[Motif],
      openingGoal: Option[OpeningGoals.Evaluation],
      openingName: Option[String],
      lineConsequence: Option[LineConsequenceEvidence] = None,
      postMoveTargetFacts: List[Fact] = Nil,
      postMoveDefendedTargetFacts: List[Fact] = Nil,
      forkEntryDefenseFacts: List[ForkEntryDefenseFact] = Nil,
      relationWitnesses: List[MoveReviewExchangeAnalyzer.RelationWitness] = Nil,
      strategicDeltas: List[PlayerFacingMoveDeltaEvidence] = Nil,
      phase: String = "",
      ply: Int = 0,
      strictLocalFacts: Boolean = false,
      forcedLineTheme: Option[ForcedLineTruth.VerifiedTheme] = None,
      practicalPositionFacts: List[PracticalPositionFact] = Nil,
      centralPractical: Option[CentralBreakTimingWitness.PracticalMove] = None
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
      source: LocalFactSource,
      producer: LocalFactProducer,
      subject: LocalFactSubject,
      ideaKind: String,
      label: String,
      text: String,
      anchors: List[LocalFactAnchor],
      evidenceRefs: List[String],
      guardrails: List[String],
      strictFallbackCandidate: Boolean = false
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
      MoveReviewIdeaRule("fork_entry_defense", forkEntryDefenseDescriptor),
      MoveReviewIdeaRule("practical_position_support", practicalPositionSupportDescriptor),
      MoveReviewIdeaRule("practical_central_challenge", practicalCentralChallengeDescriptor),
      MoveReviewIdeaRule("target_defense", targetDefenseDescriptor)
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
      val surface = openingGoalSurface(played, evidence.openingName, goal)
      descriptor(
        ideaKind = "opening_goal",
        reviewIntent = "normal_development",
        moveCharacterBand = characterBand,
        source = "opening_goal",
        title = surface.title,
        baseProse = surface.prose,
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
      val surface = castlingSafetySurface(played)
      val reasonTags = List("king_safety")
      descriptor(
        ideaKind = "king_safety",
        reviewIntent = "king_safety",
        moveCharacterBand = characterBand,
        source = "basic_move_explanation",
        title = s"${played.san} improves king safety",
        baseProse = castlingSafetyProse(surface),
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
          anchors = castlingSafetyAnchors(surface),
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs = castlingSafetyEvidenceRefs(surface),
          guardrails = List("castle_move", "pv_coupled") ++ castlingSafetyEvidenceRefs(surface)
        )),
        factFragments = List(FactFragment.KingSafetyFragment(played.san))
      )
    }

  private final case class CastlingSafetySurface(
      side: String,
      kingSquare: String,
      rookSquare: String,
      shieldPawns: Option[Int],
      exposedFiles: Option[Int],
      ringAttacked: Option[Int]
  )

  private def castlingSafetySurface(played: PlayedMove): CastlingSafetySurface =
    val short = played.san.startsWith("O-O") && !played.san.startsWith("O-O-O") || played.to.file == chess.File.G
    val side = if short then "short" else "long"
    val rank = if played.isWhite then "1" else "8"
    val rookSquare = if short then s"f$rank" else s"d$rank"
    val features = PositionAnalyzer.extractFeatures(played.afterFen, plyCount = 0).map(_.kingSafety)
    CastlingSafetySurface(
      side = side,
      kingSquare = played.to.key,
      rookSquare = rookSquare,
      shieldPawns = features.map(kingSafetyFor(played, _, _.whiteKingShield, _.blackKingShield)),
      exposedFiles = features.map(kingSafetyFor(played, _, _.whiteKingExposedFiles, _.blackKingExposedFiles)),
      ringAttacked = features.map(kingSafetyFor(played, _, _.whiteKingRingAttacked, _.blackKingRingAttacked))
    )

  private def kingSafetyFor(
      played: PlayedMove,
      features: KingSafetyFeatures,
      whiteValue: KingSafetyFeatures => Int,
      blackValue: KingSafetyFeatures => Int
  ): Int =
    if played.isWhite then whiteValue(features) else blackValue(features)

  private def castlingSafetyProse(surface: CastlingSafetySurface): String =
    val shieldText =
      surface.shieldPawns
        .filter(_ >= 2)
        .map(count => s" behind a ${numberWord(count)}-pawn shield")
        .getOrElse("")
    s"Castling ${surface.side} puts the king on ${surface.kingSquare}$shieldText and brings the rook to ${surface.rookSquare}."

  private def numberWord(count: Int): String =
    count match
      case 0 => "zero"
      case 1 => "one"
      case 2 => "two"
      case 3 => "three"
      case n => n.toString

  private def castlingSafetyAnchors(surface: CastlingSafetySurface): List[LocalFactAnchor] =
    List(
      LocalFactAnchor("castle_side", surface.side),
      LocalFactAnchor("king_square", surface.kingSquare),
      LocalFactAnchor("rook_square", surface.rookSquare)
    )

  private def castlingSafetyEvidenceRefs(surface: CastlingSafetySurface): List[String] =
    (
      List(
        s"castle_side:${surface.side}",
        s"king_square:${surface.kingSquare}",
        s"rook_square:${surface.rookSquare}"
      ) ++
        surface.shieldPawns.map(value => s"king_shield_pawns:$value") ++
        surface.exposedFiles.map(value => s"king_exposed_files:$value") ++
        surface.ringAttacked.map(value => s"king_ring_attacked:$value")
    ).distinct

  private def positionProbeSupportDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      line <- lineFacts
      delta <- evidence.strategicDeltas.find(delta =>
        supportedLocalPositionProbe(delta) &&
          positionProbeOwnedByPlayedMove(played, delta, line)
      )
      row <- MoveReviewSupportedLocalSurfaceRows.positionProbeExactSliceRow(
        delta.packet,
        lineFactsValidatedColorComplex = colorComplexLineFactsValidated(played, delta, line)
      )
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
      fact <- practicalPositionFactForMode(evidence)
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
          source = fact.source,
          producer = fact.producer,
          subject = fact.subject,
          strictFallbackCandidate = fact.strictFallbackCandidate,
          anchors = fact.anchors,
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs = fact.evidenceRefs,
          guardrails = fact.guardrails
        )),
        factFragments = Nil
      )

  private def practicalPositionFactForMode(evidence: MoveReviewEvidence): Option[PracticalPositionFact] =
    if evidence.strictLocalFacts then evidence.practicalPositionFacts.find(_.strictFallbackCandidate)
    else evidence.practicalPositionFacts.headOption

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
        lineProof = lineProof("certified_strategy", played, line, subjectOverride = preferredStrategicSubject(played, delta).orElse(Some(played.uci))),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(
          MoveReviewLocalFact.strategicMoveDeltaCandidate(
            delta,
            anchors =
              preferredStrategicSubject(played, delta).map(subject => LocalFactAnchor("preferred_subject", subject)).toList ++
                strategicExactProofAnchors(played, delta),
            guardrails = List(
              s"proof_family:${delta.packet.proofFamily}",
              s"proof_source:${delta.packet.proofSource}",
              "strict_requires_causal_or_exact_fallback"
            ),
            evidenceRefs = strategicExactProofEvidenceRefs(played, delta),
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
    ownedTacticalEvidence(played, evidence, lineFacts)
      .filterNot(owned => owned.kind == "check" && checkPreemptedByStrongerLocalEvidence(played, evidence))
      .map { owned =>
        val kind = owned.kind
        val label = kind.replace("_", " ")
        val title =
          kind match
            case "check"         => s"${played.san} gives check"
            case "trapped_piece" => s"${played.san} traps a piece"
            case _               => s"${played.san} creates a $label"
        val prose = tacticalProse(played, owned)
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
            anchors = tacticalAnchors(owned),
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

  private def tacticalProse(
      played: PlayedMove,
      owned: OwnedTacticalEvidence
  ): String =
    owned.fact match
      case Some(pin: Fact.Pin) if owned.kind == "pin" =>
        s"${played.san} pins ${tacticalPieceLabel(pin.pinned, pin.pinnedRole)} to ${tacticalPieceLabel(pin.behind, pin.behindRole)}."
      case Some(skewer: Fact.Skewer) if owned.kind == "skewer" =>
        s"${played.san} skewers ${tacticalPieceLabel(skewer.front, skewer.frontRole)} toward ${tacticalPieceLabel(skewer.back, skewer.backRole)}."
      case Some(fork: Fact.Fork) if owned.kind == "fork" =>
        s"${played.san} forks ${joinTacticalLabels(fork.targets.map { case (square, role) => tacticalPieceLabel(square, role) })}."
      case _ =>
        owned.motif match
          case check: Motif.Check if owned.kind == "check" =>
            s"${played.san} gives check to ${tacticalPieceLabel(check.targetSquare, King)}, tied to the verified reply in the checked line."
          case pin: Motif.Pin if owned.kind == "pin" =>
            (for
              pinned <- pin.pinnedSq.map(square => tacticalPieceLabel(square, pin.pinnedPiece))
              behind <- pin.behindSq.map(square => tacticalPieceLabel(square, pin.targetBehind))
            yield s"${played.san} pins $pinned to $behind.")
              .getOrElse(defaultTacticalProse(played, owned.kind))
          case skewer: Motif.Skewer if owned.kind == "skewer" =>
            (for
              front <- skewer.frontSq.map(square => tacticalPieceLabel(square, skewer.frontPiece))
              back <- skewer.backSq.map(square => tacticalPieceLabel(square, skewer.backPiece))
            yield s"${played.san} skewers $front toward $back.")
              .getOrElse(defaultTacticalProse(played, owned.kind))
          case fork: Motif.Fork if owned.kind == "fork" && fork.targetSquares.nonEmpty =>
            val targets = fork.targetSquares.zip(fork.targets).map { case (square, role) => tacticalPieceLabel(square, role) }
            s"${played.san} forks ${joinTacticalLabels(targets)}."
          case discovered: Motif.DiscoveredAttack if owned.kind == "discovered_attack" =>
            (for
              attacker <- discovered.attackingSq.map(square => tacticalPieceLabel(square, discovered.attackingPiece))
              target <- discovered.targetSq.map(square => tacticalPieceLabel(square, discovered.target))
            yield s"${played.san} opens a discovered attack from $attacker toward $target.")
              .getOrElse(defaultTacticalProse(played, owned.kind))
          case trapped: Motif.TrappedPiece if owned.kind == "trapped_piece" =>
            s"${played.san} traps ${tacticalPieceLabel(trapped.trappedSquare, trapped.trappedRole)} in the verified position."
          case _ =>
            defaultTacticalProse(played, owned.kind)

  private def defaultTacticalProse(played: PlayedMove, kind: String): String =
    kind match
      case "check" =>
        s"${played.san} gives check, tied to the verified reply in the checked line."
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

  private def tacticalPieceLabel(square: Square, role: Role): String =
    s"the ${square.key} ${roleLabel(role)}"

  private def joinTacticalLabels(labels: List[String]): String =
    labels.distinct match
      case Nil                  => "the verified tactical targets"
      case one :: Nil           => one
      case first :: second :: Nil => s"$first and $second"
      case many                 => s"${many.dropRight(1).mkString(", ")}, and ${many.last}"

  private def checkPreemptedByStrongerLocalEvidence(
      played: PlayedMove,
      evidence: MoveReviewEvidence
  ): Boolean =
    evidence.forcedLineTheme.nonEmpty ||
      evidence.lineConsequence.exists(validLineConsequence(played, evidence, _)) ||
      evidence.relationWitnesses.exists(relationWitnessEvidence(played, evidence, _).nonEmpty) ||
      ownedTargetPressureFact(played, evidence).nonEmpty ||
      ownedTargetDefenseFact(played, evidence).nonEmpty

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
                  lineConfirmsTactic("fork", fact, line, played) =>
              OwnedTacticalEvidence("fork", Some(fact), immediateMotifs.find(motifOwnsFork(played, fact, _)).get, fact.participants)
          }
        val pin =
          evidence.facts.collectFirst {
            case fact: Fact.Pin
                if fact.attacker == played.to &&
                  immediateMotifs.exists(motif => motifOwnsPin(played, fact, motif)) &&
                  lineConfirmsTactic("pin", fact, line, played) =>
              OwnedTacticalEvidence("pin", Some(fact), immediateMotifs.find(motifOwnsPin(played, fact, _)).get, fact.participants)
          }
        val skewer =
          evidence.facts.collectFirst {
            case fact: Fact.Skewer
                if fact.attacker == played.to &&
                  immediateMotifs.exists(motif => motifOwnsSkewer(played, fact, motif)) &&
                  lineConfirmsTactic("skewer", fact, line, played) =>
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
            !matchingFactMotifs.exists { case (kind, fact, _) => lineConfirmsTactic(kind, fact, line, played) }
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
      case motif: Motif.Check if basicCheckMotif(motif) => motif
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
      case motif: Motif.Check            => List(motif.targetSquare)
      case _                             => Nil

  private def motifOnlyTacticalEvidence(
      played: PlayedMove,
      line: MoveReviewPvLine.LineFacts
  )(motif: Motif): Option[OwnedTacticalEvidence] =
    motif match
      case m: Motif.Fork if m.square == played.to =>
        motifForkFact(m)
          .filter(lineConfirmsFork(played, _, line))
          .map(_ => OwnedTacticalEvidence("fork", None, m, (m.square :: m.targetSquares).distinct))
      case m: Motif.Pin
          if m.pinningSq.contains(played.to) &&
            m.pinnedSq.nonEmpty &&
            m.behindSq.nonEmpty &&
            continuationTouchesAny(line, List(m.pinnedSq, m.behindSq).flatten) =>
        Some(OwnedTacticalEvidence("pin", None, m, List(m.pinningSq, m.pinnedSq, m.behindSq).flatten.distinct))
      case m: Motif.Skewer
          if m.attackingSq.contains(played.to) =>
        motifSkewerFact(m)
          .filter(lineConfirmsSkewer(played, _, line))
          .map(_ => OwnedTacticalEvidence("skewer", None, m, List(m.attackingSq, m.frontSq, m.backSq).flatten.distinct))
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
      case m: Motif.Check if !played.isCapture && basicCheckMotif(m) && line.reply.nonEmpty =>
        Some(OwnedTacticalEvidence("check", None, m, List(m.targetSquare)))
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
      case m: Motif.Check =>
        !played.isCapture &&
          basicCheckMotif(m) &&
          m.plyIndex == 0 &&
          m.color == played.color &&
          m.piece == played.role &&
          m.move.exists(sanEquivalent(_, played.san))
      case _ => false

  private def basicCheckMotif(motif: Motif.Check): Boolean =
    motif.checkType != Motif.CheckType.Mate &&
      motif.checkType != Motif.CheckType.Smothered

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

  private def motifSkewerFact(motif: Motif.Skewer): Option[Fact.Skewer] =
    for
      attacker <- motif.attackingSq
      front <- motif.frontSq
      back <- motif.backSq
    yield Fact.Skewer(
      attacker = attacker,
      attackerRole = motif.attackingPiece,
      front = front,
      frontRole = motif.frontPiece,
      back = back,
      backRole = motif.backPiece,
      scope = FactScope.CandidatePv
    )

  private def motifForkFact(motif: Motif.Fork): Option[Fact.Fork] =
    val targets = motif.targetSquares.zip(motif.targets)
    if targets.size >= 2 then
      Some(Fact.Fork(motif.square, motif.attackingPiece, targets, FactScope.CandidatePv))
    else None

  private def lineConfirmsTactic(
      kind: String,
      fact: Fact,
      line: MoveReviewPvLine.LineFacts,
      played: PlayedMove
  ): Boolean =
    kind match
      case "fork" =>
        fact match
          case fork: Fact.Fork => lineConfirmsFork(played, fork, line)
          case _               => false
      case "pin" =>
        fact match
          case pin: Fact.Pin => continuationTouchesAny(line, List(pin.pinned, pin.behind))
          case _             => false
      case "skewer" =>
        fact match
          case skewer: Fact.Skewer => lineConfirmsSkewer(played, skewer, line)
          case _                   => false
      case _ => false

  private def lineConfirmsFork(
      played: PlayedMove,
      fork: Fact.Fork,
      line: MoveReviewPvLine.LineFacts
  ): Boolean =
    line.reply.nonEmpty &&
      !directCaptureOutweighsForkTargets(played, fork) &&
      Fen.read(Standard, Fen.Full(line.first.fenAfter)).exists { position =>
        val board = position.board
        forkGeometryPresent(board, played.color, fork) &&
          (forkTargetsAreMateriallyMeaningful(fork.targets) || forkTargetCapturedByAttackerAfterReply(line, fork))
      }

  private def directCaptureOutweighsForkTargets(played: PlayedMove, fork: Fact.Fork): Boolean =
    played.capturedRole.exists { captured =>
      val maxTargetValue = fork.targets.collect { case (_, role) if role != King => tacticalRoleValue(role) }.maxOption.getOrElse(0)
      maxTargetValue > 0 && tacticalRoleValue(captured) >= maxTargetValue
    }

  private def forkGeometryPresent(board: chess.Board, color: Color, fork: Fact.Fork): Boolean =
    val occupied = board.occupied
    board.roleAt(fork.attacker).contains(fork.attackerRole) &&
      board.colorAt(fork.attacker).contains(color) &&
      fork.targets.forall { case (target, role) =>
        board.roleAt(target).contains(role) &&
          board.colorAt(target).contains(!color) &&
          MoveReviewExchangeAnalyzer
            .roleAttacks(fork.attackerRole, fork.attacker, color, occupied)
            .contains(target)
      }

  private def forkTargetsAreMateriallyMeaningful(targets: List[(Square, Role)]): Boolean =
    targets.map(_._1).distinct.size >= 2 &&
      targets.exists { case (_, role) => role != King && role != Pawn }

  private def forkTargetCapturedByAttackerAfterReply(
      line: MoveReviewPvLine.LineFacts,
      fork: Fact.Fork
  ): Boolean =
    val capturableTargets = fork.targets.filter { case (_, role) => role != King }
    capturableTargets.nonEmpty &&
      line.reply.exists { reply =>
        MoveReviewExchangeAnalyzer
          .boundedReplay(reply.fenAfter, line.checkedContinuations.map(_.uci), maxPlies = line.checkedContinuations.size.min(4))
          .exists(_.exists { step =>
            step.move.orig == fork.attacker &&
              capturableTargets.exists { case (target, role) =>
                step.move.dest == target && step.capturedRole.contains(role)
              }
          })
      }

  private def lineConfirmsSkewer(
      played: PlayedMove,
      skewer: Fact.Skewer,
      line: MoveReviewPvLine.LineFacts
  ): Boolean =
    skewer.backRole != Pawn &&
      !directCaptureOutweighsSkewerBack(played, skewer) &&
      Fen.read(Standard, Fen.Full(line.first.fenAfter)).exists { position =>
        val board = position.board
        val piecesStillMatch =
          board.roleAt(skewer.attacker).contains(skewer.attackerRole) &&
            board.colorAt(skewer.attacker).contains(played.color) &&
            board.roleAt(skewer.front).contains(skewer.frontRole) &&
            board.colorAt(skewer.front).contains(!played.color) &&
            board.roleAt(skewer.back).contains(skewer.backRole) &&
            board.colorAt(skewer.back).contains(!played.color)
        piecesStillMatch &&
          skewerGeometryPresent(board, played.color, skewer) &&
          line.reply.exists(replyMovesSkeweredFront(_, skewer)) &&
          skewerBackCapturedByAttackerAfterReply(line, skewer)
      }

  private def directCaptureOutweighsSkewerBack(played: PlayedMove, skewer: Fact.Skewer): Boolean =
    played.capturedRole.exists(captured => tacticalRoleValue(captured) >= tacticalRoleValue(skewer.backRole))

  private def skewerGeometryPresent(board: chess.Board, color: Color, skewer: Fact.Skewer): Boolean =
    val occupied = board.occupied
    val betweenBack = Bitboard.between(skewer.attacker, skewer.back)
    val blockersBeyondFront = betweenBack & occupied & ~Bitboard(skewer.front)
    MoveReviewExchangeAnalyzer
      .roleAttacks(skewer.attackerRole, skewer.attacker, color, occupied)
      .contains(skewer.front) &&
      betweenBack.contains(skewer.front) &&
      !blockersBeyondFront.nonEmpty

  private def replyMovesSkeweredFront(reply: MoveReviewMoveRef, skewer: Fact.Skewer): Boolean =
    val uci = MoveReviewPvLine.normalizeUci(reply.uci)
    uci.take(2) == skewer.front.key && uci.slice(2, 4) != skewer.attacker.key

  private def skewerBackCapturedByAttackerAfterReply(
      line: MoveReviewPvLine.LineFacts,
      skewer: Fact.Skewer
  ): Boolean =
    line.reply.exists { reply =>
      MoveReviewExchangeAnalyzer
        .boundedReplay(reply.fenAfter, line.checkedContinuations.map(_.uci), maxPlies = line.checkedContinuations.size.min(4))
        .exists(_.exists { step =>
          step.move.orig == skewer.attacker &&
            step.move.dest == skewer.back &&
            step.capturedRole.contains(skewer.backRole)
        })
    }

  private def tacticalRoleValue(role: Role): Int =
    role match
      case King         => 100
      case chess.Queen  => 9
      case chess.Rook   => 5
      case Bishop | Knight => 3
      case Pawn         => 1

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
        owned.targetSquares.distinct.map(square => s"motif_square:${square.key}") ++
        (owned.motif match
          case check: Motif.Check => List(s"check_type:${tokenKey(check.checkType)}")
          case _                  => Nil
        )
    ).distinct

  private def tacticalAnchors(owned: OwnedTacticalEvidence): List[LocalFactAnchor] =
    (
      List(LocalFactAnchor("tactical_kind", owned.kind)) ++
        owned.targetSquares.distinct.map(square => LocalFactAnchor("tactical_square", square.key)) ++
        owned.fact.toList.flatMap(tacticalFactAnchors) ++
        tacticalMotifAnchors(owned.motif)
    ).distinct

  private def tacticalFactAnchors(fact: Fact): List[LocalFactAnchor] =
    fact match
      case pin: Fact.Pin =>
        List(
          LocalFactAnchor("attacker_square", pin.attacker.key),
          LocalFactAnchor("attacker_role", roleKey(pin.attackerRole)),
          LocalFactAnchor("pinned_square", pin.pinned.key),
          LocalFactAnchor("pinned_role", roleKey(pin.pinnedRole)),
          LocalFactAnchor("behind_square", pin.behind.key),
          LocalFactAnchor("behind_role", roleKey(pin.behindRole))
        )
      case skewer: Fact.Skewer =>
        List(
          LocalFactAnchor("attacker_square", skewer.attacker.key),
          LocalFactAnchor("attacker_role", roleKey(skewer.attackerRole)),
          LocalFactAnchor("front_square", skewer.front.key),
          LocalFactAnchor("front_role", roleKey(skewer.frontRole)),
          LocalFactAnchor("back_square", skewer.back.key),
          LocalFactAnchor("back_role", roleKey(skewer.backRole))
        )
      case fork: Fact.Fork =>
        List(
          LocalFactAnchor("attacker_square", fork.attacker.key),
          LocalFactAnchor("attacker_role", roleKey(fork.attackerRole))
        ) ++ tacticalTargetAnchors(fork.targets)
      case _ => Nil

  private def tacticalMotifAnchors(motif: Motif): List[LocalFactAnchor] =
    motif match
      case pin: Motif.Pin =>
        List(
          pin.pinningSq.map(square => LocalFactAnchor("attacker_square", square.key)),
          Some(LocalFactAnchor("attacker_role", roleKey(pin.pinningPiece))),
          pin.pinnedSq.map(square => LocalFactAnchor("pinned_square", square.key)),
          Some(LocalFactAnchor("pinned_role", roleKey(pin.pinnedPiece))),
          pin.behindSq.map(square => LocalFactAnchor("behind_square", square.key)),
          Some(LocalFactAnchor("behind_role", roleKey(pin.targetBehind)))
        ).flatten
      case skewer: Motif.Skewer =>
        List(
          skewer.attackingSq.map(square => LocalFactAnchor("attacker_square", square.key)),
          Some(LocalFactAnchor("attacker_role", roleKey(skewer.attackingPiece))),
          skewer.frontSq.map(square => LocalFactAnchor("front_square", square.key)),
          Some(LocalFactAnchor("front_role", roleKey(skewer.frontPiece))),
          skewer.backSq.map(square => LocalFactAnchor("back_square", square.key)),
          Some(LocalFactAnchor("back_role", roleKey(skewer.backPiece)))
        ).flatten
      case fork: Motif.Fork =>
        List(
          LocalFactAnchor("attacker_square", fork.square.key),
          LocalFactAnchor("attacker_role", roleKey(fork.attackingPiece))
        ) ++ tacticalTargetAnchors(fork.targetSquares.zip(fork.targets))
      case discovered: Motif.DiscoveredAttack =>
        List(
          discovered.movingSq.map(square => LocalFactAnchor("moving_square", square.key)),
          Some(LocalFactAnchor("moving_role", roleKey(discovered.movingPiece))),
          discovered.attackingSq.map(square => LocalFactAnchor("revealed_square", square.key)),
          Some(LocalFactAnchor("revealed_role", roleKey(discovered.attackingPiece))),
          discovered.targetSq.map(square => LocalFactAnchor("target_square", square.key)),
          Some(LocalFactAnchor("target_role", roleKey(discovered.target)))
        ).flatten
      case trapped: Motif.TrappedPiece =>
        List(
          LocalFactAnchor("trapped_square", trapped.trappedSquare.key),
          LocalFactAnchor("trapped_role", roleKey(trapped.trappedRole))
        )
      case check: Motif.Check =>
        List(
          LocalFactAnchor("king_square", check.targetSquare.key),
          LocalFactAnchor("king_role", "king"),
          LocalFactAnchor("check_type", tokenKey(check.checkType))
        )
      case _ => Nil

  private def tacticalTargetAnchors(targets: List[(Square, Role)]): List[LocalFactAnchor] =
    targets.zipWithIndex.flatMap { case ((square, role), index) =>
      val ordinal = index + 1
      List(
        LocalFactAnchor(s"target_${ordinal}_square", square.key),
        LocalFactAnchor(s"target_${ordinal}_role", roleKey(role)),
        LocalFactAnchor("target_square", square.key),
        LocalFactAnchor("target_role", roleKey(role))
      )
    }

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
    val verifiedTarget = pressureFact.flatMap(owned => verifiedPressureTarget(played, owned))
    val defensive = defensiveTruth(truthContract) && threatLineFacts.nonEmpty
    val unverifiedCapturePressure = played.isCapture && verifiedTarget.isEmpty
    val offensive =
      !unverifiedCapturePressure &&
        pressureFact.exists(owned => !owned.postMoveStatic || postMovePressureAllowed(played, evidence, owned.fact))
    val relationPreemptsTarget = offensive && relationPreemptsTargetPressure(played, evidence, pressureFact)
    Option.when(
      lineFacts.flatMap(_.reply).nonEmpty &&
        (defensive || (offensive && !relationPreemptsTarget))
    ) {
      val targetAttack =
        verifiedTarget.map { case (target, role) =>
          s"${played.san} attacks the ${roleLabel(role)} on ${target.key}."
        }
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
          else targetAttack.getOrElse(s"${played.san} is tied to a concrete local target."),
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
              (
                verifiedTarget.toList.flatMap { case (target, role) =>
                  List(
                    LocalFactAnchor("target_square", target.key),
                    LocalFactAnchor("target_role", roleKey(role)),
                    LocalFactAnchor("attacker_square", played.to.key),
                    LocalFactAnchor("attacker_role", roleKey(played.role))
                  )
                } ++
                  pressureFact.toList.flatMap(_.fact.participants).distinct.map(square => LocalFactAnchor("target_fact_square", square.key))
              ).distinct,
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

  private def targetDefenseDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      line <- lineFacts.filter(_.reply.nonEmpty)
      fact <- ownedTargetDefenseFact(played, evidence)
      target <- targetFactSquare(fact)
      role <- targetFactRole(fact)
    yield
      descriptor(
        ideaKind = "target_defense",
        reviewIntent = "answers_threat",
        moveCharacterBand = characterBand,
        source = "canonical_fact",
        title = s"${played.san} adds a local defender",
        baseProse = s"${played.san} adds a defender to the ${roleLabel(role)} on ${target.key}.",
        reasonTags = List("target_defense", "post_move_static_target_defense"),
        linePurpose = Some("answer_direct_threat"),
        lineProof = lineProof("target_defense", played, line, subjectOverride = Some(target.key)),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(LocalFactCandidate(
          family = LocalFactFamily.Defense,
          source = LocalFactSource.CanonicalFact,
          producer = LocalFactProducer.TargetDefense,
          subject = LocalFactSubject.Target,
          strictFallbackCandidate = true,
          anchors =
            (LocalFactAnchor("defended_target", target.key) ::
              fact.participants.distinct.map(square => LocalFactAnchor("target_fact_square", square.key))).distinct,
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs =
            (
              factEvidenceRefs(fact) ++
                List(
                  "fact_source:post_move_static_defense",
                  s"defended_target:${target.key}",
                  s"defender_square:${played.to.key}"
                )
            ).distinct,
          guardrails = List(
            "target_fact_defended_by_played_move",
            "post_move_static_target_defense",
            "pv_coupled"
          )
        )),
        factFragments = List(FactFragment.DirectThreatFragment(
          san = played.san,
          isDefensive = true,
          reason = "answer_direct_threat"
        ))
      )

  private def forkEntryDefenseDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      line <- lineFacts.filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci && line.reply.nonEmpty)
      if truthContract.forall(contract => !contract.blocksStrategicSupport)
      fact <- evidence.forkEntryDefenseFacts.headOption
    yield
      val targetSummary = targetText(fact.targets)
      descriptor(
        ideaKind = "fork_entry_defense",
        reviewIntent = "answers_threat",
        moveCharacterBand = characterBand,
        source = "canonical_fact",
        title = s"${played.san} covers a fork entry",
        baseProse =
          s"${played.san} covers ${fact.entrySquare.key} against the knight fork from ${fact.attackerSquare.key}, where the knight would hit $targetSummary.",
        reasonTags = List("fork_entry_defense", "post_move_static_fork_entry_defense"),
        linePurpose = Some("answer_direct_threat"),
        lineProof = lineProof("fork_entry_defense", played, line, subjectOverride = Some(fact.entrySquare.key)),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(LocalFactCandidate(
          family = LocalFactFamily.Defense,
          source = LocalFactSource.CanonicalFact,
          producer = LocalFactProducer.ForkEntryDefense,
          subject = LocalFactSubject.Target,
          strictFallbackCandidate = true,
          anchors =
            (
              List(
                LocalFactAnchor("fork_entry", fact.entrySquare.key),
                LocalFactAnchor("fork_attacker", fact.attackerSquare.key),
                LocalFactAnchor("defender_square", fact.defenderSquare.key)
              ) ++
                fact.targets.map { case (square, role) => LocalFactAnchor(s"fork_target_${roleLabel(role)}", square.key) }
            ).distinct,
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs =
            (
              List(
                "fact_source:post_move_fork_entry_defense",
                s"fork_entry_square:${fact.entrySquare.key}",
                s"fork_attacker:${fact.attackerSquare.key}",
                s"defender_square:${fact.defenderSquare.key}"
              ) ++
                fact.targets.map { case (square, role) => s"fork_target:${square.key}:${roleLabel(role)}" }
            ).distinct,
          guardrails = List(
            "fork_entry_square_defended_by_played_move",
            "post_move_static_fork_entry_defense",
            "hypothetical_knight_fork_targets_king_and_major",
            "pv_coupled"
          )
        )),
        factFragments = List(FactFragment.DirectThreatFragment(
          san = played.san,
          isDefensive = true,
          reason = "answer_direct_threat"
        ))
      )

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

  private def verifiedPressureTarget(
      played: PlayedMove,
      owned: OwnedPressureFact
  ): Option[(Square, Role)] =
    for
      square <- targetFactSquare(owned.fact)
      role <- targetFactRole(owned.fact)
      after <- Fen.read(Standard, Fen.Full(played.afterFen))
      piece <- after.board.pieceAt(square)
      if piece.color != played.color && piece.role == role
    yield square -> role

  private def ownedTargetDefenseFact(
      played: PlayedMove,
      evidence: MoveReviewEvidence
  ): Option[Fact] =
    evidence.postMoveDefendedTargetFacts.collectFirst {
      case fact: Fact.TargetPiece if fact.defenders.contains(played.to) => fact
      case fact: Fact.HangingPiece if fact.defenders.contains(played.to) => fact
    }

  private def targetFactSquare(fact: Fact): Option[Square] =
    fact match
      case Fact.TargetPiece(square, _, _, _, _)  => Some(square)
      case Fact.HangingPiece(square, _, _, _, _) => Some(square)
      case _                                     => None

  private def targetFactRole(fact: Fact): Option[Role] =
    fact match
      case Fact.TargetPiece(_, role, _, _, _)  => Some(role)
      case Fact.HangingPiece(_, role, _, _, _) => Some(role)
      case _                                   => None

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
          case fact: Fact.TargetPiece
              if fact.attackers.contains(played.to) && postMovePressureAllowed(played, evidence, fact) =>
            fact
          case fact: Fact.HangingPiece
              if fact.attackers.contains(played.to) && postMovePressureAllowed(played, evidence, fact) =>
            fact
        }.map(fact => OwnedPressureFact(fact, postMoveStatic = true))
      )

  private def postMovePressureAllowed(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      fact: Fact
  ): Boolean =
    !played.isCapture &&
      (
        (evidence.lineConsequence.isEmpty && exactKnightPawnPressure(played, fact)) ||
          (evidence.lineConsequence.isEmpty && exactPawnPiecePressure(played, fact)) ||
          (
            evidence.lineConsequence.isEmpty &&
              evidence.practicalPositionFacts.isEmpty &&
              exactBishopKnightPressure(played, fact)
          ) ||
          (
            evidence.strictLocalFacts &&
              !evidence.phase.trim.equalsIgnoreCase("endgame") &&
              meaningfulPressureTarget(fact)
          )
      )

  private def meaningfulPressureTarget(fact: Fact): Boolean =
    fact match
      case Fact.HangingPiece(_, role, _, _, _) => role != King
      case Fact.TargetPiece(_, role, _, _, _)  => role != King && role != chess.Pawn
      case _                                   => false

  private def exactKnightPawnPressure(played: PlayedMove, fact: Fact): Boolean =
    fact match
      case Fact.TargetPiece(_, chess.Pawn, attackers, defenders, _) =>
        played.role == chess.Knight &&
          attackers.contains(played.to) &&
          defenders.isEmpty
      case _ => false

  private def exactPawnPiecePressure(played: PlayedMove, fact: Fact): Boolean =
    def attacksNonPawnPiece(role: Role, attackers: List[Square]): Boolean =
      played.role == chess.Pawn &&
        role != King &&
        role != chess.Pawn &&
        attackers.contains(played.to)

    fact match
      case Fact.TargetPiece(_, role, attackers, _, _)  => attacksNonPawnPiece(role, attackers)
      case Fact.HangingPiece(_, role, attackers, _, _) => attacksNonPawnPiece(role, attackers)
      case _                                           => false

  private def exactBishopKnightPressure(played: PlayedMove, fact: Fact): Boolean =
    def attacksKnight(role: Role, attackers: List[Square]): Boolean =
      played.role == chess.Bishop &&
        role == chess.Knight &&
        attackers.contains(played.to)

    fact match
      case Fact.TargetPiece(_, role, attackers, _, _)  => attacksKnight(role, attackers)
      case Fact.HangingPiece(_, role, attackers, _, _) => attacksKnight(role, attackers)
      case _                                           => false

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
    for
      line <- lineFacts.filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci && line.reply.nonEmpty)
      if evidence.hasCaptureMotif && played.isCapture
    yield
      val immediateRecapture = isImmediateRecapture(played.toKey, lineFacts)
      val followUpQueenTrade = followUpQueenTradeDetail(line)
      val purpose =
        if followUpQueenTrade.nonEmpty ||
          (immediateRecapture && (played.capturedRole.exists(_ != chess.Pawn) || played.role != chess.Pawn))
        then "clarify_exchange"
        else "resolve_capture_tension"
      val captureText = capturedPieceText(played)
      val captureRefs = captureEvidenceRefs(played, immediateRecapture, followUpQueenTrade)
      descriptor(
        ideaKind = if purpose == "clarify_exchange" then "exchange_clarified" else "capture_tension",
        reviewIntent = if purpose == "clarify_exchange" then "clarifies_exchange" else "keeps_tension",
        moveCharacterBand = characterBand,
        source = "basic_move_explanation",
        title =
          if purpose == "clarify_exchange" then s"${played.san} clarifies the exchange"
          else s"${played.san} resolves the capture tension",
        baseProse = s"${played.san} captures $captureText.",
        reasonTags = List("capture_sequence"),
        linePurpose = Some(purpose),
        lineProof = lineProof(if purpose == "clarify_exchange" then "exchange_clarification" else "capture_tension", played, line),
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
          anchors = captureAnchors(played, immediateRecapture, followUpQueenTrade),
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs = captureRefs,
          guardrails = captureRefs
        )),
        factFragments = List(FactFragment.CaptureFragment(
          san = played.san,
          purpose = purpose
        ))
      )

  private def capturedPieceText(played: PlayedMove): String =
    played.capturedRole
      .map(role => s"the ${roleLabel(role)} on ${played.toKey}")
      .getOrElse(s"on ${played.toKey}")

  private def captureAnchors(
      played: PlayedMove,
      immediateRecapture: Boolean,
      followUpQueenTrade: Option[(String, String, String)]
  ): List[LocalFactAnchor] =
    (
      List(
        Some(LocalFactAnchor("captured_square", played.toKey)),
        played.capturedRole.map(role => LocalFactAnchor("captured_role", roleKey(role))),
        Option.when(immediateRecapture)(LocalFactAnchor("immediate_recapture", "true")),
        followUpQueenTrade.map((square, _, _) => LocalFactAnchor("followup_queen_trade_square", square)),
        followUpQueenTrade.map((_, captureSan, _) => LocalFactAnchor("followup_queen_trade_capture_san", captureSan)),
        followUpQueenTrade.map((_, _, recaptureSan) => LocalFactAnchor("followup_queen_trade_recapture_san", recaptureSan))
      ).flatten
    ).distinct

  private def captureEvidenceRefs(
      played: PlayedMove,
      immediateRecapture: Boolean,
      followUpQueenTrade: Option[(String, String, String)]
  ): List[String] =
    (
      List(
        "capture_motif",
        s"captured_square:${played.toKey}"
      ) ++
        played.capturedRole.map(role => s"captured_role:${roleKey(role)}") ++
        Option.when(immediateRecapture)("immediate_recapture") ++
        followUpQueenTrade.map((square, _, _) => s"followup_queen_trade_square:$square")
    ).distinct

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
      LineConsequenceEvaluator.playedMoveTargetPressureEvidenceReady(lineEvidence) &&
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
            LineConsequenceKind.ImmediateOpponentPawnCapture,
            LineConsequenceKind.ImmediateOpponentTargetPressure,
            LineConsequenceKind.PlayedMoveTargetPressure,
            LineConsequenceKind.DelayedPawnCapture,
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
      case LineConsequenceKind.ImmediateOpponentPawnCapture =>
        Some("show_immediate_pawn_capture")
      case LineConsequenceKind.ImmediateOpponentTargetPressure =>
        Some("show_immediate_reply_pressure")
      case LineConsequenceKind.PlayedMoveTargetPressure =>
        Some("show_played_target_pressure")
      case LineConsequenceKind.DelayedPawnCapture =>
        Some("clarify_delayed_capture")
      case LineConsequenceKind.OriginSquareClearance =>
        Some("show_origin_square_clearance")
      case LineConsequenceKind.MinorPieceReroute =>
        Some("show_minor_piece_reroute")
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
      case LineConsequenceKind.ImmediateOpponentPawnCapture =>
        "shows_immediate_pawn_capture"
      case LineConsequenceKind.ImmediateOpponentTargetPressure =>
        "shows_immediate_reply_pressure"
      case LineConsequenceKind.PlayedMoveTargetPressure =>
        "shows_played_target_pressure"
      case LineConsequenceKind.DelayedPawnCapture =>
        "clarifies_delayed_capture"
      case LineConsequenceKind.OriginSquareClearance =>
        "shows_origin_square_clearance"
      case LineConsequenceKind.MinorPieceReroute =>
        "shows_minor_piece_reroute"
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
      case LineConsequenceKind.ImmediateOpponentPawnCapture =>
        "immediate pawn capture"
      case LineConsequenceKind.ImmediateOpponentTargetPressure =>
        "immediate reply pressure"
      case LineConsequenceKind.PlayedMoveTargetPressure =>
        "target pressure"
      case LineConsequenceKind.DelayedPawnCapture =>
        "delayed pawn capture"
      case LineConsequenceKind.OriginSquareClearance =>
        "origin-square clearance"
      case LineConsequenceKind.MinorPieceReroute =>
        "minor-piece reroute"
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
    evidence.witness.details match
      case details: MoveReviewExchangeAnalyzer.RelationDetails.DiscoveredAttack =>
        s"${played.san} reveals a ${details.attackerRole} attack from ${details.attackerSquare} toward ${details.targetSquare} after clearing ${details.clearedSquare}."
      case details: MoveReviewExchangeAnalyzer.RelationDetails.Overload if details.targetSquares.nonEmpty =>
        s"${played.san} overloads the defender on ${details.defenderSquare} across ${joinSquareLabels(details.targetSquares)}."
      case _ =>
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
      relationWitnessCaptureEligible(played, witness, drawResource) &&
      (!evidence.phase.trim.equalsIgnoreCase("endgame") || drawResource) &&
      MoveReviewExchangeAnalyzer.relationDetailsValidForKind(witness) &&
      witness.lineMoves.headOption.exists(uci => MoveReviewPvLine.normalizeUci(uci) == played.uci)

  private def relationWitnessCaptureEligible(
      played: PlayedMove,
      witness: MoveReviewExchangeAnalyzer.RelationWitness,
      drawResource: Boolean
  ): Boolean =
    !played.isCapture ||
      drawResource ||
      (
        relationWitnessTacticalSurface(witness) &&
          MoveReviewExchangeAnalyzer.typedDetailsFromWitness(witness).exists {
            case details: MoveReviewExchangeAnalyzer.RelationDetails.DiscoveredAttack =>
              witness.kind == MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack &&
                details.clearedSquare == played.from.key
            case _ => false
          }
      )

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
      List(LocalFactAnchor("relation_kind", evidence.projection.kind)) ++
        evidence.projection.targetSquare.toList.map(square => LocalFactAnchor("relation_target", square)) ++
        evidence.projection.focusSquares.map(square => LocalFactAnchor("relation_square", square)) ++
        relationDetailsAnchors(evidence.witness.details)
    ).distinct

  private def relationDetailsAnchors(details: MoveReviewExchangeAnalyzer.RelationDetails): List[LocalFactAnchor] =
    details match
      case discovered: MoveReviewExchangeAnalyzer.RelationDetails.DiscoveredAttack =>
        List(
          LocalFactAnchor("attacker_square", discovered.attackerSquare),
          LocalFactAnchor("attacker_role", discovered.attackerRole),
          LocalFactAnchor("cleared_square", discovered.clearedSquare),
          LocalFactAnchor("target_square", discovered.targetSquare)
        )
      case overload: MoveReviewExchangeAnalyzer.RelationDetails.Overload =>
        List(
          LocalFactAnchor("defender_square", overload.defenderSquare),
          LocalFactAnchor("attacker_square", overload.attackerSquare)
        ) ++ overload.targetSquares.map(square => LocalFactAnchor("duty_square", square))
      case _ => Nil

  private def joinSquareLabels(squares: List[String]): String =
    squares.map(_.trim).filter(_.nonEmpty).distinct match
      case Nil                    => "the defended squares"
      case one :: Nil             => one
      case first :: second :: Nil => s"$first and $second"
      case many                   => s"${many.dropRight(1).mkString(", ")}, and ${many.last}"

  private def relationGuardrails(evidence: RelationWitnessEvidence): List[String] =
    List(
      "relation_witness_typed_details",
      "fen_validated_line_replayed",
      "played_move_first",
      s"relation_kind:${evidence.projection.kind}",
      s"relation_surface:${tokenKey(evidence.descriptor.surfaceRowKind)}"
    ).distinct

  private def practicalCentralChallengeDescriptor(
      played: PlayedMove,
      evidence: MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      characterBand: MoveCharacterBand,
      @unused truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewIdeaDescriptor] =
    for
      practical <- evidence.centralPractical
      if practical.kind == CentralBreakTimingWitness.PracticalKind.Challenge
      token <- BreakSurfaceToken.canonicalRoute(practical.token)
      line <- lineFacts.filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci)
    yield
      descriptor(
        ideaKind = "central_challenge",
        reviewIntent = "challenges_center",
        moveCharacterBand = characterBand,
        source = "practical_central_challenge",
        title = s"${played.san} challenges the center",
        baseProse = s"${played.san} challenges the center through ${BreakSurfaceToken.displayRoute(token)}.",
        reasonTags = List(
          "practical_central_challenge",
          "central_challenge",
          s"central_break_token:$token",
          "source:central_break_practical_witness",
          "played_move_first"
        ),
        linePurpose = Some("challenge_center"),
        lineProof = lineProof("practical_central_challenge", played, line, subjectOverride = Some(token)),
        played = played,
        evidence = evidence,
        lineFacts = lineFacts,
        requiresPvForAdmission = true,
        localFact = admittedLocalFact(LocalFactCandidate(
          family = LocalFactFamily.LineConsequence,
          source = LocalFactSource.PvCoupledLine,
          producer = LocalFactProducer.LineConsequence,
          subject = LocalFactSubject.PlayedMove,
          strictFallbackCandidate = false,
          anchors = List(
            LocalFactAnchor("central_challenge", token),
            LocalFactAnchor("played_uci", played.uci)
          ),
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs = List(
            "central_practical_kind:challenge",
            s"central_break_token:$token",
            s"played_uci:${played.uci}"
          ),
          guardrails = List(
            "central_practical_challenge",
            "played_move_first",
            "not_central_break_timing",
            "pv_coupled"
          )
        )),
        factFragments = Nil
      )

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
        baseProse = endgameFactProse(played, ownedFacts),
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
          anchors = endgameAnchors(played, ownedFacts),
          lineBinding = LocalFactLineBinding.PvCoupled,
          evidenceRefs = endgameEvidenceRefs(ownedFacts),
          guardrails = List("endgame_fact_owned_by_played_move", "pv_coupled") ++ endgameEvidenceRefs(ownedFacts)
        )),
        factFragments = List(FactFragment.EndgameFragment(
          san = played.san,
          facts = ownedFacts.map(_.toString)
        ))
      )
    }

  private def endgameFactProse(played: PlayedMove, facts: List[Fact]): String =
    facts.collectFirst { case fact: Fact.KingActivity => fact }
      .map { activity =>
        val activityText =
          if activity.proximityToCenter <= 1 then "centralizes the king"
          else "activates the king"
        s"${played.san} $activityText on ${activity.square.key} for the endgame branch."
      }
      .orElse {
        facts.collectFirst { case fact: Fact.Opposition => fact }.map { opposition =>
          val (moverKing, opposingKing) = oppositionKings(played, opposition)
          s"${played.san} sets ${tokenKey(opposition.oppositionType)} king opposition from ${moverKing.key} against the king on ${opposingKing.key}."
        }
      }
      .getOrElse(s"${played.san} is tied to a concrete endgame detail.")

  private def oppositionKings(played: PlayedMove, opposition: Fact.Opposition): (Square, Square) =
    val moverKing =
      if opposition.king == played.to then opposition.king
      else if opposition.enemyKing == played.to then opposition.enemyKing
      else played.to
    val opposingKing =
      if moverKing == opposition.king then opposition.enemyKing
      else opposition.king
    moverKing -> opposingKing

  private def endgameAnchors(played: PlayedMove, facts: List[Fact]): List[LocalFactAnchor] =
    facts.flatMap {
      case fact: Fact.KingActivity =>
        List(
          LocalFactAnchor("endgame_fact", "king_activity"),
          LocalFactAnchor("king_square", fact.square.key)
        )
      case fact: Fact.Opposition =>
        val (moverKing, opposingKing) = oppositionKings(played, fact)
        List(
          LocalFactAnchor("endgame_fact", "opposition"),
          LocalFactAnchor("king_square", moverKing.key),
          LocalFactAnchor("opposing_king_square", opposingKing.key),
          LocalFactAnchor("opposition_type", tokenKey(fact.oppositionType))
        )
      case fact: Fact.RuleOfSquare =>
        List(
          LocalFactAnchor("endgame_fact", "rule_of_square"),
          LocalFactAnchor("target_pawn", fact.targetPawn.key),
          LocalFactAnchor("promotion_square", fact.promotionSquare.key)
        )
      case _: Fact.TriangulationOpportunity =>
        List(LocalFactAnchor("endgame_fact", "triangulation"))
      case fact: Fact.RookEndgamePattern =>
        List(
          LocalFactAnchor("endgame_fact", "rook_pattern"),
          LocalFactAnchor("rook_pattern", fact.pattern)
        )
      case _: Fact.Zugzwang =>
        List(LocalFactAnchor("endgame_fact", "zugzwang"))
      case fact: Fact.PawnPromotion =>
        List(
          LocalFactAnchor("endgame_fact", "pawn_promotion"),
          LocalFactAnchor("promotion_square", fact.square.key)
        )
      case _ => Nil
    }.distinct

  private def endgameEvidenceRefs(facts: List[Fact]): List[String] =
    facts.flatMap {
      case fact: Fact.KingActivity =>
        List(
          "endgame_fact:king_activity",
          s"king_square:${fact.square.key}",
          s"king_mobility:${fact.mobility}",
          s"king_center_distance:${fact.proximityToCenter}"
        )
      case fact: Fact.Opposition =>
        List(
          "endgame_fact:opposition",
          s"king_square:${fact.king.key}",
          s"enemy_king_square:${fact.enemyKing.key}",
          s"opposition_type:${fact.oppositionType}"
        )
      case fact: Fact.RuleOfSquare =>
        List(
          "endgame_fact:rule_of_square",
          s"defender_king:${fact.defenderKing.key}",
          s"target_pawn:${fact.targetPawn.key}",
          s"promotion_square:${fact.promotionSquare.key}",
          s"rule_status:${fact.status}"
        )
      case _: Fact.TriangulationOpportunity =>
        List("endgame_fact:triangulation")
      case fact: Fact.RookEndgamePattern =>
        List("endgame_fact:rook_pattern", s"rook_pattern:${fact.pattern}")
      case _: Fact.Zugzwang =>
        List("endgame_fact:zugzwang")
      case fact: Fact.PawnPromotion =>
        List("endgame_fact:pawn_promotion", s"promotion_square:${fact.square.key}")
      case _ => Nil
    }.distinct

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
      val surface = practicalPositionSurface(played, idea, anchorValue)
      val evidenceRefs = practicalPositionEvidenceRefs(idea, anchorValue) ++ surface.toList.flatMap(_._3)
      PracticalPositionFact(
        family = family,
        source = LocalFactSource.CertifiedStrategy,
        producer = LocalFactProducer.CertifiedStrategyDelta,
        subject =
          if family == LocalFactFamily.Pressure then LocalFactSubject.Target
          else LocalFactSubject.PlanResource,
        ideaKind = idea.kind,
        label = label,
        text = surface.map(_._1).getOrElse(practicalPositionText(played, label, family, anchorValue)),
        anchors =
          List(
            LocalFactAnchor("target", anchorValue),
            LocalFactAnchor("strategic_idea_kind", idea.kind)
          ) ++ idea.focusFiles.take(2).map(file => LocalFactAnchor("focus_file", file)) ++ surface.toList.flatMap(_._2),
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

  private[analysis] def flankPawnPracticalFact(
      color: Color,
      dest: Square,
      before: StrategicStateFeatures,
      after: StrategicStateFeatures
  ): Option[PracticalPositionFact] =
    val white = color.white
    val hookBefore = if white then before.whiteHookCreationChance else before.blackHookCreationChance
    val hookAfter = if white then after.whiteHookCreationChance else after.blackHookCreationChance
    val marchAfter = if white then after.whiteRookPawnMarchReady else after.blackRookPawnMarchReady
    if !hookBefore && hookAfter then
      Some(
        flankPawnFact(
          label = "Hook creation",
          ideaKind = StrategicIdeaKind.SpaceGainOrRestriction,
          family = LocalFactFamily.Pressure,
          subject = LocalFactSubject.Target,
          anchor = dest.key,
          text = s"The checked rook-pawn move creates a flank hook on ${dest.key}.",
          feature = "hook_creation"
        )
      )
    else if marchAfter then
      Some(
        flankPawnFact(
          label = "Rook-pawn march",
          ideaKind = StrategicIdeaKind.SpaceGainOrRestriction,
          family = LocalFactFamily.PlanSupport,
          subject = LocalFactSubject.PlanResource,
          anchor = dest.key,
          text = s"The checked line advances the rook pawn to ${dest.key} for flank space.",
          feature = "rook_pawn_march"
        )
      )
    else None

  private[commentary] def flankPawnPracticalFacts(
      beforeFen: String,
      played: PlayedMove
  ): List[PracticalPositionFact] =
    if !rookPawnAdvance(played) then Nil
    else
      (for
        before <- PositionAnalyzer.extractStrategicState(beforeFen)
        after <- PositionAnalyzer.extractStrategicState(played.afterFen)
        fact <- flankPawnPracticalFact(played.color, played.to, before, after)
      yield fact).toList

  private def flankPawnFact(
      label: String,
      ideaKind: String,
      family: LocalFactFamily,
      subject: LocalFactSubject,
      anchor: String,
      text: String,
      feature: String
  ): PracticalPositionFact =
    val evidenceRefs =
      List(
        s"strategy_state_delta:$feature",
        s"rook_pawn_anchor:$anchor"
      )
    PracticalPositionFact(
      family = family,
      source = LocalFactSource.PvCoupledLine,
      producer = LocalFactProducer.CertifiedStrategyDelta,
      subject = subject,
      ideaKind = ideaKind,
      label = label,
      text = text,
      anchors =
        List(
          LocalFactAnchor("target", anchor),
          LocalFactAnchor("strategic_idea_kind", ideaKind),
          LocalFactAnchor("rook_pawn_feature", feature)
        ),
      evidenceRefs = evidenceRefs,
      guardrails =
        List(
          "practical_position_support",
          "source:strategy_state_delta",
          "rook_pawn_advance",
          "pv_coupled",
          s"strategic_idea_kind:$ideaKind"
        ) ++ evidenceRefs,
      strictFallbackCandidate = true
    )

  private def rookPawnAdvance(played: PlayedMove): Boolean =
    !played.isCapture &&
      played.role == Pawn &&
      played.from.file == played.to.file &&
      (played.to.file == chess.File.A || played.to.file == chess.File.H)

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

  private def practicalPositionSurface(
      played: PlayedMove,
      idea: StrategyIdeaSignal,
      anchor: String
  ): Option[(String, List[LocalFactAnchor], List[String])] =
    idea.kind match
      case StrategicIdeaKind.TargetFixing =>
        targetFixingSurface(played, idea)
      case StrategicIdeaKind.PawnBreak =>
        pawnBreakSurface(played, idea)
      case StrategicIdeaKind.LineOccupation =>
        lineOccupationSurface(played, idea, anchor)
      case StrategicIdeaKind.SpaceGainOrRestriction =>
        spaceGainSurface(played, idea)
      case StrategicIdeaKind.KingAttackBuildUp =>
        attackLaneSurface(played, idea, anchor)
      case _ => None

  private def spaceGainSurface(
      played: PlayedMove,
      idea: StrategyIdeaSignal
  ): Option[(String, List[LocalFactAnchor], List[String])] =
    for
      file <- spaceGainFile(played, idea)
      side <- spaceGainSide(file)
    yield
      val pawnSquare = played.to.key
      val text = s"${played.san} advances the $file-pawn for $side space."
      val anchors =
        List(
          LocalFactAnchor("space_gain_file", file),
          LocalFactAnchor("space_gain_side", side),
          LocalFactAnchor("space_gain_pawn", pawnSquare)
        )
      val evidenceRefs =
        List(
          s"space_gain_file:$file",
          s"space_gain_side:$side",
          s"space_gain_pawn:$pawnSquare",
          "space_gain_rook_pawn_advance",
          "space_gain_board_pawn_advance"
        )
      (text, anchors, evidenceRefs)

  private def spaceGainFile(
      played: PlayedMove,
      idea: StrategyIdeaSignal
  ): Option[String] =
    Option.when(
      rookPawnAdvance(played) &&
        pawnChainSpaceReady(idea.evidenceRefs) &&
        afterPositionHasPlayedPawn(played)
    ) {
      val playedFile = played.to.key.take(1)
      val focusFiles = idea.focusFiles.map(_.trim.toLowerCase).filter(_.matches("^[a-h]$")).distinct
      Option.when(focusFiles.contains(playedFile))(playedFile)
    }.flatten

  private def pawnChainSpaceReady(evidenceRefs: List[String]): Boolean =
    val refs = evidenceRefs.map(_.trim.toLowerCase)
    refs.contains("source:pawn_chain_space_motif") && refs.contains("pawn_chain_space_shape")

  private def afterPositionHasPlayedPawn(played: PlayedMove): Boolean =
    Fen
      .read(Standard, Fen.Full(played.afterFen))
      .exists(position =>
        position.board
          .pieceAt(played.to)
          .exists(piece => piece.color == played.color && piece.role == chess.Pawn)
      )

  private def spaceGainSide(file: String): Option[String] =
    file match
      case "a" => Some("queenside")
      case "h" => Some("kingside")
      case _   => None

  private def attackLaneSurface(
      played: PlayedMove,
      idea: StrategyIdeaSignal,
      anchor: String
  ): Option[(String, List[LocalFactAnchor], List[String])] =
    attackLaneTarget(played, idea, anchor).map { case (target, axis) =>
      val role = roleLabel(played.role)
      val axisText = attackLaneAxisLabel(axis)
      val text = s"${played.san} puts the $role on a $axisText toward $target."
      val anchors =
        List(
          LocalFactAnchor("attack_lane_square", target),
          LocalFactAnchor("attack_lane_axis", axis),
          LocalFactAnchor("attack_lane_attacker_square", played.to.key),
          LocalFactAnchor("attack_lane_attacker_role", role)
        )
      val evidenceRefs =
        List(
          s"attack_lane_square:$target",
          s"attack_lane_axis:$axis",
          s"attack_lane_attacker:${played.to.key}",
          s"attack_lane_role:$role",
          "attack_lane_board_attack"
        )
      (text, anchors, evidenceRefs)
    }

  private def attackLaneTarget(
      played: PlayedMove,
      idea: StrategyIdeaSignal,
      anchor: String
  ): Option[(String, String)] =
    Option.when(
      (played.role == chess.Bishop || played.role == chess.Rook || played.role == chess.Queen) &&
        directionalAttackLaneReady(idea.evidenceRefs)
    ) {
      val anchorSquares =
        Option(anchor.trim.toLowerCase).filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches).toList
      val candidates =
        (anchorSquares ++ idea.targetSquare.toList ++ idea.focusSquares ++ idea.relationFocusSquares)
          .map(_.trim.toLowerCase)
          .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
          .filter(square => square != played.from.key && square != played.to.key)
          .distinct
      Fen.read(Standard, Fen.Full(played.afterFen)).flatMap { position =>
        candidates
          .flatMap(square => Square.fromKey(square).map(square -> _))
          .find { case (_, target) =>
            position.board
              .attackers(target, played.color)
              .squares
              .contains(played.to)
          }
          .flatMap { case (square, _) =>
            attackLaneAxis(played.to.key, square).map(axis => square -> axis)
          }
      }
    }.flatten

  private def directionalAttackLaneReady(evidenceRefs: List[String]): Boolean =
    val refs = evidenceRefs.map(_.trim.toLowerCase)
    refs.contains("source:directional_attack_lane") && refs.contains("directional_attack_lane_shape")

  private def attackLaneAxis(from: String, target: String): Option[String] =
    for
      fromFile <- from.headOption.map(_ - 'a')
      targetFile <- target.headOption.map(_ - 'a')
      fromRank <- from.drop(1).toIntOption
      targetRank <- target.drop(1).toIntOption
    yield
      if fromFile == targetFile then "file"
      else if fromRank == targetRank then "rank"
      else if (fromFile - targetFile).abs == (fromRank - targetRank).abs then "diagonal"
      else "line"

  private def attackLaneAxisLabel(axis: String): String =
    axis match
      case "file"     => "file"
      case "rank"     => "rank"
      case "diagonal" => "diagonal"
      case _          => "line"

  private def targetFixingSurface(
      played: PlayedMove,
      idea: StrategyIdeaSignal
  ): Option[(String, List[LocalFactAnchor], List[String])] =
    targetFixingTarget(played, idea).map { case (target, targetKind) =>
      val label = targetFixingTargetLabel(target, targetKind)
      val text = s"${played.san} puts the ${roleLabel(played.role)} on a line toward $label."
      val anchors =
        List(
          LocalFactAnchor("target_fixing_square", target),
          LocalFactAnchor("target_fixing_target_kind", targetKind),
          LocalFactAnchor("target_fixing_attacker_square", played.to.key),
          LocalFactAnchor("target_fixing_attacker_role", roleLabel(played.role))
        )
      val evidenceRefs =
        List(
          s"target_fixing_square:$target",
          s"target_fixing_target_kind:$targetKind",
          s"target_fixing_attacker:${played.to.key}",
          s"target_fixing_role:${roleLabel(played.role)}",
          "target_fixing_board_attack"
        )
      (text, anchors, evidenceRefs)
    }

  private def targetFixingTarget(
      played: PlayedMove,
      idea: StrategyIdeaSignal
  ): Option[(String, String)] =
    Option.when(played.role == chess.Bishop || played.role == chess.Rook || played.role == chess.Queen) {
      val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
      val candidates =
        (idea.targetSquare.toList ++ idea.focusSquares ++ idea.relationFocusSquares)
          .map(_.trim.toLowerCase)
          .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
          .filter(square =>
            square != played.to.key &&
              (
                refs.contains(s"enemy_weak_square_$square") ||
                  refs.contains(s"directional_target_$square") ||
                  refs.contains(s"target_fixing_$square")
              )
          )
          .distinct
      Fen.read(Standard, Fen.Full(played.afterFen)).flatMap { position =>
        candidates.find { square =>
          Square
            .fromKey(square)
            .exists(target =>
              position.board
                .attackers(target, played.color)
                .squares
                .contains(played.to)
            )
        }
          .map(square => square -> targetFixingTargetKind(square, refs))
      }
    }.flatten

  private def targetFixingTargetKind(target: String, evidenceRefs: List[String]): String =
    if evidenceRefs.contains(s"enemy_weak_square_$target") then "weak_square"
    else "target_square"

  private def targetFixingTargetLabel(target: String, targetKind: String): String =
    if targetKind == "weak_square" then s"the weak $target square"
    else s"the $target target square"

  private def pawnBreakSurface(
      played: PlayedMove,
      idea: StrategyIdeaSignal
  ): Option[(String, List[LocalFactAnchor], List[String])] =
    for
      file <- pawnBreakFile(played, idea)
    yield
      val contested = idea.evidenceRefs.exists(_.trim.toLowerCase == s"contested_file_$file")
      val contestedText = if contested then s" and contests the $file-file" else ""
      val text = s"${played.san} plays the $file-pawn break$contestedText."
      val anchors =
        List(
          LocalFactAnchor("pawn_break_file", file)
        ) ++ Option.when(contested)(LocalFactAnchor("pawn_break_contested_file", file))
      val evidenceRefs =
        List(
          s"pawn_break_file:$file"
        ) ++ Option.when(contested)(s"pawn_break_contested_file:$file")
      (text, anchors, evidenceRefs)

  private def pawnBreakFile(
      played: PlayedMove,
      idea: StrategyIdeaSignal
  ): Option[String] =
    Option.when(
      played.role == chess.Pawn &&
        !played.isCapture &&
        played.from.file == played.to.file &&
        pawnBreakReady(idea.evidenceRefs)
    ) {
      val playedFile = played.to.key.take(1)
      val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
      val focusFiles = idea.focusFiles.map(_.trim.toLowerCase).filter(_.matches("^[a-h]$")).distinct
      Option.when(focusFiles.contains(playedFile) && refs.contains(s"break_file_$playedFile"))(playedFile)
    }.flatten

  private def pawnBreakReady(evidenceRefs: List[String]): Boolean =
    val refs = evidenceRefs.map(_.trim.toLowerCase)
    refs.contains("source:pawn_analysis_break_ready") ||
      refs.contains("source:pawn_play_break_ready")

  private def lineOccupationSurface(
      played: PlayedMove,
      idea: StrategyIdeaSignal,
      anchor: String
  ): Option[(String, List[LocalFactAnchor], List[String])] =
    for
      lineFile <- lineOccupationFile(played, idea, anchor)
      status <- lineFileStatus(idea.evidenceRefs, lineFile)
    yield
      val target = lineOccupationTarget(played, idea, lineFile)
      val statusLabel = lineFileStatusLabel(status)
      val targetText = target.map(square => s" toward $square").getOrElse("")
      val text = s"${played.san} puts the ${roleLabel(played.role)} on the $statusLabel $lineFile-file$targetText."
      val anchors =
        List(
          Some(LocalFactAnchor("line_file", lineFile)),
          Some(LocalFactAnchor("line_file_status", status)),
          target.map(square => LocalFactAnchor("line_target", square))
        ).flatten
      val evidenceRefs =
        List(
          s"line_occupation_file:$lineFile",
          s"line_occupation_status:$status"
        ) ++ target.map(square => s"line_occupation_target:$square")
      (text, anchors, evidenceRefs)

  private def lineOccupationFile(
      played: PlayedMove,
      idea: StrategyIdeaSignal,
      anchor: String
  ): Option[String] =
    Option.when(played.role == chess.Rook || played.role == chess.Queen) {
      val playedFile = played.to.key.take(1)
      val focusFiles = idea.focusFiles.map(_.trim.toLowerCase).filter(_.matches("^[a-h]$")).distinct
      val anchorFile =
        if anchor.matches("^[a-h]-file$") then Some(anchor.take(1))
        else Option.when(anchor.matches("^[a-h][1-8]$"))(anchor.take(1))
      focusFiles.find(_ == playedFile).orElse(anchorFile.filter(_ == playedFile))
    }.flatten

  private def lineOccupationTarget(
      played: PlayedMove,
      idea: StrategyIdeaSignal,
      lineFile: String
  ): Option[String] =
    idea.focusSquares
      .map(_.trim.toLowerCase)
      .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
      .filter(square => square != played.to.key && square.take(1) == lineFile)
      .headOption

  private def lineFileStatus(evidenceRefs: List[String], lineFile: String): Option[String] =
    val normalized = evidenceRefs.map(_.trim.toLowerCase)
    if normalized.exists(_.contains(s"semi_open_file_$lineFile")) then Some("semi_open")
    else if normalized.exists(_.contains(s"open_file_$lineFile")) then Some("open")
    else None

  private def lineFileStatusLabel(status: String): String =
    status match
      case "semi_open" => "semi-open"
      case "open"      => "open"
      case other       => other.replace('_', '-')

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

  private def positionProbeOwnedByPlayedMove(
      played: PlayedMove,
      delta: PlayerFacingMoveDeltaEvidence,
      lineFacts: MoveReviewPvLine.LineFacts
  ): Boolean =
    delta.packet.proofPathWitness.exactSliceProof.forall {
      case proof: PlayerFacingExactSliceProof.ColorComplexSqueeze =>
        colorComplexSqueezeOwnedByPlayedMove(played, delta.packet, lineFacts, proof)
      case _ => true
    }

  private def colorComplexLineFactsValidated(
      played: PlayedMove,
      delta: PlayerFacingMoveDeltaEvidence,
      lineFacts: MoveReviewPvLine.LineFacts
  ): Boolean =
    delta.packet.proofPathWitness.exactSliceProof.exists {
      case proof: PlayerFacingExactSliceProof.ColorComplexSqueeze =>
        colorComplexSqueezeOwnedByPlayedMove(played, delta.packet, lineFacts, proof)
      case _ => false
    }

  private def colorComplexSqueezeOwnedByPlayedMove(
      played: PlayedMove,
      packet: PlayerFacingClaimPacket,
      lineFacts: MoveReviewPvLine.LineFacts,
      proof: PlayerFacingExactSliceProof.ColorComplexSqueeze
  ): Boolean =
    PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) &&
      MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches(proof.minorPieceSquare.trim.toLowerCase) &&
      MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches(proof.targetSquare.trim.toLowerCase) &&
      ColorComplexRuntimeProof.playedMoveOwnsAndPersistsOnLine(
        playedUci = played.uci,
        playedTo = played.to,
        playedColor = played.color,
        playedRole = played.role,
        lineFacts = lineFacts,
        proof = proof
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
    (evidence.strictLocalFacts || badPieceLiquidationExactProof(delta)) &&
      !evidence.phase.trim.equalsIgnoreCase("endgame") &&
      delta.packet.releaseRisks.isEmpty &&
      delta.packet.suppressionReasons.isEmpty &&
      (
        (delta.packet.proofFamily == CentralBreakTimingWitness.ProofFamily &&
          delta.packet.proofSource == CentralBreakTimingWitness.ProofSource) ||
          MoveReviewSupportedLocalSurfaceRows.moveLocalExactSliceRow(delta.packet).nonEmpty
      )

  private def badPieceLiquidationExactProof(delta: PlayerFacingMoveDeltaEvidence): Boolean =
    delta.packet.proofPathWitness.exactSliceProof.exists {
      case _: PlayerFacingExactSliceProof.BadPieceLiquidation => true
      case _                                                  => false
    }

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
    else if outpostOccupationExactProof(delta).nonEmpty then
      outpostOccupationExactProof(delta)
        .map { case (pieceRole, square) => s"${played.san} puts the $pieceRole on the $square outpost." }
        .get
    else if exactTargetFixationPawnTarget(played, delta).nonEmpty then
      exactTargetFixationPawnTarget(played, delta)
        .map(square => s"${played.san} pressures the $square pawn target.")
        .get
    else if badPieceLiquidationExactProof(delta) then
      MoveReviewSupportedLocalSurfaceRows.moveLocalExactSliceRow(delta.packet)
        .map(_.text)
        .getOrElse(s"${played.san} clarifies the exchange.")
    else
      val anchor = preferredStrategicSubject(played, delta).map(subject => s" around $subject").getOrElse("")
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

  private def strategicExactProofAnchors(
      played: PlayedMove,
      delta: PlayerFacingMoveDeltaEvidence
  ): List[LocalFactAnchor] =
    outpostOccupationExactProof(delta)
      .map { case (pieceRole, square) =>
        List(
          LocalFactAnchor("outpost_square", square),
          LocalFactAnchor("outpost_piece_role", pieceRole)
        )
      }
      .orElse(
        exactTargetFixationPawnTarget(played, delta).map(square =>
          List(
            LocalFactAnchor("exact_target_square", square),
            LocalFactAnchor("exact_target_kind", "pawn_target"),
            LocalFactAnchor("exact_target_piece", "pawn")
          )
        )
      )
      .getOrElse(Nil)

  private def strategicExactProofEvidenceRefs(
      played: PlayedMove,
      delta: PlayerFacingMoveDeltaEvidence
  ): List[String] =
    outpostOccupationExactProof(delta)
      .map { case (pieceRole, square) =>
        List(
          s"outpost_square:$square",
          s"outpost_piece_role:$pieceRole",
          "outpost_occupation_exact_proof"
        )
      }
      .orElse(
        exactTargetFixationPawnTarget(played, delta).map { square =>
          List(
            s"exact_target_square:$square",
            "exact_target_piece:pawn",
            s"fixed_target:$square",
            "exact_target_board_attack",
            "exact_target_fixation_exact_proof"
          ) ++ exactTargetFixationPersistenceTerms(delta, square)
        }
      )
      .getOrElse(Nil)

  private def strategicExactProofSubject(
      played: PlayedMove,
      delta: PlayerFacingMoveDeltaEvidence
  ): Option[String] =
    outpostOccupationExactProof(delta)
      .map(_._2)
      .orElse(exactTargetFixationPawnTarget(played, delta))

  private def outpostOccupationExactProof(delta: PlayerFacingMoveDeltaEvidence): Option[(String, String)] =
    delta.packet.proofPathWitness.exactSliceProof.collect {
      case PlayerFacingExactSliceProof.OutpostOccupation(pieceRole, square)
          if pieceRole.trim.equalsIgnoreCase("knight") &&
            MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches(square.trim.toLowerCase) =>
        pieceRole.trim.toLowerCase -> square.trim.toLowerCase
    }

  private def exactTargetFixationExactProof(delta: PlayerFacingMoveDeltaEvidence): Option[String] =
    delta.packet.proofPathWitness.exactSliceProof.collect {
      case PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare)
          if MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches(targetSquare.trim.toLowerCase) =>
        targetSquare.trim.toLowerCase
    }

  private def exactTargetFixationPawnTarget(
      played: PlayedMove,
      delta: PlayerFacingMoveDeltaEvidence
  ): Option[String] =
    for
      proof <- delta.packet.proofPathWitness.exactSliceProof.collect {
        case proof: PlayerFacingExactSliceProof.ExactTargetFixation => proof
      }
      if PlayerFacingExactSliceProofFacts.matchesPacket(delta.packet, proof)
      square <- exactTargetFixationExactProof(delta)
      target <- Square.fromKey(square)
      if exactTargetFixationPersistenceTerms(delta, square).contains(s"target_persistent_after_line:$square")
      if exactTargetFixationPersistenceTerms(delta, square).contains(s"target_attacked_after_line:$square")
      position <- Fen.read(Standard, Fen.Full(played.afterFen))
      targetPiece <- position.board.pieceAt(target)
      if targetPiece.color != played.color && targetPiece.role == Pawn
      if position.board.attackers(target, played.color).squares.exists(_ == played.to)
    yield square

  private def exactTargetFixationPersistenceTerms(
      delta: PlayerFacingMoveDeltaEvidence,
      square: String
  ): List[String] =
    val target = square.trim.toLowerCase
    val accepted =
      Set(
        s"target_persistent_after_line:$target",
        s"target_attacked_after_line:$target"
      )
    (
      delta.packet.proofPathWitness.ownerSeedTerms ++
        delta.packet.proofPathWitness.continuationTerms ++
        delta.packet.proofPathWitness.structureTransitionTerms ++
        delta.packet.anchorTerms ++
        delta.anchorTerms
    ).map(_.trim.toLowerCase).filter(accepted.contains).distinct

  private def preferredStrategicSubject(
      played: PlayedMove,
      delta: PlayerFacingMoveDeltaEvidence
  ): Option[String] =
    strategicExactProofSubject(played, delta)
      .orElse(
        MoveReviewSupportedLocalSurfaceRows
          .moveLocalExactSliceRow(delta.packet)
          .flatMap(_.authority.flatMap(_.target))
      )
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
      case fact: Fact.Opposition =>
        played.role == King && (fact.king == played.to || fact.enemyKing == played.to)
      case fact: Fact.RuleOfSquare =>
        (played.role == King && fact.defenderKing == played.to) ||
          (played.role == chess.Pawn && fact.targetPawn == played.to)
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

  private def followUpQueenTradeDetail(line: MoveReviewPvLine.LineFacts): Option[(String, String, String)] =
    val checkedMoves = (line.reply.toList ++ line.checkedContinuations).take(6)
    MoveReviewExchangeAnalyzer
      .boundedReplay(line.first.fenAfter, checkedMoves.map(_.uci), maxPlies = checkedMoves.size)
      .flatMap { replay =>
        replay.zip(checkedMoves).zipWithIndex.collectFirst {
          case (((step, moveRef), index))
              if step.move.piece.role == chess.Queen &&
                step.move.captures &&
                step.capturedRole.contains(chess.Queen) &&
                replay.lift(index + 1).exists(next =>
                  next.move.captures &&
                    next.capturedRole.contains(chess.Queen) &&
                    next.move.dest == step.move.dest
                ) =>
            val recaptureSan = checkedMoves.lift(index + 1).map(_.san).getOrElse("")
            (step.move.dest.key, moveRef.san, recaptureSan)
        }
      }
      .filter((_, captureSan, recaptureSan) => captureSan.trim.nonEmpty && recaptureSan.trim.nonEmpty)

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

  private final case class OpeningGoalSurface(title: String, prose: String)

  private def openingGoalSurface(
      played: PlayedMove,
      openingName: Option[String],
      goal: OpeningGoals.Evaluation
  ): OpeningGoalSurface =
    val openingPart = openingName.map(name => s" in the $name").getOrElse("")
    val key = slug(goal.goalName)
    val supportText = openingGoalSupportText(goal.supportedEvidence)
    val (title, lead) =
      if key.contains("fianchetto") then
        if played.role == chess.Pawn then
          s"${played.san} builds a fianchetto shell" ->
            s"${played.san}$openingPart builds the fianchetto shell for long-diagonal play"
        else
          s"${played.san} activates the long diagonal" ->
            s"${played.san}$openingPart activates the fianchetto bishop on the long diagonal"
      else if key.contains("break_preparation") || key.contains("thematic_break") then
        s"${played.san} prepares a central break" ->
          s"${played.san}$openingPart prepares the central-break scaffold"
      else if key.contains("early_queen") then
        s"${played.san} keeps an early queen deployment playable" ->
          s"${played.san}$openingPart uses an early queen deployment that the PV keeps playable"
      else if key.contains("development") then
        val role = roleLabel(played.role)
        s"${played.san} develops the $role toward opening coordination" ->
          s"${played.san}$openingPart develops the $role toward opening coordination"
      else
        val goalPhrase = openingGoalPhrase(goal.goalName)
        s"${played.san} matches $goalPhrase" ->
          s"${played.san}$openingPart matches $goalPhrase"
    OpeningGoalSurface(title, s"$lead.$supportText")

  private def openingGoalSupportText(evidence: List[String]): String =
    val clauses = evidence.map(openingGoalEvidenceClause).filter(_.nonEmpty).distinct
    if clauses.isEmpty then ""
    else s" The local evidence is: ${clauses.mkString(", ")}."

  private def openingGoalEvidenceClause(value: String): String =
    Option(value).getOrElse("").trim match
      case "" => ""
      case "Center foothold" => "a center foothold"
      case "Minor pieces developed" => "minor pieces are developed"
      case "Development underway" => "development is underway"
      case "Checked line keeps the development move in range" => "the checked line keeps the development move in range"
      case "Fianchetto shell built" => "the fianchetto shell is built"
      case "PV activates the long diagonal" => "the checked line activates the long diagonal"
      case "Long diagonal active" => "the long diagonal is active"
      case "Center supported" => "the center is supported"
      case "Break support pieces in place" => "break-support pieces are in place"
      case "Center contact preserved" => "center contact is preserved"
      case "Break scaffold appears" => "the break scaffold appears"
      case "Development cost contained" => "the development cost is contained"
      case other => other.head.toLower.toString + other.tail

  private def openingGoalPhrase(goalName: String): String =
    val key = slug(goalName)
    if key.contains("development") || key.contains("fianchetto") then "a checked development cue"
    else if key.contains("challenge") || key.contains("sicilian") then "a checked center cue"
    else if key.contains("break") || key.contains("gambit") then "a checked pawn-break cue"
    else if key.contains("tension") || key.contains("release") then "a checked tension cue"
    else if key.contains("safety") || key.contains("castle") then "a checked king-safety cue"
    else "a checked opening cue"

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
