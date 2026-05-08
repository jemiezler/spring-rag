# =============================================================
# Multi-stage Dockerfile – Spring AI Local RAG
# =============================================================

# ── Stage 1: Build ────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /workspace

# Copy Maven wrapper + pom for better layer caching
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw

# Download Maven dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -ntp dependency:go-offline

# Copy source code
COPY src src

# Build application
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -ntp clean package -DskipTests

# Extract Spring Boot layers
RUN java -Djarmode=layertools \
    -jar target/*.jar extract --destination target/extracted


# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

USER spring

WORKDIR /app

# Copy extracted layers
COPY --from=builder /workspace/target/extracted/dependencies/ ./
COPY --from=builder /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/target/extracted/application/ ./

EXPOSE 8080

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]