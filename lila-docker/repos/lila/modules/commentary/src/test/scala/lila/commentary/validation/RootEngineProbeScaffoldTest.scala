package lila.commentary.validation

import lila.commentary.root.RootCoverageMatrix

import chess.format.Fen
import chess.variant

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

class RootEngineProbeScaffoldTest extends munit.FunSuite:

  private val engineRequiredSchemas =
    Set("outpost_square", "candidate_passer", "trapped_piece", "king_shelter_hole")

  private val requiredProbeCaseTypesBySchema =
    Map(
      "outpost_square" -> Set("exact", "near_miss", "nasty_negative"),
      "candidate_passer" -> Set("exact", "near_miss", "nasty_negative"),
      "trapped_piece" -> Set("exact", "near_miss", "nasty_negative"),
      "king_shelter_hole" -> Set("exact", "near_miss", "nasty_negative")
    )

  private val rootRows = loadRootRows()
  private val rootRowsById = rootRows.map(row => row.id -> row).toMap
  private val probeRows =
    EngineProbeExpectationCorpus.loadAll().filter(_.resolvedLayer == "root")

  test("root engine confound scaffold provides selected exact and fail-closed rows for every engine-required root schema"):
    assert(java.nio.file.Files.isRegularFile(StockfishProbe.enginePath))
    assert(probeRows.nonEmpty)
    assertEquals(probeRows.map(_.id).distinct.size, probeRows.size)

    val probeSchemas =
      probeRows
        .map: probeRow =>
          rootRowsById.getOrElse(probeRow.id, fail(s"Missing root row for ${probeRow.id}")).schema
        .toSet

    assertEquals(probeSchemas, engineRequiredSchemas)
    assertEquals(requiredProbeCaseTypesBySchema.keySet, engineRequiredSchemas)

    engineRequiredSchemas.foreach: schema =>
      val caseTypes =
        probeRows
          .collect:
            case probeRow if rootRowsById(probeRow.id).schema == schema =>
              rootRowsById(probeRow.id).caseType
          .toSet

      val requiredCaseTypes =
        requiredProbeCaseTypesBySchema.getOrElse(schema, fail(s"Missing required probe case-type contract for $schema"))
      assertEquals(caseTypes, requiredCaseTypes, s"$schema root engine scaffold case-type inventory must stay frozen")

  test("broad-confidence-green and sbt-pending engine-required schemas keep their matrix-owned selected engine probe buckets"):
    val promotedOrPendingSchemas =
      engineRequiredSchemas.filter: schema =>
        val policy = RootCoverageMatrix.policyFor(schema)
        Set("broad-confidence-green", "broad-confidence-green-candidate (sbt-pending)").contains(policy.status) &&
          policy.greenEngineProbeBuckets.nonEmpty

    promotedOrPendingSchemas.foreach: schema =>
      val actualBuckets =
        probeRows
          .collect:
            case probeRow if rootRowsById(probeRow.id).schema == schema =>
              selectedProbeBucket(rootRowsById(probeRow.id))
          .toSet
      val expectedBuckets =
        RootCoverageMatrix.greenEngineProbeBucketsFor(schema).toSet

      assertEquals(actualBuckets, expectedBuckets)

  probeRows.foreach: probeRow =>
    test(s"root engine confound position stays inside frozen budgets for ${probeRow.id}"):
      val rootRow =
        rootRowsById.getOrElse(probeRow.id, fail(s"Missing root row for ${probeRow.id}"))
      val probe = StockfishProbe.probeFen(rootRow.fen)
      assert(probe.bestMove.nonEmpty, s"${probeRow.id} returned no best move")
      assert(probe.cp.nonEmpty || probe.mate.nonEmpty, s"${probeRow.id} returned no score line")
      assert(
        probe.mate.forall(mate => math.abs(mate) > probeRow.maxMatePly),
        s"${probeRow.id} collapses into a too-short mate according to Stockfish: ${probe.rawInfo.getOrElse("")}"
      )
      probeRow.maxAbsCp.foreach: maxAbsCp =>
        assert(
          probe.cp.forall(cp => math.abs(cp) <= maxAbsCp),
          s"${probeRow.id} exceeds the eval sanity bound $maxAbsCp: ${probe.rawInfo.getOrElse("")}"
        )

  private def loadRootRows(): Vector[RootFenRow] =
    val resourcePath = "/commentary-corpus/root-expectations.jsonl"
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(resourcePath))
          .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath"))
      )
    try
      source
        .getLines()
        .filter(_.nonEmpty)
        .zipWithIndex
        .map: (line, index) =>
          Json.parse(line).validate[RootFenRow].fold(
            errors => throw IllegalArgumentException(s"Invalid root row ${index + 1}: $errors"),
            identity
          )
        .toVector
    finally source.close()

  private def selectedProbeBucket(row: RootFenRow): String =
    row.schema match
      case "outpost_square" => outpostProbeBucket(row)
      case "candidate_passer" => candidatePasserProbeBucket(row)
      case "trapped_piece" => trappedPieceProbeBucket(row)
      case "king_shelter_hole" => kingShelterHoleProbeBucket(row)
      case other => fail(s"No selected engine probe bucket helper for $other")

  private def outpostProbeBucket(row: RootFenRow): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val subject = outpostSubjectSquare(row, polarity)
    val phase =
      if boardPieces(row.fen).exists { case (piece, _) => "Qq".contains(piece) } then "middlegame"
      else "queenless_transition"
    val topology =
      if Set('a', 'b', 'g', 'h').contains(subject.head) then "edge" else "center"

    row.caseType match
      case "exact" =>
        val supportShape = outpostSupportShape(row.fen, polarity, subject, row.id)
        s"$polarity|$phase|$topology|$supportShape|exact"
      case "near_miss" | "nasty_negative" =>
        val supportAvailable = hasStructuralPawnSupport(row.fen, polarity, subject)
        val failClosedState =
          if !supportAvailable then "no_support"
          else
            assert(
              enemyPawnCanChallengeSquare(row.fen, polarity, subject),
              s"Support-present outpost probe row does not restore enemy pawn challenge for ${row.id}"
            )
            "challenge_restored"
        val supportShape =
          if supportAvailable then outpostSupportShape(row.fen, polarity, subject, row.id)
          else "-"
        s"$polarity|$phase|$topology|$supportShape|$failClosedState"
      case other => fail(s"Unexpected probe case type for ${row.id}: $other")

  private def candidatePasserProbeBucket(row: RootFenRow): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val (subject, supportShape, balanceState) =
      row.caseType match
        case "exact" =>
          val subject =
            row.trueSquares match
              case List(square) => square
              case other => fail(s"Expected one true candidate-passer square for ${row.id}: $other")
          assert(
            candidatePasserFrontIsEmpty(row.fen, polarity, subject),
            s"Exact candidate-passer probe row does not have an empty front square for ${row.id}"
          )
          val supportCount = candidatePasserSupportPawns(row.fen, polarity, subject).size
          val oppositionCount = candidatePasserOppositionPawns(row.fen, polarity, subject).size
          assert(oppositionCount > 0, s"Exact candidate-passer probe row is already passed for ${row.id}")
          assert(
            supportCount >= oppositionCount,
            s"Exact candidate-passer probe row lost support/opposition balance for ${row.id}: support=$supportCount opposition=$oppositionCount"
          )
          (subject, candidatePasserSupportShape(row.fen, polarity, subject, row.id), "candidate_exact")
        case "near_miss" | "nasty_negative" =>
          val subject = candidatePasserSubjectSquare(row, polarity)
          assert(
            candidatePasserFrontIsEmpty(row.fen, polarity, subject),
            s"Fail-closed candidate-passer probe row must keep the front square empty so rejection stays on balance/pass status for ${row.id}"
          )
          val supportCount = candidatePasserSupportPawns(row.fen, polarity, subject).size
          val oppositionCount = candidatePasserOppositionPawns(row.fen, polarity, subject).size
          val state =
            if oppositionCount == 0 then "already_passed_negative"
            else
              assert(
                supportCount < oppositionCount,
                s"Under-supported candidate-passer probe row is not actually under-supported for ${row.id}: support=$supportCount opposition=$oppositionCount"
              )
              "under_supported_near_miss"
          (subject, "-", state)
        case other => fail(s"Unexpected candidate-passer probe case type for ${row.id}: $other")

    val phase =
      val piecePlacement = row.fen.takeWhile(_ != ' ')
      val nonKingNonPawnCount = piecePlacement.count(ch => "nbrqNBRQ".contains(ch))
      if nonKingNonPawnCount == 0 then "endgame" else "transition"
    val topology =
      if Set('a', 'b', 'g', 'h').contains(subject.head) then "edge" else "center"

    s"$polarity|$phase|$topology|$supportShape|$balanceState"

  private def trappedPieceProbeBucket(row: RootFenRow): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val subject =
      row.caseType match
        case "exact" =>
          row.trueSquares match
            case List(square) => square
            case other => fail(s"Expected one true trapped-piece square for ${row.id}: $other")
        case "near_miss" | "nasty_negative" =>
          trappedPieceSubjectSquare(row, polarity)
        case other => fail(s"Unexpected trapped-piece probe case type for ${row.id}: $other")

    val position = Fen.read(variant.Standard, Fen.Full.clean(row.fen)).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.fen}"))
    val square = chess.Square.fromKey(subject).getOrElse(fail(s"Invalid trapped-piece subject square for ${row.id}: $subject"))
    val subjectPiece = position.board.pieceAt(square).getOrElse(fail(s"Missing trapped-piece subject on board for ${row.id} at $subject"))
    val subjectColor = chessColor(polarity)
    assertEquals(subjectPiece.color, subjectColor, s"Trapped-piece probe subject has wrong polarity for ${row.id}")
    val pieceFamily = trappedPieceFamily(subjectPiece.role)

    row.caseType match
      case "exact" =>
        assert(subjectPiece.role != chess.King && subjectPiece.role != chess.Pawn, s"Exact trapped-piece probe row must be non-king non-pawn for ${row.id}")
        assert(
          independentLegalAttackers(position.board, square, !subjectColor).nonEmpty,
          s"Exact trapped-piece probe row must keep the subject currently attacked for ${row.id}"
        )
        val legalMoves = position.withColor(subjectColor).generateMovesAt(square).toVector
        val safeExits = trappedPieceSafeExitKeys(position, square, subjectColor)
        assert(safeExits.isEmpty, s"Exact trapped-piece probe row has safe exits for ${row.id}: $safeExits")
        val trapMode =
          if legalMoves.isEmpty then "zero_safe_moves" else "all_exits_lose"
        s"$polarity|$pieceFamily|$trapMode"
      case "near_miss" =>
        assert(subjectPiece.role != chess.King && subjectPiece.role != chess.Pawn, s"Near-miss trapped-piece probe row must be non-king non-pawn for ${row.id}")
        val safeExits = trappedPieceSafeExitKeys(position, square, subjectColor)
        assertEquals(safeExits.size, 1, s"Near-miss trapped-piece probe row must have exactly one safe exit for ${row.id}: $safeExits")
        s"$polarity|$pieceFamily|one_exit_near_miss"
      case "nasty_negative" =>
        assert(Set(chess.King, chess.Pawn).contains(subjectPiece.role), s"Nasty trapped-piece probe negative must be excluded by king/pawn category for ${row.id}")
        s"$polarity|$pieceFamily|nasty_negative"
      case other => fail(s"Unexpected trapped-piece probe case type for ${row.id}: $other")

  private def trappedPieceSafeExitKeys(position: chess.Position, square: chess.Square, color: chess.Color): Vector[String] =
    position
      .withColor(color)
      .generateMovesAt(square)
      .toVector
      .filter(move => independentOpponentBestExchangeNet(move.after.board, move.dest, !color) <= 0)
      .map(_.dest.key)
      .distinct

  private def kingShelterHoleProbeBucket(row: RootFenRow): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val beneficiary = chessColor(polarity)
    val defender = !beneficiary
    val position = Fen.read(variant.Standard, Fen.Full.clean(row.fen)).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.fen}"))
    val phase = kingShelterPhase(row.fen)

    row.caseType match
      case "exact" =>
        row.trueSquares match
          case List(subject) =>
            val square = chess.Square.fromKey(subject).getOrElse(fail(s"Invalid king-shelter probe square for ${row.id}: $subject"))
            assert(kingShelterHomeMask(position, defender).contains(square), s"Exact king-shelter probe row is outside home mask for ${row.id}")
            assert(!position.board.pieceAt(square).exists(piece => piece.color == defender && piece.role == chess.Pawn), s"Exact king-shelter probe row still has defender pawn cover for ${row.id}")
            assert(!kingShelterPawnControlled(position, defender, square), s"Exact king-shelter probe row is still pawn-controlled by defender for ${row.id}")
            assert(kingShelterPieceAttackOrAccess(position, beneficiary, square), s"Exact king-shelter probe row lacks attacker piece access for ${row.id}")
            s"$polarity|$phase|${kingShelterDefenderHome(polarity)}|${kingShelterHoleFile(subject)}|home_shelter_exact"
          case other => fail(s"Expected one true king-shelter square for ${row.id}: $other")
      case "near_miss" =>
        val (subject, failClosedState) = kingShelterNearMissSubject(row, position, beneficiary, defender)
        s"$polarity|$phase|${kingShelterDefenderHome(polarity)}|${kingShelterHoleFile(subject)}|$failClosedState"
      case "nasty_negative" =>
        assert(kingShelterHomeMask(position, defender).isEmpty, s"Nasty king-shelter probe row must be outside the defender home-shelter regime for ${row.id}")
        s"$polarity|$phase|out_of_regime|out_of_regime|out_of_regime_negative"
      case other => fail(s"Unexpected king-shelter probe case type for ${row.id}: $other")

  private def kingShelterNearMissSubject(
      row: RootFenRow,
      position: chess.Position,
      beneficiary: chess.Color,
      defender: chess.Color
  ): (String, String) =
    val mask = kingShelterHomeMask(position, defender)
    assert(mask.nonEmpty, s"Near-miss king-shelter probe row must remain in the defender home-shelter regime for ${row.id}")
    val pawnCovered =
      mask.filter: square =>
        kingShelterPieceAttackOrAccess(position, beneficiary, square) &&
          position.board.pieceAt(square).exists(piece => piece.color == defender && piece.role == chess.Pawn)
    if pawnCovered.nonEmpty then
      pawnCovered.toVector.map(_.key).distinct match
        case Vector(subject) => subject -> "pawn_cover_near_miss"
        case other => fail(s"Unable to derive unique pawn-cover king-shelter probe subject for ${row.id}: $other")
    else
      val noAccess =
        mask.filter: square =>
          !position.board.pieceAt(square).exists(piece => piece.color == defender && piece.role == chess.Pawn) &&
            !kingShelterPawnControlled(position, defender, square) &&
            !kingShelterPieceAttackOrAccess(position, beneficiary, square)
      noAccess.toVector.map(_.key).distinct match
        case Vector(subject) => subject -> "no_access_near_miss"
        case other => fail(s"Unable to derive unique no-access king-shelter probe subject for ${row.id}: $other")

  private def outpostSubjectSquare(row: RootFenRow, polarity: String): String =
    val candidates =
      boardPieces(row.fen).collect:
        case (piece, square)
            if pieceColor(piece) == polarity &&
              !"PpKk".contains(piece) &&
              isInEnemyTerritory(polarity, square) =>
          square
    candidates.distinct match
      case Vector(subject) => subject
      case other => fail(s"Unable to derive unique outpost subject for ${row.id}: $other")

  private def candidatePasserSubjectSquare(row: RootFenRow, polarity: String): String =
    val targetPawn = if polarity == "white" then 'P' else 'p'
    boardPieces(row.fen).collect:
      case (piece, square) if piece == targetPawn => square
    .distinct match
      case Vector(subject) => subject
      case other => fail(s"Unable to derive unique candidate-passer subject for ${row.id}: $other")

  private def trappedPieceSubjectSquare(row: RootFenRow, polarity: String): String =
    val position = Fen.read(variant.Standard, Fen.Full.clean(row.fen)).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.fen}"))
    val color = chessColor(polarity)
    val candidates =
      lila.commentary.root.RootAtomRegistry.canonicalSquares.collect:
        case square if position.board.pieceAt(square).exists(_.color == color) =>
          val piece = position.board.pieceAt(square).get
          val attacked = independentLegalAttackers(position.board, square, !color).nonEmpty
          val categoryMatches =
            row.caseType match
              case "near_miss" => piece.role != chess.King && piece.role != chess.Pawn
              case "nasty_negative" => piece.role == chess.King || piece.role == chess.Pawn
              case other => fail(s"Unexpected fail-closed trapped-piece probe case type for ${row.id}: $other")
          Option.when(attacked && categoryMatches)(square.key)
    candidates.flatten.toVector.distinct match
      case Vector(subject) => subject
      case other => fail(s"Unable to derive unique trapped-piece probe subject for ${row.id}: $other")

  private def outpostSupportShape(fen: String, polarity: String, subject: String, rowId: String): String =
    val directPawn = if polarity == "white" then 'P' else 'p'
    if pawnAttackSources(subject, polarity).exists(source => pieceAt(fen, source).contains(directPawn)) then "direct"
    else if hasStructuralPawnSupport(fen, polarity, subject) then "structural_path"
    else fail(s"Missing structural pawn support for outpost probe row $rowId at $subject")

  private def candidatePasserSupportShape(fen: String, polarity: String, subject: String, rowId: String): String =
    val supportPawns = candidatePasserSupportPawns(fen, polarity, subject)
    if supportPawns.exists(square => adjacentFileChars(subject.head).contains(square.head) && square.last == subject.last) then "same_rank_support"
    else if supportPawns.exists(square => isAheadOf(polarity, square, subject)) then "ahead_support"
    else fail(s"Missing candidate-passer support for probe row $rowId at $subject")

  private def candidatePasserFrontIsEmpty(fen: String, polarity: String, subject: String): Boolean =
    val rank = subject.last.asDigit
    val targetRank = if polarity == "white" then rank + 1 else rank - 1
    1 <= targetRank && targetRank <= 8 && pieceAt(fen, s"${subject.head}$targetRank").isEmpty

  private def candidatePasserSupportPawns(fen: String, polarity: String, subject: String): Vector[String] =
    val targetPawn = if polarity == "white" then 'P' else 'p'
    val adjacent = adjacentFileChars(subject.head)
    val relevantFiles = adjacent + subject.head
    boardPieces(fen).collect:
      case (piece, square)
          if piece == targetPawn &&
            square != subject &&
            relevantFiles.contains(square.head) &&
            (isAheadOf(polarity, square, subject) || (adjacent.contains(square.head) && square.last == subject.last)) =>
        square

  private def candidatePasserOppositionPawns(fen: String, polarity: String, subject: String): Vector[String] =
    val targetPawn = if polarity == "white" then 'p' else 'P'
    val relevantFiles = adjacentFileChars(subject.head) + subject.head
    boardPieces(fen).collect:
      case (piece, square)
          if piece == targetPawn &&
            relevantFiles.contains(square.head) &&
            isAheadOf(polarity, square, subject) =>
        square

  private def adjacentFileChars(file: Char): Set[Char] =
    Vector(file - 1, file + 1).collect:
      case value if 'a' <= value.toChar && value.toChar <= 'h' => value.toChar
    .toSet

  private def isAheadOf(polarity: String, candidate: String, reference: String): Boolean =
    if polarity == "white" then candidate.last.asDigit > reference.last.asDigit
    else candidate.last.asDigit < reference.last.asDigit

  private def independentOpponentBestExchangeNet(boardState: chess.Board, square: chess.Square, attacker: chess.Color): Int =
    boardState.pieceAt(square).fold(0): occupant =>
      independentLegalAttackers(boardState, square, attacker)
        .map: origin =>
          val afterCapture = boardState.taking(origin, square).get
          roleValue(occupant.role) - independentOpponentBestExchangeNet(afterCapture, square, !attacker)
        .foldLeft(0)(math.max)

  private def independentLegalAttackers(boardState: chess.Board, square: chess.Square, attacker: chess.Color): Vector[chess.Square] =
    val attackerPosition = chess.Position(boardState, variant.Standard, attacker)
    boardState
      .attackers(square, attacker)
      .filter(origin => attackerPosition.generateMovesAt(origin).exists(_.dest == square))
      .toVector

  private def roleValue(role: chess.Role): Int = role match
    case chess.Pawn => 100
    case chess.Knight | chess.Bishop => 300
    case chess.Rook => 500
    case chess.Queen => 900
    case chess.King => 10_000

  private def trappedPieceFamily(role: chess.Role): String =
    role match
      case chess.Pawn => "pawn"
      case chess.Knight | chess.Bishop => "minor"
      case chess.Rook => "rook"
      case chess.Queen => "queen"
      case chess.King => "king"

  private def chessColor(polarity: String): chess.Color =
    if polarity == "white" then chess.Color.White else chess.Color.Black

  private def kingShelterPhase(fen: String): String =
    val nonKingNonPawnCount = boardPieces(fen).count { case (piece, _) => "nbrqNBRQ".contains(piece) }
    if nonKingNonPawnCount >= 2 then "middlegame" else "opening"

  private def kingShelterDefenderHome(polarity: String): String =
    if polarity == "white" then "black_home" else "white_home"

  private def kingShelterHoleFile(square: String): String =
    if Set('d', 'e').contains(square.head) then "center_shield" else "wing_shield"

  private def kingShelterHomeMask(position: chess.Position, defender: chess.Color): Vector[chess.Square] =
    position.board.kingPosOf(defender).toVector.flatMap: king =>
      val homeRegime =
        defender.fold(king.rank <= chess.Rank.Second, king.rank >= chess.Rank.Seventh)
      if !homeRegime then Vector.empty
      else
        (for
          rankStep <- Vector(1, 2)
          fileOffset <- Vector(-1, 0, 1)
          square <- chess.Square.at(
            king.file.value + fileOffset,
            king.rank.value + defender.fold(rankStep, -rankStep)
          )
        yield square).toVector

  private def kingShelterPawnControlled(position: chess.Position, defender: chess.Color, square: chess.Square): Boolean =
    square.pawnAttacks(!defender).exists: source =>
      position.board.pieceAt(source).exists(piece => piece.color == defender && piece.role == chess.Pawn)

  private def kingShelterPieceAttackOrAccess(position: chess.Position, beneficiary: chess.Color, square: chess.Square): Boolean =
    position.board.pieceAt(square).exists(piece => piece.color == beneficiary && piece.role != chess.Pawn && piece.role != chess.King) ||
      position.board.attackers(square, beneficiary).exists: source =>
        position.board.pieceAt(source).exists(piece => piece.color == beneficiary && piece.role != chess.Pawn && piece.role != chess.King)

  private def enemyPawnCanChallengeSquare(fen: String, beneficiary: String, target: String): Boolean =
    val enemy = opposite(beneficiary)
    pawnAttackSources(target, enemy).exists: source =>
      pawnsOnFile(fen, enemy, source.head).exists(origin => canPawnStructurallyReach(fen, origin, source, enemy))

  private def hasStructuralPawnSupport(fen: String, polarity: String, target: String): Boolean =
    pawnAttackSources(target, polarity).exists: supportSquare =>
      pawnsOnFile(fen, polarity, supportSquare.head).exists(origin => canPawnStructurallyReach(fen, origin, supportSquare, polarity))

  private def pawnsOnFile(fen: String, polarity: String, file: Char): Vector[String] =
    val pawn = if polarity == "white" then 'P' else 'p'
    boardPieces(fen).collect:
      case (piece, square) if piece == pawn && square.head == file => square

  private def pawnAttackSources(target: String, attackerColor: String): Vector[String] =
    val file = target.head - 'a'
    val rank = target.last.asDigit
    val sourceRank = if attackerColor == "white" then rank - 1 else rank + 1
    Vector(file - 1, file + 1).collect:
      case sourceFile if 0 <= sourceFile && sourceFile < 8 && 1 <= sourceRank && sourceRank <= 8 =>
        s"${('a' + sourceFile).toChar}$sourceRank"

  private def canPawnStructurallyReach(fen: String, origin: String, target: String, polarity: String): Boolean =
    if origin.head != target.head then false
    else
      val originRank = origin.last.asDigit
      val targetRank = target.last.asDigit
      val distance = if polarity == "white" then targetRank - originRank else originRank - targetRank
      if distance < 0 then false
      else if distance == 0 then pieceAt(fen, origin).contains(if polarity == "white" then 'P' else 'p')
      else
        pieceAt(fen, target).isEmpty &&
          squaresBetweenOnFile(origin, target).forall(square => pieceAt(fen, square).isEmpty)

  private def squaresBetweenOnFile(origin: String, target: String): Vector[String] =
    val file = origin.head
    val originRank = origin.last.asDigit
    val targetRank = target.last.asDigit
    val step = if targetRank > originRank then 1 else -1
    ((originRank + step) until targetRank by step).map(rank => s"$file$rank").toVector

  private def pieceAt(fen: String, square: String): Option[Char] =
    boardPieces(fen).collectFirst:
      case (piece, `square`) => piece

  private def pieceColor(piece: Char): String =
    if piece.isUpper then "white" else "black"

  private def opposite(polarity: String): String =
    if polarity == "white" then "black" else "white"

  private def isInEnemyTerritory(polarity: String, square: String): Boolean =
    val rank = square.last.asDigit
    if polarity == "white" then rank >= 5 else rank <= 4

  private def boardPieces(fen: String): Vector[(Char, String)] =
    fen
      .split(' ')
      .head
      .split('/')
      .zipWithIndex
      .iterator
      .flatMap: (rankText, rankIndex) =>
        val expanded = rankText.flatMap(ch => if ch.isDigit then "." * ch.asDigit else ch.toString)
        expanded.zipWithIndex.collect:
          case (piece, fileIndex) if piece != '.' => (piece, s"${('a' + fileIndex).toChar}${8 - rankIndex}")
      .toVector

  private final case class RootFenRow(
      id: String,
      schema: String,
      caseType: String,
      fen: String,
      polarityColor: Option[String],
      trueSquares: List[String]
  )

  private given Reads[RootFenRow] =
    (
      (__ \ "id").read[String] and
        (__ \ "schema").read[String] and
        (__ \ "caseType").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "polarityColor").readNullable[String] and
        (__ \ "trueSquares").readWithDefault[List[String]](Nil)
    )(RootFenRow.apply)
