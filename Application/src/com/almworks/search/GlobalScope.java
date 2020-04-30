package com.almworks.search;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Collections;

public class GlobalScope extends SearchScope implements CanvasRenderable {
  public Collection<GenericNode> getCurrentScope(UpdateContext context) throws CantPerformException {
    ExplorerComponent explorer = context.getSourceObject(ExplorerComponent.ROLE);
    RootNode rootNode = explorer.getRootNode();
    if (rootNode == null)
      return Collections15.emptyCollection();
    return Collections.singleton((GenericNode) rootNode);
  }

  public String getName() {
    return "Search everywhere";
  }
}
