package com.almworks.api.application.field;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.order.Order;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TableRendererCell;
import com.almworks.util.models.TableColumnAccessor;
import org.jetbrains.annotations.Nullable;

public interface ItemField<T, S> {
  void start(DBReader reader);

  void stop();

  ModelKey<T> getModelKey();

  T loadValue(ItemVersion version, LoadedItemServices itemServices);

  DBAttribute<S> getAttribute();

  TableColumnAccessor<LoadedItem, ?> getColumn();

  ConstraintDescriptor getDescriptor();

  Order createOrder();

  String getId(); 

  @Nullable
  TableRendererCell getViewerCell(ModelMap modelMap, TableRenderer renderer);

  boolean isMultilineText();

  String getDisplayName();
}
