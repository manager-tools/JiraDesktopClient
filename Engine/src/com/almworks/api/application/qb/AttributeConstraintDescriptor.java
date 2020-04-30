package com.almworks.api.application.qb;

import com.almworks.api.application.NameResolver;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.RemoveableModifiable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.FormulaWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public class AttributeConstraintDescriptor<V> extends AbstractConstraintDescriptor implements CanvasRenderable {
  private final AttributeConstraintType<V> myType;
  private final String myDisplayName;
  private final String myId;
  private final DBAttribute<V> myAttribute;
  private final RemoveableModifiable myModifiable;

  public AttributeConstraintDescriptor(AttributeConstraintType<V> type, String displayName, String id,
    DBAttribute<V> attribute, RemoveableModifiable modifiable)
  {
    myType = type;
    myDisplayName = displayName;
    myId = id;
    myAttribute = attribute;
    myModifiable = modifiable;
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  public ConstraintEditor createEditor(ConstraintEditorNodeImpl node) {
    return myType.createEditor(node);
  }

  public void writeFormula(FormulaWriter writer, PropertyMap data) {
    myType.writeFormula(writer, myId, data);
  }

  public ConstraintType getType() {
    return myType;
  }

  @NotNull
  public ConstraintDescriptor resolve(NameResolver resolver, ItemHypercube cube, PropertyMap data) {
    return this;
  }

  public CanvasRenderable getPresentation() {
    return this;
  }

  public void renderOn(Canvas canvas, CellState state) {
    canvas.setIcon(myType.getDescriptorIcon());
    canvas.appendText(myDisplayName);
  }

  public RemoveableModifiable getModifiable() {
    return myModifiable;
  }

  @Override
  @Nullable
  public BoolExpr<DP> createFilter(PropertyMap data, ItemHypercube hypercube) {
    return myType.createFilter(myAttribute, data);
  }

  @Nullable
  public Constraint createConstraint(PropertyMap data, ItemHypercube cube) {
    return myType.createConstraint(myAttribute, data);
  }

  public boolean isSameData(PropertyMap data1, PropertyMap data2) {
    return myType.isSameData(data1, data2);
  }

  public DBAttribute<V> getAttribute() {
    return myAttribute;
  }
}
