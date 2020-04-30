package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class SimpleKeyValue<T> implements JsonIssueField.ParsedValue {
  private final EntityKey<T> myKey;
  private final T myValue;

  public SimpleKeyValue(EntityKey<T> key, T value) {
    myKey = key;
    myValue = value;
  }

  @Override
  public boolean addTo(EntityHolder entity) {
    entity.setValue(myKey, myValue);
    return true;
  }

  public static <T> Collection<JsonIssueField.ParsedValue> single(EntityKey<T> key, @Nullable T value) {
    return Collections.<JsonIssueField.ParsedValue>singleton(new SimpleKeyValue<T>(key, value));
  }

  @Override
  public String toString() {
    return myKey + " <- " + myValue;
  }
}
