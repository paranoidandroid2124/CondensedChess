package chess
package db

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import java.util.UUID

object UserRepo:
  def findById(id: UUID): ConnectionIO[Option[DbUser]] =
    sql"SELECT id, email, password_hash, tier, created_at FROM users WHERE id = $id"
      .query[DbUser]
      .option

  def findByEmail(email: String): ConnectionIO[Option[DbUser]] =
    sql"SELECT id, email, password_hash, tier, created_at FROM users WHERE email = $email"
      .query[DbUser]
      .option

  def create(email: String, hash: String, tier: String = "FREE"): ConnectionIO[DbUser] =
    sql"""
      INSERT INTO users (email, password_hash, tier)
      VALUES ($email, $hash, $tier)
      RETURNING id, email, password_hash, tier, created_at
    """.query[DbUser].unique

object GameRepo:
  def insert(g: DbGame): ConnectionIO[DbGame] =
    sql"""
      INSERT INTO games (id, user_id, white_player, black_player, result, date_played, eco, pgn_headers, analysis_s3_key, created_at)
      VALUES (${g.id}, ${g.userId}, ${g.whitePlayer}, ${g.blackPlayer}, ${g.result}, ${g.datePlayed}, ${g.eco}, ${g.pgnHeaders}::jsonb, ${g.analysisS3Key}, ${g.createdAt})
      RETURNING id, user_id, white_player, black_player, result, date_played, eco, pgn_headers, analysis_s3_key, created_at
    """.query[DbGame].unique

  def listByUser(userId: java.util.UUID, limit: Int): ConnectionIO[List[DbGame]] =
    sql"""
      SELECT id, user_id, white_player, black_player, result, date_played, eco, pgn_headers, analysis_s3_key, created_at
      FROM games
      WHERE user_id = $userId
      ORDER BY created_at DESC
      LIMIT $limit
    """.query[DbGame].to[List]

  def findById(id: UUID): ConnectionIO[Option[DbGame]] =
    sql"SELECT id, user_id, white_player, black_player, result, date_played, eco, pgn_headers, analysis_s3_key, created_at FROM games WHERE id = $id"
      .query[DbGame]
      .option
      
  def listByUser(userId: UUID): ConnectionIO[List[DbGame]] =
    sql"SELECT id, user_id, white_player, black_player, result, date_played, eco, pgn_headers, analysis_s3_key, created_at FROM games WHERE user_id = $userId ORDER BY created_at DESC"
      .query[DbGame]
      .to[List]

  def updateS3Key(id: UUID, key: String): ConnectionIO[Int] =
    sql"UPDATE games SET analysis_s3_key = $key WHERE id = $id".update.run

object JobRepo:
  def create(job: DbAnalysisJob): ConnectionIO[DbAnalysisJob] =
    sql"""
      INSERT INTO analysis_jobs (id, game_id, status, progress, error_message, created_at, updated_at)
      VALUES (${job.id}, ${job.gameId}, ${job.status}, ${job.progress}, ${job.errorMessage}, ${job.createdAt}, ${job.updatedAt})
      RETURNING id, game_id, status, progress, error_message, created_at, updated_at
    """.query[DbAnalysisJob].unique

  def updateStatus(id: UUID, status: String, progress: Int): ConnectionIO[Int] =
     sql"UPDATE analysis_jobs SET status = $status, progress = $progress, updated_at = NOW() WHERE id = $id".update.run

  def findById(id: UUID): ConnectionIO[Option[DbAnalysisJob]] =
    sql"SELECT id, game_id, status, progress, error_message, created_at, updated_at FROM analysis_jobs WHERE id = $id"
      .query[DbAnalysisJob]
      .option
