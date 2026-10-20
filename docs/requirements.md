# CSE336 — Major Assessment Task: Requirements

## Part 1

### Objective

The primary goal of this project is to develop a robust, scalable, and maintainable
software system for a non-profit organization. The system will streamline operations by
managing key tasks such as donations, volunteer tracking, event organization, and
beneficiary communication. By leveraging design patterns, the system will ensure
extensibility, reusability, and long-term maintainability.

### Key Objectives

- **Flexibility:** The system should be adaptable to changing requirements and new
  features without extensive modifications.
- **Maintainability:** The codebase should be well-structured, easy to understand, and
  maintain.
- **Reusability:** Design patterns should be employed to promote code reuse and reduce
  redundancy.

### System Components and Functionality

#### Donation Management

- Track donor information, donation amounts, and donation types.
- Generate donation receipts and tax-related documents.
- Implement a flexible donation system to accommodate various donation methods
  (e.g., online, checks, in-kind).

#### Volunteer Management

- Maintain a database of volunteer information, skills, and availability.
- Assign volunteers to tasks and events based on their preferences and qualifications.
- Track volunteer hours and generate volunteer certificates.

#### Event Management

- Create, manage, and track events (e.g., fundraisers, workshops, outreach programs).
- Handle event registration, ticketing, and attendance tracking.
- Send event reminders and communications to attendees.

#### Beneficiary Management

- Store information about beneficiaries and their needs.
- Track the distribution of resources and services to beneficiaries.
- Implement a system for monitoring and evaluating the impact of programs on
  beneficiaries.

#### Communication Module

- Facilitate communication between the organization, donors, volunteers, and
  beneficiaries.
- Send email notifications, SMS messages, and other forms of communication.
- Integrate with social media platforms for outreach and engagement.

### Design Patterns to be Used

Consider using the following design patterns to achieve the project objectives:

- **Factory Pattern:** To create objects of different types based on configuration or
  user input.
- **Observer Pattern:** To notify interested parties when the state of an object changes
  (e.g., when a new donation is received, or an event is updated).
- **Strategy Pattern:** To define algorithms that can be interchanged at runtime (e.g.,
  different communication strategies for different types of recipients).
- **Template Method Pattern:** To define the skeleton of an algorithm in an operation,
  deferring some steps to subclasses.
- **Facade Pattern:** To provide a unified interface to a set of interfaces in a
  subsystem.
- All other design patterns taught in the lecture.

### Project Deliverables

- **Class Diagrams:** Detailed diagrams showing the classes, attributes, and
  relationships within the system.
- **Implementation Code:** Well-structured and commented code that adheres to design
  patterns and best practices.
- **Client Basic Requirements:** Extending the basic requirements base above by
  collecting some requirements from users.

## Evaluation Criteria

### Phase 1 — Last lecture before the midterm

| Item                | Marks  |
| ------------------- | ------ |
| Class diagram       | 2      |
| Strategy Pattern    | 1      |
| Observer Pattern    | 1      |
| Decorator Pattern   | 1      |
| Factory Pattern     | 0.5    |
| Singleton Pattern   | 0.5    |
| MVC                 | 2      |
| User Control Panel  | 1      |
| Client requirements | 0.5    |
| CRUD lookup tables  | 0.5    |
| **Total**           | **10** |

### Phase 2 — Last lecture before the final exam

| Item                    | Marks  |
| ----------------------- | ------ |
| Command Pattern         | 1      |
| Adapter Pattern         | 1      |
| Façade Pattern          | 1      |
| Template Method Pattern | 1      |
| Iterator Pattern        | 1      |
| State Pattern           | 1      |
| Proxy Pattern           | 1      |
| Admin Pages             | 1      |
| Payment Module          | 2      |
| Core module management  | 2      |
| All system working      | 3      |
| **Total**               | **15** |
