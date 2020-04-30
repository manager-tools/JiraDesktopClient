package com.almworks.items.gui.edit.helper;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.util.Env;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.components.AScrollPane;
import com.almworks.util.components.SizeDelegate;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.ScrollBarPolicy;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Shows a confirmation dialog of a user edit that led to illegal values in one or more items.
 */
public class ConfirmEditDialog {
  private final boolean myUpload;
  private final String myDetailMessage;
  private final String myGeneralMessage;
  private boolean mySaveChanges;
  private boolean myUploadAnyway;

  private static final String uploadHeaderTextOne = wrapHeader(
    "This " + Terms.ref_artifact + " has illegal values, Save & Upload may fail or lose them.<br>" +
    "Would you like to continue editing and fix these values?"
//    "It is recommended to continue editing and fix these values."
  );
  private static final String localChangeHeaderTextOne = wrapHeader(
    "This " + Terms.ref_artifact + " has illegal values, Save may fail or lose them.<br>" +
    "Would you like to continue editing and fix these values?"
  );
  private static final String uploadHeaderTextMany = wrapHeader(
    "Some of these " + Terms.ref_artifacts + " have illegal values, Save & Upload may fail or lose them.<br>" +
    "Would you like to continue editing and fix these values?"
  );
  private static final String localChangeHeaderTextMany = wrapHeader(
    "Some of these " + Terms.ref_artifacts + " have illegal values, Save may fail or lose them.<br>" +
    "Would you like to continue editing and fix these values?"
  );

  private static String wrapHeader(String s) {
    return "<html>" + Local.parse(s) + "</html>";
  }

  private ConfirmEditDialog(boolean upload, @NotNull String detailMessage, boolean many) {
    myUpload = upload;
    myDetailMessage = Util.NN(detailMessage);
    myGeneralMessage = upload
      ? many ? uploadHeaderTextMany : uploadHeaderTextOne
      : many ? localChangeHeaderTextMany : localChangeHeaderTextOne
    ;
  }

  /**
   * Shows notification to user that explains that verification of artifact edit failed, providing specified detailed
   * message.
   * @param upload if the user was attempting to upload invalid edit
   * @param many true if there are more than 1 problem items
   * @return verification result
   */
  public static Result show(DialogBuilder dialog, boolean upload, @NotNull String detailMessage, boolean many) {
    ConfirmEditDialog ui = new ConfirmEditDialog(upload, detailMessage, many);
    JComponent content = ui.createContent();
    dialog.setTitle(Env.isMac() ? "" : L.dialog("Confirm " + (upload ? "Save & Upload" : "Save")));
    dialog.setContent(content);
    dialog.setOkCancelAction(ui.createAction(false, dialog));
    dialog.addAction(ui.createAction(true, dialog));
    if (upload) dialog.addAction(ui.createUploadAction(dialog));
    dialog.setModal(true);
    dialog.setResizable(false);
    dialog.setBottomBevel(false);
    dialog.setIgnoreStoredSize(true);
    dialog.showWindow();
    return ui.getResult();
  }

  private AnAction createAction(final boolean saveChanges, final DialogBuilder dialog) {
    return new SimpleAction(getButtonName(saveChanges)) {
      @Override
      protected void customUpdate(UpdateContext context){}

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        mySaveChanges = saveChanges;
        dialog.closeWindow();
      }
    };
  }

  private AnAction createUploadAction(final DialogBuilder dialog) {
    return new SimpleAction("&Upload Anyway") {
      @Override
      protected void customUpdate(UpdateContext context) {}

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        myUploadAnyway = true;
        dialog.closeWindow();
      }
    };
  }

  private String getButtonName(boolean saveChanges) {
    return saveChanges
      ? myUpload
        ? L.actionName("&Save Draft")
        : L.actionName("&Save Anyway")
      : L.actionName("&Continue Editing");
  }

  private JPanel createContent() {
    Icon infoIcon = UIManager.getIcon("OptionPane.warningIcon");
    final JLabel infoLabel = new JLabel(myGeneralMessage, infoIcon, SwingConstants.LEADING);

    JTextArea textArea = new JTextArea();
    textArea.setOpaque(false);
    textArea.setBorder(null);
    textArea.setEditable(false);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);

    AScrollPane scrollPane = new AScrollPane(textArea);
    final int maxHeight = 10 * UIUtil.getLineHeight(textArea);
    scrollPane.setSizeDelegate(new SizeDelegate() {
      @Override
      public Dimension getPreferredSize(JComponent component, Dimension componentSize) {
        Dimension defaultPrefSz = super.getPreferredSize(component, componentSize);
        if (defaultPrefSz.height > maxHeight) {
          return new Dimension(infoLabel.getWidth(), maxHeight);
        } else {
          return defaultPrefSz;
        }
      }
    });
    scrollPane.setRowHeaderView(infoLabel);
    scrollPane.getViewport().setOpaque(false);
    scrollPane.setOpaque(false);
    scrollPane.setBorder(null);
    ScrollBarPolicy.AS_NEEDED.setBoth(scrollPane);
    UIUtil.setTextKeepView(textArea, myDetailMessage);

    DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:d:grow", ""));
    builder.append(infoLabel);
    builder.nextLine();
    builder.appendSeparator("Details");
    builder.append(scrollPane);
    builder.setBorder(new EmptyBorder(5, 5, 0, 5));

    JPanel panel = builder.getPanel();
    Aqua.disableMnemonics(panel);

    return panel;
  }

  private Result getResult() {
    return myUpload && myUploadAnyway
      ? Result.UPLOAD
      : mySaveChanges ? Result.SAVE_LOCALLY : Result.CONTINUE_EDITING;
  }

  public enum Result {
    UPLOAD,
    SAVE_LOCALLY,
    CONTINUE_EDITING;

    public boolean isUpload() {
      return this == UPLOAD;
    }

    public boolean isContinueEdit() {
      return this == CONTINUE_EDITING;
    }
  }
}