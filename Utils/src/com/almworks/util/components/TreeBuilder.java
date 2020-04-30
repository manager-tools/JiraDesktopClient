package com.almworks.util.components;

import com.almworks.util.collections.MultiMap;
import com.almworks.util.commons.Condition;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultTreeModel;
import java.util.*;

import static com.almworks.util.collections.Functional.*;
import static org.almworks.util.Collections15.*;

/**
 * Builds a ("multi") tree according to {@link TreeStructure}.<br/>
 *
 * Throughout the code the following idioms are used:
 * <ul>
 *   <li><i>element</i>: logical entity; elements form a graph that is to be represented by a "tree"</li>
 *   <li><i>key</i>: object that represents element identity</li>
 *   <li><i>node</i>: node of a resulting tree; <br/>
 *   {@code node * <-> 1 element} in case {@link TreeStructure} is {@link com.almworks.util.components.TreeStructure.MultiParent MultiParent}, <br/>
 *   {@code node 1 <-> 1 element} otherwise.
 *   </li>
 * </ul>
 */
public class TreeBuilder<E, N extends TreeModelBridge<? extends E>> {
  private final TreeStructure<E, ?, N> myTreeStructure;
  @Nullable
  private final Comparator<? super E> myComparator;
  private final N myRoot;
  private final MultiMap<Object, N> myNodes = MultiMap.create();
  /**
   * Orphaned element is an element such that one of its parents is not in the model. <br/>
   * If every parent of the element is not in the model, it is put under the root. <br/>
   * Note that not all elements that are just under the root are orphans: orphans are only those node that have parents which are not in the model, other top-level elements just don't have parents.<Br/>
   * Map is from the missing parents (concretely, missing parents' keys) to orphaned elements list. */
  private final MultiMap<Object, E> myOrphanedElements = MultiMap.create();

  /** @param root must be the only node in the tree that does not have a parent */
  public TreeBuilder(TreeStructure<E, ?, N> treeStructure, @Nullable Comparator<? super E> comparator, N root) {
    myTreeStructure = treeStructure;
    myComparator = comparator;
    myRoot = root;
  }

  public void addElement(E element) {
    Object key = myTreeStructure.getNodeKey(element);
    if (myNodes.containsKey(key)) {
      return;
    }

    // "Meet the parents" - collect parent nodes
    Set<Object> parentKeys = getParentKeys(element);
    List<N> parentNodes = arrayList();
    for (Object parentKey : parentKeys) {
      if (!Util.equals(key, parentKey)) {
        List<N> existingParents = myNodes.getAll(parentKey);
        if (existingParents == null) {
          addOrphanedElement(parentKey, element);
        } else {
          parentNodes.addAll(existingParents);
        }
      }
    }
    if (parentNodes.isEmpty()) {
      parentNodes.add(myRoot);
    }

    // Under each parent we'll insert a copy of the corresponding node
    List<N> nodes = arrayList(parentNodes.size());
    for (N parentNode : parentNodes) {
      N node = myTreeStructure.createTreeNode(element);
      nodes.add(node);
      insertNode(parentNode, node);
    }
    myNodes.addAll(key, nodes);

    // "Adopt" orphaned elements
    List<E> orphanedElements = myOrphanedElements.removeAll(key);
    for (E orphanedElement : Util.NN(orphanedElements, Collections15.<E>emptyList())) {
      List<N> orphanedNodes = myNodes.getAll(myTreeStructure.getNodeKey(orphanedElement));
      assert orphanedNodes != null && !orphanedNodes.isEmpty();
      adoptOrphans(nodes, orphanedNodes);
    }

    assert checkConsistency();
  }

