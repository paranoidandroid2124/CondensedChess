package lila.strategicPuzzle

import com.softwaremill.macwire.*

import lila.core.config.*

@Module
final class Env(
    mongo: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor):

  private val puzzleColl = mongo.mainDb(CollName("strategicPuzzle"))
  private val attemptColl = mongo.mainDb(CollName("strategicPuzzleAttempt"))
  private val progressColl = mongo.mainDb(CollName("strategicPuzzleProgress"))

  lazy val repo = PuzzleRepo(puzzleColl, mongoCache)
  lazy val attemptRepo = AttemptRepo(attemptColl)
  lazy val progressRepo = ProgressRepo(progressColl)
  lazy val selector = wire[PuzzleSelector]
  lazy val api = wire[StrategicPuzzleApi]
