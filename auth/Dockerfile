# ============================================================
# Stage 1 — Dependency cache
# Copy only pom.xml first so Maven dependency layer is cached
# independently from source code changes.
# ============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS deps

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress


# ============================================================
# Stage 2 — Build & package
# ============================================================
FROM deps AS build

COPY src ./src

# -DskipTests: tests run in CI pipeline, not in Docker build
RUN mvn package -DskipTests -B --no-transfer-progress


# ============================================================
# Stage 3 — Extract Spring Boot layers
# Spring Boot layered JAR splits the app into:
#   dependencies        (changes rarely   → cached long)
#   spring-boot-loader  (changes rarely   → cached long)
#   snapshot-dependencies (changes sometimes)
#   application         (changes every build → rebuilt only)
# This gives the smallest possible layer diffs on each deploy.
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS extractor

WORKDIR /extract

COPY --from=build /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract


# ============================================================
# Stage 4 — Final runtime image (production)
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# ── Security: create a non-root user ─────────────────────────────────────────
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# ── Copy Spring Boot layers in cache-optimal order ───────────────────────────
COPY --from=extractor --chown=appuser:appgroup /extract/dependencies/          ./
COPY --from=extractor --chown=appuser:appgroup /extract/spring-boot-loader/    ./
COPY --from=extractor --chown=appuser:appgroup /extract/snapshot-dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /extract/application/           ./

# ── Create writable dirs required by readOnlyRootFilesystem in k8s ───────────
RUN mkdir -p /tmp /app/logs && chown -R appuser:appgroup /tmp /app/logs

# ── Switch to non-root ────────────────────────────────────────────────────────
USER appuser

EXPOSE 8080

# ── JVM tuning for containerised environments ─────────────────────────────────
# +UseContainerSupport      — respect k8s CPU/memory limits (default Java 11+, explicit here)
# MaxRAMPercentage=75.0     — use 75% of container memory limit for heap
# +ExitOnOutOfMemoryError   — fail fast; let k8s restart rather than thrash
# TieredStopAtLevel=1       — faster startup (disable C2 JIT; trade peak throughput for startup)
#                             Remove this line if the service is long-lived and throughput matters
# security.egd              — avoid Tomcat blocking on /dev/random at startup
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError \
  -XX:TieredStopAtLevel=1 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# ── Docker-level health check (fallback if k8s probes are not available) ─────
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
