package lila.commentary.selection

class SourceContextClaimBoundaryTest extends munit.FunSuite:

  test("normalized handoff candidate is the only shape selection needs"):
    val candidate = SourceContextCandidate(
      candidateId = "source-handoff-opening",
      kind = SourceContextKind.Opening,
      sourceRefs = Vector(
        "opening-position:catalan-open:canonical",
        "opening-source-use:master_reference",
        "opening-source-use:online_trend"
      )
    )

    val claim = SourceContextClaimBoundary.toClaim(candidate)
    val outline = ClaimSelector.select(Vector(claim))

    assertEquals(claim.layer, ClaimLayer.SourceContext)
    assertEquals(claim.status, ClaimStatus.Context)
    assertEquals(claim.wordingStrengthCap, WordingStrength.ContextOnly)
    assertEquals(claim.exactBoardBound, false)
    assertEquals(outline.lead, None)
    assertEquals(outline.context.map(_.claim.id), Vector("source-handoff-opening"))
    assertEquals(outline.context.flatMap(_.softReasons), Vector(SuppressionReason.SourceContextOnly))

  test("source-context candidate ids are restricted before public claim ids"):
    interceptMessage[IllegalArgumentException]("requirement failed: SourceContextCandidate id must be public-safe"):
      SourceContextCandidate(
        candidateId = "retrieval-player-carlsen-result-1-0",
        kind = SourceContextKind.Retrieval,
        sourceRefs = Vector("retrieval-example:catalan-structure-example")
      )

  test("prepared opening and retrieval candidates become context-only source claims"):
    val claims = Vector(
      SourceContextClaimBoundary.toClaim(
        SourceContextCandidate(
          candidateId = "open-context-catalan-master-online",
          kind = SourceContextKind.Opening,
          sourceRefs = Vector(
            "opening-position:catalan-open:canonical",
            "opening-source-use:master_reference",
            "opening-source-use:online_trend"
          )
        )
      ),
      SourceContextClaimBoundary.toClaim(
        SourceContextCandidate(
          candidateId = "retrieval-catalan-example",
          kind = SourceContextKind.Retrieval,
          sourceRefs = Vector("retrieval-example:catalan-structure-example")
        )
      )
    )

    val outline = ClaimSelector.select(claims)

    assertEquals(outline.lead, None)
    assertEquals(
      outline.context.map(_.claim.id),
      Vector("open-context-catalan-master-online", "retrieval-catalan-example")
    )
    assertEquals(outline.context.map(_.bucket).toSet, Set(ClaimBucket.ContextOnly))
    assertEquals(outline.wordingStrengthCap, WordingStrength.ContextOnly)
    assert(outline.context.forall(_.claim.layer == ClaimLayer.SourceContext))
    assert(outline.context.forall(_.claim.wordingStrengthCap == WordingStrength.ContextOnly))
    assert(outline.context.forall(_.claim.exactBoardBound == false))

    val evidenceIds = outline.evidenceRefs.map(_.id)
    assert(evidenceIds.contains("opening-position:catalan-open:canonical"))
    assert(evidenceIds.contains("opening-source-use:master_reference"))
    assert(evidenceIds.contains("opening-source-use:online_trend"))
    assert(evidenceIds.contains("retrieval-example:catalan-structure-example"))
    assertEquals(
      outline.context.find(_.claim.id == "retrieval-catalan-example").toVector.flatMap(_.softReasons),
      Vector(SuppressionReason.SourceContextOnly, SuppressionReason.RetrievalNonAuthoritative)
    )

  test("prepared endgame-study fixture binding does not require study id and fixture id to be identical"):
    val claim = SourceContextClaimBoundary.toClaim(
      SourceContextCandidate(
        candidateId = "study-lucena-context",
        kind = SourceContextKind.EndgameStudy,
        sourceRefs = Vector("endgame-study:lucena_rook_pawn:applicable"),
        exactBoardRefs = Vector(
          SourceContextExactRef(
            id = "endgame-study-applicability:study-lucena-context",
            route = Some("lucena_rook_pawn"),
            scope = Some("exact_endgame_applicability")
          )
        )
      )
    )

    val outline = ClaimSelector.select(Vector(claim))

    assertEquals(outline.lead, None)
    assertEquals(outline.context.map(_.claim.id), Vector("study-lucena-context"))
    assertEquals(outline.context.map(_.bucket), Vector(ClaimBucket.ContextOnly))
    assertEquals(outline.suppressedClaims.map(_.claim.id), Vector.empty)
    assertEquals(outline.context.flatMap(_.softReasons), Vector(SuppressionReason.SourceContextOnly))

    val exactRefs = outline.context.flatMap(_.claim.evidenceRefs).filter(_.kind == EvidenceRefKind.ExactBoard)
    assertEquals(exactRefs.map(_.id), Vector("endgame-study-applicability:study-lucena-context"))
    assertEquals(exactRefs.flatMap(_.route), Vector("lucena_rook_pawn"))
    assertEquals(exactRefs.flatMap(_.scope), Vector("exact_endgame_applicability"))

  test("unbound endgame-study applicability remains fail-closed"):
    val claim = SourceContextClaimBoundary.toClaim(
      SourceContextCandidate(
        candidateId = "study-lucena-unbound",
        kind = SourceContextKind.EndgameStudy,
        sourceRefs = Vector("endgame-study:lucena_rook_pawn:applicable"),
        exactBoardRefs = Vector(SourceContextExactRef("endgame-study-applicability:study-lucena-context"))
      )
    )

    val outline = ClaimSelector.select(Vector(claim))

    assertEquals(outline.context, Vector.empty)
    assertEquals(outline.suppressedClaims.map(_.claim.id), Vector("study-lucena-unbound"))
    assert(outline.suppressedClaims.exists(suppressed => suppressed.reasons.contains(SuppressionReason.ForbiddenShortcut)))
