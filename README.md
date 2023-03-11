# Identity Hub

This repository contains an implementation for a Decentralized Web Node such as defined by the
[Decentralized Identity Fundation](https://identity.foundation/decentralized-web-node/spec/).

See also the main [project page on Github](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector).

## Quick start

A basic launcher configured with in-memory stores (i.e. no persistent storage) is provider [here](launcher). You
can run it as a Docker container by following the steps below.

### Build the `.jar`

```bash
./gradlew :launcher:shadowJar
```

### Create the Docker image

```bash
docker build -t identity-hub ./launcher
```

### Start the Identity Hub

```bash
docker run -d --rm --name identity-hub -p 8188:8188 identity-hub
```

## Documentation

Developer documentation can be found under [docs/developer](docs/developer), where the main concepts and decisions are
captured as [decision records](docs/developer/decision-records).

## Contributing

See [how to contribute](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/blob/main/CONTRIBUTING.md) for
details.