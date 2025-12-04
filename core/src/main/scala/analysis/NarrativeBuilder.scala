package chess
package analysis

/** Builds rich narrative context for LLM-based chapter summaries.
  * Transforms semantic tags + concept scores into storytelling prompts.
  */
object NarrativeBuilder:

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
      "bishop_pair_advantage"
    )
    val priority = tags.filter(priorityTags.contains)
    val other = tags.filterNot(priorityTags.contains)
    (priority ++ other).take(3)

  /** Generate narrative template based on tag combinations */
  def narrativeTemplate(tags: List[String], arc: Arc): String =
    val keyTags = selectKeyTags(tags)
    
    // Tag-based narrative fragments
    val fragments = keyTags.flatMap {
      case "king_safety_crisis" => Some("A menacing attack threatens the exposed king")
      case "fortress_defense" => Some("The defender constructs an impregnable fortress")
      case "locked_position" => Some("The locked pawn structure creates strategic deadlock")
      case "conversion_difficulty" => Some("Despite the advantage, converting proves challenging")
      case "conversion_difficulty_endgame" => Some("The technical endgame demands precision")
      case "tactical_complexity" => Some("Tactical complications blur the evaluation")
      case "strong_knight" => Some("A powerfully placed knight dominates the position")
      case "bishop_pair_advantage" => Some("The bishop pair exerts long-range pressure")
      case "pawn_storm" => Some("An aggressive pawn storm advances menacingly")
      case "active_rooks" => Some("Active rooks infiltrate the opponent's position")
      case "opposite_color_bishops" => Some("Opposite-colored bishops create drawing chances")
      case _ => None
    }

    // Arc-based framing
    val arcFraming = arc match
      case Arc.RisingTension => "The pressure mounts as"
      case Arc.Collapse => "The advantage evaporates when"
      case Arc.Turnaround => "A dramatic turnaround occurs as"
      case Arc.SteadyGrind => "The position subtly shifts as"
      case Arc.CriticalMoment => "A critical juncture arrives when"

    if fragments.nonEmpty then
      s"$arcFraming ${fragments.mkString(", ")}."
    else
      s"$arcFraming the position's character changes."

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
      opponentRobustness: Option[Double] = None
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
- Arc: ${arc.toString}
- Key Themes: ${keyTags.mkString(", ")}
${opponentContext}
NARRATIVE TEMPLATE:
$template

INSTRUCTIONS:
Write a 2-3 sentence narrative as if you're writing a chess book chapter.
Focus on:
1. WHY this moment matters (strategic/tactical turning point)
2. WHAT element changed (using the key themes above)
3. HOW it connects to the overall game story (use the arc context)
${if opponentContext.nonEmpty then "4. If this is a Pressure Point, emphasize how it challenges your opponent" else ""}

Tone: Educational, insightful, specific (not generic).
    """.trim

  private def fmt(d: Double): String = f"$d%.1f"
