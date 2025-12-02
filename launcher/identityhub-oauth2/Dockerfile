FROM eclipse-temurin:24.0.2_12-jre-alpine

# Optional JVM arguments, such as memory settings
ARG JVM_ARGS=""
RUN apk --no-cache add curl

WORKDIR /app

COPY ./build/libs/identity-hub-oauth2.jar /app

EXPOSE 8188

ENV WEB_HTTP_PORT="8181"
ENV WEB_HTTP_PATH="/api"

HEALTHCHECK --interval=5s --timeout=5s --retries=10 CMD curl --fail http://localhost:8181/api/check/health

# Use "exec" for graceful termination (SIGINT) to reach JVM.
# ARG can not be used in ENTRYPOINT so storing value in an ENV variable
ENV ENV_JVM_ARGS=$JVM_ARGS
ENTRYPOINT [ "sh", "-c", "exec java $ENV_JVM_ARGS -jar identity-hub-oauth2.jar"]
