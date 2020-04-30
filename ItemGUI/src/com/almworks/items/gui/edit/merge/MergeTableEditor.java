package com.almworks.items.gui.edit.merge;

import com.almworks.api.gui.MainMenu;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Convertors;
import com.almworks.util.components.ATable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.EmptyIcon;
import com.almworks.util.ui.RowIcon;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.IdActionProxy;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MergeTableEditor {
  public static final DataRole<MergeTableEditor> ROLE = DataRole.createRole(MergeTableEditor.class);

  private final List<MergeValue> myValues;
  private final MenuBuilder myMenuBuilder = new MenuBuilder();

  private MergeTableEditor(List<MergeValue> values) {
    myValues = values;
    myMenuBuilder.addAction(new IdActionProxy(MainMenu.Merge.RESOLVE_LOCAL));
    myMenuBuilder.addAction(new IdActionProxy(MainMenu.Merge.RESOLVE_REMOTE));
    myMenuBuilder.addAction(new IdActionProxy(MainMenu.Merge.RESOLVE_BASE));
    myMenuBuilder.addAction(new IdActionProxy(MainMenu.Merge.RESOLVE_IGNORE));
  }

  public static MergeTableEditor prepare(Collection<MergeValue> values) {
    List<MergeValue> list = Collections15.arrayList();
    if (values != null) list.addAll(values);
    Collections.sort(list, Containers.convertingComparator(MergeValue.GET_DISPLAY_NAME, String.CASE_INSENSITIVE_ORDER));
    return new MergeTableEditor(list);
  }

  public void notifyModel(Lifespan life, EditItemModel model) {
    for (MergeValue value : myValues) value.addChangeListener(life, model);
  }

  public MenuBuilder getMenuBuilder() {
    return myMenuBuilder;
  }

  @NotNull
  public List<MergeValue> getMergeValues() {
    return Collections15.arrayList(myValues);
  }

  public JScrollPane createTable(Lifespan life, Configuration config) {
    List<MergeValue> rowList = myValues;
    ATable<MergeValue> table = ATable.create();
    ConstProvider.addGlobalValue(table.getSwingComponent(), ROLE, this);
    table.setColumnModel(
      FixedListModel.create(COLUMN_FIELD_NAME, createValueColumn(MergeValue.LOCAL, "Local"), createStateColumn(false),
        createValueColumn(MergeValue.BASE, "Base"), createStateColumn(true),
        createValueColumn(MergeValue.REMOTE, "Remote")));
    MergeControlImpl.install(life, table, rowList, config);

    table.setGridHidden();
    table.setStriped(true);
    table.getSwingHeader().setReorderingAllowed(false);
    final JTable jTable = ((JTable) table.getSwingComponent());
    final Dimension intercellSpacing = jTable.getIntercellSpacing();
    intercellSpacing.width = 0;
    jTable.setIntercellSpacing(intercellSpacing);
    table.getSelectionAccessor().ensureSelectionExists();
    UIUtil.keepSelectionOnRemove(table);
    myMenuBuilder.addToComponent(life, jTable);
    return table.wrapWithScrollPane();
  }

  private static TableColumnAccessor<MergeValue, Object> createValueColumn(final int index,
    String id) {
    return new TableColumnBuilder<MergeValue, Object>().setConvertor(new Convertor<MergeValue, Object>() {
      @Override
      public Object convert(MergeValue value) {
        if (value == null) return null;
        return value.getValue(index);
      }
    }).setId(id).setCanvasRenderer(new CanvasRenderer<MergeValue>() {
      @Override
      public void renderStateOn(CellState state, Canvas canvas, MergeValue value) {
        if (value == null) return;
        value.render(state, canvas, index);
      }
    }).createColumn();
  }

  private static final Icon EMPTY_CHANGE = EmptyIcon.sameSize(Icons.MERGE_STATE_CONFLICT);
  private static TableColumnAccessor<MergeValue, Object> createStateColumn(final boolean remote) {
    return new TableColumnBuilder<MergeValue, Object>().setId("state" + (remote ? "Remote" : "Local")).setHeaderText("")
      .setCanvasRenderer(new CanvasRenderer<MergeValue>() {
        public void renderStateOn(CellState state, Canvas canvas, MergeValue item) {
          Icon iconChange = remote ? Icons.MERGE_CHANGE_REMOTE : Icons.MERGE_CHANGE_LOCAL;
          Icon iconConflict = remote ? Icons.MERGE_CHANGE_REMOTE_CONFLICT : Icons.MERGE_CHANGE_LOCAL_CONFLICT;
          Icon icon;
          if (item.isConflict()) icon = iconConflict;
          else icon = item.isChanged(remote) ? iconChange : EMPTY_CHANGE;
          canvas.setIcon(icon);
          canvas.setFullyOpaque(true);
        }
      })
      .setConvertor(Convertors.<MergeValue, Object>constant(""))
      .setSizePolicy(ColumnSizePolicy.Calculated.fixedPixels(EMPTY_CHANGE.getIconWidth()))
      .createColumn();
  }

  private static final Icon NOT_MARKED = EmptyIcon.sameSize(Icons.POINTING_TRIANGLE);
  private static final Icon EMPTY_STATE = EmptyIcon.sameSize(Icons.MERGE_STATE_CONFLICT);
  private static final TableColumnAccessor<MergeValue, String> COLUMN_FIELD_NAME =
    new TableColumnBuilder<MergeValue, String>().setConvertor(MergeValue.GET_DISPLAY_NAME)
      .setId("Field Name")
      .setCanvasRenderer(new CanvasRenderer<MergeValue>() {
        @Override
        public void renderStateOn(CellState state, Canvas canvas, MergeValue item) {
          if (item == null) return;
          if (!item.isResolved()) {
            boolean conflict = item.isConflict();
            if (conflict && !state.isSelected()) canvas.setForeground(Color.RED);
            if (item.isChangeOrConflict()) canvas.setFontStyle(Font.BOLD);
          }
          Icon stateIcon = getStateIcon(item);
          canvas.setIcon(RowIcon.create(state.isSelected() ? Icons.POINTING_TRIANGLE : NOT_MARKED, stateIcon));
          canvas.appendText(item.getDisplayName());
        }

        public Icon getStateIcon(MergeValue value) {
          if (value.isResolved()) return EMPTY_STATE;
          if (value.isConflict()) return Icons.MERGE_STATE_CONFLICT;
          else if (value.isChanged(false)) return Icons.MERGE_STATE_CHANGED_LOCALLY;
          else if (value.isChanged(true))  return Icons.MERGE_STATE_CHANGED_REMOTELY;
          return EMPTY_STATE;
        }
      }).createColumn();

  public static void registerActions(ActionRegistry registry) {
    registry.registerAction(MainMenu.Merge.HIDE_NOT_CHANGED, ChangesOnlyAction.INSTANCE);
    registry.registerAction(MainMenu.Merge.RESOLVE_LOCAL, ApplyVersionAction.LOCAL);
    registry.registerAction(MainMenu.Merge.RESOLVE_BASE, ApplyVersionAction.BASE);
    registry.registerAction(MainMenu.Merge.RESOLVE_REMOTE, ApplyVersionAction.REMOTE);
    registry.registerAction(MainMenu.Merge.RESOLVE_IGNORE, IgnoreConflictAction.INSTANCE);
    registry.registerAction(MainMenu.Merge.COMMIT_UPLOAD, UploadMergeAction.INSTANCE);
  }
}
