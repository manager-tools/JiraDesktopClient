package com.almworks.timetrack.gui.timesheet;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.gui.DialogResult;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.English;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.ASortedTable;
import com.almworks.util.components.ButtonActor;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.Configuration;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.Context;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.model.ModelUtils;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.SimpleColumnAccessor;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

public class TimesheetForm implements UIComponentWrapper {
  private final TimeSheetGrid myGrid2;
  private final DetachComposite myLife = new DetachComposite();
  private final TimesheetFormData myData;

  private final OrderListModel<LoadedItem> myTasksSourceModel = OrderListModel.create();
  private ConfiguredSplitPane mySplitPane;

  private final ASortedTable<WorkPeriod> myDetailsTable;
  private final OrderListModel<WorkPeriod> myDetailsModel = OrderListModel.create();
  private final JLabel myDetailsLabel = new JLabel();
  static final Color EXCLUDED_COLOR =
    ColorUtil.between(AwtUtil.getTextComponentForeground(), UIUtil.getEditorBackground(), 0.6F);

  private final EditWorkPeriodAction myEditWorkPeriodAction = new EditWorkPeriodAction();

  private final class Includer extends ButtonActor.Checkbox<WorkPeriod> {
    protected boolean isSelected(WorkPeriod item) {
      return !item.isExcluded();
    }

    protected void act(WorkPeriod edited) {
      togglePulish(edited);
      myData.onWorkPeriodChanged();
      refreshData();
    }
  }

  private final TimeTrackingCustomizer myCustomizer = Context.require(TimeTrackingCustomizer.ROLE);

  public TimesheetForm(TimesheetFormData data, Configuration config) {
    myData = data;
    myDetailsTable = createTable();
    myDetailsTable.setCollectionModel(myDetailsModel);
    myDetailsTable.setDataRoles(WorkPeriod.ROLE);
    myDetailsTable.getTable().addDoubleClickListener(myLife, (workPeriodACollectionComponent, index, element) -> {
      //noinspection ThrowableNotThrown
      ActionUtil.performSafe(myEditWorkPeriodAction, myDetailsTable);
    });

    final JPanel detailsPanel = new JPanel(new BorderLayout());

    final ToolbarBuilder tb = ToolbarBuilder.smallVisibleButtons();
    tb.addAction(new AddWorkPeriodAction(), myDetailsTable);
    tb.addAction(myEditWorkPeriodAction, myDetailsTable);
    tb.addAction(new DeleteWorkPeriodAction(), myDetailsTable);

    final JPanel tableTopPanel = new JPanel(new BorderLayout());
    tableTopPanel.add(tb.createHorizontalToolbar(), BorderLayout.WEST);
    myDetailsLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
    tableTopPanel.add(myDetailsLabel, BorderLayout.CENTER);

    detailsPanel.add(tableTopPanel, BorderLayout.NORTH);
    detailsPanel.add(myDetailsTable);

    myGrid2 = new TimeSheetGrid(data.getWorkList(), myTasksSourceModel);
    mySplitPane = ConfiguredSplitPane.createTopBottom(
      myGrid2.getComponent(), detailsPanel, config.getOrCreateSubset("splitpane"), 0.7F);

    if(Aqua.isAqua()) {
      Aqua.makeLeopardStyleSplitPane(mySplitPane);
      tableTopPanel.setBorder(Aqua.MAC_BORDER_SOUTH);
      myDetailsTable.setBorder(Aqua.MAC_BORDER_SOUTH);
      ScrollBarPolicy.setDefaultWithHorizontal(myDetailsTable, ScrollBarPolicy.AS_NEEDED);
    }
  }

  private boolean showEditWorkForm(ActionContext context, EditWorkPeriodForm form, String title)
    throws CantPerformException
  {
    DialogResult<Boolean> result = DialogResult.create(context, "editWorkPeriod");
    result.setOkResult(true);
    result.setCancelResult(false);
    Boolean r = result.showModal(title, form.getComponent());
    return r != null && r;
  }

