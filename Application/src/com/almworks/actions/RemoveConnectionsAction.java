package com.almworks.actions;

import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionViews;
import com.almworks.api.engine.Engine;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.util.AppBook;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.LText;
import com.almworks.util.i18n.LText1;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Vasya
 */
class RemoveConnectionsAction extends SimpleAction {
  private static final String PREFIX = "Application.RemoveConnectionsAction.";
  private static final Integer ONE = 1;
  private static final LText1<Integer> TITLE = AppBook.text(PREFIX + "TITLE",
    "Remove {0,choice,1#Connection|1<Connections}", ONE);
  private static final LText1<Integer> CONFIRMATION = AppBook.text(PREFIX + "CONFIRMATION",
    "Are you sure you want to remove {0,choice,1#the selected connection|1<selected connections}?", ONE);
  private static final LText PLEASE_WAIT = AppBook.text(PREFIX + "PLEASE_WAIT",
    "Calculating statistics, please wait\u2026");
  private static final LText1<Long> TOTAL_REPORT = AppBook.text(PREFIX + "TOTAL_REPORT",
    "Total " + Local.text(Terms.key_artifacts) + ": {0,number,######}", 0L);
  private static final LText1<Long> UNCOMMITED_REPORT = AppBook.text(PREFIX + "UNCOMMITTED_REPORT",
    "Locally modified " + Local.text(Terms.key_artifacts) + ": {0, number, ######}", 0L);
  private static final LText1<Integer> NAME = AppBook.text(PREFIX + "ACTION_NAME",
    "&Remove {0,choice,1#Connection|1<Connections}", ONE);
  private static final LText1<Integer> TOOLTIP = AppBook.text(PREFIX + "REMOVE_CONNECTION_ACTION_SHORT_DESC",
    "Remove {0,choice,1#connection|1<connections} and all {0,choice,1#its|1<their} " + Local.text(Terms.key_artifacts), ONE);

  private RemoveConnectionsAction() {
    super(NAME.format(ONE), Icons.ACTION_GENERIC_CANCEL_OR_REMOVE);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, TOOLTIP.format(ONE));
  }

  public static RemoveConnectionsAction create() {
    return new RemoveConnectionsAction();
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    Pair<List<ConnectionNode>, JComponent> pair = getConnectionNodes(context);
    final List<ConnectionNode> connectionNodes = pair.getFirst();
    final int size = connectionNodes.size();
    final Engine engine = context.getSourceObject(Engine.ROLE);
    final JLabel label = new JLabel() {
      public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        Font f = getFont();
        if (f != null) {
          FontMetrics metrics = getFontMetrics(f);
          int height = metrics.getHeight() * 3;
          if (size.height < height)
            size = new Dimension(size.width, height);
        }
        return size;
      }
    };
    label.setText("<html><body>" + CONFIRMATION.format(size) + "<br>" + PLEASE_WAIT.format() + "<br>");

    context.getSourceObject(Database.ROLE).readForeground(new ReadTransaction<Pair<Long, Long>>() {
      @Override
      public Pair<Long, Long> transaction(DBReader reader) throws DBOperationCancelledException {
        long total = 0;
        long modified = 0;
        for(final ConnectionNode connectionNode : connectionNodes) {
          final Connection connection = connectionNode.getConnection();
          if(!connection.getState().getValue().isDegrading()) {
            final ConnectionViews views = connection.getViews();
            total += views.getConnectionItems().query(reader).count();
            modified += views.getOutbox().query(reader).count();
          }
        }
        return Pair.create(total, modified);
      }
    }).finallyDo(ThreadGate.AWT, new Procedure<Pair<Long, Long>>() {
      @Override
      public void invoke(Pair<Long, Long> arg) {
        label.setText("<html><body>" + CONFIRMATION.format(connectionNodes.size()) +
          "<br>" + TOTAL_REPORT.format(arg.getFirst()) + "<br>" + UNCOMMITED_REPORT.format(arg.getSecond()));
      }
    });

    if (DialogsUtil.YES_OPTION == DialogsUtil.askUser(pair.getSecond(), label, TITLE.format(size), DialogsUtil.YES_NO_OPTION)) {
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          removeConnections(connectionNodes, engine);
        }
      });
    }
  }

  private void removeConnections(List<ConnectionNode> connectionNodes, Engine engine) {
    for (ConnectionNode connectionNode : connectionNodes) {
      Connection connection = connectionNode.getConnection();
      if (!connection.getState().getValue().isDegrading()) {
        engine.getConnectionManager().removeConnection(connection);
      }
    }
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.watchRole(GenericNode.NAVIGATION_NODE);
    context.setEnabled(EnableState.INVISIBLE);
    List<ConnectionNode> nodes = getConnectionNodes(context).getFirst();
    assert nodes != null;
    assert nodes.size() > 0;
    Integer count = nodes.size();
    context.putPresentationProperty(PresentationKey.NAME, NAME.format(count));
    context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, TOOLTIP.format(count));
    context.setEnabled(EnableState.ENABLED);
  }

  @NotNull
  private Pair<List<ConnectionNode>, JComponent> getConnectionNodes(ActionContext context) throws CantPerformException {
    ComponentContext<JComponent> cc = context.getComponentContext(JComponent.class, GenericNode.NAVIGATION_NODE);
    List<GenericNode> selectedNavigationNodes = cc.getSourceCollection(GenericNode.NAVIGATION_NODE);
    if (selectedNavigationNodes.isEmpty())
      throw new CantPerformException();
    List<ConnectionNode> result = null;
    for (GenericNode genericNode : selectedNavigationNodes) {
      if (genericNode instanceof ConnectionNode) {
        if (result == null)
          result = Collections15.arrayList();
        result.add((ConnectionNode) genericNode);
      } else {
        throw new CantPerformException();
      }
    }
    if (result == null)
      throw new CantPerformException();
    return Pair.create(result, cc.getComponent());
  }
}
