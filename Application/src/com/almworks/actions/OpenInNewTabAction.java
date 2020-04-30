package com.almworks.actions;

import com.almworks.api.application.*;
import com.almworks.api.engine.Connection;
import com.almworks.explorer.CollectionBasedItemSource;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.*;
import gnu.trove.HashFunctions;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

/**
 * @author Stalex
 */
public class OpenInNewTabAction extends SimpleAction {
  private static final int MAX_TOOLTIP_ROWS = 20;
  private static final int MAX_TAB_NAME_LENGTH = 25;

  public OpenInNewTabAction() {
    super("Open in New Tab");
    setDefaultPresentation(PresentationKey.SHORTCUT,
      KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK));
    watchRole(LoadedItem.LOADED_ITEM);
    watchRole(ItemWrapper.ITEM_WRAPPER);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Show selected " + Terms.ref_artifacts + " in a new tab");
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    final List<ItemWrapper> list = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    if (!list.isEmpty())
      context.setEnabled(true);
  }

  protected void doPerform(final ActionContext context) throws CantPerformException {
    final ExplorerComponent explorer = context.getSourceObject(ExplorerComponent.ROLE);
    final List<ItemWrapper> items = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    if (items.size() == 1) {
      ItemWrapper wrapper = items.get(0);
      explorer.showItemInTab(wrapper);
    } else {
      showMultipleArtifacts(context.getSourceObject(Database.ROLE), explorer, items);
    }
  }

  private void showMultipleArtifacts(Database db, final ExplorerComponent explorer, final List<ItemWrapper> items) throws CantPerformException {
    db.readForeground(new ReadTransaction<Pair<String, String>>() {
      @Override
      public Pair<String, String> transaction(DBReader reader) throws DBOperationCancelledException {
        return getNameAndTooltip(items, reader);
      }
    }).finallyDo(ThreadGate.AWT, new Procedure<Pair<String, String>>() {
      @Override
      public void invoke(Pair<String, String> p) {
        // todo NPE!
        showTab(p.getFirst(), p.getSecond(), items, explorer);
      }
    });
  }

  private static Pair<String, String> getNameAndTooltip(List<ItemWrapper> items, DBReader reader) {
    StringBuilder nameBuffer = new StringBuilder();
    String namePrefix = "";
    int lastNameLength = 0;
    String name = null;
    StringBuilder tipBuffer = new StringBuilder();
    String tipPrefix = "<html>";
    int maxrows = MAX_TOOLTIP_ROWS;
    for (ItemWrapper itemWrapper : items) {
      final Connection connection = itemWrapper.getConnection();
      if (connection == null)
        continue;
      long item = itemWrapper.getItem();
      final ItemVersion trunk = SyncUtils.readTrunk(reader, item);
      String id = connection.getExternalIdString(trunk);
      String summary = connection.getItemSummary(trunk);
      if (id == null || summary == null)
        continue;
      if (name == null) {
        nameBuffer.append(namePrefix).append(id);
        if (lastNameLength > 0 && nameBuffer.length() > MAX_TAB_NAME_LENGTH) {
          nameBuffer.setLength(lastNameLength);
          nameBuffer.append("\u2026 (").append(items.size()).append(' ').append(Local.parse(Terms.ref_artifacts)).append(')');
          name = nameBuffer.toString();
        } else {
          lastNameLength = nameBuffer.length();
          namePrefix = ", ";
        }
      }
      tipBuffer.append(tipPrefix).append(id).append(" ").append(summary);
      tipPrefix = "<br>\n";
      if (--maxrows <= 0) {
        tipBuffer.append(tipPrefix).append(items.size() - MAX_TOOLTIP_ROWS).append(" more");
        break;
      }
    }
    if (name == null)
      name = nameBuffer.toString();
    String tooltip = tipBuffer.toString();
    Pair<String, String> p = Pair.create(name, tooltip);
    return p;
  }

  private void showTab(String contextName, String tooltip, List<ItemWrapper> items, ExplorerComponent explorer) {
    int hash = getHashCode(items);
    final ItemCollectionContext collectionContext =
      ItemCollectionContext.createNoNode(contextName, tooltip, new MyTabKey(hash));
    ItemSource source = CollectionBasedItemSource.create(items);
    explorer.showItemsInTab(source, collectionContext, true);
  }

  private int getHashCode(List<ItemWrapper> items) {
    int[] ids = new int[items.size()];
    for (int i = 0; i < items.size(); i++) {
      ItemWrapper item = items.get(i);
      ids[i] = item == null ? 0 : HashFunctions.hash(item.getItem());
    }
    Arrays.sort(ids);
    int r = 0;
    for (int id : ids) {
      r = 31 * r + id;
    }
    return r;
  }

  private static class MyTabKey implements TabKey {
    private final int myHash;

    public MyTabKey(int hash) {
      myHash = hash;
    }

    public boolean isReplaceTab(TabKey tabKey) {
      return false;
    }

    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      MyTabKey myTabKey = (MyTabKey) o;

      if (myHash != myTabKey.myHash)
        return false;

      return true;
    }

    public int hashCode() {
      return myHash;
    }
  }
}
