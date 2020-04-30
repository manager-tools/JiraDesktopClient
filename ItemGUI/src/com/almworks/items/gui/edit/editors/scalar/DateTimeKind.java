package com.almworks.items.gui.edit.editors.scalar;

import com.almworks.util.components.ADateField;
import com.almworks.util.datetime.DateUtil;
import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class DateTimeKind implements DateEditorKind<Date> {
  @Override
  public Date createModelValue(Date date) {
    return date;
  }

  @Override
  public Date processForCommit(@Nullable Date date) {
    if (date == null) return null;
    long time = date.getTime();
    time = (time / Const.SECOND) * Const.SECOND;
    return time <= 0L ? null : new Date(time);
  }

  @Override
  public Date toDate(Date value) {
    return value;
  }

  @Override
  public String format(@NotNull Date value) {
    return DateUtil.LOCAL_DATE_TIME.format(value);
  }

  @NotNull
  @Override
  public ADateField.Precision getEditorPrecision() {
    return ADateField.Precision.DATE_TIME;
  }
}
