package com.almworks.engine.gui.attachments;

import com.almworks.util.threads.ThreadAWT;
import org.jetbrains.annotations.Nullable;

public interface AttachmentsEnv {

  AttachmentDownloadStatus<? extends Attachment> getDownloadStatus();

  @ThreadAWT
  void repaintAttachment(String url);

  @ThreadAWT
  void revalidateAttachments();

  @Nullable
  String getTooltipText(Attachment item);
}
