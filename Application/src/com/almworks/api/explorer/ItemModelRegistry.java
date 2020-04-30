package com.almworks.api.explorer;

import com.almworks.explorer.loader.ItemUiModelImpl;
import com.almworks.explorer.loader.ItemUpdateEvent;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.Role;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

/**
 * @author : Dyoma
 */
public interface ItemModelRegistry {
  Role<ItemModelRegistry> ROLE = Role.role(ItemModelRegistry.class.getName());
  TypedKey<Long> ICN = TypedKey.create("ICN");

  /** @return null if item type is unknown (not in the {@link com.almworks.api.application.MetaInfo#REGISTRY meta info registry})*/
  @Nullable
  ItemUiModelImpl createNewModel(long item, DBReader reader);

  /** @return null if item <br>
   * 1. type is unknown (not in the {@link com.almworks.api.application.MetaInfo#REGISTRY meta info registry})<br>
   * 2. item has no active connection (the connection registered in engine)
   */
  @Nullable
  PropertyMap extractValues(ItemVersion itemVersion);

  void addListener(Lifespan life, Listener listener);

  <T> T getActor(Role<T> role);

  public interface Listener {
    void onItemUpdated(ItemUpdateEvent event);
  }
}
