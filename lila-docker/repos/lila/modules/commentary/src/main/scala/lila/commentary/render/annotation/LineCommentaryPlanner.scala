package lila.commentary.render.annotation

import lila.commentary.selection.*
import lila.commentary.selection.PublicVariationEvidenceSafety

final case class LineCommentaryPlan(
    notes: Vector[LineNote],
    boundaries: Vector[LineCommentaryBoundary]
)

final case class LineNote(
    kind: LineNoteKind,
    meaning: LineNoteMeaning,
    annotationId: String,
    lineSan: Vector[String],
    resourceLine: Vector[LineNoteMove],
    replyLine: Vector[LineNoteMove],
    primaryProofId: String,
    companionProofIds: Vector[String],
    wordingCap: WordingStrength,
    contexts: Vector[LineContext] = Vector.empty,
    lineUci: Vector[String] = Vector.empty,
    testedMove: Option[LineNoteMove] = None,
    testedLine: Vector[LineNoteMove] = Vector.empty
)

final case class LineContext(
    kind: LineContextKind,
    authoritative: Boolean
)

final case class LineNoteMove(
    san: String,
    uci: String
)

object LineNoteMove:
  def from(move: BookAnnotationMove): LineNoteMove =
    LineNoteMove(move.san, move.uci)

enum LineNoteKind(val key: String):
  case MainLine extends LineNoteKind("main_line")
  case DefensiveResource extends LineNoteKind("defensive_resource")
  case LineResult extends LineNoteKind("line_result")
  case SupportingLine extends LineNoteKind("supporting_line")
  case Caution extends LineNoteKind("caution")

enum LineNoteMeaning(val key: String):
  case MainLine extends LineNoteMeaning("main_line")
  case DefensiveResource extends LineNoteMeaning("defensive_resource")
  case PressurePersists extends LineNoteMeaning("pressure_persists")
  case DoesNotRestoreCounterplay extends LineNoteMeaning("does_not_restore_counterplay")
  case ResourceFails extends LineNoteMeaning("resource_fails")
  case ResourceWorks extends LineNoteMeaning("resource_works")
  case DefensiveHold extends LineNoteMeaning("defensive_hold")
  case Simplifies extends LineNoteMeaning("simplifies")
  case Converts extends LineNoteMeaning("converts")
  case EarlyMoveCaution extends LineNoteMeaning("early_move_caution")
  case PrematureMove extends LineNoteMeaning("premature_move")
  case ReleasesCounterplay extends LineNoteMeaning("releases_counterplay")

enum LineContextKind(val key: String):
  case Opening extends LineContextKind("opening")
  case Pattern extends LineContextKind("pattern")
  case Endgame extends LineContextKind("endgame")
  case Example extends LineContextKind("example")

final case class LineCommentaryBoundary(
    annotationId: Option[String],
    primaryProofId: Option[String],
    reason: LineCommentaryBoundaryReason
)

enum LineCommentaryBoundaryReason(val key: String):
  case WordingCapBelowQualifiedSupport extends LineCommentaryBoundaryReason("wording_cap_below_qualified_support")
  case EmptyMainLine extends LineCommentaryBoundaryReason("empty_main_line")
  case UnsupportedLineResult extends LineCommentaryBoundaryReason("unsupported_line_result")
  case ResultRoleMismatch extends LineCommentaryBoundaryReason("result_role_mismatch")

