package com.almworks.container;

import com.almworks.api.container.ActorSelector;
import org.almworks.util.Collections15;

import java.util.Map;

class Selectors {
  private final Selectors myParent;
  private final Map<Class, Class<? extends ActorSelector>> mySelectors = Collections15.hashMap();

  public Selectors() {
    this(null);
  }

  public Selectors(Selectors parent) {
    myParent = parent;
  }

  public Class<? extends ActorSelector> getProvider(Class abstractClass) {
    Class<? extends ActorSelector> aClass = mySelectors.get(abstractClass);
    return aClass != null ? aClass : (myParent != null ? myParent.getProvider(abstractClass) : null);
  }

  public void addProvider(Class abstractClass, Class<? extends ActorSelector> concreteProvider) {
    mySelectors.put(abstractClass, concreteProvider);
  }
}
