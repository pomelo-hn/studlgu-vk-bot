FROM gradle:8.14-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

# Установка Redis
RUN apt-get update && \
    apt-get install -y redis-server && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 1001 app && \
    useradd --uid 1001 --gid 1001 -m -d /app app

COPY --from=build --chown=app:app /app/build/libs/*.jar /app/app.jar

# Скрипт запуска
RUN echo '#!/bin/bash\nredis-server --port 6379 & java -jar /app/app.jar' > /app/start.sh && \
    chmod +x /app/start.sh

USER app

EXPOSE 8080
EXPOSE 6379

ENTRYPOINT ["/app/start.sh"]