package com.almworks.items.gui.edit.editors.scalar;

import com.almworks.util.components.ADateField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public interface DateEditorKind<D> {
  @Nullable
  D createModelValue(@Nullable Date date);

  @Nullable
  D processForCommit(@Nullable D modelValue);

  Date toDate(@Nullable D value);

  String format(@NotNull D value);

  @NotNull
  ADateField.Precision getEditorPrecision();

  DateEditorKind<Date> DATE_TIME = new DateTimeKind();
  DateEditorKind<Integer> DAY = new DayEditorKind();
}
