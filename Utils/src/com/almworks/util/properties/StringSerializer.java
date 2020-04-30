package com.almworks.util.properties;

import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author : Dyoma
 */
public interface StringSerializer <T> extends StringLoader<T> {
  String storeToString(T value);

  StringSerializer<Boolean> BOOLEAN = new StringSerializer<Boolean>() {
    public String storeToString(Boolean b) {
      return b.toString();
    }

    public Boolean restoreFromString(String string) {
      return Boolean.valueOf(string);
    }
  };

  StringSerializer<String> STRING = new StringSerializer<String>() {
    public String storeToString(String value) {
      return value;
    }

    public String restoreFromString(String string) {
      return string;
    }
  };

  StringSerializer<Integer> INTEGER = new StringSerializer<Integer>() {
    public String storeToString(Integer integer) {
      return String.valueOf(integer);
    }

    public Integer restoreFromString(String string) {
      if ("null".equals(string)) return null;
      return Integer.parseInt(string);
    }
  };

  StringSerializer<Date> DATE = new StringSerializer<Date>() {
    public String storeToString(Date date) {
      return Long.toString(date.getTime());
    }

    public Date restoreFromString(String string) {
      return new Date(Long.parseLong(string));
    }
  };

  StringSerializer<BigDecimal> BIGDECIMAL = new StringSerializer<BigDecimal>() {
    public String storeToString(BigDecimal bigDecimal) {
      return bigDecimal.toString();
    }

    public BigDecimal restoreFromString(String string) {
      return new BigDecimal(string);
    }
  };

  class CollectionSerializer <T> implements StringSerializer<Collection<T>> {
    private final StringSerializer<T> myElementSerializer;

    public CollectionSerializer(StringSerializer<T> elementSerializer) {
      myElementSerializer = elementSerializer;
    }

    public String storeToString(Collection<T> ts) {
      StringBuffer buffer = new StringBuffer();
      String separator = "";
      for (T t : ts) {
        String string = myElementSerializer.storeToString(t);
        string = string.replaceAll("\\\\", "\\\\");
        buffer.append(separator);
        buffer.append(TextUtil.escapeChar(string, ','));
        separator = ",";
      }
      return buffer.toString();
    }

    public Collection<T> restoreFromString(String string) {
      if (string == null || string.length() == 0)
        return Collections15.emptyCollection();
      String[] strings = string.split(",");
      List<T> result = Collections15.arrayList();
      for (String s : strings) {
        result.add(myElementSerializer.restoreFromString(TextUtil.unescapeChar(s, ',')));
      }
      return result;
    }
  }
}
