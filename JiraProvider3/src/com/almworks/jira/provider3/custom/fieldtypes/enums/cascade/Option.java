package com.almworks.jira.provider3.custom.fieldtypes.enums.cascade;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

class Option {
  private final int myId;
  private final String myName;
  private final Option myParent;
  private final int myOrder;

  Option(int id, String name, Option parent, int order) {
    myId = id;
    myName = name;
    myParent = parent;
    myOrder = order;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    Option other = Util.castNullable(Option.class, obj);
    return other != null && other.myId == myId;
  }

  @Override
  public int hashCode() {
    return myId;
  }

  @Nullable
  public EntityHolder createEntity(EntityTransaction transaction, Entity type) {
    EntityHolder holder = transaction.addEntity(type, ServerCustomField.ENUM_ID, myId);
    if (holder == null) return null;
    holder.setNNValue(ServerCustomField.ENUM_DISPLAY_NAME, myName);
    holder.setValue(ServerCustomField.ENUM_ORDER, myOrder);
    Option parent = myParent;
    holder.setReference(ServerCustomField.ENUM_PARENT, parent != null ? transaction.addEntity(type, ServerCustomField.ENUM_ID, parent.myId) : null);
    return holder;
  }
}
