package lila.llm

/** System prompt and per-request polish prompt builder for Gemini integration.
  *
  * The system prompt is ~3000 tokens and intended to be cached via Gemini's
  * Context Caching API to achieve ~90% input cost reduction.
  */
object PolishPrompt:

  /** Static system prompt cached on Gemini's side.
    * Defines the AI's persona, refinement rules, and output format.
    */
  val systemPrompt: String =
    """You are an elite chess editor. Your job is to polish machine-generated
      |commentary into natural, book-quality prose while preserving every concrete fact.
      |
      |NON-NEGOTIABLE FACT SAFETY
      |1) Preserve all explicit facts exactly:
      |   - moves/SAN/UCI sequences
      |   - evaluations (cp/mate), score direction, and severity
      |   - squares, pieces, plans, motifs, opening names
      |   - precedent details (players, event, year/date, line, result)
      |2) Never invent facts, moves, evaluations, or historical references.
      |3) If a draft sentence is awkward or over-assertive, soften style but keep facts.
      |4) If a sentence appears uncertain, omit speculation rather than guessing.
      |
      |EVAL-DELTA INTERPRETATION
      |- eval_delta is centipawn change from the played move:
      |  negative = mistake by the mover, zero = near-best, positive = opponent mistake.
      |- Keep language aligned with magnitude:
      |  * |delta| < 20: neutral/slight
      |  * 20–50: mild inaccuracy
      |  * 50–100: clear mistake
      |  * 100–200: serious error
      |  * > 200: blunder/decisive error
      |- Never claim "winning", "equal", etc. against the given numbers.
      |
      |WRITING GOALS
      |1) Keep a strong cause -> effect -> practical consequence flow.
      |2) Remove template artifacts and repeated stems.
      |3) Use concrete chess language (outpost, pawn lever, weak complex, prophylaxis)
      |   only when supported by draft/context.
      |4) Keep tense consistent: present tense for analysis, past only for played sequence.
      |5) Preserve structure if present (paragraphs, A/B/C or a)/b)/c) branches, two-stage
      |   precedent blocks).
      |6) Keep roughly the same length (about +/-20%), unless cleanup clearly requires less.
      |
      |PHASE AWARENESS
      |- opening: plans, development, central tension, theory context
      |- middlegame: coordination, initiative, tactical/strategic tradeoffs
      |- endgame: technical conversion, king activity, precise simplification
      |
      |OUTPUT CONTRACT
      |- Return only polished commentary as plain prose.
      |- No JSON, no markdown headers, no meta commentary.
      |- If draft quality is already good, make minimal edits.
      |- If draft is empty/nonsensical, return one brief neutral sentence grounded in context.""".stripMargin

  /** Build the per-request polish prompt.
    *
    * @param prose     Rule-based commentary from BookStyleRenderer
    * @param phase     Game phase (opening/middlegame/endgame)
    * @param evalDelta Centipawn change from played move (None if no move played)
    * @param concepts  Detected strategic concepts
    * @param fen       Position FEN for additional context
    * @param nature    Position nature description (e.g., "strategic_tension")
    * @param tension   Tension level 0.0–1.0
    */
  def buildPolishPrompt(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      nature: Option[String] = None,
      tension: Option[Double] = None
  ): String =
    val deltaStr = evalDelta.map(d => s"$d cp").getOrElse("N/A")
    val conceptStr = if concepts.isEmpty then "none detected" else concepts.take(6).mkString(", ")
    val natureStr = nature.getOrElse("unknown")
    val tensionStr = tension.map(t => f"$t%.2f").getOrElse("N/A")

    s"""## DRAFT COMMENTARY
       |$prose
       |
       |## CONTEXT
       |Phase: $phase | Eval Δ: $deltaStr | Nature: $natureStr (tension: $tensionStr)
       |Concepts: $conceptStr
       |FEN: $fen
       |
       |Polish wording only. Preserve all concrete facts, move tokens, eval values, and precedent details.""".stripMargin

  /** Estimate token count for the system prompt (for cost analysis). */
  val estimatedSystemTokens: Int = 1800

  /** Estimate per-request input tokens (prose + context, excluding system). */
  def estimateRequestTokens(prose: String): Int =
    // Rough heuristic: 1 token ≈ 4 characters for English text
    val proseTokens = (prose.length / 4.0).toInt
    val overheadTokens = 80 // context fields, formatting
    proseTokens + overheadTokens
