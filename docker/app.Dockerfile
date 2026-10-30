# syntax=docker/dockerfile:1
#
# Imagem da API e do Worker. O modulo vem do build arg MODULE
# (order-book-api ou order-book-worker), definido no docker-compose.yml.
#
# O build acontece dentro da imagem: quem roda o projeto precisa so do Docker,
# sem Java nem Maven instalados. O cache de /root/.m2 (BuildKit) faz os
# builds seguintes nao baixarem as dependencias de novo.
#
# Os testes nao rodam aqui (-DskipTests): rodam no `mvn verify`.

FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build

COPY pom.xml .
COPY order-book-shared/pom.xml order-book-shared/
COPY order-book-api/pom.xml order-book-api/
COPY order-book-worker/pom.xml order-book-worker/
COPY order-book-shared/src order-book-shared/src
COPY order-book-api/src order-book-api/src
COPY order-book-worker/src order-book-worker/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -pl "${MODULE}" -am package -DskipTests \
 && cp "${MODULE}/target/${MODULE}-1.0-SNAPSHOT.jar" /build/app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER app

# Heap proporcional a memoria do container, e nao a da maquina.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
