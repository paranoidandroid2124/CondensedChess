package lila.commentary.analysis

import lila.commentary.model.{ CandidateTag, Fact, HypothesisAxis, HypothesisHorizon, NarrativeContext }
import lila.commentary.analysis.semantic.RelationObservationCatalog

/**
 * Infinite Diversity Lexicon
 * 
 * Central repository for narrative templates.
 * Uses deterministic hashing (bead) to ensure consistent output for the same position,
 * but high variety across different positions.
 */
object NarrativeLexicon {

  enum Style:
    case Book, Coach, Dramatic
  // 1. OPENING / CONTEXT SETTERS

  // Deterministic random selection based on mixed seed
  def pick(bead: Int, options: Seq[String]): String = {
    if (options.isEmpty) ""
    else options(Math.abs(bead) % options.size)
  }
  
  // Mixed seed helper
  def mixSeed(seeds: Int*): Int = seeds.foldLeft(0)(_ ^ _.hashCode)

  def gameIntro(white: String, black: String, event: String, date: String, result: String, totalPlies: Int, keyMomentsCount: Int): String = {
    def cleanMeta(value: String): Option[String] =
      Option(value)
        .map(_.trim)
        .filter(v =>
          v.nonEmpty &&
            !v.equalsIgnoreCase("unknown") &&
            v != "*" &&
            UserFacingSignalSanitizer.placeholderHits(v).isEmpty
        )

    def moveCount(totalPlies: Int): Int = math.max(1, (totalPlies + 1) / 2)

    def moveLabel(totalPlies: Int): String = {
      val moves = moveCount(totalPlies)
      if (moves == 1) "1 move" else s"$moves moves"
    }

    val whiteName = cleanMeta(white)
    val blackName = cleanMeta(black)
    val eventName = cleanMeta(event)
    val dateLabel = cleanMeta(date)
    val resultLabel = cleanMeta(result)

    val source = (whiteName, blackName) match
      case (Some(w), Some(b)) =>
        val suffix = List(eventName, dateLabel).flatten.mkString(", ")
        if suffix.nonEmpty then s"$w vs $b ($suffix)" else s"$w vs $b"
      case _ =>
        List(eventName, dateLabel).flatten.mkString(", ") match
          case s if s.nonEmpty => s"this game ($s)"
          case _               => "this game"

    val status =
      resultLabel match
        case Some("1-0") =>
          s"${whiteName.getOrElse("White")} won after ${moveLabel(totalPlies)}."
        case Some("0-1") =>
          s"${blackName.getOrElse("Black")} won after ${moveLabel(totalPlies)}."
        case Some("1/2-1/2") =>
          s"The game was drawn after ${moveLabel(totalPlies)}."
        case Some(other) =>
          s"The game finished $other after ${moveLabel(totalPlies)}."
        case None if totalPlies <= 20 =>
          s"The line is still in the opening after ${moveLabel(totalPlies)}."
        case None =>
          s"The position remains unresolved after ${moveLabel(totalPlies)}."

    val focus =
      keyMomentsCount match
        case n if n <= 0 => "The review starts from the strategic choices rather than a single decisive swing."
        case 1           => "The review focuses on one highlighted moment."
        case n           => s"The review focuses on $n highlighted moments."

    s"This review covers $source. $status $focus"
  }

  def gameConclusion(
      winner: Option[String],
      themes: List[String],
      blunders: Int,
      missedWins: Int,
      mainContest: Option[String] = None,
      decisiveShift: Option[String] = None,
      payoff: Option[String] = None
  ): String = {
    val leadTheme = themes.headOption
    val extraThemes = themes.drop(1).take(2)
    val hasContestLead = mainContest.exists(_.trim.nonEmpty)

    def themeTail: String =
      if extraThemes.nonEmpty then s" Other recurring themes were ${extraThemes.mkString(", ")}."
      else ""

    def practicalTail: String =
      if winner.isEmpty then ""
      else if payoff.exists(_.trim.nonEmpty) then ""
      else if blunders > 0 && missedWins > 0 then
        s" The practical story included $blunders blunder${if blunders == 1 then "" else "s"} and $missedWins missed win${if missedWins == 1 then "" else "s"}."
      else if blunders > 0 then
        s" The decisive practical swings came from $blunders blunder${if blunders == 1 then "" else "s"}."
      else if missedWins > 0 then
        s" The game also featured $missedWins missed win${if missedWins == 1 then "" else "s"}."
      else ""

    val detailSentences =
      List(mainContest, decisiveShift, payoff).flatten
        .map(_.trim)
        .filter(_.nonEmpty)
        .foldLeft(List.empty[String]) { case (acc, raw) =>
          val sentence =
            if raw.endsWith(".") || raw.endsWith("!") || raw.endsWith("?") then raw
            else s"$raw."
          val fingerprint =
            sentence
              .replace("**", "")
              .replaceAll("""[^\p{L}\p{N}\s]""", " ")
              .replaceAll("""\s+""", " ")
              .trim
              .toLowerCase
          if fingerprint.nonEmpty && acc.exists { existing =>
              val existingFingerprint =
                existing
                  .replace("**", "")
                  .replaceAll("""[^\p{L}\p{N}\s]""", " ")
                  .replaceAll("""\s+""", " ")
                  .trim
                  .toLowerCase
              existingFingerprint == fingerprint
            }
          then acc
          else acc :+ sentence
        }

    val core =
      winner match {
        case Some(w) if detailSentences.nonEmpty =>
          if hasContestLead then s"**$w** came out ahead once the game's main ideas became concrete."
          else
            leadTheme match
              case Some(theme) => s"**$w** came out ahead once the game's main ideas became concrete around $theme."
              case None        => s"**$w** came out ahead once the game's main ideas became concrete."
        case Some(w) if blunders > 0 =>
          leadTheme match
            case Some(theme) => s"**$w** handled the decisive mistakes better, with $theme as the main strategic thread."
            case None        => s"**$w** handled the decisive mistakes better and converted the result from there."
        case Some(w) if missedWins > 0 =>
          leadTheme match
            case Some(theme) => s"**$w** made the cleaner use of the missed chances, especially once $theme took over."
            case None        => s"**$w** made the cleaner use of the missed chances and converted the game."
        case Some(w) =>
          leadTheme match
            case Some(theme) => s"**$w** kept the game on the right strategic track through $theme."
            case None        => s"**$w** kept control of the key turning points and converted cleanly."
        case None if blunders > 0 || missedWins > 0 =>
          if hasContestLead then "The game never fully settled because both sides left practical chances on the board."
          else
            leadTheme match
              case Some(theme) => s"The game never fully settled because both sides left practical chances around $theme."
              case None        => "The game never fully settled because both sides left practical chances on the board."
        case None =>
          if hasContestLead then "The game stayed balanced while the main plans stayed in tension."
          else
            leadTheme match
              case Some(theme) => s"The game stayed balanced around $theme."
              case None        => "The game stayed balanced."
      }

    val detailTail =
      if detailSentences.nonEmpty then s" ${detailSentences.mkString(" ")}"
      else ""

    s"$core$detailTail$practicalTail$themeTail"
  }

  def momentBlockLead(bead: Int, phase: String, momentType: String, ply: Int): String = {
    val p = Option(phase).getOrElse("").trim.toLowerCase
    val m = Option(momentType).getOrElse("").trim.toLowerCase
    val moveRef = s"move ${math.max(1, (ply + 1) / 2)}"
    val phaseLabel =
      p match {
        case s if s.contains("opening")    => "opening block"
        case s if s.contains("endgame")    => "endgame block"
        case s if s.contains("middlegame") => "middlegame block"
        case _                             => "critical game block"
      }

    val templates =
      if (m.contains("opening")) List(
        s"In the $phaseLabel around $moveRef, opening choices start to define the long-term plans.",
        s"This $phaseLabel near $moveRef marks where theory gives way to independent decisions."
      )
      else if (m.contains("mate")) List(
        s"Around $moveRef, this $phaseLabel turns into a forcing tactical sequence.",
        s"The $phaseLabel near $moveRef becomes concrete: mating motifs now drive the evaluation."
      )
      else if (m.contains("blunder") || m.contains("missedwin") || m.contains("swing")) List(
        s"The $phaseLabel around $moveRef is a genuine turning point in the game.",
        s"Around $moveRef, this $phaseLabel changes the practical control balance."
      )
      else if (m.contains("pressure") || m.contains("tension")) List(
        s"This $phaseLabel near $moveRef is defined by cumulative pressure and move-order accuracy.",
        s"Around $moveRef, the $phaseLabel remains balanced but highly tension-sensitive."
      )
      else List(
        s"In this $phaseLabel around $moveRef, the game's key practical choices become clear.",
        s"Around $moveRef, this $phaseLabel frames the next strategic decisions."
      )

    pick(bead ^ 0x5f356495, templates)
  }

  def hybridBridge(
    bead: Int,
    phase: String,
    primaryPlan: Option[String],
    tacticalPressure: Boolean,
    cpWhite: Option[Int],
    ply: Int = 0
  ): String = {
    val phaseText = Option(phase).getOrElse("").trim.toLowerCase
    val planText = primaryPlan.map(_.replaceAll("""[_\-]+""", " ").trim).filter(_.nonEmpty)
    val evalHint = cpWhite.map(cp => evalOutcomeClauseFromCp(bead ^ 0x7f4a7c15, cp, ply = ply))

    val strategicTemplates = List(
      s"Strategically, this phase rewards a coherent plan${planText.map(p => s" around $p").getOrElse("")}.",
      s"The strategic task is to coordinate pieces and structure${planText.map(p => s" so that $p can be executed").getOrElse("")}."
    )
    val tacticalTemplates = List(
      s"At the same time, tactical accuracy is mandatory because forcing resources are close to the surface.",
      s"Concrete calculation now matters as much as planning, since one tempo can change the balance."
    )
    val phaseHint =
      if (phaseText.contains("endgame")) Some("Technical precision in the ending is now a major practical factor.")
      else None

    val base = if (tacticalPressure) pick(bead ^ 0x6d2b79f5, tacticalTemplates) else pick(bead ^ 0x517cc1b7, strategicTemplates)
    val withPhase = phaseHint.map(h => s"$base $h").getOrElse(base)
    evalHint.map(e => s"$withPhase $e.").getOrElse(withPhase)
  }

  def intent(bead: Int, move: String, plan: String, style: Style = Style.Book): String = {
    val templates = style match {
      case Style.Dramatic => Seq(
        s"$move! A bold decision to $plan.",
        s"$move signals an aggressive intent: $plan.",
        s"With $move, the game enters a sharp phase centered on $plan."
      )
      case Style.Coach => Seq(
        s"Note how $move immediately prepares $plan.",
        s"$move is instructive—it directly supports $plan.",
        s"A key move. $move enables the plan: $plan."
      )
      case _ => Seq( // "Shankland" Standard: Direct & Explain 'Why'
        s"$move facilitates $plan.",
        s"The purpose of $move is $plan.",
        s"$move is designed to $plan.",
        s"By playing $move, the plan to $plan is set in motion."
      )
    }
    pick(bead, templates)
  }

  def intro(bead: Int, nature: String, tension: Double, style: Style = Style.Book): String = {
    val tensionHint = if tension >= 0.7 then "high-tension" else "complex"
    val templates = style match {
      case Style.Coach => Seq(
        s"Pause here. The position is $nature, with high tension. How would you proceed?",
        s"This $nature structure demands precision.",
        s"In such $nature positions, every tempo counts.",
        s"Understanding this $nature setup is critical for improvement."
      )
      case Style.Dramatic => Seq(
        s"The board is ablaze! A chaotic $nature battle.",
        s"Tension spikes in this $nature thriller.",
        s"A clash of wills in a sharp $nature landscape.",
        s"No room for slow play in this high-octane $nature struggle."
      )
      case _ => Seq( // "Shankland" Standard: Contextual & Professional
        s"The position has taken on a $nature character.",
        s"We have reached a complex $nature middlegame.",
        s"The struggle is defined by its $tensionHint $nature nature.",
        s"Strategic complexity increases in this $nature position."
      )
    }
    pick(bead, templates)
  }

