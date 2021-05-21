package com.example

import com.github.mjakubowski84.parquet4s.ParquetWriter
import com.typesafe.scalalogging.Logger
import io.circe.generic.auto._
import io.circe.parser.decode
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringDeserializer, StringSerializer}
import org.apache.kafka.streams.{KeyValue, StreamsConfig, TopologyTestDriver}
import org.mockito.MockitoSugar
import org.mockito.captor.ArgCaptor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.slf4j.{Logger => UnderlyingLogger}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import java.nio.file.{Files, Paths}
import java.util.Properties
import scala.jdk.CollectionConverters._
import scala.util.Random

class ParquetSplitTest extends AnyFlatSpec with Matchers with MockitoSugar {

  private val topologyConfig = {
    val props = new Properties()
    props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "testing")
    props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "testing:1234")
    props
  }
  private val appConfig = ConfigSource.default.loadOrThrow[AppConfig]

  behavior of "ParquetSplit"

  it should "publish a JSON message per row in the source Parquet" in {
    val tmpPath = Paths.get(Files.createTempDirectory("test").toString, "file.parquet").toString
    case class SimpleTestRow(id: Int, text: String)

    val testRows = (1 to 12).map { i =>
      SimpleTestRow(id = i, text = Random.nextString(4))
    }
    ParquetWriter.writeAndClose(tmpPath, testRows)

    val byteArray = Files.readAllBytes(Paths.get(tmpPath))

    val driver = new TopologyTestDriver(ParquetSplit().getTopology(appConfig), topologyConfig)

    try {
      val sourceTopic =
        driver
          .createInputTopic[String, Array[Byte]](appConfig.sourceTopic, new StringSerializer, new ByteArraySerializer)
      val outputTopic =
        driver.createOutputTopic(appConfig.outputTopic, new StringDeserializer, new StringDeserializer)

      sourceTopic.pipeInput("key", byteArray)

      outputTopic
        .readKeyValuesToList()
        .asScala
        .map(
          keyValue => new KeyValue(keyValue.key, decode[SimpleTestRow](keyValue.value))
        )
        .toList shouldBe testRows.map(value => new KeyValue("key", Right(value))).toList
    } finally {
      driver.close()
    }
  }

  class ParquetSplitMockable(mockedLogger: UnderlyingLogger) extends ParquetSplit {
    override lazy val logger: Logger = Logger(mockedLogger)
  }

  it should "filter out invalid Parquet files" in {
    val mocked = mock[UnderlyingLogger]
    when(mocked.isErrorEnabled()) thenReturn true
    val captor = ArgCaptor[String]

    val driver = new TopologyTestDriver(new ParquetSplitMockable(mocked).getTopology(appConfig), topologyConfig)

    try {
      val sourceTopic =
        driver
          .createInputTopic[String, Array[Byte]](appConfig.sourceTopic, new StringSerializer, new ByteArraySerializer)
      val outputTopic =
        driver.createOutputTopic(appConfig.outputTopic, new StringDeserializer, new StringDeserializer)

      sourceTopic.pipeInput("key", Array[Byte](12, 13, 14, 15))
      sourceTopic.pipeInput("key", Array[Byte](22, 23, 24, 25))

      outputTopic.isEmpty shouldBe true
      verify(mocked, times(2)).error(captor)
      captor.values shouldBe List(
        "Invalid message at offset 0",
        "Invalid message at offset 1"
      )
    } finally {
      driver.close()
    }
  }
}
