package lila.commentary.root

private[commentary] object RootCoverageMatrix:

  import RootAtomRegistry.*

  private val resourcePath = "/commentary-corpus/root-coverage-matrix.md"

  final case class AuditPolicy(
      riskClass: String,
      enginePolicy: String,
      breadthBuckets: String,
      minimumCorpusFloor: Int,
      status: String,
      exactBlocker: String,
      greenProofBuckets: Vector[String] = Vector.empty,
      greenEngineProbeBuckets: Vector[String] = Vector.empty
  )

  private val auditBySchema: Map[String, AuditPolicy] = Map(
    SchemaId.PieceOn -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; literal occupancy is exact board truth and never engine-owned",
      breadthBuckets = "piece_role[pawn|minor|rook|queen|king], square_topology[center|edge], polarity[white|black]",
      minimumCorpusFloor = 10,
      status = "broad-confidence-green",
      exactBlocker = "none; pawn/minor/rook/queen/king role buckets, center/edge topology, and white/black polarity are closed",
      greenProofBuckets = Vector(
        "white|king|edge",
        "black|king|edge",
        "white|pawn|center",
        "black|pawn|center",
        "white|minor|center",
        "black|minor|edge",
        "white|rook|edge",
        "black|rook|edge",
        "white|queen|center",
        "black|queen|center"
      )
    ),
    SchemaId.ControlledBy -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; attacked-square semantics are exact and local",
      breadthBuckets = "polarity[white|black], attacker_family[pawn|knight|bishop|rook|queen|king], source_topology[center|edge|corner], target_occupancy[empty_target|occupied_target]",
      minimumCorpusFloor = 10,
      status = "broad-confidence-green",
      exactBlocker = "none; pawn/knight/bishop/rook/queen/king attacker families, white/black polarity, center/edge/corner source topology, and empty/occupied target buckets are closed",
      greenProofBuckets = Vector(
        "white|knight|center|empty_target",
        "white|pawn|edge|empty_target",
        "black|pawn|edge|empty_target",
        "white|bishop|center|empty_target",
        "black|bishop|center|empty_target",
        "white|rook|corner|occupied_target",
        "black|queen|edge|occupied_target",
        "white|king|center|empty_target",
        "black|king|corner|empty_target",
        "white|knight|center|occupied_target"
      )
    ),
    SchemaId.PawnControlledBy -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; pawn attack geometry is exact and local",
      breadthBuckets = "polarity[white|black], topology[center|edge], case_family[exact|near_miss]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "none; white/black polarity, center/edge attack targets, and dual-polarity near-miss rows are closed",
      greenProofBuckets = Vector(
        "white|center|exact",
        "black|-|near_miss",
        "black|center|exact",
        "white|edge|exact",
        "black|edge|exact",
        "white|-|near_miss"
      )
    ),
    SchemaId.Contested -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; overlap of attacked-square masks is exact",
      breadthBuckets = "carrier[open_file_rooks|blocked_file_rooks|knights|rooks_rank|bishops|queens|disjoint_kings], topology[file_lane|central_square|edge_square|no_overlap], occupancy[empty|occupied]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "none; open-file, blocked-file, knight, rook-rank, bishop, queen, occupied, empty, and disjoint fail-closed overlap buckets are closed",
      greenProofBuckets = Vector(
        "open_file_rooks|file_lane|occupied",
        "blocked_file_rooks|central_square|occupied",
        "knights|central_square|empty",
        "knights|central_square|occupied",
        "rooks_rank|edge_square|empty",
        "bishops|central_square|empty",
        "queens|central_square|empty",
        "disjoint_kings|no_overlap|empty"
      )
    ),
    SchemaId.OpenFile -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; pawn absence on a file is literal",
      breadthBuckets = "file_topology[edge|center|all_files], board_context[all_open|mixed_board], adjacency[isolated_open|clustered_open]",
      minimumCorpusFloor = 4,
      status = "broad-confidence-green",
      exactBlocker = "none; all-open, mixed-board, center, edge, isolated, and clustered open-file buckets are closed",
      greenProofBuckets = Vector(
        "all_open|all_files|clustered_open",
        "mixed_board|center|isolated_open",
        "mixed_board|edge|isolated_open",
        "mixed_board|center|clustered_open"
      )
    ),
    SchemaId.HalfOpenFile -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; file occupancy split is literal",
      breadthBuckets = "owner[white|black], file_topology[edge|center], rival_pawn_layout[single_enemy_pawn|stacked_enemy_pawns], fail_closed[own_pawn_restored]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "none; white/black ownership, edge/center topology, single/stacked rival-pawn layouts, and own-pawn-restored near-miss buckets are closed",
      greenProofBuckets = Vector(
        "white|edge|single_enemy_pawn|exact",
        "white|edge|-|own_pawn_restored",
        "white|center|stacked_enemy_pawns|exact",
        "black|center|single_enemy_pawn|exact",
        "black|edge|stacked_enemy_pawns|exact",
        "black|center|-|own_pawn_restored"
      )
    ),
    SchemaId.KingRingSquare -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; king-ring membership is exact king geometry",
      breadthBuckets = "king_file[corner|wing|center], polarity[white|black], adjacency[orthogonal|diagonal]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "none; corner/wing/center king-file, white/black polarity, and orthogonal/diagonal ring-square buckets are closed",
      greenProofBuckets = Vector(
        "white|wing|orthogonal",
        "white|wing|diagonal",
        "white|corner|orthogonal",
        "white|corner|diagonal",
        "white|center|orthogonal",
        "white|center|diagonal",
        "black|corner|orthogonal",
        "black|corner|diagonal",
        "black|center|orthogonal",
        "black|center|diagonal",
        "black|wing|orthogonal",
        "black|wing|diagonal"
      )
    ),
    SchemaId.WeakSquare -> AuditPolicy(
      riskClass = "structural / engine-optional sanity",
      enginePolicy = "optional; use only to reject rows where mate, extreme eval skew, or tactical crush swamps the structural square story",
      breadthBuckets = "topology[center|edge], occupancy[empty|occupied_by_beneficiary_piece], challenge_state[challenge_absent|challenge_restored], material_regime[minor_piece|heavy_piece]",
      minimumCorpusFloor = 9,
      status = "broad-confidence-green",
      exactBlocker = "none; white/black polarity, center/edge topology, empty/occupied beneficiary-piece states, minor/heavy material regimes, and challenge-restored fail-closed buckets are closed",
      greenProofBuckets = Vector(
        "white|center|empty|minor_piece|challenge_absent",
        "white|center|-|minor_piece|challenge_restored",
        "white|edge|-|minor_piece|challenge_restored",
        "white|center|occupied_by_beneficiary_piece|minor_piece|challenge_absent",
        "white|edge|empty|minor_piece|challenge_absent",
        "white|edge|occupied_by_beneficiary_piece|minor_piece|challenge_absent",
        "black|center|empty|minor_piece|challenge_absent",
        "black|edge|empty|minor_piece|challenge_absent",
        "black|center|occupied_by_beneficiary_piece|minor_piece|challenge_absent",
        "white|center|empty|heavy_piece|challenge_absent",
        "white|edge|empty|heavy_piece|challenge_absent",
        "black|center|empty|heavy_piece|challenge_absent",
        "black|edge|empty|heavy_piece|challenge_absent",
        "black|center|-|minor_piece|challenge_restored",
        "black|edge|-|minor_piece|challenge_restored"
      )
    ),
    SchemaId.OutpostSquare -> AuditPolicy(
      riskClass = "structural / engine-required confound filter",
      enginePolicy = "yes; runnable scaffold currently enforces maxMatePly=2 and maxAbsCp=250, while material-cliff and tactical-release exclusions remain audit-side row curation",
      breadthBuckets = "polarity[white|black], phase[middlegame|queenless_transition], topology[center|edge], support_shape[direct|structural_path], fail_closed_state[no_support|challenge_restored]",
      minimumCorpusFloor = 14,
      status = "broad-confidence-green",
      exactBlocker = "none; polarity, phase, topology, support shape, and fail-closed rejection buckets are closed, and the selected engine probe buckets stay inside the frozen root confound filter",
      greenProofBuckets = Vector(
        "white|queenless_transition|center|direct|exact",
        "white|middlegame|center|structural_path|exact",
        "white|queenless_transition|center|structural_path|exact",
        "white|queenless_transition|edge|direct|exact",
        "white|queenless_transition|center|-|no_support",
        "white|queenless_transition|edge|-|no_support",
        "white|queenless_transition|edge|direct|challenge_restored",
        "black|queenless_transition|center|direct|exact",
        "black|middlegame|center|direct|exact",
        "black|queenless_transition|edge|direct|exact",
        "black|middlegame|edge|structural_path|exact",
        "black|queenless_transition|center|-|no_support",
        "black|queenless_transition|edge|-|no_support",
        "black|queenless_transition|center|direct|challenge_restored"
      ),
      greenEngineProbeBuckets = Vector(
        "white|queenless_transition|edge|direct|exact",
        "white|queenless_transition|edge|-|no_support",
        "white|middlegame|center|structural_path|exact",
        "white|queenless_transition|center|structural_path|exact",
        "black|queenless_transition|center|direct|exact",
        "black|middlegame|center|direct|exact",
        "black|queenless_transition|center|direct|challenge_restored",
        "black|middlegame|edge|structural_path|exact"
      )
    ),
    SchemaId.IsolatedPawn -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; adjacent-file pawn absence is literal structure",
      breadthBuckets = "file_topology[edge|center], rank_stage[home|advanced], neighbor_state[isolated|neighbor_restored]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "none; edge/center files, home/advanced ranks, white/black polarity, and neighbor-restored fail-closed rows are closed",
      greenProofBuckets = Vector(
        "white|center|home|isolated",
        "white|-|-|neighbor_restored",
        "white|edge|advanced|isolated",
        "black|edge|advanced|isolated",
        "black|center|home|isolated",
        "black|-|-|neighbor_restored"
      )
    ),
    SchemaId.BackwardPawn -> AuditPolicy(
      riskClass = "structural / engine-optional sanity",
      enginePolicy = "optional; use only to reject rows where tactical or material collapse overwhelms the denied-front structural slice",
      breadthBuckets = "polarity[white|black], front_denial[enemy_control|direct_blockade], file_topology[edge|center], support_state[support_missing|support_restored], material_regime[pawn_chain|mixed_pieces]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "none; polarity, denial mode, topology, support restoration, and material-regime buckets are closed",
      greenProofBuckets = Vector(
        "white|enemy_control|center|support_missing|mixed_pieces",
        "white|enemy_control|center|support_missing|pawn_chain",
        "white|direct_blockade|center|support_missing|mixed_pieces",
        "black|enemy_control|center|support_missing|pawn_chain",
        "black|enemy_control|center|support_restored|pawn_chain",
        "white|direct_blockade|edge|support_missing|mixed_pieces",
        "white|enemy_control|edge|support_restored|pawn_chain",
        "black|direct_blockade|edge|support_missing|mixed_pieces",
        "black|enemy_control|edge|support_restored|pawn_chain"
      )
    ),
    SchemaId.DoubledFile -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; doubled occupancy on a file is literal",
      breadthBuckets = "file_topology[edge|center], stack_shape[adjacent|split], polarity[white|black]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "none; edge/center topology, adjacent/split stacks, white/black polarity, and fail-closed non-doubled rows are closed",
      greenProofBuckets = Vector(
        "white|center|adjacent|exact",
        "white|-|-|near_miss",
        "white|edge|adjacent|exact",
        "white|center|split|exact",
        "black|center|adjacent|exact",
        "black|edge|split|exact",
        "black|-|-|near_miss"
      )
    ),
    SchemaId.PassedPawn -> AuditPolicy(
      riskClass = "structural / engine-optional sanity",
      enginePolicy = "optional; use only to reject rows where immediate mate or severe material skew hides the exact passed-pawn slice",
      breadthBuckets = "polarity[white|black], phase[transition|endgame], file_topology[center|edge], rank_stage[midboard|7th_rank], opposition[clear_cone|blocked_cone]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "none; polarity, phase, topology, rank-stage, and opposition buckets are closed",
      greenProofBuckets = Vector(
        "white|endgame|center|midboard|clear_cone",
        "white|endgame|center|midboard|blocked_cone",
        "white|endgame|edge|7th_rank|clear_cone",
        "white|transition|center|midboard|clear_cone",
        "black|endgame|center|midboard|clear_cone",
        "black|endgame|center|midboard|blocked_cone",
        "black|endgame|edge|7th_rank|clear_cone",
        "black|endgame|edge|midboard|clear_cone",
        "black|endgame|edge|midboard|blocked_cone"
      )
    ),
    SchemaId.CandidatePasser -> AuditPolicy(
      riskClass = "structural / engine-required confound filter",
      enginePolicy = "yes; runnable scaffold currently enforces maxMatePly=2 and maxAbsCp=300, while material-cliff and tactical-release exclusions remain audit-side row curation",
      breadthBuckets = "polarity[white|black], phase[transition|endgame], file_topology[center|edge], support_shape[ahead_support|same_rank_support], balance_state[candidate_exact|under_supported_near_miss|already_passed_negative]",
      minimumCorpusFloor = 14,
      status = "broad-confidence-green",
      exactBlocker = "none; polarity, phase, topology, support shape, fail-closed balance/pass buckets, and selected engine-probe buckets are closed under the frozen root confound filter",
      greenProofBuckets = Vector(
        "white|endgame|center|ahead_support|candidate_exact",
        "white|endgame|center|-|under_supported_near_miss",
        "white|endgame|center|same_rank_support|candidate_exact",
        "white|endgame|edge|ahead_support|candidate_exact",
        "white|endgame|edge|-|under_supported_near_miss",
        "white|endgame|edge|-|already_passed_negative",
        "white|transition|center|same_rank_support|candidate_exact",
        "white|transition|center|-|under_supported_near_miss",
        "white|transition|edge|-|already_passed_negative",
        "black|endgame|center|ahead_support|candidate_exact",
        "black|transition|center|same_rank_support|candidate_exact",
        "black|transition|edge|ahead_support|candidate_exact",
        "black|endgame|edge|-|under_supported_near_miss",
        "black|endgame|edge|-|already_passed_negative"
      ),
      greenEngineProbeBuckets = Vector(
        "white|endgame|center|ahead_support|candidate_exact",
        "white|endgame|center|-|under_supported_near_miss",
        "white|endgame|center|same_rank_support|candidate_exact",
        "white|endgame|edge|ahead_support|candidate_exact",
        "white|endgame|edge|-|under_supported_near_miss",
        "white|endgame|edge|-|already_passed_negative",
        "white|transition|center|same_rank_support|candidate_exact",
        "white|transition|center|-|under_supported_near_miss",
        "white|transition|edge|-|already_passed_negative",
        "black|endgame|center|ahead_support|candidate_exact",
        "black|transition|center|same_rank_support|candidate_exact",
        "black|transition|edge|ahead_support|candidate_exact",
        "black|endgame|edge|-|under_supported_near_miss",
        "black|endgame|edge|-|already_passed_negative"
      )
    ),
    SchemaId.FixedPawn -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; enemy pawn on the stop square is literal structure",
      breadthBuckets = "polarity[white|black], file_topology[edge|center], chain_shape[single_stop|mutual_chain]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "none; white/black polarity, edge/center topology, and single-stop/mutual-chain fixed-pawn shapes are closed",
      greenProofBuckets = Vector(
        "black|center|single_stop",
        "white|center|single_stop",
        "white|edge|single_stop",
        "black|edge|single_stop",
        "white|center|mutual_chain",
        "black|center|mutual_chain"
      )
    ),
    SchemaId.LoosePiece -> AuditPolicy(
      riskClass = "structural / engine-optional sanity",
      enginePolicy = "optional; use only to reject rows where a board-wide tactical crush makes the local exchange loss uninformative",
      breadthBuckets = "polarity[white|black], piece_family[pawn|minor|rook|queen], exact_exchange_state[undefended|nominally_defended_but_losing], fail_closed_state[stably_defended], material_regime[mixed|heavy]",
      minimumCorpusFloor = 12,
      status = "broad-confidence-green",
      exactBlocker = "none; polarity, pawn/minor/rook/queen family, undefended vs nominally-defended-losing exchange states, stably-defended fail-closed rows, and mixed/heavy material buckets are closed",
      greenProofBuckets = Vector(
        "white|minor|undefended|mixed",
        "white|queen|nominally_defended_but_losing|heavy",
        "white|minor|stably_defended|mixed",
        "white|minor|stably_defended|heavy",
        "black|rook|undefended|heavy",
        "black|rook|stably_defended|heavy",
        "white|rook|undefended|heavy",
        "white|rook|stably_defended|heavy",
        "black|queen|nominally_defended_but_losing|heavy",
        "white|pawn|undefended|mixed",
        "white|pawn|stably_defended|mixed",
        "black|pawn|undefended|mixed",
        "black|pawn|stably_defended|mixed"
      )
    ),
    SchemaId.PinnedPiece -> AuditPolicy(
      riskClass = "structural / engine-optional sanity",
      enginePolicy = "optional; use only to reject rows where unrelated tactical collapse dominates the line-pin slice",
      breadthBuckets = "polarity[white|black], pin_state[absolute|relative|fail_closed], line_geometry[file|diagonal|rank], piece_family[minor|rook|queen]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "none; polarity, pin-state, geometry, and piece-family buckets are closed",
      greenProofBuckets = Vector(
        "white|absolute|file|minor",
        "white|relative|diagonal|minor",
        "white|fail_closed|file|minor",
        "black|absolute|rank|rook",
        "black|fail_closed|rank|rook",
        "black|absolute|file|queen",
        "white|relative|file|rook",
        "white|fail_closed|file|rook"
      )
    ),
    SchemaId.OverloadedPiece -> AuditPolicy(
      riskClass = "structural / engine-optional sanity",
      enginePolicy = "optional; use only to reject rows where extreme tactical noise overwhelms the local defender burden",
      breadthBuckets = "polarity[white|black], piece_family[minor|rook|queen], defended_target_count[one_near_miss|two|three_plus], target_mix[minor_pair|minor_rook|three_minor]",
      minimumCorpusFloor = 9,
      status = "broad-confidence-green",
      exactBlocker = "none; white/black polarity, minor/rook/queen defender families, two-target and three-plus target burdens, minor-pair/minor-rook/three-minor target mixes, and one-target fail-closed rows are closed",
      greenProofBuckets = Vector(
        "white|queen|two|minor_pair",
        "white|queen|one_near_miss|one_target",
        "white|rook|two|minor_rook",
        "white|minor|two|minor_pair",
        "white|queen|three_plus|three_minor",
        "black|queen|two|minor_pair",
        "black|rook|two|minor_rook",
        "black|minor|two|minor_pair",
        "black|queen|one_near_miss|one_target"
      )
    ),
    SchemaId.TrappedPiece -> AuditPolicy(
      riskClass = "structural / engine-required confound filter",
      enginePolicy = "yes; runnable scaffold enforces maxMatePly=2 for selected trap rows and maxAbsCp=350 only for selected fail-closed rows, while gross-material-cliff and tactical-release exclusions remain audit-side row curation",
      breadthBuckets = "polarity[white|black], piece_family[minor|rook|queen], trap_mode[zero_safe_moves|all_exits_lose|one_exit_near_miss], nasty_negative[king|pawn]",
      minimumCorpusFloor = 10,
      status = "broad-confidence-green",
      exactBlocker = "none; minor/rook/queen family, zero-safe/all-exits-lose/one-exit fail-closed modes, white/black polarity, king/pawn category rejection, and selected engine-probe buckets are closed",
      greenProofBuckets = Vector(
        "white|minor|all_exits_lose",
        "white|minor|zero_safe_moves",
        "white|minor|one_exit_near_miss",
        "white|pawn|nasty_negative",
        "white|king|nasty_negative",
        "white|rook|zero_safe_moves",
        "white|queen|zero_safe_moves",
        "white|rook|one_exit_near_miss",
        "white|queen|one_exit_near_miss",
        "black|rook|zero_safe_moves"
      ),
      greenEngineProbeBuckets = Vector(
        "white|minor|zero_safe_moves",
        "white|pawn|nasty_negative",
        "white|rook|zero_safe_moves",
        "white|queen|zero_safe_moves",
        "white|rook|one_exit_near_miss",
        "white|queen|one_exit_near_miss",
        "black|rook|zero_safe_moves"
      )
    ),
    SchemaId.XrayTarget -> AuditPolicy(
      riskClass = "structural / engine-optional sanity",
      enginePolicy = "optional; use only to reject rows where an unrelated tactical win makes the x-ray target non-informative",
      breadthBuckets = "polarity[white|black], slider[rook|bishop|queen], line_geometry[file|diagonal|rank], blocker_type[own_piece|enemy_piece], fail_closed[target_absent|two_blockers|minor_target]",
      minimumCorpusFloor = 10,
      status = "broad-confidence-green",
      exactBlocker = "none; white/black polarity, rook/bishop/queen slider families, file/diagonal/rank geometry, own/enemy blocker ownership, and target-absent/two-blocker/minor-target fail-closed rows are closed",
      greenProofBuckets = Vector(
        "white|rook|file|enemy_piece",
        "white|rook|file|target_absent",
        "white|rook|file|own_piece",
        "white|bishop|diagonal|enemy_piece",
        "white|queen|rank|own_piece",
        "black|rook|file|enemy_piece",
        "black|bishop|diagonal|enemy_piece",
        "black|queen|rank|own_piece",
        "black|queen|rank|two_blockers",
        "black|queen|file|minor_target"
      )
    ),
    SchemaId.LeverAvailable -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; legal pawn push plus immediate contact is exact local geometry",
      breadthBuckets = "push_type[single|double], file_topology[edge|center], contact_direction[left|right], fail_closed[blocked_push]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "none; single/double pushes, edge/center files, left/right contact directions, white/black polarity, and blocked-push fail-closed rows are closed",
      greenProofBuckets = Vector(
        "white|single|center|right|exact",
        "white|double|center|right|exact",
        "white|single|edge|right|exact",
        "black|double|center|left|exact",
        "black|single|edge|left|exact",
        "white|-|-|-|blocked_push",
        "black|-|-|-|blocked_push"
      )
    ),
    SchemaId.KingShelterHole -> AuditPolicy(
      riskClass = "structural / engine-required confound filter",
      enginePolicy = "yes; runnable scaffold currently enforces maxMatePly=2 and maxAbsCp=300, while force-retention and tactical-release exclusions remain audit-side row curation",
      breadthBuckets = "phase[opening|middlegame], defender_home[white_home|black_home], hole_file[center_shield|wing_shield], regime[home_shelter|out_of_regime_negative]",
      minimumCorpusFloor = 10,
      status = "broad-confidence-green",
      exactBlocker = "none; exact/near-miss center and wing home-shelter buckets, white/black defender homes, opening/middlegame phase, out-of-regime rejection, and selected engine-probe buckets are closed",
      greenProofBuckets = Vector(
        "white|opening|black_home|wing_shield|home_shelter_exact",
        "white|opening|black_home|wing_shield|pawn_cover_near_miss",
        "black|middlegame|white_home|wing_shield|home_shelter_exact",
        "black|opening|white_home|wing_shield|no_access_near_miss",
        "black|opening|out_of_regime|out_of_regime|out_of_regime_negative",
        "white|opening|black_home|center_shield|home_shelter_exact",
        "white|opening|black_home|center_shield|pawn_cover_near_miss",
        "black|opening|white_home|center_shield|home_shelter_exact",
        "black|opening|white_home|center_shield|pawn_cover_near_miss",
        "white|opening|out_of_regime|out_of_regime|out_of_regime_negative",
        "white|middlegame|black_home|center_shield|home_shelter_exact"
      ),
      greenEngineProbeBuckets = Vector(
        "white|opening|black_home|wing_shield|pawn_cover_near_miss",
        "black|middlegame|white_home|wing_shield|home_shelter_exact",
        "white|opening|black_home|center_shield|home_shelter_exact",
        "white|opening|black_home|center_shield|pawn_cover_near_miss",
        "black|opening|white_home|center_shield|home_shelter_exact",
        "black|opening|white_home|center_shield|pawn_cover_near_miss",
        "white|opening|out_of_regime|out_of_regime|out_of_regime_negative",
        "white|middlegame|black_home|center_shield|home_shelter_exact"
      )
    ),
    SchemaId.SideToMove -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; side-to-move is literal FEN state",
      breadthBuckets = "state[white|black], board_context[opening|sparse]",
      minimumCorpusFloor = 4,
      status = "broad-confidence-green",
      exactBlocker = "none; white/black side-to-move states are closed across opening and sparse exact-board contexts",
      greenProofBuckets = Vector(
        "white|opening",
        "black|opening",
        "white|sparse",
        "black|sparse"
      )
    ),
    SchemaId.CastlingRights -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; castling rights are exact auxiliary state, not geometry inference",
      breadthBuckets = "rights_state[none|single_king|single_queen|same_color_both|cross_color|full_kqkq], color_scope[white|black|mixed|none], fail_closed_input[orphan_right|duplicate_right]",
      minimumCorpusFloor = 8,
      status = "broad-confidence-green",
      exactBlocker = "none; none, single-side, same-color, cross-color, full-rights, and duplicate/orphan fail-closed input buckets are closed",
      greenProofBuckets = Vector(
        "full_kqkq|mixed",
        "single_king|white",
        "none|none",
        "single_queen|white",
        "single_king|black",
        "same_color_both|white",
        "same_color_both|black",
        "cross_color|mixed"
      )
    ),
    SchemaId.EnPassantState -> AuditPolicy(
      riskClass = "deterministic / no-engine broad validation",
      enginePolicy = "no; legal en-passant availability is exact auxiliary state",
      breadthBuckets = "state[none|white_capture|black_capture], file_topology[edge|center], board_context[sparse|mixed], legality[real_capture|legal_none]",
      minimumCorpusFloor = 6,
      status = "broad-confidence-green",
      exactBlocker = "none; exact-state spread and floor are closed",
      greenProofBuckets = Vector(
        "white_capture|center|sparse|real_capture",
        "white_capture|edge|mixed|real_capture",
        "black_capture|edge|sparse|real_capture",
        "black_capture|center|mixed|real_capture",
        "none|edge|sparse|legal_none",
        "none|center|mixed|legal_none"
      )
    )
  )

  require(
    auditBySchema.keySet == all.map(_.id).toSet,
    s"Audit policy coverage mismatch: ${all.map(_.id).toSet.diff(auditBySchema.keySet)} / ${auditBySchema.keySet.diff(all.map(_.id).toSet)}"
  )

  def policyFor(schemaId: String): AuditPolicy =
    auditBySchema.getOrElse(schemaId, throw IllegalArgumentException(s"Missing root audit policy for $schemaId"))

  def greenProofBucketsFor(schemaId: String): Vector[String] =
    policyFor(schemaId).greenProofBuckets

  def greenEngineProbeBucketsFor(schemaId: String): Vector[String] =
    policyFor(schemaId).greenEngineProbeBuckets

  def render(rows: Vector[RootExpectationCorpus.Row] = RootExpectationCorpus.loadAll()): String =
    val rowsBySchema = rows.groupBy(_.schema)

    val body = all.map: schema =>
      val schemaRows = rowsBySchema.getOrElse(schema.id, Vector.empty)
      val policy = auditBySchema(schema.id)
      s"| `${schema.id}` | ${schemaRows.size} | ${escapeCell(formatCaseCoverage(schemaRows))} | ${escapeCell(policy.riskClass)} | ${escapeCell(policy.enginePolicy)} | ${escapeCell(policy.breadthBuckets)} | ${policy.minimumCorpusFloor} | ${escapeCell(policy.status)} | ${escapeCell(policy.exactBlocker)} |"

    val greenProofSection =
      val promoted = all.collect:
        case schema if auditBySchema(schema.id).greenProofBuckets.nonEmpty =>
          val policy = auditBySchema(schema.id)
          s"- `${schema.id}`: ${policy.greenProofBuckets.mkString("; ")}"
      if promoted.isEmpty then Nil
      else List("", "## Frozen Broad-Green / Sbt-Pending Proof Buckets", "") ++ promoted

    (
      List(
        "# Root Coverage Matrix",
        "",
        "`broad-confidence-green` is schema-local: the same narrow exact root truth must survive across many exact-board buckets, fail closed outside those buckets, and never borrow engine eval as the truth owner.",
        "If engine sanity appears below, it is a confound filter only; root truth still comes from the extractor's exact board law.",
        "",
        "| schema | current row count | current case-type coverage | risk class | engine sanity needed? why? | proposed breadth axes/buckets | minimum corpus floor | current status: broad-confidence-green / broad-confidence-green-candidate (sbt-pending) / close / thin / blocked | exact blocker |",
        "| --- | ---: | --- | --- | --- | --- | ---: | --- | --- |"
      ) ++ body ++ greenProofSection
    ).mkString("\n")

  def loadArtifact(): String =
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(resourcePath))
          .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath")),
        "UTF-8"
      )
    try source.mkString
    finally source.close()

  private def formatCaseCoverage(rows: Vector[RootExpectationCorpus.Row]): String =
    rows
      .groupMapReduce(_.caseType)(_ => 1)(_ + _)
      .toVector
      .sortBy(_._1)
      .map: (caseType, count) =>
        s"$caseType x$count"
      .mkString("; ")

  private def escapeCell(value: String): String =
    value.replace("|", "\\|")
