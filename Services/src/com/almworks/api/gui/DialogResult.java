package com.almworks.api.gui;

import com.almworks.api.container.MutableComponentContainer;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.*;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class DialogResult<R> {
  private final DialogBuilder myBuilder;
  private R myResult = null;

  public DialogResult(DialogBuilder builder) {
    myBuilder = builder;
  }

  public DialogResult<R> setOkResult(R result) {
    myBuilder.setEmptyOkAction();
    myBuilder.addOkListener(new CloseListener(result));
    return this;
  }

  public DialogResult<R> setOkResult(R result, AnAction action) {
    myBuilder.setOkAction(action);
    myBuilder.addOkListener(new CloseListener(result));
    return this;
  }

  public DialogResult<R> setCancelResult(R result) {
    myBuilder.setEmptyCancelAction();
    myBuilder.addCancelListener(new CloseListener(result));
    return this;
  }

  public DialogResult<R> setBottomLineComponent(JComponent component) {
    myBuilder.setBottomLineComponent(component);
    return this;
  }

  public DialogResult<R> setBottomBevel(boolean bottomBevel) {
    myBuilder.setBottomBevel(bottomBevel);
    return this;
  }

  public DialogResult<R> setInitialFocusOwner(Component c) {
    myBuilder.setInitialFocusOwner(c);
    return this;
  }

  public R showModal(String title, JComponent component) {
    myBuilder.setTitle(title);
    myBuilder.setContent(component);
    return showModal();
  }

  public R showModal() {
    myBuilder.setModal(true);
    myBuilder.showWindow();
    return myResult;
  }

  public void show(String title, JPanel component, final Procedure<R> result) {
    myBuilder.setTitle(title);
    myBuilder.setContent(component);
    myBuilder.setModal(false);
    myBuilder.showWindow(new Detach() {
      protected void doDetach() throws Exception {
        // straight execution would lead to empty result
        // because myResult may not be set yet
        ThreadGate.AWT_QUEUED.execute(new Runnable() {
          public void run() {
            if (result != null) {
              result.invoke(myResult);
            }
          }
        });
      }
    });
  }

  public static <R> DialogResult<R> create(ActionContext context, String windowId) throws CantPerformException {
    return new DialogResult<R>(context.getSourceObject(DialogManager.ROLE).createBuilder(windowId));
  }

  public Configuration getConfig() {
    return myBuilder.getConfiguration();
  }

  public void toFront() {
    MutableComponentContainer container = myBuilder.getWindowContainer();
    if (container != null) {
      WindowController controller = container.getActor(WindowController.ROLE);
      if (controller != null) {
        controller.toFront();
      }
    }
  }

  public void pack() {
    myBuilder.setIgnoreStoredSize(true);
  }

  /**
   * Configures simple Yes/No question dialog.<br>
   * It contains two buttons with Yes and No texts. The result of is of type Boolean. true corresponds to yes, and false - to no.
   * null results mean the user closed the dialog without pressing any button.
   * @param yes button text for "Yes" (true) result
   * @param no button text for "No" (false) result
   */
  public static void configureYesNo(DialogResult<Boolean> dr, String yes, String no) {
    dr.setOkResult(true);
    dr.setCancelResult(false);
    dr.myBuilder.setOkAction(SimpleAction.createDoNothing(yes));
    dr.myBuilder.setCancelAction(no);
  }

  private class CloseListener implements AnActionListener{
    private final R myCloseResult;


    public CloseListener(R closeResult) {
      myCloseResult = closeResult;
    }

    public void perform(ActionContext context) throws CantPerformException {
      myResult = myCloseResult;
    }
  }
}
