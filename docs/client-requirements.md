# Client Requirements — Resala Charity Management System

**Phase:** 1 (Client requirements, 0.5 marks)

> Requirements collected from users at Resala, extending the basic requirements from the brief. Each requirement is traced to the module/class that implements it.

## Requirements

| ID | Requirement | Module | Class / feature that handles it |
|----|-------------|--------|---------------------------------|
| CR1 | Generate a yearly tax statement per donor (sum of monetary donations for a year) | Donations | `DonationService.yearlyStatement(donorId, year)` |
| CR2 | Validate volunteer skill and availability before assigning to an event | Volunteers | `VolunteerService.validateAssignment(...)` |
| CR3 | Issue a volunteer certificate after 20 cumulative hours | Volunteers | `CertificateGenerator.generate(...)`, `VolunteerService.certificate(volunteerId)` |
| CR4 | Notify registered attendees and volunteers when an event is updated or rescheduled | Events | `EventSubject` + `EventNotificationObserver` (Observer pattern) |
| CR5 | Record aid distribution per beneficiary (item, event, date) | Beneficiaries | `DistributionService.distributeAid(...)`, `distributions` table |
| CR6 | Full CRUD (create, read, update, delete) for skills and event types lookup tables | Lookups | `LookupView`, `VolunteerController`, `EventController` |
| CR7 | Support email, SMS, and social media channels, swappable at runtime | Communication | `NotificationStrategy` + `EmailStrategy`/`SmsStrategy`/`SocialStrategy`, `Notifier` (Strategy pattern) |

## Traceability to Phase 1 deliverables

| Phase 1 item | How client requirements shaped it |
|--------------|-----------------------------------|
| Class diagram | Includes `Donation`, `Donor`, `Volunteer`, `Skill`, `EventType`, `Event`, `Beneficiary`, `Distribution` |
| Factory | `DonationFactory` supports online/check/in-kind donation types |
| Decorator | `TaxReceiptDecorator` + `ThankYouReceiptDecorator` on receipts |
| Observer | Donation received and event updated notifications |
| Strategy | Email/SMS/social communication channels, swappable at runtime |
| Singleton | `DatabaseManager`, `ConfigManager` |
| CRUD lookup tables | Skills and event types CRUD via `LookupView` |
| MVC | Controllers + console/JavaFX views separated by layer |
