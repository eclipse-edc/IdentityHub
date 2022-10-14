# SQL Identity Hub store

Provides SQL persistence for Identity Hub.

## Prerequisites

Please apply this [schema](docs/schema.sql) to your SQL database.

## Entity Diagram

![ER Diagram](docs/er.png)

## Test

In order to run the test locally, you must first start a local instance of PostgreSQL:

```bash
docker run -p 5432:5432 -e POSTGRES_PASSWORD=password postgres:14.2
```

Then test can be executed through:

```bash
./gradlew :extensions:store:sql:identity-hub-store-sql:test -DincludeTags="PostgresqlIntegrationTest"
```