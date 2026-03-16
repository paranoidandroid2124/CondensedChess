package lila.study

import chess.*
import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr }
import lila.tree.ParseImport
import play.api.Configuration
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import java.time.{ Instant, ZoneOffset }
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.*
import scala.math.BigDecimal.RoundingMode
import scala.util.control.NonFatal

object AccountNotebookBuilder:
  val recentGameTarget        = 40
  val chessComArchiveScanLimit = 8

  enum ProductKind(val key: String):
    case MyAccountIntelligenceLite extends ProductKind("my_account_intelligence_lite")
    case OpponentPrep extends ProductKind("opponent_prep")
    def role = if this == MyAccountIntelligenceLite then "self" else "opponent"
    def lens = if this == MyAccountIntelligenceLite then "self_repair" else "opponent_pressure"

  object ProductKind:
    def fromKey(key: String): Option[ProductKind] =
      key.trim.toLowerCase match
        case ProductKind.MyAccountIntelligenceLite.key => Some(ProductKind.MyAccountIntelligenceLite)
        case ProductKind.OpponentPrep.key              => Some(ProductKind.OpponentPrep)
        case _                                         => None

  case class ExternalGame(
      provider: String,
      gameId: String,
      playedAt: String,
      white: String,
      black: String,
      result: String,
      sourceUrl: Option[String],
      pgn: String
  )

  case class BuildResult(
      dossier: JsObject,
      representativePgn: PgnStr,
      sampledGameCount: Int,
      eligibleGameCount: Int,
      warnings: List[String]
  )

  private enum SubjectResult:
    case Win, Draw, Loss

  private case class PlySnap(ply: Int, fen: String, san: String, uci: String, color: Color)
  private case class RepPos(snap: PlySnap, lastSan: Option[String])

  private case class ParsedGame(
      external: ExternalGame,
      subjectName: String,
      subjectColor: Color,
      subjectResult: SubjectResult,
      openingName: String,
      openingFamily: String,
      labels: List[String],
      plyCount: Int,
      rep: Option[RepPos]
  )

  private case class Pattern(
      side: Color,
      openingFamily: String,
      labels: List[String],
      games: List[ParsedGame]
  ):
    val support = games.size
    val losses  = games.count(_.subjectResult == SubjectResult.Loss)
    val draws   = games.count(_.subjectResult == SubjectResult.Draw)
    val wins    = games.count(_.subjectResult == SubjectResult.Win)
    val idBase  = slug(s"${colorLabel(side)}-$openingFamily")
    def ready = support >= 2 && games.exists(_.rep.isDefined)
    def anchor = games.sortBy(g => (resultRank(g.subjectResult), -g.plyCount)).head

  private case class PatternOut(
      sectionId: String,
      title: String,
      summary: String,
      status: String,
      evidence: JsObject,
      anchorCard: Option[JsObject],
      actions: List[JsObject],
      anchorId: Option[String],
      anchorGameId: Option[String],
      games: List[ParsedGame]
  )

  def normalizeProvider(provider: String): Option[String] =
    Option(provider).map(_.trim.toLowerCase).filter(Set("lichess", "chesscom"))

  def normalizeUsername(username: String): Option[String] =
    Option(username).map(_.trim).filter(_.matches("^[A-Za-z0-9][A-Za-z0-9_-]{1,29}$"))

  def buildFromGames(
      provider: String,
      username: String,
      kind: ProductKind,
      games: List[ExternalGame],
      requestedGameLimit: Int = recentGameTarget,
      generatedAt: Instant = Instant.now
  ): Either[String, BuildResult] =
    for
      safeProvider <- normalizeProvider(provider).toRight("Unsupported provider.")
      safeUsername <- normalizeUsername(username).toRight("Invalid username.")
      parsedGames = parseGames(games, safeUsername)
      parsed <- Option.when(parsedGames.nonEmpty)(parsedGames).toRight("No parseable public games found for this account.")
      built <- buildNotebook(safeProvider, safeUsername, kind, parsed, games.size, requestedGameLimit, generatedAt)
    yield built

  private def parseGames(games: List[ExternalGame], username: String): List[ParsedGame] =
    games.flatMap(parseGame(_, username))

  private def parseGame(game: ExternalGame, username: String): Option[ParsedGame] =
    ParseImport.full(PgnStr(game.pgn)).toOption.flatMap: imported =>
      val tags = imported.parsed.tags
      val white = tags.names.white.map(_.value.trim).filter(_.nonEmpty).getOrElse(game.white)
      val black = tags.names.black.map(_.value.trim).filter(_.nonEmpty).getOrElse(game.black)
      val subjectColor =
        if white.equalsIgnoreCase(username) then Some(Color.White)
        else if black.equalsIgnoreCase(username) then Some(Color.Black)
        else None
      subjectColor.map: color =>
        val openingName = tags("Opening").map(_.trim).filter(_.nonEmpty).getOrElse("Unclassified middlegame")
        ParsedGame(
          external = game.copy(white = white, black = black),
          subjectName = if color.white then white else black,
          subjectColor = color,
          subjectResult = subjectResult(game.result, color),
          openingName = openingName,
          openingFamily = openingFamily(openingName),
          labels = labelsFor(openingName, color),
          plyCount = imported.game.ply.value,
          rep = representativePosition(game.pgn, color)
        )

  private def buildNotebook(
      provider: String,
      username: String,
      kind: ProductKind,
      parsed: List[ParsedGame],
      eligible: Int,
      requestedGameLimit: Int,
      generatedAt: Instant
  ): Either[String, BuildResult] =
    val displayName = parsed.head.subjectName
    val seeds = selectPatterns(parsed, kind)
    val desiredPatterns = if seeds.size >= 3 then 3 else 2
    val concrete = seeds.take(desiredPatterns).zipWithIndex.map { case (pattern, idx) => patternOut(pattern, idx + 1, kind, parsed.size) }
    val patterns =
      if concrete.isEmpty then List(fallbackPattern(kind, 1, parsed.size), fallbackPattern(kind, 2, parsed.size))
      else if concrete.size == 1 then concrete :+ fallbackPattern(kind, 2, parsed.size)
      else concrete

    val exemplarGames = exemplarGamesFor(patterns, parsed, kind)
    val anchorGameIds = exemplarGames.map(_.external.gameId).toSet ++ patterns.flatMap(_.anchorGameId)
    val warnings = buildWarnings(parsed.size, eligible)
    val readyClusters = patterns.count(_.anchorId.isDefined)
    val status = if parsed.size < 8 || readyClusters < 2 then "partial" else "ready"
    val openingCards = openingCardsFor(parsed, kind)

    val openingSection = Json.obj(
      "id" -> "opening-map",
      "kind" -> "opening_map",
      "title" -> "Opening Map",
      "summary" -> s"The structures $displayName reaches most often, split by side.",
      "rank" -> 1,
      "status" -> "ready",
      "cards" -> JsArray(openingCards)
    )

    val patternSections = patterns.zipWithIndex.map { case (pattern, idx) =>
      Json.obj(
        "id" -> pattern.sectionId,
        "kind" -> "pattern_cluster",
        "title" -> pattern.title,
        "summary" -> pattern.summary,
        "rank" -> (2 + idx),
        "status" -> pattern.status,
        "evidence" -> pattern.evidence,
        "cards" -> JsArray(pattern.anchorCard.toList ++ pattern.actions)
      )
    }

    val postPatternRank = patternSections.size + 2
    val exemplarSection = Json.obj(
      "id" -> "exemplar-games",
      "kind" -> "exemplar_games",
      "title" -> "Exemplar Games",
      "summary" -> (if kind == ProductKind.MyAccountIntelligenceLite then s"Representative games that explain the repair sections for $displayName."
                    else s"A representative game to rehearse before facing $displayName."),
      "rank" -> postPatternRank,
      "status" -> "ready",
      "cards" -> JsArray(exemplarGames.take(if kind == ProductKind.MyAccountIntelligenceLite then 2 else 1).zipWithIndex.map {
        case (game, idx) => exemplarCard(game, patterns, kind, idx + 1, provider)
      })
    )

    val tailSections =
      if kind == ProductKind.MyAccountIntelligenceLite then
        List(
          Json.obj(
            "id" -> "action-page",
            "kind" -> "action_page",
            "title" -> "Action Page",
            "summary" -> "Three practical habits to carry into the next batch of games.",
            "rank" -> (postPatternRank + 1),
            "status" -> "ready",
            "cards" -> JsArray(actionPageCards(patterns, displayName))
          )
        )
      else
        List(
          Json.obj(
            "id" -> "steering-plan",
            "kind" -> "steering_plan",
            "title" -> "Steering Plan",
            "summary" -> s"How to steer $displayName toward the positions that repeat most often.",
            "rank" -> (postPatternRank + 1),
            "status" -> "ready",
            "cards" -> JsArray(steeringPlanCards(patterns, displayName))
          ),
          Json.obj(
            "id" -> "pre-game-checklist",
            "kind" -> "pre_game_checklist",
            "title" -> "Pre-game Checklist",
            "summary" -> "Short reminders to review before the game starts.",
            "rank" -> (postPatternRank + 2),
            "status" -> "ready",
            "cards" -> Json.arr(checklistCard(patterns, displayName))
          )
        )

    val overview = Json.obj(
      "title" -> "Overview",
      "dek" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           s"${parsed.size} recent public games, focused on recurring plan failures rather than clock swings."
         else s"${parsed.size} recent public games, focused on structures you can steer $displayName toward."),
      "cards" -> JsArray(overviewCards(kind, displayName, openingCards, patterns))
    )

    val sampledGames = JsArray(parsed.map: game =>
      (Json.obj(
        "id" -> game.external.gameId,
        "provider" -> provider,
        "externalGameId" -> game.external.gameId,
        "white" -> game.external.white,
        "black" -> game.external.black,
        "result" -> game.external.result,
        "opening" -> game.openingName,
        "role" -> (if anchorGameIds.contains(game.external.gameId) then "anchor" else "support")
      ) ++ optField("sourceUrl", game.external.sourceUrl))
    )

    val targetLabel = patterns.head.title.replaceFirst("^[^:]+: ", "")
    val headline =
      if kind == ProductKind.MyAccountIntelligenceLite then s"$displayName's recent public games point back to $targetLabel."
      else s"$displayName can be steered toward $targetLabel."

    val dossier = Json.obj(
      "schema" -> "chesstory.notebook.dossier.v1",
      "dossierId" -> s"${kind.key}-${provider}-${username.toLowerCase}-${generatedAt.toEpochMilli}",
      "productKind" -> kind.key,
      "subject" -> Json.obj("role" -> kind.role, "provider" -> provider, "username" -> username, "displayName" -> displayName),
      "source" -> Json.obj(
        "requestedGameLimit" -> requestedGameLimit,
        "sampledGameCount" -> parsed.size,
        "eligibleGameCount" -> eligible,
        "publicOnly" -> true,
        "includeTimeControl" -> false,
        "generatedAt" -> generatedAt.toString,
        "cache" -> Json.obj("hit" -> false, "ttlSec" -> 0)
      ),
      "status" -> status,
      "headline" -> headline,
      "summary" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           s"This notebook sampled ${parsed.size} of $eligible recent public games and keeps the focus on openings, recurring transition points, and plans that keep repeating."
         else s"This notebook sampled ${parsed.size} of $eligible recent public games and turns them into a prep surface: where to steer the game and which quiet decisions are worth forcing."),
      "overview" -> overview,
      "sections" -> JsArray(List(openingSection) ++ patternSections ++ List(exemplarSection) ++ tailSections),
      "appendix" -> Json.obj("sampledGames" -> sampledGames, "warnings" -> JsArray(warnings.map(JsString.apply)))
    )

    exemplarGames.headOption.orElse(parsed.headOption).map(_.external.pgn).map(PgnStr.apply)
      .map(pgn => BuildResult(dossier, pgn, parsed.size, eligible, warnings))
      .toRight("No representative PGN available for notebook creation.")

  private def patternOut(pattern: Pattern, idx: Int, kind: ProductKind, total: Int): PatternOut =
    val sectionId = s"pattern-$idx-${pattern.idBase}"
    val evidenceSummary = evidence(pattern.support, total)
    val title =
      if kind == ProductKind.MyAccountIntelligenceLite then s"Repair $idx: ${pattern.openingFamily}"
      else s"Pressure $idx: ${pattern.openingFamily}"
    val summary =
      if kind == ProductKind.MyAccountIntelligenceLite then
        s"${pattern.support} recent game(s) reached this ${colorLabel(pattern.side)} structure, and the first calm plan choice kept repeating."
      else
        s"${pattern.support} recent game(s) point to the same ${colorLabel(pattern.side)} structure. This is a practical steering target, not a one-off trap."
    if pattern.ready then
      val anchor = pattern.anchor
      val rep = anchor.rep.get
      val anchorId = s"$sectionId-anchor"
      val moveContext = Json.obj(
        "ply" -> rep.snap.ply,
        "moveNumber" -> ((rep.snap.ply + 1) / 2),
        "sideToMove" -> colorKey(rep.snap.color)
      ) ++ optField("lastMoveSan", rep.lastSan)
      val anchorCard = Json.obj(
        "cardKind" -> "anchor_position",
        "id" -> anchorId,
        "clusterId" -> s"$sectionId-cluster",
        "title" -> s"Anchor position: ${pattern.openingFamily}",
        "lens" -> kind.lens,
        "claim" ->
          (if kind == ProductKind.MyAccountIntelligenceLite then
             s"In ${pattern.openingFamily}, ${anchor.subjectName} often reaches a point where the next plan is not clearly chosen."
           else
             s"In ${pattern.openingFamily}, ${anchor.subjectName} can often be pushed into an early quiet decision."),
        "explanation" ->
          (if kind == ProductKind.MyAccountIntelligenceLite then
             s"This anchor marks an earlier position where ${anchor.subjectName} could still choose a cleaner file, break, or piece route."
           else
             s"This anchor repeats often enough to be a practical steering target against ${anchor.subjectName}."),
        "fen" -> rep.snap.fen,
        "moveContext" -> moveContext,
        "strategicTags" -> JsArray(pattern.labels.map(JsString.apply)),
        "questionPrompt" ->
          (if kind == ProductKind.MyAccountIntelligenceLite then
             s"Before committing here, what plan keeps your pieces coordinated in this ${pattern.openingFamily} structure?"
           else s"If you reach this structure, which decision are you forcing first?"),
        "recommendedPlan" -> Json.obj(
          "label" -> (if kind == ProductKind.MyAccountIntelligenceLite then "Recommended repair" else "Recommended steering plan"),
          "summary" ->
            (if kind == ProductKind.MyAccountIntelligenceLite then
               s"Delay the clarifying exchange until you can name the file, pawn break, or minor-piece square that should improve next."
             else s"Keep the tension and force the opponent to define the pawn break or piece trade first.")
        ),
        "antiPattern" -> Json.obj(
          "label" -> (if kind == ProductKind.MyAccountIntelligenceLite then "Repeated drift" else "Repeated concession"),
          "summary" ->
            (if kind == ProductKind.MyAccountIntelligenceLite then
               s"The recurring issue is resolving ${pattern.openingFamily} before the next plan is explicit."
             else s"The recurring opportunity is to make the opponent commit first in ${pattern.openingFamily}."),
          "playedMoveSan" -> rep.snap.san
        ),
        "evidence" -> (evidenceSummary ++ Json.obj("supportingGameIds" -> pattern.games.map(_.external.gameId))),
        "exemplarGameId" -> anchor.external.gameId
      )
      PatternOut(
        sectionId = sectionId,
        title = title,
        summary = summary,
        status = "ready",
        evidence = evidenceSummary,
        anchorCard = Some(anchorCard),
        actions = List(
          Json.obj(
            "cardKind" -> "action_item",
            "id" -> s"$sectionId-action-1",
            "title" -> (if kind == ProductKind.MyAccountIntelligenceLite then "Repair cue" else "Pressure cue"),
            "instruction" ->
              (if kind == ProductKind.MyAccountIntelligenceLite then
                 s"In your next ${pattern.openingFamily} game, pause before the first clarifying exchange and name the plan in one sentence."
               else s"Against this opponent, keep ${pattern.openingFamily} on the board until the first quiet commitment is forced."),
            "successMarker" ->
              (if kind == ProductKind.MyAccountIntelligenceLite then
                 "You can explain the next two moves without referring to a later tactic."
               else "The opponent makes the first irreversible structural choice."),
            "linkedAnchorPositionIds" -> Json.arr(anchorId)
          )
        ),
        anchorId = Some(anchorId),
        anchorGameId = Some(anchor.external.gameId),
        games = pattern.games
      )
    else
      PatternOut(
        sectionId = sectionId,
        title = title,
        summary = summary,
        status = "partial",
        evidence = evidenceSummary,
        anchorCard = None,
        actions = List(
          Json.obj(
            "cardKind" -> "action_item",
            "id" -> s"$sectionId-action-1",
            "title" -> (if kind == ProductKind.MyAccountIntelligenceLite then "Watchlist cue" else "Watchlist pressure cue"),
            "instruction" ->
              (if kind == ProductKind.MyAccountIntelligenceLite then
                 s"Track more ${pattern.openingFamily} games before locking this into a hard repair claim."
               else s"Track more ${pattern.openingFamily} games before building sharp prep around this section."),
            "successMarker" -> "Use this section as a watchlist until the pattern repeats enough to anchor.",
            "linkedAnchorPositionIds" -> Json.arr()
          )
        ),
        anchorId = None,
        anchorGameId = None,
        games = pattern.games
      )

  private def fallbackPattern(kind: ProductKind, idx: Int, total: Int): PatternOut =
    PatternOut(
      sectionId = s"pattern-$idx-general-watch",
      title =
        if kind == ProductKind.MyAccountIntelligenceLite then s"Repair $idx: General middlegame watchlist"
        else s"Pressure $idx: General steering watchlist",
      summary = "The recent sample was not broad enough to anchor a clean position here, so this section stays intentionally cautious.",
      status = "partial",
      evidence = evidence(1, total),
      anchorCard = None,
      actions = List(
        Json.obj(
          "cardKind" -> "action_item",
          "id" -> s"pattern-$idx-general-watch-action-1",
          "title" -> (if kind == ProductKind.MyAccountIntelligenceLite then "Watchlist cue" else "Watchlist pressure cue"),
          "instruction" ->
            (if kind == ProductKind.MyAccountIntelligenceLite then
               "Track the first moment when your plan becomes vague and compare it against the other anchor sections."
             else "Track which quiet structure choices this opponent accepts most often before investing in deeper prep."),
          "successMarker" -> "Promote this to an anchor section after the next batch of public games.",
          "linkedAnchorPositionIds" -> Json.arr()
        )
      ),
      anchorId = None,
      anchorGameId = None,
      games = Nil
    )

  private def selectPatterns(parsed: List[ParsedGame], kind: ProductKind): List[Pattern] =
    parsed.groupBy(g => (g.subjectColor, g.openingFamily)).values.map: games =>
      Pattern(games.head.subjectColor, games.head.openingFamily, games.head.labels, games.sortBy(g => (resultRank(g.subjectResult), -g.plyCount)))
    .toList.sortBy: pattern =>
      val priority =
        if kind == ProductKind.MyAccountIntelligenceLite then -(pattern.losses * 6 + pattern.draws * 2 + pattern.support)
        else -(pattern.support * 4 + pattern.losses * 3 + pattern.draws * 2 - pattern.wins)
      (priority, pattern.openingFamily)

  private def exemplarGamesFor(patterns: List[PatternOut], parsed: List[ParsedGame], kind: ProductKind): List[ParsedGame] =
    val fromPatterns = patterns.flatMap(_.anchorGameId).distinct.flatMap(id => parsed.find(_.external.gameId == id))
    val fallback = parsed.sortBy(g => (resultRank(g.subjectResult), -g.plyCount))
    (fromPatterns ++ fallback).distinctBy(_.external.gameId).take(if kind == ProductKind.MyAccountIntelligenceLite then 2 else 1)

  private def openingCardsFor(parsed: List[ParsedGame], kind: ProductKind): List[JsObject] =
    Color.all.flatMap: color =>
      parsed.filter(_.subjectColor == color).groupBy(_.openingFamily).values.toList.sortBy(g => (-g.size, g.head.openingFamily)).headOption.map: games =>
        val head = games.head
        Json.obj(
          "cardKind" -> "opening_map",
          "id" -> s"opening-${colorKey(color)}-${slug(head.openingFamily)}",
          "title" -> s"${colorLabel(color)} repertoire",
          "side" -> colorKey(color),
          "openingFamily" -> head.openingFamily,
          "structureLabels" -> JsArray(head.labels.map(JsString.apply)),
          "story" ->
            (if kind == ProductKind.MyAccountIntelligenceLite then
               s"As ${colorLabel(color)}, ${head.subjectName} reached ${head.openingFamily} in ${games.size} recent game(s). Treat it as a route into a familiar middlegame plan."
             else s"As ${colorLabel(color)}, ${head.subjectName} reached ${head.openingFamily} in ${games.size} recent game(s). This is a realistic steering target for preparation."),
          "evidence" -> (evidence(games.size, parsed.size) ++ Json.obj("supportingGameIds" -> games.map(_.external.gameId)))
        )
    .take(2)

  private def overviewCards(kind: ProductKind, displayName: String, openingCards: List[JsObject], patterns: List[PatternOut]): List[JsObject] =
    val openingHeadline = openingCards.headOption.flatMap(card => (card \ "openingFamily").asOpt[String]).getOrElse("Mixed structures")
    val first = patterns.headOption
    val second = patterns.drop(1).headOption.orElse(first)
    if kind == ProductKind.MyAccountIntelligenceLite then
      List(
        Json.obj(
          "id" -> "overview-opening-identity",
          "kind" -> "opening_identity",
          "title" -> "Opening identity",
          "headline" -> s"$displayName most often reaches $openingHeadline structures in the recent sample.",
          "summary" -> "Treat the opening as a route into a recurring middlegame, not as a standalone move-order quiz.",
          "priority" -> "medium",
          "confidence" -> evidenceStrength(openingCards.headOption.flatMap(card => (card \ "evidence" \ "supportingGames").asOpt[Int]).getOrElse(1)),
          "evidence" -> openingCards.headOption.flatMap(card => (card \ "evidence").asOpt[JsObject]).getOrElse(evidence(1, 1)),
          "linkedSectionId" -> "opening-map"
        ),
        Json.obj(
          "id" -> "overview-recurring-leak",
          "kind" -> "recurring_leak",
          "title" -> "Recurring leak",
          "headline" -> first.map(_.title).getOrElse("General middlegame watchlist"),
          "summary" -> first.map(_.summary).getOrElse("The sample is still thin, so this is a watchlist rather than a firm claim."),
          "priority" -> "high",
          "confidence" -> first.map(_.evidence.value("strength")).getOrElse(JsString("weak")),
          "evidence" -> first.map(_.evidence).getOrElse(evidence(1, 1)),
          "linkedSectionId" -> first.map(_.sectionId).getOrElse("opening-map")
        ) ++ optField("linkedCardId", first.flatMap(_.anchorId)),
        Json.obj(
          "id" -> "overview-repair-priority",
          "kind" -> "repair_priority",
          "title" -> "Repair priority",
          "headline" -> second.map(_.title).getOrElse("General middlegame watchlist"),
          "summary" -> "The next review batch should focus on the first calm decision, not on the final tactical mistake.",
          "priority" -> "high",
          "confidence" -> second.map(_.evidence.value("strength")).getOrElse(JsString("weak")),
          "evidence" -> second.map(_.evidence).getOrElse(evidence(1, 1)),
          "linkedSectionId" -> second.map(_.sectionId).getOrElse("opening-map")
        ) ++ optField("linkedCardId", second.flatMap(_.anchorId))
      )
    else
      List(
        Json.obj(
          "id" -> "overview-opening-identity",
          "kind" -> "opening_identity",
          "title" -> "Opening identity",
          "headline" -> s"$displayName most often reaches $openingHeadline structures in the recent sample.",
          "summary" -> "Prep should start from the structure they revisit, not from a single forcing line.",
          "priority" -> "medium",
          "confidence" -> evidenceStrength(openingCards.headOption.flatMap(card => (card \ "evidence" \ "supportingGames").asOpt[Int]).getOrElse(1)),
          "evidence" -> openingCards.headOption.flatMap(card => (card \ "evidence").asOpt[JsObject]).getOrElse(evidence(1, 1)),
          "linkedSectionId" -> "opening-map"
        ),
        Json.obj(
          "id" -> "overview-pressure-point",
          "kind" -> "pressure_point",
          "title" -> "Pressure point",
          "headline" -> first.map(_.title).getOrElse("General steering watchlist"),
          "summary" -> first.map(_.summary).getOrElse("The sample is still thin, so treat this as a provisional steering cue."),
          "priority" -> "high",
          "confidence" -> first.map(_.evidence.value("strength")).getOrElse(JsString("weak")),
          "evidence" -> first.map(_.evidence).getOrElse(evidence(1, 1)),
          "linkedSectionId" -> first.map(_.sectionId).getOrElse("opening-map")
        ) ++ optField("linkedCardId", first.flatMap(_.anchorId)),
        Json.obj(
          "id" -> "overview-steering-target",
          "kind" -> "steering_target",
          "title" -> "Steering target",
          "headline" -> second.map(_.title).getOrElse("Steering plan"),
          "summary" -> s"The goal is to force $displayName into a familiar quiet decision before the position simplifies.",
          "priority" -> "high",
          "confidence" -> second.map(_.evidence.value("strength")).getOrElse(JsString("weak")),
          "evidence" -> second.map(_.evidence).getOrElse(evidence(1, 1)),
          "linkedSectionId" -> second.map(_.sectionId).getOrElse("steering-plan")
        ) ++ optField("linkedCardId", second.flatMap(_.anchorId))
      )

  private def exemplarCard(game: ParsedGame, patterns: List[PatternOut], kind: ProductKind, idx: Int, provider: String): JsObject =
    val linked = patterns.flatMap(p => p.anchorGameId.contains(game.external.gameId).option(p.anchorId).flatten).distinct
    val gameJson = Json.obj(
      "cardKind" -> "exemplar_game",
      "id" -> s"exemplar-$idx-${slug(game.external.gameId)}",
      "title" -> s"${game.openingFamily}: ${game.external.white} vs ${game.external.black}",
      "game" -> (Json.obj(
        "id" -> game.external.gameId,
        "provider" -> provider,
        "externalGameId" -> game.external.gameId,
        "white" -> game.external.white,
        "black" -> game.external.black,
        "result" -> game.external.result,
        "opening" -> game.openingName,
        "role" -> "anchor"
      ) ++ optField("sourceUrl", game.external.sourceUrl)),
      "whyItMatters" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           s"This game is a clean example of how ${game.openingFamily} decisions spill into the middlegame."
         else s"This game is a practical rehearsal for steering ${game.subjectName} into a familiar ${game.openingFamily} structure."),
      "narrative" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           "Use this exemplar to remember the earlier plan choice, not just the eventual result."
         else "Use this exemplar to rehearse how the structure develops before the opponent has to commit."),
      "linkedAnchorPositionIds" -> JsArray(linked.map(JsString.apply)),
      "takeaway" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           s"Review the plan choice that appeared in ${game.openingFamily}, then compare it with the anchor positions."
         else s"If you can reach ${game.openingFamily}, steer toward the same quiet commitment this exemplar exposed.")
    )
    gameJson ++ optField("turningPointPly", game.rep.map(_.snap.ply))

  private def actionPageCards(patterns: List[PatternOut], displayName: String): List[JsObject] =
    List(
      Json.obj("cardKind" -> "action_item", "id" -> "action-page-1", "title" -> "Name the plan before the exchange", "instruction" -> "Before releasing central tension, say which file, pawn break, or minor-piece square should improve next.", "successMarker" -> "You can explain the next two moves in one sentence before you play them.", "linkedAnchorPositionIds" -> JsArray(patterns.flatMap(_.anchorId).take(2).map(JsString.apply))),
      Json.obj("cardKind" -> "action_item", "id" -> "action-page-2", "title" -> "Replay the anchor positions", "instruction" -> s"Revisit the anchor positions from this notebook before the next session instead of reviewing all ${displayName}'s games at once.", "successMarker" -> "The repeated structure feels familiar before move 15.", "linkedAnchorPositionIds" -> JsArray(patterns.flatMap(_.anchorId).take(3).map(JsString.apply))),
      Json.obj("cardKind" -> "action_item", "id" -> "action-page-3", "title" -> "Refresh the notebook after another batch", "instruction" -> "Generate a new notebook after 20 more public games and compare which repair sections stayed stable.", "successMarker" -> "You can tell whether the same pattern is fading or still recurring.", "linkedAnchorPositionIds" -> Json.arr())
    )

  private def steeringPlanCards(patterns: List[PatternOut], displayName: String): List[JsObject] =
    List(
      Json.obj("cardKind" -> "action_item", "id" -> "steering-plan-1", "title" -> "Steer before forcing tactics", "instruction" -> s"Against $displayName, value the structure that appears most often in this notebook over a memorized sideline novelty.", "successMarker" -> "The game reaches one of the notebook anchor structures before you calculate forcing tactics.", "linkedAnchorPositionIds" -> JsArray(patterns.flatMap(_.anchorId).take(2).map(JsString.apply))),
      Json.obj("cardKind" -> "action_item", "id" -> "steering-plan-2", "title" -> "Keep the first quiet decision on the board", "instruction" -> "Delay clarifying captures until the opponent has to declare a pawn break or piece trade first.", "successMarker" -> "The opponent makes the first irreversible structural choice.", "linkedAnchorPositionIds" -> JsArray(patterns.flatMap(_.anchorId).take(2).map(JsString.apply))),
      Json.obj("cardKind" -> "action_item", "id" -> "steering-plan-3", "title" -> "Use the exemplar game as a rehearsal", "instruction" -> "Review the exemplar once before the round and focus on the plan that repeated, not the exact move order.", "successMarker" -> "You can describe what middlegame you want before the first irreversible exchange.", "linkedAnchorPositionIds" -> Json.arr())
    )

  private def checklistCard(patterns: List[PatternOut], displayName: String): JsObject =
    Json.obj(
      "cardKind" -> "checklist",
      "id" -> "prep-checklist",
      "title" -> "Before the game",
      "items" -> Json.arr(
        Json.obj("id" -> "checklist-1", "label" -> s"Know the steering target: ${patterns.headOption.map(_.title).getOrElse("general middlegame control")}", "reason" -> "The first recurring structure matters more than memorizing a single engine line.", "priority" -> "high"),
        Json.obj("id" -> "checklist-2", "label" -> "Delay clarifying captures until the structure is favorable", "reason" -> s"$displayName's recent games repeat most often when the first quiet decision is forced early.", "priority" -> "high"),
        Json.obj("id" -> "checklist-3", "label" -> "Revisit the anchor positions before playing", "reason" -> "The notebook is strongest when you remember the plans, not just the move orders.", "priority" -> "medium")
      )
    )

  private def representativePosition(pgn: String, color: Color): Option[RepPos] =
    snapshots(pgn).toOption.flatMap: snaps =>
      val candidates = snaps.filter(_.color == color)
      if candidates.isEmpty then None
      else
        val band = candidates.filter(s => s.ply >= 11 && s.ply <= 28)
        val chosen = (if band.nonEmpty then band else candidates).sortBy(s => math.abs(s.ply - 18)).headOption
        chosen.map(s => RepPos(s, snaps.find(_.ply == s.ply - 1).map(_.san)))

  private def snapshots(pgn: String): Either[String, List[PlySnap]] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(err) => Left(err.value)
      case Right(parsed) =>
        val game = parsed.toGame
        val replay = Replay.makeReplay(game, parsed.moves)
        Right(replay.replay.chronoMoves.zipWithIndex.map { case (move, idx) =>
          val ply = game.ply.value + idx + 1
          val before = if idx == 0 then game.position else replay.replay.chronoMoves.take(idx).last.after
          PlySnap(ply, Fen.write(before, Ply(ply - 1).fullMoveNumber).value, move.toSanStr.toString, move.toUci.uci, before.color)
        })

  private def subjectResult(result: String, color: Color): SubjectResult =
    result match
      case "1-0"     => if color.white then SubjectResult.Win else SubjectResult.Loss
      case "0-1"     => if color.black then SubjectResult.Win else SubjectResult.Loss
      case "1/2-1/2" => SubjectResult.Draw
      case _         => SubjectResult.Draw

  private def resultRank(result: SubjectResult): Int =
    result match
      case SubjectResult.Loss => 0
      case SubjectResult.Draw => 1
      case SubjectResult.Win  => 2

  private def openingFamily(name: String): String =
    val trimmed = name.trim
    val primary = trimmed.takeWhile(ch => ch != ':' && ch != ',' && ch != '(').trim
    if primary.nonEmpty then primary else "Unclassified middlegame"

  private def labelsFor(openingName: String, color: Color): List[String] =
    val lower = openingName.toLowerCase
    val base =
      if lower.contains("sicilian") then List("asymmetrical center", "open c-file")
      else if lower.contains("queen's gambit") then List("queen-pawn tension", "c-file pressure")
      else if lower.contains("nimzo") then List("central restraint", "bishop pair tension")
      else if lower.contains("king's indian") then List("dark-square pressure", "kingside race")
      else if lower.contains("caro-kann") then List("solid center", "piece rerouting")
      else if lower.contains("french") then List("locked center", "queenside counterplay")
      else if lower.contains("english") then List("flank pressure", "flexible center")
      else if lower.contains("london") then List("solid setup", "kingside expansion")
      else List("central tension", "piece coordination")
    (base :+ s"${colorLabel(color)} structures").distinct.take(3)

  private def buildWarnings(sampled: Int, eligible: Int): List[String] =
    List(
      Option.when(sampled < 8)("Sample size is still thin, so some sections stay provisional."),
      Option.when(eligible > sampled)("Some fetched games were skipped because they could not be parsed cleanly.")
    ).flatten

  private def evidence(support: Int, total: Int): JsObject =
    Json.obj(
      "strength" -> evidenceStrength(support),
      "supportingGames" -> support,
      "totalSampledGames" -> total,
      "occurrenceCount" -> support,
      "clusterSharePct" -> sharePct(support, total)
    )

  private def evidenceStrength(support: Int): String =
    if support >= 5 then "strong" else if support >= 3 then "medium" else "weak"

  private def sharePct(part: Int, total: Int): Double =
    if total <= 0 then 0d else BigDecimal(part.toDouble * 100d / total.toDouble).setScale(1, RoundingMode.HALF_UP).toDouble

  private def colorLabel(color: Color): String = if color.white then "White" else "Black"
  private def colorKey(color: Color): String = if color.white then "white" else "black"

  private def slug(value: String): String =
    value.toLowerCase.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") match
      case "" => "item"
      case v  => v

  private def optField[T: Writes](name: String, value: Option[T]): JsObject =
    value.fold(Json.obj())(v => Json.obj(name -> v))

