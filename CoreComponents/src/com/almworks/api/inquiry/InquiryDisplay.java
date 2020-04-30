package com.almworks.api.inquiry;

import com.almworks.util.model.ScalarModel;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface InquiryDisplay {
  void setInquiryHandler(InquiryHandler<?> handler);

  void show();

  /**
   * @return model for boolean, which is set to TRUE if user answered the inquiry
   *         and FALSE if user cancelled the inquiry.
   */
  ScalarModel<Boolean> getResult();
}
