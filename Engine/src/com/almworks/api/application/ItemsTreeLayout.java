package com.almworks.api.application;

import com.almworks.api.engine.Connection;
import com.almworks.explorer.TableTreeStructure;
import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author dyoma
 */
@ThreadAWT
public class ItemsTreeLayout implements CanvasRenderable, Comparable<ItemsTreeLayout> {
  public static final String NO_HIERARCHY = "No hierarchy";
  public static final DataRole<ItemsTreeLayout> DATA_ROLE = DataRole.createRole(ItemsTreeLayout.class);

  public static final ItemsTreeLayout NONE = create(TableTreeStructure.FlatTree.INSTANCE, NO_HIERARCHY, "", 0, Long.MIN_VALUE);
  private static final CanvasRenderable NO_NAME = new CanvasRenderable.TextRenderable(Font.ITALIC, "unnamed");

  private String myId;
  private TableTreeStructure myTreeStructure;
  private String myDisplayName;
  private long myOwner;
  /** Long (instead of Integer) allows to fall back to item if there's no order in the DB. */
  private Long myOrder;

  private ItemsTreeLayout() {
  }

  @NotNull
  public static ItemsTreeLayout create(TableTreeStructure treeStructure, String displayName, String id, long owner, long order) {
    ItemsTreeLayout layout = new ItemsTreeLayout();
    layout.update(treeStructure, displayName, id, owner, order);
    return layout;
  }

  public void update(TableTreeStructure treeStructure, String displayName, String id, long owner, long order) {
    myTreeStructure = Util.NN(treeStructure, TableTreeStructure.FlatTree.INSTANCE);
    myDisplayName = displayName;
    myId = Util.NN(id);
    myOwner = owner;
    myOrder = order;
  }

  public TableTreeStructure getTreeStructure() {
    return myTreeStructure;
  }

  @Override
  public int compareTo(ItemsTreeLayout o) {
    if (o == null) return 1;
    int comp = Containers.compareLongs(myOrder, o.myOrder);
    if (comp != 0) return comp;
    return String.CASE_INSENSITIVE_ORDER.compare(myDisplayName, o.myDisplayName);
  }

  @Nullable
  public String getDisplayName() {
    return myDisplayName;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  public long getOwner() {
    return myOwner;
  }

  public void renderOn(Canvas canvas, CellState state) {
    String displayName = getDisplayName();
    if (displayName != null) {
      canvas.appendText(displayName);
    } else {
      NO_NAME.renderOn(canvas, state);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ItemsTreeLayout that = (ItemsTreeLayout) o;

    if (myOwner != that.myOwner) return false;
    if (!myDisplayName.equals(that.myDisplayName))
      return false;
    if (!myId.equals(that.myId))
      return false;
    if (!myTreeStructure.equals(that.myTreeStructure))
      return false;
    if (myOrder != null ? myOrder.equals(that.myOrder) : that.myOrder != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myTreeStructure.hashCode();
    result = 31 * result + myDisplayName.hashCode();
    result = 31 * result + myId.hashCode();
    result = 31 * result + (int)myOwner;
    result = 31 * result + (myOrder != null ? myOrder.hashCode() : 0);
    return result;
  }

  public String toString() {
    return "ATL[" + getDisplayName() +"]";
  }

  public static class OwnerFilter extends Condition<ItemsTreeLayout> {
    private final Connection myConnection;

    public OwnerFilter(Connection connection) {
      myConnection = connection;
    }

    @Override
    public boolean isAccepted(ItemsTreeLayout layout) {
      long owner = layout.getOwner();
      return owner == 0 || owner == myConnection.getConnectionItem() || owner == myConnection.getProviderItem();
    }
  }
}
