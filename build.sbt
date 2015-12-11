import scalariform.formatter.preferences._

name := "CmpDir"

version := "1.0.0"

scalaVersion := "2.11.7"

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(PreserveDanglingCloseParenthesis, true)

resolvers ++=
  Seq("Akka Repository" at "http://repo.akka.io/releases/",
    "Typesafe Snapshots" at "http://repo.akka.io/snapshots/",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "maven repo" at "http://central.maven.org/maven2"
  )

libraryDependencies ++= {
  val akkaVersion = "2.3.12"
  val akkaStreamVersion = "1.0"
  val scalaTestVersion = "2.2.4"
  Seq(
    "ch.qos.logback"              % "logback-classic"     % "1.1.3",
    "com.typesafe.scala-logging"  %% "scala-logging"      % "3.1.0",
    "com.typesafe.akka"           % "akka-actor_2.11"     % akkaVersion,
    "com.typesafe"                % "config"              % "1.3.0",
    "com.typesafe.akka"           %% "akka-actor"         % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"         % akkaVersion,
    "com.typesafe.akka"           %% "akka-testkit"       % akkaVersion % "test",
    "org.scalatest"               %% "scalatest"          % scalaTestVersion % "test",
    "joda-time"                   % "joda-time"           % "2.9.1"
  )
}


