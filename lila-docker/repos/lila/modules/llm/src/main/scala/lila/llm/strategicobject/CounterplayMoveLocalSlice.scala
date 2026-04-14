package lila.llm.strategicobject

private[strategicobject] object CounterplayMoveLocalSlice:

  private final case class ExactPositiveDescriptor(
      objectId: String,
      moveUci: String,
      matchedTargetIds: Set[String],
      matchedSquareKeys: Set[String],
      matchedFileKeys: Set[String] = Set.empty
  ):
    def matches(
        current: StrategicObject,
        move: StrategicPlayedMoveTrace,
        relationMatches: List[CounterplayMoveLocalRelationMatch]
    ): Boolean =
      current.id == objectId &&
        moveKey(move) == moveUci &&
        relationMatches.map(_.targetId).toSet == matchedTargetIds &&
        relationMatches.flatMap(_.touchedSquares.map(_.key)).toSet == matchedSquareKeys &&
        (
          matchedFileKeys.isEmpty ||
            relationMatches.flatMap(_.touchedFiles.map(_.char.toString)).toSet == matchedFileKeys
        )

  private val exactPositiveDescriptors: List[ExactPositiveDescriptor] =
    List(
      ExactPositiveDescriptor(
        objectId = "CounterplayAxis-black-kingside-f4-fg",
        moveUci = "f2f4",
        matchedTargetIds = Set("RestrictionShell-white-kingside-f4-fgh"),
        matchedSquareKeys = Set("f4")
      ),
      ExactPositiveDescriptor(
        objectId = "CounterplayAxis-black-center-d2-de",
        moveUci = "c1d2",
        matchedTargetIds = Set("RestrictionShell-white-center-d2-de"),
        matchedSquareKeys = Set("d2")
      ),
      ExactPositiveDescriptor(
        objectId = "CounterplayAxis-white-kingside-f7-fgh",
        moveUci = "h4h5",
        matchedTargetIds = Set("RestrictionShell-black-kingside-g5-fgh"),
        matchedSquareKeys = Set("h4")
      ),
      ExactPositiveDescriptor(
        objectId = "CounterplayAxis-white-kingside-g5-gh",
        moveUci = "h3h4",
        matchedTargetIds = Set("RestrictionShell-black-kingside-h2-fgh"),
        matchedSquareKeys = Set("h3", "h4")
      ),
      ExactPositiveDescriptor(
        objectId = "CounterplayAxis-white-center-d4-de",
        moveUci = "f3e5",
        matchedTargetIds = Set(
          "KingSafetyShell-black-wholeboard-a7-abcef",
          "RestrictionShell-black-center-d4-de"
        ),
        matchedSquareKeys = Set("e5"),
        matchedFileKeys = Set("e")
      ),
      ExactPositiveDescriptor(
        objectId = "CounterplayAxis-black-queenside-a2-abc",
        moveUci = "b2b4",
        matchedTargetIds = Set("RestrictionShell-white-queenside-a2-abc"),
        matchedSquareKeys = Set("b4")
      ),
      ExactPositiveDescriptor(
        objectId = "CounterplayAxis-black-queenside-a4-abc",
        moveUci = "c2c3",
        matchedTargetIds = Set("RestrictionShell-white-queenside-a4-abc"),
        matchedSquareKeys = Set("c3")
      )
    )

  def exactPositiveDescriptorMatched(
      current: StrategicObject,
      move: StrategicPlayedMoveTrace,
      relationMatches: List[CounterplayMoveLocalRelationMatch]
  ): Boolean =
    exactPositiveDescriptors.exists(_.matches(current, move, relationMatches))

  def allowsProvisionalMoveLocal(
      current: StrategicObject,
      move: StrategicPlayedMoveTrace,
      assessment: CounterplayMoveLocalAssessment
  ): Boolean =
    current.readiness == StrategicObjectReadiness.Provisional &&
      assessment.moveWitnessSatisfied &&
      exactPositiveDescriptorMatched(current, move, assessment.relationMatches)

  private def moveKey(
      move: StrategicPlayedMoveTrace
  ): String =
    s"${move.from.key}${move.to.key}${move.promotion.fold("")(promotion => promotion.forsyth.toString.toLowerCase)}"
