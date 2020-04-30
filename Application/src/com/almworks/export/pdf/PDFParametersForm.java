package com.almworks.export.pdf;

import com.almworks.export.FileExporterUIHelper;
import com.almworks.util.AppBook;
import com.almworks.util.components.FileSelectionField;
import com.almworks.util.config.ConfigAttach;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.CantPerformException;

import javax.swing.*;

public class PDFParametersForm extends FileExporterUIHelper {
  private JPanel myWholePanel;
  private JCheckBox myCommentsCheckBox;
  private JCheckBox myGraphicsAttachesCheckBox;
  private JCheckBox myTextAttachesCheckBox;
  private FileSelectionField myTargetFile;
  private JCheckBox myOnNewPage;
  private JCheckBox myCompactAttributes;
  private JCheckBox myUseTempFile;

  private final Configuration myConfig;

  public PDFParametersForm(Configuration config) {
    myConfig = config;
    AppBook.replaceText("Export.PDF.Form", myWholePanel);
    setupConfig(config);
    setupFileChooser(myTargetFile, "PDF files");
    setupErrorModel(myTargetFile);
    setupUseTempFile(myUseTempFile, myTargetFile);
  }

  private void setupConfig(Configuration config) {
    ConfigAttach.attachCheckbox(myLife, myCommentsCheckBox, config, "printComments", true);
    ConfigAttach.attachCheckbox(myLife, myGraphicsAttachesCheckBox, config, "printGraphAttach", true);
    ConfigAttach.attachCheckbox(myLife, myTextAttachesCheckBox, config, "printTextAttach", true);
    ConfigAttach.attachCheckbox(myLife, myCompactAttributes, config, "printCompact", true);
    ConfigAttach.attachCheckbox(myLife, myOnNewPage, config, "printOnNewPage", true);
    ConfigAttach.attachTextField(myLife, myTargetFile.getField(), config, "targetFile", "");
    ConfigAttach.attachCheckbox(myLife, myUseTempFile, config, "useTempFile", true);
  }

  protected String getDefaultExtension() {
    return "pdf";
  }

  public void confirmExport() throws CantPerformException {
    checkFileOverwrite(myTargetFile, myWholePanel);
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void addParametersTo(PropertyMap parameters) {
    parameters.put(PDFParams.TARGET_FILE, myTargetFile.getFile());
    parameters.put(PDFParams.COMMENTS, myCommentsCheckBox.isSelected());
    parameters.put(PDFParams.ATTACH_GRAPH, myGraphicsAttachesCheckBox.isSelected());
    parameters.put(PDFParams.ATTACH_TEXT, myTextAttachesCheckBox.isSelected());
    parameters.put(PDFParams.ON_NEW_PAGE, myOnNewPage.isSelected());
    parameters.put(PDFParams.COMPACT_TABLE, myCompactAttributes.isSelected());
  }
}
