package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.strategic._
import chess.Square

/**
 * Book Style Renderer
 * 
 * Transforms NarrativeContext into annotated prose paragraphs,
 * mimicking the style of professional chess book commentary.
 * 
 * Output structure:
 * 1. Position statement (1 sentence)
 * 2. Main challenge (1-2 sentences)
 * 3. Main move analysis (1 paragraph)
 * 4. Alternatives comparison (sentences, not list)
 */
object BookStyleRenderer:

  /** Converts PascalCase to lowercase spaced words. */
  private def humanize(s: String): String =
    s.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase

  /**
   * Main entry point - renders NarrativeContext as book-style prose.
   */
  def render(ctx: NarrativeContext): String = 
    renderOrganic(ctx).prose

  /**
   * Trace output: Shows which fields were used, dropped, and why.
   */
  def renderTrace(ctx: NarrativeContext): String =
    renderOrganic(ctx).trace

  /**
   * Full Coverage: Lossless structured appendix via automatic traversal.
   */
  def renderFull(ctx: NarrativeContext): String =
    AppendixRenderer.render(ctx)

  /**
   * Position Commentary containing all views.
   */
  case class FullResult(prose: String, trace: String)

  /**
   * Orchestrates rendering with live trace recording.
   */


  /** 
   * Active Trace Recorder to prevent drift.
   */

  private case class TraceEntry(field: String, status: String, value: String, reason: String)

  /**
   * Automated Lossless Appendix Renderer.
   * Recursively traverses any NarrativeContext structure.
   */
  private object AppendixRenderer:
    def render(ctx: NarrativeContext): String =
      val sb = new StringBuilder()
      sb.append("# Narrative Appendix (Lossless)\n")
      sb.append("> [!NOTE]\n")
      sb.append("> Units: 100cp = 1.0 pawn. Scores are from White's perspective (+ is better for White).\n")
      sb.append("> Field paths match the hierarchy in this appendix.\n\n")
      
      renderKeyFacts(sb, ctx)
      
      renderValue(sb, "", ctx)
      sb.toString()

    private def renderKeyFacts(sb: StringBuilder, ctx: NarrativeContext): Unit =
      sb.append("## KEY FACTS\n")
      ctx.plans.top5.headOption.foreach(p => sb.append(s"- Top Plan: ${p.name}\n"))
      ctx.threats.toUs.headOption.foreach(t => sb.append(s"- Primary Threat: ${t.kind}${t.square.map(s => s" on $s").getOrElse("")} (${t.lossIfIgnoredCp}cp)\n"))
      ctx.candidates.headOption.foreach(c => sb.append(s"- Recommended: ${c.move}${c.annotation}\n"))
      ctx.semantic.flatMap(_.practicalAssessment).foreach(a => sb.append(s"- Engine Score: ${a.engineScore}\n"))
      sb.append("\n")

    private def renderValue(sb: StringBuilder, path: String, value: Any): Unit = value match {
      case Some(v) => renderValue(sb, path, v)
      case None => sb.append(s"- $path = None\n")
      
      case seq: Iterable[_] =>
        // Headers for top-level lists (e.g., ## Candidates)
        val displayPath = path match {
          case other if other.nonEmpty => other.capitalize
          case _ => ""
        }
        val isTopLevel = path.nonEmpty && !path.contains(".") && !path.contains("[")
        if (isTopLevel) sb.append(s"## $displayPath\n")
        
        if (seq.isEmpty) sb.append(s"- $path = []\n")
        else seq.zipWithIndex.foreach { (v, i) =>
          renderValue(sb, s"$path[$i]", v)
        }
        
      case p: Product =>
        val name = p.productPrefix
        // Humanize top-level paths for test compatibility
        val displayPath = path match {
          case "header" => "Context"
          case "semantic" => "Semantic Layer"
          case "candidates" => "Candidates" // Explicit override if needed
          case other if other.nonEmpty => other.capitalize
          case _ => ""
        }
        
        // Only valid top-level keys (no dots, no brackets) get ## headers
        val isTopLevel = path.nonEmpty && !path.contains(".") && !path.contains("[")
        
        if (isTopLevel) 
           sb.append(s"## $displayPath\n")
        else if (path.nonEmpty) 
           sb.append(s"### $path ($name)\n")
        
        // Stable field ordering using productElementNames (Scala 3 / 2.13+)
        val names = p.productElementNames
        val values = p.productIterator
        names.zip(values).foreach { (fieldName, v) =>
          val nextPath = if (path.isEmpty) fieldName else s"$path.$fieldName"
          renderValue(sb, nextPath, v)
        }
        sb.append("\n")
        
      case primitive => 
        sb.append(s"- $path = $primitive\n")
    }

  // ============================================================
  // 0. TEMPLATE UTILS & CONCEPT LINKER
  // ============================================================

  /**
   * Selects a variant deterministically based on ply.
   */
  private def variant(bead: Int, options: String*): String =
    options(bead % options.length)

  /** 
   * Transition helper. 
   * Returns a transition phrase based on logical relationship.
   */
  private def transition(bead: Int, relation: String): String = 
    relation match {
      case "contrast" => variant(bead, "However,", "In contrast,", "On the other hand,")
      case "consequence" => variant(bead, "Consequently,", "As a result,", "Therefore,")
      case "addition" => variant(bead, "Furthermore,", "Additionally,", "Moreover,")
      case _ => ""
    }

  /**
   * Concept Linker: Finds logical relationships between narrative elements.
   */
  private object ConceptLinker:
    
    case class LinkedConcept(priority: Int, text: String, coveredFields: List[String])

    def findLinks(ctx: NarrativeContext, rec: TraceRecorder, bead: Int): List[LinkedConcept] =
      val links = List(
        linkThreatToPlan(ctx, rec, bead),

        linkWeaknessToCandidate(ctx, rec, bead),
        linkPlanToCounterplay(ctx, rec, bead),
        linkStrategicFeatureToCandidate(ctx, rec, bead)
      ).flatten
      links.sortBy(-_.priority)

    // Link 1: Threat -> Defensive Plan
    private def linkThreatToPlan(ctx: NarrativeContext, rec: TraceRecorder, bead: Int): Option[LinkedConcept] =
      for {
        threat <- ctx.threats.toUs.headOption if threat.lossIfIgnoredCp >= 100
        plan <- ctx.plans.top5.headOption if plan.name.toLowerCase.contains("defense")
      } yield {
        val msg = variant(bead,
          s"To meet the ${threat.kind.toLowerCase} threat, ${humanize(plan.name)} is required.",
          s"White must prioritize ${humanize(plan.name)} to handle the ${threat.kind.toLowerCase} threat."
        )
        LinkedConcept(10, msg, List("threats.toUs[0]", "plans.top5[0]"))
      }

    // Link 2: Weakness -> Targeting Candidate
    private def linkWeaknessToCandidate(ctx: NarrativeContext, rec: TraceRecorder, bead: Int): Option[LinkedConcept] =
      for {
        weakness <- ctx.semantic.flatMap(_.structuralWeaknesses.headOption)
        candidate <- ctx.candidates.headOption
      } yield {
        val squares = weakness.squares.take(2).mkString(" and ")
        
        // Phase 24: Specific Weakness Narratives
        val msg = weakness.cause match {
          case "Hanging Pawns" => 
            NarrativeLexicon.getHangingPawns(bead, squares)
          case _ if candidate.planAlignment.contains("attack") => 
            variant(bead + 1,
              s"The move **${candidate.move}** exploits the weak $squares.",
              s"Targeting the weak $squares, **${candidate.move}** poses immediate problems."
            )
          case _ => // Default generic fallback
             s"White seeks to prove that $squares is a weakness."
        }
        LinkedConcept(8, msg, List("semantic.structuralWeaknesses[0]", "candidates[0]"))
      }

    // Link 4: Strategic Feature -> Candidate (Pawn Storm / Minority Attack)
    private def linkStrategicFeatureToCandidate(ctx: NarrativeContext, rec: TraceRecorder, bead: Int): Option[LinkedConcept] =
      ctx.semantic.flatMap(_.positionalFeatures.find(f => 
        List("MinorityAttack", "PawnMajority", "GoodBishop", "BadBishop", "BishopPairAdvantage", "OppositeColorBishops", "ColorComplexWeakness", "OpenFile", "ConnectedRooks", "DoubledRooks", "RookOnSeventh", "SemiOpenFileControl", "RookBehindPassedPawn", "KingCutOff", "QueenActivity", "QueenManeuver", "MateNet", "PerpetualCheck", "RemovingTheDefender", "Initiative").contains(f.tagType)
      )).flatMap { feature =>
         // Check if candidate aligns with this feature (e.g. pawn push or typical plan)
         // For now, assume if feature is prominent, it's relevant to the main line
         feature.tagType match {
           case "MinorityAttack" =>
             val flank = feature.detail.map(_.takeWhile(_ != ' ')).getOrElse("queenside") // Extract "pure" flank from info
             Some(LinkedConcept(9, NarrativeLexicon.getMinorityAttack(bead, flank), List("semantic.positionalFeatures[MinorityAttack]")))
           case "PawnMajority" =>
             val flank = feature.detail.map(_.takeWhile(_ != ' ')).getOrElse("kingside")
             Some(LinkedConcept(9, NarrativeLexicon.getPawnStorm(bead, flank), List("semantic.positionalFeatures[PawnMajority]")))
           case "GoodBishop" | "BadBishop" | "BishopPairAdvantage" | "OppositeColorBishops" | "ColorComplexWeakness" =>
             val theme = feature.tagType.replace("Advantage", "").replace("Weakness", "")
             Some(LinkedConcept(7, NarrativeLexicon.getBishopThemes(bead, theme), List(s"semantic.positionalFeatures[${feature.tagType}]")))
           case "OpenFile" | "ConnectedRooks" | "DoubledRooks" | "RookOnSeventh" | "SemiOpenFileControl" | "RookBehindPassedPawn" | "KingCutOff" =>
             val theme = feature.tagType
             val extra = feature.detail.getOrElse("")
             Some(LinkedConcept(7, NarrativeLexicon.getRookThemes(bead, theme, extra), List(s"semantic.positionalFeatures[${feature.tagType}]")))
           case "QueenActivity" | "QueenManeuver" =>
             val theme = feature.tagType
             Some(LinkedConcept(7, NarrativeLexicon.getQueenThemes(bead, theme), List(s"semantic.positionalFeatures[${feature.tagType}]")))
           case "MateNet" | "PerpetualCheck" | "RemovingTheDefender" | "Initiative" =>
             val theme = feature.tagType
             val extra = feature.detail.getOrElse("")
             Some(LinkedConcept(8, NarrativeLexicon.getTacticalThemes(bead, theme, extra), List(s"semantic.positionalFeatures[${feature.tagType}]")))
           case _ => None
         }
      }
      
    // Link 3: Plan vs Counterplay (Contextual Weaving)
    // FIX: Prevent A vs A tautology and validate plan existence
    private def linkPlanToCounterplay(ctx: NarrativeContext, rec: TraceRecorder, bead: Int): Option[LinkedConcept] =
      for {
        myPlan <- ctx.plans.top5.headOption
        oppPlan <- ctx.opponentPlan
        // FIX #2: Suppress if plans are identical or semantically equivalent
        if !areSemanticallyEqual(myPlan.name, oppPlan.name)
        // FIX #1: Validate plan is grounded (e.g., "Queen Sacrifice" requires queen on board)
        if isPlanGrounded(myPlan.name, ctx)
      } yield {
        val msg = variant(bead + 2,
          s"White aims for ${humanize(myPlan.name)}, while Black counters with ${humanize(oppPlan.name)}.",
          s"The strategic tension: White's ${humanize(myPlan.name)} meets Black's ${humanize(oppPlan.name)}."
        )
        LinkedConcept(5, msg, List("plans.top5[0]", "opponentPlan"))
      }

    // Helper: Check if two plan names are semantically equivalent (to avoid "A vs A")
    private def areSemanticallyEqual(plan1: String, plan2: String): Boolean =
      val norm1 = plan1.toLowerCase.replaceAll("\\s+", "")
      val norm2 = plan2.toLowerCase.replaceAll("\\s+", "")
      norm1 == norm2 || 
      (norm1.contains("central") && norm2.contains("central")) ||
      (norm1.contains("kingside") && norm2.contains("kingside")) ||
      (norm1.contains("queenside") && norm2.contains("queenside"))

    // Helper: Validate that a plan is grounded in the current position
    def isPlanGrounded(planName: String, ctx: NarrativeContext): Boolean =
      val name = planName.toLowerCase
      val phase = ctx.header.phase.toLowerCase
      val material = ctx.snapshots.headOption.map(_.material.toLowerCase).getOrElse("")

      // 1. Phase Grounding
      if (name.contains("endgame") && phase != "endgame") return false
      if (name.contains("opening") && phase != "opening") return false

      // 2. Material Grounding
      if (name.contains("queen") && (name.contains("sacrifice") || name.contains("sac"))) {
        if (material.count(_ == 'q') < 1) return false
      }
      else if (name.contains("rook lift") || name.contains("rook activation")) {
        if (!material.contains("r")) return false
      }
      else if (name.contains("bishop pair")) {
        if (material.count(_ == 'b') < 2) return false
      }

      // 3. Flank/King Safety Grounding
      if (name.contains("kingside attack")) {
        // Only attack if opponent king is on kingside AND we have some attackers or their king is exposed
        val snap = ctx.snapshots.headOption
        val isKingsideAttackPossible = snap.exists { s =>
          // Parse kingSafetyThem string or check if it's "Exposed"
          s.kingSafetyThem.exists(_.toLowerCase.contains("exposed")) || 
          s.kingSafetyThem.exists(_.toLowerCase.contains("attackers"))
        }
        if (!isKingsideAttackPossible) return false
      }

      // 4. Structural Grounding
      if (name.contains("pawn break")) {
        if (!ctx.pawnPlay.breakReady && ctx.pawnPlay.tensionPolicy.toLowerCase != "maximum") return false
      }
      
      // 5. Central Control Grounding
      if (name.contains("central control") || name.contains("center control")) {
        val snap = ctx.snapshots.headOption
        val hasControl = snap.exists(s => s.centerControl.exists(_.toLowerCase.contains("dominant")) || 
                                          s.centerControl.exists(_.toLowerCase.contains("advantage")))
        if (!hasControl) return false
      }

      // Default: allow if we can't verify
      true
  end ConceptLinker

  // ============================================================
  // ORCHESTRATION: ORGANIC FLOW
  // ============================================================

  private def renderOrganic(ctx: NarrativeContext): FullResult =
    val rec = new TraceRecorder()
    val bead = Math.abs(ctx.hashCode)
    
    // ==========================================================
    // BLOCK 1: THE SETUP (Context + Challenge + Links)
    // ==========================================================
    val setupParts = scala.collection.mutable.ListBuffer[String]()
    
    // 1. Position Statement
    setupParts += renderPositionStatement(ctx, rec)
    
    // 2. Identify and Add High-Priority Links
    val links = ConceptLinker.findLinks(ctx, rec, bead)
    val primaryLink = links.headOption
    primaryLink.foreach { link =>
      setupParts += link.text
      link.coveredFields.foreach(f => rec.use(f, "Linked Concept", "Concept Linker"))
      rec.logMentions(link.text)
    }

    // 3. Fallbacks for Setup
    if (!rec.isUsed("threats.toUs[0]")) 
      renderMainChallenge(ctx, rec).foreach(setupParts += _)
      
    if (!rec.isUsed("plans.top5[0]") && !rec.isUsed("opponentPlan"))
      renderPlanSummary(ctx, rec).foreach(setupParts += _)

    // Join Setup Block
    val setupBlock = setupParts.filter(_.nonEmpty).mkString(" ")

    // ==========================================================
    // BLOCK 2: THE SOLUTION (Move + Analysis + Alternatives)
    // ==========================================================
    val solutionParts = scala.collection.mutable.ListBuffer[String]()

    // 4. Main Move
    if (!rec.isUsed("candidates[0]"))
      solutionParts += renderMainMove(ctx, rec)
      
    // 5. Alternatives (Comparison)
    solutionParts += renderAlternatives(ctx, rec)
    
    // 6. Opponent Opportunities (Post-solution context)
    solutionParts += renderOpponentOpportunities(ctx, rec)

    // Join Solution Block
    val solutionBlock = solutionParts.filter(_.nonEmpty).mkString(" ")
    
    // ==========================================================
    // FINAL ASSEMBLY
    // ==========================================================
    // Log unused
    rec.unused("header", "-", "Not rendered")
    rec.unused("probeRequests", "async", "Internal")

    // Combine blocks with double newline for paragraph break
    val finalProse = List(setupBlock, solutionBlock)
      .filter(_.nonEmpty)
      .mkString("\n\n")

    FullResult(finalProse, rec.renderTable)
    
    
  // ============================================================
  // COMPONENT RENDERERS (Individual - fallback if no link)
  // ============================================================
  
  // ============================================================
  // 1. POSITION STATEMENT
  // ============================================================
  
  /**
   * One sentence summarizing the position balance and key tension.
   */
  private def renderPositionStatement(ctx: NarrativeContext, rec: TraceRecorder): String =
    val evalScore = ctx.semantic.flatMap(_.practicalAssessment).map(_.engineScore.toDouble)
    val evalText = SemanticTranslator.evalToText(evalScore, ctx.snapshots.headOption.map(_.material).getOrElse("="))
    // Use NarrativeLexicon for infinite diversity
    val bead = Math.abs(ctx.hashCode)
    
    rec.use("snapshots.head.material", ctx.snapshots.headOption.map(_.material).getOrElse("="), "Position statement")
    rec.use("pawnPlay.tensionPolicy", ctx.pawnPlay.tensionPolicy, "Tension sentence")
    rec.use("phase.current", ctx.phase.current, "Phase context")

    val openingText = NarrativeLexicon.getOpening(bead, ctx.phase.current, evalText)
    val tensionText = NarrativeLexicon.getTension(bead, ctx.pawnPlay.tensionPolicy)
    
    val text = s"$openingText $tensionText".trim
    rec.logMentions(text)
    text
    
  // ============================================================
  // 2. MAIN CHALLENGE (Partial - logic handled by ConceptLinker mostly)
  // ============================================================
    
  private def renderMainChallenge(ctx: NarrativeContext, rec: TraceRecorder): Option[String] =
    val bead = Math.abs(ctx.hashCode)
    ctx.threats.toUs.headOption.filter(_.lossIfIgnoredCp >= 100).map { t =>
      val targetInfo = t.square.map(s => s" on $s").getOrElse("")
      rec.use("threats.toUs[0]", s"${t.kind}", "Fallback Challenge")
      variant(bead,
        s"The immediate concern is the ${t.kind.toLowerCase} threat$targetInfo.",
        s"White must immediately address the ${t.kind.toLowerCase} threat$targetInfo."
      )
    }

  private def renderPlanSummary(ctx: NarrativeContext, rec: TraceRecorder): Option[String] =
    val bead = Math.abs(ctx.hashCode)
    val topPlanMatch = ctx.plans.top5.headOption
    val topPlan = topPlanMatch.map(_.name).getOrElse("development")
    
    // Phase 20.3: Grounding Gate
    // Phase 20.3: Grounding Gate
    val isGrounded = ConceptLinker.isPlanGrounded(topPlan, ctx)
    
    // Phase 24.1: Strategic Fallback
    // If plan is ungrounded OR just generic "development", try to find a specific strategic theme
    val groundedPlan = if (isGrounded && topPlan != "development") topPlan else {
       ctx.semantic.flatMap(_.positionalFeatures.collectFirst {
         case f if f.tagType == "MinorityAttack" => "minority attack"
         case f if f.tagType == "PawnMajority" => 
           val flank = f.detail.map(_.takeWhile(_ != ' ')).getOrElse("kingside")
           s"advancement of the $flank pawn majority"
       }).orElse(
         ctx.semantic.flatMap(_.structuralWeaknesses.collectFirst {
           case w if w.cause == "Hanging Pawns" => "pressure against the hanging pawns" 
         })
       ).getOrElse("development")
    }

    // Phase 19.2: Intelligent Anchoring based on actual open files
    // Phase 23: Endgame Specialization
    val endgameFacts = ctx.facts.collect {
      case f: lila.llm.model.Fact.Opposition => f
      case f: lila.llm.model.Fact.KingActivity => f
    }

    if (ctx.header.phase.toLowerCase == "endgame" && endgameFacts.nonEmpty) {
       val factText = endgameFacts.head match {
         case f: lila.llm.model.Fact.Opposition => 
           NarrativeLexicon.getOpposition(bead, f.distance, f.isDirect)
         case f: lila.llm.model.Fact.KingActivity => 
           NarrativeLexicon.getKingActivity(bead, f.mobility)
         case _ => "executing endgame technique"
       }
       rec.use("plan", factText, "Endgame Fact")
       return Some(s"The main task is to focus on $factText.")
    }

    val anchoredPlan = groundedPlan.toLowerCase match {
      case s if s.contains("kingside") => 
        ctx.snapshots.headOption.flatMap(_.openFiles.find(f => "fgh".contains(f))).map(f => s"$groundedPlan on the $f-file").getOrElse(groundedPlan)
      case s if s.contains("queenside") => 
        ctx.snapshots.headOption.flatMap(_.openFiles.find(f => "abc".contains(f))).map(f => s"$groundedPlan on the $f-file").getOrElse(groundedPlan)
      case s if s.contains("central") => groundedPlan // humanizePlan handles "center"
      case _ => groundedPlan
    }

    // NEW: Retrospective Suppression (Phase 24.2)
    // If we fell back to "development" but we ALREADY talked about a high-level concept in the Setup/Linker,
    // we should just silence the plan summary to avoid the "development" filler.
    if (anchoredPlan == "development" && (
        rec.isUsed("semantic.positionalFeatures[MinorityAttack]") ||
        rec.isUsed("semantic.positionalFeatures[PawnMajority]") ||
        rec.isUsed("semantic.structuralWeaknesses[0]") // Linker #2 (Weakness)
    )) return None

    // Phase 21.2: Strategic Anchoring (Leveraging plan evidence)
    val specificAnchor = topPlanMatch.flatMap { pm =>
      pm.evidence.collectFirst {
        case e if e.toLowerCase.contains("tension") => "leveraging the central tension"
        case e if e.toLowerCase.contains("break") => "preparing the pawn break"
        case e if e.toLowerCase.contains("open file") => "utilizing the open file"
      }
    }.map(a => s", $a").getOrElse("")

    rec.use("plan", anchoredPlan, "Primary strategic goal")
    val planText = s"The main task is to pursue the ${SemanticTranslator.humanizePlan(anchoredPlan)}$specificAnchor."
    Some(planText)

  // ============================================================
  // 3. MAIN MOVE ANALYSIS
  // ============================================================
  
  /**
   * One paragraph on the best move with intent and integrated sequence.
   */
  private def renderMainMove(ctx: NarrativeContext, rec: TraceRecorder): String =
    val bead = Math.abs(ctx.hashCode)
    ctx.candidates.headOption.map { main =>
      // Phase 23: Prioritize Fact objects for high-fidelity commentary
      val factEv = main.facts.headOption.map(f => SemanticTranslator.factToText(f, bead))
      val tacticEv = factEv.orElse(main.tacticEvidence.headOption.map(e => SemanticTranslator.evidenceToText(e, bead)))
      
      // Phase 28: Handle Exchange Sacrifice ROI
      val roiText = main.facts.collectFirst {
        case f: lila.llm.model.Fact.TargetPiece if main.annotation.contains("!") || main.annotation.contains("?") =>
          // This is a bit of a hack to detect sacrifice ROI if not explicitly in motifs
          // But MoveAnalyzer already adds it to Capture motif.
          "" // Placeholder
      }.getOrElse("")

      val immediateText = NarrativeLexicon.getIntent(bead, main.planAlignment, tacticEv)
      
      // Combine immediate intent with downstream consequence if present
      val intent = main.downstreamTactic match {
        case Some(tactic) => s"$immediateText, preparing a $tactic"
        case None => immediateText
      }
      
      // Phase 14: Get opponent reply data
      val replySan = ctx.engineEvidence.flatMap(_.best.flatMap(_.theirReply)).map(_.san)
      
      // Phase 14: Sample line data (starting from 3nd move of PV)
      val sampleRest = ctx.engineEvidence.flatMap(_.best.flatMap(_.sampleLineFrom(2, 6)))
      
      val evalTerm = getEvalTerm(ctx)
      val consequence = ctx.engineEvidence.flatMap(_.best).map(v => renderPvConsequence(v, bead)).getOrElse("")
      
      val msg = NarrativeLexicon.getMainFlow(
        bead = bead,
        move = main.move,
        annotation = main.annotation,
        intent = intent,
        replySan = replySan,
        sampleRest = sampleRest,
        evalTerm = evalTerm,
        consequence = consequence
      )
      
      rec.use("candidates[0]", main.move, "Main move")
      rec.logMentions(msg)
      msg
    }.getOrElse("")

  private def renderPvConsequence(v: VariationLine, bead: Int): String = {
    val tags = v.tags
    if (tags.contains(VariationTag.Simplification)) 
      variant(bead, "trading down into a clearer position", "leading to a simplified endgame")
    else if (tags.contains(VariationTag.Sharp)) 
      pick(bead, List("entering a highly tactical phase", "where the position remains sharp and unforgiving"))
    else if (tags.contains(VariationTag.Prophylaxis))
       variant(bead, "neutralizing the opponent's counterplay", "restricting the enemy's options")
    else if (tags.contains(VariationTag.Solid)) 
      variant(bead, "maintaining a stable structural edge", "with a solid advantage")
    else ""
  }

  private def pick(seed: Int, options: List[String]): String = {
    if (options.isEmpty) ""
    else options(Math.abs(seed) % options.size)
  }

  private def getEvalTerm(ctx: NarrativeContext): String =
    val bead = Math.abs(ctx.hashCode)
    val evalScore = ctx.semantic.flatMap(_.practicalAssessment).map(_.engineScore.toDouble)
    val evalText = SemanticTranslator.evalToText(evalScore, ctx.snapshots.headOption.map(_.material).getOrElse("=")).toLowerCase

    def pick(b: Int, options: String*): String = options(b % options.length)

    evalText match {
      case s if s.contains("roughly balanced") || s.contains("equality") => 
        pick(bead, "equality", "the balance", "a level game")
      case s if s.contains("slight edge") => 
        pick(bead, "a slight edge", "a small advantage", "the initiative")
      case s if s.contains("clearly better") || s.contains("clear advantage") => 
        pick(bead, "the advantage", "the upper hand", "a clear plus")
      case s if s.contains("winning") => 
        pick(bead, "a winning position", "a decisive edge", "full control")
      case _ => "equality"
    }

  private def renderExpectedResponse(c: CandidateInfo, bead: Int): String =
    c.whyNot match {
      case Some(whyNot) if whyNot.contains("verified") => 
        NarrativeLexicon.getVerification(bead)
      case _ => 
        ""
    }

  // ============================================================
  // 4. ALTERNATIVES COMPARISON
  // ============================================================
  
  /**
   * Comparison sentences for alternatives.
   */
  private def renderAlternatives(ctx: NarrativeContext, rec: TraceRecorder): String =
    val bead = Math.abs(ctx.hashCode)
    val alternatives = ctx.candidates.drop(1).take(2)
    if (alternatives.isEmpty) return ""
    
    alternatives.foreach(c => rec.use(s"candidates[alt]", c.move, "Alternative Analysis"))

    def describe(c: CandidateInfo): String = {
      c.tags.headOption match {
        case Some(CandidateTag.Sharp) => s"the more sharp **${c.move}**"
        case Some(CandidateTag.Solid) => s"the more solid **${c.move}**"
        case Some(CandidateTag.Prophylactic) => s"a prophylactic **${c.move}**"
        case _ => s"**${c.move}**"
      }
    }

    if (alternatives.size >= 2) {
      val a1 = describe(alternatives(0))
      val v1 = renderExpectedResponse(alternatives(0), bead)
      val a2 = describe(alternatives(1))
      val v2 = renderExpectedResponse(alternatives(1), bead)
      variant(bead,
        s"Alternatively, $a1$v1 or $a2$v2 are both worth considering.",
        s"$a1$v1 and $a2$v2 remain solid alternatives.",
        s"One could also try $a1$v1, maintaining the balance."
      )
    } else {
      val c = alternatives.head
      val v = renderExpectedResponse(c, bead)
      variant(bead,
        s"Alternatively, ${describe(c)}$v is worth considering.",
        s"${describe(c)}$v remains a solid alternative.",
        s"One could also try ${describe(c)}$v."
      )
    }

  // ============================================================
  // 5. DELTA AND DECISION
  // ============================================================

  /**
   * What changed from the previous move + why this move is chosen.
   * Example: "The last move changed the pawn structure. The key is to maintain
   *           pressure while preventing counterplay."
   */
  
  private def renderDeltaAndDecision(ctx: NarrativeContext, rec: TraceRecorder): String =
    val deltaPart = ctx.delta.map { d =>
      val changes = List(
        d.structureChange.map(s => s"The structure changed: $s"),
        d.openFileCreated.map(f => s"The $f-file is now open"),
        d.phaseChange.map(p => s"The game has entered the $p")
      ).flatten
      if (changes.nonEmpty) {
        rec.use("delta", changes.head, "Delta section")
        changes.head 
      } else ""
    }.getOrElse("")
    
    val decisionPart = ctx.decision.flatMap { dr =>
      val cleanSummary = dr.logicSummary
        .replaceAll("\\(.*?\\)", "")
        .trim
        .toLowerCase
      
      // Global dedup check
      if (rec.isRedundant(cleanSummary)) {
        rec.drop("decision.logicSummary", cleanSummary, "Already mentioned globally")
        None
      } else if (cleanSummary.nonEmpty) {
        rec.use("decision.logicSummary", cleanSummary, "Decision rationale")
        rec.logMentions(cleanSummary) // Register for future deduplication
        Some(s"The key is to $cleanSummary.")
      } else {
        None
      }
    }.getOrElse("")
    
    List(deltaPart, decisionPart).filter(_.nonEmpty).mkString(" ")

  // ============================================================
  // 6. OPPONENT OPPORTUNITIES
  // ============================================================

  /**
   * What the opponent is trying to do - creates contrast.
   * Example: "Black is eyeing ...g5 to break open the kingside."
   */
  private def renderOpponentOpportunities(ctx: NarrativeContext, rec: TraceRecorder): String =
    val bead = Math.abs(ctx.hashCode)
    // FIX #3: threats.toUs = threats FROM opponent TO us = opponent's opportunities
    // (previously incorrectly used threats.toThem which means threats WE pose TO them)
    val oppThreat = ctx.threats.toUs.headOption.filter(_.lossIfIgnoredCp >= 50).map { t =>
      val threatDesc = t.square match {
        case Some(s) => 
          t.kind.toLowerCase match {
            case "positional" => s"positional pressure on $s"
            case "tactical" => s"a tactical shot on $s"
            case "material" => s"material on $s"
            case other => s"$other on $s"
          }
        case None =>
          // Phase 30: Use suppressed plan as fallback for ANY threat kind if square is None
          ctx.plans.suppressed.headOption.map(p => humanize(p.name)).getOrElse(s"${t.kind} threats")
      }
      rec.use("threats.toUs[0]", s"${t.kind} (${t.lossIfIgnoredCp}cp)", "Opponent opportunities (threats to us)")
      NarrativeLexicon.getOpponentThreat(bead, t.kind, threatDesc)
    }
    
    if (oppThreat.isEmpty && ctx.threats.toUs.nonEmpty)
      rec.drop("threats.toUs[0]", ctx.threats.toUs.head.lossIfIgnoredCp, "lossIfIgnoredCp < 50")
    
    // Suppressed plans = what we've blocked
    val blockedPlan = ctx.plans.suppressed.headOption.map { sp =>
      rec.use("plans.suppressed[0]", sp.name, "Blocked plan")
      s"${humanize(sp.name)} has been prevented (${sp.reason.toLowerCase})."
    }
    
    (oppThreat.toList ++ blockedPlan.toList).take(1).mkString(" ")


  /** 
   * Active Trace Recorder to prevent drift and manage global state (mentions).
   */
  private class TraceRecorder:
    private val entries = List.newBuilder[TraceEntry]
    private val mentionedTokens = scala.collection.mutable.Set[String]()
    
    def use(field: String, value: Any, reason: String): Unit =
      entries += TraceEntry(field, "USED", value.toString, reason)
    
    def drop(field: String, value: Any, reason: String): Unit =
      entries += TraceEntry(field, "DROPPED", value.toString, reason)
      
    def unused(field: String, value: Any, reason: String): Unit =
      entries += TraceEntry(field, "UNUSED", value.toString, reason)
      
    def empty(field: String, reason: String): Unit =
      entries += TraceEntry(field, "EMPTY", "-", reason)
      
    /** Checks if a text contains already mentioned tokens. */
    def isRedundant(text: String): Boolean = 
      val tokens = extractTokens(text)
      tokens.nonEmpty && tokens.forall(mentionedTokens.contains)
      
    /** Checks if a field has been logged as USED. */
    def isUsed(field: String): Boolean =
      entries.result().exists(e => e.field == field && e.status == "USED")

    /** Registers tokens from text as mentioned. */
    def logMentions(text: String): Unit =
      mentionedTokens ++= extractTokens(text)
      
    private def extractTokens(text: String): Set[String] =
      // Tokenize squares (e4), files (c-file), and key concepts
      val squares = "[a-h][1-8]".r.findAllIn(text).toSet
      val files = "[a-h]-file".r.findAllIn(text).toSet
      val plans = List("kingside", "queenside", "center", "attack", "defense").filter(text.contains).toSet
      squares ++ files ++ plans

    def renderTable: String =
      val header = "| Field | Status | Value | Reason |\n|-------|--------|-------|--------|"
      val rows = entries.result().map(e => s"| ${e.field} | ${e.status} | ${e.value.take(100)} | ${e.reason} |")
      (header :: rows).mkString("\n")



