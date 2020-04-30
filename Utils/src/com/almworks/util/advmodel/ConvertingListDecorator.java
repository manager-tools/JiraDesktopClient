package com.almworks.util.advmodel;

import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import org.almworks.util.detach.Detach;

/**
 * @author : Dyoma
 */
public class ConvertingListDecorator <D, R> extends AROList<R> {
  private final AListModel<? extends D> myDomainList;
  private final Convertor<D, R> myConvertor;

  public ConvertingListDecorator(AListModel<? extends D> domainList, Convertor<D, R> convertor) {
    myDomainList = domainList;
    myConvertor = convertor;
  }

  // todo what's with the different types?! Listener is Listener<R>, domain list is <D>
  public Detach addListener(Listener listener) {
    return myDomainList.addListener(listener);
  }


  public void removeFirstListener(Condition<Listener> condition) {
    myDomainList.removeFirstListener(condition);
  }

  public Detach addRemovedElementListener(final RemovedElementsListener<R> listener) {
    return ((AListModel<D>) myDomainList).addRemovedElementListener(new RemovedElementsListener<D>() {
      public void onBeforeElementsRemoved(RemoveNotice<D> notice) {
        listener.onBeforeElementsRemoved(notice.convertNotice(myConvertor));
      }
    });
  }

  public int getSize() {
    return myDomainList.getSize();
  }

  public R getAt(int index) {
    return myConvertor.convert(myDomainList.getAt(index));
  }

  public void forceUpdateAt(int index) {
    myDomainList.forceUpdateAt(index);
  }

  protected AListModel<? extends D> getDomainModel() {
    return myDomainList;
  }

  public static <D, R> ConvertingListDecorator<D, R> create(AListModel<? extends D> source, Convertor<D, R> convertor) {
    return new ConvertingListDecorator<D, R>(source, convertor);
  }
}
