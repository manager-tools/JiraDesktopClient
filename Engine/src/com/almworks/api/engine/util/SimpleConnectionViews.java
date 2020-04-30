package com.almworks.api.engine.util;

import com.almworks.api.engine.ConnectionViews;
import com.almworks.api.engine.PrimaryItemStructure;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.Database;
import com.almworks.items.api.ItemReference;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Modifiable;
import org.jetbrains.annotations.NotNull;

public class SimpleConnectionViews implements ConnectionViews {
  private final DBFilter myFilter;
  private final Modifiable myModifiable;
  private final DBFilter myOutbox;

  public SimpleConnectionViews(ItemReference connectionRef, Modifiable projectsChanged, Database db,
    PrimaryItemStructure primaryStructure) {
    myModifiable = projectsChanged;
    myFilter = db.filter(
      BoolExpr.and(DPEqualsIdentified.create(SyncAttributes.CONNECTION, connectionRef), primaryStructure.getPrimaryItemsFilter()));
    myOutbox = myFilter.filter(primaryStructure.getLocallyChangedFilter());
  }

  @Override
  public DBFilter getConnectionItems() {
    return myFilter;
  }

  @NotNull
  @Override
  public Modifiable connectionItemsChange() {
    return myModifiable;
  }

  @Override
  public DBFilter getOutbox() {
    return myOutbox;
  }
}
