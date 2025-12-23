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
  pgnText: String,      // Job payload for DB-only queue
  optionsJson: String,  // JSONB as String
  createdAt: Instant,
  updatedAt: Instant
)

// Maps to 'blobs' table
final case class DbBlob(
  key: String,
  content: String,
  createdAt: Instant
)
