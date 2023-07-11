# SQL Identity Hub store

Provides SQL persistence for Identity Hub.

## Prerequisites

Please apply this [schema](docs/schema.sql) to your SQL database.

## Entity Diagram

![ER Diagram](docs/er.png)

## Test

```bash
./gradlew test -DincludeTags="ComponentTest"
```