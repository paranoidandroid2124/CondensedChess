package lila.llm.analysis

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.{ PlanContinuity, PlanLifecyclePhase }

object BookmakerProseGoldenFixtures:

  final case class Fixture(
      id: String,
      title: String,
      motif: String,
      expectedLens: StrategicLens,
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack] = None
  )

  object PlannerFixtureExpectation:
    val Positive = "positive"
    val Negative = "negative"
    val Fallback = "fallback"

  final case class PlannerRuntimeFixture(
      id: String,
      title: String,
      expectation: String,
      questionKind: AuthorQuestionKind,
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack] = None,
      truthContract: Option[DecisiveTruthContract] = None,
      expectedPrimaryKind: Option[AuthorQuestionKind] = None,
      expectedClaimFragment: Option[String] = None,
      expectedFallbackClaim: Option[String] = None
  )

  private def baseContext(
      fen: String,
      playedMove: String,
      playedSan: String,
      primaryPlan: String,
      phaseLabel: String = "Middlegame"
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader(phaseLabel, "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 24,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary(primaryPlan, None, "NarrowChoice", "Maintain", "+0.20"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = primaryPlan,
            score = 0.82,
            evidence = List(s"supports $primaryPlan"),
            confidence = ConfidenceLevel.Heuristic
          )
        ),
        suppressed = Nil
      ),
      delta = None,
      phase = PhaseContext(phaseLabel, s"Representative $phaseLabel"),
      candidates = List(
        CandidateInfo(
          move = playedSan,
          annotation = "!",
          planAlignment = primaryPlan,
          downstreamTactic = None,
          tacticalAlert = None,
          practicalDifficulty = "complex",
          whyNot = None
        )
      ),
      renderMode = NarrativeRenderMode.Bookmaker
    )

  private def plan(
      id: String,
      name: String,
      score: Double = 0.84,
      theme: String = "unknown",
      evidence: List[String] = Nil,
      subplan: Option[String] = None
  ): PlanHypothesis =
    PlanHypothesis(
      planId = id,
      planName = name,
      rank = 1,
      score = score,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(score = score, label = "high", risk = "slow burn"),
      evidenceSources = evidence,
      themeL1 = theme,
      subplanId = subplan
    )

  private def question(
      id: String,
      kind: AuthorQuestionKind,
      priority: Int = 100,
      evidencePurposes: List[String] = Nil
  ): AuthorQuestion =
    AuthorQuestion(
      id = id,
      kind = kind,
      priority = priority,
      question = s"placeholder-$id",
      evidencePurposes = evidencePurposes
    )

  private def evidence(questionId: String, purpose: String, lines: List[String]): QuestionEvidence =
    QuestionEvidence(
      questionId = questionId,
      purpose = purpose,
      branches = lines.zipWithIndex.map { case (line, idx) =>
        EvidenceBranch(
          keyMove = s"line_${idx + 1}",
          line = line,
          evalCp = Some(40 - idx * 10)
        )
      }
    )

  private def threat(
      kind: String,
      lossIfIgnoredCp: Int,
      bestDefense: Option[String] = None,
      turnsToImpact: Int = 1
  ): ThreatRow =
    ThreatRow(
      kind = kind,
      side = "US",
      square = None,
      lossIfIgnoredCp = lossIfIgnoredCp,
      turnsToImpact = turnsToImpact,
      bestDefense = bestDefense,
      defenseCount = 1,
      insufficientData = false
    )

  private def truthContract(
      ownershipRole: TruthOwnershipRole,
      visibilityRole: TruthVisibilityRole,
      surfaceMode: TruthSurfaceMode,
      truthClass: DecisiveTruthClass = DecisiveTruthClass.Best,
      reasonFamily: DecisiveReasonFamily = DecisiveReasonFamily.QuietTechnicalMove
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("c3g3"),
      verifiedBestMove = Some("c3g3"),
      truthClass = truthClass,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = reasonFamily,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = ownershipRole,
      visibilityRole = visibilityRole,
      surfaceMode = surfaceMode,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  private def pressurePack(
      routePurpose: String = "kingside pressure",
      targetSquare: String = "g7",
      focusSquares: List[String] = List("g7", "h7"),
      targetReasons: List[String] = List("pressure on g7", "mating net")
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_attack_g7",
          ownerSide = "white",
          kind = StrategicIdeaKind.KingAttackBuildUp,
          group = StrategicIdeaGroup.InteractionAndTransformation,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = focusSquares,
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.91
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "R",
          from = "c3",
          route = List("c3", "g3"),
          purpose = routePurpose,
          strategicFit = 0.88,
          tacticalSafety = 0.8,
          surfaceConfidence = 0.84,
          surfaceMode = RouteSurfaceMode.Exact,
          evidence = List("probe-route", routePurpose)
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "white",
          piece = "Q",
          from = "d1",
          target = "h5",
          idea = "pressure on g7",
          evidence = List("probe-move")
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_g7",
          ownerSide = "white",
          piece = "Q",
          from = "d1",
          targetSquare = targetSquare,
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = targetReasons,
          evidence = List("probe-target", s"pressure on $targetSquare")
        )
      ),
      longTermFocus = List(s"keep pressure on $targetSquare"),
      signalDigest = Some(
        NarrativeSignalDigest(
          dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
          dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some(focusSquares.mkString(", "))
        )
      )
    )

  val rookPawnMarch: Fixture =
    Fixture(
      id = "rook_pawn_march",
      title = "Rook-Pawn March",
      motif = "rook-pawn march / h-pawn lever",
      expectedLens = StrategicLens.Structure,
      ctx = baseContext(
        fen = "r1b2rk1/pp3pp1/2n1pn1p/2pp4/3P3P/2P1PN2/PPQ1BPP1/R1B2RK1 w - - 0 16",
        playedMove = "h1h3",
        playedSan = "Rh3",
        primaryPlan = "Rook-Pawn March"
      ).copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = List(
              PieceActivityInfo(
                piece = "Rook",
                square = "h1",
                mobilityScore = 0.39,
                isTrapped = false,
                isBadBishop = false,
                keyRoutes = List("h3", "g3"),
                coordinationLinks = List("h5", "g4")
              )
            ),
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = List("rook_pawn_march", "h_pawn_lever"),
            structureProfile = Some(
              StructureProfileInfo(
                primary = "Kingside Clamp",
                confidence = 0.87,
                alternatives = Nil,
                centerState = "Locked",
                evidenceCodes = List("SPACE_RACE")
              )
            ),
            planAlignment = Some(
              PlanAlignmentInfo(
                score = 72,
                band = "Playable",
                matchedPlanIds = List("rook_pawn_march"),
                missingPlanIds = Nil,
                reasonCodes = List("SPACE_RACE", "TIMING"),
                narrativeIntent = Some("prepare the rook lift behind the h-pawn lever"),
                narrativeRisk = Some("the center can break open if the lever is mistimed")
              )
            )
          )
        ),
        mainStrategicPlans = List(
          plan(
            id = "rook_pawn_march",
            name = "Rook-Pawn March",
            theme = "rook_pawn_march",
            evidence = List("locked center", "kingside space")
          )
        ),
      )
    )

  val exchangeSacrifice: Fixture =
    Fixture(
      id = "exchange_sacrifice",
      title = "Exchange Sacrifice",
      motif = "exchange sac for long-term compensation",
      expectedLens = StrategicLens.Structure,
      ctx = baseContext(
        fen = "r2q1rk1/pb1nbppp/1pn1p3/2ppP3/3P4/2P1BN2/PPQ2PPP/R2R2K1 w - - 0 17",
        playedMove = "d1d6",
        playedSan = "Rxc6",
        primaryPlan = "Long-Term Kingside Pressure"
      ).copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = Some(
              CompensationInfo(
                investedMaterial = 180,
                returnVector = Map("Attack on King" -> 1.25, "Dark-Square Bind" -> 0.92),
                expiryPly = None,
                conversionPlan = "Mating Attack"
              )
            ),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = List("exchange_sacrifice", "long_term_compensation")
          )
        ),
        mainStrategicPlans = List(
          plan(
            id = "king_attack",
            name = "Kingside Attack",
            theme = "king_attack",
            evidence = List("dark-square bind", "open g-file")
          )
        ),
        authorEvidence = List(
          QuestionEvidence(
            questionId = "q-exchange-sac",
            purpose = "reply_multipv",
            branches = List(
              EvidenceBranch("...Qe7", "Qe7 h5 Rh6", Some(68), None, Some(21), Some("probe-exchange-sac"))
            )
          )
        )
      ),
      strategyPack = Some(
        StrategyPack(
          sideToMove = "black",
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_exchange_attack",
              ownerSide = "white",
              kind = StrategicIdeaKind.KingAttackBuildUp,
              group = StrategicIdeaGroup.InteractionAndTransformation,
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("g7", "h7"),
              focusZone = Some("kingside"),
              beneficiaryPieces = List("Q", "B"),
              confidence = 0.92
            )
          ),
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              route = List("d1", "g4", "h5"),
              purpose = "mate threats against the king",
              strategicFit = 0.9,
              tacticalSafety = 0.74,
              surfaceConfidence = 0.84,
              surfaceMode = RouteSurfaceMode.Exact,
              evidence = List("probe-exchange-sac", "queen route to h5")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_exchange_h7",
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              targetSquare = "h7",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("dark-square bind", "mating net pressure"),
              evidence = List("probe-exchange-sac", "pressure on h7")
            )
          ),
          longTermFocus = List("keep the initiative rather than recovering the material"),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("initiative against the king"),
              investedMaterial = Some(180),
              dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
              dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("g7, h7"),
              compensationVectors = List("Attack on King", "Dark-Square Bind")
            )
          )
        )
      )
    )

  val openFileFight: Fixture =
    Fixture(
      id = "open_file_fight",
      title = "Delayed Open-File Fight",
      motif = "delayed open-file fight with kingside attack",
      expectedLens = StrategicLens.Decision,
      ctx = baseContext(
        fen = "2r2rk1/pp3pp1/2pq1n1p/3p4/3P4/1QP1PNRP/P4PP1/2R3K1 w - - 0 22",
        playedMove = "c1c3",
        playedSan = "Rc3",
        primaryPlan = "Open-File Fight"
      ).copy(
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("g7")),
            logicSummary = "contest the c-file -> switch the rook to g3 -> pressure g7",
            delta = PVDelta(
              resolvedThreats = List("back-rank counterplay"),
              newOpportunities = List("g7"),
              planAdvancements = List("Met: rook lift"),
              concessions = List("queenside simplification")
            ),
            confidence = ConfidenceLevel.Probe
          )
        ),
        mainStrategicPlans = List(
          plan(
            id = "open_file_fight",
            name = "Open-File Fight",
            theme = "open_file",
            evidence = List("c-file control", "rook lift")
          )
        ),
        authorEvidence = List(
          QuestionEvidence(
            questionId = "q-open-file",
            purpose = "reply_multipv",
            branches = List(
              EvidenceBranch("...Rc8", "Rc8 Rc3 Rg6", Some(42), None, Some(23), Some("probe-open-file"))
            )
          )
        )
      )
    )

  val entrenchedPiece: Fixture =
    Fixture(
      id = "entrenched_piece",
      title = "Entrenched Piece",
      motif = "entrenched/passive piece exploitation",
      expectedLens = StrategicLens.Structure,
      ctx = baseContext(
        fen = "r2q1rk1/pp1nbpp1/2p1p2p/3pN3/3P4/2P1B3/PPQ2PPP/R4RK1 w - - 0 19",
        playedMove = "d2f1",
        playedSan = "Nf1",
        primaryPlan = "Punish the Entrenched Knight"
      ).copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = List(
              PieceActivityInfo(
                piece = "Knight",
                square = "d2",
                mobilityScore = 0.31,
                isTrapped = false,
                isBadBishop = false,
                keyRoutes = List("f1", "e3", "g4"),
                coordinationLinks = List("e3", "g4")
              )
            ),
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = List("entrenched_piece", "french_chain"),
            structureProfile = Some(
              StructureProfileInfo(
                primary = "French Chain",
                confidence = 0.79,
                alternatives = Nil,
                centerState = "Closed",
                evidenceCodes = List("ENTRENCHED")
              )
            ),
            planAlignment = Some(
              PlanAlignmentInfo(
                score = 67,
                band = "Playable",
                matchedPlanIds = List("entrenched_knight"),
                missingPlanIds = Nil,
                reasonCodes = List("ENTRENCHED", "TIMING"),
                narrativeIntent = Some("reroute the knight toward e3 and g4 to squeeze the e5-knight"),
                narrativeRisk = Some("Black gets c5 counterplay if the queenside opens first")
              )
            )
          )
        ),
        mainStrategicPlans = List(
          plan(
            id = "entrenched_knight",
            name = "Punish the Entrenched Knight",
            theme = "piece_restriction",
            evidence = List("e5 outpost fixed", "kingside space")
          )
        )
      )
    )

  val prophylacticCut: Fixture =
    Fixture(
      id = "prophylactic_cut",
      title = "Prophylactic Cut",
      motif = "prophylactic move that cuts counterplay",
      expectedLens = StrategicLens.Prophylaxis,
      ctx = baseContext(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        playedMove = "a2a3",
        playedSan = "a3",
        primaryPlan = "Kingside Expansion"
      ).copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "Queenside Counterplay",
                deniedSquares = List("b4"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = 0,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay")
              )
            ),
            conceptSummary = List("prophylaxis", "counterplay_cut")
          )
        ),
        mainStrategicPlans = List(
          plan(
            id = "kingside_expansion",
            name = "Kingside Expansion",
            theme = "kingside_space",
            evidence = List("counterplay denied")
          )
        )
      )
    )

  val practicalChoice: Fixture =
    Fixture(
      id = "practical_choice",
      title = "Practical Choice",
      motif = "practical choice vs nominal top line",
      expectedLens = StrategicLens.Practical,
      ctx = baseContext(
        fen = "r1bq1rk1/pp3ppp/2n1pn2/2bp4/3P4/2N1PN2/PPQ2PPP/R1B2RK1 w - - 0 12",
        playedMove = "a1d1",
        playedSan = "Rad1",
        primaryPlan = "Low-Risk Consolidation"
      ).copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = Some(
              PracticalInfo(
                engineScore = 14,
                practicalScore = 76.0,
                verdict = "Comfortable",
                biasFactors = List(
                  PracticalBiasInfo("Mobility", "multiple regrouping squares", 34.0),
                  PracticalBiasInfo("Forgiveness", "more than one safe continuation", 21.0)
                )
              )
            ),
            preventedPlans = Nil,
            conceptSummary = List("practical_choice", "low_risk")
          )
        ),
      )
    )

  val oppositeBishopsConversion: Fixture =
    Fixture(
      id = "opposite_bishops_conversion",
      title = "Opposite Bishops Conversion",
      motif = "opposite-coloured bishops conversion",
      expectedLens = StrategicLens.Structure,
      ctx = baseContext(
        fen = "8/5pk1/3b2p1/3P4/5P2/6P1/5BK1/8 w - - 0 45",
        playedMove = "g2f3",
        playedSan = "Bf3",
        primaryPlan = "Opposite Bishops Conversion",
        phaseLabel = "Endgame"
      ).copy(
        ply = 90,
        phase = PhaseContext("Endgame", "Opposite-colored bishops with a conversion window"),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = List(
              PositionalTagInfo("OppositeColorBishops", None, None, "Both"),
              PositionalTagInfo("ColorComplexWeakness", None, None, "Black", Some("dark squares: f6,h6"))
            ),
            compensation = None,
            endgameFeatures = Some(
              EndgameInfo(
                hasOpposition = false,
                isZugzwang = false,
                keySquaresControlled = List("f6", "g7"),
                theoreticalOutcomeHint = "Win",
                confidence = 0.88,
                primaryPattern = Some("OppositeColoredBishopsDraw"),
                transition = Some("dark-square invasion")
              )
            ),
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = List("Opposite-color bishops", "dark square bind"),
            planAlignment = Some(
              PlanAlignmentInfo(
                score = 82,
                band = "Playable",
                matchedPlanIds = List("opposite_bishops_conversion"),
                missingPlanIds = Nil,
                reasonCodes = List("TRANSFORMATION", "ENTRY_SQUARES"),
                narrativeIntent = Some("convert the opposite-colored bishops ending by penetrating on the dark squares"),
                narrativeRisk = Some("the drawing shell survives if the entry squares stay blocked")
              )
            )
          )
        ),
        mainStrategicPlans = List(
          plan(
            id = "opposite_bishops_conversion",
            name = "Opposite Bishops Conversion",
            theme = "advantage_transformation",
            evidence = List("subplan:opposite_bishops_conversion", "different color complexes"),
            subplan = Some("opposite_bishops_conversion")
          )
        ),
        planContinuity = Some(
          PlanContinuity(
            planName = "Opposite Bishops Conversion",
            planId = Some("opposite_bishops_conversion"),
            consecutivePlies = 3,
            startingPly = 86,
            phase = PlanLifecyclePhase.Fruition,
            commitmentScore = 0.83
          )
        )
      )
    )

  val all: List[Fixture] =
    List(
      rookPawnMarch,
      exchangeSacrifice,
      openFileFight,
      entrenchedPiece,
      prophylacticCut,
      practicalChoice
    )

  private val raceCtx =
    openFileFight.ctx.copy(
      summary = NarrativeSummary("Kingside Pressure", None, "NarrowChoice", "Maintain", "+0.20"),
      plans =
        PlanTable(
          top5 =
            List(
              PlanRow(
                rank = 1,
                name = "Kingside Pressure",
                score = 0.82,
                evidence = List("probe-backed"),
                confidence = ConfidenceLevel.Probe
              )
            ),
          suppressed = Nil
        ),
      threats = ThreatTable(toUs = List(threat("Counterplay", 220, Some("...Rc8"))), toThem = Nil),
      authorQuestions =
        List(question("q_race", AuthorQuestionKind.WhosePlanIsFaster, evidencePurposes = List("reply_multipv"))),
      authorEvidence =
        List(
          evidence(
            "q_race",
            "reply_multipv",
            List("23...Rc8 24.Rg3 Rc7 25.Qxg7+", "23...Rc8 24.Qh5 Rc7 25.Rg3")
          )
        ),
      opponentPlan = Some(PlanRow(1, "Queenside Counterplay", 0.72, List("...Rc8"))),
      mainStrategicPlans =
        List(
          plan(
            id = "kingside_pressure",
            name = "Kingside Pressure",
            theme = "kingside_attack",
            evidence = List("probe-backed")
          ).copy(executionSteps = List("Keep the pressure on g7."))
        ),
      strategicPlanExperiments =
        List(
          StrategicPlanExperiment(
            planId = "kingside_pressure",
            evidenceTier = "evidence_backed",
            bestReplyStable = true,
            futureSnapshotAligned = true
          )
        )
    )

  val plannerRuntimeFixtures: List[PlannerRuntimeFixture] =
    List(
      PlannerRuntimeFixture(
        id = "why_this_positive",
        title = "WhyThis surfaces move-owned purpose",
        expectation = PlannerFixtureExpectation.Positive,
        questionKind = AuthorQuestionKind.WhyThis,
        ctx =
          rookPawnMarch.ctx.copy(
            authorQuestions = List(question("q_why_this", AuthorQuestionKind.WhyThis, evidencePurposes = List("reply_multipv"))),
            authorEvidence =
              List(evidence("q_why_this", "reply_multipv", List("...Rc1+ 2.Bf1 Qxf1+", "...Rc1+ 2.Qxc1 Qxc1"))),
            meta = Some(
              MetaSignals(
                choiceType = ChoiceType.NarrowChoice,
                targets = Targets(Nil, Nil),
                planConcurrency = PlanConcurrency("Rook-Pawn March", None, "independent"),
                errorClass = Some(
                  ErrorClassification(
                    isTactical = true,
                    missedMotifs = List("Fork"),
                    errorSummary = "전술(320cp, Fork)"
                  )
                )
              )
            )
          ),
        truthContract =
          Some(
            truthContract(
              ownershipRole = TruthOwnershipRole.BlunderOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Blunder,
              reasonFamily = DecisiveReasonFamily.TacticalRefutation
            )
          ),
        expectedPrimaryKind = Some(AuthorQuestionKind.WhyThis),
        expectedClaimFragment = Some("blunder")
      ),
      PlannerRuntimeFixture(
        id = "why_this_negative",
        title = "WhyThis does not surface from shell-only support",
        expectation = PlannerFixtureExpectation.Negative,
        questionKind = AuthorQuestionKind.WhyThis,
        ctx =
          openFileFight.ctx.copy(
            semantic = None,
            decision = None,
            mainStrategicPlans = Nil,
            strategicPlanExperiments = Nil,
            latentPlans = Nil,
            whyAbsentFromTopMultiPV = Nil,
            authorQuestions = List(question("q_why_this_negative", AuthorQuestionKind.WhyThis))
          ),
        strategyPack = Some(pressurePack()),
        expectedPrimaryKind = None,
        expectedFallbackClaim = Some("This puts the rook on c3.")
      ),
      PlannerRuntimeFixture(
        id = "why_this_fallback",
        title = "WhyThis falls closed on line-only evidence",
        expectation = PlannerFixtureExpectation.Fallback,
        questionKind = AuthorQuestionKind.WhyThis,
        ctx =
          openFileFight.ctx.copy(
            semantic = None,
            decision = None,
            mainStrategicPlans = Nil,
            strategicPlanExperiments = Nil,
            authorQuestions =
              List(question("q_why_this_fallback", AuthorQuestionKind.WhyThis, evidencePurposes = List("reply_multipv"))),
            authorEvidence =
              List(evidence("q_why_this_fallback", "reply_multipv", List("14...Rc8 15.Re1 Qd8")))
          ),
        expectedPrimaryKind = None,
        expectedFallbackClaim = Some("This puts the rook on c3.")
      ),
      PlannerRuntimeFixture(
        id = "why_now_positive",
        title = "WhyNow surfaces timing pressure",
        expectation = PlannerFixtureExpectation.Positive,
        questionKind = AuthorQuestionKind.WhyNow,
        ctx =
          openFileFight.ctx.copy(
            threats = ThreatTable(toUs = List(threat("Mate", 900, Some("Qd8"))), toThem = Nil),
            authorQuestions = List(question("q_why_now", AuthorQuestionKind.WhyNow, evidencePurposes = List("reply_multipv"))),
            authorEvidence =
              List(evidence("q_why_now", "reply_multipv", List("14...Rc8 15.Re1 Qd8", "14...Rc8 15.a4 Qd8")))
          ),
        truthContract =
          Some(
            truthContract(
              ownershipRole = TruthOwnershipRole.BlunderOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Blunder,
              reasonFamily = DecisiveReasonFamily.TacticalRefutation
            )
          ),
        expectedPrimaryKind = Some(AuthorQuestionKind.WhyNow),
        expectedClaimFragment = Some("now")
      ),
      PlannerRuntimeFixture(
        id = "why_now_negative",
        title = "WhyNow demotes generic urgency",
        expectation = PlannerFixtureExpectation.Negative,
        questionKind = AuthorQuestionKind.WhyNow,
        ctx =
          openFileFight.ctx.copy(
            authorQuestions = List(question("q_why_now_negative", AuthorQuestionKind.WhyNow))
          ),
        expectedPrimaryKind = None,
        expectedFallbackClaim = Some("This puts the rook on c3.")
      ),
      PlannerRuntimeFixture(
        id = "why_now_fallback",
        title = "WhyNow falls back without timing or move owner",
        expectation = PlannerFixtureExpectation.Fallback,
        questionKind = AuthorQuestionKind.WhyNow,
        ctx =
          openFileFight.ctx.copy(
            semantic = None,
            decision = None,
            mainStrategicPlans = Nil,
            strategicPlanExperiments = Nil,
            authorQuestions = List(question("q_why_now_fallback", AuthorQuestionKind.WhyNow))
          ),
        expectedPrimaryKind = None,
        expectedFallbackClaim = Some("This puts the rook on c3.")
      ),
      PlannerRuntimeFixture(
        id = "what_changed_positive",
        title = "WhatChanged surfaces move-attributed change",
        expectation = PlannerFixtureExpectation.Positive,
        questionKind = AuthorQuestionKind.WhatChanged,
        ctx =
          openFileFight.ctx.copy(
            authorQuestions = List(question("q_changed", AuthorQuestionKind.WhatChanged, evidencePurposes = List("reply_multipv"))),
            authorEvidence =
              List(evidence("q_changed", "reply_multipv", List("14...Rc8 15.Re1 Qd8", "14...Rc8 15.a4 Qd8")))
          ),
        strategyPack = Some(pressurePack()),
        expectedPrimaryKind = Some(AuthorQuestionKind.WhatChanged),
        expectedClaimFragment = Some("back-rank counterplay")
      ),
      PlannerRuntimeFixture(
        id = "what_changed_negative",
        title = "WhatChanged rejects state-only structure summary",
        expectation = PlannerFixtureExpectation.Negative,
        questionKind = AuthorQuestionKind.WhatChanged,
        ctx =
          entrenchedPiece.ctx.copy(
            authorQuestions = List(question("q_changed_negative", AuthorQuestionKind.WhatChanged))
          ),
        expectedPrimaryKind = None,
        expectedFallbackClaim = Some("This puts the knight on f1.")
      ),
      PlannerRuntimeFixture(
        id = "what_changed_fallback",
        title = "WhatChanged falls back without move delta",
        expectation = PlannerFixtureExpectation.Fallback,
        questionKind = AuthorQuestionKind.WhatChanged,
        ctx =
          openFileFight.ctx.copy(
            semantic = None,
            decision = None,
            mainStrategicPlans = Nil,
            strategicPlanExperiments = Nil,
            authorQuestions = List(question("q_changed_fallback", AuthorQuestionKind.WhatChanged))
          ),
        expectedPrimaryKind = None,
        expectedFallbackClaim = Some("This puts the rook on c3.")
      ),
      PlannerRuntimeFixture(
        id = "what_must_be_stopped_positive",
        title = "WhatMustBeStopped surfaces defensive necessity",
        expectation = PlannerFixtureExpectation.Positive,
        questionKind = AuthorQuestionKind.WhatMustBeStopped,
        ctx =
          prophylacticCut.ctx.copy(
            authorQuestions =
              List(question("q_stop", AuthorQuestionKind.WhatMustBeStopped, evidencePurposes = List("reply_multipv"))),
            authorEvidence =
              List(evidence("q_stop", "reply_multipv", List("23...c5 24.a4 Rc8", "23...b5 24.axb4 Rc4")))
          ),
        expectedPrimaryKind = Some(AuthorQuestionKind.WhatMustBeStopped),
        expectedClaimFragment = Some("stop")
      ),
      PlannerRuntimeFixture(
        id = "what_must_be_stopped_negative",
        title = "WhatMustBeStopped demotes generic opponent plan text",
        expectation = PlannerFixtureExpectation.Negative,
        questionKind = AuthorQuestionKind.WhatMustBeStopped,
        ctx =
          openFileFight.ctx.copy(
            opponentPlan = Some(PlanRow(1, "Queenside Counterplay", 0.72, List("...Rc8"))),
            authorQuestions = List(question("q_stop_negative", AuthorQuestionKind.WhatMustBeStopped))
          ),
        strategyPack = Some(pressurePack()),
        expectedPrimaryKind = None,
        expectedFallbackClaim = Some("This puts the rook on c3.")
      ),
      PlannerRuntimeFixture(
        id = "what_must_be_stopped_fallback",
        title = "WhatMustBeStopped falls back without threat ownership",
        expectation = PlannerFixtureExpectation.Fallback,
        questionKind = AuthorQuestionKind.WhatMustBeStopped,
        ctx =
          openFileFight.ctx.copy(
            semantic = None,
            decision = None,
            mainStrategicPlans = Nil,
            strategicPlanExperiments = Nil,
            authorQuestions = List(question("q_stop_fallback", AuthorQuestionKind.WhatMustBeStopped))
          ),
        expectedPrimaryKind = None,
        expectedFallbackClaim = Some("This puts the rook on c3.")
      ),
      PlannerRuntimeFixture(
        id = "whose_plan_is_faster_positive",
        title = "WhosePlanIsFaster surfaces certified race framing",
        expectation = PlannerFixtureExpectation.Positive,
        questionKind = AuthorQuestionKind.WhosePlanIsFaster,
        ctx = raceCtx,
        strategyPack = Some(pressurePack()),
        expectedPrimaryKind = Some(AuthorQuestionKind.WhosePlanIsFaster),
        expectedClaimFragment = Some("queenside counterplay")
      ),
      PlannerRuntimeFixture(
        id = "whose_plan_is_faster_negative",
        title = "WhosePlanIsFaster demotes to stopping counterplay when only pressure survives",
        expectation = PlannerFixtureExpectation.Negative,
        questionKind = AuthorQuestionKind.WhosePlanIsFaster,
        ctx =
          prophylacticCut.ctx.copy(
            authorQuestions =
              List(question("q_race_negative", AuthorQuestionKind.WhosePlanIsFaster, evidencePurposes = List("reply_multipv"))),
            authorEvidence =
              List(evidence("q_race_negative", "reply_multipv", List("23...c5 24.a4 Rc8", "23...b5 24.axb4 Rc4"))),
            opponentPlan = Some(PlanRow(1, "Queenside Counterplay", 0.72, List("...c5 break")))
          ),
        strategyPack = Some(pressurePack()),
        expectedPrimaryKind = Some(AuthorQuestionKind.WhatMustBeStopped),
        expectedClaimFragment = Some("stop")
      ),
      PlannerRuntimeFixture(
        id = "whose_plan_is_faster_fallback",
        title = "WhosePlanIsFaster falls back when no race pair survives",
        expectation = PlannerFixtureExpectation.Fallback,
        questionKind = AuthorQuestionKind.WhosePlanIsFaster,
        ctx =
          openFileFight.ctx.copy(
            semantic = None,
            decision = None,
            mainStrategicPlans = Nil,
            strategicPlanExperiments = Nil,
            opponentPlan = None,
            authorQuestions = List(question("q_race_fallback", AuthorQuestionKind.WhosePlanIsFaster))
          ),
        expectedPrimaryKind = None,
        expectedFallbackClaim = Some("This puts the rook on c3.")
      )
    )
