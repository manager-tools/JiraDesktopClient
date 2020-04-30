package com.almworks.util.i18n.text.util;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Procedure;
import com.almworks.util.ui.AActionComponent;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.swing.SwingTreeUtil;
import com.almworks.util.ui.swing.TreeElementVisitor;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;

/**
 * Performs update of all displayable components on change.<br>
 * Instance of this class should be added to listen to {@link com.almworks.util.i18n.text.CurrentLocale#addAWTListener(org.almworks.util.detach.Lifespan, com.almworks.util.collections.ChangeListener) local changes}<br>
 * Supported updates:<br>
 * 1. Force update of all {@link AActionComponent action components}<br>
 * 2. Invoke update procedure attached to components via {@link #UPDATE_LOCALE} component property.
 */
public class LocalizedUIUpdate implements ChangeListener {
  /**
   * Points to the procedure to be invoked when locale changes. The procedure should update component text (and other locale-dependent properties)<br>
   * Use instance of class derived from {@link Visitor base component visitor} - it iterates subtree but stops if another updater found, assuming that any updater is responsible for
   * marked component and it's subtree.
   */
  public static final ComponentProperty<Procedure<JComponent>> UPDATE_LOCALE = ComponentProperty.createProperty("updateLocale");

  /**
   * Update procedure for components which gets localized resources on every paint.<br>
   * Requests re validation and repaint.
   */
  public static final Procedure<JComponent> REVALIDATE = new Procedure<JComponent>() {
    @Override
    public void invoke(JComponent component) {
      component.invalidate();
      component.revalidate();
      component.repaint();
    }
  };

  private static final TreeElementVisitor<Component> UPDATER = new TreeElementVisitor<Component>() {
    @Override
    public Result visit(Component component) {
      AActionComponent actionComponent = Util.castNullable(AActionComponent.class, component);
      if (actionComponent != null)
        actionComponent.updateNow();
      JComponent jComponent = Util.castNullable(JComponent.class, component);
      Procedure<JComponent> listener = jComponent != null ? UPDATE_LOCALE.getClientValue(jComponent) : null;
      if (listener != null)
        listener.invoke(jComponent);
      return Result.GO_ON;
    }
  };

  @Override
  public void onChange() {
    Window[] windows = Window.getWindows();
    for (Window window : windows) {
      RootPaneContainer container = Util.castNullable(RootPaneContainer.class, window);
      if (container != null) {
        JRootPane rootPane = container.getRootPane();
        SwingTreeUtil.iterateDescendants(rootPane, UPDATER);
      }
    }
  }

  public static abstract class Visitor implements TreeElementVisitor<Component>, Procedure<JComponent> {
    @Override
    public final void invoke(JComponent root) {
      updateComponent(root);
      SwingTreeUtil.iterateDescendants(root, this);
    }

    @Override
    public final Result visit(Component component) {
      JComponent jComponent = Util.castNullable(JComponent.class, component);
      if (jComponent != null) {
        Procedure<JComponent> procedure = UPDATE_LOCALE.getClientValue(jComponent);
        if (procedure != null) return Result.SKIP_SUBTREE;
      }
      updateComponent(component);
      return Result.GO_ON;
    }

    protected abstract void updateComponent(Component component);
  }
}
