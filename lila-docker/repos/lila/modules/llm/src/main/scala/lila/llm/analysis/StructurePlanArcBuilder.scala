package lila.llm.analysis

import _root_.chess.{ Board, Color, File, Role, Square }
import _root_.chess.format.Fen
import lila.llm.RouteSurfaceMode
import lila.llm.model.*
import lila.llm.model.strategic.{ PieceActivity, PositionalTag }

private[analysis] final case class PieceDeploymentCue(
    ownerSide: String,
    piece: String,
    from: String,
    route: List[String],
    destination: String,
    purpose: String,
    strategicFit: Double,
    tacticalSafety: Double,
    surfaceConfidence: Double,
    surfaceMode: String,
    source: String
):
  def routeSquares: List[String] = (from :: route).distinct.take(4)
  def lastSquare: Option[String] = routeSquares.lastOption
  def confidence: Double = surfaceConfidence

private[analysis] final case class RouteSafetyAssessment(
    tacticalSafety: Double,
    hasEnemyOccupied: Boolean,
    transitSquaresEmpty: Boolean
)

private[analysis] final case class StructurePlanArc(
    structureLabel: String,
    centerState: Option[String],
    planLabel: String,
    alignmentBand: Option[String],
    alignmentReasons: List[String],
    primaryDeployment: PieceDeploymentCue,
    secondaryDeployment: Option[PieceDeploymentCue],
    moveContribution: String,
    prophylaxisSupport: Option[String],
    practicalCoda: Option[String],
    compensationCoda: Option[String]
)

