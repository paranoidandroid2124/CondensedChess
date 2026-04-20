package lila.commentary.witness.u

import lila.commentary.witness.WitnessDescriptorId

object UAttachedScopeContract:

  val activeAttachedDescriptorIds: Vector[WitnessDescriptorId] = Vector(
    WitnessDescriptorId("structural_space_claim")
  )

  private val activeAttachedDescriptorIdSet: Set[WitnessDescriptorId] =
    activeAttachedDescriptorIds.toSet
  private val activeAttachedDescriptorIdValues: Set[String] =
    activeAttachedDescriptorIds.map(_.value).toSet

  val shellOnlyRows: Set[String] = Set(
    "material gain",
    "structural damage",
    "center",
    "kingside",
    "queenside",
    "whole-board",
    "closed center",
    "fixed chain",
    "open line",
    "create passer"
  )

  val shellOnlyDescriptorIds: Set[WitnessDescriptorId] =
    shellOnlyRows.map(row => WitnessDescriptorId(normalizeHostLabel(row)))
  private val shellOnlyDescriptorIdValues: Set[String] =
    shellOnlyDescriptorIds.map(_.value)

  val hostVocabularyRows: Set[String] = Set(
    "closed center",
    "fixed chain"
  )

  val hostVocabularyDescriptorIds: Set[WitnessDescriptorId] =
    hostVocabularyRows.map(row => WitnessDescriptorId(normalizeHostLabel(row)))
  private val hostVocabularyDescriptorIdValues: Set[String] =
    hostVocabularyDescriptorIds.map(_.value)

  val allowedHostLabelsByDescriptor: Map[WitnessDescriptorId, Set[String]] = Map(
    WitnessDescriptorId("structural_space_claim") -> Set("closed center", "fixed chain")
  )

  val disallowedHostLabelsByDescriptor: Map[WitnessDescriptorId, Set[String]] = Map(
    WitnessDescriptorId("structural_space_claim") -> Set("majority/minority asymmetry", "restriction geometry")
  )

  val allowedHostIdsByDescriptor: Map[WitnessDescriptorId, Set[String]] =
    allowedHostLabelsByDescriptor.view.mapValues(_.map(normalizeHostLabel)).toMap

  val disallowedHostIdsByDescriptor: Map[WitnessDescriptorId, Set[String]] =
    disallowedHostLabelsByDescriptor.view.mapValues(_.map(normalizeHostLabel)).toMap

  require(
    activeAttachedDescriptorIds.size == 1,
    s"Expected exactly 1 active U-attached descriptor id, found ${activeAttachedDescriptorIds.size}"
  )
  require(
    activeAttachedDescriptorIds.distinct.size == activeAttachedDescriptorIds.size,
    "Duplicate active U-attached descriptor ids are not allowed"
  )
  require(
    shellOnlyRows.size == 10,
    s"Expected exactly 10 shell-only attached rows, found ${shellOnlyRows.size}"
  )
  require(
    shellOnlyDescriptorIds.size == shellOnlyRows.size,
    "Shell-only attached rows must normalize to distinct runtime descriptor ids"
  )
  require(
    activeAttachedDescriptorIdValues.intersect(shellOnlyDescriptorIdValues).isEmpty,
    "Shell-only attached rows must not be admitted as active runtime descriptor ids"
  )
  require(
    hostVocabularyRows.subsetOf(shellOnlyRows),
    "Host vocabulary rows must stay inside the shell-only row set"
  )
  require(
    hostVocabularyDescriptorIdValues.subsetOf(shellOnlyDescriptorIdValues),
    "Host vocabulary rows must stay inside the shell-only runtime descriptor set"
  )
  require(
    allowedHostLabelsByDescriptor.keySet == activeAttachedDescriptorIdSet,
    "Every active U-attached descriptor must declare its allowed host vocabulary"
  )
  require(
    disallowedHostLabelsByDescriptor.keySet == activeAttachedDescriptorIdSet,
    "Every active U-attached descriptor must declare its disallowed host vocabulary"
  )
  require(
    allowedHostLabelsByDescriptor.valuesIterator.flatten.toSet.subsetOf(hostVocabularyRows),
    "Allowed host vocabulary must stay inside the frozen host-vocabulary row set"
  )
  require(
    disallowedHostLabelsByDescriptor.valuesIterator.flatten.toSet.intersect(hostVocabularyRows).isEmpty,
    "Disallowed host labels must not overlap the frozen host-vocabulary row set"
  )
  require(
    allowedHostIdsByDescriptor.valuesIterator.flatten.toSet.subsetOf(hostVocabularyDescriptorIdValues),
    "Allowed host runtime ids must stay inside the frozen host-vocabulary descriptor set"
  )
  require(
    allowedHostIdsByDescriptor.valuesIterator.flatten.toSet.intersect(
      disallowedHostIdsByDescriptor.valuesIterator.flatten.toSet
    ).isEmpty,
    "Allowed and disallowed host runtime ids must stay disjoint"
  )

  def isActiveAttachedDescriptorId(id: WitnessDescriptorId): Boolean =
    activeAttachedDescriptorIdValues.contains(id.value)

  def isShellOnlyAttachedDescriptorId(id: WitnessDescriptorId): Boolean =
    shellOnlyDescriptorIdValues.contains(id.value)

  def normalizeHostLabel(label: String): String =
    label.trim.toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .replaceAll("^_+|_+$", "")

  def isAllowedHostId(descriptorId: WitnessDescriptorId, hostId: String): Boolean =
    requireActiveAttachedDescriptorIds(Vector(descriptorId))

    val normalizedHostId = normalizeHostLabel(hostId)

    allowedHostIdsByDescriptor.getOrElse(descriptorId, Set.empty).contains(normalizedHostId) &&
      !disallowedHostIdsByDescriptor.getOrElse(descriptorId, Set.empty).contains(normalizedHostId)

  private[witness] def requireActiveAttachedDescriptorIds(
      descriptorIds: IterableOnce[WitnessDescriptorId]
  ): Unit =
    val normalizedDescriptorIds =
      descriptorIds.iterator
        .map(_.value)
        .toSet
        .toVector
        .sorted

    val shellOnly = normalizedDescriptorIds.filter(shellOnlyDescriptorIdValues)
    val outOfScope =
      normalizedDescriptorIds.filterNot(id =>
        activeAttachedDescriptorIdValues.contains(id) || shellOnlyDescriptorIdValues.contains(id)
      )

    require(
      shellOnly.isEmpty,
      s"Shell-only U-attached descriptor ids cannot be registered: ${shellOnly.mkString(", ")}"
    )

    require(
      outOfScope.isEmpty,
      s"Out-of-scope U-attached descriptor ids: ${outOfScope.mkString(", ")}"
    )
