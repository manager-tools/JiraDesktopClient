package com.almworks.util.datetime;

import com.almworks.integers.AbstractLongIterator;
import com.almworks.integers.LongIterable;
import com.almworks.integers.LongIterator;
import com.almworks.util.CantGetHereException;
import com.almworks.util.Env;
import com.almworks.util.collections.Convertor;
import com.almworks.util.concurrent.IncrementalComputationLongCache;
import com.almworks.util.text.parser.ParseException;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.text.*;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtil {
  public static final CustomDateFormat _LOCAL_TIME = new CustomDateFormat(DateFormat.getTimeInstance(DateFormat.SHORT), "alm.format.time");
  public static final CustomDateFormat _LOCAL_DATE = new CustomDateFormat(DateFormat.getDateInstance(DateFormat.SHORT), "alm.format.date");
  // specifically locale-dependent formats
  public static final DateFormat LOCAL_TIME = _LOCAL_TIME;
  public static final DateFormat LOCAL_DATE = _LOCAL_DATE;
  public static final DateFormat LOCAL_DATE_TIME = new CustomDateTimeFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT));

  // used for textual messages in some places
  public static final DateFormat US_MONTH_DAY = new SimpleDateFormat("MMM dd", Locale.US);
  public static final DateFormat US_MEDIUM = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
  public static final DateFormat US_FULL = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.US);
  public static final DateFormat US_HOURS_MINUTES = new SimpleDateFormat("HH:mm", Locale.US);

  public static final Set<String> MINUTES = Collections15.hashSet("m", "min", "mins", "minute", "minutes");
  public static final Set<String> HOURS = Collections15.hashSet("h", "hr", "hrs", "hour", "hours");
  public static final Set<String> DAYS = Collections15.hashSet("d", "days", "day", "ds");
  public static final Set<String> WEEKS = Collections15.hashSet("w", "wk", "week", "wks", "weeks");

  public static final Convertor<Date, String> TO_LOCAL_DATE_TIME = new Convertor<Date, String>() {
    public String convert(Date value) {
      //noinspection ConstantConditions
      return value != null ? DateUtil.toLocalDateTime(value) : null;
    }
  };
  private static final String DATE_NOT_AVAILABLE = "n/a";
  
  private static final ConcurrentHashMap<String, IncrementalComputationLongCache>[] DAY_STARTS_BY_TZ;
  static {
    DAY_STARTS_BY_TZ = new ConcurrentHashMap[2];
    // For days after the Epoch and the Epoch
    DAY_STARTS_BY_TZ[0] = new ConcurrentHashMap<String, IncrementalComputationLongCache>();
    // For days before the Epoch
    DAY_STARTS_BY_TZ[1] = new ConcurrentHashMap<String, IncrementalComputationLongCache>();
  }

  @Contract("!null,_ -> !null")
  public static String toLocalString(Date date, @NotNull ShowTime showTime) {
    if(date == null) {
      return null;
    }
    switch(showTime) {
      case NEVER: return DateUtil.toLocalDate(date);
      case IF_EXISTS: return DateUtil.toLocalDateAndMaybeTime(date, null);
      case ALWAYS: return DateUtil.toLocalDateTime(date);
    }
    throw new CantGetHereException();
  }

  public static String toLocalDateTime(@NotNull Date date) {
    // new date to save original date from modification
    return LOCAL_DATE_TIME.format(new Date(date.getTime()));
  }

  public static String toLocalDate(@NotNull Date date) {
    // new date to save original date from modification
    return LOCAL_DATE.format(new Date(date.getTime()));
  }

  public static String toLocalDateOrTime(@NotNull Date value) {
    long millis = value.getTime();
    DateFormat format = isSameDay(millis, System.currentTimeMillis()) ? LOCAL_TIME : LOCAL_DATE;
    return format.format(new Date(millis));
  }

  @Contract("!null, _ -> !null")
  public static String toLocalDateAndMaybeTime(Date value, @Nullable TimeZone tz) {
    if (value == null)
      return "";
    if (tz == null)
      tz = TimeZone.getDefault();
    assert tz != null;
    long dateUtc = value.getTime();
    int offset = tz.getOffset(dateUtc);
    long timeLocal = (dateUtc + offset) % Const.DAY;
    DateFormat format = timeLocal == 0 ? LOCAL_DATE : LOCAL_DATE_TIME;
    return format.format(new Date(dateUtc));
  }

  public static String toFriendlyDateTime(@Nullable Date date) {
    return toFriendlyDateTime(date, null, false);
  }

  public static String toFriendlyDateTime(@Nullable Date date, @Nullable DateFormat fullFormat, boolean ago) {
    if (date == null)
      return DATE_NOT_AVAILABLE;
    long millis = date.getTime();
    if (millis <= 0)
      return DATE_NOT_AVAILABLE;

    long now = System.currentTimeMillis();
    long diff = now - millis;
    if (ago && diff >= -30 * Const.SECOND) {
      if (diff < Const.MINUTE) {
        return "<1 min ago";
      } else if (diff < Const.HOUR) {
        int mins = (int) (diff / Const.MINUTE);
        return mins + " min ago";
      } else if (diff < Const.DAY) {
        int hrs = (int) (diff / Const.HOUR);
        return hrs + (hrs > 1 ? " hrs" : " hr") + " ago";
      }
    }
    // using temp to avoid changing date
    Date temp = new Date(millis);
    if (isSameDay(millis, now))
      return "today at " + LOCAL_TIME.format(temp);
    else
      return "on " + Util.NN(fullFormat, LOCAL_DATE_TIME).format(temp);
  }

  public static boolean isSameDay(long millisA, long millisB) {
    TimeZone tz = TimeZone.getDefault();
    return toDayNumberFromInstant(millisA, tz) == toDayNumberFromInstant(millisB, tz);
  }
  
  public static boolean isSameDay(Date instantA, Date instantB) {
    return isSameDay(instantA.getTime(), instantB.getTime());
  }

  public static String getFriendlyDuration(int seconds, boolean showZeroHours) {
    return getFriendlyDuration(seconds, showZeroHours, false);
  }

  public static String getFriendlyDuration(int seconds, boolean showZeroHours, boolean twoDigitMinutes) {
    if(seconds < 0) {
      return "";
    }

    final int hours = seconds / 3600;
    final int minutes = (seconds % 3600) / 60;
    if(hours == 0 && minutes == 0) {
      return "0h";
    }

    final StringBuilder b = new StringBuilder();

    if(hours > 0 || showZeroHours) {
      b.append(hours);
      b.append('h');
    }

    if(minutes > 0 || twoDigitMinutes) {
      if(b.length() > 0) {
        b.append(' ');
      }
      if(twoDigitMinutes && minutes < 10) {
        b.append('0');
      }
      b.append(minutes);
      b.append('m');
    }
    
    return b.toString();
  }

  public static String getHoursDurationFixed(int seconds) {
    if (seconds <= 0)
      return "0.0";
    if (seconds < 360)
      return "<0.1";
    int hours = seconds / 3600;
    int decihours = (seconds - hours * 3600) / 360;
    return hours + "." + decihours;
  }

  public static String getFriendlyDurationVerbose(int seconds) {
    if (seconds < 0)
      return "";
    if (seconds == 0)
      return "0";
    int s = seconds;
    int days = s / 86400;
    s = s % 86400;
    int hours = s / 3600;
    s = s % 3600;
    int mins = s / 60;
    StringBuilder b = new StringBuilder();
    if (days > 0) {
      b.append(days).append('d');
    }
    if (hours > 0 || (days > 0 && mins > 0)) {
      if (b.length() > 0)
        b.append(' ');
      b.append(hours).append('h');
    }
    if (mins > 0) {
      if (b.length() > 0)
        b.append(' ');
      b.append(mins).append('m');
    }
    return b.toString();
  }

  public static int parseDuration(String s, boolean allowZero) throws ParseException {
    if (s == null)
      throw new ParseException("null");
    List<Object> input = groupDurationInput(s);
    if (input.isEmpty())
      throw new ParseException("no data");
    int totalSeconds = 0;
    BigDecimal n = null;
    int mask = 0;
    for (Object o : input) {
      if (o instanceof BigDecimal) {
        if (n != null) throw new ParseException("two numbers in a row " + n + " " + o);
        n = (BigDecimal) o;
      } else if (o instanceof String) {
        if (n == null) throw new ParseException("no number for " + o);
        String unit = (String) o;
        int multiplier;
        if (MINUTES.contains(unit)) {
          if ((mask & 1) != 0) throw new ParseException("minutes?");
          mask |= 1;
          multiplier = 60;
        } else if (HOURS.contains(unit)) {
          if ((mask & 2) != 0) throw new ParseException("hours?");
          mask |= 2;
          multiplier = 3600;
        } else if (DAYS.contains(unit)) {
          if ((mask & 4) != 0) throw new ParseException("days?");
          mask |= 4;
          multiplier = 3600 * 8;
        } else if (WEEKS.contains(unit)) {
          if ((mask & 8) != 0) throw new ParseException("weeks?");
          mask |= 8;
          multiplier = 3600 * 8 * 5;
        } else {
          throw new ParseException("what's " + unit + "?");
        }
        totalSeconds += n.multiply(new BigDecimal(multiplier)).intValue();
        n = null;
      } else {
        assert false : o;
      }
    }
    if (n != null) {
      if (input.size() == 1) {
        // hours by default
        totalSeconds += n.multiply(new BigDecimal(3600)).intValue();
      } else {
        throw new ParseException(n + " what?");
      }
    }
    if (totalSeconds < 0) {
      throw new ParseException(s);
    }
    if (totalSeconds < 60 && !allowZero) {
      throw new ParseException(s);
    }
    return totalSeconds;
  }

  private static List<Object> groupDurationInput(String s) throws ParseException {
    s = s.replace(',', '.').toLowerCase(Locale.US);
    List<Object> input = Collections15.arrayList(6);
    int lasttype = 0;
    int from = 0;
    int len = s.length();
    for (int i = 0; i <= len; i++) {
      char c = i == len ? 0 : s.charAt(i);
      int type;
      if (Character.isWhitespace(c) || c == 0) {
        type = 0;
      } else if (Character.isDigit(c) || c == '.') {
        type = 1;
      } else {
        type = -1;
      }
      if (type != lasttype) {
        if (lasttype < 0) {
          input.add(s.substring(from, i));
        } else if (lasttype > 0){
          String n = s.substring(from, i);
          try {
            input.add(new BigDecimal(n));
          } catch (NumberFormatException e) {
            throw new ParseException("cannot parse " + n);
          }
        }
        from = i;
        lasttype = type;
      }
    }
    return input;
  }

  /**
   * returns seconds
   */
  public static int parseDurationOld(String s, boolean allowZero) throws ParseException {
    if (s == null)
      throw new ParseException("null");
    try {
      Pattern p = Pattern.compile("\\s*([0-9\\.,]*)\\s*(h[a-z]*)?\\s*([0-9]*)\\s*(m[a-z]*)?\\s*");
      Matcher m = p.matcher(s);
      if (!m.matches())
        throw new ParseException(s);

      String n1 = m.group(1);
      String n2 = m.group(3);
      boolean hoursMatched = m.group(2) != null;
      boolean minsMatched = m.group(4) != null;

      if (n1.length() == 0 && n2.length() == 0)
        throw new ParseException(s);
      n1 = n1.replace(',', '.');

      if (n1.indexOf('.') >= 0 && n2.length() != 0)
        throw new ParseException(s + " (fractional hours and minutes)");

      if (n1.length() > 0 && !hoursMatched && n2.length() > 0)
        throw new ParseException(s);

      if (minsMatched && !hoursMatched) {
        // only minutes
        n2 = n1;
        n1 = "";
      }

      int seconds = 0;

      if (n1.length() > 0) {
        BigDecimal dec = new BigDecimal(n1);
        seconds += dec.multiply(new BigDecimal(3600)).intValue();
      }

      if (n2.length() > 0) {
        int minutes = Integer.parseInt(n2);
        if (minutes >= 60)
          throw new ParseException(s);
        seconds += minutes * 60;
      }

      if (seconds < 0) {
        throw new ParseException(s);
      }
      if (seconds < 60 && !allowZero) {
        throw new ParseException(s);
      }
      return seconds;
    } catch (NumberFormatException e) {
      throw new ParseException(s);
    } catch (ArithmeticException e) {
      throw new ParseException(s);
    }
  }

  @Nullable("if the day differs from the current day more than maxDays")
  public static String toRelLocalDate(int day, int maxDays) {
    int currentDay = toDayNumberFromInstant(new Date());
    int diff = currentDay - day;
    boolean future = diff < 0;
    diff = Math.abs(diff);
    if (diff >= maxDays) return null;
    if (diff == 0) return "Today";
    if (diff == 1) return future ? "Tomorrow" : "Yesterday";
    String strDay = diff > 1 ? " days" : " day";
    return future ? "in " + diff + strDay : diff + strDay + " ago";
  }

  /**
   * Returns start of day {@code d} in the specified time zone such that {@code t} denotes a moment of time during this day. <Br/>
   * Start of the day {@code s} is the least timestamp such that {@link #toDayNumberFromInstant(java.util.Date) toDayNumberFromInstant(s)} {@code = d}.
   */
  public static long toDayStart(long t, TimeZone tz) {
    return getDayStart(toDayNumberFromInstant(t, tz), tz);
  }

  /** @return {@link #toDayStart(long, java.util.TimeZone) toDayStart} in the default time zone */
  public static long toDayStart(long t) {
    return toDayStart(t, TimeZone.getDefault());
  }

  /**
   * Returns end of day {@code d} in the specified time zone such that {@code t} denotes a moment of time during this day. <Br/>
   * End of the day {@code s} is the greatest timestamp such that {@link #toDayNumberFromInstant(java.util.Date) toDayNumberFromInstant(s)} {@code = d}.
   */
  public static long toDayEnd(long t, TimeZone tz) {
    return getDayStart(toDayNumberFromInstant(t, tz) + 1, tz) - 1L;
  }

  /** @return {@link #toDayEnd(long, java.util.TimeZone) toDayEnd} in the default time zone */
  public static long toDayEnd(long t) {
    return toDayEnd(t, TimeZone.getDefault());
  }

  /** @return {@link #toInstantOnDay(int, java.util.TimeZone)} in the default time zone */
  @NotNull
  public static Date toInstantOnDay(int nDaysSince1Jan1970) {
    return toInstantOnDay(nDaysSince1Jan1970, TimeZone.getDefault());
  }

  /**
   * Returns a Date that corresponds to the {@link #toDayStart(long, java.util.TimeZone) start of the day} in the specified time zone.
   *
   * <p>This method strives to return midnight of that day in the specified time zone, however, it is not guaranteed, because
   * due to transitions (either regular like biennial switch to Daylight Saving Time or one-time offset change)
   * there may be no midnight on the specified date, and 23:59 of the previous day is followed by, say, 1:00 of the specified date.</p>
   * <p>Generally, it should not be relied upon that the returned Date represents midnight in the specified time zone.</p>
   * <p>As an example of a non-obvious behaviour related to it, see also this
   * <a href="http://wiki.almworks.com/display/process/java.util.GregorianCalendar.clear%28%29+may+set+HOUR_OF_DAY+and+HOUR+to+non-zero">case study in our Wiki</a>.</p>
   *
   * @param nDaysSince1Jan1970 number of the day counting from Jan 1, 1970. E.g., 0 is Jan 1, 1970; 15400 is Mar 1, 2012.
   * @return a Date representing an instant (moment of time) that in the specified time zone corresponds to the day number {@code nDaysSince1Jan1970}, counting from Jan 1, 1970.
   * @see #toDayStart(long)
   * */
  @NotNull
  public static Date toInstantOnDay(int nDaysSince1Jan1970, @NotNull TimeZone tz) {
    return new Date(getDayStart(nDaysSince1Jan1970, tz));
  }

  /** This method uses pre-computed timestamps of midnights for each day instead of {@link Calendar#add Calendar.add(DATE, ...)} because
   * {@code Calendar.add()} doesn't account for changes to time&nbsp;zone offsets made in between. <br/>
   * To see the examples of such changes, see {@code DateUtilTests.testToInstantOnDay()}.
   * */
  public static long getDayStart(int nDaysSince1Jan1970, @NotNull TimeZone tz) {
    IncrementalComputationLongCache daysCache = getDayTimestampCache(tz, nDaysSince1Jan1970 >= 0);
    return daysCache.getOrComputeUpTo(nDaysSince1Jan1970 >= 0 ? nDaysSince1Jan1970 : -nDaysSince1Jan1970);
  }

  private static IncrementalComputationLongCache getDayTimestampCache(TimeZone tz, boolean positive) {
    String tzId = tz.getID();
    ConcurrentHashMap<String, IncrementalComputationLongCache> tzMap = DAY_STARTS_BY_TZ[positive ? 0 : 1];
    IncrementalComputationLongCache daysCache = tzMap.get(tzId);
    if (daysCache == null) {
      DayStartComputation fun = new DayStartComputation(tz, positive ? 1 : -1);
      int jan1_1970 = -tz.getOffset(0);
      IncrementalComputationLongCache newDaysCache = new IncrementalComputationLongCache(fun, jan1_1970);
      daysCache = tzMap.putIfAbsent(tzId, newDaysCache);
      if (daysCache == null) daysCache = newDaysCache;
    }
    return daysCache;
  }

  /** @return {@link #toDayNumberFromInstant(java.util.Date, java.util.TimeZone) toDayNumberFromInstant} in the default time zone. */
  public static int toDayNumberFromInstant(Date instant) {
    return toDayNumberFromInstant(instant, TimeZone.getDefault());
  }

  /**
   * @return how many whole days have passed since Jan 1, 1970. The event "Jan 1, 1970" is interpreted in the specified time zone.
   * Difference in time zone offsets on the Epoch and at the requested moment is accounted for.
   * */
  public static int toDayNumberFromInstant(Date instant, TimeZone tz) {
    return toDayNumberFromInstant(instant.getTime(), tz);
  }

  /** @see #toDayNumberFromInstant(java.util.Date, java.util.TimeZone)  */
  public static int toDayNumberFromInstant(long time, TimeZone tz) {
    int estimate = (int)(time / Const.DAY);
    int day = estimate;
    while (getDayStart(day, tz) > time) day -= 1;
    while (getDayStart(day + 1, tz) <= time) day += 1;
    return day;
  }

  public static int getDaysLeft(long expiration, long now) {
    TimeZone timeZone = new GregorianCalendar().getTimeZone();
    int expiresOffset = timeZone.getOffset(expiration);
    int nowOffset = timeZone.getOffset(now);

    long expiresDay = (expiration + expiresOffset) / Const.DAY;
    long nowDay = (now + nowOffset) / Const.DAY;
    return (int) Math.max(0, expiresDay - nowDay);
  }

  static void clearCaches() {
    for (ConcurrentHashMap<String, IncrementalComputationLongCache> cache : DAY_STARTS_BY_TZ) {
      cache.clear();
    }
  }

  public static enum ShowTime {
    NEVER(1), IF_EXISTS(2), ALWAYS(3);

    private final int myCode;

    ShowTime(int code) {
      myCode = code;
    }

    public int getCode() {
      return myCode;
    }

    public static ShowTime forCode(int code) {
      for(final ShowTime s : values()) {
        if(s.myCode == code) {
          return s;
        }
      }
      return null;
    }
  }

  public static class ToLocalString extends Convertor<Date, String> {
    private final ShowTime myShowTime;

    public ToLocalString(ShowTime showTime) {
      myShowTime = showTime;
    }

    @Override
    public String convert(Date value) {
      return toLocalString(value, myShowTime);
    }
  }
  
  
  private static class DayStartComputation implements IncrementalComputationLongCache.Computation {
    private final TimeZone myTimeZone;
    private final int myInc;
    
    public DayStartComputation(TimeZone timeZone, int inc) {
      myTimeZone = timeZone;
      myInc = inc;
    }

    @Override
    public LongIterable computeRange(final int startIncl, final int endExcl, final long prevResult) {
      return new AbstractLongIterator() {
        @Override
        public boolean hasValue() {
          return true;
        }

        private final Calendar c = GregorianCalendar.getInstance(myTimeZone);
        {
          c.setTimeInMillis(prevResult);
        }
        
        @Override
        public boolean hasNext() throws ConcurrentModificationException {
          return true;
        }

        @Override
        public long value() throws NoSuchElementException {
          return c.getTimeInMillis();
        }

        @Override
        public LongIterator next() {
          c.add(Calendar.DATE, myInc);
          c.set(Calendar.HOUR, 0);
          c.set(Calendar.MINUTE, 0);
          c.set(Calendar.SECOND, 0);
          c.set(Calendar.MILLISECOND, 0);
          return this;
        }
      };
    }
  }

  private static class CustomDateFormat extends DateFormat {
    @NotNull
    private final DateFormat myDefaultFormat;
    private final String myOption;
    private volatile String myLastPattern;
    private DateFormat myDateFormat;
    private boolean myCustom = false;

    public CustomDateFormat(@NotNull DateFormat defaultFormat, String option) {
      Locale locale = Locale.getDefault(Locale.Category.FORMAT);
      numberFormat = (NumberFormat) NumberFormat.getIntegerInstance(locale).clone();
      calendar = Calendar.getInstance(TimeZone.getDefault(), locale);
      myDefaultFormat = defaultFormat;
      myDateFormat = defaultFormat;
      myOption = option;
      updateFormat();
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
      return getFormat().format(date, toAppendTo, fieldPosition);
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
      return getFormat().parse(source, pos);
    }

    private void updateFormat() {
      String pattern = Env.getString(myOption);
      if (Objects.equals(myLastPattern, pattern)) return;
      synchronized (this) {
        myLastPattern = pattern;
        pattern = Util.NN(pattern).trim();
        if (pattern.isEmpty()) {
          myDateFormat = myDefaultFormat;
          myCustom = false;
          return;
        }
        SimpleDateFormat format;
        try {
          format = new SimpleDateFormat(pattern, Locale.US);
          myDateFormat = format;
          myCustom = true;
        } catch (Exception e) {
          myDateFormat = myDefaultFormat;
          myCustom = false;
        }
      }
    }

    @NotNull
    private DateFormat getFormat() {
      updateFormat();
      synchronized (this) {
        return myDateFormat;
      }
    }

    @Nullable
    private DateFormat getCustomFormat() {
      updateFormat();
      synchronized (this) {
        return myCustom ? myDateFormat : null;
      }
    }
  }

  private static class CustomDateTimeFormat extends DateFormat {
    private final DateFormat myDefault;

    public CustomDateTimeFormat(DateFormat aDefault) {
      Locale locale = Locale.getDefault(Locale.Category.FORMAT);
      numberFormat = (NumberFormat) NumberFormat.getIntegerInstance(locale).clone();
      calendar = Calendar.getInstance(TimeZone.getDefault(), locale);
      myDefault = aDefault;
    }

    @Override
    public StringBuffer format(Date d, StringBuffer toAppendTo, FieldPosition fieldPosition) {
      DateFormat date = _LOCAL_DATE.getCustomFormat();
      DateFormat time = _LOCAL_TIME.getCustomFormat();
      if (date == null && time == null) return myDefault.format(d, toAppendTo, fieldPosition);
      LOCAL_DATE.format(d, toAppendTo, fieldPosition);
      toAppendTo.append(" ");
      LOCAL_TIME.format(d, toAppendTo, fieldPosition);
      return toAppendTo;
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
      DateFormat date = _LOCAL_DATE.getCustomFormat();
      DateFormat time = _LOCAL_TIME.getCustomFormat();
      if (date == null && time == null) return myDefault.parse(source, pos);
      int start = pos.getIndex();
      DateFormat dateFormat = (DateFormat) _LOCAL_DATE.getFormat().clone();
      dateFormat.setCalendar(calendar);
      dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
      Date parsedDate = dateFormat.parse(source, pos);
      if (pos.getIndex() == start || parsedDate == null) return null;
      start = pos.getIndex();
      Date parsedTime = _LOCAL_TIME.getFormat().parse(source, pos);
      if (pos.getIndex() == start || parsedTime == null) return null;
      return new Date(parsedDate.getTime()  + parsedTime.getTime());
    }
  }
}
