package com.almworks.explorer;

import com.almworks.api.application.*;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.application.util.sources.CompositeItemSource;
import com.almworks.api.application.util.sources.ItemViewAdapter;
import com.almworks.api.application.util.sources.LoadingUserQueryItemSource;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.engine.*;
import com.almworks.api.explorer.DelayedDelegatingItemSource;
import com.almworks.api.explorer.ItemUrlService;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.search.TextSearch;
import com.almworks.api.search.TextSearchExecutor;
import com.almworks.api.search.TextSearchType;
import com.almworks.gui.CommonMessages;
import com.almworks.items.api.DBFilter;
import com.almworks.util.*;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.ExternalBrowser;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.text.TextUtil;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.FilteringFocusTraversalPolicy;
import com.almworks.util.ui.MacIntegration;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.almworks.util.collections.Functional.*;
import static com.almworks.util.commons.Condition.EMPTY_STRING;
import static com.almworks.util.commons.Condition.not;
import static java.util.Arrays.asList;
import static org.almworks.util.Collections15.emptyMap;
import static org.almworks.util.Collections15.hashMap;

public class ItemUrlServiceImpl implements ItemUrlService, Startable {
  private static final Map<String, String> SCHEME_REWRITES;
  static {
    final Map<String, String> m = Collections15.hashMap();
    m.put("almworks-dz-http", "http");
    m.put("almworks-dz-https", "https");
    m.put("almworks-jc-http", "http");
    m.put("almworks-jc-https", "https");
    SCHEME_REWRITES = Collections.unmodifiableMap(m);
  }

  private final Engine myEngine;
  private final ExplorerComponent myExplorerComponent;
  private final MainWindowManager myWindowManager;

  private final TextSearch myTextSearch;
  private final TextSearchType mySearchType = new MyTextSearchType();

  private final AnAction myOpenAction = new OpenInBrowserAction();
  private final JTextField myUrlField = new JTextField() {
    @Override
    public Dimension getPreferredSize() {
      // make it snap to different sizes - will keep it stable on similar urls
      Dimension r = super.getPreferredSize();
      r.width = (r.width / 32 + 1) * 32;
      return r;
    }
  };
  private final ConfirmationHandler myDefaultConfirmationHandler = new ConfirmationHandler() {
    @Override
    public void confirmCreateConnection(ItemProvider provider, Configuration configuration, Procedure<Boolean> onConfirm) {
      onConfirm.invoke(confirmConnectionCreation(1, true));
    }
  };

  public ItemUrlServiceImpl(Engine engine, ExplorerComponent explorerComponent, TextSearch textSearch, MainWindowManager windowManager) {
    myEngine = engine;
    myExplorerComponent = explorerComponent;
    myTextSearch = textSearch;
    myWindowManager = windowManager;
    setupField();
  }

  // todo separate showItem and toolbar
  @ThreadAWT
  @Override
  public void showItem(@NotNull final String url0, ConfirmationHandler confirmationHandler)
    throws CantPerformExceptionExplained
  {
    final String url = rewriteScheme(url0);

    final Connection connection = getConnectionForUrl(url);
    if (connection != null) {
      openUrlInConnection(connection, url);
    } else {
      final ItemProvider provider = getProviderForUrl(url);
      final Configuration configuration;
      try {
        configuration = getDefaultConnectionConfiguration(provider, url);
      } catch (ProviderDisabledException e) {
        Log.warn(e);
        return;
      }
      final boolean[] createConnectionCancelled = {false};
      Util.NN(confirmationHandler, myDefaultConfirmationHandler).confirmCreateConnection(provider, configuration, new Procedure<Boolean>() {
        public void invoke(Boolean result) {
          if (!result) {
            createConnectionCancelled[0] = true;
            return;
          }
          createConnection(provider, configuration, new Procedure<Connection>() {
            public void invoke(Connection connection) {
              if (connection != null) {
                try {
                  openUrlInConnection(connection, url);
                } catch (CantPerformException e) {
                  Log.debug(e);
                }
              }
            }
          });
        }
      });
      if (createConnectionCancelled[0]) throw new CantPerformExceptionExplained("Connection creation was cancelled");
    }
  }

  private String rewriteScheme(String url0) {
    URI uri;
    try {
      uri = new URI(url0);
      uri = new URI(rewriteScheme(uri), uri.getSchemeSpecificPart(), uri.getFragment());
      return uri.toString();
    } catch (URISyntaxException e) {
      Log.warn(e);
    }
    return url0;
  }

