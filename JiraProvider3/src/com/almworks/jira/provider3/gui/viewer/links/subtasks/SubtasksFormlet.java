package com.almworks.jira.provider3.gui.viewer.links.subtasks;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.UIController;
import com.almworks.engine.gui.AbstractFormlet;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.ViewSubtasksAction;
import com.almworks.jira.provider3.gui.actions.ViewWholeFamilyAction;
import com.almworks.util.Env;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.ATable;
import com.almworks.util.components.ColumnTooltipProvider;
import com.almworks.util.components.SizeCalculator1D;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.components.layout.WidthDrivenComponentAdapter;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.Configuration;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class SubtasksFormlet extends AbstractFormlet implements UIController<ATable<LoadedIssue>> {
  public static final DataRole<LoadedIssue> SUBTASKS = DataRole.createRole(LoadedIssue.class, "Subtasks");

  private static final TableColumnAccessor<LoadedIssue, ?> KEY = buildKey().createColumn();
  private static final TableColumnAccessor<LoadedIssue, ?> SUMMARY = buildSummary().createColumn();

  private final WidthDrivenComponentAdapter myContent;
  private final GuiFeaturesManager myFeatures;
  private final boolean myParentTask;
  private final List<ToolbarEntry> myActions = Collections15.arrayList();

  private boolean myVisible;
  private String myLastCaption;

  public SubtasksFormlet(GuiFeaturesManager features, Configuration config, boolean parentTask) {
    super(config);
    myFeatures = features;
    myParentTask = parentTask;
    ATable<LoadedIssue> table = ATable.create();
    table.setColumnModel(createColumnsModel(features));
    table.setGridHidden();
    if (!parentTask) table.setDataRoles(SUBTASKS);
    table.setStriped(true);
    myContent = new WidthDrivenComponentAdapter(table);
    CONTROLLER.putClientValue(table, this);

    AnAction viewAllAction = parentTask ? ViewWholeFamilyAction.ACTION : ViewSubtasksAction.ALL;
    myActions.add(new ActionToolbarEntry(viewAllAction, table, PresentationMapping.NONAME));
    if (!parentTask)
      new MenuBuilder().addDefaultAction(ViewSubtasksAction.TRY_SELECTED).addToComponent(Lifespan.FOREVER, table.getSwingComponent());
  }

  private static AListModel<TableColumnAccessor<LoadedIssue, ?>> createColumnsModel(GuiFeaturesManager features) {
    TableColumnAccessor<LoadedIssue, Icon> TYPE = buildType(features).createColumn();
    TableColumnAccessor<LoadedIssue, ItemKey> STATUS = buildStatus(features).createColumn();
    TableColumnAccessor<LoadedIssue, ItemKey> ASSIGNED_TO = buildAssignedTo(features).createColumn();
    return FixedListModel.create(TYPE, KEY, SUMMARY, STATUS, ASSIGNED_TO);
  }

  public boolean isVisible() {
    return myVisible;
  }

  @NotNull
  public WidthDrivenComponent getContent() {
    return myContent;
  }

  public void connectUI(@NotNull final Lifespan lifespan, @NotNull final ModelMap model, @NotNull final ATable<LoadedIssue> component) {
    if (lifespan.isEnded())
      return;
    ChangeListener listener = new ChangeListener() {
      public void onChange() {
        List<LoadedIssue> links = selectIssues(model);
        myVisible = !links.isEmpty();
        if (myVisible) {
          component.setDataModel(FixedListModel.create(links));
          myLastCaption = createCaption(links);
        }
        fireFormletChanged();
      }
    };
    model.addAWTChangeListener(lifespan, listener);
    listener.onChange();
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        component.setDataModel(AListModel.EMPTY);
      }
    });
  }

  private String createCaption(List<LoadedIssue> issues) {
    Map<String, Integer> map = Collections15.linkedHashMap();
    for (LoadedIssue issue : issues) {
      ItemKey key = issue.getStatusKey(myFeatures);
      if (key != null) {
        String status = key.getDisplayName();
        Integer k = map.get(status);
        k = k == null ? 1 : k + 1;
        map.put(status, k);
      }
    }
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, Integer> e : map.entrySet()) {
      if (builder.length() > 0)
        builder.append(", ");
      builder.append(e.getKey()).append(": ").append(e.getValue());
    }
    return builder.toString();
  }

  @Nullable
  public String getCaption() {
    return isCollapsed() ? myLastCaption : null;
  }

  @Nullable
  public List<? extends ToolbarEntry> getActions() {
    return isCollapsed() ? null : myActions;
  }

  private List<LoadedIssue> selectIssues(ModelMap model) {
    if (myParentTask) {
      LoadedIssue parent = MetaSchema.parentTask(myFeatures).getValue(model);
      return parent != null ? Collections.singletonList(parent) : Collections.<LoadedIssue>emptyList();
    }
    List<LoadedIssue> subtasks = MetaSchema.subtasks(myFeatures).getValue(model);
    if (subtasks != null && !subtasks.isEmpty()) {
      subtasks = Collections15.arrayList(subtasks);
      Collections.sort(subtasks, LoadedIssue.BY_KEY);
    } else subtasks = Collections.emptyList();
    return subtasks;
  }

  private static TableColumnBuilder<LoadedIssue, String> buildKey() {
    final boolean mac = Env.isMac();
    TableColumnBuilder<LoadedIssue, String> builder = TableColumnBuilder.create("key", "Key");
    builder.setConvertor(new Convertor<LoadedIssue, String>() {
      public String convert(LoadedIssue value) {
        String str = value.getKey();
        //noinspection ConstantConditions
        return str != null ? (mac ? ("  " + str) : str) : "";
      }
    });
    builder.setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(10));
    builder.setValueCanvasRenderer(Renderers.canvasToString(""));
    return builder;
  }
  
  private static TableColumnBuilder<LoadedIssue, String> buildSummary() {
    TableColumnBuilder<LoadedIssue, String> summary = TableColumnBuilder.create("summary", "Summary");
    summary.setConvertor(new Convertor<LoadedIssue, String>() {
      public String convert(LoadedIssue value) {
        return Util.NN(value.getSummary());
      }
    });
    summary.setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(30));
    summary.setValueCanvasRenderer(Renderers.canvasToString(""));
    return summary;
  }

  private static TableColumnBuilder<LoadedIssue, Icon> buildType(final GuiFeaturesManager features) {
    TableColumnBuilder<LoadedIssue, Icon> builder = TableColumnBuilder.create("issueType", "Issue Type");
    builder.setConvertor(new Convertor<LoadedIssue, Icon>() {
      public Icon convert(LoadedIssue value) {
        ItemKey type = value.getTypeKey(features);
        return type != null ? type.getIcon() : null;
      }
    });
    builder.setValueCanvasRenderer(Renderers.iconRenderer());
    builder.setSizePolicy(ColumnSizePolicy.Calculated.fixedPixels(20));
    builder.setTooltipProvider(new ColumnTooltipProvider<LoadedIssue>() {
      @Override
      public String getTooltip(CellState cellState, LoadedIssue element, Point cellPoint, Rectangle cellRect) {
        ItemKey type = element.getTypeKey(features);
        return type != null ? "Type: " + type.getDisplayName() : null;
      }
    });
    return builder;
  }

  private static TableColumnBuilder<LoadedIssue, ItemKey> buildStatus(final GuiFeaturesManager features) {
    TableColumnBuilder<LoadedIssue, ItemKey> builder = TableColumnBuilder.create("status", "Status");
    builder.setConvertor(new Convertor<LoadedIssue, ItemKey>() {
      public ItemKey convert(LoadedIssue value) {
        //noinspection ConstantConditions
        return value.getStatusKey(features);
      }
    });
    builder.setValueCanvasRenderer(ItemKey.ICON_NAME_RENDERER);
    builder.setSizePolicy(ColumnSizePolicy.Calculated.free(
      SizeCalculator1D.sum(5, SizeCalculator1D.fixedPixels(20), SizeCalculator1D.letterMWidth(10))));
    return builder;
  }

  private static TableColumnBuilder<LoadedIssue, ItemKey> buildAssignedTo(final GuiFeaturesManager features) {
    TableColumnBuilder<LoadedIssue, ItemKey> builder = TableColumnBuilder.create("assingedTo", "Assigned to");
    builder.setConvertor(new Convertor<LoadedIssue, ItemKey>() {
      public ItemKey convert(LoadedIssue value) {
        return value.getAssigneeKey(features);
      }
    });
    builder.setValueCanvasRenderer(Renderers.canvasDefault(""));
    builder.setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(15));
    return builder;
  }
}
