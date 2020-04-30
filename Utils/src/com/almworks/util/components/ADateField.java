package com.almworks.util.components;

import com.almworks.util.L;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.UndoUtil;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.freixas.jcalendar.DateEvent;
import org.freixas.jcalendar.DateListener;
import org.freixas.jcalendar.JCalendar;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.undo.CompoundEdit;
import java.awt.*;
import java.text.DateFormat;
import java.util.*;

import static com.almworks.util.ui.SingleChildLayout.CONTAINER;
import static com.almworks.util.ui.SingleChildLayout.PREFERRED;

/**
 * @author dyoma
 */
public class ADateField extends JComponent implements UndoUtil.Editable {
  private final FieldWithMoreButton<JTextField> myField = new FieldWithMoreButton<JTextField>();
  private final Precision myPrecision;
  private final DateFormat myFormat;
  private final FireEventSupport<UndoableEditListener> myUndoListeners =
    FireEventSupport.create(UndoableEditListener.class);
  private final Lifecycle mySwingLife = new Lifecycle(false);
  private final TimeZone myTimezone;

  private CompoundEdit myCompoundEdit = null;
  private ValueModel<Date> myDateModel = ValueModel.create();
  
  public ADateField(Precision precision) {
    this(precision, TimeZone.getDefault());
  }
  
  public ADateField(Precision precision, TimeZone timeZone) {
    myPrecision = precision;
    myTimezone = timeZone;
    myFormat = precision.createFormat(timeZone);
    myField.setField(new JTextField());
    setLayout(new SingleChildLayout(CONTAINER, PREFERRED, CONTAINER, PREFERRED));
    add(myField);
    myField.setActionName(L.tooltip("Open Calendar"));
    myField.setAction(new MyDropDown());
    updateUI();
  }

  public void addNotify() {
    super.addNotify();
    start();
  }

  private void start() {
    if (mySwingLife.cycleStart()) {
      linkModel();
      setupUndo();
    }
  }

  public void removeNotify() {
    mySwingLife.cycleEnd();
    super.removeNotify();
  }

  private void linkModel() {
    Lifespan life = mySwingLife.lifespan();
    assert !life.isEnded() : this;
    final boolean[] updating = {true};
    final JTextField textField = myField.getField();

    final Color normalFg = textField.getForeground();
    final Color errorFg = GlobalColors.ERROR_COLOR;
    UIUtil.addTextListener(life, textField, new ChangeListener() {
      public void onChange() {
        if (updating[0])
          return;
        updating[0] = true;
        try {
          boolean error = false;
          boolean hasDate = true;
          String text = textField.getText().trim();
          Date date = null;
          if (text.length() > 0) {
            myFormat.setLenient(false);
            try {
              myFormat.parse(text);
            } catch (Exception e) {
              error = true;
            }
            myFormat.setLenient(true);
            try {
              date = myFormat.parse(text);
            } catch (Exception e) {
              // ignore
              hasDate = false;
            }
          }
          textField.setForeground(error ? errorFg : normalFg);
          if (hasDate) {
            myDateModel.setValue(myPrecision.getInstant(date, myTimezone));
          }
        } finally {
          updating[0] = false;
        }
      }
    });
    ChangeListener modelListener = new ChangeListener() {
      public void onChange() {
        if (updating[0])
          return;
        updating[0] = true;
        try {
          Date value = myDateModel.getValue();
          String text = value == null ? "" : myFormat.format(value);
          String oldText = myField.getField().getText().trim();
          if (!oldText.equals(text)) {
            UIUtil.setTextKeepView(myField.getField(), text);
          }
        } finally {
          updating[0] = false;
        }
      }
    };
    myDateModel.addChangeListener(life, modelListener);
    updating[0] = false;
    modelListener.onChange();
  }

  private void setupUndo() {
    final Document document = myField.getField().getDocument();
    final UndoableEditListener listener = new UndoableEditListener() {
      public void undoableEditHappened(UndoableEditEvent e) {
        if (myCompoundEdit == null)
          myUndoListeners.getDispatcher().undoableEditHappened(e);
        else
          myCompoundEdit.addEdit(e.getEdit());
      }
    };
    document.addUndoableEditListener(listener);
    UndoUtil.removeUndoSupport(myField.getField());
    UndoUtil.CUSTOM_EDITABLE.putClientValue(myField.getField(), this);
    UndoUtil.addUndoSupport(myField.getField());
    mySwingLife.lifespan().add(new Detach() {
      protected void doDetach() throws Exception {
        document.removeUndoableEditListener(listener);
        UndoUtil.addUndoSupport(myField.getField());
      }
    });
  }

