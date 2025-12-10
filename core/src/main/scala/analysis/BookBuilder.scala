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
    // Simple greedy segmenter
    val sections = List.newBuilder[BookSection]
    var buffer = Vector.empty[PlyOutput]
    var currentType = SectionType.StructuralDeepDive // Default
    var startIdx = offsetStr

    def flush(narrative: String) =
      if buffer.nonEmpty then
        sections += createSection(
          determineTitle(currentType), 
          currentType, 
          buffer, 
          narrative
        )
        buffer = Vector.empty

    plys.foreach { p =>
      // Determine ply "nature"
      val isCrisis = p.roles.exists(r => r == "GameDecider" || r == "BlunderPunishment")
      val isTactic = p.roles.exists(r => r == "Violence" || r == "Complication")
      
      val targetType = 
        if isCrisis then SectionType.CriticalCrisis
        else if isTactic then SectionType.TacticalStorm
        else SectionType.StructuralDeepDive // or NarrativeBridge
      
      if targetType != currentType && buffer.nonEmpty then
        // If switching types, flush current buffer
        // Heuristic: Don't switch for single ply unless High Importance?
        // Let's switch.
        flush(getNarrativeHint(currentType))
        currentType = targetType
      
      buffer = buffer :+ p
    }
    flush(getNarrativeHint(currentType))
    sections.result()

  private def createSection(title: String, tpe: SectionType, plys: Vector[PlyOutput], narrative: String): BookSection =
    // Select relevant diagrams from this chunk (not every ply needs a diagram)
    // Heuristic: Filter interesting ones
    val diagPlys = plys.filter(p => p.roles.nonEmpty || p.conceptLabels.exists(c => c.structureTags.nonEmpty || c.tacticTags.nonEmpty || c.mistakeTags.nonEmpty))
    val diagrams = diagPlys.map(toBookDiagram).toList
    
    BookSection(
      title = title,
      sectionType = tpe,
      diagrams = diagrams,
      narrativeHint = narrative,
      startPly = plys.headOption.map(_.ply.value).getOrElse(0),
      endPly = plys.lastOption.map(_.ply.value).getOrElse(0)
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
