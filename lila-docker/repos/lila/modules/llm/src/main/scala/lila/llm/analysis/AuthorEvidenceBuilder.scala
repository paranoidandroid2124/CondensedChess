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
