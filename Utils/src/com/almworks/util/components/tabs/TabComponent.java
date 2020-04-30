package com.almworks.util.components.tabs;

import com.almworks.util.Env;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

public class TabComponent extends JComponent implements MouseMotionListener, MouseListener {
  private static final int ICON_SIZE = 11;
  private static final int CROSS_SIZE = 5;
  private static final Icon ACTIVE_SEL =  createIcon(true, ICON_SIZE, true);
  private static final Icon ACTIVE_UNSEL =  createIcon(true, ICON_SIZE, false);
  private static final Icon INACTIVE_SEL = createIcon(false, ICON_SIZE, true);
  private static final Icon INACTIVE_UNSEL = createIcon(false, ICON_SIZE, false);
  private static int ICON_TEXT_GAP = 7;

  private final ContentTab myTab;
  private final Rectangle myIconRect = new Rectangle();
  private final Point myLastMouse = new Point();
  private boolean myFullTextPainted = false;

  public TabComponent(ContentTab tab) {
    myTab = tab;
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  @Override
  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    super.paintComponent(g);
    Icon icon;
//    icon = createIcon(isMouseOverIcon(), 9, myTab.isSelected());
    if (isMouseOverIcon()) icon = myTab.isSelected() ? ACTIVE_SEL : ACTIVE_UNSEL;
    else icon = myTab.isSelected() ? INACTIVE_SEL : INACTIVE_UNSEL;
    Rectangle tRect = new Rectangle();
    Dimension size = getSize();
    Rectangle cRect = new Rectangle(size);
    myIconRect.setBounds(0, 0, 0, 0);
    RenderingHints savedHints = null;
    FontMetrics metrics = g.getFontMetrics();
    String text =
      SwingUtilities.layoutCompoundLabel(metrics, myTab.getName(), icon, SwingConstants.CENTER, SwingConstants.CENTER,
        SwingConstants.CENTER, SwingConstants.LEFT, cRect, myIconRect, tRect, ICON_TEXT_GAP);
    myIconRect.y = cRect.y + (cRect.height - myIconRect.height) / 2 + 1;
    if (Env.isWindows() && !myTab.isSelected()) {
      // in Windows LAF, text is displayed too low in selected tabs
      myIconRect.y -= 2;
      cRect.y -= 2;
    }
    g.drawString(text, cRect.x + tRect.x, cRect.y + tRect.y + metrics.getAscent());
    myFullTextPainted = text.equals(myTab.getName());
    icon.paintIcon(this, g, myIconRect.x, myIconRect.y);
  }

  @Override
  public Dimension getPreferredSize() {
    FontMetrics metrics = getFontMetrics(getFont());
    int width = ICON_TEXT_GAP + metrics.stringWidth(myTab.getName()) + ICON_TEXT_GAP + ICON_SIZE;
    int height = Math.max(metrics.getHeight(), ICON_SIZE);
    return new Dimension(width, height);
  }

  @Override
  public Dimension getMaximumSize() {
    return getMinimumSize();
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    if (isOverIcon(event.getX(), event.getY())) {
      StringBuilder builder = new StringBuilder();
      builder.append("Close \"").append(myTab.getName()).append("\"");
      if (!Env.isMac()) builder.append(" (Middle click)");
      return builder.toString();
    }
    else {
      String tooltip = myTab.getTooltip();
      if (tooltip == null) tooltip = myTab.getName();
      return tooltip.trim().length() == 0 || (myFullTextPainted && tooltip.equals(myTab.getName())) ? null : tooltip;
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    ToolTipManager.sharedInstance().registerComponent(this);
  }

  @Override
  public void removeNotify() {
    ToolTipManager.sharedInstance().unregisterComponent(this);
    super.removeNotify();
  }

  private boolean isOverIcon(int x, int y) {
    return myIconRect.contains(x, y);
  }

  private boolean isMouseOverIcon() {
    return isOverIcon(myLastMouse.x, myLastMouse.y);
  }

  private void onMouseMove(MouseEvent e) {
    int x = e.getID() == MouseEvent.MOUSE_EXITED ? -1 : e.getX();
    int y = e.getID() == MouseEvent.MOUSE_EXITED ? -1 : e.getY();
    boolean wasOver = isMouseOverIcon();
    myLastMouse.setLocation(x, y);
    boolean isOver = isMouseOverIcon();
    if (wasOver != isOver) repaint(myIconRect);
  }

  private void onMouseButton(MouseEvent e) {
    onMouseMove(e);
    if (e.getID() == MouseEvent.MOUSE_PRESSED) myTab.select(); // All tab actions are obtain SELECTED TAB, not the tab where mouse is pressed. So the tab should be selected first.
    if (e.isPopupTrigger())  {
      myTab.showPopup(e);
    } else if (isCloseTabEvent(e)) {
      myTab.delete();
    } else if (UIUtil.isPrimaryDoubleClick(e)) {
      myTab.toggleExpand(this);
    }
  }

  private boolean isCloseTabEvent(MouseEvent e) {
    if (e.getID() != MouseEvent.MOUSE_RELEASED) return false;
    if (e.getButton() == MouseEvent.BUTTON2) return true;
    return e.getButton() == MouseEvent.BUTTON1 && isOverIcon(e.getX(), e.getY());
  }

  public void mouseClicked(MouseEvent e) {
    onMouseButton(e);
  }

  public void mousePressed(MouseEvent e) {
    onMouseButton(e);
  }

  public void mouseReleased(MouseEvent e) {
    onMouseButton(e);
  }

  public void mouseEntered(MouseEvent e) {
    onMouseMove(e);
  }

  public void mouseExited(MouseEvent e) {
    onMouseMove(e);
  }

  public void mouseDragged(MouseEvent e) {
    onMouseMove(e);
  }

  public void mouseMoved(MouseEvent e) {
    onMouseMove(e);
  }

  private static void paintIcon(Graphics2D g, boolean active, boolean selected) {
    Color bg = selected ? UIManager.getColor("TabbedPane.background") : UIManager.getColor("Panel.background");
    Color fg = UIManager.getColor("Label.foreground");
    Color inactiveColor = ColorUtil.between(bg, fg, 0.25f);
    Color activeColor = Color.RED;

    Color color = active ? activeColor : inactiveColor;
//    Color bright = ColorUtil.between(color, bg, 0.25f);
    Color dark = ColorUtil.between(color, fg, 0.25f);

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    BasicStroke stroke =
      //active ? new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f) :
      new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
    g.setStroke(stroke);
    g.setColor(dark);
    int off = (ICON_SIZE - CROSS_SIZE) / 2;
    drawCross(g, off, off, CROSS_SIZE, CROSS_SIZE);

//    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
//    g.setStroke(new BasicStroke());
//    g.setColor(bright);
//    drawCross(g, 2, 2, size - 2, size - 2);
  }

  private static void drawCross(Graphics2D g, double x, double y, double w, double h) {
    g.draw(new Line2D.Double(x, y, x + w, y + h));
    g.draw(new Line2D.Double(x, y + h, x + w, y));
  }

  private static Icon createIcon(boolean active, int size, boolean selected) {
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
    Graphics2D g = image.createGraphics();
    paintIcon(g, active, selected);
    g.dispose();
    return new ImageIcon(image);
  }
}
