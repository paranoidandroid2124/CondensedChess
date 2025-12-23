package chess
package analysis

/**
 * Single source of truth for LLM Generation Parameters.
 * Prevents drift and "creative" hallucinations by locking down temperature.
 */
object GenerationConfig:
  // Conservative settings to reduce hallucinations and flowery language
  val Temperature: Double = 0.3
  val TopP: Double = 0.8
  val TopK: Int = 40
  
  // Output Constraints (Used by NarrativeTemplates/LlmAnnotator validation)
  val MaxSentencesPerComment: Int = 3
  val MaxSentencesPerCritical: Int = 4
  
  // Anti-fluff: Words that indicate the LLM is drifting into "Coach GPT" persona too deeply
  val BannedWords: Set[String] = Set(
  // 과장/감탄(일반)
  "amazing", "incredible", "unbelievable", "mindblowing", "astonishing",
  "jaw-dropping", "breathtaking", "spectacular", "magnificent", "stunning",
  "sensational", "phenomenal", "remarkable", "extraordinary", "epic",
  "insane", "crazy", "wild", "ridiculous", "absurd",

  // 미사여구/문학적 장식
  "beautiful", "gorgeous", "elegant", "graceful", "poetic", "lyrical",
  "artistic", "sublime", "majestic", "exquisite", "glorious",

  // 드라마/충격 프레이밍
  "shocking", "outrageous", "dramatic", "sensational", "scandalous",
  "unthinkable", "unimaginable",

  // 절대화/확신 과잉
  "always", "never", "certainly", "surely", "undoubtedly", "obviously",
  "clearly", "definitely", "absolutely", "completely", "totally", "literally",

  // 폭력적/재난 메타포(과한 비유)
  "crushing", "devastating", "deadly", "catastrophic", "disastrous",
  "annihilating", "obliterating", "massacre", "slaughter", "murderous",
  "brutal", "savage", "ruthless",

  // 신격화/영웅서사(체스 글에서 과장 유발)
  "genius", "immortal", "legendary", "iconic", "mythic",
  "masterpiece", "historic", "timeless", "godlike", "superhuman",

  // ‘완벽’류(분석문에서 특히 위험)
  "perfect", "flawless", "pure", "pristine",
  
  // LLM-isms (Flowery/Abstract)
  "battlefield", "orchestrate", "dance", "symphony", "dominates"
  )

  /** 
   * returns the "generationConfig" JSON object string for the Gemini API.
   * e.g. "generationConfig": { ... }
   */
  def jsonPart(mimeType: String = "application/json"): String =
    s""""generationConfig": {
       |  "responseMimeType": "$mimeType",
       |  "temperature": $Temperature,
       |  "topP": $TopP,
       |  "topK": $TopK
       |}""".stripMargin
