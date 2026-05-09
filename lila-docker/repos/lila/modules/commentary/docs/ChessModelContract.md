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

`side`, `target`, `secondaryTarget`, `anchor`, `route`, and `rival` are Story
identity. `legal line`, `same-board proof`, and missing evidence are StoryProof evidence.
`rank`, `role`, `leadAllowed`, and selected strength are Verdict result.
StoryProof must not own or duplicate `side`, `target`, `secondaryTarget`,
`anchor`, `route`, or `rival`.

Legal line binding is not tactical success proof. In Stage 2, legal line
binding proves only that the Story route is tied to a same-board legal path. It
does not prove that a move is good, wins material, succeeds tactically, stops
counterplay, controls a file, or opens public commentary.

StoryProof is only the minimum evidence form needed before a Story could speak.
`StoryInteractionLaw.md` owns the rule for when a named positive writer can
open a family before any public Lead is allowed.

`StoryInteractionLaw.md` owns the Stage 3 charter. This contract owns names and
shape: a positive Story needs a named `StoryWriter`, complete `StoryProof`, and
family-specific proof bound back to the same Story identity. `Tactic.Hanging`,
the narrow `Tactic.Fork` vertical slice, and the narrow `Scene.Material`
writer are the only live writers in this checkpoint.

`StoryInteractionLaw.md` owns the Stage 4 charter. This contract owns names and
shape: Engine Check evidence is internal sidecar evidence for an existing
Story. It is not Story identity, not a Story writer, and not a Verdict result.

`StoryInteractionLaw.md` owns the Stage 5 charter. This contract owns names and
shape: StoryTable assigns roles among existing Story rows and Verdict
records the result. StoryTable does not create Story identity or proof.
Stage 5-1 Hanging Role Rules are also owned by `StoryInteractionLaw.md`; this
contract records only the shape boundary.
Stage 5-2 Deterministic Ordering is also owned by `StoryInteractionLaw.md`;
this contract records only the Verdict ordering shape boundary.
Stage 5-3 Conflict and Block Rules are also owned by `StoryInteractionLaw.md`;
this contract records only the close-blocker shape boundary.
Stage 5-4 Verdict Diagnostic Boundary is also owned by `StoryInteractionLaw.md`;
this contract records only the Verdict diagnostic shape boundary.
Stage 5 closeout is also owned by `StoryInteractionLaw.md`; this contract
records only the selected-Verdict handoff boundary.
`StoryInteractionLaw.md` owns the Stage 6 charter. This contract records only
the selected-Verdict input boundary for Explanation Plan data. Stage 6-0
admits no new core type, row, Story family, renderer input, LLM input, public
route payload, engine explanation, or raw evidence handoff.
Stage 6-1 Explanation Plan Shape is also owned by `StoryInteractionLaw.md`;
this contract records only the internal shape names.
Stage 6-2 Tactic.Hanging Allowed Claim Mapping is also owned by
`StoryInteractionLaw.md`; this contract records only the claim-key boundary.
Stage 6-3 Forbidden Wording Boundary is also owned by `StoryInteractionLaw.md`;
this contract records only the forbidden-wording boundary.
Stage 6-4 Support / Context Relation is also owned by `StoryInteractionLaw.md`;
this contract records only the relation-key boundary.
Stage 6-5 Selected Verdict Only Guard is also owned by
`StoryInteractionLaw.md`; this contract records only the selected-Verdict input
guard.
Stage 6 closeout is also owned by `StoryInteractionLaw.md`; this contract
records only that Explanation Plan closes without renderer, LLM, public route,
pedagogy, or duplicated evidence/selection authority.
Fork-7 ExplanationPlan for Fork is also owned by `StoryInteractionLaw.md`;
this contract records only the selected-Verdict Fork claim-key boundary.
Fork-8 Deterministic Renderer for Fork is also owned by
`StoryInteractionLaw.md`; this contract records only the Fork
`ExplanationPlan` template boundary and the no-public-route boundary.
Fork-9 LLM Smoke for Fork is also owned by `StoryInteractionLaw.md`; this
contract records only the Fork smoke prompt boundary and the no-production-API,
no-public-route boundary.
Fork slice closeout is also owned by `StoryInteractionLaw.md`; this contract
records only that the closeout opens no sibling tactic, scene, plan, strategy,
public route, production API, or new public claim.
Stage 7-0 Deterministic Renderer Charter is also owned by
`StoryInteractionLaw.md`; this contract records only the `ExplanationPlan`
only renderer input boundary and the no-LLM, no-public-route boundary.
Stage 7-1 Renderer Input Guard is also owned by `StoryInteractionLaw.md`;
this contract records only that renderer entrypoints accept `ExplanationPlan`
and no raw proof material.
Stage 7-2 Minimal Tactic.Hanging Template is also owned by
`StoryInteractionLaw.md`; this contract records only that `can_win_piece`
maps to the first deterministic text under the ExplanationPlan boundary.
Stage 7-3 Forbidden Wording Enforcement is also owned by
`StoryInteractionLaw.md`; this contract records only that renderer output must
not violate `ExplanationPlan.forbiddenWording`.
Stage 7-4 No Text for Support / Context / Blocked is also owned by
`StoryInteractionLaw.md`; this contract records only that non-Lead and no-claim
plans produce no standalone text.
Stage 7-5 Rendered Line Shape is also owned by `StoryInteractionLaw.md`; this
contract records only that `RenderedLine` is a small expression-result shape.
Stage 7-6 Renderer Baseline Tests is also owned by `StoryInteractionLaw.md`;
this contract records only that baseline tests prove renderer output is no
stronger than ExplanationPlan.
Stage 7 closeout is also owned by `StoryInteractionLaw.md`; this contract
records only that deterministic renderer is closed as a template baseline and
Stage 8 LLM Narration may receive deterministic text and ExplanationPlan only.
Stage 8 prompt smoke is also owned by `StoryInteractionLaw.md`; this contract
records only the 8A Mock narrator and 8B Codex CLI prompt smoke shape. 8A may
receive ExplanationPlan and RenderedLine only. 8B may receive renderedText,
claimKey, strength, forbidden wording list, and the instruction `Rephrase only.
Do not add chess facts.` only. Production API validation remains closed.

Public route `200`, `/api/commentary/render`,
`/internal/commentary/render-local-probe`, production API validation,
pedagogy, natural-language verifier, raw proof input, and new chess meaning
remain closed. LLM narration authority is limited to Stage 8 prompt smoke.

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
- `RemoveGuardProof`
- `RemoveGuardRemovalKind`
- `EngineEval`
- `EngineLine`
- `EngineCheck`
- `EngineCheckStatus`
- `StoryWriter`
- `TacticHanging`
- `TacticRemoveGuard`
- `Verdict`
- `StoryTable`
- `ExplanationPlan`
- `ExplanationClaim`
- `ExplanationStrength`
- `ExplanationRelation`
- `ForbiddenWording`
- `RenderedLine`
- `LlmNarrationSmoke`
- `NarrationSmokeCheck`

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

- story size: `163`
- verdict size: `99`
- top stories: `8`

Story slots:

- `18` scene slots
- `32` plan slots
- `25` tactic slots
- `16` pawn slots
- `16` piece slots
- `16` king slots
- `8` opening slots
- `32` proof slots

Each story carries compact chess identity:

- `side`: `White`, `Black`, `Both`, or `None`
- `target`: optional `Square`
- `secondaryTarget`: optional `Square` for multi-target Stories such as the
  narrow Fork slice
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

The first piece-family slot stores Fork's secondary target square index + 1, or
`0` absent. It does not create a second proof owner; it keeps multi-target
Story identity in `Story` while `MultiTargetProof` supplies proof.

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
- `MultiTargetProof` is internal family proof for the narrow `Tactic.Fork`
  vertical slice. It records the non-pawn attacker, attacker-after-move, fork
  move, post-move attacked target squares, two named targets, target values or
  importance, bounded reply map, material-or-tempo result, same-board proof
  presence, and missing evidence.
  Its internal accessors expose fork square, target A, and target B as proof
  evidence only.
  It is not a capture Story, renderer input, LLM input, or public claim owner.
  `Story` remains the home for side, primary target, secondary target, anchor,
  route, and rival, and `StoryTable` must reject any positive Fork writer whose
  `MultiTargetProof` does not bind back to that Story identity.
- `StoryWriter.TacticFork` is the named positive Fork writer. It may enter
  StoryTable only for `Scene.Tactic`, `Tactic.Fork`, complete StoryProof,
  complete MultiTargetProof, same-board proof, legal move to fork square, two
  named targets, proven post-move target relation, and no EngineCheck Refutes
  result. Reply-map entries showing one reply can save both targets block Fork
  Lead. It does not create renderer text, LLM narration, or public material,
  best-move, forced, decisive, no-counterplay, blunder, or engine-says claims.
- EngineCheck is internal evidence only. It records same-board proof, checked move, engine line, reply line, eval before, eval after, depth or freshness, and missing evidence. EngineCheck does not create a Story, select a Story, rank a Story, write a Verdict, feed a renderer, or feed an LLM. EngineLine carries only move lines. EngineEval carries only internal centipawn numbers.
- EngineCheck.fromStory binds engine evidence to same-board BoardFacts, an existing Story route, and a same-board legal line. Wrong-board facts, route-mismatched engine lines, stale engine data, depth-missing engine data, eval-only input without a Story, and PV-only input without a Story leave missing evidence and stay diagnostic only.
- EngineCheckStatus has exactly `Unknown`, `Supports`, `Caps`, and `Refutes`. Only `Tactic.Hanging`, the narrow `Tactic.Fork` vertical slice, and the narrow `Scene.Material` writer may carry EngineCheck in this checkpoint. `Refutes` blocks the named Story. `Supports` and `Caps` do not change `Verdict.values`, create public truth, or create winning, best-move, decisive, PV-explanation, or public-eval claims.
- Fork reuses `EngineCheck`; no `ForkEngineCheck` type exists. Fork
  EngineCheck attachment requires an existing Fork Story, same-board proof, the
  same Story route, the same legal line, fresh or depth evidence, and
  evidence-ready EngineCheck data. Engine-only checks, route-mismatched checks,
  missing-depth checks, raw engine lines, raw eval, and PV-shaped data cannot
  create Fork.
