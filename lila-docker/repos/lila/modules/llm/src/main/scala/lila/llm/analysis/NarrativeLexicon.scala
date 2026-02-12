package lila.llm.analysis

import chess.{ Bishop, King, Knight, Pawn, Queen, Role, Rook }
import lila.llm.model.{ Fact, HypothesisAxis, HypothesisHorizon }

/**
 * Phase 11: Infinite Diversity Lexicon
 * 
 * Central repository for narrative templates.
 * Uses deterministic hashing (bead) to ensure consistent output for the same position,
 * but high variety across different positions.
 */
object NarrativeLexicon {

  enum Style:
    case Book, Coach, Dramatic

  // ===========================================================================
  // 1. OPENING / CONTEXT SETTERS
  // ===========================================================================

  // Deterministic random selection based on mixed seed
  def pick(bead: Int, options: Seq[String]): String = {
    if (options.isEmpty) ""
    else options(Math.abs(bead) % options.size)
  }
  
  // Mixed seed helper
  def mixSeed(seeds: Int*): Int = seeds.foldLeft(0)(_ ^ _.hashCode)

  def gameIntro(white: String, black: String, event: String, date: String, result: String): String =
    s"In this encounter between **$white** and **$black** ($event, $date), the game concluded with **$result**. Let's examine the critical moments."

  def gameConclusion(winner: Option[String], themes: List[String]): String =
    winner match {
      case Some(w) => s"**$w** prevailed, capitalizing on key turning points. The game featured themes such as ${themes.mkString(", ")}."
      case None => s"The game ended in a draw. Both sides missed opportunities to tip the balance, with themes like ${themes.mkString(", ")} emerging."
    }