  public void updateUI() {
    super.updateUI();
    JComponent field = myField.getField();
    Dimension prefSize = field.getPreferredSize();
    int sampleWidth = field.getFontMetrics(field.getFont()).stringWidth(myFormat.format(new Date()));
    field.setPreferredSize(new Dimension(sampleWidth * 5 / 4 + AwtUtil.getInsetWidth(field), prefSize.height));
  }

  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    myField.setEnabled(enabled);
  }

  public void addUndoableEditListener(UndoableEditListener listener) {
    myUndoListeners.addStraightListener(listener);
  }

  public void removeUndoableEditListener(UndoableEditListener listener) {
    myUndoListeners.removeListener(listener);
  }

  public int getColumns() {
    return myField.getField().getColumns();
  }

  public void setColumns(int columns) {
    myField.getField().setColumns(columns);
  }

  /** @param date will be stored with modified value to fit with the rules in {@link #getDateModel()}  */
  public void setDate(@Nullable Date date) {
//    String text = date == null ? "" : myFormat.format(date);
    assert myCompoundEdit == null;
    myCompoundEdit = new CompoundEdit();
    myDateModel.setValue(myPrecision.getInstant(date, myTimezone));
//    UIUtil.setFieldText(myField.getField(), text);
    myCompoundEdit.end();
    myUndoListeners.getDispatcher()
      .undoableEditHappened(new UndoableEditEvent(myField.getField().getDocument(), myCompoundEdit));
    myCompoundEdit = null;
  }

  /** Sets day value, preserving more precise information (time of day) if required by the {@link #myPrecision precision}. */
  public void setDay(Date instantOnDay) {
    myDateModel.setValue(myPrecision.changeDay(myDateModel.getValue(), instantOnDay, myTimezone));
  }

  public DateFormat getFormat() {
    return myFormat;
  }

  /** @return ValueModel with instant that corresponds to:
   * <pre> 
   * <table border="1">
   *   <tr><th>Precision</th><th>Instant</th></tr>
   *   <tr><td>{@link Precision#DAY Day}</td><td>{@link DateUtil#toDayStart Start of day}</td></tr>
   *   <tr><td>{@link Precision#DATE_TIME Day and time of the day}</td><td>Instant = day (+) time of day. 
   *   If the user changes only day, we strive to preserve time of day if possible. 
   *   (It is not possible if on the specific day there was no specified time of day, e.g. due to DST transition.)</td></tr>
   * </table>
   * </pre>
   * */
  public ValueModel<Date> getDateModel() {
    return myDateModel;
  }

  /** @param model to see what value can be contained there, see {@link #getDateModel()}. The value that is already contained there is modified according to these rules. */
  public void setDateModel(ValueModel<Date> model) {
    if (model == null)
      throw new NullPointerException();
    myDateModel = model;
    model.setValue(myPrecision.getInstant(model.getValue(), myTimezone));
    if (mySwingLife.cycleEnd()) {
      start();
    }
  }

  public JComponent getDefaultFocusComponent() {
    return myField.getField();
  }

  private class MyDropDown extends DropDownListener.ForComponent {
    public MyDropDown() {
      super(ADateField.this.myField);
    }

    protected JComponent createPopupComponent() {
      Date date = getDateModel().getValue();
      // todo http://snow:10500/browse/JC-645 we can specify selectedComponents for time of day
      JCalendar component = new JCalendar(new GregorianCalendar(myTimezone), Locale.getDefault(), JCalendar.DISPLAY_DATE, true);
      component.setBackground(UIManager.getColor("TextField.background"));
      component.setBorder(UIManager.getBorder("PopupMenu.border"));
      if (date != null) {
        component.setDate(date);
      } else {
        component.setNullAllowed(true);
        component.setDate(null);
        component.setNullAllowed(false);
      }
      component.addDateListener(new DateListener() {
        public void dateChanged(DateEvent e) {
          Date date = null;
          if (e != null) {
            Calendar selectedDate = e.getSelectedDate();
            if (selectedDate != null) {
              date = selectedDate.getTime();
            }
          }
          setDay(date);
          hideDropDown();
        }
      });
      return component;
    }

    protected void onDropDownHidden() {
      UIUtil.requestFocusLater(myField.getField());
    }
  }
  
  public enum Precision {
    DAY(DateUtil.LOCAL_DATE) {
      @Override
      public Date getInstant(Date value, TimeZone tz) {
        return value == null ? null : new Date(DateUtil.toDayStart(value.getTime(), tz));
      }

      @Override
      public Date changeDay(Date oldInstant, Date newDay, TimeZone tz) {
        return getInstant(newDay, tz);
      }
    },
    DATE_TIME(DateUtil.LOCAL_DATE_TIME) {
      @Override
      public Date getInstant(Date value, TimeZone tz) {
        return value;
      }

      @Override
      public Date changeDay(Date oldInstant, Date newInstant, TimeZone tz) {
        if (newInstant == null) return null;
        if (oldInstant == null) return newInstant;
        Calendar o = new GregorianCalendar(tz);
        Calendar n = new GregorianCalendar(tz);
        o.setTime(oldInstant);
        n.setTimeInMillis(DateUtil.toDayStart(newInstant.getTime(), tz));
        n.set(Calendar.HOUR_OF_DAY, o.get(Calendar.HOUR_OF_DAY));
        n.set(Calendar.MINUTE, o.get(Calendar.MINUTE));
        n.set(Calendar.SECOND, o.get(Calendar.SECOND));
        return n.getTime();
      }
    };

    private final DateFormat myFormatTemplate;

    private Precision(DateFormat formatTemplate) {
      myFormatTemplate = formatTemplate;
    }

    public DateFormat createFormat(TimeZone tz) {
      DateFormat format = (DateFormat)myFormatTemplate.clone();
      format.setTimeZone(tz);
      return format;
    }

    @Nullable
    public abstract Date getInstant(@Nullable Date value, TimeZone tz);

    public abstract Date changeDay(Date oldInstant, Date newDay, TimeZone tz);
  }
}
