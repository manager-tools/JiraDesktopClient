package com.almworks.api.application.order;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.ModelKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.models.TableColumnAccessor;

import java.math.BigDecimal;

public class StringAttributeOrder extends NumericAttributeOrder<String> {
  private final DBAttribute<String> myAttribute;

  public StringAttributeOrder(ModelKey<String> key, TableColumnAccessor<LoadedItem, ?> column,
    DBAttribute<String> attribute)
  {
    super(key, column);
    myAttribute = attribute;
  }

  protected BigDecimal extractValue(LoadedItem item) {
    try {
      String str = item.getModelKeyValue(getKey());
      return str != null ? new BigDecimal(str) : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  protected void setDBValue(ItemVersionCreator creator, BigDecimal value) {
    creator.setValue(myAttribute, value.toString());
  }
}
