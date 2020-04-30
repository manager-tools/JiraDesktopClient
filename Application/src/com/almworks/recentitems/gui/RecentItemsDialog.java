package com.almworks.recentitems.gui;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.WorkflowComponent2;
import com.almworks.api.gui.*;
import com.almworks.explorer.IssueDrivenPopupEntry;
import com.almworks.explorer.ItemsContextTransfer;
import com.almworks.recentitems.RecordType;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.ScrollBarPolicy;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataProviderConvertingDecorator;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.PresentationKey;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.picocontainer.Startable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

import static com.almworks.api.gui.MainMenu.RecentItems;

public class RecentItemsDialog implements Startable {
  public static final DataRole<LoadedRecord> LOADED_RECORD = DataRole.createRole(LoadedRecord.class);

  private static RecentItemsDialog ourInstance;

  private final RecentItemsLoader myLoader;
  private final DialogManager myDialogManager;
  private final WindowManager myWindowManager;
  private final TimeTrackingCustomizer myCustomizer;
  private final WorkflowComponent2 myWorkflow;

  private final Lifecycle myDialogCycle = new Lifecycle();
  private DialogBuilder myCurrBuilder;
  private ASortedTable<LoadedRecord> myCurrTable;
//  private TextComponentTracker myTracker;

  public RecentItemsDialog(
    RecentItemsLoader loader, DialogManager dialogManager, WindowManager windowManager,
    TimeTrackingCustomizer customizer, WorkflowComponent2 workflow)
  {
    myLoader = loader;
    myDialogManager = dialogManager;
    myWindowManager = windowManager;
    myCustomizer = customizer;
    myWorkflow = workflow;
  }

  @Override
  public void start() {
    ourInstance = this;
  }

  @Override
  public void stop() {
    ourInstance = null;
  }

  private void doShowDialog(Component contextComponent) {
    if(myCurrBuilder != null) {
      bringCurrentDialogToFront();
    } else {
      showNewDialog(contextComponent);
    }
  }

  private void bringCurrentDialogToFront() {
    assert myCurrBuilder != null;
    final WindowController wc = myCurrBuilder.getWindowContainer().getActor(WindowController.ROLE);
    if(wc != null) {
      wc.activate();
    }
  }

  private void showNewDialog(Component contextComponent) {
    final DialogBuilder builder = myDialogManager.createMainBuilder("recentArtifactsDialog");
    builder.setTitle("Recently Uploaded " + Local.text(Terms.ref_Artifacts));
    builder.setIgnoreStoredSize(true); // todo?
    builder.setBottomLineShown(false);
    builder.setBottomBevel(false);
    builder.setBorders(false);
    builder.setContent(createDialogContent(contextComponent));
    builder.setActionScope("RecentArtifacts");
    buildAndShowDialog(builder);
  }

