package com.almworks.api.inquiry;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface InquiryDisplayer {
  InquiryDisplay getDisplay(InquiryLevel level, InquiryKey<?> key);
}
