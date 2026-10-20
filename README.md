# Resala Charity System

Java + Maven console application for managing donations, volunteers, events, and beneficiaries.

## Build & Run

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.resala.Main"
```

## Layout

- `src/main/java/com/resala/` — source (see `files.md` for the full structure)
- `src/main/resources/` — `schema.sql`, `seed.sql`, `config.properties`
- `src/test/java/com/resala/` — JUnit tests
- `docs/` — requirements (`requirements.md`), class diagram, screenshots
- `data/resala.db` — SQLite database (git-ignored)
