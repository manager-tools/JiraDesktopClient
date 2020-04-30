package com.almworks.util.components.speedsearch;

import com.almworks.util.LogHelper;
import com.almworks.util.components.Highlightable;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.Shortcuts;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class TextSpeedSearch implements SpeedSearchPopup.Controller {
  private static final String ACTION_KEY = "openSpeedSearch";
  private final JTextComponent myComponent;
  private final Highlighter mySearchHighlighter = new DefaultHighlighter();
  private Highlighter myOldHighlighter;
  private final int mySelectionStart;
  private final int mySelectionEnd;

  public TextSpeedSearch(JTextComponent component) {
    myComponent = component;
    myOldHighlighter = component.getHighlighter();
    mySelectionStart = component.getSelectionStart();
    mySelectionEnd = component.getSelectionEnd();
    component.setHighlighter(mySearchHighlighter);
  }

  public static void show(JTextComponent component) {
    TextSpeedSearch controller = new TextSpeedSearch(component);
    SpeedSearchPopup.open(component, controller);
  }

  public static void install(final JTextComponent component, KeyStroke stroke) {
    component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, ACTION_KEY);
    component.getActionMap().put(ACTION_KEY, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        show(component);
      }
    });
  }

  public static void installCtrlF(JTextComponent component) {
    install(component, Shortcuts.ksMenu(KeyEvent.VK_F));
  }

  @Override
  public boolean maybeStopSearch(SpeedSearchPopup popup, KeyEvent e) {
    int code = e.getKeyCode();
    if (code == KeyEvent.VK_ESCAPE) {
      restoreState();
      popup.hidePopup(true);
      return true;
    }
    if (code == KeyEvent.VK_ENTER) {
      popup.hidePopup(true);
      return true;
    }
    return false;
  }

  private void restoreState() {
    myComponent.setSelectionStart(mySelectionStart);
    myComponent.setSelectionEnd(mySelectionEnd);
  }

  @Override
  public void speedSearchClosed() {
    int start = myComponent.getSelectionStart();
    int end = myComponent.getSelectionEnd();
    myComponent.setHighlighter(myOldHighlighter);
    myComponent.select(start, end);
  }

  @Override
  public boolean searchText(String pattern, int direction) {
    mySearchHighlighter.removeAllHighlights();
    if (pattern.length() == 0) return false;
    int startFrom;
    if (direction == 0) startFrom = myComponent.getSelectionStart();
    else if (direction > 0) startFrom = myComponent.getSelectionEnd();
    else startFrom = myComponent.getSelectionStart();
    String wholeText = UIUtil.getDocumentText(myComponent);
    int index = 0;
    int nextIndex = -1;
    Object currentHighlight = null;
    while (index < wholeText.length() && (index = wholeText.indexOf(pattern, index)) >= 0) {
      try {
        Object highlight = mySearchHighlighter.addHighlight(index, index + pattern.length(), Highlightable.HIGHLIGH_PAINTER);
        if (direction == 0) {
          if (nextIndex < 0 || nextIndex < startFrom) {
            nextIndex = index;
            currentHighlight = highlight;
          }
        } else if (direction < 0) {
          // Take any, but prefer (max less than startFrom) or (last match if no less than start from)
          if (nextIndex < 0 || (nextIndex < index && index < startFrom) || (startFrom <= nextIndex && nextIndex < index)) {
            nextIndex = index;
            currentHighlight = highlight;
          }
        } else {
          // Take any, but prefer (min greater that startFrom)
          if (nextIndex < 0 || (index >= startFrom && (nextIndex > index || nextIndex < startFrom))) {
            nextIndex = index;
            currentHighlight = highlight;
          }
        }
      } catch (BadLocationException e) {
        LogHelper.debug(e);
      }
      index++;
    }
    if (nextIndex < 0) return false;
    if (currentHighlight != null) {
      mySearchHighlighter.removeHighlight(currentHighlight);
      try {
        mySearchHighlighter.addHighlight(nextIndex, nextIndex + pattern.length(), Highlightable.SELECTION_PAINTER);
      } catch (BadLocationException e) {
        LogHelper.debug(e);
      }
    }
    myComponent.select(nextIndex, nextIndex + pattern.length());
    UIUtil.scrollSelectionToView(myComponent);
    return true;
  }
}
