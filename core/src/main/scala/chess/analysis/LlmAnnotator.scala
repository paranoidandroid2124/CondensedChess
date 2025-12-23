package chess
package analysis

import ujson.*
import AnalyzeUtils.{ pvToSan, uciToSanSingle }

import scala.concurrent.{ Future, Await, ExecutionContext }
import scala.concurrent.duration.*
import java.util.concurrent.Executors

/** Lightweight LLM annotator: single-call helpers for summary, mainline short comments,
  * and critical comments. All calls are optional (skip if key missing or failure).
  * Parsing is best-effort; JSON mode 응답을 기대하지만 파싱 실패 시 무시합니다.
  */
object LlmAnnotator:
  private val executor = Executors.newCachedThreadPool()
  private given ExecutionContext = ExecutionContext.fromExecutor(executor)

  private def clampNodes[T](xs: Seq[T], max: Int): Seq[T] = xs.take(max)
  private def pctInt(d: Double): Int = math.round(if d > 1.0 then d else d * 100.0).toInt
  private def round2(d: Double): Double = math.round(d * 10.0) / 10.0
  private def colorName(color: Color): String = if color == Color.White then "White" else "Black"
  private def arrStr(values: Iterable[String]): ujson.Arr = ujson.Arr.from(values.map(ujson.Str(_)))
  private def forcedFlag(legalMoves: Int, gapOpt: Option[Double]): Boolean =
    legalMoves <= 1 || gapOpt.exists(_ >= 20.0)

  private def moveLabel(ply: Ply, san: String, turn: Color): String =
    val moveNumber = (ply.value + 1) / 2
    val sep = if turn == Color.White then "." else "..."
    s"$moveNumber$sep $san"

  private def labelForPly(timelineByPly: Map[Int, AnalyzePgn.PlyOutput], ply: Int): String =
    timelineByPly
      .get(ply)
      .map(t => moveLabel(t.ply, t.san, t.turn))
      .getOrElse(s"ply $ply")

  private def labelSafe(text: String, maxMoveNumber: Int): Boolean =
    // 허용 범위를 벗어난 수 번호가 등장하면 폐기
    // IMPORTANT: 백분율("86.6%")이 "86."으로 오인되지 않도록, 뒤에 공백이나 텍스트가 오는 경우만 매칭 (negative lookahead/boundary)
    // Old: "(\\d+)\\.{1,3}" -> matches "86." in "86.6%"
    // New: "(?:^|\\s)(\\d+)\\.+(?=\\s)" -> requires space after dot
    val movePattern = "(?:^|\\s)(\\d+)\\.+(?=\\s)".r
    val nums = movePattern.findAllMatchIn(text).flatMap(m => m.group(1).toIntOption).toList
    val result = nums.isEmpty || nums.forall(n => n >= 1 && n <= maxMoveNumber)
    if !result then
      System.err.println(s"[llm-labelSafe] REJECTED text (maxMove=$maxMoveNumber, found=$nums): ${text.take(100)}...")
    result

  // private def filterWithLog[A](data: Map[Int, String], maxMoveNumber: Int, label: String): Map[Int, String] =
  //   data.flatMap { case (k, v) =>
  //     if labelSafe(v, maxMoveNumber) then Some(k -> v)
  //     else
  //       System.err.println(s"[llm-$label] drop comment for ply=$k due to out-of-range move reference")
  //       None
  //   }

  /** Simple text cleanup - no validation, no rejection. Just basic formatting fixes. */
  private def cleanText(text: String): String =
    AntiFluffGate.semiClean(text)

  /** Convert PositionalTags to structured JSON for LLM (Phase 2 enhancement) */
  private def positionalTagsToJson(tags: List[ConceptLabeler.PositionalTag]): ujson.Arr =
    import ConceptLabeler.PositionalTag.*
    val facts = tags.take(4).flatMap {
      case WeakSquare(sq, side) => Some(Obj("type" -> "WeakSquare", "square" -> sq, "side" -> colorName(side)))
      case Outpost(sq, side) => Some(Obj("type" -> "Outpost", "square" -> sq, "side" -> colorName(side)))
      case OpenFile(file, side) => Some(Obj("type" -> "OpenFile", "file" -> file, "side" -> colorName(side)))
      case LoosePiece(sq, side) => Some(Obj("type" -> "LoosePiece", "square" -> sq, "side" -> colorName(side)))
      case WeakBackRank(side) => Some(Obj("type" -> "WeakBackRank", "side" -> colorName(side)))
      case KingSafetyCrisis(side) => Some(Obj("type" -> "KingSafetyCrisis", "side" -> colorName(side)))
      case _ => None
    }
    ujson.Arr.from(facts)

  /** Extract core geometry facts for LLM (Phase 2 enhancement) */
  private def geometryToJson(features: FeatureExtractor.PositionFeatures): ujson.Obj =
    val g = features.geometry
    val obj = Obj(
      "whiteKing" -> Str(g.whiteKingSquare),
      "blackKing" -> Str(g.blackKingSquare)
    )
    if g.whiteOpenFilesNearKing.nonEmpty then obj("openFilesNearWhiteKing") = arrStr(g.whiteOpenFilesNearKing)
    if g.blackOpenFilesNearKing.nonEmpty then obj("openFilesNearBlackKing") = arrStr(g.blackOpenFilesNearKing)
    if g.whiteKingAttackers.nonEmpty then obj("whiteKingAttackers") = Num(g.whiteKingAttackers.size)
    if g.blackKingAttackers.nonEmpty then obj("blackKingAttackers") = Num(g.blackKingAttackers.size)
    obj

  /** Sparse serialization of PositionFeatures - only non-default values (Phase 2.5) */
  private def sparsePositionFeatures(pf: FeatureExtractor.PositionFeatures): ujson.Obj =
    val obj = Obj()
    val p = pf.pawns
    val a = pf.activity
    val k = pf.kingSafety
    val m = pf.materialPhase
    val t = pf.tactics
    val c = pf.coordination
    
    // Pawn Structure (only significant values)
    if p.whiteIQP then obj("whiteIQP") = Bool(true)
    if p.blackIQP then obj("blackIQP") = Bool(true)
    if p.whitePassedPawns > 0 then obj("whitePassedPawns") = Num(p.whitePassedPawns)
    if p.blackPassedPawns > 0 then obj("blackPassedPawns") = Num(p.blackPassedPawns)
    if p.whiteIsolatedPawns > 0 then obj("whiteIsolatedPawns") = Num(p.whiteIsolatedPawns)
    if p.blackIsolatedPawns > 0 then obj("blackIsolatedPawns") = Num(p.blackIsolatedPawns)
    if p.whiteDoubledPawns > 0 then obj("whiteDoubledPawns") = Num(p.whiteDoubledPawns)
    if p.blackDoubledPawns > 0 then obj("blackDoubledPawns") = Num(p.blackDoubledPawns)
    if p.whiteHangingPawns then obj("whiteHangingPawns") = Bool(true)
    if p.blackHangingPawns then obj("blackHangingPawns") = Bool(true)
    
    // King Safety (critical values only)
    if k.whiteBackRankWeak then obj("whiteBackRankWeak") = Bool(true)
    if k.blackBackRankWeak then obj("blackBackRankWeak") = Bool(true)
    if k.whiteKingExposedFiles > 0 then obj("whiteKingExposed") = Num(k.whiteKingExposedFiles)
    if k.blackKingExposedFiles > 0 then obj("blackKingExposed") = Num(k.blackKingExposedFiles)
    if k.whiteKingRingEnemyPieces >= 2 then obj("whiteKingPressure") = Num(k.whiteKingRingEnemyPieces)
    if k.blackKingRingEnemyPieces >= 2 then obj("blackKingPressure") = Num(k.blackKingRingEnemyPieces)
    
    // Tactics (hanging pieces)
    if t.whiteHangingPieces > 0 then obj("whiteHangingPieces") = Num(t.whiteHangingPieces)
    if t.blackHangingPieces > 0 then obj("blackHangingPieces") = Num(t.blackHangingPieces)
    if t.whiteLoosePieces > 0 then obj("whiteLoosePieces") = Num(t.whiteLoosePieces)
    if t.blackLoosePieces > 0 then obj("blackLoosePieces") = Num(t.blackLoosePieces)
    
    // Coordination
    if c.whiteRookOn7th then obj("whiteRookOn7th") = Bool(true)
    if c.blackRookOn7th then obj("blackRookOn7th") = Bool(true)
    if c.whiteRooksBehindPassedPawns > 0 then obj("whiteRooksBehindPP") = Num(c.whiteRooksBehindPassedPawns)
    if c.blackRooksBehindPassedPawns > 0 then obj("blackRooksBehindPP") = Num(c.blackRooksBehindPassedPawns)
    
    // Activity (significant differences only)
    val mobilityDiff = a.whiteLegalMoves - a.blackLegalMoves
    if math.abs(mobilityDiff) >= 5 then obj("mobilityAdvantage") = Str(if mobilityDiff > 0 then "White" else "Black")
    if a.whiteKnightOutposts > 0 then obj("whiteOutposts") = Num(a.whiteKnightOutposts)
    if a.blackKnightOutposts > 0 then obj("blackOutposts") = Num(a.blackKnightOutposts)
    
    // Material
    if m.materialDiff != 0 then obj("materialDiff") = Num(m.materialDiff)
    obj("phase") = Str(m.phase)
    
    obj

  /** Sparse serialization of ConceptLabels - only non-empty tags (Phase 2.5) */
  private def sparseConceptLabels(cl: ConceptLabeler.ConceptLabels): ujson.Obj =
    val obj = Obj()
    
    // Structure tags
    if cl.structureTags.nonEmpty then
      obj("structure") = arrStr(cl.structureTags.take(3).map(_.toString))
    
    // Plan tags
    if cl.planTags.nonEmpty then
      obj("plans") = arrStr(cl.planTags.take(3).map(_.toString))
    
    // Tactics tags
    if cl.tacticTags.nonEmpty then
      obj("tactics") = arrStr(cl.tacticTags.take(3).map(_.toString))
    
    // Mistake tags
    if cl.mistakeTags.nonEmpty then
      obj("mistakes") = arrStr(cl.mistakeTags.take(2).map(_.toString))
    
    // Endgame tags
    if cl.endgameTags.nonEmpty then
      obj("endgame") = arrStr(cl.endgameTags.take(2).map(_.toString))
    
    // Transition tags
    if cl.transitionTags.nonEmpty then
      obj("transition") = arrStr(cl.transitionTags.take(2).map(_.toString))
    
    obj


  def annotate(output: AnalyzePgn.Output): AnalyzePgn.Output =
    val timelineByPly = output.timeline.map(t => t.ply.value -> t).toMap
    
    val mainlineMax = output.timeline.lastOption.map(_.ply.value).getOrElse(0)
    
    val criticalMax = output.critical.map { c =>
      val maxBranchLen = if c.branches.nonEmpty then c.branches.map(_.pv.length).max else 0
      c.ply.value + maxBranchLen
    }.maxOption.getOrElse(0)

    val studyMax = output.studyChapters.flatMap { ch =>
       ch.lines.map(l => ch.anchorPly + l.pv.length)
    }.maxOption.getOrElse(0)

    val maxPly = math.max(mainlineMax, math.max(criticalMax, studyMax))
    // Add a buffer of 2 plies just in case of off-by-one or immediate follow-ups
    val maxMoveNumber = math.max(1, (maxPly + 2 + 1) / 2)
    val preview = renderPreview(output, timelineByPly)
    val critPreview = criticalPreview(output, timelineByPly)

    // Parallelize LLM calls with individual recovery
    val summaryFuture = {
      System.err.println("[LlmAnnotator] Starting summary generation...")
      LlmClient.summarize(preview).map { 
        case Some(s) => Some(cleanText(s))
        case None => 
          System.err.println("[LlmAnnotator] Summary empty/invalid, using fallback.")
          fallbackSummary(output)
      }.recover { case e: Throwable =>
        System.err.println(s"[LlmAnnotator] Summary failed: ${e.getMessage}")
        fallbackSummary(output)
      }
    }
    
    val criticalFuture = {
      System.err.println("[LlmAnnotator] Starting critical comments generation...")
      LlmClient.criticalComments(critPreview).map { raw =>
        val res = raw.flatMap { case (ply, ann) => 
          if labelSafe(ann.main, maxMoveNumber) then
             Some(ply -> ann.copy(main = cleanText(ann.main)))
          else
             System.err.println(s"[LlmAnnotator] Critical(ply=$ply) rejected for unsafe labels: ${ann.main}")
             None
        }
        System.err.println(s"[LlmAnnotator] Critical comments generation finished. Count: ${res.size}")
        res
      }.recover { case e: Throwable =>
        System.err.println(s"[LlmAnnotator] Critical comments failed: ${e.getMessage}")
        Map.empty[Int, LlmClient.CriticalAnnotation]
      }
    }

    val studyFuture = {
      System.err.println("[LlmAnnotator] Starting study chapter comments generation...")
      if output.studyChapters.nonEmpty then 
        LlmClient.studyChapterComments(preview).map { raw =>
          val res = raw.map { case (id, ann) =>
            id -> ann.copy(summary = cleanText(ann.summary))
          }
          System.err.println(s"[LlmAnnotator] Study chapter comments generation finished. Count: ${res.size}")
          res
        }
      else Future.successful(Map.empty[String, LlmClient.ChapterAnnotation])
    }.recover { case e: Throwable =>
      System.err.println(s"[LlmAnnotator] Study comments failed: ${e.getMessage}")
      Map.empty[String, LlmClient.ChapterAnnotation]
    }

    val bookFuture = {
      System.err.println("[LlmAnnotator] Starting book section narrative generation...")
      output.book match
        case Some(book) =>
          val sectionFutures = book.sections.zipWithIndex.map { case (section, _) =>
            val segment = output.timeline.filter(p => p.ply.value >= section.startPly && p.ply.value <= section.endPly)
            val prompt = NarrativeTemplates.buildSectionPrompt(section.sectionType, section.title, segment, section.diagrams)
            
            LlmClient.bookSectionNarrative(prompt).map {
              case Some(sn) =>
                val cleanNarrative = cleanText(sn.narrative)
                System.err.println(s"[LlmAnnotator] SUCCESS: Book section '${section.title}' generated. Length: ${cleanNarrative.length}")
                section.copy(
                   title = sn.title.getOrElse(section.title),
                   narrativeHint = cleanNarrative,
                   metadata = sn.metadata
                )
              case _ => section
            }
          }
          
          Future.sequence(sectionFutures).map { annotatedSections =>
            Some(book.copy(sections = annotatedSections))
          }
        case None => Future.successful(None)
    }.recover { case e: Throwable =>
       System.err.println(s"[LlmAnnotator] Book processing failed: ${e.getMessage}")
       output.book 
    }

    val (summary, criticalComments, studyAnnotations, updatedBook) = 
      try
        Await.result(
          for
            s <- summaryFuture
            c <- criticalFuture
            st <- studyFuture
            b <- bookFuture
          yield (s, c, st, b),
          120.seconds
        )
      catch
        case e: Throwable =>
          System.err.println(s"[LlmAnnotator] Await failed (timeout?): ${e.getMessage}")
          (fallbackSummary(output), Map.empty[Int, LlmClient.CriticalAnnotation], Map.empty[String, LlmClient.ChapterAnnotation], output.book)

    // Merge critical comments with key move comments from chapters
    // Priority: Critical > Chapter Key Move
    // Critical comments are Ply-based (mainline only), so we convert them to (Ply, SAN)
    val criticalCommentsWithSan = criticalComments.flatMap { case (ply, annotation) =>
      val mainlineEntry = timelineByPly.get(ply).map(t => (ply, t.san) -> annotation.main)
      
      // Map variation comments
      val variationEntries = output.critical.find(_.ply.value == ply).map { node =>
        val fenBefore = timelineByPly.get(ply).map(_.fenBefore).getOrElse("")
        node.branches.flatMap { branch =>
           // Avoid duplication if variation is same as played move
           val isPlayed = timelineByPly.get(ply).exists(_.uci == branch.move)
           if isPlayed then None
           else
             val san = if fenBefore.nonEmpty then uciToSanSingle(fenBefore, branch.move) else branch.move
             annotation.variations.get(branch.label).map { comment =>
               (ply, san) -> comment
             }
        }
      }.getOrElse(Nil)

      mainlineEntry.toSeq ++ variationEntries
    }
    
    val chapterMoveComments = studyAnnotations.values.flatMap(_.keyMoves).toMap
    val mergedComments = chapterMoveComments ++ criticalCommentsWithSan // critical overrides if overlap

    val updatedTimeline = output.timeline.map { t =>
      t.copy(shortComment = None) // suppress per-move one-liners
    }
    
    val updatedCritical = output.critical.map { c =>
      val annotationOpt = criticalComments.get(c.ply.value)
      
      // Map variation comments
      val updatedBranches = c.branches.map { b =>
         val varComment = annotationOpt.flatMap(_.variations.get(b.label)).orElse(
             // Fallback: try matching by move SAN keys if label key missing?
             // But prompt asked for label keys usually.
             // We can try to use label.
             None
         )
         b.copy(comment = varComment)
      }
      
      c.copy(
          comment = c.comment.orElse(annotationOpt.map(_.main)),
          branches = updatedBranches
      )
    }
    
    // Apply merged comments to the tree
    val updatedRoot = output.root.map(applyTreeComments(_, mergedComments))

    val updatedChapters =
      if studyAnnotations.nonEmpty then
        output.studyChapters.map { ch =>
          studyAnnotations.get(ch.id) match
            case Some(ann) if ann.summary.nonEmpty => 
              ch.copy(
                summary = Some(ann.summary),
                metadata = Some(AnalysisModel.StudyChapterMetadata(ann.title, ann.summary)),
                rootNode = ch.rootNode.map(applyTreeComments(_, mergedComments))
              )
            case _ => 
              // Even if no specific chapter annotation, we should apply global critical comments
              ch.copy(rootNode = ch.rootNode.map(applyTreeComments(_, mergedComments)))
        }
      else output.studyChapters

    output.copy(
      summaryText = output.summaryText.orElse(summary),
      timeline = updatedTimeline,
      critical = updatedCritical,
      root = updatedRoot,
      studyChapters = updatedChapters,
      book = updatedBook
    )

  private[analysis] def renderPreview(output: AnalyzePgn.Output, timelineByPly: Map[Int, AnalyzePgn.PlyOutput]): String =
    val openingObj = Obj("name" -> Str(output.opening.map(_.opening.name.value).getOrElse("unknown")))
    output.opening.foreach(op => openingObj("bookToPly") = Num(op.ply.value))
    output.openingStats.foreach { os =>
      val stats = Obj(
        "bookPly" -> Num(os.bookPly),
        "noveltyPly" -> Num(os.noveltyPly),
        "source" -> Str(os.source)
      )
      os.games.foreach(g => stats("games") = Num(g))
      os.winWhite.foreach(w => stats("winWhite") = Num(pctInt(w)))
      os.winBlack.foreach(b => stats("winBlack") = Num(pctInt(b)))
      os.draw.foreach(d => stats("draw") = Num(pctInt(d)))
      os.minYear.foreach(y => stats("minYear") = Num(y))
      os.maxYear.foreach(y => stats("maxYear") = Num(y))
      if os.yearBuckets.nonEmpty then
        val buckets = os.yearBuckets.map { case (k, v) => k -> Num(v) }
        stats("yearBuckets") = Obj.from(buckets)
      os.topMoves.headOption.foreach { tm =>
        val tmObj = Obj("san" -> Str(tm.san), "games" -> Num(tm.games))
        tm.winPct.foreach(w => tmObj("winPct") = Num(pctInt(w)))
        tm.drawPct.foreach(d => tmObj("drawPct") = Num(pctInt(d)))
        stats("topMove") = tmObj
      }
      openingObj("stats") = stats
    }
    output.openingSummary.foreach(s => openingObj("summary") = Str(s))
    output.bookExitComment.foreach(s => openingObj("bookExitComment") = Str(s))
    output.openingTrend.foreach(s => openingObj("trend") = Str(s))

    val topSwings = output.timeline
      .sortBy(t => -math.abs(t.deltaWinPct))
      .take(8)
      .map { t =>
        // Optimized: removed ply, turn, legalMoves, epLoss, winAfter (redundant/derivable)
        val wasOnlyMove = t.legalMoves <= 1 || t.bestVsSecondGap.exists(_ >= 20.0)
        val obj = Obj(
          "label" -> Str(moveLabel(t.ply, t.san, t.turn)),
          "judgement" -> Str(t.judgement),
          "deltaWinPct" -> Num(round2(t.deltaWinPct)),
          "winBefore" -> Num(round2(t.winPctBefore)),
          "semanticTags" -> arrStr(t.semanticTags.take(6)),
          "forced" -> Bool(forcedFlag(t.legalMoves, t.bestVsSecondGap))
        )
        if wasOnlyMove then obj("wasOnlyMove") = Bool(true)
        val shiftArr = conceptShifts(t.conceptDelta, limit = 2)
        if shiftArr.value.nonEmpty then obj("conceptShift") = shiftArr
        t.phaseLabel.foreach(ph => obj("phaseLabel") = Str(ph))
        t.mistakeCategory.foreach(mc => obj("mistakeCategory") = Str(mc))
        t.special.foreach(s => obj("special") = Str(s))
        
        // Tactical Context
        if t.materialDiff != 0 then obj("materialDiff") = Num(round2(t.materialDiff))
        t.tacticalMotif.foreach(m => obj("tacticalMotif") = Str(m))

        // Phase 3: Enhanced Evidence Injection
        t.conceptLabels.foreach { cl =>
          if cl.richTags.nonEmpty then
             val evidenceArr = cl.richTags.distinct.take(3).map { rt =>
               val refObj = Obj("id" -> Str(rt.id), "category" -> Str(rt.category.toString), "score" -> Num(rt.score))
               // Resolve details from EvidencePack
               rt.evidenceRefs.headOption.foreach { ref =>
                  ref.kind match
                    case "structure" => cl.evidence.structure.get(ref.id).foreach { e =>
                      refObj("detail") = Str(e.description)
                      refObj("squares") = arrStr(e.squares)
                    }
                    case "plans" => cl.evidence.plans.get(ref.id).foreach { e =>
                      refObj("concept") = Str(e.concept)
                      refObj("goal") = Str(e.goal)
                      refObj("pv") = arrStr(e.pv.take(5))
                    }
                    case "tactics" => cl.evidence.tactics.get(ref.id).foreach { e =>
                      refObj("motif") = Str(e.motif)
                      refObj("sequence") = arrStr(e.sequence)
                    }
                    case _ => 
               }
               refObj
             }
             obj("evidence") = Arr.from(evidenceArr)
              
              // Phase 2: Add structured positional facts (squares, files)
              if cl.positionalTags.nonEmpty then
                val posFacts = positionalTagsToJson(cl.positionalTags)
                if posFacts.value.nonEmpty then obj("positionalFacts") = posFacts
        }
        
        // Phase 2: Add geometry if fullFeatures available
        t.fullFeatures.foreach { ff =>
          val geo = geometryToJson(ff)
          obj("geometry") = geo
        }
        
        // Phase 2: Add hypotheses ("why not" variations)
        if t.hypotheses.nonEmpty then
          val hypos = t.hypotheses.take(2).map { h =>
            val sanMove = uciToSanSingle(t.fenBefore, h.move)
            val pvSan = pvToSan(t.fenBefore, h.pv).take(5)
            Obj(
              "move" -> Str(sanMove),
              "label" -> Str(h.label),
              "pv" -> arrStr(pvSan),
              "winPct" -> Num(round2(h.winPct))
            )
          }
          obj("hypotheses") = Arr.from(hypos)
        
        // Phase 2.5: Sparse transmission - full features with non-default values only
        t.fullFeatures.foreach { ff =>
          val sparse = sparsePositionFeatures(ff)
          if sparse.value.nonEmpty then obj("features") = sparse
        }
        
        // Phase 2.5: Sparse conceptLabels
        t.conceptLabels.foreach { cl =>
          val sparse = sparseConceptLabels(cl)
          if sparse.value.nonEmpty then obj("concepts") = sparse
        }

        obj
      }

    val criticalNodes = clampNodes(output.critical, 16).map { c =>
      val label = labelForPly(timelineByPly, c.ply.value)
      val branches = c.branches.take(3).map { b =>
        val fenForBranch = timelineByPly.get(c.ply.value).map(_.fenBefore).getOrElse("")
        val sanMove = if fenForBranch.nonEmpty then uciToSanSingle(fenForBranch, b.move) else b.move
        val pvSan = if fenForBranch.nonEmpty then pvToSan(fenForBranch, b.pv).take(8) else b.pv.take(8)
        Obj(
          "label" -> Str(b.label),
          "move" -> Str(sanMove),
          "pv" -> arrStr(pvSan),
          "winPct" -> Num(round2(b.winPct))
        )
      }
      // Optimized: removed ply, winAfter, epLoss, legalMoves, bestVsGaps (redundant)
      val obj = Obj(
        "label" -> Str(label),
        "reason" -> Str(c.reason),
        "deltaWinPct" -> Num(round2(c.deltaWinPct)),
        "branches" -> Arr.from(branches)
      )
      val wasOnlyMove = c.legalMoves.exists(_ <= 1) || c.bestVsSecondGap.exists(_ >= 20.0)
      if wasOnlyMove then obj("wasOnlyMove") = Bool(true)
      
      timelineByPly.get(c.ply.value).foreach { t =>
        obj("winBefore") = Num(round2(t.winPctBefore))
        obj("forced") = Bool(c.forced || forcedFlag(t.legalMoves, c.bestVsSecondGap.orElse(t.bestVsSecondGap)))
        val shiftArr = conceptShifts(t.conceptDelta, limit = 2)
        if shiftArr.value.nonEmpty then obj("conceptShift") = shiftArr
        t.phaseLabel.foreach(ph => obj("phaseLabel") = Str(ph))
        if t.materialDiff != 0 then obj("materialDiff") = Num(round2(t.materialDiff))
        t.tacticalMotif.foreach(m => obj("tacticalMotif") = Str(m))
      }
      obj("forced") = Bool(c.forced || c.legalMoves.exists(_ <= 1))
      if c.tags.nonEmpty then obj("tags") = arrStr(c.tags.take(6))
      c.mistakeCategory.foreach(mc => obj("mistakeCategory") = Str(mc))
      obj
    }

    val instructions = Obj(
      "goal" -> Str("Coach-style 3-4 sentence recap grounded in the provided review data."),
      "style" -> Str("Narrative and concise: describe what could change, what was chosen, and consequences; no inventions."),
      "rules" -> Arr.from(
        List(
          "Follow the instructions block plus rules; use only provided data.",
          "Reference move labels exactly (e.g., \"13. Qe2\", \"12...Ba6\"); never renumber.",
          "Lean on semanticTags/mistakeCategory/conceptShift/critical.reason to explain why evaluations shifted.",
          "Mention opening/book/novelty briefly when stats are present; if opening.summary/bookExitComment/trend is present, include one sentence.",
          "Spell out forced/only-move or large best-vs-second gaps first; avoid generic advice.",
          "PRIORITIZE tactical details (Material Loss, Missed Win, tacticalMotif) over abstract tags like 'Pawn Storm'. If tacticalMotif is present, explain IT.",
          "If a line is labeled 'practical', explicitly mention it as a 'Practical Choice' and explain why it might be easier/safer than the engine best.",
          "If 'Plan Change' tag is present, explicitly state: 'Black shifted focus from [Prev Dominant Concept] to [New Concept]' using conceptShift values.",
          "If 'evidence' array is present, YOU MUST CITE IT for key claims. E.g. 'White targets the backward pawn (Evidence: Structure d4)'.",
          "NATURALIZE TAGS: Translate technical tags (e.g. 'Pawn Storm') into descriptive prose. NEVER output raw tags."
        ).map(Str(_))
      )
    )

    val payload = Obj(
      "instructions" -> instructions,
      "opening" -> openingObj,
      "oppositeColorBishops" -> Bool(output.oppositeColorBishops),
      "timelineSize" -> Num(output.timeline.size),
      "keySwings" -> Arr.from(topSwings),
      "critical" -> Arr.from(criticalNodes)
    )
    if output.studyChapters.nonEmpty then
      val chapters = output.studyChapters.take(12).map { ch =>
        val turn = timelineByPly.get(ch.anchorPly).map(_.turn).getOrElse(if ch.anchorPly % 2 == 1 then Color.White else Color.Black)
        val label = labelForPly(timelineByPly, ch.anchorPly)
        val obj = Obj(
          "id" -> Str(ch.id),
          "anchorPly" -> Num(ch.anchorPly),
          "label" -> Str(label),
          "turn" -> Str(colorName(turn)),
          "played" -> Str(ch.played),
          "deltaWinPct" -> Num(round2(ch.deltaWinPct)),
          "winPctBefore" -> Num(round2(ch.winPctBefore)),
          "winPctAfter" -> Num(round2(ch.winPctAfter)),
          "phase" -> Str(ch.phase),
          "studyScore" -> Num(round2(ch.studyScore)),
          "tags" -> arrStr(ch.tags.take(6))
        )
        ch.best.foreach(b => obj("best") = Str(b))
        if ch.lines.nonEmpty then
          val lines = ch.lines.take(4).map { l =>
            Obj(
              "label" -> Str(l.label),
              "pv" -> arrStr(l.pv.take(6)),
              "winPct" -> Num(round2(l.winPct))
            )
          }
          obj("lines") = Arr.from(lines)
        ch.practicality.foreach { p =>
          obj("practicality") = Obj(
            "overall" -> Num(round2(p.overall)),
            "categoryGlobal" -> Str(p.categoryGlobal),
            "categoryPersonal" -> p.categoryPersonal.map(Str(_)).getOrElse(Null)
          )
        }
        timelineByPly.get(ch.anchorPly).foreach { t =>
          val shiftArr = conceptShifts(t.conceptDelta, limit = 2)
          if shiftArr.value.nonEmpty then obj("conceptShift") = shiftArr
        }
        obj
      }
      payload("studyChapters") = Arr.from(chapters)
    output.timeline.lastOption.foreach { last =>
      payload("finalFrame") = Obj(
        "label" -> Str(moveLabel(last.ply, last.san, last.turn)),
        "evalAfter" -> Num(round2(last.winPctAfterForPlayer)),
        "judgement" -> Str(last.judgement)
      )
    }

    ujson.write(payload)

  private def fallbackSummary(output: AnalyzePgn.Output): Option[String] =
    val topCrit = output.critical.headOption
    val openingName = output.opening.map(_.opening.name.value)
    val openingPart = openingName.map(n => s"Opening: $n.").getOrElse("")
    val critPart = topCrit.map(c => s"Big swing at ply ${c.ply.value}: ${c.reason}.").getOrElse("")
    val endPart = output.timeline.lastOption.map(t => s"Finished at ${t.judgement} after ${t.ply.value} plies.").getOrElse("")
    val text = List(openingPart, critPart, endPart).filter(_.nonEmpty).mkString(" ")
    if text.nonEmpty then Some(text) else None

  private[analysis] def criticalPreview(output: AnalyzePgn.Output, timelineByPly: Map[Int, AnalyzePgn.PlyOutput]): String =
    val instructions = Obj(
      "goal" -> Str("Return JSON array of {\"label\":string,\"comment\":string} covering each critical move."),
      "style" -> Str("Crisp coach notes: Heading | Why(delta/wasOnlyMove/conceptShift) | Alternatives(PV labels) | Consequence."),
      "rules" -> Arr.from(
            List(
              "Use provided labels; do not invent moves.",
              "Base severity on deltaWinPct/judgement/mistakeCategory; stay consistent with tags.",
              "Cite branches/pv for refutations without fabricating new lines.",
              "If wasOnlyMove is true, emphasize it was the only reasonable choice.",
              "NATURALIZE TAGS: Translate technical tags (e.g. 'Pawn Storm', 'Tactical Complexity') into descriptive prose (e.g. 'furious pawn avalanche', 'sharp tactical complications'). NEVER output raw tags."
            ).map(Str(_))
          )
        )

    val nodes = clampNodes(output.critical, 16).map { c =>
      val timeline = timelineByPly.get(c.ply.value)
      val label = labelForPly(timelineByPly, c.ply.value)
      val mergedTags = (timeline.map(_.semanticTags).getOrElse(Nil) ++ c.tags).distinct.take(6)
      val branches = c.branches.take(3).map { b =>
        val fenForBranch = timelineByPly.get(c.ply.value).map(_.fenBefore).getOrElse("")
        val sanMove = if fenForBranch.nonEmpty then uciToSanSingle(fenForBranch, b.move) else b.move
        val pvSan = if fenForBranch.nonEmpty then pvToSan(fenForBranch, b.pv).take(8) else b.pv.take(8)
        Obj(
          "label" -> Str(b.label),
          "move" -> Str(sanMove),
          "pv" -> arrStr(pvSan),
          "winPct" -> Num(round2(b.winPct))
        )
      }
      // Optimized: removed ply, turn, winAfter, epLoss, legalMoves, bestVsGaps
      val obj = Obj(
        "label" -> Str(label),
        "reason" -> Str(c.reason),
        "deltaWinPct" -> Num(round2(c.deltaWinPct)),
        "branches" -> Arr.from(branches)
      )
      val wasOnlyMove = c.legalMoves.exists(_ <= 1) || c.bestVsSecondGap.exists(_ >= 20.0)
      if wasOnlyMove then obj("wasOnlyMove") = Bool(true)
      
      timeline.foreach { t =>
        obj("judgement") = Str(t.judgement)
        obj("winBefore") = Num(round2(t.winPctBefore))
        obj("forced") = Bool(c.forced || forcedFlag(t.legalMoves, c.bestVsSecondGap.orElse(t.bestVsSecondGap)))
        val shiftArr = conceptShifts(t.conceptDelta, limit = 2)
        if shiftArr.value.nonEmpty then obj("conceptShift") = shiftArr
        t.phaseLabel.foreach(ph => obj("phaseLabel") = Str(ph))
      }
      if mergedTags.nonEmpty then obj("semanticTags") = arrStr(mergedTags)
      c.mistakeCategory.orElse(timeline.flatMap(_.mistakeCategory)).foreach(mc => obj("mistakeCategory") = Str(mc))
      obj("forced") = Bool(c.forced || c.legalMoves.exists(_ <= 1))
      obj
    }

    val payload = Obj(
      "instructions" -> instructions,
      "critical" -> Arr.from(nodes)
    )

    ujson.write(payload)



  private def applyTreeComments(root: AnalyzePgn.TreeNode, comments: Map[(Int, String), String]): AnalyzePgn.TreeNode =
    root.copy(
      comment = root.comment.orElse(comments.get((root.ply, root.san))),
      children = root.children.map(c => applyTreeComments(c, comments))
    )
  @scala.annotation.nowarn("msg=unused")
  private def conceptShifts(delta: AnalyzePgn.Concepts, limit: Int = 3): ujson.Arr =
    val pairs = List(
      "Drawishness" -> delta.drawish,
      "Dull/Dry Position" -> delta.dry,
      "White Comfortable" -> delta.comfortable,
      "Black Unpleasant" -> delta.unpleasant,
      "King Safety" -> delta.kingSafety,
      "Rook Activity" -> delta.rookActivity,
      "Pawn Storm" -> delta.pawnStorm,
      "Dynamic Sharpness" -> delta.dynamic,
      "Tactical Complexity" -> delta.tacticalDepth,
      "Blunder Risk" -> delta.blunderRisk,
      "Color Complex" -> delta.colorComplex,
      "Good Knight" -> delta.goodKnight,
      "Bad Bishop" -> delta.badBishop,
      "Fortress Potential" -> delta.fortress,
      "Endgame Complexity" -> delta.conversionDifficulty,
      "Long-term Compensation" -> delta.alphaZeroStyle
    )
    // Filter noise: only show shifts >= 0.15
    val top = pairs
      .filter { case (_, v) => math.abs(v) >= 0.15 }
      .sortBy { case (_, v) => -math.abs(v) }
      .take(limit)
    ujson.Arr.from(top.map { case (k, v) => Obj("name" -> Str(k), "delta" -> Num(round2(v))) })
