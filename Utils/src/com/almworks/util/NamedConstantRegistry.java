package com.almworks.util;


import org.almworks.util.Collections15;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * :todoc:
 *
 * @author sereda
 */
public class NamedConstantRegistry {
  private final List<NamedConstant> myConstants = Collections15.arrayList();
  private final Map<Object, NamedConstant> myMap = Collections15.hashMap();
  private boolean myRebuildNeeded = false;

  synchronized <T> void register(NamedConstant<T> constant) {
    myConstants.add(constant);
    myRebuildNeeded = true;
  }

  public synchronized <T, R extends NamedConstant<T>> R get(T value) {
    rebuild();
    NamedConstant constant = myMap.get(value);
    return (R) constant;
  }

  private void rebuild() {
    if (!myRebuildNeeded)
      return;
    myMap.clear();
    for (Iterator<NamedConstant> ii = myConstants.iterator(); ii.hasNext();) {
      NamedConstant constant = ii.next();
      NamedConstant oldValue = myMap.get(constant.value());
      if (oldValue != null)
        throw new IllegalArgumentException("registry already contains constant " + oldValue);
      myMap.put(constant.value(), constant);
    }
    myRebuildNeeded = false;
  }
}