  def fallbackNature(bead: Int, nature: String, tension: Double): String = {
    val templates = Seq(
      s"A quiet $nature position. The tension (${"%.1f".format(tension)}) suggests careful maneuvering is required.",
      s"Without sharp tactics, the game revolves around this $nature structure.",
      s"The position simmers with potential, defined by its $nature character.",
      s"Steady play is needed in this $nature phase.",
      s"The $nature landscape requires positional understanding over quick strikes."
    )
    pick(bead, templates)
  }

  def rhetoricalQuestion(bead: Int, plan: String, style: Style = Style.Book): String = {
     val questions = style match {
       case Style.Coach => Seq(
         s"How should we implement the plan: '$plan'?",
         s"What is the best way to achieve $plan?",
         s"Can you find the move that best supports $plan?",
         s"Why is $plan critical here?"
       )
       case Style.Dramatic => Seq(
         s"Will the bold plan of $plan succeed?",
         s"Dare we attempt $plan in such a sharp position?",
         s"Can the opponent stop the onslaught of $plan?",
         s"Is this the moment for $plan?"
       )
       case _ => Seq(
         s"The question is how to further $plan.",
         s"We must consider: is $plan feasible?",
         s"How does the position support $plan?",
         s"Does the board state justify $plan?"
       )
     }
     pick(bead, questions)
  }

  def tacticsQuestion(bead: Int, motifName: String, style: Style = Style.Book): String = {
    val questions = style match {
      case Style.Coach => Seq(
        s"Can you spot how $motifName is exploited?",
        s"Look for a tactical shot involving $motifName.",
        s"There is a $motifName pattern here. Do you see it?",
        s"Test your calculation: find the $motifName."
      )
      case Style.Dramatic => Seq(
        s"A sudden $motifName changes everything!",
        s"Can the opponent survive this $motifName?",
        s"Watch out! A deadly $motifName appears!",
        s"The $motifName strikes like lightning!"
      )
      case _ => Seq(
        s"Tactically, the theme is $motifName.",
        s"The position features a clear $motifName.",
        s"Calculation reveals a $motifName opportunity."
      )
    }
    pick(bead, questions)
  }

  def refutation(bead: Int, move: String, reply: String, outcome: String, style: Style = Style.Book): String = {
    val templates = style match {
      case Style.Dramatic => Seq(
        s"$move looks tempting, but $reply shuts it down completely ($outcome).",
        s"Disaster awaits after $move due to the crushing $reply ($outcome).",
        s"A single tempo loss with $move allows $reply, ending the resistance ($outcome)."
      )
      case _ => Seq( // "Shankland" Standard: Analytical & Educational
        s"$move fails due to the precise response $reply ($outcome).",
        s"However, $move is met by $reply, which $outcome.",
        s"The flaw in $move is revealed by $reply ($outcome).",
        s"Against $move, Black has the strong reply $reply ($outcome)."
      )
    }
    pick(bead, templates)
  }
  def getOpening(bead: Int, phase: String, evalText: String, tactical: Boolean = false, ply: Int = 0): String = {
    val p = phase.toLowerCase
    val localSeed = bead ^ (ply * 0x45d9f3b)

    val (leadPool, anglePool) = p match {
      case "opening" if tactical =>
        (
          List(
            "Opening choices are already forcing concrete decisions",
            "The opening has turned tactical quickly",
            "Development now intersects with concrete calculation",
            "The opening is already testing tactical accuracy",
            "Early tactical motifs are now dictating move-order",
            "The opening phase now demands concrete tactical precision",
            "Even development moves now carry immediate tactical weight",
            "The opening has become a calculation-heavy phase"
          ),
          List(
            "both kings still need careful handling",
            "piece placement and tactics are now intertwined",
            "one tempo can trigger forcing play",
            "there is little room for slow setup moves",
            "every developing move now has tactical consequences",
            "initiative can swing on one inaccurate developing move",
            "move-order slips can immediately invite tactical punishment",
            "both sides must calculate before committing to development",
            "one inaccurate developing move can concede practical control"
          )
        )
      case "opening" =>
        (
          List(
            "The opening phase is still fluid",
            "Opening development is still in progress",
            "The game remains in opening channels",
            "This is still an opening structure",
            "Early plans are still being defined",
            "Opening priorities are still being negotiated move by move",
            "Development goals are clear, but the structure is still flexible",
            "The position remains in a developmental opening stage"
          ),
          List(
            "piece coordination remains central",
            "central tension is still unresolved",
            "move-order precision matters more than forcing lines",
            "both sides are balancing development and structure",
            "the middlegame plans are only now taking shape",
            "central commitments are still being timed with care",
            "small developmental choices can still reshape the structure",
            "neither side wants to clarify the center too early"
          )
        )
      case "middlegame" =>
        (
          List(
            "The middlegame has fully started",
            "Plans and tactics now bite at every move",
            "This is a concrete middlegame fight",
            "Middlegame complexity is now front and center",
            "The game has moved into a tactical-strategic mix",
            "Middlegame priorities now hinge on concrete calculation",
            "The position now demands active middlegame decision-making",
            "This middlegame phase rewards concrete, well-timed choices",
            "The contest has reached a mature middlegame stage",
            "Functional middlegame coordination is the immediate challenge",
            "Strategic depth increases as the middlegame takes hold"
          ),
          List(
            "piece coordination and king safety both matter",
            "move-order nuance can shift practical control",
            "small structural concessions become long-term targets",
            "initiative and defensive resources are closely balanced",
            "automatic play is dangerous in this position",
            "routine moves can quickly backfire here",
            "small inaccuracies can hand over momentum immediately",
            "both sides must balance activity with king safety at each step",
            "practical control can change after a single move-order slip",
            "slight positional shifts can tip the overall balance",
            "precision in the center governs the upcoming phase",
            "maintaining tension requires high tactical vigilance"
          )
        )
      case "endgame" =>
        (
          List(
            "The position has simplified into an endgame",
            "The game is now in an endgame phase",
            "The remaining material makes local details more visible",
            "We are firmly in endgame territory",
            "The board has reached a late-game structure",
            "The position has moved into a simplified phase",
            "Endgame details now need concrete checking",
            "The remaining pieces and pawns define the immediate context"
          ),
          List(
            "king and pawn placement matter more than before",
            "piece activity still needs to be checked on the board",
            "pawn-structure details carry extra weight",
            "move order matters in the simplified position",
            "one tempo can change the sampled line",
            "minor king-route details need board-level support",
            "the evaluation still needs concrete line support",
            "broad strategic plans give way to current-board details"
          )
        )
      case _ =>
        (
          List(
            "The position requires careful handling",
            "This is a critical decision point",
            "Accuracy is heavily rewarded here",
            "Both sides must stay precise"
          ),
          List(
            "the next move can change the evaluation",
            "coordination and calculation are both under pressure",
            "the position is less stable than it first appears",
            "a small imbalance can have outsized consequences"
          )
        )
    }

    val lead = sentenceCase(pickWithPlyRotation(localSeed ^ 0x517cc1b7, ply, leadPool))
    val angle = sentenceCase(pickWithPlyRotation(localSeed ^ 0x6d2b79f5, ply + 1, anglePool))
    s"${punctuate(lead)} ${punctuate(angle)} $evalText."
  }

  def getGoalStatusDescription(bead: Int, evaluation: OpeningGoals.Evaluation): String = {
    evaluation.status match {
      case OpeningGoals.Status.Achieved =>
        pick(bead, List(
          "The current position supports a bounded opening-context cue.",
          "The move fits the opening context, but only at this local level.",
          "The position carries a bounded opening cue."
        ))
      
      case OpeningGoals.Status.Partial =>
        pick(bead, List(
          "The opening cue is visible only as partial context here.",
          "The current position gives partial opening context rather than a full plan."
        ))

      case OpeningGoals.Status.Premature =>
        pick(bead, List(
          "The opening cue remains only a candidate in this position.",
          "This is still background opening context until the current position supports it."
        ))

      case OpeningGoals.Status.Failed =>
        pick(bead, List(
          "The current position does not support that opening cue.",
          "The opening cue stays background context here."
        ))
        
      case OpeningGoals.Status.Mismatch => 
        evaluation.requiredFamily.map(family =>
          pick(bead, List(
            s"This opening cue needs the ${family.structureLabel} structure here.",
            s"The current position is not the ${family.structureLabel} structure for that opening cue."
          ))
        ).getOrElse("")
    }
  }

