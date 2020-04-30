package com.almworks.sumtable;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.util.Env;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.events.DetachableValue;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

class SummaryTable {
  private static final String GRID_CARD = "GRID_CARD";
  private static final String NO_DATA_CARD = "NO_DATA_CARD";

  private final SummaryTableConfiguration myConfiguration;
  private final SummaryTableQueryExecutor myExecutor;
  private final AGrid<STFilter, STFilter, Void> myGrid = AGrid.create();
  private final MyCellRenderer myCellRenderer = new MyCellRenderer();
  private final SummaryTableCounter myCounter;
  private final JPanel myWholePanel = new JPanel(new BorderLayout());
  private final JPanel mySorterSelectionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
  private final AComboBox<STFilter> mySorterSelection = new AComboBox<STFilter>();
  private final AActionButton myClearSorting = new AActionButton();
  private final JPanel myDataPlace;

  private SummaryTableData myLastData;
  private final TableDataProvider myDataProvider = new TableDataProvider();

  private final DetachableValue<QueryResult> myQueryResult = DetachableValue.create();

  private final Bottleneck myTableUpdater = new Bottleneck(100, ThreadGate.AWT, new Runnable() {
    public void run() {
      updateTable();
    }
  });

  public SummaryTable(SummaryTableConfiguration configuration, boolean windowed, SummaryTableQueryExecutor executor) {
    myConfiguration = configuration;
    myExecutor = executor;
    AListModel<STFilter> columnModel = myConfiguration.getColumnsConfiguration().getOptionsModel();
    AListModel<STFilter> rowModel = myConfiguration.getRowsConfiguration().getOptionsModel();
    AListModel<STFilter> counterModel = myConfiguration.getCounterConfiguration().getCounterModel();
    myCounter = new SummaryTableCounter(columnModel, rowModel, counterModel);
    myGrid.setColumnModel(Lifespan.FOREVER, columnModel);
    myGrid.setRowModel(Lifespan.FOREVER, rowModel);
    myGrid.setCellModel(Lifespan.FOREVER, new AGridCellFunction<STFilter, STFilter, Void>() {
      public Void getValue(STFilter row, STFilter column, int rowIndex, int columnIndex) {
        return null;
      }
    });
    myGrid.setColumnHeaderRenderer(new GridHeaderRenderer(!windowed, myConfiguration.getColumnsConfiguration(), false));
    myGrid.setRowHeaderRenderer(new GridHeaderRenderer(!windowed, myConfiguration.getRowsConfiguration(), true));
    myGrid.setCellRenderer(myCellRenderer);
    myGrid.setBackground(UIUtil.getEditorBackground());

    mySorterSelection.setCanvasRenderer(Renderers.defaultCanvasRenderer());
    mySorterSelection.setColumns(15);

    mySorterSelectionPanel.setBorder(new EmptyBorder(5, 0, 1, 0));
    JLabel label = new JLabel();
    NameMnemonic.parseString("S&ort by:").setToLabel(label);
    label.setLabelFor(mySorterSelection);
    mySorterSelectionPanel.add(label);
    mySorterSelectionPanel.add(mySorterSelection);
    if(Aqua.isAqua()) {
      mySorterSelectionPanel.setBorder(Aqua.MAC_BORDER_NORTH);
    } else if(Aero.isAero() && windowed) {
      UIUtil.addOuterBorder(mySorterSelectionPanel, Aero.getAeroBorderNorth());
    }
    myWholePanel.add(mySorterSelectionPanel, BorderLayout.SOUTH);

    myDataPlace = new JPanel(new CardLayout());

    final JComponent messagePanel = UIUtil.createMessagePanel("<html><body>No data available", true, true, UIUtil.BORDER_9);
    myDataPlace.add(messagePanel, NO_DATA_CARD);
    Aqua.cleanScrollPaneBorder(messagePanel);
    if(windowed) {
      Aero.cleanScrollPaneBorder(messagePanel);
    }

    final JComponent gridComponent = myGrid.getComponent();
    myDataPlace.add(gridComponent, GRID_CARD);
    Aqua.cleanScrollPaneBorder(gridComponent);
    Aqua.cleanScrollPaneResizeCorner(gridComponent);
    if(windowed) {
      Aero.cleanScrollPaneBorder(gridComponent);
    }

    myDataPlace.setOpaque(true);
    myDataPlace.setBackground(UIUtil.getEditorBackground());

    myWholePanel.add(myDataPlace, BorderLayout.CENTER);

    myClearSorting.setAnAction(new ClearSortingAction());
    myClearSorting.setMargin(new Insets(4, 4, 4, 4));
    JPanel panel = myGrid.createCornerPanel();
    panel.add(myClearSorting);
    myGrid.setTopLeftCorner(panel);
  }

