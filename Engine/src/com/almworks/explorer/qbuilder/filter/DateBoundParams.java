package com.almworks.explorer.qbuilder.filter;

import com.almworks.explorer.qbuilder.constraints.AbstractConstraintEditor;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ADateField;
import com.almworks.util.model.ValueModel;
import com.almworks.util.properties.BooleanPropertyKey;
import com.almworks.util.properties.PropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.ComponentEnabler;
import com.almworks.util.ui.ComponentKeyBinder;
import com.almworks.util.ui.ModelMapBinding;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.Document;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author dyoma
 */
class DateBoundParams {
  public static final DateBoundParams BEFORE = new DateBoundParams("before", true);
  public static final DateBoundParams AFTER = new DateBoundParams("after", false);
  private final BooleanPropertyKey myEnabled;
  private final BooleanPropertyKey myAbsolute;
  private final PropertyKey<ValueModel<Date>, Date> myDate;
  private final PropertyKey<Document, String> myRelative;
  private final ComboBoxConstraintKey<DateUnit> myUnit;
  private final List<PropertyKey<?, ?>> myAllKeys;
  private final boolean myLaterBound;

  public DateBoundParams(String prefix, boolean laterBound) {
    myLaterBound = laterBound;
    myEnabled = BooleanPropertyKey.createKey(prefix + ".enabled", false);
    myAbsolute = BooleanPropertyKey.createKey(prefix + ".absolute", true);
    myDate = PropertyKey.createDate(prefix + ".date");
    myRelative = PropertyKey.createText(prefix + ".relative");
    myUnit = new ComboBoxConstraintKey<DateUnit>(prefix) {
      @NotNull
      protected AListModel<DateUnit> getVariantsModel() {
        return DateUnit.ALL_UNITS;
      }

      protected DateUnit getDefaultSelection() {
        return DateUnit.DAY;
      }
    };
    List<PropertyKey<?, ?>> allKeys = Collections15.arrayList();
    allKeys.add(myEnabled);
    allKeys.add(myRelative);
    allKeys.add(myAbsolute);
    allKeys.add(myUnit);
    allKeys.add(myDate);
    myAllKeys = Collections.unmodifiableList(allKeys);
  }

  public boolean isLaterBound() {
    return myLaterBound;
  }

  public void install(ComponentKeyBinder editor, Components components) {
    if (components.myUsed != null)
      editor.setBoolean(myEnabled, components.myUsed);
    editor.setBoolean(myAbsolute, components.myAbsoluteRadio);
    editor.setInvertedBoolean(myAbsolute, components.myRelativeRadio);
    editor.setCombobox(myUnit, components.myUnits);
    editor.setDocument(myRelative, components.myRelative);
    editor.setDate(myDate, components.myAbsolute); 
    components.attachListeners(myLaterBound);
  }

  public boolean isModified(AbstractConstraintEditor editor) {
    if (editor.wasChanged(myEnabled))
      return true;
    if (!editor.getBooleanValue(myEnabled))
      return false;
    if (editor.wasChanged(myAbsolute))
      return true;
    return editor.getBooleanValue(myAbsolute) ? editor.wasChanged(myDate) : editor.wasChanged(myRelative, myUnit);
  }

  @Nullable
  public DateUnit.DateValue getValue(ModelMapBinding editor) {
    if (!editor.getBooleanValue(myEnabled))
      return null;
    boolean absolute = editor.getBooleanValue(myAbsolute);
    if (absolute) {
      return DateUnit.AbsoluteDate.fromDate(editor.getValue(myDate));
    } else {
      int value = 0;
      String s = editor.getValue(myRelative);
      if (s != null && s.length() > 0) {
        try {
          value = Integer.parseInt(s);
        } catch (NumberFormatException e) {
          Log.debug(e);
        }
      }
      return new DateUnit.RelativeDate(value, editor.getValue(myUnit), myLaterBound);
    }
  }

