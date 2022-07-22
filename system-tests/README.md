## System tests

Check the correct functioning of the Identity Hub CLI and CredentialVerifier.

## Setup 

Run test components with:

```bash
docker-compose -f system-tests/tests/docker-compose.yml up --build
```

Run test with:

```bash
./gradlew :system-tests:tests:test
```