/**
 * Semantic Translator
 * 
 * Converts internal tags and numbers to natural language.
 */
object SemanticTranslator:
  
  def evalToText(evalCp: Option[Double], material: String): String = {
    // 1. Prioritize precise CP
    val score = evalCp.map(_ / 100.0).getOrElse {
      // 2. Fallback to material string parsing
      try {
        val cleaned = material.replaceAll("[^0-9.\\-]", "")
        if (cleaned.isEmpty) 0.0 else cleaned.toDouble
      } catch { case _: Exception => 0.0 }
    }

    // Capitalize for sentence-initial use
    if (score >= 2.0) "White is clearly better"
    else if (score >= 0.3) "White has a slight edge"
    else if (score <= -2.0) "Black is clearly better"
    else if (score <= -0.3) "Black has a slight edge"
    else "The position is roughly balanced"
  }
  
  
  /* tensionToText replaced by NarrativeLexicon.getTension */
  
  // Phase 23: Humanize Fact objects into coordinate-rich narrative
  def factToText(fact: lila.llm.model.Fact, bead: Int = 0): String = fact match {
    case f: lila.llm.model.Fact.Pin =>
      val pPart = s"${f.pinnedRole.name} on ${f.pinned}"
      val bPart = s"${f.behindRole.name} on ${f.behind}"
      if (f.isAbsolute) s"pinning the $pPart to the $bPart (absolute pin)"
      else s"pinning the $pPart to the $bPart"

    case f: lila.llm.model.Fact.Fork =>
      val targets = f.targets.map { case (sq, role) => s"${role.name} on $sq" }.mkString(" and ")
      s"creating a fork with the ${f.attackerRole.name} on ${f.attacker} against the $targets"

    case f: lila.llm.model.Fact.Skewer =>
      val fPart = s"${f.frontRole.name} on ${f.front}"
      val bPart = s"${f.backRole.name} on ${f.back}"
      s"skewering the $fPart to the $bPart"

    case f: lila.llm.model.Fact.HangingPiece =>
      s"targeting the hanging ${f.role.name} on ${f.square}"

    case f: lila.llm.model.Fact.TargetPiece =>
      s"pressuring the ${f.role.name} on ${f.square}"

    case f: lila.llm.model.Fact.WeakSquare =>
      s"taking advantage of the weak ${f.square} square (${f.reason})"

    case f: lila.llm.model.Fact.Opposition =>
      NarrativeLexicon.getOpposition(bead, f.distance, f.isDirect)

    case f: lila.llm.model.Fact.KingActivity =>
      NarrativeLexicon.getKingActivity(bead, f.mobility)

    case f: lila.llm.model.Fact.Zugzwang =>
      NarrativeLexicon.getZugzwang(bead)

    case f: lila.llm.model.Fact.PawnPromotion =>
      NarrativeLexicon.getPawnPromotion(bead, f.promotedTo)

    case f: lila.llm.model.Fact.StalemateThreat =>
      NarrativeLexicon.getStalemate(bead)

    case f: lila.llm.model.Fact.DoubleCheck =>
      NarrativeLexicon.getDoubleCheck(bead)

    case _ => "creating a tactical opportunity"
  }



  def evidenceToText(evidence: String, bead: Int = 0): String = evidence match {
    case s if s.startsWith("Domination") =>
      val pattern = """Domination\((.+) dominates (.+)\)""".r
      s match {
         case pattern(dName, vName) =>
           val dRole = chess.Role.all.find(_.name.equalsIgnoreCase(dName)).getOrElse(chess.Knight)
           val vRole = chess.Role.all.find(_.name.equalsIgnoreCase(vName)).getOrElse(chess.Bishop)
           NarrativeLexicon.getDomination(bead, dRole, vRole)
         case _ => "dominating the opponent piece"
      }
    case s if s.startsWith("Maneuver") =>
      val pattern = """Maneuver\((.+), (.+)\)""".r
      s match {
        case pattern(pName, purpose) =>
          val pRole = chess.Role.all.find(_.name.equalsIgnoreCase(pName)).getOrElse(chess.Knight)
          NarrativeLexicon.getManeuver(bead, pRole, purpose)
        case _ => "maneuvering to improve piece scope"
      }
    case s if s.startsWith("TrappedPiece") =>
      val pattern = """TrappedPiece\((.+)\)""".r
      s match {
        case pattern(roleName) =>
          val role = chess.Role.all.find(_.name.equalsIgnoreCase(roleName)).getOrElse(chess.Queen)
          NarrativeLexicon.getTrappedPiece(bead, role)
        case _ => "trapping the enemy piece"
      }
    case s if s.startsWith("KnightVsBishop") =>
      val pattern = """KnightVsBishop\((\w+), (true|false)\)""".r
      s match {
        case pattern(colorStr, betterStr) =>
           val color = chess.Color.fromName(colorStr).getOrElse(chess.White)
           val isBetter = betterStr.toBoolean
           NarrativeLexicon.getKnightVsBishop(bead, color, isBetter)
        case _ => "noting the imbalance"
      }
    case s if s.startsWith("Blockade") =>
      NarrativeLexicon.getBlockade(bead)
    case s if s.startsWith("SmotheredMate") =>
      NarrativeLexicon.getSmotheredMate(bead)
    case s if s.startsWith("Pin") =>
      val pattern = """Pin\((\w+) on ([a-h][1-8]|\?) to (\w+) on ([a-h][1-8]|\?)\)""".r
      s match {
        case pattern(pRole, pSq, bRole, bSq) => 
          if (pRole.toLowerCase == "pawn") {
             val bPart = if (bSq == "?") bRole.toLowerCase else s"${bRole.toLowerCase} on $bSq"
             s"pressuring the pawn on $pSq via the $bPart"
          } else {
            val pPart = if (pSq == "?") pRole.toLowerCase else s"${pRole.toLowerCase} on $pSq"
            val bPart = if (bSq == "?") bRole.toLowerCase else s"${bRole.toLowerCase} on $bSq"
            s"pinning the $pPart to the $bPart"
          }
        case _ => "establishing a pin"
      }
    case s if s.startsWith("Fork") =>
      val pattern = """Fork\((\w+) on ([a-h][1-8]) vs (.+)\)""".r
      s match {
        case pattern(aRole, aSq, targets) =>
          s"creating a fork on $aSq against the ${targets.toLowerCase}"
        case _ => "creating a fork"
      }
    case s if s.startsWith("Skewer") =>
      val pattern = """Skewer\((\w+) through (\w+) on ([a-h][1-8]|\?) to (\w+) on ([a-h][1-8]|\?)\)""".r
      s match {
        case pattern(aRole, fRole, fSq, bRole, bSq) =>
          if (bRole.toLowerCase == "pawn") {
             s"pressuring the ${fRole.toLowerCase} on $fSq"
          } else {
            val fPart = if (fSq == "?") fRole.toLowerCase else s"${fRole.toLowerCase} on $fSq"
            val bPart = if (bSq == "?") bRole.toLowerCase else s"${bRole.toLowerCase} on $bSq"
            s"skewering the $fPart to the $bPart"
          }
        case _ => "setting up a skewer"
      }
    case s if s.startsWith("XRay") =>
      val pattern = """XRay\((\w+) through to (\w+) on ([a-h][1-8])\)""".r
      s match {
        case pattern(role, target, sq) =>
          val targetRole = chess.Role.all.find(_.name.equalsIgnoreCase(target)).getOrElse(chess.Queen)
          NarrativeLexicon.getXRay(bead, targetRole)
        case _ => "exerting x-ray pressure"
      }
    case s if s.startsWith("Battery") =>
       val pattern = """Battery\((\w+) and (\w+)\)""".r
       s match {
         case pattern(front, back) =>
            val fRole = chess.Role.all.find(_.name.equalsIgnoreCase(front)).getOrElse(chess.Queen)
            val bRole = chess.Role.all.find(_.name.equalsIgnoreCase(back)).getOrElse(chess.Rook)
            NarrativeLexicon.getBattery(bead, fRole, bRole)
         case _ => "forming an attacking battery"
       }
    case s if s.contains("WinningCapture") =>
      val pattern = """.*WinningCapture\((\w+) takes (\w+) on ([a-h][1-8])\).*""".r
      s match {
        case pattern(aRole, vRole, vSq) => 
          s"winning the ${vRole.toLowerCase} on $vSq"
        case _ => "winning material"
      }
    case s if s.contains("Check") =>
      val pattern = """.*Check\((\w+) on ([a-h][1-8])\).*""".r
      s match {
        case pattern(role, sq) => s"giving check on $sq"
        case _ => "giving check"
      }
    case s if s.contains("DiscoveredAttack") =>
      val pattern = """DiscoveredAttack\((\w+) from ([a-h][1-8]|\?) on (\w+) on ([a-h][1-8]|\?)\)""".r
      s match {
        case pattern(aRole, aSq, tRole, tSq) =>
          val aPart = if (aSq == "?") aRole.toLowerCase else s"${aRole.toLowerCase} from $aSq"
          val tPart = if (tSq == "?") tRole.toLowerCase else s"${tRole.toLowerCase} on $tSq"
          s"unleashing a discovered attack by the $aPart on the $tPart"
        case _ => "unleashing a discovered attack"
      }
    case s if s.startsWith("Outpost") =>
      val pattern = """Outpost\((\w+) on ([a-h][1-8])\)""".r
      s match {
        case pattern(role, sq) => s"establishing a ${role.toLowerCase} outpost on $sq"
        case _ => "establishing an outpost"
      }
    case s if s.startsWith("OpenFileControl") =>
      val pattern = """OpenFileControl\(Rook on ([a-h])-file\)""".r
      s match {
        case pattern(f) => s"controlling the open $f-file"
        case _ => "controlling the open file"
      }
    case s if s.startsWith("Centralization") =>
      val pattern = """Centralization\((\w+) on ([a-h][1-8])\)""".r
      s match {
        case pattern(role, sq) => s"centralizing the ${role.toLowerCase} on $sq"
        case _ => "centralizing the piece"
      }
    case s if s.startsWith("RookLift") =>
      val pattern = """RookLift\(Rook to rank ([1-8])\)""".r
      s match {
        case pattern(r) => s"activating the rook with a lift to the ${r}th rank"
        case _ => "lifting the rook"
      }
    case s if s.contains("fork") => "creating a fork"
    case s if s.contains("pin") => "establishing a pin"
    case s if s.contains("skewer") => "setting up a skewer"
    case s if s.contains("discovered") => "unleashing a discovered attack"
    case s if s.contains("capture") => "winning material"
    case s if s.contains("check") => "giving check"
    case s if s.contains("diagonal") => "opening the diagonal"
    case s if s.contains("file") => "controlling the open file"
    case _ => evidence.replaceAll("[()]", "").toLowerCase
  }

  def humanizePlan(plan: String): String = plan.toLowerCase match {
    case s if s.contains("attack") => s.replaceAll("aligned", "").trim
    case s if s.contains("control") => 
      val target = s.replaceAll("control", "").trim
      if (target.contains("central")) "control of the center"
      else s"control of the $target"
    case s if s.contains("development") => "development of the pieces"
    case s if s.contains("consolidation") => "defensive consolidation"
    case s if s.contains("prophylaxis") => "positional prophylaxis"
    case _ => plan.toLowerCase.replaceAll("good", "").trim
  }
