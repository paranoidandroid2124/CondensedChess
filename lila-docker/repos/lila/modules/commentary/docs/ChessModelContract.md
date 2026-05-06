# Chess Model Contract

This document is live authority for the chess-facing model shape and naming
rules on this branch.

## Naming Rules

Type and module names must read like chess, not project management.

New names are last-resort authority changes. Before adding a new core type,
module, row, or docs-authority name, ask whether the same chess meaning already has a home,
ask whether an existing Fact can carry the new field, ask whether the proposed
name creates a new authority, ask whether the same phenomenon would now have two
owners, and ask whether Story would later have two possible inputs to trust.

One chess meaning, one home. One observation family, one owner. One public
claim, one proof path.

Stage 2 ownership split:

- Story owns identity.
- StoryProof owns proof and missing evidence.
- Verdict carries the result.

`side`, `target`, `anchor`, `route`, and `rival` are Story identity. `legal
line`, `same-board proof`, and missing evidence are StoryProof evidence.
`rank`, `role`, `leadAllowed`, and selected strength are Verdict result.
StoryProof must not own or duplicate `side`, `target`, `anchor`, `route`, or `rival`.

Legal line binding is not tactical success proof. In Stage 2, legal line
binding proves only that the Story route is tied to a same-board legal path. It
does not prove that a move is good, wins material, succeeds tactically, stops
counterplay, controls a file, or opens public commentary.

StoryProof is only the minimum evidence form needed before a Story could speak.
`StoryInteractionLaw.md` owns the rule for when a named positive writer can
open a family before any public Lead is allowed.

`StoryInteractionLaw.md` owns the Stage 3 charter. This contract owns names and
shape: a positive Story needs a named `StoryWriter`, complete `StoryProof`, and
family-specific proof bound back to the same Story identity. `Tactic.Hanging`
is the only live writer in this checkpoint.

`StoryInteractionLaw.md` owns the Stage 4 charter. This contract owns names and
shape: Engine Check evidence is internal sidecar evidence for an existing
Story. It is not Story identity, not a Story writer, and not a Verdict result.

Renderer, LLM narration, public route `200`, `/api/commentary/render`, and
`/internal/commentary/render-local-probe` remain closed until their own
contracts and tests exist.

proofFailures are internal diagnostics only.
proofFailures are not public payload.
proofFailures are not renderer input.
proofFailures are not LLM input.
Missing evidence text must not become user commentary.

Before adding any new type, module, row, or field, classify the information as
Story identity, StoryProof evidence, or Verdict result. If that classification
is unclear, do not add the name.

Allowed core names:

- `BoardMood`
- `Square`
- `Man`
- `Piece`
- `Line`
- `BoardFacts`
- `LegalMove`
- `Attack`
- `Guard`
- `PieceContact`
- `LineKind`
- `LineShape`
- `LineFact`
- `PawnLever`
- `PawnChallenge`
- `PawnCannotChallengeSquare`
- `PawnSafeSquareObservation`
- `NoCurrentPawnChase`
- `FrontBlocker`
- `PassedPawnObservation`
- `IsolatedPawnObservation`
- `BackwardPawnFrontSquare`
- `PieceReachableSquare`
- `SquareGuardMap`
- `FileState`
- `FileFact`
- `KingSquare`
- `KingRingSquare`
- `KingRingAttack`
- `KingRingDefender`
- `LegalEscapeSquare`
- `ContactCheckObservation`
- `MissingEvidence`
- `Heat`
- `KingHeat`
- `Tactics`
- `Pawns`
- `Pieces`
- `Material`
- `Wound`
- `Opening`
- `Story`
- `StoryProof`
- `CaptureResult`
- `EngineEval`
- `EngineLine`
- `EngineCheck`
- `EngineCheckStatus`
- `StoryWriter`
- `TacticHanging`
- `Verdict`
- `StoryTable`

Forbidden in new core model names:

- `V1`, `V2`, or other version suffixes
- `Semantic`
- `Candidate`
- `Certification`
- `Object`
- `Delta`
- `InteractionLayer`
- `Selector`
- `Pipeline`
- `Gate`
- `ScoreVector`

The `Candidate` ban applies to new core model, type, and module names. Pawn
feature names `white_candidate_passer_count`, `black_candidate_passer_count`,
and the schema term `candidate_passer` are allowed because candidate passer is a
chess pawn-structure term. Legacy candidate line selectors, candidate scoring,
and candidate pipelines remain forbidden as model/type/module names and
authority paths. Non-passer candidate-shaped scalar names were removed in this
stage: pawn breaks use `white_break_chance_count` and
`black_break_chance_count`, while tactical checks use
`white_check_threat_count` and `black_check_threat_count`.

