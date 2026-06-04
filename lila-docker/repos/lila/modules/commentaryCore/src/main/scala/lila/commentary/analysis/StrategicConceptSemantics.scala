package lila.commentary.analysis

import _root_.chess.{ Board, Color, Pawn, Square }
import _root_.chess.format.Fen
import _root_.chess.variant.Standard
import lila.commentary.analysis.structure.{ StructuralDelta, StructuralDeltaAnalyzer }

private[commentary] object StrategicConceptSemantics:

  val MinorityAttackConceptId = "minority_attack"

  enum ConceptStatus:
    case Absent, Candidate, SemanticReady, Refuted, Deferred

  final case class EvidenceAtom(
      id: String,
      kind: String,
      square: Option[String] = None,
      file: Option[String] = None,
      move: Option[String] = None,
      essential: Boolean = true
  )

  final case class StrategicConceptObservation(
      conceptId: String,
      status: ConceptStatus,
      side: Color,
      wing: String,
      fileSet: List[String],
      targets: List[String] = Nil,
      primaryBreak: Option[String] = None,
      prepMoves: List[String] = Nil,
      structuralDelta: Option[StructuralDelta] = None,
      essentialEvidence: List[EvidenceAtom] = Nil,
      supportingEvidence: List[EvidenceAtom] = Nil,
      blockedReasons: List[String] = Nil,
      symmetryKey: String
  )

  private final case class BreakCandidate(
      primaryMove: String,
      prepMoves: List[String],
      target: String,
      attackSquare: String,
      startSquare: String,
      afterFen: String,
      afterBoard: Board
  )

  private final case class TargetInfo(
      square: String,
      reasons: List[String]
  )

  private final case class ConceptCondition(
      id: String,
      passed: Boolean,
      evidence: EvidenceAtom
  )

  def minorityAttackObservations(
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicConceptObservation] =
    semantic.board.toList.flatMap { board =>
      val side = sideFromString(semantic.sideToMove)
      List("queenside", "kingside").flatMap(evaluateMinorityWing(board, side, _, semantic))
    }

  private def evaluateMinorityWing(
      board: Board,
      side: Color,
      wing: String,
      semantic: StrategicIdeaSemanticContext
  ): Option[StrategicConceptObservation] =
    val files = wingFiles(wing)
    val ownPawns = pawnsOnFiles(board, side, files)
    val enemyPawns = pawnsOnFiles(board, side, files, enemy = true)
    if ownPawns.isEmpty || ownPawns.size >= enemyPawns.size || enemyPawns.size < 2 then None
    else
      val targetInfos = targetableEnemyMajority(board, side, files)
      val targetSquares = targetInfos.map(_.square)
      val breakCandidates =
        targetSquares.flatMap(target => reachableBreaks(board, side, files, ownPawns, target, semantic.fen))
      val chosenBreak = breakCandidates.headOption
      val structuralDelta =
        chosenBreak.flatMap(candidate =>
          StructuralDeltaAnalyzer.delta(
            beforeFen = semantic.fen,
            beforeBoard = board,
            afterFen = candidate.afterFen,
            afterBoard = candidate.afterBoard,
            side = side,
            files = files,
            targets = targetSquares,
            createdTensionFrom = Some(candidate.attackSquare)
          )
        )
      val kingSidePawnStorm =
        targetSquares.nonEmpty && chosenBreak.nonEmpty && wing == "kingside" && enemyKingOnWing(board, side, files)
      val blocked =
        List(
          Option.when(targetSquares.isEmpty)("targetable_enemy_majority_missing"),
          Option.when(targetSquares.nonEmpty && chosenBreak.isEmpty)("reachable_break_missing"),
          Option.when(chosenBreak.nonEmpty && structuralDelta.forall(!_.hasConsequence))(
            "structural_consequence_missing"
          ),
          Option.when(kingSidePawnStorm)("king_side_pawn_storm")
        ).flatten
      val hasSemanticCore =
        targetSquares.nonEmpty &&
          chosenBreak.nonEmpty &&
          structuralDelta.exists(_.hasConsequence) &&
          !kingSidePawnStorm
      val tacticalRefuted = hasSemanticCore && immediateTacticalRefutation(semantic)
      val status =
        if tacticalRefuted then ConceptStatus.Refuted
        else if hasSemanticCore then ConceptStatus.SemanticReady
        else if targetSquares.nonEmpty && chosenBreak.isEmpty then ConceptStatus.Deferred
        else ConceptStatus.Candidate
      val conditions =
        List(
          ConceptCondition(
            "wing_minority",
            passed = true,
            EvidenceAtom("wing_minority", "pawn_structure", essential = true)
          ),
          ConceptCondition(
            "targetable_enemy_majority",
            passed = targetSquares.nonEmpty,
            EvidenceAtom(
              "targetable_enemy_majority",
              "pawn_structure",
              square = targetSquares.headOption,
              essential = true
            )
          ),
          ConceptCondition(
            "reachable_break",
            passed = chosenBreak.nonEmpty,
            EvidenceAtom("reachable_break", "pawn_break", move = chosenBreak.map(_.primaryMove), essential = true)
          ),
          ConceptCondition(
            "structural_consequence",
            passed = structuralDelta.exists(_.hasConsequence),
            EvidenceAtom("structural_consequence", "counterfactual_delta", essential = true)
          ),
          ConceptCondition(
            "not_refuted",
            passed = !tacticalRefuted && !kingSidePawnStorm,
            EvidenceAtom("not_refuted", "soundness_guard", essential = true)
          )
        )
      val evidence =
        if status == ConceptStatus.SemanticReady then irredundantEvidence(conditions)
        else conditions.collect { case condition if condition.passed => condition.evidence }
      Some(
        StrategicConceptObservation(
          conceptId = MinorityAttackConceptId,
          status = status,
          side = side,
          wing = wing,
          fileSet = files.map(_.toString),
          targets = targetSquares,
          primaryBreak = chosenBreak.map(_.primaryMove),
          prepMoves = chosenBreak.toList.flatMap(_.prepMoves),
          structuralDelta = structuralDelta.filter(_.hasConsequence),
          essentialEvidence = evidence,
          supportingEvidence =
            (List(
              EvidenceAtom(s"minority_pawns:${ownPawns.size}-${enemyPawns.size}", "pawn_count", essential = false)
            ) ++ targetInfos.flatMap(info =>
              info.reasons.map(reason =>
                EvidenceAtom(s"target_reason:$reason", "pawn_structure", square = Some(info.square), essential = false)
              )
            )).filterNot(support => evidence.exists(_.id == support.id)),
          blockedReasons = (blocked ++ Option.when(tacticalRefuted)("tactical_refutation")).distinct,
          symmetryKey = s"${if side.white then "white" else "black"}:$wing:${files.mkString}"
        )
      )

  private def targetableEnemyMajority(
      board: Board,
      side: Color,
      files: List[Char]
  ): List[TargetInfo] =
    val enemyPawns = board.byPiece(!side, Pawn)
    val backward = PositionAnalyzer.backwardPawns(!side, enemyPawns, board).map(_.key).toSet
    val isolated = PositionAnalyzer.isolatedPawns(enemyPawns).map(_.key).toSet
    pawnsOnFiles(board, side, files, enemy = true)
      .flatMap { square =>
        val reasons =
          List(
            Option.when(hasTargetChain(board, square, side))("target_chain"),
            Option.when(backward.contains(square.key))("backward_pawn"),
            Option.when(isolated.contains(square.key))("isolated_pawn"),
            Option.when(isFixedByEnemyBlocker(board, square, side))("fixed_pawn")
          ).flatten
        Option.when(isAdvancedEnemyPawn(square, side) && reasons.nonEmpty)(
          TargetInfo(square.key, reasons.distinct)
        )
      }
      .groupBy(_.square)
      .values
      .map(infos => infos.reduce((left, right) => left.copy(reasons = (left.reasons ++ right.reasons).distinct)))
      .toList
      .sortBy(_.square)

  private def reachableBreaks(
      board: Board,
      side: Color,
      files: List[Char],
      ownPawns: List[Square],
      target: String,
      baseFen: String
  ): List[BreakCandidate] =
    attackingSquaresForTarget(target, side)
      .filter(square => square.headOption.exists(files.contains))
      .flatMap { attackSquare =>
        backward(attackSquare, side).flatMap { startSquare =>
          val pawnsOnFile = ownPawns.filter(_.key.headOption == startSquare.headOption)
          pawnsOnFile.find(pawn => canReachStagingSquare(board, side, pawn.key, startSquare)).flatMap { pawn =>
            val prepMoves =
              if pawn.key == startSquare then Nil
              else List(s"${pawn.key}$startSquare")
            val primaryMove = s"$startSquare$attackSquare"
            legalBreakFenAfter(baseFen, side, prepMoves, primaryMove).flatMap { afterFen =>
              Fen.read(Standard, Fen.Full(afterFen)).map { afterPosition =>
                BreakCandidate(
                  primaryMove = primaryMove,
                  prepMoves = prepMoves,
                  target = target,
                  attackSquare = attackSquare,
                  startSquare = startSquare,
                  afterFen = afterFen,
                  afterBoard = afterPosition.board
                )
              }
            }
          }
        }
      }

  private def immediateTacticalRefutation(
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.threatsToUs.exists(threats =>
      threats.defenseRequired &&
        !threats.threatIgnorable &&
        threats.maxLossIfIgnored >= 300
    )

  private def irredundantEvidence(conditions: List[ConceptCondition]): List[EvidenceAtom] =
    val required =
      Set("wing_minority", "targetable_enemy_majority", "reachable_break", "structural_consequence", "not_refuted")
    conditions.filter(condition =>
      condition.passed &&
        required.contains(condition.id) &&
        !semanticReadyWithout(conditions, condition.id)
    ).map(_.evidence)

  private def semanticReadyWithout(conditions: List[ConceptCondition], removedId: String): Boolean =
    val remaining = conditions.filterNot(_.id == removedId)
    Set("wing_minority", "targetable_enemy_majority", "reachable_break", "structural_consequence", "not_refuted")
      .forall(id => remaining.exists(condition => condition.id == id && condition.passed))

  private def legalFenAfter(baseFen: String, moves: List[String]): Option[String] =
    val normalizedMoves = moves.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
    Option.when(baseFen.trim.nonEmpty && normalizedMoves.nonEmpty)(normalizedMoves)
      .flatMap(
        _.foldLeft(Option(baseFen))((current, move) =>
          current.flatMap(MoveReviewPvLine.legalFenAfter(_, move))
        )
      )

  private def legalBreakFenAfter(
      baseFen: String,
      side: Color,
      prepMoves: List[String],
      primaryMove: String
  ): Option[String] =
    if prepMoves.isEmpty then legalFenAfter(baseFen, List(primaryMove))
    else
      legalFenAfter(baseFen, prepMoves).flatMap { afterPrepFen =>
        legalFenAfter(withSideToMove(afterPrepFen, side), List(primaryMove))
      }

  private def withSideToMove(fen: String, side: Color): String =
    val parts = normalizeFen(fen).split("\\s+").toList
    parts match
      case board :: _ :: castling :: ep :: rest =>
        (board :: (if side.white then "w" else "b") :: castling :: ep :: rest).mkString(" ")
      case _ => fen

  private def normalizeFen(fen: String): String =
    Option(fen).getOrElse("").trim.split("\\s+").filter(_.nonEmpty).mkString(" ")

  private def enemyKingOnWing(board: Board, side: Color, files: List[Char]): Boolean =
    board.kingPosOf(!side).exists(square => square.key.headOption.exists(files.contains))

  private def pawnsOnFiles(
      board: Board,
      side: Color,
      files: List[Char],
      enemy: Boolean = false
  ): List[Square] =
    Square.all.toList.filter { square =>
      square.key.headOption.exists(files.contains) &&
        board.pieceAt(square).exists(piece =>
          piece.role == Pawn &&
            (if enemy then piece.color != side else piece.color == side)
        )
    }

  private def isAdvancedEnemyPawn(square: Square, side: Color): Boolean =
    rankOf(square.key).exists { rank =>
      if side.white then rank <= 6 && rank >= 3
      else rank >= 3 && rank <= 6
    }

  private def hasTargetChain(board: Board, target: Square, side: Color): Boolean =
    val enemyColor = board.pieceAt(target).map(_.color)
    adjacentSquares(target.key).exists { squareKey =>
      squareAt(squareKey).flatMap(board.pieceAt).exists(piece =>
        enemyColor.contains(piece.color) && piece.role == Pawn && piece.color != side
      )
    }

  private def isFixedByEnemyBlocker(board: Board, target: Square, side: Color): Boolean =
    val enemyColor = !side
    val stopRank = target.rank.value + (if enemyColor.white then 1 else -1)
    Square.at(target.file.value, stopRank).exists(square =>
      board.pieceAt(square).exists(piece => piece.role == Pawn && piece.color == side)
    )

  private def canReachStagingSquare(
      board: Board,
      side: Color,
      pawn: String,
      staging: String
  ): Boolean =
    if pawn == staging then true
    else
      fileOf(pawn) == fileOf(staging) &&
        rankOf(pawn).zip(rankOf(staging)).exists { case (fromRank, toRank) =>
          val direction = if side.white then 1 else -1
          val distance = (toRank - fromRank) * direction
          distance > 0 &&
            pathSquares(pawn, staging, direction).forall(squareKey =>
              squareAt(squareKey).forall(square => board.pieceAt(square).isEmpty)
            )
        }

  private def attackingSquaresForTarget(target: String, side: Color): List[String] =
    for
      file <- fileOf(target).toList
      rank <- rankOf(target).toList
      sourceRank = rank - forwardDirection(side)
      sourceFile <- List((file - 1).toChar, (file + 1).toChar)
      if sourceFile >= 'a' && sourceFile <= 'h' && sourceRank >= 1 && sourceRank <= 8
    yield s"$sourceFile$sourceRank"

  private def backward(square: String, side: Color): Option[String] =
    for
      file <- fileOf(square)
      rank <- rankOf(square)
      startRank = rank - forwardDirection(side)
      if startRank >= 1 && startRank <= 8
    yield s"$file$startRank"

  private def adjacentSquares(square: String): List[String] =
    for
      file <- fileOf(square).toList
      rank <- rankOf(square).toList
      df <- List(-1, 0, 1)
      dr <- List(-1, 0, 1)
      if df != 0 || dr != 0
      nextFile = (file + df).toChar
      nextRank = rank + dr
      if nextFile >= 'a' && nextFile <= 'h' && nextRank >= 1 && nextRank <= 8
    yield s"$nextFile$nextRank"

  private def pathSquares(from: String, to: String, direction: Int): List[String] =
    (for
      file <- fileOf(from)
      fromRank <- rankOf(from)
      toRank <- rankOf(to)
    yield
      val ranks =
        Iterator
          .iterate(fromRank + direction)(_ + direction)
          .takeWhile(rank => if direction > 0 then rank <= toRank else rank >= toRank)
          .toList
      ranks.map(rank => s"$file$rank")
    ).getOrElse(Nil)

  private def sideFromString(side: String): Color =
    if side.equalsIgnoreCase("black") then Color.Black else Color.White

  private def wingFiles(wing: String): List[Char] =
    if wing == "kingside" then List('f', 'g', 'h') else List('a', 'b', 'c')

  private def forwardDirection(side: Color): Int =
    if side.white then 1 else -1

  private def squareAt(key: String): Option[Square] =
    Square.all.find(_.key == key)

  private def fileOf(square: String): Option[Char] =
    square.headOption.filter(file => file >= 'a' && file <= 'h')

  private def rankOf(square: String): Option[Int] =
    square.lift(1).flatMap(char => Option.when(char >= '1' && char <= '8')(char.asDigit))
