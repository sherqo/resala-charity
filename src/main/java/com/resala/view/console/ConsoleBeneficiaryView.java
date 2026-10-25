package com.resala.view.console;

import com.resala.controller.BeneficiaryController;
import com.resala.beneficiary.Distribution;
import com.resala.model.Beneficiary;
import com.resala.view.BeneficiaryView;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class ConsoleBeneficiaryView extends AbstractConsoleView implements BeneficiaryView {

  private final BeneficiaryController controller;

  public ConsoleBeneficiaryView(BeneficiaryController controller, Scanner input) {
    super(input);
    this.controller = controller;
  }

  @Override
  public void start() {
    boolean running = true;
    while (running) {
      System.out.println(LINE);
      System.out.println("Resala Charity - Beneficiary Module");
      System.out.println(LINE);
      System.out.println("1) List all beneficiaries");
      System.out.println("2) Add beneficiary");
      System.out.println("3) Edit beneficiary");
      System.out.println("4) Delete beneficiary");
      System.out.println("5) Record distribution");
      System.out.println("6) View beneficiary distribution history");
      System.out.println("0) Back");
      System.out.println(LINE);
      System.out.print("Choose an option: ");

      try {
        switch (readLine().trim()) {
          case "1" -> listBeneficiaries();
          case "2" -> addBeneficiary();
          case "3" -> editBeneficiary();
          case "4" -> deleteBeneficiary();
          case "5" -> recordDistribution();
          case "6" -> viewDistributionHistory();
          case "0" -> running = false;
          default  -> System.out.println("Invalid option.");
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      }
      System.out.println();
    }
  }

  // ── Beneficiary CRUD ───────────────────────────────────────────────────────

  private void listBeneficiaries() {
    List<Beneficiary> list = controller.listBeneficiaries();
    if (list.isEmpty()) {
      System.out.println("No beneficiaries found.");
      return;
    }
    System.out.println("ID  | Status  | Size | Need Type | Name                 | Phone");
    System.out.println(LINE);
    for (Beneficiary b : list) {
      System.out.printf("%-3d | %-7s | %-4d | %-9s | %-20s | %s%n",
        b.getId(),
        b.getStatus(),
        b.getHouseholdSize(),
        needTypeName(b.getNeedTypeId()),
        truncate(b.getName(), 20),
        b.getPhone() == null ? "" : b.getPhone()
      );
    }
  }

  private void addBeneficiary() {
    System.out.println("\n--- Add Beneficiary ---");
    String name = requiredLine("Name: ");
    String phone = blankToNull(readLine("Phone: "));
    int size = readInt("Household size (default 1): ");
    int needTypeId = readNeedTypeId();
    String notes = blankToNull(readLine("Notes: "));

    Beneficiary b = new Beneficiary(0, name, phone, size, needTypeId, Beneficiary.Status.ACTIVE, notes);
    Beneficiary saved = controller.add(b);
    printSuccess("Beneficiary added: #" + saved.getId() + " — " + saved.getName());
  }

  private void editBeneficiary() {
    System.out.println("\n--- Edit Beneficiary ---");
    long id = readLong("Beneficiary ID to edit: ");
    Beneficiary current = controller.getBeneficiary(id);

    System.out.println("Editing #" + current.getId() + " (" + current.getName() + ")");
    System.out.println("Press Enter to keep current value.");

    String name = value("Name", current.getName());
    String phone = blankToNull(value("Phone", current.getPhone()));
    String sizeStr = value("Household size", String.valueOf(current.getHouseholdSize()));
    int size = Integer.parseInt(sizeStr);

    String needStr = value("Need Type ID (1:Food, 2:Clothing, 3:Medical, 4:Education)", String.valueOf(current.getNeedTypeId()));
    int needTypeId = Integer.parseInt(needStr);

    String statusStr = value("Status (ACTIVE/CLOSED)", current.getStatus().name()).toUpperCase();
    Beneficiary.Status status = Beneficiary.Status.valueOf(statusStr);

    String notes = blankToNull(value("Notes", current.getNotes()));

    Beneficiary updated = new Beneficiary(id, name, phone, size, needTypeId, status, notes);
    controller.update(updated);
    printSuccess("Beneficiary #" + id + " updated successfully.");
  }

  private void deleteBeneficiary() {
    System.out.println("\n--- Delete Beneficiary ---");
    long id = readLong("Beneficiary ID to delete: ");
    Beneficiary current = controller.getBeneficiary(id);
    if (readLine("Delete \"" + current.getName() + "\"? (yes/no): ").equalsIgnoreCase("yes")) {
      controller.delete(id);
      printSuccess("Beneficiary #" + id + " deleted.");
    } else {
      System.out.println("Delete cancelled.");
    }
  }

  // ── Distribution Features (B3 & B4) ────────────────────────────────────────

  private void recordDistribution() {
    System.out.println("\n--- Record Aid Distribution ---");
    long beneficiaryId = readLong("Beneficiary ID: ");
    Beneficiary b = controller.getBeneficiary(beneficiaryId);
    System.out.println("Distributing to: " + b.getName() + " [Status: " + b.getStatus() + "]");

    String item = requiredLine("Item / Aid description: ");
    int quantity = readInt("Quantity: ");

    String dateStr = readLine("Distribution date (YYYY-MM-DD, Enter for today): ");
    LocalDate date = dateStr.isBlank() ? LocalDate.now() : LocalDate.parse(dateStr);

    String donationIdStr = readLine("In-Kind Donation ID (Enter if none/direct aid): ");
    Long donationId = donationIdStr.isBlank() ? null : Long.parseLong(donationIdStr);

    String notes = blankToNull(readLine("Notes: "));

    Distribution d = controller.recordDistribution(beneficiaryId, donationId, item, quantity, date, notes);
    printSuccess("Distribution #" + d.getId() + " recorded: " + d.getQuantity() + "x " + d.getItemDescription()
        + " to " + b.getName() + " on " + d.getDistributionDate());
  }

  private void viewDistributionHistory() {
    System.out.println("\n--- Beneficiary Distribution History ---");
    long beneficiaryId = readLong("Beneficiary ID: ");
    Beneficiary b = controller.getBeneficiary(beneficiaryId);
    List<Distribution> history = controller.getDistributionHistory(beneficiaryId);

    if (history.isEmpty()) {
      System.out.println("No distributions recorded for " + b.getName() + ".");
      return;
    }

    System.out.println("History for: " + b.getName());
    System.out.println("ID  | Date       | Qty  | Item Description             | In-Kind Stock ID");
    System.out.println(LINE);
    for (Distribution d : history) {
      System.out.printf("%-3d | %-10s | %-4d | %-28s | %s%n",
        d.getId(),
        d.getDistributionDate(),
        d.getQuantity(),
        truncate(d.getItemDescription(), 28),
        d.getDonationId() == null ? "-" : "#" + d.getDonationId()
      );
    }
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private int readNeedTypeId() {
    System.out.println("Need Types: 1) Food  2) Clothing  3) Medical  4) Education");
    String choice = readLine("Choose need type (1-4, default 1): ");
    return choice.isBlank() ? 1 : Integer.parseInt(choice);
  }

  private String needTypeName(int id) {
    return switch (id) {
      case 1 -> "Food";
      case 2 -> "Clothing";
      case 3 -> "Medical";
      case 4 -> "Education";
      default -> "Type #" + id;
    };
  }
}
