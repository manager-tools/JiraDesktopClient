package com.almworks.explorer;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.MetaInfo;
import com.almworks.api.engine.Connection;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.English;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.ACollectionComponent;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.TableDropPoint;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.dnd.ContextTransfer;
import com.almworks.util.ui.actions.dnd.DndUtil;
import com.almworks.util.ui.actions.dnd.DragContext;
import com.almworks.util.ui.actions.dnd.StringDragImageFactory;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author dyoma
 */
public abstract class BaseItemContextTransfer implements ContextTransfer {
  public static final TypedKey<LoadedItem> TARGET = TypedKey.create("target");

  @NotNull
  public Transferable transfer(DragContext context) throws CantPerformException {
    Collection<ItemWrapper> wrappers = Collections15.linkedHashSet(context.getSourceCollection(ItemWrapper.ITEM_WRAPPER));
    CantPerformException.ensureNotEmpty(wrappers);
    return new ItemWrappersTransferrable(wrappers);
  }

  public void cleanup(DragContext context) throws CantPerformException {
  }

  public void remove(ActionContext context) throws CantPerformException {
    throw new CantPerformException();
  }

  public boolean canRemove(ActionContext context) throws CantPerformException {
    return false;
  }

  public boolean canMove(ActionContext context) throws CantPerformException {
    // we use move / copy for alternative actions on pasting on multienum query
    return true;
  }

  public boolean canCopy(ActionContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    return wrappers.size() > 0;
  }

  public boolean canLink(ActionContext context) throws CantPerformException {
    return canCopy(context);
  }

  public boolean canImportData(DragContext context) throws CantPerformException {
    if (context == null)
      return false;
    Transferable transferable = context.getTransferable();
    return transferable != null &&
      DndUtil.isDataFlavorSupported(transferable, ItemWrappersTransferrable.ARTIFACTS_FLAVOR.getFlavor());
  }

  public boolean canImportDataNow(DragContext context, Component dropTarget) throws CantPerformException {
    ItemWrapper target = getTarget(context);
    if (target == null)
      return false;
    if (!isEditable(context, target))
      return false;
    return canImportDataNowToTarget(context, target);
  }

  protected boolean canImportDataNowToTarget(DragContext context, ItemWrapper target) throws CantPerformException {
    List<ItemWrapper> items = context.getTransferData(ItemWrappersTransferrable.ARTIFACTS_FLAVOR);
    if (items == null || items.isEmpty())
      return false;
    MetaInfo metaInfo = target.getMetaInfo();
    return metaInfo.canImport(context, target, items);
  }

  @Nullable
  protected static ItemWrapper getTarget(DragContext context) {
    List<ItemWrapper> targets = getTargets(context);
    return targets.isEmpty() ? null : targets.get(0);
  }
  
  protected static List<ItemWrapper> getTargets(DragContext context) {
    List<ItemWrapper> result = new ArrayList<ItemWrapper>();
    LoadedItem target = context.getValue(TARGET);
    if (target != null) {
      result.add(target);
      return result;
    }
    TableDropPoint dropPoint = context.getValue(DndUtil.TABLE_DROP_POINT);
    if (dropPoint != null && dropPoint.isValid()) {
      Object element = dropPoint.getTargetElement();
      if (!(element instanceof ItemWrapper))
        return result;
      result.add((ItemWrapper) element);
      return result;
    }
    if (context.isKeyboardTransfer()) {
      KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
      ACollectionComponent cc = getFocusedCollectionComponent(kfm.getFocusOwner());
      if (cc == null) {
        cc = getFocusedCollectionComponent(context.getComponent());
      }
      if (cc != null) {
        SelectionAccessor accessor = cc.getSelectionAccessor();
        List selectedItems = accessor.getSelectedItems();
        for (Object o : selectedItems)
          if (o instanceof ItemWrapper)
            result.add((ItemWrapper) o);
      }
    }
    return result;
  }

  private static ACollectionComponent getFocusedCollectionComponent(Component component) {
    if (component == null)
      return null;
    while (component != null && !(component instanceof ACollectionComponent))
      component = component.getParent();
    return (ACollectionComponent) component;
  }

  public void startDrag(DragContext dragContext, InputEvent event) throws CantPerformException {
  }

  public boolean canImportFlavor(DataFlavor flavor) {
    return Util.equals(flavor, ItemWrappersTransferrable.ARTIFACTS_FLAVOR.getFlavor());
  }

  public Factory<Image> getTransferImageFactory(DragContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = context.getTransferData(ItemWrappersTransferrable.ARTIFACTS_FLAVOR);
    Component c = context.getComponent();
    if (wrappers != null && c instanceof JComponent) {
      int count = wrappers.size();
      if (count > 0) {
        String text = buildDescription(wrappers);
        if (text == null) {
          text = count + " " + English.getSingularOrPlural("issue", count);
        }
        return new StringDragImageFactory(text, (JComponent) c, null);
      }
    }
    return null;
  }

  private static String buildDescription(List<ItemWrapper> wrappers) {
    StringBuilder builder = new StringBuilder();
    for (ItemWrapper wrapper : wrappers) {
      Connection c = wrapper.getConnection();
      if (c == null)
        return null;
      String id = c.getDisplayableItemId(wrapper);
      if (id == null)
        return null;
      if (builder.length() > 0)
        builder.append(", ");
      builder.append(id);
      // don't produce long lines
      if (builder.length() > 20)
        return null;
    }
    return builder.toString();
  }

  protected static boolean isEditable(DragContext context, ItemWrapper target) throws CantPerformException {
    LoadedItemServices services = target.services();
    if (services.isRemoteDeleted())
      return false;
    Connection connection = target.getConnection();
    if (connection == null || connection.getState().getValue().isDegrading())
      return false;
    if (!connection.isUploadAllowed()) return false;
    SyncManager syncMan = context.getSourceObject(SyncManager.ROLE);
    if (syncMan.findLock(target.getItem()) != null) {
      return false;
    }
    return true;
  }
}
