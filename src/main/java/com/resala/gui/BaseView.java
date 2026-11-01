package com.resala.gui;

import com.resala.gui.components.DataTable;
import com.resala.gui.components.Toolbar;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
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
        getChildren().addAll(toolbar, table.getTableView());
        VBox.setVgrow(table.getTableView(), javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Returns the underlying TableView for advanced customization.
     */
    public javafx.scene.control.TableView<?> getTableView() {
        return table.getTableView();
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
     * Convenience factory method for creating a toolbar with primary action.
     */
    protected static HBox createToolbar(Button... buttons) {
        return Toolbar.of(buttons);
    }
}