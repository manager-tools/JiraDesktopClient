package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadedFile;
import com.almworks.util.English;
import com.almworks.util.Env;
import com.almworks.util.config.Configuration;
import com.almworks.util.files.FileActions;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;

public interface AttachmentShowStrategy {
  void showFile(File file, String mimeType, Configuration viewConfig, Component owner, String title, String description);

  void showAttachment(File file, String mimeType, Attachment attachment, Configuration viewConfig, Component owner);

  void showAttachment(DownloadedFile dFile, Attachment attachment, Configuration viewConfig, Component owner);

  String getName(int size);

  AttachmentShowStrategy DEFAULT = new AttachmentShowStrategy() {
    private final String VIEW_EXTERNAL = "attachments.view.external";
    private final String DEFAULT_TYPES = "application/*;video/*";
    private final String UNKNOWN_MIME = "application/octet-stream";
    @Override
    public void showFile(File file, String mimeType, Configuration viewConfig, Component owner, String title, String description) {
      chooseStrategy(file, mimeType).showFile(file, mimeType, viewConfig, owner, title, description);
    }

    @Override
    public void showAttachment(File file, String mimeType, Attachment attachment, Configuration viewConfig, Component owner) {
      chooseStrategy(file, mimeType).showAttachment(file, mimeType, attachment, viewConfig, owner);
    }

    @Override
    public void showAttachment(DownloadedFile dFile, Attachment attachment, Configuration viewConfig, Component owner) {
      chooseStrategy(dFile.getFile(), dFile.getMimeType()).showAttachment(dFile, attachment, viewConfig, owner);
    }

    @NotNull
    private AttachmentShowStrategy chooseStrategy(File file, String mimeType) {
      String string = Util.NN(Env.getString(VIEW_EXTERNAL, DEFAULT_TYPES));
      if (mimeType == null) mimeType = AttachmentContent.guessMimeType(file);
      if (mimeType == null) return VIEW;
      int prefixEnd = mimeType.indexOf(';');
      if (prefixEnd >= 0) mimeType = mimeType.substring(0, prefixEnd);
      String[] extMimeTypes = string.split("\\;");
      if (UNKNOWN_MIME.equals(mimeType) && ArrayUtil.indexOf(extMimeTypes, "application/octet-stream") < 0) {
        // Some text files has mimeType application/octet-stream. So they matches "application/*" pattern.
        // If this is the case try better mimeType from extension. But do not this if application/octet-stream is explicitly specified
        String guess = AttachmentContent.guessMimeType(file);
        if (guess != null) mimeType = guess;
      }

      for (String type : extMimeTypes) {
        boolean matches;
        if (type.endsWith("*")) {
          type = type.substring(0, type.length() - 1);
          matches = mimeType.startsWith(type);
        } else matches = mimeType.equals(type);
        if (matches) return OPEN;
      }
      return VIEW;
    }

    @Override
    public String getName(int size) {
      return "View " + English.getSingularOrPlural("Attachment", size);
    }
  };

    AttachmentShowStrategy VIEW = new AttachmentShowStrategy() {
    @Override
    public void showFile(File file, String mimeType, Configuration viewConfig, Component owner, String title,
        String description) {
      AttachmentsControllerUtil.viewFile(file, mimeType, viewConfig, owner, title, description);
    }

    @Override
    public void showAttachment(File file, String mimeType, Attachment attachment, Configuration viewConfig, Component owner) {
      showFile(file, mimeType, viewConfig, owner, AttachmentsControllerUtil.getTitle(attachment), AttachmentsControllerUtil.getDescription(attachment));
    }

    @Override
    public void showAttachment(DownloadedFile dFile, Attachment attachment, Configuration viewConfig, Component owner) {
      showAttachment(dFile.getFile(), dFile.getMimeType(), attachment, viewConfig, owner);
    }

    @Override
    public String getName(int size) {
      return "View " + English.getSingularOrPlural("Attachment", size) + " with Internal Viewer";
    }
  };

  AttachmentShowStrategy OPEN = new AttachmentShowStrategy() {
    @Override
    public void showFile(File file, String mimeType, Configuration viewConfig, Component owner, String title,
        String description) {
      FileActions.openFile(file, owner);
    }

    @Override
    public void showAttachment(File file, String mimeType, Attachment attachment, Configuration viewConfig, Component owner) {
      FileActions.openFile(file, owner);
    }

    @Override
    public void showAttachment(DownloadedFile dFile, Attachment attachment, Configuration viewConfig, Component owner) {
      FileActions.openFile(dFile.getFile(), owner);
    }

    @Override
    public String getName(int size) {
      return "Open with Default " + English.getSingularOrPlural("Program", size);
    }
  };
}
