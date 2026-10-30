package com.resala.view.console;

import com.resala.controller.VolunteerController;
import com.resala.model.Event;
import com.resala.model.Skill;
import com.resala.view.VolunteerView;
import com.resala.volunteer.VolunteerAssignment;
import com.resala.volunteer.Volunteer;
import com.resala.volunteer.VolunteerSummary;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class ConsoleVolunteerView extends AbstractConsoleView implements VolunteerView {

  private final VolunteerController controller;

  public ConsoleVolunteerView(VolunteerController controller, Scanner input) {
    super(input);
    this.controller = controller;
  }

  @Override
  public void start() {
    boolean running = true;
    while (running) {
      System.out.println(LINE);
      System.out.println("Resala Charity - Volunteer Module");
      System.out.println(LINE);
      System.out.println("1) Volunteer Management");
      System.out.println("2) Volunteer Event Assignment");
      System.out.println("3) Volunteer Skills Management");
      System.out.println("0) Main Menu");
      System.out.println(LINE);
      System.out.print("Choose an option: ");

      try {
        switch (readLine().toUpperCase()) {
          case "1" -> volunteerList();
          case "2" -> assignList();
          case "3" -> skillsList();
          case "0" -> running = false;
          default  -> System.out.println("Invalid option.");
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      }
      System.out.println();
    }
  }

  // In main menu

  private void volunteerList(){
    boolean running = true;
    while (running) {
      System.out.println(LINE);
      System.out.println("Resala Charity - Volunteer Module");
      System.out.println("Volunteers Management");
      System.out.println(LINE);
      System.out.println("1) List all volunteers");
      System.out.println("2) Create volunteer");
      System.out.println("3) Edit volunteer");
      System.out.println("4) Delete volunteer");
      System.out.println("5) Set availability");
      System.out.println("6) List available volunteers");
      System.out.println("7) Log hours");
      System.out.println("8) Hours summary");
      System.out.println("9) Generate certificate");
      System.out.println("0) Back");
      System.out.println(LINE);
      System.out.print("Choose an option: ");

      try {
        switch (readLine().toUpperCase()) {
          case "1" -> listVolunteers();
          case "2" -> createVolunteer();
          case "3" -> editVolunteer();
          case "4" -> deleteVolunteer();
          case "5" -> setAvailability();
          case "6" -> listAvailableVolunteers();
          case "7" -> logHours();
          case "8" -> showSummary();
          case "9" -> showCertificate();
          case "0" -> running = false;
          default  -> System.out.println("Invalid option.");
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      }
      System.out.println();
    }
  }

  private void assignList(){
    boolean running = true;
    while (running) {
      System.out.println(LINE);
      System.out.println("Resala Charity - Volunteer Module");
      System.out.println("Volunteer Event Assignment");
      System.out.println(LINE);
      System.out.println("1) Assign volunteer to event");
      System.out.println("2) View assignments");
      System.out.println("0) Back");
      System.out.println(LINE);
      System.out.print("Choose an option: ");

      try {
        switch (readLine().toUpperCase()) {
          case "1" -> assignVolunteer();
          case "2" -> viewAssignments();
          case "0" -> running = false;
          default  -> System.out.println("Invalid option.");
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      }
      System.out.println();
    }
  }

  private void skillsList(){
    boolean running = true;
    while (running) {
      System.out.println(LINE);
      System.out.println("Resala Charity - Volunteer Module");
      System.out.println("Volunteer Skills Management");
      System.out.println(LINE);
      System.out.println("1) Show volunteer skills");
      System.out.println("2) Add skill to volunteer");
      System.out.println("3) Remove skill from volunteer");
      System.out.println("4) Search volunteers by skill");
      System.out.println("0) Back");
      System.out.println(LINE);
      System.out.print("Choose an option: ");

      try {
        switch (readLine().toUpperCase()) {
          case "1" -> addSkill();
          case "2" -> removeSkill();
          case "3" -> searchBySkill();
          case "4" -> showVolunteerSkills();
          case "0" -> running = false;
          default  -> System.out.println("Invalid option.");
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      }
      System.out.println();
    }
  }


  // ── Volunteer CRUD ─────────────────────────────────────────────────────────

  private void listVolunteers() {
    printVolunteers(controller.listVolunteers());
  }

  private void createVolunteer() {
    System.out.println("\n--- Create Volunteer ---");
    com.resala.model.Volunteer volunteer = controller.create(readVolunteerRequest(null));
    printSuccess("Volunteer created: #" + volunteer.getId() + " - " + volunteer.getName());
  }

  private void editVolunteer() {
    System.out.println("\n--- Edit Volunteer ---");
    long id = readLong("Volunteer ID to edit: ");
    com.resala.model.Volunteer current = controller.getVolunteer(id);
    System.out.println("Editing: #" + current.getId() + " - " + current.getName());
    System.out.println("Press Enter to keep the current value.");
    com.resala.model.Volunteer updated = controller.update(id, readVolunteerRequest(current));
    printSuccess("Volunteer #" + updated.getId() + " updated.");
  }

  private void deleteVolunteer() {
    System.out.println("\n--- Delete Volunteer ---");
    long id = readLong("Volunteer ID to delete: ");
    com.resala.model.Volunteer volunteer = controller.getVolunteer(id);
    if (readLine("Delete \"" + volunteer.getName() + "\"? (yes/no): ").equalsIgnoreCase("yes")) {
      controller.delete(id);
      printSuccess("Volunteer #" + id + " deleted.");
    } else {
      System.out.println("Cancelled.");
    }
  }

  private void setAvailability() {
    System.out.println("\n--- Set Availability ---");
    long id = readLong("Volunteer ID: ");
    com.resala.model.Volunteer volunteer = controller.getVolunteer(id);
    boolean available = readYesNo("Available? (yes/no)", volunteer.isAvailable());
    controller.setAvailability(id, available);
    printSuccess("Availability updated to " + (available ? "available" : "not available") + ".");
  }

  // ── Skills ─────────────────────────────────────────────────────────────────

  private void showVolunteerSkills() {
    System.out.println("\n--- Volunteer Skills ---");
    long id = readLong("Volunteer ID: ");
    com.resala.model.Volunteer volunteer = controller.getVolunteer(id);
    List<Skill> skills = controller.getVolunteerSkills(id);
    System.out.println(volunteer.getName() + ":");
    printSkills(skills);
  }

  private void addSkill() {
    System.out.println("\n--- Add Skill ---");
    long id = readLong("Volunteer ID: ");
    printSkills(controller.listSkills());
    int skillId = readInt("Skill ID to add: ");
    controller.addSkill(id, skillId);
    printSuccess("Skill added.");
  }

  private void removeSkill() {
    System.out.println("\n--- Remove Skill ---");
    long id = readLong("Volunteer ID: ");
    List<Skill> skills = controller.getVolunteerSkills(id);
    if (skills.isEmpty()) {
      System.out.println("This volunteer has no skills.");
      return;
    }
    printSkills(skills);
    int skillId = readInt("Skill ID to remove: ");
    controller.removeSkill(id, skillId);
    printSuccess("Skill removed.");
  }

  private void searchBySkill() {
    System.out.println("\n--- Search by Skill ---");
    printSkills(controller.listSkills());
    int skillId = readInt("Skill ID: ");
    printVolunteers(controller.searchBySkill(skillId));
  }

  private void listAvailableVolunteers() {
    System.out.println("\n--- Available Volunteers ---");
    printVolunteers(controller.listAvailableVolunteers());
  }

  // ── Assignments and hours ─────────────────────────────────────────────────

  private void assignVolunteer() {
    System.out.println("\n--- Assign Volunteer to Event ---");
    printVolunteers(controller.listVolunteers());
    long volunteerId = readLong("Volunteer ID: ");

    printEvents(controller.listEvents());
    long eventId = readLong("Event ID: ");

    printSkills(controller.listSkills());
    int skillId = readInt("Skill the volunteer will use: ");

    VolunteerAssignment assignment = controller.assign(volunteerId, eventId, skillId);
    printSuccess(
      "Assigned. Assignment ID: #" + assignment.getId() +
      " | " + assignment.getEventTitle() +
      " | " + assignment.getWorkDate()
    );
  }

  private void viewAssignments() {
    System.out.println("\n--- View Assignments ---");
    long volunteerId = readLong("Volunteer ID: ");
    printAssignments(controller.listAssignments(volunteerId));
  }

  private void logHours() {
    System.out.println("\n--- Log Hours ---");
    long volunteerId = readLong("Volunteer ID: ");
    List<VolunteerAssignment> assignments = controller.listAssignments(volunteerId);
    printAssignments(assignments);
    if (assignments.isEmpty()) return;

    long assignmentId = readLong("Assignment ID: ");
    double hours = readDouble("Hours worked: ");
    VolunteerAssignment assignment = controller.logHours(assignmentId, hours);
    printSuccess("Logged " + formatHours(assignment.getHours()) + " hours.");
  }

  private void showSummary() {
    System.out.println("\n--- Hours Summary ---");
    long volunteerId = readLong("Volunteer ID: ");
    com.resala.model.Volunteer volunteer = controller.getVolunteer(volunteerId);
    VolunteerSummary summary = controller.summary(volunteerId);

    System.out.println("Volunteer : " + volunteer.getName());
    System.out.println("Total     : " + formatHours(summary.getTotalHours()) + " hours");
    System.out.println("\nPer event:");
    printTotals(summary.getHoursByEvent());
    System.out.println("\nPer month:");
    printTotals(summary.getHoursByMonth());
  }

  private void showCertificate() {
    System.out.println("\n--- Certificate ---");
    long volunteerId = readLong("Volunteer ID: ");
    System.out.println("\n" + controller.certificate(volunteerId));
  }

  // ── Printing helpers ───────────────────────────────────────────────────────

  private void printVolunteers(List<com.resala.model.Volunteer> volunteers) {
    if (volunteers.isEmpty()) {
      System.out.println("No volunteers found.");
      return;
    }
    System.out.println("ID  | Available | Name                 | Phone        | Email");
    System.out.println(LINE);
    for (com.resala.model.Volunteer volunteer : volunteers) {
      System.out.printf(
        "%-3d | %-9s | %-20s | %-12s | %s%n",
        volunteer.getId(),
        volunteer.isAvailable() ? "Yes" : "No",
        truncate(volunteer.getName(), 20),
        truncate(volunteer.getPhone(), 12),
        truncate(volunteer.getEmail(), 30)
      );
    }
  }

  private void printSkills(List<Skill> skills) {
    if (skills.isEmpty()) {
      System.out.println("No skills found.");
      return;
    }
    System.out.println("ID  | Skill");
    System.out.println(LINE);
    for (Skill skill : skills) {
      System.out.printf("%-3d | %s%n", skill.getId(), skill.getName());
    }
  }

  private void printEvents(List<Event> events) {
    if (events.isEmpty()) {
      System.out.println("No events found.");
      return;
    }
    System.out.println("ID  | Status  | Date       | Title");
    System.out.println(LINE);
    for (Event event : events) {
      System.out.printf(
        "%-3d | %-7s | %-10s | %s%n",
        event.getId(),
        event.getStatus(),
        event.getDate(),
        event.getTitle()
      );
    }
  }

  private void printAssignments(List<VolunteerAssignment> assignments) {
    if (assignments.isEmpty()) {
      System.out.println("No assignments found.");
      return;
    }
    System.out.println("ID  | Date       | Hours | Skill            | Event");
    System.out.println(LINE);
    for (VolunteerAssignment assignment : assignments) {
      System.out.printf(
        "%-3d | %-10s | %-5s | %-16s | %s%n",
        assignment.getId(),
        assignment.getWorkDate(),
        formatHours(assignment.getHours()),
        truncate(assignment.getSkillName(), 16),
        assignment.getEventTitle()
      );
    }
  }

  private void printTotals(Map<String, Double> totals) {
    if (totals.isEmpty()) {
      System.out.println("  (no logged hours)");
      return;
    }
    for (Map.Entry<String, Double> entry : totals.entrySet()) {
      System.out.println("  " + entry.getKey() + ": " + formatHours(entry.getValue()));
    }
  }

  // ── Input helpers ──────────────────────────────────────────────────────────

  private Volunteer readVolunteerRequest(com.resala.model.Volunteer current) {
    String name = value("Name", current == null ? null : current.getName());
    String email = value("Email", current == null ? null : current.getEmail());
    String phone = value("Phone", current == null ? null : current.getPhone());
    boolean available = readYesNo(
      "Available? (y/n) default: y",
      current == null || current.isAvailable()
    );
    return new Volunteer(name, email, phone, available);
  }

  private boolean readYesNo(String prompt, boolean current) {
    String answer = readLine(prompt + " [" + (current ? "y" : "n") + "]: ");
    if (answer.isBlank()) return current;
    if (answer.equalsIgnoreCase("y")) return true;
    if (answer.equalsIgnoreCase("n")) return false;
    throw new IllegalArgumentException("Please answer y or n");
  }

  private double readDouble(String prompt) {
    return Double.parseDouble(requiredLine(prompt));
  }

  private String formatHours(double hours) {
    return String.format(Locale.US, "%.1f", hours);
  }
}
