package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;

import java.util.Collection;
import java.util.Collections;

/**
 * Field decorator. Converts null to fixed null-value or passes on not null JSON value.
 */
public class NullDecorator implements JsonIssueField {
  private final ParsedValue myNullValue;
  private final JsonIssueField myNotNull;

  public NullDecorator(ParsedValue nullValue, JsonIssueField notNull) {
    myNullValue = nullValue;
    myNotNull = notNull;
  }

  @Override
  public Collection<? extends ParsedValue> loadValue(Object jsonValue) {
    if (jsonValue == null) return Collections.singleton(myNullValue);
    return myNotNull.loadValue(jsonValue);
  }

  @Override
  public Collection<? extends ParsedValue> loadNull() {
    return Collections.singleton(myNullValue);
  }
}
