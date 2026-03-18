package lila.llm

object StrategicPuzzlePrompt:

  final case class TerminalInput(
      startFen: String,
      sideToMove: String,
      outcome: String,
      dominantFamily: Option[String],
      lineSan: List[String],
      siblingMoves: List[String],
      opening: Option[String],
      eco: Option[String],
      draftCommentary: String
  )

  final case class SummaryInput(
      startFen: String,
      sideToMove: String,
      dominantFamily: Option[String],
      mainLine: List[String],
      acceptedStarts: List[String],
      opening: Option[String],
      eco: Option[String],
      draftSummary: String,
      draftCommentary: Option[String]
  )

  private def optionalLine(label: String, value: Option[String]): Option[String] =
    value.map(_.trim).filter(_.nonEmpty).map(v => s"$label: $v")

  private def listLine(label: String, values: List[String], limit: Int): Option[String] =
    val items = values.map(_.trim).filter(_.nonEmpty).distinct.take(limit)
    Option.when(items.nonEmpty)(s"$label: ${items.mkString(", ")}")

  private def humanize(value: String): String =
    value.replace('|', ' ').replace('_', ' ').trim

  private def capitalizeSide(value: String): String =
    Option(value).map(_.trim.toLowerCase).filter(_.nonEmpty).map(_.capitalize).getOrElse("Unknown")

  private def lineText(line: List[String]): String =
    line.map(_.trim).filter(_.nonEmpty).mkString(" ")

  val terminalSystemPrompt: String =
    """You refine precomputed strategic chess puzzle reveal text into short natural English prose.
      |
      |Rules:
      |- Return JSON with one field only: { "commentary": "<text>" }.
      |- Write plain prose only inside the commentary field.
      |- Keep the reveal narrow: explain the line as one strategic route, not as a move-by-move broadcast.
      |- Stay grounded in the supplied draft and context. Do not invent moves, evaluations, or opening lore.
      |- Never mention engines, best-move language, or computer evaluations.
      |- Use at most 2 short paragraphs.
      |- Paragraph 1 should state the strategic thesis of the reached line.
      |- Paragraph 2 may explain the opponent resource, execution mechanism, or why this route differs from nearby alternatives.
      |- Do not generate headings, bullets, markdown, or metadata wrappers.""".stripMargin

  val summarySystemPrompt: String =
    """You write a short reveal summary for a strategic chess puzzle.
      |
      |Rules:
      |- Return JSON with one field only: { "commentary": "<text>" }.
      |- Write plain English prose only.
      |- Summarize the shared plan of the accepted lines rather than narrating the moves one by one.
      |- Stay grounded in the supplied draft and context. Do not invent moves, evaluations, or opening lore.
      |- Never mention engines, best-move language, or computer evaluations.
      |- Use 1-2 short paragraphs.
      |- The summary should identify the central strategic idea, the clearest featured route, and the main takeaway for the player.
      |- Open from the plan, tension, or practical race itself. Avoid stock leads such as "The central strategic idea" or "The main takeaway".
      |- Do not generate headings, bullets, markdown, or metadata wrappers.""".stripMargin

  def buildTerminalPrompt(input: TerminalInput): String =
    val context =
      List(
        Some("## PUZZLE CONTEXT"),
        Some(s"Side to move: ${capitalizeSide(input.sideToMove)}"),
        Some(s"Outcome: ${input.outcome.trim.toLowerCase}"),
        optionalLine("Dominant family", input.dominantFamily.map(humanize)),
        optionalLine("Opening", input.opening),
        optionalLine("ECO", input.eco),
        optionalLine("FEN", Option(input.startFen).map(_.trim).filter(_.nonEmpty)),
        optionalLine("Reached line", Option(lineText(input.lineSan)).filter(_.nonEmpty)),
        listLine("Sibling continuations", input.siblingMoves, limit = 4)
      ).flatten.mkString("\n")

    s"""## REQUEST
       |Refine the draft into reveal text for the strategic line the player actually reached.
       |
       |## CONTEXT
       |$context
       |
       |Keep the thesis first, then the mechanism or opponent resource.
       |If the outcome is partial, make that difference clear without sounding punitive.
       |
       |## DRAFT
       |${input.draftCommentary.trim}""".stripMargin

  def buildSummaryPrompt(input: SummaryInput): String =
    val context =
      List(
        Some("## PUZZLE CONTEXT"),
        Some(s"Side to move: ${capitalizeSide(input.sideToMove)}"),
        optionalLine("Dominant family", input.dominantFamily.map(humanize)),
        optionalLine("Opening", input.opening),
        optionalLine("ECO", input.eco),
        optionalLine("Featured line", Option(lineText(input.mainLine)).filter(_.nonEmpty)),
        listLine("Accepted starts", input.acceptedStarts, limit = 5),
        input.draftCommentary.map(_.trim).filter(_.nonEmpty).map(v => s"Supporting reveal draft: $v")
      ).flatten.mkString("\n")

    s"""## REQUEST
       |Write a short reveal summary for the whole puzzle.
       |
       |## CONTEXT
       |$context
       |
       |State the common plan first, then explain how the featured line expresses that plan better than a generic waiting move.
       |Open from the plan or pressure itself instead of stock summary phrasing.
       |End with one practical lesson only if the draft supports it.
       |
       |## DRAFT
       |${input.draftSummary.trim}""".stripMargin

  val estimatedTerminalSystemTokens: Int = 220
  val estimatedSummarySystemTokens: Int = 180

  def estimateRequestTokens(input: String): Int =
    (input.length / 4.0).toInt + 48
