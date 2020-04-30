package com.almworks.util.ui.actions;

import com.almworks.util.ui.DialogsUtil;

import javax.swing.*;
import java.awt.*;

public class CantPerformExceptionExplained extends CantPerformException {
  private final String myMessageHeader;
  private final JComponent myActiveFrameComponent;

  public CantPerformExceptionExplained(String displayableMessage, Throwable cause, String displayableMessageHeader,
    JComponent activeFrameComponent) {

    super(displayableMessage, cause);
    myMessageHeader = displayableMessageHeader;
    myActiveFrameComponent = activeFrameComponent;
  }

  public CantPerformExceptionExplained(String displayableMessage, Throwable cause, String messageHeader) {
    this(displayableMessage, cause, messageHeader, null);
  }

  public CantPerformExceptionExplained(String displayableMessage, String messageHeader) {
    this(displayableMessage, null, messageHeader, null);
  }

  public CantPerformExceptionExplained(String displayableMessage) {
    this(displayableMessage, null, null, null);
  }

  public String getMessageHeader() {
    return myMessageHeader;
  }

  public final void explain(String actionName, ActionContext context) {
    Component parentComponent = myActiveFrameComponent != null ? myActiveFrameComponent : context.getComponent();
    explainImpl(parentComponent, actionName);
  }

  protected void explainImpl(Component parentComponent, String actionName) {
    String title = myMessageHeader != null ? myMessageHeader : actionName;
    DialogsUtil.showErrorMessage(parentComponent, getMessage(), title);
  }
}
