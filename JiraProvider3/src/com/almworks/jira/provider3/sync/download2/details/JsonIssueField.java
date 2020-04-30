package com.almworks.jira.provider3.sync.download2.details;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

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
}