  private int findPreceding(AListModel<WorkPeriod> work, long time) {
    if (work == null || work.getSize() == 0)
      return -1;
    int bestIndex = -1;
    long bestDiff = Long.MAX_VALUE;
    for (int i = 0; i < work.getSize(); i++) {
      TaskTiming t = work.getAt(i).getTiming();
      if (t.getStarted() >= time) {
        break;
      }
      long diff = time - t.getStopped();
      if (diff >= 0 && diff < bestDiff) {
        bestDiff = diff;
        bestIndex = i;
      }
    }
    return bestIndex;
  }

  private long suggestTime(ActionContext context) {
    List<WorkPeriod> c = null;
    try {
      c = context.getSourceCollection(WorkPeriod.ROLE);
    } catch (CantPerformException e) {
      // ignore
    }
    if (c != null && !c.isEmpty()) {
      WorkPeriod last = c.get(c.size() - 1);
      return last.getTiming().getStopped();
    }
    Long selectionEnd = myGrid2.getSelectionEndTime();
    return selectionEnd != null ? selectionEnd : System.currentTimeMillis();
  }

  private ASortedTable<WorkPeriod> createTable() {
    final ASortedTable<WorkPeriod> table = new ASortedTable<>();
    table.setGridHidden();
    table.setStriped(true);

    final List<TableColumnAccessor<WorkPeriod, ?>> columns = Collections15.arrayList();

    columns.add(
      TableColumnBuilder.<WorkPeriod, WorkPeriod>create("include", "Publish")
        .setEditor(new Includer())
        .setRenderer(new Includer())
        .setConvertor(Convertor.identity())
        .setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(5))
        .createColumn());

    columns.add(new SimpleColumnAccessor<>(Local.parse(Terms.ref_Artifact_ID),
            new Renderers.DefaultCollectionRenderer<>((state, canvas, item) -> {
              prerender(canvas, item);
              canvas.appendText(myCustomizer.getItemKey(item.getArtifact()));
            }), new Comparator<WorkPeriod>() {
      final Comparator<ItemWrapper> keyComparator = myCustomizer.getArtifactByKeyComparator();

      public int compare(WorkPeriod o1, WorkPeriod o2) {
        return keyComparator.compare(o1.getArtifact(), o2.getArtifact());
      }
    }));

    columns.add(new SimpleColumnAccessor<>("Started",
            new Renderers.DefaultCollectionRenderer<>((state, canvas, item) -> {
              prerender(canvas, item);
              if (item != null)
                canvas.appendText(DateUtil.toLocalDateTime(new Date(item.getTiming().getStarted())));
            }), (o1, o2) -> Containers.compareLongs(o1.getTiming().getStarted(), o2.getTiming().getStarted())));

    columns.add(new SimpleColumnAccessor<>("Finished",
            new Renderers.DefaultCollectionRenderer<>((state, canvas, item) -> {
              prerender(canvas, item);
              if (item != null)
                canvas.appendText(DateUtil.toLocalDateTime(new Date(item.getTiming().getStopped())));
            }), (o1, o2) -> Containers.compareLongs(o1.getTiming().getStarted(), o2.getTiming().getStopped())));

    columns.add(new SimpleColumnAccessor<>("Hours Worked",
            new Renderers.DefaultCollectionRenderer<>((state, canvas, item) -> {
              prerender(canvas, item);
              if (item == null)
                return;
              int seconds = getDurationSeconds(item);
              canvas.appendText(DateUtil.getHoursDurationFixed(seconds));
            }), (o1, o2) -> {
              int s1 = getDurationSeconds(o1);
              int s2 = getDurationSeconds(o2);
              return Containers.compareInts(s1, s2);
            }));

    columns.add(new SimpleColumnAccessor<>("Comment",
            new Renderers.DefaultCollectionRenderer<>((state, canvas, item) -> {
              prerender(canvas, item);
              String c = item.getTiming().getComments();
              if (c != null)
                canvas.appendText(c);
            })));

    table.setColumnModel(FixedListModel.create(columns));

