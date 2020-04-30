package com.almworks.engine.gui.attachments;

import java.io.File;

public interface AddAttachmentCallback {
  void addAttachment(File file, String name, String mimeType);
}