- Material-5 reuses existing `EngineCheck` for `Scene.Material`. No
  `MaterialEngineCheck` type exists. Material EngineCheck attachment requires
  an existing Material Story, same-board proof, the same Story route, the same
  legal line, fresh or depth evidence, and Story-bound evidence-ready
  EngineCheck data. Engine-only checks, route-mismatched checks, missing-depth
  checks, raw engine lines, raw eval, and PV-shaped data cannot create
  Material.
- Eval collapse after capture or fork route may refute an existing EngineCheck only after same-board Story proof, named writer, family proof sidecar, legal route, and freshness guards pass. Eval collapse cannot create a Story, public eval claim, or engine-authored explanation.
- Verdict carries `engineCheckStatus` and `engineStrengthLimited` as internal diagnostics only. `Verdict.values`, renderer, and LLM inputs must not consume EngineCheck diagnostics.
- Stage 5 Story Order baseline is limited to existing `Tactic.Hanging` Story
  rows. The current Fork-6 slice adds only deterministic ordering between
  existing `Tactic.Hanging` rows and existing narrow `Tactic.Fork` rows.
  Material-3 adds only single-row StoryTable admission for proof-backed
  `Scene.Material` rows through `StoryWriter.SceneMaterial`.
  Material-6 adds only StoryTable integration for existing Hanging, Fork, and
  Material rows. Refuted, incomplete, writerless, or material-proof-missing
  Material rows are Blocked. Hanging, Fork, and Material can compete for Lead.
  Support and Context roles are not sentences.
  StoryTable may assign roles and deterministic ordering, but it must not
  create Stories, open broad positive families, or use engine eval, Board
  Facts, `CaptureResult`, `MultiTargetProof` text, material proof text, raw PV,
  or renderer wording as direct public claim owners or ranking inputs.
- Stage 5-1 may mark only the selected Lead row as `leadAllowed`; Support and
  Context roles are not public sentences and do not open renderer or LLM.
- Stage 5-2 ordering may use role eligibility, publicStrength, scene/tactic
  identity, side, target, anchor, route, writer presence, and blocked status.
  It must not use raw engine eval, raw PV text, proofFailures text, Board Facts
  row count, `CaptureResult` text, renderer wording, or input order.
- Stage 5-3 conflict rules may block Hanging-shaped rows from EngineCheck
  refutation, missing proof, missing capture evidence, or missing writer, and
  may keep Quiet, Source, and Opening behind board-backed Hanging without
  opening Plan, Blunder, Defense, extra counterplay, or Strategy relations.
- Fork-6 role rules block refuted Fork, incomplete Fork, writerless Fork, and
  Fork rows without `MultiTargetProof`. Hanging and Fork may compete for Lead,
  but Support and Context still create no sentence.
- Stage 5-4 keeps Verdict diagnostics outside public numeric values:
  proofFailures, EngineCheck diagnostics, and `engineStrengthLimited` remain
  internal; Verdict is not public text, and renderer, LLM, and public route
  remain closed.
- Stage 5 closeout keeps the next boundary narrow: Explanation Plan may receive
  selected Verdict data only, and must not directly consume raw Board Facts,
  raw BoardMood, root atoms, CaptureResult, EngineCheck, EngineEval,
  EngineLine, raw PV text, proofFailures text, source rows, renderer wording,
  or LLM wording.
- Stage 6-0 records only selected-Verdict speech bounds. It may carry claim,
  evidence, strength, role, support/context relation, and forbidden wording,
  but it writes no sentence and opens no renderer, LLM, public route `200`,
  pedagogy, new Story family, or engine explanation.
- Stage 6-1 opens only the Explanation Plan shape for one selected
  `Tactic.Hanging` Lead Verdict. `allowedClaim` stays a structured key such as
  `can_win_piece`; the first shape carries `bounded` strength and forbidden
  wording, not public prose.
- Stage 6-2 opens only `Tactic.Hanging` allowed claim mapping. Uncapped Lead
  Verdict only may carry an allowed claim key. Support, Context, Blocked, and
  engine-capped Verdicts do not create standalone public claims.
  `engineStrengthLimited` suppresses claim keys and strengthens forbidden
  wording.
- Stage 6-3 opens only forbidden wording boundary. Explanation Plan must carry
  the default forbidden wording set. `Tactic.Hanging` remains bounded material
  tactic wording only. `engineStrengthLimited` strengthens forbidden wording
  without carrying a claim.
- Stage 6-4 opens only Support and Context relation structure. Uncapped Lead
  only carries an allowed claim. Support, Context, Blocked, and engine-capped
  Verdicts create no standalone public claim. Blocked remains debug-only
  relation structure, and proofFailures do not feed relation wording.
- Stage 6-5 opens only the selected Verdict input guard. Explanation Plan
  accepts selected Verdict only. It accepts no raw proof material, unselected
  Story, unselected Verdict, source row, or proofFailures wording.
- Fork-7 opens only ExplanationPlan mapping for selected narrow `Tactic.Fork`
  Verdicts. Fork allowed claim keys are `forks_two_targets` and
  `attacks_two_targets`; the first emitted Fork claim key is
  `forks_two_targets`. Fork plans may carry secondaryTarget from the selected
  Verdict's Story identity, but must not read MultiTargetProof, EngineCheck,
  CaptureResult, BoardFacts, raw PV, proofFailures, or source rows directly.
  Support, Context, Blocked, capped, and engine-refuted Fork plans create no
  standalone public claim or sentence.
- Fork-8 opens only deterministic renderer text for Fork ExplanationPlan.
  Renderer input remains `ExplanationPlan` only. The first Fork template uses
  route, target, and secondaryTarget already lowered from the selected Verdict:
  `{route} forks the pieces on {targetA} and {targetB}.` It must not read
  MultiTargetProof, EngineCheck, CaptureResult, BoardFacts, BoardMood, raw PV,
  proofFailures, source rows, raw Verdict, or source Story directly. It does
  not open Fork LLM smoke itself, public/user-facing LLM narration, public
  route `200`, production API, material claims, wins-queen claims, engine-says
  wording, best-move wording, or sibling tactic families.
- Fork-9 opens only LLM smoke for selected Fork ExplanationPlan and
  RenderedLine. 8A may receive ExplanationPlan and RenderedLine only. 8B may
  receive renderedText, claimKey, strength, forbidden wording list, and the
  instruction `Rephrase only. Do not add chess facts.` only. It must not read
  raw Verdict, Story, MultiTargetProof, EngineCheck, CaptureResult,
  BoardFacts, BoardMood, EngineEval, EngineLine, engine eval, raw PV,
  proofFailures, or source rows. It rejects new move, new line, new tactic, new
  plan, engine mention, best-move, forced, winning, decisive, blunder,
  wins-queen, wins-material, target-identity, and stronger-claim output.
  Production API, user-facing LLM output, public route `200`, pedagogy, and
  sibling tactic families remain closed.
- Fork slice closeout opens no new runtime authority. `MultiTargetProof`
  remains TargetProof-shaped proof evidence and does not replace
  CaptureResult, StoryProof, EngineCheck, or StoryTable. `Tactic.PawnFork`,
  `Tactic.Skewer`, `Tactic.QueenHit`, `Tactic.Tempo`, `Tactic.InBetween`,
  `Scene.Defense`, Plan, Strategy, public route `200`, production API, and
  public/user-facing LLM narration remain closed. Fork does not open
  `Scene.Material` by implication; Material-3 separately opens only the narrow
  named `Scene.Material` writer.
- Material-0 and Material-1 are owned by `StoryInteractionLaw.md`. Material-0
  fixes `Scene.Material` as a scene Story label, not a tactic and not proof.
  `CaptureResult` or `ExchangeResult` is the proof home, while
  `material_change` is the speech claim key. Material-1 reuses
  `CaptureResult` for the first simple capture and immediate bounded recapture
  scope. `ExchangeResult` is not created in Material-1 and opens only for a
  bounded multi-move exchange sequence that capture proof cannot own without
  overloading capture meaning. Material-0/1 do not open a Material writer,
  StoryTable role rule, ExplanationPlan mapping, renderer text, LLM smoke,
  public route `200`, production API, winning, decisive, blunder, conversion,
  best-move, forced, no-counterplay, engine-says, or full-evaluation claims.
- Material-2 extends `CaptureResult` with captured pieces and bounded exchange
  sequence proof fields. The proof shape may calculate side, legal line,
  captured pieces, recapture candidates, bounded exchange sequence, material
  result, same-board proof, and missing evidence. It may prove that a line
  leaves White or Black up material, or that the exchange result is known. It
  does not create a public Story, Material writer, public sentence,
  ExplanationPlan input, renderer text, LLM input, winning-position claim,
  decisive-advantage claim, conversion claim, blunder claim, best-move claim,
  or forced-line claim.
- Material-3 opens `StoryWriter.SceneMaterial` as the named Material writer.
  `SceneMaterial` may create only `Scene.Material` Stories with complete
  StoryProof, complete material proof, same-board proof, legal line, known
  material result, and no EngineCheck Refutes result. The first allowed
  Material meaning is limited to a line changing material balance or an
  exchange leaving one side ahead in material. Material-3 does not open
  ExplanationPlan mapping, renderer text, LLM input, public route `200`,
  production API, winning, decisive, blunder, conversion, best-move, forced,
  no-counterplay, engine-says, or full-evaluation claims.
- Material-4 adds only the Material negative corpus. Legal-line missing,
  same-board missing, erased material result, unclear exchange result, king
  target, zero material result, EngineCheck Refutes, incomplete StoryProof,
  incomplete material proof, tactic-writer duplication, Hanging/Fork
  auto-duplication, and high Proof score only produce no Lead or Blocked.
  Material-4 does not open ExplanationPlan mapping, renderer text, LLM input,
  public route `200`, production API, winning, decisive, blunder, conversion,
  best-move, forced, no-counterplay, engine-says, or full-evaluation claims.
