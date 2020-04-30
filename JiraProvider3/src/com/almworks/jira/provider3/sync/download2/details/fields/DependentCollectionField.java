package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.json.simple.JSONArray;

import java.util.*;

public class DependentCollectionField implements JsonIssueField {
  private final Convertor<Object, Entity> myElementConvertor;
  private final EntityKey<Collection<Entity>> myKey;
  private final ValueSupplement<Entity> mySupplement;

  public DependentCollectionField(EntityKey<Collection<Entity>> key, Convertor<Object, Entity> elementConvertor, ValueSupplement<Entity> supplement) {
    myKey = key;
    myElementConvertor = elementConvertor;
    mySupplement = supplement;
  }

  @Override
  public Collection<? extends ParsedValue> loadValue(Object jsonValue) {
    if (jsonValue == null) return SimpleKeyValue.single(myKey, null);
    //noinspection unchecked
    List<Object> array = Util.castNullable(JSONArray.class, jsonValue);
    if (array == null) {
      LogHelper.error("Expected array", jsonValue);
      return null;
    }
    ArrayList<Entity> valueList = Collections15.arrayList();
    for (Object element : array) {
      Entity converted = myElementConvertor.convert(element);
      if (converted == null) continue;
      if (converted.isFixed()) {
        LogHelper.error("Cannot supply fixed", converted);
        converted = Entity.copy(converted);
      }
      valueList.add(converted);
    }
    return Collections.singleton(new MyValue(myKey, valueList, mySupplement));
  }

  @Override
  public Collection<? extends ParsedValue> loadNull() {
    return SimpleKeyValue.single(myKey, null);
  }

  private static class MyValue implements ParsedValue {
    private final EntityKey<Collection<Entity>> myKey;
    private final List<Entity> myValueList;
    private final ArrayList<Entity> myNotBuilt;
    private final ValueSupplement<Entity> mySupplement;

    public MyValue(EntityKey<Collection<Entity>> key, ArrayList<Entity> valueList, ValueSupplement<Entity> supplement) {
      myKey = key;
      myValueList = valueList;
      mySupplement = supplement;
      myNotBuilt = Collections15.arrayList(valueList);
    }

    @Override
    public boolean addTo(EntityHolder target) {
      for (Iterator<Entity> it = myNotBuilt.iterator(); it.hasNext(); ) {
        Entity entity = it.next();
        if (mySupplement.supply(target, entity)) {
          entity.fix();
          it.remove();
        }
      }
      if (myNotBuilt.isEmpty()) {
        target.setValue(myKey, myValueList);
        return true;
      } else return false;
    }

    @Override
    public String toString() {
      return myKey + " <- " + myValueList;
    }
  }
}
