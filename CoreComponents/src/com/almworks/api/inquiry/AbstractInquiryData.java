package com.almworks.api.inquiry;

/**
 * :todoc:
 *
 * @author sereda
 */
public class AbstractInquiryData {
  private Boolean myAnswered;

  public final boolean isInquiryFinished() {
    return myAnswered != null;
  }

  public final boolean isAnswered() {
    return myAnswered != null && myAnswered.booleanValue();
  }

  public final void setAnswered(boolean value) {
    myAnswered = Boolean.valueOf(value);
  }
}