- Material-5 reuses existing `EngineCheck` for `Scene.Material`. `Unknown`,
  `Supports`, `Caps`, and `Refutes` are the only statuses. It may support, cap,
  or refute an existing Material Story only after same-board proof, same Story
  route, same legal line, and fresh or depth evidence are present. It creates
  no Material Story, public engine truth, PV explanation, best-move
  explanation, winning claim, or duplicate engine type.
- Material-6 adds only StoryTable integration for existing Hanging, Fork, and
  Material rows. Refuted, incomplete, writerless, or material-proof-missing
  Material rows are Blocked. Hanging, Fork, and Material can compete for Lead.
  Support and Context are not sentences. StoryTable creates no Material Story,
  raw engine eval does not rank Material, material proof text and renderer
  wording remain non-public, and Material does not open conversion or winning.
- Material-7 opens only ExplanationPlan mapping for selected `Scene.Material` Verdicts.
  Material allowed claim keys are `material_balance_changes`,
  `line_leaves_material_gain`, and `exchange_leaves_side_ahead`; the first
  emitted Material claim key is `material_balance_changes`. Support, Context,
  Blocked, capped, and engine-refuted Material plans create no standalone
  public claim. It reads no material proof, `CaptureResult`, `ExchangeResult`,
  `EngineCheck`, `BoardFacts`, raw PV, proofFailures, or source row input, and
  it opens no Material renderer text, LLM smoke, public route, production API,
  winning, decisive, conversion, blunder, best-move, forced-win, or
  no-counterplay claim.
- Material-8 opens only deterministic renderer text for selected `Scene.Material` ExplanationPlan.
  Renderer input remains `ExplanationPlan` only. The first route template is
  `After {route}, White comes out ahead in material.` Renderer text must not
  exceed the Material ExplanationPlan or use winning, decisive, blunder,
  forced, best-move, no-counterplay, engine-says, conversion, or
  technically-winning wording. Material-8 opens no LLM smoke, public route,
  production API, pedagogy, engine PV commentary, best-move explanation, or
  sibling Story family.
- Material-9 opens only LLM smoke for selected Material ExplanationPlan and RenderedLine.
  8B Material Codex CLI input is limited to renderedText, claimKey, strength,
  forbidden wording, and `Rephrase only. Do not add chess facts.` It must not
  read raw Verdict, Story, material proof, CaptureResult, ExchangeResult,
  EngineCheck, BoardFacts, engine eval, raw PV, proofFailures, or source row
  data. Material LLM smoke rejects new moves, new lines, new tactics, new
  plans, engine mentions, winning, decisive, forced, blunder, best-move,
  conversion, and stronger claims. It opens no public/user-facing LLM
  narration, public route, production API, pedagogy, or full-evaluation claim.
- Material Slice Closeout confirms `Scene.Material` opened no Defense, Conversion, Winning, Plan, Strategy, or Blunder path.
  `CaptureResult` remains the proof home for simple capture and immediate
  bounded recapture material result. `ExchangeResult` remains unopened and
  reserved for bounded multi-move exchange proof outside this slice if needed.
  `Scene.Material` owns only the Story label, and `material_change` is
  speech-claim vocabulary with the current runtime key `material_balance_changes`.
  Material proof text, raw proof sidecars, engine data, BoardFacts, raw PV,
  proofFailures, and source rows do not feed ExplanationPlan, Renderer, or LLM
  smoke directly. Material reuses the existing proof home, Story writer,
  EngineCheck, StoryTable, ExplanationPlan, Renderer, and LLM smoke skeleton.
  The next named slice remains `Scene.Defense`.
- Defense-0 is owned by `StoryInteractionLaw.md`. Defense-0 opens only the `Scene.Defense` charter.
  It fixes the first scope to an attacked piece, an immediate same-board legal
  material threat, a defended move that removes, guards, or saves the target,
  and bounded prevention of material loss. `ThreatProof` names what must be
  stopped. `DefenseProof` names how it is stopped. `Scene.Defense` is the
  Story label. `defends_piece` and `prevents_material_loss` are speech claims.
  It opens no writer, proof sidecar, StoryTable integration, ExplanationPlan,
  renderer, LLM smoke, public route `200`, or production API. It does not open
  only-move, best-move, no-counterplay, refutation, solved-position, king
  safety, mate defense, strategic defense, prophylaxis, winning, or conversion
  claims.
- Defense-1 is owned by `StoryInteractionLaw.md`. Defense-1 opens only `ThreatProof`.
  ThreatProof carries rival side, threatened target, attacking piece, legal
  threat line, target value, material loss if unanswered, same-board proof,
  and missing evidence. It may prove that the rival can immediately and
  legally capture the target, that the target is attacked, and that the
  capture would cause bounded material loss. ThreatProof proves what must be
  stopped, but it does not create a Defense Story or public claim. It does not
  open DefenseProof, the named Scene.Defense writer, StoryTable integration,
  ExplanationPlan, renderer, LLM smoke, public route `200`, production API,
  king safety, mate threat, long-term pressure, strategic threat,
  no-counterplay, or engine-says-this-is-a-threat wording.
- Defense-2 is owned by `StoryInteractionLaw.md`. Defense-2 opens only `DefenseProof`.
  DefenseProof carries defending side, defense move, defended target, original
  threat, after-defense target status, material loss prevented, same-board
  proof, and missing evidence. It may prove only that the target moves away,
  the target becomes guarded, the attacker line is blocked, or the attacker is
  captured. DefenseProof proves whether a specific threat is stopped, but it
  does not create a Defense Story or public claim. It does not open the named
  Scene.Defense writer, StoryTable integration, ExplanationPlan, renderer, LLM
  smoke, public route `200`, production API, solved-position, refutation,
  stops-all-threats, only-move, best-defense, no-counterplay, king safety, or
  mate defense wording.
- Defense-3 is owned by `StoryInteractionLaw.md`. Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story.
  A SceneDefense Story requires complete StoryProof, complete ThreatProof,
  complete DefenseProof, same-board proof, legal defense move, identified
  protected target, prevented material loss, writer `SceneDefense`, and no
  refuting EngineCheck. Its first allowed meanings are only that this move
  defends the attacked piece and prevents immediate material loss. It does not
  open ExplanationPlan, renderer, LLM smoke, public route `200`, production API,
  only-move, best-move, refutes-attack, stops-counterplay, solves-position,
  king-safe, mate-stopped, winning, or decisive wording.
- Defense-4 is owned by `StoryInteractionLaw.md`. Defense-4 opens only the Defense negative corpus.
  Defense-looking rows have no Lead or are Blocked unless ThreatProof and
  DefenseProof are complete. The negative corpus covers no actual threat,
  illegal threat, already adequately defended target, defense moves that do not
  affect the target, wrong-piece guards, still-losing defenses, equivalent
  recapture, prophylaxis-looking rows, tactic/material-gain rows, king-safety
  claims, mate-defense claims, only-move claims, incomplete StoryProof,
  incomplete ThreatProof, incomplete DefenseProof, EngineCheck Refutes, and
  high Proof score only. Defense-4 opens no ExplanationPlan, renderer, LLM
  smoke, public route `200`, production API, only-move, best-move,
  no-counterplay, king-safety, mate-defense, winning, or decisive wording.
- Defense-5 is owned by `StoryInteractionLaw.md`. Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories.
  EngineCheck may support, cap, or refute an existing Defense Story, but it
  does not create Defense. Defense EngineCheck evidence requires an existing
  Defense Story, same-board proof, the same Story route, the same legal line,
  and fresh/depth evidence. Allowed statuses are Unknown, Supports, Caps, and
  Refutes. It opens no DefenseEngineCheck duplicate type, engine-created
  Defense Story, public engine truth, PV explanation, best-move explanation,
  only-move claim, refutes-attack claim, ExplanationPlan, renderer, LLM smoke,
  public route `200`, or production API.
- Defense-6 is owned by `StoryInteractionLaw.md`. Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows.
  StoryTable deterministically orders Hanging, Fork, Material, and Defense
  without creating new chess meaning. Refuted, incomplete, writerless,
  ThreatProof-missing, and DefenseProof-missing Defense rows are Blocked.
  Defense may compete for Lead only with complete proof. Support and Context
  roles are not sentences. StoryTable does not create Defense, rank Defense by
  raw engine eval, expose Defense proof text, let renderer wording affect order,
  or open only-move or no-counterplay wording.
- Defense-7 is owned by `StoryInteractionLaw.md`. Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts.
  It accepts selected Verdict only and must not accept ThreatProof directly,
  DefenseProof directly, EngineCheck, BoardFacts, raw PV, proofFailures, or
  source row input. First allowed claim keys are `defends_piece`,
  `prevents_material_loss`, and `protects_target`. Forbidden claim keys are
  `only_move`, `best_defense`, `refutes_attack`, `stops_counterplay`,
  `solves_position`, `king_safe`, `mate_defense`, and `no_counterplay`.
  Defense ExplanationPlan creates no meaning stronger than the selected
  Verdict.
- Defense-8 is owned by `StoryInteractionLaw.md`. Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan.
  Renderer receives ExplanationPlan only and may render only bounded Defense
  text such as `{route} defends the piece on {target}.` or `{route} prevents
  the piece on {target} from being lost immediately.` Renderer text is no
  stronger than the Defense ExplanationPlan. It opens no ThreatProof,
  DefenseProof, EngineCheck, BoardFacts, raw PV, proofFailures, source row,
  only-move, best-move, refutes-attack, stops-all-counterplay,
  solves-position, king-safe, mate-stopped, winning, decisive, forced, LLM
  smoke, public route `200`, or production API path.
- Defense-9 is owned by `StoryInteractionLaw.md`. Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine.
  LLM smoke may receive only ExplanationPlan and RenderedLine. 8B Codex CLI
  smoke input may receive only renderedText, claimKey, strength, forbidden
  wording, and the instruction `Rephrase only. Do not add chess facts.` It
  must not receive raw Verdict, Story, ThreatProof, DefenseProof, EngineCheck,
  BoardFacts, engine eval, raw PV, or proofFailures. It rejects new moves, new
  lines, new tactics, new plans, engine mentions, only-move, best-move,
  no-counterplay, king-safety, mate-defense, refutes-attack wording, and
  stronger claims. LLM smoke does not make Defense text stronger. Public route
  `200`, production API, and public/user-facing LLM narration remain closed.
