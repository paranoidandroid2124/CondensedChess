package lila.llm

import lila.llm.model.*
import lila.llm.model.strategic.*
import lila.llm.analysis.NarrativeLexicon
import lila.llm.analysis.NarrativeLexicon.Style

/**
 * Narrative Generator
 * 
 * Converts raw analysis data into structured text blocks for the LLM prompt.
 * Focuses on "what to say" so the LLM can focus on "how to say it".
 */
object NarrativeGenerator:

  // ============================================================
  // 1. POSITION STRUCTURE NARRATIVE
  // ============================================================
  // NOTE: describeStructure was removed - it used ConceptLabels which are not populated
  // in the current pipeline. Structure/position info is now surfaced via 
  // ExtendedAnalysisData.structuralWeaknesses and pieceActivity in describeExtended.


  // ============================================================
  // 2. MOVE NARRATIVE (The "Why")
  // ============================================================

  def describeMove(move: String, motifs: List[Motif], evalDiff: Int): String =
    val motifText = motifs.take(3).map {
      case _: Motif.Check => "delivers a check"
      case m: Motif.Capture => 
        if m.captureType == Motif.CaptureType.Sacrifice then "sacrifices material for position"
        else "captures material"
      case m: Motif.Fork => s"forks the ${m.targets.mkString(" and ")}"
      case _: Motif.Pin => "creates a pin"
      case _: Motif.PawnAdvance => "advances the pawn"
      case _: Motif.RookLift => "lifts the rook for an attack"
      case _ => "improves the position"
    }.mkString(", and ")

    val evalText = 
      if evalDiff > 100 then "This move is decisive."
      else if evalDiff < -100 then "This move is a mistake."
      else "The position remains balanced."

    s"The move $move $motifText. $evalText"


  // ============================================================
  // 3. COUNTERFACTUAL NARRATIVE (The "What If")
  // ============================================================

  /**
   * Explains why the user's move might be inferior to the engine's best.
   * Uses "Hypothesis" logic to detect if the user fell for a trap or missed a tactic.
   */
  def describeCounterfactual(
      cf: CounterfactualMatch,
      hypotheses: List[Hypothesis]
  ): String =
    val parts = List.newBuilder[String]

    // 1. Diagnosis
    parts += s"DIAGNOSIS: ${cf.severity.toUpperCase}"
    parts += s"User played ${cf.userMove}, but ${cf.bestMove} was better (CP loss: ${cf.cpLoss})."

    // 2. Why was the user move bad? (Did they miss a threat?)
    if cf.missedMotifs.nonEmpty then
      val missed = cf.missedMotifs.map(_.getClass.getSimpleName).mkString(", ")
      parts += s"Missed Opportunity: The user missed a $missed available in the best line."

    // 3. Was the user move a "Human Mistake"? (Hypothesis match)
    // Check if the user's move matches any generated hypothesis (e.g. natural capture)
    val matchedHypothesis = hypotheses.find(_.move == cf.userMove)
    
    matchedHypothesis.foreach { h =>
       parts += s"Psychology: The move ${cf.userMove} looks natural (${h.candidateType}) because ${h.rationale}."
       parts += "However, it fails tactically."
    }


    // 4. Concrete Variation comparison (Placeholder for now)
    // Ideally: "After ${cf.userMove}, opponent responds with..."
    
    parts.result().mkString("\n")


  // ============================================================
  // 4. VARIATION NARRATIVE (Multi-PV)
  // ============================================================

  // Helper to normalize score for display (caps mate values)
  private def normalizedScoreDisplay(line: lila.llm.model.strategic.VariationLine): String = line.mate match
    case Some(m) if m > 0 => s"Mate in $m"
    case Some(m) if m < 0 => s"Mated in ${-m}"
    case _ => 
      val pawns = line.scoreCp / 100.0
      f"Eval: $pawns%.1f"

  def describeVariations(lines: List[lila.llm.model.strategic.VariationLine]): String =
    val parts = List.newBuilder[String]

    lines.headOption.foreach { main =>
      val scoreText = s"(${normalizedScoreDisplay(main)})"
        
      val mainMotifs = 
        if main.tags.contains(VariationTag.Prophylaxis) then "is a strong prophylactic decision"
        else if main.tags.contains(VariationTag.Simplification) then "simplifies into a winning ending"
        else if main.tags.contains(VariationTag.Sharp) then "leads to complications" 
        else "is the most principled choice"
        
      parts += s"**MAIN LINE** $scoreText: ${main.moves.take(6).mkString(" ")} — $mainMotifs."
    }

    // Structure alternative lines with scores and ALL tags
    val alts = lines.drop(1).take(3)
    if alts.nonEmpty then
      parts += "\n**ALTERNATIVE LINES:**"
      alts.foreach { alt =>
        val scoreText = normalizedScoreDisplay(alt)
        
        // Show ALL tags, not just the first one
        val tagText = if alt.tags.nonEmpty then alt.tags.map(t => s"[${t.toString}]").mkString(" ") else ""
        val assessment = 
          if alt.tags.contains(VariationTag.Blunder) then "— blunder"
          else if alt.tags.contains(VariationTag.Mistake) then "— problematic"
          else if alt.tags.contains(VariationTag.Good) then "— playable"
          else if alt.tags.contains(VariationTag.Excellent) then "— strong alternative"
          else ""
          
        parts += s"  • ${alt.moves.take(6).mkString(" ")} ($scoreText) $tagText $assessment"
      }

    parts.result().mkString("\n")

  // ============================================================
  // 5. MOVE ORDER NARRATIVE
  // ============================================================

  // ============================================================
  // 6. SEMANTIC & PRACTICAL NARRATIVE (Phase 2 Integration)
  // ============================================================

  def describeExtended(data: ExtendedAnalysisData, style: Style = Style.Book): String =
    val parts = List.newBuilder[String]

    // Nature Description
    val natureType = data.nature.natureType.toString.toLowerCase
    // Issue #3 Fix: Use mixSeed for better variability
    val planHash = data.plans.headOption.map(_.plan.name.hashCode).getOrElse(0)
    val seed = NarrativeLexicon.mixSeed(data.nature.description.hashCode.abs, data.ply, planHash)
  
    // FALLBACK for scant data
    if (data.motifs.isEmpty && data.plans.isEmpty) {
        parts += NarrativeLexicon.fallbackNature(seed, natureType, data.nature.tension)
    } else {
        parts += NarrativeLexicon.intro(seed, natureType, data.nature.tension, style)
    }
    parts += data.nature.description
    
    // DEBT 4: High-level strategic concepts
    if (data.conceptSummary.nonEmpty) {
      val conceptText = data.conceptSummary.take(3).mkString(", ")
      parts += s"KEY THEMES: $conceptText."
    }

    // Key Structural Features (Restored - Issue #1 Fix)
    if (data.structuralWeaknesses.nonEmpty) {
      val structDescriptions = data.structuralWeaknesses.map { wc =>
        val label = if (wc.cause.nonEmpty) wc.cause else "Structural Weakness"
        s"${wc.color} has $label on ${wc.squares.take(3).map(_.key).mkString(", ")}"
      }.mkString(". ")
      val header = style match {
        case Style.Coach => "STRUCTURE (Study Point):"
        case Style.Dramatic => "STRUCTURAL BATTLEGROUND:"
        case _ => "STRUCTURE:"
      }
      parts += s"$header $structDescriptions."
    }

    // Piece Activity (Trapped Pieces only - Bad Bishop is now in PositionalTag)
    val trappedPieces = data.pieceActivity.filter(_.isTrapped).map(p => s"Trapped ${p.piece} on ${p.square.key}")
    if (trappedPieces.nonEmpty) {
      val activityIssues = trappedPieces.mkString(", ")
      parts += s"PIECE ACTIVITY ISSUES: $activityIssues."
    }

    // NEW: Positional Features (Outpost, Open File)
    if (data.positionalFeatures.nonEmpty) {
      // Phase 12: Filter out LoosePiece if same square has StrongKnight or Outpost (positive feature)
      val strongSquares = data.positionalFeatures.collect {
        case lila.llm.model.strategic.PositionalTag.StrongKnight(sq, _) => sq
        case lila.llm.model.strategic.PositionalTag.Outpost(sq, _) => sq
      }.toSet
      // Phase 14: Filter out LoosePiece if it matches the current move's destination (sacs are intended)
      val destSquareToCheck = data.prevMove.flatMap(u => chess.format.Uci(u).flatMap {
        case m: chess.format.Uci.Move => Some(m.dest)
        case _ => None
      })
      
      val filteredFeatures = data.positionalFeatures.filter { 
        case lila.llm.model.strategic.PositionalTag.LoosePiece(sq, _, _) => 
          !strongSquares.contains(sq) && !destSquareToCheck.contains(sq)
        case _ => true
      }
      val posDescriptionsList = filteredFeatures.take(6).map {
        case lila.llm.model.strategic.PositionalTag.Outpost(sq, color) => 
          s"${color.name} enjoys a strong outpost on ${sq.key}"
        case lila.llm.model.strategic.PositionalTag.OpenFile(file, color) => 
          s"${color.name} dominates the open ${file.char}-file"
        case lila.llm.model.strategic.PositionalTag.LoosePiece(sq, _, color) =>
          // Phase 14: Clearer subject - "suffers from" implies it's their weakness
          s"${color.name} suffers from a loose piece on ${sq.key}"
        case lila.llm.model.strategic.PositionalTag.WeakBackRank(color) =>
          s"${color.name}'s back rank remains vulnerable"
        case lila.llm.model.strategic.PositionalTag.RookOnSeventh(color) =>
          s"${color.name}'s rook invades the 7th rank"
        case lila.llm.model.strategic.PositionalTag.StrongKnight(sq, color) =>
          s"${color.name} anchors a dominant knight on ${sq.key}"
        case lila.llm.model.strategic.PositionalTag.WeakSquare(sq, color) =>
          s"${color.name} has a structural weakness at ${sq.key}"
        case lila.llm.model.strategic.PositionalTag.BishopPairAdvantage(color) =>
          s"${color.name} possesses the bishop pair advantage"
        case lila.llm.model.strategic.PositionalTag.BadBishop(color) =>
          s"${color.name} is hampered by a bad bishop"
        case lila.llm.model.strategic.PositionalTag.GoodBishop(color) =>
          s"${color.name} utilizes an active bishop"
        case lila.llm.model.strategic.PositionalTag.SpaceAdvantage(color) =>
          s"${color.name} commands a space advantage"
        case lila.llm.model.strategic.PositionalTag.OppositeColorBishops =>
          "The presence of opposite-colored bishops suggests a drawish tendency"
        case lila.llm.model.strategic.PositionalTag.KingStuckCenter(color) =>
          s"${color.name}'s king is dangerously exposed in the center"
        case lila.llm.model.strategic.PositionalTag.ConnectedRooks(color) =>
          s"${color.name}'s rooks are coordinated"
        case lila.llm.model.strategic.PositionalTag.DoubledRooks(file, color) =>
          s"${color.name} exerts pressure with doubled rooks on the ${file.char}-file"
        // Phase 14: Enhanced ColorComplexWeakness
        case lila.llm.model.strategic.PositionalTag.ColorComplexWeakness(color, sqColor, squares) =>
          val colorName = if (sqColor.toString.toLowerCase.contains("white")) "light" else "dark"
          val sqList = lila.llm.analysis.NarrativeUtils.squaresToAlgebraic(squares.take(3))
          s"${color.name} exhibits significant $colorName-square weaknesses around $sqList"
        // Phase 12: Human-readable PawnMajority
        case lila.llm.model.strategic.PositionalTag.PawnMajority(color, flank, count) =>
          s"${color.name} holds a $count-pawn $flank majority"
        case other => other.toString
      }
      // Phase 12: Convert to prose format (max 4 items, joined naturally)
      val topFeatures = posDescriptionsList.take(4)
      val posDescriptions = if (topFeatures.size > 1) {
        topFeatures.init.mkString(", ") + ", and " + topFeatures.last
      } else {
        topFeatures.mkString
      }
      val header = style match {
        // Phase 14: Less generic header
        case Style.Coach => "KEY STRATEGIC FACTORS:"
        case Style.Dramatic => "THE BATTLEFIELD:"
        case _ => "POSITIONAL FEATURES:"
      }
      parts += s"$header $posDescriptions."
    }

    if (data.motifs.nonEmpty) {
      val motifDescriptions = data.motifs.take(5).map { m =>
        m match {
          case c: Motif.Check => s"Check (${c.checkType})"
          case f: Motif.Fork => s"Fork: ${f.targets.mkString("/")}"
          case p: Motif.Pin => s"Pin on ${p.pinnedPiece}"
          case _: Motif.Skewer => s"Skewer"
          case d: Motif.DiscoveredAttack => s"Discovered Attack by ${d.movingPiece}"
          case o: Motif.Overloading => s"Overloading ${o.overloadedPiece}"
          case i: Motif.Interference => s"Interference blocks ${i.blockedPiece1}"
          // Enhanced Capture with Exchange Sacrifice ROI (Phase 12: Expanded intent)
          case cap: Motif.Capture if cap.sacrificeROI.isDefined =>
            val roi = cap.sacrificeROI.get
            val intent = lila.llm.analysis.NarrativeUtils.exchangeSacrificeIntent(roi.reason)
            s"Exchange Sacrifice on ${cap.square.key}: ${cap.piece} for ${cap.captured} $intent"
          case cap: Motif.Capture => s"Capture: ${cap.captureType}"
          // NEW: Zwischenzug
          case z: Motif.Zwischenzug => 
            s"Zwischenzug! ${z.move.getOrElse(z.intermediateMove)} creates ${z.threatType} instead of recapturing"
          // Deflection/Decoy (already defined but add better templates)
          case d: Motif.Deflection => s"Deflection: lures ${d.piece} away from ${d.fromSquare.key}"
          case dc: Motif.Decoy => s"Decoy: attracts ${dc.piece} to ${dc.toSquare.key}"
          case ip: Motif.IsolatedPawn => s"Isolated Pawn on ${ip.file}"
          case pp: Motif.PassedPawn => s"Passed Pawn on ${pp.file}"
          case opp: Motif.Opposition => s"Opposition (${opp.oppType})"
          case _ => m.getClass.getSimpleName
        }
      }.mkString(", ")
      // Issue #2 Fix: Style-aware section headers
      val tacticsHeader = style match {
        case Style.Coach => "TACTICS (Study these patterns):"
        case Style.Dramatic => "TACTICAL FIREWORKS:"
        case _ => "TACTICS:"
      }
      parts += s"$tacticsHeader $motifDescriptions"
      
      // Issue #4 Fix: Rhetorical question for tactics (Coach/Dramatic)
      if (style == Style.Coach || style == Style.Dramatic) {
        data.motifs.headOption.foreach { firstMotif =>
          val motifName = firstMotif.getClass.getSimpleName
          parts += NarrativeLexicon.tacticsQuestion(seed, motifName, style)
        }
      }
    }
    
    // Strategic Plans
    if (data.plans.nonEmpty) {
      val planDescriptions = data.plans.take(3).map { p =>
        val evidenceStr = p.evidence.take(2).map(_.description).mkString(", ")
        s"${p.plan.name}: $evidenceStr"
      }.mkString("; ")
      val plansHeader = style match {
        case Style.Coach => "PLANS (Key ideas):"
        case Style.Dramatic => "THE MASTER PLAN:"
        case _ => "PLANS:"
      }
      parts += s"$plansHeader $planDescriptions"
      
      // Rhetorical Device (Item #4 enhanced)
      if (style == Style.Coach || style == Style.Dramatic) {
         data.plans.headOption.foreach { bestPlan =>
            parts += NarrativeLexicon.rhetoricalQuestion(seed, bestPlan.plan.name, style)
         }
      }
    }

    // STRATEGIC FLOW (Phase 5 Integration)
    data.planSequence.foreach { seq =>
      seq.previousPlan.foreach { prev =>
        val curr = seq.currentPlans.primary.plan
        if (prev.id != curr.id) { // Only explain when plan actually changes
           val justification = lila.llm.analysis.TransitionAnalyzer.explainTransition(
             prev, 
             curr, 
             data.toContext,
             isTacticalThreatToUs = Some(data.tacticalThreatToUs),
             isTacticalThreatToThem = Some(data.tacticalThreatToThem),
             phase = Some(data.phase)
           )
           parts += s"STRATEGIC FLOW: $justification"
        }
      }
    }

    // 1. Practical Outlook (Human Context)
    data.practicalAssessment.foreach { assessment =>
      parts += s"PRACTICAL ASSESSMENT: ${assessment.verdict}."
      parts += s"Engine Score: ${assessment.engineScore}, Practical Score: ${"%.1f".format(assessment.practicalScore)}."
      if (assessment.biasFactors.nonEmpty) {
        val factors = assessment.biasFactors.map(f => s"${f.factor}: ${f.description}").mkString(", ")
        parts += s"Factors: $factors"
      }
    }

    // Phase 16: Alternatives SAN & Annotation
    if (data.alternatives.nonEmpty) {
      parts += "\n**ALTERNATIVE OPTIONS:**"
      
      // Best score from PV1 (assuming sorted)
      val bestScore = data.alternatives.headOption.map(_.effectiveScore).getOrElse(0)

      data.alternatives.zipWithIndex.foreach { case (alt, idx) =>
        val diff = bestScore - alt.effectiveScore

        val annotation = if (idx == 0) "!" 
                         else if (diff > 300) "??"
                         else if (diff > 100) "?"
                         else if (diff > 50) "?!"
                         else ""

        val label = if (idx == 0) "Main Line"
                    else if (diff > 200) "Refutation" 
                    else if (diff > 30) "Inferior"
                    else "Alternative"

        // Step 1: SAN Conversion
        val sanMoves = lila.llm.analysis.NarrativeUtils.uciListToSan(data.fen, alt.moves)
        
        // Step 2: Annotate First Move
        val movesStr = if (sanMoves.nonEmpty) {
          val first = sanMoves.head + annotation
          (first :: sanMoves.tail).mkString(" ")
        } else {
          alt.moves.mkString(" ")
        }

        val scoreText = alt.mate.map(m => s"#$m").getOrElse(s"${if (alt.scoreCp > 0) "+" else ""}${alt.scoreCp}")
        
        parts += s"${(idx + 97).toChar}) $movesStr ($scoreText) [$label]"
      }
    }

    // (Structure/Piece Activity now handled in Issue #1 block at top of describeExtended)

    // Prevented Plans (Prophylaxis)
    data.preventedPlans.foreach { pp =>
      parts += s"PREVENTION: The move prevented '${pp.planId}' which would have caused a score drop of ${pp.counterplayScoreDrop}."
    }

    // Compensation
    data.compensation.foreach { comp =>
      parts += s"COMPENSATION: ${comp.conversionPlan} (Invested: ${comp.investedMaterial}, Return: ${comp.returnVector.keys.mkString(", ")})."
    }

    // Endgame
    data.endgameFeatures.foreach { eg =>
      if (eg.hasOpposition) parts += "ENDGAME: Direct Opposition detected."
      if (eg.isZugzwang) parts += "ENDGAME: ZUGZWANG! Any move worsens the position."
      if (eg.keySquaresControlled.nonEmpty) {
        parts += s"ENDGAME: Controlling key promotion squares: ${eg.keySquaresControlled.mkString(", ")}."
      }
    }

    // Counterfactual (The "Why") - with capped cpLoss display
    data.counterfactual.foreach { cf =>
      parts += s"**TACTICAL DIAGNOSIS:**"
      parts += s"Played: ${cf.userMove} (${cf.severity})"
      // Cap cpLoss display to max 40 pawns (4000 cp) for readability
      val displayLoss = Math.min(cf.cpLoss, 4000) / 100.0
      parts += s"Best:   ${cf.bestMove} (Diff: ${f"$displayLoss%.1f"} pawns)"
      
      // Show Refutation / Punisher Move
      cf.userLine.moves.drop(1).headOption.foreach { response =>
         parts += s"Allows: $response (Opponent Response)"
      }
      
      if (cf.missedMotifs.nonEmpty) {
         val motifs = cf.missedMotifs.map(_.getClass.getSimpleName).mkString(", ")
         parts += s"Missed Opportunity: $motifs"
      }
      if (cf.userMoveMotifs.nonEmpty) {
         val motifs = cf.userMoveMotifs.map(_.getClass.getSimpleName).mkString(", ")
         parts += s"Complications: $motifs"
      }
      parts += ""
    }

    // Alternatives (Multi-PV / Book Style)
    if (data.candidates.nonEmpty) {
      parts += describeCandidatesBookStyle(data.candidates, data.fen, data.ply)
    } else if (data.alternatives.nonEmpty) {
      parts += describeVariations(data.alternatives)
    }

    parts.result().mkString("\n")

  // Helper for manual SAN generation (fallback since chess.format.San is inaccessible)
  private def simpleSan(fen: String, uciStr: String): String =
    val variant = chess.variant.Standard
    chess.format.Fen.read(variant, chess.format.Fen.Full(fen)).flatMap { sit =>
      chess.format.Uci(uciStr).flatMap {
        case u: chess.format.Uci.Move => sit.move(u).toOption
        case _ => None
      }.map { move =>
        if (move.castle.nonEmpty) {
          if (move.dest.file > move.orig.file) "O-O" else "O-O-O"
        } else {
          val piece = move.piece
          val captureIndicator = if (move.capture.nonEmpty) "x" else ""
          // Simple disambiguation: if pawn capture, include file
          val pSan = if (piece.role == chess.Pawn) {
             if (move.capture.nonEmpty) s"${move.orig.file.char}x" else ""
          } else piece.role.forsyth.toUpper.toString + captureIndicator
          
          val dest = move.dest.key
          s"$pSan$dest"
        }
      }
    }.getOrElse(uciStr)
  
  // Convert a sequence of UCI moves to SAN using iterative FEN tracking
  // Uses move.toSanStr which is the standard Lichess API
  private def convertLineToSan(startFen: String, uciMoves: List[String], startPly: Int): String =
    val variant = chess.variant.Standard
    var currentFen = startFen
    var currentPly = startPly
    
    val sanMoves = uciMoves.take(6).flatMap { uciStr =>
      chess.format.Fen.read(variant, chess.format.Fen.Full(currentFen)).flatMap { sit =>
        chess.format.Uci(uciStr).flatMap {
          case u: chess.format.Uci.Move => sit.move(u).toOption
          case _ => None
        }.map { move =>
          // Get SAN using the standard Lichess API
          val san = move.toSanStr.toString
          
          // Update FEN for next iteration using move.after (Position)
          currentFen = chess.format.Fen.write(move.after).value
          currentPly += 1
          
          san
        }
      }.orElse(Some(uciStr)) // Fallback to UCI if conversion fails
    }
    
    // Format with move number for first move
    val fullMove = (startPly / 2) + 1
    val dot = if (startPly % 2 == 0) "." else "..."
    sanMoves match {
      case Nil => ""
      case first :: rest => s"$fullMove$dot $first ${rest.mkString(" ")}".trim
    }
  
  // Verbalize score difference with material context
  private def verbalizeEval(scoreDiff: Int, altLine: List[String]): String =
    // Check for captures in the refutation line to determine material context
    val hasCapture = altLine.exists(_.contains("x")) || altLine.exists(m => m.length == 4 && m(2).isLetter)
    if (scoreDiff > 500) "loses decisive material"
    else if (scoreDiff > 300) if (hasCapture) "loses the exchange" else "loses material"
    else if (scoreDiff > 150) "is clearly inferior"
    else if (scoreDiff > 100) "is slightly worse"
    else "is playable"
  
  // Annotate moves in a line with !, !! for captures/checks
  private def annotateLine(moves: List[String]): String =
    moves.zipWithIndex.map { case (m, i) =>
      val isCapture = m.contains("x") || (m.length == 4 && m.charAt(2).isDigit && m.charAt(3).isDigit)
      val annotation = if (isCapture && i < 3) "!" else ""
      s"$m$annotation"
    }.mkString(" ")

  def describeCandidatesBookStyle(
      candidates: List[lila.llm.model.strategic.AnalyzedCandidate],
      fen: String,
      ply: Int
  ): String =
    val parts = List.newBuilder[String]
    
    // Intro
    parts += "\n**ANALYSIS OF OPTIONS:**"
    
    candidates.headOption.foreach { main =>
       val san = simpleSan(fen, main.move)
       val fullMove = (ply / 2) + 1
       val dot = if (ply % 2 == 0) "." else "..."
       val moveNum = s"$fullMove$dot"
       
       val planName = main.futureContext.toLowerCase
       
       // Intent→Result: "aims for X, so that Y"
       // Derive result from line or motifs
       val resultText = if (main.line.moves.length > 2) {
         val targetMove = main.line.moves.drop(2).headOption.getOrElse("")
         if (targetMove.contains("x") || targetMove.length >= 4) s", leading to $targetMove" else ""
       } else ""
       
       val intentText = s"$moveNum $san aims for $planName$resultText"
       
       // Evaluation verbalization
       val evalScore = main.score / 100.0
       val evalText = if (math.abs(evalScore) > 5.0) "winning" 
                      else if (math.abs(evalScore) > 1.5) "decisive advantage" 
                      else if (math.abs(evalScore) > 0.5) "slight advantage" 
                      else "balanced"
       
       parts += s"**$intentText ($evalText).**"
       
       // Main Line with annotations
       val lineSan = convertLineToSan(fen, main.line.moves, ply)
       val annotatedLine = annotateLine(lineSan.split(" ").toList)
       parts += s"**Main Line:** $annotatedLine"
       
       if (main.prophylaxisResults.nonEmpty) {
          parts += s"**Prophylaxis:** Prevents ${main.prophylaxisResults.head.planId}."
       }
       parts += "\n**Alternatives:**"
    }

    // Alternatives: a), b), c)
    val alts = candidates.drop(1)
    if (alts.isEmpty) {
      parts += "• No specific engine variations available in this position."
    } else {
      alts.zipWithIndex.foreach { case (alt, idx) =>
         val label = (idx + 97).toChar // a, b, c...
         val san = simpleSan(fen, alt.move)
         
         // Refutation or comparison
         val scoreDiff = candidates.headOption.map(m => math.abs(m.score - alt.score)).getOrElse(0)
         val outcome = verbalizeEval(scoreDiff, alt.line.moves)
         
         // Extended refutation with annotations
         val refutationText = if (scoreDiff > 100) {
            val refMoves = alt.line.moves.drop(1).take(4)
            val refSan = convertLineToSan(fen, alt.move :: refMoves.toList, ply).split(" ").drop(2).mkString(" ")
            val annotatedRef = annotateLine(refSan.split(" ").toList)
            
            // Try to add motif context if available
            val motifContext = if (alt.motifs.nonEmpty) {
              s" (${alt.motifs.head.getClass.getSimpleName.toLowerCase})"
            } else ""
            
            s"runs into $annotatedRef$motifContext ($outcome)"
         } else {
            val ctx = alt.futureContext.toLowerCase
            s"aims for $ctx, but $outcome"
         }
         
         val scoreDisp = f"${alt.score / 100.0}%.1f"
         
         // Counterfactual integration: "instead of main..."
         val counterfactualPrefix = candidates.headOption.map { main =>
           if (scoreDiff > 200) s"Instead of ${simpleSan(fen, main.move)}, " else ""
         }.getOrElse("")
         
         parts += s"$label) $counterfactualPrefix$san $refutationText ($scoreDisp)."
         
         // Thematic contrast with main line
         candidates.headOption.foreach { main =>
           if (main.futureContext != alt.futureContext) {
             parts += s"   *(Theme: ${alt.futureContext} vs Main's ${main.futureContext})*"
           }
         }
      }
    }

    parts.result().mkString("\n")

  // ============================================================
  // 7. PHASE 6: HIERARCHICAL NARRATIVE OUTPUT
  // ============================================================
  
  /**
   * Tone scaling prefixes based on ConfidenceLevel.
   * Shifting from definitive to suggestive.
   */
  private def toneWrap(level: ConfidenceLevel, category: String): String = level match {
    case ConfidenceLevel.Engine => category match {
      case "plan"   => "Confirmed Plan: "
      case "threat" => "URGENT THREAT: "
      case "target" => "CRITICAL TARGET: "
      case "rationale" => "Proven Logic"
      case _        => "Definitive: "
    }
    case ConfidenceLevel.Probe => category match {
      case "plan"   => "Verified Plan: "
      case "threat" => "VERIFIED THREAT: "
      case "target" => "VALIDATED TARGET: "
      case "rationale" => "Verified Logic"
      case _        => "Verified: "
    }
    case ConfidenceLevel.Heuristic => category match {
      case "plan"   => "Suggested Plan: "
      case "threat" => "Threat: "
      case "target" => "Target: "
      case "rationale" => "Heuristic Logic"
      case _        => ""
    }
  }

  /**
   * Generate hierarchical narrative using NarrativeContext.
   * Format: Header → Summary → Evidence Tables → Delta
   */
  def describeHierarchical(ctx: NarrativeContext): String = {
    val parts = List.newBuilder[String]
    
    // === HEADER ===
    parts += s"=== CONTEXT [${ctx.header.phase} | ${ctx.header.criticality} | ${ctx.header.choiceType} | ${ctx.header.riskLevel}] ==="
    parts += ""
    
    // === OPENING EVENT (Phase A9) ===
    ctx.openingEvent.foreach { event =>
      parts += "=== OPENING EVENT (MASTERS) ==="
      event match {
        case OpeningEvent.Intro(eco, name, theme, topMoves) =>
          parts += s"[INTRO] $eco: $name"
          parts += s"• Theme: $theme"
          parts += s"• Main moves: ${topMoves.mkString(", ")}"
          
        case OpeningEvent.BranchPoint(moves, reason, game) =>
          parts += s"[BRANCH POINT] $reason"
          parts += s"• Alternatives: ${moves.mkString(", ")}"
          game.foreach(g => parts += s"• Reference: $g")
          
        case OpeningEvent.OutOfBook(playedMove, topMoves, ply) =>
          parts += s"[OUT OF BOOK] Move $playedMove leaves known theory (ply $ply)"
          parts += s"• Theory moves were: ${topMoves.mkString(", ")}"
          
        case OpeningEvent.TheoryEnds(lastPly, count) =>
          parts += s"[THEORY ENDS] Master games thin out at ply $lastPly ($count games)"
          parts += "• Subsequent analysis based on engine evaluation only"
          
        case OpeningEvent.Novelty(move, cpLoss, evidence, ply) =>
          parts += s"[NOVELTY CANDIDATE] $move at ply $ply"
          parts += s"• Engine loss: ${cpLoss}cp (acceptable)"
          parts += s"• Evidence: $evidence"
      }
      parts += ""
    }
    
    // === SUMMARY (5 lines) ===
    parts += "=== SUMMARY ==="
    // Use the top plan's confidence for the primary plan summary
    val planConfidence = ctx.plans.top5.headOption.map(_.confidence).getOrElse(ConfidenceLevel.Heuristic)
    val threatConfidence = ctx.threats.toUs.headOption.map(_.confidence).getOrElse(ConfidenceLevel.Heuristic)

    parts += s"• Primary Plan: ${toneWrap(planConfidence, "plan")}${ctx.summary.primaryPlan}"
    ctx.summary.keyThreat.foreach(t => parts += s"• Key Threat: $t")
    parts += s"• Choice Type: ${lila.llm.analysis.NarrativeUtils.humanize(ctx.summary.choiceType)}"
    parts += s"• Tension: ${lila.llm.analysis.NarrativeUtils.humanize(ctx.summary.tensionPolicy)}"
    parts += s"• Eval: ${ctx.summary.evalDelta}"
    parts += ""
    
    // === DECISION RATIONALE (Phase F) ===
    ctx.decision.foreach { dr =>
      parts += "=== DECISION RATIONALE ==="
      dr.focalPoint.foreach(fp => parts += s"Focus: ${fp.label} (${toneWrap(dr.confidence, "rationale")})")
      parts += s"Logic: ${dr.logicSummary}"
      
      val d = dr.delta
      if (d.resolvedThreats.nonEmpty) parts += s"• Solved: ${d.resolvedThreats.mkString(", ")}"
      if (d.newOpportunities.nonEmpty) parts += s"• Gain: ${d.newOpportunities.mkString(", ")}"
      if (d.planAdvancements.nonEmpty) parts += s"• Plan: ${d.planAdvancements.mkString(", ")}"
      if (d.concessions.nonEmpty) parts += s"• Risk: ${d.concessions.mkString(", ")}"
      parts += ""
    }
    
    // === THREATS TABLE ===
    if (ctx.threats.toUs.nonEmpty || ctx.threats.toThem.nonEmpty) {
      parts += "=== THREATS ==="
      val toUsLines = ctx.threats.toUs.map { t =>
        val topMarker = if (t.isTopCandidateDefense) " [Counter-move: Top Candidate]" else ""
        s"TO US: ${t.urgencyLabel} THREAT: ${t.toNarrative}$topMarker"
      }
      val toThemLines = ctx.threats.toThem.map(t => s"TO THEM: ${t.urgencyLabel} OPPORTUNITY: ${t.toNarrative}")
      (toUsLines ++ toThemLines).foreach(parts += _)
      parts += ""
    }
    
    // === PAWN PLAY ===
    if (ctx.pawnPlay.breakReady || ctx.pawnPlay.tensionPolicy != "Ignore") {
      parts += "=== PAWN PLAY ==="
      if (ctx.pawnPlay.breakReady) {
        parts += s"Break: ${ctx.pawnPlay.breakFile.getOrElse("?")} ready (${ctx.pawnPlay.breakImpact} impact)"
      }
      parts += s"Tension: ${ctx.pawnPlay.tensionPolicy} (${ctx.pawnPlay.tensionReason})"
      if (ctx.pawnPlay.passedPawnUrgency != "Background") {
        parts += s"Passer: ${ctx.pawnPlay.passedPawnUrgency}${ctx.pawnPlay.passerBlockade.map(b => s" - blockaded at $b").getOrElse("")}"
      }
      parts += ""
    }
    
    // === PLANS (Top 5) ===
    if (ctx.plans.top5.nonEmpty) {
      parts += "=== PLANS ==="
      ctx.plans.top5.foreach { p =>
        val evidenceStr = if (p.evidence.nonEmpty) s" (${p.evidence.mkString(", ")})" else ""
        parts += s"${p.rank}. ${toneWrap(p.confidence, "plan")}${p.name} ${f"${p.score}%.2f"}$evidenceStr"
        if (p.supports.nonEmpty) parts += s"   • Support: ${p.supports.mkString(", ")}"
        if (p.blockers.nonEmpty) parts += s"   • Blocker: ${p.blockers.mkString(", ")}"
        if (p.missingPrereqs.nonEmpty) parts += s"   • Missing: ${p.missingPrereqs.mkString(", ")}"
      }
      ctx.plans.suppressed.foreach { s =>
        val status = if (s.isRemoved) "removed" else "downweighted"
        parts += s"[$status: ${s.name} - ${s.reason}]"
      }
      parts += ""
    }
    
    // === L1 SNAPSHOT ===
    val snapOpt = ctx.snapshots.headOption
    val l1Items = snapOpt.map { snap =>
      List(
        Some(s"Material: ${snap.material}"),
        snap.imbalance.map(i => s"Imbalance: $i"),
        snap.kingSafetyUs.map(k => s"King(us): $k"),
        snap.kingSafetyThem.map(k => s"King(them): $k"),
        snap.mobility.map(m => s"Mobility: $m"),
        snap.centerControl.map(c => s"Center: $c")
      ).flatten
    }.getOrElse(Nil)
    
    if (l1Items.size > 1 || snapOpt.exists(_.openFiles.nonEmpty)) {
      parts += "=== L1 SNAPSHOT ==="
      parts += l1Items.mkString(" | ")
      snapOpt.foreach { snap =>
        if (snap.openFiles.nonEmpty) {
          parts += s"Open files: ${snap.openFiles.mkString(", ")}"
        }
      }
      parts += ""
    }

    // === SEMANTIC CONTEXT (Phase A) ===
    ctx.semantic.foreach { s =>
      parts += "=== SEMANTIC CONTEXT ==="
      if (s.conceptSummary.nonEmpty) {
        parts += s"• Strategic Themes: ${s.conceptSummary.take(3).mkString(", ")}"
      }
      if (s.structuralWeaknesses.nonEmpty) {
        val weak = s.structuralWeaknesses.take(3).map(w => s"${w.squareColor}-square weakness (${w.cause}) at ${w.squares.mkString(", ")}")
        parts += s"• Key Weaknesses: ${weak.mkString("; ")}"
      }
      if (s.pieceActivity.nonEmpty) {
        val activity = s.pieceActivity.filter(p => p.isTrapped || p.isBadBishop || p.mobilityScore < 0.3).take(3).map(p => s"${p.piece} on ${p.square} (mobility: ${p.mobilityScore}${if (p.isTrapped) ", trapped" else ""})")
        if (activity.nonEmpty) parts += s"• Piece Issues: ${activity.mkString("; ")}"
      }
      if (s.positionalFeatures.nonEmpty) {
        val filtered = s.positionalFeatures.filter(_.tagType != "LoosePiece").take(5)
        if (filtered.nonEmpty) {
          val feats =
            filtered.map(f =>
              s"${lila.llm.analysis.NarrativeUtils.humanize(f.tagType)}(${f.color.toLowerCase}${f.square.map(sq => s" at $sq").getOrElse("")})"
            )
          parts += s"• Positional Features: ${feats.mkString("; ")}"
        }
      }
      s.practicalAssessment.foreach(p => parts += s"• Practical Assessment: ${p.verdict} (Score: ${p.practicalScore})")
      s.compensation.foreach(c => parts += s"• Compensation: ${c.conversionPlan} (Invested: ${c.investedMaterial})")
      parts += ""
    }

    // === OPPONENT INTENT (Phase B) ===
    ctx.opponentPlan.foreach { p =>
      // P10 Issue 2: Use "Current" for established facts, "Anticipated" for future intent
      val label = if (p.isEstablished) "Current" else "Anticipated"
      parts += s"=== OPPONENT $label ==="
      val evidenceStr = if (p.evidence.nonEmpty) s" (${p.evidence.mkString(", ")})" else ""
      parts += s"• $label: ${toneWrap(p.confidence, "plan")}${p.name}$evidenceStr"
      if (p.supports.nonEmpty) parts += s"  • Support: ${p.supports.mkString(", ")}"
      if (p.blockers.nonEmpty) parts += s"  • Blocker: ${p.blockers.mkString(", ")}"
      if (p.missingPrereqs.nonEmpty) parts += s"  • Missing: ${p.missingPrereqs.mkString(", ")}"
      parts += ""
    }
    
    // === DELTA ===
    ctx.delta.foreach { d =>
      parts += "=== DELTA ==="
      val evalSign = if (d.evalChange >= 0) "+" else ""
      parts += s"Eval: $evalSign${d.evalChange}cp"
      if (d.newMotifs.nonEmpty) parts += s"New: ${d.newMotifs.mkString(", ")}"
      if (d.lostMotifs.nonEmpty) parts += s"Lost: ${d.lostMotifs.mkString(", ")}"
      d.structureChange.foreach(s => parts += s"Structure: $s")
      parts += ""
    }
    
    // === PHASE CONTEXT (A8) ===
    parts += "=== PHASE ==="
    parts += s"Current: ${ctx.phase.current} (${ctx.phase.reason})"
    ctx.phase.transitionTrigger.foreach(t => parts += s"Trigger: $t")
    parts += ""
    
    // === STRATEGIC FLOW ===
    ctx.strategicFlow.foreach { flow =>
      parts += "=== STRATEGIC FLOW ==="
      parts += flow
      parts += ""
    }
    
    // === META SIGNALS (B-axis) ===
    ctx.meta.foreach { m =>
      parts += "=== META ==="
      parts += s"Choice: ${lila.llm.analysis.NarrativeUtils.humanize(m.choiceType.toString)}"
      if (m.targets.tactical.nonEmpty)
        parts += s"TACTICAL: ${m.targets.tactical.map(t => s"${toneWrap(t.confidence, "target")}${t.ref.label} (${t.reason})").mkString("; ")}"
      if (m.targets.strategic.nonEmpty)
        parts += s"STRATEGIC: ${m.targets.strategic.map(t => s"${toneWrap(t.confidence, "target")}${t.ref.label} (${t.reason})").mkString("; ")}"
      parts += s"Plans: ${m.planConcurrency.primary}${m.planConcurrency.secondary.map(s => s" + $s ${m.planConcurrency.relationship}").getOrElse("")}"
      m.divergence.foreach(d => parts += s"Diverge: ply ${d.divergePly}${d.punisherMove.map(p => s" punished by $p").getOrElse("")}")
      m.errorClass.foreach(e => parts += s"Error: ${e.errorSummary}")
      m.whyNot.foreach(w => parts += s"WhyNot: $w")
      parts += ""
    }
    
    // === CANDIDATES ===
    val validCandidates = ctx.candidates.filter(_.move.nonEmpty)
    if (validCandidates.isEmpty) {
      parts += "=== CANDIDATES ==="
      parts += "• No specific engine variations available in this position."
    } else {
      parts += "=== CANDIDATES ==="
      validCandidates.zipWithIndex.foreach { case (c, idx) =>
        val label = ('a' + idx).toChar
        val whyNotStr = c.whyNot.map(w => s" — $w").getOrElse("")
        val alertStr = c.tacticalAlert.map(a => s" [!$a]").getOrElse("")
        // Human-readable tag conversion (no raw enum names)
        val tagPhrases = c.tags.map {
          case CandidateTag.Sharp => "sharp line"
          case CandidateTag.Solid => "solid choice"
          case CandidateTag.Prophylactic => "prophylactic"
          case CandidateTag.Converting => "converting"
          case CandidateTag.Competitive => "competitive alternative"
          case CandidateTag.TacticalGamble => "tactical attempt"
        }
        val tagStr = if (tagPhrases.nonEmpty) s" (${tagPhrases.mkString(", ")})" else ""
        val evidenceStr = if (c.tacticEvidence.nonEmpty) s" [Evidence: ${c.tacticEvidence.mkString("; ")}]" else ""
        parts += s"$label) ${c.move}${c.annotation}$tagStr (${c.planAlignment}, ${c.practicalDifficulty})$alertStr$evidenceStr$whyNotStr"
      }
    }
    
    // === PROBE REQUESTS (Phase 6.5) ===
    if (ctx.probeRequests.nonEmpty) {
      parts += ""
      parts += "=== PROBE REQUESTS (Evidence Augmentation) ==="
      ctx.probeRequests.foreach { pr =>
        parts += s"• ${pr.id}: Probing moves [${pr.moves.mkString(", ")}] at depth ${pr.depth}"
      }
    }
    
    parts.result().mkString("\n")
  }
