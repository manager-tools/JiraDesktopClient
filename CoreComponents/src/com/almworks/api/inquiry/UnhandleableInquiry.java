package com.almworks.api.inquiry;

/**
 * :todoc:
 *
 * @author sereda
 */
public class UnhandleableInquiry extends Exception {
  private final InquiryKey myInquiryKey;

  public UnhandleableInquiry(InquiryKey inquiryKey) {
    assert inquiryKey != null;
    myInquiryKey = inquiryKey;
  }

  public InquiryKey getInquiryKey() {
    return myInquiryKey;
  }
}
