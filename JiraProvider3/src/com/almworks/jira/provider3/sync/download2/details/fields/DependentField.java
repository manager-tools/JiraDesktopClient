package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class DependentField implements JsonIssueField {
  private final EntityKey<Entity> myKey;
  private final Convertor<Object, Entity> myElementConvertor;
  private final ValueSupplement<Entity> mySupplement;

  public DependentField(EntityKey<Entity> key, Convertor<Object, Entity> elementConvertor, ValueSupplement<Entity> supplement) {
    myKey = key;
    myElementConvertor = elementConvertor;
    mySupplement = supplement;
  }

  @Override
  public Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue) {
    if (jsonValue == null) return SimpleKeyValue.single(myKey, null);
    Entity entity = myElementConvertor.convert(jsonValue);
    if (entity == null) {
      LogHelper.error("Null conversion", jsonValue.getClass());
      return SimpleKeyValue.single(myKey, null);
    }
    return Collections.singleton(new MyValue(myKey, entity, mySupplement));
  }

  @Override
  public Collection<? extends ParsedValue> loadNull() {
    return SimpleKeyValue.single(myKey, null);
  }

  private static class MyValue implements JsonIssueField.ParsedValue {
    private final EntityKey<Entity> myKey;
    private final Entity myEntityValue;
    private final ValueSupplement<Entity> mySupplement;

    public MyValue(EntityKey<Entity> key, Entity entityValue, ValueSupplement<Entity> supplement) {
      myKey = key;
      myEntityValue = entityValue;
      mySupplement = supplement;
    }

    @Override
    public boolean addTo(EntityHolder target) {
      if (!mySupplement.supply(target, myEntityValue)) return false;
      myEntityValue.fix();
      target.setValue(myKey, myEntityValue);
      return true;
    }
  }
}
