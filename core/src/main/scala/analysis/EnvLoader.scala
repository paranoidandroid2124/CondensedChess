package chess.analysis

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.*

object EnvLoader:
  private lazy val fileEnv: Map[String, String] =
    val path = Paths.get(".env")
    if Files.exists(path) then
      Files.readAllLines(path).asScala
        .map(_.trim)
        .filterNot(l => l.isEmpty || l.startsWith("#"))
        .map { line =>
          val parts = line.split("=", 2)
          if parts.length == 2 then Some(parts(0).trim -> parts(1).trim) else None
        }
        .flatten
        .toMap
    else
      Map.empty

  def get(key: String): Option[String] =
    sys.env.get(key).orElse(fileEnv.get(key))

  def getOrElse(key: String, default: String): String =
    get(key).getOrElse(default)
