package com.resala.view.console;

import com.resala.controller.DonationController;
import com.resala.controller.PaymentController;
import com.resala.donation.Donation;
import com.resala.donation.DonationFilter;
import com.resala.donation.DonationRequest;
import com.resala.model.DonationType;
import com.resala.model.Donor;
import com.resala.payment.PaymentMethod;
import com.resala.payment.PaymentOutcome;
import com.resala.payment.PaymentStatus;
import com.resala.view.DonationView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleDonationView extends AbstractConsoleView implements DonationView {

  private final DonationController controller;
  private final PaymentController payments;

  public ConsoleDonationView(DonationController controller, PaymentController payments, Scanner input) {
    super(input);
    this.controller = controller;
    this.payments = payments;
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
      System.out.println("0) Back");
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
          case "0" -> running = false;
          default  -> System.out.println("Invalid option. Please type a number from the menu.");
        }
      } catch (IllegalArgumentException e) {
        printError(e.getMessage());
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
    System.out.println("Optional: email, phone (press Enter to skip).");
    Donor donor = readDonor(null);
    donor = controller.saveDonor(donor);
    printSuccess("Donor saved: #" + donor.getId() + " - " + donor.getName());
  }

  private void findDonors() {
    System.out.println("\n--- Find Donors ---");
    System.out.println("Search by name, phone, or email. Press Enter to show all active donors.");
    String query = readLine("Search: ");
    printDonors(controller.findDonors(query));
  }

  private void editDonor() {
    System.out.println("\n--- Edit Donor ---");
    long id = readLong("Donor ID to edit: ");
    Donor current = controller.getDonor(id);
    System.out.println(
      "Editing: #" + current.getId() + " | " + current.getName() +
      " | " + current.getType() + " | " + current.getPhone()
    );
    System.out.println("Press Enter to keep current value for any field.");
    controller.saveDonor(readDonor(current));
    printSuccess("Donor updated.");
  }

  private void archiveDonor() {
    System.out.println("\n--- Archive Donor ---");
    System.out.println("Archiving removes the donor from active lists but keeps donation history.");
    long id = readLong("Donor ID to archive: ");
    if (readLine("Confirm archive of donor #" + id + "? (yes/no): ").equalsIgnoreCase("yes")) {
      controller.archiveDonor(id);
      printSuccess("Donor #" + id + " archived.");
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
      }
    }

    String notes = readLine("Notes (optional): ");
    String cleanNotes = notes.isBlank() ? null : notes;

    if (type == DonationType.ONLINE) {
      recordOnlineDonation(donorId, amountMinor, cleanNotes);
      return;
    }

    String dateText = readLine("Donation date YYYY-MM-DD [" + LocalDate.now() + "]: ");
    LocalDate donationDate = dateText.isBlank() ? LocalDate.now() : LocalDate.parse(dateText);

    DonationRequest request = new DonationRequest(
      donorId, type, amountMinor, itemDescription, quantity,
      estimatedValueMinor, referenceNumber, donationDate, cleanNotes
    );

    Donation donation = controller.recordDonation(request);
    printSuccess("Donation saved: #" + donation.getId());
    System.out.println("\nReceipt:");
    System.out.println(controller.receipt(donation.getId()));
  }

  private void recordOnlineDonation(long donorId, long amountMinor, String notes) {
    PaymentMethod method = choosePaymentMethod();
    String reference = UUID.randomUUID().toString();
    PaymentOutcome outcome = payments.payDonation(donorId, amountMinor, method, reference, notes);
    while (outcome.status() == PaymentStatus.PENDING && outcome.canRetry()) {
      printError("Payment is pending: " + outcome.message());
      String retry = readLine("Retry with the same payment reference? (y/N): ");
      if (!retry.equalsIgnoreCase("y")) {
        System.out.println("Payment reference: " + reference);
        return;
      }
      outcome = payments.payDonation(donorId, amountMinor, method, reference, notes);
    }
    if (!outcome.isPaid()) {
      printError("Payment failed: " + outcome.message());
      return;
    }
    Long donationId = outcome.referenceId();
    if (donationId == null) {
      printError("Payment succeeded but the donation was not recorded.");
      return;
    }
    printSuccess("Donation saved: #" + donationId);
    System.out.println("\nReceipt:");
    System.out.println(controller.receipt(donationId));
  }

  private PaymentMethod choosePaymentMethod() {
    List<PaymentMethod> methods = payments.availableMethods();
    System.out.println("\nPayment method:");
    for (int i = 0; i < methods.size(); i++) {
      System.out.println("  " + (i + 1) + ") " + methods.get(i).displayName());
    }
    int choice = readInt("Choose a payment method: ");
    if (choice < 1 || choice > methods.size()) {
      throw new IllegalArgumentException("Please choose a valid payment method.");
    }
    return methods.get(choice - 1);
  }

  private void listDonations() {
    System.out.println("\n--- List / Filter Donations ---");
    System.out.println("Leave blank for no filter.");
    String donorText = readLine("Donor ID: ");
    String typeText  = readLine("Type (ONLINE / CHECK / IN_KIND): ");
    String fromText  = readLine("From date YYYY-MM-DD: ");
    String toText    = readLine("To date YYYY-MM-DD: ");

    DonationFilter filter = new DonationFilter(
      donorText.isBlank() ? null : Long.parseLong(donorText),
      typeText.isBlank()  ? null : DonationType.valueOf(typeText.toUpperCase()),
      fromText.isBlank()  ? null : LocalDate.parse(fromText),
      toText.isBlank()    ? null : LocalDate.parse(toText)
    );
    printDonations(controller.findDonations(filter));
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
    printDonors(controller.findDonors(""));
    long id = readLong("Donor ID (or 0 to add a new donor): ");
    if (id != 0) return id;
    Donor newDonor = controller.saveDonor(readDonor(null));
    printSuccess("New donor saved: #" + newDonor.getId());
    return newDonor.getId();
  }

  private DonationType chooseDonationType() {
    System.out.println("\nDonation type:");
    System.out.println("  1) Online   - money received through a channel (reference optional)");
    System.out.println("  2) Check    - money received by check (check number required)");
    System.out.println("  3) In-kind  - items / goods (item description and quantity required)");
    return switch (readLine("Choose 1 / 2 / 3: ")) {
      case "1" -> DonationType.ONLINE;
      case "2" -> DonationType.CHECK;
      case "3" -> DonationType.IN_KIND;
      default  -> throw new IllegalArgumentException("Please choose 1, 2, or 3.");
    };
  }

  private Donor readDonor(Donor current) {
    String name  = value("Donor name",                   current == null ? null : current.getName());
    String type  = value("Type (INDIVIDUAL / ORGANIZATION)", current == null ? null : current.getType().name());
    String email = value("Email address",                current == null ? null : current.getEmail());
    String phone = value("Phone number",                 current == null ? null : current.getPhone());
    return new Donor(
      current == null ? 0 : current.getId(),
      name,
      Donor.Type.valueOf(type.toUpperCase()),
      blankToNull(email),
      blankToNull(phone),
      true
    );
  }

  private void printDonors(List<Donor> donors) {
    if (donors.isEmpty()) { System.out.println("No active donors found."); return; }
    System.out.println("ID  | Name                   | Type          | Phone");
    System.out.println(LINE);
    for (Donor d : donors) {
      System.out.printf("%-3d| %-22s| %-13s| %s%n",
        d.getId(), truncate(d.getName(), 22), d.getType(), d.getPhone());
    }
  }

  private void printDonations(List<Donation> donations) {
    if (donations.isEmpty()) { System.out.println("No donations found."); return; }
    System.out.println("ID  | Date       | Type      | Description / Amount");
    System.out.println(LINE);
    for (Donation d : donations) {
      String val = d.getType() == DonationType.IN_KIND
        ? d.getQuantity() + " x " + d.getItemDescription()
        : formatMoney(d.getAmountMinor());
      System.out.printf("%-3d| %-10s| %-9s| %s%n", d.getId(), d.getDonationDate(), d.getType(), val);
    }
  }

  private Long moneyToMinor(String value) {
    try {
      return new BigDecimal(value).movePointRight(2).longValueExact();
    } catch (ArithmeticException | NumberFormatException e) {
      throw new IllegalArgumentException("Invalid amount. Use numbers like 250.00.");
    }
  }

  private String formatMoney(long minor) {
    return String.format("%.2f EGP", minor / 100.0);
  }
}
