package com.almworks.timetrack.gui;

import com.almworks.timetrack.api.TimeTrackerSettings;
import com.almworks.util.Env;
import com.almworks.util.ui.ComponentEnabler;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

public class TimeTrackingOptionsForm {
  private JCheckBox myEnableAutoPause;
  private JPanel myWholePanel;
  private JSpinner myIdlePeriod;
  private JSpinner myIgnoreActivityThreshold;
  private JCheckBox myNotification;
  private JCheckBox myAlwaysOnTop;
  private JLabel myAPIdleLabel;
  private JLabel myAPIdleMin;
  private JLabel myAPIgnoreLabel;
  private JLabel myAPIgnoreSec;

  private final SpinnerNumberModel myIdlePeriodModel = new SpinnerNumberModel(1, 1, 9999, 1);
  private final SpinnerNumberModel myIgnoreActivityThresholdModel = new SpinnerNumberModel(1, 1, 9999, 1);

  public TimeTrackingOptionsForm() {
    setSpinnerSize(myIdlePeriod);
    setSpinnerSize(myIgnoreActivityThreshold);
    myIdlePeriod.setModel(myIdlePeriodModel);
    myIgnoreActivityThreshold.setModel(myIgnoreActivityThresholdModel);
    ComponentEnabler.create(myEnableAutoPause, myAPIdleLabel, myIdlePeriod, myAPIdleMin, myAPIgnoreLabel,
      myIgnoreActivityThreshold, myAPIgnoreSec, myNotification);
    myAlwaysOnTop.setVisible(!Env.isMac());
  }

  private void setSpinnerSize(JSpinner spinner) {
    JComponent e = spinner.getEditor();
    if (e != null) {
      Dimension dim = UIUtil.getRelativeDimension(e, 4, 1);
      Dimension ps = e.getPreferredSize();
      if (ps != null)
        dim.height = ps.height;
      e.setPreferredSize(dim);
    }
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void loadFrom(TimeTrackerSettings settings) {
    myEnableAutoPause.setSelected(settings.isAutoPauseEnabled());
    myIdlePeriodModel.setValue(settings.getAutoPauseTimeValue());
    myIgnoreActivityThresholdModel.setValue(settings.getFalseResumeTimeoutValue());
    myNotification.setSelected(settings.isNotifyUser());
    myAlwaysOnTop.setSelected(settings.isAlwaysOnTop() || Env.isMac());
  }

  public Component getInitialFocusOwner() {
    return myEnableAutoPause;
  }

  public void saveTo(TimeTrackerSettings settings) {
    settings.setAutoPauseEnabled(myEnableAutoPause.isSelected());
    settings.setNotifyUser(myNotification.isSelected());
    settings.setAutoPauseTimeValue(getInt(myIdlePeriodModel));
    settings.setFalseResumeTimeoutValue(getInt(myIgnoreActivityThresholdModel));
    settings.setAlwaysOnTop(myAlwaysOnTop.isSelected() || Env.isMac());
  }

  private int getInt(SpinnerNumberModel model) {
    Object value = model.getValue();
    if (!(value instanceof Number))
      return 0;
    return ((Number) value).intValue();
  }
}
