package com.almworks.actions.order;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.actions.UploadOnSuccess;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.order.Order;
import com.almworks.api.application.order.ReorderItem;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.edit.EditLifecycle;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.gui.WindowController;
import com.almworks.explorer.*;
import com.almworks.gui.ArtifactTableColumns;
import com.almworks.gui.ConfigureColumnsAction;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.IntArray;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.DelegatingCellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.models.ColumnAccessor;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.properties.PropertyChangeListener;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.DndUtil;
import com.almworks.util.ui.actions.dnd.DragContext;
import com.almworks.util.ui.actions.dnd.TransferAction;
import com.almworks.util.ui.actions.globals.GlobalDataRoot;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static com.almworks.util.ui.DialogsUtil.*;

/**
 * @author dyoma
 */
class ReorderWindow implements UIComponentWrapper2, PropertyChangeListener<Order> {
  static final DataRole<ReorderWindow> ROLE = DataRole.createRole(ReorderWindow.class);

  private static final String TABLE_COLUMNS = "tableColumns";
  private static final BaseItemContextTransfer TRANSFER = new ReorderTransfer();
  private static final AnAction SAVE_ACTION = ItemActionUtils.setupSaveAction(new CommitAction(false));
  private static final AnAction COMMIT_ACTION = ItemActionUtils.setupCommitAction(new CommitAction(true));
  private static final AnAction REMOVE_ARTIFACTS_ACTION = new RemoveAction();
  private static final AnAction RESET_ORDER_ACTION = new ClearChangesAction();

  private static final List<KeyStroke> ourStrokes = Collections15.arrayList();

  private final JPanel myWholePanel = new JPanel(new BorderLayout());
  private final ItemViewer myViewer;
  private final DetachComposite myLife = new DetachComposite();
  private final OrderListModel<ReorderItem> myDataModel = OrderListModel.create();
  private final OrderSelector myOrderSelector;
  private final DetachComposite myColumnModelLife = new DetachComposite();
  private final ATable<ReorderItem> myTable = ATable.create(TableDropHintProvider.INSERTION);
  private final UndoHistory myUndoHistory = new UndoHistory(myTable, myDataModel);
  private final OrderColumnColumn myOrderColumn;
  private final EditLifecycle myEditLifecycle;

  public ReorderWindow(Collection<? extends LoadedItem> items, OrderSelector orderSelector,
    ArtifactTableColumns<LoadedItem> columns, Configuration config, ComponentContainer container, EditLifecycle lifecycle)
  {
    myOrderSelector = orderSelector;
    myOrderSelector.addChangeListener(Lifespan.FOREVER, OrderSelector.ORDER, this);
    myEditLifecycle = lifecycle;
    myTable.setDataModel(myDataModel);
    myTable.setStriped(true);
    myTable.setColumnBackgroundsPainted(true);
    myTable.setGridHidden();
    myTable.addGlobalRoles(ItemWrapper.ITEM_WRAPPER, ReorderItem.REORDER_ITEM);
    myTable.setTransfer(TRANSFER);
    myOrderColumn = new OrderColumnColumn(getCurrentOrder().getColumn());
    SubsetModel<? extends TableColumnAccessor<LoadedItem, ?>> columnSubset =
      SubsetModel.create(myLife, columns.getAll(), false);
    myTable.setColumnModel(SegmentedListModel.<TableColumnAccessor<? super ReorderItem, ?>>create(myColumnModelLife,
      FixedListModel.create(myOrderColumn), columnSubset));
    updateCurrentOrder(Collections15.arrayList(items));
    getArtifactsSelection().ensureSelectionExists();
    ColumnsCollector columnsCollector = container.getActor(ColumnsCollector.ROLE);
    assert columnsCollector != null;
    Configuration columnConfig = getColumnConfig(config, columnsCollector);
    columnsCollector.configureColumns(myLife, myTable, columnSubset, columnConfig);
    myViewer = ItemViewer.create(config.getOrCreateSubset("viewer"), getArtifactsSelection()).getViewer();
    ConfiguredSplitPane splitPane = ConfiguredSplitPane.createTopBottomJumping(
      createTableScrollPane(myTable, columns, columnSubset, config), myViewer.getComponent(), config.getOrCreateSubset("splitter"), 0.7f,
      myTable, false);
    Aqua.makeLeopardStyleSplitPane(splitPane);
    Aero.makeBorderedDividerSplitPane(splitPane);
    myWholePanel.add(createToolbar(myTable.getSwingComponent(), splitPane), BorderLayout.NORTH);
    myWholePanel.add(splitPane, BorderLayout.CENTER);

    ConstProvider.addRoleValue(myWholePanel, ROLE, this);
    ConstProvider.addRoleValue(myWholePanel, UndoHistory.ROLE, myUndoHistory);
    GlobalDataRoot.install(myWholePanel);
    UIUtil.keepSelectionOnRemove(myTable);
    addPopupMenu();
  }

