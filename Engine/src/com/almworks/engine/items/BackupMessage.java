package com.almworks.engine.items;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.L;
import com.almworks.util.components.AToolbarButton;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.EnabledAction;
import com.almworks.util.ui.actions.PresentationKey;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class BackupMessage {
  private JPanel myWholePanel;
  private JTextField myBackupPath;
  private AToolbarButton myCopyBackupPath;
  private AToolbarButton myCopyExportedTagsPath;
  private JTextField myExportedTagsPath;
  private JLabel myBackupPathLabel;
  private JLabel myMessage;

  public static void showMessage(DialogManager manager, @Nullable File backup, String tagFileName) {
    DialogBuilder builder = manager.createBuilder("showDBBackupDone");
    builder.setTitle(L.frame("Backup"));
    BackupMessage form = new BackupMessage();
    builder.setContent(form.myWholePanel);
    builder.setEmptyOkAction();
    if (backup != null) {
      form.myBackupPath.setText(backup.getAbsolutePath());
      form.myMessage.setText("Backup copy of the old database has been successfully created.");
    }
    else {
      form.hideBackupRow();
      form.myMessage.setText("Tags has been exported from the old database.");
    }
    form.myExportedTagsPath.setText(tagFileName);
    setupAction(form.myCopyBackupPath, form.myBackupPath);
    setupAction(form.myCopyExportedTagsPath, form.myExportedTagsPath);
    builder.setIgnoreStoredSize(true);
    builder.showWindow();
  }

  private static void setupAction(AToolbarButton copyButton, final JTextField textField) {
    copyButton.setAnAction(new EnabledAction("", Icons.ACTION_COPY) {
      { setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Copy to clipboard"); }
      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        UIUtil.copyToClipboard(textField.getText());
      }
    });
  }

  private void hideBackupRow() {
    LayoutManager layout = myWholePanel.getLayout();
    if (layout instanceof FormLayout) {
      FormLayout formLayout = (FormLayout) layout;
      formLayout.setRowSpec(3, new RowSpec(Sizes.ZERO));
      formLayout.setRowSpec(4, new RowSpec(Sizes.ZERO));
    }
  }

  private void createUIComponents() {
    myBackupPath = UIUtil.createCopyableLabel();
    myExportedTagsPath = UIUtil.createCopyableLabel();
  }
}

