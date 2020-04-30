package com.almworks.screenshot.editor.layers;

import com.almworks.util.components.PlaceHolder;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

public class LayerOptionsPanel {
  private final int myWidth = UIUtil.getColumnWidth(new JLabel()) * 15;
  private final PlaceHolder myPanel = new PlaceHolder() {
    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      if (size.width != myWidth)
        return new Dimension(myWidth, size.height);
      else
        return size;
    }
  };

  private final UIComponentWrapper myNothingSelected = UIComponentWrapper.Simple.empty();

  public void show(LayerOptions options) {
    myPanel.show(options == null ? myNothingSelected : options);
    if (options != null)
      options.attach();
    Container parent = myPanel.getParent();
    if (parent != null) {
      parent.invalidate();
      if (parent instanceof JComponent)
        ((JComponent) parent).revalidate();
    }
  }

  public JComponent getComponent() {
    return myPanel;
  }
}
