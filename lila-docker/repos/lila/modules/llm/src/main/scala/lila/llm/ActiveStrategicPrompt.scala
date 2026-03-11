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
      |2. Derive an independent strategic thesis from the structured evidence rather than mirroring prior prose.
      |   Mention at least one concrete strategic plan or long-term reroute that is grounded in the provided evidence.
      |3. Do not recycle long phrases, sentence openings, or generic framing from any prior note.
      |4. When available in context, prefer citing at least one concrete piece route
      |   (example style: Nd2-f1-e3) or one explicit long-term focus.
      |   If both a structure signal and a route are present, connect them explicitly:
      |   explain that the structure calls for that deployment.
      |5. Do not invent facts, moves, evaluations, or lines that are not provided.
      |6. Do not contradict provided evaluation intent or side-to-move context.
      |7. No markdown headers, no bullet lists, no metadata.
      |8. If `Route References` are provided, cite at least one exact `routeId` in the prose.
      |9. If `Move References` are provided, cite at least one exact `move label` in the prose.
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
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): String =
    val _ = baseNarrative
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
    val conceptLine = if conceptStr.isEmpty then "none" else conceptStr.mkString(", ")
    val dossierBlock = activeDossierBlock(dossier)
    val packBlock = strategyPackBlock(strategyPack)
    val routeRefSection = routeRefBlock(routeRefs)
    val moveRefSection = moveRefBlock(moveRefs)

    s"""## MOMENT CONTEXT
       |Phase: $phase
       |Moment Type: $momentType
       |FEN: $fen
       |Concepts: $conceptLine
       |
       |## ACTIVE DOSSIER
       |$dossierBlock
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
       |Write one independent strategic branch note now.""".stripMargin

  def buildRepairPrompt(
      baseNarrative: String,
      rejectedNote: String,
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
    val reasons = failureReasons.map(_.trim).filter(_.nonEmpty).distinct
    val reasonLine = if reasons.isEmpty then "format_or_content_violation" else reasons.mkString(", ")
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
    val conceptLine = if conceptStr.isEmpty then "none" else conceptStr.mkString(", ")
    val dossierBlock = activeDossierBlock(dossier)
    val packBlock = strategyPackBlock(strategyPack)
    val routeRefSection = routeRefBlock(routeRefs)
    val moveRefSection = moveRefBlock(moveRefs)

    s"""## PRIOR NOTE TO AVOID PARAPHRASING
       |$baseNarrative
       |
       |Use the prior note only as a contradiction guard. Do not mirror its wording or sentence structure.
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
       |## ACTIVE DOSSIER
       |$dossierBlock
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
       |Rewrite into one valid independent strategic note (2-4 sentences), factual and concrete.""".stripMargin

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
            s"- ${r.side} ${r.piece} route: $route | purpose: ${r.purpose} | confidence: ${"%.2f".format(r.confidence)}$evidence"
          }
        val focus =
          pack.longTermFocus.map(_.trim).filter(_.nonEmpty).map(f => s"- $f")
        val evidence =
          pack.evidence.map(_.trim).filter(_.nonEmpty).take(4).map(e => s"- $e")
        val signalDigest =
          pack.signalDigest
            .map { digest =>
              List(
                digest.opening.map(v => s"- opening: $v"),
                Option.when(digest.strategicStack.nonEmpty)(s"- strategic stack: ${digest.strategicStack.mkString("; ")}"),
                digest.latentPlan.map(v => s"- latent plan: $v"),
                digest.latentReason.map(v => s"- latent reason: $v"),
                digest.practicalVerdict.map(v => s"- practical verdict: $v"),
                Option.when(digest.practicalFactors.nonEmpty)(s"- practical factors: ${digest.practicalFactors.mkString("; ")}"),
                digest.compensation.map(v => s"- compensation: $v"),
                Option.when(digest.compensationVectors.nonEmpty)(s"- compensation vectors: ${digest.compensationVectors.mkString("; ")}"),
                digest.investedMaterial.map(v => s"- invested material: ${v}cp"),
                digest.structuralCue.map(v => s"- structure: $v"),
                digest.structureProfile.map(v => s"- structure profile: $v"),
                digest.centerState.map(v => s"- center state: $v"),
                digest.alignmentBand.map(v => s"- alignment band: $v"),
                Option.when(digest.alignmentReasons.nonEmpty)(s"- alignment reasons: ${digest.alignmentReasons.mkString("; ")}"),
                digest.deploymentPiece.map { piece =>
                  val route =
                    if digest.deploymentRoute.nonEmpty then digest.deploymentRoute.mkString("-")
                    else "n/a"
                  s"- structure deployment: $piece $route"
                },
                digest.deploymentPurpose.map(v => s"- deployment purpose: $v"),
                digest.deploymentContribution.map(v => s"- move contribution: $v"),
                digest.deploymentConfidence.map(v => s"- deployment confidence: ${"%.2f".format(v)}"),
                digest.decisionComparison.flatMap(_.chosenMove).map(v => s"- chosen move: $v"),
                digest.decisionComparison.flatMap(_.engineBestMove).map(v => s"- engine best: $v"),
                digest.decisionComparison.flatMap(_.cpLossVsChosen).map(v => s"- cp loss vs chosen: ${v}cp"),
                digest.decisionComparison.flatMap(_.deferredMove).map(v => s"- deferred move: $v"),
                digest.decisionComparison.flatMap(_.deferredReason).map(v => s"- deferred reason: $v"),
                digest.decisionComparison.flatMap(_.evidence).map(v => s"- deferred evidence: $v"),
                Option.when(digest.decisionComparison.exists(_.practicalAlternative))("- practical alternative: true"),
                digest.prophylaxisPlan.map(v => s"- prophylaxis plan: $v"),
                digest.prophylaxisThreat.map(v => s"- prophylaxis target: $v"),
                digest.counterplayScoreDrop.map(v => s"- counterplay drop: ${v}cp"),
                digest.decision.map(v => s"- decision: $v"),
                digest.strategicFlow.map(v => s"- strategic flow: $v"),
                digest.opponentPlan.map(v => s"- opponent plan: $v"),
                Option.when(digest.preservedSignals.nonEmpty)(s"- preserved signals: ${digest.preservedSignals.mkString(", ")}")
              ).flatten
            }
            .getOrElse(Nil)

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
        val digestBlock =
          if signalDigest.isEmpty then "Signal Digest:\n- none"
          else s"Signal Digest:\n${signalDigest.mkString("\n")}"

        s"""Side to move: $side
           |$planBlock
           |$routeBlock
           |$focusBlock
           |$evidenceBlock
           |$digestBlock""".stripMargin

  private def activeDossierBlock(dossier: Option[ActiveBranchDossier]): String =
    dossier match
      case None => "none"
      case Some(value) =>
        List(
          Some(s"- dominant lens: ${value.dominantLens}"),
          Some(s"- chosen branch: ${value.chosenBranchLabel}"),
          value.engineBranchLabel.map(v => s"- engine branch: $v"),
          value.deferredBranchLabel.map(v => s"- deferred branch: $v"),
          value.whyChosen.map(v => s"- why chosen: $v"),
          value.whyDeferred.map(v => s"- why deferred: $v"),
          value.opponentResource.map(v => s"- opponent resource: $v"),
          value.routeCue.map { cue =>
            s"- route cue: ${cue.routeId} ${cue.piece}${cue.route.mkString("-")} | purpose: ${cue.purpose} | confidence: ${"%.2f".format(cue.confidence)}"
          },
          value.moveCue.map { cue =>
            val san = cue.san.map(_.trim).filter(_.nonEmpty).getOrElse("n/a")
            s"- move cue: ${cue.label} ${cue.uci} | san: $san | source: ${cue.source}"
          },
          value.evidenceCue.map(v => s"- evidence cue: $v"),
          value.continuationFocus.map(v => s"- continuation focus: $v"),
          value.practicalRisk.map(v => s"- practical risk: $v"),
          value.comparisonGapCp.map(v => s"- comparison gap: ${v}cp")
        ).flatten.mkString("\n")

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
