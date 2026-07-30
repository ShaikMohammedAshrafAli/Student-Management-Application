# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY common-lib ./common-lib
RUN cd common-lib && mvn -B clean install -DskipTests

COPY course-service/pom.xml ./course-service/pom.xml
RUN cd course-service && mvn -B dependency:go-offline
COPY course-service/src ./course-service/src
RUN cd course-service && mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/course-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