  def getIntent(bead: Int, alignment: String, evidence: Option[String], ply: Int = 0, continuity: Option[lila.commentary.model.strategic.PlanContinuity] = None): String = {
    val ev = evidence.getOrElse("")
    val hasEv = ev.nonEmpty
    val localSeed = bead ^ (ply * 0x4eb2d)
    def choose(options: List[String]): String =
      pickWithPlyRotation(localSeed ^ Math.abs(alignment.hashCode), ply, options)

    def normalizePlanKey(s: String): String =
      s.toLowerCase.replaceAll("[^a-z0-9]", "")
    val alignmentKey = normalizePlanKey(alignment)
    val isContinuing = continuity.exists { c =>
      c.consecutivePlies > 1 && (
        c.planId.exists(id => normalizePlanKey(id) == alignmentKey) ||
          normalizePlanKey(c.planName) == alignmentKey
      )
    }
    val pliesText = continuity.map(c => s" for ${c.consecutivePlies} plies").getOrElse("")

    if (isContinuing) {
       if (hasEv) return s"persists with the plan by $ev"
       return choose(List(
         s"continues the ongoing plan$pliesText",
         s"stays committed to the plan$pliesText",
         s"persists with the same idea$pliesText",
         s"presses forward with the continued plan"
       ))
    }

    alignment.toLowerCase match {
      // Tactical intents
      case s if s.contains("attack") => 
        if (hasEv) s"continues the attack by $ev"
        else choose(List(
          "continues the attack",
          "seizes the initiative",
          "keeps the pressure on",
          "poses serious practical questions",
          "maintains active momentum"
        ))
      case s if s.contains("defense") || s.contains("prophylactic") => 
        if (hasEv) s"solidifies the position by $ev"
        else choose(List(
          "prevents counterplay",
          "solidifies the position",
          "stops the enemy ideas",
          "provides necessary defense",
          "keeps defensive resources coordinated"
        ))
      case s if s.contains("tactical") => 
        if (hasEv) s"creates tactical problems by $ev"
        else choose(List(
          "creates tactical threats",
          "complicates the game",
          "introduces tactical possibilities",
          "forces concrete calculation"
        ))
      case s if s.contains("pressure") =>
        if (hasEv) s"applies pressure by $ev"
        else choose(List(
          "applies positional pressure",
          "maintains the tension",
          "keeps the opponent under pressure",
          "forces uncomfortable defensive choices"
        ))
      case s if s.contains("pawn break") =>
        if (hasEv) s"opens the position by $ev"
        else choose(List(
          "opens the position",
          "breaks through the center",
          "challenges the pawn structure",
          "tests the pawn structure directly"
        ))
      case s if s.contains("rook activation") =>
        if (hasEv) s"activates the rook by $ev"
        else choose(List(
          "activates the rook",
          "brings the rook into play",
          "places the rook on an active file",
          "improves rook mobility and scope"
        ))
      case s if s.contains("king activation") =>
        if (hasEv) s"brings the king into the game by $ev"
        else choose(List(
          "brings the king into the game",
          "centralizes the king",
          "advances the king toward the action",
          "improves king activity for the ending"
        ))
      case s if s.contains("centralization") =>
        if (hasEv && !ev.toLowerCase.contains("centraliz")) s"occupies a strong central square by $ev"
        else if (hasEv) s"improves the position by $ev"
        else choose(List(
          "centralizes the piece",
          "occupies a strong square",
          "improves piece placement",
          "improves central coordination"
        ))
      case s if s.contains("outpost") =>
        if (hasEv && !ev.toLowerCase.contains("outpost")) s"keeps an outpost cue tied to $ev"
        else if (hasEv) s"keeps the candidate-line outpost cue in view through $ev"
        else choose(List(
          "keeps an outpost cue in view",
          "points toward an outpost idea that still needs board support",
          "keeps outpost access as a theme to verify"
        ))
      case s if s.contains("simplification") =>
        if (hasEv) s"keeps the simplification cue tied to $ev"
        else choose(List(
          "keeps simplification as a line to check",
          "uses exchanges as the detail to verify",
          "points toward a simplified structure that still needs line checking",
          "reduces the question to exchange order"
        ))
      case s if s.contains("file control") =>
        if (hasEv) s"keeps the file-control cue tied to $ev"
        else choose(List(
          "keeps file control as the feature to check",
          "points toward open-file play",
          "uses the file as the positional reference",
          "keeps major-piece file activity in view"
        ))
      case s if s.contains("passed pawn") =>
        if (hasEv) s"keeps the passed-pawn cue tied to $ev"
        else choose(List(
          "keeps the passed-pawn cue in view",
          "plays around the passer",
          "points to passed-pawn handling",
          "keeps the passer as the line detail to verify"
        ))
      case s if s.contains("opposition") =>
        if (hasEv) s"keeps the opposition cue tied to $ev"
        else choose(List(
          "keeps king-distance geometry in view",
          "plays around the king-placement cue",
          "keeps opposition as an endgame cue to verify",
          "uses king geometry as the detail to check"
        ))
      case s if s.contains("zugzwang") =>
        if (hasEv && !ev.toLowerCase.contains("zugzwang")) s"keeps useful-move pressure tied to $ev"
        else choose(List(
          "keeps useful-move pressure as a line to verify",
          "treats move availability as the endgame detail to check",
          "keeps the move-availability cue support-only",
          "points to move-availability pressure without claiming the result"
        ))
      case s if s.contains("pawn run") || s.contains("pawn_race") =>
         if (hasEv) s"keeps the pawn-race cue tied to $ev"
         else choose(List(
           "keeps a pawn-race cue in view",
           "checks the passer's tempo count",
           "keeps pawn-race timing as the detail to verify",
           "points to the pawn advance without claiming promotion"
         ))
      case s if s.contains("shouldering") =>
         if (hasEv) s"keeps the king-placement cue tied to $ev"
         else choose(List(
           "keeps king-placement geometry in view",
           "checks the kings' route geometry",
           "uses king placement as the detail to verify",
           "points to king-route restriction without claiming a lockout"
         ))
      case s if s.contains("castling") =>
        if (hasEv) s"castles by $ev"
        else choose(List(
          "castles to safety",
          "brings the king to safety",
          "completes the king's evacuation",
          "stabilizes king safety before operations"
        ))
      case s if s.contains("fianchetto") =>
        if (hasEv) s"fianchettoes the bishop by $ev"
        else choose(List(
          "fianchettoes the bishop",
          "develops the bishop to the long diagonal",
          "activates the bishop on a long diagonal"
        ))
      case s if s.contains("exchange") =>
        if (hasEv) s"keeps the exchange cue tied to $ev"
        else choose(List(
          "keeps exchanges as the line detail to verify",
          "uses the exchange order as the positional reference",
          "points toward material simplification without claiming the result",
          "keeps the resulting structure as the detail to check"
        ))
      
      // Development & central control
      case s if s.contains("development") => 
        if (hasEv) s"completes development by $ev"
        else choose(List(
          "completes development",
          "brings pieces into play",
          "improves piece activity",
          "connects the rooks",
          "finishes development with good coordination"
        ))
      case s if s.contains("central") => 
        if (hasEv) s"fights for the center by $ev"
        else choose(List(
          "maintains central tension",
          "fights for the center",
          "challenges the center",
          "improves central control"
        ))
      case s if s.contains("maneuvering") =>
        if (hasEv) s"maneuvers the piece by $ev"
        else choose(List(
          "repositions the piece",
          "improves the piece's scope",
          "prepares for the next phase",
          "improves piece routes for later plans"
        ))
      
      // Final fallback
      case _ => 
        if (hasEv) s"improves the position by $ev"
        else s"improves the position"
    }
  }

  def getMainFlow(
    bead: Int,
    move: String,
    annotation: String,
    intent: String,
    replySan: Option[String],
    sampleRest: Option[String],
    evalTerm: String,
    consequence: String = "" // Narrative closer
  ): String = {
    val fullMove = s"**$move**$annotation"
    val rep = replySan.map(s => s"...$s").getOrElse("")
    val sample = sampleRest.getOrElse("")
    val cons = if (consequence.nonEmpty) s" $consequence" else ""

    val templates = (replySan, sampleRest) match {
      case (Some(_), Some(_)) => List(
        s"$fullMove $intent; after $rep, play might continue $sample. $evalTerm$cons.",
        s"$fullMove $intent, with $rep as the sampled reply and $sample as the checked continuation. $evalTerm$cons.",
        s"After $fullMove, the sampled line continues $rep $sample. $evalTerm$cons.",
        s"$fullMove $intent; if the line uses $rep, then $sample is the checked continuation. $evalTerm$cons.",
        s"$fullMove $intent, and the sampled continuation after $rep is $sample. $evalTerm$cons."
      )
      case (Some(_), None) => List(
        s"$fullMove $intent; the sampled reply is $rep. $evalTerm$cons.",
        s"After $fullMove, $rep is the checked reply in the sample. $evalTerm$cons.",
        s"$fullMove $intent, with $rep as the sampled reply. $evalTerm$cons."
      )
      case _ => List(
        s"$fullMove $intent. $evalTerm$cons.",
        s"With $fullMove, the line $intent. $evalTerm$cons.",
        s"$fullMove $intent. It remains the sampled reference move. $evalTerm$cons."
      )
    }
    pick(bead, templates)
  }

  /**
   * Expert-level analytical flavour for practical aspects.
   */
  def getAnalyticalFlavour(bead: Int, verdict: String): String = {
    val options = verdict match {
      case "Comfortable" => List(
        "White can improve steadily without forcing.",
        "Black still has a few practical problems to solve.",
        "White can keep pressing without taking undue risks.",
        "The position is pleasant to handle for White."
      )
      case "Under Pressure" => List(
        "Black must defend accurately to avoid drifting into a worse position.",
        "The defensive task is uncomfortable in practice.",
        "Counterplay is hard to generate here.",
        "One careless move can quickly change the evaluation."
      )
      case "Balanced" => List(
        "The position is balanced but full of choices.",
        "The balance depends on precise calculation.",
        "Neither side has an easy path to progress.",
        "A level game, but far from simple."
      )
      case _ => Nil
    }
    if (options.isEmpty) "" else pick(bead, options)
  }
  // 6. ALTERNATIVES & CRITIQUE

  private def pick(seed: Int, options: List[String]): String = {
    if (options.isEmpty) ""
    else {
      val mixed = scala.util.hashing.MurmurHash3.mixLast(0x9e3779b9, seed)
      val finalized = scala.util.hashing.MurmurHash3.finalizeHash(mixed, 1)
      options(Math.floorMod(finalized, options.size))
    }
  }

  /**
   * Adjacent-ply repetition suppression:
   * rotate deterministic choices by ply while keeping seed stability.
   */
  def pickWithPlyRotation(seed: Int, ply: Int, options: List[String]): String = {
    if (options.isEmpty) ""
    else {
      val base = {
        val mixed = scala.util.hashing.MurmurHash3.mixLast(0x7f4a7c15, seed)
        val finalized = scala.util.hashing.MurmurHash3.finalizeHash(mixed, 1)
        Math.floorMod(finalized, options.size)
      }
      val rotated = if (options.size <= 1) base else Math.floorMod(base + Math.floorMod(ply, options.size), options.size)
      options(rotated)
    }
  }

  private def punctuate(sentence: String): String =
    val s = sentence.trim
    if (s.isEmpty || s.endsWith(".") || s.endsWith("!") || s.endsWith("?")) s else s"$s."

  private def sentenceCase(sentence: String): String =
    val s = sentence.trim
    if s.isEmpty then s
    else s"${s.head.toUpper}${s.tail}"

  private def normalizeMotifTag(raw: String): String =
    Option(raw).getOrElse("").trim
      .replaceAll("([a-z])([A-Z])", "$1_$2")
      .toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")

  private def motifMatches(normalizedMotif: String, rawNeedle: String): Boolean =
    val motif = normalizeMotifTag(normalizedMotif)
    val needle = normalizeMotifTag(rawNeedle)
    motif.nonEmpty && needle.nonEmpty &&
      (motif.contains(needle) || motif.replace("_", "").contains(needle.replace("_", "")))

  def getOpeningReference(bead: Int, name: String, games: Int, whitePct: Double): String = {
    val statsNote = if (games >= 100) s" ($games games, White scores ${(whitePct * 100).toInt}%)" else ""
    pick(bead, List(
      s"This is a well-known position from the $name$statsNote.",
      s"We are now in the $name$statsNote.",
      s"The opening has transposed into the $name$statsNote."
    ))
  }

  def getPrecedentLead(bead: Int, factualLine: String, anchorMove: Option[String] = None): String = {
    val factual = factualLine.trim
    val templates =
      anchorMove.map(_.trim).filter(_.nonEmpty) match
        case Some(anchor) =>
          List(
            s"In the $anchor branch, a reference game shows: $factual",
            s"A model game in the $anchor line runs: $factual",
            s"Historical guidance around $anchor is clear: $factual",
            s"$factual This line is anchored by $anchor."
          )
        case None =>
          List(
            s"A related precedent: $factual",
            s"A comparable game in this structure showed: $factual",
            s"One historical model game reads: $factual",
            s"$factual This precedent remains the closest structural parallel."
          )
    pick(bead ^ 0x2f6e2b1, templates)
  }

  def getPrecedentMechanismLine(
    bead: Int,
    triggerMove: String,
    replyMove: Option[String],
    pivotMove: Option[String],
    mechanism: String
  ): String = {
    val route = replyMove.map(r => s"$triggerMove, $r").getOrElse(triggerMove)
    val pivot = pivotMove.map(p => s", then $p").getOrElse("")
    val key = Option(mechanism).getOrElse("").toLowerCase

    val templates =
      if key.contains("promotion") then List(
        s"$route$pivot marked the promotion turning point, as promotion motifs began to drive every continuation.",
        s"From $route$pivot, the turning point was the promotion race and its forcing tempo count.",
        s"$route$pivot created the key turning point, after which promotion threats dictated priorities.",
        s"$route$pivot marked the turning point because promotion timing outweighed slower strategic plans.",
        s"With $route$pivot, the transformation toward a promotion-based engine began.",
        s"The tactical fork at $route$pivot shifted the route toward promotion themes."
      )
      else if key.contains("exchange") then List(
        s"$route$pivot marked the exchange turning point, with exchange timing starting to define the evaluation.",
        s"From $route$pivot, the main shift was the exchange sequence and resulting simplification.",
        s"$route$pivot became the turning point once exchanges reshaped piece activity and defensive resources.",
        s"The critical turning point was $route$pivot, where exchange decisions shaped the resulting balance.",
        s"Structural clarity was reached at $route$pivot through a series of forcing exchanges.",
        s"The game transformed at $route$pivot as the exchange sequence reduced tactical noise."
      )
      else if key.contains("tactical") then List(
        s"$route$pivot marked the tactical turning point, once forcing tactical pressure became hard to defuse.",
        s"From $route$pivot, the main shift was king safety and concrete tactical accuracy.",
        s"$route$pivot marked the turning point because tactical threats began to override long-term plans.",
        s"The critical turning point was $route$pivot, where tactical forcing lines dictated move order.",
        s"Calculation intensity at $route$pivot defined the turning point of the struggle.",
        s"The tactical landscape shifted at $route$pivot through a forcing sequence."
      )
      else if key.contains("structural") then List(
        s"$route$pivot marked the reference route's structural shift.",
        s"From $route$pivot, the sample route changed through pawn-structure details.",
        s"$route$pivot is the structural comparison point in the sampled route.",
        s"The reference route turns on structural details around $route$pivot.",
        s"The sample game puts structural context on $route$pivot.",
        s"The comparison stays structural at $route$pivot, with current-line evidence still deciding the plan."
      )
      else List(
        s"$route$pivot marked the initiative turning point, as initiative control shifted to one side.",
        s"From $route$pivot, the main shift was initiative management rather than static factors.",
        s"$route$pivot marked the turning point because tempo and initiative started to shape the play.",
        s"The critical turning point was $route$pivot, where initiative swings changed the practical balance."
      )

    pick(bead ^ 0x5f356495, templates)
  }

