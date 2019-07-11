FROM openjdk:8-jdk-alpine
WORKDIR /data/symphony
COPY ./target/*.jar bot.jar
COPY internal_truststore internal_truststore
COPY data.csv data.csv
ENTRYPOINT [ "java", "-jar", "./bot.jar" ]
