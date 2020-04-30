package com.almworks.jira.provider3.links;

import com.almworks.api.application.ItemDownloadStageKey;
import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.UIController;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.jira.provider3.gui.ItemKeyIconsValue;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.schema.IssueKeyComparator;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Function;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.tables.HierarchicalTable;
import com.almworks.util.components.tables.TreeToListAdapter;
import com.almworks.util.config.Configuration;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.almworks.util.collections.Functional.filterArray;
import static com.almworks.util.commons.Condition.notNull;

/**
 * The links are displayed as a table with elements which form a tree of depth 2. Row type is {@code Pair<Object, ?>} with the first element being the row payload. Its actual type depends on the level.</br>
 * <table>
 *   <tr><td>Level 0</td><td>{@code String[]}</td><td>Root - the issue for which the links are displayed.</td></tr>
 *   <tr><td>Level 1</td><td>{@code String}</td><td>Link direction name</td></tr>
 *   <tr><td>Level 2</td><td>{@link LoadedLink2}</td><td>The link itself</td></tr>
 * </table>
  * */
@SuppressWarnings({"RawUseOfParameterizedType"})
class LinksController implements UIController<ATable> {
  private static final CanvasRenderer<Object> SUMMARY_RENDERER = new MySummaryRenderer();
  private static final String LINKS_ROOT = "_links_";

  private final ModelKey<List<LoadedLink2>> myKey;
  private final List<Updater> myUpdaters = Collections15.arrayList();
  private final ModelKey<String> myIssueKeyKey;
  private final Configuration myConfig;
  private final Set<String> myWereCollapsed = Collections15.hashSet();

  private final MenuBuilder myPopup = new MenuBuilder();
  private final GuiFeaturesManager myFeatures;

  private LinksController(GuiFeaturesManager features, Configuration config) {
    myFeatures = features;
    myKey = LoadedLink2.getLinksKey(features);
    myIssueKeyKey = MetaSchema.issueKey(features);
    myConfig = config;
    Collection<String> settings = myConfig.getAllSettingNames();
    for (String setting : settings) {
      myWereCollapsed.add(setting);
    }
  }

  public static LinksController install(GuiFeaturesManager features, ATable<Pair<Object, TreeModelBridge<?>>> table, Configuration collapseConfig) {
    LoadLinkProvider.install(table, LoadedLink2.DB_LINK);
    setupHierarchy(table, features);
    LinksController controller = new LinksController(features, collapseConfig);
    CONTROLLER.putClientValue(table, controller);
    return controller;
  }

  public void addPopupAction(AnAction action) {
    myPopup.addAction(action);
  }

  public void addDefaultPopupAction(AnAction action) {
    myPopup.addDefaultAction(action);
    myPopup.addSeparator();
  }

  public void connectUI(@NotNull Lifespan lifespan, @NotNull final ModelMap model, @NotNull final ATable component) {
    final HierarchicalTable table = HierarchicalTable.getHierarchicalTable(component);
    LinksTree tree = LinksTree.create(myFeatures, null);
    table.setRoot(tree.getRoot());
    table.sortBy(0, false);
    TreeUpdater updater = new TreeUpdater(model, tree, component);
    model.addAWTChangeListener(lifespan, updater);
    updater.onChange();
    table.getSelectionAccessor().clearSelection();
    final TreeToListAdapter adapter = table.getTreeAdapter();
    assert adapter != null : table;
    if (myWereCollapsed.contains(LINKS_ROOT)) {
      adapter.collapse(tree.getRoot().getPathFromRoot());
    } else {
      adapter.expand(tree.getRoot().getPathFromRoot());
      expandTypes(tree.getRoot(), adapter);
    }
    adapter.addExpansionListener(new MyTreeExpansionListener(tree.getRoot(), adapter));
    myPopup.addToComponent(lifespan, component.getSwingComponent());
  }

  private void expandTypes(TreeModelBridge root, TreeToListAdapter adapter) {
    for (int i = 0; i < root.getChildCount(); i++) {
      TreeModelBridge<Pair<String, TreeModelBridge>> type = root.getChildAt(i);
      Pair<String, TreeModelBridge> userObject = type.getUserObject();
      if (userObject != null) {
        String typeName = userObject.getFirst();
        if (myWereCollapsed.contains(typeName))
          adapter.collapse(type.getPathFromRoot());
        else
          adapter.expand(type.getPathFromRoot());
      }
    }
  }

