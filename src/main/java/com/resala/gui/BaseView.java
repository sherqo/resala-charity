package com.resala.gui;

import com.resala.gui.components.DataTable;
import com.resala.gui.components.Toolbar;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Abstract base class for all data-driven views.
 * Provides common toolbar, table, and data access.
 */
public abstract class BaseView extends VBox implements Refreshable {

    protected final HBox toolbar;
    protected final DataTable<?> table;

    protected BaseView(HBox toolbar, DataTable<?> table) {
        this.toolbar = toolbar;
        this.table = table;

        getStyleClass().add("view");

        if (toolbar != null) {
            getChildren().add(toolbar);
            // Collapse empty toolbar to avoid dead padding
            toolbar.visibleProperty().bind(Bindings.isNotEmpty(toolbar.getChildren()));
            toolbar.managedProperty().bind(toolbar.visibleProperty());
        }

        if (table != null) {
            getChildren().add(table.getTableView());
            VBox.setVgrow(table.getTableView(), Priority.ALWAYS);
        }
    }

    /**
     * Returns the underlying TableView for advanced customization.
     */
    public javafx.scene.control.TableView<?> getTableView() {
        return table != null ? table.getTableView() : null;
    }

    /**
     * Returns the underlying DataTable instance.
     */
    @SuppressWarnings("unchecked")
    public <T> DataTable<T> getDataTable() {
        return (DataTable<T>) table;
    }

    /**
     * Refreshes the view's data. Must be implemented by subclasses.
     */
    @Override
    public abstract void refresh();

    /**
     * Returns the primary action button for the header bar, or null if none.
     */
    public Button getPrimaryAction() {
        return null;
    }

    /**
     * Convenience factory method for creating a toolbar with action buttons.
     */
    protected static HBox createToolbar(Button... buttons) {
        return Toolbar.of(buttons);
    }
}
