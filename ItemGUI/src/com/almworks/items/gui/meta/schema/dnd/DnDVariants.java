package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.enums.ConnectionVariants;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.FilteringEnumVariantsSource;
import com.almworks.items.gui.meta.schema.enums.EnumType;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.Renderers;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DnDVariants {
  private final TypedKey<EnumVariantsSource> myVariants;
  private final TypedKey<CanvasRenderer<ItemKey>> myRenderer;
  private final long myEnumType;
  private final String myConfig;
  private final ConnectionVariants mySource;

  public DnDVariants(long enumType, String config) {
    myEnumType = enumType;
    myConfig = config;
    myVariants = TypedKey.create(config + "/variants");
    myRenderer = TypedKey.create(config + "/renderer");
    mySource = ConnectionVariants.createDynamic(myEnumType, myConfig);
  }

  @Nullable
  public EnumVariantsSource prepare(VersionSource source, EditModelState model) {
    ConnectionVariants variants = mySource;
    if (variants == null) return null;
    variants.prepare(source, model);
    model.putHint(myVariants, variants);
    CanvasRenderer<ItemKey> renderer = getEnumRenderer(source);
    if (renderer != null) model.putHint(myRenderer, renderer);
    return variants;
  }

  protected CanvasRenderer<ItemKey> getEnumRenderer(VersionSource source) {
    return EnumType.EDITOR_RENDERER.getValue(source.forItem(myEnumType));
  }

  @Nullable
  public EnumVariantsSource getVariants(EditModelState model) {
    return model.getValue(myVariants);
  }

  @NotNull
  public CanvasRenderer<ItemKey> getRenderer(EditModelState model) {
    CanvasRenderer<ItemKey> renderer = model.getValue(myRenderer);
    return renderer != null ? renderer : Renderers.<ItemKey>defaultCanvasRenderer();
  }

  public EnumVariantsSource filterVariants(EditModelState model, Condition<ItemKey> filter) {
    EnumVariantsSource variants = getVariants(model);
    if (variants == null) return null;
    return new FilteringEnumVariantsSource(variants, filter);
  }

  public long getEnumType() {
    return myEnumType;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    DnDVariants other = Util.castNullable(DnDVariants.class, obj);
    return other != null && myEnumType == other.myEnumType && Util.equals(myConfig, other.myConfig);
  }

  @Override
  public int hashCode() {
    return (int)myEnumType ^ DnDVariants.class.hashCode();
  }
}
