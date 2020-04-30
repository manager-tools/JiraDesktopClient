package com.almworks.api.explorer;

import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.properties.Role;

public interface AdditionalHierarchies {
  public static final Role<AdditionalHierarchies> ROLE = Role.role("additionalHierarchies", AdditionalHierarchies.class);

  AListModel<ItemsTreeLayout> getAdditionalLayouts();
}
