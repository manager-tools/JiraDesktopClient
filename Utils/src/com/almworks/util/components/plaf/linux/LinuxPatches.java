package com.almworks.util.components.plaf.linux;

import com.almworks.util.ui.ComponentProperty;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;

public class LinuxPatches {
  private static final ComponentProperty<TextComponentMenuCopied> LAF_UNIX_JTEXT_COMPONENT_MENU = ComponentProperty.createProperty("LAF.unix.JTextComponent.menu");
  private static final ComponentProperty<Boolean> NO_TEXT_MENUS = ComponentProperty.createProperty("LAF.unix.JTextComponent.noMenu");

  public static void installTextMenus() {
    Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
      public void eventDispatched(AWTEvent event) {
        int id = event.getID();
        if (id == ContainerEvent.COMPONENT_ADDED && event instanceof ContainerEvent) {
          Component child = ((ContainerEvent) event).getChild();
          if (child instanceof JTextComponent && !(child instanceof JPasswordField)) {
            JTextComponent c = (JTextComponent) child;
            if (Boolean.TRUE.equals(NO_TEXT_MENUS.getClientValue(c))) return;
            TextComponentMenuCopied menu = new TextComponentMenuCopied(c);
            LAF_UNIX_JTEXT_COMPONENT_MENU.putClientValue(c, menu);
          }
        } else if (id == ContainerEvent.COMPONENT_REMOVED && event instanceof ContainerEvent) {
          Component child = ((ContainerEvent) event).getChild();
          if (child instanceof JTextComponent) {
            JTextComponent c = (JTextComponent) child;
            uninstall(c);
          }
        }
      }
    }, AWTEvent.CONTAINER_EVENT_MASK);
  }

  public static void removeTextMenus(JTextComponent component) {
    NO_TEXT_MENUS.putClientValue(component, true);
    uninstall(component);
  }

  private static void uninstall(JTextComponent c) {
    TextComponentMenuCopied menu = LAF_UNIX_JTEXT_COMPONENT_MENU.getClientValue(c);
    if (menu != null) menu.uninstall();
  }
}
