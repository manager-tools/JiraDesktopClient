package com.almworks.timetrack.gui.timesheet;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.timetrack.gui.DurationField;
import com.almworks.timetrack.impl.TaskRemainingTime;
import com.almworks.timetrack.impl.TimeTrackingUtil;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.etable.BaseCellEditor;
import com.almworks.util.components.etable.ColumnEditor;
import com.almworks.util.components.etable.EdiTableManager;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.Context;
import com.almworks.util.i18n.Local;
import com.almworks.util.model.ModelUtils;
import com.almworks.util.models.ColumnAccessor;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.ScrollBarPolicy;
import com.almworks.util.ui.UIComponentWrapper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The "Basic" form for "Edit and Publish Time". 
 * @author Pavel Zvyagin
 */
public class BasicPublishTimeForm implements UIComponentWrapper {
  private final JPanel myWholePanel = new JPanel(new BorderLayout());
  private final ASortedTable<TaskWork> myTaskTable;
  private final OrderListModel<TaskWork> myTaskModel = OrderListModel.create();
  private final DetachComposite myLife = new DetachComposite();
  private final TimesheetFormData myData;
  private final TimeTrackingCustomizer myCustomizer = Context.require(TimeTrackingCustomizer.ROLE);
  private final Comparator<ItemWrapper> myArtifactComparator = myCustomizer.getArtifactByKeyComparator();

  public BasicPublishTimeForm(TimesheetFormData data) {
    myData = data;

    myTaskTable = createTable();
    myTaskTable.setCollectionModel(myTaskModel);
    myWholePanel.add(myTaskTable, BorderLayout.CENTER);

    ScrollBarPolicy.setDefaultWithHorizontal(myTaskTable, ScrollBarPolicy.AS_NEEDED);
    if(Aqua.isAqua()) {
      myTaskTable.setBorder(Aqua.MAC_BORDER_NORTH_SOUTH);
    }

    myData.getModifiable().addAWTChangeListener(myLife, new ChangeListener() {
      public void onChange() {
        rebuildInternals();
      }
    });
    rebuildInternals();
  }

  private void rebuildInternals() {
    final Map<LoadedItem, List<WorkPeriod>> timeMap = myData.getWorkMap();
    final Map<LoadedItem, TaskRemainingTime> remMap = myData.getRemainingTimes();
    final Map<LoadedItem, Integer> deltaMap = myData.getSpentDeltas();

    final Set<LoadedItem> items = CollectionUtil.setUnion(
      timeMap.keySet(), remMap.keySet(), deltaMap.keySet());

    final List<TaskWork> taskWorks = Collections15.arrayList();
    for(final LoadedItem a : items) {
      List<WorkPeriod> periods = timeMap.get(a);
      if(periods == null) {
        periods = Collections15.emptyList();
      }
      taskWorks.add(new TaskWork(this, a, periods, remMap.get(a), deltaMap.get(a)));
    }

    final SelectionAccessor<TaskWork> selAccessor = myTaskTable.getSelectionAccessor();
    final List<TaskWork> oldSelection = selAccessor.getSelectedItems();

    ModelUtils.syncModel(myTaskModel, taskWorks, Containers.comparablesComparator());

    // Custom code to restore selection due to TaskWork's compareTo()/equals()
    // inconsistency (which is intentional in order to use ModelUtils.syncModel()).
    if(!oldSelection.isEmpty()) {
      selAccessor.clearSelection();
      final LoadedItem[] a = { null };
      final Condition<TaskWork> c = new Condition<TaskWork>() {
        @Override
        public boolean isAccepted(TaskWork value) {
          return a[0].equals(value.myItem);
        }
      };
      for(final TaskWork oldWork : oldSelection) {
        a[0] = oldWork.myItem;
        final BasicPublishTimeForm.TaskWork newWork = myTaskModel.detect(c);
        if(newWork != null) {
          selAccessor.addSelection(newWork);
        }
      }
    }
  }