private[analysis] object StructurePlanArcBuilder:

  val ProseConfidenceCutoff = 0.55
  val SecondaryConfidenceCutoff = 0.55
  val ExactRouteCutoff = 0.82

  def build(ctx: NarrativeContext): Option[StructurePlanArc] =
    val semantic = ctx.semantic
    val structure = semantic.flatMap(_.structureProfile)
    val alignment = semantic.flatMap(_.planAlignment)
    val pieceActivity = semantic.toList.flatMap(_.pieceActivity)
    val boardOpt = Fen.read(_root_.chess.variant.Standard, Fen.Full(ctx.fen)).map(_.board)
    val ownerSide = sideNameFromFen(ctx.fen)
    val structureLabel =
      structure.flatMap(sp => normalized(sp.primary).filterNot(_.equalsIgnoreCase("Unknown")))
        .orElse(alignment.flatMap(_.narrativeIntent).flatMap(normalized).map(capitalizeFirst))
    val planLabel = leadingPlanLabel(ctx, alignment)

    for
      structureName <- structureLabel
      plan <- planLabel
      primary <- selectDeployments(pieceActivity, boardOpt, ownerSide, plan, structureName, alignment.map(_.band)).headOption
    yield
      val deployments = selectDeployments(pieceActivity, boardOpt, ownerSide, plan, structureName, alignment.map(_.band))
      val alignmentBand = alignment.map(_.band).flatMap(normalized)
      val alignmentReasons = alignment.toList.flatMap(_.reasonCodes).flatMap(humanizeAlignmentReason).distinct.take(3)
      StructurePlanArc(
        structureLabel = structureName,
        centerState = structure.flatMap(sp => normalized(sp.centerState)),
        planLabel = plan,
        alignmentBand = alignmentBand,
        alignmentReasons = alignmentReasons,
        primaryDeployment = primary,
        secondaryDeployment = deployments.drop(1).find(_.surfaceConfidence >= SecondaryConfidenceCutoff),
        moveContribution = moveContribution(ctx, primary, plan),
        prophylaxisSupport = prophylaxisSupport(ctx),
        practicalCoda = practicalCoda(ctx),
        compensationCoda = compensationCoda(ctx)
      )

  def proseEligible(arc: StructurePlanArc): Boolean =
    alignmentSupportsProse(arc.alignmentBand) && arc.primaryDeployment.surfaceConfidence >= ProseConfidenceCutoff &&
      arc.primaryDeployment.surfaceMode != RouteSurfaceMode.Hidden

  def visibleDeployment(arc: StructurePlanArc): Option[PieceDeploymentCue] =
    Option.when(arc.primaryDeployment.surfaceConfidence >= ProseConfidenceCutoff && arc.primaryDeployment.surfaceMode != RouteSurfaceMode.Hidden)(
      arc.primaryDeployment
    )

  def useExactRoute(cue: PieceDeploymentCue): Boolean =
    cue.surfaceMode == RouteSurfaceMode.Exact

  def claimText(arc: StructurePlanArc): String =
    val centerFragment =
      arc.centerState
        .filterNot(_.equalsIgnoreCase(arc.structureLabel))
        .map(center => s", with a ${center.toLowerCase} center,")
        .getOrElse("")
    s"In the ${arc.structureLabel} structure$centerFragment the long plan is ${arc.planLabel}, and the ${pieceWord(arc.primaryDeployment.piece)} belongs on ${destinationPhrase(arc.primaryDeployment)}."

  def supportPrimaryText(arc: StructurePlanArc): String =
    val cue = arc.primaryDeployment
    s"The ${pieceWord(cue.piece)} wants ${routeTargetPhrase(cue)} because that is where ${normalizePurpose(cue.purpose, arc.planLabel)} gains force."

  def supportSecondaryText(arc: StructurePlanArc): String =
    arc.moveContribution

  def cautionSupportText(arc: StructurePlanArc): String =
    s"In this structure the ${pieceWord(arc.primaryDeployment.piece)} still wants ${routeTargetPhrase(arc.primaryDeployment)}, so the move needs extra justification."

  def focusLine(arc: StructurePlanArc): String =
    s"${arc.primaryDeployment.piece} ${routeLead(arc.primaryDeployment)} for ${arc.primaryDeployment.purpose}"

  def cueFromStrategicActivity(
      activity: PieceActivity,
      boardOpt: Option[Board],
      routeColor: Color,
      positionalFeatures: List[PositionalTag],
      planLabels: List[String],
      structureHint: Option[String] = None,
      alignmentBand: Option[String] = None
  ): Option[PieceDeploymentCue] =
    if activity.keyRoutes.isEmpty then None
    else
      boardOpt
        .flatMap(_.pieceAt(activity.square))
        .filter(_.color == routeColor)
        .flatMap { piece =>
          val route = activity.keyRoutes.map(_.key).distinct.take(3)
          Option.when(route.nonEmpty)(
            buildPieceDeploymentCue(
              ownerSide = sideName(routeColor),
              piece = pieceToken(piece.role),
              from = activity.square.key,
              route = route,
              purpose = derivedStrategicPurpose(
                role = piece.role,
                activity = activity,
                destination = route.lastOption.getOrElse(activity.square.key),
                boardOpt = boardOpt,
                routeColor = routeColor,
                positionalFeatures = positionalFeatures,
                planLabels = planLabels,
                structureHint = structureHint
              ),
              strategicFit = strategicRouteFit(activity, alignmentBand, planLabels.headOption.getOrElse("")),
              boardOpt = boardOpt,
              ownerColor = Some(routeColor),
              source = "piece_activity"
            )
          ).flatten
        }

  def evidenceFromStrategicActivity(activity: PieceActivity, destination: String): List[String] =
    List(
      Option.when(activity.isBadBishop)("bishop_quality_signal"),
      Option.when(activity.isTrapped)("trapped_piece_signal"),
      Option.when(activity.mobilityScore < 0.4)("low_mobility_signal"),
      Option.when(activity.keyRoutes.size >= 2)("multi_hop_route"),
      Option.when(activity.coordinationLinks.nonEmpty)(s"coordination_links_${activity.coordinationLinks.size.min(4)}"),
      Option.when(activity.keyRoutes.lastOption.exists(_.key == destination))("destination_from_piece_activity"),
      Some("piece_activity")
    ).flatten

  private def selectDeployments(
      pieceActivity: List[PieceActivityInfo],
      boardOpt: Option[Board],
      ownerSide: String,
      planLabel: String,
      structureLabel: String,
      alignmentBand: Option[String]
  ): List[PieceDeploymentCue] =
    pieceActivity
      .flatMap(buildCue(_, boardOpt, ownerSide, planLabel, structureLabel, alignmentBand))
      .sortBy(cue => (-cue.surfaceConfidence, routeLead(cue), cue.piece))
      .distinctBy(cue => s"${cue.piece}|${cue.from}|${cue.destination}")
      .take(3)

  private def buildCue(
      activity: PieceActivityInfo,
      boardOpt: Option[Board],
      ownerSide: String,
      planLabel: String,
      structureLabel: String,
      alignmentBand: Option[String]
  ): Option[PieceDeploymentCue] =
    val piece = pieceToken(activity.piece)
    val squares = activity.keyRoutes.map(_.trim).filter(_.nonEmpty).distinct.take(3)
    val from = normalized(activity.square)
    for
      p <- piece
      start <- from
      route <- Option.when(squares.nonEmpty)(squares)
      cue <- buildPieceDeploymentCue(
        ownerSide = ownerSide,
        piece = p,
        from = start,
        route = route,
        purpose = deriveSemanticPurpose(p, route, planLabel, structureLabel, activity),
        strategicFit = semanticStrategicFit(activity, alignmentBand, planLabel),
        boardOpt = boardOpt,
        ownerColor = colorFromSide(ownerSide),
        source = "piece_activity"
      )
    yield cue

  private def buildPieceDeploymentCue(
      ownerSide: String,
      piece: String,
      from: String,
      route: List[String],
      purpose: String,
      strategicFit: Double,
      boardOpt: Option[Board],
      ownerColor: Option[Color],
      source: String
  ): Option[PieceDeploymentCue] =
    val routeSquares = (from :: route).distinct.take(4)
    val destinationSquare = route.lastOption.getOrElse(from)
    val safetyAssessment = assessRouteSafety(boardOpt, ownerColor, routeSquares)
    Option.when(!safetyAssessment.hasEnemyOccupied && route.nonEmpty) {
      val destination = fileDestination(piece, destinationSquare, purpose).getOrElse(destinationSquare)
      val surfaceConfidence = strategicFit.min(safetyAssessment.tacticalSafety)
      PieceDeploymentCue(
        ownerSide = ownerSide,
        piece = piece,
        from = from,
        route = route,
        destination = destination,
        purpose = purpose,
        strategicFit = strategicFit,
        tacticalSafety = safetyAssessment.tacticalSafety,
        surfaceConfidence = surfaceConfidence,
        surfaceMode = surfaceModeFor(piece, purpose, routeSquares, surfaceConfidence, safetyAssessment),
        source = source
      )
    }

  private def moveContribution(
      ctx: NarrativeContext,
      cue: PieceDeploymentCue,
      planLabel: String
  ): String =
    parseUci(ctx.playedMove).fold(s"This move supports that route by making ${planLabel.toLowerCase} easier to organize.") {
      case (from, to) if from == cue.from && cue.route.headOption.contains(to) =>
        "This move starts that route immediately."
      case (from, to) if from == cue.from && cue.destination == to =>
        s"This move places the ${pieceWord(cue.piece)} straight onto that post."
      case (_, to) if cue.route.contains(to) =>
        "This move connects with that route directly."
      case (_, to) if sameFile(to, cue.destination) =>
        "This move reinforces the target file for that deployment."
      case (_, to) if cue.lastSquare.exists(isAdjacent(_, to)) =>
        "This move guards the entry squares of that route."
      case _ if isPawnMove(ctx.playedSan) =>
        "This move fixes the structure so that route can become relevant."
      case _ =>
        s"This move supports that route by making ${planLabel.toLowerCase} easier to organize."
    }

  private def prophylaxisSupport(ctx: NarrativeContext): Option[String] =
    ctx.semantic.flatMap(_.preventedPlans.headOption).flatMap { prevented =>
      val target =
        prevented.preventedThreatType.flatMap(normalized)
          .orElse(prevented.breakNeutralized.map(file => s"$file-break"))
          .orElse(normalized(prevented.planId))
      target.map { text =>
        s"That deployment also matters because it cuts out ${text.toLowerCase}."
      }
    }

  private def practicalCoda(ctx: NarrativeContext): Option[String] =
    ctx.semantic.flatMap(_.practicalAssessment).flatMap { practical =>
      normalized(practical.verdict).map { verdict =>
        s"In practical terms the resulting task is ${verdict.toLowerCase.stripSuffix(".")}."
      }
    }

  private def compensationCoda(ctx: NarrativeContext): Option[String] =
    CompensationInterpretation.effectiveSemanticDecision(ctx).flatMap { accepted =>
      accepted.decision.signal.summary.flatMap(normalized).map { plan =>
        s"The long-term pay-off still depends on converting the position into ${plan.toLowerCase}."
      }
    }

  private def leadingPlanLabel(
      ctx: NarrativeContext,
      alignment: Option[PlanAlignmentInfo]
  ): Option[String] =
    ctx.mainStrategicPlans.headOption.flatMap(h => normalized(h.planName))
      .orElse(ctx.plans.top5.headOption.flatMap(p => normalized(p.name)))
      .orElse(alignment.flatMap(_.narrativeIntent).flatMap(normalized).map(capitalizeFirst))

  private def semanticStrategicFit(
      activity: PieceActivityInfo,
      alignmentBand: Option[String],
      planLabel: String
  ): Double =
    strategicFitBase(
      mobilityScore = activity.mobilityScore,
      isTrapped = activity.isTrapped,
      isBadBishop = activity.isBadBishop,
      coordinationCount = activity.coordinationLinks.size,
      routeLength = activity.keyRoutes.size,
      alignmentBand = alignmentBand,
      hasPlanLabel = normalized(planLabel).nonEmpty
    )

  private def strategicRouteFit(
      activity: PieceActivity,
      alignmentBand: Option[String],
      planLabel: String
  ): Double =
    strategicFitBase(
      mobilityScore = activity.mobilityScore,
      isTrapped = activity.isTrapped,
      isBadBishop = activity.isBadBishop,
      coordinationCount = activity.coordinationLinks.size,
      routeLength = activity.keyRoutes.size,
      alignmentBand = alignmentBand,
      hasPlanLabel = normalized(planLabel).nonEmpty
    )

  private def strategicFitBase(
      mobilityScore: Double,
      isTrapped: Boolean,
      isBadBishop: Boolean,
      coordinationCount: Int,
      routeLength: Int,
      alignmentBand: Option[String],
      hasPlanLabel: Boolean
  ): Double =
    val mobilityBonus = (0.60 - mobilityScore).max(0.0) * 0.30
    val trappedBonus = if isTrapped then 0.16 else 0.0
    val bishopBonus = if isBadBishop then 0.12 else 0.0
    val coordinationBonus = coordinationCount.min(3) * 0.03
    val routeBonus = routeLength.min(3) * 0.08
    val alignmentBonus =
      alignmentBand.flatMap(normalized).map(_.toLowerCase) match
        case Some("onbook")   => 0.12
        case Some("playable") => 0.08
        case Some("offplan")  => -0.06
        case Some("unknown")  => -0.08
        case _                => 0.0
    val planBonus = if hasPlanLabel then 0.05 else 0.0
    (0.36 + mobilityBonus + trappedBonus + bishopBonus + coordinationBonus + routeBonus + alignmentBonus + planBonus)
      .max(0.25)
      .min(0.94)

  private def deriveSemanticPurpose(
      piece: String,
      route: List[String],
      planLabel: String,
      structureLabel: String,
      activity: PieceActivityInfo
  ): String =
    purposeBase(
      piece = piece,
      plan = Option(planLabel).getOrElse("").toLowerCase,
      structure = Option(structureLabel).getOrElse("").toLowerCase,
      isTrapped = activity.isTrapped,
      isBadBishop = activity.isBadBishop,
      routeNonEmpty = route.nonEmpty,
      coordinationNonEmpty = activity.coordinationLinks.nonEmpty,
      mobilityScore = activity.mobilityScore,
      outpostSignal = false,
      rookFileSignal = false,
      centerSignal = false,
      planActivationSignal = false
    )

  private def derivedStrategicPurpose(
      role: Role,
      activity: PieceActivity,
      destination: String,
      boardOpt: Option[Board],
      routeColor: Color,
      positionalFeatures: List[PositionalTag],
      planLabels: List[String],
      structureHint: Option[String]
  ): String =
    val destinationSquare = Square.all.find(_.key == destination)
    val outpostSignal =
      positionalFeatures.exists {
        case PositionalTag.Outpost(square, color) =>
          color == routeColor && square.key == destination
        case PositionalTag.StrongKnight(square, color) =>
          color == routeColor && square.key == destination
        case _ => false
      }
    val rookFileSignal =
      role == _root_.chess.Rook &&
        destinationSquare.exists(sq => boardOpt.exists(board => isOpenOrSemiOpenFileFor(board, sq.file, routeColor)))
    val centerSignal = destinationSquare.exists(isCentralSquare)
    val planActivationSignal =
      planLabels.exists { raw =>
        val name = raw.trim.toLowerCase
        (name.contains("attack") && role == _root_.chess.Queen) ||
        (name.contains("file") && role == _root_.chess.Rook) ||
        (name.contains("activation") && (role == _root_.chess.Knight || role == _root_.chess.Bishop))
      }
    purposeBase(
      piece = pieceToken(role),
      plan = planLabels.headOption.getOrElse("").toLowerCase,
      structure = structureHint.getOrElse("").toLowerCase,
      isTrapped = activity.isTrapped,
      isBadBishop = activity.isBadBishop,
      routeNonEmpty = activity.keyRoutes.nonEmpty,
      coordinationNonEmpty = activity.coordinationLinks.nonEmpty,
      mobilityScore = activity.mobilityScore,
      outpostSignal = outpostSignal,
      rookFileSignal = rookFileSignal,
      centerSignal = centerSignal,
      planActivationSignal = planActivationSignal
    )

  private def purposeBase(
      piece: String,
      plan: String,
      structure: String,
      isTrapped: Boolean,
      isBadBishop: Boolean,
      routeNonEmpty: Boolean,
      coordinationNonEmpty: Boolean,
      mobilityScore: Double,
      outpostSignal: Boolean,
      rookFileSignal: Boolean,
      centerSignal: Boolean,
      planActivationSignal: Boolean
  ): String =
    if isBadBishop then "a cleaner bishop circuit"
    else if isTrapped then "piece liberation"
    else if piece == "R" && (plan.contains("minority attack") || plan.contains("queenside pressure")) then "queenside pressure"
    else if piece == "R" && (plan.contains("rook-pawn") || plan.contains("kingside") || plan.contains("attack")) then "kingside clamp"
    else if outpostSignal then "outpost reinforcement"
    else if rookFileSignal then "open-file occupation"
    else if plan.contains("minority attack") then "the minority attack"
    else if plan.contains("queenside pressure") then "queenside pressure"
    else if plan.contains("kingside") || plan.contains("attack") then "kingside pressure"
    else if plan.contains("iqp") || plan.contains("break") then "piece activation before the break"
    else if structure.contains("hedgehog") || structure.contains("maroczy") then "counterplay restraint"
    else if structure.contains("french") || structure.contains("stonewall") then "restriction play"
    else if planActivationSignal then "plan activation lane"
    else if coordinationNonEmpty && routeNonEmpty then "coordination improvement"
    else if centerSignal then "centralization route"
    else if mobilityScore < 0.45 then "mobility recovery"
    else if normalized(plan).nonEmpty then s"$plan pressure"
    else "coordination improvement"

  private def fileDestination(piece: String, destination: String, purpose: String): Option[String] =
    Option.when(piece == "R" && (purpose.contains("pressure") || purpose.contains("clamp")) && destination.matches("""[a-h][1-8]""")) {
      s"${destination.head}-file"
    }

  private def assessRouteSafety(
      boardOpt: Option[Board],
      ownerColor: Option[Color],
      routeSquares: List[String]
  ): RouteSafetyAssessment =
    (boardOpt, ownerColor) match
      case (Some(board), Some(color)) =>
        val nonOriginSquares = routeSquares.drop(1).flatMap(squareByKey)
        if nonOriginSquares.isEmpty then RouteSafetyAssessment(tacticalSafety = 0.0, hasEnemyOccupied = false, transitSquaresEmpty = false)
        else
          val hasEnemyOccupied = nonOriginSquares.exists(sq => board.pieceAt(sq).exists(_.color != color))
          val transitSquaresEmpty = nonOriginSquares.forall(sq => board.pieceAt(sq).isEmpty)
          if hasEnemyOccupied then RouteSafetyAssessment(tacticalSafety = 0.0, hasEnemyOccupied = true, transitSquaresEmpty = transitSquaresEmpty)
          else
            val (unsafeCount, contestedCount) =
              nonOriginSquares.foldLeft((0, 0)) { case ((unsafe, contested), sq) =>
                if board.pieceAt(sq).exists(_.color == color) then (unsafe + 1, contested)
                else
                  val defenders = board.attackers(sq, color).count
                  val attackers = board.attackers(sq, !color).count
                  if attackers == 0 || defenders > attackers then (unsafe, contested)
                  else if defenders == attackers && attackers > 0 then (unsafe, contested + 1)
                  else (unsafe + 1, contested)
              }
            RouteSafetyAssessment(
              tacticalSafety = (1.0 - (unsafeCount * 0.30) - (contestedCount * 0.12)).max(0.0).min(1.0),
              hasEnemyOccupied = false,
              transitSquaresEmpty = transitSquaresEmpty
            )
      case _ =>
        RouteSafetyAssessment(tacticalSafety = 0.0, hasEnemyOccupied = false, transitSquaresEmpty = false)

  private def surfaceModeFor(
      piece: String,
      purpose: String,
      routeSquares: List[String],
      surfaceConfidence: Double,
      safetyAssessment: RouteSafetyAssessment
  ): String =
    if surfaceConfidence < ProseConfidenceCutoff then RouteSurfaceMode.Hidden
    else if allowsExactSurface(piece, purpose, routeSquares, surfaceConfidence, safetyAssessment) then RouteSurfaceMode.Exact
    else RouteSurfaceMode.Toward

  private def allowsExactSurface(
      piece: String,
      purpose: String,
      routeSquares: List[String],
      surfaceConfidence: Double,
      safetyAssessment: RouteSafetyAssessment
  ): Boolean =
    surfaceConfidence >= ExactRouteCutoff &&
      safetyAssessment.tacticalSafety >= ExactRouteCutoff &&
      safetyAssessment.transitSquaresEmpty &&
      routeSquares.drop(1).nonEmpty &&
      (piece match
        case "N" => true
        case "B" => routeSquares.size <= 3
        case "R" => routeSquares.size <= 3 && allowsRookExactPurpose(purpose)
        case "Q" => false
        case _   => false
      )

  private def allowsRookExactPurpose(purpose: String): Boolean =
    normalized(purpose).exists { value =>
      val low = value.toLowerCase
      low.contains("open-file") || low.contains("file") || low.contains("lift")
    }

  private def alignmentSupportsProse(band: Option[String]): Boolean =
    band.flatMap(normalized).exists { value =>
      val low = value.toLowerCase
      low == "onbook" || low == "playable"
    }

  private def routeLead(cue: PieceDeploymentCue): String =
    if useExactRoute(cue) then cue.routeSquares.mkString("via ", "-", "")
    else s"toward ${cue.lastSquare.getOrElse(cue.destination)}"

  private def routeTargetPhrase(cue: PieceDeploymentCue): String =
    if useExactRoute(cue) then cue.routeSquares.mkString("-")
    else destinationPhrase(cue)

  private def destinationPhrase(cue: PieceDeploymentCue): String =
    if cue.destination.endsWith("-file") then s"the ${cue.destination}"
    else cue.destination

  private def normalizePurpose(purpose: String, fallbackPlan: String): String =
    normalized(purpose).getOrElse(fallbackPlan.toLowerCase)

  private def pieceToken(raw: String): Option[String] =
    normalized(raw).map(_.toLowerCase).collect {
      case "knight" => "N"
      case "bishop" => "B"
      case "rook"   => "R"
      case "queen"  => "Q"
      case "king"   => "K"
    }

  private def pieceWord(piece: String): String =
    piece match
      case "N" => "knight"
      case "B" => "bishop"
      case "R" => "rook"
      case "Q" => "queen"
      case "K" => "king"
      case other => other.toLowerCase

  private def pieceToken(role: Role): String =
    role match
      case _root_.chess.Pawn   => "P"
      case _root_.chess.Knight => "N"
      case _root_.chess.Bishop => "B"
      case _root_.chess.Rook   => "R"
      case _root_.chess.Queen  => "Q"
      case _root_.chess.King   => "K"

  private def sideName(color: Color): String =
    if color.white then "white" else "black"

  private def colorFromSide(side: String): Option[Color] =
    normalized(side).map(_.toLowerCase).collect {
      case "white" => Color.White
      case "black" => Color.Black
    }

  private def sideNameFromFen(fen: String): String =
    colorFromFen(fen).map(sideName).getOrElse("white")

  private def colorFromFen(fen: String): Option[Color] =
    Option(fen)
      .map(_.trim.split("\\s+").toList.lift(1).getOrElse(""))
      .collect {
        case "w" => Color.White
        case "b" => Color.Black
      }

  private def squareByKey(key: String): Option[Square] =
    Square.all.find(_.key == key)

  private def parseUci(uci: Option[String]): Option[(String, String)] =
    uci.flatMap(normalized).filter(_.length >= 4).map(move => move.take(2) -> move.drop(2).take(2))

  private def sameFile(square: String, destination: String): Boolean =
    square.headOption.isDefined && destination.headOption.isDefined && square.head == destination.head

  private def isAdjacent(a: String, b: String): Boolean =
    (for
      af <- a.headOption
      ar <- a.drop(1).headOption
      bf <- b.headOption
      br <- b.drop(1).headOption
      if af.isLetter && ar.isDigit && bf.isLetter && br.isDigit
    yield
      Math.abs(af - bf) <= 1 && Math.abs(ar - br) <= 1
    ).getOrElse(false)

  private def isPawnMove(playedSan: Option[String]): Boolean =
    playedSan.flatMap(normalized).exists { san =>
      san.headOption.exists(ch => ch.isLower || (ch >= 'a' && ch <= 'h'))
    }

  private def humanizeAlignmentReason(raw: String): Option[String] =
    normalized(raw).map(_.toUpperCase).map {
      case "PA_MATCH"     => "expected plans are present"
      case "PRECOND_MISS" => "some structural preconditions are missing"
      case "ANTI_PLAN"    => "the move order fights the structure"
      case "LOW_CONF"     => "the structure read is uncertain"
      case code if code.startsWith("TOP_") => "the current top plan disagrees with the structure template"
      case code => code.toLowerCase.replace('_', ' ')
    }

  private def normalized(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.nonEmpty)

  private def capitalizeFirst(value: String): String =
    normalized(value).map(v => s"${v.head.toUpper}${v.tail}").getOrElse(value)

  private def isOpenOrSemiOpenFileFor(
      board: Board,
      file: File,
      color: Color
  ): Boolean =
    val mask = _root_.chess.Bitboard.file(file)
    val ownPawns = board.pawns & board.byColor(color) & mask
    ownPawns.isEmpty

  private def isCentralSquare(square: Square): Boolean =
    Set(File.C, File.D, File.E, File.F).contains(square.file) &&
      Set(_root_.chess.Rank.Third, _root_.chess.Rank.Fourth, _root_.chess.Rank.Fifth, _root_.chess.Rank.Sixth).contains(square.rank)