Version and shape information belongs in companion constants, not type names.

```scala
object BoardMood:
  val Schema = 1
  val Bits = 48
  val Scalars = 256
  val Size = Bits * 64 + Scalars
```

## Board Shape

`BoardMood` is the dense board-facing vector. It carries exact root-state
transport, interpretable scalar summaries, and public-proof diagnostics. No
public chess story is allowed to bypass it, but `BoardMood` is not public proof
authority by itself.

Fixed shape:

- `48` bit slots
- `256` scalar slots
- total size: `3,328`

There is no `BoardMood` expansion beyond this shape in the current checkpoint.
Split/cut re-entry requires a named law and same-board producer proof; otherwise
the old slot remains `0`/silent.

Runtime input boundary:

- `BoardFacts` -> `BoardMood.fromFacts` is the runtime input boundary.
- `BoardFacts.md` is the Stage 1 charter for board observations. A
  chess-readable Board Fact name such as open file, pin, weak square, loose
  piece, or pawn lever does not create public claim authority.
- `BoardFacts.seen` is the Stage 1 runtime observation surface. It may expose
  legal moves, primitive attacks, primitive guards, `PieceContact` rows,
  `FileFact` rows, `LineFact` rows, pawn levers, pawn/square observations,
  king-ring observations, legal king moves, contact-check observations, and
  missing-evidence rows. `PieceContact` consolidates the former derived
  piece-contact rows; `FileFact` consolidates the former file rows; `LineFact`
  consolidates ordinary line/ray geometry plus non-public pin-to-king and
  king-line geometry. These rows are observations only and do not write Story
  proof, renderer payload, route binding, plan quality, file control, invasion,
  tactic proof, unsafe-king proof, mate-net proof, or public claim pressure.
- `BoardFacts.fromFen` is the strict root transport entrypoint. It accepts a
  `Fen.Full` or raw FEN string, validates through
  `RootExtractor.fromFenWithPositionFailClosed`, and returns
  `Either[String, BoardFacts]`.
  Invalid FEN, illegal standard positions, mismatched castling rights, and
  mismatched en-passant fields return `Left`; they do not produce
  `BoardMood.empty` fallbacks.
- `BoardFacts.fromPosition` is an internal/test boundary only. It must run the
  same strict position validation that does not depend on raw FEN fields, derive
  its own root state from the supplied position, and require an explicit
  positive fullmove number supplied by the internal caller. It is not raw-FEN
  castling or en-passant mismatch validation.
- Runtime BoardFacts factories must not accept caller-supplied root, legal,
  material, control, or pawn facts. Strict same-board producers record the
  factory-created `BoardFacts` instance identity as ready; constructor
  parameters do not carry readiness authority.
- Manual `BoardFacts` assembly remains only for contract tests and fail-closed
  boundary checks. It is untrusted by definition and must keep S015
  `position_ready` at `0` even when every nested fact is `known && sane`.
- `BoardFacts` must not expose case-class `copy` or product reconstruction;
  reflective construction and caller-supplied fields must not create readiness.
- BoardFacts required fields are `root`, `sideToMove`, `header`, `sideLegal`,
  `rivalLegal`, `control`, `material`, and `pawns`. Callers must provide them
  explicitly; default construction is not a runtime contract.
- Nested BoardFacts facts must be marked `known = true` before they can
  contribute to readiness. Default constructors create incomplete nested facts
  even when their numeric fields are zero.
- `sideToMove` must be `White` or `Black`.
- S015 `position_ready` may be `1` only when all nested facts are known and sane:
  `header`, `sideLegal`, `rivalLegal`, `control`, `material`, and `pawns`.
  It also requires factory-created instance identity recorded by strict
  producers such as `BoardFacts.fromFen` or `BoardFacts.fromPosition`; reflective
  construction of matching fields remains unready.
- Legal move sidecars are known facts, not positive-count facts. A known
  zero-legal-move sidecar remains valid for mate and stalemate. If `moveCount`
  is positive, the sidecar must carry a non-zero legal destination summary or at
  least one legal line destination. Move, capture, and check counts must be
  non-negative, and capture/check counts must not exceed `moveCount`.
- `Moves.lines` carries all legal `(from, to)` pairs as same-board diagnostic
  summaries. Castling stores only the king route, promotion stores no promotion
  role, and en-passant stores no captured-pawn square at this stage.
