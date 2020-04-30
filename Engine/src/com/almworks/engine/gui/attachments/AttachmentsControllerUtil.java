package com.almworks.engine.gui.attachments;

import com.almworks.api.config.MiscConfig;
import com.almworks.api.download.DownloadManager;
import com.almworks.api.download.DownloadedFile;
import com.almworks.api.download.FileDownloadListener;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.config.Configuration;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.progress.ProgressActivityFormat;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Date;

import static com.almworks.api.download.DownloadedFile.State.*;

public class AttachmentsControllerUtil {
  public static void initiateDownload(DownloadManager downloadManager, Attachment attachment) {
    LogHelper.assertNotNull(downloadManager, attachment);
    if (downloadManager == null) return;
    String url = attachment.getUrl();
    if (url == null) {
      assert false : attachment;
      return;
    }
    downloadManager.initiateDownload(url, attachment.createDownloadRequest(), true, false);
  }

  public static void viewFile(File file, String mimeType, Configuration viewConfig, Component owner, String title,
    String description)
  {
    if (owner == null) {
      assert false;
      return;
    }
    if (!isGoodFile(file))
      return;
    DialogManager dm = ActionUtil.getActor(owner, DialogManager.ROLE);
    MiscConfig miscConfig = ActionUtil.getActor(owner, MiscConfig.ROLE);
    AttachmentDisplayWindow.showFile(file, mimeType, title, description, viewConfig, dm, miscConfig);
  }

  public static boolean isGoodFile(File file) {
    return file != null && file.isFile() && file.canRead();
  }

  public static String getTitle(Attachment attachment) {
    String fileName = attachment.getDisplayName();
    return fileName == null ? "Attachment" : fileName;
  }

  public static String getDescription(Attachment attachment) {
    Date created = attachment.getDate();
    String createdStr = created != null ? DateUtil.toLocalDate(created) : attachment.getDateString();
    String user = attachment.getUser();
    StringBuffer sb = new StringBuffer(getTitle(attachment));
    if (user != null || createdStr != null)
      sb.append(", uploaded");
    if (createdStr != null)
      sb.append(" ").append(createdStr);
    if (user != null)
      sb.append(" by ").append(user);
    return sb.toString();
  }

  public static void downloadAndShowAttachment(AttachmentsController controller, Attachment attachment, @NotNull Component parentComponent, Configuration viewConfig,
    AttachmentShowStrategy strat)
  {
    AttachmentDownloadStatus<? extends Attachment> status = controller.getDownloadStatus();
    File file = attachment.getLocalFile(status);
    if (file != null) strat.showAttachment(file, attachment.getMimeType(status), attachment, viewConfig, parentComponent);
    else if (!attachment.isLocal()) {
      DownloadedFile dfile = attachment.getDownloadedFile(status);
      if (isDownloadNeeded(dfile)) {
        initiateDownload(status.getDownloadManager(), attachment);
        waitDownloadedAndShow(true, attachment, viewConfig, parentComponent, strat);
      } else {
        if (dfile != null) {
          DownloadedFile.State state = dfile.getState();
          if (state != READY) waitDownloadedAndShow(false, attachment, viewConfig, parentComponent, strat);
          else strat.showAttachment(dfile, attachment, viewConfig, parentComponent);
        }
      }
    }
  }

  private static void waitDownloadedAndShow(boolean canCancel, final Attachment attachment, final Configuration viewConfig, final Component parentComponent,
      AttachmentShowStrategy strat)
  {
    String url = attachment.getUrl();
    if (url == null) {
      assert false : attachment;
      return;
    }
    DownloadManager downloadManager = Context.require(DownloadManager.ROLE);
    DialogManager dm = Context.require(DialogManager.ROLE);
    waitDownloadAndShow(dm, downloadManager, canCancel, url, getTitle(attachment), getDescription(attachment), viewConfig, parentComponent, strat);
  }

  public static void waitDownloadAndShow(DialogManager dm, DownloadManager downloadManager, boolean canCancel, String url, final String title, final String description,
      final Configuration viewConfig, final Component parentComponent, AttachmentShowStrategy strat)
  {
    FileDownloadListener.Tracker.perform(downloadManager, url, ThreadGate.AWT,
      new DownloadProgress(dm, viewConfig, parentComponent, title, description, downloadManager, url, strat));
  }

  public static boolean isDownloadNeeded(DownloadedFile dfile) {
    if (dfile == null) return true;
    DownloadedFile.State state = dfile.getState();
    return state == null || state == DOWNLOAD_ERROR || state == LOST || state == UNKNOWN;
  }

  public static void registerActions(ActionRegistry registry) {
    registry.registerAction(MainMenu.Attachments.DOWNLOAD_ALL, DownloadAllAttachmentsAction.INSTANCE);
    registry.registerAction(MainMenu.Attachments.DOWNLOAD, DownloadAttachmentsAction.INSTANCE);
    registry.registerAction(MainMenu.Attachments.SAVE_AS, SaveAttachmentAction.INSTANCE);
    registry.registerAction(MainMenu.Attachments.DOWNLOAD_AND_VIEW, DownloadAndShowAttachmentAction.DEFAULT_INSTANCE);
    registry.registerAction(MainMenu.Attachments.DOWNLOAD_AND_VIEW_INTERNAL, DownloadAndShowAttachmentAction.VIEW_INSTANCE);
    registry.registerAction(MainMenu.Attachments.DOWNLOAD_AND_OPEN_EXTERNAL, DownloadAndShowAttachmentAction.OPEN_INSTANCE);
    registry.registerAction(MainMenu.Attachments.COPY_FILE_URL, CopyFileURLAction.INSTANCE);
    registry.registerAction(MainMenu.Attachments.SAVE_ALL, SaveAllAttachmentsAction.INSTANCE);
    registry.registerAction(MainMenu.Attachments.COPY_FILE_PATH, CopyFileLocationAction.INSTANCE);
    AttachmentFileAction.register(registry);

  }

