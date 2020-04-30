package com.almworks.timetrack.gui;

import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.DesignPlaceHolder;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;

/**
 * The form used to change the remaining time estimate in the Time Tracker.
 */
public class ChangeRemainingTimeForm extends BaseAdjustmentForm<Integer> {
  public static final int CANCEL = -1;
  public static final int REVERT = -3;

  private JPanel myWholePanel;
  private DesignPlaceHolder myEstimatePlace;
  private JButton myButton1;
  private JButton myButton2;

  private DurationField myEstimateField;

  public ChangeRemainingTimeForm(Integer currentEst) {
    initField(currentEst);
  }

  private void initField(Integer currentEst) {
    myEstimateField = new DurationField() {
      @Override
      public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
        UIUtil.requestFocusLater(this);
      }
    };
    myEstimateField.setColumns(10);
    if(currentEst != null) {
      myEstimateField.setSeconds(currentEst);
    }
    myEstimatePlace.setComponent(myEstimateField);
  }

  protected void doCancel(Procedure<Integer> proc) {
    proc.invoke(CANCEL);
  }

  protected void doOk(Procedure<Integer> proc) {
    final int seconds = myEstimateField.getSeconds();
    if(seconds < 0) {
      if(myEstimateField.getText().trim().length() == 0) {
        proc.invoke(REVERT);
      } else {
        proc.invoke(CANCEL);
      }
    } else {
      proc.invoke(seconds);
    }
  }

  protected Pair<JButton, JButton> getButtons() {
    return Pair.create(myButton1, myButton2);
  }

  protected JComponent getFocusGrabber() {
    return myEstimateField;
  }

  protected JPanel getMainPanel() {
    return myWholePanel;
  }
}
