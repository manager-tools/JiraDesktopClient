package com.almworks.export.csv;

import com.almworks.export.FileExporterUIHelper;
import com.almworks.util.AppBook;
import com.almworks.util.advmodel.*;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.FileSelectionField;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.ConfigAttach;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.CantPerformException;

import javax.swing.*;
import java.nio.charset.Charset;

public class CSVParametersForm extends FileExporterUIHelper {
  private JCheckBox myOutputHeaderRow;
  private JCheckBox myUseQuotesAlways;
  private FileSelectionField myTargetFile;
  private JLabel myTargetFileLabel;
  private JPanel myWholePanel;
  private AComboBox<Character> myDelimiter;
  private JLabel myDelimiterLabel;
  private JCheckBox myProtectFormula;
  private JCheckBox myUseTemporaryFile;
  private AComboBox<Charset> myEncoding;

  private final Configuration myConfig;

  public CSVParametersForm(Configuration config) {
    myConfig = config;
    AppBook.replaceText("Export.CSV.Form.", myWholePanel);
    setupDelimiters();
    setupConfig();
    setupVisual();
    setupErrorModel(myTargetFile);
    setupFileChooser(myTargetFile, "Comma-separated files");
    setupUseTempFile(myUseTemporaryFile, myTargetFile);
    Configuration encodingConfig = config.getOrCreateSubset("encoding");
    UIUtil.configureEncodingCombo(myLife, myEncoding, encodingConfig.getOrCreateSubset("value"), null, encodingConfig.getOrCreateSubset("recents"), CSVExporter.getDefaultCharset());
  }

  protected String getDefaultExtension() {
    return "csv";
  }

  private void setupDelimiters() {
    OrderListModel<Character> options = new OrderListModel<Character>();
    options.addElement((char) 0);
    options.addElement(',');
    options.addElement(';');
    SelectionInListModel<Character> model =
      SelectionInListModel.createForever(FixedListModel.create((char) 0, ',', ';'), null);
    myDelimiter.setModel(model);
    myDelimiter.setCanvasRenderer(new CanvasRenderer<Character>() {
      public void renderStateOn(CellState state, Canvas canvas, Character item) {
        if (item == null)
          return;
        final String text;
        switch (item.charValue()) {
        case (char) 0:
          text = "(Default)";
          break;
        case ',':
          text = "Comma (,)";
          break;
        case ';':
          text = "Semicolon (;)";
          break;
        default:
          text = "Other (" + item + ")";
          break;
        }
        canvas.appendText(text);
      }
    });
  }

  private void setupVisual() {
    myTargetFileLabel.setLabelFor(myTargetFile.getField());
    myDelimiterLabel.setLabelFor(myDelimiter);
  }

  private void setupConfig() {
    ConfigAttach.attachCheckbox(myLife, myOutputHeaderRow, myConfig, "outputHeaderRow", true);
    ConfigAttach.attachCheckbox(myLife, myUseQuotesAlways, myConfig, "useQuotesAlways", false);
    ConfigAttach.attachTextField(myLife, myTargetFile.getField(), myConfig, "targetFile", "");
    ConfigAttach.attachCheckbox(myLife, myProtectFormula, myConfig, "protectFormula", true);
    ConfigAttach.attachCheckbox(myLife, myUseTemporaryFile, myConfig, "useTempFile", true);
    attachDelimiter();
  }

  private void attachDelimiter() {
    final String delimiter = "delimiter";
    String setting = myConfig.getSetting(delimiter, null);
    Character c;
    if (setting == null || "0".equals(setting)) {
      c = (char) 0;
    } else {
      c = setting.charAt(0);
    }
    final AComboboxModel<Character> model = myDelimiter.getModel();
    assert model.getSize() > 0;
    if (model.getSize() == 0)
      return;
    int index = model.indexOf(c);
    if (index >= 0) {
      model.setSelectedItem(c);
    } else {
      model.setSelectedItem(model.getAt(0));
    }

    model.addSelectionListener(myLife, new SelectionListener.SelectionOnlyAdapter() {
      public void onSelectionChanged() {
        Character item = model.getSelectedItem();
        char c = item == null ? 0 : item.charValue();
        String serialized = c == 0 ? "0" : String.valueOf(c);
        myConfig.setSetting(delimiter, serialized);
      }
    });
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void addParametersTo(PropertyMap parameters) {
    parameters.put(CSVParams.TARGET_FILE, myTargetFile.getFile());
    parameters.put(CSVParams.QUOTES_ALWAYS, myUseQuotesAlways.isSelected());
    parameters.put(CSVParams.OUTPUT_HEADER, myOutputHeaderRow.isSelected());
    parameters.put(CSVParams.DELIMITER_CHAR, myDelimiter.getModel().getSelectedItem());
    parameters.put(CSVParams.PROTECT_FORMULA, myProtectFormula.isSelected());
    parameters.put(CSVParams.CHARSET, RecentController.<Charset>unwrap(myEncoding.getModel().getSelectedItem()));
  }

  public void confirmExport() throws CantPerformException {
    checkFileOverwrite(myTargetFile, myWholePanel);
  }

  public void setExtraOptionsVisible(boolean visible) {
    myOutputHeaderRow.setVisible(visible);
    myProtectFormula.setVisible(visible);
    myUseQuotesAlways.setVisible(visible);
  }
}