  private JComponent createDialogContent(Component contextComponent) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(createTable(contextComponent), BorderLayout.CENTER);
    panel.add(createToolbar(), BorderLayout.NORTH);
    return panel;
  }

  private JComponent createTable(Component contextComponent) {
    final Lifespan life = myDialogCycle.lifespan();
    final ASortedTable<LoadedRecord> table = new ASortedTable<LoadedRecord>();
    table.setGridHidden();
    table.setStriped(true);
    table.setColumnModel(createColumns());
    life.add(table.setCollectionModel(myLoader.getLoadedModel()));
    Aqua.cleanScrollPaneBorder(table);
    Aero.cleanScrollPaneBorder(table);
    Aqua.cleanScrollPaneResizeCorner(table);
    ScrollBarPolicy.setDefaultWithHorizontal(table, ScrollBarPolicy.AS_NEEDED);
    final int prefRows = Util.bounded(10, myLoader.getLoadedModel().getSize(), 20);
    table.adjustSize(55, prefRows, 20, 5);
    table.setDataRoles(LOADED_RECORD);
    decorateProvider(table.getTable(), ItemWrapper.ITEM_WRAPPER);
    decorateProvider(table.getTable(), LoadedItem.LOADED_ITEM);
//    myTracker = new TextComponentTracker(life, null, contextComponent);
//    ConstProvider.addRoleValue(table.getTable(), TextComponentTracker.TEXT_COMPONENT_TRACKER, myTracker);
    ConstProvider.addRoleValue(table.getTable(), WindowManager.ROLE, myWindowManager);
    table.setTransfer(new ItemsContextTransfer());
    installPopupMenu(life, table);
    installSpeedSearch(table);
    myCurrTable = table;
    return table;
  }

  private AListModel<TableColumnAccessor<LoadedRecord, ?>> createColumns() {
    final List<TableColumnAccessor<LoadedRecord, ?>> columns = Collections15.arrayList();
    columns.add(createTimeColumn());
    columns.add(createKeyColumn());
    columns.add(createSummaryColumn());
    return FixedListModel.create(columns);
  }

  private TableColumnAccessor<LoadedRecord, ?> createTimeColumn() {
    final Column col = new TimeColumn();
    return TableColumnBuilder.<LoadedRecord, LoadedRecord>create("timestamp", "When")
      .setCanvasRenderer(col)
      .setComparator(col)
      .setConvertor(Convertor.<LoadedRecord>identity())
      .setSizePolicy(ColumnSizePolicy.Calculated.fixedTextWithMargin(DateUtil.toLocalDateOrTime(new Date(0L)), 4))
      .createColumn();
  }

  private TableColumnAccessor<LoadedRecord, ?> createKeyColumn() {
    final KeyColumn col = new KeyColumn();
    return TableColumnBuilder.<LoadedRecord, LoadedRecord>create("artifactKey", Local.text(Terms.key_Artifact_ID))
      .setCanvasRenderer(col)
      .setComparator(col)
      .setConvertor(Convertor.<LoadedRecord>identity())
      .setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(10))
      .createColumn();
  }

  private TableColumnAccessor<LoadedRecord, ?> createSummaryColumn() {
    final SummaryColumn col = new SummaryColumn();
    return TableColumnBuilder.<LoadedRecord, LoadedRecord>create("summary", "Summary")
      .setCanvasRenderer(col)
      .setComparator(col)
      .setConvertor(Convertor.<LoadedRecord>identity())
      .setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(40))
      .createColumn();
  }

  private <T> void decorateProvider(JComponent component, DataRole<T> role) {
    DataProviderConvertingDecorator.decorate(
      component, LOADED_RECORD, role, this.<T>recordToArtifact());
  }

  private <T> Convertor<LoadedRecord, Collection<T>> recordToArtifact() {
    return new Convertor<LoadedRecord, Collection<T>>() {
      @Override
      public Collection<T> convert(LoadedRecord value) {
        return Collections.<T>singletonList((T)value.myItem);
      }
    };
  }

  private void installPopupMenu(Lifespan life, ASortedTable<LoadedRecord> table) {
    new MenuBuilder()
      .addDefaultAction(MainMenu.RecentItems.OPEN_IN_TAB)
      .addSeparator()
      .addAction(MainMenu.RecentItems.OPEN_IN_FRAME)
      .addAction(MainMenu.RecentItems.OPEN_IN_BROWSER)
      .addAction(RecentItems.EDIT_ITEM)
      .addSeparator()
      .addAction(MainMenu.RecentItems.COPY_ITEM)
      .addAction(MainMenu.RecentItems.COPY_ID_SUMMARY)
      .addAction(RecentItems.CUSTOM_COPY)
//      .addAction(RecentArtifacts.PASTE_ITEM_KEY)
      .addSeparator()
      .addEnabledActionsFromModel(myWorkflow.getWorkflowActions())
      .addSeparator()
      .addEntry(createIssueDrivenEntry())
    .addToComponent(life, table.getSwingComponent());
  }

  private IssueDrivenPopupEntry createIssueDrivenEntry() {
    return new IssueDrivenPopupEntry(new Convertor<LoadedRecord, LoadedItem>() {
      @Override
      public LoadedItem convert(LoadedRecord value) {
        return value.myItem;
      }
    });
  }

  private void installSpeedSearch(ASortedTable<LoadedRecord> table) {
    final ListSpeedSearch<LoadedRecord> ssc =
      ListSpeedSearch.<LoadedRecord>install(table.getTable(), 2, new SummaryColumn());
    ssc.setCaseSensitive(false);
    ssc.setSearchSubstring(true);
  }

  private JComponent createToolbar() {
    final ToolbarBuilder builder = ToolbarBuilder.smallVisibleButtons();
    builder.setContextComponent(myCurrTable.getSwingComponent());
    builder.addAction(MainMenu.RecentItems.COPY_ITEM);
    builder.addAction(MainMenu.RecentItems.COPY_ID_SUMMARY);
    builder.addAction(RecentItems.CUSTOM_COPY);
//    builder.addAction(RecentArtifacts.PASTE_ITEM_KEY);
    builder.addSeparator();
    builder.addAction(MainMenu.RecentItems.OPEN_IN_FRAME);
    builder.addAction(MainMenu.RecentItems.OPEN_IN_TAB);
    builder.addAction(MainMenu.RecentItems.OPEN_IN_BROWSER);
    builder.addAction(MainMenu.RecentItems.EDIT_ITEM);
    builder.addSeparator();
    builder.addAction(new CloseAction());

    final AToolbar toolbar = builder.createHorizontalToolbar();
    Aqua.addSouthBorder(toolbar);
    Aero.addLightSouthBorder(toolbar);
    return toolbar;
  }

  private void buildAndShowDialog(DialogBuilder builder) {
    builder.showWindow(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        myDialogCycle.cycle();
        myCurrBuilder = null;
        myCurrTable = null;
//        myTracker = null;
      }
    });
    myCurrBuilder = builder;
    myCurrTable.getSwingComponent().requestFocusInWindow();
    myCurrTable.getSelectionAccessor().ensureSelectionExists();

