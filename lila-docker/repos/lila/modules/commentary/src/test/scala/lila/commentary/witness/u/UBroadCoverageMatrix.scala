package lila.commentary.witness.u

import lila.commentary.root.RootCoverageMatrix
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

private[commentary] object UBroadCoverageMatrix:

  private val resourcePath = "/commentary-corpus/u-coverage-matrix.md"
  private val witnessCorpusResourcePath = "/commentary-corpus/witness-expectations.jsonl"

  final case class DescriptorPolicy(
      descriptorId: String,
      ownerClass: String,
      currentFormalCorpusRows: Int,
      currentRuleTestCoverage: String,
      requiredRootDependencies: String,
      rootDependencyGate: String,
      anchorPolarityPayloadContract: String,
      breadthBuckets: String,
      minimumCorpusFloor: Int,
      status: String,
      exactBlocker: String
  )

  final case class WitnessCorpusRow(
      id: String,
      caseType: String,
      fen: String,
      descriptorId: String,
      expectation: String,
      coverageAxis: Option[String],
      coverageBucket: Option[String]
  ):
    def coveragePair: Option[(String, String)] =
      (coverageAxis, coverageBucket) match
        case (Some(axis), Some(bucket)) => Some(axis -> bucket)
        case (None, None)               => None
        case _ =>
          throw IllegalArgumentException(
            s"U witness corpus row $id must provide coverageAxis and coverageBucket together"
          )

  private val closedRootDependencyGate =
    "closed: required root dependencies are root broad-confidence-green"
  private val allowedFormalCaseTypes = Set("exact", "near_miss", "nasty_negative")
  private val allowedFormalExpectations = Set("present", "absent")
  private val BucketSpec = """([A-Za-z0-9_]+)\[([^\]]+)\]""".r

  private given Reads[WitnessCorpusRow] =
    (
      (__ \ "id").read[String] and
        (__ \ "caseType").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "descriptorId").read[String] and
        (__ \ "expectation").read[String] and
        (__ \ "coverageAxis").readNullable[String] and
        (__ \ "coverageBucket").readNullable[String]
    )(WitnessCorpusRow.apply)

  val primaryPolicies: Vector[DescriptorPolicy] = Vector(
    DescriptorPolicy(
      descriptorId = "file_lane_state",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 10,
      currentRuleTestCoverage = "unit: open exact; semi-open exact",
      requiredRootDependencies = "open_file; half_open_file",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "neutral file anchor; state=open|semi_open; semi-open payload carries open_for_color",
      breadthBuckets = "variant[open|semi_open], file_topology[edge|center], board_context[sparse|mixed], semi_open_owner[white|black], near_miss[closed_file]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers open/semi-open variants, edge/center files, sparse/mixed contexts, both semi-open owners, and closed-file fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "diagonal_lane_only",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 14,
      currentRuleTestCoverage = "unit: diagonal ray exact; underlength near-miss",
      requiredRootDependencies = "piece_on; controlled_by",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "neutral ray anchor; payload carries ray, source_piece_squares, endpoint_squares, optional blocker_square",
      breadthBuckets = "slider[bishop|queen], direction[NE|NW|SE|SW], endpoint[empty_ray|controlled_blocker], length[minimum|long], near_miss[underlength]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers bishop/queen sliders, queen controlled-blockers, all four diagonal directions, empty-ray and controlled-blocker endpoints, minimum/long rays, minimum controlled-blockers, and underlength fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "weak_pawn_target_state",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 12,
      currentRuleTestCoverage = "unit: fixed-pawn exact; passed-pawn near-miss",
      requiredRootDependencies = "piece_on; fixed_pawn; backward_pawn; isolated_pawn",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary piece-square anchor on defender pawn; payload carries square and weakness_tags",
      breadthBuckets = "weakness_tag[fixed|backward|isolated|combined], polarity[white|black], file_topology[edge|center], rank_stage[home|advanced], near_miss[healthy|passed]",
      minimumCorpusFloor = 10,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers fixed/backward/isolated/combined tags, both beneficiary colors, edge/center files, home/advanced ranks, and healthy/passed fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "passed_pawn_entity_state",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 10,
      currentRuleTestCoverage = "unit: passed exact; candidate-only near-miss",
      requiredRootDependencies = "piece_on; passed_pawn",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "owner piece-square anchor on passed pawn; payload carries square and owner",
      breadthBuckets = "polarity[white|black], file_topology[edge|center], rank_stage[midboard|7th_rank], phase[endgame|transition], near_miss[candidate_only|blocked_cone]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers both owner colors, edge/center files, midboard/7th-rank passers, endgame/transition contexts, and candidate-only/blocked-cone fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "weak_outpost_square_state",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 11,
      currentRuleTestCoverage = "unit: outpost exact; residual weak-square exact; outpost suppresses duplicate weak variant",
      requiredRootDependencies = "weak_square; outpost_square",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary square anchor; payload carries square and state=outpost|weak; outpost variant wins over residual weak variant",
      breadthBuckets = "state[outpost|weak], polarity[white|black], topology[center|edge], occupancy[empty|piece_occupied], fail_closed[challenge_restored|support_missing]",
      minimumCorpusFloor = 10,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers outpost/residual-weak variants, both beneficiary colors, center/edge topology, empty/occupied squares, support-missing outpost rejection, and challenge-restored outpost/weak fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "loose_piece_target_state",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 10,
      currentRuleTestCoverage = "unit: loose exact; defended exchange-parity near-miss",
      requiredRootDependencies = "piece_on; loose_piece",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary piece-square anchor on defender piece; payload carries square",
      breadthBuckets = "defender_polarity[white|black], piece_family[pawn|minor|rook|queen], exchange_state[undefended|losing_defended], near_miss[stably_defended]",
      minimumCorpusFloor = 10,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers both defender colors, pawn/minor/rook/queen targets, undefended and losing-defended exchange states, and stably-defended fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "pawn_push_break_contact_source",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 14,
      currentRuleTestCoverage = "unit: strategic contact exact; non-strategic lever near-miss; forged illegal lever near-miss",
      requiredRootDependencies = "piece_on; lever_available; fixed_pawn; backward_pawn; isolated_pawn; controlled_by; contested",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "owner piece-square anchor on pawn; payload carries owner_pawn_square and contact_variants with arrival/target/break-point squares",
      breadthBuckets = "push_type[single|double], target_family[burdened|center|chain_base|chain_head|phalanx_edge], break_point[fixed|contested|controlled], file_topology[edge|center], near_miss[non_strategic|illegal_push]",
      minimumCorpusFloor = 12,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers single/double pushes, burdened/center/chain-base/chain-head/phalanx-edge targets, fixed/contested/controlled break points, edge/center files, and non-strategic/illegal-push fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "sector_asymmetry_state",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 9,
      currentRuleTestCoverage = "unit: unequal sector exact; equal-count near-miss",
      requiredRootDependencies = "piece_on",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "neutral sector anchor; payload carries sector, white/black pawn counts, majority_side, minority_side",
      breadthBuckets = "sector[queenside|center|kingside], majority_side[white|black], magnitude[one|many], near_miss[equal_count]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers all sectors, both majority colors, one/many magnitude, and equal-count fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "available_lever_trigger",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 10,
      currentRuleTestCoverage = "unit: single/double variants exact; capture-only near-miss; forged illegal root near-miss",
      requiredRootDependencies = "piece_on; lever_available",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "owner piece-square anchor on pawn; payload carries owner_pawn_square and available_variants",
      breadthBuckets = "push_type[single|double], file_topology[edge|center], target_direction[left|right], polarity[white|black], near_miss[capture_only|illegal_push]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers single/double push variants, edge/center files, left/right target directions, both owner colors, and capture-only/illegal-push fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "rook_on_open_file_state",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 8,
      currentRuleTestCoverage = "unit: open-file exact; semi-open substitution near-miss",
      requiredRootDependencies = "piece_on; open_file",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "owner piece-square anchor on rook; payload carries rook_square and file",
      breadthBuckets = "polarity[white|black], file_topology[edge|center], board_context[sparse|mixed], near_miss[semi_open|blocked_file]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers both owner colors, edge/center files, sparse/mixed contexts, and semi-open/blocked-file fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "bishop_pair_state",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 8,
      currentRuleTestCoverage = "unit: bishop pair exact; promoted extra exact; single-bishop near-miss",
      requiredRootDependencies = "piece_on",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "owner board anchor; payload carries bishop_member_squares",
      breadthBuckets = "polarity[white|black], member_count[exact_two|promoted_extra], member_distribution[initial_pair|promoted_extra], near_miss[single_bishop]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers owner polarity, exact/promoted member counts, distribution, and single-bishop fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "knight_on_outpost_square",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 8,
      currentRuleTestCoverage = "unit: knight-on-outpost exact; no-outpost near-miss",
      requiredRootDependencies = "piece_on; outpost_square",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "owner piece-square anchor on knight; payload carries knight_square",
      breadthBuckets = "polarity[white|black], topology[center|edge], phase[middlegame|queenless_transition], near_miss[weak_not_outpost|non_knight_outpost]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers both knight colors, center/edge outposts, middlegame/queenless-transition contexts, and weak-not-outpost/non-knight-outpost fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "duty_bound_defender",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 12,
      currentRuleTestCoverage = "unit: pin-bound duty exact; no-pin near-miss; pinned-without-separate-duty near-miss",
      requiredRootDependencies = "piece_on; pinned_piece; trapped_piece; controlled_by; xray_target; king_ring_square; contested",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary piece-square anchor on defender; payload carries bound_modes and assigned/occupied/king-gate duty squares",
      breadthBuckets = "bound_mode[pin_bound|trapped_bound|combined], defender_family[minor|rook|queen], duty_family[occupied_pressure|king_gate], polarity[white|black], near_miss[unbound|no_separate_duty]",
      minimumCorpusFloor = 12,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers pin-bound, trapped-bound, and combined binding, minor/rook/queen defenders, occupied-pressure and king-gate duties, both beneficiary colors, and unbound/no-separate-duty fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "short_run_slider_gate_restriction",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 14,
      currentRuleTestCoverage = "unit: partial throttling exact; full trap/self-blocked/edge/uncontrolled/remote-wall near-misses",
      requiredRootDependencies = "piece_on; controlled_by",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary piece-square anchor on restricted slider; payload carries throttled/testable directions, ray mobility, gate squares, and open directions",
      breadthBuckets = "slider_family[bishop|rook|queen], throttled_ratio[partial_majority], gate_source[occupied|controlled|mixed], topology[center|edge], near_miss[full_trap|self_block|edge_short|uncontrolled|remote_wall]",
      minimumCorpusFloor = 12,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers bishop/rook/queen restricted sliders, partial-majority throttling, occupied/controlled/mixed gate sources, center/edge topology, and full-trap/self-block/edge-short/uncontrolled/remote-wall fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "pin",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 13,
      currentRuleTestCoverage = "unit: absolute exact; relative exact; unpinned near-miss",
      requiredRootDependencies = "piece_on; pinned_piece",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary ray anchor; payload carries ray, attacker/blocker/anchor squares, and pin_mode",
      breadthBuckets = "pin_mode[absolute_king|relative_anchor], geometry[file|rank|diagonal], slider_family[bishop|rook|queen], blocker_family[minor|rook|queen], near_miss[not_pinned|wrong_anchor_value]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers absolute/relative pin modes, file/rank/diagonal rays, bishop/rook/queen sliders, minor/rook/queen blockers, and not-pinned/wrong-anchor-value fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "fork",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 14,
      currentRuleTestCoverage = "unit: two-target exact; single-target near-miss",
      requiredRootDependencies = "piece_on",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary piece-square anchor on attacker; payload carries attacker_role, anchor_square, target_squares, and target_roles",
      breadthBuckets = "attacker_family[pawn|knight|bishop|rook|queen|king], target_count[two|three_plus], target_mix[nonking_pair|king_plus_piece], polarity[white|black], near_miss[single_target]",
      minimumCorpusFloor = 10,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers pawn/knight/bishop/rook/queen/king attackers, two and three-plus target counts, non-king and king-plus-piece target mixes, both beneficiary colors, and single-target/friendly-target fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "skewer",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 14,
      currentRuleTestCoverage = "unit: higher-front exact; wrong-value-order near-miss",
      requiredRootDependencies = "piece_on",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary ray anchor; payload carries ray, attacker square, slider role, front/rear squares, and front/rear roles",
      breadthBuckets = "slider_family[bishop|rook|queen], geometry[file|rank|diagonal], front_family[queen|rook|minor], rear_family[rook|minor|pawn], near_miss[wrong_value_order|no_rear]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers bishop/rook/queen sliders, file/rank/diagonal rays, queen/rook/minor front targets, rook/minor/pawn rear targets, and wrong-value-order/no-rear fail-closed boundaries"
    ),
    DescriptorPolicy(
      descriptorId = "overload",
      ownerClass = "U-primary",
      currentFormalCorpusRows = 11,
      currentRuleTestCoverage = "unit: two-duty exact; one-duty near-miss",
      requiredRootDependencies = "piece_on; overloaded_piece",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary piece-square anchor on overloaded defender; payload carries anchor role/square and duty_squares",
      breadthBuckets = "defender_family[minor|rook|queen], duty_count[two|three_plus], duty_piece_mix[minor_minor|minor_rook], polarity[white|black], near_miss[one_duty|root_overload_absent]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers minor/rook/queen overloaded defenders, two and three-plus occupied duties, minor-minor and minor-rook duty piece mixes, both beneficiary colors, and one-duty/root-absent fail-closed boundaries"
    )
  )

  val attachedPolicies: Vector[DescriptorPolicy] = Vector(
    DescriptorPolicy(
      descriptorId = "structural_space_claim",
      ownerClass = "U-attached",
      currentFormalCorpusRows = 16,
      currentRuleTestCoverage = "unit: closed-center exact; fixed-chain white exact; fixed-chain black exact; occupied/lone/branched/single-seed/disconnected near-misses",
      requiredRootDependencies = "piece_on; fixed_pawn; controlled_by",
      rootDependencyGate = closedRootDependencyGate,
      anchorPolarityPayloadContract = "beneficiary sector anchor attached only to closed_center or fixed_chain host vocabulary; payload carries host_id, sector, beneficiary, claimed_squares, boundary_pawn_squares, optional host_owner",
      breadthBuckets = "host[closed_center|fixed_chain], host_owner[white|black|none], sector[center|wing], beneficiary[white|black], component_shape[connected|best_component], near_miss[occupied_frontier|lone_lock|branched_chain|single_seed|disconnected_union]",
      minimumCorpusFloor = 12,
      status = "broad-confidence-green",
      exactBlocker = "closed: formal exact-board corpus covers closed-center/fixed-chain hosts, white/black/no host owner, center/wing sectors, both beneficiary colors, connected/best-component shapes, and occupied-frontier/lone-lock/branched-chain/single-seed/disconnected-union boundaries"
    )
  )

  val allPolicies: Vector[DescriptorPolicy] = primaryPolicies ++ attachedPolicies

  require(
    primaryPolicies.map(_.descriptorId).toSet == UScopeContract.activePrimaryDescriptorIds.map(_.value).toSet,
    "U broad matrix must cover exactly the active U-primary descriptor ids"
  )
  require(
    attachedPolicies.map(_.descriptorId).toSet == UAttachedScopeContract.activeAttachedDescriptorIds.map(_.value).toSet,
    "U broad matrix must cover exactly the active U-attached descriptor ids"
  )
  require(
    expectedFormalCorpusRows > 0 || allPolicies.forall(_.status != "broad-confidence-green"),
    "U broad-confidence-green must not be claimed before formal descriptor-local corpus rows exist"
  )
  require(
    allPolicies.forall(_.rootDependencyGate == closedRootDependencyGate),
    "R dependency gates must stay closed now that root broad-confidence-green is complete"
  )
  require(
    unresolvedRootDependencies.isEmpty,
    s"U root dependency gates cannot close while required root schemas are not green: $unresolvedRootDependencies"
  )
  require(
    expectedFormalCorpusRows > 0 || allPolicies.forall(_.status == "thin"),
    "U descriptors must remain thin until formal descriptor-local corpus rows exist"
  )
  require(
    formalCorpusRowsByDescriptor.keySet.subsetOf(activeDescriptorIdSet),
    s"Formal U corpus rows must use only active U descriptor ids: ${formalCorpusRowsByDescriptor.keySet.diff(activeDescriptorIdSet).toVector.sorted}"
  )
  require(
    formalCorpusDescriptorRowCounts == expectedFormalCorpusRowsByDescriptor,
    s"Formal U corpus descriptor counts must match the broad matrix: actual=$formalCorpusDescriptorRowCounts, expected=$expectedFormalCorpusRowsByDescriptor"
  )
  require(
    broadGreenPromotionViolations.isEmpty,
    s"U broad-confidence-green promotion gates are not closed: $broadGreenPromotionViolations"
  )

  def expectedFormalCorpusRows: Int =
    allPolicies.map(_.currentFormalCorpusRows).sum

  def expectedFormalCorpusRowsByDescriptor: Map[String, Int] =
    allPolicies.map(policy => policy.descriptorId -> policy.currentFormalCorpusRows).toMap

  def activePrimaryDescriptorIds: Vector[String] =
    primaryPolicies.map(_.descriptorId)

  def activeAttachedDescriptorIds: Vector[String] =
    attachedPolicies.map(_.descriptorId)

  def statuses: Vector[String] =
    allPolicies.map(_.status)

  def policyFor(descriptorId: String): DescriptorPolicy =
    allPolicies
      .find(_.descriptorId == descriptorId)
      .getOrElse(throw IllegalArgumentException(s"Unknown U descriptor id $descriptorId"))

  def formalCorpusRows: Vector[WitnessCorpusRow] =
    validateFormalCorpusRows(
      readResource(witnessCorpusResourcePath)
        .linesIterator
        .filter(_.trim.nonEmpty)
        .zipWithIndex
        .map: (line, index) =>
          Json.parse(line).validate[WitnessCorpusRow] match
            case JsSuccess(row, _) => row
            case JsError(errors) =>
              throw IllegalArgumentException(
                s"Failed to parse U witness corpus row ${index + 1}: ${JsError.toJson(errors)}"
              )
        .toVector
    )

  def formalCorpusRowsByDescriptor: Map[String, Vector[WitnessCorpusRow]] =
    formalCorpusRows.groupBy(_.descriptorId)

  def formalCorpusDescriptorRowCounts: Map[String, Int] =
    allPolicies
      .map: policy =>
        policy.descriptorId -> formalCorpusRowsByDescriptor.getOrElse(policy.descriptorId, Vector.empty).size
      .toMap

  def broadGreenPromotionViolations: Map[String, Vector[String]] =
    allPolicies
      .filter(_.status == "broad-confidence-green")
      .map: policy =>
        val rows = formalCorpusRowsByDescriptor.getOrElse(policy.descriptorId, Vector.empty)
        val rowCoveragePairs = rows.flatMap(_.coveragePair).toSet
        val missingCaseTypes = allowedFormalCaseTypes.diff(rows.map(_.caseType).toSet)
        val missingBuckets = requiredBreadthBucketPairs(policy).diff(rowCoveragePairs)
        val violations =
          Vector(
            Option.when(rows.size < policy.minimumCorpusFloor)(
              s"row count ${rows.size} is below minimum floor ${policy.minimumCorpusFloor}"
            ),
            Option.when(missingCaseTypes.nonEmpty)(
              s"missing case types ${missingCaseTypes.toVector.sorted.mkString(", ")}"
            ),
            Option.when(missingBuckets.nonEmpty)(
              s"missing breadth buckets ${formatBucketPairs(missingBuckets)}"
            )
          ).flatten
        policy.descriptorId -> violations
      .filter(_._2.nonEmpty)
      .toMap

  def rootDependencyGates: Vector[String] =
    allPolicies.map(_.rootDependencyGate)

  def rootDependencyStatuses: Map[String, String] =
    allPolicies
      .flatMap(requiredRootDependencies)
      .distinct
      .map(rootId => rootId -> RootCoverageMatrix.policyFor(rootId).status)
      .toMap

  def unresolvedRootDependencies: Map[String, Vector[String]] =
    allPolicies
      .map: policy =>
        policy.descriptorId ->
          requiredRootDependencies(policy)
            .filter(rootId => RootCoverageMatrix.policyFor(rootId).status != "broad-confidence-green")
      .filter(_._2.nonEmpty)
      .toMap

  def render(): String =
    val body = allPolicies.map: policy =>
      s"| `${policy.descriptorId}` | ${policy.ownerClass} | ${policy.currentFormalCorpusRows} | ${escapeCell(policy.currentRuleTestCoverage)} | ${escapeCell(policy.requiredRootDependencies)} | ${escapeCell(policy.rootDependencyGate)} | ${escapeCell(policy.anchorPolarityPayloadContract)} | ${escapeCell(policy.breadthBuckets)} | ${policy.minimumCorpusFloor} | ${escapeCell(policy.status)} | ${escapeCell(policy.exactBlocker)} |"

    (
      List(
        "# U Coverage Matrix",
        "",
        "`broad-confidence-green` at U is descriptor-local: a live U descriptor must arise only from the frozen `U = phi(R)` root bundle, keep its anchor/polarity/payload law stable across exact-board buckets, and fail closed at its negative boundaries.",
        "Engine evidence is not U truth. If a row needs engine sanity, that sanity belongs at root confound filtering or at Object/Certification validation, not inside U admission.",
        "",
        "The current formal U corpus is `witness-expectations.jsonl`. Descriptor unit tests are audit evidence only; U broad-confidence-green promotion requires descriptor-local formal rows.",
        "Formal U corpus rows must use runtime `descriptorId` values from the active U inventory, `caseType` in `exact|near_miss|nasty_negative`, `expectation` in `present|absent`, and descriptor-local `coverageAxis` / `coverageBucket` tags from the frozen breadth buckets; aggregate row count alone is not a promotion gate.",
        "All listed R dependency gates are closed by the root 25/25 broad-confidence-green result; every live U descriptor is broad-confidence-green under the current descriptor-local corpus.",
        "",
        "| descriptor | owner class | current formal corpus rows | current rule-test coverage | required root dependencies | R dependency gate | anchor / polarity / payload contract | breadth axes / buckets | minimum corpus floor | current status | exact blocker |",
        "| --- | --- | ---: | --- | --- | --- | --- | --- | ---: | --- | --- |"
      ) ++ body
    ).mkString("\n")

  def loadArtifact(): String =
    readResource(resourcePath)

  def formalCorpusRowCount(): Int =
    formalCorpusRows.size

  private def readResource(path: String): String =
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(path))
          .getOrElse(throw IllegalStateException(s"Missing test resource $path")),
        "UTF-8"
      )
    try source.mkString
    finally source.close()

  private def escapeCell(value: String): String =
    value.replace("|", "\\|")

  private def requiredRootDependencies(policy: DescriptorPolicy): Vector[String] =
    policy.requiredRootDependencies
      .split(";")
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .toVector

  private def activeDescriptorIdSet: Set[String] =
    allPolicies.map(_.descriptorId).toSet

  private def requiredBreadthBucketPairs(policy: DescriptorPolicy): Set[(String, String)] =
    policy.breadthBuckets
      .split(",")
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap:
        case BucketSpec(axis, buckets) =>
          buckets.split("\\|").iterator.map(bucket => axis -> bucket.trim)
        case malformed =>
          throw IllegalArgumentException(
            s"Malformed U breadth bucket spec for ${policy.descriptorId}: $malformed"
          )
      .toSet

  private def formatBucketPairs(pairs: Set[(String, String)]): String =
    pairs.toVector
      .sortBy((axis, bucket) => (axis, bucket))
      .map((axis, bucket) => s"$axis=$bucket")
      .mkString(", ")

  private def validateFormalCorpusRows(rows: Vector[WitnessCorpusRow]): Vector[WitnessCorpusRow] =
    val duplicateIds =
      rows.groupBy(_.id).collect { case (id, matches) if matches.size > 1 => id }.toVector.sorted
    require(duplicateIds.isEmpty, s"Duplicate U witness corpus row ids: ${duplicateIds.mkString(", ")}")

    rows.foreach: row =>
      require(row.id.trim.nonEmpty, "U witness corpus row id must be non-empty")
      require(row.fen.trim.nonEmpty, s"U witness corpus row ${row.id} must carry a FEN")
      require(
        activeDescriptorIdSet.contains(row.descriptorId),
        s"U witness corpus row ${row.id} uses inactive descriptorId ${row.descriptorId}"
      )
      require(
        allowedFormalCaseTypes.contains(row.caseType),
        s"U witness corpus row ${row.id} has invalid caseType ${row.caseType}; expected one of ${allowedFormalCaseTypes.toVector.sorted.mkString(", ")}"
      )
      require(
        allowedFormalExpectations.contains(row.expectation),
        s"U witness corpus row ${row.id} has invalid expectation ${row.expectation}; expected one of ${allowedFormalExpectations.toVector.sorted.mkString(", ")}"
      )
      require(
        row.coveragePair.nonEmpty,
        s"U witness corpus row ${row.id} must carry descriptor-local coverageAxis and coverageBucket"
      )
      require(
        requiredBreadthBucketPairs(policyFor(row.descriptorId)).contains(row.coveragePair.get),
        s"U witness corpus row ${row.id} uses coverage bucket ${row.coveragePair.get} outside ${row.descriptorId}'s frozen breadth buckets"
      )

    rows
