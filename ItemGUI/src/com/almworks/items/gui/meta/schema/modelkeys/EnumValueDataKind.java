package com.almworks.items.gui.meta.schema.modelkeys;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.DataAccessor;
import com.almworks.api.application.util.DataIO;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.BadUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

class EnumValueDataKind extends ModelKeyLoader {
  static final SerializableFeature<ModelKeyLoader> FEATURE =
    new SerializableFeature<ModelKeyLoader>() {
      @Override
      public ModelKeyLoader restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        long attrItem = stream.nextLong();
        long enumTypeItem = stream.nextLong();
        if (!stream.isSuccessfullyAtEnd()) return null;
        DBAttribute<?> attribute = BadUtil.getAttribute(reader, attrItem);
        if (attribute.getScalarClass() != Long.class) {
          LogHelper.error("Wrong attribute", attribute, stream);
          return null;
        }
        if (enumTypeItem <= 0) {
          LogHelper.error("Wrong enum type", enumTypeItem, attribute, stream);
          return null;
        }
        EnumTypesCollector enumTypes = EnumTypesCollector.getInstance(reader);
        return new EnumValueDataKind(attribute, enumTypeItem, enumTypes);
      }

      @Override
      public Class<ModelKeyLoader> getValueClass() {
        return ModelKeyLoader.class;
      }
    };

  private static final TypedKey<DBAttribute<?>> ATTRIBUTE = TypedKey.create("attribute");
  private static final TypedKey<Long> ENUM_TYPE = TypedKey.create("enumType");
  private static final TypedKey<?>[] KEYS = {ATTRIBUTE, ENUM_TYPE};
  private final EnumTypesCollector myEnumTypes;

  public EnumValueDataKind(DBAttribute<?> attribute, long enumTypeItem, EnumTypesCollector enumTypes) {
    super(new DataHolder(KEYS).setValue(ATTRIBUTE, attribute).setValue(ENUM_TYPE, enumTypeItem));
    myEnumTypes = enumTypes;
  }

  @SuppressWarnings( {"unchecked"})
  @Override
  public boolean loadKey(LoadedModelKey.Builder<?> b, GuiFeaturesManager guiFeatures) {
    DBAttribute<?> attribute = getData(ATTRIBUTE);
    Long enumTypeItem = getData(ENUM_TYPE);
    EnumTypesCollector.Loaded enumType = myEnumTypes.getType(enumTypeItem);
    if (attribute == null || enumType == null) return false;
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    switch (composition) {
    case SCALAR:
      DBAttribute<Long> itemAttr = BadUtil.castScalar(Long.class, attribute);
      if (itemAttr == null) break;
      buildSingleEnum(enumType, itemAttr, b.setDataClass(ItemKey.class));
      return true;
    case SET:
    case LIST:
      DBAttribute<? extends Collection<Long>> collectionAttr = BadUtil.castCollectionAttribute(Long.class, attribute);
      if (collectionAttr == null) break;
      buildMultiEnum(enumType, collectionAttr, b.setListDataClass(ItemKey.class));
      return true;
    }
    LogHelper.error("Unknown attribute composition/class", composition, attribute);
    return false;
  }

  private void buildSingleEnum(EnumTypesCollector.Loaded enumType, final DBAttribute<Long> attribute,
    LoadedModelKey.Builder<ItemKey> builder) {
    builder.setAccessor(new DataAccessor.SimpleDataAccessor<ItemKey>(builder.getName()));
    builder.setIO(new SingleEnumIO(attribute, enumType));
  }

  private void buildMultiEnum(EnumTypesCollector.Loaded enumType,
    @NotNull DBAttribute<? extends Collection<Long>> attribute, LoadedModelKey.Builder<List<ItemKey>> builder)
  {
    builder.setAccessor(new DataAccessor.SimpleDataAccessor<List<ItemKey>>(builder.getName()));
    builder.setIO(new MultiEnumIO(attribute, enumType));
  }

  private static class SingleEnumIO implements DataIO<ItemKey> {
    private final DBAttribute<Long> myAttribute;
    private final EnumTypesCollector.Loaded myEnumType;

    public SingleEnumIO(DBAttribute<Long> attribute, EnumTypesCollector.Loaded enumType) {
      myAttribute = attribute;
      myEnumType = enumType;
    }

    @Override
    public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values,
      ModelKey<ItemKey> modelKey)
    {
      modelKey.setValue(values, myEnumType.getResolvedItem(itemServices.getItemKeyCache(), itemVersion, myAttribute));
    }

    @Override
    public String toString() {
      return String.format("SingleEnuIO[%s : %s", myAttribute, myEnumType);
    }
  }
}
