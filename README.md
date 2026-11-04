# Resala Charity System

Java app for managing donations, volunteers, events, and beneficiaries.

## Requirements

- JDK 17+
- Maven 3.9+

## Compile & Run

```bash
mvn compile
mvn -q exec:java
mvn javafx:run
```

The SQLite database is created at `data/resala.db` on first run. Delete it to reset.
Schema: `src/main/resources/schema.sql` (it also inserts the sample lookup data).
