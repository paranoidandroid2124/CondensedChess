package lila.commentary.analysis.semantic.evidence

import munit.FunSuite

class StrategicIdeaEvidenceSupportTest extends FunSuite:

  test("normalizeFileToken shares break-file boundary and rejects prose prefixes") {
    assertEquals(StrategicIdeaEvidenceSupport.normalizeFileToken("neutralized_break:d5"), Some("d"))
    assertEquals(StrategicIdeaEvidenceSupport.normalizeFileToken("deny_c_file"), Some("c"))
    assertEquals(StrategicIdeaEvidenceSupport.normalizeFileToken("hidden_freeing_break"), None)
    assertEquals(StrategicIdeaEvidenceSupport.normalizeFileToken("stops a break"), None)
  }
