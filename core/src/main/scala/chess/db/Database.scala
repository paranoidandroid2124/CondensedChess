package chess
package db

import cats.effect.*
import doobie.*
import doobie.hikari.HikariTransactor
import chess.analysis.EnvLoader

object Database:
  def make(
    url: String = EnvLoader.getOrElse("DB_URL", "jdbc:postgresql://localhost:5432/chess"),
    user: String = EnvLoader.getOrElse("DB_USER", "chess"),
    pass: String = EnvLoader.getOrElse("DB_PASS", "password"),
    poolSize: Int = EnvLoader.get("DB_POOL_SIZE").flatMap(_.toIntOption).getOrElse(4)
  ): Resource[IO, HikariTransactor[IO]] =
    for
      ce <- ExecutionContexts.fixedThreadPool[IO](poolSize)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        url,
        user,
        pass,
        ce
      )
    yield xa
