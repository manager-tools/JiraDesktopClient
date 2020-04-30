package com.almworks.util.collections;

import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Fires updates each specified period of time repeatedly.
 * */
public class TimedModifiable extends SimpleModifiable {
  private final javax.swing.Timer myTimer;

  public TimedModifiable(int period, Lifespan life) {
    this(period, period, life);
  }

  public TimedModifiable(int initialDelay, int period, Lifespan life) {
    if (life.isEnded()) {
      myTimer = null;
    } else {
      myTimer = new javax.swing.Timer(period, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          fireChanged();
        }
      });
      myTimer.setRepeats(true);
      myTimer.setCoalesce(true);
      myTimer.setInitialDelay(initialDelay);
      life.add(new Detach() {
        @Override
        protected void doDetach() throws Exception {
          myTimer.stop();
        }
      });
      myTimer.start();
    }
  }
}
