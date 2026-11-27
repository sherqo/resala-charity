# Resala Charity System

Java app for managing donations, volunteers, events, and beneficiaries.

> **Phase 2:** this branch is a clean slate. All Phase 1 source code, docs,
> diagrams and the report were removed. They remain available on `main` and in
> git history if ever needed.

## Requirements

- JDK 17+
- Maven 3.9+

## Build

```bash
mvn verify
```

## Layout

- `src/main/java` — application source (to be rebuilt for Phase 2)
- `src/main/resources` — config, styles, and app icons
- `pom.xml` — build config (JavaFX 23, SQLite, JUnit 5)

The SQLite database is created at `data/resala.db` on first run. The `data/`
directory is ignored by git.