- `rivalLegal` is a required known legal sidecar. Missing rival legal facts must
  keep S015 `position_ready` at `0`.
- `rivalLegal` is computed by viewing the same exact board with the opposite
  side to move. It is a board-interaction diagnostic, not timeline proof.
- `BoardMood.fromPieces` is scaffold-only and not runtime authority. It may
  transport local piece-on atoms and debug summaries, but it must not mark a
  position ready for runtime use.

Bit contract:

- B00..B45 are packed `RootStateVector` transport words, not ordinary 64-square
  bitboards.
- B00..B45 are not ordinary 64-square bitboards.
- Decode rule: bit `k` of B`n` decodes to root atom index `64*n+k`, valid only
  when that index is `< RootAtomRegistry.RootSize`.
- B45 stores root atom indices `2880..2890`; bits above index `2890` are
  canonical padding and must be zero.
- B46 is `side_legal_destination_union`.
- B47 is `rival_legal_destination_union`.

Low-level scalar construction:

- `BoardMood.fromPacked`, `BoardMood.fromParts`, and `BoardMood.fromRoot`
  accept dense scalar transport for live diagnostics only.
- These low-level constructors canonicalize all closed scalar slots to `0` on
  input. Closed means `BoardMoodSplitLaw.md`, `BoardMoodCutLaw.md`, and
  S224..S255 proof/pressure slots.
- Callers cannot inject broad scores, cut meanings, exact-board binding, legal
  replay binding, source, evidence, render safety, proof counts, or pressure
  values through scalar transport.
- `BoardMood` must not expose case-class `copy` or product reconstruction.
  Raw construction, including reflective construction, must still canonicalize
  all closed scalar slots to `0`.
- A closed scalar may re-enter only as a named exact chess fact or proof writer
  admitted in this contract and the appropriate BoardMood law document before
  runtime use.
- At this checkpoint no `BoardMood` Sxxx re-entry or proof writer is admitted.
  Closed, cut, split, proof, source, and pressure slots stay `0`/silent without
  a named law and same-board producer proof.
- Cut BoardMood meanings may be spoken only by Story under
  `StoryResurrectionLaw.md`; BoardMood remains `0`/silent for those slots.

B46 and B47 are legal destination summaries, not proof.

B46 and B47 are summary legal-destination masks only. They do not prove origin
legality, route legality, castling legality, en-passant legality, promotion
legality, tactic legality, forcing lines, or public claim legality. Public
claims require LegalMove, Ray, LineProof, and Source sidecars; those sidecars
must remain bound to the same root state.

Route legality, castling legality, en-passant legality, promotion legality, and
tactic legality must come from legal replay sidecars, not from BoardMood masks.

Scalar contract:

