package com.almworks.explorer;

import com.almworks.api.application.*;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.application.tree.TagNode;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemProvider;
import com.almworks.api.explorer.ApplicationToolbar;
import com.almworks.api.gui.*;
import com.almworks.api.platform.ProductInformation;
import com.almworks.api.syncreg.SyncRegistry;
import com.almworks.explorer.loader.ItemModelRegistryImpl;
import com.almworks.explorer.tree.NavigationTree;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.util.Getter;
import com.almworks.util.Terms;
import com.almworks.util.components.ATree;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.HighlighterTreeElement;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.tabs.TabsManager;
import com.almworks.util.config.Configuration;
import com.almworks.util.events.EventSource;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.model.ValueModel;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.Collection;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class ExplorerComponentImpl implements Startable, ExplorerComponent {
  public static final DataRole<TabsManager> MAIN_TABS_MANAGER = DataRole.createRole(TabsManager.class, "MainTabs");
  public static final ComponentProperty<ExplorerComponent> COMPONENT_KEY = ComponentProperty.createProperty("Explorer");
  private final ItemModelRegistryImpl myEditRegistry;
  private final Configuration myConfiguration;
  private final Engine myEngine;
  private final ActionRegistry myActionRegistry;

  private final MainWindowManager myWindowManager;
  private final NavigationTree myTree;

  private Explorer myExplorer = null;

  private final ProductInformation myProductInfo;
  private final ValueModel<SelectionAccessor<LoadedItem>> myCurrentSelection = ValueModel.create();
  private final ComponentContainer myContainer;
  private final ApplicationToolbar myToolbar;
  private final SyncRegistry mySyncRegistry;
  private final TimeTracker myTimeTracker;

  private final Map<TypedKey, KeyedHighlighting> myHighlighting = Collections15.hashMap();
  private ItemViewerController myController;
  private final ApplicationLoadStatus.StartupActivity myStartup;

  public ExplorerComponentImpl(MainWindowManager windowManager, Configuration configuration,
    ItemModelRegistryImpl editRegistry, ComponentContainer container, Engine engine, ProductInformation productInfo,
    ActionRegistry actionRegistry, ApplicationToolbar toolbar,
    SyncRegistry syncRegistry, TimeTracker timeTracker, ApplicationLoadStatus startup)
  {
    myProductInfo = productInfo;
    myWindowManager = windowManager;
    myConfiguration = configuration;
    myEditRegistry = editRegistry;
    myEngine = engine;
    myActionRegistry = actionRegistry;
    myContainer = container;
    myToolbar = toolbar;
    mySyncRegistry = syncRegistry;
    myTimeTracker = timeTracker;
    myTree = container.createSubcontainer("navigationTree").instantiate(NavigationTree.class);
    myStartup = startup.createActivity("explorer");
  }

  public SearchResult showItemsInTab(@NotNull ItemSource source, ItemCollectionContext contextInfo,
    boolean focusToTable)
  {
    return myExplorer.setCollectionContext(source, contextInfo, focusToTable);
  }

  public void showItemInTab(@NotNull ItemWrapper item) {
    if (item instanceof LoadedItem) {
      ItemViewerController controller = getArtifactViewerController();
      myExplorer.setItemContext((LoadedItem) item, controller);
    } else {
//      assert false : item;
      Log.warn(
        "not a loaded item: " + item + " " + (item == null ? "" : String.valueOf(item.getClass())));
    }
  }

  public void showItemInWindow(@NotNull ItemWrapper item) {
    ItemViewerController controller = getArtifactViewerController();
    FrameBuilder frameBuilder = controller.getItemFrame(item);
    if (frameBuilder == null) {
      if (item instanceof LoadedItem) {
        LoadedItem loadedItem = (LoadedItem) item;
        frameBuilder = myContainer.requireActor(WindowManager.ROLE).createFrame("ItemWrapper");
        controller.addItemInNewWindow(loadedItem, frameBuilder);
      } else {
        assert false : item;
        Log.warn(
          "not a loaded item: " + item + " " + (item == null ? "" : String.valueOf(item.getClass())));
      }
    } else {
      WindowController windowController = frameBuilder.getWindowContainer().requireActor(WindowController.ROLE);
      windowController.activate();
    }
  }

  private ItemViewerController getArtifactViewerController() {
    if (myController == null) {
      myController = new ItemViewerController(myConfiguration.createSubset("avc"), this);
    }
    return myController;
  }

  public void showComponent(UIComponentWrapper component, String name) {
    myExplorer.showComponent(component, name);
  }

  public void start() {
    ThreadGate.AWT_IMMEDIATE.execute(() -> {
      ApplicationLoadStatus appStatus = myContainer.getActor(ApplicationLoadStatus.ROLE);
      myExplorer = new Explorer(myConfiguration.getOrCreateSubset("explorer"), myEngine.getConnectionManager().getProviders(), myProductInfo, myCurrentSelection,
        appStatus, myContainer.getActor(ColumnsCollector.ROLE), ExplorerComponentImpl.this, myEngine);
    });
    registerActions();

    final EventSource<ScalarModel.Consumer<Boolean>> events = mySyncRegistry.getStartedModel().getEventSource();
    events.addAWTListener(Lifespan.FOREVER, new ScalarModel.Adapter<Boolean>() {
      public void onScalarChanged(ScalarModelEvent<Boolean> event) {
        Boolean value = event.getNewValue();
        if (value == null || !value)
          return;
        events.removeListener(this);
        doStart();
      }
    });
    myStartup.doneOn(myTree.isReady());
  }

  @ThreadAWT
  private void doStart() {
    myTree.start(getNavigationTree());
    final JComponent component = myExplorer.getComponent();
    final JComponent toolbar = myToolbar.getComponent();
    Aqua.addSouthBorder(toolbar);
    component.add(toolbar, BorderLayout.NORTH);
    myTree.setupTree();
    COMPONENT_KEY.putClientValue(myExplorer.getComponent(), this);
    myWindowManager.setContentComponent(component);
    myWindowManager.showWindow(true);
    myExplorer.setupWelcome();
  }

  public ATree<ATreeNode<GenericNode>> getNavigationTree() {
    Explorer explorer = myExplorer;
    return explorer == null ? null : explorer.getQueriesTree();
  }

  private void registerActions() {
    myActionRegistry.registerAction(MainMenu.File.NEW_CONNECTION, new SimpleAction() {
      {
        setDefaultText(PresentationKey.NAME, "&New " + Terms.ref_ConnectionType + " Connection\u2026");
        setDefaultText(PresentationKey.SHORT_DESCRIPTION, "Create a new connection");
      }

      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.setEnabled(getTheProvider() != null);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        // todo: better treatment of no providers/multiple providers
        final ItemProvider provider = getTheProvider();
        if(provider != null) {
          provider.showNewConnectionWizard();
        }
      }

      private ItemProvider getTheProvider() {
        final Collection<ItemProvider> providers = myEngine.getConnectionManager().getProviders();
        return providers == null || providers.isEmpty() ? null : providers.iterator().next();
      }
    });
  }

  public void stop() {
    myWindowManager.showWindow(false);
    myWindowManager.setContentComponent(null);
    myExplorer = null;
  }

  public void createDefaultQueries(Connection connection, Runnable whenCreated) {
    myTree.createDefaultQueries(connection, whenCreated);
  }

  public void expandConnectionNode(Connection connection, boolean expandAll) {
    myTree.expandConnectionNode(connection, expandAll);
  }

  @Override
  public Map<DBIdentifiedObject, TagNode> getTags() {
    return myTree.getTags();
  }

  @Nullable
  public RootNode getRootNode() {
    return myTree.getRootNode();
  }

  @Override
  public Engine getEngine() {
    return myEngine;
  }

  public void setHighlightedNodes(TypedKey highlightKey, Collection<? extends GenericNode> nodes, Color color,
    Icon icon, String caption)
  {
    if (nodes == null || nodes.size() == 0) {
      clearHighlightedNodes(highlightKey);
    } else {
      ATree<ATreeNode<GenericNode>> tree = getNavigationTree();
      if (tree == null)
        return;
      myHighlighting.put(highlightKey, new KeyedHighlighting(nodes, color, icon, caption));
      resetHighlighting(tree);
    }
  }

  private void resetHighlighting(ATree<ATreeNode<GenericNode>> tree) {
    tree.removeAllHighlighted();
    for (KeyedHighlighting highlighting : myHighlighting.values()) {
      for (final GenericNode node : highlighting.myNodes) {
        tree.addHighlightedElement(HighlighterTreeElement.simple(new Getter<TreeNode>() {
          public TreeNode get() {
            return node.getTreeNode();
          }
        }, highlighting.myColor, highlighting.myIcon, highlighting.myCaption));
      }
    }
  }

  public void clearHighlightedNodes(TypedKey highlightKey) {
    ATree<ATreeNode<GenericNode>> tree = getNavigationTree();
    if (tree == null)
      return;
    if (myHighlighting.remove(highlightKey) != null)
      resetHighlighting(tree);
  }

  @NotNull
  public JComponent getGlobalContextComponent() {
    assert myExplorer != null : this;
    JPanel component = myExplorer.getComponent();
    assert component != null : myExplorer;
    return component;
  }

  public void whenReady(final ThreadGate gate, final Runnable runnable) {
    Boolean ready = myTree.isReady().getValue();
    if (ready != null && ready) {
      gate.execute(runnable);
    } else {
      final DetachComposite life = new DetachComposite();
      myTree.isReady().getEventSource().addListener(life, gate, new ScalarModel.Adapter<Boolean>() {
        public void onScalarChanged(ScalarModelEvent<Boolean> event) {
          if (life.isEnded())
            return;
          Boolean v = event.getNewValue();
          if (v != null && v) {
            gate.execute(runnable);
            life.detach();
          }
        }
      });
    }
  }

  public void selectNavigationNode(GenericNode node) {
    myTree.selectNode(node);
  }


  public ItemsCollectionController createLoader(ItemCollectorWidget widget, ItemSource source) {
    return new ItemsCollectorImpl(myEditRegistry, myTimeTracker, widget, source);
  }

  private static class KeyedHighlighting {
    private final Collection<? extends GenericNode> myNodes;
    private final Color myColor;
    private final Icon myIcon;
    private final String myCaption;

    public KeyedHighlighting(Collection<? extends GenericNode> nodes, Color color, Icon icon, String caption) {
      myNodes = Collections15.arrayList(nodes);
      myColor = color;
      myIcon = icon;
      myCaption = caption;
    }
  }
}