  public Component getComponent() {
    return myWholePanel;
  }

  public DataProvider getDataProvider() {
    return myDataProvider;
  }

  public void attach(Lifespan lifespan, final GenericNode node, QueryResult result) {
    myQueryResult.set(lifespan, result);
    myCounter.attach(lifespan, myExecutor.getDatabase(), node);
    attachUpdateTable(lifespan);
    attachUpdateTableFormat(lifespan);
    attachUpdateSorter(lifespan);
  }

  private void attachUpdateSorter(Lifespan lifespan) {
    CounterConfiguration counterConfiguration = myConfiguration.getCounterConfiguration();
    AListModel<STFilter> counters = counterConfiguration.getCounterModel();
    final SelectionInListModel<STFilter> model = SelectionInListModel.create(lifespan, counters, null);
    int sorterIndex = counterConfiguration.getSorterIndex();
    if (sorterIndex >= 0 && sorterIndex < model.getSize())
      model.setSelectedItem(model.getAt(sorterIndex));

    mySorterSelection.setModel(model);
    SorterStateSynchronizer listener = new SorterStateSynchronizer(counterConfiguration, model);
    counters.addAWTChangeListener(lifespan, listener);
    model.addSelectionListener(lifespan, listener);
  }

  private void attachUpdateTableFormat(Lifespan lifespan) {
    lifespan.add(
      myConfiguration.getCounterConfiguration().getCounterModel().addListener(new AListModel.Adapter<STFilter>() {
        public void onInsert(int index, int length) {
          updateTableFormat();
        }

        public void onRemove(int index, int length, AListModel.RemovedEvent<STFilter> event) {
          updateTableFormat();
        }
      }));
    updateTableFormat();
  }

  private void attachUpdateTable(Lifespan lifespan) {
    ScalarModel<CountingSummaryTableData> dataModel = myCounter.getDataModel();
    dataModel.getEventSource().addAWTListener(lifespan, new ScalarModel.Adapter<CountingSummaryTableData>() {
      public void onScalarChanged(ScalarModelEvent<CountingSummaryTableData> event) {
        myTableUpdater.request();
      }
    });
    myConfiguration.addSortingListener(lifespan, new ChangeListener() {
      public void onChange() {
        myTableUpdater.request();
      }
    });
    updateTable();
  }

  private void updateTableFormat() {
    int counters = myConfiguration.getCounterConfiguration().getCounterModel().getSize();
    FontMetrics metrics = myCellRenderer.getFontMetrics(myCellRenderer.getFont());
    int lineHeight = Math.max(18, metrics.getAscent() + metrics.getDescent());
    int rowHeight = Math.max(1, counters) * lineHeight;
    myGrid.setRowHeight(rowHeight);
  }