object LineCommentaryPlanner:

  def plan(annotation: BookAnnotationPlan): LineCommentaryPlan =
    val results = annotation.units.map(unit => notesForUnit(annotation.wording.maxStrength, unit))
    LineCommentaryPlan(
      notes = results.flatMap(_.notes),
      boundaries = results.flatMap(_.boundaries)
    )

  private final case class UnitResult(
      notes: Vector[LineNote],
      boundaries: Vector[LineCommentaryBoundary]
  )

  private def notesForUnit(planCap: WordingStrength, unit: BookAnnotationUnit): UnitResult =
    val cap = WordingStrength.weaker(planCap, unit.wordingCap)
    if cap.rank < WordingStrength.QualifiedSupport.rank then
      UnitResult(Vector.empty, Vector(boundary(unit, LineCommentaryBoundaryReason.WordingCapBelowQualifiedSupport)))
    else if unit.lineSan.isEmpty then
      UnitResult(Vector.empty, Vector(boundary(unit, LineCommentaryBoundaryReason.EmptyMainLine)))
    else
      val contexts = contextsFor(unit)
      val result = resultMeaning(unit.testResult) match
        case Some(meaning) if resultRoleAllowed(unit, meaning) =>
          val mainNote = note(
            unit = unit,
            kind = LineNoteKind.MainLine,
            meaning = LineNoteMeaning.MainLine,
            resourceLine = Vector.empty,
            replyLine = Vector.empty,
            cap = cap,
            contexts = contexts
          )
          val resourceNote =
            Option
              .when(unit.resourceLine.nonEmpty || unit.replyLine.nonEmpty):
                note(
                  unit = unit,
                  kind = LineNoteKind.DefensiveResource,
                  meaning = LineNoteMeaning.DefensiveResource,
                  resourceLine = unit.resourceLine.map(LineNoteMove.from),
                  replyLine = unit.replyLine.map(LineNoteMove.from),
                  cap = cap,
                  contexts = contexts
                )
              .toVector
          val lineResultNote =
            note(
              unit = unit,
              kind = LineNoteKind.LineResult,
              meaning = meaning,
              resourceLine = Vector.empty,
              replyLine = Vector.empty,
              cap = cap,
              contexts = contexts
            )
          val detailNotes =
            unit.supportingLines.map(supportNote(unit, _, cap)) ++
              unit.cautionLines.map(cautionNote(unit, _, cap))
          UnitResult(
            notes = Vector(mainNote) ++ resourceNote ++ Vector(lineResultNote) ++ detailNotes,
            boundaries = Vector.empty
          )
        case Some(_) =>
          UnitResult(Vector.empty, Vector(boundary(unit, LineCommentaryBoundaryReason.ResultRoleMismatch)))
        case None =>
          UnitResult(
            notes = Vector.empty,
            boundaries = Vector(boundary(unit, LineCommentaryBoundaryReason.UnsupportedLineResult))
          )
      result

  private def note(
      unit: BookAnnotationUnit,
      kind: LineNoteKind,
      meaning: LineNoteMeaning,
      resourceLine: Vector[LineNoteMove],
      replyLine: Vector[LineNoteMove],
      cap: WordingStrength,
      contexts: Vector[LineContext],
      lineSan: Vector[String] = Vector.empty,
      lineUci: Vector[String] = Vector.empty,
      testedMove: Option[LineNoteMove] = None,
      testedLine: Vector[LineNoteMove] = Vector.empty
  ): LineNote =
    LineNote(
      kind = kind,
      meaning = meaning,
      annotationId = unit.claimId,
      lineSan = if lineSan.nonEmpty then lineSan else unit.lineSan,
      resourceLine = resourceLine,
      replyLine = replyLine,
      primaryProofId = unit.proofIds.primaryProofId,
      companionProofIds = unit.proofIds.companionProofIds,
      wordingCap = cap,
      contexts = contexts,
      lineUci = if lineUci.nonEmpty then lineUci else unit.lineUci,
      testedMove = testedMove,
      testedLine = testedLine
    )

  private def supportNote(unit: BookAnnotationUnit, support: LineSupport, cap: WordingStrength): LineNote =
    detailNote(
      unit = unit,
      kind = LineNoteKind.SupportingLine,
      meaning = supportMeaning(support.kind),
      detail = support.detail,
      cap = cap
    )

  private def cautionNote(unit: BookAnnotationUnit, caution: LineCaution, cap: WordingStrength): LineNote =
    detailNote(
      unit = unit,
      kind = LineNoteKind.Caution,
      meaning = cautionMeaning(caution.kind),
      detail = caution.detail,
      cap = cap
    )

  private def detailNote(
      unit: BookAnnotationUnit,
      kind: LineNoteKind,
      meaning: LineNoteMeaning,
      detail: LineCommentaryDetail,
      cap: WordingStrength
  ): LineNote =
    note(
      unit = unit,
      kind = kind,
      meaning = meaning,
      resourceLine = detail.resourceLine.map(LineNoteMove.from),
      replyLine = detail.replyLine.map(LineNoteMove.from),
      cap = WordingStrength.weaker(cap, detail.wordingCap),
      contexts = Vector.empty,
      lineSan = detail.lineSan,
      lineUci = detail.lineUci,
      testedMove = detail.testedMove.map(LineNoteMove.from),
      testedLine = detail.testedLine.map(LineNoteMove.from)
    )

  private def contextsFor(unit: BookAnnotationUnit): Vector[LineContext] =
    val allowedProofIds = (Vector(unit.proofIds.primaryProofId) ++ unit.proofIds.companionProofIds).toSet
    unit.sourceFrames
      .filter(frame => !frame.authoritative && allowedProofIds.contains(frame.proofId) && validSourceFrame(frame))
      .flatMap(frame => contextKind(frame.kind).map(LineContext(_, authoritative = false)))
      .distinct

  private def contextKind(kind: PlanAnnotationFrameKind): Option[LineContextKind] =
    kind match
      case PlanAnnotationFrameKind.Opening => Some(LineContextKind.Opening)
      case PlanAnnotationFrameKind.Motif => Some(LineContextKind.Pattern)
      case PlanAnnotationFrameKind.EndgameStudy => Some(LineContextKind.Endgame)
      case PlanAnnotationFrameKind.Retrieval => Some(LineContextKind.Example)

  private def validSourceFrame(frame: BookAnnotationSourceFrame): Boolean =
    sourceContextKind(frame.kind).exists(kind =>
      frame.sourceRefIds.nonEmpty &&
        frame.sourceRefIds.forall(ref => PublicVariationEvidenceSafety.lineTestProofIdForKind(ref, kind).contains(frame.proofId))
    )

  private def sourceContextKind(kind: PlanAnnotationFrameKind): Option[SourceContextKind] =
    kind match
      case PlanAnnotationFrameKind.Opening => Some(SourceContextKind.Opening)
      case PlanAnnotationFrameKind.Motif => Some(SourceContextKind.Motif)
      case PlanAnnotationFrameKind.EndgameStudy => Some(SourceContextKind.EndgameStudy)
      case PlanAnnotationFrameKind.Retrieval => Some(SourceContextKind.Retrieval)

  private def resultMeaning(result: VariationTestResult): Option[LineNoteMeaning] =
    result match
      case VariationTestResult.PressurePersists => Some(LineNoteMeaning.PressurePersists)
      case VariationTestResult.DoesNotRestoreCounterplay => Some(LineNoteMeaning.DoesNotRestoreCounterplay)
      case VariationTestResult.ResourceFails => Some(LineNoteMeaning.ResourceFails)
      case VariationTestResult.ResourceWorks => Some(LineNoteMeaning.ResourceWorks)
      case VariationTestResult.DefensiveHold => Some(LineNoteMeaning.DefensiveHold)
      case VariationTestResult.Simplifies => Some(LineNoteMeaning.Simplifies)
      case VariationTestResult.Converts => Some(LineNoteMeaning.Converts)
      case _ => None

  private def resultRoleAllowed(unit: BookAnnotationUnit, meaning: LineNoteMeaning): Boolean =
    meaning match
      case LineNoteMeaning.PressurePersists =>
        unit.proofRole == VariationEvidenceRole.Persistence
      case LineNoteMeaning.DoesNotRestoreCounterplay =>
        unit.proofRole == VariationEvidenceRole.DefenderResource
      case LineNoteMeaning.ResourceFails | LineNoteMeaning.ResourceWorks =>
        unit.proofRole == VariationEvidenceRole.DefenderResource
      case LineNoteMeaning.DefensiveHold =>
        unit.proofRole == VariationEvidenceRole.Hold
      case LineNoteMeaning.Simplifies =>
        unit.proofRole == VariationEvidenceRole.Simplification ||
          unit.proofRole == VariationEvidenceRole.Conversion
      case LineNoteMeaning.Converts =>
        unit.proofRole == VariationEvidenceRole.Conversion
      case LineNoteMeaning.MainLine | LineNoteMeaning.DefensiveResource => true
      case LineNoteMeaning.EarlyMoveCaution | LineNoteMeaning.PrematureMove | LineNoteMeaning.ReleasesCounterplay => false

  private def supportMeaning(kind: LineSupportKind): LineNoteMeaning =
    kind match
      case LineSupportKind.PressurePersists => LineNoteMeaning.PressurePersists
      case LineSupportKind.DoesNotRestoreCounterplay => LineNoteMeaning.DoesNotRestoreCounterplay
      case LineSupportKind.ResourceFails => LineNoteMeaning.ResourceFails
      case LineSupportKind.ResourceWorks => LineNoteMeaning.ResourceWorks
      case LineSupportKind.DefensiveHold => LineNoteMeaning.DefensiveHold
      case LineSupportKind.Simplifies => LineNoteMeaning.Simplifies
      case LineSupportKind.Converts => LineNoteMeaning.Converts

  private def cautionMeaning(kind: LineCautionKind): LineNoteMeaning =
    kind match
      case LineCautionKind.EarlyMoveCaution => LineNoteMeaning.EarlyMoveCaution
      case LineCautionKind.PrematureMove => LineNoteMeaning.PrematureMove
      case LineCautionKind.ReleasesCounterplay => LineNoteMeaning.ReleasesCounterplay

  private def boundary(
      unit: BookAnnotationUnit,
      reason: LineCommentaryBoundaryReason
  ): LineCommentaryBoundary =
    LineCommentaryBoundary(
      annotationId = Some(unit.claimId),
      primaryProofId = Some(unit.proofIds.primaryProofId),
      reason = reason
    )
