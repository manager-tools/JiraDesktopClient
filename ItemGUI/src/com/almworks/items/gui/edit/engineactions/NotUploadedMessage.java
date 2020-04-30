package com.almworks.items.gui.edit.engineactions;

import com.almworks.api.config.MiscConfig;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Failure;
import org.almworks.util.detach.Detach;

import javax.swing.*;

public class NotUploadedMessage implements EditCommit {
  private static final String CANCELED_EARLIER = "canceledEarlier";

  private final DialogBuilder myBuilder;
  private final String myTitle;
  private final String myMessage;
  private final JCheckBox myDontShowAgain;
  private final Configuration myConfig;

  private NotUploadedMessage(DialogBuilder builder, String title, String message, Configuration config) {
    myBuilder = builder;
    myTitle = title;
    myMessage = message;
    myConfig = config;
    myDontShowAgain = new JCheckBox(L.checkbox("Don't show this message in the future"));
    myDontShowAgain.setMnemonic('d');
  }

  private static NotUploadedMessage create(ActionContext context, String title, String message)
      throws CantPerformException
  {
    DialogBuilder builder = context.getSourceObject(DialogManager.ROLE).createMainBuilder("notUploadedMessage");
    Configuration config = context.getSourceObject(MiscConfig.ROLE).getConfig("notUploadedMessage");
    return new NotUploadedMessage(builder, title, message, config);
  }

  public static NotUploadedMessage create(ActionContext context) throws CantPerformException {
    String title = null;
    String message = null;
    try {
      EditDescriptor.DescriptionStrings descriptions = context.getSourceObject(EditDescriptor.DESCRIPTION_STRINGS);
      title = descriptions.getNotUploadedTitle();
      message = descriptions.getNotUploadedMessage();
    } catch (CantPerformException e) {
      // ignore
    }
    if (title == null) title = Local.parse("Info - " + Terms.ref_Deskzilla);
    if (message == null) message = "Changes were saved in the local database.";
    return create(context, title, message);
  }

  public Procedure<LongList> asProcedure() {
    return new Procedure<LongList>() {
      @Override
      public void invoke(LongList arg) {
        if (arg != null) ThreadGate.AWT.execute(new Runnable() {
          @Override
          public void run() {
            NotUploadedMessage.this.showWindow();
          }
        });
      }};
  }

  public static void addTo(ActionContext context, AggregatingEditCommit commit, String title)
      throws CantPerformException
  {
    String message = L.content("Your changes are saved in the local database");
    commit.addProcedure(ThreadGate.AWT, create(context, title, message));
  }

  private void showWindow() {
    if (myConfig.getBooleanSetting(CANCELED_EARLIER, false))
      return;
    myBuilder.setTitle(myTitle);
    setMessage(myMessage, JOptionPane.INFORMATION_MESSAGE);
    myBuilder.setModal(true);
    myBuilder.setBottomBevel(false);
    myBuilder.setBottomLineComponent(myDontShowAgain);
    myBuilder.setEmptyOkCancelAction();
    myBuilder.showWindow(new Detach() {
      protected void doDetach() {
        myConfig.setSetting(CANCELED_EARLIER, myDontShowAgain.isSelected());
      }
    });
  }

  private void setMessage(String message, int type) {
    Icon icon;
    switch(type) {
      case JOptionPane.PLAIN_MESSAGE: icon = null; break;
      case JOptionPane.ERROR_MESSAGE: icon = UIManager.getIcon("OptionPane.errorIcon");  break;
      case JOptionPane.INFORMATION_MESSAGE: icon = UIManager.getIcon("OptionPane.informationIcon"); break;
      case JOptionPane.WARNING_MESSAGE: icon = UIManager.getIcon("OptionPane.warningIcon"); break;
      case JOptionPane.QUESTION_MESSAGE: icon = UIManager.getIcon("OptionPane.questionIcon"); break;
      default: throw new Failure(String.valueOf(type));
    }
    myBuilder.setContent(new JLabel(message, icon, SwingConstants.LEADING));
  }

  @Override
  public void performCommit(EditDrain drain) throws DBOperationCancelledException {}

  @Override
  public void onCommitFinished(boolean success) {
    if (!success)
      return;
    showWindow();
  }
}
