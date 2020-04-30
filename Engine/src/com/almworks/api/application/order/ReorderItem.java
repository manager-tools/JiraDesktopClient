package com.almworks.api.application.order;

import com.almworks.api.application.BaseItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.collections.Convertor;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.PropertyMapValueDecorator;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author dyoma
 */
public class ReorderItem extends BaseItemWrapper implements LoadedItem {
  public static final DataRole<ReorderItem> REORDER_ITEM = DataRole.createRole(ReorderItem.class);
  
  public static final Convertor<ReorderItem, Object> GET_ORDER_VALUE = new Convertor<ReorderItem, Object>() {
    public Object convert(ReorderItem value) {
      return value.getNewOrderValue();
    }
  };
  private final LoadedItem myItem;
  private final Object myReorderOwner;
  private final int myInitialIndex;
  private Object myNewOrderValue = null;

  public ReorderItem(LoadedItem item, Object reorderOwner, int initialIndex) {
    myItem = item;
    myReorderOwner = reorderOwner;
    myInitialIndex = initialIndex;
  }

  @Override
  public String toString() {
    return "RA[" + myInitialIndex + "," + myItem + "," + myNewOrderValue + "]";
  }

  public Object getNewOrderValue() {
    return myNewOrderValue;
  }

  public void setNewOrderValue(Object newOrderValue) {
    myNewOrderValue = newOrderValue;
  }

  public Object getReorderOwner() {
    return myReorderOwner;
  }

  public int getInitialIndex() {
    return myInitialIndex;
  }

  public PropertyMap getValues() {
    return myItem.getValues();
  }

  public <T> boolean setValue(TypedKey<T> key, T value) {
    return myItem.setValue(key, value);
  }

  public void setValueDecorator(@Nullable PropertyMapValueDecorator decorator) {
    myItem.setValueDecorator(decorator);
  }

  public List<? extends AnAction> getWorkflowActions() {
    return myItem.getWorkflowActions();
  }

  public List<? extends AnAction> getActions() {
    return myItem.getActions();
  }

  public boolean hasProblems() {
    return myItem.hasProblems();
  }

  public LoadedItemServices services() {
    return myItem.services();
  }

  public boolean isOutOfDate() {
    return myItem.isOutOfDate();
  }

  public void addAWTListener(ChangeListener1<? extends LoadedItem> listener) {
    myItem.addAWTListener(listener);
  }

  public void removeAWTListener(ChangeListener1<? extends LoadedItem> listener) {
    myItem.removeAWTListener(listener);
  }

  public long getItem() {
    return myItem.getItem();
  }

  @NotNull
  public DBStatus getDBStatus() {
    return myItem.getDBStatus();
  }

  public <T> T getModelKeyValue(ModelKey<? extends T> key) {
    return myItem.getModelKeyValue(key);
  }

  public boolean isEditable() {
    return myItem.isEditable();
  }

  public PropertyMap getLastDBValues() {
    return myItem.getLastDBValues();
  }

  public static List<ReorderItem> collect(List<? extends LoadedItem> items, Object reorderOwner) {
    List<ReorderItem> result = Collections15.arrayList(items.size());
    for (int i = 0; i < items.size(); i++) {
      LoadedItem item = items.get(i);
      result.add(new ReorderItem(item, reorderOwner, i));
    }
    return result;
  }

  public LoadedItem getOriginal() {
    return myItem;
  }
}
