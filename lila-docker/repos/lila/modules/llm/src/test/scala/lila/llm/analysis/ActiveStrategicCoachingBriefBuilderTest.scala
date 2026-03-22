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
    assertEquals(brief.primaryIdea, Some("space in the kingside"))
    assertEquals(brief.whyNow, Some("This move starts the knight reroute demanded by the structure."))
    assertEquals(brief.opponentReply, Some("Black still hopes for ...c5 counterplay."))
    assertEquals(brief.executionHint, Some("knight toward e3 to reinforce the kingside clamp"))
    assert(
      brief.longTermObjective.exists(text =>
        text.toLowerCase.contains("g4") &&
          text.toLowerCase.contains("knight")
      )
    )
    assertEquals(brief.keyTrigger, Some("If White drifts, the queenside counterplay revives."))
  }

  test("coverage detects grounded forward-looking coaching language") {
    val brief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, Nil)
    val coverage =
      ActiveStrategicCoachingBriefBuilder.evaluateCoverage(
        text =
          "With the back-rank pressure resolved, White can keep building space on the kingside by bringing the knight to e3, where it helps hold down ...c5 counterplay.",
        brief = brief
      )

    assert(coverage.hasDominantIdea)
    assert(coverage.hasGroundedSignal)
    assert(coverage.hasForwardPlan)
    assert(coverage.hasOpponentOrTrigger)
  }

  test("coverage keeps descriptive space language grounded") {
    val brief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, Nil)
    val coverage =
      ActiveStrategicCoachingBriefBuilder.evaluateCoverage(
        text = "White still has more space on the kingside, and Black still has queenside counterplay.",
        brief = brief
      )

    assert(coverage.hasGroundedSignal)
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
    assert(
      brief.whyNow.exists { text =>
        val lowered = text.toLowerCase
        lowered.contains("recover the material") ||
          lowered.contains("recovering material") ||
          lowered.contains("initiative") ||
          lowered.contains("material can wait") ||
          lowered.contains("gives up material")
      },
      clue(brief.whyNow)
    )
    assert(brief.longTermObjective.exists(_.trim.nonEmpty), clue(brief.longTermObjective))
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
    assert(note.exists(_.contains("White")))
    assert(note.exists(_.trim.nonEmpty))
    assert(!note.exists(_.toLowerCase.contains("plan still revolves around")))
    assert(!note.exists(_.toLowerCase.contains("a useful route is")))
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
    val note =
      ActiveStrategicCoachingBriefBuilder.buildDeterministicNote(
        strategyPack = quietCompensationPack,
        dossier = None,
        routeRefs = Nil,
        moveRefs = Nil
      )

    assertEquals(brief.primaryIdea, Some("fixed queenside targets"))
    assertEquals(brief.compensationLead, Some("queenside pressure against fixed targets"))
    assert(!brief.executionHint.exists(_.toLowerCase.contains("kingside clamp")), clue(brief.executionHint))
    assert(
      brief.whyNow.exists { text =>
        val lowered = text.toLowerCase
        (lowered.contains("recover the material") && lowered.contains("queenside")) ||
          lowered.contains("material can wait") ||
          lowered.contains("gives up material")
      },
      clue(brief.whyNow)
    )
    assert(
      brief.longTermObjective.exists(text =>
        text.toLowerCase.contains("queenside targets") || text.toLowerCase.contains("queenside files")
      ),
      clue(brief.longTermObjective)
    )
    assert(note.exists(_.toLowerCase.contains("compensation comes from queenside pressure against fixed targets")), clue(note))
  }

  test("line-occupation compensation note falls back to a canonical file-pressure lead") {
    val lineOccupationCompensationPack =
      Some(
        StrategyPack(
          sideToMove = "black",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "black",
              piece = "R",
              from = "a8",
              route = List("a8", "b8", "b4"),
              purpose = "work the queenside files",
              strategicFit = 0.82,
              tacticalSafety = 0.77,
              surfaceConfidence = 0.79,
              surfaceMode = RouteSurfaceMode.Toward
            )
          ),
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "black",
              piece = "R",
              from = "a8",
              target = "b4",
              idea = "bring the rook to b4"
            )
          ),
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_benko_line",
              ownerSide = "black",
              kind = StrategicIdeaKind.LineOccupation,
              group = "slow_structural",
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("a4", "b4", "b2"),
              focusFiles = List("a", "b"),
              focusZone = Some("queenside"),
              beneficiaryPieces = List("R", "Q"),
              confidence = 0.86
            )
          ),
          longTermFocus = List("queenside file pressure before winning the material back"),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through line pressure and delayed recovery"),
              compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)"),
              investedMaterial = Some(100),
              dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
              dominantIdeaGroup = Some("slow_structural"),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("a4, b4, b2")
            )
          )
        )
      )

    val brief = ActiveStrategicCoachingBriefBuilder.build(lineOccupationCompensationPack, None, Nil, Nil)
    val note =
      ActiveStrategicCoachingBriefBuilder.buildDeterministicNote(
        strategyPack = lineOccupationCompensationPack,
        dossier = None,
        routeRefs = Nil,
        moveRefs = Nil
      )
    val surface = StrategyPackSurface.from(lineOccupationCompensationPack)

    assert(brief.compensationLead.exists(_.toLowerCase.contains("queenside file pressure")), clue(brief.compensationLead))
    assert(note.exists(text => CompensationContractMatcher.mentionsCompensationContract(text, surface)), clue(note))
  }

  test("strict compensation fallback can use dossier continuation focus with canonical family") {
    val compensationPack =
      Some(
        StrategyPack(
          sideToMove = "black",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "black",
              piece = "R",
              from = "a8",
              route = List("a8", "b8", "b4"),
              purpose = "work the queenside files",
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
              idea = "fix the queenside targets",
              evidence = List("target_pawn")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_b2",
              ownerSide = "black",
              piece = "R",
              from = "b8",
              targetSquare = "b2",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("backward pawn")
            )
          ),
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_qs_targets",
              ownerSide = "black",
              kind = StrategicIdeaKind.TargetFixing,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("b2", "c4"),
              focusFiles = List("b", "c"),
              focusZone = Some("queenside"),
              beneficiaryPieces = List("R", "Q"),
              confidence = 0.85
            )
          ),
          longTermFocus = List("fix the queenside targets before recovering the pawn"),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through line pressure and delayed recovery"),
              compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
              investedMaterial = Some(100),
              dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
              dominantIdeaGroup = Some(StrategicIdeaGroup.StructuralChange),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("b2, c4")
            )
          )
        )
      )
    val note =
      ActiveStrategicCoachingBriefBuilder.buildStrictCompensationFallbackNote(
        strategyPack = compensationPack,
        dossier =
          Some(
            ActiveBranchDossier(
              dominantLens = "compensation",
              chosenBranchLabel = "queenside bind",
              continuationFocus = Some("keep the queenside files under pressure before winning the material back")
            )
          ),
        routeRefs = Nil,
        moveRefs = Nil
      )

    assert(note.exists(_.toLowerCase.contains("compensation comes from")), clue(note))
    assert(note.exists(_.toLowerCase.contains("anchored on")), clue(note))
    assert(note.exists(_.toLowerCase.contains("from there")), clue(note))
  }

  test("strict compensation fallback can use an aligned directional target as the concrete anchor") {
    val compensationPack =
      Some(
        StrategyPack(
          sideToMove = "black",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "black",
              piece = "Q",
              from = "d8",
              target = "b6",
              idea = "fix the queenside targets",
              evidence = List("target_pawn")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_b2",
              ownerSide = "black",
              piece = "R",
              from = "b8",
              targetSquare = "b2",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("backward pawn")
            )
          ),
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_qs_line",
              ownerSide = "black",
              kind = StrategicIdeaKind.LineOccupation,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("b2", "b4"),
              focusFiles = List("b"),
              focusZone = Some("queenside"),
              beneficiaryPieces = List("R"),
              confidence = 0.83
            )
          ),
          longTermFocus = List("fix the queenside targets before recovering the pawn"),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through line pressure and delayed recovery"),
              compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
              investedMaterial = Some(100),
              dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
              dominantIdeaGroup = Some(StrategicIdeaGroup.StructuralChange),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("b2, b4")
            )
          )
        )
      )
    val note =
      ActiveStrategicCoachingBriefBuilder.buildStrictCompensationFallbackNote(
        strategyPack = compensationPack,
        dossier = None,
        routeRefs = Nil,
        moveRefs = Nil
      )

    assert(note.exists(_.toLowerCase.contains("compensation comes from")), clue(note))
    assert(note.exists(text => text.toLowerCase.contains("b2") || text.toLowerCase.contains("rook can use b2")), clue(note))
    assert(note.exists(_.toLowerCase.contains("from there")), clue(note))
  }

  test("strict compensation fallback can use route cue and execution hint to supply anchor and continuation") {
    val compensationPack =
      Some(
        StrategyPack(
          sideToMove = "black",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "black",
              piece = "B",
              from = "c8",
              route = List("c8", "e6", "g4"),
              purpose = "keep the pressure fixed there",
              strategicFit = 0.81,
              tacticalSafety = 0.78,
              surfaceConfidence = 0.79,
              surfaceMode = RouteSurfaceMode.Toward
            )
          ),
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_center",
              ownerSide = "black",
              kind = StrategicIdeaKind.TargetFixing,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("e4", "d3"),
              focusZone = Some("center"),
              beneficiaryPieces = List("B"),
              confidence = 0.84
            )
          ),
          longTermFocus = List("central targets tied down before winning the material back"),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through line pressure and delayed recovery"),
              compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
              investedMaterial = Some(100),
              dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
              dominantIdeaGroup = Some(StrategicIdeaGroup.StructuralChange),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("e4, d3")
            )
          )
        )
      )
    val note =
      ActiveStrategicCoachingBriefBuilder.buildStrictCompensationFallbackNote(
        strategyPack = compensationPack,
        dossier =
          Some(
            ActiveBranchDossier(
              dominantLens = "compensation",
              chosenBranchLabel = "center bind",
              routeCue = Some(
                ActiveBranchRouteCue(
                  routeId = "route_1",
                  ownerSide = "black",
                  piece = "B",
                  route = List("c8", "e6", "g4"),
                  purpose = "keep the pressure fixed there",
                  strategicFit = 0.81,
                  tacticalSafety = 0.78,
                  surfaceConfidence = 0.79,
                  surfaceMode = RouteSurfaceMode.Toward
                )
              )
            )
          ),
        routeRefs = Nil,
        moveRefs = Nil
      )

    assert(note.exists(_.toLowerCase.contains("compensation comes from")), clue(note))
    assert(note.exists(text => text.toLowerCase.contains("g4") || text.toLowerCase.contains("bishop toward g4")), clue(note))
    assert(note.exists(_.toLowerCase.contains("from there")), clue(note))
  }

  test("strict compensation fallback stays valid for attack-led blocker carriers with noisy why-now text") {
    val compensationPack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "R",
              from = "h1",
              route = List("h1", "g1", "g4"),
              purpose = "kingside clamp",
              strategicFit = 0.8185714285714286,
              tacticalSafety = 1.0,
              surfaceConfidence = 0.8185714285714286,
              surfaceMode = RouteSurfaceMode.Toward
            ),
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "N",
              from = "d2",
              route = List("d2", "c4", "d6"),
              purpose = "kingside pressure",
              strategicFit = 0.7275,
              tacticalSafety = 0.88,
              surfaceConfidence = 0.7275,
              surfaceMode = RouteSurfaceMode.Toward
            )
          ),
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "N",
              from = "d2",
              target = "c6",
              idea = "contest the pawn on c6",
              evidence = List("enemy_occupied_endpoint", "target_pawn", "piece_activity")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_white_n_d2_d3",
              ownerSide = "white",
              piece = "N",
              from = "d2",
              targetSquare = "d3",
              readiness = DirectionalTargetReadiness.Contested,
              strategicReasons = List("supports kingside pawn storm", "central foothold", "improves piece placement")
            )
          ),
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_1",
              ownerSide = "white",
              kind = StrategicIdeaKind.KingAttackBuildUp,
              group = StrategicIdeaGroup.InteractionAndTransformation,
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("d6"),
              focusZone = Some("center"),
              beneficiaryPieces = List("N"),
              confidence = 0.98
            ),
            StrategyIdeaSignal(
              ideaId = "idea_2",
              ownerSide = "white",
              kind = StrategicIdeaKind.TargetFixing,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Ready,
              focusSquares = List("c5", "d6", "e5"),
              focusZone = Some("center"),
              confidence = 0.98
            )
          ),
          longTermFocus = List(
            "The move gives up material because the knight can still head for d3.",
            "compensation carrier: a path to compensation through initiative and continuing pressure, backed by Initiative (0.6) and continuing pressure (0.7)",
            "dominant idea: pressure on d6",
            "secondary idea: fixed targets on c5, d6, and e5",
            "objective: work toward making d3 available for the knight",
            "objective: work toward making d3 available for the rook"
          ),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("a path to compensation through initiative and continuing pressure"),
              compensationVectors = List(
                "Initiative (0.6)",
                "continuing pressure (0.7)",
                "waiting before winning the material back (0.4)",
                "path to compensation (0.5)"
              ),
              investedMaterial = Some(100)
            )
          )
        )
      )
    val dossier =
      Some(
        ActiveBranchDossier(
          dominantLens = "decision",
          chosenBranchLabel = "Rook-Lift Attack -> Focus on b2",
          whyChosen = Some("This move starts that route immediately."),
          opponentResource = Some("Attacking a fixed pawn"),
          routeCue = Some(
            ActiveBranchRouteCue(
              routeId = "route_1",
              ownerSide = "white",
              piece = "R",
              route = List("h1", "g1", "g4"),
              purpose = "kingside clamp",
              strategicFit = 0.8185714285714286,
              tacticalSafety = 1.0,
              surfaceConfidence = 0.8185714285714286,
              surfaceMode = RouteSurfaceMode.Toward
            )
          ),
          continuationFocus = Some("The move gives up material because the knight can still head for d3.")
        )
      )

    val note =
      ActiveStrategicCoachingBriefBuilder.buildStrictCompensationFallbackNote(
        strategyPack = compensationPack,
        dossier = dossier,
        routeRefs = Nil,
        moveRefs = Nil
      )
    val validation =
      note.map { text =>
        ActiveStrategicNoteValidator.validate(
          candidateText = text,
          baseNarrative = "White gives up material to keep the pressure going.",
          dossier = dossier,
          strategyPack = compensationPack,
          routeRefs = Nil,
          moveRefs = Nil,
          strategyReasons = Nil
        )
      }

    assert(note.exists(_.toLowerCase.contains("fixed central targets")), clue(note))
    assert(note.exists(_.toLowerCase.contains("anchored on")), clue(note))
    assert(note.exists(_.toLowerCase.contains("from there")), clue(note))
    assert(!note.exists(_.toLowerCase.contains("this move starts that route immediately")), clue(note))
    assert(validation.exists(_.isAccepted), clue(validation))
  }

  test("strict compensation fallback stays valid for move-ref-led blocker carriers with noisy why-now text") {
    val compensationPack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "B",
              from = "f2",
              target = "e6",
              idea = "contest the pawn on e6",
              evidence = List("enemy_occupied_endpoint", "target_pawn", "piece_activity")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_white_n_b3_d5",
              ownerSide = "white",
              piece = "N",
              from = "b3",
              targetSquare = "d5",
              readiness = DirectionalTargetReadiness.Contested,
              strategicReasons = List("supports prophylaxis against counterplay", "central foothold", "open-file pressure")
            ),
            StrategyDirectionalTarget(
              targetId = "target_white_r_d3_f5",
              ownerSide = "white",
              piece = "R",
              from = "d3",
              targetSquare = "f5",
              readiness = DirectionalTargetReadiness.Contested,
              strategicReasons = List("supports prophylaxis against counterplay", "central foothold", "line access")
            )
          ),
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_1",
              ownerSide = "white",
              kind = StrategicIdeaKind.LineOccupation,
              group = StrategicIdeaGroup.PieceAndLineManagement,
              readiness = StrategicIdeaReadiness.Ready,
              focusSquares = List("d3", "f5", "f6"),
              focusFiles = List("d", "f"),
              focusZone = Some("kingside"),
              beneficiaryPieces = List("R", "Q"),
              confidence = 0.98
            ),
            StrategyIdeaSignal(
              ideaId = "idea_2",
              ownerSide = "white",
              kind = StrategicIdeaKind.SpaceGainOrRestriction,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Build,
              focusZone = Some("center"),
              confidence = 0.8792
            )
          ),
          longTermFocus = List(
            "The move gives up material because the knight can still head for d5.",
            "compensation carrier: a path to compensation through initiative and continuing pressure, backed by Initiative (0.6) and continuing pressure (0.6)",
            "dominant idea: pressure on d3, f5, and f6",
            "secondary idea: space in the center",
            "objective: work toward making d5 available for the knight",
            "objective: work toward making f5 available for the rook"
          ),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("a path to compensation through initiative and continuing pressure"),
              compensationVectors = List(
                "Initiative (0.6)",
                "continuing pressure (0.6)",
                "waiting before winning the material back (0.4)",
                "path to compensation (0.5)"
              ),
              investedMaterial = Some(100)
            )
          )
        )
      )
    val dossier =
      Some(
        ActiveBranchDossier(
          dominantLens = "tactical",
          chosenBranchLabel = "Blunder via Rd4",
          whyChosen = Some("Focus on c5."),
          opponentResource = Some("Immediate counterplay"),
          continuationFocus = Some("The move gives up material because the knight can still head for d5.")
        )
      )

    val note =
      ActiveStrategicCoachingBriefBuilder.buildStrictCompensationFallbackNote(
        strategyPack = compensationPack,
        dossier = dossier,
        routeRefs = Nil,
        moveRefs = Nil
      )
    val validation =
      note.map { text =>
        ActiveStrategicNoteValidator.validate(
          candidateText = text,
          baseNarrative = "White is trying to justify the material deficit with activity.",
          dossier = dossier,
          strategyPack = compensationPack,
          routeRefs = Nil,
          moveRefs = Nil,
          strategyReasons = Nil
        )
      }

    assert(note.exists(_.toLowerCase.contains("fixed central targets")), clue(note))
    assert(note.exists(_.toLowerCase.contains("anchored on")), clue(note))
    assert(note.exists(_.toLowerCase.contains("from there")), clue(note))
    assert(!note.exists(_.toLowerCase.contains("focus on c5")), clue(note))
    assert(validation.exists(_.isAccepted), clue(validation))
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
        lowered.contains("pressure on") || lowered.contains("attacking chances")
      }
    )
    assert(
      brief.whyNow.exists(text =>
        text.toLowerCase.contains("initiative") ||
          text.toLowerCase.contains("attack")
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
