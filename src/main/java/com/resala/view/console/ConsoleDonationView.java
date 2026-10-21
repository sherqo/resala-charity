package com.resala.view.console;

import com.resala.controller.DonationController;
import com.resala.donation.Donation;
import com.resala.donation.DonationFilter;
import com.resala.donation.DonationRequest;
import com.resala.model.DonationType;
import com.resala.model.Donor;
import com.resala.view.DonationView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

// console implemntation for the UI boundary, this should be enhanced with some abstraction since we will have shared code between difference modules
public class ConsoleDonationView implements DonationView {

  private static final String LINE = "----------------------------------------";

  private final DonationController controller;
  private final Scanner input;

  public ConsoleDonationView(DonationController controller, Scanner input) {
    this.controller = controller;
    this.input = input;
  }

  public void start() {
    printHeader();
    boolean running = true;
    while (running) {
      System.out.println(LINE);
      System.out.println("Resala Charity - Donation Module");
      System.out.println(LINE);
      System.out.println("1) Record a donation");
      System.out.println("2) Add a donor");
      System.out.println("3) Find donors");
      System.out.println("4) Edit a donor");
      System.out.println("5) Archive a donor");
      System.out.println("6) List / filter donations");
      System.out.println("7) Print a receipt");
      System.out.println("8) Yearly donor statement");
      System.out.println("0) Exit");
      System.out.println(LINE);
      System.out.print("Choose an option: ");

      try {
        switch (readLine()) {
          case "1" -> recordDonation();
          case "2" -> addDonor();
          case "3" -> findDonors();
          case "4" -> editDonor();
          case "5" -> archiveDonor();
          case "6" -> listDonations();
          case "7" -> printReceipt();
          case "8" -> printYearlyStatement();
          case "0" -> {
            System.out.println("Goodbye.");
            running = false;
          }
          default -> System.out.println("Invalid option. Please type a number from the menu.");
        }
      } catch (IllegalArgumentException e) {
        System.out.println("Error: " + e.getMessage());
      }
      System.out.println();
    }
  }

  private void printHeader() {
    System.out.println("Welcome to Resala Charity Donation System");
  }

  /* ---------- donor flow ---------- */

  private void addDonor() {
    System.out.println("\n--- Add Donor ---");
    System.out.println("Required: name, type (INDIVIDUAL or ORGANIZATION).");
    System.out.println("Optional: email, phone, tax ID (press Enter to skip).");
    Donor donor = readDonor(null);
    donor = controller.saveDonor(donor);
    System.out.println("Donor saved successfully: #" + donor.getId() + " - " + donor.getName());
  }

  private void findDonors() {
    System.out.println("\n--- Find Donors ---");
    System.out.println("Search by name, phone, or email. Press Enter to show all active donors.");
    String query = readLine("Search: ");
    List<Donor> donors = controller.findDonors(query);
    printDonors(donors);
  }

  private void editDonor() {
    System.out.println("\n--- Edit Donor ---");
    long id = readLong("Donor ID to edit: ");
    Donor current = controller.getDonor(id);
    System.out.println(
      "Editing: #" +
        current.getId() +
        " | " +
        current.getName() +
        " | " +
        current.getType() +
        " | " +
        current.getPhone()
    );
    System.out.println("Press Enter to keep current value for any field.");
    Donor updated = readDonor(current);
    controller.saveDonor(updated);
    System.out.println("Donor updated successfully.");
  }

  private void archiveDonor() {
    System.out.println("\n--- Archive Donor ---");
    System.out.println("Archiving removes the donor from active lists but keeps donation history.");
    long id = readLong("Donor ID to archive: ");
    if (readLine("Confirm archive of donor #" + id + "? (yes/no): ").equalsIgnoreCase("yes")) {
      controller.archiveDonor(id);
      System.out.println("Donor #" + id + " archived.");
    } else {
      System.out.println("Cancelled.");
    }
  }

  /* ---------- donation flow ---------- */

  private void recordDonation() {
    System.out.println("\n--- Record Donation ---");

    long donorId = chooseOrCreateDonor();
    DonationType type = chooseDonationType();

    Long amountMinor = null;
    String itemDescription = null;
    Integer quantity = null;
    Long estimatedValueMinor = null;
    String referenceNumber = null;

    if (type == DonationType.IN_KIND) {
      System.out.println("In-kind donation: item + quantity (+ estimated value).");
      itemDescription = requiredLine("Item description (e.g. blankets): ");
      quantity = readInt("Quantity (e.g. 10): ");
      String value = readLine("Estimated value in EGP (optional, e.g. 50.00): ");
      estimatedValueMinor = value.isBlank() ? null : moneyToMinor(value);
    } else {
      System.out.println("Money donation: amount (in EGP).");
      amountMinor = moneyToMinor(requiredLine("Amount (e.g. 250.00): "));
      if (type == DonationType.CHECK) {
        referenceNumber = requiredLine("Check number: ");
      } else {
        String ref = readLine("Payment reference (press Enter to auto-generate): ");
        referenceNumber = ref.isBlank() ? null : ref;
      }
    }

    String dateText = readLine("Donation date YYYY-MM-DD [" + LocalDate.now() + "]: ");
    LocalDate donationDate = dateText.isBlank() ? LocalDate.now() : LocalDate.parse(dateText);
    String notes = readLine("Notes (optional): ");

    DonationRequest request = new DonationRequest(
      donorId,
      type,
      amountMinor,
      itemDescription,
      quantity,
      estimatedValueMinor,
      referenceNumber,
      donationDate,
      notes.isBlank() ? null : notes
    );

    Donation donation = controller.recordDonation(request);
    System.out.println("Donation saved successfully: #" + donation.getId());
    System.out.println("\nReceipt:");
    System.out.println(controller.receipt(donation.getId()));
  }

