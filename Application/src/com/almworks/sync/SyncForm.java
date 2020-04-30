package com.almworks.sync;

import com.almworks.api.application.viewer.JEditorPaneWrapper;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.ProgressComponentWrapper;
import com.almworks.api.engine.SyncProblem;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.*;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.ConfigAccessors;
import com.almworks.util.config.Configuration;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.LText;
import com.almworks.util.i18n.LText1;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.models.ColumnAccessor;
import com.almworks.util.models.DeafTableColumnAccessor;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicBorders;
import java.awt.*;
import java.util.List;
import java.util.*;

import static com.almworks.util.model.SetHolderUtils.fromChangeListener;

/**
 * :todoc:
 *
 * @author sereda
 */
class SyncForm implements UIComponentWrapper {
  private static final String PREFIX = "Application.SyncForm.";

  private static final LText TAB_SYNC_STATE = AppBook.text(PREFIX + "TAB_SYNC_STATE", "Synchronization &State");
  private static final LText TAB_PROBLEMS = AppBook.text(PREFIX + "TAB_PROBLEMS", "&Problems");

  private static final LText CHECKBOX_SHOW_WHEN_SYNC = AppBook.text(PREFIX + "CHECKBOX_SHOW_WHEN_SYNC",
    "Show this window when synchronization is requested");
  private static final LText TOOLTIP_CANCEL_ALL_SYNCHRONIZATION = AppBook.text(
    PREFIX + "TOOLTIP_CANCEL_ALL_SYNCHRONIZATION", "Cancel all synchronization");
  private static final LText TOOLTIP_CANCEL_ONE_SYNCHRONIZATION = AppBook.text(
    PREFIX + "TOOLTIP_CANCEL_ONE_SYNCHRONIZATION", "Cancel selected synchronization task");
  private static final LText TOOLTIP_RESOLVE_SELECTED_PROBLEM = AppBook.text(
    PREFIX + "TOOLTIP_RESOLVE_SELECTED_PROBLEM", "Resolve selected synchronization problem");
  private static final LText COLUMN_PROBLEM = AppBook.text(PREFIX + "COLUMN_PROBLEM", "Problem");
  private static final LText COLUMN_CONNECTION = AppBook.text(PREFIX + "COLUMN_CONNECTION", "Connection");
  private static final LText COLUMN_TIME_OCCURED = AppBook.text(PREFIX + "COLUMN_TIME_OCCURED", "Time Occurred");
  private static final LText1<Integer> PROBLEM_STATEMENT = AppBook.text(PREFIX + "PROBLEM_STATEMENT",
    "There {0,choice,0#are {0} problems|1#is one problem|1<are {0,number,integer} problems}", 0);

  private static final LText CANCELLED = AppBook.text(PREFIX + "CANCELLED", "Cancelled");
  private static final LText SYNCHRONIZED = AppBook.text(PREFIX + "SYNCHRONIZED", "Synchronized");
  private static final LText FAILED = AppBook.text(PREFIX + "FAILED", "Failed");
  private static final LText NEVER_HAPPENED = AppBook.text(PREFIX + "NEVER_HAPPENED", "Never Happened");
  private static final LText SUSPENDED = AppBook.text(PREFIX + "SUSPENDED", "Suspended");
  private static final LText WORKING = AppBook.text(PREFIX + "WORKING", "Working\u2026");

  public static final Condition<SyncProblem> RESOLVABLE_PROBLEM = new Condition<SyncProblem>() {
    public boolean isAccepted(SyncProblem problem) {
      return problem.isResolvable();
    }
  };
  private static final Convertor<SyncTask, Collection<SyncProblem>> PROBLEMS_EXTRACTOR = new Convertor<SyncTask, Collection<SyncProblem>>() {
    public Collection<SyncProblem> convert(SyncTask task) {
      return task.getProblems().copyCurrent();
    }
  };
  private static final Condition<SyncTask> CANCELLABLE_TASK = new Condition<SyncTask>() {
    public boolean isAccepted(SyncTask task) {
      return task.isCancellableState();
    }
  };

  private JPanel myWholePanel;
  private JTabbedPane myTabbedPane;

  private JSplitPane myProblemsSplitPane;
  private JPanel myProblemsPanel;
  private AToolbar myToolbar;
  private JScrollPane myTasksScrollpane;
  private AList<SyncTask> myTasksList;
  private ASortedTable<SyncProblem> myProblemsTable;