  private String rewriteScheme(URI uri) {
    final String scheme = uri.getScheme();
    if(SCHEME_REWRITES.containsKey(scheme)) {
      return SCHEME_REWRITES.get(scheme);
    }
    return scheme;
  }

  public Pair<JComponent, AnAction> createUrlFieldAndAction() {
    ItemUrlServiceImpl impl = new ItemUrlServiceImpl(myEngine, null, null, null);
    // do not start this!
    return Pair.create((JComponent)impl.myUrlField, impl.myOpenAction);
  }

  public void start() {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        myTextSearch.addTextSearchType(Lifespan.FOREVER, mySearchType);
      }
    });
  }

  public void stop() {
  }

  private void createConnection(ItemProvider provider, Configuration configuration,
    final Procedure<Connection> procedure)
  {
    try {
      final Connection newConnection = myEngine.getConnectionManager().createConnection(provider, configuration);
      if (newConnection == null) {
        // no access, maybe
        procedure.invoke(null);
        return;
      }
      EngineUtils.runWhenConnectionIsReady(newConnection, ThreadGate.AWT, new Runnable() {
        public void run() {
          procedure.invoke(newConnection);
        }
      });
    } catch (ConfigurationException e) {
      Log.debug(new CantPerformExceptionExplained("cannot create connection with default settings"));
    } catch (ProviderDisabledException e) {
      Log.debug(new CantPerformExceptionExplained("provider is disabled (wrd)"));
    }
  }

  @NotNull
  public static ItemSource getItemSource(@NotNull final Connection connection, @NotNull final String url)
    throws CantPerformExceptionExplained
  {
    ItemSource source = connection.getItemSourceForUrls(Collections.singleton(url));
    if (source == null) {
      source = getDefaultItemSource(connection, url);
    }
    return source;
  }

  private static ItemSource getAggregateItemSource(MultiMap<Connection, String> urlMap) throws CantPerformExceptionExplained {
    return getAggregateItemSource(urlMap, "Multiple URLs", "Loading");
  }

  public static ItemSource getAggregateItemSource(final MultiMap<Connection, String> urlMap, String sourceName, String activityName)
    throws CantPerformExceptionExplained
  {
    if (urlMap.isEmpty())
      return ItemSource.EMPTY;
    if (urlMap.size() == 1) {
      Connection c = first(urlMap.keySet());
      if (c == null) return ItemSource.EMPTY;
      return getItemSource(c, urlMap.getSingle(c));
    }
    return new CompositeItemSource(sourceName, activityName) {
      @Override
      protected void reloadingPrepare(ItemsCollector collector) {
        super.reloadingPrepare(collector);
        for (Connection c : urlMap.keySet()) {
          try {
            List<String> urls = urlMap.getAll(c);
            if (c == null || urls == null) continue;
            ItemSource source = c.getItemSourceForUrls(urls);
            if (source != null) {
              add(collector, source, urls.size());
            } else {
              // the tough way: many little sources
              for (String url : urls) add(collector, getItemSource(c, url), 1);
            }
          } catch (CantPerformExceptionExplained ee) {
            Log.warn(ee);
          }
        }
      }
    };
  }

  @NotNull
  public static ItemSource getDefaultItemSource(@NotNull final Connection connection, @NotNull final String url)
    throws CantPerformExceptionExplained
  {
    final Pair<DBFilter, Constraint> pair = connection.getViewAndConstraintForUrl(url);
    return new CompositeItemSource("URL", "Loading\u2026") {
      protected void reloadingPrepare(ItemsCollector collector) {
        clear(collector);
        ItemViewAdapter viewAdapter = ItemViewAdapter.create(pair.getFirst());
        add(collector, viewAdapter, 1000);
        final BasicScalarModel<Long> finalCN = BasicScalarModel.createWithValue(null, true);
        viewAdapter.setRequiredCNModel(collector, finalCN);
        ItemSource source = new LoadingUserQueryItemSource(connection, url, pair.getSecond(), pair.getFirst(), null, new Procedure<SyncTask>() {
          @Override
          public void invoke(SyncTask arg) {
            finalCN.setValue(arg.getLastCommittedCN());
          }
        });
        add(collector, source, 2000);
      }
    };
  }

  private Connection getConnectionForUrl(String url) {
    return myEngine.getConnectionManager().getConnectionForUrl(url);
  }

  public static Configuration getDefaultConnectionConfiguration(ItemProvider provider, String url)
    throws CantPerformExceptionExplained, ProviderDisabledException
  {
    if (provider == null)
      throw new CantPerformExceptionExplained("unrecognized url '" + url + '\'');
    final Configuration configuration = provider.createDefaultConfiguration(url);
    if (configuration == null)
      throw new CantPerformExceptionExplained("cannot create connection with default settings");
    return configuration;
  }

  private ItemProvider getProviderForUrl(String url) {
    return myEngine.getConnectionManager().getProviderForUrl(url);
  }

  private void openUrlInConnection(@NotNull Connection connection, @NotNull String url)
    throws CantPerformExceptionExplained
  {
    ItemSource source = getItemSource(connection, url);
    String title = Util.NN(connection.getItemIdForUrl(url), "URL Lookup");
    if (source != null) {
      SimpleTabKey key = new SimpleTabKey();
      myExplorerComponent.showItemsInTab(source, ItemCollectionContext.createNoNode(title, url, key), true);
      MacIntegration.sendActivate();
      myWindowManager.bringToFront();
    }
  }

  private void setupField() {
    final String toolTipText =
      String.format("%s URL, hit %s to select, or %s to copy to clipboard.", Terms.ref_Artifact, Shortcuts.alt("U"), Shortcuts.menu("C"));
    myUrlField.setToolTipText(Local.parse(toolTipText));

    myUrlField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.ALT_DOWN_MASK), "focus");
    myUrlField.getActionMap().put("focus", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        myUrlField.requestFocus();
      }
    });

    myUrlField.setEditable(false);
    myUrlField.setBorder(UIUtil.getToolbarEditorBorder());
    myUrlField.setOpaque(false);
    FilteringFocusTraversalPolicy.NO_FOCUS.putClientValue(myUrlField, true);

    myUrlField.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent event) {
        int length = myUrlField.getDocument().getLength();
        if (length > 0) {
          myUrlField.selectAll();
          try {
            Rectangle r = myUrlField.modelToView(length);
            if (r != null)
              myUrlField.scrollRectToVisible(r);
          } catch (BadLocationException e) {
            // ignore
          }
        }
      }
    });
  }

  /**
   * @param confirmConnectionsCreation takes number of connections to be created. If returns true, they are created and item source is returned; otherwise, no item source is created.
   * @param stopOnFirstFailedConnection if true, no items source is returned in case some of the required connections could not be created
   * @param onUrlsCannotBeLoaded accepts URLs of items that are understood to be not loaded
   * */
  @NotNull
  public static ItemSource getItemSourceForUrls(final Iterable<String> urls, final ConnectionManager connMan,
    @NotNull Function<Integer, Boolean> confirmConnectionsCreation, boolean stopOnFirstFailedConnection, final Procedure<List<String>> onUrlsCannotBeLoaded
  ) throws CantPerformExceptionExplained, ProviderDisabledException, ConfigurationException
  {
    MultiMap<Connection, String> urlMap = mapUrlsToExistingConnections(urls, connMan);
    if (!urlMap.keySet().contains(null)) {
      return getAggregateItemSource(urlMap);
    }
    Map<Configuration, ItemProvider> connectionsToCreate = getConfigsForConnectionsToCreate(urlMap, connMan, stopOnFirstFailedConnection);
    int count = connectionsToCreate.size();
    if (count == 0 && stopOnFirstFailedConnection) {
      // in case when stop on first failed connection is not needed, this map can be legally empty; otherwise, weird
      assert false : count + " " + urlMap;
      return ItemSource.EMPTY;
    } else {
      if (!confirmConnectionsCreation.invoke(count)) {
        throw new CantPerformExceptionSilently("aborted");
      }
    }

    for (Map.Entry<Configuration, ItemProvider> e : connectionsToCreate.entrySet()) {
      Connection c = connMan.createConnection(e.getValue(), e.getKey());
      if (c == null) {
        // possibly, license does not permit creation of this connection (e.g. site license)
        String msg = "cannot automatically create connection " + e.getKey();
        Log.warn(msg);
        if (stopOnFirstFailedConnection)
          throw new CantPerformExceptionSilently(msg);
      }
    }
    final DelayedDelegatingItemSource source = new DelayedDelegatingItemSource("W", "Waiting for connection set-up");
    EngineUtils.runWhenConnectionsAreStable(Lifespan.FOREVER, ThreadGate.AWT, new Runnable() {
      public void run() {
        MultiMap<Connection, String> urlMap = mapUrlsToExistingConnections(urls, connMan);
        List<String> notLoadedUrls = urlMap.removeAll(null);
        if (notLoadedUrls != null) {
          onUrlsCannotBeLoaded.invoke(notLoadedUrls);
        }
        try {
          source.setDelegate(getAggregateItemSource(urlMap));
        } catch (CantPerformExceptionExplained e) {
          Log.debug(e);
        }
      }
    });
    return source;
  }

  /** @return multimap: null key corresponds to connectionless items */
  private static MultiMap<Connection, String> mapUrlsToExistingConnections(Iterable<String> urls, ConnectionManager connMan) {
    MultiMap<Connection, String> map = MultiMap.create();
    for (String url : filter(urls, not(EMPTY_STRING))) {
      map.add(connMan.getConnectionForUrl(url), url);
    }
    return map;
  }

  /**
   * @param urlConnections result of {@link #mapUrlsToExistingConnections}
   * @param stopOnFirstFailedConnection
   */
  private static Map<Configuration, ItemProvider> getConfigsForConnectionsToCreate(MultiMap<Connection, String> urlConnections, ConnectionManager connMan, boolean stopOnFirstFailedConnection)
    throws CantPerformExceptionExplained, ProviderDisabledException
  {
    List<String> connectionlessUrls = urlConnections.getAll(null);
    if (connectionlessUrls == null) return emptyMap();
    Map<Configuration, ItemProvider> additionalConfigurations = hashMap();
    for (String url : connectionlessUrls) {
      ItemProvider provider = connMan.getProviderForUrl(url);
      if (provider == null && !stopOnFirstFailedConnection) continue;
      final Configuration configuration = getDefaultConnectionConfiguration(provider, url);
      // check if configuration already created
      boolean alreadyThere = !isEmpty(filter(additionalConfigurations.keySet(), new Condition<Configuration>() {
        @Override
        public boolean isAccepted(Configuration value) {
          return ConfigurationUtil.haveSameSettings(value, configuration);
        }}));
      if (!alreadyThere) {
        additionalConfigurations.put(configuration, provider);
      }
    }
    return additionalConfigurations;
  }

  private Boolean confirmConnectionCreation(Integer nConnectionsToCreate, boolean oneUrl) {
    JRootPane parent = SwingUtilities.getRootPane(myUrlField);
    String msg = oneUrl ? Local.parse(
      "<html><body>To load and display this " + Terms.ref_artifact + " " + Terms.ref_Deskzilla +
        " needs to configure a connection.<br>" + "Would you like to let " + Terms.ref_Deskzilla +
        " automatically create a connection with default settings now?")
      : Local.parse(
      "<html><body>To load and display these " + Terms.ref_artifacts + " " + Terms.ref_Deskzilla +
        " needs to configure " + English.numberOf(nConnectionsToCreate, "connection") + ".<br>" + "Would you like to let " +
        Terms.ref_Deskzilla + " automatically create connections with default settings now?");
    int r = JOptionPane.showConfirmDialog(parent, msg, "Confirm Connection Creation", JOptionPane.YES_NO_OPTION,
      JOptionPane.QUESTION_MESSAGE);
    return r == JOptionPane.YES_OPTION;
  }

  @Override
  public SearchBuilder createSearchBuilder() {
    return new SearchBuilderImpl(myEngine, mySearchType, myUrlField);
  }


  private class MyTextSearchType implements TextSearchType {
    @Nullable
    public TextSearchExecutor parse(String searchString) {
      String[] urlArray = searchString.trim().split("[\\s,]+");
      SearchBuilder builder = createSearchBuilder();
      List<String> list = filterToList(asList(urlArray), not(EMPTY_STRING));
      for (String url : list) {
        if (!builder.addUrl(url)) return null;
      }
      return builder.createExecutor();
    }

    public String getDisplayableShortName() {
      return "url";
    }

    public int getWeight() {
      return TextSearchType.Weight.ID_SEARCH - 100;
    }
  }


  public class SearchBuilderImpl implements SearchBuilder {
    private final Engine myEngine;
    private final TextSearchType mySearchType;
    private final JTextField myUrlField;
    private final List<String> myUrlList = Collections15.arrayList();
    private final List<String> mySearchDescription = Collections15.arrayList();
    public SearchBuilderImpl(Engine engine, TextSearchType searchType, JTextField urlField) {
      myEngine = engine;
      mySearchType = searchType;
      myUrlField = urlField;
    }

    @Override
    public boolean addUrl(String url) {
      ItemProvider provider = getProviderForUrl(url);
      if (provider == null) return false;
      String shortId = provider.getDisplayableItemIdFromUrl(url);
      mySearchDescription.add(shortId != null ? shortId : url);
      myUrlList.add(url);
      return true;
    }

    private ItemProvider getProviderForUrl(String url) {
      return myEngine.getConnectionManager().getProviderForUrl(url);
    }

    @Override
    @Nullable
    public TextSearchExecutor createExecutor() {
      if (myUrlList.isEmpty()) return null;
      String description = TextUtil.separate(mySearchDescription, ", ");
      return new MyExecutor(myEngine, mySearchType, myUrlField, myUrlList, description);
    }
  }


  private class MyExecutor implements TextSearchExecutor {
    private final Engine myEngine;
    private final TextSearchType mySearchType;
    private final JTextField myUrlField;
    private final List<String> myUrls;
    private final String mySearchDescription;

    public MyExecutor(Engine engine, TextSearchType searchType, JTextField urlField, @NotNull List<String> urls,
      String searchDescription) {
      myEngine = engine;
      mySearchType = searchType;
      myUrlField = urlField;
      myUrls = urls;
      mySearchDescription = searchDescription;
    }

    @NotNull
    @ThreadAWT
    public ItemSource executeSearch(Collection<? extends GenericNode> scope) throws CantPerformExceptionSilently {
      try {
        ConnectionManager connMan = myEngine.getConnectionManager();
        return getItemSourceForUrls(myUrls, connMan, new Function<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer nConnectionsToCreate) {
              return confirmConnectionCreation(nConnectionsToCreate, myUrls.size() == 1);
            }
          },
          true,
          new Procedure<List<String>>() {
            @Override
            public void invoke(List<String> urls) {
              Log.warn("cannot load urls: " + urls);
            }
          });
      } catch (ProviderDisabledException e) {
        Log.warn("cannot show " + myUrls, e);
        throw new CantPerformExceptionSilently(e.getMessage());
      } catch (CantPerformExceptionExplained e) {
        if (e instanceof CantPerformExceptionSilently)
          throw ((CantPerformExceptionSilently) e);
        assert false : e;
        Log.warn("cannot show " + myUrls);
        throw new CantPerformExceptionSilently(e.getMessage());
      } catch (ConfigurationException e) {
        Log.warn("cannot show " + myUrls, e);
        throw new CantPerformExceptionSilently(e.getMessage());
      }
    }

    @NotNull
    public TextSearchType getType() {
      return mySearchType;
    }

    @NotNull
    public String getSearchDescription() {
      return mySearchDescription;
    }

    @NotNull
    public Collection<GenericNode> getRealScope(@NotNull Collection<GenericNode> nodes) {
      if (nodes == null || nodes.size() == 0)
        return nodes;
      GenericNode first = nodes.iterator().next();
      RootNode root = first.getRoot();
      if (root != null)
        return Collections.<GenericNode>singleton(root);
      else
        return nodes;
    }
  }


  private static final String WSFX = Env.isMac() ? "" : " (F9)";
  private class OpenInBrowserAction extends SimpleAction {
    public OpenInBrowserAction() {
      // kludge: add F9 manually
      super("Open selected " + Terms.ref_artifact + " in browser" + WSFX, Icons.ACTION_OPEN_IN_BROWSER);
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
        "Open selected " + Terms.ref_artifact + " in browser" + WSFX);
      watchRole(ItemWrapper.ITEM_WRAPPER);
      watchRole(LoadedItem.LOADED_ITEM);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      String text = "";
      try {
        ItemWrapper wrapper = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
        boolean deleted = wrapper.services().isRemoteDeleted();
        if (deleted) {
          text = "deleted " + Local.text(Terms.key_artifact);
        } else {
          Connection connection = wrapper.getConnection();
          if (connection == null || connection.getState().getValue().isDegrading()) {
            text = "connection is not ready";
          } else {
            String itemUrl = wrapper.getItemUrl();
            if (itemUrl == null || itemUrl.length() == 0) {
              text = "no url";
            } else {
              text = itemUrl;
              if(connection.isItemUrl(itemUrl)) {
                context.setEnabled(true);
              }
            }
          }
        }
      } finally {
        myUrlField.setText(text);
        Dimension psz = myUrlField.getPreferredSize();
        Dimension sz = myUrlField.getSize();
        if (!psz.equals(sz)) {
          myUrlField.invalidate();
        }
        int length = text.length();
        if (length > 0) {
          try {
            Rectangle r = myUrlField.modelToView(length);
            if (r != null)
              myUrlField.scrollRectToVisible(r);
          } catch (BadLocationException e) {
            // ignore
          }
        }
      }
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      String url = myUrlField.getText();
      ExternalBrowser browser = new ExternalBrowser();
      browser.setUrl(url + "#", false);
      browser.setDialogHandler(CommonMessages.OPEN_IN_BROWSER.create(),
        L.content("Failed to open " + url + " in default browser."));
      browser.openBrowser();
    }
  }
}
