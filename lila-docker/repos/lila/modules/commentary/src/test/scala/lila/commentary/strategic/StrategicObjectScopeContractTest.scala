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
