package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class FieldMultiplexer implements JsonIssueField {
  private final JsonIssueField[] myFields;

  public FieldMultiplexer(JsonIssueField ... fields) {
    myFields = ArrayUtil.arrayCopy(fields);
  }

  @Override
  public Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue) {
    ArrayList<ParsedValue> result = Collections15.arrayList();
    for (JsonIssueField field : myFields) {
      Collection<? extends ParsedValue> values = field.loadValue(jsonValue);
      if (values != null) result.addAll(values);
    }
    return result;
  }

  @Override
  public Collection<? extends ParsedValue> loadNull() {
    ArrayList<ParsedValue> result = Collections15.arrayList();
    for (JsonIssueField field : myFields) {
      Collection<? extends ParsedValue> values = field.loadNull();
      if (values != null) result.addAll(values);
    }
    return result;
  }
}
