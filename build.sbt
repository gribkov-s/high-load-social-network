name := "high-load-social-network"

version := "0.1"

scalaVersion := "2.13.8"

val betterMonadicFor = "0.3.1"
val circe = "0.14.2"
val doobie = "0.13.4"
val flyway = "9.5.1"
val http4s = "0.21.26"
val jawn = "1.4.0"
val kindProjector = "0.13.2"
val organizeImports = "0.6.0"
val pureConfig = "0.17.1"
val zio = "1.0.16"
val zioInteropCats = "2.5.1.0"
val zioLogging = "0.5.14"
val logbackVersion = "1.2.3"


libraryDependencies ++= Seq(

  "com.github.pureconfig" %% "pureconfig" % pureConfig,
  //"org.scala-lang.modules" %% "scala-collection-contrib" % "0.2.2",

  "dev.zio" %% "zio-interop-cats" % zioInteropCats,
  "dev.zio" %% "zio-logging-slf4j" % zioLogging,
  "dev.zio" %% "zio-logging" % zioLogging,
  "dev.zio" %% "zio-test-sbt" % zio % "test",
  "dev.zio" %% "zio-test" % zio   % "test",
  "dev.zio" %% "zio" % zio,

  "io.circe" %% "circe-core" % circe,
  "io.circe" %% "circe-generic" % circe,
  "io.circe" %% "circe-parser" % circe,
  "io.circe" %% "circe-refined" % circe,
  "io.circe" %% "circe-generic-extras" % circe,
  "io.circe" %% "circe-literal" % circe % "test",

  "ch.qos.logback" % "logback-classic" % logbackVersion,

  "org.flywaydb" % "flyway-core" % flyway,
  "org.flywaydb" % "flyway-mysql" % flyway,

  "org.http4s" %% "http4s-blaze-server" % http4s,
  "org.http4s" %% "http4s-circe" % http4s,
  "org.http4s" %% "http4s-dsl" % http4s,

  "org.tpolecat" %% "doobie-core" % doobie,
  "org.tpolecat" %% "doobie-refined" % doobie,
  "org.tpolecat" %% "doobie-hikari" % doobie,

  "mysql" % "mysql-connector-java" % "8.0.17",

  "org.typelevel" %% "jawn-parser" % jawn  % "test",

  compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicFor),
  compilerPlugin(("org.typelevel" % "kind-projector" % kindProjector).cross(CrossVersion.full))
)

//val projectMainClass = "com.sgribkov.socialnetwork.Main"
//mainClass in (Compile, run) := Some(projectMainClass)

scalacOptions += "-Ymacro-annotations"

assemblyMergeStrategy in assembly := {
  case x if Assembly.isConfigFile(x) => MergeStrategy.concat
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
