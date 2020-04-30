package com.almworks.api.application.tree;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.util.Pair;
import com.almworks.util.components.EditableText;

public interface DistributionQueryNode extends QueryNode {
  /**
   * @param pinned if true, query is a part of distribution; if not - distribution has
   * changed and this query is due to be deleted.
   */
  void setPinned(boolean pinned);

  EditableText getPresentation();

  Pair<ConstraintDescriptor, ItemKey> getAttributeValue();

  boolean isPinned();

  void updateName();
}
