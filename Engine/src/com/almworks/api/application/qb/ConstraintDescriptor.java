package com.almworks.api.application.qb;

import com.almworks.api.application.NameResolver;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.RemoveableModifiable;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.FormulaWriter;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * @author dyoma
 */
public interface ConstraintDescriptor {
  Comparator<ConstraintDescriptor> ORDER_BY_DISPLAY_NAME = new MyComparator();
  Convertor<ConstraintDescriptor, String> TO_ID = new ToIdConvertor();

  boolean equals(Object obj);

  int hashCode();

  String getDisplayName();

  @NotNull
  String getId();

  /**
   * Delegates to {@link com.almworks.api.application.qb.ConstraintType}
   */
  ConstraintEditor createEditor(ConstraintEditorNodeImpl node);

  /**
   * Delegates to {@link com.almworks.api.application.qb.ConstraintType}
   */
  void writeFormula(FormulaWriter writer, PropertyMap data);

  ConstraintType getType();

  /**
   * Resolves itself and (optionally) resolves data. If no data is needed to be resolved set data to null.
   * @param resolver
   * @param cube
   * @param data (optional) data to be resolved, or null if data resolution isn't needed
   * @return resolved version of itself
   */
  @NotNull
  ConstraintDescriptor resolve(NameResolver resolver, @Nullable ItemHypercube cube, @Nullable PropertyMap data);

  /**
   * Gets updated when the result of createFilter() and createConstraint() may change.
   */
  RemoveableModifiable getModifiable();

  @Nullable
  @ThreadAWT
  public BoolExpr<DP> createFilter(PropertyMap data, ItemHypercube hypercube);

  @Nullable
  Constraint createConstraint(PropertyMap data, ItemHypercube cube);

  boolean isSameData(PropertyMap data1, PropertyMap data2);

  CanvasRenderable getPresentation();

  @CanBlock
  void waitForInitialization() throws InterruptedException;

  @Nullable
  <T> T cast(Class<T> descriptorClass);

  public static class MyComparator implements Comparator<ConstraintDescriptor> {
    public int compare(ConstraintDescriptor o1, ConstraintDescriptor o2) {
      if (o1 == null) return o2 == null ? 0 : 1;
      if (o2 == null) return -1;
      return String.CASE_INSENSITIVE_ORDER.compare(Util.NN(o1.getDisplayName()), Util.NN(o2.getDisplayName()));
    }
  }

  public static class ToIdConvertor extends Convertor<ConstraintDescriptor, String> {
    public String convert(ConstraintDescriptor value) {
      return value == null ? null : value.getId();
    }
  }
}
