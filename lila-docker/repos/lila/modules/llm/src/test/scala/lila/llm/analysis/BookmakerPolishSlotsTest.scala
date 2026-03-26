package lila.llm.analysis

import munit.FunSuite
import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*

class BookmakerPolishSlotsTest extends FunSuite:

  private def truthContract(
      ownershipRole: TruthOwnershipRole,
      visibilityRole: TruthVisibilityRole,
      surfaceMode: TruthSurfaceMode,
      truthClass: DecisiveTruthClass = DecisiveTruthClass.Best
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("c3g3"),
      verifiedBestMove = Some("c3g3"),
      truthClass = truthClass,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = DecisiveReasonFamily.QuietTechnicalMove,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = ownershipRole,
      visibilityRole = visibilityRole,
      surfaceMode = surfaceMode,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  private def surfaceDrivenPack(
      ideaKind: String = StrategicIdeaKind.KingAttackBuildUp,
      focusSquares: List[String] = List("g7"),
      routePiece: String = "R",
      route: List[String] = List("c3", "g3"),
      routePurpose: String = "rook lift",
      targetPiece: String = "Q",
      targetSquare: String = "g7",
      compensation: Option[String] = None,
      investedMaterial: Option[Int] = None
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_attack_g7",
          ownerSide = "white",
          kind = ideaKind,
          group = StrategicIdeaGroup.InteractionAndTransformation,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = focusSquares,
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.91
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = routePiece,
          from = route.headOption.getOrElse("c3"),
          route = route,
          purpose = routePurpose,
          strategicFit = 0.88,
          tacticalSafety = 0.74,
          surfaceConfidence = 0.82,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_g7",
          ownerSide = "white",
          piece = targetPiece,
          from = "d1",
          targetSquare = targetSquare,
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("mating net")
        )
      ),
      longTermFocus = List(s"keep pressure on $targetSquare"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = compensation,
          investedMaterial = investedMaterial,
          dominantIdeaKind = Some(ideaKind),
          dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some(focusSquares.mkString(", "))
        )
      )
    )

  private def tacticalCtx(base: NarrativeContext): NarrativeContext =
    base.copy(
      meta = Some(
        MetaSignals(
          choiceType = ChoiceType.NarrowChoice,
          targets = Targets(Nil, Nil),
          planConcurrency = PlanConcurrency("Attack", None, "independent"),
          errorClass = Some(
            ErrorClassification(
              isTactical = true,
              missedMotifs = List("Fork"),
              errorSummary = "tactical miss"
            )
          )
        )
      )
    )

  private def genericDecisionOutline(claim: String, followUp: String): NarrativeOutline =
    NarrativeOutline(
      beats = List(
        OutlineBeat(kind = OutlineBeatKind.Context, text = "Context beat."),
        OutlineBeat(kind = OutlineBeatKind.MainMove, text = s"$claim $followUp")
      )
    )

  BookmakerProseGoldenFixtures.all.foreach { fixture =>
    test(s"${fixture.id} builds bookmaker polish slots for ${fixture.expectedLens}") {
      val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
      val slotsOpt = BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)
      assert(slotsOpt.isDefined, clues(s"expected slots for ${fixture.id}"))
      val slots = slotsOpt.get
      clues(fixture.id, slots.claim, slots.supportPrimary, slots.tension)
      assertEquals(slots.lens, fixture.expectedLens)
      assert(BookmakerProseContract.stripMoveHeader(slots.claim).nonEmpty)
      assert(slots.paragraphPlan.headOption.contains("p1=claim"))
      assert(slots.paragraphPlan.exists(_.startsWith("p2=")))
      assert(slots.paragraphPlan.size <= 3)
      assertEquals(slots.supportSecondary, None)
      assert(BookmakerProseContract.claimLikeFirstParagraph(slots.claim, slots.claim))
    }
  }

  test("structure-led slots preserve deployment reason and move contribution roles") {
    val fixture = BookmakerProseGoldenFixtures.entrenchedPiece
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slots =
      BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)
        .getOrElse(fail("missing slots"))
    val claim = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(claim.contains("french chain"))
    assert(!claim.contains("aiming at"))
    assert(slots.supportPrimary.exists(text => {
      val low = text.toLowerCase
      low.contains("structure's logic") || low.contains("reroute") || low.contains("knight")
    }))
    assert(slots.supportPrimary.exists(text => {
      val low = text.toLowerCase
      low.contains("gains force") || low.contains("squeeze")
    }))
    assert(slots.tension.exists(text => {
      val low = text.toLowerCase
      low.contains("route") || low.contains("post") || low.contains("g4") || low.contains("justification")
    }))
  }

  test("soft repair restores missing claim and strips placeholders") {
    val fixture = BookmakerProseGoldenFixtures.practicalChoice
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slots = BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack).get
    val broken =
      """The move keeps pressure and remains preferable.
        |
        |The direct alternative stays secondary because Piece Activation is deferred as PlayableByPV under strict evidence mode (supported by engine-coupled continuation (probe evidence pending)). Further probe work still targets Piece Activation [subplan:worst piece improvement].""".stripMargin
    val repaired = BookmakerSoftRepair.repair(broken, slots)
    assert(repaired.applied)
    assertEquals(repaired.evaluation.placeholderHits, Nil)
    assertEquals(repaired.evaluation.genericHits, Nil)
    assert(repaired.evaluation.claimLikeFirstParagraph)
    assert(repaired.evaluation.paragraphBudgetOk)
    assert(repaired.materialApplied)
    assert(repaired.materialActions.nonEmpty)
  }

  test("soft repair treats claim-only restore as cosmetic") {
    val slots =
      BookmakerPolishSlots(
        lens = StrategicLens.Decision,
        claim = "1. e4: This move claims central space.",
        supportPrimary = Some("It opens lines for both bishops."),
        supportSecondary = None,
        tension = None,
        evidenceHook = None,
        coda = None,
        factGuardrails = Nil,
        paragraphPlan = List("p1=claim", "p2=support_chain")
      )
    val generic =
      """The move remains preferable.
        |
        |It opens lines for both bishops.""".stripMargin
    val repaired = BookmakerSoftRepair.repair(generic, slots)
    assert(repaired.applied)
    assert(repaired.actions.contains("claim_restore"))
    assert(!repaired.materialApplied)
    assertEquals(repaired.materialActions, Nil)
  }

  test("deterministic third paragraph wraps bare variation evidence in prose") {
    val slots =
      BookmakerPolishSlots(
        lens = StrategicLens.Decision,
        claim = "1. e4: This move claims central space.",
        supportPrimary = Some("It opens lines for both bishops."),
        supportSecondary = Some("This move prepares rapid development."),
        tension = Some("Black can still challenge the center if White drifts."),
        evidenceHook = Some("a) ...c5 Nf3 Nc6 (+0.2)"),
        coda = None,
        factGuardrails = Nil,
        paragraphPlan = List("p1=claim", "p2=support_chain", "p3=tension_or_evidence")
      )
    val paragraphs = BookmakerSoftRepair.deterministicParagraphs(slots)
    assertEquals(paragraphs.size, 3)
    assert(paragraphs(2).contains("One concrete line that keeps the idea in play is a) ...c5 Nf3 Nc6 (+0.2)"))
  }

  test("sanitizer preserves chess ellipsis markers in prose") {
    val raw =
      "Probe evidence starts with. Rc8: Rc8 Rc3 Rg6 (+0.42). The alternative is 12.. Qh5, and fixing lines with.. h5 makes.. Rh6-g6 realistic."
    val sanitized = BookmakerSlotSanitizer.sanitizeUserText(raw)
    assertEquals(
      sanitized,
      "Probe evidence starts with ...Rc8: Rc8 Rc3 Rg6 (+0.42). The alternative is 12...Qh5, and fixing lines with ...h5 makes ...Rh6-g6 realistic."
    )
  }

  test("tactical blunders promote the negative main-move claim ahead of strategic thesis text") {
    val ctx =
      BookmakerProseGoldenFixtures.rookPawnMarch.ctx.copy(
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.NarrowChoice,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Rook-Pawn March", None, "independent"),
            errorClass = Some(
              ErrorClassification(
                isTactical = true,
                missedMotifs = List("Fork"),
                errorSummary = "전술(320cp, Fork)"
              )
            )
          )
        )
      )
    val outline =
      NarrativeOutline(
        beats = List(
          OutlineBeat(
            kind = OutlineBeatKind.Context,
            text = "The move reorganizes the pieces around Kingside Clamp, aiming at Rook-Pawn March."
          ),
          OutlineBeat(
            kind = OutlineBeatKind.MainMove,
            text =
              "**h5??** is a tactical blunder; it misses the idea of a fork on e6 and allows ...Rc1+ by force. Better is **Rc3** to keep tighter control. The forcing reply lands before the kingside plan gets going."
          )
        )
      )
    val slots = BookmakerPolishSlotsBuilder.build(ctx, outline, refs = None).getOrElse(fail("missing slots"))
    val claimCore = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(claimCore.contains("tactical blunder"))
    assert(!claimCore.contains("reorganizes the pieces around kingside clamp"))
    assert(slots.support.exists(_.toLowerCase.contains("rc3")), clue(slots.support))
  }

  test("compensation slots surface campaign owner and execution objective from strategy pack") {
    val fixture = BookmakerProseGoldenFixtures.exchangeSacrifice
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slots =
      BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)
        .getOrElse(fail("missing slots"))

    val claim = BookmakerProseContract.stripMoveHeader(slots.claim)
    assert(
      claim.toLowerCase.contains("material can wait") ||
        claim.toLowerCase.contains("recover the material") ||
        claim.toLowerCase.contains("gives up material") ||
        claim.toLowerCase.contains("winning it back"),
      clue(claim)
    )
    assert(!claim.contains("180cp"))
    assert((slots.support :+ slots.tension.getOrElse("")).exists(text => {
      val low = text.toLowerCase
      low.contains("initiative") ||
        low.contains("attack") ||
        low.contains("mating attack") ||
        low.contains("winning the material back") ||
        low.contains("bringing the queen") ||
        low.contains("pressure on") ||
        low.contains("h5") ||
        low.contains("queen") ||
        low.contains("g7")
    }))
  }

  test("compensation slots avoid broken compensation skeleton fragments") {
    val fixture = BookmakerProseGoldenFixtures.exchangeSacrifice
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slots =
      BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)
        .getOrElse(fail("missing slots"))

    val rendered = (slots.claim :: slots.support).appended(slots.tension.getOrElse("")).mkString(" ").toLowerCase
    assert(!rendered.contains("while aiming for"), clue(rendered))
    assert(!rendered.contains("via queen toward"), clue(rendered))
    assert(!rendered.contains("the play still runs through"), clue(rendered))
    assert(!rendered.contains("pressure keeps building through"), clue(rendered))
  }

  test("weak compensation slots reframe to normal move-purpose instead of forcing compensation copy") {
    val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val weakPack =
      Some(
        StrategyPack(
          sideToMove = "white",
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("initiative against the king"),
              investedMaterial = Some(100),
              dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
              dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("g7")
            )
          )
        )
      )

    val slots =
      BookmakerPolishSlotsBuilder.build(ctx, outline, refs = None, strategyPack = weakPack)
        .getOrElse(fail("missing weak-compensation slots"))

    val rendered = (slots.claim :: slots.support).mkString(" ").toLowerCase
    assert(!rendered.contains("material can wait"), clue(rendered))
    assert(!rendered.contains("winning the material back"), clue(rendered))
  }

  test("decision slots prefer centered plan wording when strategy-pack surface is available") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        whyAbsentFromTopMultiPV = List("the immediate 'Qh5' thrust lets Black trade queens and kill the attack")
      )
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      BookmakerPolishSlotsBuilder.build(ctx, outline, refs = None, strategyPack = Some(surfaceDrivenPack()))
        .getOrElse(fail("missing slots"))

    val claim = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(!claim.contains("the key decision is to choose"))
    assert(!claim.contains("near the center of the plan"))
    assert(
      claim.contains("g7") ||
        slots.support.exists(text => text.toLowerCase.contains("g7")) ||
        slots.tension.exists(text => text.toLowerCase.contains("g7"))
    )
    assert(slots.support.exists(text => {
      val low = text.toLowerCase
      low.contains("g7") || low.contains("back-rank counterplay")
    }))
    assert(slots.tension.exists(text => {
      val low = text.toLowerCase
      low.contains("g7") || low.contains("decision")
    }))
  }

  test("EVA01-style tactical override cannot replace a surfaced thesis with whole-decision scaffolding") {
    val ctx = tacticalCtx(BookmakerProseGoldenFixtures.openFileFight.ctx)
    val outline =
      genericDecisionOutline(
        "The whole decision turns on b7.",
        "The practical alternative Ng5 stays secondary because the attack is not ready."
      )
    val slots =
      BookmakerPolishSlotsBuilder.build(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(
          surfaceDrivenPack(
            focusSquares = List("d6"),
            routePiece = "N",
            route = List("b1", "a3", "c2", "e3"),
            routePurpose = "plan activation lane",
            targetPiece = "N",
            targetSquare = "d6"
          )
        )
      ).getOrElse(fail("missing slots"))

    val claim = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(!claim.contains("near the center of the plan"))
  }

  test("KG01-style tactical override cannot replace a surfaced thesis with whole-decision scaffolding") {
    val ctx = tacticalCtx(BookmakerProseGoldenFixtures.openFileFight.ctx)
    val outline =
      genericDecisionOutline(
        "The whole decision turns on e5.",
        "The practical alternative Qd2 stays secondary because the attack is not ready."
      )
    val slots =
      BookmakerPolishSlotsBuilder.build(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(
          surfaceDrivenPack(
            focusSquares = List("e5", "g5"),
            routePiece = "Q",
            route = List("d1", "f3", "f5"),
            routePurpose = "kingside pressure",
            targetPiece = "N",
            targetSquare = "e5"
          )
        )
      ).getOrElse(fail("missing slots"))

    val claim = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(!claim.contains("the whole decision turns on"))
    assert(!claim.contains("near the center of the plan"))
    assert(
      claim.contains("e5") ||
        slots.support.exists(text => text.toLowerCase.contains("e5")) ||
        slots.tension.exists(text => text.toLowerCase.contains("e5"))
    )
  }

  test("CAT02-style surfaced thesis remains stable after generic-scaffold hardening") {
    val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      BookmakerPolishSlotsBuilder.build(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(
          surfaceDrivenPack(
            ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
            focusSquares = List("d4", "d5"),
            routePiece = "R",
            route = List("a8", "a6", "c6"),
            routePurpose = "open file occupation",
            targetPiece = "R",
            targetSquare = "b3"
          )
        )
      ).getOrElse(fail("missing slots"))

    val claim = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(!claim.contains("near the center of the plan"))
    assert(
      claim.contains("d4") ||
        claim.contains("b3") ||
        slots.support.exists(text => text.toLowerCase.contains("d4")) ||
        slots.support.exists(text => text.toLowerCase.contains("b3")) ||
        slots.tension.exists(text => text.toLowerCase.contains("d4"))
    )
    assert(!claim.contains("the key decision is to choose"))
  }

  test("QID02-style surfaced thesis remains stable after generic-scaffold hardening") {
    val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      BookmakerPolishSlotsBuilder.build(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(
          surfaceDrivenPack(
            focusSquares = Nil,
            routePiece = "Q",
            route = List("d7", "b5", "f5"),
            routePurpose = "plan activation lane",
            targetPiece = "Q",
            targetSquare = "d3"
          )
        )
      ).getOrElse(fail("missing slots"))

    val claim = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(!claim.contains("near the center of the plan"))
    assert(!claim.contains("the key decision is to choose"))
  }

  test("neutral truth contract suppresses compensation thesis even when raw compensation surface is present") {
    val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val rawCompensationPack =
      Some(
        surfaceDrivenPack(
          compensation = Some("pressure on g7"),
          investedMaterial = Some(100)
        )
      )

    val slots =
      BookmakerPolishSlotsBuilder.build(
        ctx,
        outline,
        refs = None,
        strategyPack = rawCompensationPack,
        truthContract =
          Some(
            truthContract(
              ownershipRole = TruthOwnershipRole.NoneRole,
              visibilityRole = TruthVisibilityRole.Hidden,
              surfaceMode = TruthSurfaceMode.Neutral
            )
          )
      ).getOrElse(fail("missing slots"))

    val rendered = (slots.claim :: slots.support).mkString(" ").toLowerCase
    assertNotEquals(slots.lens, StrategicLens.Compensation)
    assert(!rendered.contains("material can wait"), clue(rendered))
    assert(!rendered.contains("winning the material back"), clue(rendered))
  }
