package com.almworks.explorer;

import com.almworks.api.application.*;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.viewer.Comment;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.MetaInfoCollector;
import com.almworks.api.explorer.TableController;
import com.almworks.api.gui.MainMenu;
import com.almworks.engine.gui.CommonIssueViewer;
import com.almworks.engine.gui.attachments.Attachment;
import com.almworks.gui.ArtifactTableColumns;
import com.almworks.gui.ConfigureColumnsAction;
import com.almworks.integers.LongList;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.*;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.PrimitiveUtils;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.components.tables.HierarchicalTable;
import com.almworks.util.components.tables.SortingTableHeaderController;
import com.almworks.util.components.tabs.ContentTab;
import com.almworks.util.components.tabs.TabsManager;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.model.ValueModel;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.ConfiguredSplitPane;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIComponentWrapper2Support;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.TransferAction;
import com.almworks.util.ui.actions.globals.GlobalData;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
class TableControllerImpl implements ItemCollectorWidget, TableController {
  private final static String LIFE_MODE = "lifeMode";
  private static final TypedKey<TableControllerImpl> DATA_KEY = TypedKey.create(TableControllerImpl.class);
  private static final Pattern EMPTY_PATTERN = Pattern.compile("");

  private final SimpleProvider myDatas =
    new SimpleProvider(LifeMode.LIFE_MODE_DATA, ItemsTreeLayout.DATA_ROLE, TABLE_COMPONENT,
      ARTIFACT_VIWER_COMPONENT, ItemCollectionContext.ROLE);

  private final AScrollPane myScrollpane = new AScrollPane();
  private final HierarchicalTable<LoadedItem> myArtifactsTree =
    new HierarchicalTable<LoadedItem>(ATable.<LoadedItem>createInscrollPane(myScrollpane));

  private final QueryTitle2 myTitle = new QueryTitle2();

  private final HierarchyController myHierarchy;
  private final MetaInfoCollector myMetaInfoCollector = new MetaInfoCollector();
  private final Configuration myConfig;

  private final Map<LoadedItem, WeakReference<String>> myArtifactStringCache =
    new HashMap<LoadedItem, WeakReference<String>>(128);


  @Nullable
  private ItemCollectionContext myContextInfo = null;
  private final Lifecycle myColumnsLife = new Lifecycle();
  private Detach mySourceDetach = Detach.NOTHING;
  @Nullable
  private ItemsCollectionController myCurrentCollector = null;
  private final OutOfDateStripe myOutOfDateStripe = OutOfDateStripe.create(myArtifactsTree, myScrollpane);
  private final JLabel NOT_FOUND_YET = createMessage(L.html("<html><body>" + Terms.Query +
    " is running.<br>No matching items were found yet.<br>Please wait\u2026</body></html>"));
  private final JLabel EMPTY_RESULT = createMessage(L.content(Terms.Query + " result is empty"));
  private final PlaceHolder myDataPlace = new PlaceHolder();
  private final ContentTab myTab;

  private final LoadedArtifactStatusColumn myStatusColumn = new LoadedArtifactStatusColumn(myArtifactsTree.getTable());

  private boolean myCancelled = false;
  private boolean myFocusOnTable = false;
  private final ColumnsCollector myColumnsCollector;
  private final ExplorerComponent myExplorer;
  private boolean myFilterMatched;
  private final ValueModel<Pattern> myFilterPattern = ValueModel.create();

  private FilteringListDecorator<? extends LoadedItem> myFilteringListDecorator;

  private final Bottleneck myUpdateFilteringModelBottleneck = new Bottleneck(200, ThreadGate.AWT, new Runnable() {
    @Override
    public void run() {
      boolean smthFound =
        onSearchParamsChanged(myTitle.getHighlightText(), myTitle.getRegexp(), myTitle.getCaseSensitive(),
            myTitle.getFilterMatched());
      myTitle.setHighlightBackground(smthFound ? Color.WHITE : Color.PINK);
    }
  });
  private boolean mySelectOnFilterUpdate;
  private final ItemViewer.Controller myViewerController;


  public TableControllerImpl(Configuration config, ContentTab tab, ColumnsCollector columnsCollector, ExplorerComponent explorer, @Nullable Connection sourceConnection)
  {
    myExplorer = explorer;
    myConfig = config;
    myColumnsCollector = columnsCollector;
    myOutOfDateStripe.showStripe(false);
    myHierarchy = new HierarchyController(myDatas, myArtifactsTree, myMetaInfoCollector, sourceConnection);
    myDatas.setSingleData(LifeMode.LIFE_MODE_DATA, LifeMode.NOT_APPLICABLE);
    myDatas.setSingleData(TABLE_COMPONENT, myArtifactsTree.getSwingComponent());

    myTab = tab;
    myTab.setUserProperty(DATA_KEY, this);
    myTab.setVisible(false);
    myTab.resetTabMenu();
    myTab.getMenuBuilder().addAction(new CloneTabAction());
    myDataPlace.setOpaque(false);
    // exp
//    myArtifactsTree.setSortPolicy(CollectionSortPolicy.QUICK);

    InputMap tableInputMap =
      myArtifactsTree.getTable().getSwingComponent().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "selectLastRow");
    tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "selectFirstRow");
    SortingTableHeaderController<LoadedItem> headerController = myArtifactsTree.getHeaderController();
    AListModel<TableColumnAccessor<LoadedItem, ?>> userColumns = headerController.getUserFilteredColumnModel();
    myArtifactsTree.setColumnModel(SegmentedListModel.prepend(myStatusColumn, userColumns));
    setupTable(myArtifactsTree);
    myArtifactsTree.getSwingComponent().setName("TCI.Table");

    if(Aqua.isAqua()) {
      myScrollpane.setBorder(Aqua.MAC_LIGHT_BORDER_NORTH);
    } else if(Aero.isAero()) {
      myScrollpane.setBorder(Aero.getLightBorderNorth());
    } else {
      myScrollpane.setBorder(null);
    }

    myArtifactsTree.setStriped(true);
    myArtifactsTree.setColumnLinesPainted(true);
    
    // experimenting with performance
