package com.resala.gui.views;

import com.resala.controller.DonationController;
import com.resala.donation.DonationFilter;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Donor;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Donors view - manage donor directory.
 */
public class DonorView extends BaseView {

    private final AppShell appShell;
    private final DonationController donationController;

    private final DataTable<Donor> donorsTable;

    @SuppressWarnings("unchecked")
    public DonorView(AppShell appShell, DonationController donationController) {
        super(createToolbar(), createDonorsTable());

        this.appShell = appShell;
        this.donationController = donationController;
        this.donorsTable = (DataTable<Donor>) this.table;

        Button deleteBtn = Toolbar.ghost("Archive Donor", e -> deleteSelected());
        toolbar.getChildren().add(deleteBtn);

        this.primaryActionButton = Toolbar.primary("Add Donor", e -> showAddDonorDialog());
    }

    private Button primaryActionButton;

    @Override
    public Button getPrimaryAction() {
        return primaryActionButton;
    }

    private static HBox createToolbar() {
        return Toolbar.of();
    }

    private static DataTable<Donor> createDonorsTable() {
        return DataTable.<Donor>builder()
            .column("ID", 60, (java.util.function.Function<Donor, String>) d -> String.valueOf(d.getId()))
            .column("Name", 200, Donor::getName)
            .column("Type", 100, d -> d.getType().name())
            .column("Email", 200, d -> d.getEmail() != null ? d.getEmail() : "")
            .column("Phone", 150, d -> d.getPhone() != null ? d.getPhone() : "")
            .column("Status", 80, d -> d.isActive() ? "Active" : "Archived")
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void showAddDonorDialog() {
        var fields = List.of(
            new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true),
            new FormDialog.Field("type", "Type", FormDialog.Type.COMBO, true,
                Donor.Type.INDIVIDUAL.name(), Donor.Type.ORGANIZATION.name()),
            new FormDialog.Field("email", "Email", FormDialog.Type.TEXT, false),
            new FormDialog.Field("phone", "Phone", FormDialog.Type.TEXT, false)
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Donor", fields);
        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                Donor.Type type = Donor.Type.valueOf((String) map.get("type"));
                String email = (String) map.get("email");
                String phone = (String) map.get("phone");

                Donor donor = new Donor();
                donor.setName(name);
                donor.setType(type);
                donor.setEmail(email.isBlank() ? null : email);
                donor.setPhone(phone.isBlank() ? null : phone);
                donor.setActive(true);

                donationController.saveDonor(donor);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Donor added successfully!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void deleteSelected() {
        Donor selected = donorsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a donor first.", Toast.Type.WARNING);
            return;
        }

        List<com.resala.donation.Donation> donations = donationController.findDonations(
            new DonationFilter(selected.getId(), null, null, null));

        if (!donations.isEmpty()) {
            appShell.getToast().show(appShell.getStage(),
                "Cannot delete donor with existing donations. Archive instead.",
                Toast.Type.WARNING);
            return;
        }

        if (appShell.getConfirmDialog().ask(appShell.getStage(),
                "Delete donor '" + selected.getName() + "'?")) {
            donationController.archiveDonor(selected.getId());
            appShell.refreshAll();
            appShell.getToast().show(appShell.getStage(), "Donor archived.", Toast.Type.SUCCESS);
        }
    }

    @Override
    public void refresh() {
        List<Donor> donors = donationController.findDonors("");
        donorsTable.getTableView().setItems(FXCollections.observableArrayList(donors));
    }
}