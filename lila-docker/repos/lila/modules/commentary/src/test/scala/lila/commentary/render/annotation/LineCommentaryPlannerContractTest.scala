package lila.commentary.render.annotation

import lila.commentary.selection.*

class LineCommentaryPlannerContractTest extends munit.FunSuite:

  private def assert(condition: => Boolean)(using loc: munit.Location): Unit =
    super.assert(condition, clues(""))

  private def assertEquals[A, B](obtained: A, expected: B)(using
      loc: munit.Location,
      compare: munit.Compare[A, B],
      diffOptions: munit.diff.DiffOptions
  ): Unit =
    super.assertEquals(obtained, expected, clues(""))

  test("one valid book annotation unit yields main line defensive resource and line result notes"):
    val commentary = LineCommentaryPlanner.plan(bookPlan(validUnit()))

    assertEquals(
      commentary.notes.map(note => note.kind),
      Vector(LineNoteKind.MainLine, LineNoteKind.DefensiveResource, LineNoteKind.LineResult)
    )
    assertEquals(
      commentary.notes.map(note => note.meaning),
      Vector(LineNoteMeaning.MainLine, LineNoteMeaning.DefensiveResource, LineNoteMeaning.PressurePersists)
    )
    assertEquals(commentary.notes.head.lineSan, Vector("Nf6", "Ng5"))
    assertEquals(commentary.notes(1).resourceLine.map(_.uci), Vector("d8b6", "d1d2"))
    assertEquals(commentary.notes(1).replyLine.map(_.uci), Vector("f3g5"))
    assertEquals(commentary.notes(2).lineSan, Vector("Nf6", "Ng5"))
    assertEquals(commentary.boundaries, Vector.empty)

  test("does-not-restore counterplay is a safe line result note"):
    val commentary =
      LineCommentaryPlanner.plan(
        bookPlan(
          validUnit().copy(
            proofRole = VariationEvidenceRole.DefenderResource,
            testResult = VariationTestResult.DoesNotRestoreCounterplay
          )
        )
      )

    assertEquals(
      commentary.notes.map(note => note.kind),
      Vector(LineNoteKind.MainLine, LineNoteKind.DefensiveResource, LineNoteKind.LineResult)
    )
    assertEquals(commentary.notes.last.meaning, LineNoteMeaning.DoesNotRestoreCounterplay)
    assertEquals(commentary.boundaries, Vector.empty)

  test("does-not-restore counterplay requires defender-resource role"):
    val commentary =
      LineCommentaryPlanner.plan(bookPlan(validUnit().copy(testResult = VariationTestResult.DoesNotRestoreCounterplay)))

    assertEquals(commentary.notes, Vector.empty)
    assert(commentary.boundaries.exists(_.reason == LineCommentaryBoundaryReason.ResultRoleMismatch))

  test("valid source frames attach coarse non-authoritative contexts to admitted line notes"):
    val contexts = Vector(
      sourceFrame(PlanAnnotationFrameKind.Opening),
      sourceFrame(PlanAnnotationFrameKind.Motif),
      sourceFrame(PlanAnnotationFrameKind.EndgameStudy),
      sourceFrame(PlanAnnotationFrameKind.Retrieval)
    )
    val commentary = LineCommentaryPlanner.plan(bookPlan(validUnit().copy(sourceFrames = contexts)))
    val expectedContexts = Vector(
      LineContext(LineContextKind.Opening, authoritative = false),
      LineContext(LineContextKind.Pattern, authoritative = false),
      LineContext(LineContextKind.Endgame, authoritative = false),
      LineContext(LineContextKind.Example, authoritative = false)
    )

    assertEquals(commentary.notes.map(_.contexts), Vector.fill(3)(expectedContexts))
    assertEquals(commentary.notes.flatMap(_.contexts.map(_.kind.key)).distinct, Vector("opening", "pattern", "endgame", "example"))
    assert(commentary.notes.forall(_.contexts.forall(context => !context.authoritative)))

  test("source frames on unsupported or mismatched units produce no notes"):
    val unsupported =
      LineCommentaryPlanner.plan(
        bookPlan(validUnit().copy(testResult = VariationTestResult.MovePremature, sourceFrames = allContextFrames))
      )
    val mismatched =
      LineCommentaryPlanner.plan(
        bookPlan(validUnit().copy(testResult = VariationTestResult.ResourceWorks, sourceFrames = allContextFrames))
      )

    assertEquals(unsupported.notes, Vector.empty)
    assert(unsupported.boundaries.exists(_.reason == LineCommentaryBoundaryReason.UnsupportedLineResult))
    assertEquals(mismatched.notes, Vector.empty)
    assert(mismatched.boundaries.exists(_.reason == LineCommentaryBoundaryReason.ResultRoleMismatch))

  test("authoritative source frame is ignored for line contexts"):
    val commentary =
      LineCommentaryPlanner.plan(
        bookPlan(validUnit().copy(sourceFrames = Vector(sourceFrame(PlanAnnotationFrameKind.Opening, authoritative = true))))
      )

    assertEquals(commentary.notes.map(_.kind), Vector(LineNoteKind.MainLine, LineNoteKind.DefensiveResource, LineNoteKind.LineResult))
    assert(commentary.notes.forall(_.contexts.isEmpty))

  test("malformed source frames are ignored for line contexts"):
    val malformedFrames = Vector(
      sourceFrame(PlanAnnotationFrameKind.Opening).copy(sourceRefIds = Vector("opening-line-test:defender-resource:context")),
      sourceFrame(PlanAnnotationFrameKind.Retrieval).copy(sourceRefIds = Vector("opening-line-test:candidate-main:context")),
      sourceFrame(PlanAnnotationFrameKind.Motif).copy(sourceRefIds = Vector("branch-cache-source-row"))
    )
    val commentary =
      LineCommentaryPlanner.plan(bookPlan(validUnit().copy(sourceFrames = malformedFrames)))

    assertEquals(commentary.notes.map(_.kind), Vector(LineNoteKind.MainLine, LineNoteKind.DefensiveResource, LineNoteKind.LineResult))
    assert(commentary.notes.forall(_.contexts.isEmpty))

  test("support and negative source frames do not attach to positive line notes"):
    val frames = Vector(
      sourceFrame(PlanAnnotationFrameKind.Opening).copy(proofId = "support-line", sourceRefIds = Vector("opening-line-test:support-line:context")),
      sourceFrame(PlanAnnotationFrameKind.Motif).copy(proofId = "negative-line", sourceRefIds = Vector("motif-line-test:negative-line:context"))
    )
    val commentary =
      LineCommentaryPlanner.plan(
        bookPlan(
          validUnit().copy(
            sourceFrames = frames,
            proofIds = validUnit().proofIds.copy(supportProofIds = Vector("support-line"), negativeProofIds = Vector("negative-line"))
          )
        )
      )

    assertEquals(commentary.notes.map(_.kind), Vector(LineNoteKind.MainLine, LineNoteKind.DefensiveResource, LineNoteKind.LineResult))
    assert(commentary.notes.forall(_.contexts.isEmpty))

  test("support detail creates supporting line note only behind admitted strong unit"):
    val support = supportDetail(LineSupportKind.Converts)
    val commentary =
      LineCommentaryPlanner.plan(bookPlan(validUnit().copy(supportingLines = Vector(support))))
    val supportNotes = commentary.notes.filter(_.kind == LineNoteKind.SupportingLine)

    assertEquals(
      commentary.notes.map(_.kind),
      Vector(LineNoteKind.MainLine, LineNoteKind.DefensiveResource, LineNoteKind.LineResult, LineNoteKind.SupportingLine)
    )
    assertEquals(supportNotes.map(_.meaning), Vector(LineNoteMeaning.Converts))
    assertEquals(supportNotes.head.lineSan, support.detail.lineSan)
    assertEquals(supportNotes.head.lineUci, support.detail.lineUci)
    assertEquals(supportNotes.head.testedMove.map(_.uci), Some("f4d5"))
    assertEquals(supportNotes.head.testedLine.map(_.uci), Vector("f4d5", "e6d5"))
    assertEquals(supportNotes.head.replyLine.map(_.uci), Vector("e6d5"))
    assertEquals(supportNotes.head.resourceLine.map(_.uci), Vector("d8b6"))

  test("caution detail creates caution note only behind admitted strong unit"):
    val caution = cautionDetail(LineCautionKind.EarlyMoveCaution)
    val commentary =
      LineCommentaryPlanner.plan(bookPlan(validUnit().copy(cautionLines = Vector(caution))))
    val cautionNotes = commentary.notes.filter(_.kind == LineNoteKind.Caution)
    val surface = cautionNotes.toString.toLowerCase

    assertEquals(
      commentary.notes.map(_.kind),
      Vector(LineNoteKind.MainLine, LineNoteKind.DefensiveResource, LineNoteKind.LineResult, LineNoteKind.Caution)
    )
    assertEquals(cautionNotes.map(_.meaning), Vector(LineNoteMeaning.EarlyMoveCaution))
    assertEquals(cautionNotes.head.lineSan, caution.detail.lineSan)
    assertEquals(cautionNotes.head.lineUci, caution.detail.lineUci)
    assertEquals(cautionNotes.head.testedMove.map(_.uci), Some("f4d5"))
    assertEquals(cautionNotes.head.testedLine.map(_.uci), Vector("f4d5", "e6d5"))
    assert(!surface.contains("natural"), clues(surface))
    assert(!surface.contains("tempting"), clues(surface))
    assert(!surface.contains("failed-line"), clues(surface))

  test("negative ids without caution detail create no caution note"):
    val commentary =
      LineCommentaryPlanner.plan(
        bookPlan(validUnit().copy(proofIds = validUnit().proofIds.copy(negativeProofIds = Vector("failed-line"))))
      )

    assertEquals(commentary.notes.map(_.kind), Vector(LineNoteKind.MainLine, LineNoteKind.DefensiveResource, LineNoteKind.LineResult))
    assert(!commentary.notes.exists(_.kind == LineNoteKind.Caution))

  test("support and caution details do not surface when primary unit is unsupported mismatched or low cap"):
    val detailedUnit =
      validUnit().copy(
        supportingLines = Vector(supportDetail(LineSupportKind.Converts)),
        cautionLines = Vector(cautionDetail(LineCautionKind.EarlyMoveCaution))
      )
    val cases =
      Vector(
        detailedUnit.copy(testResult = VariationTestResult.MovePremature),
        detailedUnit.copy(testResult = VariationTestResult.ResourceWorks),
        detailedUnit.copy(wordingCap = WordingStrength.ContextOnly)
      )

    cases.foreach: unit =>
      val commentary = LineCommentaryPlanner.plan(bookPlan(unit))

      assert(!commentary.notes.exists(_.kind == LineNoteKind.SupportingLine), clues(commentary))
      assert(!commentary.notes.exists(_.kind == LineNoteKind.Caution), clues(commentary))

  test("safe role result pairs yield line result notes with closed meanings"):
    val cases = Vector(
      safeLineResultCase(VariationEvidenceRole.Persistence, VariationTestResult.PressurePersists, "PressurePersists"),
      safeLineResultCase(
        VariationEvidenceRole.DefenderResource,
        VariationTestResult.DoesNotRestoreCounterplay,
        "DoesNotRestoreCounterplay"
      ),
      safeLineResultCase(VariationEvidenceRole.DefenderResource, VariationTestResult.ResourceFails, "ResourceFails"),
      safeLineResultCase(VariationEvidenceRole.DefenderResource, VariationTestResult.ResourceWorks, "ResourceWorks"),
      safeLineResultCase(VariationEvidenceRole.Hold, VariationTestResult.DefensiveHold, "DefensiveHold"),
      safeLineResultCase(VariationEvidenceRole.Simplification, VariationTestResult.Simplifies, "Simplifies"),
      safeLineResultCase(VariationEvidenceRole.Conversion, VariationTestResult.Simplifies, "Simplifies"),
      safeLineResultCase(VariationEvidenceRole.Conversion, VariationTestResult.Converts, "Converts")
    )

    cases.foreach: row =>
      val commentary =
        LineCommentaryPlanner.plan(bookPlan(unitFor(row.proofRole, row.testResult)))
      val lineResultNotes = commentary.notes.filter(_.kind == LineNoteKind.LineResult)

      assertEquals(lineResultNotes.map(_.meaning), Vector(LineNoteMeaning.valueOf(row.expectedMeaning)))
      assertEquals(commentary.boundaries, Vector.empty)

  test("safe result with wrong role fails closed without notes"):
    val mismatches = Vector(
      VariationEvidenceRole.Persistence -> VariationTestResult.ResourceWorks,
      VariationEvidenceRole.DefenderResource -> VariationTestResult.PressurePersists,
      VariationEvidenceRole.Hold -> VariationTestResult.Converts,
      VariationEvidenceRole.Simplification -> VariationTestResult.Converts
    )

    mismatches.foreach: (role, result) =>
      val commentary = LineCommentaryPlanner.plan(bookPlan(unitFor(role, result)))

      assertEquals(commentary.notes, Vector.empty)
      assert(commentary.boundaries.exists(_.reason == LineCommentaryBoundaryReason.ResultRoleMismatch))

  test("negative and risky role results fail closed without notes"):
    val riskyRows = Vector(
      VariationEvidenceRole.FailedTemptingMove -> VariationTestResult.MovePremature,
      VariationEvidenceRole.PrematureMove -> VariationTestResult.MovePremature,
      VariationEvidenceRole.ReleaseRisk -> VariationTestResult.ReleasesCounterplay,
      VariationEvidenceRole.ReleaseRisk -> VariationTestResult.PressurePersists
    )

    riskyRows.foreach: (role, result) =>
      val commentary =
        LineCommentaryPlanner.plan(
          bookPlan(unitFor(role, result).copy(proofIds = validUnit().proofIds.copy(negativeProofIds = Vector("negative-line"))))
        )

      assertEquals(commentary.notes, Vector.empty)
      assert(!commentary.notes.exists(_.kind == LineNoteKind.LineResult))
      assert(commentary.boundaries.nonEmpty)

  test("empty book annotation plan yields no notes and no fallback"):
    val commentary = LineCommentaryPlanner.plan(bookPlan(Vector.empty))

    assertEquals(commentary.notes, Vector.empty)
    assertEquals(commentary.boundaries, Vector.empty)

  test("wording cap below qualified support yields no positive notes"):
    val commentary =
      LineCommentaryPlanner.plan(bookPlan(validUnit().copy(wordingCap = WordingStrength.ContextOnly)))

    assertEquals(commentary.notes, Vector.empty)
    assert(commentary.boundaries.exists(_.reason == LineCommentaryBoundaryReason.WordingCapBelowQualifiedSupport))

  test("empty main line yields no notes and no fallback"):
    val commentary =
      LineCommentaryPlanner.plan(bookPlan(validUnit().copy(lineSan = Vector.empty, lineUci = Vector.empty)))

    assertEquals(commentary.notes, Vector.empty)
    assert(commentary.boundaries.exists(_.reason == LineCommentaryBoundaryReason.EmptyMainLine))

  test("unsupported negative result yields boundary and no generic line result fallback"):
    val commentary =
      LineCommentaryPlanner.plan(bookPlan(validUnit().copy(testResult = VariationTestResult.MovePremature)))

    assertEquals(commentary.notes, Vector.empty)
    assert(!commentary.notes.exists(_.kind == LineNoteKind.LineResult))
    assert(commentary.boundaries.exists(_.reason == LineCommentaryBoundaryReason.UnsupportedLineResult))

  test("notes contain no final prose fields and no forbidden wording tokens"):
    val validSourceFrame = sourceFrame(PlanAnnotationFrameKind.Opening)
    val malformedSourceFrame =
      validSourceFrame.copy(sourceRefIds = Vector("opening-line-test:best-theory-forced-winning-drawn-engine says-proves:context"))
    val commentary =
      LineCommentaryPlanner.plan(
        bookPlan(
          validUnit().copy(
            sourceFrames = Vector(validSourceFrame, malformedSourceFrame),
            supportingLines = Vector(supportDetail(LineSupportKind.Converts)),
            cautionLines = Vector(cautionDetail(LineCautionKind.EarlyMoveCaution)),
            proofIds = validUnit().proofIds.copy(supportProofIds = Vector("support-line"), negativeProofIds = Vector("failed-line"))
          )
        )
      )
    val productElementNames =
      (commentary.productElementNames.toVector ++
        commentary.notes.flatMap(_.productElementNames.toVector) ++
        commentary.notes.flatMap(_.contexts.flatMap(_.productElementNames.toVector)) ++
        commentary.notes.flatMap(_.resourceLine.flatMap(_.productElementNames.toVector)) ++
        commentary.notes.flatMap(_.replyLine.flatMap(_.productElementNames.toVector)) ++
        commentary.notes.flatMap(_.testedMove.toVector.flatMap(_.productElementNames.toVector)) ++
        commentary.notes.flatMap(_.testedLine.flatMap(_.productElementNames.toVector)) ++
        commentary.boundaries.flatMap(_.productElementNames.toVector))
        .map(_.toLowerCase)
    val forbiddenFieldTokens = Vector("text", "publictext", "template", "sentence", "phrase")
    val forbiddenSurfaceTokens = Vector("best", "theory", "forced", "winning", "drawn", "engine says", "proves", "natural", "tempting", "support-line", "failed-line", "source_ref", "raw")
    val proseFieldNames = productElementNames.filterNot(_ == "contexts")
    val surface = commentary.toString.toLowerCase

    assert(commentary.notes.exists(_.contexts.nonEmpty))
    (validSourceFrame.sourceRefIds ++ malformedSourceFrame.sourceRefIds).foreach: ref =>
      assert(!surface.contains(ref.toLowerCase), clues(surface))
    forbiddenFieldTokens.foreach: token =>
      assert(!proseFieldNames.exists(_.contains(token)), clues(productElementNames))
    forbiddenSurfaceTokens.foreach: token =>
      assert(!surface.contains(token), clues(surface))

  test("planner consumes only BookAnnotationPlan and requires no raw claim source or engine data"):
    val planMethods =
      LineCommentaryPlanner.getClass.getMethods.toVector
        .filter(method => method.getName == "plan")
        .filterNot(_.isBridge)
    assert(planMethods.exists(method => method.getParameterTypes.toVector == Vector(classOf[BookAnnotationPlan])))
    assert(
      !planMethods.exists(method =>
        method.getParameterTypes.exists(param =>
          param.getName.contains("CommentaryClaim") ||
            param.getName.contains("PreparedVariationEvidence") ||
            param.getName.contains("Source") ||
            param.getName.contains("Engine")
        )
      )
    )

    val commentary = LineCommentaryPlanner.plan(bookPlan(validUnit().copy(sourceFrames = Vector.empty)))

    assertEquals(commentary.notes.map(_.annotationId), Vector("pressure-claim", "pressure-claim", "pressure-claim"))

  private def bookPlan(units: Vector[BookAnnotationUnit]): BookAnnotationPlan =
    BookAnnotationPlan(
      units = units,
      boundaries = Vector.empty,
      wording = BookAnnotationWordingRules(WordingStrength.QualifiedSupport)
    )

  private def bookPlan(unit: BookAnnotationUnit): BookAnnotationPlan =
    bookPlan(Vector(unit))

  private final case class SafeLineResultCase(
      proofRole: VariationEvidenceRole,
      testResult: VariationTestResult,
      expectedMeaning: String
  )

  private def safeLineResultCase(
      proofRole: VariationEvidenceRole,
      testResult: VariationTestResult,
      expectedMeaning: String
  ): SafeLineResultCase =
    SafeLineResultCase(proofRole, testResult, expectedMeaning)

  private def unitFor(proofRole: VariationEvidenceRole, testResult: VariationTestResult): BookAnnotationUnit =
    val base = validUnit().copy(proofRole = proofRole, testResult = testResult)
    if proofRole == VariationEvidenceRole.DefenderResource then base
    else base.copy(resourceLine = Vector.empty, replyLine = Vector.empty)

  private def allContextFrames: Vector[BookAnnotationSourceFrame] =
    Vector(
      sourceFrame(PlanAnnotationFrameKind.Opening),
      sourceFrame(PlanAnnotationFrameKind.Motif),
      sourceFrame(PlanAnnotationFrameKind.EndgameStudy),
      sourceFrame(PlanAnnotationFrameKind.Retrieval)
    )

  private def sourceFrame(
      kind: PlanAnnotationFrameKind,
      authoritative: Boolean = false
  ): BookAnnotationSourceFrame =
    BookAnnotationSourceFrame(
      kind = kind,
      proofId = "candidate-main",
      sourceRefIds = Vector(s"${lineTestPrefix(kind)}:candidate-main:context"),
      authoritative = authoritative
    )

  private def lineTestPrefix(kind: PlanAnnotationFrameKind): String =
    kind match
      case PlanAnnotationFrameKind.Opening => "opening-line-test"
      case PlanAnnotationFrameKind.Motif => "motif-line-test"
      case PlanAnnotationFrameKind.EndgameStudy => "endgame-line-test"
      case PlanAnnotationFrameKind.Retrieval => "retrieval-line-test"

  private def supportDetail(kind: LineSupportKind): LineSupport =
    LineSupport(kind, sharedDetail())

  private def cautionDetail(kind: LineCautionKind): LineCaution =
    LineCaution(kind, sharedDetail())

  private def sharedDetail(): LineCommentaryDetail =
    LineCommentaryDetail(
      lineSan = Vector("Nxd5", "...exd5"),
      lineUci = Vector("f4d5", "e6d5"),
      testedMove = Some(BookAnnotationMove("Nxd5", "f4d5")),
      testedLine = Vector(BookAnnotationMove("Nxd5", "f4d5"), BookAnnotationMove("...exd5", "e6d5")),
      replyLine = Vector(BookAnnotationMove("...exd5", "e6d5")),
      resourceLine = Vector(BookAnnotationMove("...Qb6", "d8b6")),
      wordingCap = WordingStrength.QualifiedSupport
    )

  private def validUnit(): BookAnnotationUnit =
    BookAnnotationUnit(
      claimId = "pressure-claim",
      lineSan = Vector("Nf6", "Ng5"),
      lineUci = Vector("g8f6", "f3g5"),
      resourceLine = Vector(BookAnnotationMove("...Qb6", "d8b6"), BookAnnotationMove("Qd2", "d1d2")),
      replyLine = Vector(BookAnnotationMove("Ng5", "f3g5")),
      proofRole = VariationEvidenceRole.Persistence,
      testResult = VariationTestResult.PressurePersists,
      sourceFrames = Vector.empty,
      wordingCap = WordingStrength.QualifiedSupport,
      proofIds = BookAnnotationProofIds(
        primaryProofId = "candidate-main",
        companionProofIds = Vector("defender-resource"),
        supportProofIds = Vector.empty,
        negativeProofIds = Vector.empty
      )
    )
