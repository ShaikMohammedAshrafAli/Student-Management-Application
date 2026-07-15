# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY common-lib ./common-lib
RUN cd common-lib && mvn -B clean install -DskipTests

COPY enrollment-service/pom.xml ./enrollment-service/pom.xml
RUN cd enrollment-service && mvn -B dependency:go-offline
COPY enrollment-service/src ./enrollment-service/src
RUN cd enrollment-service && mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/enrollment-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