  private void adoptOrphans(List<N> parentNodes, List<N> orphanNodes) {
    List<N> spareNodes = arrayList();
    for (N orphan : orphanNodes) {
      // Note that an orphaned element (element missing {parent = the node being inserted}) may have a node that has {parent != node being inserted}
      if (isTopLevel(orphan)) {
        spareNodes.add(orphan);
      }
      // Prevent cycles
      // todo JCO-1164 these checks are insufficient: need to check whole subtrees of orphans against ancestor path of parentNodes
      for (N node : parentNodes) {
        if (orphan.isAncestorOf(node)) {
          return;
        }
      }
    }
    assert spareNodes.size() <= 1;
    Iterator<N> spares = spareNodes.iterator();
    for (N node : parentNodes) {
      moveSpareOrCopy(node, spares, orphanNodes.get(0));
    }
    while (spares.hasNext()) {
      removeSubtree(spares.next());
    }
  }

  /** Every visible node has a parent, top-level nodes have a special parent {@link #myRoot} which has no parents. This method does not work for myRoot. */
  private boolean isTopLevel(N node) {
    TreeModelBridge<? extends E> parent = node.getParent();
    assert parent != null : node;
    return parent.getParent() == null;
  }

  private Set<Object> getParentKeys(E element) {
    TreeStructure<E, ?, N> treeStructure = myTreeStructure;
    return (Set<Object>) getParentKeys(element, treeStructure);
  }

  @NotNull
  public static <E, K, N extends TreeModelBridge<? extends E>> Set<K> getParentKeys(E element, TreeStructure<E, K, N> treeStructure) {
    if(treeStructure instanceof TreeStructure.MultiParent) {
      return ((TreeStructure.MultiParent) treeStructure).getNodeParentKeys(element);
    }
    final K key = treeStructure.getNodeParentKey(element);
    return key == null ? Collections.<K>emptySet() : Collections.singleton(key);
  }

  @Nullable
  public List<N> findNodes(E element) {
    Object key = myTreeStructure.getNodeKey(element);
    return myNodes.getAll(key);
  }

  public void removeElement(E element) {
    final Object key = myTreeStructure.getNodeKey(element);
    if(!myNodes.containsKey(key)) {
      return;
    }

    final List<N> nodes = myNodes.removeAll(key);
    assert nodes != null;

    final MultiMap<Object, N> allOrphanedNodes = MultiMap.create();
    for(final N node : nodes) {
      node.removeFromParent();
      final List<? extends N> orphanedNodes = (List)node.childrenToList();
      for(final N orphanedNode : orphanedNodes) {
        final E e = orphanedNode.getUserObject();
        final Object k = myTreeStructure.getNodeKey(e);
        allOrphanedNodes.add(k, orphanedNode);
        addOrphanedElement(key, e);
      }
    }

    for(final Object k : allOrphanedNodes.keySet()) {
      final List<N> orphanedNodes = allOrphanedNodes.getAll(k);
      assert orphanedNodes != null && !orphanedNodes.isEmpty();
      final Iterator<N> it = orphanedNodes.iterator();
      final List<N> allNodes = myNodes.getAll(k);
      assert allNodes != null && !allNodes.isEmpty() && allNodes.containsAll(orphanedNodes);
      if(orphanedNodes.containsAll(allNodes)) {
        it.next().moveTo(myRoot, myComparator);
      }
      while(it.hasNext()) {
        removeSubtree(it.next());
      }
    }

    removeOrphanedElement(element);

    assert checkConsistency();
  }

  public final void updateAll(Collection<? extends E> elements) {
    MultiMap<TreeModelBridge, N> updates = MultiMap.create();
    DefaultTreeModel commonModel = null;
    for (E element : elements) {
      List<N> nodes = myNodes.getAll(myTreeStructure.getNodeKey(element));
      if (nodes == null) {
        assert false : element;
        Log.warn("cannot update " + element);
        continue;
      }

      Set<Object> newParentKeys = getParentKeys(element);
      if (newParentKeys.isEmpty()) {
        // Element now has no parents; after we finish, there will be only one node
        commonModel = updateNoParents(element, nodes, updates, commonModel);
      } else {
        commonModel = updateWithParents(element, nodes, updates, newParentKeys, commonModel);
      }
    }

    if (updates.isEmpty() == (commonModel == null)) {
      fireMultipleUpdates(updates, commonModel);
    } else {
      Log.error("TB: inconsistent update " + updates + " " + commonModel);
    }

    assert checkConsistency();
  }

