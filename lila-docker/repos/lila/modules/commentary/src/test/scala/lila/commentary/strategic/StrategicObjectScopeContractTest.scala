package lila.commentary.strategic

class StrategicObjectScopeContractTest extends munit.FunSuite:

  test("strategic object scope contract is frozen to the Object 7 families"):
    assertEquals(
      StrategicObjectScopeContract.activeObjectFamilyIds.map(_.value),
      Vector(
        "OpeningDevelopmentRegime",
        "DistributedContactRegime",
        "EndgameRaceScaffold",
        "AttackScaffold",
        "FortressHoldingShell",
        "KingSafetyShell",
        "CentralContactFront"
      )
    )

  test("strategic object scope contract rejects duplicate or out-of-scope family ids"):
    intercept[IllegalArgumentException]:
      StrategicObjectScopeContract.requireActiveObjectFamilyIds(
        Vector(
          StrategicObjectId("OpeningDevelopmentRegime"),
          StrategicObjectId("OpeningDevelopmentRegime")
        )
      )

    intercept[IllegalArgumentException]:
      StrategicObjectScopeContract.requireActiveObjectFamilyIds(
        Vector(StrategicObjectId("TradeInvariant"))
      )

  test("strategic object scope contract rejects missing active family drift"):
    intercept[IllegalArgumentException]:
      StrategicObjectScopeContract.requireExactActiveObjectFamilyIds(
        Vector(
          StrategicObjectId("OpeningDevelopmentRegime"),
          StrategicObjectId("DistributedContactRegime"),
          StrategicObjectId("EndgameRaceScaffold"),
          StrategicObjectId("AttackScaffold"),
          StrategicObjectId("FortressHoldingShell"),
          StrategicObjectId("KingSafetyShell")
        )
      )

  test("strategic object scope contract rejects reordered active family drift"):
    intercept[IllegalArgumentException]:
      StrategicObjectScopeContract.requireExactActiveObjectFamilyIds(
        Vector(
          StrategicObjectId("DistributedContactRegime"),
          StrategicObjectId("OpeningDevelopmentRegime"),
          StrategicObjectId("EndgameRaceScaffold"),
          StrategicObjectId("AttackScaffold"),
          StrategicObjectId("FortressHoldingShell"),
          StrategicObjectId("KingSafetyShell"),
          StrategicObjectId("CentralContactFront")
        )
      )

  test("strategic object runtime rejects reordered registration at the live boundary"):
    intercept[IllegalArgumentException]:
      StrategicInternalRuntime.validateRegisteredRules(
        Vector(
          DistributedContactRegimeRule,
          OpeningDevelopmentRegimeRule,
          EndgameRaceScaffoldRule,
          AttackScaffoldRule,
          FortressHoldingShellRule,
          KingSafetyShellRule,
          CentralContactFrontRule
        )
      )
