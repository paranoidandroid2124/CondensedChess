package lila.llm.analysis

import lila.llm.model.NarrativeContext

import scala.util.matching.Regex

/**
 * Post‑Critic
 *
 * A lightweight, deterministic editor that removes clichés, repetition, and
 * overconfident filler after the prose has been rendered.
 *
 * Important: This is intentionally conservative (string-level edits) to avoid
 * breaking formatting such as alternatives blocks.
 */
object PostCritic:

  def revise(ctx: NarrativeContext, prose: String): String =
    val raw = prose.trim
    if raw.isEmpty then prose
    else
      val cpWhite = evalCpWhite(ctx)
      val isClose = cpWhite.abs <= 40

      val replaced = rewriteCliches(ctx, raw)
      val toned = toneDownOverconfidence(replaced, isClose)
      val earlyOpening =
        if (ctx.phase.current.equalsIgnoreCase("Opening") && ctx.ply <= 2)
          toned.replace("The balance depends on precise calculation.", "Both sides have many sound options from here.")
        else toned
      val deduped0 = limitSentence(earlyOpening, "The position remains roughly balanced.", max = 1)
      val deduped1 = limitSentence(deduped0, "The position is roughly balanced.", max = 1)
      val deduped2 = limitSentence(deduped1, "White can play for a small edge.", max = 2)
      val deduped3 = limitSentence(deduped2, "Black can play for a small edge.", max = 2)
      val familyDeduped = collapseFamilyDuplicates(deduped3)
      val boilerplateRewritten = rewriteBoilerplate(familyDeduped)
      val terminalToneSanitized = sanitizeTerminalTone(boilerplateRewritten)

      cleanup(terminalToneSanitized)

  private def evalCpWhite(ctx: NarrativeContext): Int =
    ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).orElse {
      ctx.semantic.flatMap(_.practicalAssessment).map { a =>
        // PracticalAssessment is currently in side-to-move perspective; normalize to White.
        val whiteToMove = (ctx.ply % 2 == 1)
        if (whiteToMove) a.engineScore else -a.engineScore
      }
    }.getOrElse(0)

  private def rewriteCliches(ctx: NarrativeContext, text: String): String =
    val phase = ctx.phase.current.toLowerCase
    val replacement =
      phase match
        case "opening" =>
          if (ctx.ply <= 2) "We are at the start of the opening."
          else if (ctx.authorQuestions.nonEmpty || ctx.counterfactual.isDefined) "The opening has reached a critical moment."
          else "Opening play continues."
        case "middlegame" =>
          if (ctx.authorQuestions.nonEmpty || ctx.counterfactual.isDefined) "The middlegame has reached a critical moment."
          else "The middlegame is taking shape."
        case "endgame" =>
          if (ctx.authorQuestions.nonEmpty || ctx.counterfactual.isDefined) "The endgame has reached a critical moment."
          else "Endgame technique will be key."
        case _ => "The position calls for accuracy."

    text
      // Remove the most obvious filler opener regardless of ply.
      .replace("The game begins.", replacement)
      // Remove brittle "task" filler when it provides no concrete content.
      .replace("The main task is to pursue the pawn chain maintenance.", "")
      .replace("The main task is to pursue the pawn chain maintenance", "")
      .replace("The main task is to focus on pawn chain maintenance.", "")
      .replace("The main task is to focus on pawn chain maintenance", "")
      .replace("The main task is to pursue the development of the pieces.", "")
      .replace("The main task is to focus on development of the pieces.", "")
      .replace("The balance depends on precise calculation.", "")

  private def toneDownOverconfidence(text: String, isClose: Boolean): String = text

  private def limitSentence(text: String, sentence: String, max: Int): String =
    if (max < 0) return text
    // Never consume newlines when removing a sentence: keep paragraph structure intact.
    val pat = (s"(?i)${Regex.quote(sentence)}[ \\t]*").r
    val matches = pat.findAllMatchIn(text).toList
    if (matches.size <= max) text
    else
      val sb = new StringBuilder(text.length)
      var last = 0
      var kept = 0
      matches.foreach { m =>
        sb.append(text.substring(last, m.start))
        if (kept < max) {
          sb.append(text.substring(m.start, m.end))
          kept += 1
        }
        last = m.end
      }
      sb.append(text.substring(last))
      sb.toString

  private def cleanup(text: String): String =
    text
      .replaceAll("(?m)[ \\t]+$", "")           // trim line endings
      .replaceAll("(?m)[ \\t]{2,}", " ")       // collapse extra spaces (not newlines)
      .replaceAll(" \\.", ".")                 // " ."
      .replaceAll(" ,", ",")                   // " ,"
      .replaceAll("\\n{3,}", "\n\n")           // collapse extra blank lines
      .trim

  private def rewriteBoilerplate(text: String): String = text

  private def sanitizeTerminalTone(text: String): String = text

  private def collapseFamilyDuplicates(text: String): String =
    val families = List(
      "balanced" -> List("roughly equal", "about level", "essentially balanced", "dynamically balanced"),
      "playable" -> List("another good option", "also playable here", "deserves attention"),
      "comfortable" -> List("comfortable to play", "easier side to press with", "more comfortable here"),
      "planlead" -> List(
        "key theme:",
        "strategic focus:",
        "strategic priority:",
        "practical roadmap centers on",
        "play revolves around",
        "current play is organized around"
      )
    )

    val lines = text.split("\n").toList
    val seen = scala.collection.mutable.HashSet.empty[String]

    val kept = lines.filter { line =>
      val lower = line.toLowerCase
      val matchedFamily = families.collectFirst {
        case (fam, needles) if needles.exists(lower.contains) => fam
      }
      matchedFamily match
        case None => true
        case Some(fam) =>
          if seen.contains(fam) then false
          else
            seen += fam
            true
    }

    kept.mkString("\n")
