package lila.llm

object ActiveStrategicPrompt:

  val systemPrompt: String =
    """You lightly rewrite a deterministic active-note draft.
      |Preserve the draft's concrete strategic content and avoid adding new ideas.
      |Return JSON with one field only:
      |{ "commentary": "<strategic note>" }""".stripMargin

  private def optionalLine(label: String, value: Option[String]): Option[String] =
    value.map(_.trim).filter(_.nonEmpty).map(v => s"$label: $v")

  private def section(title: String, body: String): Option[String] =
    Option(body).map(_.trim).filter(_.nonEmpty).map(value => s"## $title\n$value")

  def buildPrompt(
      draftNote: String,
      phase: String,
      momentType: String,
      fen: String,
      concepts: List[String],
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): String =
    val _ = strategyPack
    val _ = dossier
    val _ = routeRefs
    val _ = moveRefs
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
    List(
      Some("## MOMENT CONTEXT"),
      Some(s"Phase: $phase"),
      Some(s"Moment Type: $momentType"),
      optionalLine("FEN", Option(fen)),
      Option.when(conceptStr.nonEmpty)(s"Concepts: ${conceptStr.mkString(", ")}"),
      section("DETERMINISTIC DRAFT", draftNote),
      Some("Rewrite the deterministic draft more cleanly without adding new semantics.")
    ).flatten.mkString("\n\n")

  def buildRepairPrompt(
      draftNote: String,
      rejectedPolish: String,
      failureReasons: List[String],
      phase: String,
      momentType: String,
      fen: String,
      concepts: List[String],
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): String =
    val _ = strategyPack
    val _ = dossier
    val _ = routeRefs
    val _ = moveRefs
    val reasons = failureReasons.map(_.trim).filter(_.nonEmpty).distinct
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
    List(
      Some("## MOMENT CONTEXT"),
      Some(s"Phase: $phase"),
      Some(s"Moment Type: $momentType"),
      optionalLine("FEN", Option(fen)),
      Option.when(conceptStr.nonEmpty)(s"Concepts: ${conceptStr.mkString(", ")}"),
      section("DETERMINISTIC DRAFT", draftNote),
      section("REJECTED POLISH", rejectedPolish),
      section("REPAIR REASONS", reasons.mkString(", ")),
      Some("Repair the rejected text by staying anchored to the deterministic draft only.")
    ).flatten.mkString("\n\n")
