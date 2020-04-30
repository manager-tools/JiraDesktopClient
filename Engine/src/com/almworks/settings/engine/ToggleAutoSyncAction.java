package com.almworks.settings.engine;

import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.spi.provider.AbstractConnection;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.ui.actions.*;

import java.util.List;

import static com.almworks.api.engine.Connection.AutoSyncMode;
import static com.almworks.api.engine.Connection.AutoSyncMode.AUTOMATIC;
import static com.almworks.api.engine.Connection.AutoSyncMode.MANUAL;

public abstract class ToggleAutoSyncAction extends SimpleAction {
  public static final AnAction ON = new ToggleAutoSyncAction("On") {
    @Override
    protected boolean isMyMode(AutoSyncMode mode) {
      return mode == AUTOMATIC;
    }
    @Override
    protected AutoSyncMode getNewMode(AutoSyncMode oldMode) {
      return AUTOMATIC;
    }
  };

  public static final AnAction OFF = new ToggleAutoSyncAction("Off") {
    @Override
    protected boolean isMyMode(AutoSyncMode mode) {
      return mode != AUTOMATIC;
    }
    @Override
    protected AutoSyncMode getNewMode(AutoSyncMode oldMode) {
      return MANUAL;
    }
  };

  public static final AnAction TOGGLE = new ToggleAutoSyncAction("Get Changes in Background") {
    { watchRole(GenericNode.NAVIGATION_NODE); }
    @Override
    protected boolean isMyMode(AutoSyncMode mode) {
      return mode == AUTOMATIC;
    }
    @Override
    protected AutoSyncMode getNewMode(AutoSyncMode oldMode) {
      return oldMode == AUTOMATIC ? MANUAL : AUTOMATIC;
    }
  };

  protected ToggleAutoSyncAction(String name) {
    super(name);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    final AbstractConnection conn = getConnection(context);
    if(conn != null) {
      context.putPresentationProperty(PresentationKey.TOGGLED_ON, isMyMode(getAutoSyncMode(conn)));
    } else {
      context.setEnabled(EnableState.INVISIBLE);
    }
  }

  private AbstractConnection getConnection(ActionContext context) throws CantPerformException {
    final List<GenericNode> nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
    if(nodes.size() != 1) {
      return null;
    }

    final GenericNode node = nodes.get(0);
    if(!(node instanceof ConnectionNode)) {
      return null;
    }

    final Connection conn = ((ConnectionNode) node).getConnection();
    if(!(conn instanceof AbstractConnection)) {
      return null;
    }

    return (AbstractConnection) conn;
  }

  protected abstract boolean isMyMode(AutoSyncMode mode);

  private AutoSyncMode getAutoSyncMode(AbstractConnection conn) {
    final BasicScalarModel<AutoSyncMode> model = conn.getAutoSyncMode();
    return model.getValue();
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final AbstractConnection conn = getConnection(context);
    if(conn != null) {
      final AutoSyncMode newMode = getNewMode(getAutoSyncMode(conn));
      conn.getAutoSyncMode().setValue(newMode);
    }
  }

  protected abstract AutoSyncMode getNewMode(AutoSyncMode oldMode);
}
