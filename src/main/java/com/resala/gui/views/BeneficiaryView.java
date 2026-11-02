package com.resala.gui.views;

import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.EmptyState;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Beneficiary;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Beneficiaries view - manage beneficiaries, aid distribution, impact tracking.
 * Note: This is a placeholder - full implementation requires BeneficiaryController.
 */
public class BeneficiaryView extends BaseView {

    private final AppShell appShell;

    private DataTable<Beneficiary> beneficiariesTable;

    public BeneficiaryView(AppShell appShell) {
        super(createToolbar(), createBeneficiariesTable());

        this.appShell = appShell;

        this.beneficiariesTable = (DataTable<Beneficiary>) getTableView().getProperties().get("dataTable");
        if (this.beneficiariesTable == null) {
            this.beneficiariesTable = createBeneficiariesTable();
            getTableView().getProperties().put("dataTable", this.beneficiariesTable);
        }

        this.primaryActionButton = Toolbar.primary("+ Add Beneficiary", e -> showAddBeneficiaryDialog());
    }

    private Button primaryActionButton;

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
            .column("Household", 80, b -> String.valueOf(b.getHouseholdSize()))
            .column("Need Type", 100, b -> "Type #" + b.getNeedTypeId())
            .column("Status", 80, b -> b.getStatus().name())
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void showAddBeneficiaryDialog() {
        var fields = List.of(
            new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true),
            new FormDialog.Field("phone", "Phone", FormDialog.Type.TEXT, false),
            new FormDialog.Field("householdSize", "Household Size", FormDialog.Type.NUMBER, true),
            new FormDialog.Field("needTypeId", "Need Type ID", FormDialog.Type.NUMBER, true),
            new FormDialog.Field("notes", "Notes", FormDialog.Type.TEXTAREA, false)
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Beneficiary", fields);
        result.ifPresent(map -> {
            appShell.getToast().show(appShell.getStage(),
                "Beneficiary management requires BeneficiaryController (not yet implemented).",
                Toast.Type.INFO);
        });
    }

    private void showAidDialog() {
        appShell.getToast().show(appShell.getStage(),
            "Aid distribution requires BeneficiaryController (not yet implemented).",
            Toast.Type.INFO);
    }

    private void showImpactDialog() {
        appShell.getToast().show(appShell.getStage(),
            "Impact tracking requires BeneficiaryController (not yet implemented).",
            Toast.Type.INFO);
    }

    private void deleteSelected() {
        appShell.getToast().show(appShell.getStage(),
            "Delete requires BeneficiaryController (not yet implemented).",
            Toast.Type.INFO);
    }

    @Override
    public void refresh() {
        beneficiariesTable.getTableView().setItems(FXCollections.observableArrayList());
    }
}