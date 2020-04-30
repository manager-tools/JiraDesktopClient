package com.almworks.items.entities.dbwrite;

import com.almworks.items.api.DBReader;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.sync.ItemVersion;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CustomResolution {
  CustomResolution DUMMY = new CustomResolution() {
    @Override
    public long resolve(@NotNull Helper helper, @NotNull Entity entity) {
      return 0;
    }
  };

  long resolve(@NotNull Helper helper, @NotNull Entity entity);

  interface Helper {
    @NotNull
    ItemVersion getConnection();

    DBReader getReader();

    long resolve(Entity entity);

    <T> void putCachedValue(TypedKey<T> key, @Nullable T value);

    @Nullable
    <T> T getCachedValue(TypedKey<T> key);
  }

  public class Composite implements CustomResolution {
    private final CustomResolution[] myResolutions;

    public Composite(List<CustomResolution> resolutions) {
      myResolutions = resolutions.toArray(new CustomResolution[resolutions.size()]);
    }

    @Override
    public long resolve(@NotNull Helper helper, @NotNull Entity entity) {
      for (CustomResolution resolution : myResolutions) {
        long item = resolution.resolve(helper, entity);
        if (item > 0) return item;
      }
      return 0;
    }
  }
}