  private void addPopupMenu() {
    MenuBuilder builder = new MenuBuilder();
    builder
      .addAction(MainMenu.Search.OPEN_ITEM_IN_BROWSER)
      .addSeparator()
      .addAction(MainMenu.Edit.COPY_ID_SUMMARY)
      .addAction(MainMenu.Edit.CUSTOM_COPY);
    if (!ExplorerForm.DISABLE_CLIPBOARD_IN_POPUPS) builder.addAction(TransferAction.COPY);
    builder.addToComponent(myLife, myTable.getSwingComponent());
  }

  private void remove(List<ItemWrapper> items) {
    myUndoHistory.addRemove(saveOrder(), getArtifactsSelection().getSelectedIndexes());
    myDataModel.removeAll(items);
  }

  private void updateCurrentOrder(List<LoadedItem> artifactsList) {
    Set<Long> selection = ItemWrapper.GET_ITEM.collectSet(getArtifactsSelection().getSelectedItems());
    TableColumnAccessor<ReorderItem, ?> orderColumn = getCurrentOrder().getColumn();
    assert orderColumn.getComparator() != null;
    Collections.sort(artifactsList, Containers.reverse(getCurrentOrder().getComparator()));
    List<ReorderItem> list = ReorderItem.collect(artifactsList, this);
    myOrderColumn.setColumn(orderColumn);
    myTable.getColumnModel().forceUpdateAt(0);
    myDataModel.setElements(list);
    if (!selection.isEmpty()) {
      getArtifactsSelection().clearSelection();
      getArtifactsSelection().selectAll(ItemWrapper.GET_ITEM.elementOf(selection));
    }
  }

  private static JScrollPane createTableScrollPane(ATable<ReorderItem> table,
      ArtifactTableColumns<LoadedItem> columns, SubsetModel<? extends TableColumnAccessor<LoadedItem, ?>> selectedColumns, Configuration config)
  {
    AScrollPane scrollPane = new AScrollPane();
    ConfigureColumnsAction configureColumns = new ConfigureColumnsAction(Factory.Const.newConst(columns), (Factory)Factory.Const.newConst(selectedColumns));
    configureColumns.addCornerButton(scrollPane);
    ATable.addHeaderActions(table, configureColumns);
    Aqua.cleanScrollPaneBorder(scrollPane);
    Aero.cleanScrollPaneBorder(scrollPane);
    Aqua.cleanScrollPaneResizeCorner(scrollPane);
    return table.wrapWithScrollPane(scrollPane);
  }

  private JComponent createToolbar(JComponent context, ConfiguredSplitPane splitPane) {
    ToolbarBuilder builder = new ToolbarBuilder();
    builder.setCommonPresentation(PresentationMapping.VISIBLE_NONAME);
    builder.addAction(COMMIT_ACTION, context, PresentationMapping.DEFAULT);
    builder.addAction(SAVE_ACTION, context, PresentationMapping.DEFAULT);
    builder.addAction(ItemActionUtils.DISCARD, null, PresentationMapping.DEFAULT);
    builder.addSeparator();
    builder.addAction(REMOVE_ARTIFACTS_ACTION);
    builder.addAction(UndoHistory.UNDO_ACTION);
    builder.addAction(UndoHistory.REDO_ACTION);
    builder.addAction(RESET_ORDER_ACTION);
    builder.addAction(splitPane.createShowBottomAction("Details Panel"));
    builder.addSeparator();
    createMoveActions(builder);
    AToolbar toolbar = builder.createHorizontalToolbar();
    JPanel panel = new JPanel(UIUtil.createBorderLayout());
    panel.add(toolbar, BorderLayout.CENTER);
    panel.add(myOrderSelector.getWholePanel(), BorderLayout.EAST);
    Aqua.addSouthBorder(panel);
    Aero.addLightSouthBorder(panel);
    return panel;
  }

