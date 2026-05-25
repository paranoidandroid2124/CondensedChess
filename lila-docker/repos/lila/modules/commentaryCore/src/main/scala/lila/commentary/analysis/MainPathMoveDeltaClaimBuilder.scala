package lila.commentary.analysis

import lila.commentary.{ RouteSurfaceMode, StrategyPack }
import lila.commentary.analysis.claim.PlayerFacingClaimPrefixKind
import lila.commentary.analysis.semantic.StrategicObservationIds.ProofFamilyId
import lila.commentary.model.*
import lila.commentary.model.strategic.{ VariationLine, VariationTag }

private[commentary] enum PlayerFacingClaimScope:
  case MoveLocal
  case PositionLocal
  case LineScoped

private[commentary] final case class MainPathScopedClaim(
    scope: PlayerFacingClaimScope,
    mode: PlayerFacingTruthMode,
    deltaClass: Option[PlayerFacingMoveDeltaClass],
    claimText: String,
    anchorTerms: List[String],
    evidenceLines: List[String],
    sourceKind: String,
    tacticalOwnership: Option[String],
    prefixKind: PlayerFacingClaimPrefixKind = PlayerFacingClaimPrefixKind.None,
    packet: Option[PlayerFacingClaimPacket] = None
):
  def lens: StrategicLens =
    if tacticalOwnership.nonEmpty then StrategicLens.Decision
    else
      deltaClass match
        case Some(PlayerFacingMoveDeltaClass.CounterplayReduction) => StrategicLens.Prophylaxis
        case Some(PlayerFacingMoveDeltaClass.ExchangeForcing)      => StrategicLens.Decision
        case Some(PlayerFacingMoveDeltaClass.ResourceRemoval)      => StrategicLens.Decision
        case Some(PlayerFacingMoveDeltaClass.PressureIncrease)     => StrategicLens.Structure
        case Some(PlayerFacingMoveDeltaClass.NewAccess)            => StrategicLens.Structure
        case Some(PlayerFacingMoveDeltaClass.PlanAdvance)          => StrategicLens.Structure
        case None                                                  => StrategicLens.Decision

private[commentary] final case class MainPathClaimBundle(
    mainClaim: Option[MainPathScopedClaim],
    lineScopedClaim: Option[MainPathScopedClaim]
):
  def primaryClaim: Option[MainPathScopedClaim] = mainClaim.orElse(lineScopedClaim)

