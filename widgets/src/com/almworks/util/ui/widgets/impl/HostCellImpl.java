package com.almworks.util.ui.widgets.impl;

import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.collections.ROArrayListWrapper;
import com.almworks.util.collections.SortedArrayMap;
import com.almworks.util.collections.SortedArraySet;
import com.almworks.util.commons.IntIntFunction;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.genutil.Log;
import com.almworks.util.ui.widgets.util.WidgetUtil;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

final class HostCellImpl implements ModifiableHostCell {
  private static final Log<HostCellImpl> log = Log.get(HostCellImpl.class);
  private static final TypedKey<JComponent> LIVE_COMPONENT = TypedKey.create("component");
  private static final TypedKey<DetachComposite> LIFESPAN = TypedKey.create("activeLife");
  public static final ComponentProperty<HostCellImpl> OWNING_CELL = ComponentProperty.createProperty("owingCell");
  public static final HostCellImpl[] EMPTY_ARRAY = new HostCellImpl[0];
  
  private final HostCellImpl myParent;
  private final HostComponentState<?> myState;
  private final Widget<?> myWidget;
  private SortedArrayMap<TypedKey<?>, Object> myValues = null;
  private SortedArraySet<TypedKey<?>> myPerm = null;
  private SearchFunction mySearchFunction = null;
  private boolean myChildrenSorted = true;
  private int myId;
  private int myPurpose = 0;
  private HostCellImpl[] myChildren = EMPTY_ARRAY;
  private int myChildrenSize = 0;
  private boolean myNotifyReshape = false;
  /**
   * In host's coordinates
   */
  private final Rectangle myBounds = new Rectangle();

  HostCellImpl(HostCellImpl parent, Widget<?> widget, int id, HostComponentState<?> state) {
    myParent = parent;
    myWidget = widget;
    myId = id;
    myState = state;
  }

  public boolean isActive() {
    return myPurpose != 0 || myChildrenSize != 0;
  }

  boolean isActive(Purpose purpose) {
    return (myPurpose & (1 << purpose.ordinal())) != 0;
  }

  public HostCellImpl getParent() {
    return myParent;
  }

  public Widget<?> getWidget() {
    return myWidget;
  }

  @Nullable
  public HostCellImpl findChild(int id) {
    int index = findIndex(id);
    return index >= 0 ? myChildren[index] : null;
  }

  @Override
  public void deleteAll() {
    boolean wasActive = isActive();
    boolean wasVisible = isActive(Purpose.VISIBLE);
    boolean hadFocus = isActive(Purpose.FOCUSED);
    boolean hadMouse = isActive(Purpose.MOUSE_HOVER);
    while (myChildrenSize > 0) {
      HostCellImpl child = myChildren[myChildrenSize - 1];
      myChildren[myChildrenSize - 1] = null;
      myChildrenSize--;
      assert child.isActive();
      child.deleteAll();
    }
    if (wasActive) {
      myPurpose = 0;
      doDeactivate(wasVisible, hadFocus, hadMouse);
    }
  }

  @Override
  public void repaint() {
    if (!isActive(Purpose.VISIBLE)) return;
    priRepaint();
  }

  private void priRepaint() {
    myState.repaint(myBounds.x, myBounds.y, myBounds.width, myBounds.height);
  }

  @Override
  public void invalidate() {
    myState.getLayoutManager().invalidate(this);
    myState.getEventManager().postEventToAncestors(this, EventContext.CELL_INVALIDATED, this);
  }

  @Override
  public int getId() {
    return myId;
  }

  @Override
  public int[] getChildrenCellIds() {
    ensureChildrenSorted();
    if (myChildrenSize == 0) return Const.EMPTY_INTS;
    int[] ids = new int[myChildrenSize];
    for (int i = 0; i < myChildrenSize; i++) {
      HostCellImpl child = myChildren[i];
      ids[i] = child.myId;
    }
    return ids;
  }

  @Override
  public void remapChildrenIds(int[] newIds) {
    if (newIds == null || newIds.length != myChildrenSize)
    throw new IllegalArgumentException("Wrong number of ids. Expected " + myChildrenSize);
    int[] copy = ArrayUtil.arrayCopy(newIds);
    Arrays.sort(copy);
    if (copy.length != ArrayUtil.removeSubsequentDuplicates(copy, 0, copy.length))
      throw new IllegalArgumentException("Duplicated ids found");
    for (int i = 0; i < myChildrenSize; i++) {
      HostCellImpl child = myChildren[i];
      child.myId = newIds[i];
    }
    myChildrenSorted = false;
  }

