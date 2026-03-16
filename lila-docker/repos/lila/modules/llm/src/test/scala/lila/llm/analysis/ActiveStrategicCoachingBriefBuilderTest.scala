package lila.llm.analysis

import munit.FunSuite

import lila.llm.*

class ActiveStrategicCoachingBriefBuilderTest extends FunSuite:

  private val tacticalFen = "r2qk2r/1b1nbppp/pp1ppn2/8/2PQ4/BPN2NP1/P3PPBP/R2R2K1 w kq - 2 11"
  private val tacticalFenAfter = "r2qk2r/1b1nbppp/pp1Qpn2/8/2P5/BPN2NP1/P3PPBP/R2R2K1 b kq - 0 11"

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
    assertEquals(brief.executionHint, Some("knight toward e3 to reinforce the kingside clamp"))
    assertEquals(brief.longTermObjective, Some("making g4 available for the knight (build)"))
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

  test("build prioritizes immediate tactical gain in why-now when available") {
    val brief =
      ActiveStrategicCoachingBriefBuilder.build(
        strategyPack = strategyPack,
        dossier = dossier,
        routeRefs = routeRefs,
        moveRefs =
          List(
            ActiveStrategicMoveRef(
              label = "Engine preference",
              source = "top_engine_move",
              uci = "d4d6",
              san = Some("Qxd6"),
              fenAfter = Some(tacticalFenAfter)
            )
          ),
        currentFen = Some(tacticalFen)
      )

    assertEquals(brief.whyNow, Some("Qxd6 immediately wins a pawn."))
  }

  test("build rewrites occupied-square focus language without banning control globally") {
    val focusedDossier =
      Some(
        ActiveBranchDossier(
          dominantLens = "structure",
          chosenBranchLabel = "queenside bind",
          whyChosen = Some("Focus on c3."),
          opponentResource = Some("Black still hopes for ...b5.")
        )
      )

    val brief =
      ActiveStrategicCoachingBriefBuilder.build(
        strategyPack = strategyPack,
        dossier = focusedDossier,
        routeRefs = routeRefs,
        moveRefs = Nil,
        currentFen = Some(tacticalFen)
      )

    assertEquals(brief.whyNow, Some("Keep the knight anchored on c3."))
    assertEquals(
      ActiveStrategicCoachingBriefBuilder.build(
        strategyPack = strategyPack,
        dossier =
          Some(
            ActiveBranchDossier(
              dominantLens = "structure",
              chosenBranchLabel = "queenside bind",
              whyChosen = Some("Control of c3 keeps the queenside sealed.")
            )
          ),
        routeRefs = routeRefs,
        moveRefs = Nil,
        currentFen = Some(tacticalFen)
      ).whyNow,
      Some("Control of c3 keeps the queenside sealed.")
    )
  }

  test("build surfaces campaign owner and compensation objective when owner mismatches side to move") {
    val compensationPack =
      Some(
        strategyPack.get.copy(
          sideToMove = "black",
          strategicIdeas = List(
            strategyPack.get.strategicIdeas.head.copy(
              ownerSide = "white",
              kind = StrategicIdeaKind.KingAttackBuildUp,
              group = StrategicIdeaGroup.InteractionAndTransformation
            )
          ),
          longTermFocus = List("keep the initiative rather than recovering material"),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("initiative against the king"),
              investedMaterial = Some(160),
              dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
              dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("g7, h7")
            )
          )
        )
      )

    val brief = ActiveStrategicCoachingBriefBuilder.build(compensationPack, dossier, routeRefs, Nil)

    assertEquals(brief.campaignRole, Some("White's campaign: the plan is being consolidated move by move"))
    assert(brief.primaryIdea.exists(_.startsWith("White: ")))
    assert(brief.whyNow.exists(_.toLowerCase.contains("do not rush to recover the material")))
    assert(
      brief.longTermObjective.exists(text =>
        text.toLowerCase.contains("recovering the material") ||
          text.toLowerCase.contains("compensation") ||
          text.toLowerCase.contains("initiative") ||
          text.toLowerCase.contains("making g4 available")
      )
    )
  }

  test("deterministic note stays acceptable for compensation owner-mismatch cases") {
    val compensationPack =
      Some(
        strategyPack.get.copy(
          sideToMove = "black",
          strategicIdeas = List(
            strategyPack.get.strategicIdeas.head.copy(
              ownerSide = "white",
              kind = StrategicIdeaKind.KingAttackBuildUp,
              group = StrategicIdeaGroup.InteractionAndTransformation
            )
          ),
          longTermFocus = List("keep the initiative rather than recovering material"),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("initiative against the king"),
              investedMaterial = Some(160),
              dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
              dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("g7, h7"),
              opponentPlan = Some("Black still hopes for ...c5 counterplay.")
            )
          )
        )
      )

    val note =
      ActiveStrategicCoachingBriefBuilder.buildDeterministicNote(
        strategyPack = compensationPack,
        dossier = dossier,
        routeRefs = routeRefs,
        moveRefs = Nil
      )

    assert(note.nonEmpty)
    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText = note.get,
        baseNarrative = "Black to move, but White still owns the initiative.",
        dossier = dossier,
        strategyPack = compensationPack,
        routeRefs = routeRefs,
        moveRefs = Nil,
        strategyReasons = Nil
      )

    assert(result.isAccepted)
    assert(note.exists(_.contains("White")))
    assert(
      note.exists(text =>
        text.toLowerCase.contains("initiative") ||
          text.toLowerCase.contains("compensation") ||
          text.toLowerCase.contains("recover the material")
      )
    )
  }

  test("quiet compensation note uses normalized dominant idea and execution instead of raw kingside clamp wording") {
    val quietCompensationPack =
      Some(
        StrategyPack(
          sideToMove = "black",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "black",
              piece = "R",
              from = "a8",
              route = List("a8", "d8", "d3"),
              purpose = "kingside clamp",
              strategicFit = 0.82,
              tacticalSafety = 0.77,
              surfaceConfidence = 0.79,
              surfaceMode = RouteSurfaceMode.Toward
            )
          ),
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "black",
              piece = "Q",
              from = "d8",
              target = "b6",
              idea = "fix the queenside targets"
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_b2",
              ownerSide = "black",
              piece = "R",
              from = "d8",
              targetSquare = "b2",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("backward pawn")
            )
          ),
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_benko_line",
              ownerSide = "black",
              kind = StrategicIdeaKind.LineOccupation,
              group = "slow_structural",
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("b2", "c4", "d4"),
              focusFiles = List("b", "c", "d"),
              focusZone = Some("queenside"),
              beneficiaryPieces = List("R", "Q"),
              confidence = 0.86
            ),
            StrategyIdeaSignal(
              ideaId = "idea_benko_targets",
              ownerSide = "black",
              kind = StrategicIdeaKind.TargetFixing,
              group = "slow_structural",
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("b2", "a6"),
              focusFiles = List("a", "b"),
              focusZone = Some("queenside"),
              beneficiaryPieces = List("R"),
              confidence = 0.79
            )
          ),
          longTermFocus = List("fix the queenside targets before recovering the pawn"),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through line pressure and delayed recovery"),
              compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
              investedMaterial = Some(100),
              dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
              dominantIdeaGroup = Some("slow_structural"),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("b2, c4, d4")
            )
          )
        )
      )

    val brief = ActiveStrategicCoachingBriefBuilder.build(quietCompensationPack, None, Nil, Nil)

    assertEquals(brief.primaryIdea, Some("fixed queenside targets"))
    assert(brief.executionHint.exists(_.toLowerCase.contains("queenside targets")), clue(brief.executionHint))
    assert(!brief.executionHint.exists(_.toLowerCase.contains("kingside clamp")), clue(brief.executionHint))
    assert(
      brief.longTermObjective.exists(text =>
        text.toLowerCase.contains("queenside targets") || text.toLowerCase.contains("queenside files")
      ),
      clue(brief.longTermObjective)
    )
  }

  test("attack-led compensation note keeps raw initiative wording when subtype normalization is inactive") {
    val brief =
      ActiveStrategicCoachingBriefBuilder.build(
        strategyPack = BookmakerProseGoldenFixtures.exchangeSacrifice.strategyPack,
        dossier = None,
        routeRefs = Nil,
        moveRefs = Nil
      )

    assert(
      brief.primaryIdea.exists { text =>
        val lowered = text.toLowerCase
        lowered.contains("king-attack") || lowered.contains("king attack")
      }
    )
    assert(
      brief.whyNow.exists(text =>
        text.toLowerCase.contains("initiative alive") || text.toLowerCase.contains("initiative")
      ),
      clue(brief.whyNow)
    )
    assert(!brief.whyNow.exists(_.toLowerCase.contains("open-line pressure")), clue(brief.whyNow))
  }

  test("deterministic note normalizes raw route and move-ref phrasing") {
    val roughPack =
      Some(
        strategyPack.get.copy(
          pieceRoutes = List(
            StrategyPieceRoute(
              side = "black",
              piece = "Black R",
              from = "b8",
              route = List("b8", "b7"),
              purpose = "kingside clamp",
              confidence = 0.82
            )
          ),
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "black",
              piece = "B",
              from = "f1",
              target = "c4",
              idea = "contest the pawn on c4"
            )
          ),
          strategicIdeas = List(
            strategyPack.get.strategicIdeas.head.copy(
              ownerSide = "black",
              kind = StrategicIdeaKind.KingAttackBuildUp,
              group = StrategicIdeaGroup.InteractionAndTransformation
            )
          ),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("initiative against the king"),
              investedMaterial = Some(120),
              dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
              dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("g7, h7")
            )
          )
        )
      )

    val note =
      ActiveStrategicCoachingBriefBuilder.buildDeterministicNote(
        strategyPack = roughPack,
        dossier = None,
        routeRefs = Nil,
        moveRefs = Nil
      )

    assert(note.nonEmpty)
    assert(note.exists(text => !text.contains("Black R toward")))
    assert(note.exists(text => !text.contains("for contest")))
    assert(note.exists(text => !text.contains("for keep")))
    assert(note.exists(text => !text.contains("working toward working toward")))
    assert(note.exists(text => text.contains("to contest") || text.contains("to keep the pressure fixed there")))
  }
