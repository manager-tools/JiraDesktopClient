package com.almworks.engine.gui.attachments;

import com.almworks.api.config.MiscConfig;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.misc.WorkArea;
import com.almworks.util.Env;
import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.recent.AddRecentFromComboBox;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.components.recent.UnwrapCombo;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileActions;
import com.almworks.util.files.FileUtil;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import util.external.UID;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;

public class AttachmentUtils {
  static final MessageFormat CANNOT_WRITE_FILE =
    new MessageFormat("<html><body>Cannot write {0}<br>Error: {1}</body></html>");

  public static AnAction createOpenWithAction(final File file, final Component dialogOwner) {
    return new FileDataAction("&Open With\u2026") {
      protected void perform(FileData data) {
        FileActions.openAs(file, dialogOwner);
      }
    };
  }

  public static AnAction createOpenContainingFolderAction(final File file, final Component owner) {
    return new FileDataAction(FileActions.OPEN_FOLDER_TITLE) {
      protected void perform(FileData data) {
        FileActions.openContainingFolder(file, owner);
      }
    };
  }

  public static AnAction createSaveAsAction(final File file, final Component owner, final Configuration config) {
    return new FileDataAction("Save &As\u2026") {
      protected void perform(FileData data) {
        saveAs(file, owner, data, config);
      }
    };
  }

  public static void saveAs(File file, final Component owner, final FileData data, Configuration config) {
    final File target = AttachmentChooserSaveAs.show(file, owner, config);
    if (target != null) {
      ThreadGate.LONG.execute(new Runnable() {
        public void run() {
          try {
            FileUtil.writeFile(target, data.getBytesInternal());
          } catch (final IOException e) {
            ThreadGate.AWT.execute(new ReportErrorRunnable(target, e, owner));
          }
        }
      });
    }
  }


  @ThreadAWT
  public static void saveAs(final File file, final Component owner, Configuration config) {
    Threads.assertAWTThread();
    final File target = AttachmentChooserSaveAs.show(file, owner, config);
    if (target != null) {
      ThreadGate.LONG.execute(new Runnable() {
        public void run() {
          try {
            FileUtil.copyFile(file, target, true);
          } catch (final IOException e) {
            ThreadGate.AWT.execute(new ReportErrorRunnable(target, e, owner));
          }
        }
      });
    }
  }

  public static void open(File file, Component owner) {
    FileActions.openFile(file, owner);
  }

  public static String makeFileCopyForUpload(WorkArea workArea, File originalFile) throws AttachmentSaveException {
    File uploadDir = workArea.getUploadDir();
    assert uploadDir != null;
    String name = originalFile.getName();
    String storeName = name;

    if (!originalFile.isFile()) {
      assert false : originalFile;
      return storeName;
    }

    File originalFileDir = originalFile.getAbsoluteFile().getParentFile();
    if (originalFileDir != null && originalFileDir.equals(uploadDir.getAbsoluteFile())) {
      // already in "upload" dir
      return storeName;
    }

    File file = new File(uploadDir, name);
    if (file.exists()) {
      int i;
      int tries = 10;
      for (i = 0; i < tries; i++) {
        String subdir = new UID().toString() + ".tmp";
        File dir = new File(uploadDir, subdir);
        if (!dir.exists()) {
          dir.mkdir();
          file = new File(dir, name);
          storeName = subdir + "/" + name;
          if (!file.exists())
            break;
        }
      }
      if (i == tries)
        throw new AttachmentSaveException("cannot create temporary space in folder " + uploadDir);
    }
    try {
      // todo this may take time! maybe have some kind of progress bar
      FileUtil.copyFile(originalFile, file);
    } catch (IOException e) {
      throw new AttachmentSaveException("cannot copy attached file to a temporary folder " + file.getParent());
    }
    return storeName;
  }

