package com.almworks.util.advmodel;

import com.almworks.util.events.FireEventSupport;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 * @deprecated Should be replaced with {@link com.almworks.util.advmodel.SegmentedListModel}
 */
@Deprecated
public abstract class BaseListModelHolder<T> extends AROList<T> {
  private final FireEventSupport<RemovedElementsListener> myRemovedElementsListeners =
    FireEventSupport.createSynchronized(RemovedElementsListener.class);
  private Detach myListDetach = Detach.NOTHING;

  public Detach addRemovedElementListener(RemovedElementsListener<T> listener) {
    return myRemovedElementsListeners.addStraightListener(listener);
  }

  public int getSize() {
    AListModel<? extends T> model = getHoldingModel();
    return model != null ? model.getSize() : 0;
  }

  public T getAt(int index) {
    AListModel<? extends T> model = getHoldingModel();
    if (model == null)
      throw new IndexOutOfBoundsException("No list set when getting at index: " + index);
    return model.getAt(index);
  }

  @Nullable
  protected abstract AListModel<? extends T> getHoldingModel();

  @NotNull
  protected abstract Listener getListenerDispatcher();

  protected Detach setListImpl(int oldSize, Detach additionalDetach) {
    myListDetach.detach();
    Listener dispatcher = getListenerDispatcher();
    AListModel<? extends T> model = getHoldingModel();
    //noinspection RawUseOfParameterizedType
    RemovedElementsListener removeDispatcher = myRemovedElementsListeners.getDispatcher();
    if (oldSize > 0) {
      AROList.fireRemove(removeDispatcher, dispatcher, toList(), 0);
    }
    if (model != null) {
      DetachComposite detach = new DetachComposite();
      detach.add(additionalDetach);
      detach.add(model.addRemovedElementListener(removeDispatcher));
      myListDetach = detach;
    } else {
      myListDetach = Detach.NOTHING;
      assert additionalDetach == Detach.NOTHING : additionalDetach;
    }
    int newSize = getSize();
    if (newSize > 0)
      dispatcher.onInsert(0, newSize);
    return new Detach() {
      protected void doDetach() {
        myListDetach.detach();
        myListDetach = NOTHING;
      }
    };
  }
}
