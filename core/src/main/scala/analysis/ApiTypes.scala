package chess
package analysis

import ujson.Value

object ApiTypes:

  enum Language:
    case English, Korean
  
  object Language:
    def fromString(s: String): Language = s.toLowerCase match
      case "ko" | "korean" => Korean
      case _ => English

  enum DepthProfile:
    case Fast, Deep, Tournament
  
  object DepthProfile:
    def fromString(s: String): DepthProfile = s.toLowerCase match
      case "deep" => Deep
      case "tournament" => Tournament
      case _ => Fast

  case class ReviewOptions(
    language: Language = Language.English,
    depthProfile: DepthProfile = DepthProfile.Fast,
    maxExperimentsPerPly: Option[Int] = None,
    forceCriticalPlys: Set[Int] = Set.empty
  )

  case class ReviewRequest(
    pgn: String,
    gameId: Option[String] = None,
    options: Option[ReviewOptions] = None
  )

  case class ChapterReviewResponse(
    jobId: String,
    status: String,
    result: Option[Value] = None
  )

  final val SCHEMA_VERSION = 3

  case class ApiError(
      code: String,
      message: String,
      details: Option[String] = None
  )
