package com.almworks.export.html;

import com.almworks.api.misc.WorkArea;
import com.almworks.export.FileExporterUIHelper;
import com.almworks.util.AppBook;
import com.almworks.util.components.FileSelectionField;
import com.almworks.util.config.ConfigAttach;
import com.almworks.util.config.Configuration;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.File;

public class HTMLParametersForm extends FileExporterUIHelper {
  private static final int DEFAULT_BUGS_PER_PAGE = 30;

  private JCheckBox myUseTempFile;
  private JCheckBox myUseCss;
  private JLabel myTargetFileLabel;
  private FileSelectionField myTargetFile;
  private FileSelectionField myCssFile;
  private JPanel myWholePanel;
  private JCheckBox myFormatForPrinting;
  private JSpinner mySplitPagesSpinner;
  private final WorkArea myWorkArea;

  public HTMLParametersForm(Configuration config, WorkArea workArea) {
    myWorkArea = workArea;
    AppBook.replaceText("Export.HTML.Form", myWholePanel);
    setupConfig(config);
    setupErrorModel(myTargetFile);
    setupFileChooser(myTargetFile, "HTML files");
    setupUseTempFile(myUseTempFile, myTargetFile);
    setupCssEnabled();
    setupVisual();
    setupFormatForPrinting();
  }

  private void setupFormatForPrinting() {
    myLife.add(UIUtil.setupConditionalEnabled(myFormatForPrinting, false, mySplitPagesSpinner));
  }

  private void setupVisual() {
    myTargetFileLabel.setLabelFor(myTargetFile);
  }

  private void setupCssEnabled() {
    myLife.add(UIUtil.setupConditionalEnabled(myUseCss, false, myCssFile));
  }

  private void setupConfig(final Configuration config) {
    ConfigAttach.attachCheckbox(myLife, myUseTempFile, config, "useTempFile", true);
    ConfigAttach.attachCheckbox(myLife, myUseCss, config, "useCss", true);
    ConfigAttach.attachTextField(myLife, myTargetFile.getField(), config, "targetFile", "");
    ConfigAttach.attachTextField(myLife, myCssFile.getField(), config, "cssFile", getDefaultCssFile());
    ConfigAttach.attachCheckbox(myLife, myFormatForPrinting, config, "splitPages", true);
    attachSpinner(config);
  }

  private void attachSpinner(final Configuration config) {
    final String setting = "splitBugsCount";
    int count = config.getIntegerSetting(setting, DEFAULT_BUGS_PER_PAGE);
    final SpinnerNumberModel model = new SpinnerNumberModel(count, 1, 9999, 1);
    mySplitPagesSpinner.setModel(model);
    final ChangeListener listener = new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Object value = mySplitPagesSpinner.getValue();
        if (value instanceof Integer) {
          config.setSetting(setting, ((Integer) value).intValue());
        }
      }
    };
    model.addChangeListener(listener);
    myLife.add(new Detach() {
      protected void doDetach() {
        model.removeChangeListener(listener);
      }
    });
  }

  private String getDefaultCssFile() {
    File etcFile = myWorkArea.getEtcFile(WorkArea.ETC_HTML_EXPORT_CSS);
    return etcFile == null ? "" : etcFile.getAbsolutePath();
  }

  protected String getDefaultExtension() {
    return "html";
  }

  public Detach getDetach() {
    return myLife;
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void addParametersTo(PropertyMap parameters) {
    parameters.put(HTMLParams.TARGET_FILE, myTargetFile.getFile());
    File cssFile = getCssFile();
    if (cssFile != null)
      parameters.put(HTMLParams.CSS_FILE, cssFile);
    if (myFormatForPrinting.isSelected()) {
      Object value = mySplitPagesSpinner.getValue();
      if (value instanceof Integer)
        parameters.put(HTMLParams.BUGS_PER_TABLE, (Integer) value);
    }
  }

  private File getCssFile() {
    return myUseCss.isSelected() ? myCssFile.getFile() : null;
  }

  @NotNull
  public ScalarModel<String> getFormErrorModel() {
    return myErrorModel;
  }

  public void confirmExport() throws CantPerformException {
    checkFileOverwrite(myTargetFile, myWholePanel);
  }
}
