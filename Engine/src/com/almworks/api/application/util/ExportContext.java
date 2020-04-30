package com.almworks.api.application.util;

import com.almworks.util.collections.UserDataHolder;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

public class ExportContext {
  private final NumberFormat myNumberFormat;
  private final DateFormat myDateFormat;
  private final boolean myHtmlAccepted;
  private final UserDataHolder myUserData = new UserDataHolder();

  public ExportContext(NumberFormat numberFormat, DateFormat dateFormat, boolean htmlAccepted) {
    myNumberFormat = numberFormat;
    myDateFormat = dateFormat;
    myHtmlAccepted = htmlAccepted;
  }

  public String formatDate(Date value) {
    if (value == null) return "";
    return myDateFormat.format(new Date(value.getTime()));
  }

  public String formatNumber(double value) {
    return myNumberFormat.format(value);
  }

  public String formatNumber(Number value) {
    if (value == null) return "";
    return myNumberFormat.format(value);
  }

  public UserDataHolder getUserData() {
    return myUserData;
  }

  public boolean isHtmlAccepted() {
    return myHtmlAccepted;
  }
}
