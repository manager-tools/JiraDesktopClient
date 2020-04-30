package com.almworks.util.ui;

import com.almworks.util.threads.Threads;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public class LoadingComponentWrapper implements UIComponentWrapper {
  private static final String INITIAL = "initial";
  private static final String ACTUAL = "actual";

  private final JPanel myWholePanel;
  private final JPanel myContentPanel;
  private UIComponentWrapper myContent = null;
  private boolean myDisposed = false;

  public LoadingComponentWrapper(JPanel wholePanel, JPanel contentPanel, JComponent initialContent) {
    myWholePanel = wholePanel;
    myContentPanel = contentPanel;
    myContentPanel.setLayout(new CardLayout());
    myContentPanel.add(initialContent, INITIAL);
    showComponent(INITIAL);
  }

  private void showComponent(String name) {
    ((CardLayout) myContentPanel.getLayout()).show(myContentPanel, name);
  }

  public void setContent(UIComponentWrapper content) {
    Threads.assertAWTThread();
    if (myDisposed && content != null) {
      content.getComponent();
      content.dispose();
      return;
    }
    if (myContent == content)
      return;
    disposeActualContent();
    myContent = content;
    if (myContent != null) {
      myContentPanel.add(myContent.getComponent(), ACTUAL);
      showComponent(ACTUAL);
    } else
      showComponent(INITIAL);
  }

  private void disposeActualContent() {
    if (myContent != null) {
      myContentPanel.remove(myContent.getComponent());
      myContent.dispose();
      myContent = null;
    }
  }

  public JComponent getComponent() {
    assert !myDisposed;
    return myWholePanel;
  }

  public void dispose() {
    disposeActualContent();
    myDisposed = true;
  }
}
