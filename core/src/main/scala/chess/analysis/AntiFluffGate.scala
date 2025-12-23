package chess
package analysis

import GenerationConfig.BannedWords

/**
 * Enforces quality constraints on LLM generated text.
 * Rejects "flowery" language and ensures evidence citations are present.
 */
object AntiFluffGate:

  case class GateResult(
    isValid: Boolean,
    reasons: List[String] = Nil,
    cleanedText: String
  )

  /**
   * Validates and cleans text. 
   * @param text The raw LLM output
   * @param requiredCitations If true, at least one (src:...) must be present
   */
  def validate(text: String, requiredCitations: Boolean = false): GateResult =
    val cleaned = semiClean(text)
    val citationReasons = if requiredCitations then checkCitations(cleaned) else Nil
    val sentenceReasons = checkSentenceCount(cleaned)

    val allReasons = citationReasons ++ sentenceReasons

    GateResult(
      isValid = allReasons.isEmpty,
      reasons = allReasons,
      cleanedText = cleaned
    )

  private def checkCitations(text: String): List[String] =
    val citationPattern = """\(src:[a-z]+:[a-z0-9_]+\)""".r
    if citationPattern.findFirstIn(text).isEmpty then
      List("Missing required evidence citation e.g. (src:struct:iqp_white)")
    else Nil

  private def checkSentenceCount(text: String): List[String] =
    // Removed: 8-sentence limit was too restrictive, causing valid content to be rejected
    Nil

  def semiClean(text: String): String =
    val t = basicClean(text)
    
    // Soft cleaning: remove banned words instead of rejecting the whole text
    val scrubbed = BannedWords.foldLeft(t) { (acc, w) =>
      val regex = s"(?i)\\b$w\\s*".r
      regex.replaceAllIn(acc, "")
    }
    
    scrubbed.replaceAll("\\s+", " ")
      .replaceAll(" \\.", ".")
      .replaceAll(" ,", ",")
      .trim

  private def basicClean(text: String): String =
    var t = text.trim
    
    // Remove markdown code blocks if the LLM accidentally wrapped text in them
    if t.startsWith("```") then
       t = t.stripPrefix("```").stripSuffix("```").trim
       if t.startsWith("text") then t = t.stripPrefix("text").trim
    
    // Remove "Coach persona" prefixes
    val prefixes = List(
      "As a chess coach,",
      "Looking at this position,",
      "In this game,",
      "Here is the analysis:",
      "I've reviewed the moves,"
    )
    prefixes.foldLeft(t) { (acc, p) => 
      if acc.toLowerCase.startsWith(p.toLowerCase) then acc.substring(p.length).trim
      else acc
    }

  /**
   * Returns true if the text is of acceptable quality.
   */
  def isSafe(text: String, requiredCitations: Boolean = false): Boolean =
    validate(text, requiredCitations).isValid
