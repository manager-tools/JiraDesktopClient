package com.almworks.engine.gui.attachments;

import com.almworks.util.Env;
import com.almworks.util.config.Configuration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;

abstract class AttachmentChooserBase {
  public static final String CHOOSER_DIR_SETTING = "attachDir";

  protected final JFileChooser myChooser;
  protected final Component myOwner;

  private final Configuration myConfig;

  public AttachmentChooserBase(Component owner, @NotNull Configuration config) {
    myOwner = owner;
    myConfig = config;
    myChooser = new JFileChooser(getDefaultDirectory()) {
      public void approveSelection() {
        File file = getSelectedFile();
        if (!AttachmentChooserBase.this.approveSelection(file))
          return;
        super.approveSelection();
      }
    };
  }

  protected boolean approveSelection(File file) {
    return true;
  }

  protected String getDefaultDirectory() {
    return getDefaultDirectory(myConfig);
  }

  public File choose() {
    prepareDialog();
    myChooser.setMultiSelectionEnabled(false);
    int r = showDialog();
    return r == JFileChooser.APPROVE_OPTION ? myChooser.getSelectedFile() : null;
  }

  public File[] chooseMultiple() {
    prepareDialog();
    myChooser.setMultiSelectionEnabled(true);
    int r = showDialog();
    return r == JFileChooser.APPROVE_OPTION ? myChooser.getSelectedFiles() : null;
  }

  protected void prepareDialog() {
    myChooser.setDialogTitle(getTitle());
    myChooser.setAcceptAllFileFilterUsed(true);
    myChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
  }

  private final int showDialog() {
    Window owner = myOwner == null ? null : SwingUtilities.getWindowAncestor(myOwner);
    int r = showChooser(owner);
    if (r == JFileChooser.APPROVE_OPTION) {
      updateConfig();
    }
    return r;
  }

  protected int showChooser(Window owner) {
    return myChooser.showSaveDialog(owner);
  }

  private void updateConfig() {
    File file = myChooser.getSelectedFile();
    File dir = null;
    if (file != null) {
      if (file.isDirectory()) {
        dir = file;
      } else {
        file = file.getParentFile();
        if (file != null && file.isDirectory()) {
          dir = file;
        }
      }
    }
    if (dir != null) {
      myConfig.setSetting(CHOOSER_DIR_SETTING, dir.getAbsolutePath());
    }
  }

  protected abstract String getTitle();

  public static String getDefaultDirectory(Configuration config) {
    String dir = config.getSetting(CHOOSER_DIR_SETTING, null);
    if (dir == null) {
      dir = Env.getUserHome();
    }
    return dir == null ? "." : dir;
  }
}
