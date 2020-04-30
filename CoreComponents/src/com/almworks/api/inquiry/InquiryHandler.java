package com.almworks.api.inquiry;

import com.almworks.util.ui.UIComponentWrapper;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface InquiryHandler <T> extends UIComponentWrapper {
  InquiryKey<T> getInquiryKey();

  void setInquiryData(T data);

  T getInquiryData();

  void setListener(Listener listener);

  String getInquiryTitle();

  interface Listener {
    void onAnswer(boolean answerGiven);
  }
}
