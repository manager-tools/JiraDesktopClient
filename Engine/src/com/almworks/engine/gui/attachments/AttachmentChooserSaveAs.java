package com.almworks.engine.gui.attachments;

import com.almworks.util.config.Configuration;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.MessageFormat;

class AttachmentChooserSaveAs extends AttachmentChooserBase {
  private static final MessageFormat SAVE_AS_CONFIRM_OVERWRITE =
    new MessageFormat("File {0} already exists. Do you want to replace it?");

  private final File myInitialSelection;

  public AttachmentChooserSaveAs(Component owner, File initialSelection, Configuration config) {
    super(owner, config);
    myInitialSelection = initialSelection;
  }

  protected void prepareDialog() {
    super.prepareDialog();
    File file = new File(myInitialSelection.getName());
    myChooser.setSelectedFile(file);
    myChooser.ensureFileIsVisible(file);
  }

  protected String getTitle() {
    return "Save Attachment";
  }

  protected boolean approveSelection(File file) {
    if (file == null)
      return false;
    if (file.exists()) {
      int r = JOptionPane.showConfirmDialog(myChooser, SAVE_AS_CONFIRM_OVERWRITE.format(new Object[] {file.getName()}),
        "File Exists", JOptionPane.YES_NO_OPTION);
      if (r != JOptionPane.YES_OPTION)
        return false;
    }
    return true;
  }


  public static File show(File file, Component owner, Configuration config) {
    AttachmentChooserSaveAs myChooser = new AttachmentChooserSaveAs(owner, file, config);
    return myChooser.choose();
  }
}
