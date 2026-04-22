package lila.commentary.certification

import chess.Color
import chess.format.{ Fen, Uci }

import lila.commentary.delta.{ StrategicDeltaExtractor, StrategicDeltaSet }
import lila.commentary.root.{ RootAtomRegistry, RootStateVector }
import lila.commentary.strategic.StrategicObjectExtractor
import lila.commentary.strategic.{ StrategicObjectExtraction, StrategicObjectSet }
import lila.commentary.validation.CertificationRuntimeTestSupport
import lila.commentary.witness.{ WitnessAnchor, WitnessValue }
import lila.commentary.witness.WitnessSet

class CertificationBoundaryTest extends munit.FunSuite:

  test("CertificationScopeContract freezes the certification family inventory"):
    assertEquals(CertificationScopeContract.frozenCertificationInventoryRowCount, 10)
    assertEquals(
      CertificationScopeContract.activeCertificationFamilyIds.map(_.value),
      Vector(
        "DevelopmentComparison",
        "InitiativeWindow",
        "MobilityComparison",
        "ComparativeKingFragility",
        "CertifiedKingSafetyEdge",
        "MateNetCertification",
        "MaterialHarvest",
        "WinningEndgame",
        "FortressDrawCertification",
        "PerpetualCheckHolding",
        "PromotionRace"
      )
    )
    assertEquals(
      CertificationScopeContract.frozenCertificationFamilyIds.map(_.value),
      CertificationScopeContract.activeCertificationFamilyIds.map(_.value)
    )

  test("CertificationEvidenceBundle rejects duplicate certification evidence identities"):
    val current = CertificationRuntimeTestSupport.objectExtractionForRowId("cert-development-comparison-exact")

    interceptMessage[IllegalArgumentException](
      "requirement failed: Duplicate certification evidence entries are not allowed: DevelopmentComparison|white|board"
    ):
      CertificationEvidenceBundle.forObjectExtraction(
        current,
        Vector(
          CertificationEvidence(
            familyId = CertificationId("DevelopmentComparison"),
            color = Color.White,
            purposeStrengths = Map(
              CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied
            )
          ),
          CertificationEvidence(
            familyId = CertificationId("DevelopmentComparison"),
            color = Color.White,
            purposeStrengths = Map(
              CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Insufficient
            )
          )
        )
      )

  test("CertificationExtractor rejects stale evidence bundles bound to a different exact board"):
    val staleSource = CertificationRuntimeTestSupport.objectExtractionForRowId("cert-development-comparison-exact")
    val current = CertificationRuntimeTestSupport.objectExtractionForRowId("cert-development-comparison-near-miss")
    val staleBundle =
      CertificationEvidenceBundle.forObjectExtraction(
        staleSource,
        Vector(
          CertificationEvidence(
            familyId = CertificationId("DevelopmentComparison"),
            color = Color.White,
            purposeStrengths = Map(
              CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied
            )
          )
        )
      )

    assertEquals(
      CertificationExtractor.fromObjectExtraction(current, staleBundle),
      Left("Non-empty certification evidence bundle must be bound to the same current root state")
    )

  test("CertificationAdmissionSupport resolves required support families across object, delta, and certification families"):
    val current =
      StrategicObjectExtractor
        .fromFen(Fen.Full.clean("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 2 3"))
        .fold(message => fail(message), identity)
    val delta =
      StrategicDeltaExtractor
        .fromFens(
          Fen.Full.clean("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"),
          moveUci("d6c7"),
          Fen.Full.clean("4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1")
        )
        .fold(message => fail(message), identity)
    val extractedSoFar =
      CertificationSet(
        Vector(
          Certification(
            familyId = CertificationId("DevelopmentComparison"),
            scope = CertificationScope.Comparative,
            burdenTag = CertificationBurdenTag("development_superiority"),
            verdict = CertificationVerdict.Deferred,
            anchor = WitnessAnchor.BoardAnchor,
            owner = Some(Color.White)
          )
        )
      )

    val missing =
      CertificationAdmissionSupport.missingSupportFamilies(
        requiredSupportFamilies = Vector(
          CertificationSupportFamily("OpeningDevelopmentRegime"),
          CertificationSupportFamily("TradeInvariant"),
          CertificationSupportFamily("DevelopmentComparison"),
          CertificationSupportFamily("PromotionRace")
        ),
        current = current,
        delta = Some(delta),
        extractedSoFar = extractedSoFar
      )

    assertEquals(missing.map(_.value), Vector("PromotionRace"))

  test("CertificationAdmissionSupport keeps certification support-family checks color-sensitive"):
    val current =
      StrategicObjectExtractor
        .fromFen(Fen.Full.clean("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"))
        .fold(message => fail(message), identity)
    val extractedSoFar =
      CertificationSet(
        Vector(
          Certification(
            familyId = CertificationId("DevelopmentComparison"),
            scope = CertificationScope.Comparative,
            burdenTag = CertificationBurdenTag("development_superiority"),
            verdict = CertificationVerdict.Certified,
            anchor = WitnessAnchor.BoardAnchor,
            owner = Some(Color.White)
          )
        )
      )

    val missingForWhite =
      CertificationAdmissionSupport.missingSupportFamilies(
        requiredSupportFamilies = Vector(CertificationSupportFamily("DevelopmentComparison")),
        color = Color.White,
        current = current,
        delta = None,
        extractedSoFar = extractedSoFar
      )
    val missingForBlack =
      CertificationAdmissionSupport.missingSupportFamilies(
        requiredSupportFamilies = Vector(CertificationSupportFamily("DevelopmentComparison")),
        color = Color.Black,
        current = current,
        delta = None,
        extractedSoFar = extractedSoFar
      )

    assertEquals(missingForWhite, Vector.empty)
    assertEquals(missingForBlack.map(_.value), Vector("DevelopmentComparison"))

  test("CertificationExtractor exposes typed current-position extraction and preserves the current-root-state-bound evidence bundle"):
    val rowId = "cert-development-comparison-exact"
    val current = CertificationRuntimeTestSupport.objectExtractionForRowId(rowId)
    val evidenceBundle = CertificationRuntimeTestSupport.evidenceBundleForRowId(rowId)

    val extraction =
      CertificationExtractor
        .fromObjectExtraction(current, evidenceBundle)
        .fold(message => fail(message), identity)

    assertEquals(extraction.current, current)
    assertEquals(extraction.delta, None)
    assertEquals(extraction.evidenceBundle, evidenceBundle)
    assert(!evidenceBundle.isEmpty)
    assert(evidenceBundle.matches(current.rootState))
    assert(
      extraction.certifications.contains(
        "DevelopmentComparison",
        WitnessAnchor.BoardAnchor,
        owner = Some(Color.White),
        verdict = Some(CertificationVerdict.Certified)
      )
    )

  test("CertificationExtractor preserves the explicit empty sentinel evidence bundle"):
    val current = CertificationRuntimeTestSupport.objectExtractionForRowId("cert-development-comparison-exact")

    val extraction =
      CertificationExtractor
        .fromObjectExtraction(current, CertificationEvidenceBundle.empty)
        .fold(message => fail(message), identity)

    assertEquals(extraction.current, current)
    assertEquals(extraction.delta, None)
    assertEquals(extraction.evidenceBundle, CertificationEvidenceBundle.empty)
    assert(extraction.evidenceBundle.isEmpty)
    assert(extraction.evidenceBundle.matches(current.rootState))

  test("CertificationExtractor exposes typed transition extraction and keeps delta.after aligned with current extraction"):
    val delta =
      StrategicDeltaExtractor
        .fromFens(
          Fen.Full.clean("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"),
          moveUci("d6c7"),
          Fen.Full.clean("4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1")
        )
        .fold(message => fail(message), identity)

    val extraction =
      CertificationExtractor
        .fromDeltaExtraction(delta, CertificationEvidenceBundle.empty)
        .fold(message => fail(message), identity)

    assertEquals(extraction.current, delta.after)
    assertEquals(extraction.delta, Some(delta))
    assertEquals(extraction.evidenceBundle, CertificationEvidenceBundle.empty)

  test("CertificationExtractor rejects forged strategic delta payloads on the public transition path"):
    val delta =
      StrategicDeltaExtractor
        .fromFens(
          Fen.Full.clean("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"),
          moveUci("d6c7"),
          Fen.Full.clean("4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1")
        )
        .fold(message => fail(message), identity)
    val forged = delta.copy(deltas = StrategicDeltaSet.empty)

    val fromDeltaOnly =
      CertificationExtractor.fromDeltaExtraction(forged, CertificationEvidenceBundle.empty)
    val fromExplicitCurrent =
      CertificationExtractor.fromExtractions(
        forged.after,
        Some(forged),
        CertificationEvidenceBundle.empty
      )

    assert(
      fromDeltaOnly.left.exists(_.contains("canonical exact-board delta set")),
      clues(fromDeltaOnly)
    )
    assert(
      fromExplicitCurrent.left.exists(_.contains("canonical exact-board delta set")),
      clues(fromExplicitCurrent)
    )

  test("CertificationExtractor rejects forged canonical object carriers on current-position entrypoints"):
    val current = CertificationRuntimeTestSupport.objectExtractionForRowId("cert-development-comparison-exact")
    val forgedCurrent = current.copy(primaryWitnesses = WitnessSet.empty)

    val result =
      CertificationExtractor.fromObjectExtraction(forgedCurrent, CertificationEvidenceBundle.empty)

    assert(
      result.left.exists(_.contains("Certification current extraction canonicalization failed")),
      clues(result)
    )

  test("CertificationContext.build fails closed on malformed one-hot auxiliary root state"):
    val malformedRoot = RootStateVector.fromIndices(
      Vector(
        RootAtomRegistry.sideToMoveIndex(Color.White),
        RootAtomRegistry.sideToMoveIndex(Color.Black)
      )
    )
    val malformedExtraction =
      StrategicObjectExtraction(
        rootState = malformedRoot,
        primaryWitnesses = WitnessSet.empty,
        attachedWitnesses = WitnessSet.empty,
        objects = StrategicObjectSet.empty
      )

    val result = CertificationContext.build(malformedExtraction, CertificationEvidenceBundle.empty)

    assert(
      result.left.exists(_.contains("exactly one active side_to_move bit")),
      clues(result)
    )

  test("CertificationContext.build fails closed on raw castling rights that contradict the board"):
    val invalidRoot = RootStateVector.fromIndices(
      Vector(
        RootAtomRegistry.pieceOnIndex(Color.White, chess.King, chess.Square.E1),
        RootAtomRegistry.pieceOnIndex(Color.Black, chess.King, chess.Square.E8),
        RootAtomRegistry.sideToMoveIndex(Color.White),
        RootAtomRegistry.castlingRightsIndex(1),
        RootAtomRegistry.enPassantNoneIndex
      )
    )
    val invalidExtraction =
      StrategicObjectExtraction(
        rootState = invalidRoot,
        primaryWitnesses = WitnessSet.empty,
        attachedWitnesses = WitnessSet.empty,
        objects = StrategicObjectSet.empty
      )

    val result = CertificationContext.build(invalidExtraction, CertificationEvidenceBundle.empty)

    assert(
      result.left.exists(_.contains("Illegal castling-rights state (K)")),
      clues(result)
    )

  test("CertificationContext does not invent castling rights from board geometry alone"):
    val current =
      StrategicObjectExtractor
        .fromFen(Fen.Full.clean("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1"))
        .fold(message => fail(message), identity)
    val context =
      CertificationContext
        .build(current, CertificationEvidenceBundle.empty)
        .fold(message => fail(message), identity)

    assert(
      context
        .legalMoves(Color.White)
        .forall(move => !(move.orig.key == "e1" && Set("g1", "c1", "h1", "a1").contains(move.dest.key)))
    )

  test("CertificationContext keeps exact en passant capture availability from the root state"):
    val current =
      StrategicObjectExtractor
        .fromFenFailClosed(Fen.Full.clean("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"))
        .fold(message => fail(message), identity)
    val context =
      CertificationContext
        .build(current, CertificationEvidenceBundle.empty)
        .fold(message => fail(message), identity)

    assert(
      context.legalMoves(Color.White).exists(move => move.orig.key == "e5" && move.dest.key == "d6"),
      clues(context.legalMoves(Color.White).map(move => s"${move.orig.key}${move.dest.key}"))
    )

  test("CertificationContext legalMoveCount reflects castling-right availability for both colors"):
    val withRights =
      StrategicObjectExtractor
        .fromFenFailClosed(Fen.Full.clean("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"))
        .fold(message => fail(message), identity)
    val withoutRights =
      StrategicObjectExtractor
        .fromFenFailClosed(Fen.Full.clean("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1"))
        .fold(message => fail(message), identity)
    val withRightsContext =
      CertificationContext
        .build(withRights, CertificationEvidenceBundle.empty)
        .fold(message => fail(message), identity)
    val withoutRightsContext =
      CertificationContext
        .build(withoutRights, CertificationEvidenceBundle.empty)
        .fold(message => fail(message), identity)

    val extraWhiteMoves =
      withRightsContext.legalMoves(Color.White).map(move => s"${move.orig.key}${move.dest.key}").toSet --
        withoutRightsContext.legalMoves(Color.White).map(move => s"${move.orig.key}${move.dest.key}").toSet
    val extraBlackMoves =
      withRightsContext.legalMoves(Color.Black).map(move => s"${move.orig.key}${move.dest.key}").toSet --
        withoutRightsContext.legalMoves(Color.Black).map(move => s"${move.orig.key}${move.dest.key}").toSet

    assertEquals(extraWhiteMoves, Set("e1a1", "e1c1", "e1g1", "e1h1"))
    assertEquals(extraBlackMoves, Set("e8a8", "e8c8", "e8g8", "e8h8"))
    assertEquals(
      withRightsContext.legalMoveCount(Color.White),
      withoutRightsContext.legalMoveCount(Color.White) + extraWhiteMoves.size
    )
    assertEquals(
      withRightsContext.legalMoveCount(Color.Black),
      withoutRightsContext.legalMoveCount(Color.Black) + extraBlackMoves.size
    )

  test("CertificationContext keeps exact black en passant capture availability from the root state"):
    val withEnPassant =
      StrategicObjectExtractor
        .fromFenFailClosed(Fen.Full.clean("4k3/8/8/8/3Pp3/8/8/4K3 b - d3 0 1"))
        .fold(message => fail(message), identity)
    val withoutEnPassant =
      StrategicObjectExtractor
        .fromFenFailClosed(Fen.Full.clean("4k3/8/8/8/3Pp3/8/8/4K3 b - - 0 1"))
        .fold(message => fail(message), identity)
    val withEnPassantContext =
      CertificationContext
        .build(withEnPassant, CertificationEvidenceBundle.empty)
        .fold(message => fail(message), identity)
    val withoutEnPassantContext =
      CertificationContext
        .build(withoutEnPassant, CertificationEvidenceBundle.empty)
        .fold(message => fail(message), identity)

    assert(
      withEnPassantContext.legalMoves(Color.Black).exists(move => move.orig.key == "e4" && move.dest.key == "d3"),
      clues(withEnPassantContext.legalMoves(Color.Black).map(move => s"${move.orig.key}${move.dest.key}"))
    )
    assertEquals(
      withEnPassantContext.legalMoveCount(Color.Black),
      withoutEnPassantContext.legalMoveCount(Color.Black) + 1
    )

  test("Certification runtime keeps missing required evidence fail-closed as deferred"):
    val rowIds = Vector(
      "cert-development-comparison-exact",
      "cert-initiative-window-exact",
      "cert-comparative-king-fragility-exact",
      "cert-certified-king-safety-edge-exact",
      "cert-material-harvest-exact",
      "cert-fortress-draw-certification-exact",
      "cert-mate-net-certification-exact"
    )

    rowIds.foreach: rowId =>
      val extraction =
        CertificationExtractor
          .fromObjectExtraction(
            CertificationRuntimeTestSupport.objectExtractionForRowId(rowId),
            CertificationEvidenceBundle.empty
          )
          .fold(message => fail(message), identity)

      val ownerClaim = CertificationRuntimeTestSupport.ownerClaimForRowId(extraction, rowId)
      val rivalClaim = CertificationRuntimeTestSupport.rivalClaimForRowId(extraction, rowId)

      assertEquals(ownerClaim.verdict, CertificationVerdict.Deferred, clues(rowId))
      assertEquals(rivalClaim.verdict, CertificationVerdict.Rejected, clues(rowId))

  test("CertificationRule rejects live candidates when required support families are missing"):
    object SupportGatedRule extends CertificationRule:
      val familyId: CertificationId = CertificationId("SupportGatedSynthetic")
      val scope: CertificationScope = CertificationScope.Comparative
      val burdenTag: CertificationBurdenTag = CertificationBurdenTag("development_superiority")
      protected val helperTags: Vector[String] = Vector("synthetic_support_gate")
      protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
        Vector(CertificationEvidencePurpose.ComparativeSuperiority)
      protected val insufficientEvidenceVerdict: CertificationVerdict =
        CertificationVerdict.SupportOnly
      override protected val requiredSupportFamilies: Vector[CertificationSupportFamily] =
        Vector(CertificationSupportFamily("DevelopmentComparison"))

      protected def candidateFor(
          color: Color,
          context: CertificationContext,
          extractedSoFar: CertificationSet
      ): Option[CertificationCandidate] =
        Option.when(color.white)(CertificationCandidate())

    val current = CertificationRuntimeTestSupport.objectExtractionForRowId("cert-development-comparison-exact")
    val context =
      CertificationContext
        .build(
          current,
          CertificationRuntimeTestSupport.evidenceBundleForFamily(
            current = current,
            familyId = "SupportGatedSynthetic",
            color = Color.White,
            purposeStrengths = Map(
              CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied
            )
          )
        )
        .fold(message => fail(message), identity)

    val claim =
      SupportGatedRule
        .extract(context, CertificationSet.empty)
        .find(_.owner.contains(Color.White))
        .getOrElse(fail("missing white synthetic certification"))

    assertEquals(claim.verdict, CertificationVerdict.Rejected)
    assertEquals(
      claim.payload.get("missing_support_families"),
      Some(WitnessValue.TokenListValue(Vector("DevelopmentComparison")))
    )

  test("CertificationExtractor fails closed when the current extraction does not match delta.after"):
    val delta =
      StrategicDeltaExtractor
        .fromFens(
          Fen.Full.clean("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"),
          moveUci("d6c7"),
          Fen.Full.clean("4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1")
        )
        .fold(message => fail(message), identity)
    val wrongCurrent =
      StrategicObjectExtractor
        .fromFen(Fen.Full.clean("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 2 3"))
        .fold(message => fail(message), identity)

    assertEquals(
      CertificationExtractor.fromExtractions(
        wrongCurrent,
        Some(delta),
        CertificationEvidenceBundle.empty
      ),
      Left("Certification extraction must use the current object extraction that matches delta.after")
    )

  private def moveUci(uci: String): Uci.Move =
    Uci(uci) match
      case Some(move: Uci.Move) => move
      case Some(_) => fail(s"Unsupported non-move UCI: $uci")
      case None => fail(s"Invalid UCI: $uci")
