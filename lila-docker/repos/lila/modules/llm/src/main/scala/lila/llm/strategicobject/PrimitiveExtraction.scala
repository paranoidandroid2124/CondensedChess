package lila.llm.strategicobject

import chess.{ Bishop, Bitboard, Board, Color, File, King, Knight, Pawn, Piece, Queen, Rank, Rook, Role, Square }
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
    val targetSquares = extractTargetSquares(evidence)
    val breakCandidates = extractBreakCandidates(board)
    val exchangeSquares = extractExchangeSquares(board)
    val accessRoutes = extractAccessRoutes(board)
    val routeContestSeeds = extractRouteContestSeeds(board, accessRoutes)
    val defendedResources = extractDefendedResources(evidence)
    val pieceRoleIssues = extractPieceRoleIssues(board)
    val passerSeeds = extractPasserSeeds(board)
    val criticalSquares = extractCriticalSquares(board, breakCandidates, passerSeeds)
    val routeAnchors = buildRouteAnchors(board, targetSquares, exchangeSquares, criticalSquares)
    val leverContactSeeds = extractLeverContactSeeds(board)
    val hookContactSeeds = extractHookContactSeeds(board)
    val tensionContactSeeds = extractTensionContactSeeds(board, leverContactSeeds)
    val releaseCandidates = extractReleaseCandidates(board, breakCandidates, leverContactSeeds, tensionContactSeeds)
    val counterplayResourceSeeds =
      extractCounterplayResourceSeeds(board, routeAnchors, breakCandidates, leverContactSeeds)

    PrimitiveBank(
      targetSquares = targetSquares,
      breakCandidates = breakCandidates,
      routeContestSeeds = routeContestSeeds,
      exchangeSquares = exchangeSquares,
      accessRoutes = accessRoutes,
      defendedResources = defendedResources,
      pieceRoleIssues = pieceRoleIssues,
      criticalSquares = criticalSquares,
      passerSeeds = passerSeeds,
      diagonalLaneSeeds = extractDiagonalLaneSeeds(board, routeAnchors),
      liftCorridorSeeds = extractLiftCorridorSeeds(board, routeAnchors),
      knightRouteSeeds = extractKnightRouteSeeds(board, routeAnchors),
      redeploymentPathSeeds = extractRedeploymentPathSeeds(board, routeAnchors, pieceRoleIssues),
      leverContactSeeds = leverContactSeeds,
      hookContactSeeds = hookContactSeeds,
      counterplayResourceSeeds = counterplayResourceSeeds,
      tensionContactSeeds = tensionContactSeeds,
      releaseCandidates = releaseCandidates
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
            .filter(target => board.pieceAt(target).contains(Piece(!owner, Pawn)))

        pushTarget.map { target =>
          BreakCandidate(
            owner = owner,
            sourceSquare = square,
            file = square.file,
            breakSquare = target,
            mode = BreakMode.Push,
            targetSquares = pawnPressureTargets(board, owner, target).sortBy(_.key),
            supportCount = countAttackers(board, target, owner),
            resistanceCount = countAttackers(board, target, !owner)
          )
        }.toList ++
          captureTargets.map { target =>
            BreakCandidate(
              owner = owner,
              sourceSquare = square,
              file = square.file,
              breakSquare = target,
              mode = BreakMode.Capture,
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
        val carrierSquares =
          board.byColor(owner).squares
            .filter(sq => sq.file == file)
            .filter(sq => board.roleAt(sq).exists(role => role == Rook || role == Queen))
            .sortBy(_.key)
        val routeRoles = carrierSquares.flatMap(board.roleAt).toSet
        Option.when(carrierSquares.nonEmpty && (isOpenFile(board, file) || isSemiOpenFileFor(board, file, owner))) {
          AccessRoute(
            owner = owner,
            file = file,
            carrierSquares = carrierSquares,
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
            carrierSquares = route.carrierSquares,
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

  private def extractDiagonalLaneSeeds(
      board: Board,
      routeAnchors: Map[Color, List[Square]]
  ): List[DiagonalLaneSeed] =
    Owners.flatMap { owner =>
      board.byColor(owner).squares.flatMap { origin =>
        board.pieceAt(origin).toList.flatMap { piece =>
          if piece.role != Bishop && piece.role != Queen then Nil
          else
            routeAnchors(owner)
              .flatMap { target =>
                val attackerCount = countAttackers(board, target, owner)
                val defenderCount = countAttackers(board, target, !owner)
                Option.when(
                  target != origin &&
                    board.pieceAt(target).forall(_.color != owner) &&
                    sameDiagonal(origin, target) &&
                    attackMaskFrom(piece.role, origin, board.occupied, owner).contains(target) &&
                    keepRouteAnchor(board, owner, target, attackerCount, defenderCount)
                ) {
                  DiagonalLaneSeed(
                    owner = owner,
                    origin = origin,
                    target = target,
                    role = piece.role,
                    attackerCount = attackerCount,
                    defenderCount = defenderCount
                  )
                }
              }
              .sortBy(seed => (routeAnchorPriority(board, owner, seed.target), geometryDistance(seed.origin, seed.target)))
              .take(2)
        }
      }
    }

  private def extractLiftCorridorSeeds(
      board: Board,
      routeAnchors: Map[Color, List[Square]]
  ): List[LiftCorridorSeed] =
    Owners.flatMap { owner =>
      board.byPiece(owner, Rook).squares.flatMap { origin =>
        if relativeRank(origin, owner) > 2 then Nil
        else
          val candidates =
            attackMaskFrom(Rook, origin, board.occupied, owner).squares
              .filter(sq => sq.file == origin.file && board.pieceAt(sq).isEmpty && isLiftCorridorSquare(sq, owner))
              .flatMap { liftSquare =>
                routeAnchors(owner).flatMap { target =>
                  val attackerCount = countAttackers(board, target, owner)
                  val defenderCount = countAttackers(board, target, !owner)
                  Option.when(
                    target != liftSquare &&
                      target.rank == liftSquare.rank &&
                      horizontalDistance(liftSquare, target) >= 2 &&
                      board.pieceAt(target).forall(_.color != owner) &&
                      clearOrthogonalPath(board, liftSquare, target) &&
                      keepRouteAnchor(board, owner, target, attackerCount, defenderCount)
                  ) {
                    LiftCorridorSeed(
                      owner = owner,
                      origin = origin,
                      liftSquare = liftSquare,
                      target = target,
                      attackerCount = attackerCount,
                      defenderCount = defenderCount
                    )
                  }
                }
              }
          candidates
            .sortBy(seed => (routeAnchorPriority(board, owner, seed.target), geometryDistance(seed.liftSquare, seed.target)))
            .take(2)
      }
    }

  private def extractKnightRouteSeeds(
      board: Board,
      routeAnchors: Map[Color, List[Square]]
  ): List[KnightRouteSeed] =
    Owners.flatMap { owner =>
      board.byPiece(owner, Knight).squares.flatMap { origin =>
        val currentAttacks = attackMaskFrom(Knight, origin, board.occupied, owner)
        val candidates =
          routeAnchors(owner).flatMap { target =>
            if currentAttacks.contains(target) then Nil
            else
              origin.knightAttacks.squares.flatMap { via =>
                val attackerCount = countAttackers(board, target, owner)
                val defenderCount = countAttackers(board, target, !owner)
                Option.when(
                  board.pieceAt(via).isEmpty &&
                    isOperationalSquare(via, owner) &&
                    isStableSquare(board, via, owner) &&
                    via.knightAttacks.contains(target) &&
                    keepRouteAnchor(board, owner, target, attackerCount, defenderCount)
                ) {
                  KnightRouteSeed(
                    owner = owner,
                    origin = origin,
                    via = via,
                    target = target,
                    attackerCount = attackerCount,
                    defenderCount = defenderCount
                  )
                }
              }
          }
        candidates
          .sortBy(seed => (routeAnchorPriority(board, owner, seed.target), geometryDistance(seed.origin, seed.via)))
          .take(2)
      }
    }

  private def extractRedeploymentPathSeeds(
      board: Board,
      routeAnchors: Map[Color, List[Square]],
      pieceRoleIssues: List[PieceRoleIssue]
  ): List[RedeploymentPathSeed] =
    val badBishopSquares =
      pieceRoleIssues.collect {
        case PieceRoleIssue(owner, square, Bishop, PieceRoleIssueKind.BadBishop) => (owner, square)
      }.toSet

    Owners.flatMap { owner =>
      board.byPiece(owner, Bishop).squares.flatMap { origin =>
        val currentMobility = mobility(board, origin, Bishop, owner)
        if !badBishopSquares.contains((owner, origin)) && currentMobility > 4 then Nil
        else
          val occupiedAfterMove = occupiedWithout(board, origin)
          val candidates =
            attackMaskFrom(Bishop, origin, board.occupied, owner).squares
              .filter(sq => board.pieceAt(sq).isEmpty && isOperationalSquare(sq, owner) && isStableSquare(board, sq, owner))
              .flatMap { via =>
                val projected = projectedMobility(board, via, Bishop, owner, origin)
                routeAnchors(owner).flatMap { target =>
                  Option.when(
                    target != via &&
                      !attackMaskFrom(Bishop, origin, board.occupied, owner).contains(target) &&
                      attackMaskFrom(Bishop, via, occupiedAfterMove, owner).contains(target) &&
                      projected > currentMobility
                  ) {
                    RedeploymentPathSeed(
                      owner = owner,
                      origin = origin,
                      via = via,
                      target = target,
                      role = Bishop,
                      mobilityGain = projected - currentMobility
                    )
                  }
                }
              }
          candidates
            .sortBy(seed => (routeAnchorPriority(board, owner, seed.target), -seed.mobilityGain, geometryDistance(seed.origin, seed.via)))
            .take(2)
      }
    }

  private def extractLeverContactSeeds(board: Board): List[LeverContactSeed] =
    pawnContactPairs(board).flatMap { contact =>
      val flank = isFlankFile(contact.from.file) || isFlankFile(contact.target.file)
      val supportCount = countAttackers(board, contact.target, contact.owner)
      val resistanceCount = countAttackers(board, contact.target, !contact.owner)
      val structured =
        hasContactSupport(board, contact.owner, contact.from, contact.target) &&
          (flank || isCentralFile(contact.from.file) || relativeRank(contact.from, contact.owner) >= 4) &&
          supportCount >= resistanceCount
      Option.when(structured) {
        LeverContactSeed(
          owner = contact.owner,
          from = contact.from,
          target = contact.target,
          supportCount = supportCount,
          resistanceCount = resistanceCount,
          flank = flank
        )
      }
    }

  private def extractHookContactSeeds(board: Board): List[HookContactSeed] =
    Owners.flatMap { owner =>
      board.byPiece(owner, Pawn).squares.flatMap { from =>
        forwardSquare(from, owner)
          .filter(next => board.pieceAt(next).isEmpty && isFlankFile(next.file))
          .toList
          .flatMap { createSquare =>
            createSquare.pawnAttacks(owner).squares.flatMap { target =>
              val supportCount = countAttackers(board, createSquare, owner)
              val resistanceCount = countAttackers(board, createSquare, !owner)
              Option.when(
                board.pieceAt(target).contains(Piece(!owner, Pawn)) &&
                  isFlankFile(target.file) &&
                  relativeRank(target, owner) >= 4 &&
                  supportCount >= resistanceCount
              ) {
                HookContactSeed(
                  owner = owner,
                  from = from,
                  createSquare = createSquare,
                  target = target,
                  supportCount = supportCount,
                  resistanceCount = resistanceCount
                )
              }
            }
          }
      }
    }

  private def extractTensionContactSeeds(
      board: Board,
      leverContactSeeds: List[LeverContactSeed]
  ): List[TensionContactSeed] =
    val leverPairs = leverContactSeeds.map(seed => (seed.owner, seed.from, seed.target)).toSet
    pawnContactPairs(board).flatMap { contact =>
      val maintainable = isStableSquare(board, contact.from, contact.owner)
      val supportCount = countAttackers(board, contact.target, contact.owner)
      val resistanceCount = countAttackers(board, contact.target, !contact.owner)
      val flank = isFlankFile(contact.from.file) || isFlankFile(contact.target.file)
      Option.when(
        leverPairs.contains((contact.owner, contact.from, contact.target)) &&
          (maintainable || supportCount >= resistanceCount)
      ) {
        TensionContactSeed(
          owner = contact.owner,
          from = contact.from,
          target = contact.target,
          supportCount = supportCount,
          resistanceCount = resistanceCount,
          maintainable = maintainable,
          flank = flank
        )
      }
    }

  private def extractReleaseCandidates(
      board: Board,
      breakCandidates: List[BreakCandidate],
      leverContactSeeds: List[LeverContactSeed],
      tensionContactSeeds: List[TensionContactSeed]
  ): List[ReleaseCandidate] =
    val captureBreakPairs =
      breakCandidates.collect {
        case candidate if candidate.mode == BreakMode.Capture =>
          (candidate.owner, candidate.sourceSquare, candidate.breakSquare)
      }.toSet
    val releasePairs =
      (leverContactSeeds.map(seed => (seed.owner, seed.from, seed.target)) ++
        tensionContactSeeds.map(seed => (seed.owner, seed.from, seed.target))).toSet

    val contactReleases =
      pawnContactPairs(board).flatMap { contact =>
        val supportCount = countAttackers(board, contact.target, contact.owner)
        val resistanceCount = countAttackers(board, contact.target, !contact.owner)
        val kind =
          if captureBreakPairs.contains((contact.owner, contact.from, contact.target)) then ReleaseCandidateKind.Break
          else if resistanceCount > 0 then ReleaseCandidateKind.Exchange
          else ReleaseCandidateKind.Capture
        Option.when(
          releasePairs.contains((contact.owner, contact.from, contact.target)) &&
            supportCount >= resistanceCount
        ) {
          ReleaseCandidate(
            owner = contact.owner,
            from = contact.from,
            target = contact.target,
            kind = kind,
            supportCount = supportCount,
            resistanceCount = resistanceCount
          )
        }
      }

    val pushBreakReleases =
      breakCandidates.flatMap { candidate =>
        Option.when(
          candidate.mode == BreakMode.Push &&
            candidate.targetSquares.nonEmpty &&
            candidate.supportCount >= candidate.resistanceCount
        ) {
          ReleaseCandidate(
            owner = candidate.owner,
            from = candidate.sourceSquare,
            target = candidate.breakSquare,
            kind = ReleaseCandidateKind.Break,
            supportCount = candidate.supportCount,
            resistanceCount = candidate.resistanceCount
          )
        }
      }

    contactReleases ++ pushBreakReleases

  private def extractCounterplayResourceSeeds(
      board: Board,
      routeAnchors: Map[Color, List[Square]],
      breakCandidates: List[BreakCandidate],
      leverContactSeeds: List[LeverContactSeed]
  ): List[CounterplayResourceSeed] =
    val leverOrigins = leverContactSeeds.map(seed => (seed.owner, seed.from)).toSet
    Owners.flatMap { owner =>
      val anchors =
        (routeAnchors(owner) ++
          breakCandidates.filter(_.owner == owner).map(_.breakSquare) ++
          leverContactSeeds.filter(_.owner == owner).map(_.target)).distinct.sortBy(_.key)

      board.byColor(owner).squares.flatMap { square =>
        board.pieceAt(square).flatMap { piece =>
          if piece.role == King then None
          else
            val attackMask = attackMaskFrom(piece.role, square, board.occupied, owner)
            val pressureSquares =
              anchors.filter(target => target != square && attackMask.contains(target)).distinct.sortBy(_.key)
            val supportCount = countAttackers(board, square, owner)
            val attackerCount = countAttackers(board, square, !owner)
            Option.when(
              pressureSquares.nonEmpty &&
                isStableSquare(board, square, owner) &&
                qualifiesAsCounterplayResource(board, owner, square, piece.role, pressureSquares.size, leverOrigins.contains((owner, square)))
            ) {
              CounterplayResourceSeed(
                owner = owner,
                square = square,
                role = piece.role,
                pressureSquares = pressureSquares.take(4),
                supportCount = supportCount,
                attackerCount = attackerCount
              )
            }
        }
      }
    }

  private def buildRouteAnchors(
      board: Board,
      targetSquares: List[TargetSquare],
      exchangeSquares: List[ExchangeSquare],
      criticalSquares: List[CriticalSquare]
  ): Map[Color, List[Square]] =
    Owners.map { owner =>
      val targetAnchors = targetSquares.filter(_.owner == owner).map(_.square)
      val exchangeAnchors = exchangeSquares.filter(_.owner == owner).map(_.square)
      val criticalAnchors = criticalSquares.filter(_.owner == owner).map(_.square)
      val structuralAnchors =
        board.byColor(!owner).squares.filter { square =>
          board.roleAt(square).exists(role => isMeaningfulEnemyAnchor(board, owner, square, role))
        }
      owner -> (targetAnchors ++ exchangeAnchors ++ criticalAnchors ++ structuralAnchors).distinct.sortBy(_.key)
    }.toMap

  private def pawnContactPairs(board: Board): List[PawnContact] =
    Owners.flatMap { owner =>
      board.byPiece(owner, Pawn).squares.flatMap { from =>
        from.pawnAttacks(owner).squares.flatMap { target =>
          Option.when(board.pieceAt(target).contains(Piece(!owner, Pawn))) {
            PawnContact(owner = owner, from = from, target = target)
          }
        }
      }
    }

  private def qualifiesAsCounterplayResource(
      board: Board,
      owner: Color,
      square: Square,
      role: Role,
      pressureCount: Int,
      hasLeverAnchor: Boolean
  ): Boolean =
    role match
      case Pawn =>
        hasLeverAnchor || relativeRank(square, owner) >= 4
      case Knight | Bishop =>
        inEnemyHalf(square, owner) || pressureCount >= 2
      case Rook | Queen =>
        isOpenFile(board, square.file) || isSemiOpenFileFor(board, square.file, owner) || pressureCount >= 2
      case _ => false

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

  private def keepRouteAnchor(
      board: Board,
      owner: Color,
      target: Square,
      attackerCount: Int,
      defenderCount: Int
  ): Boolean =
    board.pieceAt(target).exists(_.color == !owner) || isOperationalSquare(target, owner) && attackerCount >= defenderCount

  private def hasContactSupport(board: Board, owner: Color, from: Square, target: Square): Boolean =
    countAttackers(board, from, owner) > 0 ||
      countAttackers(board, target, !owner) > 0 ||
      countAttackers(board, target, owner) > 1

  private def isMeaningfulExchangeSquare(square: Square, role: Role): Boolean =
    role != King &&
      (role != Pawn || square.file == File.C || square.file == File.D || square.file == File.E || square.file == File.F)

  private def isMeaningfulEnemyAnchor(
      board: Board,
      owner: Color,
      square: Square,
      role: Role
  ): Boolean =
    role != King &&
      (
        role == Pawn &&
          (relativeRank(square, owner) >= 3 || isCentralFile(square.file) || isFlankFile(square.file)) ||
          role != Pawn &&
            (inEnemyHalf(square, owner) || isCentralFile(square.file) || countAttackers(board, square, owner) > 0)
      )

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
      board.pieceAt(front).contains(Piece(pawnOwner, Pawn)) &&
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
      .filter(target => board.pieceAt(target).contains(Piece(!owner, Pawn)))

  private def candidateRouteSquares(board: Board, owner: Color, file: File): List[Square] =
    squaresOnFile(file)
      .filter(square => inEnemyHalf(square, owner) && board.pieceAt(square).isEmpty)
      .sortBy(square => (-relativeRank(square, owner), -countAttackers(board, square, owner)))

  private def squaresOnFile(file: File): List[Square] =
    Square.all.filter(_.file == file)

  private def inEnemyHalf(square: Square, owner: Color): Boolean =
    if owner.white then square.rank.value >= Rank.Fifth.value else square.rank.value <= Rank.Fourth.value

  private def isOperationalSquare(square: Square, owner: Color): Boolean =
    inEnemyHalf(square, owner) || isCentralFile(square.file) || relativeRank(square, owner) >= 3

  private def isOpenFile(board: Board, file: File): Boolean =
    board.pawns.squares.forall(_.file != file)

  private def isSemiOpenFileFor(board: Board, file: File, owner: Color): Boolean =
    board.byPiece(owner, Pawn).squares.forall(_.file != file)

  private def isLiftCorridorSquare(square: Square, owner: Color): Boolean =
    val rank = relativeRank(square, owner)
    rank == 3 || rank == 4

  private def isStableSquare(board: Board, square: Square, owner: Color): Boolean =
    val defenders = countAttackers(board, square, owner)
    val attackers = countAttackers(board, square, !owner)
    attackers == 0 || defenders >= attackers

  private def isOutpostSquare(board: Board, square: Square, owner: Color): Boolean =
    inEnemyHalf(square, owner) &&
      enemyPawnAttackers(board, square, !owner).isEmpty &&
      countAttackers(board, square, owner) >= 1 &&
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
    val rawTargets = attackMaskFrom(role, square, board.occupied, owner)
    (rawTargets & ~board.byColor(owner)).count

  private def projectedMobility(
      board: Board,
      square: Square,
      role: Role,
      owner: Color,
      origin: Square
  ): Int =
    val occupied = occupiedWithout(board, origin)
    val targets = attackMaskFrom(role, square, occupied, owner)
    (targets & ~(board.byColor(owner) & ~origin.bb)).count

  private def attackMaskFrom(role: Role, square: Square, occupied: Bitboard, owner: Color): Bitboard =
    role match
      case Pawn   => square.pawnAttacks(owner)
      case Knight => square.knightAttacks
      case Bishop => square.bishopAttacks(occupied)
      case Rook   => square.rookAttacks(occupied)
      case Queen  => square.queenAttacks(occupied)
      case King   => square.kingAttacks

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

  private def isCentralFile(file: File): Boolean =
    file == File.C || file == File.D || file == File.E || file == File.F

  private def isFlankFile(file: File): Boolean =
    file == File.A || file == File.B || file == File.G || file == File.H

  private def sameDiagonal(a: Square, b: Square): Boolean =
    math.abs(a.file.value - b.file.value) == math.abs(a.rank.value - b.rank.value)

  private def clearOrthogonalPath(board: Board, from: Square, to: Square): Boolean =
    if from.file == to.file then
      squaresBetweenSameFile(from, to).forall(square => board.pieceAt(square).isEmpty)
    else if from.rank == to.rank then
      squaresBetweenSameRank(from, to).forall(square => board.pieceAt(square).isEmpty)
    else false

  private def squaresBetweenSameFile(from: Square, to: Square): List[Square] =
    val step = if to.rank.value > from.rank.value then 1 else -1
    ((from.rank.value + step) until to.rank.value by step).flatMap(rank => Square.at(from.file.value, rank)).toList

  private def squaresBetweenSameRank(from: Square, to: Square): List[Square] =
    val step = if to.file.value > from.file.value then 1 else -1
    ((from.file.value + step) until to.file.value by step).flatMap(file => Square.at(file, from.rank.value)).toList

  private def geometryDistance(from: Square, to: Square): Int =
    math.abs(from.file.value - to.file.value) + math.abs(from.rank.value - to.rank.value)

  private def horizontalDistance(from: Square, to: Square): Int =
    math.abs(from.file.value - to.file.value)

  private def routeAnchorPriority(board: Board, owner: Color, target: Square): Int =
    board.pieceAt(target) match
      case Some(piece) if piece.color == !owner && piece.role != Pawn => 0
      case Some(piece) if piece.color == !owner                       => 1
      case _                                                          => 2

  private def occupiedWithout(board: Board, origin: Square): Bitboard =
    board.occupied & ~origin.bb

  private final case class PawnContact(
      owner: Color,
      from: Square,
      target: Square
  )
