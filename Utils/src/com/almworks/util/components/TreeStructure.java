package com.almworks.util.components;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author dyoma
 */
public interface TreeStructure<E, K, N extends TreeModelBridge<? extends E>> {
  K getNodeKey(E element);

  @Nullable
  K getNodeParentKey(E element);

  N createTreeNode(E element);

  interface MultiParent<E, K, N extends TreeModelBridge<? extends E>> extends TreeStructure<E, K, N> {
    Set<K> getNodeParentKeys(E element);
  }
}