- S000 `side_to_move`
- S001 `ply_from_start`
- S002 `phase_total`
- S003 `phase_non_pawn`
- S004 `halfmove_clock`
- S005 `fullmove_number`
- S006 `castling_mask`
- S007 `ep_square_plus_one`
- S008 `in_check_mask`
- S009 `legal_move_count`
- S010 `legal_capture_count`
- S011 `legal_check_count`
- S012 `snapshot_ply`
- S013 `board_hash_lo`
- S014 `board_hash_hi`
- S015 `position_ready`
- S016 `white_pawn_count`
- S017 `white_knight_count`
- S018 `white_bishop_count`
- S019 `white_rook_count`
- S020 `white_queen_count`
- S021 `white_king_count`
- S022 `black_pawn_count`
- S023 `black_knight_count`
- S024 `black_bishop_count`
- S025 `black_rook_count`
- S026 `black_queen_count`
- S027 `black_king_count`
- S028 `white_material`
- S029 `black_material`
- S030 `material_diff`
- S031 `material_imbalance`
- S032 `white_pawn_mobility`
- S033 `white_knight_mobility`
- S034 `white_bishop_mobility`
- S035 `white_rook_mobility`
- S036 `white_queen_mobility`
- S037 `white_king_mobility`
- S038 `black_pawn_mobility`
- S039 `black_knight_mobility`
- S040 `black_bishop_mobility`
- S041 `black_rook_mobility`
- S042 `black_queen_mobility`
- S043 `black_king_mobility`
- S044 `white_pawn_safe_mobility`
- S045 `white_knight_safe_mobility`
- S046 `white_bishop_safe_mobility`
- S047 `white_rook_safe_mobility`
- S048 `white_queen_safe_mobility`
- S049 `white_king_safe_mobility`
- S050 `black_pawn_safe_mobility`
- S051 `black_knight_safe_mobility`
- S052 `black_bishop_safe_mobility`
- S053 `black_rook_safe_mobility`
- S054 `black_queen_safe_mobility`
- S055 `black_king_safe_mobility`
- S056 `white_space`
- S057 `black_space`
- S058 `space_diff`
- S059 `white_controlled_squares`
- S060 `black_controlled_squares`
- S061 `contested_squares`
- S062 `white_attacked_twice`
- S063 `black_attacked_twice`
- S064 `white_king_square`
- S065 `white_king_ring_squares`
- S066 `white_king_ring_enemy_attacks`
- S067 `white_king_ring_friendly_defenders`
- S068 `white_safe_escape_count`
- S069 `white_legal_check_count_against`
- S070 `white_shelter_holes`
- S071 `white_storm_pawns_against`
- S072 `white_open_file_exposure`
- S073 `white_diagonal_exposure`
- S074 `white_rook_queen_line_exposure`
- S075 `white_pinned_defender_count`
- S076 `white_overloaded_defender_count`
- S077 `white_contact_check_threats`
- S078 `white_mate_net_pressure`
- S079 `white_king_heat`
- S080 `black_king_square`
- S081 `black_king_ring_squares`
- S082 `black_king_ring_enemy_attacks`
- S083 `black_king_ring_friendly_defenders`
- S084 `black_safe_escape_count`
- S085 `black_legal_check_count_against`
- S086 `black_shelter_holes`
- S087 `black_storm_pawns_against`
- S088 `black_open_file_exposure`
- S089 `black_diagonal_exposure`
- S090 `black_rook_queen_line_exposure`
- S091 `black_pinned_defender_count`
- S092 `black_overloaded_defender_count`
- S093 `black_contact_check_threats`
- S094 `black_mate_net_pressure`
- S095 `black_king_heat`
- S096 `white_pawn_file_counts`: packed 8-file pawn counts, one file count per
  nibble from a-file through h-file.
- S097 `white_isolated_pawn_count`
- S098 `white_backward_pawn_count`
- S099 `white_doubled_file_count`
- S100 `white_passed_pawn_count`
- S101 `white_candidate_passer_count`
- S102 `white_protected_passer_count`
- S103 `white_fixed_pawn_count`
- S104 `white_chain_base_count`
- S105 `white_lever_count`
- S106 `white_break_chance_count`
- S107 `white_blockaded_pawn_count`
- S108 `white_promotion_distance_best`
- S109 `white_pawn_support`
- S110 `white_pawn_risk`
- S111 `white_pawn_structure_score`
- S112 `black_pawn_file_counts`: packed 8-file pawn counts, one file count per
  nibble from a-file through h-file.
