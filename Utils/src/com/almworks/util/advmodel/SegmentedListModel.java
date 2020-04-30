package com.almworks.util.advmodel;

import com.almworks.util.collections.Convertor;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author dyoma
 */
public class SegmentedListModel<T> extends AbstractAListModel<T> {
  private final List<AListModel<? extends T>> mySegments = Collections15.arrayList();
  private final List<Detach> myDetaches = Collections15.arrayList();

  public int getSize() {
    return sumSize(mySegments.size());
  }

  private int sumSize(int count) {
    int sum = 0;
    for (int i = 0; i < count; i++)
      sum += mySegments.get(i).getSize();
    return sum;
  }

  public T getAt(int index) {
//    Iterator<AListModel<? extends T>> it = mySegments.iterator();
    AListModel<? extends T> segment;
    int i = 0;
    while (true) {
      if (i >= mySegments.size())
//      if (!it.hasNext())
        throw new IndexOutOfBoundsException(index + " size: " + getSize());
//      segment = it.next();
      segment = mySegments.get(i);
      i++;
      int size = segment.getSize();
      if (index < size)
        break;
      index -= size;
    }
    return segment.getAt(index);
  }

  @ThreadAWT
  public void insertSegment(int index, AListModel<? extends T> segment) {
    mySegments.add(index, EMPTY);
    myDetaches.add(index, Detach.NOTHING);
    attachAt(index, segment);
  }

  @ThreadAWT
  public void insertSegments(int index, List<? extends AListModel<? extends T>> segments) {
    for (int i = 0; i < segments.size(); i++) {
      AListModel<? extends T> segment = segments.get(i);
      insertSegment(index + i, segment);
    }
  }

  @ThreadAWT
  public void addSegment(AListModel<? extends T> segment) {
    insertSegment(mySegments.size(), segment);
  }

  @Nullable
  @ThreadAWT
  public AListModel<? extends T> getSegment(int index) {
    if (index < 0 || index >= mySegments.size())
      return null;
    else
      return mySegments.get(index);
  }

  @ThreadAWT
  public AListModel<? extends T> setSegment(int index, AListModel<? extends T> segment) {
    AListModel<? extends T> old = detachAt(index);
    if (segment != null) {
      attachAt(index, segment);
    }
    return old;
  }

  public boolean removeSegment(AListModel<? extends T> model) {
    int index = mySegments.indexOf(model);
    if (index < 0) {
      return false;
    } else {
      AListModel<? extends T> old = removeSegment(index);
      assert old == model;
      return true;
    }
  }

  public AListModel<? extends T> removeSegment(int index) {
    AListModel<? extends T> old = detachAt(index);
    mySegments.remove(index);
    myDetaches.remove(index);
    return old;
  }

  public void removeAll() {
    while (mySegments.size() > 0) {
      removeSegment(0);
    }
  }

  @ThreadAWT
  public void removeSegmentsRange(int index, int length) {
    while (length > 0) {
      removeSegment(index);
      length--;
    }
  }

  private void attachAt(int index, AListModel<? extends T> segment) {
    attach_noEvent(index, segment);
    fireInsert(sumSize(index), segment.getSize());
  }

  private void attach_noEvent(int index, AListModel<? extends T> segment) {
    assert mySegments.get(index) == EMPTY;
    assert myDetaches.get(index) == Detach.NOTHING;
    if (segment == null)
      segment = AListModel.EMPTY;
    mySegments.set(index, segment);
    DetachComposite detach = new DetachComposite();
    myDetaches.set(index, detach);
    ListenerRedispatcher listener = new ListenerRedispatcher(segment);
    detach.add(segment.addListener((Listener) listener));
    //noinspection RawUseOfParameterizedType
    detach.add(((AListModel) segment).addRemovedElementListener(listener));
  }

  private AListModel<? extends T> detachAt(int index) {
    AListModel<? extends T> toRemove = mySegments.get(index);
    RemovedEvent<T> event;
    if (toRemove.getSize() > 0) {
      int headSize = sumSize(index);
      event = fireBeforeElementsRemoved(headSize, toRemove.toList());
    } else
      event = null;
    mySegments.set(index, EMPTY);
    myDetaches.set(index, Detach.NOTHING).detach();
    if (event != null)
      fireRemoved(event);
    return toRemove;
  }

