package com.almworks.search;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.ErrorHunt;
import com.almworks.util.components.PlainTextCanvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.TreeUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Collection;

public class AddScopeDialog {
  private static final TypedKey NEW_SCOPE = TypedKey.create("");
  private static final Color HIGHLIGHT_COLOR = new Color(0, 0x33, 0xcc);

  private JPanel myWholePanel;
  private JTextField myScopeName;
  private JLabel myScopeNameLabel;
  private JLabel myScopeNodesLabel;
  private JTextPane myScopeNodes;

  private final JComponent mySourceComponent;
  private final ExplorerComponent myExplorer;
  private final DialogBuilder myBuilder;

  private boolean myChangingName = false;
  private boolean myNameChangedManually = false;
  private Collection<GenericNode> myLastNodeList = null;
  private boolean myOk;

  public AddScopeDialog(ActionContext context, JComponent sourceComponent) throws CantPerformException {
    mySourceComponent = sourceComponent;
    myExplorer = context.getSourceObject(ExplorerComponent.ROLE);
    DialogManager mgr = context.getSourceObject(DialogManager.ROLE);
    myBuilder = mgr.createBuilder("addScope");
    setupDialog();
    setupVisual();
    setupNameField();
  }

  private void setupNameField() {
    DocumentUtil.addListener(Lifespan.FOREVER, myScopeName.getDocument(), new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        if (!myChangingName)
          myNameChangedManually = true;
      }
    });
  }

  private void setupVisual() {
    myScopeNameLabel.setLabelFor(myScopeName);
    myScopeNodesLabel.setLabelFor(myScopeNodes);
  }


  protected void setupDialog() {
    myBuilder.setTitle("Add Search Scope");
    myBuilder.setContent(myWholePanel);
    myBuilder.setModal(false);
    myBuilder.addProvider(new ComponentDelegateDataProvider(mySourceComponent, GenericNode.NAVIGATION_NODE));
    myBuilder.setOkAction(new OkAction());
    myBuilder.setEmptyCancelAction();
    myBuilder.setInitialFocusOwner(myScopeName);
  }

  private void updateNodeList(Collection<GenericNode> nodes) {
    String text;
    if (nodes.size() == 0) {
      text = "No nodes selected in the Navigation Tree.";
    } else {
      StringBuffer buffer = new StringBuffer();
      for (GenericNode node : nodes) {
        buffer.append(SearchComponentUtils.getStringPath(node)).append('\n');
      }
      text = buffer.toString();
    }
    ErrorHunt.setEditorPaneText(myScopeNodes, text);
    UIUtil.scrollToTopDelayed(myScopeNodes);
    myExplorer.setHighlightedNodes(NEW_SCOPE, nodes, HIGHLIGHT_COLOR, Icons.LOOKING_GLASS, "new scope");
    setDefaultName(nodes);
    myLastNodeList = nodes;
  }

  private void setDefaultName(Collection<GenericNode> nodes) {
    if (myNameChangedManually)
      return;
    String text;
    if (nodes.size() == 0) {
      text = "";
    } else if (nodes.size() == 1) {
      text = SearchComponentUtils.getStringPath(nodes.iterator().next());
    } else {
      int count = 0;
      StringBuffer result = new StringBuffer();
      PlainTextCanvas canvas = new PlainTextCanvas();
      for (GenericNode node : nodes) {
        if (++count > 10) {
          result.append(" and other");
          break;
        }
        if (count > 1) {
          result.append(", ");
        }
        node.getPresentation().renderOn(canvas, CellState.LABEL);
        result.append(canvas.getText());
        canvas.clear();
      }
      text = result.toString();
    }
    myChangingName = true;
    myScopeName.setText(text);
    myChangingName = false;
  }

  public FixedScope getNewScope() {
    String name = Util.NN(myScopeName.getText().trim(), "Unnamed");
    if ((myOk && myLastNodeList != null && myLastNodeList.size() > 0))
      return FixedScope.create(name, myLastNodeList);
    else
      return null;
  }

  public void show(final Runnable whenFinished) {
    myBuilder.showWindow(new Detach() {
      protected void doDetach() {
        myExplorer.clearHighlightedNodes(NEW_SCOPE);
        whenFinished.run();
      }
    });
  }

  private class OkAction extends SimpleAction {
    public OkAction() {
      super("OK");
      watchRole(GenericNode.NAVIGATION_NODE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      Collection<GenericNode> nodes;
      try {
        nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
        nodes = TreeUtil.excludeDescendants(nodes, GenericNode.GET_PARENT_NODE);
      } catch (CantPerformException e) {
        nodes = Collections15.emptyList();
      }
      updateNodeList(nodes);
      context.setEnabled(nodes.size() > 0);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      myOk = true;
    }
  }
}
