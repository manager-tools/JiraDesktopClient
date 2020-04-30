package com.almworks.api.application.order;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.ModelKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.models.TableColumnAccessor;

import java.math.BigDecimal;

public class BigDecimalAttributeOrder extends NumericAttributeOrder<BigDecimal> {
  private final DBAttribute<BigDecimal> myAttribute;

  public BigDecimalAttributeOrder(ModelKey<BigDecimal> key, TableColumnAccessor<LoadedItem, ?> column,
    DBAttribute<BigDecimal> attribute)
  {
    super(key, column);
    myAttribute = attribute;
  }

  protected BigDecimal extractValue(LoadedItem item) {
    return item.getModelKeyValue(getKey());
  }

  protected void setDBValue(ItemVersionCreator creator, BigDecimal value) {
    creator.setValue(myAttribute, value);
  }
}
