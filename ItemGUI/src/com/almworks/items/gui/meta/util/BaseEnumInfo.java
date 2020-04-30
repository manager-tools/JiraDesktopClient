package com.almworks.items.gui.meta.util;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.schema.applicability.Applicabilities;
import com.almworks.items.gui.meta.schema.constraints.Descriptors;
import com.almworks.items.gui.meta.schema.dnd.DnDChange;
import com.almworks.items.gui.meta.schema.export.ModelKeyValueExport;
import com.almworks.items.gui.meta.schema.export.RenderValueExport;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeys;
import com.almworks.items.gui.meta.schema.renderers.ItemRenderers;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseEnumInfo<C extends BaseEnumInfo<C>> extends BaseFieldInfo<C> {
  private EnumTypeBuilder myEnumTypeBuilder;
  private DBStaticObject myEnumType;
  private ScalarSequence myConstraintKind = Descriptors.SEQUENCE_ENUM_CONSTRAINT;
  private ItemDownloadStage myRendererMinStage = ItemDownloadStage.QUICK;
  private String myEditorVariantsConfig;
  private ScalarSequence myDnDApplicability;
  private ScalarSequence myExportElementRenderer = ItemRenderers.defaultCanvas(0, "");

  public C setEnumType(DBStaticObject enumType) {
    LogHelper.assertError(myEnumTypeBuilder == null, "Already built");
    myEnumType = enumType;
    return self();
  }

  public C setConstraintKind(ScalarSequence kind) {
    myConstraintKind = kind;
    return self();
  }

  public C setNullableEnum(String id, String displayName) {
    LogHelper.assertError(myConstraintKind == Descriptors.SEQUENCE_ENUM_CONSTRAINT, "Already has not default kind", myConstraintKind);
    myConstraintKind = Descriptors.nullableEnum(id, displayName);
    return self();
  }

  public EnumTypeBuilder buildType() {
    LogHelper.assertError(myEnumType == null, "Already set");
    if (myEnumTypeBuilder == null) myEnumTypeBuilder = new EnumTypeBuilder();
    return myEnumTypeBuilder;
  }

  public C setExportElementRenderer(@Nullable ScalarSequence exportElementRenderer) {
    myExportElementRenderer = exportElementRenderer;
    return self();
  }

  protected DBStaticObject getEnumType() {
    if (myEnumType != null) return myEnumType;
    else if (myEnumTypeBuilder != null) return myEnumTypeBuilder.create();
    LogHelper.error("Missing enum type", getDisplayName());
    return null;
  }

  @Override
  public DBStaticObject createDescriptor() {
    return Descriptors.create(getOwner(), getAttribute(), getDisplayName(), getConstraintId(), getEnumType(), myConstraintKind, getConstraintIcon());
  }

  public abstract DBAttribute<?> getAttribute();

  protected ScalarSequence createKeyDataLoader() {
    return ModelKeys.enumAttribute(getAttribute(), getEnumType());
  }

  @Override
  public boolean hasExport() {
    return myExportElementRenderer != null;
  }

  protected ItemDownloadStage getRendererMinStage() {
    return myRendererMinStage;
  }

  protected ScalarSequence getExportElementRenderer() {
    return myExportElementRenderer;
  }

  public C setRendererMinStage(ItemDownloadStage rendererMinStage) {
    myRendererMinStage = rendererMinStage;
    return self();
  }

  public C setDefaultDnD(String variantsConfig, ScalarSequence ... applicability) {
    myEditorVariantsConfig = Util.NN(variantsConfig);
    if (applicability.length > 0) myDnDApplicability = Applicabilities.satisfyAll(applicability);
    return self();
  }

  @Override
  public boolean hasDnDChange() {
    return myEditorVariantsConfig != null;
  }

  @Override
  @NotNull
  public DBStaticObject createDnDChange() {
    if (myEditorVariantsConfig == null) {
      LogHelper.error("Missing DnD policy", getAttribute());
      //noinspection ConstantConditions
      return null;
    }
    return DnDChange.create(getOwner(), getAttribute(), myEditorVariantsConfig, getEnumType(), createModelKey(), createDescriptor(), myDnDApplicability);
  }

  @Override
  protected ScalarSequence createExportPolicy(DBStaticObject modelKey) {
    ScalarSequence renderer = createExportRenderer();
    if (renderer == null) return null;
    return ModelKeyValueExport.create(createModelKey(), ExportValueType.STRING, RenderValueExport.create(renderer));
  }

  protected abstract ScalarSequence createExportRenderer();

  @Override
  public DBStaticObject createReorder() {
    return null;
  }
}
