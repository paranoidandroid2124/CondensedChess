package lila.accountintel.dossier

import chess.format.pgn.PgnStr
import play.api.libs.json.*

import java.time.Instant
import scala.math.BigDecimal.RoundingMode

import lila.accountintel.*
import lila.accountintel.AccountIntel.*
import lila.accountintel.cluster.ClusterSelector

object NotebookDossierAssembler:

  def assemble(
      provider: String,
      username: String,
      kind: ProductKind,
      bundle: PrimitiveBundle,
      selectedClusters: List[SnapshotCluster],
      allClusters: List[SnapshotCluster],
      requestedGameLimit: Int,
      generatedAt: Instant
  ): Either[String, NotebookBuildArtifact] =
    val parsed = bundle.parsedGames
    val displayName = parsed.head.subjectName
    val duplicateStructureFamilies =
      selectedClusters
        .groupBy(_.structureFamily)
        .collect { case (family, items) if items.size > 1 => family }
        .toSet
    val concrete =
      selectedClusters.zipWithIndex.map { case (cluster, idx) =>
        clusterOut(
          cluster = cluster,
          idx = idx + 1,
          kind = kind,
          total = parsed.size,
          showSideInTitle = duplicateStructureFamilies(cluster.structureFamily)
        )
      }
    val sections =
      if concrete.isEmpty then
        List(fallbackCluster(kind, 1, parsed.size), fallbackCluster(kind, 2, parsed.size))
      else if concrete.size == 1 then concrete :+ fallbackCluster(kind, 2, parsed.size)
      else concrete

    val exemplarGames = exemplarGamesFor(sections, parsed)
    val anchorGameIds = exemplarGames.map(_.external.gameId).toSet ++ sections.flatMap(_.anchorGameId)
    val readyClusters = sections.count(_.anchorId.isDefined)
    val status = if parsed.size < 8 || readyClusters < 2 then "partial" else "ready"
    val openingCards = openingCardsFor(parsed)

    val openingSection = Json.obj(
      "id" -> "opening-map",
      "kind" -> "opening_map",
      "title" -> "Opening Map",
      "summary" -> s"The opening buckets $displayName enters most often, split by side.",
      "rank" -> 1,
      "status" -> "ready",
      "cards" -> JsArray(openingCards)
    )

    val patternSections = sections.zipWithIndex.map { case (section, idx) =>
      Json.obj(
        "id" -> section.sectionId,
        "kind" -> "pattern_cluster",
        "title" -> section.title,
        "summary" -> section.summary,
        "rank" -> (2 + idx),
        "status" -> section.status,
        "evidence" -> section.evidence,
        "cards" -> JsArray(section.anchorCard.toList ++ section.actions)
      )
    }

    val postPatternRank = patternSections.size + 2
    val exemplarSection = Json.obj(
      "id" -> "exemplar-games",
      "kind" -> "exemplar_games",
      "title" -> "Exemplar Games",
      "summary" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           s"Representative games that explain the repair sections for $displayName."
         else s"A representative game to rehearse before facing $displayName."),
      "rank" -> postPatternRank,
      "status" -> "ready",
      "cards" -> JsArray(
        exemplarGames.take(if kind == ProductKind.MyAccountIntelligenceLite then 2 else 1).zipWithIndex.map {
          case (game, idx) => exemplarCard(game, sections, kind, idx + 1, provider)
        }
      )
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
            "cards" -> JsArray(actionPageCards(sections, displayName))
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
            "cards" -> JsArray(steeringPlanCards(sections, displayName))
          ),
          Json.obj(
            "id" -> "pre-game-checklist",
            "kind" -> "pre_game_checklist",
            "title" -> "Pre-game Checklist",
            "summary" -> "Short reminders to review before the game starts.",
            "rank" -> (postPatternRank + 2),
            "status" -> "ready",
            "cards" -> Json.arr(checklistCard(sections, displayName))
          )
        )

    val overview = Json.obj(
      "title" -> "Overview",
      "dek" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           s"${parsed.size} recent public games, focused on recurring plan failures rather than clock swings."
         else
           s"${parsed.size} recent public games, focused on structures you can steer $displayName toward."),
      "cards" -> JsArray(overviewCards(kind, displayName, openingCards, sections))
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
      ) ++ optField("sourceUrl", game.external.sourceUrl)))

    val targetLabel = sections.head.title.replaceFirst("^[^:]+: ", "")
    val headline =
      if kind == ProductKind.MyAccountIntelligenceLite then
        s"$displayName's recent public games point back to $targetLabel."
      else s"$displayName can be steered toward $targetLabel."

    val dossier = Json.obj(
      "schema" -> "chesstory.notebook.dossier.v1",
      "dossierId" -> s"${kind.key}-${provider}-${username.toLowerCase}-${generatedAt.toEpochMilli}",
      "productKind" -> kind.key,
      "subject" -> Json.obj(
        "role" -> kind.role,
        "provider" -> provider,
        "username" -> username,
        "displayName" -> displayName
      ),
      "source" -> Json.obj(
        "requestedGameLimit" -> requestedGameLimit,
        "sampledGameCount" -> bundle.sampledGameCount,
        "eligibleGameCount" -> bundle.eligibleGameCount,
        "publicOnly" -> true,
        "includeTimeControl" -> false,
        "generatedAt" -> generatedAt.toString,
        "cache" -> Json.obj("hit" -> false, "ttlSec" -> 0)
      ),
      "status" -> status,
      "headline" -> headline,
      "summary" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           s"This notebook sampled ${bundle.sampledGameCount} of ${bundle.eligibleGameCount} recent public games and keeps the focus on openings, recurring transition points, and plans that keep repeating."
         else
           s"This notebook sampled ${bundle.sampledGameCount} of ${bundle.eligibleGameCount} recent public games and turns them into a prep surface: where to steer the game and which quiet decisions are worth forcing."),
      "overview" -> overview,
      "sections" -> JsArray(List(openingSection) ++ patternSections ++ List(exemplarSection) ++ tailSections),
      "appendix" -> Json.obj(
        "sampledGames" -> sampledGames,
        "warnings" -> JsArray(bundle.warnings.map(JsString.apply))
      )
    )

    val surface = accountSurface(
      provider = provider,
      username = username,
      displayName = displayName,
      kind = kind,
      bundle = bundle,
      generatedAt = generatedAt,
      status = status,
      headline = headline,
      summary = (dossier \ "summary").as[String],
      overview = overview,
      openingCards = openingCards,
      sections = sections,
      selectedClusters = selectedClusters,
      exemplarGames = exemplarGames
    )

    exemplarGames.headOption
      .orElse(parsed.headOption)
      .map(_.external.pgn)
      .map(PgnStr.apply)
      .map(pgn =>
        NotebookBuildArtifact(
          dossier,
          surface,
          pgn,
          bundle.sampledGameCount,
          bundle.eligibleGameCount,
          bundle.warnings,
          NotebookBuildDiagnostics(
            allClusters = allClusters,
            selectedClusters = selectedClusters
          )
        )
      )
      .toRight("No representative PGN available for notebook creation.")

  private def accountSurface(
      provider: String,
      username: String,
      displayName: String,
      kind: ProductKind,
      bundle: PrimitiveBundle,
      generatedAt: Instant,
      status: String,
      headline: String,
      summary: String,
      overview: JsObject,
      openingCards: List[JsObject],
      sections: List[ClusterSection],
      selectedClusters: List[SnapshotCluster],
      exemplarGames: List[ParsedGame]
  ): JsObject =
    val pairedPatterns = selectedClusters.zip(sections).map { case (cluster, section) =>
      patternSurface(provider, cluster, section)
    }
    val primaryExemplars = exemplarGames.take(if kind == ProductKind.MyAccountIntelligenceLite then 2 else 1)
    val actionCards =
      if kind == ProductKind.MyAccountIntelligenceLite then actionPageCards(sections, displayName)
      else steeringPlanCards(sections, displayName)
    val checklist =
      Option.when(kind == ProductKind.OpponentPrep)(checklistCard(sections, displayName))
    val confidenceScore = confidenceScoreFor(selectedClusters)
    Json.obj(
      "schema" -> "chesstory.account.surface.v1",
      "productKind" -> kind.key,
      "subject" -> Json.obj(
        "provider" -> provider,
        "username" -> username,
        "displayName" -> displayName
      ),
      "generatedAt" -> generatedAt.toString,
      "status" -> status,
      "headline" -> headline,
      "summary" -> summary,
      "source" -> Json.obj(
        "sampledGameCount" -> bundle.sampledGameCount,
        "eligibleGameCount" -> bundle.eligibleGameCount
      ),
      "confidence" -> Json.obj(
        "label" -> confidenceLabelFor(confidenceScore),
        "score" -> confidenceScore
      ),
      "warnings" -> JsArray(bundle.warnings.map(JsString.apply)),
      "overview" -> overview,
      "openingCards" -> JsArray(openingCards),
      "patterns" -> JsArray(pairedPatterns),
      "exemplarGames" -> JsArray(primaryExemplars.zipWithIndex.map { case (game, idx) =>
        exemplarCard(game, sections, kind, idx + 1, provider)
      }),
      "actions" -> JsArray(actionCards),
      "checklist" -> checklist.getOrElse(JsNull)
    )

  private def patternSurface(
      provider: String,
      cluster: SnapshotCluster,
      section: ClusterSection
  ): JsObject =
    val candidate = cluster.exemplar.candidate
    val evidenceGames =
      cluster.candidates
        .map(_.game)
        .groupBy(_.external.gameId)
        .values
        .flatMap(_.headOption)
        .toList
        .sortBy(_.external.playedAt)(Ordering[String].reverse)
        .take(4)
        .zipWithIndex
        .map { case (game, idx) =>
          (Json.obj(
            "id" -> game.external.gameId,
            "provider" -> provider,
            "externalGameId" -> game.external.gameId,
            "white" -> game.external.white,
            "black" -> game.external.black,
            "result" -> game.external.result,
            "opening" -> game.openingName,
            "role" -> (if idx == 0 then "anchor" else "support")
          ) ++ optField("sourceUrl", game.external.sourceUrl))
        }
    Json.obj(
      "id" -> cluster.id,
      "title" -> section.title,
      "summary" -> section.summary,
      "side" -> colorKey(cluster.side),
      "structureFamily" -> cluster.structureFamily,
      "triggerType" -> cluster.triggerType,
      "priorityScore" -> cluster.priorityScore,
      "collapseBackedRate" -> cluster.collapseBackedRate,
      "snapshotConfidenceMean" -> cluster.snapshotConfidenceMean,
      "evidence" -> section.evidence,
      "anchor" -> section.anchorCard.getOrElse(JsNull),
      "actions" -> JsArray(section.actions),
      "evidenceGames" -> JsArray(evidenceGames),
      "collapseBacked" -> candidate.exists(_.collapseBacked)
    )

  private[accountintel] def selectClustersForNotebook(
      kind: ProductKind,
      clusters: List[SnapshotCluster]
  ): List[SnapshotCluster] = ClusterSelector.select(kind, clusters)

  private case class ClusterSection(
      sectionId: String,
      title: String,
      summary: String,
      status: String,
      evidence: JsObject,
      anchorCard: Option[JsObject],
      actions: List[JsObject],
      anchorId: Option[String],
      anchorGameId: Option[String]
  )

  private def clusterOut(
      cluster: SnapshotCluster,
      idx: Int,
      kind: ProductKind,
      total: Int,
      showSideInTitle: Boolean
  ): ClusterSection =
    val sectionId = s"pattern-$idx-${cluster.id}"
    val evidenceSummary = evidence(cluster.support, total)
    val structureLabel =
      if showSideInTitle then s"${colorLabel(cluster.side)} ${cluster.structureFamily}" else cluster.structureFamily
    val decisionLabel = decisionLabelFor(cluster.structureFamily, cluster.triggerType)
    val title =
      if kind == ProductKind.MyAccountIntelligenceLite then s"Repair $idx: $structureLabel"
      else s"Pressure $idx: $structureLabel"
    val summary =
      if kind == ProductKind.MyAccountIntelligenceLite then
        s"${cluster.support} recent game(s) returned to $structureLabel, and the first quiet decision about $decisionLabel kept repeating."
      else
        s"${cluster.support} recent game(s) point back to $structureLabel. The practical steering point is to force an early decision about $decisionLabel."
    if cluster.ready then
      val anchor = cluster.exemplar.game
      val candidate = cluster.exemplar.candidate.get
      val anchorId = s"$sectionId-anchor"
      val moveContext = Json.obj(
        "ply" -> candidate.ply,
        "moveNumber" -> ((candidate.ply + 1) / 2),
        "sideToMove" -> colorKey(candidate.side)
      ) ++ optField("lastMoveSan", candidate.lastSan)
      val triggerLabel = candidate.triggerType.replace('_', ' ')
      val collapseBacked = candidate.collapseBacked && candidate.earliestPreventablePly.isDefined
      val collapseSummary =
        candidate.earliestPreventablePly.fold(""):
          earliest =>
            if collapseBacked then
              s" A quieter repair window shows up earlier, starting around ply $earliest."
            else s" The first quiet commitment usually becomes visible by around ply $earliest."
      val explanation =
        if kind == ProductKind.MyAccountIntelligenceLite then
          if collapseBacked then
            s"This anchor keeps leading back to an earlier quiet decision window, not just to the later mistake. Around this $triggerLabel moment, ${anchor.subjectName} could still decide $decisionLabel more cleanly.$collapseSummary"
          else
            s"This anchor repeats as a first quiet commitment in $structureLabel. Before the middlegame hardens, ${anchor.subjectName} still has time to decide $decisionLabel more cleanly.$collapseSummary"
        else if collapseBacked then
          s"This anchor is useful because the later slip traces back to an earlier quiet decision window. If you can reach this $triggerLabel moment, you can ask ${anchor.subjectName} to decide $decisionLabel before the position clarifies.$collapseSummary"
        else
          s"This anchor repeats as a first quiet commitment often enough to be a practical steering target. Reach this $triggerLabel moment, then keep the structure flexible until ${anchor.subjectName} decides $decisionLabel first.$collapseSummary"
      val antiPatternSummary =
        if collapseBacked then
          "This pattern was confirmed by an earlier quiet decision window before the later mistake appeared."
        else "This pattern keeps repeating as a first quiet commitment before the middlegame clarifies."
      val anchorCard = Json.obj(
        "cardKind" -> "anchor_position",
        "id" -> anchorId,
        "clusterId" -> s"$sectionId-cluster",
        "title" -> s"Anchor position: $structureLabel",
        "lens" -> kind.lens,
        "claim" ->
          (if kind == ProductKind.MyAccountIntelligenceLite then
             s"In $structureLabel, ${anchor.subjectName} often reaches a point where the plan around $decisionLabel is not clearly chosen."
           else
             s"In $structureLabel, ${anchor.subjectName} can often be pushed into deciding $decisionLabel too early."),
        "explanation" -> explanation,
        "fen" -> candidate.fen,
        "moveContext" -> moveContext,
        "strategicTags" -> JsArray(cluster.labels.map(JsString.apply)),
        "questionPrompt" ->
          (if kind == ProductKind.MyAccountIntelligenceLite then
             if collapseBacked then
               s"What quieter decision about $decisionLabel was available before the later mistake became inevitable?"
             else s"What first quiet commitment about $decisionLabel would have kept the plan more flexible?"
           else if collapseBacked then
             s"How can you force the earlier quiet decision about $decisionLabel before the later mistake disappears?"
           else s"How can you force this first quiet decision about $decisionLabel before the position clarifies?"),
        "recommendedPlan" -> Json.obj(
          "label" -> (if kind == ProductKind.MyAccountIntelligenceLite then "Cleaner plan"
                      else "Steering route"),
          "summary" ->
            (if kind == ProductKind.MyAccountIntelligenceLite then
               s"Keep the first quiet decision about $decisionLabel flexible until the $structureLabel structure makes the priority clear."
             else
               s"Steer toward the $structureLabel structure, then delay clarifying trades until ${anchor.subjectName} decides $decisionLabel first.")
        ),
        "antiPattern" -> Json.obj(
          "label" -> "Repeated trigger",
          "summary" ->
            candidate.planAlignmentBand.fold(antiPatternSummary): band =>
              s"$antiPatternSummary The alignment band most often shows up as $band."
        ),
        "evidence" -> (evidenceSummary ++ Json.obj(
          "supportingGameIds" -> JsArray(cluster.candidates.map(c => JsString(c.gameId)).distinct)
        )),
        "exemplarGameId" -> anchor.external.gameId
      )
      ClusterSection(
        sectionId = sectionId,
        title = title,
        summary = summary,
        status = "ready",
        evidence = evidenceSummary,
        anchorCard = Some(anchorCard),
        actions = patternActions(kind, sectionId, anchorId, structureLabel, decisionLabel),
        anchorId = Some(anchorId),
        anchorGameId = Some(anchor.external.gameId)
      )
    else
      ClusterSection(
        sectionId = sectionId,
        title = title,
        summary = summary,
        status = "partial",
        evidence = evidenceSummary,
        anchorCard = None,
        actions = List(
          Json.obj(
            "cardKind" -> "action_item",
            "id" -> s"$sectionId-action",
            "title" -> "Wait for more evidence",
            "instruction" -> "This structure appeared, but not often enough to claim a stable recurring snapshot yet.",
            "successMarker" -> "Another batch of public games confirms the same quiet decision point."
          )
        ),
        anchorId = None,
        anchorGameId = None
      )

  private def fallbackCluster(kind: ProductKind, idx: Int, total: Int): ClusterSection =
    val sectionId = s"pattern-$idx-fallback"
    ClusterSection(
      sectionId = sectionId,
      title =
        if kind == ProductKind.MyAccountIntelligenceLite then s"Repair $idx: provisional"
        else s"Pressure $idx: provisional",
      summary =
        "The sample is still thin, so this section stays provisional until more recurring structures appear.",
      status = "partial",
      evidence = evidence(1, total.max(1)),
      anchorCard = None,
      actions = List(
        Json.obj(
          "cardKind" -> "action_item",
          "id" -> s"$sectionId-action",
          "title" -> "Build a larger sample",
          "instruction" -> "Refresh the notebook after another batch of public games before treating this section as stable.",
          "successMarker" -> "At least two structures repeat with clear anchor positions."
        )
      ),
      anchorId = None,
      anchorGameId = None
    )

  private def openingCardsFor(parsed: List[ParsedGame]): List[JsObject] =
    parsed.groupBy(_.subjectColor).toList.sortBy((side, _) => if side.white then 0 else 1).flatMap {
      case (side, games) =>
        games
          .groupBy(_.openingBucket)
          .toList
          .sortBy { case (_, sameOpening) => -sameOpening.size }
          .take(1)
          .map { case (openingBucket, sameOpening) =>
            val support = sameOpening.size
            val sharePct = if games.isEmpty then 0d else support.toDouble * 100d / games.size.toDouble
            val canonicalFamily = sameOpening.head.openingFamily
            val relation = sameOpening.head.openingRelation
            val subjectName = sameOpening.head.subjectName
            val label =
              if support >= 3 && sharePct >= 15d then openingBucket
              else s"Mixed ${colorLabel(side)} repertoire"
            val titleLabel =
              if label.startsWith("Mixed ") then s"${colorLabel(side)}: Mixed repertoire"
              else s"${colorLabel(side)}: $label"
            val story =
              if support >= 3 && sharePct >= 15d then
                relation match
                  case "chose" =>
                    s"$support recent ${colorLabel(side).toLowerCase} game(s) showed $subjectName choosing $canonicalFamily most often."
                  case "faced" =>
                    s"$support recent ${colorLabel(side).toLowerCase} game(s) showed $subjectName facing $canonicalFamily most often."
                  case _ =>
                    s"$support recent ${colorLabel(side).toLowerCase} game(s) kept returning to $canonicalFamily structures."
              else
                s"No single opening bucket dominates ${colorLabel(side).toLowerCase} yet. $label appeared most often, but only in $support recent game(s)."
            Json.obj(
              "cardKind" -> "opening_map",
              "id" -> s"opening-${colorKey(side)}-${slug(label)}",
              "title" -> titleLabel,
              "side" -> colorKey(side),
              "openingFamily" -> label,
              "structureLabels" -> JsArray(
                sameOpening.flatMap(_.labels).distinct.take(4).map(JsString.apply)
              ),
              "story" -> story,
              "evidence" -> (evidence(support, parsed.size) ++ Json
                .obj("supportingGameIds" -> JsArray(sameOpening.map(g => JsString(g.external.gameId)))))
            )
          }
    }

  private def overviewCards(
      kind: ProductKind,
      displayName: String,
      openingCards: List[JsObject],
      sections: List[ClusterSection]
  ): List[JsObject] =
    val first = sections.headOption
    val second = sections.lift(1).orElse(first)
    val openingHeadline = openingIdentityHeadline(displayName, openingCards)
    if kind == ProductKind.MyAccountIntelligenceLite then
      List(
        Json.obj(
          "id" -> "overview-opening-identity",
          "kind" -> "opening_identity",
          "title" -> "Opening identity",
          "headline" -> openingHeadline,
          "summary" -> "This notebook keeps those structures separate by side before drawing repair conclusions.",
          "priority" -> "medium",
          "confidence" -> evidenceStrength(
            openingCards.headOption
              .flatMap(card => (card \ "evidence" \ "supportingGames").asOpt[Int])
              .getOrElse(1)
          ),
          "evidence" -> openingCards.headOption
            .flatMap(card => (card \ "evidence").asOpt[JsObject])
            .getOrElse(evidence(1, 1)),
          "linkedSectionId" -> "opening-map"
        ),
        Json.obj(
          "id" -> "overview-recurring-leak",
          "kind" -> "recurring_leak",
          "title" -> "Recurring leak",
          "headline" -> first.map(_.title).getOrElse("Provisional leak"),
          "summary" -> first
            .map(_.summary)
            .getOrElse("The sample is still thin, so this remains provisional."),
          "priority" -> "high",
          "confidence" -> first.map(_.evidence.value("strength")).getOrElse(JsString("weak")),
          "evidence" -> first.map(_.evidence).getOrElse(evidence(1, 1)),
          "linkedSectionId" -> first.map(_.sectionId).getOrElse("opening-map")
        ) ++ optField("linkedCardId", first.flatMap(_.anchorId)),
        Json.obj(
          "id" -> "overview-repair-priority",
          "kind" -> "repair_priority",
          "title" -> "Repair priority",
          "headline" -> second.map(_.title).getOrElse("Build the next sample"),
          "summary" -> "Start with the first stable anchor position and only then widen to more games.",
          "priority" -> "high",
          "confidence" -> second.map(_.evidence.value("strength")).getOrElse(JsString("weak")),
          "evidence" -> second.map(_.evidence).getOrElse(evidence(1, 1)),
          "linkedSectionId" -> second.map(_.sectionId).getOrElse("action-page")
        ) ++ optField("linkedCardId", second.flatMap(_.anchorId))
      )
    else
      List(
        Json.obj(
          "id" -> "overview-opening-identity",
          "kind" -> "opening_identity",
          "title" -> "Opening identity",
          "headline" -> openingHeadline,
          "summary" -> "Prep should start from the structure they revisit, not from a single forcing line.",
          "priority" -> "medium",
          "confidence" -> evidenceStrength(
            openingCards.headOption
              .flatMap(card => (card \ "evidence" \ "supportingGames").asOpt[Int])
              .getOrElse(1)
          ),
          "evidence" -> openingCards.headOption
            .flatMap(card => (card \ "evidence").asOpt[JsObject])
            .getOrElse(evidence(1, 1)),
          "linkedSectionId" -> "opening-map"
        ),
        Json.obj(
          "id" -> "overview-pressure-point",
          "kind" -> "pressure_point",
          "title" -> "Pressure point",
          "headline" -> first.map(_.title).getOrElse("General steering watchlist"),
          "summary" -> first
            .map(_.summary)
            .getOrElse("The sample is still thin, so treat this as a provisional steering cue."),
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

  private def openingIdentityHeadline(displayName: String, openingCards: List[JsObject]): String =
    val openings =
      openingCards.flatMap: card =>
        for
          side <- (card \ "side").asOpt[String]
          opening <- (card \ "openingFamily").asOpt[String]
          support <- (card \ "evidence" \ "supportingGames").asOpt[Int]
        yield (side, opening, support)
    openings match
      case List((_, white, _), (_, black, _))
          if white.startsWith("Mixed ") || black.startsWith("Mixed ") =>
        s"$displayName's opening identity is still diffuse in the recent sample, so this notebook leans more on recurring structures than on a single line."
      case List((_, _, whiteSupport), (_, _, blackSupport)) if whiteSupport < 3 || blackSupport < 3 =>
        s"$displayName's opening identity is still diffuse in the recent sample, so this notebook leans more on recurring structures than on a single line."
      case List(("white", white, _), ("black", black, _)) =>
        s"In the recent sample, $displayName most often $white with White and $black with Black."
      case List((side, opening, _)) if opening.startsWith("Mixed ") =>
        s"$displayName's opening identity is still diffuse on the ${side.capitalize.toLowerCase} side of the sample."
      case List((side, _, support)) if support < 3 =>
        s"$displayName's opening identity is still diffuse on the ${side.capitalize.toLowerCase} side of the sample."
      case List((side, opening, _)) =>
        s"In the recent sample, $displayName most often $opening with ${side.capitalize}."
      case _ =>
        s"$displayName most often returns to a small group of practical structures in the recent sample."

  private def exemplarGamesFor(sections: List[ClusterSection], parsed: List[ParsedGame]): List[ParsedGame] =
    val anchored = sections.flatMap(_.anchorGameId).distinct
    val anchoredGames = anchored.flatMap(id => parsed.find(_.external.gameId == id))
    if anchoredGames.nonEmpty then anchoredGames else parsed.take(2)

  private def exemplarCard(
      game: ParsedGame,
      sections: List[ClusterSection],
      kind: ProductKind,
      idx: Int,
      provider: String
  ): JsObject =
    val linked =
      sections.flatMap(s => s.anchorGameId.contains(game.external.gameId).option(s.anchorId).flatten).distinct
    Json.obj(
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
         else
           s"This game is a practical rehearsal for steering ${game.subjectName} into a familiar ${game.openingFamily} structure."),
      "narrative" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           "Use this exemplar to remember the earlier plan choice, not just the eventual result."
         else "Use this exemplar to rehearse how the structure develops before the opponent has to commit."),
      "linkedAnchorPositionIds" -> JsArray(linked.map(JsString.apply)),
      "takeaway" ->
        (if kind == ProductKind.MyAccountIntelligenceLite then
           s"Review the plan choice that appeared in ${game.openingFamily}, then compare it with the anchor positions."
         else
           s"If you can reach ${game.openingFamily}, steer toward the same quiet commitment this exemplar exposed.")
    ) ++ optField("turningPointPly", game.rep.map(_.snap.ply))

  private def actionPageCards(sections: List[ClusterSection], displayName: String): List[JsObject] =
    List(
      Json.obj(
        "cardKind" -> "action_item",
        "id" -> "action-page-1",
        "title" -> "Name the plan before the exchange",
        "instruction" -> "Before releasing central tension, say which file, pawn break, or minor-piece square should improve next.",
        "successMarker" -> "You can explain the next two moves in one sentence before you play them.",
        "linkedAnchorPositionIds" -> JsArray(sections.flatMap(_.anchorId).take(2).map(JsString.apply))
      ),
      Json.obj(
        "cardKind" -> "action_item",
        "id" -> "action-page-2",
        "title" -> "Replay the anchor positions",
        "instruction" -> s"Revisit the anchor positions from this notebook before the next session instead of reviewing all $displayName's games at once.",
        "successMarker" -> "The repeated structure feels familiar before move 15.",
        "linkedAnchorPositionIds" -> JsArray(sections.flatMap(_.anchorId).take(3).map(JsString.apply))
      ),
      Json.obj(
        "cardKind" -> "action_item",
        "id" -> "action-page-3",
        "title" -> "Refresh the notebook after another batch",
        "instruction" -> "Generate a new notebook after 20 more public games and compare which repair sections stayed stable.",
        "successMarker" -> "You can tell whether the same pattern is fading or still recurring.",
        "linkedAnchorPositionIds" -> Json.arr()
      )
    )

  private def steeringPlanCards(sections: List[ClusterSection], displayName: String): List[JsObject] =
    List(
      Json.obj(
        "cardKind" -> "action_item",
        "id" -> "steering-plan-1",
        "title" -> "Steer before forcing tactics",
        "instruction" -> s"Against $displayName, value the structure that appears most often in this notebook over a memorized sideline novelty.",
        "successMarker" -> "The game reaches one of the notebook anchor structures before you calculate forcing tactics.",
        "linkedAnchorPositionIds" -> JsArray(sections.flatMap(_.anchorId).take(2).map(JsString.apply))
      ),
      Json.obj(
        "cardKind" -> "action_item",
        "id" -> "steering-plan-2",
        "title" -> "Keep the first quiet decision on the board",
        "instruction" -> "Delay clarifying captures until the opponent has to declare a pawn break or piece trade first.",
        "successMarker" -> "The opponent makes the first irreversible structural choice.",
        "linkedAnchorPositionIds" -> JsArray(sections.flatMap(_.anchorId).take(2).map(JsString.apply))
      ),
      Json.obj(
        "cardKind" -> "action_item",
        "id" -> "steering-plan-3",
        "title" -> "Use the exemplar game as a rehearsal",
        "instruction" -> "Review the exemplar once before the round and focus on the plan that repeated, not the exact move order.",
        "successMarker" -> "You can describe what middlegame you want before the first irreversible exchange.",
        "linkedAnchorPositionIds" -> Json.arr()
      )
    )

  private def checklistCard(sections: List[ClusterSection], displayName: String): JsObject =
    Json.obj(
      "cardKind" -> "checklist",
      "id" -> "prep-checklist",
      "title" -> "Before the game",
      "items" -> Json.arr(
        Json.obj(
          "id" -> "checklist-1",
          "label" -> s"Know the steering target: ${sections.headOption.map(_.title).getOrElse("general middlegame control")}",
          "reason" -> "The first recurring structure matters more than memorizing a single engine line.",
          "priority" -> "high"
        ),
        Json.obj(
          "id" -> "checklist-2",
          "label" -> "Delay clarifying captures until the structure is favorable",
          "reason" -> s"$displayName's recent games repeat most often when the first quiet decision is forced early.",
          "priority" -> "high"
        ),
        Json.obj(
          "id" -> "checklist-3",
          "label" -> "Revisit the anchor positions before playing",
          "reason" -> "The notebook is strongest when you remember the plans, not just the move orders.",
          "priority" -> "medium"
        )
      )
    )

  private def patternActions(
      kind: ProductKind,
      sectionId: String,
      anchorId: String,
      structureFamily: String,
      decisionLabel: String
  ): List[JsObject] =
    List(
      Json.obj(
        "cardKind" -> "action_item",
        "id" -> s"$sectionId-action-1",
        "title" -> (if kind == ProductKind.MyAccountIntelligenceLite then "Rehearse the cleaner plan"
                    else "Rehearse the steering route"),
        "instruction" ->
          (if kind == ProductKind.MyAccountIntelligenceLite then
             s"Replay this $structureFamily anchor and name the cleaner decision about $decisionLabel before any clarifying exchange."
           else
             s"Replay this $structureFamily anchor and rehearse how to keep the decision about $decisionLabel on the board."),
        "successMarker" -> "You can describe the intended middlegame before the position simplifies.",
        "linkedAnchorPositionIds" -> Json.arr(anchorId)
      )
    )

  private def decisionLabelFor(structureFamily: String, triggerType: String): String =
    val lower = structureFamily.toLowerCase
    if lower.contains("iqp") then
      if triggerType == "tension_release" then "whether to clarify the center or keep pressure against the isolani"
      else "whether to support the isolani with pieces or reshape the center"
    else if lower.contains("locked center") then
      "which wing should carry the first pawn break"
    else if lower.contains("fianchetto") then
      "whether to commit the kingside shell or switch the fight back to the center"
    else if triggerType == "major_simplification" then
      "which trade actually improves the position"
    else if triggerType == "file_commitment" then
      "which file deserves the first rook or queen commitment"
    else "which file, break, or piece route should define the middlegame"

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

  private def confidenceScoreFor(selectedClusters: List[SnapshotCluster]): Double =
    if selectedClusters.isEmpty then 0.35
    else
      BigDecimal(
        selectedClusters.map(cluster =>
          (cluster.snapshotConfidenceMean * 0.7) + (cluster.collapseBackedRate * 0.3)
        ).sum / selectedClusters.size.toDouble
      ).setScale(2, RoundingMode.HALF_UP).toDouble

  private def confidenceLabelFor(score: Double): String =
    if score >= 0.74 then "strong"
    else if score >= 0.56 then "medium"
    else "weak"

  private def sharePct(part: Int, total: Int): Double =
    if total <= 0 then 0d
    else BigDecimal(part.toDouble * 100d / total.toDouble).setScale(1, RoundingMode.HALF_UP).toDouble

  private def optField[T: Writes](name: String, value: Option[T]): JsObject =
    value.fold(Json.obj())(v => Json.obj(name -> v))
