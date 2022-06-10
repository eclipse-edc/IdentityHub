# Generating the OpenApi Spec

It is possible to generate an OpenAPI spec in the form of a `*.yaml` file by invoking a Gradle
task.

The file is at `resources/openapi/yaml/identity-hub.yaml`.

## Generate OpenAPI `yaml` file

To re-generate the YAML file, invoke 
```shell
./gradlew clean resolve
```

## Generate REST client

A REST client module is generated from the `yaml` file in the `rest-client` directory.

To re-generate the client module, invoke
```shell
./gradlew rest-client:clean rest-client:build
```