  def getPrecedentRouteLine(
    bead: Int,
    triggerMove: String,
    replyMove: Option[String],
    pivotMove: Option[String]
  ): String = {
    val route = List(Some(triggerMove), replyMove, pivotMove).flatten.map(_.trim).filter(_.nonEmpty)
    val routeText =
      route match
        case a :: b :: c :: Nil => s"$a -> $b -> $c"
        case a :: b :: Nil      => s"$a -> $b"
        case a :: Nil           => a
        case _                  => triggerMove
    val templates = List(
      s"Line route: $routeText.",
      s"The branch follows $routeText.",
      s"The move path here is $routeText.",
      s"The practical route is $routeText."
    )
    pick(bead ^ 0x11f17f1d, templates)
  }

  def getPrecedentStrategicTransitionLine(
    bead: Int,
    mechanism: String
  ): String = {
    val m = mechanism.trim
    val templates = List(
      s"The precedent sample tags this route with $m.",
      s"In the reference route, $m is the comparison handle.",
      s"That branch is mainly useful as $m context.",
      s"Treat $m as opening-context support, not a current-line verdict."
    )
    pick(bead ^ 0x284f2d5b, templates)
  }

  def getPrecedentDecisionDriverLine(
    bead: Int,
    mechanism: String
  ): String = {
    val m = mechanism.trim
    val templates = List(
      s"The precedent comparison records $m as route context.",
      s"The route sample highlights $m as a reference motif.",
      s"Use $m as a comparison label while current-line evidence decides relevance.",
      s"The sampled routes keep $m in view without proving it for the current position."
    )
    pick(bead ^ 0x3124bcf5, templates)
  }

  def getAlternative(bead: Int, move: String, whyNot: Option[String]): String = {
    whyNot match {
      case Some(reason) if reason.nonEmpty =>
        pick(bead, List(
          s"**$move** is another candidate, but $reason, so practical timing becomes critical.",
          s"A practical sideline is **$move**; however, $reason, which changes the structural route.",
          s"**$move** keeps options open, yet $reason, and coordination can drift if move order slips.",
          s"**$move** is playable over the board, although $reason, so initiative management becomes harder."
        ))
      case _ =>
        pick(bead, List(
          s"**$move** remains a live candidate, and it steers the game toward a different strategic route.",
          s"One practical detour is **$move**, where coordination matters more than immediate tactics.",
          s"**$move** points to a different strategic route, so tempo handling becomes the key test.",
          s"**$move** is still feasible in practice, while the follow-up relies on cleaner sequencing."
        ))
    }
  }

  def getHypothesisObservationClause(bead: Int, observation: String): String = {
    val obs = observation.trim.stripSuffix(".")
    pick(bead ^ 0x5f356495, List(
      s"From the board, $obs.",
      s"Initial board read: $obs.",
      s"Observed directly: $obs.",
      s"Concrete observation first: $obs."
    ))
  }

  def getHypothesisClause(
    bead: Int,
    claim: String,
    confidence: Double,
    horizon: HypothesisHorizon,
    axis: HypothesisAxis
  ): String = {
    val clean = claim.trim.stripSuffix(".")
    val axisText = axisLabel(axis, bead ^ 0x13a5b7c9)
    val leadTemplates =
      if confidence >= 0.78 then List(
        "Working hypothesis:",
        "Strongest read is that",
        "Clearest read is that"
      )
      else if confidence >= 0.58 then List(
        "The working hypothesis is that",
        "A likely explanation is that",
        "The most plausible read is that",
        "Current evidence suggests that"
      )
      else List(
        "A cautious hypothesis is that",
        "A provisional read is that",
        "One possibility is that"
      )
    val lead = pick(bead ^ 0x24d8f59c, leadTemplates)
    val horizonText =
      horizon match
        case HypothesisHorizon.Short  => "short-horizon"
        case HypothesisHorizon.Medium => "medium-horizon"
        case HypothesisHorizon.Long   => "long-horizon"
    val lensSentence = pick(bead ^ 0x6d2b79f5, List(
      s"The explanatory lens is $axisText with $horizonText consequences.",
      s"Interpret this through $axisText, where $horizonText tradeoffs dominate.",
      s"The underlying axis is $axisText, and the payoff window is $horizonText.",
      s"Strategic weight shifts toward $axisText on a $horizonText timeframe.",
      s"Analysis focuses on $axisText within a $horizonText perspective.",
      s"The framing centers on $axisText, governed by $horizonText dynamics."
    ))
    s"$lead $clean $lensSentence"
  }

  def getHypothesisValidationClause(
    bead: Int,
    supportSignals: List[String],
    conflictSignals: List[String],
    confidence: Double
  ): String = {
    val support0 = supportSignals.map(_.trim).filter(_.nonEmpty).take(2)
    val support =
      if support0.size >= 2 then
        val (engineSignals, otherSignals) = support0.partition(_.toLowerCase.contains("engine gap"))
        if engineSignals.nonEmpty && otherSignals.nonEmpty then
          if Math.floorMod(bead, 3) == 0 then engineSignals ++ otherSignals
          else otherSignals ++ engineSignals
        else if Math.floorMod(bead, 2) == 1 then support0.reverse
        else support0
      else support0
    val conflict = conflictSignals.map(_.trim).filter(_.nonEmpty).take(1)
    if support.nonEmpty && conflict.isEmpty then
      pick(bead ^ 0x3b5296f1, List(
        s"Validation evidence includes ${support.mkString(" and ")}.",
        s"Validation evidence points to ${support.mkString(" and ")}.",
        s"Validation lines up with ${support.mkString(" and ")}.",
        s"This read is validated by ${support.mkString(" plus ")}.",
        s"Validation evidence, specifically ${support.mkString(" and ")}, backs the claim.",
        s"Validation confirmation comes from ${support.mkString(" and ")}."
      ))
    else if support.nonEmpty && conflict.nonEmpty then
      pick(bead ^ 0x6d2b79f5, List(
        s"Validation is mixed: ${support.mkString(" and ")} support the idea, but ${conflict.head} keeps caution necessary.",
        s"Validation evidence supports the read via ${support.mkString(" and ")}, yet ${conflict.head} limits certainty.",
        s"Validation remains conditional: ${support.mkString(" and ")} back the claim, while ${conflict.head} is unresolved.",
        s"Validation support from ${support.mkString(" and ")} is tempered by ${conflict.head}.",
        s"Validation context: while ${support.mkString(" and ")} align with the read, ${conflict.head} suggests a more cautious view."
      ))
    else if confidence < 0.55 then
      pick(bead ^ 0x11f17f1d, List(
        "Validation is still thin, so this stays a working possibility.",
        "Validation evidence is limited, so the claim should remain provisional.",
        "Validation remains incomplete; treat this as a candidate explanation."
      ))
    else
      pick(bead ^ 0x517cc1b7, List(
        "Validation relies on broader strategic consistency rather than a single forcing proof.",
        "Validation for the claim is supported by positional consistency, not by one tactical sequence.",
        "Validation evidence is mostly strategic, so practical confirmation still matters."
      ))
  }

  def getSupportingHypothesisClause(
    bead: Int,
    claim: String,
    confidence: Double,
    axis: HypothesisAxis
  ): String = {
    val clean = claim.trim.stripSuffix(".")
    val toneOptions =
      if confidence >= 0.72 then pick(bead ^ 0x12345678, List(
        "A supporting hypothesis is that",
        "A corroborating idea is that",
        "Supporting that, we see that",
        "Another key pillar is that"
      ))
      else if confidence >= 0.52 then pick(bead ^ 0x23456789, List(
        "A secondary read is that",
        "Collateral evidence suggests that",
        "A contributing factor is that",
        "A parallel hypothesis is that"
      ))
      else pick(bead ^ 0x34567890, List(
        "A weaker supporting idea is that",
        "A peripheral consideration is that",
        "A minor supporting thread is that"
      ))
    val axisText = axisLabel(axis, bead ^ 0x27d4eb2f)
    val tail = pick(bead ^ 0x11f17f1d, List(
      s"This reinforces the $axisText perspective.",
      s"It supports the $axisText reading.",
      s"This adds weight to the $axisText interpretation.",
      s"The $axisText angle is bolstered by this idea.",
      s"It further cements the $axisText focus."
    ))
    s"$toneOptions $clean $tail"
  }

  def getHypothesisPracticalClause(
    bead: Int,
    horizon: HypothesisHorizon,
    axis: HypothesisAxis,
    move: String
  ): String = {
    val axisText = axisLabel(axis, bead ^ 0x85ebca6b)
    val templates =
      horizon match
        case HypothesisHorizon.Short =>
          List(
            s"In practical terms, the split should appear in the next few moves, especially around $axisText handling.",
            s"Practical short-horizon test: the next move-order around $axisText will test whether **$move** holds up.",
            s"Immediate practical impact is expected: $axisText in the next sequence is critical.",
            s"Practical short-term handling matters here, because $axisText errors are punished quickly.",
            s"The practical immediate future revolves around $axisText accuracy.",
            s"Within a few moves, practical $axisText choices should separate the sampled lines."
          )
        case HypothesisHorizon.Medium =>
          List(
            s"Practically, this should influence middlegame choices where $axisText commitments are tested.",
            s"The practical medium-horizon task is keeping $axisText synchronized before the position simplifies.",
            s"After development, practical $axisText decisions are likely to test whether **$move** remains robust.",
            s"The practical burden appears in the middlegame phase, once $axisText tradeoffs become concrete.",
            s"Practical middlegame stability is tied to how $axisText is handled in the next regrouping.",
            s"Practical strategic balance depends on $axisText management as the game transitions."
          )
        case HypothesisHorizon.Long =>
          List(
            s"In practical terms, the divergence is long-horizon: $axisText choices now may shape the later simplified position.",
            s"The practical implication is long-term; $axisText tradeoffs here are likely to resurface in the ending.",
            s"Practically this points to a late-phase split, where $axisText decisions today shape the later direction."
          )
    pick(bead ^ 0x4f6cdd1d, templates)
  }

