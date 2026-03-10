package lila.llm.analysis

import _root_.chess.Color
import _root_.chess.format.Fen
import lila.llm.{ StrategyPack, StrategyPieceRoute, StrategySidePlan }
import lila.llm.model.{ ExtendedAnalysisData, NarrativeContext }
import lila.llm.model.authoring.PlanHypothesis
import lila.llm.model.strategic.PieceActivity
import lila.llm.model.strategic.PositionalTag

object StrategyPackBuilder:

  private val MaxPlans = 4
  private val MaxRoutes = 4
  private val MaxFocus = 6
  private val MaxEvidence = 8

  def build(
      data: ExtendedAnalysisData,
      ctx: NarrativeContext
  ): Option[StrategyPack] =
    val sideToMoveColor = if data.isWhiteToMove then Color.White else Color.Black
    val sideToMove = sideName(sideToMoveColor)
    val structureArc = StructurePlanArcBuilder.build(ctx)
    val plans = buildPlans(ctx, sideToMoveColor)
    val routes = buildRoutes(data, sideToMoveColor, plans, structureArc)
    val longTermFocus = buildLongTermFocus(ctx, plans, routes, structureArc)
    val evidence = buildEvidence(ctx, routes, structureArc)
    val signalDigest = NarrativeSignalDigestBuilder.build(ctx)

    Option.when(plans.nonEmpty || routes.nonEmpty || longTermFocus.nonEmpty)(
      StrategyPack(
        sideToMove = sideToMove,
        plans = plans,
        pieceRoutes = routes,
        longTermFocus = longTermFocus,
        evidence = evidence,
        signalDigest = signalDigest
      )
    )

  def promptHints(pack: StrategyPack): List[String] =
    val planHints = pack.plans.take(2).map { p =>
      s"${p.side} long plan: ${p.planName}"
    }
    val routeHints = pack.pieceRoutes.take(2).map { r =>
      s"${r.side} ${r.piece} route ${r.route.mkString("-")} (${r.purpose})"
    }
    val focusHints = pack.longTermFocus.take(2).map(v => s"long-term focus: $v")
    val digestHints =
      pack.signalDigest.toList.flatMap { digest =>
        List(
          digest.opening.map(v => s"opening: $v"),
          digest.latentPlan.map(v => s"latent plan: $v"),
          digest.authoringEvidence.map(v => s"authoring evidence: $v"),
          digest.practicalVerdict.map(v => s"practical: $v"),
          Option.when(digest.practicalFactors.nonEmpty)(s"practical factors: ${digest.practicalFactors.mkString("; ")}"),
          digest.structureProfile.map(v => s"structural profile: $v"),
          digest.alignmentBand.map(v => s"plan fit: $v"),
          digest.deploymentPiece.map { piece =>
            val route =
              if digest.deploymentRoute.nonEmpty then digest.deploymentRoute.mkString("-")
              else "n/a"
            s"deployment: $piece $route"
          },
          digest.deploymentPurpose.map(v => s"deployment purpose: $v"),
          digest.prophylaxisPlan.map(v => s"prophylaxis: $v"),
          digest.decision.map(v => s"decision: $v"),
          digest.decisionComparison.flatMap(_.engineBestMove).map(v => s"engine best: $v"),
          digest.decisionComparison.flatMap(_.deferredMove).map { v =>
            val label = if digest.decisionComparison.exists(_.practicalAlternative) then "deferred practical" else "deferred"
            s"$label: $v"
          },
          digest.opponentPlan.map(v => s"opponent plan: $v")
        ).flatten
      }
    (planHints ++ routeHints ++ focusHints ++ digestHints).map(_.trim).filter(_.nonEmpty).distinct.take(8)

  private def buildPlans(
      ctx: NarrativeContext,
      sideToMoveColor: Color
  ): List[StrategySidePlan] =
    val mover = sideName(sideToMoveColor)
    val opponent = sideName(!sideToMoveColor)

    val moverFromHypothesis =
      ctx.mainStrategicPlans
        .sortBy(h => (-h.score, h.rank))
        .map(h => fromHypothesis(mover, h))

    val moverFromPlanTable =
      ctx.plans.top5
        .sortBy(r => (r.rank, -r.score))
        .map(p => fromPlanRow(mover, p.name, p.supports ++ p.evidence, p.blockers ++ p.missingPrereqs))

    val moverPlans =
      (moverFromHypothesis ++ moverFromPlanTable)
        .distinctBy(_.planName.trim.toLowerCase)
        .take(3)

    val opponentPlan =
      ctx.opponentPlan.map { p =>
        fromPlanRow(opponent, p.name, p.supports ++ p.evidence, p.blockers ++ p.missingPrereqs)
      }

    (moverPlans ++ opponentPlan.toList).distinctBy(p => s"${p.side}|${p.planName.trim.toLowerCase}").take(MaxPlans)

  private def buildRoutes(
      data: ExtendedAnalysisData,
      sideToMoveColor: Color,
      plans: List[StrategySidePlan],
      structureArc: Option[StructurePlanArc]
  ): List[StrategyPieceRoute] =
    val boardOpt =
      Fen.read(_root_.chess.variant.Standard, Fen.Full(data.fen)).map(_.board)
    val sides = List(sideToMoveColor, !sideToMoveColor)
    val sideToMove = sideName(sideToMoveColor)
    val preferredRouteKey =
      structureArc.map(arc => s"${arc.primaryDeployment.piece}|${arc.primaryDeployment.from}|${arc.primaryDeployment.route.lastOption.getOrElse(arc.primaryDeployment.from)}")

    sides
      .flatMap { routeColor =>
        val routeSide = sideName(routeColor)
        val sidePlans = plans.filter(_.side == routeSide)
        data.pieceActivity.flatMap(pa => toPieceRoute(pa, boardOpt, routeColor, data.positionalFeatures, sidePlans))
      }
      .sortBy { r =>
        val routeKey = s"${r.piece}|${r.from}|${r.route.lastOption.getOrElse(r.from)}"
        val preferred = preferredRouteKey.contains(routeKey)
        (if preferred then 0 else 1, if r.side == sideToMove then 0 else 1, -r.confidence)
      }
      .distinctBy(r => s"${r.side}|${r.piece}|${r.from}|${r.route.lastOption.getOrElse(r.from)}")
      .take(MaxRoutes)

  private def toPieceRoute(
      pa: PieceActivity,
      boardOpt: Option[_root_.chess.Board],
      routeColor: Color,
      positionalFeatures: List[PositionalTag],
      plans: List[StrategySidePlan]
  ): Option[StrategyPieceRoute] =
    StructurePlanArcBuilder
      .cueFromStrategicActivity(
        activity = pa,
        boardOpt = boardOpt,
        routeColor = routeColor,
        positionalFeatures = positionalFeatures,
        planLabels = plans.map(_.planName)
      )
      .map { cue =>
        StrategyPieceRoute(
          side = sideName(routeColor),
          piece = cue.piece,
          from = cue.from,
          route = cue.routeSquares,
          purpose = cue.purpose,
          confidence = cue.confidence,
          evidence = StructurePlanArcBuilder.evidenceFromStrategicActivity(pa, cue.lastSquare.getOrElse(cue.from))
        )
      }

  private def fromHypothesis(side: String, h: PlanHypothesis): StrategySidePlan =
    StrategySidePlan(
      side = side,
      horizon = if h.score >= 0.68 then "long" else "medium",
      planName = h.planName,
      priorities = h.executionSteps.take(3),
      riskTriggers = h.failureModes.take(3)
    )

  private def fromPlanRow(
      side: String,
      name: String,
      priorities: List[String],
      risks: List[String]
  ): StrategySidePlan =
    StrategySidePlan(
      side = side,
      horizon = "medium",
      planName = name,
      priorities = priorities.take(3),
      riskTriggers = risks.take(3)
    )

  private def buildLongTermFocus(
      ctx: NarrativeContext,
      plans: List[StrategySidePlan],
      routes: List[StrategyPieceRoute],
      structureArc: Option[StructurePlanArc]
  ): List[String] =
    val scored = scala.collection.mutable.HashMap.empty[String, Double]
    val dominantThesis = StrategicThesisBuilder.build(ctx).map(_.claim).map(_.trim).filter(_.nonEmpty)
    val planNameKeys = plans.map(_.planName.trim.toLowerCase).toSet

    def push(raw: String, weight: Double): Unit =
      val clean = raw.trim
      if clean.nonEmpty then
        val key = clean.toLowerCase
        val existing = scored.getOrElse(key, Double.MinValue)
        if weight > existing then scored.update(key, weight)

    plans.take(3).zipWithIndex.foreach { case (plan, idx) =>
      val baseWeight = 2.30 - (idx * 0.20)
      push(s"${plan.side} plan: ${plan.planName}", baseWeight)
      plan.priorities.take(2).zipWithIndex.foreach { case (priority, i) =>
        push(s"${plan.side} execution: ${priority}", baseWeight - 0.35 - (i * 0.10))
      }
      plan.riskTriggers.take(1).foreach { risk =>
        push(s"${plan.side} risk trigger: ${risk}", baseWeight - 0.65)
      }
    }

    ctx.mainStrategicPlans.take(3).foreach { hypothesis =>
      val nameKey = hypothesis.planName.trim.toLowerCase
      if !planNameKeys.contains(nameKey) then
        push(s"hypothesis: ${hypothesis.planName}", 1.55 + hypothesis.score * 0.25)
        hypothesis.executionSteps.take(1).foreach(step => push(s"follow-up: $step", 1.35 + hypothesis.score * 0.20))
    }

    ctx.planContinuity.foreach { continuity =>
      val phase = continuity.phase.toString.toLowerCase
      push(
        s"continuity: ${continuity.planName} (${continuity.consecutivePlies} plies, phase $phase)",
        1.70 + continuity.commitmentScore * 0.30
      )
    }

    routes.take(2).foreach { route =>
      push(
        s"${route.side} ${route.piece} route ${route.route.mkString("-")} for ${route.purpose}",
        1.95 + route.confidence * 0.35
      )
    }

    structureArc.foreach { arc =>
      push(s"structure deployment: ${arc.structureLabel} asks for ${StructurePlanArcBuilder.focusLine(arc)}", 2.18)
      push(s"move contribution: ${arc.moveContribution}", 1.92)
    }
    dominantThesis.foreach(thesis => push(s"dominant thesis: $thesis", 2.42))

    ctx.semantic.foreach { semantic =>
      semantic.conceptSummary.take(4).zipWithIndex.foreach { case (concept, idx) =>
        push(concept, 1.10 - (idx * 0.05))
      }
      semantic.planAlignment.foreach { alignment =>
        push(s"plan alignment: ${alignment.band}", 1.25 + (alignment.score.toDouble / 100.0))
        alignment.narrativeIntent.foreach(intent => push(s"alignment intent: $intent", 1.20))
      }
    }

    ctx.whyAbsentFromTopMultiPV.take(2).foreach { reason =>
      push(s"engine-line gap: $reason", 1.45)
    }
    NarrativeSignalDigestBuilder.build(ctx)
      .flatMap(_.decisionComparison)
      .foreach { comparison =>
        comparison.engineBestMove.foreach(move => push(s"engine best: $move", 1.72))
        comparison.deferredMove.foreach { move =>
          val prefix = if comparison.practicalAlternative then "practical alternative" else "deferred branch"
          push(s"$prefix: $move", 1.68)
        }
      }

    scored.toList
      .sortBy { case (text, weight) => (-weight, text) }
      .map(_._1)
      .take(MaxFocus)

  private def buildEvidence(
      ctx: NarrativeContext,
      routes: List[StrategyPieceRoute],
      structureArc: Option[StructurePlanArc]
  ): List[String] =
    val dominantThesis = StrategicThesisBuilder.build(ctx).map(_.claim).map(_.trim).filter(_.nonEmpty)
    val routeEvidence =
      routes.flatMap(route => route.evidence.map(signal => s"route:${route.side}:$signal"))
    val structureEvidence =
      structureArc.toList.flatMap { arc =>
        List(
          Some(s"structure_arc:${arc.structureLabel}:${arc.planLabel}"),
          Some(s"deployment:${arc.primaryDeployment.piece}:${arc.primaryDeployment.routeSquares.mkString("-")}:${arc.primaryDeployment.purpose}"),
          Some(s"deployment_contribution:${arc.moveContribution}")
        ).flatten
        }
    val comparisonEvidence =
      DecisionComparisonBuilder.build(ctx).toList.flatMap { comparison =>
        List(
          comparison.engineBestMove.map(move => s"decision_compare:engine_best:$move"),
          comparison.deferredMove.map { move =>
            val prefix = if comparison.practicalAlternative then "practical" else "deferred"
            s"decision_compare:${prefix}:$move"
          },
          comparison.deferredReason.map(reason => s"decision_compare:why:$reason"),
          comparison.evidence.map(ev => s"decision_compare:evidence:$ev")
        ).flatten
      }
    val authoringEvidence =
      AuthoringEvidenceSummaryBuilder
        .summarizeEvidence(ctx)
        .flatMap { summary =>
          val branchHint =
            Option.when(summary.branchCount > 0)(s"branches=${summary.branchCount}")
          val pendingHint =
            Option.when(summary.pendingProbeCount > 0)(s"pending_probes=${summary.pendingProbeCount}")
          val planHint =
            summary.linkedPlans.headOption.map(plan => s"plan=$plan")
          val detail = List(branchHint, pendingHint, planHint).flatten.mkString(" ")
          Option(summary.question.trim)
            .filter(_.nonEmpty)
            .map { question =>
              val suffix = if detail.nonEmpty then s" [$detail]" else ""
              s"authoring:${summary.questionKind}:${summary.status}:$question$suffix"
            }
        }
    (
      ctx.mainStrategicPlans.flatMap(_.evidenceSources) ++
        ctx.whyAbsentFromTopMultiPV ++
        routeEvidence ++
        structureEvidence ++
        comparisonEvidence ++
        dominantThesis.toList.map(v => s"dominant_thesis:$v") ++
        authoringEvidence ++
        AuthoringEvidenceSummaryBuilder.headline(ctx).toList.map(v => s"authoring_headline:$v")
    ).map(_.trim).filter(_.nonEmpty).distinct.take(MaxEvidence)

  private def sideName(color: Color): String =
    if color.white then "white" else "black"
