package com.example

import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.StreamsBuilder

object ParquetSplit {
  def getTopology(appConfig: AppConfig): Topology = {
    val builder = new StreamsBuilder()
    val sourceStream = builder.stream[String, Array[Byte]](appConfig.sourceTopic)

    sourceStream.to(appConfig.outputTopic)
    builder.build()
  }
}