  static HierarchicalTable setupHierarchy(ATable<? extends Pair<?, TreeModelBridge<?>>> table, GuiFeaturesManager features) {
    HierarchicalTable tree = new HierarchicalTable(table);

    TableColumnBuilder<Pair<Object, ?>, Object> idBuilder = TableColumnBuilder.create();
    idBuilder.setConvertor(Pair.<Object>convertorGetFirst());
    List<Pair<String, Function<Object, ItemKey>>> itemKeyGetters = filterArray(notNull(),
      EnumValueGetter.create(features, "Status", LoadedLink.STATUS),
      EnumValueGetter.create(features, "Type", LoadedLink.ISSUE_TYPE),
      EnumValueGetter.create(features, "Priority", LoadedLink.PRIORITY)
    );
    idBuilder.setValueCanvasRenderer(new MyIdRenderer(itemKeyGetters));
    idBuilder.setId("id");
    idBuilder.setValueComparator(new MyLinksComparator(features, ItemKeyIconsValue.comparator(itemKeyGetters)));
    idBuilder.setSizePolicy(ColumnSizePolicy.FIXED);
    idBuilder.setValueTooltipProvider(ItemKeyIconsValue.iconTooltip(itemKeyGetters, ItemKeyIconsValue.RowIconCreator.NO_GAPS));

    TableColumnAccessor<Pair<Object, ?>, Object> idColumn = idBuilder.createColumn();
    TableColumnBuilder<Pair<Object, ?>, Object> summaryBuilder = TableColumnBuilder.create();
    summaryBuilder.setConvertor(Pair.<Object>convertorGetFirst());
    summaryBuilder.setId("summary");
    summaryBuilder.setValueCanvasRenderer(SUMMARY_RENDERER);

    tree.setColumnModel(FixedListModel.<TableColumnAccessor<Pair<Object, ?>, Object>>create(idColumn, summaryBuilder.createColumn()));
    tree.sortBy(idColumn, false);
    tree.setGridHidden();
    tree.setShowRootHanders(true);
    tree.setResetTreeColumnSizeCache(true);
    return tree;
  }

  public void update(Updater updater) {
    myUpdaters.add(updater);
  }

  public static interface Updater {
    void update(LinksTree tree);
  }


  private class TreeUpdater implements ChangeListener {
    private final ModelMap myModel;
    private final LinksTree myTree;
    private final ATable myComponent;

    public TreeUpdater(ModelMap model, LinksTree tree, ATable component) {
      myModel = model;
      myTree = tree;
      myComponent = component;
    }

    public void onChange() {
      String key = Util.NN(myIssueKeyKey.getValue(myModel));
      if (key.length() == 0 && !ItemDownloadStageKey.isUploaded(myModel)) key = "New Issue";
      myTree.setRootObject(key);

      myTree.update(myKey.getValue(myModel));
      for (Updater updater : myUpdaters) {
        updater.update(myTree);
      }
      myComponent.resizeColumns();
      myComponent.invalidate();
      myComponent.revalidate();
      myComponent.forcePreferredColumnWidth(0);
    }
  }


  private static class MySummaryRenderer implements CanvasRenderer<Object> {
    public void renderStateOn(CellState state, Canvas canvas, Object item) {
      if (item instanceof String[]) {
      } else if (item instanceof String) {
      } else if (item instanceof LoadedLink) {
        LoadedLink link = ((LoadedLink) item);
        String summary = link.getOppositeString(LoadedLink.SUMMARY);
        if (summary != null)
          canvas.appendText(summary);
      } else LogHelper.warning("LinksController.MySummaryRenderer: unknown item", item);
    }
  }


  private static class MyLinksComparator implements Comparator<Object> {
    private final GuiFeaturesManager myFeatures;
    private final Comparator<Object> myStpComparator;

    public MyLinksComparator(GuiFeaturesManager features, Comparator<Object> stpComparator) {
      myFeatures = features;
      myStpComparator = stpComparator;
    }

