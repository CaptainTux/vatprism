package net.marvk.fs.vatsim.map.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

/**
 * https://stackoverflow.com/a/46186195/3000387
 */
public class NoneSelectionModel<T> extends TableView.TableViewSelectionModel<T> {
    public NoneSelectionModel(final TableView<T> tableView) {
        super(tableView);
    }

    @Override
    public ObservableList<Integer> getSelectedIndices() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public ObservableList<T> getSelectedItems() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public void selectIndices(int index, int... indices) {
    }

    @Override
    public void selectAll() {
    }

    @Override
    public void selectFirst() {
    }

    @Override
    public void selectLast() {
    }

    @Override
    public void clearAndSelect(int index) {
    }

    @Override
    public void select(int index) {
    }

    @Override
    public void select(T obj) {
    }

    @Override
    public void clearSelection(int index) {
    }

    @Override
    public void clearSelection() {
    }

    @Override
    public boolean isSelected(int index) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void selectPrevious() {
    }

    @Override
    public void selectNext() {
    }

    @Override
    public ObservableList<TablePosition> getSelectedCells() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public boolean isSelected(final int row, final TableColumn<T, ?> column) {
        return false;
    }

    @Override
    public void select(final int row, final TableColumn<T, ?> column) {
    }

    @Override
    public void clearAndSelect(final int row, final TableColumn<T, ?> column) {
    }

    @Override
    public void clearSelection(final int row, final TableColumn<T, ?> column) {
    }

    @Override
    public void selectLeftCell() {
    }

    @Override
    public void selectRightCell() {
    }

    @Override
    public void selectAboveCell() {
    }

    @Override
    public void selectBelowCell() {
    }
}
