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
    case CriticalMoment   // Decisive swing

  /** Detect narrative arc from delta */
  def detectArc(deltaWinPct: Double): Arc =
    if deltaWinPct <= -10 then Arc.Collapse
    else if deltaWinPct <= -3 then Arc.CriticalMoment
    else if deltaWinPct >= 10 then Arc.Turnaround
    else if deltaWinPct >= 3 then Arc.RisingTension
    else Arc.SteadyGrind

  /** Select top 2-3 most important tags for narrative */
  def selectKeyTags(tags: List[String]): List[String] =
    val priorityTags = List(
      "king_safety_crisis",
      "fortress_defense",
      "conversion_difficulty",
      "tactical_complexity",
      "locked_position",
      "strong_knight",
      "bishop_pair_advantage",
      "restricted_bishop",
      "weak_f7",
      "weak_f2"
    )
    val priority = tags.filter(priorityTags.contains)
    val other = tags.filterNot(priorityTags.contains)
    (priority ++ other).take(3)

  /** Generate narrative template based on tag combinations */
  def narrativeTemplate(tags: List[String], arc: Arc): String =
    val keyTags = selectKeyTags(tags)
    
    // Tag-based narrative fragments
    val fragments = keyTags.flatMap {
      case "king_safety_crisis" => Some("a direct attack against the king becomes realistic")
      case "fortress_defense" => Some("the defender is close to building a fortress despite material deficit")
      case "locked_position" => Some("the locked pawn structure leaves both sides with limited pawn breaks and long-term manoeuvring")
      case "conversion_difficulty" => Some("converting the edge will require patience and clean technique")
      case "conversion_difficulty_endgame" => Some("the technical endgame demands precision")
      case "tactical_complexity" => Some("tactical complications blur the evaluation")
      case "strong_knight" => Some("a powerfully placed knight dominates the position")
      case "restricted_bishop" => Some("a bishop is restricted by its own pawn chain")
      case "bishop_pair_advantage" => Some("the bishop pair exerts long-range pressure")
      case "pawn_storm" => Some("an aggressive pawn storm advances with tempo")
      case "active_rooks" => Some("active rooks infiltrate along open files")
      case "opposite_color_bishops" => Some("opposite-colored bishops tilt the game toward drawing motifs")
      case "weak_f7" | "weak_f2" => Some("the home-square weakness on f7/f2 remains a long-term target")
      case _ => None
    }

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
      anchorPly: Int,
      tags: List[String],
      phase: String,
      arc: Arc,
      studyScore: Double
  ): StudyChapterMetadata =
    val keyTags = selectKeyTags(tags)
    
    // 1. Generate Title
    val title = (keyTags.headOption, arc) match
      case (Some("king_safety_crisis"), _) => "King in Danger"
      case (Some("tactical_complexity"), _) => "Tactical Storm"
      case (Some("conversion_difficulty"), _) => "Technical Task"
      case (Some("fortress_defense"), _) => "Fortress Attempt"
      case (Some("strong_knight"), _) => "The Octopus Knight"
      case (Some("bad_bishop"), _) => "Bad Bishop Woes"
      case (_, Arc.Collapse) => "The Turning Point"
      case (_, Arc.CriticalMoment) => "Critical Decision"
      case (_, Arc.Turnaround) => "The Comeback"
      case (_, Arc.RisingTension) => "Building Pressure"
      case _ => if phase == "endgame" then "Endgame Grind" else "Strategic Battle"

    // 2. Generate Description
    // Use the existing narrative template logic but make it more "book-like"
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
    val arc = detectArc(deltaWinPct)
    val keyTags = selectKeyTags(tags)
    val template = narrativeTemplate(tags, arc)
    
    val phaseLabel = phase match
      case "opening" => "Opening Phase"
      case "middlegame" => "Middlegame"
      case "endgame" => "Endgame"
      case _ => "Game Phase"

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

    val bestContext = best.map(b => s"Best was $b.").getOrElse("")
    val practicalContext = practicalMove.map(m => s"Practical alternative: $m (easier to play).").getOrElse("")

    val opponentContext = opponentRobustness match
      case Some(or) if or < 0.3 =>
        val estimatedMoves = if or < 0.15 then "only 1-2 good defensive moves" else "a narrow defensive window"
        s"""
OPPONENT PERSPECTIVE (Pressure Point):
- This move created a difficult defensive problem for your opponent
- Opponent Robustness: ${fmt(or)} (${estimatedMoves})
- Your opponent must find precise moves to avoid worse outcomes
"""
      case Some(or) =>
        s"""
OPPONENT PERSPECTIVE:
- Opponent Robustness: ${fmt(or)} (multiple good replies available)
"""
      case None => ""

    s"""
CONTEXT:
- Phase: $phaseLabel (Ply $anchorPly)
- Before: $advantageContext (${fmt(winPctBefore)}%)
- After: ${fmt(winPctAfter)}% ($changeContext)
- Played: $played
- Best: ${best.getOrElse("N/A")}
- $practicalContext
- Arc: ${arc.toString}
- Key Themes: ${keyTags.mkString(", ")}
${opponentContext}
NARRATIVE TEMPLATE:
$template

INSTRUCTIONS:
Write 2–3 sentences in the style of a modern instructional chess book:
- First state the evaluation shift clearly (clear edge / slight edge / equal / unpleasant defence).
- Then explain the key plan or idea (intending / aiming to / preparing to).
- Add a concise takeaway naming the central theme (e.g., "Theme/Takeaway: locked structure → manoeuvre slowly").
- Use concrete chess vocabulary; avoid hype or filler.
- Keep paragraphs short and objective.
- If a Practical alternative is listed, mention why it might be a good choice (e.g. "safer", "simpler plan") compared to the engine best.
Optional: you may use a brief "Question: …? Answer: …" to highlight an instructive choice.
${if opponentContext.nonEmpty then "- If this is a Pressure Point, stress why the defence is difficult." else ""}
    """.trim

  private def fmt(d: Double): String = f"$d%.1f"
