package com.example

import org.apache.kafka.common.serialization.{
  ByteArrayDeserializer,
  ByteArraySerializer,
  StringDeserializer,
  StringSerializer
}
import org.apache.kafka.streams.{StreamsConfig, TopologyTestDriver}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import java.util.Properties

class ParquetSplitTest extends AnyFlatSpec with Matchers {

  private val topologyConfig = {
    val props = new Properties()
    props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "testing")
    props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "testing:1234")
    props
  }
  private val appConfig = ConfigSource.default.loadOrThrow[AppConfig]

  "The ParquetSplit" should "split and pipe through individual messages" in {
    val driver = new TopologyTestDriver(ParquetSplit.getTopology(appConfig), topologyConfig)

    try {
      val sourceTopic =
        driver
          .createInputTopic[String, Array[Byte]](appConfig.sourceTopic, new StringSerializer, new ByteArraySerializer)
      val outputTopic =
        driver.createOutputTopic(appConfig.outputTopic, new StringDeserializer, new ByteArrayDeserializer)

      sourceTopic.pipeInput("key", Array[Byte](10, -32, 17, 22))

      val record = outputTopic.readRecord()

      record.key() shouldBe "key"
      record.value() shouldBe Array[Byte](10, -32, 17, 22)

    } finally {
      driver.close()
    }
  }
}
