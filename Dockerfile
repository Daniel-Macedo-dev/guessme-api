FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY guessme/pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress
COPY guessme/src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]