  private void fireMultipleUpdates(MultiMap<TreeModelBridge, N> updates, DefaultTreeModel commonModel) {
    for (TreeModelBridge parent : updates.keySet()) {
      List<N> updatedChildren = updates.getAll(parent);
      assert !isEmpty(updatedChildren) : String.valueOf(updatedChildren);
      // null elements are needed to mark the parent node for order update
      updatedChildren = filterToList(updatedChildren, Condition.<N>notNull());
      for (N child : updatedChildren)
        child.fireChanged_DontNotifyModel();
      //noinspection ConstantConditions
      if (!isEmpty(updatedChildren))
        commonModel.nodesChanged(parent, parent.getChildIndecies(updatedChildren));
      if (myComparator != null)
        parent.updateOrder(myComparator);
    }
  }

  private DefaultTreeModel updateNoParents(E element, List<N> nodes, MultiMap<TreeModelBridge, N> updates, DefaultTreeModel commonModel) {
    N first = null;
    for (N node : nodes) {
      assert node.getParent() != null;
      if (isTopLevel(node)) {
        first = node;
        break;
      }
    }
    if (first != null) {
      commonModel = accumulateSingleUpdate(first, false, updates, commonModel);
    } else {
      first = nodes.get(0);
      first.moveTo(myRoot, myComparator);
    }
    for (N node : arrayList(nodes)) {
      if (node != first) {
        removeSubtree(node);
      }
    }
    removeOrphanedElement(element);
    assert !hasNth(myNodes.getAll(element), 1) : element + " " + myNodes;
    return commonModel;
  }

  private DefaultTreeModel updateWithParents(E element, List<N> nodes, MultiMap<TreeModelBridge, N> updates, Set<Object> newParentKeys, DefaultTreeModel commonModel) {
    // Remove entries corresponding to the updated element for the removed parents
    for (Object k : myOrphanedElements.keySet().toArray()) {
      if (myOrphanedElements.hasValue(k, element) && !newParentKeys.contains(k)) {
        myOrphanedElements.remove(k, element);
      }
    }

    Set<Object> addedParentKeys = hashSet(newParentKeys);
    List<N> spareNodes = arrayList();
    boolean seenUnchangedParents = false;
    for (N node : nodes) {
      TreeModelBridge<? extends E> p = node.getParent();
      //noinspection ConstantConditions
      if (p.getParent() == null) {
        // Node parent is the "root" (invisible), so this is a top-level node in the GUI 
        spareNodes.add(node);
      } else {
        Object pk = myTreeStructure.getNodeKey(p.getUserObject());
        if (newParentKeys.contains(pk)) {
          commonModel = accumulateSingleUpdate(node, true, updates, commonModel);
          seenUnchangedParents = true;
        } else {
          spareNodes.add(node);
        }
        addedParentKeys.remove(pk);
      }
    }

    List<N> newParents = arrayList();
  ADDED_KEYS:
    for (Object addedKey : addedParentKeys) {
      List<N> parentNodes = myNodes.getAll(addedKey);
      if (parentNodes == null) {
        addOrphanedElement(addedKey, element);
      } else {
        // todo JCO-1164 these checks are too local: need to check the whole subtree of each node from nodes to be not an ancestor of any ancestor of any parentNode 
        for (N parentNode : parentNodes) {
          for (N node : nodes) {
            if (node.isAncestorOf(parentNode)) {
              continue ADDED_KEYS;
            }
          }
        }
        newParents.addAll(parentNodes);
      }
    }

    if (newParents.isEmpty() && !seenUnchangedParents) {
      newParents.add(myRoot);
    }

    Iterator<N> it = spareNodes.iterator();
    for (N newParent : newParents) {
      moveSpareOrCopy(newParent, it, nodes.get(0));
    }

    while (it.hasNext()) {
      removeSubtree(it.next());
    }

    return commonModel;
  }

