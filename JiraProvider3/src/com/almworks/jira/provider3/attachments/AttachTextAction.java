package com.almworks.jira.provider3.attachments;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.config.MiscConfig;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.misc.WorkArea;
import com.almworks.engine.gui.attachments.AddAttachmentCallback;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.io.IOUtils;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

class AttachTextAction extends SimpleAction {
  public AttachTextAction() {
    super("Attach Text\u2026", Icons.ACTION_ATTACH_TEXT);
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    AttachFileAction.ensureCanAttach(context);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    DialogBuilder builder = context.getSourceObject(DialogManager.ROLE).createBuilder("jira.attachText");
    String issueKey = MetaSchema.issueKey(issue);
    MiscConfig miscConfig = context.getSourceObject(MiscConfig.ROLE);
    final SyncManager syncManager = context.getSourceObject(SyncManager.ROLE);
    attach(builder, issueKey, miscConfig, new AddAttachmentCallback() {
      @Override
      public void addAttachment(File file, String name, String mimeType) {
        AttachEditFeature.createAttachment(syncManager, issue.getItem(), file, name, mimeType);
      }
    });
  }

  public static void attach(DialogBuilder builder, String issueKey, MiscConfig miscConfig, AddAttachmentCallback callback) {
    String title = "Attach Text to " + Util.NN(issueKey, "New Issue");
    DetachComposite life = new DetachComposite();
    AttachTextForm form = AttachTextForm.create(life, miscConfig.getConfig("attachTextForm"), miscConfig);
    form.pasteText();
    builder.setContent(form.getComponent());
    builder.setTitle(title + " - " + Local.text(Terms.key_Deskzilla));
    builder.setEmptyCancelAction();
    builder.setEmptyOkAction();
    builder.setOkAction(new MyOkAction(form, title, callback));
    builder.setModal(false);
    builder.setPreferredSize(new Dimension(300, 300));
    builder.setInitialFocusOwner(form.getFileNameField());
    builder.showWindow(life);
  }

  public static void attachText(DialogManager dialogs, String errorTitle, File uploadDir, String rawFileName, String mimeType, String content, Charset charset, AddAttachmentCallback callback) {
    rawFileName = FileUtil.excludeForbddenChars(rawFileName);
    Pair<String, String> nameExt = FileUtil.getNameAndExtension(rawFileName, "attachment", "txt");
    final File outputFile = FileUtil.createFileMaybeAdjusted(uploadDir, nameExt.getFirst(), nameExt.getSecond());
    if (outputFile == null)
      return;
    FileOutputStream stream = null;
    try {
      stream = new FileOutputStream(outputFile);
      stream.write(content.getBytes(charset));
    } catch (IOException e) {
      dialogs.showErrorMessage(errorTitle, e.getLocalizedMessage());
      return;
    } finally {
      IOUtils.closeStreamIgnoreExceptions(stream);
    }
    String attachmentName = nameExt.getFirst() + "." + nameExt.getSecond();
    callback.addAttachment(outputFile, attachmentName, mimeType);
  }

  private static class MyOkAction extends SimpleAction {
    private final AttachTextForm myForm;
    private final String myTitle;
    private final AddAttachmentCallback myCallback;

    public MyOkAction(AttachTextForm form, String title, AddAttachmentCallback callback) {
      super("Attach");
      myForm = form;
      myTitle = title;
      myCallback = callback;
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      myForm.subscribeUpdates(context.getUpdateRequest());
      CantPerformException.ensureNotNull(myForm.getCharset());
      CantPerformException.ensure(myForm.isTextNotEmpty());
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      final String fileName = myForm.getFileNameField().getText();
      String mimeType = Util.NN(myForm.getMimeTypes().getModel().getSelectedItem()).trim();
      final String finalMimeType = mimeType.isEmpty() ? null : mimeType;
      final String text = myForm.getText();
      final Charset charset = myForm.getCharset();
      final File uploadDir = context.getSourceObject(WorkArea.APPLICATION_WORK_AREA).getUploadDir();
      final DialogManager dialogs = context.getSourceObject(DialogManager.ROLE);
      ThreadGate.LONG.execute(new Runnable() {
        @Override
        public void run() {
          attachText(dialogs, myTitle, uploadDir, fileName, finalMimeType, text, charset, myCallback);
        }
      });
    }
  }
}
