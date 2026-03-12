package lila.llm.analysis

import munit.FunSuite

import lila.llm.*

class ActiveStrategicCoachingBriefBuilderTest extends FunSuite:

  private val strategyPack = Some(
    StrategyPack(
      sideToMove = "white",
      plans = List(
        StrategySidePlan(
          side = "white",
          horizon = "long",
          planName = "Kingside expansion",
          priorities = List("Push f-pawn"),
          riskTriggers = List("Back rank weakness")
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          side = "white",
          piece = "N",
          from = "d2",
          route = List("d2", "f1", "e3"),
          purpose = "kingside clamp",
          confidence = 0.81
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_white_n_d2_g4",
          ownerSide = "white",
          piece = "N",
          from = "d2",
          targetSquare = "g4",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("supports kingside expansion"),
          prerequisites = List("prepare the supporting squares first")
        )
      ),
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_1",
          ownerSide = "white",
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          group = StrategicIdeaGroup.StructuralChange,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("e3", "g4"),
          focusZone = Some("kingside"),
          beneficiaryPieces = List("N"),
          confidence = 0.89
        )
      ),
      signalDigest = Some(
        NarrativeSignalDigest(
          structuralCue = Some("French Chain structure with a semi-open center"),
          decision = Some("Resolves back-rank pressure before expanding"),
          opponentPlan = Some("Black still wants ...c5 counterplay.")
        )
      )
    )
  )

  private val dossier = Some(
    ActiveBranchDossier(
      dominantLens = "structure",
      chosenBranchLabel = "French Chain -> kingside clamp",
      whyChosen = Some("This move starts the knight reroute demanded by the structure."),
      whyDeferred = Some("If White hesitates, Black's queenside counterplay returns."),
      opponentResource = Some("Black still hopes for ...c5 counterplay."),
      routeCue = Some(
        ActiveBranchRouteCue(
          routeId = "route_1",
          piece = "N",
          route = List("d2", "f1", "e3"),
          purpose = "kingside clamp",
          confidence = 0.81
        )
      ),
      continuationFocus = Some("White still wants to clamp the kingside dark squares."),
      practicalRisk = Some("If White drifts, the queenside counterplay revives."),
      threadStage = Some("Build")
    )
  )

  private val routeRefs = List(
    ActiveStrategicRouteRef(
      routeId = "route_1",
      piece = "N",
      route = List("d2", "f1", "e3"),
      purpose = "kingside clamp",
      confidence = 0.81
    )
  )

  test("build compresses strategy context into a coaching brief") {
    val brief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, Nil)

    assertEquals(brief.campaignRole, Some("the plan is being consolidated move by move"))
    assertEquals(brief.primaryIdea, Some("space gain or restriction around e3, g4"))
    assertEquals(brief.whyNow, Some("This move starts the knight reroute demanded by the structure."))
    assertEquals(brief.opponentReply, Some("Black still hopes for ...c5 counterplay."))
    assertEquals(brief.executionHint, Some("knight toward e3 for kingside clamp"))
    assertEquals(brief.longTermObjective, Some("work toward making g4 available for the knight"))
    assertEquals(brief.keyTrigger, Some("If White drifts, the queenside counterplay revives."))
  }

  test("coverage detects grounded forward-looking coaching language") {
    val brief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, Nil)
    val coverage =
      ActiveStrategicCoachingBriefBuilder.evaluateCoverage(
        text =
          "With the back-rank pressure resolved, White can keep building the space gain and restriction plan around e3 and g4 by bringing the knight toward e3, where it helps hold down ...c5 counterplay.",
        brief = brief
      )

    assert(coverage.hasDominantIdea)
    assert(coverage.hasGroundedSignal)
    assert(coverage.hasForwardPlan)
    assert(coverage.hasOpponentOrTrigger)
  }

  test("coverage rejects descriptive text without a forward plan") {
    val brief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, Nil)
    val coverage =
      ActiveStrategicCoachingBriefBuilder.evaluateCoverage(
        text = "The space gain or restriction idea is centered on e3 and g4, and Black still has queenside counterplay.",
        brief = brief
      )

    assert(coverage.hasGroundedSignal)
    assert(!coverage.hasForwardPlan)
  }