  private DefaultTreeModel accumulateSingleUpdate(N node, boolean onlyUpdateOrder, MultiMap<TreeModelBridge, N> updates, DefaultTreeModel commonModel) {
    TreeModelBridge<? extends E> parent = node.getParent();
    DefaultTreeModel model = node.getTreeModel();
    if (commonModel == null) {
      commonModel = model;
    }
    if (commonModel == model) {
      updates.add(parent, onlyUpdateOrder ? null : node);
    } else {
      Log.error("TB: different model " + node + " " + model + " " + commonModel);
      // This can be really slow
      node.fireChanged();
      if (myComparator != null && parent != null) parent.updateOrder(myComparator);
    }
    return commonModel;
  }

  private void moveSpareOrCopy(N root, Iterator<N> spares, N sample) {
    if(spares.hasNext()) {
      spares.next().moveTo(root, myComparator);
    } else {
      insertNode(root, copySubtree(sample));
    }
  }

  public void clear() {
    myRoot.removeAll();
    myNodes.clear();
    myOrphanedElements.clear();
  }

  public void removeFromTree() {
    if (myRoot.getParent() != null)
      myRoot.removeFromParent();
  }

  public final void addAll(Collection<? extends E> ts) {
    for (E e : ts)
      addElement(e);
  }

  public final void updateElement(E element) {
    updateAll(Collections.singleton(element));
  }

  public final N getRoot() {
    return myRoot;
  }

  public final void removeAll(Collection<E> elements) {
    for(E e : elements) {
      removeElement(e);
    }
  }

  private void insertNode(N parent, N child) {
    if(myComparator != null) {
      parent.addOrdered((TreeModelBridge)child, myComparator);
    } else {
      parent.add((TreeModelBridge)child);
    }
  }

  private void removeSubtree(N root) {
    root.removeFromParent();
    removeSubtree0(root);
  }

  private void removeSubtree0(N root) {
    myNodes.remove(myTreeStructure.getNodeKey(root.getUserObject()), root);
    for(int i = 0; i < root.getChildCount(); i++) {
      removeSubtree0((N)root.getChildAt(i));
    }
  }

  private N copySubtree(TreeModelBridge<? extends E> root) {
    final E element = root.getUserObject();
    final N newRoot = myTreeStructure.createTreeNode(element);
    myNodes.add(myTreeStructure.getNodeKey(element), newRoot);
    for(int i = 0; i < root.getChildCount(); i++) {
      newRoot.add((TreeModelBridge)copySubtree(root.getChildAt(i)));
    }
    return newRoot;
  }

  private void addOrphanedElement(Object key, E element) {
    if(myOrphanedElements.hasValue(key, element)) {
      return;
    }
    myOrphanedElements.add(key, element);
  }

  private void removeOrphanedElement(E element) {
    for(final Object k : myOrphanedElements.keySet().toArray()) {
      myOrphanedElements.remove(k, element);
    }
  }

  private boolean checkConsistency() {
    if (myNodes.size() > 1500) return true; // Limit performance degradation
    final List<String> errors = checkConsistency0();
    if(errors != null && !errors.isEmpty()) {
      for(final String error : errors) {
        Log.warn(error);
      }
      return false;
    }
    return true;
  }

