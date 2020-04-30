package com.almworks.api.inquiry;

import com.almworks.util.exec.ThreadGate;

/**
 * This interface provides possibility of querying user during some process.
 * There are two types of querying - critical (read "modal"), which should be
 * answered by user before application continues; and non-critical (ordinary),
 * which may be answered at user's discretion - but a component may wait for
 * the answer.
 *
 * @author sereda
 */
public interface Inquiries {
  /**
   * Inquires user and blocks until data is available.
   */
  <T extends AbstractInquiryData> T inquire(InquiryLevel level, InquiryKey<T> key, T inquiryData)
    throws UnhandleableInquiry, InterruptedException;

  /**
   * Inquires user and continues, answer being passed later via AnswerAcceptor
   */
  <T extends AbstractInquiryData> void inquire(InquiryLevel level, InquiryKey<T> key, T inquiryData,
    AnswerAcceptor<T> answerAcceptor, ThreadGate answerGate) throws UnhandleableInquiry;
}
