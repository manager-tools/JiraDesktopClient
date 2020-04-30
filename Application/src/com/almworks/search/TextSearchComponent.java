package com.almworks.search;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.SimpleTabKey;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Engine;
import com.almworks.api.explorer.ApplicationToolbar;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.search.TextSearch;
import com.almworks.api.search.TextSearchExecutor;
import com.almworks.api.store.Store;
import com.almworks.api.store.StoreUtils;
import com.almworks.explorer.tree.NavigationTree;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.combobox.MacPrettyComboBox;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.io.persist.PersistableHashMap;
import com.almworks.util.io.persist.PersistableInteger;
import com.almworks.util.io.persist.PersistableString;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.properties.Role;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.DocumentUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;

import static com.almworks.util.ui.SingleChildLayout.CONTAINER;
import static com.almworks.util.ui.SingleChildLayout.PREFERRED;

public class TextSearchComponent implements Startable {
  static final Role<TextSearchComponent> ROLE = Role.role("smartSearcher");

  private static final int HISTORY_LIMIT = 30;
  private static final String ID = "RecentSearchWords";
  private static final String TAG_SEARCH_SCOPE = "searchScope";
  private static final String TAG_SCOPE_NAME = "scopeName";
  private static final String TAG_NODE = "node";

  private final Store myStore;
  private final ActionRegistry myActionRegistry;
  private final TextSearch mySearcher;
  private final ApplicationToolbar myToolbar;
  private final Configuration myConfiguration;
  private final ExplorerComponent myExplorerComponent;
  private final Engine myEngine;

  private final SearchComboBox myCombo = new SearchComboBox();
  private final JPanel myPanel = new JPanel(new BorderLayout(1, 0));
  private final HintWindow myHintWindow = new HintWindow(myCombo);

  private final OrderListModel<String> myHistoryModel = OrderListModel.create();

  private WeakReference<Component> myPreviousFocusOwner = null;

  private final OrderListModel<FixedScope> myUserScopes = OrderListModel.create();
  private final List<SearchScope> mySpecialScopes = Collections15.arrayList();
  private final SegmentedListModel<SearchScope> myAllScopes = SegmentedListModel.create();

  private final BasicScalarModel<SearchScope> mySelectedScope = BasicScalarModel.createWithValue(null, true);
  private final BasicScalarModel<Collection<GenericNode>> myScopeNodes =
    BasicScalarModel.createWithValue(Collections15.<GenericNode>emptyCollection(), true);

  private final BasicScalarModel<ShowScopeHintsMode> myShowScopeHints =
    BasicScalarModel.createWithValue(ShowScopeHintsMode.HIDE, true);

  private final Bottleneck myUpdateScopeHintsBottleneck = new Bottleneck(200, ThreadGate.AWT, new Runnable() {
    public void run() {
      updateScopeHints();
    }
  });
  private final Bottleneck myUpdateScopeNodesModelBottleneck = new Bottleneck(200, ThreadGate.AWT, new Runnable() {
    public void run() {
      updateScopeNodesModel();
    }
  });



  private static final TypedKey SEARCH_NODES = ROLE;

  private Popup mySelectorPopup;
  private JPanel myPopupContents;
  private AList<SearchScope> myPopupScopeList;
  private final Lifecycle mySelectorPopupLife = new Lifecycle(false);
  private boolean mySelectorPopupMouseClicked;
  private boolean mySelectorPopupNewScopeSelected;


  public TextSearchComponent(Store store, ActionRegistry actionRegistry, TextSearch searcher,
    ApplicationToolbar toolbar, Configuration configuration, ExplorerComponent explorerComponent, Engine engine)
  {
    myStore = store;
    myActionRegistry = actionRegistry;
    mySearcher = searcher;
    myToolbar = toolbar;
    myConfiguration = configuration;
    myExplorerComponent = explorerComponent;
    myEngine = engine;

    setupSpecialScopes();
    setupCombo();
    setupButton();
    setupPanel();
    setupScopeListeners();
    setupScopeSelector();

    mySelectedScope.setValue(mySpecialScopes.get(0));
  }