  private List<String> checkConsistency0() {
    final List<String> errors = linkedList();

    for(final Object key : myOrphanedElements.keySet()) {
      if(myNodes.containsKey(key)) {
        errors.add("Orphaned elements found for key " + key + ", which is present in the cache.");
      }
      final List<E> elems = myOrphanedElements.getAll(key);
      if(elems == null || elems.isEmpty()) {
        errors.add("Bad orphaned elements list " + elems + " for key " + key + ".");
      } else {
        final Map<E, Boolean> map = new IdentityHashMap<E, Boolean>();
        for(final E elem : elems) {
          if(map.put(elem, Boolean.TRUE) != null) {
            errors.add("Orphaned element " + elem + " repeats for key " + key + ".");
          }
        }
      }
    }

    final Map<N, Boolean> modelNodes = new IdentityHashMap<N, Boolean>();
    List<N> front = Collections.singletonList(myRoot);
    while(!front.isEmpty()) {
      final List<N> nextFront = arrayList();
      for(final N node : front) {
        for(final ATreeNode<? extends E> child : node.childrenToList()) {
          final Boolean previous = modelNodes.put((N)child, Boolean.TRUE);
          if(previous != null) {
            errors.add("Node " + child + " repeats in the tree model.");
          }
          nextFront.add((N)child);
        }
      }
      front = nextFront;
    }

    final Map<N, Boolean> cacheNodes = new IdentityHashMap<N, Boolean>();
    for(final Object key : myNodes.keySet()) {
      final List<N> nodes = myNodes.getAll(key);
      if(nodes == null || nodes.isEmpty()) {
        errors.add("Bad cache list " + nodes + " for key " + key + ".");
      } else {
        for(final N node : nodes) {
          final Boolean previous = cacheNodes.put(node, Boolean.TRUE);
          if(previous != null) {
            errors.add("Node " + node + " repeats in the cache.");
          }
        }
      }
    }

    if(modelNodes.size() != cacheNodes.size()) {
      errors.add("Size mismatch: " + modelNodes.size() + " in the model, " + cacheNodes.size() + " in the cache.");
    }

    for(final N modelNode : modelNodes.keySet()) {
      if(!cacheNodes.containsKey(modelNode)) {
        errors.add("Model node " + modelNode + " is not present in the cache.");
      }
    }

    for(final N cacheNode : cacheNodes.keySet()) {
      if(!modelNodes.containsKey(cacheNode)) {
        errors.add("Cache node " + cacheNode + " is not present in the model.");
      }
    }

    boolean noDupKeysOnPaths = checkNoDuplicateKeysOnPathsFromRoot(linkedHashSet(), myRoot, /*errors*/null);

    // Due to the locality of checks, there actually may be duplicate keys on paths from roots. And they will lead to errors reported during the next check.
    // If we are going to fix it, look at todo JCO-1164  
    if (noDupKeysOnPaths) {
      for (final Object key : myNodes.keySet()) {
        final List<N> nodes = myNodes.getAll(key);
        if (nodes == null || nodes.size() < 2) {
          continue;
        }
        final Iterator<N> it = nodes.iterator();
        final N firstChild = it.next();
        final List<Object> childKeys = getChildKeys(firstChild, errors);
        while (it.hasNext()) {
          final N nextChild = it.next();
          final List<Object> nextChildKeys = getChildKeys(nextChild, errors);
          if (!nextChildKeys.equals(childKeys)) {
            errors.add(
              "Child keys " + nextChildKeys + " for node " + nextChild
                + " don't match " + childKeys + " for node " + firstChild + ".");
          }
        }
      }
    }

    return errors;
  }

  private boolean checkNoDuplicateKeysOnPathsFromRoot(LinkedHashSet<Object> keys, TreeModelBridge<? extends E> node, @Nullable List<String> errors) {
    E element = node.getUserObject();
    boolean checkResult = true;
    Object key = element == null ? null : myTreeStructure.getNodeKey(element);
    if (key != null && !keys.add(key)) {
      StringBuilder error = new StringBuilder();
      error.append(element).append(" twice in path from root: ");
      for (Object pathKey : keys) error.append("\n\t").append(myNodes.getLast(pathKey));
      if (errors != null) errors.add(error.toString());
      checkResult = false;
    }
    for (int i = 0, iEnd = node.getChildCount(); i < iEnd; ++i) {
      checkResult &= checkNoDuplicateKeysOnPathsFromRoot(keys, node.getChildAt(i), errors);
    }
    keys.remove(key);
    return checkResult;
  }

  private List<Object> getChildKeys(N node, List<String> errors) {
    final List<? extends ATreeNode<? extends E>> children = node.childrenToList();
    if(children.isEmpty()) {
      return Collections.emptyList();
    }
    final List<Object> childKeys = arrayList();
    for(final ATreeNode<? extends E> child : children) {
      final Object childKey = myTreeStructure.getNodeKey(child.getUserObject());
      if(childKeys.contains(childKey)) {
        errors.add("Node for key " + childKey + " repeats under parent node " + node + ".");
      }
      childKeys.add(childKey);
    }
    return childKeys;
  }
}
