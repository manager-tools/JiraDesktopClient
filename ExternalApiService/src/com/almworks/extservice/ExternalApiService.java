package com.almworks.extservice;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.TagNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.explorer.ApplicationToolbar;
import com.almworks.api.explorer.ItemUrlService;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.install.TrackerProperties;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.platform.ProductInformation;
import com.almworks.dup.util.ApiLog;
import com.almworks.explorer.tree.FavoritesNode;
import com.almworks.items.api.*;
import com.almworks.tracker.alpha.AlphaProtocol;
import com.almworks.tracker.eapi.alpha.ArtifactLoadOption;
import com.almworks.util.Env;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.OrderListModelGate;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.AToolbar;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.*;
import com.almworks.util.xmlrpc.*;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.Startable;
import util.concurrent.SynchronizedBoolean;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import static com.almworks.tracker.alpha.AlphaProtocol.Messages.ToClient;
import static com.almworks.tracker.alpha.AlphaProtocol.Messages.ToClient.ADD_WATCHED_COLLECTION;
import static com.almworks.tracker.alpha.AlphaProtocol.Messages.ToTracker;

public class ExternalApiService implements Startable {
  public static final Role<ExternalApiService> ROLE = Role.role(ExternalApiService.class);
  private static final String PROTOCOL = AlphaProtocol.Protocols.ALPHA;

  private final MessageInbox myInbox = new MessageInbox();

  private final ItemUrlService myItemUrlService;
  private final ProductInformation myProductInformation;
  private final MainWindowManager myWindowManager;
  private final Engine myEngine;
  private final WorkArea myWorkArea;
  private final ApplicationToolbar myApplicationToolbar;
  private final ActionRegistry myActionRegistry;
  private final DialogManager myDialogManager;
  private final ExplorerComponent myExplorerComponent;
  private final Database myDatabase;

  /**
   * Maps client port to a connection.
   */
  private final Map<Integer, EapiClient> myClients = Collections15.hashMap();

  private final OrderListModel<EapiClient> myClientsModel = OrderListModel.create();

  /**
   * Temporarily stores generated settings for auto-generated connections while they are being created.
   * Used to avoid multiple creation of a single connection.
   */
  private final Map<Configuration, Connection> myCreatingConnections =
    Collections.synchronizedMap(Collections15.<Configuration, Connection>hashMap());

  private final Bottleneck myUpdater = new Bottleneck(2000, ThreadGate.AWT, new Runnable() {
    public void run() {
      updateAll();
    }
  });

  private final SynchronizedBoolean myToolbarSetUp = new SynchronizedBoolean(false);

  private final javax.swing.Timer myPongTimer;

  private AddCollectionDialog myAddCollectionDialog;
  private AToolbar myToolbar;
  private final ApplicationLoadStatus myApplicationLoadStatus;

