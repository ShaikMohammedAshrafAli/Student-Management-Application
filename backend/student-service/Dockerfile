# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY common-lib ./common-lib
RUN cd common-lib && mvn -B clean install -DskipTests

COPY student-service/pom.xml ./student-service/pom.xml
RUN cd student-service && mvn -B dependency:go-offline
COPY student-service/src ./student-service/src
RUN cd student-service && mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/student-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