  private void updateTable() {
    CountingSummaryTableData table = myCounter.getDataModel().getValue();
    myLastData = table;
    if (table != null) {
      if (!table.isDataAvailable()) {
        ((CardLayout) myDataPlace.getLayout()).show(myDataPlace, NO_DATA_CARD);
        myGrid.setAxisModels(Lifespan.FOREVER, AListModel.EMPTY, AListModel.EMPTY);
      } else {
        boolean sorterVisible = false;
        ((CardLayout) myDataPlace.getLayout()).show(myDataPlace, GRID_CARD);
        List<STFilter> columns = table.getColumns();
        List<STFilter> rows = table.getRows();

        AxisConfiguration hconf = myConfiguration.getColumnsConfiguration();
        STFilter colSorter = hconf.getSortFilter();
        int colDirection = hconf.getSortDirection();
        int colSorterIndex = colSorter == null ? -1 : columns.indexOf(STFilter.findById(columns, colSorter.getId()));

        AxisConfiguration vconf = myConfiguration.getRowsConfiguration();
        STFilter rowSorter = vconf.getSortFilter();
        int rowDirection = vconf.getSortDirection();
        int rowSorterIndex = rowSorter == null ? -1 : rows.indexOf(STFilter.findById(rows, rowSorter.getId()));

        if ((colSorterIndex >= 0 && colDirection != 0) || (rowSorterIndex >= 0 && rowDirection != 0)) {
          int counterSorterIndex = myConfiguration.getCounterConfiguration().getSorterIndex();

          myLastData = table.sort(colSorterIndex, colDirection, rowSorterIndex, rowDirection, counterSorterIndex);
          columns = myLastData.getColumns();
          rows = myLastData.getRows();

          sorterVisible = myLastData.getCounters().size() > 1;
        }

        myGrid.setAxisModels(Lifespan.FOREVER, FixedListModel.create(rows), FixedListModel.create(columns));
        myGrid.setEqualColumnWidthByHeaderRenderer(15);
        mySorterSelectionPanel.setVisible(sorterVisible);
      }
    }
    myDataProvider.fireChanged(SummaryTableData.DATA);
  }


  private static class GridHeaderRenderer extends BaseRendererComponent implements CollectionRenderer<STFilter> {
    private static Rectangle ourPaintRect = new Rectangle();
    private static Rectangle ourIconRect = new Rectangle();
    private static Rectangle ourTextRect = new Rectangle();

    private static final int BORDER_MARGIN = 5;
    private static final int ICON_TEXT_MARGIN = 4;
    private static final String MIN_PROTOTYPE = "99999";
    private static final String MAX_PROTOTYPE = "012345678901234567890123456789";

    private final AxisConfiguration myConfiguration;
    private final boolean myVertical;
    private final boolean myNarrowSetup;

    private Color myPressedColor;
    private Color myHoverColor;
    private Color myTotalBackground;
    private Font myTotalFont;
    private boolean myButton1;
    private int myDefaultMinWidth;
    private int myDefaultMaxWidth;

    private CellState myState;
    private STFilter myItem;
    private boolean myHover;
    private boolean myPressed;
    private Icon myIcon;
    private String myName;
    private int myMaxWidth;


    public GridHeaderRenderer(boolean narrowSetup, AxisConfiguration configuration, boolean vertical) {
      myNarrowSetup = narrowSetup;
      myConfiguration = configuration;
      myVertical = vertical;
    }

    public JComponent getRendererComponent(CellState state, STFilter item) {
      prepare(state, item);
      return this;
    }

    private void prepare(CellState state, STFilter item) {
      myState = state;
      myItem = item;
      myHover = false;
      myPressed = false;
      myIcon = item.getIcon();
      myName = item.getName();
      if (myDefaultMinWidth == 0) {
        myDefaultMinWidth = getPrototypeWidth(state, MIN_PROTOTYPE);
      }
      if (myDefaultMaxWidth == 0) {
        myDefaultMaxWidth = Math.min(getPrototypeWidth(state, MAX_PROTOTYPE), 100);
      }
      myMaxWidth = myDefaultMaxWidth;
      if (myNarrowSetup) {
        if (myIcon != null) {
          myName = "";
        } else {
          myMaxWidth = myVertical ? myDefaultMinWidth * 2 : myDefaultMinWidth * 6 / 5;
        }
      }
    }

    private int getPrototypeWidth(CellState state, String s) {
      Font font = state.getFont();
      Rectangle2D r = font.getStringBounds(s, new FontRenderContext(null, false, false));
      return (int) Math.ceil(r.getWidth()) + 2 * BORDER_MARGIN;
    }

