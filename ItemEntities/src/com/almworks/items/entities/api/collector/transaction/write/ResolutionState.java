package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.sync.ItemProxy;

class ResolutionState {
  private final int myCreateResolution;
  private final long myItem;
  private final EntityPlace myPlace;
  private final ItemProxy myProxy;

  ResolutionState(EntityPlace place, int createResolution, long item, ItemProxy proxy) {
    myCreateResolution = createResolution;
    myPlace = place;
    myItem = item;
    myProxy = proxy;
  }

  boolean isResolved() {
    return myItem > 0;
  }

  public long getItem() {
    return myItem;
  }

  public ItemProxy getProxy() {
    return myProxy;
  }

  public ResolutionState better(ResolutionState state) {
    if (state == null || isResolved()) return this;
    if (state.isResolved()) return state;
    if (isCanCreate()) return this;
    if (state.isCanCreate()) return state;
    return this;
  }

  public boolean isCanCreate() {
    return myCreateResolution >= 0 || myProxy != null;
  }

  public EntityPlace getPlace() {
    return myPlace;
  }

  public static ResolutionState byItem(EntityPlace place, long item) {
    return new ResolutionState(place, -1, item, null);
  }

  public static ResolutionState canCreate(int resolutionIndex, EntityPlace place) {
    return new ResolutionState(place, resolutionIndex, 0, null);
  }

  public static ResolutionState notFound(EntityPlace place) {
    return new ResolutionState(place, -1, 0, null);
  }

  public int getCreateResolution() {
    return myCreateResolution;
  }

  public static ResolutionState byProxy(EntityPlace place, ItemProxy proxy) {
    return new ResolutionState(place, -1, 0, proxy);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("Resolution[");
    if (myItem > 0) builder.append(myItem);
    else if (myProxy != null) builder.append(myProxy);
    else if (myCreateResolution >= 0) builder.append("create#").append(myCreateResolution);
    else builder.append("search");
    builder.append(" -> ").append(myPlace);
    return builder.append("]").toString();
  }
}
