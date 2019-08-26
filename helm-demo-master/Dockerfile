FROM openjdk:8-jdk-alpine

MAINTAINER guoyz@jfrogchina.com

COPY target/api-1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
