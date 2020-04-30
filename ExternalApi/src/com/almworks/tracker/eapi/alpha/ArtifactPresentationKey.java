package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.TypedKey;

import java.util.Comparator;

/**
 * A key to get a representation of an Artifact from ArtifactInfo.
 * A key may be treated as a column in artifacts table.
 *
 * @see ArtifactInfo
 */
public class ArtifactPresentationKey<T> extends TypedKey<T> {
  private final Comparator<T> myComparator;
  private final Class<T> myValueClass;
  private final boolean myCellPresentable;

  ArtifactPresentationKey(String name, Class<T> valueClass, Comparator<T> comparator, boolean cellPresentable) {
    super(name);
    myValueClass = valueClass;
    myComparator = comparator;
    myCellPresentable = cellPresentable;
  }

  /**
   * Returns comparator for sorting artifacts.
   */
  public Comparator<T> getComparator() {
    return myComparator;
  }

  /**
   * Returns true if this key may be presented in a cell (table, tree, list).
   */
  public boolean isCellPresentable() {
    return myCellPresentable;
  }

  public Class<T> getValueClass() {
    return myValueClass;
  }
}
