package com.almworks.util.components;

import com.almworks.util.collections.ListPatch;
import com.almworks.util.collections.ListPatchPolicy;
import com.almworks.util.collections.ListPatchStep;
import com.almworks.util.commons.Condition2;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

// todo #1037
public class TreeFilterNode<T> extends AbstractATreeWritableNode<T> {
  private static final TreeFilterNode[] EMPTY_CHILDREN = {};
  private final TreeModelFilter<T> myFilter;

  @Nullable("when no children have been added yet")
  private List<TreeFilterNode<T>> myChildren;

  @Nullable
  private ATreeNode<T> mySourceNode;

  @Nullable
  private TreeFilterNode<T> myParent;

  /**
   * used when an image node, temporarily removed, is added back to the tree.
   */
  private TreeFilterNode<T> myReuseAddedNodeHint;
  private int myParentIndex;

  private Throwable myCreationStackTrace;

  TreeFilterNode(TreeModelFilter<T> filter, TreeFilterNode<T> parent, int parentIndex, ATreeNode<T> sourceNode) {
    myFilter = filter;
    myParent = parent;
    myParentIndex = parentIndex;
    mySourceNode = sourceNode;
    assert debugNodeCreation();
  }

  private boolean debugNodeCreation() {
    myCreationStackTrace = new Throwable();
    return true;
  }

  public TreeFilterNode<T> getChildAt(int childIndex) {
    if (myChildren == null)
      throw new ArrayIndexOutOfBoundsException(childIndex);
    return myChildren.get(childIndex);
  }

  public TreeFilterNode<T> getParent() {
    return myParent;
  }

  public TreeFilterNode<T>[] childrenToArray() {
    int size = getChildCount();
    //noinspection ConstantConditions
    return size == 0 ? EMPTY_CHILDREN : myChildren.toArray(new TreeFilterNode[size]);
  }

  public TreePath getPathFromRoot() {
    return UIUtil.getPathToRoot(this);
  }

  public List<ATreeNode<T>> childrenToList() {
    int size = getChildCount();
    if (size == 0)
      return Collections15.emptyList();
    return Collections15.<ATreeNode<T>>arrayList(myChildren);
  }

  public List<ATreeNode<T>> childrenToList(List<ATreeNode<T>> destination) {
    if (myChildren != null)
      destination.addAll(myChildren);
    return destination;
  }

  public boolean isAttachedToModel() {
    return myFilter.isAttachedToModel();
  }

  public void insert(ATreeNode<T> child, int index) {
    assert mySourceNode != null;
    assert index >= 0 && index <= getChildCount() : index + " " + getChildCount();
    int sourceIndex = getSourceInsertionIndex(index);
    if (child instanceof TreeFilterNode) {
      TreeFilterNode orphanFilterNode = ((TreeFilterNode) child);
      if (orphanFilterNode.getFilter() == myFilter && orphanFilterNode.getParent() == null) {
        // this node is the node from this tree that has been removed earlier.
        insertMyNode(orphanFilterNode, sourceIndex);
        return;
      }
    }
    assert mySourceNode != null;
    mySourceNode.insert(child, sourceIndex);
  }

  private int getSourceInsertionIndex(int index) {
    ATreeNode<T> sourceNode = mySourceNode;
    assert sourceNode != null;
    int sourceIndex;
    if (index == getChildCount()) {
      sourceIndex = sourceNode.getChildCount();
    } else if (index == 0) {
      sourceIndex = 0;
    } else {
      TreeFilterNode<T> afterThis = getChildAt(index - 1);
      ATreeNode<T> afterThisSource = afterThis.getSourceNode();
      sourceIndex = sourceNode.getIndex(afterThisSource) + 1;
    }
    assert sourceIndex >= 0 && sourceIndex <= sourceNode.getChildCount();
    return sourceIndex;
  }

  private void insertMyNode(TreeFilterNode<T> child, int sourceIndex) {
    assert mySourceNode != null;
    assert child.getFilter() == myFilter : child.getFilter() + " " + getFilter();
    assert child.getSourceNode() != null : child;
    assert child.getSourceNode().getParent() == null : child.getSourceNode();
    myReuseAddedNodeHint = child;
    //noinspection ConstantConditions
    mySourceNode.insert(child.getSourceNode(), sourceIndex);
    // if source node was allowed, this would be true.
    // todo allow added nodes anyway, regardless of filter
    boolean allowed = myReuseAddedNodeHint == null;
    myReuseAddedNodeHint = null;
  }

