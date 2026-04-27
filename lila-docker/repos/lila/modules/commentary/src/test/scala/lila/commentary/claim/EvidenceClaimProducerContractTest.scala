package lila.commentary.claim

import chess.Color
import chess.Square
import chess.format.Fen

import lila.commentary.CommentaryCore
import lila.commentary.api.{ CommentaryApiJson, CommentaryBackendSeam, CommentaryRequest, CommentaryResponseStatus }
import lila.commentary.certification.*
import lila.commentary.projection.*
import lila.commentary.render.RenderStatus
import lila.commentary.selection.*
import lila.commentary.source.*
import lila.commentary.strategic.StrategicObjectExtraction
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessValue }
import lila.commentary.witness.seed.{ StrategySupportSeedExtraction, StrategySupportSeedExtractor }

class EvidenceClaimProducerContractTest extends munit.FunSuite:

  test("default backend path still produces exact-board claims"):
    val response = CommentaryBackendSeam.render(request(startingFen))

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(response.render.evidenceRefs.exists(ref => ref.kind == EvidenceRefKind.Object && ref.id == "piece_inventory"))

  test("bounded certification extraction can become a same-board certification claim"):
    val current = objectExtraction(startingFen)
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(certification = Some(certificationExtraction(current)))
      )

    assert(claims.exists(claim => claim.layer == ClaimLayer.Certification && claim.id == "certification-development-comparison-white-board"))
    assert(claims.filter(_.layer == ClaimLayer.Certification).forall(_.exactBoardBound))
    assert(claims.filter(_.layer == ClaimLayer.Certification).forall(_.lowerCarrierRefs.exists(_.kind == EvidenceRefKind.ExactBoard)))

  test("stale certification extraction and raw engine packet cannot create certification claims"):
    val current = objectExtraction(startingFen)
    val stale = objectExtraction(afterE4Fen)
    val staleClaims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(certification = Some(certificationExtraction(stale)))
      )
    val engineOnly =
      CommentaryBackendSeam.render(request(bareKingsFen, enginePacket = Some(emptyEnginePacket(bareKingsFen))))

    assert(!staleClaims.exists(_.layer == ClaimLayer.Certification))
    assertEquals(engineOnly.status, CommentaryResponseStatus.NoCommentary)
    assert(!engineOnly.render.evidenceRefs.exists(_.kind == EvidenceRefKind.Certification))
    assert(!engineOnly.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification))

  test("result-oracle certification families are not emitted"):
    val current = objectExtraction(startingFen)
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(certification = Some(certificationExtraction(current, family = "WinningEndgame")))
      )

    assert(!claims.exists(_.layer == ClaimLayer.Certification))
    assert(claims.forall(claim => !forbiddenWords.exists(token => claim.id.toLowerCase.contains(token))))

  test("claim-shaped projection handoff without runtime admission cannot create a projection claim"):
    val current = objectExtraction(startingFen)
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projection = Vector(centerReleaseProjection(current)))
      )

    assert(!claims.exists(_.layer == ClaimLayer.Projection))

  test("stale projection evidence bound to another exact board cannot create a projection claim"):
    val current = objectExtraction(startingFen)
    val stale = objectExtraction(afterE4Fen)
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projection = Vector(centerReleaseProjection(stale)))
      )

    assert(!claims.exists(_.layer == ClaimLayer.Projection))

  test("raw or unbound projection evidence cannot create a projection claim"):
    val current = objectExtraction(startingFen)
    val rawCarrier =
      centerReleaseProjection(current).copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.RawEngine, "raw-pv", owner = Some("white"), anchor = Some("board"), route = Some("center_pawn_target"), scope = Some("position_local"))
        )
      )
    val unboundCarrier =
      centerReleaseProjection(current).copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Witness, "available_lever_trigger")
        )
      )

    val rawClaims = EvidenceClaimProducer.produce(current, None, EvidenceClaimHandoff(projection = Vector(rawCarrier)))
    val unboundClaims = EvidenceClaimProducer.produce(current, None, EvidenceClaimHandoff(projection = Vector(unboundCarrier)))

    assert(!rawClaims.exists(_.layer == ClaimLayer.Projection))
    assert(!unboundClaims.exists(_.layer == ClaimLayer.Projection))

  test("admitted typed projection result with exact lower carrier can become a bounded projection claim"):
    val current = objectExtraction(centerReleaseFen)
    val admission = centerReleaseAdmission(current)
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projectionAdmissions = Vector(admission))
      )
    val projectionClaim =
      claims
        .find(claim => claim.layer == ClaimLayer.Projection && claim.band.contains("S05"))
        .getOrElse(fail("expected admitted projection claim"))

    assertEquals(projectionClaim.id, "projection-s05-white-d5-center-pawn-target")
    assertEquals(projectionClaim.status, ClaimStatus.Admitted)
    assertEquals(projectionClaim.owner, Some("white"))
    assertEquals(projectionClaim.beneficiary, Some("white"))
    assertEquals(projectionClaim.defender, Some("black"))
    assertEquals(projectionClaim.anchor, Some("d5"))
    assertEquals(projectionClaim.route, Some("center_pawn_target"))
    assertEquals(projectionClaim.scope, Some("position_local"))
    assertEquals(projectionClaim.wordingStrengthCap, WordingStrength.QualifiedSupport)
    assert(projectionClaim.exactBoardBound)
    assertEquals(projectionClaim.lowerCarrierRefs.exists(ref => ref.kind == EvidenceRefKind.Witness && ref.id == "available_lever_trigger"), true)
    assertEquals(projectionClaim.lowerCarrierRefs.exists(ref => ref.kind == EvidenceRefKind.Witness && ref.id == "pawn_push_break_contact_source"), true)
    assertEquals(projectionClaim.evidenceRefs.exists(ref => ref.kind == EvidenceRefKind.Projection && ref.id == "center_release_route_certified"), true)

    val outline = ClaimSelector.select(claims)
    assert(outline.lead.exists(_.claim.id == projectionClaim.id))

  test("admitted typed projection result may carry defender-owned exact lower carrier when the band requires it"):
    val current = objectExtraction(kingShelterBreachFen)
    val admission = kingShelterBreachAdmission(current)
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projectionAdmissions = Vector(admission))
      )
    val projectionClaim =
      claims
        .find(claim => claim.layer == ClaimLayer.Projection && claim.band.contains("S04"))
        .getOrElse(fail("expected admitted S04 projection claim"))

    assertEquals(projectionClaim.owner, Some("white"))
    assertEquals(projectionClaim.defender, Some("black"))
    assertEquals(projectionClaim.route, Some("shell_payload_breach"))
    assertEquals(projectionClaim.scope, Some("position_local"))
    assert(
      projectionClaim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Object &&
          ref.id == "KingSafetyShell" &&
          ref.owner.contains("black") &&
          ref.anchor.contains("g8") &&
          ref.route.contains("shell_payload_breach") &&
          ref.scope.contains("position_local")
      )
    )
    assert(
      projectionClaim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Certification &&
          ref.id == "CertifiedKingSafetyEdge" &&
          ref.owner.contains("white")
      )
    )

  test("typed projection admission cannot detach caller metadata from the Sxx evidence carrier that passed"):
    val current = objectExtraction(centerReleaseFen)
    val detached = detachedCenterReleaseAdmission(current)
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projectionAdmissions = Vector(detached))
      )

    assertEquals(detached.status, StrategyProjectionAdmissionStatus.Rejected)
    assert(!claims.exists(_.layer == ClaimLayer.Projection))

  test("rejected typed projection result cannot become a claim"):
    val current = objectExtraction(centerReleaseFen)
    val rejected = centerReleaseAdmission(current, route = "open_center_wording_only")
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projectionAdmissions = Vector(rejected))
      )

    assert(!claims.exists(_.layer == ClaimLayer.Projection))

  test("typed projection result missing exact lower carrier cannot become a claim"):
    val current = objectExtraction(centerReleaseFen)
    val missingCarrier = centerReleaseAdmission(current, lowerCarrierRefs = Vector.empty)
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projectionAdmissions = Vector(missingCarrier))
      )

    assert(!claims.exists(_.layer == ClaimLayer.Projection))

  test("typed projection result with wrong binding cannot become a claim"):
    val current = objectExtraction(centerReleaseFen)
    val wrongBinding =
      centerReleaseAdmission(
        current,
        lowerCarrierRefs = Vector(
          projectionCarrier(StrategyProjectionCarrierKind.Witness, "available_lever_trigger", route = "wrong_route"),
          projectionCarrier(StrategyProjectionCarrierKind.Witness, "pawn_push_break_contact_source")
        )
      )
    val stale = centerReleaseAdmission(objectExtraction(afterE4Fen))
    val wrongBindingClaims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projectionAdmissions = Vector(wrongBinding))
      )
    val staleClaims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projectionAdmissions = Vector(stale))
      )

    assert(!wrongBindingClaims.exists(_.layer == ClaimLayer.Projection))
    assert(!staleClaims.exists(_.layer == ClaimLayer.Projection))

  test("typed projection result with broad concept scope cannot become a claim"):
    val current = objectExtraction(centerReleaseFen)
    val broadScope = centerReleaseAdmission(current, scope = "broad_center_concept")
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projectionAdmissions = Vector(broadScope))
      )

    assert(!claims.exists(_.layer == ClaimLayer.Projection))

  test("projection claim wording cap blocks best forced result oracle theory recommendation wording"):
    val current = objectExtraction(centerReleaseFen)
    val projectionClaims =
      EvidenceClaimProducer
        .produce(
          current,
          None,
          EvidenceClaimHandoff(projectionAdmissions = Vector(centerReleaseAdmission(current)))
        )
        .filter(_.layer == ClaimLayer.Projection)

    assert(projectionClaims.nonEmpty)
    assertEquals(projectionClaims.forall(_.wordingStrengthCap.rank <= WordingStrength.QualifiedSupport.rank), true)
    assertEquals(projectionClaims.forall(claim => !forbiddenWords.exists(token => claim.id.toLowerCase.contains(token))), true)
    assertEquals(projectionClaims.forall(claim => !forbiddenWords.exists(token => claim.route.exists(_.toLowerCase.contains(token)))), true)
    assertEquals(projectionClaims.forall(claim => !forbiddenWords.exists(token => claim.scope.exists(_.toLowerCase.contains(token)))), true)

  test("normalized opening source context is context-only and preserves separate source families"):
    val sourceClaim =
      EvidenceClaimProducer
        .produce(
          objectExtraction(bareKingsFen),
          None,
          EvidenceClaimHandoff(sourceContext = Vector(openingCandidate()))
        )
        .find(_.layer == ClaimLayer.SourceContext)
        .getOrElse(fail("expected source context claim"))
    val outline = ClaimSelector.select(Vector(sourceClaim))

    assertEquals(sourceClaim.status, ClaimStatus.Context)
    assertEquals(outline.lead, None)
    assert(outline.context.exists(_.claim.id == "opening-source-context-catalan"))
    assert(sourceClaim.evidenceRefs.exists(_.id == "opening-source-use:master_reference"))
    assert(sourceClaim.evidenceRefs.exists(_.id == "opening-source-use:online_trend"))

  test("normalized motif, endgame, and retrieval context stay bounded by their source contracts"):
    val current = objectExtraction(bareKingsFen)
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(
          sourceContext = Vector(
            motifCandidate(),
            endgameCandidate(),
            retrievalCandidate()
          )
        )
      )

    assertEquals(claims.count(_.sourceContextKind.contains(SourceContextKind.Motif)), 1)
    assertEquals(claims.count(_.sourceContextKind.contains(SourceContextKind.EndgameStudy)), 1)
    assertEquals(claims.count(_.sourceContextKind.contains(SourceContextKind.Retrieval)), 1)
    assert(claims.forall(_.layer == ClaimLayer.SourceContext))
    assert(claims.forall(_.wordingStrengthCap == WordingStrength.ContextOnly))

  test("exact-board lead keeps source context subordinate in the backend path"):
    val seam =
      CommentaryBackendSeam.withEvidenceHandoffProvider(_ =>
        EvidenceClaimHandoff(sourceContext = Vector(openingCandidate()))
      )
    val response = seam.render(request(startingFen))

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(response.render.blocks.exists(_.role == lila.commentary.render.RenderRole.Primary))
    assert(response.render.blocks.exists(_.role == lila.commentary.render.RenderRole.Context))
    assert(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.Object))
    assert(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.SourceContext))

  test("no safe higher evidence preserves noCommentary or exact-board-only behavior"):
    val emptyHigher =
      EvidenceClaimProducer.produce(objectExtraction(bareKingsFen), None, EvidenceClaimHandoff.empty)
    val exactOnly =
      EvidenceClaimProducer.produce(objectExtraction(startingFen), None, EvidenceClaimHandoff.empty)

    assertEquals(emptyHigher, Vector.empty)
    assert(exactOnly.exists(_.layer == ClaimLayer.Object))
    assert(!exactOnly.exists(claim => claim.layer == ClaimLayer.Certification || claim.layer == ClaimLayer.Projection || claim.layer == ClaimLayer.SourceContext))

  test("public render/backend response does not expose raw evidence packets or debug internals"):
    val seam =
      CommentaryBackendSeam.withEvidenceHandoffProvider(_ =>
        EvidenceClaimHandoff(
          certification = Some(certificationExtraction(objectExtraction(startingFen))),
          projection = Vector(centerReleaseProjection(objectExtraction(startingFen))),
          projectionAdmissions = Vector(centerReleaseAdmission(objectExtraction(centerReleaseFen))),
          sourceContext = Vector(openingCandidate())
        )
      )
    val response = seam.renderDebug(request(startingFen))
    import CommentaryApiJson.given
    val json = play.api.libs.json.Json.toJson(response).toString

    assertEquals(response.render.status, RenderStatus.Rendered)
    assert(!json.contains("currentExtraction"))
    assert(!json.contains("rootState"))
    assert(!json.contains("certificationExtraction"))
    assert(!json.contains("projection ="))
    assert(!json.contains("StrategyProjectionAdmissionResult"))
    assert(!json.contains("StrategyProjectionEvidenceClaim"))
    assert(!json.contains("SourceContextInput"))
    assert(!json.contains("raw-pv"))
    assert(!json.contains("pvLines"))
    assert(!json.contains("engineConfigFingerprint"))
    assert(response.render.blocks.forall(_.text.publicText.forall(text => !forbiddenWords.exists(text.toLowerCase.contains))))

  private val forbiddenWords: Vector[String] =
    Vector("best", "forced", "winning", "drawn", "result", "oracle", "theory", "recommend")

  private val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val afterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
  private val bareKingsFen = "8/8/8/8/8/8/4k3/7K w - - 0 1"
  private val centerReleaseFen = "6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1"
  private val kingShelterBreachFen = "6k1/8/6p1/4B3/8/8/7R/6K1 w - - 0 1"

  private def objectExtraction(fen: String): StrategicObjectExtraction =
    CommentaryCore.extractStrategicObjectsFromFenFailClosed(fen).fold(fail(_), identity)

  private def certificationExtraction(
      current: StrategicObjectExtraction,
      family: String = "DevelopmentComparison"
  ): CertificationExtraction =
    val familyId = CertificationId(family)
    CertificationExtraction(
      current = current,
      delta = None,
      evidence = CertificationEvidenceBundle.forObjectExtraction(
        current,
        Vector(
          CertificationEvidence(
            familyId = familyId,
            color = Color.White,
            purposeStrengths = Map(
              CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied
            )
          )
        )
      ),
      claims = CertificationSet(
        Vector(
          Certification(
            familyId = familyId,
            scope = CertificationScope.Comparative,
            burdenTag = CertificationBurdenTag("development_comparison"),
            verdict = CertificationVerdict.Certified,
            anchor = WitnessAnchor.BoardAnchor,
            owner = Some(Color.White)
          )
        )
      )
    )

  private def centerReleaseProjection(current: StrategicObjectExtraction): ProjectionClaimCandidate =
    ProjectionClaimCandidate(
      id = "projection-center-release-white-board",
      rootState = current.rootState,
      bandId = StrategyProjectionScopeContract.S05,
      evidenceKind = StrategyProjectionScopeContract.CenterReleaseRouteCertified,
      owner = Color.White,
      anchor = WitnessAnchor.BoardAnchor,
      route = "center_pawn_target",
      scope = "position_local",
      lowerCarrierRefs = Vector(
        boundRef(EvidenceRefKind.Witness, "available_lever_trigger", "white", "center_pawn_target", "position_local"),
        boundRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source", "white", "center_pawn_target", "position_local")
      )
    )

  private def centerReleaseAdmission(
      current: StrategicObjectExtraction,
      route: String = "center_pawn_target",
      scope: String = "position_local",
      lowerCarrierRefs: Vector[StrategyProjectionCarrierRef] = Vector(
        projectionCarrier(StrategyProjectionCarrierKind.Witness, "available_lever_trigger"),
        projectionCarrier(StrategyProjectionCarrierKind.Witness, "pawn_push_break_contact_source")
      )
  ): StrategyProjectionAdmissionResult =
    val seed = seedExtraction(centerReleaseFen)
    StrategyProjectionAdmission.admit(
      projectionId = "projection-s05-white-d5-center-pawn-target",
      bandId = StrategyProjectionScopeContract.S05,
      extraction = seed,
      currentRootState = current.rootState,
      evidence = s05Evidence(seed, "c2", "d5", route),
      owner = Color.White,
      beneficiary = Some(Color.White),
      defender = Some(Color.Black),
      anchor = WitnessAnchor.SquareAnchor(squareFromKey("d5")),
      route = route,
      scope = scope,
      lowerCarrierRefs = lowerCarrierRefs,
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

  private def kingShelterBreachAdmission(current: StrategicObjectExtraction): StrategyProjectionAdmissionResult =
    val seed = seedExtraction(kingShelterBreachFen)
    StrategyProjectionAdmission.admit(
      projectionId = "projection-s04-white-g8-shell-payload-breach",
      bandId = StrategyProjectionScopeContract.S04,
      extraction = seed,
      currentRootState = current.rootState,
      evidence = s04Evidence(seed),
      owner = Color.White,
      beneficiary = Some(Color.White),
      defender = Some(Color.Black),
      anchor = WitnessAnchor.SquareAnchor(squareFromKey("g8")),
      route = "shell_payload_breach",
      scope = "position_local",
      lowerCarrierRefs = Vector(
        StrategyProjectionCarrierRef(
          kind = StrategyProjectionCarrierKind.Object,
          id = "KingSafetyShell",
          owner = "black",
          anchor = "g8",
          route = "shell_payload_breach",
          scope = "position_local"
        ),
        StrategyProjectionCarrierRef(
          kind = StrategyProjectionCarrierKind.Certification,
          id = "CertifiedKingSafetyEdge",
          owner = "white",
          anchor = "g8",
          route = "shell_payload_breach",
          scope = "position_local"
        )
      ),
      wordingStrengthCap = WordingStrength.QualifiedSupport,
      certificationEvidence = kingSafetyEdgeEvidenceFor(current, Color.White)
    )

  private def detachedCenterReleaseAdmission(current: StrategicObjectExtraction): StrategyProjectionAdmissionResult =
    val seed = seedExtraction(centerReleaseFen)
    val source = squareFromKey("c2")
    val realTarget = squareFromKey("d5")
    val detachedTarget = squareFromKey("e4")
    val evidence =
      StrategyProjectionEvidence.forSeedExtraction(
        seed,
        Vector(
          StrategyProjectionEvidenceClaim(
            bandId = StrategyProjectionScopeContract.S05,
            kind = StrategyProjectionScopeContract.CenterReleaseRouteCertified,
            owner = Color.White,
            anchor = WitnessAnchor.SquareAnchor(realTarget),
            payload = WitnessPayload(
              "contact_source_square" -> WitnessValue.SquareValue(source),
              "target_square" -> WitnessValue.SquareValue(realTarget),
              "center_release_route" -> WitnessValue.Token("center_pawn_target")
            )
          ),
          StrategyProjectionEvidenceClaim(
            bandId = StrategyProjectionScopeContract.S05,
            kind = StrategyProjectionScopeContract.CenterReleaseRouteCertified,
            owner = Color.White,
            anchor = WitnessAnchor.SquareAnchor(detachedTarget),
            payload = WitnessPayload(
              "contact_source_square" -> WitnessValue.SquareValue(source),
              "target_square" -> WitnessValue.SquareValue(detachedTarget),
              "center_release_route" -> WitnessValue.Token("center_pawn_target")
            )
          )
        )
      )
    StrategyProjectionAdmission.admit(
      projectionId = "projection-s05-white-e4-detached",
      bandId = StrategyProjectionScopeContract.S05,
      extraction = seed,
      currentRootState = current.rootState,
      evidence = evidence,
      owner = Color.White,
      beneficiary = Some(Color.White),
      defender = Some(Color.Black),
      anchor = WitnessAnchor.SquareAnchor(detachedTarget),
      route = "center_pawn_target",
      scope = "position_local",
      lowerCarrierRefs = Vector(
        StrategyProjectionCarrierRef(
          kind = StrategyProjectionCarrierKind.Witness,
          id = "available_lever_trigger",
          owner = "white",
          anchor = "e4",
          route = "center_pawn_target",
          scope = "position_local"
        )
      ),
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

  private def projectionCarrier(
      kind: StrategyProjectionCarrierKind,
      id: String,
      owner: String = "white",
      anchor: String = "d5",
      route: String = "center_pawn_target",
      scope: String = "position_local"
  ): StrategyProjectionCarrierRef =
    StrategyProjectionCarrierRef(
      kind = kind,
      id = id,
      owner = owner,
      anchor = anchor,
      route = route,
      scope = scope
    )

  private def seedExtraction(fen: String): StrategySupportSeedExtraction =
    StrategySupportSeedExtractor
      .fromFen(Fen.Full.clean(fen))
      .fold(message => fail(message), identity)

  private def s05Evidence(
      extraction: StrategySupportSeedExtraction,
      contactSourceSquare: String,
      targetSquare: String,
      centerReleaseRoute: String
  ): StrategyProjectionEvidence =
    val source = squareFromKey(contactSourceSquare)
    val target = squareFromKey(targetSquare)
    StrategyProjectionEvidence.forSeedExtraction(
      extraction,
      Vector(
        StrategyProjectionEvidenceClaim(
          bandId = StrategyProjectionScopeContract.S05,
          kind = StrategyProjectionScopeContract.CenterReleaseRouteCertified,
          owner = Color.White,
          anchor = WitnessAnchor.SquareAnchor(target),
          payload = WitnessPayload(
            "contact_source_square" -> WitnessValue.SquareValue(source),
            "target_square" -> WitnessValue.SquareValue(target),
            "center_release_route" -> WitnessValue.Token(centerReleaseRoute)
          )
        )
      )
    )

  private def s04Evidence(extraction: StrategySupportSeedExtraction): StrategyProjectionEvidence =
    val defendingKing = squareFromKey("g8")
    val shellAnchor = squareFromKey("g7")
    StrategyProjectionEvidence.forSeedExtraction(
      extraction,
      Vector(
        StrategyProjectionEvidenceClaim(
          bandId = StrategyProjectionScopeContract.S04,
          kind = StrategyProjectionScopeContract.KingShelterBreachRouteCertified,
          owner = Color.White,
          anchor = WitnessAnchor.SquareAnchor(defendingKing),
          payload = WitnessPayload(
            "defending_king_square" -> WitnessValue.SquareValue(defendingKing),
            "shell_anchor_square" -> WitnessValue.SquareValue(shellAnchor),
            "breach_squares" -> WitnessValue.SquareListValue(Vector("f6", "h6", "g7", "h7").map(squareFromKey)),
            "king_shelter_breach_route" -> WitnessValue.Token("shell_payload_breach"),
            "certification_family" -> WitnessValue.Token("CertifiedKingSafetyEdge")
          )
        )
      )
    )

  private def kingSafetyEdgeEvidenceFor(
      current: StrategicObjectExtraction,
      owner: Color
  ): CertificationEvidenceBundle =
    CertificationEvidenceBundle.forObjectExtraction(
      current,
      Vector(
        CertificationEvidence(
          familyId = CertificationId("ComparativeKingFragility"),
          color = owner,
          purposeStrengths =
            Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        ),
        CertificationEvidence(
          familyId = CertificationId("CertifiedKingSafetyEdge"),
          color = owner,
          purposeStrengths = Map(
            CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied,
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
          )
        )
      )
    )

  private def squareFromKey(square: String): Square =
    Square.fromKey(square).getOrElse(fail(s"bad square $square"))

  private def boundRef(
      kind: EvidenceRefKind,
      id: String,
      owner: String,
      route: String,
      scope: String
  ): EvidenceRef =
    EvidenceRef(
      kind = kind,
      id = id,
      owner = Some(owner),
      anchor = Some("board"),
      route = Some(route),
      scope = Some(scope)
    )

  private def openingCandidate(): SourceContextCandidate =
    SourceContextAdapter
      .normalize(
        SourceContextInput.Opening(
          candidateId = "opening-source-context-catalan",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("master_reference", "online_trend")
        )
      )
      .fold(error => fail(error.reason), identity)

  private def motifCandidate(): SourceContextCandidate =
    SourceContextAdapter
      .normalize(
        SourceContextInput.Motif(
          candidateId = "motif-source-context-pin",
          motifExampleId = "pin-carrier",
          detectorCarrierId = "pin-carrier",
          motifId = Some("pin")
        )
      )
      .fold(error => fail(error.reason), identity)

  private def endgameCandidate(): SourceContextCandidate =
    SourceContextAdapter
      .normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "endgame-source-context-philidor",
          studyId = "philidor_rook_pawn",
          applicabilityFixtureId = "philidor-applicability",
          applicabilityVerified = true
        )
      )
      .fold(error => fail(error.reason), identity)

  private def retrievalCandidate(): SourceContextCandidate =
    SourceContextAdapter
      .normalize(
        SourceContextInput.Retrieval(
          candidateId = "retrieval-source-context-example",
          retrievalExampleId = "similar-plan-sequence"
        )
      )
      .fold(error => fail(error.reason), identity)

  private def request(
      currentFen: String,
      enginePacket: Option[CertificationEngineRuntimeIntake.RuntimeEnginePacket] = None
  ): CommentaryRequest =
    CommentaryRequest(
      currentFen = currentFen,
      beforeFen = None,
      playedMove = None,
      nodeId = "evidence-producer-node",
      ply = 0,
      enginePacket = enginePacket
    )

  private def emptyEnginePacket(fen: String): CertificationEngineRuntimeIntake.RuntimeEnginePacket =
    CertificationEngineRuntimeIntake.RuntimeEnginePacket(
      fen = fen,
      nodeId = "evidence-producer-node",
      ply = 0,
      requestedDepth = 18,
      realizedDepth = 18,
      multiPv = 1,
      completed = true,
      generatedAtEpochMs = 0L,
      maxAgeMs = 60_000L,
      engineConfigFingerprint = "evidence-producer-empty-engine",
      score = CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(0),
      scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.White,
      pvLines = Vector.empty,
      claims = Vector.empty
    )
