package lila.llm.analysis

import _root_.chess.Square
import _root_.chess.format.Fen
import _root_.chess.variant.Standard

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
      val delta =
        PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
          ctx,
          StrategyPackSurface.from(pack),
          None
        )
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
      println(s"structural_weaknesses=${renderWeaknesses(ctx.semantic.toList.flatMap(_.structuralWeaknesses))}")
      println(s"decision_focus=${renderDecisionFocus(ctx.decision)}")
      println(s"target_fixation_diag=${renderTargetFixationDiag(ctx, pack)}")
      println(s"surface_targets=${pack.map(renderTargets).getOrElse("-")}")
      println(s"surface_move_refs=${pack.map(renderMoveRefs).getOrElse("-")}")
      println(s"surface_routes=${pack.map(renderRoutes).getOrElse("-")}")
      println(s"analysis_plans=${renderPlanMatches(data.plans)}")
      println(s"main_plans=${renderPlans(ctx.mainStrategicPlans)}")
      println(s"experiments=${renderExperiments(ctx.strategicPlanExperiments)}")
      println(s"prevented_now=${renderPreventedPlans(ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now))}")
      println(s"named_route_surface=${inputs.namedRouteNetworkSurface}")
      println(s"main_delta_packet=${delta.map(renderDeltaPacket).getOrElse("-")}")
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
      AuthorQuestion("what_matters_here", AuthorQuestionKind.WhatMattersHere, 90, "What matters here?"),
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
            s"focus=${idea.focusSquares.take(3).mkString(",")}",
            s"evidence=${idea.evidenceRefs.take(3).mkString(",")}"
          ).mkString("[", " | ", "]")
        )
        .mkString(" ; ")

  private def renderWeaknesses(weaknesses: List[WeakComplexInfo]): String =
    if weaknesses.isEmpty then "-"
    else
      weaknesses
        .take(5)
        .map(weakness =>
          List(
            weakness.owner,
            weakness.squareColor,
            s"squares=${weakness.squares.mkString(",")}",
            s"cause=${weakness.cause}"
          ).mkString("[", " | ", "]")
        )
        .mkString(" ; ")

  private def renderDecisionFocus(decision: Option[DecisionRationale]): String =
    decision
      .map(info =>
        List(
          s"focal=${info.focalPoint.map(_.label).getOrElse("-")}",
          s"logic=${info.logicSummary}",
          s"new=${info.delta.newOpportunities.mkString(",")}",
          s"plan=${info.delta.planAdvancements.mkString(",")}"
        ).mkString("[", " | ", "]")
      )
      .getOrElse("-")

  private def renderTargetFixationDiag(
      ctx: NarrativeContext,
      packOpt: Option[lila.llm.StrategyPack]
  ): String =
    val targetFixIdea =
      packOpt.toList
        .flatMap(_.strategicIdeas)
        .find(_.kind == lila.llm.StrategicIdeaKind.TargetFixing)
    val branchKey =
      ctx.engineEvidence.toList
        .flatMap(_.variations)
        .headOption
        .flatMap(line =>
          line.moves.take(2).flatMap(move => Option(move).map(_.trim).filter(_.nonEmpty)) match
            case first :: second :: Nil => Some(s"${first.toLowerCase}|${second.toLowerCase}")
            case _                      => None
        )
        .getOrElse("-")
    val occupants =
      targetFixIdea
        .map { idea =>
          val boardOpt = Fen.read(Standard, Fen.Full(ctx.fen)).map(_.board)
          idea.focusSquares.map(_.trim).filter(_.nonEmpty).distinct.map { squareKey =>
            val occupant =
              boardOpt
                .flatMap(board => Square.all.find(_.key == squareKey.toLowerCase).flatMap(board.pieceAt))
                .map(piece => s"${piece.color.name}:${piece.role}")
                .getOrElse("-")
            s"$squareKey=$occupant"
          }.mkString(",")
        }
        .getOrElse("-")
    s"[branchKey=$branchKey | occupants=$occupants]"

  private def renderTargets(pack: lila.llm.StrategyPack): String =
    if pack.directionalTargets.isEmpty then "-"
    else
      pack.directionalTargets
        .take(5)
        .map(target =>
          List(
            target.targetSquare,
            target.piece,
            target.readiness.toString,
            s"reasons=${target.strategicReasons.mkString(",")}",
            s"evidence=${target.evidence.take(3).mkString(",")}"
          ).mkString("[", " | ", "]")
        )
        .mkString(" ; ")

  private def renderMoveRefs(pack: lila.llm.StrategyPack): String =
    if pack.pieceMoveRefs.isEmpty then "-"
    else
      pack.pieceMoveRefs
        .take(5)
        .map(ref =>
          List(
            s"${ref.from}->${ref.target}",
            ref.piece,
            ref.idea,
            s"evidence=${ref.evidence.take(3).mkString(",")}"
          ).mkString("[", " | ", "]")
        )
        .mkString(" ; ")

  private def renderRoutes(pack: lila.llm.StrategyPack): String =
    if pack.pieceRoutes.isEmpty then "-"
    else
      pack.pieceRoutes
        .take(5)
        .map(route =>
          List(
            route.piece,
            route.route.mkString("->"),
            route.purpose,
            route.surfaceMode.toString,
            s"evidence=${route.evidence.take(3).mkString(",")}"
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

  private def renderDeltaPacket(delta: PlayerFacingMoveDeltaEvidence): String =
    val packet = delta.packet
    List(
      s"deltaClass=${delta.deltaClass}",
      s"ownerSource=${packet.ownerSource}",
      s"ownerFamily=${packet.ownerFamily}",
      s"trigger=${packet.triggerKind}",
      s"bestDefenseBranchKey=${packet.bestDefenseBranchKey.getOrElse("-")}",
      s"sameBranchState=${packet.sameBranchState}",
      s"persistence=${packet.persistence}",
      s"suppression=${if packet.suppressionReasons.isEmpty then "-" else packet.suppressionReasons.mkString(",")}",
      s"releaseRisks=${if packet.releaseRisks.isEmpty then "-" else packet.releaseRisks.mkString(",")}",
      s"fallback=${packet.fallbackMode}",
      s"ownerSeed=${if packet.ownerPathWitness.ownerSeedTerms.isEmpty then "-" else packet.ownerPathWitness.ownerSeedTerms.mkString(",")}",
      s"continuation=${if packet.ownerPathWitness.continuationTerms.isEmpty then "-" else packet.ownerPathWitness.continuationTerms.mkString(",")}"
    ).mkString("[", " | ", "]")
