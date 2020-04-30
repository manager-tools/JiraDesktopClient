package com.almworks.util.advmodel;

import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.ObjectArray;
import org.almworks.util.detach.Lifespan;

/**
 * @author dyoma
 */
public class CachingConvertingListDecorator<D, R> extends ConvertingListDecorator<D, R> {
  private final ObjectArray<R> myImage = new ObjectArray();

  public CachingConvertingListDecorator(AListModel<? extends D> domainList, Convertor<D, R> convertor) {
    super(domainList, convertor);
  }

  public R getAt(int index) {
    R cached = myImage.get(index);
    if (cached == null) {
      cached = super.getAt(index);
      myImage.set(index, cached);
    }
    return cached;
  }

  private void removeRange(int index, int length) {
    myImage.removeRange(index, length);
  }

  private void insertRange(int index, int length) {
    myImage.insertRange(index, length, null);
  }

  private void invalidateRange(AListEvent event) {
    for (int i = event.getLowAffectedIndex(); i <= event.getHighAffectedIndex(); i++)
      if (event.isAffected(i))
        myImage.set(i, null);
  }

  public static <D, R> ConvertingListDecorator<D, R> createCaching(Lifespan life, AListModel<? extends D> domainList,
    Convertor<D, R> convertor)
  {
    if (life.isEnded())
      return create(domainList, convertor);
    final CachingConvertingListDecorator<D, R> decorator =
      new CachingConvertingListDecorator<D, R>(domainList, convertor);
    Listener<D> listener = new Listener<D>() {
      public void onInsert(int index, int length) {
        decorator.insertRange(index, length);
      }

      public void onRemove(int index, int length, RemovedEvent<D> event) {
        decorator.removeRange(index, length);
      }

      public void onListRearranged(AListEvent event) {
        decorator.invalidateRange(event);
      }

      public void onItemsUpdated(UpdateEvent event) {
        decorator.invalidateRange(event);
      }
    };
    listener.onInsert(0, domainList.getSize());
    life.add(((AListModel<D>) domainList).addListener(listener));
    return decorator;
  }
}
