package com.almworks.items.gui.meta.schema.modelkeys;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.StateIcon;
import com.almworks.api.application.StateIconHelper;
import com.almworks.api.application.util.DataIO;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.Break;
import com.almworks.util.Trio;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import com.almworks.util.images.IconHandle;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

class BooleanDataKindWithStateIcon extends ModelKeyLoader {
  static final SerializableFeature<ModelKeyLoader> FEATURE = new SerializableFeature<ModelKeyLoader>() {
    @Override
    public ModelKeyLoader restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      try {
        DBAttribute<Boolean> attr = ModelKeyLoader.readAttribute(stream, reader, Boolean.class);
        String iconImageName = stream.nextUTF8();
        int stateIconPriority = stream.nextInt();
        String tooltipPattern = stream.nextUTF8();
        if (!stream.isSuccessfullyAtEnd()) return null;
        return new BooleanDataKindWithStateIcon(attr, iconImageName, stateIconPriority, tooltipPattern);
      } catch (Break err) {
        return null;
      }
    }

    @Override
    public Class<ModelKeyLoader> getValueClass() {
      return ModelKeyLoader.class;
    }
  };

  private static final TypedKey<Trio<String, Integer, String>> ICON = TypedKey.create("icon");
  private static final TypedKey<DBAttribute<Boolean>> ATTRIBUTE = TypedKey.create("attribute");
  private static final TypedKey<?>[] KEYS = {ICON, ATTRIBUTE};
  // Not taking part in equals()
  private final StateIcon myIconCached;

  public BooleanDataKindWithStateIcon(DBAttribute<Boolean> attribute, String iconImageName, int stateIconPriority, String tooltipPattern) {
    super(new DataHolder(KEYS).setValue(ATTRIBUTE, attribute).setValue(ICON, Trio.create(iconImageName, stateIconPriority, tooltipPattern)));
    myIconCached = new StateIcon(IconHandle.forName(IconHandle.class, iconImageName).getIcon(), stateIconPriority, tooltipPattern);
  }

  @Override
  public boolean loadKey(LoadedModelKey.Builder<?> b, GuiFeaturesManager guiFeatures) {
    DBAttribute<Boolean> attribute = getData(ATTRIBUTE);
    LoadedModelKey.Builder<Boolean> builder = ModelKeyLoader.prepareSimpleBuilder(b, attribute);
    if (builder == null) return false;
    builder.setIO(new DataIO.SimpleIO<Boolean>(attribute) {
      @Override
      public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values, ModelKey<Boolean> key) {
        Boolean value = itemVersion.getValue(getAttribute());
        if (Boolean.TRUE.equals(value)) {
          StateIconHelper.addStateIcon(values, myIconCached);
        }
        setValue(value, values, key);
      }

      @Override
      public String toString() {
        return toString("BooleanDataKindWithStateIcon.IO");
      }
    });
    return true;
  }
}
