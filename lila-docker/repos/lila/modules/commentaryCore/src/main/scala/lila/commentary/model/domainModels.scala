package lila.commentary.model

import chess.{Square, Piece, File, Color, Board, Pawn}
import lila.commentary.model.structure.PawnStructureType

final case class EcoCode(value: String) extends AnyVal:
  def get_group: Char = if (value.nonEmpty) value.head else '?'
  def check_flank: Boolean = get_group == 'A'
  def check_semi_open: Boolean = get_group == 'B'

enum BoardZone:
  case Kingside, Queenside, Center

object BoardZone:
  def locate_zone(sq: Square): BoardZone =
    val index = sq.file.value
    if (index < 3) BoardZone.Queenside
    else if (index > 4) BoardZone.Kingside
    else BoardZone.Center

  def collect_zones(squares: Set[Square]): Set[BoardZone] =
    squares.map(locate_zone)

enum RoutePurpose:
  case OutpostEntrenchment
  case KingsideAttack
  case QueensideAttack
  case FileControl
  case RookLift(targetRank: Int)
  case Prophylaxis
  case Redeployment

final case class PieceRoute(
    piece: Piece,
    path: List[Square],
    purpose: RoutePurpose
):
  def get_start: Option[Square] = path.headOption
  def get_end: Option[Square] = path.lastOption
  def check_rook_lift: Boolean = purpose match
    case RoutePurpose.RookLift(_) => true
    case _ => false

package structure {
  final case class ChessBoardState(board: Board):
    def has_pawn(sq: Square, color: Color): Boolean =
      board.pieceAt(sq).contains(Piece(color, Pawn))

  enum PawnStructureType:
    case Carlsbad, FrenchAdvance, NajdorfScheveningen, Benoni, KidLocked, MaroczyBind, Unknown

  object PawnStructureMatcher:
    import chess.Square.*

    def match_structure(state: ChessBoardState): PawnStructureType =
      if (state.has_pawn(E4, Color.White) && state.has_pawn(E5, Color.Black))
        PawnStructureType.Carlsbad
      else if (state.has_pawn(E5, Color.White) && state.has_pawn(E6, Color.Black))
        PawnStructureType.FrenchAdvance
      else if (state.has_pawn(E4, Color.White) && state.has_pawn(D6, Color.Black))
        PawnStructureType.NajdorfScheveningen
      else if (state.has_pawn(D5, Color.White) && state.has_pawn(C5, Color.Black))
        PawnStructureType.Benoni
      else
        PawnStructureType.Unknown
}

enum OpeningFamily(val id: String, val label: String):
  case OpenGames extends OpeningFamily("open_games", "Open Games (1.e4 e5)")
  case Sicilian extends OpeningFamily("sicilian", "Sicilian Defense")
  case French extends OpeningFamily("french", "French Defense")
  case CaroKann extends OpeningFamily("caro_kann", "Caro-Kann Defense")
  case NimzoIndian extends OpeningFamily("nimzo_indian", "Nimzo-Indian Defense")
  case KingsIndian extends OpeningFamily("kings_indian", "King's Indian Defense")
  case Benoni extends OpeningFamily("benoni", "Benoni Defense")
  case Catalan extends OpeningFamily("catalan", "Catalan Opening")
  case QueensGambit extends OpeningFamily("queens_gambit", "Queen's Gambit")
  case London extends OpeningFamily("london", "London System")
  case English extends OpeningFamily("english", "English Opening")
  case Austrian extends OpeningFamily("austrian", "Austrian Attack")

object OpeningFamily:
  def check_match(family: OpeningFamily, struct: PawnStructureType): Boolean =
    (family, struct) match
      case (OpeningFamily.Sicilian, PawnStructureType.NajdorfScheveningen) => true
      case (OpeningFamily.Benoni, PawnStructureType.Benoni) => true
      case _ => false

enum KingExposure:
  case Safe, ShieldWeakened, Exposed, UnderAttack

final case class KingSafetyEvaluation(
    exposure: KingExposure,
    defenderCount: Int,
    openLinesNearby: Boolean
)
