package lila.commentary

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import scala.jdk.CollectionConverters.*

import munit.FunSuite
import play.api.libs.json.*
import lila.commentary.model.*

class RuntimeChronicleBoundaryTest extends FunSuite:

  test("runtime GameArc JSON omits raw strategic and authoring carriers") {
    val moment =
      GameArcMoment(
        ply = 12,
        momentType = "KeyMoment",
        narrative = "A compact diagnostic moment.",
        analysisData =
          ExtendedAnalysisData(
            fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
            nature = PositionNature(NatureType.Dynamic, 0.5, 0.5, "Dynamic position"),
            motifs = Nil,
            plans = Nil,
            preventedPlans = Nil,
            pieceActivity = Nil,
            structuralWeaknesses = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            prevMove = None,
            ply = 12,
            evalCp = 0,
            isWhiteToMove = true
          ),
        probeRequests = List(ProbeRequest("probe-1", "fen", List("e2e4"), 18)),
        authorQuestions =
          List(
            AuthorQuestionSummary(
              id = "q1",
              kind = "why_now",
              priority = 1,
              question = "Why now?",
              confidence = "medium"
            )
          ),
        authorEvidence =
          List(
            AuthorEvidenceSummary(
              questionId = "q1",
              questionKind = "why_now",
              question = "Why now?",
              status = "pending",
              pendingProbeCount = 1
            )
          ),
        mainStrategicPlans =
          List(
            lila.commentary.model.authoring.PlanHypothesis(
              planId = "plan-1",
              planName = "Plan",
              rank = 1,
              score = 0.7,
              preconditions = Nil,
              executionSteps = Nil,
              failureModes = Nil,
              viability = lila.commentary.model.authoring.PlanViability(0.7, "medium", "test")
            )
          )
      )
    val json = Json.toJson(moment).as[JsObject]
    val forbiddenKeys =
      Set(
        "strategyPack",
        "signalDigest",
        "probeRequests",
        "probeRefinementRequests",
        "authorQuestions",
        "authorEvidence",
        "mainStrategicPlans",
        "strategicPlanExperiments",
        "strategicBranch"
      )

    assertEquals(json.keys.intersect(forbiddenKeys), Set.empty[String], clue(json))
  }

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
