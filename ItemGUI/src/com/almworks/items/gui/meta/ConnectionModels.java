package com.almworks.items.gui.meta;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.ItemReference;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.model.ValueModel;
import com.almworks.util.models.TableColumnAccessor;
import org.almworks.util.detach.Lifespan;

public class ConnectionModels {
  private final AListModel<ConstraintDescriptor> myDescriptors;
  private final AListModel<? extends TableColumnAccessor<LoadedItem, ?>> myMainColumns;

  private ConnectionModels(AListModel<ConstraintDescriptor> descriptors,
    AListModel<? extends TableColumnAccessor<LoadedItem, ?>> columns) {
    myDescriptors = descriptors;
    myMainColumns = columns;
  }

  public static ConnectionModels create(Lifespan life, GuiFeaturesManager features, ItemReference... owners) {
    ValueModel<? extends ScopeFilter> scope = ScopeFilter.refersAny(life, features.getImage().getDatabase(), DBCommons.OWNER,
      owners);
    AListModel<ConstraintDescriptor> descriptors = features.getDescriptorsModel(life, scope);
    AListModel<? extends TableColumnAccessor<LoadedItem, ?>> columns = features.getColumnsModel(life, scope);
    return new ConnectionModels(descriptors, columns);
  }

  public AListModel<? extends ConstraintDescriptor> getDescriptors() {
    return myDescriptors;
  }

  public AListModel<? extends TableColumnAccessor<LoadedItem, ?>> getMainColumns() {
    return myMainColumns;
  }
}
