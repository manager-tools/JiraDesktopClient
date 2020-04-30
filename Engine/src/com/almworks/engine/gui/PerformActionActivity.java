package com.almworks.engine.gui;

import com.almworks.util.components.RendererActivityController;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.*;

import java.awt.*;

public class PerformActionActivity implements RendererActivity {
  private final AnActionListener myAction;
  private final String myTooltip;

  public PerformActionActivity(AnActionListener action, String tooltip) {
    myAction = action;
    myTooltip = tooltip;
  }

  public void apply(RendererActivityController controller, Rectangle rectangle) {
    ActionContext context = new DefaultActionContext(controller.getWholeComponent());
//      context = context.childContext(new ConstProvider().addData(RendererContext.RENDERER_CONTEXT, myContext));
    CantPerformExceptionExplained failure = ActionUtil.performSafe(myAction, context);
    if (failure != null)
      failure.explain(myTooltip, context);
  }

  public <T> void storeValue(ComponentProperty<T> key, T value) {
  }

  public String getTooltip() {
    return myTooltip;
  }

  public AnActionListener getAction() {
    return myAction;
  }
}
