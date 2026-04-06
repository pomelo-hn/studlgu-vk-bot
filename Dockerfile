# ===== Stage 1: Build Java app =====
FROM gradle:8.14-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

# ===== Stage 2: Runtime with Redis =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Установка Redis и bash
RUN apt-get update && \
    apt-get install -y redis-server bash && \
    rm -rf /var/lib/apt/lists/*

# Создание пользователя
RUN groupadd --gid 1001 app && \
    useradd --uid 1001 --gid 1001 -m -d /app app

# Копируем собранный JAR
COPY --from=build --chown=app:app /app/build/libs/*.jar /app/app.jar

# Скрипт запуска с проверкой Redis
RUN echo '#!/bin/bash\n\
echo "Starting Redis..."\n\
redis-server --port 6379 --daemonize yes\n\
sleep 2\n\
# Проверка что Redis поднят\n\
if redis-cli -p 6379 ping | grep -q PONG; then\n\
    echo "Redis is up"\n\
else\n\
    echo "Redis failed to start"\n\
    exit 1\n\
fi\n\
echo "Starting Java app..."\n\
exec java -jar /app/app.jar' > /app/start.sh && chmod +x /app/start.sh

USER app

# Порты
EXPOSE 8080
EXPOSE 6379

ENTRYPOINT ["/app/start.sh"]