package com.almworks.extservice;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.DialogSingleton;
import com.almworks.items.api.DBFilter;
import com.almworks.tracker.alpha.AlphaProtocol;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.actions.*;
import com.almworks.util.xmlrpc.SimpleOutgoingMessage;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collections;
import java.util.Hashtable;

import static com.almworks.tracker.alpha.AlphaProtocol.Messages.ToClient.ADD_WATCHED_COLLECTION;

class AddCollectionDialog extends DialogSingleton {
  private final JPanel myWholePanel = new JPanel(new BorderLayout(0, 5));
  private final JTextArea mySelectionDescription = new JTextArea();
  private final ExplorerComponent myExplorerComponent;
  private static final TypedKey WATCH_COLLECTION = TypedKey.create("");
  private static final Color WATCH_COLLECTION_COLOR = GlobalColors.CORPORATE_COLOR_1;

  private EapiClient myClient;

  public AddCollectionDialog(DialogManager dialogManager, ExplorerComponent explorerComponent) {
    super(dialogManager, "addWatchedCollection");
    myExplorerComponent = explorerComponent;
    setupPanel();
  }

  private void setupPanel() {
    EapiClient client = myClient;
    String clientName = client == null ? "an external tool" : client.getShortName();
    myWholePanel.add(new JLabel("Select a query to be monitored in " + clientName + ":"), BorderLayout.NORTH);
    myWholePanel.add(new JScrollPane(mySelectionDescription), BorderLayout.CENTER);
    mySelectionDescription.setEditable(false);
    myWholePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
  }

  protected void setupDialog(DialogBuilder builder) {
    builder.setTitle("Add Query For External Monitoring");
    builder.setContent(myWholePanel);
    builder.setModal(false);
    builder.setEmptyCancelAction();
    builder.setOkAction(new MyOKAction());
    builder.addProvider(new ComponentDelegateDataProvider(myExplorerComponent.getGlobalContextComponent(), GenericNode.NAVIGATION_NODE));
  }


  protected void detach() {
    super.detach();
    myExplorerComponent.clearHighlightedNodes(WATCH_COLLECTION);
    myClient = null;
  }

  public void show(EapiClient client) {
    if (!isShowing())
      myClient = client;
    super.show();
  }

  private class MyOKAction extends SimpleAction {
    public MyOKAction() {
      super("OK");
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      GenericNode node = null;
      try {
        context.watchRole(GenericNode.NAVIGATION_NODE);
        node = ExtServiceUtils.getNarrowingNode(context);
        DBFilter view = node.getQueryResult().getDbFilter();
        if (view == null) {
          context.setEnabled(false);
          node = null;
        }
      } finally {
        try {
          if (node == null) {
            myExplorerComponent.clearHighlightedNodes(WATCH_COLLECTION);
            mySelectionDescription.setText("No valid query selected");
          } else {
            myExplorerComponent.setHighlightedNodes(WATCH_COLLECTION, Collections.singleton(node),
              WATCH_COLLECTION_COLOR, null, " watch this");
            mySelectionDescription.setText("Watch: " + node.getName());
          }
        } catch (Exception e) {
          Log.error(e);
        }
      }
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      GenericNode node = ExtServiceUtils.getNarrowingNode(context);
      assert node != null;
      String id = node.getNodeId();
      Hashtable props = new Hashtable();
      props.put(AlphaProtocol.Messages.ToClient.CollectionProps.NAME, node.getName());
      myClient.send(new SimpleOutgoingMessage(ADD_WATCHED_COLLECTION, id, Boolean.TRUE, props));
    }
  }
}