//    final WindowController wc = myCurrBuilder.getWindowContainer().getActor(WindowController.ROLE);
//    if(wc != null) {
//      myTracker.setToolWindow(wc.getWindow());
//    }
  }

  public static void showDialog(Component contextComponent) {
    if(ourInstance != null) {
      ourInstance.doShowDialog(contextComponent);
    }
  }

  private static class CloseAction extends WindowController.CloseWindowAction {
    private CloseAction() {
      setDefaultPresentation(PresentationKey.SMALL_ICON, Icons.ACTION_GENERIC_CANCEL_OR_REMOVE);
      setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.ksPlain(KeyEvent.VK_ESCAPE));
    }
  }

  private static abstract class Column implements CanvasRenderer<LoadedRecord>, Comparator<LoadedRecord> {
    @Override
    public void renderStateOn(CellState state, Canvas canvas, LoadedRecord item) {
      decorateCanvas(canvas, item);
      canvas.appendText(getText(item));
    }

    private void decorateCanvas(Canvas canvas, LoadedRecord item) {
      if(item.myType == RecordType.NEW_UPLOAD) {
        canvas.setFontStyle(Font.BOLD);
      }
    }

    protected abstract String getText(LoadedRecord item);
  }

  private class TimeColumn extends Column {
    @Override
    protected String getText(LoadedRecord item) {
      return DateUtil.toLocalDateOrTime(item.myTimestamp);
    }

    @Override
    public int compare(LoadedRecord o1, LoadedRecord o2) {
      return o1.myTimestamp.compareTo(o2.myTimestamp);
    }
  }

  private class KeyColumn extends Column {
    @Override
    protected String getText(LoadedRecord item) {
      return myCustomizer.getItemKey(item.myItem);
    }

    @Override
    public int compare(LoadedRecord o1, LoadedRecord o2) {
      return myCustomizer.getArtifactByKeyComparator().compare(o1.myItem, o2.myItem);
    }
  }

  private class SummaryColumn extends Column {
    @Override
    protected String getText(LoadedRecord item) {
      return myCustomizer.getItemSummary(item.myItem);
    }

    @Override
    public int compare(LoadedRecord o1, LoadedRecord o2) {
      final String s1 = myCustomizer.getItemSummary(o1.myItem);
      final String s2 = myCustomizer.getItemSummary(o2.myItem);
      return s1.compareTo(s2);
    }
  }
}