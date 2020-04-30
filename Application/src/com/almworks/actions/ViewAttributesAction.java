package com.almworks.actions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.AdjustedSplitPane;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class ViewAttributesAction extends SimpleAction {
  public ViewAttributesAction() {
    super("View Attributes...");
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    LongArray items = ItemActionUtils.collectItems(wrappers);
    items.sortUnique();
    showAttributes(context, items);
  }

  private static void showAttributes(ActionContext context, LongArray items) throws CantPerformException {
    ConnectionManager connections = context.getSourceObject(Engine.ROLE).getConnectionManager();
    DialogManager dialogs = context.getSourceObject(DialogManager.ROLE);
    context.getSourceObject(Database.ROLE).readBackground(new LoadAttributes(items, connections, dialogs));
  }

  public static AnAction createViewItem() {
    return new SimpleAction("View Item Attributes...") {
      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
      }

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        JTextField field = new JTextField();
        field.setColumns(10);
        JPanel panel = new JPanel(UIUtil.createBorderLayout());
        panel.add(new JLabel("Item:"), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        int answer =
          DialogsUtil.askUser(context.getComponent(), panel, "View Attributes", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) return;
        long item;
        try {
          item = Long.parseLong(field.getText());
        } catch (NumberFormatException e) {
          DialogsUtil.showErrorMessage(context.getComponent(), "Illegal item " + field.getText(), "View Attributes");
          return;
        }
        showAttributes(context, LongArray.create(item));
      }
    };
  }

  private static class ItemHolder {
    private final long myItem;
    private final AttributeMap myValues;
    private final String myTypeString;
    public static final CanvasRenderer<ItemHolder> RENDERER = new CanvasRenderer<ItemHolder>() {
      @Override
      public void renderStateOn(CellState state, Canvas canvas, ItemHolder item) {
        canvas.appendLong(item.myItem);
        CanvasSection section = canvas.newSection();
        section.appendText(" ").appendText(item.getTypeString()).setFontStyle(Font.ITALIC);
        if (!state.isSelected()) section.setForeground(Color.DARK_GRAY);
      }
    };

    private String getTypeString() {
      return myTypeString;
    }

    private ItemHolder(long item, AttributeMap values, String typeString) {
      myItem = item;
      myValues = values;
      myTypeString = typeString;
    }

    public List<String> getValuesList() {
      List<String> result = Collections15.arrayList();
      for (DBAttribute<?> attribute : myValues.keySet()) {
        result.add(attribute.getId() + " = " +
          String.valueOf(myValues.get(attribute))
            .replaceAll("\r", "\\\\r")
            .replaceAll("\n", "\\\\n")
            .replaceAll("\t", "\\\\t"));
      }
      Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
      return result;
    }
  }

  private static class LoadAttributes implements ReadTransaction<Object>, Runnable {
    private final LongList mySourceItems;
    private final ConnectionManager myConnections;
    private final DialogManager myDialogs;
    private final List<ItemHolder> myLoaded = Collections15.arrayList();

    public LoadAttributes(LongList items, ConnectionManager connections, DialogManager dialogs) {
      mySourceItems = items;
      myConnections = connections;
      myDialogs = dialogs;
    }

    @Override
    public Object transaction(DBReader reader) throws DBOperationCancelledException {
      LongList items = collectItems(reader);
      for (int i = 0; i < items.size(); i++) {
        ItemVersion item = SyncUtils.readTrunk(reader, items.get(i));
        AttributeMap values = item.getAllValues();
        Long type = values.get(DBAttribute.TYPE);
        String typeString = type != null ? item.forItem(type).getValue(DBAttribute.ID) : null;
        if (typeString == null) typeString = "<Unknown> " + type;
        myLoaded.add(new ItemHolder(item.getItem(), values, typeString));
      }
      ThreadGate.AWT.execute(this);
      return null;
    }

    private LongList collectItems(DBReader reader) {
      LongSetBuilder result = new LongSetBuilder();
      for (int i = 0; i < mySourceItems.size(); i++) {
        ItemVersion item = SyncUtils.readTrunk(reader, mySourceItems.get(i));
        result.add(item.getItem());
        Long cItem = item.getValue(SyncAttributes.CONNECTION);
        if (cItem == null) continue;
        Connection connection = myConnections.findByItem(cItem);
        if (connection == null) continue;
        LongList slaves = connection.getProvider().getPrimaryStructure().loadEditableSlaves(item);
        result.addAll(slaves);
      }
      return result.commitToArray();
    }

    @Override
    public void run() {
      DialogBuilder builder = myDialogs.createBuilder("viewItemAttributes");
      builder.setTitle("Attributes " + mySourceItems.toString());
      builder.setContent(createContent(builder.getConfiguration()));
      builder.setEmptyCancelAction();
      builder.setModal(false);
      builder.showWindow();
    }

    private AdjustedSplitPane createContent(Configuration config) {
      final AList<String> values = new AList<String>();
      final AList<ItemHolder> list = new AList<ItemHolder>();
      SelectionAccessor<ItemHolder> selection = list.getSelectionAccessor();
      selection.addAWTChangeListener(new ChangeListener() {
        @Override
        public void onChange() {
          ItemHolder holder = list.getSelectionAccessor().getSelection();
          if (holder == null) values.setCollectionModel(AListModel.EMPTY);
          else {
            List<String> valueList = holder.getValuesList();
            values.setCollectionModel(FixedListModel.create(valueList));
          }
        }
      });
      list.setCollectionModel(FixedListModel.create(myLoaded));
      list.setCanvasRenderer(ItemHolder.RENDERER);
      selection.ensureSelectionExists();
      return UIUtil.createSplitPane(new JScrollPane(list), new JScrollPane(values), false, config, "divider", 0.5, 0);
    }
  }
}
