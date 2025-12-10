# Chess Feature Definitions

This document defines the chess features extracted by `FeatureExtractor.scala`. These features serve as the input for `RoleLabeler` (to identify the character of the position) and `ConceptLabeler` (to identify instructional concepts).

## 1. Pawn Structure Features (`PawnStructureFeatures`)

Analysis of the pawn skeleton, which is often the most static and defining element of the position.

| Feature | Type | Definition |
| :--- | :--- | :--- |
| **Isolated Pawn (IQP included)** | Int | A pawn with no friendly pawns on adjacent files. |
| **Doubled Pawn** | Int | A pawn that has another friendly pawn on the same file in front of or behind it. |
| **Backward Pawn** | Int | A pawn that cannot safely advance (stop square controlled by enemy) and has no friendly pawns on adjacent files behind/beside it to support. |
| **Passed Pawn** | Int | A pawn with no opposing pawns on its file or adjacent files in front of it. |
| **Hanging Pawns** | Boolean | Two adjacent friendly pawns on the 4th rank (typically c & d files) separated from other friendly pawns. |
| **IQP (Isolated Queen Pawn)** | Boolean | Specifically an isolated pawn on the d-file (d4 for White, d5 for Black). |
| **Minority Attack Ready** | Boolean | Side has fewer pawns on the queenside (a-c files) than the opponent, creating a target structure. |
| **Central Fixed** | Boolean | Central pawns (d, e files) are blocked by enemy pawns, creating a closed center. |

## 2. King Safety Features (`KingSafetyFeatures`)

Measures of the king's vulnerability.

| Feature | Type | Definition |
| :--- | :--- | :--- |
| **Castling State** | Enum | Current castling rights (Can Castle Short/Long, Castled, None). |
| **King Shield** | Int | Number of friendly pawns directly protecting the castled king (files f, g, h for kingside). |
| **King Exposed Files** | Int | Number of open or semi-open files adjacent to the king. |
| **Back Rank Weakness** | Boolean | King is on the back rank with no flight square and no immediate protection against rook/queen checks. |
| **King Ring Pressure** | Int | Number of enemy pieces attacking squares adjacent to the king. |

## 3. Activity Features (`ActivityFeatures`)

Measures of piece mobility and influence.

| Feature | Type | Definition |
| :--- | :--- | :--- |
| **Legal Moves** | Int | Total number of legal moves available. Indicator of space and freedom. |
| **Mobility (Minor/Rook/Queen)** | Int | Number of legal moves for specific piece types. |
| **Center Control** | Int | Number of central squares (d4, d5, e4, e5) attacked or occupied. |
| **Rook on Open File** | Int | Number of rooks on files with no pawns of either color. |
| **Knight Outpost** | Int | Knight on a protected square (ranks 4-5 for White) that cannot be attacked by enemy pawns. |
| **Bad Bishop** | Int | Bishop blocked by its own pawns on the same color squares. |
| **Bishop Pair** | Boolean | Possession of both light-squared and dark-squared bishops. |

## 4. Material & Phase (`MaterialPhaseFeatures`)

| Feature | Type | Definition |
| :--- | :--- | :--- |
## 5. Development & Formation (`DevelopmentFeatures`)

Measures of piece deployment and coordination.

| Feature | Type | Definition |
| :--- | :--- | :--- |
| **Undeveloped Minors** | Int | Number of Knights/Bishops still on their starting squares (b1/g1/c1/f1 for White). |
| **Undeveloped Rooks** | Int | Number of Rooks still on starting squares (a1/h1). |
| **Rooks Connected** | Boolean | Rooks are on the same rank/file with no pieces between them (typically back rank or 7th). |
| **Castled** | Boolean | Has the king successfully castled? |

## 6. Space & Control (`SpaceFeatures`)

Measures of territorial dominance.

| Feature | Type | Definition |
| :--- | :--- | :--- |
| **Central Space** | Int | Number of central squares (c3-f6 box) controlled by pawns. |
| **Enemy Camp Presence** | Int | Number of pieces occupying squares in the opponent's half of the board (ranks 5-8 for White). |
| **Restrictive Pawns** | Int | Number of pawns advanced to rank 5 or 6 that restrict enemy piece movement (e.g., e5 pawn restricting f6/d6). |

## 7. Piece Coordination (`CoordinationFeatures`)

| Feature | Type | Definition |
| :--- | :--- | :--- |
| **Battery Aligned** | Boolean | Queen+Bishop or Queen+Rook or Rook+Rook aligned on the same file/diagonal. |
| **Rook on 7th** | Boolean | Rook occupying the 7th rank (relative). |
| **Piece Outposts** | Int | Pieces (not just Knights) established on protected outposts in enemy territory. |

## 8. Tactical Surface (`TacticalFeatures`)

Static tactical weakness indicators (heuristic).

| Feature | Type | Definition |
| :--- | :--- | :--- |
| **Hanging Pieces** | Int | Number of pieces attacked by enemy and not defended. |
| **En Prise** | Int | Number of pieces attacked by a lower value piece (e.g. Queen attacked by Pawn). |
| **Loose Pieces** | Int | Number of pieces with 0 defenders (undefended). |

