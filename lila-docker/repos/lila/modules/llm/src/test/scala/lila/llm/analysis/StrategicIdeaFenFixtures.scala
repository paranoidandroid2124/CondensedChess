package lila.llm.analysis

import lila.llm.*
import lila.llm.model.structure.StructureId

private[llm] object StrategicIdeaFenFixtures:

  enum ProducerCheck:
    case StructureProfileIs(id: StructureId)
    case BreakCandidate
    case SpaceRestriction
    case WeakSquareOrWeakComplex
    case LineAccess
    case OutpostAnchor
    case MinorPieceImbalance
    case ProphylaxisSignal
    case AttackBuildUpSignal
    case SimplifyBias
    case CounterBreakWatch

  final case class Fixture(
      id: String,
      label: String,
      fen: String,
      expectedDominant: String,
      boundaryAgainst: Option[String] = None,
      forbiddenKinds: List[String] = Nil,
      producerChecks: List[ProducerCheck] = Nil,
      phase: String = "middlegame",
      sourceSeedId: Option[String] = None,
      stockfishScoreCp: Option[Int] = None,
      stockfishMaxAbsCp: Option[Int] = None,
      requireMaterialParity: Boolean = false
  )

  val canonical: List[Fixture] = List(
    Fixture(
      id = "K01",
      label = "French f6 break",
      fen = "r2qk2r/pp1bbppp/2n1p3/3pPn2/NP1P4/P2B1N2/1B3PPP/R2QK2R b KQkq - 8 12",
      expectedDominant = StrategicIdeaKind.PawnBreak,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.FrenchAdvanceChain),
        ProducerCheck.BreakCandidate
      )
    ),
    Fixture(
      id = "K02",
      label = "Maroczy bind core",
      fen = "r2q1rk1/pp1bppbp/2np1np1/8/2P1P3/2N1B3/PPN1BPPP/R2Q1RK1 w - - 3 10",
      expectedDominant = StrategicIdeaKind.SpaceGainOrRestriction,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.MaroczyBind),
        ProducerCheck.SpaceRestriction
      )
    ),
    Fixture(
      id = "K03",
      label = "Carlsbad fixed targets",
      fen = "r1bqrnk1/1p2bppp/2p2n2/p2p2B1/3P4/P1NBP3/1PQ1NPPP/1R3RK1 b - - 0 12",
      expectedDominant = StrategicIdeaKind.TargetFixing,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.Carlsbad),
        ProducerCheck.WeakSquareOrWeakComplex
      )
    ),
    Fixture(
      id = "K04",
      label = "QGD open c-file",
      fen = "r1b2rk1/pp1nqppp/2p1p3/3n4/2BP4/2N1PN2/PP3PPP/2RQ1RK1 w - - 1 11",
      expectedDominant = StrategicIdeaKind.LineOccupation,
      producerChecks = List(
        ProducerCheck.LineAccess
      )
    ),
    Fixture(
      id = "K05",
      label = "Sveshnikov d5 outpost",
      fen = "r1bq1rk1/5ppp/p1np1b2/1p1Np3/4P3/2P5/PPN2PPP/R2QKB1R w KQ - 2 12",
      expectedDominant = StrategicIdeaKind.OutpostCreationOrOccupation,
      producerChecks = List(
        ProducerCheck.OutpostAnchor
      )
    ),
    Fixture(
      id = "K06",
      label = "Closed French knight vs bishop",
      fen = "r2q1rk1/pp1bbppp/2n1p3/3pPn2/3P4/5N2/PPN1BPPP/R1BQ1RK1 w - - 3 11",
      expectedDominant = StrategicIdeaKind.MinorPieceImbalanceExploitation,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.FrenchAdvanceChain),
        ProducerCheck.MinorPieceImbalance
      )
    ),
    Fixture(
      id = "K07",
      label = "Ruy Lopez before h3",
      fen = "r1bq1rk1/2p1bppp/p1np1n2/1p2p3/4P3/1BP2N2/PP1P1PPP/RNBQR1K1 w - - 1 9",
      expectedDominant = StrategicIdeaKind.Prophylaxis,
      producerChecks = List(
        ProducerCheck.ProphylaxisSignal
      ),
      phase = "opening"
    ),
    Fixture(
      id = "K08",
      label = "Dragon attack shell",
      fen = "2rq1rk1/pp1bppb1/3p1np1/4n2p/3NP2P/1BN1BP2/PPPQ2P1/2KR3R w - - 0 13",
      expectedDominant = StrategicIdeaKind.KingAttackBuildUp,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.FianchettoShell),
        ProducerCheck.AttackBuildUpSignal
      )
    ),
    Fixture(
      id = "K09",
      label = "IQP trade-down window",
      fen = "r1bqr1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
      expectedDominant = StrategicIdeaKind.FavorableTradeOrTransformation,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.IQPBlack),
        ProducerCheck.SimplifyBias
      )
    ),
    Fixture(
      id = "K10",
      label = "Hedgehog containment",
      fen = "r2qk2r/1b1nbppp/pp1ppn2/8/2PQ4/BPN2NP1/P3PPBP/R2R2K1 w kq - 2 11",
      expectedDominant = StrategicIdeaKind.CounterplaySuppression,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.Hedgehog),
        ProducerCheck.CounterBreakWatch
      )
    ),
    Fixture(
      id = "B11",
      label = "KID fianchetto clamp setup",
      fen = "r1bq1rk1/ppp1npbp/3p1np1/3Pp3/2P1P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 1 9",
      expectedDominant = StrategicIdeaKind.Prophylaxis,
      boundaryAgainst = Some(StrategicIdeaKind.CounterplaySuppression),
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.FianchettoShell),
        ProducerCheck.ProphylaxisSignal
      )
    ),
    Fixture(
      id = "B12",
      label = "Maroczy stronger bind stays space-led",
      fen = "r2q1rk1/pp2ppbp/2bp1np1/8/2PBP3/2N2P2/PP2B1PP/2RQ1RK1 w - - 0 12",
      expectedDominant = StrategicIdeaKind.SpaceGainOrRestriction,
      boundaryAgainst = Some(StrategicIdeaKind.CounterplaySuppression),
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.MaroczyBind),
        ProducerCheck.SpaceRestriction
      )
    ),
    Fixture(
      id = "B13",
      label = "Knight on e5 stays outpost-led",
      fen = "r2q1rk1/pb1nbpp1/1p3n1p/2ppN3/3P3B/2NBP3/PP3PPP/2RQ1RK1 w - - 1 12",
      expectedDominant = StrategicIdeaKind.OutpostCreationOrOccupation,
      boundaryAgainst = Some(StrategicIdeaKind.LineOccupation),
      producerChecks = List(
        ProducerCheck.LineAccess,
        ProducerCheck.OutpostAnchor
      )
    ),
    Fixture(
      id = "B14",
      label = "Open-file pressure with kingside hints",
      fen = "r2q1rk1/pb1nbpp1/1p3n1p/2pp4/3P3B/2NBPN2/PP2QPPP/2R2RK1 w - - 1 12",
      expectedDominant = StrategicIdeaKind.LineOccupation,
      boundaryAgainst = Some(StrategicIdeaKind.KingAttackBuildUp),
      producerChecks = List(
        ProducerCheck.LineAccess,
        ProducerCheck.AttackBuildUpSignal
      )
    ),
    Fixture(
      id = "B15",
      label = "Carlsbad fixed-chain pressure",
      fen = "r1bqr1k1/pp2bppp/2p1nn2/3p4/3P3B/2NBPP2/PPQ1N1PP/R4RK1 w - - 2 12",
      expectedDominant = StrategicIdeaKind.TargetFixing,
      boundaryAgainst = Some(StrategicIdeaKind.SpaceGainOrRestriction),
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.Carlsbad),
        ProducerCheck.WeakSquareOrWeakComplex
      )
    ),
    Fixture(
      id = "B16",
      label = "Carlsbad pressure resists trade overclassification",
      fen = "r1b1rnk1/pp2qppp/2p5/3p3n/3P4/2NBPP2/PPQ1N1PP/R4RK1 w - - 0 13",
      expectedDominant = StrategicIdeaKind.TargetFixing,
      forbiddenKinds = List(StrategicIdeaKind.FavorableTradeOrTransformation),
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.Carlsbad),
        ProducerCheck.WeakSquareOrWeakComplex
      )
    ),
    Fixture(
      id = "B17",
      label = "Neutral French exchange shell",
      fen = "rnbqr1k1/pp3ppp/2pb1n2/3p4/3P1B2/2NB4/PPP1NPPP/R2Q1RK1 w - - 2 9",
      expectedDominant = StrategicIdeaKind.SpaceGainOrRestriction,
      forbiddenKinds = List(StrategicIdeaKind.FavorableTradeOrTransformation),
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.IQPWhite),
        ProducerCheck.SpaceRestriction
      )
    ),
    Fixture(
      id = "B18",
      label = "French central files stay line-led",
      fen = "r1bq1rk1/1p1n1ppp/2n1p3/p1bpP3/8/2PB1N2/PP1N1PPP/R1BQR1K1 w - - 2 11",
      expectedDominant = StrategicIdeaKind.LineOccupation,
      boundaryAgainst = Some(StrategicIdeaKind.OutpostCreationOrOccupation),
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.FrenchAdvanceChain),
        ProducerCheck.LineAccess
      )
    ),
    Fixture(
      id = "K11",
      label = "Slav main line tension",
      fen = "rnbqk2r/pp2bppp/4pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1 w kq - 0 8",
      expectedDominant = StrategicIdeaKind.PawnBreak,
      producerChecks = List(
        ProducerCheck.BreakCandidate,
        ProducerCheck.SpaceRestriction
      )
    ),
    Fixture(
      id = "K12",
      label = "Meran semi-slav complex",
      fen = "r1bqk2r/p2nbppp/1pn1p3/2ppP3/3P4/2PB1N2/PP1N1PPP/R1BQ1RK1 w kq - 1 10",
      expectedDominant = StrategicIdeaKind.MinorPieceImbalanceExploitation,
      producerChecks = List(
        ProducerCheck.MinorPieceImbalance
      )
    ),
    Fixture(
      id = "K13",
      label = "Moscow semi-slav lines",
      fen = "r1bqkb1r/pp3ppp/2n1p3/2ppP3/3P4/2P2N2/P1P2PPP/R1BQKB1R w KQkq - 1 8",
      expectedDominant = StrategicIdeaKind.LineOccupation,
      producerChecks = List(
        ProducerCheck.LineAccess
      )
    ),
    Fixture(
      id = "B19",
      label = "Ruy Lopez Chigorin prophylaxis",
      fen = "r1bq1rk1/2p1bppp/p1np1n2/1p2p3/4P3/1BP2N2/PP1P1PPP/RNBQR1K1 w - - 0 10",
      expectedDominant = StrategicIdeaKind.Prophylaxis,
      producerChecks = List(
        ProducerCheck.ProphylaxisSignal,
        ProducerCheck.LineAccess
      )
    ),
    Fixture(
      id = "B20",
      label = "Ruy Lopez Breyer prophylaxis",
      fen = "r1bq1rk1/1pp1bppp/p1np1n2/4p3/B3P3/2P2N2/PP1P1PPP/RNBQR1K1 w - - 0 10",
      expectedDominant = StrategicIdeaKind.Prophylaxis,
      producerChecks = List(
        ProducerCheck.ProphylaxisSignal,
        ProducerCheck.LineAccess
      )
    ),
    Fixture(
      id = "K14",
      label = "Berlin Endgame targets",
      fen = "r1bk1b1r/ppp2ppp/2p5/4P3/6n1/8/PPP2PPP/RNB1KB1R w KQ - 1 8",
      expectedDominant = StrategicIdeaKind.TargetFixing,
      phase = "endgame",
      producerChecks = List(
        ProducerCheck.WeakSquareOrWeakComplex
      )
    ),
    Fixture(
      id = "K15",
      label = "Grunfeld Exchange Mainline",
      fen = "r1bq1rk1/pp2ppbp/2np1np1/2p5/2PPP3/2N1BP2/PP2N1PP/R2QKB1R w KQ - 0 9",
      expectedDominant = StrategicIdeaKind.PawnBreak,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.FluidCenter)
      )
    ),
    Fixture(
      id = "B21",
      label = "Modern Benoni target fixing",
      fen = "rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 0 9",
      expectedDominant = StrategicIdeaKind.TargetFixing,
      producerChecks = List(
        ProducerCheck.WeakSquareOrWeakComplex
      )
    ),
    Fixture(
      id = "K16",
      label = "KID Mar del Plata clamp",
      fen = "r1bq1rk1/ppp1npbp/3p1np1/3Pp3/2P1P3/2N1BN2/PP2BPPP/R2Q1RK1 w - - 3 10",
      expectedDominant = StrategicIdeaKind.Prophylaxis,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.FianchettoShell),
        ProducerCheck.SpaceRestriction
      )
    ),
    Fixture(
      id = "K17",
      label = "Open Catalan tension",
      fen = "rn1q1rk1/pb2bppp/1p2pn2/2p5/2pP4/2N2NP1/PP2PPBP/R1BQR1K1 w - - 0 10",
      expectedDominant = StrategicIdeaKind.PawnBreak,
      producerChecks = List(
        ProducerCheck.StructureProfileIs(StructureId.IQPWhite),
        ProducerCheck.BreakCandidate
      )
    ),
    Fixture(
      id = "K18",
      label = "Rook Endgame Files",
      fen = "8/5pk1/1p2p1p1/p1r4p/P4P1P/1P1R2P1/5K2/8 w - - 1 40",
      expectedDominant = StrategicIdeaKind.KingAttackBuildUp,
      phase = "endgame",
      producerChecks = List(
        ProducerCheck.LineAccess
      )
    ),
    Fixture(
      id = "K19",
      label = "Endgame Material Up",
      fen = "8/1k1r4/4p3/1P1pP2p/R2P3P/3K2P1/8/8 w - - 1 45",
      expectedDominant = StrategicIdeaKind.KingAttackBuildUp,
      phase = "endgame",
      producerChecks = Nil
    ),
    // --- Phase 8: Sharp Gambit & Dynamic Position Evaluation ---
    Fixture(
      id = "G01",
      label = "Danish Gambit (Accepted) - Attack build-up",
      fen = "r1bqk1nr/pppp1ppp/2n5/2b1p3/2BPP3/2P2N2/PP3PPP/RNBQK2R b KQkq - 0 5",
      expectedDominant = StrategicIdeaKind.KingAttackBuildUp,
      producerChecks = List(
        ProducerCheck.AttackBuildUpSignal,
        ProducerCheck.MinorPieceImbalance
      )
    ),
    Fixture(
      id = "G02",
      label = "King's Gambit (Muzio) - Line occupation",
      fen = "rnbq1rk1/pppp1ppp/5n2/2b1p3/2BPP3/2P2N2/PP3PPP/RNBQ1RK1 b - - 0 7",
      expectedDominant = StrategicIdeaKind.LineOccupation,
      producerChecks = List(
        ProducerCheck.LineAccess
      )
    ),
    Fixture(
      id = "G03",
      label = "Evans Gambit - Target fixing",
      fen = "r1bqk1nr/pppp1ppp/2n5/1B2p3/1b1PP3/5N2/PPP2PPP/RNBQK2R b KQkq - 0 5",
      expectedDominant = StrategicIdeaKind.TargetFixing,
      producerChecks = List(
        ProducerCheck.WeakSquareOrWeakComplex
      )
    ),
    Fixture(
      id = "G04",
      label = "Smith-Morra Gambit - Space gain",
      fen = "r1bqkbnr/pp1ppppp/2n5/8/2PP4/5N2/PP3PPP/RNBQKB1R b KQkq - 0 4",
      expectedDominant = StrategicIdeaKind.SpaceGainOrRestriction,
      producerChecks = List(
        ProducerCheck.SpaceRestriction,
        ProducerCheck.LineAccess
      )
    ),
    Fixture(
      id = "G05",
      label = "Benko Gambit - Long-term target fixing",
      fen = "rn1qkb1r/4pp1p/3p1np1/1p1P4/8/5N2/PP2PPPP/R1BQKB1R w KQkq - 0 9",
      expectedDominant = StrategicIdeaKind.TargetFixing,
      producerChecks = List(
        ProducerCheck.WeakSquareOrWeakComplex,
        ProducerCheck.LineAccess
      )
    )
  )

  private val canonicalById: Map[String, Fixture] =
    canonical.map(fixture => fixture.id -> fixture).toMap

  private def followUp(
      id: String,
      seedId: String,
      label: String,
      fen: String,
      stockfishScoreCp: Int,
      stockfishMaxAbsCp: Int = 120,
      requireMaterialParity: Boolean = true
  ): Fixture =
    val seed = canonicalById.getOrElse(seedId, sys.error(s"unknown seed fixture: $seedId"))
    seed.copy(
      id = id,
      label = label,
      fen = fen,
      sourceSeedId = Some(seedId),
      stockfishScoreCp = Some(stockfishScoreCp),
      stockfishMaxAbsCp = Some(stockfishMaxAbsCp),
      requireMaterialParity = requireMaterialParity
    )

  val stockfishBalancedSupplemental: List[Fixture] = List(
    followUp(
      id = "K01A",
      seedId = "K01",
      label = "French f6 break continuation 1",
      fen = "r2qk2r/p2bbppp/1pn1p3/3pPn2/NP1P4/P2B1N2/1B3PPP/R2Q1RK1 b kq - 1 13",
      stockfishScoreCp = 21
    ),
    followUp(
      id = "K01B",
      seedId = "K01",
      label = "French f6 break continuation 2",
      fen = "r2q1rk1/pp1bbppp/2n1p3/2NpPn2/1P1P4/P2B1N2/1B3PPP/R2QK2R b KQ - 10 13",
      stockfishScoreCp = 84
    ),
    followUp(
      id = "K01C",
      seedId = "K01",
      label = "French f6 break continuation 3",
      fen = "2rqk2r/pp1bbppp/2n1p3/3pPn2/NP1P2P1/P2B1N2/1B3P1P/R2QK2R b KQk - 0 13",
      stockfishScoreCp = 63
    ),
    followUp(
      id = "K01D",
      seedId = "K01",
      label = "French f6 break continuation 4",
      fen = "r2qk2r/1p1bbppp/p1n1p3/3pPn2/NP1P2P1/P2B1N2/1B3P1P/R2QK2R b KQkq - 0 13",
      stockfishScoreCp = 102
    ),
    followUp(
      id = "K02B",
      seedId = "K02",
      label = "Maroczy bind core continuation 4",
      fen = "r2q1rk1/pp1bppb1/2np1npp/8/2P1P3/2N1BP2/PPN1B1PP/R2Q1RK1 w - - 0 11",
      stockfishScoreCp = 118
    ),
    followUp(
      id = "K03A",
      seedId = "K03",
      label = "Carlsbad fixed targets continuation 1",
      fen = "r1bqrnk1/4bppp/2p2n2/pp1p2B1/3P4/P1NBP3/1PQ1NPPP/3R1RK1 b - - 1 13",
      stockfishScoreCp = -13
    ),
    followUp(
      id = "K03B",
      seedId = "K03",
      label = "Carlsbad fixed targets continuation 2",
      fen = "r1bqrnk1/1p3ppp/2pb1n2/p2p2B1/3P4/P1NBP2P/1PQ1NPP1/1R3RK1 b - - 0 13",
      stockfishScoreCp = 36
    ),
    followUp(
      id = "K03C",
      seedId = "K03",
      label = "Carlsbad fixed targets continuation 3",
      fen = "r1bqrnk1/1p2bpp1/2p2n1p/p2p4/3P1B2/P1NBP3/1PQ1NPPP/1R3RK1 b - - 1 13",
      stockfishScoreCp = 28
    ),
    followUp(
      id = "K03D",
      seedId = "K03",
      label = "Carlsbad fixed targets continuation 4",
      fen = "r2qrnk1/1p2bppp/2p1bn2/p2p2B1/3P4/P1NBP3/1PQ1NPPP/3R1RK1 b - - 2 13",
      stockfishScoreCp = 29
    ),
    followUp(
      id = "K05A",
      seedId = "K05",
      label = "Sveshnikov d5 outpost continuation 1",
      fen = "r1bq1rk1/5ppp/p1np4/1p1Np1b1/4P3/2P3P1/PPN2P1P/R2QKB1R w KQ - 1 13",
      stockfishScoreCp = 48
    ),
    followUp(
      id = "K05B",
      seedId = "K05",
      label = "Sveshnikov d5 outpost continuation 4",
      fen = "r1bq1rk1/5ppp/p1np4/1p1Np1b1/4P3/2PB4/PPN2PPP/R2QK2R w KQ - 4 13",
      stockfishScoreCp = 17
    ),
    followUp(
      id = "K06A",
      seedId = "K06",
      label = "Closed French knight vs bishop continuation 1",
      fen = "r2q1rk1/pp1bbppp/2n1p3/3pP3/3P3n/4NN2/PP2BPPP/R1BQ1RK1 w - - 5 12",
      stockfishScoreCp = 52
    ),
    followUp(
      id = "K06B",
      seedId = "K06",
      label = "Closed French knight vs bishop continuation 2",
      fen = "r4rk1/pp1bbppp/1qn1p3/3pPn2/3P4/5N2/PPN1BPPP/1RBQ1RK1 w - - 5 12",
      stockfishScoreCp = 40
    ),
    followUp(
      id = "K06C",
      seedId = "K06",
      label = "Closed French knight vs bishop continuation 3",
      fen = "r2q1rk1/pp1bbppp/2n1p3/3pP3/3P2Pn/5N2/PPN1BP1P/R1BQ1RK1 w - - 1 12",
      stockfishScoreCp = 44
    ),
    followUp(
      id = "K06D",
      seedId = "K06",
      label = "Closed French knight vs bishop continuation 4",
      fen = "r4rk1/pp1bbppp/1qn1p3/3pPn2/3P4/P4N2/1PN1BPPP/R1BQ1RK1 w - - 1 12",
      stockfishScoreCp = 28
    ),
    followUp(
      id = "K07A",
      seedId = "K07",
      label = "Ruy Lopez before h3 continuation 1",
      fen = "r1bq1rk1/2p1bppp/p2p1n2/np2p3/4P3/1BP2N1P/PP1P1PP1/RNBQR1K1 w - - 1 10",
      stockfishScoreCp = 90
    ),
    followUp(
      id = "K07B",
      seedId = "K07",
      label = "Ruy Lopez before h3 continuation 2",
      fen = "rnbq1rk1/2p1bppp/p2p1n2/1p2p3/4P3/2P2N2/PPBP1PPP/RNBQR1K1 w - - 3 10",
      stockfishScoreCp = 105
    ),
    followUp(
      id = "K07C",
      seedId = "K07",
      label = "Ruy Lopez before h3 continuation 4",
      fen = "r1bq1rk1/2p1bppp/p2p1n2/np2p3/4P3/1BPP1N2/PP3PPP/RNBQR1K1 w - - 1 10",
      stockfishScoreCp = 50
    ),
    followUp(
      id = "K08A",
      seedId = "K08",
      label = "Dragon attack shell continuation 1",
      fen = "2rq1rk1/pp1bppb1/3p1np1/7p/2nNP2P/1BN1BP2/PPPQ2P1/1K1R3R w - - 2 14",
      stockfishScoreCp = 13
    ),
    followUp(
      id = "K08B",
      seedId = "K08",
      label = "Dragon attack shell continuation 3",
      fen = "2r2rk1/ppqbppb1/3p1np1/4n2p/3NP2P/PBN1BP2/1PPQ2P1/2KR3R w - - 1 14",
      stockfishScoreCp = 40
    ),
    followUp(
      id = "K09A",
      seedId = "K09",
      label = "IQP trade-down window continuation 1",
      fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PP2PPBP/2RQ1RK1 w - - 4 13",
      stockfishScoreCp = 92
    ),
    followUp(
      id = "K09B",
      seedId = "K09",
      label = "IQP trade-down window continuation 2",
      fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13",
      stockfishScoreCp = 60
    ),
    followUp(
      id = "K09C",
      seedId = "K09",
      label = "IQP trade-down window continuation 3",
      fen = "r1b1r1k1/pp1qbpp1/2n2n1p/3p4/3N4/P1N1B1P1/1P2PPBP/R2Q1RK1 w - - 1 13",
      stockfishScoreCp = 87
    ),
    followUp(
      id = "K10A",
      seedId = "K10",
      label = "Hedgehog containment continuation 4",
      fen = "r2qk2r/1b2bppp/pp1ppn2/2n5/2P5/BPN1QNP1/P3PPBP/R2R2K1 w kq - 4 12",
      stockfishScoreCp = 40
    ),
    followUp(
      id = "B11A",
      seedId = "B11",
      label = "KID fianchetto clamp setup continuation 2",
      fen = "r1bq1rk1/ppp1npbp/3p2p1/3Pp2n/2P1P3/2N2N2/PPQ1BPPP/R1B2RK1 w - - 3 10",
      stockfishScoreCp = 110
    ),
    followUp(
      id = "B12B",
      seedId = "B12",
      label = "Maroczy stronger bind stays space-led continuation 4",
      fen = "r2q1rk1/pp1nppbp/2bp2p1/8/2PBP3/2N2P2/PPR1B1PP/3Q1RK1 w - - 2 13",
      stockfishScoreCp = 63
    ),
    followUp(
      id = "B14A",
      seedId = "B14",
      label = "Open-file pressure with kingside hints continuation 1",
      fen = "r2q1rk1/pb1nbpp1/1p5p/2pp3n/3P3B/2NBPN2/PPQ2PPP/2R2RK1 w - - 3 13",
      stockfishScoreCp = 74,
      stockfishMaxAbsCp = 130
    ),
    followUp(
      id = "B14B",
      seedId = "B14",
      label = "Open-file pressure with kingside hints continuation 3",
      fen = "r2q1rk1/pb1nbpp1/1p5p/2pp4/3Pn2B/2NBPN2/PP2QPPP/2RR2K1 w - - 3 13",
      stockfishScoreCp = 64
    ),
    followUp(
      id = "B14C",
      seedId = "B14",
      label = "Open-file pressure with kingside hints continuation 4",
      fen = "r2q1rk1/pb1nbpp1/1p5p/2pp4/3Pn3/2NBPNB1/PP2QPPP/2R2RK1 w - - 3 13",
      stockfishScoreCp = 48
    ),
    followUp(
      id = "B15A",
      seedId = "B15",
      label = "Carlsbad fixed-chain pressure continuation 1",
      fen = "r1bqr1k1/pp2bpp1/2p1nn1p/3p4/3P3B/2NBPP2/PPQ1N1PP/3R1RK1 w - - 0 13",
      stockfishScoreCp = 79
    ),
    followUp(
      id = "B16B",
      seedId = "B16",
      label = "Carlsbad pressure resists trade overclassification continuation 2",
      fen = "r1b1rnk1/pp2qppp/2p2n2/3p4/3P4/3BPP2/PPQ1N1PP/R2N1RK1 w - - 2 14",
      stockfishScoreCp = 62
    ),
    followUp(
      id = "B17A",
      seedId = "B17",
      label = "Neutral French exchange shell continuation 1",
      fen = "rnbqr1k1/pp3pp1/2pb1n1p/3p4/3P1B2/2NB4/PPPQNPPP/R4RK1 w - - 0 10",
      stockfishScoreCp = 24
    ),
    followUp(
      id = "B17B",
      seedId = "B17",
      label = "Neutral French exchange shell continuation 2",
      fen = "r1bqr1k1/pp3ppp/n1pb1n2/3p4/3P1B2/2NB4/PPP1NPPP/R2QR1K1 w - - 4 10",
      stockfishScoreCp = 21
    ),
    followUp(
      id = "B17C",
      seedId = "B17",
      label = "Neutral French exchange shell continuation 4",
      fen = "rnbqr1k1/pp3pp1/2pb1n1p/3p4/3P1B2/2NB4/PPP1NPPP/R1Q2RK1 w - - 0 10",
      stockfishScoreCp = 82
    ),
    followUp(
      id = "B18A",
      seedId = "B18",
      label = "French central files stay line-led continuation 3",
      fen = "r1bq1rk1/1p1n2pp/2n1p3/p1bpPp2/P7/2PB1N2/1P1N1PPP/R1BQR1K1 w - f6 0 12",
      stockfishScoreCp = 83,
      stockfishMaxAbsCp = 130
    ),
    followUp(
      id = "K05C",
      seedId = "K05",
      label = "Sveshnikov d5 outpost continuation 6",
      fen = "r1bq1rk1/5ppp/p1np4/1p1Np1b1/4P3/2P1N3/PP3PPP/R2QKB1R w KQ - 4 13",
      stockfishScoreCp = 36
    ),
    followUp(
      id = "B13A",
      seedId = "B13",
      label = "Knight on e5 stays outpost-led continuation 6",
      fen = "r2q1rk1/1b1nbpp1/pp3n1p/2ppN3/Q2P3B/2NBP3/PP3PPP/2R2RK1 w - - 0 13",
      stockfishScoreCp = 43
    ),
    followUp(
      id = "K05D",
      seedId = "K05",
      label = "Sveshnikov d5 outpost continuation 8",
      fen = "r1bq1rk1/4nppp/p2p4/1p1Np1b1/4P3/2PB1Q2/PPN2PPP/R3K2R w KQ - 6 14",
      stockfishScoreCp = 13
    ),
    followUp(
      id = "K05E",
      seedId = "K05",
      label = "Sveshnikov d5 outpost continuation 9",
      fen = "1rbq1rk1/5ppp/p1np4/1p1Np1b1/4P3/2PBN3/PP3PPP/R2QK2R w KQ - 6 14",
      stockfishScoreCp = 47
    ),
    followUp(
      id = "K05F",
      seedId = "K05",
      label = "Sveshnikov d5 outpost continuation 10",
      fen = "r1bq1rk1/5ppp/p1np3b/1p1Np3/4P2P/2P3P1/PPN2P2/R2QKB1R w KQ - 1 14",
      stockfishScoreCp = 53
    ),
    followUp(
      id = "B13B",
      seedId = "K05",
      label = "Knight on e5 stays outpost-led continuation 5",
      fen = "r2q1rk1/pb1nbp2/1p3npp/2ppNB2/3P3B/2N1P3/PP3PPP/2RQ1RK1 w - - 0 13",
      stockfishScoreCp = 53
    ),
    followUp(
      id = "K09D",
      seedId = "K09",
      label = "IQP trade-down window curated continuation 1",
      fen = "1r1q1rk1/pp3ppp/2n2n2/3p4/3P2b1/2N2N2/PP2BPPP/2RQ1RK1 w - - 3 13",
      stockfishScoreCp = -5
    ),
    followUp(
      id = "K09E",
      seedId = "K09",
      label = "IQP trade-down window curated continuation 2",
      fen = "r1bq1rk1/pp3ppp/5n2/3p4/1PnP4/2N2N2/P3BPPP/R2Q1RK1 w - - 1 13",
      stockfishScoreCp = -17
    ),
    followUp(
      id = "K09F",
      seedId = "K09",
      label = "IQP trade-down window continuation 4",
      fen = "2rqr1k1/pp2bpp1/2n1bn1p/3p4/3N4/P1N1B1P1/1P2PPBP/2RQ1RK1 w - - 1 14",
      stockfishScoreCp = 40
    ),
    followUp(
      id = "K09G",
      seedId = "K09",
      label = "IQP trade-down window continuation 5",
      fen = "r2qr1k1/pp2bpp1/4bn1p/n2p4/3N4/1QN1B1P1/PP2PPBP/R4RK1 w - - 6 14",
      stockfishScoreCp = 40
    ),
    followUp(
      id = "K09H",
      seedId = "K09",
      label = "IQP trade-down window continuation 6",
      fen = "r1b1r1k1/pp1qbpp1/5n1p/3pn3/1P1N4/P1N1B1P1/4PPBP/R2Q1RK1 w - - 1 14",
      stockfishScoreCp = 56
    ),
    followUp(
      id = "K09I",
      seedId = "K09",
      label = "IQP trade-down window curated continuation 3",
      fen = "1r1q1rk1/pp3ppp/2n2n2/3p1b2/3P4/2N2N1P/PP2BPP1/R2Q1RK1 w - - 1 13",
      stockfishScoreCp = 13
    ),
    followUp(
      id = "K08C",
      seedId = "K08",
      label = "Dragon attack shell continuation 5",
      fen = "2r2rk1/ppqbppb1/3p1np1/7p/2nNP2P/PBN1BPP1/1PPQ4/2KR3R w - - 1 15",
      stockfishScoreCp = 1
    ),
    followUp(
      id = "K08D",
      seedId = "K08",
      label = "Dragon attack shell continuation 6",
      fen = "2rq1rk1/1p1bppb1/p2p1np1/4n1Bp/3NP2P/1BN2P2/PPPQ2P1/2KR3R w - - 0 14",
      stockfishScoreCp = 16
    ),
    followUp(
      id = "K08E",
      seedId = "K08",
      label = "Dragon attack shell continuation 7",
      fen = "2rq1rk1/1p1bppb1/p2p1np1/4n2p/3NP2P/1BN1BP2/PPP1Q1P1/2KR3R w - - 0 14",
      stockfishScoreCp = -25
    ),
    followUp(
      id = "K08F",
      seedId = "K08",
      label = "Dragon attack shell continuation 8",
      fen = "2r2rk1/ppqbppb1/3p1np1/6Bp/2nNP2P/PBN2P2/1PPQ2P1/2KR3R w - - 3 15",
      stockfishScoreCp = 43
    ),
    followUp(
      id = "K08G",
      seedId = "K08",
      label = "Dragon attack shell continuation 9",
      fen = "2rq1r2/pp1bppbk/3p1np1/4n2p/3NP2P/1BN1BP2/PPPQ2P1/1K1R3R w - - 2 14",
      stockfishScoreCp = 52
    ),
    followUp(
      id = "K10B",
      seedId = "K10",
      label = "Hedgehog containment continuation 5",
      fen = "r2q1rk1/1b2bppp/pp1ppn2/2n5/2P4P/BPN1QNP1/P3PPB1/R2R2K1 w - - 1 13",
      stockfishScoreCp = 25
    ),
    followUp(
      id = "K10C",
      seedId = "K10",
      label = "Hedgehog containment continuation 6",
      fen = "r2qk2r/1b2bppp/pp1ppn2/8/1PP1n3/B1N1QNP1/P3PPBP/R2R2K1 w kq - 1 13",
      stockfishScoreCp = 64
    ),
    followUp(
      id = "K10D",
      seedId = "K10",
      label = "Hedgehog containment continuation 7",
      fen = "r2qk2r/1b2bppp/pp1ppn2/2n5/2PQ3N/BPN3P1/P3PPBP/R2R2K1 w kq - 4 12",
      stockfishScoreCp = 69
    ),
    // --- Generated Slav / Meran / Moscow ---
    followUp(
      id = "K11A",
      seedId = "K11",
      label = "Slav main line tension continuation 2",
      fen = "rnbq1rk1/pp2bppp/4pn2/2p5/2BP4/2N1PN2/PP3PPP/R1BQ1RK1 w - - 2 1",
      stockfishScoreCp = 110
    ),
    followUp(
      id = "K12A",
      seedId = "K12",
      label = "Meran semi-slav complex continuation 2",
      fen = "r2qk2r/pb1nbppp/1pn1p3/2ppP3/3P4/2PB1N2/PP1N1PPP/R1BQR1K1 w kq - 2 1",
      stockfishScoreCp = 112
    ),
    followUp(
      id = "K12B",
      seedId = "K12",
      label = "Meran semi-slav complex continuation 4",
      fen = "r2qk2r/pb1nbpp1/1pn1p2p/2ppP3/3P4/2PB1N2/PP3PPP/R1BQRNK1 w kq - 0 1",
      stockfishScoreCp = 112
    ),
    followUp(
      id = "K12C",
      seedId = "K12",
      label = "Meran semi-slav complex continuation 6",
      fen = "r3k2r/pbqnbpp1/1pn1p2p/2ppP3/3P4/2PB1N2/PP1B1PPP/R2QRNK1 w kq - 2 1",
      stockfishScoreCp = 112
    ),
    followUp(
      id = "K12D",
      seedId = "K12",
      label = "Meran semi-slav complex continuation 2",
      fen = "r2qk2r/pb1nbppp/1pn1p3/2ppP3/3P4/2PB1N1P/PP1N1PP1/R1BQ1RK1 w kq - 1 1",
      stockfishScoreCp = 108
    ),
    followUp(
      id = "K12E",
      seedId = "K12",
      label = "Meran semi-slav complex continuation 4",
      fen = "r3k2r/pbqnbppp/1pn1p3/2ppP3/3P4/2PB1N1P/PP1N1PP1/R1BQR1K1 w kq - 3 1",
      stockfishScoreCp = 108
    ),
    followUp(
      id = "K12F",
      seedId = "K12",
      label = "Meran semi-slav complex continuation 6",
      fen = "r3k2r/pbqnbpp1/1pn1p2p/2ppP3/3P4/2PB1N1P/PP3PP1/R1BQRNK1 w kq - 0 1",
      stockfishScoreCp = 108
    ),
    followUp(
      id = "K13A",
      seedId = "K13",
      label = "Moscow semi-slav lines continuation 2",
      fen = "r1bqkb1r/pp3ppp/2n1p3/3pP3/2pP4/2PB1N2/P1P2PPP/R1BQK2R w KQkq - 0 1",
      stockfishScoreCp = -9
    ),
    followUp(
      id = "K13B",
      seedId = "K13",
      label = "Moscow semi-slav lines continuation 4",
      fen = "r1bqk2r/pp2bppp/2n1p3/3pP3/2pP4/2P2N2/P1P1BPPP/R1BQK2R w KQkq - 2 1",
      stockfishScoreCp = -9
    ),
    followUp(
      id = "K13C",
      seedId = "K13",
      label = "Moscow semi-slav lines continuation 6",
      fen = "r1bq1rk1/pp2bppp/2n1p3/3pP3/2pP4/2P2N2/P1P1BPPP/R1BQ1RK1 w - - 4 1",
      stockfishScoreCp = -9
    ),
    // --- Generated Ruy Lopez ---
    followUp(
      id = "B19A",
      seedId = "B19",
      label = "Ruy Lopez Chigorin prophylaxis continuation 2",
      fen = "r1bq1rk1/2p1bppp/p2p1n2/np2p3/4P3/1BP2N1P/PP1P1PP1/RNBQR1K1 w - - 1 1",
      stockfishScoreCp = 87
    ),
    followUp(
      id = "B20A",
      seedId = "B20",
      label = "Ruy Lopez Breyer prophylaxis continuation 2",
      fen = "r1bq1rk1/2p1bppp/p1np1n2/1p2p3/B3P3/2PP1N2/PP3PPP/RNBQR1K1 w - - 0 1",
      stockfishScoreCp = 91
    ),
    // --- Generated Berlin / Grunfeld / Benoni ---
    followUp(
      id = "K14A",
      seedId = "K14",
      label = "Berlin Endgame targets continuation 2",
      fen = "r1bk3r/ppp2ppp/2p5/2b1P3/5Bn1/8/PPP2PPP/RN2KB1R w KQ - 2 1",
      stockfishScoreCp = -12
    ),
    followUp(
      id = "K15A",
      seedId = "K15",
      label = "Grunfeld Exchange Mainline continuation 2",
      fen = "r1bq1rk1/pp3pbp/2nppnp1/2p5/2PPP3/2N1BP2/PP1QN1PP/R3KB1R w KQ - 0 1",
      stockfishScoreCp = 71
    ),
    followUp(
      id = "B21A",
      seedId = "B21",
      label = "Modern Benoni target fixing continuation 2",
      fen = "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 2 1",
      stockfishScoreCp = 119
    ),
    // --- KID & Catalan & Endgames (Qualitative Selections) ---
    followUp(
      id = "K16A",
      seedId = "K16",
      label = "KID Mar del Plata prophylaxis 2",
      fen = "r1bq1rk1/pppnnpbp/3p2p1/3Pp3/2P1P3/2N1BN2/PP1QBPPP/R4RK1 b - - 2 10",
      stockfishScoreCp = 15
    ),
    followUp(
      id = "K17A",
      seedId = "K17",
      label = "Open Catalan tension continuation 2",
      fen = "rnq2rk1/pb2bppp/1p2pn2/2p5/2p5/2N2NP1/PP2PPBP/R1BQR1K1 w - - 1 11",
      stockfishScoreCp = -24
    ),
    followUp(
      id = "K18A",
      seedId = "K18",
      label = "Rook Endgame maneuvering 2",
      fen = "8/5pk1/1p2p1p1/p1r4p/P1R2P1P/1P1R2P1/5K2/8 b - - 1 41",
      stockfishScoreCp = 45
    ),
    followUp(
      id = "K19A",
      seedId = "K19",
      label = "Endgame material conversion 2",
      fen = "8/1k1r4/4p3/1P1pP2p/R2P3P/3K2P1/8/8 b - - 1 45",
      stockfishScoreCp = 115,
      requireMaterialParity = false
    )
  )

  val all: List[Fixture] =
    canonical ++ stockfishBalancedSupplemental
