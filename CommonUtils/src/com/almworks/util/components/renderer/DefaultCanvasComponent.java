package com.almworks.util.components.renderer;

import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public class DefaultCanvasComponent extends BaseRendererComponent implements CanvasComponent {
  private static final EmptyBorder EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);
  private final CanvasImpl myCanvas;
  private final Map<Integer, Font> myFontCache = Collections15.hashMap();
  private CanvasElement myCurrentElement;
  private boolean myPreferedSizeCleared = false;
  private Pattern myHighlightPattern = null;

  public DefaultCanvasComponent() {
    setBorder(EMPTY_BORDER);
    myCanvas = new CanvasImpl();
    updateUI();
  }

  public JComponent getComponent() {
    return this;
  }

  public void setCurrentElement(CanvasElement element) {
    myCurrentElement = element;
  }

  public void setComponentFont(Font font) {
    setFont(font);
  }

  public Font getDerivedFont(int fontStyle) {
    Font font = myFontCache.get(fontStyle);
    if (font != null)
      return font;
    font = getFont().deriveFont(fontStyle);
    myFontCache.put(fontStyle, font);
    return font;
  }

  public void clear() {
    myHighlightPattern = null;
    if (!myPreferedSizeCleared) {
      setPreferredSize(new Dimension(0, 0));
      myPreferedSizeCleared = true;
    }
  }

  public Pattern getHighlightPattern() {
    return myHighlightPattern;
  }

  public void setPreferredSize(Dimension preferredSize) {
    super.setPreferredSize(preferredSize);
    myPreferedSizeCleared = false;
  }

  public void setFont(Font font) {
    if (!Util.equals(font, getFont()))
      myFontCache.clear();
    super.setFont(font);
  }

  public Color getBackground() {
    if (myCurrentElement != null) {
      Color bg = myCurrentElement.getAttributes().getBackground();
      if (bg != null) {
        return bg;
      }
    }
    return super.getBackground();
  }

  public void setBackground(Color bg) {
    myCanvas.setBackground(bg);
    super.setBackground(bg);
  }

  public void setForeground(Color fg) {
    myCanvas.setForeground(fg);
    super.setForeground(fg);
  }

  public Dimension getPreferredSize() {
    myCanvas.updateComponent(this);
    Dimension result = super.getPreferredSize();
    if (result.height != 0 && result.width != 0)
      return result;
    myCanvas.getSize(result, this);
    AwtUtil.addInsets(result, getInsets());
    return result;
  }

  public void updateUI() {
    super.updateUI();
    setFont(UIManager.getFont("Label.font"));
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    myCanvas.updateComponent(this);
    myCanvas.paint(g, getSize(), this);
  }

  public Border getBorder() {
    return EMPTY_BORDER;
//    return myCanvas.getAttributes().getBorder();
  }

  public CanvasImpl prepareCanvas(CellState cellState) {
    myCanvas.clear();
    myCanvas.setupProperties(cellState);
    myCanvas.updateComponent(this);
    myHighlightPattern = cellState.getHighlightPattern();
    return myCanvas;
  }

  public RootAttributes getCanvasAttributes() {
    return myCanvas.getAttributes();
  }

  public CanvasImpl getCanvas() {
    return myCanvas;
  }
}