- S113 `black_isolated_pawn_count`
- S114 `black_backward_pawn_count`
- S115 `black_doubled_file_count`
- S116 `black_passed_pawn_count`
- S117 `black_candidate_passer_count`
- S118 `black_protected_passer_count`
- S119 `black_fixed_pawn_count`
- S120 `black_chain_base_count`
- S121 `black_lever_count`
- S122 `black_break_chance_count`
- S123 `black_blockaded_pawn_count`
- S124 `black_promotion_distance_best`
- S125 `black_pawn_support`
- S126 `black_pawn_risk`
- S127 `black_pawn_structure_score`
- S128 `white_minor_activity`
- S129 `white_rook_activity`
- S130 `white_queen_activity`
- S131 `white_piece_mobility_score`
- S132 `white_outpost_count`
- S133 `white_bad_bishop_count`
- S134 `white_rook_open_file_count`
- S135 `white_rook_seventh_count`
- S136 `white_loose_piece_count`
- S137 `white_hanging_piece_count`
- S138 `white_pinned_piece_count`
- S139 `white_overloaded_piece_count`
- S140 `white_trapped_piece_count`
- S141 `white_xray_target_count`
- S142 `white_route_clarity`
- S143 `white_piece_coordination`
- S144 `black_minor_activity`
- S145 `black_rook_activity`
- S146 `black_queen_activity`
- S147 `black_piece_mobility_score`
- S148 `black_outpost_count`
- S149 `black_bad_bishop_count`
- S150 `black_rook_open_file_count`
- S151 `black_rook_seventh_count`
- S152 `black_loose_piece_count`
- S153 `black_hanging_piece_count`
- S154 `black_pinned_piece_count`
- S155 `black_overloaded_piece_count`
- S156 `black_trapped_piece_count`
- S157 `black_xray_target_count`
- S158 `black_route_clarity`
- S159 `black_piece_coordination`
- S160 `white_forcing_move_count`
- S161 `white_check_threat_count`
- S162 `white_capture_threat_count`
- S163 `white_see_best_gain`
- S164 `white_conversion_prize`
- S165 `white_fork_motif_count`
- S166 `white_pin_motif_count`
- S167 `white_skewer_motif_count`
- S168 `white_xray_discovery_count`
- S169 `white_overload_deflect_count`
- S170 `white_remove_guard_count`
- S171 `white_back_rank_pressure`
- S172 `white_promotion_threat`
- S173 `white_defender_shortage`
- S174 `white_counterplay`
- S175 `white_tactical_score`
- S176 `black_forcing_move_count`
- S177 `black_check_threat_count`
- S178 `black_capture_threat_count`
- S179 `black_see_best_gain`
- S180 `black_conversion_prize`
- S181 `black_fork_motif_count`
- S182 `black_pin_motif_count`
- S183 `black_skewer_motif_count`
- S184 `black_xray_discovery_count`
- S185 `black_overload_deflect_count`
- S186 `black_remove_guard_count`
- S187 `black_back_rank_pressure`
- S188 `black_promotion_threat`
- S189 `black_defender_shortage`
- S190 `black_counterplay`
- S191 `black_tactical_score`
- S192 `plan_minority`
- S193 `plan_majority`
- S194 `plan_center_break`
- S195 `plan_flank_break`
- S196 `plan_storm`
- S197 `plan_expansion`
- S198 `plan_cramp`
- S199 `plan_outpost`
- S200 `plan_bad_piece`
- S201 `plan_reroute`
- S202 `plan_bishops`
- S203 `plan_blockade`
- S204 `plan_open_file`
- S205 `plan_seventh`
- S206 `plan_color_bind`
- S207 `plan_weak_square`
- S208 `plan_isolani`
- S209 `plan_backward_pawn`
- S210 `plan_hanging_pawns`
- S211 `plan_chain_base`
- S212 `plan_passer_make`
- S213 `plan_passer_block`
- S214 `plan_race`
- S215 `plan_trade`
- S216 `plan_simplify`
- S217 `plan_keep_pieces`
- S218 `plan_overload`
- S219 `plan_prophy`
- S220 `plan_counterplay`
- S221 `plan_initiative`
- S222 `plan_king_convert`
- S223 `plan_convert`
- S224 `exact_board_binding`
- S225 `legal_replay_binding`
- S226 `owner_binding`
- S227 `anchor_binding`
- S228 `route_binding`
- S229 `same_root_certificate`
- S230 `engine_depth`
- S231 `engine_freshness_ply`
- S232 `engine_eval_stability`
- S233 `eval_delta_cp`
- S234 `source_fit`
- S235 `source_public_safety`
- S236 `opening_line_fit`
- S237 `novelty_signal`
- S238 `stale_or_forbidden`
- S239 `render_safe`
- S240 `ray_count`
- S241 `line_proof_count`
- S242 `source_count`
- S243 `evidence_count`
- S244 `side_piece_pressure`
- S245 `rival_piece_pressure`
- S246 `side_pawn_pressure`
- S247 `rival_pawn_pressure`
- S248 `side_king_pressure`
- S249 `rival_king_pressure`
- S250 `side_plan_pressure`
- S251 `rival_plan_pressure`
- S252 `side_tactic_pressure`
- S253 `rival_tactic_pressure`
- S254 `board_story_pressure`
- S255 `public_claim_pressure`

Binding and proof slots remain zero. The current root transport baseline writes
board facts, legal destination summaries, material, control summaries, and pawn
facts; it does not create public proof authority.
S224..S255 remain zero from `BoardFacts.fromFen` through `BoardMood.fromFacts`
and through low-level scalar constructors. Nonzero proof, binding, source, and
pressure values require named writers admitted here and exposed through a
dedicated API; they are not placeholders.

Root transport control facts use the same geometric attack semantics as
`RootExtractor`, not legal-attack proof. Pinned pieces still contribute
geometric attacks, control, and diagnostic mobility. Legal replay sidecars
remain the authority for actual legal moves.

