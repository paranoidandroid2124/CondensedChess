package lila.llm.strategicobject

import lila.llm.analysis.{ DecisiveTruthContract, MoveTruthFrame }
import munit.FunSuite

import java.util.regex.Pattern
import java.nio.file.{ Files, Path, Paths }
import scala.jdk.CollectionConverters.*

class PrimitiveBoundaryTest extends FunSuite:

  private val strategicObjectDir =
    resolveRepoPath(
      Paths.get("modules", "llm", "src", "main", "scala", "lila", "llm", "strategicobject"),
      Paths.get("lila-docker", "repos", "lila", "modules", "llm", "src", "main", "scala", "lila", "llm", "strategicobject")
    )

  test("raw evidence and board ingress stay confined to the primitive extractor boundary") {
    assertTokenUsers("RawPositionEvidence", List("PrimitiveExtraction.scala", "RawPositionEvidence.scala"))
    assertTokenUsers("PositionAnalyzer", List("RawPositionEvidence.scala"))
    assertTokenUsers("FactExtractor", List("RawPositionEvidence.scala"))
    assertTokenUsers("pieceAt(", List("PrimitiveExtraction.scala"))
    assertTokenUsers("attackers(", List("PrimitiveExtraction.scala"))
  }

  test("primitive extractor public signature stays singular") {
    val extractMethods = classOf[PrimitiveExtractor].getMethods.filter(_.getName == "extract")

    assertEquals(extractMethods.length, 1)
    assertEquals(
      extractMethods.head.getParameterTypes.toList,
      List(classOf[RawPositionEvidence], classOf[MoveTruthFrame], classOf[DecisiveTruthContract])
    )
    assertEquals(extractMethods.head.getReturnType, classOf[PrimitiveBank])
  }

  test("downstream strategicobject signatures stay above the primitive boundary") {
    assertMethodParameters(
      owner = classOf[StrategicObjectSynthesizer],
      method = "synthesize",
      expected = List(classOf[PrimitiveBank], classOf[MoveTruthFrame])
    )
    assertMethodParameters(
      owner = classOf[StrategicObjectDeltaProjector],
      method = "project",
      expected = List(classOf[DecisiveTruthContract], classOf[List[?]])
    )
    assertMethodParameters(
      owner = classOf[ClaimCertification],
      method = "certify",
      expected = List(classOf[DecisiveTruthContract], classOf[List[?]], classOf[List[?]])
    )
    assertMethodParameters(
      owner = classOf[QuestionPlanner],
      method = "plan",
      expected = List(classOf[DecisiveTruthContract], classOf[List[?]])
    )
    assertMethodParameters(
      owner = classOf[Renderer],
      method = "render",
      expected = List(classOf[PlannedQuestion], classOf[List[?]])
    )
  }

  test("downstream strategicobject files reject raw and legacy semantic ingress") {
    val downstreamFiles = List(
      "StrategicObjectFamily.scala",
      "StrategicObjectEvidence.scala",
      "StrategicObjectProfile.scala",
      "StrategicObject.scala",
      "StrategicObjectSynthesizer.scala",
      "StrategicObjectDeltaProjector.scala",
      "ClaimCertification.scala",
      "QuestionPlanner.scala",
      "Renderer.scala"
    )
    val forbiddenTokens = List(
      "RawPositionEvidence",
      "BoardFeatureEvidence",
      "PositionAnalyzer",
      "FactExtractor",
      "lila.llm.model.Fact",
      "PlanMatcher",
      "StrategyPackBuilder",
      "StrategyPackSurface",
      "PlayerFacingTruthModePolicy",
      "MainPathMoveDeltaClaimBuilder",
      "QuietMoveIntentBuilder",
      "CertifiedDecisionFrameBuilder",
      "QuestionFirstCommentaryPlanner",
      "Board",
      "Bitboard",
      "pieceAt(",
      "attackers(",
      "factsFor(",
      "staticFactsByColor",
      "semanticDigest",
      "supportDigest",
      "narrativeDigest",
      "fallbackText",
      "lessonText",
      "strategyName",
      "themeName"
    )

    downstreamFiles.foreach { fileName =>
      val content = read(strategicObjectDir.resolve(fileName))
      forbiddenTokens.foreach { token =>
        assert(
          !containsForbiddenToken(content, token),
          clue(s"$fileName must not contain forbidden token: $token")
        )
      }
    }
  }

  private def assertMethodParameters(
      owner: Class[?],
      method: String,
      expected: List[Class[?]]
  ): Unit =
    val actual =
      owner.getMethods
        .find(m => m.getName == method && m.getParameterCount == expected.size)
        .map(_.getParameterTypes.toList)
        .getOrElse(fail(s"missing method $method on ${owner.getName}"))

    assertEquals(actual, expected)

  private def scalaSources(root: Path): List[Path] =
    Files.walk(root).iterator().asScala.filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala")).toList

  private def assertTokenUsers(token: String, expectedFiles: List[String]): Unit =
    val actual =
      scalaSources(strategicObjectDir).flatMap { path =>
        Option.when(read(path).contains(token))(path.getFileName.toString)
      }
    assertEquals(actual.sorted, expectedFiles.sorted, clue(s"unexpected users for token: $token"))

  private def read(path: Path): String =
    Files.readString(path)

  private def containsForbiddenToken(content: String, token: String): Boolean =
    val matcher =
      if token.forall(ch => ch.isLetterOrDigit || ch == '_' || ch == '.')
      then Pattern.compile(s"\\b${Pattern.quote(token)}\\b")
      else Pattern.compile(Pattern.quote(token))
    matcher.matcher(content).find()

  private def resolveRepoPath(relatives: Path*): Path =
    val cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath.normalize
    Iterator
      .iterate(Option(cwd))(_.flatMap(path => Option(path.getParent)))
      .takeWhile(_.nonEmpty)
      .flatten
      .flatMap { base =>
        relatives.iterator.map(base.resolve(_).normalize)
      }
      .find(Files.isDirectory(_))
      .getOrElse(throw new IllegalStateException(s"unable to resolve any of ${relatives.mkString(", ")} from $cwd"))
