package lila.commentary.analysis

import scala.io.Source

import munit.FunSuite
import play.api.libs.json.*

class PlayerFacingSupportPolicyContractTest extends FunSuite:

  private case class LabelCase(raw: String, expected: String)
  private case class ContractFile(labelCases: List[LabelCase])

  private given Reads[LabelCase] = Json.reads[LabelCase]
  private given Reads[ContractFile] = Json.reads[ContractFile]

  private val contract: ContractFile =
    val source = Source.fromResource("playerFacingSupportContract.json")
    try Json.parse(source.mkString).as[ContractFile]
    finally source.close()

  test("surface-label cleanup matches shared contract") {
    contract.labelCases.foreach { entry =>
      assertEquals(
        PlayerFacingSupportPolicy.cleanNarrativeSurfaceLabel(entry.raw),
        entry.expected,
        clue(entry)
      )
    }
  }
