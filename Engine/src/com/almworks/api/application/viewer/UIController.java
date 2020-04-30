package com.almworks.api.application.viewer;

import com.almworks.api.application.ModelMap;
import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;

/**
 * @author : Dyoma
 */
public interface UIController <C extends JComponent> {
  ComponentProperty<UIController> CONTROLLER = ComponentProperty.createProperty("controller");

  void connectUI(@NotNull Lifespan lifespan, @NotNull ModelMap model, @NotNull C component);

  UIController NUMB = new UIController() {
    public void connectUI(Lifespan lifespan, ModelMap model, JComponent component) {
    }
  };


  public class Composite implements UIController<JComponent> {
    private final Collection<UIController<?>> myControllers = Collections15.arrayList();

    public void connectUI(@NotNull Lifespan lifespan, @NotNull ModelMap model, @NotNull JComponent component) {

    }

    public static <C extends JComponent> void append(C component, UIController<? super C> controller) {
      UIController<?> existing = CONTROLLER.getClientValue(component);
      if (existing == null)
        CONTROLLER.putClientValue(component, controller);
      else {
        Composite composite;
        if (existing instanceof Composite)
          composite = (Composite) existing;
        else {
          composite = new Composite();
          composite.add(existing);
          CONTROLLER.putClientValue(component, composite);
        }
        composite.add(controller);
      }
    }

    private void add(UIController<?> controller) {
      myControllers.add(controller);
    }
  }
}
