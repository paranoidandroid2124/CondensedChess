package lila.llm.analysis

import munit.FunSuite
import lila.llm.{ ActivePlanRef, GameChronicleMoment, NarrativeSignalDigest, StrategyIdeaSignal, StrategyPack, StrategyPieceMoveRef, StrategyPieceRoute, StrategySidePlan }

class StrategicBranchSelectorTest extends FunSuite:

  private def moment(
      ply: Int,
      momentType: String,
      moveClassification: Option[String] = None,
      transitionType: Option[String] = None,
      selectionKind: String = "key",
      selectionLabel: Option[String] = Some("Key Moment"),
      wpaSwing: Option[Double] = None,
      strategyPack: Option[StrategyPack] = None
  ): GameChronicleMoment =
    GameChronicleMoment(
      momentId = s"ply_$ply",
      ply = ply,
      moveNumber = (ply + 1) / 2,
      side = if ply % 2 == 1 then "white" else "black",
      moveClassification = moveClassification,
      momentType = momentType,
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1",
      narrative = "Narrative",
      selectionKind = selectionKind,
      selectionLabel = selectionLabel,
      concepts = Nil,
      variations = Nil,
      cpBefore = 0,
      cpAfter = 0,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = wpaSwing,
      strategicSalience = Some("High"),
      transitionType = transitionType,
      transitionConfidence = None,
      activePlan = None,
      topEngineMove = None,
      collapse = None,
      strategyPack = strategyPack
    )

  private def threadedMoment(
      ply: Int,
      subplanId: String = "minority_attack_fixation",
      theme: String = "Minority attack",
      structure: String = "Carlsbad",
      transitionType: Option[String] = None,
      decision: String = "keep building the same minority attack",
      selectionKind: String = "key"
  ): GameChronicleMoment =
    moment(
      ply = ply,
      momentType = if selectionKind == "thread_bridge" then "StrategicBridge" else "SustainedPressure",
      transitionType = transitionType,
      selectionKind = selectionKind,
      selectionLabel = Some(if selectionKind == "thread_bridge" then "Campaign Bridge" else "Key Moment"),
      wpaSwing = Some(6),
      strategyPack = Some(
        StrategyPack(
          sideToMove = "white",
          plans = List(StrategySidePlan("white", "long", theme)),
          pieceRoutes = List(StrategyPieceRoute("white", "R", "a1", List("a1", "b1", "b3"), "queenside pressure", 0.78)),
          longTermFocus = List("queenside pressure")
        )
      )
    ).copy(
      activePlan = Some(ActivePlanRef(theme, Some(subplanId), Some("Execution"), Some(0.8))),
      signalDigest = Some(
        NarrativeSignalDigest(
          structureProfile = Some(structure),
          structuralCue = Some(s"$structure structure"),
          deploymentPurpose = Some("queenside pressure"),
          decision = Some(decision)
        )
      )
    )

  private def truthContract(
      ownershipRole: TruthOwnershipRole,
      visibilityRole: TruthVisibilityRole,
      surfaceMode: TruthSurfaceMode,
      exemplarRole: Option[TruthExemplarRole] = None,
      truthClass: DecisiveTruthClass = DecisiveTruthClass.Best,
      truthPhase: Option[InvestmentTruthPhase] = None,
      chainKey: Option[String] = None,
      payoffAnchor: Option[String] = Some("open-file pressure")
  ): DecisiveTruthContract =
    val resolvedExemplarRole =
      exemplarRole.getOrElse:
        if ownershipRole == TruthOwnershipRole.CommitmentOwner then TruthExemplarRole.VerifiedExemplar
        else TruthExemplarRole.NonExemplar
    DecisiveTruthContract(
      playedMove = Some("d1d5"),
      verifiedBestMove = Some("d1d5"),
      truthClass = truthClass,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = DecisiveReasonFamily.InvestmentSacrifice,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = surfaceMode == TruthSurfaceMode.InvestmentExplain,
      truthPhase = truthPhase,
      ownershipRole = ownershipRole,
      visibilityRole = visibilityRole,
      surfaceMode = surfaceMode,
      exemplarRole = resolvedExemplarRole,
      surfacedMoveOwnsTruth =
        ownershipRole == TruthOwnershipRole.CommitmentOwner ||
          ownershipRole == TruthOwnershipRole.ConversionOwner ||
          ownershipRole == TruthOwnershipRole.BlunderOwner,
      verifiedPayoffAnchor = payoffAnchor,
      compensationProseAllowed = surfaceMode == TruthSurfaceMode.InvestmentExplain,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = chainKey
    )

  test("selector keeps only top 3 threads visible and caps active-note targets at 8") {
    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          threadedMoment(11),
          threadedMoment(19),
          threadedMoment(27, transitionType = Some("NaturalShift"), decision = "convert the first thread"),
          threadedMoment(61),
          threadedMoment(69),
          threadedMoment(77, transitionType = Some("NaturalShift"), decision = "convert the second thread"),
          threadedMoment(111),
          threadedMoment(119),
          threadedMoment(127, transitionType = Some("NaturalShift"), decision = "convert the third thread"),
          threadedMoment(161),
          threadedMoment(169),
          threadedMoment(177, transitionType = Some("NaturalShift"), decision = "convert the hidden fourth thread")
        )
      )

    assertEquals(selection.threads.size, 3)
    assertEquals(selection.selectedMoments.size, 9)
    assertEquals(selection.activeNoteMoments.size, 8)
    assert(!selection.selectedMoments.exists(_.ply >= 160))
  }

  test("selector fills spare visible slots with blunder, missed win, mate, then opening branch events") {
    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          threadedMoment(11),
          threadedMoment(19),
          threadedMoment(27, transitionType = Some("NaturalShift"), decision = "convert thread one"),
          threadedMoment(61),
          threadedMoment(69),
          threadedMoment(77, transitionType = Some("NaturalShift"), decision = "convert thread two"),
          threadedMoment(111),
          threadedMoment(119),
          threadedMoment(127, transitionType = Some("NaturalShift"), decision = "convert thread three"),
          moment(130, "AdvantageSwing", moveClassification = Some("Blunder")),
          moment(132, "AdvantageSwing", moveClassification = Some("MissedWin")),
          moment(134, "MatePivot"),
          moment(136, "OpeningNovelty", selectionKind = "opening", selectionLabel = Some("Opening Event")),
          moment(138, "OpeningIntro", selectionKind = "opening", selectionLabel = Some("Opening Event"))
        )
      )

    val plies = selection.selectedMoments.map(_.ply)
    assertEquals(selection.selectedMoments.size, 12)
    assert(plies.contains(130))
    assert(plies.contains(132))
    assert(plies.contains(134))
    assert(!plies.contains(136))
    assert(!plies.contains(138))
  }

  test("selector preserves investment pivots as visible decisive moments even without strict compensation subtype") {
    val threadedSeed = threadedMoment(11, decision = "seed thread one")
    val threadedBuild = threadedMoment(19, decision = "build thread one")
    val threadedConvert = threadedMoment(27, transitionType = Some("NaturalShift"), decision = "convert thread one")
    val investmentMoment =
      moment(
        ply = 31,
        momentType = "InvestmentPivot",
        moveClassification = Some("CompensatedInvestment"),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              longTermFocus = List("queenside pressure against fixed targets"),
              signalDigest =
                Some(
                  NarrativeSignalDigest(
                    compensation = Some("queenside pressure against fixed targets"),
                    compensationVectors = List("fixed targets"),
                    investedMaterial = Some(100)
                  )
                )
            )
          )
      )

    val selection =
      StrategicBranchSelector.buildSelection(List(threadedSeed, threadedBuild, threadedConvert, investmentMoment))

    assert(selection.selectedMoments.exists(_.ply == 31))
    assert(selection.activeNoteMoments.exists(_.ply == 31))
  }

  test("selector treats conversion pivots as core decisive events without turning them into investment pivots") {
    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          moment(18, "TensionPeak"),
          moment(36, "SustainedPressure", transitionType = Some("ExchangeConversion")),
          moment(40, "OpeningNovelty", selectionKind = "opening", selectionLabel = Some("Opening Event"))
        )
      )

    assert(selection.selectedMoments.exists(_.ply == 36))
  }

  test("commitment owner stays visible even when prose permission is withheld") {
    val commitment =
      moment(
        ply = 31,
        momentType = "InvestmentPivot",
        moveClassification = Some("WinningInvestment"),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              longTermFocus = List("open-file pressure")
            )
          )
      )

    val selection =
      StrategicBranchSelector.buildSelection(
        List(commitment),
        Map(
          31 -> truthContract(
            ownershipRole = TruthOwnershipRole.CommitmentOwner,
            visibilityRole = TruthVisibilityRole.PrimaryVisible,
            surfaceMode = TruthSurfaceMode.Neutral,
            truthClass = DecisiveTruthClass.WinningInvestment,
            truthPhase = Some(InvestmentTruthPhase.FirstInvestmentCommitment),
            chainKey = Some("white:open-file pressure")
          )
        )
      )

    assert(selection.selectedMoments.exists(_.ply == 31), clue(selection.selectedMoments))
  }

  test("provisional exemplar stays visible without compensation-positive prose") {
    val provisional =
      moment(
        ply = 31,
        momentType = "InvestmentPivot",
        moveClassification = Some("CompensatedInvestment"),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              longTermFocus = List("open-file pressure")
            )
          )
      )

    val selection =
      StrategicBranchSelector.buildSelection(
        List(provisional),
        Map(
          31 -> truthContract(
            ownershipRole = TruthOwnershipRole.NoneRole,
            visibilityRole = TruthVisibilityRole.SupportingVisible,
            surfaceMode = TruthSurfaceMode.Neutral,
            exemplarRole = Some(TruthExemplarRole.ProvisionalExemplar),
            truthClass = DecisiveTruthClass.CompensatedInvestment,
            truthPhase = Some(InvestmentTruthPhase.FirstInvestmentCommitment),
            chainKey = Some("white:open-file pressure")
          )
        )
      )

    assert(selection.selectedMoments.exists(_.ply == 31), clue(selection.selectedMoments))
    assert(selection.activeNoteMoments.exists(_.ply == 31), clue(selection.activeNoteMoments))
  }

  test("maintenance echo cannot outrank a nearby commitment owner in the same chain") {
    val commitment =
      moment(
        ply = 31,
        momentType = "InvestmentPivot",
        moveClassification = Some("WinningInvestment"),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              longTermFocus = List("open-file pressure")
            )
          )
      )
    val maintenance =
      moment(
        ply = 35,
        momentType = "SustainedPressure",
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              longTermFocus = List("pressure on e6")
            )
          )
      )

    val selection =
      StrategicBranchSelector.buildSelection(
        List(maintenance, commitment),
        Map(
          31 -> truthContract(
            ownershipRole = TruthOwnershipRole.CommitmentOwner,
            visibilityRole = TruthVisibilityRole.PrimaryVisible,
            surfaceMode = TruthSurfaceMode.InvestmentExplain,
            truthClass = DecisiveTruthClass.WinningInvestment,
            truthPhase = Some(InvestmentTruthPhase.FirstInvestmentCommitment),
            chainKey = Some("white:open-file pressure")
          ),
          35 -> truthContract(
            ownershipRole = TruthOwnershipRole.MaintenanceEcho,
            visibilityRole = TruthVisibilityRole.SupportingVisible,
            surfaceMode = TruthSurfaceMode.MaintenancePreserve,
            exemplarRole = Some(TruthExemplarRole.NonExemplar),
            truthPhase = Some(InvestmentTruthPhase.CompensationMaintenance),
            chainKey = Some("white:open-file pressure"),
            payoffAnchor = Some("pressure on e6")
          )
        )
      )

    assert(selection.selectedMoments.map(_.ply).contains(31), clue(selection.selectedMoments))
    assert(selection.selectedMoments.count(_.ply == 31) == 1, clue(selection.selectedMoments))
    assert(selection.selectedMoments.map(_.ply).contains(35), clue(selection.selectedMoments))
    assert(
      selection.selectedMoments.map(_.ply).indexOf(31) < selection.selectedMoments.map(_.ply).indexOf(35),
      clue(selection.selectedMoments)
    )
  }

  test("bridge moments are visible only when they occupy a representative stage slot") {
    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          threadedMoment(11, decision = "seed thread one"),
          threadedMoment(19, decision = "build via bridge", selectionKind = "thread_bridge"),
          threadedMoment(29, transitionType = Some("NaturalShift"), decision = "convert thread one"),
          threadedMoment(61, decision = "seed thread two"),
          threadedMoment(69, decision = "standard build thread two"),
          threadedMoment(73, decision = "non representative bridge", selectionKind = "thread_bridge"),
          threadedMoment(81, transitionType = Some("NaturalShift"), decision = "convert thread two")
        )
      )

    val plies = selection.selectedMoments.map(_.ply)
    assert(plies.contains(19))
    assert(!plies.contains(73))
  }

  test("selector falls back to core tactical and opening branch events when no threads exist") {
    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          moment(9, "OpeningIntro", selectionKind = "opening", selectionLabel = Some("Opening Event")),
          moment(14, "OpeningTheoryEnds", selectionKind = "opening", selectionLabel = Some("Opening Event")),
          moment(18, "AdvantageSwing", moveClassification = Some("Blunder")),
          moment(22, "MatePivot"),
          moment(30, "SustainedPressure")
        )
      )

    assertEquals(selection.threads, Nil)
    assertEquals(selection.activeNoteMoments, Nil)
    assertEquals(selection.selectedMoments.map(_.ply), List(14, 18, 22))
  }

  test("selector keeps strategic fallback moments visible when no threads or core events exist") {
    def fallbackMoment(ply: Int, ideaKind: String, focus: String) =
      moment(
        ply = ply,
        momentType = "SustainedPressure",
        strategyPack = Some(
          StrategyPack(
            sideToMove = "black",
            strategicIdeas = List(
              StrategyIdeaSignal(
                ideaId = s"idea_$ply",
                ownerSide = "black",
                kind = ideaKind,
                group = "slow_structural",
                readiness = "building",
                focusSquares = Nil,
                focusFiles = Nil,
                focusDiagonals = Nil,
                focusZone = Some("queenside"),
                beneficiaryPieces = List("R"),
                confidence = 0.74
              )
            ),
            longTermFocus = List(focus)
          )
        )
      ).copy(
        signalDigest = Some(
          NarrativeSignalDigest(
            dominantIdeaKind = Some(ideaKind),
            dominantIdeaFocus = Some("queenside pressure")
          )
        )
      )

    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          fallbackMoment(11, "line_occupation", "line pressure on the queenside"),
          fallbackMoment(19, "target_fixing", "fix the a-file and b-file targets"),
          fallbackMoment(27, "favorable_trade_or_transformation", "convert pressure into a lasting queenside bind")
        )
      )

    assertEquals(selection.threads, Nil)
    assertEquals(selection.activeNoteMoments.map(_.ply), List(11, 19, 27))
    assertEquals(selection.selectedMoments.map(_.ply), List(11, 19, 27))
  }

  test("selector fills spare active-note slots with visible strategic key moments outside thread representatives") {
    val threadedSeed = threadedMoment(11, decision = "seed the kingside campaign")
    val threadedBuild = threadedMoment(19, decision = "build the same route")
    val threadedConvert = threadedMoment(29, transitionType = Some("NaturalShift"), decision = "convert the thread")
    val visibleStrategicKey =
      moment(
        ply = 25,
        momentType = "TensionPeak",
        strategyPack = Some(
          StrategyPack(
            sideToMove = "white",
            strategicIdeas = List(
              StrategyIdeaSignal(
                ideaId = "idea_25",
                ownerSide = "white",
                kind = "line_occupation",
                group = "slow_structural",
                readiness = "ready",
                focusSquares = List("d5", "d7"),
                beneficiaryPieces = List("R"),
                confidence = 0.86
              )
            ),
            pieceRoutes = List(StrategyPieceRoute("white", "R", "d1", List("d1", "d5"), "open file occupation", 0.84)),
            longTermFocus = List("line pressure on d5")
          )
        )
      ).copy(
        signalDigest = Some(
          NarrativeSignalDigest(
            dominantIdeaKind = Some("line_occupation"),
            dominantIdeaFocus = Some("d5, d7"),
            compensation = Some("return vector through initiative and line pressure")
          )
        )
      )

    val selection =
      StrategicBranchSelector.buildSelection(List(threadedSeed, threadedBuild, visibleStrategicKey, threadedConvert))

    assert(selection.activeNoteMoments.map(_.ply).contains(25))
  }

  test("selector prioritizes quiet durable compensation over attack-led compensation in fallback notes") {
    val quietCompensation =
      moment(
        ply = 21,
        momentType = "SustainedPressure",
        strategyPack = Some(
          StrategyPack(
            sideToMove = "black",
            strategicIdeas = List(
              StrategyIdeaSignal(
                ideaId = "idea_quiet_comp",
                ownerSide = "black",
                kind = "target_fixing",
                group = "slow_structural",
                readiness = "building",
                focusSquares = List("b2"),
                focusFiles = List("b"),
                focusZone = Some("queenside"),
                beneficiaryPieces = List("R"),
                confidence = 0.83
              )
            ),
            pieceRoutes = List(StrategyPieceRoute("black", "R", "a8", List("a8", "b8", "b4"), "queenside pressure", 0.81)),
            longTermFocus = List("fix the queenside targets before recovering the pawn"),
            signalDigest = Some(
              NarrativeSignalDigest(
                compensation = Some("return vector through line pressure and delayed recovery"),
                compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)"),
                investedMaterial = Some(100)
              )
            )
          )
        )
      )
    val attackShell =
      moment(
        ply = 23,
        momentType = "TensionPeak",
        strategyPack = Some(
          StrategyPack(
            sideToMove = "white",
            strategicIdeas = List(
              StrategyIdeaSignal(
                ideaId = "idea_attack_comp",
                ownerSide = "white",
                kind = "king_attack_build_up",
                group = "interaction_and_transformation",
                readiness = "building",
                focusSquares = List("g7"),
                focusZone = Some("kingside"),
                beneficiaryPieces = List("Q", "R"),
                confidence = 0.86
              )
            ),
            longTermFocus = List("keep the initiative alive on the kingside"),
            signalDigest = Some(
              NarrativeSignalDigest(
                compensation = Some("return vector through initiative"),
                compensationVectors = List("Initiative (0.7)"),
                investedMaterial = Some(100)
              )
            )
          )
        )
      )

    val selection = StrategicBranchSelector.buildSelection(List(attackShell, quietCompensation))

    assertEquals(selection.activeNoteMoments.headOption.map(_.ply), Some(21))
    assert(selection.activeNoteMoments.map(_.ply).contains(23))
  }

  test("selector keeps strict compensation moments visible before spare core-event slots are filled") {
    val strictCompensation =
      moment(
        ply = 25,
        momentType = "SustainedPressure",
        strategyPack = Some(
          StrategyPack(
            sideToMove = "black",
            strategicIdeas = List(
              StrategyIdeaSignal(
                ideaId = "idea_strict_comp",
                ownerSide = "black",
                kind = "target_fixing",
                group = "slow_structural",
                readiness = "building",
                focusSquares = List("b2"),
                focusFiles = List("b"),
                focusZone = Some("queenside"),
                beneficiaryPieces = List("R"),
                confidence = 0.84
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
            longTermFocus = List("fix the queenside targets before recovering the pawn"),
            signalDigest = Some(
              NarrativeSignalDigest(
                compensation = Some("return vector through line pressure and delayed recovery"),
                compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
                investedMaterial = Some(100),
                dominantIdeaKind = Some("target_fixing"),
                dominantIdeaFocus = Some("b2")
              )
            )
          )
        )
      )

    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          moment(18, "AdvantageSwing", moveClassification = Some("Blunder")),
          moment(22, "MatePivot"),
          strictCompensation
        )
      )

    assert(selection.selectedMoments.map(_.ply).contains(25), clue(selection.selectedMoments))
    assert(selection.activeNoteMoments.map(_.ply).contains(25), clue(selection.activeNoteMoments))
  }
