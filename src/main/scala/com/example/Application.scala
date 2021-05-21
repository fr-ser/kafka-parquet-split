package com.example

import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KafkaStreams.{State, StateListener}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import java.time.Duration

object Application extends App with LazyLogging {
  val appConfig = ConfigSource.default.loadOrThrow[AppConfig]

  val topology = ParquetSplit().getTopology(appConfig)

  logger.info(topology.describe.toString)

  val streams = new KafkaStreams(topology, appConfig.kafkaStreamsConfig)
  streams.setStateListener(AppStateListener)
  streams.start()

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(10))
    ()
  }

  object AppStateListener extends StateListener with LazyLogging {
    def onChange(newState: State, oldState: State): Unit = {
      logger.info(s"Current state is: $newState (prev: $oldState)")
    }
  }
}
