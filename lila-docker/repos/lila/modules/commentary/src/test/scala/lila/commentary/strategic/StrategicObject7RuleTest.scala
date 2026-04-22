package lila.commentary.strategic

import StrategicObjectTestSupport.*
import lila.commentary.witness.WitnessSector

class StrategicObject7RuleTest extends munit.FunSuite:

  test("opening development regime is present on the frozen exact board"):
    assert(findBoard("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 2 3", "OpeningDevelopmentRegime").nonEmpty)

  test("opening development regime stays absent on the near miss"):
    assert(findBoard("r1bq1rk1/ppp1bppp/2np1n2/4p3/3PP3/2N1BN2/PPP1BPPP/R2QK2R w KQ - 6 7", "OpeningDevelopmentRegime").isEmpty)

  test("opening development regime rejects sparse late residue that is no longer an opening"):
    assert(findBoard("1n3b1k/3pp3/8/8/8/8/3PP3/2B3NK w - - 0 1", "OpeningDevelopmentRegime").isEmpty)

  test("opening development regime yields to a live central contact front"):
    val fen = "rnb1kbnr/ppp2ppp/8/3pp3/3PP3/8/PPP2PPP/RNB1KBNR w KQkq - 0 1"
    val current = extraction(fen)
    val context = StrategicObjectContext(current.rootState, current.primaryWitnesses, current.attachedWitnesses)

    assert(StrategicObjectHelpers.openingDevelopmentWindow(context))
    assert(findSector(fen, "CentralContactFront", WitnessSector.Center).nonEmpty)
    assert(findBoard(fen, "OpeningDevelopmentRegime").isEmpty)

  test("opening development regime yields to a live distributed contact regime"):
    val fen = "r3kbnr/p1p2ppp/8/1p1pp3/1P1PP1b1/3B4/P1P2PPP/RNB1K2R w - - 0 1"
    val current = extraction(fen)
    val context = StrategicObjectContext(current.rootState, current.primaryWitnesses, current.attachedWitnesses)

    assert(StrategicObjectHelpers.openingDevelopmentWindow(context))
    assert(findBoard(fen, "DistributedContactRegime").nonEmpty)
    assert(findBoard(fen, "OpeningDevelopmentRegime").isEmpty)

  test("opening development regime yields to a live endgame race scaffold"):
    val fen = "rnb1kbnr/ppppppp1/8/P7/7p/8/1PPPPPPP/RNB1KBNR w KQkq - 0 1"
    val current = extraction(fen)
    val context = StrategicObjectContext(current.rootState, current.primaryWitnesses, current.attachedWitnesses)

    assert(StrategicObjectHelpers.openingDevelopmentWindow(context))
    assert(findBoard(fen, "EndgameRaceScaffold").nonEmpty)
    assert(findBoard(fen, "OpeningDevelopmentRegime").isEmpty)

  test("distributed contact regime is present on the frozen exact board"):
    assert(findBoard("6k1/3n2pp/8/1pppp3/1PPPP3/5N2/6PP/6K1 w - - 0 1", "DistributedContactRegime").nonEmpty)

  test("distributed contact regime stays absent on the central-only near miss"):
    assert(findBoard("6k1/3n2pp/8/3pp3/3PP3/5N2/6PP/6K1 w - - 0 1", "DistributedContactRegime").isEmpty)

  test("endgame race scaffold is present on the frozen exact board"):
    assert(findBoard("4k3/2p5/3P4/8/6p1/8/4K3/8 w - - 0 1", "EndgameRaceScaffold").nonEmpty)

  test("endgame race scaffold stays absent on the one-sided near miss"):
    assert(findBoard("4k3/8/3P4/8/8/8/4K3/8 w - - 0 1", "EndgameRaceScaffold").isEmpty)