    public int compare(Object o1, Object o2) {
      if (o1 == o2) return 0;
      if (o1 == null) return -1;
      if (o2 == null) return 1;
      // root (L0)
      if (o1 instanceof String[]) return -1;
      if (o2 instanceof String[]) return 1;
      // L1, L2 or mixed
      String s1 = Util.castNullable(String.class, o1);
      String s2 = Util.castNullable(String.class, o2);
      LoadedLink l1 = Util.castNullable(LoadedLink.class, o1);
      LoadedLink l2 = Util.castNullable(LoadedLink.class, o2);
      if (s1 != null && s2 != null) {
        return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
      } else if (l1 != null && l2 != null) {
        String d1 = l1.getDescription(myFeatures);
        String d2 = l2.getDescription(myFeatures);
        int order = String.CASE_INSENSITIVE_ORDER.compare(d1, d2);
        if (order != 0) return order;
        order = IssueKeyComparator.INSTANCE.compare(l1.getOppositeString(LoadedLink.KEY), l2.getOppositeString(
          LoadedLink.KEY));
        if (order != 0) return order;
        return myStpComparator.compare(l1, l2);
      } else if ((l1 != null || l2 != null) && (s1 != null || s2 != null)) {
        String fromL = (l1 != null ? l1 : l2).getDescription(myFeatures);
        String fromS = s1 != null ? s1 : s2;
        return String.CASE_INSENSITIVE_ORDER.compare(fromL, fromS) * (l1 != null ? 1 : -1);
      } else LogHelper.error(o1, o2);
      return 0;
    }
  }

  private static class MyIdRenderer implements CanvasRenderer<Object> {
    private final List<Pair<String, Function<Object, ItemKey>>> myItemKeyGetters;

    private MyIdRenderer(List<Pair<String, Function<Object, ItemKey>>> itemKeyGetters) {
      myItemKeyGetters = itemKeyGetters;
    }

    public void renderStateOn(CellState state, Canvas canvas, Object item) {
      if (item instanceof String[]) {
        canvas.setFontStyle(Font.BOLD);
        canvas.appendText(((String[]) item)[0]);
      } else if (item instanceof String) {
        canvas.appendText((String) item);
      } else if (item instanceof LoadedLink) {
        LoadedLink link = ((LoadedLink) item);
        ItemKeyIconsValue.createPartialCell(link, myItemKeyGetters).renderOn(canvas, state);
        String oppositeKey = link.getOppositeString(LoadedLink.KEY);
        canvas.appendText(oppositeKey != null ? oppositeKey : "<new>");
        CanvasSection section = canvas.getCurrentSection();
        section.setBorder(canvas.getCanvasBorder());
        section.setBackground(canvas.getCanvasBackground());
        canvas.setCanvasBackground(null);
        canvas.setCanvasBorder(null);
      } else LogHelper.warning("LinksController.MyIdRenderer: unknown item", item);
    }
  }


  private static class EnumValueGetter implements Function<Object, ItemKey> {
    private final GuiFeaturesManager myManager;
    private final TypedKey<ItemKey> myKey;

    private EnumValueGetter(GuiFeaturesManager manager, TypedKey<ItemKey> key) {
      myManager = manager;
      myKey = key;
    }

    public static Pair<String, Function<Object, ItemKey>> create(GuiFeaturesManager features, String displayName, TypedKey<ItemKey> key) {
      Function<Object, ItemKey> getter = new EnumValueGetter(features, key);
      return Pair.create(displayName, getter);
    }

    @Override
    public ItemKey invoke(Object obj) {
      if (obj instanceof LoadedLink) return ((LoadedLink) obj).getOppositeEnum(myManager, myKey);
      else return null;
    }
  }


  private class MyTreeExpansionListener implements TreeExpansionListener {
    private final TreeModelBridge myRoot;
    private final TreeToListAdapter myAdapter;

    public MyTreeExpansionListener(TreeModelBridge root, TreeToListAdapter adapter) {
      myRoot = root;
      myAdapter = adapter;
    }

    public void treeExpanded(TreeExpansionEvent event) {
      String setting = saveSetting(event, true);
      if (!LINKS_ROOT.equals(setting))
        return;
      expandTypes(myRoot, myAdapter);
    }

    public void treeCollapsed(TreeExpansionEvent event) {
      saveSetting(event, false);
    }

    @Nullable
    private String saveSetting(TreeExpansionEvent event, boolean expanded) {
      String setting = getAffectedName(event);
      if (setting == null || setting.length() == 0)
        return setting;
      if (expanded)
        myWereCollapsed.remove(setting);
      else
        myWereCollapsed.add(setting);
      boolean markCollapsed = myConfig.isSet(setting);
      if (expanded && markCollapsed)
        myConfig.removeSettings(setting);
      else if (!expanded && !markCollapsed)
        myConfig.setSetting(setting, true);
      return setting;
    }

    @Nullable
    private String getAffectedName(TreeExpansionEvent event) {
      TreePath path = event.getPath();
      int pathCount = path.getPathCount();
      if (pathCount == 1)
        return LINKS_ROOT;
      if (pathCount > 2)
        return null;
      Pair<String, ?> userObject = ((TreeModelBridge<Pair<String, ?>>) path.getLastPathComponent()).getUserObject();
      return userObject == null ? null : userObject.getFirst();
    }
  }
}
