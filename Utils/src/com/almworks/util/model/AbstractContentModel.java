package com.almworks.util.model;

import com.almworks.util.events.AddListenerHook;
import com.almworks.util.events.EventSource;
import com.almworks.util.events.FireEventSupport;
import util.concurrent.SynchronizedBoolean;

/**
 * :todoc:
 *
 * @author sereda
 */
public abstract class AbstractContentModel <
  CE extends ContentModelEvent,
  CL extends ContentModelConsumer<CE>,
  C extends ContentModel<CE, CL>,
  M extends ModelSetter<C, M>
  > extends AddListenerHook.Adapter<CL> implements ContentModel<CE, CL>, ModelSetter<C, M> {

  protected final boolean myContentChangeable;
  protected final SynchronizedBoolean myContentKnown;
  protected final SynchronizedBoolean myContentRequested = new SynchronizedBoolean(false);
  protected final FireEventSupport<CL> myEventSupport;
  protected final FireEventSupport<ModelSetter.RequestConsumer<M>> myRequestSupport;
  protected final Object myLock = new Object();

  public AbstractContentModel(boolean contentKnown, boolean contentChangeable, Class<CL> eventListener) {
    myContentKnown = new SynchronizedBoolean(contentKnown, myLock);
    myContentChangeable = contentChangeable;
    myEventSupport = FireEventSupport.create(eventListener, myLock, false, this);
    myRequestSupport = FireEventSupport.create((Class) ModelSetter.RequestConsumer.class, myLock, true, null);
  }

  public boolean isContentKnown() {
    return myContentKnown.get();
  }

  public void requestContent() {
    if (!myContentRequested.commit(false, true))
      return;
    ModelSetter setter = this;
    myRequestSupport.getDispatcher().valueRequested((M) setter);
  }

  public boolean isContentChangeable() {
    return myContentChangeable;
  }

  public EventSource<CL> getEventSource() {
    return myEventSupport;
  }

  public C getModel() {
    ContentModel model = this;
    return (C) model;
  }

  public EventSource<RequestConsumer<M>> getRequestEventSource() {
    return myRequestSupport;
  }

  public void setContentKnown() {
    CL dispatcher = null;
    synchronized (myLock) {
      if (myContentKnown.commit(false, true))
        dispatcher = myEventSupport.getDispatcherSnapshot();
    }
    if (dispatcher != null)
      dispatcher.onContentKnown(createDefaultEvent());
  }

  public boolean isContentRequested() {
    return myContentRequested.get();
  }

  protected abstract CE createDefaultEvent();

  public Object getLock() {
    return myLock;
  }
}