  public ExternalApiService(ItemUrlService itemUrlService, ProductInformation productInformation,
    MainWindowManager windowManager, Engine engine, WorkArea workArea, ApplicationToolbar toolbar,
    ActionRegistry actionRegistry, DialogManager dialogManager, ExplorerComponent explorerComponent,
    ApplicationLoadStatus applicationLoadStatus, Database database)
  {
    if (Env.getBoolean(TrackerProperties.DEBUG)) {
      ApiLog.configureLogging(workArea.getLogDir());
    }
    myApplicationLoadStatus = applicationLoadStatus;
    myExplorerComponent = explorerComponent;
    myDialogManager = dialogManager;
    myItemUrlService = itemUrlService;
    myProductInformation = productInformation;
    myWindowManager = windowManager;
    myEngine = engine;
    myWorkArea = workArea;
    myApplicationToolbar = toolbar;
    myActionRegistry = actionRegistry;
    myDatabase = database;
    myPongTimer = new javax.swing.Timer(5000, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        massPong();
      }
    });
  }

  public void start() {
    ScalarModel<Boolean> model = myApplicationLoadStatus.getApplicationLoadedModel();
    final DetachComposite life = new DetachComposite();
    model.getEventSource().addListener(life, ThreadGate.LONG(this), new ScalarModel.Adapter<Boolean>() {
      public void onScalarChanged(ScalarModelEvent<Boolean> event) {
        if (life.isEnded())
          return;
        Boolean v = event.getNewValue();
        if (v != null && v) {
          life.detach();
          doStart();
        }
      }
    });
  }

  private void doStart() {
    try {
      boolean debug = ProductInformation.DEBUG.equalsIgnoreCase(myProductInformation.getVersionType());
      TrackerStartCommand.store(debug);
    } catch (CannotStoreTrackerStarterException e) {
      Log.debug(e);
    }

    myInbox.addFactory(new IMPing());
    myInbox.addFactory(new IMGetSupportedProtocols());
    myInbox.addFactory(new IMOpenArtifacts());
    myInbox.addFactory(new IMSubscribe());
    myInbox.addFactory(new IMUnsubscribe());
    myInbox.addFactory(new IMToFront());
    myInbox.addFactory(new IMRequestAddCollectionAction());
    myInbox.addFactory(new IMWatchCollection());
    myInbox.addFactory(new IMUnwatchCollection());
    myInbox.start();

    myDatabase.addListener(Lifespan.FOREVER, new DBListener() {
      @Override
      public void onDatabaseChanged(DBEvent event, DBReader reader) {
        myUpdater.request();
      }
    });

    int activePort = myInbox.getActivePort();
    assert activePort > 0 : activePort;
    myWorkArea.announceApiPort(activePort);

    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        registerActions();
        myPongTimer.start();
      }
    });
  }

  @ThreadAWT
  private void registerActions() {
    myActionRegistry.registerAction(MainMenu.Tools.EXTERNAL_SEARCH, new FindExternallyAction(myClientsModel));
    myActionRegistry.registerAction(MainMenu.Tools.WATCH_IN_IDEA, new MonitorExternallyAction());
  }

  @ThreadAWT
  private void updateToolbar() {
    boolean toolbarNeeded = myClientsModel.getSize() > 0;
    if (!myToolbarSetUp.commit(!toolbarNeeded, toolbarNeeded))
      return;
    if (toolbarNeeded && myToolbar == null) {
      ToolbarBuilder builder = new ToolbarBuilder();
      builder.setCommonPresentation(PresentationMapping.NONAME);
      builder.addAction(MainMenu.Tools.EXTERNAL_SEARCH);
      builder.addAction(MainMenu.Tools.WATCH_IN_IDEA);
      myToolbar = builder.createHorizontalToolbar();
    }
    myApplicationToolbar.setSectionComponent(ApplicationToolbar.Section.EXTERNAL_ARTIFACT_TOOLS,
      toolbarNeeded ? myToolbar : null);
  }

  public void stop() {
    ExtServiceUtils.gate().execute(new Runnable() {
      public void run() {
        myInbox.shutdown();
        EapiClient[] clients = getClients();
        for (EapiClient client : clients) {
          client.dispose();
        }
        synchronized (myClients) {
          myClients.clear();
        }
        ThreadGate.AWT.execute(new Runnable() {
          public void run() {
            myClientsModel.clear();
            myPongTimer.stop();
            updateToolbar();
          }
        });
      }
    });
  }

  private EapiClient[] getClients() {
    EapiClient[] clients;
    synchronized (myClients) {
      clients = myClients.values().toArray(new EapiClient[myClients.size()]);
    }
    return clients;
  }

  @ThreadAWT
  private void updateAll() {
    for (EapiClient client : getClients()) {
      client.updateAll();
    }
  }

  @ThreadAWT
  private void showAddCollectionDialog(@NotNull EapiClient client) {
    if (myAddCollectionDialog == null)
      myAddCollectionDialog = new AddCollectionDialog(myDialogManager, myExplorerComponent);
    myAddCollectionDialog.show(client);
  }

  private EapiClient getOrCreateClient(int clientPort) {
    boolean created = false;
    EapiClient client;
    synchronized (myClients) {
      client = myClients.get(clientPort);
      if (client == null) {
        client = new EapiClient(clientPort, myDatabase, myCreatingConnections, myEngine, myExplorerComponent);
        myClients.put(clientPort, client);
        final EapiClient finalClient = client;
        ThreadGate.AWT.execute(new Runnable() {
          public void run() {
            myClientsModel.addElement(finalClient);
            updateToolbar();
          }
        });
        created = true;
      }
    }
    if (created) {
      client.start();
    }
    return client;
  }

  private void stopClient(int clientPort) {
    final EapiClient client;
    synchronized (myClients) {
      client = myClients.remove(clientPort);
    }
    if (client != null) {
      client.dispose();
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          myClientsModel.remove(client);
          updateToolbar();
        }
      });
    }
  }

  private void stopClient(final EapiClient client) {
    boolean removed;
    synchronized (myClients) {
      removed = myClients.values().remove(client);
    }
    if (client != null) {
      client.dispose();
      if (removed) {
        ThreadGate.AWT.execute(new Runnable() {
          public void run() {
            myClientsModel.remove(client);
            updateToolbar();
          }
        });
      }
    }
  }

  private void clientUpdated(final EapiClient client) {
    OrderListModelGate.create(myClientsModel, ThreadGate.AWT).updateElement(client);
  }


  private OutgoingMessage createPong(final EapiClient forClient) {
    Hashtable hashtable = new Hashtable();
    hashtable.put(ToClient.Ping.TRACKER_NAME, myProductInformation.getName());
    hashtable.put(ToClient.Ping.TRACKER_VERSION,
      myProductInformation.getVersion() + " build " + myProductInformation.getBuildNumber());
    hashtable.put(ToClient.Ping.TRACKER_WORKSPACE, myWorkArea.getRootDir().getAbsolutePath());
    return new SimpleOutgoingMessage(ToClient.PING, hashtable, forClient.getConnectionId()) {
      protected void requestFailed(Exception problem) {
        ApiLog.debug("client " + forClient + " disconnected", problem);
        stopClient(forClient);
      }
    };
  }

  private void pong(EapiClient client) {
    if (client.confirmPong()) {
      client.send(createPong(client));
    }
  }

  private void massPong() {
    EapiClient[] clients = getClients();
    for (EapiClient client : clients) {
      pong(client);
    }
  }


  /**
   * Carries client port as a first parameter
   */
  private abstract class IMClientIdentifiedMessage extends CloningIncomingMessage {
    private EapiClient myClient;
    private int myPort = 0;

    protected IMClientIdentifiedMessage(String methodName) {
      super(methodName);
    }

    protected void reset(Vector parameters) {
      myPort = (Integer) parameters.remove(0);
      if (myPort <= 0 || myPort > 65535)
        throw new IllegalArgumentException(String.valueOf(myPort));
      myClient = getOrCreateClient(myPort);
      super.reset(parameters);
    }

    public EapiClient getClient() {
      return myClient;
    }

    public int getPort() {
      return myPort;
    }
  }


  private abstract class IMAwtUrls extends IMClientIdentifiedMessage implements Runnable {
    protected Set<String> myUrls;

    protected IMAwtUrls(String methodName) {
      super(methodName);
    }

    protected void reset(Vector parameters) {
      super.reset(parameters);
      myUrls = Collections15.hashSet();
      for (Object parameter : parameters) {
        if (parameter instanceof String)
          myUrls.add((String) parameter);
        else
          Log.warn("invalid parameter " + parameter);
      }
    }

    protected void process() throws MessageProcessingException {
      ThreadGate.AWT.execute(this);
    }
  }


  private class IMPing extends IMClientIdentifiedMessage {
    public IMPing() {
      super(ToTracker.PING);
    }

    protected final void process() throws MessageProcessingException {
      EapiClient client = getClient();
      Vector parameters = getParameters();
      int connectionId = (Integer) parameters.get(0);
      if (connectionId > 0 && client.getConnectionId() != connectionId) {
        int port = getPort();
        stopClient(port);
        client = getOrCreateClient(port);
      }
      String name = (String) parameters.get(1);
      String shortName = (String) parameters.get(2);
      boolean changed = client.updateNames(name, shortName);
      pong(client);
      if (changed)
        clientUpdated(client);
    }
  }


  private class IMGetSupportedProtocols extends IMClientIdentifiedMessage {
    public IMGetSupportedProtocols() {
      super(ToTracker.GET_SUPPORTED_PROTOCOLS);
    }

    protected void process() throws MessageProcessingException {
      getClient().send(new SimpleOutgoingMessage(ToClient.SUPPORTED_PROTOCOLS, PROTOCOL));
    }
  }


  private class IMToFront extends CloningIncomingMessage {
    public IMToFront() {
      super(ToTracker.TO_FRONT);
    }

    protected void process() throws MessageProcessingException {
      ThreadGate.AWT.execute(new Runnable() {
        @Override
        public void run() {
          myWindowManager.bringToFront();
        }
      });
    }
  }


  private class IMOpenArtifacts extends IMAwtUrls {
    public IMOpenArtifacts() {
      super(ToTracker.OPEN_ARTIFACTS);
    }

    public void run() {
      for (String url : myUrls) {
        try {
          myItemUrlService.showItem(url, ItemUrlService.ConfirmationHandler.ALWAYS_AGREE);
        } catch (CantPerformExceptionExplained e) {
          Log.debug("cannot show " + url, e);
        }
      }
      myWindowManager.bringToFront();
    }
  }


  private class IMSubscribe extends IMClientIdentifiedMessage implements Runnable {
    private Map<String, Set<ArtifactLoadOption>> myUrls;

    public IMSubscribe() {
      super(ToTracker.SUBSCRIBE);
    }

    protected void process() throws MessageProcessingException {
      ThreadGate.AWT.execute(this);
    }

    protected void reset(Vector parameters) {
      super.reset(parameters);
      myUrls = Collections15.hashMap();
      Hashtable<Object, Object> table = (Hashtable) parameters.get(0);
      for (Map.Entry<Object, Object> entry : table.entrySet()) {
        String url = (String) entry.getKey();
        Vector options = (Vector) entry.getValue();
        Set<ArtifactLoadOption> set = Collections15.hashSet();
        if (options != null) {
          for (Object option : options) {
            ArtifactLoadOption loadOption = ArtifactLoadOption.forExternalName((String) option);
            if (loadOption != null)
              set.add(loadOption);
          }
        }
        myUrls.put(url, set);
      }
    }

    public void run() {
      EapiClient client = getClient();
      for (Map.Entry<String, Set<ArtifactLoadOption>> entry : myUrls.entrySet()) {
        client.subscribe(entry.getKey(), entry.getValue());
      }
    }
  }


  private class IMUnsubscribe extends IMAwtUrls {
    public IMUnsubscribe() {
      super(ToTracker.UNSUBSCRIBE);
    }

    public void run() {
      EapiClient client = getClient();
      for (String url : myUrls) {
        client.unsubscribe(url);
      }
    }
  }


  private class IMRequestAddCollectionAction extends IMClientIdentifiedMessage {
    public IMRequestAddCollectionAction() {
      super(ToTracker.REQUEST_ADD_COLLECTION_ACTION);
    }

    protected void process() throws MessageProcessingException {
      Vector parameters = getParameters();
      Boolean b = parameters.size() > 0 ? (Boolean) parameters.get(0) : null;
      final boolean sendDefault = b != null && b;
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          if (!sendDefault) {
            myWindowManager.bringToFront();
            showAddCollectionDialog(getClient());
          } else {
            Map<DBIdentifiedObject, TagNode> tags = myExplorerComponent.getTags();
            FavoritesNode node = Condition.isInstance(FavoritesNode.class).detectUntyped(tags.values());
            if (node != null) {
              Hashtable props = new Hashtable();
              props.put(AlphaProtocol.Messages.ToClient.CollectionProps.NAME, node.getName());
              getClient().send(new SimpleOutgoingMessage(ADD_WATCHED_COLLECTION, node.getNodeId(), Boolean.TRUE, props));
            }
          }
        }
      });
    }
  }


  private class IMWatchCollection extends IMClientIdentifiedMessage {
    public IMWatchCollection() {
      super(ToTracker.WATCH_COLLECTION);
    }

    protected void process() throws MessageProcessingException {
      Vector parameters = getParameters();
      final String nodeId = (String) parameters.get(0);
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          getClient().watchNode(nodeId);
        }
      });
    }
  }


  private class IMUnwatchCollection extends IMClientIdentifiedMessage {
    public IMUnwatchCollection() {
      super(ToTracker.UNWATCH_COLLECTION);
    }

    protected void process() throws MessageProcessingException {
      Vector parameters = getParameters();
      final String nodeId = (String) parameters.get(0);
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          getClient().unwatchNode(nodeId);
        }
      });
    }
  }


  private class MonitorExternallyAction extends EapiClientAction {
    public MonitorExternallyAction() {
      super("Watch in external tools", Icons.WATCH_IN_IDE, ExternalApiService.this.myClientsModel);
      watchRole(GenericNode.NAVIGATION_NODE);
    }

    protected void updateUI(UpdateContext context) {
      context.putPresentationProperty(PresentationKey.NAME, adjusted("Watch in $", "External &Monitoring"));
      context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, adjusted("Monitor query in $", "Monitor query in external tools"));
    }

    protected void customUpdate2(UpdateContext context) throws CantPerformException {
      GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
      context.setEnabled(node.isNarrowing());
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      EapiClient client = selectClient(context);
      if (client != null)
        showAddCollectionDialog(client);
    }
  }
}
