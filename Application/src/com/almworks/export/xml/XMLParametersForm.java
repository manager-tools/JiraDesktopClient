package com.almworks.export.xml;

import com.almworks.export.FileExportParams;
import com.almworks.export.FileExporterUIHelper;
import com.almworks.util.AppBook;
import com.almworks.util.components.FileSelectionField;
import com.almworks.util.config.ConfigAttach;
import com.almworks.util.config.Configuration;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class XMLParametersForm extends FileExporterUIHelper {
  private JCheckBox myUseTempFile;
  private JLabel myTargetFileLabel;
  private FileSelectionField myTargetFile;
  private JPanel myWholePanel;

  public XMLParametersForm(Configuration config) {
    AppBook.replaceText("Export.XML.Form", myWholePanel);
    setupConfig(config);
    setupErrorModel(myTargetFile);
    setupFileChooser(myTargetFile, "XML files");
    setupUseTempFile(myUseTempFile, myTargetFile);
    setupVisual();
  }

  private void setupVisual() {
    myTargetFileLabel.setLabelFor(myTargetFile);
  }

  private void setupConfig(final Configuration config) {
    ConfigAttach.attachCheckbox(myLife, myUseTempFile, config, "useTempFile", true);
    ConfigAttach.attachTextField(myLife, myTargetFile.getField(), config, "targetFile", "");
  }

  protected String getDefaultExtension() {
    return "xml";
  }

  public Detach getDetach() {
    return myLife;
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void addParametersTo(PropertyMap parameters) {
    parameters.put(FileExportParams.TARGET_FILE, myTargetFile.getFile());
  }

  @NotNull
  public ScalarModel<String> getFormErrorModel() {
    return myErrorModel;
  }

  public void confirmExport() throws CantPerformException {
    checkFileOverwrite(myTargetFile, myWholePanel);
  }
}
