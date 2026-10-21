FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Najprej samo odvisnosti, da se ob spremembi kode ne prenašajo znova.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Testi potrebujejo Docker (Testcontainers), zato tečejo ločeno z "./mvnw test", ne med gradnjo slike.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/teren-api-*.jar app.jar

FROM eclipse-temurin:25-jre
RUN apt-get update -y && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 1001 spring && useradd --system --uid 1001 --gid spring spring \
    && mkdir -p /app/logs && chown spring:spring /app/logs
WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

USER spring
EXPOSE 8080
ENV LOG_PATH=/app/logs

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=6 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