private[commentary] object MainPathMoveDeltaClaimBuilder:

  private val QueenTradeShieldFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.QueenTradeShield).get.wireKey
  private val IQPInducementFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.IQPInducement).get.wireKey
  private val DefenderTradeFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.DefenderTrade).get.wireKey
  private val BadPieceLiquidationFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.BadPieceLiquidation).get.wireKey
  private val BoundedFavorableSimplificationFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.SimplificationWindow).get.wireKey

  def build(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      candidateEvidenceLines: List[String] = Nil
  ): Option[MainPathClaimBundle] =
    val surface = StrategyPackSurface.from(strategyPack)
    val classifiedMode = PlayerFacingTruthModePolicy.classify(ctx, strategyPack, truthContract)
    classifiedMode match
      case PlayerFacingTruthMode.Tactical =>
        buildTacticalBundle(ctx, truthContract, candidateEvidenceLines)
      case PlayerFacingTruthMode.Strategic =>
        buildStrategicBundle(ctx, strategyPack, surface, truthContract, candidateEvidenceLines)
      case PlayerFacingTruthMode.Minimal
          if PlayerFacingTruthModePolicy
            .mainPathMoveDeltaEvidence(ctx, surface, truthContract)
            .exists(_.packet.admitsStrategicTruthMode) =>
        buildStrategicBundle(ctx, strategyPack, surface, truthContract, candidateEvidenceLines)
      case PlayerFacingTruthMode.Minimal =>
        buildTacticalLineOnly(ctx, truthContract, candidateEvidenceLines)

  private def buildTacticalBundle(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract],
      candidateEvidenceLines: List[String]
  ): Option[MainPathClaimBundle] =
    val evidenceLines = tacticalEvidenceLines(ctx, candidateEvidenceLines)
    val ownership = tacticalOwnership(ctx, truthContract)
    val mainClaim =
      PlayerFacingTruthModePolicy
        .tacticalLeadSentence(ctx, truthContract)
        .flatMap(clean)
        .map { lead =>
          MainPathScopedClaim(
            scope = PlayerFacingClaimScope.MoveLocal,
            mode = PlayerFacingTruthMode.Tactical,
            deltaClass = None,
            claimText = lead,
            anchorTerms = Nil,
            evidenceLines = evidenceLines,
            sourceKind = tacticalSourceKind(ctx, truthContract),
            tacticalOwnership = ownership
          )
        }
    val lineClaim =
      evidenceLines.headOption.map { line =>
        MainPathScopedClaim(
          scope = PlayerFacingClaimScope.LineScoped,
          mode = PlayerFacingTruthMode.Tactical,
          deltaClass = None,
          claimText = line,
          anchorTerms = Nil,
          evidenceLines = List(line),
          sourceKind = "tactical_line",
          tacticalOwnership = ownership
        )
      }
    mainClaim
      .map(main => MainPathClaimBundle(Some(main), lineClaim))
      .orElse(lineClaim.map(line => MainPathClaimBundle(None, Some(line))))

  private def buildStrategicBundle(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract],
      candidateEvidenceLines: List[String]
  ): Option[MainPathClaimBundle] =
    PlayerFacingTruthModePolicy
      .mainPathMoveDeltaEvidence(ctx, surface, truthContract)
      .flatMap { delta =>
        val anchorTerms =
          (
            delta.packet.anchorTerms ++
              delta.packet.proofPathWitness.ownerSeedTerms ++
              delta.packet.proofPathWitness.structureTransitionTerms
          ).filter(_.trim.nonEmpty).distinct
        val sourceKind = strategicSourceKind(delta.packet, surface, truthContract)
        val lineEvidence =
          Option.when(delta.allowsLineEvidenceHook) {
            strategicEvidenceLines(
              delta = delta,
              ctx = ctx,
              strategyPack = strategyPack,
              truthContract = truthContract,
              candidateEvidenceLines = candidateEvidenceLines
            )
          }.getOrElse(Nil)
        val mainClaim =
          strategicClaim(delta, ctx, surface, truthContract)
            .flatMap(clean)
            .filter(_ => delta.allowsWeakMainClaim || delta.check_qualifying)
            .filter(text =>
              PlayerFacingTruthModePolicy.allowsStrategicClaimText(
                text,
                ctx,
                strategyPack,
                truthContract
              )
            )
            .map { text =>
              val finalClaimText =
                if (delta.allowsWeakMainClaim) text
                else qualify_text(text)
              val prefix =
                if (delta.deltaClass == PlayerFacingMoveDeltaClass.PressureIncrease &&
                    (delta.packet.proofSource == PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource ||
                     delta.packet.proofSource == PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource)) {
                  PlayerFacingClaimPrefixKind.KeyStrategicFact
                } else {
                  PlayerFacingClaimPrefixKind.None
                }
              MainPathScopedClaim(
                scope = claimScope(delta.packet),
                mode = PlayerFacingTruthMode.Strategic,
                deltaClass = Some(delta.deltaClass),
                claimText = finalClaimText,
                prefixKind = prefix,
                anchorTerms = anchorTerms,
                evidenceLines = lineEvidence,
                sourceKind = sourceKind,
                tacticalOwnership = None,
                packet = Some(delta.packet)
              )
            }
        val lineClaim =
          lineEvidence.headOption.map { line =>
            MainPathScopedClaim(
              scope = PlayerFacingClaimScope.LineScoped,
              mode = PlayerFacingTruthMode.Strategic,
              deltaClass = Some(delta.deltaClass),
              claimText = line,
              anchorTerms = anchorTerms,
              evidenceLines = List(line),
              sourceKind = s"${sourceKind}_line",
              tacticalOwnership = None,
              packet = Some(delta.packet.copy(scope = PlayerFacingPacketScope.LineScoped))
            )
          }
        Option.when(mainClaim.nonEmpty || lineClaim.nonEmpty) {
          MainPathClaimBundle(mainClaim, lineClaim)
        }
      }

  private def buildTacticalLineOnly(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract],
      candidateEvidenceLines: List[String]
  ): Option[MainPathClaimBundle] =
    tacticalEvidenceLines(ctx, candidateEvidenceLines).headOption.map { line =>
      val lineClaim =
        MainPathScopedClaim(
          scope = PlayerFacingClaimScope.LineScoped,
          mode = PlayerFacingTruthMode.Tactical,
          deltaClass = None,
          claimText = line,
          anchorTerms = Nil,
          evidenceLines = List(line),
          sourceKind = "tactical_line_only",
          tacticalOwnership = tacticalOwnership(ctx, truthContract)
        )
      MainPathClaimBundle(None, Some(lineClaim))
    }

  private def tacticalEvidenceLines(
      ctx: NarrativeContext,
      candidateEvidenceLines: List[String]
  ): List[String] =
    val external =
      candidateEvidenceLines
        .flatMap(clean)
        .filter(lineShowsImmediateTacticalProof)
        .take(1)
    val engine =
      Option.when(external.isEmpty) {
        variationPreview(ctx.engineEvidence.toList.flatMap(_.variations))
          .map(text => s"Line: a) $text.")
          .flatMap(clean)
          .filter(lineShowsImmediateTacticalProof)
      }.flatten.toList
    (external ++ engine).distinct.take(1)

  private def strategicEvidenceLines(
      delta: PlayerFacingMoveDeltaEvidence,
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      candidateEvidenceLines: List[String]
  ): List[String] =
    val external =
      candidateEvidenceLines
        .flatMap(clean)
        .filter(line =>
          PlayerFacingTruthModePolicy.allowsStrategicSupportText(
            line,
            "line-scoped",
            ctx,
            strategyPack,
            truthContract
          ) && PlayerFacingTruthModePolicy.lineShowsMainPathDelta(line, delta)
        )
        .take(1)
    val engine =
      Option.when(external.isEmpty) {
        variationPreview(ctx.engineEvidence.toList.flatMap(_.variations))
          .map(text => s"Line: a) $text.")
          .flatMap(clean)
          .filter(line =>
            PlayerFacingTruthModePolicy.lineShowsMainPathDelta(line, delta)
          )
      }.flatten.toList
    (external ++ engine).distinct.take(1)

  private def strategicClaim(
      delta: PlayerFacingMoveDeltaEvidence,
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    def anchor: Option[String] =
      routeDestination(surface)
        .orElse(targetSquare(surface))
        .orElse(moveTarget(surface))
        .orElse(contractAnchor(truthContract))
    delta.deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess =>
        anchor.map { square =>
          delta.modalityTier match
            case PlayerFacingClaimModalityTier.Advances => s"This opens access to $square."
            case _                                      => s"This still leaves access to $square."
        }
      case PlayerFacingMoveDeltaClass.PressureIncrease =>
        PlayerFacingTruthModePolicy.pressureIncreaseMainClaim(
          packet = delta.packet,
          modalityTier = delta.modalityTier,
          fallbackAnchor = preferredWitnessAnchor(delta.packet).orElse(anchor)
        )
      case PlayerFacingMoveDeltaClass.ExchangeForcing =>
        preferredWitnessAnchor(delta.packet).orElse(anchor).map { square =>
          if delta.packet.proofFamily == QueenTradeShieldFamily &&
              delta.packet.proofSource == PlayerFacingTruthModePolicy.QueenTradeShieldProofSource
          then "This exchange moves the game into the queenless branch."
          else if delta.packet.proofFamily == IQPInducementFamily &&
              delta.packet.proofSource == PlayerFacingTruthModePolicy.IQPInducementProbeProofSource
          then "This sequence leaves an isolated pawn as the local target."
          else if delta.packet.proofFamily == DefenderTradeFamily &&
              delta.packet.proofSource == PlayerFacingTruthModePolicy.DefenderTradeProofSource
          then "This exchange removes a defender on the local branch."
          else if delta.packet.proofFamily == BadPieceLiquidationFamily &&
              delta.packet.proofSource == PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource
          then "This trade clears the bad piece from the local branch."
          else if delta.packet.proofFamily == BoundedFavorableSimplificationFamily then
            delta.modalityTier match
              case PlayerFacingClaimModalityTier.Forces =>
                s"This favorable simplification keeps the same local edge after the trade on $square."
              case _ =>
                s"This trade keeps the same local edge on $square."
          else
            delta.modalityTier match
              case PlayerFacingClaimModalityTier.Forces => s"This makes the exchange on $square hard to avoid."
              case _                                    => s"This keeps the exchange on $square available."
        }
      case PlayerFacingMoveDeltaClass.CounterplayReduction =>
        val namedBreakOnly = delta.packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey
        val namedResourceOnly = delta.packet.proofFamily == ProofFamilyId.CounterplayRestraint.wireKey
        strategicCounterplayClaim(ctx, delta)
          .orElse {
            Option.unless(namedBreakOnly || namedResourceOnly) {
              anchor.map { focal =>
                delta.ontologyFamily match
                  case PlayerFacingClaimOntologyKind.RouteDenial =>
                    "This keeps the opponent from getting easy entry there."
                  case PlayerFacingClaimOntologyKind.ColorComplexSqueeze =>
                    s"This keeps a longer squeeze around $focal."
                  case _ =>
                    if delta.modalityTier == PlayerFacingClaimModalityTier.Supports then
                      s"This cuts down the opponent's counterplay around $focal."
                    else s"This continues to restrain counterplay around $focal."
              }
            }.flatten
          }
          .orElse(Option.unless(namedBreakOnly || namedResourceOnly)("This continues to restrain the opponent's counterplay."))
      case PlayerFacingMoveDeltaClass.ResourceRemoval =>
        resourceRemovalClaim(ctx, delta)
          .orElse(anchor.map { focal =>
            if delta.modalityTier == PlayerFacingClaimModalityTier.Removes then
              s"This removes the defensive resource tied to $focal."
            else s"This limits the defensive resource tied to $focal."
          })
      case PlayerFacingMoveDeltaClass.PlanAdvance =>
        if delta.packet.proofFamily == CentralBreakTimingWitness.ProofFamily &&
            delta.packet.proofSource == CentralBreakTimingWitness.ProofSource
        then
          CentralBreakTimingWitness
            .exact(ctx)
            .map(PlayerFacingTruthModePolicy.centralBreakTimingClaimText)
        else
          preferredWitnessAnchor(delta.packet).orElse(anchor).map { square =>
            delta.modalityTier match
              case PlayerFacingClaimModalityTier.Advances => s"This advances the plan toward $square."
              case PlayerFacingClaimModalityTier.Supports => s"This supports the plan toward $square."
              case _                                      => s"This keeps the plan toward $square available."
          }

  private def strategicCounterplayClaim(
      ctx: NarrativeContext,
      delta: PlayerFacingMoveDeltaEvidence
  ): Option[String] =
    val witnessAnchor = preferredWitnessAnchor(delta.packet)
    val namedBreakOnly = delta.packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey
    val namedResourceOnly = delta.packet.proofFamily == ProofFamilyId.CounterplayRestraint.wireKey
    Option.unless(HeavyPieceLocalBindValidation.blocksPlayerFacingShell(ctx) && !namedBreakOnly) {
      val preventedPlans =
        ctx.semantic.toList.flatMap(_.preventedPlans)
      val namedBreakClaim =
        preventedPlans
          .find(_.sourceScope == FactScope.Now)
          .flatMap { prevented =>
            prevented.breakNeutralized
              .flatMap(clean)
              .orElse(witnessAnchor)
              .map(file => s"This keeps $file from coming right away.")
          }
      if namedBreakOnly then namedBreakClaim
      else
        LocalFileEntryProof.certifiedSurfacePair(ctx)
          .map(pair =>
            s"This keeps ${pair.entrySquare} closed and takes the ${pair.file} away as a counterplay route."
          )
          .orElse {
          preventedPlans
            .find(_.sourceScope == FactScope.Now)
            .flatMap { prevented =>
              delta.ontologyFamily match
                case PlayerFacingClaimOntologyKind.RouteDenial =>
                  prevented.deniedSquares.headOption
                    .flatMap(clean)
                    .orElse(witnessAnchor)
                    .map(square => s"This keeps the opponent out of $square.")
                case PlayerFacingClaimOntologyKind.ColorComplexSqueeze =>
                  clean(prevented.planId)
                    .filterNot(_.equalsIgnoreCase("counterplay"))
                    .map(plan => s"This keeps a longer squeeze on $plan.")
                case _ =>
                  Option.when(namedResourceOnly) {
                      clean(prevented.planId)
                        .filterNot(_.equalsIgnoreCase("counterplay"))
                        .map(plan => s"This slows down $plan before it gets started.")
                  }.flatten
                    .orElse {
                      prevented.breakNeutralized
                        .flatMap(clean)
                        .orElse(witnessAnchor)
                        .map(file => s"This keeps $file from coming right away.")
                    }
                    .orElse(Option.unless(namedResourceOnly) {
                      prevented.deniedSquares.headOption
                        .flatMap(clean)
                        .orElse(witnessAnchor)
                        .map(square => s"This keeps the opponent out of $square.")
                    }.flatten)
                    .orElse(Option.unless(namedResourceOnly) {
                      clean(prevented.planId)
                        .filterNot(_.equalsIgnoreCase("counterplay"))
                        .orElse(witnessAnchor)
                        .map(plan => s"This slows down $plan before it gets started.")
                    }.flatten)
                    .filterNot(_ => namedResourceOnly && clean(prevented.planId).isEmpty)
            }
        }
    }.flatten

  private def resourceRemovalClaim(
      ctx: NarrativeContext,
      delta: PlayerFacingMoveDeltaEvidence
  ): Option[String] =
    val witnessAnchor = preferredWitnessAnchor(delta.packet)
    ctx.semantic.toList
      .flatMap(_.preventedPlans)
      .find(plan =>
        plan.sourceScope == FactScope.Now && (
          plan.preventedThreatType.exists(_.trim.nonEmpty) ||
            plan.breakNeutralized.exists(_.trim.nonEmpty) ||
            plan.deniedSquares.nonEmpty
        )
      )
      .flatMap { prevented =>
        val verb =
          if delta.modalityTier == PlayerFacingClaimModalityTier.Removes then "removes"
          else "limits"
        delta.ontologyFamily match
          case PlayerFacingClaimOntologyKind.RouteDenial =>
            prevented.deniedSquares.headOption
              .flatMap(clean)
              .orElse(witnessAnchor)
              .map(square => s"This $verb entry on $square as a defensive resource.")
          case _ =>
            prevented.breakNeutralized
              .flatMap(clean)
              .orElse(witnessAnchor)
              .map(file => s"This $verb $file as a defensive resource.")
              .orElse(
                prevented.deniedSquares.headOption
                  .flatMap(clean)
                  .orElse(witnessAnchor)
                  .map(square => s"This $verb $square as a defensive square.")
              )
              .orElse(
                prevented.preventedThreatType
                  .flatMap(clean)
                  .map(threat => s"This $verb the $threat resource.")
              )
      }

  private def routeDestination(surface: StrategyPackSurface.Snapshot): Option[String] =
    surface.topRoute.flatMap(_.route.lastOption).flatMap(clean)

  private def targetSquare(surface: StrategyPackSurface.Snapshot): Option[String] =
    surface.topDirectionalTarget.flatMap(target => clean(target.targetSquare))

  private def moveTarget(surface: StrategyPackSurface.Snapshot): Option[String] =
    surface.topMoveRef.flatMap(ref => clean(ref.target))

  private def contractAnchor(truthContract: Option[DecisiveTruthContract]): Option[String] =
    truthContract.flatMap(_.verifiedPayoffAnchor).flatMap(clean)

  private def preferredWitnessAnchor(packet: PlayerFacingClaimPacket): Option[String] =
    (
      packet.proofPathWitness.ownerSeedTerms ++
        packet.proofPathWitness.structureTransitionTerms ++
        packet.anchorTerms
    ).find(term => term.matches("[a-h][1-8]") || term.toLowerCase.contains("file"))

  private def claimScope(packet: PlayerFacingClaimPacket): PlayerFacingClaimScope =
    packet.scope match
      case PlayerFacingPacketScope.MoveLocal    => PlayerFacingClaimScope.MoveLocal
      case PlayerFacingPacketScope.PositionLocal => PlayerFacingClaimScope.PositionLocal
      case PlayerFacingPacketScope.LineScoped   => PlayerFacingClaimScope.LineScoped
      case PlayerFacingPacketScope.BackendOnly  => PlayerFacingClaimScope.LineScoped

  private def tacticalSourceKind(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): String =
    truthContract match
      case Some(contract)
          if contract.truthClass == DecisiveTruthClass.Blunder ||
            contract.truthClass == DecisiveTruthClass.MissedWin =>
        "tactical_contract"
      case Some(contract)
          if contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense ||
            contract.reasonFamily == DecisiveReasonKind.TacticalRefutation =>
        "forcing_contract"
      case _ if looksLikeTacticalSacrifice(ctx, truthContract) =>
        "tactical_sacrifice"
      case _ =>
        "tactical"

  private def tacticalOwnership(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    truthContract.flatMap { contract =>
      contract.truthClass match
        case DecisiveTruthClass.Blunder   => Some("blunder")
        case DecisiveTruthClass.MissedWin => Some("missed_win")
        case DecisiveTruthClass.Best if contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense =>
          Some("only_move_defense")
        case DecisiveTruthClass.Best if contract.reasonFamily == DecisiveReasonKind.TacticalRefutation =>
          Some("tactical_refutation")
        case _ => None
    }.orElse(Option.when(looksLikeTacticalSacrifice(ctx, truthContract))("tactical_sacrifice"))

  private def mainSourceKind(
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): String =
    if surface.topRoute.exists(_.surfaceMode == RouteSurfaceMode.Exact) then "route"
    else if surface.topDirectionalTarget.exists(_.targetSquare.trim.nonEmpty) then "target"
    else if surface.topMoveRef.exists(_.target.trim.nonEmpty) then "move_ref"
    else if truthContract.flatMap(_.verifiedPayoffAnchor).exists(_.trim.nonEmpty) then "contract"
    else "evidence"

  private def strategicSourceKind(
      packet: PlayerFacingClaimPacket,
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): String =
    Option.when(
      packet.proofSource == PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource ||
        packet.proofSource == PlayerFacingTruthModePolicy.QueenTradeShieldProofSource ||
        packet.proofSource == PlayerFacingTruthModePolicy.IQPInducementProbeProofSource ||
        packet.proofSource == PlayerFacingTruthModePolicy.DefenderTradeProofSource ||
        packet.proofSource == PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource ||
        packet.proofSource == CentralBreakTimingWitness.ProofSource
    ) {
      packet.proofSource
    }.getOrElse(mainSourceKind(surface, truthContract))

  private def looksLikeTacticalSacrifice(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    truthContract.exists(contract =>
      contract.reasonFamily == DecisiveReasonKind.InvestmentSacrifice ||
        contract.truthClass == DecisiveTruthClass.WinningInvestment ||
        contract.truthClass == DecisiveTruthClass.CompensatedInvestment
    ) &&
      ctx.engineEvidence.toList.flatMap(_.variations).headOption.exists { variation =>
        val window = variation.parsedMoves.take(4)
        val captureCount = window.count(_.isCapture)
        val checkCount = window.count(_.givesCheck)
        variation.mate.nonEmpty ||
        variation.tags.contains(VariationTag.Forced) ||
        (captureCount >= 2) ||
        (checkCount >= 2) ||
        (captureCount >= 1 && checkCount >= 1)
      }

  private def lineShowsImmediateTacticalProof(text: String): Boolean =
    val window = Option(text).getOrElse("").split("\\s+").toList.take(12)
    val captureCount = window.count(_.contains("x"))
    val checkCount = window.count(token => token.contains("+") || token.contains("#") || token.toLowerCase.contains("mate"))
    captureCount >= 2 || checkCount >= 2 || (captureCount >= 1 && checkCount >= 1)

  private def variationPreview(variations: List[VariationLine]): Option[String] =
    variations.headOption.flatMap { line =>
      val moves =
        if line.parsedMoves.nonEmpty then line.parsedMoves.take(3).map(_.san.trim).filter(_.nonEmpty)
        else line.moves.take(3).map(_.trim).filter(_.nonEmpty)
      Option.when(moves.nonEmpty)(moves.mkString(" "))
    }

  private def clean(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty).map(UserFacingSignalSanitizer.sanitize)

  private def qualify_text(raw: String): String =
    val low = raw.trim
    if low.startsWith("This opens access") then
      low.replace("This opens access", "This attempts to open access")
    else if low.startsWith("This makes the exchange") then
      low.replace("This makes the exchange", "This prepares the exchange")
    else if low.startsWith("This removes") then
      low.replace("This removes", "This attempts to remove")
    else if low.startsWith("This limits") then
      low.replace("This limits", "This seeks to limit")
    else if low.startsWith("This keeps") then
      low.replace("This keeps", "This tries to keep")
    else if low.startsWith("This continues") then
      low.replace("This continues", "This attempts to continue")
    else if low.startsWith("This cuts down") then
      low.replace("This cuts down", "This aims to cut down")
    else if low.startsWith("This slows down") then
      low.replace("This slows down", "This seeks to slow down")
    else if low.startsWith("This advances") then
      low.replace("This advances", "This plans to advance")
    else if low.startsWith("This supports") then
      low.replace("This supports", "This hopes to support")
    else if low.startsWith("This trade clears") then
      low.replace("This trade clears", "This trade intends to clear")
    else if low.startsWith("This favorable simplification keeps") then
      low.replace("This favorable simplification keeps", "This simplification aims to keep")
    else if low.startsWith("This ") then
      low.replaceFirst("This ", "This tentatively ")
    else
      s"$low (intended)"
