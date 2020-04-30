package com.almworks.timetrack.gui;

import com.almworks.timetrack.impl.TimeTrackingUtil;
import com.almworks.util.English;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.ADateField;
import com.almworks.util.components.DesignPlaceHolder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Const;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class ChangeEventTimeForm extends BaseAdjustmentForm<Long> {
  private static final DateFormat FULL_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

  private JSlider mySlider;
  private JPanel myWholePanel;
  private DesignPlaceHolder myTimePlace;
  private DesignPlaceHolder mySpentPlace;
  private JButton myButton1;
  private JButton myButton2;
  private JLabel myHeadLabel;
  private JLabel myAdditionalInfo;
  private JLabel myTimeLabel;

  private ADateField myTimeField = new ADateField(ADateField.Precision.DATE_TIME);
  private DurationField mySpentField;
  private DefaultBoundedRangeModel mySliderModel = new DefaultBoundedRangeModel();

  private static final long STEP = 5 * Const.MINUTE;
  private final ValueModel<Date> myDateModel = ValueModel.create();

  private final long myMinSliderTime;
  private final long myMaxSliderTime;
  private final long mySliderTimeRange;
  private final boolean myEndTime;
  private final TimePeriod myEditedPeriod;
  private final List<TimePeriod> myOtherPeriods;

  public ChangeEventTimeForm(boolean endTime, TimePeriod period, List<TimePeriod> others) {
    myEndTime = endTime;

    assert period != null;

    myEditedPeriod = period;
    myOtherPeriods = others;

    final long value = myEndTime ? myEditedPeriod.stopped : myEditedPeriod.started;

    myHeadLabel.setText(
      endTime ? "Adjust end time of the last work period:" :
        "Adjust start time of the current work period:"
    );
    myHeadLabel.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
    myAdditionalInfo.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
    UIUtil.setDefaultLabelAlignment(myWholePanel);
    Aqua.disableMnemonics(myWholePanel);

    final Pair<Long, Long> timeRange = getTimeRange();
    myMinSliderTime = timeRange.getFirst();
    myMaxSliderTime = timeRange.getSecond();
    mySliderTimeRange = myMaxSliderTime - myMinSliderTime;

    mySliderModel.setMinimum(0);
    mySliderModel.setMaximum((int)(mySliderTimeRange / STEP));
    mySliderModel.setExtent(0);
    mySlider.setModel(mySliderModel);

    myDateModel.setValue(new Date(value));
    myTimeField.setDateModel(myDateModel);

    linkModels();
    configureFields();
    calibrateLabels();
    calibrateTicks();
    configureShortcuts();

    com.almworks.util.collections.ChangeListener listener = new com.almworks.util.collections.ChangeListener() {
      public void onChange() {
        Date date = myDateModel.getValue();
        String info = getInfo(date == null ? 0L : date.getTime());
        if (!Util.equals(info, myAdditionalInfo.getText())) {
          myAdditionalInfo.setText(info);
        }
      }
    };
    myDateModel.addAWTChangeListener(listener);
  }

  /**
   * Configure keyboard shorcuts for the slider.
   */
  private void configureShortcuts() {
    final InputMap imap = mySlider.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK), "minScroll");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK), "maxScroll");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK), "negativeBlockIncrement");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), "positiveBlockIncrement");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "negativeUnitIncrement");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.SHIFT_DOWN_MASK), "negativeBlockIncrement");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK), "minScroll");

