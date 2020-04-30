package com.almworks.jira.provider3.custom.impl;

import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumDescriptor;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumKind;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

abstract class MigrationChecker<T> {
  private static final MigrationChecker<Map<TypedKey<?>, ?>> MAP_CHECKER = new MigrationChecker<Map<TypedKey<?>, ?>>() {
    @Override
    protected boolean checkCanMigrate(Map<TypedKey<?>, ?> fromValue, Map<TypedKey<?>, ?> toValue) {
      return canMigrate(fromValue, toValue);
    }
  };
  private static final MigrationChecker<Object> ALWAYS_TRUE = new MigrationChecker<Object>() {
    @Override
    protected boolean checkCanMigrate(Object fromValue, Object toValue) {
      return true;
    }
  };

  private static final Map<TypedKey<?>, MigrationChecker<?>> CHECKERS = Collections15.hashMap();
  static {
    CHECKERS.put(ConfigKeys.EDITABLE, MAP_CHECKER);
    CHECKERS.put(ConfigKeys.UPLOAD_JSON, ALWAYS_TRUE);
    CHECKERS.put(EnumKind.JSON_ENUM_PARSER, ALWAYS_TRUE);
    CHECKERS.put(ConfigKeys.NO_REMOTE_SEARCH, ALWAYS_TRUE);
    CHECKERS.put(EnumDescriptor.LOAD_FULL_SET, ALWAYS_TRUE);
  }

  static boolean canMigrate(Map<TypedKey<?>, ?> from, Map<TypedKey<?>, ?> to) {
    Set<TypedKey<?>> toOnly = Collections15.hashSet(to.keySet());
    for (TypedKey<?> key : from.keySet()) {
      toOnly.remove(key);
      if (!canMigrate(key, from, to)) return false;
    }
    for (TypedKey<?> key : toOnly) if (!canMigrate(key, null, to)) return false;
    return true;
  }

  private static <T> boolean canMigrate(TypedKey<T> key, @Nullable Map<TypedKey<?>, ?> from, @Nullable Map<TypedKey<?>, ?> to) {
    T fromValue = from == null ? null : key.getFrom(from);
    T toValue = to == null ? null : key.getFrom(to);
    if (Util.equals(fromValue, toValue)) return true;
    MigrationChecker<T> checker = getChecker(key);
    if (checker == null)
      return false;
    //noinspection RedundantIfStatement
    if (checker.checkCanMigrate(fromValue, toValue)) return true;
    return false;
  }

  protected abstract boolean checkCanMigrate(T fromValue, T toValue);

  @Nullable("When no one registered")
  private static <T> MigrationChecker<T> getChecker(TypedKey<T> key) {
    //noinspection unchecked
    return (MigrationChecker<T>) CHECKERS.get(key);
  }
}