  public void replaceRoot(DefaultTreeModel model) {
    assert model != null;
    Object oldRoot = model.getRoot();
    if (oldRoot == this)
      return;
    assert this == myFilter.getFilteredRoot();
    model.setRoot(this);
    myFilter.setModel(model);
  }

  public ATreeNodeManager<T> getNodeManager() {
    return myFilter;
  }

  public TreeFilterNode<T> remove(int index) {
    assert index >= 0 && index < getChildCount();
    TreeFilterNode<T> child = getChildAt(index);
    ATreeNode<T> sourceChild = child.getSourceNode();
    ATreeNode<T> sourceParent = getSourceNode();
    int sourceIndex = sourceParent.getIndex(sourceChild);
    assert sourceIndex >= 0;
    ATreeNode<T> removed = sourceParent.remove(sourceIndex);
    assert removed == sourceChild;
    assert child.getParent() == null;
    assert getIndex(child) == -1;
    return child;
  }

  public void setUserObject(T object) {
    ATreeNode<T> sourceNode = mySourceNode;
    assert sourceNode != null : this;
    if (sourceNode != null)
      sourceNode.setUserObject(object);
  }

  public T getUserObject() {
    ATreeNode<T> sourceNode = mySourceNode;
    return sourceNode == null ? null : sourceNode.getUserObject();
  }

  public int getChildCount() {
    return myChildren == null ? 0 : myChildren.size();
  }

  public int getIndex(TreeNode node) {
    if (node instanceof TreeFilterNode) {
      return ((TreeFilterNode) node).myParentIndex;
    } else {
      return myChildren == null ? -1 : myChildren.indexOf(node);
    }
  }

  public boolean getAllowsChildren() {
    return true;
  }

  public boolean isLeaf() {
    return getChildCount() == 0;
  }

  public Enumeration children() {
    return Collections.enumeration(myChildren == null ? Collections15.emptyList() : myChildren);
  }

  public void fireChanged() {
    assert mySourceNode != null;
    if (mySourceNode != null)
      mySourceNode.fireChanged();
  }

  void clear() {
    if (myChildren != null) {
      for (TreeFilterNode child : myChildren) {
        child.clear();
      }
    }
    myChildren = null;
    mySourceNode = null;
    myParent = null;
    myParentIndex = -1;
  }

  ATreeNode<T> getSourceNode() {
    return mySourceNode;
  }

  void nodesChanged(int[] sourceChildIndices) {
    assert getRoot() == myFilter.getFilteredRoot() : this;
    if (sourceChildIndices == null || sourceChildIndices.length == 0)
      return;
    Object[] sourceChildren = getSourceChildren(sourceChildIndices);
    int[] imageIndices = getImageIndicesBySourceNodes(sourceChildren);
    if (imageIndices.length != 0) {
      myFilter.notifyModelNodesChanged(this, imageIndices);
    }
    checkChangedNodesAgainstFilter(sourceChildIndices);
  }

  private Object[] getSourceChildren(int[] sourceChildIndices) {
    int length = sourceChildIndices.length;
    ATreeNode<T> sourceNode = mySourceNode;
    if (length == 0 || sourceNode == null)
      return Const.EMPTY_OBJECTS;
    assert sourceNode != null;
    Object[] sourceChildren = new Object[sourceChildIndices.length];
    for (int i = 0; i < length; i++)
      sourceChildren[i] = sourceNode.getChildAt(sourceChildIndices[i]);
    return sourceChildren;
  }

  private void checkChangedNodesAgainstFilter(int[] sourceChildIndices) {
    int length = sourceChildIndices.length;
    ATreeNode<T> sourceNode = mySourceNode;
    if (length == 0 || sourceNode == null)
      return;
    assert sourceNode != null;
    for (int index : sourceChildIndices) {
      ATreeNode<T> sourceChild = sourceNode.getChildAt(index);
      boolean accepted = myFilter.accept(sourceChild);
      TreeFilterNode image = myFilter.getMapped(sourceChild);
      if (image == null && accepted) {
        seekPlaceAndAddChild(index, sourceChild);
      } else if (image != null && !accepted) {
        assert image.getParent() == this;
        int parentIndex = image.getParentIndex();
        assert parentIndex >= 0 && parentIndex < getChildCount();
        assert getChildAt(parentIndex) == image;
        TreeFilterNode removed = removeChild(parentIndex);
        assert removed == image;
        if (removed != image)
          continue;
        if (removed != null)
          myFilter.notifyModelNodesWereRemoved(this, new int[] {parentIndex}, new Object[] {removed});
      }
    }
  }

