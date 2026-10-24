FROM gradle:9.7.1-jdk21 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/fashionscape-bot.jar

ENTRYPOINT ["java", "-jar", "-Xmx3500m", "/app/fashionscape-bot.jar"]
