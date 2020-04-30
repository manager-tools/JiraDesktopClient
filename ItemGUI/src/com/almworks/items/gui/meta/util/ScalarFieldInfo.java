package com.almworks.items.gui.meta.util;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.schema.columns.ColumnRenderer;
import com.almworks.items.gui.meta.schema.constraints.Descriptors;
import com.almworks.items.gui.meta.schema.export.ModelKeyValueExport;
import com.almworks.items.gui.meta.schema.reorders.Reorders;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;

public class ScalarFieldInfo<T> extends BaseFieldInfo<ScalarFieldInfo<T>> {
  private final DBIdentity myKeyLoaderFeature;
  private final DBIdentity myFieldBehaviour;
  private ScalarSequence myCustomKeyLoader;
  private DBAttribute<T> myAttribute;
  private ScalarSequence myRenderer;
  private ScalarSequence myConstraint = Descriptors.SEQUENCE_SCALAR_CONSTRAINT;
  private ScalarSequence myExportPolicy;
  private ExportValueType myExportValueType;
  private boolean myAllowReorder = false;

  public ScalarFieldInfo(DBIdentity keyLoaderFeature, DBIdentity fieldBehaviour) {
    myKeyLoaderFeature = keyLoaderFeature;
    myFieldBehaviour = fieldBehaviour;
  }

  public ScalarFieldInfo<T> setAttribute(DBAttribute<T> attribute) {
    myAttribute = attribute;
    return this;
  }

  public ScalarFieldInfo<T> setCustomKeyLoader(ScalarSequence loaderSequence) {
    myCustomKeyLoader = loaderSequence;
    return this;
  }

  public ScalarFieldInfo<T> setSimpleRenderer(ItemDownloadStage minStage, int fontStyle, String nullText) {
    return setRenderer(ColumnRenderer.valueCanvasDefault(minStage, fontStyle, nullText));
  }

  public ScalarFieldInfo<T> setEmptyNullRenderer(ItemDownloadStage minStage) {
    return setSimpleRenderer(minStage, 0, "");
  }

  public ScalarFieldInfo<T> setRenderer(ScalarSequence renderer) {
    myRenderer = renderer;
    return this;
  }

  public ScalarFieldInfo<T> setConstraint(ScalarSequence constraint) {
    myConstraint = constraint;
    return this;
  }

  public ScalarFieldInfo<T> setModelKeyExport(ExportValueType type, ScalarSequence export) {
    if ((type == null) != (export == null)) throw new NullPointerException(type + " " + export);
    myExportValueType = type;
    myExportPolicy = export;
    return this;
  }

  public ScalarFieldInfo<T> allowReorder() {
    myAllowReorder = true;
    return this;
  }

  @Override
  public boolean hasExport() {
    return myExportPolicy != null && myExportValueType != null;
  }

  @Override
  protected ScalarSequence createExportPolicy(DBStaticObject modelKey) {
    if (myExportPolicy == null || myExportValueType == null) {
      LogHelper.error("No export", myExportPolicy, myExportValueType, this);
      return null;
    }
    return ModelKeyValueExport.create(modelKey, myExportValueType, myExportPolicy);
  }

  protected ScalarSequence createKeyDataLoader() {
    return Util.NN(myCustomKeyLoader, new ScalarSequence.Builder().append(myKeyLoaderFeature).append(myAttribute).create());
  }

  @Override
  public DBStaticObject createDescriptor() {
    return Descriptors.create(getOwner(), myAttribute, getDisplayName(), getConstraintId(), null, myConstraint, getConstraintIcon());
  }

  @Override
  public boolean hasDnDChange() {
    return false;
  }

  @Override
  public DBStaticObject createDnDChange() {
    return null;
  }

  @Override
  protected ScalarSequence createFieldBehaviour(DBStaticObject modelKey, String displayName, boolean hideEmptyLeftField, boolean isMultiline) {
    return new ScalarSequence.Builder()
      .append(myFieldBehaviour)
      .append(modelKey)
      .append(displayName)
      .append(hideEmptyLeftField)
      .append(isMultiline)
    .create();
  }

  @Override
  protected ScalarSequence createRenderer() {
    return myRenderer;
  }

  @Override
  public DBStaticObject createReorder() {
    if (!myAllowReorder) return null;
    return Reorders.create(getOwner(), getModelKeyId(), getDisplayName(), myAttribute, createModelKey(), createColumn(), getApplicability());
  }

  /**
   * Allows to use the instance as prototype.
   * @return new instance with all values set to same (as this instance)
   */
  public ScalarFieldInfo<T> copy() {
    ScalarFieldInfo<T> copy = new ScalarFieldInfo<T>(myKeyLoaderFeature, myFieldBehaviour);
    copyPrototypeTo(copy);
    return copy;
  }

  protected void copyPrototypeTo(ScalarFieldInfo<T> target) {
    target.myCustomKeyLoader = myCustomKeyLoader;
    target.myAttribute = myAttribute;
    target.myRenderer = myRenderer;
    target.myExportPolicy = myExportPolicy;
    target.myExportValueType = myExportValueType;
    target.myAllowReorder = myAllowReorder;
    target.myConstraint = myConstraint;
    super.copyPrototypeTo(target);
  }
}
