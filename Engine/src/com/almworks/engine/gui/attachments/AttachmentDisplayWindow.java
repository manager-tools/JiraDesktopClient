package com.almworks.engine.gui.attachments;

import com.almworks.api.config.MiscConfig;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.engine.gui.attachments.content.ContentView;
import com.almworks.util.Terms;
import com.almworks.util.components.AActionButton;
import com.almworks.util.components.ALabel;
import com.almworks.util.config.Configuration;
import com.almworks.util.files.FileActions;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.SimpleProvider;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import java.awt.*;
import java.io.File;

class AttachmentDisplayWindow {

  public static void showFile(File file, String mimeType, String title, String description, final Configuration viewConfig, DialogManager dm, final MiscConfig miscConfig) {
    final DetachComposite life = new DetachComposite();

    final JPanel wholePanel = new JPanel(UIUtil.createBorderLayout());

    final SimpleProvider dataProvider = new SimpleProvider(FileData.FILE_DATA);
    DataProvider.DATA_PROVIDER.putClientValue(wholePanel, dataProvider);

    final AttachmentContent attachmentContent = new AttachmentContent(file, mimeType);
    attachmentContent.setListener(new AttachmentContent.Listener() {
      public void onLoaded(FileData data, ContentView contentView) {
        if (contentView != null)
          contentView.init(life, viewConfig, miscConfig);
        wholePanel.revalidate();
        wholePanel.repaint();
        if (data == null)
          dataProvider.removeAllData();
        else
          dataProvider.setSingleData(FileData.FILE_DATA, data);
      }
    });
    attachmentContent.loadFile();

    JPanel topPanel = createTopPanel(file, description, viewConfig, wholePanel, attachmentContent);
    wholePanel.add(topPanel, BorderLayout.NORTH);
    wholePanel.add(attachmentContent.getComponent(), BorderLayout.CENTER);
    DialogBuilder builder = dm.createBuilder("showFile_" + Util.NN(mimeType, "notype"));
    builder.setTitle(title + " - " + Local.text(Terms.key_Deskzilla));
    builder.setModal(false);
    builder.setCancelAction("Close Window");
    builder.setContent(wholePanel);
    builder.setPreferredSize(new Dimension(700, 560));
    builder.showWindow(life);
  }

  private static JPanel createTopPanel(File file, String description, Configuration viewConfig, JPanel wholePanel, final AttachmentContent attachmentContent) {
    ALabel descriptionLabel = new ALabel(description);
    descriptionLabel.setHorizontalAlignment(SwingConstants.LEADING);
    UIUtil.adjustFont(descriptionLabel, 1.05F, Font.BOLD, true);

    AnAction toClipboardAction = new FileDataAction("Copy to &Clipboard") {
      protected void perform(FileData data) {
        attachmentContent.copyToClipboard();
      }
    };

    AnAction saveAsAction = AttachmentUtils.createSaveAsAction(file, wholePanel, viewConfig.getOrCreateSubset("save"));

    JPanel toolbar = new JPanel(new InlineLayout(InlineLayout.HORISONTAL, 5));
    toolbar.add(createButton(saveAsAction, wholePanel));
    toolbar.add(createButton(toClipboardAction, wholePanel));
    if (FileActions.isSupported(FileActions.Action.OPEN_AS)) {
      toolbar.add(createButton(AttachmentUtils.createOpenWithAction(file, wholePanel), wholePanel));
    }
    if (FileActions.isSupported(FileActions.Action.OPEN_CONTAINING_FOLDER)) {
      toolbar.add(createButton(AttachmentUtils.createOpenContainingFolderAction(file, wholePanel), wholePanel));
    }

    JPanel topPanel = new JPanel(UIUtil.createBorderLayout());
    topPanel.add(descriptionLabel, BorderLayout.CENTER);
    topPanel.add(toolbar, BorderLayout.SOUTH);
    return topPanel;
  }

  private static AActionButton createButton(AnAction action, JPanel wholePanel) {
    AActionButton button = new AActionButton(action);
    button.setContextComponent(wholePanel);
    return button;
  }
}
