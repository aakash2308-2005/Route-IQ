# Multi-stage build for the Spring Boot backend.
# Stage 1: build the jar with a full JDK + Maven (using the wrapper).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the Maven wrapper and pom first to leverage Docker layer caching for dependencies.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Copy sources and build (skip tests here; tests run in CI).
COPY src/ src/
RUN ./mvnw -q -B clean package -DskipTests

# Stage 2: slim runtime image with only a JRE and the built jar.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user (defense in depth).
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
