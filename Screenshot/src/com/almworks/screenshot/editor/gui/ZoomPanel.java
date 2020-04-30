package com.almworks.screenshot.editor.gui;

import com.almworks.screenshot.editor.image.ImageDesk;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.util.text.NameMnemonic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * @author Stalex
 */
public class ZoomPanel implements ActionListener {

  private final JComboBox myZoomBox = new JComboBox();
  private final JComponent myCanvas;
  private final JPanel myPanel;
  private final WorkingImage myImage;
  private boolean myLocked = false;

  public ZoomPanel(WorkingImage image, ImageDesk desk) {
    myImage = image;
    myCanvas = desk.getComponent();
    myPanel = new JPanel(new BorderLayout(5, 0));
    myPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    myPanel.add(myZoomBox, BorderLayout.CENTER);
    JLabel label = new JLabel();
    NameMnemonic.parseString("&Zoom:").setToLabel(label);
    label.setLabelFor(myZoomBox);
    myPanel.add(label, BorderLayout.LINE_START);

    for (float scale : ImageDesk.PREDEFFINED_SCALES) {
      myZoomBox.addItem(new ZoomItem(scale));
    }

    myZoomBox.addActionListener(this);
//    myZoomBox.setToolTipText("Hint: use CTRL + wheel");

    myCanvas.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        actionPerformed(null);
      }
    });
  }

  public JComponent getComponent() {
    return myPanel;
  }

  public void actionPerformed(ActionEvent e) {
    if (!myLocked) {
      Object item = myZoomBox.getSelectedItem();
      myLocked = true;
      myImage.setScale(((ZoomItem) item).getMyScale());
    } else {
      myLocked = false;
    }
  }

  public void setCustomScale(double newValue) {

    if (!myLocked) {
      myLocked = true;
      myZoomBox.setSelectedItem(new ZoomItem((float) newValue));
    } else {
      myLocked = false;
    }
  }


  private class ZoomItem implements Comparable<ZoomItem> {
    @Override
    public boolean equals(Object obj) {

      return obj instanceof ZoomItem && Math.abs(myScale - ((ZoomItem) obj).myScale) < 0.1;
    }

    public String getMyCaption() {
      return myCaption;
    }

    public String toString() {
      return myCaption;
    }

    public float getMyScale() {
      return myScale;
    }

    private float myScale;
    private String myCaption;

    private ZoomItem(float myScale) {
      myCaption = Integer.toString(Math.round(myScale * 100)) + "%";
      this.myScale = myScale;
    }

    public int compareTo(ZoomItem o) {
      return Float.compare(myScale, o.myScale);
    }
  }
}