  @Nullable
  public String getDisplayString(ModelMapBinding editor) {
    if (!editor.getBooleanValue(myEnabled))
      return null;
    if (editor.getBooleanValue(myAbsolute)) {
      Date date = editor.getValue(myDate);
      return date == null ? null : DateUnit.AbsoluteDate.USER_FORMAT.format(date);
    } else {
      String relative = editor.getValue(myRelative);
      DateUnit unit = editor.getValue(myUnit);
      return unit.getDisplayString(relative);
    }
  }

  public boolean isAbsolute(ModelMapBinding editor) {
    return editor.getBooleanValue(myAbsolute);
  }

  public void setInitialValue(@Nullable DateUnit.DateValue date, PropertyMap values) {
    if (date == null) {
      myEnabled.setInitialValue(values, Boolean.FALSE);
      return;
    }
    myEnabled.setInitialValue(values, Boolean.TRUE);
    //noinspection ChainOfInstanceofChecks
    if (date instanceof DateUnit.AbsoluteDate) {
      myAbsolute.setInitialValue(values, Boolean.TRUE);
      myDate.setInitialValue(values, ((DateUnit.AbsoluteDate) date).getDate());
    } else if (date instanceof DateUnit.RelativeDate) {
      DateUnit.RelativeDate relative = (DateUnit.RelativeDate) date;
      myAbsolute.setInitialValue(values, Boolean.FALSE);
      myRelative.setInitialValue(values, String.valueOf(relative.getRelative()));
      myUnit.setInitialValue(values, relative.getRelativeUnit());
    } else
      assert false : date.getClass().getName();
  }

  public boolean isEnabled(ModelMapBinding binding) {
    return binding.getBooleanValue(myEnabled);
  }

  public void getCurrentValuesFrom(DateBoundParams other, ModelMapBinding from, PropertyMap to) {
    for (int i = 0; i < myAllKeys.size(); i++)
      copyValueFrom(other, from, to, i);
  }

  private <V> void copyValueFrom(DateBoundParams other, ModelMapBinding from, PropertyMap to, int index) {
    PropertyKey<?, V> selfKey = (PropertyKey<?, V>) myAllKeys.get(index);
    PropertyKey<?, V> otherKey = (PropertyKey<?, V>) other.myAllKeys.get(index);
    to.put(selfKey.getValueKey(), from.getValue(otherKey));
  }

  public void getValuesFrom(DateBoundParams other, PropertyMap from, ModelMapBinding to) {
    for (int i = 0; i < myAllKeys.size(); i++)
      copyValueFrom(other, from, to, i);
  }

  private <V> void copyValueFrom(DateBoundParams other, PropertyMap from, ModelMapBinding to, int index) {
    PropertyKey<?, V> selfKey = (PropertyKey<?, V>) myAllKeys.get(index);
    PropertyKey<?, V> otherKey = (PropertyKey<?, V>) other.myAllKeys.get(index);
    TypedKey<V> otherValueKey = otherKey.getValueKey();
    if (!from.containsKey(otherValueKey))
      return;
    to.setModelValue(selfKey, from.get(otherValueKey));
  }

  public void setEnabled(ModelMapBinding binding) {
    binding.setModelValue(myEnabled, true);
  }

  static class Components {
    @Nullable
    private final JToggleButton myUsed;
    private final JRadioButton myRelativeRadio;
    private final JRadioButton myAbsoluteRadio;
    private final JTextField myRelative;
    private final AComboBox<DateUnit> myUnits;
    private final ADateField myAbsolute;
    private DateBoundController myController;

    public Components(@Nullable JToggleButton used, JRadioButton relativeRadio, JRadioButton absoluteRadio,
      JTextField relative, AComboBox<DateUnit> units, ADateField absolute)
    {
      myUsed = used;
      myRelativeRadio = relativeRadio;
      myAbsoluteRadio = absoluteRadio;
      myRelative = relative;
      myUnits = units;
      myAbsolute = absolute;
      if (myUsed != null) 
        ComponentEnabler.create(myUsed, myRelativeRadio, myAbsoluteRadio);
    }

    public void attachListeners(boolean laterBound) {
      myController =
        new DateBoundController(myRelativeRadio, myAbsoluteRadio, myRelative, myUnits, myAbsolute, laterBound);
    }

    public void adjustDates() {
      assert myController != null;
      myController.adjustDates();
    }
  }
}
