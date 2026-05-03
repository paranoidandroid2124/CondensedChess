package lila.commentary.selection

import play.api.libs.json.*
import lila.commentary.projection.StrategyProjectionScopeContract

import java.nio.file.{ Files, Paths }

class CommentarySelectionContractTest extends munit.FunSuite:

  private val plannerRows = SelectionCorpus.loadPlannerRows()
  private val surfaceRows = SelectionCorpus.loadSurfaceRows()

  private def assert(condition: => Boolean)(using loc: munit.Location): Unit =
    super.assert(condition, clues(""))

  private def assertEquals[A, B](obtained: A, expected: B)(using
      loc: munit.Location,
      compare: munit.Compare[A, B],
      diffOptions: munit.diff.DiffOptions
  ): Unit =
    super.assertEquals(obtained, expected, clues(""))

  test("certified exact-board conversion lead beats opening and retrieval context"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "claim-material-harvest",
          layer = ClaimLayer.Certification,
          status = ClaimStatus.Admitted,
          owner = Some("white"),
          anchor = Some("square:c7"),
          route = Some("material_harvest"),
          impact = ClaimImpact(resultMaterialImpact = 90, forcedness = 80, immediacy = 80),
          evidenceRefs = Vector(
            EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("square:c7"), Some("material_harvest")),
            EvidenceRef(EvidenceRefKind.ExactBoard, "planner-selection-certified-conversion-vs-opening")
          ),
          wordingStrengthCap = WordingStrength.AssertiveCertified
        ),
        sourceClaim("source-catalan-context", SourceContextKind.Opening),
        sourceClaim("source-catalan-retrieval", SourceContextKind.Retrieval)
      )
    )

    assertEquals(outline.lead.map(_.claim.id), Some("claim-material-harvest"))
    assertEquals(outline.lead.map(_.bucket), Some(ClaimBucket.MustLead))
    assert(outline.context.exists(_.claim.id == "source-catalan-context"))
    assert(outline.context.exists(_.claim.id == "source-catalan-retrieval"))
    assertEquals(outline.wordingStrengthCap, WordingStrength.AssertiveCertified)

  test("raw engine swing is suppressed and cannot become support truth"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "raw-engine-swing",
          layer = ClaimLayer.Engine,
          status = ClaimStatus.Admitted,
          impact = ClaimImpact(evalSwing = 500),
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-cp-plus-500"))
        )
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "raw-engine-swing", SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, "raw-engine-swing", SuppressionReason.NoBoardReason)

  test("generic current-board tactical liability cannot become public lead"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "generic-loose-piece-smell",
          layer = ClaimLayer.Object,
          status = ClaimStatus.Admitted,
          owner = Some("white"),
          beneficiary = Some("white"),
          defender = Some("black"),
          sideToMove = Some("white"),
          anchor = Some("d4"),
          route = Some("tactical_liability"),
          scope = Some("position_local"),
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Root, "loose_piece", Some("white"), Some("d4"), Some("tactical_liability"), Some("position_local"))),
          lowerCarrierRefs = Vector.empty
        )
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "generic-loose-piece-smell", SuppressionReason.ForbiddenShortcut)

  test("move-local loose-piece claims require an immediate-capture carrier"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "loose-without-capture-payload",
          layer = ClaimLayer.Delta,
          status = ClaimStatus.Admitted,
          owner = Some("white"),
          beneficiary = Some("white"),
          defender = Some("black"),
          sideToMove = Some("white"),
          anchor = Some("d4"),
          route = Some("moved_piece_left_loose"),
          scope = Some("move_local"),
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Root, "loose_piece", Some("white"), Some("d4"), Some("moved_piece_left_loose"), Some("move_local"))),
          lowerCarrierRefs = Vector.empty
        )
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "loose-without-capture-payload", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "loose-without-capture-payload", SuppressionReason.NoBoardReason)

  test("raw engine refs cannot be smuggled through certification or support"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "cert-with-raw-engine-ref",
          layer = ClaimLayer.Certification,
          status = ClaimStatus.Admitted,
          owner = Some("white"),
          anchor = Some("board"),
          route = Some("raw_eval_shortcut"),
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-cp-plus-500"))
        ),
        selectionClaim(
          id = "support-only-raw-engine",
          layer = ClaimLayer.Engine,
          status = ClaimStatus.SupportOnly,
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-pv-line"))
        )
      )
    )

    assertEquals(outline.lead, None)
    assert(!outline.support.exists(_.claim.id == "support-only-raw-engine"))
    assertSuppressed(outline, "cert-with-raw-engine-ref", SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, "cert-with-raw-engine-ref", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "support-only-raw-engine", SuppressionReason.RawEngineOnly)

  test("raw engine lower carrier cannot be smuggled through certification board claim"):
    val claim = engineCertifiedClaim(
      id = "cert-with-raw-engine-lower-carrier",
      route = Some("material_harvest"),
      impact = ClaimImpact(resultMaterialImpact = 90, evalSwing = 700, evidenceConfidence = 90),
      certificationId = "MaterialHarvest",
      boardReasons = Vector(EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))),
      wordingStrengthCap = WordingStrength.AssertiveCertified
    ).copy(
      lowerCarrierRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-lower-carrier-eval"))
    )

    val outline = ClaimSelector.select(Vector(claim))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "cert-with-raw-engine-lower-carrier", SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, "cert-with-raw-engine-lower-carrier", SuppressionReason.NoBoardReason)

  test("engine evidence can select only after bounded Certification evidence exists"):
    val outline = ClaimSelector.select(
      Vector(
        engineCertifiedClaim(
          id = "certified-engine-swing",
          route = Some("best_defense_survival"),
          impact = ClaimImpact(resultMaterialImpact = 60, evalSwing = 320, evidenceConfidence = 95),
          boardReasons = Vector(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("board"), Some("best_defense_survival"), Some("position_local"))),
          wordingStrengthCap = WordingStrength.AssertiveCertified
        )
      )
    )

    assertEquals(outline.lead.map(_.claim.id), Some("certified-engine-swing"))
    assertEquals(outline.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), true)

  test("engine certification cannot use generic exact-board lower carrier as board reason"):
    val exactBoardCarrier =
      EvidenceRef(EvidenceRefKind.ExactBoard, "certification-current-board", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val claim =
      engineCertifiedClaim(
        id = "engine-certified-exact-board-carrier",
        route = Some("material_harvest"),
        impact = ClaimImpact(resultMaterialImpact = 90, evalSwing = 500, evidenceConfidence = 95),
        certificationId = "MaterialHarvest",
        boardReasons = Vector.empty,
        wordingStrengthCap = WordingStrength.AssertiveCertified
      ).copy(lowerCarrierRefs = Vector(exactBoardCarrier))

    val outline = ClaimSelector.select(Vector(claim))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "engine-certified-exact-board-carrier", SuppressionReason.NoBoardReason)

  test("engine certified eval swing without board reason cannot become lead"):
    val opaqueEval = engineCertifiedClaim(
      id = "engine-certified-opaque-eval",
      route = Some("opaque_eval_swing"),
      impact = ClaimImpact(resultMaterialImpact = 95, evalSwing = 900, evidenceConfidence = 95),
      boardReasons = Vector.empty,
      wordingStrengthCap = WordingStrength.AssertiveCertified
    )
    val outline = ClaimSelector.select(Vector(opaqueEval))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "engine-certified-opaque-eval", SuppressionReason.NoBoardReason)

  test("board explainable Sxx beats larger opaque engine certified eval swing"):
    val sxx = admittedProjection("s03-board-explainable-diagonal", "S03", anchor = "king:g8", route = "king_facing_diagonal_entry", score = 70)
    val opaqueEval = engineCertifiedClaim(
      id = "engine-certified-large-opaque-swing",
      anchor = Some("king:g8"),
      route = Some("opaque_eval_swing"),
      impact = ClaimImpact(resultMaterialImpact = 96, evalSwing = 1200, evidenceConfidence = 96),
      boardReasons = Vector.empty,
      wordingStrengthCap = WordingStrength.AssertiveCertified
    )

    val outline = ClaimSelector.select(Vector(opaqueEval, sxx))

    assertEquals(outline.lead.map(_.claim.id), Some("s03-board-explainable-diagonal"))
    assertSuppressed(outline, "engine-certified-large-opaque-swing", SuppressionReason.NoBoardReason)

  test("engine certified board reason can beat Sxx when result impact justifies it"):
    val sxx = admittedProjection("s07-board-explainable-initiative", "S07", anchor = "board", route = "development_led_window", score = 88)
    val certifiedConversion = engineCertifiedClaim(
      id = "engine-certified-material-conversion",
      route = Some("material_harvest"),
      impact = ClaimImpact(resultMaterialImpact = 96, forcedness = 80, evalSwing = 850, evidenceConfidence = 94, boardExplainability = 85),
      certificationId = "MaterialHarvest",
      boardReasons = Vector(EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))),
      wordingStrengthCap = WordingStrength.AssertiveCertified
    )

    val outline = ClaimSelector.select(Vector(sxx, certifiedConversion))

    assertEquals(outline.lead.map(_.claim.id), Some("engine-certified-material-conversion"))
    assertEquals(outline.lead.map(_.bucket), Some(ClaimBucket.MustLead))
    assert(outline.support.exists(_.claim.id == "s07-board-explainable-initiative"))

  test("engine certified stale wrong binding and config evidence is suppressed before ranking"):
    val stale = engineCertifiedClaim(
      id = "engine-certified-stale-evidence",
      impact = ClaimImpact(resultMaterialImpact = 99, evalSwing = 900, evidenceConfidence = 99),
      boardReasons = Vector(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("board"), Some("route"), Some("position_local"))),
      exactBoardBound = false
    )
    val wrongNode = engineCertifiedClaim(
      id = "engine-certified-wrong-node",
      impact = ClaimImpact(resultMaterialImpact = 98, evalSwing = 800, evidenceConfidence = 98),
      boardReasons = Vector(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("board"), Some("route"), Some("position_local"))),
      engineRef = Some(EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("white"), Some("wrong-node"), Some("route"), Some("position_local")))
    )
    val wrongFen = engineCertifiedClaim(
      id = "engine-certified-wrong-fen",
      impact = ClaimImpact(resultMaterialImpact = 97, evalSwing = 700, evidenceConfidence = 97),
      boardReasons = Vector(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("board"), Some("route"), Some("position_local"))),
      engineRef = Some(EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("wrong-owner"), Some("board"), Some("route"), Some("position_local")))
    )
    val wrongRoute = engineCertifiedClaim(
      id = "engine-certified-wrong-route",
      impact = ClaimImpact(resultMaterialImpact = 96, evalSwing = 650, evidenceConfidence = 96),
      boardReasons = Vector(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("board"), Some("route"), Some("position_local"))),
      engineRef = Some(EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("white"), Some("board"), Some("other_route"), Some("position_local")))
    )
    val wrongConfig = engineCertifiedClaim(
      id = "engine-certified-wrong-config",
      impact = ClaimImpact(resultMaterialImpact = 95, evalSwing = 600, evidenceConfidence = 95),
      boardReasons = Vector(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("board"), Some("route"), Some("position_local"))),
      engineRef = Some(EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("white"), Some("board"), Some("route"), Some("engine_config:other")))
    )

    val outline = ClaimSelector.select(Vector(stale, wrongNode, wrongFen, wrongRoute, wrongConfig))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "engine-certified-stale-evidence", SuppressionReason.StaleEvidence)
    assertSuppressed(outline, "engine-certified-wrong-node", SuppressionReason.WrongAnchor)
    assertSuppressed(outline, "engine-certified-wrong-fen", SuppressionReason.WrongOwner)
    assertSuppressed(outline, "engine-certified-wrong-route", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "engine-certified-wrong-config", SuppressionReason.ScopeMismatch)

  test("engine certified MultiPV ambiguity and mate cp boundary stay non-leading"):
    val ambiguousBestDefense = engineCertifiedClaim(
      id = "engine-certified-multipv-ambiguous",
      status = ClaimStatus.Deferred,
      impact = ClaimImpact(resultMaterialImpact = 95, evalSwing = 900, evidenceConfidence = 95),
      suppressionHints = Vector(SuppressionReason.NoBoardReason),
      boardReasons = Vector(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("board"), Some("route"), Some("position_local")))
    )
    val untypedMateCpScore = engineCertifiedClaim(
      id = "engine-certified-mate-cp-untyped",
      status = ClaimStatus.SupportOnly,
      impact = ClaimImpact(resultMaterialImpact = 95, evalSwing = 1000, evidenceConfidence = 95),
      suppressionHints = Vector(SuppressionReason.NoBoardReason),
      boardReasons = Vector(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("board"), Some("route"), Some("position_local")))
    )

    val outline = ClaimSelector.select(Vector(ambiguousBestDefense, untypedMateCpScore))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "engine-certified-multipv-ambiguous", SuppressionReason.Deferred)
    assertSuppressed(outline, "engine-certified-multipv-ambiguous", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "engine-certified-mate-cp-untyped", SuppressionReason.SupportOnly)
    assertSuppressed(outline, "engine-certified-mate-cp-untyped", SuppressionReason.NoBoardReason)

  test("engine certification evidence cannot lead without same-root Certification evidence"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "engine-cert-without-root-cert",
          layer = ClaimLayer.Certification,
          status = ClaimStatus.Admitted,
          owner = Some("white"),
          anchor = Some("board"),
          route = Some("best_defense_survival"),
          impact = ClaimImpact(resultMaterialImpact = 80, evalSwing = 420, evidenceConfidence = 90),
          evidenceRefs = Vector(
            EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("white"), Some("board"), Some("best_defense_survival"), Some("position_local"))
          ),
          wordingStrengthCap = WordingStrength.AssertiveCertified
        )
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "engine-cert-without-root-cert", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "engine-cert-without-root-cert", SuppressionReason.NoBoardReason)

  test("engine certification evidence cannot lead with unscoped claim and refs"):
    val outline = ClaimSelector.select(
      Vector(
        engineCertifiedClaim(
          id = "engine-cert-unscoped-binding",
          scope = None,
          impact = ClaimImpact(resultMaterialImpact = 90, evalSwing = 620, evidenceConfidence = 95),
          boardReasons = Vector(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("board"), Some("route"), None)),
          wordingStrengthCap = WordingStrength.AssertiveCertified
        )
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "engine-cert-unscoped-binding", SuppressionReason.ScopeMismatch)
    assertSuppressed(outline, "engine-cert-unscoped-binding", SuppressionReason.NoBoardReason)

  test("source context refs cannot be smuggled through board claims"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "cert-with-source-ref",
          layer = ClaimLayer.Certification,
          status = ClaimStatus.Admitted,
          owner = Some("white"),
          anchor = Some("board"),
          route = Some("source_shortcut"),
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "retrieval-example"))
        )
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "cert-with-source-ref", SuppressionReason.SourceContextOnly)
    assertSuppressed(outline, "cert-with-source-ref", SuppressionReason.NoBoardReason)

  test("S24 is not a generic tactic owner"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "generic-fork-picture",
          layer = ClaimLayer.Projection,
          status = ClaimStatus.Admitted,
          band = Some("S24"),
          owner = Some("white"),
          anchor = Some("piece:e4"),
          route = Some("generic_tactic"),
          impact = ClaimImpact(forcedness = 80, immediacy = 90),
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.ExactBoard, "proj-s24-tactic-alone-nasty-negative")),
          lowerCarrierRefs = Vector.empty
        )
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "generic-fork-picture", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "generic-fork-picture", SuppressionReason.NoBoardReason)

  test("S24 remains public-closed even with same-target forcing and conversion evidence"):
    val forcingOnly = admittedProjection("s24-forcing-only", "S24", anchor = "piece:e4", route = "same_target_realization", score = 90)
      .copy(
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.Projection, "same_target_forcing_realization", Some("white"), Some("piece:e4"), Some("same_target_realization"), Some("position_local"))
        )
      )
    val complete = forcingOnly.copy(
      id = "s24-complete",
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Projection, "same_target_forcing_realization", Some("white"), Some("piece:e4"), Some("same_target_realization"), Some("position_local")),
        EvidenceRef(EvidenceRefKind.Projection, "same_target_conversion_certified", Some("white"), Some("piece:e4"), Some("same_target_realization"), Some("position_local"))
      )
    )
    val outline = ClaimSelector.select(Vector(forcingOnly, complete))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s24-complete", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s24-complete", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "s24-forcing-only", SuppressionReason.ForbiddenShortcut)

  test("admitted Sxx can lead only with exact lower carrier and allowed evidence kind"):
    val admitted = selectionClaim(
      id = "admitted-s07",
      layer = ClaimLayer.Projection,
      status = ClaimStatus.Admitted,
      band = Some("S07"),
      owner = Some("white"),
      anchor = Some("board"),
      route = Some("development_led_window"),
      impact = ClaimImpact(persistenceAfterDefense = 65, evidenceConfidence = 80),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Projection, "initiative_conversion_route_certified", Some("white"), Some("board"), Some("development_led_window"), Some("position_local"))
      ),
      lowerCarrierRefs = Vector(
        EvidenceRef(EvidenceRefKind.Object, "OpeningDevelopmentRegime", Some("white"), Some("board"), Some("development_led_window"), Some("position_local")),
        EvidenceRef(EvidenceRefKind.Certification, "DevelopmentComparison", Some("white"), Some("board"), Some("development_led_window"), Some("position_local")),
        EvidenceRef(EvidenceRefKind.Certification, "InitiativeWindow", Some("white"), Some("board"), Some("development_led_window"), Some("position_local"))
      )
    )
    val stale = admitted.copy(
      id = "stale-s07",
      exactBoardBound = false,
      suppressionHints = Vector(SuppressionReason.StaleEvidence)
    )
    val outline = ClaimSelector.select(Vector(stale, admitted))

    assertEquals(outline.lead.map(_.claim.id), Some("admitted-s07"))
    assertSuppressed(outline, "stale-s07", SuppressionReason.StaleEvidence)

  test("supportOnly, deferred, and anti-case claims cannot become lead"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim("support-only-cert", ClaimLayer.Certification, ClaimStatus.SupportOnly),
        selectionClaim("deferred-cert", ClaimLayer.Certification, ClaimStatus.Deferred),
        selectionClaim("anti-case-cert", ClaimLayer.Certification, ClaimStatus.AntiCase)
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "support-only-cert", SuppressionReason.SupportOnly)
    assertSuppressed(outline, "deferred-cert", SuppressionReason.Deferred)
    assertSuppressed(outline, "anti-case-cert", SuppressionReason.AntiCase)

  test("context-only outline is allowed only when no stronger exact-board lead exists"):
    val lucenaReference = sourceClaim("lucena-reference", SourceContextKind.EndgameStudy)
      .copy(
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.SourceContext, "endgame-study:lucena-reference:applicable"),
          EvidenceRef(
            EvidenceRefKind.ExactBoard,
            "endgame-study-applicability:lucena-reference",
            route = Some("lucena-reference"),
            scope = Some("exact_endgame_applicability")
          )
        )
      )
    val contextOnly = ClaimSelector.select(Vector(lucenaReference))
    assertEquals(contextOnly.lead, None)
    assertEquals(contextOnly.context.map(_.claim.id), Vector("lucena-reference"))
    assertEquals(contextOnly.wordingStrengthCap, WordingStrength.ContextOnly)
    assertSelectedContextReason(contextOnly, "lucena-reference", SuppressionReason.SourceContextOnly)
    assertNotSuppressed(contextOnly, "lucena-reference")

    val withLead = ClaimSelector.select(
      Vector(
        lucenaReference,
        selectionClaim(
          id = "certified-hold",
          layer = ClaimLayer.Certification,
          status = ClaimStatus.Admitted,
          owner = Some("black"),
          anchor = Some("square:g8"),
          route = Some("fortress_hold"),
          impact = ClaimImpact(resultMaterialImpact = 80, persistenceAfterDefense = 90),
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "FortressDrawCertification", Some("black"), Some("square:g8"), Some("fortress_hold")))
        )
      )
    )
    assertEquals(withLead.lead.map(_.claim.id), Some("certified-hold"))
    assertEquals(withLead.context.map(_.claim.id), Vector("lucena-reference"))

  test("retrieval snippet cannot become current-position truth"):
    val outline = ClaimSelector.select(
      Vector(sourceClaim("retrieval-catalan-example", SourceContextKind.Retrieval).copy(status = ClaimStatus.Admitted))
    )

    assertEquals(outline.lead, None)
    assertEquals(outline.context.map(_.claim.id), Vector("retrieval-catalan-example"))
    assertSelectedContextReason(outline, "retrieval-catalan-example", SuppressionReason.RetrievalNonAuthoritative)
    assertNotSuppressed(outline, "retrieval-catalan-example")

  test("wrong owner anchor route and scope suppress dependent claim"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "wrong-route-s08",
          layer = ClaimLayer.Projection,
          status = ClaimStatus.Admitted,
          band = Some("S08"),
          owner = Some("white"),
          anchor = Some("square:d5"),
          route = Some("wrong_source"),
          scope = Some("move_local"),
          evidenceRefs = Vector(
            EvidenceRef(
              kind = EvidenceRefKind.Projection,
              id = "counterplay_denial_route_certified",
              owner = Some("black"),
              anchor = Some("square:e5"),
              route = Some("other_source"),
              scope = Some("position_local")
            )
          ),
          lowerCarrierRefs = Vector(
            EvidenceRef(
              kind = EvidenceRefKind.Certification,
              id = "InitiativeWindow",
              owner = Some("black"),
              anchor = Some("square:e5"),
              route = Some("other_source"),
              scope = Some("position_local")
            )
          )
        )
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "wrong-route-s08", SuppressionReason.WrongOwner)
    assertSuppressed(outline, "wrong-route-s08", SuppressionReason.WrongAnchor)
    assertSuppressed(outline, "wrong-route-s08", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "wrong-route-s08", SuppressionReason.ScopeMismatch)

  test("ambiguous side beneficiary and defender fail closed"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "ambiguous-beneficiary",
          layer = ClaimLayer.Certification,
          status = ClaimStatus.Admitted,
          owner = Some("white"),
          beneficiary = None,
          defender = None,
          sideToMove = None,
          anchor = Some("board"),
          route = Some("conversion")
        )
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "ambiguous-beneficiary", SuppressionReason.WrongOwner)
    assertSuppressed(outline, "ambiguous-beneficiary", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "ambiguous-beneficiary", SuppressionReason.ScopeMismatch)

  test("raw engine lower carrier cannot admit Sxx projection"):
    val outline = ClaimSelector.select(
      Vector(
        admittedProjection("s07-raw-lower", "S07", route = "development_led_window", score = 70)
          .copy(lowerCarrierRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-engine-pv")))
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s07-raw-lower", SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, "s07-raw-lower", SuppressionReason.ForbiddenShortcut)

  test("unbound lower carrier cannot admit Sxx projection"):
    val outline = ClaimSelector.select(
      Vector(
        admittedProjection("s07-unbound-lower", "S07", route = "development_led_window", score = 70)
          .copy(lowerCarrierRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "InitiativeWindow")))
      )
    )

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s07-unbound-lower", SuppressionReason.WrongOwner)
    assertSuppressed(outline, "s07-unbound-lower", SuppressionReason.WrongAnchor)
    assertSuppressed(outline, "s07-unbound-lower", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "s07-unbound-lower", SuppressionReason.ScopeMismatch)

  test("competing Sxx claims require nonredundancy by owner anchor route and scope"):
    val strong = admittedProjection("s07-main", "S07", route = "development_led_window", score = 70)
    val weakerDuplicate = admittedProjection("s07-duplicate", "S07", route = "development_led_window", score = 40)
    val independent = admittedProjection("s21-independent", "S21", anchor = "square:d5", route = "center_source_survives", score = 50)

    val outline = ClaimSelector.select(Vector(weakerDuplicate, independent, strong))

    assertEquals(outline.lead.map(_.claim.id), Some("s07-main"))
    assert(outline.support.exists(_.claim.id == "s21-independent"))
    assertSuppressed(outline, "s07-duplicate", SuppressionReason.DuplicateWeakerClaim)

  test("lead bucket priority beats higher impact shouldLead projection"):
    val certifiedConversion = selectionClaim(
      id = "certified-conversion-must-lead",
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = Some("white"),
      anchor = Some("board"),
      route = Some("certified_conversion"),
      impact = ClaimImpact(resultMaterialImpact = 80, forcedness = 80, immediacy = 70, evalSwing = 180),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Certification, "MaterialConversion", Some("white"), Some("board"), Some("certified_conversion"), Some("position_local")),
        EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-conversion", Some("white"), Some("board"), Some("certified_conversion"), Some("position_local")),
        EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("white"), Some("board"), Some("certified_conversion"), Some("position_local"))
      ),
      wordingStrengthCap = WordingStrength.AssertiveCertified
    )
    val strategicProjection = admittedProjection("s07-higher-score-should-lead", "S07", route = "development_led_window", score = 100)
      .copy(impact = ClaimImpact(resultMaterialImpact = 100, forcedness = 100, immediacy = 100, persistenceAfterDefense = 100, evidenceConfidence = 100))

    val outline = ClaimSelector.select(Vector(strategicProjection, certifiedConversion))

    assertEquals(outline.lead.map(_.claim.id), Some("certified-conversion-must-lead"))
    assertEquals(outline.lead.map(_.bucket), Some(ClaimBucket.MustLead))
    assert(outline.support.exists(_.claim.id == "s07-higher-score-should-lead"))

  test("S07 S08 S21 cluster selects exact initiative lead and suppresses adjacent false leads"):
    val initiativeLead = admittedProjection("s07-development-window-lead", "S07", route = "development_led_window", score = 84)
    val counterplaySupport = admittedProjection("s21-far-wing-counterplay-support", "S21", anchor = "piece:h2", route = "far_wing_source_survives", score = 72)
    val adjacentReleaseRival = admittedProjection("s08-adjacent-release-rival", "S08", route = "rival_break_source_suppressed", score = 70)
    val supportOnlyWindow = selectionClaim(
      id = "initiative-window-support-only",
      layer = ClaimLayer.Certification,
      status = ClaimStatus.SupportOnly,
      owner = Some("white"),
      anchor = Some("board"),
      route = Some("development_led_window"),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "InitiativeWindow", Some("white"), Some("board"), Some("development_led_window")))
    )
    val rawEngineShortcut = selectionClaim(
      id = "initiative-raw-engine-shortcut",
      layer = ClaimLayer.Engine,
      status = ClaimStatus.Admitted,
      impact = ClaimImpact(evalSwing = 440),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-engine-plus-440"))
    )
    val openingContext = sourceClaim("sicilian-release-context", SourceContextKind.Opening)

    val outline = ClaimSelector.select(
      Vector(
        rawEngineShortcut,
        openingContext,
        adjacentReleaseRival,
        supportOnlyWindow,
        counterplaySupport,
        initiativeLead
      ),
      rendererRequestedCap = Some(WordingStrength.AssertiveCertified)
    )

    assertEquals(outline.lead.map(_.claim.id), Some("s07-development-window-lead"))
    assertEquals(outline.lead.map(_.bucket), Some(ClaimBucket.ShouldLead))
    assert(outline.support.exists(_.claim.id == "s21-far-wing-counterplay-support"))
    assert(outline.context.exists(_.claim.id == "sicilian-release-context"))
    assertSuppressed(outline, "s08-adjacent-release-rival", SuppressionReason.RivalBand)
    assertSuppressed(outline, "initiative-window-support-only", SuppressionReason.SupportOnly)
    assertSuppressed(outline, "initiative-raw-engine-shortcut", SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, "initiative-raw-engine-shortcut", SuppressionReason.NoBoardReason)
    assertSelectedContextReason(outline, "sicilian-release-context", SuppressionReason.SourceContextOnly)
    assertNotSuppressed(outline, "sicilian-release-context")
    assertEquals(outline.wordingStrengthCap, WordingStrength.QualifiedSupport)
    assertSuppressed(outline, "renderer-wording-upgrade", SuppressionReason.RendererNotAllowed)

  test("S07 S08 S21 cluster fails closed on wrong binding"):
    val wrongBinding = selectionClaim(
      id = "s21-wrong-binding-counterplay",
      layer = ClaimLayer.Projection,
      status = ClaimStatus.Admitted,
      band = Some("S21"),
      owner = Some("white"),
      anchor = Some("piece:h2"),
      route = Some("far_wing_source_survives"),
      scope = Some("position_local"),
      evidenceRefs = Vector(
        EvidenceRef(
          EvidenceRefKind.Projection,
          "counterplay_survival_route_certified",
          owner = Some("black"),
          anchor = Some("piece:a7"),
          route = Some("center_source_survives"),
          scope = Some("move_local")
        )
      ),
      lowerCarrierRefs = Vector(
        EvidenceRef(
          EvidenceRefKind.Certification,
          "InitiativeWindow",
          owner = Some("black"),
          anchor = Some("piece:a7"),
          route = Some("center_source_survives"),
          scope = Some("move_local")
        )
      )
    )

    val outline = ClaimSelector.select(Vector(wrongBinding))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s21-wrong-binding-counterplay", SuppressionReason.WrongOwner)
    assertSuppressed(outline, "s21-wrong-binding-counterplay", SuppressionReason.WrongAnchor)
    assertSuppressed(outline, "s21-wrong-binding-counterplay", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "s21-wrong-binding-counterplay", SuppressionReason.ScopeMismatch)

  test("S15 selected lead requires same-candidate creation carrier and keeps S13 S14 as support"):
    val s15 = admittedProjection("s15-same-candidate-creation", "S15", anchor = "piece:a4", route = "s13_wing_damage", score = 86)
    val s13Support = admittedProjection("s13-same-candidate-support", "S13", anchor = "piece:a4", route = "phalanx_edge_target", score = 62)
    val s14Support = admittedProjection("s14-same-candidate-support", "S14", anchor = "piece:a4", route = "chain_base_target", score = 58)
    val source = sourceClaim("queenside-minority-context", SourceContextKind.Opening)

    val outline = ClaimSelector.select(Vector(source, s13Support, s14Support, s15))

    assertEquals(outline.lead.map(_.claim.id), Some("s15-same-candidate-creation"))
    assertEquals(outline.lead.map(_.bucket), Some(ClaimBucket.ShouldLead))
    assert(outline.support.exists(_.claim.id == "s13-same-candidate-support"))
    assert(outline.support.exists(_.claim.id == "s14-same-candidate-support"))
    assert(outline.context.exists(_.claim.id == "queenside-minority-context"))
    assertSelectedContextReason(outline, "queenside-minority-context", SuppressionReason.SourceContextOnly)
    assertNotSuppressed(outline, "queenside-minority-context")

  test("S16 selected lead requires same-enemy-passer suppression carrier"):
    val s16 = admittedProjection("s16-same-enemy-passer-suppression", "S16", anchor = "piece:g5", route = "blockade_hold", score = 88)
    val rawEngine = selectionClaim(
      id = "passer-raw-engine-shortcut",
      layer = ClaimLayer.Engine,
      status = ClaimStatus.Admitted,
      impact = ClaimImpact(evalSwing = 520),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-passer-eval"))
    )
    val retrieval = sourceClaim("retrieved-passer-example", SourceContextKind.Retrieval).copy(status = ClaimStatus.Admitted)

    val outline = ClaimSelector.select(Vector(rawEngine, retrieval, s16))

    assertEquals(outline.lead.map(_.claim.id), Some("s16-same-enemy-passer-suppression"))
    assertEquals(outline.lead.map(_.bucket), Some(ClaimBucket.ShouldLead))
    assertSuppressed(outline, "passer-raw-engine-shortcut", SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, "passer-raw-engine-shortcut", SuppressionReason.NoBoardReason)
    assertSelectedContextReason(outline, "retrieved-passer-example", SuppressionReason.SourceContextOnly)
    assertSelectedContextReason(outline, "retrieved-passer-example", SuppressionReason.RetrievalNonAuthoritative)
    assertNotSuppressed(outline, "retrieved-passer-example")

  test("S13 S14 cannot support S15 when same-candidate owner or anchor is broken"):
    val s15 = admittedProjection("s15-supported-creation", "S15", anchor = "piece:a4", route = "s14_chain_base", score = 82)
    val wrongOwnerS13 = admittedProjection("s13-wrong-owner-support", "S13", anchor = "piece:a4", route = "phalanx_edge_target", score = 60)
      .copy(
        owner = Some("black"),
        beneficiary = Some("black"),
        defender = Some("white"),
        sideToMove = Some("black"),
        evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Projection, "wing_damage_route_certified", Some("black"), Some("piece:a4"), Some("phalanx_edge_target"), Some("position_local"))),
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Witness, "sector_asymmetry_state", Some("black"), Some("piece:a4"), Some("phalanx_edge_target"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Witness, "available_lever_trigger", Some("black"), Some("piece:a4"), Some("phalanx_edge_target"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source", Some("black"), Some("piece:a4"), Some("phalanx_edge_target"), Some("position_local"))
        )
      )
    val wrongAnchorS14 = admittedProjection("s14-wrong-candidate-support", "S14", anchor = "piece:b4", route = "chain_base_target", score = 60)

    val outline = ClaimSelector.select(Vector(wrongOwnerS13, wrongAnchorS14, s15))

    assertEquals(outline.lead.map(_.claim.id), Some("s15-supported-creation"))
    assert(!outline.support.exists(_.claim.id == "s13-wrong-owner-support"))
    assert(!outline.support.exists(_.claim.id == "s14-wrong-candidate-support"))
    assertSuppressed(outline, "s13-wrong-owner-support", SuppressionReason.WrongOwner)
    assertSuppressed(outline, "s14-wrong-candidate-support", SuppressionReason.WrongAnchor)

  test("S13 S14 support cannot outrank same-candidate S15 lead"):
    val s15 = admittedProjection("s15-owner-truth", "S15", anchor = "piece:a4", route = "s13_wing_damage", score = 60)
    val highImpactS13 = admittedProjection("s13-high-impact-support", "S13", anchor = "piece:a4", route = "phalanx_edge_target", score = 99)
    val highImpactS14 = admittedProjection("s14-high-impact-support", "S14", anchor = "piece:a4", route = "chain_base_target", score = 98)

    val outline = ClaimSelector.select(Vector(highImpactS13, highImpactS14, s15))

    assertEquals(outline.lead.map(_.claim.id), Some("s15-owner-truth"))
    assert(outline.support.exists(_.claim.id == "s13-high-impact-support"))
    assert(outline.support.exists(_.claim.id == "s14-high-impact-support"))

  test("S15 rejects existing passer and split-anchor creation shortcuts"):
    val existingPasserOnly = admittedProjection("s15-existing-passer-only", "S15", anchor = "piece:d4", route = "s13_wing_damage", score = 76)
      .copy(lowerCarrierRefs = Vector(EvidenceRef(EvidenceRefKind.Root, "passed_pawn_entity_state", Some("white"), Some("piece:d4"), Some("s13_wing_damage"), Some("position_local"))))
    val splitAnchor = admittedProjection("s15-split-anchor-route", "S15", anchor = "piece:e4", route = "s14_chain_base", score = 72)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Root, "candidate_passer", Some("white"), Some("piece:e4"), Some("s14_chain_base"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Witness, "same_candidate_s14_creation_route", Some("white"), Some("piece:a4"), Some("s14_chain_base"), Some("position_local"))
        )
      )

    val outline = ClaimSelector.select(Vector(existingPasserOnly, splitAnchor))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s15-existing-passer-only", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s15-existing-passer-only", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "s15-split-anchor-route", SuppressionReason.WrongAnchor)

  test("S16 rejects blocker shell without same-enemy-passer carrier"):
    val blockerOnly = admittedProjection("s16-blocker-shell-only", "S16", anchor = "piece:g5", route = "blockade_hold", score = 78)
      .copy(lowerCarrierRefs = Vector(EvidenceRef(EvidenceRefKind.Object, "blocker_shell", Some("white"), Some("piece:g5"), Some("blockade_hold"), Some("position_local"))))

    val outline = ClaimSelector.select(Vector(blockerOnly))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s16-blocker-shell-only", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s16-blocker-shell-only", SuppressionReason.NoBoardReason)

  test("S15 rejects forged candidate carrier kind and unsupported creation route"):
    val forgedCandidateKind = admittedProjection("s15-forged-candidate-kind", "S15", anchor = "piece:a4", route = "s13_wing_damage", score = 81)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Certification, "candidate_passer", Some("white"), Some("piece:a4"), Some("s13_wing_damage"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Witness, "same_candidate_s13_wing_damage_creation_route", Some("white"), Some("piece:a4"), Some("s13_wing_damage"), Some("position_local"))
        )
      )
    val bogusRoute = admittedProjection("s15-bogus-creation-route", "S15", anchor = "piece:a4", route = "generic_passer_play", score = 80)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Root, "candidate_passer", Some("white"), Some("piece:a4"), Some("generic_passer_play"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Witness, "same_candidate_generic_passer_play_creation_route", Some("white"), Some("piece:a4"), Some("generic_passer_play"), Some("position_local"))
        )
      )

    val outline = ClaimSelector.select(Vector(forgedCandidateKind, bogusRoute))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s15-forged-candidate-kind", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s15-forged-candidate-kind", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "s15-bogus-creation-route", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "s15-bogus-creation-route", SuppressionReason.ForbiddenShortcut)

  test("S16 rejects forged enemy passer carrier kind and unsupported suppression route"):
    val forgedEnemyPasserKind = admittedProjection("s16-forged-passer-kind", "S16", anchor = "piece:g5", route = "blockade_hold", score = 81)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Object, "passed_pawn_entity_state", Some("black"), Some("piece:g5"), Some("blockade_hold"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Certification, "FortressDrawCertification", Some("white"), Some("piece:g5"), Some("blockade_hold"), Some("position_local"))
        )
      )
    val bogusRoute = admittedProjection("s16-bogus-suppression-route", "S16", anchor = "piece:g5", route = "generic_hold", score = 80)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Witness, "passed_pawn_entity_state", Some("black"), Some("piece:g5"), Some("generic_hold"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Certification, "FortressDrawCertification", Some("white"), Some("piece:g5"), Some("generic_hold"), Some("position_local"))
        )
      )

    val outline = ClaimSelector.select(Vector(forgedEnemyPasserKind, bogusRoute))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s16-forged-passer-kind", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s16-forged-passer-kind", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "s16-bogus-suppression-route", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "s16-bogus-suppression-route", SuppressionReason.ForbiddenShortcut)

  test("S16 rejects same-side owner defender ambiguity"):
    val selfOwnedSuppression = admittedProjection("s16-self-owned-passer-suppression", "S16", anchor = "piece:g5", route = "blockade_hold", score = 86)
      .copy(
        defender = Some("white"),
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Witness, "passed_pawn_entity_state", Some("white"), Some("piece:g5"), Some("blockade_hold"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Certification, "FortressDrawCertification", Some("white"), Some("piece:g5"), Some("blockade_hold"), Some("position_local"))
        )
      )

    val outline = ClaimSelector.select(Vector(selfOwnedSuppression))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s16-self-owned-passer-suppression", SuppressionReason.WrongOwner)

  test("S15 S16 same-bucket conflict requires beneficiary defender and side clarity"):
    val s15 = admittedProjection("s15-clear-creation", "S15", anchor = "piece:a4", route = "s13_wing_damage", score = 70)
    val ambiguousS16 = admittedProjection("s16-ambiguous-suppression", "S16", anchor = "piece:g5", route = "blockade_hold", score = 96)
      .copy(beneficiary = None, defender = None, sideToMove = None)
    val rivalS16 = admittedProjection("s16-clear-rival", "S16", anchor = "piece:g5", route = "blockade_hold", score = 68)

    val outline = ClaimSelector.select(Vector(rivalS16, ambiguousS16, s15))

    assertEquals(outline.lead.map(_.claim.id), Some("s15-clear-creation"))
    assertSuppressed(outline, "s16-ambiguous-suppression", SuppressionReason.WrongOwner)
    assertSuppressed(outline, "s16-ambiguous-suppression", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "s16-ambiguous-suppression", SuppressionReason.ScopeMismatch)
    assertSuppressed(outline, "s16-clear-rival", SuppressionReason.RivalBand)

  test("S15 S16 rival suppression is selector-derived and requires coherent bindings"):
    val s15 = admittedProjection("s15-selector-owned-rival", "S15", anchor = "piece:a4", route = "s13_wing_damage", score = 70)
    val hintedS16 = admittedProjection("s16-hinted-without-rival", "S16", anchor = "piece:g5", route = "blockade_hold", score = 76)
      .copy(suppressionHints = Vector(SuppressionReason.RivalBand))
    val mismatchedS16 = admittedProjection("s16-mismatched-defender", "S16", anchor = "piece:g5", route = "blockade_hold", score = 68)
      .copy(defender = Some("white"))

    val hintedOutline = ClaimSelector.select(Vector(hintedS16))
    assertEquals(hintedOutline.lead.map(_.claim.id), Some("s16-hinted-without-rival"))
    assert(!hintedOutline.suppressedClaims.exists(_.claim.id == "s16-hinted-without-rival"))

    val conflictOutline = ClaimSelector.select(Vector(s15, mismatchedS16))
    assertEquals(conflictOutline.lead.map(_.claim.id), Some("s15-selector-owned-rival"))
    assertSuppressed(conflictOutline, "s16-mismatched-defender", SuppressionReason.WrongOwner)

  test("renderer cannot upgrade passer outline wording strength"):
    val outline = ClaimSelector.select(
      Vector(admittedProjection("s15-qualified-creation", "S15", anchor = "piece:a4", route = "s13_wing_damage", score = 76)),
      rendererRequestedCap = Some(WordingStrength.AssertiveCertified)
    )

    assertEquals(outline.wordingStrengthCap, WordingStrength.QualifiedSupport)
    assertSuppressed(outline, "renderer-wording-upgrade", SuppressionReason.RendererNotAllowed)

  test("S17 S18 S19 S22 cluster selects exact admitted leads only"):
    val s17 = admittedProjection("s17-liability-relief-lead", "S17", anchor = "piece:c1", route = "repair_route", score = 82)
    val s18 = admittedProjection("s18-bishop-pair-conversion-lead", "S18", anchor = "piece:g2", route = "bishop_pair_to_material", score = 84)
    val s19 = admittedProjection("s19-material-simplification-lead", "S19", anchor = "move:exd5", route = "trade_invariant_to_material", score = 86)
    val s22 = admittedProjection("s22-fortress-hold-lead", "S22", anchor = "square:g8", route = "fortress_draw_hold", score = 88)

    assertEquals(ClaimSelector.select(Vector(s17)).lead.map(_.claim.id), Some("s17-liability-relief-lead"))
    assertEquals(ClaimSelector.select(Vector(s18)).lead.map(_.claim.id), Some("s18-bishop-pair-conversion-lead"))
    assertEquals(ClaimSelector.select(Vector(s19)).lead.map(_.claim.id), Some("s19-material-simplification-lead"))
    assertEquals(ClaimSelector.select(Vector(s22)).lead.map(_.claim.id), Some("s22-fortress-hold-lead"))

  test("S17 rejects generic relief wording and outranks same-piece S19 simplification"):
    val s17 = admittedProjection("s17-same-piece-relief-owner", "S17", anchor = "piece:c1", route = "exchange_relief", score = 70)
    val highScoreS19 = admittedProjection("s19-relief-stealing-simplification", "S19", anchor = "piece:c1", route = "trade_invariant_to_material", score = 99)
    val genericRelief = admittedProjection("s17-generic-relief-wording", "S17", anchor = "piece:c1", route = "generic_relief", score = 90)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Witness, "generic_relief_wording", Some("white"), Some("piece:c1"), Some("generic_relief"), Some("position_local"))
        )
      )

    val outline = ClaimSelector.select(Vector(highScoreS19, genericRelief, s17))

    assertEquals(outline.lead.map(_.claim.id), Some("s17-same-piece-relief-owner"))
    assertSuppressed(outline, "s19-relief-stealing-simplification", SuppressionReason.RivalBand)
    assertSuppressed(outline, "s17-generic-relief-wording", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s17-generic-relief-wording", SuppressionReason.NoBoardReason)

  test("S18 rejects unbacked conversion and outranks same-anchor S19 simplification"):
    val s18 = admittedProjection("s18-bishop-pair-owner", "S18", anchor = "piece:g2", route = "bishop_pair_to_structure", score = 72)
    val highScoreS19 = admittedProjection("s19-bishop-pair-stealing-simplification", "S19", anchor = "piece:g2", route = "trade_invariant_to_material", score = 98)
    val unbackedConversion = admittedProjection("s18-unbacked-conversion", "S18", anchor = "piece:g2", route = "bishop_pair_to_material", score = 90)
      .copy(
        lowerCarrierRefs = Vector(EvidenceRef(EvidenceRefKind.Witness, "bishop_pair_state", Some("white"), Some("piece:g2"), Some("bishop_pair_to_material"), Some("position_local")))
      )

    val outline = ClaimSelector.select(Vector(highScoreS19, unbackedConversion, s18))

    assertEquals(outline.lead.map(_.claim.id), Some("s18-bishop-pair-owner"))
    assertSuppressed(outline, "s19-bishop-pair-stealing-simplification", SuppressionReason.RivalBand)
    assertSuppressed(outline, "s18-unbacked-conversion", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s18-unbacked-conversion", SuppressionReason.NoBoardReason)

  test("S22 hold suppresses S19 simplification and unbacked conversion on contradiction"):
    val s22 = admittedProjection("s22-certified-hold-owner", "S22", anchor = "square:g8", route = "perpetual_hold", score = 74)
    val s19 = admittedProjection("s19-hold-stealing-simplification", "S19", anchor = "square:g8", route = "trade_invariant_to_hold", score = 97)
    val unbackedConversion = admittedProjection("s18-hold-contradicted-conversion", "S18", anchor = "square:g8", route = "bishop_pair_to_material", score = 95)
      .copy(
        impact = ClaimImpact(resultMaterialImpact = 30, persistenceAfterDefense = 95, evidenceConfidence = 95),
        lowerCarrierRefs = Vector(EvidenceRef(EvidenceRefKind.Witness, "bishop_pair_state", Some("white"), Some("square:g8"), Some("bishop_pair_to_material"), Some("position_local")))
      )

    val outline = ClaimSelector.select(Vector(unbackedConversion, s19, s22))

    assertEquals(outline.lead.map(_.claim.id), Some("s22-certified-hold-owner"))
    assertSuppressed(outline, "s19-hold-stealing-simplification", SuppressionReason.RivalBand)
    assertSuppressed(outline, "s18-hold-contradicted-conversion", SuppressionReason.ForbiddenShortcut)

  test("S22 hold beats fully backed but under-qualified S18 conversion"):
    val s22 = admittedProjection("s22-same-anchor-certified-hold", "S22", anchor = "square:g8", route = "fortress_draw_hold", score = 76)
    val underQualifiedConversion = admittedProjection("s18-underqualified-backed-conversion", "S18", anchor = "square:g8", route = "bishop_pair_to_material", score = 99)
      .copy(impact = ClaimImpact(resultMaterialImpact = 40, persistenceAfterDefense = 99, evidenceConfidence = 99))

    val outline = ClaimSelector.select(Vector(underQualifiedConversion, s22))

    assertEquals(outline.lead.map(_.claim.id), Some("s22-same-anchor-certified-hold"))
    assertSuppressed(outline, "s18-underqualified-backed-conversion", SuppressionReason.RivalBand)

  test("S18 can beat S22 hold only through material conversion evidence"):
    val s22 = admittedProjection("s22-hold-against-extra-material", "S22", anchor = "square:g8", route = "fortress_draw_hold", score = 76)
    val initiativeWithExtraMaterial = admittedProjection("s18-initiative-with-extra-material", "S18", anchor = "square:g8", route = "bishop_pair_to_initiative", score = 99)
      .copy(
        impact = ClaimImpact(resultMaterialImpact = 95, persistenceAfterDefense = 99, evidenceConfidence = 99),
        lowerCarrierRefs = admittedProjection("unused", "S18", anchor = "square:g8", route = "bishop_pair_to_initiative", score = 99).lowerCarrierRefs :+
          EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("square:g8"), Some("bishop_pair_to_initiative"), Some("position_local"))
      )
    val materialConversion = admittedProjection("s18-material-conversion-over-hold", "S18", anchor = "square:g8", route = "bishop_pair_to_material", score = 80)
      .copy(impact = ClaimImpact(resultMaterialImpact = 90, persistenceAfterDefense = 80, evidenceConfidence = 80))

    val blocked = ClaimSelector.select(Vector(initiativeWithExtraMaterial, s22))
    assertEquals(blocked.lead.map(_.claim.id), Some("s22-hold-against-extra-material"))
    assertSuppressed(blocked, "s18-initiative-with-extra-material", SuppressionReason.RivalBand)

    val justified = ClaimSelector.select(Vector(materialConversion, s22))
    assertEquals(justified.lead.map(_.claim.id), Some("s18-material-conversion-over-hold"))
    assertSuppressed(justified, "s22-hold-against-extra-material", SuppressionReason.RivalBand)

  test("conversion simplification hold cluster rejects raw source and wrong binding"):
    val wrongBinding = admittedProjection("s19-wrong-binding-simplification", "S19", anchor = "move:exd5", route = "trade_invariant_to_material", score = 80)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("black"), Some("move:cxd5"), Some("trade_invariant_to_hold"), Some("move_local")),
          EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("black"), Some("move:cxd5"), Some("trade_invariant_to_hold"), Some("move_local"))
        )
      )
    val rawEngine = selectionClaim(
      id = "conversion-raw-engine-shortcut",
      layer = ClaimLayer.Engine,
      status = ClaimStatus.Admitted,
      impact = ClaimImpact(evalSwing = 700),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-conversion-eval"))
    )
    val source = sourceClaim("conversion-source-context", SourceContextKind.Retrieval).copy(status = ClaimStatus.Admitted)

    val outline = ClaimSelector.select(Vector(source, rawEngine, wrongBinding))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s19-wrong-binding-simplification", SuppressionReason.WrongOwner)
    assertSuppressed(outline, "s19-wrong-binding-simplification", SuppressionReason.WrongAnchor)
    assertSuppressed(outline, "s19-wrong-binding-simplification", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "s19-wrong-binding-simplification", SuppressionReason.ScopeMismatch)
    assertSuppressed(outline, "conversion-raw-engine-shortcut", SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, "conversion-raw-engine-shortcut", SuppressionReason.NoBoardReason)
    assertSelectedContextReason(outline, "conversion-source-context", SuppressionReason.SourceContextOnly)
    assertSelectedContextReason(outline, "conversion-source-context", SuppressionReason.RetrievalNonAuthoritative)
    assertNotSuppressed(outline, "conversion-source-context")

  test("conversion simplification hold cluster rejects unbound projection evidence"):
    val unboundEvidence = admittedProjection("s17-unbound-projection-evidence", "S17", anchor = "piece:c1", route = "repair_route", score = 82)
      .copy(evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Projection, "liability_relief_certified", Some("white"), Some("piece:c1"), Some("repair_route"))))
    val wrongRouteEvidence = admittedProjection("s18-wrong-route-projection-evidence", "S18", anchor = "piece:g2", route = "bishop_pair_to_material", score = 84)
      .copy(evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Projection, "bishop_pair_material_conversion_certified", Some("white"), Some("piece:g2"), Some("bishop_pair_to_structure"), Some("position_local"))))
    val exact = admittedProjection("s22-bound-projection-evidence", "S22", anchor = "square:g8", route = "perpetual_hold", score = 74)

    val outline = ClaimSelector.select(Vector(unboundEvidence, wrongRouteEvidence, exact))

    assertEquals(outline.lead.map(_.claim.id), Some("s22-bound-projection-evidence"))
    assertSuppressed(outline, "s17-unbound-projection-evidence", SuppressionReason.ScopeMismatch)
    assertSuppressed(outline, "s17-unbound-projection-evidence", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s18-wrong-route-projection-evidence", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "s18-wrong-route-projection-evidence", SuppressionReason.ForbiddenShortcut)

  test("renderer cannot upgrade conversion simplification hold wording strength"):
    val outline = ClaimSelector.select(
      Vector(admittedProjection("s22-qualified-hold", "S22", anchor = "square:g8", route = "fortress_draw_hold", score = 76)),
      rendererRequestedCap = Some(WordingStrength.AssertiveCertified)
    )

    assertEquals(outline.wordingStrengthCap, WordingStrength.QualifiedSupport)
    assertSuppressed(outline, "renderer-wording-upgrade", SuppressionReason.RendererNotAllowed)

  test("S01 S02 S03 S04 king attack cluster selects exact admitted leads only"):
    val s01 = admittedProjection("s01-king-wing-storm-lead", "S01", anchor = "piece:h2", route = "same_wing_contact", score = 82)
    val s02 = admittedProjection("s02-king-ring-concentration-lead", "S02", anchor = "king:g8", route = "direct_piece_concentration", score = 84)
    val s03 = admittedProjection("s03-diagonal-king-attack-lead", "S03", anchor = "king:g8", route = "king_facing_diagonal_entry", score = 86)
    val s04 = admittedProjection("s04-shelter-breach-lead", "S04", anchor = "king:g8", route = "shell_payload_breach", score = 88)

    Vector(s01, s02, s03, s04).foreach: claim =>
      val outline = ClaimSelector.select(Vector(claim))
      assertEquals(outline.lead.map(_.claim.id), Some(claim.id))
      assertEquals(outline.lead.map(_.bucket), Some(ClaimBucket.ShouldLead))

  test("same king S01 S02 S03 S04 claims remain nonredundant by route and anchor"):
    val storm = admittedProjection("s01-same-king-storm", "S01", anchor = "piece:h2", route = "same_wing_contact", score = 82)
    val concentration = admittedProjection("s02-same-king-concentration", "S02", anchor = "king:g8", route = "direct_piece_concentration", score = 78)
    val diagonal = admittedProjection("s03-same-king-diagonal", "S03", anchor = "king:g8", route = "king_facing_diagonal_entry", score = 76)
    val shelter = admittedProjection("s04-same-king-shelter", "S04", anchor = "king:g8", route = "shell_payload_breach", score = 74)

    val outline = ClaimSelector.select(Vector(shelter, diagonal, concentration, storm))

    assertEquals(outline.lead.map(_.claim.id), Some("s01-same-king-storm"))
    assert(outline.support.exists(_.claim.id == "s02-same-king-concentration"))
    assert(outline.support.exists(_.claim.id == "s03-same-king-diagonal"))
    assert(outline.support.exists(_.claim.id == "s04-same-king-shelter"))
    assert(!outline.suppressedClaims.exists(_.reasons.contains(SuppressionReason.DuplicateWeakerClaim)))

  test("king attack cluster rejects wrong owner king route and generic source shortcuts"):
    val wrongBinding = admittedProjection("s03-wrong-king-diagonal", "S03", anchor = "king:g8", route = "king_facing_diagonal_entry", score = 80)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Witness, "diagonal_lane_only", Some("black"), Some("king:h8"), Some("wrong_diagonal"), Some("move_local")),
          EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("black"), Some("king:h8"), Some("wrong_diagonal"), Some("move_local")),
          EvidenceRef(EvidenceRefKind.Certification, "ComparativeKingFragility", Some("black"), Some("king:h8"), Some("wrong_diagonal"), Some("move_local"))
        )
      )
    val genericAttack = admittedProjection("s01-generic-attack-wording", "S01", anchor = "king:g8", route = "generic_attack", score = 90)
    val motifContext = sourceClaim("motif-greek-gift-context", SourceContextKind.Motif).copy(status = ClaimStatus.Admitted)
    val retrieval = sourceClaim("retrieved-attack-example", SourceContextKind.Retrieval).copy(status = ClaimStatus.Admitted)

    val outline = ClaimSelector.select(Vector(wrongBinding, genericAttack, motifContext, retrieval))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "s03-wrong-king-diagonal", SuppressionReason.WrongOwner)
    assertSuppressed(outline, "s03-wrong-king-diagonal", SuppressionReason.WrongAnchor)
    assertSuppressed(outline, "s03-wrong-king-diagonal", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "s03-wrong-king-diagonal", SuppressionReason.ScopeMismatch)
    assertSuppressed(outline, "s01-generic-attack-wording", SuppressionReason.WrongRoute)
    assertSuppressed(outline, "s01-generic-attack-wording", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s01-generic-attack-wording", SuppressionReason.NoBoardReason)
    assertSelectedContextReason(outline, "motif-greek-gift-context", SuppressionReason.SourceContextOnly)
    assertSelectedContextReason(outline, "retrieved-attack-example", SuppressionReason.SourceContextOnly)
    assertSelectedContextReason(outline, "retrieved-attack-example", SuppressionReason.RetrievalNonAuthoritative)
    assertNotSuppressed(outline, "motif-greek-gift-context")
    assertNotSuppressed(outline, "retrieved-attack-example")

  test("king attack carrier ownership mirrors runtime S03 and S04 admission"):
    val s04DefenderOwnedShell = admittedProjection("s04-defender-owned-shell-lead", "S04", anchor = "king:g8", route = "shell_payload_breach", score = 82)
    val s03MissingFragility = admittedProjection("s03-missing-fragility-cert", "S03", anchor = "king:g8", route = "king_facing_diagonal_entry", score = 90)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Witness, "diagonal_lane_only", Some("white"), Some("king:g8"), Some("king_facing_diagonal_entry"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("king:g8"), Some("king_facing_diagonal_entry"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Certification, "CertifiedKingSafetyEdge", Some("white"), Some("king:g8"), Some("king_facing_diagonal_entry"), Some("position_local"))
        )
      )
    val s03MissingSafety = admittedProjection("s03-missing-safety-cert", "S03", anchor = "king:g8", route = "fragility_linked_diagonal", score = 88)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Witness, "diagonal_lane_only", Some("white"), Some("king:g8"), Some("fragility_linked_diagonal"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("king:g8"), Some("fragility_linked_diagonal"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Certification, "ComparativeKingFragility", Some("white"), Some("king:g8"), Some("fragility_linked_diagonal"), Some("position_local"))
        )
      )
    val s04MissingSupportBreak = admittedProjection("s04-missing-support-break-lane", "S04", anchor = "king:g8", route = "support_break_breach", score = 86)
      .copy(
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.Object, "KingSafetyShell", Some("black"), Some("king:g8"), Some("support_break_breach"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.Certification, "CertifiedKingSafetyEdge", Some("white"), Some("king:g8"), Some("support_break_breach"), Some("position_local"))
        )
      )

    val outline = ClaimSelector.select(Vector(s03MissingFragility, s03MissingSafety, s04MissingSupportBreak, s04DefenderOwnedShell))

    assertEquals(outline.lead.map(_.claim.id), Some("s04-defender-owned-shell-lead"))
    assertSuppressed(outline, "s03-missing-fragility-cert", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s03-missing-fragility-cert", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "s03-missing-safety-cert", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s03-missing-safety-cert", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "s04-missing-support-break-lane", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "s04-missing-support-break-lane", SuppressionReason.NoBoardReason)

  test("king attack cluster rejects raw engine false lead and support deferred shortcuts"):
    val supportOnlyAttack = admittedProjection("s02-support-only-concentration", "S02", anchor = "king:g8", route = "direct_piece_concentration", score = 70)
      .copy(status = ClaimStatus.SupportOnly)
    val deferredAttack = admittedProjection("s04-deferred-shelter-breach", "S04", anchor = "king:g8", route = "shell_payload_breach", score = 72)
      .copy(status = ClaimStatus.Deferred)
    val rawEngine = selectionClaim(
      id = "king-attack-raw-engine",
      layer = ClaimLayer.Engine,
      status = ClaimStatus.Admitted,
      impact = ClaimImpact(evalSwing = 900),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-attack-eval"))
    )

    val outline = ClaimSelector.select(Vector(rawEngine, supportOnlyAttack, deferredAttack))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, "king-attack-raw-engine", SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, "king-attack-raw-engine", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "s02-support-only-concentration", SuppressionReason.SupportOnly)
    assertSuppressed(outline, "s04-deferred-shelter-breach", SuppressionReason.Deferred)

  test("certified result owner outranks king attack projection but raw engine does not"):
    val attackProjection = admittedProjection("s04-qualified-shelter-breach", "S04", anchor = "king:g8", route = "support_break_breach", score = 100)
    val certifiedResult = selectionClaim(
      id = "certified-mate-net-owner",
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = Some("white"),
      anchor = Some("king:g8"),
      route = Some("mate_net"),
      impact = ClaimImpact(resultMaterialImpact = 100, forcedness = 95, immediacy = 90, evidenceConfidence = 95),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "MateNetCertification", Some("white"), Some("king:g8"), Some("mate_net"), Some("position_local"))),
      wordingStrengthCap = WordingStrength.AssertiveCertified
    )
    val rawEngine = selectionClaim(
      id = "king-attack-raw-eval-shortcut",
      layer = ClaimLayer.Engine,
      status = ClaimStatus.Admitted,
      impact = ClaimImpact(evalSwing = 1000),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-attack-eval"))
    )

    val outline = ClaimSelector.select(Vector(rawEngine, attackProjection, certifiedResult))

    assertEquals(outline.lead.map(_.claim.id), Some("certified-mate-net-owner"))
    assertEquals(outline.lead.map(_.bucket), Some(ClaimBucket.MustLead))
    assert(outline.support.exists(_.claim.id == "s04-qualified-shelter-breach"))
    assertSuppressed(outline, "king-attack-raw-eval-shortcut", SuppressionReason.RawEngineOnly)

  test("renderer cannot upgrade king attack wording strength"):
    val outline = ClaimSelector.select(
      Vector(admittedProjection("s03-qualified-diagonal-attack", "S03", anchor = "king:g8", route = "fragility_linked_diagonal", score = 76)),
      rendererRequestedCap = Some(WordingStrength.AssertiveCertified)
    )

    assertEquals(outline.wordingStrengthCap, WordingStrength.QualifiedSupport)
    assertSuppressed(outline, "renderer-wording-upgrade", SuppressionReason.RendererNotAllowed)

  test("renderer cannot upgrade engine eval swing wording strength"):
    val outline = ClaimSelector.select(
      Vector(
        engineCertifiedClaim(
          id = "engine-certified-qualified-material",
          route = Some("material_harvest"),
          impact = ClaimImpact(resultMaterialImpact = 80, evalSwing = 500, evidenceConfidence = 90),
          certificationId = "MaterialHarvest",
          boardReasons = Vector(EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))),
          wordingStrengthCap = WordingStrength.QualifiedSupport
        )
      ),
      rendererRequestedCap = Some(WordingStrength.AssertiveCertified)
    )

    assertEquals(outline.lead.map(_.claim.id), Some("engine-certified-qualified-material"))
    assertEquals(outline.wordingStrengthCap, WordingStrength.QualifiedSupport)
    assertSuppressed(outline, "renderer-wording-upgrade", SuppressionReason.RendererNotAllowed)

  test("source context fallback admits only safe context when no board lead exists"):
    val opening = sourceClaim("opening-context-only", SourceContextKind.Opening)
    val endgameStudy = sourceClaim("lucena-context-only", SourceContextKind.EndgameStudy)
      .copy(
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.SourceContext, "endgame-study:lucena:applicable"),
          EvidenceRef(
            EvidenceRefKind.ExactBoard,
            "endgame-study-applicability:lucena",
            route = Some("lucena"),
            scope = Some("exact_endgame_applicability")
          )
        )
      )
    val retrieval = sourceClaim("retrieval-reference-only", SourceContextKind.Retrieval)
    val motif = sourceClaim("motif-example-context-only", SourceContextKind.Motif)
      .copy(
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.SourceContext, "motif-example:pin"),
          EvidenceRef(EvidenceRefKind.ExactBoard, "motif-detector-carrier:pin")
        )
      )

    val outline = ClaimSelector.select(Vector(opening, endgameStudy, retrieval, motif))

    assertEquals(outline.lead, None)
    assertEquals(outline.context.map(_.claim.id).toSet, Set("opening-context-only", "lucena-context-only", "retrieval-reference-only", "motif-example-context-only"))
    assert(outline.context.forall(_.bucket == ClaimBucket.ContextOnly))
    assertEquals(outline.wordingStrengthCap, WordingStrength.ContextOnly)
    assertSelectedContextReason(outline, "opening-context-only", SuppressionReason.SourceContextOnly)
    assertSelectedContextReason(outline, "lucena-context-only", SuppressionReason.SourceContextOnly)
    assertSelectedContextReason(outline, "retrieval-reference-only", SuppressionReason.RetrievalNonAuthoritative)
    assertSelectedContextReason(outline, "motif-example-context-only", SuppressionReason.SourceContextOnly)
    Vector("opening-context-only", "lucena-context-only", "retrieval-reference-only", "motif-example-context-only")
      .foreach(id => assertNotSuppressed(outline, id))

  test("exact board lead keeps source context bounded to support context"):
    val lead = admittedProjection("s07-board-lead-over-source", "S07", anchor = "board", route = "development_led_window", score = 80)
    val opening = sourceClaim("opening-context-after-lead", SourceContextKind.Opening)
    val retrieval = sourceClaim("retrieval-reference-after-lead", SourceContextKind.Retrieval)

    val outline = ClaimSelector.select(Vector(opening, retrieval, lead))

    assertEquals(outline.lead.map(_.claim.id), Some("s07-board-lead-over-source"))
    assertEquals(outline.context.map(_.bucket).toSet, Set(ClaimBucket.Support))
    assert(outline.context.exists(_.claim.id == "opening-context-after-lead"))
    assert(outline.context.exists(_.claim.id == "retrieval-reference-after-lead"))
    assertSelectedContextReason(outline, "opening-context-after-lead", SuppressionReason.SourceContextOnly)
    assertSelectedContextReason(outline, "retrieval-reference-after-lead", SuppressionReason.RetrievalNonAuthoritative)
    assertNotSuppressed(outline, "opening-context-after-lead")
    assertNotSuppressed(outline, "retrieval-reference-after-lead")

  test("unsafe source context families and shortcuts fail closed"):
    val ambiguousOpening = sourceClaim("opening-ambiguous-transposition", SourceContextKind.Opening)
      .copy(evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:std:ambiguous")))
    val openingTruthPromotion = sourceClaim("opening-best-move-truth-promotion", SourceContextKind.Opening)
      .copy(evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:std:canonical"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:std:best-move")
      ))
    val openingSpecificCitation = sourceClaim("opening-specific-game-citation", SourceContextKind.Opening)
      .copy(evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:std:canonical"),
        EvidenceRef(EvidenceRefKind.SourceContext, "game_id:abc123"),
        EvidenceRef(EvidenceRefKind.SourceContext, "gameUrl:https://example.invalid/game"),
        EvidenceRef(EvidenceRefKind.SourceContext, "player:example-player"),
        EvidenceRef(EvidenceRefKind.SourceContext, "event:example-event")
      ))
    val openingMergedRankings = sourceClaim("opening-merged-master-online-rankings", SourceContextKind.Opening)
      .copy(evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:std:canonical"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-source-use:master_reference+online_trend")
      ))
    val motifTagOnly = sourceClaim("motif-tag-without-detector", SourceContextKind.Motif)
      .copy(evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "motif-example:pin")))
    val spoofedMotifCarrier = sourceClaim("motif-spoofed-detector-carrier", SourceContextKind.Motif)
      .copy(
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.SourceContext, "motif-example:pin"),
          EvidenceRef(EvidenceRefKind.ExactBoard, "motif-detector-carrier:fork")
        )
      )
    val suffixedMotifCarrier = sourceClaim("motif-suffixed-unverified-carrier", SourceContextKind.Motif)
      .copy(
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.SourceContext, "motif-example:pin:unverified"),
          EvidenceRef(EvidenceRefKind.ExactBoard, "motif-detector-carrier:pin")
        )
      )
    val deferredMotifCarrier = sourceClaim("motif-deferred-helper-carrier", SourceContextKind.Motif)
      .copy(
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.SourceContext, "motif-example:back_rank_mate"),
          EvidenceRef(EvidenceRefKind.ExactBoard, "motif-detector-carrier:back_rank_mate")
        )
      )
    val endgameResultLanguage = sourceClaim("endgame-study-result-language", SourceContextKind.EndgameStudy)
      .copy(
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.SourceContext, "endgame-study:lucena:result-win"),
          EvidenceRef(EvidenceRefKind.ExactBoard, "endgame-study-applicability:lucena")
        )
      )
    val endgameMissingApplicableSuffix = sourceClaim("endgame-study-missing-applicable-suffix", SourceContextKind.EndgameStudy)
      .copy(
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.SourceContext, "endgame-study:lucena:example"),
          EvidenceRef(EvidenceRefKind.ExactBoard, "endgame-study-applicability:lucena")
        )
      )
    val retrievalTruthPromotion = sourceClaim("retrieval-current-position-truth", SourceContextKind.Retrieval)
      .copy(evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "retrieval-example:current-position-truth")))
    val gameContext = sourceClaim("game-context-deferred-family", SourceContextKind.Opening)
      .copy(sourceContextKind = None, route = Some("gameContext"))
    val practicality = sourceClaim("practicality-deferred-family", SourceContextKind.Opening)
      .copy(sourceContextKind = None, route = Some("practicality"))
    val tablebase = sourceClaim("tablebase-result-service-deferred-family", SourceContextKind.EndgameStudy)
      .copy(sourceContextKind = None, route = Some("tablebase"))
    val resultService = sourceClaim("endgame-result-service-deferred-family", SourceContextKind.EndgameStudy)
      .copy(sourceContextKind = None, route = Some("endgameResultService"))

    val outline = ClaimSelector.select(Vector(ambiguousOpening, openingTruthPromotion, openingSpecificCitation, openingMergedRankings, motifTagOnly, spoofedMotifCarrier, suffixedMotifCarrier, deferredMotifCarrier, endgameResultLanguage, endgameMissingApplicableSuffix, retrievalTruthPromotion, gameContext, practicality, tablebase, resultService))

    assertEquals(outline.lead, None)
    assertEquals(outline.context, Vector.empty)
    assertSuppressed(outline, "opening-ambiguous-transposition", SuppressionReason.AmbiguousTransposition)
    assertSuppressed(outline, "opening-best-move-truth-promotion", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "opening-best-move-truth-promotion", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "opening-specific-game-citation", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "opening-specific-game-citation", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "opening-merged-master-online-rankings", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "opening-merged-master-online-rankings", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "motif-tag-without-detector", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "motif-tag-without-detector", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "motif-spoofed-detector-carrier", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "motif-spoofed-detector-carrier", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "motif-suffixed-unverified-carrier", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "motif-suffixed-unverified-carrier", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "motif-deferred-helper-carrier", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "motif-deferred-helper-carrier", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "endgame-study-result-language", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "endgame-study-result-language", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "endgame-study-missing-applicable-suffix", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "endgame-study-missing-applicable-suffix", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "retrieval-current-position-truth", SuppressionReason.RetrievalNonAuthoritative)
    assertSuppressed(outline, "retrieval-current-position-truth", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "game-context-deferred-family", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "practicality-deferred-family", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "tablebase-result-service-deferred-family", SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, "endgame-result-service-deferred-family", SuppressionReason.ForbiddenShortcut)

  test("renderer cannot upgrade source context fallback wording strength"):
    val outline = ClaimSelector.select(
      Vector(sourceClaim("opening-context-renderer-cap", SourceContextKind.Opening)),
      rendererRequestedCap = Some(WordingStrength.AssertiveCertified)
    )

    assertEquals(outline.lead, None)
    assertEquals(outline.wordingStrengthCap, WordingStrength.ContextOnly)
    assertSuppressed(outline, "renderer-wording-upgrade", SuppressionReason.RendererNotAllowed)

  test("duplicate suppression is scoped to Sxx projections"):
    val certification = selectionClaim(
      id = "same-key-cert",
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = Some("white"),
      anchor = Some("board"),
      route = Some("shared_route"),
      scope = Some("position_local"),
      impact = ClaimImpact(resultMaterialImpact = 80, evidenceConfidence = 70),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "DevelopmentComparison", Some("white"), Some("board"), Some("shared_route")))
    )
    val delta = certification.copy(
      id = "same-key-delta",
      layer = ClaimLayer.Delta,
      impact = ClaimImpact(resultMaterialImpact = 60, evidenceConfidence = 60),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("white"), Some("board"), Some("shared_route")))
    )
    val outline = ClaimSelector.select(Vector(delta, certification))

    assertEquals(outline.lead.map(_.claim.id), Some("same-key-cert"))
    assert(outline.support.exists(_.claim.id == "same-key-delta"))
    assert(!outline.suppressedClaims.exists(suppressed =>
      suppressed.claim.id == "same-key-delta" &&
        suppressed.reasons.contains(SuppressionReason.DuplicateWeakerClaim)
    ))

  test("non-certification eval swing is ignored during ranking"):
    val strategic = selectionClaim(
      id = "strategic-board-reason",
      layer = ClaimLayer.Delta,
      status = ClaimStatus.Admitted,
      owner = Some("white"),
      anchor = Some("board"),
      route = Some("route"),
      impact = ClaimImpact(resultMaterialImpact = 40, evidenceConfidence = 60),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("white"), Some("board"), Some("route")))
    )
    val evalOnly = strategic.copy(
      id = "delta-with-eval-shortcut",
      impact = ClaimImpact(resultMaterialImpact = 40, evidenceConfidence = 50, evalSwing = 900)
    )
    val outline = ClaimSelector.select(Vector(evalOnly, strategic))

    assertEquals(outline.lead.map(_.claim.id), Some("strategic-board-reason"))

  test("renderer cannot upgrade wording strength cap"):
    val outline = ClaimSelector.select(
      Vector(
        selectionClaim(
          id = "qualified-s06",
          layer = ClaimLayer.Projection,
          status = ClaimStatus.Admitted,
          band = Some("S06"),
          owner = Some("white"),
          anchor = Some("sector:center"),
          route = Some("outpost_anchor"),
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Projection, "space_bind_restriction_route_certified", Some("white"), Some("sector:center"), Some("outpost_anchor"), Some("position_local"))),
          lowerCarrierRefs = Vector(
            EvidenceRef(EvidenceRefKind.Witness, "structural_space_claim", Some("white"), Some("sector:center"), Some("outpost_anchor"), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "short_run_slider_gate_restriction", Some("white"), Some("sector:center"), Some("outpost_anchor"), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "SpaceBindRestrictionCertification", Some("white"), Some("sector:center"), Some("outpost_anchor"), Some("position_local"))
          ),
          wordingStrengthCap = WordingStrength.QualifiedSupport
        )
      ),
      rendererRequestedCap = Some(WordingStrength.AssertiveCertified)
    )

    assertEquals(outline.wordingStrengthCap, WordingStrength.QualifiedSupport)
    assertSuppressed(outline, "renderer-wording-upgrade", SuppressionReason.RendererNotAllowed)

  test("renderer cannot upgrade wording strength cap without a lead"):
    val outline = ClaimSelector.select(
      Vector(sourceClaim("opening-reference", SourceContextKind.Opening)),
      rendererRequestedCap = Some(WordingStrength.AssertiveCertified)
    )

    assertEquals(outline.lead, None)
    assertEquals(outline.wordingStrengthCap, WordingStrength.ContextOnly)
    assertSuppressed(outline, "renderer-wording-upgrade", SuppressionReason.RendererNotAllowed)

  test("rejected source context is suppressed but not emitted as context"):
    val outline = ClaimSelector.select(
      Vector(sourceClaim("rejected-opening-reference", SourceContextKind.Opening).copy(status = ClaimStatus.Rejected))
    )

    assertEquals(outline.context, Vector.empty)
    assertSuppressed(outline, "rejected-opening-reference", SuppressionReason.ForbiddenShortcut)

  test("selected source context wording cap is clamped to context only"):
    val source = sourceClaim("assertive-opening-context", SourceContextKind.Opening)
      .copy(wordingStrengthCap = WordingStrength.AssertiveCertified)
    val outline = ClaimSelector.select(Vector(source))

    assertEquals(outline.lead, None)
    assertEquals(outline.wordingStrengthCap, WordingStrength.ContextOnly)
    assertEquals(outline.context.map(_.claim.wordingStrengthCap), Vector(WordingStrength.ContextOnly))
    assertSelectedContextReason(outline, "assertive-opening-context", SuppressionReason.SourceContextOnly)
    assertNotSuppressed(outline, "assertive-opening-context")

  test("global closure covers every S01 S25 start-ready band at selection"):
    val bands = StrategyProjectionScopeContract.startReadyBandIds.map(_.value)
    assertEquals(bands, (1 to 25).map(index => f"S$index%02d").toVector)

    bands.foreach: band =>
      val claim = closureProjection(band)
      val outline = ClaimSelector.select(Vector(claim))
      if band == StrategyProjectionScopeContract.S24.value then
        assertEquals(outline.lead, None, clues(band, outline.suppressedClaims))
        assertSuppressed(outline, claim.id, SuppressionReason.ForbiddenShortcut)
        assertSuppressed(outline, claim.id, SuppressionReason.NoBoardReason)
      else
        assertEquals(outline.lead.map(_.claim.band), Some(Some(band)), clues(band, outline.suppressedClaims))
        assertEquals(outline.lead.map(_.bucket), Some(ClaimBucket.ShouldLead), clues(band))
        assertEquals(outline.wordingStrengthCap, WordingStrength.QualifiedSupport, clues(band))

  test("S24 complete-looking projection remains blocker-only without descriptor runtime K"):
    val claim = closureProjection("S24")
    val outline = ClaimSelector.select(Vector(claim))

    assertEquals(outline.lead, None)
    assertSuppressed(outline, claim.id, SuppressionReason.ForbiddenShortcut)
    assertSuppressed(outline, claim.id, SuppressionReason.NoBoardReason)

  test("planner and surface rows cover the frozen selection contract cases"):
    val plannerById = plannerRows.map(row => row.id -> row).toMap

    val expectedPlannerRows = Map(
      "selector-certified-conversion-beats-opening-context" -> ("lead", Vector.empty, Some("mustLead")),
      "selector-raw-engine-suppressed" -> ("suppressed", Vector("raw_engine_only", "no_board_reason"), None),
      "selector-engine-through-certification-only" -> ("lead", Vector.empty, Some("mustLead")),
      "selector-engine-cert-without-root-cert-suppressed" -> ("suppressed", Vector("forbidden_shortcut", "no_board_reason"), None),
      "selector-source-ref-smuggling-suppressed" -> ("suppressed", Vector("source_context_only", "no_board_reason"), None),
      "selector-s24-not-generic-tactic-owner" -> ("suppressed", Vector("forbidden_shortcut", "no_board_reason"), None),
      "selector-s24-requires-forcing-and-conversion" -> ("suppressed", Vector("forbidden_shortcut", "no_board_reason"), None),
      "selector-s24-public-closed-with-complete-scaffold" -> ("suppressed", Vector("forbidden_shortcut", "no_board_reason"), None),
      "selector-admitted-sxx-requires-lower-evidence" -> ("lead", Vector("stale_evidence"), Some("shouldLead")),
      "selector-raw-engine-ref-smuggling-suppressed" -> ("suppressed", Vector("raw_engine_only", "no_board_reason"), None),
      "selector-raw-engine-lower-carrier-board-claim-suppressed" -> ("suppressed", Vector("raw_engine_only", "no_board_reason"), None),
      "selector-raw-engine-lower-carrier-suppressed" -> ("suppressed", Vector("raw_engine_only", "forbidden_shortcut"), None),
      "selector-unbound-lower-carrier-suppressed" -> ("suppressed", Vector("wrong_owner", "wrong_anchor", "wrong_route", "scope_mismatch"), None),
      "selector-support-deferred-not-lead" -> ("suppressed", Vector("support_only", "deferred"), None),
      "selector-context-only-without-board-lead" -> ("context_only", Vector("source_context_only"), Some("contextOnly")),
      "selector-retrieval-non-authoritative" -> ("context_only", Vector("retrieval_non_authoritative"), Some("contextOnly")),
      "selector-opening-context-only-fallback" -> ("context_only", Vector("source_context_only"), Some("contextOnly")),
      "selector-motif-context-only-detector-carrier" -> ("context_only", Vector("source_context_only"), Some("contextOnly")),
      "selector-endgame-study-context-only-applicable" -> ("context_only", Vector("source_context_only"), Some("contextOnly")),
      "selector-retrieval-context-only-reference" -> ("context_only", Vector("source_context_only", "retrieval_non_authoritative"), Some("contextOnly")),
      "selector-exact-board-lead-bounds-source-context" -> ("support", Vector("source_context_only", "retrieval_non_authoritative"), Some("support")),
      "selector-ambiguous-opening-transposition-suppressed" -> ("suppressed", Vector("source_context_only", "ambiguous_transposition"), None),
      "selector-opening-specific-citation-suppressed" -> ("suppressed", Vector("source_context_only", "forbidden_shortcut", "no_board_reason"), None),
      "selector-motif-without-detector-carrier-suppressed" -> ("suppressed", Vector("source_context_only", "forbidden_shortcut", "no_board_reason"), None),
      "selector-endgame-study-result-language-suppressed" -> ("suppressed", Vector("source_context_only", "forbidden_shortcut", "no_board_reason"), None),
      "selector-retrieval-truth-promotion-suppressed" -> ("suppressed", Vector("source_context_only", "retrieval_non_authoritative", "forbidden_shortcut", "no_board_reason"), None),
      "selector-deferred-source-families-suppressed" -> ("suppressed", Vector("source_context_only", "forbidden_shortcut"), None),
      "selector-wrong-owner-anchor-route-scope" -> ("suppressed", Vector("wrong_owner", "wrong_anchor", "wrong_route", "scope_mismatch"), None),
      "selector-ambiguous-side-beneficiary-defender" -> ("suppressed", Vector("wrong_owner", "wrong_route", "scope_mismatch"), None),
      "selector-competing-sxx-nonredundancy" -> ("support", Vector("duplicate_weaker_claim"), Some("support")),
      "selector-bucket-priority-mustlead-beats-shouldlead" -> ("lead", Vector.empty, Some("mustLead")),
      "selector-s07-s08-s21-initiative-lead-support-rivals" -> ("lead", Vector("rival_band", "support_only", "raw_engine_only", "no_board_reason", "source_context_only", "renderer_not_allowed"), Some("shouldLead")),
      "selector-s21-wrong-binding-suppressed" -> ("suppressed", Vector("wrong_owner", "wrong_anchor", "wrong_route", "scope_mismatch"), None),
      "selector-s15-same-candidate-creation-lead" -> ("lead", Vector("source_context_only"), Some("shouldLead")),
      "selector-s16-same-enemy-passer-suppression-lead" -> ("lead", Vector("raw_engine_only", "no_board_reason", "source_context_only", "retrieval_non_authoritative"), Some("shouldLead")),
      "selector-s15-s13-s14-support-same-candidate-only" -> ("support", Vector("wrong_owner", "wrong_anchor"), Some("support")),
      "selector-s13-s14-cannot-outrank-s15-owner" -> ("support", Vector.empty, Some("support")),
      "selector-s15-existing-passer-split-anchor-suppressed" -> ("suppressed", Vector("forbidden_shortcut", "no_board_reason", "wrong_anchor"), None),
      "selector-s16-blocker-shell-only-suppressed" -> ("suppressed", Vector("forbidden_shortcut", "no_board_reason"), None),
      "selector-s15-forged-kind-route-suppressed" -> ("suppressed", Vector("forbidden_shortcut", "no_board_reason", "wrong_route"), None),
      "selector-s16-forged-kind-route-suppressed" -> ("suppressed", Vector("forbidden_shortcut", "no_board_reason", "wrong_route"), None),
      "selector-s16-self-owned-passer-suppressed" -> ("suppressed", Vector("wrong_owner"), None),
      "selector-s15-s16-conflict-clarity-required" -> ("lead", Vector("wrong_owner", "wrong_route", "scope_mismatch", "rival_band"), Some("shouldLead")),
      "selector-s17-liability-relief-lead" -> ("lead", Vector.empty, Some("shouldLead")),
      "selector-s18-bishop-pair-conversion-lead" -> ("lead", Vector.empty, Some("shouldLead")),
      "selector-s19-simplification-lead" -> ("lead", Vector.empty, Some("shouldLead")),
      "selector-s22-certified-hold-lead" -> ("lead", Vector.empty, Some("shouldLead")),
      "selector-s17-s19-conflict" -> ("lead", Vector("rival_band", "forbidden_shortcut", "no_board_reason"), Some("shouldLead")),
      "selector-s18-s19-conflict" -> ("lead", Vector("rival_band", "forbidden_shortcut", "no_board_reason"), Some("shouldLead")),
      "selector-s19-s22-conflict" -> ("lead", Vector("rival_band"), Some("shouldLead")),
      "selector-conversion-hold-contradiction-suppressed" -> ("suppressed", Vector("forbidden_shortcut", "no_board_reason"), None),
      "selector-underqualified-conversion-below-hold" -> ("lead", Vector("rival_band"), Some("shouldLead")),
      "selector-s18-material-conversion-route-above-hold" -> ("lead", Vector("rival_band"), Some("shouldLead")),
      "selector-conversion-cluster-wrong-binding-and-source-suppressed" -> ("suppressed", Vector("wrong_owner", "wrong_anchor", "wrong_route", "scope_mismatch", "raw_engine_only", "no_board_reason", "source_context_only", "retrieval_non_authoritative"), None),
      "selector-conversion-cluster-unbound-projection-evidence-suppressed" -> ("suppressed", Vector("wrong_route", "scope_mismatch", "forbidden_shortcut"), None),
      "selector-s01-king-wing-storm-lead" -> ("lead", Vector.empty, Some("shouldLead")),
      "selector-s02-king-ring-concentration-lead" -> ("lead", Vector.empty, Some("shouldLead")),
      "selector-s03-diagonal-king-attack-lead" -> ("lead", Vector.empty, Some("shouldLead")),
      "selector-s04-king-shelter-breach-lead" -> ("lead", Vector.empty, Some("shouldLead")),
      "selector-king-attack-same-king-nonredundant" -> ("support", Vector.empty, Some("support")),
      "selector-king-attack-wrong-binding-and-source-suppressed" -> ("suppressed", Vector("wrong_owner", "wrong_anchor", "wrong_route", "scope_mismatch", "forbidden_shortcut", "no_board_reason", "source_context_only", "retrieval_non_authoritative"), None),
      "selector-king-attack-runtime-carrier-ownership" -> ("lead", Vector("forbidden_shortcut", "no_board_reason"), Some("shouldLead")),
      "selector-king-attack-raw-support-deferred-suppressed" -> ("suppressed", Vector("raw_engine_only", "no_board_reason", "support_only", "deferred"), None),
      "selector-certified-result-beats-king-attack" -> ("lead", Vector("raw_engine_only"), Some("mustLead")),
      "selector-engine-certified-board-reason-lead" -> ("lead", Vector.empty, Some("mustLead")),
      "selector-engine-certified-without-board-reason-suppressed" -> ("suppressed", Vector("no_board_reason"), None),
      "selector-board-sxx-beats-opaque-engine-swing" -> ("lead", Vector("no_board_reason"), Some("shouldLead")),
      "selector-engine-certified-result-beats-sxx-with-board-reason" -> ("lead", Vector.empty, Some("mustLead")),
      "selector-engine-certified-stale-wrong-binding-suppressed" -> ("suppressed", Vector("stale_evidence", "wrong_owner", "wrong_anchor", "wrong_route", "scope_mismatch"), None),
      "selector-engine-certified-multipv-ambiguity-suppressed" -> ("suppressed", Vector("deferred", "no_board_reason"), None),
      "selector-engine-score-normalization-boundary-suppressed" -> ("suppressed", Vector("support_only", "no_board_reason"), None),
      "selector-non-cert-eval-swing-ignored" -> ("lead", Vector.empty, Some("shouldLead")),
      "selector-cross-layer-same-key-not-duplicate" -> ("support", Vector.empty, Some("support")),
      "selector-global-s01-s25-band-closure" -> ("lead", Vector.empty, Some("shouldLead")),
      "outline-builder-lead-maps-main" -> ("lead", Vector.empty, Some("shouldLead")),
      "outline-builder-context-soft-reasons" -> ("context_only", Vector("source_context_only"), Some("contextOnly")),
      "outline-builder-suppressed-maps-blocked" -> ("suppressed", Vector("raw_engine_only", "no_board_reason"), None)
    )
    expectedPlannerRows.foreach { case (id, (expectation, reasons, bucket)) =>
      val row = plannerById.getOrElse(id, fail(s"missing planner row $id"))
      assertEquals(row.expectation, expectation)
      assertEquals(row.requiredReasons, reasons)
      assertEquals(row.expectedBucket, bucket)
    }

    val expectedSurfaceRows =
      Set(
        "surface-renderer-cannot-upgrade-wording-cap",
        "surface-renderer-cannot-admit-lead",
        "surface-renderer-cannot-rank-claims",
        "surface-renderer-cannot-source-truth",
        "surface-renderer-cannot-suppress-claims",
        "surface-s07-s08-s21-renderer-cap-preserved",
        "surface-passer-structure-renderer-cap-preserved",
        "surface-conversion-simplification-hold-renderer-cap-preserved",
        "surface-king-attack-renderer-cap-preserved",
        "surface-engine-eval-renderer-cap-preserved",
        "surface-source-context-renderer-cap-preserved",
        "surface-outline-builder-cap-preserved",
        "surface-outline-builder-evidence-not-recomputed",
        "surface-outline-builder-opening-context-not-truth"
      )
    expectedSurfaceRows.foreach: id =>
      val row = surfaceRows.find(_.id == id).getOrElse(fail(s"missing surface row $id"))
      assertEquals(row.expectation, "wording_cap_preserved")
      assertEquals(row.requiredReasons, Vector("renderer_not_allowed"))
      assertEquals(row.expectedBucket, None)
    plannerRows.foreach(_.validated)
    surfaceRows.foreach(_.validated)

  test("selection contract docs keep literal parity with executable keys"):
    val docs =
      Vector(
        "modules/commentary/docs/legacy-pre-semantic-reset/CommentarySelectionContract.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/CommentaryCoreSSOT.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/ValidationMethodology.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/SourceContextContract.md"
      ).map(path => Files.readString(Paths.get(path))).mkString("\n")

    (ClaimBucket.values.map(_.key) ++ SuppressionReason.values.map(_.key) ++ WordingStrength.values.map(_.key) ++ Vector(
      "ClaimBucket",
      "SuppressionReason",
      "CommentaryOutline",
      "wordingStrengthCap",
      "Engine E only reaches selection through Certification",
      "S24 is not the generic tactic owner"
    )).distinct.foreach(token => assert(docs.contains(token), clues(token)))

  private def selectionClaim(
      id: String,
      layer: ClaimLayer,
      status: ClaimStatus,
      band: Option[String] = None,
      owner: Option[String] = None,
      beneficiary: Option[String] = Some("white"),
      defender: Option[String] = Some("black"),
      sideToMove: Option[String] = Some("white"),
      anchor: Option[String] = Some("board"),
      route: Option[String] = Some("route"),
      scope: Option[String] = Some("position_local"),
      impact: ClaimImpact = ClaimImpact(evidenceConfidence = 50),
      evidenceRefs: Vector[EvidenceRef] = Vector(EvidenceRef(EvidenceRefKind.ExactBoard, "exact-board")),
      lowerCarrierRefs: Vector[EvidenceRef] = Vector(EvidenceRef(EvidenceRefKind.Witness, "lower-carrier", Some("white"), Some("board"), Some("route"), Some("position_local"))),
      exactBoardBound: Boolean = true,
      wordingStrengthCap: WordingStrength = WordingStrength.QualifiedSupport,
      suppressionHints: Vector[SuppressionReason] = Vector.empty
  ): CommentaryClaim =
    CommentaryClaim(
      id = id,
      layer = layer,
      status = status,
      band = band,
      owner = owner,
      beneficiary = beneficiary,
      defender = defender,
      sideToMove = sideToMove,
      anchor = anchor,
      route = route,
      scope = scope,
      impact = impact,
      evidenceRefs = evidenceRefs,
      lowerCarrierRefs = lowerCarrierRefs,
      exactBoardBound = exactBoardBound,
      wordingStrengthCap = wordingStrengthCap,
      suppressionHints = suppressionHints
    )

  private def sourceClaim(id: String, kind: SourceContextKind): CommentaryClaim =
    val evidenceRefs =
      kind match
        case SourceContextKind.Opening =>
          Vector(EvidenceRef(EvidenceRefKind.SourceContext, s"opening-position:$id:canonical"))
        case SourceContextKind.Motif =>
          Vector(
            EvidenceRef(EvidenceRefKind.SourceContext, s"motif-example:$id"),
            EvidenceRef(EvidenceRefKind.ExactBoard, s"motif-detector-carrier:$id")
          )
        case SourceContextKind.EndgameStudy =>
          Vector(
            EvidenceRef(EvidenceRefKind.SourceContext, s"endgame-study:$id:applicable"),
            EvidenceRef(EvidenceRefKind.ExactBoard, s"endgame-study-applicability:$id")
          )
        case SourceContextKind.Retrieval =>
          Vector(EvidenceRef(EvidenceRefKind.SourceContext, s"retrieval-example:$id"))
    selectionClaim(
      id = id,
      layer = ClaimLayer.SourceContext,
      status = ClaimStatus.Context,
      anchor = None,
      route = Some(kind.key),
      scope = Some("context_only"),
      impact = ClaimImpact(),
      evidenceRefs = evidenceRefs,
      lowerCarrierRefs = Vector.empty,
      wordingStrengthCap = WordingStrength.ContextOnly
    ).copy(sourceContextKind = Some(kind))

  private def engineCertifiedClaim(
      id: String,
      status: ClaimStatus = ClaimStatus.Admitted,
      owner: Option[String] = Some("white"),
      anchor: Option[String] = Some("board"),
      route: Option[String] = Some("route"),
      scope: Option[String] = Some("position_local"),
      impact: ClaimImpact,
      certificationId: String = "CertifiedKingSafetyEdge",
      engineRef: Option[EvidenceRef] = None,
      boardReasons: Vector[EvidenceRef],
      exactBoardBound: Boolean = true,
      wordingStrengthCap: WordingStrength = WordingStrength.AssertiveCertified,
      suppressionHints: Vector[SuppressionReason] = Vector.empty
  ): CommentaryClaim =
    selectionClaim(
      id = id,
      layer = ClaimLayer.Certification,
      status = status,
      owner = owner,
      anchor = anchor,
      route = route,
      scope = scope,
      impact = impact,
      evidenceRefs =
        Vector(
          EvidenceRef(EvidenceRefKind.Certification, certificationId, owner, anchor, route, scope),
          engineRef.getOrElse(EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", owner, anchor, route, scope))
        ) ++ boardReasons,
      lowerCarrierRefs = Vector.empty,
      exactBoardBound = exactBoardBound,
      wordingStrengthCap = wordingStrengthCap,
      suppressionHints = suppressionHints
    )

  private def admittedProjection(
      id: String,
      band: String,
      anchor: String = "board",
      route: String,
      score: Int
  ): CommentaryClaim =
    val evidenceKind =
      band match
        case "S07" => "initiative_conversion_route_certified"
        case "S01" => "king_wing_storm_route_certified"
        case "S02" => "king_ring_concentration_route_certified"
        case "S03" => "diagonal_king_attack_route_certified"
        case "S04" => "king_shelter_breach_route_certified"
        case "S05" => "center_release_route_certified"
        case "S06" => "space_bind_restriction_route_certified"
        case "S09" => "file_penetration_route_certified"
        case "S10" => "outpost_occupation_route_certified"
        case "S11" => "weak_pawn_target_pressure_persistence_certified"
        case "S12" => "local_access_superiority_route_certified"
        case "S08" => "counterplay_denial_route_certified"
        case "S13" => "wing_damage_route_certified"
        case "S14" => "chain_base_contact_route_certified"
        case "S15" => "passer_creation_route_certified"
        case "S16" => "passer_suppression_route_certified"
        case "S17" => "liability_relief_certified"
        case "S18" =>
          route match
            case "bishop_pair_to_initiative" => "bishop_pair_initiative_conversion_certified"
            case "bishop_pair_to_structure" => "bishop_pair_structure_conversion_certified"
            case "bishop_pair_to_material" => "bishop_pair_material_conversion_certified"
            case _ => "bishop_pair_material_conversion_certified"
        case "S19" =>
          route match
            case "trade_invariant_to_hold" => "trade_invariant_hold_simplification_certified"
            case _ => "trade_invariant_material_simplification_certified"
        case "S22" =>
          route match
            case "perpetual_hold" => "perpetual_hold_certified"
            case _ => "fortress_hold_certified"
        case "S20" => "mobility_domination_route_certified"
        case "S21" => "counterplay_survival_route_certified"
        case "S23" =>
          route match
            case "king_opposition" => "king_opposition_certified"
            case _ => "king_entry_conversion_certified"
        case "S24" => "same_target_forcing_realization"
        case "S25" => "rank_access_consequence_certified"
        case other => s"${other.toLowerCase}_route_certified"
    val lowerCarrierRefs =
      band match
        case "S01" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "available_lever_trigger", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "CertifiedKingSafetyEdge", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S02" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "CertifiedKingSafetyEdge", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S03" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "diagonal_lane_only", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "ComparativeKingFragility", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "CertifiedKingSafetyEdge", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S04" =>
          val supportBreakRefs =
            Option
              .when(route == "support_break_breach")(
                EvidenceRef(EvidenceRefKind.Witness, "diagonal_lane_only", Some("white"), Some(anchor), Some(route), Some("position_local"))
              )
              .toVector
          Vector(
            EvidenceRef(EvidenceRefKind.Object, "KingSafetyShell", Some("black"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "CertifiedKingSafetyEdge", Some("white"), Some(anchor), Some(route), Some("position_local"))
          ) ++ supportBreakRefs
        case "S05" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "available_lever_trigger", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S06" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "structural_space_claim", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "short_run_slider_gate_restriction", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "SpaceBindRestrictionCertification", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S07" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Object, "OpeningDevelopmentRegime", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "DevelopmentComparison", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "InitiativeWindow", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S08" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Certification, "InitiativeWindow", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S09" =>
          Vector(EvidenceRef(EvidenceRefKind.Witness, "file_lane_state", Some("white"), Some(anchor), Some(route), Some("position_local")))
        case "S10" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "weak_outpost_square_state", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "knight_on_outpost_square", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S11" =>
          Vector(EvidenceRef(EvidenceRefKind.Witness, "weak_pawn_target_state", Some("white"), Some(anchor), Some(route), Some("position_local")))
        case "S12" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "weak_outpost_square_state", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "short_run_slider_gate_restriction", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S13" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "sector_asymmetry_state", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "available_lever_trigger", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S14" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "available_lever_trigger", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S15" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Root, "candidate_passer", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, s"same_candidate_${route}_creation_route", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S16" =>
          val routeProof =
            route match
              case "restriction_hold" =>
                Vector(
                  EvidenceRef(EvidenceRefKind.Witness, "short_run_slider_gate_restriction", Some("white"), Some(anchor), Some(route), Some("position_local")),
                  EvidenceRef(EvidenceRefKind.Certification, "PerpetualCheckHolding", Some("white"), Some(anchor), Some(route), Some("position_local"))
                )
              case "non_losing_race" =>
                Vector(EvidenceRef(EvidenceRefKind.Certification, "PromotionRace", Some("white"), Some(anchor), Some(route), Some("position_local")))
              case _ =>
                Vector(EvidenceRef(EvidenceRefKind.Certification, "FortressDrawCertification", Some("white"), Some(anchor), Some(route), Some("position_local")))
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "passed_pawn_entity_state", Some("black"), Some(anchor), Some(route), Some("position_local")),
          ) ++ routeProof
        case "S20" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Certification, "MobilityComparison", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "short_run_slider_gate_restriction", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S21" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, "InitiativeWindow", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S17" =>
          val reliefSeed =
            route match
              case "repair_route" => "same_piece_repair_route_seed"
              case "exchange_relief" => "same_piece_exchange_relief_seed"
              case _ => route
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "same_piece_liability_anchor_seed", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, reliefSeed, Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S18" =>
          val supportId =
            route match
              case "bishop_pair_to_initiative" => "InitiativeWindow"
              case "bishop_pair_to_structure" => "MobilityComparison"
              case "bishop_pair_to_material" => "MaterialHarvest"
              case _ => "MaterialHarvest"
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "bishop_pair_state", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, supportId, Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S19" =>
          val supportId =
            route match
              case "trade_invariant_to_hold" => "FortressDrawCertification"
              case _ => "MaterialHarvest"
          Vector(
            EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Certification, supportId, Some("white"), Some(anchor), Some(route), Some("position_local"))
              )
        case "S22" =>
          route match
            case "perpetual_hold" =>
              Vector(EvidenceRef(EvidenceRefKind.Certification, "PerpetualCheckHolding", Some("white"), Some(anchor), Some(route), Some("position_local")))
            case _ =>
              Vector(
                EvidenceRef(EvidenceRefKind.Object, "FortressHoldingShell", Some("white"), Some(anchor), Some(route), Some("position_local")),
                EvidenceRef(EvidenceRefKind.Certification, "FortressDrawCertification", Some("white"), Some(anchor), Some(route), Some("position_local"))
              )
        case "S23" =>
          route match
            case "king_opposition" =>
              Vector(EvidenceRef(EvidenceRefKind.Witness, "king_opposition_contact_seed", Some("white"), Some(anchor), Some(route), Some("position_local")))
            case _ =>
              Vector(
                EvidenceRef(EvidenceRefKind.Witness, "king_entry_square_seed", Some("white"), Some(anchor), Some(route), Some("position_local")),
                EvidenceRef(EvidenceRefKind.Witness, "king_access_route_seed", Some("white"), Some(anchor), Some(route), Some("position_local"))
              )
        case "S24" =>
          Vector(
            EvidenceRef(EvidenceRefKind.Witness, "target_resource_dependency_seed", Some("white"), Some(anchor), Some(route), Some("position_local")),
            EvidenceRef(EvidenceRefKind.Witness, "target_attack_convergence_seed", Some("white"), Some(anchor), Some(route), Some("position_local"))
          )
        case "S25" =>
          Vector(EvidenceRef(EvidenceRefKind.Witness, "rank_corridor_state_seed", Some("white"), Some(anchor), Some(route), Some("position_local")))
        case _ =>
          Vector(EvidenceRef(EvidenceRefKind.Certification, "lower-certification", Some("white"), Some(anchor), Some(route), Some("position_local")))
    selectionClaim(
      id = id,
      layer = ClaimLayer.Projection,
      status = ClaimStatus.Admitted,
      band = Some(band),
      owner = Some("white"),
      anchor = Some(anchor),
      route = Some(route),
      scope = Some("position_local"),
      impact = ClaimImpact(persistenceAfterDefense = score, evidenceConfidence = score),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Projection, evidenceKind, Some("white"), Some(anchor), Some(route), Some("position_local"))),
      lowerCarrierRefs = lowerCarrierRefs
    )

  private def closureProjection(band: String): CommentaryClaim =
    val route =
      band match
        case "S01" => "same_wing_contact"
        case "S02" => "direct_piece_concentration"
        case "S03" => "king_facing_diagonal_entry"
        case "S04" => "shell_payload_breach"
        case "S05" => "center_pawn_target"
        case "S06" => "outpost_anchor"
        case "S07" => "development_led_window"
        case "S08" => "rival_break_source_suppressed"
        case "S09" => "open_file_entry"
        case "S10" => "knight_only_outpost_occupancy"
        case "S11" => "same_target_fixation"
        case "S12" => "weak_square_route"
        case "S13" => "phalanx_edge_target"
        case "S14" => "chain_base_target"
        case "S15" => "s13_wing_damage"
        case "S16" => "blockade_hold"
        case "S17" => "repair_route"
        case "S18" => "bishop_pair_to_initiative"
        case "S19" => "trade_invariant_to_material"
        case "S20" => "mobility_plus_restriction"
        case "S21" => "center_source_survives"
        case "S22" => "fortress_draw_hold"
        case "S23" => "king_entry_route"
        case "S24" => "same_target_realization"
        case "S25" => "cross_wing_rank_switch"
        case _ => s"${band.toLowerCase}_closure_route"
    val base =
      admittedProjection(
        id = s"global-${band.toLowerCase}-selection-closure",
        band = band,
        anchor = s"anchor:$band",
        route = route,
        score = 70
      )
    val evidenceKinds =
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand
        .getOrElse(band, fail(s"missing projection evidence for $band"))
        .map(_.value)
    val evidenceRefs =
      if band == "S24" then
        evidenceKinds.map(kind => EvidenceRef(EvidenceRefKind.Projection, kind, Some("white"), Some(s"anchor:$band"), Some(route), Some("position_local")))
      else
        Vector(EvidenceRef(EvidenceRefKind.Projection, evidenceKinds.head, Some("white"), Some(s"anchor:$band"), Some(route), Some("position_local")))
    base.copy(evidenceRefs = evidenceRefs)

  private def assertSuppressed(
      outline: CommentaryOutline,
      claimId: String,
      reason: SuppressionReason
  ): Unit =
    assert(
      outline.suppressedClaims.exists(suppressed =>
        suppressed.claim.id == claimId && suppressed.reasons.contains(reason)
      ),
      clues(claimId, reason, outline.suppressedClaims)
    )

  private def assertNotSuppressed(
      outline: CommentaryOutline,
      claimId: String
  ): Unit =
    assert(
      !outline.suppressedClaims.exists(_.claim.id == claimId),
      clues(claimId, outline.suppressedClaims)
    )

  private def assertSelectedContextReason(
      outline: CommentaryOutline,
      claimId: String,
      reason: SuppressionReason
  ): Unit =
    assert(
      outline.context.exists(selected =>
        selected.claim.id == claimId && selected.softReasons.contains(reason)
      ),
      clues(claimId, reason, outline.context)
    )

