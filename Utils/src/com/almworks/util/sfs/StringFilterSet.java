package com.almworks.util.sfs;

import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class StringFilterSet {
  public static final StringFilterSet ALL = new StringFilterSet(Kind.ALL, Collections15.<StringFilter>emptyList());

  private static final String KIND_SETTING = "kind";
  private static final String FILTER_SETTING = "filter";

  private final Kind myKind;
  private final List<StringFilter> myFilters;

  public StringFilterSet(Kind kind, List<StringFilter> filters) {
    myKind = kind;
    myFilters = Collections.unmodifiableList(filters);
  }

  public Kind getKind() {
    return myKind;
  }

  public List<StringFilter> getFilters() {
    return myFilters;
  }

  public boolean isAccepted(@NotNull String string) {
    switch (myKind) {
    case INCLUSIVE:
      return matches(string);
    case EXCLUSIVE:
      return !matches(string);
    default:
      return true;
    }
  }

  private boolean matches(String string) {
    for (StringFilter filter : myFilters) {
      if (filter.isAccepted(string))
        return true;
    }
    return false;
  }

  @NotNull
  public static StringFilterSet readFrom(ReadonlyConfiguration config) {
    int kindInt = config.getIntegerSetting(KIND_SETTING, 0);
    Kind kind;
    if (kindInt == -1)
      kind = Kind.EXCLUSIVE;
    else if (kindInt == 1)
      kind = Kind.INCLUSIVE;
    else
      kind = Kind.ALL;
    List<? extends ReadonlyConfiguration> configs = config.getAllSubsets(FILTER_SETTING);
    List<StringFilter> filters = Collections15.arrayList();
    if (configs != null) {
      for (ReadonlyConfiguration c : configs) {
        filters.add(StringFilter.readFrom(c));
      }
    }
    return new StringFilterSet(kind, filters);
  }

  public void writeTo(Configuration config) {
    int code;
    if (myKind == Kind.INCLUSIVE)
      code = 1;
    else if (myKind == Kind.EXCLUSIVE)
      code = -1;
    else
      code = 0;
    config.setSetting(KIND_SETTING, code);
    List<Configuration> subsets = config.getAllSubsets(FILTER_SETTING);
    for (Configuration subset : subsets) {
      subset.removeMe();
    }
    for (StringFilter filter : myFilters) {
      filter.writeTo(config.createSubset(FILTER_SETTING));
    }
  }

  public static boolean isFiltering(StringFilterSet filter) {
    return filter != null && filter.getKind() != Kind.ALL;
  }

  public static enum Kind {
    ALL,
    INCLUSIVE,
    EXCLUSIVE
  }
}
