package com.almworks.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.impl.AttributeInfo;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static com.almworks.util.collections.Convertor.conv;
import static com.almworks.util.collections.Convertors.TO_STRING;
import static com.almworks.util.collections.Convertors.replaceAll;
import static com.almworks.util.collections.Functional.compose;
import static com.almworks.util.components.renderer.Renderers.*;

public class ViewShadowsAction extends SimpleAction {
  public ViewShadowsAction() {
    super("View Shadows...");
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    ItemWrapper item = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    DialogManager dialogs = context.getSourceObject(DialogManager.ROLE);
    context.getSourceObject(Database.ROLE).readBackground(new LoadShadows(item.getItem(), dialogs));
  }

  private static class LoadShadows implements ReadTransaction<Object>, Runnable {
    private static final CanvasRenderer<DBAttribute<?>> ATTRIBUTE_RENDERER = new CanvasRenderer<DBAttribute<?>>() {
      @Override
      public void renderStateOn(CellState state, Canvas canvas, DBAttribute<?> item) {
        if (item == null) return;
        String id = item.getId();
        int lastDot = id.lastIndexOf('.');
        if (lastDot <= 0 || lastDot >= id.length() - 1) {
          canvas.appendText(id);
          return;
        }
        canvas.appendText(id.substring(lastDot + 1));
        CanvasSection gray = canvas.emptySection();
        gray.setFontStyle(Font.ITALIC);
        if (!state.isSelected()) gray.setForeground(ColorUtil.between(state.getBackground(), state.getForeground(), 0.5f));
        gray.appendText(" (");
        gray.appendText(id.substring(0, lastDot));
        gray.appendText(")");
      }
    };
    private static final TableColumnAccessor<DBAttribute<?>, String> ATTRIBUTE_COLUMN;
    static {
      TableColumnBuilder<DBAttribute<?>, String> name = TableColumnBuilder.create("id", "ID");
      name.setConvertor(DBAttribute.TO_ID);
      name.setCanvasRenderer(ATTRIBUTE_RENDERER);
      ATTRIBUTE_COLUMN = name.createColumn();
    }

    private final long myItem;
    private final DialogManager myDialogs;
    private final AttributeMap myNotChanged = new AttributeMap();
    private AttributeMap myBase;
    private AttributeMap myConflict;
    private AttributeMap myDownload;
    private AttributeMap myTrunk;
    private List<DBAttribute<?>> myNotChangedList;
    private List<DBAttribute<?>> myChangedList;

    public LoadShadows(long item, DialogManager dialogs) {
      myItem = item;
      myDialogs = dialogs;
    }

    @Override
    public Object transaction(DBReader reader) throws DBOperationCancelledException {
      ItemVersion item = SyncUtils.readTrunk(reader, myItem);
      AttributeMap allValues = item.getAllValues();
      myTrunk = item.getAllShadowableMap();
      AttributeInfo info = AttributeInfo.instance(reader);
      for (DBAttribute<?> attr : allValues.keySet())
        if (!info.isShadowable(attr)) myNotChanged.putFrom(allValues, attr);
      myBase = item.getValue(SyncSchema.BASE);
      myConflict = item.getValue(SyncSchema.CONFLICT);
      myDownload = item.getValue(SyncSchema.DOWNLOAD);
      collectAttributes(reader);
      ThreadGate.AWT.execute(this);
      return null;
    }

    private void collectAttributes(DBReader reader) {
      HashSet<DBAttribute<?>> changed = Collections15.hashSet();
      addAttributes(changed, myBase);
      addAttributes(changed, myTrunk);
      addAttributes(changed, myConflict);
      addAttributes(changed, myDownload);
      HashSet<DBAttribute<?>> notChanged = Collections15.hashSet();
      for (Iterator<DBAttribute<?>> it = changed.iterator(); it.hasNext();) {
        DBAttribute<?> attribute = it.next();
        if (equalToTrunk(reader, attribute, myBase)
          && equalToTrunk(reader, attribute, myConflict)
          && equalToTrunk(reader, attribute, myDownload)) {
          it.remove();
          notChanged.add(attribute);
        }
      }
      for (DBAttribute<?> attribute : notChanged)
        if (!myNotChanged.containsKey(attribute)) myNotChanged.putFrom(myTrunk, attribute);
      myNotChangedList = orderAttributes(myNotChanged.keySet());
      myChangedList = orderAttributes(changed);
    }