    public Dimension getPreferredSize() {
      if (myItem == null)
        return new Dimension(0, 0);

      FontMetrics fontMetrics = getFontMetrics(selectFont(myState.getFont()));
      ourPaintRect.setBounds(0, 0, Short.MAX_VALUE, Short.MAX_VALUE);
      ourIconRect.setBounds(0, 0, 0, 0);
      ourTextRect.setBounds(0, 0, 0, 0);
      SwingUtilities.layoutCompoundLabel(this, fontMetrics, myName, myIcon, SwingConstants.CENTER, SwingConstants.LEFT,
        SwingConstants.CENTER, SwingConstants.RIGHT, ourPaintRect, ourIconRect, ourTextRect, ICON_TEXT_MARGIN);

      int w;
      if (myName == null || myName.length() == 0)
        w = ourIconRect.width;
      else if (myIcon == null)
        w = ourTextRect.width;
      else
        w = ourTextRect.width + (ourTextRect.x - ourIconRect.x);

      int sortIconWidth = Icons.TABLE_ROW_SORTED_DESCENDING.getIconWidth();
      w += 2 * BORDER_MARGIN + ICON_TEXT_MARGIN + sortIconWidth;
      w = Math.max(Math.min(w, myMaxWidth), myDefaultMinWidth);
      int h = Math.max(ourTextRect.height, ourIconRect.height + 2);
      return new Dimension(w, h);
    }

    protected void paintComponent(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      ourPaintRect.setBounds(0, 0, getWidth(), getHeight());
      clickHeader();
      paintHeader(g);
    }

    private void clickHeader() {
      boolean wasButton1 = myButton1;
      myButton1 = false;
      MouseEvent event = myState.getMouseEvent();
      if (event == null || event.isConsumed())
        return;
      Point point = myState.getMousePoint(this);
      if (point != null && ourPaintRect.contains(point)) {
        event.consume();
        boolean clicked = false;
        int id = event.getID();
        if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_ENTERED) {
          myHover = true;
        } else if (id == MouseEvent.MOUSE_PRESSED) {
          myButton1 = (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK;
          if (myButton1) {
            myPressed = true;
          }
        } else
        if (id == MouseEvent.MOUSE_CLICKED && wasButton1 && !event.isPopupTrigger() && event.getClickCount() == 1) {
          clicked = true;
        }

        if (clicked) {
          myConfiguration.applySorting(myItem);
        }
      }
    }

    private void paintHeader(Graphics g) {
      Font font = selectFont(myState.getFont());
      g.setColor(getBackgroundColor());
      g.setFont(font);
      UIUtil.safeFillRect(g, ourPaintRect.x, ourPaintRect.y, ourPaintRect.width, ourPaintRect.height);
      Border border = myState.getBorder();
      if (border != null) {
        border.paintBorder(this, g, ourPaintRect.x, ourPaintRect.y, ourPaintRect.width, ourPaintRect.height);
        Insets insets = border.getBorderInsets(this);
        ourPaintRect.x += insets.left;
        ourPaintRect.y += insets.top;
        ourPaintRect.width -= insets.left + insets.right;
        ourPaintRect.height -= insets.top + insets.bottom;
      }
      ourPaintRect.grow(-BORDER_MARGIN, 0);

      Icon sortingIcon = null;
      if (STFilter.equalIds(myItem, myConfiguration.getSortFilter())) {
        int direction = myConfiguration.getSortDirection();
        if (direction != 0) {
          if (myVertical) {
            sortingIcon = direction > 0 ? Icons.TABLE_ROW_SORTED_ASCENDING : Icons.TABLE_ROW_SORTED_DESCENDING;
          } else {
            sortingIcon = direction > 0 ? Icons.TABLE_COLUMN_SORTED_ASCENDING : Icons.TABLE_COLUMN_SORTED_DESCENDING;
          }
        }
      }

      int sortingIconX = -1;
      if (sortingIcon != null) {
        int w = sortingIcon.getIconWidth();
        sortingIconX = ourPaintRect.x + ourPaintRect.width - w;
        ourPaintRect.width -= w + ICON_TEXT_MARGIN;
      }

      FontMetrics fontMetrics = getFontMetrics(font);
      ourIconRect.setBounds(0, 0, 0, 0);
      ourTextRect.setBounds(0, 0, 0, 0);
      String renderName = SwingUtilities.layoutCompoundLabel(this, fontMetrics, myName, myIcon, SwingConstants.CENTER,
        SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.RIGHT, ourPaintRect, ourIconRect, ourTextRect,
        ICON_TEXT_MARGIN);

      int baseline =
        renderName.length() > 0 ? ourTextRect.y + fontMetrics.getAscent() : ourIconRect.y + ourIconRect.height - 1;

      if (sortingIcon != null) {
        sortingIcon.paintIcon(this, g, sortingIconX, baseline - sortingIcon.getIconHeight());
      }

      if (myIcon != null) {
        myIcon.paintIcon(this, g, ourIconRect.x, ourIconRect.y);
      }

      if (renderName.length() > 0) {
        g.setColor(myState.getForeground());
        g.drawString(renderName, ourTextRect.x, baseline);
      }

      String originalName = myItem.getName();
      if (!originalName.equals(renderName)) {
        myState.setFeedbackTooltip(originalName);
      }
    }

