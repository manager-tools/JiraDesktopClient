package com.almworks.items.gui.meta.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.ItemKeySubloader;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.schema.enums.EnumType;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.util.properties.TypedKeyWithEquality;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class EnumTypeBuilder {
  public static final ScalarSequence ICON_BY_URL = EnumType.iconByUrl(FieldInfo.ICON_URL);

  private DBItemType myType;
  private ScalarSequence myRenderer;
  private DBAttribute<?> myUniqueKey;
  private ScalarSequence myNarrower;
  private ScalarSequence myOrder;
  private final List<ScalarSequence> mySubloaders = Collections15.arrayList();
  private boolean mySearchSubstring = false;
  @Nullable
  private ScalarSequence myEditorRenderer;
  private ScalarSequence myIconLoader = ICON_BY_URL;

  public EnumTypeBuilder renderFirstNotNull(DBAttribute<?> ... attributes) {
    myRenderer = EnumType.rendererFirstNotNull(attributes);
    return this;
  }

  public EnumTypeBuilder renderPathFromRoot(DBAttribute<Long> parentAttr, String pathSep, DBAttribute<?>... attributes) {
    myRenderer = EnumType.rendererPathFromRoot(parentAttr, pathSep, attributes);
    return this;
  }

  public EnumTypeBuilder setRenderer(ScalarSequence renderer) {
    myRenderer = renderer;
    return this;
  }

  public EnumTypeBuilder setUniqueKey(DBAttribute<?> uniqueKey) {
    myUniqueKey = uniqueKey;
    return this;
  }

  public EnumTypeBuilder setIconLoader(ScalarSequence loader) {
    myIconLoader = loader;
    return this;
  }

  public EnumTypeBuilder narrowByAttribute(DBAttribute<Long> issueAttribute, DBAttribute<?> enumRestriction) {
    return setNarrower(EnumType.narrowByAttribute(issueAttribute, enumRestriction));
  }

  /**
   * Serialized {@link com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower}.
   * @param narrower [narrowerLoaderFeature, params] sequence representing {@link com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower narrower}
   * @return this builder
   * @see com.almworks.items.gui.meta.commons.SerializableFeature
   * @see com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower
   * @see com.almworks.items.gui.meta.commons.FeatureRegistry#register(com.almworks.items.sync.util.identity.DBIdentity, Object)
   * @see com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower
   */
  public EnumTypeBuilder setNarrower(ScalarSequence narrower) {
    myNarrower = narrower;
    return this;
  }

  public EnumTypeBuilder orderByNumber(DBAttribute<? extends Number> enumOrder, boolean direct) {
    myOrder = EnumType.orderByNumber(enumOrder, direct);
    return this;
  }

  public EnumTypeBuilder setOrder(ScalarSequence sequence) {
    myOrder = sequence;
    return this;
  }

  public EnumTypeBuilder setSearchSubstring(boolean searchSubstring) {
    mySearchSubstring = searchSubstring;
    return this;
  }

  public EnumTypeBuilder addAttributeSubloaders(DBAttribute<?>... attributes) {
    for(final DBAttribute<?> attribute : attributes) {
      mySubloaders.add(ItemKeySubloader.Attribute.sequence(attribute));
    }
    return this;
  }

  public EnumTypeBuilder addParentSubloader(DBAttribute<Long> attribute, TypedKeyWithEquality<LoadedItemKey> key) {
    mySubloaders.add(ItemKeySubloader.Parent.sequence(attribute, key));
    return this;
  }

  public EnumTypeBuilder addSubtreeSubloader(DBAttribute<Long> attribute, TypedKeyWithEquality<Set<Long>> key) {
    mySubloaders.add(ItemKeySubloader.Subtree.sequence(attribute, key));
    return this;
  }

  public EnumTypeBuilder setType(DBItemType type) {
    myType = type;
    return this;
  }

  public EnumTypeBuilder setEditorRenderer(@Nullable ScalarSequence editorRenderer) {
    myEditorRenderer = editorRenderer;
    return this;
  }

  public DBStaticObject create() {
    DBIdentity identity = EnumType.simpleId(myType);
    DBStaticObject.Builder enumType =
      new DBStaticObject.Builder()
        .putSequence(EnumType.ItemKeys.RENDERER, getRenderer())
        .put(EnumType.SEARCH_SUBSTRING, mySearchSubstring)
        .putReference(EnumType.ItemKeys.UNIQUE_KEY, myUniqueKey)
        .putSequence(EnumType.ItemKeys.ICON_LOADER, myIconLoader)
        .putSequence(EnumType.NARROWER, getNarrower())
        .putSequence(EnumType.ItemKeys.ORDER, getOrder());
    ScalarSequence subloaders = getSubloaders();
    if (subloaders != null) enumType.putSequence(EnumType.ItemKeys.SUBLOADERS, subloaders);
    if (myEditorRenderer != null) enumType.putSequence(EnumType.EDITOR_RENDERER.getAttribute(), myEditorRenderer);
    return enumType.create(identity);
  }

  private ScalarSequence getOrder() {
    return myOrder != null ? myOrder : EnumType.ItemKeys.SEQUENCE_BY_DISPLAY_NAME;
  }

  private ScalarSequence getNarrower() {
    return myNarrower != null ? myNarrower : EnumType.NARROW_DEFAULT;
  }

  private ScalarSequence getRenderer() {
    return myRenderer != null ? myRenderer : EnumType.rendererFirstNotNull(myUniqueKey);
  }

  @Nullable
  private ScalarSequence getSubloaders() {
    if (mySubloaders.isEmpty()) return null;
    final ScalarSequence.Builder builder = new ScalarSequence.Builder();
    builder.append(mySubloaders.size());
    for(final ScalarSequence s : mySubloaders) {
      builder.appendSubsequence(s);
    }
    return builder.create();
  }
}