  def getLongHorizonBridgeClause(
    bead: Int,
    move: String,
    axis: HypothesisAxis
  ): String = {
    val axisText = axisLabel(axis, bead ^ 0x2a2a2a2a)
    val templates = List(
      s"Because **$move** stabilizes $axisText now, the important comparison may appear when later simplification choices begin.",
      s"The immediate point of **$move** is cleaner $axisText coordination now, while the later simplified position still needs checking.",
      s"By locking in $axisText with **$move** now, the long-horizon test is whether that detail survives later exchanges.",
      s"With **$move**, the short-term position stays controlled around $axisText now, but the later simplified line still needs verification.",
      s"**$move** secures today's $axisText tradeoff now, and that shifts what must be checked at the next major transition.",
      s"After **$move** fixes the current $axisText framework now, the later branch comparison remains line-dependent."
    )
    pick(bead ^ 0x7f4a7c15, templates)
  }

  def getAlternativeHypothesisDifferenceVariants(
    bead: Int,
    alternativeMove: String,
    mainMove: String,
    mainAxis: Option[HypothesisAxis],
    alternativeAxis: Option[HypothesisAxis],
    alternativeClaim: Option[String],
    confidence: Double,
    horizon: HypothesisHorizon
  ): List[String] = {
    val altAxisText = alternativeAxis.map(axisLabel).getOrElse("plan balance")
    val mainAxisText = mainAxis.map(axis => axisLabel(axis, bead ^ 0x4f6cdd1d)).getOrElse("the principal plan")
    val altAxisVar = alternativeAxis.map(axis => axisLabel(axis, bead ^ 0x63d5a6f1)).getOrElse(altAxisText)
    val axisContrastOptions =
      if alternativeAxis == mainAxis then
        List(
          s"keeps the same $altAxisVar focus, but the timing window shifts",
          s"tracks the same $altAxisVar theme while changing move-order timing",
          s"stays on the $altAxisVar route, yet it sequences commitments differently",
          s"leans on the same $altAxisVar logic, with a different timing profile"
        )
      else
        List(
          s"shifts priority toward $altAxisVar rather than $mainAxisText",
          s"rebalances the plan toward $altAxisVar instead of $mainAxisText",
          s"places $altAxisVar ahead of $mainAxisText in the practical order",
          s"redirects emphasis to $altAxisVar while reducing $mainAxisText priority",
          s"changes the center of gravity from $mainAxisText to $altAxisVar",
          s"tilts the strategic balance toward $altAxisVar relative to $mainAxisText",
          s"prioritizes $altAxisVar over the standard $mainAxisText reading",
          s"reorients the game around $altAxisVar instead of $mainAxisText",
          s"elevates the importance of $altAxisVar over $mainAxisText",
          s"emphasizes $altAxisVar at the expense of $mainAxisText"
        )
    val confidenceLead =
      if confidence >= 0.72 then "likely"
      else if confidence >= 0.5 then "plausibly"
      else "possibly"
    val strategicAnchor =
      alternativeAxis.orElse(mainAxis) match
        case Some(HypothesisAxis.PawnBreakTiming) => "timing"
        case Some(HypothesisAxis.PieceCoordination) => "coordination"
        case Some(HypothesisAxis.Initiative) => "timing"
        case Some(HypothesisAxis.KingSafety) => "timing"
        case Some(HypothesisAxis.Conversion) => "timing"
        case _ => "trajectory"
    val strategicSentenceOptions =
      strategicAnchor match
        case "timing" =>
          List(
            "The position still turns on timing precision.",
            "Timing discipline still sets the practical limit.",
            "Timing accuracy remains the practical priority.",
            "Move-order timing is still the central strategic issue.",
            "Strategic comparison still hinges on tempo management.",
            "The plan remains tempo-sensitive at every turn.",
            "Timing control continues to define the position.",
            "Precise sequencing remains the strategic anchor.",
            "Tempo handling still governs practical stability.",
            "Strategic pressure remains tied to accurate timing.",
            "The position still rewards strict move-order precision.",
            "Timing remains a key strategic resource."
          )
        case "coordination" =>
          List(
            "The position still turns on coordination quality.",
            "Piece coordination remains the strategic baseline.",
            "The comparison still comes from cleaner coordination.",
            "Strategic clarity still depends on piece harmony.",
            "Coordination quality remains the key strategic metric.",
            "The plan continues to revolve around coordinated piece play.",
            "Piece harmony remains the practical coordination driver.",
            "Strategic control still rests on coordinated deployment.",
            "Coordination remains the central positional requirement.",
            "The position still favors superior piece coordination.",
            "Strategic stability remains tied to coordination discipline.",
            "Coordination quality still shapes the next practical choices."
          )
        case _ =>
          List(
            "The position still turns on long-term trajectory.",
            "The practical story still follows a long-term strategic path.",
            "Long-range planning remains the core strategic task.",
            "Strategic direction still matters more than short-term noise.",
            "The position remains governed by long-horizon planning.",
            "Long-term route selection is still the strategic anchor.",
            "Overall trajectory still shapes the strategic direction.",
            "The game still turns on long-range strategic direction.",
            "Long-horizon structure remains a key strategic layer.",
            "The position still rewards coherent long-term planning.",
            "Strategic balance remains defined by trajectory management.",
            "Long-term strategic steering remains essential here."
          )
    val strategicSentence =
      pick(bead ^ 0x11f6d5a3, strategicSentenceOptions)
    val claimPart = alternativeClaim.map(_.trim.stripSuffix(".")).filter(_.nonEmpty)
      .map(c => s"$c.")
      .getOrElse("")
    val horizonTailOptions =
      horizon match
        case HypothesisHorizon.Short =>
          List(
            "The split should surface in immediate move-order fights.",
            "This difference is expected to matter right away in concrete sequencing.",
            s"Concrete tactical play after **$alternativeMove** should expose the split quickly."
          )
        case HypothesisHorizon.Medium =>
          List(
            s"Middlegame pressure around **$alternativeMove** is where this route starts to separate from the main plan.",
            s"After **$alternativeMove**, concrete commitments harden and coordination plans must be rebuilt.",
            s"The key practical fork is likely during the first serious middlegame regrouping after **$alternativeMove**.",
            s"This contrast tends to become visible when **$alternativeMove** reaches concrete middlegame commitments.",
            s"From a medium-horizon view, **$alternativeMove** often diverges once plan commitments become irreversible.",
            s"A few moves later, **$alternativeMove** often shifts which side controls the strategic transition.",
            s"The real test for **$alternativeMove** appears when middlegame plans have to be fixed to one structure.",
            s"Middlegame stability around **$alternativeMove** is tested as the structural tension resolves.",
            s"The practical split at **$alternativeMove** tends to widen once transition plans become concrete."
          )
        case HypothesisHorizon.Long =>
          List(
            s"The divergence after **$alternativeMove** is expected to surface later in the simplified position.",
            s"With **$alternativeMove**, this split should reappear when later exchanges clarify the structure.",
            s"For **$alternativeMove**, the practical difference is likely delayed until late-phase details are checked.",
            s"Long-range consequences of **$alternativeMove** often surface only after structural simplification.",
            s"The full strategic weight of **$alternativeMove** is tested when the final structure becomes clearer."
          )
    val wrappers = List(
      "Compared with",
      "Relative to",
      "Against the main move",
      "Versus the principal choice",
      "In contrast to",
      "Set against",
      "Measured against"
    )
    val axisStart = Math.floorMod(bead ^ 0x5f356495, axisContrastOptions.size)
    val horizonStart = Math.floorMod(bead ^ 0x4b4b4b4b, horizonTailOptions.size)
    wrappers.zipWithIndex.map { case (prefix, idx) =>
      val axisContrast = axisContrastOptions(Math.floorMod(axisStart + idx, axisContrastOptions.size))
      val horizonTail = horizonTailOptions(Math.floorMod(horizonStart + idx, horizonTailOptions.size))
      s"$prefix **$mainMove**, **$alternativeMove** $confidenceLead $axisContrast. $claimPart $horizonTail $strategicSentence"
    }
      .map(_.replaceAll("""\s+""", " ").trim)
      .distinct
  }

  def getAlternativeHypothesisDifference(
    bead: Int,
    alternativeMove: String,
    mainMove: String,
    mainAxis: Option[HypothesisAxis],
    alternativeAxis: Option[HypothesisAxis],
    alternativeClaim: Option[String],
    confidence: Double,
    horizon: HypothesisHorizon
  ): String = {
    pick(
      bead ^ 0x2f6e2b1,
      getAlternativeHypothesisDifferenceVariants(
        bead = bead,
        alternativeMove = alternativeMove,
        mainMove = mainMove,
        mainAxis = mainAxis,
        alternativeAxis = alternativeAxis,
        alternativeClaim = alternativeClaim,
        confidence = confidence,
        horizon = horizon
      )
    )
  }

  def getWrapUpDecisiveDifferenceVariants(
    bead: Int,
    mainMove: String,
    altMove: String,
    mainAxis: HypothesisAxis,
    altAxis: HypothesisAxis,
    mainHorizon: HypothesisHorizon,
    altHorizon: HypothesisHorizon
  ): List[String] = {
    val mainAxisText = axisLabel(mainAxis, bead ^ 0x11f17f1d)
    val altAxisText = axisLabel(altAxis, bead ^ 0x3124bcf5)
    val axisContrast =
      if mainAxis == altAxis then s"the same $mainAxisText axis"
      else s"$mainAxisText versus $altAxisText"
    val horizonBlend =
      if mainHorizon == altHorizon then s"a shared ${horizonLabel(mainHorizon)} horizon"
      else s"${horizonLabel(mainHorizon)} vs ${horizonLabel(altHorizon)} horizon"
    List(
      s"Main split: **$mainMove** versus **$altMove** on $axisContrast with $horizonBlend.",
      s"Core contrast: **$mainMove** and **$altMove** diverge by $axisContrast across $horizonBlend.",
      s"By comparison, **$mainMove** and **$altMove** diverge through $axisContrast, with $horizonBlend.",
      s"Practical split: **$mainMove** against **$altMove** is $axisContrast under $horizonBlend.",
      s"At the main split, **$mainMove** and **$altMove** divide along $axisContrast with $horizonBlend.",
      s"Final comparison: **$mainMove** vs **$altMove**, defined by $axisContrast and $horizonBlend.",
      s"From a practical contrast angle, **$mainMove** and **$altMove** separate through $axisContrast under $horizonBlend.",
      s"Key difference at the main fork: **$mainMove** vs **$altMove** hinges on $axisContrast within $horizonBlend.",
      s"Comparatively, **$mainMove** and **$altMove** are distinguished by $axisContrast during $horizonBlend.",
      s"The principal fork is **$mainMove** versus **$altMove**: $axisContrast under $horizonBlend."
    )
      .map(_.replaceAll("""\s+""", " ").trim)
      .distinct
  }

  def getWrapUpDecisiveDifference(
    bead: Int,
    mainMove: String,
    altMove: String,
    mainAxis: HypothesisAxis,
    altAxis: HypothesisAxis,
    mainHorizon: HypothesisHorizon,
    altHorizon: HypothesisHorizon
  ): String = {
    pick(
      bead ^ 0x19f8b4ad,
      getWrapUpDecisiveDifferenceVariants(
        bead = bead,
        mainMove = mainMove,
        altMove = altMove,
        mainAxis = mainAxis,
        altAxis = altAxis,
        mainHorizon = mainHorizon,
        altHorizon = altHorizon
      )
    )
  }

  private def axisLabel(axis: HypothesisAxis): String =
    axisLabel(axis, axis.toString.hashCode)

