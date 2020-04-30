package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.LoadedModelKey;
import org.almworks.util.Util;

import java.util.Collections;
import java.util.List;
import java.util.Set;

abstract class NotSupportedDnD extends DnDChange {
  protected final long myModelKey;
  protected final String myMessage;

  public NotSupportedDnD(long modelKey, String message) {
    myModelKey = modelKey;
    myMessage = message;
  }

  protected abstract DBAttribute<?> getAttribute();

  @Override
  public void prepare(DnDApplication application) {
    if (needsChange(application)) application.getProblems().addNotSupported(myMessage);
  }

  protected abstract boolean needsChange(DnDApplication application);

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    NotSupportedDnD other = Util.castNullable(NotSupportedDnD.class, obj);
    return other != null && myModelKey == other.myModelKey && Util.equals(getAttribute(), other.getAttribute());
  }

  @Override
  public int hashCode() {
    return (int)myModelKey;
  }

  public static class Single extends NotSupportedDnD {
    private final DBAttribute<Long> myAttribute;

    public Single(DBAttribute<Long> attribute, long modelKey, String message) {
      super(modelKey, message);
      myAttribute = attribute;
    }

    protected DBAttribute<Long> getAttribute() {
      return myAttribute;
    }

    @Override
    protected boolean needsChange(DnDApplication application) {
      LoadedModelKey<ItemKey> modelKey = application.singleModelKey(myModelKey);
      if (modelKey == null) return false;
      TargetValues target = application.getTargetValues(myAttribute);
      for (ItemWrapper wrapper : application.getItems()) {
        ItemKey value = wrapper.getModelKeyValue(modelKey);
        if (!target.matches(value)) return true;
      }
      return false;
    }
  }

  public static class Multi extends NotSupportedDnD {
    private final DBAttribute<Set<Long>> myAttribute;

    public Multi(DBAttribute<Set<Long>> attribute, long modelKey, String message) {
      super(modelKey, message);
      myAttribute = attribute;
    }

    protected DBAttribute<Set<Long>> getAttribute() {
      return myAttribute;
    }

    @Override
    protected boolean needsChange(DnDApplication application) {
      LoadedModelKey<List<ItemKey>> modelKey = application.multiModelKey(myModelKey);
      if (modelKey == null) return false;
      TargetValues target = application.getTargetValues(myAttribute);
      for (ItemWrapper wrapper : application.getItems()) {
        List<ItemKey> list = Util.NN(wrapper.getModelKeyValue(modelKey), Collections.<ItemKey>emptyList());
        if (!target.matches(list)) return true;
      }
      return false;
    }
  }
}
