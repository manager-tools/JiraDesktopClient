package com.almworks.jira.provider3.sync.download2.details;

import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.fields.SimpleKeyValue;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.Convertor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * REST issue presentation parsing has two stages:<br>
 * 1. Decode JSON. Extract important info from JSON object and save it as instance of {@link ParsedValue}<br>
 * 2. Apply extracted info to issue. This stage is required in case when final entity object depends on other issue fields.
 * For example version depends on value of issue project. So version JSON object cannot be converted to entity object until issue project (as entity) is known.<br><br>
 * This interface represents first stage and {@link ParsedValue} is the second one.
 */
public interface JsonIssueField {
  /**
   * Called when JSON contains a value for the field. The value can be null. But it must be mentioned.
   * @return collection of processed values. Null has the same meaning as an empty collection
   */
  @Nullable
  Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue);

  /**
   * Called when JSON does not contain any value (including null) for the field.
   * @return same as {@link #loadValue(Object)}
   * @see #loadValue(Object)
   */
  @Nullable
  Collection<? extends ParsedValue> loadNull();

  interface ParsedValue {
    /**
     * Apply preprocessed JSON to issue entity if sufficient info is already available.
     * @param issue
     * @return true if issue is updated.<br>
     *   false means that important data is still missing. The value should be asked to add again when more data is added to the issue.
     */
    boolean addTo(EntityHolder issue);
  }

  /**
   * This implementation enables to ignore value (as if they are not provided by Jira).
   * This makes sense when Jira sometimes reports invalid values, so if it's possible to distinguish valid value,
   * this instance implementation enables to ignore doubtful values.
   * @see com.almworks.jira.provider3.sync.download2.details.fields.ScalarField
   */
  class FilteredValue<T> implements JsonIssueField {
    private final Convertor<Object, T> myConvertor;
    private final Predicate<T> myAccept;
    private final EntityKey<T> myKey;

    public FilteredValue(Convertor<Object, T> convertor, Predicate<T> accept, EntityKey<T> key) {
      myConvertor = convertor;
      myAccept = accept;
      myKey = key;
    }

    /**
     * Ignores false values (as if they were not provided by Jira)
     */
    public static JsonIssueField boolTrueOnly(EntityKey<Boolean> key) {
      return new FilteredValue<>(JSONKey.FALSE_TO_NULL, Boolean.TRUE::equals, key);
    }

    public static JsonIssueField integerPositive(EntityKey<Integer> key) {
      return new FilteredValue<>(JSONKey.INTEGER, integer -> integer != null && integer > 0, key);
    }

    @Nullable
    @Override
    public Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue) {
      return checkValue(myConvertor.convert(jsonValue));
    }

    @Nullable
    @Override
    public Collection<? extends ParsedValue> loadNull() {
      return checkValue(null);
    }

    @Nullable
    private Collection<? extends ParsedValue> checkValue(@Nullable T value) {
      if (!myAccept.test(value)) return null;
      return SimpleKeyValue.single(myKey, value);
    }
  }
}