    private Font selectFont(Font font) {
      if (myItem.isTotal()) {
        if (myTotalFont == null) {
          myTotalFont = font.deriveFont(Font.BOLD);
        }
        return myTotalFont;
      } else {
        return font;
      }
    }

    private Color getBackgroundColor() {
      if (myPressed) {
        if (myPressedColor == null) {
          myPressedColor = ColorUtil.between(myState.getDefaultBackground(), myState.getDefaultForeground(), 0.2F);
        }
        return myPressedColor;
      } else if (myHover) {
        if (myHoverColor == null) {
          myHoverColor = ColorUtil.between(myState.getDefaultBackground(), myState.getDefaultForeground(), 0.1F);
        }
        return myHoverColor;
      } else if (myItem.isTotal()) {
        return getTotalBackground();
      } else {
        return myState.getBackground();
      }
    }

    private Color getTotalBackground() {
      if (myTotalBackground == null) {
        myTotalBackground = ColorUtil.between(UIUtil.getEditorBackground(), Color.YELLOW, 0.1F);
      }
      return myTotalBackground;
    }
  }


  private class MyCellRenderer extends BaseRendererComponent implements CollectionRenderer<Void>, STFilterController {
    private int myColumn;
    private int myRow;
    private CellState myState;
    private MouseEvent myLastMouseEvent;
    private Rectangle myCounterR = new Rectangle();
    private STFilter myCurrentCounter;

    private Color myTotalBackground;
    private Font myTotalFont;

    public MyCellRenderer() {
      setFont(UIManager.getFont("Label.font"));
    }

    public JComponent getRendererComponent(CellState state, Void item) {
      myState = state;
      myColumn = state.getCellColumn();
      myRow = state.getCellRow();
      myLastMouseEvent = state.getMouseEvent();

      Color bg = state.getBackground();
      Font font = state.getFont();
      SummaryTableData data = myLastData;
      if (data != null) {
        boolean totalColumn = false;
        boolean totalRow = false;
        List<STFilter> columns = data.getColumns();
        if (myColumn >= 0 && myColumn < columns.size()) {
          totalColumn = columns.get(myColumn).isTotal();
        }
        List<STFilter> rows = data.getRows();
        if (myRow >= 0 && myRow < rows.size()) {
          totalRow = rows.get(myRow).isTotal();
        }
        if ((totalColumn || totalRow) && !state.isSelected()) {
          bg = getTotalBackground();
        }
        if (totalColumn && totalRow) {
          font = getTotalFont(font);
        }
      }
      setBackground(bg);
      setForeground(state.getForeground());
      setFont(font);
      setBorder(state.getBorder());

      return this;
    }

    private Font getTotalFont(Font font) {
      if (myTotalFont == null) {
        myTotalFont = font.deriveFont(Font.BOLD);
      }
      return myTotalFont;
    }


    public void paint(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      Graphics bgg = Env.isMac() ? g.create() : g;
      try {
        bgg.setColor(getBackground());
        bgg.fillRect(0, 0, getWidth(), getHeight());
      } finally {
        if (bgg != g)
          bgg.dispose();
      }
      paintComponent(g);
      paintBorder(g);
    }

