package lila.accountintel.snapshot

import lila.accountintel.*
import lila.accountintel.AccountIntel.*

object DecisionSnapshotDetector:

  private val TriggerPriority = List(
    "tension_release",
    "pawn_structure_mutation",
    "castling_commitment",
    "major_simplification",
    "file_commitment"
  )

  def detect(rows: List[SnapshotFeatureRow]): List[DecisionSnapshotCandidate] =
    val rowsByGame = rows.groupBy(_.gameId).view.mapValues(_.sortBy(_.ply)).toMap
    rows
      .sortBy(row => (row.gameId, row.ply))
      .flatMap: seed =>
        primaryTrigger(seed).flatMap: trigger =>
          chooseAnchor(seed, trigger, rowsByGame.getOrElse(seed.gameId, Nil))
      .distinctBy(candidate =>
        s"${candidate.gameId}:${candidate.ply}:${candidate.triggerType}:${candidate.structureFamily}:${candidate.windowStartPly}:${candidate.windowEndPly}"
      )

  private def chooseAnchor(
      seed: SnapshotFeatureRow,
      trigger: String,
      gameRows: List[SnapshotFeatureRow]
  ): Option[DecisionSnapshotCandidate] =
    val collapseBacked =
      seed.earliestPreventablePly.isDefined && seed.collapseMomentPly.exists(_ >= seed.ply)
    val windowStart = if collapseBacked then seed.earliestPreventablePly.get else (seed.ply - 2).max(1)
    val windowEnd = if collapseBacked then seed.collapseMomentPly.get else seed.ply + 1
    val windowRows =
      gameRows.filter: row =>
        row.ply >= windowStart &&
          row.ply <= windowEnd &&
          row.structureFamily == seed.structureFamily

    val eligibleRows =
      windowRows.filter: row =>
        row.quiet &&
          (!collapseBacked || row.transitionType.exists(_ != "Continuation") || row.planAlignmentBand.contains(
            "OffPlan"
          ))

    val chosen =
      eligibleRows
        .map(row => row -> commitmentScore(row, trigger, collapseBacked))
        .sortBy { case (row, score) => (row.ply, -score, -snapshotConfidence(row, score)) }
        .headOption
        .orElse:
          Option.when(acceptance(seed, trigger, commitmentScore(seed, trigger, collapseBacked), collapseBacked))(
            seed -> commitmentScore(seed, trigger, collapseBacked)
          )

    chosen.flatMap: (row, commitment) =>
      val confidence = snapshotConfidence(row, commitment)
      Option.when(acceptance(row, trigger, commitment, collapseBacked)):
        val transitionBucket = row.transitionType.filterNot(_ == "Continuation").getOrElse("quiet")
        val profileBucket = row.planAlignmentBand.getOrElse("na")
        DecisionSnapshotCandidate(
          gameId = row.gameId,
          triggerType = trigger,
          side = row.subjectColor,
          openingFamily = row.openingFamily,
          structureFamily = row.structureFamily,
          labels = row.labels,
          ply = row.ply,
          fen = row.fen,
          quiet = row.quiet,
          playedUci = row.playedUci,
          explainabilityScore = row.explainabilityScore,
          preventabilityScore = row.preventabilityScore,
          branchingScore = row.branchingScore,
          snapshotConfidence = confidence,
          commitmentScore = commitment,
          collapseBacked = collapseBacked,
          transitionType = row.transitionType,
          planAlignmentBand = row.planAlignmentBand,
          earliestPreventablePly = row.earliestPreventablePly,
          windowStartPly = windowStart,
          windowEndPly = windowEnd,
          repeatabilityKey =
            s"${colorKey(row.subjectColor)}|${slug(row.structureFamily)}|$trigger|${transitionBucket.toLowerCase}|${profileBucket.toLowerCase}",
          game = row.game,
          lastSan = row.lastSan
        )

  private def primaryTrigger(row: SnapshotFeatureRow): Option[String] =
    TriggerPriority
      .find(trigger => row.triggerHints.contains(trigger))
      .filter: trigger =>
        val commitment = commitmentScore(row, trigger, collapseBacked = false)
        acceptance(row, trigger, commitment, row.earliestPreventablePly.isDefined)

  private def commitmentScore(
      row: SnapshotFeatureRow,
      trigger: String,
      collapseBacked: Boolean
  ): Double =
    val base =
      trigger match
        case "tension_release" => 0.40
        case "pawn_structure_mutation" => 0.35
        case "major_simplification" => 0.25
        case "castling_commitment" => 0.18
        case "file_commitment" => 0.10
        case _ => 0.10
    val transitionBonus = Option.when(row.transitionType.exists(_ != "Continuation"))(0.15).getOrElse(0.0)
    val offPlanBonus = Option.when(row.planAlignmentBand.contains("OffPlan"))(0.15).getOrElse(0.0)
    val quietBonus = Option.when(row.quiet)(0.10).getOrElse(0.0)
    val collapseBonus = Option.when(collapseBacked)(0.20).getOrElse(0.0)
    (base + transitionBonus + offPlanBonus + quietBonus + collapseBonus).min(1.0)

  private def snapshotConfidence(row: SnapshotFeatureRow, commitment: Double): Double =
    val quietBonus = if row.quiet then 1.0 else 0.0
    (
      row.preventabilityScore * 0.30 +
        row.branchingScore * 0.25 +
        row.explainabilityScore * 0.20 +
        commitment * 0.15 +
        quietBonus * 0.10
    ).min(1.0)

  private def acceptance(
      row: SnapshotFeatureRow,
      trigger: String,
      commitment: Double,
      collapseBacked: Boolean
  ): Boolean =
    val confidence = snapshotConfidence(row, commitment)
    val preventable =
      row.earliestPreventablePly.exists(_ <= row.ply) || row.preventabilityScore >= 0.58
    val explainable =
      row.explainabilityScore >= 0.58 &&
        (row.planIntent.isDefined || row.transitionType.isDefined || row.hypothesisThemes.nonEmpty)
    val branching =
      row.branchingScore >= 0.55 ||
        row.transitionType.exists(Set("ForcedPivot", "NaturalShift", "Opportunistic"))
    val quietEnough =
      if trigger == "major_simplification" then row.quiet || collapseBacked
      else row.quiet
    val fileCommitmentPenalty =
      trigger != "file_commitment" ||
        row.planAlignmentBand.contains("OffPlan") ||
        row.branchingScore >= 0.72
    preventable && explainable && branching && quietEnough && fileCommitmentPenalty && confidence >= 0.58

