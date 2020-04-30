package com.almworks.util.components;

import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Condition;
import com.almworks.util.files.ExtensionFileFilter;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.CantPerformException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

public abstract class BaseFileSelectionField<F extends JComponent> extends FieldWithMoreButton<F> {
  protected final JFileChooser myChooser;
  @Nullable
  private File myBaseDir = null;
  @Nullable
  private String myDialogSelectButtonText = null;
  @Nullable
  private Condition<File> myApproveCondition = null;
  private boolean myConfirmOverwrite = false;
  private String myDefaultExtension = null;
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  public BaseFileSelectionField(F field) {
    super.setField(field);
    super.setAction(new AnActionListener() {
      public void perform(ActionContext context) throws CantPerformException {
        selectFile(context);
      }
    });
    myChooser = new JFileChooser() {
      public void approveSelection() {
        File file = getSelectedFile();
        if (!BaseFileSelectionField.this.approveSelection(file))
          return;
        super.approveSelection();
      }
    };
    setActionName("\u2026");
    setChooserDefaults();
  }

  public SimpleModifiable getModifiable() {
    return myModifiable;
  }

  protected boolean approveSelection(File file) {
    Condition<File> approveCondition = myApproveCondition;
    if (approveCondition != null) {
      if (!approveCondition.isAccepted(file))
        return false;
    }
    if (myConfirmOverwrite) {
      if (!confirmOverwrite(file))
        return false;
    }
    return true;
  }

  private boolean confirmOverwrite(File file) {
    assert myConfirmOverwrite;
    if (ExtensionFileFilter.isWindowsFloppy(file)) {
      // win2k safeguard
      return true;
    }
    if (file.exists()) {
      int r = JOptionPane.showConfirmDialog(this,
        "Would you like to overwrite the existing file named '" + file.getName() + "'?", "Confirm File Overwriting",
        JOptionPane.YES_NO_OPTION);
      if (r != JOptionPane.YES_OPTION)
        return false;
    }
    return true;
  }

  public boolean isConfirmOverwrite() {
    return myConfirmOverwrite;
  }

  public void setConfirmOverwrite(boolean confirmOverwrite) {
    myConfirmOverwrite = confirmOverwrite;
  }

  protected final void setChooserDefaults() {
    myChooser.setDialogTitle("Select File");
    myChooser.setAcceptAllFileFilterUsed(true);
    myChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    myChooser.setMultiSelectionEnabled(false);
    myChooser.setDialogType(JFileChooser.SAVE_DIALOG);
  }

  public void addExtensionFilter(@NotNull final String filterName, @NotNull String extension) {
    FileFilter filter = new ExtensionFileFilter(extension, filterName, true);
    myChooser.addChoosableFileFilter(filter);
  }

  @Nullable
  public File getBaseDir() {
    return myBaseDir;
  }

  public void setBaseDir(@Nullable File baseDir) {
    myBaseDir = baseDir;
  }

  @Nullable
  public String getDialogSelectButtonText() {
    return myDialogSelectButtonText;
  }

  public void setDialogSelectButtonText(String text) {
    myDialogSelectButtonText = text;
  }

  @NotNull
  public File getFile() {
    return new File(getBaseDir(), getFilename());
  }

  public void setFile(@Nullable File file) {
    setFilename(file == null ? null : file.getAbsolutePath());
    fireChanged();
  }

  protected final void fireChanged() {
    myModifiable.fireChanged();
  }

  @NotNull
  public JFileChooser getFileChooser() {
    return myChooser;
  }

  @NotNull
  public abstract String getFilename();

  protected abstract void setFilename(@Nullable String filename);

  public final void setAction(AnActionListener action) {
    throw new UnsupportedOperationException();
  }

  public final void setField(F field) {
    throw new UnsupportedOperationException();
  }

  protected void selectFile(ActionContext context) {
    File file = getFile();
    if (file.exists()) {
      myChooser.setSelectedFile(file);
      myChooser.ensureFileIsVisible(file);
    } else {
      File dir = null;
      File parent = file.getParentFile();
      if (parent != null && parent.isDirectory())
        dir = parent;
      if (dir == null && myBaseDir != null && myBaseDir.isDirectory())
        dir = myBaseDir;
      if (dir == null)
        dir = getDefaultDirectory();
      myChooser.setCurrentDirectory(dir);
    }
    int r = myChooser.showDialog(this, myDialogSelectButtonText);
    if (r == JFileChooser.APPROVE_OPTION) {
      File selectedFile = myChooser.getSelectedFile();
      if (myBaseDir != null) {
        // todo make file relative to base dir
      }
      selectedFile = applyDefaultExtension(selectedFile);
      setFile(selectedFile);
    }
  }

  private File applyDefaultExtension(File selectedFile) {
    if (myDefaultExtension == null || myDefaultExtension.length() == 0)
      return selectedFile;
    String name = selectedFile.getName();
    if (name == null)
      return selectedFile;
    if (name.indexOf('.') >= 0)
      return selectedFile;
    String path = selectedFile.getAbsolutePath();
    if (myDefaultExtension.charAt(0) != '.')
      path += '.';
    path += myDefaultExtension;
    return new File(path);
  }

  @NotNull
  protected File getDefaultDirectory() {
    String property = System.getProperty("user.home");
    return property != null ? new File(property) : new File(".");
  }

  public void setDefaultExtension(String extension) {
    myDefaultExtension = extension;
  }

  public String getDefaultExtension() {
    return myDefaultExtension;
  }
}
