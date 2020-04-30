package com.almworks.export;

import com.almworks.util.AppBook;
import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.properties.StringSerializer;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.UIComponentWrapper2Support;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;
import java.util.Locale;

class ExportFormatForm<T extends Format> extends UIComponentWrapper2Support {
  private static final String FORMAT = "format";

  private JPanel myWholePanel;
  private JRadioButton myStandardRadio;
  private JRadioButton myCustomRadio;
  private JLabel mySampleLabel;
  private AComboBox<T> myStandard;
  private JTextField myCustom;
  private JTextField mySample;

  private Border myCustomGoodBorder;
  private Border myCustomBadBorder;

  private final DetachComposite myLife = new DetachComposite();
  private final ButtonGroup myButtonGroup = new ButtonGroup();
  private final BasicScalarModel<T> mySelectedFormat = BasicScalarModel.create(true, false);

  private final StringSerializer<T> mySerializer;
  private final Object mySampleValue;
  private final ScalarModel<Locale> myLocale;

  public ExportFormatForm(Configuration config, StringSerializer<T> serializer, AComboboxModel<T> standards,
    Object sampleValue, String borderName, ScalarModel<Locale> locale)
  {
    mySerializer = serializer;
    mySampleValue = sampleValue;
    myLocale = locale;
    AppBook.replaceText("ExportFormatForm", myWholePanel);
    setupButtonGroup();
    setupStandardFormats(standards);
    setupSelectedFormat();
    setupPeristence(config);
    setupSample();
    setupVisual(borderName);
    updateSelectedFormat();
    updateSample();
  }

  private void setupVisual(String borderName) {
    mySampleLabel.setLabelFor(mySample);
    if (borderName != null) {
      Border border = myWholePanel.getBorder();
      if (border instanceof TitledBorder) {
        ((TitledBorder) border).setTitle(borderName);
      }
    }
  }

  public Detach getDetach() {
    return myLife;
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public ScalarModel<T> getSelectedFormatModel() {
    return mySelectedFormat;
  }

  private void setupSample() {
    mySelectedFormat.getEventSource().addAWTListener(myLife, new ScalarModel.Adapter<T>() {
      public void onScalarChanged(ScalarModelEvent<T> event) {
        updateSample();
      }
    });
  }

  private void updateSample() {
    T format = mySelectedFormat.getValue();
    if (format == null) {
      mySample.setText("(invalid format)");
    } else {
      mySample.setText(format.format(mySampleValue));
    }
  }

  private void setupPeristence(final Configuration config) {
    String setting = config.getSetting(FORMAT, null);
    if (setting != null) {
      String custom = setting;
      for (T item : myStandard.getModel().toList()) {
        if (setting.equals(mySerializer.storeToString(item))) {
          myStandard.getModel().setSelectedItem(item);
          custom = null;
          break;
        }
      }
      if (custom != null) {
        myCustom.setText(custom);
        myCustomRadio.setSelected(true);
      } else {
        myCustom.setText("");
        myStandardRadio.setSelected(true);
      }
    }
    myLife.add(mySelectedFormat.getEventSource().addStraightListener(new ScalarModel.Adapter<T>() {
      public void onScalarChanged(ScalarModelEvent<T> event) {
        T format = event.getNewValue();
        if (format != null)
          config.setSetting(FORMAT, mySerializer.storeToString(format));
      }
    }));
  }

  private void setupSelectedFormat() {
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateSelectedFormat();
        updateCustomBorder();
      }
    };
    myLife.add(UIUtil.addActionListener(myCustomRadio, listener));
    myLife.add(UIUtil.addActionListener(myStandardRadio, listener));
    DocumentUtil.addListener(myLife, myCustom.getDocument(), new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        updateSelectedFormat();
        updateCustomBorder();
      }
    });
    myStandard.getModel().addSelectionListener(myLife, new SelectionListener.SelectionOnlyAdapter() {
      public void onSelectionChanged() {
        updateSelectedFormat();
      }
    });
    myLocale.getEventSource().addAWTListener(myLife, new ScalarModel.Adapter<Locale>() {
      public void onScalarChanged(ScalarModelEvent<Locale> event) {
        updateSelectedFormat();
        updateCustomBorder();
      }
    });
  }

  private void updateCustomBorder() {
    if (myCustom.isEnabled()) {
      String text = myCustom.getText();
      boolean good = text != null && text.length() > 0 && mySerializer.restoreFromString(text) != null;
      Border border = getCustomBorder(good);
      if (border != null)
        myCustom.setBorder(border);
    } else {
      myCustom.setBorder(getCustomBorder(true));
    }
  }

  private Border getCustomBorder(boolean good) {
    if (myCustomBadBorder == null || myCustomGoodBorder == null) {
      Border border = myCustom.getBorder();
      if (border == null)
        return null;
      myCustomGoodBorder = border;//new CompoundBorder(new LineBorder(myWholePanel.getBackground(), 2), border);
      myCustomBadBorder = new CompoundBorder(new LineBorder(GlobalColors.ERROR_COLOR, 2, true), border);
    }
    return good ? myCustomGoodBorder : myCustomBadBorder;
  }

  private void updateSelectedFormat() {
    T selected;
    if (myCustomRadio.isSelected()) {
      selected = getCustomFormat();
    } else {
      selected = myStandard.getModel().getSelectedItem();
    }
    if (!Util.equals(selected, mySelectedFormat.getValue())) {
      mySelectedFormat.setValue(selected);
    }
  }

  private T getCustomFormat() {
    String text = myCustom.getText();
    if (text == null || text.length() == 0)
      return null;
    return mySerializer.restoreFromString(text);
  }

  private void setupStandardFormats(AComboboxModel<T> standards) {
    myStandard.setModel(standards);
    myStandard.setCanvasRenderer(new CanvasRenderer<T>() {
      public void renderStateOn(CellState state, Canvas canvas, T item) {
        String string = mySerializer.storeToString(item);
        if (string == null)
          string = "(unrecognizable format)";
        canvas.appendText(string);
      }
    });
  }

  private void setupButtonGroup() {
    myButtonGroup.add(myStandardRadio);
    myButtonGroup.add(myCustomRadio);
    myStandardRadio.setSelected(true);
    myLife.add(UIUtil.setupConditionalEnabled(myStandardRadio, false, myStandard));
    myLife.add(UIUtil.setupConditionalEnabled(myCustomRadio, false, myCustom));
  }
}
