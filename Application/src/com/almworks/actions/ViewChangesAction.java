package com.almworks.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelKeySetUtil;
import com.almworks.api.explorer.ItemModelRegistry;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.ShadowVersionSource;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.AList;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import util.external.BitSet2;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class ViewChangesAction extends SimpleAction {
  public ViewChangesAction() {
    super("View Local Changes...");
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ItemWrapper item = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    ItemWrapper.DBStatus status = item.getDBStatus();
    context.setEnabled(status.isDiscardable() ? EnableState.ENABLED : EnableState.INVISIBLE);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    ItemWrapper item = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    if (!item.getDBStatus().isDiscardable()) return;
    ItemModelRegistry registry = context.getSourceObject(ItemModelRegistry.ROLE);
    DialogManager windows = context.getSourceObject(DialogManager.ROLE);
    context.getSourceObject(Database.ROLE).readBackground(new LoadChanges(item.getItem(), registry, windows,
      context.getComponent()));
  }

  private static class LoadChanges implements ReadTransaction<Object>, Runnable {
    private final long myItem;
    private final ItemModelRegistry myRegistry;
    private final DialogManager myWindows;
    private final Component myAncestorComponent;
    private final HashSet<ModelKey<?>> myChanged = Collections15.hashSet();
    private final HashSet<DBAttribute<?>> myAttributes = Collections15.hashSet();

    public LoadChanges(long item, ItemModelRegistry registry, DialogManager windows, Component ancestorComponent) {
      myItem = item;
      myRegistry = registry;
      myWindows = windows;
      myAncestorComponent = ancestorComponent;
    }

    @Override
    public Object transaction(DBReader reader) throws DBOperationCancelledException {
      ItemVersion server = ShadowVersionSource.conflict(reader).forItem(myItem);
      ItemVersion trunk = SyncUtils.readTrunk(reader, myItem);
      collectModelKeys(server, trunk);
      collectAttributes(server, trunk);
      ThreadGate.AWT.execute(this);
      return null;
    }

    private void collectAttributes(ItemVersion serverShadow, ItemVersion trunkShadow) {
      AttributeMap server = serverShadow.getAllShadowableMap();
      AttributeMap trunk = trunkShadow.getAllShadowableMap();
      myAttributes.addAll(server.keySet());
      myAttributes.addAll(trunk.keySet());
      for (Iterator<DBAttribute<?>> it = myAttributes.iterator(); it.hasNext();) {
        DBAttribute<?> attribute = it.next();
        if (SyncUtils.isEqualValueInMap(trunkShadow.getReader(), attribute, server, trunk)) it.remove();
      }
    }

    private void collectModelKeys(ItemVersion serverShadow, ItemVersion trunkShadow) {
      PropertyMap server = myRegistry.extractValues(serverShadow);
      PropertyMap trunk = myRegistry.extractValues(trunkShadow);
      if (server == null || trunk == null) return;
      BitSet2 allKeys = new BitSet2();
      allKeys.or(ModelKey.ALL_KEYS.getValue(server));
      allKeys.or(ModelKey.ALL_KEYS.getValue(trunk));
      for (int i = allKeys.nextSetBit(0); i >= 0; i = allKeys.nextSetBit(i + 1)) {
        ModelKey<?> key = ModelKeySetUtil.getKey(i);
        if (key == null) continue;
        if (key.isEqualValue(server, trunk)) continue;
        myChanged.add(key);
      }
    }

    @Override
    public void run() {
      if (myChanged.isEmpty()) {
        DialogsUtil.showMessage(myAncestorComponent, "Nothing changed", "Changes", JOptionPane.INFORMATION_MESSAGE);
        return;
      }
      DialogBuilder builder = myWindows.createBuilder("itemChanges");
      builder.setTitle("Changes");
      builder.setContent(createContent(builder.getConfiguration().getOrCreateSubset("content")));
      builder.setCancelAction("Close");
      builder.setModal(false);
      builder.showWindow();
    }

    private JComponent createContent(Configuration config) {
      JComponent keysContent = createKeysContent();
      JComponent attrContent = createAttributeContent();
      return UIUtil.createSplitPane(keysContent, attrContent, false, config, "divider", 0.5f, 0);
    }

    private JComponent createAttributeContent() {
      return createList(myAttributes, DBAttribute.TO_ID);
    }

    private JComponent createKeysContent() {
      return createList(myChanged, ModelKey.GET_NAME);
    }

    private <T> JComponent createList(Collection<? extends T> source, Convertor<T, String> convertor) {
      List<String> changed = convertor.collectList(source);
      Collections.sort(changed, String.CASE_INSENSITIVE_ORDER);
      AList<String> list = new AList<String>(FixedListModel.create(changed));
      list.getSelectionAccessor().ensureSelectionExists();
      return new JScrollPane(list);
    }
  }
}
