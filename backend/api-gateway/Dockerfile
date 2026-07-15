# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY common-lib ./common-lib
RUN cd common-lib && mvn -B clean install -DskipTests

COPY api-gateway/pom.xml ./api-gateway/pom.xml
RUN cd api-gateway && mvn -B dependency:go-offline
COPY api-gateway/src ./api-gateway/src
RUN cd api-gateway && mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
