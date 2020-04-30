package com.almworks.util.ui.swing;

import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Condition;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class DescendantsIterator<T extends Component> implements Iterator<T> {
  /**
   * When not-null peforms instance check and skips components which aren't instances of given class
   */
  @Nullable
  private final Class<? extends T> myInstanceFilter;
  /**
   * When not-null iterates tree only under container passing condition
   * (skips subtrees of containers not passing condition).
   * Hoverer the container itself will be iterated if if passes instance check.
   */
  @Nullable
  private final Condition<? super Container> myTreeFilter;
  @NotNull
  private final Container myAncestor;
  private final boolean myStrict;
  /**
   * Current container until iterator not surely at end, can look for next.
   * <br> null means iterator already reached end (hoverer next still may return element)
   */
  @Nullable
  private Container myCurrentContainer;
  private int myNextIndex;
  private T myNextComponent;
  public static final Condition<Object> IS_JCOMPONENT = Condition.<Object>isInstance(JComponent.class);

  private DescendantsIterator(@NotNull Container ancestor, boolean strict, Class<? extends T> instanceFilter, Condition<? super Container> treeFilter) {
    myAncestor = ancestor;
    myStrict = strict;
    myInstanceFilter = instanceFilter;
    myTreeFilter = treeFilter;
    reset();
  }

  public void reset() {
    myCurrentContainer = myAncestor;
    myNextIndex = 0;
    if (!myStrict)
      myNextComponent = applyInstanceFilter(myAncestor);
    Container container = myAncestor;
    myCurrentContainer = includeDescendants(container) ? myAncestor : null;
  }

  private boolean includeDescendants(Container container) {
    return (myTreeFilter == null || myTreeFilter.isAccepted(container)) &&
      container.getComponentCount() > 0;
  }

  public boolean hasNext() {
    lookForNextIfNeeded();
    return myNextComponent != null;
  }

  private void lookForNextIfNeeded() {
    if (myNextComponent != null || myCurrentContainer == null)
      return;
    assert SwingTreeUtil.isAncestor(myAncestor, myCurrentContainer);
    do {
      //noinspection ConstantConditions
      int count = myCurrentContainer.getComponentCount();
      while (myNextIndex < count) {
        assert myNextComponent == null;
        //noinspection ConstantConditions
        Component component = myCurrentContainer.getComponent(myNextIndex);
        myNextIndex++;
        myNextComponent = applyInstanceFilter(component);
        if (component instanceof Container) {
          Container container = (Container) component;
          if (includeDescendants(container)) {
            myCurrentContainer = container;
            myNextIndex = 0;
          }
        }
        if (myNextComponent != null)
          return;
      }
      //noinspection ConstantConditions
      Container parent = myCurrentContainer != myAncestor ? myCurrentContainer.getParent() : null;
      if (parent == null) {
        assert myCurrentContainer == myAncestor;
        myCurrentContainer = null;
        break;
      }
      myNextIndex = SwingTreeUtil.getComponentIndex(parent, myCurrentContainer);
      if (myNextIndex < 0) {
        assert false;
        myCurrentContainer = null;
        break;
      }
      myCurrentContainer = parent;
      myNextIndex++;
    } while (myCurrentContainer != null);
    myCurrentContainer = null;
  }

  private T applyInstanceFilter(Component component) {
    return myInstanceFilter != null ? Util.castNullable(myInstanceFilter, component) : (T)component;
  }

  public T next() {
    lookForNextIfNeeded();
    T next = myNextComponent;
    if (next == null)
      throw new NoSuchElementException();
    myNextComponent = null;
    return next;
  }

  public void remove() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public static <T extends Component> DescendantsIterator<T> allInstances(@NotNull Container ancestor, Class<? extends T> instanceFilter) {
    return create(ancestor, false, null, instanceFilter);
  }

  public static DescendantsIterator<Component> all(@NotNull Container ancestor, boolean strict) {
    return new DescendantsIterator<Component>(ancestor, strict, null, null);
  }

  public static <T extends Component> DescendantsIterator<T> instances(@NotNull Container ancestor, boolean strict, Class<? extends T> instanceFilter) {
    return new DescendantsIterator<T>(ancestor, strict, instanceFilter, null);
  }

  public static DescendantsIterator<Component> skipDescendants(@NotNull Container ancestor, boolean strict, Condition<? super Container> treeFilter) {
    return new DescendantsIterator<Component>(ancestor, strict, null, treeFilter);
  }

  public static <T extends Component> DescendantsIterator<T> create(@NotNull Container ancestor, boolean strict, Condition<? super Container> treeFilter, Class<? extends T> instanceFilter) {
    return new DescendantsIterator<T>(ancestor, strict, instanceFilter, treeFilter);
  }

  public static List<JComponent> collectDescendants(Container container, boolean strict) {
    return Containers.collectList(create(container, strict, Condition.<Object>isInstance(JComponent.class), JComponent.class));
  }
}
