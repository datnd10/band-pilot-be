# ─── Stage 1: Build ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first to cache dependency layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

# Copy source and build (skip tests — tests run in CI, not in image build)
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the fat JAR from the build stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Run with the "docker" profile by default; override with SPRING_PROFILES_ACTIVE env var
ENTRYPOINT ["java", "-jar", "app.jar"]
ENV SPRING_PROFILES_ACTIVE=docker
