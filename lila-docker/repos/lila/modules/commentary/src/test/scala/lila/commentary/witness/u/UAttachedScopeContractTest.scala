package lila.commentary.witness.u

import lila.commentary.witness.{ Witness, WitnessDescriptorId }

class UAttachedScopeContractTest extends munit.FunSuite:

  test("u attached scope contract is frozen to structural_space_claim plus shell-only 10"):
    assertEquals(
      UAttachedScopeContract.activeAttachedDescriptorIds.map(_.value),
      Vector("structural_space_claim")
    )
    assertEquals(
      UAttachedScopeContract.shellOnlyRows,
      Set(
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
    )
    assertEquals(
      UAttachedScopeContract.shellOnlyDescriptorIds.map(_.value),
      Set(
        "material_gain",
        "structural_damage",
        "center",
        "kingside",
        "queenside",
        "whole_board",
        "closed_center",
        "fixed_chain",
        "open_line",
        "create_passer"
      )
    )
    assertEquals(
      UAttachedScopeContract.hostVocabularyRows,
      Set("closed center", "fixed chain")
    )
    assertEquals(
      UAttachedScopeContract.hostVocabularyDescriptorIds.map(_.value),
      Set("closed_center", "fixed_chain")
    )
    assertEquals(
      UAttachedScopeContract.allowedHostLabelsByDescriptor(WitnessDescriptorId("structural_space_claim")),
      Set("closed center", "fixed chain")
    )
    assertEquals(
      UAttachedScopeContract.disallowedHostLabelsByDescriptor(WitnessDescriptorId("structural_space_claim")),
      Set("majority/minority asymmetry", "restriction geometry")
    )

  test("u attached scope contract rejects shell-only or out-of-scope descriptor registration"):
    val shellOnly =
      intercept[IllegalArgumentException]:
        UAttachedScopeContract.requireActiveAttachedDescriptorIds(
          Vector(
            WitnessDescriptorId("structural_space_claim"),
            WitnessDescriptorId("open_line")
          )
        )

    assert(shellOnly.getMessage.contains("Shell-only U-attached descriptor ids cannot be registered"))

    val outOfScope =
      intercept[IllegalArgumentException]:
        UAttachedScopeContract.requireActiveAttachedDescriptorIds(
          Vector(WitnessDescriptorId("pin"))
        )

    assert(outOfScope.getMessage.contains("Out-of-scope U-attached descriptor ids"))

  test("u attached scope contract canonicalizes host labels and blocks non-host vocabulary ids"):
    val descriptorId = WitnessDescriptorId("structural_space_claim")

    assert(UAttachedScopeContract.isAllowedHostId(descriptorId, "closed center"))
    assert(UAttachedScopeContract.isAllowedHostId(descriptorId, "closed_center"))
    assert(UAttachedScopeContract.isAllowedHostId(descriptorId, "fixed-chain"))
    assert(!UAttachedScopeContract.isAllowedHostId(descriptorId, "center"))
    assert(!UAttachedScopeContract.isAllowedHostId(descriptorId, "majority/minority asymmetry"))
    assert(!UAttachedScopeContract.isAllowedHostId(descriptorId, "majority_minority_asymmetry"))
    assert(!UAttachedScopeContract.isAllowedHostId(descriptorId, "restriction_geometry"))

  test("u attached runtime registry rejects duplicate or out-of-scope rules before extraction"):
    final case class TestRule(descriptorId: WitnessDescriptorId) extends UScopedAttachedRule:
      def extract(context: UExtractionContext): Vector[Witness] = Vector.empty

    val claim = TestRule(WitnessDescriptorId("structural_space_claim"))

    UAttachedScopeContract.requireActiveAttachedDescriptorIds(
      Vector(claim.descriptorId)
    )

    intercept[IllegalArgumentException]:
      UAttachedInternalRuntime.validateRegisteredRules(Vector(claim, claim))

    intercept[IllegalArgumentException]:
      UAttachedInternalRuntime.validateRegisteredRules(
        Vector(TestRule(WitnessDescriptorId("open_line")))
      )

    val validated = UAttachedInternalRuntime.validateRegisteredRules(Vector(claim))
    assertEquals(validated.map(_.descriptorId.value), Vector("structural_space_claim"))
