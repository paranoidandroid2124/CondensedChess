package lila.llm.analysis

import lila.llm.model.*
import lila.llm.model.authoring.AuthorQuestion
import lila.llm.model.authoring.AuthorQuestionKind
import lila.llm.model.strategic.VariationLine

object TaskShiftReviewWorkup:

  private val fixtures = TaskShiftProvingFixtures.reviewFixtures

  @main def runTaskShiftReviewWorkup(ids: String*): Unit =
    val requested = ids.map(_.trim).filter(_.nonEmpty).toSet
    val selected =
      fixtures.filter(fixture => requested.isEmpty || requested.contains(fixture.id))

    selected.foreach { fixture =>
      val data =
        CommentaryEngine
          .assessExtended(
            fen = fixture.fen,
            variations = List(VariationLine(fixture.pvMoves, fixture.scoreCp, depth = 16)),
            phase = Some(fixture.phase),
            ply = fixture.ply
          )
          .getOrElse(sys.error(s"analysis missing for ${fixture.id}"))
      val ctx =
        NarrativeContextBuilder
          .build(data, data.toContext, None)
          .copy(authorQuestions = defaultQuestions)
      val pack =
        StrategyPackBuilder.build(data, ctx)
      val inputs =
        QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
      val ranked =
        QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)

      println(s"=== ${fixture.id} :: ${fixture.label} ===")
      println(s"fen=${fixture.fen}")
      println(s"expected_tags=${fixture.expectedTags.mkString(",")}")
      println(s"expected_note=${fixture.note}")
      println(s"phase=${ctx.phase.current} ply=${ctx.ply} taskMode=${ctx.header.taskMode}")
      println(s"pv=${fixture.pvMoves.mkString(" ")}")
      println(s"strategy_ideas=${pack.map(renderIdeas).getOrElse("-")}")
      println(s"analysis_plans=${renderPlanMatches(data.plans)}")
      println(s"main_plans=${renderPlans(ctx.mainStrategicPlans)}")
      println(s"experiments=${renderExperiments(ctx.strategicPlanExperiments)}")
      println(s"prevented_now=${renderPreventedPlans(ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now))}")
      println(s"named_route_surface=${inputs.namedRouteNetworkSurface}")
      println(s"main_bundle=${inputs.mainBundle.flatMap(_.mainClaim).map(_.claimText).getOrElse("-")}")
      println(s"line_claim=${inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText).getOrElse("-")}")
      println(s"quiet_intent=${inputs.quietIntent.map(_.claimText).getOrElse("-")}")
      println(s"planner_primary=${ranked.primary.map(renderPlan).getOrElse("-")}")
      println(s"planner_secondary=${ranked.secondary.map(renderPlan).getOrElse("-")}")
      println(
        s"planner_rejected=${
            ranked.rejected
              .map(rejected =>
                s"${rejected.questionKind}:${rejected.fallbackMode}:${rejected.reasons.mkString("+")}"
              )
              .mkString(" || ")
          }"
      )
      println()
    }

  private def defaultQuestions =
    List(
      AuthorQuestion("why_this", AuthorQuestionKind.WhyThis, 100, "Why this move?"),
      AuthorQuestion("what_changed", AuthorQuestionKind.WhatChanged, 80, "What changed?"),
      AuthorQuestion("why_now", AuthorQuestionKind.WhyNow, 60, "Why now?")
    )

  private def renderPlans(plans: List[lila.llm.model.authoring.PlanHypothesis]): String =
    if plans.isEmpty then "-"
    else
      plans
        .map(plan =>
          List(
            plan.planId,
            plan.subplanId.getOrElse("-"),
            plan.planName,
            f"score=${plan.score}%.2f",
            s"sources=${plan.evidenceSources.take(3).mkString(",")}"
          ).mkString("[", " | ", "]")
        )
        .mkString(" ; ")

  private def renderIdeas(pack: lila.llm.StrategyPack): String =
    if pack.strategicIdeas.isEmpty then "-"
    else
      pack.strategicIdeas
        .take(5)
        .map(idea =>
          List(
            idea.kind,
            f"conf=${idea.confidence}%.2f",
            s"evidence=${idea.evidenceRefs.take(3).mkString(",")}"
          ).mkString("[", " | ", "]")
        )
        .mkString(" ; ")

  private def renderPlanMatches(plans: List[lila.llm.model.PlanMatch]): String =
    if plans.isEmpty then "-"
    else
      plans
        .take(5)
        .map(plan =>
          List(
            plan.plan.id.toString,
            f"score=${plan.score}%.2f",
            s"supports=${plan.supports.take(4).mkString(",")}"
          ).mkString("[", " | ", "]")
        )
        .mkString(" ; ")

  private def renderExperiments(experiments: List[StrategicPlanExperiment]): String =
    if experiments.isEmpty then "-"
    else
      experiments
        .map(experiment =>
          List(
            experiment.planId,
            experiment.subplanId.getOrElse("-"),
            experiment.themeL1,
            experiment.evidenceTier,
            s"stable=${experiment.bestReplyStable}",
            s"future=${experiment.futureSnapshotAligned}",
            s"moveOrder=${experiment.moveOrderSensitive}"
          ).mkString("[", " | ", "]")
        )
        .mkString(" ; ")

  private def renderPreventedPlans(plans: List[PreventedPlanInfo]): String =
    if plans.isEmpty then "-"
    else
      plans
        .map(plan =>
          List(
            plan.planId,
            plan.breakNeutralized.getOrElse("-"),
            s"drop=${plan.counterplayScoreDrop}",
            s"denied=${plan.deniedSquares.mkString(",")}"
          ).mkString("[", " | ", "]")
        )
        .mkString(" ; ")

  private def renderPlan(plan: QuestionPlan): String =
    List(
      plan.questionKind.toString,
      plan.ownerFamily.wireName,
      plan.ownerSource,
      plan.claim,
      s"sourceKinds=${plan.sourceKinds.mkString(",")}"
    ).mkString("[", " | ", "]")
