package com.resala.gui.views;

import com.resala.controller.BeneficiaryController;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Beneficiary;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Beneficiaries view - manage beneficiaries, aid distribution, impact tracking.
 */
public class BeneficiaryView extends BaseView {

    private static final Map<Integer, String> NEED_TYPES = Map.of(
        1, "Food",
        2, "Clothing",
        3, "Medical",
        4, "Education"
    );

    private final AppShell appShell;
    private final BeneficiaryController beneficiaryController;
    private final DataTable<Beneficiary> beneficiariesTable;
    private final Button primaryActionButton;

    public BeneficiaryView(AppShell appShell) {
        this(appShell, null);
    }

    @SuppressWarnings("unchecked")
    public BeneficiaryView(AppShell appShell, BeneficiaryController beneficiaryController) {
        super(createToolbar(), createBeneficiariesTable());

        this.appShell = appShell;
        this.beneficiaryController = beneficiaryController;
        this.beneficiariesTable = (DataTable<Beneficiary>) this.table;

        Button aidBtn = Toolbar.ghost("Distribute Aid", e -> showAidDialog());
        Button impactBtn = Toolbar.ghost("Impact Report", e -> showImpactDialog());
        Button deleteBtn = Toolbar.ghost("Delete", e -> deleteSelected());
        toolbar.getChildren().addAll(aidBtn, impactBtn, deleteBtn);

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
            .column("Need Type", 120, b -> NEED_TYPES.getOrDefault(b.getNeedTypeId(), "Type #" + b.getNeedTypeId()))
            .column("Status", 80, b -> b.getStatus().name())
            .column("Impact", 70, b -> String.valueOf(b.getImpactScore()))
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void showAddBeneficiaryDialog() {
        var fields = List.of(
            new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true),
            new FormDialog.Field("phone", "Phone", FormDialog.Type.TEXT, false),
            new FormDialog.Field("householdSize", "Household Size", FormDialog.Type.NUMBER, true),
            new FormDialog.Field("needTypeId", "Need Type", FormDialog.Type.COMBO, true,
                "1: Food", "2: Clothing", "3: Medical", "4: Education"),
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

                if (beneficiaryController != null) {
                    Beneficiary b = new Beneficiary(0, name, phone, householdSize, needTypeId, Beneficiary.Status.ACTIVE, notes);
                    beneficiaryController.add(b);
                    appShell.refreshAll();
                    appShell.getToast().show(appShell.getStage(), "Beneficiary added!", Toast.Type.SUCCESS);
                } else {
                    appShell.getToast().show(appShell.getStage(), "Beneficiary added.", Toast.Type.SUCCESS);
                }
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showAidDialog() {
        appShell.getToast().show(appShell.getStage(),
            "Select a beneficiary and aid item to distribute.",
            Toast.Type.INFO);
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
                if (beneficiaryController != null) {
                    beneficiaryController.setImpactScore(selected.getId(), score);
                    appShell.refreshAll();
                    appShell.getToast().show(appShell.getStage(),
                        "Impact score set to " + score + " for " + selected.getName(), Toast.Type.SUCCESS);
                } else {
                    appShell.getToast().show(appShell.getStage(), "Impact score set.", Toast.Type.SUCCESS);
                }
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
            if (beneficiaryController != null) {
                beneficiaryController.delete(selected.getId());
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Beneficiary deleted.", Toast.Type.SUCCESS);
            }
        }
    }

    @Override
    public void refresh() {
        if (beneficiaryController != null) {
            beneficiariesTable.getTableView().setItems(
                FXCollections.observableArrayList(beneficiaryController.listBeneficiaries()));
        } else {
            beneficiariesTable.getTableView().setItems(FXCollections.observableArrayList());
        }
    }
}
