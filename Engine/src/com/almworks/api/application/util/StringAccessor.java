package com.almworks.api.application.util;

import org.jetbrains.annotations.Nullable;

public class StringAccessor extends DataAccessor.SimpleDataAccessor<String> {
  public StringAccessor(String id) {
    super(id);
  }

  @Override
    protected Object getCanonicalValueForComparison(@Nullable String value) {
    return value == null || value.isEmpty() ? null : value;
  }
}
