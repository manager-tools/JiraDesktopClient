package com.almworks.fontscale;

import com.almworks.api.fontscale.FontScaler;
import com.almworks.appinit.AWTEventPreprocessor;
import com.almworks.appinit.EventQueueReplacement;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.ComponentProperty;
import org.picocontainer.Startable;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

// todo next release
// this doesn't work now
// too much to do
public class FontScalerImpl implements FontScaler, Startable {
  private static final ComponentProperty<Font> FONT_SCALE_ROOT = ComponentProperty.createProperty("font");
  private final Configuration myConfiguration;

  public FontScalerImpl(Configuration configuration) {
    myConfiguration = configuration;
  }

  public void installScaleRoot(JComponent component, String configClass) {
    assert FONT_SCALE_ROOT.getClientValue(component) == null;
    Font font = component.getFont();
    assert font != null;
    FONT_SCALE_ROOT.putClientValue(component, font);

  }

  public void start() {
    EventQueueReplacement.ensureInstalled().addPreprocessor(new AWTEventPreprocessor() {
      public boolean preprocess(AWTEvent event, boolean alreadyConsumed) {
        if (alreadyConsumed)
          return false;
        if (event instanceof MouseWheelEvent) {
          MouseWheelEvent e = (MouseWheelEvent) event;
          if ((e.getModifiersEx() & (MouseWheelEvent.CTRL_DOWN_MASK | MouseWheelEvent.ALT_DOWN_MASK | MouseWheelEvent.META_DOWN_MASK)) == MouseWheelEvent.CTRL_DOWN_MASK) {
            Object source = e.getSource();
            if (source instanceof Component) {
              Point point = e.getPoint();
              Component component = SwingUtilities.getDeepestComponentAt((Component) source, point.x, point.y);
              int clicks = e.getWheelRotation();
              if (clicks != 0) {
                Font font = component.getFont();
                if (font != null) {
                  int size = font.getSize();
                  int newSize = adjustSize(size, clicks);
                  component.setFont(font.deriveFont((float)newSize));
                  if (component instanceof JTable) {
                    double factor = ((double)((JTable) component).getRowHeight()) / size;
                    int newRowHeight = (int) Math.round(newSize * factor);
                    ((JTable) component).setRowHeight(newRowHeight);
                    JTableHeader tableHeader = ((JTable) component).getTableHeader();
                    tableHeader.setPreferredSize(new Dimension(0, newRowHeight));
                    tableHeader.invalidate();
                  }
                  if (component instanceof JTree) {
                    double factor = ((double)((JTree) component).getRowHeight()) / size;
                    int newRowHeight = (int) Math.round(newSize * factor);
                    ((JTree) component).setRowHeight(newRowHeight);
                  }
                  component.invalidate();
                  Container parent = component.getParent();
                  if (parent instanceof JComponent)
                    ((JComponent) parent).revalidate();
                  component.repaint();
                  return true;
                }
              }
            }
          }
        }
        return false;
      }

      @Override
      public boolean postProcess(AWTEvent event, boolean alreadyConsumed) {
        return false;
      }

      private int adjustSize(int size, int clicks) {
        if (size <= 20)
          size += clicks;
        else if (size <= 40)
          size += clicks * 2;
        else if (size <= 60)
          size += clicks * 4;
        else
          size += clicks * 6;
        if (size <= 5)
          return 5;
        if (size >= 72)
          return 72;
        return size;
      }
    });

  }

  public void stop() {

  }
}
