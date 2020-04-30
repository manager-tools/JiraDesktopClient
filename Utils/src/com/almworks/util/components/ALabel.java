package com.almworks.util.components;

import com.almworks.util.debug.DebugFrame;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Failure;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ALabel extends JLabel implements Antialiasable, SizeDelegating, WidthFriendly {
  private SizeDelegate mySizeDelegate = null;
  private boolean myAntialiased = false;
  private int myPreferredWidth;

  public ALabel(String text, Icon icon, int horizontalAlignment) {
    super(text, icon, horizontalAlignment);
  }

  public ALabel(String text, int horizontalAlignment) {
    super(text, horizontalAlignment);
  }

  public ALabel(Icon image, int horizontalAlignment) {
    super(image, horizontalAlignment);
  }

  public ALabel(String text) {
    super(text);
  }

  public ALabel(Icon image) {
    super(image);
  }

  public ALabel() {
    super();
  }

  public Dimension getMaximumSize() {
    return SizeDelegate.maximum(this, mySizeDelegate, super.getMaximumSize());
  }

  public Dimension getMinimumSize() {
    return SizeDelegate.minimum(this, mySizeDelegate, super.getMinimumSize());
  }

  public Dimension getPreferredSize() {
    return SizeDelegate.preferred(this, mySizeDelegate, getDefaultPreferredSize());
  }

  private Dimension getDefaultPreferredSize() {
    // if maximum size is set, try to observe maximum width
    Dimension prefSize = super.getPreferredSize();
    if (myPreferredWidth <= 0 || (prefSize != null && prefSize.width == myPreferredWidth))
      return prefSize;
    int height = getPreferredHeightForWidth(myPreferredWidth);
    if (height <= 0)
      return prefSize;
    return new Dimension(myPreferredWidth, height);
  }

  public boolean isAntialiased() {
    return myAntialiased;
  }

  public void setAntialiased(boolean antialiased) {
    myAntialiased = antialiased;
    repaint();
  }

  public void setSizeDelegate(SizeDelegate delegate) {
    mySizeDelegate = delegate;
    revalidate();
  }

  public Dimension getSuperSize(SizeKey sizeKey) {
    if (sizeKey == SizeKey.PREFERRED)
      return getDefaultPreferredSize();
    else if (sizeKey == SizeKey.MAXIMUM)
      return super.getMaximumSize();
    else if (sizeKey == SizeKey.MINIMUM)
      return super.getMinimumSize();
    else
      throw new Failure(sizeKey.toString());
  }

  public Dimension getSize(SizeKey sizeKey) {
    return sizeKey.getFrom(this);
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    super.paintComponent(g);
  }

  public void setPreferredWidth(int width) {
    myPreferredWidth = width;
  }

  public int getPreferredWidth() {
    return myPreferredWidth;
  }

  public int getPreferredHeightForWidth(int width) {
    String text = getText();
    if (!BasicHTML.isHTMLString(text))
      return 0;

    // todo use installed views

    View view = BasicHTML.createHTMLView(this, text);
    view.setSize(width, Short.MAX_VALUE);
    float span = view.getPreferredSpan(View.Y_AXIS);
    if (span <= 0)
      return 0;

    Insets insets = getInsets();
    return insets.top + insets.bottom + (int)span + 8;
  }

  public static void main2(String[] args) {
    JPanel panel = new JPanel(new BorderLayout());
    final ALabel label = new ALabel("<html>This is a test label<br><br>");
    label.setOpaque(true);
    panel.add(label, BorderLayout.NORTH);

    JPanel black = new JPanel();
    black.setOpaque(true);
    black.setBackground(Color.BLACK);
    black.setPreferredSize(new Dimension(100, 40));
    panel.add(black, BorderLayout.CENTER);

    JPanel toolbar = new JPanel(new FlowLayout());
    JButton button = new JButton("More Text");
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        label.setText(label.getText() + " More test text! Should be wrapped!");
      }
    });
    toolbar.add(button);

    button = new JButton("Pack");
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SwingUtilities.getWindowAncestor(label).pack();
      }
    });
    toolbar.add(button);

    panel.add(toolbar, BorderLayout.SOUTH);

    label.setMaximumSize(new Dimension(350, Short.MAX_VALUE));
    DebugFrame.show(panel);
  }
}
