package com.almworks.explorer;

import com.almworks.actions.EditConnectionAction;
import com.almworks.actions.RetryInitializationAction;
import com.almworks.actions.ShowConnectionInfo;
import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemProvider;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.platform.ProductInformation;
import com.almworks.settings.engine.ToggleAutoSyncAction;
import com.almworks.tags.EditTagAction;
import com.almworks.tags.NewTagAction;
import com.almworks.util.AppBook;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.tabs.TabsManager;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.i18n.LText;
import com.almworks.util.i18n.LText1;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.TransferAction;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

/**
 * @author dyoma
 */
public class ExplorerForm {
  public static final boolean DISABLE_CLIPBOARD_IN_POPUPS =
    Env.getBoolean(GlobalProperties.HACK_DISABLE_CLIPBOARD_IN_POPUPS);

  public static final TypedKey<ExplorerForm> ROLE = DataRole.create(ExplorerForm.class);
  private static final String LOADING_CARD = "loading";
  private static final String WELCOME_CARD = "welcome";
  private static final String CONTENT_CARD = "content";

  private static final String X = "Application.Navigation.Delete.";
  private static final LText1<Integer> DEL_CONFIRMATION = AppBook.text(X + "DEL_CONFIRMATION",
    "Are you sure you want to remove {0,choice,1#the selected item|1<{0} selected items}?", 0);
  private static final LText DEL_CONFIRMATION_TITLE = AppBook.text(X + "DEL_CONFIRMATION_TITLE", "Remove");

  private final JPanel myWholePanel;
  private final ATree<ATreeNode<GenericNode>> myNavigation;
  private final TabsManager myTabsManager = new TabsManager();

  private final JPanel myBigWelcome;

  private final Collection<ItemProvider> myProviders;

  private WelcomeScreen myWelcomeOverall;

  private final ProductInformation myProductInfo;
  private final ApplicationLoadStatus myAppStatus;
  private final AdjustedSplitPane myContentAndNavigation;
  private final JSplitPane myNavigationArea;
  private int myHiddenNavAreaDividerPos = 0;
  private final SimpleModifiable myNavAreaModifiable = new SimpleModifiable();

  private final ExplorerDistributionTable mySumtable;
  private final JComponent myTableArea;
  private final Lifecycle myTableVisibleLife = new Lifecycle(false);
  private final Configuration myConfig;

  private boolean myWhatsNewShown;

  private static final String SUMTABLE_VISIBLE = "sumtableVisible";
  private static final String TREE_CARD_NAVIGATION = "nav-navigation";
  private static final String TREE_CARD_NEWCONNECTION = "nav-newConnection";
  private static final String QUERY_COLLECTION_SPLITTER = "queryCollectionSplitter";

