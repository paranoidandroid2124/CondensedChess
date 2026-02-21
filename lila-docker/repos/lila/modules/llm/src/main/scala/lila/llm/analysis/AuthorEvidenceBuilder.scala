package lila.llm.analysis

import lila.llm.model.*
import lila.llm.model.authoring.*

/**
 * Convert ProbeResults into renderable, validated evidence for AuthorQuestions.
 *
 * Important: Evidence lines are validated (UCI→SAN length match) to avoid illegal/partial citations.
 */
object AuthorEvidenceBuilder:

  def build(
    fen: String,
    ply: Int,
    playedMove: Option[String],
    bestMove: Option[String],
    authorQuestions: List[AuthorQuestion],
    probeResults: List[ProbeResult]
  ): List[QuestionEvidence] =
    if playedMove.isEmpty then return Nil
    val playedUci = playedMove.get
    if (probeResults.isEmpty) return Nil

    val afterFen = NarrativeUtils.uciListToFen(fen, List(playedUci))
    val bestUciOpt = bestMove.filter(_.nonEmpty)
    val afterBestFenOpt = bestUciOpt.map(uci => NarrativeUtils.uciListToFen(fen, List(uci)))

    val byQuestion = probeResults.flatMap { pr =>
      pr.questionId.map(qid => qid -> pr)
    }.groupBy(_._1).view.mapValues(_.map(_._2)).toMap

    def dropUnsafeEndingCaptures(sans: List[String]): List[String] =
      // Recapture sanity: do not cite a line that ends on an unrefuted capture.
      // (Allow captures that give check/mate: "+" / "#".)
      sans.reverse.dropWhile(s => s.contains("x") && !s.contains("+") && !s.contains("#")).reverse

    authorQuestions.flatMap { q =>
      val rs = byQuestion.getOrElse(q.id, Nil)
      if (rs.isEmpty) Nil
      else
        val grouped = rs.groupBy(_.purpose.getOrElse(""))
        grouped.toList.flatMap { case (purpose, prs) =>
          purpose match
            case "free_tempo_branches" =>
              q.latentPlan.toList.flatMap { lp =>
                val cfg = lp.evidencePolicy.freeTempoVerify.getOrElse(FreeTempoVerify())

                def seedAppears(theirUci: String, pv: List[String]): Boolean =
                  if (pv.isEmpty) false
                  else
                    val afterTheirFen = NarrativeUtils.uciListToFen(afterFen, List(theirUci))
                    val within = Math.max(1, cfg.seedMustAppearWithinPlies)
                    pv.take(within).exists { u =>
                      lp.candidateMoves.exists(p => MovePredicates.matchesMovePattern(afterTheirFen, u, p))
                    }

                val branches =
                  prs
                    .flatMap(pr => pr.probedMove.map(m => m -> pr))
                    .distinctBy(_._1)
                    .flatMap { (theirUci, pr) =>
                      val pvTail = pr.bestReplyPv.take(10)
                      if (pvTail.isEmpty) None
                      else if (!seedAppears(theirUci, pvTail)) None
                      else
                        val lineUcis = theirUci :: pvTail.take(8)
                        val sans = NarrativeUtils.uciListToSan(afterFen, lineUcis)
                        val ok = sans.size == lineUcis.size
                        if (!ok) None
                        else
                          val safeSans = dropUnsafeEndingCaptures(sans)
                          if (safeSans.isEmpty) None
                          else
                            val line = NarrativeUtils.formatSanWithMoveNumbers(ply + 1, safeSans)
                            val key = s"...${NarrativeUtils.uciToSanOrFormat(afterFen, theirUci)}"
                            Some(
                              EvidenceBranch(
                                keyMove = key,
                                line = line,
                                evalCp = Some(pr.evalCp),
                                mate = pr.mate,
                                depth = pr.depth,
                                sourceId = Some(pr.id)
                              )
                            )
                    }
                    .take(cfg.maxBranches)

                Option.when(branches.size >= cfg.minBranches)(
                  QuestionEvidence(questionId = q.id, purpose = purpose, branches = branches)
                )
              }

            case "latent_plan_immediate" =>
              val branches =
                prs
                  .flatMap(pr => pr.probedMove.map(m => m -> pr))
                  .distinctBy(_._1)
                  .flatMap { (candUci, pr) =>
                    // This probe tested playing the candidate immediately.
                    // If the eval drastically drops compared to baseline, it's a "Why not now?" proof.
                    // We render: Candidate -> Response -> ...
                    val pv = pr.bestReplyPv.take(10)
                    val lineUcis = candUci :: pv
                    val sans = NarrativeUtils.uciListToSan(fen, lineUcis)
                    val ok = sans.size == lineUcis.size
                    if (!ok) None
                    else
                      val safeSans = dropUnsafeEndingCaptures(sans)
                      if (safeSans.isEmpty) None
                      else
                        val line = NarrativeUtils.formatSanWithMoveNumbers(ply + 1, safeSans)
                        val key = NarrativeUtils.uciToSanOrFormat(fen, candUci)
                        Some(
                          EvidenceBranch(
                            keyMove = key,
                            line = line,
                            evalCp = Some(pr.evalCp),
                            mate = pr.mate,
                            depth = pr.depth,
                            sourceId = Some(pr.id)
                          )
                        )
                  }
                  .sortBy(_.evalCp.getOrElse(0)) // Sort by eval (lower is worse for us, assuming us POV? No, eval is white POV usually)
                  // We want to show the "refutation", i.e. lines where we suffer.
              
              // `ply` is the played-move ply (odd=White move, even=Black move).
              // In latent_plan_immediate, we are evaluating "play this now" from the
              // same side that made the played move.
              val isWhite = ply % 2 == 1
              val ordered = 
                if (isWhite) branches.sortBy(_.evalCp.getOrElse(0)) // Show worst first
                else branches.sortBy(b => -b.evalCp.getOrElse(0)) // Black to move, high eval is bad for Black
              
              Option.when(ordered.nonEmpty)(
                QuestionEvidence(questionId = q.id, purpose = purpose, branches = ordered.take(2))
              )

            case "latent_plan_refutation" =>
              val branches =
                prs
                  .flatMap(pr => pr.probedMove.map(m => m -> pr))
                  .distinctBy(_._1)
                  .flatMap { (theirUci, pr) =>
                    val pvTail = pr.bestReplyPv.take(10)
                    val lineUcis = theirUci :: pvTail.take(8)
                    val sans = NarrativeUtils.uciListToSan(afterFen, lineUcis)
                    val ok = sans.size == lineUcis.size
                    if (!ok) None
                    else
                      val safeSans = dropUnsafeEndingCaptures(sans)
                      if (safeSans.isEmpty) None
                      else
                        val line = NarrativeUtils.formatSanWithMoveNumbers(ply + 1, safeSans)
                        val key = s"...${NarrativeUtils.uciToSanOrFormat(afterFen, theirUci)}"
                        Some(
                          EvidenceBranch(
                            keyMove = key,
                            line = line,
                            evalCp = Some(pr.evalCp),
                            mate = pr.mate,
                            depth = pr.depth,
                            sourceId = Some(pr.id)
                          )
                        )
                  }
                  .sortBy(_.evalCp.getOrElse(0))
                  .take(2)

              val themIsWhite = (ply % 2 == 0) // after played move, opponent is to move
              val ordered =
                if (themIsWhite) branches.sortBy(b => -b.evalCp.getOrElse(0))
                else branches.sortBy(_.evalCp.getOrElse(0))

              Option.when(ordered.nonEmpty)(
                QuestionEvidence(questionId = q.id, purpose = purpose, branches = ordered)
              )

            case "recapture_branches" =>
              val branches =
                prs
                  .flatMap(pr => pr.probedMove.map(m => m -> pr))
                  .distinctBy(_._1)
                  .flatMap { (recapUci, pr) =>
                    val pvTail = pr.bestReplyPv.take(10)
                    val lineUcis = playedUci :: recapUci :: pvTail
                    val sans = NarrativeUtils.uciListToSan(fen, lineUcis)
                    val ok = sans.size == lineUcis.size
                    if (!ok) None
                    else
                      val afterSans = dropUnsafeEndingCaptures(sans.drop(1))
                      if (afterSans.size < 2) None
                      else
                        val line = NarrativeUtils.formatSanWithMoveNumbers(ply + 1, afterSans)
                        val key = s"...${NarrativeUtils.uciToSanOrFormat(afterFen, recapUci)}"
                        Some(
                          EvidenceBranch(
                            keyMove = key,
                            line = line,
                            evalCp = Some(pr.evalCp),
                            mate = pr.mate,
                            depth = pr.depth,
                            sourceId = Some(pr.id)
                          )
                        )
                  }
                  // For opponent choices, lower evalCp (White POV) is better for the opponent.
                  .sortBy(_.evalCp.getOrElse(0))
                  .take(3)

              Option.when(branches.size >= 2)(
                QuestionEvidence(questionId = q.id, purpose = purpose, branches = branches)
              )

            case "reply_multipv" | "defense_reply_multipv" | "convert_reply_multipv" =>
              val branches =
                prs
                  .flatMap(pr => pr.probedMove.map(m => m -> pr))
                  .flatMap { (_, pr) =>
                    val pvs = pr.replyPvs.getOrElse(if (pr.bestReplyPv.nonEmpty) List(pr.bestReplyPv) else Nil)
                    pvs.take(3).flatMap { pv =>
                      val lineUcis = playedUci :: pv.take(9)
                      val sans = NarrativeUtils.uciListToSan(fen, lineUcis)
                      val ok = sans.size == lineUcis.size
                      if (!ok) None
                      else
                        val afterSans = dropUnsafeEndingCaptures(sans.drop(1))
                        if (afterSans.isEmpty) None
                        else
                          val line = NarrativeUtils.formatSanWithMoveNumbers(ply + 1, afterSans)
                          val firstReplyUci = pv.headOption
                          val key =
                            firstReplyUci match
                              case Some(u) => s"...${NarrativeUtils.uciToSanOrFormat(afterFen, u)}"
                              case None    => "..."
                          Some(
                            EvidenceBranch(
                              keyMove = key,
                              line = line,
                              evalCp = Some(pr.evalCp),
                              mate = pr.mate,
                              depth = pr.depth,
                              sourceId = Some(pr.id)
                            )
                          )
                    }
                  }
                  .distinctBy(_.keyMove)
                  .take(3)

              Option.when(branches.size >= 2)(
                QuestionEvidence(questionId = q.id, purpose = purpose, branches = branches)
              )

            case "keep_tension_branches" =>
              val bestUci = bestUciOpt.getOrElse("")
              val afterBestFen = afterBestFenOpt.getOrElse("")
              if (bestUci.isEmpty || afterBestFen.isEmpty) Nil
              else
                val branches =
                  prs
                    .flatMap(pr => pr.probedMove.map(m => m -> pr))
                    .distinctBy(_._1)
                    .flatMap { (theirUci, pr) =>
                      val pvTail = pr.bestReplyPv.take(10)
                      val lineUcis = bestUci :: theirUci :: pvTail
                      val sans = NarrativeUtils.uciListToSan(fen, lineUcis)
                      val ok = sans.size == lineUcis.size
                      if (!ok) None
                      else
                        val afterSans = dropUnsafeEndingCaptures(sans.drop(1))
                        if (afterSans.isEmpty) None
                        else
                          val line = NarrativeUtils.formatSanWithMoveNumbers(ply + 1, afterSans)
                          val key = s"...${NarrativeUtils.uciToSanOrFormat(afterBestFen, theirUci)}"
                          Some(
                            EvidenceBranch(
                              keyMove = key,
                              line = line,
                              evalCp = Some(pr.evalCp),
                              mate = pr.mate,
                              depth = pr.depth,
                              sourceId = Some(pr.id)
                            )
                          )
                    }
                    .sortBy(_.evalCp.getOrElse(0))
                    .take(3)

                Option.when(branches.size >= 2)(
                  QuestionEvidence(questionId = q.id, purpose = purpose, branches = branches)
                )

            case _ => Nil
        }
    }