  private def axisLabel(axis: HypothesisAxis, seed: Int): String =
    axis match
      case HypothesisAxis.Plan =>
        pick(seed, List("plan direction", "strategic route", "plan cadence", "long-plan map", "strategic trajectory", "plan orientation", "long-term roadmap", "strategic pathing"))
      case HypothesisAxis.Structure =>
        pick(seed, List("structure management", "structural control", "pawn-structure handling", "square-complex management", "positional integrity", "structural stability", "pawn-center management", "structural coordination"))
      case HypothesisAxis.Initiative =>
        pick(seed, List("initiative control", "momentum balance", "initiative timing", "tempo initiative", "initiative management", "momentum control", "dynamic balance", "initiative pulse"))
      case HypothesisAxis.Conversion =>
        pick(seed, List("simplification timing", "exchange timing", "late-phase coordination", "simplification route", "simplification logic", "exchange-order precision", "late-phase structure", "simplification pathing"))
      case HypothesisAxis.KingSafety =>
        pick(seed, List("king-safety timing", "king security management", "defensive king timing", "king-cover stability", "king-safety assessment", "defensive synchronization", "protective coordination", "king-security profile"))
      case HypothesisAxis.PieceCoordination =>
        pick(seed, List("piece coordination", "piece-route harmony", "coordination lanes", "piece synchronization", "piece interaction", "coordination efficiency", "piece harmony", "coordination stability"))
      case HypothesisAxis.PawnBreakTiming =>
        pick(seed, List("pawn-break timing", "pawn-lever timing", "central break timing", "pawn tension timing", "break-order precision", "pawn-break execution", "tension resolution timing", "lever-activation timing"))
      case HypothesisAxis.EndgameTrajectory =>
        pick(seed, List("endgame trajectory", "late-phase path", "late-phase trajectory", "endgame direction", "simplified-position trajectory", "endgame roadmap", "late-game trajectory", "late-phase direction"))

  private def horizonLabel(horizon: HypothesisHorizon): String =
    horizon match
      case HypothesisHorizon.Short  => "short"
      case HypothesisHorizon.Medium => "medium"
      case HypothesisHorizon.Long   => "long"

  def getThreatWarning(bead: Int, kind: String, square: Option[String], ctx: Option[NarrativeContext] = None): String = {
    val loc = square.map(s => s" on $s").getOrElse("")
    ctx match
      case Some(narrativeCtx)
          if StandardCommentaryClaimPolicy.quietStandardPosition(narrativeCtx) &&
            !StandardCommentaryClaimPolicy.allowsAmberTier(narrativeCtx) =>
        ""
      case Some(narrativeCtx)
          if StandardCommentaryClaimPolicy.isStandard(narrativeCtx) &&
            !StandardCommentaryClaimPolicy.allowsRedTier(narrativeCtx) =>
        pick(bead, List(
          s"There may be ${kind.toLowerCase} pressure$loc to keep in mind.",
          s"The ${kind.toLowerCase} idea$loc is one point to watch.",
          s"The ${kind.toLowerCase} resource$loc can influence the next move order."
        ))
      case _ =>
        pick(bead, List(
          s"Watch out for the ${kind.toLowerCase}$loc.",
          s"Be alert to the ${kind.toLowerCase} threat$loc.",
          s"The opponent may threaten ${kind.toLowerCase}$loc."
        ))
  }

  def getAnnotationPositive(bead: Int, playedSan: String): String = {
    pick(bead, List(
      s"**$playedSan** keeps the pieces coordinated and does not create a new weakness.",
      s"**$playedSan** is a solid move that keeps the plan clear.",
      s"**$playedSan** improves the position without giving the opponent a new target.",
      s"With **$playedSan**, the next plan stays legible over the board.",
      s"**$playedSan** keeps development on track and avoids unnecessary complications.",
      s"**$playedSan** is a reliable move that keeps the structure under control.",
      s"**$playedSan** keeps the position coherent while preserving the main ideas.",
      s"**$playedSan** holds the position together and keeps the useful options open."
    ))
  }

  def getAnnotationInvestment(bead: Int, playedSan: String): String = {
    pick(bead, List(
      s"**$playedSan** is a real investment that keeps the main pressure alive.",
      s"**$playedSan** commits material to preserve the active plan.",
      s"With **$playedSan**, the game turns on whether the invested material keeps enough pressure.",
      s"**$playedSan** is an investment move that keeps the active plan in play.",
      s"**$playedSan** leans into a material investment to keep the strategic payoff available."
    ))
  }

  def getAnnotationNegative(bead: Int, playedSan: String, bestSan: String, cpLoss: Int): String = {
    val mark = Thresholds.annotationMark(cpLoss)
    val markedMove = if mark.nonEmpty then s"**$playedSan** $mark" else s"**$playedSan**"

    if Option(bestSan).forall(_.trim.isEmpty) then getAnnotationNegativeWithoutBenchmark(bead, playedSan, cpLoss)
    else
      Thresholds.classifySeverity(cpLoss) match {
        case "blunder" =>
          pick(bead, List(
            s"$markedMove is a blunder; it opens immediate tactical problems, so your position loses control quickly. **$bestSan** was the cleanest defense.",
            s"$markedMove is a serious error, and as a result coordination collapses quickly. The critical move was **$bestSan**.",
            s"$markedMove is a blunder that hands over game flow, while **$bestSan** was the more resilient route.",
            s"$markedMove is a blunder because it disconnects defense from counterplay; **$bestSan** was the most resilient way to hold things together.",
            s"$markedMove is a major error, so king safety and coordination both deteriorate; **$bestSan** held the balance."
          ))
        case "mistake" =>
          pick(bead, List(
            s"$markedMove is a clear mistake; it gives the opponent a cleaner route, so defense becomes more demanding. Stronger is **$bestSan**.",
            s"$markedMove is a mistake that worsens coordination, while **$bestSan** keeps the structure more coherent.",
            s"$markedMove is a serious mistake that shifts coordination control, and **$bestSan** was the preferable route.",
            s"$markedMove is a mistake because it loosens tempo order; **$bestSan** keeps the position connected.",
            s"$markedMove is inaccurate in move-order terms, so **$bestSan** was the cleaner way to preserve coordination."
          ))
        case "inaccuracy" =>
          pick(bead, List(
            s"$markedMove is an inaccuracy, and **$bestSan** keeps cleaner control of coordination.",
            s"$markedMove concedes coordination clarity, so **$bestSan** is the tighter continuation.",
            s"$markedMove drifts from the best plan, while **$bestSan** keeps coordination better anchored.",
            s"$markedMove is slightly loose in timing, and **$bestSan** keeps the structure more coherent.",
            s"$markedMove gives up some coordination control; therefore **$bestSan** is the cleaner route."
          ))
        case _ =>
          pick(bead, List(
            s"$markedMove is slightly imprecise, while **$bestSan** is cleaner in move-order terms.",
            s"$markedMove is playable but second-best, and **$bestSan** keeps the sampled route clearer.",
            s"$markedMove is not the top choice here, so **$bestSan** remains the reference move.",
            s"$markedMove can be played, but **$bestSan** keeps better structure and coordination.",
            s"$markedMove is acceptable yet less direct, whereas **$bestSan** keeps coordination more stable."
          ))
      }
  }

  def getAnnotationNegativeWithoutBenchmark(bead: Int, playedSan: String, cpLoss: Int): String = {
    val mark = Thresholds.annotationMark(cpLoss)
    val markedMove = if mark.nonEmpty then s"**$playedSan** $mark" else s"**$playedSan**"

    Thresholds.classifySeverity(cpLoss) match {
      case "blunder" =>
        pick(bead, List(
          s"$markedMove is a blunder; it opens immediate tactical problems and loses control quickly.",
          s"$markedMove is a serious error, so coordination collapses and the position turns difficult fast.",
          s"$markedMove is a blunder that hands over game flow and leaves too much to defend.",
          s"$markedMove is a major error because defense disconnects from counterplay and coordination slips away."
        ))
      case "mistake" =>
        pick(bead, List(
          s"$markedMove is a clear mistake; it gives the opponent a cleaner route and makes defense more demanding.",
          s"$markedMove is a serious mistake that shifts coordination control away from your side.",
          s"$markedMove is a mistake because it loosens coordination and leaves the position harder to manage.",
          s"$markedMove is inaccurate in move-order terms, so the opponent gets a cleaner route to steer."
        ))
      case "inaccuracy" =>
        pick(bead, List(
          s"$markedMove is an inaccuracy that gives up some coordination clarity.",
          s"$markedMove drifts from the cleanest plan and leaves coordination a bit looser.",
          s"$markedMove is slightly loose in timing, so the opponent gets cleaner play.",
          s"$markedMove gives up some coordination control and leaves the position less secure."
        ))
      case _ =>
        pick(bead, List(
          s"$markedMove is slightly imprecise in practical terms.",
          s"$markedMove is playable but not the cleanest continuation here.",
          s"$markedMove can be played, though the position becomes a bit less direct to handle.",
          s"$markedMove is acceptable yet less tidy in structure and coordination."
        ))
    }
  }

  def getEngineRankContext(bead: Int, rank: Option[Int], bestSan: String, cpLoss: Int = 0): Option[String] =
    rank match {
      case Some(2) if cpLoss <= 35 =>
        Some(pick(bead, List(
          s"Engine preference still leans to **$bestSan**, but the score gap is modest and coordination themes stay similar.",
          s"The engine line order is close here; **$bestSan** is still cleaner, while structure remains comparable.",
          s"By score order, **$bestSan** remains first, although the evaluation difference stays small."
        )))
      case Some(2) =>
        Some(pick(bead, List(
          s"Engine preference still leans to **$bestSan** as the cleaner move-order, so coordination demands are lower.",
          s"In engine ordering, **$bestSan** remains first, while this line requires tighter coordination.",
          s"The top engine continuation starts with **$bestSan**, because it keeps the structure more resilient."
        )))
      case Some(r) if r >= 3 && cpLoss <= 35 =>
        Some(pick(bead, List(
          s"Engine ordering is still close; **$bestSan** remains the cleanest reference line, while this stays playable.",
          s"This line is playable, but **$bestSan** keeps a cleaner move-order and clearer checked path.",
          s"Score ordering still prefers **$bestSan**, though the evaluation gap stays small and strategic plans overlap."
        )))
      case Some(r) if r >= 3 =>
        Some(pick(bead, List(
          s"This sits below the principal engine candidates, so **$bestSan** gives the more reliable setup.",
          s"Engine ranking puts this in a lower tier, while **$bestSan** keeps tighter control of coordination.",
          s"This is outside the top engine choices, and **$bestSan** remains the stable engine reference."
        )))
      case None if cpLoss <= 35 =>
        Some(pick(bead, List(
          s"This move was not in the sampled MultiPV set, so **$bestSan** remains the engine reference.",
          s"The sampled principal lines still favor **$bestSan**, with only a modest evaluation difference in coordination.",
          s"From sampled engine lines, **$bestSan** remains the cleaner benchmark for strategic stability."
        )))
      case None =>
        Some(pick(bead, List(
          s"This move is outside sampled principal lines, and **$bestSan** is the engine reference for a more stable setup.",
          s"The move is not in the main MultiPV set, so **$bestSan** is the more robust engine alternative.",
          s"Sampled principal lines do not prioritize this, while **$bestSan** remains the engine benchmark."
        )))
      case _ => None
    }

  private def formatPawnUnits(absCp: Int): String =
    f"${absCp.toDouble / 100}%.1f"

  /** Delta phrasing for move-annotation mode (immediate before/after). */
  def getEvalSwingAfterMoveStatement(bead: Int, mover: String, moverCp: Int): Option[String] = {
    val absCp = Math.abs(moverCp)
    if (absCp < 20) None
    else {
      val amount = formatPawnUnits(absCp)
      val verb =
        if (absCp >= 200) pick(bead, List("swings", "tilts", "flips"))
        else if (absCp >= 80) pick(bead, List("shifts", "tilts", "nudges"))
        else pick(bead, List("nudges", "edges", "slightly shifts"))

      val templates =
        if (moverCp > 0) List(
          s"Engine-wise, it $verb the evaluation in $mover's favor by about $amount pawns.",
          s"That $verb the balance toward $mover (≈$amount).",
          s"It $verb the engine's view toward $mover by roughly $amount."
        )
        else List(
          s"It $verb the evaluation away from $mover by about $amount pawns.",
          s"That hands the opponent extra counterplay (≈$amount).",
          s"Engine-wise, $mover gives up about $amount in the evaluation."
        )

      Some(pick(bead, templates))
    }
  }

