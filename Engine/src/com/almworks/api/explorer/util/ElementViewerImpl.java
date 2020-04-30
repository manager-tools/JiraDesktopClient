package com.almworks.api.explorer.util;

import com.almworks.api.application.ItemUiModel;
import com.almworks.api.application.viewer.DefaultUIController;
import com.almworks.engine.gui.CommonIssueViewer;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.ui.ElementViewer;
import com.almworks.util.ui.UIComponentWrapper;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author dyoma
 */
public class ElementViewerImpl implements ElementViewer<ItemUiModel> {
  private final UIComponentWrapper myComponent;
  @Nullable
  private final ScalarModel<? extends JComponent> myToolbarActionsHolder;
  private final Lifecycle myLife = new Lifecycle();

  public ElementViewerImpl(UIComponentWrapper component, @Nullable ScalarModel<? extends JComponent> toolbarActionsHolder) {
    myComponent = component;
    myToolbarActionsHolder = toolbarActionsHolder;
  }

  public ElementViewerImpl(JComponent component) {
    this(new UIComponentWrapper.Simple(component), null);
  }

  public void showElement(ItemUiModel item) {
    myLife.cycle();
    DefaultUIController.connectComponent(myLife.lifespan(), item.getModelMap(), myComponent.getComponent());
  }

  public JComponent getToolbarEastComponent() {
    return null;
  }

  @Nullable
  public ScalarModel<? extends JComponent> getToolbarActionsHolder() {
    return myToolbarActionsHolder;
  }

  public JComponent getComponent() {
    return myComponent.getComponent();
  }

  public PlaceHolder getToolbarPlace() {
    if(myComponent instanceof CommonIssueViewer) {
      return ((CommonIssueViewer)myComponent).getToolbarPlace();
    }
    return null;
  }

  public PlaceHolder getBottomPlace() {
    if (myComponent instanceof CommonIssueViewer) return ((CommonIssueViewer) myComponent).getBottomPlace();
    else return null;
  }

  public void dispose() {
    myLife.dispose();
    myComponent.dispose();
  }
}
