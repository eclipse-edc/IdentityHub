# Identity Hub

This repository contains an implementation for
the [Decentralized Claims Protocol (DCP) specification](https://projects.eclipse.org/projects/technology.dataspace-dcp).
In short, IdentityHub contains multiple VerifiableCredentials and
makes them available to authorized parties as VerifiablePresentations. It also receives VerifiableCredentials issued by
an issuer and stores them. Convenience features like automatic credential renewal and re-issuance are also included.
This functionality is sometimes referred to as "wallet".

IdentityHub makes heavy use of EDC components for core functionality, specifically those of
the [connector](https://github.com/eclipse-edc/Connector) for extension loading, runtime bootstrap, configuration, API
handling etc., while adding specific functionality using the EDC
extensibility mechanism.

Here, developers find everything necessary to build and run a basic "vanilla" version of IdentityHub.

## Quick start

A basic launcher configured with in-memory stores (i.e. no persistent storage) can be found [here](launcher/). There are
two ways of running IdentityHub:

1. As native Java process
2. Inside a Docker image

### Build the `*.jar` file

```bash
./gradlew :launcher:shadowJar
```

### Start IdentityHub as Java process

Once the jar file is built, IdentityHub can be launched using this shell command:

```bash
java -Dweb.http.resolution.port=10001 \
     -Dweb.http.resolution.path="/api/resolution" \
     -Dweb.http.port=8181 \
     -Dweb.http.path="/api" \
     -Dweb.http.identity.port=8182 \
     -Dweb.http.identity.path="/api/identity" \
     -Dedc.ih.api.superuser.key="c3VwZXItdXNlcgo=c3VwZXItc2VjcmV0Cg==" \
     -jar launcher/build/libs/identity-hub.jar
```

this will expose the Presentation API at `http://localhost:10001/api/resolution` and the Identity API
at `http://localhost:8191/api/identity`. More information about IdentityHub's APIs can be
found [here](docs/developer/architecture/identityhub-apis.md)

### Create the Docker image

```bash
docker build -t identity-hub ./launcher
```

### Start the Identity Hub

```bash
docker run --rm --name identity-hub \
            -e "WEB_HTTP_RESOLUTION_PORT=10001" \
            -e "WEB_HTTP_RESOLUTION_PATH=/api/resolution/" \
            -e "WEB_HTTP_PATH=/api" \
            -e "WEB_HTTP_PORT=8181" \
            -e "WEB_HTTP_IDENTITY_PORT=8182" \
            -e "WEB_HTTP_IDENTITY_PATH=/api/identity" \
            -e "EDC_IH_API_SUPERUSER_KEY=c3VwZXItdXNlcgo=c3VwZXItc2VjcmV0Cg==" \
            identity-hub:latest
```

## Architectural concepts of IdentityHub

Key architectural concepts are
outlined [here](docs/developer/architecture/identity-trust-protocol/identity.hub.architecture.md).

## Module structure of IdentityHub

IdentityHub's module structure and key SPIs is
described [here](docs/developer/architecture/identity-trust-protocol/identity-hub-modules.md).

_Please note that some classes or functionalities mentioned there may not yet have been implemented, for example
automatic credential renewal._

## API overview of IdentityHub

IdentityHub exposes several APIs that are described in more
detail [here](docs/developer/architecture/identityhub-apis.md).

## Future work

- Implementation of the Credential Issuance Protocol
- Support for VC Presentation Definition
- Support for VC Data Model 2.0

## Other documentation

Developer documentation can be found under [docs/developer](docs/developer), where the main concepts and decisions are
captured as [decision records](docs/developer/decision-records).

## References

- Decentralized Claims Protocol (DCP): https://projects.eclipse.org/projects/technology.dataspace-dcp
- VerifiableCredentials Data Model: https://www.w3.org/TR/vc-data-model/ (currently supported)
  and https://www.w3.org/TR/vc-data-model-2.0/ (planned)
- EDC Connector: https://github.com/eclipse-edc/Connector

## Contributing

See [how to contribute](https://github.com/eclipse-edc/docs/blob/main/CONTRIBUTING.md) for details.