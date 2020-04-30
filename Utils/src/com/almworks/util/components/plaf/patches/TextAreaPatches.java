package com.almworks.util.components.plaf.patches;

import com.almworks.util.components.plaf.LAFExtension;

import javax.swing.*;
import java.awt.*;

public class TextAreaPatches extends LAFExtension {
  public void install(LookAndFeel laf) {
    Font font = defaults().getFont("TextArea.font");
    Font editorFont = defaults().getFont("EditorPane.font");
    if (font == null || editorFont == null) {
      return;
    }
    if (!font.equals(editorFont)) {
      defaults().put("TextArea.font", editorFont);
    }
  }
}
