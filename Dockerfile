FROM openjdk:8-jdk-alpine

RUN apk update && apk add ffmpeg

ARG JAR_FILE=app/target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]