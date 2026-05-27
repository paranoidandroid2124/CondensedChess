package lila.commentary.analysis

import lila.commentary.*
import lila.commentary.model.NarrativeContext
import lila.commentary.analysis.claim.{ ClaimAuthorityResolver, ClaimAuthorityTier }
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.analysis.PlanEvidenceEvaluator.{ EvaluatedPlan, UserFacingPlanEligibility }
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme }
import chess.format.{ Fen, Uci }
import chess.variant.Standard

object MoveReviewPlayerPayloadBuilder:

  private val Schema = "chesstory.move_review.player_surface.v2"
  private val MinDecisionSurfaceGapCp = 35
  private val ClearDecisionSurfaceGapCp = 60
  private val ExactComparativeSource = "exact"

  def decisionComparisonSurface(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs]
  ): Option[MoveReviewPlayerDecisionComparison] =
    decisionComparisonSurface(
      comparison = DecisionComparisonBuilder.digest(ctx, refs),
      lineEvidence = LineConsequenceEvaluator.surfaceCandidate(ctx, refs)
    )

  private[analysis] def decisionComparisonSurface(
      comparison: Option[DecisionComparisonDigest],
      lineEvidence: Option[LineConsequenceEvidence]
  ): Option[MoveReviewPlayerDecisionComparison] =
    for
      digest <- comparison
      evidence <- lineEvidence
      if evidence.surfaceReady
      if hasComparableDecisionShape(digest, evidence)
      if hasDecisionSurfaceReason(digest)
      secondary <- safeDecisionSecondaryText(evidence)
    yield MoveReviewPlayerDecisionComparison(
      kicker = "Decision point",
      gapLabel = digest.cpLossVsChosen.map(decisionGapLabel),
      chosenSan = cleanMove(digest.chosenMove),
      engineSan = cleanMove(digest.engineBestMove),
      comparedSan = cleanMove(digest.comparedMove),
      deferredSan = None,
      secondaryText = Some(secondary),
      chosenMatchesBest = digest.chosenMatchesBest
    )

  def build(
      ctx: NarrativeContext,
      moveReviewExplanation: Option[MoveReviewExplanation],
      moveReviewLedger: Option[MoveReviewStrategicLedger],
      refs: Option[MoveReviewRefs],
      evaluatedPlans: List[EvaluatedPlan],
      authoringSurface: AuthoringEvidenceSurface,
      supportedLocalRows: List[MoveReviewPlayerSurfaceRow] = Nil,
      decisionComparisonSurface: Option[MoveReviewPlayerDecisionComparison] = None
  ): MoveReviewPlayerSurface =
    val knownSans = refs.toList.flatMap(_.variations.flatMap(_.moves.map(_.san))).map(normalizeSan).toSet
    val promotedPlans = evaluatedPlans.filter(isProbeBackedPlan).sortBy(_.hypothesis.rank)
    val summaryRows =
      (mainPlanRow(promotedPlans).toList ++ sanitizeRows(supportedLocalRows, knownSans))
        .distinctBy(row => (row.label, row.text))
    MoveReviewPlayerSurface(
      schema = Schema,
      title =
        moveReviewExplanation.flatMap(explanation => cleanOpt(Some(explanation.title)))
          .orElse(ctx.playedSan.flatMap(san => cleanOpt(Some(s"Move review: $san")))),
      summaryRows = summaryRows,
      advancedRows = advancedRows(promotedPlans),
      decisionComparison = decisionComparisonSurface.map(sanitizeDecisionComparison),
      probeRows = ledgerRows(moveReviewLedger, knownSans),
      authorRows = authorRows(authoringSurface, knownSans)
    )

  private def mainPlanRow(promotedPlans: List[EvaluatedPlan]): Option[MoveReviewPlayerSurfaceRow] =
    val plans =
      promotedPlans
        .flatMap(plan => cleanOpt(Some(plan.hypothesis.planName)))
        .distinct
        .take(2)
    row("Main plans", plans.mkString(" / "))

  private def advancedRows(
      promotedPlans: List[EvaluatedPlan]
  ): List[MoveReviewPlayerSurfaceRow] =
    val planRows =
      promotedPlans.take(2).flatMap(planRowsFromPromotedPlan)
    planRows.distinctBy(row => (row.label, row.text)).take(8)

  private def planRowsFromPromotedPlan(plan: EvaluatedPlan): List[MoveReviewPlayerSurfaceRow] =
    val hypothesis = plan.hypothesis
    List(
      row("Execution", hypothesis.executionSteps.take(2).mkString(" - ")),
      row("Objective", hypothesis.preconditions.take(2).mkString(" - ")),
      Option.when(isProphylaxisPlan(plan)) {
        val text =
          cleanOpt(Some(hypothesis.planName))
            .orElse(cleanOpt(hypothesis.failureModes.headOption))
            .getOrElse("")
        row("Prophylaxis", text)
      }.flatten
    ).flatten

  private def ledgerRows(
      moveReviewLedger: Option[MoveReviewStrategicLedger],
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    moveReviewLedger.toList.flatMap { ledger =>
      List(ledger.primaryLine, ledger.resourceLine).flatten.flatMap { line =>
        val text =
          cleanOpt(line.note)
            .orElse(cleanOpt(Some(line.sanMoves.mkString(" "))))
        row(
          label = line.title,
          text = text.getOrElse(""),
          refSans = refSans(line.sanMoves, knownSans)
        )
      }
    }

  private def authorRows(
      authoringSurface: AuthoringEvidenceSurface,
      knownSans: Set[String]
  ): List[MoveReviewPlayerAuthorRow] =
    val evidenceRows =
      authoringSurface.evidence.take(2).flatMap(summary => authorEvidenceRow(summary, knownSans))
    if evidenceRows.nonEmpty then evidenceRows
    else authoringSurface.questions.take(2).flatMap(authorQuestionRow)

  private def authorEvidenceRow(
      summary: AuthorEvidenceSummary,
      knownSans: Set[String]
  ): Option[MoveReviewPlayerAuthorRow] =
    val question = cleanOpt(Some(summary.question))
    question.map { q =>
      MoveReviewPlayerAuthorRow(
        title = humanizeToken(summary.questionKind),
        status = clean(summary.status),
        question = q,
        why = cleanOpt(summary.why),
        meta = Nil,
        branches =
          summary.branches.take(2).flatMap { branch =>
            row(
              label = branch.keyMove,
              text = branch.line,
              refSans = refSans(List(branch.keyMove), knownSans)
            )
          }
      )
    }

  private def authorQuestionRow(summary: AuthorQuestionSummary): Option[MoveReviewPlayerAuthorRow] =
    cleanOpt(Some(summary.question)).map { question =>
      MoveReviewPlayerAuthorRow(
        title = humanizeToken(summary.kind),
        status = "question_only",
        question = question,
        why = cleanOpt(summary.why),
        meta = Nil
      )
    }

  private def isProbeBackedPlan(plan: EvaluatedPlan): Boolean =
    plan.userFacingEligibility == UserFacingPlanEligibility.ProbeBacked &&
      plan.supportProbeIds.nonEmpty

  private def isProphylaxisPlan(plan: EvaluatedPlan): Boolean =
    plan.themeL1 == PlanTheme.RestrictionProphylaxis.id ||
      plan.subplanId.exists(id =>
        Set(
          PlanKind.ProphylaxisRestraint.id,
          PlanKind.BreakPrevention.id,
          PlanKind.FlankClamp.id,
          PlanKind.CentralSpaceBind.id,
          PlanKind.MobilitySuppression.id,
          PlanKind.KeySquareDenial.id
        ).contains(id)
      )

  private def row(
      label: String,
      text: String,
      refSans: List[String] = Nil,
      tone: Option[String] = None
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      cleanLabel <- cleanOpt(Some(label))
      cleanText <- cleanOpt(Some(text))
    yield MoveReviewPlayerSurfaceRow(
      label = cleanLabel,
      text = cleanText,
      tone = tone.flatMap(value => cleanOpt(Some(value))),
      source = None,
      refSans = cleanList(refSans)
    )

  private def sanitizeRows(
      rows: List[MoveReviewPlayerSurfaceRow],
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    rows.flatMap { sourceRow =>
      for
        cleanLabel <- cleanOpt(Some(sourceRow.label))
        cleanText <- cleanOpt(Some(sourceRow.text))
      yield MoveReviewPlayerSurfaceRow(
        label = cleanLabel,
        text = cleanText,
        tone = sourceRow.tone.flatMap(value => cleanOpt(Some(value))),
        source = None,
        refSans = refSans(sourceRow.refSans, knownSans),
        authority = sourceRow.authority
      )
    }

  private def sanitizeDecisionComparison(
      comparison: MoveReviewPlayerDecisionComparison
  ): MoveReviewPlayerDecisionComparison =
    comparison.copy(
      kicker = clean(comparison.kicker),
      gapLabel = cleanOpt(comparison.gapLabel),
      chosenSan = cleanOpt(comparison.chosenSan),
      engineSan = cleanOpt(comparison.engineSan),
      comparedSan = cleanOpt(comparison.comparedSan),
      deferredSan = cleanOpt(comparison.deferredSan),
      secondaryText = cleanOpt(comparison.secondaryText)
    )

  private def hasTwoComparableMoves(digest: DecisionComparisonDigest): Boolean =
    List(digest.chosenMove, digest.engineBestMove, digest.comparedMove)
      .flatten
      .flatMap(move => cleanMove(Some(move)))
      .map(normalizeMove)
      .distinct
      .size >= 2

  private def hasComparableDecisionShape(
      digest: DecisionComparisonDigest,
      evidence: LineConsequenceEvidence
  ): Boolean =
    hasTwoComparableMoves(digest) || hasLaterLineDivergence(digest, evidence)

  private def hasLaterLineDivergence(
      digest: DecisionComparisonDigest,
      evidence: LineConsequenceEvidence
  ): Boolean =
    val compared = cleanMove(digest.comparedMove)
    val anchorMoves = List(digest.chosenMove, digest.engineBestMove).flatten.flatMap(move => cleanMove(Some(move)))
    compared.exists { comparedMove =>
      anchorMoves.exists(anchor => normalizeMove(anchor) == normalizeMove(comparedMove)) &&
        evidence.kind != LineConsequenceKind.PreviewOnly &&
        triggerPly(evidence).exists(_ > 1)
    }

  private def hasDecisionSurfaceReason(digest: DecisionComparisonDigest): Boolean =
    digest.cpLossVsChosen.exists(_ >= MinDecisionSurfaceGapCp) ||
      digest.practicalAlternative ||
      (
        digest.comparativeConsequence.exists(_.trim.nonEmpty) &&
          digest.comparativeSource.exists(_.toLowerCase.contains(ExactComparativeSource))
      )

  private def decisionGapLabel(cp: Int): String =
    if cp >= ClearDecisionSurfaceGapCp then s"${cp}cp"
    else s"${cp}cp slight"

  private def triggerPly(evidence: LineConsequenceEvidence): Option[Int] =
    evidence.triggerSan
      .flatMap(trigger =>
        evidence.sanMoves.indexWhere(san => normalizeMove(san) == normalizeMove(trigger)) match
          case -1  => None
          case idx => Some(idx + 1)
      )

  private def safeDecisionSecondaryText(evidence: LineConsequenceEvidence): Option[String] =
    Option
      .when(evidence.kind != LineConsequenceKind.PreviewOnly)(clean(evidence.playerSentence).trim)
      .filter(_.nonEmpty)

  private def cleanMove(raw: Option[String]): Option[String] =
    raw.map(clean).map(_.trim).filter(_.nonEmpty)

  private def normalizeMove(raw: String): String =
    raw.replaceAll("""[+#?!]+$""", "").toLowerCase

  private def refSans(sans: List[String], knownSans: Set[String]): List[String] =
    val cleaned = cleanList(sans)
    if knownSans.isEmpty then cleaned
    else cleaned.filter(san => knownSans.contains(normalizeSan(san)))

  private def clean(value: String): String =
    UserFacingSignalSanitizer.sanitize(value)

  private def cleanOpt(value: Option[String]): Option[String] =
    value.map(clean).map(_.trim).filter(_.nonEmpty)

  private def cleanList(values: List[String]): List[String] =
    values.flatMap(value => cleanOpt(Some(value))).distinct

  private def normalizeSan(value: String): String =
    Option(value).getOrElse("").replaceAll("[+#?!]+", "").trim.toLowerCase

  private def humanizeToken(raw: String): String =
    clean(raw.replace("_", " ").replace("-", " "))
      .split("\\s+")
      .filter(_.nonEmpty)
      .map(part => part.take(1).toUpperCase + part.drop(1).toLowerCase)
      .mkString(" ")

private[commentary] object NeutralizeKeyBreakSurfaceGate:

  val MissingNamedBreak = "surface:named_break_missing"
  val PlayedMoveCollision = "surface:played_move_collision"
  val PlayedMoveUnverified = "surface:played_move_unverified"

  final case class Decision(token: Option[String], rejectReason: Option[String]):
    def admitted: Boolean = token.nonEmpty && rejectReason.isEmpty

  def decideForPlanPacket(
      plan: QuestionPlan,
      packet: PlayerFacingClaimPacket,
      ctx: NarrativeContext
  ): Decision =
    val planToken = plan.timingWitness.flatMap(_.namedBreak).flatMap(canonicalToken)
    val packetToken = packetBreakToken(packet)
    (planToken, packetToken) match
      case (Some(left), Some(right)) if left == right =>
        decideToken(left, Some(ctx))
      case _ =>
        Decision(None, Some(MissingNamedBreak))

  def decideForPacket(
      packet: PlayerFacingClaimPacket,
      ctx: NarrativeContext
  ): Decision =
    decideForPacket(packet, Some(ctx))

  def decideForPacket(
      packet: PlayerFacingClaimPacket,
      ctx: Option[NarrativeContext]
  ): Decision =
    packetBreakToken(packet)
      .map(token => decideToken(token, ctx))
      .getOrElse(Decision(None, Some(MissingNamedBreak)))

  def surfaceText(token: String): String =
    s"On the checked line, this stops the $token break before it appears."

  private def packetBreakToken(packet: PlayerFacingClaimPacket): Option[String] =
    packet.proofPathWitness.exactSliceProof.collect {
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken) => breakToken
    }.flatMap(canonicalToken)

  private def decideToken(token: String, ctx: Option[NarrativeContext]): Decision =
    singleSquareToken(token) match
      case Some(square) =>
        ctx.flatMap(legalPlayedTargetSquare) match
          case Some(target) if target == square => Decision(None, Some(PlayedMoveCollision))
          case Some(_)                         => Decision(Some(token), None)
          case None                            => Decision(None, Some(PlayedMoveUnverified))
      case None => Decision(Some(token), None)

  private def canonicalToken(raw: String): Option[String] =
    val lower = Option(raw).map(_.trim.toLowerCase).getOrElse("")
    if lower.isEmpty ||
        lower.contains("_") ||
        lower.contains(":") ||
        lower.contains("|") ||
        lower.contains(" ") ||
        lower.contains("counterplay") ||
        lower.contains("neutralize")
    then None
    else
      val hasEllipsis = lower.startsWith("...")
      val core = if hasEllipsis then lower.drop(3) else lower
      Option.when(core.matches("""[a-h][1-8]""") || core.matches("""[a-h][1-8]-[a-h][1-8]""")) {
        s"${if hasEllipsis then "..." else ""}$core"
      }

  private def singleSquareToken(token: String): Option[String] =
    val core = token.stripPrefix("...")
    Option.when(core.matches("""[a-h][1-8]"""))(core)

  private def legalPlayedTargetSquare(ctx: NarrativeContext): Option[String] =
    for
      position <- Fen.read(Standard, Fen.Full(ctx.fen))
      uci <- ctx.playedMove.map(_.trim.toLowerCase).filter(_.nonEmpty)
      move <- Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)
    yield move.dest.key

private[commentary] object CentralBreakTimingSurfaceGate:

  val MissingExactWitness = "surface:central_break_exact_witness_missing"
  val MalformedBreakToken = "surface:central_break_token_malformed"

  final case class Decision(token: Option[String], rejectReason: Option[String]):
    def admitted: Boolean = token.nonEmpty && rejectReason.isEmpty

  def decide(witness: CentralBreakTimingWitness.Witness): Decision =
    val token = Option(witness.breakToken).map(_.trim).filter(_.nonEmpty)
    token match
      case Some(value) =>
        classifyRouteToken(value) match
          case RouteToken.Valid(canonical) => Decision(Some(canonical), None)
          case RouteToken.Malformed        => Decision(None, Some(MalformedBreakToken))
      case None =>
        Decision(None, Some(MissingExactWitness))

  def surfaceText(witness: CentralBreakTimingWitness.Witness, token: String): String =
    if witness.sourceTags.contains("board:played_break") then
      s"On the checked line, this also plays the $token break at this moment."
    else s"On the checked line, this also leaves the $token break available on this branch."

  private enum RouteToken:
    case Valid(token: String)
    case Malformed

  private def classifyRouteToken(raw: String): RouteToken =
    val lower = Option(raw).map(_.trim.toLowerCase).getOrElse("")
    val hasEllipsis = lower.startsWith("...")
    val core = if hasEllipsis then lower.drop(3) else lower
    core.split("-", 2).toList match
      case from :: to :: Nil
          if from.matches("""[a-h][1-8]""") && to.matches("""[a-h][1-8]""") =>
        RouteToken.Valid(s"${if hasEllipsis then "..." else ""}$from-$to")
      case _ => RouteToken.Malformed

private[commentary] object MoveReviewSupportedLocalSurfaceRows:

  private val CounterplayBreakLabel = "Counterplay break"
  private val CentralBreakLabel = "Central break"

  def build(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      truthContract: Option[DecisiveTruthContract]
  ): List[MoveReviewPlayerSurfaceRow] =
    val planRows =
      List(rankedPlans.primary, rankedPlans.secondary).flatten
        .flatMap(plan => rowForPlan(ctx, inputs, truthContract, plan))
    val packetRows =
      mainPathClaims(inputs)
        .flatMap(claim => rowForClaim(ctx, inputs, truthContract, claim))
    (planRows ++ packetRows)
      .distinctBy(row => (row.label, row.text))
      .take(1)

  private def rowForPlan(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[MoveReviewPlayerSurfaceRow] =
    Option.when(isNeutralizeKeyBreakPlan(plan)) {
      ClaimAuthorityResolver.supportedLocalNeutralizeKeyBreakTimingAdmission(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract,
        plan = plan
      )
    }.flatten
      .filter(admission =>
        admission.decision.tier == ClaimAuthorityTier.SupportedLocal && admission.decision.vetoReasons.isEmpty
      )
      .flatMap { admission =>
        NeutralizeKeyBreakSurfaceGate.decideForPlanPacket(plan, admission.packet, ctx).token
      }
      .flatMap(token =>
        row(
          CounterplayBreakLabel,
          NeutralizeKeyBreakSurfaceGate.surfaceText(token),
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some(token)))
        )
      )

  private def rowForClaim(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      claim: MainPathScopedClaim
  ): Option[MoveReviewPlayerSurfaceRow] =
    claim.packet.flatMap { packet =>
      if packet.proofSource == ProofSourceId.CounterplayAxisSuppression.wireKey &&
          packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey
      then
        val decision =
          ClaimAuthorityResolver.supportedLocalNeutralizeKeyBreakPacketDecision(
            ctx = Some(ctx),
            inputs = inputs,
            truthContract = truthContract,
            packet = packet
          )
        Option
          .when(decision.tier == ClaimAuthorityTier.SupportedLocal && decision.vetoReasons.isEmpty)(packet)
          .flatMap(packet => NeutralizeKeyBreakSurfaceGate.decideForPacket(packet, ctx).token)
          .flatMap(token =>
            row(
              CounterplayBreakLabel,
              NeutralizeKeyBreakSurfaceGate.surfaceText(token),
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some(token)))
            )
          )
      else if packet.proofSource == CentralBreakTimingWitness.ProofSource &&
          packet.proofFamily == CentralBreakTimingWitness.ProofFamily
      then
        ClaimAuthorityResolver
          .supportedLocalCentralBreakTimingAdmission(
            ctx = Some(ctx),
            inputs = inputs,
            truthContract = truthContract,
            packet = packet
          )
          .filter(admission =>
            admission.decision.tier == ClaimAuthorityTier.SupportedLocal &&
              admission.decision.vetoReasons.isEmpty
          )
          .flatMap { admission =>
            CentralBreakTimingSurfaceGate
              .decide(admission.witness)
              .token
              .map(token =>
                token ->
                  CentralBreakTimingSurfaceGate.surfaceText(admission.witness, token)
              )
          }
          .flatMap { case (token, text) =>
            row(
              CentralBreakLabel,
              text,
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralBreak, token = Some(token)))
            )
          }
        else None
      }

  private def mainPathClaims(inputs: QuestionPlannerInputs): List[MainPathScopedClaim] =
    inputs.mainBundle.toList.flatMap { bundle =>
      List(bundle.mainClaim, bundle.lineScopedClaim).flatten
    }

  private def isNeutralizeKeyBreakPlan(plan: QuestionPlan): Boolean =
    plan.timingWitness.exists(_.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey)

  private def row(
      label: String,
      text: String,
      authority: Option[MoveReviewSurfaceAuthority]
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      cleanLabel <- cleanOpt(label)
      cleanText <- cleanOpt(text)
    yield MoveReviewPlayerSurfaceRow(
      label = cleanLabel,
      text = cleanText,
      tone = None,
      source = None,
      refSans = Nil,
      authority = authority
    )

  private def cleanOpt(value: String): Option[String] =
    Option(value)
      .map(UserFacingSignalSanitizer.sanitize)
      .map(_.trim)
      .filter(_.nonEmpty)
