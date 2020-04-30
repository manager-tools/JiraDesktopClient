package com.almworks.util.images;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.events.FireEventSupport;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.util.IdentityHashMap;
import java.util.Map;

public class AnimatedIconWatcher {
  public static final AnimatedIconWatcher INSTANCE = new AnimatedIconWatcher();

  private final Map<ImageIcon, Observer> myManagedIcons = new IdentityHashMap<ImageIcon, Observer>();

  private AnimatedIconWatcher() {}

  public void registerPainter(Icon icon, Lifespan life, ChangeListener painter) {
    if(icon instanceof ImageIcon) {
      final ImageIcon iicon = (ImageIcon)icon;
      synchronized(myManagedIcons) {
        Observer o = myManagedIcons.get(iicon);
        if(o == null) {
          o = new Observer();
          iicon.setImageObserver(o);
          myManagedIcons.put(iicon, o);
        }
        o.addListener(life, painter);
        life.add(new Detach() {
          @Override
          protected void doDetach() throws Exception {
            checkListeners(iicon);
          }
        });
      }
    }
  }

  private void checkListeners(ImageIcon iicon) {
    synchronized(myManagedIcons) {
      final Observer o = myManagedIcons.get(iicon);
      if(o != null && o.hasNoListeners()) {
        o.detach();
        myManagedIcons.remove(iicon);
        if(iicon.getImageObserver() == o) {
          iicon.setImageObserver(null);
        }
      }
    }
  }

  private static class Observer implements ImageObserver {
    private final FireEventSupport<ChangeListener> myEventSupport = FireEventSupport.create(ChangeListener.class);
    private volatile boolean myDetached = false;

    @Override
    public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
      if(myDetached) {
        return false;
      }

      if((flags & (FRAMEBITS | ALLBITS)) != 0) {
        myEventSupport.getDispatcherSnapshot().onChange();
      }

      return (flags & (ALLBITS | ABORT)) == 0;
    }

    public boolean hasNoListeners() {
      return myEventSupport.getListenersCount() == 0;
    }

    public void addListener(Lifespan life, ChangeListener painter) {
      myEventSupport.addAWTListener(life, painter);
    }

    public void detach() {
      myDetached = true;
    }
  }
}
