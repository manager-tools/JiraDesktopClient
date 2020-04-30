package com.almworks.items.entities.api.util;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DataSequence {
  private final Object[] myData;

  private DataSequence(Object[] data) {
    myData = data;
  }

  public static EntityKey<DataSequence> createKey(String id, @Nullable Entity description) {
    return EntityKey.scalar(id, DataSequence.class, description);
  }

  public int size() {
    return myData.length;
  }

  public Object get(int index) {
    return myData[index];
  }

  public static class Builder {
    private final List<Object> myData = Collections15.arrayList();

    public Builder appendText(String text) {
      myData.add(text);
      return this;
    }

    public Builder appendEntity(Entity entity) {
      myData.add(entity);
      return this;
    }

    public Builder appendBoolean(boolean bool) {
      myData.add(bool);
      return this;
    }

    public DataSequence create() {
      return new DataSequence(myData.toArray());
    }
  }
}
