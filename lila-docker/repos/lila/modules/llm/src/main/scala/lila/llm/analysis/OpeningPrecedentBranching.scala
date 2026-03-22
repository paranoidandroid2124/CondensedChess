package lila.llm.analysis

import lila.llm.model.*

private[analysis] enum OpeningBranchMechanism:
  case TacticalPressure
  case ExchangeCascade
  case PromotionRace
  case StructuralTransformation
  case InitiativeSwing

private[analysis] final case class OpeningBranchPrecedent(
    branchLabel: String,
    mechanism: OpeningBranchMechanism,
    mechanismSummary: String,
    triggerMove: Option[String],
    routePreview: String,
    gameDescriptor: String,
    winnerSummary: Option[String],
    score: Int,
    confidence: Double
):
  def representativeSentence: String =
    val winnerPart = winnerSummary.map(summary => s", where $summary").getOrElse("")
    s"In $gameDescriptor, after $routePreview, play headed into the $branchLabel branch$winnerPart."

  def summarySentence: String =
    s"The clearest master precedent here points to the $branchLabel branch, where $mechanismSummary."

private[analysis] object OpeningPrecedentBranching:

  private[analysis] def normalizePlayerName(name: String): Option[String] =
    normalizePlayer(name)

  private[analysis] def precedentSanMoves(pgn: Option[String]): List[String] =
    openingPrecedentSanMoves(pgn)

  private[analysis] def inferMechanismFromSanMoves(sanMoves: List[String]): OpeningBranchMechanism =
    inferMechanism(sanMoves)

  def representative(
      ctx: NarrativeContext,
      openingRef: Option[OpeningReference],
      requireFocus: Boolean
  ): Option[OpeningBranchPrecedent] =
    val focusMoves = focusMovesOf(ctx)
    val planHint = planHintOf(ctx)
    val structureHint = structureHintOf(ctx)
    val branchHint = branchHintOf(ctx)
    openingRef.toList
      .flatMap(_.sampleGames)
      .flatMap { game =>
        buildPrecedent(
          game = game,
          focusMoves = focusMoves,
          planHint = planHint,
          structureHint = structureHint,
          branchHint = branchHint,
          requireFocus = requireFocus
        )
      }
      .sortBy(p => (-p.score, -p.confidence, p.gameDescriptor))
      .headOption

  def representativeSentence(
      ctx: NarrativeContext,
      openingRef: Option[OpeningReference],
      requireFocus: Boolean
  ): Option[String] =
    representative(ctx, openingRef, requireFocus).map(_.representativeSentence)

  def summarySentence(
      ctx: NarrativeContext,
      openingRef: Option[OpeningReference],
      requireFocus: Boolean
  ): Option[String] =
    representative(ctx, openingRef, requireFocus).map(_.summarySentence)

  def relationSentence(
      ctx: NarrativeContext,
      openingRef: Option[OpeningReference],
      requireFocus: Boolean
  ): Option[String] =
    representative(ctx, openingRef, requireFocus).map(renderRelationSentence(ctx, _))

  private def buildPrecedent(
      game: ExplorerGame,
      focusMoves: Set[String],
      planHint: Option[String],
      structureHint: Option[String],
      branchHint: Option[String],
      requireFocus: Boolean
  ): Option[OpeningBranchPrecedent] =
    val snippet = formatSnippet(game)
    val sanMoves = openingPrecedentSanMoves(game.pgn)
    val routePreview = routePreviewOf(sanMoves)
    val mechanism = inferMechanism(sanMoves)
    val overlap = overlapScore(focusMoves, sanMoves)
    val metadataScore = metadataScoreOf(game)
    val compatibilityScore = compatibilityScoreOf(planHint, structureHint, branchHint, mechanism)
    val routeScore = if sanMoves.size >= 4 then 2 else if sanMoves.nonEmpty then 1 else 0
    val totalScore = metadataScore + compatibilityScore + routeScore + overlapBonus(overlap, requireFocus)
    val confidence =
      ((overlapConfidence(overlap) * 0.45) +
        ((metadataScore.toDouble / 12.0).min(1.0) * 0.30) +
        (mechanismConfidence(mechanism, planHint, structureHint) * 0.25))
        .max(0.0)
        .min(1.0)
    val branchLabel = branchLabelOf(planHint, branchHint, structureHint, mechanism)
    val mechanismSummary = mechanismSummaryOf(mechanism, branchLabel)
    for
      text <- snippet
      route <- routePreview
      if totalScore > 0
    yield
      OpeningBranchPrecedent(
        branchLabel = branchLabel,
        mechanism = mechanism,
        mechanismSummary = mechanismSummary,
        triggerMove = sanMoves.headOption,
        routePreview = route,
        gameDescriptor = text,
        winnerSummary = winnerSummaryOf(game),
        score = totalScore,
        confidence = confidence
      )

  private def renderRelationSentence(ctx: NarrativeContext, precedent: OpeningBranchPrecedent): String =
    val played = ctx.playedSan.map(normalizeMoveToken).filter(_.nonEmpty)
    val trigger = precedent.triggerMove.map(normalizeMoveToken).filter(_.nonEmpty)
    val topReferenceMoves =
      openingRefMovesOf(ctx).map(normalizeMoveToken).filter(_.nonEmpty).toSet
    val followsRepresentative = played.exists(p => trigger.contains(p))
    val staysWithinReference = followsRepresentative || played.exists(topReferenceMoves.contains)
    val planLabel = branchPlanLabelOf(ctx, precedent)
    ctx.openingEvent match
      case Some(OpeningEvent.OutOfBook(_, _, _)) =>
        s"The current move bends away from the established ${precedent.branchLabel} branch and instead tries to justify $planLabel over the board."
      case Some(OpeningEvent.Novelty(_, _, _, _)) =>
        s"The current move deliberately bends away from the usual ${precedent.branchLabel} branch, betting that $planLabel will compensate."
      case Some(OpeningEvent.BranchPoint(_, _, _)) if followsRepresentative =>
        s"The current move keeps the game inside that ${precedent.branchLabel} branch rather than forcing a new split."
      case Some(OpeningEvent.BranchPoint(_, _, _)) =>
        val continuation =
          if normalizeText(planLabel).equalsIgnoreCase(normalizeText(precedent.branchLabel)) then
            "but with a different move order"
          else s"and points instead toward $planLabel"
        s"Here the move steps away from the precedent's ${precedent.branchLabel} route $continuation."
      case Some(OpeningEvent.TheoryEnds(_, _)) =>
        s"The current move still leans on that ${precedent.branchLabel} branch, but from here the plans have to be justified without much theory support."
      case Some(OpeningEvent.Intro(_, _, _, _)) if staysWithinReference =>
        s"So the move stays within the classical ${precedent.branchLabel} branch."
      case Some(OpeningEvent.Intro(_, _, _, _)) =>
        s"So the move already shades away from the classical ${precedent.branchLabel} branch toward $planLabel."
      case _ if staysWithinReference =>
        s"The current move keeps to that ${precedent.branchLabel} branch."
      case _ =>
        s"The current move bends the game away from that ${precedent.branchLabel} branch toward $planLabel."

  private def focusMovesOf(ctx: NarrativeContext): Set[String] =
    val played = ctx.playedSan.toList
    val best = ctx.engineEvidence.flatMap(_.best).flatMap(_.ourMove.map(_.san)).toList
    val candidateMoves = ctx.candidates.take(3).map(_.move)
    val openingTopMoves = ctx.openingData.toList.flatMap(_.topMoves.take(2).map(_.san))
    (played ++ best ++ candidateMoves ++ openingTopMoves)
      .map(normalizeMoveToken)
      .filter(_.nonEmpty)
      .toSet

  private def overlapScore(focusMoves: Set[String], sanMoves: List[String]): Int =
    if focusMoves.isEmpty then 0
    else sanMoves.map(normalizeMoveToken).count(focusMoves.contains)

  private def overlapBonus(overlap: Int, requireFocus: Boolean): Int =
    overlap match
      case n if n >= 3 => 12
      case 2           => 8
      case 1           => 4
      case _ if requireFocus => -6
      case _           => 0

  private def overlapConfidence(overlap: Int): Double =
    overlap match
      case n if n >= 3 => 1.0
      case 2           => 0.82
      case 1           => 0.62
      case _           => 0.35

  private def mechanismConfidence(
      mechanism: OpeningBranchMechanism,
      planHint: Option[String],
      structureHint: Option[String]
  ): Double =
    val plan = planHint.getOrElse("")
    val structure = structureHint.getOrElse("")
    mechanism match
      case OpeningBranchMechanism.StructuralTransformation if structure.nonEmpty => 0.95
      case OpeningBranchMechanism.TacticalPressure if plan.contains("attack") || plan.contains("pressure") => 0.92
      case OpeningBranchMechanism.InitiativeSwing if plan.contains("initiative") || plan.contains("pressure") => 0.88
      case OpeningBranchMechanism.ExchangeCascade if plan.contains("exchange") || plan.contains("simpl") => 0.9
      case OpeningBranchMechanism.PromotionRace if plan.contains("pawn") || plan.contains("passer") => 0.86
      case _ => 0.68

  private def metadataScoreOf(game: ExplorerGame): Int =
    (if game.year > 0 then 3 else 0) +
      (if game.winner.isDefined then 2 else 0) +
      (if normalizePlayer(game.white.name).isDefined then 1 else 0) +
      (if normalizePlayer(game.black.name).isDefined then 1 else 0) +
      (if game.event.exists(_.trim.nonEmpty) then 2 else 0) +
      (if game.pgn.exists(_.trim.nonEmpty) then 3 else 0)

  private def compatibilityScoreOf(
      planHint: Option[String],
      structureHint: Option[String],
      branchHint: Option[String],
      mechanism: OpeningBranchMechanism
  ): Int =
    val plan = planHint.getOrElse("")
    val structure = structureHint.getOrElse("")
    val branch = branchHint.getOrElse("")
    val planCompat =
      mechanism match
        case OpeningBranchMechanism.TacticalPressure =>
          if plan.contains("attack") || plan.contains("pressure") || branch.contains("novelty") then 3 else 0
        case OpeningBranchMechanism.InitiativeSwing =>
          if plan.contains("initiative") || plan.contains("pressure") || branch.contains("branch point") then 3 else 0
        case OpeningBranchMechanism.ExchangeCascade =>
          if plan.contains("exchange") || plan.contains("simpl") || branch.contains("theory ends") then 3 else 0
        case OpeningBranchMechanism.PromotionRace =>
          if plan.contains("pawn") || plan.contains("passer") then 3 else 0
        case OpeningBranchMechanism.StructuralTransformation =>
          if structure.nonEmpty || plan.contains("minority") || plan.contains("chain") || plan.contains("clamp") then 3 else 0
    val branchCompat =
      mechanism match
        case OpeningBranchMechanism.StructuralTransformation if branch.contains("theory") => 1
        case OpeningBranchMechanism.InitiativeSwing if branch.contains("branch point") || branch.contains("out of book") => 1
        case OpeningBranchMechanism.TacticalPressure if branch.contains("novelty") => 1
        case _ => 0
    planCompat + branchCompat

  private def branchLabelOf(
      planHint: Option[String],
      branchHint: Option[String],
      structureHint: Option[String],
      mechanism: OpeningBranchMechanism
  ): String =
    val raw =
      planHint
        .orElse(branchHint)
        .orElse(structureHint.map(s => s"$s structure play"))
        .getOrElse(defaultBranchLabel(mechanism))
    normalizeBranchLabel(raw)

  private def defaultBranchLabel(mechanism: OpeningBranchMechanism): String =
    mechanism match
      case OpeningBranchMechanism.TacticalPressure      => "forcing initiative"
      case OpeningBranchMechanism.ExchangeCascade       => "simplification"
      case OpeningBranchMechanism.PromotionRace         => "passed-pawn race"
      case OpeningBranchMechanism.StructuralTransformation => "structural transformation"
      case OpeningBranchMechanism.InitiativeSwing       => "piece-activity initiative"

  private def mechanismSummaryOf(mechanism: OpeningBranchMechanism, branchLabel: String): String =
    mechanism match
      case OpeningBranchMechanism.TacticalPressure =>
        s"forcing threats and king-safety questions begin to decide the $branchLabel line"
      case OpeningBranchMechanism.ExchangeCascade =>
        s"exchange timing clarifies which side reaches the cleaner version of the $branchLabel line"
      case OpeningBranchMechanism.PromotionRace =>
        s"passed-pawn tempo counts start to dominate the $branchLabel line"
      case OpeningBranchMechanism.StructuralTransformation =>
        s"pawn-structure changes reroute the long-term plans inside the $branchLabel line"
      case OpeningBranchMechanism.InitiativeSwing =>
        s"piece activity and tempo swings decide who keeps the initiative in the $branchLabel line"

  private def inferMechanism(sanMoves: List[String]): OpeningBranchMechanism =
    val captures = sanMoves.count(_.contains("x"))
    val checks = sanMoves.count(m => m.contains("+") || m.contains("#"))
    val promotions = sanMoves.count(_.contains("="))
    val pawnPushes = sanMoves.count(isLikelyPawnMove)
    val pieceMoves = sanMoves.count(isPieceMove)
    val forcingDensity =
      if sanMoves.nonEmpty then (captures + checks + promotions).toDouble / sanMoves.size.toDouble
      else 0.0
    val mechanismScores = Map(
      OpeningBranchMechanism.TacticalPressure ->
        (checks * 2 + captures + Option.when(forcingDensity >= 0.45)(1).getOrElse(0)),
      OpeningBranchMechanism.ExchangeCascade ->
        (captures * 2 + Option.when(captures >= 2)(2).getOrElse(0) + Option.when(pieceMoves >= 2)(1).getOrElse(0)),
      OpeningBranchMechanism.PromotionRace ->
        (promotions * 3 + Option.when(captures >= 1)(1).getOrElse(0) + Option.when(checks >= 1)(1).getOrElse(0)),
      OpeningBranchMechanism.StructuralTransformation ->
        (pawnPushes * 2 + Option.when(captures <= 1)(1).getOrElse(0) + Option.when(pieceMoves >= 1)(1).getOrElse(0)),
      OpeningBranchMechanism.InitiativeSwing ->
        (pieceMoves + Option.when(captures == 1)(1).getOrElse(0) + Option.when(checks == 0)(1).getOrElse(0))
    )
    mechanismScores.maxBy(_._2)._1

  private def formatSnippet(game: ExplorerGame): Option[String] =
    val whiteName = normalizePlayer(game.white.name)
    val blackName = normalizePlayer(game.black.name)
    val year = Option.when(game.year > 0)(game.year)
    for
      white <- whiteName
      black <- blackName
      y <- year
    yield
      val eventSuffix = game.event.map(_.trim).filter(_.nonEmpty).map(ev => s", $ev").getOrElse("")
      s"$white-$black ($y$eventSuffix)"

  private def winnerSummaryOf(game: ExplorerGame): Option[String] =
    game.winner.flatMap { color =>
      val winner =
        if color == chess.White then normalizePlayer(game.white.name)
        else normalizePlayer(game.black.name)
      winner.map { name =>
        val result = if color == chess.White then "1-0" else "0-1"
        s"$name eventually won ($result)"
      }
    }

  private def routePreviewOf(sanMoves: List[String]): Option[String] =
    val route = sanMoves.take(3).map(_.trim).filter(_.nonEmpty)
    Option.when(route.nonEmpty)(route.mkString(" "))

  private def openingPrecedentSanMoves(pgn: Option[String]): List[String] =
    val resultTokens = Set("1-0", "0-1", "1/2-1/2", "*")
    pgn.toList
      .flatMap(_.trim.split("\\s+").toList)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replaceAll("""^\d+\.(?:\.\.)?""", ""))
      .map(_.replaceAll("""^\.\.\.""", ""))
      .filter(token => token.nonEmpty && !resultTokens.contains(token))

  private def normalizePlayer(name: String): Option[String] =
    Option(name)
      .map(_.trim)
      .filter(n => n.nonEmpty && n != "?")
      .map { n =>
        val parts = n.split(",").map(_.trim).filter(_.nonEmpty).toList
        parts match
          case last :: first :: Nil => s"$first $last"
          case _                    => n
      }

  private def planHintOf(ctx: NarrativeContext): Option[String] =
    StrategicNarrativePlanSupport.evidenceBackedLeadingPlanName(ctx)
      .map(normalizeText)
      .filter(_.nonEmpty)
      .map(_.toLowerCase)

  private def openingRefMovesOf(ctx: NarrativeContext): List[String] =
    ctx.openingData.toList.flatMap(_.topMoves.take(3).map(_.san))

  private def structureHintOf(ctx: NarrativeContext): Option[String] =
    ctx.semantic.flatMap(_.structureProfile).map(_.primary)
      .map(normalizeText)
      .filter(_.nonEmpty)
      .map(_.toLowerCase)

  private def branchHintOf(ctx: NarrativeContext): Option[String] =
    ctx.openingEvent.flatMap {
      case OpeningEvent.Intro(_, _, theme, _) =>
        Option(theme).map(normalizeText).filter(_.nonEmpty)
      case OpeningEvent.BranchPoint(_, reason, _) =>
        Option(reason).map(normalizeText).filter(_.nonEmpty)
      case OpeningEvent.OutOfBook(_, _, _) =>
        Some("out of book")
      case OpeningEvent.TheoryEnds(_, _) =>
        Some("theory ends")
      case OpeningEvent.Novelty(_, _, _, _) =>
        Some("novelty")
    }.map(_.toLowerCase)

  private def normalizeBranchLabel(raw: String): String =
    val normalized = normalizeText(raw).toLowerCase
    if normalized.endsWith(" branch") then normalized.stripSuffix(" branch")
    else normalized

  private def branchPlanLabelOf(ctx: NarrativeContext, precedent: OpeningBranchPrecedent): String =
    planHintOf(ctx)
      .orElse(structureHintOf(ctx).map(s => s"$s structure play"))
      .getOrElse(precedent.branchLabel)

  private def normalizeMoveToken(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
      .replaceAll("""^\d+\.(?:\.\.)?\s*""", "")
      .replaceAll("""^\.{2,}\s*""", "")
      .replaceAll("""[+#?!]+$""", "")
      .replaceAll("\\s+", "")

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""[_\-]+""", " ").replaceAll("\\s+", " ").trim

  private def isLikelyPawnMove(move: String): Boolean =
    Option(move).getOrElse("").trim.matches("""^[a-h](?:x[a-h])?[1-8](?:=[QRBN])?[+#]?$""")

  private def isPieceMove(move: String): Boolean =
    Option(move).getOrElse("").headOption.exists(ch => "KQRBN".contains(ch))
