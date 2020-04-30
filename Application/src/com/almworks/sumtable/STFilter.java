package com.almworks.sumtable;

import com.almworks.api.application.UnresolvedNameException;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.util.DPHolder;
import com.almworks.util.Env;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.images.Icons;
import com.almworks.util.text.parser.FormulaWriter;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.ColorUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.LineMetrics;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class STFilter implements CanvasRenderable {
  private static final int TEXT_ICON_GAP = 4;
  private static final Color[] AUTO_COLORS = new Color[] {Color.BLACK, Color.BLUE, Color.RED, Color.CYAN};
  private static final HashMap<Color, Icon> COLOR_ICONS = Collections15.hashMap();
  public static final STFilter TOTAL = new STFilter(BoolExpr.<DP>TRUE(), "Total", Icons.TOTAL_SIGMA, "#total");

  private static Rectangle ourViewR = new Rectangle();
  private static Rectangle ourIconR = new Rectangle();
  private static Rectangle ourTextR = new Rectangle();

  private static final String UNNAMED = "<Unnamed>";

  @Nullable
  private String myName;

  @Nullable
  private STFilterFormat myFormat;

  @Nullable
  private DPHolder myFilter;

  @Nullable
  private String myFormula;

  @Nullable
  private FilterNode myFilterNode;

  @Nullable
  private Icon myIcon;

  @NotNull
  private String myId;

  private DBAttribute myAttribute;
  private Set<Long> myLongs;

  private transient int myLastPressModifiers;

  public STFilter(@Nullable BoolExpr<DP> filter, String name, Icon icon, String id) {
    myIcon = icon;
    myFilter = DPHolder.wrap(filter);
    myName = name;
    myId = id;
  }

  public STFilter(String name, String formula, FilterNode filterNode, @Nullable BoolExpr<DP> filter) {
    myName = name;
    myFormula = formula;
    myFilterNode = filterNode;
    myFilter = DPHolder.wrap(filter);
    myId = formula == null ? name : formula;
  }

  public STFilter(BoolExpr<DP> filter, String displayName, Icon icon, String id, DBAttribute attr, Set<Long> longs) {
    this(filter, displayName, icon, id);
    myAttribute = attr;
    myLongs = longs;
  }

  public String getName() {
    return Util.NN(myName);
  }

  @CanBlock
  public boolean accepts(long item, DBReader reader) {
    DPHolder filter = myFilter;
    if (filter == null) return false;
    BoolExpr<DP> dp = filter.getResolved(reader);
    return dp.eval(DP.evaluator(item, reader));
  }

  public DBAttribute getAttribute() {
    return myAttribute;
  }

  public boolean accepts(Object attrValue) {
    if(myLongs != null) {
      if(attrValue instanceof Collection) {
        Collection<?> collection = (Collection<?>) attrValue;
        if (collection.isEmpty()) attrValue = null;
        else return containsAny(myLongs, collection);
      }
      return attrValue != null ? myLongs.contains(attrValue) : myLongs.contains(0l);
    }
    assert false;
    return false;
  }

  private boolean containsAny(Set<Long> longs, Collection<?> values) {
    for(final Object val : values) {
      if(longs.contains(val)) {
        return true;
      }
    }
    return false;
  }

  public DBFilter filter(DBFilter view) {
    DPHolder filter = myFilter;
    return view.filter(filter == null ? BoolExpr.<DP>FALSE() : filter.getFilter());
  }

  @NotNull
  public STFilterFormat getFormat() {
    return myFormat == null ? STFilterFormat.DEFAULT : myFormat;
  }

  public void renderOn(Canvas canvas, CellState state) {
    if (myIcon != null) {
      canvas.setIcon(myIcon);
    }
    if (myName == null || myName.length() == 0)
      canvas.appendText(UNNAMED);
    else
      canvas.appendText(myName);
  }

  public void paintCount(JComponent c, Graphics g, int counterIndex, int countersCount, Rectangle r,
    @Nullable Integer count, @Nullable Point mousePoint, @Nullable MouseEvent lastMouseEvent,
    STFilterController controller)
  {
    STFilterFormat format = getFormat();

    String string = count == null ? "" : String.valueOf(count);
    if (format.isUseLabel() && count != null && count > 0) {
      String label;
      if (format.getLabelMode() == STFilterFormat.LABEL_MODE_SPECIFIED) {
        label = format.getSpecifiedLabel();
      } else {
        label = getName().trim();
        if (label.length() > 0 && !label.endsWith(":")) {
          label = label + ":";
        }
      }
      if (label.length() > 0) {
        string = label + " " + string;
      }
    }

    Icon icon = null;
    if (format.isUseIcon() && count != null && count > 0) {
      int iconMode = format.getIconMode();
      if (iconMode == STFilterFormat.ICON_MODE_SELECTED_COLOR) {
        Color color = format.getSelectedColor();
        if (color != null) {
          icon = getColorIcon(color);
        }
      } else if (iconMode == STFilterFormat.ICON_MODE_BY_FILTER) {
        icon = myIcon;
      } else {
        if (counterIndex < AUTO_COLORS.length && countersCount > 1) {
          icon = getColorIcon(AUTO_COLORS[counterIndex]);
        }
      }
    }

    ourViewR.setBounds(r);

    ourViewR.grow(-2, 0);
    ourIconR.setBounds(0, 0, 0, 0);
    ourTextR.setBounds(0, 0, 0, 0);

    Font font = c.getFont();
    FontMetrics metrics = c.getFontMetrics(font);
    string = SwingUtilities.layoutCompoundLabel(c, metrics, string, icon, SwingConstants.CENTER, SwingConstants.RIGHT,
      SwingConstants.CENTER, SwingConstants.RIGHT, ourViewR, ourIconR, ourTextR, TEXT_ICON_GAP);

    ourTextR.grow(6, 0);
    boolean highlight = mousePoint != null && ourTextR.contains(mousePoint);
    ourTextR.grow(-6, 0);

    Color color = getColor(c, highlight, count);
    g.setColor(color);
    g.setFont(font);
    g.drawString(string, ourTextR.x, ourTextR.y + metrics.getAscent());
    if (highlight) {
      LineMetrics lineMetrics = metrics.getLineMetrics(string, g);
      int y = Math.round(ourTextR.y + lineMetrics.getAscent() + lineMetrics.getUnderlineOffset() + 1);
      int x = icon == null ? ourTextR.x - 1 : ourIconR.x - 1;
      g.drawLine(x, y, ourTextR.x + ourTextR.width + 1, y);
      controller.setFeedbackCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      if (countersCount > 1)
        controller.setFeedbackTooltip(getName().trim());

    }

    if (icon != null) {
      icon.paintIcon(c, g, ourIconR.x, ourIconR.y);
    }

    if (lastMouseEvent != null) {
      int id = lastMouseEvent.getID();
      if (id == MouseEvent.MOUSE_PRESSED) {
        myLastPressModifiers = lastMouseEvent.getModifiersEx();
      }
      boolean shouldGo = highlight || lastMouseEvent.getClickCount() > 1;
      if (!lastMouseEvent.isConsumed() && id == MouseEvent.MOUSE_CLICKED && shouldGo) {
        if ((myLastPressModifiers & MouseEvent.BUTTON1_DOWN_MASK) > 0) {
          lastMouseEvent.consume();
          boolean newTab = (myLastPressModifiers & (MouseEvent.CTRL_DOWN_MASK | MouseEvent.ALT_DOWN_MASK)) > 0;
          controller.runQuery(newTab);
        }
      }
      if (lastMouseEvent.isPopupTrigger()) {
        // todo
      }
    }
  }

  private Icon getColorIcon(Color color) {
    Icon icon = COLOR_ICONS.get(color);
    if (icon == null) {
      icon = new ColorBox(color, 4);
      COLOR_ICONS.put(color, icon);
    }
    return icon;
  }

  private Color getColor(Component component, boolean highlight, Integer count) {
    Color c;
    if (highlight) {
      c = UIManager.getColor("Link.hover");
      if (c != null)
        return c;
      else
        return Color.BLUE;
    } else {
      if (count != null && count > 0) {
        return component.getForeground();
      } else {
        return ColorUtil.between(component.getBackground(), component.getForeground(), 0.3F);
      }
    }
  }

  public void setName(String name) {
    myName = name;
  }

  public void setFormat(STFilterFormat format) {
    myFormat = format;
  }

  @NotNull
  @ThreadAWT
  public String getFormula() {
    Threads.assertAWTThread();
    return Util.NN(myFormula);
  }

  public void setFilterNode(@Nullable FilterNode filterNode, ItemHypercube hypercube) {
    myFilterNode = filterNode;
    if (filterNode == null) {
      myFormula = null;
      myFilter = null;
      myId = Util.NN(myName);
    } else {
      myFormula = FormulaWriter.write(filterNode);
      myId = myFormula;
      try {
        myFilter = DPHolder.wrap(filterNode.createFilter(hypercube));
      } catch (UnresolvedNameException e) {
        Log.warn("cannot resolve " + e.getMessage());
        myFilter = null;
      }
    }
  }

  @Nullable
  public FilterNode getFilterNode() {
    return myFilterNode;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  boolean isTotal() {
    return this == TOTAL;
  }

  @Nullable
  public Icon getIcon() {
    return myIcon;
  }

  public void setIcon(Icon icon) {
    myIcon = icon;
  }


  @Nullable
  public BoolExpr<DP> getFilter() {
    DPHolder filter = myFilter;
    return filter != null ? filter.getFilter() : null;
  }

  public static STFilter findById(Iterable<STFilter> it, @Nullable String id) {
    for (STFilter s : it)
      if (s != null && s.myId.equals(id))
        return s;
    return null;
  }

  public static STFilter findById(Iterable<STFilter> it, @Nullable STFilter s) {
    return findById(it, s == null ? null : s.myId);
  }

  public static boolean equalIds(STFilter f1, STFilter f2) {
    return f1 == f2 || f1 != null && f2 != null && f1.myId.equals(f2.myId);
  }

  private static class ColorBox implements Icon {
    private final Color myColor;
    private final int mySize;

    public ColorBox(Color color, int size) {
      myColor = color;
      mySize = size;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics bgg = Env.isMac() ? g.create() : g;
      try {
        bgg.setColor(myColor);
        bgg.fillRect(x + 1, y + 1, mySize - 1, mySize - 1);
      } finally {
        if (bgg != g)
          bgg.dispose();
      }
    }

    public int getIconWidth() {
      return mySize;
    }

    public int getIconHeight() {
      return mySize;
    }
  }
}
