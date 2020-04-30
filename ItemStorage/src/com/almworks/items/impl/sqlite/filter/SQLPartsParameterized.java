package com.almworks.items.impl.sqlite.filter;

import com.almworks.sqlite4java.SQLParts;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class SQLPartsParameterized {
  private final SQLParts myParts = new SQLParts();
  private List<Object> myParameters;

  public SQLParts getParts() {
    return myParts;
  }

  public List<Object> getParameters() {
    return myParameters;
  }

  public void clear() {
    myParts.clear();
    if (myParameters != null)
      myParameters.clear();
  }

  public void addParameters(Object ... values) {
    addParametersList(Arrays.asList(values));
  }

  public void addParametersList(@Nullable List<?> values) {
    if (values == null) return;
    if (myParameters == null)
      myParameters = Collections15.arrayList();
    myParameters.addAll(values);
  }
}
