package com.almworks.explorer.tree;

import com.almworks.api.application.tree.*;
import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ViewWeightManager {
  private static final List<Class> CLASSES = Collections15.arrayList();
  private static final Map<Class, Integer> MAP = new HashMap<Class, Integer>();
  private static int counter = 0;

  static {
    add(TagsFolderNode.class);
    add(FavoritesNode.class);
    add(NoteNode.class);
    add(NoteNodeImpl.class);
    add(RootNode.class);
    add(RootNodeImpl.class);
    add(ConnectionNode.class);
    add(ConnectionNodeImpl.class);
    add(ConnectionLoadingNode.class);
    add(ConnectionLoadingNodeImpl.class);
    add(DistributionQueryNode.class);
    add(DistributionQueryNodeImpl.class);
    add(DistributionGroupNode.class);
    add(DistributionGroupNodeImpl.class);
    add(DistributionFolderNode.class);
    add(DistributionFolderNodeImpl.class);
    add(LazyDistributionNodeImpl.class);
    add(DistributionFolderNodeImpl.ExpandingProgressNode.class);
    add(FolderNode.class);
    add(FolderNode.class);
    add(UserQueryNode.class);
    add(UserQueryNodeImpl.class);
    add(TagNode.class);
    add(TagNodeImpl.class);
    add(TemporaryQueriesNode.class);
    add(RemoteQueriesNode.class);
    add(RemoteQueries2Node.class);
    add(RemoteQueryNode.class);
    add(OutboxNode.class);
  }

  private static void add(Class c) {
    CLASSES.add(c);
    MAP.put(c, counter++);
  }

  private ViewWeightManager() {
  }

  public static int getWeight(GenericNode o) {
    if (o == null)
      return 0;
    Integer r = MAP.get(o.getClass());
    if (r != null)
      return r;
    for (int i = 0; i < CLASSES.size(); i++) {
      Class aClass = CLASSES.get(i);
      if (aClass.isInstance(o))
        return i;
    }
    assert false: "Unmapped class: " + o != null ? o.getClass().getName() : "null";
    return 0;
  }

  public static int compare(GenericNode o1, GenericNode o2) {
    return Containers.compareInts(getWeight(o1), getWeight(o2));
  }
}
