package com.almworks.explorer;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.WindowController;
import com.almworks.api.inquiry.*;
import com.almworks.util.L;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAbstractAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.almworks.util.Log;

/**
 * :todoc:
 *
 * @author sereda
 */
class ModalInquiriesDisplayer implements InquiryDisplayer {
  private final DialogManager myDialogManager;

  public ModalInquiriesDisplayer(DialogManager dialogManager) {
    assert dialogManager != null;
    myDialogManager = dialogManager;
  }

  public InquiryDisplay getDisplay(InquiryLevel level, InquiryKey<?> key) {
    // we do only critical inquiries
    if (level != InquiryLevel.CRITICAL)
      return null;
    DialogBuilder builder = myDialogManager.createBuilder("inquiry." + key.getName());
    return new InquiryDisplayImpl(builder);
  }

  private static class InquiryDisplayImpl implements InquiryDisplay, InquiryHandler.Listener {
    private final DialogBuilder myBuilder;
    private final BasicScalarModel<Boolean> myResult = BasicScalarModel.create(false);

    public InquiryDisplayImpl(DialogBuilder builder) {
      myBuilder = builder;
    }

    public void setInquiryHandler(InquiryHandler<?> handler) {
      myBuilder.setContent(handler);
      myBuilder.setTitle(handler.getInquiryTitle());
      handler.setListener(this);
    }

    public void show() {
      myBuilder.setCancelAction(new AnAbstractAction(L.actionName("Ignore")) {
        public void perform(ActionContext context) throws CantPerformException {
          setResult(false);
        }
      });
      myBuilder.setModal(true);
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          try {
            myBuilder.showWindow();
          } catch (Exception e) {
            Log.error(e);
            myResult.setValue(Boolean.FALSE);
          }
        }
      });
    }

    public ScalarModel<Boolean> getResult() {
      return myResult;
    }

    public void onAnswer(boolean answerGiven) {
      setResult(answerGiven);
    }

    private void setResult(boolean result) {
      myResult.setValue(Boolean.valueOf(result));
      final WindowController window = myBuilder.getWindowContainer().getActor(WindowController.ROLE);
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          @SuppressWarnings({"ConstantConditions"}) CantPerformExceptionExplained closed = window.close();
          assert closed == null : window;
        }
      });
    }
  }
}
