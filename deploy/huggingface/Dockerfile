# Hugging Face Space build: fetches the app source from GitHub and builds it.
# Upload this file and README.md (from this folder) to a Docker Space.
FROM eclipse-temurin:25-jdk AS build
RUN apt-get update && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

ARG REPO=https://github.com/Adzky/eco-nomz.git
ARG BRANCH=main

# Busts the build cache whenever the branch gets a new commit
ADD https://api.github.com/repos/Adzky/eco-nomz/git/refs/heads/${BRANCH} /tmp/ref.json

RUN git clone --depth 1 --branch "${BRANCH}" "${REPO}" /app
WORKDIR /app
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
    && ./mvnw -B -DskipTests package \
    && cp target/*.jar /app.jar

# ---- Runtime ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
# Hugging Face runs Spaces as UID 1000 (the image's existing non-root "ubuntu" user)
USER 1000

ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=7860 \
    JAVA_OPTS="-XX:MaxRAMPercentage=60"
EXPOSE 7860

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
