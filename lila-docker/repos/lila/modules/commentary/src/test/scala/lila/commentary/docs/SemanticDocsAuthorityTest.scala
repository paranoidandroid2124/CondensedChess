package lila.commentary.docs

import java.nio.file.{ Files, Paths }

import scala.jdk.CollectionConverters.*

class SemanticDocsAuthorityTest extends munit.FunSuite:

  private val docsRoot = Paths.get("modules/commentary/docs")

  test("semantic model branch exposes only reset docs as live authority"):
    val liveDocs =
      Files
        .list(docsRoot)
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path))
        .map(path => path.getFileName.toString)
        .toVector
        .sorted

    assertEquals(
      liveDocs,
      Vector(
        "CommentaryCoreSSOT.md",
        "LegacyArchiveIndex.md",
        "README.md",
        "SemanticModelArchitecture.md"
      )
    )
    assert(Files.isDirectory(docsRoot.resolve("legacy-pre-semantic-reset")))

  test("live authority declares legacy docs historical only"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val core = Files.readString(docsRoot.resolve("CommentaryCoreSSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("SemanticModelArchitecture.md"))

    val normalizedReadme = readme.replaceAll("\\s+", " ")

    assert(normalizedReadme.contains("historical reference only"))
    assert(normalizedReadme.contains("they do not grant runtime authority"))
    assert(core.contains("public-claim paths are legacy behavior"))
    assert(architecture.contains("The model replaces selector authority, not renderer authority."))
