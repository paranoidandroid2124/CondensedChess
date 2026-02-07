package lila.llm.analysis

import chess.{ Bishop, King, Knight, Pawn, Queen, Role, Rook }
import lila.llm.model.Fact

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
        s"Pause here. The position is $nature, with high tension. How would you proceed?",
        s"This $nature structure demands precision.",
        s"In such $nature positions, every tempo counts.",
        s"Understanding this $nature setup is critical for improvement."
      )
      case Style.Dramatic => Seq(
        s"The board is ablaze! A chaotic $nature battle.",
        s"Tension spikes in this $nature thriller.",
        s"A clash of wills in a sharp $nature landscape.",
        s"No room for error in this high-octane $nature struggle."
      )
      case _ => Seq( // "Shankland" Standard: Contextual & Professional
        s"The position has taken on a $nature character.",
        s"We have reached a complex $nature middlegame.",
        s"The struggle is defined by its $nature nature.",
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
        s"A single slip—$move—allows $reply, ending the resistance ($outcome)."
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

  def getJudgment(bead: Int, context: String): String = {
    val templates = context match {
      case "risky" => Seq("ambitious but loose", "creating unnecessary complications", "provocative")
      case "solid" => Seq("principled", "structurally sound", "pragmatic")
      case "lazy" => Seq("lazy", "automatic", "superficial")
      case "greedy" => Seq("materialistic", "greedy", "shortsighted")
      case _ => Seq("interesting", "notable", "complex")
    }
    pick(bead, templates)
  }

  def getUrgency(bead: Int): String = pick(bead, Seq(
    "cannot afford to wait",
    "must act immediately",
    "has no time for slow maneuvers",
    "needs to act with urgency"
  ))

  def getConcession(bead: Int, move: String): String = pick(bead, Seq(
    s"It is true that $move looks natural,",
    s"Admittedly, $move seems sufficient,",
    s"While $move is the standard response,",
    s"On the surface, $move appears strong,"
  ))

  def getRebuttal(bead: Int, problem: String): String = pick(bead, Seq(
    s"however, it fails to address $problem.",
    s"but it ignores the tactical reality of $problem.",
    s"yet this leaves the position vulnerable to $problem.",
    s"nevertheless, the underlying issue of $problem remains."
  ))

  def getHypothetical(bead: Int, move: String, consequence: String): String = pick(bead, Seq(
    s"Had they played $move, $consequence would have followed.",
    s"If $move, then $consequence decides the game.",
    s"The alternative $move runs into $consequence."
  ))

  // ===========================================================================
  // 1. OPENING / CONTEXT SETTERS
  // ===========================================================================
  
  def getOpening(bead: Int, phase: String, evalText: String, tactical: Boolean = false, ply: Int = 0): String = {
    val p = phase.toLowerCase
    val localSeed = bead ^ (ply * 0x45d9f3b)
    val templates = (p, evalText) match {
      case ("opening", _) => List(
          s"${if tactical then "The opening has turned tactical quickly" else "The opening phase is still fluid"}. $evalText.",
          s"${if tactical then "Early tactical motifs are already on the board" else "Early skirmishes are shaping the plans"}. $evalText.",
          s"${if tactical then "Development now intersects with concrete calculation" else "Development remains the first priority"}. $evalText.",
          s"${if tactical then "Both kings still need care as the position sharpens" else "Piece coordination and development are still in progress"}. $evalText.",
          s"${if tactical then "The opening is already testing tactical accuracy" else "It is early, but the strategic contours are visible"}. $evalText.",
          s"${if tactical then "Opening choices now can trigger forcing sequences" else "Opening play continues, with piece activity at a premium"}. $evalText.",
          s"${if tactical then "This opening has moved beyond quiet setup ideas" else "The game is still in opening channels"}. $evalText.",
          s"${if tactical then "The initial deployment phase is still active despite tactical tension" else "The initial deployment phase is not over yet"}. $evalText.",
          s"${if tactical then "Opening decisions now can immediately shape tactical outcomes" else "Opening decisions now will define the middlegame plan"}. $evalText."
      )
      case ("middlegame", _) => List(
          s"The game transitions into a complex middlegame. $evalText.",
          s"Middlegame complications begin to arise. $evalText.",
          s"We are now in the middlegame phase. $evalText.",
          s"The middlegame is underway. $evalText.",
          s"This is a full middlegame now. $evalText.",
          s"Plans and tactics start to bite. $evalText."
      )
      case ("endgame", _) => List( // "Shankland" Standard: Technical & Precise
          s"The position simplifies into an endgame. $evalText.",
          s"In this endgame, piece activity is paramount. $evalText.",
          s"Precision is required in this endgame. $evalText.",
          s"We are firmly in endgame territory. $evalText.",
          s"Endgame technique will decide a lot here. $evalText.",
          s"Small details matter in this endgame. $evalText."
      )
      case _ => List(
          s"The position requires careful handling. $evalText.",
          s"It is a critical moment. $evalText.",
          s"This is a position that rewards accuracy. $evalText.",
          s"Both sides need to be careful here. $evalText."
      )
    }
    pick(localSeed, templates)
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

  def getOpposition(bead: Int, isDirect: Boolean): String = {
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
      "A good bishop can exert long-range pressure.",
      "The good bishop becomes a major strategic asset.",
      "The strong bishop dominates key diagonals."
    ))
    case "BadBishop" => pick(bead, List(
      "The bad bishop is a long-term problem.",
      "A bad bishop often becomes a serious positional handicap.",
      "One side is stuck with a restricted bishop."
    ))
    case "BishopPair" => pick(bead, List(
      "The bishop pair can become a lasting advantage.",
      "The two bishops promise superior coordination in an open position.",
      "Long-range pressure from the bishop pair is a persistent theme."
    ))
    case "OppositeColorBishops" => pick(bead, List(
      "Opposite-coloured bishops often keep the game tense even with reduced material.",
      "With opposite-coloured bishops, plans tend to revolve around colour complexes.",
      "Opposite-coloured bishops can make defence difficult despite an 'equal' evaluation."
    ))
    case "ColorComplex" => pick(bead, List(
      "Control of the weakened colour complex is a key strategic factor.",
      "Exploiting holes on the vulnerable colour squares should guide the play.",
      "Dominating the critical light or dark squares can decide the game."
    ))
    case _ => ""
  }

  def getRookThemes(bead: Int, theme: String, extra: String = ""): String = theme match {
    case "SemiOpenFileControl" => pick(bead, List(
      s"applying pressure along the semi-open $extra-file",
      s"occupying the semi-open $extra-file to exert long-term pressure",
      s"placing the rook on the semi-open $extra-file for better activity"
    ))
    case "SeventhRankInvasion" | "RookOnSeventh" => pick(bead, List(
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
      "Neither side should rush to resolve the central tension.",
      "The central tension remains a key theme.",
      "It is best to keep the central tension and the position fluid.",
      "Resolving the tension prematurely would be a mistake.",
      "The tension in the center defines the struggle.",
      "Maintaining the central tension is advised."
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
    " An accurate continuation.",
    " The best practical chance.",
    " The most principled line.",
    " Verified by analysis.",
    " The critical test."
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
        "One inaccurate move can quickly change the evaluation."
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
    else {
      val mixed = scala.util.hashing.MurmurHash3.mixLast(0x9e3779b9, seed)
      val finalized = scala.util.hashing.MurmurHash3.finalizeHash(mixed, 1)
      options(Math.floorMod(finalized, options.size))
    }
  }

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

  def getAlternative(bead: Int, move: String, whyNot: Option[String]): String = {
    whyNot match {
      case Some(reason) if reason.nonEmpty =>
        pick(bead, List(
          s"**$move** is also possible, though $reason.",
          s"Alternatively, **$move** ($reason).",
          s"**$move** is worth considering, but $reason."
        ))
      case _ =>
        pick(bead, List(
          s"**$move** remains a credible candidate.",
          s"Alternatively, **$move** offers a different strategic route.",
          s"**$move** stays fully in contention over the board."
        ))
    }
  }

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
                s"From a practical standpoint, $side has the easier roadmap.",
                s"$side has the easier game to play."
              )
              case 1 => List(
                s"$side can press with a comparatively straightforward conversion scheme.",
                s"$side's strategic plan is easier to execute without tactical risk."
              )
              case _ => List(
                s"$side can play on intuition here.",
                s"$side can improve naturally without forcing complications."
              )
            pick(localSeed, families)
          case None =>
            val families = cycle match
              case 0 => List(
                "In practical terms, handling this position is straightforward.",
                "The position is easier to navigate than it first appears."
              )
              case 1 => List(
                "Plans are relatively clear for both players.",
                "There are no immediate tactical emergencies for either side."
              )
              case _ => List(
                "This is a manageable position from a practical perspective.",
                "The position allows methodical play without forcing tactics."
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
                s"$side has less margin for error here."
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
                "One slip can change the evaluation quickly.",
                "It is easy to misstep if you relax."
              )
              case _ => List(
                "A single tempo can swing the position.",
                "Defensive technique matters more than raw activity here."
              )
            pick(localSeed, families)
      case _ =>
        val families = cycle match
          case 0 => List(
            "The position is dynamically balanced.",
            "Counterplay exists for both sides.",
            "The practical chances are still shared between both players."
          )
          case 1 => List(
            "Both sides retain tactical resources, so concrete move-order precision matters.",
            "The position stays tense, and one inaccurate tempo can swing the initiative."
          )
          case _ => List(
            "Neither side has stabilized a lasting edge.",
            "The evaluation is close enough that accuracy still matters most."
          )
        pick(localSeed, families)
    }
  }

  def getThreatPlanLink(bead: Int, threatKind: String, planName: String): String = {
    pick(bead, List(
      s"The key is managing the $threatKind while pursuing $planName.",
      s"Balancing the $threatKind with $planName is the challenge.",
      s"The $threatKind complicates the plan of $planName."
    ))
  }

  def getWeaknessCandidateLink(bead: Int, weakness: String, move: String): String = {
    pick(bead, List(
      s"**$move** targets the $weakness directly.",
      s"The $weakness is exploited by **$move**.",
      s"**$move** puts pressure on the $weakness."
    ))
  }

  def getAnnotationPositive(bead: Int, playedSan: String): String = {
    pick(bead, List(
      s"**$playedSan** keeps to the strongest continuation.",
      s"**$playedSan** is fully sound and matches the position's demands.",
      s"**$playedSan** is accurate and consistent with the main plan."
    ))
  }

  def getAnnotationNegative(bead: Int, playedSan: String, bestSan: String, cpLoss: Int): String = {
    val mark = Thresholds.annotationMark(cpLoss)
    val markedMove = if mark.nonEmpty then s"**$playedSan** $mark" else s"**$playedSan**"

    Thresholds.classifySeverity(cpLoss) match {
      case "blunder" =>
        pick(bead, List(
          s"$markedMove is a blunder; it allows a forcing sequence against your position. **$bestSan** was required.",
          s"$markedMove is a decisive error and the position starts collapsing. The critical move was **$bestSan**.",
          s"$markedMove is a blunder that hands over the game flow; **$bestSan** was the only stable route."
        ))
      case "mistake" =>
        pick(bead, List(
          s"$markedMove is a clear mistake; it gives the opponent the initiative. Stronger is **$bestSan**.",
          s"$markedMove is a mistake that worsens coordination. Better is **$bestSan**.",
          s"$markedMove is serious enough to shift practical control; **$bestSan** was preferable."
        ))
      case "inaccuracy" =>
        pick(bead, List(
          s"$markedMove is an inaccuracy; **$bestSan** keeps better control.",
          s"$markedMove is slightly off and concedes practical ease; **$bestSan** is tighter.",
          s"$markedMove drifts from the best plan; the cleaner move is **$bestSan**."
        ))
      case _ =>
        pick(bead, List(
          s"$markedMove is slightly imprecise; **$bestSan** is cleaner.",
          s"$markedMove is playable but second-best; **$bestSan** keeps the position simpler.",
          s"$markedMove is not the top choice here; **$bestSan** remains the reference move."
        ))
    }
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

  def getMotifPrefix(bead: Int, motifs: List[String]): Option[String] = {
    val m = motifs.map(_.toLowerCase)
    val prefix = if (m.contains("greek_gift")) Some("A classic Greek Gift sacrifice! ")
    else if (m.contains("smothered_mate")) Some("Setting up a potential smothered mate. ")
    else if (m.contains("zugzwang")) Some("A pure zugzwang scenario: each legal move worsens the defender's coordination. ")
    else if (m.contains("isolated_pawn") || m.contains("iqp")) Some("The battle revolves around the isolated queen's pawn. ")
    else if (m.contains("hanging_pawns")) Some("The hanging pawns in the center create significant tension. ")
    else if (m.contains("minority_attack")) Some("White launches a minority attack on the queenside. ")
    else if (m.contains("opposite_bishops")) Some("Opposite-colored bishops signal an attack on the king. ")
    else if (m.contains("underpromotion")) Some("A rare and necessary underpromotion! ")
    else if (m.contains("stalemate_trick")) Some("A desperate but clever stalemate resource. ")
    else if (m.contains("prophylaxis")) Some("A deep prophylactic move to shore up the defense. ")
    else if (m.contains("interference")) Some("A tactical interference cutting off the defensive line. ")
    else if (m.contains("deflection")) Some("A sharp deflection to lure the pieces away. ")
    else if (m.contains("rook_lift")) Some("A powerful rook lift to join the assault. ")
    else if (m.contains("bishop_pair")) Some("The bishop pair exerts immense pressure on the open board. ")
    else if (m.contains("passed_pawn")) Some("The passed pawn is a constant threat that must be addressed. ")
    else if (m.contains("bad_bishop")) Some("Black is burdened by a bad bishop, restricted by its own pawn structure. ")
    else if (m.contains("knight_domination")) Some("The knight dominates its counterpart on this outpost. ")
    else if (m.contains("battery")) Some("A powerful battery on the open file exerts immense pressure. ")
    else if (m.contains("simplification") || m.contains("simplify")) Some("Endgame technique and simplification will be the decisive factors. ")
    else if (m.contains("liquidate") || m.contains("pawn_break")) Some("The central liquidation changes the character of the struggle. ")
    else if (m.contains("zwischenzug")) Some("A clever zwischenzug or intermediate move to seize the initiative. ")
    else if (m.contains("trapped_piece")) Some("A critical piece is trapped on the rim with no escape. ")
    else if (m.contains("king_hunt")) Some("A relentless king hunt begins! ")
    else if (m.contains("pawn_storm")) Some("A massive pawn storm is brewing on the kingside. ")
    else if (m.contains("repetition")) Some("The game heads toward a repeat of the position. ")
    else if (m.contains("novelty")) Some("An interesting opening novelty that changes the character of the game. ")
    else if (m.contains("novelty")) Some("An interesting opening novelty that changes the character of the game. ")
    else None

    prefix.map(p => pick(bead, List(p, p.replace("!", ".").replace(".", "!"))))
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
        "key squares and files can slip away.",
        "you can drift into a passive, hard-to-defend structure."
      ))
      case _ => pick(bead ^ 0x44e4d8b, List(
        "this needs an accurate response.",
        "the initiative can slip quickly.",
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

  def getBriefingFallback(conceptIds: List[String]): String = {
    if (conceptIds.isEmpty) "The position requires careful consideration."
    else s"Points to consider: ${conceptIds.take(2).mkString(", ")}."
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
    evalOutcomeClauseFromCp(Math.abs(cp.hashCode), cp)

  def evalOutcomeClauseFromCp(bead: Int, cp: Int): String = {
    val absCp = Math.abs(cp)
    val pawns = absCp.toDouble / 100.0
    val approx =
      if absCp >= 30 then
        val sign = if cp >= 0 then "+" else "-"
        Some(f"(≈$sign$pawns%.1f)")
      else None

    def withApprox(base: String): String =
      approx match
        case Some(a) if Math.abs(bead ^ base.hashCode) % 3 == 0 => s"$base $a"
        case _ => base

    if (cp >= 500) pick(bead, List(
      withApprox("White is winning"),
      withApprox("White has a decisive advantage"),
      withApprox("White is completely on top"),
      withApprox("White should convert with correct play"),
      withApprox("White is close to a winning position")
    ))
    else if (cp >= 300) pick(bead, List(
      withApprox("White holds a clear advantage"),
      withApprox("White is clearly better"),
      withApprox("White is pressing with a stable edge"),
      withApprox("White has the initiative and the better game"),
      withApprox("White has a comfortable plus")
    ))
    else if (cp >= 100) pick(bead, List(
      withApprox("White has a slight advantage"),
      withApprox("White is a bit better"),
      withApprox("White has the more pleasant position"),
      withApprox("White can play for two results"),
      withApprox("White is slightly ahead in the evaluation")
    ))
    else if (cp >= 30) pick(bead, List(
      withApprox("White has a small pull"),
      withApprox("White is just a touch better"),
      withApprox("White has a modest edge"),
      withApprox("White can keep up mild pressure"),
      withApprox("White has the easier side to press with")
    ))
    else if (cp <= -500) pick(bead, List(
      withApprox("Black is winning"),
      withApprox("Black has a decisive advantage"),
      withApprox("Black is completely on top"),
      withApprox("Black should convert with correct play"),
      withApprox("Black is close to a winning position")
    ))
    else if (cp <= -300) pick(bead, List(
      withApprox("Black holds a clear advantage"),
      withApprox("Black is clearly better"),
      withApprox("Black is pressing with a stable edge"),
      withApprox("Black has the initiative and the better game"),
      withApprox("Black has a comfortable plus")
    ))
    else if (cp <= -100) pick(bead, List(
      withApprox("Black has a slight advantage"),
      withApprox("Black is a bit better"),
      withApprox("Black has the more pleasant position"),
      withApprox("Black can play for two results"),
      withApprox("Black is slightly ahead in the evaluation")
    ))
    else if (cp <= -30) pick(bead, List(
      withApprox("Black has a small pull"),
      withApprox("Black is just a touch better"),
      withApprox("Black has a modest edge"),
      withApprox("Black can keep up mild pressure"),
      withApprox("Black has the easier side to press with")
    ))
    else pick(bead, List(
      "The position is near parity",
      "The position is about level",
      "Neither side has a clear advantage",
      "The evaluation is essentially balanced",
      "It's close to equal, with play for both sides"
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
          s"The pin on ${pinned.key} makes that ${roleLabel(pinnedRole)} awkward to handle."
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

  def getComplexityNote(bead: Int, sharpness: String): String = {
    pick(bead, List(
      s"The position is exceptionally $sharpness, where one slip is fatal.",
      s"High $sharpness demands extreme precision from both sides.",
      s"Practically speaking, the $sharpness nature of the battle favors the better prepared."
    ))
  }
}
