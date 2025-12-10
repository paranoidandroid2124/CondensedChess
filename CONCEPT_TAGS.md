# Analysis Concept Tags

This document defines the semantic tags used by `ConceptLabeler` to annotate chess positions. These tags drive the narrative generation and book structure.

## 1. Structure Tags (Static & Positional)
Tags describing pawn structure and long-term positional factors.

| Tag | Description | Criteria |
| :--- | :--- | :--- |
| `IqpWhite` / `IqpBlack` | Isolated Queen's Pawn | Side has an isolated d-pawn. |
| `HangingPawnsWhite` / `HangingPawnsBlack` | Hanging Pawns | Side has adjacent isolated pawns (usually c+d). |
| `MinorityAttackCandidate` | Minority Attack Potential | Side has fewer pawns on a wing suitable for creating weaknesses. |
| `CentralBreakAvailable` | Central Break Possible | A candidate move exists that pushes a central pawn (e.g., ...d5). |
| `CentralBreakSuccess` | Central Break Good | Analysis shows the break improves the position (> +0.6 CP). |
| `CentralBreakBad` | Central Break Bad | The break leads to a worse position (< -0.6 CP). |
| `SpaceAdvantageWhite/Black` | Space Advantage | Side controls significantly more space (rank 4+). |
| `KingExposedWhite/Black` | King Exposure | King has open files or weak shield. |
| `BadBishopWhite/Black` | Bad Bishop | Bishop blocked by own pawns on same color. |

## 2. Plan Tags (Strategic Goals)
Tags identifying the success or failure of strategic plans.

| Tag | Description | Criteria |
| :--- | :--- | :--- |
| `KingsideAttackGood` | Successful K-side Attack | Experiments show pushing pawns/pieces on kingside yields advantage. |
| `KingsideAttackBad` | Failed K-side Attack | Premature or unsound attack leading to disadvantage. |
| `QueensideMajorityGood` | Effective Q-side Majority | Advancing majority creates passed pawn/weakness. |
| `QueensideMajorityBad` | Ineffective Majority | Majority advance creates weakness or is blocked easily. |
| `RookLiftGood` | Successful Rook Lift | Lifting a rook leads to strong attack. |

## 3. Tactic Tags (Dynamic Patterns)
Tags for specific tactical motifs.

| Tag | Description | Criteria |
| :--- | :--- | :--- |
| `GreekGiftSound` | Sound Greek Gift | Bishop sacrifice on h7/h2 leads to winning attack. |
| `GreekGiftUnsound` | Unsound Greek Gift | Sacrifice fails. |
| `BackRankMatePattern` | Back Rank Threat | Mate threat involves back rank weakness. |
| `TacticalPatternMiss` | Missed Tactic Pattern | Player missed a specific tactical pattern (distinguished from generic Mistake). |
| `ForkSound` | Fork | Piece attacks two targets simultaneously. |
| `PinSound` | Pin | Piece is pinned to a more valuable piece. |

## 4. Mistake Tags (Errors)
Taxonomy of player errors.

| Tag | Description | Criteria |
| :--- | :--- | :--- |
| `TacticalMiss` | Missed Win | Generic tag for missing a winning tactic (high eval swing). |
| `PrematurePawnPush` | Weakening Push | Pawn push creating unnecessary weakness. |
| `PassiveMove` | Passivity | Ignoring active plan for a neutral one. |

## 5. Endgame Tags (Technique)
Tags specific to endgame technique.

| Tag | Description | Criteria |
| :--- | :--- | :--- |
| `KingActivityGood` | Active King | King moves towards center/action in pawn endgame. |
| `KingActivityIgnored` | Passive King | King moves away from center/action when it should be active. |
| `RookBehindPassedPawnObeyed` | Tarrasch Rule Obeyed | Rook placed behind passed pawn. |
| `RookBehindPassedPawnIgnored` | Tarrasch Rule Ignored | Rook placed ineffectively vs passed pawn. |
| `WrongCornerColor` | Wrong Bishop | K+B+P vs K ending with wrong bishop color. |

## 6. Transition Tags (Phase Changes)
Tags marking significant state changes.

| Tag | Description | Criteria |
| :--- | :--- | :--- |
| `EndgameTransition` | Into Endgame | Transition from middlegame to endgame. |
| `FortressStructure` | Fortress | Position is a drawn fortress despite material deficit. |
| `PositionalSacrifice` | Exchange Sac | Material sacrifice for long-term compensation. |
