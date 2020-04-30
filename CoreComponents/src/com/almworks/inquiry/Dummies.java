package com.almworks.inquiry;

import com.almworks.api.inquiry.*;

import javax.swing.*;

/**
 * :todoc:
 *
 * @author sereda
 */
class Dummies {
  static final InquiryDisplayer DUMMY_DISPLAYER = new InquiryDisplayer() {
    public InquiryDisplay getDisplay(InquiryLevel level, InquiryKey<?> key) {
      throw new UnsupportedOperationException();
    }
  };
  static final InquiryHandler DUMMY_HANDLER = new InquiryHandler() {
    public InquiryKey getInquiryKey() {
      throw new UnsupportedOperationException();
    }

    public void setInquiryData(Object o) {
      throw new UnsupportedOperationException();
    }

    public Object getInquiryData() {
      throw new UnsupportedOperationException();
    }

    public JComponent getComponent() {
      throw new UnsupportedOperationException();
    }

    public void dispose() {
      throw new UnsupportedOperationException();
    }

    public void setListener(Listener listener) {
      throw new UnsupportedOperationException();
    }

    public String getInquiryTitle() {
      throw new UnsupportedOperationException();
    }
  };
}
