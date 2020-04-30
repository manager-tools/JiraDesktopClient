package com.almworks.explorer.qbuilder.filter;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ADateField;
import com.almworks.util.components.ButtonSelectedListener;
import com.almworks.util.ui.ComponentEnabler;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author dyoma
 */
public class DateBoundController {
  @NotNull
  private final JTextComponent myFromNow;
  @NotNull
  private final ADateField myAbsolute;
  private final AComboboxModel<DateUnit> myUnitsModel;
  private final JRadioButton myAbsoluteRadion;
  private final JRadioButton myRelativeRadio;
  private final boolean myLaterBound;

  public DateBoundController(@NotNull JRadioButton relative, @NotNull JRadioButton absolute,
    @NotNull JTextComponent fromNow, @NotNull AComboBox<DateUnit> units, @NotNull ADateField absoluteDate, boolean laterBound)
  {
    myRelativeRadio = relative;
    myFromNow = fromNow;
    myAbsolute = absoluteDate;
    myLaterBound = laterBound;

    myUnitsModel = units.getModel();
    ComponentEnabler.create(myRelativeRadio, myFromNow, units);
    myAbsoluteRadion = absolute;
    ComponentEnabler.create(myAbsoluteRadion, (JComponent) absoluteDate);
    UIUtil.transferFocus(myRelativeRadio, myFromNow);
    UIUtil.transferFocus(myAbsoluteRadion, absoluteDate.getDefaultFocusComponent());
    new ButtonSelectedListener(myAbsoluteRadion) {
      protected void selectionChanged() {
        adjustRelative();
      }
    }.attach();
    new ButtonSelectedListener(myRelativeRadio) {
      protected void selectionChanged() {
        adjustDateToRelative();
      }
    }.attach();
    JointChangeListener adjustDateToRelativeListener = new JointChangeListener() {
      protected void processChange() {
        adjustDateToRelative();
      }
    };
    myUnitsModel.addAWTChangeListener(Lifespan.FOREVER, adjustDateToRelativeListener);
    UIUtil.addTextListener(Lifespan.FOREVER, myFromNow, adjustDateToRelativeListener);
    absoluteDate.getDateModel().addAWTChangeListener(Lifespan.FOREVER,
      new JointChangeListener(adjustDateToRelativeListener.getUpdateFlag()) {
        protected void processChange() {
          adjustRelative();
        }
      });
    units.setCanvasRenderer(DateUnit.RENDERER);
    if (!myAbsoluteRadion.isSelected() && !myRelativeRadio.isSelected())
      myRelativeRadio.setSelected(true);
    adjustDates();
  }

  private void adjustDateToRelative() {
    if (!isRelativeMode())
      return;
    DateUnit unit = myUnitsModel.getSelectedItem();
    Integer relative = getRelative();
    if (relative == null || unit == null)
      return;
    Calendar calendar = getNow();
    unit.subtract(calendar, relative, myLaterBound);
    setDate(calendar.getTime());
  }

  private Calendar getNow() {
    return Calendar.getInstance(Locale.getDefault());
  }

  private void adjustRelative() {
    Date date = myAbsolute.getDateModel().getValue();
    if (!isAbsoluteMode() || date == null)
      return;
    DateUnit unit = myUnitsModel.getSelectedItem();
    if (unit == null)
      return;
    Calendar calendar = getNow();
    calendar.setTime(date);
    int relative = unit.getDifference(getNow(), calendar);
    UIUtil.setFieldText(myFromNow, String.valueOf(relative));
  }

  private boolean isRelativeMode() {
    return myRelativeRadio.isSelected();
  }

  private boolean isAbsoluteMode() {
    return myAbsoluteRadion.isSelected();
  }

  private void setDate(Date time) {
    myAbsolute.setDate(time);
  }

  @Nullable
  private Integer getRelative() {
    String text = myFromNow.getText();
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public void adjustDates() {
    adjustDateToRelative();
    adjustRelative();
  }
}
