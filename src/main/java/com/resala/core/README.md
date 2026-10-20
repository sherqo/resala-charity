# Core

Shared machinery used by every module.

## Done

- `ConfigManager` — Singleton, loads `config.properties`
- `DatabaseManager` — Singleton, opens SQLite, applies `schema.sql`, seeds once

## To create

- `AuthService` — login, logout, self-signup (VOLUNTEER only)
- `PasswordHasher` — PBKDF2 + per-user salt, stored as `iterations:salt:hash`
- `Session` — current user + `can(Permission)`
- `NonProfitFacade` — Phase 2 (Facade): one entry point for views
- `command/` — Phase 2 (Command): menu actions, undo, action log
- `AccessDeniedException` — unchecked, caught by the menu

## Rules

- `Main` only wires dependencies together and starts the menu.
- `DatabaseManager` is the only place that opens a connection.
