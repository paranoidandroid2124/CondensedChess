package chess
package db

import cats.effect.*
import java.nio.file.{Files, Paths, StandardOpenOption}

trait BlobStorage[F[_]]:
  def save(key: String, content: String): F[Unit]
  def load(key: String): F[Option[String]]

object BlobStorage:
  // Emulate S3 using local filesystem
  class FileBlobStorage(baseDir: String = "data") extends BlobStorage[IO]:
    
    // Ensure base dir exists
    private val root = Paths.get(baseDir)
    if !Files.exists(root) then Files.createDirectories(root)

    def save(key: String, content: String): IO[Unit] = IO.blocking {
      // Key "games/123-analysis.json" -> Path "data/games/123-analysis.json"
      val path = root.resolve(key)
      val parent = path.getParent
      if parent != null && !Files.exists(parent) then Files.createDirectories(parent)
      
      Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    def load(key: String): IO[Option[String]] = IO.blocking {
      val path = root.resolve(key)
      if Files.exists(path) then Some(Files.readString(path))
      else None
    }
