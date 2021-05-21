package com.example

import com.github.mjakubowski84.parquet4s._
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import io.circe.{Encoder, Json}
import scalaz._
import scalaz.std.either._
import scalaz.std.list._

import java.nio.file.Files

sealed trait AppError
case class ParsingError(message: String) extends AppError
case class UnknownError(e: Throwable)    extends AppError

object ParquetParser extends LazyLogging {

  implicit val encodeParquet: Encoder[RowParquetRecord] = (row: RowParquetRecord) => {
    val parsedRow = row.iterator.toList.map(rowElement => {
      val parsedValue = rowElement._2 match {
        case LongValue(v)   => Right(v.asJson)
        case IntValue(v)    => Right(v.asJson)
        case FloatValue(v)  => Right(v.asJson)
        case DoubleValue(v) => Right(v.asJson)
        // TODO: leave as binary, as it could also not be a string...
        case BinaryValue(v)  => Right(v.toStringUsingUTF8.asJson)
        case BooleanValue(v) => Right(v.asJson)
        case NullValue       => Right(Json.Null)
        case _ =>
          logger.warn(f"Unknown type in column '${rowElement._1}': ${rowElement._2}")
          Left(f"Encountered unparsable row for column '${rowElement._1}'")
      }
      parsedValue.map(json => (rowElement._1, json))
    })

    Traverse[List].sequence(parsedRow) match {
      case Left(errorMessage) => Json.fromString(errorMessage)
      case Right(data)        => Json.obj(data: _*)
    }
  }

  private val tmpFile = Files.createTempFile("parquet-split", ".parquet")

  /**
    * @param data a parquet file with no particular schema.
    * @return list of JSON documents (one document per input/Parquet row)
    */
  def parseAndSplit(data: Array[Byte]): Either[AppError, List[Json]] = {
    Files.write(tmpFile, data)

    val readData = ParquetReader.read[RowParquetRecord](tmpFile.toString)
    try {
      val recordList = readData
        .map(record => {
          val encodedRow = record.asJson
          encodedRow.isObject match {
            case false => Left(ParsingError(encodedRow.noSpaces))
            case true  => Right(encodedRow)
          }
        })
        .toList
      Traverse[List].sequence(recordList)
    } catch {
      case e: Throwable if e.toString.contains("is not a Parquet file") => Left(ParsingError("invalid parquet file"))
      case e: Throwable => Left(UnknownError(e))
    } finally readData.close()
  }
}
