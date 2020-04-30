package com.almworks.jira.provider3.remotedata.issue.fields.scalar;

import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.datetime.DateUtil;
import org.almworks.util.Util;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public interface ScalarUploadType<T> {
  ScalarUploadType<String> TEXT = new Text();

  DateTime DATE = new DateTime();

  ScalarUploadType<Integer> DAYS = new ScalarUploadType<Integer>() {
    @Override
    public Object toJsonValue(Integer value, RestServerInfo serverInfo) {
      if (value == null) return null;
      Date date = DateUtil.toInstantOnDay(value, serverInfo.getServerTimeZone());
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
      format.setTimeZone(serverInfo.getServerTimeZone());
      return format.format(date);
    }

    @Override
    public String toFormValue(Integer value, RestServerInfo serverInfo) {
      if (value == null) return "";
      Date date = DateUtil.toInstantOnDay(value, serverInfo.getServerTimeZone());
      DateFormat format = ScalarProperties.getDaysDateFormat();
      format.setTimeZone(serverInfo.getServerTimeZone());
      return format.format(date);
    }
  };

  ScalarUploadType<BigDecimal> DECIMAL = new ScalarUploadType<BigDecimal>() {

    @Override
    public Object toJsonValue(BigDecimal value, RestServerInfo serverInfo) {
      return value != null ? value.doubleValue() : null;
    }

    @Override
    public String toFormValue(BigDecimal value, RestServerInfo serverInfo) {
      return value.toPlainString();
    }
  };

  Object toJsonValue(T value, RestServerInfo serverInfo);

  String toFormValue(T value, RestServerInfo serverInfo);

  class Text implements ScalarUploadType<String> {
    @Override
    public Object toJsonValue(String value, RestServerInfo serverInfo) {
      return normalize(value);
    }

    @Override
    public String toFormValue(String value, RestServerInfo serverInfo) {
      return Util.NN(value).trim();
    }

    public static String normalize(String value) {
      if (value != null) value = value.trim();
      return value != null ? value : "";
    }
  }


  class DateTime implements ScalarUploadType<Date> {
    @Override
    public Object toJsonValue(Date value, RestServerInfo serverInfo) {
      TimeZone timeZone = serverInfo.getServerTimeZone();
      return toJsonValue(value, timeZone);
    }

    public Object toJsonValue(Date value) {
      return toJsonValue(value, (TimeZone)null);
    }

    public Object toJsonValue(Date value, TimeZone timeZone) {
      if (value == null) return null;
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
      if (timeZone != null) format.setTimeZone(timeZone);
      return format.format(value);
    }

    @Override
    public String toFormValue(Date value, RestServerInfo serverInfo) {
      if (value == null) return "";
      SimpleDateFormat format = ScalarProperties.getDateTimeFormat();
      format.setTimeZone(serverInfo.getServerTimeZone());
      return format.format(value);
    }
  }
}
