package com.almworks.jira.provider3.links;

import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.components.ATable;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.components.tables.HierarchicalTable;
import com.almworks.util.components.tables.TreeToListAdapter;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class LinksTree {
  private final GuiFeaturesManager myManager;
  private final TreeModelBridge<Pair<Object, TreeModelBridge<?>>> myRoot;

  private LinksTree(GuiFeaturesManager manager, TreeModelBridge<Pair<Object, TreeModelBridge<?>>> root) {
    myManager = manager;
    myRoot = root;
  }

  public static LinksTree create(GuiFeaturesManager manager, @Nullable String rootObj) {
    TreeModelBridge<Pair<Object, TreeModelBridge<?>>> root = createNode0(null);
    LinksTree tree = new LinksTree(manager, root);
    if (rootObj != null) tree.setRootObject(rootObj);
    return tree;
  }

  public ATable<Pair<Object, TreeModelBridge<?>>> showAsTree() {
    ATable<Pair<Object, TreeModelBridge<?>>> table = new ATable<Pair<Object, TreeModelBridge<?>>>();
    attachTable(table);
    return table;
  }

  public void attachTable(ATable<? extends Pair<?, TreeModelBridge<?>>> table) {
    HierarchicalTable tree = LinksController.setupHierarchy(table, myManager);
    tree.setRoot(myRoot);
    tree.sortBy(0, false);
    TreeToListAdapter treeAdapter = tree.getTreeAdapter();
    assert treeAdapter != null;
    treeAdapter.expandAll();
  }

  public String getSummaryString() {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      TreeModelBridge<Pair<Object, TreeModelBridge<?>>> type = myRoot.getChildAt(i);
      int count = type.getChildCount();
      if (count > 0) {
        Pair<Object, TreeModelBridge<?>> pair = type.getUserObject();
        if (pair != null) {
          String typeString = (String) pair.getFirst();
          if (typeString != null && typeString.length() > 0) {
            if (buffer.length() > 0)
              buffer.append(", ");
            buffer.append(typeString).append(' ').append(count);
          }
        }
      }
    }
    return buffer.toString();
  }

  public void update(@Nullable List<? extends LoadedLink> links) {
    links = Util.NN(links, Collections.<LoadedLink>emptyList());
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      TreeModelBridge type = myRoot.getChildAt(i);
      for (int j = 0; j < type.getChildCount(); j++) {
        TreeModelBridge<Pair<LoadedLink, ?>> link = type.getChildAt(j);
        Pair<LoadedLink, ?> obj = link.getUserObject();
        if (obj != null) {
          int index = indexOf(myManager, links, obj.getFirst());
          if (index >= 0) {
            setUserObject(link, links.get(index));
            continue;
          }
        }
        link.removeFromParent();
        j--;
      }
    }
    for (LoadedLink link : links) {
      TreeModelBridge type = findOrCreateType(link);
      ensureExists(type, link);
    }
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      TreeModelBridge type = myRoot.getChildAt(i);
      if (type.getChildCount() == 0)
        type.removeFromParent();
    }
  }

  public static int indexOf(GuiFeaturesManager manager, List<? extends LoadedLink> links, LoadedLink link) {
    if (links == null || link == null) return -1;
    for (int i = 0; i < links.size(); i++) if (isSameTypeAndOpposite(manager, link, links.get(i))) return i;
    return -1;
  }

  public static boolean isSameTypeAndOpposite(GuiFeaturesManager manager, LoadedLink o1, LoadedLink o2) {
    if (o1 == o2)
      return true;
    if (o1 == null || o2 == null)
      return false;
    long type = o1.getType();
    if (type != o2.getType())
      return false;
    boolean isoType = LoadedLink2.isIsotropicType(manager, type);
    if (!isoType && (o1.getOutward() != o2.getOutward())) return false;
    String o1op = o1.getOppositeString(LoadedLink.KEY);
    return o1op != null && o1op.equalsIgnoreCase(o2.getOppositeString(LoadedLink.KEY));
  }

  public void setRootObject(String rootObj) {
    myRoot.setUserObject(Pair.<Object, TreeModelBridge<?>>create(new String[] {rootObj}, myRoot));
  }

  public TreeModelBridge<Pair<Object, TreeModelBridge<?>>> getRoot() {
    return myRoot;
  }

  private TreeModelBridge<Pair<Object, TreeModelBridge<?>>> findOrCreateType(LoadedLink link) {
    String description = link.getDescription(myManager);
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      TreeModelBridge<Pair<Object, TreeModelBridge<?>>> type = myRoot.getChildAt(i);
      Pair<Object, TreeModelBridge<?>> object = type.getUserObject();
      if (object != null) {
        String d = (String) object.getFirst();
        if (d != null && d.equals(description))
          return type;
      }
    }
    TreeModelBridge<Pair<Object, TreeModelBridge<?>>> child = (TreeModelBridge)createNode(description);
    myRoot.add(child);
    return child;
  }

  private void ensureExists(TreeModelBridge type, LoadedLink newLink) {
    for (int i = 0; i < type.getChildCount(); i++) {
      Pair<LoadedLink, ?> obj = (Pair<LoadedLink, ?>) type.getChildAt(i).getUserObject();
      if (obj != null) {
        LoadedLink first = obj.getFirst();
        if (first != null && isSameTypeAndOpposite(myManager, first, newLink)) return;
      }
    }
    type.add(LinksTree.createNode(newLink));
  }

  private static <T> TreeModelBridge<Pair<T, TreeModelBridge<?>>> createNode(T obj) {
    LogHelper.assertWarning(obj != null);
    return createNode0(obj);
  }

  private static <T> TreeModelBridge<Pair<T, TreeModelBridge<?>>> createNode0(T obj) {
    TreeModelBridge<Pair<T, TreeModelBridge<?>>> node = TreeModelBridge.create(null);
    node.setUserObject(Pair.<T, TreeModelBridge<?>>create(obj, node));
    return node;
  }

  private static <T> void setUserObject(TreeModelBridge<Pair<T, ?>> node, T obj) {
    node.setUserObject(Pair.<T, TreeModelBridge>create(obj, node));
  }
}
