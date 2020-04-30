package com.almworks.actions;

import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.search.TextSearchUtils;
import com.almworks.util.commons.FunctionE;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class ConnectionAction extends SimpleAction {
  public static final FunctionE<ActionContext, Connection, CantPerformException> SELECTED_NODE =
    new FunctionE<ActionContext, Connection, CantPerformException>() {
      @Override
      public Connection invoke(ActionContext context) throws CantPerformException {
        final List<GenericNode> nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
        return getConnection(nodes);
      }
    };

  public static final FunctionE<ActionContext, Connection, CantPerformException> PARENT_NODE =
    new FunctionE<ActionContext, Connection, CantPerformException>() {
      @Override
      public Connection invoke(ActionContext context) throws CantPerformException {
        final Collection<GenericNode>
          nodes = TextSearchUtils.escalateToConnectionNodes(context.getSourceCollection(GenericNode.NAVIGATION_NODE));
        return getConnection(nodes);
      }
    };

  private static Connection getConnection(Collection<GenericNode> nodes) {
    if(nodes == null || nodes.size() != 1) {
      return null;
    }
    final GenericNode node = nodes.iterator().next();
    if(node instanceof ConnectionNode) {
      return node.getConnection();
    }
    return null;
  }

  private final FunctionE<ActionContext, Connection, CantPerformException> myConnectionExtractor;

  protected ConnectionAction(@Nullable String name, FunctionE<ActionContext, Connection, CantPerformException> connectionExtractor) {
    super(name);
    myConnectionExtractor = connectionExtractor;
  }

  protected Connection extractConnection(ActionContext context) throws CantPerformException {
    return myConnectionExtractor.invoke(context);
  }
}