  def momentBlockLead(bead: Int, phase: String, momentType: String, ply: Int): String = {
    val p = Option(phase).getOrElse("").trim.toLowerCase
    val m = Option(momentType).getOrElse("").trim.toLowerCase
    val phaseLabel =
      p match {
        case s if s.contains("opening")    => "opening block"
        case s if s.contains("endgame")    => "endgame block"
        case s if s.contains("middlegame") => "middlegame block"
        case _                             => "critical game block"
      }

    val templates =
      if (m.contains("opening")) List(
        s"In the $phaseLabel around ply $ply, opening choices start to define the long-term plans.",
        s"This $phaseLabel near ply $ply marks where theory gives way to independent decisions."
      )
      else if (m.contains("mate")) List(
        s"Around ply $ply, this $phaseLabel turns into a forcing tactical sequence.",
        s"The $phaseLabel near ply $ply becomes concrete: mating motifs now drive the evaluation."
      )
      else if (m.contains("blunder") || m.contains("missedwin") || m.contains("swing")) List(
        s"The $phaseLabel around ply $ply is a genuine turning point in the game.",
        s"Around ply $ply, this $phaseLabel creates a decisive shift in practical control."
      )
      else if (m.contains("pressure") || m.contains("tension")) List(
        s"This $phaseLabel near ply $ply is defined by cumulative pressure and move-order accuracy.",
        s"Around ply $ply, the $phaseLabel remains balanced but highly tension-sensitive."
      )
      else List(
        s"In this $phaseLabel around ply $ply, the game's key practical choices become clear.",
        s"Around ply $ply, this $phaseLabel frames the next strategic decisions."
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

  // ===========================================================================
  // 1.5. HUMAN TOUCH: PSYCHOLOGY & CONCESSION (New Phase 6.5)
  // ===========================================================================

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
            "This middlegame phase rewards concrete, well-timed choices"
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
            "practical control can change after a single move-order slip"
          )
        )
      case "endgame" =>
        (
          List(
            "The position has simplified into an endgame",
            "This is a technical endgame phase",
            "Endgame details now dominate the game",
            "We are firmly in endgame territory",
            "The game has entered a conversion-oriented endgame",
            "The struggle has shifted into technical endgame play",
            "This phase is now about endgame conversion technique",
            "Endgame precision now outweighs broad strategic plans"
          ),
          List(
            "king activity and tempi become decisive",
            "piece activity outweighs broad strategic plans",
            "pawn-structure details carry extra weight",
            "precision matters more than ambition here",
            "one tempo can decide the technical outcome",
            "minor king-route details can decide the evaluation",
            "the evaluation now depends on accurate technical handling",
            "practical endgame technique matters more than broad plans"
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
            "the next move can redefine the practical balance",
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

  def getIntent(bead: Int, alignment: String, evidence: Option[String], ply: Int = 0): String = {
    // Phase 21.1: If evidence already contains 'by' or is a gerund, we adjust intro
    val ev = evidence.getOrElse("")
    val hasEv = ev.nonEmpty
    val localSeed = bead ^ (ply * 0x4eb2d)
    def choose(options: List[String]): String =
      pickWithPlyRotation(localSeed ^ Math.abs(alignment.hashCode), ply, options)
    
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
      
      // Phase 22: New intent categories
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
        if (hasEv && !ev.toLowerCase.contains("outpost")) s"establishes an outpost by $ev"
        else if (hasEv) s"improves the position by $ev"
        else choose(List(
          "establishes a strong outpost",
          "places the piece on an unassailable square",
          "anchors a piece on a durable outpost"
        ))
      case s if s.contains("simplification") =>
        if (hasEv) s"simplifies the position by $ev"
        else choose(List(
          "simplifies into a favorable endgame",
          "trades down to an easier position",
          "heads for the endgame",
          "reduces complexity in a favorable way"
        ))
      case s if s.contains("file control") =>
        if (hasEv) s"seizes the open file by $ev"
        else choose(List(
          "seizes the open file",
          "controls the key file",
          "takes command of the file",
          "improves major-piece control on the file"
        ))
      case s if s.contains("passed pawn") =>
        if (hasEv) s"advances the passed pawn by $ev"
        else choose(List(
          "pushes the passed pawn",
          "advances the trumping pawn",
          "creates promotion threats",
          "forces attention to promotion races"
        ))
      case s if s.contains("opposition") =>
        if (hasEv) s"takes the opposition by $ev"
        else choose(List(
          "takes the opposition",
          "gains the opposition",
          "seizes the key squares",
          "improves king geometry in the ending"
        ))
      case s if s.contains("zugzwang") =>
        if (hasEv) s"forces zugzwang by $ev"
        else choose(List(
          "places the opponent in zugzwang",
          "forces a fatal concession",
          "squeezes the opponent",
          "limits useful moves until concessions appear"
        ))
      case s if s.contains("pawn run") || s.contains("pawn_race") =>
         if (hasEv) s"pushes for promotion by $ev"
         else choose(List(
           "races for promotion",
           "pushes the pawn",
           "accelerates the pawn",
           "starts a direct promotion race"
         ))
      case s if s.contains("shouldering") =>
         if (hasEv) s"shoulders the enemy king by $ev"
         else choose(List(
           "uses the king to shoulder the opponent",
           "keeps the enemy king out",
           "dominates with the king",
           "improves king placement to restrict counterplay"
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
        if (hasEv) s"forces an exchange by $ev"
        else choose(List(
          "forces a favorable exchange",
          "trades pieces",
          "simplifies the material",
          "steers the game toward a cleaner structure"
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
    consequence: String = "" // Phase 21.3: Narrative closer
  ): String = {
    val fullMove = s"**$move**$annotation"
    val rep = replySan.map(s => s"...$s").getOrElse("")
    val sample = sampleRest.getOrElse("")
    val cons = if (consequence.nonEmpty) s" $consequence" else ""

    val templates = (replySan, sampleRest) match {
      case (Some(_), Some(_)) => List(
        s"$fullMove $intent; after $rep, play might continue $sample. $evalTerm$cons.",
        s"$fullMove $intent, inviting $rep, where the sequence $sample follows. $evalTerm$cons.",
        s"With $fullMove, White $intent; after $rep $sample follows. $evalTerm$cons.",
        s"$fullMove $intent — if Black defends with $rep, then $sample results in a clear outcome. $evalTerm$cons.",
        s"$fullMove $intent, causing problems after $rep $sample. $evalTerm$cons."
      )
      case (Some(_), None) => List(
        s"$fullMove $intent, and after $rep. $evalTerm$cons.",
        s"By playing $fullMove, White $intent, forcing $rep. $evalTerm$cons.",
        s"Black responds to $fullMove $intent with $rep. $evalTerm$cons."
      )
      case _ => List(
        s"$fullMove $intent. $evalTerm$cons.",
        s"With $fullMove, White $intent. $evalTerm$cons.",
        s"$fullMove $intent. It remains a precise choice. $evalTerm$cons."
      )
    }
    pick(bead, templates)
  }

  /**
   * Phase 18: Expert-level analytical flavour for practical aspects.
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

  // ===========================================================================
  // 6. ALTERNATIVES & CRITIQUE
  // ===========================================================================

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
  private def pickWithPlyRotation(seed: Int, ply: Int, options: List[String]): String = {
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

  // ===========================================================================
  // PHASE 5: NEW SSOT SUPPORT FUNCTIONS
  // ===========================================================================

  def getTeachingPoint(bead: Int, theme: String, cpLoss: Int): String = {
    val severity = if (cpLoss >= 200) "significant" else if (cpLoss >= 100) "noticeable" else "slight"
    val t = theme.trim
    val tLow = t.toLowerCase
    val isSeverityWord = Set("inaccuracy", "mistake", "blunder", "error").contains(tLow)

    if isSeverityWord then
      pick(bead, List(
        s"That's an $tLow with concrete practical consequences.",
        s"A $tLow that hands over practical control.",
        s"It gives the opponent a clearer and easier continuation."
      ))
    else
      pick(bead, List(
        s"Missing $t was a $severity oversight.",
        s"Keeping $t in mind would have avoided the practical setback.",
        s"A $severity oversight: $t was available."
      ))
  }

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
        s"$route$pivot marked the turning point because promotion timing outweighed slower strategic plans."
      )
      else if key.contains("exchange") then List(
        s"$route$pivot marked the exchange turning point, with exchange timing starting to define the evaluation.",
        s"From $route$pivot, the decisive shift was the exchange sequence and resulting simplification.",
        s"$route$pivot became the turning point once exchanges reshaped piece activity and defensive resources.",
        s"The critical turning point was $route$pivot, where exchange decisions fixed the practical balance."
      )
      else if key.contains("tactical") then List(
        s"$route$pivot marked the tactical turning point, once forcing tactical pressure became hard to defuse.",
        s"From $route$pivot, the decisive shift was king safety and concrete tactical accuracy.",
        s"$route$pivot marked the turning point because tactical threats began to override long-term plans.",
        s"The critical turning point was $route$pivot, where tactical forcing lines dictated move order."
      )
      else if key.contains("structural") then List(
        s"$route$pivot marked the structural turning point, when structural features began to dominate planning.",
        s"From $route$pivot, the decisive shift was structural transformation and piece rerouting.",
        s"$route$pivot became the turning point once structure and square control outweighed short tactics.",
        s"The key turning point was $route$pivot, where structural shifts fixed the strategic roadmap."
      )
      else List(
        s"$route$pivot marked the initiative turning point, as initiative control shifted to one side.",
        s"From $route$pivot, the decisive shift was initiative management rather than static factors.",
        s"$route$pivot marked the turning point because tempo and initiative started to decide the play.",
        s"The critical turning point was $route$pivot, where initiative swings determined the practical outcome."
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
      s"Strategically, the game turned on $m.",
      s"The position's strategic transition was driven by $m.",
      s"That branch shifts plans through $m.",
      s"The practical turning factor was $m."
    )
    pick(bead ^ 0x284f2d5b, templates)
  }

  def getPrecedentDecisionDriverLine(
    bead: Int,
    mechanism: String
  ): String = {
    val m = mechanism.trim
    val templates = List(
      s"The decisive practical driver was control of $m.",
      s"Results hinged on who managed $m more accurately.",
      s"The key match result factor was handling $m under pressure.",
      s"Conversion quality around $m separated the outcomes."
    )
    pick(bead ^ 0x3124bcf5, templates)
  }

  def getAlternative(bead: Int, move: String, whyNot: Option[String]): String = {
    whyNot match {
      case Some(reason) if reason.nonEmpty =>
        pick(bead, List(
          s"**$move** is another candidate, but $reason, so practical timing becomes critical.",
          s"A practical sideline is **$move**; however, $reason, which changes the conversion route.",
          s"**$move** keeps options open, yet $reason, and coordination can drift if move order slips.",
          s"**$move** is playable over the board, although $reason, so initiative management becomes harder."
        ))
      case _ =>
        pick(bead, List(
          s"**$move** remains a live candidate, and it steers the game toward a different strategic route.",
          s"One practical detour is **$move**, where coordination matters more than immediate tactics.",
          s"**$move** points to a different strategic route, so tempo handling becomes the key test.",
          s"**$move** is still feasible in practice, while conversion relies on cleaner sequencing."
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
        "Strongest read:",
        "Clearest read:"
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
        s"The evidence, specifically ${support.mkString(" and ")}, backs the claim.",
        s"Confirmation comes from ${support.mkString(" and ")}."
      ))
    else if support.nonEmpty && conflict.nonEmpty then
      pick(bead ^ 0x6d2b79f5, List(
        s"Validation is mixed: ${support.mkString(" and ")} support the idea, but ${conflict.head} keeps caution necessary.",
        s"Evidence supports the read via ${support.mkString(" and ")}, yet ${conflict.head} limits certainty.",
        s"Verification remains conditional: ${support.mkString(" and ")} back the claim, while ${conflict.head} is unresolved.",
        s"The support from ${support.mkString(" and ")} is tempered by ${conflict.head}.",
        s"While ${support.mkString(" and ")} align with the read, ${conflict.head} suggests a more cautious view."
      ))
    else if confidence < 0.55 then
      pick(bead ^ 0x11f17f1d, List(
        "Validation is still thin, so this stays a working possibility.",
        "Evidence is limited, so the claim should remain provisional.",
        "Verification remains incomplete; treat this as a candidate explanation."
      ))
    else
      pick(bead ^ 0x517cc1b7, List(
        "Validation relies on broader strategic consistency rather than a single forcing proof.",
        "The claim is supported by positional consistency, not by one tactical sequence.",
        "Evidence is mostly strategic, so practical confirmation still matters."
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
            s"Short-horizon test: the next move-order around $axisText will determine whether **$move** holds up.",
            s"Immediate practical impact is expected: $axisText in the next sequence is critical.",
            s"Short-term handling is decisive here, because $axisText errors are punished quickly.",
            s"The tactical immediate future revolves around $axisText accuracy.",
            s"Within a few moves, $axisText choices will separate the outcomes."
          )
        case HypothesisHorizon.Medium =>
          List(
            s"Practically, this should influence middlegame choices where $axisText commitments are tested.",
            s"The medium-horizon task is keeping $axisText synchronized before the position simplifies.",
            s"After development, $axisText decisions are likely to determine whether **$move** remains robust.",
            s"The practical burden appears in the middlegame phase, once $axisText tradeoffs become concrete.",
            s"Middlegame stability is tied to how $axisText is handled in the next regrouping.",
            s"Strategic balance depends on $axisText management as the game transitions."
          )
        case HypothesisHorizon.Long =>
          List(
            s"In practical terms, the divergence is long-horizon: $axisText choices now can decide the later conversion path.",
            s"The implication is long-term; $axisText tradeoffs here are likely to resurface in the ending.",
            s"Practically this points to a late-phase split, where $axisText decisions today shape the endgame trajectory."
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
      s"Because **$move** stabilizes $axisText now, the decisive split is expected when later simplification choices begin.",
      s"The immediate gain from **$move** is cleaner $axisText coordination now, and that usually decides the game later once the position simplifies.",
      s"By locking in $axisText with **$move** now, the practical payoff tends to appear later when the endgame plan must be converted.",
      s"With **$move**, the short-term position stays controlled around $axisText now, but the real test arrives later in the conversion phase.",
      s"**$move** secures today's $axisText tradeoff now, and that shifts the balance later when late-phase technique becomes the main battleground.",
      s"After **$move** fixes the current $axisText framework now, the resulting advantage normally appears later at the next major transition."
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
          s"changes the center of gravity from $mainAxisText to $altAxisVar"
        )
    val confidenceLead =
      if confidence >= 0.72 then "likely"
      else if confidence >= 0.5 then "plausibly"
      else "possibly"
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
            s"The divergence after **$alternativeMove** is expected to surface later in the endgame trajectory.",
            s"With **$alternativeMove**, this split should reappear in long-term conversion phases.",
            s"For **$alternativeMove**, the practical difference is likely delayed until late-phase technique.",
            s"Long-range consequences of **$alternativeMove** often surface only after structural simplification.",
            s"The full strategic weight of **$alternativeMove** is felt during the final technical phase."
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
      s"$prefix **$mainMove**, **$alternativeMove** $confidenceLead $axisContrast. $claimPart $horizonTail"
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
      s"Decisive split: **$mainMove** versus **$altMove** on $axisContrast with $horizonBlend.",
      s"Key difference: **$mainMove** and **$altMove** separate by $axisContrast across $horizonBlend.",
      s"Decisively, **$mainMove** and **$altMove** diverge through $axisContrast, with $horizonBlend.",
      s"Practical key difference: **$mainMove** against **$altMove** is $axisContrast under $horizonBlend.",
      s"At the decisive split, **$mainMove** and **$altMove** divide along $axisContrast with $horizonBlend.",
      s"Final decisive split: **$mainMove** vs **$altMove**, defined by $axisContrast and $horizonBlend.",
      s"From a key-difference angle, **$mainMove** and **$altMove** contrast through $axisContrast under $horizonBlend."
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
        pick(seed, List("plan direction", "strategic route", "plan cadence", "long-plan map"))
      case HypothesisAxis.Structure =>
        pick(seed, List("structure management", "structural control", "pawn-structure handling", "square-complex management"))
      case HypothesisAxis.Initiative =>
        pick(seed, List("initiative control", "momentum balance", "initiative timing", "tempo initiative"))
      case HypothesisAxis.Conversion =>
        pick(seed, List("conversion timing", "conversion technique", "simplification timing", "technical conversion"))
      case HypothesisAxis.KingSafety =>
        pick(seed, List("king-safety timing", "king security management", "defensive king timing", "king-cover stability"))
      case HypothesisAxis.PieceCoordination =>
        pick(seed, List("piece coordination", "piece-route harmony", "coordination lanes", "piece synchronization"))
      case HypothesisAxis.PawnBreakTiming =>
        pick(seed, List("pawn-break timing", "pawn-lever timing", "central break timing", "pawn tension timing"))
      case HypothesisAxis.EndgameTrajectory =>
        pick(seed, List("endgame trajectory", "long-phase conversion path", "late-phase trajectory", "endgame direction"))

  private def horizonLabel(horizon: HypothesisHorizon): String =
    horizon match
      case HypothesisHorizon.Short  => "short"
      case HypothesisHorizon.Medium => "medium"
      case HypothesisHorizon.Long   => "long"

  def getThreatWarning(bead: Int, kind: String, square: Option[String]): String = {
    val loc = square.map(s => s" on $s").getOrElse("")
    pick(bead, List(
      s"Watch out for the ${kind.toLowerCase}$loc.",
      s"Be alert to the ${kind.toLowerCase} threat$loc.",
      s"The opponent may threaten ${kind.toLowerCase}$loc."
    ))
  }

  def getPracticalVerdict(bead: Int, verdict: String): String =
    getPracticalVerdict(bead, verdict, cpWhite = 0)

  def getPracticalVerdict(bead: Int, verdict: String, cpWhite: Int, ply: Int = 0): String = {
    val advantageSide =
      if cpWhite >= 80 then Some("White")
      else if cpWhite <= -80 then Some("Black")
      else None
    val cycle = Math.floorMod(ply, 3)
    val localSeed = bead ^ (ply * 0x7f4a7c15)

    verdict match {
      case "Comfortable" =>
        advantageSide match
          case Some(side) =>
            val families = cycle match
              case 0 => List(
                s"From a practical standpoint, $side has the cleaner roadmap.",
                s"$side can follow the more straightforward practical plan.",
                s"$side has the more comfortable practical route here.",
                s"$side's practical choices are easier to execute with fewer risks."
              )
              case 1 => List(
                s"$side can press with a comparatively straightforward conversion scheme.",
                s"$side's strategic plan is easier to execute without tactical risk.",
                s"$side can improve with lower practical risk move by move.",
                s"$side can keep improving without forcing tactical concessions."
              )
              case _ => List(
                s"$side can play on intuition here.",
                s"$side can improve naturally without forcing complications.",
                s"$side can maintain pressure without overextending.",
                s"$side can progress with measured moves and minimal tactical exposure."
              )
            pick(localSeed, families)
          case None =>
            val families = cycle match
              case 0 => List(
                "In practical terms, handling this position is straightforward.",
                "The position is easier to navigate than it first appears.",
                "Practical handling is relatively direct for both sides.",
                "Both sides can follow understandable plans without immediate tactical chaos."
              )
              case 1 => List(
                "Plans are relatively clear for both players.",
                "There are no immediate tactical emergencies for either side.",
                "The position allows measured play without urgent tactical firefighting.",
                "Both sides can prioritize structure and coordination over tactics."
              )
              case _ => List(
                "This is a manageable position from a practical perspective.",
                "The position allows methodical play without forcing tactics.",
                "Practical decision-making is stable if move order stays accurate.",
                "Long-term plans matter more than tactical fireworks right now."
              )
            pick(localSeed, families)
      case "Under Pressure" =>
        val defenseSide = advantageSide.map(side => if side == "White" then "Black" else "White")
        defenseSide match
          case Some(side) =>
            val families = cycle match
              case 0 => List(
                s"$side has to defend accurately to stay afloat.",
                s"$side is under practical pressure and must be precise."
              )
              case 1 => List(
                s"The defensive burden falls on $side.",
                s"$side has less margin for passive play here."
              )
              case _ => List(
                s"$side needs concrete accuracy to avoid drifting worse.",
                s"$side must find precise moves to prevent a structural collapse."
              )
            pick(localSeed, families)
          case None =>
            val families = cycle match
              case 0 => List(
                "Precise defensive choices are needed to keep equality.",
                "The defensive burden is noticeable."
              )
              case 1 => List(
                "One tempo can change the evaluation quickly.",
                "It is easy to misstep if you relax.",
                "A small timing error can hand over practical control.",
                "A single inexact move can create immediate defensive burdens."
              )
              case _ => List(
                "A single tempo can swing the position.",
                "Defensive technique matters more than raw activity here.",
                "Defensive precision is more important than active-looking moves.",
                "Defensive accuracy is the main practical requirement in this phase."
              )
            pick(localSeed, families)
      case _ =>
        val families = cycle match
          case 0 => List(
            "The position remains dynamically balanced.",
            "Counterplay exists for both sides.",
            "The practical chances are still shared between both players.",
            "Neither side has converted small edges into a stable advantage."
          )
          case 1 => List(
            "Both sides retain tactical resources, so concrete move-order precision matters.",
            "The position stays tense, and one careless tempo can swing the initiative.",
            "Both players still need concrete accuracy before committing.",
            "Each side still has tactical resources that punish inaccurate move order."
          )
          case _ => List(
            "Neither side has stabilized a lasting edge.",
            "The margin is narrow enough that practical accuracy remains decisive.",
            "The game remains balanced, and precision will decide the result.",
            "Fine margins mean technical accuracy still determines practical outcomes."
          )
        pick(localSeed, families)
    }
  }

  def getAnnotationPositive(bead: Int, playedSan: String): String = {
    pick(bead, List(
      s"**$playedSan** keeps to the strongest continuation, so coordination remains intact.",
      s"**$playedSan** follows the principal engine roadmap, while preserving structural clarity.",
      s"**$playedSan** fits the position's strategic demands, and it limits tactical drift.",
      s"**$playedSan** preserves coordination and keeps the best practical structure, which eases conversion.",
      s"With **$playedSan**, the position stays aligned with the main plan, so tempo handling stays simple.",
      s"**$playedSan** is a reliable move that maintains the reference continuation, while king safety stays stable.",
      s"**$playedSan** keeps the technical roadmap compact and stable, and move-order risks stay manageable.",
      s"**$playedSan** keeps conversion tasks straightforward in practice, because structure and activity stay connected.",
      s"With **$playedSan**, practical conversion remains organized and manageable, so initiative does not drift.",
      s"**$playedSan** keeps long-term coordination intact while limiting tactical drift, which supports clean follow-up.",
      s"**$playedSan** steers the position into a stable plan with clear follow-up, and defensive duties remain light.",
      s"**$playedSan** keeps the game strategically tidy and easier to handle, even if the position stays tense.",
      s"**$playedSan** supports a controlled continuation with minimal structural risk, so practical choices remain clear.",
      s"**$playedSan** keeps the practical roadmap readable without forcing complications, while initiative remains balanced."
    ))
  }

  def getAnnotationNegative(bead: Int, playedSan: String, bestSan: String, cpLoss: Int): String = {
    val mark = Thresholds.annotationMark(cpLoss)
    val markedMove = if mark.nonEmpty then s"**$playedSan** $mark" else s"**$playedSan**"

    Thresholds.classifySeverity(cpLoss) match {
      case "blunder" =>
        pick(bead, List(
          s"$markedMove is a blunder; it allows a forcing sequence, so your position loses tactical control. **$bestSan** was required.",
          s"$markedMove is a decisive error, and as a result coordination collapses quickly. The critical move was **$bestSan**.",
          s"$markedMove is a blunder that hands over game flow, while **$bestSan** was the only stable route.",
          s"$markedMove is a blunder because it disconnects defense from counterplay; **$bestSan** was necessary.",
          s"$markedMove is a major error, so king safety and initiative both deteriorate; **$bestSan** held the balance."
        ))
      case "mistake" =>
        pick(bead, List(
          s"$markedMove is a clear mistake; it gives the opponent initiative, so practical defense becomes harder. Stronger is **$bestSan**.",
          s"$markedMove is a mistake that worsens coordination, while **$bestSan** keeps the structure easier to manage.",
          s"$markedMove is a serious mistake that shifts practical control, and **$bestSan** was the preferable route.",
          s"$markedMove is a mistake because it loosens tempo order; **$bestSan** keeps the position connected.",
          s"$markedMove is inaccurate in practical terms, so **$bestSan** was needed to preserve initiative."
        ))
      case "inaccuracy" =>
        pick(bead, List(
          s"$markedMove is an inaccuracy, and **$bestSan** keeps cleaner control of coordination.",
          s"$markedMove concedes practical ease, so **$bestSan** is the tighter continuation.",
          s"$markedMove drifts from the best plan, while **$bestSan** keeps initiative better anchored.",
          s"$markedMove is slightly loose in timing, and **$bestSan** keeps the structure easier to handle.",
          s"$markedMove gives up some practical control; therefore **$bestSan** is the cleaner route."
        ))
      case _ =>
        pick(bead, List(
          s"$markedMove is slightly imprecise, while **$bestSan** is cleaner in practical terms.",
          s"$markedMove is playable but second-best, and **$bestSan** keeps the position simpler to convert.",
          s"$markedMove is not the top choice here, so **$bestSan** remains the reference move.",
          s"$markedMove can be played, but **$bestSan** keeps better structure and coordination.",
          s"$markedMove is acceptable yet less direct, whereas **$bestSan** keeps initiative more stable."
        ))
    }
  }

  def getEngineRankContext(bead: Int, rank: Option[Int], bestSan: String, cpLoss: Int = 0): Option[String] =
    rank match {
      case Some(2) if cpLoss <= 35 =>
        Some(pick(bead, List(
          s"Engine preference still leans to **$bestSan**, but the practical gap is modest and coordination themes stay similar.",
          s"The engine line order is close here; **$bestSan** is still cleaner, while structure remains comparable.",
          s"By score order, **$bestSan** remains first, although the practical difference stays small."
        )))
      case Some(2) =>
        Some(pick(bead, List(
          s"Engine preference still leans to **$bestSan** as the cleaner move-order, so initiative handling is easier.",
          s"In engine ordering, **$bestSan** remains first, while this line requires tighter coordination.",
          s"The top engine continuation starts with **$bestSan**, because it keeps the structure more resilient."
        )))
      case Some(r) if r >= 3 && cpLoss <= 35 =>
        Some(pick(bead, List(
          s"Engine ordering is still close; **$bestSan** remains the cleanest reference line, while this stays playable.",
          s"This line is playable, but **$bestSan** keeps a cleaner move-order and easier conversion path.",
          s"Score ordering still prefers **$bestSan**, though the practical gap stays small and strategic plans overlap."
        )))
      case Some(r) if r >= 3 =>
        Some(pick(bead, List(
          s"This sits below the principal engine candidates, so **$bestSan** gives the more reliable setup.",
          s"Engine ranking puts this in a lower tier, while **$bestSan** keeps tighter control of initiative.",
          s"This is outside the top engine choices, and **$bestSan** remains the stable reference for conversion."
        )))
      case None if cpLoss <= 35 =>
        Some(pick(bead, List(
          s"This move was not in the sampled MultiPV set, so **$bestSan** remains the engine reference.",
          s"The sampled principal lines still favor **$bestSan**, with only a modest practical difference in coordination.",
          s"From sampled engine lines, **$bestSan** remains the cleaner benchmark for strategic stability."
        )))
      case None =>
        Some(pick(bead, List(
          s"This move is outside sampled principal lines, and **$bestSan** is the engine reference for safer conversion.",
          s"The move is not in the main MultiPV set, so **$bestSan** is the robust practical alternative.",
          s"Sampled principal lines do not prioritize this, while **$bestSan** remains the practical benchmark."
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
    val m = motif.trim
    if (m.isEmpty) ""
    else
      pick(bead, List(
        s"The $m motif fades from the position.",
        s"One theme disappears: $m.",
        s"The immediate idea of $m no longer applies."
      ))
  }

  private val motifPrefixSignals: Set[String] = Set(
    "bad_bishop",
    "battery",
    "bishop_pair",
    "blockade",
    "color_complex",
    "connected_rooks",
    "deflection",
    "domination",
    "doubled_rooks",
    "exchange_sacrifice",
    "good_bishop",
    "greek_gift",
    "hanging_pawns",
    "interference",
    "iqp",
    "isolated_pawn",
    "king_cut_off",
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
    "rook_behind_passed_pawn",
    "rook_lift",
    "rook_on_seventh",
    "semi_open_file_control",
    "simplification",
    "simplify",
    "skewer",
    "skewer_queen",
    "smothered_mate",
    "stalemate",
    "stalemate_trap",
    "stalemate_trick",
    "trapped_piece",
    "trapped_piece_queen",
    "underpromotion",
    "xray",
    "xray_queen",
    "zugzwang",
    "zwischenzug"
  )

  def isMotifPrefixSignal(rawMotif: String): Boolean =
    val normalized = normalizeMotifTag(rawMotif)
    normalized.nonEmpty && motifPrefixSignals.exists(sig => motifMatches(normalized, sig))

  def getMotifPrefix(bead: Int, motifs: List[String], ply: Int = 0): Option[String] = {
    val normalized = motifs.map(normalizeMotifTag).filter(_.nonEmpty)
    def hasAny(keys: String*): Boolean =
      keys.exists(k => normalized.exists(m => motifMatches(m, k)))

    val templates =
      if (hasAny("greek_gift")) Some(List(
        "A classic Greek Gift sacrifice is in the air.",
        "The setup hints at a Greek Gift pattern.",
        "The bishop sacrifice motif on h7/h2 is becoming relevant."
      ))
      else if (hasAny("smothered_mate")) Some(List(
        "A potential smothered mate pattern is forming.",
        "The geometry of a smothered mate is starting to appear.",
        "Knight-and-queen coordination points toward smothered mate ideas."
      ))
      else if (hasAny("zugzwang")) Some(List(
        "Zugzwang ideas are central: useful moves are running out.",
        "This has a zugzwang flavor where every move concedes something.",
        "The key endgame issue is zugzwang: improving moves are scarce."
      ))
      else if (hasAny("isolated_pawn", "iqp")) Some(List(
        "The isolated-queen-pawn structure is shaping the plans.",
        "The IQP defines the strategic battle here.",
        "Play revolves around the strengths and weaknesses of the isolated pawn."
      ))
      else if (hasAny("hanging_pawns") && !hasAny("passed_pawn") && !hasAny("bad_bishop")) Some(List(
        "The hanging pawns in the center are a major strategic factor.",
        "Central hanging pawns keep the position tense.",
        "Managing the hanging pawns will decide the middlegame plans."
      ))
      else if (hasAny("minority_attack") && !(hasAny("bad_bishop") && ply >= 30) && !hasAny("liquidate", "liquidation")) Some(List(
        "A minority attack structure is emerging on the queenside.",
        "Queenside minority attack ideas are now practical.",
        "The queenside minority attack is becoming a concrete lever."
      ))
      else if (hasAny("opposite_bishops")) Some(List(
        "Opposite-colored bishops sharpen attacking chances.",
        "With opposite-colored bishops, king attacks gain practical value.",
        "Opposite-colored bishops increase both drawing and attacking resources."
      ))
      else if (hasAny("underpromotion")) Some(List(
        "An underpromotion resource is becoming relevant.",
        "Underpromotion choices may be non-standard here.",
        "A rare underpromotion idea appears in the position."
      ))
      else if (hasAny("stalemate", "stalemate_trick", "stalemate_trap")) Some(List(
        "A stalemate trick is part of the defensive resources.",
        "Stalemate motifs complicate straightforward conversion.",
        "The defender has potential stalemate-based counterplay."
      ))
      else if (hasAny("prophylaxis", "prophylactic")) Some(List(
        "A prophylactic idea is a key theme in this position.",
        "A prophylactic move to restrict counterplay is the central task.",
        "The strongest plan starts with a prophylactic preventive move."
      ))
      else if (hasAny("interference")) Some(List(
        "Interference motifs are cutting defensive coordination.",
        "A tactical interference idea is shaping move order.",
        "Interference on key lines is now the tactical backbone."
      ))
      else if (hasAny("deflection")) Some(List(
        "Deflection ideas are pulling defenders off key squares.",
        "A deflection motif is dictating tactical priorities.",
        "The tactical battle revolves around a key deflection."
      ))
      else if (hasAny("rook_lift")) Some(List(
        "A rook lift idea can accelerate the attack.",
        "Rook lift geometry is becoming available.",
        "The rook can swing into action via a lift."
      ))
      else if (hasAny("good_bishop")) Some(List(
        "A good bishop is becoming a strong strategic asset.",
        "Activating the good bishop can improve long-range pressure.",
        "The good bishop has unobstructed diagonals and lasting influence."
      ))
      else if (hasAny("bishop_pair")) Some(List(
        "The bishop pair is a long-term strategic asset.",
        "Open-board dynamics favor the bishop pair.",
        "The bishop pair increases pressure across both wings."
      ))
      else if (hasAny("passed_pawn") && !hasAny("rook_behind_passed_pawn")) Some(List(
        "The passed pawn is now a central practical factor.",
        "Passed pawn dynamics are starting to dominate plans.",
        "Containing the passed pawn is becoming urgent."
      ))
      else if (hasAny("bad_bishop")) Some(List(
        "A bad bishop problem is limiting piece quality.",
        "The bad bishop is restricted by its own pawn chain.",
        "The bad bishop's scope is a strategic weakness here."
      ))
      else if (hasAny("knight_domination")) Some(List(
        "A knight-domination pattern is emerging.",
        "The knight is outperforming its counterpart.",
        "Outpost control gives the knight a stable edge."
      ))
      else if (hasAny("battery")) Some(List(
        "A battery alignment increases tactical pressure.",
        "Line-piece battery coordination is becoming dangerous.",
        "The battery motif is shaping immediate threats."
      ))
      else if (hasAny("simplification", "simplify")) Some(List(
        "Simplification choices now define the practical result.",
        "Trade decisions and simplification are steering the game toward a technical phase.",
        "The position is entering a conversion-through-simplification stage."
      ))
      else if (hasAny("liquidate", "liquidation", "pawn_break")) Some(List(
        "Central liquidation is changing the position's character.",
        "A central liquidation sequence is redefining strategic priorities.",
        "The structure is about to shift through liquidation."
      ))
      else if (hasAny("zwischenzug")) Some(List(
        "Intermediate-move (zwischenzug) resources are available.",
        "A zwischenzug can reverse move-order assumptions.",
        "The tactical point is finding the in-between move."
      ))
      else if (hasAny("trapped_piece_queen")) Some(List(
        "A trapped queen motif is now relevant.",
        "The queen is close to becoming strategically trapped.",
        "Trapping the queen is becoming a concrete tactical idea."
      ))
      else if (hasAny("trapped_piece")) Some(List(
        "A trapped piece motif is now relevant.",
        "Piece mobility is restricted enough to create trapping ideas.",
        "One unit is close to becoming strategically trapped."
      ))
      else if (hasAny("king_hunt")) Some(List(
        "A king hunt scenario is developing.",
        "King safety has become the primary tactical axis of a king hunt.",
        "The attack can escalate into a direct king hunt."
      ))
      else if (hasAny("pawn_storm")) Some(List(
        "A pawn storm structure is taking shape on the flank.",
        "Pawn storm timing is becoming the central attacking question.",
        "Flank pawn storms are now driving the initiative battle."
      ))
      else if (hasAny("repetition", "repeat")) Some(List(
        "Repeat ideas are now part of the practical decision tree.",
        "The position allows a repeat if neither side commits.",
        "Draw-by-repeat resources are becoming relevant."
      ))
      else if (hasAny("novelty")) Some(List(
        "An opening novelty has changed the expected plans.",
        "This move carries novelty value compared with mainline play.",
        "The game has left familiar theory with a fresh novelty."
      ))
      else if (hasAny("semi_open_file_control")) Some(List(
        "Semi-open file pressure is now the key strategic lever.",
        "Control of the semi-open file is becoming the central plan.",
        "The semi-open file is the main channel for rook activity."
      ))
      else if (hasAny("rook_on_seventh")) Some(List(
        "Rook activity on the seventh rank is becoming practical.",
        "Seventh-rank invasion ideas are now central.",
        "A rook on the seventh rank could decide the technical battle."
      ))
      else if (hasAny("rook_behind_passed_pawn")) Some(List(
        "Rook placement behind the passed pawn is the technical priority.",
        "The key endgame principle is keeping the rook behind the passed pawn.",
        "Behind-the-passed-pawn rook geometry is becoming decisive."
      ))
      else if (hasAny("king_cut_off")) Some(List(
        "Cutting off the enemy king is the key endgame method.",
        "King cut-off geometry is becoming the core technical idea.",
        "The conversion plan revolves around restricting the enemy king."
      ))
      else if (hasAny("doubled_rooks")) Some(List(
        "Doubled rooks can generate immediate file pressure.",
        "The doubled-rooks setup is becoming the main attacking structure.",
        "Stacking rooks on one file is the practical plan."
      ))
      else if (hasAny("connected_rooks")) Some(List(
        "Connected rooks improve coordination for both attack and defense.",
        "Connecting rooks is now a key positional milestone.",
        "Linking connected rooks makes file-control plans easier to execute."
      ))
      else if (hasAny("maneuver")) Some(List(
        "A rerouting maneuver is now the practical plan.",
        "Piece transfer to a better square is the main idea.",
        "A switch in piece placement can improve control."
      ))
      else if (hasAny("domination")) Some(List(
        "A domination pattern is emerging around key squares.",
        "One side is starting to dominate a critical piece route.",
        "Positional domination is becoming a stable long-term edge."
      ))
      else if (hasAny("knight_vs_bishop")) Some(List(
        "The knight-versus-bishop balance now defines the strategic fight.",
        "This structure highlights a direct knight-and-bishop contrast.",
        "Closed/open square dynamics will decide whether the knight or bishop thrives."
      ))
      else if (hasAny("blockade")) Some(List(
        "Blockade technique is central to the conversion plan.",
        "Stopping the passed pawn with a blockade is the key task.",
        "A stable blockade can neutralize the opponent's main counterplay."
      ))
      else if (hasAny("pin_queen")) Some(List(
        "A pin against the queen is a tactical resource in the position.",
        "Queen pin geometry is becoming a practical motif.",
        "The tactical point is creating pressure through a queen pin."
      ))
      else if (hasAny("pin")) Some(List(
        "Pin geometry is becoming a concrete tactical factor.",
        "A pin motif is shaping move-order choices.",
        "The position features tactical pressure through a pin."
      ))
      else if (hasAny("skewer_queen")) Some(List(
        "A skewer against the queen is becoming a concrete tactical idea.",
        "Queen-skewer geometry is now part of the calculation tree.",
        "Skewering the queen is an active tactical theme in the position."
      ))
      else if (hasAny("skewer")) Some(List(
        "A skewer motif is now relevant in the tactical battle.",
        "Line-based skewer ideas are shaping move order.",
        "Skewer geometry is part of the current tactical landscape."
      ))
      else if (hasAny("xray_queen")) Some(List(
        "X-ray pressure toward the queen is becoming practical.",
        "The tactical idea is x-ray pressure on the queen.",
        "X-ray alignment against the queen is now a concrete resource."
      ))
      else if (hasAny("xray")) Some(List(
        "X-ray pressure is becoming a practical tactical resource.",
        "Line-piece x-ray geometry now influences move order.",
        "X-ray motifs are part of the current tactical landscape."
      ))
      else if (hasAny("exchange_sacrifice")) Some(List(
        "An exchange-sacrifice idea can create dominant file control.",
        "Sacrificing the exchange for initiative is now a practical option.",
        "Exchange investment for dominant activity is part of the position."
      ))
      else if (hasAny("color_complex")) Some(List(
        "Color complex control is the key strategic battleground.",
        "The struggle revolves around weak color complex squares.",
        "Color complex imbalances are guiding both plans."
      ))
      else if (hasAny("open_file")) Some(List(
        "Open-file control is a central strategic objective.",
        "The open file is the key channel for major-piece activity.",
        "File control along the open line can dictate the middlegame."
      ))
      else None

    templates.map(ts => punctuate(pickWithPlyRotation(bead ^ 0x3c6ef372, ply, ts)))
  }

  def getThreatStatement(bead: Int, kind: String, loss: Int): String = {
    val severity = if (loss >= 300) "urgent" else if (loss >= 100) "serious" else "real"
    val k = kind.trim.toLowerCase
    val kindLabel = k match
      case "mate"       => "mate threat"
      case "material"   => "material threat"
      case "positional" => "positional squeeze"
      case _            => s"$k threat"
    val consequence = k match
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

    pick(bead, List(
      s"A $severity $kindLabel is on the board; $consequence",
      s"The $kindLabel requires immediate attention; $consequence",
      s"Handling the $kindLabel is the priority here; $consequence"
    ))
  }

  def getPlanStatement(bead: Int, planName: String, ply: Int = 0): String = {
    val localSeed = bead ^ (ply * 0x9e3779b9)
    val p = planDisplay(localSeed, planName.trim, ply)
    if p.isEmpty then ""
    else {
      val cycle = Math.floorMod(ply, 4)
      val templates = cycle match
        case 0 => List(
          s"Key theme: **$p**.",
          s"Strategic focus: **$p**."
        )
        case 1 => List(
          s"Current play is organized around **$p**.",
          s"The most reliable roadmap here is built around **$p**."
        )
        case 2 => List(
          s"Strategic priority: **$p**.",
          s"The practical roadmap centers on **$p**."
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
      withApprox("White is winning"),
      withApprox("White has a decisive advantage"),
      withApprox("White is completely on top"),
      withApprox("White should convert with correct play"),
      withApprox("White is close to a winning position")
    ))
    else if (cp >= 300) choose(List(
      withApprox("White holds a clear advantage"),
      withApprox("White is clearly better"),
      withApprox("White is pressing with a stable edge"),
      withApprox("White has the initiative and the better game"),
      withApprox("White has a comfortable plus")
    ))
    else if (cp >= 100) choose(List(
      withApprox("White has a slight advantage"),
      withApprox("White is a bit better"),
      withApprox("White has the more pleasant position"),
      withApprox("White can play for two results"),
      withApprox("White is slightly ahead in the evaluation")
    ))
    else if (cp >= 30) choose(List(
      withApprox("White has a small pull"),
      withApprox("White is just a touch better"),
      withApprox("White has a modest edge"),
      withApprox("White can keep up mild pressure"),
      withApprox("White has the easier side to press with")
    ))
    else if (cp <= -500) choose(List(
      withApprox("Black is winning"),
      withApprox("Black has a decisive advantage"),
      withApprox("Black is completely on top"),
      withApprox("Black should convert with correct play"),
      withApprox("Black is close to a winning position")
    ))
    else if (cp <= -300) choose(List(
      withApprox("Black holds a clear advantage"),
      withApprox("Black is clearly better"),
      withApprox("Black is pressing with a stable edge"),
      withApprox("Black has the initiative and the better game"),
      withApprox("Black has a comfortable plus")
    ))
    else if (cp <= -100) choose(List(
      withApprox("Black has a slight advantage"),
      withApprox("Black is a bit better"),
      withApprox("Black has the more pleasant position"),
      withApprox("Black can play for two results"),
      withApprox("Black is slightly ahead in the evaluation")
    ))
    else if (cp <= -30) choose(List(
      withApprox("Black has a small pull"),
      withApprox("Black is just a touch better"),
      withApprox("Black has a modest edge"),
      withApprox("Black can keep up mild pressure"),
      withApprox("Black has the easier side to press with")
    ))
    else choose(List(
      "The position is finely balanced",
      "The position is about level",
      "Neither side has established a clear advantage",
      "The evaluation is essentially balanced",
      "It's close to equal, with play for both sides",
      "The position is practically balanced with chances for both sides"
    ))
  }

  // ===========================================================================
  // PHASE 6.8: DENSITY INJECTION SUPPORT
  // ===========================================================================

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

  private def roleLabel(role: Role): String =
    role match
      case Pawn   => "pawn"
      case Knight => "knight"
      case Bishop => "bishop"
      case Rook   => "rook"
      case Queen  => "queen"
      case King   => "king"

  def getFactStatement(bead: Int, fact: Fact): String =
    fact match
      case Fact.HangingPiece(square, role, attackers, defenders, _) =>
        val a = attackers.size
        val d = defenders.size
        def plural(n: Int, one: String, many: String): String = if n == 1 then one else many
        val aText = if a == 0 then "no attackers" else s"$a ${plural(a, "attacker", "attackers")}"
        val dText = if d == 0 then "no defenders" else s"$d ${plural(d, "defender", "defenders")}"
        val balance = if d == 0 then s"$aText, $dText" else s"$aText vs $dText"
        pick(bead, List(
          s"The ${roleLabel(role)} on ${square.key} is hanging ($balance).",
          s"The ${roleLabel(role)} on ${square.key} is underdefended: $balance.",
          s"Keep an eye on the ${roleLabel(role)} on ${square.key} — $balance."
        ))

      case Fact.Pin(_, _, pinned, pinnedRole, behind, behindRole, isAbsolute, _) =>
        val abs = if isAbsolute then " (absolute)" else ""
        pick(bead, List(
          s"The ${roleLabel(pinnedRole)} on ${pinned.key} is pinned$abs to the ${roleLabel(behindRole)} on ${behind.key}.",
          s"There's a pin: the ${roleLabel(pinnedRole)} on ${pinned.key} cannot move without exposing the ${roleLabel(behindRole)} on ${behind.key}.",
          s"${pinned.key} is pinned, leaving the ${roleLabel(pinnedRole)} with limited mobility.",
          s"The pin on ${pinned.key} slows coordination of that ${roleLabel(pinnedRole)}, costing valuable tempi.",
          s"The pin restrains the ${roleLabel(pinnedRole)} on ${pinned.key}, reducing practical flexibility."
        ))

      case Fact.Fork(attacker, attackerRole, targets, _) =>
        val targetText =
          targets.take(2).map { case (sq, r) => s"${roleLabel(r)} on ${sq.key}" } match
            case a :: b :: Nil => s"$a and $b"
            case a :: Nil      => a
            case _             => "multiple targets"
        pick(bead, List(
          s"The ${roleLabel(attackerRole)} on ${attacker.key} has a fork idea against $targetText.",
          s"Watch for a fork by the ${roleLabel(attackerRole)} on ${attacker.key} hitting $targetText.",
          s"A fork motif is in the air: ${attacker.key} can attack $targetText."
        ))

      case Fact.WeakSquare(square, color, reason, _) =>
        val owner = color.name.toLowerCase
        val why = reason.trim
        val detail = if why.nonEmpty then s" ($why)" else ""
        pick(bead, List(
          s"${square.key} is a weak square for $owner$detail.",
          s"The square ${square.key} looks vulnerable$detail.",
          s"A potential outpost on ${square.key} appears$detail."
        ))

      case Fact.Outpost(square, role, _) =>
        pick(bead, List(
          s"${square.key} can serve as an outpost for a ${roleLabel(role)}.",
          s"An outpost on ${square.key} could be valuable for a ${roleLabel(role)}.",
          s"Keep ${square.key} in mind as an outpost square."
        ))

      case Fact.Opposition(_, _, _, isDirect, _) =>
        val kind = if isDirect then "direct opposition" else "opposition"
        pick(bead, List(
          s"The kings are in $kind.",
          s"$kind is an important endgame detail.",
          s"King opposition becomes a key factor."
        ))

      case Fact.KingActivity(square, mobility, _, _) =>
        pick(bead, List(
          s"The king on ${square.key} is active (mobility: $mobility).",
          s"King activity matters: ${square.key} has $mobility safe steps.",
          s"The king on ${square.key} is well-placed for the endgame."
        ))

      case _ => ""

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