    private List<DBAttribute<?>> orderAttributes(Set<DBAttribute<?>> attributes) {
      List<DBAttribute<?>> list = Collections15.arrayList(attributes);
      Collections.sort(list, Containers.convertingComparator(DBAttribute.TO_ID, String.CASE_INSENSITIVE_ORDER));
      return list;
    }

    private boolean equalToTrunk(DBReader reader, DBAttribute<?> attribute, AttributeMap shadow) {
      return shadow == null || SyncUtils.isEqualValueInMap(reader, attribute, myTrunk, shadow);
    }

    private void addAttributes(HashSet<DBAttribute<?>> all, AttributeMap shadow) {
      if (shadow != null) all.addAll(shadow.keySet());
    }

    @Override
    public void run() {
      DialogBuilder builder = myDialogs.createBuilder("viewShadows");
      builder.setTitle("Shadows for #" + myItem);
      builder.setContent(createContent(builder.getConfiguration()));
      builder.setEmptyCancelAction();
      builder.setModal(false);
      builder.showWindow();
    }

    private JComponent createContent(Configuration config) {
      return UIUtil.createSplitPane(createTable(myChangedList).wrapWithScrollPane(),
        createNotChanged().wrapWithScrollPane(),
        false, config, "divider", 0.5, 0);
    }

    private ATable<DBAttribute<?>> createNotChanged() {
      ATable<DBAttribute<?>> table = new ATable<DBAttribute<?>>();
      table.setCollectionModel(FixedListModel.create(myNotChangedList));
      List<TableColumnAccessor<DBAttribute<?>, ?>> columns = Collections15.arrayList();
      columns.add(ATTRIBUTE_COLUMN);
      addColumn(columns, myNotChanged, "Common Value");
      table.setColumnModel(FixedListModel.create(columns));
      return table;
    }

    private ATable<DBAttribute<?>> createTable(List<DBAttribute<?>> attributes) {
      ATable<DBAttribute<?>> table = new ATable<DBAttribute<?>>();
      table.setCollectionModel(FixedListModel.create(attributes));
      List<TableColumnAccessor<DBAttribute<?>, ?>> columns = Collections15.arrayList();
      columns.add(ATTRIBUTE_COLUMN);
      addColumn(columns, myTrunk, "Trunk");
      addColumn(columns, myBase, "Base");
      addColumn(columns, myConflict, "Conflict");
      addColumn(columns, myDownload, "Download");
      table.setColumnModel(FixedListModel.create(columns));
      return table;
    }

    private void addColumn(List<TableColumnAccessor<DBAttribute<?>, ?>> columns, final AttributeMap shadow, String name) {
      if (shadow == null) return;
      TableColumnBuilder<DBAttribute<?>, Object> builder = TableColumnBuilder.create(name, name);
      builder.setConvertor(new Convertor<DBAttribute<?>, Object>() {
        @Override
        public Object convert(DBAttribute<?> value) {
          return shadow.get(value);
        }
      });
      Convertor visualizeLineSep = conv(compose(compose(replaceAll("\r", "\\\\r").fun(), replaceAll("\n", "\\\\n").fun()), TO_STRING.fun()));
      CanvasRenderable.TextRenderable nullValue = new CanvasRenderable.TextRenderable(Font.ITALIC, "<null>");
      builder.setValueCanvasRenderer(quotedStringRenderer(convertingCanvasRenderer(defaultCanvasRenderer(nullValue), visualizeLineSep)));
      columns.add(builder.createColumn());
    }
  }
}
