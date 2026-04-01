package lila.llm.analysis

import lila.llm.*
import lila.llm.model.NarrativeContext
import lila.llm.model.authoring.PlanHypothesis

private[llm] enum CertifiedDecisionFrameAxis:
  case Intent
  case Battlefront
  case Urgency

private[llm] enum CertifiedUrgencyClass:
  case Immediate
  case Pressing
  case Slow

private[llm] enum BattlefrontAnchorClass:
  case None
  case MoveLocal
  case ProbeBacked

private[llm] final case class CertifiedDecisionSupport(
    axis: CertifiedDecisionFrameAxis,
    sentence: String,
    priority: Int,
    sourceKind: String
)

private[llm] final case class CertifiedDecisionFrame(
    intent: Option[CertifiedDecisionSupport] = None,
    battlefront: Option[CertifiedDecisionSupport] = None,
    urgency: Option[CertifiedDecisionSupport] = None,
    ownerSide: Option[String] = None,
    alignmentKeys: Set[String] = Set.empty,
    carrierAlignmentKeys: Set[String] = Set.empty
):
  def orderedSupports(max: Int = 2): List[String] =
    List(intent, battlefront, urgency).flatten.sortBy(signal => -signal.priority).map(_.sentence).distinct.take(max)

  def ideaRefs(max: Int = 2): List[ActiveStrategicIdeaRef] =
    ownerSide.toList.flatMap { side =>
      List(intent, battlefront).flatten
        .sortBy(signal => -signal.priority)
        .take(max)
        .zipWithIndex
        .map { case (support, idx) =>
          ActiveStrategicIdeaRef(
            ideaId = s"decision_frame_${support.axis.toString.toLowerCase}_${idx + 1}",
            ownerSide = side,
            kind = (
              support.axis match
                case CertifiedDecisionFrameAxis.Intent      => "intent"
                case CertifiedDecisionFrameAxis.Battlefront => "battlefront"
                case CertifiedDecisionFrameAxis.Urgency     => "urgency"
            ),
            group = "decision_frame",
            readiness = "certified",
            focusSummary = CertifiedDecisionFrameBuilder.ideaFocusSummary(support, side),
            confidence = (support.priority.toDouble / 100.0).max(0.0).min(1.0)
          )
        }
    }

  def hasCarrierAlignment: Boolean = ownerSide.nonEmpty && carrierAlignmentKeys.nonEmpty

  def alignedRouteRefs(routeRefs: List[ActiveStrategicRouteRef]): List[ActiveStrategicRouteRef] =
    if !hasCarrierAlignment then Nil
    else
      routeRefs.filter { ref =>
        CertifiedDecisionFrameBuilder.sameSide(ref.ownerSide, ownerSide.get) &&
          (
            ref.route.flatMap(CertifiedDecisionFrameBuilder.squareAlignmentKeys).exists(carrierAlignmentKeys.contains) ||
              CertifiedDecisionFrameBuilder.textAlignmentKeys(ref.purpose).exists(carrierAlignmentKeys.contains)
          )
      }

  def alignedMoveRefs(moveRefs: List[ActiveStrategicMoveRef]): List[ActiveStrategicMoveRef] =
    if !hasCarrierAlignment then Nil
    else
      moveRefs.filter { ref =>
        CertifiedDecisionFrameBuilder.moveAlignmentKeys(ref).exists(carrierAlignmentKeys.contains)
      }

  def alignedTargets(targets: List[StrategyDirectionalTarget]): List[StrategyDirectionalTarget] =
    if !hasCarrierAlignment then Nil
    else
      targets.filter { target =>
        ownerSide.forall(side => CertifiedDecisionFrameBuilder.sameSide(target.ownerSide, side)) &&
          (
            CertifiedDecisionFrameBuilder.squareAlignmentKeys(target.targetSquare).exists(carrierAlignmentKeys.contains) ||
              CertifiedDecisionFrameBuilder.textAlignmentKeys(target.strategicReasons.mkString(" ")).exists(carrierAlignmentKeys.contains)
          )
      }

  def alignedDossier(dossier: Option[ActiveBranchDossier]): Option[ActiveBranchDossier] =
    if !hasCarrierAlignment then None
    else
      dossier.filter { value =>
        value.routeCue.exists { cue =>
          ownerSide.forall(side => CertifiedDecisionFrameBuilder.sameSide(cue.ownerSide, side)) &&
          (
            cue.route.flatMap(CertifiedDecisionFrameBuilder.squareAlignmentKeys).exists(carrierAlignmentKeys.contains) ||
              CertifiedDecisionFrameBuilder.textAlignmentKeys(cue.purpose).exists(carrierAlignmentKeys.contains)
          )
        } ||
        value.moveCue.exists(cue => CertifiedDecisionFrameBuilder.moveAlignmentKeys(cue).exists(carrierAlignmentKeys.contains)) ||
        List(
          value.evidenceCue,
          value.whyChosen,
          value.opponentResource,
          value.continuationFocus,
          value.practicalRisk
        ).flatten.exists(text => CertifiedDecisionFrameBuilder.textAlignmentKeys(text).exists(carrierAlignmentKeys.contains))
      }

