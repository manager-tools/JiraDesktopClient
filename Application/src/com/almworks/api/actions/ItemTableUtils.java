package com.almworks.api.actions;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ReplaceTabKey;
import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.QueryUtil;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.TableController;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;

public class ItemTableUtils {
  public static void showArtifactSource(ActionContext context, String queryName, Connection connection,
    ItemSource source, ReplaceTabKey tabKey)
    throws CantPerformException
  {
    ItemCollectionContext currentContext;
    try {
      TableController table = context.getSourceObject(TableController.DATA_ROLE);
      currentContext = table.getItemCollectionContext();
    } catch (CantPerformException e) {
      currentContext = null;
    }
    ExplorerComponent explorer = context.getSourceObject(ExplorerComponent.ROLE);
    ConnectionNode node = CantPerformException.ensureNotNull(QueryUtil.findConnectionNode(explorer, connection));
    ItemCollectionContext linkedContext = ItemCollectionContext.createLinked(node, queryName, tabKey, currentContext);
    explorer.showItemsInTab(source, linkedContext, true);
  }
}
