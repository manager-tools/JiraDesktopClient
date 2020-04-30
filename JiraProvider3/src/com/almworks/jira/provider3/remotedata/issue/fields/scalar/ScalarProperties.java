package com.almworks.jira.provider3.remotedata.issue.fields.scalar;

import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Equality;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ScalarProperties<T> {
  @NotNull
  private final Convertor<Object, T> myFromJson;
  @Nullable("When field does not support generic edit upload (read-only or modifiable only by special operations)")
  private final ScalarUploadType<T> myUploadType;
  private final Equality<T> myEquality;
  @NotNull
  private final Convertor<T, String> myToDisplayable;
  @NotNull
  private final Class<T> myScalarClass;

  public ScalarProperties(Convertor<Object, T> fromJson, @Nullable ScalarUploadType<T> uploadType, Equality<T> equality, Convertor<T, String> toDisplayable, Class<T> scalarClass) {
    myFromJson = fromJson;
    myUploadType = uploadType;
    myEquality = equality;
    myToDisplayable = toDisplayable;
    myScalarClass = scalarClass;
  }

  public boolean isEditSupported() {
    return myUploadType != null;
  }

  @NotNull
  public Class<T> getScalarClass() {
    return myScalarClass;
  }

  public String toDisplayable(T value) {
    return myToDisplayable.convert(value);
  }

  @NotNull
  public Convertor<Object, T> getFromJson() {
    return myFromJson;
  }

  @Nullable
  public ScalarUploadType<T> getUploadType() {
    return myUploadType;
  }

  public boolean areEqual(T a, T b) {
    return myEquality.areEqual(a, b);
  }

  public String convertToForm(T value, RestServerInfo serverInfo) {
    if (myUploadType == null) {
      LogHelper.error("Upload not supported", myScalarClass, myFromJson);
      return "";
    }
    return myUploadType.toFormValue(value, serverInfo);
  }

  private static final String OPT_DAY_FORMAT = "jira.format.date.default";
  private static final String OPT_DATE_TIME_FORMAT = "jira.format.datetime.default";
  private static final String DEFAULT_DAYS_FORMAT = "dd/MMM/yy";
  public static DateFormat getDaysDateFormat() {
    String format = Env.getString(OPT_DAY_FORMAT);
    if (format == null) format = DEFAULT_DAYS_FORMAT;
    return new SimpleDateFormat(format, Locale.US);
  }

  public static SimpleDateFormat getDateTimeFormat() {
    String format = Env.getString(OPT_DATE_TIME_FORMAT);
    if (format == null) {
      String daysFormat = Env.getString(OPT_DAY_FORMAT);
      if (daysFormat == null) daysFormat = DEFAULT_DAYS_FORMAT;
      format = daysFormat + " hh:mm a";
    }
    return new SimpleDateFormat(format, Locale.US);
  }
}
