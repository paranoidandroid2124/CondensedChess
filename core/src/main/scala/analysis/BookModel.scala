package chess
package analysis

import chess.analysis.ConceptLabeler.{ StructureTag, PlanTag, TacticTag, MistakeTag, EndgameTag, TransitionTag }

object BookModel:

  case class BookDiagram(
    id: String, // ply as string or unique id
    fen: String,
    roles: List[String],
    ply: Int,
    tags: TagBundle
  )

  case class TagBundle(
    structure: List[StructureTag],
    plan: List[PlanTag],
    tactic: List[TacticTag],
    mistake: List[MistakeTag],
    endgame: List[EndgameTag],
    transition: List[TransitionTag] = Nil
  )

  case class BookTurningPoint(
    ply: Int,
    side: String, // "white" | "black"
    playedMove: String,
    bestMove: String,
    evalBefore: Int,     // Eval of position before move (CP)
    evalAfterPlayed: Int, // Eval resulting from played move
    evalAfterBest: Int,   // Eval resulting from best move
    mistakeTags: List[MistakeTag]
  )

  case class BookTacticalMoment(
    ply: Int,
    side: String,
    motifTags: List[TacticTag],
    evalGainIfPlayed: Option[Int], // if missed tactic, how much gain?
    wasMissed: Boolean
  )

  enum SectionType:
    case OpeningPortrait
    case CriticalCrisis
    case StructuralDeepDive
    case TacticalStorm
    case EndgameMasterclass
    case NarrativeBridge // Generic transition

  case class BookSection(
    title: String,
    sectionType: SectionType,
    diagrams: List[BookDiagram],
    narrativeHint: String,
    startPly: Int,
    endPly: Int
  )

  case class BookStructureSection(
    dominantStructure: String, // human-readable
    structureTags: List[StructureTag],
    narrativeHint: String
  )

  case class BookEndgameSection(
    exists: Boolean,
    tags: List[EndgameTag],
    narrativeHint: String
  )

  case class ChecklistBlock(
    category: String, // "Structure & Plans", "Tactics", etc.
    hintTags: List[String] // Using String representation of Enums for simplicity or Enums themselves? User said Union of Tags. List[Any] is ugly. Let's use List[String] of tag names.
  )

  case class GameMeta(
    white: String,
    black: String,
    result: String,
    openingName: Option[String]
  )

  case class Book(
    gameMeta: GameMeta,
    sections: List[BookSection],
    // Keep summaries for quick checklists/navigation
    turningPoints: List[BookTurningPoint],
    tacticalMoments: List[BookTacticalMoment],
    checklist: List[ChecklistBlock]
  )
