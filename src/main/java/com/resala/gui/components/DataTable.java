package com.resala.gui.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DataTable<T> {

    private final TableView<T> tableView;

    private DataTable(TableView<T> tableView) {
        this.tableView = tableView;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public TableView<T> getTableView() {
        return tableView;
    }

    public static class Builder<T> {
        private final List<ColumnSpec<T>> columns = new ArrayList<>();
        private ObservableList<T> items = FXCollections.observableArrayList();
        private Consumer<T> onRowDoubleClick = null;

        public Builder<T> column(String name, int width, Function<T, String> mapper) {
            columns.add(new ColumnSpec<>(name, width, mapper));
            return this;
        }

        public Builder<T> items(ObservableList<T> items) {
            this.items = items;
            return this;
        }

        public Builder<T> onRowDoubleClick(Consumer<T> handler) {
            this.onRowDoubleClick = handler;
            return this;
        }

        public DataTable<T> build() {
            TableView<T> table = new TableView<>(items);
            table.getStyleClass().add("table-view");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

            Label emptyLabel = new Label("No records found");
            emptyLabel.getStyleClass().add("empty-hint");
            table.setPlaceholder(emptyLabel);

            table.setRowFactory(tv -> {
                TableRow<T> row = new TableRow<>();
                if (onRowDoubleClick != null) {
                    row.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2 && !row.isEmpty()) {
                            onRowDoubleClick.accept(row.getItem());
                        }
                    });
                }
                return row;
            });

            for (int i = 0; i < columns.size(); i++) {
                ColumnSpec<T> spec = columns.get(i);
                TableColumn<T, String> col = new TableColumn<>(spec.name);
                col.setPrefWidth(spec.width);
                col.setMinWidth(Math.min(spec.width, 60));

                col.setCellValueFactory(cellData ->
                    new SimpleStringProperty(spec.mapper.apply(cellData.getValue())));

                table.getColumns().add(col);
            }

            table.setFixedCellSize(34);

            return new DataTable<>(table);
        }

        private static class ColumnSpec<T> {
            final String name;
            final int width;
            final Function<T, String> mapper;

            ColumnSpec(String name, int width, Function<T, String> mapper) {
                this.name = name;
                this.width = width;
                this.mapper = mapper;
            }
        }
    }
}
