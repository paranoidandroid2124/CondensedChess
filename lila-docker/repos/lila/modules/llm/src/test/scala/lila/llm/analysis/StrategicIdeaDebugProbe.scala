package lila.llm.analysis

import lila.llm.model.strategic.VariationLine

object StrategicIdeaDebugProbe:

  @main def run(id: String = "K01", overrideSideToMove: String = ""): Unit =
    val fixtures =
      if id == "all" then StrategicIdeaFenFixtures.all
      else StrategicIdeaFenFixtures.all.filter(_.id == id)
    if fixtures.isEmpty then sys.error(s"unknown fixture: $id")

    fixtures.foreach { fixture =>
      val fen =
        if overrideSideToMove == "w" || overrideSideToMove == "b" then
          fixture.fen.split(" ").toList match
            case placement :: _ :: castling :: ep :: halfmove :: fullmove :: Nil =>
              s"$placement $overrideSideToMove $castling $ep $halfmove $fullmove"
            case _ => fixture.fen
        else fixture.fen

      val data =
        CommentaryEngine
          .assessExtended(
            fen = fen,
            variations = List(VariationLine(Nil, 0, depth = 0)),
            phase = Some(fixture.phase),
            ply = 24
          )
          .getOrElse(sys.error("analysis missing"))

      val ctx = NarrativeContextBuilder.build(data, data.toContext, None)
      val semantic = StrategicIdeaSemanticContext.from(data, ctx, None)
      val pack = StrategyPackBuilder.build(data, ctx).getOrElse(sys.error("pack missing"))

      println(s"${fixture.id} ${fixture.label}")
      println(s"fen=$fen")
      println(s"goal=${fixture.expectedDominant}")
      println(s"ideas=${pack.strategicIdeas.map(i => s"${i.kind}:${i.confidence}").mkString(", ")}")
      println(s"pawnAnalysis=${data.toContext.pawnAnalysis}")
      println(s"opponentPawnAnalysis=${data.toContext.opponentPawnAnalysis}")
      println(s"threatsToUs=${data.toContext.threatsToUs}")
      println(s"threatsToThem=${data.toContext.threatsToThem}")
      println(s"planAlignment=${data.toContext.planAlignment.map(_.reasonCodes)}")
      println(
        s"structurProfile=${data.structureProfile.map(p => s"${p.primary}:${p.centerState}:${p.evidenceCodes.mkString(",")}")}"
      )
      println(s"positionalFeatures=${data.positionalFeatures.mkString(" | ")}")
      println(s"structuralWeaknesses=${data.structuralWeaknesses.mkString(" | ")}")
      println(s"preventedPlans=${data.preventedPlans.mkString(" | ")}")
      println(s"routes=${pack.pieceRoutes.mkString(" | ")}")
      println(s"directionalTargets=${pack.directionalTargets.mkString(" | ")}")
      println(s"signalDigest=${pack.signalDigest}")
      println(s"semanticPlanAlignmentReasonCodes=${semantic.planAlignmentReasonCodes.mkString(",")}")
      println()
    }
