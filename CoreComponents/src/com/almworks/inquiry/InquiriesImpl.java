package com.almworks.inquiry;

import com.almworks.api.inquiry.*;
import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * :todoc:
 *
 * @author sereda
 */
public class InquiriesImpl implements Inquiries {
  private final List<InquiryDisplayer> myDisplayers = Collections15.arrayList();
  private final Map<InquiryKey, InquiryHandler> myHandlers = Collections15.hashMap();

  public InquiriesImpl(InquiryDisplayer[] displayers, InquiryHandler[] handlers) {
    for (int i = 0; i < displayers.length; i++) {
      InquiryDisplayer displayer = displayers[i];
      if (displayer != Dummies.DUMMY_DISPLAYER)
        myDisplayers.add(displayer);
    }
    for (int i = 0; i < handlers.length; i++) {
      InquiryHandler handler = handlers[i];
      if (handler == Dummies.DUMMY_HANDLER)
        continue;
      InquiryKey key = handler.getInquiryKey();
      if (myHandlers.put(key, handler) != null)
        throw new IllegalStateException("multiple handlers for key " + key);
    }
  }

  public static InquiriesImpl empty() {
    return new InquiriesImpl(new InquiryDisplayer[] {}, new InquiryHandler[] {});
  }

  public <T extends AbstractInquiryData> T inquire(InquiryLevel level, InquiryKey<T> key, T inquiryData)
    throws UnhandleableInquiry, InterruptedException {

    Threads.assertLongOperationsAllowed();
    Pair<ScalarModel<Boolean>, InquiryHandler<T>> result = showInquiry(key, level, inquiryData);
    Boolean answered = result.getFirst().getValueBlocking();
    InquiryHandler<T> handler = result.getSecond();
    inquiryData = handler.getInquiryData();
    handler.dispose();
    if (inquiryData != null)
      inquiryData.setAnswered(answered.booleanValue());
    return inquiryData;
  }

  public <T extends AbstractInquiryData> void inquire(InquiryLevel level, InquiryKey<T> key, final T inquiryData,
    final AnswerAcceptor<T> answerAcceptor, ThreadGate answerGate) throws UnhandleableInquiry {

    final Pair<ScalarModel<Boolean>, InquiryHandler<T>> result = showInquiry(key, level, inquiryData);
    result.getFirst().getEventSource().addListener(answerGate, new ScalarModel.Adapter<Boolean>() {
      public void onContentKnown(ScalarModelEvent<Boolean> event) {
        InquiryHandler<T> handler = result.getSecond();
        T data = handler.getInquiryData();
        handler.dispose();
        data.setAnswered(event.getNewValue().booleanValue());
        answerAcceptor.acceptAnswer(data);
      }
    });
  }

  private <T> Pair<ScalarModel<Boolean>, InquiryHandler<T>> showInquiry(InquiryKey<T> key, InquiryLevel level,
    T inquiryData) throws UnhandleableInquiry {

    InquiryHandler<T> handler = findHandler(key);
    InquiryDisplay display = findDisplay(level, key);

    handler.setInquiryData(inquiryData);
    display.setInquiryHandler(handler);

    display.show();
    ScalarModel<Boolean> result = display.getResult();
    return Pair.create(result, handler);
  }

  private <T> InquiryHandler<T> findHandler(InquiryKey<T> key) throws UnhandleableInquiry {
    InquiryHandler handler = myHandlers.get(key);
    if (handler == null)
      throw new UnhandleableInquiry(key);
    return handler;
  }

  private InquiryDisplay findDisplay(InquiryLevel level, InquiryKey<?> key) throws UnhandleableInquiry {
    for (Iterator<InquiryDisplayer> ii = myDisplayers.iterator(); ii.hasNext();) {
      InquiryDisplayer inquiryDisplayer = ii.next();
      InquiryDisplay display = inquiryDisplayer.getDisplay(level, key);
      if (display != null)
        return display;
    }
    throw new UnhandleableInquiry(key);
  }
}
