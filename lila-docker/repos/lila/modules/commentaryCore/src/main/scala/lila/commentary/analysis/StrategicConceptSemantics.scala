package lila.commentary.analysis

import _root_.chess.{ Board, Color, Pawn, Square }

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

  final case class StructuralDelta(
      openedFiles: List[String] = Nil,
      semiOpenedFiles: List[String] = Nil,
      newWeakPawns: List[String] = Nil,
      newWeakSquares: List[String] = Nil,
      pawnTensionBefore: Int = 0,
      pawnTensionAfter: Int = 0,
      kingShelterDelta: Int = 0,
      mobilityDelta: Int = 0
  ):
    def pawnTensionDelta: Int = pawnTensionAfter - pawnTensionBefore
    def hasConsequence: Boolean =
      openedFiles.nonEmpty ||
        semiOpenedFiles.nonEmpty ||
        newWeakPawns.nonEmpty ||
        newWeakSquares.nonEmpty ||
        pawnTensionDelta > 0 ||
        kingShelterDelta != 0 ||
        mobilityDelta != 0

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
      startSquare: String
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
      val targetSquares = targetableEnemyMajority(board, side, files)
      val breakCandidates =
        targetSquares.flatMap(target => reachableBreaks(board, side, files, ownPawns, target))
      val chosenBreak = breakCandidates.headOption
      val structuralDelta = chosenBreak.flatMap(candidate => structuralDeltaFor(board, side, targetSquares, candidate))
      val blocked =
        List(
          Option.when(targetSquares.isEmpty)("targetable_enemy_majority_missing"),
          Option.when(targetSquares.nonEmpty && chosenBreak.isEmpty)("reachable_break_missing"),
          Option.when(chosenBreak.nonEmpty && structuralDelta.forall(!_.hasConsequence))(
            "structural_consequence_missing"
          )
        ).flatten
      val hasSemanticCore =
        targetSquares.nonEmpty && chosenBreak.nonEmpty && structuralDelta.exists(_.hasConsequence)
      val tacticalRefuted = hasSemanticCore && immediateTacticalRefutation(semantic)
      val status =
        if tacticalRefuted then ConceptStatus.Refuted
        else if hasSemanticCore then ConceptStatus.SemanticReady
        else if targetSquares.nonEmpty && chosenBreak.isEmpty then ConceptStatus.Deferred
        else ConceptStatus.Candidate
      val evidence =
        List(
          Some(EvidenceAtom("wing_minority", "pawn_structure", essential = true)),
          Option.when(targetSquares.nonEmpty)(
            EvidenceAtom(
              "targetable_enemy_majority",
              "pawn_structure",
              square = targetSquares.headOption,
              essential = true
            )
          ),
          chosenBreak.map(candidate =>
            EvidenceAtom("reachable_break", "pawn_break", move = Some(candidate.primaryMove), essential = true)
          ),
          structuralDelta.filter(_.hasConsequence).map(_ =>
            EvidenceAtom("structural_consequence", "counterfactual_delta", essential = true)
          )
        ).flatten
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
            List(
              EvidenceAtom(s"minority_pawns:${ownPawns.size}-${enemyPawns.size}", "pawn_count", essential = false)
            ),
          blockedReasons = (blocked ++ Option.when(tacticalRefuted)("tactical_refutation")).distinct,
          symmetryKey = s"${if side.white then "white" else "black"}:$wing:${files.mkString}"
        )
      )

  private def targetableEnemyMajority(
      board: Board,
      side: Color,
      files: List[Char]
  ): List[String] =
    pawnsOnFiles(board, side, files, enemy = true)
      .filter(square => isAdvancedEnemyPawn(square, side) && hasTargetChain(board, square, side))
      .map(_.key)
      .distinct

  private def reachableBreaks(
      board: Board,
      side: Color,
      files: List[Char],
      ownPawns: List[Square],
      target: String
  ): List[BreakCandidate] =
    attackingSquaresForTarget(target, side)
      .filter(square => square.headOption.exists(files.contains))
      .flatMap { attackSquare =>
        backward(attackSquare, side).flatMap { startSquare =>
          val pawnsOnFile = ownPawns.filter(_.key.headOption == startSquare.headOption)
          pawnsOnFile.find(pawn => canReachStagingSquare(board, side, pawn.key, startSquare)).map { pawn =>
            val prepMoves =
              if pawn.key == startSquare then Nil
              else List(s"${pawn.key}$startSquare")
            BreakCandidate(
              primaryMove = s"$startSquare$attackSquare",
              prepMoves = prepMoves,
              target = target,
              attackSquare = attackSquare,
              startSquare = startSquare
            )
          }
        }
      }

  private def structuralDeltaFor(
      board: Board,
      side: Color,
      targets: List[String],
      candidate: BreakCandidate
  ): Option[StructuralDelta] =
    val beforeAttacks = targets.filter(target => sidePawnAttacksTarget(board, side, target))
    val afterAttacks =
      targets.filter(target => pawnAttacks(candidate.attackSquare, side).contains(target))
    val newTargets = afterAttacks.diff(beforeAttacks).distinct
    Some(
      StructuralDelta(
        newWeakPawns = newTargets,
        newWeakSquares = newTargets,
        pawnTensionBefore = beforeAttacks.size,
        pawnTensionAfter = afterAttacks.size
      )
    )

  private def immediateTacticalRefutation(
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.threatsToUs.exists(threats =>
      threats.defenseRequired &&
        !threats.threatIgnorable &&
        threats.maxLossIfIgnored >= 300
    )

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

  private def sidePawnAttacksTarget(
      board: Board,
      side: Color,
      target: String
  ): Boolean =
    Square.all.exists { square =>
      board.pieceAt(square).exists(piece => piece.color == side && piece.role == Pawn) &&
        pawnAttacks(square.key, side).contains(target)
    }

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

  private def pawnAttacks(square: String, side: Color): List[String] =
    for
      file <- fileOf(square).toList
      rank <- rankOf(square).toList
      targetRank = rank + forwardDirection(side)
      targetFile <- List((file - 1).toChar, (file + 1).toChar)
      if targetFile >= 'a' && targetFile <= 'h' && targetRank >= 1 && targetRank <= 8
    yield s"$targetFile$targetRank"

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
