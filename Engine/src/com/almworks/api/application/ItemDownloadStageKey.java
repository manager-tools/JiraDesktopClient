package com.almworks.api.application;

import com.almworks.items.sync.ItemVersion;
import com.almworks.util.LogHelper;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ItemDownloadStageKey extends SystemKey<ItemDownloadStage> implements AutoAddedModelKey<ItemDownloadStage> {
  public static final Role<ItemDownloadStageKey> ROLE = Role.role(ItemDownloadStageKey.class);
  public static final ItemDownloadStageKey INSTANCE = new ItemDownloadStageKey();

  private ItemDownloadStageKey() {
    super("downloadStage");
  }

  public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values) {
    ItemDownloadStage stage = ItemDownloadStage.getValue(itemVersion);
    values.put(getModelKey(), stage);
  }

  @NotNull
  public static ItemDownloadStage retrieveValue(PropertyMap values) {
    ItemDownloadStageKey key = INSTANCE;
    ItemDownloadStage value = key.getValue(values);
    if (value == null) {
      assert false : values;
      return ItemDownloadStage.DEFAULT;
    }
    return value;
  }

  public static ItemDownloadStage retrieveValue(ModelMap map) {
    ItemDownloadStageKey key = INSTANCE;
    ItemDownloadStage value = key.getValue(map);
    if (value == null) {
      assert false : map;
      return ItemDownloadStage.DEFAULT;
    }
    return value;
  }

  @NotNull
  public static ItemDownloadStage retrieveValue(@NotNull ItemWrapper wrapper) {
    return retrieveValue(wrapper.getLastDBValues());
  }

  public static boolean isAllHasActualDetails(Collection<ItemWrapper> wrappers) {
    if (wrappers == null || wrappers.isEmpty()) return false;
    for (ItemWrapper wrapper : wrappers) {
      ItemDownloadStage stage = wrapper.getModelKeyValue(INSTANCE);
      if (stage == null) {
        LogHelper.error("Missing stage for", wrapper);
        return false;
      }
      if (stage == ItemDownloadStage.DUMMY || stage == ItemDownloadStage.QUICK || stage == ItemDownloadStage.STALE) return false;
    }
    return true;
  }

  public static boolean isUploaded(ModelMap model) {
    if (model == null) return false;
    ItemDownloadStage stage = INSTANCE.getValue(model);
    LogHelper.assertError(stage != null, model);
    return stage != ItemDownloadStage.NEW;
  }
}