private[llm] object CertifiedDecisionFrameBuilder:

  private final case class BattlefrontCarrier(
      key: String,
      phrase: String,
      sourceKind: String,
      independenceKey: String,
      anchorClass: BattlefrontAnchorClass
  )

  private val SquarePattern = """\b([a-h][1-8])\b""".r
  private val FilePattern = """\b([a-h])(?:-|\s*)file\b""".r
  private val ZonePattern = """\b(kingside|queenside|center|central)\b""".r
  private val ComplexPattern = """\b(light|dark)(?:[\s-]+square(?:s|d)?)\b""".r
  private val UciPattern = """^[a-h][1-8][a-h][1-8][qrbn]?$""".r
  private val GenericShellTokens = Set(
    "strategic campaign",
    "the key idea is",
    "a likely follow-up is",
    "a concrete target is",
    "further probe work",
    "latent plan",
    "pv coupled",
    "deferred"
  )

  def build(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      mainBundle: Option[MainPathClaimBundle] = None,
      quietIntent: Option[QuietMoveIntentClaim] = None
  ): CertifiedDecisionFrame =
    val surface = StrategyPackSurface.from(strategyPack)
    val ownerSide = surface.campaignOwner.orElse(surface.sideToMove).orElse(sideFromPly(ctx.ply))
    val evidenceBackedPlans = StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx)
    val evidencePlanKeys = planAlignmentKeys(evidenceBackedPlans)
    val admittedKeys = admittedAlignmentKeys(mainBundle, quietIntent)
    val intent =
      buildIntent(
        ownerSide = ownerSide,
        evidenceBackedPlans = evidenceBackedPlans,
        evidencePlanKeys = evidencePlanKeys,
        admittedKeys = admittedKeys,
        targetOpt = surface.topDirectionalTarget,
        routeOpt = surface.topRoute,
        moveRefOpt = surface.topMoveRef
      )
    val battlefront =
      buildBattlefront(
        evidenceBackedPlans = evidenceBackedPlans,
        admittedKeys = admittedKeys,
        carriers =
          claimBattlefrontCarriers(mainBundle, quietIntent) ++
            planBattlefrontCarriers(evidenceBackedPlans) ++
            surfaceBattlefrontCarriers(surface)
      )
    val urgency =
      buildUrgency(
        mode = PlayerFacingTruthModePolicy.classify(ctx, strategyPack, truthContract),
        criticality = Some(ctx.header.criticality),
        choiceType = Some(ctx.header.choiceType),
        tensionScore =
          Some(
            (ctx.threats.toUs ++ ctx.threats.toThem)
              .map(_.lossIfIgnoredCp)
              .filter(_ > 0)
              .maxOption
              .getOrElse(0)
          )
      )
    buildFrame(ownerSide, intent, battlefront, urgency, admittedKeys)

  def build(
      moment: GameChronicleMoment,
      deltaBundle: PlayerFacingMoveDeltaBundle,
      dossier: Option[ActiveBranchDossier]
  ): CertifiedDecisionFrame =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val ownerSide = surface.campaignOwner.orElse(surface.sideToMove).orElse(Some(normalized(moment.side)))
    val evidenceBackedPlans = evidenceBackedMainPlans(moment)
    val evidencePlanKeys = planAlignmentKeys(evidenceBackedPlans)
    val admittedKeys =
      deltaBundle.claims.flatMap(claim => claim.anchorText :: claim.evidenceLines).flatMap(textAlignmentKeys).toSet ++
        deltaBundle.visibleDirectionalTargets.filter(target =>
          ownerSide.forall(side => sameSide(target.ownerSide, side))
        ).flatMap(target =>
          squareAlignmentKeys(target.targetSquare) ++ textAlignmentKeys(target.strategicReasons.mkString(" "))
        ) ++
        deltaBundle.visibleRouteRefs.filter(route =>
          ownerSide.forall(side => sameSide(route.ownerSide, side))
        ).flatMap(route =>
          route.route.lastOption.toList.flatMap(squareAlignmentKeys) ++ textAlignmentKeys(route.purpose)
        ) ++
        deltaBundle.visibleMoveRefs.flatMap(moveAlignmentKeys)
    val themeSurface =
      ActiveThemeSurfaceBuilder.build(moment).filter(ActiveThemeSurfaceBuilder.isCanonical)
    val intent =
      buildIntent(
        ownerSide = ownerSide,
        evidenceBackedPlans = evidenceBackedPlans,
        evidencePlanKeys = evidencePlanKeys,
        admittedKeys = admittedKeys,
        targetOpt = surface.topDirectionalTarget.orElse(deltaBundle.visibleDirectionalTargets.headOption),
        routeOpt =
          surface.topRoute.orElse(
            dossier.flatMap(_.routeCue).map(cue =>
              StrategyPieceRoute(
                ownerSide = cue.ownerSide,
                piece = cue.piece,
                from = cue.route.headOption.getOrElse(""),
                route = cue.route,
                purpose = cue.purpose,
                strategicFit = cue.strategicFit,
                tacticalSafety = cue.tacticalSafety,
                surfaceConfidence = cue.surfaceConfidence,
                surfaceMode = cue.surfaceMode
              )
            )
          ),
        moveRefOpt = surface.topMoveRef
      )
    val battlefront =
      buildBattlefront(
        evidenceBackedPlans = evidenceBackedPlans,
        admittedKeys = admittedKeys,
        carriers =
          deltaBundleBattlefrontCarriers(deltaBundle, ownerSide) ++
            dossierBattlefrontCarriers(dossier, ownerSide) ++
            planBattlefrontCarriers(evidenceBackedPlans) ++
            surfaceBattlefrontCarriers(surface) ++
            themeBattlefrontCarriers(themeSurface)
      )
    val urgency =
      buildUrgency(
        mode = PlayerFacingTruthModePolicy.classify(moment),
        criticality = None,
        choiceType = None,
        tensionScore =
          Some(
            moment.signalDigest.flatMap(_.counterplayScoreDrop).getOrElse(
              moment.signalDigest.flatMap(_.decisionComparison).flatMap(_.cpLossVsChosen)
                .orElse(moment.topEngineMove.flatMap(_.cpLossVsPlayed))
                .getOrElse(0)
            )
          )
      )
    buildFrame(ownerSide, intent, battlefront, urgency, admittedKeys)

  private def buildFrame(
      ownerSide: Option[String],
      intent: Option[CertifiedDecisionSupport],
      battlefront: Option[CertifiedDecisionSupport],
      urgency: Option[CertifiedDecisionSupport],
      admittedKeys: Set[String]
  ): CertifiedDecisionFrame =
    val alignmentKeys =
      (intent.toList ++ battlefront.toList)
        .flatMap(signal => textAlignmentKeys(signal.sentence))
        .toSet
    val carrierAlignmentKeys =
      Option.when(intent.nonEmpty || battlefront.nonEmpty)(admittedKeys).getOrElse(Set.empty)
    CertifiedDecisionFrame(
      intent = intent,
      battlefront = battlefront,
      urgency = urgency,
      ownerSide = ownerSide,
      alignmentKeys = alignmentKeys,
      carrierAlignmentKeys = carrierAlignmentKeys
    )

  private def buildIntent(
      ownerSide: Option[String],
      evidenceBackedPlans: List[PlanHypothesis],
      evidencePlanKeys: Set[String],
      admittedKeys: Set[String],
      targetOpt: Option[StrategyDirectionalTarget],
      routeOpt: Option[StrategyPieceRoute],
      moveRefOpt: Option[StrategyPieceMoveRef]
  ): Option[CertifiedDecisionSupport] =
    if ownerSide.isEmpty || evidenceBackedPlans.isEmpty || admittedKeys.isEmpty then None
    else
      def planAligned(candidateKeys: Set[String]): Boolean =
        alignedToAdmittedKeys(candidateKeys, admittedKeys) ||
          alignedToPlanKeys(candidateKeys, evidencePlanKeys)

      val fromTarget =
        targetOpt
          .filter(target => sameSide(target.ownerSide, ownerSide.get))
          .filter(target =>
            target.evidence.nonEmpty &&
              target.readiness != DirectionalTargetReadiness.Premature &&
              target.strategicReasons.nonEmpty
          )
          .filter(target =>
            planAligned(squareAlignmentKeys(target.targetSquare).toSet ++ sourceAlignmentKeys(target.strategicReasons.mkString(" ")))
          )
          .map { target =>
            CertifiedDecisionSupport(
              axis = CertifiedDecisionFrameAxis.Intent,
              sentence = s"${sideLabel(ownerSide.get)} is playing for pressure on ${target.targetSquare}.",
              priority = 90,
              sourceKind = "directional_target"
            )
          }

      val fromRoute =
        Option.when(fromTarget.isEmpty) {
          routeOpt
            .filter(route =>
                sameSide(route.ownerSide, ownerSide.get) &&
                route.surfaceMode != RouteSurfaceMode.Hidden &&
                route.route.nonEmpty &&
                route.purpose.trim.nonEmpty
            )
            .filter(route =>
              planAligned(route.route.lastOption.toList.flatMap(squareAlignmentKeys).toSet ++ sourceAlignmentKeys(route.purpose))
            )
            .flatMap { route =>
              route.route.lastOption.map(square =>
                CertifiedDecisionSupport(
                  axis = CertifiedDecisionFrameAxis.Intent,
                  sentence = s"${sideLabel(ownerSide.get)} is trying to bring the ${pieceLabel(route.piece)} toward $square.",
                  priority = 80,
                  sourceKind = "piece_route"
                )
              )
            }
        }.flatten

      val fromMoveRef =
        Option.when(fromTarget.isEmpty && fromRoute.isEmpty) {
          moveRefOpt
            .filter(moveRef => sameSide(moveRef.ownerSide, ownerSide.get))
            .filter(moveRef => moveRef.evidence.nonEmpty)
            .filter(moveRef => planAligned(squareAlignmentKeys(moveRef.target).toSet ++ sourceAlignmentKeys(moveRef.idea)))
            .map { moveRef =>
              CertifiedDecisionSupport(
                axis = CertifiedDecisionFrameAxis.Intent,
                sentence = s"${sideLabel(ownerSide.get)} is trying to bring the ${pieceLabel(moveRef.piece)} to ${moveRef.target}.",
                priority = 70,
                sourceKind = "move_ref"
              )
            }
        }.flatten

      List(fromTarget, fromRoute, fromMoveRef).flatten
        .map(sanitizeSupport)
        .find(_.nonEmpty)
        .flatten

  private def buildBattlefront(
      evidenceBackedPlans: List[PlanHypothesis],
      admittedKeys: Set[String],
      carriers: List[BattlefrontCarrier]
  ): Option[CertifiedDecisionSupport] =
    if carriers.isEmpty || evidenceBackedPlans.isEmpty || admittedKeys.isEmpty then None
    else
      val grouped = carriers.groupBy(_.key).view.mapValues(_.distinctBy(carrier => (carrier.independenceKey, carrier.anchorClass))).toMap
      val selected =
        grouped.values.toList
          .filter(group =>
            group.map(_.independenceKey).distinct.size >= 2 &&
              group.exists(carrier => carrier.anchorClass != BattlefrontAnchorClass.None) &&
              group.exists(carrier => admittedKeys.contains(carrier.key))
          )
          .sortBy(group => (-group.size, theaterPriority(group.head.key)))
          .headOption
      selected.flatMap { group =>
        val sentence = s"The real fight is ${group.head.phrase}."
        sanitizeSupport(
          CertifiedDecisionSupport(
            axis = CertifiedDecisionFrameAxis.Battlefront,
            sentence = sentence,
            priority = 85,
            sourceKind = group.map(_.sourceKind).distinct.mkString("+")
          )
        )
      }

  private def buildUrgency(
      mode: PlayerFacingTruthMode,
      criticality: Option[String],
      choiceType: Option[String],
      tensionScore: Option[Int]
  ): Option[CertifiedDecisionSupport] =
    val urgencyClass =
      if mode == PlayerFacingTruthMode.Tactical then Some(CertifiedUrgencyClass.Immediate)
      else if criticality.exists(normalized(_) == "forced") ||
          choiceType.exists(normalized(_) == "onlymove") ||
          tensionScore.exists(_ >= 180)
      then Some(CertifiedUrgencyClass.Pressing)
      else if mode != PlayerFacingTruthMode.Minimal then Some(CertifiedUrgencyClass.Slow)
      else None

    urgencyClass.flatMap {
      case CertifiedUrgencyClass.Immediate =>
        sanitizeSupport(
          CertifiedDecisionSupport(
            axis = CertifiedDecisionFrameAxis.Urgency,
            sentence = "This is immediate: the tactical point matters right now.",
            priority = 100,
            sourceKind = "tactical_truth_mode"
          )
        )
      case CertifiedUrgencyClass.Pressing =>
        sanitizeSupport(
          CertifiedDecisionSupport(
            axis = CertifiedDecisionFrameAxis.Urgency,
            sentence = "The timing matters now, and there is not much room to drift.",
            priority = 70,
            sourceKind = "tension_signal"
          )
        )
      case CertifiedUrgencyClass.Slow =>
        sanitizeSupport(
          CertifiedDecisionSupport(
            axis = CertifiedDecisionFrameAxis.Urgency,
            sentence = "This is slower, so improving the setup matters more than forcing play right away.",
            priority = 45,
            sourceKind = "slow_truth_mode"
          )
        )
    }

  private def admittedAlignmentKeys(
      mainBundle: Option[MainPathClaimBundle],
      quietIntent: Option[QuietMoveIntentClaim]
  ): Set[String] =
    mainBundle.toList.flatMap(_.mainClaim.toList.flatMap(claim => claim.anchorTerms ++ claim.evidenceLines)).flatMap(textAlignmentKeys).toSet ++
      quietIntent.toList.flatMap(intent => textAlignmentKeys(intent.claimText) ++ intent.evidenceLine.toList.flatMap(textAlignmentKeys))

  private def claimBattlefrontCarriers(
      mainBundle: Option[MainPathClaimBundle],
      quietIntent: Option[QuietMoveIntentClaim]
  ): List[BattlefrontCarrier] =
    mainBundle.toList.flatMap(_.mainClaim.toList.flatMap { claim =>
      claim.anchorTerms.flatMap(textBattlefrontCarriers(_, "claim_anchor", "claim", BattlefrontAnchorClass.MoveLocal))
    }) ++
      quietIntent.toList.flatMap(intent =>
        textBattlefrontCarriers(intent.claimText, "quiet_claim", "quiet_claim", BattlefrontAnchorClass.MoveLocal)
      )

  private def deltaBundleBattlefrontCarriers(
      deltaBundle: PlayerFacingMoveDeltaBundle,
      ownerSide: Option[String]
  ): List[BattlefrontCarrier] =
    deltaBundle.claims.flatMap(claim =>
      textBattlefrontCarriers(claim.anchorText, s"delta_${claim.sourceKind}", "delta_claim", BattlefrontAnchorClass.MoveLocal)
    ) ++
      deltaBundle.visibleDirectionalTargets.filter(target =>
        ownerSide.forall(side => sameSide(target.ownerSide, side))
      ).flatMap(target =>
        textBattlefrontCarriers(target.targetSquare, "delta_target", "delta_target", BattlefrontAnchorClass.MoveLocal) ++
          textBattlefrontCarriers(
            target.strategicReasons.mkString(" "),
            "delta_target_reason",
            "delta_target",
            BattlefrontAnchorClass.None
          )
      ) ++
      deltaBundle.visibleRouteRefs.filter(route =>
        ownerSide.forall(side => sameSide(route.ownerSide, side))
      ).flatMap(route =>
        route.route.lastOption.toList.flatMap(square =>
          textBattlefrontCarriers(square, "delta_route", "delta_route", BattlefrontAnchorClass.MoveLocal)
        ) ++
          textBattlefrontCarriers(route.purpose, "delta_route_purpose", "delta_route", BattlefrontAnchorClass.None)
      )

  private def dossierBattlefrontCarriers(
      dossier: Option[ActiveBranchDossier],
      ownerSide: Option[String]
  ): List[BattlefrontCarrier] =
    dossier.toList.flatMap { value =>
      value.routeCue.toList.flatMap(cue =>
        Option.when(ownerSide.forall(side => sameSide(cue.ownerSide, side))) {
          cue.route.lastOption.toList.flatMap(square =>
            textBattlefrontCarriers(square, "dossier_route", "dossier_route", BattlefrontAnchorClass.None)
          ) ++
            textBattlefrontCarriers(cue.purpose, "dossier_route_purpose", "dossier_route", BattlefrontAnchorClass.None)
        }.getOrElse(Nil)
      ) ++
      value.moveCue.toList.flatMap(cue =>
        cue.san.toList.flatMap(text => textBattlefrontCarriers(text, "dossier_move", "dossier_move", BattlefrontAnchorClass.None))
      ) ++
      List(value.evidenceCue, value.whyChosen, value.continuationFocus).flatten.flatMap(text =>
        textBattlefrontCarriers(text, "dossier_text", "dossier_text", BattlefrontAnchorClass.None)
      )
    }

  private def planBattlefrontCarriers(
      evidenceBackedPlans: List[PlanHypothesis]
  ): List[BattlefrontCarrier] =
    evidenceBackedPlans.flatMap { plan =>
      concretePlanTexts(plan)
        .flatMap(text =>
          textBattlefrontCarriers(text, "plan_hypothesis", "probe_plan", BattlefrontAnchorClass.ProbeBacked)
        )
    }

  private def surfaceBattlefrontCarriers(
      surface: StrategyPackSurface.Snapshot
  ): List[BattlefrontCarrier] =
    surface.topDirectionalTarget.toList.flatMap(target =>
      textBattlefrontCarriers(target.targetSquare, "surface_target_square", "surface_target", BattlefrontAnchorClass.None) ++
        zoneBattlefrontCarriers(
          "surface_target_zone",
          "surface_target",
          BattlefrontAnchorClass.None,
          target.targetSquare :: target.strategicReasons
        )
    ) ++
      surface.topRoute.toList.flatMap(route =>
        route.route.lastOption.toList.flatMap(square =>
          textBattlefrontCarriers(square, "surface_route_square", "surface_route", BattlefrontAnchorClass.None)
        ) ++
          zoneBattlefrontCarriers(
            "surface_route_zone",
            "surface_route",
            BattlefrontAnchorClass.None,
            route.route ++ List(route.purpose)
          )
      ) ++
      surface.topMoveRef.toList.flatMap(moveRef =>
        textBattlefrontCarriers(moveRef.target, "surface_move_square", "surface_move", BattlefrontAnchorClass.None) ++
          zoneBattlefrontCarriers("surface_move_zone", "surface_move", BattlefrontAnchorClass.None, List(moveRef.idea))
      ) ++
      surface.dominantIdea.toList.flatMap(idea =>
        zoneBattlefrontCarriers(
          "surface_dominant_zone",
          "surface_dominant",
          BattlefrontAnchorClass.None,
          idea.focusZone.toList ++ idea.focusSquares ++ idea.focusFiles.map(file => s"$file-file")
        )
      ) ++
      Nil

  private def themeBattlefrontCarriers(
      themeSurface: Option[ActiveThemeSurfaceBuilder.ThemeSurface]
  ): List[BattlefrontCarrier] =
    themeSurface.toList.flatMap { surface =>
      surface.zones.toList.flatMap(zone =>
        textBattlefrontCarriers(zone, "theme_zone", "theme_zone", BattlefrontAnchorClass.None)
      ) ++
        surface.structuralCue.toList.flatMap(text =>
          textBattlefrontCarriers(text, "theme_structure", "theme_structure", BattlefrontAnchorClass.None)
        )
    }

  private def zoneBattlefrontCarriers(
      sourceKind: String,
      independenceKey: String,
      anchorClass: BattlefrontAnchorClass,
      texts: List[String]
  ): List[BattlefrontCarrier] =
    texts.flatMap(textBattlefrontCarriers(_, sourceKind, independenceKey, anchorClass)).filterNot(_.key.startsWith("square:"))

  private def textBattlefrontCarriers(
      text: String,
      sourceKind: String,
      independenceKey: String,
      anchorClass: BattlefrontAnchorClass
  ): List[BattlefrontCarrier] =
    sourceAlignmentKeys(text).toList.map(key =>
      BattlefrontCarrier(key, phraseFromKey(key), sourceKind, independenceKey, anchorClass)
    )

  private def sanitizeSupport(
      support: CertifiedDecisionSupport
  ): Option[CertifiedDecisionSupport] =
    val sanitized =
      UserFacingSignalSanitizer.sanitize(
        LiveNarrativeCompressionCore.rewritePlayerLanguage(Option(support.sentence).getOrElse("").trim)
      ).replaceAll("""\s+""", " ").trim
    Option.when(
      sanitized.nonEmpty &&
        LiveNarrativeCompressionCore.keepPlayerFacingSentence(sanitized) &&
        !LiveNarrativeCompressionCore.isLowValueNarrativeSentence(sanitized) &&
        !GenericShellTokens.exists(token => sanitized.toLowerCase.contains(token))
    )(
      support.copy(sentence = sanitized)
    )

  private def alignedToAdmittedKeys(
      candidateKeys: Set[String],
      admittedKeys: Set[String]
  ): Boolean =
    candidateKeys.nonEmpty &&
      admittedKeys.nonEmpty &&
      candidateKeys.exists(admittedKeys.contains)

  private def alignedToPlanKeys(
      candidateKeys: Set[String],
      evidencePlanKeys: Set[String]
  ): Boolean =
    candidateKeys.nonEmpty &&
      evidencePlanKeys.nonEmpty &&
      candidateKeys.exists(evidencePlanKeys.contains)

  private def evidenceBackedMainPlans(moment: GameChronicleMoment): List[PlanHypothesis] =
    StrategicNarrativePlanSupport.filterEvidenceBacked(
      moment.mainStrategicPlans,
      moment.strategicPlanExperiments
    )

  private def planAlignmentKeys(
      evidenceBackedPlans: List[PlanHypothesis]
  ): Set[String] =
    evidenceBackedPlans.flatMap { plan =>
      concretePlanTexts(plan).flatMap(sourceAlignmentKeys)
    }.toSet

  private def concretePlanTexts(
      plan: PlanHypothesis
  ): List[String] =
    (plan.preconditions ++ plan.executionSteps ++ plan.failureModes ++ plan.refutation.toList)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def sourceAlignmentKeys(text: String): Set[String] =
    Option.when(!isForbiddenDecisionFrameSourceText(text))(textAlignmentKeys(text)).getOrElse(Set.empty)

  private def isForbiddenDecisionFrameSourceText(text: String): Boolean =
    val low = normalized(text)
    low.isEmpty || GenericShellTokens.exists(token => low.contains(token))

  private[analysis] def moveAlignmentKeys(moveRef: ActiveStrategicMoveRef): Set[String] =
    moveRef.san.toList.flatMap(textAlignmentKeys).toSet ++ textAlignmentKeys(moveRef.label) ++ textAlignmentKeys(moveRef.uci)

  private[analysis] def moveAlignmentKeys(moveCue: ActiveBranchMoveCue): Set[String] =
    moveCue.san.toList.flatMap(textAlignmentKeys).toSet ++ textAlignmentKeys(moveCue.label) ++ textAlignmentKeys(moveCue.uci)

  private[analysis] def textAlignmentKeys(text: String): Set[String] =
    Option(text).toList.flatMap { raw =>
      val normalizedText = raw.trim.toLowerCase
      val uciSquare =
        normalizedText match
          case UciPattern() => squareAlignmentKeys(normalizedText.slice(2, 4))
          case _            => Nil
      val squares = SquarePattern.findAllMatchIn(normalizedText).flatMap(m => squareAlignmentKeys(m.group(1))).toList
      val files = FilePattern.findAllMatchIn(normalizedText).map(m => fileKey(m.group(1))).toList
      val zones = ZonePattern.findAllMatchIn(normalizedText).map(m => zoneKey(m.group(1))).toList
      val complexes = ComplexPattern.findAllMatchIn(normalizedText).map(m => complexKey(m.group(1))).toList
      uciSquare ++ squares ++ files ++ zones ++ complexes
    }.toSet

  private[analysis] def squareAlignmentKeys(square: String): List[String] =
    cleanSquare(square).map(squareKey).toList

  private def phraseFromKey(key: String): String =
    if key.startsWith("square:") then s"around ${key.stripPrefix("square:")}"
    else if key.startsWith("file:") then s"on the ${key.stripPrefix("file:")}-file"
    else if key.startsWith("zone:") then
      key.stripPrefix("zone:") match
        case "center" => "in the center"
        case zone     => s"on the $zone"
    else if key.startsWith("complex:") then s"on the ${key.stripPrefix("complex:")} squares"
    else "in the current sector"

  private def theaterPriority(key: String): Int =
    if key.startsWith("zone:") then 0
    else if key.startsWith("file:") then 1
    else if key.startsWith("complex:") then 2
    else if key.startsWith("square:") then 3
    else 4

  private def sideFromPly(ply: Int): Option[String] =
    Option.when(ply > 0)(if ply % 2 == 1 then "white" else "black")

  private def sideLabel(side: String): String =
    if sameSide(side, "black") then "Black" else "White"

  private[analysis] def ideaFocusSummary(
      support: CertifiedDecisionSupport,
      ownerSide: String
  ): String =
    val text = stripTrailingSentencePunctuation(support.sentence)
    support.axis match
      case CertifiedDecisionFrameAxis.Intent =>
        stripKnownPrefix(
          text,
          List(
            s"${sideLabel(ownerSide)} is playing for ",
            s"${sideLabel(ownerSide)} is trying to bring the "
          )
        ).getOrElse(text)
      case CertifiedDecisionFrameAxis.Battlefront =>
        stripKnownPrefix(text, List("The real fight is ")).getOrElse(text)
      case CertifiedDecisionFrameAxis.Urgency =>
        text

  private def pieceLabel(piece: String): String =
    Option(piece).map(_.trim.toUpperCase).getOrElse("") match
      case "P" => "pawn"
      case "N" => "knight"
      case "B" => "bishop"
      case "R" => "rook"
      case "Q" => "queen"
      case "K" => "king"
      case other if other.nonEmpty => other.toLowerCase
      case _                       => "piece"

  private def cleanSquare(square: String): Option[String] =
    Option(square).map(_.trim.toLowerCase).filter(_.matches("^[a-h][1-8]$"))

  private def stripKnownPrefix(text: String, prefixes: List[String]): Option[String] =
    prefixes.collectFirst(Function.unlift(prefix =>
      Option.when(text.startsWith(prefix))(text.drop(prefix.length).trim)
    )).filter(_.nonEmpty)

  private def stripTrailingSentencePunctuation(text: String): String =
    Option(text).map(_.trim.stripSuffix(".").stripSuffix("!").stripSuffix("?").trim).getOrElse("")

  private def squareKey(square: String): String = s"square:${square.toLowerCase}"

  private def fileKey(file: String): String = s"file:${file.trim.toLowerCase.take(1)}"

  private def zoneKey(zone: String): String =
    val normalizedZone =
      normalized(zone) match
        case "central" => "center"
        case other      => other
    s"zone:$normalizedZone"

  private def complexKey(kind: String): String = s"complex:${normalized(kind)}"

  private def normalized(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase.replaceAll("""\s+""", " ")

  private[analysis] def sameSide(left: String, right: String): Boolean =
    normalized(left) == normalized(right)
