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

  // Legacy section types (for backward compatibility)
  enum SectionType:
    case OpeningPortrait
    case CriticalCrisis
    case StructuralDeepDive
    case TacticalStorm
    case EndgameMasterclass
    case NarrativeBridge // Generic transition
    // New checklist-aligned types (Phase 1)
    case TitleSummary
    case KeyDiagrams
    case OpeningReview
    case TurningPoints
    case TacticalMoments
    case MiddlegamePlans
    case EndgameLessons
    case FinalChecklist

  // Typed section data for new checklist-aligned format
  sealed trait SectionData
  case class TitleSummaryData(title: String, summary: String) extends SectionData
  case class KeyDiagramsData(diagrams: List[KeyDiagram]) extends SectionData
  case class OpeningReviewData(
    structure: String,
    mainPlans: List[String],
    deviation: Option[String]
  ) extends SectionData
  case class TurningPointsData(points: List[BookTurningPoint]) extends SectionData
  case class TacticalMomentsData(moments: List[BookTacticalMoment]) extends SectionData
  case class MiddlegamePlansData(
    dominantStructure: String,
    plans: List[PlanSummary]
  ) extends SectionData
  case class EndgameLessonsData(principles: List[String]) extends SectionData
  case class FinalChecklistData(items: List[ChecklistItem]) extends SectionData
  case class LegacySectionData(diagrams: List[BookDiagram], narrativeHint: String) extends SectionData

  case class KeyDiagram(
    fen: String,
    role: String, // "opening" | "turning_point" | "tactical" | "endgame"
    tags: List[String],
    referenceMoves: List[String]
  )

  case class PlanSummary(
    planType: String,
    quality: String, // "good" | "neutral" | "bad"
    description: String
  )

  case class ChecklistItem(
    id: String,
    category: String, // "structure" | "plans" | "tactics" | "endgame"
    text: String,
    tags: List[String]
  )

  // New unified section (Phase 1 aligned with checklist)
  case class ChapterSection(
    sectionType: SectionType,
    data: SectionData
  )

  // Legacy section (kept for backward compatibility)
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
    openingName: Option[String],
    createdAt: Option[String] = None  // ISO-8601 timestamp
  )

  // New Chapter format (Phase 1 - checklist aligned)
  case class GameChapter(
    chapterId: String,
    meta: GameMeta,
    sections: List[ChapterSection]
  )

  // Legacy Book format (kept for backward compatibility)
  case class Book(
    gameMeta: GameMeta,
    sections: List[BookSection],
    // Keep summaries for quick checklists/navigation
    turningPoints: List[BookTurningPoint],
    tacticalMoments: List[BookTacticalMoment],
    checklist: List[ChecklistBlock]
  )

