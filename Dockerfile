# Stage 1: build app
FROM gradle:8.14-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

# Stage 2: runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]