package com.almworks.util.components;

import com.almworks.util.Getter;
import com.almworks.util.collections.Containers;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class HighlighterTreeElement implements Comparable<HighlighterTreeElement> {
  private final Getter<TreeNode> myRootGetter;
  private final Color myColor;
  private final Color myCaptionBackground;
  private final Color myCaptionForeground;
  private final float myFillAlpha;
  private final Icon myIcon;
  private final String myCaption;
  private final Font myCaptionFont;

  private BufferedImage myCachedCaptionImage;
  private int myCachedTreeStartRow = -1;
  private TreeNode myCachedTreeNode = null;

  public HighlighterTreeElement(Getter<TreeNode> rootGetter, Color color, float fillAlpha, Icon icon,
    String caption, Color captionBackground, Color captionForeground, Font captionFont)
  {
    assert rootGetter != null;
    assert color != null;
    assert fillAlpha > 0F;
    assert fillAlpha < 0.9F;
    myCaption = caption;
    myCaptionBackground = captionBackground;
    myCaptionFont = captionFont;
    myCaptionForeground = captionForeground;
    myColor = color;
    myFillAlpha = fillAlpha;
    myIcon = icon;
    myRootGetter = rootGetter;
  }

  public String getCaption() {
    return myCaption;
  }

  public Color getCaptionBackground() {
    return myCaptionBackground;
  }

  public Font getCaptionFont() {
    return myCaptionFont;
  }

  public Color getCaptionForeground() {
    return myCaptionForeground;
  }

  public Color getColor() {
    return myColor;
  }

  public float getFillAlpha() {
    return myFillAlpha;
  }

  public Icon getIcon() {
    return myIcon;
  }

  public Getter<TreeNode> getRootGetter() {
    return myRootGetter;
  }

  public BufferedImage getCachedCaptionImage(JTree tree, Graphics componentGraphics) {
    if (myCaption == null)
      return null;
    if (myCachedCaptionImage == null) {
      Font f = myCaptionFont;
      if (f == null)
        f = tree.getFont();
      FontMetrics fontMetrics = tree.getFontMetrics(f);
      Rectangle2D stringBounds = fontMetrics.getStringBounds(myCaption, componentGraphics);
      LineMetrics lineMetrics = fontMetrics.getLineMetrics(myCaption, componentGraphics);
      int captH = (int)Math.ceil(lineMetrics.getAscent() + lineMetrics.getDescent());
      int captW = (int)Math.ceil(stringBounds.getWidth());
      BufferedImage image = new BufferedImage(captH, captW, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = (Graphics2D) image.getGraphics();
      try {
        g.setColor(tree.getBackground());
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.translate((int) lineMetrics.getDescent(), 0);
        g.transform(AffineTransform.getRotateInstance(Math.PI / 2));
        AwtUtil.applyRenderingHints(g);
        g.setColor(myCaptionForeground == null ? tree.getForeground() : myCaptionForeground);
        g.setFont(f);
        g.drawString(myCaption, 0, 0);
      } finally {
        g.dispose();
      }

      myCachedCaptionImage = image;
    }
    return myCachedCaptionImage;
  }

  public int compareTo(HighlighterTreeElement o) {
    return Containers.compareInts(myCachedTreeStartRow,  o.myCachedTreeStartRow);
  }

  boolean setCachedRow(int startRow) {
    if (startRow != myCachedTreeStartRow) {
      myCachedTreeStartRow = startRow;
      return true;
    } else {
      return false;
    }
  }

  void setCachedNode(TreeNode node) {
    myCachedTreeNode = node;
  }

  int getCachedRow() {
    return myCachedTreeStartRow;
  }

  TreeNode getCachedNode() {
    return myCachedTreeNode;
  }

  public static HighlighterTreeElement simple(Getter<TreeNode> nodeGetter, Color color, Icon icon, String caption) {
    Font font = UIManager.getFont("Tree.font");
    assert font != null;
    font = font.deriveFont((float)(font.getSize() - 2));
    HashMap attributes = new HashMap();
    attributes.put(TextAttribute.WIDTH, TextAttribute.WIDTH_EXTENDED);
    font = font.deriveFont(attributes);
    return new HighlighterTreeElement(nodeGetter, color, 0.1F, icon, caption, null, null, font);
  }
}
