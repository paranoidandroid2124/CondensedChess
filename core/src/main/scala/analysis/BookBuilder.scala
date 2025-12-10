package chess
package analysis

import chess.analysis.AnalysisModel.PlyOutput
import chess.analysis.BookModel.*
import chess.analysis.ConceptLabeler.{ MistakeTag, TacticTag, TransitionTag }
import org.slf4j.LoggerFactory

object BookBuilder:
  private val logger = LoggerFactory.getLogger("chess.analysis.BookBuilder")

  def buildBook(timeline: Vector[PlyOutput], meta: GameMeta): Book =
    
    // 1. Summaries (Keep existing logic for index/checklists)
    val turningPoints = timeline
      .filter(p => p.roles.exists(r => r == "Equalizer" || r == "GameDecider" || r == "BlunderPunishment" || r == "MissedOpportunity"))
      .map(toTurningPoint)
      .toList

    val tacticalMoments = timeline
      .filter(p => p.roles.exists(r => r == "Violence" || r == "Complication")) 
      .map(toTacticalMoment)
      .toList

    // 2. Diagrams (legacy filtering, maybe unused in new sections but kept for safety if needed)
    // Actually, sections will contain diagrams. We don't need a flat list in Book anymore if sections cover it.
    // But BookModel defined 'diagrams' removed? No, I removed it in previous step.
    
    // 3. Storyboard Sections
    val sections = storyboard(timeline)

    // 4. Checklist (Updated to use new sections/tags)
    val checklist = buildChecklist(timeline, sections, turningPoints, tacticalMoments)

    // Observability Logging (Phase 21)
    val sectionStats = sections.groupBy(_.sectionType.toString).map { case (k, v) => s"$k: ${v.size}" }.mkString(", ")
    val greedCount = timeline.count(_.conceptLabels.exists(_.mistakeTags.contains(MistakeTag.Greed)))
    // Checking all "Refuted" tags (CentralBreakRefuted, KingsideAttackRefuted, etc.)
    val refuteAll = timeline.count(_.conceptLabels.exists(c => 
       c.planTags.exists(_.toString.contains("Refuted"))
    ))

    logger.info(s"Generated Book: $sectionStats. Tags: Greed=$greedCount, Refuted=$refuteAll")

    Book(
      gameMeta = meta,
      sections = sections,
      turningPoints = turningPoints,
      tacticalMoments = tacticalMoments,
      checklist = checklist
    )

  def storyboard(timeline: Vector[PlyOutput]): List[BookSection] =
    if timeline.isEmpty then return Nil

    val sections = List.newBuilder[BookSection]
    
    // A. Opening (Ply 0 to ~20 or Transition)
    // Find generic "Endgame" transition to cap everything
    val endgameStart = timeline.indexWhere(p => 
       p.conceptLabels.exists(_.transitionTags.contains(TransitionTag.EndgameTransition))
    )
    val limit = if endgameStart != -1 then endgameStart else timeline.size
    
    val openingEnd = Math.min(limit, 16) // Heuristic: first 8 moves
    if openingEnd > 0 then
      sections += createSection(
        "Opening Phase", 
        SectionType.OpeningPortrait, 
        timeline.take(openingEnd), 
        "Key development decisions and opening strategy."
      )

    // B. Middlegame (openingEnd until limit)
    if openingEnd < limit then
      val mgPlys = timeline.slice(openingEnd, limit)
      sections ++= segmentMiddlegame(mgPlys, openingEnd)

    // C. Endgame (from limit to end)
    if endgameStart != -1 then
      val egPlys = timeline.drop(endgameStart)
      sections += createSection(
        "Endgame", 
        SectionType.EndgameMasterclass, 
        egPlys, 
        "Conversion and technical play in the endgame."
      )

    sections.result()

  private def segmentMiddlegame(plys: Vector[PlyOutput], offsetStr: Int): List[BookSection] =
    // Smoother Segmentation: Group by chunks of ~8-12 moves, or break at major Crisis.
    // We want to avoid 1-move sections.
    val sections = List.newBuilder[BookSection]
    val MIN_CHUNK_SIZE = 6
    val IDEAL_CHUNK_SIZE = 12

    var buffer = Vector.empty[PlyOutput]
    var currentType = SectionType.StructuralDeepDive // Default starting type
    // We will determine the "Type" of the section *after* collecting the chunk, based on its dominant content.
    
    // Explicit Turning Points that MUST start a new section (unless too close to start)
    // - GameDecider (Role)
    // - BlunderPunishment (Role)
    // - High concept shift?
    
    plys.foreach { p =>
      val isCrisis = p.roles.exists(r => r == "GameDecider" || r == "BlunderPunishment")
      val isViolence = p.roles.exists(r => r == "Violence" || r == "Complication")
      
      // Decision to flush:
      // 1. If buffer >= IDEAL_CHUNK_SIZE
      // 2. If buffer >= MIN_CHUNK_SIZE AND we hit a Crisis (major turning point)
      
      val hitLimit = buffer.size >= IDEAL_CHUNK_SIZE
      val hitCrisis = buffer.size >= MIN_CHUNK_SIZE && isCrisis
      
      if (hitLimit || hitCrisis) then
        // Flush buffer
        val (finalType, finalTitle) = analyzeChunk(buffer)
        sections += createSection(finalTitle, finalType, buffer, getNarrativeHint(finalType))
        buffer = Vector.empty
      
      buffer = buffer :+ p
    }
    
    // Flush remaining
    if buffer.nonEmpty then
      val (finalType, finalTitle) = analyzeChunk(buffer)
      sections += createSection(finalTitle, finalType, buffer, getNarrativeHint(finalType))
      
    sections.result()

  private def analyzeChunk(plys: Vector[PlyOutput]): (SectionType, String) =
    // Determine the dominant character of the chunk
    val tacticalCount = plys.count(p => p.roles.exists(r => r == "Violence" || r == "Complication") || p.conceptLabels.exists(_.tacticTags.nonEmpty))
    val crisisCount = plys.count(p => p.roles.exists(r => r == "GameDecider" || r == "BlunderPunishment"))
    val blunderCount = plys.count(p => p.conceptLabels.exists(_.mistakeTags.nonEmpty))
    
    val total = plys.size
    val tacticRatio = tacticalCount.toDouble / total
    
    if crisisCount > 0 then
      (SectionType.CriticalCrisis, "Critical Turning Point")
    else if tacticRatio > 0.4 then
      (SectionType.TacticalStorm, "Tactical Complications")
    else if blunderCount >= 2 then
      (SectionType.StructuralDeepDive, "Missed Opportunities") // Or Strategic Errors? keeping it simple
    else
      // Default to Structural/Strategic
      (SectionType.StructuralDeepDive, "Strategic Maneuvering")

  private def createSection(title: String, tpe: SectionType, plys: Vector[PlyOutput], narrative: String): BookSection =
    // Smart Diagram Selection:
    // Only pick diagrams for High Importance moves or spaced intervals
    val candidatePlys = plys.filter { p =>
      val isCrisis = p.roles.exists(r => r == "GameDecider" || r == "BlunderPunishment")
      val isTactic = p.roles.exists(r => r == "Violence" || r == "Complication")
      val isHighDelta = Math.abs(p.deltaWinPct) > 5.0
      val hasStructureChange = p.conceptLabels.exists(_.structureTags.nonEmpty)
      
      isCrisis || isTactic || isHighDelta || (hasStructureChange && Math.abs(p.deltaWinPct) > 2.0)
    }

    // Filter for spacing (min 4 plies gap)
    var selectedDiagrams = Vector.empty[PlyOutput]
    var lastPly = -10
    
    // User Request: For Opening, show the LAST move (final state) instead of the first
    // So we don't force head here. We will ensure last is added later.

    candidatePlys.foreach { p =>
      if (p.ply.value - lastPly >= 4) then
        selectedDiagrams = selectedDiagrams :+ p
        lastPly = p.ply.value
    }

    // If section is long (>10 plies) and no diagrams selected, add the LAST one (final state)
    // ALSO: If it's an Opening Portrait, we MUST include the final state as per user request
    val forceLast = (selectedDiagrams.isEmpty && plys.length > 8) || (tpe == SectionType.OpeningPortrait && plys.nonEmpty)
    
    if (forceLast) {
       val last = plys.last
       // Avoid duplicate if candidate selection already picked it
       if (!selectedDiagrams.exists(_.ply.value == last.ply.value)) {
          selectedDiagrams = selectedDiagrams :+ last
       }
    }

    BookSection(
      title = title,
      sectionType = tpe,
      narrativeHint = narrative,
      startPly = plys.headOption.map(_.ply.value).getOrElse(0),
      endPly = plys.lastOption.map(_.ply.value).getOrElse(0),
      diagrams = selectedDiagrams.map(toBookDiagram).toList,
      metadata = None
    )

  private def determineTitle(tpe: SectionType): String =
    tpe match
      case SectionType.OpeningPortrait => "The Opening"
      case SectionType.CriticalCrisis => "Critical Turning Point"
      case SectionType.TacticalStorm => "Tactical Complications"
      case SectionType.StructuralDeepDive => "Strategic Maneuvering"
      case SectionType.EndgameMasterclass => "Endgame Technique"
      case SectionType.NarrativeBridge => "Game Flow"
      // New checklist-aligned types (Phase 1)
      case SectionType.TitleSummary => "Game Summary"
      case SectionType.KeyDiagrams => "Key Positions"
      case SectionType.OpeningReview => "Opening Analysis"
      case SectionType.TurningPoints => "Turning Points"
      case SectionType.TacticalMoments => "Tactical Highlights"
      case SectionType.MiddlegamePlans => "Strategic Plans"
      case SectionType.EndgameLessons => "Endgame Lessons"
      case SectionType.FinalChecklist => "Key Takeaways"

  private def getNarrativeHint(tpe: SectionType): String =
    tpe match
      case SectionType.CriticalCrisis => "Analysis of the decisive moment."
      case SectionType.TacticalStorm => "A sharp tactical sequence."
      case SectionType.StructuralDeepDive => "Positional play and planning."
      case _ => "Continuing the game."

  private def toBookDiagram(p: PlyOutput): BookDiagram =
    val labels = p.conceptLabels.get
    BookDiagram(
      id = p.ply.value.toString,
      fen = p.fen,
      roles = p.roles,
      ply = p.ply.value,
      tags = TagBundle(
        structure = labels.structureTags,
        plan = labels.planTags,
        tactic = labels.tacticTags,
        mistake = labels.mistakeTags,
        endgame = labels.endgameTags,
        transition = labels.transitionTags
      )
    )

  private def toTurningPoint(p: PlyOutput): BookTurningPoint =
    val cl = p.conceptLabels.get
    val bestLine = p.evalBeforeDeep.lines.headOption
    val bestEval = bestLine.flatMap(_.cp).getOrElse(0)
    val bestMove = bestLine.map(_.pv.headOption.getOrElse("")).getOrElse("?")
    
    BookTurningPoint(
      ply = p.ply.value,
      side = p.turn.name,
      playedMove = p.uci,
      bestMove = bestMove,
      evalBefore = p.evalBeforeDeep.lines.headOption.flatMap(_.cp).getOrElse(0),
      evalAfterPlayed = p.playedEvalCp.getOrElse(0),
      evalAfterBest = bestEval, 
      mistakeTags = cl.mistakeTags
    )

  private def toTacticalMoment(p: PlyOutput): BookTacticalMoment =
    val cl = p.conceptLabels.get
    val isMiss = cl.mistakeTags.contains(MistakeTag.TacticalMiss) || cl.tacticTags.contains(TacticTag.TacticalPatternMiss)
    val gain = if isMiss then 
       val best = p.evalBeforeDeep.lines.headOption.flatMap(_.cp).getOrElse(0)
       val played = p.playedEvalCp.getOrElse(0)
       Some(best - played)
    else None

    BookTacticalMoment(
      ply = p.ply.value,
      side = p.turn.name,
      motifTags = cl.tacticTags,
      evalGainIfPlayed = gain,
      wasMissed = isMiss
    )

  private def buildChecklist(
      timeline: Vector[PlyOutput],
      sections: List[BookSection],
      turning: List[BookTurningPoint],
      tactics: List[BookTacticalMoment]
  ): List[ChecklistBlock] =
    val blocks = List.newBuilder[ChecklistBlock]
    
    // 1. Structure (Gather from Structural Sections)
    val structTags = sections.filter(_.sectionType == SectionType.StructuralDeepDive)
      .flatMap(_.diagrams.flatMap(_.tags.structure))
      .distinct
      .map(_.toString)
    if structTags.nonEmpty then blocks += ChecklistBlock("Strategic Themes", structTags)

    // 2. Tactics
    val tacticTags = tactics.flatMap(_.motifTags).distinct.map(_.toString)
    if tacticTags.nonEmpty then blocks += ChecklistBlock("Tactical Motifs", tacticTags)

    // 3. Mistakes
    val mistakeTags = turning.flatMap(_.mistakeTags).distinct.map(_.toString)
    if mistakeTags.nonEmpty then blocks += ChecklistBlock("Critical Errors", mistakeTags)

    // 4. Endgame
    val endgameTags = sections.filter(_.sectionType == SectionType.EndgameMasterclass)
      .flatMap(_.diagrams.flatMap(_.tags.endgame))
      .distinct
      .map(_.toString)
    if endgameTags.nonEmpty then blocks += ChecklistBlock("Endgame Skills", endgameTags)

    blocks.result()
