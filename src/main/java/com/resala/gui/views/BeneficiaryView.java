package com.resala.gui.views;

import com.resala.beneficiary.Distribution;
import com.resala.beneficiary.policy.ByNeedDistributionPolicy;
import com.resala.beneficiary.policy.EqualDistributionPolicy;
import com.resala.beneficiary.policy.FifoDistributionPolicy;
import com.resala.controller.BeneficiaryController;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.PreviewDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Beneficiary;
import com.resala.model.NeedType;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BeneficiaryView extends BaseView {

    // Filled on every refresh from the database.
    private static final Map<Integer, String> needTypes = new HashMap<>();

    private final AppShell appShell;
    private final BeneficiaryController beneficiaryController;
    private final DataTable<Beneficiary> beneficiariesTable;
    private final Button primaryActionButton;

    @SuppressWarnings("unchecked")
    public BeneficiaryView(AppShell appShell, BeneficiaryController beneficiaryController) {
        super(createToolbar(), createBeneficiariesTable());

        this.appShell = appShell;
        this.beneficiaryController = beneficiaryController;
        this.beneficiariesTable = (DataTable<Beneficiary>) this.table;

        Button aidBtn = Toolbar.ghost("Distribute Aid", e -> showAidDialog());
        Button historyBtn = Toolbar.ghost("History", e -> showHistoryDialog());
        Button impactBtn = Toolbar.ghost("Impact Report", e -> showImpactDialog());
        Button deleteBtn = Toolbar.ghost("Delete", e -> deleteSelected());
        toolbar.getChildren().addAll(aidBtn, historyBtn, impactBtn, deleteBtn);

        this.primaryActionButton = Toolbar.primary("Add Beneficiary", e -> showAddBeneficiaryDialog());
    }

    @Override
    public Button getPrimaryAction() {
        return primaryActionButton;
    }

    private static HBox createToolbar() {
        return Toolbar.of();
    }

    private static DataTable<Beneficiary> createBeneficiariesTable() {
        return DataTable.<Beneficiary>builder()
            .column("ID", 60, (java.util.function.Function<Beneficiary, String>) b -> String.valueOf(b.getId()))
            .column("Name", 180, Beneficiary::getName)
            .column("Phone", 150, b -> b.getPhone() != null ? b.getPhone() : "")
            .column("Household", 90, b -> String.valueOf(b.getHouseholdSize()))
            .column("Need Type", 120, b -> needTypes.getOrDefault(b.getNeedTypeId(), "Type #" + b.getNeedTypeId()))
            .column("Status", 80, b -> b.getStatus().name())
            .column("Impact", 70, b -> String.valueOf(b.getImpactScore()))
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void showAddBeneficiaryDialog() {
        if (needTypes.isEmpty()) {
            appShell.getToast().show(appShell.getStage(), "No need types found.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true),
            new FormDialog.Field("phone", "Phone", FormDialog.Type.TEXT, false),
            new FormDialog.Field("householdSize", "Household Size", FormDialog.Type.NUMBER, true),
            new FormDialog.Field("needTypeId", "Need Type", FormDialog.Type.COMBO, true,
                comboOptions()),
            new FormDialog.Field("notes", "Notes", FormDialog.Type.TEXTAREA, false)
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Beneficiary", fields);
        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                String phone = (String) map.get("phone");
                int householdSize = Integer.parseInt(map.get("householdSize").toString().trim());
                int needTypeId = Integer.parseInt(((String) map.get("needTypeId")).split(":")[0].trim());
                String notes = (String) map.get("notes");

                Beneficiary b = new Beneficiary(0, name, phone, householdSize, needTypeId, Beneficiary.Status.ACTIVE, notes);
                beneficiaryController.add(b);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Beneficiary added!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private String[] comboOptions() {
        return needTypes.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + ": " + e.getValue())
            .toArray(String[]::new);
    }

    private void showAidDialog() {
        var fields = List.of(
            new FormDialog.Field("item", "Item", FormDialog.Type.TEXT, true),
            new FormDialog.Field("total", "Total Quantity", FormDialog.Type.NUMBER, true),
            new FormDialog.Field("policy", "Policy", FormDialog.Type.COMBO, true,
                "By Need", "Equal", "FIFO"),
            new FormDialog.Field("donationId", "Donation ID (optional)", FormDialog.Type.TEXT, false),
            new FormDialog.Field("date", "Date YYYY-MM-DD (optional)", FormDialog.Type.TEXT, false),
            new FormDialog.Field("notes", "Notes", FormDialog.Type.TEXTAREA, false)
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Distribute Aid", fields);
        result.ifPresent(map -> {
            try {
                String item = (String) map.get("item");
                int total = Integer.parseInt(map.get("total").toString().trim());
                String policy = (String) map.get("policy");
                String donationText = (String) map.get("donationId");
                String dateText = (String) map.get("date");
                String notes = (String) map.get("notes");

                Long donationId = donationText == null || donationText.isBlank()
                    ? null : Long.parseLong(donationText.trim());
                LocalDate date = dateText == null || dateText.isBlank()
                    ? null : LocalDate.parse(dateText.trim());

                switch (policy) {
                    case "Equal" -> beneficiaryController.setDistributionPolicy(new EqualDistributionPolicy());
                    case "FIFO" -> beneficiaryController.setDistributionPolicy(new FifoDistributionPolicy());
                    default -> beneficiaryController.setDistributionPolicy(new ByNeedDistributionPolicy());
                }

                Map<Beneficiary, Integer> preview = beneficiaryController.previewDistribution(total);
                if (preview.isEmpty()) {
                    appShell.getToast().show(appShell.getStage(),
                        "No active beneficiaries to distribute to.", Toast.Type.WARNING);
                    return;
                }

                String previewText = previewText(preview);
                boolean confirmed = appShell.getConfirmDialog().ask(
                    appShell.getStage(),
                    "Confirm Distribution",
                    "Distribute " + total + " x " + item + "?\n\n" + previewText
                );
                if (!confirmed) return;

                List<Distribution> recorded = beneficiaryController.distributeByPolicy(
                    item, total, donationId, date, notes);

                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Created " + recorded.size() + " distribution record(s).", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private String previewText(Map<Beneficiary, Integer> preview) {
        StringBuilder text = new StringBuilder();
        int shown = 0;
        for (Map.Entry<Beneficiary, Integer> entry : preview.entrySet()) {
            if (shown == 15) {
                text.append("...\n");
                break;
            }
            text.append(entry.getKey().getName())
                .append(": ")
                .append(entry.getValue())
                .append("\n");
            shown++;
        }
        return text.toString();
    }

    private void showHistoryDialog() {
        Beneficiary selected = beneficiariesTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a beneficiary first.", Toast.Type.WARNING);
            return;
        }

        List<Distribution> history = beneficiaryController.getDistributionHistory(selected.getId());
        if (history.isEmpty()) {
            PreviewDialog.show(appShell, "Distribution History",
                "No distributions recorded for " + selected.getName() + ".");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("Distributions for ").append(selected.getName()).append("\n\n");
        for (Distribution d : history) {
            text.append("#").append(d.getId())
                .append("  ").append(d.getItemDescription())
                .append(" x").append(d.getQuantity())
                .append("  ").append(d.getDistributionDate())
                .append("\n");
        }
        PreviewDialog.show(appShell, "Distribution History", text.toString());
    }

    private void showImpactDialog() {
        Beneficiary selected = beneficiariesTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a beneficiary first.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("impactScore", "Impact Score (0-10)", FormDialog.Type.NUMBER, true,
                String.valueOf(selected.getImpactScore()))
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Set Impact Score", fields);
        result.ifPresent(map -> {
            try {
                int score = Integer.parseInt(((String) map.get("impactScore")).trim());
                if (score < 0 || score > 10) {
                    throw new IllegalArgumentException("Impact score must be between 0 and 10.");
                }
                beneficiaryController.setImpactScore(selected.getId(), score);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Impact score set to " + score + " for " + selected.getName(), Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void deleteSelected() {
        Beneficiary selected = beneficiariesTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a beneficiary first.", Toast.Type.WARNING);
            return;
        }

        if (appShell.getConfirmDialog().ask(appShell.getStage(), "Delete beneficiary '" + selected.getName() + "'?")) {
            beneficiaryController.delete(selected.getId());
            appShell.refreshAll();
            appShell.getToast().show(appShell.getStage(), "Beneficiary deleted.", Toast.Type.SUCCESS);
        }
    }

    @Override
    public void refresh() {
        needTypes.clear();
        for (NeedType type : beneficiaryController.listNeedTypes()) {
            needTypes.put(type.getId(), type.getName());
        }

        beneficiariesTable.getTableView().setItems(
            FXCollections.observableArrayList(beneficiaryController.listBeneficiaries()));
    }
}
