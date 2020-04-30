package com.almworks.engine.gui.attachments;

import com.almworks.api.config.MiscConfig;
import com.almworks.api.download.DownloadedFile;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

class SaveAllAttachmentsAction extends AbstractAttachmentAction {
  public static final AnAction INSTANCE = new SaveAllAttachmentsAction();

  private static final String OVERWRITE = "Overwrite";
  private static final String OVERWRITE_ALL = "Overwrite all";
  private static final String SKIP = "Skip";
  private static final String SKIP_ALL_EXIST = "Skip all existing";
  private static final String CANCEL = "Cancel";
  private static final String PROCEED = "Proceed";
  private static final String PROCEED_SILENTLY = "Proceed silently";
  private static final Integer ESCAPE = -1;

  private static final int FLAG_SKIP_EXISTING = 1;
  private static final int FLAG_OVERWRITE_EXISTING = 2;
  private static final int FLAG_IGNORE_UNWRITABLE = 4;

  private static final Object THREAD_GATE_KEY = new Object();

  private SaveAllAttachmentsAction() {
    super("&Save a Copy\u2026", Icons.ACTION_SAVE_ALL);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Save all attachments to a folder");
    setDefaultPresentation(PresentationKey.SHORTCUT,
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
    watchRole(AttachmentsController.ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    AttachmentsController controller = context.getSourceObject(AttachmentsController.ROLE);
    context.updateOnChange(controller.getAllAttachmentsModifiable());
    context.updateOnChange(controller.getDownloadedStatusModifiable());
    Collection<? extends Attachment> attachments = controller.getAttachments();
    boolean enabled = !attachments.isEmpty();
    for (Attachment attachment : attachments) {
      if (!attachment.isLocal()) {
        DownloadedFile dfile = attachment.getDownloadedFile(controller.getDownloadStatus());
        if (dfile == null || dfile.getState() != DownloadedFile.State.READY) {
          enabled = false;
          break;
        }
      }
    }
    context.setEnabled(enabled);
    context.putPresentationProperty(PresentationKey.NAME, attachments.size() == 1 ? "&Save a Copy\u2026" : "Save Copies\u2026");
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    Configuration config = context.getSourceObject(MiscConfig.ROLE).getConfig("saveAttachments");
    JFileChooser fileChooser = new JFileChooser(getDefaultDirectory(config));
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setApproveButtonText("Select");
    fileChooser.setApproveButtonMnemonic(0);
    fileChooser.setDialogTitle("Select a Folder to Copy Attachments To");

    Component owner = context.getComponent();
    if (owner != null) {
      owner = SwingUtilities.getWindowAncestor(owner);
    }
    int r = fileChooser.showSaveDialog(owner);
    if (r == JFileChooser.APPROVE_OPTION) {
      File dir = fileChooser.getSelectedFile();
      if (!dir.isDirectory()) {
        JOptionPane.showMessageDialog(owner, "Cannot save to " + dir, "Cannot Save", JOptionPane.ERROR_MESSAGE);
      } else {
        AttachmentsController controller = context.getSourceObject(AttachmentsController.ROLE);
        config.setSetting(AttachmentChooserBase.CHOOSER_DIR_SETTING, dir.getAbsolutePath());
        Collection<? extends Attachment> attachments = controller.getAttachments();
        List<File> files = Collections15.arrayList();
        for (Attachment attachment : attachments) {
          File file = attachment.getLocalFile(controller.getDownloadStatus());
          if (file != null) files.add(file);
        }
        saveFilesAwtStep(files, dir, owner, 0);
      }
    }
  }


  @ThreadAWT
  private void saveFilesAwtStep(final List<File> files, final File dir, final Component owner, int flags) {
    if (files.size() == 0)
      return;
    final File file = files.remove(0);
    final String fileName = file.getName();
    final File targetFile = new File(dir, fileName);
    boolean write = true;
    boolean proceed = true;
    if (targetFile.exists()) {
      write = false;
      if (targetFile.isFile() /*&& targetFile.canWrite()*/) {
        if ((flags & FLAG_OVERWRITE_EXISTING) == FLAG_OVERWRITE_EXISTING) {
          write = true;
        } else if ((flags & FLAG_SKIP_EXISTING) == 0) {
          String[] options = new String[] {OVERWRITE, OVERWRITE_ALL, SKIP, SKIP_ALL_EXIST, CANCEL};
          String message = "File " + fileName + " already exists. What would you like to do?";
          Object selectedValue =
            showMessage("File Already Exists", message, JOptionPane.QUESTION_MESSAGE, options, owner);
          if (OVERWRITE == selectedValue || OVERWRITE_ALL == selectedValue) {
            write = true;
          }
          if (OVERWRITE_ALL == selectedValue) {
            flags = flags | FLAG_OVERWRITE_EXISTING;
          }
          if (SKIP_ALL_EXIST == selectedValue) {
            flags = flags | FLAG_SKIP_EXISTING;
          }
          if (selectedValue == null || CANCEL == selectedValue || ESCAPE.equals(selectedValue)) {
            return;
          }
        }
      } else {
        if ((flags & FLAG_IGNORE_UNWRITABLE) == 0) {
          String message = "Cannot write file " + fileName + ".";
          String[] options = new String[] {PROCEED, PROCEED_SILENTLY, CANCEL};
          Object result = showMessage("Cannot Write File", message, JOptionPane.INFORMATION_MESSAGE, options, owner);
          if (result == null || result == CANCEL || ESCAPE.equals(result)) {
            return;
          }
          if (result == PROCEED_SILENTLY) {
            flags = flags | FLAG_IGNORE_UNWRITABLE;
          }
        }
      }
    }
    final int finalFlags = flags;
    if (write) {
      ThreadGate.LONG_QUEUED(THREAD_GATE_KEY).execute(new Runnable() {
        public void run() {
          final IOException error = copyFile(file, targetFile);
          ThreadGate.AWT.execute(new Runnable() {
            public void run() {
              int flags = finalFlags;
              if (error != null) {
                if ((flags & FLAG_IGNORE_UNWRITABLE) == 0) {
                  String errorMessage = error.getMessage();
                  if (errorMessage == null)
                    errorMessage = "";
                  else if (errorMessage.length() > 80)
                    errorMessage = errorMessage.substring(0, 79) + "\u2026";

                  String message = "<html><body>Cannot write file " + fileName + ".<br>" + errorMessage;
                  String[] options = new String[] {PROCEED, PROCEED_SILENTLY, CANCEL};
                  Object result =
                    showMessage("Cannot Write File", message, JOptionPane.INFORMATION_MESSAGE, options, owner);
                  if (result == null || result == CANCEL || ESCAPE.equals(result)) {
                    return;
                  }
                  if (result == PROCEED_SILENTLY) {
                    flags = flags | FLAG_IGNORE_UNWRITABLE;
                  }
                }
              }
              saveFilesAwtStep(files, dir, owner, flags);
            }
          });
        }
      });
    } else {
      saveFilesAwtStep(files, dir, owner, flags);
    }
  }

  private Object showMessage(String title, String message, int messageType, String[] options, Component owner) {
    JOptionPane pane = new JOptionPane(message, messageType);
    pane.setOptions(options);
    pane.setInitialValue(options[0]);
    JDialog dialog = pane.createDialog(owner, title);
    pane.selectInitialValue();
    dialog.show();
    dialog.dispose();
    return pane.getValue();
  }

  private IOException copyFile(File file, File targetFile) {
    try {
      FileUtil.copyFile(file, targetFile, true);
      return null;
    } catch (IOException e) {
      return e;
    }
  }


  private static String getDefaultDirectory(Configuration configuration) {
    return AttachmentChooserBase.getDefaultDirectory(configuration);
  }
}
