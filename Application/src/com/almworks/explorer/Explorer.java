package com.almworks.explorer;

import com.almworks.api.application.*;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemProvider;
import com.almworks.api.platform.ProductInformation;
import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.tabs.ContentTab;
import com.almworks.util.components.tabs.TabsManager;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ModelUtils;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import java.util.Collection;
import java.util.List;

/**
 * @author : Dyoma
 */
class Explorer {
  private static final TypedKey<Boolean> DEFAULT_TAB = TypedKey.create("default");
  private static final TypedKey<TabKey> QUERY_TAB_KEY = TypedKey.create("queryKey");

  private final ExplorerForm myForm;
  private final Configuration myFormConfig;
  private final ApplicationLoadStatus myAppStatus;
  private final ColumnsCollector myColumnsCollector;
  private final ExplorerComponent myExplorer;


  public Explorer(Configuration configuration, Collection<ItemProvider> providers, ProductInformation productInfo,
                  final ValueModel<SelectionAccessor<LoadedItem>> selectionModelModel, ApplicationLoadStatus appStatus,
                  ColumnsCollector columnsCollector, ExplorerComponent explorer, Engine engine)
  {
    myExplorer = explorer;
    myAppStatus = appStatus;
    myFormConfig = configuration.getOrCreateSubset("layout");
    myForm = new ExplorerForm(myFormConfig, explorer, providers, productInfo, appStatus, engine);
    myColumnsCollector = columnsCollector;
    getTabsManager().addSelectionListener(new ChangeListener1<ContentTab>() {
      public void onChange(ContentTab tab) {
        SelectionAccessor<LoadedItem> selectionModel = null;
        TableControllerImpl controller = TableControllerImpl.findIn(tab);
        if (controller != null) {
          selectionModel = controller.getSelectedArtifacts();
        } else {
          selectionModel = ItemViewerController.findSingleSelectionInTab(tab);
        }
        selectionModelModel.setValue(selectionModel);
      }
    }, ThreadGate.STRAIGHT);
    JComponent defaultComponent = new JPanel();
    defaultComponent.setBackground(UIUtil.getEditorBackground());
    defaultComponent = new ScrollPaneBorder(defaultComponent);
    Aqua.cleanScrollPaneBorder(defaultComponent);
    getTabsManager().setDefaultComponent(defaultComponent);
  }

  JPanel getComponent() {
    return myForm.getWholePanel();
  }

  SearchResult setCollectionContext(final ItemSource source, ItemCollectionContext contextInfo, boolean focusToTable) {
    ContentTab tab = findOrCreateTab(contextInfo);
    if (tab == null)
      return SearchResult.EMPTY;
    tab.setUserProperty(QUERY_TAB_KEY, contextInfo.getQueryKey());
    TableControllerImpl tableController =
      new TableControllerImpl(myFormConfig, tab, myColumnsCollector, myExplorer, contextInfo.getSourceConnection());
    SearchResult result = tableController.showSource(source, contextInfo, focusToTable);
    if (contextInfo.isUseShortNameForHighlight()) {
      tableController.setSearchFilterString(contextInfo.getShortName(), false, false, false);
    }
    return result;
  }

  void setItemContext(final LoadedItem item, ItemViewerController itemViewerController) {
    ContentTab tab = itemViewerController.getItemTab(item);
    if (tab == null) {
      tab = getTabsManager().createTab();
      itemViewerController.addItemInNewTab(item, tab);
    } else {
      tab.select();
    }
  }

  /**
   * @return null if no query restart is requered. Not null to (re)start query in returned tab.
   */
  @Nullable
  private ContentTab findOrCreateTab(ItemCollectionContext context) {
    List<ContentTab> tabs = getTabsManager().getTabs();
    for (ContentTab tab : tabs) {
      TabKey tabKey = tab.getUserProperty(QUERY_TAB_KEY);
      if (context.isSameType(tabKey)) {
        tab.select();
        if (context.isReplaceTab(tabKey)) {
          return tab;
        } else {
          TableControllerImpl controller = TableControllerImpl.findIn(tab);
          if (controller == null)
            return null;
          return controller.isCancelled() ? tab : null;
        }
      }
    }
    return getTabsManager().createTab();
  }

  public ATree<ATreeNode<GenericNode>> getQueriesTree() {
    return myForm.getNavigationTree();
  }

  public void setupWelcome() {
    myForm.getNavigationTree().getModel().addTreeModelListener(new TreeModelAdapter() {
      public void treeModelEvent(TreeModelEvent e) {
        checkForWelcome();
      }
    });
    ModelUtils.whenTrue(myAppStatus.getApplicationLoadedModel(), ThreadGate.AWT, new Runnable() {
      public void run() {
        checkForWelcome();
      }
    });
    checkForWelcome();
  }

  private void checkForWelcome() {
    myForm.checkForWelcome();
  }

  private ContentTab getDefaultTab() {
    TabsManager tabsManager = getTabsManager();
    for (ContentTab tab : tabsManager.getTabs())
      if (Boolean.TRUE.equals(tab.getUserProperty(DEFAULT_TAB))) {
        tab.select();
        return tab;
      }
    ContentTab tab = tabsManager.createTab();
    tab.setUserProperty(DEFAULT_TAB, Boolean.TRUE);
    return tab;
  }

  public void showComponent(UIComponentWrapper component, String name) {
    ContentTab defaultTab = getDefaultTab();
    defaultTab.setComponent(component);
    defaultTab.setName(name);
  }

  @NotNull
  private TabsManager getTabsManager() {
    return myForm.getTabsManager();
  }
}
