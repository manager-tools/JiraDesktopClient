package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.EventContext;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

final class EventContextImpl extends CellContextImpl implements EventContext {
  private static final Log<EventContextImpl> log = Log.get(EventContextImpl.class);
  private final TypedKey<?> myDataKey;
  private final Object myData;
  private boolean myConsumed = false;
  private Cursor myCursor;

  public <T> EventContextImpl(HostComponentState<?> state, TypedKey<? super T> dataKey, T data) {
    super(state);
    myDataKey = dataKey;
    myData = data;
  }

  @Nullable
  @Override
  public <T> T getData(TypedKey<? extends T> key) {
    return myDataKey.equals(key) ? (T) myData : null;
  }

  @Override
  public void consume() {
    myConsumed = true;
  }

  @Override
  public void setCursor(Cursor cursor) {
    myCursor = cursor;
  }

  public Cursor getCursor() {
    return myCursor;
  }

  public boolean isConsumed() {
    return myConsumed;
  }

}