  private void setupScopeSelector() {
    myCombo.getSelectorButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (mySelectorPopup != null) {
          hideSelectorPopup(false, true);
        } else {
          showSelectorPopup();
        }
      }
    });
  }

  private void showSelectorPopup() {
    mySelectorPopupLife.cycleStart();
    JButton button = myCombo.getSelectorButton();
    Point location = button.getLocation();
    location.y += button.getHeight();
    SwingUtilities.convertPointToScreen(location, button);
    mySelectorPopup = UIUtil.getPopup(button, getSelectorPopupContents(), location.x, location.y);
    mySelectorPopup.show();
    UIUtil.requestFocusLater(myPopupScopeList.getSwingComponent());
  }

  private void hideSelectorPopup(boolean apply, boolean grabFocus) {
    if (mySelectorPopup == null)
      return;
    if (apply) {
      SearchScope scope = myPopupScopeList.getSelectionAccessor().getSelection();
      if (scope != null)
        mySelectedScope.setValue(scope);
    }
    mySelectorPopup.hide();
    mySelectorPopupLife.cycleEnd();
    mySelectorPopup = null;
    if (grabFocus) {
      focusText();
    }
  }

  private Component getSelectorPopupContents() {
    if (myPopupContents == null) {
      myAllScopes.addSegment(FixedListModel.create(mySpecialScopes));
      myAllScopes.addSegment(myUserScopes);

      myPopupScopeList = new AList<SearchScope>(myAllScopes);
      myPopupScopeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      myPopupScopeList.setCanvasRenderer(Renderers.defaultCanvasRenderer());

      myPopupContents = new JPanel(new BorderLayout());
      myPopupContents.setOpaque(true);
      LineBorder outer =
        new LineBorder(ColorUtil.between(UIUtil.getEditorForeground(), AwtUtil.getPanelBackground(), 0.8F));
      myPopupContents.setBorder(new CompoundBorder(outer, new EmptyBorder(0, 5, 5, 5)));

      JLabel label = new JLabel();
      NameMnemonic.parseString("Use search &scope:").setToLabel(label);
      label.setLabelFor(myPopupScopeList.getScrollable());

      myPopupContents.add(label, BorderLayout.NORTH);
      AScrollPane scrollPane = new AScrollPane(myPopupScopeList);
      scrollPane.setSizeDelegate(SizeConstraints.boundedPreferredSize(new Dimension(300, 400)));
      myPopupContents.add(scrollPane, BorderLayout.CENTER);
      JPanel buttons = new JPanel(new FormLayout("center:d:g(1), center:d:g(1)", "d"));
      buttons.add(new AActionButton(new AddScopeAction()), new CellConstraints(1, 1));
      buttons.add(new AActionButton(new RemoveScopeAction()), new CellConstraints(2, 1));
      myPopupContents.add(buttons, BorderLayout.SOUTH);
    }

    final SelectionAccessor<SearchScope> accessor = myPopupScopeList.getSelectionAccessor();
    final SearchScope initial = mySelectedScope.getValue();
    accessor.setSelected(initial);

    mySelectorPopupNewScopeSelected = false;
    Lifespan lifespan = mySelectorPopupLife.lifespan();
    accessor.addChangeListener(lifespan, new com.almworks.util.collections.ChangeListener() {
      public void onChange() {
        SearchScope newSelection = accessor.getSelection();
        if (newSelection != initial) {
          mySelectorPopupNewScopeSelected = true;
          checkHidePopupOnMouse();
        }
      }
    });

    mySelectorPopupMouseClicked = false;
    UIUtil.addMouseListener(lifespan, myPopupScopeList.getScrollable(), new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        mySelectorPopupMouseClicked = true;
        checkHidePopupOnMouse();
      }
    });

    UIUtil.addKeyListener(lifespan, myPopupScopeList.getScrollable(), new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_ENTER) {
          hideSelectorPopup(true, true);
          e.consume();
        } else if (code == KeyEvent.VK_ESCAPE) {
          hideSelectorPopup(false, true);
          e.consume();
        }
        mySelectorPopupMouseClicked = false;
      }
    });

    UIUtil.addToolkitListener(lifespan, AWTEvent.FOCUS_EVENT_MASK, new AWTEventListener() {
      public void eventDispatched(AWTEvent event) {
        int id = event.getID();
        if (id == FocusEvent.FOCUS_LOST || id == FocusEvent.FOCUS_GAINED) {
          FocusEvent fe = ((FocusEvent) event);
          Component owner = id == FocusEvent.FOCUS_GAINED ? fe.getComponent() : fe.getOppositeComponent();
          while (owner != null && owner != myPopupContents && !(owner instanceof Window)) {
            owner = owner.getParent();
          }
          if (owner != myPopupContents) {
            hideSelectorPopup(false, false);
          }
        }
      }
    });

    return myPopupContents;
  }

  private void checkHidePopupOnMouse() {
    if (mySelectorPopup != null) {
      if (mySelectorPopupNewScopeSelected && mySelectorPopupMouseClicked) {
        hideSelectorPopup(true, true);
      }
    }
  }

  private void setupSpecialScopes() {
    mySpecialScopes.add(new SelectionScope());
    mySpecialScopes.add(new GlobalScope());
  }

  private void setupCombo() {
    myCombo.setModel(SelectionInListModel.createForever(myHistoryModel, null));
//    LineBorder line = new LineBorder(UIUtil.getPanelBackground().darker(), 1);
//    myCombo.setBorder(new CompoundBorder(line, new IconBorder(Icons.LOOKING_GLASS, 3, 1)) {
//      public boolean isBorderOpaque() {
//        return true;
//      }
//    });
    myCombo.initialize();
    setupEnterKeyRunsSearch();
    setupComboFocus();
    setupEscapeRemovesFocus();
    setupCtrlDown();
    setupListenTyping();
  }

  private void setupListenTyping() {
    Component component = myCombo.getEditor().getEditorComponent();
    if (component instanceof JTextComponent) {
      DocumentUtil.addListener(Lifespan.FOREVER, ((JTextComponent) component).getDocument(), new DocumentAdapter() {
        protected void documentChanged(DocumentEvent e) {
          myUpdateScopeNodesModelBottleneck.request();
        }
      });
    }
  }

  private void setupCtrlDown() {
    myCombo.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN &&
          ((e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK)) != 0))
        {
          myCombo.getSelectorButton().doClick();
        }
      }
    });
  }

  private void setupButton() {
//    mySearchButton.setFocusReceiver(myCombo.getEditor().getEditorComponent());
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        readScopesFromConfiguration();
      }
    });
  }

  @ThreadAWT
  private void readScopesFromConfiguration() {
    myUserScopes.clear();
    List<Configuration> subsets = myConfiguration.getAllSubsets(TAG_SEARCH_SCOPE);
    for (Configuration subset : subsets) {
      String name = subset.getSetting(TAG_SCOPE_NAME, null);
      List<String> nodes = subset.getAllSettings(TAG_NODE);
      int nodesCount = nodes.size();
      if (name == null || nodesCount == 0) {
        Log.warn("bad scope (" + name + ", " + nodesCount + "), removing");
        subset.removeMe();
        continue;
      }
      myUserScopes.addElement(new FixedScope(name, nodes));
    }
  }

  private void writeScopesToConfiguration() {
    myConfiguration.clear();
    for (SearchScope scope : myUserScopes.toList()) {
      String name = scope.getName();
      List<String> nodeIds = ((FixedScope) scope).getNodeIds();
      Configuration subset = myConfiguration.createSubset(TAG_SEARCH_SCOPE);
      subset.setSetting(TAG_SCOPE_NAME, name);
      subset.setSettings(TAG_NODE, nodeIds);
    }
  }

  private void setupPanel() {
    myPanel.add(SingleChildLayout.envelop(myCombo, CONTAINER, PREFERRED, CONTAINER, CONTAINER, 0F, 0.5F),
      BorderLayout.CENTER);

    JButton run = new AToolbarButton(new EnabledAction("", Icons.ACTION_RUN_QUERY) {
      {
        setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Run text search");
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        search(false);
      }
    });

    myPanel.add(SingleChildLayout.envelopCenter(run), BorderLayout.EAST);
  }

  private void setupScopeListeners() {
    myShowScopeHints.getEventSource()
      .addAWTListener(Lifespan.FOREVER, (ScalarModel.Adapter) new ScalarModel.Adapter<Object>() {
        public void onScalarChanged(ScalarModelEvent event) {
          myUpdateScopeNodesModelBottleneck.request();
          myUpdateScopeHintsBottleneck.requestDelayed();
        }
      });
    myScopeNodes.getEventSource()
      .addAWTListener(Lifespan.FOREVER, (ScalarModel.Adapter) new ScalarModel.Adapter<Object>() {
        public void onScalarChanged(ScalarModelEvent event) {
          myUpdateScopeHintsBottleneck.requestDelayed();
        }
      });

    KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    focusManager.addPropertyChangeListener("focusOwner", new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        checkFocus();
      }
    });
    mySelectedScope.getEventSource().addListener(ThreadGate.STRAIGHT, new ScalarModel.Adapter<SearchScope>() {
      public void onScalarChanged(ScalarModelEvent<SearchScope> event) {
        myUpdateScopeNodesModelBottleneck.request();
        myUpdateScopeHintsBottleneck.requestDelayed();
      }
    });
  }

  private void setupEscapeRemovesFocus() {
    final Component editorComponent = myCombo.getEditor().getEditorComponent();
    editorComponent.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getModifiersEx() == 0) {
          if (myPreviousFocusOwner != null) {
            Component previous = myPreviousFocusOwner.get();
            myPreviousFocusOwner.clear();
            myPreviousFocusOwner = null;
            if (previous != null && previous.isDisplayable() && previous.isVisible()) {
              Component editorComponent = myCombo.getEditor().getEditorComponent();
              if (myCombo.hasFocus() || editorComponent.hasFocus() || myCombo.getSelectorButton().hasFocus()) {
                previous.requestFocus();
                e.consume();
                return;
              }
            }
          }
          KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(myPanel);
        }
      }
    });
  }

  private void setupComboFocus() {
    final Component editorComponent = myCombo.getEditor().getEditorComponent();
    editorComponent.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        Component opposite = e.getOppositeComponent();
        myPreviousFocusOwner = opposite == null ? null : new WeakReference<Component>(opposite);
        if (editorComponent instanceof JTextComponent)
          ((JTextComponent) editorComponent).selectAll();
      }

      public void focusLost(FocusEvent e) {
      }
    });
  }                                                  

  private void setupEnterKeyRunsSearch() {
    myCombo.getEditor().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          String searchString = getSearchString();
          updateModel(searchString);
          search(false);
        } catch (CantPerformExceptionSilently ee) {
          // ignore
        }
      }
    });
  }

  private void updateScopeHints() {
    boolean visible = myShowScopeHints.getValue() != ShowScopeHintsMode.HIDE;
    Collection<GenericNode> nodes = visible ? myScopeNodes.getValue() : null;
    myHintWindow.update(visible, nodes);
    // todo refactor here - move up from myHintWindow.update checking for same nodes
    if (nodes != null)
      nodes = TreeUtil.excludeDescendants(nodes, GenericNode.GET_PARENT_NODE);
    myExplorerComponent.setHighlightedNodes(SEARCH_NODES, nodes, UIUtil.getEditorForeground(), Icons.LOOKING_GLASS,
      "search here");
  }

  private void checkFocus() {
    ShowScopeHintsMode currentMode = myShowScopeHints.getValue();
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    boolean inFocus = isMyComponent(focusOwner);
    if (inFocus) {
      if (currentMode != ShowScopeHintsMode.SHOW_ON_FOCUS) {
        myShowScopeHints.setValue(ShowScopeHintsMode.SHOW_ON_FOCUS);
      }
    } else {
      if (currentMode == ShowScopeHintsMode.SHOW_ON_FOCUS) {
        myShowScopeHints.setValue(ShowScopeHintsMode.HIDE);
      }
    }
  }
         
  private boolean isMyComponent(Component component) {
    if (component == null)
      return false;
    for (Component c = component; c != null; c = c.getParent()) {
      if (c == myPanel)
        return true;
    }
    JRootPane focusRootPane = UIUtil.getRootPaneFixed(component);
    Component hintPanel = myHintWindow.getMainPanel();
    if (hintPanel != null && focusRootPane == UIUtil.getRootPaneFixed(hintPanel)) {
      return true;
    }
    JComponent dd = myPopupContents;
    if (dd != null && focusRootPane == UIUtil.getRootPaneFixed(dd)) {
      return true;
    }
    return false;
  }

  private String getSearchString() throws CantPerformExceptionSilently {
    String search = (String) myCombo.getEditor().getItem();
    if (search == null)
      throw new CantPerformExceptionSilently("ignore");
    search = search.trim();
    if (search.length() == 0)
      throw new CantPerformExceptionSilently("ignore");
    return search;
  }

  private void updateModel(String newText) {
    if (newText == null)
      return;
    boolean hasChanges = false;
    final int index = myHistoryModel.indexOf(newText);
    if (index < 0) {
      myHistoryModel.insert(0, newText);
      hasChanges = true;
    } else if (index > 0) {
      myHistoryModel.remove(newText);
      myHistoryModel.insert(0, newText);
      hasChanges = true;
    }
    while (true) {
      final int size = myHistoryModel.getSize();
      if (size <= HISTORY_LIMIT)
        break;
      myHistoryModel.removeAt(size - 1);
      hasChanges = true;
    }
    myCombo.getModel().setSelectedItem(newText);
    if (hasChanges)
      storeHistory();
  }

  private void storeHistory() {
    final PersistableHashMap<Integer, String> myRecentMap =
      new PersistableHashMap<Integer, String>(new PersistableInteger(), new PersistableString());
    final Map<Integer, String> map = Collections15.hashMap();
    for (int i = 0; i < myHistoryModel.getSize(); i++)
      map.put(i, myHistoryModel.getAt(i));
    myRecentMap.set(map);
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        StoreUtils.storePersistable(myStore, ID, myRecentMap);
      }
    });
  }

  public void start() {
    restoreHistory();
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        myToolbar.setSectionComponent(ApplicationToolbar.Section.SEARCH, myPanel);
        registerAction(myActionRegistry);
        myEngine.registerGlobalDescriptor(new FullTextSearchDescriptor(myEngine));
      }
    });
  }

  public void stop() {
  }

  private void updateScopeNodesModel() {
    Collection<GenericNode> nodes = getScopeNodes();
    String type = null;
    if (nodes != null) {
      try {
        String string = getSearchString();
        TextSearchExecutor executor = mySearcher.getSearchExecutor(string);
        if (executor != null) {
          type = executor.getType().getDisplayableShortName();
          nodes = executor.getRealScope(nodes);
        }
      } catch (CantPerformExceptionSilently e) {
        // ignore
      }
    }
    myScopeNodes.setValue(nodes == null ? Collections15.<GenericNode>emptyCollection() : nodes);
    myHintWindow.setSearchType(type);
  }

  private Collection<GenericNode> getScopeNodes() {
    SearchScope scope = mySelectedScope.getValue();
    if (scope != null) {
      try {
        return scope.getCurrentScope(new DefaultUpdateContext(myCombo, Updatable.NEVER));
      } catch (CantPerformException e) {
        // ignore
      }
    }
    return null;
  }



  private void registerAction(ActionRegistry actionRegistry) {
    actionRegistry.registerAction(MainMenu.Search.QUICK_SEARCH, new SimpleAction(L.actionName("Q&uick Search"), null) {
      {
        setDefaultText(PresentationKey.SHORT_DESCRIPTION,
          L.tooltip("Find " + Terms.ref_artifacts + " by ID or search words"));
        setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
        
        // This is only for kicking the action occasionally. Without it,
        // the first customUpdate() calls (those for the main window and
        // the Mac OS X hidden frame) will erroneously disable their actions,
        // because ownWindow would be null (go see). The issue is observed
        // on both Windows and Mac, but it won't reproduce in debugger with
        // a breakpoint set in customUpdate(). Seems like customUpdate()
        // somehow gets called before the UI is fully constructed.
        // The NavigationTree here happens to be provided by the main window,
        // so it's kinda good thing to watch for.
        watchRole(NavigationTree.NAVIGATION_JTREE);
      }

      protected void doPerform(ActionContext context) {       
        focusText();
      }

      protected void customUpdate(UpdateContext context) throws CantPerformException {
        final Component component = context.getComponent();
        final Window contextWindow = SwingTreeUtil.getOwningWindow(component);
        final Window ownWindow = SwingTreeUtil.getOwningWindow(myCombo);
        context.setEnabled(contextWindow == ownWindow ? EnableState.ENABLED : EnableState.INVISIBLE);
      }
    });
  }

  private void focusText() {
    myCombo.getEditor().getEditorComponent().requestFocus();
  }

  private void restoreHistory() {
    final PersistableHashMap<Integer, String> recentMap =
      new PersistableHashMap<Integer, String>(new PersistableInteger(), new PersistableString());
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        if (StoreUtils.restorePersistable(myStore, ID, recentMap)) {
          final List<Integer> keys = Collections15.arrayList();
          final Map<Integer, String> map = recentMap.access();
          Iterator<Integer> iterator = map.keySet().iterator();
          while (iterator.hasNext())
            keys.add(iterator.next());
          Collections.sort(keys);
          ThreadGate.AWT.execute(new Runnable() {
            public void run() {
              myHistoryModel.clear();
              for (int i = 0; i < keys.size(); i++)
                myHistoryModel.addElement(map.get(keys.get(i)));
            }
          });
        }
      }
    });
  }

  private void search(boolean newTab) throws CantPerformExceptionSilently {

    String search = getSearchString();
    Collection<GenericNode> nodes = myScopeNodes.getValue();
    if (nodes == null || nodes.size() == 0)
      throw new CantPerformExceptionSilently("ignored");
    updateModel(search);
    SimpleTabKey tab = newTab ? new SimpleTabKey() : null;
    mySearcher.search(search, nodes, tab);
  }


  private void requestFocusForSearch() {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        focusText();
      }
    });
  }


  private abstract static class ModifyScopeAction extends SimpleAction {
    protected ModifyScopeAction(@Nullable String name) {
      super(name);
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
    }
  }


  private class RemoveScopeAction extends ModifyScopeAction {
    public RemoveScopeAction() {
      super("Remove scopes\u2026");
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.updateOnChange(myUserScopes);
      context.setEnabled(myUserScopes.getSize() > 0);
    }


    protected void doPerform(ActionContext context) throws CantPerformException {
      RemoveScopesDialog dialog = new RemoveScopesDialog(context);
      dialog.show(myUserScopes.toList());
      Collection<FixedScope> scopes = dialog.getSelectedScopes();
      if (scopes != null) {
        myUserScopes.removeAll(scopes);
        if (scopes.contains(mySelectedScope.getValue())) {
          mySelectedScope.setValue(mySpecialScopes.get(0));
        }
        writeScopesToConfiguration();
      }
      requestFocusForSearch();
    }
  }


  private class AddScopeAction extends ModifyScopeAction {
    public AddScopeAction() {
      super("Add scope\u2026");
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      final AddScopeDialog dialog = new AddScopeDialog(context, myCombo);
      dialog.show(new Runnable() {
        public void run() {
          FixedScope scope = dialog.getNewScope();
          if (scope != null) {
            myUserScopes.addElement(scope);
            mySelectedScope.setValue(scope);
            writeScopesToConfiguration();
          }
          requestFocusForSearch();
        }
      });
    }
  }


  private static enum ShowScopeHintsMode {
    HIDE,
    SHOW_ON_HOVER,
    SHOW_ON_FOCUS
  }


  private class SearchComboBox extends AComboBox<String> {
    public SearchComboBox() {
      super(new SearchComboBoxHost(), 0);
    }

    public void initialize() {
      SearchComboBoxHost host = (SearchComboBoxHost) getCombobox();
      setEditable(true);
      FilteringFocusTraversalPolicy.NO_FOCUS.putClientValue(this, true);
      FilteringFocusTraversalPolicy.NO_FOCUS.putClientValue(host, true);
      setToolTipText(L.tooltip(Local.parse("Enter search words, IDs or URL")));

      ComboBoxEditor editor = myCombo.getEditor();
      if (editor == null) {
        assert false : this;
        return;
      }

      Component editorComponent = editor.getEditorComponent();
      if (!(editorComponent instanceof JTextComponent)) {
        assert false : editorComponent;
        return;
      }

      JTextComponent textComponent = (JTextComponent) editorComponent;
      FilteringFocusTraversalPolicy.NO_FOCUS.putClientValue(textComponent, true);
      Color bg = textComponent.getBackground();
      if (bg != null) {
        setBackground(bg);
        setOpaque(true);
        host.setOpaque(true);
      }
      host.initialize();
    }

    public Dimension getPreferredSize() {
      Dimension result = super.getPreferredSize();
      Insets insets = getInsets(PaintUtil.insets);
      int min = ((SearchComboBoxHost) getCombobox()).mySearchIcon.getIconHeight() + insets.top + insets.bottom;
      if (result.height < min)
        result.height = min;
      return result;
    }

    public JButton getSelectorButton() {
      return ((SearchComboBoxHost) getCombobox()).mySelectorButton;
    }
  }

  private class SearchComboBoxHost extends MacPrettyComboBox {
    @NotNull
    private Icon mySearchIcon = Icons.LOOKING_GLASS_WITH_ARROW;
    private int myIconSpace;
    private JButton mySelectorButton;

    public SearchComboBoxHost() {
      mySelectorButton = new MyJButton();
      mySelectorButton.setOpaque(false);
      mySelectorButton.setBorderPainted(false);
      mySelectorButton.setBorder(AwtUtil.EMPTY_BORDER);
      mySelectorButton.setMargin(AwtUtil.EMPTY_INSETS);
      mySelectorButton.setFocusable(false);
      add(mySelectorButton);
    }

    protected void paintComponent(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      super.paintComponent(g);
      if (myIconSpace > 0) {
        paintSelectorIcon(g);
      }
    }

    private void paintSelectorIcon(Graphics g) {
      Rectangle bounds = getBounds();
      Insets insets = getInsets(PaintUtil.insets);
      // fill rectangle taken by empty icon space
      int x = insets.left - myIconSpace;
      if (isOpaque()) {
        g.setColor(getBackground());
        UIUtil.safeFillRect(g, x, insets.top, myIconSpace, bounds.height - insets.top - insets.bottom);
      }
    }

    public void initialize() {
      assert myIconSpace == 0; // can't set it twice by now
      myIconSpace = mySearchIcon.getIconWidth() + 6;
      setBorder(UIUtil.getCompoundBorder(getBorder(), new EmptyBorder(0, myIconSpace, 0, 0)));
    }

    public void layout() {
      super.layout();
      Rectangle bounds = getBounds();
      Insets insets = getInsets(PaintUtil.insets);
      mySelectorButton.setBounds(0, 0, insets.left, bounds.height);
    }

    @Override
    protected Insets getFocusRingInsets() {
      return new Insets(0, myIconSpace, 0, 0);
    }

    private class MyJButton extends JButton {
      public MyJButton() {
        super(SearchComboBoxHost.this.mySearchIcon);
      }

      protected void paintBorder(Graphics g) {
      }

      protected void paintComponent(Graphics g) {
        AwtUtil.applyRenderingHints(g);
        int height = mySearchIcon.getIconHeight();
        int width = mySearchIcon.getIconWidth();
        mySearchIcon.paintIcon(this, g, (getWidth() - width) >> 1, (getHeight() - height) >> 1);
      }
    }
  }
}