- Defense Slice Closeout confirms `Scene.Defense` opened no King safety, Mate defense, Plan, Strategy, Counterplay, or Prophylaxis path.
  Defense closes as a narrow proof-backed attacked-piece material-loss defense
  slice only. `ThreatProof`, `DefenseProof`, `Scene.Defense`, and
  `defends_piece` each keep one home. ThreatProof owns what must be stopped;
  DefenseProof owns how the move stops it; StoryProof owns same-board Story
  identity evidence; EngineCheck supports, caps, or refutes an existing
  Defense Story only; StoryTable arbitrates roles without creating Defense.
  The Defense negative corpus keeps defense-looking false positives silent
  without complete ThreatProof and DefenseProof. The slice reused the shared
  charter, proof home, named writer, negative corpus, EngineCheck, StoryTable,
  ExplanationPlan, deterministic renderer, LLM smoke, and closeout skeleton.
  Fischer-Spassky 1972 game 6 after 6...h6, 7.Bh4 is covered as the real-game
  attacked-piece defense smoke. Next candidates remain line-based tactic or
  king-forcing tactic; Defense does not open king safety, mate defense, or
  counterplay. Public route `200`, production API, and public/user-facing LLM
  narration remain closed.
- Middlegame Interaction Hardening checks StoryTable role stability, selected
  Verdict stability, ExplanationPlan stability, and renderer/LLM smoke
  boundaries among already-open Hanging, Fork, Material, and Defense rows.
  Middlegame Interaction Hardening opens no chess meaning. It stress-tests
  already-open meanings. MIH-0 opens no new Story family, proof home, renderer
  wording, public route, production API, or public/user-facing LLM narration.
  MIH-0 opens only interaction hardening among existing rows and complex
  middlegame fixture based role stability checks. It checks selected Verdict,
  ExplanationPlan, and renderer/LLM smoke boundary stability without opening
  new speech. It may apply only the minimum StoryTable ordering fix if an
  existing ordering bug is exposed. MIH-1 opens only complex middlegame test
  fixtures for already-open Hanging, Fork, Material, and Defense rows. Fixture
  Map names board, rows, roles, selected Verdict, and forbidden claims without
  opening new meaning. MIH-2 opens only StoryTable role stability checks over
  existing Hanging, Fork, Material, and Defense rows. Role Stability keeps
  selected Verdict deterministic, prevents duplicate Lead, blocks incomplete
  or refuted rows, and keeps capped rows from standalone strong claims without
  opening new meaning. MIH-3 opens only the Material vs Defense collision rule
  over existing `Scene.Material` and `Scene.Defense` rows. Material vs Defense
  collision selects actual material change now over prevented immediate loss,
  blocks speculative material loss, and keeps both public boundaries bounded.
  MIH-4 opens only existing EngineCheck interaction checks over already-open
  Hanging, Fork, Material, and Defense rows. EngineCheck Interaction reuses
  existing EngineCheck statuses, keeps Supports and Unknown non-speaking,
  suppresses or weakens Caps, blocks Refutes, and prevents engine eval, raw
  PV, engine-says, best-move, and eval-number public leakage.
  MIH-5 opens only close false-positive negative corpus tests over
  already-open Hanging, Fork, Material, Defense, and EngineCheck rows.
  Negative Corpus keeps close false positives silent unless complete proof
  exists, and no plausible-looking row may reach selected public output
  through StoryTable, ExplanationPlan, renderer, or LLM smoke boundaries.
  MIH-6 opens only downstream boundary smoke over selected Verdict, existing
  ExplanationPlan, existing DeterministicRenderer, and existing LLM smoke.
  Downstream Boundary Smoke passes only selected Lead Verdict data through
  existing ExplanationPlan, renderer, and LLM smoke boundaries, while non-Lead,
  capped, and refuted rows stay silent.
  MIH-7 opens only diagnostics boundary smoke over already-open Hanging, Fork,
  Material, Defense, StoryTable, selected Verdict, ExplanationPlan, renderer,
  and LLM smoke. Diagnostics Boundary keeps proofFailures, raw proof failure
  text, engine text, source row data, and StoryTable debug relations out of
  public meaning, Verdict.values, ExplanationPlan source inputs, renderer
  wording, and LLM smoke prompts.
  MIH Closeout opens no chess meaning. It only audits the MIH hardening
  surface. MIH closes as interaction hardening only, with no new Story family,
  no new proof home, no duplicate meaning owner, no broad-term authority, no
  duplicated live rule authority outside StoryInteractionLaw.md, no promoted
  test helper, no public route 200, no production API, and no
  public/user-facing LLM narration.
  Same-board Material vs Defense collisions are resolved by actual
  material change now over prevented immediate loss, with speculative material
  loss blocked. EngineCheck may support, cap, or refute existing rows only. Raw
  engine eval and renderer wording must not rank rows, and Support/Context
  remain relation roles, not sentences. Line/Ray, RemoveGuard, Pin, Skewer,
  Pawn, BackRank, Plan, Strategy, Initiative, Pressure, new Story family, new
  proof home, new renderer wording, public route `200`, production API, and
  public/user-facing LLM narration remain closed.
- Line-0 and Line-1 are owned by `StoryInteractionLaw.md`. Line-0 opens only
  the first narrow line/ray charter: a legal move reveals one slider attack on
  one non-king material target. Line-1 opens only `LineProof` as a narrow proof
  home. `LineProof` proves side, slider piece, blocker or moved piece, revealed
  target, legal revealing move, line kind, same-board proof, before-move
  blocked or inactive line, after-move slider attack, and non-king material
  target. LineProof is not a public Story, LineFact is not a public Story, and
  proof failure text must not become renderer or LLM input. Line-1 opens no
  Story writer, StoryTable integration, ExplanationPlan mapping, renderer
  wording, LLM smoke, public route `200`, production API, pin, skewer, x-ray
  public Story, RemoveGuard, pressure, initiative, winning, decisive, blunder,
  best-move, forced-line, mate threat, or king-safety claim.
- Line-2 opens only the named `TacticDiscoveredAttack` writer for one narrow
  `Tactic.DiscoveredAttack` Story. It requires complete StoryProof, complete
  LineProof, same-board legal replay, legal revealing move, target exists,
  after-move slider attack, writer identity, and no EngineCheck Refutes status.
  The Story identity is tactic DiscoveredAttack, revealing side, revealed
  target square, moved-piece or slider anchor, revealing route, and opposite
  rival side. Line-2 opens no Tactic.Pin, Tactic.Skewer, Tactic.XRay,
  RemoveGuard, king-target speech, ExplanationPlan mapping, renderer wording,
  LLM smoke, public route `200`, production API, winning, decisive, blunder,
  best-move, forced-line, pressure, initiative, mate threat, or king-safety
  claim.
- Line-3 opens only the negative corpus for the narrow `Tactic.DiscoveredAttack`
  slice. It keeps discovered-attack-looking false positives silent when the
  legal move is absent, same-board proof is absent, the line is not actually
  opened, the target is still not attacked after the move, the slider is not a
  slider, the target is king, another piece still blocks after the blocker
  moved, the discovered-looking move has no target, pressure, initiative, or
  mate wording tries to enter, or Pin, Skewer, or XRay classification tries to
  enter. Geometry is not enough. Revealed attack proof or silence.
- Line-4 opens only existing `EngineCheck` reuse for existing
  `Tactic.DiscoveredAttack` Stories. EngineCheck cannot create
  DiscoveredAttack, Supports creates no new claim, Caps suppresses strong
  expression, Refutes blocks the Story, and Unknown creates no engine
  expression. It opens no raw eval ordering, raw PV explanation, engine says,
  best move, winning tactic, ExplanationPlan mapping, renderer wording, LLM
  smoke, public route `200`, or production API.
- Line-5 opens only StoryTable integration for existing `Tactic.Hanging`,
  `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, and
  `Tactic.DiscoveredAttack` rows. The selected Verdict remains stable when
  input order changes. DiscoveredAttack does not own a Material claim, Hanging
  and Material on the same target do not both become Lead, Defense without an
  actual threat cannot create a claim that blocks DiscoveredAttack, and Fork
  without two-target proof cannot absorb DiscoveredAttack. It opens no Material
  claim for DiscoveredAttack, no Defense claim without ThreatProof, no Fork
  claim without two-target proof, renderer wording, LLM smoke, public route
  `200`, or production API.
- Line-6 opens only ExplanationPlan mapping for selected Lead
  `Tactic.DiscoveredAttack` Verdicts. The only allowed claim key is
  `reveals_attack_on_piece`. Forbidden claim keys are `wins_material`,
  `pins_piece`, `skewers_piece`, `creates_pressure`, `takes_initiative`,
  `mate_threat`, `best_move`, `forced`, and `decisive`. Support, Context,
  Blocked, capped, and refuted DiscoveredAttack rows create no standalone
  claim. It opens no renderer wording, LLM smoke, public route `200`,
  production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard,
  pressure, initiative, mate threat, best-move, forced-line, winning, or
  decisive claim.
- Line-7 opens only deterministic renderer text for selected
  `Tactic.DiscoveredAttack` ExplanationPlan. Renderer receives
  `ExplanationPlan` only and may render `{route} reveals an attack on the
  piece on {target}.` It must not render `wins material`, `winning`,
  `decisive`, `best move`, `forces`, `pins`, `skewers`, `puts pressure`, or
  `creates a mating threat`. It opens no LLM smoke, public route `200`,
  production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard,
  pressure, initiative, mate threat, best-move, forced-line, winning, or
  decisive claim.
- Line-8 opens only LLM smoke for selected DiscoveredAttack ExplanationPlan
  and RenderedLine. It reuses existing 8B prompt smoke only. LLM input is
  renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do
  not add chess facts.` It must not read raw Story, raw LineProof, LineFact,
  BoardFacts, EngineCheck, raw PV, or proofFailures, and it must reject new
  moves, new lines, mate, pressure, initiative, and winning claims. It opens
  no public/user-facing LLM narration, public route `200`, production API,
  Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure,
  initiative, mate threat, best-move, forced-line, winning, or decisive claim.
