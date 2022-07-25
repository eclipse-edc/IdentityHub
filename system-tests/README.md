## System tests

System tests deploy a sample EDC connector with the Identity Hub extension and check the correct functioning of the Identity Hub CLI and CredentialVerifier.

## Running tests locally 

Build launcher for system tests
```bash
./gradlew :system-tests:launcher:build
```

Run test components with:

```bash
docker-compose -f system-tests/tests/docker-compose.yml up --build
```

Run test with:

```bash
./gradlew :system-tests:tests:test
```