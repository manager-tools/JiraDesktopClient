package com.almworks.export;

import com.almworks.api.install.Setup;
import com.almworks.util.components.FileSelectionField;
import com.almworks.util.files.FileUtil;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.UIComponentWrapper2Support;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public abstract class FileExporterUIHelper extends UIComponentWrapper2Support implements ExporterUI {
  protected final DetachComposite myLife = new DetachComposite();
  protected final BasicScalarModel<String> myErrorModel = BasicScalarModel.create(true);

  public Detach getDetach() {
    return myLife;
  }

  @NotNull
  public ScalarModel<String> getFormErrorModel() {
    return myErrorModel;
  }

  protected abstract String getDefaultExtension();

  protected void setupUseTempFile(final JCheckBox temporaryFileCheckbox, final FileSelectionField targetFile) {
    myLife.add(UIUtil.setupConditionalEnabled(temporaryFileCheckbox, true, targetFile));
    myLife.add(UIUtil.addActionListener(temporaryFileCheckbox, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setTemporaryFileName(temporaryFileCheckbox, targetFile);
      }
    }));
    setTemporaryFileName(temporaryFileCheckbox, targetFile);
  }

  private void setTemporaryFileName(JCheckBox temporaryFileCheckbox, FileSelectionField targetFile) {
    if (temporaryFileCheckbox.isSelected()) {
      targetFile.setFile(createTemporaryFile());
    }
  }

  private File createTemporaryFile() {
    try {
      File file = File.createTempFile(Setup.getProductId() + "-", "." + getDefaultExtension());
      FileUtil.deleteFile(file, false);
      return file;
    } catch (IOException e) {
      return new File("");
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }

  protected void setupFileChooser(FileSelectionField targetFile, String description) {
    targetFile.addExtensionFilter(description, getDefaultExtension());
    targetFile.setDefaultExtension(getDefaultExtension());
  }

  protected void setupErrorModel(final FileSelectionField targetFile) {
    DocumentUtil.addListener(myLife, targetFile.getField().getDocument(), new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        updateErrorModel(targetFile);
      }
    });
    updateErrorModel(targetFile);
  }

  private void updateErrorModel(FileSelectionField targetFile) {
    myErrorModel.setValue(isValidFile(targetFile.getFile()) ? null : "Valid target file name is required");
  }

  private boolean isValidFile(File file) {
    if(file == null) {
      return false;
    }
    if(file.exists()) {
      return file.isFile() && file.canWrite();
    }
    if(file.getPath().length() == 0) {
      return false;
    }

    final File dir = file.getParentFile();
    if(dir == null) {
      return true;
    }
    if(!dir.exists()) {
      return true;
    }
    if(!dir.isDirectory()) {
      return false;
    }
    return true;
  }

  protected void checkFileOverwrite(FileSelectionField targetFile, JPanel owner) throws CantPerformException {
    File file = targetFile.getFile();
    if (file == null)
      throw new CantPerformException();
    if (file.exists()) {
      int r = JOptionPane.showConfirmDialog(owner,
        "Would you like to overwrite the existing file named '" + file.getName() + "'?", "Confirm File Replace",
        JOptionPane.YES_NO_OPTION);
      if (r != JOptionPane.YES_OPTION)
        throw new CantPerformExceptionSilently("user cancelled action");
    }
  }

  public abstract void confirmExport() throws CantPerformException;
}