final class AccountNotebookBuilder(
    appConfig: Configuration,
    ws: StandaloneWSClient
)(using Executor):
  import AccountNotebookBuilder.*

  private val logger = lila.log("accountNotebook")
  private val requestTimeout = 12.seconds
  private val utcDateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)

  private def configured(path: String): Option[String] =
    appConfig.getOptional[String](path).map(_.trim).filter(_.nonEmpty)

  private val lichessImportApiBase =
    configured("external.import.lichess.api_base")
      .orElse(sys.env.get("LICHESS_IMPORT_API_BASE").map(_.trim).filter(_.nonEmpty))
      .getOrElse("https://lichess.org")
      .stripSuffix("/")

  private val lichessWebBase =
    configured("external.import.lichess.web_base")
      .orElse(sys.env.get("LICHESS_WEB_BASE").map(_.trim).filter(_.nonEmpty))
      .getOrElse("https://lichess.org")
      .stripSuffix("/")

  private val chessComApiBase =
    configured("external.import.chesscom.api_base")
      .orElse(sys.env.get("CHESSCOM_API_BASE").map(_.trim).filter(_.nonEmpty))
      .getOrElse("https://api.chess.com")
      .stripSuffix("/")

  def build(provider: String, username: String, kind: ProductKind): Fu[Either[String, BuildResult]] =
    normalizeProvider(provider).fold(fuccess(Left("Unsupported provider."))) { safeProvider =>
      normalizeUsername(username).fold(fuccess(Left("Invalid username."))) { safeUsername =>
        fetchRecentGames(safeProvider, safeUsername).map:
          buildFromGames(safeProvider, safeUsername, kind, _, recentGameTarget, nowInstant)
      }
    }

  def fetchRecentGames(provider: String, username: String): Fu[List[ExternalGame]] =
    normalizeProvider(provider).fold(fuccess(Nil)) {
      case "lichess"  => fetchRecentLichessGames(username)
      case "chesscom" => fetchRecentChessComGames(username)
    }

  private def fetchRecentLichessGames(username: String): Fu[List[ExternalGame]] =
    val url = s"$lichessImportApiBase/api/games/user/$username?max=$recentGameTarget&pgnInJson=true"
    ws.url(url)
      .withHttpHeaders("Accept" -> "application/x-ndjson")
      .withRequestTimeout(requestTimeout)
      .get()
      .map: res =>
        if res.status == 200 then
          Option(res.body[String]).toList
            .flatMap(_.split('\n').toList)
            .map(_.trim)
            .filter(_.nonEmpty)
            .flatMap(parseLichessNdjsonLine)
            .take(recentGameTarget)
        else
          logger.warn(s"account notebook lichess import failed username=$username status=${res.status}")
          Nil
      .recover { case NonFatal(err) =>
        logger.warn(s"account notebook lichess import exception username=$username err=${err.getMessage}")
        Nil
      }

  private def parseLichessNdjsonLine(line: String): Option[ExternalGame] =
    try
      val js = Json.parse(line)
      (js \ "pgn").asOpt[String].map(_.trim).filter(_.nonEmpty).map: pgn =>
        val id = (js \ "id").asOpt[String].getOrElse(s"lichess-${Math.abs(pgn.hashCode)}")
        val winner = (js \ "winner").asOpt[String]
        ExternalGame(
          provider = "lichess",
          gameId = id,
          playedAt = formatEpochMs((js \ "lastMoveAt").asOpt[Long].orElse((js \ "createdAt").asOpt[Long])),
          white = playerName(js, "white"),
          black = playerName(js, "black"),
          result =
            if winner.contains("white") then "1-0"
            else if winner.contains("black") then "0-1"
            else "1/2-1/2",
          sourceUrl = Some(s"$lichessWebBase/$id"),
          pgn = pgn
        )
    catch
      case NonFatal(_) => None

  private def playerName(js: JsValue, side: String): String =
    (js \ "players" \ side \ "user" \ "name").asOpt[String]
      .orElse((js \ "players" \ side \ "name").asOpt[String])
      .getOrElse(side.capitalize)

  private def fetchRecentChessComGames(username: String): Fu[List[ExternalGame]] =
    val archivesUrl = s"$chessComApiBase/pub/player/${username.toLowerCase}/games/archives"
    ws.url(archivesUrl)
      .withRequestTimeout(requestTimeout)
      .get()
      .flatMap: res =>
        if res.status == 200 then
          val archives = (Json.parse(res.body[String]) \ "archives").asOpt[List[String]].getOrElse(Nil).reverse.take(chessComArchiveScanLimit)
          collectChessComGames(archives, Nil)
        else
          logger.warn(s"account notebook chesscom archives failed username=$username status=${res.status}")
          fuccess(Nil)
      .recover { case NonFatal(err) =>
        logger.warn(s"account notebook chesscom archives exception username=$username err=${err.getMessage}")
        Nil
      }

  private def collectChessComGames(archives: List[String], acc: List[ExternalGame]): Fu[List[ExternalGame]] =
    if acc.size >= recentGameTarget || archives.isEmpty then fuccess(acc.take(recentGameTarget))
    else
      ws.url(archives.head).withRequestTimeout(requestTimeout).get().flatMap: res =>
        val merged =
          if res.status == 200 then acc ++ parseChessComArchive(res.body[String])
          else acc
        collectChessComGames(archives.tail, merged.take(recentGameTarget))
      .recoverWith { case NonFatal(_) => collectChessComGames(archives.tail, acc) }

  private def parseChessComArchive(jsonText: String): List[ExternalGame] =
    (Json.parse(jsonText) \ "games").asOpt[List[JsObject]].getOrElse(Nil)
      .sortBy(g => (g \ "end_time").asOpt[Long].getOrElse(0L))
      .reverse
      .flatMap(parseChessComGame)

  private def parseChessComGame(game: JsObject): Option[ExternalGame] =
    (game \ "pgn").asOpt[String].map(_.trim).filter(_.nonEmpty).map: pgn =>
      val url = (game \ "url").asOpt[String]
      ExternalGame(
        provider = "chesscom",
        gameId = url.flatMap(_.split('/').lastOption).filter(_.nonEmpty).getOrElse(s"chesscom-${Math.abs(pgn.hashCode)}"),
        playedAt = formatEpochMs((game \ "end_time").asOpt[Long].map(_ * 1000L)),
        white = (game \ "white" \ "username").asOpt[String].getOrElse("White"),
        black = (game \ "black" \ "username").asOpt[String].getOrElse("Black"),
        result = chessComResult(game),
        sourceUrl = url,
        pgn = pgn
      )

  private def chessComResult(game: JsObject): String =
    val w = (game \ "white" \ "result").asOpt[String].getOrElse("").toLowerCase
    val b = (game \ "black" \ "result").asOpt[String].getOrElse("").toLowerCase
    if w == "win" then "1-0" else if b == "win" then "0-1" else "1/2-1/2"

  private def formatEpochMs(millisOpt: Option[Long]): String =
    millisOpt.map(ms => utcDateFmt.format(Instant.ofEpochMilli(ms))).getOrElse("-")
