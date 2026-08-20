FROM node:24-alpine AS frontend-build

WORKDIR /frontend
COPY Website/Frontend/package*.json ./
RUN npm ci
COPY Website/Frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY --from=frontend-build /frontend/dist/Frontend/browser ./src/main/resources/META-INF/resources
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /deployments
COPY --from=build /workspace/target/quarkus-app/ ./

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
