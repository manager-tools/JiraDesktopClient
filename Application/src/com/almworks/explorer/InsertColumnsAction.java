package com.almworks.explorer;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.gui.ArtifactTableColumns;
import com.almworks.util.L;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.AList;
import com.almworks.util.components.ATable;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.components.tables.HierarchicalTable;
import com.almworks.util.i18n.Local;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

class InsertColumnsAction extends ATable.HeaderAction {
  private final HierarchicalTable<?> myTable;
  private final Factory<? extends ArtifactTableColumns.ColumnsSet<?>> myAllColumns;
  private final String myColumnsName;
  private final boolean myAux;

  public InsertColumnsAction(HierarchicalTable<?> table, Factory<? extends ArtifactTableColumns.ColumnsSet<?>> allColumns, String columnsName, boolean aux) {
    super(L.actionName("Insert " + columnsName + "\u2026"), null);
    myTable = table;
    myAllColumns = allColumns;
    myColumnsName = NameMnemonic.parseString(Local.parse(columnsName)).getText();
    myAux = aux;
  }

  protected ATable<?> getTable() {
    return myTable.getTable();
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    CantPerformException.ensureNotNull(getColumnAccessor(context));
    if (myAux) {
      ArtifactTableColumns.ColumnsSet<?> columns = myAllColumns.create();
      if (columns != null) context.updateOnChange(columns.model);
      boolean notEmpty = columns != null && columns.model.getSize() > 0;
      context.setEnabled(notEmpty ? EnableState.ENABLED : EnableState.INVISIBLE);
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    DetachComposite life = new DetachComposite();
    AList<? extends TableColumnAccessor<?, ?>> list = createColumnsListComponent(life);
    JScrollPane content = new JScrollPane(list);

    String configName = "insert" + myColumnsName.replaceAll("\\s", "") + "Columns";
    DialogBuilder builder = context.getSourceObject(DialogManager.ROLE).createBuilder(configName);
    builder.setTitle("Insert " + myColumnsName);
    builder.setInitialFocusOwner(list.getScrollable());
    builder.setContent(content);
    builder.setEmptyCancelAction();
    builder.setEmptyOkAction();

    builder.addOkListener(new DoInsertColumns(getColumn(context), list.getSelectionAccessor()));
    setupCloseOnDoubleClick(list, builder);

    builder.setModal(true);
    builder.showWindow(life);
  }

  private AList<? extends TableColumnAccessor<?, ?>> createColumnsListComponent(DetachComposite life) throws CantPerformException {
    final AList<? extends TableColumnAccessor<?,?>> list = new AList<TableColumnAccessor<?,?>>(getSortedColumnsModel(life));
    list.setCanvasRenderer(TableColumnAccessor.NAME_RENDERER);
    ListSpeedSearch.install(list).setSearchSubstring(myAux);
    list.getSelectionAccessor().ensureSelectionExists();
    return list;
  }

  private AListModel<TableColumnAccessor<?, ?>> getSortedColumnsModel(DetachComposite life) throws CantPerformException {
    ArtifactTableColumns.ColumnsSet<?> columns = myAllColumns.create();
    AListModel allColumns = columns.model;
    CantPerformException.ensureNotNull(columns.model);
    AListModel<TableColumnAccessor<?,?>> availableColumns = FilteringListDecorator.<TableColumnAccessor<?,?>>create(life, allColumns, (Condition)notSelected());
    return allColumns instanceof SortedListDecorator ?
        availableColumns :
        SortedListDecorator.create(life, availableColumns, Util.NN(columns.order, (Comparator)TableColumnAccessor.NAME_ORDER));
  }

  private Condition<? extends TableColumnAccessor<?, ?>> notSelected() {
    final AListModel<TableColumnAccessor<?, ?>> selectedColumns = (AListModel)myTable.getHeaderController().getUserColumnsSubsetModel();
    return new Condition<TableColumnAccessor<?, ?>>() {
      @Override
      public boolean isAccepted(TableColumnAccessor<?, ?> value) {
        return !selectedColumns.contains(value);
      }
    };
  }

  private void setupCloseOnDoubleClick(AList<? extends TableColumnAccessor<?, ?>> list, final DialogBuilder builder) {
    list.getScrollable().addMouseListener(new MouseAdapter(){
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) builder.pressOk();
      }
    });
  }

  private class DoInsertColumns implements AnActionListener {
    final int myInsertionPoint;
    final SelectionAccessor<? extends TableColumnAccessor<?, ?>> mySelectionAccessor;

    public DoInsertColumns(int myInsertionPoint, SelectionAccessor<? extends TableColumnAccessor<?, ?>> selectionAccessor) {
      this.myInsertionPoint = myInsertionPoint;
      mySelectionAccessor = selectionAccessor;
    }

    public void perform(ActionContext context) {
      TableColumnModel visibleModel = myTable.getSwingHeader().getColumnModel();
      Collection<? extends TableColumnAccessor<?, ?>> added = mySelectionAccessor.getSelectedItems();
      int initialSize = visibleModel.getColumnCount();
      addColumnsToEnd(added);
      moveColumnsToInsertionPoint(visibleModel, added, initialSize);
    }

    private void addColumnsToEnd(Collection<? extends TableColumnAccessor<?, ?>> addColumns) {
      SubsetModel<TableColumnAccessor<?,?>> columns = (SubsetModel)myTable.getHeaderController().getUserColumnsSubsetModel();
      for (Iterator<? extends TableColumnAccessor<?, ?>> toAdd = addColumns.iterator(); toAdd.hasNext(); ) {
        TableColumnAccessor<?, ?> addColumn = toAdd.next();
        if (columns.getComplementSet().indexOf(addColumn) >= 0)
          columns.add(addColumn);
        else
          toAdd.remove();
      }
    }

    private void moveColumnsToInsertionPoint(TableColumnModel visibleModel, Collection<? extends TableColumnAccessor<?, ?>> addColumns, int initialSize) {
      int newIndex = getNewIndex(initialSize);
      for (int i = 0; i < addColumns.size(); i++) {
        visibleModel.moveColumn(initialSize + i, newIndex + i + 1);
      }
    }

    private int getNewIndex(int initialSize) {
      int newIndex = myInsertionPoint;
      if (newIndex < 0 || newIndex >= initialSize) newIndex = initialSize - 1;
      return newIndex;
    }
  }
}
