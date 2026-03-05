package lila.llm

object ActiveStrategicPrompt:

  val systemPrompt: String =
    """You are a grandmaster-level strategic coach writing a separate strategic note
      |for one critical branch moment in a full PGN review.
      |
      |## GOAL
      |- Produce a short, human-readable strategic note that explains long-term plans
      |  and piece deployment choices for both sides when relevant.
      |
      |## HARD RULES
      |1. Return exactly one concise note in 2-4 sentences.
      |2. Mention at least one concrete strategic plan.
      |3. When available in context, prefer citing at least one concrete piece route
      |   (example style: Nd2-f1-e3) or one explicit long-term focus.
      |4. Do not invent facts, moves, evaluations, or lines that are not provided.
      |5. Do not contradict provided evaluation intent or side-to-move context.
      |6. No markdown headers, no bullet lists, no metadata.
      |7. If `Route References` are provided, cite at least one exact `routeId` in the prose.
      |8. If `Move References` are provided, cite at least one exact `move label` in the prose.
      |
      |## OUTPUT FORMAT
      |Return JSON with one field only:
      |{ "commentary": "<strategic note>" }""".stripMargin

  def buildPrompt(
      baseNarrative: String,
      phase: String,
      momentType: String,
      fen: String,
      concepts: List[String],
      strategyPack: Option[StrategyPack],
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): String =
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
    val conceptLine = if conceptStr.isEmpty then "none" else conceptStr.mkString(", ")
    val packBlock = strategyPackBlock(strategyPack)
    val routeRefSection = routeRefBlock(routeRefs)
    val moveRefSection = moveRefBlock(moveRefs)

    s"""## BASE MOMENT NARRATIVE
       |$baseNarrative
       |
       |## MOMENT CONTEXT
       |Phase: $phase
       |Moment Type: $momentType
       |FEN: $fen
       |Concepts: $conceptLine
       |
       |## STRATEGY PACK
       |$packBlock
       |
       |## REFERENCE CATALOG
       |Route References:
       |$routeRefSection
       |
       |Move References:
       |$moveRefSection
       |
       |When references are available, cite exact routeId and/or move label in your prose.
       |
       |Write one strategic branch note now.""".stripMargin

  def buildRepairPrompt(
      baseNarrative: String,
      rejectedNote: String,
      failureReasons: List[String],
      phase: String,
      momentType: String,
      fen: String,
      concepts: List[String],
      strategyPack: Option[StrategyPack],
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): String =
    val reasons = failureReasons.map(_.trim).filter(_.nonEmpty).distinct
    val reasonLine = if reasons.isEmpty then "format_or_content_violation" else reasons.mkString(", ")
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
    val conceptLine = if conceptStr.isEmpty then "none" else conceptStr.mkString(", ")
    val packBlock = strategyPackBlock(strategyPack)
    val routeRefSection = routeRefBlock(routeRefs)
    val moveRefSection = moveRefBlock(moveRefs)

    s"""## BASE MOMENT NARRATIVE
       |$baseNarrative
       |
       |## REJECTED NOTE
       |$rejectedNote
       |
       |## REPAIR REASONS
       |$reasonLine
       |
       |## MOMENT CONTEXT
       |Phase: $phase
       |Moment Type: $momentType
       |FEN: $fen
       |Concepts: $conceptLine
       |
       |## STRATEGY PACK
       |$packBlock
       |
       |## REFERENCE CATALOG
       |Route References:
       |$routeRefSection
       |
       |Move References:
       |$moveRefSection
       |
       |Repair output must cite exact routeId and/or move label when available.
       |
       |Rewrite into one valid strategic note (2-4 sentences), factual and concrete.""".stripMargin

  private def strategyPackBlock(strategyPack: Option[StrategyPack]): String =
    strategyPack match
      case None => "none"
      case Some(pack) =>
        val side = Option(pack.sideToMove).map(_.trim).filter(_.nonEmpty).getOrElse("unknown")
        val plans =
          pack.plans
            .map { p =>
              val priorities =
                p.priorities.map(_.trim).filter(_.nonEmpty).take(3) match
                  case Nil  => ""
                  case list => s" | priorities: ${list.mkString("; ")}"
              val risks =
                p.riskTriggers.map(_.trim).filter(_.nonEmpty).take(3) match
                  case Nil  => ""
                  case list => s" | risk triggers: ${list.mkString("; ")}"
              s"- ${p.side} ${p.horizon} plan: ${p.planName}$priorities$risks"
            }
        val routes =
          pack.pieceRoutes.map { r =>
            val route = r.route.map(_.trim).filter(_.nonEmpty).mkString("-")
            val evidence =
              r.evidence.map(_.trim).filter(_.nonEmpty).take(3) match
                case Nil  => ""
                case list => s" | evidence: ${list.mkString(", ")}"
            s"- ${r.piece} route: $route | purpose: ${r.purpose} | confidence: ${"%.2f".format(r.confidence)}$evidence"
          }
        val focus =
          pack.longTermFocus.map(_.trim).filter(_.nonEmpty).map(f => s"- $f")
        val evidence =
          pack.evidence.map(_.trim).filter(_.nonEmpty).take(4).map(e => s"- $e")

        val planBlock =
          if plans.isEmpty then "Plans:\n- none"
          else s"Plans:\n${plans.mkString("\n")}"
        val routeBlock =
          if routes.isEmpty then "Piece Routes:\n- none"
          else s"Piece Routes:\n${routes.mkString("\n")}"
        val focusBlock =
          if focus.isEmpty then "Long-Term Focus:\n- none"
          else s"Long-Term Focus:\n${focus.mkString("\n")}"
        val evidenceBlock =
          if evidence.isEmpty then "Evidence:\n- none"
          else s"Evidence:\n${evidence.mkString("\n")}"

        s"""Side to move: $side
           |$planBlock
           |$routeBlock
           |$focusBlock
           |$evidenceBlock""".stripMargin

  private def routeRefBlock(routeRefs: List[ActiveStrategicRouteRef]): String =
    val lines =
      routeRefs
        .map { r =>
          val routePath = r.route.map(_.trim).filter(_.nonEmpty).mkString("-")
          s"- ${r.routeId}: ${r.piece}${routePath} | purpose: ${r.purpose} | confidence: ${"%.2f".format(r.confidence)}"
        }
        .take(4)
    if lines.isEmpty then "- none"
    else lines.mkString("\n")

  private def moveRefBlock(moveRefs: List[ActiveStrategicMoveRef]): String =
    val lines =
      moveRefs
        .map { m =>
          val san = m.san.map(_.trim).filter(_.nonEmpty).getOrElse("n/a")
          s"- ${m.label}: ${m.uci} | san: $san | source: ${m.source}"
        }
        .take(4)
    if lines.isEmpty then "- none"
    else lines.mkString("\n")
