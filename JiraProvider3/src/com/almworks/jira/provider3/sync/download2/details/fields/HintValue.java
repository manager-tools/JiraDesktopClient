package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import org.almworks.util.Util;

import java.util.Collection;
import java.util.Collections;

/**
 * This implementation sets hint value to the issue.<br>
 * Also it provides utilities.
 * @param <T> type of hint value
 * @see #getValue(com.almworks.items.entities.api.collector.transaction.EntityHolder)
 * @see #isValueSet(com.almworks.items.entities.api.collector.transaction.EntityHolder)
 * @see #singleton()
 */
public class HintValue<T> implements JsonIssueField.ParsedValue {
  private final EntityKey<T> myHint;
  private final T myHintValue;

  public HintValue(EntityKey<T> hint, T hintValue) {
    myHint = hint;
    myHintValue = hintValue;
  }

  @Override
  public boolean addTo(EntityHolder issue) {
    issue.setValue(myHint, myHintValue);
    return true;
  }

  /**
   * @return flag-setter. Client may check if the flag has been set via {@link #isValueSet(com.almworks.items.entities.api.collector.transaction.EntityHolder)}
   * @see #create(String, Class, Object)
   */
  public static HintValue<Boolean> flag(String hintId) {
    return create(hintId, Boolean.class, true);
  }

  /**
   * @param hintId id to create hint key
   * @param aClass hint value type
   * @param hintValue hint value to set
   */
  public static <T> HintValue<T> create(String hintId, Class<T> aClass, T hintValue) {
    EntityKey<T> hint = EntityKey.hint(hintId, aClass);
    return new HintValue<T>(hint, hintValue);
  }

  public boolean isValueSet(EntityHolder issue) {
    T value = getValue(issue);
    return Util.equals(myHintValue, value);
  }

  public T getValue(EntityHolder issue) {
    return issue.getScalarValue(myHint);
  }

  /**
   * @return singleton collection of this {@link com.almworks.jira.provider3.sync.download2.details.JsonIssueField.ParsedValue}
   */
  public Collection<? extends JsonIssueField.ParsedValue> singleton() {
    return Collections.singletonList(this);
  }
}
