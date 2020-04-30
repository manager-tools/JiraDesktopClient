package com.almworks.items.gui.edit.editors.scalar;

import com.almworks.util.components.ADateField;
import com.almworks.util.datetime.DateUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class DayEditorKind implements DateEditorKind<Integer> {
  @Override
  public Integer createModelValue(@Nullable Date date) {
    return date == null ? null : DateUtil.toDayNumberFromInstant(date);
  }

  @Override
  public Integer processForCommit(@Nullable Integer modelValue) {
    return modelValue;
  }

  @Override
  public Date toDate(@Nullable Integer value) {
    return value == null ? null : DateUtil.toInstantOnDay(value);
  }

  @Override
  public String format(@NotNull Integer value) {
    return DateUtil.toLocalDate(toDate(value));
  }

  @NotNull
  @Override
  public ADateField.Precision getEditorPrecision() {
    return ADateField.Precision.DAY;
  }
}