  public ExplorerForm(Configuration config, ExplorerComponent explorer, Collection<ItemProvider> providers, ProductInformation productInfo,
    ApplicationLoadStatus appStatus, Engine engine)
  {
    myAppStatus = appStatus;
    DataProviders.globalizeAs(myTabsManager.getComponent(), TabsManager.ROLE, ExplorerComponentImpl.MAIN_TABS_MANAGER);
    myProductInfo = productInfo;
    myProviders = providers;

    myNavigation = new ATree<>();
    TREE_POPUP.addToComponent(Lifespan.FOREVER, myNavigation.getSwingComponent());
    myNavigation.setEditable(false);
    myNavigation.setRootVisible(false);
    myNavigation.setDoubleClickExpandEnabled(false);
    myNavigation.setShowsRootHandles(true);
    myNavigation.addKeyListener(new ATree.TreeKeyAdapter(myNavigation) {
      protected void keyPressed(KeyEvent e, Object[] selection) {
        if (e.getKeyCode() != KeyEvent.VK_DELETE)
          return;
        ActionBridge actionBridge = new ActionBridge(TransferAction.REMOVE, (JComponent) e.getComponent());
        try {
          actionBridge.startUpdate();
          if (!actionBridge.isEnabled())
            return;
        } finally {
          actionBridge.stopUpdate();
        }
        if (checkConfirmation(myNavigation, selection))
          actionBridge.performIfEnabled();
      }
    });

    final JScrollPane navigationTree;
    if (!Aqua.isAqua()) {
      navigationTree = new AScrollPane(myNavigation);
    } else {
      final JComponent treeMargin = new JPanel(new BorderLayout());
      treeMargin.setOpaque(false);
      treeMargin.setBorder(new EmptyBorder(5, 5, 0, 0));
      treeMargin.add(myNavigation, BorderLayout.CENTER);
      navigationTree = new AScrollPane(treeMargin);
      navigationTree.setBorder(null);
    }

    final AScrollPane newConnection = new AScrollPane(createNewConnectionPanel());
    Aqua.cleanScrollPaneBorder(newConnection);

    JPanel treeArea = new JPanel(new CardLayout());
    treeArea.add(navigationTree, TREE_CARD_NAVIGATION);
    treeArea.add(newConnection, TREE_CARD_NEWCONNECTION);

    myConfig = config;
    mySumtable =
      new ExplorerDistributionTable(explorer, myNavigation.getSelectionAccessor(), myConfig.getOrCreateSubset("sumtable"));
    myTableArea = SingleChildLayout.envelop(mySumtable.getComponent(), SingleChildLayout.CONTAINER);

    myNavigationArea =
      ConfiguredSplitPane.createTopBottom(treeArea, myTableArea, myConfig.getOrCreateSubset("treeTableSplit"), 0.7F);
    myNavigationArea.setResizeWeight(1F);
    myNavigationArea.setOneTouchExpandable(false);
    Aqua.makeLeopardStyleSplitPane(myNavigationArea);

    final JComponent contentArea = myTabsManager.getComponent();
    myContentAndNavigation =
      UIUtil.createSplitPane(myNavigationArea, contentArea, true, myConfig, QUERY_COLLECTION_SPLITTER, 0.25d, 320);
    myContentAndNavigation.setResizeWeight(0);
    Aqua.makeLeopardStyleSplitPane(myContentAndNavigation);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        myNavigation.getSwingComponent().requestFocusInWindow();
      }
    });

    Component loadingPanel = createLoadingPanel();

    myBigWelcome = new JPanel(new CardLayout());
    myBigWelcome.add(loadingPanel, LOADING_CARD);
    myBigWelcome.add(myContentAndNavigation, CONTENT_CARD);

    if (Aqua.isAqua()) {
      myWholePanel = new JPanel(new BorderLayout());
    } else {
      myWholePanel = new JPanel(new BorderLayout(1, 1));
      myWholePanel.setBorder(new EmptyBorder(0, 2, 0, 2));
    }

    myWholePanel.add(myBigWelcome, BorderLayout.CENTER);
    ConstProvider.addGlobalValue(myWholePanel, ROLE, this);
    showDistributionTable(myConfig.getBooleanSetting(SUMTABLE_VISIBLE, false));

    myTabsManager.getModifiable().addAWTChangeListener(Lifespan.FOREVER, new ChangeListener() {
      @Override
      public void onChange() {
        if(myTabsManager.getSelectedTab() == null && !isNavigationAreaShown()) {
          showNavigationArea(true);
        }
      }
    });
    ChangeListener cl = new NavigationAreaChanger(engine, treeArea, this);
    engine.getConnectionManager().getConnectionsModifiable().addChangeListener(Lifespan.FOREVER, cl);
    cl.onChange();
  }

  private JComponent createNewConnectionPanel() {
    final JPanel panel = new JPanel(new InlineLayout(InlineLayout.VERTICAL, 5, true));
    panel.setOpaque(false);
    panel.setBorder(UIUtil.BORDER_9);

    final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    toolbar.setBorder(AwtUtil.EMPTY_BORDER);
    toolbar.setOpaque(false);
    final AActionButton newConnection = new AActionButton(new IdActionProxy(MainMenu.File.NEW_CONNECTION));
    toolbar.add(newConnection);

    final ALabel label = new ALabel("<html>Please set up a connection.");
    label.setHorizontalAlignment(SwingConstants.CENTER);
    panel.add(label);
    panel.add(toolbar);

    final JPanel overall = new JPanel(new BorderLayout());
    overall.setOpaque(true);
    overall.setBackground(UIUtil.getEditorBackground());
    overall.add(panel, BorderLayout.WEST);
    overall.add(new JLabel(), BorderLayout.CENTER);

    return overall;
  }

  private Component createLoadingPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(21, 21, 21, 21));
    panel.add(new LoadingLabel(), BorderLayout.CENTER);
    return panel;
  }

  private static boolean checkConfirmation(Component c, Object[] selection) {
    if (selection == null || selection.length == 0)
      return false;
    int askResult = DialogsUtil.askUser(c, DEL_CONFIRMATION.format(selection.length), DEL_CONFIRMATION_TITLE.format(),
      DialogsUtil.YES_NO_OPTION);
    return DialogsUtil.YES_OPTION == askResult;
  }

  public JPanel getWholePanel() {
    return myWholePanel;
  }

  public ATree<ATreeNode<GenericNode>> getNavigationTree() {
    return myNavigation;
  }

  public void checkForWelcome() {
    final JPanel bigWelcome = myBigWelcome;
    final CardLayout layout = ((CardLayout) bigWelcome.getLayout());
    final Boolean loaded = myAppStatus.getApplicationLoadedModel().getValue();

    if (loaded == null || !loaded) {
      layout.show(bigWelcome, LOADING_CARD);
    } else if (!!myProviders.isEmpty()) {
      WelcomeScreen welcome = getWelcomeScreen();
      welcome.reset();
      layout.show(bigWelcome, WELCOME_CARD);
    } else {
      layout.show(bigWelcome, CONTENT_CARD);
      if (!myWhatsNewShown) {
        myWhatsNewShown = true;
        HintScreen hintScreen = Context.get(HintScreen.class);
        if (hintScreen != null) {
          hintScreen.showWhatsNew(true);
        }
      }
    }
  }

  private WelcomeScreen getWelcomeScreen() {
    if(myWelcomeOverall == null) {
      myWelcomeOverall = new WelcomeScreen(myProviders, myProductInfo, true);
      // kludge :(
      myBigWelcome.add(myWelcomeOverall.getComponent(), WELCOME_CARD);
    }
    return myWelcomeOverall;
  }

  @NotNull
  public TabsManager getTabsManager() {
    return myTabsManager;
  }

  public boolean isNavigationAreaShown() {
    return myContentAndNavigation.getDividerLocation() > 0;
  }

  public Modifiable getNavigationAreaModifiable() {
    return myNavAreaModifiable;
  }

  public void showNavigationArea(boolean show) {
    if(show != isNavigationAreaShown()) {
      if(show) {
        myContentAndNavigation.showDivider();
        myContentAndNavigation.setDividerLocation(myHiddenNavAreaDividerPos);
        myNavigationArea.setMinimumSize(null);
      } else {
        myHiddenNavAreaDividerPos = myContentAndNavigation.getDividerLocation();
        myNavigationArea.setMinimumSize(new Dimension(0, 0));
        myContentAndNavigation.setDividerLocation(0);
        myContentAndNavigation.hideDivider();
        myConfig.setSetting(QUERY_COLLECTION_SPLITTER, myHiddenNavAreaDividerPos);
        getTabsManager().requestFocusInWindow();
      }
      myNavAreaModifiable.fireChanged();
    }
  }

  public void showDistributionTable(boolean show) {
    myNavigationArea.setBottomComponent(show ? myTableArea : null);
    myConfig.setSetting(SUMTABLE_VISIBLE, show);
    if (!show) {
      myTableVisibleLife.cycleEnd();
      return;
    }
    if (!myTableVisibleLife.isCycleStarted()) {
      myTableVisibleLife.cycleStart();
      mySumtable.attach(myTableVisibleLife.lifespan());
    }
  }

  private static final MenuBuilder TREE_POPUP;
  static {
    TREE_POPUP = new MenuBuilder()
//      .addDefaultAction(MainMenu.Tools.VIEW_NOTE)
      .addDefaultAction(MainMenu.Search.RUN_QUERY)
      .addAction(MainMenu.Search.RELOAD_QUERY)
//      .addDefaultAction(MainMenu.Search.EXPAND_DISTRIBUTION)
      .addAction(MainMenu.Search.RUN_QUERY_IN_BROWSER)
      .addSeparator()
      .addAction(MainMenu.Edit.NEW_ITEM_HERE)
      .addAction(MainMenu.Search.NEW_QUERY)
      .addAction(MainMenu.Search.NEW_DISTRIBUTION)
      .addAction(MainMenu.Edit.NEW_FOLDER)
      .addAction(new NewTagAction(true))
      .addAction(MainMenu.Search.EXCLUDE_FROM_DISTRIBUTION)
      .createSubMenu("Empty Sub-Queries")
        .addToggleAction(MainMenu.Search.HIDE_EMPTY_QUERIES_OFF)
        .addToggleAction(MainMenu.Search.HIDE_EMPTY_QUERIES_ON)
        .endSubMenu()
      .addSeparator()
      .addAction(MainMenu.Tools.WATCH_IN_IDEA)
      .addSeparator()
      .addAction(MainMenu.Search.EDIT_QUERY)
      .addAction(new EditTagAction())
      .addAction(MainMenu.Search.TOP_DUPLICATE_QUERY)
      .addAction(MainMenu.File.DOWNLOAD_CHANGES_QUICK_POPUP)
      .createSubMenu("Get Changes in Background")
        .addToggleAction(ToggleAutoSyncAction.ON)
        .addToggleAction(ToggleAutoSyncAction.OFF)
        .endSubMenu()
      .addAction(MainMenu.File.RELOAD_CONFIGURATION_POPUP)
      .addSeparator()
      .addAction(RetryInitializationAction.POPUP)
      .addAction(ShowConnectionInfo.POPUP)
      .addAction(EditConnectionAction.POPUP)
      .addAction(MainMenu.File.REMOVE_CONNECTION)
      .addSeparator()
      .addAction(MainMenu.Edit.SORT_NODES)
      .addAction(MainMenu.Edit.RENAME);

    if (!DISABLE_CLIPBOARD_IN_POPUPS) {
      TREE_POPUP.addAction(TransferAction.COPY);
      TREE_POPUP.addAction(TransferAction.PASTE);
    }

    TREE_POPUP.addAction(new AnActionDelegator(TransferAction.REMOVE) {
      public void perform(ActionContext context) throws CantPerformException {
        if (checkConfirmation(null, context.getSourceCollection(GenericNode.NAVIGATION_NODE).toArray()))
          super.perform(context);
      }
    });
  }

  private static class LoadingLabel extends ALabel {
    private Font myBaseFont;
    private final FontRenderContext myFrc = new FontRenderContext(null, true, false);

    public LoadingLabel() {
      super("Loading\u2026");
      setAntialiased(true);
      setForeground(ColorUtil.between(getForeground(), AwtUtil.getPanelBackground(), 0.5F));
      setHorizontalAlignment(SwingUtilities.CENTER);
      myBaseFont = getFont();
    }

    public void reshape(int x, int y, int w, int h) {
      super.reshape(x, y, w, h);
      if (myBaseFont == null) {
        myBaseFont = getFont();
      }
      assert myBaseFont != null;
      Rectangle2D lineMetrics = myBaseFont.getStringBounds(getText(), myFrc);
      double baseWidth = lineMetrics.getWidth();
      if (baseWidth > 10 && w > 100) {
        double ratio = 1.0 * w / baseWidth;
        ratio = Math.min(10.0, Math.max(1.0, ratio));
        int newSize = (int) (ratio * myBaseFont.getSize2D());
        if (newSize != getFont().getSize()) {
          Font f = myBaseFont.deriveFont((float) newSize);
          setFont(f);
        }
      }
    }
  }


  private static class NavigationAreaChanger implements ChangeListener {
    private String currentlyShown;
    private final Engine myEngine;
    private final JPanel myTreeArea;
    private final ExplorerForm myExplorerForm;

    public NavigationAreaChanger(Engine engine, JPanel treeArea, ExplorerForm explorerForm) {
      myEngine = engine;
      myTreeArea = treeArea;
      myExplorerForm = explorerForm;
    }

    @Override
    public void onChange() {
      boolean connections = myEngine.getConnectionManager().getConnections().getCurrentCount() > 0;
      // todo activatable license?
      String card = connections ? TREE_CARD_NAVIGATION : TREE_CARD_NEWCONNECTION;
      if (!card.equals(currentlyShown)) {
        currentlyShown = card;
        ((CardLayout) myTreeArea.getLayout()).show(myTreeArea, card);
      }
      if(!connections && !myExplorerForm.isNavigationAreaShown()) {
        myExplorerForm.showNavigationArea(true);
      }
    }
  }
}