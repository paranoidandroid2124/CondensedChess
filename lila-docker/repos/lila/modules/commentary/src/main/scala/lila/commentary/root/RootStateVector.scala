package lila.commentary.root

import java.util.Arrays

import chess.{ Color, Role }

final class RootStateVector private (private val words: Array[Long]):

  import RootAtomRegistry.*

  def contains(index: Int): Boolean =
    requireValidIndex(index)
    val word = words(index >>> 6)
    (word & (1L << (index & 63))) != 0L

  def activeIndices: Vector[Int] =
    val builder = Vector.newBuilder[Int]
    var wordIndex = 0
    while wordIndex < words.length do
      var word = words(wordIndex)
      while word != 0L do
        val bit = java.lang.Long.numberOfTrailingZeros(word)
        val index = wordIndex * 64 + bit
        if index < RootSize then builder += index
        word &= (word - 1L)
      wordIndex += 1
    builder.result()

  def activeIndicesForSchema(schemaId: String): Vector[Int] =
    val schema = requireSchema(schemaId)
    activeIndices.filter(schema.contains)

  def activeAtomIds: Vector[String] =
    activeIndices.flatMap(index => decode(index).map(_.atomId))

  def mirrorColorSwap: RootStateVector =
    RootStateVector.fromIndices(activeIndices.map(RootAtomRegistry.mirrorColorSwapIndex))

  def squareMask64(
      schemaId: String,
      color: Option[Color] = None,
      role: Option[Role] = None
  ): Option[Long] =
    requireSchema(schemaId).family match
      case SchemaFamily.ColorPieceSquare =>
        for
          resolvedColor <- color
          resolvedRole <- role
        yield canonicalSquares.foldLeft(0L): (mask, square) =>
          if contains(pieceOnIndex(resolvedColor, resolvedRole, square)) then mask | square.bl else mask
      case SchemaFamily.ColorSquare =>
        color.map: resolvedColor =>
          canonicalSquares.foldLeft(0L): (mask, square) =>
            if contains(colorSquareIndex(schemaId, resolvedColor, square)) then mask | square.bl else mask
      case SchemaFamily.ColorPawnSquare =>
        color.map: resolvedColor =>
          canonicalPawnSquares.foldLeft(0L): (mask, square) =>
            val index = colorPawnSquareIndex(schemaId, resolvedColor, square).get
            if contains(index) then mask | square.bl else mask
      case SchemaFamily.NeutralSquare =>
        Some(canonicalSquares.foldLeft(0L): (mask, square) =>
          if contains(neutralSquareIndex(schemaId, square)) then mask | square.bl else mask
        )
      case _ => None

  def fileMask8(schemaId: String, color: Option[Color] = None): Option[Int] =
    requireSchema(schemaId).family match
      case SchemaFamily.ColorFile =>
        color.map: resolvedColor =>
          canonicalFiles.foldLeft(0): (mask, file) =>
            if contains(colorFileIndex(schemaId, resolvedColor, file)) then mask | (1 << file.value) else mask
      case SchemaFamily.NeutralFile =>
        Some(canonicalFiles.foldLeft(0): (mask, file) =>
          if contains(neutralFileIndex(schemaId, file)) then mask | (1 << file.value) else mask
        )
      case _ => None

  override def equals(other: Any): Boolean =
    other match
      case that: RootStateVector => Arrays.equals(words, that.words)
      case _ => false

  override def hashCode(): Int = Arrays.hashCode(words)

  override def toString: String =
    s"RootStateVector(${activeAtomIds.mkString(", ")})"

  private def requireValidIndex(index: Int): Unit =
    require(0 <= index && index < RootSize, s"Invalid root index: $index")

object RootStateVector:

  val empty: RootStateVector = new RootStateVector(new Array[Long](RootAtomRegistry.WordCount))

  def builder: Builder = new Builder

  def fromIndices(indices: IterableOnce[Int]): RootStateVector =
    val builder = new Builder
    indices.iterator.foreach(builder.set)
    builder.result()

  final class Builder private[root] ():
    private val words = new Array[Long](RootAtomRegistry.WordCount)

    def set(index: Int): Builder =
      require(0 <= index && index < RootAtomRegistry.RootSize, s"Invalid root index: $index")
      words(index >>> 6) |= 1L << (index & 63)
      this

    def setAll(indices: IterableOnce[Int]): Builder =
      indices.iterator.foreach(set)
      this

    def result(): RootStateVector = new RootStateVector(words.clone())
