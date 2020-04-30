package com.almworks.screenshot.editor.image;

import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Stalex
 */
public class ImageToolbarBuilder {
  private List<ImageTool.ToolButton> myToolList = Collections15.arrayList();

  public ImageToolbarBuilder() {
  }

  public void addTool(ImageTool.ToolButton toolButton) {
    myToolList.add(toolButton);
  }

  public JPanel createBar() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    for (ImageTool.ToolButton toolButton : myToolList) {
      JPanel p = new JPanel(new BorderLayout(0, 0));
      p.add(toolButton, BorderLayout.CENTER);
      toolButton.setAlignmentX(Component.LEFT_ALIGNMENT);
      toolButton.setHorizontalAlignment(SwingConstants.LEFT);
      panel.add(p);
    }

//    return SingleChildLayout.envelop(panel, SingleChildLayout.PREFERRED, SingleChildLayout.PREFERRED,
//      SingleChildLayout.CONTAINER, SingleChildLayout.CONTAINER, 0F, 0F);
    return panel;
  }
}
