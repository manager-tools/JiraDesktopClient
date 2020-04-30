package com.almworks.api.application.util;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.viewer.Comment;
import com.almworks.engine.gui.attachments.Attachment;

import java.util.List;

public class ExportDescription {
  private final String myTypeDisplayName;
  private final List<? extends ItemExport> myExports;
  private final ItemExport myKeyExport;
  private final ItemExport mySummaryExport;
  private final ModelKey<List<Comment>> myComments;
  private final ModelKey<List<Attachment>> myAttachments;

  public ExportDescription(String typeDisplayName, List<? extends ItemExport> exports, ItemExport keyExport, ItemExport summaryExport, ModelKey<List<Comment>> comments,
    ModelKey<List<Attachment>> attachments) {
    myTypeDisplayName = typeDisplayName;
    myExports = exports;
    myKeyExport = keyExport;
    mySummaryExport = summaryExport;
    myComments = comments;
    myAttachments = attachments;
  }

  public String getTypeDisplayName() {
    return myTypeDisplayName;
  }

  public List<? extends ItemExport> getExports() {
    return myExports;
  }

  public ItemExport getItemKeyExport() {
    return myKeyExport;
  }

  public ItemExport getItemSummaryExport() {
    return mySummaryExport;
  }

  public ModelKey<List<Comment>> getComments() {
    return myComments;
  }

  public ModelKey<List<Attachment>> getAttachments() {
    return myAttachments;
  }
}