  private void listDonations() {
    System.out.println("\n--- List / Filter Donations ---");
    System.out.println("Leave blank for no filter.");
    String donorText = readLine("Donor ID: ");
    String typeText = readLine("Type (ONLINE / CHECK / IN_KIND): ");
    String fromText = readLine("From date YYYY-MM-DD: ");
    String toText = readLine("To date YYYY-MM-DD: ");

    DonationFilter filter = new DonationFilter(
      donorText.isBlank() ? null : Long.parseLong(donorText),
      typeText.isBlank() ? null : DonationType.valueOf(typeText.toUpperCase()),
      fromText.isBlank() ? null : LocalDate.parse(fromText),
      toText.isBlank() ? null : LocalDate.parse(toText)
    );

    List<Donation> donations = controller.findDonations(filter);
    printDonations(donations);
  }

  private void printReceipt() {
    System.out.println("\n--- Print Receipt ---");
    long id = readLong("Donation ID: ");
    System.out.println("\n" + controller.receipt(id));
  }

  private void printYearlyStatement() {
    System.out.println("\n--- Yearly Donor Statement ---");
    long donorId = readLong("Donor ID: ");
    int year = readInt("Year (e.g. 2026): ");
    System.out.println("\n" + controller.yearlyStatement(donorId, year));
  }

  /* ---------- helpers ---------- */

  private long chooseOrCreateDonor() {
    System.out.println("\nSelect donor:");
    List<Donor> donors = controller.findDonors("");
    printDonors(donors);
    long id = readLong("Donor ID (or 0 to add a new donor): ");
    if (id != 0) return id;
    Donor newDonor = controller.saveDonor(readDonor(null));
    System.out.println("New donor saved: #" + newDonor.getId());
    return newDonor.getId();
  }

  private DonationType chooseDonationType() {
    System.out.println("\nDonation type:");
    System.out.println("  1) Online   - money received through a channel (reference optional)");
    System.out.println("  2) Check    - money received by check (check number required)");
    System.out.println("  3) In-kind  - items / goods (item description and quantity required)");
    String choice = readLine("Choose 1 / 2 / 3: ");
    return switch (choice) {
      case "1" -> DonationType.ONLINE;
      case "2" -> DonationType.CHECK;
      case "3" -> DonationType.IN_KIND;
      default -> throw new IllegalArgumentException("Please choose 1, 2, or 3.");
    };
  }

  private Donor readDonor(Donor current) {
    String name = value("Donor name", current == null ? null : current.getName());
    String type = value("Type (INDIVIDUAL / ORGANIZATION)", current == null ? null : current.getType().name());
    String email = value("Email address", current == null ? null : current.getEmail());
    String phone = value("Phone number", current == null ? null : current.getPhone());
    return new Donor(
      current == null ? 0 : current.getId(),
      name,
      com.resala.model.Donor.Type.valueOf(type.toUpperCase()),
      blankToNull(email),
      blankToNull(phone),
      true
    );
  }

  private void printDonors(List<Donor> donors) {
    if (donors.isEmpty()) {
      System.out.println("No active donors found.");
      return;
    }
    System.out.println("ID  | Name                   | Type          | Phone");
    System.out.println(LINE);
    for (Donor d : donors) {
      System.out.printf("%-3d| %-22s| %-13s| %s%n", d.getId(), truncate(d.getName(), 22), d.getType(), d.getPhone());
    }
  }

  private void printDonations(List<Donation> donations) {
    if (donations.isEmpty()) {
      System.out.println("No donations found.");
      return;
    }
    System.out.println("ID  | Date       | Type      | Description / Amount");
    System.out.println(LINE);
    for (Donation d : donations) {
      String value;
      if (d.getType() == DonationType.IN_KIND) {
        value = d.getQuantity() + " x " + d.getItemDescription();
      } else {
        value = formatMoney(d.getAmountMinor());
      }
      System.out.printf("%-3d| %-10s| %-9s| %s%n", d.getId(), d.getDonationDate(), d.getType(), value);
    }
  }

  private String value(String label, String current) {
    String suffix = current == null ? "" : " (current: " + current + ")";
    System.out.print(label + suffix + ": ");
    String entered = input.nextLine().trim();
    return entered.isBlank() && current != null ? current : entered;
  }

  private String requiredLine(String prompt) {
    String value = readLine(prompt);
    if (value.isBlank()) throw new IllegalArgumentException("This field is required.");
    return value;
  }

  private String readLine(String prompt) {
    System.out.print(prompt);
    return input.nextLine().trim();
  }

  private String readLine() {
    return input.nextLine().trim();
  }

  private long readLong(String prompt) {
    return Long.parseLong(requiredLine(prompt));
  }

  private int readInt(String prompt) {
    return Integer.parseInt(requiredLine(prompt));
  }

  private Long moneyToMinor(String value) {
    try {
      return new BigDecimal(value).movePointRight(2).longValueExact();
    } catch (ArithmeticException | NumberFormatException e) {
      throw new IllegalArgumentException("Invalid amount. Use numbers like 250.00.");
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String truncate(String text, int max) {
    return text == null ? "" : text.length() > max ? text.substring(0, max) : text;
  }

  private String formatMoney(long minor) {
    return String.format("%.2f EGP", minor / 100.0);
  }
}
