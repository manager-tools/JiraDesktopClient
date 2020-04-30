package com.almworks.settings.engine;

import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.EngineListener;
import com.almworks.api.engine.SyncParameters;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

public abstract class DoSynchronizeAction extends AnAbstractAction {
  private final EngineListener myEventSink;

  protected DoSynchronizeAction(String text, Icon icon, String tooltip, EngineListener eventSink) {
    super(text, icon);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, tooltip);
    myEventSink = eventSink;
  }

  @Override
  public void update(UpdateContext context) throws CantPerformException {
    super.update(context);
    final Engine engine = context.getSourceObject(Engine.ROLE);
    final CollectionModel<Connection> conns = engine.getConnectionManager().getConnections();
    context.updateOnChange(conns.getCountModel());
    context.setEnabled(conns.getCurrentCount() > 0 ? EnableState.ENABLED : EnableState.DISABLED);
  }

  @Override
  public void perform(ActionContext context) throws CantPerformException {
    myEventSink.onSynchronizationRequested(false);
    final Engine engine = context.getSourceObject(Engine.ROLE);
    engine.getSynchronizer().synchronize(getSyncParameters(context));
  }

  protected abstract SyncParameters getSyncParameters(ActionContext context) throws CantPerformException;

  private static void checkForSelectedConnections(UpdateContext context) throws CantPerformException {
    if(context.isDisabled() || getSelectedConnections(context) == null) {
      context.setEnabled(EnableState.INVISIBLE);
    }
  }

  private static Collection<Connection> getAutoSyncedConnections(ActionContext context) throws CantPerformException {
    final Engine engine = context.getSourceObject(Engine.ROLE);
    final List<Connection> manuals = Collections15.arrayList();
    final List<Connection> autos = Collections15.arrayList();
    final List<Connection> conns = engine.getConnectionManager().getConnections().copyCurrent();
    for(final Connection conn : conns) {
      if(conn.getAutoSyncMode().getValue() == Connection.AutoSyncMode.MANUAL) {
        manuals.add(conn);
      } else {
        autos.add(conn);
      }
    }
    return autos.isEmpty() ? manuals : autos;
  }

  private static Collection<Connection> getSelectedConnections(ActionContext context) throws CantPerformException {
    final List<GenericNode> nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
    if(nodes.isEmpty()) {
      return null;
    }

    List<Connection> conns = null;
    for(final GenericNode node : nodes) {
      if(node instanceof ConnectionNode) {
        final Connection conn = ((ConnectionNode) node).getConnection();
        if(conns == null) {
          conns = Collections15.arrayList();
        }
        conns.add(conn);
      } else {
        return null;
      }
    }

    return conns;
  }

  private static Collection<Connection> requireSelectedConnections(ActionContext context) throws CantPerformException {
    final Collection<Connection> conns = getSelectedConnections(context);
    if(conns == null) {
      throw new CantPerformException();
    }
    return conns;
  }

  public static class ReloadConfig extends DoSynchronizeAction {
    public ReloadConfig(EngineListener eventSink) {
      super(
        L.actionName("Reload " + Terms.ref_ConnectionType + " &Configuration"),
        Icons.ACTION_SYNCHRONIZE_DOWNLOAD_ONLY,
        Local.parse("Download server configuration and metadata from " + Terms.ref_ConnectionType),
        eventSink);
    }

    @Override
    protected SyncParameters getSyncParameters(ActionContext context) throws CantPerformException {
      return SyncParameters.downloadChangesAndMeta(getAutoSyncedConnections(context));
    }
  }

  public static class ReloadConfigPopup extends ReloadConfig {
    public ReloadConfigPopup(EngineListener eventSink) {
      super(eventSink);
      setDefaultPresentation(PresentationKey.SMALL_ICON, null);
    }

    @Override
    public void update(UpdateContext context) throws CantPerformException {
      context.watchRole(GenericNode.NAVIGATION_NODE);
      super.update(context);
      checkForSelectedConnections(context);
    }

    @Override
    protected SyncParameters getSyncParameters(ActionContext context) throws CantPerformException {
      return SyncParameters.downloadChangesAndMeta(requireSelectedConnections(context));
    }
  }

  public static class GetChangesNow extends DoSynchronizeAction {
    public GetChangesNow(EngineListener eventSink) {
      super(L.actionName("&Get Changes Now"), null,
        Local.parse("Check for changes on server and download changed $(app.term.artifacts) now"),
        eventSink);
    }

    @Override
    protected SyncParameters getSyncParameters(ActionContext context) throws CantPerformException {
      return SyncParameters.downloadChanges(getAutoSyncedConnections(context));
    }
  }

  public static class GetChangesNowPopup extends GetChangesNow {
    public GetChangesNowPopup(EngineListener eventSink) {
      super(eventSink);
    }

    @Override
    public void update(UpdateContext context) throws CantPerformException {
      context.watchRole(GenericNode.NAVIGATION_NODE);
      super.update(context);
      checkForSelectedConnections(context);
    }

    @Override
    protected SyncParameters getSyncParameters(ActionContext context) throws CantPerformException {
      return SyncParameters.downloadChanges(requireSelectedConnections(context));
    }
  }
}
