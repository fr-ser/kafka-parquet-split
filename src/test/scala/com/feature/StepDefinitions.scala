package com.feature

import com.example.AppConfig
import com.github.mjakubowski84.parquet4s._
import io.cucumber.scala.{EN, ScalaDsl}
import io.circe.parser._
import io.cucumber.datatable.DataTable
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.{BINARY, BOOLEAN, DOUBLE, INT64}
import org.apache.parquet.schema.Type.Repetition.REQUIRED
import org.apache.parquet.schema.{LogicalTypeAnnotation, MessageType, Types}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import java.nio.file.{Files, Paths}
import java.time.Duration
import java.util.Collections
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class StepDefinitions extends ScalaDsl with EN {
  private val appConfig = ConfigSource.default.loadOrThrow[AppConfig]

  private val consumer = KafkaHelper.getConsumer("etl.readings")
  private val producer = KafkaHelper.getProducer

  /**
    * @return isAssigned: Boolean flag of whether consumer assignment is empty or not after the deadline
    */
  def subscribeConsumerHelper(consumer: KafkaConsumer[String, String], topic: String): Boolean = {
    consumer.subscribe(Collections.singletonList(topic))

    val deadline = 20.seconds.fromNow
    while (deadline.hasTimeLeft() && consumer.assignment.isEmpty) {
      consumer.poll(Duration.ofMillis(100))
    }
    !consumer.assignment().isEmpty
  }

  After.apply(_ => {
    producer.close()
    consumer.close()
  })

  Given("""we listen on the output topic""") { () =>
    val isAssigned = subscribeConsumerHelper(consumer, appConfig.outputTopic)
    if (!isAssigned) {
      throw new AssertionError(s"Did not get an assignment within 20 seconds for ${appConfig.outputTopic}")
    }
  }
  Given("""we publish the following messages as a parquet file to the input topic""") { (dataTable: DataTable) =>
    def matcher(
      map: Map[String, String],
      lambdaS: (String, String) => Unit,
      lambdaB: (String, Boolean) => Unit,
      lambdaL: (String, Long) => Unit,
      lambdaD: (String, Double) => Unit
    ): Unit = {
      map.foreach {
        case (key, value) =>
          parse(value) match {
            case Left(msg) =>
              throw new IllegalArgumentException(s"Could not parse value for $key as JSON: $value - $msg")
            case Right(parsedValue) if parsedValue.isString  => lambdaS(key, parsedValue.asString.get)
            case Right(parsedValue) if parsedValue.isBoolean => lambdaB(key, parsedValue.asBoolean.get)
            case Right(parsedValue) if parsedValue.isNumber =>
              parsedValue.asNumber.flatMap(_.toLong) match { // TODO: its only a makeshift test and does not work always
                case Some(_) => lambdaL(key, parsedValue.asNumber.flatMap(_.toLong).get)
                case None    => lambdaD(key, parsedValue.asNumber.get.toDouble)
              }
            case _ => throw new IllegalArgumentException(s"Unknown data type for $key: $value")
          }
      }
    }

    val path = Files.createTempDirectory("cucumber-test").toString

    val rows = dataTable.asMaps().asScala.map { rawTableRow =>
      {
        val parquetRow = RowParquetRecord.empty
        matcher(
          rawTableRow.asScala.toMap,
          (key, value) => {
            parquetRow.add(key, BinaryValue(value.getBytes))
            ()
          },
          (key, value) => {
            parquetRow.add(key, BooleanValue(value))
            ()
          },
          (key, value) => {
            parquetRow.add(key, LongValue(value))
            ()
          },
          (key, value) => {
            parquetRow.add(key, DoubleValue(value))
            ()
          }
        )
        parquetRow
      }
    }
    val schemaBuilder = Types.buildMessage()
    dataTable.asMaps().asScala.take(1).foreach { rawTableRow =>
      {
        matcher(
          rawTableRow.asScala.toMap,
          (key, _) => {
            schemaBuilder.addField(Types.primitive(BINARY, REQUIRED).as(LogicalTypeAnnotation.stringType()).named(key))
            ()
          },
          (key, _) => {
            schemaBuilder.addField(Types.primitive(BOOLEAN, REQUIRED).named(key))
            ()
          },
          (key, _) => {
            schemaBuilder
              .addField(Types.primitive(INT64, REQUIRED).as(LogicalTypeAnnotation.intType(64, true)).named(key))
            ()
          },
          (key, _) => {
            schemaBuilder.addField(Types.primitive(DOUBLE, REQUIRED).named(key))
            ()
          }
        )
      }
    }
    implicit val rowSchema: MessageType = schemaBuilder.named("GenericSchema")
    ParquetWriter.writeAndClose(s"$path/rows.parquet", rows)
    val byteArray = Files.readAllBytes(Paths.get(s"$path/rows.parquet"))

    producer.send(new ProducerRecord[String, Array[Byte]](appConfig.sourceTopic, "key", byteArray))
    producer.flush()
  }
  Then("""we expect to find the following json messages in the output topic""") { (dataTable: DataTable) =>
    val rows = dataTable.asMaps().iterator().asScala.toList
    KafkaHelper.getMessages(consumer, rows.size) match {
      case Left(value) => throw new AssertionError(value)
      case Right(messages) =>
        if (messages.size != rows.size) {
          throw new AssertionError(
            s"Wanted ${rows.size} messages but got ${messages.size}"
          )
        } else {
          for ((row, received) <- rows zip messages) {
            parse(row.get("value")) shouldBe parse(received.value())
          }
        }
    }
  }
}
