package lila.llm.tools

import java.nio.file.{ Path, Paths }

object CommentaryPlayerReviewShardBuilder:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      reviewQueuePath: Path = DefaultReviewDir.resolve("review_queue.jsonl"),
      shardDir: Path = DefaultReviewDir.resolve("shards")
  )

  private val shardOrder = List(
    "quiet_openings",
    "normal_strategic_middlegames",
    "prophylaxis_defense",
    "tactical_cited_line",
    "compensation_practical",
    "chronicle_full_game_moments"
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val queue =
      readJsonLines[ReviewQueueEntry](config.reviewQueuePath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[player-qc-shards] failed to read review queue `${config.reviewQueuePath}`: $err")
          sys.exit(1)

    ensureDir(config.shardDir)
    val grouped = queue.groupBy(shardKey).withDefaultValue(Nil)
    val orderedShards =
      if usesAuditSharding(queue) then grouped.keySet.toList.sorted
      else shardOrder
    orderedShards.foreach { shard =>
      val entries = grouped(shard).sortBy(_.sampleId)
      writeJsonLines(config.shardDir.resolve(s"$shard.jsonl"), entries)
    }
    println(s"[player-qc-shards] wrote `${config.shardDir}` (${orderedShards.map(shard => s"$shard=${grouped(shard).size}").mkString(", ")})")

  private def shardKey(entry: ReviewQueueEntry): String =
    if entry.tier.nonEmpty && usesAuditSharding(List(entry)) then
      val surfaceKey =
        entry.surface match
          case ReviewSurface.Chronicle if entry.reviewKind == ReviewKind.WholeGame => "chronicle_whole_game"
          case ReviewSurface.Chronicle                                             => "chronicle_focus"
          case ReviewSurface.Bookmaker                                             => "bookmaker"
          case ReviewSurface.ActiveNote                                            => "active_note"
          case other                                                               => other
      s"${surfaceKey}_${entry.tier.getOrElse("unknown")}"
    else if entry.surface == ReviewSurface.Chronicle then "chronicle_full_game_moments"
    else
      entry.sliceKind match
        case SliceKind.OpeningTransition       => "quiet_openings"
        case SliceKind.StrategicChoice         => "normal_strategic_middlegames"
        case SliceKind.Prophylaxis             => "prophylaxis_defense"
        case SliceKind.TacticalTurn            => "tactical_cited_line"
        case SliceKind.CompensationOrExchangeSac | SliceKind.PracticalSimplification =>
          "compensation_practical"
        case SliceKind.EndgameConversion       => "normal_strategic_middlegames"
        case _                                 => "normal_strategic_middlegames"

  private def usesAuditSharding(entries: Iterable[ReviewQueueEntry]): Boolean =
    entries.exists(entry =>
      entry.tier.nonEmpty &&
        Set(ReviewKind.WholeGame, ReviewKind.FocusMoment, ReviewKind.BookmakerFocus, ReviewKind.ActiveParity).contains(entry.reviewKind)
    )

  private def parseConfig(args: List[String]): Config =
    val positional = args.filterNot(_.startsWith("--"))
    Config(
      reviewQueuePath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultReviewDir.resolve("review_queue.jsonl")),
      shardDir = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultReviewDir.resolve("shards"))
    )
