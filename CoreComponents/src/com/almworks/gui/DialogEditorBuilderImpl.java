package com.almworks.gui;

import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.api.gui.WindowController;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.DialogEditor;
import com.almworks.util.ui.WindowUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.detach.Detach;
import org.picocontainer.Startable;
import util.concurrent.Synchronized;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
class DialogEditorBuilderImpl implements DialogEditorBuilder {
  private static final Role<Startable> RESET_ON_START = Role.role("resetOnStart");
  private final DialogBuilder myDialog;
  private boolean myWasApplied = false;
  private final FireEventSupport<Procedure<EditingEvent>> myStateChangeProcedures =
    (FireEventSupport) FireEventSupport.createSynchronized(Procedure.class);
  private boolean myApplyButtonIsVisible = true;
  private final Synchronized<EditingEvent> myLastEvent = new Synchronized(null);

  public DialogEditorBuilderImpl(DialogBuilder dialog) {
    myDialog = dialog;
    myDialog.setEmptyCancelAction();
    myDialog.addCancelListener(new AnActionListener() {
      public void perform(ActionContext context) {
        myStateChangeProcedures.getDispatcher().invoke(CANCEL_EVENT);
      }
    });
    addStateListener(new Procedure<EditingEvent>() {
      public void invoke(EditingEvent arg) {
        myLastEvent.set(arg);
      }
    });
  }

  public void setTitle(String title) {
    myDialog.setTitle(title);
  }

  public void setInitialFocusOwner(Component component) {
    myDialog.setInitialFocusOwner(component);
  }

  public Component getInitialFocusOwner() {
    return myDialog.getInitialFocusOwner();
  }

  public void detachOnDispose(Detach detach) {
    myDialog.detachOnDispose(detach);
  }

  public void addProvider(DataProvider provider) {
    myDialog.addProvider(provider);
  }

  public WindowController showWindow() {
    return showWindow(Detach.NOTHING);
  }

  public void hideApplyButton() {
    myApplyButtonIsVisible = false;
  }

  public void setBottomLineComponent(JComponent component) {
    myDialog.setBottomLineComponent(component);
  }

  @Override
  public void addGlobalDataRoot() {
    myDialog.addGlobalDataRoot();
  }

  public WindowController showWindow(Detach disposeNotification) {
    myDialog.setOkAction(new ApplySettings("OK", false, getContentRole(), OK_EVENT));
    if (myApplyButtonIsVisible)
      myDialog.addAction(new ApplySettings("Apply", true, getContentRole(), APPLY_EVENT));
    final MutableComponentContainer container = getWindowContainer();
    assert container != null;
    container.registerActor(RESET_ON_START, new Startable() {
      public void start() {
        DialogEditor actor = container.getActor(getContentRole());
        assert actor != null;
        actor.reset();
      }

      public void stop() {
      }
    });
    return myDialog.showWindow(disposeNotification);
  }

  public void setPreferredSize(Dimension size) {
    myDialog.setPreferredSize(size);
  }

  public void setIgnoreStoredSize(boolean ignore) {
    myDialog.setIgnoreStoredSize(ignore);
  }

  public MutableComponentContainer getWindowContainer() {
    return myDialog.getWindowContainer();
  }

  public Configuration getConfiguration() {
    return myDialog.getConfiguration();
  }

  public void setContent(DialogEditor content) {
    myDialog.setContent(content);
  }

  public void setContent(JComponent content) {
    setContent(new DialogEditor.SimpleEditor(content));
  }

  public void setCloseConfirmation(AnActionListener confirmation) {
    myDialog.setCloseConfirmation(confirmation);
  }

  public void setContentClass(Class<? extends DialogEditor> contentClass) {
    myDialog.setContentClass(contentClass);
  }

  public Role<? extends DialogEditor> getContentRole() {
    return (Role<? extends DialogEditor>) myDialog.getContentRole();
  }

  public boolean isChangesApplied() {
    return myWasApplied;
  }

  public void addStateListener(Procedure<EditingEvent> closeProcedure) {
    myStateChangeProcedures.addStraightListener(closeProcedure);
  }

  public void setModal(boolean modal) {
    myDialog.setModal(modal);
  }

  public EditingEvent getLastEvent() {
    return myLastEvent.get();
  }

  public void pressOk() {
    myDialog.pressOk();
  }

  @Override
  public void setWindowPositioner(WindowUtil.WindowPositioner adjuster) {
    myDialog.setWindowPositioner(adjuster);
  }

  private class ApplySettings extends AnAbstractAction {
    private final Role<? extends DialogEditor> myEditorRole;
    private final boolean myModifies;
    private final EditingEvent myEditingEvent;

    public ApplySettings(String name, boolean modifiable, Role<? extends DialogEditor> role, EditingEvent editingEvent) {
      super(name);
      myModifies = modifiable;
      myEditorRole = role;
      myEditingEvent = editingEvent;
      setDefaultPresentation(PresentationKey.ENABLE, myModifies ? EnableState.DISABLED : EnableState.ENABLED);
    }

    public void perform(ActionContext context) throws CantPerformException {
      DialogEditor editor = context.getSourceObject(myEditorRole);
      if (editor.isModified()) {
        editor.apply();
        myWasApplied = true;
      }
      myStateChangeProcedures.getDispatcher().invoke(myEditingEvent);
    }

    public void update(UpdateContext context) throws CantPerformException {
      super.update(context);
      if (myModifies) {
        try {
          context.watchRole(myEditorRole);
          DialogEditor editor = context.getSourceObject(myEditorRole);
          context.updateOnChange(editor.getModifiable());
        } catch (CantPerformException e) {
          // ignore
        }
      }
      DialogEditor editor = context.getSourceObject(myEditorRole);
      boolean enabled = !myModifies || editor.isModified();
      context.setEnabled(enabled ? EnableState.ENABLED : EnableState.DISABLED);
    }
  }

  @Override
  public void setActionScope(String scope) {
    myDialog.setActionScope(scope);
  }

  @Override
  public void setDefaultButton(JButton button) {
    assert false : "Default button is always OK button";
  }

  @Override
  public boolean isModal() {
    return myDialog.isModal();
  }
}
