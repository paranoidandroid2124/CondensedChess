package lila.strategicPuzzle

import lila.core.userId.UserId
import lila.strategicPuzzle.StrategicPuzzle.*

final class StrategicPuzzleApi(
    repo: PuzzleRepo,
    selector: PuzzleSelector,
    attemptRepo: AttemptRepo,
    progressRepo: ProgressRepo
)(using Executor):

  private val logger = lila.log("strategicPuzzle")

  def bootstrapById(id: String, userId: Option[UserId]): Fu[Option[BootstrapPayload]] =
    repo.byId(id).flatMap:
      case Some(puzzle) =>
        userId match
          case Some(uid) =>
            loadProgress(uid).flatMap(progress => bootstrapForPuzzle(puzzle, progress.some).map(Some(_)))
          case None =>
            bootstrapForPuzzle(puzzle, none[ProgressDoc]).map(Some(_))
      case None => fuccess(None)

  def nextBootstrap(userId: Option[UserId], excludeId: Option[String]): Fu[Option[BootstrapPayload]] =
    userId match
      case Some(uid) =>
        loadProgress(uid).flatMap { progress =>
          selector.nextFor(progress, excludeId).flatMap:
            case Some(puzzle) => bootstrapForPuzzle(puzzle, progress.some).map(Some(_))
            case None         => fuccess(None)
        }
      case None =>
        selector.nextAnonymous(excludeId).flatMap:
          case Some(puzzle) => bootstrapForPuzzle(puzzle, none[ProgressDoc]).map(Some(_))
          case None         => fuccess(None)

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
            val progressFu =
              userId match
                case Some(uid) =>
                  loadProgress(uid).flatMap { progress =>
                    val alreadyCleared = progress.clearedPuzzleIds.contains(puzzle.id)
                    if alreadyCleared then fuccess(false -> progress)
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
                      val nextProgress = applyAttempt(progress, attempt)
                      attemptRepo.insert(attempt).flatMap { inserted =>
                        persistProgress(uid, nextProgress).map(_ => inserted -> nextProgress)
                      }
                  }
                case None =>
                  fuccess(false -> emptyProgress)

            progressFu.flatMap { case (saved, progress) =>
              val nextProgress = userId.fold(none[ProgressDoc])(_ => progress.some)
              nextPuzzleId(userId, nextProgress, Some(id)).map { nextId =>
                CompleteOutcome.Success(
                  CompleteResponse(
                    saved = saved,
                    currentStreak = progress.currentStreak,
                    nextPuzzleId = nextId
                  )
                )
              }
            }

  private def nextPuzzleId(
      userId: Option[UserId],
      progress: Option[ProgressDoc],
      excludeId: Option[String]
  ): Fu[Option[String]] =
    userId match
      case Some(_) => progress.fold(fuccess(none[String]))(selector.nextIdFor(_, excludeId))
      case None    => selector.nextAnonymousId(excludeId)

  private def bootstrapForPuzzle(
      puzzle: StrategicPuzzleDoc,
      progressDoc: Option[ProgressDoc]
  ): Fu[BootstrapPayload] =
    val shell = runtimeShellFor(puzzle).get
    val publicPuzzle = puzzle.copy(runtimeShell = None)
    progressDoc match
      case Some(progress) =>
        fuccess(
          BootstrapPayload(
            puzzle = publicPuzzle,
            runtimeShell = shell,
            progress = progressPayload(authenticated = true, progress = progress)
          )
        )
      case None =>
        fuccess(
          BootstrapPayload(
            puzzle = publicPuzzle,
            runtimeShell = shell,
            progress = ProgressPayload(authenticated = false, currentStreak = 0, recentAttempts = Nil)
          )
        )

  private def loadProgress(userId: UserId): Fu[ProgressDoc] =
    progressRepo.byUser(userId).flatMap:
      case Some(progress) => fuccess(progress)
      case None =>
        attemptRepo.all(userId).flatMap { attempts =>
          val rebuilt = progressFromAttempts(userId, attempts)
          persistProgress(userId, rebuilt).map(_ => rebuilt)
        }

  private def persistProgress(userId: UserId, progress: ProgressDoc): Fu[ProgressDoc] =
    progressRepo
      .upsert(progress.copy(_id = userId.value))
      .map(_ => progress.copy(_id = userId.value))
      .recover { case err =>
        logger.warn(s"strategic puzzle progress upsert failed for ${userId.value}: ${err.getMessage}")
        progress.copy(_id = userId.value)
      }

  private def runtimeShellFor(puzzle: StrategicPuzzleDoc): Option[RuntimeShell] =
    puzzle.runtimeShell.map(_.materialize(puzzle.dominantFamily))

  private def progressPayload(authenticated: Boolean, progress: ProgressDoc): ProgressPayload =
    ProgressPayload(
      authenticated = authenticated,
      currentStreak = progress.currentStreak,
      recentAttempts = progress.latestAttemptsByPuzzle.take(12)
    )

  private val emptyProgress =
    ProgressDoc(
      _id = "",
      currentStreak = 0,
      latestAttemptsByPuzzle = Nil,
      clearedPuzzleIds = Nil,
      updatedAt = currentIsoInstant()
    )
