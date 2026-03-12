package lila.llm.analysis

import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.{ PlanContinuity, PlanLifecyclePhase }

object BookmakerProseGoldenFixtures:

  final case class Fixture(
      id: String,
      title: String,
      motif: String,
      expectedLens: StrategicLens,
      ctx: NarrativeContext
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
        whyAbsentFromTopMultiPV = List("the direct kingside break only works once the rook lift is in place")
      )
    )

  val exchangeSacrifice: Fixture =
    Fixture(
      id = "exchange_sacrifice",
      title = "Exchange Sacrifice",
      motif = "exchange sac for long-term compensation",
      expectedLens = StrategicLens.Compensation,
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
        whyAbsentFromTopMultiPV = List("the calm recapture lets Black untangle and trade off the attack"),
        authorEvidence = List(
          QuestionEvidence(
            questionId = "q-exchange-sac",
            purpose = "free_tempo_branches",
            branches = List(
              EvidenceBranch("...Qe7", "Qe7 h5 Rh6", Some(68), None, Some(21), Some("probe-exchange-sac"))
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
        whyAbsentFromTopMultiPV = List("""the immediate "Qh5" thrust lets Black trade queens and kill the attack"""),
        authorEvidence = List(
          QuestionEvidence(
            questionId = "q-open-file",
            purpose = "latent_plan_refutation",
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
        whyAbsentFromTopMultiPV = List("""the sharper "e4" push gives Black tactical counterplay for little gain""")
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
