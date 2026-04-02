package lila.llm

import munit.FunSuite
import play.api.libs.json.{ JsObject, Json }

import lila.llm.model.StrategicPlanExperiment
import lila.llm.model.authoring.*
import lila.strategicPuzzle.StrategicPuzzle.*

class UserFacingPayloadSanitizerTest extends FunSuite:

  private val bannedPhrases = List(
    "PlayableByPV",
    "PlayedPV",
    "strict evidence mode",
    "probe evidence pending",
    "engine-coupled continuation",
    "theme:",
    "subplan:",
    "support:",
    "seed:",
    "{seed}",
    "{us}",
    "{them}",
    "return vector",
    "cash out"
  )

  private def assertNoLeaks(text: String): Unit =
    bannedPhrases.foreach { phrase =>
      assert(!text.toLowerCase.contains(phrase.toLowerCase), clue(text))
    }

  test("sanitizes bookmaker response across structured user-facing fields") {
    val response =
      CommentResponse(
        commentary =
          "Piece activation is deferred as PlayableByPV under strict evidence mode, and the return vector only holds if the line pressure cash out works.",
        concepts = List("PlayableByPV", "return vector"),
        authorQuestions = List(
          AuthorQuestionSummary(
            id = "q1",
            kind = "probe",
            priority = 1,
            question = "Does {seed} still work?",
            why = Some("supported by engine-coupled continuation"),
            anchors = List("theme:piece_redeployment"),
            confidence = "medium",
            latentPlanName = Some("PlayedPV follow-up")
          )
        ),
        authorEvidence = List(
          AuthorEvidenceSummary(
            questionId = "q1",
            questionKind = "probe",
            question = "Does the plan cash out?",
            why = Some("probe evidence pending"),
            status = "pending",
            purposes = List("theme:piece_redeployment"),
            probeObjectives = List("return vector"),
            linkedPlans = List("seed:pawnstorm_kingside")
          )
        ),
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "PieceActivation",
            planName = "PlayableByPV plan",
            rank = 1,
            score = 0.7,
            preconditions = List("theme:piece_redeployment", "{seed}"),
            executionSteps = List("cash out through c-file pressure"),
            failureModes = List("probe evidence pending"),
            viability = PlanViability(score = 0.7, label = "medium", risk = "return vector collapse"),
            refutation = Some("engine-coupled continuation fails"),
            evidenceSources = List("probe_backed:validated_support")
          )
        ),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "PieceActivation",
            themeL1 = "piece_redeployment",
            subplanId = Some("rook_lift_scaffold"),
            evidenceTier = "pv_coupled",
            supportProbeCount = 0,
            refuteProbeCount = 0,
            bestReplyStable = false,
            futureSnapshotAligned = false,
            counterBreakNeutralized = false,
            moveOrderSensitive = true,
            experimentConfidence = 0.52
          )
        ),
        strategyPack = Some(
          StrategyPack(
            sideToMove = "white",
            plans = List(
              StrategySidePlan(
                side = "white",
                horizon = "long",
                planName = "PlayableByPV plan",
                priorities = List("cash out"),
                riskTriggers = List("probe evidence pending")
              )
            ),
            longTermFocus = List("cash out only after return vector proves itself"),
            evidence = List("theme:piece_redeployment", "support:engine_hypothesis"),
            signalDigest = Some(
              NarrativeSignalDigest(
                strategicStack = List("PlayableByPV continuation"),
                latentPlan = Some("seed:pawnstorm_kingside"),
                latentReason = Some("probe evidence pending"),
                decisionComparison = Some(
                  DecisionComparisonDigest(
                    deferredMove = Some("Qe3"),
                    deferredReason = Some("accepted as PlayableByPV fallback"),
                    deferredSource = Some("engine-coupled continuation"),
                    evidence = Some("return vector through line pressure")
                  )
                ),
                authoringEvidence = Some("theme:piece_redeployment"),
                practicalVerdict = Some("cash out"),
                practicalFactors = List("return vector"),
                compensation = Some("return vector through line pressure and delayed recovery"),
                compensationVectors = List("Return Vector (0.5)", "Line Pressure (0.7)", "Delayed Recovery (0.4)")
              )
            )
          )
        ),
        bookmakerLedger = Some(
          BookmakerStrategicLedgerV1(
            motifKey = "attack",
            motifLabel = "PlayableByPV attack",
            stageKey = "build",
            stageLabel = "cash out",
            carryOver = false,
            stageReason = Some("probe evidence pending"),
            prerequisites = List("theme:piece_redeployment"),
            conversionTrigger = Some("return vector"),
            primaryLine = Some(
              BookmakerLedgerLineV1(
                title = "PlayedPV line",
                note = Some("cash out through initiative"),
                source = "rule"
              )
            )
          )
        )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val rendered =
      List(
        sanitized.commentary,
        sanitized.concepts.mkString(" "),
        sanitized.authorQuestions
          .flatMap(q => q.why.toList ++ List(q.question) ++ q.anchors ++ q.latentPlanName.toList)
          .mkString(" "),
        sanitized.authorEvidence
          .flatMap(e => e.why.toList ++ List(e.question) ++ e.purposes ++ e.probeObjectives ++ e.linkedPlans)
          .mkString(" "),
        sanitized.mainStrategicPlans
          .flatMap(p =>
            p.preconditions ++ p.executionSteps ++ p.failureModes ++ List(p.planName, p.viability.risk) ++ p.refutation.toList
          )
          .mkString(" "),
        sanitized.strategicPlanExperiments.map(_.themeL1).mkString(" "),
        sanitized.strategyPack.toList.flatMap(pack =>
          pack.longTermFocus ++
            pack.evidence ++
            pack.signalDigest.toList.flatMap(d =>
              d.strategicStack ++
                d.practicalFactors ++
                d.compensation.toList ++
                d.compensationVectors ++
                d.authoringEvidence.toList ++
                d.practicalVerdict.toList ++
                d.latentPlan.toList ++
                d.latentReason.toList ++
                d.decisionComparison.toList.flatMap(dc => dc.deferredReason.toList ++ dc.deferredSource.toList ++ dc.evidence.toList)
            )
        ).mkString(" "),
        sanitized.bookmakerLedger.toList.flatMap(ledger =>
          ledger.stageReason.toList ++
            ledger.prerequisites ++
            ledger.conversionTrigger.toList ++
            ledger.primaryLine.toList.flatMap(line => line.note.toList ++ List(line.title)) ++
            List(ledger.motifLabel, ledger.stageLabel)
        ).mkString(" ")
      ).mkString(" ")

    val json = Json.toJson(sanitized).as[JsObject]

    assertNoLeaks(rendered)
    assertEquals(json.keys.contains("latentPlans"), false, clue(json))
    assertEquals(json.keys.contains("whyAbsentFromTopMultiPV"), false, clue(json))
    assertEquals(sanitized.signalDigest.flatMap(_.latentPlan), None, clue(sanitized.signalDigest))
    assertEquals(sanitized.signalDigest.flatMap(_.latentReason), None, clue(sanitized.signalDigest))
    assertEquals(sanitized.signalDigest.flatMap(_.opponentPlan), None, clue(sanitized.signalDigest))
    assertEquals(sanitized.signalDigest.flatMap(_.dominantIdeaKind), None, clue(sanitized.signalDigest))
    assertEquals(sanitized.signalDigest.flatMap(_.decision), None, clue(sanitized.signalDigest))
    assertEquals(sanitized.mainStrategicPlans.size, 1, clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.strategicPlanExperiments.size, 1, clue(sanitized.strategicPlanExperiments))
    assert(rendered.toLowerCase.contains("pays off") || rendered.toLowerCase.contains("engine-backed"), clue(rendered))
  }

  test("sanitizes game chronicle moments without dropping strategic plan experiment metadata") {
    val response =
      GameChronicleResponse(
        schema = "chesstory.game_chronicle.v2",
        intro = "PlayableByPV intro",
        moments = List(
          GameChronicleMoment(
            momentId = "m1",
            ply = 19,
            moveNumber = 10,
            side = "white",
            moveClassification = Some("Critical"),
            momentType = "Key",
            fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
            narrative = "PlayableByPV bridge with return vector.",
            concepts = List("PlayableByPV"),
            variations = Nil,
            cpBefore = 12,
            cpAfter = 45,
            mateBefore = None,
            mateAfter = None,
            wpaSwing = None,
            strategicSalience = None,
            transitionType = None,
            transitionConfidence = None,
            activePlan = None,
            topEngineMove = None,
            collapse = None,
            mainStrategicPlans = List(
              PlanHypothesis(
                planId = "king_attack",
                planName = "PlayableByPV attack",
                rank = 1,
                score = 0.8,
                preconditions = Nil,
                executionSteps = Nil,
                failureModes = Nil,
                viability = PlanViability(0.8, "high", "cash out"),
                themeL1 = "king_attack",
                subplanId = Some("rook_lift_scaffold"),
                evidenceSources = List("probe_backed:validated_support")
              )
            ),
            strategicPlanExperiments = List(
              StrategicPlanExperiment(
                planId = "king_attack",
                themeL1 = "king_attack",
                subplanId = Some("rook_lift_scaffold"),
                evidenceTier = "evidence_backed",
                supportProbeCount = 2,
                refuteProbeCount = 0,
                bestReplyStable = true,
                futureSnapshotAligned = true,
                counterBreakNeutralized = true,
                moveOrderSensitive = false,
                experimentConfidence = 0.81
              )
            ),
            signalDigest = Some(
              NarrativeSignalDigest(
                dominantIdeaKind = Some("pawn_break"),
                dominantIdeaReadiness = Some("build"),
                dominantIdeaFocus = Some("d5, e4"),
                opponentPlan = Some("Preparing the d-break"),
                preservedSignals = List("opponent_plan", "authoring_evidence")
              )
            )
          )
        ),
        conclusion = "PlayedPV closeout",
        themes = List("PlayableByPV"),
        review = None
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    assertEquals(
      sanitized.moments.head.strategicPlanExperiments.map(_.evidenceTier),
      List("evidence_backed"),
      clue(sanitized.moments.head.strategicPlanExperiments)
    )
    assertEquals(
      sanitized.moments.head.strategicPlanExperiments.headOption.flatMap(_.subplanId),
      Some("rook_lift_scaffold"),
      clue(sanitized.moments.head.strategicPlanExperiments)
    )
    val momentJson = Json.toJson(sanitized.moments.head).as[JsObject]
    assertEquals(sanitized.moments.head.concepts, Nil, clue(sanitized.moments.head))
    assertEquals(momentJson.keys.contains("latentPlans"), false, clue(momentJson))
    assertEquals(momentJson.keys.contains("whyAbsentFromTopMultiPV"), false, clue(momentJson))
    assertEquals(sanitized.moments.head.signalDigest.flatMap(_.opponentPlan), None, clue(sanitized.moments.head.signalDigest))
    assertEquals(sanitized.moments.head.signalDigest.flatMap(_.dominantIdeaKind), None, clue(sanitized.moments.head.signalDigest))
    assertEquals(sanitized.themes, Nil, clue(sanitized))
    assertEquals(sanitized.strategicThreads, Nil, clue(sanitized))
  }

  test("humanizes raw plan label families in user-facing payloads") {
    val response =
      CommentResponse(
        commentary = "Preparing e-break Break.",
        concepts = Nil,
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "break",
            planName = "Preparing e-break Break",
            rank = 1,
            score = 0.66,
            preconditions = Nil,
            executionSteps = List("Piece Activation"),
            failureModes = List("Opening Development and Center Control"),
            viability = PlanViability(0.66, "medium", "Exploiting Space Advantage"),
            refutation = Some("Simplification into Endgame"),
            evidenceSources = List("probe_backed:validated_support")
          )
        ),
        signalDigest = Some(
          NarrativeSignalDigest(
            latentPlan = Some("Preparing d-break Break"),
            latentReason = Some("Simplification into Endgame")
          )
        )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(sanitized.mainStrategicPlans.map(_.planName), List("Preparing the e-break"), clue(sanitized))
    assertEquals(
      sanitized.mainStrategicPlans.flatMap(_.executionSteps),
      List("Improving piece placement"),
      clue(sanitized)
    )
    assertEquals(
      sanitized.mainStrategicPlans.flatMap(_.failureModes),
      List("Development and central control"),
      clue(sanitized)
    )
    assertEquals(
      sanitized.mainStrategicPlans.map(_.viability.risk),
      List("Using the space advantage"),
      clue(sanitized)
    )
    assertEquals(
      sanitized.mainStrategicPlans.flatMap(_.refutation),
      List("Simplifying toward an endgame"),
      clue(sanitized)
    )
    assertEquals(sanitized.signalDigest.flatMap(_.latentPlan), None, clue(sanitized))
    assertEquals(sanitized.signalDigest.flatMap(_.latentReason), None, clue(sanitized))
  }

  test("sanitizes active strategic route surfaces in chronicle payloads") {
    val response =
      GameChronicleResponse(
        schema = GameChronicleResponse.schemaV6,
        intro = "Intro",
        moments =
          List(
            GameChronicleMoment(
              momentId = "ply_20",
              ply = 20,
              moveNumber = 10,
              side = "black",
              moveClassification = None,
              momentType = "SustainedPressure",
              fen = "4k3/8/8/8/8/8/8/4K3 b - - 0 1",
              narrative = "Narrative",
              concepts = Nil,
              variations = Nil,
              cpBefore = 0,
              cpAfter = 0,
              mateBefore = None,
              mateAfter = None,
              wpaSwing = None,
              strategicSalience = Some("High"),
              transitionType = None,
              transitionConfidence = None,
              activePlan = None,
              topEngineMove = None,
              collapse = None,
              activeStrategicRoutes =
                List(
                  ActiveStrategicRouteRef(
                    routeId = "route_1",
                    ownerSide = "black",
                    piece = "R",
                    route = List("a8", "b8", "b4"),
                    purpose = "PlayableByPV cash out on the b-file",
                    strategicFit = 0.8,
                    tacticalSafety = 0.8,
                    surfaceConfidence = 0.8,
                    surfaceMode = RouteSurfaceMode.Toward
                  )
                )
            )
          ),
        conclusion = "Conclusion",
        themes = Nil
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val routePurpose = sanitized.moments.head.activeStrategicRoutes.headOption.map(_.purpose).getOrElse("")

    assert(routePurpose.nonEmpty, clue(sanitized))
    assertNoLeaks(routePurpose)
  }

  test("sanitizes stored strategic puzzle runtime shell on read") {
    val shell =
      RuntimeShell(
        schema = RuntimeShellSchema,
        startFen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        sideToMove = "white",
        prompt = "PlayableByPV prompt with return vector and cash out",
        plans = List(
          PuzzlePlan(
            id = "plan_attack",
            familyKey = Some("king_attack_build_up|h7"),
            dominantIdeaKind = Some("king_attack_build_up"),
            anchor = Some("{them}"),
            task = "cash out the compensation before the return vector lands",
            feedback = "engine-coupled continuation and {seed}",
            allowedStarts =
              List(
                PlanStart(
                  uci = "e2e4",
                  san = "e4",
                  credit = "full",
                  label = Some("PlayedPV label"),
                  feedback = "engine-coupled continuation and {seed}",
                  afterFen = None,
                  terminalId = Some("t1")
                )
              ),
            featuredTerminalId = "t1",
            featuredStartUci = Some("e2e4")
          )
        ),
        proof = RuntimeProofLayer(
          rootChoices = List(
            ShellChoice(
              uci = "e2e4",
              san = "e4",
              credit = "full",
              nextNodeId = Some("n1"),
              terminalId = None,
              afterFen = None,
              familyKey = None,
              label = Some("PlayedPV label"),
              feedback = "engine-coupled continuation and {seed}"
            )
          ),
          nodes = List(
            PlayerNode(
              id = "n1",
              step = 1,
              fen = "4k3/8/8/8/8/8/8/4K3 b - - 0 1",
              prompt = "probe evidence pending",
              badMoveFeedback = "return vector through line pressure",
              choices = Nil
            )
          ),
          forcedReplies = Nil
        ),
        terminals = List(
          TerminalReveal(
            id = "t1",
            outcome = "full",
            title = "PlayableByPV title",
            summary = "cash out the compensation",
            commentary = "The return vector only holds if delayed recovery keeps the line pressure alive.",
            familyKey = None,
            dominantIdeaKind = None,
            anchor = Some("{us}"),
            lineSan = Nil,
            siblingMoves = Nil,
            opening = Some("theme:piece_redeployment"),
            eco = None,
            dominantFamilyKey = None,
            planId = Some("plan_attack"),
            planTask = Some("cash out the compensation before the return vector lands"),
            whyPlan = Some("engine-coupled continuation and {seed}"),
            whyMove = Some("The return vector only holds if delayed recovery keeps the line pressure alive."),
            acceptedStarts = List("e4", "Qe2"),
            featuredStart = Some("e4")
          )
        )
      )

    val payload =
      BootstrapPayload(
        puzzle =
          StrategicPuzzleDoc(
            id = "sp1",
            schema = "chesstory.strategicPuzzle.v1",
            source = SourcePayload(seedId = "seed", opening = None, eco = None),
            position = PositionPayload(fen = shell.startFen, sideToMove = shell.sideToMove),
            dominantFamily = Some(DominantFamilySummary(key = "attack", dominantIdeaKind = "king_attack_build_up", anchor = "cash out")),
            qualityScore = QualityScore(total = 90),
            generationMeta = GenerationMeta(selectionStatus = PublicSelectionStatus),
            runtimeShell = Some(shell)
          ),
        runtimeShell = shell,
        progress = ProgressPayload(authenticated = false, currentStreak = 0, recentAttempts = Nil)
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(payload)
    val rendered =
      List(
        sanitized.runtimeShell.prompt,
        sanitized.runtimeShell.plans.flatMap(plan => List(plan.task, plan.feedback) ++ plan.anchor.toList ++ plan.allowedStarts.flatMap(start => start.label.toList :+ start.feedback)).mkString(" "),
        sanitized.runtimeShell.proof.rootChoices.flatMap(choice => choice.label.toList :+ choice.feedback).mkString(" "),
        sanitized.runtimeShell.proof.nodes.flatMap(node => List(node.prompt, node.badMoveFeedback)).mkString(" "),
        sanitized.runtimeShell.terminals.flatMap(reveal => List(reveal.title, reveal.summary, reveal.commentary) ++ reveal.anchor.toList ++ reveal.opening.toList ++ reveal.planTask.toList ++ reveal.whyPlan.toList ++ reveal.whyMove.toList ++ reveal.acceptedStarts ++ reveal.featuredStart.toList).mkString(" "),
        sanitized.puzzle.dominantFamily.toList.map(_.anchor).mkString(" ")
      ).mkString(" ")

    assertNoLeaks(rendered)
    assert(rendered.toLowerCase.contains("pays off") || rendered.toLowerCase.contains("current engine line"), clue(rendered))
  }