  def getPhaseTransitionStatement(bead: Int, from: String, to: String): String = {
    val f = from.toLowerCase
    val t = to.toLowerCase
    pick(bead, List(
      s"It also marks the shift from the $f into the $t.",
      s"The character changes here: the game moves from $f to $t.",
      s"This is the moment the $f gives way to the $t."
    ))
  }

  def getOpenFileCreatedStatement(bead: Int, file: String): String = {
    val f = file.trim
    if (f.isEmpty) ""
    else
      pick(bead, List(
        s"The $f-file opens up, so file control becomes a theme.",
        s"The $f-file is now open—rooks will matter.",
        s"With the $f-file opening, activity on that file is a priority."
      ))
  }

  def getStructureChangeStatement(bead: Int, change: String): String = {
    val c = change.trim
    if (c.isEmpty) ""
    else
      pick(bead, List(
        s"Structurally, $c.",
        s"One structural detail: $c.",
        s"The pawn structure shifts: $c."
      ))
  }

  def getMotifAppearsStatement(bead: Int, motif: String): String = {
    deferredMotifDeltaLabel(motif) match
      case Some(None) => ""
      case Some(Some(m)) =>
        pick(bead, List(
          s"A new practical theme appears: $m.",
          s"The position now points to $m.",
          s"This introduces the practical idea of $m."
        ))
      case None =>
        val m = motif.trim
        if (m.isEmpty) ""
        else
          pick(bead, List(
            s"A new motif appears: $m.",
            s"Tactically, $m enters the position.",
            s"This introduces the idea of $m."
          ))
  }

  def getMotifFadesStatement(bead: Int, motif: String): String = {
    deferredMotifDeltaLabel(motif) match
      case Some(None) => ""
      case Some(Some(m)) =>
        pick(bead, List(
          s"The $m theme fades from the position.",
          s"One practical theme eases: $m.",
          s"The immediate idea of $m no longer applies."
        ))
      case None =>
        val m = motif.trim
        if (m.isEmpty) ""
        else
          pick(bead, List(
            s"The $m motif fades from the position.",
            s"One theme disappears: $m.",
            s"The immediate idea of $m no longer applies."
          ))
  }

  private def deferredMotifDeltaLabel(rawMotif: String): Option[Option[String]] =
    if RelationObservationCatalog.relationWitnessOnlyMotifTag(rawMotif) then Some(None)
    else RelationObservationCatalog.deferredFallbackForMotifTag(rawMotif).map(_.label)

  private val motifPrefixSignals: Set[String] = Set(
    "bad_bishop",
    "battery",
    "bishop_pair",
    "blockade",
    "color_complex",
    "connected_rooks",
    "deflection",
    "doubled_rooks",
    "exchange_sacrifice",
    "good_bishop",
    "greek_gift",
    "hanging_pawns",
    "interference",
    "iqp",
    "isolated_pawn",
    "king_hunt",
    "knight_domination",
    "knight_vs_bishop",
    "liquidate",
    "liquidation",
    "maneuver",
    "minority_attack",
    "novelty",
    "open_file",
    "opposite_bishops",
    "passed_pawn",
    "pawn_break",
    "pawn_storm",
    "pin",
    "pin_queen",
    "prophylactic",
    "prophylaxis",
    "repeat",
    "repetition",
    "rook_lift",
    "rook_on_seventh",
    "semi_open_file_control",
    "simplification",
    "simplify",
    "skewer",
    "skewer_queen",
    "smothered_mate",
    "stalemate",
    "stalemate_trick",
    "underpromotion",
    "xray",
    "xray_queen"
  )

  def isMotifPrefixSignal(rawMotif: String): Boolean =
    val normalized = normalizeMotifTag(rawMotif)
    normalized.nonEmpty &&
      !RelationObservationCatalog.relationWitnessOnlyMotifTag(normalized) &&
      RelationObservationCatalog.deferredFallbackForMotifTag(normalized).isEmpty &&
      motifPrefixSignals.exists(sig => motifMatches(normalized, sig))

  def getMotifPrefix(bead: Int, motifs: List[String], ply: Int = 0): Option[String] = {
    val templates = NarrativeMotifPrefixTable.templatesFor(motifs, ply)
    templates.map(ts => punctuate(pickWithPlyRotation(bead ^ 0x3c6ef372, ply, ts)))
  }

  def getThreatStatement(bead: Int, kind: String, loss: Int, ctx: Option[NarrativeContext] = None): String = {
    val strongTierAllowed = ctx.forall(c => StandardCommentaryClaimPolicy.allowsRedTier(c))
    val quietSuppressed =
      ctx.exists(c => StandardCommentaryClaimPolicy.quietStandardPosition(c) && loss < 50)
    if quietSuppressed then ""
    else {
      val severity =
        if strongTierAllowed then
          if (loss >= 300) "urgent" else if (loss >= 100) "serious" else "real"
        else if (loss >= 300) "important"
        else "real"
      val k = kind.trim.toLowerCase
      val kindLabel = k match
        case "mate"       => "mate threat"
        case "material"   => "material threat"
        case "positional" => "positional squeeze"
        case _            => s"$k threat"
      val consequence =
        if strongTierAllowed then
          k match
            case "mate" => pick(bead ^ 0x11b1a5e, List(
              "king safety must come first.",
              "one careless move can end the game.",
              "you cannot afford a slow move."
            ))
            case "material" => pick(bead ^ 0x22c2b6f, List(
              "a piece can be lost if this is ignored.",
              "ignoring it concedes material immediately.",
              "the opponent's tactical gain is hard to stop."
            ))
            case "positional" => pick(bead ^ 0x33d3c7a, List(
              "ignoring it can tie your pieces to defense.",
              "key squares and files can be conceded.",
              "you can drift into a passive, hard-to-defend structure."
            ))
            case _ => pick(bead ^ 0x44e4d8b, List(
              "this needs an accurate response.",
              "the initiative can swing quickly.",
              "you should address this before starting your own plan."
            ))
        else
          k match
            case "mate" => pick(bead ^ 0x11b1a5e, List(
              "king safety needs watching.",
              "move order around the king matters.",
              "you cannot drift for long here."
            ))
            case "material" => pick(bead ^ 0x22c2b6f, List(
              "material pressure can build if it is ignored.",
              "ignoring it can leave the position awkward to hold.",
              "the opponent's practical gain can grow quickly."
            ))
            case "positional" => pick(bead ^ 0x33d3c7a, List(
              "ignoring it can tie your pieces to defense.",
              "key squares and files can become harder to hold.",
              "you can drift into a passive structure."
            ))
            case _ => pick(bead ^ 0x44e4d8b, List(
              "this deserves a careful response.",
              "the initiative can start to drift.",
              "it is worth addressing before starting a slower plan."
            ))

      if strongTierAllowed then
        pick(bead, List(
          s"A $severity $kindLabel is on the board; $consequence",
          s"The $kindLabel requires immediate attention; $consequence",
          s"Handling the $kindLabel is the priority here; $consequence"
        ))
      else
        pick(bead, List(
          s"There is a $severity $kindLabel to keep in mind; $consequence",
          s"The $kindLabel is one practical concern here; $consequence",
          s"The $kindLabel can shape the next move order; $consequence"
        ))
    }
  }

  def getAnnotationTagOnlyHint(bead: Int, tag: CandidateTag, moveHint: String): Option[String] =
    tag match
      case CandidateTag.TacticalGamble =>
        Some(pick(bead ^ 0x1f1f1f, List(
          "It's a tactical try—be ready for a precise response.",
          "This line is a tactical gamble; one loose move can backfire.",
          s"**$moveHint** is a provocation that requires tight tactical tracking.",
          "This branch is a sharp tactical gamble with high variance."
        )))
      case CandidateTag.Sharp =>
        Some(pick(bead ^ 0x2f2f2f, List(
          "The position stays sharp; calculation matters.",
          "Expect complications—accuracy matters here.",
          "The geometry remains sharp, so move-order slips are punished.",
          "Calculation intensity rises as the position remains tactically sharp."
        )))
      case CandidateTag.Prophylactic =>
        Some(pick(bead ^ 0x3f3f3f, List(
          "It also limits counterplay.",
          "A useful prophylactic touch, restricting the opponent's options.",
          "This has a prophylactic flavor, managing risk before it escalates.",
          "The purpose is preventive, shutting down active counterplay early."
        )))
      case CandidateTag.Converting =>
        Some(pick(bead ^ 0x4f4f4f, List(
          "It nudges the game toward simplification.",
          "A practical exchange-oriented approach, aiming for a clearer route.",
          "This is a simplification choice that still needs verification.",
          "Focus shifts to exchange order and follow-up accuracy."
        )))
      case CandidateTag.Solid =>
        Some(pick(bead ^ 0x5f5f5f, List(
          "A solid, low-risk choice.",
          "A steady improving move with few drawbacks.",
          "This is a structurally robust choice with manageable risks.",
          "The hallmark of this line is strategic solidness and stability."
        )))
      case CandidateTag.Competitive =>
        Some(pick(bead ^ 0x6f6f6f, List(
          "Several moves are close in strength.",
          "This is a competitive option among several near-equals.",
          "In a congested engine ranking, this remains a top competitive choice.",
          "The evaluation is narrow, leaving this as a highly competitive route."
        )))

  def getAnnotationDifficultyHint(bead: Int, diff: String, moveHint: String, phase: String): Option[String] =
    val p = phase.trim.toLowerCase
    if diff.contains("complex") then
      Some(pick(bead ^ 0x7f7f7f, List(
        s"After **$moveHint**, the line is complex; keep calculating.",
        s"**$moveHint** leads to complex play where precision is rewarded.",
        s"There are tactical resources after **$moveHint**; stay alert.",
        s"**$moveHint** can produce a messy middlegame; calculation matters.",
        s"This is not a line to play on autopilot after **$moveHint**.",
        s"The ensuing complexity after **$moveHint** demands deep concrete verification."
      )))
    else if diff.contains("clean") then
      if p == "endgame" then
        Some(pick(bead ^ 0x8f8f8f, List(
          s"The line after **$moveHint** is quieter, with king routes and tempi still needing line support.",
          s"**$moveHint** guides play into a simplified position that still needs checking.",
          s"After **$moveHint**, local endgame details matter more than broad labels.",
          s"**$moveHint** keeps the structure stable and highlights current-board details.",
          s"With **$moveHint**, progress is mostly about methodical coordination.",
          s"After **$moveHint**, the simplified structure remains the main reference point."
        )))
      else
        Some(pick(bead ^ 0x9f9f9f, List(
          s"The line after **$moveHint** is relatively quiet, with less tactical turbulence.",
          s"After **$moveHint**, strategy tightens; tactics recede.",
          s"With **$moveHint**, planning depth tends to matter more than short tactics.",
          s"With **$moveHint**, the structure stays stable and plan choices become clearer.",
          s"Structural considerations come to the fore after **$moveHint**, as tactical noise clears.",
          s"The route after **$moveHint** remains strategically transparent but still needs checking.",
          s"With **$moveHint**, tactical complications settle into a clear strategic direction.",
          s"The practical burden after **$moveHint** is structural rather than tactical.",
          s"Follow-up after **$moveHint** guides the game into a quieter structural phase.",
          s"Sequencing after **$moveHint** shifts the priority toward structural details.",
          s"Strategic clarity increases after **$moveHint** as forcing lines resolve.",
          s"The game enters a phase of structural consolidation after **$moveHint**.",
          s"**$moveHint** leads to a structure that rewards accurate follow-up.",
          s"Tactical dust settles after **$moveHint**, leaving a quieter strategic fight."
        )))
    else None

