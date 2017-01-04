enablePlugins(JavaServerAppPackaging)

name := "akka-http-batch-api"
organization := "org.yashsriv"
version := "1.0"
scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature")

resolvers ++= Seq(
  "bintray/vikraman" at "https://dl.bintray.com/vikraman/maven"
)

libraryDependencies ++= {
  val akkaV         = "2.4.16"
  val akkaArgonautV = "0.6.0"
  val akkaHttpV     = "10.0.1"
  val argonautV     = "6.2-RC2"
  val scalaTestV    = "3.0.1"
  Seq(
    "com.typesafe.akka" %% "akka-actor"        % akkaV,
    "com.typesafe.akka" %% "akka-stream"       % akkaV,
    "com.typesafe.akka" %% "akka-testkit"      % akkaV,
    "com.typesafe.akka" %% "akka-http"         % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV,
    "io.argonaut"       %% "argonaut"          % argonautV,
    "org.scalatest"     %% "scalatest"         % scalaTestV % "test",
    "org.vikraman"      %% "akka-argonaut"     % akkaArgonautV
  )
}

Revolver.settings