private object SelectionCorpus:

  final case class Row(
      id: String,
      caseType: String,
      expectation: String,
      requiredReasons: Vector[String],
      expectedBucket: Option[String],
      notes: Option[String]
  ):
    def validated: Unit =
      require(id.matches("^[a-z0-9][a-z0-9_-]*$"), s"Invalid row id $id")
      require(
        Set("exact", "negative", "surface_reinflation", "comparative_false_rival").contains(caseType),
        s"Row $id uses unsupported caseType $caseType"
      )
      require(
        Set("lead", "support", "context_only", "suppressed", "wording_cap_preserved").contains(expectation),
        s"Row $id uses unsupported expectation $expectation"
      )
      requiredReasons.foreach: reason =>
        require(
          SuppressionReason.fromKey(reason).nonEmpty,
          s"Row $id uses unsupported suppression reason $reason"
        )
      expectedBucket.foreach: bucket =>
        require(
          ClaimBucket.fromKey(bucket).nonEmpty,
          s"Row $id uses unsupported bucket $bucket"
        )

  private given Reads[Row] = Json.reads[Row]

  def loadPlannerRows(): Vector[Row] =
    loadRows("/commentary-corpus/planner-expectations.jsonl")

  def loadSurfaceRows(): Vector[Row] =
    loadRows("/commentary-corpus/surface-expectations.jsonl")

  private def loadRows(path: String): Vector[Row] =
    val stream =
      Option(getClass.getResourceAsStream(path)).getOrElse(
        throw IllegalStateException(s"Missing $path")
      )
    try
      scala.io.Source
        .fromInputStream(stream, "UTF-8")
        .getLines()
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(line => Json.parse(line).as[Row])
        .toVector
    finally stream.close()