  @NotNull
  @Override
  public Rectangle getHostBounds(Rectangle target) {
    if (target == null) target = new Rectangle();
    if (isActive())
      target.setBounds(myBounds);
    else
      target.setBounds(-1, -1, -1, -1);
    return target;
  }

  @Nullable
  @Override
  public <T> T getStateValue(TypedKey<? extends T> key) {
    SortedArrayMap<TypedKey<?>, Object> values = myValues;
    return values != null ? key.cast(values.get(key)) : null;
  }

  @Override
  public <T> T getStateValue(TypedKey<? extends T> key, T nullValue) {
    T value = getStateValue(key);
    return value != null ? value : nullValue;
  }

  @Override
  public <T> void putStateValue(TypedKey<? super T> key, T value, boolean permanent) {
    if (key == LIVE_COMPONENT || key == LIFESPAN) throw new IllegalArgumentException();
    putStateValueImpl(key, value, permanent);
  }

  @Nullable
  @Override
  public HostCell getActiveCell() {
    return isActive() ? this : null;
  }

  public int getWidth() {
    return myBounds.width;
  }

  public int getHeight() {
    return myBounds.height;
  }

  public void requestFocus() {
    myState.getFocusManager().requestFocus(this);
  }

  public int getHostX() {
    return myBounds.x;
  }

  public int getHostY() {
    return myBounds.y;
  }

  @Override
  public boolean isFocused() {
    return myState.getFocusManager().getFocusedCell() == this;
  }

  @Override
  public void removeLiveComponent() {
    setLiveComponent(null);
  }

  @Override
  public JComponent getLiveComponent() {
    return getStateValue(LIVE_COMPONENT);
  }

  @Override
  public void setLiveComponent(JComponent component) {
    if (component == getLiveComponent()) return;
    priRemoveLiveComponent();
    if (component == null) return;
    Container parent = component.getParent();
    JComponent hostComponent = myState.getHost().getHostComponent();
    if (parent != null && parent != hostComponent) parent.remove(component);
    if (component.getParent() == null) component.setBounds(-1, -1, 0, 0);
    putStateValueImpl(LIVE_COMPONENT, component, true);
    OWNING_CELL.putClientValue(component, this);
    if (component.getParent() != hostComponent) hostComponent.add(component);
    myNotifyReshape = true;
    postEvent(EventContext.CELL_RESHAPED, null);
  }

  @Nullable
  @Override
  public HostCell getNextChild(int childId, boolean forward) {
    int index = findIndex(childId);
    if (index >= 0) {
      index += forward ? 1 : -1;
    } else {
      index = -index - 1;
      index += forward ? 0 : -1;
    }
    return (index >= 0 && index < myChildrenSize) ? myChildren[index] : null;
  }

  @Override
  public int getTreeDepth() {
    int count = 0;
    HostCellImpl ancestor = myParent;
    while (ancestor != null) {
      count++;
      ancestor = ancestor.myParent;
    }
    return count;
  }

  @Nullable
  @Override
  public <T> T restoreValue(Widget<? extends T> widget) {
    if (widget != myWidget) {
      log.error(this, "Wrong widget", widget, myWidget);
      return null;
    }
    CellStack stack = new CellStack();
    stack.buildToAncestor(this, myState.getRootCell(), myState.getValue());
    return (T) stack.topValue();
  }

  @Override
  public <T> void postEvent(TypedKey<T> reason, T data) {
    myState.getEventManager().postEvent(this, reason, data);
  }

  @Nullable
  @Override
  public HostCell getFirstChild(boolean fromBeginning) {
    if (myChildrenSize == 0) return null;
    ensureChildrenSorted();
    return fromBeginning ? myChildren[0] : myChildren[myChildrenSize - 1];
  }

  @Override
  public WidgetHost getHost() {
    return myState;
  }

  @Override
  public <T> int getChildPreferedWidth(int id, Widget<? super T> child, T value) {
    HostCellImpl childCell = findChild(id);
    return child.getPreferedWidth(childCell != null ? childCell : new CellContextImpl(myState), value);
  }

  @Override
  public <T> int getChildPreferedHeight(int id, Widget<? super T> child, T value, int width) {
    HostCellImpl childCell = findChild(id);
    return child.getPreferedHeight(childCell != null ? childCell : new CellContextImpl(myState), width, value);
  }

  @Override
  @Nullable
  public HostCell getAncestor(Widget<?> ancestorWidget) {
    HostCellImpl ancestor = myParent;
    while (ancestor != null && ancestor.myWidget != ancestorWidget) ancestor = ancestor.myParent;
    return ancestor;
  }

