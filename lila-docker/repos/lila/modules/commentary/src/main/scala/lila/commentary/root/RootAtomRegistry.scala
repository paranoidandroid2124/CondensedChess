package lila.commentary.root

import chess.{ Color, File, Rank, Role, Square }
import chess.{ Bishop, King, Knight, Pawn, Queen, Rook }

object RootAtomRegistry:

  final val RootSize = 2891
  final val WordCount = (RootSize + 63) / 64

  enum SchemaFamily:
    case ColorPieceSquare
    case ColorSquare
    case ColorPawnSquare
    case ColorFile
    case NeutralSquare
    case NeutralFile
    case SideToMoveState
    case CastlingRightsState
    case EnPassantState

  object SchemaId:
    val PieceOn = "piece_on"
    val ControlledBy = "controlled_by"
    val PawnControlledBy = "pawn_controlled_by"
    val Contested = "contested"
    val OpenFile = "open_file"
    val HalfOpenFile = "half_open_file"
    val KingRingSquare = "king_ring_square"
    val WeakSquare = "weak_square"
    val OutpostSquare = "outpost_square"
    val IsolatedPawn = "isolated_pawn"
    val BackwardPawn = "backward_pawn"
    val DoubledFile = "doubled_file"
    val PassedPawn = "passed_pawn"
    val CandidatePasser = "candidate_passer"
    val FixedPawn = "fixed_pawn"
    val LoosePiece = "loose_piece"
    val PinnedPiece = "pinned_piece"
    val OverloadedPiece = "overloaded_piece"
    val TrappedPiece = "trapped_piece"
    val XrayTarget = "xray_target"
    val LeverAvailable = "lever_available"
    val KingShelterHole = "king_shelter_hole"
    val SideToMove = "side_to_move"
    val CastlingRights = "castling_rights"
    val EnPassantState = "en_passant_state"

  final case class RootSchema(
      id: String,
      start: Int,
      size: Int,
      family: SchemaFamily,
      localIndexFormula: String
  ):
    val end: Int = start + size - 1
    def contains(index: Int): Boolean = start <= index && index <= end

  final case class DecodedAtom(index: Int, schema: RootSchema, atomId: String)

  val canonicalColors: Vector[Color] = Vector(Color.White, Color.Black)
  val canonicalRoles: Vector[Role] = Vector(Pawn, Knight, Bishop, Rook, Queen, King)
  val canonicalFiles: Vector[File] = Vector(File.A, File.B, File.C, File.D, File.E, File.F, File.G, File.H)
  val canonicalSquares: Vector[Square] = Square.all.toVector
  val canonicalPawnSquares: Vector[Square] =
    canonicalSquares.filter(square => square.rank >= Rank.Second && square.rank <= Rank.Seventh)

  val all: Vector[RootSchema] = Vector(
    RootSchema(SchemaId.PieceOn, 0, 768, SchemaFamily.ColorPieceSquare, "start + 384 * colorIndex + 64 * pieceIndex + squareIndex"),
    RootSchema(SchemaId.ControlledBy, 768, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.PawnControlledBy, 896, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.Contested, 1024, 64, SchemaFamily.NeutralSquare, "start + squareIndex"),
    RootSchema(SchemaId.OpenFile, 1088, 8, SchemaFamily.NeutralFile, "start + fileIndex"),
    RootSchema(SchemaId.HalfOpenFile, 1096, 16, SchemaFamily.ColorFile, "start + 8 * colorIndex + fileIndex"),
    RootSchema(SchemaId.KingRingSquare, 1112, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.WeakSquare, 1240, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.OutpostSquare, 1368, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.IsolatedPawn, 1496, 96, SchemaFamily.ColorPawnSquare, "start + 48 * colorIndex + pawnSquareIndex"),
    RootSchema(SchemaId.BackwardPawn, 1592, 96, SchemaFamily.ColorPawnSquare, "start + 48 * colorIndex + pawnSquareIndex"),
    RootSchema(SchemaId.DoubledFile, 1688, 16, SchemaFamily.ColorFile, "start + 8 * colorIndex + fileIndex"),
    RootSchema(SchemaId.PassedPawn, 1704, 96, SchemaFamily.ColorPawnSquare, "start + 48 * colorIndex + pawnSquareIndex"),
    RootSchema(SchemaId.CandidatePasser, 1800, 96, SchemaFamily.ColorPawnSquare, "start + 48 * colorIndex + pawnSquareIndex"),
    RootSchema(SchemaId.FixedPawn, 1896, 96, SchemaFamily.ColorPawnSquare, "start + 48 * colorIndex + pawnSquareIndex"),
    RootSchema(SchemaId.LoosePiece, 1992, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.PinnedPiece, 2120, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.OverloadedPiece, 2248, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.TrappedPiece, 2376, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.XrayTarget, 2504, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.LeverAvailable, 2632, 96, SchemaFamily.ColorPawnSquare, "start + 48 * colorIndex + pawnSquareIndex"),
    RootSchema(SchemaId.KingShelterHole, 2728, 128, SchemaFamily.ColorSquare, "start + 64 * colorIndex + squareIndex"),
    RootSchema(SchemaId.SideToMove, 2856, 2, SchemaFamily.SideToMoveState, "start + sideToMoveIndex"),
    RootSchema(SchemaId.CastlingRights, 2858, 16, SchemaFamily.CastlingRightsState, "start + rightsMask"),
    RootSchema(SchemaId.EnPassantState, 2874, 17, SchemaFamily.EnPassantState, "start + enPassantStateIndex")
  )

  private val byId: Map[String, RootSchema] = all.map(schema => schema.id -> schema).toMap

  require(all.last.end + 1 == RootSize, s"Frozen root size mismatch: ${all.last.end + 1} != $RootSize")

  def schema(id: String): Option[RootSchema] = byId.get(id)

  def requireSchema(id: String): RootSchema =
    schema(id).getOrElse(throw IllegalArgumentException(s"Unknown root schema: $id"))

  def colorIndex(color: Color): Int = if color.white then 0 else 1

  def pieceIndex(role: Role): Int = role match
    case Pawn => 0
    case Knight => 1
    case Bishop => 2
    case Rook => 3
    case Queen => 4
    case King => 5

  def fileIndex(file: File): Int = file.value

  def squareIndex(square: Square): Int = square.value

  def mirrorColorSwapSquare(square: Square): Square =
    Square(square.file, Rank(7 - square.rank.value).get)

  def pawnSquareIndex(square: Square): Option[Int] =
    Option.when(square.rank >= Rank.Second && square.rank <= Rank.Seventh)((square.rank.value - 1) * 8 + square.file.value)

  def pieceOnIndex(color: Color, role: Role, square: Square): Int =
    val schema = requireSchema(SchemaId.PieceOn)
    schema.start + 384 * colorIndex(color) + 64 * pieceIndex(role) + squareIndex(square)

  def colorSquareIndex(schemaId: String, color: Color, square: Square): Int =
    val schema = requireSchema(schemaId)
    require(schema.family == SchemaFamily.ColorSquare, s"$schemaId is not a color-square schema")
    schema.start + 64 * colorIndex(color) + squareIndex(square)

  def colorPawnSquareIndex(schemaId: String, color: Color, square: Square): Option[Int] =
    val schema = requireSchema(schemaId)
    require(schema.family == SchemaFamily.ColorPawnSquare, s"$schemaId is not a color-pawn-square schema")
    pawnSquareIndex(square).map(local => schema.start + 48 * colorIndex(color) + local)

  def colorFileIndex(schemaId: String, color: Color, file: File): Int =
    val schema = requireSchema(schemaId)
    require(schema.family == SchemaFamily.ColorFile, s"$schemaId is not a color-file schema")
    schema.start + 8 * colorIndex(color) + fileIndex(file)

  def neutralSquareIndex(schemaId: String, square: Square): Int =
    val schema = requireSchema(schemaId)
    require(schema.family == SchemaFamily.NeutralSquare, s"$schemaId is not a neutral-square schema")
    schema.start + squareIndex(square)

  def neutralFileIndex(schemaId: String, file: File): Int =
    val schema = requireSchema(schemaId)
    require(schema.family == SchemaFamily.NeutralFile, s"$schemaId is not a neutral-file schema")
    schema.start + fileIndex(file)

  def sideToMoveIndex(color: Color): Int =
    requireSchema(SchemaId.SideToMove).start + colorIndex(color)

  def castlingRightsIndex(rightsMask: Int): Int =
    require(0 <= rightsMask && rightsMask < 16, s"Invalid castling-rights mask: $rightsMask")
    requireSchema(SchemaId.CastlingRights).start + rightsMask

  def enPassantNoneIndex: Int = requireSchema(SchemaId.EnPassantState).start

  def enPassantIndex(capturerColor: Color, file: File): Int =
    val schema = requireSchema(SchemaId.EnPassantState)
    schema.start + 1 + 8 * colorIndex(capturerColor) + fileIndex(file)

  def sideToMoveState(color: Color): String =
    if color.white then "white_to_move" else "black_to_move"

  def castlingRightsState(mask: Int): String =
    if mask == 0 then "-"
    else
      val builder = StringBuilder()
      if (mask & 1) != 0 then builder += 'K'
      if (mask & 2) != 0 then builder += 'Q'
      if (mask & 4) != 0 then builder += 'k'
      if (mask & 8) != 0 then builder += 'q'
      builder.result()

  def castlingRightsMask(state: String): Option[Int] =
    if state == "-" then Some(0)
    else
      state.foldLeft(Option(0)): (acc, ch) =>
        acc.flatMap: current =>
          ch match
            case 'K' => Some(current | 1)
            case 'Q' => Some(current | 2)
            case 'k' => Some(current | 4)
            case 'q' => Some(current | 8)
            case _ => None

  def enPassantStateLabel(capturerColor: Color, file: File): String =
    s"${capturerColor.name}_can_capture_on_file_${file.char}"

  def enPassantState(index: Int): Option[String] =
    val schema = requireSchema(SchemaId.EnPassantState)
    Option.when(schema.contains(index)):
      val local = index - schema.start
      if local == 0 then "none"
      else
        val color = canonicalColors((local - 1) / 8)
        val file = canonicalFiles((local - 1) % 8)
        enPassantStateLabel(color, file)

  def sideToMoveIndexFromState(state: String): Option[Int] =
    state match
      case "white_to_move" => Some(sideToMoveIndex(Color.White))
      case "black_to_move" => Some(sideToMoveIndex(Color.Black))
      case _ => None

  def castlingRightsIndexFromState(state: String): Option[Int] =
    castlingRightsMask(state).map(castlingRightsIndex)

  def enPassantIndexFromState(state: String): Option[Int] =
    if state == "none" then Some(enPassantNoneIndex)
    else
      state match
        case s"white_can_capture_on_file_${fileChar}" =>
          File.fromChar(fileChar.head).map(enPassantIndex(Color.White, _))
        case s"black_can_capture_on_file_${fileChar}" =>
          File.fromChar(fileChar.head).map(enPassantIndex(Color.Black, _))
        case _ => None

  def indexFromState(schemaId: String, state: String): Option[Int] =
    schemaId match
      case SchemaId.SideToMove => sideToMoveIndexFromState(state)
      case SchemaId.CastlingRights => castlingRightsIndexFromState(state)
      case SchemaId.EnPassantState => enPassantIndexFromState(state)
      case _ => None

  def mirrorColorSwapCastlingMask(mask: Int): Int =
    (if (mask & 4) != 0 then 1 else 0) |
      (if (mask & 8) != 0 then 2 else 0) |
      (if (mask & 1) != 0 then 4 else 0) |
      (if (mask & 2) != 0 then 8 else 0)

  def mirrorColorSwapIndex(index: Int): Int =
    val schema = all.find(_.contains(index)).getOrElse(throw IllegalArgumentException(s"Invalid root index: $index"))
    schema.family match
      case SchemaFamily.ColorPieceSquare =>
        val local = index - schema.start
        val color = canonicalColors(local / 384)
        val role = canonicalRoles((local % 384) / 64)
        val square = canonicalSquares(local % 64)
        pieceOnIndex(!color, role, mirrorColorSwapSquare(square))
      case SchemaFamily.ColorSquare =>
        val local = index - schema.start
        val color = canonicalColors(local / 64)
        val square = canonicalSquares(local % 64)
        colorSquareIndex(schema.id, !color, mirrorColorSwapSquare(square))
      case SchemaFamily.ColorPawnSquare =>
        val local = index - schema.start
        val color = canonicalColors(local / 48)
        val square = canonicalPawnSquares(local % 48)
        colorPawnSquareIndex(schema.id, !color, mirrorColorSwapSquare(square)).get
      case SchemaFamily.ColorFile =>
        val local = index - schema.start
        val color = canonicalColors(local / 8)
        val file = canonicalFiles(local % 8)
        colorFileIndex(schema.id, !color, file)
      case SchemaFamily.NeutralSquare =>
        val square = canonicalSquares(index - schema.start)
        neutralSquareIndex(schema.id, mirrorColorSwapSquare(square))
      case SchemaFamily.NeutralFile =>
        index
      case SchemaFamily.SideToMoveState =>
        val color = canonicalColors(index - schema.start)
        sideToMoveIndex(!color)
      case SchemaFamily.CastlingRightsState =>
        castlingRightsIndex(mirrorColorSwapCastlingMask(index - schema.start))
      case SchemaFamily.EnPassantState =>
        val local = index - schema.start
        if local == 0 then enPassantNoneIndex
        else
          val color = canonicalColors((local - 1) / 8)
          val file = canonicalFiles((local - 1) % 8)
          enPassantIndex(!color, file)

  def indicesForSquareMask(
      schemaId: String,
      mask64: Long,
      color: Option[Color] = None,
      role: Option[Role] = None
  ): Vector[Int] =
    requireSchema(schemaId).family match
      case SchemaFamily.ColorPieceSquare =>
        val resolvedColor = color.getOrElse(throw IllegalArgumentException(s"$schemaId requires a color"))
        val resolvedRole = role.getOrElse(throw IllegalArgumentException(s"$schemaId requires a role"))
        canonicalSquares.collect:
          case square if (mask64 & square.bl) != 0L => pieceOnIndex(resolvedColor, resolvedRole, square)
      case SchemaFamily.ColorSquare =>
        val resolvedColor = color.getOrElse(throw IllegalArgumentException(s"$schemaId requires a color"))
        canonicalSquares.collect:
          case square if (mask64 & square.bl) != 0L => colorSquareIndex(schemaId, resolvedColor, square)
      case SchemaFamily.ColorPawnSquare =>
        val resolvedColor = color.getOrElse(throw IllegalArgumentException(s"$schemaId requires a color"))
        canonicalPawnSquares.collect:
          case square if (mask64 & square.bl) != 0L => colorPawnSquareIndex(schemaId, resolvedColor, square).get
      case SchemaFamily.NeutralSquare =>
        canonicalSquares.collect:
          case square if (mask64 & square.bl) != 0L => neutralSquareIndex(schemaId, square)
      case other =>
        throw IllegalArgumentException(s"$schemaId does not use a square mask: $other")

  def indicesForFileMask(schemaId: String, mask8: Int, color: Option[Color] = None): Vector[Int] =
    requireSchema(schemaId).family match
      case SchemaFamily.ColorFile =>
        val resolvedColor = color.getOrElse(throw IllegalArgumentException(s"$schemaId requires a color"))
        canonicalFiles.collect:
          case file if (mask8 & (1 << file.value)) != 0 => colorFileIndex(schemaId, resolvedColor, file)
      case SchemaFamily.NeutralFile =>
        canonicalFiles.collect:
          case file if (mask8 & (1 << file.value)) != 0 => neutralFileIndex(schemaId, file)
      case other =>
        throw IllegalArgumentException(s"$schemaId does not use a file mask: $other")

  def decode(index: Int): Option[DecodedAtom] =
    all.find(_.contains(index)).map: schema =>
      val atomId =
        schema.family match
          case SchemaFamily.ColorPieceSquare =>
            val local = index - schema.start
            val color = canonicalColors(local / 384)
            val role = canonicalRoles((local % 384) / 64)
            val square = canonicalSquares(local % 64)
            s"${schema.id}(${color.name},${role.name},${square.key})"
          case SchemaFamily.ColorSquare =>
            val local = index - schema.start
            val color = canonicalColors(local / 64)
            val square = canonicalSquares(local % 64)
            s"${schema.id}(${color.name},${square.key})"
          case SchemaFamily.ColorPawnSquare =>
            val local = index - schema.start
            val color = canonicalColors(local / 48)
            val square = canonicalPawnSquares(local % 48)
            s"${schema.id}(${color.name},${square.key})"
          case SchemaFamily.ColorFile =>
            val local = index - schema.start
            val color = canonicalColors(local / 8)
            val file = canonicalFiles(local % 8)
            s"${schema.id}(${color.name},${file.char})"
          case SchemaFamily.NeutralSquare =>
            val square = canonicalSquares(index - schema.start)
            s"${schema.id}(${square.key})"
          case SchemaFamily.NeutralFile =>
            val file = canonicalFiles(index - schema.start)
            s"${schema.id}(${file.char})"
          case SchemaFamily.SideToMoveState =>
            val color = canonicalColors(index - schema.start)
            s"${schema.id}:${sideToMoveState(color)}"
          case SchemaFamily.CastlingRightsState =>
            s"${schema.id}:${castlingRightsState(index - schema.start)}"
          case SchemaFamily.EnPassantState =>
            s"${schema.id}:${enPassantState(index).getOrElse("unknown")}"
      DecodedAtom(index = index, schema = schema, atomId = atomId)
