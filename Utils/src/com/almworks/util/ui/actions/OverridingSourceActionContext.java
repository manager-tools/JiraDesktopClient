package com.almworks.util.ui.actions;

import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OverridingSourceActionContext extends DelegatingActionContext {
  private final Map<TypedKey, List> myOverride = Collections15.hashMap();

  public OverridingSourceActionContext(ActionContext delegate) {
    super(delegate);
  }

  public <T> void override(TypedKey<T> key, T value) {
    assert key != null;
    assert value != null;
    myOverride.put(key, Collections.singletonList(value));
  }

  public <T> void override(TypedKey<T> key, List<T> values) {
    assert key != null;
    assert values != null;
    myOverride.put(key, values);
  }

  public <T> T getSourceObject(TypedKey<? extends T> role) throws CantPerformException {
    List c = myOverride.get(role);
    if (c != null) {
      if (c.size() != 1)
        throw new CantPerformException();
      else
        return (T) c.get(0);
    }
    return super.getSourceObject(role);
  }

  public <T> List<T> getSourceCollection(@NotNull TypedKey<? extends T> role) throws CantPerformException {
    List c = myOverride.get(role);
    if (c != null) {
      return Collections15.arrayList(c);
    }
    return super.getSourceCollection(role);
  }
}