  public static void deleteUploadFile(final File file) {
    try {
      boolean success = FileUtil.deleteFile(file, true);
      if (!success) {
        Log.warn("cannot remove file " + file);
      } else {
        // see if we need to remove temp dir
        File dir = file.getParentFile();
        if (dir != null && dir.isDirectory() && dir.getName().endsWith(".tmp")) {
          FileUtil.deleteFile(dir, false);
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }

  public static void configureEncodingCombo(Lifespan life, AComboBox<Charset> combo, final Configuration initialConfig,
    MiscConfig miscConfig, @Nullable final Procedure<Charset> proc)
  {
    Configuration recentsConfig = miscConfig.getConfig("encodingRecents");
    UIUtil.configureEncodingCombo(life, combo, initialConfig, proc, recentsConfig, Charset.defaultCharset());
  }


  private static final String S_SELECTED_MIME = "selectedMime";
  private static final String DEFAULT_MIMETYPE = "text/plain";
  private static final String S_KNOWN_MIME = "knownMime";
  public static void configureMimeTypesCombo(Lifespan life, AComboBox<String> combo, final Configuration initialConfig, final MiscConfig miscConfig) {
    final ArrayList<String> allTypes = Collections15.arrayList();
    allTypes.addAll(initialConfig.getAllSettings(S_KNOWN_MIME));
    if (!allTypes.contains(DEFAULT_MIMETYPE)) allTypes.add(DEFAULT_MIMETYPE);
    Collections.sort(allTypes, String.CASE_INSENSITIVE_ORDER);

    SelectionInListModel<String> model = SelectionInListModel.create(allTypes, null);
    Configuration recentsConfig = miscConfig.getConfig("mimeTypeRecents");
    RecentController<String> recents = new RecentController<String>();
    recents.setup(model, recentsConfig);
    recents.setWrapRecents(true);
    recents.setRenderer(Renderers.defaultCanvasRenderer());
    recents.setIdentityConvertor(Convertor.<String>identity());
    recents.setupAComboBox(combo, life);
    AddRecentFromComboBox.install(life, recents, combo);

    final AComboboxModel<String> decoratedModel = combo.getModel();
    decoratedModel.addSelectionChangeListener(life, new ChangeListener() {
      @Override
      public void onChange() {
        String mimeType = Util.NN(UnwrapCombo.getUnwrapSelected(decoratedModel)).trim();
        if (mimeType.isEmpty()) {
          if (!allTypes.contains(mimeType)) {
            ArrayList<String> types = Collections15.arrayList(initialConfig.getAllSettings(S_KNOWN_MIME));
            types.add(mimeType);
            initialConfig.setSettings(S_KNOWN_MIME, types);
          }
          initialConfig.setSetting(S_SELECTED_MIME, mimeType);
        }
      }
    });
    String initial = initialConfig.getSetting(S_SELECTED_MIME, DEFAULT_MIMETYPE);
    initial = Util.NN(initial, DEFAULT_MIMETYPE);
    recents.addToRecent(initial);
    UnwrapCombo.selectRecent(decoratedModel, initial);
  }

  public static MenuBuilder createAttachmentPopupMenu(String deleteAction, String renameAction /*, String editAction*/) {
    MenuBuilder builder = new MenuBuilder();
    boolean openExternalSupported = Env.isWindows() || FileActions.isDesktopOpenSupported();
//    if (openExternalSupported)
//      builder.addDefaultAction(MainMenu.Attachments.DOWNLOAD_AND_OPEN_EXTERNAL);
//    else
      builder.addDefaultAction(MainMenu.Attachments.DOWNLOAD_AND_VIEW);
    builder.addAction(MainMenu.Attachments.DOWNLOAD);
    if (openExternalSupported) {
      builder.addAction(MainMenu.Attachments.DOWNLOAD_AND_OPEN_EXTERNAL);
      builder.addAction(MainMenu.Attachments.DOWNLOAD_AND_VIEW_INTERNAL);
    }
    builder.addAction(MainMenu.Attachments.SAVE_AS);
    if (renameAction != null) builder.addAction(renameAction);
    if (deleteAction != null) builder.addAction(deleteAction);
//    if (editAction != null) builder.addAction(editAction);
    builder.addSeparator();
    builder.addAction(MainMenu.Attachments.OPEN_WITH);
    builder.addAction(MainMenu.Attachments.OPEN_FOLDER);
    builder.addAction(MainMenu.Attachments.COPY_FILE_PATH);
    builder.addAction(MainMenu.Attachments.COPY_FILE_URL);
    return builder;
  }

  private static class ReportErrorRunnable implements Runnable {
    private final File myTarget;
    private final IOException myE;
    private final Component myOwner;

    public ReportErrorRunnable(File target, IOException e, Component owner) {
      myTarget = target;
      myE = e;
      myOwner = owner;
    }

    public void run() {
      String message = CANNOT_WRITE_FILE.format(new Object[] {myTarget.getAbsolutePath(), myE.getMessage()});
      JOptionPane.showMessageDialog(myOwner, message, "Save Error", JOptionPane.ERROR_MESSAGE);
    }
  }
}
