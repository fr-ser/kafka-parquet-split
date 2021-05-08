package com.example

import com.github.mjakubowski84.parquet4s.ParquetWriter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.{Files, Paths}
import scala.util.Random

class ParquetParserTest extends AnyFlatSpec with Matchers {
  "The ParquetParser" should "parse a parquet and return a list of items for each row" in {
    // TODO: Why does the test fail if I move the class definition higher?
    case class TestRow(id: Int, text: String)
    val tmpPath = Paths.get(Files.createTempDirectory("test").toString, "file.parquet").toString
    val testRows = (1 to 12).map { i =>
      TestRow(id = i, text = Random.nextString(4))
    }
    ParquetWriter.writeAndClose(tmpPath, testRows)

    val byteArray = Files.readAllBytes(Paths.get(tmpPath))

    ParquetParser.parseAndSplit(byteArray).length shouldBe testRows.length
  }

  //  "The ParquetParser" should "convert the rows to valid json" in {
  //    // TODO: write test
  //  }
}
