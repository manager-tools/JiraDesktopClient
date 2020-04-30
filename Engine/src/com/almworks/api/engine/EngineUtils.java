package com.almworks.api.engine;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.English;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Collections15;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedBoolean;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import java.util.Iterator;
import java.util.List;

public class EngineUtils {
  private static final String[] SECOND_ORDER_WORDS = {"BUG", "ISSUE", "OPEN", "JIRA", "TRACK"};

  public static Detach runWhenConnectionIsReady(Connection connection, final ThreadGate gate, final Runnable runnable) {
    final DetachComposite detach = new DetachComposite(true);
    detach.add(connection.getState().getEventSource().addListener(gate, new ScalarModel.Adapter<ConnectionState>() {
      public void onScalarChanged(ScalarModelEvent<ConnectionState> event) {
        ConnectionState state = event.getNewValue();
        if (state != null && state.isReady()) {
          detach.detach();
          gate.execute(runnable);
        }
      }
    }));
    return detach;
  }

  public static Engine getEngine(DBReader reader) {
    return reader.getDatabaseUserData().getUserData(Engine.ROLE);
  }

  public static void runWhenConnectionsAreStable(Lifespan life, final ThreadGate gate, final Runnable runnable) {
    if (life.isEnded())
      return;
    final DetachComposite detach = new DetachComposite(true);
    final SynchronizedBoolean lock = new SynchronizedBoolean(false);
    life.add(detach);
    final ConnectionManager connectionManager = Context.require(Engine.class).getConnectionManager();
    connectionManager.whenConnectionsLoaded(detach, ThreadGate.STRAIGHT, new Runnable() {
      public void run() {
        if (detach.isEnded())
          return;
        final List<Connection> connections = Collections15.arrayList(connectionManager.getConnections().copyCurrent());
        checkRemainingConnections(connections, gate, runnable, detach, lock);
        if (!detach.isEnded()) {
          ScalarModel.Adapter<ConnectionState> listener = new ScalarModel.Adapter<ConnectionState>() {
            public void onScalarChanged(ScalarModelEvent<ConnectionState> event) {
              checkRemainingConnections(connections, gate, runnable, detach, lock);
            }
          };
          Connection[] pending;
          synchronized (lock.getLock()) {
            pending = connections.toArray(new Connection[connections.size()]);
          }
          for (Connection connection : pending) {
            connection.getState().getEventSource().addListener(detach, ThreadGate.STRAIGHT, listener);
          }
        }
      }
    });
  }

  private static void checkRemainingConnections(List<Connection> connections, ThreadGate gate, Runnable runnable,
    DetachComposite detach, SynchronizedBoolean lock)
  {
    if (detach.isEnded())
      return;
    boolean run = false;
    synchronized (lock.getLock()) {
      if (lock.get())
        return;
      for (Iterator<Connection> ii = connections.iterator(); ii.hasNext();) {
        Connection connection = ii.next();
        ConnectionState state = connection.getState().getValue();
        if (state != null && state.isStable()) {
          ii.remove();
        }
      }
      if (connections.size() == 0) {
        lock.set(true);
        run = true;
      }
    }
    if (run) {
      gate.execute(runnable);
      detach.detach();
    }
  }

  @Nullable
  public static String suggestConnectionNameByUrl(String url) {
    url = Util.NN(url).trim();
    if (url.length() == 0)
      return null;
    if ("https://".contains(url) || "http://".contains(url)) {
      // too short url
      return null;
    }
    // remove protocol
    int k = url.indexOf("://");
    if (k >= 0)
      url = url.substring(k + 3);
    if (url.length() == 0)
      return null;

    List<String> words = getWords(url);
    if (words.size() == 0)
      return null;

    String guess = null;
    // 1st pass
    for (String name : words) {
      if (name.length() <= 3)
        continue;
      int j = 0;
      int len = SECOND_ORDER_WORDS.length;
      for (; j < len; j++)
        if (name.indexOf(SECOND_ORDER_WORDS[j]) >= 0)
          break;
      if (j != len)
        continue;
      guess = name;
      break;
    }
    // 2nd pass
    return guess != null ? guess : words.get(0);
  }

  public static ScalarModel<String> createNameSuggestionByUrl(final Document urlDocument) {
    final BasicScalarModel<String> result = BasicScalarModel.create(true, false);
    DocumentUtil.addListener(Lifespan.FOREVER, urlDocument, new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        String guess = suggestConnectionNameByUrl(DocumentUtil.getDocumentText(urlDocument));
        if (guess != null)
          result.setValue(English.capitalize(guess));
      }
    });
    return result;
  }

  private static List<String> getWords(String url) {
    List<String> result = Collections15.arrayList();
    String[] strings = url.split("[^\\w\\d_\\-]");
    for (int i = 0; i < strings.length; i++) {
      String string = strings[i];
      if (string.length() > 0)
        result.add(Util.upper(string));
    }
    return result;
  }

  @ThreadAWT
  public static void runWhenConnectionDescriptorsAreReady(final Connection connection, final ThreadGate gate,
    final Runnable runnable)
  {
    final List<ConstraintDescriptor> descriptors = Collections15.arrayList();
    AListModel<? extends ConstraintDescriptor> descriptorsModel = connection.getDescriptors();
    if (descriptorsModel != null) {
      descriptors.addAll(descriptorsModel.toList());
    }
    AListModel<ConstraintDescriptor> globalModel = Context.require(Engine.class).getGlobalDescriptors();
    if (globalModel != null) {
      descriptors.addAll(globalModel.toList());
    }
    if (descriptors.size() != 0) {
      ThreadGate.NEW_THREAD.execute(new Runnable() {
        public void run() {
          try {
            for (ConstraintDescriptor descriptor : descriptors) {
              if (connection.getState().getValue() != ConnectionState.READY)
                return;
              descriptor.waitForInitialization();
            }
            gate.execute(runnable);
          } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
          }
        }
      });
    } else {
      gate.execute(runnable);
    }
  }

  @Nullable
  public static String getConnectionName(Connection connection) {
    if (connection == null) return null;
    Engine engine = connection.getConnectionContainer().getActor(Engine.ROLE);
    if (engine == null)
      return null;
    return engine.getConnectionManager().getConnectionName(connection.getConnectionID());
  }

  public static Long getMe(ItemVersion item) {
    Long connection = item.getValue(SyncAttributes.CONNECTION);
    if (connection == null) {
      LogHelper.error("No connection for " + item);
      return null;
    }
    return item.getReader().getValue(connection, Connection.USER);
  }

  private static class Kludge {
    private final Configuration kludge;

    Kludge(Configuration configuration) {
      kludge = configuration;
    }
  }
}
