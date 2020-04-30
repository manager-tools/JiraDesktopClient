package com.almworks.util.ui.actions;

import org.almworks.util.Collections15;

import java.util.List;
import java.util.Map;


/**
 * @author : Dyoma
 */
public interface AnAction extends AnActionListener {
  List<AnAction> EMPTY_LIST = Collections15.emptyList(); // todo inline?
  AnAction[] EMPTY_ARRAY = new AnAction[0];

  AnAction DEAF = new ConstEnabledAction(EnableState.ENABLED);
  AnAction DISABLED = new ConstEnabledAction(EnableState.DISABLED);
  AnAction INVISIBLE = new ConstEnabledAction(EnableState.INVISIBLE);

  void update(UpdateContext context) throws CantPerformException;

  public static class ConstEnabledAction implements AnAction {
    private final EnableState myEnable;

    public ConstEnabledAction(EnableState enable) {
      myEnable = enable;
    }

    public void update(UpdateContext context) throws CantPerformException {
      context.setEnabled(myEnable);
    }

    public void perform(ActionContext context) throws CantPerformException {
    }
  }

  class OverridePresentation implements AnAction {
    private final AnAction myAction;
    private final Map<PresentationKey<?>, Object> myPresentation = Collections15.hashMap();

    public OverridePresentation(AnAction action) {
      myAction = action;
    }

    @Override
    public void update(UpdateContext context) throws CantPerformException {
      CantPerformException exception = null;
      try {
        myAction.update(context);
      } catch (CantPerformException e) {
        exception = e;
      }
      for (Map.Entry<PresentationKey<?>, Object> entry : myPresentation.entrySet()) {
        context.putPresentationProperty((PresentationKey<Object>) entry.getKey(), entry.getValue());
      }
      if (exception != null) throw exception;
    }

    @Override
    public void perform(ActionContext context) throws CantPerformException {
      myAction.perform(context);
    }

    public <T> void overridePresentation(PresentationKey<T> key, T value) {
      key.putTo(myPresentation, value);
    }
  }
}
