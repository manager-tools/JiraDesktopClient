package com.almworks.api.application.qb;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.UnresolvedNameException;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.FormulaWriter;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author dyoma
 */
public class ConstraintFilterNode implements FilterNode {
  private final ConstraintDescriptor myDescriptor;
  private final PropertyMap myData;

  public ConstraintFilterNode(@NotNull ConstraintDescriptor descriptor, PropertyMap data) {
    myDescriptor = descriptor;
    myData = data;
  }

  @NotNull
  public EditorNode createEditorNode(@NotNull EditorContext context) {
    return new ConstraintEditorNodeImpl(context, myDescriptor, myDescriptor.getType().getEditorData(myData));
  }

  @Nullable("when filter node or its child nodes are not valid")
  public BoolExpr<DP> createFilter(ItemHypercube hypercube) throws UnresolvedNameException {
    return myDescriptor.createFilter(myData, hypercube);
  }

  @Nullable("when filter node or its child nodes are not valid")
  public Constraint createConstraint(ItemHypercube cube) {
    return myDescriptor.createConstraint(myData, cube);
  }

  public boolean isSame(@Nullable FilterNode filterNode) {
    if (filterNode == null)
      return false;
    if (!(filterNode instanceof ConstraintFilterNode))
      return false;
    ConstraintFilterNode filterNode2 = (ConstraintFilterNode) filterNode;
    return myDescriptor.getId().equals(filterNode2.myDescriptor.getId()) &&
      myDescriptor.isSameData(myData, filterNode2.myData);
  }

  public void normalizeNames(@NotNull NameResolver resolver, ItemHypercube cube) {
    int size = myData.size();
    myDescriptor.resolve(resolver, cube, myData);
    assert myData.size() == size : "new:" + myData.size() + " old:" + size;
  }

  public FilterNode createCopy() {
    return new ConstraintFilterNode(myDescriptor, new PropertyMap(myData));
  }

  public void writeFormula(FormulaWriter writer) {
    myDescriptor.writeFormula(writer, myData);
  }

  public ConstraintType getType() {
    return myDescriptor.getType();
  }

  @Nullable
  public String getSuggestedName(Map<TypedKey<?>, ?> hints) throws CannotSuggestNameException {
    ConstraintType type = getType();
    String name = getDescriptor().getDisplayName();
    return type != null ? type.suggestName(name, myData, hints) : null;
  }

  public ConstraintDescriptor getDescriptor() {
    return myDescriptor;
  }

  @Nullable
  public <T> T getValue(TypedKey<T> key) {
    return myData.get(key);
  }

  public static FilterNode parsed(String conditionId, ConstraintType type, PropertyMap values) {
    return new ConstraintFilterNode(ConstraintDescriptorProxy.stub(conditionId, type), values);
  }
}
