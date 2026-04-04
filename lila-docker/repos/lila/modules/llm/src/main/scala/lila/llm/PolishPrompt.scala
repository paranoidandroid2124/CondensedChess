package lila.llm

object PolishPrompt:

  private def optionalLine(label: String, value: Option[String]): Option[String] =
    value.map(_.trim).filter(_.nonEmpty).map(v => s"$label: $v")

  private def contextLines(
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String],
      nature: Option[String],
      tension: Option[Double],
      salience: Option[lila.llm.model.strategic.StrategicSalience],
      momentType: Option[String]
  ): String =
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(6)
    List(
      Some(s"Phase: $phase"),
      Some(s"Eval Delta: ${evalDelta.map(d => s"$d cp").getOrElse("N/A")}"),
      optionalLine("Opening", openingName),
      Option.when(conceptStr.nonEmpty)(s"Concepts: ${conceptStr.mkString(", ")}"),
      optionalLine("FEN", Option(fen)),
      optionalLine("Nature", nature),
      tension.map(t => f"Tension: $t%.2f"),
      salience.map(value => s"Salience: $value"),
      momentType.map(value => s"Moment Type: $value")
    ).flatten.mkString("\n")

  val systemPrompt: String =
    """You lightly polish deterministic chess commentary.
      |Preserve supplied facts, concrete chess tokens, and the original claim.
      |Return only commentary prose.""".stripMargin

  def buildPolishPrompt(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      nature: Option[String] = None,
      tension: Option[Double] = None,
      salience: Option[lila.llm.model.strategic.StrategicSalience] = None,
      momentType: Option[String] = None,
      bookmakerSlots: Option[Any] = None
  ): String =
    val _ = bookmakerSlots
    s"""## REQUEST
       |Refine the draft commentary below without adding new semantics.
       |
       |## CONTEXT
       |${contextLines(phase, evalDelta, concepts, fen, openingName, nature, tension, salience, momentType)}
       |
       |## DRAFT COMMENTARY
       |$prose""".stripMargin

  def buildRepairPrompt(
      originalProse: String,
      rejectedPolish: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      allowedSans: List[String] = Nil,
      bookmakerSlots: Option[Any] = None
  ): String =
    val _ = bookmakerSlots
    val allowed = allowedSans.map(_.trim).filter(_.nonEmpty).distinct.mkString(", ")
    s"""## REQUEST
       |Repair the rejected polish while staying anchored to the original draft.
       |
       |## CONTEXT
       |${contextLines(phase, evalDelta, concepts, fen, openingName, None, None, None, Some("Repair"))}
       |
       |## ALLOWED SAN EXTENSIONS
       |${if allowed.nonEmpty then allowed else "none"}
       |
       |## ORIGINAL_DRAFT
       |$originalProse
       |
       |## REJECTED_POLISH
       |$rejectedPolish""".stripMargin

  def buildSegmentRepairPrompt(
      originalSegment: String,
      rejectedPolish: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None
  ): String =
    s"""## REQUEST
       |Repair the rejected segment without changing chess tokens or lock anchors.
       |
       |## CONTEXT
       |${contextLines(phase, evalDelta, concepts, fen, openingName, None, None, None, Some("Segment Repair"))}
       |
       |## ORIGINAL_SEGMENT
       |$originalSegment
       |
       |## REJECTED_POLISH
       |$rejectedPolish""".stripMargin

  val estimatedSystemTokens: Int = 400

  def estimateRequestTokens(input: String): Int =
    (input.length / 4.0).toInt + 80
