package com.resala.view.console;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.resala.controller.BeneficiaryController;
import com.resala.beneficiary.Distribution;
import com.resala.beneficiary.policy.ByNeedDistributionPolicy;
import com.resala.beneficiary.policy.EqualDistributionPolicy;
import com.resala.beneficiary.policy.FifoDistributionPolicy;
import com.resala.controller.BeneficiaryController;
import com.resala.model.Beneficiary;
import com.resala.model.NeedType;
import com.resala.view.BeneficiaryView;

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
      System.out.println("7) Distribute aid via Policy (Strategy Pattern)");
      System.out.println("8) Manage Need Types");
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
          case "7" -> distributeWithPolicy();
          case "8" -> manageNeedTypes();
          case "0" -> running = false;
          default  -> System.out.println("Invalid option.");
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      }
      System.out.println();
    }
  }

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

    System.out.println("Current Need Type: " + needTypeName(current.getNeedTypeId()) + " (ID: " + current.getNeedTypeId() + ")");
    printNeedTypesInline();
    String needStr = value("Need Type ID", String.valueOf(current.getNeedTypeId()));
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

  private void distributeWithPolicy() {
    System.out.println("\n--- Batch Aid Distribution (Strategy Pattern) ---");
    System.out.println("Select Distribution Policy:");
    System.out.println("1) By Need (Proportional to household size)");
    System.out.println("2) Equal (Even distribution across active)");
    System.out.println("3) FIFO (First-In, First-Out by registration)");
    System.out.print("Choose policy (1-3, default 1): ");

    String choice = readLine().trim();
    switch (choice) {
      case "2" -> controller.setDistributionPolicy(new EqualDistributionPolicy());
      case "3" -> {
        int quota = readInt("Enter quota per beneficiary (default 1): ");
        controller.setDistributionPolicy(new FifoDistributionPolicy(quota));
      }
      default  -> controller.setDistributionPolicy(new ByNeedDistributionPolicy());
    }

    System.out.println("Active Policy: " + controller.getDistributionPolicy().getName());

    String item = requiredLine("Item / Aid description: ");
    int totalQty = readInt("Total quantity available to distribute: ");

    Map<Beneficiary, Integer> preview = controller.previewDistribution(totalQty);
    if (preview.isEmpty()) {
      System.out.println("No active beneficiaries found to distribute aid to.");
      return;
    }

    System.out.println("\n--- Allocation Preview (" + controller.getDistributionPolicy().getName() + ") ---");
    System.out.println("ID  | Household Size | Name                 | Allocated Qty");
    System.out.println(LINE);
    for (Map.Entry<Beneficiary, Integer> entry : preview.entrySet()) {
      Beneficiary b = entry.getKey();
      System.out.printf("%-3d | %-14d | %-20s | %d%n",
          b.getId(),
          b.getHouseholdSize(),
          truncate(b.getName(), 20),
          entry.getValue()
      );
    }
    System.out.println(LINE);

    String confirm = readLine("Confirm and record this distribution? (yes/no): ");
    if (confirm.equalsIgnoreCase("yes")) {
      String dateStr = readLine("Distribution date (YYYY-MM-DD, Enter for today): ");
      LocalDate date = dateStr.isBlank() ? LocalDate.now() : LocalDate.parse(dateStr);

      String donationIdStr = readLine("In-Kind Donation ID (Enter if none/direct aid): ");
      Long donationId = donationIdStr.isBlank() ? null : Long.parseLong(donationIdStr);

      String notes = blankToNull(readLine("Notes: "));

      List<Distribution> results = controller.distributeByPolicy(item, totalQty, donationId, date, notes);
      printSuccess("Successfully recorded " + results.size() + " distributions using policy: "
          + controller.getDistributionPolicy().getName());
    } else {
      System.out.println("Distribution cancelled.");
    }
  }

  private void manageNeedTypes() {
    boolean inSubMenu = true;
    while (inSubMenu) {
      System.out.println("\n" + LINE);
      System.out.println("Resala Charity - Need Types Management");
      System.out.println(LINE);
      System.out.println("1) List all need types");
      System.out.println("2) Add need type");
      System.out.println("3) Edit need type");
      System.out.println("4) Delete need type");
      System.out.println("0) Back to Beneficiary Module");
      System.out.println(LINE);
      System.out.print("Choose an option: ");

      try {
        switch (readLine().trim()) {
          case "1" -> listNeedTypes();
          case "2" -> addNeedType();
          case "3" -> editNeedType();
          case "4" -> deleteNeedType();
          case "0" -> inSubMenu = false;
          default  -> System.out.println("Invalid option.");
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      }
    }
  }

  private void listNeedTypes() {
    List<NeedType> types = controller.listNeedTypes();
    if (types.isEmpty()) {
      System.out.println("No need types found.");
      return;
    }
    System.out.println("\nID  | Need Type Name");
    System.out.println(LINE);
    for (NeedType nt : types) {
      System.out.printf("%-3d | %s%n", nt.getId(), nt.getName());
    }
  }

  private void addNeedType() {
    System.out.println("\n--- Add Need Type ---");
    String name = requiredLine("Need Type Name (e.g. Housing, Winter Supplies): ");
    NeedType created = controller.addNeedType(name);
    printSuccess("Need Type #" + created.getId() + " (" + created.getName() + ") added.");
  }

  private void editNeedType() {
    System.out.println("\n--- Edit Need Type ---");
    listNeedTypes();
    int id = readInt("Need Type ID to edit: ");
    NeedType existing = controller.getNeedType(id);
    String newName = value("Name", existing.getName());
    NeedType updated = controller.updateNeedType(id, newName);
    printSuccess("Need Type #" + id + " updated to \"" + updated.getName() + "\".");
  }

  private void deleteNeedType() {
    System.out.println("\n--- Delete Need Type ---");
    listNeedTypes();
    int id = readInt("Need Type ID to delete: ");
    NeedType existing = controller.getNeedType(id);
    if (readLine("Delete \"" + existing.getName() + "\"? (yes/no): ").equalsIgnoreCase("yes")) {
      controller.deleteNeedType(id);
      printSuccess("Need Type #" + id + " deleted.");
    } else {
      System.out.println("Delete cancelled.");
    }
  }

  private void printNeedTypesInline() {
    List<NeedType> types = controller.listNeedTypes();
    if (types.isEmpty()) return;
    System.out.print("Available Need Types: ");
    for (int i = 0; i < types.size(); i++) {
      NeedType nt = types.get(i);
      System.out.print(nt.getId() + ") " + nt.getName() + (i < types.size() - 1 ? "  " : "\n"));
    }
  }

  private int readNeedTypeId() {
    List<NeedType> types = controller.listNeedTypes();
    if (types.isEmpty()) {
      System.out.println("Warning: No need types defined in database. Defaulting to 1.");
      return 1;
    }
    printNeedTypesInline();
    int defaultId = types.get(0).getId();
    String choice = readLine("Choose need type ID (default " + defaultId + "): ");
    return choice.isBlank() ? defaultId : Integer.parseInt(choice);
  }

  private String needTypeName(int id) {
    return controller.listNeedTypes().stream()
        .filter(nt -> nt.getId() == id)
        .findFirst()
        .map(NeedType::getName)
        .orElse("Type #" + id);
  }
}
