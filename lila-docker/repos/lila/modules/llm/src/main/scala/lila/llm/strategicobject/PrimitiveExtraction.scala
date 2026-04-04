package lila.llm.strategicobject

import chess.{ Bishop, Board, Color, File, King, Knight, Pawn, Queen, Rank, Rook, Role, Square }
import lila.llm.analysis.{ DecisiveTruthContract, MoveTruthFrame }
import lila.llm.model.Fact

trait PrimitiveExtractor:
  def extract(
      evidence: RawPositionEvidence,
      truth: MoveTruthFrame,
      contract: DecisiveTruthContract
  ): PrimitiveBank

object CanonicalPrimitiveExtractor extends PrimitiveExtractor:

  private val Owners = List(Color.White, Color.Black)

  def extract(
      evidence: RawPositionEvidence,
      truth: MoveTruthFrame,
      contract: DecisiveTruthContract
  ): PrimitiveBank =
    val board = evidence.board
    val breakCandidates = extractBreakCandidates(board)
    val accessRoutes = extractAccessRoutes(board)
    val routeContestSeeds = extractRouteContestSeeds(board, accessRoutes)
    val passerSeeds = extractPasserSeeds(board)
    PrimitiveBank(
      targetSquares = extractTargetSquares(evidence),
      breakCandidates = breakCandidates,
      routeContestSeeds = routeContestSeeds,
      exchangeSquares = extractExchangeSquares(board),
      accessRoutes = accessRoutes,
      defendedResources = extractDefendedResources(evidence),
      pieceRoleIssues = extractPieceRoleIssues(board),
      criticalSquares = extractCriticalSquares(board, breakCandidates, passerSeeds),
      passerSeeds = passerSeeds
    ).normalized

  private def extractTargetSquares(evidence: RawPositionEvidence): List[TargetSquare] =
    val board = evidence.board
    val factualTargets =
      Owners.flatMap { owner =>
        val enemy = !owner
        evidence.factsFor(enemy).flatMap {
          case Fact.TargetPiece(square, role, attackers, defenders, _)
              if keepTargetSquare(board, owner, square, role, attackers.size, defenders.size) =>
            Some(
              TargetSquare(
                owner = owner,
                square = square,
                targetOwner = enemy,
                occupant = Some(role),
                attackerCount = attackers.size,
                defenderCount = defenders.size,
                fixed = isFixedTarget(board, owner, square, enemy)
              )
            )
          case Fact.HangingPiece(square, role, attackers, defenders, _)
              if keepTargetSquare(board, owner, square, role, attackers.size, defenders.size) =>
            Some(
              TargetSquare(
                owner = owner,
                square = square,
                targetOwner = enemy,
                occupant = Some(role),
                attackerCount = attackers.size,
                defenderCount = defenders.size,
                fixed = isFixedTarget(board, owner, square, enemy)
              )
            )
          case _ => None
        }
      }

    val structuralTargets =
      Owners.flatMap { owner =>
        val enemy = !owner
        board.byPiece(enemy, Pawn).squares.flatMap { square =>
          Option.when(isStructuralTargetPawn(board, owner, square, enemy)) {
            TargetSquare(
              owner = owner,
              square = square,
              targetOwner = enemy,
              occupant = Some(Pawn),
              attackerCount = countAttackers(board, square, owner),
              defenderCount = countAttackers(board, square, enemy),
              fixed = isFixedTarget(board, owner, square, enemy)
            )
          }
        }
      }

    factualTargets ++ structuralTargets

  private def extractBreakCandidates(board: Board): List[BreakCandidate] =
    Owners.flatMap { owner =>
      board.byPiece(owner, Pawn).squares.flatMap { square =>
        val pushTarget =
          forwardSquare(square, owner).filter { next =>
            board.pieceAt(next).isEmpty && pawnPressureTargets(board, owner, next).nonEmpty
          }
        val captureTargets =
          square
            .pawnAttacks(owner)
            .squares
            .filter(target => board.pieceAt(target).exists(piece => piece.color != owner && piece.role == Pawn))

        pushTarget.map { target =>
          BreakCandidate(
            owner = owner,
            file = square.file,
            breakSquare = target,
            targetSquares = pawnPressureTargets(board, owner, target),
            supportCount = countAttackers(board, target, owner),
            resistanceCount = countAttackers(board, target, !owner)
          )
        }.toList ++
          captureTargets.map { target =>
            BreakCandidate(
              owner = owner,
              file = square.file,
              breakSquare = target,
              targetSquares = List(target),
              supportCount = countAttackers(board, target, owner),
              resistanceCount = countAttackers(board, target, !owner)
            )
          }
      }
    }

  private def extractAccessRoutes(board: Board): List[AccessRoute] =
    Owners.flatMap { owner =>
      File.all.flatMap { file =>
        val routeRoles =
          board
            .byColor(owner)
            .squares
            .filter(sq => sq.file == file)
            .flatMap(board.roleAt)
            .filter(role => role == Rook || role == Queen)
            .toSet
        Option.when(routeRoles.nonEmpty && (isOpenFile(board, file) || isSemiOpenFileFor(board, file, owner))) {
          AccessRoute(
            owner = owner,
            file = file,
            roles = routeRoles
          )
        }
      }
    }

  private def extractRouteContestSeeds(
      board: Board,
      accessRoutes: List[AccessRoute]
  ): List[RouteContestSeed] =
    accessRoutes.flatMap { route =>
      candidateRouteSquares(board, route.owner, route.file).flatMap { square =>
        val attackerCount = countAttackers(board, square, route.owner)
        val defenderCount = countAttackers(board, square, !route.owner)
        Option.when(attackerCount > 0 && attackerCount >= defenderCount) {
          RouteContestSeed(
            owner = route.owner,
            square = square,
            lane = route.file,
            supportingRoles = route.roles,
            attackerCount = attackerCount,
            defenderCount = defenderCount
          )
        }
      }
    }

  private def extractExchangeSquares(board: Board): List[ExchangeSquare] =
    Owners.flatMap { owner =>
      val enemy = !owner
      board.byColor(enemy).squares.flatMap { square =>
        board.roleAt(square).flatMap { role =>
          val attackerRoles = attackerRolesOn(board, square, owner)
          val defenderRoles = attackerRolesOn(board, square, enemy)
          val leastAttacker = attackerRoles.map(pieceValue).minOption
          Option.when(
            role != King &&
              attackerRoles.nonEmpty &&
              defenderRoles.nonEmpty &&
              leastAttacker.exists(_ <= pieceValue(role)) &&
              isMeaningfulExchangeSquare(square, role)
          ) {
            ExchangeSquare(
              owner = owner,
              square = square,
              targetOwner = enemy,
              occupant = role,
              attackerCount = attackerRoles.size,
              defenderCount = defenderRoles.size
            )
          }
        }
      }
    }

  private def extractDefendedResources(evidence: RawPositionEvidence): List[DefendedResource] =
    Owners.flatMap { owner =>
      evidence.factsFor(owner).flatMap {
        case Fact.TargetPiece(square, role, attackers, defenders, _)
            if attackers.nonEmpty && defenders.nonEmpty && keepDefendedResource(square, role) =>
          Some(
            DefendedResource(
              owner = owner,
              square = square,
              role = role,
              attackerCount = attackers.size,
              defenderCount = defenders.size
            )
          )
        case Fact.HangingPiece(square, role, attackers, defenders, _)
            if attackers.nonEmpty && defenders.nonEmpty && keepDefendedResource(square, role) =>
          Some(
            DefendedResource(
              owner = owner,
              square = square,
              role = role,
              attackerCount = attackers.size,
              defenderCount = defenders.size
            )
          )
        case _ => None
      }
    }

  private def extractPieceRoleIssues(board: Board): List[PieceRoleIssue] =
    Owners.flatMap { owner =>
      board.byColor(owner).squares.flatMap { square =>
        board.pieceAt(square).flatMap { piece =>
          piece.role match
            case Bishop if isBadBishop(board, square, owner) =>
              Some(PieceRoleIssue(owner = owner, square = square, role = piece.role, issue = PieceRoleIssueKind.BadBishop))
            case role if role != Pawn && role != King && isTrappedPiece(board, square, owner, role) =>
              Some(
                PieceRoleIssue(
                  owner = owner,
                  square = square,
                  role = piece.role,
                  issue = PieceRoleIssueKind.TrappedPiece
                )
              )
            case _ => None
        }
      }
    }

  private def extractCriticalSquares(
      board: Board,
      breakCandidates: List[BreakCandidate],
      passerSeeds: List[PasserSeed]
  ): List[CriticalSquare] =
    val outposts =
      Owners.flatMap { owner =>
        Square.all.flatMap { square =>
          Option.when(isOutpostSquare(board, square, owner)) {
            CriticalSquare(
              owner = owner,
              square = square,
              kind = CriticalSquareKind.Outpost,
              pressure = countAttackers(board, square, owner)
            )
          }
        }
      }
    val promotionSquares =
      passerSeeds.flatMap { seed =>
        promotionSquare(seed.square, seed.owner).map { square =>
          CriticalSquare(
            owner = seed.owner,
            square = square,
            kind = CriticalSquareKind.PromotionSquare,
            pressure = countAttackers(board, square, seed.owner)
          )
        }
      }
    val breakContacts =
      breakCandidates.map { axis =>
        CriticalSquare(
          owner = axis.owner,
          square = axis.breakSquare,
          kind = CriticalSquareKind.BreakContact,
          pressure = countAttackers(board, axis.breakSquare, axis.owner)
        )
      }
    outposts ++ promotionSquares ++ breakContacts

  private def extractPasserSeeds(board: Board): List[PasserSeed] =
    Owners.flatMap { owner =>
      board.byPiece(owner, Pawn).squares.flatMap { square =>
        Option.when(isPassedPawn(board, square, owner)) {
          PasserSeed(
            owner = owner,
            square = square,
            protectedByPawn = isProtectedByPawn(board, square, owner),
            relativeRank = relativeRank(square, owner)
          )
        }
      }
    }

  private def keepTargetSquare(
      board: Board,
      owner: Color,
      square: Square,
      role: Role,
      attackers: Int,
      defenders: Int
  ): Boolean =
    role == Pawn && (attackers >= defenders || isStructuralTargetPawn(board, owner, square, !owner)) ||
      role != Pawn && role != King && isMeaningfulExchangeSquare(square, role)

  private def keepDefendedResource(square: Square, role: Role): Boolean =
    role == Pawn || isMeaningfulExchangeSquare(square, role)

  private def isMeaningfulExchangeSquare(square: Square, role: Role): Boolean =
    role != King &&
      (role != Pawn || square.file == File.C || square.file == File.D || square.file == File.E || square.file == File.F)

  private def isStructuralTargetPawn(
      board: Board,
      owner: Color,
      square: Square,
      pawnOwner: Color
  ): Boolean =
    board.roleAt(square).contains(Pawn) &&
      (
        isIsolatedPawn(board, square, pawnOwner) ||
          isBackwardPawn(board, square, pawnOwner) ||
          isFixedTarget(board, owner, square, pawnOwner) ||
          isRearFixedChainTarget(board, owner, square, pawnOwner)
      )

  private def isFixedTarget(
      board: Board,
      owner: Color,
      square: Square,
      pawnOwner: Color
  ): Boolean =
    forwardSquare(square, pawnOwner).exists { next =>
      board.pieceAt(next).exists(_.color == owner) || countAttackers(board, next, owner) > 0
    }

  private def isRearFixedChainTarget(
      board: Board,
      owner: Color,
      square: Square,
      pawnOwner: Color
  ): Boolean =
    pawnSupportSquares(square, pawnOwner).exists { front =>
      board.pieceAt(front).exists(piece => piece.color == pawnOwner && piece.role == Pawn) &&
      isFixedTarget(board, owner, front, pawnOwner)
    }

  private def isIsolatedPawn(board: Board, square: Square, color: Color): Boolean =
    adjacentFiles(square.file).forall { file =>
      board.byPiece(color, Pawn).squares.forall(_.file != file)
    }

  private def isBackwardPawn(board: Board, square: Square, color: Color): Boolean =
    val next = forwardSquare(square, color)
    val adjacentSupport =
      adjacentFiles(square.file).exists { file =>
        board.byPiece(color, Pawn).squares.exists { pawn =>
          pawn.file == file && relativeRank(pawn, color) >= relativeRank(square, color)
        }
      }
    next.exists(target => board.pieceAt(target).isEmpty && countAttackers(board, target, !color) > 0) &&
      !adjacentSupport

  private def pawnSupportSquares(square: Square, color: Color): List[Square] =
    val forwardDelta = if color.white then 1 else -1
    List(-1, 1).flatMap { fileDelta =>
      Square.at(square.file.value + fileDelta, square.rank.value + forwardDelta)
    }

  private def pawnPressureTargets(board: Board, owner: Color, pushedSquare: Square): List[Square] =
    pushedSquare
      .pawnAttacks(owner)
      .squares
      .filter(target => board.pieceAt(target).exists(piece => piece.color != owner && piece.role == Pawn))

  private def candidateRouteSquares(board: Board, owner: Color, file: File): List[Square] =
    squaresOnFile(file)
      .filter(square => inEnemyHalf(square, owner) && board.pieceAt(square).isEmpty)
      .sortBy(square => (-relativeRank(square, owner), -countAttackers(board, square, owner)))

  private def squaresOnFile(file: File): List[Square] =
    Square.all.filter(_.file == file)

  private def inEnemyHalf(square: Square, owner: Color): Boolean =
    if owner.white then square.rank.value >= Rank.Fifth.value else square.rank.value <= Rank.Fourth.value

  private def isOpenFile(board: Board, file: File): Boolean =
    board.pawns.squares.forall(_.file != file)

  private def isSemiOpenFileFor(board: Board, file: File, owner: Color): Boolean =
    board.byPiece(owner, Pawn).squares.forall(_.file != file)

  private def isOutpostSquare(board: Board, square: Square, owner: Color): Boolean =
    inEnemyHalf(square, owner) &&
      enemyPawnAttackers(board, square, !owner).isEmpty &&
      (countAttackers(board, square, owner) >= 1) &&
      board.pieceAt(square).forall(piece => piece.color == owner)

  private def enemyPawnAttackers(board: Board, square: Square, color: Color): List[Square] =
    board.byPiece(color, Pawn).squares.filter(pawn => pawn.pawnAttacks(color).contains(square))

  private def isPassedPawn(board: Board, square: Square, color: Color): Boolean =
    val enemy = !color
    val filesToCheck = List(square.file.value - 1, square.file.value, square.file.value + 1).filter(i => i >= 0 && i <= 7)
    filesToCheck.forall { fileIndex =>
      File.all.lift(fileIndex).forall { file =>
        board.byPiece(enemy, Pawn).squares
          .filter(_.file == file)
          .forall { enemyPawn =>
            if color.white then enemyPawn.rank.value <= square.rank.value
            else enemyPawn.rank.value >= square.rank.value
          }
      }
    }

  private def isProtectedByPawn(board: Board, square: Square, color: Color): Boolean =
    board.byPiece(color, Pawn).squares.exists(pawn => pawn != square && pawn.pawnAttacks(color).contains(square))

  private def isBadBishop(board: Board, square: Square, owner: Color): Boolean =
    val sameColorPawns =
      board.byPiece(owner, Pawn).squares.count(_.isLight == square.isLight)
    sameColorPawns >= 3 && mobility(board, square, Bishop, owner) <= 4

  private def isTrappedPiece(board: Board, square: Square, owner: Color, role: Role): Boolean =
    mobility(board, square, role, owner) <= 1 && countAttackers(board, square, !owner) > 0

  private def mobility(board: Board, square: Square, role: Role, owner: Color): Int =
    val occupied = board.occupied
    val rawTargets = role match
      case Pawn   => square.pawnAttacks(owner)
      case Knight => square.knightAttacks
      case Bishop => square.bishopAttacks(occupied)
      case Rook   => square.rookAttacks(occupied)
      case Queen  => square.queenAttacks(occupied)
      case King   => square.kingAttacks
    (rawTargets & ~board.byColor(owner)).count

  private def attackerRolesOn(board: Board, square: Square, color: Color): List[Role] =
    board.attackers(square, color).squares.flatMap(board.roleAt)

  private def countAttackers(board: Board, square: Square, color: Color): Int =
    board.attackers(square, color).count

  private def forwardSquare(square: Square, color: Color): Option[Square] =
    Square.at(square.file.value, square.rank.value + (if color.white then 1 else -1))

  private def promotionSquare(square: Square, color: Color): Option[Square] =
    Square.at(square.file.value, if color.white then Rank.Eighth.value else Rank.First.value)

  private def adjacentFiles(file: File): List[File] =
    List(file.value - 1, file.value + 1).flatMap(File.all.lift)

  private def relativeRank(square: Square, color: Color): Int =
    if color.white then square.rank.value + 1 else 8 - square.rank.value

  private def pieceValue(role: Role): Int = role match
    case Pawn   => 1
    case Knight => 3
    case Bishop => 3
    case Rook   => 5
    case Queen  => 9
    case King   => 100
