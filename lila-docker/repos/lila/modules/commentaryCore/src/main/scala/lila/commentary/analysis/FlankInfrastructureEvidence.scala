package lila.commentary.analysis

import chess.*
import chess.format.Fen
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme, ThemeResolver }
import lila.commentary.analysis.PlanMoveEvidenceSupport.*
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }

object FlankInfrastructureEvidence:

  final case class FlankCandidate(
      kind: PlanKind,
      role: Role,
      from: Square,
      to: Square,
      file: File,
      createsHookContact: Boolean,
      kingAligned: Boolean,
      score: Double,
      reasons: List[String]
  ):
    def uci: String = s"${from.key}${to.key}"

  def candidatesFromFen(fen: String, color: Color): List[FlankCandidate] =
    Fen
      .read(chess.variant.Standard, Fen.Full(fen))
      .toList
      .flatMap { pos =>
        val oriented = orientedPosition(pos, color)
        candidates(oriented, color)
      }

  def candidates(pos: Position, color: Color): List[FlankCandidate] =
    val oriented = orientedPosition(pos, color)
    oriented.legalMoves.toList
      .flatMap(moveCandidates(oriented.board, _))
      .sortBy(c => (-c.score, c.kind.id, c.from.key, c.to.key))
      .distinctBy(c => s"${c.kind.id}|${c.uci}")

  def planHypotheses(fen: String, color: Color): List[PlanHypothesis] =
    candidatesFromFen(fen, color)
      .groupBy(_.kind)
      .values
      .toList
      .flatMap(_.sortBy(c => -c.score).headOption)
      .sortBy(c => -c.score)
      .take(2)
      .map(toHypothesis)

  def movesForSubplan(pos: Position, subplan: PlanKind, legalMoves: List[Move]): List[Move] =
    legalMoves
      .filter(_.piece.color == pos.color)
      .flatMap { mv =>
        moveCandidates(pos.board, mv).find(_.kind == subplan).map(candidate => mv -> candidate.score)
      }
      .sortBy { case (_, score) => -score }
      .map(_._1)
      .distinct

  private def moveCandidates(board: Board, mv: Move): List[FlankCandidate] =
    if mv.piece.role == Rook then rookLiftCandidate(board, mv).toList
    else if mv.piece.role != Pawn || mv.promotion.nonEmpty || !advancesTowardPromotion(mv, mv.piece.color) then Nil
    else
      val color = mv.piece.color
      val hook = createsEnemyPawnContact(mv.after.board, mv.dest, color)
      val aligned = kingAlignedWithRookPawn(board, color, mv.orig.file)
      val rookPawnCandidate =
        Option.when(isRookPawnFile(mv.orig.file) && !mv.captures && (aligned || hook)) {
          val reasons =
            List(
              Some("rook_pawn_advance"),
              Option.when(aligned)("king_flank_aligned"),
              Option.when(hook)("creates_hook_contact")
            ).flatten
          FlankCandidate(
            kind = PlanKind.RookPawnMarch,
            role = Pawn,
            from = mv.orig,
            to = mv.dest,
            file = mv.orig.file,
            createsHookContact = hook,
            kingAligned = aligned,
            score = flankScore(PlanKind.RookPawnMarch, hook, aligned, mv.captures),
            reasons = reasons
          )
        }
      val hookCandidate =
        Option.when(hook && isFlankFile(mv.dest.file)) {
          val reasons =
            List(
              Some("creates_hook_contact"),
              Option.when(isRookPawnFile(mv.orig.file))("rook_pawn_hook"),
              Option.when(aligned)("king_flank_aligned"),
              Option.when(mv.captures)("capture_opens_hook_file")
            ).flatten
          FlankCandidate(
            kind = PlanKind.HookCreation,
            role = Pawn,
            from = mv.orig,
            to = mv.dest,
            file = mv.dest.file,
            createsHookContact = true,
            kingAligned = aligned,
            score = flankScore(PlanKind.HookCreation, hook = true, aligned, mv.captures),
            reasons = reasons
          )
        }
      List(rookPawnCandidate, hookCandidate).flatten.filter(_.score >= 0.58)

  private def toHypothesis(candidate: FlankCandidate): PlanHypothesis =
    val moveText = displayMove(candidate.uci)
    val planName =
      candidate.kind match
        case PlanKind.RookPawnMarch =>
          s"Advance the rook pawn from ${candidate.from.key} toward ${candidate.to.key}"
        case PlanKind.HookCreation =>
          s"Create a flank hook with $moveText"
        case PlanKind.RookLiftScaffold =>
          s"Lift the rook from ${candidate.from.key} toward ${candidate.to.key}"
        case _ =>
          s"Build flank infrastructure with $moveText"
    val structuralTag =
      candidate.kind match
        case PlanKind.RookPawnMarch => "rook_pawn_march"
        case PlanKind.HookCreation  => "hook_creation"
        case PlanKind.RookLiftScaffold => "rook_lift_scaffold"
        case _                      => "flank_infrastructure"
    val executionSteps =
      flankExecutionText(candidate, moveText)
    val failureModes =
      candidate.kind match
        case PlanKind.RookLiftScaffold =>
          List(
            "central counterplay can make the rook lift too slow",
            "the rook may be chased before attacking pieces coordinate"
          )
        case _ =>
          List(
            "central counterplay can punish the flank tempo",
            "the hook can be fixed against the attacker if pieces cannot join"
          )
    PlanHypothesis(
      planId = candidate.kind.id,
      planName = planName,
      rank = 0,
      score = candidate.score,
      preconditions = flankWhyText(candidate, moveText).distinct.take(5),
      executionSteps = executionSteps,
      failureModes = failureModes,
      viability = PlanViability(
        score = candidate.score,
        label = if candidate.score >= 0.72 then "high" else "medium",
        risk = "flank infrastructure can fail if central counterplay arrives first"
      ),
      refutation = Some("change plans if the center opens before the flank hook matters"),
      evidenceSources = List(
        ThemeResolver.themeTag(PlanTheme.FlankInfrastructure),
        ThemeResolver.subplanTag(candidate.kind),
        ThemeResolver.structuralStateTag(structuralTag),
        "flank_infrastructure"
      ) ++ candidate.reasons,
      themeL1 = PlanTheme.FlankInfrastructure.id,
      subplanId = Some(candidate.kind.id)
    )

  private def flankWhyText(candidate: FlankCandidate, moveText: String): List[String] =
    val fileText = s"${candidate.file.char}-file"
    val why =
      List(
        Some(
          candidate.kind match
            case PlanKind.RookLiftScaffold => s"$moveText reaches a lift rank on the $fileText"
            case PlanKind.HookCreation     => s"$moveText creates contact on the $fileText"
            case PlanKind.RookPawnMarch    => s"$moveText advances the rook pawn on the $fileText"
            case _                         => s"$moveText builds play on the $fileText"
        ),
        Option.when(candidate.kingAligned)(
          if candidate.role == Rook then s"the enemy king is close enough to the $fileText for rook pressure"
          else s"the rook-pawn file is aligned with the king flank"
        ),
        Option.when(candidate.createsHookContact)(
          "the move creates a hook for later flank pressure"
        ),
        Option.when(candidate.reasons.contains("rook_lift_attacks_resource"))(
          "the rook lift also attacks a non-king resource"
        ),
        Option.when(candidate.reasons.contains("rook_lift_gives_check"))(
          "the rook lift gives check immediately"
        ),
        mobilityGain(candidate.reasons).map(gain =>
          s"the rook gains $gain mobility ${if gain == 1 then "square" else "squares"}"
        )
      ).flatten
    if why.nonEmpty then why else List(s"$moveText improves flank infrastructure")

  private def flankExecutionText(candidate: FlankCandidate, moveText: String): List[String] =
    candidate.kind match
      case PlanKind.RookLiftScaffold =>
        List(
          s"lift the rook with $moveText toward the flank target",
          "keep the rook coordinated so it is not chased before pressure lands"
        )
      case PlanKind.HookCreation =>
        List(
          s"play $moveText while the center stays controlled",
          "bring pieces behind the hook before the flank pawn becomes a target"
        )
      case PlanKind.RookPawnMarch =>
        List(
          s"advance the rook pawn with $moveText only while central counterplay is covered",
          "prepare pieces behind the pawn before the hook becomes fixed"
        )
      case _ =>
        List(
          s"play $moveText while the center stays controlled",
          "coordinate pieces before committing fully to the flank"
        )

  private def mobilityGain(reasons: List[String]): Option[Int] =
    reasonPayload(reasons, "mobility_gain:").flatMap(_.toIntOption)

  private def reasonPayload(reasons: List[String], prefix: String): Option[String] =
    reasons.collectFirst {
      case reason if reason.startsWith(prefix) => reason.drop(prefix.length).trim
    }.filter(_.nonEmpty)

  private def displayMove(uci: String): String =
    if uci.length >= 4 then s"${uci.take(2)}-${uci.slice(2, 4)}" else uci

  private def flankScore(kind: PlanKind, hook: Boolean, aligned: Boolean, captures: Boolean): Double =
    val base =
      kind match
        case PlanKind.HookCreation  => 0.66
        case PlanKind.RookLiftScaffold => 0.64
        case PlanKind.RookPawnMarch => 0.60
        case _                      => 0.55
    (base +
      (if hook then 0.08 else 0.0) +
      (if aligned then 0.05 else 0.0) +
      (if captures then 0.03 else 0.0))
      .max(0.30)
      .min(0.82)

  private def rookLiftCandidate(board: Board, mv: Move): Option[FlankCandidate] =
    val color = mv.piece.color
    val quiet = !mv.captures && mv.promotion.isEmpty
    val liftRank =
      if color.white then mv.dest.rank == Rank.Third || mv.dest.rank == Rank.Fourth
      else mv.dest.rank == Rank.Sixth || mv.dest.rank == Rank.Fifth
    val enemyKingNearFile =
      board.kingPosOf(!color).exists(king => (king.file.value - mv.dest.file.value).abs <= 2)
    val mobilityGain = pieceMobility(mv.after.board, mv.piece, mv.dest) - pieceMobility(board, mv.piece, mv.orig)
    val attacksResource =
      attackedEnemyPieces(mv.after.board, mv.piece, mv.dest)
        .exists(square => mv.after.board.pieceAt(square).exists(piece => piece.color == !color && piece.role != King))
    val givesCheck = mv.after.check.yes
    val qualifies =
      quiet &&
        liftRank &&
        (enemyKingNearFile || attacksResource || givesCheck) &&
        (mobilityGain >= 0 || attacksResource || givesCheck)
    Option.when(qualifies) {
      val reasons =
        List(
          Some("rook_lift_rank"),
          Option.when(enemyKingNearFile)("enemy_king_flank_file"),
          Option.when(mobilityGain > 0)(s"mobility_gain:$mobilityGain"),
          Option.when(attacksResource)("rook_lift_attacks_resource"),
          Option.when(givesCheck)("rook_lift_gives_check")
        ).flatten
      FlankCandidate(
        kind = PlanKind.RookLiftScaffold,
        role = Rook,
        from = mv.orig,
        to = mv.dest,
        file = mv.dest.file,
        createsHookContact = false,
        kingAligned = enemyKingNearFile,
        score = flankScore(PlanKind.RookLiftScaffold, hook = attacksResource || givesCheck, aligned = enemyKingNearFile, captures = false),
        reasons = reasons
      )
    }