- Line Closeout opens no new chess meaning. It audits only that LineFact
  observes geometry, LineProof proves one revealed line, Tactic.DiscoveredAttack
  owns the Story identity, `reveals_attack_on_piece` owns speech vocabulary,
  renderer/LLM stay no stronger than the selected ExplanationPlan, detailed
  Line closeout authority stays in `StoryInteractionLaw.md`, and public route
  `200`, production API, and public/user-facing LLM narration remain closed.
- Pin-0 is owned by `StoryInteractionLaw.md`. Pin-0 opens only the charter for
  the second narrow line/defender contact vertical slice. The first positive
  Pin scope is not a broad pin family. The Pin first scope is a legal move that
  creates or reveals a line where one non-king piece is pinned to its king.
  LineFact observes geometry, LineProof binds the line, PinProof proves the
  pinned relation, and Tactic.Pin may speak only after proof. Pin-0 opens only
  narrow Tactic.Pin, king-behind line relation, one non-king pinned target, a
  legal move that creates or reveals the pin relation, and bounded pin wording
  after selected Verdict only. Pin-0 opens no broad LineTactic, broad AbsPin or
  RelPin family, Skewer, XRay public Story, RemoveGuard, DiscoveredAttack
  expansion, mate threat, king safety, winning material, decisive tactic,
  forced move, best move, cannot move wording, pressure, initiative, public
  route `200`, production API, or public/user-facing LLM narration.
- Pin-1 is owned by `StoryInteractionLaw.md`. Pin-1 opens only `PinProof` as a
  narrow proof home. PinProof proves side creating the pin, pinned target,
  pinning slider, king behind target, legal pinning or revealing move, line
  kind, same-board proof, before/after relation, target non-king, target and
  king same side, and slider attacks through target toward king after move.
  PinProof is not a public Story, LineFact is not a public Story, and LineProof
  is not a public Story. PinProof says no material gain, king unsafe, mate,
  pressure, or initiative, and proof failure text must not become renderer or
  LLM input.
- Pin-2 is owned by `StoryInteractionLaw.md`. Pin-2 opens only the named
  `TacticPin` writer for one narrow `Tactic.Pin` Story. TacticPin requires
  complete StoryProof, complete PinProof, same-board legal replay, legal
  pinning or revealing move, pinned target, pinning slider,
  king-behind-target relation, writer identity, and no EngineCheck Refutes
  status. Pin Story identity is tactic Pin, scene Tactic, pinning side,
  pinned-side rival, pinned target square, pinning slider or moved-piece anchor,
  and pinning/revealing route. Pin-2 opens no Material claim, Defense
  ownership, RemoveGuard ownership, king target speech, broad AbsPin or RelPin
  family, ExplanationPlan mapping, renderer wording, LLM smoke, public route
  `200`, production API, or public/user-facing LLM narration.
- Pin-3 is owned by `StoryInteractionLaw.md`. Pin-3 opens only the negative
  corpus for the narrow `Tactic.Pin` slice. Pin-3 keeps illegal moves, missing
  same-board proof, non-sliders, missing king-behind-target relation,
  wrong-side king relation, broken target-to-king line, king targets, extra
  blockers, stale pin-looking geometry, discovered-only lines, skewer-looking
  lines, and mate, king-safety, or pressure wording silent. A line to a king is
  not enough. Complete pinned relation or silence.
- Pin-4 is owned by `StoryInteractionLaw.md`. Pin-4 opens only existing
  `EngineCheck` reuse for existing `Tactic.Pin` Stories. EngineCheck cannot
  create Pin; Supports creates no new claim; Caps suppresses allowed claim or
  keeps downstream speech bounded when opened later; Refutes blocks the Pin
  Story; Unknown creates no engine expression. Pin-4 opens no engine-says
  wording, best-move wording, only-move wording, winning tactic, forced win,
  raw PV explanation, eval number public value, ExplanationPlan mapping,
  renderer wording, LLM smoke, public route `200`, production API, or
  public/user-facing LLM narration.
- Pin-5 is owned by `StoryInteractionLaw.md`. Pin-5 opens only StoryTable
  integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`,
  `Scene.Defense`, `Tactic.DiscoveredAttack`, and `Tactic.Pin` rows. Pin-5
  keeps selected Verdict stable across input order, prevents duplicate Lead for
  same-line DiscoveredAttack and Pin, keeps Pin from owning Material or
  king-safety claims, keeps actual material change in Scene.Material, and gives
  Defense no claim without complete ThreatProof and DefenseProof. Pin-5 opens
  no new Pin proof home, new Story family, broad AbsPin or RelPin family,
  Material claim from Pin, king-safety claim from Pin, Defense claim from
  incomplete Defense rows, ExplanationPlan mapping, renderer wording, LLM
  smoke, public route `200`, production API, or public/user-facing LLM
  narration.
- Pin-6 is owned by `StoryInteractionLaw.md`. Pin-6 opens only ExplanationPlan
  mapping for selected uncapped Lead `Tactic.Pin` Verdicts. Pin-6 allows only
  the `pins_piece` claim key and forbids wins_material, king_unsafe,
  mate_threat, best_move, only_move, forced, decisive, creates_pressure,
  takes_initiative, and cannot_move. Support, Context, Blocked, capped, and
  refuted Pin rows create no standalone claim. Pin-6 opens no renderer wording,
  LLM smoke, public route `200`, production API, or public/user-facing LLM
  narration.
- Pin-7 is owned by `StoryInteractionLaw.md`. Pin-7 opens only deterministic
  renderer text for selected `Tactic.Pin` ExplanationPlan. Pin-7 renderer input
  is ExplanationPlan only and may render `{route} pins the piece on {target}.`
  Pin-7 forbids cannot move, the king is unsafe, wins material, winning,
  decisive, best move, only move, forces, creates pressure, and threatens mate.
  Pin-7 opens no raw Verdict, raw Story, PinProof, LineFact, LineProof,
  EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or
  public/user-facing LLM narration.
- Pin-8 is owned by `StoryInteractionLaw.md`. Pin-8 opens only LLM smoke for
  selected Pin ExplanationPlan and RenderedLine. Pin-8 reuses only the existing
  8B Codex CLI prompt smoke contract with renderedText, claimKey, strength,
  forbidden wording, and `Rephrase only. Do not add chess facts.` Pin-8 forbids
  raw Story, raw PinProof, raw LineProof, BoardFacts, EngineCheck, raw PV,
  proofFailures, new move, new line, mate, pressure, initiative, winning, and
  cannot-move claims. Pin-8 opens no public/user-facing LLM narration, public
  route `200`, production API, raw proof repair, or engine explanation.
- Pin Closeout opens no new chess meaning. It audits only that LineFact
  observes geometry, LineProof binds line evidence, PinProof proves one
  pinned-to-king relation, Tactic.Pin owns the Story identity, `pins_piece`
  owns speech vocabulary, renderer/LLM stay no stronger than the selected
  ExplanationPlan, test helpers are not runtime authority, detailed Pin
  closeout authority stays in `StoryInteractionLaw.md`, and public route `200`,
  production API, and public/user-facing LLM narration remain closed.
- RemoveGuard-0 is owned by `StoryInteractionLaw.md`. RemoveGuard-0 opens only
  the charter for the third narrow line/defender contact vertical slice. The
  first positive RemoveGuard scope is not a broad remove-the-guard motif. The
  RemoveGuard first scope is a legal move that removes one defender from one
  non-king material target. First runtime positive path stays centered on
  defender capture when possible; deflection is allowed only when exact-board
  proof immediately after the same move shows the defender no longer guards the
  target. BoardFacts observes guard relation, RemoveGuardProof proves the guard
  was removed, and Tactic.RemoveGuard may speak only after proof. RemoveGuard-0
  opens only narrow Tactic.RemoveGuard, one non-king material target, one
  defender, one legal move that removes the defender guard relation, and
  bounded remove-guard wording after selected Verdict only. RemoveGuard-0 opens
  no broad deflection tactic, overloaded defender theory, discovered attack
  expansion, Pin/Skewer/XRay expansion, material win claim, winning, decisive,
  forced, best move, only move, no defense, refutes defense, collapses position,
  pressure, initiative, public route `200`, production API, or
  public/user-facing LLM narration.
- RemoveGuard-1 is owned by `StoryInteractionLaw.md`. RemoveGuard-1 opens only
  `RemoveGuardProof` as a narrow proof home. RemoveGuardProof proves side
  removing the guard, rival side, guarded target, removed defender, legal
  remove-guard move, target non-king material piece, defender guarded target
  before move, after move defender no longer guards target, same-board proof,
  and exact-board after-move relation. RemoveGuard-1 permits DefenderCaptured
  first and GuardLineBlocked only when one legal move blocks a slider defender
  guard line and exact-board proof shows the defender no longer guards the
  target. RemoveGuard-1 keeps opponent-reply deflection, sacrifice lure,
  overloaded defender, remove guard by long tactic sequence, and
  defender-cannot-defend general theory closed. RemoveGuardProof is not a
  public Story and owns no material result, and proofFailures stay out of
  renderer/LLM input. RemoveGuard-1 opens no Tactic.RemoveGuard writer,
  StoryTable integration, ExplanationPlan mapping, renderer wording, LLM
  smoke, material win claim, public route `200`, production API, or
  public/user-facing LLM narration.
- RemoveGuard-2 is owned by `StoryInteractionLaw.md`. RemoveGuard-2 opens only
  the named `TacticRemoveGuard` writer for one narrow `Tactic.RemoveGuard`
  Story. TacticRemoveGuard requires complete StoryProof, complete
  RemoveGuardProof, same-board legal replay, legal remove-guard move, guarded
  target, removed defender that guarded the target before move, defender no
  longer guards target after move, writer identity, and no EngineCheck Refutes
  status. RemoveGuard Story identity is tactic RemoveGuard, scene Tactic,
  guard-removing side, target/defender-side rival, guarded target square,
  removed-defender or moving-piece anchor, and remove-guard route.
  RemoveGuard-2 opens no Scene.Material claim, Tactic.Hanging replacement,
  Defense refutation wording, material win claim, ExplanationPlan mapping,
  renderer wording, LLM smoke, public route `200`, production API, or
  public/user-facing LLM narration.
- RemoveGuard-3 is owned by `StoryInteractionLaw.md`. RemoveGuard-3 opens only
  the negative corpus for the narrow `Tactic.RemoveGuard` slice. RemoveGuard-3
  keeps illegal moves, missing same-board proof, king targets, non-guarding
  defenders, still-guarding defenders, broad claims from other defenders,
  material-gain claims, Pin/DiscoveredAttack/Skewer misclassification,
  opponent-reply deflection, overloaded-defender claims, and no-defense,
  wins-material, and best-move wording silent. Removing one guard is not
  winning material. Complete guard-removal proof or silence. RemoveGuard-3
  opens no new proof home, new writer, StoryTable ordering change,
  ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`,
  production API, or public/user-facing LLM narration.
