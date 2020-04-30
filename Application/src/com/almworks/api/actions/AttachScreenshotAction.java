package com.almworks.api.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.config.MiscConfig;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.screenshot.ScreenShooter;
import com.almworks.api.screenshot.Screenshot;
import com.almworks.engine.gui.attachments.AddAttachmentCallback;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.files.FileUtil;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;


public abstract class AttachScreenshotAction extends SimpleAction {
  public static final Role<Image> IMAGE = Role.role("image");

  protected AttachScreenshotAction() {
    super("Attach Screenshot\u2026", Icons.ACTION_ATTACH_SCREENSHOT);
    setDefaultPresentation(
      PresentationKey.SHORT_DESCRIPTION,
      "Attach a screenshot to the selected " + Terms.ref_artifact);
  }
  

  protected void customUpdate(UpdateContext context) throws CantPerformException {
//    DBDataRoles.checkAnyConnectionHasCapability(context, Connection.Capability.EDIT_ITEM);
    ItemActionUtils.basicUpdate(context, false);
    ItemWrapper item = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    CantPerformException.ensure(CantPerformException.ensureNotNull(item.getConnection()).isUploadAllowed());
  }

  protected void doPerform(final ActionContext context) throws CantPerformException {
    final ItemWrapper item = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    Image image = null;
    try {
      image = context.getSourceObject(IMAGE);
    } catch (CantPerformException e) {
      // fall through
    }

    attachImage(context, item, image);
  }

  public void attachImage(ActionContext context, final ItemWrapper item, Image image) throws CantPerformException {
    final SyncManager syncMan = context.getSourceObject(SyncManager.ROLE);
    MiscConfig miscConfig = context.getSourceObject(MiscConfig.ROLE);
    attach(context, image, miscConfig, new AddAttachmentCallback() {
      @Override
      public void addAttachment(final File file, final String name, final String mimeType) {
        syncMan.commitEdit(new EditCommit.Adapter() {
          @Override
          public void performCommit(EditDrain drain) throws DBOperationCancelledException {
            makeAttach(item.getItem(), file, name, drain, mimeType);
          }
        });
      }
    });
  }

  public static void attach(ActionContext context, @Nullable Image image, MiscConfig miscConfig, final AddAttachmentCallback callback) throws CantPerformException {
    ScreenShooter ss = context.getSourceObject(ScreenShooter.ROLE);
    final File uploadDir = context.getSourceObject(WorkArea.APPLICATION_WORK_AREA).getUploadDir();
    Configuration conf = miscConfig.getConfig("screenShooter");
    Procedure<Screenshot> acceptor = new Procedure<Screenshot>() {
      public void invoke(Screenshot arg) {
        String rawFileName = FileUtil.excludeForbddenChars(arg.getAttachmentName());
        Pair<String, String> nameExt = FileUtil.getNameAndExtension(rawFileName, "screenshot", "png");
        File output = FileUtil.createFileMaybeAdjusted(uploadDir, nameExt.getFirst(), nameExt.getSecond());
        if (output == null)
          return;
        try {
          ImageIO.write(arg.getImage(), "png", output);
        } catch (IOException e) {
          Log.warn("cannot save screenshot", e);
        }
        String attachmentName = nameExt.getFirst() + "." + nameExt.getSecond();
        callback.addAttachment(output, attachmentName, "image/png");
      }
    };
    if (image == null)
      ss.shoot(context.getComponent(), conf, acceptor);
    else
      ss.edit(image, conf,  acceptor);
  }

  protected abstract void makeAttach(long primaryItem, File file, String description, EditDrain drain, String mimeType);
}