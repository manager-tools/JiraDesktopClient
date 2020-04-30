package com.almworks.timetrack.gui.timesheet;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.api.application.ItemOrder;
import com.almworks.api.application.LoadedItem;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.timetrack.impl.TimeTrackingUtil;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Containers;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.images.Icons;
import com.almworks.util.model.ModelUtils;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.EmptyIcon;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

import static javax.swing.SwingConstants.CENTER;

@SuppressWarnings("WeakerAccess")
public class TimeSheetGrid {
  private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat(" dd ", Locale.US);
  private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.US);
  private static final long H2 = 2 * Const.HOUR;
  static final ItemKeyStub TOTAL_KEY = new ItemKeyStub("total", "Total", ItemOrder.NO_ORDER);

  private final AListModel<WorkPeriod> myWorkList;
  private final AGrid<TaskEntry, DateEntry, Long> myGrid;
  private final OrderListModel<TaskEntry> myTasks = OrderListModel.create();
  private final ValueModel<List<GroupingFunction>> myGroupings = ValueModel.create();
  private final AListModel<LoadedItem> myTasksSourceModel;
  private final Comparator<TaskEntry> myTaskEntryComparator = new TaskEntryComparator();
  private final OrderListModel<DateEntry> myColumns = new OrderListModel<>();
  private final Map<TaskEntry, Map<DateEntry, Long>> myCache = Collections15.hashMap();

  private Color myWeekendColor;
  private final Color[] myColorGrades = new Color[7];
  private final Map<Integer, Icon> myIndentIconCache = Collections15.hashMap();// todo:
  private final ALabel myCornerLabel = new ALabel("Publish Hours Worked");


  public TimeSheetGrid(AListModel<WorkPeriod> workList, AListModel<LoadedItem> sourceArtifactsModel) {
    myWorkList = workList;
    myTasksSourceModel = sourceArtifactsModel;
    myWeekendColor = ColorUtil.between(UIUtil.getEditorBackground(), Color.GRAY, 0.1F);
    myGrid = AGrid.create();
    myGrid.setRowHeaderMaxColumns(30);
    myGrid.setRowModel(Lifespan.FOREVER, myTasks);
    JPanel panel = myGrid.createCornerPanel();
    UIUtil.adjustFont(myCornerLabel, 0.9F, Font.ITALIC, true);
    panel.add(myCornerLabel);
    myGrid.setTopLeftCorner(panel);
    if (Aqua.isAqua()) myGrid.getComponent().setBorder(Aqua.MAC_BORDER_NORTH);
    myGroupings.addAWTChangeListener(this::resyncTaskModel);
    myTasksSourceModel.addAWTChangeListener(Lifespan.FOREVER, this::resyncTaskModel);
    resyncTaskModel();
    myGrid.setRowHeaderRenderer(new LabelRenderer<TaskEntry>() {
      @Override
      protected void setElement(TaskEntry element, CellState state) {
        myLabel.setText(element.toString());
        Color bg = getBackground(element);
        if (bg != null)
          myLabel.setBackground(bg);
        myLabel.setIcon(getIndentIcon(element.getDepth()));
      }
    });
    setupCells();
    syncDateEntries();
    myGrid.setColumnModel(Lifespan.FOREVER, myColumns);
    workList.addAWTChangeListener(Lifespan.FOREVER, this::syncDateEntries);
    myGrid.setColumnHeaderRenderer(new MyColumnHeaderRenderer());
    myGrid.setColumnHeaderPaintsGrid(false);
    myGrid.setEqualColumnWidthByHeaderAndCellRenderer(15);
  }

  @SuppressWarnings("SameParameterValue")
  public void setCornerText(String text) {
    myCornerLabel.setText(text);
  }

  public JComponent getComponent() {
    return myGrid.getComponent();
  }

  public Pair<Long, Long> getDateRange() {
    AGridSelectionModel sm = myGrid.getSelectionModel();
    if (sm.getSelectionType() == AGridSelectionModel.NOTHING_SELECTED)
      return null;
    int rfrom = sm.getRowFrom();
    int rto = sm.getRowTo();
    int cfrom = sm.getColumnFrom();
    int cto = sm.getColumnTo();
    if (rfrom > rto || cfrom > cto)
      return null;
    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE;
    for (int c = cfrom; c <= cto; c++) {
      DateEntry entry = myGrid.getColumnModel().getAt(c);
      min = Math.min(min, entry.getStart());
      max = Math.max(max, entry.getEnd());
    }
    return Pair.create(min, max);
  }

  public Set<LoadedItem> getSelectedArtifacts() {
    AGridSelectionModel sm = myGrid.getSelectionModel();
    if (sm.getSelectionType() == AGridSelectionModel.NOTHING_SELECTED)
      return null;
    int from = sm.getRowFrom();
    int to = sm.getRowTo();
    if (from > to)
      return null;
    HashSet<LoadedItem> set = Collections15.hashSet();
    for (int i = Math.max(0, from); i <= Math.min(to, myTasks.getSize() - 1); i++) {
      addArtifactsToSet(myTasks.getAt(i), set);
    }
    return set;
  }

  private void addArtifactsToSet(TaskEntry entry, Set<LoadedItem> set) {
    if (entry instanceof ArtifactTaskEntry) {
      set.add(((ArtifactTaskEntry) entry).getArtifact());
    } else if (entry instanceof GroupTaskEntry) {
      for (TaskEntry child : ((GroupTaskEntry) entry).getChildren()) {
        addArtifactsToSet(child, set);
      }
    }
  }

  private void syncDateEntries() {
    long mint = Long.MAX_VALUE, maxt = Long.MIN_VALUE;
    for (WorkPeriod p : myWorkList) {
      mint = Math.min(mint, p.getTiming().getStarted());
      maxt = Math.max(maxt, p.getTiming().getStopped() - 1000);
    }
    List<DateEntry> newColumns = okForTiming(mint, maxt) ? createDateEntries(mint, maxt) : Collections.emptyList();
    if (newColumns.size() == myColumns.getSize()) {
      boolean areEqual = true;
      for (int i = 0; i < newColumns.size(); i++) {
        DateEntry newE = newColumns.get(i);
        if (!Objects.equals(newE, myColumns.getAt(i))) {
          areEqual = false;
          break;
        }
      }
      if (areEqual) return;
    }
    myColumns.setElements(newColumns);
  }

  public void setGroupings(List<GroupingFunction> groupings) {
    myGroupings.setValue(groupings);
  }

  private void resyncTaskModel() {
    List<GroupingFunction> groupings = myGroupings.getValue();
    GroupTaskEntry total = new GroupTaskEntry(null, TOTAL_KEY);
    List<TaskEntry> r;
    if (groupings != null) {
      r = createGroupedList(groupings, total);
    } else {
      r = Collections15.arrayList();
      r.add(total);
      for (LoadedItem item : myTasksSourceModel) {
        r.add(new ArtifactTaskEntry(total, item));
      }
    }
    r.sort(myTaskEntryComparator);
    ModelUtils.syncModel(myTasks, r, myTaskEntryComparator);
  }

  private List<TaskEntry> createGroupedList(List<GroupingFunction> groupings, GroupTaskEntry total) {
    List<TaskEntry> r = Collections15.arrayList();
    r.add(total);
    List<Object[]> table = Collections15.arrayList();
    for (LoadedItem item : myTasksSourceModel) {
      Object[] v = new Object[groupings.size() + 1];
      for (int i = 0; i < groupings.size(); i++) {
        GroupingFunction grouping = groupings.get(i);
        ItemKey key = grouping.getGroupValue(item);
        v[i] = key;
      }
      v[groupings.size()] = item;
      table.add(v);
    }
    for (int i = groupings.size() - 1; i >= 0; i--) {
      table.sort(taskGroupingComparator(i));
    }
    boolean[] diffGroups = new boolean[groupings.size()];
    for (int i = 0; i < diffGroups.length; i++) {
      diffGroups[i] = hasDifferences(table, i);
    }
    addGroups(r, table, diffGroups, 0, table.size(), 0, groupings.size(), total);
    return r;
  }

  private boolean hasDifferences(List<Object[]> table, int i) {
    ItemKey first = null;
    for (Object[] a : table) {
      if (first == null)
        first = (ItemKey) a[i];
      else if (!first.equals(a[i])) {
        return true;
      }
    }
    return false;
  }

  private void addGroups(List<TaskEntry> target, List<Object[]> table, boolean[] diffGroups, int from, int to,
                         int tableDepth, int leafDepth, GroupTaskEntry parent)
  {
    if (from >= to)
      return;
    if (tableDepth == leafDepth) {
      for (int i = from; i < to; i++) {
        target.add(new ArtifactTaskEntry(parent, (LoadedItem) table.get(i)[tableDepth]));
      }
    } else if (!diffGroups[tableDepth]) {
      addGroups(target, table, diffGroups, from, to, tableDepth + 1, leafDepth, parent);
    } else {
      int groupIndex = from;
      while (groupIndex < to) {
        ItemKey groupValue = (ItemKey) table.get(groupIndex)[tableDepth];
        GroupTaskEntry p = new GroupTaskEntry(parent, groupValue);
        target.add(p);
        int n;
        for (n = groupIndex; n < to; n++) {
          if (!groupValue.equals(table.get(n)[tableDepth])) {
            break;
          }
        }
        addGroups(target, table, diffGroups, groupIndex, n, tableDepth + 1, leafDepth, p);
        groupIndex = n;
      }
    }
  }

  private Comparator<? super Object[]> taskGroupingComparator(final int i) {
    return (Comparator<Object[]>) (o1, o2) -> {
      ItemKey k1 = (ItemKey) o1[i];
      ItemKey k2 = (ItemKey) o2[i];
      return k1.compareTo(k2);
    };
  }

  private void setupCells() {
    myGrid.setCellModel(Lifespan.FOREVER, new AGridCellFunction<TaskEntry, DateEntry, Long>() {
      @Override
      public Long getValue(TaskEntry row, DateEntry column, int rowIndex, int columnIndex) {
        return getCachedTimeInfo(row, column);
      }
    });
    myGrid.setCellRenderer(new LabelRenderer<Long>() {
      {
        myLabel.setHorizontalAlignment(JLabel.RIGHT);
      }

      @Override
      protected void setElement(Long item, CellState state) {
        if (!state.isSelected()) {
          int row = state.getCellRow();
          int column = state.getCellColumn();
          if (row >= 0 && row < myTasks.getSize() && column >= 0 && column < myGrid.getColumnModel().getSize()) {
            TaskEntry e = myTasks.getAt(row);
            DateEntry dateEntry = myGrid.getColumnModel().getAt(column);
            Color bg;
            if (dateEntry.isWeekend()) {
              bg = myWeekendColor;
            } else if (dateEntry.getLevel() == Integer.MAX_VALUE || e.getDepth() == 0) {
              bg = getSummaryColor(3);
            } else if (dateEntry.getLevel() > 0) {
              bg = getSummaryColor(1);
            } else {
              bg = getBackground(e);
            }
            if (bg != null) {
              myLabel.setBackground(bg);
            }
          }
        }
        if (item != null && item > 0) {
          myLabel.setText(DateUtil.getHoursDurationFixed((int) (item / 1000L)));
        } else {
          myLabel.setText("");
        }
      }
    });
  }

  private Long getCachedTimeInfo(TaskEntry row, DateEntry column) {
    Map<DateEntry, Long> rowmap = myCache.computeIfAbsent(row, k -> Collections15.hashMap());
    if (rowmap.containsKey(column)) {
      // can be null
      return rowmap.get(column);
    }
    Long info = calculateTimeInfo(row, column);
    rowmap.put(column, info);
    return info;
  }

  private Long calculateTimeInfo(TaskEntry row, DateEntry column) {
    if (row instanceof GroupTaskEntry) {
      long total = 0;
      List<TaskEntry> children = ((GroupTaskEntry) row).getChildren();
      for (TaskEntry child : children) {
        Long r = getCachedTimeInfo(child, column);
        if (r != null && r > 0)
          total += r;
      }
      return total;
    } else {
      assert row instanceof ArtifactTaskEntry;
      LoadedItem a = ((ArtifactTaskEntry) row).getArtifact();
      long r = 0;
      long from = column.getStart();
      long to = column.getEnd();
      for (WorkPeriod period : myWorkList) {
        if (period.isExcluded())
          continue;
        if (!Util.equals(period.getArtifact(), a))
          continue;
        TaskTiming timing = period.getTiming();
        long started = timing.getStarted();
        long stopped = timing.getStopped();
        if (stopped >= from && started < to) {
          long s = Math.max(started, from);
          long f = Math.min(stopped, to);
          if (s < f) {
            r += f - s;
          }
        }
      }
      return r;
    }
  }

  private Color getBackground(TaskEntry e) {
    if (e instanceof GroupTaskEntry) {
      return getSummaryColor(e.getDepth() == 0 ? 3 : 1);
    }
    return null;
  }

  private Color getSummaryColor(int colorGrade) {
    if (colorGrade < 0)
      colorGrade = 0;
    if (colorGrade >= myColorGrades.length)
      colorGrade = myColorGrades.length - 1;
    if (myColorGrades[colorGrade] == null) {
      myColorGrades[colorGrade] =
              ColorUtil.between(UIUtil.getEditorBackground(), Color.YELLOW, 0.11F * (colorGrade + 1));
    }
    return myColorGrades[colorGrade];
  }

  private List<DateEntry> createDateEntries(long from, long to) {
    TimeZone tz = TimeZone.getDefault();
    Calendar cal = Calendar.getInstance(tz, Locale.getDefault());
    List<DateEntry> dates = Collections15.arrayList();
    fillFromTo(dates, from, to, tz, cal);

    int firstDay = cal.getFirstDayOfWeek();
    expandStartToFullWeek(dates, firstDay, tz, cal);

    int lastDay = firstDay == Calendar.SUNDAY ?
            Calendar.SATURDAY : (firstDay == Calendar.MONDAY ? Calendar.SUNDAY : firstDay - 1);
    expandEndToFullWeek(dates, lastDay, tz, cal);

    addWeeks(dates, firstDay, lastDay, cal);

    dates.add(new DateEntry(
            dates.get(0).getStart(), dates.get(dates.size() - 1).getEnd(), "Total", Integer.MAX_VALUE, false));

    return dates;
  }


  private void addWeeks(List<DateEntry> dates, int firstDay, int lastDay, Calendar cal) {
    long weekStart = 0;
    int offset = 0;
    for (int i = 0; i < dates.size(); i++) {
      DateEntry entry = dates.get(i);
      cal.setTimeInMillis(entry.getStart());
      int dow = cal.get(Calendar.DAY_OF_WEEK);
      entry.setWeekOffset(offset++);
      if (dow == firstDay) {
        weekStart = entry.getStart();
      } else if (dow == lastDay) {
        assert weekStart > 0;
        dates.add(i + 1, new DateEntry(weekStart, entry.getEnd(),
                MONTH_FORMAT.format(new Date(weekStart)) + "    Week " + cal.get(Calendar.WEEK_OF_YEAR), 1, false));
        offset = 0;
        i++;
      }
    }
  }

  private static void expandEndToFullWeek(List<DateEntry> dates, int lastDay, TimeZone tz, Calendar cal) {
    long s = dates.get(dates.size() - 1).getEnd();
    if(s <= 0L) {
      Log.warn("Invalid end time " + s + " " + dates);
      assert false;
    }
    while (true) {
      cal.setTimeInMillis(s - H2);
      if (cal.get(Calendar.DAY_OF_WEEK) == lastDay)
        break;
      long e = DateUtil.toDayStart(s + Const.DAY + H2, tz);
      cal.setTimeInMillis(s);
      dates.add(new DateEntry(s, e, DAY_FORMAT.format(new Date(s)), 0, isNonworkingDay(cal)));
      s = e;
    }
  }

  private static void expandStartToFullWeek(List<DateEntry> dates, int firstDay, TimeZone tz, Calendar cal) {
    long t = dates.get(0).getStart();
    if(t <= 0L) {
      Log.warn("Invalid start time " + t + " " + dates);
      assert false;
    }
    while (true) {
      cal.setTimeInMillis(t);
      if (cal.get(Calendar.DAY_OF_WEEK) == firstDay)
        return;
      long f = DateUtil.toDayStart(t - H2, tz);
      cal.setTimeInMillis(f);
      dates.add(0, new DateEntry(f, t, DAY_FORMAT.format(new Date(f)), 0, isNonworkingDay(cal)));
      t = f;
    }
  }

  private static void fillFromTo(List<DateEntry> dates, long from, long to, TimeZone tz, Calendar cal) {
    from = DateUtil.toDayStart(from, tz);
    to = DateUtil.toDayStart(to, tz);
    for (long t = from; t <= to;) {
      long e = DateUtil.toDayStart(t + Const.DAY + H2, tz);
      cal.setTimeInMillis(t);
      dates.add(new DateEntry(t, e, DAY_FORMAT.format(new Date(t)), 0, isNonworkingDay(cal)));
      t = e;
    }
  }

  private static boolean isNonworkingDay(Calendar cal) {
    int dow = cal.get(Calendar.DAY_OF_WEEK);
    return dow == Calendar.SUNDAY || dow == Calendar.SATURDAY;
  }

  static boolean okForTiming(long from, long to) {
    return from > 0 && to > 0 && from < to && (to - from) >= TimeTrackingUtil.MINIMAL_INTERVAL;
  }

  @Nullable
  public Long getSelectionEndTime() {
    AGridSelectionModel sm = myGrid.getSelectionModel();
    if (sm.getSelectionType() != AGridSelectionModel.NOTHING_SELECTED) {
      int col = sm.getColumnTo();
      if (col >= 0 && col < myGrid.getColumnModel().getSize()) {
        DateEntry dateEntry = myGrid.getColumnModel().getAt(col);
        return dateEntry.getEnd();
      }
    }
    return null;
  }

  private Icon getIndentIcon(int depth) {
    Icon icon = myIndentIconCache.get(depth);
    if (icon != null)
      return icon;
    icon = new EmptyIcon(depth * 12, 1);
    myIndentIconCache.put(depth, icon);
    return icon;
  }

  public boolean hasSelection() {
    return myGrid.getSelectionModel().getSelectionType() == AGridSelectionModel.NOTHING_SELECTED;
  }

  @Nullable("When no or multiple rows selected")
  public TaskEntry getSingleSelectedRow() {
    AGridSelectionModel sm = myGrid.getSelectionModel();
    if (sm.getRowFrom() == sm.getRowTo() && sm.getRowFrom() >= 0 && sm.getRowFrom() < myTasks.getSize())
      return myTasks.getAt(sm.getRowFrom());
    return null;
  }

  /**
   * @return a pair: the first is {@link DateEntry#getStart() start} of the first selected column and the second is
   * {@link DateEntry#getEnd() end} of the last selected column
   */
  @Nullable("When has no selected columns or selection is not valid")
  public Pair<Long, Long> getSelectedDateRange() {
    AGridSelectionModel sm = myGrid.getSelectionModel();
    if (sm.getSelectionType() != AGridSelectionModel.NOTHING_SELECTED && sm.getColumnFrom() >= 0 &&
            sm.getColumnTo() < myGrid.getColumnModel().getSize()) {
      return Pair.create(myGrid.getColumnModel().getAt(sm.getColumnFrom()).getStart(), myGrid.getColumnModel().getAt(sm.getColumnTo()).getEnd());
    }
    return null;
  }

  public void refresh() {
    myCache.clear();
    myGrid.getComponent().repaint();
  }

  public void addSelectionChangeListener(ChangeListener listener) {
    myGrid.getSelectionModel().getModifiable().addAWTChangeListener(listener);
  }

  private class MyColumnHeaderRenderer extends BaseRendererComponent implements CollectionRenderer<DateEntry> {
    private final int ROWS = 2;
    private DateEntry myEntry;
    private int myRowHeight;

    private Dimension psize = new Dimension();
    private Rectangle pviewR = new Rectangle();
    private Rectangle ptextR = new Rectangle();
    private Rectangle piconR = new Rectangle();
    private final FontRenderContext myFrc = new FontRenderContext(null, false, false);

    private MyColumnHeaderRenderer() {
      myRowHeight = UIManager.getInt("Table.rowHeight");
      if (myRowHeight == 0)
        myRowHeight = 14;
      setFont(UIManager.getFont("Label.font"));
      setForeground(UIManager.getColor("Label.foreground"));
      setOpaque(true);
      setBackground(UIUtil.getEditorBackground());
    }

    public JComponent getRendererComponent(CellState state, DateEntry item) {
      myEntry = item;
      return this;
    }

    @Override
    public Dimension getPreferredSize() {
      if (myEntry == null || myEntry.getLevel() > 0)
        return new Dimension(5, 5);
      Rectangle2D bounds = getFont().getStringBounds(myEntry.getName(), myFrc);
      return new Dimension((int) bounds.getWidth() + 4, myRowHeight * ROWS);
    }

    @Override
    protected void paintComponent(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      getSize(psize);
      g.setColor(getBackground());
      g.fillRect(0, 0, psize.width, psize.height);
      if (myEntry == null)
        return;
      Dimension spacing = myGrid.getGridSpacing();
      int y = psize.height - myRowHeight;
      Font f = getFont();
      FontMetrics fm = getFontMetrics(f);
      paintLowestLevel(g, y, psize.height, psize.width, spacing, f, fm);
      int y2 = y;
      y -= myRowHeight;
      paintWeek(g, y, y2, psize.width, spacing, f, fm);
    }

    private void paintWeek(Graphics g, int y, int y2, int width, Dimension spacing, Font f, FontMetrics fm) {
      g.setColor(getSummaryColor(myEntry.getLevel() == Integer.MAX_VALUE ? 3 : 1));
      g.fillRect(0, y, width, y2 - y);
      g.setColor(myGrid.getGridColor());
      if (myEntry.getLevel() == 0) {
        g.fillRect(0, y2 - 1, width, spacing.height);
        int ofs = myEntry.getWeekOffset();
        if (ofs >= 0) {
          int i = myGrid.getColumnModel().indexOf(myEntry);
          if (i >= 0) {
            DateEntry week = null;
            for (i++; i < myGrid.getColumnModel().getSize(); i++) {
              DateEntry e = myGrid.getColumnModel().getAt(i);
              if (e.getLevel() == 1) {
                week = e;
                break;
              }
            }
            if (week != null) {
              int x = -ofs * width + 2;
              Rectangle2D bounds = f.getStringBounds(week.getName(), myFrc);
              if (x + bounds.getWidth() > 0) {
                g.setColor(getForeground());
                g.setFont(f);
                int dy = Math.max(0, (y2 - y - fm.getAscent() - fm.getDescent()) / 2);
                g.drawString(week.getName(), x, y + dy + fm.getAscent());
              }
            }
          }
        }
      } else {
        g.fillRect(width - 1, y, spacing.width, y2 - y);
      }
    }

    private void paintLowestLevel(Graphics g, int y, int height, int width, Dimension spacing, Font f, FontMetrics fm) {
      if (myEntry.isWeekend()) {
        g.setColor(myWeekendColor);
        g.fillRect(0, y, width, height - y);
      } else if (myEntry.getLevel() > 0) {
        g.setColor(getSummaryColor(myEntry.getLevel() == Integer.MAX_VALUE ? 3 : 1));
        g.fillRect(0, y, width, height - y);
      }
      int level = myEntry.getLevel();
      if (level == 0) {
        pviewR.setBounds(0, y, width, height - y);
        piconR.setBounds(0, 0, 0, 0);
        ptextR.setBounds(0, 0, 0, 0);
        String s =
                SwingUtilities.layoutCompoundLabel(this, fm, myEntry.getName(), null, CENTER, CENTER, 0, 0, pviewR, piconR,
                        ptextR, 0);
        g.setFont(f);
        g.setColor(getForeground());
        g.drawString(s, ptextR.x, ptextR.y + fm.getAscent());
      } else if (level == Integer.MAX_VALUE) {
        pviewR.setBounds(0, y, width, height - y);
        piconR.setBounds(0, 0, 0, 0);
        ptextR.setBounds(0, 0, 0, 0);
        SwingUtilities.layoutCompoundLabel(this, fm, "", Icons.TOTAL_SIGMA, CENTER, CENTER, 0, 0, pviewR, piconR,
                ptextR, 0);
        Icons.TOTAL_SIGMA.paintIcon(this, g, piconR.x, piconR.y);
      }
      Color c = myGrid.getGridColor();
      g.setColor(c);
      g.fillRect(0, height - 1, width, spacing.height);
      g.fillRect(width - 1, y, spacing.width, height - y);
    }
  }

  private class TaskEntryComparator implements Comparator<TaskEntry> {
    public int compare(TaskEntry o1, TaskEntry o2) {
      if (o1 == o2)
        return 0;
      if (o1 == null || o2 == null) {
        assert false : o1 + " " + o2;
        return o1 == null ? -1 : 1;
      }
      int d1 = o1.getDepth();
      int d2 = o2.getDepth();
      if (d1 < d2)
        return compare(o1, o2.getParent());
      if (d2 < d1)
        return compare(o1.getParent(), o2);
      if (o1.getParent() != o2.getParent())
        return compare(o1.getParent(), o2.getParent());
      if (o1 instanceof GroupTaskEntry) {
        assert o2 instanceof GroupTaskEntry;
        return ((GroupTaskEntry) o1).getGroupValue().compareTo(((GroupTaskEntry) o2).getGroupValue());
      }
      assert o1 instanceof ArtifactTaskEntry;
      assert o2 instanceof ArtifactTaskEntry;
      return compareArtifactEntries(((ArtifactTaskEntry) o1), ((ArtifactTaskEntry) o2));
    }

    private int compareArtifactEntries(ArtifactTaskEntry e1, ArtifactTaskEntry e2) {
      // todo - something more obvious
      if (e1 == e2)
        return 0;
      int r = e1.getKey().compareToIgnoreCase(e2.getKey());
      return r != 0 ? r :
              Containers.compareLongs(e1.getArtifact().getItem(), e2.getArtifact().getItem());
    }
  }
}
