FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY guessme/pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress
COPY guessme/src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r appuser \
    && useradd -r -g appuser appuser
COPY --from=build /build/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENV PORT=8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:${PORT}/api/game/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