Root transport pawn facts populate file counts, isolated pawns, backward pawns,
doubled files, passed pawns, candidate passers, fixed pawns, lever counts, and
best promotion distance from the same exact board/root. Protected passers,
chain bases, break chances, blockaded pawns, support, risk, and structure are
closed by `BoardMoodSplitLaw.md` or `BoardMoodCutLaw.md` and stay `0` in live
BoardMood.

## Phase 2 Position / Material Completeness

S000..S031 are complete for strict `BoardFacts.fromFen` runtime input:

- S000 `side_to_move`: `White.ordinal` or `Black.ordinal` from the validated
  position color.
- S001 `ply_from_start`: `(fullmove_number - 1) * 2 + 1` when black is to move,
  otherwise `(fullmove_number - 1) * 2`.
- S002 `phase_total`: sum across both sides using `N/B=1`, `R=2`, `Q=4`,
  `P/K=0`.
- S003 `phase_non_pawn`: count of all non-pawn, non-king pieces.
- S004 `halfmove_clock`: the non-negative FEN halfmove clock.
- S005 `fullmove_number`: the positive FEN fullmove number.
- S006 `castling_mask`: bitmask `white king-side=1`, `white queen-side=2`,
  `black king-side=4`, `black queen-side=8`, after strict castling validation.
- S007 `ep_square_plus_one`: `0` when absent; otherwise validated en-passant
  square index plus one.
- S008 `in_check_mask`: bitmask `white=1`, `black=2`, from exact-board check
  detection.
- S009 `legal_move_count`: count of side-to-move legal moves from legal replay.
- S010 `legal_capture_count`: count of legal moves whose replayed move is a
  capture or en-passant capture.
- S011 `legal_check_count`: count of legal moves whose replayed after-position
  gives check.
- S012 `snapshot_ply`: equal to S001 for this phase.
- S013 `board_hash_lo` and S014 `board_hash_hi`: cut BoardMood chess meanings.
  They must be `0` for S015 readiness and cannot support a public chess
  sentence.
- S015 `position_ready`: `1` only when root transport is non-empty and all
  nested facts are `known && sane`; zero-baseline hash and material-imbalance
  fields must still be zero.
- S016..S027 piece counts: exact count by side and role in order pawn, knight,
  bishop, rook, queen, king.
- S028 `white_material` and S029 `black_material`: material value in centipawns
  using `P=100`, `N=320`, `B=330`, `R=500`, `Q=900`, `K=0`.
- S030 `material_diff`: `white_material - black_material`.
- S031 `material_imbalance`: deterministic zero-baseline in this phase. It
  remains `0` and must be `0` for S015 readiness. It is closed by
  `BoardMoodSplitLaw.md` unless replaced by smaller exact material facts.

## Phase 3 BoardMood Mobility / Control / Space Completeness

S032..S063 are complete for strict `BoardFacts.fromFen` and internal
`BoardFacts.fromPosition` runtime input:

- S032..S043 role mobility: per-side, per-role pseudo/geometric destination
  counts from the exact board. Sliding pieces use current blockers and include
  the first blocker square, then same-color occupied destinations are removed.
  Knights and kings use geometric destination masks with same-color occupied
  destinations removed. Pawns count one-square forward non-capture pushes when
  the forward square is empty, plus diagonal attack destinations that are not
  occupied by a same-color piece. Double pawn pushes are not counted in this
  phase. These slots are diagnostics, not legal self-check proof.
- S044..S055 safe mobility: the same per-side, per-role mobility destinations
  filtered to destinations not controlled by the opponent under the same
  geometric `RootExtractor` attack semantics. Safe mobility is not legal move
  proof and does not certify king safety under replay.
- S056 `white_space`: count of empty squares on files c through f and ranks 4
  through 6 that are controlled by White and not controlled by Black.
- S057 `black_space`: count of empty squares on files c through f and ranks 3
  through 5 that are controlled by Black and not controlled by White.
- S058 `space_diff`: `white_space - black_space`.
- S059 `white_controlled_squares` and S060 `black_controlled_squares`: popcount
  of each side's same-board geometric attack/control union, aligned with
  `RootExtractor`.
- S061 `contested_squares`: popcount of the intersection of the two geometric
  control unions.
- S062 `white_attacked_twice` and S063 `black_attacked_twice`: count of squares
  attacked by at least two same-side geometric attack masks.

S032..S063 may be nonzero only as board diagnostics. They do not set any
S224..S255 proof, binding, source, or pressure slot, and they do not grant
public commentary authority by themselves.

