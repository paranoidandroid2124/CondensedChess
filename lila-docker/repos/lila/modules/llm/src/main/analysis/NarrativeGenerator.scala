package lila.llm

import lila.llm.model.*

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

  def describeStructure(labels: ConceptLabels, nature: PositionNature): String =
    val parts = List.newBuilder[String]

    // Nature Description
    parts += s"The position is ${nature.natureType.toString.toLowerCase} (tension: ${"%.1f".format(nature.tension)})."
    parts += nature.description

    // Key Structural Features
    val structText = labels.structureTags.map {
      case StructureTag.IqpWhite => "White has an Isolated Queen Pawn (IQP), creating dynamic chances but a static weakness."
      case StructureTag.IqpBlack => "Black has an IQP, offering activity at the cost of structure."
      case StructureTag.HangingPawnsWhite => "White has Hanging Pawns, demanding active piece play."
      case StructureTag.SpaceAdvantageWhite => "White enjoys a significant space advantage."
      case StructureTag.KingExposedBlack => "Black's king is exposed to attack."
      case StructureTag.MinorityAttackCandidate => "A minority attack on the queenside is a key theme."
      case t => t.toString
    }.mkString(" ")
    
    if (structText.nonEmpty) parts += structText

    // Positional Features (Geometric)
    val positionalText = labels.positionalTags.map {
      case PositionalTag.Outpost(sq, color) => s"${color.name} has a strong outpost on ${sq}."
      case PositionalTag.OpenFile(file, color) => s"${color.name} controls the open ${file}-file."
      case PositionalTag.WeakSquare(sq, color) => s"${color.name} has a weak square on ${sq}."
      case PositionalTag.LoosePiece(sq, color) => s"The ${color.name} piece on ${sq} is undefended."
      case PositionalTag.WeakBackRank(color) => s"${color.name} suffers from a weak back rank."
      case PositionalTag.BishopPairAdvantage(color) => s"${color} has the bishop pair advantage."
      case PositionalTag.BadBishop(color) => s"${color}'s bishop is bad, restricted by its own pawns."
      case PositionalTag.GoodBishop(color) => s"${color} has an active, good bishop."
      case _ => "" 
    }.filter(_.nonEmpty).mkString(" ")

    if (positionalText.nonEmpty) parts += positionalText

    // Plans
    val planText = labels.planTags.map {
      case PlanTag.KingsideAttackGood => "A direct kingside attack is the most promising plan."
      case PlanTag.CentralControlGood => "Control of the center is the primary objective."
      // MinorityAttackCandidate moved to structure tags loop
      case PlanTag.PromotionThreat => "Pushing passed pawns to promotion is the main threat."
      case PlanTag.PawnBreak(sq) => s"A key strategic idea is the pawn break $sq."
      case t => s"Idea: ${t.toString}"
    }.mkString("\n")

    if (planText.nonEmpty) parts += s"\nStrategic Themes:\n$planText"

    parts.result().mkString("\n")


  // ============================================================
  // 2. MOVE NARRATIVE (The "Why")
  // ============================================================

  def describeMove(move: String, motifs: List[Motif], evalDiff: Int): String =
    val motifText = motifs.take(3).map {
      case _: CheckMotif => "delivers a check"
      case m: CaptureMotif => 
        if m.captureType == CaptureType.Sacrifice then "sacrifices material for position"
        else "captures material"
      case m: ForkMotif => s"forks the ${m.targets.mkString(" and ")}"
      case _: PinMotif => "creates a pin"
      case _: PawnAdvance => "advances the pawn"
      case _: RookLift => "lifts the rook for an attack"
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

  def describeVariations(lines: List[VariationLine]): String =
    val parts = List.newBuilder[String]

    lines.headOption.foreach { main =>
      val scoreText = main.mate match
        case Some(m) => if (m > 0) s"(Mate in $m)" else s"(Mated in ${-m})"
        case None => s"(Eval: ${main.scoreCp / 100.0})"
        
      val mainMotifs = 
        if main.tags.contains(VariationTag.Prophylaxis) then "is a strong prophylactic decision"
        else if main.tags.contains(VariationTag.Simplification) then "simplifies into a winning ending"
        else if main.tags.contains(VariationTag.Sharp) then "leads to complications" 
        else "is the most principled choice"
        
      parts += s"**MAIN LINE** $scoreText: ${main.moves.take(6).mkString(" ")} — $mainMotifs."
    }

    // Structure alternative lines with scores
    val alts = lines.drop(1).take(3)
    if alts.nonEmpty then
      parts += "\n**ALTERNATIVE LINES:**"
      alts.foreach { alt =>
        val scoreText = alt.mate match
          case Some(m) => if (m > 0) s"Mate in $m" else s"Mated in ${-m}"
          case None => s"Eval: ${alt.scoreCp / 100.0}"
        
        val tagText = alt.tags.headOption.map(t => s"[${t.toString}]").getOrElse("")
        val assessment = 
          if alt.tags.contains(VariationTag.Mistake) then "— problematic"
          else if alt.tags.contains(VariationTag.Good) then "— playable"
          else if alt.tags.contains(VariationTag.Excellent) then "— strong alternative"
          else ""
          
        parts += s"  • ${alt.moves.take(6).mkString(" ")} ($scoreText) $tagText $assessment"
      }

    parts.result().mkString("\n")
  // ============================================================
  // 5. MOVE ORDER NARRATIVE
  // ============================================================

  def describeMoveOrderIssue(ev: MoveOrderAnalyzer.MoveOrderEvidence): String =
    if (ev.isTransposition) then
       s"MOVE ORDER FLEXIBILITY: Playing ${ev.originalOrder.mkString(" then ")} vs ${ev.flippedOrder.mkString(" then ")} leads to the exact same position (Transposition). Both are equally valid."
    else
      val pieceStr = ev.affectedPiece.map(_.name).getOrElse("piece")
      val sqStr = ev.affectedSquare.map(_.toString).getOrElse("target square")
      
      val reasonText = ev.issueType match
        case "missed_intermezzo" => s"the move ${ev.originalOrder.head} is a forcing intermezzo that must be inserted first."
        case _ => s"${ev.description} and the $pieceStr on $sqStr loses its necessary protection."

      s"""MOVE ORDER SENSITIVITY: 
         |Playing ${ev.originalOrder.mkString(" then ")} is structurally sound.
         |However, ${ev.flippedOrder.mkString(" then ")} fails because $reasonText"""
        .stripMargin
