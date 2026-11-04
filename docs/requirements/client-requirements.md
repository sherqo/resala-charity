# Client Requirements — Resala Charity Management System

**Phase:** 1 (Client requirements, 0.5 marks)

> Client-facing needs that drove Phase 1. They are derived from the project brief and the branch workflow, and each one is traced to the module that implements it.

## Requirements

| ID | Client need and expected outcome | Acceptance | Module / feature |
|----|----------------------------------|------------|------------------|
| CR1 | At year end, staff rebuild each donor's total by hand. The client needs an accurate statement for any donor and year, covering monetary donations only, without re-entering data. | The total matches the recorded online and check donations for that donor and year. | Donations: `DonationService.yearlyStatement(donorId, year)` |
| CR2 | Coordinators assign volunteers from memory, which causes skill mismatches and no-shows. The client needs every assignment checked against availability and the required skill before it is confirmed. | An unavailable or unskilled volunteer is refused with a reason; an eligible volunteer is assigned. | Volunteers: `VolunteerService.validateAssignment(...)` |
| CR3 | Certificates are calculated by hand and issued late. The client needs a certificate produced only after 20 verified hours, showing the correct total. | Below 20 hours the certificate is refused; at or above 20 it shows the recorded total. | Volunteers: `CertificateGenerator.generate(...)`, `VolunteerService.certificate(...)` |
| CR4 | Attendees are not told when an event changes, so some travel for nothing. The client needs every registered attendee informed automatically when the event is updated, opened, closed or rescheduled. | Each registered attendee receives a notification after the change. | Events: `EventSubject` + `EventNotificationObserver` |
| CR5 | Aid is recorded on paper, so the branch cannot say who received what, when, or whether it was already given. The client needs one history per beneficiary that staff can check before distributing again. | The history lists only that beneficiary's distributions, with item, quantity and date. | Beneficiaries: `BeneficiaryService.recordDistribution(...)`, `/distributeByPolicy(...)` |
| CR6 | Every new event type or skill currently needs a developer. The client needs staff to add, rename and remove these values themselves without breaking existing records. | All four operations work; duplicate names and deletion of a used value are refused. | Lookups: `LookupView`, `EventController.renameEventType(...)`, `VolunteerController.renameSkill(...)` |
| CR7 | Supporters use different channels, and the branch cannot run a separate tool for each. The client needs one message screen where the channel can be changed immediately. | The next message uses the newly selected channel and is recorded in the audit log. | Communication: `NotificationStrategy` + `EmailStrategy`/`SmsStrategy`/`SocialStrategy`, `Notifier` |

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
