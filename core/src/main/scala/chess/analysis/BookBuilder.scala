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

    // 4. Checklist (Disabled)
    val checklist = Nil

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
        "Opening Analysis", 
        SectionType.OpeningReview, 
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
        "Endgame Lessons", 
        SectionType.EndgameLessons, 
        egPlys, 
        "Conversion and technical play in the endgame."
      )

    sections.result()

  private def segmentMiddlegame(plys: Vector[PlyOutput], _offsetStr: Int): List[BookSection] =
    // Smoother Segmentation: Group by chunks of ~8-12 moves, or break at major Crisis.
    // We want to avoid 1-move sections.
    val sections = List.newBuilder[BookSection]
    val MIN_CHUNK_SIZE = 6
    val IDEAL_CHUNK_SIZE = 12

    var buffer = Vector.empty[PlyOutput]
    
    plys.foreach { p =>
      val isCrisis = p.roles.exists(r => r == "GameDecider" || r == "BlunderPunishment")
      
      val hitLimit = buffer.size >= IDEAL_CHUNK_SIZE
      val hitCrisis = buffer.size >= MIN_CHUNK_SIZE && isCrisis
      
      if (hitLimit || hitCrisis) then
        // Flush buffer
        val (finalType, finalTitle) = analyzeChunk(buffer)
        val specificHint = generateSpecificHint(buffer, finalType)
        sections += createSection(finalTitle, finalType, buffer, specificHint)
        buffer = Vector.empty
      
      buffer = buffer :+ p
    }
    
    // Flush remaining
    if buffer.nonEmpty then
      val (finalType, finalTitle) = analyzeChunk(buffer)
      val specificHint = generateSpecificHint(buffer, finalType)
      sections += createSection(finalTitle, finalType, buffer, specificHint)
      
    sections.result()

  private def analyzeChunk(plys: Vector[PlyOutput]): (SectionType, String) =
    // Determine the dominant character of the chunk
    val tacticalCount = plys.count(p => p.roles.exists(r => r == "Violence" || r == "Complication") || p.conceptLabels.exists(_.tacticTags.nonEmpty))
    val crisisCount = plys.count(p => p.roles.exists(r => r == "GameDecider" || r == "BlunderPunishment"))
    val blunderCount = plys.count(p => p.conceptLabels.exists(_.mistakeTags.nonEmpty))
    
    val total = plys.size
    val tacticRatio = tacticalCount.toDouble / total
    
    if crisisCount > 0 then
      (SectionType.TurningPoints, "Turning Point")
    else if tacticRatio > 0.4 then
      (SectionType.TacticalStorm, "Tactical Storm")
    else if blunderCount >= 2 then
      (SectionType.MiddlegamePlans, "Missed Opportunities") 
    else
      (SectionType.MiddlegamePlans, "Strategic Plans")

  private def generateSpecificHint(plys: Vector[PlyOutput], tpe: SectionType): String =
    // Try to find dominant plan tags
    val plans = plys.flatMap(_.conceptLabels).flatMap(_.planTags).groupBy(identity).maxByOption(_._2.size).map(_._1.toString.replaceAll("([a-z])([A-Z])", "$1 $2"))
    
    tpe match
      case SectionType.TurningPoints => "Analysis of the decisive moment."
      case SectionType.TacticalStorm => "A sharp tactical sequence."
      case SectionType.MiddlegamePlans => 
         plans.map(p => s"Focus on $p.").getOrElse("Positional play and planning.")
      case _ => "Continuing the game."

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
    val forceLast = (selectedDiagrams.isEmpty && plys.length > 8) || (tpe == SectionType.OpeningReview && plys.nonEmpty)
    
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

  // private def determineTitle(tpe: SectionType): String = ...

  // private def getNarrativeHint(tpe: SectionType): String = ...

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