  private <T> void putStateValueImpl(TypedKey<? super T> key, T value, boolean perm) {
    if (myValues == null && value == null) return;
    if (myValues == null) {
      if (value == null) return;
      else myValues = SortedArrayMap.create();
    }
    int index = myValues.getKeyIndex(key);
    if (index < 0 && value == null) return;
    if (index >= 0) myValues.putValue(index, value);
    else myValues.put(key, value);
    if (myPerm == null) myPerm = SortedArraySet.create();
    if (perm)
      myPerm.add(key);
    else
      myPerm.remove(key);
  }

  void getChildrenImpl(List<HostCellImpl> children) {
    //noinspection ManualArrayToCollectionCopy
    for (int i = 0; i < myChildrenSize; i++) children.add(myChildren[i]);
  }

  HostCellImpl[] getChildren() {
    if (myChildrenSize == 0) return EMPTY_ARRAY;
    HostCellImpl[] array = new HostCellImpl[myChildrenSize];
    System.arraycopy(myChildren, 0, array, 0, array.length);
    return array;
  }

  public List<HostCell> getChildrenList() {
    if (myChildrenSize == 0) return Collections15.emptyList();
    return new ROArrayListWrapper(myChildren, myChildrenSize);
  }

  @Override
  public Lifespan getActiveLife() {
    DetachComposite life = getStateValue(LIFESPAN);
    if (life == null && isActive()) {
      life = new DetachComposite();
      putStateValueImpl(LIFESPAN, life, true);
    }
    return life != null && !life.isEnded() ? life : Lifespan.NEVER;
  }

  boolean isValid() {
    return false;
  }

  public void setBounds(int x, int y, int width, int height) {
    myState.getHost().repaint(myBounds.x, myBounds.y, myBounds.width, myBounds.height);
    myBounds.setBounds(x, y, width, height);
    myState.getMouseDispatcher().cellReshaped(this);
    myState.getHost().repaint(myBounds.x, myBounds.y, myBounds.width, myBounds.height);
    if (myNotifyReshape) myState.getEventManager().postEvent(this, EventContext.CELL_RESHAPED, null);
  }

  public void activate(Purpose purpose) {
    boolean wasActive = isActive();
    myPurpose = addActivation(myPurpose, purpose);
    if (!wasActive)
      doActivate();
  }

  public static int addActivation(int current, Purpose purpose) {
    return current | (1 << purpose.ordinal());
  }

  private void doActivate() {
    WidgetUtil.activateWidget(myWidget, this);
    if (myParent != null)
      myParent.childActivated(this);
  }

  private void childActivated(HostCellImpl child) {
    boolean wasActive = isActive();
    insertChild(child);
    if (!wasActive)
      doActivate();
  }

  private void insertChild(HostCellImpl child) {
    int index = findIndex(child.myId);
    if (index >= 0) {
      child.deleteAll();
    } else {
      myChildren = ArrayUtil.ensureCapacity(myChildren, myChildrenSize + 1);
      int insIndex = -index - 1;
      if (myChildrenSize - insIndex > 0)
        System.arraycopy(myChildren, insIndex, myChildren, insIndex + 1, myChildrenSize - insIndex);
      myChildren[insIndex] = child;
      myChildrenSize++;
    }
  }

  private int findIndex(int id) {
    if (myChildren.length == 0) return -1;
    ensureChildrenSorted();
    if (mySearchFunction == null) mySearchFunction = new SearchFunction();
    return mySearchFunction.find(id);
  }

  private void ensureChildrenSorted() {
    if (!myChildrenSorted) {
      if (myChildrenSize > 1) {
        Arrays.sort(myChildren, 0, myChildrenSize, ID_ORDER);
        int prevId = myChildren[0].myId;
        List<HostCellImpl> toDelete = null;
        for (int i = 1; i < myChildrenSize; i++) {
          HostCellImpl child = myChildren[i];
          int curId = child.myId;
          if (prevId == curId) {
            removeChild(i);
            if (toDelete == null)
              toDelete = Collections15.arrayList();
            toDelete.add(child);
          }
          prevId = curId;
        }
        myChildrenSorted = true;
        if (toDelete != null)
          for (HostCellImpl child : toDelete)
            child.deleteAll();
      }
    }
  }

  private void removeChild(int index) {
    System.arraycopy(myChildren, index + 1, myChildren, index, myChildrenSize - 1 - index);
    myChildren[myChildrenSize - 1] = null;
    myChildrenSize--;
  }

  public boolean contains(int x, int y) {
    return myBounds.contains(x, y);
  }

  public void deactivate(Purpose purpose) {
    boolean wasActive = isActive();
    myPurpose &= (~(1 << purpose.ordinal()));
    if (wasActive && !isActive()) {
      if (purpose == Purpose.VISIBLE) priRepaint();
      doDeactivate(false, false, false);
    }
  }

