package lila.llm.analysis

import scala.util.Random
import chess.Role
import chess.Queen

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

  def intent(bead: Int, move: String, plan: String, style: Style = Style.Book): String = {
    val templates = style match {
      case Style.Dramatic => Seq(
        s"$move! A bold choice aiming to $plan.",
        s"With $move, the intention is clear: $plan.",
        s"$move sets the stage for $plan."
      )
      case _ => Seq(
        s"$move aims for $plan.",
        s"$move, with the idea of $plan.",
        s"The goal of $move is $plan.",
        s"$move seeks $plan.",
        s"$move is driven by $plan."
      )
    }
    pick(bead, templates)
  }

  def scoreDiffAdjective(bead: Int, scoreDiff: Int): String = {
    val absDiff = scoreDiff.abs
    val adj = if (absDiff > 500) Seq("crushing", "decisive", "hopeless", "devastating")
              else if (absDiff > 200) Seq("significant", "clear", "dangerous", "unpleasant")
              else if (absDiff > 50) Seq("slight", "noticeable", "nagging", "tangible")
              else Seq("minimal", "negligible", "unclear", "balanced")
    pick(bead, adj)
  }

  def intro(bead: Int, nature: String, tension: Double, style: Style = Style.Book): String = {
    val templates = style match {
      case Style.Coach => Seq(
        s"Let's pause and assess. The position is $nature, and the tension is ${"%.1f".format(tension)}. What would you do?",
        s"A key moment to learn about $nature positions (tension: ${"%.1f".format(tension)}).",
        s"Study the pawn structure carefully. It dictates this $nature game.",
        s"Mastery requires handling these $nature positions with care.",
        s"The tension is ${"%.1f".format(tension)}. In such $nature positions, precision is everything.",
        s"Take a deep breath. It's a $nature grind."
      )
      case Style.Dramatic => Seq(
        s"The board is ablaze! A $nature chaos with high tension (${"%.1f".format(tension)})!",
        s"Tension spikes to ${"%.1f".format(tension)} in this $nature thriller.",
        s"A clash of titans in a $nature battlefield.",
        s"No room for error in this high-octane $nature position!",
        s"Pure adrenaline. The tension is palpable at ${"%.1f".format(tension)}.",
        s"This isn't just a game; it's a $nature war.",
        s"Chaos reigns in this $nature spectacle!"
      )
      case _ => Seq(
        s"The position is fundamentally $nature (tension: ${"%.1f".format(tension)}).",
        s"We find ourselves in a $nature landscape, with a tension index of ${"%.1f".format(tension)}.",
        s"The strategic character is defined by its $nature nature.",
        s"Balancing the $nature elements is the current priority.",
        s"From a positional standpoint, this is $nature.",
        s"The tension metric (${"%.1f".format(tension)}) confirms a $nature dynamic.",
        s"We have reached a $nature middlegame phase."
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
        s"$move seems natural, but $reply is a killer blow ($outcome)!",
        s"Disaster strikes after $move? No, $reply turns the tables!",
        s"$move falls into a trap: $reply seals the deal ($outcome).",
        s"A shocking twist! $move is met by $reply ($outcome)."
      )
      case Style.Coach => Seq(
        s"If $move, consider $reply. What is the result? $outcome.",
        s"Calculate $move... do you see $reply? It leads to $outcome.",
        s"$move is a mistake due to $reply ($outcome). Always check opponent's replies.",
        s"Why not $move? Because $reply creates a $outcome."
      )
      case _ => Seq(
        s"$move is refuted by $reply, leading to a $outcome.",
        s"The problem with $move is the reply $reply ($outcome).",
        s"After $move, $reply demonstrates the error ($outcome).",
        s"Analysis shows $move fails to $reply ($outcome)."
      )
    }
    pick(bead, templates)
  }

  // ===========================================================================
  // 1. OPENING / CONTEXT SETTERS
  // ===========================================================================
  
  def getOpening(bead: Int, phase: String, evalText: String): String = {
    val p = phase.toLowerCase
    val templates = (p, evalText) match {
      case ("opening", _) => List(
          s"We are in the opening phase. $evalText.",
          s"The game begins. $evalText.",
          s"Early in the game, $evalText.",
          s"Opening play continues. $evalText.",
          s"In the opening stage, $evalText."
      )
      case ("middlegame", _) => List(
          s"Entering the middlegame. $evalText.",
          s"As we transition to the middlegame, $evalText.",
          s"The middlegame battle is underway. $evalText.",
          s"With the middlegame in full swing, $evalText.",
          s"Middlegame complexities arise. $evalText.",
          s"We find ourselves in the middlegame. $evalText.",
          s"The position has evolved into a complex middlegame. $evalText."
      )
      case ("endgame", _) => List(
          s"The game is now in the endgame. $evalText.",
          s"We have reached an endgame. $evalText.",
          s"The endgame phase begins. $evalText.",
          s"In this endgame, $evalText.",
          s"The board simplifies into an endgame. $evalText.",
          s"Endgame technique will be key. $evalText."
      )
      case _ => List(
          s"The position is $evalText.",
          s"Reflecting on the board, it is $evalText.",
          s"Analysis shows a $evalText game."
      )
    }
    pick(bead, templates)
  }

  def getEndgameContext(bead: Int, context: String, evalText: String): String = {
    val templates = context.toLowerCase match {
      case s if s.contains("pawn race") => List(
          s"A sharp pawn race unfolds. $evalText.",
          s"Both sides race to promote. $evalText.",
          s"It comes down to a race of passed pawns. $evalText."
      )
      case s if s.contains("zugzwang") => List(
          s"The opponent is in zugzwang. $evalText.",
          s"A textbook zugzwang position. $evalText.",
          s"Black is running out of useful moves. $evalText."
      )
      case s if s.contains("outside passed pawn") => List(
          s"The outside passed pawn is decisive. $evalText.",
          s"White creates a decisive outside passed pawn. $evalText.",
          s"The distant passed pawn ties down the opponent. $evalText."
      )
       case s if s.contains("wrong bishop") => List(
          s"A theoretical draw due to the wrong bishop. $evalText.",
          s"Despite the material advantage, it's a wrong bishop draw. $evalText.",
          s"The bishop cannot control the promotion square. $evalText."
      )
      case _ => List(
          s"Endgame technique will be key. $evalText."
      )
    }
    pick(bead, templates)
  }

  def getOpposition(bead: Int, distance: Int, isDirect: Boolean): String = {
    if (isDirect) {
      pick(bead, List(
        "securing direct opposition",
        "taking direct opposition",
        "winning the opposition",
        "dominating through direct opposition"
      ))
    } else {
      pick(bead, List(
        "gaining distant opposition",
        "maintaining distant opposition",
        "jockeying for opposition",
        "controlling the long-range opposition"
      ))
    }
  }

  def getKingActivity(bead: Int, mobility: Int): String = {
    if (mobility >= 5) {
      pick(bead, List(
        "activating the king for the final phase",
        "bringing the king into the battle",
        "centralizing the king decisively",
        "marching the king forward"
      ))
    } else {
      pick(bead, List(
        "improving king activity",
        "gradually activating the king",
        "nudging the king toward the center",
        "beginning the king's central march"
      ))
    }
  }

  def getZugzwang(bead: Int): String = {
    pick(bead, List(
      "ZUGZWANG! Any move worsens the position",
      "caught in a squeeze, with no safe moves",
      "suffering from a total lack of constructive options",
      "in zugzwang, where inactivity is the only move"
    ))
  }

  def getPawnPromotion(bead: Int, role: Option[Role]): String = {
    role match {
      case Some(Queen) => pick(bead, List("pushing for a new queen", "threatening immediate promotion", "marching toward glory"))
      case Some(_) => pick(bead, List("preparing an underpromotion", "pushing the passed pawn"))
      case None => pick(bead, List("advancing the passed pawn", "creating immediate promotion threats"))
    }
  }

  def getDoubleCheck(bead: Int): String = {
    pick(bead, List(
      "unleashing a devastating double check",
      "firing from two sides at once",
      "creating a double check that paralyzes the defense"
    ))
  }

  def getStalemate(bead: Int): String = {
    pick(bead, List(
      "looking for a desperate stalemate",
      "setting a stalemate trap",
      "clinging to hope through stalemate"
    ))
  }

  // Phase 24: High-Level Strategic Concepts
  def getPawnStorm(bead: Int, flank: String): String = {
    pick(bead, List(
      s"A pawn storm on the $flank forces the opponent to react.",
      s"The rolling $flank pawn majority creates serious cramping issues.",
      s"An aggressive pawn expansion on the $flank dictates play."
    ))
  }

  def getHangingPawns(bead: Int, squares: String): String = {
    pick(bead, List(
      s"The hanging pawns on $squares require constant vigilance.",
      s"While the hanging pawns control space, they remain potential targets.",
      s"The dynamic hanging pawn duo offers both attacking chances and structural liability."
    ))
  }



  def getMinorityAttack(bead: Int, flank: String): String = {
    pick(bead, List(
      s"The minority attack on the $flank seeks to create structural weaknesses.",
      s"A minority attack is underway on the $flank to undermine the pawn chain.",
      s"Pressuring the $flank via a minority attack is the correct long-term plan."
    ))
  }

  // Phase 25: Knight Motifs
  def getDomination(bead: Int, domPiece: Role, victim: Role): String = {
    pick(bead, List(
      s"completely dominating the ${victim.name} with the ${domPiece.name}",
      s"paralyzing the ${victim.name} via the ${domPiece.name}",
      s"exerting a crushing grip on the ${victim.name} with the ${domPiece.name}"
    ))
  }

  def getManeuver(bead: Int, piece: Role, purpose: String): String = {
    purpose match {
      case "rerouting" => pick(bead, List(
        s"rerouting the ${piece.name} to a more active square",
        s"transferring the ${piece.name} to a better circuit",
        s"switching the ${piece.name} to a more promising diagonal or outpost"
      ))
      case _ => pick(bead, List(
        s"improving the scope of the ${piece.name}",
        s"optimizing the ${piece.name}'s placement",
        s"finding a superior post for the ${piece.name}"
      ))
    }
  }
  
  def getTrappedPiece(bead: Int, piece: Role): String = {
    pick(bead, List(
      s"trapping the ${piece.name} with no escape",
      s"exploiting the trapped ${piece.name}",
      s"entombing the ${piece.name} with no safe moves"
    ))
  }

  def getKnightVsBishop(bead: Int, color: chess.Color, isKnightBetter: Boolean): String = {
    if (isKnightBetter) pick(bead, List(
      s"favoring ${color}'s knight in this closed structure",
      "exploiting the superior knight against the bad bishop",
      "dominating with the knight on strong outposts"
    )) else pick(bead, List(
      "leveraging the bishop's long-range power",
      "utilizing the bishop in the open position",
      "overpowering the knight with the bishop pair"
    ))
  } 

  def getBlockade(bead: Int): String = pick(bead, List(
    "establishing a blockade against the passed pawn",
    "stopping the dangerous passer in its tracks",
    "using the knight as an ideal blockader"
  ))

  def getSmotheredMate(bead: Int): String = pick(bead, List(
    "delivering a classic smothered mate",
    "suffocating the king for a spectacular finish",
    "executing the rare and beautiful smothered mate"
  )) // End Phase 26

  // Phase 27: Bishop Motifs
  def getPin(bead: Int, pinned: Role, behind: Role): String = {
    pick(bead, List(
      s"pinning the ${pinned.name} to the ${behind.name}",
      s"paralyzing the ${pinned.name} with a pin against the ${behind.name}",
      s"establishing a troublesome pin on the ${pinned.name}"
    ))
  }

  def getSkewer(bead: Int, front: Role, back: Role): String = {
    pick(bead, List(
      s"skewering the ${front.name} and ${back.name}",
      s"firing through the ${front.name} to strike the ${back.name}",
      s"executing a powerful skewer across the diagonal"
    ))
  }

  def getXRay(bead: Int, target: Role): String = {
    pick(bead, List(
      s"exerting x-ray pressure on the ${target.name}",
      s"aiming an x-ray attack through the defense toward the ${target.name}",
      s"peering through the blockers to target the ${target.name}"
    ))
  }

  def getBattery(bead: Int, front: Role, back: Role): String = {
    pick(bead, List(
      s"forming a powerful ${front.name} and ${back.name} battery",
      s"aligning pieces into a dangerous battery",
      s"backing up the ${front.name} with the ${back.name}"
    ))
  }

  def getBishopThemes(bead: Int, theme: String): String = theme match {
    case "GoodBishop" => pick(bead, List(
      "activating the strong bishop",
      "leveraging the power of the good bishop",
      "dominating with the unobstructed bishop"
    ))
    case "BadBishop" => pick(bead, List(
      "struggling with the bad bishop",
      "dealing with the restricted mobility of the bishop",
      "limiting the scope of the blocked bishop"
    ))
    case "BishopPair" => pick(bead, List(
      "leveraging the bishop pair advantage",
      "exerting long-range pressure with the bishop pair",
      "winning with the superior coordination of the two bishops"
    ))
    case "OppositeColorBishops" => pick(bead, List(
      "navigating the complexities of opposite-colored bishops",
      "attacking on the weak color complexes in and endgames with opposite-colored bishops",
      "heading toward a draw with opposite-colored bishops"
    ))
    case "ColorComplex" => pick(bead, List(
      "dominating the weakened color complex",
      "exploiting the holes in the enemy's color structure",
      "controlling the critical light or dark squares"
    ))
    case _ => ""
  }

  def getRookThemes(bead: Int, theme: String, extra: String = ""): String = theme match {
    case "SemiOpenFileControl" => pick(bead, List(
      s"applying pressure along the semi-open $extra-file",
      s"occupying the semi-open $extra-file to exert long-term pressure",
      s"placing the rook on the semi-open $extra-file for better activity"
    ))
    case "SeventhRankInvasion" => pick(bead, List(
      "invading the seventh rank with the rook",
      "poking into the enemy's seventh rank",
      "establishing a rook on the seventh rank to paralyze the defense",
      "creating a 'rook on the seventh' situation that is technically winning"
    ))
    case "RookBehindPassedPawn" => pick(bead, List(
      s"placing the rook behind the passed $extra-pawn (Tarrasch rule)",
      s"supporting the passed $extra-pawn from behind",
      s"following the principle of placing rooks behind passed pawns"
    ))
    case "KingCutOff" => pick(bead, List(
      s"cutting off the enemy king along the $extra",
      s"restricting the enemy king's movement via the $extra",
      s"controlling the $extra to keep the enemy king trapped"
    ))
    case "OpenFile" => pick(bead, List(
      s"placing the rook on the open $extra-file for better activity",
      s"applying pressure along the open $extra-file",
      s"controlling the open $extra-file to restrict the opponent"
    ))
    case "DoubledRooks" => pick(bead, List(
      "doubling the rooks for massive firepower",
      "stacking the rooks on the file to force a breakthrough",
      "creating a powerful battery of doubled rooks"
    ))
    case "ConnectedRooks" => pick(bead, List(
      "connecting the rooks to solidify the back rank",
      "bringing the rooks into coordination",
      "linking the rooks for mutual protection"
    ))
    case "RookOnSeventh" | "SeventhRankInvasion" => pick(bead, List(
      "invading the seventh rank with the rook",
      "poking into the enemy's seventh rank",
      "establishing a rook on the seventh rank to paralyze the defense",
      "creating a 'rook on the seventh' situation that is technically winning"
    ))
    case _ => ""
  }

  def getQueenThemes(bead: Int, theme: String, extra: String = ""): String = theme match {
    case "QueenActivity" => pick(bead, List(
      "maximizing queen activity",
      "centralizing the queen for dynamic pressure",
      "leveraging the queen's dominant scope"
    ))
    case "QueenManeuver" => pick(bead, List(
      "repositioning the queen for a more effective attack",
      "transferring the queen to a stronger sector",
      "rerouting the queen to exploit central weaknesses"
    ))
    case _ => ""
  }

  def getTacticalThemes(bead: Int, theme: String, extra: String = ""): String = theme match {
    case "MateNet" => pick(bead, List(
      "weaving a lethal mate net around the king",
      "tightening the net for a decisive finish",
      "entombing the enemy king in a coordinate assault"
    ))
    case "PerpetualCheck" => pick(bead, List(
      "securing a draw through perpetual check",
      "forcing a repetition via infinite checks",
      "clinging to a draw in a difficult position through perpetual check"
    ))
    case "RemovingTheDefender" => pick(bead, List(
      s"stripping the defense by removing the $extra",
      s"eliminating the $extra to expose the target",
      s"decisively removing the $extra which defended the objective"
    ))
    case "Initiative" => pick(bead, List(
      "seizing the initiative with energetic play",
      "maintaining the pressure and dictating the flow",
      "driving the initiative forward through active piece play"
    ))
    case "Decoy" => pick(bead, List(
      "using a clever decoy to lure the opponent into a trap",
      "luring the piece to an unfavorable square",
      "setting up a decisive shot via a tactical decoy"
    ))
    case "Deflection" => pick(bead, List(
      "deflecting the defender away from its post",
      "forcing a piece away from its critical defensive duty",
      "executing a precise deflection to break the coordination"
    ))
    case _ => ""
  }

  def getSacrificeROI(bead: Int, reason: String): String = {
    val reasons = reason.split(",").toList
    val primary = reasons.headOption.getOrElse("unknown")
    primary match {
      case "open_file" => pick(bead, List(
        "sacrificing material to seize a vital open file",
        "giving up an exchange for long-term control of the file",
        "accepting material deficit in return for dominant rook activity"
      ))
      case "king_exposed" => pick(bead, List(
        "sacrificing material to expose the enemy king",
        "exchanging a rook for a minor piece to launch a decisive attack",
        "accepting a material imbalance to strip the king's pawn shield"
      ))
      case "passed_pawn" => pick(bead, List(
        "sacrificing material to support the advance of a passed pawn",
        "giving an exchange to clear the path for the promotion race",
        "trading a rook for a minor piece to secure a decisive passer"
      ))
      case "piece_activity" => pick(bead, List(
        "sacrificing material for superior piece activity and coordination",
        "improving piece scope at the cost of the exchange",
        "leveraging dynamic compensation for the material deficit"
      ))
      case "bishop_pair" => pick(bead, List(
        "sacrificing the exchange to keep the powerful bishop pair",
        "giving up a rook for a knight to maintain the two bishops",
        "prioritizing the bishop pair over the material balance"
      ))
      case _ => pick(bead, List(
        "sacrificing material for dynamic compensation",
        "giving up the exchange for positional pressure"
      ))
    }
  }
  
  def getTension(bead: Int, policy: String): String = policy.toLowerCase match {
    case "maintain" => pick(bead, List(
      "Tension should be maintained for now.",
      "Keeping the tension is beneficial.",
      "Neither side should rush to resolve the center.",
      "The central tension remains a key theme.",
      "It is best to keep the position fluid.",
      "Resolving the tension prematurely would be a mistake.",
      "The tension in the center defines the struggle.",
      "Maintaining the status quo in the center is advised."
    ))
    case "release" => pick(bead, List(
      "It is time to release the tension.",
      "Clarifying the center is the right path.",
      "A simplified structure is preferable here.",
      "Resolving the pawn tension helps clarify the plans.",
      "The best course is to exchange and clear the center."
    ))
    case _ => ""
  }

  // ===========================================================================
  // 3. TRANSITIONS & CONNECTORS
  // ===========================================================================

  def getContrast(bead: Int): String = pick(bead, List(
    "However,", "On the other hand,", "Conversely,", "In contrast,", "Alternatively,", "But,", "Yet,"
  ))

  def getResultConnector(bead: Int): String = pick(bead, List(
    "Consequently,", "Therefore,", "As a result,", "Thus,", "This leads to", "This allows"
  ))

  def getPriority(bead: Int): String = pick(bead, List(
    "The main task is to",
    "White must prioritize",
    "The priority is to",
    "Key objective:",
    "Attention must be focused on",
    "The battle revolves around"
  ))
  
  // ===========================================================================
  // 4. MAIN MOVE & INTENT
  // ===========================================================================

  def getMoveIntro(bead: Int, move: String, isMain: Boolean): String = {
    if (!isMain) return s"**$move**"
    // Variety in introducing the key move
    pick(bead, List(
      s"**$move**",
      s"White plays **$move**",
      s"The strong **$move**",
      s"A precise choice, **$move**",
      s"**$move** is the recommendation"
    ))
  }

  def getIntent(bead: Int, alignment: String, evidence: Option[String]): String = {
    // Phase 21.1: If evidence already contains 'by' or is a gerund, we adjust intro
    val ev = evidence.getOrElse("")
    val hasEv = ev.nonEmpty
    
    alignment.toLowerCase match {
      // Tactical intents
      case s if s.contains("attack") => 
        if (hasEv) s"continues the attack by $ev"
        else pick(bead, List("continues the attack", "drives the initiative", "keeps the pressure on", "poses serious questions"))
      case s if s.contains("defense") || s.contains("prophylactic") => 
        if (hasEv) s"solidifies the position by $ev"
        else pick(bead, List("prevents counterplay", "solidifies the position", "stops the enemy ideas", "provides necessary defense"))
      case s if s.contains("tactical") => 
        if (hasEv) s"creates tactical problems by $ev"
        else pick(bead, List("creates tactical threats", "complicates the game", "introduces tactical possibilities"))
      case s if s.contains("pressure") =>
        if (hasEv) s"applies pressure by $ev"
        else pick(bead, List("applies positional pressure", "maintains the tension", "keeps the opponent under pressure"))
      
      // Phase 22: New intent categories
      case s if s.contains("pawn break") =>
        if (hasEv) s"opens the position by $ev"
        else pick(bead, List("opens the position", "breaks through the center", "challenges the pawn structure"))
      case s if s.contains("rook activation") =>
        if (hasEv) s"activates the rook by $ev"
        else pick(bead, List("activates the rook", "brings the rook into play", "places the rook on an active file"))
      case s if s.contains("king activation") =>
        if (hasEv) s"brings the king into the game by $ev"
        else pick(bead, List("brings the king into the game", "centralizes the king", "advances the king toward the action"))
      case s if s.contains("centralization") =>
        if (hasEv && !ev.toLowerCase.contains("centraliz")) s"occupies a strong central square by $ev"
        else if (hasEv) s"improves the position by $ev"
        else pick(bead, List("centralizes the piece", "occupies a strong square", "improves piece placement"))
      case s if s.contains("outpost") =>
        if (hasEv && !ev.toLowerCase.contains("outpost")) s"establishes an outpost by $ev"
        else if (hasEv) s"improves the position by $ev"
        else pick(bead, List("establishes a strong outpost", "places the piece on an unassailable square"))
      case s if s.contains("simplification") =>
        if (hasEv) s"simplifies the position by $ev"
        else pick(bead, List("simplifies into a favorable endgame", "trades down to an easier position", "heads for the endgame"))
      case s if s.contains("file control") =>
        if (hasEv) s"seizes the open file by $ev"
        else pick(bead, List("seizes the open file", "controls the key file", "takes command of the file"))
      case s if s.contains("passed pawn") =>
        if (hasEv) s"advances the passed pawn by $ev"
        else pick(bead, List("pushes the passed pawn", "advances the trumping pawn", "creates promotion threats"))
      case s if s.contains("opposition") =>
        if (hasEv) s"takes the opposition by $ev"
        else pick(bead, List("takes the opposition", "gains the opposition", "seizes the key squares"))
      case s if s.contains("zugzwang") =>
        if (hasEv) s"forces zugzwang by $ev"
        else pick(bead, List("places the opponent in zugzwang", "forces a fatal concession", "squeezes the opponent"))
      case s if s.contains("pawn run") || s.contains("pawn_race") =>
         if (hasEv) s"pushes for promotion by $ev"
         else pick(bead, List("races for promotion", "pushes the pawn", "accelerates the pawn"))
      case s if s.contains("shouldering") =>
         if (hasEv) s"shoulders the enemy king by $ev"
         else pick(bead, List("uses the king to shoulder the opponent", "keeps the enemy king out", "dominates with the king"))
      case s if s.contains("castling") =>
        if (hasEv) s"castles by $ev"
        else pick(bead, List("castles to safety", "brings the king to safety", "completes the king's evacuation"))
      case s if s.contains("fianchetto") =>
        if (hasEv) s"fianchettoes the bishop by $ev"
        else pick(bead, List("fianchettoes the bishop", "develops the bishop to the long diagonal"))
      case s if s.contains("exchange") =>
        if (hasEv) s"forces an exchange by $ev"
        else pick(bead, List("forces a favorable exchange", "trades pieces", "simplifies the material"))
      
      // Development & central control
      case s if s.contains("development") => 
        if (hasEv) s"completes development by $ev"
        else pick(bead, List("completes development", "brings pieces into play", "improves piece activity", "connects the rooks"))
      case s if s.contains("central") => 
        if (hasEv) s"fights for the center by $ev"
        else pick(bead, List("maintains central tension", "fights for the center", "challenges the center"))
      case s if s.contains("maneuvering") =>
        if (hasEv) s"maneuvers the piece by $ev"
        else pick(bead, List("repositions the piece", "improves the piece's scope", "prepares for the next phase"))
      
      // Final fallback
      case _ => 
        if (hasEv) s"improves the position by $ev"
        else s"improves the position"
    }
  }

  def getVerification(bead: Int): String = pick(bead, List(
    " This is a verified strong continuation.",
    " Engine analysis confirms this is best.",
    " This path is verified to be precise.",
    " The most accurate continuation.",
    " Verified as the optimal path."
  ))

  // ===========================================================================
  // 5. FLUID NARRATIVE FLOW (Phase 15)
  // ===========================================================================

  /**
   * Complex template to integrate move, intent, opponent reply and sample continuation
   * into a single fluid paragraph without fragmented parentheses.
   */
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
        s"$fullMove $intent; after $rep, play might continue $sample with $evalTerm$cons.",
        s"$fullMove $intent, inviting $rep, where the sequence $sample leads to $evalTerm$cons.",
        s"With $fullMove, White $intent; after $rep $sample follows, maintaining $evalTerm$cons.",
        s"$fullMove $intent — if Black defends with $rep, then $sample results in $evalTerm$cons.",
        s"$fullMove $intent, causing problems after $rep $sample, where White has $evalTerm$cons."
      )
      case (Some(_), None) => List(
        s"$fullMove $intent, and after $rep, the position remains $evalTerm$cons.",
        s"By playing $fullMove, White $intent, forcing $rep$cons.",
        s"Black responds to $fullMove $intent with $rep$cons."
      )
      case _ => List(
        s"$fullMove $intent, and the position remains $evalTerm$cons.",
        s"With $fullMove, White $intent, keeping $evalTerm$cons.",
        s"$fullMove $intent. It remains a precise choice for $evalTerm$cons."
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
        "White is tightening the screw slowly.",
        "There is no hurry to resolve the situation.",
        "The position remains practically easier to play for White.",
        "White maintains full control of the proceedings."
      )
      case "Under Pressure" => List(
        "Black faces an uphill battle to find counterplay.",
        "The defensive task is mentally taxing here.",
        "The position remains unattractive for Black.",
        "One must proceed with extreme caution."
      )
      case "Balanced" => List(
        "The game remains a tense positional struggle.",
        "Both sides must navigate the complications carefully.",
        "The balance depends on precise calculation.",
        "A level game, but far from simple."
      )
      case _ => Nil
    }
    if (options.isEmpty) "" else pick(bead, options)
  }

  // ===========================================================================
  // 6. ALTERNATIVES & CRITIQUE
  // ===========================================================================

  def getDubiousVerdict(bead: Int, move: String, reason: String): String = pick(bead, List(
    s"$move is less accurate, $reason.",
    s"$move is imprecise as it risks $reason.",
    s"Avoid $move, which ends up $reason.",
    s"$move falls short because of $reason.",
    s"$move is a superficial try, $reason.",
    s"$move looks plausible but fails to $reason."
  ))

  def getPlayableVerdict(bead: Int, move: String, reason: String): String = pick(bead, List(
    s"$move is another option$reason.",
    s"$move is also playable$reason.",
    s"Alternatively, $move is worth considering$reason.",
    s"$move remains a solid alternative$reason.",
    s"One could also try $move$reason."
  ))
  
  // ===========================================================================
  // 6. THREATS & OPPONENT
  // ===========================================================================
  
  def getOpponentThreat(bead: Int, kind: String, desc: String): String = pick(bead, List(
    s"The opponent threatens $desc.",
    s"Black is preparing $desc.",
    s"Care is needed against $desc.",
    s"The immediate danger is $desc.",
    s"Black's idea is $desc."
  ))

  // ===========================================================================
  // HELPER: Deterministic Picker
  // ===========================================================================
  
  private def pick(seed: Int, options: List[String]): String = {
    if (options.isEmpty) ""
    else options(Math.abs(seed) % options.size)
  }
}
