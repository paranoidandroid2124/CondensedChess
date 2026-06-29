package lila.study

import lila.core.study.Visibility

class StudyFormTest extends munit.FunSuite:

  test("import game form defaults new review studies to shared by link"):
    val result = StudyForm.importGame.form.bind(Map.empty[String, String]).get

    assertEquals(result.visibility, Visibility.unlisted)

  test("import game form accepts public review studies"):
    val result = StudyForm.importGame.form.bind(Map("visibility" -> "public")).get

    assertEquals(result.visibility, Visibility.public)

  test("import game form accepts private review studies"):
    val result = StudyForm.importGame.form.bind(Map("visibility" -> "private")).get

    assertEquals(result.visibility, Visibility.`private`)

  test("import game form accepts review study and first section names"):
    val result = StudyForm.importGame.form
      .bind(
        Map(
          "name" -> "ych24 vs RojoCapo review",
          "chapterName" -> "Opening to middlegame"
        )
      )
      .get

    assertEquals(result.studyName.map(_.value), Some("ych24 vs RojoCapo review"))
    assertEquals(result.chapterName.map(_.value), Some("Opening to middlegame"))
