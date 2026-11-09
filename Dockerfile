# ---- Build stage: compiles the React frontend and the Spring Boot jar ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Cache Maven dependencies separately from source changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY assets assets
COPY frontend frontend
COPY src src
RUN ./mvnw -B -DskipTests package && cp target/*.jar app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /app/app.jar app.jar
USER app

# Tuned for small free-tier instances (~512 MB RAM)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# Render (and most PaaS) inject PORT; fall back to 8080 locally
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
