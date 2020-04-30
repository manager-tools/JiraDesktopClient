package com.almworks.api.application.qb;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.UnresolvedNameException;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.text.parser.WritableFormula;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface FilterNode extends WritableFormula {
  FilterNode ALL_ITEMS = AllItemsFilterNode.INSTANCE;

  /**
   * @return editor node to be shown in the query tree
   */
  @NotNull
  EditorNode createEditorNode(@NotNull EditorContext context);

  /**
   * creates database filter to be used to search for items
   */
  @Nullable("when filter node or its child nodes are not valid")
  BoolExpr<DP> createFilter(ItemHypercube hypercube) throws UnresolvedNameException;

  /**
   * creates QB-independent constraint tree
   * @param cube
   */
  @Nullable("when filter node or its child nodes are not valid")
  Constraint createConstraint(ItemHypercube cube);

  boolean isSame(@Nullable FilterNode filterNode);

  /**
   * normalizes names used in this filter node and child nodes
   */
  void normalizeNames(@NotNull NameResolver resolver, ItemHypercube cube);

  FilterNode createCopy();

  @Nullable
  ConstraintType getType();

  /**
   * Null means the filter doesn't filter anything. Exception means that filter is there but the name cannot be given.
   */
  @Nullable
  String getSuggestedName(Map<TypedKey<?>, ?> hints) throws CannotSuggestNameException;
}
