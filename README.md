# Resala Charity System

Java console app for managing donations, volunteers, events, and beneficiaries.

## Requirements

- JDK 17+
- Maven 3.9+

## Run

```bash
mvn compile
mvn exec:java
```

The SQLite database is created at `data/resala.db` on first run. Delete it to reset.
Schema: `src/main/resources/schema.sql`. Sample data: `seed.sql`.
