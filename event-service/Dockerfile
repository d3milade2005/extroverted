FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

# Copy only pom files first to leverage Docker cache
COPY pom.xml .
COPY event-service/pom.xml event-service/pom.xml
COPY event-service/settings.xml /root/.m2/settings.xml

# Pre-download dependencies (this layer will be cached unless POM changes)
RUN mvn -f event-service/pom.xml dependency:go-offline -B

# Now copy source code (this layer changes frequently)
COPY event-service/src/main/resources event-service/src/main/resources
COPY event-service/src/main/java event-service/src/main/java

# Build the JAR
RUN mvn -f event-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="olakunledemilade11@gmail.com"
LABEL service="event-service"
LABEL version="1.0.0"

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=build /workspace/event-service/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]