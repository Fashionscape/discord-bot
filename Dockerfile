FROM gradle:7.2.0-jdk16 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle clean --no-daemon
RUN gradle build --no-daemon

FROM openjdk:16

EXPOSE 8080

ENV ENVIRONMENT=PROD
ENV GOOGLE_APPLICATION_CREDENTIALS=/root/.fsbot/google_application_credentials.json

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/* /app/fashionscape-bot.jar

ENTRYPOINT ["java","-jar","-Xmx3500m","/app/fashionscape-bot.jar"]