    final JComponent c = table.getSwingComponent();
    c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
      KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle");
    c.getActionMap().put("toggle", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        final List<WorkPeriod> items = myDetailsTable.getSelectionAccessor().getSelectedItems();
        for (final WorkPeriod item : items) {
          togglePulish(item);
        }
        myData.onWorkPeriodChanged();
        refreshData();
      }
    });

    return table;
  }

  private void togglePulish(WorkPeriod item) {
    item.setExcluded(!item.isExcluded());
    myData.setSpentDeltaNoUpdate(item.getArtifact(), null);
  }

  private static int getDurationSeconds(WorkPeriod item) {
    return item.getTiming().getLength();
  }

  private void prerender(Canvas canvas, WorkPeriod item) {
    if (item.isExcluded()) {
      canvas.setForeground(EXCLUDED_COLOR);
    }
  }

  public void init() {
    setupGroupings();
    myTasksSourceModel.clear();
    myTasksSourceModel.addAll(myData.getArtifacts());

    myGrid2.addSelectionChangeListener(this::updateTableFromSelection);
    updateTableFromSelection();
  }

  private void updateTableFromSelection() {
    int excluded = 0;
    List<WorkPeriod> selected = Collections15.arrayList();
    Set<LoadedItem> selectedItems = myGrid2.getSelectedArtifacts();
    Pair<Long, Long> range = myGrid2.getDateRange();
    for (WorkPeriod period : myData.getWorkList()) {
      if (!isPeriodAccepted(period, selectedItems, range))
        continue;
      selected.add(period);
      if (period.isExcluded())
        excluded++;
    }
    Collections.sort(selected);
    List<WorkPeriod> oldSelection = myDetailsTable.getSelectionAccessor().getSelectedItems();
    ModelUtils.syncModel(myDetailsModel, selected, Containers.comparablesComparator());
    if (!oldSelection.isEmpty())
      myDetailsTable.getSelectionAccessor().setSelected(oldSelection);

    StringBuilder b = new StringBuilder("Showing ");
    if (selected.size() == 0)
      b.append("details ");
    else
      b.append(selected.size()).append(" ").append(English.getSingularOrPlural("record", selected.size())).append(' ');
    b.append("for ");
    if (!myGrid2.hasSelection()) {
      b.append("all ").append(Terms.ref_artifacts);
    } else {
      TaskEntry entry = myGrid2.getSingleSelectedRow();
      if (entry != null) {
        if (entry instanceof ArtifactTaskEntry) {
          b.append(Terms.ref_artifact).append(" ").append(((ArtifactTaskEntry) entry).getKey());
        } else if (entry instanceof GroupTaskEntry) {
          b.append(((GroupTaskEntry) entry).getGroupValue() == TimeSheetGrid.TOTAL_KEY ? "all" : String.valueOf(entry));
          b.append(" ").append(Terms.ref_artifacts);
        }
      } else {
        b.append("multiple ").append(Terms.ref_artifacts);
      }
    }
    Pair<Long, Long> dateRange = myGrid2.getSelectedDateRange();
    if (dateRange != null) {
      TimeZone tz = TimeZone.getDefault();
      long from = DateUtil.toDayStart(dateRange.getFirst(), tz);
      long to = DateUtil.toDayStart(dateRange.getSecond() - Const.HOUR, tz);
      if (from == to)
        b.append(" on day ").append(DateUtil.LOCAL_DATE.format(new Date(from)));
      else
        b.append(" during ")
                .append(DateUtil.LOCAL_DATE.format(new Date(from)))
                .append("\u2014")
                .append(DateUtil.LOCAL_DATE.format(new Date(to)));
    }

    if (excluded > 0) {
      b.append(" (").append(excluded).append(" excluded)");
    }
    myDetailsLabel.setText(Local.parse(b.toString()));
  }

  private boolean isPeriodAccepted(WorkPeriod period, Set<LoadedItem> selectedItems, Pair<Long, Long> range) {
    if (myGrid2.hasSelection())
      return true;
    if (selectedItems == null || range == null)
      return false;
    if (!selectedItems.contains(period.getArtifact()))
      return false;
    TaskTiming timing = period.getTiming();
    return timing.getStarted() < range.getSecond() && timing.getStopped() > range.getFirst();
  }

  private void setupGroupings() {
    myGrid2.setGroupings(myCustomizer.getGroupingFunctions());
  }

  public JComponent getComponent() {
    return mySplitPane;
  }

  public void dispose() {
    myLife.detach();
  }

  private void refreshData() {
    myGrid2.refresh();
    updateTableFromSelection();
    int[] selection = myDetailsTable.getSelectionAccessor().getSelectedIndexes();
    if (selection.length > 0) {
      for (int index : selection) {
        myDetailsModel.forceUpdateAt(index);
      }
    }
    myDetailsTable.repaint();
  }

  private class EditWorkPeriodAction extends SimpleAction {
    EditWorkPeriodAction() {
      super("&Edit", Icons.ACTION_WORKLOG_EDIT);
      watchRole(WorkPeriod.ROLE);
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(WorkPeriod.ROLE);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      WorkPeriod period = context.getSourceObject(WorkPeriod.ROLE);
      EditWorkPeriodForm form = new EditWorkPeriodForm();
      form.setArtifacts(myTasksSourceModel, period.getArtifact());
      TaskTiming timing = period.getTiming();
      form.setDates(timing.getStarted(), timing.getStopped());
      form.setComments(timing.getComments());
      form.setCurrent(timing.isCurrent());

      boolean r = showEditWorkForm(context, form, "Edit Work Period");
      if (r) {
        LoadedItem a = form.getSelectedArtifact();
        long from = form.getFrom();
        long to = form.getTo();
        String comments = Util.NN(form.getComments());
        if (a != null && TimeSheetGrid.okForTiming(from, to)) {
          WorkPeriod replacement = new WorkPeriod(new TaskTiming(from, to, comments), a);
          if (period.isExcluded())
            replacement.setExcluded(true);
          myData.replace(period, replacement);
          refreshData();
        }
      }
    }
  }

  private class AddWorkPeriodAction extends EnabledAction {
    AddWorkPeriodAction() {
      super("&Add", Icons.ACTION_WORKLOG_ADD);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      long time = suggestTime(context);
      AListModel<WorkPeriod> work = myData.getWorkList();
      int index = findPreceding(work, time);
      long from = time;
      long to = time;
      LoadedItem a = null;
      if (index >= 0 && index < work.getSize()) {
        from = work.getAt(index).getTiming().getStopped();
        WorkPeriod p = work.getAt(index);
        a = p.getArtifact();
        if (index + 1 < work.getSize()) {
          to = work.getAt(index + 1).getTiming().getStopped();
        }
      }
      EditWorkPeriodForm form = new EditWorkPeriodForm();
      form.setArtifacts(myTasksSourceModel, a);
      form.setDates(from, to);
      boolean r = showEditWorkForm(context, form, "Add Work Period");
      if (r) {
        a = form.getSelectedArtifact();
        from = form.getFrom();
        to = form.getTo();
        String comments = Util.NN(form.getComments());
        if (a != null && TimeSheetGrid.okForTiming(from, to)) {
          myData.addNew(new WorkPeriod(new TaskTiming(from, to, comments), a));
          refreshData();
        }
      }
    }
  }

  private class DeleteWorkPeriodAction extends SimpleAction {
    DeleteWorkPeriodAction() {
      super("&Delete", Icons.ACTION_WORKLOG_DELETE);
      setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
      watchRole(WorkPeriod.ROLE);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      final List<WorkPeriod> periods = context.getSourceCollection(WorkPeriod.ROLE);
      boolean enabled = false;
      for(final WorkPeriod p : periods) {
        if(p.getTiming().isCurrent()) {
          enabled = false;
          break;
        }
        enabled = true;
      }
      context.setEnabled(enabled);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      final List<WorkPeriod> periods = context.getSourceCollection(WorkPeriod.ROLE);
      myData.deleteAll(periods);
      refreshData();
    }
  }
}
