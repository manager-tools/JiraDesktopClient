package com.almworks.util.text;

import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SmartDateParser {
  private final List<ThreadLocal<Format>> myFormats = Collections15.arrayList();
  private boolean myIgnoreInvalidTimezones;
  private long myLastWarningAboutBadTimestamp;

  public SmartDateParser(String... formats) {
    this(null, formats);
  }
  
  public SmartDateParser(@Nullable Locale locale, String... formats) {
    assert formats != null;
    assert formats.length != 0;
    for (int i = 0; i < formats.length; i++) {
      String format = formats[i];
      addSimpleDateFormat(format, locale);
    }
  }

  public SmartDateParser setIgnoreInvalidTimezones(boolean ignore) {
    myIgnoreInvalidTimezones = ignore;
    return this;
  }

  private synchronized void addSimpleDateFormat(final String format, final @Nullable Locale locale) {
    ThreadLocal<Format> formatterHolder = new ThreadLocal<Format>() {
      public Format initialValue() {
        return new Format(format, locale);
      }
    };
    // try for this thread
    Format formatter = null;
    try {
      formatter = formatterHolder.get();
    } catch (RuntimeException e) {
      Log.error("cannot initialize format " + format);
      return;
    }
    if (formatter != null) {
      myFormats.add(formatterHolder);
    } else {
      Log.error("null format for " + format);
    }
  }

  public Date parse(String date, Date defaultValue, @Nullable TimeZone defaultTimeZone,
    String warningPrefix)
  {
    Date result = parse(date, null, defaultTimeZone);
    if (result == null) {
      Log.warn(warningPrefix + " [" + date + "]");
      return defaultValue;
    }
    return result;
  }

  public synchronized Date parse(String date) throws ParseException {
    Date result = parse(date, null, null);
    if (result == null)
      throw new ParseException(date, 0);
    return result;
  }

  // todo bottleneck!
  // cache dates?
  // #1600
  public synchronized Date parse(String date, Date defaultValue, @Nullable TimeZone defaultZone) {
    if (date == null)
      return defaultValue;
    date = date.trim();
    date = removeMicroseconds(date);
    Date result = parseDate(date, defaultZone);
    if (myIgnoreInvalidTimezones && result == null) {
      if (defaultZone == null) {
        defaultZone = TimeZone.getDefault();
      }
      //noinspection ConstantConditions
      result = parseDateReplaceTimezone(date, defaultZone);
      if (result != null) {
        long now = System.currentTimeMillis();
        if (now - myLastWarningAboutBadTimestamp > 10000) {
          myLastWarningAboutBadTimestamp = now;
          Log.warn("cannot parse date [" + date + "], parsed successfully by ignoring timestamp");
        }
      }
    }
    return result == null ? defaultValue : result;
  }

  private Date parseDateReplaceTimezone(String date, @NotNull TimeZone zone) {
    for (ThreadLocal<Format> myFormat : myFormats) {
      Format format = myFormat.get();
      DateFormat prefixFormat = format.getPrefixFormat();
      if (prefixFormat == null)
        continue;
      if (!Util.equals(prefixFormat.getTimeZone(), zone)) {
        prefixFormat.setTimeZone(zone);
      }
      ParsePosition pp = new ParsePosition(0);
      Date result = prefixFormat.parse(date, pp);
      if (result != null && pp.getErrorIndex() < 0) {
        return result;
      }
    }
    return null;
  }

  @Nullable
  private Date parseDate(String date, @Nullable TimeZone defaultZone) {
    // todo - either remove thread local holders because we already syunchronized, or remove synchronized
    for (ThreadLocal<Format> myFormat : myFormats) {
      Format format = myFormat.get();
      TimeZone[] zone = {defaultZone};
      String adjustedDate = expandTimezone(date, format, zone); // performance!
      format.setZone(zone[0]);
      ParsePosition pos = new ParsePosition(0);
      Date result = format.getFormat().parse(adjustedDate, pos);
      if (result != null && pos.getErrorIndex() < 0 && pos.getIndex() == adjustedDate.length()) {
        // parsed whole string
        return result;
      }
    }
    return null;
  }

  private String removeMicroseconds(String date) {
    int k = date.lastIndexOf('.');
    if (k < 0)
      return date;
    int l = date.length();
    for (int i = k + 1; i < l; i++) {
      if (!Character.isDigit(date.charAt(i)))
        return date;
    }
    // we made sure that date ends with ".1923921"
    return date.substring(0, k);
  }

  private String expandTimezone(String date, Format format, @NotNull TimeZone[] zone) {
    int tzs = format.findTimezone(date);
    if (tzs < 0)
      return date;
    int tze = format.findTimezoneEnd(date, tzs);
    if (tze <= tzs)
      return date;

    String timezone = date.substring(tzs, tze);
    if ("GMT".equalsIgnoreCase(timezone) || "UTC".equalsIgnoreCase(timezone)) {
      zone[0] = TimeZone.getTimeZone("GMT");
      return date;
    }

    String prefix = date.substring(0, tzs);
    String suffix = date.substring(tze);
    String rfcTimezone = format.getRfcTimezone(date);
    String timezoneReplacement = processTimezone(timezone, prefix, suffix, format, zone, rfcTimezone);

    // here: timezoneReplacement == null; prefix contains date, zone[0] contains default zone

    return timezoneReplacement == null ? date : prefix + timezoneReplacement + suffix;
  }

  /**
   * @param format gets modified with appropriate timezone
   * @return replacement for timezone or null if no replacement required
   */
  @Nullable
  private String processTimezone(String timezone, String prefix, String suffix, Format format, TimeZone[] zoneResult,
    @Nullable String rfcTimezone)
  {
    TimeZone zone = null;
    String result = null;

    char firstChar = timezone.charAt(0);
    if (Character.isLetter(firstChar)) {
      if (!Character.isLetter(timezone.charAt(timezone.length() - 1))) {
        // check for GMT+-N
        result = fixGmt(timezone);
      } else {
        ParsePosition pp = new ParsePosition(0);
        String gmtDate = prefix + "GMT" + suffix;
        Date dateAsGmt = format.getFormat().parse(gmtDate, pp); // performance!
        if (dateAsGmt != null && pp.getIndex() == gmtDate.length() && pp.getErrorIndex() < 0) {
          TimeZone[] z = {null};
          result = TimezoneExpansion.convertTimezone(timezone, dateAsGmt, z, rfcTimezone);
          if (result != null) {
            zone = z[0];
          }
        }
      }
    } else if (Character.isDigit(firstChar) || firstChar == '-' || firstChar == '+') {
      // convert from RFC 'Z' to SimpleDateFormat 'z'
      if (firstChar != '+' && firstChar != '-') {
        timezone = '+' + timezone;
      }
      if (timezone.length() == 5) {
        if ("0000".equals(timezone.substring(1))) {
          zoneResult[0] = TimeZone.getTimeZone("GMT");
          return "GMT";
        } else {
          StringBuffer fix = new StringBuffer("GMT");
          fix.append(timezone.substring(0, 3));
          fix.append(':');
          fix.append(timezone.substring(3));
          result = fix.toString();
        }
      }
    }

    if (zone == null && result != null) {
      zone = TimeZone.getTimeZone(result);
      if ("GMT".equalsIgnoreCase(zone.getID())) {
        zone = null;
      }
    }

    if (zone == null) {
      zone = TimeZone.getTimeZone(timezone);
      if ("GMT".equalsIgnoreCase(zone.getID())) {
        zone = null;
      }
    }

    if (zone != null) {
      zoneResult[0] = zone;
    }

    return result;
  }

  private String fixGmt(String timezone) {
    timezone = Util.upper(timezone);
    int length = timezone.length();
    if (length < 5)
      return null;
    if (timezone.charAt(0) != 'G' || timezone.charAt(1) != 'M' || timezone.charAt(2) != 'T')
      return null;
    char sign = timezone.charAt(3);
    if (sign != '+' && sign != '-')
      return null;
    StringBuffer hours = new StringBuffer();
    StringBuffer mins = new StringBuffer();
    boolean hrs = true;
    for (int i = 4; i < length; i++) {
      char c = timezone.charAt(i);
      if (c == ':') {
        if (hrs)
          hrs = false;
        else
          return null;
      } else if (Character.isDigit(c)) {
        if (hours.length() == 2 && hrs)
          hrs = false;
        if (hrs)
          hours.append(c);
        else
          mins.append(c);
      } else {
        return null;
      }
    }
    if (hours.length() > 2 || mins.length() > 2)
      return null;
    while (hours.length() < 2)
      hours.insert(0, '0');
    while (mins.length() < 2)
      mins.insert(0, '0');
    return "GMT" + sign + hours + ":" + mins;
  }

  private static class Format {
    private final SimpleDateFormat myFormat;

    @Nullable
    private SimpleDateFormat myPrefixFormat;
    private boolean myPrefixCalculated;

    @Nullable
    private SimpleDateFormat myRfcPrefixFormat;
    private boolean myRfcPrefixCalculated;

    public Format(String format, @Nullable Locale locale) {
      myFormat = new SimpleDateFormat(format, Util.NN(locale, Locale.US));
      myFormat.setLenient(true);
    }

    public SimpleDateFormat getFormat() {
      return myFormat;
    }

    public int findTimezone(String date) { ///performance!
      DateFormat prefixFormat = getPrefixFormat();
      if (prefixFormat == null)
        return -1;
      ParsePosition pp = new ParsePosition(0);
      Date r = prefixFormat.parse(date, pp);///performance!
      return r != null && pp.getErrorIndex() < 0 ? pp.getIndex() : -1;
    }

    @Nullable
    public DateFormat getPrefixFormat() {
      if (!myPrefixCalculated) {
        myPrefixCalculated = true;
        myPrefixFormat = calculatePrefixFormat('z');
      }
      return myPrefixFormat;
    }

    @Nullable
    public DateFormat getRfcPrefixFormat() {
      if (!myRfcPrefixCalculated) {
        myRfcPrefixCalculated = true;
        myRfcPrefixFormat = calculatePrefixFormat('Z');
      }
      return myRfcPrefixFormat;
    }

    private SimpleDateFormat calculatePrefixFormat(char formatChar) {
      String pattern = myFormat.toPattern();
      int k = pattern.indexOf(formatChar);
      if (k >= 0) {
        int p = pattern.indexOf(formatChar, k + 1);
        if (p >= 0) {
          assert false : pattern;
        } else {
          String subpattern = pattern.substring(0, k);
          try {
            return new SimpleDateFormat(subpattern, Locale.US);
          } catch (RuntimeException e) {
            Log.debug("bad subformat " + subpattern, e);
          }
        }
      }
      return null;
    }

    public int findTimezoneEnd(String date, int start) {
      // hack: support only timezone at the end, possibly with non-letter and non-digit characters
      if (start < 0)
        return -1;
      int end = date.length() - 1;

      // ignore trailing spaces
      for (; end >= start && Character.isSpace(date.charAt(end)); end--) {
      }

      // ignore trailing non-letters, non-digits, and non-timezone symbols
      for (; end >= start && !isTimezoneSymbol(date.charAt(end)); end--) {
      }

      return end + 1;
    }

    private boolean isTimezoneSymbol(char c) {
      return Character.isLetter(c) || Character.isDigit(c) || c == '/' || c == '-' || c == ':' || c == '+' || c == '_';
    }

    public String toString() {
      return myFormat.toPattern();
    }

    public void setZone(TimeZone zone) {
      if (zone == null)
        zone = TimeZone.getDefault();
      if (!Util.equals(myFormat.getTimeZone(), zone)) {
        myFormat.setTimeZone(zone);
      }
    }

    @Nullable
    public String getRfcTimezone(String date) {
      DateFormat format = getRfcPrefixFormat();
      if (format == null)
        return null;
      ParsePosition pp = new ParsePosition(0);
      Date r = format.parse(date, pp);
      if (r == null || pp.getErrorIndex() >= 0)
        return null;
      int start = pp.getIndex();
      if (start + 5 > date.length())
        return null;
      String result = date.substring(start, start + 5);
      return result;
    }
  }
}
