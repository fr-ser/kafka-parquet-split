package com.feature

import com.example.AppConfig
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer, ByteArraySerializer}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import java.time.{Duration, Instant}
import java.util.Properties
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object KafkaHelper {
  // TODO: All Serdes are Strings until I can figure out how to use the implicit Serdes
  // https://github.com/azhur/kafka-serde-scala/issues/220

  private val appConfig = ConfigSource.default.loadOrThrow[AppConfig]

  def getProducer: KafkaProducer[String, Array[Byte]] = {
    val props = new Properties
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.bootstrapServers)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer])
    new KafkaProducer(props)
  }

  /**
    * creates a new consumer and waits for it to be assigned before it returns
    *
    * @return consumer assigned to the destination topic
    */
  def getConsumer(groupPrefix: String): KafkaConsumer[String, String] = {
    val props = new Properties
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.bootstrapServers)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    props.put(ConsumerConfig.GROUP_ID_CONFIG, s"integration-test-consumer-$groupPrefix-${Instant.now.getEpochSecond}")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    new KafkaConsumer(props)

  }


  /**
    * This method tries to get at least the expectedMessageCount of messages before it returns.
    * It waits for a timeout until it has enough messages or when enough messages have arrived it waits to receive
    * no new messages for a while before returning
    *
    * @param consumer             initialized consumer to read messages from
    * @param expectedMessageCount the number of messages for which to wait
    * @return the list of received messages
    */
  def getMessages(consumer: KafkaConsumer[String, String], expectedMessageCount: Integer): Either[String, List[ConsumerRecord[String, String]]] = {
    // TODO: use recursive approach instead of mutable variable
    var records = List[ConsumerRecord[String, String]]()

    val deadline = 10.seconds.fromNow

    while (deadline.hasTimeLeft()) {
      val newRecords = consumer.poll(Duration.ofSeconds(1))
      if (newRecords.isEmpty && records.size >= expectedMessageCount) return Right(records)

      if (!newRecords.isEmpty) {
        records = records.concat(newRecords.iterator().asScala)
      }
    }

    Left(s"Received ${records.size} messages of the wanted $expectedMessageCount within 10 seconds")
  }
}
