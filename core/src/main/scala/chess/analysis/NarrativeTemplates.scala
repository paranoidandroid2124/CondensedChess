package chess
package analysis

import AnalysisModel.StudyChapterMetadata

/** Builds rich narrative context for LLM-based chapter summaries.
  * Transforms semantic tags + concept scores into storytelling prompts.
  */
object NarrativeTemplates:

  /** Narrative arc classification */
  enum Arc:
    case RisingTension    // Advantage growing
    case Collapse         // Advantage lost
    case Turnaround       // Underdog gains ground
    case SteadyGrind      // Small eval changes
    case CriticalMoment   // Decisive swing (large delta)

  /** Detect narrative arc from delta and context */
  def detectArc(deltaWinPct: Double, winPctBefore: Double): Arc =
    if deltaWinPct <= -10 then
      // If we were already losing badly, it's just more collapse.
      // If we were winning (>= 60), it's a Collapse.
      Arc.Collapse
    else if deltaWinPct <= -3 then Arc.CriticalMoment
    else if deltaWinPct >= 10 then
      // Turnaround only if previously disadvantage or equal
      if winPctBefore <= 45 then Arc.Turnaround
      else Arc.RisingTension // Already winning, just winning more
    else if deltaWinPct >= 3 then Arc.RisingTension
    else Arc.SteadyGrind

  // --- Tag Logic Refactor ---

  case class TagMeta(id: String, priority: Int, title: String, fragment: String)

  // Centralized Tag Strategy Definitions
  private val tagStrategies: Map[String, TagMeta] = List(
    TagMeta("king_safety_crisis", 10, "King in Danger", "a direct attack against the king becomes realistic"),
    TagMeta("tactical_complexity", 9, "Tactical Storm", "tactical complications blur the evaluation"),
    TagMeta("mate_threat", 9, "Mating Net", "a mating attack is threatened"), // Added common one
    
    // Positional / Strategic (High Priority)
    TagMeta("fortress_defense", 8, "Fortress Attempt", "the defender may build a fortress despite material deficit"),
    TagMeta("conversion_difficulty", 8, "Technical Task", "converting the edge will require patience and clean technique"),
    TagMeta("conversion_difficulty_endgame", 8, "Endgame Precision", "the technical endgame demands precision"),
    TagMeta("pawn_storm", 8, "Pawn Storm", "an aggressive pawn storm advances with tempo"),
    
    // Piece Activity (Medium Priority)
    TagMeta("strong_knight", 7, "The Octopus Knight", "a powerfully placed knight dominates the position"),
    TagMeta("bishop_pair_advantage", 7, "Bishop Pair", "the bishop pair exerts long-range pressure"),
    TagMeta("active_rooks", 7, "Rook Invasion", "active rooks infiltrate along open files"),
    
    // Weaknesses / Static (Medium-Low)
    TagMeta("weak_f7", 6, "Weak F7", "the home-square weakness on f7 remains a long-term target"),
    TagMeta("weak_f2", 6, "Weak F2", "the home-square weakness on f2 remains a long-term target"),
    TagMeta("bad_bishop", 6, "Bad Bishop Woes", "a bishop is restricted by its own pawn chain"),
    TagMeta("restricted_bishop", 6, "Passive Bishop", "a bishop is passively placed"),
    TagMeta("opposite_color_bishops", 6, "Opposite Color Bishops", "opposite-colored bishops tilt the game toward drawing motifs"),
    TagMeta("locked_position", 5, "Locked Center", "the locked pawn structure leaves both sides with limited pawn breaks")
  ).map(t => t.id -> t).toMap

  /** Select top 2-3 most important tags for narrative */
  def selectKeyTags(tags: List[String]): List[String] =
    tags
      .flatMap(tagStrategies.get)
      .sortBy(-_.priority)
      .take(3)
      .map(_.id)

  /** Generate narrative template based on tag combinations */
  def narrativeTemplate(tags: List[String], arc: Arc): String =
    val keyTags = selectKeyTags(tags)
    
    // Tag-based narrative fragments
    val fragments = keyTags.flatMap(t => tagStrategies.get(t).map(_.fragment))

    // Arc-based framing
    val arcFraming = arc match
      case Arc.RisingTension => "The pressure increases as"
      case Arc.Collapse => "The advantage slips away when"
      case Arc.Turnaround => "The game turns in favour of the other side when"
      case Arc.SteadyGrind => "The position slowly shifts as"
      case Arc.CriticalMoment => "A critical moment arises when"

    val joined =
      if fragments.isEmpty then "one side improves its position"
      else if fragments.length == 1 then fragments.head
      else fragments.init.mkString(", ") + ", and " + fragments.last

    s"$arcFraming $joined."

  def buildChapterMetadata(
      tags: List[String],
      phase: String,
      arc: Arc
  ): StudyChapterMetadata =
    val keyTags = selectKeyTags(tags)
    
    // 1. Generate Title (Priority: Top Tag Title -> Arc Title -> Phase Fallback)
    val title = keyTags.headOption.flatMap(tagStrategies.get).map(_.title).getOrElse {
      arc match
        case Arc.Collapse => "The Turning Point"
        case Arc.CriticalMoment => "Critical Decision"
        case Arc.Turnaround => "The Comeback"
        case Arc.RisingTension => "Building Pressure"
        case _ => if phase == "endgame" then "Endgame Grind" else "Strategic Battle"
    }

    // 2. Generate Description
    val description = narrativeTemplate(tags, arc)

    StudyChapterMetadata(
      name = title,
      description = description
    )

  /** Build rich context for LLM prompt */
  def buildContext(
      anchorPly: Int,
      played: String,
      best: Option[String],
      deltaWinPct: Double,
      tags: List[String],
      phase: String,
      winPctBefore: Double,
      winPctAfter: Double,
      opponentRobustness: Option[Double] = None,
      practicalMove: Option[String] = None
  ): String =
    val arc = detectArc(deltaWinPct, winPctBefore)
    val keyTags = selectKeyTags(tags)
    val template = narrativeTemplate(tags, arc)
    
    val phaseLabel = phase match
      case "opening" => "Opening Phase"
      case "middlegame" => "Middlegame"
      case "endgame" => "Endgame"
      case _ => "Game Phase"

    val moveStr = plyToMove(anchorPly)

    val advantageContext =
      if winPctBefore >= 70 then "White was clearly winning"
      else if winPctBefore >= 55 then "White had a slight advantage"
      else if winPctBefore >= 45 then "The position was roughly equal"
      else if winPctBefore >= 30 then "Black had a slight advantage"
      else "Black was clearly winning"

    val changeContext =
      if deltaWinPct <= -5 then s"lost ${fmt(-deltaWinPct)}% advantage"
      else if deltaWinPct <= -1 then s"conceded ${fmt(-deltaWinPct)}%"
      else if deltaWinPct >= 5 then s"gained ${fmt(deltaWinPct)}% advantage"
      else if deltaWinPct >= 1 then s"improved by ${fmt(deltaWinPct)}%"
      else "maintained the balance"


    val practicalContext = practicalMove.map(m => s"Practical alternative: $m (easier to play).").getOrElse("")

    val opponentContext = opponentRobustness match
      case Some(or) if or < 0.3 =>
        val estimatedMoves = if or < 0.15 then "only 1-2 good defensive moves" else "a narrow defensive window"
        s"""
| OPPONENT PERSPECTIVE (Pressure Point):
| - This move created a difficult defensive problem for the opponent
| - Opponent Robustness: ${fmt(or)} (${estimatedMoves})
| - The opponent must find precise moves to avoid worse outcomes
| """
      case Some(or) =>
        s"""
| OPPONENT PERSPECTIVE:
| - Opponent Robustness: ${fmt(or)} (multiple good replies available)
| """
      case None => ""

    s"""
| CONTEXT:
| - Phase: $phaseLabel (Move $moveStr)
| - Before: $advantageContext (${fmt(winPctBefore)}%)
| - After: ${fmt(winPctAfter)}% ($changeContext)
| - Played: $played
| - Best: ${best.getOrElse("N/A")}
| - $practicalContext
| - Arc: ${arc.toString}
| - Key Themes: ${keyTags.mkString(", ")}
| ${opponentContext}
| NARRATIVE TEMPLATE:
| $template
| 
| INSTRUCTIONS:
| Write a concise, insightful paragraph (3-4 sentences) in the style of a modern instructional chess book:
| - First state the evaluation shift clearly (clear edge / slight edge / equal / unpleasant defence).
| - Then explain the key plan or idea, using the Theme tags provided as guidance.
| - Add a concise takeaway naming the central theme (e.g., "Theme/Takeaway: locked structure → manoeuvre slowly").
| - Use concrete chess vocabulary; avoid hype or filler.
| - Keep paragraphs short and objective.
| - If a Practical alternative is listed, mention why it might be a good choice (e.g. "safer", "simpler plan") compared to the engine best.
| Optional: you may use a brief "Question: …? Answer: …" to highlight an instructive choice.
| ${if opponentContext.nonEmpty then "- If this is a Pressure Point, stress why the defence is difficult." else ""}
| """.stripMargin

  private def plyToMove(ply: Int): String =
    val fullMove = (ply + 1) / 2
    if ply % 2 != 0 then s"${fullMove}. w" else s"${fullMove}... b"

  private def fmt(d: Double): String = f"$d%.1f"

  /** Build prompt for a specific book section */
  def buildSectionPrompt(
      sectionType: BookModel.SectionType,
      title: String,
      plys: Vector[AnalysisModel.PlyOutput],
      diagrams: List[BookModel.BookDiagram]
  ): String =
    val startPly = plys.headOption.map(_.ply.value).getOrElse(0)
    val endPly = plys.lastOption.map(_.ply.value).getOrElse(0)
    
    // Aggregate data for context
    val keyRoles = plys.flatMap(_.roles).groupBy(identity).mapValues(_.size).toList.sortBy(-_._2).take(3).map(_._1)
    val avgDyn = if plys.nonEmpty then plys.map(_.concepts.dynamic).sum / plys.size else 0.0

    
    // Detailed Timeline Evidence (Hybrid Approach)
    val keyMoments = diagrams.flatMap { d =>
      plys.find(_.ply.value == d.ply).map { p =>
        val moveNumber = (p.ply.value + 1) / 2
        val dots = if p.ply.value % 2 != 0 then "." else "..."
        val moveLabel = s"$moveNumber$dots ${p.san}"
        
        val delta = p.deltaWinPct
        val rawTags = p.semanticTags ++ p.mistakeCategory.toList
        // Filter important tags for the story
        val storyTags = rawTags.filter(t => 
           t.contains("Greed") || t.contains("Refuted") || t.contains("Premature") ||
           t.contains("Mistake") || t.contains("Blunder") || t.contains("Sacrifice") || t.contains("Mate") ||
           t.contains("Fork") || t.contains("Pin") || t.contains("Skewer") || t.contains("Discovered")
        )
        val tagStr = if storyTags.nonEmpty then s"[${storyTags.mkString(", ")}]" else ""
        val evalStr = if delta < -2 then s"Dropped ${fmt(-delta)}%" else if delta > 2 then s"Gained ${fmt(delta)}%" else "Neutral"
        
        s"Move $moveLabel: $tagStr - $evalStr"
      }
    }.mkString("\n      ")
    
    // Extract structural context from PositionFeatures (if available)
    val structuralContext = plys.headOption.flatMap(_.fullFeatures).map { f =>
      val hints = List.newBuilder[String]
      
      // Pawn Structure
      if f.pawns.whiteIQP then hints += "White has an Isolated Queen's Pawn (IQP)"
      if f.pawns.blackIQP then hints += "Black has an Isolated Queen's Pawn (IQP)"
      if f.pawns.whitePassedPawns > 0 then hints += s"White has ${f.pawns.whitePassedPawns} passed pawn(s)"
      if f.pawns.blackPassedPawns > 0 then hints += s"Black has ${f.pawns.blackPassedPawns} passed pawn(s)"
      if f.pawns.whiteHangingPawns then hints += "White has hanging pawns (c+d file)"
      if f.pawns.blackHangingPawns then hints += "Black has hanging pawns (c+d file)"
      
      // King Safety
      if f.kingSafety.whiteKingExposedFiles >= 2 then hints += "White's king is exposed (2+ open files nearby)"
      if f.kingSafety.blackKingExposedFiles >= 2 then hints += "Black's king is exposed (2+ open files nearby)"
      if f.kingSafety.whiteBackRankWeak then hints += "White has a weak back rank"
      if f.kingSafety.blackBackRankWeak then hints += "Black has a weak back rank"
      
      // Activity
      if f.activity.whiteKnightOutposts > 0 then hints += s"White has ${f.activity.whiteKnightOutposts} knight(s) on outpost(s)"
      if f.activity.blackKnightOutposts > 0 then hints += s"Black has ${f.activity.blackKnightOutposts} knight(s) on outpost(s)"
      
      // Space
      val spaceDiff = f.space.whiteCentralSpace - f.space.blackCentralSpace
      if spaceDiff >= 3 then hints += "White has a significant space advantage"
      if spaceDiff <= -3 then hints += "Black has a significant space advantage"
      
      if hints.result().nonEmpty then s"\n      |Structural Notes: ${hints.result().mkString("; ")}" else ""
    }.getOrElse("")
    
    val context = s"""
      |SECTION CONTEXT:
      |Type: $sectionType
      |Title Hint: $title
      |Move Range: $startPly - $endPly (${plyToMove(startPly)} to ${plyToMove(endPly)})
      |Dominant Roles: ${keyRoles.mkString(", ")}
      |Avg Dynamic Score: ${fmt(avgDyn)}$structuralContext
      |Key Moments (Evidence):
      |      $keyMoments
    """.stripMargin

    val commonInstructions =
      """
        |STYLE RULES:
        |- Write like a chess book author, not a computer report.
        |- ALWAYS cite specific moves (e.g., "15.Nxe5") and squares, but ONLY if they are explicitly in the source data.
        |- **NO ORIGIN ASSUMPTIONS**: If input says "Nxe5", do NOT write "The knight *from f3* captures..." unless you know it was on f3.
        |- Use natural chess language: "wins a pawn", "creates threats", "exposes the king".
        |- NEVER use: win percentages, "eval", "deltaWinPct", "conceptShift", or technical jargon.
        |- STRICT PERSPECTIVE RULE: NEVER use "You", "Your", "We", "I". Always use "White" and "Black". The PGN does not identify the hero.
        |- Be SPECIFIC about WHAT happened: "After 15.Nxe5, the knight forks the king and rook".
        |
        |OUTPUT FORMAT:
        |Return a valid JSON object with the following fields:
        |- "title": A short, engaging title (3-5 words) that captures the essence of the section.
        |- "narrative": A concise paragraph (3-4 sentences) describing the action with SPECIFIC moves.
        |- "theme": A 1-2 word strategic theme (e.g. "Space Advantage", "King Attack", "Endgame Grind").
        |- "atmosphere": A 1-word adjective describing the tension (e.g. "Tense", "Calm", "Chaotic", "Desperate").
        |- "context": A subset of key metrics or facts (e.g. {"Material": "Equal", "Mistakes": "None"}).
        |
        |BAD EXAMPLE:
        |"The position's dynamism increased and tactical complexity shifted the evaluation."
        |
        |GOOD EXAMPLE:
        |"After 15.Nxe5!, White wins the d7 pawn because Black's bishop is overloaded defending both d7 and b7. Black tried 15...Qe7 but 16.Bc4 pins the f7 pawn to the king."
        |
        |JSON EXAMPLE:
        |{
        |  "title": "A Sudden King Attack",
        |  "narrative": "White seized the initiative with 18.Bxh7+! Kxh7 19.Ng5+, a classic Greek Gift sacrifice. Black's king was forced into the open after Kg6 20.Qd3+ f5 21.Qg3, and White had a winning attack.",
        |  "theme": "King Attack",
        |  "atmosphere": "Chaotic",
        |  "context": { "Material": "-1 Piece", "Key Moment": "Move 18" }
        |}
      """.stripMargin

    sectionType match
      case BookModel.SectionType.OpeningReview =>
        s"""$context
           |GOAL: Describe the opening phase.
           |INSTRUCTIONS:
           |- Explain which side emerged with an advantage.
           |- Identify the opening line if clear.
           |$commonInstructions
         """.stripMargin
         
      case BookModel.SectionType.TacticalStorm =>
        s"""$context
           |GOAL: Describe a tactical sequence.
           |INSTRUCTIONS:
           |- Highlight the intensity and complexity.
           |- Mention who initiated the complications.
           |$commonInstructions
         """.stripMargin
         
      case BookModel.SectionType.TurningPoints =>
        s"""$context
           |GOAL: Describe a turning point or crisis.
           |INSTRUCTIONS:
           |- Identify the critical error or decisive moment.
           |- Explain the shift in momentum.
           |$commonInstructions
         """.stripMargin
         
      case BookModel.SectionType.EndgameLessons =>
        s"""$context
           |GOAL: Describe the endgame phase.
           |INSTRUCTIONS:
           |- Focus on technique and conversion/defense.
           |- Mention key theoretical concepts.
           |$commonInstructions
         """.stripMargin
         
      case BookModel.SectionType.MiddlegamePlans =>
        s"""$context
           |GOAL: Describe the strategic plans and maneuvering (moves $startPly-$endPly).
           |INSTRUCTIONS:
           |- Identify the dominant plan (e.g. Queen's side attack, Center control).
           |- Explain how the plan unfolded.
           |$commonInstructions
         """.stripMargin
         
      case _ => // Mixed / Structural
        s"""$context
           |GOAL: Describe the flow of this game segment (moves $startPly-$endPly).
           |INSTRUCTIONS:
           |- This segment may contain a mix of tactical and strategic moves.
           |- If there are tactical moments (see Key Moments), highlight them prominently.
           |- If it's mostly maneuvering, focus on the plans.
           |- Explain how the advantage shifted (or didn't) during this phase.
           |$commonInstructions
         """.stripMargin
