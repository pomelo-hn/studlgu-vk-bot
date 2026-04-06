FROM gradle:8.14-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

# Установка Redis и tini
RUN apt-get update && \
    apt-get install -y redis-server tini && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 1001 app && \
    useradd --uid 1001 --gid 1001 -m -d /app app

COPY --from=build --chown=app:app /app/build/libs/*.jar /app/app.jar

# Скрипт запуска (runtime)
RUN echo '#!/bin/bash\n\
echo "Starting Redis..."\n\
redis-server --port 6379 &\n\
echo "Waiting for Redis to be ready..."\n\
until redis-cli -p 6379 ping | grep -q PONG; do\n\
  sleep 0.5\n\
done\n\
echo "Redis is up, starting Java app..."\n\
exec java -jar /app/app.jar' > /app/start.sh && chmod +x /app/start.sh

USER app

EXPOSE 8080
EXPOSE 6379

ENTRYPOINT ["/usr/bin/tini", "--", "/app/start.sh"]