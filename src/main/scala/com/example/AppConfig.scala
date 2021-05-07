package com.example

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.StreamsConfig

import java.util.Properties

case class AppConfig(
  maybeBootstrapServers: Option[String],
  maybeSourceTopic: Option[String],
  maybeOutputTopic: Option[String],
  commitIntervalMs: Option[Long],
  cacheMaxBytesBuffering: Option[Long]) {
  val bootstrapServers = maybeBootstrapServers.getOrElse("localhost:9092,localhost:9093")
  val sourceTopic      = maybeSourceTopic.getOrElse("etl.readings.batch")
  val outputTopic      = maybeOutputTopic.getOrElse("etl.readings")

  private val applicationName     = "example.batch-split"
  private val processingGuarantee = "at_least_once"
  private val autoOffsetReset     = "latest"

  def kafkaStreamsConfig: Properties = {
    val config = new Properties()
    config.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, applicationName)
    config.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    config.setProperty(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, processingGuarantee)
    config.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset)
    commitIntervalMs match {
      case None => ()
      case Some(value) =>
        config.setProperty(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, value.toString)
    }
    commitIntervalMs match {
      case None => ()
      case Some(value) =>
        config.setProperty(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, value.toString)
    }
    config
  }
}
