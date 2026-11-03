package com.resala.volunteer;

import static org.junit.jupiter.api.Assertions.*;

import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.event.EventService;
import com.resala.model.Event;
import com.resala.model.Skill;
import com.resala.model.Volunteer;
import com.resala.model.VolunteerEventAssignment;
import com.resala.persistence.sqlite.SqliteEventRegistrationRepository;
import com.resala.persistence.sqlite.SqliteEventRepository;
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
  }

  @AfterEach
  void tearDown() throws Exception {
    if (conn != null && !conn.isClosed()) {
      conn.close();
    }
  }

  @Test
  void volunteerRegistrationCreatesEventRegistrationAndAssignsSkill() {
    // 1. Create volunteer (without initial skills, unavailable)
    Volunteer volunteer = new Volunteer("Sarah Ahmed", "sarah@example.com", "0100000000", false);
    volunteer = volunteerRepo.save(volunteer);
    assertFalse(volunteer.isAvailable());

    // 2. Create skill
    Skill skill = volunteerService.createSkill("Packing Aid");

    // 3. Create open event
    Event event = eventService.create(new EventRequest(
      "Ramadan Food Drive",
      1,
      LocalDate.now().plusDays(5),
      50,
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

    // 5. Verify volunteer is now available
    Volunteer updatedVolunteer = volunteerService.getVolunteer(volunteer.getId());
    assertTrue(updatedVolunteer.isAvailable(), "Volunteer should automatically be marked available");

    // 6. Verify skill was automatically added to volunteer
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
      "Limited spots"
    ));
    eventService.open(event.getId());

    // 2. Normal attendee registers first
    eventService.register(event.getId(), "Attendee One", "one@example.com");
    assertEquals(1, registrationRepo.countRegistered(event.getId()));

    // 3. Volunteer attempts to register
    Volunteer volunteer = volunteerRepo.save(new Volunteer("Vol One", "vol@example.com", "123", true));
    Skill skill = volunteerService.createSkill("Tutoring Kids");

    IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
      volunteerService.assign(volunteer.getId(), (int) event.getId(), skill.getId())
    );
    assertTrue(ex.getMessage().contains("Event is full"));
  }

  @Test
  void loggingHoursAutomaticallyChecksInEventRegistration() {
    // 1. Create volunteer and skill
    Volunteer volunteer = volunteerRepo.save(new Volunteer("Omar Ali", "omar@example.com", "123", true));
    Skill skill = volunteerService.createSkill("Logistics");

    // 2. Create open event
    Event event = eventService.create(new EventRequest(
      "Charity Marathon",
      1,
      LocalDate.now().plusDays(1),
      20,
      "Annual Marathon"
    ));
    eventService.open(event.getId());

    // 3. Register volunteer
    VolunteerEventAssignment assignment = volunteerService.assign(
      volunteer.getId(),
      (int) event.getId(),
      skill.getId()
    );

    // Initial check-in should be false
    EventRegistration regBefore = registrationRepo
      .findByVolunteerAndEvent(volunteer.getId(), event.getId())
      .orElseThrow();
    assertFalse(regBefore.isCheckedIn());

    // 4. Log hours
    volunteerService.logHours(assignment.getId(), 4);

    // 5. Verify registration is now checked-in
    EventRegistration regAfter = registrationRepo
      .findByVolunteerAndEvent(volunteer.getId(), event.getId())
      .orElseThrow();
    assertTrue(regAfter.isCheckedIn(), "Event registration should be checked in after logging hours");

    // 6. Verify Attendance Report reflects this
    String report = eventService.attendanceReport(event.getId());
    assertTrue(report.contains("Registered : 1 / 20"));
    assertTrue(report.contains("Attended   : 1"));
    assertTrue(report.contains("No-show    : 0"));
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
