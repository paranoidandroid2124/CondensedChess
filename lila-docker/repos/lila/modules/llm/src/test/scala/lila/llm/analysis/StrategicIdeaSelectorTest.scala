package lila.llm.analysis

import chess.{ Color, Square }
import chess.format.Fen
import chess.variant.Standard
import lila.llm.*
import lila.llm.model.StrategicPlanExperiment
import lila.llm.model.strategic.PositionalTag
import munit.FunSuite

class StrategicIdeaSelectorTest extends FunSuite:

  private def selectFromFen(
      fen: String,
      phase: String = "middlegame"
  ): List[StrategyIdeaSignal] =
    val board =
      Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fen,
          variations = List(lila.llm.model.strategic.VariationLine(Nil, 0, depth = 0)),
          phase = Some(phase),
          ply = 24
        )
        .getOrElse(fail(s"analysis missing for $fen"))
    val ctx = NarrativeContextBuilder.build(data, data.toContext, None)
    val semantic = StrategicIdeaSemanticContext.from(data, ctx, Some(board))
    val pack = StrategyPackBuilder.build(data, ctx).getOrElse(fail(s"strategy pack missing for $fen"))
    StrategicIdeaSelector.select(pack, semantic)

  private def noisyPack(): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      plans = List(
        StrategySidePlan(
          side = "white",
          horizon = "long",
          planName = "Build kingside clamp and space gain"
        )
      ),
      longTermFocus = List("misleading long-term focus about exchange play"),
      signalDigest = Some(
        NarrativeSignalDigest(
          structuralCue = Some("misleading space gain text"),
          latentPlan = Some("misleading favorable exchange text"),
          decision = Some("misleading prophylaxis text"),
          prophylaxisPlan = Some("misleading prevent text"),
          opponentPlan = Some("misleading counterplay text")
        )
      )
    )

  test("typed selector beats legacy keyword fallback on text-only ambiguity") {
    val pack = noisyPack()

    val typed = StrategicIdeaSelector.select(pack, StrategicIdeaSemanticContext.empty("white"))
    val legacy = LegacyStrategicIdeaTextClassifier.select(pack)

    assertEquals(typed, Nil)
    assertEquals(legacy.headOption.map(_.kind), Some(StrategicIdeaKind.SpaceGainOrRestriction))
  }

  test("source registry keeps prose-only fields out of authoritative selector inputs") {
    assert(StrategicIdeaSourceRegistry.authoritative.contains("pawn_analysis"))
    assert(StrategicIdeaSourceRegistry.derivedTyped.contains("classification"))
    assert(StrategicIdeaSourceRegistry.proseOnly.contains("plan_name"))
    assert(!StrategicIdeaSourceRegistry.authoritative.contains("route_purpose"))
  }

  test("refuted experiment blocks matching king-attack idea") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.MateNet(Color.White)),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "flank_attack",
            themeL1 = ThemeTaxonomy.ThemeL1.FlankInfrastructure.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.HookCreation.id),
            evidenceTier = "refuted",
            refuteProbeCount = 1,
            moveOrderSensitive = true,
            experimentConfidence = 0.08
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assertEquals(ideas, Nil)
  }

  test("pv-coupled move-order-sensitive experiment demotes but does not erase slow space idea") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.SpaceAdvantage(Color.White)),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "space_clamp",
            themeL1 = ThemeTaxonomy.ThemeL1.SpaceClamp.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.CentralSpaceBind.id),
            evidenceTier = "pv_coupled",
            moveOrderSensitive = true,
            experimentConfidence = 0.52
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.SpaceGainOrRestriction))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("experiment:space_clamp")))
  }

  test("evidence-backed experiment adds experiment refs to surviving outpost idea") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.Outpost(Square.E5, Color.White)),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "outpost_plan",
            themeL1 = ThemeTaxonomy.ThemeL1.PieceRedeployment.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.OutpostEntrenchment.id),
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            experimentConfidence = 0.91
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.OutpostCreationOrOccupation))
    assert(ideas.headOption.exists(_.evidenceRefs.exists(_ == "experiment:piece_redeployment")))
    assert(ideas.headOption.exists(_.evidenceRefs.exists(_ == "experiment_subplan:outpost_entrenchment")))
  }

  test("family-first staging keeps concrete pawn break ahead of slow structural noise") {
    val ideas =
      selectFromFen("r2qk2r/pp1bbppp/2n1p3/3pPn2/NP1P4/P2B1N2/1B3PPP/R2QK2R b KQkq - 8 12")

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.PawnBreak))
  }

  test("family-first staging keeps IQP trade-down windows in the conversion family") {
    val ideas =
      selectFromFen("r1bqr1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12")

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.FavorableTradeOrTransformation))
  }
