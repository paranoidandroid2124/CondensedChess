package lila.commentary.witness.u

import chess.{ Color, Pawn, Square }

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors, canonicalSquares }
import lila.commentary.witness.*
import lila.commentary.witness.u.UWitnessHelpers.*

private[u] final case class StructuralHostCandidate(
    hostId: String,
    boundaryPawnSquares: Vector[Square],
    variant: Option[WitnessVariantId],
    hostOwner: Option[Color] = None
)

private[u] final case class StructuralClaimCandidate(
    host: StructuralHostCandidate,
    beneficiaryColor: Color,
    claimedSquares: Vector[Square],
    attachedSeeds: Vector[Square]
)

private[u] object StructuralSpaceClaimRule extends UScopedAttachedRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("structural_space_claim")

  private val ClosedCenterHostId = UAttachedScopeContract.normalizeHostLabel("closed center")
  private val FixedChainHostId = UAttachedScopeContract.normalizeHostLabel("fixed chain")

  private val ClosedCenterHostVariant = Some(WitnessVariantId("closed_center_host"))
  private val FixedChainHostVariantByOwner: Map[Color, Option[WitnessVariantId]] = Map(
    Color.White -> Some(WitnessVariantId("fixed_chain_host_white_segment")),
    Color.Black -> Some(WitnessVariantId("fixed_chain_host_black_segment"))
  )

  def extract(context: UExtractionContext): Vector[Witness] =
    WitnessSector.values.toVector.flatMap: sector =>
      val claimCandidates =
        hostCandidates(context, sector).flatMap: host =>
          canonicalColors.flatMap: beneficiaryColor =>
            bestClaimCandidate(context, host, beneficiaryColor, sector)

      claimCandidates
        .groupBy(candidate =>
          (candidate.beneficiaryColor, candidate.host.variant, candidate.host.hostOwner)
        )
        .values
        .flatMap(_.sortBy(claimCandidateSortKey).headOption.map(candidate =>
          beneficiary(
            color = candidate.beneficiaryColor,
            anchor = WitnessAnchor.SectorAnchor(sector),
            payload = WitnessPayload.from(
              Vector(
                "host_id" -> WitnessValue.Token(candidate.host.hostId),
                "sector" -> WitnessValue.SectorValue(sector),
                "beneficiary" -> WitnessValue.ColorValue(candidate.beneficiaryColor),
                "claimed_squares" -> WitnessValue.SquareListValue(candidate.claimedSquares),
                "boundary_pawn_squares" -> WitnessValue.SquareListValue(candidate.host.boundaryPawnSquares)
              ) ++ candidate.host.hostOwner.map(color => "host_owner" -> WitnessValue.ColorValue(color))
            ),
            support = rootSupport(
              indices =
                candidate.host.boundaryPawnSquares.flatMap(square =>
                  Vector(
                    context.pieceOnRootIndex(Color.White, Pawn, square),
                    context.pieceOnRootIndex(Color.Black, Pawn, square),
                    context.colorPawnSquareRootIndex(SchemaId.FixedPawn, Color.White, square),
                    context.colorPawnSquareRootIndex(SchemaId.FixedPawn, Color.Black, square)
                  ).flatten
                ) ++ candidate.claimedSquares.flatMap(square =>
                  context.colorSquareRootIndex(SchemaId.ControlledBy, candidate.beneficiaryColor, square)
                ),
              targetSquares = candidate.claimedSquares
            ),
            variant = candidate.host.variant
          )
        ))
        .toVector

  private def hostCandidates(
      context: UExtractionContext,
      sector: WitnessSector
  ): Vector[StructuralHostCandidate] =
    val rawCandidates =
      closedCenterHost(context, sector).toVector ++ fixedChainHosts(context, sector)

    rawCandidates.filter(host => UAttachedScopeContract.isAllowedHostId(descriptorId, host.hostId))

  private def closedCenterHost(
      context: UExtractionContext,
      sector: WitnessSector
  ): Option[StructuralHostCandidate] =
    Option.when(sector == WitnessSector.Center):
      val whiteBoundary = fixedPawnsInSector(context, Color.White, sector)
      val blackBoundary = fixedPawnsInSector(context, Color.Black, sector)
      val boundarySquares = (whiteBoundary ++ blackBoundary).distinct.sortBy(_.value)
      val distinctCenterFiles = boundarySquares.map(_.file).distinct

      Option.when(
        whiteBoundary.nonEmpty &&
          blackBoundary.nonEmpty &&
          distinctCenterFiles.size == 2 &&
          boundarySquares.size >= 4 &&
          formsConnectedFront(boundarySquares)
      )(
        StructuralHostCandidate(
          hostId = ClosedCenterHostId,
          boundaryPawnSquares = boundarySquares,
          variant = ClosedCenterHostVariant
        )
      )
    .flatten

  private def fixedChainHosts(
      context: UExtractionContext,
      sector: WitnessSector
  ): Vector[StructuralHostCandidate] =
    canonicalColors.flatMap: color =>
      fixedChainBoundarySegments(context, color, sector).map: boundarySquares =>
        StructuralHostCandidate(
          hostId = FixedChainHostId,
          boundaryPawnSquares = boundarySquares,
          variant = FixedChainHostVariantByOwner(color),
          hostOwner = Some(color)
        )

  private def fixedPawnsInSector(
      context: UExtractionContext,
      color: Color,
      sector: WitnessSector
  ): Vector[Square] =
    context.activePieceSquares(color, Pawn).filter: square =>
      sectorOf(square.file) == sector &&
        context.hasColorPawnSquare(SchemaId.FixedPawn, color, square)
    .sortBy(_.value)

  private def fixedChainBoundarySegments(
      context: UExtractionContext,
      color: Color,
      sector: WitnessSector
  ): Vector[Vector[Square]] =
    val fixedSquares = fixedPawnsInSector(context, color, sector)
    val adjacency =
      fixedSquares.map: square =>
        square -> fixedSquares.filter(other =>
          other != square &&
            isChainNeighbor(color, square, other)
        ).toSet
      .toMap

    val components = connectedComponents(fixedSquares, adjacency)
    val candidateComponents =
      components.filter(component => component.size >= 2 && isRearSupportedSegment(component, color))

    candidateComponents
      .map(_.toVector.sortBy(_.value))
      .sortBy(component => (-component.size, component.map(_.value).min))

  private def isChainNeighbor(color: Color, left: Square, right: Square): Boolean =
    left.pawnAttacks(color).contains(right) || right.pawnAttacks(color).contains(left)

  private def connectedComponents(
      fixedSquares: Vector[Square],
      adjacency: Map[Square, Set[Square]]
  ): Vector[Set[Square]] =
    val remaining = scala.collection.mutable.Set.from(fixedSquares)
    val components = Vector.newBuilder[Set[Square]]

    while remaining.nonEmpty do
      val start = remaining.head
      val queue = scala.collection.mutable.Queue(start)
      val component = scala.collection.mutable.Set.empty[Square]

      while queue.nonEmpty do
        val square = queue.dequeue()
        if remaining.contains(square) then
          remaining -= square
          component += square
          adjacency.getOrElse(square, Set.empty).filter(remaining.contains).foreach(queue.enqueue(_))

      components += component.toSet

    components.result()

  private def isRearSupportedSegment(
      component: Set[Square],
      color: Color
  ): Boolean =
    val successors =
      component.iterator.map: square =>
        square -> component.filter(other => square.pawnAttacks(color).contains(other))
      .toMap
    val predecessors =
      component.iterator.map: square =>
        square -> component.filter(other => other.pawnAttacks(color).contains(square))
      .toMap

    val hasSingleChainTopology =
      component.forall: square =>
        successors.getOrElse(square, Set.empty).size <= 1 &&
          predecessors.getOrElse(square, Set.empty).size <= 1

    val rears = component.filter(square => predecessors.getOrElse(square, Set.empty).isEmpty)
    val fronts = component.filter(square => successors.getOrElse(square, Set.empty).isEmpty)

    hasSingleChainTopology &&
    rears.size == 1 &&
    fronts.size == 1 &&
    traversesWholeSegment(rears.head, successors, component.size)

  private def traversesWholeSegment(
      rear: Square,
      successors: Map[Square, Set[Square]],
      expectedLength: Int
  ): Boolean =
    val visited = scala.collection.mutable.Set.empty[Square]
    var current = Option(rear)

    while current.nonEmpty do
      val square = current.get
      if visited.contains(square) then return false
      visited += square
      current = successors.getOrElse(square, Set.empty).headOption

    visited.size == expectedLength

  private def bestClaimCandidate(
      context: UExtractionContext,
      host: StructuralHostCandidate,
      beneficiaryColor: Color,
      sector: WitnessSector,
  ): Option[StructuralClaimCandidate] =
    val frontierSeeds = attachedFrontierSeeds(context, beneficiaryColor, sector, host.boundaryPawnSquares)
    val attachedComponents =
      attachedClaimComponents(context, beneficiaryColor, sector, frontierSeeds)

    attachedComponents
      .map(component => component -> attachedSeedsForComponent(component, frontierSeeds))
      .filter { case (component, _) => component.size >= 2 }
      .sortBy { case (component, attachedSeeds) =>
        (
          -component.size,
          -attachedSeeds.size,
          component.head.value
        )
      }
      .headOption
      .map { case (component, attachedSeeds) =>
        StructuralClaimCandidate(
          host = host,
          beneficiaryColor = beneficiaryColor,
          claimedSquares = component,
          attachedSeeds = attachedSeeds
        )
      }

  private def attachedFrontierSeeds(
      context: UExtractionContext,
      beneficiaryColor: Color,
      sector: WitnessSector,
      boundaryPawnSquares: Vector[Square]
  ): Vector[Square] =
    boundaryPawnSquares.flatMap(square =>
      context.forwardSquare(beneficiaryColor, square).filter(target =>
        sectorOf(target.file) == sector &&
          context.hasColorSquare(SchemaId.ControlledBy, beneficiaryColor, target)
      )
    ).distinct.sortBy(_.value)

  private def attachedClaimComponents(
      context: UExtractionContext,
      beneficiaryColor: Color,
      sector: WitnessSector,
      frontierSeeds: Vector[Square]
  ): Vector[Vector[Square]] =
    val claimSquares =
      sectorSquares(sector).filter(square =>
        context.pieceAt(square).isEmpty &&
          context.hasColorSquare(SchemaId.ControlledBy, beneficiaryColor, square)
      )
    val adjacency =
      claimSquares.map: square =>
        square -> claimSquares.filter(other =>
          other != square &&
            square.kingAttacks.contains(other)
        ).toSet
      .toMap

    connectedComponents(claimSquares, adjacency)
      .map(_.toVector.sortBy(_.value))
      .filter(component => attachedSeedsForComponent(component, frontierSeeds).nonEmpty)
      .sortBy(component => (-component.size, component.head.value))

  private def attachedSeedsForComponent(
      component: Vector[Square],
      frontierSeeds: Vector[Square]
  ): Vector[Square] =
    frontierSeeds.filter(seed =>
      component.exists(square =>
        square == seed || square.kingAttacks.contains(seed)
      )
    )

  private def claimCandidateSortKey(candidate: StructuralClaimCandidate): (Int, Int, Int, Int, Int) =
    (
      -candidate.claimedSquares.size,
      -candidate.attachedSeeds.size,
      -candidate.host.boundaryPawnSquares.size,
      candidate.claimedSquares.head.value,
      candidate.host.boundaryPawnSquares.head.value
    )

  private def formsConnectedFront(boundarySquares: Vector[Square]): Boolean =
    val adjacency =
      boundarySquares.map: square =>
        square -> boundarySquares.filter(other =>
          other != square &&
            square.kingAttacks.contains(other)
        ).toSet
      .toMap

    connectedComponents(boundarySquares, adjacency).size == 1

  private def sectorSquares(sector: WitnessSector): Vector[Square] =
    canonicalSquares.filter(square => sectorOf(square.file) == sector)
