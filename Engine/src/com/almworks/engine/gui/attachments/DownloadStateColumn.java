package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadedFile;
import com.almworks.util.components.CollectionRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.models.SimpleColumnAccessor;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import static com.almworks.api.download.DownloadedFile.State.DOWNLOADING;
import static com.almworks.api.download.DownloadedFile.State.UNKNOWN;

class DownloadStateColumn extends SimpleColumnAccessor<Attachment>
  implements CollectionRenderer<Attachment>
{
  private final AttachmentDownloadStatus<?> myStatus;
  private final JLabel myLabel = new JLabel();
  private final Border myLabelBorder = new EmptyBorder(0, 0, 0, 4);
  private final JProgressBar myProgressBar = new JProgressBar(0, 100);

  public DownloadStateColumn(AttachmentDownloadStatus<?> status) {
    super("State");
    myStatus = status;

    UIUtil.addOuterBorder(myProgressBar, new EmptyBorder(1, 4, 1, 4));
    myLabel.setOpaque(true);
    myProgressBar.setOpaque(false);
  }

  public CollectionRenderer<Attachment> getDataRenderer() {
    return this;
  }

  public JComponent getRendererComponent(CellState state, Attachment item) {
    DownloadedFile dfile = item.getDownloadedFile(myStatus);
    DownloadedFile.State fileState = dfile == null ? UNKNOWN : dfile.getState();
    if (fileState == DOWNLOADING) {
      assert dfile != null;
      double progress = dfile.getDownloadProgressSource().getProgress();
      myProgressBar.setValue((int) (100F * progress));
      return myProgressBar;
    } else {
      state.setToLabel(myLabel, myLabelBorder);
      myLabel.setText(AttachmentsPanel.getStateString(item, fileState));
      return myLabel;
    }
  }
}