  void nodesInserted(int[] sourceChildIndices) {
    assert getRoot() == myFilter.getFilteredRoot() : this;
    int length = sourceChildIndices.length;
    ATreeNode<T> sourceNode = mySourceNode;
    if (length == 0 || sourceNode == null)
      return;
    assert sourceNode != null;
    for (int i = 0; i < length; i++) {
      ATreeNode<T> node = sourceNode.getChildAt(sourceChildIndices[i]);
      assert node != null;
      assert myFilter.getMapped(node) == null;
      if (!myFilter.accept(node))
        continue;
      seekPlaceAndAddChild(sourceChildIndices[i], node);
    }
  }

  /**
   * Searches if any peer source node before the given in arguments is present in image. If yes,
   * inserts this node right after. If no, inserts as a first node.
   */
  private void seekPlaceAndAddChild(int sourceIndex, ATreeNode<T> sourceNode) {
    int insertAfter = -1;
    for (int k = sourceIndex - 1; k >= 0; k--) {
      assert mySourceNode != null : this;
      ATreeNode<T> peer = mySourceNode.getChildAt(k);
      TreeFilterNode peerImage = myFilter.getMapped(peer);
      if (peerImage != null) {
        assert peerImage.getParent() == this;
        if (peerImage.getParent() != this) {
          Log.warn("node filter mismatch: " + sourceNode + "==>" + this + "; " + peer + "=>" + peerImage  + " " + peerImage.getParent());
          continue;
        }
        insertAfter = peerImage.getParentIndex();
        assert insertAfter >= 0 && insertAfter < getChildCount();
        break;
      }
    }
    int place = insertAfter + 1;
    addChild(sourceNode, place);
    myFilter.notifyModelNodesWereInserted(this, new int[] {place});
  }

  void nodesRemoved(Object[] sourceChildren) {
    assert getRoot() == myFilter.getFilteredRoot() : this;
    int[] imageIndices = getImageIndicesBySourceNodes(sourceChildren);
    if (imageIndices.length == 0)
      return;
    TreeFilterNode[] removed = new TreeFilterNode[imageIndices.length];
    for (int i = 0; i < imageIndices.length; i++) {
      removed[i] = removeChild(imageIndices[i] - i);
      assert removed[i] != null;
    }
    myFilter.notifyModelNodesWereRemoved(this, imageIndices, removed);
  }

  private int[] getImageIndicesBySourceNodes(Object[] sourceChildren) {
    int length = sourceChildren.length;
    if (length == 0 || mySourceNode == null)
      return Const.EMPTY_INTS;
    int childCount = getChildCount();
    assert mySourceNode != null;
    int[] imageIndices = new int[length];
    int imageIndicesCount = 0;
    for (int i = 0; i < length; i++) {
      ATreeNode<T> node = (ATreeNode<T>) sourceChildren[i];
      assert node != null;
      TreeFilterNode imageNode = myFilter.getMapped(node);
      if (imageNode == null)
        continue;
      int index = imageNode.getParentIndex();
      assert imageNode.getParent() == this : imageNode + " " + this;
      assert index >= 0 && index < childCount;
      if (myChildren != null) {
        assert myChildren.get(index) == imageNode;
      }
      if (imageNode.getParent() != this || index < 0 || index >= getChildCount() ||
        (myChildren != null && myChildren.get(index) != imageNode))
      {
        Log.warn("tree filter inconsistency " + this + " " + imageNode + " " + node + " " + index);
        continue;
      }
      imageIndices[imageIndicesCount++] = index;
    }
    if (imageIndicesCount == 0)
      return Const.EMPTY_INTS;
    if (imageIndicesCount < imageIndices.length) {
      int[] arr = new int[imageIndicesCount];
      System.arraycopy(imageIndices, 0, arr, 0, imageIndicesCount);
      imageIndices = arr;
    }
    Arrays.sort(imageIndices);
    return imageIndices;
  }

