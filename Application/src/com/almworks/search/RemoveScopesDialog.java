package com.almworks.search;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.AList;
import com.almworks.util.components.AScrollPane;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.SizeConstraints;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.TypedKey;

import java.awt.*;
import java.util.Collection;
import java.util.List;

class RemoveScopesDialog {
  private static final TypedKey REMOVING_NODES = TypedKey.create("");
  private static final Color REMOVING_COLOR = GlobalColors.ERROR_COLOR;

  private final ExplorerComponent myExplorer;

  private final OrderListModel<FixedScope> myModel;

  private final AList<FixedScope> myList = new AList<FixedScope>();
  private final AScrollPane myWholePanel = new AScrollPane(myList);
  private boolean myOk = false;
  private List<FixedScope> mySelected;
  private final DialogBuilder myBuilder;

  public RemoveScopesDialog(ActionContext context) throws CantPerformException {
    myExplorer = context.getSourceObject(ExplorerComponent.ROLE);
    myModel = OrderListModel.create();
    myList.setCollectionModel(myModel);
    myList.setCanvasRenderer(Renderers.defaultCanvasRenderer());
    myWholePanel.setSizeDelegate(new SizeConstraints.PreferredSizeBoundedByConstant(new Dimension(100, 200), null));
    DialogManager mgr = context.getSourceObject(DialogManager.ROLE);
    myBuilder = mgr.createBuilder("removeScopes");
    setupDialog();
  }

  protected void setupDialog() {
    myBuilder.setTitle("Remove Search Scopes");
    myBuilder.setModal(true);
    myBuilder.setContent(myWholePanel);
    myBuilder.setEmptyCancelAction();
    myBuilder.setOkAction(new SimpleAction("OK") {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        SelectionAccessor<FixedScope> accessor = myList.getSelectionAccessor();
        context.updateOnChange(accessor);
        FixedScope scope = accessor.getSelection();
        context.setEnabled(scope != null);
        if (scope == null) {
          myExplorer.clearHighlightedNodes(REMOVING_NODES);
        } else {
          Collection<GenericNode> currentScope = scope.getCurrentScope(context);
          myExplorer.setHighlightedNodes(REMOVING_NODES, currentScope, REMOVING_COLOR, null, null);
        }
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        myOk = true;
        mySelected = myList.getSelectionAccessor().getSelectedItems();
      }
    });
    myBuilder.setInitialFocusOwner(myList);
  }

  public void show(List<FixedScope> fixedScopes) {
    myModel.clear();
    myModel.addAll(fixedScopes);
    myBuilder.showWindow();
    myExplorer.clearHighlightedNodes(REMOVING_NODES);
  }

  public Collection<FixedScope> getSelectedScopes() {
    return myOk ? mySelected : null;
  }
}

