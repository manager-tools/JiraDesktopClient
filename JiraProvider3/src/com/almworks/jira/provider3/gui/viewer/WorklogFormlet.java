package com.almworks.jira.provider3.gui.viewer;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.UIController;
import com.almworks.engine.gui.AbstractFormlet;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.SyncState;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklog;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.ASortedTable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.components.layout.WidthDrivenComponentAdapter;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.model.ModelUtils;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.actions.ActionToolbarEntry;
import com.almworks.util.ui.actions.PresentationMapping;
import com.almworks.util.ui.actions.ToolbarEntry;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class WorklogFormlet extends AbstractFormlet {
  private static final TableColumnAccessor<LoadedWorklog, LoadedWorklog> WHEN = buildWhen().createColumn();
  private static final TableColumnAccessor<LoadedWorklog, LoadedWorklog> WHO = buildWho().createColumn();
  private static final TableColumnAccessor<LoadedWorklog, LoadedWorklog> DURATION = buildDuration().createColumn();
  private static final TableColumnAccessor<LoadedWorklog, LoadedWorklog> VISIBILITY = buildVisibility().createColumn();
  private static final TableColumnAccessor<LoadedWorklog, LoadedWorklog> COMMENT = buildComment().createColumn();
  private static final int MAX_COMMENT_LENGTH = 80;

  private final WidthDrivenComponentAdapter myContent;
  private final ASortedTable<LoadedWorklog> myTable;
  private final ModelKey<List<LoadedWorklog>> myModelKey;
  private final Actions myActions = new Actions();

  private boolean myVisible;
  private String myLastCaption;

  public WorklogFormlet(Configuration configuration, GuiFeaturesManager guiFeaturesManager) {
    super(configuration);
    myModelKey = (ModelKey<List<LoadedWorklog>>) guiFeaturesManager.findModelKey(MetaSchema.KEY_WORKLOG_LIST);

    myTable = new ASortedTable<LoadedWorklog>();
    AListModel<TableColumnAccessor<LoadedWorklog, ?>> columns =
      FixedListModel.<TableColumnAccessor<LoadedWorklog, ?>>create(WHEN, WHO, DURATION, VISIBILITY, COMMENT);
    myTable.setColumnModel(columns);
    myTable.setStriped(true);
    myTable.setGridHidden();
    myTable.setBorder(null);
    myTable.addGlobalRoles(LoadedWorklog.WORKLOG);
    myTable.setScrollableMode(false);
/*    myTable.getTable().addDoubleClickListener(Lifespan.FOREVER, new CollectionCommandListener<LoadedWorklog>() {
      public void onCollectionCommand(ACollectionComponent<LoadedWorklog> loadedWorklogACollectionComponent, int index,
        LoadedWorklog element)
      {
        myTable.getSelectionAccessor().setSelected(element);
        ActionUtil.performAction(JiraActions.LOG_WORK_EDIT, myTable.getSwingComponent());
      }
    });
    myTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteWorklog");
    myTable.getActionMap().put("deleteWorklog", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ActionUtil.performAction(JiraActions.LOG_WORK_DELETE, myTable.getSwingComponent());
      }
    });
*/

    myActions.add(JiraActions.LOG_WORK);
    myActions.addDefault(JiraActions.LOG_WORK_EDIT);
    myActions.add(JiraActions.LOG_WORK_DELETE);
    myActions.add(JiraActions.LOG_WORK_ROLLBACK);

    if(Aqua.isAqua()) {
      final JPanel jPanel = new JPanel(new BorderLayout());
      jPanel.add(myTable, BorderLayout.CENTER);
      jPanel.setBorder(Aqua.MAC_LIGHT_BORDER_NORTH);
      myContent = new WidthDrivenComponentAdapter(jPanel);
      final JTable jTable = (JTable) myTable.getSwingComponent();
      DocumentFormAugmentor.DO_NOT_AUGMENT.putClientValue(jTable.getTableHeader(), true);
    } else {
      myContent = new WidthDrivenComponentAdapter(myTable);
    }
    myActions.addToComponent(myTable.getSwingComponent());

    UIController.CONTROLLER.putClientValue(myTable, new UIController<ASortedTable>() {
      public void connectUI(@NotNull Lifespan lifespan, @NotNull ModelMap model, @NotNull ASortedTable component) {
        assert component == myTable;
        connectTable(lifespan, model);
      }
    });
  }

  private void connectTable(Lifespan life, final ModelMap mm) {
    if (life.isEnded())
      return;
    final OrderListModel<LoadedWorklog> model = OrderListModel.create();
    ChangeListener listener = new ChangeListener() {
      public void onChange() {
        List<LoadedWorklog> worklogs = myModelKey.getValue(mm);
        myVisible = worklogs != null && !worklogs.isEmpty();
        if (myVisible) {
          resyncModel(model, worklogs);
          myLastCaption = createCaption(worklogs);
        } else {
          model.clear();
        }
        fireFormletChanged();
      }
    };
    mm.addAWTChangeListener(life, listener);
    listener.onChange();
    myTable.setDataModel(model);
    life.add(new Detach() {
      protected void doDetach() throws Exception {
        myTable.setDataModel(AListModel.EMPTY);
      }
    });
  }

  private String createCaption(List<LoadedWorklog> worklogs) {
    return worklogs.size() + " work log entries";
  }

  private void resyncModel(OrderListModel<LoadedWorklog> model, List<LoadedWorklog> worklogs) {
    SelectionAccessor<LoadedWorklog> accessor = myTable.getSelectionAccessor();
    List<LoadedWorklog> oldSelection = accessor.getSelectedItems();
    
    List<LoadedWorklog> list = Collections15.arrayList(worklogs);
    Collections.sort(list, LoadedWorklog.BY_STARTED);
    ModelUtils.syncModel(model, list, LoadedWorklog.BY_STARTED);

    if (!oldSelection.isEmpty()) {
      List<LoadedWorklog> newSelection = Collections15.arrayList(oldSelection.size());
      for (LoadedWorklog w : oldSelection) {
        for (LoadedWorklog a : model) {
          if (w != null && a != null && w.getItem() == a.getItem()) {
            newSelection.add(a);
            break;
          }
        }
      }
      accessor.setSelected(newSelection);
      accessor.ensureSelectionExists();
    }
  }

  public boolean isVisible() {
    return myVisible;
  }

  public List<? extends ToolbarEntry> getActions() {
    return isCollapsed() ? null : myActions.getActions();
  }

  @NotNull
  public WidthDrivenComponent getContent() {
    return myContent;
  }

  private static TableColumnBuilder<LoadedWorklog, LoadedWorklog> buildWho() {
    TableColumnBuilder<LoadedWorklog, LoadedWorklog> column = TableColumnBuilder.create("who", "Who");
    column.setConvertor(Convertor.<LoadedWorklog>identity());
    column.setValueCanvasRenderer(new CanvasRenderer<LoadedWorklog>() {
      public void renderStateOn(CellState state, Canvas canvas, LoadedWorklog item) {
        if (item == null)
          return;
        presetCanvas(item, canvas, state);
        canvas.appendText(item.getWhoText());
      }
    });
    column.setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(15));
    column.setComparator(new Comparator<LoadedWorklog>() {
      public int compare(LoadedWorklog o1, LoadedWorklog o2) {
        return ItemKey.COMPARATOR.compare(o1.getWho(), o2.getWho());
      }
    });
    return column;
  }

  private static TableColumnBuilder<LoadedWorklog, LoadedWorklog> buildWhen() {
    TableColumnBuilder<LoadedWorklog, LoadedWorklog> column = TableColumnBuilder.create("when", "Started");
    column.setConvertor(Convertor.<LoadedWorklog>identity());
    column.setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(10));
    column.setValueCanvasRenderer(new CanvasRenderer<LoadedWorklog>() {
      public void renderStateOn(CellState state, Canvas canvas, LoadedWorklog item) {
        if (item == null)
          return;
        presetCanvas(item, canvas, state);
        Date started = item.getStarted();
        if (started != null && started.getTime() > Const.DAY) {
          canvas.appendText(DateUtil.toLocalDateTime(started));
        }
      }
    });
    column.setComparator(LoadedWorklog.BY_STARTED);
    return column;
  }

  private static TableColumnBuilder<LoadedWorklog, LoadedWorklog> buildDuration() {
    TableColumnBuilder<LoadedWorklog, LoadedWorklog> column = TableColumnBuilder.create("duration", "Duration");
    column.setConvertor(Convertor.<LoadedWorklog>identity());
    column.setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(8));
    column.setValueCanvasRenderer(new CanvasRenderer<LoadedWorklog>() {
      public void renderStateOn(CellState state, Canvas canvas, LoadedWorklog item) {
        if (item == null)
          return;
        presetCanvas(item, canvas, state);
        int duration = item.getDurationSeconds();
        if (duration >= 0) {
          canvas.appendText(DateUtil.getFriendlyDuration(duration, true));
        }
      }
    });
    column.setComparator(Containers.reverse(new Comparator<LoadedWorklog>() {
      public int compare(LoadedWorklog o1, LoadedWorklog o2) {
        return Containers.compareInts(o1.getDurationSeconds(), o2.getDurationSeconds());
      }
    }));
    return column;
  }

  private static TableColumnBuilder<LoadedWorklog, LoadedWorklog> buildVisibility() {
    TableColumnBuilder<LoadedWorklog, LoadedWorklog> column = TableColumnBuilder.create("visibility", "Visibility");
    column.setConvertor(Convertor.<LoadedWorklog>identity());
    column.setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(15));
    column.setValueCanvasRenderer(new CanvasRenderer<LoadedWorklog>() {
      public void renderStateOn(CellState state, Canvas canvas, LoadedWorklog item) {
        if (item == null)
          return;
        presetCanvas(item, canvas, state);
        canvas.appendText(item.getSecurityText());
      }
    });
    column.setComparator(new Comparator<LoadedWorklog>() {
      public int compare(LoadedWorklog o1, LoadedWorklog o2) {
        return ItemKey.COMPARATOR.compare(o1.getVisibility(), o2.getVisibility());
      }
    });
    return column;
  }

  private static TableColumnBuilder<LoadedWorklog, LoadedWorklog> buildComment() {
    TableColumnBuilder<LoadedWorklog, LoadedWorklog> column = TableColumnBuilder.create("comment", "Comment");
    column.setConvertor(Convertor.<LoadedWorklog>identity());
    column.setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(15));
    column.setValueCanvasRenderer(new CanvasRenderer<LoadedWorklog>() {
      public void renderStateOn(CellState state, Canvas canvas, LoadedWorklog item) {
        if (item == null)
          return;
        presetCanvas(item, canvas, state);
        String comment = item.getComment();
        if (comment.length() > MAX_COMMENT_LENGTH)
          comment = comment.substring(0, MAX_COMMENT_LENGTH) + "\u2026";
        canvas.appendText(comment);
      }
    });
    return column;
  }

  private static void presetCanvas(LoadedWorklog item, Canvas canvas, CellState cell) {
    SyncState state = item.getSyncState();
    if (state == SyncState.NEW || state == SyncState.EDITED) {
      canvas.setFontStyle(Font.BOLD);
    } else if (state == SyncState.LOCAL_DELETE) {
      canvas.setForeground(ColorUtil.between(cell.getDefaultBackground(), cell.getDefaultForeground(), 0.6F));
    }
  }

  @Nullable
  public String getCaption() {
    return isCollapsed() ? myLastCaption : null;
  }

  private static class Actions {
    private final List<ToolbarEntry> myActions = Collections15.arrayList();
    private final MenuBuilder myMenu = new MenuBuilder();

    public void add(String actionId) {
      myActions.add(ActionToolbarEntry.create(actionId, PresentationMapping.NONAME));
      myMenu.addAction(actionId);
    }

    public void addDefault(String actionId) {
      myActions.add(ActionToolbarEntry.create(actionId, PresentationMapping.NONAME));
      myMenu.addDefaultAction(actionId);
    }

    public void addToComponent(JComponent component) {
      myMenu.addToComponent(Lifespan.FOREVER, component);
    }

    public List<? extends ToolbarEntry> getActions() {
      return myActions;
    }
  }
}
