package chess
package analysis

import cats.effect.*
import doobie.*
import doobie.implicits.*
import java.util.UUID
import java.time.Instant
import chess.db.{GameRepo, DbGame, BlobStorage}

class AnalysisService(xa: Transactor[IO], blob: BlobStorage[IO]):

  private val TagRegex = """\[(\w+) "([^"]+)"\]""".r

  private def extractTag(pgn: String, name: String): Option[String] =
    TagRegex.findAllMatchIn(pgn)
      .collectFirst { case m if m.group(1) == name => m.group(2) }

  def save(id: String, output: AnalyzePgn.Output, userId: Option[UUID]): IO[Unit] =
    val pgn = output.pgn
    val uuid = UUID.fromString(id)
    val now = Instant.now()
    
    // 1. Prepare Metadata
    val game = DbGame(
      id = uuid,
      userId = userId, // Anonymous for now
      whitePlayer = extractTag(pgn, "White"),
      blackPlayer = extractTag(pgn, "Black"),
      result = extractTag(pgn, "Result"),
      datePlayed = None, // Parse Date later if needed
      eco = output.opening.map(_.opening.eco.value),
      pgnHeaders = "{}", // Use proper JSON lib to extract all tags if needed
      analysisS3Key = Some(s"games/$id/analysis.json"),
      createdAt = now
    )

    // 2. Render JSON
    val json = AnalyzePgn.render(output)

    // 3. Save to Blob Storage (Simulated S3)
    // We use the same key pattern as DB
    val saveBlob = blob.save(game.analysisS3Key.get, json)

    // 4. Save to DB target
    // We update the existing game record created by JobManager with the S3 key
    val saveDb = GameRepo.updateS3Key(uuid, game.analysisS3Key.get).transact(xa)

    for
      _ <- saveBlob
      _ <- saveDb
    yield ()

  /** Loads analysis JSON. Uses ID to look up S3 Key (if we strictly followed the new schema)
    * or just assumes standard path for now given `Persistence` legacy.
    * To be fully correct: DB Lookup -> Get Key -> Load Blob.
    */ 
  def load(id: String): IO[Option[String]] =
    val uuid = scala.util.Try(UUID.fromString(id)).toOption
    
    val loadFromDb = uuid match
      case Some(uid) =>
        GameRepo.findById(uid).transact(xa).flatMap {
          case Some(game) => 
            game.analysisS3Key match
              case Some(key) => blob.load(key)
              case None => IO.pure(None)
          case None => 
            // Fallback for legacy files not in DB?
            // "games/{id}/analysis.json"
            blob.load(s"games/$id/analysis.json") 
        }
      case None => IO.pure(None)

    // Fallback: Check standard path directly if DB lookup failed or returned nothing
    // This supports the transition where some files might exist without DB entries?
    // actually let's just try direct path if Db path returns None
    loadFromDb.flatMap {
      case Some(json) => IO.pure(Some(json))
      case None => blob.load(s"games/$id/analysis.json") // Try direct key
    }

object AnalysisService:
  // Factory for convenience if needed, but direct class usage is fine
  def make(xa: Transactor[IO], blob: BlobStorage[IO]): AnalysisService = new AnalysisService(xa, blob)