  private void doDeactivate(boolean wasVisible, boolean hadFocus, boolean hadMouse) {
    removeLifeComponentFromHost();
    WidgetUtil.deactivate(myWidget, this);
    putStateValueImpl(LIVE_COMPONENT, null, true);
    DetachComposite life = getStateValue(LIFESPAN);
    putStateValueImpl(LIFESPAN, null, true);
    if (myValues != null) myValues.clear();
    if (myPerm != null) myPerm.clear();
    if (hadFocus)
      myState.getFocusManager().focusedCellDeactivated(this);
    if (hadMouse)
      myState.getMouseDispatcher().resendMouse();
    if (myParent != null)
      myParent.childDeactivated(this, wasVisible);
    else if (wasVisible) {
      IWidgetHostComponent host = myState.getHost();
      host.repaint(0, 0, host.getWidth(), host.getHeight());
    }
    if (life != null) life.detach();
  }

  private void childDeactivated(HostCellImpl cellRef, boolean wasVisible) {
    int index = findIndex(cellRef.myId);
    if (index < 0 || myChildren[index] != cellRef)
      return;
    removeChild(index);
    if (!isActive())
      doDeactivate(wasVisible, false, false);
    else if (wasVisible)
      repaint();
  }


  @Nullable
  public HostCellImpl findChild(int x, int y) {
    for (int i = 0; i < myChildrenSize; i++) {
      HostCellImpl child = myChildren[i];
      assert child.isActive();
      if (child.contains(x, y))
        return child;
    }
    return null;
  }

  public HostComponentState<?> getHostState() {
    return myState;
  }

  @Nullable
  HostCellImpl getOrCreateChild(Widget<?> widget, int cellId) {
    assert widget != null;
    HostCellImpl child = findChild(cellId);
    if (child != null && widget != child.getWidget()) {
      child.deleteAll();
      if (!isActive()) return null;
    }
    if (child == null)
      child = new HostCellImpl(this, widget, cellId, myState);
    return child;
  }

  public void moveBounds(int dx, int dy, boolean repaint) {
    int oldX = myBounds.x;
    int oldY = myBounds.y;
    myBounds.setLocation(oldX + dx, oldY + dy);
    myState.getMouseDispatcher().cellReshaped(this);
    if (repaint)
      repaintMove(myState.getHost(), dx, dy, oldX, oldY, myBounds.width, myBounds.height);
    if (myNotifyReshape) myState.getEventManager().postEvent(this, EventContext.CELL_RESHAPED, null);
  }

  public static void repaintMove(IWidgetHostComponent hostComponent, int dx, int dy, int oldX, int oldY, int width, int height) {
    int x = oldX + dx;
    int y = oldY + dy;
    hostComponent.repaint(Math.min(oldX, x), Math.min(oldY, y), width + Math.abs(dx), height + Math.abs(dy));
  }

  public boolean isBoundsIntersect(Rectangle rectangle) {
    return myBounds.intersects(rectangle);
  }

  private JComponent priRemoveLiveComponent() {
    JComponent live = removeLifeComponentFromHost();
    putStateValueImpl(LIVE_COMPONENT, null, true);
    return live;
  }

  private JComponent removeLifeComponentFromHost() {
    JComponent live = getStateValue(LIVE_COMPONENT);
    if (live != null) {
      Container parent = live.getParent();
      IWidgetHostComponent stateHost = myState.getHost();
      JComponent host = stateHost.getHostComponent();
      if (parent == host) {
        parent.repaint(live.getX(), live.getY(), live.getWidth(), live.getHeight());
        stateHost.setRemovingComponent(live);
        try {
          parent.remove(live);
        } finally {
          stateHost.setRemovingComponent(null);
        }
        myNotifyReshape = false;
      }
    }
    return live;
  }

  public boolean intersects(Rectangle rectangle) {
    return myBounds.intersects(rectangle);
  }

  public boolean inAncestorOf(HostCellImpl decendant) {
    while (decendant != null) {
      if (decendant == this) return true;
      decendant = decendant.getParent();
    }
    return false;
  }

  public int getChildCount() {
    return myChildrenSize;
  }

  public HostComponentState<?> getState() {
    return myState;
  }

  private class SearchFunction implements IntIntFunction {
    private int mySearchId = -1;

    public int invoke(int index) {
      return myChildren[index].myId - mySearchId;
    }

    public int find(int id) {
      mySearchId = id;
      return CollectionUtil.binarySearch(myChildrenSize, this);
    }
  }
}
