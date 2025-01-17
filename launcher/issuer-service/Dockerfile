# -buster is required to have apt available
FROM openjdk:17-slim-buster

# Optional JVM arguments, such as memory settings
ARG JVM_ARGS=""

RUN apt update \
  && apt install -y curl \
  && rm -rf /var/cache/apt/archives /var/lib/apt/lists

WORKDIR /app

COPY ./build/libs/issuer-service.jar /app

EXPOSE 8188

ENV WEB_HTTP_PORT="8181"
ENV WEB_HTTP_PATH="/api"

HEALTHCHECK --interval=5s --timeout=5s --retries=10 CMD curl --fail http://localhost:8181/api/check/health

# Use "exec" for graceful termination (SIGINT) to reach JVM.
# ARG can not be used in ENTRYPOINT so storing value in an ENV variable
ENV ENV_JVM_ARGS=$JVM_ARGS
ENTRYPOINT [ "sh", "-c", "exec java $ENV_JVM_ARGS -jar issuer-service.jar"]