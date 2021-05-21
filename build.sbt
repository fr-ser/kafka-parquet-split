organization := "com.example"
name := "parquet-split"
version := "1.0.0"

scalaVersion := "2.13.5"

lazy val kafkaVersion = "2.6.2"

lazy val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  // Kafka
  "org.apache.kafka" %% "kafka-streams-scala"     % kafkaVersion,
  "org.apache.kafka" % "kafka-streams-test-utils" % kafkaVersion % "test",
  // parquet parsing
  "com.github.mjakubowski84" %% "parquet4s-core" % "1.9.1",
  "org.apache.hadoop"        % "hadoop-client"   % "3.3.0",
  // json parsing
  "io.circe" %% "circe-core"    % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser"  % circeVersion,
  // functional utilities
  "org.scalaz" %% "scalaz-core" % "7.3.3",
  // config loading
  "com.github.pureconfig" %% "pureconfig" % "0.15.0",
  // logging
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2",
  "ch.qos.logback"             % "logback-classic" % "1.2.3",
  // Test
  "org.scalatest" %% "scalatest" % "3.2.3" % "test",
  "org.mockito" %% "mockito-scala" % "1.16.37" % "test",
)

testOptions in Test += Tests.Argument(
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
