package com.example

import com.github.mjakubowski84.parquet4s.{ParquetReader, RowParquetRecord}

import java.nio.file.Files

object ParquetParser {

  private val tmpFile    = Files.createTempFile("parquet-split", ".parquet")

  def parseAndSplit(data: Array[Byte]): List[String] = {
    Files.write(tmpFile, data)

    var recordList = List.empty[String]

    val readData = ParquetReader.read[RowParquetRecord](tmpFile.toString)
    try {
      recordList = readData.map(record => record.toString).toList
    } finally readData.close()

    recordList
  }
}