    protected void paintComponent(Graphics g) {
      Insets insets = getInsets();
      SummaryTableData data = myLastData;
      java.util.List<STFilter> counters = data == null ? null : data.getCounters();
      if (counters == null)
        return;
      int countersCount = counters.size();
      if (countersCount == 0) {
        // todo
      } else {
        Dimension size = getSize();
        Point mousePoint = null;
        if (myLastMouseEvent != null) {
          Point p = CellState.getMousePoint(myLastMouseEvent, this);
          if (p != null && p.x >= insets.left && p.x < size.width - insets.right) {
            mousePoint = p;
          }
        }

        int height = (size.height - insets.top - insets.bottom) / countersCount;
        Rectangle r = new Rectangle(insets.left, insets.top, size.width - insets.left - insets.right, height);
        for (int i = 0; i < countersCount; i++) {
          STFilter counter = counters.get(i);
          Integer count = data.getCellCount(i, myRow, myColumn);
          myCounterR.setBounds(r);
          myCurrentCounter = counter;
          if (mousePoint != null && r.contains(mousePoint)) {
            counter.paintCount(this, g, i, countersCount, myCounterR, count, mousePoint, myLastMouseEvent, this);
          } else {
            counter.paintCount(this, g, i, countersCount, myCounterR, count, null, null, this);
          }
          myCurrentCounter = null;
          r.y += height;
        }
      }
    }

    private Color getTotalBackground() {
      if (myTotalBackground == null) {
        myTotalBackground = ColorUtil.between(UIUtil.getEditorBackground(), Color.YELLOW, 0.1F);
      }
      return myTotalBackground;
    }

    public void runQuery(boolean newTab) {
      QueryResult queryResult = myQueryResult.get();
      if (queryResult == null)
        return;

      SummaryTableData data = myLastData;
      if (data == null) {
        assert false;
        return;
      }
      List<STFilter> counters = data.getCounters();
      STFilter counter = myCurrentCounter;
      if (counter == null) {
        assert false;
        return;
      }
      int counterIndex = counters.indexOf(counter);
      if (counterIndex < 0) {
        assert false : counter + " " + counters;
        return;
      }
      List<STFilter> rows = data.getRows();
      if (myRow < 0 || myRow >= rows.size()) {
        assert false : myRow + " " + rows;
        return;
      }
      List<STFilter> columns = data.getColumns();
      if (myColumn < 0 || myColumn >= columns.size()) {
        assert false : myColumn + " " + columns;
        return;
      }
      STFilter rowFilter = rows.get(myRow);
      STFilter columnFilter = columns.get(myColumn);
      Integer count = data.getCellCount(counterIndex, myRow, myColumn);
      myExecutor.runQuery(queryResult, counter, columnFilter, rowFilter, count, newTab);
    }

    public void setFeedbackCursor(Cursor cursor) {
      myState.setFeedbackCursor(cursor);
    }

    public void setFeedbackTooltip(String string) {
      myState.setFeedbackTooltip(string);
    }
  }


  private static class SorterStateSynchronizer extends SelectionListener.SelectionOnlyAdapter
    implements ChangeListener
  {
    private final CounterConfiguration myConfiguration;
    private final SelectionInListModel<STFilter> myModel;

    public SorterStateSynchronizer(CounterConfiguration configuration, SelectionInListModel<STFilter> model) {
      myConfiguration = configuration;
      myModel = model;
    }

    public void onChange() {
      STFilter selectedItem = myModel.getSelectedItem();
      STFilter item = selectedItem;
      if (item != null && myModel.indexOf(item) == -1)
        item = null;
      if (item == null && myModel.getSize() > 0)
        item = myModel.getAt(0);
      if (item != selectedItem)
        myModel.setSelectedItem(item);
    }

    public void onSelectionChanged() {
      myConfiguration.setSortingCounter(myModel.getSelectedItem());
    }
  }


  private class ClearSortingAction extends SimpleAction {
    public ClearSortingAction() {
      super("cs");
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Clear sorting");
      setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK));
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      AxisConfiguration rc = myConfiguration.getRowsConfiguration();
      AxisConfiguration cc = myConfiguration.getColumnsConfiguration();
      context.updateOnChange(rc.getSortingModifiable());
      context.updateOnChange(cc.getSortingModifiable());
      context.setEnabled(rc.getSortFilter() != null || cc.getSortFilter() != null);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      myConfiguration.getRowsConfiguration().setSorting(null, 0);
      myConfiguration.getColumnsConfiguration().setSorting(null, 0);
    }
  }


  private class TableDataProvider extends AbstractDataProvider {
    public TableDataProvider() {
      super(SummaryTableData.DATA);
    }

    @Nullable
    public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
        if (role == SummaryTableData.DATA) {
          SummaryTableData data = myLastData;
          return data == null ? Collections15.<T>emptyList() : Collections.singletonList((T)data);
        } else {
          return null;
        }
      }
  }
}
