FROM gradle:8.14-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --gid 1001 app && \
    useradd --uid 1001 --gid 1001 -m -d /app app

COPY --from=build --chown=app:app /app/build/libs/*.jar /app/app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]