# Auth Service Dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy parent pom and all module pom.xml files (Maven reactor needs all modules)
COPY pom.xml ./
COPY config-server/pom.xml ./config-server/
COPY auth-service/pom.xml ./auth-service/
COPY user-service/pom.xml ./user-service/
COPY loan-service/pom.xml ./loan-service/
COPY api-gateway/pom.xml ./api-gateway/

# Copy auth-service source code
COPY auth-service/src ./auth-service/src

# Build the application
RUN mvn clean package -pl auth-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring

# Copy the built JAR
COPY --from=builder /app/auth-service/target/*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/auth/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
