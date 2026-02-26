package lila.llm.analysis

import lila.llm.model.Motif
import munit.FunSuite

class MoveAnalyzerParityTest extends FunSuite:

  private val StartFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val Pv = List("e2e4", "e7e5", "g1f3", "b8c6")

  private def motifSignature(m: Motif): String =
    val prefix = m.getClass.getSimpleName.replace("$", "")
    s"$prefix|${m.category}|${m.color}|${m.plyIndex}|${m.move.getOrElse("")}"

  private def withMoveAnalyzerProps[T](props: Map[String, String])(run: => T): T =
    val keys = props.keys.toList
    val snapshot = keys.map(k => k -> sys.props.get(k)).toMap
    props.foreach { case (k, v) => sys.props.update(k, v) }
    try run
    finally
      snapshot.foreach {
        case (k, Some(v)) => sys.props.update(k, v)
        case (k, None)    => sys.props.remove(k)
      }

  test("state/trajectory provider remains parity-equivalent to legacy path on baseline PV") {
    val trajectory = withMoveAnalyzerProps(
      Map(
        "llm.moveAnalyzer.useStateTrajectoryProvider" -> "true",
        "llm.moveAnalyzer.dualRunParity" -> "false"
      )
    ) {
      MoveAnalyzer.tokenizePv(StartFen, Pv).map(motifSignature)
    }

    val legacy = withMoveAnalyzerProps(
      Map(
        "llm.moveAnalyzer.useStateTrajectoryProvider" -> "false",
        "llm.moveAnalyzer.dualRunParity" -> "false"
      )
    ) {
      MoveAnalyzer.tokenizePv(StartFen, Pv).map(motifSignature)
    }

    assertEquals(trajectory, legacy)
    assert(trajectory.nonEmpty)
  }

  test("dual-run parity mode keeps output stable while enabled") {
    val withDualRun = withMoveAnalyzerProps(
      Map(
        "llm.moveAnalyzer.useStateTrajectoryProvider" -> "true",
        "llm.moveAnalyzer.dualRunParity" -> "true",
        "llm.moveAnalyzer.parityLogEvery" -> "100000"
      )
    ) {
      MoveAnalyzer.tokenizePv(StartFen, Pv).map(motifSignature)
    }

    val withoutDualRun = withMoveAnalyzerProps(
      Map(
        "llm.moveAnalyzer.useStateTrajectoryProvider" -> "true",
        "llm.moveAnalyzer.dualRunParity" -> "false"
      )
    ) {
      MoveAnalyzer.tokenizePv(StartFen, Pv).map(motifSignature)
    }

    assertEquals(withDualRun, withoutDualRun)
  }
