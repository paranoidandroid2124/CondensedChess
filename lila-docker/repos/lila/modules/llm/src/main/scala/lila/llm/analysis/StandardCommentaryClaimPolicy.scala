package lila.llm.analysis

import _root_.chess.{ Pawn, Role, Square }
import java.util.concurrent.atomic.AtomicLong
import lila.llm.model.*

private[analysis] object StandardCommentaryClaimPolicy:

  private val EarlyOpeningPlyCutoff = 10
  private val GuardedOpeningPlyCutoff = 16
  private val AmberThreatCp = 50
  private val StrongThreatCp = 80
  private val UrgentThreatCp = 200
  private val SevereCounterfactualCp = 150
  private val CentralOpeningPawnSquares =
    Set("c4", "c5", "d4", "d5", "e4", "e5", "f4", "f5")

  private val redTierSuppressions = new AtomicLong(0L)
  private val amberTierDowngrades = new AtomicLong(0L)
  private val noEventNoteEmissions = new AtomicLong(0L)
  private val quietStandardSentenceCount = new AtomicLong(0L)

  enum HangingTier:
    case Red
    case Amber
    case Suppress

  def isStandard(ctx: NarrativeContext): Boolean =
    EarlyOpeningNarrationPolicy.normalizeVariantKey(Some(ctx.variantKey)) ==
      EarlyOpeningNarrationPolicy.StandardVariant

  def openingLike(ctx: NarrativeContext): Boolean =
    Option(ctx.phase.current).exists(_.trim.equalsIgnoreCase("opening")) ||
      Option(ctx.header.phase).exists(_.trim.equalsIgnoreCase("opening")) ||
      ctx.openingEvent.isDefined

  def hasMeaningfulOpeningEvent(ctx: NarrativeContext): Boolean =
    ctx.openingEvent.exists {
      case OpeningEvent.BranchPoint(_, _, _) => true
      case OpeningEvent.OutOfBook(_, _, _)   => true
      case OpeningEvent.TheoryEnds(_, _)     => true
      case OpeningEvent.Novelty(_, _, _, _)  => true
      case OpeningEvent.Intro(_, _, _, _)    => false
    }

  def hasForcedOrCriticalState(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    TacticalTensionPolicy.hasForcedOrCriticalState(ctx, truthContract)

  def hasStrongTacticalPressure(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    TacticalTensionPolicy.hasStrongTacticalPressure(ctx, truthContract)

  def hasSevereCounterfactual(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    TacticalTensionPolicy.hasSevereCounterfactual(ctx, truthContract)

  def hasDurableStructuralCommitment(ctx: NarrativeContext): Boolean =
    ctx.delta.exists(d => d.structureChange.exists(_.trim.nonEmpty) || d.openFileCreated.exists(_.trim.nonEmpty)) ||
      ctx.semantic.exists { semantic =>
        semantic.structuralWeaknesses.nonEmpty ||
          semantic.positionalFeatures.exists { tag =>
            Set("OpenFile", "WeakSquare", "Outpost").contains(tag.tagType.trim)
          }
      } ||
      ctx.pawnPlay.tensionPolicy.equalsIgnoreCase("Release") ||
      ctx.pawnPlay.breakImpact.equalsIgnoreCase("High") ||
      ctx.pawnPlay.counterBreak ||
      List("break_ready", "tension_critical", "passed_pawn").contains(ctx.pawnPlay.primaryDriver.trim.toLowerCase) ||
      ctx.snapshots.exists(snapshot =>
        List(snapshot.kingSafetyUs, snapshot.kingSafetyThem).flatten.exists { value =>
          val low = value.trim.toLowerCase
          low.contains("exposed") ||
          low.contains("unsafe") ||
          low.contains("broken") ||
          low.contains("weakened")
        }
      )

  def allowsRedTier(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    !isStandard(ctx) ||
      hasForcedOrCriticalState(ctx, truthContract) ||
      ctx.threats.toUs.exists(t => t.lossIfIgnoredCp >= UrgentThreatCp || t.kind.toLowerCase.contains("mate")) ||
      hasStrongTacticalPressure(ctx, truthContract) ||
      hasSevereCounterfactual(ctx, truthContract)

  def allowsAmberTier(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    !isStandard(ctx) || (!quietOpeningNoEvent(ctx, truthContract) && signalSupportCount(ctx) >= 2)

  def quietStandardPosition(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Boolean =
    quietOpeningNoEvent(ctx, truthContract) || quietLaterNoEvent(ctx, truthContract)

  def noEventNote(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[String] =
    Option.when(quietStandardPosition(ctx, truthContract)) {
      val note =
        if openingLike(ctx) && ctx.ply <= GuardedOpeningPlyCutoff then openingNoEventSentence(ctx)
        else quietPositionSentence(ctx)
      noEventNoteEmissions.incrementAndGet()
      quietStandardSentenceCount.addAndGet(sentenceCount(note))
      note
    }

  def shouldSuppressPlanStatement(ctx: NarrativeContext): Boolean =
    isStandard(ctx) &&
      openingLike(ctx) &&
      ctx.ply <= GuardedOpeningPlyCutoff &&
      !hasMeaningfulOpeningEvent(ctx) &&
      !hasDurableStructuralCommitment(ctx) &&
      !hasEvidenceBackedMainPlan(ctx)

  def hangingTier(
      ctx: NarrativeContext,
      square: Square,
      role: Role,
      attackers: List[Square],
      defenders: List[Square]
  ): HangingTier =
    if !isStandard(ctx) then HangingTier.Red
    else if isEarlyOpeningCentralPawn(ctx, square, role) && !allowsRedTier(ctx) then HangingTier.Suppress
    else if attackers.isEmpty then HangingTier.Suppress
    else if allowsRedTier(ctx) && immediatelyPunishable(role, attackers, defenders) then HangingTier.Red
    else if allowsAmberTier(ctx) && role != Pawn && (defenders.isEmpty || attackers.size > defenders.size) then
      amberTierDowngrade()
      HangingTier.Amber
    else
      redTierSuppressions.incrementAndGet()
      HangingTier.Suppress

  def branchReasonFromFact(ctx: NarrativeContext, fact: Fact): Option[String] =
    fact match
      case Fact.HangingPiece(square, role, attackers, defenders, _) =>
        hangingTier(ctx, square, role, attackers, defenders) match
          case HangingTier.Red =>
            Some(s"It also keeps the ${roleLabel(role)} on ${square.key} from becoming a tactical liability.")
          case HangingTier.Amber =>
            Some(s"It also keeps pressure from building against the ${roleLabel(role)} on ${square.key}.")
          case HangingTier.Suppress => None
      case Fact.Pin(_, _, pinned, pinnedRole, _, _, _, _) =>
        Some(s"It reduces the pin pressure against the ${roleLabel(pinnedRole)} on ${pinned.key}.")
      case Fact.WeakSquare(square, _, _, _) if allowsAmberTier(ctx) || hasDurableStructuralCommitment(ctx) =>
        Some(s"It prevents longer-term weakening around ${square.key}.")
      case _ => None

  def finalizeProse(
      ctx: NarrativeContext,
      rawText: String,
      truthContract: Option[DecisiveTruthContract] = None
  ): String =
    noEventNote(ctx, truthContract).getOrElse(sanitizeClaimStrength(ctx, rawText, truthContract))

  private def sanitizeClaimStrength(
      ctx: NarrativeContext,
      rawText: String,
      truthContract: Option[DecisiveTruthContract]
  ): String =
    if !isStandard(ctx) then Option(rawText).getOrElse("").trim
    else
      val trimmed = Option(rawText).getOrElse("").trim
      if trimmed.isEmpty then trimmed
      else
        var updated = trimmed
        if !allowsRedTier(ctx, truthContract) then
          updated = replaceRegex(updated, "(?i)\\bhanging\\b", "loose")
          updated = replaceRegex(updated, "(?i)\\bunderdefended\\b", "lightly defended")
          updated = replaceLiteralIgnoreCase(updated, "tactical liability", "point of pressure")
          updated = replaceLiteralIgnoreCase(updated, "direct tactical target", "practical point of pressure")
          updated = replaceLiteralIgnoreCase(updated, "requires immediate attention", "deserves attention")
          updated = replaceLiteralIgnoreCase(updated, "priority here", "practical concern here")
          updated = replaceRegex(updated, "(?i)\\burgent\\b", if allowsAmberTier(ctx, truthContract) then "important" else "notable")
        if !allowsAmberTier(ctx, truthContract) then
          updated = replaceLiteralIgnoreCase(updated, "dominant plan", "main idea")
          updated = replaceLiteralIgnoreCase(updated, "direct target", "point of pressure")
          updated = replaceLiteralIgnoreCase(updated, "lasting weakness", "potential weakness")
        updated.trim

  private def quietOpeningNoEvent(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    isStandard(ctx) &&
      openingLike(ctx) &&
      ctx.ply <= EarlyOpeningPlyCutoff &&
      !hasMeaningfulOpeningEvent(ctx) &&
      !hasDurableStructuralCommitment(ctx) &&
      !hasEvidenceBackedMainPlan(ctx) &&
      !hasStrongNarrativeDriver(ctx, truthContract)

  private def quietLaterNoEvent(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    isStandard(ctx) &&
      !openingLike(ctx) &&
      !hasEvidenceBackedMainPlan(ctx) &&
      !hasStrongNarrativeDriver(ctx, truthContract) &&
      !hasDurableStructuralCommitment(ctx) &&
      !hasActiveStrategicSurface(ctx)

  private def hasStrongNarrativeDriver(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    hasForcedOrCriticalState(ctx, truthContract) ||
      hasStrongTacticalPressure(ctx, truthContract) ||
      hasSevereCounterfactual(ctx, truthContract) ||
      ctx.threats.toUs.exists(t => t.lossIfIgnoredCp >= AmberThreatCp || t.kind.toLowerCase.contains("mate"))

  private def hasEvidenceBackedMainPlan(ctx: NarrativeContext): Boolean =
    StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx).nonEmpty

  private def hasActiveStrategicSurface(ctx: NarrativeContext): Boolean =
    ctx.decision.exists { decision =>
      decision.logicSummary.trim.nonEmpty ||
      decision.delta.resolvedThreats.nonEmpty ||
      decision.delta.newOpportunities.nonEmpty ||
      decision.delta.planAdvancements.nonEmpty ||
      decision.delta.concessions.nonEmpty
    } ||
      ctx.opponentPlan.isDefined ||
      ctx.semantic.exists { semantic =>
        semantic.compensation.isDefined ||
          semantic.practicalAssessment.exists(pa => pa.verdict.trim.nonEmpty || pa.biasFactors.nonEmpty) ||
          semantic.preventedPlans.nonEmpty ||
          semantic.structuralWeaknesses.nonEmpty ||
          semantic.positionalFeatures.nonEmpty
      } ||
      ctx.whyAbsentFromTopMultiPV.exists(_.trim.nonEmpty) ||
      ctx.pawnPlay.breakReady ||
      ctx.pawnPlay.counterBreak ||
      !ctx.pawnPlay.primaryDriver.trim.equalsIgnoreCase("quiet") ||
      ctx.pawnPlay.breakImpact.trim.equalsIgnoreCase("High") ||
      ctx.delta.exists(delta =>
        delta.newMotifs.nonEmpty || delta.lostMotifs.nonEmpty || delta.phaseChange.exists(_.trim.nonEmpty)
      ) ||
      ctx.facts.exists {
        case _: Fact.Pin | _: Fact.Fork => true
        case Fact.HangingPiece(square, role, attackers, defenders, _) =>
          hangingTier(ctx, square, role, attackers, defenders) != HangingTier.Suppress
        case _ => false
      }

  private def signalSupportCount(ctx: NarrativeContext): Int =
    List(
      hasMeaningfulOpeningEvent(ctx),
      hasDurableStructuralCommitment(ctx),
      hasEvidenceBackedMainPlan(ctx),
      ctx.threats.toUs.exists(_.lossIfIgnoredCp >= AmberThreatCp),
      ctx.semantic.exists(s => s.structuralWeaknesses.nonEmpty || s.positionalFeatures.nonEmpty || s.compensation.isDefined),
      ctx.opponentPlan.isDefined,
      ctx.decision.isDefined,
      ctx.pawnPlay.breakReady || ctx.pawnPlay.counterBreak,
      ctx.counterfactual.exists(_.cpLoss >= Thresholds.INACCURACY_CP)
    ).count(identity)

  private def openingNoEventSentence(ctx: NarrativeContext): String =
    ctx.openingData.flatMap(_.name).map(_.trim).filter(_.nonEmpty)
      .map(name => s"This is still normal $name development, and no major imbalance has hardened yet.")
      .getOrElse("This is still standard opening development, and no major imbalance has hardened yet.")

  private def quietPositionSentence(ctx: NarrativeContext): String =
    val movePurpose =
      ctx.candidates.headOption
        .map(_.planAlignment.trim)
        .filter(purpose =>
          purpose.nonEmpty &&
            !purpose.equalsIgnoreCase("development") &&
            !purpose.equalsIgnoreCase("quiet")
        )
    movePurpose
      .map(purpose => s"The move keeps the position balanced, and its main job is $purpose rather than a fresh tactical or strategic event.")
      .getOrElse("The move keeps the position balanced, and there is not much to claim beyond steady coordination and normal play.")

  private def immediatelyPunishable(
      role: Role,
      attackers: List[Square],
      defenders: List[Square]
  ): Boolean =
    attackers.nonEmpty &&
      (
        defenders.isEmpty && role != Pawn ||
        attackers.size >= defenders.size + 2 ||
        (role != Pawn && attackers.size > defenders.size)
      )

  private def isEarlyOpeningCentralPawn(ctx: NarrativeContext, square: Square, role: Role): Boolean =
    role == Pawn &&
      isStandard(ctx) &&
      openingLike(ctx) &&
      ctx.ply <= GuardedOpeningPlyCutoff &&
      CentralOpeningPawnSquares.contains(square.key.toLowerCase)

  private def roleLabel(role: Role): String =
    role.toString.toLowerCase

  private def sentenceCount(text: String): Long =
    Option(text)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.split("(?<=[.!?])\\s+").count(_.trim.nonEmpty).toLong)
      .getOrElse(0L)

  private def amberTierDowngrade(): Unit =
    amberTierDowngrades.incrementAndGet()

  private def replaceLiteralIgnoreCase(text: String, needle: String, replacement: String): String =
    val pattern = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(needle), java.util.regex.Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(text)
    if matcher.find() then
      redTierSuppressions.incrementAndGet()
      matcher.replaceAll(java.util.regex.Matcher.quoteReplacement(replacement))
    else text

  private def replaceRegex(text: String, regex: String, replacement: String): String =
    val pattern = java.util.regex.Pattern.compile(regex)
    val matcher = pattern.matcher(text)
    if matcher.find() then
      redTierSuppressions.incrementAndGet()
      matcher.replaceAll(java.util.regex.Matcher.quoteReplacement(replacement))
    else text
