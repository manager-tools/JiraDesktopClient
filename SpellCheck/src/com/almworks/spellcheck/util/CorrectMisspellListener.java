package com.almworks.spellcheck.util;

import com.almworks.spellcheck.TextSpellChecker;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CorrectMisspellListener extends MouseAdapter {
  private final JTextComponent myComponent;

  public CorrectMisspellListener(JTextComponent component) {
    myComponent = component;
  }

  public static void install(JTextComponent component) {
    component.addMouseListener(new CorrectMisspellListener(component));
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    processMouse(e);
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    processMouse(e);
  }

  @Override
  public void mousePressed(MouseEvent e) {
    processMouse(e);
  }

  private void processMouse(MouseEvent e) {
    if (!e.isPopupTrigger()) return;
    e.consume();
    final TextSpellChecker.Misspell misspell = getMisspell(myComponent, e.getPoint());
    if (misspell == null) return;
    JPopupMenu menu = ReplaceMisspellAction.createPopupMenu(myComponent, misspell);
    if (menu != null) menu.show(myComponent, e.getX(), e.getY());
  }

  public static TextSpellChecker.Misspell getMisspell(JTextComponent component, Point point) {
    TextSpellChecker checker = TextSpellChecker.getInstance(component);
    int offset = component.getUI().viewToModel(component, point);
    return checker.misspellAt(offset);
  }
}
