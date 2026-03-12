package lila.llm.analysis

import lila.llm.{ GameNarrativeMoment, StrategyPack, StrategyPieceRoute }

object ActiveThemeSurfaceBuilder:

  val GenericThemeKey = "strategic_campaign"

  final case class ThemeSurface(
      themeKey: String,
      themeLabel: String,
      priority: Int,
      specificityScore: Double,
      planKey: Option[String] = None,
      planLabel: Option[String] = None,
      routeFamilies: Set[String] = Set.empty,
      structuralCue: Option[String] = None,
      zones: Set[String] = Set.empty
  )

  private case class Signals(
      moment: GameNarrativeMoment,
      subplanId: Option[String],
      planKey: Option[String],
      planLabel: Option[String],
      corpus: String,
      routes: List[StrategyPieceRoute],
      routeFamilies: Set[String],
      structuralCue: Option[String],
      zones: Set[String],
      hasDecisionCompare: Boolean,
      hasOpponentCounterplan: Boolean,
      hasRoute: Boolean,
      hasCompensation: Boolean,
      hasProphylaxis: Boolean
  )

  private val ZoneLexicon = List("kingside", "queenside", "center", "central")
  private val ConversionTerms = List("convert", "conversion", "simplif", "passer", "penetrat", "invasion", "endgame")
  private val WholeBoardTerms = List("whole board", "switch", "transfer", "other flank", "across the board")
  private val ExchangeTerms = List("exchange", "trade", "defender trade", "queen trade", "simplification window")
  private val ActiveSuppressionTerms =
    List("counterplay", "active resource", "active piece", "only active", "defender", "neutralize")
  private val RookLiftTerms = List("kingside pressure", "hook", "clamp", "attack", "lift")
  private val InvasionTerms = List("invasion", "penetration", "entry square", "transition")
  private val OppositeBishopTerms =
    List("opposite bishops", "opposite-colored bishops", "opposite coloured bishops")

  def build(moment: GameNarrativeMoment): Option[ThemeSurface] =
    val signals = collectSignals(moment)
    List(
      pickOppositeBishopsConversion(signals),
      pickActivePassiveExchange(signals),
      pickRookLiftAttack(signals),
      pickMinorityAttack(signals),
      pickOutpostEntrenchment(signals),
      pickWholeBoardPlay(signals),
      pickInvasionTransition(signals),
      pickRookPawnMarch(signals),
      pickProphylacticRestriction(signals),
      pickCompensationAttack(signals),
      pickGeneric(signals)
    ).flatten.headOption

  def isCanonical(surface: ThemeSurface): Boolean =
    surface.themeKey != GenericThemeKey

  private def pickOppositeBishopsConversion(signals: Signals): Option[ThemeSurface] =
    val hasTheme =
      signals.subplanId.contains("opposite_bishops_conversion") ||
        containsAny(signals.corpus, OppositeBishopTerms)
    val hasConversion = containsAny(signals.corpus, ConversionTerms)
    Option.when(hasTheme && hasConversion)(theme("opposite_bishops_conversion", "Opposite-Coloured Bishops Conversion", 1, signals))

  private def pickActivePassiveExchange(signals: Signals): Option[ThemeSurface] =
    val hasExchange =
      Set("simplification_window", "defender_trade", "queen_trade_shield").contains(signals.subplanId.orNull) ||
        containsAny(signals.corpus, ExchangeTerms)
    val hasSuppression =
      signals.hasOpponentCounterplan ||
        signals.hasProphylaxis ||
        containsAny(signals.corpus, ActiveSuppressionTerms)
    Option.when(hasExchange && hasSuppression)(theme("active_passive_exchange", "Active-Passive Exchange", 2, signals))

  private def pickRookLiftAttack(signals: Signals): Option[ThemeSurface] =
    val rookLiftRoute =
      signals.routes.exists { route =>
        normalize(route.piece) == "r" &&
        route.route.exists(square => square.endsWith("3") || square.endsWith("4")) &&
        containsAny(normalize(route.purpose), RookLiftTerms)
      }
    val routeTheme =
      signals.subplanId.contains("rook_lift_scaffold") ||
        containsAny(signals.corpus, List("rook lift", "rooklift"))
    Option.when(rookLiftRoute || routeTheme)(theme("rook_lift_attack", "Rook-Lift Attack", 3, signals))

  private def pickMinorityAttack(signals: Signals): Option[ThemeSurface] =
    Option.when(
      signals.subplanId.contains("minority_attack_fixation") ||
        containsAny(signals.corpus, List("minority attack"))
    )(theme("minority_attack", "Minority Attack", 4, signals))

  private def pickOutpostEntrenchment(signals: Signals): Option[ThemeSurface] =
    Option.when(
      signals.subplanId.contains("outpost_entrenchment") ||
        containsAny(signals.corpus, List("outpost", "entrenched piece", "entrench"))
    )(theme("outpost_entrenchment", "Outpost Entrenchment", 4, signals))

  private def pickWholeBoardPlay(signals: Signals): Option[ThemeSurface] =
    val hasMultipleZones = signals.zones.size >= 2
    val hasTransferCue =
      signals.hasRoute ||
        signals.subplanId.contains("invasion_transition") ||
        containsAny(signals.corpus, WholeBoardTerms ++ InvasionTerms)
    Option.when(hasMultipleZones && hasTransferCue)(
      theme("whole_board_play", "Whole-Board Play", 5, signals)
    )

  private def pickInvasionTransition(signals: Signals): Option[ThemeSurface] =
    Option.when(
      signals.subplanId.contains("invasion_transition") ||
        (containsAny(signals.corpus, InvasionTerms) && (signals.hasRoute || signals.hasDecisionCompare))
    )(theme("invasion_transition", "Invasion Transition", 6, signals))

  private def pickRookPawnMarch(signals: Signals): Option[ThemeSurface] =
    Option.when(
      signals.subplanId.contains("rook_pawn_march") ||
        containsAny(signals.corpus, List("rook-pawn march", "pawn storm"))
    )(theme("rook_pawn_march", "Rook-Pawn March", 7, signals))

  private def pickProphylacticRestriction(signals: Signals): Option[ThemeSurface] =
    Option.when(
      signals.hasProphylaxis ||
        containsAny(signals.corpus, List("restriction", "restrain", "clamp", "counterplay restraint"))
    )(theme("prophylactic_restriction", "Prophylactic Restriction", 8, signals))

  private def pickCompensationAttack(signals: Signals): Option[ThemeSurface] =
    Option.when(signals.hasCompensation)(theme("compensation_attack", "Compensation Attack", 9, signals))

  private def pickGeneric(signals: Signals): Option[ThemeSurface] =
    Option.when(
      signals.planKey.isDefined || signals.planLabel.isDefined || signals.hasRoute || signals.structuralCue.isDefined
    )(theme(GenericThemeKey, "Strategic Campaign", 10, signals))

  private def theme(
      key: String,
      label: String,
      priority: Int,
      signals: Signals
  ): ThemeSurface =
    ThemeSurface(
      themeKey = key,
      themeLabel = label,
      priority = priority,
      specificityScore = (1.18 - (priority * 0.08)).max(0.28),
      planKey = signals.planKey,
      planLabel = signals.planLabel,
      routeFamilies = signals.routeFamilies,
      structuralCue = signals.structuralCue,
      zones = signals.zones
    )

  private def collectSignals(moment: GameNarrativeMoment): Signals =
    val digest = moment.signalDigest
    val pack = moment.strategyPack
    val routes = pack.toList.flatMap(_.pieceRoutes)
    val routeFamilies = routes.flatMap(routeFamily).toSet
    val planKey =
      moment.activePlan.flatMap(_.subplanId).map(normalize).filter(_.nonEmpty)
        .orElse(pack.toList.flatMap(_.plans.headOption.map(_.planName)).map(normalize).find(_.nonEmpty))
    val planLabel =
      moment.activePlan.map(_.themeL1).flatMap(nonEmpty)
        .orElse(pack.flatMap(_.plans.headOption.map(_.planName)).flatMap(nonEmpty))
    val structuralCue =
      digest.flatMap(_.structureProfile).flatMap(nonEmpty).map(normalize)
        .orElse(digest.flatMap(_.structuralCue).flatMap(nonEmpty).map(normalize))
    val corpus = buildCorpus(moment, pack)
    val zones = ZoneLexicon.filter(corpus.contains).toSet

    Signals(
      moment = moment,
      subplanId = moment.activePlan.flatMap(_.subplanId).map(normalize).filter(_.nonEmpty),
      planKey = planKey,
      planLabel = planLabel,
      corpus = corpus,
      routes = routes,
      routeFamilies = routeFamilies,
      structuralCue = structuralCue,
      zones = zones,
      hasDecisionCompare = digest.flatMap(_.decisionComparison).isDefined,
      hasOpponentCounterplan =
        digest.flatMap(_.opponentPlan).flatMap(nonEmpty).isDefined ||
          digest.flatMap(_.prophylaxisThreat).flatMap(nonEmpty).isDefined,
      hasRoute = routes.nonEmpty,
      hasCompensation =
        digest.flatMap(_.compensation).flatMap(nonEmpty).isDefined ||
          digest.flatMap(_.investedMaterial).exists(_ > 0),
      hasProphylaxis =
        digest.flatMap(_.prophylaxisPlan).flatMap(nonEmpty).isDefined ||
          digest.flatMap(_.prophylaxisThreat).flatMap(nonEmpty).isDefined ||
          digest.flatMap(_.counterplayScoreDrop).exists(_ > 0)
    )

  private def buildCorpus(moment: GameNarrativeMoment, pack: Option[StrategyPack]): String =
    normalize(
      List(
        Some(moment.momentType),
        moment.moveClassification,
        moment.activePlan.map(_.themeL1),
        moment.activePlan.flatMap(_.subplanId),
        pack.map(_.plans.map(_.planName).mkString(" ")),
        pack.map(_.plans.flatMap(_.priorities).mkString(" ")),
        pack.map(_.plans.flatMap(_.riskTriggers).mkString(" ")),
        pack.map(_.pieceRoutes.map(_.purpose).mkString(" ")),
        pack.map(_.longTermFocus.mkString(" ")),
        pack.map(_.evidence.mkString(" ")),
        moment.signalDigest.flatMap(_.structuralCue),
        moment.signalDigest.flatMap(_.structureProfile),
        moment.signalDigest.flatMap(_.deploymentPurpose),
        moment.signalDigest.flatMap(_.deploymentContribution),
        moment.signalDigest.flatMap(_.decision),
        moment.signalDigest.flatMap(_.strategicFlow),
        moment.signalDigest.flatMap(_.opponentPlan),
        moment.signalDigest.flatMap(_.prophylaxisPlan),
        moment.signalDigest.flatMap(_.prophylaxisThreat),
        moment.signalDigest.flatMap(_.compensation),
        moment.signalDigest.flatMap(_.decisionComparison).flatMap(_.deferredReason),
        moment.signalDigest.flatMap(_.decisionComparison).flatMap(_.evidence),
        Some(moment.authorEvidence.flatMap(ev => List(ev.question, ev.why.getOrElse("")) ++ ev.linkedPlans).mkString(" "))
      ).flatten.mkString(" ")
    )

  private def routeFamily(route: StrategyPieceRoute): Option[String] =
    val piece = normalize(route.piece)
    val purpose = normalize(route.purpose)
    val squares = route.route.map(normalize).filter(_.nonEmpty)
    Option.when(piece.nonEmpty || purpose.nonEmpty || squares.nonEmpty)(
      List(piece, purpose, squares.lastOption.getOrElse("")).filter(_.nonEmpty).mkString("|")
    )

  private def containsAny(corpus: String, terms: List[String]): Boolean =
    terms.exists(term => corpus.contains(normalize(term)))

  private def normalize(text: String): String =
    Option(text)
      .getOrElse("")
      .toLowerCase
      .replaceAll("""[^\p{L}\p{N}]+""", " ")
      .replaceAll("""\s+""", " ")
      .trim

  private def nonEmpty(text: String): Option[String] =
    Option(text).map(_.trim).filter(_.nonEmpty)
