package com.example

import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.ValueTransformer
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.StreamsBuilder

case class ParquetSplit() extends LazyLogging {

  def getTopology(appConfig: AppConfig): Topology = {
    val builder      = new StreamsBuilder()
    val sourceStream = builder.stream[String, Array[Byte]](appConfig.sourceTopic)

    sourceStream.flatTransformValues(() => AppProcessorTransformer).to(appConfig.outputTopic)
    builder.build()
  }

  case object AppProcessorTransformer extends ValueTransformer[Array[Byte], Iterable[String]] {
    private var context: ProcessorContext = null

    override def init(context: ProcessorContext): Unit = {
      this.context = context
    }

    override def transform(parquetByteArray: Array[Byte]): List[String] = {
      ParquetParser.parseAndSplit(parquetByteArray) match {
        case Right(parsedList) => parsedList.map(_.noSpaces)
        case Left(ParsingError(_)) =>
          logger.error(f"Invalid message at offset ${this.context.offset()}")
          List.empty[String]
        case Left(_) => List.empty[String]
      }
    }

    override def close(): Unit = {}
  }
}
