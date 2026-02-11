package lila.llm

import lila.llm.model.*
import lila.llm.model.strategic.*

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
  // 2. VARIATION NARRATIVE (Multi-PV)
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
  // 6. LEGACY SEMANTIC RENDERER (REMOVED)
  // ============================================================

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
