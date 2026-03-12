package lila.llm.analysis

import lila.llm.*

private[llm] object StrategicSignalMatcher:

  private val StrategyStopwords = Set(
    "the",
    "and",
    "for",
    "with",
    "from",
    "into",
    "onto",
    "over",
    "under",
    "this",
    "that",
    "these",
    "those",
    "white",
    "black",
    "plan",
    "plans",
    "long",
    "term",
    "focus",
    "route",
    "routes",
    "piece",
    "pieces",
    "move",
    "moves",
    "side",
    "their",
    "your"
  )

  private val PieceWordByCode = Map(
    "P" -> List("pawn"),
    "N" -> List("knight"),
    "B" -> List("bishop"),
    "R" -> List("rook"),
    "Q" -> List("queen"),
    "K" -> List("king")
  )

  def signalTokens(text: String): Set[String] =
    Option(text)
      .getOrElse("")
      .toLowerCase
      .replaceAll("[^a-z0-9\\s]", " ")
      .split("\\s+")
      .toList
      .map(_.trim)
      .filter(token => token.length >= 4 && !token.forall(_.isDigit) && !StrategyStopwords(token))
      .toSet

  def phraseMentioned(textLower: String, phrase: String): Boolean =
    val normalized = Option(phrase).map(_.trim.toLowerCase).getOrElse("")
    normalized.nonEmpty && textLower.contains(normalized)

  def normalizeComparisonLabel(raw: String): List[String] =
    raw
      .split("""[^a-z0-9]+""")
      .toList
      .map(_.trim)
      .filter(token => token.length > 3)
      .distinct

  def containsComparablePhrase(normalizedText: String, phrase: String): Boolean =
    val normalizedPhrase = Option(phrase).map(_.trim.toLowerCase).getOrElse("")
    normalizedPhrase.nonEmpty &&
      (normalizedText.contains(normalizedPhrase) ||
        normalizeComparisonLabel(normalizedPhrase).exists(normalizedText.contains))

  def routeMentioned(textLower: String, route: StrategyPieceRoute): Boolean =
    routeSquaresMentioned(textLower = textLower, pieceCode = route.piece, routeSquares = route.route)

  def routeRefMentioned(textLower: String, routeRef: ActiveStrategicRouteRef): Boolean =
    phraseMentioned(textLower, routeRef.routeId) ||
      routeSquaresMentioned(textLower = textLower, pieceCode = routeRef.piece, routeSquares = routeRef.route)

  def routeCueMentioned(textLower: String, routeCue: ActiveBranchRouteCue): Boolean =
    phraseMentioned(textLower, routeCue.routeId) ||
      routeSquaresMentioned(textLower = textLower, pieceCode = routeCue.piece, routeSquares = routeCue.route)

  def moveRefMentioned(
      textLower: String,
      moveRef: ActiveStrategicMoveRef,
      comparison: Option[DecisionComparisonDigest]
  ): Boolean =
    moveMentioned(
      textLower = textLower,
      label = moveRef.label,
      uci = moveRef.uci,
      san = moveRef.san,
      comparison = comparison
    )

  def moveCueMentioned(
      textLower: String,
      moveCue: ActiveBranchMoveCue,
      comparison: Option[DecisionComparisonDigest]
  ): Boolean =
    moveMentioned(
      textLower = textLower,
      label = moveCue.label,
      uci = moveCue.uci,
      san = moveCue.san,
      comparison = comparison
    )

  private def moveMentioned(
      textLower: String,
      label: String,
      uci: String,
      san: Option[String],
      comparison: Option[DecisionComparisonDigest]
  ): Boolean =
    List(
      Some(label),
      Some(uci),
      san,
      comparison.flatMap(_.chosenMove),
      comparison.flatMap(_.engineBestMove),
      comparison.flatMap(_.deferredMove)
    ).flatten.exists(candidate => phraseMentioned(textLower, candidate))

  private def extractSquares(textLower: String): Set[String] =
    "(?i)\\b[a-h][1-8]\\b".r.findAllIn(textLower).map(_.toLowerCase).toSet

  private def containsOrderedRoute(textLower: String, route: List[String]): Boolean =
    if route.size < 2 then false
    else
      route.foldLeft((-1, true)) { case ((idx, ok), square) =>
        if !ok then (idx, false)
        else
          val next = textLower.indexOf(square, idx + 1)
          (next, next >= 0)
      }._2

  private def routeSquaresMentioned(
      textLower: String,
      pieceCode: String,
      routeSquares: List[String]
  ): Boolean =
    val normalizedRoute =
      routeSquares
        .map(_.trim.toLowerCase)
        .filter(_.matches("^[a-h][1-8]$"))
        .distinct
    if normalizedRoute.isEmpty then false
    else
      val seenSquares = extractSquares(textLower)
      val squareHits = normalizedRoute.count(seenSquares.contains)
      val code = Option(pieceCode).map(_.trim.toUpperCase).getOrElse("")
      val pieceWords = PieceWordByCode.getOrElse(code, Nil)
      val pieceWithSquareMention = normalizedRoute.exists(square => textLower.contains(s"${code.toLowerCase}$square"))
      val mentionsPiece = pieceWords.exists(textLower.contains) || pieceWithSquareMention
      containsOrderedRoute(textLower, normalizedRoute) ||
        squareHits >= 2 ||
        (mentionsPiece && squareHits >= 1)
