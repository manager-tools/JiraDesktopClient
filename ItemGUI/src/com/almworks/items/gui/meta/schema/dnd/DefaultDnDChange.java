package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.schema.applicability.Applicability;
import com.almworks.items.gui.meta.schema.applicability.SatisfyAll;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

abstract class DefaultDnDChange<M> extends DnDChange {
  private final DnDVariants myVariants;
  private final long myModelKey;
  private final long myConstraint;
  private final Applicability myApplicability;

  public DefaultDnDChange(DnDVariants variants, long modelKey, long constraint, Applicability applicability) {
    myVariants = variants;
    myModelKey = modelKey;
    myConstraint = constraint;
    myApplicability = applicability;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    DefaultDnDChange other = Util.castNullable(DefaultDnDChange.class, obj);
    return other != null && Util.equals(getAttribute(), other.getAttribute()) && Util.equals(myVariants, other.myVariants) && myConstraint == other.myConstraint &&
      myModelKey == other.myModelKey;
  }

  @Override
  public int hashCode() {
    return (int)myModelKey ^ DefaultDnDChange.class.hashCode();
  }

  public DnDVariants getVariants() {
    return myVariants;
  }

  @Override
  public void prepare(DnDApplication application) {
    TargetValues target = application.getTargetValues(getAttribute());
    ModelKey<M> modelKey = getModelKey(application, myModelKey);
    BaseEnumConstraintDescriptor constraint = application.getEnumConstraint(myConstraint);
    if (modelKey == null || constraint == null) {
      LogHelper.error("Missing data", modelKey, constraint);
      return;
    }
    if (!isApplicableToAny(application.getItems())) {
      application.getProblems().addBasic("Cannot change " + constraint.getDisplayName());
      return;
    }
    prepare(application, target, modelKey, constraint);
  }

  private boolean isApplicableToAny(Iterable<? extends ItemWrapper> wrappers) {
    if (myApplicability == SatisfyAll.SATISFY_ANY) return true;
    for (ItemWrapper wrapper : wrappers) if (myApplicability.isApplicable(wrapper)) return true;
    return false;
  }
  
  public boolean isApplicableToAnyItem(Iterable<? extends ItemVersion> items) {
    for (ItemVersion item : items) if (myApplicability.isApplicable(item)) return true;
    return false;
  }

  public boolean isApplicableTo(EditItemModel model) {
    return myApplicability.isApplicable(model);
  }

  public boolean isApplicableTo(ItemVersion item) {
    return myApplicability.isApplicable(item);
  }

  protected abstract DBAttribute<?> getAttribute();

  protected abstract LoadedModelKey<M> getModelKey(DnDApplication application, long modelKey);

  protected abstract void prepare(DnDApplication application, TargetValues target, ModelKey<M> modelKey, BaseEnumConstraintDescriptor constraint);

  protected ItemKey getMissingKey(BaseEnumConstraintDescriptor constraint) {
    ItemKey empty = constraint.getMissingItem();
    if (empty != null && empty.getItem() > 0) empty = null;
    return empty;
  }

  protected boolean checkSinglePositive(DnDApplication application, String labelText, long single, @Nullable ItemKey empty) {
    if (single != 0 || empty != null) return true;
    application.getProblems().addNotSupported("Cannot clear value of '" + labelText + "'");
    LogHelper.error("Cannot set no value", getAttribute());
    return false;
  }
}
