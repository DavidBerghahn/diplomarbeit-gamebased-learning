FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /deployments
COPY --from=build /workspace/target/quarkus-app/ ./

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
