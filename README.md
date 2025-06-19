# EDC Identity Hub

[![documentation](https://img.shields.io/badge/documentation-8A2BE2?style=flat-square)](https://eclipse-edc.github.io)
[![discord](https://img.shields.io/badge/discord-chat-brightgreen.svg?style=flat-square&logo=discord)](https://discord.gg/n4sD9qtjMQ)
[![latest version](https://img.shields.io/maven-central/v/org.eclipse.edc/boot?logo=apache-maven&style=flat-square&label=latest%20version)](https://search.maven.org/artifact/org.eclipse.edc/boot)
[![license](https://img.shields.io/github/license/eclipse-edc/IdentityHub?style=flat-square&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
<br>
[![build](https://img.shields.io/github/actions/workflow/status/eclipse-edc/IdentityHub/verify.yaml?branch=main&logo=GitHub&style=flat-square&label=ci)](https://github.com/eclipse-edc/IdentityHub/actions/workflows/verify.yaml?query=branch%3Amain)
[![snapshot build](https://img.shields.io/github/actions/workflow/status/eclipse-edc/IdentityHub/trigger_snapshot.yml?branch=main&logo=GitHub&style=flat-square&label=snapshot-build)](https://github.com/eclipse-edc/IdentityHub/actions/workflows/trigger_snapshot.yml)
[![nightly build](https://img.shields.io/github/actions/workflow/status/eclipse-edc/Identity-Hub/nightly.yml?branch=main&logo=GitHub&style=flat-square&label=nightly-build)](https://github.com/eclipse-edc/IdentityHub/actions/workflows/nightly.yml)

---

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

## Documentation

Base documentation can be found on the [documentation website](https://eclipse-edc.github.io). \
Developer documentation can be found under [docs/developer](docs/developer/README.md), \
where the main concepts and decisions are captured as [decision records](docs/developer/decision-records/README.md).

## Security Warning

Older versions of IdentityHub (in particular <= 0.3.1 ) **must not be used anymore**, as they were intended for
proof-of-concept
purposes only and may contain **significant security vulnerabilities** (for example missing authn/authz on the API) and
possibly
others.
**Please always use the latest version of IdentityHub.**

## Quick start

A basic launcher configured with in-memory stores (i.e. no persistent storage) can be
found [here](launcher/identityhub). There are
two ways of running IdentityHub:

1. As native Java process
2. Inside a Docker image

### Build the `*.jar` file

```bash
./gradlew :launcher:identityhub:shadowJar
```

### Start IdentityHub as Java process

Once the jar file is built, IdentityHub can be launched using this shell command:

```bash
java -Dweb.http.credentials.port=10001 \
     -Dweb.http.credentials.path="/api/credentials" \
     -Dweb.http.port=8181 \
     -Dweb.http.path="/api" \
     -Dweb.http.identity.port=8182 \
     -Dweb.http.identity.path="/api/identity" \
     -jar launcher/identityhub/build/libs/identity-hub.jar
```

this will expose the Presentation API at `http://localhost:10001/api/presentation` and the Identity API
at `http://localhost:8182/api/identity`. More information about IdentityHub's APIs can be
found [here](docs/developer/architecture/identityhub-apis.md)

### Create the Docker image

```bash
docker build -t identity-hub ./launcher/identityhub
```

### Start the Identity Hub

```bash
docker run -d --rm --name identityhub \
            -e "WEB_HTTP_IDENTITY_PORT=8182" \
            -e "WEB_HTTP_IDENTITY_PATH=/api/identity" \
            -e "WEB_HTTP_PRESENTATION_PORT=10001" \
            -e "WEB_HTTP_PRESENTATION_PATH=/api/presentation" \
            -e "EDC_IAM_STS_PRIVATEKEY_ALIAS=privatekey-alias" \
            -e "EDC_IAM_STS_PUBLICKEY_ID=publickey-id" \
            identityhub:latest
```

## Architectural concepts of IdentityHub

Key architectural concepts are
outlined [here](docs/developer/architecture/decentralized-claims-protocol/identity.hub.architecture.md).

## Module structure of IdentityHub

IdentityHub's module structure and key SPIs is
described [here](docs/developer/architecture/decentralized-claims-protocol/identity-hub-modules.md).

_Please note that some classes or functionalities mentioned there may not yet have been implemented, for example
automatic credential renewal._

## API overview of IdentityHub

IdentityHub exposes several APIs that are described in more
detail [here](docs/developer/architecture/identityhub-apis.md).

## Future work

- Implementation of the Credential Issuance Protocol
- Support for VC Presentation Definition
- Support for VC Data Model 2.0

## References

- Decentralized Claims Protocol (DCP): https://projects.eclipse.org/projects/technology.dataspace-dcp
- VerifiableCredentials Data Model: https://www.w3.org/TR/vc-data-model/ (currently supported)
  and https://www.w3.org/TR/vc-data-model-2.0/ (planned)
- EDC Connector: https://github.com/eclipse-edc/Connector

## Contributing

See [how to contribute](https://github.com/eclipse-edc/eclipse-edc.github.io/blob/main/CONTRIBUTING.md).
