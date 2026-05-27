package lila.commentary

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import scala.jdk.CollectionConverters.*

import munit.FunSuite

class RuntimeChronicleBoundaryTest extends FunSuite:

  test("runtime commentary sources do not expose Chronicle response or moment models") {
    val mainRoots =
      List(
        Paths.get("modules/commentaryCore/src/main"),
        Paths.get("modules/commentary/src/main"),
        Paths.get("app/controllers"),
        Paths.get("ui/analyse/src")
      )

    val forbidden =
      List(
        "GameChronicleResponse",
        "GameChronicleMoment",
        "ActivePlanRef",
        "activeStrategicNote",
        "activeStrategicSourceMode",
        "activeStrategicIdeas",
        "activeStrategicRoutes",
        "activeStrategicMoves",
        "activeDirectionalTargets",
        "activeBranchDossier",
        "strategicThreads",
        "ActiveBranchDossier",
        "ActiveStrategicThread",
        "ActiveStrategicThreadRef",
        "activeNoteMoment",
        "activeNotePlies",
        "strategicThreadId"
      )

    val hits =
      mainRoots
        .filter(Files.exists(_))
        .flatMap(sourceFiles)
        .flatMap { path =>
          val text = Files.readString(path, StandardCharsets.UTF_8)
          forbidden.collect {
            case token if text.contains(token) => s"$path contains $token"
          }
        }

    assertEquals(hits, Nil)
  }

  private def sourceFiles(root: Path): List[Path] =
    Files
      .walk(root)
      .iterator()
      .asScala
      .filter(path => Files.isRegularFile(path) && isSource(path))
      .toList

  private def isSource(path: Path): Boolean =
    val name = path.toString
    name.endsWith(".scala") || name.endsWith(".ts") || name.endsWith(".tsx")
