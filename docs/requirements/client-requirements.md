# Client Requirements — Resala Charity Management System

**Phase:** 1 (Client requirements, 0.5 marks)

> Client-facing needs that drove Phase 1. They are derived from the project brief and the branch workflow, and each one is traced to the module that implements it.

## Requirements

| ID | Client need and expected outcome | Addressed by |
|----|----------------------------------|--------------|
| CR1 | **Year-end statements.** At year end, staff rebuild each donor's total by hand. The client needs an accurate statement for any donor and year, covering monetary donations only, without re-entering data. | **FR-D6**; Donations: `DonationService.yearlyStatement(donorId, year)` |
| CR2 | **Safe volunteer assignment.** Coordinators assign volunteers from memory, which causes skill mismatches and no-shows. The client needs every assignment checked against availability and the required skill before it is confirmed. | **FR-V4**; Volunteers: `VolunteerService.validateAssignment(...)` |
| CR3 | **Volunteer certificates.** Certificates are calculated by hand and issued late. The client needs a certificate produced only after 20 verified hours, showing the correct total. | **FR-V5, FR-V6**; Volunteers: `CertificateGenerator.generate(...)`, `VolunteerService.certificate(...)` |
| CR4 | **Event changes.** Attendees are not told when an event changes, so some travel for nothing. The client needs every registered attendee informed automatically when the event is updated, opened, closed or rescheduled. | **FR-E6**; Events: `EventSubject` + `EventNotificationObserver` |
| CR5 | **Aid history.** Aid is recorded on paper, so the branch cannot say who received what, when, or whether it was already given. The client needs one history per beneficiary that staff can check before distributing again. | **FR-B3, FR-B4**; Beneficiaries: `BeneficiaryService.recordDistribution(...)`, `/distributeByPolicy(...)` |
| CR6 | **Staff-maintained lists.** Every new event type or skill currently needs a developer. The client needs staff to add, rename and remove these values themselves without breaking existing records. | **FR-R1 to FR-R5**; Lookups: `LookupView`, `EventController.renameEventType(...)`, `VolunteerController.renameSkill(...)` |
| CR7 | **One message screen.** Supporters use different channels, and the branch cannot run a separate tool for each. The client needs one message screen where the channel can be changed immediately. | **FR-C1 to FR-C3**; Communication: `NotificationStrategy` + `EmailStrategy`/`SmsStrategy`/`SocialStrategy`, `Notifier` |

## Traceability to Phase 1 deliverables

| Phase 1 item | How client requirements shaped it |
|--------------|-----------------------------------|
| Class diagram | Includes `Donation`, `Donor`, `Volunteer`, `Skill`, `EventType`, `Event`, `Beneficiary`, `Distribution` |
| Factory | `DonationFactory` supports online/check/in-kind donation types |
| Decorator | `TaxReceiptDecorator` + `ThankYouReceiptDecorator` on receipts |
| Observer | Donation received and event updated notifications |
| Strategy | Email/SMS/social communication channels, swappable at runtime |
| Singleton | `DatabaseManager`, `ConfigManager` |
| CRUD lookup tables | Event types (the chosen module) and skills via `LookupView` |
| MVC | Controllers + console/JavaFX views separated by layer |