  def getAnnotationTerminalMoveHint(bead: Int, moveHint: String): String =
    pick(bead ^ 0xafafaf, List(
      s"**$moveHint** forces an immediate tactical resolution.",
      s"**$moveHint** ends the game sequence on the spot.",
      s"After **$moveHint**, there is no long maneuvering phase left.",
      s"Tactical finality is reached immediately with **$moveHint**."
    ))

  def getAnnotationTagHint(
    bead: Int,
    tags: List[CandidateTag],
    practicalDifficulty: String,
    moveHint: String,
    phase: String,
    isTerminalMove: Boolean
  ): Option[String] =
    if isTerminalMove then Some(getAnnotationTerminalMoveHint(bead, moveHint))
    else
      tags.flatMap(t => getAnnotationTagOnlyHint(bead, t, moveHint)).headOption
        .orElse(getAnnotationDifficultyHint(bead, practicalDifficulty, moveHint, phase))

  def getPlanStatement(bead: Int, planName: String, ctx: Option[NarrativeContext] = None, ply: Int = 0): String = {
    val localSeed = bead ^ (ply * 0x9e3779b9)
    val p = planDisplay(localSeed, planName.trim, ply)
    if p.isEmpty || ctx.exists(StandardCommentaryClaimPolicy.shouldSuppressPlanStatement) then ""
    else {
      val cycle = Math.floorMod(ply, 4)
      val templates = cycle match
        case 0 => List(
          s"Key theme: **$p**.",
          s"Play still revolves around **$p**."
        )
        case 1 => List(
          s"Current play is organized around **$p**.",
          s"The most reliable roadmap here is built around **$p**."
        )
        case 2 => List(
          s"The practical plan still centers on **$p**.",
          s"The clearest route still runs through **$p**."
        )
        case _ => List(
          s"Move-order choices are justified by **$p**.",
          s"Most sensible plans here converge on **$p**."
        )
      pick(localSeed, templates)
    }
  }

  private def planDisplay(bead: Int, planName: String, ply: Int): String =
    val localSeed = bead ^ (ply * 0x632be59b)
    planName match
      case "Pawn Chain Maintenance" =>
        pick(localSeed, List(
          "maintaining pawn tension",
          "keeping the pawn chain intact",
          "preserving the central pawn structure",
          "avoiding premature pawn breaks",
          "keeping the structure flexible",
          "coordinating piece play before structural commitments",
          "postponing pawn commitments until pieces are ready",
          "holding the center without clarifying too early",
          "keeping central levers available for later timing",
          "synchronizing development with future pawn breaks"
        ))
      case "Central Control" =>
        pick(localSeed, List(
          "central control",
          "a grip on the centre",
          "control of key central squares",
          "central space and control",
          "dominating the centre",
          "space advantage in the center",
          "restricting counterplay through central presence",
          "dictating piece routes via central influence"
        ))
      case other => other

  def evalOutcomeClauseFromCp(cp: Int): String =
    evalOutcomeClauseFromCp(Math.abs(cp.hashCode), cp, ply = 0)

  def evalOutcomeClauseFromCp(bead: Int, cp: Int, ply: Int = 0): String = {
    val absCp = Math.abs(cp)
    val pawns = absCp.toDouble / 100.0
    val approx =
      if absCp >= 30 then
        val sign = if cp >= 0 then "+" else "-"
        Some(f"(≈$sign$pawns%.1f)")
      else None
    def choose(options: List[String]): String =
      pickWithPlyRotation(bead ^ 0x2c1b3c6d ^ Math.abs(cp.hashCode), ply, options)

    def withApprox(base: String): String =
      approx match
        case Some(a) if Math.abs(bead ^ base.hashCode) % 3 == 0 => s"$base $a"
        case _ => base

    if (cp >= 500) choose(List(
      withApprox("White has a very large evaluation edge"),
      withApprox("The engine evaluation is heavily in White's favor"),
      withApprox("White is far ahead on the evaluation"),
      withApprox("White holds a major objective edge"),
      withApprox("The position evaluates strongly for White")
    ))
    else if (cp >= 300) choose(List(
      withApprox("White holds a clear evaluation edge"),
      withApprox("The engine prefers White by a clear margin"),
      withApprox("White is ahead on the objective evaluation"),
      withApprox("White has a sizeable evaluation plus"),
      withApprox("The position evaluates clearly for White")
    ))
    else if (cp >= 100) choose(List(
      withApprox("White has a modest evaluation edge"),
      withApprox("The engine prefers White"),
      withApprox("White is ahead in the evaluation"),
      withApprox("White has a small-to-moderate engine edge"),
      withApprox("The position evaluates a bit better for White")
    ))
    else if (cp >= 30) choose(List(
      withApprox("White has a small evaluation pull"),
      withApprox("The engine gives White a small edge"),
      withApprox("White is a touch ahead on the evaluation"),
      withApprox("The eval is slightly in White's favor"),
      withApprox("White has a modest objective edge")
    ))
    else if (cp <= -500) choose(List(
      withApprox("Black has a very large evaluation edge"),
      withApprox("The engine evaluation is heavily in Black's favor"),
      withApprox("Black is far ahead on the evaluation"),
      withApprox("Black holds a major objective edge"),
      withApprox("The position evaluates strongly for Black")
    ))
    else if (cp <= -300) choose(List(
      withApprox("Black holds a clear evaluation edge"),
      withApprox("The engine prefers Black by a clear margin"),
      withApprox("Black is ahead on the objective evaluation"),
      withApprox("Black has a sizeable evaluation plus"),
      withApprox("The position evaluates clearly for Black")
    ))
    else if (cp <= -100) choose(List(
      withApprox("Black has a modest evaluation edge"),
      withApprox("The engine prefers Black"),
      withApprox("Black is ahead in the evaluation"),
      withApprox("Black has a small-to-moderate engine edge"),
      withApprox("The position evaluates a bit better for Black")
    ))
    else if (cp <= -30) choose(List(
      withApprox("Black has a small evaluation pull"),
      withApprox("The engine gives Black a small edge"),
      withApprox("Black is a touch ahead on the evaluation"),
      withApprox("The eval is slightly in Black's favor"),
      withApprox("Black has a modest objective edge")
    ))
    else choose(List(
      "The position is close to level",
      "The evaluation is near equal",
      "Neither side has a clear evaluation edge",
      "The engine assessment is essentially balanced",
      "The position remains objectively close"
    ))
  }

  def getEvaluativePlanStatement(bead: Int, cp: Int, wPlan: Option[String], bPlan: Option[String], ply: Int = 0): String = {
    val baseEval = evalOutcomeClauseFromCp(bead, cp, ply)
    if (Math.abs(cp) > 40 || (wPlan.isEmpty && bPlan.isEmpty)) baseEval
    else {
      val w = wPlan.map(p => planDisplay(bead ^ 0x111, p, ply)).getOrElse("maintaining the structure")
      val b = bPlan.map(p => planDisplay(bead ^ 0x222, p, ply)).getOrElse("solidifying their position")
      
      pickWithPlyRotation(bead ^ 0x333, ply, List(
        s"With a level evaluation, White looks to play around **$w**, while Black focuses on **$b**.",
        s"The position is finely balanced; White's strategy centers on **$w**, whereas Black aims for **$b**.",
        s"In this dynamically equal position, White's priority is **$w**, and Black will counter with **$b**.",
        s"The evaluation is near level. White's practical roadmap involves **$w**, against Black's plan of **$b**."
      ))
    }
  }

  def getEvaluativeImbalanceStatement(
    bead: Int,
    cp: Int,
    whiteAdvantage: String,
    blackAdvantage: String,
    ply: Int = 0
  ): String = {
    val localSeed = bead ^ (ply * 0x3f1ab)
    
    val baseEval = 
      if (cp > 30) pickWithPlyRotation(localSeed, ply, List(
        "White has a slight pull",
        "White is slightly better",
        "The position slightly favors White"
      ))
      else if (cp < -30) pickWithPlyRotation(localSeed, ply, List(
        "Black has a slight pull",
        "Black is slightly better",
        "The position slightly favors Black"
      ))
      else pickWithPlyRotation(localSeed, ply, List(
        "The material is balanced",
        "The position is dynamically equal",
        "The evaluation is level",
        "The game is in dynamic balance"
      ))

    pickWithPlyRotation(
      localSeed ^ 0x6d2b79f5,
      ply,
      List(
        s"$baseEval, but the tension lies in the imbalances: White has $whiteAdvantage against Black's $blackAdvantage.",
        s"$baseEval; the key lies in White's $whiteAdvantage versus Black's $blackAdvantage.",
        s"$baseEval. The strategic battle pits White's $whiteAdvantage against Black's $blackAdvantage."
      )
    )
  }

  def getPawnPlayStatement(bead: Int, breakFile: String, breakImpact: String, tensionPolicy: String): String = {
    val file = breakFile.trim.toLowerCase
    val lever =
      if file.length == 1 && "abcdefgh".contains(file) then s"${file}-pawn break"
      else s"$breakFile break"

    val impact = breakImpact.trim.toLowerCase match
      case "high" =>
        pick(bead, List("the main lever", "a key lever", "the critical lever"))
      case "medium" =>
        pick(bead, List("a useful lever", "a thematic lever", "a lever to keep in mind"))
      case "low" =>
        pick(bead, List("a secondary lever", "more of a long-term lever", "not urgent yet"))
      case _ =>
        pick(bead, List("a strategic lever", "a structural lever"))

    tensionPolicy.trim.toLowerCase match
      case "maintain" =>
        pick(bead, List(
          s"Keep the pawn tension; the $lever is $impact.",
          s"With tension preserved, the $lever becomes $impact.",
          s"Don't rush to clarify the pawn structure; the $lever is $impact."
        ))
      case "release" =>
        pick(bead, List(
          s"Clarify the pawn tension first; then the $lever can be played with more effect.",
          s"Releasing the tension can make the $lever more potent.",
          s"Once the structure is clarified, the $lever tends to carry more weight."
        ))
      case _ =>
        pick(bead, List(
          s"Watch for the $lever — it is $impact.",
          s"The $lever is $impact in this structure.",
          s"The structure often turns on the $lever, $impact."
        ))
  }

  def getFactStatement(bead: Int, fact: Fact, ctx: NarrativeContext): String =
    CommentaryIdeaSurface.statement(bead, fact, ctx).getOrElse("")

  def getPreventedPlanStatement(bead: Int, planName: String): String = {
    pick(bead, List(
      s"Crucially, this stops the opponent's idea of $planName.",
      s"A key prophylactic benefit is preventing $planName.",
      s"The move effectively neutralizes $planName."
    ))
  }

  def getCompensationStatement(bead: Int, tpe: String, severity: String): String = {
    val typ = tpe.trim
    val sev = severity.trim
    val typLower = typ.toLowerCase
    val sevLower = sev.toLowerCase
    val descriptor =
      if typLower.contains("compensation") then typ
      else if sev.nonEmpty then s"$sev ${typ.trim}".trim
      else typ
    val conciseDescriptor =
      descriptor
        .replaceAll("(?i)\\bcompensation\\b", "")
        .replaceAll("\\s{2,}", " ")
        .trim
    val compact = if conciseDescriptor.nonEmpty then conciseDescriptor else descriptor
    val article =
      if compact.nonEmpty && "aeiou".contains(compact.head.toLower) then "an" else "a"
    val neutralDescriptor =
      if compact.nonEmpty then compact
      else if sevLower.nonEmpty then sev
      else "dynamic"

    pick(bead, List(
      s"The material investment is justified by $article $neutralDescriptor edge in activity.",
      s"White gets $article $neutralDescriptor return for the material deficit.",
      s"The sacrificed material is balanced by $article $neutralDescriptor practical return."
    ))
  }
}