  private final ProblemForm myProblemForm;
  private final Configuration myConfiguration;
  private final OrderListModel<SyncProblem> myProblemsModel = OrderListModel.create();
  private final OrderListModel<TableColumnAccessor<SyncProblem, ?>> myProblemsTableColumns = OrderListModel.create();

  private final Lifecycle myTasksLife = new Lifecycle();
  private final Map<SyncTask, Detach> myTaskDetaches = Collections15.hashMap();
  private final DetachComposite myUIDetach = new DetachComposite();
  private static final int TASKS_TAB_INDEX = 0;
  private static final int PROBLEMS_TAB_INDEX = 1;
  private final JCheckBox myOpenSyncWindowCheckbox;

  public SyncForm(Configuration configuration, final ConfigAccessors.Bool showOnSync, TextDecoratorRegistry textDecoratorRegistry) {
    myConfiguration = configuration;
    myProblemForm = new ProblemForm(myUIDetach, textDecoratorRegistry);

    setupProblemColumns();

    myProblemsTable = new ASortedTable<SyncProblem>();
    myProblemsTable.setStriped(true);
    myProblemsTable.setGridHidden();
    myProblemsTable.setDataModel(myProblemsModel);
    myProblemsTable.setColumnModel(myProblemsTableColumns);
//    myProblemsTable.setColumnModel(myProblemsTableColumns, true);
    myProblemsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myProblemsTable.getSelectionAccessor().addAWTChangeListener(Lifespan.FOREVER, new ChangeListener() {
      public void onChange() {
        onProblemSelected(myProblemsTable.getSelectionAccessor().getSelection());
      }
    });
    ScrollBarPolicy.setDefaultWithHorizontal(myProblemsTable, ScrollBarPolicy.NEVER);

    myProblemsTable.setDataRoles(SyncProblem.DATA);
    DataProviderConvertingDecorator.decorate((JComponent) myProblemsTable.getViewport().getView(),
      SyncProblem.DATA, SyncTask.DATA, new Convertor<SyncProblem, Collection<SyncTask>>() {
        public Collection<SyncTask> convert(SyncProblem problem) {
          return Collections.<SyncTask>singletonList(problem.getConnectionSynchronizer());
        }
      });

    myProblemsSplitPane = ConfiguredSplitPane.createTopBottomJumping(
      myProblemsTable, myProblemForm.getComponent(), myConfiguration, 0.5F, myProblemsTable, true);

    // providing background to split panel
    myProblemsPanel = new JPanel(new BorderLayout(0, 0));
    myProblemsPanel.add(myProblemsSplitPane);

    myTasksList = new AList<SyncTask>();
    myTasksList.setDataRoles(SyncTask.DATA);
    DataProviderConvertingDecorator.decorate(myTasksList, SyncTask.DATA, SyncProblem.DATA, PROBLEMS_EXTRACTOR);
    myTasksList.setCellRenderer(new TaskRenderer());
    myTasksList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    myTasksScrollpane = new JScrollPane(myTasksList);
    ScrollBarPolicy.setDefaultWithHorizontal(myTasksScrollpane, ScrollBarPolicy.NEVER);

    if(Aqua.isAqua()) {
      myTasksScrollpane.setBorder(Aqua.MAC_BORDER_NORTH_SOUTH);
      myProblemsPanel.setBorder(Aqua.MAC_BORDER_NORTH_SOUTH);
    }
    Aqua.makeLeopardStyleSplitPane(myProblemsSplitPane);
    Aqua.cleanScrollPaneBorder(myProblemsTable);
    Aero.makeBorderedDividerSplitPane(myProblemsSplitPane);
    Aero.cleanScrollPaneBorder(myTasksScrollpane);
    Aero.cleanScrollPaneBorder(myProblemsTable);

    myTabbedPane = UIUtil.createTabbedPane(
      new String[]{ L.tabName(TAB_SYNC_STATE.format()), L.tabName(TAB_PROBLEMS.format()) },
      new JComponent[]{ myTasksScrollpane, myProblemsPanel },
      myConfiguration, "tab", 0);
    Aqua.makeBorderlessTabbedPane(myTabbedPane);
    Aero.makeBorderlessTabbedPane(myTabbedPane);

    TabbedPaneDataProvider.install(myTabbedPane).
      provide(0, myTasksList).
//      provide(1, myProblemsTable);
      provide(1, (JComponent) myProblemsTable.getViewport().getView());  // todo ASortedTable does not have providers

    buildToolbar();

    JPanel stateAndProgressPanel = new JPanel(new BorderLayout(0, 0));
    stateAndProgressPanel.add(myTabbedPane, BorderLayout.CENTER);
    stateAndProgressPanel.add(myToolbar, BorderLayout.NORTH);

    myWholePanel = stateAndProgressPanel;


    onProblemSelected(null);

    myOpenSyncWindowCheckbox = new JCheckBox(L.checkbox(CHECKBOX_SHOW_WHEN_SYNC.format()));
    myOpenSyncWindowCheckbox.setSelected(showOnSync.getBool());
    myOpenSyncWindowCheckbox.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        showOnSync.setBool(myOpenSyncWindowCheckbox.isSelected());
      }
    });
  }

  private void buildToolbar() {
    myToolbar = new AToolbar(myTabbedPane);
    // todo provide means for different action presentation

    myToolbar.addAction(new AnAbstractAction(null, Icons.ACTION_CANCEL_ALL_TASKS) {
      {
        setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
          L.tooltip(TOOLTIP_CANCEL_ALL_SYNCHRONIZATION.format()));

        setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.ESCAPE);
      }

      public void perform(ActionContext context) throws CantPerformException {
        cancelTasks(myTasksList.getCollectionModel().toList());
      }

      public void update(UpdateContext context) throws CantPerformException {
        super.update(context);
        context.watchRole(SyncTask.DATA);
        detectCancelApplicability(context, myTasksList.getCollectionModel().toList());
      }
    });

    myToolbar.addSeparator();

    myToolbar.addAction(new AnAbstractAction(null, Icons.ACTION_GENERIC_CANCEL_OR_REMOVE) {
      public void perform(ActionContext context) throws CantPerformException {
        cancelTasks(context.getSourceCollection(SyncTask.DATA));
      }

      public void update(UpdateContext context) throws CantPerformException {
        super.update(context);
        context.watchRole(SyncTask.DATA);
        context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION,
          L.tooltip(TOOLTIP_CANCEL_ONE_SYNCHRONIZATION.format()));
        detectCancelApplicability(context, context.getSourceCollection(SyncTask.DATA));
      }
    });
    myToolbar.addAction(new AnAbstractAction(null, Icons.ACTION_RESOLVE_PROBLEM) {
      public void perform(ActionContext context) throws CantPerformException {
        List<SyncProblem> problems = context.getSourceCollection(SyncProblem.DATA);
        for (Iterator<SyncProblem> ii = problems.iterator(); ii.hasNext();) {
          SyncProblem problem = ii.next();
          if (problem.isResolvable())
            problem.resolve(context);
        }
      }

      public void update(UpdateContext context) throws CantPerformException {
        super.update(context);
        context.watchRole(SyncProblem.DATA);
        context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION,
          L.tooltip(TOOLTIP_RESOLVE_SELECTED_PROBLEM.format()));
        List<SyncProblem> problems = context.getSourceCollection(SyncProblem.DATA);
        context.setEnabled(RESOLVABLE_PROBLEM.detect(problems) != null ? EnableState.ENABLED : EnableState.DISABLED);
      }
    });

    myToolbar.addSeparator();

