# ─── Stage 1: Build ─────────────────────────────────────────────

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Windows/Git may not preserve executable permission
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -q


# ─── Stage 2: Runtime ───────────────────────────────────────────

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Run application as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built jar
COPY --from=builder /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]