  private static class DownloadProgress implements FileDownloadListener, AnActionListener {
    private final DialogManager myDialogs;
    private final Configuration myViewConfig;
    private final Component myParentComponent;
    private final String myTitle;
    private final String myDescription;
    private final DownloadManager myDownload;
    @Nullable private final String myUrl;
    private DownloadProgressForm myVisibleForm = null;
    private boolean myProgressShown = false;
    private DialogBuilder myBuilder;
    private AttachmentShowStrategy myShowStrategy;

    public DownloadProgress(DialogManager dialogs, Configuration viewConfig, Component parentComponent, String title,
      String description, @Nullable DownloadManager dm, @Nullable String url, AttachmentShowStrategy strat) {
      myDialogs = dialogs;
      myViewConfig = viewConfig;
      myParentComponent = parentComponent;
      myTitle = title;
      myDescription = description;
      myDownload = dm;
      myUrl = url;
      myShowStrategy = strat;
    }

    public void onDownloadStatus(DownloadedFile dFile) {
      Threads.assertAWTThread();
      if (dFile.getState() == READY) {
        closeProgress();
        myShowStrategy.showFile(dFile.getFile(), dFile.getMimeType(), myViewConfig, myParentComponent, myTitle, myDescription);
      } else if (dFile.getState() == DOWNLOAD_ERROR) showError(dFile.getLastDownloadError());
      else showProgress(dFile);
    }

    private void showProgress(DownloadedFile dFile) {
      DownloadProgressForm content = getVisibleForm();
      if (content == null) return;
      DownloadedFile.State state = dFile.getState();
      content.setState(DownloadedFile.State.getStateString(state));
      if (state == DOWNLOADING) content.setProgress(dFile.getDownloadProgressSource());
      else content.setProgress(null);
    }

    private DownloadProgressForm getVisibleForm() {
      if (myVisibleForm == null && !myProgressShown) {
        myProgressShown = true;
        showForm();
      }
      return myVisibleForm;
    }

    private DownloadProgressForm showForm() {
      if (myVisibleForm != null) return myVisibleForm;
      myVisibleForm = new DownloadProgressForm();
      myBuilder = myDialogs.createBuilder("attachmentDownloadProgress");
      myBuilder.setTitle("Download: " + myTitle);
      myBuilder.setContent(myVisibleForm);
      if (myDownload != null && myUrl != null) {
        myBuilder.setCancelAction("Cancel");
        myBuilder.addCancelListener(this);
      } else myBuilder.setCancelAction("Close");
      myBuilder.showWindow();
      return myVisibleForm;
    }

    private void showError(String error) {
      DownloadProgressForm form = showForm();
      form.setError(error);
    }

    private void closeProgress() {
      if (myBuilder == null) return;
      try {
        myBuilder.closeWindow();
      } catch (CantPerformException e) {
        Log.error(e);
      } finally {
        myBuilder = null;
        myVisibleForm = null;
      }
    }

    public void perform(ActionContext context) throws CantPerformException {
      myDownload.cancelDownload(myUrl);
    }
  }

  private static class DownloadProgressForm implements UIComponentWrapper, ChangeListener {
    private JPanel myWholePanel;
    private JProgressBar myProgressBar;
    private JLabel myState;
    private JLabel myLastMessage;
    private ProgressSource myProgress;
    private String myError;

    private DownloadProgressForm() {
      myLastMessage.setVisible(false);
    }

    public JComponent getComponent() {
      return myWholePanel;
    }

    public void dispose() {
      stopListenProgress();
    }

    public void setState(String state) {
      myState.setText(state);
    }

    public void setProgress(ProgressSource progress) {
      if (progress == myProgress) return;
      if (myProgress != null) stopListenProgress();
      if (progress != null) {
        myProgress = progress;
        myProgress.getModifiable().addAWTChangeListener(Lifespan.FOREVER, this);
        onChange();
      }
      myProgressBar.setVisible(myProgress != null);
    }

    private void stopListenProgress() {
      if (myProgress != null) {
        myProgress.getModifiable().removeChangeListener(this);
        myProgress = null;
      }
    }

    public void onChange() {
      if (myError != null) return;
      if (myProgress == null) return;
      myProgressBar.setValue((int) (myProgress.getProgress() * 100));
      showMessage(ProgressActivityFormat.DEFAULT.format(myProgress.getActivity()));
    }

    public void setError(String error) {
      myError = error;
      myProgressBar.setVisible(false);
      showMessage(myError);
      myLastMessage.setForeground(GlobalColors.ERROR_COLOR);
      myState.setText("Failed");
    }

    private void showMessage(String message) {
      message = Util.NN(message).trim();
      if (message.length() == 0) myLastMessage.setVisible(false);
      else {
        myLastMessage.setVisible(true);
        myLastMessage.setText(message);
      }
    }
  }
}