- RemoveGuard-4 is owned by `StoryInteractionLaw.md`. RemoveGuard-4 opens only
  existing `EngineCheck` reuse for existing `Tactic.RemoveGuard` Stories.
  EngineCheck cannot create RemoveGuard; Supports creates no new claim; Caps
  suppresses standalone claim or keeps downstream speech bounded when opened
  later; Refutes blocks the RemoveGuard Story; Unknown creates no engine
  expression. RemoveGuard-4 opens no engine-says wording, best-move wording,
  only-move wording, winning tactic, raw PV explanation, eval number public
  value, ExplanationPlan mapping, renderer wording, LLM smoke, public route
  `200`, production API, or public/user-facing LLM narration.
- RemoveGuard-5 is owned by `StoryInteractionLaw.md`. RemoveGuard-5 opens only
  StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`,
  `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`,
  and `Tactic.RemoveGuard` rows. RemoveGuard-5 keeps selected Verdict stable
  across input order, keeps RemoveGuard from owning Material or Hanging claims,
  blocks incomplete Defense responses, prevents duplicate Lead for same-line
  Pin and RemoveGuard, and keeps actual material change now in Scene.Material.
  RemoveGuard-5 opens no new proof home, new Story family, Material claim from
  RemoveGuard, Hanging claim from RemoveGuard, incomplete Defense response
  claim, ExplanationPlan mapping, renderer wording, LLM smoke, public route
  `200`, production API, or public/user-facing LLM narration.
- RemoveGuard-6 is owned by `StoryInteractionLaw.md`. RemoveGuard-6 opens only
  ExplanationPlan mapping for selected uncapped Lead `Tactic.RemoveGuard`
  Verdicts. RemoveGuard-6 allows only the `removes_defender` claim key and
  forbids wins_material, target_is_hanging, no_defense, refutes_defense,
  best_move, only_move, forced, decisive, creates_pressure, and
  takes_initiative. Support, Context, Blocked, capped, and refuted RemoveGuard
  rows create no standalone claim. RemoveGuard-6 opens no renderer wording,
  LLM smoke, public route `200`, production API, or public/user-facing LLM
  narration.
- RemoveGuard-7 is owned by `StoryInteractionLaw.md`. RemoveGuard-7 opens only
  deterministic renderer text for selected `Tactic.RemoveGuard`
  ExplanationPlan. RemoveGuard-7 renderer input is ExplanationPlan only and may
  render `{route} removes the defender of the piece on {target}.`
  RemoveGuard-7 forbids wins material, leaves it undefended, no defender
  remains, best move, only move, forces, decisive, refutes the defense, and
  creates pressure. RemoveGuard-7 opens no raw Verdict, raw Story,
  RemoveGuardProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public
  route `200`, production API, or public/user-facing LLM narration.
- RemoveGuard-8 is owned by `StoryInteractionLaw.md`. RemoveGuard-8 opens only
  LLM smoke for selected RemoveGuard ExplanationPlan and RenderedLine.
  RemoveGuard-8 reuses only the existing 8B Codex CLI prompt smoke contract
  with renderedText, claimKey, strength, forbidden wording, and
  `Rephrase only. Do not add chess facts.` RemoveGuard-8 forbids raw Story,
  raw RemoveGuardProof, BoardFacts, EngineCheck, raw PV, proofFailures, new
  move, new line, material-win, no-defense, pressure, and initiative claims.
  RemoveGuard-8 opens no public/user-facing LLM narration, public route `200`,
  production API, raw proof repair, or engine explanation.
- RemoveGuard Closeout opens no new chess meaning. It audits only that
  BoardFacts guard relation observes same-side guard contact, RemoveGuardProof
  proves one removed guard relation, Tactic.RemoveGuard owns the Story
  identity, `removes_defender` owns speech vocabulary, RemoveGuard owns no
  Material, Hanging, Defense, Pin, or DiscoveredAttack meaning, deflection,
  overload, no-defender, and wins-material terms are not live public authority,
  renderer/LLM stay no stronger than the selected ExplanationPlan, test helpers
  are not runtime authority, detailed RemoveGuard closeout authority stays in
  `StoryInteractionLaw.md`, and public route `200`, production API, and
  public/user-facing LLM narration remain closed.
- Line / Defender Interaction Hardening opens no new Story. It proves that
  existing DiscoveredAttack, Pin, and RemoveGuard rows do not steal each
  other's meaning or the existing Hanging, Fork, Material, and Defense claim
  homes. LDH-0 may apply only the minimum StoryTable ordering fix if an
  existing DiscoveredAttack ordering bug is exposed.
- LDH-1 opens only the Fixture Map for complex same-board Line/Defender
  interaction fixtures. Each fixture states same-board FEN, side to move,
  candidate legal lines, expected open rows, expected blocked rows, expected
  Lead, Support, Context, or Blocked role, expected selected Verdict, and
  forbidden claims. The required LDH-1 categories are DiscoveredAttack vs Pin,
  DiscoveredAttack vs RemoveGuard, Pin vs RemoveGuard, DiscoveredAttack + Pin
  + RemoveGuard same-board, Line/Defender row vs Material, Line/Defender row
  vs Hanging, Line/Defender row vs Defense, and EngineCheck
  Supports/Caps/Refutes over existing Line/Defender rows. LDH-1 does not open
  Skewer, XRay, broad LineTactic, broad deflection, overloaded defender,
  pressure, initiative, mate threat, king safety, material win claim, public
  route `200`, production API, or public/user-facing LLM narration.
- LDH-2 opens only StoryTable role stability checks over existing Line/Defender
  rows. It verifies input-order stable selected Verdicts, no duplicate Lead for
  the same meaning, incomplete rows not Lead, refuted rows Blocked, capped rows
  without standalone claims, and separated ownership for same-line
  Pin/DiscoveredAttack, Pin/RemoveGuard, and DiscoveredAttack/RemoveGuard
  collisions. LDH-2 does not open Skewer, XRay, broad LineTactic, broad
  deflection, overloaded defender, pressure, initiative, mate threat, king
  safety, material win claim, public route `200`, production API, or
  public/user-facing LLM narration.
- LDH-3 opens only Meaning Ownership Boundary checks over existing
  Line/Defender and collision rows. It fixes each row to its own meaning:
  DiscoveredAttack reveals one slider attack on one material target, Pin pins
  one non-king piece to its own king on a line, RemoveGuard removes one
  defender guard relation from one target, Scene.Material owns actual material
  balance change now, Tactic.Hanging owns capturable target with bounded
  material gain proof, and Scene.Defense owns complete ThreatProof plus
  DefenseProof preventing immediate material loss. LDH-3 forbids RemoveGuard
  material-gain claims, Pin cannot-move or king-unsafe claims, DiscoveredAttack
  wins-material claims, Material line-tactic identity, and Defense
  stopped-threat wording without complete threat proof. LDH-3 does not open
  Skewer, XRay, broad LineTactic, broad deflection, overloaded defender,
  pressure, initiative, mate threat, king safety, material win claim, public
  route `200`, production API, or public/user-facing LLM narration.
- LDH-4 opens only existing EngineCheck interaction checks over existing
  Line/Defender rows. It verifies Supports creates no new claim, Caps
  suppresses allowed claim or keeps downstream speech bounded, Refutes blocks
  the checked Story, and Unknown creates no engine-related expression. LDH-4
  forbids engine-says wording, raw PV explanation, eval number public value,
  best move, only move, forced line, Skewer, XRay, broad LineTactic, broad
  deflection, overloaded defender, pressure, initiative, mate threat, king
  safety, material win claim, public route `200`, production API, or
  public/user-facing LLM narration. LDH-4 does not open a new EngineCheck proof
  home, new Story family, engine-owned wording, public route `200`, production
  API, or public/user-facing LLM narration.
- LDH-5 opens only close false-positive negative corpus tests over existing
  Line/Defender rows and already-open collision rows. It keeps
  line-open-without-attack, king targets, pin-looking lines without
  king-behind-target, still-guarding defenders, incomplete Material or Hanging
  proof after defender removal, incomplete DiscoveredAttack or Pin proof,
  wrong-board or stale same-board proof, route mismatch, engine-refuted
  plausible rows, and Skewer-looking relations silent. LDH-5 rule: Looks like
  a line tactic is not enough. Existing complete proof or silence. LDH-5 does
  not open a new Story family, proof home, Skewer, XRay, broad LineTactic,
  broad deflection, overloaded defender, pressure, initiative, mate threat,
  king safety, material win claim, public route `200`, production API, or
  public/user-facing LLM narration.
- LDH-6 opens only downstream boundary smoke over selected Lead Verdicts from
  existing Line/Defender rows. It verifies ExplanationPlan receives selected
  Verdict only, Renderer receives ExplanationPlan only, LLM smoke prompt
  carries only renderedText, claimKey, strength, forbidden wording, and the
  rephrase-only instruction, and Support, Context, Blocked, capped, and
  refuted rows create no standalone text. LDH-6 forbids new renderer
  templates, new LLM behavior, and raw Story, Proof, or EngineCheck passing
  into renderer or LLM smoke. LDH-6 does not open a new Story family, proof
  home, renderer template, LLM behavior, raw Story, Proof, or EngineCheck
  downstream path, Skewer, XRay, broad LineTactic, broad deflection,
  overloaded defender, pressure, initiative, mate threat, king safety,
  material win claim, public route `200`, production API, or
  public/user-facing LLM narration.
- LDH-7 opens only diagnostics boundary smoke over existing Line/Defender
  rows, StoryTable, selected Verdict, ExplanationPlan, renderer, LLM smoke,
  and test-helper authority. It verifies proofFailures stay internal
  diagnostic only, raw proof text stays out of Verdict.values, EngineCheck text
  does not flow directly into ExplanationPlan, StoryTable debug relations do
  not become renderer wording, and test helpers do not become runtime
  authority. LDH-7 does not open a new Story family, proof home, renderer
  wording, LLM behavior, runtime authority helper, raw Story, raw Proof, raw
  EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad
  deflection, overloaded defender, pressure, initiative, mate threat, king
  safety, material win claim, public route `200`, production API, or
  public/user-facing LLM narration.
- LDH Closeout opens no chess meaning. It only audits the Line / Defender
  Interaction Hardening surface. LDH closes as interaction hardening only,
  with no new Story family, no new proof home, no mixed LineFact, LineProof,
  PinProof, or RemoveGuardProof authority, no Line/Defender meaning theft, no
  Material, Hanging, or Defense claim-home invasion, no broad-term authority,
  no duplicated live rule authority outside StoryInteractionLaw.md, no public
  route 200, no production API, and no public/user-facing LLM narration.
- Skewer-0 is owned by `StoryInteractionLaw.md`. Skewer-0 opens only the
  charter for the fourth narrow line/defender contact vertical slice. The
  first Skewer positive scope is not a broad skewer tactic. The Skewer first
  scope is a legal move that creates or reveals a slider attack on one front
  non-king material target, with a second non-king material target behind it
  on the same line. LineFact observes geometry, SkewerProof proves the
  front-and-back target relation, and Tactic.Skewer may speak only after
  proof. Skewer-0 opens only narrow Tactic.Skewer, one slider, one front
  target, one rear target, front/rear target same-line relation, a legal move
  that creates or reveals the front/rear relation, and bounded skewer wording
  after selected Verdict only. Skewer-0 opens no broad LineTactic, XRay public
  Story, Pin expansion, RemoveGuard expansion, material win claim, front piece
  must move, wins rear piece, forced line, best move, only move, winning,
  decisive, king safety, mate threat, pressure, initiative, public route `200`,
  production API, or public/user-facing LLM narration.
- Skewer-1 is owned by `StoryInteractionLaw.md`. Skewer-1 opens only
  `SkewerProof` as a narrow proof home. SkewerProof proves side creating the
  skewer, rival side, skewer slider, front target, rear target, legal skewer
  or revealing move, line kind, same-board proof, front target non-king
  material piece, rear target non-king material piece, front and rear target
  same rival side, after move slider attacks front target, rear target behind
  front target on the same ray, no extra blocker breaks the front-to-rear
  relation, and before move the skewer relation was absent or blocked.
  SkewerProof is not a public Story, and LineFact and LineProof are not public
  Stories. SkewerProof says no material gain, front piece must move, or wins
  rear piece; proofFailures stay out of renderer/LLM input. Skewer-1 opens no
  Tactic.Skewer writer, StoryTable integration, ExplanationPlan mapping,
  renderer wording, LLM smoke, material gain claim, front piece must move
  wording, wins rear piece claim, public route `200`, production API, or
  public/user-facing LLM narration.
- Skewer-2 is owned by `StoryInteractionLaw.md`. Skewer-2 opens only the
  named `TacticSkewer` writer for one narrow `Tactic.Skewer` Story.
  TacticSkewer requires complete StoryProof, complete SkewerProof, same-board
  legal replay, legal skewer or revealing move, front target, rear target,
  slider, complete front-and-back line relation, writer identity, and no
  EngineCheck Refutes status. Skewer Story identity is tactic Skewer, scene
  Tactic, skewer-creating side, front/rear target side as rival, front target
  square, rear target square as secondaryTarget, slider or moved-piece anchor,
  and skewer/revealing route. TacticSkewer creates no Scene.Material claim,
  does not replace Tactic.Pin, and keeps rear-target king positions silent.
  Skewer-2 opens no StoryTable Lead admission, ExplanationPlan mapping,
  renderer wording, LLM smoke, material gain claim, front piece must move
  wording, wins rear piece claim, Pin replacement, public route `200`,
  production API, or public/user-facing LLM narration.
- Skewer-3 is owned by `StoryInteractionLaw.md`. Skewer-3 opens only the
  negative corpus for the narrow `Tactic.Skewer` slice. Skewer-3 keeps
  illegal moves, missing same-board proof, non-sliders, missing front target,
  missing rear target, front or rear king targets, front/rear targets not on
  the same rival side, rear targets not behind the front target on the same
  line, extra blockers between front and rear target, DiscoveredAttack-only
  lines, Pin-looking positions, front-piece-must-move assumptions, and
  material-win, forced, or best-move wording silent. Two pieces on a line is
  not enough. Complete front-and-back skewer proof or silence. Skewer-3 opens
  no new proof home, new writer, StoryTable Lead admission, ExplanationPlan
  mapping, renderer wording, LLM smoke, Scene.Material claim, Pin replacement,
  front piece must move wording, wins rear piece claim, public route `200`,
  production API, or public/user-facing LLM narration.
- Skewer-4 is owned by `StoryInteractionLaw.md`. Skewer-4 opens only
  existing `EngineCheck` reuse for existing `Tactic.Skewer` Stories.
  EngineCheck cannot create Skewer; Supports creates no new claim; Caps
  suppresses standalone claim or keeps downstream speech bounded when opened
  later; Refutes blocks the Skewer Story; Unknown creates no engine
  expression. Skewer-4 opens no engine-says wording, best-move wording,
  only-move wording, forced-win wording, winning tactic, raw PV explanation,
  eval number public value, StoryTable Lead admission, ExplanationPlan
  mapping, renderer wording, LLM smoke, public route `200`, production API, or
  public/user-facing LLM narration.
- Skewer-5 is owned by `StoryInteractionLaw.md`. Skewer-5 opens only
  StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`,
  `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`,
  `Tactic.RemoveGuard`, and `Tactic.Skewer` rows. Skewer-5 keeps selected
  Verdict stable across input order, prevents duplicate Lead for
  DiscoveredAttack and Skewer, keeps Skewer from owning Material,
  Pin king-relation, or RemoveGuard defender-relation claims, keeps actual
  material change now in Scene.Material, and keeps incomplete front/rear
  relations silent so only complete existing rows remain. Skewer-5 opens no
  new proof home, new Story family, broad LineTactic, broad XRay, Material
  claim from Skewer, DiscoveredAttack duplicate Lead from Skewer, Pin king
  relation from Skewer, RemoveGuard defender relation from Skewer,
  ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`,
  production API, or public/user-facing LLM narration.
- Skewer-6 is owned by `StoryInteractionLaw.md`. Skewer-6 opens only
  ExplanationPlan mapping for selected uncapped Lead `Tactic.Skewer` Verdicts.
  Skewer-6 allows only the `skewers_piece_to_piece` claim key and forbids
  wins_material, wins_rear_piece, front_piece_must_move, best_move, only_move,
  forced, decisive, king_unsafe, mate_threat, creates_pressure, and
  takes_initiative. Support, Context, Blocked, capped, and refuted Skewer rows
  create no standalone claim. Skewer-6 opens no renderer wording, LLM smoke,
  public route `200`, production API, or public/user-facing LLM narration.
- Skewer-7 is owned by `StoryInteractionLaw.md`. Skewer-7 opens only
  deterministic renderer text for selected `Tactic.Skewer` ExplanationPlan.
  Skewer-7 renderer input is ExplanationPlan only and may render `{route}
  skewers the piece on {target} to the piece on {secondaryTarget}.` Skewer-7
  forbids wins material, wins the piece behind it, the front piece must move,
  best move, only move, forces, decisive, king is unsafe, threatens mate, and
  creates pressure. Skewer-7 opens no raw Verdict, raw Story, SkewerProof,
  LineFact, LineProof, BoardFacts, EngineCheck, proofFailures, LLM smoke,
  public route `200`, production API, or public/user-facing LLM narration.
- Skewer-8 is owned by `StoryInteractionLaw.md`. Skewer-8 opens only LLM smoke
  for selected Skewer ExplanationPlan and RenderedLine. Skewer-8 reuses only
  the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey,
  strength, forbidden wording, and `Rephrase only. Do not add chess facts.`
  Skewer-8 forbids raw Story, raw SkewerProof, raw LineProof, BoardFacts,
  EngineCheck, raw PV, proofFailures, new move, new line, material-win, forced,
  pressure, initiative, and mate claims. Skewer-8 opens no public/user-facing
  LLM narration, public route `200`, production API, raw proof repair, or
  engine explanation.
- Skewer Closeout opens no new chess meaning. It audits only LineFact,
  LineProof, SkewerProof, Tactic.Skewer, speech key, renderer/LLM, docs
  authority, test-helper boundary, and closed public surfaces. It confirms
  Skewer owns no Material, Hanging, Pin, DiscoveredAttack, RemoveGuard, or
  Defense meaning; front-piece-must-move, wins-rear-piece, wins-material, and
  forced-skewer terms are not live authority; renderer/LLM wording stays no
  stronger than `skewers_piece_to_piece`; public route `200`, production API,
  and public/user-facing LLM narration remain closed. It also audits meaning,
  authority, terminology, and document duplication without duplicating detailed
  rules outside StoryInteractionLaw.md.
- Line / Defender closeout summaries are non-authoritative:
  `StoryInteractionLaw.md` owns LNC-0 through LNC-7 and final completion
  detail, while this document only records that `Tactic.DiscoveredAttack`, `Tactic.Pin`,
  `Tactic.RemoveGuard`, and `Tactic.Skewer` close as four narrow proof-backed
  slices with broad line/ray/XRay, pressure, initiative, material-win, public
  route `200`, production API, and public/user-facing LLM narration surfaces
  closed.
- Current implementation scope is Pawn / Promotion Neighborhood third vertical slice.
- PawnAdvance-0 is owned by `StoryInteractionLaw.md`. It opens only narrow
  `Scene.PawnAdvance` over `PawnAdvanceProof` for an already-passed non-king
  pawn making a legal one-square non-capturing non-promotion advance and
  remaining passed on the exact after-board. The only bounded claim key is
  `advances_passed_pawn`; broad PawnTactic, `Tactic.PawnPush`, promotion
  threat, unstoppable pawn, winning endgame, conversion, pawn race, strategy,
  public route `200`, production API, and public/user-facing LLM narration
  remain closed.
- PawnStop-0 is owned by `StoryInteractionLaw.md`. It opens only narrow
  `Scene.PawnStop` over `PawnStopProof` for a legal move directly stopping an
  already-passed pawn's next non-promotion advance square on the exact
  after-board. The only bounded claim key is
  `stops_pawn_advance`; broad pawn defense, promotion stop,
  permanent stop, tablebase draw, best defense, only move, endgame hold,
  conversion stopped, pawn race, king route, opposition, broad pawn strategy,
  public route `200`, production API, and public/user-facing LLM narration
  remain closed.
- PawnStop Closeout opens no new chess meaning; it audits only
  `PassedPawnObservation`, `PawnStopProof`, `Scene.PawnStop`,
  `stops_pawn_advance`, renderer/LLM bounds, docs authority, and closed
  public surfaces. PawnStop Closeout confirms PawnStop owns no PawnAdvance,
  PromotionThreat, Promotion, PawnBreak, Defense, Material, Hanging, or
  Line/Defender tactic meaning; permanent stop, draw, tablebase, best defense,
  and only move remain forbidden wording, not live authority.
- PromotionThreat-0 is owned by `StoryInteractionLaw.md`. It opens only narrow
  `Scene.PromotionThreat` over `PromotionThreatProof` for a legal pawn move
  that creates a legal next-move promotion route on the exact after-board. The
  only bounded claim key is `threatens_promotion_next`; actual Promotion, unstoppable
  pawn, cannot-be-stopped, winning endgame, conversion, tablebase, pawn race,
  best move, only move, forced win, no-counterplay, public route `200`,
  production API, and public/user-facing LLM narration remain closed.
- PIH-0 is owned by `StoryInteractionLaw.md`. It opens no new pawn meaning and
  hardens existing `Scene.PawnAdvance` and `Scene.PawnStop` interactions so
  pawn rows do not steal promotion, conversion, defense, material, or tactic
  meaning. Public route `200`, production API, and public/user-facing LLM
  narration remain closed.
- PIH-1 is owned by `StoryInteractionLaw.md`. It opens only the Pawn
  Interaction Hardening fixture map over already-open rows and keeps
  PromotionThreat, Promotion, PawnBreak, tablebase, endgame-result, public route
  `200`, production API, and public/user-facing LLM narration closed.
- PIH-2 is owned by `StoryInteractionLaw.md`. It opens only StoryTable role
  stability checks over already-open pawn and collision rows, including
  input-order stability, duplicate Lead prevention, incomplete/refuted/capped
  row boundaries, and pawn rows not owning Defense, promotion, or conversion
  claims.
- PIH-3 is owned by `StoryInteractionLaw.md`. It opens only Meaning Ownership
  Boundary checks proving each already-open pawn, material, defense, and
  line/defender row keeps its own claim and cannot steal promotion, conversion,
  endgame, defense, pawn progress, or sibling tactic meaning.
- PIH-4 is owned by `StoryInteractionLaw.md`. It opens only EngineCheck
  interaction checks over already-open PawnAdvance and PawnStop rows, reusing
  existing Supports, Caps, Refutes, and Unknown boundaries without engine-owned
  claims, raw PV, eval numbers, best move, only move, tablebase-like authority,
  public route `200`, production API, or public/user-facing LLM narration.
- PIH-5 is owned by `StoryInteractionLaw.md`. It opens only the close pawn
  false-positive negative corpus and enforces existing complete proof or silence
  for not-passed advances, missing exact after-board replay, stop-looking
  non-stops, long-term blockade, promotion-looking, tablebase-looking,
  opposition-looking, pawn-race-looking, route mismatch, stale/wrong-board, and
  refuted pawn rows.
- PIH-6 is owned by `StoryInteractionLaw.md`. It opens only downstream boundary
  smoke for already-open PawnAdvance and PawnStop rows, sends only selected Lead
  Verdict data through ExplanationPlan, renderer, and LLM smoke, and keeps
  Support, Context, Blocked, capped, and refuted rows textless while forbidding
  promotion, unstoppable, winning, conversion, draw/hold, tablebase, best/only,
  forced, pressure, initiative, public route `200`, production API, and
  public/user-facing LLM narration.
- PIH-7 is owned by `StoryInteractionLaw.md`. It opens only diagnostics
  boundary hardening for already-open PawnAdvance and PawnStop rows, keeping
  proofFailures, raw proof text, EngineCheck text, StoryTable debug relations,
  and test-helper terminology out of Verdict values, ExplanationPlan, renderer
  wording, LLM smoke, production API, public route `200`, and public/user-facing
  LLM narration.
- PIH Closeout is owned by `StoryInteractionLaw.md`. It opens no new pawn
  meaning, Story family, proof home, Story writer, claim key, renderer wording,
  LLM behavior, public route `200`, production API, or public/user-facing LLM
  narration; it confirms PawnAdvance and PawnStop remain separated from each
  other, promotion, pawn-break, tablebase, race, king-route, opposition,
  Material, Defense, Hanging, and Line/Defender homes.
- Stage 6 closeout confirms Explanation Plan only. Blocked, Support, Context,
  engine-capped, and engine-refuted Verdicts create no allowed claim or public
  claim. Stage 7 deterministic renderer may receive Explanation Plan only and
  must not read raw Verdict, EngineCheck, CaptureResult, Board Facts,
  BoardMood, raw PV, proofFailures text, source rows, or raw engine evidence
  directly.
- Stage 7-0 opens only the Deterministic Renderer charter. It admits
  `ExplanationPlan` only input, deterministic template, `Tactic.Hanging`
  bounded claim phrasing, forbidden wording check, no LLM, and no public
  route. It must not admit raw Verdict, Story, Board Facts, CaptureResult,
  EngineCheck, EngineEval, EngineLine, raw PV, proofFailures text, source
  rows, user-level pedagogy, engine PV explanation, best-move explanation,
  engine-says wording, winning, decisive, forced, blunder, or free-piece
  wording.
- Stage 7-1 opens only the Renderer input guard. Renderer receives
  `ExplanationPlan` only and exposes no raw Verdict, Story, BoardFacts,
  BoardMood, CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV,
  proofFailures, or source-row input.
- Stage 7-2 opens only the minimal `Tactic.Hanging` template for the
  `can_win_piece` claim key. It requires Lead role, `Bounded` strength,
  non-debug plan, route, target, evidenceLine, and forbidden wording. The
  first deterministic text must not exceed the ExplanationPlan claim key or
  evidenceLine.
- Stage 7-3 opens only forbidden wording enforcement. Renderer output must not
  violate `ExplanationPlan.forbiddenWording`. `win material` wording is
  allowed only when `allowedClaim` is `CanWinPiece`, `winning position`
  remains forbidden, engine-strength-limited plans must fail strong wording
  output, and plans with no allowedClaim or debugOnly true produce no output.
- Stage 7-4 opens only the no-standalone-text boundary. Renderer phrases only
  Lead plans with an allowed claim. Support, Context, Blocked, capped
  no-claim, and engine-refuted relation plans produce no text.
- Stage 7-5 opens only the RenderedLine shape. `RenderedLine` owns no chess
  meaning, proof, or engine data; RenderedLine is only the expression result
  of ExplanationPlan. It carries text, claim key, strength, and
  forbidden-check result only.
- Stage 7-6 opens only renderer baseline tests. Renderer output is no stronger
  than ExplanationPlan, and there is no new renderer wording, no new input, no
  public route `200`, and no LLM narration.
- Stage 7 closeout confirms deterministic renderer is closed as a template
  baseline. Stage 8 opens only 8A Mock narrator and 8B Codex CLI prompt smoke
  test. Stage 8B Codex CLI prompt smoke may receive renderedText, claimKey,
  strength, forbidden wording list, and the instruction `Rephrase only. Do not
  add chess facts.` only. 8A Mock narrator may receive ExplanationPlan and
  RenderedLine only. Production API validation remains closed. Stage 8 must
  not read raw Verdict, Story, EngineCheck, CaptureResult, Board Facts,
  BoardMood, raw PV, proofFailures text, or source rows directly.
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

Only the named `Tactic.Hanging` writer, the narrow named `Tactic.Fork`
writer, and the narrow named `Scene.Material` writer are live in this checkpoint. Numeric `Proof` scores may rank
blocked/context `Verdict` rows only; they cannot set `leadAllowed=true` or
produce `Role.Lead` unless the Story has its named writer, complete
StoryProof, same-board proof, and family proof: positive `CaptureResult` for
Hanging, complete `MultiTargetProof` for Fork, or complete `CaptureResult` for
Material.

Missing side, target, anchor, route, rival, required legal line, or same-root
proof sidecar is a hard public-output block, not weak scoring, deferred work,
or renderer repair.

## Verdict Layout

`Verdict.values` has exactly `99` slots:

- `0`: role code
- `1`: rank
- `2`: lead-allowed bit
- `3`: strength
- `4`: side ordinal
- `5`: rival ordinal
- `6`: target square index + 1, or `0` absent
- `7`: anchor square index + 1, or `0` absent
- `8..24`: scene one-hot
- `25..56`: plan one-hot
- `57..81`: tactic one-hot
- `82..97`: all `16` proof values

Route identity remains in `Verdict.story.route` and in `Story.values`; it is not
duplicated in the `99`-slot verdict vector.

## Scenes

The model has exactly `18` public scene families:

- `Tactic`
- `Blunder`
- `Material`
- `King`
- `Defense`
- `Opening`
- `Pawns`
- `PawnAdvance`
- `PawnStop`
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

The model has exactly `25` tactic families:

- `Loose`
- `Hanging`
- `AbsPin`
- `RelPin`
- `Pin`
- `Skewer`
- `Xray`
- `Fork`
- `DiscoveredAttack`
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
