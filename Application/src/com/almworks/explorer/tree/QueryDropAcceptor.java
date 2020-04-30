package com.almworks.explorer.tree;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.gui.meta.schema.dnd.DropItemAction;
import com.almworks.util.Pair;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.DndAction;
import com.almworks.util.ui.actions.dnd.DndUtil;
import com.almworks.util.ui.actions.dnd.DragContext;
import com.almworks.util.ui.actions.dnd.StringDragImageFactory;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class QueryDropAcceptor {
  private static final TypedKey<Map<GenericNode, AnAction>> COPY_DROP_ACTION_MAP = TypedKey.create("COPY_DROP_ACTION_MAP");
  private static final TypedKey<Map<GenericNode, AnAction>> MOVE_DROP_ACTION_MAP = TypedKey.create("MOVE_DROP_ACTION_MAP");

  public static boolean canAcceptItems(List<ItemWrapper> wrappers, GenericNode node, DragContext context) throws CantPerformException {
    if (node == null || !node.isNarrowing()) return false;
    AnAction dropAction = getDropAction(wrappers, node, context);
    UpdateContext uc = getUpdateContext(context, wrappers);
    dropAction.update(uc);
    boolean enabled = uc.getEnabled() == EnableState.ENABLED;
    if (enabled) {
      String description = uc.getPresentationProperty(PresentationKey.SHORT_DESCRIPTION);
      Pair<Color,Color> colors = uc.getPresentationProperty(PresentationKey.DESCRIPTION_FG_BG);
      if (colors == null) colors = GlobalColors.DND_DESCRIPTION_DARK_FG_BG;
      if (description != null && description.length() > 0) {
        StringDragImageFactory.ensureContext(context, DndUtil.ACTION_IMAGE_FACTORY, description, uc.getComponent(), colors);
      }
    }
    return enabled;
  }

  public static void acceptItems(List<ItemWrapper> wrappers, GenericNode node, DragContext context) {
    if (node == null || !node.isNarrowing()) return;
    AnAction dropAction = getDropAction(wrappers, node, context);
    try {
      UpdateContext uc = getUpdateContext(context, wrappers);
      dropAction.update(uc);
      if (uc.getEnabled() == EnableState.ENABLED) {
        dropAction.perform(uc);
      }
    } catch (CantPerformException e) {
      Log.debug("can't drop", e);
    }
  }

  @NotNull
  private static AnAction getDropAction(List<ItemWrapper> wrappers, GenericNode node, DragContext context) {
    boolean move = context.getAction() == DndAction.MOVE;
    TypedKey<Map<GenericNode, AnAction>> mapkey = move ? MOVE_DROP_ACTION_MAP : COPY_DROP_ACTION_MAP;
    AnAction dropAction = null;
    Map<GenericNode, AnAction> map = context.getValue(mapkey);
    if (map == null) {
      map = Collections15.hashMap();
      context.putValue(mapkey, map);
    } else {
      dropAction = map.get(node);
    }

    if (dropAction == null) {
      ItemHypercube hypercube = node.getHypercube(true);
      dropAction = getDropAction(wrappers, hypercube, "drop." + node.getNodeId(), move);
      if (dropAction == null)
        dropAction = AnAction.INVISIBLE;
      map.put(node, dropAction);
    }
    return dropAction;
  }

  private static UpdateContext getUpdateContext(DragContext context, List<ItemWrapper> wrappers) {
    Component c = context.getComponent();
    if (c == null) {
      assert false;
      c = new JLabel();
    }
    DropUpdateContext r = new DropUpdateContext(c, context);
    r.setWrappers(wrappers);
    return r;
  }

  private static AnAction getDropAction(List<ItemWrapper> wrappers, @Nullable ItemHypercube hypercube, String frameId,
    boolean move)
  {
    if (hypercube == null)
      return null;
    if (wrappers == null || wrappers.isEmpty())
      return null;
    return new DropItemAction(hypercube, frameId, move);
  }


  private static class DropUpdateContext extends DefaultUpdateContext {
    private final DragContext myParent;
    private List<ItemWrapper> myWrappers;

    public DropUpdateContext(Component c, DragContext context) {
      super(c, Updatable.NEVER);
      myParent = context;
    }

    @NotNull
    public <T> List<T> getSourceCollection(@NotNull TypedKey<? extends T> role) throws CantPerformException {
      if (role == ItemWrapper.ITEM_WRAPPER) {
        assert myWrappers != null;
        return (List<T>) myWrappers;
      }
      List<T> r = null;
      try {
        r = myParent.getSourceCollection(role);
      } catch (CantPerformException e) {
        // fall through
      }
      if (r != null && !r.isEmpty()) {
        return r;
      }
      return super.getSourceCollection(role);
    }

    public void setWrappers(List<ItemWrapper> wrappers) {
      myWrappers = wrappers;
    }
  }
}
