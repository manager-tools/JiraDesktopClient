package com.almworks.util.rt;

import com.almworks.util.ui.UIUtil;
import org.almworks.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Vasya
 */
public class MemStatusComponent extends JComponent implements MemoryStateGetter {
  private static final int REFRESH_PERIOD_MILLIS = 500;
  private static final float CHANGES_THRESHOLD = .02f;
  private static final String uiClassID = "MemStatusComponentUI";
  private static final MemoryState NULL_STATE = new MemoryState(0, 0, 0);

  private MemoryStateGetter myGetter;
  private Timer myTimer;
  private Color myIndicatorColor;
  private Color myPeakColor;

  static {
    // todo move to LAF extensions
    UIManager.put(uiClassID, MemStatusComponentUI.class.getName());
  }

  public MemStatusComponent() {
    this(new DefaultMemoryStateGetter());
  }

  public MemStatusComponent(MemoryStateGetter getter) {
    myGetter = getter;
    updateUI();
    initTimer();
  }

  private void initTimer() {
    myTimer = new Timer(REFRESH_PERIOD_MILLIS, new ActionListener() {
      private float myLastRatio = 0f;

      public void actionPerformed(ActionEvent e) {
        try {
          final MemoryState memoryState = getMemoryState();
          if (memoryState == null) {
            myLastRatio = 0;
          } else {
            final float ratio = memoryState.getUsedRatio();
            if (Math.abs(myLastRatio - ratio) > CHANGES_THRESHOLD) {
              repaint();
              myLastRatio = ratio;
            }
          }
        } catch (Exception ex) {
          Log.error(ex.getMessage(), ex);
        }
      }
    });
    myTimer.setRepeats(true);
  }

  public void addNotify() {
    super.addNotify();
    if (myTimer != null && !myTimer.isRunning())
      myTimer.start();
  }

  public void removeNotify() {
    super.removeNotify();
    if (myTimer != null && myTimer.isRunning())
      myTimer.stop();
  }

  public void updateUI() {
    setUI(UIManager.getUI(this));
  }

  public String getUIClassID() {
    return uiClassID;
  }

  public Dimension getPreferredSize() {
    int height = (int)(UIUtil.getLineHeight(this) * 1.4F);
    int width = height * 9 / 2;
    return new Dimension(width, height);
  }

  public MemoryState getMemoryState() {
    return myGetter != null ? myGetter.getMemoryState() : NULL_STATE;
  }

  public Color getIndicatorColor() {
    return myIndicatorColor;
  }

  public Color getPeakColor() {
    return myPeakColor;
  }

  public void setPeakColor(Color peakColor) {
    Color oldColor = myPeakColor;
    myPeakColor = peakColor;
    repaint();
    firePropertyChange("peakColor", oldColor, peakColor);
  }

  public void setIndicatorColor(Color indicatorColor) {
    Color oldColor = myIndicatorColor;
    myIndicatorColor = indicatorColor;
    repaint();
    firePropertyChange("indicatorColor", oldColor, indicatorColor);
  }
}
