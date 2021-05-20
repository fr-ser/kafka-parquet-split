package com.example

import com.github.mjakubowski84.parquet4s.ParquetWriter
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.{Files, Paths}
import scala.util.Random

class ParquetParserTest extends AnyFlatSpec with Matchers {
  "The ParquetParser" should "parse a parquet and return a list of items for each row" in {
    val tmpPath = Paths.get(Files.createTempDirectory("test").toString, "file.parquet").toString
    case class SimpleTestRow(id: Int, text: String)

    val testRows = (1 to 12).map { i =>
      SimpleTestRow(id = i, text = Random.nextString(4))
    }
    ParquetWriter.writeAndClose(tmpPath, testRows)

    val byteArray = Files.readAllBytes(Paths.get(tmpPath))

    ParquetParser.parseAndSplit(byteArray) match {
      case Left(errorMessage) => errorMessage shouldBe null
      case Right(data)        => data.length shouldBe testRows.length
    }
  }

  "The ParquetParser" should "convert the rows to valid json" in {
    val tmpPath = Paths.get(Files.createTempDirectory("test").toString, "file.parquet").toString
    case class TestRow(
      id: Int,
      text: String,
      active: Boolean,
      time: Long,
      amount: Double
      // TODO: handling of null in the parquet library is tricky
      // https://github.com/mjakubowski84/parquet4s/issues/204
      // maybeSomething: Option[Int]
    )

    val testRows = List(
      TestRow(12, "hi", active = true, 122L, 12.3),
      TestRow(13, "hello", active = false, 133L, 13.3)
    )
    ParquetWriter.writeAndClose(tmpPath, testRows)

    val byteArray = Files.readAllBytes(Paths.get(tmpPath))

    val expected = Right(testRows.map(row => row.asJson))

    ParquetParser.parseAndSplit(byteArray) shouldBe expected
  }

  "The ParquetParser" should "return a Left for invalid parquet ArrayBuffers" in {
    ParquetParser.parseAndSplit(Array[Byte](12.toByte, 144.toByte)).isLeft shouldBe true
  }
}
