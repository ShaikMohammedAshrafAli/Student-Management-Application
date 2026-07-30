# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY common-lib ./common-lib
RUN cd common-lib && mvn -B clean install -DskipTests

COPY grade-service/pom.xml ./grade-service/pom.xml
RUN cd grade-service && mvn -B dependency:go-offline
COPY grade-service/src ./grade-service/src
RUN cd grade-service && mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/grade-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
