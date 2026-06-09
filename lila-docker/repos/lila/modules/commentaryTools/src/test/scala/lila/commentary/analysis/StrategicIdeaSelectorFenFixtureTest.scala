package lila.commentary.analysis

import _root_.chess.{ Bishop, Board, Color, Knight, Pawn, Queen, Rook }
import _root_.chess.format.Fen
import _root_.chess.variant.Standard

import lila.commentary.*
import lila.commentary.analysis.StrategicIdeaFenFixtures.ProducerCheck
import lila.commentary.model.Motif
import lila.commentary.model.strategic.{ PositionalTag, VariationLine }
import munit.FunSuite

class StrategicIdeaSelectorFenFixtureTest extends FunSuite:

  private final case class EvaluatedFixture(
      fixture: StrategicIdeaFenFixtures.Fixture,
      board: Board,
      semantic: StrategicIdeaSemanticContext,
      pack: StrategyPack
  ):
    def actualIdeas: List[StrategyIdeaSignal] = pack.strategicIdeas
    def dominantIdea: StrategyIdeaSignal =
      actualIdeas.headOption.getOrElse(fail(s"missing dominant idea for ${fixture.id} ${fixture.label}"))
    def actualIdeaKinds: List[String] = actualIdeas.map(_.kind)

  private lazy val evaluatedFixtures: List[EvaluatedFixture] =
    StrategicIdeaFenFixtures.all.map(evaluate)

  private lazy val evaluatedById: Map[String, EvaluatedFixture] =
    evaluatedFixtures.map(fx => fx.fixture.id -> fx).toMap

  private def evaluate(fixture: StrategicIdeaFenFixtures.Fixture): EvaluatedFixture =
    val board =
      Fen.read(Standard, Fen.Full(fixture.fen)).map(_.board).getOrElse(fail(s"invalid FEN: ${fixture.id} ${fixture.fen}"))

    val data =
      CommentaryEngine
        .assessExtended(
          fen = fixture.fen,
          variations = List(VariationLine(Nil, 0, depth = 0)),
          phase = Some(fixture.phase),
          ply = 24
        )
        .getOrElse(fail(s"analysis missing for ${fixture.id} ${fixture.label}"))

    val ctx = NarrativeContextBuilder.build(data, data.toContext, None)
    val semantic = StrategicIdeaSemanticContext.from(data, ctx, Some(board))
    val pack = StrategyPackBuilder.build(data, ctx).getOrElse(fail(s"strategy pack missing for ${fixture.id} ${fixture.label}"))

    EvaluatedFixture(fixture = fixture, board = board, semantic = semantic, pack = pack)

  private def materialValueDiff(board: Board): Int =
    def score(color: Color): Int =
      board.byPiece(color, Pawn).count +
        board.byPiece(color, Knight).count * 3 +
        board.byPiece(color, Bishop).count * 3 +
        board.byPiece(color, Rook).count * 5 +
        board.byPiece(color, Queen).count * 9

    score(Color.White) - score(Color.Black)

  private def materialSummary(board: Board, color: Color): String =
    s"Q${board.byPiece(color, Queen).count} R${board.byPiece(color, Rook).count} " +
      s"B${board.byPiece(color, Bishop).count} N${board.byPiece(color, Knight).count} P${board.byPiece(color, Pawn).count}"

  private def compensationSideHasMaterialDeficit(fixture: StrategicIdeaFenFixtures.Fixture, diff: Int): Boolean =
    fixture.compensationSide.forall {
      case "white" => diff < 0
      case "black" => diff > 0
      case _       => false
    }

  private def misleadingPack(base: StrategyPack): StrategyPack =
    base.copy(
      plans = List(
        StrategySidePlan(
          side = base.sideToMove,
          horizon = "long",
          planName = "Misleading kingside attack",
          priorities = List("trade queens"),
          riskTriggers = List("avoid the outpost story")
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
      ),
      strategicIdeas = Nil
    )

  private def activeColor(side: String): Color =
    if side == "white" then Color.White else Color.Black

  private def opponentColor(side: String): Color =
    !activeColor(side)

  private def sameSide(color: Color, side: String): Boolean =
    color == activeColor(side)

  private def spaceDiffForActiveSide(evaluated: EvaluatedFixture): Int =
    evaluated.semantic.positionFeatures.fold(0)(features =>
      if evaluated.semantic.sideToMove == "white" then features.centralSpace.spaceDiff
      else -features.centralSpace.spaceDiff
    )

  private def clampForActiveSide(evaluated: EvaluatedFixture): Boolean =
    evaluated.semantic.strategicState.exists { state =>
      if evaluated.semantic.sideToMove == "white" then state.whiteColorComplexClamp
      else state.blackColorComplexClamp
    }

  private def kindSummary(ideas: List[StrategyIdeaSignal]): String =
    ideas
      .map(idea =>
        s"${idea.kind}:${idea.confidence}[${idea.evidenceRefs.mkString("|")}]"
      )
      .mkString(", ")

  private def hasPiece(board: Board, color: Color, square: _root_.chess.Square, role: _root_.chess.Role): Boolean =
    board.pieceAt(square).exists(piece => piece.color == color && piece.role == role)

  private def pawnAt(board: Board, color: Color, square: _root_.chess.Square): Boolean =
    hasPiece(board, color, square, Pawn)

  private def diagonalClear(board: Board, from: _root_.chess.Square, to: _root_.chess.Square): Boolean =
    val fileStep = math.signum(to.file.value - from.file.value)
    val rankStep = math.signum(to.rank.value - from.rank.value)
    val fileDiff = (to.file.value - from.file.value).abs
    val rankDiff = (to.rank.value - from.rank.value).abs
    if fileDiff != rankDiff || fileDiff == 0 then false
    else
      (1 until fileDiff).forall { offset =>
        _root_.chess.Square
          .at(from.file.value + offset * fileStep, from.rank.value + offset * rankStep)
          .forall(board.pieceAt(_).isEmpty)
      }

  private def hasBishopPinWatch(evaluated: EvaluatedFixture): Boolean =
    evaluated.semantic.board.exists { board =>
      if evaluated.semantic.sideToMove == "white" then
        hasPiece(board, Color.White, _root_.chess.Square.F3, Knight) &&
        hasPiece(board, Color.Black, _root_.chess.Square.C8, Bishop) &&
        diagonalClear(board, _root_.chess.Square.C8, _root_.chess.Square.G4)
      else
        hasPiece(board, Color.Black, _root_.chess.Square.F6, Knight) &&
        hasPiece(board, Color.White, _root_.chess.Square.C1, Bishop) &&
        diagonalClear(board, _root_.chess.Square.C1, _root_.chess.Square.G5)
    }

  private def hasQueensideClampWatch(evaluated: EvaluatedFixture): Boolean =
    evaluated.semantic.board.exists { board =>
      if evaluated.semantic.sideToMove == "white" then
        pawnAt(board, Color.White, _root_.chess.Square.C4) &&
        pawnAt(board, Color.White, _root_.chess.Square.D5) &&
        pawnAt(board, Color.White, _root_.chess.Square.E4) &&
        pawnAt(board, Color.Black, _root_.chess.Square.D6) &&
        pawnAt(board, Color.Black, _root_.chess.Square.E5) &&
        pawnAt(board, Color.Black, _root_.chess.Square.G6) &&
        pawnAt(board, Color.Black, _root_.chess.Square.B7)
      else
        pawnAt(board, Color.Black, _root_.chess.Square.C5) &&
        pawnAt(board, Color.Black, _root_.chess.Square.D4) &&
        pawnAt(board, Color.Black, _root_.chess.Square.E5) &&
        pawnAt(board, Color.White, _root_.chess.Square.D3) &&
        pawnAt(board, Color.White, _root_.chess.Square.E4) &&
        pawnAt(board, Color.White, _root_.chess.Square.G3) &&
        pawnAt(board, Color.White, _root_.chess.Square.B2)
    }

  private def hasFrenchF6BreakSeed(evaluated: EvaluatedFixture): Boolean =
    evaluated.semantic.board.exists { board =>
      evaluated.semantic.sideToMove == "black" &&
      evaluated.semantic.structureProfile.exists(_.primary == lila.commentary.model.structure.StructureId.FrenchAdvanceChain) &&
      pawnAt(board, Color.Black, _root_.chess.Square.F7) &&
      pawnAt(board, Color.Black, _root_.chess.Square.E6) &&
      pawnAt(board, Color.Black, _root_.chess.Square.D5) &&
      pawnAt(board, Color.White, _root_.chess.Square.E5)
    }

  private def hasHedgehogBreakDenialGeometry(evaluated: EvaluatedFixture): Boolean =
    evaluated.semantic.board.exists { board =>
      evaluated.semantic.sideToMove == "white" &&
      evaluated.semantic.structureProfile.exists(_.primary == lila.commentary.model.structure.StructureId.Hedgehog) &&
      pawnAt(board, Color.White, _root_.chess.Square.C4) &&
      pawnAt(board, Color.Black, _root_.chess.Square.A6) &&
      pawnAt(board, Color.Black, _root_.chess.Square.B6) &&
      pawnAt(board, Color.Black, _root_.chess.Square.D6)
    }

  private def hasMaroczyBreakDenialGeometry(evaluated: EvaluatedFixture): Boolean =
    evaluated.semantic.board.exists { board =>
      evaluated.semantic.sideToMove == "white" &&
      evaluated.semantic.structureProfile.exists(_.primary == lila.commentary.model.structure.StructureId.MaroczyBind) &&
      pawnAt(board, Color.White, _root_.chess.Square.C4) &&
      pawnAt(board, Color.White, _root_.chess.Square.E4) &&
      pawnAt(board, Color.Black, _root_.chess.Square.C6) &&
      pawnAt(board, Color.Black, _root_.chess.Square.D6)
    }

  private def producerSummary(evaluated: EvaluatedFixture): String =
    val semantic = evaluated.semantic
    val profile = semantic.structureProfile.map(_.primary.toString).getOrElse("None")
    val pawnAnalysis =
      semantic.pawnAnalysis.map(analysis =>
        s"breakReady=${analysis.pawnBreakReady}, breakFile=${analysis.breakFile.getOrElse("-")}, " +
          s"advanceOrCapture=${analysis.advanceOrCapture}, counterBreak=${analysis.counterBreak}, " +
          s"tension=${analysis.tensionSquares.mkString("[", ",", "]")}"
      ).getOrElse("None")
    val threatsToUs =
      semantic.threatsToUs.map(threats =>
        s"prophylaxisNeeded=${threats.prophylaxisNeeded}, strategic=${threats.strategicThreat}, " +
          s"immediate=${threats.immediateThreat}, maxLoss=${threats.maxLossIfIgnored}"
      ).getOrElse("None")
    val threatsToThem =
      semantic.threatsToThem.map(threats =>
        s"strategic=${threats.strategicThreat}, immediate=${threats.immediateThreat}, " +
          s"maxLoss=${threats.maxLossIfIgnored}, driver=${threats.primaryDriver}"
      ).getOrElse("None")
    val classification =
      semantic.classification.map(c =>
        s"simplify=${c.simplifyBias.shouldSimplify}, convert=${c.taskMode.isConvertMode}, task=${c.taskMode.primaryDriver}"
      ).getOrElse("None")
    val prevented =
      semantic.preventedPlans.map(plan =>
        s"${plan.planId}(break=${plan.breakNeutralized.getOrElse("-")}, denied=${plan.deniedSquares.map(_.key).mkString("[", ",", "]")}, drop=${plan.counterplayScoreDrop})"
      ).mkString("[", ", ", "]")
    val experiments =
      semantic.strategicPlanExperiments
        .map(experiment =>
          s"${experiment.themeL1}/${experiment.subplanId.getOrElse("-")}:" +
            s"${experiment.evidenceTier},stable=${experiment.bestReplyStable}," +
            s"future=${experiment.futureSnapshotAligned},neutralized=${experiment.counterBreakNeutralized}," +
            s"moveOrder=${experiment.moveOrderSensitive},support=${experiment.supportProbeCount}," +
            s"refute=${experiment.refuteProbeCount}"
        )
        .mkString("[", ", ", "]")
    val routes =
      evaluated.pack.pieceRoutes.map(route => s"${route.piece}:${route.surfaceMode}:${route.route.mkString("->")}").mkString("[", ", ", "]")
    val targets =
      evaluated.pack.directionalTargets.map(target => s"${target.piece}:${target.targetSquare}:${target.readiness}").mkString("[", ", ", "]")
    s"profile=$profile; pawnAnalysis=$pawnAnalysis; threatsToUs=$threatsToUs; threatsToThem=$threatsToThem; " +
      s"classification=$classification; prevented=$prevented; experiments=$experiments; routes=$routes; targets=$targets; ideas=${kindSummary(evaluated.actualIdeas)}"

  private def checkProducerSignal(evaluated: EvaluatedFixture, check: ProducerCheck): Boolean =
    val semantic = evaluated.semantic
    val side = semantic.sideToMove
    val them = opponentColor(side)

    check match
      case ProducerCheck.StructureProfileIs(id) =>
        semantic.structureProfile.exists(_.primary == id)
      case ProducerCheck.BreakCandidate =>
        semantic.pawnAnalysis.exists(analysis =>
          analysis.pawnBreakReady ||
            analysis.advanceOrCapture ||
            analysis.breakFile.exists(_.nonEmpty) ||
            analysis.tensionSquares.nonEmpty
        ) ||
          hasFrenchF6BreakSeed(evaluated)
      case ProducerCheck.SpaceRestriction =>
        semantic.positionalFeatures.exists {
          case PositionalTag.SpaceAdvantage(color) => sameSide(color, side)
          case _                                   => false
        } ||
          spaceDiffForActiveSide(evaluated) > 0 ||
          clampForActiveSide(evaluated) ||
          semantic.structureProfile.exists(profile =>
            (profile.primary == lila.commentary.model.structure.StructureId.MaroczyBind && side == "white") ||
              (profile.primary == lila.commentary.model.structure.StructureId.IQPWhite &&
                side == "white" &&
                semantic.board.exists(board => pawnAt(board, Color.White, _root_.chess.Square.D4)))
          )
      case ProducerCheck.WeakSquareOrWeakComplex =>
        semantic.positionalFeatures.exists {
          case PositionalTag.WeakSquare(_, color) => color == them
          case PositionalTag.ColorComplexWeakness(color, _, _) => color == them
          case _ => false
        } ||
          semantic.structuralWeaknesses.exists(weakness => weakness.color == them && weakness.squares.nonEmpty)
      case ProducerCheck.LineAccess =>
        semantic.positionalFeatures.exists {
          case PositionalTag.OpenFile(_, color)       => sameSide(color, side)
          case PositionalTag.DoubledRooks(_, color)   => sameSide(color, side)
          case PositionalTag.ConnectedRooks(color)    => sameSide(color, side)
          case PositionalTag.RookOnSeventh(color)     => sameSide(color, side)
          case _                                      => false
        } ||
          semantic.positionFeatures.exists(features =>
            features.lineControl.openFilesCount > 0 ||
              (if side == "white" then features.lineControl.whiteSemiOpenFiles else features.lineControl.blackSemiOpenFiles) > 0 ||
              (if side == "white" then features.lineControl.whiteRookOn7th else features.lineControl.blackRookOn7th)
          ) ||
          evaluated.pack.pieceRoutes.exists(route =>
            route.ownerSide == side &&
              route.surfaceMode != RouteSurfaceMode.Hidden &&
              (route.piece == "R" || route.piece == "Q")
          )
      case ProducerCheck.OutpostAnchor =>
        semantic.positionalFeatures.exists {
          case PositionalTag.Outpost(_, color)      => sameSide(color, side)
          case PositionalTag.StrongKnight(_, color) => sameSide(color, side)
          case _                                    => false
        } ||
          evaluated.pack.pieceRoutes.exists(route =>
            route.ownerSide == side &&
              route.surfaceMode != RouteSurfaceMode.Hidden &&
              (route.piece == "N" || route.piece == "B")
          )
      case ProducerCheck.MinorPieceImbalance =>
        semantic.positionalFeatures.exists {
          case PositionalTag.BishopPairAdvantage(color) => sameSide(color, side)
          case PositionalTag.BadBishop(color)           => !sameSide(color, side)
          case PositionalTag.GoodBishop(color)          => sameSide(color, side)
          case PositionalTag.OppositeColorBishops       => true
          case _                                        => false
        }
      case ProducerCheck.ProphylaxisSignal =>
        semantic.threatsToUs.exists(_.prophylaxisNeeded) ||
          semantic.preventedPlans.exists(plan =>
            plan.preventedThreatType.isDefined ||
              plan.deniedSquares.nonEmpty ||
              plan.defensiveSufficiency.exists(_ > 0)
          ) ||
          hasBishopPinWatch(evaluated) ||
          hasQueensideClampWatch(evaluated)
      case ProducerCheck.AttackBuildUpSignal =>
        semantic.threatsToThem.exists(threats =>
          threats.strategicThreat ||
            threats.immediateThreat ||
            threats.maxLossIfIgnored > 0
        ) ||
          semantic.motifs.exists {
            case _: Motif.RookLift | _: Motif.Battery | _: Motif.PieceLift | _: Motif.Check => true
            case _                                                                          => false
          } ||
          evaluated.pack.pieceRoutes.exists(route =>
            route.ownerSide == side &&
              route.surfaceMode != RouteSurfaceMode.Hidden &&
              route.route.nonEmpty
          )
      case ProducerCheck.SimplifyBias =>
        semantic.classification.exists(classification =>
          classification.simplifyBias.shouldSimplify || classification.taskMode.isConvertMode
        ) ||
          semantic.structureProfile.exists(profile =>
            profile.primary == lila.commentary.model.structure.StructureId.IQPBlack && side == "white"
          ) ||
          semantic.positionalFeatures.exists {
            case PositionalTag.RemovingTheDefender(_, color) => sameSide(color, side)
            case _                                           => false
          }
      case ProducerCheck.CounterBreakWatch =>
        semantic.opponentPawnAnalysis.exists(_.counterBreak) ||
          semantic.preventedPlans.exists(plan =>
            plan.breakNeutralized.isDefined ||
              plan.deniedResourceClass.contains("break") ||
              plan.breakNeutralizationStrength.exists(_ > 0)
          ) ||
          hasHedgehogBreakDenialGeometry(evaluated) ||
          hasMaroczyBreakDenialGeometry(evaluated)

  test("FEN fixture bank stays legal and complete") {
    val fixtures = StrategicIdeaFenFixtures.all
    assertEquals(StrategicIdeaFenFixtures.canonical.size, 30)
    assertEquals(StrategicIdeaFenFixtures.stockfishBalancedSupplemental.size, 74)
    assertEquals(StrategicIdeaFenFixtures.stockfishCompensationAcceptance.size, 10)
    assertEquals(fixtures.size, 114)
    assertEquals(fixtures.map(_.id).distinct.size, fixtures.size)
    assertEquals(fixtures.count(_.id.startsWith("K")), 77)
    assertEquals(fixtures.count(_.id.startsWith("B")), 27)
    assertEquals(fixtures.count(_.id.startsWith("G")), 10)

    evaluatedFixtures.foreach { evaluated =>
      val diff = materialValueDiff(evaluated.board)

      if (evaluated.fixture.requireMaterialParity) {
        assertEquals(
          diff,
          0,
          clue(
            s"${evaluated.fixture.id} ${evaluated.fixture.label} material mismatch: " +
              s"W=${materialSummary(evaluated.board, Color.White)} " +
              s"B=${materialSummary(evaluated.board, Color.Black)}"
          )
        )
      }

      if (evaluated.fixture.requireMaterialImbalance) {
        assertNotEquals(
          diff,
          0,
          clue(
            s"${evaluated.fixture.id} ${evaluated.fixture.label} expected material imbalance: " +
              s"W=${materialSummary(evaluated.board, Color.White)} " +
              s"B=${materialSummary(evaluated.board, Color.Black)}"
          )
        )
        assert(
          compensationSideHasMaterialDeficit(evaluated.fixture, diff),
          clue(
            s"${evaluated.fixture.id} ${evaluated.fixture.label} compensation side mismatch: " +
              s"diff=$diff side=${evaluated.fixture.compensationSide.getOrElse("-")}"
          )
        )
      }
    }
  }

  test("supplemental fixture bank keeps material parity and stockfish balance metadata") {
    val failures =
      StrategicIdeaFenFixtures.stockfishBalancedSupplemental.flatMap { fixture =>
        val evaluated = evaluatedById(fixture.id)

        List(
          Option.when(fixture.stockfishScoreCp.isEmpty) {
            s"${fixture.id} missing recorded Stockfish cp"
          },
          Option.when(fixture.stockfishMaxAbsCp.isEmpty) {
            s"${fixture.id} missing recorded Stockfish max abs cp"
          },
          Option.when(fixture.requireMaterialParity && materialValueDiff(evaluated.board) != 0) {
            s"${fixture.id} material mismatch: W=${materialSummary(evaluated.board, Color.White)} " +
              s"B=${materialSummary(evaluated.board, Color.Black)}"
          },
          for
            score <- fixture.stockfishScoreCp
            maxCp <- fixture.stockfishMaxAbsCp
            if math.abs(score) > maxCp
          yield s"${fixture.id} recorded score outside allowed window: cp=$score max=$maxCp seed=${fixture.sourceSeedId.getOrElse("-")}"
        ).flatten
      }

    assert(
      failures.isEmpty,
      clue(failures.mkString("\n"))
    )
  }

  test("compensation acceptance bank keeps material imbalance, stockfish balance, and side metadata") {
    val failures =
      StrategicIdeaFenFixtures.stockfishCompensationAcceptance.flatMap { fixture =>
        val evaluated = evaluatedById(fixture.id)
        val diff = materialValueDiff(evaluated.board)
        val side = evaluated.semantic.sideToMove

        List(
          Option.when(fixture.stockfishScoreCp.isEmpty) {
            s"${fixture.id} missing recorded Stockfish cp"
          },
          Option.when(fixture.stockfishMaxAbsCp.isEmpty) {
            s"${fixture.id} missing recorded Stockfish max abs cp"
          },
          Option.when(!fixture.requireMaterialImbalance) {
            s"${fixture.id} compensation bank fixture must require material imbalance"
          },
          Option.when(diff == 0) {
            s"${fixture.id} expected material imbalance: W=${materialSummary(evaluated.board, Color.White)} " +
              s"B=${materialSummary(evaluated.board, Color.Black)}"
          },
          Option.when(!compensationSideHasMaterialDeficit(fixture, diff)) {
            s"${fixture.id} compensation side ${fixture.compensationSide.getOrElse("-")} does not match diff=$diff"
          },
          Option.when(fixture.compensationSide.isEmpty) {
            s"${fixture.id} missing compensation side metadata"
          },
          Option.when(fixture.sideToMoveMismatch && fixture.compensationSide.contains(side)) {
            s"${fixture.id} expected side-to-move mismatch but side=$side"
          },
          Option.when(!fixture.sideToMoveMismatch && fixture.compensationSide.exists(_ != side)) {
            s"${fixture.id} expected evaluated side to match compensation side but side=$side comp=${fixture.compensationSide.getOrElse("-")}"
          },
          for
            score <- fixture.stockfishScoreCp
            maxCp <- fixture.stockfishMaxAbsCp
            if math.abs(score) > maxCp
          yield s"${fixture.id} recorded score outside allowed window: cp=$score max=$maxCp seed=${fixture.sourceSeedId.getOrElse("-")}"
        ).flatten
      }

    assert(
      failures.isEmpty,
      clue(failures.mkString("\n"))
    )
  }

  test("K06 French minor-piece surface keeps practical minor witness refs") {
    val evaluated = evaluatedById("K06")
    val minorIdea =
      evaluated.actualIdeas
        .find(_.kind == StrategicIdeaKind.MinorPieceImbalanceExploitation)
        .getOrElse(fail(s"K06 missing minor-piece idea; ${kindSummary(evaluated.actualIdeas)}"))
    val requiredRefs =
      List(
        "source:french_minor_piece_profile",
        "structure_french_advance_chain",
        "source:piece_activity_bad_bishop"
      )

    assert(
      requiredRefs.forall(minorIdea.evidenceRefs.contains),
      clue(s"K06 refs=${minorIdea.evidenceRefs.mkString("|")}; required=${requiredRefs.mkString("|")}")
    )
    assert(
      minorIdea.focusSquares.exists(square => minorIdea.evidenceRefs.contains(s"enemy_bad_bishop_$square")),
      clue(s"K06 focus=${minorIdea.focusSquares.mkString("|")} refs=${minorIdea.evidenceRefs.mkString("|")}")
    )
  }

  test("FEN fixtures keep locked-center space witness refs when selected") {
    val lockedCenterIdeas =
      evaluatedFixtures.flatMap(evaluated =>
        evaluated.actualIdeas
          .filter(_.kind == StrategicIdeaKind.SpaceGainOrRestriction)
          .filter(_.evidenceRefs.contains("source:locked_center_bind"))
          .map(evaluated.fixture.id -> _)
      )

    assert(
      lockedCenterIdeas.nonEmpty,
      clue(evaluatedFixtures.map(evaluated => s"${evaluated.fixture.id}: ${kindSummary(evaluated.actualIdeas)}").mkString("\n"))
    )
    assert(
      lockedCenterIdeas.forall { case (_, idea) => idea.evidenceRefs.contains("structure_locked_center") },
      clue(lockedCenterIdeas.map { case (id, idea) => s"$id refs=${idea.evidenceRefs.mkString("|")}" }.mkString("\n"))
    )
  }

  test("FEN fixtures keep file-opening break witness refs when selected") {
    val fileOpeningIdeas =
      evaluatedFixtures.flatMap(evaluated =>
        evaluated.actualIdeas
          .filter(_.kind == StrategicIdeaKind.PawnBreak)
          .filter(_.evidenceRefs.contains("source:file_opening_consequence"))
          .map(evaluated.fixture.id -> _)
      )

    assert(
      fileOpeningIdeas.nonEmpty,
      clue(evaluatedFixtures.map(evaluated => s"${evaluated.fixture.id}: ${kindSummary(evaluated.actualIdeas)}").mkString("\n"))
    )
    assert(
      fileOpeningIdeas.forall { case (_, idea) =>
        idea.focusFiles.exists(file => idea.evidenceRefs.contains(s"contested_file_$file"))
      },
      clue(fileOpeningIdeas.map { case (id, idea) => s"$id files=${idea.focusFiles.mkString("|")} refs=${idea.evidenceRefs.mkString("|")}" }.mkString("\n"))
    )
  }

  test("FEN fixtures keep French ...f6 break seed witness refs when selected") {
    val frenchSeedIdeas =
      evaluatedFixtures.flatMap(evaluated =>
        evaluated.actualIdeas
          .filter(_.kind == StrategicIdeaKind.PawnBreak)
          .filter(_.evidenceRefs.contains("source:french_f6_break_seed"))
          .map(evaluated.fixture.id -> _)
      )
    val requiredRefs =
      List(
        "french_f6_break_seed_shape",
        "white_e5_chain",
        "black_f7_break_pawn"
      )

    assert(
      frenchSeedIdeas.nonEmpty,
      clue(evaluatedFixtures.map(evaluated => s"${evaluated.fixture.id}: ${kindSummary(evaluated.actualIdeas)}").mkString("\n"))
    )
    assert(
      frenchSeedIdeas.forall { case (_, idea) => requiredRefs.forall(idea.evidenceRefs.contains) },
      clue(frenchSeedIdeas.map { case (id, idea) => s"$id refs=${idea.evidenceRefs.mkString("|")}" }.mkString("\n"))
    )
  }

  test("FEN fixtures keep outpost tag witness refs when selected") {
    val outpostIdeas =
      evaluatedFixtures.flatMap(evaluated =>
        evaluated.actualIdeas
          .filter(_.kind == StrategicIdeaKind.OutpostCreationOrOccupation)
          .filter(_.evidenceRefs.contains("source:outpost_tag"))
          .map(evaluated.fixture.id -> _)
      )

    assert(
      outpostIdeas.nonEmpty,
      clue(evaluatedFixtures.map(evaluated => s"${evaluated.fixture.id}: ${kindSummary(evaluated.actualIdeas)}").mkString("\n"))
    )
    assert(
      outpostIdeas.forall { case (_, idea) =>
        idea.focusSquares.exists(square => idea.evidenceRefs.contains(s"outpost_$square"))
      },
      clue(outpostIdeas.map { case (id, idea) => s"$id focus=${idea.focusSquares.mkString("|")} refs=${idea.evidenceRefs.mkString("|")}" }.mkString("\n"))
    )
  }

  test("K04 occupied-line surface keeps practical line witness refs") {
    val evaluated = evaluatedById("K04")
    val lineIdea =
      evaluated.actualIdeas
        .find(idea =>
          idea.kind == StrategicIdeaKind.LineOccupation &&
            idea.evidenceRefs.contains("source:occupied_line_control")
        )
        .getOrElse(fail(s"K04 missing occupied-line idea; ${kindSummary(evaluated.actualIdeas)}"))

    assert(
      lineIdea.focusSquares.exists(square => lineIdea.evidenceRefs.contains(s"occupied_r_$square")),
      clue(s"K04 focus=${lineIdea.focusSquares.mkString("|")} refs=${lineIdea.evidenceRefs.mkString("|")}")
    )
    assert(
      lineIdea.focusFiles.exists(file =>
        lineIdea.evidenceRefs.contains(s"open_file_$file") ||
          lineIdea.evidenceRefs.contains(s"semi_open_file_$file")
      ),
      clue(s"K04 files=${lineIdea.focusFiles.mkString("|")} refs=${lineIdea.evidenceRefs.mkString("|")}")
    )
    assertEquals(lineIdea.readiness, StrategicIdeaReadiness.Ready)
    assert(lineIdea.confidence >= 0.78, clue(lineIdea))
    assert(lineIdea.beneficiaryPieces.exists(_.equalsIgnoreCase("R")), clue(lineIdea))
  }

  test("FEN fixtures keep weak-square pressure witness refs when selected") {
    val weakPressureIdeas =
      List("K14", "B21", "K14A", "B21A").flatMap(id =>
        evaluatedById(id).actualIdeas
          .filter(_.kind == StrategicIdeaKind.TargetFixing)
          .filter { idea =>
            val weakSquareBound =
              idea.evidenceRefs.contains("source:enemy_weak_square") &&
                idea.focusSquares.exists(square => idea.evidenceRefs.contains(s"enemy_weak_square_$square"))
            val weakComplexBound =
              idea.evidenceRefs.contains("source:weak_complex_fixation") &&
                idea.evidenceRefs.exists(ref => ref.startsWith("weak_complex_") && ref != "weak_complex_fixation")
            weakSquareBound || weakComplexBound
          }
          .map(id -> _)
      )

    assert(
      weakPressureIdeas.nonEmpty,
      clue(
        List("K14", "B21", "K14A", "B21A")
          .map(id => s"$id: ${kindSummary(evaluatedById(id).actualIdeas)}")
          .mkString("\n")
      )
    )
    assertEquals(weakPressureIdeas.map(_._1).toSet, Set("K14", "B21", "K14A", "B21A"))
  }

  test("FEN fixtures keep IQP trade-down witness refs when selected") {
    val iqpTradeIdeas =
      evaluatedFixtures.flatMap(evaluated =>
        evaluated.actualIdeas
          .filter(_.kind == StrategicIdeaKind.FavorableTradeOrTransformation)
          .filter(_.evidenceRefs.contains("source:iqp_simplification_profile"))
          .map(evaluated.fixture.id -> _)
      )

    assert(
      iqpTradeIdeas.nonEmpty,
      clue(evaluatedFixtures.map(evaluated => s"${evaluated.fixture.id}: ${kindSummary(evaluated.actualIdeas)}").mkString("\n"))
    )
    assert(
      iqpTradeIdeas.forall { case (_, idea) =>
        idea.evidenceRefs.contains("structure_iqp_black") &&
          (idea.evidenceRefs.contains("capture_or_exchange") || idea.evidenceRefs.contains("iqp_trade_down_plan"))
      },
      clue(iqpTradeIdeas.map { case (id, idea) => s"$id refs=${idea.evidenceRefs.mkString("|")}" }.mkString("\n"))
    )
  }

  test("FEN fixtures keep board-pattern prophylaxis witness refs when selected") {
    val boardPatternProphylaxisIdeas =
      evaluatedFixtures.flatMap(evaluated =>
        evaluated.actualIdeas
          .filter(_.kind == StrategicIdeaKind.Prophylaxis)
          .filter(idea =>
            idea.evidenceRefs.contains("source:bishop_pin_watch") ||
              idea.evidenceRefs.contains("source:queenside_counterbreak_watch")
          )
          .map(evaluated.fixture.id -> _)
      )

    assert(
      boardPatternProphylaxisIdeas.nonEmpty,
      clue(evaluatedFixtures.map(evaluated => s"${evaluated.fixture.id}: ${kindSummary(evaluated.actualIdeas)}").mkString("\n"))
    )
    assert(
      boardPatternProphylaxisIdeas.forall { case (_, idea) =>
        (
          idea.evidenceRefs.contains("source:bishop_pin_watch") &&
            idea.focusSquares.exists(square => square == "g4" || square == "g5")
        ) ||
          (
            idea.evidenceRefs.contains("source:queenside_counterbreak_watch") &&
              idea.focusFiles.contains("b")
          )
      },
      clue(
        boardPatternProphylaxisIdeas
          .map { case (id, idea) => s"$id focus=${idea.focusSquares.mkString("|")} files=${idea.focusFiles.mkString("|")} refs=${idea.evidenceRefs.mkString("|")}" }
          .mkString("\n")
      )
    )
  }

  test("FEN fixtures keep counterplay break-denial geometry witness refs when selected") {
    val restraintIdeas =
      evaluatedFixtures.flatMap(evaluated =>
        evaluated.actualIdeas
          .filter(_.kind == StrategicIdeaKind.CounterplaySuppression)
          .filter(idea =>
            idea.evidenceRefs.contains("source:hedgehog_break_denial_geometry") ||
              idea.evidenceRefs.contains("source:maroczy_break_denial_geometry")
          )
          .map(evaluated.fixture.id -> _)
      )

    assert(
      restraintIdeas.nonEmpty,
      clue(evaluatedFixtures.map(evaluated => s"${evaluated.fixture.id}: ${kindSummary(evaluated.actualIdeas)}").mkString("\n"))
    )
    assert(
      restraintIdeas.forall { case (_, idea) =>
        val refs = idea.evidenceRefs
        if refs.contains("source:hedgehog_break_denial_geometry") then
          refs.contains("structure_hedgehog") &&
            refs.contains("hedgehog_break_denial_shape") &&
            idea.focusFiles.contains("b") &&
            idea.focusFiles.contains("d")
        else
          refs.contains("structure_maroczy_bind") &&
            refs.contains("maroczy_break_denial_shape") &&
            idea.focusFiles.contains("c") &&
            idea.focusFiles.contains("d")
      },
      clue(
        restraintIdeas
          .map { case (id, idea) => s"$id files=${idea.focusFiles.mkString("|")} refs=${idea.evidenceRefs.mkString("|")}" }
          .mkString("\n")
      )
    )
  }

  test("K08 fianchetto assault surface keeps practical attack witness refs") {
    val evaluated = evaluatedById("K08")
    val attackIdea =
      evaluated.actualIdeas
        .find(idea =>
          idea.kind == StrategicIdeaKind.KingAttackBuildUp &&
            idea.evidenceRefs.contains("source:fianchetto_assault_profile")
        )
        .getOrElse(fail(s"K08 missing fianchetto assault idea; ${kindSummary(evaluated.actualIdeas)}"))
    val requiredRefs =
      List(
        "source:fianchetto_assault_profile",
        "source:opposite_side_storm",
        "structure_fianchetto_shell"
      )

    assert(
      requiredRefs.forall(attackIdea.evidenceRefs.contains),
      clue(s"K08 refs=${attackIdea.evidenceRefs.mkString("|")}; required=${requiredRefs.mkString("|")}")
    )
    assertEquals(attackIdea.readiness, StrategicIdeaReadiness.Build)
    assertEquals(attackIdea.focusZone.map(_.toLowerCase), Some("kingside"))
    assert(attackIdea.confidence >= 0.90, clue(attackIdea))
  }

  StrategicIdeaFenFixtures.all.foreach { fixture =>
    test(s"${fixture.id} producer signals: ${fixture.label}") {
      val evaluated = evaluatedById(fixture.id)
      fixture.producerChecks.foreach { check =>
        assert(
          checkProducerSignal(evaluated, check),
          clue(s"${evaluated.fixture.id} ${evaluated.fixture.label} missing producer check $check; ${producerSummary(evaluated)}")
        )
      }
    }

    test(s"${fixture.id} dominant idea: ${fixture.label}") {
      val evaluated = evaluatedById(fixture.id)
      val dominant = evaluated.dominantIdea

      assertEquals(
        dominant.kind,
        evaluated.fixture.expectedDominant,
        clue(s"${evaluated.fixture.id} ${evaluated.fixture.label}; ${producerSummary(evaluated)}")
      )

      evaluated.fixture.boundaryAgainst.foreach { other =>
        assertNotEquals(
          dominant.kind,
          other,
          clue(s"${evaluated.fixture.id} ${evaluated.fixture.label}; ${producerSummary(evaluated)}")
        )
      }

      evaluated.fixture.forbiddenKinds.foreach { forbidden =>
        assert(
          !evaluated.actualIdeaKinds.contains(forbidden),
          clue(s"${evaluated.fixture.id} ${evaluated.fixture.label} emitted forbidden kind $forbidden; ${producerSummary(evaluated)}")
        )
      }
    }

    test(s"${fixture.id} prose invariance: ${fixture.label}") {
      val evaluated = evaluatedById(fixture.id)
      val ideas = StrategicIdeaSelector.select(misleadingPack(evaluated.pack), evaluated.semantic)
      val dominant = ideas.headOption.getOrElse(fail(s"misleading pack lost dominant idea for ${evaluated.fixture.id}"))

      assertEquals(
        dominant.kind,
        evaluated.fixture.expectedDominant,
        clue(s"${evaluated.fixture.id} ${evaluated.fixture.label}; ${kindSummary(ideas)}")
      )
    }
  }
