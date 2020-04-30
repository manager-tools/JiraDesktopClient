package com.almworks.jira.provider3.sync.download2;

import com.almworks.util.datetime.DateUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.TimeZone;

public class ConversionUtil {
  /** Returns the number of whole days passed in the server time zone since Jan 1, 1970 until the start of the specified day. */
  @Nullable
  public static Integer serverMidnightToDays(@Nullable Date dayStart, @NotNull TimeZone serverTz) {
    if (dayStart != null) {
      int day = DateUtil.toDayNumberFromInstant(dayStart, serverTz);
      // We may be off 1 day in case time zone information is out of date on server or locally.
      // Example: on Mar 1, 2012 tzdata2011l gives offset +4, and tzdata2011l +3. If we calculate day boundary for +3, and server sends us day boundary for +4, we'll get the previous day.
      long instant = dayStart.getTime();
      long diff = instant - DateUtil.getDayStart(day, serverTz);
      long diff1 = DateUtil.getDayStart(day + 1, serverTz) - instant;
      if (diff1 < diff) day = day + 1;
      return day;
    }
    return null;
  }
}
