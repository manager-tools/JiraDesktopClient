package com.almworks.util;

import com.almworks.util.text.TextUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class DecentFormatter extends Formatter {
  private final QuickDateFormatter myDateFormat = new QuickDateFormatter("yyyyMMdd", '-', (char) 0, '.', true);

  public String format(LogRecord record) {
    StringBuffer result = new StringBuffer(200);
    // Minimize memory allocations here.
    myDateFormat.formatTo(record.getMillis(), result);
    result.append(' ');
    result.append(record.getLevel().getName());
    result.append(' ');
    String message = formatMessage(record);
    if (message != null) {
      message = GlobalLogPrivacy.examineLogString(message);
      result.append(message);
    }
    result.append(TextUtil.LINE_SEPARATOR);
    Throwable thrown = record.getThrown();
    if (thrown != null)
      printStackTrace(thrown, result, 0);
    return result.toString();
  }

  public static void printStackTrace(Throwable thrown, StringBuffer result, int descent) {
    try {
      result.ensureCapacity(1000);
      result.append(thrown.getClass().getName());
      String message = thrown.getLocalizedMessage();
      if (message != null) {
        result.append(':').append(' ');
        result.append(message);
      }
      result.append(TextUtil.LINE_SEPARATOR);

      printStackTrace(thrown.getStackTrace(), result);

      Throwable cause = thrown.getCause();
      if (cause != null) {
        result.append("Caused by: ");
        printStackTrace(cause, result, descent + 1);
      }
      if (descent == 0) {
        result.append("==========================================================================================");
        result.append(TextUtil.LINE_SEPARATOR);
      }
    } catch (Exception ex) {
      // ignore
    }
  }

  public static void printStackTrace(StackTraceElement[] trace, StringBuffer result) {
    if (trace != null) {
      for (int i = 0; i < trace.length; i++) {
        StackTraceElement elem = trace[i];
        result.append("\tat ");
        result.append(elem.getClassName());
        result.append('.');
        result.append(elem.getMethodName());
        if (elem.isNativeMethod()) {
          result.append("(Native Method)");
        } else {
          String fileName = elem.getFileName();
          int lineNumber = elem.getLineNumber();
          if (fileName != null && lineNumber >= 0) {
            result.append('(').append(fileName).append(':').append(lineNumber).append(')');
          } else {
            if (fileName != null)
              result.append('(').append(fileName).append(')');
            else
              result.append("Unknown Source");
          }
        }
        result.append(TextUtil.LINE_SEPARATOR);
      }
    }
  }

  public static void install(String loggerName) {
    DecentFormatter formatter = new DecentFormatter();
    Logger logger = Logger.getLogger(loggerName);
    if (logger != null) {
      Handler[] handlers = logger.getHandlers();
      for (Handler handler : handlers) {
        handler.setFormatter(formatter);
      }
    }
  }

  public static final class QuickDateFormatter {
    private static final char[] ourDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private final SimpleDateFormat myDayFormat;
    private final boolean myOutputMillis;
    private final char myDelimDateTime;
    private final char myDelimMillis;
    private final char myDelimTime;
    private final int myZoneOffset;

    private volatile String myLastDay = "";
    private volatile int myLastDayPoint = 0;

    public QuickDateFormatter(String dayPattern, char delimDateTime, char delimTime, char delimMillis,
      boolean outputMillis)
    {
      myDayFormat = new SimpleDateFormat(dayPattern);
      myDelimDateTime = delimDateTime;
      myDelimTime = delimTime;
      myDelimMillis = delimMillis;
      myOutputMillis = outputMillis;
      TimeZone tz = TimeZone.getDefault();
      myDayFormat.setTimeZone(tz);
      myZoneOffset = tz.getOffset(System.currentTimeMillis());
    }

    public void formatTo(long time, StringBuffer appendTo) {
      long localTime = time + myZoneOffset;
      int utime = (int) (localTime / 1000);
      int millis = (int) (localTime - utime * 1000);
      int day = utime / 86400;
      int intraday = utime - day * 86400;
      if (day != myLastDayPoint) {
        synchronized (this) {
          myLastDayPoint = day;
          myLastDay = myDayFormat.format(new Date(time));
        }
      }
      int hour = intraday / 3600;
      intraday = intraday - hour * 3600;
      int minute = intraday / 60;
      intraday = intraday - minute * 60;
      appendTo.append(myLastDay);
      if (myDelimDateTime != 0)
        appendTo.append(myDelimDateTime);
      appendInt(hour, appendTo, 2);
      if (myDelimTime != 0)
        appendTo.append(myDelimTime);
      appendInt(minute, appendTo, 2);
      if (myDelimTime != 0)
        appendTo.append(myDelimTime);
      appendInt(intraday, appendTo, 2);
      if (myOutputMillis) {
        if (myDelimMillis != 0)
          appendTo.append(myDelimMillis);
        appendInt(millis, appendTo, 3);
      }
    }

    private void appendInt(int v, StringBuffer appendTo, int digitNumber) {
      int digit;
      if (digitNumber >= 3) {
        digit = v / 100;
        if (digit > 9)
          throw new IllegalArgumentException("" + v);
        appendTo.append(ourDigits[digit]);
        v -= digit * 100;
      }
      digit = v / 10;
      if (digit > 9)
        throw new IllegalArgumentException("" + v);
      appendTo.append(ourDigits[digit]);
      v -= digit * 10;
      if (v > 9)
        throw new IllegalArgumentException("" + v);
      appendTo.append(ourDigits[v]);
    }
  }
}
