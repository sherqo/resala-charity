package com.resala.gui;

import com.resala.gui.components.DataTable;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public abstract class BaseView extends VBox implements Refreshable {

    protected final HBox toolbar;
    protected final DataTable<?> table;

    protected BaseView(HBox toolbar, DataTable<?> table) {
        this.toolbar = toolbar;
        this.table = table;

        getStyleClass().add("view");

        if (toolbar != null) {
            getChildren().add(toolbar);

            toolbar.visibleProperty().bind(Bindings.isNotEmpty(toolbar.getChildren()));
            toolbar.managedProperty().bind(toolbar.visibleProperty());
        }

        if (table != null) {
            getChildren().add(table.getTableView());
            VBox.setVgrow(table.getTableView(), Priority.ALWAYS);
        }
    }

    public javafx.scene.control.TableView<?> getTableView() {
        return table != null ? table.getTableView() : null;
    }

    @Override
    public abstract void refresh();

    public Button getPrimaryAction() {
        return null;
    }
}
