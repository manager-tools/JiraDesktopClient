package com.almworks.api.application.qb;

import com.almworks.api.application.NameResolver;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.RemoveableModifiable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.FormulaWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConstraintDescriptorStub extends AbstractConstraintDescriptor implements CanvasRenderable{
  private final ConstraintType myType;
  protected final String myId;

  public ConstraintDescriptorStub(String id, ConstraintType type) {
    myType = type;
    myId = id;
  }
  public ConstraintEditor createEditor(ConstraintEditorNodeImpl node) {
    return myType.createEditor(node);
  }

  public void writeFormula(FormulaWriter writer, PropertyMap data) {
    myType.writeFormula(writer, getId(), data);
  }

  public ConstraintType getType() {
    return myType;
  }

  public String getDisplayName() {
    return myId;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  public CanvasRenderable getPresentation() {
    return this;
  }

  public void renderOn(Canvas canvas, CellState state) {
    canvas.appendText(myId);
  }

  @NotNull
  public ConstraintDescriptor resolve(NameResolver resolver, @Nullable ItemHypercube cube, PropertyMap data) {
    ConstraintDescriptor descriptor = (cube != null) ? resolver.getConditionDescriptor(myId, cube) : null;
    return descriptor != null ? descriptor.resolve(resolver, cube, data) : this;
  }

  public RemoveableModifiable getModifiable() {
    return Modifiable.NEVER;
  }

  @Nullable
  public BoolExpr<DP> createFilter(PropertyMap data, ItemHypercube hypercube) {
    return null;
  }

  @Nullable
  public Constraint createConstraint(PropertyMap data, ItemHypercube cube) {
    return null;
  }

  public boolean isSameData(PropertyMap data1, PropertyMap data2) {
    return false;
  }
}
