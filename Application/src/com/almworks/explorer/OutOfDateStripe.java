package com.almworks.explorer;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.macosx.MacCornerButton;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.BrokenLineBorder;
import com.almworks.util.ui.actions.PresentationMapping;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author dyoma
 */
public class OutOfDateStripe extends JComponent implements Scrollable {
  private static final String TOOLTIP = Local.parse(
    "This " + Terms.ref_artifact + " has been changed. To update the table, click on the refresh button above.");

  private final static Icon OUT_OF_DATE = Icons.ARTIFACT_STATE_OUTDATED;
  private final static int LEFT_MARGINE = 2;
  private final static int RIGHT_MARGINE = 2;
  private final static int TOTAL_MARGINE = LEFT_MARGINE + RIGHT_MARGINE;
  private final FlatCollectionComponent<LoadedItem> myComponent;
  private final Lifecycle mySwingLife = new Lifecycle(false);
  private final JComponent myCornerComponent;
  private final JScrollPane myScrollPane;

  private ATable myTableNeighbor;

  private OutOfDateStripe(FlatCollectionComponent<LoadedItem> component, JComponent cornerComponent,
    JScrollPane scrollPane)
  {
    myComponent = component;
    myCornerComponent = cornerComponent;
    myScrollPane = scrollPane;
  }

  public void showStripe(boolean show) {
    if (show) {
      JViewport rowHeader = myScrollPane.getRowHeader();
      if (rowHeader != null && rowHeader.getView() == this) {
        assert myCornerComponent == myScrollPane.getCorner(JScrollPane.UPPER_LEFT_CORNER);
        return;
      }
      myScrollPane.setRowHeaderView(this);
      myScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, myCornerComponent);

      final Component view = myScrollPane.getViewport().getView();
      if(view instanceof ATable) {
        myTableNeighbor = (ATable) view;
      } else {
        myTableNeighbor = null;
      }
    } else {
      myScrollPane.setRowHeader(null);
      myScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, null);
    }
  }

  public Dimension getPreferredSize() {
    return getMaximumSize();
  }

  public Dimension getMinimumSize() {
    return setWidth(new Dimension(0, 0));
  }

  public Dimension getMaximumSize() {
    return setWidth(new Dimension(0, Short.MAX_VALUE));
  }

  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return 0;
  }

  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return 0;
  }

  public void addNotify() {
    super.addNotify();
    if (mySwingLife.cycleStart()) {
      myComponent.getCollectionModel().addAWTChangeListener(mySwingLife.lifespan(), new ChangeListener() {
        public void onChange() {
          repaint();
        }
      });
    }
  }

  public void removeNotify() {
    mySwingLife.cycleEnd();
    super.removeNotify();
  }

  protected void paintComponent(Graphics gg) {
    Graphics g = gg.create();
    try {
      AwtUtil.applyRenderingHints(g);
      Rectangle clipBounds = g.getClipBounds();

      if(myTableNeighbor != null && myTableNeighbor.isStriped()) {
        paintStripes(gg, clipBounds);
      }

      int minRow = myComponent.getScrollingElementAt(1, clipBounds.y);
      int maxRow = myComponent.getScrollingElementAt(1, clipBounds.y + clipBounds.height);
      if (maxRow < 0) {
        return;
      }
      if (minRow < 0) {
        minRow = 0;
      }
      for (int i = minRow; i <= maxRow; i++) {
        paintRowIcon(i, g);
      }
      ImmediateTooltips.tooltipChanged(this);
    } finally {
      g.dispose();
    }
  }

  private void paintRowIcon(int i, Graphics g) {
    LoadedItem item = myComponent.getCollectionModel().getAt(i);
    if (item.isOutOfDate()) {
      Rectangle rowRect = myComponent.getElementRect(i);
      if (rowRect != null) {
        int y = rowRect.y + (rowRect.height - OUT_OF_DATE.getIconHeight()) / 2;
        OUT_OF_DATE.paintIcon(this, g, LEFT_MARGINE, y);
      }
    }
  }

  private void paintStripes(Graphics g, Rectangle clip) {
    final int rowHeight = myTableNeighbor.getRowHeight();
    final int height = clip.y + clip.height;
    for(int i = 0; i <= height / rowHeight; i++) {
      g.setColor(i % 2 == 0 ? myTableNeighbor.getBackground() : myTableNeighbor.getStripeBackground());
      g.fillRect(clip.x, i * rowHeight, clip.width, rowHeight);
    }
  }

  public String getToolTipText(MouseEvent event) {
    if (event == null)
      return null;
    int yy = event.getY();
    int row = myComponent.getScrollingElementAt(1, yy);
    AListModel<? extends LoadedItem> model = myComponent.getCollectionModel();
    if (row < 0 || row >= model.getSize())
      return null;
    Rectangle rect = myComponent.getElementRect(row);
    if (rect == null || rect.y > yy || rect.y + rect.height < yy)
      return null;
    LoadedItem item = model.getAt(row);
    return item.isOutOfDate() ? TOOLTIP : null;
  }

  private Dimension setWidth(Dimension size) {
    size.width = OUT_OF_DATE.getIconWidth() + TOTAL_MARGINE;
    return size;
  }

  public static OutOfDateStripe create(FlatCollectionComponent<LoadedItem> list, JScrollPane scrollPane) {
    AActionButton button;
    if(Aqua.isAqua()) {
      button = new MacCornerButton();
      button.setBorder(new BrokenLineBorder(
        Aqua.MAC_LIGHT_BORDER_COLOR, 1, BrokenLineBorder.SOUTH | BrokenLineBorder.EAST));
    } else {
      button = new AActionButton();
    }
    button.setActionById(MainMenu.Search.REFRESH_RESULTS);
    button.overridePresentation(PresentationMapping.VISIBLE_NONAME);
    OutOfDateStripe stripe = new OutOfDateStripe(list, button, scrollPane);
    scrollPane.setRowHeaderView(stripe);
    scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, stripe);
    ImmediateTooltips.installImmediateTooltipManager(Lifespan.FOREVER, stripe, TooltipLocationProvider.UNDER_MOUSE);
    return stripe;
  }
}
