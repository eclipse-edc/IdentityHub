# Command-line client

The client is a Java JAR that provides access to an identity hub service via REST.

## Running the client

To run the command line client, and list available options and commands:

```bash
cd IdentityHub
./gradlew build
java -jar client-cli/build/libs/identity-hub-cli.jar --help
```

For example, to get verifiable credentials:

```
java -jar client-cli/build/libs/identity-hub-cli.jar \
  -s=http://localhost:8181/api \
  vc get
```

The client can also be run from a local Maven repository:

```
cd IdentityHub
./gradlew publishToMavenLocal
```

```
cd OtherDirectory
mvn dependency:copy -Dartifact=org.eclipse.dataspaceconnector.identityhub:identity-hub-cli:0.0.1-SNAPSHOT:jar:all -DoutputDirectory=.
java -jar identity-hub-cli-0.0.1-SNAPSHOT-all.jar --help
```

