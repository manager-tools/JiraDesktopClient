package com.almworks.util.advmodel;

import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

public class AListModelDetacher<T> extends Detach implements AListModel.Listener<T> {
  private boolean myEnded = false;

  private AListModel.Listener<T> myListener;
  private Detach myDetach;

  public AListModelDetacher() {
  }

  @ThreadAWT
  public void attachListener(Lifespan lifespan, AListModel<T> model, AListModel.Listener<T> listener) {
    if (lifespan.isEnded()) {
      return;
    }
    boolean detaching = isDetachStarted();
    if (myListener != null || myDetach != null || detaching) {
      assert false : "already used (" + myListener + ", " + detaching + ")";
      return;
    }
    myListener = listener;
    myDetach = model.addListener(this);
    lifespan.add(this);
  }

  public void preDetach() {
    super.preDetach();
    myEnded = true;
    Detach detach = myDetach;
    if (detach != null) {
      detach.preDetach();
    }
  }

  protected void doDetach() throws Exception {
    assert myEnded;
    myEnded = true;
    Detach detach = myDetach;
    myDetach = null;
    myListener = null;
    if (detach != null) {
      detach.detach();
    }
  }

  public void onInsert(int index, int length) {
    AListModel.Listener<T> listener = myListener;
    if (!myEnded && listener != null) {
      listener.onInsert(index, length);
    }
  }

  public void onItemsUpdated(AListModel.UpdateEvent event) {
    AListModel.Listener<T> listener = myListener;
    if (!myEnded && listener != null) {
      listener.onItemsUpdated(event);
    }
  }

  public void onListRearranged(AListModel.AListEvent event) {
    AListModel.Listener<T> listener = myListener;
    if (!myEnded && listener != null) {
      listener.onListRearranged(event);
    }
  }

  public void onRemove(int index, int length, AListModel.RemovedEvent<T> event) {
    AListModel.Listener<T> listener = myListener;
    if (!myEnded && listener != null) {
      listener.onRemove(index, length, event);
    }
  }

  public static <T> Detach attach(Lifespan lifespan, AListModel<T> model, AListModel.Listener<T> listener) {
    if (lifespan.isEnded())
      return NOTHING;
    AListModelDetacher<T> detacher = new AListModelDetacher<T>();
    detacher.attachListener(lifespan, model, listener);
    return detacher;
  }
}
