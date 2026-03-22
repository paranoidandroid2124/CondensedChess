package lila.llm.analysis

import lila.llm.{ BookmakerRefsV1, StrategyPack }
import lila.llm.model.*
import lila.llm.model.authoring.{ NarrativeOutline, OutlineBeatKind }

private[llm] object BookmakerLiveCompressionPolicy:

  private enum ClaimFamily:
    case Annotation
    case Decision
    case Prophylaxis
    case Strategic
    case Practical
    case Fallback

  private final case class ClaimSelection(
      family: ClaimFamily,
      lens: StrategicLens,
      claim: String,
      support: List[String]
  )

  private val movePurposeMarkers = List(
    "keeps",
    "extends",
    "supports",
    "prepares",
    "starts",
    "reroutes",
    "cuts",
    "invests",
    "builds",
    "organizes",
    "reorganizes",
    "opens",
    "chooses",
    "claims",
    "simplifies"
  )

  def systemLanguageBanList: List[String] = LiveNarrativeCompressionCore.systemLanguageBanList

  def systemLanguageHits(raw: String): List[String] =
    LiveNarrativeCompressionCore.systemLanguageHits(raw)

  def buildSlotsOrFallback(
      ctx: NarrativeContext,
      outline: NarrativeOutline,
      refs: Option[BookmakerRefsV1],
      strategyPack: Option[StrategyPack]
  ): BookmakerPolishSlots =
    buildSlots(ctx, outline, refs, strategyPack).getOrElse(compactFallbackSlots(ctx, outline))

  private[analysis] def cleanNarrativeSentence(raw: String, ctx: NarrativeContext): Option[String] =
    cleanSentence(raw, ctx)

  def buildSlots(
      ctx: NarrativeContext,
      outline: NarrativeOutline,
      refs: Option[BookmakerRefsV1],
      strategyPack: Option[StrategyPack]
  ): Option[BookmakerPolishSlots] =
    quietStandardOpeningClaim(ctx)
      .orElse(StandardCommentaryClaimPolicy.noEventNote(ctx))
      .flatMap(cleanSentence(_, ctx).orElse(quietStandardOpeningClaim(ctx)).orElse(StandardCommentaryClaimPolicy.noEventNote(ctx)))
      .map { quietClaim =>
        BookmakerPolishSlots(
          lens = StrategicLens.Decision,
          claim = prefixMoveHeader(ctx, quietClaim),
          supportPrimary = None,
          supportSecondary = None,
          tension = None,
          evidenceHook = None,
          coda = None,
          factGuardrails = Nil,
          paragraphPlan = List("p1=claim")
        )
      }
      .orElse {
        val compactOpening = EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx)
        val thesis = StrategicThesisBuilder.build(ctx, strategyPack)
        val surface = StrategyPackSurface.from(strategyPack)
        val mainMoveBeat = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).filter(_.nonEmpty)
        val contextBeat = outline.beats.find(_.kind == OutlineBeatKind.Context).map(_.text).filter(_.nonEmpty)
        val wrapBeat = outline.beats.find(_.kind == OutlineBeatKind.WrapUp).map(_.text).filter(_.nonEmpty)
        val mainMoveSentences =
          splitSentences(mainMoveBeat.getOrElse(""))
            .flatMap(cleanSentence(_, ctx))
        val annotationClaim = annotationOverride(ctx, mainMoveSentences, surface)
        val forceAnnotationLead = shouldLeadWithAnnotation(ctx, annotationClaim)

        val claimSelection =
          if forceAnnotationLead then
            annotationChoice(annotationClaim, ctx)
              .orElse(prophylaxisChoice(thesis, ctx))
              .orElse(strategicChoice(thesis, ctx))
              .orElse(decisionChoice(thesis, ctx))
              .orElse(practicalChoice(thesis, ctx))
              .orElse(fallbackChoice(mainMoveSentences, contextBeat, ctx))
          else
            prophylaxisChoice(thesis, ctx)
              .orElse(strategicChoice(thesis, ctx))
              .orElse(decisionChoice(thesis, ctx))
              .orElse(practicalChoice(thesis, ctx))
              .orElse(annotationChoice(annotationClaim, ctx))
              .orElse(fallbackChoice(mainMoveSentences, contextBeat, ctx))

        claimSelection.map { selection =>
          val supportPrimary =
            Option.unless(compactOpening) {
              selection.support.headOption
                .orElse(fallbackSupport(ctx, selection.family))
            }.flatten
          val thirdChoice =
            Option.unless(compactOpening) {
              chooseThirdParagraph(
                ctx = ctx,
                selection = selection,
                supportRemainder = selection.support.drop(1),
                refs = refs
              )
            }.flatten
          val coda =
            Option.unless(compactOpening) {
              chooseCoda(ctx, wrapBeat, thirdChoice)
            }.flatten
          val concreteSupportTension =
            selection.support.drop(1).find(text =>
              (
                LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
                  text.toLowerCase.contains("decision")
              ) &&
                !text.toLowerCase.contains("stays secondary because")
            )
          val (rawTension, evidenceHook) =
            thirdChoice match
              case Some(text) if LineScopedCitation.hasConcreteSanLine(text) => (None, Some(text))
              case Some(text)                                                => (Some(text), None)
              case None                                                      => (None, None)
          val tension =
            rawTension.filter(text =>
              (
                LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
                  text.toLowerCase.contains("decision")
              ) &&
                !text.toLowerCase.contains("stays secondary because")
            ).orElse(concreteSupportTension)
              .orElse(
                supportPrimary.filter(text =>
                  LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
                    text.toLowerCase.contains("decision")
                )
              )
              .orElse(rawTension)
          val paragraphPlan =
            if compactOpening then List("p1=claim")
            else
              List(
                Some("p1=claim"),
                Option.when(supportPrimary.nonEmpty)("p2=support"),
                evidenceHook.map(_ => "p3=cited_line")
                  .orElse(tension.map(_ => "p3=practical_nuance"))
                  .orElse(coda.map(_ => "p3=coda"))
              ).flatten
          BookmakerPolishSlots(
            lens = selection.lens,
            claim = prefixMoveHeader(ctx, selection.claim),
            supportPrimary = supportPrimary,
            supportSecondary = None,
            tension = tension.filter(_.nonEmpty),
            evidenceHook = evidenceHook.filter(_.nonEmpty),
            coda = coda.filter(_.nonEmpty),
            factGuardrails = List(evidenceHook).flatten,
            paragraphPlan = paragraphPlan
          )
        }
      }

  private def quietStandardOpeningClaim(ctx: NarrativeContext): Option[String] =
    StandardCommentaryClaimPolicy.noEventNote(ctx)
      .filter(_ =>
        StandardCommentaryClaimPolicy.openingLike(ctx) &&
          ctx.ply <= 16
      )

  private def annotationChoice(
      annotationOverride: Option[(String, List[String])],
      ctx: NarrativeContext
  ): Option[ClaimSelection] =
    annotationOverride.flatMap { case (claim, support) =>
      cleanSentence(claim, ctx).map { safeClaim =>
        refineSelection(
          ClaimSelection(
          family = ClaimFamily.Annotation,
          lens = StrategicLens.Decision,
          claim = safeClaim,
          support = support.flatMap(cleanSentence(_, ctx)).take(1)
          )
        )
      }
    }

  private def decisionChoice(
      thesis: Option[StrategicThesis],
      ctx: NarrativeContext
  ): Option[ClaimSelection] =
    thesis.filter(_.lens == StrategicLens.Decision).flatMap { candidate =>
      cleanSentence(candidate.claim, ctx).map { safeClaim =>
        refineSelection(
          ClaimSelection(
          family = ClaimFamily.Decision,
          lens = candidate.lens,
          claim = safeClaim,
          support = candidate.support.flatMap(cleanSentence(_, ctx)).take(3)
          )
        )
      }
    }

  private def prophylaxisChoice(
      thesis: Option[StrategicThesis],
      ctx: NarrativeContext
  ): Option[ClaimSelection] =
    val currentBoardPrevented =
      ctx.semantic.toList.flatMap(_.preventedPlans).find(_.sourceScope == FactScope.Now)
    thesis
      .filter(_.lens == StrategicLens.Prophylaxis)
      .filter(_ => currentBoardPrevented.nonEmpty)
      .flatMap { candidate =>
        cleanSentence(candidate.claim, ctx).map { safeClaim =>
          refineSelection(
            ClaimSelection(
            family = ClaimFamily.Prophylaxis,
            lens = candidate.lens,
            claim = safeClaim,
            support = candidate.support.flatMap(cleanSentence(_, ctx)).take(2)
            )
          )
        }
      }

  private def strategicChoice(
      thesis: Option[StrategicThesis],
      ctx: NarrativeContext
  ): Option[ClaimSelection] =
    thesis
      .filter(t => Set(StrategicLens.Structure, StrategicLens.Compensation, StrategicLens.Opening).contains(t.lens))
      .flatMap { candidate =>
        cleanSentence(candidate.claim, ctx)
          .filter(text => candidate.lens == StrategicLens.Structure || namedPlanAllowed(text, ctx))
          .map { safeClaim =>
          refineSelection(
            ClaimSelection(
            family = ClaimFamily.Strategic,
            lens = candidate.lens,
            claim = safeClaim,
            support = candidate.support.flatMap(cleanSentence(_, ctx)).take(2)
            )
          )
          }
      }

  private def practicalChoice(
      thesis: Option[StrategicThesis],
      ctx: NarrativeContext
  ): Option[ClaimSelection] =
    thesis.filter(_.lens == StrategicLens.Practical).flatMap { candidate =>
      cleanSentence(candidate.claim, ctx).map { safeClaim =>
        refineSelection(
          ClaimSelection(
          family = ClaimFamily.Practical,
          lens = candidate.lens,
          claim = safeClaim,
          support = candidate.support.flatMap(cleanSentence(_, ctx)).take(2)
          )
        )
      }
    }

  private def fallbackChoice(
      mainMoveSentences: List[String],
      contextBeat: Option[String],
      ctx: NarrativeContext
  ): Option[ClaimSelection] =
    mainMoveSentences.headOption
      .flatMap(cleanSentence(_, ctx))
      .orElse(contextBeat.flatMap(cleanSentence(_, ctx)))
      .map { safeClaim =>
        refineSelection(
          ClaimSelection(
          family = ClaimFamily.Fallback,
          lens = StrategicLens.Decision,
          claim = safeClaim,
          support = mainMoveSentences.drop(1).flatMap(cleanSentence(_, ctx)).take(1)
          )
        )
      }

  private def fallbackSupport(
      ctx: NarrativeContext,
      family: ClaimFamily
  ): Option[String] =
    family match
      case ClaimFamily.Decision =>
        ctx.decision
          .map(_.logicSummary)
          .flatMap(renderDecisionLogic)
          .flatMap(cleanSentence(_, ctx))
      case ClaimFamily.Prophylaxis =>
        ctx.semantic.toList
          .flatMap(_.preventedPlans)
          .find(_.sourceScope == FactScope.Now)
          .flatMap(renderCurrentBoardProphylaxisSupport)
          .flatMap(cleanSentence(_, ctx))
      case ClaimFamily.Practical =>
        ctx.semantic.flatMap(_.practicalAssessment)
          .flatMap(renderPracticalSupport)
          .flatMap(cleanSentence(_, ctx))
      case _ => None

  private def chooseThirdParagraph(
      ctx: NarrativeContext,
      selection: ClaimSelection,
      supportRemainder: List[String],
      refs: Option[BookmakerRefsV1]
  ): Option[String] =
    val citedLine =
      variationGuardrail(refs)
        .flatMap(cleanSentence(_, ctx))
        .filter(_ => shouldUseCitedLine(ctx, selection.family))
    val preferredNuance =
      Option.when(citedLine.isEmpty) {
        supportRemainder
          .flatMap(cleanSentence(_, ctx))
          .find(text =>
            LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
              text.toLowerCase.contains("decision")
          )
      }.flatten
    val alternative =
      Option.when(citedLine.isEmpty && preferredNuance.isEmpty)(candidateAlternative(ctx)).flatten
    val nuance =
      Option.when(citedLine.isEmpty && alternative.isEmpty) {
        supportRemainder.headOption.flatMap(cleanSentence(_, ctx))
      }.flatten
    citedLine.orElse(preferredNuance).orElse(alternative).orElse(nuance)

  private def chooseCoda(
      ctx: NarrativeContext,
      wrapBeat: Option[String],
      thirdChoice: Option[String]
  ): Option[String] =
    Option.when(thirdChoice.isEmpty && ctx.phase.current.equalsIgnoreCase("Endgame")) {
      wrapBeat.flatMap(cleanSentence(_, ctx))
    }.flatten

  private def shouldUseCitedLine(ctx: NarrativeContext, family: ClaimFamily): Boolean =
    val tacticallyTense =
      ctx.header.criticality.equalsIgnoreCase("critical") ||
        ctx.header.criticality.equalsIgnoreCase("forced") ||
        ctx.header.choiceType.equalsIgnoreCase("OnlyMove") ||
        ctx.counterfactual.nonEmpty ||
        ctx.meta.flatMap(_.errorClass).exists(_.isTactical)
    tacticallyTense && family != ClaimFamily.Practical

  private def renderDecisionLogic(raw: String): Option[String] =
    normalized(raw).map { text =>
      val normalizedArrows =
        text
          .replace("->", " then ")
          .replace(">", " then ")
          .replaceAll("""\s+""", " ")
      if normalizedArrows.toLowerCase.startsWith("the move") then normalizedArrows
      else s"The move first $normalizedArrows."
    }

  private def renderCurrentBoardProphylaxisSupport(prevented: PreventedPlanInfo): Option[String] =
    val breakSentence =
      prevented.breakNeutralized
        .flatMap(normalized)
        .map(file => s"It keeps ...${file}5 from coming right away.")
    val squareSentence =
      prevented.deniedSquares.headOption
        .flatMap(normalized)
        .map(square => s"It keeps the opponent out of $square for the moment.")
    val planSentence =
      normalized(prevented.planId)
        .filterNot(_.equalsIgnoreCase("counterplay"))
        .map(plan => s"It slows down $plan before it gets started.")
    breakSentence.orElse(squareSentence).orElse(planSentence)

  private def renderPracticalSupport(practical: PracticalInfo): Option[String] =
    val drivers =
      practical.biasFactors
        .sortBy(b => -Math.abs(b.weight))
        .take(2)
        .flatMap { bias =>
          for
            factor <- normalized(bias.factor)
            desc <- normalized(bias.description)
            rendered <- LiveNarrativeCompressionCore.renderPracticalBiasPlayer(factor, desc)
          yield rendered
        }
    drivers match
      case Nil =>
        normalized(practical.verdict).map(_ => "That version is easier to handle over the board.")
      case many =>
        Some(s"That is easier because ${many.mkString(" and ")}.")

  private def candidateAlternative(ctx: NarrativeContext): Option[String] =
    ctx.candidates.drop(1).find(_.whyNot.exists(_.trim.nonEmpty)).flatMap { candidate =>
      val move = normalized(candidate.move).getOrElse("the alternative")
      candidate.whyNot
        .flatMap(normalized)
        .map(reason => s"The alternative $move stays secondary because $reason.")
        .flatMap(cleanSentence(_, ctx))
    }

  private def annotationOverride(
      ctx: NarrativeContext,
      mainMoveSentences: List[String],
      surface: StrategyPackSurface.Snapshot
  ): Option[(String, List[String])] =
    Option.when(
      CriticalAnnotationPolicy.shouldPrioritizeClaim(ctx) &&
        mainMoveSentences.nonEmpty &&
        !shouldPreserveStrategicThesis(surface, mainMoveSentences.head)
    ) {
      val claim = mainMoveSentences.head
      val support = mainMoveSentences.drop(1)
      (claim, support)
    }

  private def shouldPreserveStrategicThesis(
      surface: StrategyPackSurface.Snapshot,
      candidateClaim: String
  ): Boolean =
    hasStrategicClaimSurface(surface) ||
      startsWithGenericDecisionScaffold(candidateClaim)

  private def shouldLeadWithAnnotation(
      ctx: NarrativeContext,
      annotationClaim: Option[(String, List[String])]
  ): Boolean =
    annotationClaim.exists { case (claim, _) =>
      val low = Option(claim).getOrElse("").trim.toLowerCase
      CriticalAnnotationPolicy.shouldPrioritizeClaim(ctx) &&
      (
        low.contains("blunder") ||
          low.contains("mistake") ||
          low.contains("tactical") ||
          low.contains("by force") ||
          low.contains("misses") ||
          low.contains("fork")
      )
    }

  private def hasStrategicClaimSurface(surface: StrategyPackSurface.Snapshot): Boolean =
    surface.dominantIdeaText.nonEmpty ||
      surface.executionText.nonEmpty ||
      surface.objectiveText.nonEmpty ||
      surface.compensationPosition ||
      surface.investedMaterial.exists(_ > 0)

  private def startsWithGenericDecisionScaffold(text: String): Boolean =
    val low = Option(text).getOrElse("").trim.toLowerCase
    low.startsWith("the whole decision turns on") ||
      low.startsWith("a central square in the decision is") ||
      low.startsWith("the key decision is to choose")

  private def cleanSentence(raw: String, ctx: NarrativeContext): Option[String] =
    normalized(raw)
      .map(BookmakerSlotSanitizer.sanitizeUserText)
      .map(rewritePlayerLanguage)
      .flatMap(normalized)
      .map(trimLeadScaffold)
      .flatMap(normalized)
      .filter(text => systemLanguageHits(text).isEmpty)
      .filter(text => LiveNarrativeCompressionCore.playerLanguageHits(text).isEmpty)
      .filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence)
      .filterNot(LiveNarrativeCompressionCore.isLowValueNarrativeSentence)
      .filter(text => !containsNonBackedPlanName(text, ctx))
      .filter(text => namedPlanAllowed(text, ctx))
      .filterNot(_.equalsIgnoreCase("Concrete support is still limited."))

  private def rewritePlayerLanguage(raw: String): String =
    LiveNarrativeCompressionCore.rewritePlayerLanguage(raw)

  private def trimLeadScaffold(raw: String): String =
    LiveNarrativeCompressionCore.trimLeadScaffold(raw)

  private def containsNonBackedPlanName(text: String, ctx: NarrativeContext): Boolean =
    val low = text.toLowerCase
    val backed = StrategicNarrativePlanSupport.evidenceBackedPlanNames(ctx).flatMap(normalized).toSet
    val otherPlanNames =
      (
        ctx.mainStrategicPlans.map(_.planName) ++
          ctx.plans.top5.map(_.name) ++
          ctx.latentPlans.map(_.planName)
      ).flatMap(normalized).distinct
    otherPlanNames.exists(name =>
      !backed.contains(name) &&
        name.split("\\s+").length >= 2 &&
        low.contains(name)
    )

  private def namedPlanAllowed(text: String, ctx: NarrativeContext): Boolean =
    val low = text.toLowerCase
    val backed = StrategicNarrativePlanSupport.evidenceBackedPlanNames(ctx).flatMap(normalized).distinct
    val containsBackedPlan = backed.exists(low.contains)
    !containsNonBackedPlanName(text, ctx) &&
      (!containsBackedPlan || movePurposeMarkers.exists(low.contains))

  private def refineSelection(selection: ClaimSelection): ClaimSelection =
    promoteAnchoredSupport(selection)

  private def promoteAnchoredSupport(selection: ClaimSelection): ClaimSelection =
    val promoted =
      Option.when(claimNeedsPromotion(selection.claim)) {
        selection.support.find(isStrategicSupportCandidate)
      }.flatten
    promoted match
      case Some(betterClaim) =>
        val keepOldClaim =
          Option.when(
            !sameSentence(selection.claim, betterClaim) &&
              !LiveNarrativeCompressionCore.isLowValueNarrativeSentence(selection.claim) &&
              (
                LiveNarrativeCompressionCore.hasConcreteAnchor(selection.claim) ||
                  selection.claim.toLowerCase.contains("secondary") ||
                  selection.claim.toLowerCase.contains("works only")
              )
          )(selection.claim)
        selection.copy(
          claim = betterClaim,
          support = (keepOldClaim.toList ++ selection.support.filterNot(sameSentence(_, betterClaim))).distinct
        )
      case None =>
        selection

  private def claimNeedsPromotion(text: String): Boolean =
    val low = Option(text).getOrElse("").trim.toLowerCase
    LiveNarrativeCompressionCore.isLowValueNarrativeSentence(text) ||
      low.startsWith("the move is mainly about bringing the ") ||
      low.startsWith("the move follows the structure") ||
      low.startsWith("the move improves the piece placement") ||
      low.contains("route toward") ||
      low.contains("headed for") ||
      low.contains("available.")

  private def isStrategicSupportCandidate(text: String): Boolean =
    val low = Option(text).getOrElse("").trim.toLowerCase
    (LiveNarrativeCompressionCore.hasConcreteAnchor(text) || low.contains("works only")) &&
      (
        low.contains("pressure") ||
          low.contains("target") ||
          low.contains("break") ||
          low.contains("counterplay") ||
          low.contains("exchange") ||
          low.contains("file") ||
          low.contains("stays secondary") ||
          low.contains("works only") ||
          low.contains("decision")
      )

  private def splitSentences(text: String): List[String] =
    Option(text)
      .getOrElse("")
      .split("""(?<=[.!?])\s+""")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

  private def prefixMoveHeader(ctx: NarrativeContext, claim: String): String =
    if Option(claim).exists(_.matches("""^\d+\.(?:\.\.)?\s+[^:]+:\s*.*""")) then claim
    else
      val moveHeader =
        for
          san <- ctx.playedSan.filter(_.trim.nonEmpty)
        yield
          val moveNum = (ctx.ply + 1) / 2
          val prefix = if ctx.ply % 2 == 1 then s"$moveNum." else s"$moveNum..."
          s"$prefix $san:"
      moveHeader.map(h => s"$h $claim").getOrElse(claim)

  private def compactFallbackSlots(
      ctx: NarrativeContext,
      outline: NarrativeOutline
  ): BookmakerPolishSlots =
    val mainMoveBeat = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).filter(_.nonEmpty)
    val contextBeat = outline.beats.find(_.kind == OutlineBeatKind.Context).map(_.text).filter(_.nonEmpty)
    val fallbackSentences =
      (mainMoveBeat.toList ++ contextBeat.toList)
        .flatMap(splitSentences)
        .flatMap(cleanSentence(_, ctx))
    val claim =
      fallbackSentences.headOption
        .orElse(fallbackSupport(ctx, ClaimFamily.Decision))
        .orElse(fallbackMovePurpose(ctx))
        .orElse(quietStandardOpeningClaim(ctx).flatMap(cleanSentence(_, ctx)))
        .orElse(StandardCommentaryClaimPolicy.noEventNote(ctx).flatMap(cleanSentence(_, ctx)))
        .getOrElse("The move improves the position without forcing a sharp change yet.")
    val support =
      fallbackSentences.drop(1).headOption
        .orElse(fallbackSupport(ctx, ClaimFamily.Decision))
    BookmakerPolishSlots(
      lens = StrategicLens.Decision,
      claim = prefixMoveHeader(ctx, claim),
      supportPrimary = support,
      supportSecondary = None,
      tension = None,
      evidenceHook = None,
      coda = None,
      factGuardrails = Nil,
      paragraphPlan = List(Some("p1=claim"), support.map(_ => "p2=support")).flatten
    )

  private def variationGuardrail(refs: Option[BookmakerRefsV1]): Option[String] =
    refs.flatMap(_.variations.headOption).flatMap { variation =>
      val preview =
        variation.moves
          .take(3)
          .map(_.san.trim)
          .filter(_.nonEmpty)
          .mkString(" ")
          .trim
      Option.when(preview.nonEmpty) {
        val eval = formatVariationScore(variation.scoreCp, variation.mate)
        s"One concrete line that keeps the idea in play is a) $preview$eval."
      }
    }

  private def formatVariationScore(scoreCp: Int, mate: Option[Int]): String =
    mate match
      case Some(m) if m > 0 => s" (mate in $m)"
      case Some(m) if m < 0 => s" (mated in ${Math.abs(m)})"
      case Some(_)          => ""
      case None =>
        val sign = if scoreCp >= 0 then "+" else ""
        f" ($sign${scoreCp.toDouble / 100}%.1f)"

  private def normalized(raw: String): Option[String] =
    Option(raw)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replaceAll("""\s+""", " ").trim)

  private def sameSentence(left: String, right: String): Boolean =
    val normalizedLeft = normalized(left).map(_.toLowerCase).getOrElse("")
    val normalizedRight = normalized(right).map(_.toLowerCase).getOrElse("")
    normalizedLeft.nonEmpty && normalizedLeft == normalizedRight

  private def fallbackMovePurpose(ctx: NarrativeContext): Option[String] =
    ctx.playedSan.flatMap(normalized).flatMap { san =>
      val cleanSan = san.replaceAll("""[!?+#]+$""", "")
      if cleanSan == "O-O" then Some("It brings the king to safety and connects the rooks.")
      else if cleanSan == "O-O-O" then Some("It puts the king on the queenside and brings the rook into play.")
      else
        val targetSquare = """([a-h][1-8])""".r.findAllMatchIn(cleanSan).map(_.group(1)).toList.lastOption
        val piece =
          cleanSan.headOption.collect {
            case 'K' => "king"
            case 'Q' => "queen"
            case 'R' => "rook"
            case 'B' => "bishop"
            case 'N' => "knight"
          }.getOrElse("pawn")
        if cleanSan.contains("x") then
          targetSquare.map { square =>
            if piece == "pawn" then s"It clarifies the position with the capture on $square."
            else s"It chooses the capture on $square and improves the $piece at the same time."
          }
        else
          targetSquare.map { square =>
            if piece == "pawn" then s"It uses the pawn move to support $square and keep the structure flexible."
            else s"It improves the $piece by bringing it to $square."
          }
    }.flatMap(cleanSentence(_, ctx))