`BoardMood.fromPieces(pieces, side)` is a local scaffold over root `piece_on`
atoms and basic scalar summaries. It is not an authoritative piece-map or heat
map layout, it is not runtime backend integration, and it is not allowed to set
S015 `position_ready`.

## Story Shape

`Story` is the unit that may compete for commentary. Lower facts, source rows,
engine numbers, pins, and legacy claim carriers are not public stories by
themselves.

Fixed shape:

- story size: `160`
- verdict size: `96`
- top stories: `8`

Story slots:

- `16` scene slots
- `32` plan slots
- `24` tactic slots
- `16` pawn slots
- `16` piece slots
- `16` king slots
- `8` opening slots
- `32` proof slots

Each story carries compact chess identity:

- `side`: `White`, `Black`, `Both`, or `None`
- `target`: optional `Square`
- `anchor`: optional `Square`
- `route`: optional ordered `Line`
- `rival`: `White`, `Black`, `Both`, or `None`

`Story.values` does not hash identity strings. The first six pawn-family slots
store typed identity:

- `0`: side ordinal
- `1`: rival ordinal
- `2`: target square index + 1, or `0` absent
- `3`: anchor square index + 1, or `0` absent
- `4`: route-from square index + 1, or `0` absent
- `5`: route-to square index + 1, or `0` absent

Square scalar slots store square index + 1; `0` means absent. Producers must not
encode A1 as `0`.

Proof values occupy the first `16` proof slots.

Each story carries these proof scores as `0..100` integers:

- `boardProof`
- `lineProof`
- `ownerProof`
- `anchorProof`
- `routeProof`
- `persistence`
- `immediacy`
- `forcing`
- `conversionPrize`
- `counterplayRisk`
- `kingHeat`
- `pieceSupport`
- `pawnSupport`
- `sourceFit`
- `novelty`
- `clarity`

Lead fail-closed rules:

- No Story may lead at the public surface without concrete side, target, anchor,
  route, rival, required legal line, and same-root proof sidecar.
- `StoryProof` is the Stage 2 sidecar that records the required legal line,
  same-board proof presence, and missing evidence for the existing `Story`
  identity fields. It does not duplicate side, target, anchor, route, or rival
  ownership.
- `CaptureResult` is internal family proof for `Tactic.Hanging`. It records the
  capture side, capturing piece, target piece, capture line, captured value,
  recapture candidates, material result after bounded recapture check,
  same-board proof presence, and missing evidence. These coordinates are proof
  echoes only: `Story` remains the home for side, target, anchor, route, and
  rival, and `StoryTable` must reject any positive writer whose
  `CaptureResult` does not bind back to that Story identity.
- EngineCheck is internal evidence only. It records same-board proof, checked move, engine line, reply line, eval before, eval after, depth or freshness, and missing evidence. EngineCheck does not create a Story, select a Story, rank a Story, write a Verdict, feed a renderer, or feed an LLM. EngineLine carries only move lines. EngineEval carries only internal centipawn numbers.
- EngineCheck.fromStory binds engine evidence to same-board BoardFacts, an existing Story route, and a same-board legal line. Wrong-board facts, route-mismatched engine lines, stale engine data, depth-missing engine data, eval-only input without a Story, and PV-only input without a Story leave missing evidence and stay diagnostic only.
- EngineCheckStatus has exactly `Unknown`, `Supports`, `Caps`, and `Refutes`. Only `Tactic.Hanging` may carry EngineCheck in this checkpoint. `Refutes` blocks the Hanging Story. `Supports` and `Caps` do not change `Verdict.values`, create public truth, or create winning, best-move, decisive, PV-explanation, or public-eval claims.
- Eval collapse after capture may refute an existing Hanging EngineCheck only after same-board Story proof, named writer, CaptureResult, legal route, and freshness guards pass. Eval collapse cannot create a Story, public eval claim, or engine-authored explanation.
- Verdict carries `engineCheckStatus` and `engineStrengthLimited` as internal diagnostics only. `Verdict.values`, renderer, and LLM inputs must not consume EngineCheck diagnostics.
- `ownerProof >= 70` requires `side` to be `White`, `Black`, or `Both`; `None`
  cannot lead.
- `anchorProof >= 70` requires a concrete `anchor`.
- `routeProof >= 70` requires a concrete `route`.
- `lineProof = 0` blocks tactical and line-backed stories from leading.
- `Scene.Tactic` requires a concrete tactic motif.
- `Proof.truth` includes `lineProof`, so a missing line proof can cap public
  strength.

