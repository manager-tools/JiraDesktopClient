package com.almworks.api.application.tree;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Engine;
import com.almworks.api.syncreg.SyncRegistry;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.util.commons.Condition;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public interface RootNode extends GenericNode {
  ComponentContainer getContainer();

  Engine getEngine();

  @NotNull
  SyncRegistry getSyncRegistry();

  TreeNodeFactory getNodeFactory();

  List<GenericNode> collectNodes(Condition<GenericNode> condition);

  @Nullable
  @ThreadAWT
  GenericNode getNodeById(String nodeId);

  ItemsPreviewManager getItemsPreviewManager();

  @ThreadSafe
  Map<DBIdentifiedObject, TagNode> getTags();
}
