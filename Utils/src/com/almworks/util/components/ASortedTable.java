package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.tables.SortingTableHeaderController;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.dnd.ContextTransfer;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * @author : Dyoma
 */
public class ASortedTable <T> extends NotScrollableScrollPane implements FlatCollectionComponent<T>, Highlightable{
  private final ATable<T> myTable = new ATable<T>(new ASortedTableModel.SortedTableModelAdapter(), TableDropHintProvider.DEFAULT);
  private final SortingTableHeaderController<T> myHeaderController;

  public ASortedTable() {
    super();
    ActionContext.ACTUAL_COMPONENT.putClientValue(this, ActionContext.ACTUAL_COMPONENT.getClientValue(myTable));
    myHeaderController = SortingTableHeaderController.create(myTable);
    setViewportView(myTable);
    setColumnHeaderView(myTable.getSwingHeader());
    getCollectionModel().addListener(new AListModel.Adapter() {
      public void onListRearranged(AListModel.AListEvent event) {
        int index = getSelectionAccessor().getSelectedIndex();
        Rectangle viewRect = getViewport().getViewRect();
        Rectangle rowRect = myTable.getScrollable().getCellRect(index, -1, true);
        if (viewRect.y <= rowRect.y && viewRect.getMaxY() >= rowRect.getMaxY())
          return;
        Point viewPosition = getViewport().getViewPosition();
        viewPosition.y = rowRect.y;
        getViewport().setViewPosition(viewPosition);
      }
    });
  }

  public void setStriped(boolean striped) {
    myTable.setStriped(striped);
  }

  public void setColumnLinesPainted(boolean painted) {
    myTable.setColumnLinesPainted(painted);
  }

  public void setColumnBackgroundsPainted(boolean painted) {
    myTable.setColumnBackgroundsPainted(painted);
  }

  public void setDataModel(AListModel<? extends T> model) {
    myTable.setDataModel(model);
  }

  public void setColumnModel(AListModel<TableColumnAccessor<T, ?>> columns) {
    myTable.setColumnModel(columns);
  }

  public SortingTableHeaderController getHeaderController() {
    return myHeaderController;
  }

  public SubsetEditor<?> createColumnsEditor() {
    SubsetEditor<TableColumnAccessor<T, ?>> editor = SubsetEditor.create(myHeaderController.getUserColumnsSubsetModel(),
      (Comparator) TableColumnAccessor.NAME_ORDER, "&Selected Columns:", "A&vailable Columns:", false);
    editor.setCanvasRenderer(TableColumnAccessor.NAME_RENDERER);
    return editor;
  }

  public TableColumnModel getTableColumnModel() {
    return myTable.getTableColumnModel();
  }

  public void setTopRightComponent(JComponent component) {
    setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
    setCorner(JScrollPane.UPPER_RIGHT_CORNER, component);
  }

  public void setLeftComponent(JComponent component) {
    setRowHeaderView(component);
  }

  public void setTopLeftComponent(JComponent component) {
    setCorner(JScrollPane.UPPER_LEFT_CORNER, component);
  }

  public ListSelectionModel getSelectionModel() {
    return myTable.getSelectionModel();
  }

  public AListModel<? extends T> getCollectionModel() {
    return myTable.getDataModel();
  }

  public Detach setCollectionModel(AListModel<? extends T> model) {
    return myTable.setDataModel(model);
  }

  public int getElementIndexAt(int x, int y) {
    return myTable.getElementIndexAt(x - myTable.getX(), y - myTable.getY());
  }

  public T getElementAt(Point point) {
    return AComponentUtil.getElementAtPoint(this, point);
  }

  public int getScrollingElementAt(int x, int y) {
    return myTable.getElementIndexAt(x, y);
  }

  public Rectangle getElementRect(int elementIndex) {
    return myTable.getElementRect(elementIndex);
  }

  @NotNull
  public SelectionAccessor<T> getSelectionAccessor() {
    return myTable.getSelectionAccessor();
  }

  public void setTransfer(ContextTransfer transfer) {
    myTable.setTransfer(transfer);
  }

  public JComponent toComponent() {
    return this;
  }

  public ATable<T> getTable() {
    return myTable;
  }

  public JComponent getSwingComponent() {
    return myTable.getSwingComponent();
  }

  public void scrollSelectionToView() {
    myTable.scrollSelectionToView();
  }

  public void setDataRoles(DataRole ... roles) {
    myTable.setDataRoles(roles);
  }

  public void addGlobalRoles(DataRole<?>... roles) {
    myTable.addGlobalRoles(roles);
  }

  public void setGridHidden() {
    JTable jtable = (JTable) getSwingComponent();
    jtable.setShowVerticalLines(false);
    jtable.setShowHorizontalLines(false);
    jtable.setIntercellSpacing(new Dimension(0, 0));
  }

  public void addTooltipProvider(TableTooltipProvider provider) {
    myTable.addTooltipProvider(provider);
  }

  public void removeTooltipProvider(TableTooltipProvider provider) {
    myTable.removeTooltipProvider(provider);
  }

  public void forcePreferredColumnWidths() {
    myTable.forcePreferredColumnWidths();
  }

  public void setHighlightPattern(Pattern pattern) {
    myTable.setHighlightPattern(pattern);
  }

  public void adjustSize(int prefCols, int prefRows, int minCols, int minRows) {
    final JComponent c = getSwingComponent();
    setPreferredSize(calcSize(c, prefCols, prefRows));
    setMinimumSize(calcSize(c, minCols, minRows));
  }

  private Dimension calcSize(JComponent comp, int cols, int rows) {
    if(comp instanceof JTable) {
      final JTable table = (JTable) comp;
      final int width = UIUtil.getColumnWidth(table) * cols;
      final int height = table.getTableHeader().getPreferredSize().height + rows * table.getRowHeight();
      return new Dimension(width, height);
    } else {
      return UIUtil.getRelativeDimension(comp, cols, rows + 1);
    }
  }
}
