inThisBuild(
  Seq(
    scalaVersion := "3.7.4",
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
    "-release:21"
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
    "org.xerial" % "sqlite-jdbc" % "3.46.1.0"
  ),
  resolvers += "jitpack".at("https://jitpack.io")
)

lazy val root = project
  .in(file("."))
  .settings(publish := {}, publish / skip := true)
  .aggregate(scalachess)

addCommandAlias("prepare", "scalafixAll; scalafmtAll")
addCommandAlias("check", "; scalafixAll --check; scalafmtCheckAll")
