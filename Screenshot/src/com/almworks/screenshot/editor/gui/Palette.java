package com.almworks.screenshot.editor.gui;

import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Stalex
 */
public class Palette {
  public static Color DEFAULT_COLOR = new Color(0xB30900);

  private ColorChangeEvent myListener;

  private final PaletteComponent myPanel = new PaletteComponent();

  public static Color[] COLORS = {
    Color.BLACK, DEFAULT_COLOR, new Color(0xFD0006), new Color(0xFFB700), new Color(0xFFFD03), new Color(0x8BD353),
    new Color(0x34955F), new Color(0x00B7E2), new Color(0x0C6BBB), new Color(0x021D6E), new Color(0x732CB0)};

  private int mySelectedColor;

  public Palette() {
    myPanel.setPreferredSize(new Dimension(200, 61));
    myPanel.setMinimumSize(new Dimension(30, 50));
    setColor(DEFAULT_COLOR);
  }


  public void setListener(ColorChangeEvent listener) {
    myListener = listener;
  }

  public PaletteComponent getComponent() {
    return myPanel;
  }

  public void setColor(Color color) {
    for (int i = 0; i < COLORS.length; i++) {
      Color myColor = COLORS[i];
      if (color.equals(myColor)) {
        if (mySelectedColor != i) {
          mySelectedColor = i;
          myPanel.repaint();
        }
        break;
      }
    }
  }

  public Color getColor() {
    return COLORS[mySelectedColor];
  }

  private class PaletteComponent extends JComponent {
    private int myColorsOnLine = 3;


    private int myMargin;
    private int myItemWidth;
    private int myItemHeight;
    private int myLinesCount = COLORS.length / myColorsOnLine;

    private PaletteComponent() {
      addComponentListener(new ComponentListener() {
        public void componentResized(ComponentEvent e) {
          Dimension size = getSize();

          myMargin = 6;

          myItemWidth = (int) (size.getWidth() / (myColorsOnLine)) - myMargin;
          myItemHeight = (int) (size.getHeight() / (myLinesCount)) - myMargin;

          repaint();
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }
      });
      addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          mouseEvent(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
          mouseEvent(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          mouseEvent(e);
        }
      });
    }

    private void mouseEvent(MouseEvent e) {
      Point p = e.getPoint();
      int x = myItemWidth + myMargin > 0 ? p.x / (myItemWidth + myMargin) : 0;
      int y = myItemHeight + myMargin > 0 ? p.y / (myItemHeight + myMargin) : 0;
      int c = y * myColorsOnLine + x;
      if (c >= 0 && c < COLORS.length) {
        mySelectedColor = c;
        repaint();
        notifyChange(COLORS[mySelectedColor]);
      }
    }

    @Override

    public void paint(Graphics g) {
      AwtUtil.applyRenderingHints(g);

      for (int i = 0; i < myLinesCount; i++) {
        int itemsOnLine = myColorsOnLine;
        for (int j = 0; j < itemsOnLine; j++) {
          g.setColor(Color.BLACK);
          g.drawRect(j * (myItemWidth + myMargin) + myMargin / 2, i * (myItemHeight + myMargin) + myMargin / 2,
            myItemWidth, myItemHeight);
          g.setColor(COLORS[i * myColorsOnLine + j]);
          g.fillRect(j * (myItemWidth + myMargin) + myMargin / 2, i * (myItemHeight + myMargin) + myMargin / 2,
            myItemWidth, myItemHeight);
        }
      }

      int y = mySelectedColor / myColorsOnLine;
      int x = mySelectedColor % myColorsOnLine;
      g.setColor(Color.BLACK);

      g.drawRect(x * (myItemWidth + myMargin), y * (myItemHeight + myMargin), myItemWidth + myMargin,
        myItemHeight + myMargin);
    }
  }

  private void notifyChange(Color myColor) {
    myListener.colorChanged(myColor);
  }

  public interface ColorChangeEvent {
    void colorChanged(Color newColor);
  }
}
