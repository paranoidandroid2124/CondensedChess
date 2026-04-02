package lila.llm.analysis

object TransitionRouteCorpusWorkup:

  private val candidates = TaskShiftProvingFixtures.transitionCandidates

  @main def runTransitionRouteCorpusWorkup(args: String*): Unit =
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
    val selectedCandidates =
      candidates.filter(candidate =>
        requestedIds.isEmpty || requestedIds.contains(candidate.id)
      )

    HeavyPieceLocalBindEngineVerifier.resolvedEnginePath() match
      case None =>
        println("No engine available. Set STOCKFISH_BIN or LLM_ACTIVE_CORPUS_ENGINE_PATH.")
      case Some(enginePath) =>
        println(s"engine_path=$enginePath")
        println(
          "id\tsource\texpected_tags\tnote\tside\tengine_best_move\tpv1_head\tbest_move_consistent\tdepth\tmulti_pv\treplay_complete\treplay_features\tpv1\tpv2\tpv3\tpv4\tpv5"
        )
        selectedCandidates.foreach { candidate =>
          val depth = requestedDepth.getOrElse(candidate.depth)
          val multiPv = requestedMultiPv.getOrElse(candidate.multiPv)
          val analysis =
            HeavyPieceLocalBindEngineVerifier
              .analyze(
                fen = candidate.fen,
                depth = depth,
                multiPv = multiPv
              )
              .getOrElse(sys.error(s"engine analysis missing for ${candidate.id}"))
          val bestLine = analysis.lines.headOption
          val replay =
            bestLine.flatMap(line =>
              HeavyPieceLocalBindValidation.replayBranchLine(candidate.fen, line.moves)
            )
          val side =
            candidate.fen.trim.split("\\s+").lift(1).getOrElse("?")
          val pv1Head =
            bestLine.flatMap(_.moves.headOption).getOrElse("")
          val pvs =
            (1 to 5).toList.map { idx =>
              analysis.lines.lift(idx - 1) match
                case Some(line) =>
                  val mate =
                    line.mate.map(m => s" mate=$m").getOrElse("")
                  s"${line.moves.mkString(" ")} [cp=${line.scoreCp}$mate depth=${line.depth}]"
                case None => ""
            }
          val replayFeatures =
            replay.map(_.features.mkString(",")).getOrElse("")
          val replayComplete =
            replay.exists(_.complete)
          val row =
            List(
              candidate.id,
              candidate.source,
              candidate.expectedTags.mkString(","),
              candidate.note,
              side,
              analysis.bestMove.getOrElse(""),
              pv1Head,
              analysis.bestMove.contains(pv1Head).toString,
              depth.toString,
              multiPv.toString,
              replayComplete.toString,
              replayFeatures
            ) ++ pvs
          println(row.map(escape).mkString("\t"))
        }

  private def escape(value: String): String =
    value.replace("\t", " ").replace("\r", " ").replace("\n", " ").trim
