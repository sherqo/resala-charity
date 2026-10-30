this system is for Resala Charity and we will divide it into several modules.

Donation

- Donor CRUD (individual/organization), record donations (online/check/in-kind), list/filter by donor, date, type.
- Receipt per donation; yearly tax statement per donor (sum).
- Donation-type lookup CRUD; fake payment gateway for online (Phase 2).
- Money stored as integer minor units, never double.
- Patterns: Factory (DonationFactory), Decorator (TaxReceipt, ThankYou), Observer (new donation → notify). Phase 2: Adapter (payment gateway), Template Method (receipt/monthly report).

Volunteer

- Volunteer CRUD + availability; skills (lookup) per volunteer.
- Assign to events (skill/date check); log hours per assignment; summaries (total, per event, per month).
- Certificate at ≥20 hours; search by skill/availability.
- Patterns: MVC + concrete `SqliteVolunteerRepository`; assignment checks are a private method in `VolunteerService`. Iterator and Strategy are intentionally not used.

Events

- Event CRUD; lifecycle DRAFT → OPEN → CLOSED.
- Register attendee with capacity check; ticket reference code.
- Attendance check-in (required, not optional); attendance report (registered/attended/no-show).
- Reminders delegated to Communication.
- Patterns: State (Phase 2, Draft/Open/Closed), Observer (reuse — event update → attendees).

Beneficiary

- Beneficiary CRUD; need types (lookup).
- Record distributions, incl. distributing in-kind stock; history per beneficiary.
- Impact metrics (people served, items/value distributed, per period) — all derived from stored rows.
- Patterns: none required. Optional Strategy (DistributionPolicy: by-need / equal / FIFO).

Communication

- Send to one person or a group (attendees, volunteers by skill, donors in a period).
- Channel chosen at runtime: email / SMS / social (mocked).
- Templates (receipt, thank-you, reminder, certificate); notification log; auto-triggers (new donation, event update).
- Patterns: Strategy (channel at runtime), Observer (home of Subject/Observer).

Core / admin

- Login, logout, self-signup (VOLUNTEER only); ADMIN creates STAFF/ADMIN.
- RBAC: Permission enum + Role (ADMIN/STAFF/VOLUNTEER) + Session.can(); menus filtered, services/proxy enforce.
- Single UI, role-filtered menu; summary dashboard; lookup-table CRUD; user management; undo last action.
- DB + config singletons (done).
- Patterns: Singleton (config/DB), MVC (view interfaces + controllers), Proxy (Phase 2, RBAC), Facade (Phase 2, NonProfitFacade), Abstract Factory (bonus, UiFactory). Cross-cutting: Command (Phase 2, menu action + undo).
