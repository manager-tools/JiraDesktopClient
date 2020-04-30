package com.almworks.api.application.qb;

import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.FormulaWriter;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Represents a type of leaf constraints in QB tree; currently only backed by an attribute.
 * Used to render constraint name (CanvasRenderable)
 *
 * @author : Dyoma
 */
public interface ConstraintType {
  ConstraintEditor createEditor(ConstraintEditorNodeImpl node);

  boolean equals(Object other);

  int hashCode();

  void writeFormula(FormulaWriter writer, String conditionId, PropertyMap data);

  PropertyMap getEditorData(PropertyMap data);

  @Nullable
  String suggestName(String descriptorName, PropertyMap data, Map<TypedKey<?>, ?> hints) throws CannotSuggestNameException;
}
