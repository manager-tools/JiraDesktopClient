package com.almworks.items.gui.meta.schema.modelkeys;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.DataIO;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MultiEnumIO implements DataIO<List<ItemKey>> {
  private final DBAttribute<? extends Collection<Long>> myAttribute;
  private final EnumTypesCollector.Loaded myEnumType;

  public MultiEnumIO(DBAttribute<? extends Collection<Long>> attribute, EnumTypesCollector.Loaded enumType) {
    myAttribute = attribute;
    myEnumType = enumType;
  }

  @Override
  public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values,
    ModelKey<List<ItemKey>> modelKey)
  {
    List<ItemKey> items = Collections15.arrayList();
    myEnumType.collectResolvedItems(itemServices.getItemKeyCache(), itemVersion, myAttribute, items);
    Collections.sort(items);
    modelKey.setValue(values, items);
  }

  @Override
  public String toString() {
    return String.format("MultiEnuIO[%s : %s", myAttribute, myEnumType);
  }
}
