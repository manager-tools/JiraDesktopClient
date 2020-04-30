package com.almworks.api.inquiry;

/**
 * :todoc:
 *
 * @author sereda
 */
public final class InquiryLevel {
  public static final InquiryLevel CRITICAL = new InquiryLevel("critical");
  public static final InquiryLevel ORDINARY = new InquiryLevel("ordinary");

  private final String myName;

  private InquiryLevel(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }
}
