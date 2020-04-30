package com.almworks.api.application;

import com.almworks.util.collections.Convertor;
import com.almworks.util.ui.actions.DataRole;

public class ItemKeyGroup {
  public static final ItemKeyGroup NULL_GROUP_SENTINEL = new ItemKeyGroup("- No group -", Integer.MIN_VALUE);
  public static final DataRole<ItemKeyGroup> ITEM_KEY_GROUP_ROLE = DataRole.createRole(ItemKeyGroup.class, "ITEM_KEY_GROUP");
  public static final Convertor<ItemKeyGroup, String> TO_DISPLAY_STRING = new Convertor<ItemKeyGroup, String>() {
    public String convert(ItemKeyGroup value) {
      return value.getDisplayableName();
    }
  };

  private final String myDisplayName;
  private final int myOrder;

  public ItemKeyGroup(String displayName) {
    this(displayName, 0);
  }

  public ItemKeyGroup(String displayName, int order) {
    myDisplayName = displayName;
    myOrder = order;
  }

  public String getDisplayableName() {
    return myDisplayName;
  }

  public int getOrder() {
    return myOrder;
  }

  public String toString() {
    return myDisplayName;
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ItemKeyGroup that = (ItemKeyGroup) o;

    if (myDisplayName != null ? !myDisplayName.equals(that.myDisplayName) : that.myDisplayName != null)
      return false;

    return true;
  }

  public int hashCode() {
    return (myDisplayName != null ? myDisplayName.hashCode() : 0);
  }
}
