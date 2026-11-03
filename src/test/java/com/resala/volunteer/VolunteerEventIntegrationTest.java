package com.resala.volunteer;

import static org.junit.jupiter.api.Assertions.*;

import com.resala.controller.EventController;
import com.resala.event.EventError;
import com.resala.event.EventException;
import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.event.EventService;
import com.resala.event.EventTypeService;
import com.resala.model.Event;
import com.resala.model.Skill;
import com.resala.model.Volunteer;
import com.resala.model.VolunteerEventAssignment;
import com.resala.persistence.sqlite.SqliteEventRegistrationRepository;
import com.resala.persistence.sqlite.SqliteEventRepository;
import com.resala.persistence.sqlite.SqliteEventTypeRepository;
import com.resala.persistence.sqlite.SqliteVolunteerRepository;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VolunteerEventIntegrationTest {

  private Connection conn;
  private SqliteVolunteerRepository volunteerRepo;
  private SqliteEventRepository eventRepo;
  private SqliteEventRegistrationRepository registrationRepo;
  private EventService eventService;
  private EventController eventController;
  private VolunteerService volunteerService;

  @BeforeEach
  void setUp() throws Exception {
    conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement st = conn.createStatement()) {
      st.execute("PRAGMA foreign_keys = ON");
    }
    runSchema(conn);

    volunteerRepo = new SqliteVolunteerRepository(conn);
    eventRepo = new SqliteEventRepository(conn);
    registrationRepo = new SqliteEventRegistrationRepository(conn);

    eventService = new EventService(eventRepo, registrationRepo);
    volunteerService = new VolunteerService(volunteerRepo, eventRepo, registrationRepo);
    eventController = new EventController(
      eventService,
      new EventTypeService(new SqliteEventTypeRepository(conn)),
      volunteerService
    );
  }

  @AfterEach
  void tearDown() throws Exception {
    if (conn != null && !conn.isClosed()) {
      conn.close();
    }
  }

  @Test
  void volunteerRegistrationCreatesEventRegistrationForAvailableSkilledVolunteer() {
    // 1. Create an available volunteer
    Volunteer volunteer = new Volunteer("Sarah Ahmed", "sarah@example.com", "0100000000", true);
    volunteer = volunteerRepo.save(volunteer);
    assertTrue(volunteer.isAvailable());

    // 2. Create a skill and add it to the volunteer's profile
    Skill skill = createSkillFor(volunteer, "Packing Aid");

    // 3. Create open event
    Event event = eventService.create(new EventRequest(
      "Ramadan Food Drive",
      1,
      LocalDate.now().plusDays(5),
      50,
      4,
      "Food drive packing"
    ));
    eventService.open(event.getId());

    // 4. Register volunteer to event
    VolunteerEventAssignment assignment = volunteerService.assign(
      volunteer.getId(),
      (int) event.getId(),
      skill.getId()
    );

    assertNotNull(assignment);
    assertEquals(event.getId(), assignment.getEventId());
    assertEquals(skill.getId(), assignment.getSkillId());

    // 5. Verify volunteer remains available
    Volunteer updatedVolunteer = volunteerService.getVolunteer(volunteer.getId());
    assertTrue(updatedVolunteer.isAvailable(), "Volunteer must be available when assigned");

    // 6. Verify the assignment uses a skill the volunteer already has
    List<Skill> volunteerSkills = volunteerService.getVolunteerSkills(volunteer.getId());
    assertEquals(1, volunteerSkills.size());
    assertEquals("Packing Aid", volunteerSkills.get(0).getName());

    // 7. Verify unified EventRegistration was created in event_registrations
    Optional<EventRegistration> regOpt = registrationRepo.findByVolunteerAndEvent(
      volunteer.getId(),
      event.getId()
    );
    assertTrue(regOpt.isPresent(), "Event registration should exist for volunteer");
    EventRegistration reg = regOpt.get();
    assertEquals("Sarah Ahmed", reg.getAttendeeName());
    assertEquals("sarah@example.com", reg.getAttendeeEmail());
    assertNotNull(reg.getTicketRef());
    assertEquals(8, reg.getTicketRef().length());
    assertEquals(volunteer.getId(), reg.getVolunteerId());
    assertEquals(skill.getId(), reg.getSkillId());
    assertFalse(reg.isCheckedIn());

    // 8. Verify event capacity count includes the volunteer
    assertEquals(1, registrationRepo.countRegistered(event.getId()));
  }

  @Test
  void registrationFailsWhenEventCapacityIsFull() {
    // 1. Create an event with capacity = 1
    Event event = eventService.create(new EventRequest(
      "Small Workshop",
      1,
      LocalDate.now().plusDays(2),
      1,
      3,
      "Limited spots"
    ));
    eventService.open(event.getId());

    // 2. Normal attendee registers first
    eventService.register(event.getId(), "Attendee One", "one@example.com");
    assertEquals(1, registrationRepo.countRegistered(event.getId()));

    // 3. Volunteer attempts to register
    Volunteer volunteer = volunteerRepo.save(new Volunteer("Vol One", "vol@example.com", "123", true));
    Skill skill = createSkillFor(volunteer, "Tutoring Kids");

    IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
      volunteerService.assign(volunteer.getId(), (int) event.getId(), skill.getId())
    );
    assertTrue(ex.getMessage().contains("Event is full"));
  }

  @Test
  void checkInCreditsHoursAndUnlocksCertificateAtTwenty() {
    Volunteer volunteer = volunteerRepo.save(
      new Volunteer("Mona Hassan", "mona@example.com", "0100000001", true)
    );
    Skill skill = createSkillFor(volunteer, "Event Support");

    Event event = eventService.create(new EventRequest(
      "Community Day",
      1,
      LocalDate.now().plusDays(3),
      30,
      20,
      "Full-day community event"
    ));
    eventService.open(event.getId());

    VolunteerEventAssignment assignment = volunteerService.assign(
      volunteer.getId(),
      (int) event.getId(),
      skill.getId()
    );

    // Hours are 0 before check-in
    assertEquals(0.0, volunteerService.summary(volunteer.getId()).getTotalHours(),
      "Hours are 0 before volunteer checks in");

    // Retrieve registration and check in
    EventRegistration reg = registrationRepo
      .findByVolunteerAndEvent(volunteer.getId(), event.getId())
      .orElseThrow();
    eventController.checkIn(reg.getId());

    // Hours should now be credited
    assertEquals(20.0, volunteerService.summary(volunteer.getId()).getTotalHours());
    assertEquals(20.0, volunteerService.getAssignment(assignment.getId()).getHours());

    // Registration should be checked in
    EventRegistration refreshed = registrationRepo.findByVolunteerAndEvent(volunteer.getId(), event.getId()).orElseThrow();
    assertTrue(refreshed.isCheckedIn(), "Checking in should mark registration as checked in");

    // Certificate should be printable with 20+ hours
    String certificate = volunteerService.certificate(volunteer.getId());
    assertTrue(certificate.contains("Mona Hassan"));
    assertTrue(certificate.contains("20.0"));
  }

  @Test
  void certificateIsRejectedWhenHoursBelowTwentyAfterCheckIn() {
    Volunteer volunteer = volunteerRepo.save(
      new Volunteer("Ali Mostafa", "ali@example.com", "0100000002", true)
    );
    Skill skill = createSkillFor(volunteer, "Logistics Support");

    Event event = eventService.create(new EventRequest(
      "Half-Day Drive",
      1,
      LocalDate.now().plusDays(2),
      10,
      10,
      "Short event"
    ));
    eventService.open(event.getId());
    volunteerService.assign(volunteer.getId(), (int) event.getId(), skill.getId());

    // Check in to credit hours
    EventRegistration reg = registrationRepo
      .findByVolunteerAndEvent(volunteer.getId(), event.getId())
      .orElseThrow();
    eventController.checkIn(reg.getId());

    // Hours should be 10.0
    assertEquals(10.0, volunteerService.summary(volunteer.getId()).getTotalHours());

    // Certificate should be rejected because below 20
    assertThrows(IllegalStateException.class, () -> volunteerService.certificate(volunteer.getId()));
  }

  @Test
  void assignmentRequiresAvailabilityAndMatchingSkill() {
    Event event = eventService.create(new EventRequest(
      "Skills Workshop",
      1,
      LocalDate.now().plusDays(2),
      10,
      5,
      "Skills-based assignment"
    ));
    eventService.open(event.getId());

    Volunteer unavailable = volunteerRepo.save(
      new Volunteer("Busy Volunteer", "busy@example.com", "111", false)
    );
    Skill firstAid = createSkillFor(unavailable, "First Aid");
    IllegalStateException unavailableEx = assertThrows(IllegalStateException.class, () ->
      volunteerService.assign(unavailable.getId(), (int) event.getId(), firstAid.getId())
    );
    assertTrue(unavailableEx.getMessage().contains("not available"));

    Volunteer available = volunteerRepo.save(
      new Volunteer("Ready Volunteer", "ready@example.com", "222", true)
    );
    Skill untrained = volunteerService.createSkill("Untrained Skill");
    IllegalStateException skillEx = assertThrows(IllegalStateException.class, () ->
      volunteerService.assign(available.getId(), (int) event.getId(), untrained.getId())
    );
    assertTrue(skillEx.getMessage().contains("required skill"));
  }

  @Test
  void eventCanBeReopenedAfterClose() {
    Event event = eventService.create(new EventRequest(
      "Final Event",
      1,
      LocalDate.now().plusDays(1),
      10,
      5,
      "Closed events are final"
    ));
    eventService.open(event.getId());
    eventController.close(event.getId());

    // Event should be reopenable
    eventService.open(event.getId());
    assertEquals(Event.Status.OPEN, eventService.getEvent(event.getId()).getStatus());
  }

  private Skill createSkillFor(Volunteer volunteer, String name) {
    Skill skill = volunteerService.createSkill(name);
    volunteerService.addSkill(volunteer.getId(), skill.getId());
    return skill;
  }

  private void runSchema(Connection conn) throws Exception {
    String script;
    try (InputStream in = getClass().getResourceAsStream("/schema.sql")) {
      script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    for (String sql : script.replaceAll("(?m)--.*$", "").split(";")) {
      if (sql.isBlank()) continue;
      try (Statement st = conn.createStatement()) {
        st.execute(sql.trim());
      }
    }
  }
}
