FROM hseeberger/scala-sbt:11.0.11_1.5.4_2.13.6 AS builder
# Java 11.0.11., SBT 1.5.4, Scala 2.13.6

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
