# Kafka Batch Split

This repo is meant as a small demo application using Kafka Streams in Scala.
This application reads data from a topic containing parquet files and splits those into individual json rows.

## ToDos

- split Parquet into rows
- publish rows as JSON
- run e2e test with cucumber
- "extends App" is threading an issue here?
- run app within docker
- update sbt version
- run tests in CI (GitHub actions?)
- try other parquet packages