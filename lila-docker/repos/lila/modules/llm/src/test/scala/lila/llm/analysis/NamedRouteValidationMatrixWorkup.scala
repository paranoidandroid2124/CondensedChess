package lila.llm.analysis

object NamedRouteValidationMatrixWorkup:

  final case class Candidate(
      id: String,
      source: String,
      fen: String,
      trigger: String,
      depth: Int = 18,
      multiPv: Int = 5
  )

  private val candidates =
    List(
      Candidate(
        id = "b6_control",
        source = "NamedRouteChainBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4"
      ),
      Candidate(
        id = "b4_file_occupancy_only",
        source = "LocalFileEntryBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "c3b4"
      ),
      Candidate(
        id = "b5_queen_infiltration",
        source = "HeavyPieceLocalBindNegativeValidationTest",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24",
        trigger = "c2c4"
      ),
      Candidate(
        id = "k09b_b7_blocker",
        source = "StrategicIdeaFenFixtures.K09B",
        fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13",
        trigger = "d4e6"
      ),
      Candidate(
        id = "yan_posture_blocker",
        source = "CommentaryTrustHardening.md B8 local QC seed",
        fen = "2b3k1/p3qp1p/6p1/4P3/3p4/P2B3P/3Q1PP1/6K1 w - - 0 28",
        trigger = "d2a5"
      ),
      Candidate(
        id = "k09c",
        source = "StrategicIdeaFenFixtures.K09C",
        fen = "r1b1r1k1/pp1qbpp1/2n2n1p/3p4/3N4/P1N1B1P1/1P2PPBP/R2Q1RK1 w - - 1 13",
        trigger = "d1b3"
      ),
      Candidate(
        id = "k09g",
        source = "StrategicIdeaFenFixtures.K09G",
        fen = "r2qr1k1/pp2bpp1/4bn1p/n2p4/3N4/1QN1B1P1/PP2PPBP/R4RK1 w - - 6 14",
        trigger = "b3c2"
      ),
      Candidate(
        id = "k09h",
        source = "StrategicIdeaFenFixtures.K09H",
        fen = "r1b1r1k1/pp1qbpp1/5n1p/3pn3/1P1N4/P1N1B1P1/4PPBP/R2Q1RK1 w - - 1 14",
        trigger = "d1b3"
      ),
      Candidate(
        id = "b21a",
        source = "StrategicIdeaFenFixtures.B21A",
        fen = "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 2 1",
        trigger = "f3d2"
      ),
      Candidate(
        id = "rubinstein_duras",
        source = "web:ModernChess Rubinstein-Duras 1911",
        fen = "r4bk1/1r2n1p1/p2p1p1p/1q1Pp3/2N1P3/RP1QBPP1/6KP/R7 w - - 0 27",
        trigger = "d3d2"
      )
    )

  @main def runNamedRouteValidationMatrixWorkup(args: String*): Unit =
    val requestedIds =
      args
        .find(_.startsWith("--ids="))
        .map(_.stripPrefix("--ids=").split(",").toList.map(_.trim).filter(_.nonEmpty).toSet)
        .getOrElse(Set.empty)
    val requestedDepth =
      args
        .find(_.startsWith("--depth="))
        .flatMap(arg => arg.stripPrefix("--depth=").toIntOption)
    val requestedMultiPv =
      args
        .find(_.startsWith("--multipv="))
        .flatMap(arg => arg.stripPrefix("--multipv=").toIntOption)

    val selected =
      candidates.filter(candidate =>
        requestedIds.isEmpty || requestedIds.contains(candidate.id)
      )

    HeavyPieceLocalBindEngineVerifier.resolvedEnginePath() match
      case None =>
        println("No engine available. Set STOCKFISH_BIN or LLM_ACTIVE_CORPUS_ENGINE_PATH.")
      case Some(enginePath) =>
        println(s"engine_path=$enginePath")
        println(
          "id\tsource\tfen\ttrigger\troot_best\ttrigger_is_root_best\troot_pv1\tafter_best\tafter_pv1\treplay_complete\treplay_features"
        )
        selected.foreach { candidate =>
          val depth = requestedDepth.getOrElse(candidate.depth)
          val multiPv = requestedMultiPv.getOrElse(candidate.multiPv)
          val root =
            HeavyPieceLocalBindEngineVerifier
              .analyze(
                fen = candidate.fen,
                depth = depth,
                multiPv = multiPv
              )
              .getOrElse(sys.error(s"missing root analysis for ${candidate.id}"))
          val after =
            HeavyPieceLocalBindEngineVerifier
              .analyze(
                fen = candidate.fen,
                depth = depth,
                multiPv = multiPv,
                moves = List(candidate.trigger)
              )
              .getOrElse(sys.error(s"missing after-trigger analysis for ${candidate.id}"))
          val replay =
            after.lines.headOption.flatMap { line =>
              HeavyPieceLocalBindValidation.replayBranchLine(
                candidate.fen,
                candidate.trigger :: line.moves
              )
            }

          val row =
            List(
              candidate.id,
              candidate.source,
              candidate.fen,
              candidate.trigger,
              root.bestMove.getOrElse(""),
              (root.bestMove.contains(candidate.trigger)).toString,
              root.lines.headOption.map(renderLine).getOrElse(""),
              after.bestMove.getOrElse(""),
              after.lines.headOption.map(renderLine).getOrElse(""),
              replay.exists(_.complete).toString,
              replay.map(_.features.mkString(",")).getOrElse("")
            )
          println(row.map(escape).mkString("\t"))
        }

  private def renderLine(line: HeavyPieceLocalBindEngineVerifier.EngineLine): String =
    val mate = line.mate.map(m => s" mate=$m").getOrElse("")
    s"${line.moves.mkString(" ")} [cp=${line.scoreCp}$mate depth=${line.depth}]"

  private def escape(value: String): String =
    Option(value).getOrElse("").replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
