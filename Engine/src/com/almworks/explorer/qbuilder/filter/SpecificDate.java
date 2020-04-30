package com.almworks.explorer.qbuilder.filter;

import com.almworks.util.AppBook;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ADateField;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.ModelMapDialogEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author dyoma
 */
public class SpecificDate extends ModelMapDialogEditor {
  public static final DateBoundParams PARAMS = new DateBoundParams("SpecificDate", false);
  private JRadioButton myRelative;
  private JRadioButton myAbsolute;
  private JTextField myFromNow;
  private AComboBox<DateUnit> myUnits;
  private ADateField myDate;
  private JPanel myWholePanel;

  public SpecificDate(@NotNull PropertyMap values) {
    super(values);
    AppBook.replaceText(DateConstraintEditor.BOOK_PREFIX, myWholePanel);
    PARAMS.install(getBinder(), new DateBoundParams.Components(null, myRelative, myAbsolute, myFromNow, myUnits, myDate));
//    new DateBoundController(myRelative, myAbsolute, myFromNow, myUnits, myDate, false);
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  private void createUIComponents() {
    myDate = new ADateField(ADateField.Precision.DAY); 
  }
}
