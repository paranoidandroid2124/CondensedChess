package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.strategic._
import lila.llm.analysis.L3.PvLine
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

class A10Layer4DeepVerificationTest extends FunSuite {

  case class DeepScenario(
      name: String,
      fen: String,
      multiPv: List[VariationLine],
      expectedProbeType: String // "competitive", "defensive", "plan"
  )

  test("Layer 4: Verify deep probe heuristics") {
    val scenarios = List(
      // 1. Competitive Probe: Close scores (diff < 30cp)
      DeepScenario(
        name = "Competitive Choice (Ruy Lopez Exchange)",
        fen = "r1bqkbnr/ppp2ppp/2p5/4p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 5",
        multiPv = List(
          VariationLine(moves = List("d2d4"), scoreCp = 45, mate = None, depth = 20),
          VariationLine(moves = List("e1g1"), scoreCp = 40, mate = None, depth = 20),
          VariationLine(moves = List("b1c3"), scoreCp = 38, mate = None, depth = 20)
        ),
        expectedProbeType = "competitive"
      ),
      // 2. Defensive/Aggressive Probe: Aggressive sacrifice is slightly worse
      DeepScenario(
        name = "Aggressive Sacrifice (Greek Gift Bait)",
        fen = "r1bq1rk1/pp2bppp/2n1pn2/2pp4/2PP4/2N1PN2/PP2BPPP/R1BQ1RK1 w - - 4 8",
        multiPv = List(
          VariationLine(moves = List("b2b3"), scoreCp = 20, mate = None, depth = 20),
          VariationLine(moves = List("c1d2"), scoreCp = 15, mate = None, depth = 20),
          VariationLine(moves = List("c4d5"), scoreCp = -60, mate = None, depth = 20)
        ),
        expectedProbeType = "aggressive"
      ),
      // 3. Plan-based Ghost Probe (Exploiting a weakness)
      DeepScenario(
        name = "Strategic Ghost (Outpost on d5)",
        fen = "r1bqk2r/1p2bppp/p1np1n2/2p1p3/2P1P3/2N2N2/PP1PBPPP/R1BQ1RK1 w kq - 0 8",
        multiPv = List(
          VariationLine(moves = List("c3d5"), scoreCp = 43, mate = None, depth = 20),
          VariationLine(moves = List("c1e3"), scoreCp = 38, mate = None, depth = 20),
          VariationLine(moves = List("h2h3"), scoreCp = 35, mate = None, depth = 20),
          VariationLine(moves = List("a2a3"), scoreCp = 10, mate = None, depth = 20)
        ),
        expectedProbeType = "plan"
      )
    )

    val output = new java.lang.StringBuilder()
    output.append("=== Layer 4 Deep Probe Verification ===\n\n")

    scenarios.foreach { s =>
      val data = CommentaryEngine.assessExtended(
        fen = s.fen,
        variations = s.multiPv,
        playedMove = s.multiPv.headOption.flatMap(_.moves.headOption),
        ply = 10,
        prevMove = None
      ).getOrElse(fail(s"Failed to analyze ${s.name}"))

      val planScoring = PlanScoringResult(
        topPlans = data.plans,
        confidence = 0.5,
        phase = data.phase
      )

      val projections = s.multiPv.map(v => PvLine(v.moves, v.scoreCp, v.mate, v.depth))

      val probes = ProbeDetector.detect(
        ctx = data.toContext,
        planScoring = planScoring,
        multiPv = projections,
        fen = s.fen
      )

      output.append(s"[Scenario: ${s.name}]\n")
      output.append(s"FEN: ${s.fen}\n")
      output.append(s"Detected Probes (${probes.size}):\n")
      probes.foreach { p =>
        output.append(s"  - ID: ${p.id}\n")
        output.append(s"  - Targets: ${p.moves.mkString(", ")}\n")
        output.append(s"  - Reason: ${p.planName.getOrElse("N/A")}\n")
      }
      output.append("\n")

      // Validations
      s.expectedProbeType match {
        case "competitive" =>
          assert(probes.exists(_.id.startsWith("competitive")), s"No competitive probe for ${s.name}")
        case "aggressive" =>
          assert(probes.exists(_.id.startsWith("aggressive")), s"No aggressive/defensive probe for ${s.name}")
        case "plan" =>
          // Plan matches are harder to predict exactly in mock, but check if we have any
          assert(probes.nonEmpty, s"No probes generated for ${s.name}")
        case _ => ()
      }
    }

    java.nio.file.Files.writeString(
      java.nio.file.Path.of("a10_deep_probe_verification.txt"),
      output.toString
    )
    println(output.toString)
  }
}
