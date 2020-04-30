package com.almworks.util;

import org.almworks.util.Log;

import javax.swing.*;
import javax.swing.text.EditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyledEditorKit;
import java.awt.*;

public class ErrorHunt {
  public static void setEditorPaneText(JEditorPane pane, String text) {
    if (!EventQueue.isDispatchThread()) {
      Log.error("NDP JEditorPane!", new RuntimeException());
    }
    try {
      EditorKit kit = pane.getEditorKit();
      if (kit instanceof StyledEditorKit) {
        StyledEditorKit styledKit = (StyledEditorKit) kit;
        MutableAttributeSet inputAttrs = styledKit.getInputAttributes();
        inputAttrs.removeAttributes(inputAttrs);
      }
      pane.setText(text);
    } catch (Exception e) {
      Log.error(e);
    }
  }
}
