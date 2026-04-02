package lila.strategicPuzzle

import lila.core.userId.UserId
import lila.strategicPuzzle.StrategicPuzzle.*

final class StrategicPuzzleApi(
    repo: PuzzleRepo,
    selector: PuzzleSelector,
    progressRepo: ProgressRepo
)(using Executor):

  def bootstrapById(id: String, userId: Option[UserId]): Fu[Option[BootstrapPayload]] =
    repo.byId(id).flatMap {
      case Some(puzzle) => bootstrapForPuzzle(puzzle, userId).map(Some(_))
      case None         => fuccess(None)
    }

  def nextBootstrap(userId: Option[UserId], excludeId: Option[String]): Fu[Option[BootstrapPayload]] =
    nextPuzzle(userId, excludeId).flatMap {
      case Some(puzzle) => bootstrapForPuzzle(puzzle, userId).map(Some(_))
      case None         => fuccess(None)
    }

  def complete(
      id: String,
      userId: Option[UserId],
      req: CompleteRequest
  ): Fu[CompleteOutcome] =
    repo.byId(id).flatMap:
      case None =>
        fuccess(CompleteOutcome.MissingPuzzle)
      case Some(puzzle) =>
        runtimeShellFor(puzzle).flatMap(resolveCompletion(_, req)) match
          case None => fuccess(CompleteOutcome.Invalid)
          case Some(resolved) =>
            val saveFu =
              userId match
                case Some(uid) =>
                  progressRepo.hasCleared(uid, puzzle.id).flatMap { alreadyCleared =>
                    val insertFu =
                      if alreadyCleared then funit
                      else
                        val attempt = newAttempt(
                          uid,
                          puzzle.id,
                          resolved.status,
                          resolved.lineUcis,
                          resolved.planId,
                          resolved.startUci,
                          resolved.terminalId,
                          resolved.terminalFamilyKey
                        )
                        progressRepo.insert(attempt)
                    insertFu >> progressRepo.currentStreak(uid).map(streak => true -> streak)
                  }
                case None =>
                  fuccess(false -> 0)

            saveFu.flatMap { case (saved, streak) =>
              nextBootstrap(userId, Some(id)).map { next =>
                CompleteOutcome.Success(
                  CompleteResponse(
                    saved = saved,
                    currentStreak = streak,
                    nextPuzzleId = next.map(_.puzzle.id),
                    nextPuzzleUrl = next.map(nb => s"/strategic-puzzle/${nb.puzzle.id}")
                  ),
                  next
                )
              }
            }

  private def nextPuzzle(userId: Option[UserId], excludeId: Option[String]): Fu[Option[StrategicPuzzleDoc]] =
    userId match
      case Some(uid) => selector.nextFor(uid, excludeId)
      case None      => selector.nextAnonymous(excludeId)

  private def bootstrapForPuzzle(
      puzzle: StrategicPuzzleDoc,
      userId: Option[UserId]
  ): Fu[BootstrapPayload] =
    val shell = runtimeShellFor(puzzle).get
    val publicPuzzle = puzzle.copy(runtimeShell = Some(shell))
    userId match
      case Some(uid) =>
        for
          streak <- progressRepo.currentStreak(uid)
          recent <- progressRepo.recent(uid)
        yield BootstrapPayload(
          puzzle = publicPuzzle,
          runtimeShell = shell,
          progress = progress(authenticated = true, streak = streak, recent = recent)
        )
      case None =>
        fuccess(
          BootstrapPayload(
          puzzle = publicPuzzle,
          runtimeShell = shell,
          progress = progress(authenticated = false, streak = 0, recent = Nil)
          )
        )

  private def runtimeShellFor(puzzle: StrategicPuzzleDoc): Option[RuntimeShell] =
    puzzle.runtimeShell.map(_.materialize(puzzle.dominantFamily))
