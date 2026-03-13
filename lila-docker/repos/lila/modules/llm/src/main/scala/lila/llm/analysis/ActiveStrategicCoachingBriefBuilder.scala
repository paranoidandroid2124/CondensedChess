package lila.llm.analysis

import _root_.chess.{ Bishop, Board, Color, Knight, Pawn, Queen, Rook, Square }
import _root_.chess.format.Fen
import _root_.chess.variant.Standard

import lila.llm.*

private[llm] object ActiveStrategicCoachingBriefBuilder:

  final case class Brief(
      campaignRole: Option[String],
      primaryIdea: Option[String],
      whyNow: Option[String],
      opponentReply: Option[String],
      executionHint: Option[String],
      longTermObjective: Option[String],
      keyTrigger: Option[String]
  ):
    def nonEmptySections: List[(String, String)] =
      List(
        "Campaign role" -> campaignRole,
        "Primary idea" -> primaryIdea,
        "Why now" -> whyNow,
        "Opponent reply to watch" -> opponentReply,
        "Execution hint" -> executionHint,
        "Long-term objective" -> longTermObjective,
        "Key trigger or failure mode" -> keyTrigger
      ).collect { case (label, Some(value)) if value.trim.nonEmpty => label -> value }

  final case class Coverage(
      hasDominantIdea: Boolean,
      hasForwardPlan: Boolean,
      hasGroundedSignal: Boolean,
      hasOpponentOrTrigger: Boolean
  )

  private val PieceNames = Map(
    "P" -> "pawn",
    "N" -> "knight",
    "B" -> "bishop",
    "R" -> "rook",
    "Q" -> "queen",
    "K" -> "king"
  )

  private val ForwardCuePatterns = List(
    """\b(should|must|needs? to|want(?:s)? to|aim(?:s)? to|plan(?:s)? to|prepare(?:s)? to|look(?:s)? to|tries? to)\b""",
    """\b(can then|so that|before [^.!?]{0,48}\bcan\b|if [^.!?]{0,64}\bthen\b|once\b|next\b|follow(?:s)? with\b)\b""",
    """\b(reroute|reroutes|rerouting|expand|expands|expanding|clamp|clamps|clamping|target|targets|targeting|press|presses|pressing|challenge|challenges|challenging|consolidate|consolidates|consolidating|switch|switches|switching|convert|converts|converting|prevent|prevents|preventing|build(?:s|ing)? toward|head(?:s|ing)? toward)\b"""
  ).map(_.r)

  def build(
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      currentFen: Option[String] = None
  ): Brief =
    val digest = strategyPack.flatMap(_.signalDigest)
    val dominantIdea = strategyPack.toList.flatMap(_.strategicIdeas).headOption
    val preferredSide = dominantIdea.map(_.ownerSide).orElse(strategyPack.map(_.sideToMove)).getOrElse("white")
    val currentBoard = currentFen.flatMap(parseBoard)
    val tacticalReality = immediateTacticalReality(currentBoard, preferredSide, moveRefs)
    val primaryIdea =
      dominantIdea.map(primaryIdeaLabel).flatMap(value => contextualizeSignal(Some(value), currentBoard, preferredSide))
    val whyNow =
      contextualizeSignal(
        dedupe(
          pickFirst(
            tacticalReality,
            dossier.flatMap(_.whyChosen),
            digest.flatMap(_.decision),
            digest.flatMap(_.structuralCue),
            digest.flatMap(_.dominantIdeaFocus).map(focus => s"The position is already pointing toward $focus."),
            digest.flatMap(_.practicalVerdict),
            dossier.flatMap(_.evidenceCue)
          ),
          primaryIdea
        ),
        currentBoard,
        preferredSide
      )
    val opponentReply =
      contextualizeSignal(
        pickFirst(
          dossier.flatMap(_.opponentResource),
          digest.flatMap(_.opponentPlan),
          digest.flatMap(_.prophylaxisThreat),
          dossier.flatMap(_.threadOpponentCounterplan)
        ),
        currentBoard,
        preferredSide
      )
    val executionHint = selectExecutionHint(strategyPack, dossier, routeRefs, dominantIdea)
    val longTermObjective = selectLongTermObjective(strategyPack, dominantIdea, executionHint)
    val keyTrigger =
      contextualizeSignal(
        dedupe(
          pickFirst(
            dossier.flatMap(_.practicalRisk),
            dossier.flatMap(_.whyDeferred),
            digest.flatMap(_.latentReason),
            digest.flatMap(_.counterplayScoreDrop).map(cp => s"If the plan drifts, the counterplay can rise by about ${cp}cp."),
            strategyPack.flatMap(_.pieceMoveRefs.headOption.map(moveRefSummary)),
            moveRefs.headOption.flatMap(_.san.map(san => s"The follow-up still depends on getting ${san.trim} into the right structure."))
          ),
          primaryIdea,
          whyNow,
          opponentReply,
          executionHint,
          longTermObjective
        ),
        currentBoard,
        preferredSide
      )
    Brief(
      campaignRole = dossier.flatMap(_.threadStage).flatMap(stageRoleDescription),
      primaryIdea = primaryIdea,
      whyNow = whyNow,
      opponentReply = opponentReply,
      executionHint = executionHint,
      longTermObjective = longTermObjective,
      keyTrigger = keyTrigger
    )

  def evaluateCoverage(text: String, brief: Brief): Coverage =
    val normalizedText = normalize(text)
    val textTokens = StrategicSignalMatcher.signalTokens(normalizedText)

    def mentioned(signal: Option[String]): Boolean =
      signal.exists(signalMentioned(normalizedText, textTokens, _))

    val dominantIdeaMentioned =
      brief.primaryIdea.exists(primary =>
        StrategicSignalMatcher.phraseMentioned(normalizedText, normalize(primary)) ||
          StrategicSignalMatcher.signalTokens(normalize(primary)).intersect(textTokens).size >= 2
      )

    val forwardCue = ForwardCuePatterns.exists(_.findFirstIn(normalizedText).nonEmpty)
    val structuralSequenceCue =
      List("before", "then", "next", "once", "after", "if").exists(word => normalizedText.contains(s" $word ")) ||
        normalizedText.startsWith("if ")
    val objectiveCue =
      brief.longTermObjective.exists(_ => normalizedText.contains("work toward") || normalizedText.contains("making"))
    val executionCueMentioned = mentioned(brief.executionHint)

    Coverage(
      hasDominantIdea = dominantIdeaMentioned,
      hasForwardPlan =
        forwardCue ||
          ((dominantIdeaMentioned || executionCueMentioned || objectiveCue) && (structuralSequenceCue || executionCueMentioned || objectiveCue)),
      hasGroundedSignal = dominantIdeaMentioned,
      hasOpponentOrTrigger = mentioned(brief.opponentReply) || mentioned(brief.keyTrigger)
    )

  private def pickFirst(values: Option[String]*): Option[String] =
    values.iterator.flatMap(cleanSignal).toSeq.headOption

  private def dedupe(value: Option[String], others: Option[String]*): Option[String] =
    value.filter { current =>
      val normalized = normalize(current)
      normalized.nonEmpty && !others.exists(other => normalize(other.getOrElse("")) == normalized)
    }

  private def cleanSignal(raw: Option[String]): Option[String] =
    raw.flatMap { value =>
      val sanitized = UserFacingSignalSanitizer.sanitize(naturalizeLabel(value))
      Option(sanitized).map(_.trim).filter(_.nonEmpty)
    }

  private def contextualizeSignal(raw: Option[String], board: Option[Board], side: String): Option[String] =
    raw.flatMap { value =>
      cleanSignal(Some(rewriteOccupiedSquareLanguage(value, board, side)))
    }

  private def signalMentioned(normalizedText: String, textTokens: Set[String], signal: String): Boolean =
    val normalizedSignal = normalize(signal)
    if normalizedSignal.isEmpty then false
    else
      StrategicSignalMatcher.phraseMentioned(normalizedText, normalizedSignal) ||
        StrategicSignalMatcher.signalTokens(normalizedSignal).intersect(textTokens).nonEmpty

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def naturalizeLabel(raw: String): String =
    Option(raw)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(
        _.replace("->", " leading to ")
          .replaceAll("(?i)\\b([a-h])-break\\s+Break\\b", "$1-break")
          .replaceAll("\\s+", " ")
      )
      .getOrElse("")

  private def pieceName(code: String): String =
    PieceNames.getOrElse(Option(code).map(_.trim.toUpperCase).getOrElse(""), "piece")

  private def primaryIdeaLabel(idea: StrategyIdeaSignal): String =
    val ideaLabel = StrategicIdeaSelector.humanizedKind(idea.kind)
    val focus = StrategicIdeaSelector.focusSummary(idea)
    cleanSignal(Some(s"$ideaLabel around $focus")).getOrElse(ideaLabel)

  private def selectExecutionHint(
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      dominantIdea: Option[StrategyIdeaSignal]
  ): Option[String] =
    val preferredSide = dominantIdea.map(_.ownerSide).orElse(strategyPack.map(_.sideToMove))
    pickFirst(
      dossier.flatMap(_.routeCue).filter(cue => preferredSide.forall(_ == cue.ownerSide)).map(routeCueSummary),
      routeRefs.find(ref => preferredSide.forall(_ == ref.ownerSide)).map(routeRefSummary),
      strategyPack.flatMap(
        _.pieceRoutes.find(route =>
          route.surfaceMode != RouteSurfaceMode.Hidden && preferredSide.forall(_ == route.ownerSide)
        ).map(routeSummary)
      )
    )

  private def selectLongTermObjective(
      strategyPack: Option[StrategyPack],
      dominantIdea: Option[StrategyIdeaSignal],
      executionHint: Option[String]
  ): Option[String] =
    val preferredSide = dominantIdea.map(_.ownerSide).orElse(strategyPack.map(_.sideToMove))
    val executionDestination = extractLastSquare(executionHint.getOrElse(""))
    strategyPack.toList
      .flatMap(_.directionalTargets)
      .find(target =>
        preferredSide.forall(_ == target.ownerSide) &&
          !executionDestination.contains(target.targetSquare)
      )
      .flatMap { target =>
        cleanSignal(Some(s"work toward making ${target.targetSquare} available for the ${pieceName(target.piece)}"))
      }

  private def routeCueSummary(cue: ActiveBranchRouteCue): String =
    routeLabel(
      ownerSide = None,
      piece = cue.piece,
      route = cue.route,
      purpose = Some(cue.purpose),
      surfaceMode = cue.surfaceMode
    )

  private def routeRefSummary(routeRef: ActiveStrategicRouteRef): String =
    routeLabel(
      ownerSide = None,
      piece = routeRef.piece,
      route = routeRef.route,
      purpose = Some(routeRef.purpose),
      surfaceMode = routeRef.surfaceMode
    )

  private def routeSummary(route: StrategyPieceRoute): String =
    routeLabel(
      ownerSide = None,
      piece = route.piece,
      route = route.route,
      purpose = Some(route.purpose),
      surfaceMode = route.surfaceMode
    )

  private def moveRefSummary(moveRef: StrategyPieceMoveRef): String =
    cleanSignal(Some(s"${pieceName(moveRef.piece)} contesting ${moveRef.target} for ${moveRef.idea}")).getOrElse(moveRef.idea)

  private def routeLabel(
      ownerSide: Option[String],
      piece: String,
      route: List[String],
      purpose: Option[String],
      surfaceMode: String
  ): String =
    val destination =
      route
        .map(_.trim.toLowerCase)
        .filter(_.matches("^[a-h][1-8]$"))
        .lastOption
    val deploymentText =
      if surfaceMode == RouteSurfaceMode.Exact && route.nonEmpty then s"${pieceName(piece)} via ${route.mkString("-")}"
      else
        destination match
          case Some(square) => s"${pieceName(piece)} toward $square"
          case None         => s"${pieceName(piece)} redeployment"
    val sidePrefix = cleanSignal(ownerSide).map(_ + " ").getOrElse("")
    val prefixedDeployment = s"$sidePrefix$deploymentText".trim
    cleanSignal(purpose).map(text => s"$prefixedDeployment for $text").getOrElse(prefixedDeployment)

  def stageRoleDescription(rawStage: String): Option[String] =
    Option(rawStage)
      .map(_.trim.toLowerCase)
      .filter(_.nonEmpty)
      .map {
        case "seed"    => "the plan is only starting to take shape"
        case "build"   => "the plan is being consolidated move by move"
        case "switch"  => "the game is pivoting toward a new sector or target"
        case "convert" => "the accumulated pressure should now turn into something concrete"
        case other     => naturalizeLabel(other)
      }
      .flatMap(text => cleanSignal(Some(text)))

  private def extractLastSquare(text: String): Option[String] =
    """\b([a-h][1-8])\b""".r.findAllMatchIn(Option(text).getOrElse("").toLowerCase).map(_.group(1)).toList.lastOption

  private def parseBoard(fen: String): Option[Board] =
    Fen.read(Standard, Fen.Full(fen)).map(_.board)

  private def materialScore(board: Board, color: Color): Int =
    board.byPiece(color, Pawn).count +
      board.byPiece(color, Knight).count * 3 +
      board.byPiece(color, Bishop).count * 3 +
      board.byPiece(color, Rook).count * 5 +
      board.byPiece(color, Queen).count * 9

  private def sideColor(side: String): Color =
    if side == "white" then Color.White else Color.Black

  private def immediateTacticalReality(
      currentBoard: Option[Board],
      side: String,
      moveRefs: List[ActiveStrategicMoveRef]
  ): Option[String] =
    currentBoard.flatMap { before =>
      val activeColor = sideColor(side)
      val currentEdge = materialScore(before, activeColor) - materialScore(before, !activeColor)

      moveRefs
        .flatMap { ref =>
          val san = ref.san.map(_.trim).filter(_.nonEmpty)
          val afterEdge =
            ref.fenAfter.flatMap(parseBoard).map(after =>
              materialScore(after, activeColor) - materialScore(after, !activeColor)
            )
          val gain = afterEdge.map(_ - currentEdge).getOrElse(0)
          san.flatMap { move =>
            if gain > 0 then
              Some(
                (
                  20 + gain,
                  s"$move immediately wins ${materialGainLabel(gain)}${if isForcingSan(move) then " while forcing the issue" else ""}."
                )
              )
            else if isForcingSan(move) then Some(10 -> s"$move forces the issue immediately.")
            else None
          }
        }
        .sortBy { case (priority, _) => -priority }
        .headOption
        .map(_._2)
    }

  private def materialGainLabel(gain: Int): String =
    if gain >= 9 then "a queen"
    else if gain >= 5 then "a rook"
    else if gain >= 3 then "a piece"
    else if gain >= 1 then "a pawn"
    else "material"

  private def isForcingSan(san: String): Boolean =
    val trimmed = Option(san).map(_.trim).getOrElse("")
    trimmed.contains("+") || trimmed.contains("#")

  private def rewriteOccupiedSquareLanguage(
      text: String,
      board: Option[Board],
      side: String
  ): String =
    board.fold(text) { currentBoard =>
      val color = sideColor(side)

      def occupantLabel(squareKey: String): Option[String] =
        Square.all
          .find(_.key == squareKey)
          .flatMap(currentBoard.pieceAt)
          .filter(_.color == color)
          .map(piece => pieceName(roleToken(piece.role)))

      def preserveCapitalization(original: String, replacement: String): String =
        if original.headOption.exists(_.isUpper) then replacement.take(1).toUpperCase + replacement.drop(1)
        else replacement

      val focus = """(?i)\bfocus on ([a-h][1-8])\b""".r
      val pointing = """(?i)\bpointing toward ([a-h][1-8])\b""".r

      val step1 =
        focus.replaceAllIn(text, m =>
          occupantLabel(m.group(1).toLowerCase)
            .map(piece => preserveCapitalization(m.matched, s"keep the $piece anchored on ${m.group(1).toLowerCase}"))
            .getOrElse(m.matched)
        )

      pointing.replaceAllIn(step1, m =>
        occupantLabel(m.group(1).toLowerCase)
          .map(piece => preserveCapitalization(m.matched, s"the $piece already anchored on ${m.group(1).toLowerCase}"))
          .getOrElse(m.matched)
      )
    }

  private def roleToken(role: _root_.chess.Role): String =
    role match
      case Knight => "N"
      case Bishop => "B"
      case Rook   => "R"
      case Queen  => "Q"
      case Pawn   => "P"
      case _      => "K"
