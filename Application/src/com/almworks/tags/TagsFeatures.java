package com.almworks.tags;

import com.almworks.api.application.*;
import com.almworks.api.application.util.DataAccessor;
import com.almworks.api.engine.Engine;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeyLoader;
import com.almworks.items.gui.meta.schema.modelkeys.MultiEnumIO;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.Terms;
import com.almworks.util.commons.Function;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TagsFeatures implements GuiFeaturesManager.Provider {
  public static final DBNamespace NS = Engine.NS.subModule("tags");
  /**
   * Sequence [iconPath:DBAttribute&lt;String&gt;]
   */
  static final DBIdentity FEATURE_ICON_FROM_RESOURCE = DBIdentity.fromDBObject(NS.object("iconLoader.resource"));
  static final DBIdentity FEATURE_TAGS_ICON = DBIdentity.fromDBObject(NS.object("features.tags.constraintIcon"));
  static final DBIdentity FEATURE_KEY_DATA_LOADER = DBIdentity.fromDBObject(NS.object("features.tags.keyDataLoader"));

  @Override
  public void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_KEY_DATA_LOADER, SerializableFeature.NoParameters.create(KEY_LOADER, ModelKeyLoader.class));
    registry.register(FEATURE_TAGS_ICON, SerializableFeature.NoParameters.create(Icons.TAG_DEFAULT, Icon.class));
    registry.register(FEATURE_ICON_FROM_RESOURCE, TagIcons.LOADER);
  }

  private static final Function<LoadedItem, String> TOOLTIP_GETTER = new Function<LoadedItem, String>() {
    public String invoke(LoadedItem argument) {
      ModelKey<List<ItemKey>> mk = TagsComponentImpl.getModelKey(argument.services().getActor(GuiFeaturesManager.ROLE));
      if (mk == null) return null;
      List<ItemKey> tags = argument.getModelKeyValue(mk);
      return TextUtil.separate(tags, ", ", ItemKey.DISPLAY_NAME);
    }
  };
  static final ModelKeyLoader KEY_LOADER = new ModelKeyLoader(new DataHolder(TypedKey.EMPTY_ARRAY)) {
    @Override
    public boolean loadKey(LoadedModelKey.Builder<?> b, GuiFeaturesManager guiFeatures) {
      EnumTypesCollector.Loaded enumType = guiFeatures.getEnumTypes().getType(TagsComponentImpl.ENUM_TYPE);
      LoadedModelKey.Builder<List<ItemKey>> builder = b.setListDataClass(ItemKey.class);
      builder.setAccessor(new DataAccessor.SimpleDataAccessor<List<ItemKey>>(builder.getName()));
      builder.setIO(new MultiEnumIO(TagsComponentImpl.TAGS_ATTRIBUTE, enumType) {
        @Override
        public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values, ModelKey<List<ItemKey>> modelKey) {
          super.extractValue(itemVersion, itemServices, values, modelKey);
          List<ItemKey> tags = modelKey.getValue(values);
          if (tags == null || tags.isEmpty()) return;
          Set<String> addedIcons = Collections15.hashSet();
          for (ItemKey tag : tags) {
            LoadedItemKey loadedTag = Util.castNullable(LoadedItemKey.class, tag);
            if (loadedTag == null) LogHelper.error("Wrong loaded tag key", tag);
            else StateIconHelper.addStateIcon(values, getNewStateIcon(loadedTag, addedIcons));
          }
        }

        @Override
        public String toString() {
          return "TagsIO";
        }
      });
      return true;
    }

    private final Map<String, StateIcon> myStateIconCache = Collections15.hashMap();
    private synchronized StateIcon getNewStateIcon(LoadedItemKey tag, Set<String> addedIcons) {
      if (tag == null) return null;
      String iconPath = tag.getValue(TagsComponentImpl.ICON_PATH);
      Icon icon = tag.getIcon();
      if (icon == null || iconPath == null) return null;
      if (TagIcons.NO_ICON.equals(iconPath) || !addedIcons.add(iconPath)) return null;
      StateIcon stateIcon = myStateIconCache.get(iconPath);
      if (stateIcon == null) {
        stateIcon = new StateIcon(icon, -10, Terms.ref_Artifact + " is tagged", TOOLTIP_GETTER);
        myStateIconCache.put(iconPath, stateIcon);
      }
      return stateIcon;
    }
  };
}
