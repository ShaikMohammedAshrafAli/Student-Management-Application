# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Build and install common-lib into the local Maven repo first,
# since auth-service depends on it as a local artifact.
COPY common-lib ./common-lib
RUN cd common-lib && mvn -B clean install -DskipTests

COPY auth-service/pom.xml ./auth-service/pom.xml
RUN cd auth-service && mvn -B dependency:go-offline
COPY auth-service/src ./auth-service/src
RUN cd auth-service && mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
