FROM openjdk:17-slim-buster

# Optional JVM arguments, such as memory settings
ARG JVM_ARGS=""

WORKDIR /app

COPY ./build/libs/app.jar /app

EXPOSE 8181
EXPOSE 9191
EXPOSE 8282

ENV WEB_HTTP_PORT="8181"
ENV WEB_HTTP_PATH="/api"
ENV WEB_HTTP_DATA_PORT="9191"
ENV WEB_HTTP_DATA_PATH="/api/v1/data"
ENV WEB_HTTP_IDS_PORT="8282"
ENV WEB_HTTP_IDS_PATH="/api/v1/ids"

ENV JVM_ARGS=$JVM_ARGS
ENTRYPOINT [ "sh", "-c", "java $JVM_ARGS -jar app.jar"]
