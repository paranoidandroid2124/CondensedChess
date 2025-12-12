package chess
package db

import java.time.Instant
import java.util.UUID

// Maps to 'users' table
final case class DbUser(
  id: UUID,
  email: String,
  passwordHash: String,
  tier: String,
  createdAt: Instant
)

// Maps to 'games' table
final case class DbGame(
  id: UUID,
  userId: Option[UUID],
  whitePlayer: Option[String],
  blackPlayer: Option[String],
  result: Option[String],
  datePlayed: Option[Instant], // Simplified from Date for now
  eco: Option[String],
  pgnHeaders: String, // JSONB stored as String
  analysisS3Key: Option[String],
  createdAt: Instant
)

// Maps to 'analysis_jobs' table
final case class DbAnalysisJob(
  id: UUID,
  gameId: UUID,
  status: String,
  progress: Int,
  errorMessage: Option[String],
  createdAt: Instant,
  updatedAt: Instant
)
