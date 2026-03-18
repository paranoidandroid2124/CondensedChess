package lila.strategicPuzzle

import com.softwaremill.macwire.*

import lila.core.config.*

@Module
final class Env(
    mongo: lila.db.Env
)(using Executor):

  private val puzzleColl = mongo.mainDb(CollName("strategicPuzzle"))
  private val attemptColl = mongo.mainDb(CollName("strategicPuzzleAttempt"))

  lazy val repo = PuzzleRepo(puzzleColl)
  lazy val progressRepo = ProgressRepo(attemptColl)
  lazy val selector = wire[PuzzleSelector]
  lazy val api = wire[StrategicPuzzleApi]