  void resync(boolean produceEvents) {
    List<ATreeNode<T>> filteredSourceNodes = Collections15.arrayList();
    ATreeNode<T> sn = mySourceNode;
    if (sn == null)
      return;
    int sourceCount = sn.getChildCount();
    for (int i = 0; i < sourceCount; i++) {
      ATreeNode<T> sourceChild = sn.getChildAt(i);
      if (myFilter.accept(sourceChild))
        filteredSourceNodes.add(sourceChild);
    }
    List<TreeFilterNode<T>> current = myChildren != null ? myChildren : Collections15.<TreeFilterNode<T>>emptyList();
    ListPatch<TreeFilterNode<T>, ATreeNode<T>> patch = ListPatch.create(current, filteredSourceNodes);
    patch.setPolicy(ListPatchPolicy.SINGLE_UNREPEATING_ITEMS);
    patch.setEquality(new Condition2<TreeFilterNode<T>, ATreeNode<T>>() {
      public boolean isAccepted(TreeFilterNode<T> value1, ATreeNode<T> value2) {
        return value1.getSourceNode().equals(value2);
      }
    });
    List<ListPatchStep> steps = patch.generate();
    for (ListPatchStep step : steps) {
      ListPatchStep.Action action = step.getAction();
      int i = step.getSubjectIndex();
      assert i >= 0 && i <= getChildCount();
      if (action == ListPatchStep.Action.REMOVE) {
        assert i < getChildCount();
        TreeFilterNode removed = removeChild(i);
        assert removed != null;
        if (produceEvents)
          myFilter.notifyModelNodesWereRemoved(this, new int[] {i}, new Object[] {removed});
      } else if (action == ListPatchStep.Action.ADD) {
        int j = step.getGoalIndex();
        assert i >= 0 && i < filteredSourceNodes.size();
        ATreeNode<T> sourceNode = filteredSourceNodes.get(j);
        addChild(sourceNode, i);
        if (produceEvents)
          myFilter.notifyModelNodesWereInserted(this, new int[] {i});
      } else {
        assert false : step;
      }
    }
    if (myChildren != null) {
      for (TreeFilterNode child : myChildren) {
        child.resync(produceEvents);
      }
    }
  }

  private TreeFilterNode removeChild(int index) {
    List<TreeFilterNode<T>> children = myChildren;
    if (children == null)
      return null;
    TreeFilterNode<T> removed = children.remove(index);
    assert removed != null;
    assert removed.getParentIndex() == index;
    int size = children.size();
    for (int i = index; i < size; i++) {
      TreeFilterNode following = children.get(i);
      assert following.getParentIndex() == i + 1;
      following.setParentIndex(i);
    }
    TreeFilterNode removedFromMap = myFilter.removeMapping(removed.getSourceNode());
    assert removedFromMap == removed;
    removed.myParent = null;
    removed.myParentIndex = -1;
    return removed;
  }

  private TreeFilterNode addChild(ATreeNode<T> sourceNode, int place) {
    List<TreeFilterNode<T>> children = myChildren;
    if (children == null) {
      children = Collections15.arrayList();
      myChildren = children;
    }
    assert place >= 0 : place;
    assert place <= children.size();
    if (place < 0 || place > children.size()) {
      Log.warn("bad place, not adding - " + sourceNode + "; " + place);
      return null;
    }
    TreeFilterNode<T> imageNode;
    assert myReuseAddedNodeHint == null || myReuseAddedNodeHint.getSourceNode() == sourceNode;
    if (myReuseAddedNodeHint != null && myReuseAddedNodeHint.getSourceNode() == sourceNode) {
      myReuseAddedNodeHint.myParent = this;
      myReuseAddedNodeHint.myParentIndex = place;
      imageNode = myReuseAddedNodeHint;
      myReuseAddedNodeHint = null;
    } else {
      imageNode = new TreeFilterNode<T>(myFilter, this, place, sourceNode);
    }
    children.add(place, imageNode);
    int childCount = children.size();
    for (int i = place + 1; i < childCount; i++) {
      TreeFilterNode following = children.get(i);
      assert following.getParentIndex() == i - 1;
      following.setParentIndex(i);
    }
    myFilter.putMapping(sourceNode, imageNode);
    imageNode.resync(false);
    return imageNode;
  }

  void structureChanged(Object[] path) {
    ATreeNode<T> node = path.length == 0 ? null : (ATreeNode<T>) path[path.length - 1];
    if (!Util.equals(mySourceNode, node)) {
      assert this == myFilter.getFilteredRoot() : this;
      mySourceNode = node;
    }
    resync(false);
    myFilter.notifyModelNodeStructureChanged(this);
  }

  private TreeModelFilter getFilter() {
    return myFilter;
  }

  private int getParentIndex() {
    return myParentIndex;
  }

  private void setParentIndex(int index) {
    myParentIndex = index;
  }

  public String toString() {
    return "F{" + mySourceNode + "}";
  }
}
