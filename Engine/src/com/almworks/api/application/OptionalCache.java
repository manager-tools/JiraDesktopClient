package com.almworks.api.application;

import com.almworks.api.application.util.PredefinedKey;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public class OptionalCache {
  private static final ModelKey<PropertyMap> KEY = PredefinedKey.create("optionalCache");

  @ThreadAWT
  @Nullable
  public static <T> T getCachedValue(TypedKey<T> key, PropertyMap map) {
    return getCache(map).get(key);
  }

  @ThreadAWT
  @NotNull
  private static PropertyMap getCache(PropertyMap map) {
    Threads.assertAWTThread();
    PropertyMap cache = KEY.getValue(map);
    if (cache == null) {
      final PropertyMap newCache = new PropertyMap();
      cache = newCache;
      map.addAWTChangeListener(Lifespan.FOREVER, new ChangeListener() {
        public void onChange() {
          newCache.clear();
        }
      });
      KEY.setValue(map, cache);
    }
    return cache;
  }

  @ThreadAWT
  public static <T> void cacheValue(TypedKey<T> key, PropertyMap item, T value) {
    getCache(item).put(key, value);
  }
}
