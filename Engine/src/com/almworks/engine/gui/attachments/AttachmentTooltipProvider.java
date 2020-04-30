package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadedFile;
import org.almworks.util.Util;

public abstract class AttachmentTooltipProvider<T extends Attachment> {
  public abstract void addTooltipText(StringBuilder tooltip, T item, DownloadedFile dfile);

  protected StringBuilder lineBreak(StringBuilder tooltip) {
    if (tooltip.length() > 0)
      tooltip.append("<br>");
    return tooltip;
  }

  protected void append(StringBuilder tooltip, T item, DownloadedFile dfile,
    AttachmentProperty<? super T, ?> property, String prefix, String suffix)
  {
    String text = property.getStringValue(item, dfile);
    if (text == null || text.length() == 0) return;
    text = text.replaceAll("[<>]", "");
    lineBreak(tooltip);
    if (prefix != null)
      tooltip.append(prefix);
    tooltip.append(text);
    if (suffix != null)
      tooltip.append(suffix);
  }

  protected void appendSize(StringBuilder tooltip, Attachment item, DownloadedFile dfile) {
    String size = Util.NN(AttachmentProperty.SIZE.getStringValue(item, dfile));
    if (size.length() > 0) {
      lineBreak(tooltip).append("Size: ").append(size);
    }
  }
}
