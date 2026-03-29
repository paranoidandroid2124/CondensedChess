package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.authoring._

import scala.util.matching.Regex

/**
 * NarrativeOutlineValidator: SSOT for hard gate enforcement.
 *
 * Enforces quality constraints on NarrativeOutline before rendering.
 * Rules from HighEffortBookCommentary.md Section 5.2.
 */
object NarrativeOutlineValidator:

  /**
   * Validate and clean the outline, applying all hard gates.
   */
  def validate(
    outline: NarrativeOutline,
    diag: OutlineDiagnostics,
    rec: TraceRecorder,
    ctx: Option[NarrativeContext] = None
  ): NarrativeOutline =
    var currentDiag = diag
    var beats = outline.beats

    // 1. Trim text and drop empty beats
    beats = beats.map(b => b.copy(text = b.text.trim))
    val (nonEmpty, empty) = beats.partition(_.text.nonEmpty)
    if empty.nonEmpty then
      rec.drop("outline.empty", empty.size, "Dropped empty outline beats")
      empty.foreach(b => currentDiag = currentDiag.addDropped(b.kind, "EMPTY_TEXT"))
    beats = nonEmpty

    // 2. Drop exact duplicates
    beats = dropDuplicateBeats(beats, rec)

    // 3. Validate evidence requirements per question / evidence-purpose mapping.
    beats = validateEvidenceRequirements(beats, rec)

    // 4. Validate minimum branches for evidence beats.
    beats = validateMinBranches(beats, rec)

    // 5. Validate tactical-stop theme mention.
    beats = validateTacticalTestTheme(beats, rec)

    // 6. Validate must-mention anchors.
    beats = validateMustMention(beats, rec)

    // 7. Reconcile evidence metadata.
    beats = reconcileEvidenceMetadata(beats)

    NarrativeOutline(beats, Some(currentDiag))

  private def dropDuplicateBeats(beats: List[OutlineBeat], rec: TraceRecorder): List[OutlineBeat] =
    val seen = scala.collection.mutable.Set.empty[(OutlineBeatKind, List[String])]
    val out = scala.collection.mutable.ListBuffer.empty[OutlineBeat]
    var dropped = 0

    beats.foreach { b =>
      val key = (b.kind, b.conceptIds.sorted)
      if seen.contains(key) then
        dropped += 1
      else
        seen += key
        out += b
    }

    if dropped > 0 then rec.drop("outline.dupes", dropped, "Dropped duplicate beats (DUPLICATE_BEAT)")
    out.toList

  private def validateEvidenceRequirements(beats: List[OutlineBeat], rec: TraceRecorder): List[OutlineBeat] =
    beats.map { b =>
      if !b.requiresEvidence then b
      else
        val satisfied = EvidencePlanner.isSatisfied(b.expectedEvidencePurposes.toSet, b.evidencePurposes.toSet)
        if satisfied then b
        else
          rec.drop(s"outline.evidence_req", b.kind.toString, "Downgraded due to missing evidence (QK_EVIDENCE_MAP)")
          b.copy(confidenceLevel = b.confidenceLevel * 0.5)
    }

  private def validateMinBranches(beats: List[OutlineBeat], rec: TraceRecorder): List[OutlineBeat] =
    beats.filter { b =>
      if b.kind != OutlineBeatKind.Evidence then true
      else
        val branchCount = countBranches(b.text)
        val minRequired =
          if b.expectedEvidencePurposes.nonEmpty then EvidencePlanner.minBranches(b.expectedEvidencePurposes.toSet)
          else 2
        if branchCount >= minRequired then true
        else
          rec.drop("outline.min_branches", s"$branchCount < $minRequired", "Dropped Evidence beat (MIN_BRANCHES)")
          false
    }

  private val BranchPattern: Regex = "(?m)^[a-z]\\)\\s+".r

  private def countBranches(text: String): Int =
    BranchPattern.findAllMatchIn(text).size

  private def validateTacticalTestTheme(beats: List[OutlineBeat], rec: TraceRecorder): List[OutlineBeat] =
    beats.map { b =>
      if !b.questionKinds.contains(AuthorQuestionKind.WhatMustBeStopped) then b
      else
        val anchors = b.allAnchors.filter(isUserFacingAnchor)
        val textLower = b.text.toLowerCase
        val mentioned = anchors.exists(a => textLower.contains(a.toLowerCase))

        if mentioned then b
        else
          anchors.headOption match
            case Some(anchor) =>
              rec.drop("outline.tactical_theme", anchor, "Forced theme mention (TACTICAL_TEST_THEME)")
              // Avoid leaking validation/debug text into user-facing prose.
              b.copy(confidenceLevel = b.confidenceLevel * 0.7)
            case None => b
    }

  private def validateMustMention(beats: List[OutlineBeat], rec: TraceRecorder): List[OutlineBeat] =
    beats.map { b =>
      // Only enforce user-facing anchors (moves/squares), and never append debug notes.
      if b.text.trim.isEmpty then b
      else
        val anchors = b.allAnchors.filter(isUserFacingAnchor)
        if anchors.isEmpty then b
        else
          val textLower = b.text.toLowerCase
          val missing = anchors.filterNot(a => textLower.contains(a.toLowerCase))

          if missing.isEmpty then b
          else
            rec.drop("outline.must_mention", missing.mkString(","), "Missing anchors (MUST_MENTION)")
            b.copy(confidenceLevel = b.confidenceLevel * 0.7)
    }

  private def isUserFacingAnchor(anchor: String): Boolean =
    val a = anchor.trim
    if a.isEmpty then false
    else {
      val isUci = a.matches("(?i)^[a-h][1-8][a-h][1-8][qrbn]?$")
      val isSquare = a.matches("(?i)^[a-h][1-8]$")
      val isSan =
        a == "O-O" || a == "O-O-O" ||
          a.matches("(?i)^[kqrbn]?[a-h]?[1-8]?x?[a-h][1-8](=[qrbn])?[+#]?$") ||
          a.matches("(?i)^\\.{3}[kqrbn]?[a-h]?[1-8]?x?[a-h][1-8](=[qrbn])?[+#]?$")
      isUci || isSquare || isSan
    }

  private def reconcileEvidenceMetadata(beats: List[OutlineBeat]): List[OutlineBeat] =
    val evidencePurposesInOutline =
      beats.filter(_.kind == OutlineBeatKind.Evidence).flatMap(_.evidencePurposes).toSet

    if evidencePurposesInOutline.nonEmpty then beats
    else
      beats.map { b =>
        if b.kind == OutlineBeatKind.DecisionPoint && (b.evidencePurposes.nonEmpty || b.evidenceSourceIds.nonEmpty) then
          b.copy(evidencePurposes = Nil, evidenceSourceIds = Nil)
        else b
      }

  def validate(outline: NarrativeOutline, rec: TraceRecorder): NarrativeOutline =
    validate(outline, outline.diagnostics.getOrElse(OutlineDiagnostics()), rec, None)
