package com.almworks.util;

import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.List;

public class LazySubscriptionTests extends BaseTestCase {
  public void test() {
    FireEventSupport<ChangeListener1<String>> eventSource =
      (FireEventSupport) FireEventSupport.create(ChangeListener1.class);
    Dispatcher dispatcher = new Dispatcher(eventSource);
    ChangeListener1<String> source = eventSource.getDispatcher();
    source.onChange("1");
    dispatcher.checkEmpty();

    EventLog log = new EventLog();
    dispatcher.addListener(log);
    source.onChange("2");
    dispatcher.check("2");
    log.check("-2");

    dispatcher.removeListener(log);
    source.onChange("3");
    dispatcher.checkEmpty();
    log.checkEmpty();

    dispatcher.addListener(log);
    EventLog log2 = new EventLog();
    dispatcher.addListener(log2);
    source.onChange("4");
    dispatcher.removeListener(log);
    source.onChange("5");
    dispatcher.addListener(log);
    dispatcher.removeListener(log2);
    source.onChange("6");
    dispatcher.removeListener(log2);
    source.onChange("7");
    dispatcher.removeListener(log);
    source.onChange("8");
    dispatcher.check("4", "5", "6", "7");
    log.check("-4", "-6", "-7");
    log2.check("-4", "-5");
  }

  private static class EventLog implements ChangeListener1<String> {
    private static final CollectionsCompare CHECK = new CollectionsCompare();
    private final List<String> myLog = Collections15.arrayList();

    @Override
    public void onChange(String object) {
      myLog.add(object);
    }

    public void checkEmpty() {
      CHECK.empty(myLog);
    }

    public void check(String... expected) {
      CHECK.order(expected, myLog);
      myLog.clear();
    }
  }

  private static class Dispatcher implements LazySubscription.Subscriber, ChangeListener1<String> {
    private final LazySubscription<ChangeListener1<String>> myListeners;
    private final FireEventSupport<ChangeListener1<String>> mySource;
    private final EventLog myLog = new EventLog();

    private Dispatcher(FireEventSupport<ChangeListener1<String>> source) {
      mySource = source;
      myListeners = (LazySubscription) LazySubscription.create(ChangeListener1.class, this);
    }

    @Override
    public void subscribe() {
      mySource.addListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, this);
    }

    @Override
    public void unsubscribe() {
      mySource.removeListener(this);
    }

    @Override
    public void onChange(String object) {
      myListeners.getDispatcher().onChange("-" + object);
      myLog.onChange(object);
    }

    public void checkEmpty() {
      myLog.checkEmpty();
    }

    public void addListener(ChangeListener1<String> listener) {
      myListeners.addListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, listener);
    }

    public void removeListener(ChangeListener1<String> listener) {
      myListeners.removeListener(listener);
    }

    public void check(String ... expected) {
      myLog.check(expected);
    }
  }

}
