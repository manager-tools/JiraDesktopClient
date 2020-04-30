package com.almworks.engine.items;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.Terms;
import com.almworks.util.components.AScrollPane;
import com.almworks.util.components.SizeDelegate;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;

import javax.swing.*;

class CleanDBForm {
  private JPanel myWholePanel;
  private JTextArea myMessage;
  private JCheckBox myBackup;
  private AScrollPane myMessagePane;
  private JLabel myIconPlace;
  private boolean myErase = false;

  CleanDBForm() {
    Icon warningIcon = UIManager.getIcon("OptionPane.informationIcon");
    if (warningIcon == null) myIconPlace.setVisible(false);
    else myIconPlace.setIcon(warningIcon);
    myMessagePane.setSizeDelegate(new SizeDelegate.Fixed().setMinHeight(10).setMinWidth(10).setPrefWidth(230));
    myMessage.setBorder(null);
    myMessage.setOpaque(false);
    myMessagePane.getViewport().setOpaque(false);
    myMessagePane.setOpaque(false);
    myMessagePane.setBorder(null);
  }

  public static CleanDBForm showDialog(DialogManager manager, String problem) {
    final CleanDBForm form = new CleanDBForm();
    form.myMessage.setText(problem);
    final DialogBuilder builder = manager.createBuilder("cleanDBConfirmation");
    builder.setTitle(Local.text(Terms.key_Deskzilla));
    builder.setContent(form.myWholePanel);
    builder.addAction(new SimpleAction("&Continue") {
      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
      }

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        form.myErase = true;
        builder.closeWindow();
      }
    });
    builder.setCancelAction(SimpleAction.createDoNothing("E&xit Application"));
    builder.setModal(true);
    builder.setIgnoreStoredSize(true);
    builder.showWindow();
    return form;
  }

  public boolean isErase() {
    return myErase;
  }

  public boolean isBackup() {
    return myBackup.isSelected();
  }
}
