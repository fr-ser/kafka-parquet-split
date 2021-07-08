FROM hseeberger/scala-sbt:11.0.10_1.4.7_2.13.5 AS builder
# Java 11.0.10., SBT 1.4.7, Scala 2.13.5

WORKDIR /app
COPY build.sbt /app
COPY project /app/project

RUN sbt update

COPY src/main /app/src/main
RUN sbt assembly

FROM adoptopenjdk/openjdk11:jre-11.0.10_9-alpine

COPY --from=builder /app/target /app

WORKDIR /app

ENTRYPOINT [ "java", "-jar", "application.jar" ]
