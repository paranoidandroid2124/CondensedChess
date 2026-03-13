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
      phase: String = "middlegame"
  )

  val all: List[Fixture] = List(
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
    )
  )
