FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

# Copy only pom files first to leverage Docker cache
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/settings.xml /root/.m2/settings.xml

# Pre-download dependencies (this layer will be cached unless POM changes)
RUN mvn -f api-gateway/pom.xml dependency:go-offline -B

# Now copy source code (this layer changes frequently)
COPY api-gateway/src/main/resources api-gateway/src/main/resources
COPY api-gateway/src/main/java api-gateway/src/main/java


# Build the JAR
RUN mvn -f api-gateway/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="olakunledemilade11@gmail.com"
LABEL service="api-gateway"
LABEL version="1.0.0"

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=build /workspace/api-gateway/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8090/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]