  private void createMoveActions(ToolbarBuilder builder) {
    ourStrokes.clear();
    builder.addAction(new MoveAction(Icons.ARROW_UP, "Move selected " + Terms.ref_artifacts + " up", false, false));
    builder.addAction(new MoveAction(Icons.ARROW_DOWN, "Move selected " + Terms.ref_artifacts + " down", true, false));
    builder.addAction(
      new MoveAction(Icons.ARROW_UP_STRESSED, "Move selected " + Terms.ref_artifacts + " to the top", false, true));
    builder.addAction(
      new MoveAction(Icons.ARROW_DOWN_STRESSED, "Move selected " + Terms.ref_artifacts + " to the bottom", true, true));

    removeStrokes(JComponent.WHEN_FOCUSED);
    removeStrokes(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    removeStrokes(JComponent.WHEN_IN_FOCUSED_WINDOW);

    myTable.setName("__reorder");
  }

  private void removeStrokes(int when) {
    InputMap map = myTable.getSwingComponent().getInputMap(when);
    for (KeyStroke stroke : ourStrokes) {
      if (map.get(stroke) != null) {
        map.put(stroke, "---");
      }
    }
  }

  private Order getCurrentOrder() {
    return myOrderSelector.getCurrentOrder();
  }

  private Configuration getColumnConfig(Configuration config, ColumnsCollector columnsCollector) {
    Configuration result;
    if (!config.isSet(TABLE_COLUMNS)) {
      result = config.createSubset(TABLE_COLUMNS);
      columnsCollector.copyDefaultColumns(result);
    } else
      result = config.getSubset(TABLE_COLUMNS);
    return result;
  }

  private boolean canInsert(int row, List<ItemWrapper> items) {
    if (items.isEmpty())
      return false;
    if (!isAllCanReorder(items))
      return false;
    if (items.size() > 1)
      return true;
    long artifact = items.get(0).getItem();
    if (row > 0 && artifact == myDataModel.getAt(row - 1).getItem())
      return false;
    //noinspection RedundantIfStatement
    if (row < myDataModel.getSize() && Util.equals(artifact, myDataModel.getAt(row).getItem()))
      return false;
    return true;
  }

  private void move(int insertAt, Collection<? extends ItemWrapper> items) {
    assert !myEditLifecycle.isDuringCommit();
    ReorderItem[] prevOrder = saveOrder();
    Object[] prevValues = saveOrderValues(prevOrder);
    int[] selection = getArtifactsSelection().getSelectedIndexes();

    final HashSet<Long> changedItems = Collections15.hashSet();
    for (ItemWrapper item : items) {
      changedItems.add(item.getItem());
    }
    int prevCount = 0;
    for (int i = 0; i < insertAt; i++)
      if (changedItems.contains(myDataModel.getAt(i).getItem()))
        prevCount++;
    insertAt -= prevCount;
    assert insertAt >= 0;
    myDataModel.removeAll(new Condition<LoadedItem>() {
      public boolean isAccepted(LoadedItem value) {
        return changedItems.contains(value.getItem());
      }
    });
    assert insertAt <= myDataModel.getSize();
    List<ReorderItem> insert = Collections15.arrayList(items.size());
    for (ItemWrapper item : items) {
      insert.add((item instanceof ReorderItem) && ((ReorderItem) item).getReorderOwner() == this ?
        (ReorderItem) item : new ReorderItem((LoadedItem) item, this, -1));
    }
    myDataModel.insertAll(insertAt, insert);

    updateOrder(changedItems, null);

    myUndoHistory.addEntry(prevOrder, prevValues, selection);
    myTable.setSelectionWhenDndDone(myDataModel.indeciesOf(insert));
    myDataModel.updateRange(0, myDataModel.getSize() - 1);
  }

  private void updateOrder(HashSet<Long> changedArtifacts, int[] changedIndexes) {
    List<ReorderItem> order = copyArtifacts();
    IntArray indexes = buildChangedIndexesArray(order, changedArtifacts, changedIndexes);
    getCurrentOrder().updateOrder(order, indexes.toNativeArray());
  }

  private IntArray buildChangedIndexesArray(List<ReorderItem> order, HashSet<Long> changedArtifacts,
    int[] changedIndexes)
  {
    IntArray indexes = new IntArray();
    int cii = 0;
    for (int i = 0; i < order.size(); i++) {
      boolean changed;
      if (changedIndexes != null && cii < changedIndexes.length && changedIndexes[cii] == i) {
        cii++;
        changed = true;
      } else {
        ReorderItem artifact = order.get(i);
        changed = artifact.getNewOrderValue() != null;
        changed = changed || (changedArtifacts != null && changedArtifacts.contains(artifact.getItem()));
      }
      if (changed) {
        indexes.add(i);
      }
    }
    return indexes;
  }

  private List<ReorderItem> copyArtifacts() {
    return Collections15.arrayList(myDataModel.toList());
  }

  private Object[] saveOrderValues(ReorderItem[] prevOrder) {
    return ReorderItem.GET_ORDER_VALUE.collectArray(prevOrder, Object.class);
  }

  private ReorderItem[] saveOrder() {
    return myDataModel.toList().toArray(new ReorderItem[myDataModel.getSize()]);
  }

  private void resetOrder() {
    ReorderItem[] order = saveOrder();
    Object[] values = saveOrderValues(order);
    int[] selection = getArtifactsSelection().getSelectedIndexes();
    for (int i = 0; i < myDataModel.getSize(); i++)
      myDataModel.getAt(i).setNewOrderValue(null);
    myDataModel.sort(Containers.reverse(getCurrentOrder().getComparator()));
    myUndoHistory.addEntry(order, values, selection);
  }

  private boolean isAllCanReorder(Collection<? extends ItemWrapper> items) {
    for (ItemWrapper item : items) {
      if (!getCurrentOrder().canOrder(item))
        return false;
    }
    return true;
  }

  public AnActionListener createCloseConfirmation() {
    return new AnActionListener() {
      public void perform(ActionContext context) throws CantPerformException {
        boolean needsConfirmation = hasChanges();
        if (!needsConfirmation)
          return;
        context.getSourceObject(WindowController.ROLE).toFront();
        int reply = DialogsUtil.askUser(context.getComponent(), L.content("Would you like to save entered information as a draft?"), L.dialog("Confirm Close Window"), YES_NO_CANCEL_OPTION);
        switch(reply) {
        case CLOSED_OPTION:
        case CANCEL_OPTION: throw new CantPerformExceptionSilently("Cancelled");
        case DialogsUtil.NO_OPTION: break;
        case DialogsUtil.YES_OPTION:
          SAVE_ACTION.perform(context);
          break;
        default:
          assert false : reply;
        }
      }
    };
  }

  public void commitToDb(ActionContext context, final boolean upload) throws CantPerformException {
    EditLifecycle editLifecycle = context.getSourceObject(EditLifecycle.ROLE);
    editLifecycle.checkCommitAction();
    final List<ReorderItem> items = collectItemsToCommit();
    final Order order = getCurrentOrder();
    AggregatingEditCommit commit = new AggregatingEditCommit();
    commit.addProcedure(null, new EditCommit.Adapter() {
      @Override
      public void performCommit(EditDrain drain) throws DBOperationCancelledException {
        order.updateItems(drain, items);
      }
    });
    if (upload) commit.addProcedure(null, UploadOnSuccess.create(items));
    editLifecycle.commit(context, commit, true);
  }

  private List<ReorderItem> collectItemsToCommit() {
    List<ReorderItem> items = Collections15.arrayList(myDataModel.toList());
    Iterator<ReorderItem> iterator = items.iterator();
    while (iterator.hasNext()) {
      ReorderItem item = iterator.next();
      if (item.getNewOrderValue() == null)
        iterator.remove();
    }
    return items;
  }

  boolean hasChanges() {
    for (int i = 0; i < myDataModel.getSize(); i++)
      if (myDataModel.getAt(i).getNewOrderValue() != null)
        return true;
    return false;
  }
//
//  private void uploadArtifacts(List<ReorderItem> items) {
//    HashMap<Connection, LongArray> byConnection = Collections15.hashMap();
//    for (ReorderItem rItem : items) {
//      Connection connection = rItem.getConnection();
//      LongArray toUpload = byConnection.get(connection);
//      if (toUpload == null) {
//        toUpload = new LongArray();
//        byConnection.put(connection, toUpload);
//      }
//      toUpload.add(rItem.getItem());
//    }
//    for (Map.Entry<Connection, LongArray> entry : byConnection.entrySet())
//      entry.getKey().uploadItems(entry.getValue());
//  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  @Deprecated
  public void dispose() {
    myViewer.dispose();
    myLife.detach();
    myColumnModelLife.detach();
    myOrderSelector.dispose();
  }

  public Detach getDetach() {
    return new Disposer(this);
  }

  public void propertyChanged(TypedKey<Order> typedKey, Object bean, Order oldValue, Order newValue) {
    assert bean == myOrderSelector;
    assert typedKey == OrderSelector.ORDER;
    List<LoadedItem> items = Collections15.arrayList(myDataModel.getSize());
    for (int i = 0; i < myDataModel.getSize(); i++)
      items.add(myDataModel.getAt(i).getOriginal());
    updateCurrentOrder(items);
    myUndoHistory.clear();
  }

  private static void enableWhenModelChanged(UpdateContext context) throws CantPerformException {
    context.updateOnChange(context.getComponentContext(FlatCollectionComponent.class, ItemWrapper.ITEM_WRAPPER)
      .getComponent().getCollectionModel());
    context.setEnabled(context.getSourceObject(ROLE).hasChanges());
  }

  private SelectionAccessor<ReorderItem> getArtifactsSelection() {
    return myTable.getSelectionAccessor();
  }

  private static class CommitAction extends SimpleAction {
    private final boolean myUpload;

    public CommitAction(boolean upload) {
      myUpload = upload;
      watchModifiableRole(EditLifecycle.MODIFIABLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      if (myUpload) IdActionProxy.setShortcut(context, MainMenu.NewItem.COMMIT);
      context.getSourceObject(EditLifecycle.ROLE).checkCommitAction();
      enableWhenModelChanged(context);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      context.getSourceObject(ROLE).commitToDb(context, myUpload);
    }
  }


  private class OrderColumnColumn extends TableColumnAccessor.DelegatingColumn<ReorderItem, Object>
    implements CollectionRenderer<ReorderItem>
  {
    // We match column background with opaque renderer's background.
    final float ALPHA = 0.3f;
    final Color BACKGROUND = ((JTable)myTable.getSwingComponent()).getSelectionBackground();

    public JComponent getRendererComponent(CellState state, ReorderItem item) {
      if (!state.isSelected()) {
        final Color defaultBackground = state.getDefaultBackground();
        state = new DelegatingCellState(state) {
          @Override
          public Color getBackground() {
            return getBackground(true);
          }

          @Override
          public Color getBackground(boolean opaque) {
            return ColorUtil.between(defaultBackground, BACKGROUND, ALPHA);
          }
        };
      }
      return getDelegate().getDataRenderer().getRendererComponent(state, item);
    }

    public OrderColumnColumn(TableColumnAccessor<? super ReorderItem, ? extends Object> column) {
      super(column);
    }

    public int getPreferredWidth(JTable table, ATableModel aTableModel, ColumnAccessor renderingAccessor,
      int columnIndex)
    {
      return getDelegate().getPreferredWidth(table, aTableModel, renderingAccessor, columnIndex);
    }

    public String getId() {
      return "#ReorderWindow.FirstColumn#";
    }

    public CollectionRenderer<ReorderItem> getDataRenderer() {
      return this;
    }

    public boolean isOrderChangeAllowed() {
      return false;
    }

    @Override
    public <T> T getHint(TypedKey<T> key) {
      if(key == TableColumnAccessor.BACKGROUND_COLOR_HINT) {
        return (T)BACKGROUND;
      }
      if(key == TableColumnAccessor.BACKGROUND_ALPHA_HINT) {
        return (T)Float.valueOf(ALPHA);
      }
      return super.getHint(key);
    }
  }


  private static class ReorderTransfer extends BaseItemContextTransfer {
    public void acceptTransfer(DragContext context, Transferable tranferred)
      throws CantPerformException, UnsupportedFlavorException, IOException
    {
      ReorderWindow window = context.getSourceObject(ROLE);
      List<ItemWrapper> items = getTransferedArtifacts(context);
      assert window.isAllCanReorder(items);
      window.move(getTragetRow(context), items);
    }

    public boolean canImportDataNow(DragContext context, Component dropTarget) throws CantPerformException {
      ReorderWindow window = DefaultActionContext.getSourceObject(ROLE, dropTarget);
      List<ItemWrapper> items = getTransferedArtifacts(context);
      return window.canInsert(getTragetRow(context), items);
    }

    private int getTragetRow(DragContext context) throws CantPerformException {
      TableDropPoint point = context.getValue(DndUtil.TABLE_DROP_POINT);
      if (point == null || !point.isValid())
        throw new CantPerformException();
      return point.getTargetRow() + 1;
    }

    @NotNull
    private List<ItemWrapper> getTransferedArtifacts(DragContext context) throws CantPerformException {
      List<ItemWrapper> items = context.getTransferData(ItemWrappersTransferrable.ARTIFACTS_FLAVOR);
      if (items == null || items.isEmpty())
        throw new CantPerformException();
      return items;
    }
  }


  private static class RemoveAction extends SimpleAction {
    public RemoveAction() {
      super("Remove", Icons.ACTION_GENERIC_REMOVE);
      setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.DELETE);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Remove from the list of reordered " + Terms.ref_artifacts + ", ignore any changes to order");
      watchRole(ItemWrapper.ITEM_WRAPPER);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(!context.getSourceCollection(ItemWrapper.ITEM_WRAPPER).isEmpty());
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      List<ItemWrapper> items = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
      for (ItemWrapper item : items)
        if ((item instanceof ReorderItem) && (((ReorderItem) item).getNewOrderValue() != null)) {
          break;
        }
      context.getSourceObject(ROLE).remove(items);
    }
  }


  private static class ClearChangesAction extends SimpleAction {
    public ClearChangesAction() {
      super("Clear Changes", Icons.ACTION_RESET);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      enableWhenModelChanged(context);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      context.getSourceObject(ROLE).resetOrder();
    }
  }


  private class MoveAction extends SimpleAction {
    private final boolean myForward;
    private final boolean myFarthest;

    public MoveAction(Icon icon, String name, boolean forward, boolean farthest) {
      super(name, icon);
      myForward = forward;
      myFarthest = farthest;

      final int key = forward ? KeyEvent.VK_DOWN : KeyEvent.VK_UP;
      final int mask = farthest ? Shortcuts.MENU_MASK | KeyEvent.SHIFT_DOWN_MASK : Shortcuts.MENU_MASK;
      final KeyStroke keyStroke = KeyStroke.getKeyStroke(key, mask);
      setDefaultPresentation(PresentationKey.SHORTCUT, keyStroke);
      ourStrokes.add(keyStroke);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.updateOnChange(myTable.getSelectionAccessor());
      int[] indexes = myTable.getSelectionAccessor().getSelectedIndexes();
      boolean enabled = indexes.length > 0;
      if (enabled) {
        int limit = myForward ? myDataModel.getSize() - 1 : 0;
        Order order = getCurrentOrder();
        for (int index : indexes) {
          if (index == limit || !order.canOrder(myDataModel.getAt(index))) {
            enabled = false;
            break;
          }
        }
      }
      context.setEnabled(enabled);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      int[] selection = myTable.getSelectionAccessor().getSelectedIndexes();
      if (selection.length == 0)
        return;

      ReorderItem[] prevOrder = saveOrder();
      Object[] prevValues = saveOrderValues(prevOrder);
      int[] prevSelection = selection.clone();

      Arrays.sort(selection);
      int move;
      int start;
      int size = myDataModel.getSize();
      if (myForward) {
        start = selection.length - 1;
        move = myFarthest ? size - 1 - selection[start] : 1;
      } else {
        start = 0;
        move = myFarthest ? -selection[0] : -1;
      }
      if (move == 0) {
        assert false : move + " " + Arrays.toString(selection);
        return;
      }

      for (int i = start; i >= 0 && i < selection.length; i += myForward ? -1 : 1) {
        int index = selection[i];
        int newIndex = index + move;
        if (newIndex < 0 || newIndex >= size)
          continue;
//        myDataModel.swap(newIndex, index);
        ReorderItem item = myDataModel.removeAt(index);
        myDataModel.insert(newIndex, item);
        selection[i] = newIndex;
      }

      updateOrder(null, selection);

      myUndoHistory.addEntry(prevOrder, prevValues, prevSelection);
      myTable.getSelectionAccessor().setSelectedIndexes(selection);
      myDataModel.updateRange(0, size - 1);
    }
  }
}
