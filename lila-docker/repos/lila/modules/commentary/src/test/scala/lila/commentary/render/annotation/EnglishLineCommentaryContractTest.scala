package lila.commentary.render.annotation

import lila.commentary.selection.WordingStrength

class EnglishLineCommentaryContractTest extends munit.FunSuite:

  private def assert(condition: => Boolean)(using loc: munit.Location): Unit =
    super.assert(condition, clues(""))

  private def assertEquals[A, B](obtained: A, expected: B)(using
      loc: munit.Location,
      compare: munit.Compare[A, B],
      diffOptions: munit.diff.DiffOptions
  ): Unit =
    super.assertEquals(obtained, expected, clues(""))

  test("main resource reply and result create one compact book-style comment"):
    val commentary = EnglishLineCommentaryWriter.write(linePlan(mainNote(), resourceNote(), resultNote()))

    assertEquals(commentary.comments.size, 1)
    assertEquals(commentary.comments.head.annotationId, "pressure-claim")
    assertEquals(commentary.comments.head.primaryProofId, "candidate-main")
    assertEquals(commentary.comments.head.wordingCap, WordingStrength.QualifiedSupport)
    assertEquals(
      commentary.comments.head.comment,
      "After Nf6 Ng5, ...Qb6 Qd2 is met by Ng5, and the pressure stays on."
    )

  test("missing main line or missing line result creates no comment"):
    val missingMain = EnglishLineCommentaryWriter.write(linePlan(resourceNote(), resultNote()))
    val missingResult = EnglishLineCommentaryWriter.write(linePlan(mainNote(), resourceNote()))

    assertEquals(missingMain.comments, Vector.empty)
    assertEquals(missingResult.comments, Vector.empty)

  test("unsupported meanings caution-only and support-only create no comment"):
    val unsupported =
      EnglishLineCommentaryWriter.write(linePlan(mainNote(), resultNote(LineNoteMeaning.PrematureMove)))
    val cautionOnly = EnglishLineCommentaryWriter.write(linePlan(cautionNote()))
    val supportOnly = EnglishLineCommentaryWriter.write(linePlan(supportNote()))

    assertEquals(unsupported.comments, Vector.empty)
    assertEquals(cautionOnly.comments, Vector.empty)
    assertEquals(supportOnly.comments, Vector.empty)

  test("support and caution sentences append only when proof-bound to the admitted main comment"):
    val boundSupport = supportNote().copy(primaryProofId = "candidate-main")
    val boundCaution = cautionNote().copy(primaryProofId = "candidate-main")
    val withDetails =
      EnglishLineCommentaryWriter.write(linePlan(mainNote(), resultNote(), boundSupport, boundCaution))
    val crossProofDetails =
      EnglishLineCommentaryWriter.write(linePlan(mainNote(), resultNote(), supportNote(), cautionNote()))
    val withoutMain = EnglishLineCommentaryWriter.write(linePlan(resultNote(), supportNote(), cautionNote()))

    assertEquals(
      withDetails.comments.map(_.comment),
      Vector(
        "After Nf6 Ng5, the pressure stays on. In Nxd5 ...exd5, the continuation becomes clearer. By contrast, Nxd5 ...exd5 comes too early."
      )
    )
    assertEquals(crossProofDetails.comments.map(_.comment), Vector("After Nf6 Ng5, the pressure stays on."))
    assertEquals(withoutMain.comments, Vector.empty)

  test("line result phrases stay player-facing without repetitive engine-style wording"):
    val pressure =
      EnglishLineCommentaryWriter.write(linePlan(mainNote(), resultNote(LineNoteMeaning.PressurePersists)))
    val counterplay =
      EnglishLineCommentaryWriter.write(linePlan(mainNote(), resultNote(LineNoteMeaning.DoesNotRestoreCounterplay)))
    val conversion =
      EnglishLineCommentaryWriter.write(linePlan(mainNote(), resultNote(LineNoteMeaning.Converts)))

    assertEquals(pressure.comments.map(_.comment), Vector("After Nf6 Ng5, the pressure stays on."))
    assertEquals(counterplay.comments.map(_.comment), Vector("After Nf6 Ng5, counterplay still does not return."))
    assertEquals(conversion.comments.map(_.comment), Vector("After Nf6 Ng5, the continuation becomes clearer."))
    (pressure.comments ++ counterplay.comments ++ conversion.comments).foreach: comment =>
      assert(!comment.comment.toLowerCase.contains("in the line"), clues(comment.comment))

  test("same annotation alternate complete line does not splice into the first comment"):
    val alternateMain =
      mainNote().copy(primaryProofId = "alternate-main", lineSan = Vector("d4", "...d5"), lineUci = Vector("d2d4", "d7d5"))
    val alternateResult =
      resultNote().copy(primaryProofId = "alternate-main", lineSan = Vector("d4", "...d5"), lineUci = Vector("d2d4", "d7d5"))
    val alternateSupport =
      supportNote().copy(primaryProofId = "alternate-main", lineSan = Vector("c4", "...e6"), lineUci = Vector("c2c4", "e7e6"))
    val commentary =
      EnglishLineCommentaryWriter.write(linePlan(mainNote(), resultNote(), alternateMain, alternateResult, alternateSupport))

    assertEquals(commentary.comments.size, 1)
    assertEquals(commentary.comments.head.primaryProofId, "candidate-main")
    assertEquals(commentary.comments.head.comment, "After Nf6 Ng5, the pressure stays on.")
    assert(!commentary.comments.head.comment.contains("d4"), clues(commentary.comments.head.comment))
    assert(!commentary.comments.head.comment.contains("c4"), clues(commentary.comments.head.comment))

  test("comment text contains no forbidden wording source ids internal ids or proof ids"):
    val commentary =
      EnglishLineCommentaryWriter.write(
        linePlan(
          mainNote().copy(contexts = Vector(LineContext(LineContextKind.Opening, authoritative = false))),
          resourceNote(),
          resultNote(),
          supportNote(),
          cautionNote()
        )
      )
    val text = commentary.comments.map(_.comment).mkString(" ").toLowerCase
    val forbidden =
      Vector(
        "natural",
        "tempting",
        "better plan",
        "critical",
        "best",
        "only",
        "must",
        "has to",
        "forced",
        "winning",
        "drawn",
        "decisive",
        "refutes",
        "engine",
        "stockfish",
        "eval",
        "depth",
        "pv",
        "proof",
        "theory proves",
        "tablebase",
        "oracle",
        "candidate-main",
        "defender-resource",
        "support-line",
        "failed-line",
        "opening-line-test",
        "source_ref",
        "raw"
      )

    assert(commentary.comments.nonEmpty)
    forbidden.foreach(token => assert(!text.contains(token), clues(token, text)))

  test("grammar guard rejects blank SAN and emits clean punctuation"):
    val clean =
      EnglishLineCommentaryWriter.write(
        linePlan(
          mainNote(),
          resourceNote(),
          resultNote(),
          supportNote().copy(primaryProofId = "candidate-main"),
          cautionNote().copy(primaryProofId = "candidate-main")
        )
      )
    val blankSan =
      EnglishLineCommentaryWriter.write(
        linePlan(mainNote().copy(lineSan = Vector("Nf6", "", "Ng5")), resultNote())
      )
    val text = clean.comments.map(_.comment).mkString(" ")

    assert(clean.comments.nonEmpty)
    assertEquals(blankSan.comments, Vector.empty)
    assert(!text.contains("  "), clues(text))
    assert(!text.contains(" ,"), clues(text))
    assert(text.contains("...exd5"), clues(text))
    assert(!text.contains("...."), clues(text))
    assert(!text.contains(", is"), clues(text))
    assert(!text.contains("After ,"), clues(text))

  test("writer consumes only LineCommentaryPlan and no lower evidence or source objects"):
    val writeMethods =
      EnglishLineCommentaryWriter.getClass.getMethods.toVector
        .filter(method => method.getName == "write")
        .filterNot(_.isBridge)

    assert(writeMethods.exists(method => method.getParameterTypes.toVector == Vector(classOf[LineCommentaryPlan])))
    assert(
      !writeMethods.exists(method =>
        method.getParameterTypes.exists(param =>
          param.getName.contains("CommentaryClaim") ||
            param.getName.contains("PreparedVariationEvidence") ||
            param.getName.contains("CandidateLineEvidence") ||
            param.getName.contains("Source") ||
            param.getName.contains("Probe") ||
            param.getName.contains("Cache") ||
            param.getName.contains("Engine") ||
            param.getName.contains("Certification")
        )
      )
    )

  private def linePlan(notes: LineNote*): LineCommentaryPlan =
    LineCommentaryPlan(notes.toVector, Vector.empty)

  private def mainNote(): LineNote =
    baseNote(
      kind = LineNoteKind.MainLine,
      meaning = LineNoteMeaning.MainLine
    )

  private def resourceNote(): LineNote =
    baseNote(
      kind = LineNoteKind.DefensiveResource,
      meaning = LineNoteMeaning.DefensiveResource,
      resourceLine = Vector(LineNoteMove("...Qb6", "d8b6"), LineNoteMove("Qd2", "d1d2")),
      replyLine = Vector(LineNoteMove("Ng5", "f3g5"))
    )

  private def resultNote(meaning: LineNoteMeaning = LineNoteMeaning.PressurePersists): LineNote =
    baseNote(
      kind = LineNoteKind.LineResult,
      meaning = meaning
    )

  private def supportNote(): LineNote =
    baseNote(
      kind = LineNoteKind.SupportingLine,
      meaning = LineNoteMeaning.Converts,
      lineSan = Vector("Nxd5", "...exd5"),
      lineUci = Vector("f4d5", "e6d5"),
      testedLine = Vector(LineNoteMove("Nxd5", "f4d5"), LineNoteMove("...exd5", "e6d5"))
    ).copy(primaryProofId = "support-line")

  private def cautionNote(): LineNote =
    baseNote(
      kind = LineNoteKind.Caution,
      meaning = LineNoteMeaning.EarlyMoveCaution,
      lineSan = Vector("Nxd5", "...exd5"),
      lineUci = Vector("f4d5", "e6d5"),
      testedLine = Vector(LineNoteMove("Nxd5", "f4d5"), LineNoteMove("...exd5", "e6d5"))
    ).copy(primaryProofId = "failed-line", wordingCap = WordingStrength.NegativeOnly)

  private def baseNote(
      kind: LineNoteKind,
      meaning: LineNoteMeaning,
      lineSan: Vector[String] = Vector("Nf6", "Ng5"),
      lineUci: Vector[String] = Vector("g8f6", "f3g5"),
      resourceLine: Vector[LineNoteMove] = Vector.empty,
      replyLine: Vector[LineNoteMove] = Vector.empty,
      testedLine: Vector[LineNoteMove] = Vector.empty
  ): LineNote =
    LineNote(
      kind = kind,
      meaning = meaning,
      annotationId = "pressure-claim",
      lineSan = lineSan,
      resourceLine = resourceLine,
      replyLine = replyLine,
      primaryProofId = "candidate-main",
      companionProofIds = Vector("defender-resource"),
      wordingCap = WordingStrength.QualifiedSupport,
      contexts = Vector.empty,
      lineUci = lineUci,
      testedMove = None,
      testedLine = testedLine
    )
