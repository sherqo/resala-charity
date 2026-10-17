# Resala Charity System: Plans (Java)

Team of 5. Java 17+, Maven, JUnit 5, SQLite (via `sqlite-jdbc`) or in-memory lists. Core app first, JavaFX GUI only if time allows.

## 1. Work Split

| # | Role | Owns | Patterns |
|---|------|------|----------|
| 1 | Lead / Core | Project skeleton, MVC wiring, DB, auth/login, integration, final demo | Singleton, Facade, Proxy, Command |
| 2 | Donations + Payment | Donor CRUD, donation types, receipts, payment module | Factory, Decorator, Adapter, Template Method |
| 3 | Volunteers + Events | Volunteer CRUD, hours, certificates, event registration and attendance | State, Iterator |
| 4 | Beneficiaries + Communication | Beneficiary CRUD, resource distribution, email/SMS/social (mocked) | Strategy, Observer |
| 5 | UI + Docs | Control panel, admin pages, lookup-table CRUD, class diagram, client requirements | None (owns the 2-mark diagram) |

## 2. Ground Rules

1. **Day 1, 1 hour together:** agree on shared entities (`Donor`, `Volunteer`, `Event`, `Beneficiary`) and each module's public interface. Person 1 pushes the skeleton first; everyone branches from it.
2. **Person 5 is not the easy role.** Diagram, client requirements, and control panel are 4+ marks in Phase 1. Collect requirements from real Resala volunteers early (graded item).
3. **Persons 3 and 4 have fewer patterns.** Give them extra CRUD and the admin pages' backend to balance the load.
4. **Git from day 1.** One branch per person (`feature/<name>-<module>`), merge to `main` through Person 1 only. Merging everything the night before is the biggest trap.
5. **Phase 1 first, Phase 2 later.** Don't touch Command, Adapter, etc. until Phase 1 is graded and working.
6. **Each pattern once, in the obvious place,** and drawn clearly in the class diagram.

## 3. Architecture

- **MVC, console UI first.** Models are plain POJOs, controllers hold logic, views are console menus (later replaceable by JavaFX).
- **Layers:** `view` -> `controller` -> `service` -> `repository` -> `db`.
- **Rule for the GUI plan:** controllers and services must never print or read from `System.in`. Only `view` classes do I/O. This keeps the JavaFX swap cheap.
- **Interfaces between modules:** each module exposes one `*Service` interface. Other modules depend on the interface, never the implementation.

## 4. Pattern Mapping (Java)

### Phase 1

| Pattern | Where | Java sketch |
|---------|-------|-------------|
| Strategy | Notification channels | `interface NotificationStrategy { void send(String to, String msg); }` with `EmailStrategy`, `SmsStrategy`, `SocialStrategy`; `Notifier` holds the current strategy |
| Observer | New donation / event update notifies subscribers | `interface Observer { void update(Event e); }`, `Subject` keeps a `List<Observer>`; volunteers and donors subscribe |
| Decorator | Donation add-ons | `interface Donation { double getAmount(); String getDescription(); }`, `DonationDecorator` base, `TaxReceiptDecorator`, `ThankYouDecorator` |
| Factory | Donation types | `DonationFactory.create(type, amount, donor)` returns `OnlineDonation`, `CheckDonation`, `InKindDonation` |
| Singleton | DB connection / config | `DatabaseManager.getInstance()` (enum or holder idiom, thread-safe) |

### Phase 2

| Pattern | Where | Java sketch |
|---------|-------|-------------|
| Command | Undo/redo and action log | `interface Command { void execute(); void undo(); }`, `CommandHistory` with two `Deque<Command>` |
| Adapter | Fake payment gateway | `PaymentProcessor` interface, `FakeGatewayAdapter` wraps `LegacyGateway` with a different API |
| Facade | UI entry point | `NonProfitFacade` exposes `donate()`, `registerVolunteer()`, `createEvent()`, etc. |
| Template Method | Reports / receipts | `abstract class ReportGenerator { final String generate() { header(); body(); footer(); } }` |
| Iterator | Loop over donations/volunteers | Implement `Iterable<T>` on collection classes with a custom `Iterator<T>` (e.g., filter by date) |
| State | Event status | `EventState` interface; `DraftState`, `OpenState`, `ClosedState` each handle `next()` and allowed actions |
| Proxy | Access control for admin pages | `AdminService` interface, `AdminServiceProxy` checks `currentUser.role` before delegating |

## 5. Practical Choices

- Use the Java version and tools your lecturer teaches. Don't switch stacks.
- SQLite or in-memory lists. No real DB server.
- Mock email, SMS, and payment (print to console / write to a log file).
- Build Donations fully. Keep other modules thin with basic CRUD.
- Unit-test every pattern with at least one JUnit test; it doubles as demo proof.

## 6. Timeline

| When | What |
|------|------|
| Week 1 | Skeleton, shared models, diagram draft, requirements interviews |
| Weeks 2-3 | Phase 1 patterns + CRUD |
| Phase 1 deadline - 3 days | Integrate, bug fixes only |
| After Phase 1 | Phase 2 patterns |
| Last week before final | Full integration testing ("All system working" = 3 marks) |

## 7. Optional: JavaFX GUI Plan (only if time allows)

**Trigger:** start only when Phase 1 is graded, all CRUD works from the console, and Phase 2 patterns are at least integrated. If Phase 2 is behind, skip this entirely.

### Approach
- JavaFX 21 via Maven (`org.openjfx:javafx-controls`, `javafx-fxml`) with `javafx-maven-plugin`.
- **FXML + controllers** for screens, so it stays MVC. FX controllers only call `NonProfitFacade`; no business logic inside them.
- Keep the console UI working as a fallback. `Main` picks `--gui` or console.

### Screens (in priority order)
1. **Login** (uses the Proxy/auth already built)
2. **Dashboard** (counts of donors, volunteers, upcoming events; links to modules)
3. **Donations** (table + add form, type dropdown backed by the Factory, decorator checkboxes)
4. **Volunteers + Events** (tables, register volunteer to event, event state button)
5. **Beneficiaries** (table + distribution form)
6. **Admin / Control panel** (lookup tables, user roles)
7. **Notifications** (channel picker using Strategy)

### Who does what

| Person | GUI task |
|--------|----------|
| 1 | Main app launcher, scene navigation helper, login screen, wiring `--gui` flag |
| 2 | Donations screen |
| 3 | Volunteers + Events screens |
| 4 | Beneficiaries + Notifications screens |
| 5 | Dashboard, admin panel, shared CSS and layout, screenshots for docs |

### GUI Rules
- One shared `SceneManager` class (`switchTo("donations")`).
- Use `TableView` with `ObservableList` bound to service results; refresh after each action.
- Run anything slow on a background `Task`; update UI via `Platform.runLater`.
- Show errors with `Alert`, never stack traces.
- One shared `style.css`; no per-screen styling.

### GUI Timeline (2 weeks max)
| When | What |
|------|------|
| Days 1-2 | Maven setup, `SceneManager`, login + dashboard shell |
| Days 3-7 | Each person builds their screens in parallel |
| Days 8-10 | Integration, CSS polish |
| Days 11-14 | Bug fixes, demo rehearsal |
