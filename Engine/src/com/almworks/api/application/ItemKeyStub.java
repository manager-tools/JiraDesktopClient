package com.almworks.api.application;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ItemKeyStub extends ItemKey {
  public static final ItemKeyStub ABSENT = new ItemKeyStub("");

  private final String myId;
  private final String myDisplayName;
  private final ItemOrder myOrder;
  private final Icon myIcon;

  public ItemKeyStub(String id, String displayName, ItemOrder order) {
    this(id, displayName, order, null);
  }

  public ItemKeyStub(String id, String displayName, ItemOrder order, Icon icon) {
    myId = id == null ? id : id.intern();
    displayName = hackFixHtmlValueName(displayName);
    myDisplayName = displayName == null ? displayName : displayName.intern();
    myOrder = order;
    myIcon = icon;
  }

  public ItemKeyStub(ItemKey value) {
    this(value.getId(), value.getDisplayName(), value.getOrder(), value.getIcon());
  }

  public ItemKeyStub(String id) {
    this(id, id, ItemOrder.byString(id));
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return myIcon;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  @NotNull
  public String getDisplayName() {
    return myDisplayName;
  }

  @NotNull
  public ItemOrder getOrder() {
    return myOrder;
  }
}
