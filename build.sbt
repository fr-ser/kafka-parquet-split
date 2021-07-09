organization := "com.example"
name := "parquet-split"
version := "1.0.0"

scalaVersion := "2.13.6"

enablePlugins(CucumberPlugin)

CucumberPlugin.monochrome := false
CucumberPlugin.glues := List("com.feature")
CucumberPlugin.mainClass := "io.cucumber.core.cli.Main"

lazy val kafkaVersion = "2.6.2"
lazy val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  // Kafka
  "org.apache.kafka" %% "kafka-streams-scala"     % kafkaVersion,
  "org.apache.kafka" % "kafka-streams-test-utils" % kafkaVersion % "test",
  // parquet parsing
  "com.github.mjakubowski84" %% "parquet4s-core" % "1.9.2",
  "org.apache.hadoop"        % "hadoop-client"   % "3.3.1",
  // json parsing
  "io.circe" %% "circe-core"    % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser"  % circeVersion,
  // functional utilities
  "org.scalaz" %% "scalaz-core" % "7.3.4",
  // config loading
  "com.github.pureconfig" %% "pureconfig" % "0.15.0",
  // logging
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.4",
  "ch.qos.logback"             % "logback-classic" % "1.2.3",
  // Test
  "org.scalatest" %% "scalatest"      % "3.2.9"   % "test",
  "org.mockito"   %% "mockito-scala"  % "1.16.37" % "test",
  "io.cucumber"   %% "cucumber-scala" % "6.10.4"   % "test",
)

assembly / mainClass := Some("com.example.Application")
assembly / assemblyMergeStrategy  := {
  case PathList("META-INF", "services", "org.apache.hadoop.fs.FileSystem") => MergeStrategy.filterDistinctLines
  case PathList("META-INF", _ @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
assembly / assemblyOutputPath  := file("target/application.jar")

Test / testOptions += Tests.Argument(
  TestFrameworks.ScalaCheck,
  "-maxSize",
  "5",
  "-minSuccessfulTests",
  "33",
  "-workers",
  s"${java.lang.Runtime.getRuntime.availableProcessors - 1}",
  "-verbosity",
  "1"
)