//    myScrollpane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);

    JPanel tablePanel = new JPanel(new BorderLayout());
    JPanel northPanel = new JPanel(new BorderLayout());
    northPanel.add(myTitle.getComponent(), BorderLayout.CENTER);

    ToolbarBuilder tablToolbar = createTabToolbar();
    JComponent bulkToolbar = createBulkToolbar();
    JPanel tableTop = new JPanel(new FlowLayoutVFixed(FlowLayout.LEADING, 0, 0));
    tableTop.add(tablToolbar.createHorizontalToolbar());
    tableTop.add(bulkToolbar);

    northPanel.add(tableTop, BorderLayout.SOUTH);
    LookAndFeel.installColorsAndFont(tablePanel, "Table.background", "Table.foreground", "Table.font");
    tablePanel.add(myDataPlace, BorderLayout.CENTER);
    tablePanel.add(northPanel, BorderLayout.NORTH);

    myViewerController = ItemViewer.create(myConfig, myArtifactsTree.getSelectionAccessor());
    myDatas.setSingleData(ARTIFACT_VIWER_COMPONENT, myViewerController.getViewer().getComponent());

    JScrollPane tableSection = UIUtil.getScrollPaned(tablePanel);
    Aqua.cleanScrollPaneBorder(tableSection);
    Aero.cleanScrollPaneBorder(tableSection);

    final JSplitPane content =
      ConfiguredSplitPane.createTopBottomJumping(
        tableSection, myViewerController.getViewer().getComponent(), myConfig, 0.5F, myArtifactsTree, true);
    content.setResizeWeight(0);
    Aqua.makeLeopardStyleSplitPane(content);
    Aero.makeBorderedDividerSplitPane(content);
    DataProvider.DATA_PROVIDER.putClientValue(content, myDatas);
    GlobalData.KEY.addClientValue(content, LifeMode.LIFE_MODE_DATA);
    GlobalData.KEY.addClientValue(content, ItemsTreeLayout.DATA_ROLE);
    GlobalData.KEY.addClientValue(content, TABLE_COMPONENT);
    GlobalData.KEY.addClientValue(content, ARTIFACT_VIWER_COMPONENT);

    myTab.setComponent(new UIComponentWrapper2Support() {
      @SuppressWarnings({"Deprecation"})
      @Deprecated
      @Override
      public void dispose() {
        myViewerController.dispose();
        myArtifactsTree.clearRoot();
        myColumnsLife.dispose();
        myArtifactsTree.setColumnModel(AListModel.EMPTY);
        super.dispose();
      }

      @Override
      public Detach getDetach() {
        return mySourceDetach;
      }

      @Override
      public JComponent getComponent() {
        return content;
      }
    });
    UIComponentWrapper wrapper = myTab.getComponent();
    assert wrapper != null : myTab;
    CommonIssueViewer.PATTERN_MODEL_PROPERTY.putClientValue(wrapper.getComponent(), myFilterPattern);
    ConstProvider.addGlobalValue(content, DATA_ROLE, this);

    myTitle.getFilterParamModifiable().addAWTChangeListener(new ChangeListener() {
      @Override
      public void onChange() {
        mySelectOnFilterUpdate = true;
        myUpdateFilteringModelBottleneck.request();
      }
    });

    final Runnable nextAction = new Runnable() {
      @Override
      public void run() {
        selectNextHighlighted();
      }
    };

    final Runnable prevAction = new Runnable() {
      @Override
      public void run() {
        selectPrevHighlighted();
      }
    };

    myTitle.setNextPrevActions(nextAction, prevAction);
    myTitle.addHighlightKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        JComponent jComponent = myArtifactsTree.getSwingComponent();
        switch (e.getKeyCode()) {
        case KeyEvent.VK_ESCAPE:
          myTitle.setHighlightPanelVisible(false);
          jComponent.requestFocusInWindow();
          break;
        case KeyEvent.VK_DOWN:
          selectNextHighlighted();
          break;
        case KeyEvent.VK_UP:
          selectPrevHighlighted();
          break;
        case KeyEvent.VK_ENTER:
          jComponent.requestFocusInWindow();
        }
      }
    });
    myFilterPattern.setValue(EMPTY_PATTERN);
  }

  private JComponent createBulkToolbar() {
    final SelectionAccessor<LoadedItem> accessor = myArtifactsTree.getSelectionAccessor();
    BulkActionPanel panel = new BulkActionPanel(accessor);
    accessor.addAWTChangeListener(panel);
    panel.onChange();
    return panel.getComponent();
  }

  private ToolbarBuilder createTabToolbar() {
    ToolbarBuilder builder = ToolbarBuilder.smallEnabledButtons();
    builder.addAction(MainMenu.Search.KEEP_LIVE_RESULTS);
    builder.addSeparator();
    builder.addComponent(myHierarchy.getComponent());
    builder.addAction(MainMenu.Search.CUSTOMIZE_HIERARCHY);
    builder.addSeparator();
    builder.addAction(MainMenu.Tools.EXPORT);
    builder.addAction(MainMenu.Tools.QUICK_EXPORT);
    builder.addAction(MainMenu.Tools.REORDER_TABLE);
    return builder;
  }

  @Override
  public void setHighlightPanelVisible(boolean visible) {
    myTitle.setHighlightPanelVisible(visible);
  }

  @Override
  public boolean isHighlightPanelVisible() {
    return myTitle.isHighlightPanelVisible();
  }

  @Override
  public Modifiable getHighlightPanelModifiable() {
    return myTitle.getHighlightPanelModifiable();
  }

  @Nullable
  @Override
  public String getCollectionShortName() {
    ItemCollectionContext info = myContextInfo;
    return info == null ? null : info.getShortName();
  }

  @Nullable
  @Override
  public GenericNode getCollectionNode() {
    ItemCollectionContext info = myContextInfo;
    return info == null ? null : info.getQuery();
  }

  @Nullable
  @Override
  public ItemCollectionContext getItemCollectionContext() {
    return myContextInfo;
  }

  @Override
  public void setSearchFilterString(String str, boolean regexp, boolean caseSensitive, boolean filter) {
    myTitle.setFilterPattern(str, caseSensitive, regexp, filter);
  }

  private boolean onSearchParamsChanged(String str, boolean regexp, boolean caseSensitive, boolean filter) {
    boolean active = str.length() > 0;
    boolean select = mySelectOnFilterUpdate;
    mySelectOnFilterUpdate = false;

    int attr = 0;
    attr |= (caseSensitive) ? 0 : Pattern.CASE_INSENSITIVE;
    attr |= (regexp) ? 0 : Pattern.LITERAL;

    Pattern pattern = null;
    try {
      pattern = active ? Pattern.compile(str, attr) : EMPTY_PATTERN;
    } catch (Exception e) {
      // ignore
    }

    pattern = changePattern(pattern);

    if (!active) {
      filter = false;
    }
    if (filter || myFilterMatched) {
      myFilterMatched = filter;
      List<LoadedItem> itemList = myArtifactsTree.getSelectionAccessor().getSelectedItems();
      myFilteringListDecorator.resynch();
      myArtifactsTree.getSelectionAccessor().setSelected(itemList);
      myArtifactsTree.getSelectionAccessor().ensureSelectionExists();
    }

    if (!myFilterPattern.getValue().pattern().equals("")) {
      assert active : str;
      int curIndex = myArtifactsTree.getTable().getSelectionAccessor().getSelectedIndex();
      int result;
      if (curIndex < 0) {
        result = findNextHighlighted(-1);
      } else {
        result = findNextHighlighted(curIndex - 1);
        if (result < 0) {
          result = findPrevHighlighted(curIndex + 1);
        }
      }
      if (result >= 0) {
        if (select || curIndex < 0)
          selectArtifact(result);
        return true;
      } else {
        return false;
      }
    }
    return true;
  }

  private Pattern changePattern(Pattern pattern) {
    if (pattern != null) {
      myFilterPattern.setValue(pattern);
    } else {
      pattern = myFilterPattern.getValue();
    }

    Canvas.PATTERN_PROPERTY.putClientValue(myArtifactsTree.getSwingComponent(), pattern);
    myArtifactsTree.setHighlightPattern(pattern);

    return pattern;
  }

  @Override
  public void selectNextHighlighted() {
    selectArtifact(findNextHighlighted());
  }

  @Override
  public void selectPrevHighlighted() {
    selectArtifact(findPrevHighlighted());
  }

  private void selectArtifact(int index) {
    if (index < 0)
      return;
    AListModel<? extends LoadedItem> model = getCollectionModel();
    if (index >= model.getSize())
      return;
    Pattern value = myFilterPattern.getValue();
    if (value != null && !value.pattern().equals("")) {
      myArtifactsTree.getTable().getSelectionAccessor().setSelectedIndex(index);
      myArtifactsTree.getTable().scrollSelectionToView();
    }
  }

  private int findPrevHighlighted(int k) {
    AListModel<? extends LoadedItem> model = getCollectionModel();
    int size = model.getSize();
    if (k < 0 || k > size) {
      k = size;
    }
    for (int i = k - 1; i >= 0; i--) {
      LoadedItem at = model.getAt(i);
      if (matchesPattern(at)) {
        return i;
      }
    }
    return -1;
  }

  private int findPrevHighlighted() {
    return findPrevHighlighted(getSelectedArtifacts().getSelectedIndex());
  }

  private int findNextHighlighted() {
    return findNextHighlighted(getSelectedArtifacts().getSelectedIndex());
  }

  private int findNextHighlighted(int k) {
    AListModel<? extends LoadedItem> model = getCollectionModel();
    if (k < 0)
      k = -1;
    for (int i = k + 1; i < model.getSize(); i++) {
      LoadedItem at = model.getAt(i);
      if (matchesPattern(at)) {
        return i;
      }
    }
    return -1;
  }

  private LoadedItem findFirstHighlighted(Pattern pattern) {
    AListModel<? extends LoadedItem> aListModel = getCollectionModel();

    for (int i = 0; i < aListModel.getSize(); i++) {
      LoadedItem at = aListModel.getAt(i);
      if (matchesPattern(at)) {
        return at;
      }
    }
    return null;
  }

  private SearchResult attachArtifactsCollector(final ItemsCollectionController controller) {
    myCurrentCollector = controller;
    DetachComposite detach = new DetachComposite();
    detach.add(new Detach() {
      @Override
      protected void doDetach() {
        assert controller == myCurrentCollector;
        myHierarchy.setConfig(null, null);
        controller.dispose();
        myCurrentCollector = null;
        myDatas.setSingleData(LifeMode.LIFE_MODE_DATA, LifeMode.NOT_APPLICABLE);
        myDatas.removeData(ItemCollectionContext.ROLE);
        myContextInfo = null;
      }
    });
    setListModelUpdater(detach, controller.getListModelUpdater());
    myHierarchy.listenAdditionalHierarchies(detach);
    controller.addAWTChangeListener(detach, new ChangeListener() {
      @Override
      public void onChange() {
        updateLifeMode();
      }
    });
    ScalarModel<LifeMode> model = controller.getLifeModeModel();
    updateLifeMode();
    model.getEventSource().addAWTListener(detach, new ScalarModel.Adapter<LifeMode>() {
      @Override
      public void onScalarChanged(ScalarModelEvent<LifeMode> event) {
        updateLifeMode();
      }
    });
    SearchResultImpl result = SearchResultImpl.create(controller, myViewerController, myArtifactsTree);
    detach.add(result.getDetach());
    mySourceDetach = detach;
    return result;
  }

  private void updateLifeMode() {
    ItemsCollectionController controller = myCurrentCollector;
    if (controller == null)
      return;
    LifeMode mode = controller.getLifeModeModel().getValue();
    if (controller.isLoading())
      mode = mode.getLoadingMode();
    myDatas.setSingleData(LifeMode.LIFE_MODE_DATA, mode);
  }

  @Override
  public void loadingDone() {
    Threads.assertAWTThread();
    Runnable finish = new Runnable() {
      @Override
      public void run() {
        myTitle.hideProgress();
        // todo #1013 show total node count, not just expanded
        int size = myArtifactsTree.getCollectionModel().getSize();
        if (size == 0)
          myDataPlace.show(EMPTY_RESULT);
        else
          myArtifactsTree.getSelectionAccessor().ensureSelectionExists();
        myTitle.hideLoadingPanel();
        setLiveMode(isLiveModeDefault());
      }
    };
    myHierarchy.loadingDone(finish);
  }

  @Override
  public void showLoadingMessage(String message) {
    myTitle.showLoadingMessage(message);
  }

  @Override
  public void showProgress(float percent) {
    myTitle.showProgress((int) (percent * 100));
  }

  public void hideProgress() {
    myTitle.hideProgress();
  }

  @Override
  public void showErrors(List<String> errors) {
    myCancelled = errors != null && !errors.isEmpty();
    myTitle.showErrors(errors);
  }

  private boolean isLiveModeDefault() {
    return getLifeModeConfig().getBooleanSetting(LIFE_MODE, true);
  }

  @NotNull
  private Configuration getLifeModeConfig() {
    ItemCollectionContext context = myContextInfo;
    if (context == null) return myConfig;
    Configuration config = context.getContextConfig();
    if (config == null) return myConfig;
    if (!config.isSet(LIFE_MODE)) config.setSetting(LIFE_MODE, myConfig.getBooleanSetting(LIFE_MODE, true));
    return config;
  }

  private void setLiveMode(boolean live) {
    Threads.assertAWTThread();
    getLifeModeConfig().setSetting(LIFE_MODE, live);
    LifeMode mode = myDatas.getSingleValue(LifeMode.LIFE_MODE_DATA);
    assert mode != null;
    if (!mode.isApplicable() || live == mode.isLife())
      return;
    myOutOfDateStripe.showStripe(!live);
    ItemsCollectionController collector = myCurrentCollector;
    if (collector != null)
      collector.setLiveMode(live);
  }

  @Override
  public void toggleLifeMode() {
    LifeMode mode = myDatas.getSingleValue(LifeMode.LIFE_MODE_DATA);
    assert mode != null;
    if (!mode.isApplicable())
      return;
    setLiveMode(!mode.isLife());
  }

  @Override
  public void setTreeLayout(@Nullable ItemsTreeLayout layout) {
    myHierarchy.setTreeLayout(layout);
  }

  @Override
  public void setTreeLayoutById(String layoutId) {
    myHierarchy.setTreeLayoutById(layoutId);
  }


  @NotNull
  @Override
  public MetaInfoCollector getMetaInfoCollector() {
    return myMetaInfoCollector;
  }
  
  private void setListModelUpdater(Lifespan life, AListModelUpdater<? extends LoadedItem> updater) {
    final AListModel<? extends LoadedItem> model = updater.getModel();
    myFilteringListDecorator = FilteringListDecorator.create(life, model, new ArtifactShowCondition());
    myHierarchy.setListModelUpdater(life, updater, myFilteringListDecorator);
    if (model.getSize() == 0) {
      myDataPlace.show(NOT_FOUND_YET);
    } else {
      showTable();
    }
    life.add(((AListModel<LoadedItem>) model).addListener(new AListModel.Adapter<LoadedItem>() {
      @Override
      public void onChange() {
        int size = model.getSize();
        myTitle.updateArtifactCounter(size);
        if (size > 0) {
          showTable();
          myStatusColumn.updateWidth(model);
        }
        myUpdateFilteringModelBottleneck.requestDelayed();
        //myArtifactsTree.getSwingComponent().updateUI();
      }

      @Override
      public void onItemsUpdated(AListModel.UpdateEvent event) {
        super.onItemsUpdated(event);
        List<LoadedItem> itemList = event.collectUpdated(model);
        for (LoadedItem loadedItem : itemList) {
          myArtifactStringCache.remove(loadedItem);
        }
      }

      @Override
      public void onInsert(int index, int length) {
        super.onInsert(index, length);
        //myFilteringListDecorator.onInsert(index, length);
        myMetaInfoCollector.onNewArtifacts(model.subList(index, index + length));
        if (length == model.getSize()) {
          ItemsCollectionController collector = myCurrentCollector;
          if (collector != null && !collector.isLoading()) myArtifactsTree.getSelectionAccessor().ensureSelectionExists();
        }
      }

      @Override
      public void onRemove(int index, int length, AListModel.RemovedEvent<LoadedItem> event) {
        super.onRemove(index, length, event);
        myMetaInfoCollector.onArtifactsRemoved(event.getAllRemoved());
        List<LoadedItem> itemList = event.getAllRemoved();
        for (LoadedItem loadedItem : itemList) {
          myArtifactStringCache.remove(loadedItem);
        }
      }
    }));
  }

  private void showTable() {
    myDataPlace.show(myScrollpane);
    if (myFocusOnTable) {
      JComponent c = myArtifactsTree.getSwingComponent();
      assert c.isDisplayable();
      if (c.isDisplayable()) {
        c.requestFocusInWindow();
      }
      myFocusOnTable = false;
    }
  }

  private void setupTable(final HierarchicalTable<LoadedItem> table) {
    table.setRootVisible(false);
    table.setTreeColumnIndex(1);
    table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    table.addGlobalRoles(ItemWrapper.ITEM_WRAPPER, ItemWrapper.FIRST_ITEM_WRAPPER,
      ItemWrapper.LAST_ITEM_WRAPPER, LoadedItem.LOADED_ITEM);
    table.setShowRootHanders(true);
    table.setTransfer(new ItemsContextTransfer());

    MenuBuilder builder = new MenuBuilder();
    builder.addDefaultAction(MainMenu.Edit.EDIT_ITEM);
    builder.addSeparator();
    builder.addAction(MainMenu.Search.OPEN_ITEM_IN_FRAME);
    builder.addAction(MainMenu.Search.OPEN_ITEM_IN_TAB);
    builder.addAction(MainMenu.Search.OPEN_ITEM_IN_BROWSER);
    builder.addSeparator();
    builder.addAction(MainMenu.Edit.DOWNLOAD)
      .addAction(MainMenu.Edit.DOWNLOAD_ATTACHMENTS)
      .addAction(MainMenu.Edit.UPLOAD)
      .addAction(MainMenu.Edit.VIEW_CHANGES)
      .addAction(MainMenu.Edit.VIEW_ATTRIBUTES)
      .addAction(MainMenu.Edit.VIEW_SHADOWS)
      .addAction(MainMenu.Edit.MERGE)
      .addAction(MainMenu.Edit.DISCARD)
      .addAction(MainMenu.Edit.VIEW_PROBLEMS)
      .addAction(MainMenu.Tools.DUPLICATE_PUBLIC_BUG)
      .addSeparator()
      .addAction(MainMenu.Edit.TAG)
      .addAction(MainMenu.Edit.COPY_ID_SUMMARY)
      .addAction(MainMenu.Edit.CUSTOM_COPY);
    if (!ExplorerForm.DISABLE_CLIPBOARD_IN_POPUPS) {
      builder.addAction(TransferAction.COPY);
      builder.addAction(TransferAction.PASTE);
    }
    builder.addSeparator()
      .addEntry(IssueDrivenPopupEntry.ENTRY);

    builder.addToComponent(Lifespan.FOREVER, table.getSwingComponent());
    UIUtil.keepSelectionOnRemove(table);
    table.setGridHidden();
    ImmediateTooltips.installImmediateTooltipManager(Lifespan.FOREVER, table.getSwingComponent(),
      TooltipLocationProvider.UNDER_TABLE_CELL);
  }

  private void setupTableHeader()
  {
    final SortingTableHeaderController<LoadedItem> header = myArtifactsTree.getHeaderController();
    header.setResizingAllowed(true);
    final ConfigureColumnsAction configureAction = createConfigureHeaderAction(header);
    configureAction.addCornerButton(myScrollpane);

    JTableHeader tableHeader = myArtifactsTree.getTable().getSwingHeader();
    MenuBuilder builder = new MenuBuilder();
    builder.addAction(createRemoveColumnAction(myArtifactsTree));
    builder.addAction(ATable.createForceColumnWidthAction(myArtifactsTree.getTable()));
    builder.addSeparator();
    appendAddColumnsActions(builder, myArtifactsTree);
    builder.addAction(configureAction);
    builder.addAction(ATable.createAutoAdjustAllColumnsAction(myArtifactsTree.getTable()));
    builder.addToComponent(Lifespan.FOREVER, tableHeader);
  }

  private void appendAddColumnsActions(MenuBuilder builder, HierarchicalTable<LoadedItem> table) {
    @Nullable Connection connection = null;
    if (myContextInfo != null) {
      connection = myContextInfo.getSourceConnection();
    } else assert false;

    ArtifactTableColumns.ColumnsSet<?> auxColumns = myColumnsCollector.getColumns(connection).getAux();
    boolean auxColsAbsent = auxColumns == null || auxColumns.model.getSize() == 0;
    if (auxColsAbsent) {
      builder.addAction(new InsertColumnsAction(table, getMainColumns(), "Columns", false));
    } else {
      builder.addAction(new InsertColumnsAction(table, getMainColumns(), Terms.ref_Main_columns, false));
      builder.addAction(new InsertColumnsAction(table, getAuxColumns(), Terms.ref_Auxiliary_columns, true));
    }
  }

  private Factory<ArtifactTableColumns.ColumnsSet<?>> getMainColumns() {
    return new Factory<ArtifactTableColumns.ColumnsSet<?>>() {
      @Override
      public ArtifactTableColumns.ColumnsSet<?> create() {
        ItemCollectionContext ci = getContextInfo();
        return ci != null ? myColumnsCollector.getColumns(ci.getSourceConnection()).getMain() : ArtifactTableColumns.ColumnsSet.empty();
      }
    };
  }

  private Factory<ArtifactTableColumns.ColumnsSet<?>> getAuxColumns() {
    return new Factory<ArtifactTableColumns.ColumnsSet<?>>() {
      @Override
      public ArtifactTableColumns.ColumnsSet<?> create() {
        ItemCollectionContext ci = getContextInfo();
        return ci != null ? myColumnsCollector.getColumns(ci.getSourceConnection()).getAux() : null;
      }
    };
  }


  private ConfigureColumnsAction createConfigureHeaderAction(final SortingTableHeaderController<LoadedItem> header) {
    return new ConfigureColumnsAction(new Factory<ArtifactTableColumns<LoadedItem>>() {
      @Override
      public ArtifactTableColumns<LoadedItem> create() {
        ItemCollectionContext contextInfo = getContextInfo();
        //noinspection ConstantConditions
        return contextInfo != null ? myColumnsCollector.getColumns(contextInfo.getSourceConnection()) : null;
      }
    }, new Factory<SubsetModel<? extends TableColumnAccessor<LoadedItem, ?>>>() {
      @Override
      public SubsetModel<? extends TableColumnAccessor<LoadedItem, ?>> create() {
        return header.getUserColumnsSubsetModel();
      }
    });
  }

  private ItemCollectionContext getContextInfo() {
    if (myContextInfo == null) {
      assert false;
      Log.warn("TCI: context info");
      return null;
    }
    return myContextInfo;
  }

  private static AnAction createRemoveColumnAction(final HierarchicalTable<?> table) {
    return new ATable.HeaderAction("Remove Column", null) {
      @Override
      protected ATable<?> getTable() {
        return table.getTable();
      }

      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        int mi = getColumnModelIndex(context);
        if (mi <= 0) throw new CantPerformException();
        TableColumnAccessor<?, ?> column = getColumnModel().getAt(mi);
        context.putPresentationProperty(PresentationKey.NAME, "Remove \"" + column.getName() + "\"");
      }

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        int mi = getColumnModelIndex(context);
        if (mi < 0) throw new CantPerformException();
        SubsetModel<? extends TableColumnAccessor<?, ?>> model =
          table.getHeaderController().getUserColumnsSubsetModel();
        model.removeAllAt(new int[] {mi - 1});
        table.getSwingHeader().setDraggedColumn(null);
      }
    };
  }

  @NotNull
  @Override
  public List<TableColumnAccessor<LoadedItem, ?>> getSelectedColumns() {
    ArrayList<TableColumnAccessor<LoadedItem, ?>> result = Collections15.arrayList();
    int columnCount = myArtifactsTree.getHeaderController().getSwingColumnModel().getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      TableColumnAccessor<?, ?> accessor = myArtifactsTree.getTable().getColumnAccessorAtVisual(i);
      if (accessor != null) result.add((TableColumnAccessor<LoadedItem, ?>) accessor);
    }
    return result;
  }

  @Override
  public boolean isLoadingDone() {
    ItemsCollectionController controller = myCurrentCollector;
    return controller != null && !controller.isLoading();
  }

  @NotNull
  @Override
  public SelectionAccessor<LoadedItem> getSelectedArtifacts() {
    return myArtifactsTree.getSelectionAccessor();
  }

  @Override
  public void updateAllArtifacts() {
    Threads.assertAWTThread();
    ItemsCollectionController collector = myCurrentCollector;
    if (collector != null)
      collector.updateAllItems();
  }

  @Override
  public AListModel<? extends LoadedItem> getCollectionModel() {
    return myArtifactsTree.getCollectionModel();
  }

  @Override
  public boolean isContentOutOfDate() {
    if (myCurrentCollector == null)
      return false;
    if (myCurrentCollector.isElementSetChanged())
      return true;
    AListModel<? extends LoadedItem> model = getCollectionModel();
    for (int i = 0; i < model.getSize(); i++)
      if (model.getAt(i).isOutOfDate())
        return true;
    return false;
  }

  @Override
  public void stopLoading() {
    Threads.assertAWTThread();
    ItemsCollectionController collector = myCurrentCollector;
    if (collector == null || !collector.isLoading())
      return;
    collector.cancelLoading(L.content(Terms.Query + " execution was cancelled"));
  }

  public void resetSource() {
    detachSource();
  }

  public SearchResult showSource(@NotNull ItemSource source, @NotNull ItemCollectionContext contextInfo, boolean focusToTable) {
    Threads.assertAWTThread();
    detachSource();
    myContextInfo = contextInfo;
    myHierarchy.setConfig(contextInfo, myConfig);
    myDatas.setSingleData(ItemCollectionContext.ROLE, contextInfo);
    myColumnsLife.cycle();
    myTab.setName(contextInfo.getShortName());
    myTab.setTooltip(contextInfo.getTooltip());
    myTab.setVisible(true);
    myCancelled = false;
    myTitle.showLoadingQuery(myContextInfo);
    if (focusToTable) {
      JComponent table = myArtifactsTree.getSwingComponent();
      if (table.isDisplayable()) {
        table.requestFocusInWindow();
      } else {
        myTab.requestFocusInWindow();
        myFocusOnTable = true;
      }
    }

    ItemsCollectionController itemsCollector = myExplorer.createLoader(this, source);

    //    ArtifactsCollectionController artifactsCollector = new ArtifactsCollectorImpl(editRegistry, this, source);
    SearchResult result = attachArtifactsCollector(itemsCollector);
    itemsCollector.reload();
    setupTableHeader();
    configureColumns();
    return result;
  }

  private void configureColumns() {
    ItemCollectionContext contextInfo = myContextInfo;
    assert contextInfo != null;
    SortingTableHeaderController<LoadedItem> headerController = myArtifactsTree.getHeaderController();
    Lifespan life = myColumnsLife.lifespan();
    headerController.setUserFullColumnsModel(life, myColumnsCollector.getColumns(contextInfo.getSourceConnection()).getAll(), false);
    myColumnsCollector.configureColumns(life, contextInfo.getContextConfig(), headerController);
  }

  private void createCopy(ContentTab tab, WorkflowComponent2 workflow) {
    ItemsCollectionController controller = myCurrentCollector;
    ItemCollectionContext contextInfo = myContextInfo;
    assert controller != null;
    assert contextInfo != null;
    TableControllerImpl copy = new TableControllerImpl(myConfig, tab, myColumnsCollector, myExplorer, contextInfo.getSourceConnection());
    copy.myContextInfo = contextInfo;
    copy.myHierarchy.setConfig(contextInfo, copy.myConfig);
    copy.setupTableHeader();
    copy.myDatas.setSingleData(ItemCollectionContext.ROLE, contextInfo);
    copy.myCancelled = myCancelled;
    tab.setName(contextInfo.getShortName());
    tab.setTooltip(contextInfo.getTooltip());
    tab.setVisible(true);
    copy.myTitle.setQueryInfo(contextInfo);
    myTitle.copyStateTo(copy.myTitle);
    ItemsCollectionController controllerCopy = controller.createCopy(copy);
    copy.myOutOfDateStripe.showStripe(!controllerCopy.getLifeModeModel().getValue().isLife());
    copy.attachArtifactsCollector(controllerCopy);

    LongList selection = PrimitiveUtils.collect(UiItem.GET_ITEM, getSelectedArtifacts().getSelectedItems());

    FlatCollectionComponent<LoadedItem> copiedTable = copy.myArtifactsTree;

    List<? extends LoadedItem> copiedArtifacts = copiedTable.getCollectionModel().toList();
    List<LoadedItem> newSelection = Collections15.arrayList();
    for (LoadedItem loadedItem : copiedArtifacts) {
      if (selection.contains(loadedItem.getItem()))
        newSelection.add(loadedItem);
    }
    copiedTable.getSelectionAccessor().setSelected(newSelection);
    copy.configureColumns();
  }

  private void detachSource() {
    Threads.assertAWTThread();
    myTab.setName(null);
    myTab.setTooltip(null);
    myTab.setVisible(false);
    mySourceDetach.detach();
    mySourceDetach = Detach.NOTHING;
    assert myCurrentCollector == null;
  }

  private static JLabel createMessage(String message) {
    JLabel label = UIUtil.createMessage(message);
    LookAndFeel.installColorsAndFont(label, "Table.background", "Table.foreground", "Table.font");
    Aqua.setLightNorthBorder(label);
    return label;
  }

  @Nullable
  public static TableControllerImpl findIn(@Nullable ContentTab tab) {
    return tab != null ? tab.getUserProperty(DATA_KEY) : null;
  }

  public boolean isCancelled() {
    return myCancelled;
  }

  private static class CloneTabAction extends SimpleAction {
    public CloneTabAction() {
      super(L.actionName("Open Duplicate Tab"));
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Open a new tab with the same contents");
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      TableController tab = context.getSourceObject(DATA_ROLE);
      context.setEnabled(tab.isLoadingDone());
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      WorkflowComponent2 workflow = context.getSourceObject(WorkflowComponent2.ROLE);
      ContentTab tab = context.getSourceObject(TabsManager.ROLE).createTab();
      ((TableControllerImpl) context.getSourceObject(DATA_ROLE)).createCopy(tab, workflow);
    }
  }


  private boolean matchesPattern(LoadedItem value) {
    final Pattern pattern = myFilterPattern.getValue();
    WeakReference<String> ref = myArtifactStringCache.get(value);
    String str = ref == null ? null : ref.get();
    if (str == null) {
      StringBuilder b = new StringBuilder(512);
      for (ModelKey key : value.getMetaInfo().getKeys()) {
        matchesPatternSwitch(key, value.getValues(), b);
      }
      str = b.toString();
      myArtifactStringCache.put(value, new WeakReference<String>(str));
    }
    Matcher m = pattern.matcher(str);
    return m.find();
  }

  private void matchesPatternSwitch(ModelKey key, PropertyMap map, StringBuilder buf) {
    Object val = key.getValue(map);
    if (val != null) {
      String str = null;
      if (val instanceof ItemKey) {
        str = ((ItemKey) val).getDisplayName();
      } else if (val instanceof String) {
        str = (String) val;
      } else if (val instanceof Number) {
        str = String.valueOf(val);
      } else if (val instanceof Collection) { //for custom fields and comments
        for (Object v : (Collection) val) {
          if (v instanceof Comment) {
            buf.append(((Comment) v).getText()).append(' ');
          } else if (v instanceof Attachment) {
            buf.append(((Attachment) v).getDisplayName());
          } else if (v instanceof ItemKey) {
            buf.append(((ItemKey) v).getDisplayName());
          }
        }
      }
      if (str != null) {
        buf.append(str).append(' ');
      }
    }
  }

  private class ArtifactShowCondition extends Condition<LoadedItem> {
    @Override
    public boolean isAccepted(LoadedItem value) {
      if (myFilterMatched && myFilterPattern.getValue().pattern() != "") {
        return matchesPattern(value);
      } else {
        return true;
      }
    }
  }


  private static class BulkActionPanel implements ChangeListener {
    private final JPanel myPanel;
    private final JLabel myLabel;
    private final Set<MetaInfo> myMetaInfos = Collections15.linkedHashSet();
    private final SelectionAccessor<LoadedItem> myAccessor;
    private int myLastSelectedCount;

    public BulkActionPanel(SelectionAccessor<LoadedItem> accessor) {
      myAccessor = accessor;
      myPanel = new JPanel(new BorderLayout());
      myLabel = new JLabel("Apply to selected:");
      myPanel.setBorder(new EmptyBorder(0, 14, 0, 0));
      myPanel.add(myLabel, BorderLayout.CENTER);
    }

    @Override
    public void onChange() {
      List<LoadedItem> items = myAccessor.getSelectedItems();
      int size = items.size();
      if (size < 2) {
        if (myPanel.isVisible()) {
          myPanel.setVisible(false);
        }
        return;
      }
      if (size != myLastSelectedCount) {
        myLastSelectedCount = size;
        myLabel.setText("Apply to " + size + " selected:");
      }
      boolean diff = false;
      for (LoadedItem item : items) {
        if (myMetaInfos.add(item.getMetaInfo())) {
          diff = true;
        }
      }
      if (diff) {
        Set<ToolbarEntry> commonToolbar = Collections15.linkedHashSet();
        for (MetaInfo metaInfo : myMetaInfos) {
          ToolbarBuilder b = metaInfo.getToolbarBuilder(false);
          if (b != null) {
            commonToolbar.addAll(b.getEntries());
          }
        }
        ToolbarBuilder builder = ToolbarBuilder.smallEnabledButtons();
        builder.addAll(commonToolbar);
        myPanel.add(builder.createHorizontalToolbar(), BorderLayout.EAST);
      }
      if (!myPanel.isVisible()) {
        myPanel.setVisible(true);
      }
    }

    public JComponent getComponent() {
      return myPanel;
    }
  }
}