//    mySlider.getActionMap().put("leftmost", new AbstractAction() {
//      public void actionPerformed(ActionEvent e) {
//        mySliderModel.setValue(mySliderModel.getMinimum());
//      }
//    });
//    mySlider.getActionMap().put("rightmost", new AbstractAction() {
//      public void actionPerformed(ActionEvent e) {
//        mySliderModel.setValue(mySliderModel.getMaximum());
//      }
//    });
  }

  /**
   * Create labels for the slider marking start and stop times of
   * the current period and other periods. Puts the artifact id
   * between the markers, if there's space for it.
   */
  private void calibrateLabels() {
    final Hashtable labels = new Hashtable();
    labels.put(mySliderModel.getMinimum(), createMarker(myMinSliderTime, MarkerIcon.START));
    labels.put(mySliderModel.getMaximum(), createMarker(myMaxSliderTime, MarkerIcon.END));

    final long farEnough = Math.round(0.12d * mySliderTimeRange);

    addPeriod(labels, myEditedPeriod, farEnough);
    for(final TimePeriod p : myOtherPeriods) {
      addPeriod(labels, p, farEnough);
    }

    mySlider.setLabelTable(labels);
    mySlider.setPaintLabels(true);
  }

  /**
   * Adds marker labels for the given period. If there's
   * enough space, also adds period's artifact ID.
   * @param labels The label table.
   * @param period The period to add labels for.
   * @param farEnough The minimum period length to paint its id.
   */
  private void addPeriod(Hashtable labels, TimePeriod period, long farEnough) {
    if(!isOkForSlider(period.started) && !isOkForSlider(period.stopped)) {
      return;
    }

    final long min = Math.max(period.started, myMinSliderTime);
    final long max = period.stopped <= 0 ?
      myMaxSliderTime : Math.min(period.stopped, myMaxSliderTime);

    if(isOkForSlider(min)) {
      putMarker(labels, min, MarkerIcon.START);
    }

    if(isOkForSlider(max)) {
      putMarker(labels, max, MarkerIcon.END);
    }

    if(max - min >= farEnough) {
      putLabel(labels, (min + max) / 2, period.artifactId);
    }
  }

  /**
   * Puts a marker icon into the label table. If there's already
   * a different marker icon at this position, puts a SWITCH
   * marker instead.
   * @param labels The label table.
   * @param value The value.
   * @param icon The marker icon to use.
   */
  private void putMarker(Hashtable labels, long value, MarkerIcon icon) {
    final int sliderValue = timeToSlider(value);

    final Object oldMarker = labels.put(sliderValue, new JLabel(icon));
    if(oldMarker instanceof JLabel) {
      if(((JLabel)oldMarker).getIcon() != icon) {
        labels.put(sliderValue, new JLabel(MarkerIcon.SWITCH));
      }
    }
  }

  /**
   * Puts a textual label into the label table.
   * @param labels The label table.
   * @param value The value.
   * @param text The text.
   */
  private void putLabel(Hashtable labels, long value, String text) {
    labels.put(timeToSlider(value), createMarker(text, null));
  }

  /**
   * Determines whether the given value is within
   * the slider range.
   * @param value The value.
   * @return {@code true} if {@code value} is between
   * the minimum and maximum slider times, exclusive.
   */
  private boolean isOkForSlider(long value) {
    return value > myMinSliderTime && value < myMaxSliderTime;
  }

  /**
   * Set major and minor tick spacing for the slider
   * depending on the displayed range.
   */
  private void calibrateTicks() {
    final int[][] spacings = {{3, 1}, {6, 2}, {12, 3}};

    int i;
    for(i = 0; i < spacings.length - 1; i++) {
      if(mySliderModel.getMaximum() / spacings[i][0] <= 10) {
        break;
      }
    }

    mySlider.setPaintTicks(true);
    mySlider.setMajorTickSpacing(spacings[i][0]);
    mySlider.setMinorTickSpacing(spacings[i][1]);
  }

  /**
   * Calculate the consequences of moving the edited event
   * to a particular time, and return the string describing
   * the required adjustments.
   * @param time The event time.
   * @return The string describing the consequences.
   */
  private String getInfo(long time) {
    final long originalTime;
    final boolean canAdjustOthers;

    if(myEndTime) {
      originalTime = myEditedPeriod.stopped;
      canAdjustOthers = time > originalTime;
    } else {
      originalTime = myEditedPeriod.started;
      canAdjustOthers = time < originalTime;
    }

    if(canAdjustOthers) {
      final Pair<List<TimePeriod>, TimePeriod> listPair =
        TimePeriod.getAdjustments(myOtherPeriods, originalTime, time);

      final StringBuilder result = new StringBuilder();
      result.append("<html>");

      final List<TimePeriod> removed = listPair.getFirst();
      if(!removed.isEmpty()) {
        final int size = removed.size();
        result.append(size).append(" ")
          .append(English.getSingularOrPlural("entry", size))
          .append(" removed (").append(TimePeriod.getArtifactIds(removed, 3))
          .append(")<br>");
      }

      final TimePeriod adjusted = listPair.getSecond();
      if(adjusted != null) {
        if(adjusted.canMergeWith(myEditedPeriod)) {
          result.append("2 entries merged (").append(adjusted.artifactId).append(")");
        } else {
          result.append("1 entry adjusted (").append(adjusted.artifactId).append(")");
        }
      }

      return removed.isEmpty() && adjusted == null ? "<html>None required" : result.toString();
    } else {
      final TimePeriod.AdjustmentStatus status = myEditedPeriod.getAdjustmentStatus(originalTime, time);
      switch (status) {
      case REMOVED:
        return "<html>Current entry removed (" + myEditedPeriod.artifactId + ")";
      default:
        return "<html>None required";
      }
    }
  }

  private void linkModels() {
    final class MyListener implements
      ChangeListener, com.almworks.util.collections.ChangeListener, Procedure<Integer>
    {
      boolean updating = false;

      public void stateChanged(ChangeEvent e) {
        if (!updating) {
          updating = true;
          try {
            myDateModel.setValue(new Date(sliderToTime(mySliderModel.getValue())));
            updateSpent();
          } finally {
            updating = false;
          }
        }
      }

      public void onChange() {
        if (!updating) {
          updating = true;
          try {
            final Date date = myDateModel.getValue();
            if (date != null) {
              mySliderModel.setValue(timeToSlider(date.getTime()));
              updateSpent();
            }
          } finally {
            updating = false;
          }
        }
      }

      public void invoke(Integer arg) {
        if (arg != null && !updating) {
          updating = true;
          try {
            final long millis = TimeTrackingUtil.millis(arg);
            final long moment;

            if(myEndTime) {
              moment = myEditedPeriod.started + millis;
            } else {
              moment = System.currentTimeMillis() - millis;
            }

            myDateModel.setValue(new Date(moment));
            mySliderModel.setValue(timeToSlider(moment));
          } finally {
            updating = false;
          }
        }
      }

      private void updateSpent() {
        final long start;
        final long end;

        if(myEndTime) {
          start = myEditedPeriod.started;
          end = myDateModel.getValue().getTime();
        } else {
          start = myDateModel.getValue().getTime();
          end = System.currentTimeMillis();
        }

        mySpentField.setSeconds(TimeTrackingUtil.seconds(end - start));
      }
    }

    final MyListener myListener = new MyListener();
    mySliderModel.addChangeListener(myListener);
    myDateModel.addAWTChangeListener(myListener);
    mySpentField = new DurationField(myListener);
    myListener.onChange();
  }

  private void configureFields() {
    myTimePlace.setComponent(myTimeField);
    mySpentField.setColumns(10);
    mySpentPlace.setComponent(mySpentField);
    if (myEndTime) {
      myTimeLabel.setText("Work stopped:");
    } else {
      myTimeLabel.setText("Work started:");
    }
  }

  protected void doCancel(Procedure<Long> proc) {
    proc.invoke(null);
  }

  protected void doOk(Procedure<Long> proc) {
    final Date date = myDateModel.getValue();
    if (date == null) {
      proc.invoke(null);
    } else {
      proc.invoke(
        Math.min(date.getTime(), System.currentTimeMillis()));
    }
  }

  protected Pair<JButton, JButton> getButtons() {
    return Pair.create(myButton1, myButton2);
  }

  protected JComponent getFocusGrabber() {
    return mySlider;
  }

  protected JPanel getMainPanel() {
    return myWholePanel;
  }

  public void attach(final Procedure<Long> procedure) {
    myAdditionalInfo.setText(getInfo(myDateModel.getValue().getTime()));
    super.attach(procedure);
  }

  private void createUIComponents() {
    mySlider = new JSlider() {
      @Override
      public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
        UIUtil.requestFocusLater(this);
      }
    };
  }

  /**
   * Calculates and returns the time range for the slider,
   * based on some work day start time and now.
   * @return The pair of long timestamps: (start; end).
   */
  private static Pair<Long, Long> getTimeRange() {
    final long now = System.currentTimeMillis() / STEP * STEP + STEP;

    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 8); // todo: configurable starting time?
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    long start = cal.getTimeInMillis();
    while(now - start < Const.HOUR) {
      start -= Const.HOUR;
    }

    return Pair.create(start, now);
  }

  private int timeToSlider(long time) {
    return (int)((time - myMinSliderTime) / STEP);
  }

  private long sliderToTime(int slider) {
    return slider * STEP + myMinSliderTime;
  }

  /**
   * Icons used for time markers on the slider.
   */
  private static enum MarkerIcon implements Icon {
    /** "Period start" marker. */
    START {
      protected void paintBottomLine(Graphics g, int x, int y, int xCenter) {
        g.drawLine(xCenter, y, x + ICON_WIDTH, y);
      }
    },

    /** "Task switched" marker. */
    SWITCH
      {
      protected void paintBottomLine(Graphics g, int x, int y, int xCenter) {
        g.drawLine(x, y, x + ICON_WIDTH, y);
      }
    },

    /** "Period end" marker. */
    END {
      protected void paintBottomLine(Graphics g, int x, int y, int xCenter) {
        g.drawLine(x, y, xCenter, y);
      }
    };

    /** The icon height. */
    private static final int ICON_HEIGHT = 5;

    /** The icon width. */
    private static final int ICON_WIDTH = 8;

    public int getIconWidth() {
      return ICON_WIDTH;
    }

    public int getIconHeight() {
      return ICON_HEIGHT;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(Aqua.MAC_BORDER_COLOR); // todo: find a better color, though on Mac it matches perfectly.

      final int xCenter = x + ICON_WIDTH / 2;
      g.drawLine(xCenter, y, xCenter, y + ICON_HEIGHT);
      paintBottomLine(g, x, y + ICON_HEIGHT, xCenter);
    }

    /** Icon-specific method to paint the bottom line. */
    protected abstract void paintBottomLine(Graphics g, int x, int y, int xCenter);
  }

  /**
   * Create a marker component.
   * @param text The text to use as the label.
   * @param icon The icon to use.
   * @return The marker component.
   */
  private static JComponent createMarker(String text, Icon icon) {
    final JLabel label = new JLabel();

    label.setText(text);
    label.setIcon(icon);

    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setHorizontalTextPosition(SwingConstants.CENTER);
    label.setVerticalTextPosition(SwingConstants.BOTTOM);
    label.setIconTextGap(4);

    if(!Aqua.isAqua()) {
      UIUtil.adjustFont(label, 0.75f, 0, false);
    } else {
      Aqua.makeMiniComponent(label);
    }

    return label;
  }

  /**
   * Create a marker component.
   * @param time The timestamp in milliseconds to use as the label.
   * @param icon The icon to use.
   * @return The marker component.
   */
  private static JComponent createMarker(long time, Icon icon) {
    return createMarker(DateUtil.toLocalDateOrTime(new Date(time)), icon);
  }
}