BoardMood carries proof summaries, not public proof authority by itself. A
numeric `Proof` score is forgeable unless side, target, anchor, route, rival,
required legal line, and same-root proof sidecars are bound to the same root
state.

Only the named `Tactic.Hanging` positive `Story` writer is live in this
checkpoint. Numeric `Proof` scores may rank blocked/context `Verdict` rows
only; they cannot set `leadAllowed=true` or produce `Role.Lead` unless the
Story has the named `Tactic.Hanging` writer, complete StoryProof, same-board
proof, and positive `CaptureResult`.

Missing side, target, anchor, route, rival, required legal line, or same-root
proof sidecar is a hard public-output block, not weak scoring, deferred work,
or renderer repair.

## Verdict Layout

`Verdict.values` has exactly `96` slots:

- `0`: role code
- `1`: rank
- `2`: lead-allowed bit
- `3`: strength
- `4`: side ordinal
- `5`: rival ordinal
- `6`: target square index + 1, or `0` absent
- `7`: anchor square index + 1, or `0` absent
- `8..23`: scene one-hot
- `24..55`: plan one-hot
- `56..79`: tactic one-hot
- `80..95`: all `16` proof values

Route identity remains in `Verdict.story.route` and in `Story.values`; it is not
duplicated in the `96`-slot verdict vector.

## Scenes

The model has exactly `16` public scene families:

- `Tactic`
- `Blunder`
- `Material`
- `King`
- `Defense`
- `Opening`
- `Pawns`
- `Plan`
- `Pieces`
- `Space`
- `Initiative`
- `Convert`
- `Endgame`
- `Counterplay`
- `Source`
- `Quiet`

## Plans

The model has exactly `32` long-term plan families:

- `Minority`
- `Majority`
- `CenterBreak`
- `FlankBreak`
- `Storm`
- `Expansion`
- `Cramp`
- `Outpost`
- `BadPiece`
- `Reroute`
- `Bishops`
- `Blockade`
- `OpenFile`
- `Seventh`
- `ColorBind`
- `WeakSquare`
- `Isolani`
- `BackwardPawn`
- `HangingPawns`
- `ChainBase`
- `PasserMake`
- `PasserBlock`
- `Race`
- `Trade`
- `Simplify`
- `KeepPieces`
- `Overload`
- `Prophy`
- `Counterplay`
- `Initiative`
- `KingConvert`
- `Convert`

## Tactics

The model has exactly `24` tactic families:

- `Loose`
- `Hanging`
- `AbsPin`
- `RelPin`
- `Skewer`
- `Xray`
- `Fork`
- `Discover`
- `RemoveGuard`
- `Overload`
- `BackRank`
- `MateNet`
- `SafeCheck`
- `PawnFork`
- `PawnPush`
- `Trap`
- `QueenHit`
- `KingOpen`
- `Promote`
- `InBetween`
- `Clear`
- `Decoy`
- `Deflect`
- `Tempo`

## Story Strength

The first deterministic scoring contract is fixed.

```text
truth = min(boardProof, lineProof, ownerProof, anchorProof, routeProof)

tacticHeat =
  0.30 * forcing +
  0.25 * conversionPrize +
  0.20 * kingHeat +
  0.15 * lineProof +
  0.10 * immediacy

planHeat =
  0.28 * persistence +
  0.20 * routeProof +
  0.16 * conversionPrize +
  0.14 * pieceSupport +
  0.12 * pawnSupport +
  0.10 * clarity -
  0.22 * counterplayRisk

publicStrength = min(truth, max(tacticHeat, planHeat))
```

Lead requirements:

- `publicStrength >= 65`
- `truth >= 70`
- `counterplayRisk <= 70`
- `Quiet` only if no story has `publicStrength >= 55`
- `Tactic` outranks `Plan` if `tacticHeat >= 70` and `lineProof >= 65`
- `Plan` can lead only if no opposing `Tactic` or `Blunder` story is at least `70`
- `Source` and `Opening` never lead over a board-backed story at least `55`
- a pin can lead only through `Tactics`, never from line weather alone

## Legacy Deletion Rule

Retired pre-reset carrier stacks such as legacy root/intermediate lanes,
certificates, broad source wrappers, engine wrappers, and projected claim
paths are deletion targets, not proof sources. They do not supply public proof
authority by surviving in code or documentation.

These are deletion targets:

- renderer-facing public claim paths
- source-only opening truth owners
- raw-engine truth owners
- special cases that bypass `StoryTable`
- legacy carriers that cannot fill board, story, or proof slots