//    myToolbar.addAction(new IdActionProxy.ForToolbar(MainMenu.Connection.SYNCHRONIZE_ALL));
    myToolbar.addAction(MainMenu.File.RELOAD_CONFIGURATION).overridePresentation(PresentationMapping.VISIBLE_NONAME);
//    myToolbar.addAction(new IdActionProxy.ForToolbar(MainMenu.Connection.SYNCHRONIZE_FULL));
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void dispose() {
    myTasksLife.cycle();
    myUIDetach.detach();
  }

  public void addProblems(Collection<SyncProblem> collection) {
    myProblemsModel.addAll(collection);
    myProblemsTable.getSelectionAccessor().ensureSelectionExists();
  }

  public void removeProblems(Collection<SyncProblem> collection) {
    myProblemsModel.removeAll(Condition.inCollection(collection));
    myProblemsTable.getSelectionAccessor().ensureSelectionExists();
  }

  public void revalidate() {
  }

  public void setTaskListModel(final AListModel<SyncTask> tasks) {
    myTasksLife.cycle();
    Lifespan lifespan = myTasksLife.lifespan();
    lifespan.add(myTasksList.setCollectionModel(tasks));
    lifespan.add(tasks.addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        for (int i = 0; i < length; i++)
          listenTask(tasks.getAt(index + i));
      }
    }));
    lifespan.add(tasks.addRemovedElementListener(new AListModel.RemovedElementsListener<SyncTask>() {
      public void onBeforeElementsRemoved(AListModel.RemoveNotice<SyncTask> syncTasks) {
        for (SyncTask syncTask : syncTasks.getList())
          Util.NN(myTaskDetaches.get(syncTask), Detach.NOTHING).detach();
      }
    }));
    for (int i = 0; i < tasks.getSize(); i++) {
      final SyncTask task = tasks.getAt(i);
      listenTask(task);
    }
  }

  private void listenTask(final SyncTask task) {
    DetachComposite detach = new DetachComposite();
    ChangeListener fUpdateTask = new ChangeListener() { @Override public void onChange() {
      updateTask(task);
    }};
    ProgressComponentWrapper progress = task.getProgressComponentWrapper();
    if (progress != null) {
      progress.addAWTChangeListener(detach, fUpdateTask);
    }
    task.getState().getEventSource().addAWTListener(detach, new ScalarModel.Adapter<SyncTask.State>() {
      @Override
      public void onScalarChanged(ScalarModelEvent<SyncTask.State> event) {
        updateTask(task);
      }
    });
    task.getProblems().addInitListener(detach, ThreadGate.AWT, fromChangeListener(fUpdateTask));
    myTasksLife.lifespan().add(detach);
    myTaskDetaches.put(task, detach);
  }

  private void updateTask(SyncTask task) {
    int index = myTasksList.getCollectionModel().detectIndex(Condition.isEqual(task));
    if (index >= 0) {
      Rectangle rect = myTasksList.getCellBounds(index);
      if (rect != null)
        myTasksList.repaint(rect);
    }
  }

  public void updateActions() {
    // kludge - dyoma review
    ((ListSelectionListener) myTasksList.getSelectionAccessor()).valueChanged(null);
  }

  private void detectCancelApplicability(UpdateContext context, List<SyncTask> selection) {
    context.setEnabled(CANCELLABLE_TASK.detect(selection) != null ? EnableState.ENABLED : EnableState.DISABLED);
  }

  private void cancelTasks(List<SyncTask> selection) {
    if (selection == null)
      return;
    for (Iterator<SyncTask> ii = selection.iterator(); ii.hasNext();) {
      SyncTask task = ii.next();
      if (task.isCancellableState())
        task.cancel();
    }
  }

  private void onProblemSelected(SyncProblem problem) {
    myProblemForm.setProblem(problem);
  }

  private void setupProblemColumns() {
    myProblemsTableColumns.addElement(
      new ProblemTableColumnAccessor(
        L.tableColumn(COLUMN_PROBLEM.format()),
        "Inconsistent Connectivity Exception Width",
        new Function<SyncProblem, String>() {
          final boolean mac = Aqua.isAqua();
          public String invoke(SyncProblem problem) {
            final String descr = problem.getShortDescription();
            return mac ? (" " + descr) : descr;
          }
        }));

    myProblemsTableColumns.addElement(
      new ProblemTableColumnAccessor(
        L.tableColumn(COLUMN_CONNECTION.format()),
        "My Server Connection", new Function<SyncProblem, String>() {
          public String invoke(SyncProblem problem) {
            return problem.getConnectionSynchronizer().getTaskName();
          }
        }));

    myProblemsTableColumns.addElement(
      new ProblemTableColumnAccessor(
        L.tableColumn(COLUMN_ARTIFACT().format()),
        "B199901",
        new Function<SyncProblem, String>() {
          public String invoke(SyncProblem problem) {
            if (!(problem instanceof ItemSyncProblem))
              return "---";
            return ((ItemSyncProblem) problem).getDisplayableId();
          }
        }));

    myProblemsTableColumns.addElement(
      new ProblemTableColumnAccessor(
        L.tableColumn(COLUMN_TIME_OCCURED.format()),
        "99:99:99 Time",
        new Function<SyncProblem, String>() {
          public String invoke(SyncProblem problem) {
            return DateUtil.toLocalDateOrTime(problem.getTimeHappened());
          }
        }));
  }

  private static LText COLUMN_ARTIFACT() {
    return AppBook.text(PREFIX + "COLUMN_CONNECTION", Local.text(Terms.key_Artifact));
  }

  public void focusOnProblem(SyncProblem problem) {
    ensureProblemsVisible();
    myProblemsTable.getSelectionAccessor().setSelected(problem);
  }

  private void ensureProblemsVisible() {
    myTabbedPane.setSelectedIndex(PROBLEMS_TAB_INDEX);
  }

  public JCheckBox getBottomLineComponent() {
    return myOpenSyncWindowCheckbox;
  }

  public void clearModels() {
    myProblemsModel.clear();
  }

  public void focusOnTasks() {
    ensureTasksVisible();
  }

  private void ensureTasksVisible() {
    myTabbedPane.setSelectedIndex(TASKS_TAB_INDEX);
  }

  private static final class TaskRenderer implements CollectionRenderer<SyncTask> {
    private final JLabel myTaskProblems;
    private final JLabel myDefaultTaskStateComponent;
    private final ALabel myTaskName;
    private final JPanel myPanel;

    private final Border myUnselectedUnfocusedBorder;
    private final Border myUnselectedFocusedBorder;
    private final Border mySelectedUnfocusedBorder;
    private final Border mySelectedFocusedBorder;

    private JComponent myLastTaskStateComponent = null;
    private final Color mySelectedBackground;
    private final Color myUnselectedBackground;
    private Color myForegroundColor;
    private Color myErrorColor;
    private Border myTaskNameNormalBorder;
    private Border myTaskNameErrorBorder;


    public TaskRenderer() {
      myTaskProblems = new JLabel();
      myDefaultTaskStateComponent = createDefaultTaskState();

      myForegroundColor = myTaskProblems.getForeground();

      myUnselectedBackground = Util.NN(UIManager.getColor("List.background"), Color.WHITE);
      mySelectedBackground = UIUtil.transformColor(myUnselectedBackground, 0.95F, 0.95F, 1.0F);
      myErrorColor = new Color(0x66, 0, 0);

      myUnselectedUnfocusedBorder = new EmptyBorder(5, 5, 10, 5);
      myUnselectedFocusedBorder = new CompoundBorder(
        new LineBorder(UIUtil.transformColor(mySelectedBackground.darker(), 1, 1, 1), 1),
        new EmptyBorder(4, 4, 9, 4));
      mySelectedUnfocusedBorder = myUnselectedUnfocusedBorder;
      mySelectedFocusedBorder = myUnselectedFocusedBorder;

      myTaskName = new ALabel();
      Font baseFont = myTaskName.getFont();
      myTaskName.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() * 1.15F));
      myTaskName.setForeground(myForegroundColor);
      myTaskNameNormalBorder = new BrokenLineBorder(myForegroundColor, 2, BrokenLineBorder.SOUTH);
      myTaskNameErrorBorder = new BrokenLineBorder(myErrorColor, 2, BrokenLineBorder.SOUTH);
      myTaskName.setBorder(myTaskNameNormalBorder);
      myTaskName.setAntialiased(true);

      myPanel = new JPanel(UIUtil.createBorderLayout());
      myPanel.add(myTaskName, BorderLayout.NORTH);
      myPanel.add(myDefaultTaskStateComponent, BorderLayout.CENTER);
      myPanel.add(myTaskProblems, BorderLayout.SOUTH);
      myPanel.setBorder(myUnselectedUnfocusedBorder);
      Dimension size = new Dimension(-1, UIUtil.getLineHeight(myTaskName) * 6 + 10);
      myPanel.setMinimumSize(size);
      myPanel.setPreferredSize(size);
      myPanel.setOpaque(true);

      myTaskProblems.setForeground(myErrorColor);
    }

    public JComponent getRendererComponent(CellState state, SyncTask task) {
      update(state, task);
      return myPanel;
    }

    private Color getBackground(boolean selected) {
      return selected ? mySelectedBackground : myUnselectedBackground;
    }

    private Border getBorder(boolean selected, boolean focused) {
      return selected ?
        (focused ? mySelectedFocusedBorder : mySelectedUnfocusedBorder) :
        (focused ? myUnselectedFocusedBorder : myUnselectedUnfocusedBorder);
    }

    private void update(CellState state, SyncTask task) {
      List<SyncProblem> taskProblems = task.getProblems().copyCurrent();
      int problemCount = taskProblems.size();
      boolean problemSet = false;
      if (problemCount == 0) {
        myTaskProblems.setText("");
        problemSet = true;
      } else if (problemCount == 1) {
        Iterator<SyncProblem> ii = taskProblems.iterator();
        if (ii.hasNext()) {
          myTaskProblems.setText(ii.next().getMediumDescription());
          problemSet = true;
        }
      }
      if (!problemSet)
        myTaskProblems.setText(L.content(PROBLEM_STATEMENT.format(problemCount)));

      UIComponentWrapper wrapper = task.getProgressComponentWrapper();
      JComponent component = null;
      if (wrapper != null)
        component = wrapper.getComponent();
      if (component == null) {
        SyncTask.State taskState = task.getState().getValue();
        String message = getTaskStateName(taskState);
        myDefaultTaskStateComponent.setText(message);
        component = myDefaultTaskStateComponent;
      }

      if (myLastTaskStateComponent != component) {
        if (myLastTaskStateComponent != null)
          myPanel.remove(myLastTaskStateComponent);
        myLastTaskStateComponent = component;
        myPanel.add(myLastTaskStateComponent, BorderLayout.CENTER);
      }
      myLastTaskStateComponent.setForeground(problemCount > 0 ? myErrorColor : myForegroundColor);


      myTaskName.setText(task.getTaskName());
      myTaskName.setForeground(problemCount > 0 ? myErrorColor : myForegroundColor);
      myTaskName.setBorder(problemCount > 0 ? myTaskNameErrorBorder : myTaskNameNormalBorder);

      myPanel.setBackground(getBackground(state.isSelected()));
      myPanel.setBorder(getBorder(state.isSelected(), state.isFocused()));
    }

    private String getTaskStateName(SyncTask.State state) {
      if (state == null)
        return "";
      else if (state == SyncTask.State.CANCELLED)
        return CANCELLED.format();
      else if (state == SyncTask.State.DONE)
        return SYNCHRONIZED.format();
      else if (state == SyncTask.State.FAILED)
        return FAILED.format();
      else if (state == SyncTask.State.NEVER_HAPPENED)
        return NEVER_HAPPENED.format();
      else if (state == SyncTask.State.SUSPENDED)
        return SUSPENDED.format();
      else if (state == SyncTask.State.WORKING)
        return WORKING.format();
      else {
        assert false : state;
        return English.humanizeEnumerable(state);
      }
    }

    private static JLabel createDefaultTaskState() {
      ALabel label = new ALabel(WORKING.format());
      label.setBorder(new EmptyBorder(2, 0, 5, 5));
      label.setVerticalAlignment(JLabel.TOP);
      UIUtil.adjustFont(label, 1.5F, -1, true);
      return label;
    }
  }

  private static class ProblemTableColumnAccessor extends DeafTableColumnAccessor<SyncProblem, String> {
    private final String myPrototypeValue;
    private final Function<SyncProblem, String> myValueGetter;

    public ProblemTableColumnAccessor(String name, String prototypeValue, final Function<SyncProblem, String> valueGetter) {
      super(name,
        new Renderers.DefaultCollectionRenderer(
          new CanvasRenderer<SyncProblem>() {
            public void renderStateOn(CellState state, Canvas canvas, SyncProblem item) {
              canvas.appendText(valueGetter.invoke(item));
            }
          }),
        String.CASE_INSENSITIVE_ORDER);
      assert prototypeValue != null;
      myPrototypeValue = prototypeValue;
      myValueGetter = valueGetter;
    }

    public int getPreferredWidth(JTable table, ATableModel<SyncProblem> tableModel, ColumnAccessor<SyncProblem> renderingAccessor,
      int columnIndex) {
      int maxAdvance = table.getFontMetrics(table.getFont()).getMaxAdvance();
      return maxAdvance * (myPrototypeValue.length() + 3);
    }

    public String getValue(SyncProblem object) {
      return myValueGetter.invoke(object);
    }
  }

  static class ProblemForm implements ElementVisitor<JLabel> {
    private ScrollablePanel myContainerPanel;
    private JPanel myWholePanel;
    private JComponent myProblemDescription;
    private JTextField myProblemConnection;
    private ALabel myProblemTitle;
    private JTextField myProblemTimeOccurred;
    private JTextField myProblemArtifact;
    private JScrollPane myDescriptionScrollPane;
    private JScrollPane myScrollpane;
    private JLabel myArtifactLabel;
    private JLabel myDescriptionLabel;
    private JEditorPaneWrapper myProblemDescriptionWrapper;

    public ProblemForm(DetachComposite uiDetach, TextDecoratorRegistry textDecoratorRegistry) {
      LogHelper.assertError(textDecoratorRegistry != null, "ProblemForm.textDecoratorRegistry");
      getTextDecoratorRegistry().setDelegate(textDecoratorRegistry);
      AppBook.replaceText(PREFIX + "ProblemPanel.", myWholePanel);

      myArtifactLabel.setText(Local.text(Terms.ref_Artifact));
      myWholePanel.setBorder(UIUtil.EDITOR_PANEL_BORDER);
      UIUtil.adjustFont(myProblemTitle, 1.2F, Font.BOLD, true);
      myProblemTitle.setBorder(UIUtil.createSouthBevel(myWholePanel.getBackground()));
      myProblemTitle.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
      new DocumentFormAugmentor().augmentForm(uiDetach, myWholePanel, true);

      // Hack to "baseline-align"
      myDescriptionLabel.setBorder(new EmptyBorder(myDescriptionScrollPane.getInsets().top + myProblemDescription.getInsets().top, 0, 0, 0));

      myContainerPanel = new ScrollablePanel(myWholePanel) {
        public void setVisible(boolean visible) {
          super.setVisible(visible);
          if (visible) {
            invalidate();
            validate();
          }
        }

        public Dimension getPreferredSize() {
          Dimension preferredSize = super.getPreferredSize();
          int height = myWholePanel.getPreferredSize().height;
          // adjust for scrollpane
          height -= myDescriptionScrollPane.getPreferredSize().height;
          height += myProblemDescription.getPreferredSize().height;
          height += 30; // todo fix magic number
          preferredSize = new Dimension(preferredSize.width, height);
          return preferredSize;
        }
      };
      myContainerPanel.setOpaque(true);
      myContainerPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

      myScrollpane = new JScrollPane(myContainerPanel);
      Aqua.cleanScrollPaneBorder(myScrollpane);
      Aero.cleanScrollPaneBorder(myScrollpane);
      UIUtil.setDefaultLabelAlignment(myWholePanel);
      UIUtil.visitComponents(myWholePanel, JLabel.class, this);
    }

    private TextDecoratorRegistry.Delegating myTextDecoratorRegistry;
    private TextDecoratorRegistry.Delegating getTextDecoratorRegistry() {
      if (myTextDecoratorRegistry == null) myTextDecoratorRegistry = new TextDecoratorRegistry.Delegating();
      return myTextDecoratorRegistry;
    }

    public JComponent getComponent() {
      return myScrollpane;
    }

    public void setProblem(SyncProblem problem) {
      if (problem != null) {
        myProblemTimeOccurred.setText(DateUtil.toLocalDateOrTime(problem.getTimeHappened()));
        myProblemConnection.setText(problem.getConnectionSynchronizer().getTaskName());
        myProblemDescriptionWrapper.setText(Util.NN(problem.getLongDescription()));
        myProblemTitle.setText(problem.getShortDescription());
        myProblemArtifact.setText(
          problem instanceof ItemSyncProblem ? ((ItemSyncProblem) problem).getDisplayableId() : "");
      } else {
        myProblemTimeOccurred.setText("");
        myProblemConnection.setText("");
        myProblemDescriptionWrapper.setText("");
        myProblemTitle.setText("");
        myProblemArtifact.setText("");
      }
    }

    public boolean isVisible() {
      return myContainerPanel.isVisible();
    }

    public boolean visit(JLabel element) {
      if(!(element instanceof ALabel)) {
        element.setText(element.getText() + ":");
      }
      return true;
    }

    private void createUIComponents() {
      createProblemDescription();
    }

    private void createProblemDescription() {
      myProblemDescriptionWrapper = JEditorPaneWrapper.decoratedViewer(getTextDecoratorRegistry());
      myProblemDescription = myProblemDescriptionWrapper.getComponent();
      if (myProblemDescription instanceof JEditorPane) {
        JEditorPane editor = (JEditorPane) myProblemDescription;
        editor.setMargin(new Insets(3, 3, 3, 3));
        editor.setBorder(new BasicBorders.MarginBorder());
      }
    }
  }
}
