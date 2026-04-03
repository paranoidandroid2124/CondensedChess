package lila.llm.analysis

import lila.llm.{ RouteSurfaceMode, StrategyPack }
import lila.llm.model.*
import lila.llm.model.strategic.{ VariationLine, VariationTag }

private[llm] enum PlayerFacingClaimScope:
  case MoveLocal
  case LineScoped

private[llm] final case class MainPathScopedClaim(
    scope: PlayerFacingClaimScope,
    mode: PlayerFacingTruthMode,
    deltaClass: Option[PlayerFacingMoveDeltaClass],
    claimText: String,
    anchorTerms: List[String],
    evidenceLines: List[String],
    sourceKind: String,
    tacticalOwnership: Option[String],
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

private[llm] final case class MainPathClaimBundle(
    mainClaim: Option[MainPathScopedClaim],
    lineScopedClaim: Option[MainPathScopedClaim]
):
  def primaryClaim: Option[MainPathScopedClaim] = mainClaim.orElse(lineScopedClaim)

private[llm] object MainPathMoveDeltaClaimBuilder:

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
          delta.packet.anchorTerms
            .filter(_.trim.nonEmpty)
            .distinct
        val sourceKind = mainSourceKind(surface, truthContract)
        val lineEvidence =
          Option.when(delta.allowsLineEvidenceHook) {
            strategicEvidenceLines(
              delta = delta,
              ctx = ctx,
              strategyPack = strategyPack,
              surface = surface,
              truthContract = truthContract,
              candidateEvidenceLines = candidateEvidenceLines
            )
          }.getOrElse(Nil)
        val mainClaim =
          strategicClaim(delta, ctx, surface, truthContract)
            .flatMap(clean)
            .filter(_ => delta.allowsWeakMainClaim)
            .filter(text =>
              PlayerFacingTruthModePolicy.allowsStrategicClaimText(
                text,
                ctx,
                strategyPack,
                truthContract
              )
            )
            .map { text =>
              MainPathScopedClaim(
                scope = PlayerFacingClaimScope.MoveLocal,
                mode = PlayerFacingTruthMode.Strategic,
                deltaClass = Some(delta.deltaClass),
                claimText = text,
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
      surface: StrategyPackSurface.Snapshot,
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
        anchor.map { square =>
          delta.modalityTier match
            case PlayerFacingClaimModalityTier.Supports => s"This increases pressure on $square."
            case _                                      => s"This continues to allow pressure on $square."
        }
      case PlayerFacingMoveDeltaClass.ExchangeForcing =>
        anchor.map { square =>
          delta.modalityTier match
            case PlayerFacingClaimModalityTier.Forces => s"This makes the exchange on $square hard to avoid."
            case _                                    => s"This keeps the exchange on $square available."
        }
      case PlayerFacingMoveDeltaClass.CounterplayReduction =>
        val namedBreakOnly = delta.packet.ownerFamily == "neutralize_key_break"
        val namedResourceOnly = delta.packet.ownerFamily == "counterplay_restraint"
        strategicCounterplayClaim(ctx, delta)
          .orElse {
            Option.unless(namedBreakOnly || namedResourceOnly) {
              anchor.map { focal =>
                delta.ontologyFamily match
                  case PlayerFacingClaimOntologyFamily.RouteDenial =>
                    "This keeps the opponent from getting easy entry there."
                  case PlayerFacingClaimOntologyFamily.ColorComplexSqueeze =>
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
        anchor.map { square =>
          delta.modalityTier match
            case PlayerFacingClaimModalityTier.Advances => s"This advances the plan toward $square."
            case PlayerFacingClaimModalityTier.Supports => s"This supports the plan toward $square."
            case _                                      => s"This keeps the plan toward $square available."
        }

  private def strategicCounterplayClaim(
      ctx: NarrativeContext,
      delta: PlayerFacingMoveDeltaEvidence
  ): Option[String] =
    Option.unless(HeavyPieceLocalBindValidation.blocksPlayerFacingShell(ctx)) {
      val namedBreakOnly = delta.packet.ownerFamily == "neutralize_key_break"
      val namedResourceOnly = delta.packet.ownerFamily == "counterplay_restraint"
      val preventedPlans =
        ctx.semantic.toList.flatMap(_.preventedPlans)
      LocalFileEntryBindCertification.certifiedSurfacePair(ctx)
        .map(pair =>
          s"This keeps ${pair.entrySquare} closed and takes the ${pair.file} away as a counterplay route."
        )
        .orElse {
          preventedPlans
            .find(_.sourceScope == FactScope.Now)
            .flatMap { prevented =>
              delta.ontologyFamily match
                case PlayerFacingClaimOntologyFamily.RouteDenial =>
                  prevented.deniedSquares.headOption
                    .flatMap(clean)
                    .map(square => s"This keeps the opponent out of $square.")
                case PlayerFacingClaimOntologyFamily.ColorComplexSqueeze =>
                  clean(prevented.planId)
                    .filterNot(_.equalsIgnoreCase("counterplay"))
                    .map(plan => s"This keeps a longer squeeze on $plan.")
                case _ =>
                  prevented.breakNeutralized
                    .flatMap(clean)
                    .map(file => s"This keeps $file from coming right away.")
                    .orElse(Option.unless(namedBreakOnly) {
                      prevented.deniedSquares.headOption
                        .flatMap(clean)
                        .map(square => s"This keeps the opponent out of $square.")
                    }.flatten)
                    .orElse(Option.unless(namedBreakOnly) {
                      clean(prevented.planId)
                        .filterNot(_.equalsIgnoreCase("counterplay"))
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
          case PlayerFacingClaimOntologyFamily.RouteDenial =>
            prevented.deniedSquares.headOption
              .flatMap(clean)
              .map(square => s"This $verb entry on $square as a defensive resource.")
          case _ =>
            prevented.breakNeutralized
              .flatMap(clean)
              .map(file => s"This $verb $file as a defensive resource.")
              .orElse(
                prevented.deniedSquares.headOption
                  .flatMap(clean)
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
          if contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense ||
            contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation =>
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
        case DecisiveTruthClass.Best if contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense =>
          Some("only_move_defense")
        case DecisiveTruthClass.Best if contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation =>
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

  private def looksLikeTacticalSacrifice(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    truthContract.exists(contract =>
      contract.reasonFamily == DecisiveReasonFamily.InvestmentSacrifice ||
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
