package lila.commentary

import munit.FunSuite

import lila.commentary.analysis.PlayerFacingTruthModePolicy
import lila.commentary.analysis.StrategyPackSurface
import lila.commentary.model.authoring.AuthorQuestionKind

class CommentaryApiActiveParityTest extends FunSuite:

  private def minimalMoment(
      strategyPack: Option[StrategyPack] = None
  ): GameChronicleMoment =
    GameChronicleMoment(
      momentId = "ply_24",
      ply = 24,
      moveNumber = 12,
      side = "white",
      moveClassification = Some("Quiet"),
      momentType = "StrategicMoment",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      narrative = "A quiet move.",
      concepts = Nil,
      variations = Nil,
      cpBefore = 20,
      cpAfter = 24,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = None,
      strategicSalience = Some("Low"),
      transitionType = None,
      transitionConfidence = None,
      activePlan = None,
      topEngineMove = None,
      collapse = None,
      strategyPack = strategyPack
    )

  private val resolvedCompensationPack = Some(
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_comp",
          ownerSide = "black",
          kind = StrategicIdeaKind.TargetFixing,
          group = StrategicIdeaGroup.StructuralChange,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("b2", "c4"),
          focusFiles = List("b", "c"),
          focusZone = Some("queenside"),
          beneficiaryPieces = List("R", "Q"),
          confidence = 0.86
        )
      ),
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

  private val unresolvedCompensationPack = Some(
    StrategyPack(
      sideToMove = "black",
      signalDigest = Some(
        NarrativeSignalDigest(
          investedMaterial = Some(100)
        )
      )
    )
  )

  test("resolved compensation candidates are parity eligible for active-note attachment") {
    val surface = StrategyPackSurface.from(resolvedCompensationPack)

    assert(surface.compensationContractResolved, clue(surface))
    assert(surface.strictCompensationPosition, clue(surface))
    assert(CommentaryApi.activeCompensationParityEligible(resolvedCompensationPack))
    assert(CommentaryApi.activeCompensationParityEligible(surface))
  }

  test("unresolved compensation candidates stay outside the parity attach set") {
    val surface = StrategyPackSurface.from(unresolvedCompensationPack)

    assert(!surface.compensationContractResolved, clue(surface))
    assert(!CommentaryApi.activeCompensationParityEligible(unresolvedCompensationPack))
  }

  test("anchored compensation candidates stay eligible for active-note selection even when parity resolution is absent") {
    val subtype =
      StrategyPackSurface.CompensationSubtype(
        pressureTheater = "center",
        pressureMode = "target_fixing",
        recoveryPolicy = "delayed",
        stabilityClass = "durable_pressure"
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(
          StrategyIdeaSignal(
            ideaId = "idea_anchor",
            ownerSide = "black",
            kind = StrategicIdeaKind.TargetFixing,
            group = StrategicIdeaGroup.StructuralChange,
            readiness = StrategicIdeaReadiness.Ready,
            focusSquares = List("e4", "c4"),
            focusZone = Some("center"),
            confidence = 0.93
          )
        ),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = List(
          StrategyPieceMoveRef(
            ownerSide = "black",
            piece = "B",
            from = "g7",
            target = "e3",
            idea = "contest the pawn on e3",
            evidence = List("target_pawn")
          )
        ),
        topMoveRef = Some(
          StrategyPieceMoveRef(
            ownerSide = "black",
            piece = "B",
            from = "g7",
            target = "e3",
            idea = "contest the pawn on e3",
            evidence = List("target_pawn")
          )
        ),
        allDirectionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "target_e3",
            ownerSide = "black",
            piece = "B",
            from = "g7",
            targetSquare = "e3",
            readiness = DirectionalTargetReadiness.Contested,
            strategicReasons = List("fixed central target")
          )
        ),
        topDirectionalTarget = Some(
          StrategyDirectionalTarget(
            targetId = "target_e3",
            ownerSide = "black",
            piece = "B",
            from = "g7",
            targetSquare = "e3",
            readiness = DirectionalTargetReadiness.Contested,
            strategicReasons = List("fixed central target")
          )
        ),
        longTermFocus = Some("central targets tied down before winning the material back"),
        evidenceHints = Nil,
        compensationSummary = Some("a path to compensation through waiting before winning the material back"),
        compensationVectors = List("Waiting Before Winning the Material Back (0.5)", "Path to Compensation (0.5)"),
        investedMaterial = Some(100),
        compensationSubtype = Some(subtype),
        displayNormalization = None
      )

    assert(!surface.compensationContractResolved, clue(surface))
    assert(!CommentaryApi.activeCompensationParityEligible(surface), clue(surface))
    assert(CommentaryApi.activeCompensationNoteExpected(surface), clue(surface))
    assert(CommentaryApi.activeCompensationSelectionEligible(surface), clue(surface))
  }

  test("parity eligibility follows the resolved compensation contract even when display accessors prefer raw attack wording") {
    val subtype =
      StrategyPackSurface.CompensationSubtype(
        pressureTheater = "kingside",
        pressureMode = "line_occupation",
        recoveryPolicy = "immediate",
        stabilityClass = "durable_pressure"
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(
          StrategyIdeaSignal(
            ideaId = "idea_attack",
            ownerSide = "black",
            kind = StrategicIdeaKind.KingAttackBuildUp,
            group = StrategicIdeaGroup.InteractionAndTransformation,
            readiness = StrategicIdeaReadiness.Build,
            focusZone = Some("kingside"),
            confidence = 0.92
          )
        ),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = Some("keep the attack rolling on the kingside"),
        evidenceHints = Nil,
        compensationSummary = Some("initiative against the king"),
        compensationVectors = List("Initiative (0.7)", "Line Pressure (0.6)"),
        investedMaterial = Some(300),
        compensationSubtype = Some(subtype),
        displayNormalization = Some(
          StrategyPackSurface.DisplayNormalization(
            normalizedDominantIdeaText = Some("kingside file pressure"),
            normalizedExecutionText = Some("rook toward g-file to keep the lines active"),
            normalizedObjectiveText = Some("kingside file pressure before recovering the exchange"),
            normalizedLongTermFocusText = Some("keep the kingside files active before recovering the exchange"),
            normalizedCompensationLead = Some("kingside file pressure"),
            normalizedCompensationSubtype = Some(subtype),
            normalizationActive = true,
            normalizationConfidence = 7,
            preparationSubtype = Some(subtype),
            payoffSubtype = Some(subtype),
            selectedDisplaySubtype = Some(subtype),
            displaySubtypeSource = "path",
            payoffConfidence = 6,
            pathConfidence = 7
          )
        )
      )

    assertEquals(surface.displaySubtypeSource, "raw_fallback", clue(surface))
    assertEquals(StrategyPackSurface.resolvedDisplaySubtypeSource(surface), "path", clue(surface))
    assert(surface.compensationContractResolved, clue(surface))
    assert(surface.strictCompensationPosition, clue(surface))
    assert(CommentaryApi.activeCompensationParityEligible(surface))
  }

  test("parity eligibility widens to resolved transition-only compensation candidates") {
    val subtype =
      StrategyPackSurface.CompensationSubtype(
        pressureTheater = "center",
        pressureMode = "conversion_window",
        recoveryPolicy = "delayed",
        stabilityClass = "transition_only"
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = None,
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = Some("central conversion window before recovering the pawn"),
        evidenceHints = Nil,
        compensationSummary = Some("central conversion window"),
        compensationVectors = List("Conversion Window (0.6)", "Delayed Recovery (0.5)"),
        investedMaterial = Some(100),
        compensationSubtype = Some(subtype),
        displayNormalization = Some(
          StrategyPackSurface.DisplayNormalization(
            normalizedDominantIdeaText = Some("central conversion pressure"),
            normalizedExecutionText = Some("rook toward e4 to keep the center under control"),
            normalizedObjectiveText = Some("central conversion window before recovering the pawn"),
            normalizedLongTermFocusText = Some("central conversion window before recovering the pawn"),
            normalizedCompensationLead = Some("central conversion pressure"),
            normalizedCompensationSubtype = Some(subtype),
            normalizationActive = true,
            normalizationConfidence = 8,
            preparationSubtype = Some(subtype),
            payoffSubtype = Some(subtype),
            selectedDisplaySubtype = Some(subtype),
            displaySubtypeSource = "path",
            payoffConfidence = 7,
            pathConfidence = 8
          )
        )
      )

    assert(surface.compensationPosition, clue(surface))
    assert(surface.compensationContractResolved, clue(surface))
    assert(!surface.strictCompensationPosition, clue(surface))
    assert(CommentaryApi.activeCompensationParityEligible(surface))
  }

  test("provider split only gates optional polish and never canonical deterministic attachment") {
    val surface = StrategyPackSurface.from(resolvedCompensationPack)

    assert(CommentaryApi.activeCompensationParityEligible(surface), clue(surface))
    assert(
      CommentaryApi.activeNoteOptionalPolishEligible(
        activeEligible = true,
        allowAiPolish = true,
        providerResolved = "gemini",
        deterministicDraftPresent = true
      )
    )
    assert(
      !CommentaryApi.activeNoteOptionalPolishEligible(
        activeEligible = true,
        allowAiPolish = true,
        providerResolved = "none",
        deterministicDraftPresent = true
      )
    )
    assert(
      !CommentaryApi.activeNoteOptionalPolishEligible(
        activeEligible = true,
        allowAiPolish = true,
        providerResolved = "gemini",
        deterministicDraftPresent = false
      )
    )
  }

  test("only WhyNow may attach an active note without visible delta support") {
    assert(CommentaryApi.activePlannerPrimaryCanAttachWithoutVisibleSupport(AuthorQuestionKind.WhyNow))
    assert(!CommentaryApi.activePlannerPrimaryCanAttachWithoutVisibleSupport(AuthorQuestionKind.WhyThis))
    assert(!CommentaryApi.activePlannerPrimaryCanAttachWithoutVisibleSupport(AuthorQuestionKind.WhatChanged))
    assert(!CommentaryApi.activePlannerPrimaryCanAttachWithoutVisibleSupport(AuthorQuestionKind.WhatMustBeStopped))
    assert(!CommentaryApi.activePlannerPrimaryCanAttachWithoutVisibleSupport(AuthorQuestionKind.WhosePlanIsFaster))
  }

  test("planner-approved WhyNow remains active-note eligible even without strategic-branch selection") {
    assert(CommentaryApi.activePlannerSelectionEligible(strategicBranchSelected = true, primaryKind = None))
    assert(CommentaryApi.activePlannerSelectionEligible(strategicBranchSelected = false, primaryKind = Some(AuthorQuestionKind.WhyNow)))
    assert(!CommentaryApi.activePlannerSelectionEligible(strategicBranchSelected = false, primaryKind = Some(AuthorQuestionKind.WhyThis)))
    assert(!CommentaryApi.activePlannerSelectionEligible(strategicBranchSelected = false, primaryKind = Some(AuthorQuestionKind.WhatChanged)))
  }

  test("planner-approved WhyNow may bypass the minimal truth-mode gate but other kinds may not") {
    val moment = minimalMoment()

    assertEquals(PlayerFacingTruthModePolicy.classify(moment).toString, "Minimal")
    assert(!PlayerFacingTruthModePolicy.allowsActiveNote(moment))
    assert(CommentaryApi.activePlannerPolicyEligible(moment, Some(AuthorQuestionKind.WhyNow)))
    assert(!CommentaryApi.activePlannerPolicyEligible(moment, Some(AuthorQuestionKind.WhyThis)))
  }
