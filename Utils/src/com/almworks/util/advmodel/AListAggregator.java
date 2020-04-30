package com.almworks.util.advmodel;

import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates lists so that resulting list has union of all elements.
 * Duplicate elements are removed, even if they are from the same list.
 * The order of elements is not defined.
 * T must be hashable
 */
@ThreadAWT
public class AListAggregator<T> {
  private final Map<Object, DetachComposite> myDetaches = Collections15.hashMap();
  private final Map<T, OwnerInfo> myElements = Collections15.linkedHashMap();
  private final OrderListModel<T> myImage = OrderListModel.create();

  public static <T> AListAggregator<T> create() {
    return new AListAggregator<T>();
  }

  public void add(@NotNull Lifespan life, @NotNull final Object key, @NotNull final AListModel<? extends T> model) {
    DetachComposite modelLife = new DetachComposite(true);
    modelLife.add(model.addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        for (int i = index; i < index + length; i++) {
          addElement(model.getAt(i), key);
        }
      }
    }));
    modelLife.add(((AListModel<T>) model).addRemovedElementListener(new AListModel.RemovedElementsListener<T>() {
      public void onBeforeElementsRemoved(AListModel.RemoveNotice<T> elements) {
        for (T element : elements.getList()) {
          removeElement(element,  key);
        }
      }
    }));
    for (T element : model.toList()) {
      addElement(element, key);
    }
    modelLife.add(new Detach() {
      protected void doDetach() {
        removeAll(key);
      }
    });
    DetachComposite oldDetach = myDetaches.put(key, modelLife);
    if (oldDetach != null) {
      assert false : key;
      oldDetach.detach();
    }
    life.add(modelLife);
  }

  private void removeElement(T element, Object key) {
    OwnerInfo info = myElements.get(element);
    if (info == null) {
      assert false : element;
      return;
    }
    assert myImage.indexOf(element) >= 0 : element;
    boolean orphan = info.removeOwner(key);
    if (orphan) {
      myElements.remove(element);
      myImage.remove(element);
    }
  }

  private void addElement(T element, Object key) {
    OwnerInfo info = myElements.get(element);
    if (info == null) {
      assert myImage.indexOf(element) == -1 : element;
      info = new OwnerInfo(key);
      myElements.put(element, info);
      myImage.addElement(element);
    } else {
      assert myImage.indexOf(element) >= 0 : element;
      info.addOwner(key);
    }
  }

  private void removeAll(Object key) {
    for (Iterator<Map.Entry<T, OwnerInfo>> ii = myElements.entrySet().iterator(); ii.hasNext();) {
      Map.Entry<T, OwnerInfo> entry = ii.next();
      OwnerInfo info = entry.getValue();
      boolean orphan = info.removeOwner(key);
      if (orphan) {
        myImage.remove(entry.getKey());
        ii.remove();
      }
    }
  }

  public AListModel<T> getImage() {
    return myImage;
  }

  public void remove(@NotNull Object key) {
    DetachComposite detach = myDetaches.remove(key);
    if (detach != null)
      detach.detach();
  }

  private static final class OwnerInfo {
    private Object mySingleOwner;
    private Set<Object> myOtherOwners;


    public OwnerInfo(Object owner) {
      mySingleOwner = owner;
    }

    public void addOwner(Object key) {
      if (mySingleOwner == null) {
        mySingleOwner = key;
      } else {
        if (myOtherOwners == null)
          myOtherOwners = Collections15.linkedHashSet();
        myOtherOwners.add(key);
      }
    }

    /**
     * @return true if element is not owned anymore
     */
    public boolean removeOwner(Object key) {
      if (key != null && key.equals(mySingleOwner))
        mySingleOwner = null;
      if (myOtherOwners != null)
        myOtherOwners.remove(key);
      return mySingleOwner == null && (myOtherOwners == null || myOtherOwners.size() == 0);
    }
  }
}