  public static <T> SegmentedListModel<T> create() {
    return new SegmentedListModel<T>();
  }

  public static <T> SegmentedListModel<T> create(Lifespan life) {
    final SegmentedListModel<T> result = create();
    life.add(result.getRemoveAllDetach());
    return result;
  }

  public static <T> SegmentedListModel<T> create(Lifespan life, AListModel<? extends T>... models) {
    final SegmentedListModel<T> result = create(models);
    life.add(result.getRemoveAllDetach());
    return result;
  }

  private Detach getRemoveAllDetach() {
    return new Detach() {
      protected void doDetach() throws Exception {
        removeAll();
      }
    };
  }

  public static <T> SegmentedListModel<T> create(Lifespan life, Collection<? extends AListModel<? extends T>> models) {
    //noinspection unchecked
    return create(life, models.toArray(new AListModel[models.size()]));
  }


  public static <T> SegmentedListModel<T> create(AListModel<? extends T>... models) {
    SegmentedListModel<T> model = new SegmentedListModel<T>();
    for (AListModel<? extends T> m : models) {
      int index = model.mySegments.size();
      model.mySegments.add(index, EMPTY);
      model.myDetaches.add(index, Detach.NOTHING);
      model.attach_noEvent(index, m);
    }
    return model;
  }


  public static <L, T> AListModel<T> flatten(final Lifespan life, final AListModel<L> original,
    final Convertor<L, AListModel<T>> modelAspect)
  {
    final SegmentedListModel<T> model = create();
    Adapter<L> listener = new Adapter<L>() {
      public void onInsert(int index, int length) {
        if (!life.isEnded())
          model.insertSegments(index, modelAspect.collectList(original.subList(index, length)));
      }

      public void onRemove(int index, int length, RemovedEvent<L> event) {
        if (!life.isEnded())
          model.removeSegmentsRange(index, length);
      }

      public void onChange() {
        assert false : "Not implemented yet";
      }
    };
    life.add(original.addListener(listener));
    life.add(new Detach() {
      protected void doDetach() throws Exception {
        model.removeAll();
      }
    });
    listener.onInsert(0, original.getSize());
    return model;
  }

  public static <T> AListModel<T> prepend(T item, AListModel<? extends T> model) {
    return create(FixedListModel.create(item), model);
  }

//  private static final TypedKey<RemovedEvent> REMOVE_EVENT = TypedKey.create("removeEvent");


  private class ListenerRedispatcher implements Listener<T>, RemovedElementsListener<T> {
    private final AListModel<?> myModel;

    public ListenerRedispatcher(AListModel<?> model) {
      myModel = model;
    }

    public void onInsert(int index, int length) {
      fireInsert(index + getHeadSize(), length);
    }

    public void onBeforeElementsRemoved(RemoveNotice<T> elements) {
      RemovedEvent<T> event = fireBeforeElementsRemoved(elements.translateIndex(getHeadSize()));
//      elements.putUserData(REMOVE_EVENT, event);
    }

    public void onRemove(int index, int length, RemovedEvent<T> event) {
      RemovedEvent<T> translatedEvent = event.translateIndex(getHeadSize());
//      RemovedEvent<T> userData = event.getUserData(REMOVE_EVENT);
//      assert userData != null;
      fireRemoved(translatedEvent);
    }

    public void onListRearranged(AListEvent event) {
      fireRearrange(event.translateIndex(getHeadSize()));
    }

    public void onItemsUpdated(UpdateEvent event) {
      fireUpdate(event.translateIndex(getHeadSize()));
    }

    private int getHeadSize() {
      Iterator<? extends AListModel<?>> it = mySegments.iterator();
      int sum = 0;
      while (it.hasNext()) {
        AListModel<?> model = it.next();
        if (model == myModel)
          return sum;
        sum += model.getSize();
      }
      assert false;
      return 0;
    }
  }
}
