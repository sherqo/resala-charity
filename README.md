# Resala Charity System

Console application (Java + Maven) for managing donations, volunteers, events, and
beneficiaries for a non-profit organization.

## Requirements

- **JDK 17 or newer** — that is the only thing you must install.
- Maven is **not** required: the project ships the Maven Wrapper (`mvnw` / `mvnw.cmd`),
  which downloads the right Maven version on first run.
- Optional: IntelliJ IDEA (or any IDE) if you prefer not to use the terminal.

## Run

Linux / macOS:

```bash
./mvnw compile       # first run downloads Maven + dependencies, may take a minute
./mvnw exec:java     # starts com.resala.Main
```

Windows:

```bat
mvnw.cmd compile
mvnw.cmd exec:java
```

If you already have Maven installed globally, `mvn compile` and `mvn exec:java` work too.

### Run from IntelliJ IDEA

1. `File > Open` and select the **folder containing `pom.xml`** — not a plain project.
2. Wait for the Maven import to finish (progress bar at the bottom).
3. Open `src/main/java/com/resala/Main.java` and press Run.

If you get `No suitable driver found for jdbc:sqlite:...`, the Maven dependencies were
not attached to the run configuration: right-click `pom.xml` → *Add as Maven Project*
(or *Reload All Maven Projects* in the Maven panel), then run again.

## Database

SQLite, created automatically on first start at `data/resala.db`
(see `db.url` in `src/main/resources/config.properties`).

- `schema.sql` — tables, run on every start (`CREATE TABLE IF NOT EXISTS`).
- `seed.sql` — sample data, loaded only when the `users` table is empty.
- **Reset the data:** delete `data/resala.db` and run again.

The database file is git-ignored; it is not part of the repository.

## Layout

- `src/main/java/com/resala/` — application source (packaged by feature)
- `src/main/resources/` — `config.properties`, `schema.sql`, `seed.sql`
- `docs/` — assignment requirements and client requirements
- `plans.md` — team plan, module ownership, and pattern map

## Status

The project is at the start of Phase 1: project setup (Maven, JDK, SQLite, Singleton
`ConfigManager` / `DatabaseManager`) is in place, and the feature modules are being
built on top of it.
