package com.almworks.util.advmodel;

import com.almworks.util.collections.ChangeListener;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

/**
 * This mediator class provides a AListModel that copies all data from another source model.
 * Whenever the source model changes, repeating list model resyncs with the source model (not
 * necessarily in the optimal way! no LCS here). When the source model is changed, the change
 * is also synced in the same way - so if you change one source model to another source model with
 * the same data, no events will be fired.
 * <p/>
 * This model can also "freeze", which makes it do no synchronization. When model unfreezes, it
 * does synchronization according to the latest state of source model.
 * <p>
 * Acceptable only for small models
 *
 * @param <T>
 */
public class RepeatingListModel<T> {
  private final OrderListModel<T> myImage = OrderListModel.create();
  private final Lifecycle mySourceLife = new Lifecycle();
  private final ChangeListener myListener = new ChangeListener() {
    public void onChange() {
      sync();
    }
  };

  private AListModel<T> mySource;
  private boolean myFrozen;

  public RepeatingListModel() {
  }

  public static <T> RepeatingListModel<T> create() {
    return new RepeatingListModel<T>();
  }

  public boolean isSourceSet() {
    return mySource != null;
  }

  public void setSource(@Nullable AListModel<T> source) {
    setSource(Lifespan.FOREVER, source);
  }
  
  public void setSource(Lifespan life, @Nullable AListModel<T> source) {
    mySourceLife.cycle();
    mySource = source;
    if (mySource != null) {
      mySource.addAWTChangeListener(mySourceLife.lifespan(), myListener);
      life.add(mySourceLife.getCurrentCycleDetach());
    }
    sync();
  }

  public void freeze() {
    myFrozen = true;
  }

  public void unfreeze() {
    myFrozen = false;
    sync();
  }

  private void sync() {
    if (myFrozen)
      return;
    AListModel<T> source = mySource == null ? AListModel.EMPTY : mySource;
    boolean prefRemove = myImage.getSize() > source.getSize();
    int i = 0, j = 0;
    while (i < source.getSize()) {
      T next = source.getAt(i++);
      int k = myImage.indexOf(next, j, myImage.getSize());
      if (k < 0) {
        myImage.insert(j, next);
      } else if (k > j) {
        if (prefRemove) {
          myImage.removeRange(j, k - 1);
          assert Util.equals(myImage.getAt(j), next);
        } else {
          myImage.insert(j, next);
        }
      } else {
        assert k == j : j + " " + k;
      }
      j++;
    }
    if (j < myImage.getSize()) {
      myImage.removeRange(j, myImage.getSize() - 1);
    }
  }

  public AListModel<T> getModel() {
    return myImage;
  }
}
