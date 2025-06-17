FROM eclipse-temurin:8-jdk-alpine
MAINTAINER pewee 20250617
RUN mkdir -p /app/java/  /media/music
ENV auther=pewee
WORKDIR /app/java/
COPY ./build/libs/neteasemusic-1.0.0.jar /app/java/app.jar
ENTRYPOINT java -jar app.jar