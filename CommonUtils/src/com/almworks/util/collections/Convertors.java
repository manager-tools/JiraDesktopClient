package com.almworks.util.collections;

import com.almworks.util.commons.Factory;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class Convertors {
  public static final Convertor<Object, String> TO_STRING = new ToString(null);
  public static final Convertor<Object, String> TO_NN_STRING = new ToString("");

  private static final Convertor ID_CONVERTOR = new Convertor() {
    public Object convert(Object value) {
      return value;
    }
  };

  public static final Convertor<String, String> TO_LOWER_CASE = new Convertor<String, String>() {
    public String convert(String value) {
      //noinspection ConstantConditions
      return value == null ? null : Util.lower(value);
    }
  };

  private static final Convertor<String, BigDecimal> PARSE_BIGDECIMAL = new Convertor<String, BigDecimal>() {
    @SuppressWarnings({"ConstantConditions"})
    public BigDecimal convert(String value) {
      try {
        return value != null ? new BigDecimal(value) : null;
      } catch (Exception e) {
        return null;
      }
    }
  };

  public static final Convertor<?, ?> TO_NULL = new Convertor<Object, Object>() {
    @Override
    public Object convert(Object value) {
      return null;
    }
  };

  @SuppressWarnings("unchecked")
  public static <T> Convertor<T, String> getToString() {
    return (Convertor<T, String>) TO_STRING;
  }

  @SuppressWarnings("unchecked")
  public static <T> Convertor<T, String> getToNNString() {
    return (Convertor<T, String>) TO_NN_STRING;
  }

  public static <D, R> Convertor<D, R> fromFactory(final Factory<R> factory) {
    return new Convertor<D, R>() {
      public R convert(D value) {
        return factory.create();
      }
    };
  }

  public static <D, R> Convertor<D, R> fromMap(final Map<D, R> map) {
    return new Convertor<D, R>() {
      public R convert(D key) {
        return map.get(key);
      }
    };
  }

  public static <T> Convertor<T, T> identity() {
    return ID_CONVERTOR;
  }

  public static <D, R> Convertor<D, R> constant(final R value) {
    return new Convertor<D, R>() {
      public R convert(D x) {
        return value;
      }
    };
  }

  public static Convertor<String, Date> dateParser(final DateFormat dateFormat) {
    return new Convertor<String, Date>() {
      public Date convert(String value) {
        try {
          return value != null ? dateFormat.parse(value) : null;
        } catch (ParseException e) {
          return null;
        }
      }
    };
  }

  public static Convertor<String, BigDecimal> parseBigDecimal() {
    return PARSE_BIGDECIMAL;
  }

  public static <D, R> Convertor<D, R> toNull() {
    return (Convertor<D, R>) TO_NULL;
  }

  public static Convertor<String, String> replaceAll(final String regex, final String replacement) {
    return new Convertor<String, String>() {
      @Override
      public String convert(String value) {
        return value != null ? value.replaceAll(regex, replacement) : null;
      }
    };
  }

  public static <D, R> Convertor<D, R> fromReadAccessor(final ReadAccessor<D, R> accessor) {
    return new Convertor<D, R>() {
      @Override
      public R convert(D value) {
        return accessor.getValue(value);
      }
    };
  }

  public static <D, I, R> Convertor<D, R> superposition(final Convertor<? super D, I> first, final Convertor<I, R> second) {
    return new Convertor<D, R>() {
      @Override
      public R convert(D value) {
        I intermediate = first.convert(value);
        return second.convert(intermediate);
      }
    };
  }

  public static <T> Convertor<Map<? extends TypedKey<?>, ?>, T> getFromMap(final TypedKey<? extends T> key) {
    return new Convertor<Map<? extends TypedKey<?>, ?>, T>() {
      @Override
      public T convert(Map<? extends TypedKey<?>, ?> value) {
        return value != null && key != null ? key.getFrom(value) : null;
      }
    };
  }

  private static class ToString extends Convertor<Object, String> {
    private final String myNullValue;

    private ToString(String nullValue) {
      myNullValue = nullValue;
    }

    public String convert(Object t) {
      return t == null ? myNullValue : t.toString();
    }
  }
}
