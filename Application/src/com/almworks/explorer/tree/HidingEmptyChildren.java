package com.almworks.explorer.tree;

import com.almworks.api.application.tree.DistributionGroupNode;
import com.almworks.api.application.tree.GenericNode;

import java.util.Iterator;

class HidingEmptyChildren {
  private final GenericNode myNode;
  private GenericNode myCachedCandidate = null;

  HidingEmptyChildren(GenericNode node) {
    myNode = node;
  }

  public boolean hasHiddenOrNotCounted() {
    if(!Boolean.TRUE.equals(myNode.getHideEmptyChildren())) {
      return false;
    }
    GenericNode empty = findEmptyOrUnknown(myNode.getChildrenIterator());
    return empty != null;
  }

  private GenericNode findEmptyOrUnknown(Iterator<? extends GenericNode> it) {
    GenericNode candidate = getCachedCandidate();
    if (candidate != null) return candidate;
    candidate = doFindEmptyOrUnknown(it);
    myCachedCandidate = candidate;
    return candidate;
  }

  private GenericNode getCachedCandidate() {
    GenericNode prevEmpty = myCachedCandidate;
    if (prevEmpty == null) return null;
    GenericNode parent = prevEmpty.getParent();
    if (parent == null || prevEmpty.getCusionedPreviewCount() > 0) {
      myCachedCandidate = null;
      return null;
    }
    if (parent == myNode) return prevEmpty;
    if (!(parent instanceof DistributionGroupNode)) {
      myCachedCandidate = null;
      return null;
    }
    parent = parent.getParent();
    if (parent != myNode) {
      myCachedCandidate = null;
      return null;
    }
    return prevEmpty;
  }

  private static GenericNode doFindEmptyOrUnknown(Iterator<? extends GenericNode> it) {
    while (it.hasNext()) {
      GenericNode node = it.next();
      if (node instanceof DistributionGroupNode) {
        GenericNode empty = doFindEmptyOrUnknown(node.getChildrenIterator());
        if (empty != null) return empty;
      } else {
        final int count = node.getCusionedPreviewCount();
        if (count <= 0) return node;
      }
    }
    return null;
  }

  private static boolean hasUnknown(Iterator<? extends GenericNode> it) {
    while (it.hasNext()) {
      GenericNode node = it.next();
      if (node instanceof DistributionGroupNode) {
        if (hasUnknown(node.getChildrenIterator())) return true;
      } else {
        final int count = node.getCusionedPreviewCount();
        if (count < 0) return true;
      }
    }
    return false;
  }
}
