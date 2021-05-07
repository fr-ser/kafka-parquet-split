organization := "com.example"
name := "parquet-split"
version := "1.0.0"

scalaVersion := "2.13.5"
// For Settings/Task reference, see http://www.scala-sbt.org/release/sxr/sbt/Keys.scala.html

lazy val kafkaVersion = "2.6.2"

libraryDependencies ++= Seq(
  // Kafka
  "org.apache.kafka" %% "kafka-streams-scala" % kafkaVersion,
  "org.apache.kafka" % "kafka-streams-test-utils" % kafkaVersion % "test",
  // config loading
  "com.github.pureconfig" %% "pureconfig" % "0.15.0",
  // logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  // Test
  "org.scalatest" %% "scalatest" % "3.2.3" % "test",
)

testOptions in Test += Tests.Argument(
  TestFrameworks.ScalaCheck, "-maxSize", "5", "-minSuccessfulTests", "33",
  "-workers", s"${java.lang.Runtime.getRuntime.availableProcessors - 1}" ,
  "-verbosity", "1"
)