  private ASortedTable<TaskWork> createTable() {
    final ASortedTable<TaskWork> table = new ASortedTable<TaskWork>();
    table.setGridHidden();
    table.setStriped(true);

    final JTable jTable = (JTable)table.getSwingComponent();
    jTable.getTableHeader().setReorderingAllowed(false);

    final List<TableColumnAccessor<TaskWork, ?>> columns = Collections15.arrayList();

    // "Publish" checkbox column.
    class Includer extends ButtonActor.Checkbox<TaskWork> {
      protected boolean isSelected(TaskWork item) {
        return !item.excluded;
      }

      protected void act(TaskWork edited) {
        togglePublish(edited);
        myData.onWorkPeriodChanged();
      }
    }

    columns.add(
      TableColumnBuilder.<TaskWork, TaskWork>create("include", "Publish")
        .setEditor(new Includer())
        .setRenderer(new Includer())
        .setConvertor(Convertor.<TaskWork>identity())
        .setSizePolicy(ColumnSizePolicy.Calculated.fixedTextWithMargin("Publish", 1))
        .createColumn());

    // "Issue Key" / "Bug ID" column.
    class KeySummary implements CanvasRenderer<TaskWork>, Comparator<TaskWork> {
      public void renderStateOn(CellState state, Canvas canvas, TaskWork item) {
        if (item.excluded) {
          canvas.setForeground(TimesheetForm.EXCLUDED_COLOR);
        }
        final Pair<String, String> pair = myCustomizer.getItemKeyAndSummary(item.myItem);
        canvas.appendText(pair.getFirst() + " " + pair.getSecond());
      }
      
      public int compare(TaskWork o1, TaskWork o2) {
        return myArtifactComparator.compare(o1.myItem, o2.myItem);
      }
    }

    final KeySummary ks = new KeySummary();
    columns.add(
      TableColumnBuilder.<TaskWork, TaskWork>create("keySummary", Local.parse(Terms.ref_Artifact))
        .setCanvasRenderer(ks)
        .setComparator(ks)
        .setConvertor(Convertor.<TaskWork>identity())
        .setSizePolicy(new MySizePolicy(300))
        .createColumn());


    // Auxiliary objects and classes for time columns.
    final Comparator<Integer> intComparator = Containers.comparablesComparator(true);
    
    abstract class TimeGetter implements CanvasRenderer<TaskWork>, Comparator<TaskWork> {
      abstract Integer getTimeValue(TaskWork item);

      public void renderStateOn(CellState state, Canvas canvas, TaskWork item) {
        if (item.excluded) {
          canvas.setForeground(TimesheetForm.EXCLUDED_COLOR);
        } else {
          // The concept of a focused cell is not needed while rendering.
          // While editing, the "focus" (edited cell) traversals are managed by EdiTableManager.
          canvas.setForeground(state.isSelected()
            ? jTable.getSelectionForeground()
            : jTable.getForeground());
        }

        if(isOverridden(item)) {
          canvas.setFontStyle(Font.BOLD);
        }

        final Integer time = getTimeValue(item);
        if(time != null) {
          canvas.appendText(DateUtil.getFriendlyDuration(time, false));
        }
      }

      public int compare(TaskWork o1, TaskWork o2) {
        return intComparator.compare(getTimeValue(o1), getTimeValue(o2));
      }

      protected boolean isOverridden(TaskWork item) {
        return false;
      }
    }

    final ColumnSizePolicy myPolicy = ColumnSizePolicy.Calculated.fixedTextWithMargin("Remaining Time", 1);

    // Additional Time Spent column.
    final BaseCellEditor<DurationField, TaskWork> sptEditor = new SpentCellEditor();
    final CanvasRenderer<TaskWork> sptRenderer =
      new TimeGetter() {
        Integer getTimeValue(TaskWork item) {
          return item.totalTimeSpent();
        }

        @Override
        protected boolean isOverridden(TaskWork item) {
          return item.timeSpentDelta != null;
        }
      };

    columns.add(
      TableColumnBuilder.<TaskWork, TaskWork>create("addTimeSpent", "Time Spent")
        .setEditor(sptEditor)
        .setCanvasRenderer(sptRenderer)
        .setConvertor(Convertor.<TaskWork>identity())
        .setSizePolicy(myPolicy)
        .createColumn());

    // New Remaining Time column.
    final BaseCellEditor<DurationField, TaskWork> remEditor = new RemainingCellEditor();
    final CanvasRenderer<TaskWork> remRenderer =
      new TimeGetter() {
        Integer getTimeValue(TaskWork item) {
          return item.remainingNew;
        }

        @Override
        protected boolean isOverridden(TaskWork item) {
          return myData.getRemainingTimes().containsKey(item.myItem);
        }
      };

    columns.add(
      TableColumnBuilder.<TaskWork, TaskWork>create("newRemaining", "Remaining Time")
        .setEditor(remEditor)
        .setCanvasRenderer(remRenderer)
        .setConvertor(Convertor.<TaskWork>identity())
        .setSizePolicy(myPolicy)
        .createColumn());

    table.setColumnModel(FixedListModel.create(columns));

    // Spacebar to toggle "Publish".
    jTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle");
    jTable.getActionMap().put("toggle", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        final List<TaskWork> items = table.getSelectionAccessor().getSelectedItems();
        for(final TaskWork item : items) {
          togglePublish(item);
        }
        myData.onWorkPeriodChanged();
      }
    });

    // Setting up keyboard navigation.
    final Map<ColumnEditor, Integer> ecMap = Collections15.hashMap();
    ecMap.put(sptEditor, 2);
    ecMap.put(remEditor, 3);
    new EdiTableManager(jTable, ecMap);

    return table;
  }

  private void togglePublish(TaskWork item) {
    final List<WorkPeriod> periods = myData.getWorkMap().get(item.myItem);
    if(periods != null && !periods.isEmpty()) {
      for(final WorkPeriod p : periods) {
        p.setExcluded(!item.excluded);
      }
    }
    myData.setSpentDeltaNoUpdate(item.myItem, null);
    myData.setExcluded(item.myItem, !item.excluded);
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void dispose() {
    myLife.detach();
  }

  /**
   * A structure summarizing all WorkPeriods on a certain
   * artifact. Keeps total spent and remaining times and
   * a compound included/excluded state.
   */
  final static class TaskWork implements Comparable<TaskWork> {
    final LoadedItem myItem;
    final BasicPublishTimeForm myForm;

    final Boolean excluded;
    final Integer timeSpentOld;
    final Integer timeSpentAdd;
    final Integer timeSpentDelta;
    final Integer remainingOld;
    final Integer remainingNew;

    TaskWork(
      BasicPublishTimeForm form, LoadedItem item,
      List<WorkPeriod> periods, TaskRemainingTime remaining, Integer timeDelta)
    {
      myItem = item;
      myForm = form;

      timeSpentOld = myForm.myCustomizer.getTimeSpent(item);
      remainingOld = myForm.myCustomizer.getRemainingTime(item);

      boolean hasIncludedPeriods = false;
      for(final WorkPeriod p : periods) {
        if(!p.isExcluded()) {
          hasIncludedPeriods = true;
          break;
        }
      }

      if(remaining == null && timeDelta == null) {
        excluded = !hasIncludedPeriods;
      } else {
        excluded = myForm.myData.isExcluded(item);
      }

      timeSpentAdd = periods.isEmpty() ?
        null : TimeTrackingUtil.getElapsedTimeSinceForPeriods(periods, Long.MIN_VALUE);
      timeSpentDelta = timeDelta;

      if(remaining == null && remainingOld != null) {
        remaining = TaskRemainingTime.old(remainingOld);
      }
      remainingNew = TimeTrackingUtil.getRemainingTimeForPeriods(periods, remaining);
    }

    public Integer totalTimeSpent() {
      if(timeSpentAdd != null || timeSpentDelta != null) {
        return Util.NN(timeSpentAdd, 0) + Util.NN(timeSpentDelta, 0);
      }
      return null;
    }

    /* equals() is NOT consistent with compareTo(). Generated for ModelUtils.syncModel(). */
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final TaskWork taskWork = (TaskWork) o;

      if (!myItem.equals(taskWork.myItem)) {
        return false;
      }
      if (excluded != null ? !excluded.equals(taskWork.excluded) : taskWork.excluded != null) {
        return false;
      }
      if (remainingNew != null ? !remainingNew.equals(taskWork.remainingNew) : taskWork.remainingNew != null) {
        return false;
      }
      if (remainingOld != null ? !remainingOld.equals(taskWork.remainingOld) : taskWork.remainingOld != null) {
        return false;
      }
      if (timeSpentAdd != null ? !timeSpentAdd.equals(taskWork.timeSpentAdd) : taskWork.timeSpentAdd != null) {
        return false;
      }
      if (timeSpentDelta != null ? !timeSpentDelta.equals(taskWork.timeSpentDelta) : taskWork.timeSpentDelta != null) {
        return false;
      }
      if (timeSpentOld != null ? !timeSpentOld.equals(taskWork.timeSpentOld) : taskWork.timeSpentOld != null) {
        return false;
      }

      return true;
    }

    /* hashCode() generated to be consistent with equals(). */
    @Override
    public int hashCode() {
      int result = myItem.hashCode();
      result = 31 * result + (excluded != null ? excluded.hashCode() : 0);
      result = 31 * result + (timeSpentOld != null ? timeSpentOld.hashCode() : 0);
      result = 31 * result + (timeSpentAdd != null ? timeSpentAdd.hashCode() : 0);
      result = 31 * result + (timeSpentDelta != null ? timeSpentDelta.hashCode() : 0);
      result = 31 * result + (remainingOld != null ? remainingOld.hashCode() : 0);
      result = 31 * result + (remainingNew != null ? remainingNew.hashCode() : 0);
      return result;
    }

    /* compareTo() is NOT consistent with equals(). Created for ModelUtils.syncModel(). */
    public int compareTo(TaskWork o) {
      return myForm.myArtifactComparator.compare(myItem, o.myItem);
    }
  }

  /**
   * The concrete column/cell editor for the "Remaining Time" column.
   */
  private class RemainingCellEditor extends BaseCellEditor<DurationField, TaskWork> {
    private RemainingCellEditor() {
      super(new DurationField());
    }

    @Override
    protected void doSetValue(DurationField field, TaskWork item) {
      if(item.remainingNew != null) {
        field.setSeconds(item.remainingNew);
      } else {
        field.setText("");
      }
    }

    @Override
    protected void doSaveEdit(DurationField field, TaskWork item) {
      final int seconds = field.getSeconds();
      if(seconds < 0
          && field.getText().length() == 0
          && item.remainingNew != null) {
        myData.setRemainingTime(item.myItem, null);
      } else if(seconds >= 0
          && (item.remainingNew == null
            || item.remainingNew != seconds)) {
        myData.setRemainingTime(item.myItem, TaskRemainingTime.now(seconds));
      }
    }
  }

  /**
   * The concrete column/cell editor for the "Time Spent" column.
   */
  private class SpentCellEditor extends BaseCellEditor<DurationField, TaskWork> {
    private SpentCellEditor() {
      super(new DurationField());
    }

    @Override
    protected void doSetValue(DurationField field, TaskWork item) {
      final Integer spent = item.totalTimeSpent();
      if(spent != null) {
        field.setSeconds(spent);
      } else {
        field.setText("");
      }
    }

    @Override
    protected void doSaveEdit(DurationField field, TaskWork item) {
      final int seconds = field.getSeconds();
      if(seconds < 0
          && field.getText().length() == 0
          && item.timeSpentDelta != null) {
        myData.setSpentDelta(item.myItem, null);
      } else if(seconds >= 0) {
        final int realSpent = Util.NN(item.timeSpentAdd, 0);
        if(realSpent != seconds) {
          myData.setSpentDelta(item.myItem, seconds - realSpent);
        }
      }
    }
  }

  /**
   * A custom {@code ColumnSizePolicy} that works here.
   */
  private static final class MySizePolicy implements ColumnSizePolicy {
    static final int MIN = 75;
    static final int MAX = 65000;

    private final int myMin;
    private final int myPref;

    public MySizePolicy(int pref) {
      myPref = pref;
      myMin = Math.min(pref, MIN);
    }

    public <E> int getPreferredWidth(JTable table, ATableModel<E> model, ColumnAccessor<E> column, int columnIndex) {
      return myPref;
    }

    public void setWidthParameters(int preferredWidth, TableColumn column) {
      if(myMin > column.getMaxWidth()) {
        column.setMaxWidth(MAX);
        column.setMinWidth(myMin);
      } else {
        column.setMinWidth(myMin);
        column.setMaxWidth(MAX);
      }
      column.setPreferredWidth(preferredWidth);
     }

    public int validateForcedWidth(TableColumn column, int width) {
      if(width <= 0) {
        return -1;
      }

      final int pref = column.getPreferredWidth();
      if(width == pref) {
        return width;
      }

      if(width < pref) {
        return Math.max(width, myMin);
      } else {
        return Math.min(width, MAX);
      }
    }
  }
}
