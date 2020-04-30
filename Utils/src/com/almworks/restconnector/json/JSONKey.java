package com.almworks.restconnector.json;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.text.TextUtil;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONKey<T> {
  public static final Convertor<Object, String> TEXT = CastConvertor.create(String.class);
  public static final Convertor<Object, Boolean> BOOLEAN = CastConvertor.create(Boolean.class);
  public static final Convertor<Object, JSONObject> OBJECT = CastConvertor.create(JSONObject.class);
  public static final Convertor<Object, Integer> INTEGER = new Convertor<Object, Integer>() {
    @Override
    public Integer convert(Object value) {
      Integer iValue = Util.castNullable(Integer.class, value);
      if (iValue != null) return iValue;
      Long lValue = Util.castNullable(Long.class, value);
      if (lValue != null) return lValue.intValue();
      String string = TEXT.convert(value);
      if (string == null) return null;
      try {
        return Integer.parseInt(string);
      } catch (NumberFormatException e) {
        LogHelper.error(e, string);
        return null;
      }
    }
  };
  public static final Convertor<Object, Long> LONG = new Convertor<Object, Long>() {
    @Override
    public Long convert(Object value) {
      Integer iValue = Util.castNullable(Integer.class, value);
      if (iValue != null) return iValue.longValue();
      Long lValue = Util.castNullable(Long.class, value);
      if (lValue != null) return lValue;
      String string = TEXT.convert(value);
      if (string == null) return null;
      try {
        return Long.parseLong(string);
      } catch (NumberFormatException e) {
        LogHelper.error(e, string);
        return null;
      }
    }
  };
  public static final Convertor<Object, Date> DATE_TIME = new Convertor<Object, Date>() {
    private final ThreadLocal<SimpleDateFormat> LOCAL_FORMAT = new ThreadLocal<SimpleDateFormat>() {
      @Override
      protected SimpleDateFormat initialValue() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
      }
    };
    /**
     * Fallback format for buggy plugins that does not respect specified date-time format.
     * com.atlassian.jira.ext.charting:firstresponsedate
     */
    private final ThreadLocal<SimpleDateFormat> LOCAL_NO_TIMEZONE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
      @Override
      protected SimpleDateFormat initialValue() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        if (gmt != null) format.setTimeZone(gmt);
        else LogHelper.error("No GMT timezone");
        return format;
      }
    };
    @Override
    public Date convert(Object value) {
      String text = TEXT.convert(value);
      if (text == null) return null;
      text = text.trim();
      if (text.isEmpty()) return null;
      try {
        return LOCAL_FORMAT.get().parse(text);
      } catch (RuntimeException e) {
        LogHelper.error("Wrong date", text, e);
        return null;
      } catch (ParseException e) {
        try {
          return LOCAL_NO_TIMEZONE_FORMAT.get().parse(text);
        } catch (RuntimeException e1) {
          LogHelper.error("Wrong date", text, e1);
          return null;
        } catch (ParseException e1) {
          LogHelper.error("Unexpected JSON date format", text, e1);
          return null;
        }
      }
    }
  };
  public static final Convertor<Object, TimeZone> TIME_ZONE_FROM_TIME = new Convertor<Object, TimeZone>() {
    private final Pattern TIME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}?([+-]\\d{4})");
    @Override
    public TimeZone convert(Object value) {
      String text = TEXT.convert(value);
      if (text == null) return null;
      Matcher m = TIME.matcher(text);
      if (!m.matches()) {
        LogHelper.error("Bad time format", text);
        return null;
      }
      // TimeZone.getTimeZone("GMT-04:25")
      String offset = m.group(1);
      return TimeZone.getTimeZone("GMT" + offset.substring(0, 3) + ":" + offset.substring(3));
    }
  };
  public static final Convertor<Object, TimeZone> TIME_ZONE_ID = new Convertor<Object, TimeZone>() {
    @Override
    public TimeZone convert(Object value) {
      String text = TEXT.convert(value);
      if (text == null) return null;
      TimeZone zone = TimeZone.getTimeZone(text);
      LogHelper.assertWarning(ArrayUtil.indexOf(TimeZone.getAvailableIDs(), text) >= 0, "Unknown timeZone", text);
      return zone;
    }
  };

  public static final Convertor<Object, Date> DATE = new Convertor<Object, Date>() {
    @Override
    public Date convert(Object value) {
      String text = TEXT.convert(value);
      if (text == null) return null;
      return DATE_TIME.convert(text + "T00:00:00.000+0000");
    }
  };
  public static final Convertor<Object, Integer> DAYS_DATE = new Convertor<Object, Integer>() {
    @Override
    public Integer convert(Object value) {
      Date date = DATE.convert(value); // GMT midnight in default local timeZone
      if (date == null) return null;
      return DateUtil.toDayNumberFromInstant(date, TimeZone.getTimeZone("GMT"));
    }
  };
  public static final Convertor<Object, String> TEXT_NN_TRIM = new TextTrim(true);
  public static final Convertor<Object, String> TEXT_TRIM = new TextTrim(null);
  public static final Convertor<Object, String> TEXT_TRIM_TO_NULL = new TextTrim(false);
  public static final Convertor<Object, String> TEXT_INTEGER = new Convertor<Object, String>() {
    @Override
    public String convert(Object value) {
      String text = TEXT.convert(value);
      if (text == null) {
        LogHelper.warning("Expected not-null int", value);
        return null;
      }
      text = text.trim();
      try {
        //noinspection ResultOfMethodCallIgnored
        Integer.parseInt(text);
      } catch (NumberFormatException e) {
        LogHelper.warning("Expected int", value);
        return null;
      }
      return text;
    }
  };

  public static final Convertor<Object, String> TEXT_TRIM_LINES = new TrimLines(false);

  public static final Convertor<Object, String> TEXT_LOWER = new Convertor<Object, String>() {
    @Override
    public String convert(Object value) {
      String text = TEXT.convert(value);
      return text != null ? Util.lower(text) : null;
    }
  };
  public static final Convertor<Object, String> TEXT_OR_INTEGER = new Convertor<Object, String>() {
    @Override
    public String convert(Object value) {
      if (value == null) return null;
      String str = Util.castNullable(String.class, value);
      if (str != null) return str;
      Integer integer = Util.castNullable(Integer.class, value);
      if (integer != null) return integer.toString();
      Long aLong = Util.castNullable(Long.class, value);
      if (aLong != null) return aLong.toString();
      LogHelper.error("Expected string or integer", value);
      return null;
    }
  };

  public static final Convertor<Object, BigDecimal> DECIMAL = new Convertor<Object, BigDecimal>() {
    @Override
    public BigDecimal convert(Object value) {
      Integer integer = Util.castNullable(Integer.class, value);
      if (integer != null) return new BigDecimal(integer);
      Long aLong = Util.castNullable(Long.class, value);
      if (aLong != null) return new BigDecimal(aLong);
      Double aDouble = Util.castNullable(Double.class, value);
      if (aDouble != null) return new BigDecimal(aDouble);
      String text = TEXT_TRIM.convert(value);
      if (text != null && !text.isEmpty()) {
        try {
          // Values are formatted in the neutral locale, don't use serverContext.getLocale()
          return new BigDecimal(text);
        } catch (NumberFormatException e) {
          LogHelper.error(e, text);
        }
      }
      return null;
    }
  };

  public static final Convertor<Object, Boolean> FALSE_TO_NULL = new Convertor<Object, Boolean>() {
    @Override
    public Boolean convert(Object value) {
      Boolean bool = BOOLEAN.convert(value);
      return bool == null || Boolean.TRUE.equals(bool) ? bool : null;
    }
  };

  public static final RootObject ROOT_OBJECT = new RootObject();

  private final String myName;
  private final Convertor<Object, T> myConvertor;

  public JSONKey(String name, Convertor<Object, T> convertor) {
    LogHelper.assertError(convertor != null, name);
    myName = Util.NN(name);
    myConvertor = convertor;
  }

  public T getValue(Object jsonObject) {
    if (jsonObject == null) return null;
    if ("".equals(myName)) return myConvertor.convert(jsonObject);
    JSONObject obj = OBJECT.convert(jsonObject);
    if (obj == null) return null;
    Object value = obj.get(myName);
    try {
      return convertValue(value);
    } catch (RuntimeException e) {
      LogHelper.error("Error converting value", this, myName, value);
      throw e;
    }
  }

  @NotNull
  public T getNotNull(Object jsonObject) throws JSONValueException {
    T value = getValue(jsonObject);
    if (value == null) throw new JSONValueException("Null value: '" + myName + "'");
    return value;
  }

  public boolean hasValue(Object jsonObject) {
    if ("".equals(myName)) return true;
    JSONObject obj = OBJECT.convert(jsonObject);
    return obj != null && obj.containsKey(myName);
  }

  public T convertValue(Object value) {
    return value == null ? null : myConvertor.convert(value);
  }

  public String getName() {
    return myName;
  }

  @Override
  public String toString() {
    return "JSONKey[" + myName + "]";
  }

  public static JSONKey<String> text(String name) {
    return new JSONKey<String>(name, TEXT);
  }

  public static JSONKey<String> textNNTrim(String name) {
    return new JSONKey<String>(name, TEXT_NN_TRIM);
  }

  public static JSONKey<String> textTrimLines(String name) {
    // http://snow:10500/browse/JC-123
    return new JSONKey<String>(name, emptyTextToNull(TEXT_TRIM_LINES));
  }

  public static JSONKey<String> textTrim(String name) {
    return new JSONKey<String>(name, TEXT_TRIM);
  }

  public static JSONKey<String> textLower(String name) {
    return new JSONKey<String>(name, TEXT_LOWER);
  }

  public static JSONKey<String> textOrInteger(String name) {
    return new JSONKey<String>(name, TEXT_OR_INTEGER);
  }

  public static JSONKey<Boolean> bool(String name) {
    return new JSONKey<Boolean>(name, BOOLEAN);
  }

  public static JSONKey<JSONObject> object(String name) {
    return new JSONKey<>(name, OBJECT);
  }

  public static JSONKey<Integer> integer(String name) {
    return new JSONKey<Integer>(name, INTEGER);
  }

  public static JSONKey<Long> longInt(String name) {
    return new JSONKey<Long>(name, LONG);
  }

  public static JSONKey<Date> dateTime(String name) {
    return new JSONKey<Date>(name, DATE_TIME);
  }

  public static JSONKey<TimeZone> dateTimeZone(String name) {
    return new JSONKey<TimeZone>(name, TIME_ZONE_FROM_TIME);
  }

  public static JSONKey<TimeZone> timeZoneID(String name) {
    return new JSONKey<TimeZone>(name, TIME_ZONE_ID);
  }

  public static JSONKey<Date> date(String name) {
    return new JSONKey<Date>(name, DATE);
  }

  public static Convertor<Object, String> emptyTextToNull(final Convertor<Object, String> textConvertor) {
    return new Convertor<Object, String>() {
      @Override
      public String convert(Object value) {
        String text = textConvertor.convert(value);
        return text == null || text.isEmpty() ? null : text;
      }
    };
  }

  public static <T> JSONKey<T> composition(final JSONKey<JSONObject> reference, final JSONKey<T> accessor) {
    return new JSONKey<T>(reference.getName(), new Convertor<Object, T>() {
      @Override
      public T convert(Object value) {
        JSONObject extObject = reference.convertValue(value);
        return extObject != null ? accessor.getValue(extObject) : null;
      }
    });
  }

  static class CastConvertor<T> extends Convertor<Object, T> {
    private final Class<T> myClass;

    public CastConvertor(Class<T> aClass) {
      myClass = aClass;
    }

    public static <T> CastConvertor<T> create(Class<T> aClass) {
      return new CastConvertor<T>(aClass);
    }

    @Override
    public T convert(Object value) {
      if (value == null) return null;
      T text = Util.castNullable(myClass, value);
      if (text == null) LogHelper.error("Expected", myClass, value);
      return text;
    }
  }


  private static class TrimLines extends Convertor<Object, String> {
    private final boolean myNotNull;

    private TrimLines(boolean notNull) {
      myNotNull = notNull;
    }

    @Override
    public String convert(Object value) {
      String text = TEXT.convert(value);
      if (text == null) return myNotNull ? "" : null;
      return TextUtil.trimLines(text);
    }
  }


  private static class TextTrim extends Convertor<Object, String> {
    private final Boolean myNotNull;

    public TextTrim(Boolean notNull) {
      myNotNull = notNull;
    }

    @Override
    public String convert(Object value) {
      String text = TEXT.convert(value);
      if (text == null) return Boolean.TRUE.equals(myNotNull) ? "" : null;
      text = text.trim();
      if (Boolean.FALSE.equals(myNotNull) && text.isEmpty()) return null;
      return text;
    }
  }

  public static class RootObject extends JSONKey<JSONObject> {
    public RootObject() {
      super("", OBJECT);
    }

    @NotNull
    public JSONObject parse(String json) throws JSONValueException {
      Object res;
      try {
        res = new JSONParser().parse(json);
      } catch (org.json.simple.parser.ParseException e) {
        throw new JSONValueException(e);
      }
      return JSONKey.ROOT_OBJECT.getNotNull(res);
    }

    /**
     * Logs all problems and returns null if cannot parse root object
     * @param json JSON to parse
     * @return parsed object
     */
    @Nullable
    public JSONObject parseNoExceptions(String json) {
      if (json == null) {
        LogHelper.error("Null JSON");
        return null;
      }
      try {
        return parse(json);
      } catch (JSONValueException e) {
        LogHelper.error("Failed to parse JSON", e);
        return null;
      }
    }
  }
}
