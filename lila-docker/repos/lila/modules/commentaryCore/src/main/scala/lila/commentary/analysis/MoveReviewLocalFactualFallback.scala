package lila.commentary.analysis

import chess.{ Bishop, Board, Color, King, Knight, Pawn, Queen, Role, Rook, Square }
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.MoveReviewRefs
import lila.commentary.model.NarrativeContext

private[commentary] object MoveReviewLocalFactualFallback:

  private final case class LocalFacts(
      capturedRole: Option[Role],
      enPassant: Boolean,
      promotedRole: Option[Role],
      materialSupport: Option[String],
      tacticalSupport: Option[String],
      tacticalSupportTag: Option[String],
      pvSupport: Option[String]
  )

  final case class Result(
      claim: String,
      support: Option[String],
      factGuardrails: List[String],
      sourceTag: String
  )

  def build(ctx: NarrativeContext, refs: Option[MoveReviewRefs]): Option[Result] =
    MoveReviewExplanationBuilder.current(ctx).filter(sanMatchesLegalMove).flatMap { played =>
      val facts = localFacts(ctx, played, refs)
      claimFor(played, facts).map { claim =>
        val supportFacts =
          List(facts.materialSupport, facts.tacticalSupport, facts.pvSupport).flatten.take(3).distinct
        val support = Option.when(supportFacts.nonEmpty)(supportFacts.mkString(" "))
        Result(
          claim = claim,
          support = support,
          factGuardrails =
            List(
              Some("MoveReview local factual source: legal_current_move"),
              Some(s"MoveReview local factual UCI: ${played.uci}"),
              Some(s"MoveReview local factual role: ${roleLabel(played.role)}"),
              Some(s"MoveReview local factual origin: ${played.from.key}"),
              Some(s"MoveReview local factual target: ${played.toKey}"),
              facts.capturedRole.map(role => s"MoveReview local factual captured role: ${roleLabel(role)}"),
              facts.promotedRole.map(role => s"MoveReview local factual promotion role: ${roleLabel(role)}"),
              Option.when(facts.enPassant)("MoveReview local factual capture: en_passant"),
              facts.materialSupport.map(_ => "MoveReview local factual material delta: board_diff"),
              facts.tacticalSupportTag.map(tag => s"MoveReview local factual tactical motif: $tag"),
              facts.pvSupport.map(_ => "MoveReview local factual PV: coupled_legal_line")
            ).flatten.distinct,
          sourceTag = "legal_local_factual"
        )
      }
    }

  private def claimFor(played: CommentaryIdeaSurface.PlayedMove, facts: LocalFacts): Option[String] =
    if played.isCastle && longCastle(played.uci) then Some("This castles long.")
    else if played.isCastle then Some("This castles.")
    else if facts.enPassant then Some(s"This captures the pawn en passant and lands on ${played.toKey}.")
    else if facts.promotedRole.nonEmpty && facts.capturedRole.nonEmpty then
      Some(
        s"This captures the ${roleLabel(facts.capturedRole.get)} on ${played.toKey} and promotes to a ${roleLabel(facts.promotedRole.get)}."
      )
    else if facts.promotedRole.nonEmpty then
      Some(s"This promotes to a ${roleLabel(facts.promotedRole.get)} on ${played.toKey}.")
    else if promotion(played.uci) then Some(s"This promotes on ${played.toKey}.")
    else if played.isCapture then
      facts.capturedRole.map(role => s"This captures the ${roleLabel(role)} on ${played.toKey}.")
        .orElse(Some("This captures."))
    else if played.role == Pawn then Some(s"This moves the pawn to ${played.toKey}.")
    else Some(s"This moves the ${roleLabel(played.role)} from ${played.from.key} to ${played.toKey}.")

  private def localFacts(
      ctx: NarrativeContext,
      played: CommentaryIdeaSurface.PlayedMove,
      refs: Option[MoveReviewRefs]
  ): LocalFacts =
    val beforePosition = Fen.read(Standard, Fen.Full(ctx.fen))
    val afterPosition = Fen.read(Standard, Fen.Full(played.afterFen))
    val materialCapturedRole =
      for
        before <- beforePosition
        after <- afterPosition
        role <- capturedRoleByMaterialDiff(before.board, after.board, !played.color)
      yield role
    val promoted = promotedRole(played, afterPosition.map(_.board))
    val enPassant =
      isEnPassant(played, beforePosition.map(_.board), materialCapturedRole)
    val captured =
      if enPassant then Some(Pawn) else played.capturedRole.orElse(materialCapturedRole)
    val material = materialSupport(captured, promoted)
    val (tactical, tacticalTag) =
      tacticalSupport(
        givesCheckmate = afterPosition.exists(_.checkMate),
        givesCheck = afterPosition.exists(_.check.yes)
      )
    LocalFacts(
      capturedRole = captured,
      enPassant = enPassant,
      promotedRole = promoted,
      materialSupport = material,
      tacticalSupport = tactical,
      tacticalSupportTag = tacticalTag,
      pvSupport = coupledPvSupport(ctx, played, refs)
    )

  private def capturedRoleByMaterialDiff(before: Board, after: Board, capturedColor: Color): Option[Role] =
    MaterialRoles.collectFirst {
      case role if roleCount(before, capturedColor, role) - roleCount(after, capturedColor, role) == 1 => role
    }

  private val MaterialRoles: List[Role] =
    List(Queen, Rook, Bishop, Knight, Pawn)

  private def roleCount(board: Board, color: Color, role: Role): Int =
    Square.all.count(square => board.pieceAt(square).exists(piece => piece.color == color && piece.role == role))

  private def isEnPassant(
      played: CommentaryIdeaSurface.PlayedMove,
      beforeBoard: Option[Board],
      materialCapturedRole: Option[Role]
  ): Boolean =
    played.role == Pawn &&
      played.san.contains("x") &&
      played.capturedRole.isEmpty &&
      materialCapturedRole.contains(Pawn) &&
      beforeBoard.exists(_.pieceAt(played.to).isEmpty) &&
      played.uci.length >= 4 &&
      played.uci.charAt(0) != played.uci.charAt(2)

  private def promotedRole(played: CommentaryIdeaSurface.PlayedMove, afterBoard: Option[Board]): Option[Role] =
    if played.role != Pawn then None
    else
      val promoted =
        played.uci.lift(4).flatMap {
          case 'q' => Some(Queen)
          case 'r' => Some(Rook)
          case 'b' => Some(Bishop)
          case 'n' => Some(Knight)
          case _   => None
        }
      promoted.filter(role => afterBoard.exists(_.pieceAt(played.to).exists(piece => piece.color == played.color && piece.role == role)))

  private def materialSupport(capturedRole: Option[Role], promotedRole: Option[Role]): Option[String] =
    (capturedRole, promotedRole) match
      case (Some(captured), Some(promoted)) =>
        Some(s"The local material change is a captured ${roleLabel(captured)} plus a pawn becoming a ${roleLabel(promoted)}.")
      case (Some(captured), None) =>
        Some(s"The local material change is a captured ${roleLabel(captured)}.")
      case (None, Some(promoted)) =>
        Some(s"The local material change is a pawn becoming a ${roleLabel(promoted)}.")
      case (None, None) => None

  private def tacticalSupport(
      givesCheckmate: Boolean,
      givesCheck: Boolean
  ): (Option[String], Option[String]) =
    if givesCheckmate then Some("It also gives checkmate.") -> Some("checkmate")
    else if givesCheck then Some("It also gives check.") -> Some("check")
    else None -> None

  private def coupledPvSupport(
      ctx: NarrativeContext,
      played: CommentaryIdeaSurface.PlayedMove,
      refs: Option[MoveReviewRefs]
  ): Option[String] =
    MoveReviewPvLine.firstCoupled(ctx.fen, played.uci, refs).flatMap { line =>
      val preview =
        List(Some(line.first.san), line.reply.map(_.san)).flatten
          .map(_.trim)
          .filter(_.nonEmpty)
      Option.when(preview.size >= 2)(s"The checked line begins ${preview.mkString(" ")}.")
    }

  private def longCastle(uci: String): Boolean =
    Set("e1c1", "e8c8").contains(uci)

  private def promotion(uci: String): Boolean =
    uci.length == 5

  private def sanMatchesLegalMove(played: CommentaryIdeaSurface.PlayedMove): Boolean =
    val san = played.san.trim
    if san.startsWith("O-O") then played.isCastle
    else sanRole(san).contains(played.role)

  private def sanRole(san: String): Option[Role] =
    san.replaceFirst("""^\d+\.{1,3}""", "").trim.headOption.flatMap {
      case 'K'                          => Some(King)
      case 'Q'                          => Some(Queen)
      case 'R'                          => Some(Rook)
      case 'B'                          => Some(Bishop)
      case 'N'                          => Some(Knight)
      case file if file >= 'a' && file <= 'h' => Some(Pawn)
      case _                            => None
    }

  private def roleLabel(role: Role): String =
    role match
      case King   => "king"
      case Queen  => "queen"
      case Rook   => "rook"
      case Bishop => "bishop"
      case Knight => "knight"
      case Pawn   => "pawn"
