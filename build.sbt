inThisBuild(
  Seq(
    scalaVersion := "3.7.0",
    version := "17.14.1",
    organization := "com.github.lichess-org.scalachess",
    licenses += ("MIT" -> url("https://opensource.org/licenses/MIT")),
    publishTo := Option(Resolver.file("file", new File(sys.props.getOrElse("publishTo", "")))),
    semanticdbEnabled := true, // for scalafix
    Compile / packageDoc / publishArtifact := false
  )
)

val scalalibVersion = "11.9.5"

val commonSettings = Seq(
  scalacOptions := Seq(
    "-encoding",
    "utf-8",
    "-rewrite",
    // "-indent",
    "-feature",
    "-language:postfixOps",
    "-Wunused:all",
    "-release:17"
    // "-Werror"
    // Warnings as errors!
    /* "-Xfatal-warnings" */
  )
)

lazy val scalachess: Project = Project("scalachess", file("core")).settings(
  commonSettings,
  name := "scalachess",
  libraryDependencies ++= List(
    "com.github.lichess-org.scalalib" %% "scalalib-core" % scalalibVersion,
    "com.github.lichess-org.scalalib" %% "scalalib-model" % scalalibVersion,
    "com.lihaoyi" %% "ujson" % "4.1.0",
    "org.typelevel" %% "cats-core" % "2.13.0",
    "org.typelevel" %% "alleycats-core" % "2.13.0",
    "org.typelevel" %% "cats-parse" % "1.1.0",
    "dev.optics" %% "monocle-core" % "3.3.0",
    "org.typelevel" %% "kittens" % "3.5.0",
    "org.xerial" % "sqlite-jdbc" % "3.46.1.0",
    "ch.qos.logback" % "logback-classic" % "1.4.14",
    "org.typelevel" %% "cats-effect" % "3.5.4",
    "org.http4s" %% "http4s-ember-server" % "0.23.27",
    "org.http4s" %% "http4s-dsl" % "0.23.27",
    "org.http4s" %% "http4s-circe" % "0.23.27",
    "org.scalameta" %% "munit" % "1.0.0" % Test,
    // Database (Doobie)
    "org.tpolecat" %% "doobie-core" % "1.0.0-RC5",
    "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC5",
    "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC5",
    // Redis (Job Queue)
    "dev.profunktor" %% "redis4cats-effects" % "1.5.2",
    "dev.profunktor" %% "redis4cats-streams" % "1.5.2",
    // Authentication (Tsec + JBCrypt)
    "org.mindrot" % "jbcrypt" % "0.4",
    "io.github.jmcardon" %% "tsec-jwt-mac" % "0.5.0",
    "com.google.cloud.sql" % "postgres-socket-factory" % "1.15.1"
  ),
  resolvers += "jitpack".at("https://jitpack.io"),
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", "versions", _*) => MergeStrategy.discard
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
    case x if x.endsWith("module-info.class") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val root = project
  .in(file("."))
  .settings(publish := {}, publish / skip := true)
  .aggregate(scalachess)

addCommandAlias("prepare", "scalafixAll; scalafmtAll")
addCommandAlias("check", "; scalafixAll --check; scalafmtCheckAll")
